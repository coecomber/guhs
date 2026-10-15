package nl.juiced.guhs.feature.ringh3;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import nl.juiced.guhs.feature.barbecuether.BarbecuePutStructure;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * bbq2 (ring-h3): the structure type of De Mijnen van Knabbelmoria ({@code guhs:ringh3_mijn}). It is a
 * {@link BarbecuePutStructure} (one template with its centre jigsaw on a cave floor of the Guhbarbecuether; the same JSON
 * fields; everything that asks "is this a cave building" still says yes) with ONE difference: the barbecueput stands on the
 * HIGHEST cave floor of its column, the mine on the LOWEST (just above the frying-sauce sea when there is room).
 * <p>
 * Why: the mine hangs 30 blocks deep under its floor. On the highest floor of a column the land round it is usually far
 * lower (the first real copy stood on a ledge at y 98 with caves 20 to 45 blocks deeper all round it: a fifth of the mine
 * hung in the open as a block of rock). So the floors of the column are tried from the lowest up, and a floor only counts
 * when the whole box of the mine under it lies in the ground ({@link #inDeGrond}: rock or frying sauce, hardly any open air)
 * and there is open cave at its height at most of the four corners ({@link #EISEN}).
 * The one guaranteed copy must always find a spot: when its ring has none, the search of {@link
 * nl.juiced.guhs.world.GegarandeerdPlacement} comes back more patient (flatness factor 2 and 3) and so do the demands.
 */
public class MijnStructure extends BarbecuePutStructure {
    public static final MapCodec<MijnStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.pool),
            Identifier.CODEC.fieldOf("start_jigsaw_name").forGetter(s -> s.jigsaw),
            Codec.INT.fieldOf("check_radius").forGetter(s -> s.straal),
            Codec.INT.fieldOf("max_height_difference").forGetter(s -> s.verschil),
            Codec.INT.optionalFieldOf("headroom", 10).forGetter(s -> s.ruimte),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(BarbecuePutStructure::keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.eigenVoorrang)
    ).apply(i, MijnStructure::new));

    private final Holder<StructureTemplatePool> pool;
    private final Identifier jigsaw;
    private final int straal, verschil, ruimte;
    private final Optional<Integer> eigenVoorrang;

    public MijnStructure(StructureSettings settings, Holder<StructureTemplatePool> pool, Identifier jigsaw, int straal, int verschil, int ruimte,
                         int keepClear, Optional<Integer> voorrang) {
        // (the last argument is ring-h1's surface pool of the barbecueput: the mine only stands under a ceiling)
        super(settings, pool, jigsaw, straal, verschil, ruimte, keepClear, voorrang, Optional.empty());
        this.pool = pool;
        this.jigsaw = jigsaw;
        this.straal = straal;
        this.verschil = verschil;
        this.ruimte = ruimte;
        this.eigenVoorrang = voorrang;
    }

    /**
     * What a floor must be, by the patience of the search (index 1, 2, 3; rows of {corners with a floor at its height, share of
     * open air under it}; one row is enough). Measured on 16 seeds (every floor of every start chunk the search tried, dev
     * server): with "four corners, whatever is under it" the mine had 35 % open air under its floor on average (89 % at worst);
     * with these rows every seed found a spot in the first round, with 7 % at worst.
     */
    private static final double[][][] EISEN = {{}, {{3, 0.08}}, {{2, 0.15}}, {{2, 0.30}, {4, 1.0}}};
    /** The grid of the sample columns under the mine. */
    private static final int STAP = 11;

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        var biome = context.biomeSource().getNoiseBiome(x >> 2, 64 >> 2, z >> 2, context.randomState().sampler());
        if (!biome.is(CAVE_BIOMES)) {
            return Optional.empty();
        }
        // (the guaranteed copy's search grows patient when its ring has no spot: 1, 2, 3; see GegarandeerdPlacement.RONDES)
        double[][] eisen = EISEN[Math.clamp(BouwRuimte.vlakFactor(context, this), 1, EISEN.length - 1)];
        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        int van = Math.max(context.chunkGenerator().getSeaLevel() + 2, context.heightAccessor().getMinY() + 6);
        int tot = context.heightAccessor().getMaxY() + 1 - 12 - ruimte;
        // every floor of the column, the lowest first
        for (int y = van; y <= tot; y++) {
            if (!isVloer(column, y) || !vrij(column, y, ruimte)) {
                continue;
            }
            int hoeken = hoeken(context, x, z, y);
            double lucht = 0.0;
            for (double[] eis : eisen) {
                if (hoeken >= eis[0]) {
                    lucht = Math.max(lucht, eis[1]);
                }
            }
            if (lucht <= 0.0) {
                continue;
            }
            Optional<GenerationStub> stub = BouwRuimte.claim(context, this, JigsawPlacement.addPieces(context, pool, Optional.of(jigsaw), 1,
                    new BlockPos(x, y, z), false, Optional.empty(), new JigsawStructure.MaxDistance(64), PoolAliasLookup.EMPTY, DimensionPadding.ZERO,
                    LiquidSettings.IGNORE_WATERLOGGING));
            if (stub.isPresent() && (lucht >= 1.0 || inDeGrond(context, stub.get().getPiecesBuilder().getBoundingBox(), y, lucht))) {
                return stub;
            }
        }
        return Optional.empty();
    }

    /** How many of the four corners of the land between the forecourts have a floor at about this height (open cave there). */
    private int hoeken(GenerationContext context, int x, int z, int y) {
        int n = 0;
        for (int[] c : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            if (vloerBij(context, x + c[0] * straal / 2, z + c[1] * straal / 2, y)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Is the mine in the ground here? Columns on a grid over the whole box of the building, from its bottom to two under the
     * floor: at most {@code lucht} of those cells is open air (rock and the frying sauce both hide the mine).
     */
    private static boolean inDeGrond(GenerationContext context, BoundingBox doos, int vloer, double lucht) {
        int onder = Math.max(doos.minY(), context.heightAccessor().getMinY() + 1), boven = vloer - 2;
        int kolommen = ((doos.getXSpan() - 1 - STAP / 2) / STAP + 1) * ((doos.getZSpan() - 1 - STAP / 2) / STAP + 1);
        int mag = (int) (kolommen * (boven - onder + 1) * lucht), open = 0;
        for (int x = doos.minX() + STAP / 2; x <= doos.maxX(); x += STAP) {
            for (int z = doos.minZ() + STAP / 2; z <= doos.maxZ(); z += STAP) {
                NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
                for (int y = onder; y <= boven; y++) {
                    if (column.getBlock(y).isAir() && ++open > mag) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean vloerBij(GenerationContext context, int x, int z, int y) {
        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        for (int dy = 0; dy <= verschil / 2; dy++) {
            for (int s : new int[] {y + dy, y - dy}) {
                if (isVloer(column, s) && vrij(column, s, 4)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isVloer(NoiseColumn column, int y) {
        BlockState onder = column.getBlock(y - 1);
        return column.getBlock(y).isAir() && !onder.isAir() && onder.getFluidState().isEmpty();
    }

    private static boolean vrij(NoiseColumn column, int y, int hoog) {
        for (int i = 0; i < hoog; i++) {
            if (!column.getBlock(y + i).isAir()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StructureType<?> type() {
        return RingH3Feature.MIJN_TYPE.get();
    }
}
