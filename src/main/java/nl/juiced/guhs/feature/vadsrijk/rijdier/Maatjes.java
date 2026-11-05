package nl.juiced.guhs.feature.vadsrijk.rijdier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierEffect;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierEntity;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;

/**
 * T-RIJDIER (roadmap §8 + decision R2: NO pet damage): what a pet does for you. Every pet gets a role and one automatic ability
 * ({@link Kracht}), built on G-PETS' {@link HuisdierEffect} extension point (tick every second + a small passive stat):
 * <ul>
 *   <li>{@link Rol#STEUNER}: a small heal, shield or energy boost (Klodderreus: shield when you're low; Koning Klodder: regen out of
 *       combat; Galmwolf: energy in combat; Vahoegje: the pink dragon-warmth, grows with the story);</li>
 *   <li>{@link Rol#SNUFFELAAR}: out of the fight: picks up your loot (Piepje) or sniffs out secrets nearby with a sparkle trail
 *       (Kruinroofvogel); both sometimes find a bonus coin on a kill.</li>
 * </ul>
 * Pets never deal damage, are never hit (they're invulnerable, G-PETS) and never need care. They level up a little (1..{@link #MAX},
 * xp = kills while they walk with you, bosses 10): every level makes the ability ~5 % stronger.
 * <p>New pets (zone 4+): {@code Maatjes.zet("petId", Maatjes.kracht("ability_id"))} or a whole new {@link Kracht} with your own
 * {@link HuisdierEffect}; texts {@code vadsrijk.huisdier.kracht.<id>.naam|tekst}. Unknown pets get the light default
 * ({@link #STANDAARD}: picks up loot).
 * <p>Storage: {@code SpelerData.extra("huisdieren")}: {@code lv} {id: level}, {@code xp} {id: xp}.
 */
public final class Maatjes {
    public enum Rol {
        STEUNER, SNUFFELAAR;

        public String naamKey() {
            return "vadsrijk.huisdier.rol." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** A pet's ability: id (texts {@code vadsrijk.huisdier.kracht.<id>.naam|tekst}), its role and what it does. */
    public record Kracht(String id, Rol rol, HuisdierEffect effect) {
        public String naamKey() {
            return "vadsrijk.huisdier.kracht." + id + ".naam";
        }

        public String tekstKey() {
            return "vadsrijk.huisdier.kracht." + id + ".tekst";
        }
    }

    public static final int MAX = 10;
    /** Seconds without hitting or being hit before you count as "out of combat". */
    public static final int RUST = 5;

    private static final Map<String, Kracht> KRACHTEN = new ConcurrentHashMap<>();
    private static final Map<String, Kracht> PER_DIER = new ConcurrentHashMap<>();
    /** Server: last fight tick per player (hit or being hit). */
    private static final Map<UUID, Long> GEVECHT = new ConcurrentHashMap<>();
    /** Server: ability cooldowns, "uuid|kracht" -> game time it is ready again. */
    private static final Map<String, Long> KLAAR = new ConcurrentHashMap<>();
    /** Server: secrets already announced per player (sniffing), to say "I smell something" once per spot. */
    private static final Map<UUID, java.util.Set<Long>> GEMELD = new ConcurrentHashMap<>();

    // ---- the abilities -------------------------------------------------------------------------------------------------------------

    public static final Kracht KLODDERSCHILD = registreer(new Kracht("klodderschild", Rol.STEUNER, new HuisdierEffect() {
        @Override
        public void tick(ServerPlayer p, HuisdierEntity h) {
            if (inGevecht(p) && Spelers.vads(p) < Spelers.maxVads(p) * 0.5 && klaar(p, "klodderschild", 30)) {
                Statussen.geef(p, Status.KNUFFELSCHILD, 160, Spelers.maxVads(p) * 0.08 * sterkte(p), p);
                poef(p, h, ParticleTypes.ITEM_SLIME);
                melding(p, "vadsrijk.huisdier.kracht.klodderschild.doet");
            }
        }

        @Override
        public Stats extraStats(ServerPlayer p) {
            return Stats.van(Stat.ELEMENTWEERSTAND, 2.0);
        }
    }));

    public static final Kracht KUSSENTJE = registreer(new Kracht("koninklijk_kussentje", Rol.STEUNER, new HuisdierEffect() {
        @Override
        public void tick(ServerPlayer p, HuisdierEntity h) {
            if (!inGevecht(p) && Spelers.vads(p) < Spelers.maxVads(p)) {
                Schade.heal(null, p, Spelers.maxVads(p) * 0.02 * sterkte(p));
                if (h.level() instanceof ServerLevel sl && h.tickCount % 3 == 0) {
                    sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.9, p.getZ(), 1, 0.2, 0.1, 0.2, 0);
                }
            }
        }

        @Override
        public Stats extraStats(ServerPlayer p) {
            return Stats.van(Stat.HEALKRACHT, 3.0);
        }
    }));

    public static final Kracht GALMLIED = registreer(new Kracht("galmlied", Rol.STEUNER, new HuisdierEffect() {
        @Override
        public void tick(ServerPlayer p, HuisdierEntity h) {
            if (inGevecht(p) && Spelers.energie(p) < Spelers.maxEnergie(p) && klaar(p, "galmlied", 10)) {
                var d = Spelers.data(p);
                d.energie = Math.min(Spelers.maxEnergie(p), Spelers.energie(p) + Spelers.maxEnergie(p) * 0.08 * sterkte(p));
                Spelers.sync(p);
                poef(p, h, ParticleTypes.NOTE);
            }
        }

        @Override
        public Stats extraStats(ServerPlayer p) {
            return Stats.van(Stat.LOOPSNELHEID, 2.0);
        }
    }));

    public static final Kracht KRUIMELS = registreer(new Kracht("kruimeltjes_rapen", Rol.SNUFFELAAR, new HuisdierEffect() {
        @Override
        public void tick(ServerPlayer p, HuisdierEntity h) {
            raap(p, h, 6 + 0.3 * level(p, h.soortId()));
        }

        @Override
        public Stats extraStats(ServerPlayer p) {
            return Stats.van(Stat.LOOPSNELHEID, 2.0);
        }
    }));

    public static final Kracht SCHERP_OOG = registreer(new Kracht("scherp_oog", Rol.SNUFFELAAR, new HuisdierEffect() {
        @Override
        public void tick(ServerPlayer p, HuisdierEntity h) {
            if (h.tickCount % 100 < 20 && !inGevecht(p)) {   // every 5 s
                Snuffelen.snuffel(p, h, 24 + level(p, h.soortId()), GEMELD.computeIfAbsent(p.getUUID(), k -> ConcurrentHashMap.newKeySet()));
            }
        }

        @Override
        public Stats extraStats(ServerPlayer p) {
            return Stats.van(Stat.LOOPSNELHEID, 2.0);
        }
    }));

    /** Vahoegje's own ability: grows with the story ({@link Vahoegje#stadium}). */
    public static final Kracht DRAKENWARMTE = registreer(new Kracht("drakenwarmte", Rol.STEUNER, new HuisdierEffect() {
        @Override
        public void tick(ServerPlayer p, HuisdierEntity h) {
            boolean groot = Vahoegje.stadium(p) >= Vahoegje.MIDDEL;
            if (!inGevecht(p) && Spelers.vads(p) < Spelers.maxVads(p)) {
                Schade.heal(null, p, Spelers.maxVads(p) * (groot ? 0.015 : 0.01) * sterkte(p));
            }
            if (inGevecht(p) && Spelers.vads(p) < Spelers.maxVads(p) * 0.35 && klaar(p, "drakenwarmte", groot ? 60 : 75)) {
                Schade.heal(null, p, Spelers.maxVads(p) * (groot ? 0.22 : 0.15) * sterkte(p));
                if (groot) {
                    Statussen.geef(p, Status.KNUFFELSCHILD, 120, Spelers.maxVads(p) * 0.08 * sterkte(p), p);
                }
                vlammetje(p, h, groot);
                melding(p, groot ? "vadsrijk.huisdier.kracht.drakenwarmte.doet_groot" : "vadsrijk.huisdier.kracht.drakenwarmte.doet");
            }
        }

        @Override
        public Stats extraStats(ServerPlayer p) {
            return Vahoegje.stadium(p) >= Vahoegje.MIDDEL ? Stats.van(Stat.HEALKRACHT, 4.0, Stat.LOOPSNELHEID, 2.0) : Stats.van(Stat.HEALKRACHT, 2.0);
        }
    }));

    /** For pets without their own ability (a later zone's boss pet before it gets one): picks up loot, no passive. */
    public static final Kracht STANDAARD = registreer(new Kracht("snuffeltje", Rol.SNUFFELAAR, new HuisdierEffect() {
        @Override
        public void tick(ServerPlayer p, HuisdierEntity h) {
            raap(p, h, 4);
        }
    }));

    static {
        zet("klodderreus", KLODDERSCHILD);
        zet("koning_klodder", KUSSENTJE);
        zet("galmwolf", GALMLIED);
        zet("piepje", KRUIMELS);
        zet("kruinroofvogel", SCHERP_OOG);
        zet("vahoegje", DRAKENWARMTE);
    }

    private Maatjes() {
    }

    public static Kracht registreer(Kracht k) {
        KRACHTEN.put(k.id(), k);
        return k;
    }

    @Nullable
    public static Kracht kracht(String id) {
        return KRACHTEN.get(id);
    }

    /** Gives a pet its ability (other slices: zone-4 pets). */
    public static void zet(String huisdier, Kracht k) {
        PER_DIER.put(huisdier, k);
    }

    /** The pet's ability: its own {@link Huisdier#effect} wins, else the one set here, else {@link #STANDAARD}. */
    public static Kracht van(Huisdier h) {
        Kracht k = PER_DIER.get(h.id());
        if (k != null) {
            return k;
        }
        if (h.effect() != null) {
            return new Kracht(h.id(), Rol.STEUNER, h.effect());
        }
        return STANDAARD;
    }

    // ---- levels --------------------------------------------------------------------------------------------------------------------

    private static CompoundTag tag(ServerPlayer p) {
        return Spelers.data(p).extra("huisdieren");
    }

    public static int level(ServerPlayer p, String id) {
        return Math.max(1, Math.min(MAX, tag(p).getCompoundOrEmpty("lv").getIntOr(id, 1)));
    }

    public static long xp(ServerPlayer p, String id) {
        return tag(p).getCompoundOrEmpty("xp").getLongOr(id, 0);
    }

    /** Xp from this level to the next (kills while it walks along; a boss = 10). */
    public static long xpNodig(int level) {
        return level >= MAX ? 0 : 12L * level;
    }

    /** The strength factor of the active pet's ability: +5 % per level above 1. */
    static double sterkte(ServerPlayer p) {
        Huisdier h = Huisdieren.actief(p);
        return 1 + 0.05 * ((h == null ? 1 : level(p, h.id())) - 1);
    }

    /** Gives the pet xp; levels up with a little moment. Returns the new level. */
    public static int geefXp(ServerPlayer p, String id, long n) {
        if (!Huisdieren.heeft(p, id) || n <= 0) {
            return level(p, id);
        }
        int lv = level(p, id);
        long xp = xp(p, id) + n;
        boolean op = false;
        while (lv < MAX && xp >= xpNodig(lv)) {
            xp -= xpNodig(lv);
            lv++;
            op = true;
        }
        if (lv >= MAX) {
            xp = 0;
        }
        CompoundTag l = tag(p).getCompoundOrEmpty("lv"), x = tag(p).getCompoundOrEmpty("xp");
        l.putInt(id, lv);
        x.putLong(id, xp);
        tag(p).put("lv", l);
        tag(p).put("xp", x);
        if (op) {
            Huisdier h = Huisdieren.van(id);
            p.sendSystemMessage(Component.translatable("vadsrijk.huisdier.level_op", Component.translatable(h == null ? id : h.naamKey()), lv)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.8f);
            HuisdierEntity e = Huisdieren.uit(p);
            if (e != null && e.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 0.4, e.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
            }
        }
        return lv;
    }

    /** Dev: set a pet's level. */
    public static void zetLevel(ServerPlayer p, String id, int lv) {
        CompoundTag l = tag(p).getCompoundOrEmpty("lv"), x = tag(p).getCompoundOrEmpty("xp");
        l.putInt(id, Math.max(1, Math.min(MAX, lv)));
        x.putLong(id, 0);
        tag(p).put("lv", l);
        tag(p).put("xp", x);
    }

    // ---- hooks ---------------------------------------------------------------------------------------------------------------------

    /** Every second for every player with a pet walking along (in the Vadsrijk). */
    static void tick(ServerPlayer p) {
        Huisdier h = Huisdieren.actief(p);
        HuisdierEntity e = Huisdieren.uit(p);
        if (h == null || e == null || e.isRemoved() || !Vadsrijk.in(p) || !p.isAlive() || !h.id().equals(e.soortId())) {
            return;
        }
        try {
            van(h).effect().tick(p, e);
        } catch (RuntimeException ex) {
            com.mojang.logging.LogUtils.getLogger().warn("[Vadsrijk] pet ability {} failed", h.id(), ex);
        }
    }

    /** Kern {@code Spelers.EXTRA_STATS}: the active pet's small passive (only while it walks along in the Vadsrijk). */
    static Stats stats(ServerPlayer p) {
        Huisdier h = Huisdieren.actief(p);
        if (h == null || !Vadsrijk.in(p)) {
            return null;
        }
        Stats s = van(h).effect().extraStats(p);
        return s;
    }

    /** Kern {@code Schade.NA_KLAP}: hitting or being hit = in combat. */
    static void klap(Schade.Uitkomst u) {
        if (u.klap().bron() instanceof ServerPlayer p) {
            GEVECHT.put(p.getUUID(), p.level().getGameTime());
        }
        if (u.klap().doel() instanceof ServerPlayer p) {
            GEVECHT.put(p.getUUID(), p.level().getGameTime());
        }
    }

    /** Kern {@code Haken.VERSLAGEN}: pet xp for every helper whose pet walks along, and the Snuffelaars' bonus coin. */
    static void verslagen(Haken.Verslagen v) {
        boolean baas = v.mob() instanceof VadsrijkBaas;
        for (ServerPlayer p : v.helpers()) {
            Huisdier h = Huisdieren.actief(p);
            if (h == null || Huisdieren.uit(p) == null) {
                continue;
            }
            geefXp(p, h.id(), baas ? 10 : 1);
            Kracht k = van(h);
            if (k.rol() == Rol.SNUFFELAAR && k != STANDAARD) {
                double kans = 0.05 * sterkte(p);
                if (p.getRandom().nextDouble() < kans) {
                    long brons = Math.max(2, v.mob().mobLevel() * 3L);
                    Spelers.geefMunten(p, brons);
                    p.sendOverlayMessage(Component.translatable("vadsrijk.huisdier.bonus", Component.translatable(h.naamKey()), Spelers.munten(brons))
                            .withStyle(ChatFormatting.GOLD));
                    HuisdierEntity e = Huisdieren.uit(p);
                    if (e != null && e.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.WAX_ON, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }
        }
    }

    public static boolean inGevecht(ServerPlayer p) {
        Long t = GEVECHT.get(p.getUUID());
        return t != null && p.level().getGameTime() - t < RUST * 20L;
    }

    /** (tests / dev) pretend the player just fought, or clear it. */
    public static void zetGevecht(ServerPlayer p, boolean aan) {
        if (aan) {
            GEVECHT.put(p.getUUID(), p.level().getGameTime());
        } else {
            GEVECHT.remove(p.getUUID());
        }
    }

    /** An ability's cooldown: true (and starts the cooldown) when ready. */
    static boolean klaar(ServerPlayer p, String kracht, int seconden) {
        String k = p.getUUID() + "|" + kracht;
        long nu = p.level().getGameTime();
        Long op = KLAAR.get(k);
        if (op != null && nu < op) {
            return false;
        }
        KLAAR.put(k, nu + seconden * 20L);
        return true;
    }

    /** (tests / dev) all cooldowns of a player ready again. */
    public static void wisCooldowns(ServerPlayer p) {
        String pre = p.getUUID() + "|";
        KLAAR.keySet().removeIf(k -> k.startsWith(pre));
        GEMELD.remove(p.getUUID());
    }

    static void uitgelogd(ServerPlayer p) {
        GEVECHT.remove(p.getUUID());
        GEMELD.remove(p.getUUID());
        wisCooldowns(p);
    }

    // ---- little effects ------------------------------------------------------------------------------------------------------------

    /** Picks up the owner's loot lying within r blocks (vanilla pickup rules: personal drops stay personal). Returns how many. */
    static int raap(ServerPlayer p, HuisdierEntity h, double r) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return 0;
        }
        int n = 0;
        for (ItemEntity it : sl.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(r), e -> e.isAlive() && !e.hasPickUpDelay())) {
            if (sl.getNearestPlayer(it, r + 2) != p) {
                continue;
            }
            int voor = it.getItem().getCount();
            Vec3 van = it.position();
            it.playerTouch(p);
            if (it.isRemoved() || it.getItem().getCount() < voor) {
                n++;
                sl.sendParticles(ParticleTypes.WAX_ON, van.x, van.y + 0.2, van.z, 4, 0.1, 0.1, 0.1, 0.02);
            }
        }
        if (n > 0) {
            sl.playSound(null, h.blockPosition(), SoundEvents.FOX_EAT, SoundSource.NEUTRAL, 0.35f, 1.9f);
        }
        return n;
    }

    private static void poef(ServerPlayer p, HuisdierEntity h, net.minecraft.core.particles.ParticleOptions deeltje) {
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(deeltje, h.getX(), h.getY() + 0.4, h.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.4, p.getZ(), 3, 0.35, 0.3, 0.35, 0);
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
    }

    /** Vahoegje's pink flame: a line of pink dust from the pet to you + hearts (+ a bigger burst when it has grown). */
    private static void vlammetje(ServerPlayer p, HuisdierEntity h, boolean groot) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 a = h.position().add(0, 0.35, 0), b = p.position().add(0, 1.0, 0);
        DustParticleOptions roze = new DustParticleOptions(0xFF5FB4, groot ? 1.3f : 1.0f);
        for (int i = 0; i <= 12; i++) {
            Vec3 v = a.lerp(b, i / 12.0);
            sl.sendParticles(roze, v.x, v.y, v.z, 2, 0.05, 0.05, 0.05, 0);
        }
        sl.sendParticles(ParticleTypes.HEART, b.x, b.y + 0.6, b.z, groot ? 8 : 4, 0.4, 0.3, 0.4, 0);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, b.x, b.y, b.z, groot ? 24 : 12, 0.5, 0.5, 0.5, 0.03);
        sl.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.35f, 1.9f);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    private static void melding(ServerPlayer p, String key) {
        Huisdier h = Huisdieren.actief(p);
        p.sendOverlayMessage(Component.translatable(key, h == null ? Component.empty() : Component.translatable(h.naamKey()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** P-screen data per owned pet: {lv, xp, nodig}; plus Vahoegje's stage. */
    public static void schermData(ServerPlayer p, CompoundTag d) {
        CompoundTag l = new CompoundTag();
        for (String id : Huisdieren.bezit(p)) {
            CompoundTag c = new CompoundTag();
            int lv = level(p, id);
            c.putInt("lv", lv);
            c.putLong("xp", xp(p, id));
            c.putLong("nodig", xpNodig(lv));
            l.put(id, c);
        }
        d.put("levels", l);
        d.putInt("vahoegje", Vahoegje.stadium(p));
    }
}
