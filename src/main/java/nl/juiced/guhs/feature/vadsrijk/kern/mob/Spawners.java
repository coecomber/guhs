package nl.juiced.guhs.feature.vadsrijk.kern.mob;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;

/**
 * Mob spawning in the dimension is code (no natural spawns): a {@link Spawner} keeps up to {@code max} of one mob type alive
 * in a circle while a player is near. Fix round 1 ("leven"):
 * <ul>
 * <li>a spawner is <b>active</b> while a player is within {@link #ACTIEF} blocks (flat; caves also inside their y-range,
 * surface spawners only for players above {@link #OPPERVLAK_MIN_Y}). Waking up after {@link #RUST_VOOR_VERS} ticks of rest
 * fills it up at once (a fresh field); after that every missing mob comes back {@code respawnTicks} after it went (a queue,
 * so 3 beaten mobs are back together, not one by one);</li>
 * <li>mobs of a resting spawner with no player within {@link #WEG} blocks are removed (despawn far away);</li>
 * <li>caps: {@code max} per spawner and {@link #GEBIED_MAX} per kern area (summed over the spawners);</li>
 * <li>never in a block, in the air, in fluid, on a mushroom cap/leaves/roof ({@link #slechteVloer}), within
 * {@link #VEILIG_STRAAL} of a {@link #VEILIGE_PLEKKEN} spot (campfires) or where a {@link #VERBODEN} test says no (NPCs), in a
 * town area, within {@link #MIN_SPELER} of a player, or (balans 2026-11) within {@link #PAD_VRIJ} of a path ({@link #PADEN},
 * registered by the zone slices from the map's path tables; only at the path's own height, so caves under a path and the hat
 * layer above one still spawn).</li>
 * </ul>
 * Elites roll with {@code eliteKans} and get 1-2 random affixes.
 */
public final class Spawners {
    /** A player within this many blocks (flat) wakes a spawner up. */
    public static final int ACTIEF = 80;
    /** A resting spawner's mobs vanish when no player is within this many blocks (flat). */
    public static final int WEG = 112;
    /** Never spawn closer to a player than this. */
    public static final int MIN_SPELER = 12;
    /** No spawning within this of a safe spot (campfires) or an NPC. */
    public static final int VEILIG_STRAAL = 12;
    /** Surface spawners only wake for players above this y (a player in the caves below does not fill the surface). */
    public static final int OPPERVLAK_MIN_Y = 58;
    /** Rested this long (ticks) = filled up at once when a player comes back. */
    public static final long RUST_VOOR_VERS = 600;

    /**
     * A spawner. Open air (default): the first good ground from the top (heightmap, then down past mushroom caps and roofs) in
     * a circle. Cave/interior mode ({@link #grot}): the first standing spot searching down from {@code maxY} to {@code minY}
     * (solid floor, 2 free blocks, no fluid), and every distance check is flat (2D) within that y-range, so a cave spawner
     * under a hill or a room inside a building works.
     */
    public record Spawner(String id, BlockPos centrum, int straal, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int max, int respawnTicks,
            int minLevel, int maxLevel, Element element, double eliteKans, boolean grot, int minY, int maxY) {
        /** Open air (the original 10-argument form). */
        public Spawner(String id, BlockPos centrum, int straal, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int max, int respawnTicks,
                int minLevel, int maxLevel, Element element, double eliteKans) {
            this(id, centrum, straal, type, max, respawnTicks, minLevel, maxLevel, element, eliteKans, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        /** The same spawner in cave/interior mode between these heights. */
        public Spawner grot(int minY, int maxY) {
            return new Spawner(id, centrum, straal, type, max, respawnTicks, minLevel, maxLevel, element, eliteKans, true, minY, maxY);
        }

        /** The box mobs of this spawner are counted in (leashed mobs stay well inside it). */
        public AABB gebied() {
            int r = straal + 40;
            if (grot) {
                return new AABB(centrum.getX() - r, minY - 6, centrum.getZ() - r, centrum.getX() + r + 1, maxY + 6, centrum.getZ() + r + 1);
            }
            return new AABB(centrum.getX() - r, 40, centrum.getZ() - r, centrum.getX() + r + 1, 220, centrum.getZ() + r + 1);
        }
    }

    private static final Map<String, Spawner> ALLE = new LinkedHashMap<>();
    /** Per spawner: the game times at which a missing mob comes back. */
    private static final Map<String, ArrayDeque<Long>> WACHTRIJ = new HashMap<>();
    /** Per spawner: the last game time it was active. */
    private static final Map<String, Long> LAATST_ACTIEF = new HashMap<>();
    /** Per spawner: its kern area id (cached). */
    private static final Map<String, String> GEBIED = new HashMap<>();

    /** A map path (flat polyline, world x/z) and its half width. */
    public record Pad(String naam, int[][] punten, double breedte) {
    }

    /** No wild spawn within this many blocks of a path's edge (balans 2026-11, second playtest). */
    public static final double PAD_VRIJ = 5;
    /** ... at the path's own height (stand y within this of the path surface next to it). */
    public static final int PAD_HOOGTE = 4;
    /** The paths of the zones (name -> path); the zone slices register theirs. */
    public static final Map<String, Pad> PADEN = new java.util.concurrent.ConcurrentHashMap<>();

    public static void registreerPad(String naam, int[][] punten, double breedte) {
        PADEN.put(naam, new Pad(naam, punten, breedte));
    }

    /**
     * The nearest path point to (x, z): {x, z, distance from the path's EDGE (centre distance - half width), dirX, dirZ of the
     * segment}, or null without paths.
     */
    @Nullable
    public static double[] dichtstbijPad(double x, double z) {
        double[] best = null;
        for (Pad pad : PADEN.values()) {
            int[][] p = pad.punten();
            for (int i = 0; i + 1 < p.length; i++) {
                double ax = p[i][0], az = p[i][1], dx = p[i + 1][0] - ax, dz = p[i + 1][1] - az;
                double L2 = dx * dx + dz * dz;
                if (L2 <= 0) {
                    continue;
                }
                double t = Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / L2));
                double px = ax + t * dx, pz = az + t * dz;
                double rand = Math.hypot(x - px, z - pz) - pad.breedte();
                if (best == null || rand < best[2]) {
                    double L = Math.sqrt(L2);
                    best = new double[] {px, pz, rand, dx / L, dz / L};
                }
            }
        }
        return best;
    }

    /** The flat distance from (x, z) to the nearest path edge (MAX_VALUE without paths; negative = on the path). */
    public static double padAfstand(double x, double z) {
        double[] d = dichtstbijPad(x, z);
        return d == null ? Double.MAX_VALUE : d[2];
    }

    /** Is this stand spot on or next to a path (within {@link #PAD_VRIJ} of its edge, at the path's height)? */
    public static boolean bijPad(ServerLevel sl, BlockPos p) {
        double[] d = dichtstbijPad(p.getX() + 0.5, p.getZ() + 0.5);
        if (d == null || d[2] >= PAD_VRIJ) {
            return false;
        }
        int px = (int) Math.floor(d[0]), pz = (int) Math.floor(d[1]);
        if (!sl.isLoaded(new BlockPos(px, 0, pz))) {
            return true;
        }
        int padY = grond(sl, px, pz);
        return padY == Integer.MIN_VALUE || Math.abs(p.getY() - padY) <= PAD_HOOGTE;
    }

    /** Max alive per kern area id (summed over all its spawners); no entry = no area cap. */
    public static final Map<String, Integer> GEBIED_MAX = new HashMap<>();
    /** Spots nothing spawns near ({@link #VEILIG_STRAAL}, flat, within 16 up/down): the campfires. */
    public static final List<BlockPos> VEILIGE_PLEKKEN = new CopyOnWriteArrayList<>();
    /** Extra "not here" tests (true = forbidden), e.g. an NPC nearby. */
    public static final List<BiPredicate<ServerLevel, BlockPos>> VERBODEN = new CopyOnWriteArrayList<>();

    /** The numbers of the last tick (dev command / notes). */
    public static int laatstActief, laatstLevend, laatstGespawnd, laatstWeg;
    public static final Map<String, Integer> LAATST_PER_GEBIED = new LinkedHashMap<>();

    private Spawners() {
    }

    public static synchronized void registreer(Spawner s) {
        ALLE.put(s.id(), s);
        GEBIED.remove(s.id());
    }

    @Nullable
    public static Spawner van(String id) {
        return ALLE.get(id);
    }

    public static java.util.Collection<Spawner> alle() {
        return java.util.Collections.unmodifiableCollection(ALLE.values());
    }

    /** The kern area id of a spawner ("" = none). */
    public static String gebiedVan(Spawner s) {
        return GEBIED.computeIfAbsent(s.id(), k -> {
            Gebieden.Gebied g = Gebieden.op(s.centrum());
            return g == null ? "" : g.id();
        });
    }

    /** How many (not yet plofd) mobs of this spawner are alive in its area now. */
    public static int levend(ServerLevel sl, Spawner s) {
        return mobs(sl, s).size();
    }

    static List<VadsrijkMob> mobs(ServerLevel sl, Spawner s) {
        return sl.getEntitiesOfClass(VadsrijkMob.class, s.gebied(), m -> s.id().equals(m.spawner) && !m.geplofd());
    }

    /** Is a (non-spectator) player near: within {@link #ACTIEF} blocks flat, inside the y-range for a cave spawner. */
    static boolean spelerInBuurt(ServerLevel sl, Spawner s) {
        return spelerBinnen(sl, s, ACTIEF);
    }

    private static boolean spelerBinnen(ServerLevel sl, Spawner s, int afstand) {
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator()) {
                continue;
            }
            double dx = p.getX() - s.centrum().getX(), dz = p.getZ() - s.centrum().getZ();
            if (dx * dx + dz * dz > (double) afstand * afstand) {
                continue;
            }
            if (s.grot() ? p.getY() >= s.minY() - 10 && p.getY() <= s.maxY() + 10 : p.getY() >= OPPERVLAK_MIN_Y) {
                return true;
            }
        }
        return false;
    }

    /** The flat distance² from this point to the nearest non-spectator player (MAX_VALUE without one). */
    static double spelerAfstand2(ServerLevel sl, double x, double z) {
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator()) {
                double dx = p.getX() - x, dz = p.getZ() - z;
                best = Math.min(best, dx * dx + dz * dz);
            }
        }
        return best;
    }

    /** Every second (kern). */
    public static void tick(ServerLevel sl) {
        long nu = sl.getGameTime();
        int actief = 0, gespawnd = 0, weg = 0, totaal = 0;
        Map<String, Integer> perGebied = new HashMap<>();
        List<Spawner> wakker = new ArrayList<>();
        Map<String, List<VadsrijkMob>> levendPer = new HashMap<>();
        for (Spawner s : ALLE.values()) {
            if (!sl.isLoaded(s.centrum())) {
                continue;
            }
            boolean wakkerNu = spelerInBuurt(sl, s);
            List<VadsrijkMob> m = mobs(sl, s);
            if (!wakkerNu) {
                // resting: its mobs far from every player vanish (not while they are fighting someone)
                for (VadsrijkMob mob : m) {
                    if (mob.getTarget() == null && spelerAfstand2(sl, mob.getX(), mob.getZ()) > (double) WEG * WEG) {
                        mob.discard();
                        weg++;
                    }
                }
                continue;
            }
            // over the cap (e.g. a chunk came back with its old mobs): the extras that are not fighting go
            if (m.size() > s.max()) {
                for (VadsrijkMob mob : new ArrayList<>(m)) {
                    if (m.size() <= s.max()) {
                        break;
                    }
                    if (mob.getTarget() == null) {
                        mob.discard();
                        m.remove(mob);
                        weg++;
                    }
                }
            }
            wakker.add(s);
            levendPer.put(s.id(), m);
            perGebied.merge(gebiedVan(s), m.size(), Integer::sum);
            totaal += m.size();
        }
        for (Spawner s : wakker) {
            actief++;
            String g = gebiedVan(s);
            int ruimte = GEBIED_MAX.getOrDefault(g, Integer.MAX_VALUE) - perGebied.getOrDefault(g, 0);
            int n = stap(sl, s, levendPer.get(s.id()).size(), nu, ruimte);
            if (n > 0) {
                perGebied.merge(g, n, Integer::sum);
                gespawnd += n;
                totaal += n;
            }
            LAATST_ACTIEF.put(s.id(), nu);
        }
        laatstActief = actief;
        laatstLevend = totaal;
        laatstGespawnd = gespawnd;
        laatstWeg = weg;
        LAATST_PER_GEBIED.clear();
        perGebied.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> LAATST_PER_GEBIED.put(e.getKey(), e.getValue()));
    }

    /**
     * One step of an awake spawner with {@code levend} mobs alive: fresh (rested long enough) = fill up at once; otherwise
     * queue a respawn for every newly missing mob and spawn the ones whose time has come. At most {@code ruimte} new ones
     * (the area cap). Returns how many it spawned.
     */
    public static int stap(ServerLevel sl, Spawner s, int levend, long nu, int ruimte) {
        ArrayDeque<Long> rij = WACHTRIJ.computeIfAbsent(s.id(), k -> new ArrayDeque<>());
        Long laatst = LAATST_ACTIEF.get(s.id());
        boolean vers = laatst == null || nu - laatst > RUST_VOOR_VERS;
        int tekort = s.max() - levend;
        if (tekort <= 0) {
            rij.clear();
            return 0;
        }
        int klaar;
        if (vers) {
            rij.clear();
            klaar = tekort;
        } else {
            while (rij.size() > tekort) {
                rij.pollLast();
            }
            while (rij.size() < tekort) {
                rij.addLast(nu + s.respawnTicks());
            }
            klaar = 0;
            for (Long t : rij) {
                if (t <= nu) {
                    klaar++;
                }
            }
        }
        int gedaan = 0;
        for (int i = 0; i < klaar && gedaan < ruimte; i++) {
            if (spawn(sl, s) != null) {
                gedaan++;
                if (!rij.isEmpty() && rij.peekFirst() <= nu) {
                    rij.pollFirst();
                }
            }
        }
        LAATST_ACTIEF.put(s.id(), nu);
        return gedaan;
    }

    /** Forget the wake-up state of every spawner (tests, the dev command). */
    public static void vergeetToestand() {
        WACHTRIJ.clear();
        LAATST_ACTIEF.clear();
    }

    public static VadsrijkMob spawn(ServerLevel sl, Spawner s) {
        RandomSource r = sl.getRandom();
        for (int poging = 0; poging < 12; poging++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * s.straal();
            int x = s.centrum().getX() + (int) Math.round(Math.cos(a) * d), z = s.centrum().getZ() + (int) Math.round(Math.sin(a) * d);
            if (!sl.isLoaded(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = s.grot() ? vloer(sl, x, z, s.minY(), s.maxY()) : grond(sl, x, z);
            if (y == Integer.MIN_VALUE || y <= sl.getMinY() + 1 || !sl.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                continue;
            }
            BlockPos plek = new BlockPos(x, y, z);
            if (!magHier(sl, plek)) {
                continue;
            }
            VadsrijkMob m = s.type().get().create(sl, EntitySpawnReason.SPAWNER);
            if (m == null) {
                return null;
            }
            boolean elite = r.nextDouble() < s.eliteKans();
            Set<Affix> af = EnumSet.noneOf(Affix.class);
            if (elite) {
                Affix[] alle = Affix.values();
                af.add(alle[r.nextInt(alle.length)]);
                if (r.nextBoolean()) {
                    af.add(alle[r.nextInt(alle.length)]);
                }
            }
            int lvl = s.minLevel() + (s.maxLevel() > s.minLevel() ? r.nextInt(s.maxLevel() - s.minLevel() + 1) : 0);
            m.snapTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360, 0);
            if (!sl.noCollision(m)) {
                continue;
            }
            m.stelIn(lvl, s.element(), elite, af);
            m.spawner = s.id();
            sl.addFreshEntity(m);
            return m;
        }
        return null;
    }

    /** May a spawner put a mob at this stand position? (Safe spots, NPCs, towns, players.) */
    public static boolean magHier(ServerLevel sl, BlockPos p) {
        for (BlockPos v : VEILIGE_PLEKKEN) {
            double dx = v.getX() - p.getX(), dz = v.getZ() - p.getZ();
            if (dx * dx + dz * dz <= VEILIG_STRAAL * VEILIG_STRAAL && Math.abs(v.getY() - p.getY()) <= 16) {
                return false;
            }
        }
        for (BiPredicate<ServerLevel, BlockPos> t : VERBODEN) {
            if (t.test(sl, p)) {
                return false;
            }
        }
        if (nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.in(sl)) {
            Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
            if (g != null && g.stad()) {
                return false;
            }
            if (bijPad(sl, p)) {
                return false;
            }
        }
        return spelerAfstand2(sl, p.getX() + 0.5, p.getZ() + 0.5) > MIN_SPELER * MIN_SPELER;
    }

    /**
     * Open-air ground: from the heightmap down, the first standing spot on real ground (not a mushroom cap, leaves, a roof...):
     * under a giant mushroom that is the forest floor below its cap. MIN_VALUE when nothing within 48 below the top.
     */
    static int grond(ServerLevel sl, int x, int z) {
        int top = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int onder = Math.max(sl.getMinY() + 1, top - 48);
        for (int y = top; y >= onder; y--) {
            m.set(x, y - 1, z);
            BlockState vl = sl.getBlockState(m);
            if (vl.getCollisionShape(sl, m).isEmpty() || !sl.getFluidState(m).isEmpty()) {
                continue;
            }
            m.set(x, y, z);
            BlockState b1 = sl.getBlockState(m);
            boolean vrij1 = b1.getCollisionShape(sl, m).isEmpty() && sl.getFluidState(m).isEmpty();
            m.set(x, y + 1, z);
            boolean vrij2 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty();
            if (vrij1 && vrij2 && !slechteVloer(vl)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** Not ground to stand a wild mob on: mushroom caps/stems, leaves, logs, roofs, glass, fences. */
    public static boolean slechteVloer(BlockState s) {
        if (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(BlockTags.PLANKS) || s.is(BlockTags.FENCES) || s.is(BlockTags.WALLS)
                || s.is(BlockTags.SLABS) || s.is(BlockTags.STAIRS) || s.is(BlockTags.WOOL) || s.is(BlockTags.CAMPFIRES)) {
            return true;
        }
        String n = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return n.contains("hoed") || n.contains("steel") || n.contains("mushroom") || n.contains("froglight") || n.contains("shroomlight")
                || n.contains("glass") || n.contains("barrier") || n.contains("bladeren") || n.contains("dak") || n.contains("wolk");
    }

    /** The first standing spot searching down from maxY to minY (solid floor, 2 free blocks, no fluid), or MIN_VALUE. */
    static int vloer(ServerLevel sl, int x, int z, int minY, int maxY) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int boven = Math.min(maxY, sl.getMaxY() - 2), onder = Math.max(minY, sl.getMinY() + 1);
        for (int y = boven; y >= onder; y--) {
            m.set(x, y - 1, z);
            if (sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() || !sl.getFluidState(m).isEmpty()) {
                continue;
            }
            m.set(x, y, z);
            boolean vrij1 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() && sl.getFluidState(m).isEmpty();
            m.set(x, y + 1, z);
            boolean vrij2 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty();
            if (vrij1 && vrij2) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
