package nl.juiced.guhs.feature.weer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.juiced.guhs.network.ModNetworking;

/**
 * 1.8.0: where the Weer vadsers make their weather. A zone is a circle around a Weer vadser of 10 blocks, 20 / 35 / 60 with
 * its Wolkenantenne ({@link #STRALEN}; a test zone {@link #STRAAL}) (or a
 * test zone of {@code /guhs weer zone}); outside every zone the world keeps its own weather. A running Weer vadser renews
 * its zone every tick it works ({@link #houdVast}); a zone that is not renewed for {@link #VERVAL} ticks is gone (the block
 * was broken, lost its vadskracht, or its chunk unloaded). The clients get the zones of their dimension
 * ({@link WeerPayloads.Zones}) whenever they change and when they log in or change dimension.
 * <p>
 * The API for other code: {@link #weerOp} (which weather is it here?) and {@link #regentOp} (does it rain on this spot? the
 * zones first, then the world's own weather). Both work on both sides (the client uses its synced copy).
 */
public final class WeerZones {
    /**
     * 1.8.0 (vadskracht): the radius of a zone (blocks, horizontally) per level of Wolkenantenne: 10 without one, then 20 / 35
     * / 60 with Wolkenantenne I / II / III. {@link #STRAAL} is the biggest (what code that looks around must cover).
     */
    public static final int[] STRALEN = {10, 20, 35, 60};
    public static final int STRAAL = 60, RAND = 8;

    /** The soft edge inside a zone of this radius where the weather eases out (8 for the biggest, at least 2). */
    public static int rand(int straal) {
        return Math.max(2, Math.round(straal * RAND / (float) STRAAL));
    }

    /** The radius that goes with this antenna level (0..3). */
    public static int straal(int niveau) {
        return STRALEN[Math.max(0, Math.min(STRALEN.length - 1, niveau))];
    }

    /**
     * The Wolkenantenne level of a weather block at pos: the antennas that went in ({@code ingebouwd}) or a Wolkenantenne
     * on top of it or right next to it, the highest wins.
     */
    public static int antenneNiveau(net.minecraft.world.level.Level level, BlockPos pos, int ingebouwd) {
        int niveau = ingebouwd;
        if (level != null) {
            niveau = Math.max(niveau, WeerFeature.antenneNiveau(level.getBlockState(pos.above())));
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                niveau = Math.max(niveau, WeerFeature.antenneNiveau(level.getBlockState(pos.relative(d))));
            }
        }
        return niveau;
    }

    /**
     * THE one place that says how far a weather block at pos reaches: 10 blocks, or more with Wolkenantennes
     * ({@link #antenneNiveau}). (Other radius hooks, such as a weather station's own radius, belong here too: the biggest wins.)
     */
    public static int straalVan(net.minecraft.world.level.Level level, BlockPos pos, int ingebouwd) {
        int straal = straal(antenneNiveau(level, pos, ingebouwd));
        if (level instanceof ServerLevel sl) {
            straal = Math.max(straal, STRAAL_VOOR.applyAsInt(sl, pos));
        }
        return straal;
    }
    /** A zone that is not renewed this many ticks is gone. */
    public static final int VERVAL = 40;

    /**
     * 1.8.0 (vadskracht): other features widen the radius of a running Weer vadser at this spot by wrapping this hook (the
     * Knabbel-weerstation of feature/koepel: a whole base). The biggest wins ({@link #straalVan}); 0 = no say.
     */
    public static java.util.function.ToIntBiFunction<ServerLevel, BlockPos> STRAAL_VOOR = (level, pos) -> 0;

    /** One zone: its centre, its weather, until when it holds (game time; Long.MAX_VALUE: a test zone) and its radius. */
    public record Zone(BlockPos midden, Weertje weertje, long tot, int straal) {
        public Zone(BlockPos midden, Weertje weertje, long tot) {
            this(midden, weertje, tot, STRAAL);
        }

        public boolean binnen(double x, double z) {
            double dx = x - (midden.getX() + 0.5), dz = z - (midden.getZ() + 0.5);
            return dx * dx + dz * dz <= (double) straal * straal;
        }

        /** 1 deep inside, easing to 0 at the edge. */
        public float gewicht(double x, double z) {
            double dx = x - (midden.getX() + 0.5), dz = z - (midden.getZ() + 0.5);
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d >= straal) {
                return 0f;
            }
            return (float) Math.min(1.0, (straal - d) / rand(straal));
        }
    }

    private static final Map<ResourceKey<Level>, Map<BlockPos, Zone>> ZONES = new HashMap<>();
    /** What every dimension's clients were last sent (a short fingerprint). */
    private static final Map<ResourceKey<Level>, String> VERSTUURD = new HashMap<>();

    private WeerZones() {
    }

    // =====================================================================================================================
    // server
    // =====================================================================================================================

    /** A running Weer vadser at pos keeps its zone (called every tick it works). */
    public static synchronized void houdVast(ServerLevel level, BlockPos pos, Weertje w, int straal) {
        ZONES.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(pos.immutable(), new Zone(pos.immutable(), w, level.getGameTime() + VERVAL, straal));
    }

    /** A test zone (no block, no vadskracht) at pos until {@link #wisTest}. */
    public static synchronized void testZone(ServerLevel level, BlockPos pos, Weertje w) {
        testZone(level, pos, w, STRAAL);
    }

    /** A test zone of this radius. */
    public static synchronized void testZone(ServerLevel level, BlockPos pos, Weertje w, int straal) {
        ZONES.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(pos.immutable(), new Zone(pos.immutable(), w, Long.MAX_VALUE, straal));
    }

    /** Removes the zone of a Weer vadser at pos at once (it was switched to "normaal" or broken). */
    public static synchronized void laatLos(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Zone> m = ZONES.get(level.dimension());
        if (m != null) {
            m.remove(pos);
        }
    }

    /** Removes every test zone of this dimension; returns how many. */
    public static synchronized int wisTest(ServerLevel level) {
        Map<BlockPos, Zone> m = ZONES.get(level.dimension());
        if (m == null) {
            return 0;
        }
        int voor = m.size();
        m.values().removeIf(z -> z.tot() == Long.MAX_VALUE);
        return voor - m.size();
    }

    /** Every zone of this dimension now. */
    public static synchronized List<Zone> zones(ServerLevel level) {
        Map<BlockPos, Zone> m = ZONES.get(level.dimension());
        return m == null ? List.of() : new ArrayList<>(m.values());
    }

    /** (Server tick) old zones out; the clients of a dimension hear it when its zones changed. */
    static synchronized void tick(ServerLevel level) {
        Map<BlockPos, Zone> m = ZONES.get(level.dimension());
        long nu = level.getGameTime();
        if (m != null) {
            m.values().removeIf(z -> z.tot() < nu);
        }
        String vinger = vingerafdruk(m);
        if (!vinger.equals(VERSTUURD.getOrDefault(level.dimension(), ""))) {
            VERSTUURD.put(level.dimension(), vinger);
            WeerPayloads.Zones p = payload(level);
            for (ServerPlayer sp : level.players()) {
                ModNetworking.sendTo(sp, p);
            }
        }
    }

    /** Sends a player the zones of the dimension they are in (login, dimension change). */
    static void stuur(ServerPlayer player) {
        ModNetworking.sendTo(player, payload(player.level()));
    }

    private static String vingerafdruk(@Nullable Map<BlockPos, Zone> m) {
        if (m == null || m.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        m.values().stream().sorted((a, b) -> Long.compare(a.midden().asLong(), b.midden().asLong()))
                .forEach(z -> sb.append(z.midden().asLong()).append(':').append(z.weertje().ordinal()).append(':').append(z.straal()).append(';'));
        return sb.toString();
    }

    private static synchronized WeerPayloads.Zones payload(ServerLevel level) {
        List<Long> plekken = new ArrayList<>();
        List<Integer> weertjes = new ArrayList<>();
        List<Integer> stralen = new ArrayList<>();
        for (Zone z : zones(level)) {
            plekken.add(z.midden().asLong());
            weertjes.add(z.weertje().ordinal());
            stralen.add(z.straal());
        }
        return new WeerPayloads.Zones(plekken, weertjes, stralen);
    }

    /** (Tests) forgets everything. */
    public static synchronized void wisAlles() {
        ZONES.clear();
        VERSTUURD.clear();
    }

    // =====================================================================================================================
    // the API (both sides)
    // =====================================================================================================================

    /** The zone that holds this spot (the nearest centre when zones overlap), or null: the world's own weather. */
    @Nullable
    public static Zone zoneOp(Level level, double x, double z) {
        List<Zone> lijst = level.isClientSide() ? Client.ZONES : zonesVan(level);
        Zone beste = null;
        double besteD = Double.MAX_VALUE;
        for (Zone zone : lijst) {
            if (zone.binnen(x, z)) {
                double dx = x - zone.midden().getX(), dz = z - zone.midden().getZ();
                double d = dx * dx + dz * dz;
                if (d < besteD) {
                    besteD = d;
                    beste = zone;
                }
            }
        }
        return beste;
    }

    private static synchronized List<Zone> zonesVan(Level level) {
        Map<BlockPos, Zone> m = ZONES.get(level.dimension());
        return m == null ? List.of() : new ArrayList<>(m.values());
    }

    /** Which weather is it at this spot? {@link Weertje#NORMAAL}: no zone (the world's own weather). */
    public static Weertje weerOp(Level level, BlockPos pos) {
        Zone z = zoneOp(level, pos.getX() + 0.5, pos.getZ() + 0.5);
        return z == null ? Weertje.NORMAAL : z.weertje();
    }

    /**
     * Does it rain (or snow) on this spot? In a zone: its weather decides (rain, thunderstorm and the rainbow weather rain;
     * everything else is dry), for spots under the open sky. Outside the zones: the world's own {@link Level#isRainingAt}.
     */
    public static boolean regentOp(Level level, BlockPos pos) {
        Weertje w = weerOp(level, pos);
        if (w == Weertje.NORMAAL) {
            return level.isRainingAt(pos);
        }
        return w.regent && level.canSeeSky(pos) && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() <= pos.getY();
    }

    /** The client's copy of the zones of its dimension. */
    public static final class Client {
        static final List<Zone> ZONES = new CopyOnWriteArrayList<>();

        public static void zet(List<Long> plekken, List<Integer> weertjes, List<Integer> stralen) {
            List<Zone> nieuw = new ArrayList<>();
            for (int i = 0; i < plekken.size() && i < weertjes.size(); i++) {
                int straal = i < stralen.size() ? stralen.get(i) : STRAAL;
                nieuw.add(new Zone(BlockPos.of(plekken.get(i)), Weertje.byIndex(weertjes.get(i)), Long.MAX_VALUE, straal));
            }
            ZONES.clear();
            ZONES.addAll(nieuw);
        }

        public static List<Zone> zones() {
            return ZONES;
        }

        public static void wis() {
            ZONES.clear();
        }

        private Client() {
        }
    }
}
