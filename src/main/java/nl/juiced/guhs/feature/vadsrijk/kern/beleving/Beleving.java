package nl.juiced.guhs.feature.vadsrijk.kern.beleving;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * Vadsrijk "beleving" (fix round 1, VOORSTEL_EPISCHER_ZONE1 ideas 19-22, 25, 27, 28): the server side of the combat feel.
 * Everything hooks into kern ({@link Schade#VOOR_KLAP}/{@link Schade#NA_KLAP}, {@link Haken#VERSLAGEN}, {@link Spelers#XP_FACTOR},
 * {@link BaasKist#CEREMONIE}, the phase call in {@code VadsrijkBaas.krijgSchade}); the client effects are in
 * {@code beleving.client.BelevingClient}.
 * <ul>
 *   <li>hit-stop + camera kick on heavy hits and crits, screen shake on boss hits, big landings and ults;</li>
 *   <li>the sound layer ({@link Klankkleur}); the damage numbers ({@link Getalstijl});</li>
 *   <li>the Vrolijke reeks ({@link Reeks}): HUD counter, rising plof notes, confetti + xp bonus from x10;</li>
 *   <li>{@link #faseMoment}: every boss phase is a mini-moment (camera swoop to the boss, freeze, roar, the bar flashes the phase name);</li>
 *   <li>{@link #overwinning}: slow-mo, a pink shockwave over the arena, guhs dance, the title card "Opgevrolijkt!";</li>
 *   <li>the chest ceremony ({@link Kistceremonie}).</li>
 * </ul>
 */
public final class Beleving {
    public static final int ROZE = 0xFF7AC8;
    /** Ticks after a boss defeat before its chests start to fall. */
    public static final int KIST_NA_OVERWINNING = 50;

    private record Taak(long tick, Runnable r) {
    }

    private static final List<Taak> TAKEN = new ArrayList<>();
    @Nullable
    private static Schade.Klap huidig;
    private static final Map<UUID, Long> KLANK = new HashMap<>();
    private static final Map<Integer, Long> ELEMENT_KLANK = new HashMap<>();
    private static final Map<UUID, Double> ULT = new HashMap<>();
    private static final Map<Integer, double[]> BAAS_LUCHT = new HashMap<>();
    private static final Set<String> FASES_GEHAD = new HashSet<>();
    /** The last boss victory (for the chest timing): tick, x, y, z. */
    static long laatsteOverwinning = -1000;
    @Nullable
    static Vec3 overwinningPlek;

    private Beleving() {
    }

    public static void register() {
        Schade.VOOR_KLAP.add(0, (k, n) -> {
            huidig = k;
            return n;
        });
        Schade.NA_KLAP.add(Beleving::naKlap);
        Haken.VERSLAGEN.add(Beleving::verslagen);
        Spelers.XP_FACTOR.add(p -> Reeks.xpFactor(Reeks.stand(p.getUUID(), nu(p))));
        BaasKist.CEREMONIE = new Kistceremonie();
        NeoForge.EVENT_BUS.addListener(Beleving::tick);
        NeoForge.EVENT_BUS.addListener(Beleving::baasTick);
        NeoForge.EVENT_BUS.addListener((LivingFallEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && Vadsrijk.in(p) && e.getDistance() > 4) {
                schud(p, (float) Math.min(0.8, 0.15 + e.getDistance() * 0.04), 8);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            Reeks.wis(e.getEntity().getUUID());
            ULT.remove(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            Kistceremonie.allesNeer();
            for (Taak t : new ArrayList<>(TAKEN)) {
                if (t.r() instanceof Kistceremonie.Neer) {
                    t.r().run();
                }
            }
            TAKEN.clear();
            Reeks.wisAlles();
            FASES_GEHAD.clear();
        });
        NeoForge.EVENT_BUS.addListener(BelevingCommando::register);
    }

    public static void payloads(PayloadRegistrar r) {
        BelevingPayloads.register(r);
    }

    static long nu(ServerPlayer p) {
        return p.level().getServer() == null ? p.level().getGameTime() : p.level().getServer().getTickCount();
    }

    /** Was the hit being shown right now a DoT tick (a status)? Read by {@link Getalstijl}. */
    static boolean huidigeIsDot() {
        Schade.Klap k = huidig;
        return k != null && "status".equals(k.skillId());
    }

    /** Runs {@code r} {@code ticks} server ticks from now. */
    static void later(ServerLevel sl, int ticks, Runnable r) {
        TAKEN.add(new Taak(sl.getServer().getTickCount() + Math.max(0, ticks), r));
    }

    private static void tick(ServerTickEvent.Post e) {
        long nu = e.getServer().getTickCount();
        if (!TAKEN.isEmpty()) {
            List<Taak> klaar = new ArrayList<>();
            Iterator<Taak> it = TAKEN.iterator();
            while (it.hasNext()) {
                Taak t = it.next();
                if (t.tick() <= nu) {
                    klaar.add(t);
                    it.remove();
                }
            }
            for (Taak t : klaar) {
                try {
                    t.r().run();
                } catch (RuntimeException ex) {
                    com.mojang.logging.LogUtils.getLogger().warn("vadsrijk beleving: a timed effect failed", ex);
                }
            }
        }
        Kistceremonie.tick(e.getServer());
        if (nu % 5 == 0) {
            for (Map.Entry<UUID, Integer> weg : Reeks.verlopen(nu).entrySet()) {
                if (e.getServer().getPlayerList().getPlayer(weg.getKey()) instanceof ServerPlayer p) {
                    CompoundTag d = new CompoundTag();
                    d.putInt("n", weg.getValue());
                    BelevingPayloads.stuur(p, "reeks_weg", d);
                }
            }
        }
        // ults: the ult charge drops from full to (almost) nothing = an ult was used
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (!Vadsrijk.in(p)) {
                continue;
            }
            double nuLading = Spelers.data(p).ultLading;
            Double voor = ULT.put(p.getUUID(), nuLading);
            if (voor != null && voor >= 99.5 && nuLading < 50) {
                schud(p, 0.7f, 12);
                for (ServerPlayer ander : spelersBij((ServerLevel) p.level(), p.position(), 20)) {
                    if (ander != p) {
                        schud(ander, 0.3f, 8);
                    }
                }
            }
        }
        if (KLANK.size() > 512) {
            KLANK.clear();
        }
        if (ELEMENT_KLANK.size() > 1024) {
            ELEMENT_KLANK.clear();
        }
    }

    // ---- hits ---------------------------------------------------------------------------------------------------------

    private static void naKlap(Schade.Uitkomst u) {
        huidig = null;
        Schade.Klap k = u.klap();
        if (u.schade() <= 0 || !(k.doel().level() instanceof ServerLevel sl)) {
            return;
        }
        LivingEntity doel = k.doel(), bron = k.bron();
        boolean dot = "status".equals(k.skillId());
        long nu = sl.getServer().getTickCount();
        if (bron instanceof ServerPlayer p) {
            String soort = wapenSoort(p);
            if (!dot && !Long.valueOf(nu).equals(KLANK.put(p.getUUID(), nu))) {
                Klankkleur.speel(sl, doel, Klankkleur.wapen(soort), 1f);
            }
            if (u.kritiek()) {
                Klankkleur.speel(sl, doel, Klankkleur.KRITIEK, 1f);
                sl.sendParticles(ParticleTypes.END_ROD, doel.getX(), doel.getY() + doel.getBbHeight() * 0.7, doel.getZ(), 6, 0.3, 0.3, 0.3, 0.08);
            }
            if (!Long.valueOf(nu).equals(ELEMENT_KLANK.put(doel.getId(), nu))) {
                Klankkleur.speel(sl, doel, Klankkleur.element(k.element()), dot ? 0.45f : 1f);
            }
            if (!dot && doel instanceof VadsrijkMob m) {
                boolean zwaar = u.schade() >= 0.12 * m.maxVads() || (("hamer".equals(soort) || "knots".equals(soort)) && "basis".equals(k.skillId()));
                int ticks = hitStop(u.kritiek(), zwaar);
                if (ticks > 0) {
                    klap(sl, m, p, ticks, u.kritiek() ? 1.0f : 0.6f);
                }
            }
        } else if (bron instanceof VadsrijkBaas && doel instanceof ServerPlayer dp && !dot) {
            float sterkte = (float) Math.max(0.25, Math.min(1.0, u.schade() / Math.max(1, Spelers.maxVads(dp)) * 4));
            schud(dp, sterkte, 10);
        }
    }

    /** Pure: the hit-stop length in ticks (0 = none). */
    public static int hitStop(boolean kritiek, boolean zwaar) {
        return kritiek && zwaar ? 4 : kritiek ? 3 : zwaar ? 2 : 0;
    }

    static String wapenSoort(ServerPlayer p) {
        Uitrusting w = Spelers.wapen(p);
        return w == null || w.basisType() == null ? "vuist" : w.basisType().soort();
    }

    /** The hit-stop: the target freezes for everyone near; the attacker's camera kicks. */
    static void klap(ServerLevel sl, LivingEntity doel, @Nullable ServerPlayer aanvaller, int ticks, float kick) {
        for (ServerPlayer p : spelersBij(sl, doel.position(), 40)) {
            CompoundTag d = new CompoundTag();
            d.putInt("id", doel.getId());
            d.putInt("ticks", ticks);
            d.putFloat("kick", p == aanvaller ? kick : 0);
            BelevingPayloads.stuur(p, "klap", d);
        }
    }

    /** Screen shake for one player (sterkte 0..1, scaled again by their own setting). */
    public static void schud(ServerPlayer p, float sterkte, int ticks) {
        CompoundTag d = new CompoundTag();
        d.putFloat("sterkte", sterkte);
        d.putInt("ticks", ticks);
        BelevingPayloads.stuur(p, "schud", d);
    }

    /** Screen shake for everyone around a spot, weaker with distance. */
    public static void schudRond(ServerLevel sl, Vec3 plek, double straal, float sterkte, int ticks) {
        for (ServerPlayer p : spelersBij(sl, plek, straal)) {
            float f = (float) (1 - p.position().distanceTo(plek) / straal);
            if (f > 0.05f) {
                schud(p, sterkte * (0.35f + 0.65f * f), ticks);
            }
        }
    }

    static List<ServerPlayer> spelersBij(ServerLevel sl, Vec3 plek, double straal) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.position().distanceToSqr(plek) <= straal * straal) {
                l.add(p);
            }
        }
        return l;
    }

    /** Boss landings: a boss that falls more than 2.5 blocks shakes the screens around it. */
    private static void baasTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof VadsrijkBaas b) || !(b.level() instanceof ServerLevel sl)) {
            return;
        }
        double[] s = BAAS_LUCHT.computeIfAbsent(b.getId(), i -> new double[] {1, b.getY()});
        boolean grond = b.onGround();
        if (!grond) {
            s[1] = s[0] > 0 ? b.getY() : Math.max(s[1], b.getY());
        } else if (s[0] == 0 && s[1] - b.getY() > 2.5 && !b.geplofd()) {
            float sterkte = (float) Math.min(1.0, 0.3 + (s[1] - b.getY()) * 0.08);
            schudRond(sl, b.position(), 28, sterkte, 12);
            sl.sendParticles(ParticleTypes.POOF, b.getX(), b.getY() + 0.2, b.getZ(), 20, b.getBbWidth() * 0.6, 0.1, b.getBbWidth() * 0.6, 0.05);
        }
        s[0] = grond ? 1 : 0;
        if (b.isRemoved() || b.geplofd()) {
            BAAS_LUCHT.remove(b.getId());
        }
    }

    // ---- defeats: the streak and the victory ---------------------------------------------------------------------------

    private static void verslagen(Haken.Verslagen v) {
        VadsrijkMob m = v.mob();
        if (!(m.level() instanceof ServerLevel sl)) {
            return;
        }
        for (ServerPlayer p : v.helpers()) {
            long nu = nu(p);
            int n = Reeks.tel(p.getUUID(), nu);
            CompoundTag d = new CompoundTag();
            d.putInt("n", n);
            d.putBoolean("mijlpaal", Reeks.mijlpaal(n));
            d.putInt("xp", (int) Math.round((Reeks.xpFactor(n) - 1) * 100));
            BelevingPayloads.stuur(p, "reeks", d);
            if (Reeks.mijlpaal(n)) {
                confetti(sl, m.position().add(0, m.getBbHeight() * 0.6, 0), n >= 25 ? 2 : 1);
            }
        }
        if (m instanceof VadsrijkBaas b) {
            overwinning(b);
        }
    }

    /** The confetti plof (streak milestones). */
    static void confetti(ServerLevel sl, Vec3 c, int groot) {
        int[] kleuren = {0xFF5FC8, 0xFFD24A, 0x5BD1FF, 0x8CFF6A, 0xC88CFF, 0xFFFFFF};
        for (int kleur : kleuren) {
            sl.sendParticles(new DustParticleOptions(kleur, 1.4f), c.x, c.y, c.z, 10 * groot, 0.6 * groot, 0.5 * groot, 0.6 * groot, 0.4);
        }
        sl.sendParticles(ParticleTypes.FIREWORK, c.x, c.y, c.z, 20 * groot, 0.2, 0.2, 0.2, 0.25);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.9f, 1.3f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 0.7f, 1.4f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    private static Vec3 midden(VadsrijkBaas b) {
        Arena a = b.arena();
        return a != null ? Vec3.atBottomCenterOf(a.centrum()) : b.position();
    }

    private static double straal(VadsrijkBaas b) {
        Arena a = b.arena();
        return a != null ? Math.max(8, a.straal()) : Math.max(12, b.losseStraal() > 0 ? Math.min(b.losseStraal(), 24) : 16);
    }

    private static List<ServerPlayer> kijkers(ServerLevel sl, VadsrijkBaas b) {
        Vec3 c = midden(b);
        List<ServerPlayer> l = spelersBij(sl, c, straal(b) + 24);
        for (ServerPlayer p : spelersBij(sl, b.position(), 32)) {
            if (!l.contains(p)) {
                l.add(p);
            }
        }
        return l;
    }

    /**
     * The phase mini-moment (idea 25), for every boss: kern calls this when a phase starts (before {@code opFase}); the bazen
     * slice may call it too (it plays once per boss and phase). For the players around: a 1 s slow-mo feel (camera swoop to the
     * boss, the boss freezes, letterbox), the boss roars, and the boss bar flashes with the phase name
     * ({@code vadsrijk.kern.beleving.fase.<entity id path>.<fase>}, else "Fase N: Nu wordt het menens!").
     */
    public static void faseMoment(VadsrijkBaas baas, int fase) {
        faseMoment(baas, fase, false);
    }

    static void faseMoment(VadsrijkBaas baas, int fase, boolean altijd) {
        if (!(baas.level() instanceof ServerLevel sl) || fase < 2) {
            return;
        }
        if (!FASES_GEHAD.add(baas.getUUID() + ":" + fase) && !altijd) {
            return;
        }
        if (FASES_GEHAD.size() > 2000) {
            FASES_GEHAD.clear();
        }
        String pad = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(baas.getType()).getPath();
        String terugval = "Fase " + fase + ": " + (fase >= 3 ? "Alles of niks!" : "Nu wordt het menens!");
        for (ServerPlayer p : kijkers(sl, baas)) {
            CompoundTag d = new CompoundTag();
            d.putInt("id", baas.getId());
            d.putInt("fase", fase);
            d.putString("key", "vadsrijk.kern.beleving.fase." + pad + "." + fase);
            d.putString("terugval", terugval);
            BelevingPayloads.stuur(p, "fase", d);
            schud(p, 0.6f, 16);
        }
        Vec3 c = baas.position();
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.6f, 0.85f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.35f, 1.7f);
        double r = baas.getBbWidth() + 1.5;
        for (int i = 0; i < 40; i++) {
            double a = Math.PI * 2 * i / 40;
            sl.sendParticles(new DustParticleOptions(0xC23A3A, 2.0f), c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + baas.getBbHeight() * 0.5, c.z, 2, 0.5, 0.5, 0.5, 0);
    }

    /**
     * The victory moment (idea 27): the final blow in slow-mo (client), then a pink shockwave over the arena that leaves petals
     * behind, the guhs around dance, and a title card "Opgevrolijkt!". The chests fall a little later ({@link Kistceremonie}).
     */
    public static void overwinning(VadsrijkBaas baas) {
        if (!(baas.level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 c = midden(baas);
        double straal = straal(baas);
        laatsteOverwinning = sl.getServer().getTickCount();
        overwinningPlek = c;
        List<ServerPlayer> kijkers = kijkers(sl, baas);
        for (ServerPlayer p : kijkers) {
            CompoundTag d = new CompoundTag();
            d.putInt("id", baas.getId());
            BelevingPayloads.stuur(p, "overwinning", d);
        }
        Vec3 b = baas.position();
        sl.playSound(null, b.x, b.y, b.z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.2f, 1.2f);
        sl.playSound(null, b.x, b.y, b.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
        later(sl, 18, () -> schokgolf(sl, c, straal));
        later(sl, 20, () -> dans(sl, c, straal + 16));
        Component naam = Component.translatable(baas.titelKey());
        later(sl, 34, () -> {
            for (ServerPlayer p : kijkers) {
                if (!p.isRemoved()) {
                    RijkScenes.titel(p, Component.translatableWithFallback("vadsrijk.kern.beleving.opgevrolijkt", "Opgevrolijkt!")
                            .withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE), naam, 70, ROZE);
                }
            }
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.1f);
        });
    }

    /** The pink shockwave: a ring that rolls out over 24 ticks and leaves petals on the floor. */
    static void schokgolf(ServerLevel sl, Vec3 c, double straal) {
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.6f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.0f, 0.8f);
        schudRond(sl, c, straal + 16, 0.5f, 14);
        DustParticleOptions roze = new DustParticleOptions(0xFF5FC8, 2.2f), licht = new DustParticleOptions(0xFFC2E8, 1.6f);
        int stappen = 24;
        for (int i = 0; i < stappen; i++) {
            int stap = i;
            later(sl, i, () -> {
                double r = straal * (stap + 1) / stappen;
                int punten = (int) Math.min(72, Math.max(16, r * 4));
                for (int j = 0; j < punten; j++) {
                    double a = Math.PI * 2 * j / punten + stap * 0.1;
                    double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r;
                    sl.sendParticles(j % 2 == 0 ? roze : licht, x, c.y + 0.4, z, 1, 0, 0.15, 0, 0);
                }
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x, c.y + 0.5, c.z, 8 + stap, r * 0.6, 0.2, r * 0.6, 0.02);
            });
        }
        for (int i = 0; i < 6; i++) {
            later(sl, stappen + i * 8, () -> sl.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x, c.y + 2, c.z, 40, straal * 0.6, 1.0, straal * 0.6, 0.01));
        }
    }

    /** The guhs around come alive: little hops with notes for 3 s (they stay where they are). */
    static void dans(ServerLevel sl, Vec3 c, double straal) {
        List<nl.juiced.guhs.entity.GuhEntity> guhs = sl.getEntitiesOfClass(nl.juiced.guhs.entity.GuhEntity.class, new AABB(c, c).inflate(straal, 12, straal));
        for (int i = 0; i < 8; i++) {
            int stap = i;
            later(sl, i * 8, () -> {
                for (nl.juiced.guhs.entity.GuhEntity g : guhs) {
                    if (g.isRemoved() || !g.onGround()) {
                        continue;
                    }
                    if ((g.getId() + stap) % 2 == 0) {
                        g.setDeltaMovement(g.getDeltaMovement().add(0, 0.42, 0));
                        g.hurtMarked = true;
                    }
                    sl.sendParticles(ParticleTypes.NOTE, g.getX(), g.getY() + g.getBbHeight() + 0.4, g.getZ(), 1, 0.2, 0, 0.2, 1);
                    if (stap % 3 == 0) {
                        sl.sendParticles(ParticleTypes.HEART, g.getX(), g.getY() + g.getBbHeight() + 0.2, g.getZ(), 1, 0.2, 0.1, 0.2, 0);
                    }
                }
            });
        }
    }

    /** A pink screen flash for one player. */
    static void flits(ServerPlayer p, int kleur, int ticks) {
        CompoundTag d = new CompoundTag();
        d.putInt("kleur", kleur);
        d.putInt("ticks", ticks);
        BelevingPayloads.stuur(p, "flits", d);
    }
}
