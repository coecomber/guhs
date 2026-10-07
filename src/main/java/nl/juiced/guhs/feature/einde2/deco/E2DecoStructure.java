package nl.juiced.guhs.feature.einde2.deco;

import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * A decoration building of the outer islands of the Guheinde ({@code guhs:einde2_deco_bouwsel}): a jigsaw structure of one
 * piece, like guhs:guheinde_eiland (only far from the main island, only where the island is really there), but it looks at
 * the ground under the PIECE itself instead of around the middle of the chunk: the piece is centred on the start, which is
 * the corner of the chunk, and it is small, so a look around the middle of the chunk misses half of it. Nine points over
 * the footprint of the piece (its middle, the middles of its sides, its corners a little inside) must all hold island at
 * min_surface_y or higher, and the highest and the lowest may differ at most max_verschil blocks: a building never hangs
 * over the rim of an island, and never stands half in a hill. Then it claims its room (BouwRuimte: keep_clear, voorrang).
 */
public class E2DecoStructure extends Structure implements BouwRuimte.Ruimte {
    public static final MapCodec<E2DecoStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Structure.DIRECT_CODEC.fieldOf("jigsaw").forGetter(s -> s.jigsaw),
            Codec.INT.fieldOf("min_distance").forGetter(s -> s.minDistance),
            Codec.INT.fieldOf("min_surface_y").forGetter(s -> s.minSurfaceY),
            Codec.INT.fieldOf("max_verschil").forGetter(s -> s.maxVerschil),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, E2DecoStructure::new));

    private final Structure jigsaw;
    private final int minDistance;
    private final int minSurfaceY;
    private final int maxVerschil;
    /** Room kept around the build (see BouwRuimte). */
    private final int keepClear;
    private final Optional<Integer> voorrang;

    public E2DecoStructure(StructureSettings settings, Structure jigsaw, int minDistance, int minSurfaceY, int maxVerschil, int keepClear,
                           Optional<Integer> voorrang) {
        super(settings);
        this.jigsaw = jigsaw;
        this.minDistance = minDistance;
        this.minSurfaceY = minSurfaceY;
        this.maxVerschil = maxVerschil;
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

    public int maxVerschil() {
        return maxVerschil;
    }

    public int minSurfaceY() {
        return minSurfaceY;
    }

    /** The nine points of a footprint the ground is looked at: the middle, the middles of the sides, the corners a little inside. */
    public static int[][] punten(BoundingBox doos) {
        int mx = (doos.minX() + doos.maxX()) / 2, mz = (doos.minZ() + doos.maxZ()) / 2;
        int rx = Math.max(1, doos.getXSpan() / 2 - 2), rz = Math.max(1, doos.getZSpan() / 2 - 2);
        int[][] uit = new int[9][];
        int n = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                // (the footprints are round: a corner lies a little inside)
                double f = i != 0 && j != 0 ? 0.75 : 1.0;
                uit[n++] = new int[]{mx + (int) Math.round(i * rx * f), mz + (int) Math.round(j * rz * f)};
            }
        }
        return uit;
    }

    private static int grond(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        // (the piece is centred on the start: the north-west corner of the chunk)
        long x = chunk.getMinBlockX(), z = chunk.getMinBlockZ();
        if (x * x + z * z < (long) minDistance * minDistance) {
            return Optional.empty();
        }
        // (the cheapest look first: most chunks that come by are not in this building's biome at all)
        if (!context.validBiome().test(context.chunkGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock((int) x), QuartPos.fromBlock(64),
                QuartPos.fromBlock((int) z), context.randomState().sampler())) || grond(context, (int) x, (int) z) < minSurfaceY) {
            return Optional.empty();
        }
        Optional<GenerationStub> stub = jigsaw.findValidGenerationPoint(context);
        if (stub.isEmpty()) {
            return stub;
        }
        // (the pieces are built once, here, and handed on: the turn of the piece decides its footprint)
        StructurePiecesBuilder builder = stub.get().getPiecesBuilder();
        if (builder.isEmpty()) {
            return Optional.empty();
        }
        int laag = Integer.MAX_VALUE, hoog = Integer.MIN_VALUE;
        for (int[] p : punten(builder.getBoundingBox())) {
            int h = grond(context, p[0], p[1]);
            laag = Math.min(laag, h);
            hoog = Math.max(hoog, h);
            if (h < minSurfaceY || hoog - laag > maxVerschil) {
                return Optional.empty();
            }
        }
        return BouwRuimte.claim(context, this, Optional.of(new GenerationStub(stub.get().position(), Either.right(builder))));
    }

    @Override
    public StructureType<?> type() {
        return E2DecoFeature.BOUWSEL.get();
    }
}
