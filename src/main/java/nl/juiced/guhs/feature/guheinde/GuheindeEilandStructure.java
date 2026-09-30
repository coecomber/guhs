package nl.juiced.guhs.feature.guheinde;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A structure of the Guheinde's outer islands (the Mika-vesting), like the end city: a normal jigsaw structure, but only
 * far from the main island (min_distance blocks from 0,0) and only where the island is really there and high enough:
 * the ground around the start (a 3x3 grid, check_radius apart) must be at least min_surface_y everywhere.
 */
public class GuheindeEilandStructure extends Structure implements nl.juiced.guhs.world.BouwRuimte.Ruimte {
    public static final MapCodec<GuheindeEilandStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Structure.DIRECT_CODEC.fieldOf("jigsaw").forGetter(s -> s.jigsaw),
            Codec.INT.fieldOf("min_distance").forGetter(s -> s.minDistance),
            Codec.INT.fieldOf("min_surface_y").forGetter(s -> s.minSurfaceY),
            Codec.INT.optionalFieldOf("check_radius", 8).forGetter(s -> s.checkRadius),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, GuheindeEilandStructure::new));

    private final Structure jigsaw;
    private final int minDistance;
    private final int minSurfaceY;
    private final int checkRadius;
    /** Room kept around the build (see BouwRuimte). */
    private final int keepClear;
    private final Optional<Integer> voorrang;

    public GuheindeEilandStructure(StructureSettings settings, Structure jigsaw, int minDistance, int minSurfaceY, int checkRadius,
                                   int keepClear, Optional<Integer> voorrang) {
        super(settings);
        this.jigsaw = jigsaw;
        this.minDistance = minDistance;
        this.minSurfaceY = minSurfaceY;
        this.checkRadius = checkRadius;
        this.keepClear = keepClear;
        this.voorrang = voorrang;
    }

    @Override
    public int keepClear() {
        return keepClear;
    }

    @Override
    public int voorrang() {
        return voorrang.orElse(keepClear);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        long x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        if (x * x + z * z < (long) minDistance * minDistance) {
            return Optional.empty();
        }
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int h = context.chunkGenerator().getBaseHeight((int) x + i * checkRadius, (int) z + j * checkRadius, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState());
                if (h < minSurfaceY) {
                    return Optional.empty();
                }
            }
        }
        return nl.juiced.guhs.world.BouwRuimte.claim(context, this, jigsaw.findValidGenerationPoint(context));
    }

    @Override
    public StructureType<?> type() {
        return GuheindeFeature.EILAND_STRUCTURE.get();
    }
}
