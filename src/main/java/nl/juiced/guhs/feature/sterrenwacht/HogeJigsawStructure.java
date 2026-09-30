package nl.juiced.guhs.feature.sterrenwacht;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

/**
 * A normal jigsaw structure (all the fields of {@code minecraft:jigsaw} in the same JSON object) that only starts where
 * the ground is high: the surface at the start and in the middle of the start chunk must be at least
 * {@code min_start_y}. Used as the inner "jigsaw" of the Guh-Sterrenwacht's {@code guhs:flat_jigsaw} (so the flatness
 * check and BouwRuimte stay the shared ones): the sterrenwacht stands HIGH on the Guhpieken and the Vadskliffen, not
 * down in a valley between them.
 */
public class HogeJigsawStructure extends Structure {
    public static final MapCodec<HogeJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            JigsawStructure.CODEC.forGetter(s -> s.jigsaw),
            Codec.INT.fieldOf("min_start_y").forGetter(s -> s.minStartY)
    ).apply(i, HogeJigsawStructure::new));

    private final JigsawStructure jigsaw;
    private final int minStartY;

    public HogeJigsawStructure(JigsawStructure jigsaw, int minStartY) {
        super(new StructureSettings(jigsaw.biomes(), jigsaw.spawnOverrides(), jigsaw.step(), jigsaw.terrainAdaptation()));
        this.jigsaw = jigsaw;
        this.minStartY = minStartY;
    }

    public int minStartY() {
        return minStartY;
    }

    /** Is the ground at this chunk's start (its corner, where the jigsaw starts, and its middle) high enough? */
    public boolean hoogGenoeg(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int[][] punten = {{chunk.getMinBlockX(), chunk.getMinBlockZ()}, {chunk.getMiddleBlockX(), chunk.getMiddleBlockZ()}};
        for (int[] p : punten) {
            int h = context.chunkGenerator().getBaseHeight(p[0], p[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
                    context.randomState());
            if (h < minStartY) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!hoogGenoeg(context)) {
            return Optional.empty();
        }
        return jigsaw.findValidGenerationPoint(context);
    }

    @Override
    public StructureType<?> type() {
        return SterrenwachtFeature.HOGE_JIGSAW.get();
    }
}
