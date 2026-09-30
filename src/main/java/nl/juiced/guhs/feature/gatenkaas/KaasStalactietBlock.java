package nl.juiced.guhs.feature.gatenkaas;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A kaas stalactite (hanging, tip down) or stalagmite (standing, tip up): the pointed dripstone of the gatenkaas caves.
 * Stacked pieces get thinner towards the tip (base, middle, frustum, tip). The tip of a stalactite drips kaassaus; if
 * what it hangs on is gone, the whole stalactite falls (and it hurts: don't stand under it!). Falling onto the tip of a
 * stalagmite hurts too.
 */
public class KaasStalactietBlock extends Block implements Fallable {
    public static final MapCodec<KaasStalactietBlock> CODEC = simpleCodec(KaasStalactietBlock::new);
    public static final EnumProperty<Direction> TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;

    private static final VoxelShape TIP_UP = Block.box(5, 0, 5, 11, 11, 11);
    private static final VoxelShape TIP_DOWN = Block.box(5, 5, 5, 11, 16, 11);
    private static final VoxelShape FRUSTUM = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape MIDDLE = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape BASE = Block.box(2, 0, 2, 14, 16, 14);

    public KaasStalactietBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIP_DIRECTION, Direction.UP).setValue(THICKNESS, DripstoneThickness.TIP));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP_DIRECTION, THICKNESS);
    }

    public static boolean isStalactite(BlockState state) {
        return state.getBlock() instanceof KaasStalactietBlock && state.getValue(TIP_DIRECTION) == Direction.DOWN;
    }

    private static boolean sameDirection(BlockState state, Direction dir) {
        return state.getBlock() instanceof KaasStalactietBlock && state.getValue(TIP_DIRECTION) == dir;
    }

    /** A stalactite hangs on something solid (or on the piece above it); a stalagmite stands on something. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction dir = state.getValue(TIP_DIRECTION);
        BlockPos behind = pos.relative(dir.getOpposite());
        BlockState there = level.getBlockState(behind);
        return there.isFaceSturdy(level, behind, dir) || sameDirection(there, dir);
    }

    /** Base, middle, frustum or tip: how many pieces there are further towards the tip, and whether one is behind. */
    public static DripstoneThickness thickness(LevelReader level, BlockPos pos, Direction dir) {
        BlockState next = level.getBlockState(pos.relative(dir));
        if (!sameDirection(next, dir)) {
            return DripstoneThickness.TIP;
        }
        BlockState nextNext = level.getBlockState(pos.relative(dir, 2));
        if (!sameDirection(nextNext, dir)) {
            return DripstoneThickness.FRUSTUM;
        }
        BlockState behind = level.getBlockState(pos.relative(dir.getOpposite()));
        return sameDirection(behind, dir) ? DripstoneThickness.MIDDLE : DripstoneThickness.BASE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction wanted = context.getNearestLookingVerticalDirection().getOpposite();
        for (Direction dir : new Direction[] {wanted, wanted.getOpposite()}) {
            BlockState state = defaultBlockState().setValue(TIP_DIRECTION, dir);
            if (canSurvive(state, level, pos)) {
                return state.setValue(THICKNESS, thickness(level, pos, dir));
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction from, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (from.getAxis() != Direction.Axis.Y) {
            return state;
        }
        Direction dir = state.getValue(TIP_DIRECTION);
        if (from == dir.getOpposite() && !canSurvive(state, level, pos)) {
            level.scheduleTick(pos, this, dir == Direction.DOWN ? 2 : 1);
            return state;
        }
        return state.setValue(THICKNESS, thickness(level, pos, dir));
    }

    /** Lost its hold: a stalagmite just breaks, a stalactite falls down, all pieces below it too. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (canSurvive(state, level, pos)) {
            return;
        }
        if (!isStalactite(state)) {
            level.destroyBlock(pos, true);
            return;
        }
        BlockPos.MutableBlockPos at = pos.mutable();
        BlockState piece = state;
        while (isStalactite(piece)) {
            FallingBlockEntity falling = FallingBlockEntity.fall(level, at, piece);
            if (piece.getValue(THICKNESS) == DripstoneThickness.TIP) {
                falling.setHurtsEntities(Math.max(6f, 1 + pos.getY() - at.getY()), 40);
                break;
            }
            at.move(Direction.DOWN);
            piece = level.getBlockState(at);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == DripstoneThickness.TIP) {
            entity.causeFallDamage(fallDistance + 2f, 2f, level.damageSources().stalagmite());
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingStalactite(entity);
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (!fallingBlock.isSilent()) {
            level.levelEvent(1045, pos, 0);
        }
    }

    /** The tip of a stalactite drips kaassaus (honey-coloured drops). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (isStalactite(state) && state.getValue(THICKNESS) == DripstoneThickness.TIP && random.nextInt(6) == 0) {
            Vec3 offset = state.getOffset(level, pos);
            level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.FALLING_HONEY : ParticleTypes.DRIPPING_HONEY,
                    pos.getX() + 0.5 + offset.x, pos.getY() + 0.3, pos.getZ() + 0.5 + offset.z, 0, 0, 0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = switch (state.getValue(THICKNESS)) {
            case TIP, TIP_MERGE -> state.getValue(TIP_DIRECTION) == Direction.DOWN ? TIP_DOWN : TIP_UP;
            case FRUSTUM -> FRUSTUM;
            case MIDDLE -> MIDDLE;
            case BASE -> BASE;
        };
        Vec3 offset = state.getOffset(level, pos);
        return shape.move(offset.x, 0, offset.z);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected float getMaxHorizontalOffset() {
        return 0.125f;
    }
}
