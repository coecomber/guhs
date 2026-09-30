package nl.juiced.guhs.feature.onderwater;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The water of the Diepe Guhzee. The Guhmension has no sea level (its valleys are dry, and must stay so), so the deep
 * seas bring their own water: in every column of the chunk where the sea noise is at least min_value, all air below
 * water_level becomes water (the sea surface is at water_level - 1).
 * <p>
 * It can't leak: the noise router (tools/features/diepzee.py) puts a dam around every sea, solid up to one block above
 * the surface, over the band of the noise just below min_value. A column next to a sea column is either a sea column
 * too, or in that dam. Runs in the "lakes" step: before the structures (the Guhbubbel) and all other features.
 */
public class DiepzeeWaterFeature extends Feature<DiepzeeWaterFeature.Config> {
    public record Config(ResourceKey<NormalNoise.NoiseParameters> noise, double minValue, int waterLevel) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(Config::noise),
                Codec.DOUBLE.fieldOf("min_value").forGetter(Config::minValue),
                Codec.INT.fieldOf("water_level").forGetter(Config::waterLevel)
        ).apply(i, Config::new));
    }

    public DiepzeeWaterFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        ServerLevel server = level.getLevel();
        NormalNoise noise = server.getChunkSource().randomState().getOrCreateNoise(context.config().noise());
        return fill(level, noise, new ChunkPos(context.origin()), context.config().minValue(), context.config().waterLevel()) > 0;
    }

    /** Fills the sea columns of this chunk; returns how many water blocks were placed. */
    public static int fill(WorldGenLevel level, NormalNoise noise, ChunkPos chunk, double minValue, int waterLevel) {
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int placed = 0;
        int bottom = level.getMinY();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunk.getMinBlockX() + dx, z = chunk.getMinBlockZ() + dz;
                if (noise.getValue(x, 0, z) < minValue) {
                    continue;
                }
                for (int y = bottom; y < waterLevel; y++) {
                    p.set(x, y, z);
                    if (level.getBlockState(p).isAir()) {
                        level.setBlock(p, water, 2);
                        placed++;
                    }
                }
            }
        }
        return placed;
    }
}
