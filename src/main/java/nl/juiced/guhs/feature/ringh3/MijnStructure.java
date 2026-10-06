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
 * hung in the open as a block of rock). Under the lowest floor there is only rock and the sauce sea, so the mine is where a
 * mine belongs: in the ground under the plain.
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
        super(settings, pool, jigsaw, straal, verschil, ruimte, keepClear, voorrang);
        this.pool = pool;
        this.jigsaw = jigsaw;
        this.straal = straal;
        this.verschil = verschil;
        this.ruimte = ruimte;
        this.eigenVoorrang = voorrang;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        var biome = context.biomeSource().getNoiseBiome(x >> 2, 64 >> 2, z >> 2, context.randomState().sampler());
        if (!biome.is(CAVE_BIOMES)) {
            return Optional.empty();
        }
        int y = laagsteVloer(context, x, z);
        if (y == Integer.MIN_VALUE) {
            return Optional.empty();
        }
        // the four corners of the forecourts' land: a floor at about the same height
        for (int[] c : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            if (!vloerBij(context, x + c[0] * straal / 2, z + c[1] * straal / 2, y)) {
                return Optional.empty();
            }
        }
        return BouwRuimte.claim(context, this, JigsawPlacement.addPieces(context, pool, Optional.of(jigsaw), 1, new BlockPos(x, y, z), false,
                Optional.empty(), new JigsawStructure.MaxDistance(64), PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING));
    }

    /** The LOWEST cave floor (first free y above solid ground) of this column above the sauce sea with room above, or MIN_VALUE. */
    private int laagsteVloer(GenerationContext context, int x, int z) {
        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        int van = Math.max(context.chunkGenerator().getSeaLevel() + 2, context.heightAccessor().getMinY() + 6);
        int tot = context.heightAccessor().getMaxY() + 1 - 12 - ruimte;
        for (int y = van; y <= tot; y++) {
            if (isVloer(column, y) && vrij(column, y, ruimte)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
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
