package nl.juiced.guhs.feature.spiesburcht;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * De verkoolde mikakop: the charred head of a Knekel-Mika (a skull, standing or on a wall; you can wear it). Three of
 * them on a T of four as_blok wake up the Aangebrande Mika, in any dimension ({@link #checkSpawn}).
 */
public class MikakopBlock extends HorizontalDirectionalBlock implements Equipable {
    public static final MapCodec<MikakopBlock> CODEC = simpleCodec(MikakopBlock::new);
    protected static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);
    @Nullable
    private static BlockPattern pattern;

    public MikakopBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock()) && level instanceof ServerLevel server) {
            checkSpawn(server, pos);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.55,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
    }

    public static boolean isHead(BlockState state) {
        return state.is(SpiesburchtFeature.VERKOOLDE_MIKAKOP.get()) || state.is(SpiesburchtFeature.VERKOOLDE_MIKAKOP_MUUR.get());
    }

    /** The T: three heads on top of three ash blocks, one more ash block under the middle, air in the lower corners. */
    public static BlockPattern pattern() {
        if (pattern == null) {
            pattern = BlockPatternBuilder.start()
                    .aisle("^^^", "###", "~#~")
                    .where('#', b -> b.getState().is(SpiesburchtFeature.AANGEBRAND_BASIS))
                    .where('^', b -> isHead(b.getState()))
                    .where('~', b -> b.getState().isAir())
                    .build();
        }
        return pattern;
    }

    /** A head was just put down: is it the last one of a T? Then the Aangebrande Mika wakes up. */
    @Nullable
    public static AangebrandeMikaEntity checkSpawn(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || level.getDifficulty() == Difficulty.PEACEFUL) {
            return null;
        }
        BlockPattern.BlockPatternMatch match = pattern().find(level, pos);
        if (match == null) {
            return null;
        }
        AangebrandeMikaEntity boss = SpiesburchtFeature.AANGEBRANDE_MIKA.get().create(level);
        if (boss == null) {
            return null;
        }
        CarvedPumpkinBlock.clearPatternBlocks(level, match);
        BlockInWorld centre = match.getBlock(1, 2, 0);
        BlockPos at = centre.getPos();
        float yaw = match.getForwards().getAxis() == Direction.Axis.X ? 0.0f : 90.0f;
        boss.moveTo(at.getX() + 0.5, at.getY() + 0.55, at.getZ() + 0.5, yaw, 0.0f);
        boss.yBodyRot = yaw;
        boss.startSpawning();
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(50.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, boss);
            nl.juiced.guhs.quest.GuhAdvancements.grant(player, "aangebrande_mika_opgeroepen");
        }
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 60, 1.0, 1.5, 1.0, 0.05);
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
        return boss;
    }

    /** The head on a wall. */
    public static class Wall extends MikakopBlock {
        public static final MapCodec<Wall> WALL_CODEC = simpleCodec(Wall::new);
        public static final DirectionProperty WALL_FACING = FACING;
        private static final Map<Direction, VoxelShape> AABBS = ImmutableMap.of(
                Direction.NORTH, Block.box(4.0, 4.0, 8.0, 12.0, 12.0, 16.0),
                Direction.SOUTH, Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 8.0),
                Direction.EAST, Block.box(0.0, 4.0, 4.0, 8.0, 12.0, 12.0),
                Direction.WEST, Block.box(8.0, 4.0, 4.0, 16.0, 12.0, 12.0));

        public Wall(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return WALL_CODEC;
        }

        @Override
        public String getDescriptionId() {
            return SpiesburchtFeature.VERKOOLDE_MIKAKOP.get().getDescriptionId();
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return AABBS.get(state.getValue(FACING));
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = defaultBlockState();
            for (Direction direction : context.getNearestLookingDirections()) {
                if (direction.getAxis().isHorizontal()) {
                    state = state.setValue(FACING, direction.getOpposite());
                    if (!context.getLevel().getBlockState(context.getClickedPos().relative(direction)).canBeReplaced(context)) {
                        return state;
                    }
                }
            }
            return null;
        }
    }
}
