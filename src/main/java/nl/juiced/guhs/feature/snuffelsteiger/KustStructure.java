package nl.juiced.guhs.feature.snuffelsteiger;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * Structure type {@code guhs:kust_steiger}: one template on the SHORE of a Diepe Guhzee, turned so that its {@code +z}
 * points out to sea (the steigerhuisje: its plot on the land, its pier over the water).
 * <p>
 * The Guhmensie has no sea level: a Diepe Guhzee is where the noise {@code zee_noise} is high (tools/features/diepzee.py:
 * the land is lowered into a basin there and filled with water up to y 62). The shore is a contour of that noise, so a
 * start is found without looking at a single block:
 * <ol>
 *   <li>the noise in the middle of the start chunk must lie in {@code band} (near a shore), else nothing starts;</li>
 *   <li>from there a walk straight up or down the noise's slope (at most {@link #LOOP} blocks) to the contour
 *       {@code oever}; the nearest compass direction of the slope there is "out to sea";</li>
 *   <li>along that direction the real ground is asked (a few columns) for the water's edge: the first column whose ground
 *       lies under the sea's surface. The template's {@code anker} (the first plank of the pier) lands on that column at
 *       the fixed height {@code y};</li>
 *   <li>then the spot is checked with the real ground: every {@code land} point {@code [x, z, lo, hi]} (template
 *       coordinates) has its surface {@code lo..hi} blocks from the deck (no cliff behind the cottage, no hole under
 *       it), every {@code water} point {@code [x, z, diepte]} is sea at least that deep (the pier stands in water, the
 *       boat floats), and {@code open_zee} blocks further out it is still sea (the boat can sail away).</li>
 * </ol>
 * No terrain adaptation: the template brings its own foundation and its own air. It takes part in {@link BouwRuimte}
 * like every guhs building ({@code keep_clear}, {@code voorrang}) and keeps {@code eigen_afstand} blocks between two of
 * its own kind; tools/features/snuffel_steiger.py writes the JSON.
 */
public class KustStructure extends Structure implements BouwRuimte.Ruimte {
    /** How far the walk from the middle of the start chunk to the shore's contour may be (blocks). */
    public static final int LOOP = 40;
    /** How far along "out to sea" the water's edge is looked for, on both sides of the contour (blocks). */
    public static final int RAND = 16;

    /** The numbers of the sea: its noise, the band a start chunk's middle must lie in, the shore's contour, where the water is. */
    public record Zee(ResourceKey<NormalNoise.NoiseParameters> noise, double bandVan, double bandTot, double oever, double water, int waterY) {
        public static final MapCodec<Zee> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceKey.codec(Registries.NOISE).fieldOf("zee_noise").forGetter(Zee::noise),
                Codec.DOUBLE.fieldOf("band_van").forGetter(Zee::bandVan),
                Codec.DOUBLE.fieldOf("band_tot").forGetter(Zee::bandTot),
                Codec.DOUBLE.fieldOf("oever").forGetter(Zee::oever),
                Codec.DOUBLE.fieldOf("water_van").forGetter(Zee::water),
                Codec.INT.fieldOf("water_y").forGetter(Zee::waterY)
        ).apply(i, Zee::new));
    }

    public static final MapCodec<KustStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Zee.CODEC.forGetter(s -> s.zee),
            BlockPos.CODEC.fieldOf("anker").forGetter(s -> s.anker),
            Codec.INT.fieldOf("y").forGetter(s -> s.y),
            Codec.INT.listOf().listOf().fieldOf("land").forGetter(s -> s.land),
            Codec.INT.listOf().listOf().fieldOf("water").forGetter(s -> s.water),
            Codec.INT.listOf().optionalFieldOf("open_zee", List.of()).forGetter(s -> s.openZee),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang),
            Codec.INT.optionalFieldOf("eigen_afstand", BouwRuimte.MARGIN).forGetter(s -> s.eigenAfstand)
    ).apply(i, KustStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final Zee zee;
    private final BlockPos anker;
    private final int y;
    private final List<List<Integer>> land, water;
    private final List<Integer> openZee;
    private final int keepClear;
    private final Optional<Integer> voorrang;
    private final int eigenAfstand;

    public KustStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, Zee zee, BlockPos anker, int y, List<List<Integer>> land,
                         List<List<Integer>> water, List<Integer> openZee, int keepClear, Optional<Integer> voorrang, int eigenAfstand) {
        super(settings);
        this.startPool = startPool;
        this.zee = zee;
        this.anker = anker;
        this.y = y;
        this.land = land;
        this.water = water;
        this.openZee = openZee;
        this.keepClear = keepClear;
        this.voorrang = voorrang;
        this.eigenAfstand = eigenAfstand;
    }

    @Override
    public int keepClear() {
        return keepClear;
    }

    /** The room between two shore buildings of this kind ({@code eigen_afstand}: no row of docks on one shore). */
    @Override
    public int eigenAfstand() {
        return eigenAfstand;
    }

    @Override
    public int voorrang() {
        return voorrang.orElse(keepClear);
    }

    public Zee zee() {
        return zee;
    }

    /** The template's block that lands on the water's edge (the first plank of the pier). */
    public BlockPos anker() {
        return anker;
    }

    /** The world height of the anchor's layer (the deck). */
    public int y() {
        return y;
    }

    /** A spot for a start: the world position of the template's anchor and how the template is turned. */
    public record Plek(BlockPos anker, Rotation draai, Direction zee) {
    }

    /** Why a chunk has no start (for the dev command and the game test's count). */
    public enum Reden {
        GOED, GEEN_KUST, GEEN_HELLING, GEEN_OEVER, GEEN_WATERRAND, LAND_TE_HOOG, LAND_TE_LAAG, WATER_ONDIEP, GEEN_OPEN_ZEE
    }

    /** What {@link #zoek} found: the spot, or why not. */
    public record Uitkomst(@Nullable Plek plek, Reden reden) {
    }

    /** The template turned so that its +z looks this way. */
    public static Rotation draaiNaar(Direction zee) {
        return switch (zee) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Template coordinates (relative to the anchor's column) to the world, for a template turned to this sea side. */
    private static BlockPos wereld(Plek p, BlockPos anker, int tx, int tz) {
        BlockPos rel = StructureTemplate.transform(new BlockPos(tx - anker.getX(), 0, tz - anker.getZ()), Mirror.NONE, p.draai(), BlockPos.ZERO);
        return p.anker().offset(rel);
    }

    /**
     * Looks for the spot of a start in this chunk (see the class text). {@code hoogte}: the first free y over the ground
     * of a column (the chunk generator's base height), asked for a few dozen columns at most, and only near a shore.
     */
    public Uitkomst zoek(NormalNoise noise, ChunkPos chunk, Hoogte hoogte) {
        int mx = chunk.getMiddleBlockX(), mz = chunk.getMiddleBlockZ();
        double n0 = noise.getValue(mx, 0, mz);
        if (n0 < zee.bandVan() || n0 > zee.bandTot()) {
            return new Uitkomst(null, Reden.GEEN_KUST);
        }
        // up or down the slope to the shore's contour
        double x = mx, z = mz, n = n0;
        boolean op = n0 < zee.oever();
        boolean gevonden = false;
        for (int stap = 0; stap <= LOOP; stap++) {
            double gx = noise.getValue(x + 2, 0, z) - noise.getValue(x - 2, 0, z), gz = noise.getValue(x, 0, z + 2) - noise.getValue(x, 0, z - 2);
            double g = Math.hypot(gx, gz);
            if (g < 1e-7) {
                return new Uitkomst(null, Reden.GEEN_HELLING);
            }
            if ((n >= zee.oever()) == op) {
                gevonden = true;
                break;
            }
            double s = op ? 1 : -1;
            x += s * gx / g;
            z += s * gz / g;
            n = noise.getValue(x, 0, z);
        }
        if (!gevonden) {
            return new Uitkomst(null, Reden.GEEN_OEVER);
        }
        int px = (int) Math.floor(x), pz = (int) Math.floor(z);
        double gx = noise.getValue(px + 3, 0, pz) - noise.getValue(px - 3, 0, pz), gz = noise.getValue(px, 0, pz + 3) - noise.getValue(px, 0, pz - 3);
        Direction zeeKant = Math.abs(gx) >= Math.abs(gz) ? (gx > 0 ? Direction.EAST : Direction.WEST) : (gz > 0 ? Direction.SOUTH : Direction.NORTH);
        // the water's edge along that direction: the first column of sea (its ground under the surface), land right behind it
        int rand = Integer.MIN_VALUE;
        int vorige = hoogte.van(px - zeeKant.getStepX() * (RAND + 1), pz - zeeKant.getStepZ() * (RAND + 1));
        for (int d = -RAND; d <= RAND; d++) {
            int wx = px + zeeKant.getStepX() * d, wz = pz + zeeKant.getStepZ() * d;
            int h = hoogte.van(wx, wz);
            if (h <= zee.waterY() && vorige > zee.waterY() && noise.getValue(wx, 0, wz) >= zee.water()) {
                rand = d;
                break;
            }
            vorige = h;
        }
        if (rand == Integer.MIN_VALUE) {
            return new Uitkomst(null, Reden.GEEN_WATERRAND);
        }
        Plek plek = new Plek(new BlockPos(px + zeeKant.getStepX() * rand, y, pz + zeeKant.getStepZ() * rand), draaiNaar(zeeKant), zeeKant);
        for (List<Integer> l : land) {
            BlockPos w = wereld(plek, anker, l.get(0), l.get(1));
            int d = hoogte.van(w.getX(), w.getZ()) - 1 - y;      // the ground's top block against the deck's layer
            if (d > l.get(3)) {
                return new Uitkomst(null, Reden.LAND_TE_HOOG);
            }
            if (d < l.get(2)) {
                return new Uitkomst(null, Reden.LAND_TE_LAAG);
            }
        }
        for (List<Integer> l : water) {
            BlockPos w = wereld(plek, anker, l.get(0), l.get(1));
            if (noise.getValue(w.getX(), 0, w.getZ()) < zee.water() || zee.waterY() + 1 - hoogte.van(w.getX(), w.getZ()) < l.get(2)) {
                return new Uitkomst(null, Reden.WATER_ONDIEP);
            }
        }
        for (int ver : openZee) {
            BlockPos w = wereld(plek, anker, anker.getX(), anker.getZ() + ver);
            if (noise.getValue(w.getX(), 0, w.getZ()) < zee.water()) {
                return new Uitkomst(null, Reden.GEEN_OPEN_ZEE);
            }
        }
        return new Uitkomst(plek, Reden.GOED);
    }

    /** The first free y over the ground of a column. */
    @FunctionalInterface
    public interface Hoogte {
        int van(int x, int z);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        NormalNoise noise = context.randomState().getOrCreateNoise(zee.noise());
        Uitkomst uit = zoek(noise, context.chunkPos(), (x, z) -> context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState()));
        Plek plek = uit.plek();
        if (plek == null) {
            return Optional.empty();
        }
        StructurePoolElement element = startPool.value().getRandomTemplate(context.random());
        Rotation draai = plek.draai();
        // the template's corner: the anchor block lands on the spot
        BlockPos hoek = plek.anker().subtract(StructureTemplate.transform(anker, Mirror.NONE, draai, BlockPos.ZERO));
        BoundingBox doos = element.getBoundingBox(context.structureTemplateManager(), hoek, draai);
        PoolElementStructurePiece stuk = new PoolElementStructurePiece(context.structureTemplateManager(), element, hoek, element.getGroundLevelDelta(), draai,
                doos, LiquidSettings.APPLY_WATERLOGGING);
        return BouwRuimte.claim(context, this, Optional.of(new GenerationStub(plek.anker(), builder -> builder.addPiece(stuk))));
    }

    @Override
    public StructureType<?> type() {
        return SnuffelsteigerFeature.KUST_STEIGER.get();
    }
}
