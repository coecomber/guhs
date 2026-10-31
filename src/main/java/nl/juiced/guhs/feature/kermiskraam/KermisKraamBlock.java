package nl.juiced.guhs.feature.kermiskraam;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The counter of a kermis stall. The kermis stamp only has to put this block down: the block keeps its own stall guh behind it
 * ({@link KermisKraamBlockEntity}), one block to the back of FACING. FACING is the customer's side, and turns with the template.
 */
public class KermisKraamBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 11, 16);

    public final Kraam kraam;
    private final MapCodec<KermisKraamBlock> codec;

    public KermisKraamBlock(Kraam kraam, Properties properties) {
        super(properties);
        this.kraam = kraam;
        this.codec = simpleCodec(p -> new KermisKraamBlock(kraam, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
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
        if (!(player instanceof ServerPlayer p) || !(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        KermisKraam.klik(p, kraam, KermisKraam.zorgVoorGuh(server, pos), pos);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KermisKraamBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != KermisKraamFeature.KRAAM_BE.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<KermisKraamBlockEntity>) KermisKraamBlockEntity::tick;
    }
}
