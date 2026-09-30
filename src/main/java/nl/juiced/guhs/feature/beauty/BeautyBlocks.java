package nl.juiced.guhs.feature.beauty;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The blocks of the guh beauty theatre: the invisible stage markers and the loaner wardrobe. */
public final class BeautyBlocks {
    /** What a stage marker marks. */
    public enum Spot implements StringRepresentable {
        /** Where the model stands while it's being dressed (the nose of the guh-face stage). */
        MODEL,
        /** The end of the catwalk, where the model poses for the jury. */
        EINDE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * An invisible marker in the beauty theatre (like the verstopguh markers): you can't see, touch or break it. The
     * Showguh finds the stage and the end of the catwalk with them.
     */
    public static class Plek extends Block {
        public static final MapCodec<Plek> CODEC = simpleCodec(Plek::new);
        public static final EnumProperty<Spot> SPOT = EnumProperty.create("spot", Spot.class);

        public Plek(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(SPOT, Spot.MODEL));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(SPOT);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }
    }

    /**
     * The loaner wardrobe ("leenkledingkast"): full of guh clothes you may borrow during a show. Right-click it while
     * you're dressing the model to open the wardrobe screen; nothing ever comes out of it as an item.
     */
    public static class Leenkast extends HorizontalDirectionalBlock {
        public static final MapCodec<Leenkast> CODEC = simpleCodec(Leenkast::new);

        public Leenkast(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                BeautyShow show = BeautyShow.of(serverPlayer);
                if (show != null && show.isDressing()) {
                    show.openWardrobe(serverPlayer);
                } else {
                    serverPlayer.sendOverlayMessage(Component.translatable("gui.guhs.beauty.leenkast.closed")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    private BeautyBlocks() {
    }
}
