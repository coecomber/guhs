package nl.juiced.guhs.feature.verhaal.wereld;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * 3.0: the warm turquoise water of Guhwai'i. The Guhmension has no sea level, so the island region brings its own water (like
 * the Diepe Guhzee's DiepzeeWaterFeature): in every column of the chunk that lies in the region (its {@link Regio}: the
 * guhwaii noise at least {@code min_value}, the masks passing), the air from {@code water_level - 1} down to the first solid
 * block becomes water. It can't leak: the router (tools/features/verhaal_wereld.py) raises a rim of sand to the water level
 * around the region, over the band of the noise just below min_value. Runs in the "lakes" step.
 */
public class GuhwaiiWaterFeature extends Feature<GuhwaiiWaterFeature.Config> {
    public record Config(Regio regio, double minValue, int waterLevel) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Regio.CODEC.fieldOf("regio").forGetter(Config::regio),
                Codec.DOUBLE.fieldOf("min_value").forGetter(Config::minValue),
                Codec.INT.fieldOf("water_level").forGetter(Config::waterLevel)
        ).apply(i, Config::new));
    }

    public GuhwaiiWaterFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        return fill(context.level(), context.level().getSeed(), new ChunkPos(context.origin()), context.config()) > 0;
    }

    /** Fills the region's columns of this chunk; returns how many water blocks were placed. */
    public static int fill(WorldGenLevel level, long seed, ChunkPos chunk, Config c) {
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int placed = 0;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunk.getMinBlockX() + dx, z = chunk.getMinBlockZ() + dz;
                if (c.regio().waarde(seed, x, z) < c.minValue()) {
                    continue;
                }
                for (int y = c.waterLevel() - 1; y > level.getMinBuildHeight(); y--) {
                    p.set(x, y, z);
                    if (!level.getBlockState(p).isAir()) {
                        break;
                    }
                    level.setBlock(p, water, 2);
                    placed++;
                }
            }
        }
        return placed;
    }
}
