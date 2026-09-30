package nl.juiced.guhs.feature.elftocht;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * The Elf-Guhjestocht's own structure type ({@code guhs:elfguhjestocht}): exactly one tour on the peak of every
 * Guhpolder (like the Knuffeldal town). Its structure set uses {@link ElftochtPlacement} (cells of {@code cell} chunks,
 * the potential start chunk is the chunk of the cell's peak); here the peak is checked ({@link ElftochtPiek#isPolderPiek}:
 * high enough, the highest of its polder, the polder all around it) and the one 256 x 256 template is placed with its
 * middle jigsaw ({@code start_jigsaw_name}, at ground level) on the top block of the ground at the tour's spot: with a
 * {@code vlak} the best spot near the peak where the square lies on dead-flat polder ({@link ElftochtPiek#spot}; a polder's
 * noise peak can lie right at a coast, where the sea mask cuts the polder off), and only when the real ground under the
 * square is level enough ({@code max_ongelijk} grid points may be under water or too low/high, see {@link #ongelijk}).
 * <p>
 * {@code cell_chunks} is only for tools/make_v2.py bouwruimte(): the anchor always lies inside the start chunk (1).
 */
public class ElftochtStructure extends Structure implements BouwRuimte.Ruimte {
    public static final MapCodec<ElftochtStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Identifier.CODEC.fieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
            NormalNoise.NoiseParameters.CODEC.fieldOf("polder_noise").forGetter(s -> s.noise),
            Codec.intRange(2, 64).fieldOf("cell").forGetter(s -> s.cell),
            Codec.intRange(1, 4).optionalFieldOf("cell_chunks", 1).forGetter(s -> s.cellChunks),
            Codec.DOUBLE.fieldOf("min_value").forGetter(s -> s.minValue),
            Codec.DOUBLE.fieldOf("dal_value").forGetter(s -> s.dalValue),
            Codec.DOUBLE.fieldOf("flat_value").forGetter(s -> s.flatValue),
            Codec.intRange(0, 200).fieldOf("flat_radius").forGetter(s -> s.flatRadius),
            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistance),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang),
            ElftochtPiek.Vlak.CODEC.optionalFieldOf("vlak", ElftochtPiek.Vlak.GEEN).forGetter(s -> s.vlak),
            Codec.intRange(0, 25).optionalFieldOf("max_ongelijk", 25).forGetter(s -> s.maxOngelijk)
    ).apply(i, ElftochtStructure::new));

    /** The terrain guard: grid points of the square whose ground may lie more than this much lower or higher. */
    public static final int ZAKKEN = 10, STIJGEN = 12;

    private final Holder<StructureTemplatePool> startPool;
    private final Identifier startJigsawName;
    private final Holder<NormalNoise.NoiseParameters> noise;
    private final int cell;
    private final int cellChunks;
    private final double minValue;
    private final double dalValue;
    private final double flatValue;
    private final int flatRadius;
    private final int maxDistance;
    private final int keepClear;
    private final Optional<Integer> voorrang;
    private final ElftochtPiek.Vlak vlak;
    private final int maxOngelijk;

    public ElftochtStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, Identifier startJigsawName,
                             Holder<NormalNoise.NoiseParameters> noise, int cell, int cellChunks, double minValue, double dalValue,
                             double flatValue, int flatRadius, int maxDistance, int keepClear, Optional<Integer> voorrang,
                             ElftochtPiek.Vlak vlak, int maxOngelijk) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.noise = noise;
        this.cell = cell;
        this.cellChunks = cellChunks;
        this.minValue = minValue;
        this.dalValue = dalValue;
        this.flatValue = flatValue;
        this.flatRadius = flatRadius;
        this.maxDistance = maxDistance;
        this.keepClear = keepClear;
        this.voorrang = voorrang;
        this.vlak = vlak;
        this.maxOngelijk = maxOngelijk;
    }

    /**
     * The tour of the cell holding this chunk, if that cell has one: its spot (else null). The cell's peak must be the
     * polder's peak (high enough, the highest of its polder), the noise flat on the ring (flat_radius, if set), and the
     * best spot near it must lie with at least {@code vlak.min_flat} of its grid points on dead-flat polder.
     */
    public ElftochtPiek.Spot plek(long seed, int chunkX, int chunkZ) {
        int cx = Math.floorDiv(chunkX, cell), cz = Math.floorDiv(chunkZ, cell);
        if (!ElftochtPiek.isPolderPiek(seed, noise, cell, cx, cz, minValue, dalValue)) {
            return null;
        }
        ElftochtPiek.Peak peak = ElftochtPiek.peak(seed, noise, cell, cx, cz);
        if (!ElftochtPiek.vlakRond(seed, noise, peak, flatRadius, flatValue)) {
            return null;
        }
        ElftochtPiek.Spot spot = ElftochtPiek.spot(seed, noise, vlak, cell, cx, cz);
        return spot.vlak() >= vlak.minVlak() ? spot : null;
    }

    /** The real ground under the square (the grid of {@link ElftochtPiek}): how many points lie under water or more than
     *  {@link #ZAKKEN} lower / {@link #STIJGEN} higher than the middle (the beard can't smooth those). */
    public static int ongelijk(GenerationContext context, int x, int z, int midden) {
        int bad = 0;
        int half = ElftochtPiek.GRID / 2;
        for (int a = -half; a <= half; a++) {
            for (int b = -half; b <= half; b++) {
                int px = x + a * ElftochtPiek.SAMPLE, pz = z + b * ElftochtPiek.SAMPLE;
                int bodem = context.chunkGenerator().getBaseHeight(px, pz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                int top = context.chunkGenerator().getBaseHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                if (top > bodem + 1 || bodem < midden - ZAKKEN || bodem > midden + STIJGEN) {
                    bad++;
                }
            }
        }
        return bad;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        ElftochtPiek.Spot spot = plek(context.seed(), chunk.x(), chunk.z());
        if (spot == null || (spot.x() >> 4) != chunk.x() || (spot.z() >> 4) != chunk.z()) {
            return Optional.empty();
        }
        int surface = context.chunkGenerator().getBaseHeight(spot.x(), spot.z(), Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
                context.randomState());
        if (maxOngelijk < ElftochtPiek.GRID * ElftochtPiek.GRID && ongelijk(context, spot.x(), spot.z(), surface) > maxOngelijk) {
            return Optional.empty();     // (the ground under the square isn't polder enough after all: no tour in this polder)
        }
        // the template's ground (GROND_Y, where the anchor is) on the top block of the ground; 2.10: the start element knows
        // its real ground (guhs:grond_single_pool_element, no moat), see KnuffeldalStadjeStructure
        return BouwRuimte.claim(context, this, JigsawPlacement.addPieces(context, startPool, Optional.of(startJigsawName), 1,
                new BlockPos(spot.x(), nl.juiced.guhs.world.grond.Grond.startY(startPool, surface), spot.z()), false, Optional.empty(), maxDistance, PoolAliasLookup.EMPTY, DimensionPadding.ZERO,
                LiquidSettings.IGNORE_WATERLOGGING));
    }

    public ElftochtPiek.Vlak vlak() {
        return vlak;
    }

    @Override
    public int keepClear() {
        return keepClear;
    }

    @Override
    public int voorrang() {
        return voorrang.orElse(keepClear);
    }

    public Holder<NormalNoise.NoiseParameters> noise() {
        return noise;
    }

    public int cell() {
        return cell;
    }

    @Override
    public StructureType<?> type() {
        return ElftochtFeature.STRUCTURE_TYPE.get();
    }
}
