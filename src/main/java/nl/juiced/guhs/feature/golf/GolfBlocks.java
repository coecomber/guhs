package nl.juiced.guhs.feature.golf;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * The special blocks of the guh golf course. They only come with the course (you can't break them there, and they
 * drop nothing): the Golfguh finds the holes by scanning for them.
 */
public final class GolfBlocks {
    /** Which of the 9 holes a tee or a cup belongs to. */
    public static final IntegerProperty HOLE = IntegerProperty.create("hole", 1, 9);

    /**
     * 2.10: which level's tee it is (every hole has three: makkelijk close to the cup, medium where it always was, lastig
     * further away or behind the obstacles). Medium is the default: the tees of an older course are all medium tees.
     */
    public enum TeeNiveau implements StringRepresentable {
        MAKKELIJK, MEDIUM, LASTIG;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public Niveau niveau() {
            return Niveau.values()[ordinal()];
        }

        public static TeeNiveau of(Niveau niveau) {
            return values()[niveau.ordinal()];
        }
    }

    public static final EnumProperty<TeeNiveau> NIVEAU = EnumProperty.create("niveau", TeeNiveau.class);

    /**
     * A tee ("afslag"): the ball of hole HOLE starts on it, and FACING is the way you play (the arrow on the mat). 2.10:
     * NIVEAU says for which level (the mat's arrow is green, pink or red).
     */
    public static class Afslag extends HorizontalDirectionalBlock {
        public static final MapCodec<Afslag> CODEC = simpleCodec(Afslag::new);

        public Afslag(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(HOLE, 1)
                    .setValue(NIVEAU, TeeNiveau.MEDIUM));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, HOLE, NIVEAU);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }
    }

    /**
     * The cup of hole HOLE, with its numbered flag. It looks like a real hole, but you walk over it like a normal floor:
     * the guh golf ball finds it by itself (and only drops in when it's slow enough, see GolfBallEntity).
     */
    public static class Hole extends Block {
        public static final MapCodec<Hole> CODEC = simpleCodec(Hole::new);

        public Hole(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(HOLE, 1));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HOLE);
        }
    }

    /** The hub of the guh windmill (a guh face): FACING is where its sails face. The Golfguh turns the sails around it. */
    public static class Molenas extends HorizontalDirectionalBlock {
        public static final MapCodec<Molenas> CODEC = simpleCodec(Molenas::new);

        public Molenas(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
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
    }

    private GolfBlocks() {
    }
}
