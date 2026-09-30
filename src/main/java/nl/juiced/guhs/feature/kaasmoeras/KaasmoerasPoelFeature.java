package nl.juiced.guhs.feature.kaasmoeras;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * A pool of the kaasmoeras: a roundish hole with a kaasmodder bottom and rim, kaasriet and moerasgras along the edge.
 * <ul>
 *   <li>a water pool (lily pads, sometimes a few blobs of borrelende kaassaus bubbling in it);</li>
 *   <li>a borrelplas: a pool of pure borrelende kaassaus (bounce!);</li>
 *   <li>now and then a big water pool gets a knabbelvlotje: a little raft with a mast, a guh flag, a lampion and a
 *       barrel of loot (loot table guhs:chests/knabbelvlotje).</li>
 * </ul>
 * Only on (roughly) flat ground, so the water stays in, and never into a building.
 */
public class KaasmoerasPoelFeature extends Feature<NoneFeatureConfiguration> {
    /** Out of this many big water pools, one gets a knabbelvlotje. */
    public static final int VLOTJE_CHANCE = 5;

    public enum Kind { WATER, BORREL, VLOTJE }

    public KaasmoerasPoelFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource random = context.random();
        int roll = random.nextInt(100);
        Kind kind = roll < 30 ? Kind.BORREL : Kind.WATER;
        double rx = 3 + random.nextInt(4), rz = 3 + random.nextInt(4);
        if (kind == Kind.WATER && rx >= 5 && rz >= 5 && random.nextInt(VLOTJE_CHANCE) == 0) {
            kind = Kind.VLOTJE;
            rx += 1;
            rz += 1;
        }
        if (kind == Kind.BORREL) {
            rx -= 1;
            rz -= 1;
        }
        return place(context.level(), random, context.origin(), kind, rx, rz, Heightmap.Types.WORLD_SURFACE_WG);
    }

    /**
     * Makes a pool of this kind around origin (x, z); false if the ground is too uneven. The ground height comes from the
     * heightmap surface, or (surface null) is origin's y everywhere (the first air above the ground).
     */
    public static boolean place(WorldGenLevel level, RandomSource random, BlockPos origin, Kind kind, double rx, double rz,
                                @javax.annotation.Nullable Heightmap.Types surface) {
        double wobble = random.nextDouble() * Math.PI;
        int r = (int) Math.ceil(Math.max(rx, rz)) + 2;
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dist(dx, dz, rx + 1.5, rz + 1.5, wobble) <= 1) {
                    int h = surface == null ? origin.getY() : level.getHeight(surface, origin.getX() + dx, origin.getZ() + dz);
                    low = Math.min(low, h);
                    high = Math.max(high, h);
                }
            }
        }
        if (high - low > 2 || low <= level.getMinBuildHeight() + 4) {
            return false;
        }
        int top = low - 1;   // the water (or the bubbling cheese) is level with the lowest ground around
        // (never into a building: the buildings of the chunks around are already there when this runs)
        if (nl.juiced.guhs.world.BouwRuimte.touchesBuilding(level, origin.getX() - r - 1, top - 4, origin.getZ() - r - 1,
                origin.getX() + r + 1, high + 6, origin.getZ() + r + 1)) {
            return false;
        }
        BlockState modder = KaasmoerasFeature.KAASMODDER.get().defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState borrel = KaasmoerasFeature.BORRELENDE_KAASSAUS.get().defaultBlockState();
        boolean blobs = kind == Kind.WATER && random.nextInt(3) == 0;
        double rim = 1 + 1.8 / Math.min(rx, rz);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = dist(dx, dz, rx, rz, wobble);
                BlockPos column = new BlockPos(origin.getX() + dx, top, origin.getZ() + dz);
                if (d <= 1) {
                    for (int y = 1; y <= 4; y++) {
                        BlockState above = level.getBlockState(column.above(y));
                        if (!above.is(net.minecraft.tags.BlockTags.LOGS)) {
                            level.setBlock(column.above(y), Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                    if (kind == Kind.BORREL) {
                        level.setBlock(column, borrel, 2);
                        level.setBlock(column.below(), modder, 2);
                        level.setBlock(column.below(2), modder, 2);
                        continue;
                    }
                    int depth = d < 0.35 ? 3 : d < 0.7 ? 2 : 1;
                    for (int y = 0; y < depth; y++) {
                        level.setBlock(column.below(y), water, 2);
                    }
                    level.setBlock(column.below(depth), modder, 2);
                    if (blobs && d < 0.3 && random.nextInt(3) == 0) {
                        level.setBlock(column, borrel, 2);
                    } else if (d > 0.25 && random.nextInt(6) == 0 && !(kind == Kind.VLOTJE && d < 0.45)) {
                        BlockState pad = random.nextInt(4) == 0 ? ModBlocks.GUH_WATERLELIE.get().defaultBlockState()
                                : Blocks.LILY_PAD.defaultBlockState();
                        level.setBlock(column.above(), pad, 2);
                    }
                } else if (d <= rim) {
                    level.setBlock(column, modder, 2);
                    BlockPos plant = column.above();
                    if (level.getBlockState(plant).isAir() || level.getBlockState(plant).canBeReplaced()) {
                        int p = random.nextInt(10);
                        BlockState riet = KaasmoerasFeature.KAASRIET.get().defaultBlockState();
                        if (p < 3 && level.getBlockState(plant.above()).isAir() && riet.canSurvive(level, plant)) {
                            DoublePlantBlock.placeAt(level, riet, plant, 2);
                        } else if (p < 5) {
                            level.setBlock(plant, KaasmoerasFeature.MOERASGRAS.get().defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
        if (kind == Kind.VLOTJE) {
            vlotje(level, random, new BlockPos(origin.getX(), top, origin.getZ()));
        }
        return true;
    }

    /**
     * A knabbelvlotje floating on the water at y = top: a 4 x 3 raft of (waterlogged) top slabs with a mast, a guh flag,
     * a lampion and a barrel of loot.
     */
    public static void vlotje(WorldGenLevel level, RandomSource random, BlockPos centre) {
        BlockState slab = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        BlockState edge = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        for (int dx = -2; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(centre.offset(dx, 0, dz), slab, 2);
                for (int y = 1; y <= 4; y++) {
                    level.setBlock(centre.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // two logs along the sides: the raft's floaters (they stick out a little over the water)
        for (int dx = -2; dx <= 1; dx++) {
            level.setBlock(centre.offset(dx, 0, -2), edge, 2);
            level.setBlock(centre.offset(dx, 0, 2), edge, 2);
        }
        // the mast with a pink guh flag on top and a pink wool sail
        BlockPos mast = centre.offset(0, 1, 0);
        for (int y = 0; y < 3; y++) {
            level.setBlock(mast.above(y), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
        }
        level.setBlock(mast.above(3), Blocks.PINK_BANNER.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, random.nextInt(16)), 2);
        level.setBlock(mast.offset(-1, 1, 0), Blocks.PINK_WOOL.defaultBlockState(), 2);
        level.setBlock(mast.offset(-1, 2, 0), Blocks.WHITE_WOOL.defaultBlockState(), 2);
        // a yellow lampion on a post, the barrel with the loot and a pile of stolen kaasknabbels
        level.setBlock(centre.offset(1, 1, 1), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
        BlockState lampion = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(nl.juiced.guhs.Guhs.id("lampion_geel")).defaultBlockState();
        level.setBlock(centre.offset(1, 2, 1), lampion, 2);
        BlockPos barrel = centre.offset(-2, 1, -1);
        level.setBlock(barrel, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 2);
        RandomizableContainer.setBlockEntityLootTable(level, random, barrel, KaasmoerasEvents.VLOTJE_LOOT);
        level.setBlock(centre.offset(-2, 1, 1), ModBlocks.BLOCK_OF_KAASKNABBELS.get().defaultBlockState(), 2);
    }

    static double dist(int dx, int dz, double rx, double rz, double wobble) {
        double a = Math.atan2(dz, dx);
        double w = 1 + 0.2 * Math.sin(a * 3 + wobble) + 0.08 * Math.sin(a * 5 + wobble * 2);
        return Math.sqrt((dx / rx) * (dx / rx) + (dz / rz) * (dz / rz)) / w;
    }
}
