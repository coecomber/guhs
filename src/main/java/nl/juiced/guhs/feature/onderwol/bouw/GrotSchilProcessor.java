package nl.juiced.guhs.feature.onderwol.bouw;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import org.jspecify.annotations.Nullable;

/**
 * Processor {@code guhs:onderwol_grot_schil}: the rock around the rooms of an underground template.
 * <p>
 * A template made for {@code guhs:grot_bouw} holds its rooms as air (carved out of the world), leaves the solid rock
 * further away out (the world's own rock stays, with its ores and caves) and marks the rock right around the rooms with
 * {@link #MARKER} (bedrock). Where the world already has solid rock there, it stays as it is; where the world is open
 * (another cave runs through, a lake, the sky) the marker becomes the layer stone of that spot ({@link #laagsteen}), so a
 * room never opens into a neighbouring cave or a lake by accident, and its walls are always of the layer it lies in.
 * <p>
 * Only the blocks inside the box being placed (one chunk) look at the world: during worldgen the neighbouring chunks may
 * not be readable (the rest of the template is placed when those chunks generate).
 */
public class GrotSchilProcessor extends StructureProcessor {
    public static final GrotSchilProcessor INSTANCE = new GrotSchilProcessor();
    public static final MapCodec<GrotSchilProcessor> CODEC = MapCodec.unit(() -> INSTANCE);
    /** The marker of the rock around the rooms in a template. */
    public static final Block MARKER = Blocks.BEDROCK;
    /** The natural layer stones of the Guhmension (data/guhs/tags/block/onderwol/laagsteen.json). */
    public static final TagKey<Block> LAAGSTEEN = TagKey.create(Registries.BLOCK, Guhs.id("onderwol/laagsteen"));
    /** The average layer borders (OnderWolLagen: viltsteen from about y 64, knuffelsteen from 40, guhkristalsteen from 20). */
    public static final int VILT_Y = 62, KNUFFEL_Y = 41, KRISTAL_Y = 20;

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pivot,
                                                                       StructureTemplate.StructureBlockInfo original,
                                                                       StructureTemplate.StructureBlockInfo current, StructurePlaceSettings settings) {
        if (!current.state().is(MARKER)) {
            return current;
        }
        BlockPos pos = current.pos();
        BoundingBox box = settings.getBoundingBox();
        if (box == null || !box.isInside(pos)) {
            return current;   // (not placed now anyway: outside the chunk being generated)
        }
        BlockState world = level.getBlockState(pos);
        if (vast(world)) {
            return null;      // (the world's own rock stays)
        }
        return new StructureTemplate.StructureBlockInfo(pos, laagsteen(level, pos, box), null);
    }

    /** Solid rock (or any solid block): not air, no fluid, not something a block may simply replace (plants, snow...). */
    public static boolean vast(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
    }

    /**
     * The layer stone for this spot: the nearest natural layer stone straight around it (up to 4 blocks, only inside
     * {@code box}), else the stone of its average layer by height.
     */
    public static BlockState laagsteen(LevelReader level, BlockPos pos, @Nullable BoundingBox box) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int d = 1; d <= 4; d++) {
            for (Direction dir : Direction.values()) {
                p.set(pos).move(dir, d);
                if (box != null && !box.isInside(p)) {
                    continue;
                }
                BlockState s = level.getBlockState(p);
                if (s.is(LAAGSTEEN)) {
                    return s;
                }
            }
        }
        return laagsteen(pos.getY());
    }

    /** The stone of the average layer at this height (pink wool above the viltsteen). */
    public static BlockState laagsteen(int y) {
        String id = y < KRISTAL_Y ? "guh_kristalsteen" : y < KNUFFEL_Y ? "knuffelsteen" : y < VILT_Y ? "viltsteen" : null;
        if (id == null) {
            return Blocks.PINK_WOOL.defaultBlockState();
        }
        Block b = BuiltInRegistries.BLOCK.getValue(Guhs.id(id));
        return (b == Blocks.AIR ? Blocks.STONE : b).defaultBlockState();
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return OnderWolBouw.SCHIL.get();
    }
}
