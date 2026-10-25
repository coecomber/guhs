package nl.juiced.guhs.world;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.StructureType;
import nl.juiced.guhs.registry.ModStructureTypes;
import nl.juiced.guhs.world.grond.GrondPoolElement;

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
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang),
            Codec.BOOL.optionalFieldOf("hut_op_grond", false).forGetter(s -> s.hutOpGrond)
    ).apply(i, FlatJigsawStructure::new));

    private final Structure jigsaw;
    private final int checkRadius;
    private final int maxHeightDifference;
    private final int keepClear;
    private final Optional<Integer> voorrang;
    /**
     * (1.7.0 the cave minigames) every piece after the start piece with its own ground (a guhs:grond_single_pool_element:
     * the entrance hut over a cave hall) goes up or down to the ground at its own spot, not the ground at the middle of the
     * start piece. The Wolkenliftje reaches straight up and down through the rock, so the shaft in between may get longer
     * or shorter.
     */
    private final boolean hutOpGrond;

    public FlatJigsawStructure(StructureSettings settings, Structure jigsaw, int checkRadius, int maxHeightDifference, int keepClear,
                               Optional<Integer> voorrang) {
        this(settings, jigsaw, checkRadius, maxHeightDifference, keepClear, voorrang, false);
    }

    public FlatJigsawStructure(StructureSettings settings, Structure jigsaw, int checkRadius, int maxHeightDifference, int keepClear,
                               Optional<Integer> voorrang, boolean hutOpGrond) {
        super(settings);
        this.hutOpGrond = hutOpGrond;
        this.jigsaw = jigsaw;
        this.checkRadius = checkRadius;
        this.maxHeightDifference = maxHeightDifference;
        this.keepClear = keepClear;
        this.voorrang = voorrang;
    }

    /** F1b: the jigsaw inside (a structure placed by hand by an op skips the flatness and room checks). */
    public Structure jigsaw() {
        return jigsaw;
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
        Optional<GenerationStub> stub = jigsaw.findValidGenerationPoint(context);
        if (hutOpGrond && stub.isPresent()) {
            StructurePiecesBuilder builder = stub.get().getPiecesBuilder();
            opGrond(builder.build().pieces(), (x, z) -> context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState()));
            stub = Optional.of(new GenerationStub(stub.get().position(), Either.right(builder)));
        }
        return BouwRuimte.claim(context, this, stub);
    }

    /**
     * Puts every piece after the first that has its own ground ({@link GrondPoolElement}) on the ground at the middle of
     * its box: its minY + ground_level_delta becomes the first free height there. Returns how far each moved (tests).
     */
    public static int[] opGrond(List<StructurePiece> pieces, java.util.function.IntBinaryOperator vrij) {
        int[] verplaatst = new int[pieces.size()];
        for (int i = 1; i < pieces.size(); i++) {
            if (pieces.get(i) instanceof PoolElementStructurePiece p && p.getElement() instanceof GrondPoolElement grond) {
                BoundingBox b = p.getBoundingBox();
                int dy = vrij.applyAsInt((b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2) - (b.minY() + grond.groundLevelDelta());
                p.move(0, dy, 0);
                verplaatst[i] = dy;
            }
        }
        return verplaatst;
    }

    /**
     * Is the ground around this chunk flat enough (the surface on a 5x5 grid differs at most max_height_difference x factor)?
     * (1.7.0: the quick {@link ZoekHoogte}, without the caves.)
     */
    public boolean vlakGenoeg(net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.world.level.levelgen.RandomState random,
                              net.minecraft.world.level.LevelHeightAccessor height, ChunkPos chunk, int factor) {
        if (checkRadius <= 0) {
            return true;
        }
        int x0 = chunk.getMiddleBlockX(), z0 = chunk.getMiddleBlockZ(), max = maxHeightDifference * Math.max(1, factor);
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                int h = ZoekHoogte.hoogte(generator, random, height, x0 + i * checkRadius / 2, z0 + j * checkRadius / 2);
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
