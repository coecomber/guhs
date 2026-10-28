package nl.juiced.guhs.feature.droomwolkje;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.dromen.DroomReis;

/**
 * "Mijn droomwolkje": the brievenbusje on the Droomplein that floats you to your own droomwolkje (after Snoes' story).
 * Sneak-click: how to make friends who may build along, and how to visit someone.
 */
public class WolkjeBusBlock extends HorizontalDirectionalBlock {
    static final VoxelShape VORM = Shapes.or(Block.box(7, 0, 7, 9, 8, 9), Block.box(4, 8, 3, 12, 15, 13));
    public static final MapCodec<WolkjeBusBlock> CODEC = simpleCodec(WolkjeBusBlock::new);

    public WolkjeBusBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer p)) {
            return InteractionResult.SUCCESS;
        }
        if (!DroomReis.inDroom(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.droomplein.bus.wakker").withStyle(ChatFormatting.GRAY));
        } else if (p.isShiftKeyDown()) {
            p.sendSystemMessage(Component.translatable("gui.guhs.droomwolkje.bus.uitleg").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            Wolkjes.naarEigen(p);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
