package nl.juiced.guhs.feature.molentje;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The three blocks of the Maanmolentje: the {@link Tandwiel} (a cog on the puzzle wall, its arrow in {@link Tandwiel#STAND}),
 * the {@link Kussentje} (a Blub comes to sit on it and blow) and the {@link Wieken} (the hub; client.WiekenRenderer draws the
 * four big sails, still or turning: {@link Wieken#DRAAIT}).
 */
public final class MolenBlokken {
    private MolenBlokken() {
    }

    /** A box (pixels, for a block facing south) turned to every side. */
    static Map<Direction, VoxelShape> vormen(double x0, double y0, double z0, double x1, double y1, double z1) {
        Map<Direction, VoxelShape> m = new EnumMap<>(Direction.class);
        m.put(Direction.SOUTH, Block.box(x0, y0, z0, x1, y1, z1));
        m.put(Direction.NORTH, Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0));
        m.put(Direction.WEST, Block.box(16 - z1, y0, x0, 16 - z0, y1, x1));
        m.put(Direction.EAST, Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0));
        return m;
    }

    /** A cog on the wall: click it and it turns a quarter (and at the mill its neighbour along: {@link Molen#klikTandwiel}). */
    public static class Tandwiel extends HorizontalDirectionalBlock {
        public static final MapCodec<Tandwiel> CODEC = simpleCodec(Tandwiel::new);
        /** Where its arrow points: 0 up, 1 right, 2 down, 3 left. */
        public static final IntegerProperty STAND = IntegerProperty.create("stand", 0, 3);
        private static final Map<Direction, VoxelShape> VORM = vormen(1, 1, 14, 15, 15, 16);

        public Tandwiel(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(STAND, 0));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, STAND);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM.get(state.getValue(FACING));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
                Molen.klikTandwiel(sl, pos, sp);
            }
            return InteractionResult.SUCCESS;
        }

        /** Turn one cog a quarter clockwise (with a little click). */
        static void draai(Level level, BlockPos pos) {
            BlockState s = level.getBlockState(pos);
            if (s.getBlock() instanceof Tandwiel) {
                level.setBlock(pos, s.setValue(STAND, (s.getValue(STAND) + 1) % 4), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6f, 1.2f + level.getRandom().nextFloat() * 0.3f);
            }
        }
    }

    /** The blowing cushion: click it at the mill and the nearest Blub hops over to sit on it and blow. */
    public static class Kussentje extends Block {
        public static final MapCodec<Kussentje> CODEC = simpleCodec(Kussentje::new);
        private static final VoxelShape VORM = Block.box(2, 0, 2, 14, 4, 14);

        public Kussentje(Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
                Molen.roepBlub(sl, pos, sp);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** The hub of the sails; its renderer draws the four big sails ({@link #DRAAIT}: turning, else still at a slant). */
    public static class Wieken extends HorizontalDirectionalBlock implements EntityBlock {
        public static final MapCodec<Wieken> CODEC = simpleCodec(Wieken::new);
        public static final BooleanProperty DRAAIT = BooleanProperty.create("draait");
        private static final Map<Direction, VoxelShape> VORM = vormen(4, 4, 0, 12, 12, 8);

        public Wieken(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(DRAAIT, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, DRAAIT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM.get(state.getValue(FACING));
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new WiekenBlockEntity(pos, state);
        }
    }

    /** Only there so the renderer can draw the sails; it keeps no data (the run lives in {@link Molen.Stand}). */
    public static class WiekenBlockEntity extends BlockEntity {
        public WiekenBlockEntity(BlockPos pos, BlockState state) {
            super(MolentjeFeature.WIEKEN_BE.get(), pos, state);
        }
    }
}
