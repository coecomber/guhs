package nl.juiced.guhs.feature.einde2.zij;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.einde2.castschurk.E2CastSchurkFeature;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;

/**
 * einde2 zij: the Westelijke Luchttempel ({@code westelijke_luchttempel}, four steps, air): the temple that hangs upside
 * down has gone silent and its sky bison herd has strayed.
 * <ol start="0">
 *   <li>talk to the luchtnon-guh in the hanging hall (she asks for air bending);</li>
 *   <li>make the five wind corridors sing: stand on the balcony at the mouth of each ({@code windfluit_1..5}) and use
 *       your AIR there;</li>
 *   <li>bring the herd home: five strayed bison wait on ledges and clouds ({@code kudde_1..5}, only reachable flying:
 *       Appa, wings); a click and it flies to its own pen ({@code bizon_1..5});</li>
 *   <li>back to the luchtnon.</li>
 * </ol>
 * The herd is per player: the strays the phase-A build placed show only to players who have not reached step 2; a player
 * at step 2 sees their own five ({@link Zicht#alleenVoor}, never saved), and the pens hold the herd (residents of this
 * slice at {@code bizon_*}) for whoever brought it home.
 */
public final class Luchttempel {
    static final String Q = Zij.Q + "lucht.";
    static final String S = Einde2Ids.WESTELIJKE_LUCHTTEMPEL;
    public static final int KUDDE = 5, VLUCHT_TICKS = 100;
    /** The entity tag of a player's own bison (with persistent data {@code Eigenaar}, {@code Nr}). */
    public static final String TAG = "guhs_einde2_zij_kudde";

    /** A bison of one player that flies home: where from, where to, since when. */
    private record Vlucht(Vec3 van, Vec3 naar, long start) {
    }

    /** (not saved) player -&gt; their own five bison entities (index nr - 1). */
    private static final Map<UUID, UUID[]> EIGEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Vlucht> VLIEGT = new ConcurrentHashMap<>();

    private Luchttempel() {
    }

    static Verhaallijn ln() {
        return ZijLijnen.LUCHT;
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.LUCHTNON_GUH, S + "/luchtnon", new Rol());
        Krachten.NA_GEBRUIK.add(Luchttempel::kracht);
        CastWezen.BIJ_KLIK.add(Luchttempel::klik);
        for (int i = 1; i <= KUDDE; i++) {
            Bewoners.anders(S + "/bizon_" + i, "guhs:luchtbizon_guh", i - 1, true, 0);
        }
        Bewoners.BIJ_MAKEN.add((e, plek) -> zicht(e, plek));
    }

    /** The strays of the build only for players before step 2, the herd in the pens only for those who brought it home. */
    static void zicht(Entity e, String plek) {
        if (plek.startsWith(S + "/kudde_")) {
            Zicht.alleenBij(e, ln().id(), 0, 1);
        } else if (plek.startsWith(S + "/bizon_")) {
            Zicht.alleenBij(e, ln().id(), 3, 99);
        }
    }

    static final class Rol extends QuestRol {
        Rol() {
            super(ZijLijnen.LUCHT);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.speel("praat");
            switch (stap) {
                case 0 -> {
                    if (!Zij.element(p, ZijLijnen.LUCHTTEMPEL, npc)) {
                        return;
                    }
                    ln().begin(p);
                    scherm(p, npc, Q + "welkom", new Praat.Optie(1, Q + "optie.help"), new Praat.Optie(2, Q + "optie.wat"));
                }
                case 1 -> {
                    zeg(p, npc, Q + "fluit_nog", KUDDE - gezongen(p));
                    hint(p, Q + "hint.fluit");
                }
                case 2 -> {
                    zeg(p, npc, Q + "kudde_nog", KUDDE - thuis(p));
                    hint(p, Q + "hint.kudde");
                }
                case 3 -> {
                    if (verder(p, 3)) {
                        npc.speel("fluit");
                        scherm(p, npc, Q + "klaar", new Praat.Optie(9, Q + "optie.dank"));
                        Zij.eenmalig(ln(), p, "beloning", new ItemStack(nl.juiced.guhs.feature.einde2.deco.E2DecoFeature.WINDGONG_ITEM.get(), 2),
                                new ItemStack(nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get(), 16));
                        p.level().playSound(null, npc.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.NEUTRAL, 1f, 1.2f);
                    }
                }
                default -> {
                    npc.speel("fluit");
                    zeg(p, npc, Q + "dank." + p.getRandom().nextInt(3));
                }
            }
        }

        @Override
        protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
            if (stap != 0) {
                return;
            }
            if (optie == 2) {
                scherm(p, npc, Q + "uitleg", new Praat.Optie(1, Q + "optie.help"));
            } else if (optie == 1 && verder(p, 0)) {
                zeg(p, npc, Q + "fluit");
                hint(p, Q + "hint.fluit");
            }
        }
    }

    static int gezongen(ServerPlayer p) {
        int n = 0;
        for (int i = 1; i <= KUDDE; i++) {
            n += ln().vlag(p, "fluit_" + i) ? 1 : 0;
        }
        return n;
    }

    static int thuis(ServerPlayer p) {
        int n = 0;
        for (int i = 1; i <= KUDDE; i++) {
            n += ln().vlag(p, "kudde_" + i) ? 1 : 0;
        }
        return n;
    }

    // --- step 1: the wind corridors ------------------------------------------------------------------------------------------

    static void kracht(ServerPlayer p, Krachten.Kracht k) {
        if (k.element() == Einde2Ids.Element.LUCHT && ln().stap(p) == 2) {
            blaasNaarHuis(p);
            return;
        }
        if (k.element() != Einde2Ids.Element.LUCHT || ln().stap(p) != 1) {
            return;
        }
        ZijPlek.Kopie kopie = ZijPlek.bij(p, S, 8);
        if (kopie == null) {
            return;
        }
        for (int i = 1; i <= KUDDE; i++) {
            if (!kopie.bij("windfluit_" + i, p.position(), 4.5)) {
                continue;
            }
            if (ln().vlag(p, "fluit_" + i)) {
                Zij.melding(p, Q + "fluit_al");
                return;
            }
            ln().vlag(p, "fluit_" + i, true);
            zing(p, kopie, i);
            int n = gezongen(p);
            if (n >= KUDDE && ln().verder(p, 1)) {
                for (int j = 1; j <= KUDDE; j++) {
                    zing(p, kopie, j);
                }
                Zij.titel(p, net.minecraft.network.chat.Component.translatable(Q + "fluit_titel").withStyle(net.minecraft.ChatFormatting.AQUA), null, 4, 40, 10);
                Zij.stem(p, Q + "naam.luchtnon", Q + "fluit_klaar");
                Zij.hint(p, Q + "hint.kudde");
            } else {
                Zij.melding(p, Q + "fluit_een", n, KUDDE);
            }
            return;
        }
    }

    /** One corridor sings for this player: a flute tone of its own and a gust through the corridor. */
    private static void zing(ServerPlayer p, ZijPlek.Kopie kopie, int i) {
        ServerLevel level = p.level();
        float[] toon = {0.75f, 0.9f, 1.0f, 1.2f, 1.5f};
        Zij.geluid(p, SoundEvents.NOTE_BLOCK_FLUTE.value(), 1f, toon[i - 1]);
        Zij.geluid(p, SoundEvents.BREEZE_WHIRL, 0.6f, 0.8f + 0.1f * i);
        AABB gang = kopie.zone("windgang_" + i);
        if (gang != null) {
            for (int s = 0; s < 12; s++) {
                double f = s / 11.0;
                double x = gang.minX + (gang.maxX - gang.minX) * (gang.getXsize() > gang.getZsize() ? f : 0.5);
                double z = gang.minZ + (gang.maxZ - gang.minZ) * (gang.getXsize() > gang.getZsize() ? 0.5 : f);
                level.sendParticles(p, ParticleTypes.GUST, true, false, x, gang.minY + 1.5, z, 1, 0.2, 0.2, 0.2, 0);
                level.sendParticles(p, ParticleTypes.CLOUD, true, false, x, gang.minY + 1.5, z, 3, 0.4, 0.4, 0.4, 0.02);
            }
        }
    }

    // --- step 2: the herd ------------------------------------------------------------------------------------------------------

    /** How far an air power reaches a stray (fix round 1: the strays are hard to reach, a gust from afar sends one home). */
    static final double BLAAS_BEREIK = 40, BLAAS_HOEK = 12;

    /**
     * fix round 1 (fix2-geestzij): an air power aimed at one of your own strays (within {@value #BLAAS_BEREIK} blocks, within
     * {@value #BLAAS_HOEK} degrees of where you look) sends it home as a click does: no need to fly right up to a cloud.
     */
    static void blaasNaarHuis(ServerPlayer p) {
        UUID[] eigen = EIGEN.get(p.getUUID());
        if (eigen == null) {
            return;
        }
        Vec3 oog = p.getEyePosition();
        Vec3 kijk = p.getViewVector(1f);
        CastWezen best = null;
        double bestCos = Math.cos(Math.toRadians(BLAAS_HOEK));
        for (UUID u : eigen) {
            Entity e = u == null ? null : p.level().getEntity(u);
            if (!(e instanceof CastWezen w) || VLIEGT.containsKey(u)) {
                continue;
            }
            Vec3 naar = w.position().add(0, w.getBbHeight() / 2, 0).subtract(oog);
            double d = naar.length();
            if (d > BLAAS_BEREIK || d < 0.01) {
                continue;
            }
            double cos = naar.normalize().dot(kijk);
            if (cos >= bestCos) {
                bestCos = cos;
                best = w;
            }
        }
        if (best != null) {
            p.level().sendParticles(p, ParticleTypes.GUST, true, false, best.getX(), best.getY() + 1, best.getZ(), 3, 0.5, 0.5, 0.5, 0);
            klik(best, p);
        }
    }

    /** A click on one of the player's own strays: it flies home. */
    static boolean klik(CastWezen w, ServerPlayer p) {
        if (!w.entityTags().contains(TAG)) {
            return false;
        }
        String eigenaar = w.getPersistentData().getStringOr("Eigenaar", "");
        int nr = w.getPersistentData().getIntOr("Nr", 0);
        if (!eigenaar.equals(p.getStringUUID()) || ln().stap(p) != 2 || nr < 1 || nr > KUDDE || ln().vlag(p, "kudde_" + nr)) {
            return true;
        }
        ZijPlek.Kopie kopie = ZijPlek.bij(p.level(), w.blockPosition(), S, 64);
        Vec3 naar = kopie == null ? null : kopie.midden("bizon_" + nr);
        if (naar == null) {
            return true;
        }
        ln().vlag(p, "kudde_" + nr, true);
        w.zetAnimatie("brul");
        w.level().playSound(null, w.blockPosition(), SoundEvents.CAMEL_AMBIENT, SoundSource.NEUTRAL, 1f, 0.6f);
        VLIEGT.put(w.getUUID(), new Vlucht(w.position(), naar, w.level().getGameTime()));
        int n = thuis(p);
        if (n >= KUDDE && ln().verder(p, 2)) {
            Zij.titel(p, net.minecraft.network.chat.Component.translatable(Q + "kudde_titel").withStyle(net.minecraft.ChatFormatting.AQUA), null, 4, 40, 10);
            Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.7f, 1.2f);
            Zij.stem(p, Q + "naam.luchtnon", Q + "kudde_klaar");
            Zij.hint(p, Q + "hint.terug");
        } else {
            Zij.melding(p, Q + "kudde_een", n, KUDDE);
        }
        return true;
    }

    /** Every 10 ticks per player at step 2 inside the temple: their own five bison are there (strays or at home). */
    static void tick(ServerPlayer p) {
        UUID id = p.getUUID();
        int stap = ln().stap(p);
        ZijPlek.Kopie kopie = stap == 2 ? ZijPlek.bij(p, S, 48) : null;
        if (kopie == null) {
            ruim(p.level(), id);
            return;
        }
        ServerLevel level = p.level();
        UUID[] eigen = EIGEN.computeIfAbsent(id, u -> new UUID[KUDDE]);
        for (int i = 1; i <= KUDDE; i++) {
            Entity e = eigen[i - 1] == null ? null : level.getEntity(eigen[i - 1]);
            if (e != null && e.isAlive()) {
                continue;
            }
            boolean thuis = ln().vlag(p, "kudde_" + i);
            Vec3 plek = kopie.midden(thuis ? "bizon_" + i : "kudde_" + i);
            if (plek == null || !level.isLoaded(BlockPos.containing(plek))) {
                continue;
            }
            CastWezen w = E2CastSchurkFeature.LUCHTBIZON_GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (w == null) {
                continue;
            }
            w.snapTo(plek.x, plek.y, plek.z, 90f * i, 0f);
            w.zetSoort(i - 1);
            w.zetStil(true);
            w.zetAnimatie(thuis ? "idle" : "vlieg");
            w.setNoGravity(true);
            w.setInvulnerable(true);
            w.addTag(TAG);
            w.getPersistentData().putString("Eigenaar", p.getStringUUID());
            w.getPersistentData().putInt("Nr", i);
            Zicht.alleenVoor(w, id);
            ZijActeurs.tijdelijk(w);
            level.addFreshEntity(w);
            eigen[i - 1] = w.getUUID();
        }
    }

    /** Every tick: the bison on their way home glide along an arc. */
    static void serverTick(ServerLevel level) {
        if (VLIEGT.isEmpty()) {
            return;
        }
        long nu = level.getGameTime();
        for (Map.Entry<UUID, Vlucht> e : List.copyOf(VLIEGT.entrySet())) {
            Entity ent = level.getEntity(e.getKey());
            if (ent == null) {
                continue;
            }
            Vlucht v = e.getValue();
            double f = Math.min(1.0, (nu - v.start()) / (double) VLUCHT_TICKS);
            double s = f * f * (3 - 2 * f);
            Vec3 pos = v.van().lerp(v.naar(), s).add(0, Math.sin(Math.PI * f) * 8, 0);
            Vec3 richting = v.naar().subtract(v.van());
            float yaw = (float) Math.toDegrees(Math.atan2(-richting.x, richting.z));
            ent.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
            if (ent instanceof CastWezen w && f >= 1.0) {
                w.zetAnimatie("idle");
                level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 12, 1, 0.3, 1, 0.02);
                VLIEGT.remove(e.getKey());
            } else if (nu % 5 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.6, pos.z, 2, 0.8, 0.2, 0.8, 0.01);
            }
        }
    }

    /** Takes a player's own bison away (they left, or their step changed). */
    static void ruim(ServerLevel level, UUID speler) {
        UUID[] eigen = EIGEN.remove(speler);
        if (eigen == null) {
            return;
        }
        for (UUID u : eigen) {
            if (u == null) {
                continue;
            }
            VLIEGT.remove(u);
            Entity e = level.getEntity(u);
            if (e != null) {
                e.discard();
            }
        }
    }

    /** (tests) the player's own bison that stand in the world now. */
    static List<Entity> eigen(ServerLevel level, UUID speler) {
        List<Entity> uit = new ArrayList<>();
        UUID[] eigen = EIGEN.get(speler);
        if (eigen != null) {
            for (UUID u : eigen) {
                Entity e = u == null ? null : level.getEntity(u);
                if (e != null) {
                    uit.add(e);
                }
            }
        }
        return uit;
    }

    @Nullable
    static UUID[] eigenIds(UUID speler) {
        return EIGEN.get(speler);
    }
}
