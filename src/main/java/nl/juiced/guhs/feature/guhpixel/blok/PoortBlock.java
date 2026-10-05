package nl.juiced.guhs.feature.guhpixel.blok;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.Toegang;

/**
 * The Guhpixel-poort for home ({@code guhs:guhpixel_poort}): a little old beige monitor; right-click it to go to the lobby.
 * Its recipe needs a Netwerkkabeltje, which only the lobby greeter gives, so it can only be made after the first visit;
 * a player who has not unlocked Guhpixel gets the same funny refusal as /lobby.
 */
public class PoortBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PoortBlock> CODEC = simpleCodec(PoortBlock::new);
    private static final VoxelShape VORM = Block.box(1, 0, 1, 15, 14, 15);

    public PoortBlock(Properties properties) {
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
        if (player instanceof ServerPlayer sp && !Guhpixel.echt(level)) {
            Toegang.naarLobby(sp);
        }
        return InteractionResult.SUCCESS;
    }
}
