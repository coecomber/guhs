package nl.juiced.guhs.feature.creche;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** The little blocks and items of the Knuffelcreche (registered in {@link CrecheFeature}). */
public final class CrecheBlocks {
    /**
     * guhs:feestslingers: a string of paper flags with guh faces, knutseled by the babyguhtjes. Hangs anywhere (no
     * collision), and it's the creche's feesttaakje for the Grote Knusfeest (tag guhs:knus/feestslingers).
     */
    public static class Feestslingers extends Block {
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
        private static final VoxelShape X = Block.box(0, 6, 7, 16, 16, 9), Z = Block.box(7, 6, 0, 9, 16, 16);

        public Feestslingers(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AXIS);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }
    }

    /** guhs:speelkleed: a soft play mat with a guh face (a carpet). */
    public static class Speelkleed extends CarpetBlock {
        public Speelkleed(Properties properties) {
            super(properties);
        }
    }

    /** An item with a line of Dutch lore under its name (lang &lt;item&gt;.lore). */
    public static class Lore extends Item {
        public Lore(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** A block item with a line of lore. */
    public static class LoreBlock extends BlockItem {
        public LoreBlock(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    private CrecheBlocks() {
    }
}
