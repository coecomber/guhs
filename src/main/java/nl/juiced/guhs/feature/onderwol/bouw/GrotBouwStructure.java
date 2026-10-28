package nl.juiced.guhs.feature.onderwol.bouw;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.BouwRuimte;
import nl.juiced.guhs.world.ZoekHoogte;

/**
 * Structure type {@code guhs:grot_bouw} (1.7.0 "Onder de Wol"): a building under the ground, in an open space.
 * <p>
 * How a start is made (all of it a pure function of the seed and the chunk, as {@link BouwRuimte} needs):
 * <ol>
 *   <li>the floor height: in the band {@code min_y}..{@code max_y} (the y you stand on). With {@code ruimte} "grot" or
 *       "beide" the column in the middle of the chunk is searched (from the top of the band down) for the floor of a cave
 *       that is already there, with at least {@link #HOOFDRUIMTE} blocks of air above it: the building goes on that floor.
 *       Without one, "grot" gives up and "zaal"/"beide" take a height in the band at random (then the template's air or
 *       the hall below carves the room);</li>
 *   <li>the pieces: a jigsaw from {@code start_pool} (depth {@code size}, at most {@code max_distance} blocks out) whose
 *       start jigsaw {@code start_jigsaw_name} lands IN the floor layer (one below the y you stand on), in the middle of the
 *       chunk;</li>
 *   <li>the checks: every piece stays {@code dekking} blocks under the surface (the terrain without caves, at the corners
 *       and the middle of all pieces together: so also under the sea floor), above y {@link #BODEM} (the knuffelnaad), and
 *       the floor never in the saus sea's height band ({@link #SAUS_TOT});</li>
 *   <li>{@code zaal} (optional): a natural hall around the start piece (or every piece) is carved first
 *       ({@link GrotZaalPiece}): {@code rand} blocks wider than the piece, {@code boven} blocks above its top, wobbling with
 *       {@code ruw};</li>
 *   <li>{@code ingang} (optional): the build holds a jigsaw named {@link #INGANG} (pool minecraft:empty) in its floor layer,
 *       with a free 3 x 3 column above it: with chance {@code kans} an entrance hut from {@code ingang.pool} goes on the
 *       surface straight above it (its own jigsaw {@link #INGANG_BOVEN} in the middle of its 3 x 1 floor opening, in the
 *       hut's floor layer, its top side pointing where the lift lets you off), and a shaft ({@link GrotSchachtPiece})
 *       connects the two. Only where the ground there is dry and flat enough ({@link #HUT_VLAK}); {@code verplicht}: no
 *       hut, no building; {@code pogingen}: the whole build tries up to that many spots ({@link #SCHUIVEN}) for a fit
 *       hut before it settles for the first one without;</li>
 *   <li>then it claims its room like every guhs structure ({@link BouwRuimte#claim}: keep_clear, voorrang).</li>
 * </ol>
 * The template pool elements use the processor list {@code guhs:onderwol_grot_schil} ({@link GrotSchilProcessor}). Python:
 * {@code tools/features/onderwol_bouw.py grot_bouw(...)} writes all of this.
 */
public class GrotBouwStructure extends Structure implements BouwRuimte.Ruimte {
    /** The jigsaw in a build's floor layer where the shaft of the entrance hut comes down. */
    public static final Identifier INGANG = Guhs.id("grot_ingang");
    /** The jigsaw in the middle of the floor opening of an entrance hut. */
    public static final Identifier INGANG_BOVEN = Guhs.id("grot_ingang_boven");
    /** Air needed above a cave floor to build on it. */
    public static final int HOOFDRUIMTE = 5;
    /** Nothing under this y (the knuffelnaad and the layers just above it). */
    public static final int BODEM = 8;
    /** The saus sea fills the caves up to y 10 in its regions (OnderWolWorldgen.SAUS_Y): no floor at or below this. */
    public static final int SAUS_TOT = 11;
    /** How far the jigsaw pieces may go up or down from the floor (a mine's stairs): the buildings stay in their layer. */
    public static final int VERTICAAL = 16;
    /** How much the ground under an entrance hut may differ (blocks). */
    public static final int HUT_VLAK = 3;
    /** 1.8.0: the entrance huts come this much less often than their kans (not for a {@link Ingang#verplicht} hut). */
    public static final float HUT_FACTOR = 0.5f;
    /** 1.8.0: no entrance hut where its ground lies in one of these biomes (the lakes, rivers and seas: tools/features/ingang_hut.py). */
    public static final TagKey<Biome> GEEN_HUT = TagKey.create(Registries.BIOME, Guhs.id("onderwol/geen_ingang_hut"));

    public record Zaal(int rand, int boven, float ruw, boolean alle) {
        public static final Codec<Zaal> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 32).optionalFieldOf("rand", 4).forGetter(Zaal::rand),
                Codec.intRange(0, 32).optionalFieldOf("boven", 3).forGetter(Zaal::boven),
                Codec.floatRange(0.0f, 4.0f).optionalFieldOf("ruw", 1.0f).forGetter(Zaal::ruw),
                Codec.BOOL.optionalFieldOf("alle", false).forGetter(Zaal::alle)
        ).apply(i, Zaal::new));
    }

    /**
     * The entrance hut: {@code kans} the chance of one, {@code verplicht} no hut no building, {@code pogingen} how many
     * spots the whole build may try (the middle of the chunk first, then {@link #SCHUIF} blocks to the sides and corners)
     * until the ground above its shaft marker is fit for the hut; when none is, the build stands at the first spot without it.
     */
    public record Ingang(Holder<StructureTemplatePool> pool, float kans, boolean verplicht, int pogingen) {
        public static final Codec<Ingang> CODEC = RecordCodecBuilder.create(i -> i.group(
                StructureTemplatePool.CODEC.fieldOf("pool").forGetter(Ingang::pool),
                Codec.floatRange(0.0f, 1.0f).optionalFieldOf("kans", 1.0f).forGetter(Ingang::kans),
                Codec.BOOL.optionalFieldOf("verplicht", false).forGetter(Ingang::verplicht),
                Codec.intRange(1, 9).optionalFieldOf("pogingen", 1).forGetter(Ingang::pogingen)
        ).apply(i, Ingang::new));
    }

    /** How far (blocks) a build moves for another try at its entrance hut ({@link Ingang#pogingen}). */
    public static final int SCHUIF = 8;
    /** The spots a build tries for its entrance hut, in order: the middle, the four sides, the four corners. */
    static final int[][] SCHUIVEN = {{0, 0}, {SCHUIF, 0}, {-SCHUIF, 0}, {0, SCHUIF}, {0, -SCHUIF}, {SCHUIF, SCHUIF}, {-SCHUIF, -SCHUIF},
            {SCHUIF, -SCHUIF}, {-SCHUIF, SCHUIF}};

    public static final MapCodec<GrotBouwStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Identifier.CODEC.fieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
            Codec.intRange(1, 20).optionalFieldOf("size", 1).forGetter(s -> s.size),
            Codec.intRange(1, 128).optionalFieldOf("max_distance", 64).forGetter(s -> s.maxDistance),
            Codec.INT.fieldOf("min_y").forGetter(s -> s.minY),
            Codec.INT.fieldOf("max_y").forGetter(s -> s.maxY),
            Codec.intRange(0, 64).optionalFieldOf("dekking", 6).forGetter(s -> s.dekking),
            Codec.STRING.optionalFieldOf("ruimte", "beide").forGetter(s -> s.ruimte),
            Zaal.CODEC.optionalFieldOf("zaal").forGetter(s -> s.zaal),
            Ingang.CODEC.optionalFieldOf("ingang").forGetter(s -> s.ingang),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, GrotBouwStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final Identifier startJigsawName;
    private final int size, maxDistance, minY, maxY, dekking;
    private final String ruimte;
    private final Optional<Zaal> zaal;
    private final Optional<Ingang> ingang;
    private final int keepClear;
    private final Optional<Integer> voorrang;

    public GrotBouwStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, Identifier startJigsawName, int size,
                             int maxDistance, int minY, int maxY, int dekking, String ruimte, Optional<Zaal> zaal, Optional<Ingang> ingang,
                             int keepClear, Optional<Integer> voorrang) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.size = size;
        this.maxDistance = maxDistance;
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
        this.dekking = dekking;
        this.ruimte = ruimte;
        this.zaal = zaal;
        this.ingang = ingang;
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

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public int dekking() {
        return dekking;
    }

    public Optional<Ingang> ingang() {
        return ingang;
    }

    public Optional<Zaal> zaal() {
        return zaal;
    }

    // --- the floor -------------------------------------------------------------------------------------------------------

    /** Is y a cave floor in this column (air at y with HOOFDRUIMTE of air above, solid dry ground below)? */
    public static boolean grotVloer(NoiseColumn column, int y) {
        BlockState onder = column.getBlock(y - 1);
        if (onder.isAir() || !onder.getFluidState().isEmpty()) {
            return false;
        }
        for (int k = 0; k < HOOFDRUIMTE; k++) {
            if (!column.getBlock(y + k).isAir()) {
                return false;
            }
        }
        return true;
    }

    /** The highest cave floor in the band in this column, or MIN_VALUE. */
    public static int zoekGrotVloer(NoiseColumn column, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            if (grotVloer(column, y)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** The lowest floor of the band: the structure's own, never at or below the saus sea or the bottom. */
    public int bandOnder() {
        return Math.max(minY, Math.max(BODEM + 2, SAUS_TOT + 1));
    }

    // --- the start -------------------------------------------------------------------------------------------------------

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        int laag = bandOnder(), hoog = maxY;
        if (hoog < laag) {
            return Optional.empty();
        }
        // (the cheapest look first: is this the right biome at all, and is there rock enough above the band?)
        if (!context.validBiome().test(context.chunkGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock((laag + hoog) / 2),
                QuartPos.fromBlock(z), context.randomState().sampler()))) {
            return Optional.empty();
        }
        int oppervlak = ZoekHoogte.hoogte(context.chunkGenerator(), context.randomState(), context.heightAccessor(), x, z);
        if (oppervlak - dekking - HOOFDRUIMTE < laag) {
            return Optional.empty();
        }
        WorldgenRandom random = context.random();
        int y = Integer.MIN_VALUE;
        if (!ruimte.equals("zaal")) {
            NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
            y = zoekGrotVloer(column, laag, Math.min(hoog, oppervlak - dekking - HOOFDRUIMTE));
        }
        if (y == Integer.MIN_VALUE) {
            if (ruimte.equals("grot")) {
                return Optional.empty();
            }
            y = laag + random.nextInt(hoog - laag + 1);
        }
        final int vloerY = y;
        // (1.8.0: half as often as the kans in the json, except where the hut is the building: same random draw, so the
        // buildings themselves keep their spots)
        boolean hutGewenst = ingang.isPresent() && random.nextFloat() < ingang.get().kans() * (ingang.get().verplicht() ? 1.0f : HUT_FACTOR);
        int pogingen = hutGewenst ? ingang.get().pogingen() : 1;
        Optional<GenerationStub> zonderHut = Optional.empty();
        for (int k = 0; k < pogingen; k++) {
            int px = x + SCHUIVEN[k][0], pz = z + SCHUIVEN[k][1];
            List<StructurePiece> bouw = stukken(context, px, vloerY, pz);
            if (bouw == null) {
                continue;
            }
            StructurePiecesBuilder builder = new StructurePiecesBuilder();
            // 1. the hall (before the build: the template's blocks win)
            zaal(context, bouw, builder, vloerY, chunk);
            // 2. the build itself
            bouw.forEach(builder::addPiece);
            // 3. the entrance hut and its shaft
            if (hutGewenst && ingang(context, bouw, builder)) {
                return BouwRuimte.claim(context, this, Optional.of(new GenerationStub(new BlockPos(px, vloerY, pz), Either.right(builder))));
            }
            if (zonderHut.isEmpty()) {
                // (a failed hut adds nothing: this is the build on its own, for when no spot gets its hut)
                zonderHut = Optional.of(new GenerationStub(new BlockPos(px, vloerY, pz), Either.right(builder)));
            }
        }
        if (ingang.isPresent() && ingang.get().verplicht()) {
            return Optional.empty();   // (no hut, no building)
        }
        return BouwRuimte.claim(context, this, zonderHut);
    }

    /** The jigsaw pieces of the build with its start in the floor layer under (x, vloerY, z), or null when they don't fit. */
    private List<StructurePiece> stukken(GenerationContext context, int x, int vloerY, int z) {
        Optional<GenerationStub> stub = JigsawPlacement.addPieces(context, startPool, Optional.of(startJigsawName), size, new BlockPos(x, vloerY, z), false,
                Optional.empty(), new JigsawStructure.MaxDistance(maxDistance, VERTICAAL), PoolAliasLookup.EMPTY, DimensionPadding.ZERO,
                LiquidSettings.IGNORE_WATERLOGGING);
        if (stub.isEmpty()) {
            return null;
        }
        StructurePiecesBuilder jigsaw = stub.get().getPiecesBuilder();
        if (jigsaw.isEmpty() || !genoegDekking(context, jigsaw.getBoundingBox())) {
            return null;
        }
        return new ArrayList<>(jigsaw.build().pieces());
    }

    /** The natural hall around the start piece (or every piece), when the structure has one. */
    private void zaal(GenerationContext context, List<StructurePiece> bouw, StructurePiecesBuilder builder, int vloerY, ChunkPos chunk) {
        if (zaal.isEmpty()) {
            return;
        }
        Zaal z0 = zaal.get();
        BoundingBox start = bouw.get(0).getBoundingBox();
        List<StructurePiece> om = z0.alle() ? bouw : bouw.subList(0, 1);
        for (int k = 0; k < om.size(); k++) {
            BoundingBox b = om.get(k).getBoundingBox();
            int vloer = b.minY() + (vloerY - start.minY());
            builder.addPiece(new GrotZaalPiece((b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2, vloer,
                    b.getXSpan() / 2 + z0.rand(), b.getZSpan() / 2 + z0.rand(), b.maxY() - vloer + 1 + z0.boven(), z0.ruw(), dekking,
                    context.seed() ^ chunk.pack() * 31L ^ k));
        }
    }

    /** Is every part of this box at least dekking blocks under the surface (at its corners and middle), and above the bottom? */
    private boolean genoegDekking(GenerationContext context, BoundingBox b) {
        if (b.minY() <= BODEM) {
            return false;
        }
        int[][] punten = {{b.minX(), b.minZ()}, {b.maxX(), b.minZ()}, {b.minX(), b.maxZ()}, {b.maxX(), b.maxZ()},
                {(b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2}};
        for (int[] p : punten) {
            if (ZoekHoogte.hoogte(context.chunkGenerator(), context.randomState(), context.heightAccessor(), p[0], p[1]) - dekking <= b.maxY()) {
                return false;
            }
        }
        return true;
    }

    /** The jigsaw named {@code naam} in one of these pieces, as placed (its world position and rotated state), or null. */
    public static StructureTemplate.StructureBlockInfo vind(GenerationContext context, List<? extends StructurePiece> pieces, Identifier naam) {
        for (StructurePiece piece : pieces) {
            if (piece instanceof PoolElementStructurePiece pe) {
                for (StructureTemplate.JigsawBlockInfo j : pe.getElement().getShuffledJigsawBlocks(context.structureTemplateManager(), pe.getPosition(),
                        pe.getRotation(), RandomSource.create(0L))) {
                    if (j.name().equals(naam)) {
                        return j.info();
                    }
                }
            }
        }
        return null;
    }

    /**
     * The entrance hut straight above the build's {@link #INGANG} jigsaw, and the shaft between them. False when the build
     * has no such jigsaw or the ground there is not fit for a hut (water, too steep, too little rock in between).
     */
    private boolean ingang(GenerationContext context, List<StructurePiece> bouw, StructurePiecesBuilder builder) {
        StructureTemplate.StructureBlockInfo onder = vind(context, bouw, INGANG);
        if (onder == null) {
            return false;
        }
        int x = onder.pos().getX(), z = onder.pos().getZ();
        int h = ZoekHoogte.hoogte(context.chunkGenerator(), context.randomState(), context.heightAccessor(), x, z);
        if (h <= context.chunkGenerator().getSeaLevel() + 1 || nat(context, x, h, z)) {
            return false;   // (in or at the water)
        }
        for (int[] d : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            int hd = ZoekHoogte.hoogte(context.chunkGenerator(), context.randomState(), context.heightAccessor(), x + d[0], z + d[1]);
            if (Math.abs(hd - h) > HUT_VLAK || nat(context, x + d[0], hd, z + d[1])) {
                return false;
            }
        }
        // (the hut's jigsaw lands in the top block of the ground: its floor layer)
        Optional<GenerationStub> hut = JigsawPlacement.addPieces(context, ingang.get().pool(), Optional.of(INGANG_BOVEN), 1, new BlockPos(x, h, z), false,
                Optional.empty(), new JigsawStructure.MaxDistance(32), PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);
        if (hut.isEmpty() || hut.get().getPiecesBuilder().isEmpty()) {
            return false;
        }
        List<StructurePiece> hutStukken = new ArrayList<>(hut.get().getPiecesBuilder().build().pieces());
        StructureTemplate.StructureBlockInfo boven = vind(context, hutStukken, INGANG_BOVEN);
        if (boven == null || boven.pos().getY() - onder.pos().getY() < 8) {
            return false;
        }
        Direction richting = JigsawBlock.getTopFacing(boven.state());
        if (!richting.getAxis().isHorizontal()) {
            richting = Direction.SOUTH;
        }
        int hutOnder = Integer.MAX_VALUE;
        for (StructurePiece p : hutStukken) {
            hutOnder = Math.min(hutOnder, p.getBoundingBox().minY());
            builder.addPiece(p);
        }
        builder.addPiece(new GrotSchachtPiece(x, z, onder.pos().getY(), boven.pos().getY(), hutOnder - 1, richting));
        return true;
    }

    /**
     * 1.8.0: is the ground at (x, y, z) wet? The Guhmension's sea level is 0 and its lakes, rivers and ditches are filled in
     * at worldgen step "lakes", after the structures chose their spots: the biome there tells it ({@link #GEEN_HUT}).
     */
    static boolean nat(GenerationContext context, int x, int y, int z) {
        Holder<Biome> b = context.chunkGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y),
                QuartPos.fromBlock(z), context.randomState().sampler());
        return b.is(GEEN_HUT);
    }

    @Override
    public StructureType<?> type() {
        return OnderWolBouw.GROT_BOUW.get();
    }
}
