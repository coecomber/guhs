package nl.juiced.guhs.feature.bio.wereld;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * Structure type {@code guhs:bio_plek} (biomes3): a jigsaw building that starts at a named kind of spot of the three
 * biomes' terrain: the river bank, over the river, the foot of a waterfall, a lake island, free air above the meadow...
 * (the kinds: {@link BioPlekken}). Structures are positioned before any terrain exists, so the spot comes from the
 * terrain model, not from the world.
 * <p>
 * The file has the shape of a {@code guhs:flat_jigsaw} structure (what the generator's {@code h.structure} writes) with
 * another type and the field {@code plek}; tools/features/bio_wereld_plek.py turns one into the other. Fields besides the
 * usual structure settings:
 * <ul>
 *   <li>{@code jigsaw}: {@code start_pool}, {@code size} (at least 1), {@code max_distance_from_center}, optional
 *       {@code start_jigsaw_name}; anything else in it is ignored (the height and heightmap fields have no meaning here);</li>
 *   <li>{@code plek}: the kind of spot; {@code hoogte}: blocks above the meadow, kind {@code lucht} only (default 24);</li>
 *   <li>{@code ruimte}: kind {@code lucht} only: no natural island or cloud within this many blocks of the start chunk's
 *       middle, from the meadow to the sky (default 20);</li>
 *   <li>{@code vlak}: kind {@code terras} only: plain level terrace this far around the spot (default 6, even);</li>
 *   <li>{@code per_regio} (biomes3 fix-plaatsing): ONE per region instead of "wherever the set tries a chunk with a spot":
 *       a number 0..1, the chance that a region (a valley with its lake, a Wolkenweide) has this building; its place is
 *       chosen from the terrain model for the whole region ({@link RegioKeuze}). {@code twee_vanaf}: regions whose lake
 *       or meadow has at least this mean radius get two;</li>
 *   <li>{@code keep_clear}, {@code voorrang}: as for every guhs structure ({@link BouwRuimte});</li>
 *   <li>{@code alleen_test}: test data, only generates when the environment variable GUHS_BIO_PLEKTEST is set.</li>
 * </ul>
 * Where the start piece lands: its start jigsaw block (or, without {@code start_jigsaw_name}, the corner of its template)
 * comes IN the spot's top ground block (the y of the spot; for {@code lucht} the asked height). Which way: the template's
 * NORTH side (its low-z side, as built) is turned to the spot's "kijk" direction: to the river, the water, the fall, the
 * tree. It starts in the chunk the structure set picked, and gives up when that chunk has no such spot.
 */
public class BioPlekStructure extends Structure implements BouwRuimte.Ruimte {
    /** The fields of the inner jigsaw that matter here. */
    public record Jigsaw(Holder<StructureTemplatePool> startPool, Optional<Identifier> startJigsawName, int size, JigsawStructure.MaxDistance maxDistance) {
        public static final Codec<Jigsaw> CODEC = RecordCodecBuilder.create(i -> i.group(
                StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(Jigsaw::startPool),
                Identifier.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(Jigsaw::startJigsawName),
                Codec.intRange(1, 20).optionalFieldOf("size", 1).forGetter(Jigsaw::size),
                JigsawStructure.MaxDistance.CODEC.optionalFieldOf("max_distance_from_center", new JigsawStructure.MaxDistance(80)).forGetter(Jigsaw::maxDistance)
        ).apply(i, Jigsaw::new));
    }

    public static final MapCodec<BioPlekStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Jigsaw.CODEC.fieldOf("jigsaw").forGetter(s -> s.jigsaw),
            BioPlekken.Soort.CODEC.fieldOf("plek").forGetter(s -> s.soort),
            Codec.intRange(1, 160).optionalFieldOf("hoogte", 24).forGetter(s -> s.hoogte),
            Codec.intRange(0, 64).optionalFieldOf("ruimte", 20).forGetter(s -> s.ruimte),
            Codec.intRange(2, 16).optionalFieldOf("vlak", BioPlekken.VLAK).forGetter(s -> s.vlak), // biomes3 merge
            Codec.doubleRange(0, 1).optionalFieldOf("per_regio").forGetter(s -> s.perRegio), // biomes3 fix-plaatsing
            Codec.intRange(0, 100000).optionalFieldOf("twee_vanaf", 0).forGetter(s -> s.tweeVanaf), // biomes3 fix-plaatsing
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang),
            Codec.BOOL.optionalFieldOf("alleen_test", false).forGetter(s -> s.alleenTest)
    ).apply(i, BioPlekStructure::new));

    /** Test structures (alleen_test) only generate with this environment variable set (a scratch dev server). */
    public static final boolean TEST_AAN = System.getenv("GUHS_BIO_PLEKTEST") != null;

    private final Jigsaw jigsaw;
    private final BioPlekken.Soort soort;
    private final int hoogte;
    private final int ruimte;
    private final int vlak;
    private final Optional<Double> perRegio;
    private final int tweeVanaf;
    private final int keepClear;
    private final Optional<Integer> voorrang;
    private final boolean alleenTest;

    public BioPlekStructure(StructureSettings settings, Jigsaw jigsaw, BioPlekken.Soort soort, int hoogte, int ruimte, int vlak, Optional<Double> perRegio,
                            int tweeVanaf, int keepClear, Optional<Integer> voorrang, boolean alleenTest) {
        super(settings);
        this.vlak = vlak;
        this.perRegio = perRegio;
        this.tweeVanaf = tweeVanaf;
        this.jigsaw = jigsaw;
        this.soort = soort;
        this.hoogte = hoogte;
        this.ruimte = ruimte;
        this.keepClear = keepClear;
        this.voorrang = voorrang;
        this.alleenTest = alleenTest;
    }

    public BioPlekken.Soort soort() {
        return soort;
    }

    public int ruimte() {
        return ruimte;
    }

    public int hoogte() {
        return hoogte;
    }

    public int vlak() {
        return vlak;
    }

    /** biomes3 fix-plaatsing: one per region ({@link RegioKeuze}): the chance that a region has this building; empty: wherever the set tries a chunk with a spot. */
    public Optional<Double> perRegio() {
        return perRegio;
    }

    /** biomes3 fix-plaatsing: a region whose lake or meadow has at least this mean radius gets a second one (0: never). */
    public int tweeVanaf() {
        return tweeVanaf;
    }

    public Holder<StructureTemplatePool> startPool() {
        return jigsaw.startPool();
    }

    public Optional<Identifier> startJigsaw() {
        return jigsaw.startJigsawName();
    }

    /** A number of this structure's own, the same in every run (from the name of its start pool): what its choices per region are drawn with. */
    public long zout() {
        return jigsaw.startPool().unwrapKey().map(k -> (long) k.identifier().toString().hashCode()).orElse(0L) & 0xFFFFFFFFL;
    }

    /** Does this structure generate at all (test data only on a test server)? */
    public boolean doetMee() {
        return !alleenTest || TEST_AAN;
    }

    /** The rotation that turns a template's north side to this direction. */
    public static Rotation draai(Direction kijk) {
        return switch (kijk) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!doetMee()) {
            return Optional.empty();
        }
        // biomes3 fix-plaatsing: counted (BioModel.teller): what the structure starts cost
        boolean buitenste = BioModel.plekIn();
        long t0 = buitenste ? System.nanoTime() : 0;
        try {
            return vind(context);
        } finally {
            BioModel.plekUit(buitenste, t0);
        }
    }

    private Optional<GenerationStub> vind(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        BioModel model = BioModel.van(context.randomState());
        BioPlekken.Plek p;
        if (perRegio.isPresent()) {
            // biomes3 fix-plaatsing: one per region. The spot was chosen from the terrain model for the whole region; this
            // chunk starts the structure when the spot lies in its cell of the structure set's grid (so the building can
            // stand a few chunks from its start chunk)
            RandomSpreadStructurePlacement plaatsing = Luchtruim.plaatsing(this);
            if (plaatsing == null) {
                return Optional.empty();
            }
            Optional<BioPlekken.Plek> gekozen = RegioKeuze.inCel(model, this, chunk.x(), chunk.z(), plaatsing.spacing());
            if (gekozen.isEmpty()) {
                return Optional.empty();
            }
            p = gekozen.get();
        } else {
            Optional<BioPlekken.Plek> plek = BioPlekken.zoek(model, soort, chunk.x(), chunk.z(), hoogte, vlak);
            if (plek.isEmpty()) {
                return Optional.empty();
            }
            p = plek.get();
            // biomes3 bouw-wolk2: a building in the air hangs over the meadow itself, not over its rim, where the land of the
            // neighbours rises into the building: the meadow must be whole all round the start, as far as the building reaches
            // (biomes3 fix-plaatsing: asked of the region values, exactly as Luchtruim does for the air it keeps free)
            if (soort == BioPlekken.Soort.LUCHT && !RegioKeuze.heleWeide(model, p.x(), p.z(), ruimte)) {
                return Optional.empty();
            }
        }
        // vanilla's jigsaw placement draws the start piece's rotation first thing from the context's random: hand it a
        // random whose first draw is the rotation we want (found by trying seeds; the same every time)
        Rotation wil = draai(p.kijk());
        WorldgenRandom random = null;
        for (int poging = 0; poging < 64 && random == null; poging++) {
            long seed = context.seed() + 7919L * poging;
            WorldgenRandom proef = new WorldgenRandom(new LegacyRandomSource(0L));
            proef.setLargeFeatureSeed(seed, chunk.x(), chunk.z());
            if (Rotation.getRandom(proef) == wil) {
                random = new WorldgenRandom(new LegacyRandomSource(0L));
                random.setLargeFeatureSeed(seed, chunk.x(), chunk.z());
            }
        }
        if (random == null) {
            return Optional.empty();
        }
        GenerationContext gedraaid = new GenerationContext(context.registryAccess(), context.chunkGenerator(), context.biomeSource(),
                context.randomState(), context.structureTemplateManager(), random, context.seed(), chunk, context.heightAccessor(), context.validBiome());
        // (an unprojected start piece is moved down by its ground level delta, 1: ask one higher, so the start jigsaw
        // lands in the spot's top ground block)
        return BouwRuimte.claim(context, this, JigsawPlacement.addPieces(gedraaid, jigsaw.startPool(), jigsaw.startJigsawName(), jigsaw.size(),
                new BlockPos(p.x(), p.y() + 1, p.z()), false, Optional.empty(), jigsaw.maxDistance(), PoolAliasLookup.EMPTY, DimensionPadding.ZERO,
                LiquidSettings.IGNORE_WATERLOGGING));
    }

    @Override
    public int keepClear() {
        // biomes3 fix-plaatsing: a one-per-region building can stand anywhere in its start chunk's grid cell
        RandomSpreadStructurePlacement plaatsing = perRegio.isPresent() ? Luchtruim.plaatsing(this) : null;
        return keepClear + (plaatsing == null ? 0 : 16 * (plaatsing.spacing() - 1));
    }

    @Override
    public int voorrang() {
        return voorrang.orElse(keepClear);
    }

    @Override
    public StructureType<?> type() {
        return WereldSlice.BIO_PLEK.get();
    }
}
