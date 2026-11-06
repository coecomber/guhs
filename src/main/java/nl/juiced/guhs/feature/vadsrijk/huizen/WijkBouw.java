package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * U-HUIZEN: builds the streets and the square of a ward once (the grass comes from the flat generator). Then every plot gets its
 * "te koop" sign or its house ({@link Ontwerp}). All connecting blocks go through the same connection pass as the houses.
 * <ul>
 *   <li>the square: pink/white paving, a quartz fountain, three market stalls (west, Meubelmeester Mollie in the middle one), benches
 *   (east), the notice board and the pink gate back to the town (north), lamp posts on the corners;</li>
 *   <li>four streets with sidewalks and lamp posts between the plots, a hedge at the end; four little parks between the streets.</li>
 * </ul>
 */
public final class WijkBouw {
    /** Relative to the ward centre: the notice board box and Mollie's spot. */
    public static final int BORD_X0 = 3, BORD_X1 = 7, BORD_Z = -10;
    public static final BlockPos MOLLIE = new BlockPos(-10, WijkPlan.VLOER, 0);

    private WijkBouw() {
    }

    /** Writes world blocks, remembers them for the connection pass. */
    static final class Schrijver {
        final ServerLevel level;
        final BlockPos m;
        final Map<BlockPos, BlockState> geschreven = new LinkedHashMap<>();

        Schrijver(ServerLevel level, BlockPos m) {
            this.level = level;
            this.m = m;
        }

        void zet(int x, int y, int z, BlockState s) {
            BlockPos p = new BlockPos(m.getX() + x, y, m.getZ() + z);
            level.setBlock(p, s, Block.UPDATE_ALL);
            geschreven.put(p, s);
        }

        void zet(int x, int y, int z, Block b) {
            zet(x, y, z, b.defaultBlockState());
        }

        void verbind() {
            for (BlockPos p : geschreven.keySet()) {
                BlockState nu = level.getBlockState(p);
                if (!nu.isAir() && !nu.getFluidState().isSource()) {
                    BlockState goed = Block.updateFromNeighbourShapes(nu, level, p);
                    if (goed != nu) {
                        level.setBlock(p, goed, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }

        void lantaarnpaal(int x, int z) {
            // V-INT: u-deco's guhlantaarnpaal (2 blocks) instead of the cherry fence + lantern (fallback when the block is missing)
            Block paal = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(nl.juiced.guhs.Guhs.id("guh_lantaarnpaal"));
            if (paal instanceof nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Lantaarnpaal) {
                var half = nl.juiced.guhs.feature.bouwhulp.blokken.DetailBlokken.Lantaarnpaal.HALF;
                zet(x, WijkPlan.VLOER, z, paal.defaultBlockState().setValue(half, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
                zet(x, WijkPlan.VLOER + 1, z, paal.defaultBlockState().setValue(half, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
                return;
            }
            for (int y = WijkPlan.VLOER; y < WijkPlan.VLOER + 3; y++) {
                zet(x, y, z, Blocks.CHERRY_FENCE);
            }
            zet(x, WijkPlan.VLOER + 3, z, Blocks.LANTERN);
        }

        void bord(int x, int y, int z, Direction kant, String... regels) {
            zet(x, y, z, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, kant));
            BlockPos p = new BlockPos(m.getX() + x, y, m.getZ() + z);
            if (level.getBlockEntity(p) instanceof SignBlockEntity sign) {
                SignText t = new SignText();
                for (int i = 0; i < regels.length && i < 4; i++) {
                    t = t.setMessage(i, Component.translatable(regels[i]));
                }
                sign.setText(t, true);
                sign.setWaxed(true);
                sign.setChanged();
                level.sendBlockUpdated(p, sign.getBlockState(), sign.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Builds ward {@code wijk}: square, streets, parks, and every plot (free: sign; sold: the house in {@code data}). */
    public static void bouw(ServerLevel level, int wijk, HuizenData data) {
        BlockPos m = WijkPlan.midden(wijk);
        Schrijver w = new Schrijver(level, m);
        RandomSource r = RandomSource.create(wijk * 977L + 5);
        int g = WijkPlan.GROND, y = WijkPlan.VLOER, P = WijkPlan.PLEIN;

        // ---- the square ---------------------------------------------------------------------------------------------------------
        for (int x = -P; x <= P; x++) {
            for (int z = -P; z <= P; z++) {
                boolean rand = Math.max(Math.abs(x), Math.abs(z)) == P;
                w.zet(x, g, z, rand || (Math.abs(x) + Math.abs(z)) % 4 == 0 ? Blocks.PINK_TERRACOTTA : Blocks.POLISHED_DIORITE);
            }
        }
        // fountain
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) == 3) {
                    w.zet(x, y, z, Blocks.SMOOTH_QUARTZ);
                } else {
                    w.zet(x, g, z, Blocks.QUARTZ_BLOCK);
                    if (x != 0 || z != 0) {
                        w.zet(x, y, z, Blocks.WATER);
                    }
                }
            }
        }
        w.zet(0, y, 0, Blocks.QUARTZ_PILLAR);
        w.zet(0, y + 1, 0, Blocks.QUARTZ_PILLAR);
        w.zet(0, y + 2, 0, Blocks.WATER);
        // the gate back to the town (north side)
        for (int yy = y; yy <= y + 3; yy++) {
            w.zet(-2, yy, -10, Blocks.QUARTZ_PILLAR);
            w.zet(2, yy, -10, Blocks.QUARTZ_PILLAR);
        }
        for (int x = -2; x <= 2; x++) {
            w.zet(x, y + 4, -10, Blocks.PINK_TERRACOTTA);
            w.zet(x, g, -10, Blocks.PINK_CONCRETE);
        }
        for (int x = -3; x <= 3; x++) {
            w.zet(x, y + 5, -10, Blocks.CHERRY_SLAB);
        }
        w.zet(-3, y + 4, -10, Blocks.CHERRY_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF,
                net.minecraft.world.level.block.state.properties.Half.TOP));
        w.zet(3, y + 4, -10, Blocks.CHERRY_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF,
                net.minecraft.world.level.block.state.properties.Half.TOP));
        w.bord(0, y + 4, -9, Direction.SOUTH, "vadsrijk.huizen.poort.bord1", "vadsrijk.huizen.poort.bord2");
        // the notice board
        for (int yy = y; yy <= y + 1; yy++) {
            w.zet(BORD_X0 + 1, yy, BORD_Z, Blocks.SPRUCE_FENCE);
            w.zet(BORD_X1 - 1, yy, BORD_Z, Blocks.SPRUCE_FENCE);
        }
        for (int x = BORD_X0 + 1; x <= BORD_X1 - 1; x++) {
            w.zet(x, y + 2, BORD_Z, Blocks.SPRUCE_PLANKS);
            w.zet(x, y + 3, BORD_Z, Blocks.SPRUCE_PLANKS);
        }
        for (int x = BORD_X0; x <= BORD_X1; x++) {
            w.zet(x, y + 4, BORD_Z, Blocks.SPRUCE_SLAB);
        }
        w.bord((BORD_X0 + BORD_X1) / 2, y + 3, BORD_Z + 1, Direction.SOUTH, "vadsrijk.huizen.prikbord.bord1", "vadsrijk.huizen.prikbord.bord2",
                "", "vadsrijk.huizen.bord.klik");
        // market stalls (west side)
        Block[] waren = {Blocks.POTTED_PINK_TULIP, Blocks.POTTED_ALLIUM, Blocks.POTTED_AZALEA};
        int i = 0;
        for (int zc : new int[] {-5, 0, 5}) {
            for (int z = zc - 1; z <= zc + 1; z++) {
                w.zet(-9, y, z, Blocks.STRIPPED_CHERRY_WOOD);
                for (int x = -11; x <= -9; x++) {
                    w.zet(x, y + 3, z, (x + z) % 2 == 0 ? Blocks.PINK_WOOL : Blocks.WHITE_WOOL);
                }
            }
            for (int z : new int[] {zc - 1, zc + 1}) {
                w.zet(-9, y + 1, z, Blocks.CHERRY_FENCE);
                w.zet(-9, y + 2, z, Blocks.CHERRY_FENCE);
                for (int yy = y; yy <= y + 2; yy++) {
                    w.zet(-11, yy, z, Blocks.CHERRY_FENCE);
                }
            }
            w.zet(-9, y + 1, zc, waren[i++ % waren.length]);
            w.zet(-10, y + 2, zc, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        // benches (east side), lamp posts on the corners
        for (int z : new int[] {-3, -2, 2, 3}) {
            w.zet(9, y, z, Blocks.CHERRY_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        }
        w.zet(9, y, 0, Blocks.FLOWERING_AZALEA);
        for (int[] c : new int[][] {{-P + 1, -P + 1}, {P - 1, -P + 1}, {-P + 1, P - 1}, {P - 1, P - 1}}) {
            w.lantaarnpaal(c[0], c[1]);
        }

        // ---- streets ------------------------------------------------------------------------------------------------------------
        for (Direction o : WijkPlan.ARMEN) {
            Direction s = o.getClockWise();
            for (int a = WijkPlan.STRAAT_BEGIN; a <= WijkPlan.STRAAT_EIND + 1; a++) {
                for (int z = -3; z <= 3; z++) {
                    int x0 = a * o.getStepX() + z * s.getStepX(), z0 = a * o.getStepZ() + z * s.getStepZ();
                    if (a > WijkPlan.STRAAT_EIND) {
                        w.zet(x0, y, z0, Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                        continue;
                    }
                    Block b = Math.abs(z) == 3 ? Blocks.PINK_TERRACOTTA : Math.abs(z) == 2 ? Blocks.POLISHED_ANDESITE : Blocks.POLISHED_GRANITE;
                    w.zet(x0, g, z0, b);
                }
            }
            for (int a = WijkPlan.KAVEL_BEGIN - 1; a <= WijkPlan.STRAAT_EIND; a += WijkPlan.STAP) {
                for (int z : new int[] {-3, 3}) {
                    w.lantaarnpaal(a * o.getStepX() + z * s.getStepX(), a * o.getStepZ() + z * s.getStepZ());
                }
            }
            // a street name sign at the start
            w.bord(WijkPlan.STRAAT_BEGIN * o.getStepX() + 4 * s.getStepX(), y + 1, WijkPlan.STRAAT_BEGIN * o.getStepZ() + 4 * s.getStepZ(),
                    s.getOpposite(), "vadsrijk.huizen.straat." + o.getName());
            w.zet(WijkPlan.STRAAT_BEGIN * o.getStepX() + 5 * s.getStepX(), y + 1, WijkPlan.STRAAT_BEGIN * o.getStepZ() + 5 * s.getStepZ(), Blocks.SPRUCE_PLANKS);
            w.zet(WijkPlan.STRAAT_BEGIN * o.getStepX() + 5 * s.getStepX(), y, WijkPlan.STRAAT_BEGIN * o.getStepZ() + 5 * s.getStepZ(), Blocks.SPRUCE_FENCE);
        }

        // ---- parks between the streets ------------------------------------------------------------------------------------------
        Block[] bloemen = {Blocks.PINK_TULIP, Blocks.ALLIUM, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.PINK_PETALS};
        for (int qx : new int[] {-1, 1}) {
            for (int qz : new int[] {-1, 1}) {
                int tx = 19 * qx, tz = 19 * qz;
                for (int ax = 4; ax <= 25; ax++) {
                    for (int az = 4; az <= 25; az++) {
                        if (Math.max(ax, az) < WijkPlan.STRAAT_BEGIN) {
                            continue;
                        }
                        int x = ax * qx, z = az * qz;
                        boolean boom = Math.abs(x - tx) <= 2 && Math.abs(z - tz) <= 2;
                        boolean bord = Math.abs(x) <= WijkPlan.STRAAT_BEGIN + 1 && Math.abs(z) <= 6 || Math.abs(z) <= WijkPlan.STRAAT_BEGIN + 1 && Math.abs(x) <= 6;
                        if (!boom && !bord && r.nextInt(8) == 0) {
                            w.zet(x, y, z, bloemen[r.nextInt(bloemen.length)]);
                        }
                    }
                }
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        for (int dy = 3; dy <= 5; dy++) {
                            if (Math.abs(dx) + Math.abs(dz) + (dy == 5 ? 1 : 0) <= 3 && !(dx == 0 && dz == 0 && dy < 5)) {
                                w.zet(tx + dx, y + dy, tz + dz, Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                            }
                        }
                    }
                }
                for (int dy = 0; dy < 5; dy++) {
                    w.zet(tx, y + dy, tz, Blocks.CHERRY_LOG);
                }
                w.zet(tx - 2 * qx, y, tz, Blocks.CHERRY_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                w.zet(tx, y, tz - 2 * qz, Blocks.CHERRY_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            }
        }
        w.verbind();

        // ---- the plots ------------------------------------------------------------------------------------------------------------
        for (WijkPlan.Kavel k : WijkPlan.kavels(wijk)) {
            HuizenData.Huis h = data.huis(k.sleutel());
            if (h == null) {
                Ontwerp.teKoop(level, k);
            } else {
                Ontwerp.huis(level, k, h.soort, h.variant);
            }
        }
    }

    /** Is this block (world) part of the notice board? */
    public static boolean isBord(BlockPos p) {
        int w = WijkPlan.wijkVan(p.getX());
        int x = p.getX() - w * WijkPlan.AFSTAND, z = p.getZ();
        return x >= BORD_X0 && x <= BORD_X1 && (z == BORD_Z || z == BORD_Z + 1) && p.getY() >= WijkPlan.VLOER && p.getY() <= WijkPlan.VLOER + 4;
    }
}
