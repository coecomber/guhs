package nl.juiced.guhs.feature.eilanden;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The invisible stream above a {@link WolkenliftBlock} (think bubble column, but made of cloud puffs). Only the stream
 * block at an entity's feet pushes it, so standing in two blocks at once never counts double:
 * <ul>
 *     <li>up: rises at {@link #LIFT_SPEED}; in the top two blocks it slows down and puffs you off towards FACING (onto
 *     the island next to it);</li>
 *     <li>down: floats you down at {@link #SINK_SPEED}, without any fall damage.</li>
 * </ul>
 * It runs on the client (your own movement) and the server (mobs, items), like vanilla's bubble column. It needs the
 * pad or more stream below it: break the pad and the whole column puffs away. Water, lava and kaassaus can't wash it
 * away (they flow around it), so nobody can break the islands' lifts with a bucket from outside the protected square.
 */
public class WolkenstroomBlock extends HorizontalDirectionalBlock implements LiquidBlockContainer {
    public static final MapCodec<WolkenstroomBlock> CODEC = simpleCodec(WolkenstroomBlock::new);
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final double LIFT_SPEED = 0.9, SINK_SPEED = 0.28, TOP_UP = 0.36, TOP_PUSH = 0.32, SIDEWAYS = 0.1;

    public WolkenstroomBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DOWN);
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
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(EilandenFeature.WOLKENLIFT_ITEM.get());
    }

    // --- the column holds together from the pad up -----------------------------------------------------------------

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(this) || below.is(EilandenFeature.WOLKENLIFT.get())) && below.getValue(DOWN) == state.getValue(DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            level.scheduleTick(pos, this, 1); // one block per tick: no huge chain of updates
        }
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.removeBlock(pos, false);
        }
    }

    // --- fluids flow around it instead of washing the column away ------------------------------------------------------

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return false;
    }

    /** The highest block of this column (the one with no stream above it). */
    public static boolean isTop(BlockGetter level, BlockPos pos) {
        return !level.getBlockState(pos.above()).is(EilandenFeature.WOLKENSTROOM.get());
    }

    // --- the ride --------------------------------------------------------------------------------------------------

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity || entity instanceof ItemEntity) || entity.isPassenger()
                || entity instanceof Player player && player.getAbilities().flying) {
            return;
        }
        if (Mth.floor(entity.getY() + 1.0E-4) != pos.getY()) {
            return; // only the block at its feet
        }
        push(state, level, pos, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            EilandenEvents.onLift(player, state.getValue(DOWN));
        }
    }

    /** What the stream does to an entity with its feet in this block. */
    public static void push(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        Vec3 v = entity.getDeltaMovement();
        entity.resetFallDistance();
        if (state.getValue(DOWN)) {
            entity.setDeltaMovement(clamp(v.x), -SINK_SPEED, clamp(v.z));
            return;
        }
        if (isTop(level, pos) || isTop(level, pos.above())) {
            // the top: slow down and puff off towards the island
            Direction facing = state.getValue(FACING);
            entity.setDeltaMovement(facing.getStepX() * TOP_PUSH, TOP_UP, facing.getStepZ() * TOP_PUSH);
        } else {
            entity.setDeltaMovement(clamp(v.x), Math.min(LIFT_SPEED, Math.max(v.y, 0) + 0.2), clamp(v.z));
        }
    }

    private static double clamp(double sideways) {
        return Mth.clamp(sideways, -SIDEWAYS, SIDEWAYS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + random.nextDouble(), y = pos.getY() + random.nextDouble(), z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0, state.getValue(DOWN) ? -0.12 : 0.18, 0);
        }
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, state.getValue(DOWN) ? -0.05 : 0.08, 0);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
