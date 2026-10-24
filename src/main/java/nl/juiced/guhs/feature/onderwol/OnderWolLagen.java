package nl.juiced.guhs.feature.onderwol;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.juiced.guhs.Guhs;
import org.jspecify.annotations.Nullable;

/**
 * Surface rule {@code guhs:onderwol_lagen} (speeltest 2, ronde 27): the stone layers under the Guhmension.
 * <ul>
 *   <li>Measured from the real top of each column (the WG heightmap, so also in the Klaterdal and the other biomes3 regions,
 *   whose own terrain model the preliminary surface doesn't know): the top 3-5 blocks are always left to the biome's own
 *   rules, except on low ground where a "kale plek" (bare patch) lets the viltsteen through.</li>
 *   <li>The borders follow the land: they rise under hills and sink under valleys and seas (from the preliminary surface,
 *   lerped like vanilla's, so they stay smooth), wave along {@code guhs:onderwol_golf} and are speckled over 3 blocks.
 *   On average viltsteen from y 64, knuffelsteen from y 40, guhkristalsteen from y 20; the knuffelnaad at the bottom.</li>
 *   <li>More pink in the knuffelsteen: veins of roze knuffelsteen, bubbles of wool left in the stone, and thin stripes of
 *   pastel terracotta.</li>
 * </ul>
 * Everything is deterministic per seed and position.
 */
public record OnderWolLagen() implements SurfaceRules.RuleSource {
    public static final MapCodec<OnderWolLagen> DATA_CODEC = MapCodec.unit(OnderWolLagen::new);
    public static final KeyDispatchDataCodec<OnderWolLagen> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    /** The surface height the average borders belong to. */
    private static final int REF = 72;
    private static final ResourceKey<NormalNoise.NoiseParameters> GOLF = ruis("onderwol_golf");
    private static final ResourceKey<NormalNoise.NoiseParameters> ROZE = ruis("onderwol_roze");
    private static final ResourceKey<NormalNoise.NoiseParameters> WOLBEL = ruis("onderwol_wolbel");
    private static final ResourceKey<NormalNoise.NoiseParameters> KAAL = ruis("onderwol_kaal");
    private static final ResourceKey<NormalNoise.NoiseParameters> STREEP = ruis("onderwol_streep");

    private static ResourceKey<NormalNoise.NoiseParameters> ruis(String naam) {
        return ResourceKey.create(Registries.NOISE, Guhs.id(naam));
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
        return new Regel(context);
    }

    private static BlockState blok(String id) {
        return BuiltInRegistries.BLOCK.getValue(Guhs.id(id)).defaultBlockState();
    }

    private static final class Regel implements SurfaceRules.SurfaceRule {
        private final ChunkAccess chunk;
        private final NoiseChunk noiseChunk;
        private final NormalNoise golf, roze, wolbel, kaal, streep;
        private final BlockState naad = blok("knuffelnaad"), kristal = blok("guh_kristalsteen"), knuffel = blok("knuffelsteen"),
                rozeKnuffel = blok("roze_knuffelsteen"), vilt = blok("viltsteen");
        private static final BlockState[] WOL = {Blocks.PINK_WOOL.defaultBlockState(), Blocks.MAGENTA_WOOL.defaultBlockState()};
        private static final BlockState[] STREPEN = {Blocks.PINK_TERRACOTTA.defaultBlockState(), Blocks.MAGENTA_TERRACOTTA.defaultBlockState(),
                Blocks.WHITE_TERRACOTTA.defaultBlockState()};
        private long kolom = Long.MIN_VALUE;
        private long cel = Long.MIN_VALUE;
        private final int[] hoeken = new int[4];
        private int top, minDiepte, golfY, bVilt, bKnuffel, bKristal;
        private boolean strepen;

        Regel(SurfaceRules.Context c) {
            this.chunk = c.chunk;
            this.noiseChunk = c.noiseChunk;
            this.golf = c.randomState.getOrCreateNoise(GOLF);
            this.roze = c.randomState.getOrCreateNoise(ROZE);
            this.wolbel = c.randomState.getOrCreateNoise(WOLBEL);
            this.kaal = c.randomState.getOrCreateNoise(KAAL);
            this.streep = c.randomState.getOrCreateNoise(STREEP);
        }

        @Override
        public @Nullable BlockState tryApply(int x, int y, int z) {
            long k = (long) x << 32 | (z & 0xffffffffL);
            if (k != kolom) {
                kolom = k;
                kolom(x, z);
            }
            if (y < 3 || y < 6 && kans(x, y, z, 1) < (6 - y) / 4.0) {
                return naad;
            }
            if (top - y < minDiepte) {
                return null;
            }
            if (onder(bKristal, x, y, z, 2)) {
                return kristal;
            }
            if (onder(bKnuffel, x, y, z, 3)) {
                return knuffelsteen(x, y, z);
            }
            return onder(bVilt, x, y, z, 4) ? vilt : null;
        }

        /** Below the border b, speckled over 3 blocks (like a vertical_gradient from b - 2 to b + 1). */
        private static boolean onder(int b, int x, int y, int z, int zout) {
            return y < b - 2 || y < b + 1 && kans(x, y, z, zout) < (b + 1 - y) / 3.0;
        }

        private BlockState knuffelsteen(int x, int y, int z) {
            if (wolbel.getValue(x, y * 1.5, z) > 0.55) {
                return WOL[kans(x, y, z, 5) < 0.7 ? 0 : 1];
            }
            int s = y + golfY;
            if (strepen && Math.floorMod(s, 9) == 0) {
                return STREPEN[Math.floorMod(Math.floorDiv(s, 9), STREPEN.length)];
            }
            // (the y stretched: the veins lie as flattish, wavy bands)
            return Math.abs(roze.getValue(x, y * 3.0, z)) < 0.07 ? rozeKnuffel : knuffel;
        }

        private void kolom(int x, int z) {
            top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            double p = voorlopig(x, z) - REF;
            golfY = Mth.clamp((int) Math.round(golf.getValue(x, 0, z) * 8), -4, 4);
            bVilt = 64 + golfY + Mth.clamp((int) Math.round(p * 0.35), -10, 12);
            bKnuffel = Math.min(40 + golfY + Mth.clamp((int) Math.round(p * 0.25), -8, 8), bVilt - 8);
            bKristal = Math.min(20 + golfY / 2 + Mth.clamp((int) Math.round(p * 0.12), -4, 4), bKnuffel - 8);
            minDiepte = 3 + (int) (kans(x, 0, z, 6) * 3);
            // a bare patch: only on low ground, where the viltsteen comes close to the surface anyway
            if (top < bVilt + 3 && kaal.getValue(x, 0, z) > 0.45) {
                minDiepte = 0;
            }
            strepen = streep.getValue(x, 0, z) > 0.0;
        }

        /** The preliminary surface at x, z, lerped between the corners of its 16x16 cell (as SurfaceRules.Context does). */
        private double voorlopig(int x, int z) {
            int cx = x >> 4, cz = z >> 4;
            long c = (long) cx << 32 | (cz & 0xffffffffL);
            if (c != cel) {
                cel = c;
                hoeken[0] = noiseChunk.preliminarySurfaceLevel(cx << 4, cz << 4);
                hoeken[1] = noiseChunk.preliminarySurfaceLevel((cx + 1) << 4, cz << 4);
                hoeken[2] = noiseChunk.preliminarySurfaceLevel(cx << 4, (cz + 1) << 4);
                hoeken[3] = noiseChunk.preliminarySurfaceLevel((cx + 1) << 4, (cz + 1) << 4);
            }
            return Mth.lerp2((x & 15) / 16.0, (z & 15) / 16.0, hoeken[0], hoeken[1], hoeken[2], hoeken[3]);
        }

        /** A fixed chance in [0, 1) per position. */
        private static double kans(int x, int y, int z, int zout) {
            long h = Mth.getSeed(x, y, z) ^ zout * 0x9E3779B97F4A7C15L;
            h ^= h >>> 33;
            h *= 0xff51afd7ed558ccdL;
            h ^= h >>> 33;
            return (h >>> 11) * 0x1.0p-53;
        }
    }
}
