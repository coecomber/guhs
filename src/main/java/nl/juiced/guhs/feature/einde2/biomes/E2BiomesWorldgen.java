package nl.juiced.guhs.feature.einde2.biomes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * einde2, slice biomes: the worldgen features of the six biomes of the outer islands (configured and placed in
 * data/guhs/worldgen as einde2_biomes_*, see tools/features/einde2_biomes.py). The terrain of the Guheinde is the same
 * in every biome; the look of a biome comes from these: holes and sauce ponds in the Gatenkaasheuvels, boulders, dunes
 * and an oasis in the Korstwoestijn, floating crumb islets and clouds in the Kruimelwolken, giant mushrooms and ponds on
 * the Schimmelkaasvlakte, crystal spires on the Kristalkaasklippen and the knabbelkoor of the Knabbelkoorbos.
 * <p>
 * Rules they all keep: they only start on the ground of the islands ({@code #guhs:einde2/eilandgrond}), never in the void;
 * they stay within {@link #BEREIK} blocks of their origin (a feature may only write in the chunks next to its own); they
 * never go above y {@link Einde2Ids#NATUUR_MAX}; whatever digs or builds keeps {@link #AFSTAND} blocks away from every
 * structure piece; and a hole or a pond never opens the underside of an island.
 */
public final class E2BiomesWorldgen {
    /** The furthest any of these writes from its origin (x and z). */
    public static final int BEREIK = 12;
    /** How far the digging and building features stay from structure pieces. */
    public static final int AFSTAND = 4;

    static boolean grond(BlockState state) {
        return state.is(Einde2Ids.EILANDGROND);
    }

    /** Air, or a small plant that may make room. */
    static boolean vrij(BlockState state) {
        return state.isAir() || state.getBlock() instanceof VegetationBlock;
    }

    /**
     * The first free cell above the ground in this column, looked for around y (the heightmaps of worldgen do not exist in a
     * finished world, and the islands lie in layers): Integer.MIN_VALUE when there is no ground within {@code zoek} blocks.
     */
    static int vloer(WorldGenLevel level, int x, int y, int z, int zoek) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
        if (vrij(level.getBlockState(p))) {
            for (int i = 0; i < zoek && p.getY() > level.getMinY(); i++) {
                p.move(Direction.DOWN);
                if (!vrij(level.getBlockState(p))) {
                    return grond(level.getBlockState(p)) ? p.getY() + 1 : Integer.MIN_VALUE;
                }
            }
            return Integer.MIN_VALUE;
        }
        for (int i = 0; i < zoek; i++) {
            BlockState onder = level.getBlockState(p);
            p.move(Direction.UP);
            if (vrij(level.getBlockState(p))) {
                return grond(onder) ? p.getY() : Integer.MIN_VALUE;
            }
        }
        return Integer.MIN_VALUE;
    }

    static boolean bijGebouw(WorldGenLevel level, int x0, int y0, int z0, int x1, int y1, int z1) {
        return BouwRuimte.touchesBuilding(level, x0 - AFSTAND, y0 - AFSTAND, z0 - AFSTAND, x1 + AFSTAND, y1 + AFSTAND, z1 + AFSTAND);
    }

    static boolean teHoog(int y) {
        return y > Einde2Ids.NATUUR_MAX;
    }

    private abstract static class Basis extends Feature<NoneFeatureConfiguration> {
        Basis() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public final boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            return maak(ctx.level(), ctx.origin(), ctx.random());
        }

        abstract boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random);
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * Gatenkaasheuvels: round holes in the cheese, one to three together, big ones you can sit in and small pockmarks. A
     * hole is only made where the island is thick enough to keep its underside closed.
     */
    public static class Kaasgat extends Basis {
        @Override
        boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random) {
            int gemaakt = 0;
            int n = 1 + random.nextInt(3);
            for (int i = 0; i < n; i++) {
                int x = origin.getX() + (i == 0 ? 0 : random.nextInt(13) - 6), z = origin.getZ() + (i == 0 ? 0 : random.nextInt(13) - 6);
                int top = vloer(level, x, origin.getY(), z, 6);
                if (top == Integer.MIN_VALUE) {
                    continue;
                }
                float r = random.nextBoolean() ? 1.3f + random.nextFloat() * 0.8f : 2.3f + random.nextFloat() * 2.2f;
                if (gat(level, x, top - 1, z, r)) {
                    gemaakt++;
                }
            }
            return gemaakt > 0;
        }

        /** One hole: a ball that pokes out of the surface at (x, sy, z). */
        static boolean gat(WorldGenLevel level, int x, int sy, int z, float r) {
            double cy = sy + 0.5 - r * 0.3;
            int bodem = Mth.floor(cy - r);
            int ri = Mth.ceil(r);
            // the island must be closed under the whole hole
            for (int[] d : new int[][]{{0, 0}, {ri, 0}, {-ri, 0}, {0, ri}, {0, -ri}}) {
                if (!grond(level.getBlockState(new BlockPos(x + d[0], bodem - 1, z + d[1]))) || !grond(level.getBlockState(new BlockPos(x + d[0], bodem - 2, z + d[1])))) {
                    return false;
                }
            }
            if (bijGebouw(level, x - ri, bodem, z - ri, x + ri, sy + 4, z + ri)) {
                return false;
            }
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean iets = false;
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (!grond(level.getBlockState(p.set(x + dx, bodem - 1, z + dz)))) {
                        continue;
                    }
                    for (int y = bodem; y <= sy + 4; y++) {
                        double ddy = Math.min(y + 0.5 - cy, r * 0.3);     // (above the middle the hole goes straight up: an open mouth)
                        if (dx * dx + dz * dz + ddy * ddy > r * r) {
                            continue;
                        }
                        BlockState s = level.getBlockState(p.set(x + dx, y, z + dz));
                        if (grond(s) || !s.isAir() && vrij(s) || s.getBlock() instanceof SnowLayerBlock) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                            iets = true;
                        }
                    }
                }
            }
            return iets;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * A little pool sunk into flat ground: the shared maker of the kaassaus ponds, the ponds of the Schimmelkaasvlakte and
     * the oasis. It only comes where the ground around is level and closed, so nothing leaks: every cell of fluid has
     * ground under it and ground or fluid on all four sides.
     *
     * @return the y of the fluid's surface, or Integer.MIN_VALUE when there is no room for a pool here
     */
    static int poel(WorldGenLevel level, BlockPos origin, RandomSource random, float r, int diep, BlockState vocht) {
        int cx = origin.getX(), cz = origin.getZ();
        float ruim = r + 1.6f;
        int ri = Mth.ceil(ruim);
        int laag = Integer.MAX_VALUE, hoog = Integer.MIN_VALUE;
        int[][] top = new int[2 * ri + 1][2 * ri + 1];
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dz = -ri; dz <= ri; dz++) {
                if (dx * dx + dz * dz > ruim * ruim) {
                    continue;
                }
                int t = vloer(level, cx + dx, origin.getY(), cz + dz, 5);
                if (t == Integer.MIN_VALUE) {
                    return Integer.MIN_VALUE;
                }
                top[dx + ri][dz + ri] = t;
                laag = Math.min(laag, t - 1);
                hoog = Math.max(hoog, t - 1);
            }
        }
        if (hoog - laag > 2) {
            return Integer.MIN_VALUE;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dz = -ri; dz <= ri; dz++) {
                if (dx * dx + dz * dz > ruim * ruim) {
                    continue;
                }
                for (int d = 0; d <= diep + 1; d++) {
                    if (!grond(level.getBlockState(p.set(cx + dx, laag - d, cz + dz)))) {
                        return Integer.MIN_VALUE;
                    }
                }
            }
        }
        if (bijGebouw(level, cx - ri, laag - diep, cz - ri, cx + ri, hoog + 2, cz + ri)) {
            return Integer.MIN_VALUE;
        }
        float fase = random.nextFloat() * 6.28f;
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dz = -ri; dz <= ri; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                double rand = r * (0.86 + 0.14 * Math.sin(Math.atan2(dz, dx) * 3 + fase));
                if (d > rand) {
                    continue;
                }
                for (int y = top[dx + ri][dz + ri] + 1; y > laag; y--) {
                    BlockState s = level.getBlockState(p.set(cx + dx, y, cz + dz));
                    if (!s.isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                int diepte = d < rand - 1.4 ? diep : 1;
                for (int i = 0; i < diepte; i++) {
                    level.setBlock(p.set(cx + dx, laag - i, cz + dz), vocht, 2);
                }
            }
        }
        return laag;
    }

    /** Gatenkaasheuvels: a little pond of kaassaus. */
    public static class Sausmeertje extends Basis {
        @Override
        boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random) {
            return poel(level, origin, random, 1.8f + random.nextFloat() * 1.6f, 1 + random.nextInt(2), ModBlocks.KAAS_SAUS.get().defaultBlockState()) != Integer.MIN_VALUE;
        }
    }

    /** Schimmelkaasvlakte: a still pond, with glowing schimmelpaddenstoelen on its bank. */
    public static class Vijver extends Basis {
        @Override
        boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random) {
            float r = 2.4f + random.nextFloat() * 2.8f;
            int y = poel(level, origin, random, r, 1 + random.nextInt(2), Blocks.WATER.defaultBlockState());
            if (y == Integer.MIN_VALUE) {
                return false;
            }
            BlockState zwam = E2BiomesFeature.SCHIMMELPADDENSTOEL.get().defaultBlockState();
            for (int i = 0; i < 10; i++) {
                double a = random.nextFloat() * 6.28;
                double d = r + 0.8 + random.nextFloat() * 1.8;
                int x = origin.getX() + (int) Math.round(Math.cos(a) * d), z = origin.getZ() + (int) Math.round(Math.sin(a) * d);
                int t = vloer(level, x, y + 1, z, 4);
                if (t != Integer.MIN_VALUE && level.getBlockState(new BlockPos(x, t, z)).isAir()) {
                    level.setBlock(new BlockPos(x, t, z), zwam, 2);
                }
            }
            return true;
        }
    }

    /**
     * Korstwoestijn: the one oasis in a long way: a pond of clear water in a ring of fresh kaaskorst, thick with korstgras,
     * a few boulders to sit on.
     */
    public static class Oase extends Basis {
        @Override
        boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random) {
            float r = 2.6f + random.nextFloat() * 1.6f;
            int y = poel(level, origin, random, r, 2, Blocks.WATER.defaultBlockState());
            if (y == Integer.MIN_VALUE) {
                return false;
            }
            BlockState korst = GuheindeFeature.KAASKORST.get().defaultBlockState();
            BlockState gras = E2BiomesFeature.KORSTGRAS.get().defaultBlockState();
            float buiten = Math.min(r + 5.5f, BEREIK - 1);
            int ri = Mth.ceil(buiten);
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > buiten - random.nextFloat() * 1.5f) {
                        continue;
                    }
                    int x = origin.getX() + dx, z = origin.getZ() + dz;
                    int t = vloer(level, x, y + 1, z, 4);
                    if (t == Integer.MIN_VALUE || !level.getFluidState(p.set(x, t - 1, z)).isEmpty()) {
                        continue;
                    }
                    // fresh crust instead of the cracked desert floor and its sand (the two blocks under the surface too, so
                    // no sand is left next to the water)
                    for (int i = 1; i <= 3; i++) {
                        BlockState s = level.getBlockState(p.set(x, t - i, z));
                        if (s.is(E2BiomesFeature.GEBARSTEN_KORST.get()) || s.is(E2BiomesFeature.KRUIMELZAND.get())) {
                            level.setBlock(p, korst, 2);
                        }
                    }
                    BlockState er = level.getBlockState(p.set(x, t, z));
                    if (!er.isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                    if (random.nextFloat() < (d < r + 3 ? 0.55f : 0.25f)) {
                        level.setBlock(p, gras, 2);
                    }
                }
            }
            for (int i = 0; i < 2 + random.nextInt(2); i++) {
                double a = random.nextFloat() * 6.28;
                int x = origin.getX() + (int) Math.round(Math.cos(a) * (r + 2.5)), z = origin.getZ() + (int) Math.round(Math.sin(a) * (r + 2.5));
                int t = vloer(level, x, y + 1, z, 4);
                if (t != Integer.MIN_VALUE) {
                    Kei.kei(level, new BlockPos(x, t, z), random, 1.0f + random.nextFloat() * 0.5f);
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** A boulder of kruimelrots, half sunk into the ground: a crumb the size of a guhhuisje. */
    public static class Kei extends Basis {
        @Override
        boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random) {
            int t = vloer(level, origin.getX(), origin.getY(), origin.getZ(), 6);
            if (t == Integer.MIN_VALUE || bijGebouw(level, origin.getX() - 5, t - 2, origin.getZ() - 5, origin.getX() + 5, t + 6, origin.getZ() + 5)) {
                return false;
            }
            return kei(level, new BlockPos(origin.getX(), t, origin.getZ()), random, 1.2f + random.nextFloat() * 1.5f);
        }

        /** Three overlapping lumps around the cell {@code voet} (the first free cell on the ground). */
        static boolean kei(WorldGenLevel level, BlockPos voet, RandomSource random, float r0) {
            BlockState rots = E2BiomesFeature.KRUIMELROTS.get().defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean iets = false;
            for (int k = 0; k < 3; k++) {
                float r = r0 * (0.65f + random.nextFloat() * 0.35f);
                double cx = voet.getX() + 0.5 + (k == 0 ? 0 : (random.nextFloat() - 0.5) * r0 * 1.6), cz = voet.getZ() + 0.5 + (k == 0 ? 0 : (random.nextFloat() - 0.5) * r0 * 1.6);
                double cy = voet.getY() + r * 0.35;
                int ri = Mth.ceil(r);
                for (int dx = -ri; dx <= ri; dx++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        for (int dy = -ri; dy <= ri; dy++) {
                            p.set(Mth.floor(cx) + dx, Mth.floor(cy) + dy, Mth.floor(cz) + dz);
                            double ex = p.getX() + 0.5 - cx, ey = p.getY() + 0.5 - cy, ez = p.getZ() + 0.5 - cz;
                            if (ex * ex + ey * ey + ez * ez <= r * r && vrij(level.getBlockState(p)) && !teHoog(p.getY())
                                    && Math.abs(p.getX() - voet.getX()) <= BEREIK && Math.abs(p.getZ() - voet.getZ()) <= BEREIK) {
                                level.setBlock(p, rots, 2);
                                iets = true;
                            }
                        }
                    }
                }
            }
            return iets;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Korstwoestijn: a dune of crumb sand, a long low back whose slopes are layers of kruimelzandlaag. */
    public static class Duin extends Basis {
        @Override
        boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random) {
            int t0 = vloer(level, origin.getX(), origin.getY(), origin.getZ(), 6);
            if (t0 == Integer.MIN_VALUE) {
                return false;
            }
            float a = 4.0f + random.nextFloat() * 5.0f, b = 2.2f + random.nextFloat() * 2.4f, hoog = 1.1f + random.nextFloat() * 2.2f;
            double hoek = random.nextFloat() * Math.PI;
            double cos = Math.cos(hoek), sin = Math.sin(hoek);
            int ri = Math.min(Mth.ceil(a), BEREIK - 1);
            if (bijGebouw(level, origin.getX() - ri, t0 - 1, origin.getZ() - ri, origin.getX() + ri, t0 + 5, origin.getZ() + ri)) {
                return false;
            }
            BlockState zand = E2BiomesFeature.KRUIMELZAND.get().defaultBlockState();
            BlockState laag = E2BiomesFeature.KRUIMELZANDLAAG.get().defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean iets = false;
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double u = (dx * cos + dz * sin) / a, v = (-dx * sin + dz * cos) / b;
                    double d2 = u * u + v * v;
                    if (d2 >= 1) {
                        continue;
                    }
                    int lagen = (int) Math.round(hoog * Math.pow(1 - d2, 1.3) * 8);
                    int x = origin.getX() + dx, z = origin.getZ() + dz;
                    int t = vloer(level, x, t0, z, 3);
                    if (lagen <= 0 || t == Integer.MIN_VALUE || Math.abs(t - t0) > 1) {
                        continue;
                    }
                    BlockState onder = level.getBlockState(p.set(x, t - 1, z));
                    if (!onder.isCollisionShapeFullBlock(level, p) && !onder.is(E2BiomesFeature.KRUIMELZAND.get())) {
                        continue;
                    }
                    int y = t;
                    for (; lagen >= 8; lagen -= 8, y++) {
                        if (!vrij(level.getBlockState(p.set(x, y, z))) || teHoog(y)) {
                            lagen = 0;
                            break;
                        }
                        level.setBlock(p, zand, 2);
                        iets = true;
                    }
                    if (lagen > 0 && vrij(level.getBlockState(p.set(x, y, z))) && !teHoog(y)) {
                        level.setBlock(p, laag.setValue(SnowLayerBlock.LAYERS, lagen), 2);
                        iets = true;
                    }
                }
            }
            return iets;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * Kruimelwolken: a crumb that floats: a small islet of kruimelrots under a cap of kaaskorst, with a few smaller crumbs
     * drifting next to it. Only in open air.
     */
    public static class Kruimeleilandje extends Basis {
        @Override
        boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random) {
            float r = 2.5f + random.nextFloat() * 3.0f;
            if (teHoog(origin.getY() + 3) || !eilandje(level, origin, random, r, true)) {
                return false;
            }
            for (int i = 0; i < random.nextInt(4); i++) {
                BlockPos naast = origin.offset(random.nextInt(15) - 7, random.nextInt(9) - 5, random.nextInt(15) - 7);
                eilandje(level, naast, random, 1.0f + random.nextFloat() * 1.2f, false);
            }
            return true;
        }

        private static boolean eilandje(WorldGenLevel level, BlockPos top, RandomSource random, float r, boolean gras) {
            int ri = Mth.ceil(r);
            float diep = r * (0.8f + random.nextFloat() * 0.6f);
            int di = Mth.ceil(diep) + 1;
            if (top.getY() - di < level.getMinY() + 8 || teHoog(top.getY() + 2)) {
                return false;
            }
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int dx = -ri - 1; dx <= ri + 1; dx++) {
                for (int dz = -ri - 1; dz <= ri + 1; dz++) {
                    for (int dy = -di; dy <= 2; dy++) {
                        if (!level.getBlockState(p.set(top.getX() + dx, top.getY() + dy, top.getZ() + dz)).isAir()) {
                            return false;
                        }
                    }
                }
            }
            if (bijGebouw(level, top.getX() - ri, top.getY() - di, top.getZ() - ri, top.getX() + ri, top.getY() + 2, top.getZ() + ri)) {
                return false;
            }
            BlockState korst = GuheindeFeature.KAASKORST.get().defaultBlockState();
            BlockState rots = E2BiomesFeature.KRUIMELROTS.get().defaultBlockState();
            float fase = random.nextFloat() * 6.28f;
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz) / (r * (0.85 + 0.15 * Math.sin(Math.atan2(dz, dx) * 3 + fase)));
                    if (d > 1) {
                        continue;
                    }
                    int onder = (int) Math.round(diep * Math.pow(1 - d * d, 0.7) * (0.8 + random.nextFloat() * 0.4));
                    for (int dy = 0; dy >= -onder; dy--) {
                        level.setBlock(p.set(top.getX() + dx, top.getY() + dy, top.getZ() + dz), dy == 0 || random.nextInt(5) == 0 ? korst : rots, 2);
                    }
                    if (gras && random.nextInt(6) == 0) {
                        level.setBlock(p.set(top.getX() + dx, top.getY() + 1, top.getZ() + dz), E2BiomesFeature.KORSTGRAS.get().defaultBlockState(), 2);
                    }
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Kruimelwolken: a cloud of kruimelwolk, a few flat puffs drifting into each other. Only in open air. */
    public static class Wolk extends Basis {
        @Override
        boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random) {
            if (teHoog(origin.getY() + 4) || bijGebouw(level, origin.getX() - BEREIK, origin.getY() - 4, origin.getZ() - BEREIK,
                    origin.getX() + BEREIK, origin.getY() + 4, origin.getZ() + BEREIK)) {
                return false;
            }
            BlockState wolk = E2BiomesFeature.KRUIMELWOLK.get().defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean iets = false;
            double cx = origin.getX() + 0.5, cy = origin.getY() + 0.5, cz = origin.getZ() + 0.5;
            int n = 2 + random.nextInt(3);
            for (int k = 0; k < n; k++) {
                float rx = 2.5f + random.nextFloat() * 3.0f, ry = 1.1f + random.nextFloat() * 1.0f, rz = 2.2f + random.nextFloat() * 2.6f;
                for (int dx = -Mth.ceil(rx); dx <= Mth.ceil(rx); dx++) {
                    for (int dz = -Mth.ceil(rz); dz <= Mth.ceil(rz); dz++) {
                        for (int dy = -Mth.ceil(ry); dy <= Mth.ceil(ry); dy++) {
                            p.set(Mth.floor(cx) + dx, Mth.floor(cy) + dy, Mth.floor(cz) + dz);
                            double ex = (p.getX() + 0.5 - cx) / rx, ey = (p.getY() + 0.5 - cy) / ry, ez = (p.getZ() + 0.5 - cz) / rz;
                            if (ex * ex + ey * ey + ez * ez <= 1 && Math.abs(p.getX() - origin.getX()) <= BEREIK && Math.abs(p.getZ() - origin.getZ()) <= BEREIK
                                    && !teHoog(p.getY()) && level.getBlockState(p).isAir()) {
                                level.setBlock(p, wolk, 2);
                                iets = true;
                            }
                        }
                    }
                }
                cx += (random.nextFloat() - 0.5) * 7;
                cy += random.nextInt(3) - 1;
                cz += (random.nextFloat() - 0.5) * 6;
            }
            return iets;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Schimmelkaasvlakte: a giant glowing mushroom: a pale stem under a blue cap with a hanging rim. */
    public static class Schimmelreus extends Basis {
        @Override
        boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random) {
            int t = vloer(level, origin.getX(), origin.getY(), origin.getZ(), 6);
            if (t == Integer.MIN_VALUE) {
                return false;
            }
            int hoog = 4 + random.nextInt(6);
            int straal = hoog >= 7 ? 3 : 2;
            if (teHoog(t + hoog + 2)) {
                return false;
            }
            BlockPos voet = new BlockPos(origin.getX(), t, origin.getZ());
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int y = 0; y <= hoog + 1; y++) {
                int rr = y >= hoog - 1 ? straal : 0;
                for (int dx = -rr; dx <= rr; dx++) {
                    for (int dz = -rr; dz <= rr; dz++) {
                        if (!vrij(level.getBlockState(p.set(voet.getX() + dx, voet.getY() + y, voet.getZ() + dz)))) {
                            return false;
                        }
                    }
                }
            }
            if (BouwRuimte.inBuilding(level, voet)) {
                return false;
            }
            BlockState steel = E2BiomesFeature.SCHIMMELSTEEL.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            BlockState hoed = E2BiomesFeature.SCHIMMELHOED.get().defaultBlockState();
            for (int y = 0; y < hoog; y++) {
                level.setBlock(voet.above(y), steel, 2);
            }
            for (int dx = -straal; dx <= straal; dx++) {
                for (int dz = -straal; dz <= straal; dz++) {
                    boolean hoek = Math.abs(dx) == straal && Math.abs(dz) == straal;
                    boolean rand = Math.max(Math.abs(dx), Math.abs(dz)) == straal;
                    if (hoek) {
                        continue;
                    }
                    level.setBlock(voet.offset(dx, hoog, dz), hoed, 2);
                    // the rim hangs down one block, with a gap here and there
                    if (rand && random.nextInt(5) != 0) {
                        level.setBlock(voet.offset(dx, hoog - 1, dz), hoed, 2);
                    }
                    if (straal == 3 && Math.max(Math.abs(dx), Math.abs(dz)) <= 1) {
                        level.setBlock(voet.offset(dx, hoog + 1, dz), hoed, 2);
                    }
                }
            }
            // little ones at its foot
            BlockState zwam = E2BiomesFeature.SCHIMMELPADDENSTOEL.get().defaultBlockState();
            for (int i = 0; i < 2 + random.nextInt(4); i++) {
                int x = voet.getX() + random.nextInt(7) - 3, z = voet.getZ() + random.nextInt(7) - 3;
                int y = vloer(level, x, voet.getY(), z, 3);
                if (y != Integer.MIN_VALUE && level.getBlockState(p.set(x, y, z)).isAir()) {
                    level.setBlock(p, zwam, 2);
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * Kristalkaasklippen: a stand of cliff columns of old klipkaas, tall and tapering, a little askew, with veins of
     * kristalkaas in them and golden clusters growing on their sides and tips.
     */
    public static class Kristalspits extends Basis {
        @Override
        boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random) {
            int t0 = vloer(level, origin.getX(), origin.getY(), origin.getZ(), 6);
            if (t0 == Integer.MIN_VALUE || bijGebouw(level, origin.getX() - BEREIK, t0 - 3, origin.getZ() - BEREIK, origin.getX() + BEREIK, t0 + 32, origin.getZ() + BEREIK)) {
                return false;
            }
            int n = 3 + random.nextInt(4);
            boolean iets = false;
            for (int i = 0; i < n; i++) {
                int x = origin.getX() + (i == 0 ? 0 : random.nextInt(13) - 6), z = origin.getZ() + (i == 0 ? 0 : random.nextInt(13) - 6);
                int t = vloer(level, x, t0, z, 5);
                if (t == Integer.MIN_VALUE) {
                    continue;
                }
                // one tall, thick column in the middle and lower ones leaning around it
                int hoog = (i == 0 ? 16 : 6) + random.nextInt(i == 0 ? 15 : 13);
                iets |= spits(level, origin, new BlockPos(x, t, z), random, hoog, 1.6f + random.nextFloat() * 1.8f + (i == 0 ? 1.0f : 0));
            }
            return iets;
        }

        private static boolean spits(WorldGenLevel level, BlockPos origin, BlockPos voet, RandomSource random, int hoog, float r0) {
            BlockState klip = E2BiomesFeature.KLIPKAAS.get().defaultBlockState();
            BlockState erts = E2BiomesFeature.KRISTALKAAS_ERTS.get().defaultBlockState();
            BlockState kristal = E2BiomesFeature.KRISTALKAAS_KRISTAL.get().defaultBlockState();
            double scheefX = (random.nextFloat() - 0.5) * 0.2, scheefZ = (random.nextFloat() - 0.5) * 0.2;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int top = Integer.MIN_VALUE;
            for (int y = -2; y <= hoog; y++) {
                if (teHoog(voet.getY() + y)) {
                    break;
                }
                double r = y < 0 ? r0 : r0 * Math.pow(1.0 - y / (double) (hoog + 1), 0.8) + 0.3;
                double cx = voet.getX() + 0.5 + scheefX * Math.max(y, 0), cz = voet.getZ() + 0.5 + scheefZ * Math.max(y, 0);
                int ri = Mth.ceil(r);
                for (int dx = -ri; dx <= ri; dx++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        p.set(Mth.floor(cx) + dx, voet.getY() + y, Mth.floor(cz) + dz);
                        double ex = p.getX() + 0.5 - cx, ez = p.getZ() + 0.5 - cz;
                        if (ex * ex + ez * ez > r * r || Math.abs(p.getX() - origin.getX()) > BEREIK || Math.abs(p.getZ() - origin.getZ()) > BEREIK) {
                            continue;
                        }
                        BlockState s = level.getBlockState(p);
                        if (vrij(s) || s.is(kristal.getBlock())) {
                            level.setBlock(p, random.nextInt(60) == 0 ? erts : klip, 2);
                            top = Math.max(top, p.getY());
                        }
                    }
                }
            }
            if (top == Integer.MIN_VALUE) {
                return false;
            }
            // clusters: one on the tip, a few on the sides
            int tx = Mth.floor(voet.getX() + 0.5 + scheefX * (top - voet.getY())), tz = Mth.floor(voet.getZ() + 0.5 + scheefZ * (top - voet.getY()));
            if (level.getBlockState(p.set(tx, top, tz)).is(klip.getBlock()) && level.getBlockState(p.set(tx, top + 1, tz)).isAir() && !teHoog(top + 1)) {
                level.setBlock(p, kristal.setValue(AmethystClusterBlock.FACING, Direction.UP), 2);
            }
            for (int i = 0; i < 2 + hoog / 4; i++) {
                int y = voet.getY() + 1 + random.nextInt(Math.max(1, top - voet.getY()));
                Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                double cx = voet.getX() + 0.5 + scheefX * (y - voet.getY()), cz = voet.getZ() + 0.5 + scheefZ * (y - voet.getY());
                p.set(Mth.floor(cx), y, Mth.floor(cz));
                // walk out of the column to its skin on this side
                int stap = 0;
                while (stap++ < 6 && level.getBlockState(p).is(klip.getBlock())) {
                    p.move(d);
                }
                if (stap > 1 && level.getBlockState(p).isAir() && Math.abs(p.getX() - origin.getX()) <= BEREIK && Math.abs(p.getZ() - origin.getZ()) <= BEREIK) {
                    level.setBlock(p, kristal.setValue(AmethystClusterBlock.FACING, d), 2);
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * Knabbelkoorbos: a whole knabbelkoor, grown at once ({@link Knabbelkoor#groei}). The wild one stands on the ground of
     * the islands in the first free cell at its origin; the cultivated one ({@code gekweekt}) grows from exactly its origin,
     * in place of the young flower that stood there.
     */
    public static class KnabbelkoorBoom extends Basis {
        private final boolean gekweekt;

        public KnabbelkoorBoom(boolean gekweekt) {
            this.gekweekt = gekweekt;
        }

        @Override
        boolean maak(WorldGenLevel level, BlockPos origin, RandomSource random) {
            BlockPos voet = origin;
            if (!this.gekweekt) {
                int t = vloer(level, origin.getX(), origin.getY(), origin.getZ(), 6);
                if (t == Integer.MIN_VALUE) {
                    return false;
                }
                voet = new BlockPos(origin.getX(), t, origin.getZ());
                if (!level.getBlockState(voet).isAir() && !vrij(level.getBlockState(voet)) || BouwRuimte.inBuilding(level, voet)) {
                    return false;
                }
            } else if (!level.getBlockState(voet).is(E2BiomesFeature.KNABBELKOORBLOEM.get()) && !level.getBlockState(voet).isAir()) {
                return false;
            }
            BlockState onder = level.getBlockState(voet.below());
            if (!onder.is(Knabbelkoor.GROND) && !onder.is(E2BiomesFeature.KNABBELKOORPLANT.get()) || !level.isEmptyBlock(voet.above()) || teHoog(voet.getY() + 24)) {
                return false;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (!vrij(level.getBlockState(voet.relative(d)))) {
                    return false;
                }
                if (!level.getBlockState(voet.relative(d)).isAir()) {
                    level.setBlock(voet.relative(d), Blocks.AIR.defaultBlockState(), 2);      // (a plant makes room: the stem wants free sides)
                }
            }
            Knabbelkoor.groei(level, voet, random, 8);
            return true;
        }
    }

    private E2BiomesWorldgen() {
    }
}
