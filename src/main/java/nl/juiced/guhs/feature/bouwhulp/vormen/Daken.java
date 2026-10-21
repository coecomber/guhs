package nl.juiced.guhs.feature.bouwhulp.vormen;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;

/**
 * The dakmal (design 2.5): zadeldak, schilddak, tent-/kegeldak and the guh-oortjesdak, for a house of b × d (plus the
 * overhang on every side), in the stairs of the chosen material (its full block where it has no stairs). The gables of a
 * zadeldak are filled with the rand material when wanted. The roof's eaves are at y 0: put it on top of the walls.
 */
public final class Daken {
    public enum Dak {
        ZADEL, SCHILD, TENT, OORTJES;

        public static Dak van(int i) {
            return values()[Math.floorMod(i, values().length)];
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static Ontwerp maak(VormInstelling v, BouwNiveau niveau) {
        return Ontwerp.van(blokken(v.begrensd(niveau.maxVorm)));
    }

    public static Map<BlockPos, BlockState> blokken(VormInstelling v) {
        Block mat = v.wand().eerste(standaard());
        Block gevel = v.rand().eerste(Blocks.WHITE_TERRACOTTA);
        return blokken(v.dak(), v.b(), v.d(), v.overstek(), v.gevels(), mat, gevel, v.rand().isLeeg() ? Blocks.PINK_WOOL : gevel);
    }

    /** A roof of this kind over a house of b × d (walls included in b and d). */
    public static Map<BlockPos, BlockState> blokken(Dak dak, int b, int d, int overstek, boolean gevels, Block mat, Block gevel, Block oor) {
        int w = b + 2 * overstek, l = d + 2 * overstek;
        Familie f = Familie.van(mat);
        Map<BlockPos, BlockState> uit = switch (dak) {
            case ZADEL, OORTJES -> zadel(w, l, overstek, gevels, f, gevel, dak == Dak.OORTJES ? oor : null);
            case SCHILD -> schild(w, l, f);
            case TENT -> tent(w, l, f);
        };
        trapvormen(uit);
        return uit;
    }

    private static Block standaard() {
        Block b = Familie.blok("guhs:dakpan_roze");
        return b != null ? b : Blocks.SPRUCE_PLANKS;
    }

    /** A gable roof with the ridge along the longer side (along x when equal). */
    private static Map<BlockPos, BlockState> zadel(int w, int l, int o, boolean gevels, Familie f, Block gevel, Block oor) {
        boolean langsZ = l > w;
        int len = langsZ ? l : w, breed = langsZ ? w : l;
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        int top = (breed - 1) / 2;
        for (int a = 0; a < len; a++) {
            for (int s = 0; s < breed; s++) {
                int k = Math.min(s, breed - 1 - s);
                BlockState st;
                if (breed % 2 == 1 && s == top) {
                    st = f.plaat() != null ? f.plaat().defaultBlockState() : f.vol().defaultBlockState();
                } else if (f.trap() != null) {
                    st = f.trap().defaultBlockState().setValue(StairBlock.FACING, s < breed / 2.0 ? Direction.SOUTH : Direction.NORTH);
                } else {
                    st = f.vol().defaultBlockState();
                }
                zet(uit, langsZ, a, k, s, st);
                // the gables: under the roof, above the walls, at both ends of the house
                if (gevels && (a == o || a == len - 1 - o) && s >= o && s < breed - o) {
                    for (int y = 0; y < k; y++) {
                        zet(uit, langsZ, a, y, s, gevel.defaultBlockState());
                    }
                }
            }
        }
        if (oor != null) {
            int ty = breed % 2 == 1 ? top : top - 1;
            int mid = (breed - 1) / 2;
            for (int a : new int[] {Math.min(len - 1, o + 1), Math.max(0, len - 2 - o)}) {
                zet(uit, langsZ, a, ty + 1, mid, oor.defaultBlockState());
                zet(uit, langsZ, a, ty + 2, mid, oor.defaultBlockState());
                zet(uit, langsZ, a, ty + 3, mid, oor.defaultBlockState());
                if (mid > 0) {
                    zet(uit, langsZ, a, ty + 1, mid - 1, oor.defaultBlockState());
                }
                if (mid + 1 < breed) {
                    zet(uit, langsZ, a, ty + 1, mid + 1, oor.defaultBlockState());
                    if (breed % 2 == 0) {
                        zet(uit, langsZ, a, ty + 2, mid + 1, oor.defaultBlockState());
                        zet(uit, langsZ, a, ty + 3, mid + 1, oor.defaultBlockState());
                    }
                }
            }
        }
        return uit;
    }

    /** a = along the ridge, s = across: along x (s = z) or turned (along z: x = s, z = a; south becomes east). */
    private static void zet(Map<BlockPos, BlockState> uit, boolean langsZ, int a, int y, int s, BlockState st) {
        if (!langsZ) {
            uit.put(new BlockPos(a, y, s), st);
            return;
        }
        if (st.hasProperty(StairBlock.FACING)) {
            Direction f = st.getValue(StairBlock.FACING);
            st = st.setValue(StairBlock.FACING, f == Direction.SOUTH ? Direction.EAST : f == Direction.NORTH ? Direction.WEST : f);
        }
        uit.put(new BlockPos(s, y, a), st);
    }

    /** A hip roof: every side slopes in, the top a ridge (or a point). */
    private static Map<BlockPos, BlockState> schild(int w, int l, Familie f) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        int max = (Math.min(w, l) - 1) / 2;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < l; z++) {
                int dx0 = x, dx1 = w - 1 - x, dz0 = z, dz1 = l - 1 - z;
                int k = Math.min(Math.min(dx0, dx1), Math.min(dz0, dz1));
                BlockState st;
                boolean nok = k == max && (Math.min(w, l) % 2 == 1);
                if (nok || f.trap() == null) {
                    st = nok && f.plaat() != null ? f.plaat().defaultBlockState() : f.vol().defaultBlockState();
                } else {
                    Direction op = k == dz0 ? Direction.SOUTH : k == dz1 ? Direction.NORTH : k == dx0 ? Direction.EAST : Direction.WEST;
                    st = f.trap().defaultBlockState().setValue(StairBlock.FACING, op);
                }
                uit.put(new BlockPos(x, k, z), st);
            }
        }
        return uit;
    }

    /** A round, steep tent/cone roof: a ring of stairs per layer, a spire on top. */
    private static Map<BlockPos, BlockState> tent(int w, int l, Familie f) {
        Map<BlockPos, BlockState> uit = new LinkedHashMap<>();
        double cx = (w - 1) / 2.0, cz = (l - 1) / 2.0, r0 = Math.min(w, l) / 2.0;
        int lagen = (int) Math.ceil(r0) + 1;
        for (int y = 0; y < lagen; y++) {
            double r = r0 - y, binnen = r - 1.2;
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < l; z++) {
                    double dx = x - cx, dz = z - cz, a = Math.sqrt(dx * dx + dz * dz);
                    if (a <= r && a > binnen) {
                        BlockState st;
                        if (f.trap() == null || r < 1.5) {
                            st = f.vol().defaultBlockState();
                        } else {
                            Direction op = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                            st = f.trap().defaultBlockState().setValue(StairBlock.FACING, op);
                        }
                        uit.put(new BlockPos(x, y, z), st);
                    }
                }
            }
        }
        BlockPos spits = new BlockPos((int) Math.round(cx), lagen, (int) Math.round(cz));
        uit.put(spits, f.vol().defaultBlockState());
        uit.put(spits.above(), Blocks.LIGHTNING_ROD.defaultBlockState());
        return uit;
    }

    // =====================================================================================================================
    // stair corners: the shape vanilla gives a stair from its neighbours (StairBlock#getStairsShape), worked out on the
    // ontwerp itself, so hips and corners are right however the blocks get placed
    // =====================================================================================================================

    public static void trapvormen(Map<BlockPos, BlockState> m) {
        for (Map.Entry<BlockPos, BlockState> e : m.entrySet()) {
            BlockState s = e.getValue();
            if (s.getBlock() instanceof StairBlock) {
                e.setValue(s.setValue(StairBlock.SHAPE, vorm(s, m, e.getKey())));
            }
        }
    }

    private static StairsShape vorm(BlockState s, Map<BlockPos, BlockState> m, BlockPos p) {
        Direction dir = s.getValue(StairBlock.FACING);
        BlockState voor = m.get(p.relative(dir));
        if (trap(voor) && voor.getValue(StairBlock.HALF) == s.getValue(StairBlock.HALF)) {
            Direction d1 = voor.getValue(StairBlock.FACING);
            if (d1.getAxis() != dir.getAxis() && kan(s, m, p, d1.getOpposite())) {
                return d1 == dir.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
            }
        }
        BlockState achter = m.get(p.relative(dir.getOpposite()));
        if (trap(achter) && achter.getValue(StairBlock.HALF) == s.getValue(StairBlock.HALF)) {
            Direction d2 = achter.getValue(StairBlock.FACING);
            if (d2.getAxis() != dir.getAxis() && kan(s, m, p, d2)) {
                return d2 == dir.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
            }
        }
        return StairsShape.STRAIGHT;
    }

    private static boolean kan(BlockState s, Map<BlockPos, BlockState> m, BlockPos p, Direction kant) {
        BlockState n = m.get(p.relative(kant));
        return !trap(n) || n.getValue(StairBlock.FACING) != s.getValue(StairBlock.FACING) || n.getValue(StairBlock.HALF) != s.getValue(StairBlock.HALF);
    }

    private static boolean trap(BlockState s) {
        return s != null && s.getBlock() instanceof StairBlock;
    }

    /** (for the catalogue) a slab block of this family, bottom half. */
    static BlockState plaat(Familie f) {
        return f.plaat() != null ? f.plaat().defaultBlockState() : f.vol().defaultBlockState();
    }

    static boolean isPlaat(BlockState s) {
        return s.getBlock() instanceof SlabBlock;
    }

    private Daken() {
    }
}
