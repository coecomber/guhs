package nl.juiced.guhs.world;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import nl.juiced.guhs.registry.ModStructureTypes;

/**
 * A normal jigsaw structure ("jigsaw"), but it only goes where the ground is flat enough: the surface height is sampled
 * on a 5x5 grid around the start, and if the highest and lowest point differ more than max_height_difference, it
 * doesn't generate there. So guh structures don't slice through half a mountain or hang over a valley.
 * (check_radius 0: no flatness check, for the underground ones.)
 * <p>
 * keep_clear (blocks: how far its pieces reach from the middle of the start chunk) keeps guh structures from growing
 * into each other, see {@link BouwRuimte}; voorrang (default keep_clear) says who goes first.
 */
public class FlatJigsawStructure extends Structure implements BouwRuimte.Ruimte {
    public static final MapCodec<FlatJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Structure.DIRECT_CODEC.fieldOf("jigsaw").forGetter(s -> s.jigsaw),
            Codec.INT.fieldOf("check_radius").forGetter(s -> s.checkRadius),
            Codec.INT.fieldOf("max_height_difference").forGetter(s -> s.maxHeightDifference),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, FlatJigsawStructure::new));

    private final Structure jigsaw;
    private final int checkRadius;
    private final int maxHeightDifference;
    private final int keepClear;
    private final Optional<Integer> voorrang;

    public FlatJigsawStructure(StructureSettings settings, Structure jigsaw, int checkRadius, int maxHeightDifference, int keepClear,
                               Optional<Integer> voorrang) {
        super(settings);
        this.jigsaw = jigsaw;
        this.checkRadius = checkRadius;
        this.maxHeightDifference = maxHeightDifference;
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
        // (1.1.2: a guaranteed copy that found no flat enough spot on its whole ring may take a hillier one, see GegarandeerdPlacement)
        if (!vlakGenoeg(context.chunkGenerator(), context.randomState(), context.heightAccessor(), context.chunkPos(),
                checkRadius > 0 ? BouwRuimte.vlakFactor(context, this) : 1)) {
            return Optional.empty();
        }
        return BouwRuimte.claim(context, this, jigsaw.findValidGenerationPoint(context));
    }

    /** Is the ground around this chunk flat enough (the surface on a 5x5 grid differs at most max_height_difference x factor)? */
    public boolean vlakGenoeg(net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.world.level.levelgen.RandomState random,
                              net.minecraft.world.level.LevelHeightAccessor height, ChunkPos chunk, int factor) {
        if (checkRadius <= 0) {
            return true;
        }
        int x0 = chunk.getMiddleBlockX(), z0 = chunk.getMiddleBlockZ(), max = maxHeightDifference * Math.max(1, factor);
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                int h = generator.getBaseHeight(x0 + i * checkRadius / 2, z0 + j * checkRadius / 2, Heightmap.Types.WORLD_SURFACE_WG, height, random);
                low = Math.min(low, h);
                high = Math.max(high, h);
                if (high - low > max) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.FLAT_JIGSAW.get();
    }
}
