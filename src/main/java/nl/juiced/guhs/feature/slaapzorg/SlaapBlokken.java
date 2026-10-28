package nl.juiced.guhs.feature.slaapzorg;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The slaapzorg blocks: little furniture that turns to face you ({@link Deco}), with a lamp you click on and off
 * ({@link Lampje}: the nachtkastje and the maannachtlampjes), the Muziekdoosje and the Mobiel that play the lullaby
 * ({@link Speeldoos}: a countdown in the block state, no block entity needed for the song), and the droomvanger on a wall
 * ({@link Wand}).
 */
public final class SlaapBlokken {
    private SlaapBlokken() {
    }

    /** A shape (pixels, facing north) turned to every side. */
    static Map<Direction, VoxelShape> vormen(double[]... boxes) {
        Map<Direction, VoxelShape> m = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
                shape = Shapes.or(shape, switch (dir) {
                    case SOUTH -> Block.box(16 - x1, b[1], 16 - z1, 16 - x0, b[4], 16 - z0);
                    case EAST -> Block.box(16 - z1, b[1], x0, 16 - z0, b[4], x1);
                    case WEST -> Block.box(z0, b[1], 16 - x1, z1, b[4], 16 - x0);
                    default -> Block.box(x0, b[1], z0, x1, b[4], z1);
                });
            }
            m.put(dir, shape.optimize());
        }
        return m;
    }

    /** Furniture that faces you. */
    public static class Deco extends HorizontalDirectionalBlock {
        final Map<Direction, VoxelShape> vorm;
        final double[][] boxes;

        public Deco(Properties p, double[]... boxes) {
            super(p);
            this.boxes = boxes;
            this.vorm = vormen(boxes);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(p -> new Deco(p, boxes));
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
            return vorm.get(state.getValue(FACING));
        }
    }

    /** A little lamp (nachtkastje, maannachtlampjes): click it on or off. Light level from the block's properties. */
    public static class Lampje extends Deco {
        public static final BooleanProperty LIT = BlockStateProperties.LIT;

        public Lampje(Properties p, double[]... boxes) {
            super(p, boxes);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, true));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(p -> new Lampje(p, boxes));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, LIT);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                boolean aan = !state.getValue(LIT);
                level.setBlock(pos, state.setValue(LIT, aan), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, aan ? 1.4f : 1.1f);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /**
     * The Muziekdoosje and the Mobiel: click and the lullaby plays (about 24 seconds; {@link Slaapliedje}), the guhs around
     * yawn and fall asleep. {@link #SPEELT}: how many 2-second steps are left (0: quiet; a click then starts it again).
     */
    public static class Speeldoos extends Deco implements EntityBlock {
        public static final IntegerProperty SPEELT = IntegerProperty.create("speelt", 0, 12);
        private final double bereik;
        private final float volume;
        private final boolean mobiel;

        public Speeldoos(Properties p, double bereik, float volume, boolean mobiel, double[]... boxes) {
            super(p, boxes);
            this.bereik = bereik;
            this.volume = volume;
            this.mobiel = mobiel;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SPEELT, 0));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(p -> new Speeldoos(p, bereik, volume, mobiel, boxes));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, SPEELT);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (state.getValue(SPEELT) > 0) {
                return InteractionResult.CONSUME;   // still playing: no second song on top
            }
            if (level instanceof ServerLevel sl) {
                speel(sl, pos, state);
            }
            return InteractionResult.SUCCESS;
        }

        /** Starts the song (also the test command and the game tests); returns how many guhs listen. */
        public int speel(ServerLevel level, BlockPos pos, BlockState state) {
            level.setBlock(pos, state.setValue(SPEELT, 12), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 40);
            return Slaapliedje.zing(level, pos, bereik, volume);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int rest = state.getValue(SPEELT);
            if (rest <= 0) {
                return;
            }
            Slaapliedje.noten(level, mobiel ? pos.below() : pos);
            level.setBlock(pos, state.setValue(SPEELT, rest - 1), Block.UPDATE_ALL);
            if (rest > 1) {
                level.scheduleTick(pos, this, 40);
            }
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return mobiel ? new MobielBlockEntity(pos, state) : null;
        }
    }

    /** The droomvanger: hangs flat on a wall (or stands against the side you look at), no collision. */
    public static class Wand extends Deco {
        public Wand(Properties p, double[]... boxes) {
            super(p, boxes);
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(p -> new Wand(p, boxes));
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction kant = context.getClickedFace();
            return defaultBlockState().setValue(FACING, kant.getAxis().isHorizontal() ? kant : context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }
    }
}
