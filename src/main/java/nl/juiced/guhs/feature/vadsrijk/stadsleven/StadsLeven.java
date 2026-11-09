package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * U-LEVEN-STAD: the manager of town life in Het Vadsrijk (zones 1-6). Every half second it looks at the players who are in a
 * town ({@link Steden}) and keeps a few little scenes ({@link Groep}) around them, up to a cap per kind and town:
 * <ul>
 *   <li><b>Budget / LOD:</b> a scene only exists within its kind's radius ({@link Groep.Soort#zicht}, 34-64 blocks) of a player
 *       in that town, and goes the moment nobody is within that radius + 16. Caps per town ({@link Groep.Soort#max}; ZACHT =
 *       half), at most {@link #MAX_GUHS} town guhs and {@link #MAX_ENTITEITEN} entities in the whole dimension. Each player's
 *       client setting "Stadsleven: VOL / ZACHT / UIT" is sent to the server ({@link StadslevenPayloads}): UIT = nothing is
 *       spawned for, or sent to, that player; ZACHT = half.</li>
 *   <li><b>Never saved, never twice:</b> all entity types are noSave; a join guard only lets in what this manager is spawning
 *       right now; an entity that leaves its level without being removed is discarded next tick; every 5 s a sweep removes
 *       anything no scene owns. Unique scenes (a town's playground, cart, musician, a terrace bench) have one key each; spawning
 *       waits for {@link Dubbel#klaar} (u-dubbel: the chunk's entities are really there).</li>
 * </ul>
 * Dev: {@code /guhs vadsrijk leven status | aan | uit | niveau | spawn <soort> | tp <stad> | speeltuin <stad> | wis}.
 */
public final class StadsLeven {
    public enum Niveau {
        VOL, ZACHT, UIT
    }

    public static final String TAG = "guhs_vr_stadsleven";
    public static final int MAX_GUHS = 48, MAX_ENTITEITEN = 140;
    static boolean aan = true;
    /** Dev: a forced level for everybody (null = each player's own setting). */
    @Nullable
    static Niveau devNiveau;
    static final Set<UUID> EIGEN = new HashSet<>();
    static final List<Entity> WEG = new ArrayList<>();
    static final Map<String, Groep> GROEPEN = new LinkedHashMap<>();
    static final Map<UUID, Niveau> NIVEAU = new ConcurrentHashMap<>();
    private static final Map<String, Niveau> STAD_NIVEAU = new HashMap<>();
    private static int teller;

    private StadsLeven() {
    }

    // =====================================================================================================================
    // the players' setting
    // =====================================================================================================================

    static Niveau niveau(ServerPlayer p) {
        return devNiveau != null ? devNiveau : NIVEAU.getOrDefault(p.getUUID(), Niveau.VOL);
    }

    /** Does this player see town life (setting not UIT)? */
    static boolean ziet(ServerPlayer p) {
        return aan && niveau(p) != Niveau.UIT;
    }

    /** The richest setting of the players in a town right now. */
    static Niveau niveauIn(Steden.Stad stad) {
        return STAD_NIVEAU.getOrDefault(stad.id(), Niveau.VOL);
    }

    // =====================================================================================================================
    // ticking
    // =====================================================================================================================

    static void tick(MinecraftServer server) {
        ruimWegOp();
        if (testModus) {
            return;   // a gametest drives the manager itself
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        if (!aan) {
            if (!GROEPEN.isEmpty()) {
                allesWeg();
            }
            return;
        }
        tickGroepen(sl);
        long t = sl.getGameTime();
        if (t % 10 == 3) {
            List<Kijker> kijkers = new ArrayList<>();
            List<Vec3> ogen = new ArrayList<>();   // everybody who sees town life, also just outside a town
            for (ServerPlayer p : sl.players()) {
                if (p.isSpectator() || !ziet(p)) {
                    continue;
                }
                ogen.add(p.position());
                Steden.Stad s = Steden.op(p.position());
                if (s != null) {
                    kijkers.add(new Kijker(p.position(), s, niveau(p)));
                }
            }
            plan(sl, kijkers, ogen);
        }
        if (t % 100 == 50) {
            veeg(sl);
        }
    }

    /** Gametests: the server tick leaves the manager alone (the test calls {@link #tickGroepen} and {@link #plan}). */
    static boolean testModus;

    /** One tick of every scene (a broken or finished one is taken away). */
    static void tickGroepen(ServerLevel sl) {
        for (Iterator<Groep> it = GROEPEN.values().iterator(); it.hasNext();) {
            Groep g = it.next();
            boolean door;
            try {
                door = g.heel(sl) && g.tick(sl) && (g.duur < 0 || g.t <= g.duur + 20 * 60);
            } catch (RuntimeException e) {
                door = false;   // one broken scene never takes the server down
            }
            if (!door) {
                g.weg();
                it.remove();
            }
        }
    }

    /** A player in a town (position, town, setting). */
    record Kijker(Vec3 pos, Steden.Stad stad, Niveau niveau) {
    }

    /** The planning pass: scenes away that nobody is near, spot scans, new scenes under the caps. */
    static void plan(ServerLevel sl, List<Kijker> kijkers, List<Vec3> ogen) {
        STAD_NIVEAU.clear();
        for (Kijker k : kijkers) {
            Niveau oud = STAD_NIVEAU.get(k.stad().id());
            if (oud == null || k.niveau().ordinal() < oud.ordinal()) {
                STAD_NIVEAU.put(k.stad().id(), k.niveau());
            }
        }
        // away: scenes nobody (with Stadsleven on) is near any more
        for (Iterator<Groep> it = GROEPEN.values().iterator(); it.hasNext();) {
            Groep g = it.next();
            if (!iemandBij(ogen, g.midden(), g.soort.zicht + 16)) {
                g.weg();
                it.remove();
            }
        }
        if (kijkers.isEmpty()) {
            return;
        }
        // spot scans near the players (a few chunks per pass)
        Plekken.budget = 3;
        for (Kijker k : kijkers) {
            Plekken.verken(sl, k.pos(), 3);
        }
        // new scenes: per town, one try per kind per pass, while under the caps
        int guhs = 0, ents = 0;
        for (Groep g : GROEPEN.values()) {
            guhs += g.guhs();
            ents += g.leden.size();
        }
        RandomSource r = sl.getRandom();
        Map<String, List<Kijker>> perStad = new LinkedHashMap<>();
        for (Kijker k : kijkers) {
            perStad.computeIfAbsent(k.stad().id(), x -> new ArrayList<>()).add(k);
        }
        for (List<Kijker> ks : perStad.values()) {
            Steden.Stad stad = ks.get(0).stad();
            Niveau n = niveauIn(stad);
            Map<Groep.Soort, Integer> tel = tel(stad);
            for (Groep.Soort soort : Groep.Soort.values()) {
                if (ents >= MAX_ENTITEITEN) {
                    return;
                }
                if (tel.getOrDefault(soort, 0) >= soort.max(n)) {
                    continue;
                }
                if (guhs >= MAX_GUHS && soort != Groep.Soort.DIERTJE && soort != Groep.Soort.KAT && soort != Groep.Soort.EENDJES
                        && soort != Groep.Soort.VOGEL) {
                    continue;
                }
                Kijker k = ks.get(r.nextInt(ks.size()));
                if (!Dubbel.klaar(sl, BlockPos.containing(k.pos()))) {
                    continue;
                }
                Groep g = maak(sl, soort, stad, k.pos(), r, n);
                if (g != null) {
                    GROEPEN.put(g.sleutel, g);
                    guhs += g.guhs();
                    ents += g.leden.size();
                }
            }
        }
    }

    @Nullable
    static Groep maak(ServerLevel sl, Groep.Soort soort, Steden.Stad stad, Vec3 bij, RandomSource r, Niveau n) {
        return switch (soort) {
            case DIERTJE -> {   // W-LEVEN: mice and blobs each under their own cap (the mice times the town's multiplier)
                int[] mb = diertjes(stad);
                Boolean blob = Beestjes.volgendeIsBlob(stad, n, mb[1], mb[0], r);
                yield blob == null ? null : Beestjes.Diertje.maak(sl, stad, bij, r, blob);
            }
            case KAT -> Beestjes.Kat.maak(sl, stad, bij, r);
            case EENDJES -> Beestjes.Eendjes.maak(sl, stad, bij, r);
            case VOGEL -> Beestjes.Vogel.maak(sl, stad, bij, r);
            case WANDELAAR -> Straat.Wandelaar.maak(sl, stad, bij, r);
            case MUZIKANT -> Straat.Muzikant.maak(sl, stad, bij, r);
            case KAR -> Straat.Kar.maak(sl, stad, bij, r);
            case PICKNICK -> Straat.Picknick.maak(sl, stad, bij, r);
            case TIKKERTJE -> Straat.Tikkertje.maak(sl, stad, bij, r);
            case TERRAS -> Straat.Terras.maak(sl, stad, bij, r);
            case SPEELTUIN -> Speeltuin.maak(sl, stad, bij, r, n);
        };
    }

    static Map<Groep.Soort, Integer> tel(Steden.Stad stad) {
        Map<Groep.Soort, Integer> m = new EnumMap<>(Groep.Soort.class);
        for (Groep g : GROEPEN.values()) {
            if (g.stad == stad) {
                m.merge(g.soort, 1, Integer::sum);
            }
        }
        return m;
    }

    /** W-LEVEN: the town's critters now: {mice, blobs}. */
    static int[] diertjes(Steden.Stad stad) {
        int[] mb = new int[2];
        for (Groep g : GROEPEN.values()) {
            if (g.stad == stad && g instanceof Beestjes.Diertje d) {
                mb[d.blob ? 1 : 0]++;
            }
        }
        return mb;
    }

    private static boolean iemandBij(List<Vec3> ogen, Vec3 p, double r) {
        for (Vec3 q : ogen) {
            double dx = q.x - p.x, dz = q.z - p.z;
            if (dx * dx + dz * dz < r * r && Math.abs(q.y - p.y) < 40) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================================================================
    // helpers for the scenes
    // =====================================================================================================================

    static String nieuweSleutel(String soort) {
        return soort + "#" + (++teller);
    }

    static boolean heeft(String sleutel) {
        return GROEPEN.containsKey(sleutel);
    }

    /** Gametests: force day/night and dry/rain (null = the real world). */
    @Nullable
    static Boolean testNacht, testRegen;

    static boolean regen(ServerLevel sl) {
        return testRegen != null ? testRegen : sl.isRaining();
    }

    static boolean nacht(ServerLevel sl) {
        if (testNacht != null) {
            return testNacht;
        }
        return nl.juiced.guhs.feature.vadsrijk.ritme.Rooster.nacht(sl);   // V-INT: one day/night switch for the towns (u-leven-ritme)
    }

    /** No other town-life entity within r of p (scenes do not pile up on one spot). */
    static boolean vrij(Vec3 p, double r) {
        for (Groep g : GROEPEN.values()) {
            for (Entity e : g.leden) {
                if (e.distanceToSqr(p) < r * r) {
                    return false;
                }
            }
        }
        return true;
    }

    /** W-LEVEN: how many town-life entities are within r of p (the crowding check of {@link Plekken#magDruk}). */
    static int druk(Vec3 p, double r) {
        int n = 0;
        for (Groep g : GROEPEN.values()) {
            for (Entity e : g.leden) {
                if (e.distanceToSqr(p) < r * r) {
                    n++;
                }
            }
        }
        return n;
    }

    /** Bubbles showing right now in this town (the throttle). */
    static int bubbelsIn(Steden.Stad stad) {
        int n = 0;
        for (Groep g : GROEPEN.values()) {
            if (g.stad != stad) {
                continue;
            }
            for (Entity e : g.leden) {
                if (e instanceof LevenGuh l && l.praat() || e instanceof LevenProp p && p.isCustomNameVisible()) {
                    n++;
                }
            }
        }
        return n;
    }

    static void weg(@Nullable Entity e) {
        if (e != null) {
            EIGEN.remove(e.getUUID());
            if (!e.isRemoved()) {
                e.discard();
            }
        }
    }

    private static final GuhVariant[] VARIANTEN = {GuhVariant.NORMAL, GuhVariant.NORMAL, GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.CHOCO,
            GuhVariant.SNOW, GuhVariant.PLUISGUH, GuhVariant.BLOESEMGUH, GuhVariant.STARRY, GuhVariant.WOLK, GuhVariant.RAINBOW};
    private static final GuhClothes[] HOEDJES = {GuhClothes.ZONNEHOEDJE, GuhClothes.STRAW_HAT, GuhClothes.BLOESEMKRANSJE, GuhClothes.TUINHOEDJE,
            GuhClothes.THEEMUTSJE, GuhClothes.BOERDERIJ_HOEDJE, GuhClothes.PLUKMUTS, GuhClothes.BALLONPET};
    private static final GuhClothes[] KIND = {GuhClothes.PARTY_HAT, GuhClothes.OORSTRIKJE_ROZE, GuhClothes.OORSTRIKJE_MINT, GuhClothes.OORSTRIKJE_GEEL,
            GuhClothes.KAPSEL_STRIKJES, GuhClothes.KAPSEL_KNOTJES, GuhClothes.KAPSEL_PLUISBOL, GuhClothes.KAPSEL_KUIFJE, GuhClothes.SPELEN_ZWEETBANDJE};
    private static final GuhClothes[] SJAAL = {GuhClothes.KNUS_SJAALTJE, GuhClothes.VR_ITEM_VADSRIJKSJAAL, GuhClothes.KOORSTRIKJE, GuhClothes.RED_BOWTIE};

    /** A town guh (a child at {@link LevenGuh#KIND} scale), dressed a little after its zone. Never saved. */
    @Nullable
    static LevenGuh maakGuh(ServerLevel sl, Vec3 p, float yaw, RandomSource r, boolean kind, Steden.Stad stad) {
        LevenGuh g = StadslevenRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
        if (g == null) {
            return null;
        }
        g.setVariant(VARIANTEN[r.nextInt(VARIANTEN.length)]);
        if (kind) {
            g.maat(LevenGuh.KIND);
            g.wear(KIND[r.nextInt(KIND.length)]);
        } else {
            g.maat(0.85f + r.nextFloat() * 0.3f);
            switch (stad.zone()) {
                case 2 -> g.wear(r.nextBoolean() ? GuhClothes.WINTER_SCARF : GuhClothes.ELFTOCHT_SCHAATSMUTS);
                case 4 -> g.wear(r.nextBoolean() ? GuhClothes.VISSERSHOEDJE : GuhClothes.STRAW_HAT);
                case 5 -> g.wear(r.nextBoolean() ? GuhClothes.WOLKENMUTS : GuhClothes.BIEB_LEESBRIL);
                default -> {
                    if (r.nextInt(3) > 0) {
                        g.wear(HOEDJES[r.nextInt(HOEDJES.length)]);
                    }
                }
            }
            if (r.nextInt(3) == 0) {
                g.wear(SJAAL[r.nextInt(SJAAL.length)]);
            }
        }
        g.setCustomNameVisible(false);
        g.snapTo(p.x, p.y, p.z, yaw, 0);
        g.kijk(yaw);
        g.addTag(TAG);
        EIGEN.add(g.getUUID());
        if (!sl.addFreshEntity(g)) {
            EIGEN.remove(g.getUUID());
            return null;
        }
        return g;
    }

    // =====================================================================================================================
    // clicks
    // =====================================================================================================================

    private static final Map<UUID, Long> KLIK_PAUZE = new HashMap<>();

    static void klik(ServerPlayer p, LevenGuh g) {
        if (!pauze(p)) {
            return;
        }
        String pool = g.groep == null ? null : g.groep.klikRegel(g);
        RandomSource r = p.getRandom();
        Component regel = switch (pool == null ? "klets" : pool) {
            case "wandel" -> Babbels.regel("wandel", Babbels.WANDEL, r);
            case "muziek" -> Babbels.regel("muziek", Babbels.MUZIEK, r);
            case "picknick" -> Babbels.regel("picknick", Babbels.PICKNICK, r);
            case "kind" -> Babbels.regel("kind", Babbels.KIND, r);
            case "speel" -> Babbels.regel("kind", Babbels.KIND, r);
            default -> g.groep == null ? Babbels.regel("klets", Babbels.KLETS, r) : Babbels.kletsregel(g.groep.stad, r);
        };
        g.getLookControl().setLookAt(p);
        p.sendSystemMessage(Component.translatable("vadsrijk.stadsleven.zegt",
                Component.translatable(g.getScale() < 0.7f ? "vadsrijk.stadsleven.naam.kind" : "vadsrijk.stadsleven.naam.guh"), regel)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    static void klikKat(ServerPlayer p, LevenKat k) {
        if (!pauze(p)) {
            return;
        }
        ServerLevel sl = (ServerLevel) k.level();
        sl.sendParticles(ParticleTypes.HEART, k.getX(), k.getY() + 0.7, k.getZ(), 2, 0.2, 0.1, 0.2, 0);
        sl.playSound(null, k.getX(), k.getY(), k.getZ(), Beestjes.geluidMc("entity.cat.purreow"), SoundSource.NEUTRAL, 0.5f, 1.1f);
        p.sendSystemMessage(Babbels.regel("kat", Babbels.KAT, p.getRandom()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static boolean pauze(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Long tot = KLIK_PAUZE.get(p.getUUID());
        if (tot != null && nu < tot) {
            return false;
        }
        KLIK_PAUZE.put(p.getUUID(), nu + 20);
        return true;
    }

    // =====================================================================================================================
    // never saved, never twice
    // =====================================================================================================================

    static boolean isLeven(Entity e) {
        return e instanceof LevenGuh || e instanceof LevenProp || e instanceof LevenKat;
    }

    static boolean magErin(Entity e) {
        return !isLeven(e) || EIGEN.contains(e.getUUID());
    }

    static void verlaten(Entity e) {
        if (!isLeven(e)) {
            return;
        }
        EIGEN.remove(e.getUUID());
        if (!e.isRemoved()) {
            WEG.add(e);
        }
    }

    static void ruimWegOp() {
        if (WEG.isEmpty()) {
            return;
        }
        for (Entity e : List.copyOf(WEG)) {
            if (!e.isRemoved()) {
                e.discard();
            }
        }
        WEG.clear();
    }

    static boolean hoortBij(Entity e) {
        for (Groep g : GROEPEN.values()) {
            if (g.leden.contains(e)) {
                return true;
            }
        }
        return false;
    }

    /** The sweep: every town-life entity that no scene owns is discarded. */
    static int veeg(ServerLevel sl) {
        List<Entity> los = new ArrayList<>();
        for (Entity e : sl.getEntities(StadslevenRegistry.PROP.get(), e -> true)) {
            los.add(e);
        }
        for (Entity e : sl.getEntities(StadslevenRegistry.GUH.get(), e -> true)) {
            los.add(e);
        }
        for (Entity e : sl.getEntities(StadslevenRegistry.KAT.get(), e -> true)) {
            los.add(e);
        }
        int n = 0;
        for (Entity e : los) {
            if (!hoortBij(e)) {
                EIGEN.remove(e.getUUID());
                e.discard();
                n++;
            }
        }
        return n;
    }

    static void allesWeg() {
        for (Groep g : GROEPEN.values()) {
            g.weg();
        }
        GROEPEN.clear();
    }

    /** Server stopped: forget everything. */
    static void wis() {
        GROEPEN.clear();
        EIGEN.clear();
        WEG.clear();
        STAD_NIVEAU.clear();
        KLIK_PAUZE.clear();
        Plekken.wis();
    }

    /** Dev/tests: how many entities of each kind of scene. */
    static Map<Groep.Soort, int[]> stats() {
        Map<Groep.Soort, int[]> m = new EnumMap<>(Groep.Soort.class);
        for (Groep g : GROEPEN.values()) {
            int[] a = m.computeIfAbsent(g.soort, k -> new int[2]);
            a[0]++;
            a[1] += g.leden.size();
        }
        return m;
    }

    /** Dev: the playground of a town as a block (the corner). */
    @Nullable
    static BlockPos speeltuin(String stad) {
        return Speeltuin.van(stad);
    }
}
