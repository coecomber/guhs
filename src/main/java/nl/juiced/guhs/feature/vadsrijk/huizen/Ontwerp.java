package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.huizen.WijkPlan.Kavel;

/**
 * U-HUIZEN: the house templates, generated in code: 3 sizes ({@link HuisSoort}) x 3 looks ({@link Palet}: Snoepje, Muntje,
 * Lavendel), plus the empty "te koop" plot. Written in the plot's canonical frame (street NORTH) and turned per plot.
 * <ul>
 *   <li>Every connecting block (fences, panes, gates, stairs) gets its real connection state in a final pass
 *   ({@link Block#updateFromNeighbourShapes}) over everything the template wrote: no lone posts.</li>
 *   <li>Walkable: the path from the gate to the door is flat, the upper floor has a ladder, every room has a real floor and light.</li>
 *   <li>Interior: empty for the owner to furnish, with ceiling lanterns and the trophy shelf on the back wall.</li>
 *   <li>Garden: flowers, a little cherry tree, the stable corner (hay, posts, a water trough) and the mailbox by the gate.</li>
 * </ul>
 */
public final class Ontwerp {
    /** Height (above the floor) cleared when a plot is (re)built. */
    static final int LEEG_HOOGTE = WijkPlan.HOOGTE + 8;

    /** One look: wall, corner/trim, socle, roof stairs/slab/ridge, floor, window pane, door, fence + gate. */
    public record Palet(String id, Block muur, Block rand, Block sokkel, Block dakTrap, Block dakPlaat, Block nok, Block vloer, Block raam, Block deur,
                        Block hek, Block poort, boolean nokLangsV, boolean veranda) {
        public String naamKey() {
            return "vadsrijk.huizen.variant." + id;
        }
    }

    public static final List<Palet> PALETTEN = List.of(
            new Palet("snoepje", Blocks.WHITE_TERRACOTTA, Blocks.STRIPPED_CHERRY_LOG, Blocks.PINK_TERRACOTTA, Blocks.CHERRY_STAIRS, Blocks.CHERRY_SLAB,
                    Blocks.CHERRY_PLANKS, Blocks.CHERRY_PLANKS, Blocks.PINK_STAINED_GLASS_PANE, Blocks.CHERRY_DOOR, Blocks.CHERRY_FENCE,
                    Blocks.CHERRY_FENCE_GATE, false, false),
            new Palet("muntje", Blocks.CALCITE, Blocks.STRIPPED_BIRCH_LOG, Blocks.PINK_TERRACOTTA, Blocks.WARPED_STAIRS, Blocks.WARPED_SLAB,
                    Blocks.WARPED_PLANKS, Blocks.BIRCH_PLANKS, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.BIRCH_DOOR, Blocks.BIRCH_FENCE,
                    Blocks.BIRCH_FENCE_GATE, true, false),
            new Palet("lavendel", Blocks.SMOOTH_QUARTZ, Blocks.PURPUR_PILLAR, Blocks.MAGENTA_TERRACOTTA, Blocks.PURPUR_STAIRS, Blocks.PURPUR_SLAB,
                    Blocks.PURPUR_BLOCK, Blocks.PALE_OAK_PLANKS, Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.PALE_OAK_DOOR, Blocks.PALE_OAK_FENCE,
                    Blocks.PALE_OAK_FENCE_GATE, false, true));

    public static Palet palet(int i) {
        return PALETTEN.get(Math.floorMod(i, PALETTEN.size()));
    }

    /** What a template wrote: block positions (world) of everything non-air, and the special spots (world points). */
    public static final class Uitkomst {
        public final List<BlockPos> blokken = new ArrayList<>();
        public final List<Vec3> trofeeen = new ArrayList<>();
        public Vec3 stal = Vec3.ZERO, tuinMin = Vec3.ZERO, tuinMax = Vec3.ZERO;
        public BlockPos post = BlockPos.ZERO, deur = BlockPos.ZERO, bord = BlockPos.ZERO;
    }

    /** Writes canonical (u, y, v) blocks into a plot, turned, and remembers them for the connection pass. */
    static final class Pen {
        final ServerLevel level;
        final Kavel k;
        final Map<BlockPos, BlockState> geschreven = new LinkedHashMap<>();

        Pen(ServerLevel level, Kavel k) {
            this.level = level;
            this.k = k;
        }

        void zet(int u, int y, int v, BlockState s) {
            BlockPos p = k.wereld(u, y, v);
            BlockState t = s.rotate(k.rotatie());
            level.setBlock(p, t, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            geschreven.put(p, t);
        }

        void zet(int u, int y, int v, Block b) {
            zet(u, y, v, b.defaultBlockState());
        }

        /** The connection pass: every written block takes its neighbours into account (fences join, panes join walls, stairs corner). */
        void verbind() {
            for (Map.Entry<BlockPos, BlockState> e : geschreven.entrySet()) {
                BlockState nu = level.getBlockState(e.getKey());
                if (nu.isAir()) {
                    continue;
                }
                BlockState goed = Block.updateFromNeighbourShapes(nu, level, e.getKey());
                if (goed != nu) {
                    level.setBlock(e.getKey(), goed, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }

    private Ontwerp() {
    }

    /** Empties the plot (air above the grass, grass on top, dirt below). Used by every template first. */
    static void leeg(Pen pen) {
        for (int u = 0; u < WijkPlan.KAVEL; u++) {
            for (int v = 0; v < WijkPlan.KAVEL; v++) {
                for (int y = WijkPlan.VLOER; y <= WijkPlan.VLOER + LEEG_HOOGTE; y++) {
                    BlockPos p = pen.k.wereld(u, y, v);
                    if (!pen.level.getBlockState(p).isAir()) {
                        pen.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
                pen.level.setBlock(pen.k.wereld(u, WijkPlan.GROND, v), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                pen.level.setBlock(pen.k.wereld(u, WijkPlan.GROND - 1, v), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** A free plot: grass, pink corner stones and the "te koop" sign by the street. */
    public static Uitkomst teKoop(ServerLevel level, Kavel k) {
        Pen pen = new Pen(level, k);
        leeg(pen);
        int g = WijkPlan.GROND, y = WijkPlan.VLOER, m = WijkPlan.KAVEL - 1;
        for (int[] c : new int[][] {{0, 0}, {m, 0}, {0, m}, {m, m}}) {
            pen.zet(c[0], g, c[1], Blocks.PINK_CONCRETE);
        }
        for (int v = 0; v < 4; v++) {
            pen.zet(WijkPlan.POORT_U, g, v, Blocks.DIRT_PATH);
        }
        pen.zet(WijkPlan.POORT_U, y, 1, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8));   // (faces the street)
        Uitkomst uit = new Uitkomst();
        uit.bord = k.wereld(WijkPlan.POORT_U, y, 1);
        if (level.getBlockEntity(uit.bord) instanceof SignBlockEntity sign) {
            SignText t = new SignText()
                    .setMessage(0, Component.translatable("vadsrijk.huizen.bord.te_koop"))
                    .setMessage(1, Component.translatable("vadsrijk.huizen.bord.kavel", k.nr() + 1))
                    .setMessage(2, Component.translatable("vadsrijk.huizen.bord.wijk", k.wijk() + 1))
                    .setMessage(3, Component.translatable("vadsrijk.huizen.bord.klik"));
            sign.setText(t, true);
            sign.setText(t, false);
            sign.setWaxed(true);
            sign.setChanged();
            level.sendBlockUpdated(uit.bord, sign.getBlockState(), sign.getBlockState(), Block.UPDATE_CLIENTS);
        }
        pen.verbind();
        uit.blokken.addAll(pen.geschreven.keySet());
        return uit;
    }

    /** A house of size {@code s} in look {@code variant} on plot {@code k}. */
    public static Uitkomst huis(ServerLevel level, Kavel k, HuisSoort s, int variant) {
        Palet p = palet(variant);
        Pen pen = new Pen(level, k);
        leeg(pen);
        Uitkomst uit = new Uitkomst();
        int g = WijkPlan.GROND, y0 = WijkPlan.VLOER, m = WijkPlan.KAVEL - 1;
        int u0 = s.u0(), u1 = s.u1(), v0 = s.v0(), v1 = s.v1(), top = s.muurTop();
        RandomSource r = RandomSource.create(k.sleutel() * 31L + variant);

        // ---- fence round the plot, gate, lamp posts, path ---------------------------------------------------------------------------
        for (int i = 0; i <= m; i++) {
            pen.zet(i, y0, 0, p.hek());
            pen.zet(i, y0, m, p.hek());
            pen.zet(0, y0, i, p.hek());
            pen.zet(m, y0, i, p.hek());
        }
        pen.zet(WijkPlan.POORT_U, y0, 0, p.poort().defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        for (int u : new int[] {WijkPlan.POORT_U - 1, WijkPlan.POORT_U + 1}) {
            pen.zet(u, y0 + 1, 0, p.hek());
            pen.zet(u, y0 + 2, 0, Blocks.LANTERN);
        }
        for (int v = 0; v < v0; v++) {
            pen.zet(WijkPlan.POORT_U, g, v, Blocks.DIRT_PATH);
        }

        // ---- floor, socle, walls ------------------------------------------------------------------------------------------------------
        for (int u = u0; u <= u1; u++) {
            for (int v = v0; v <= v1; v++) {
                boolean rand = u == u0 || u == u1 || v == v0 || v == v1;
                pen.zet(u, g, v, rand ? p.sokkel() : p.vloer());
            }
        }
        for (int y = y0; y <= top; y++) {
            int verdieping = (y - y0) / 4, inVerdieping = (y - y0) % 4;
            for (int u = u0; u <= u1; u++) {
                for (int v = v0; v <= v1; v++) {
                    boolean hoekU = u == u0 || u == u1, hoekV = v == v0 || v == v1;
                    if (!hoekU && !hoekV) {
                        continue;
                    }
                    Block b = (hoekU && hoekV) || inVerdieping == 0 && verdieping > 0 ? p.rand() : p.muur();
                    boolean raam = inVerdieping == 1 || inVerdieping == 2;
                    if (raam && !(hoekU && hoekV)) {
                        int du = Math.abs(u - WijkPlan.POORT_U);
                        boolean langs = hoekV ? du == 2 || du == 5 || v == v1 && du == 0 : (v - v0) % 3 == 2 && v < v1;
                        if (langs && !(v == v0 && Math.abs(u - WijkPlan.POORT_U) <= 1 && verdieping == 0)) {
                            b = p.raam();
                        }
                    }
                    if (inVerdieping == 0 && verdieping > 0) {
                        b = p.nok();   // (a band between the floors)
                    }
                    pen.zet(u, y, v, b);
                }
            }
        }
        // the door (outward to the street)
        BlockState deur = p.deur().defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        pen.zet(WijkPlan.POORT_U, y0, v0, deur.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        pen.zet(WijkPlan.POORT_U, y0 + 1, v0, deur.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        uit.deur = k.wereld(WijkPlan.POORT_U, y0, v0);
        // a band on top of the walls (the roof and the ceiling rest on it)
        for (int u = u0; u <= u1; u++) {
            for (int v = v0; v <= v1; v++) {
                if (u == u0 || u == u1 || v == v0 || v == v1) {
                    pen.zet(u, top + 1, v, p.nok());
                }
            }
        }

        // ---- floors and ceilings ------------------------------------------------------------------------------------------------------
        int ladderU = u0 + 1, ladderV = v1 - 1;
        for (int verdieping = 1; verdieping <= s.vloeren; verdieping++) {
            int yp = y0 + 4 * verdieping;   // (floor of the next storey, or the ceiling of the top one)
            for (int u = u0 + 1; u < u1; u++) {
                for (int v = v0 + 1; v < v1; v++) {
                    boolean gat = verdieping < s.vloeren && u == ladderU && v == ladderV;
                    if (!gat) {
                        pen.zet(u, yp, v, verdieping < s.vloeren ? p.vloer() : p.nok());
                    }
                }
            }
            // ceiling lanterns of the storey below
            for (int u = u0 + 2; u < u1 - 1; u += 3) {
                for (int v = v0 + 2; v < v1 - 1; v += 3) {
                    if (!(u == ladderU && v == ladderV)) {
                        pen.zet(u, yp - 1, v, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
                    }
                }
            }
        }
        if (s.vloeren > 1) {
            for (int y = y0; y <= y0 + 4; y++) {
                pen.zet(ladderU, y, ladderV, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
            }
        }

        // ---- trophy shelf on the back wall (ground floor) -----------------------------------------------------------------------------
        int plank = y0 + 1;
        for (int u = u0 + 2; u <= u1 - 2; u++) {
            if (u == ladderU && s.vloeren > 1) {
                continue;
            }
            pen.zet(u, plank, v1 - 1, p.dakPlaat().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            if ((u - u0) % 2 == 0) {
                uit.trofeeen.add(k.wereld(u + 0.5, plank + 1.0, v1 - 1 + 0.5));
            }
        }

        // ---- roof -----------------------------------------------------------------------------------------------------------------------
        dak(pen, p, u0, u1, v0, v1, top + 1);
        if (p.veranda()) {
            int pu0 = WijkPlan.POORT_U - 2, pu1 = WijkPlan.POORT_U + 2;
            for (int u = pu0; u <= pu1; u++) {
                for (int v = 2; v < v0; v++) {
                    pen.zet(u, y0 + 3, v, p.dakPlaat());
                    pen.zet(u, g, v, u == WijkPlan.POORT_U ? Blocks.DIRT_PATH : p.vloer());
                }
            }
            for (int u : new int[] {pu0, pu1}) {
                for (int y = y0; y < y0 + 3; y++) {
                    pen.zet(u, y, 2, p.rand());   // (porch pillars)
                }
            }
            pen.zet(WijkPlan.POORT_U, y0 + 2, 3, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            // chimney with a little smoke
            int cu = u1 - 1, cv = v1 - 1;
            int dakTop = top + 1 + (s.diep + 2) / 2;
            for (int y = top + 1; y <= dakTop + 1; y++) {
                pen.zet(cu, y, cv, Blocks.BRICKS);
            }
            pen.zet(cu, dakTop + 2, cv, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        }

        // ---- garden: stable corner, little tree, flowers, mailbox ---------------------------------------------------------------------
        for (int u = 1; u <= 4; u++) {
            for (int v = 15; v <= 18; v++) {
                pen.zet(u, g, v, Blocks.HAY_BLOCK);
                pen.zet(u, y0 + 3, v, p.dakPlaat());
            }
        }
        for (int[] c : new int[][] {{1, 15}, {4, 15}, {1, 18}, {4, 18}}) {
            for (int y = y0; y < y0 + 3; y++) {
                pen.zet(c[0], y, c[1], p.hek());
            }
        }
        for (int v = 16; v <= 17; v++) {
            pen.zet(4, y0, v, p.hek());   // (the pen's side rail: the stable is open towards the house)
        }
        pen.zet(2, y0, 18, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        pen.zet(3, y0, 18, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        pen.zet(2, y0 + 2, 16, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        uit.stal = k.wereld(3.0, y0, 16.3);
        // cherry tree
        int tu = 16, tv = 16;
        for (int du = -2; du <= 2; du++) {
            for (int dv = -2; dv <= 2; dv++) {
                for (int dy = 2; dy <= 4; dy++) {
                    int d = Math.abs(du) + Math.abs(dv) + (dy == 4 ? 1 : 0);
                    if (d <= 3 && !(du == 0 && dv == 0 && dy < 4)) {
                        pen.zet(tu + du, y0 + dy, tv + dv, Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                    }
                }
            }
        }
        for (int y = y0; y < y0 + 4; y++) {
            pen.zet(tu, y, tv, Blocks.CHERRY_LOG);
        }
        // flowers on the free garden grass
        Block[] bloemen = {Blocks.PINK_TULIP, Blocks.ALLIUM, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.PINK_PETALS};
        for (int u = 1; u < m; u++) {
            for (int v = 1; v < m; v++) {
                boolean huis = u >= u0 - 1 && u <= u1 + 1 && v >= v0 - 1 && v <= v1 + 1;
                boolean stal = u <= 5 && v >= 14;
                boolean boom = Math.abs(u - tu) <= 1 && Math.abs(v - tv) <= 1;
                boolean pad = Math.abs(u - WijkPlan.POORT_U) <= 2 && v < v0 || u >= WijkPlan.POST_U - 1 && u <= WijkPlan.POST_U + 1 && v <= 2;
                if (!huis && !stal && !boom && !pad && r.nextInt(9) == 0) {
                    pen.zet(u, y0, v, bloemen[r.nextInt(bloemen.length)]);
                }
            }
        }
        uit.tuinMin = k.wereld(1.5, y0, 1.5);
        uit.tuinMax = k.wereld(m - 1.5, y0, m - 1.5);
        // the mailbox (a post with a barrel on it, by the gate)
        pen.zet(WijkPlan.POST_U, y0, WijkPlan.POST_V, p.hek());
        pen.zet(WijkPlan.POST_U, y0 + 1, WijkPlan.POST_V, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.NORTH));
        uit.post = k.wereld(WijkPlan.POST_U, y0 + 1, WijkPlan.POST_V);

        pen.verbind();
        for (Map.Entry<BlockPos, BlockState> e : pen.geschreven.entrySet()) {
            if (!e.getValue().isAir()) {
                uit.blokken.add(e.getKey());
            }
        }
        return uit;
    }

    /** A gable roof from y {@code y0} up, with a 1-block overhang; the ridge runs along u (or along v: {@link Palet#nokLangsV}). */
    static void dak(Pen pen, Palet p, int u0, int u1, int v0, int v1, int y0) {
        if (!p.nokLangsV()) {
            int voor = v0 - 1, achter = v1 + 1;
            for (int k = 0; voor <= achter; k++, voor++, achter--) {
                int y = y0 + k;
                for (int u = u0 - 1; u <= u1 + 1; u++) {
                    if (voor == achter) {
                        pen.zet(u, y, voor, p.dakPlaat());
                    } else {
                        pen.zet(u, y, voor, p.dakTrap().defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH).setValue(StairBlock.HALF, Half.BOTTOM));
                        pen.zet(u, y, achter, p.dakTrap().defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.BOTTOM));
                    }
                }
                for (int v = voor + 1; v < achter; v++) {   // (gable ends)
                    if (k > 0) {
                        pen.zet(u0, y, v, p.muur());
                        pen.zet(u1, y, v, p.muur());
                    }
                }
            }
        } else {
            int links = u0 - 1, rechts = u1 + 1;
            for (int k = 0; links <= rechts; k++, links++, rechts--) {
                int y = y0 + k;
                for (int v = v0 - 1; v <= v1 + 1; v++) {
                    if (links == rechts) {
                        pen.zet(links, y, v, p.dakPlaat());
                    } else {
                        pen.zet(links, y, v, p.dakTrap().defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.BOTTOM));
                        pen.zet(rechts, y, v, p.dakTrap().defaultBlockState().setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, Half.BOTTOM));
                    }
                }
                for (int u = links + 1; u < rechts; u++) {
                    if (k > 0) {
                        pen.zet(u, y, v0, p.muur());
                        pen.zet(u, y, v1, p.muur());
                    }
                }
            }
            // a round window in the gable facing the street
            int mid = (u0 + u1) / 2;
            pen.zet(mid, y0 + 1, v0, p.raam());
        }
    }

    /** Where the trophies stand (world points on the shelf of the back wall); the same columns {@link #huis} gives the shelf. */
    public static List<Vec3> trofeePlekken(Kavel k, HuisSoort s) {
        List<Vec3> l = new ArrayList<>();
        int ladderU = s.u0() + 1;
        for (int u = s.u0() + 2; u <= s.u1() - 2; u++) {
            if (u == ladderU && s.vloeren > 1) {
                continue;
            }
            if ((u - s.u0()) % 2 == 0) {
                l.add(k.wereld(u + 0.5, WijkPlan.VLOER + 2.0, s.v1() - 1 + 0.5));
            }
        }
        return l;
    }

    /** Where the mount stands in the stable corner. */
    public static Vec3 stalPlek(Kavel k) {
        return k.wereld(3.0, WijkPlan.VLOER, 16.3);
    }

    /** The house box (world, for "inside the house"): footprint + up to the roof. */
    public static boolean inHuis(Kavel k, HuisSoort s, double x, double y, double z) {
        int[] l = k.lokaal(BlockPos.containing(x, y, z));
        return l[0] > s.u0() && l[0] < s.u1() && l[1] > s.v0() && l[1] < s.v1() && y >= WijkPlan.VLOER - 0.5 && y < s.muurTop() + 1;
    }
}
