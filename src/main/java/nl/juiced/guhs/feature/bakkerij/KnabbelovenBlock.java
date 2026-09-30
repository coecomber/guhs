package nl.juiced.guhs.feature.bakkerij;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The knabbeloven: a round pink guh oven with a face (its eyes above the door, little ears on top) and a chimney pipe.
 * Right-click it to bake: choose dough, shape and topping, slide it in and take it out at the right moment
 * ({@link Bakken}). While something bakes it glows and puffs knabbelwolkjes.
 */
public class KnabbelovenBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<KnabbelovenBlock> CODEC = simpleCodec(KnabbelovenBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);

    public KnabbelovenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer p) {
            Bakken.open(p, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Glows while baking: off again when nothing bakes in it any more. */
    public static void aan(ServerLevel level, BlockPos pos, int ticks) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof KnabbelovenBlock) {
            if (!state.getValue(LIT)) {
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            }
            level.scheduleTick(pos, state.getBlock(), Math.max(20, ticks));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && !Bakken.bakt(level, pos)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        } else if (state.getValue(LIT)) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        if (state.getValue(LIT)) {
            double fx = x + facing.getStepX() * 0.52, fz = z + facing.getStepZ() * 0.52;
            level.addParticle(ParticleTypes.SMALL_FLAME, fx + (random.nextDouble() - 0.5) * 0.3, y + 0.25, fz + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
            if (random.nextInt(3) == 0) {
                level.addParticle(BakkerijFeature.KNABBELWOLKJE.get(), x - facing.getStepX() * 0.25, y + 1.15, z - facing.getStepZ() * 0.25,
                        0, 0.03, 0);
            }
            if (random.nextInt(8) == 0) {
                level.playLocalSound(x, y, z, net.minecraft.sounds.SoundEvents.FURNACE_FIRE_CRACKLE, net.minecraft.sounds.SoundSource.BLOCKS,
                        0.6f, 1.2f, false);
            }
        } else if (random.nextInt(12) == 0) {
            level.addParticle(BakkerijFeature.MEELSTOFJE.get(), x + (random.nextDouble() - 0.5) * 0.8, y + 1.0, z + (random.nextDouble() - 0.5) * 0.8,
                    0, 0.01, 0);
        }
    }
}
