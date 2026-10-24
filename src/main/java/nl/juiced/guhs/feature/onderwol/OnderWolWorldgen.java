package nl.juiced.guhs.feature.onderwol;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * 1.7.0 "Onder de Wol": the piece of the underground that is code, not data (see {@link OnderWolFeature}).
 */
public final class OnderWolWorldgen {
    /** The level the saus sea fills the deep caves up to. */
    public static final int SAUS_Y = 10;
    /** Below this nothing is carved (the knuffelnaad). */
    public static final int BODEM_Y = 5;
    /** How big the saus regions are (blocks per noise unit) and which share of the underground they cover (~25%). */
    private static final double SAUS_SCHAAL = 320.0, SAUS_DREMPEL = 0.38;

    private static volatile SimplexNoise sausRuis;
    private static volatile long sausZaad;

    /** Is (x, z) in a saus region of this world? Pure function of the seed, so neighbouring chunks agree. */
    public static boolean sausgebied(long zaad, int x, int z) {
        SimplexNoise n = sausRuis;
        if (n == null || sausZaad != zaad) {
            n = new SimplexNoise(new LegacyRandomSource(zaad ^ 0x5A05_5A05L));
            sausRuis = n;
            sausZaad = zaad;
        }
        return n.getValue(x / SAUS_SCHAAL, z / SAUS_SCHAAL) > SAUS_DREMPEL;
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * The saus sea: in a saus region every cave space at y {@value #BODEM_Y}..{@value #SAUS_Y} of the chunk fills with kaas
     * saus (placed once per chunk, in the step after the caves are carved). Elsewhere the deep caves stay dry.
     */
    public static class Sauszee extends Feature<NoneFeatureConfiguration> {
        public Sauszee() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            long zaad = level.getSeed();
            int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
            BlockState saus = ModBlocks.KAAS_SAUS.get().defaultBlockState();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            boolean iets = false;
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    if (!sausgebied(zaad, x0 + dx, z0 + dz)) {
                        continue;
                    }
                    for (int y = BODEM_Y; y <= SAUS_Y; y++) {
                        p.set(x0 + dx, y, z0 + dz);
                        if (level.getBlockState(p).isAir()) {
                            level.setBlock(p, saus, Block.UPDATE_CLIENTS);
                            iets = true;
                        }
                    }
                }
            }
            return iets;
        }
    }

    private OnderWolWorldgen() {
    }
}
