package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

/**
 * T-PUZZELS: puts a puzzle into the world. The world itself (the map tiles) is not touched by the generators: a puzzle is
 * stamped by code the first time a player comes within {@link Puzzels#STAMP_AFSTAND} blocks of its anchor, at the flattest
 * free spot within {@link #ZOEK} blocks of anchor + offset (no water, no logs/walls in the way, height difference at most
 * {@link #MAX_VERSCHIL}). The spot is remembered ({@link PuzzelData}); a moved anchor (world shift) or a changed layout
 * stamps it again (the old box is cleared first).
 * <p><b>Walkability:</b> a real floor under every cell (filled down to the ground), the whole box cleared of plants/leaves up to
 * {@code hoogte}, and on every side the walkable edge cell nearest the middle gets stairs out to the terrain when the
 * difference is more than 1 block (down or up, max 8 steps).
 */
public final class PuzzelStempel {
    static final int ZOEK = 24, MAX_VERSCHIL = 2, GEEN = Integer.MIN_VALUE;
    private static final Map<Integer, String> TRAP = Map.of(1, "minecraft:mossy_stone_brick_stairs", 2, "minecraft:polished_diorite_stairs", 3,
            "minecraft:spruce_stairs");

    private PuzzelStempel() {
    }

    /** Every second: stamp the puzzles that a player is near and that are not (or no longer correctly) placed. */
    static void tick(MinecraftServer server, ServerLevel rijk) {
        if (rijk.players().isEmpty()) {
            return;
        }
        PuzzelData data = PuzzelData.get(server);
        for (PuzzelDef d : Puzzels.defs()) {
            BlockPos anker = d.anker().punt();
            if (anker == null) {
                continue;
            }
            PuzzelData.Plek pl = data.plek(d.id());
            if (pl != null && pl.vingerafdruk() == vingerafdruk(d) && Puzzels.inst(d.id()) != null) {
                continue;
            }
            boolean dichtbij = false;
            for (ServerPlayer p : rijk.players()) {
                if (p.blockPosition().distSqr(anker) < (double) Puzzels.STAMP_AFSTAND * Puzzels.STAMP_AFSTAND) {
                    dichtbij = true;
                    break;
                }
            }
            if (dichtbij && geladen(rijk, anker, ZOEK + Math.max(d.breed(), d.diep()) + 10)) {
                plaats(rijk, d, false);
            }
        }
    }

    static boolean geladen(ServerLevel l, BlockPos m, int r) {
        return l.isLoaded(m.offset(-r, 0, -r)) && l.isLoaded(m.offset(r, 0, r)) && l.isLoaded(m.offset(-r, 0, r)) && l.isLoaded(m.offset(r, 0, -r));
    }

    /** Layout + anchor point: a change means "stamp again". */
    public static int vingerafdruk(PuzzelDef d) {
        return Objects.hash(d.anker().punt(), d.breed(), d.diep(), d.hoogte(), d.blokken(), d.elementen(), d.deur(), d.vloer());
    }

    /**
     * Finds the spot, (re)stamps and activates a world puzzle. {@code opnieuw}: forget the old spot and search again.
     * Returns the origin, or null (no anchor / not loaded).
     */
    @Nullable
    public static BlockPos plaats(ServerLevel l, PuzzelDef d, boolean opnieuw) {
        BlockPos anker = d.anker().punt();
        if (anker == null) {
            return null;
        }
        PuzzelData data = PuzzelData.get(l.getServer());
        PuzzelData.Plek oud = data.plek(d.id());
        BlockPos origin = null;
        if (oud != null) {
            boolean zelfde = oud.vingerafdruk() == vingerafdruk(d);
            if (zelfde && !opnieuw) {
                origin = oud.origin();
            } else {
                wis(l, oud);
            }
        }
        if (origin == null) {
            origin = zoek(l, d, anker);
        }
        stempel(l, d, origin);
        data.zet(d.id(), new PuzzelData.Plek(origin, d.breed(), d.diep(), d.hoogte(), vingerafdruk(d)));
        Puzzels.Inst in = Puzzels.activeer(d, l, origin, false);
        Puzzels.herstel(in);
        return origin;
    }

    /** Clears an old stamp (its box above the floor). */
    static void wis(ServerLevel l, PuzzelData.Plek p) {
        BlockState lucht = Blocks.AIR.defaultBlockState();
        for (int x = 0; x < p.breed(); x++) {
            for (int z = 0; z < p.diep(); z++) {
                for (int y = 0; y < p.hoogte(); y++) {
                    l.setBlock(p.origin().offset(x, y, z), lucht, 2);
                }
            }
        }
    }

    // ---- finding a spot ---------------------------------------------------------------------------------------------------------

    /**
     * The standing level at x/z: scanning down from {@code ref + 8} to {@code ref - 12}, the first solid (non-leaf) block with two
     * free blocks above it. GEEN = none (a wall, a trunk, water, a void).
     */
    static int grond(ServerLevel l, int x, int z, int ref) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = ref + 8; y >= ref - 12; y--) {
            m.set(x, y - 1, z);
            BlockState onder = l.getBlockState(m);
            if (!vast(l, m, onder)) {
                continue;
            }
            if (!onder.getFluidState().isEmpty()) {
                return GEEN;
            }
            m.set(x, y, z);
            BlockState a = l.getBlockState(m);
            m.set(x, y + 1, z);
            BlockState b = l.getBlockState(m);
            if (vrij(a) && vrij(b) && a.getFluidState().isEmpty()) {
                return y;
            }
            return GEEN;
        }
        return GEEN;
    }

    static boolean vast(ServerLevel l, BlockPos p, BlockState s) {
        return !s.isAir() && !s.is(BlockTags.LEAVES) && !s.getCollisionShape(l, p).isEmpty() || !s.getFluidState().isEmpty();
    }

    /** Free to stand in / to clear: air, plants, leaves, snow layers (not logs, walls, puzzle blocks, fluids). */
    static boolean vrij(BlockState s) {
        return s.isAir() || s.is(BlockTags.LEAVES) || s.canBeReplaced() && s.getFluidState().isEmpty();
    }

    static BlockPos zoek(ServerLevel l, PuzzelDef d, BlockPos anker) {
        int w = d.breed(), dp = d.diep();
        BlockPos beste = null;
        double besteScore = Double.MAX_VALUE;
        for (int r = 0; r <= ZOEK; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int cx = anker.getX() + dx - w / 2, cz = anker.getZ() + dz - dp / 2;
                    int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
                    boolean ok = true;
                    int[] alle = new int[(w + 2) * (dp + 2)];
                    int n = 0;
                    for (int x = -1; ok && x <= w; x++) {
                        for (int z = -1; ok && z <= dp; z++) {
                            int g = grond(l, cx + x, cz + z, anker.getY());
                            boolean rand = x < 0 || z < 0 || x >= w || z >= dp;
                            if (g == GEEN) {
                                ok = rand;   // (the ring around may be odd; the box itself must be ground)
                                continue;
                            }
                            if (!rand) {
                                lo = Math.min(lo, g);
                                hi = Math.max(hi, g);
                                alle[n++] = g;
                            }
                        }
                    }
                    if (!ok || n == 0 || hi - lo > MAX_VERSCHIL || !boxVrij(l, cx, cz, w, dp, lo, d.hoogte())) {
                        continue;
                    }
                    java.util.Arrays.sort(alle, 0, n);
                    int y = alle[n / 2];
                    double score = (hi - lo) * 40 + Math.sqrt(dx * dx + dz * dz) + Math.abs(y - anker.getY()) * 2;
                    if (score < besteScore) {
                        besteScore = score;
                        beste = new BlockPos(cx, y, cz);
                    }
                }
            }
            if (beste != null && besteScore < 40) {
                break;   // (a flat spot this close: good enough)
            }
        }
        if (beste == null) {
            // nothing flat nearby: stamp at the anchor itself, raised on its own floor (stairs connect it)
            int g = grond(l, anker.getX(), anker.getZ(), anker.getY());
            beste = new BlockPos(anker.getX() - w / 2, g == GEEN ? anker.getY() : g, anker.getZ() - dp / 2);
        }
        return beste;
    }

    /** No logs/walls/buildings inside the box above the lowest ground (leaves and plants are fine: they get cleared). */
    static boolean boxVrij(ServerLevel l, int x0, int z0, int w, int dp, int y0, int h) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < dp; z++) {
                for (int y = MAX_VERSCHIL + 1; y < h; y++) {
                    m.set(x0 + x, y0 + y, z0 + z);
                    if (!vrij(l.getBlockState(m))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // ---- stamping ----------------------------------------------------------------------------------------------------------

    /** Places the whole puzzle with its west-north standing corner at {@code o}. Also used by the gametests (no search). */
    public static void stempel(ServerLevel l, PuzzelDef d, BlockPos o) {
        BlockState vloer = blok(d.vloer()), vul = blok(d.vul()), lucht = Blocks.AIR.defaultBlockState();
        Map<Long, BlockState> anders = new java.util.HashMap<>();
        for (PuzzelDef.VloerCel c : d.vloerAnders()) {
            anders.put(BlockPos.asLong(c.x(), 0, c.z()), blok(c.blok()));
        }
        int flags = 2;
        for (int x = 0; x < d.breed(); x++) {
            for (int z = 0; z < d.diep(); z++) {
                for (int y = 0; y < d.hoogte(); y++) {
                    l.setBlock(o.offset(x, y, z), lucht, flags);
                }
                l.setBlock(o.offset(x, -1, z), anders.getOrDefault(BlockPos.asLong(x, 0, z), vloer), flags);
                for (int y = -2; y > -18; y--) {
                    BlockPos b = o.offset(x, y, z);
                    BlockState s = l.getBlockState(b);
                    if (vast(l, b, s) && s.getFluidState().isEmpty()) {
                        break;
                    }
                    l.setBlock(b, vul, flags);
                }
            }
        }
        // the ring around: plants/leaves on the first 3 levels go (no decoration in the way of the entrances)
        for (int x = -1; x <= d.breed(); x++) {
            for (int z = -1; z <= d.diep(); z++) {
                if (x >= 0 && z >= 0 && x < d.breed() && z < d.diep()) {
                    continue;
                }
                for (int y = 0; y < 3; y++) {
                    BlockPos b = o.offset(x, y, z);
                    BlockState s = l.getBlockState(b);
                    if (!s.isAir() && vrij(s)) {
                        l.setBlock(b, lucht, flags);
                    }
                }
            }
        }
        for (PuzzelDef.Blok b : d.blokken()) {
            l.setBlock(o.offset(b.x(), b.y(), b.z()), blok(b.blok()), flags);
        }
        for (PuzzelDef.Element e : d.elementen()) {
            l.setBlock(o.offset(e.lokaal()), Puzzels.staat(e.blok(), e.stand(), false), 3);
        }
        BlockState deur = Puzzels.staat("geheimdeur", 0, false);
        for (BlockPos b : d.deur()) {
            l.setBlock(o.offset(b), deur, flags);
        }
        aansluiten(l, d, o);
        // a player standing inside the new floor goes on top of it
        for (ServerPlayer p : l.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(o.getX() - 1, o.getY() - 4, o.getZ() - 1,
                o.getX() + d.breed() + 1, o.getY() + d.hoogte(), o.getZ() + d.diep() + 1))) {
            if (p.getY() < o.getY()) {
                p.teleportTo(p.getX(), o.getY(), p.getZ());
            }
        }
    }

    /** Stairs from the middle-most walkable edge cell of each side out to the terrain (when it differs more than 1 block). */
    static void aansluiten(ServerLevel l, PuzzelDef d, BlockPos o) {
        int w = d.breed(), dp = d.diep();
        java.util.Set<Long> bezet = new java.util.HashSet<>();
        for (PuzzelDef.Blok b : d.blokken()) {
            if (b.y() == 0 || b.y() == 1) {
                bezet.add(BlockPos.asLong(b.x(), 0, b.z()));
            }
        }
        for (PuzzelDef.Element e : d.elementen()) {
            if (e.y() == 0) {
                bezet.add(BlockPos.asLong(e.x(), 0, e.z()));
            }
        }
        for (BlockPos b : d.deur()) {
            bezet.add(BlockPos.asLong(b.getX(), 0, b.getZ()));
        }
        // N, O, Z, W sides: (cells along the side, the outward direction)
        int[][] uit = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int kant = 0; kant < 4; kant++) {
            int lengte = kant % 2 == 0 ? w : dp;
            int beste = -1, besteAfstand = Integer.MAX_VALUE;
            for (int i = 0; i < lengte; i++) {
                int x = kant == 1 ? w - 1 : kant == 3 ? 0 : i, z = kant == 0 ? 0 : kant == 2 ? dp - 1 : i;
                if (bezet.contains(BlockPos.asLong(x, 0, z))) {
                    continue;
                }
                int a = Math.abs(2 * i - (lengte - 1));
                if (a < besteAfstand) {
                    besteAfstand = a;
                    beste = i;
                }
            }
            if (beste < 0) {
                continue;
            }
            int x = kant == 1 ? w - 1 : kant == 3 ? 0 : beste, z = kant == 0 ? 0 : kant == 2 ? dp - 1 : beste;
            trap(l, d.zone(), o.offset(x, 0, z), uit[kant][0], uit[kant][1]);
        }
    }

    static void trap(ServerLevel l, int zone, BlockPos rand, int dx, int dz) {
        Direction naarBuiten = Direction.fromDelta(dx, 0, dz);
        if (naarBuiten == null) {
            return;
        }
        BlockState trede = blok(TRAP.getOrDefault(zone, "minecraft:mossy_stone_brick_stairs"));
        BlockState vul = Blocks.DIRT.defaultBlockState(), lucht = Blocks.AIR.defaultBlockState();
        int c = rand.getY();
        for (int i = 1; i <= 8; i++) {
            int x = rand.getX() + dx * i, z = rand.getZ() + dz * i;
            int g = grond(l, x, z, c);
            if (g == GEEN) {
                return;
            }
            if (Math.abs(g - c) <= 1) {
                return;   // connected
            }
            if (g < c) {
                // down: a step whose high side faces the puzzle, filled underneath
                BlockPos t = new BlockPos(x, c - 1, z);
                l.setBlock(t, trede.trySetValue(StairBlock.FACING, naarBuiten.getOpposite()), 2);
                for (int y = 0; y < 3; y++) {
                    l.setBlock(t.above(1 + y), lucht, 2);
                }
                for (int y = c - 2; y > g - 2; y--) {
                    l.setBlock(new BlockPos(x, y, z), vul, 2);
                }
                c--;
            } else {
                // up: carve a step into the slope
                BlockPos t = new BlockPos(x, c, z);
                l.setBlock(t, trede.trySetValue(StairBlock.FACING, naarBuiten), 2);
                for (int y = 1; y <= 3; y++) {
                    l.setBlock(t.above(y), lucht, 2);
                }
                c++;
            }
        }
    }

    /** "minecraft:azalea_leaves[persistent=true]" -> the state (unknown block = stone, unknown property = ignored). */
    static BlockState blok(String s) {
        String naam = s, props = "";
        int k = s.indexOf('[');
        if (k > 0) {
            naam = s.substring(0, k);
            props = s.substring(k + 1, s.length() - 1);
        }
        Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(naam));
        if (b == null || b == Blocks.AIR && !naam.endsWith(":air")) {
            b = Blocks.STONE;
        }
        BlockState st = b.defaultBlockState();
        if (!props.isEmpty()) {
            for (String kv : props.split(",")) {
                String[] p = kv.split("=");
                if (p.length == 2) {
                    st = metWaarde(st, p[0].trim(), p[1].trim());
                }
            }
        }
        return st;
    }

    private static <T extends Comparable<T>> BlockState zet(BlockState s, Property<T> p, String waarde) {
        return p.getValue(waarde).map(v -> s.setValue(p, v)).orElse(s);
    }

    private static BlockState metWaarde(BlockState s, String naam, String waarde) {
        Property<?> p = s.getBlock().getStateDefinition().getProperty(naam);
        return p == null ? s : zet(s, p, waarde);
    }

    /** Where to put a player who comes to look: just outside the middle of the south side. */
    public static Vec3 kijkPlek(BlockPos origin, PuzzelDef d) {
        return new Vec3(origin.getX() + d.breed() / 2.0 + 0.5, origin.getY(), origin.getZ() + d.diep() + 1.5);
    }
}
