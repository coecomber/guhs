package nl.juiced.guhs.feature.elftocht;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.world.level.ScheduledTickAccess;
/**
 * The Elf-Guhjestocht's night lights: they light up when it gets dark and go out in the morning (checked on random
 * ticks, like a lamplighter walking along the canal: the lights come on one by one at dusk).
 */
public final class NachtlichtBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Night (in day time ticks): from dusk to dawn. */
    public static final int NACHT_VAN = 12300, NACHT_TOT = 23700;

    /** Is it lamp time in this level? */
    public static boolean nacht(Level level) {
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        long t = nl.juiced.guhs.world.GuhTime.timeOfDay(level);
        return t >= NACHT_VAN && t < NACHT_TOT;
    }

    /** Switches a night light to the right state for the time of day (true if it changed). */
    static boolean bijwerken(ServerLevel level, BlockPos pos, BlockState state) {
        boolean aan = nacht(level);
        if (state.getValue(LIT) != aan) {
            level.setBlock(pos, state.setValue(LIT, aan), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    /** A paper lampion (standing or hanging, like a lantern). */
    public static class Lampion extends LanternBlock {
        public Lampion(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(LIT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(LIT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null ? null : state.setValue(LIT, nacht(context.getLevel()));
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            bijwerken(level, pos, state);
        }
    }

    /** A vuurkorf: an iron fire basket on three legs; at night it burns (flames, sparks, a warm crackle; no harm at all). */
    public static class Vuurkorf extends Block {
        public static final MapCodec<Vuurkorf> CODEC = simpleCodec(Vuurkorf::new);
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

        public Vuurkorf(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(LIT, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LIT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(LIT, nacht(context.getLevel()));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return Block.canSupportCenter(level, pos.below(), Direction.UP);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return direction == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            bijwerken(level, pos, state);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (!state.getValue(LIT)) {
                return;
            }
            double x = pos.getX() + 0.5, y = pos.getY() + 0.8, z = pos.getZ() + 0.5;
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
            }
            if (random.nextInt(5) == 0) {
                level.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.3, y + 0.2, z + (random.nextDouble() - 0.5) * 0.3, 0, 0.04, 0);
            }
            if (random.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.SMOKE, x, y + 0.4, z, 0, 0.05, 0);
            }
            if (random.nextInt(14) == 0) {
                level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6f, 0.8f + random.nextFloat() * 0.4f, false);
            }
        }
    }

    private NachtlichtBlock() {
    }
}
