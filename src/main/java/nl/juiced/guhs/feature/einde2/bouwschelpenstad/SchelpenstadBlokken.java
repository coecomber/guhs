package nl.juiced.guhs.feature.einde2.bouwschelpenstad;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * einde2, slice bouw_schelpenstad: the block classes of the decorative blocks of the road trip (De Ruige Kroeg, the
 * souvenir shop of Schelpenstad). None of them does anything: they stand, hang and look the part, and none asks for a
 * neighbour (a tile build puts them down as its template has them). Registered in {@link E2BouwSchelpenstadFeature};
 * models and textures: tools/features/einde2_bouw_schelpenstad_blokken.py.
 */
public final class SchelpenstadBlokken {
    private SchelpenstadBlokken() {
    }

    /** A box in the model's own space (north = the front), turned to the way a block looks. */
    static Map<Direction, VoxelShape> gedraaid(double x0, double y0, double z0, double x1, double y1, double z1) {
        Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
        out.put(Direction.NORTH, Block.box(x0, y0, z0, x1, y1, z1));
        out.put(Direction.EAST, Block.box(16 - z1, y0, x0, 16 - z0, y1, x1));
        out.put(Direction.SOUTH, Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0));
        out.put(Direction.WEST, Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0));
        return out;
    }

    /** A block with one shape that is smaller than a cube (the lamp, the cushion, the sprinkler, the postcard rack). */
    public static class Vorm extends Block {
        private final VoxelShape vorm;

        public Vorm(VoxelShape vorm, Properties properties) {
            super(properties);
            this.vorm = vorm;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return vorm;
        }
    }

    /** A block that looks one way: placed facing the player, turned and mirrored with a build; its shape turns with it. */
    public abstract static class Gericht extends Block {
        public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
        private final Map<Direction, VoxelShape> vormen;

        protected Gericht(Map<Direction, VoxelShape> vormen, Properties properties) {
            super(properties);
            this.vormen = vormen;
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return vormen.get(state.getValue(FACING));
        }
    }

    /** Something flat on the wall behind it: the socket of the drying lamp, the neon sign of De Ruige Kroeg. */
    public static class Wandding extends Gericht {
        public static final MapCodec<Wandding> CODEC = simpleCodec(p -> new Wandding(2, 14, p));

        /** The plate reaches from {@code laag} to {@code hoog} (of 16) across the wall and up it, one pixel thick. */
        public Wandding(int laag, int hoog, Properties properties) {
            super(gedraaid(laag, laag, 15, hoog, hoog, 16), properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
    }

    /**
     * A dried souvenir on its little stand: flat as a pancake, with googly eyes. Block state {@code soort} 0-3 = a star, a
     * block of gatenkaas, a kwal, a fish guh; a placed one takes the look that follows from where it stands, so a shelf
     * fills up with all four.
     */
    public static class Souvenir extends Gericht {
        public static final MapCodec<Souvenir> CODEC = simpleCodec(Souvenir::new);
        public static final IntegerProperty SOORT = IntegerProperty.create("soort", 0, 3);

        public Souvenir(Properties properties) {
            super(gedraaid(1, 0, 5, 15, 15, 11), properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SOORT, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, SOORT);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return super.getStateForPlacement(context).setValue(SOORT, soortOp(context.getClickedPos()));
        }

        /** The look of a souvenir that is put down here (0-3): neighbours differ. */
        public static int soortOp(BlockPos pos) {
            return Math.floorMod(pos.getX() + 2 * pos.getY() + 3 * pos.getZ(), 4);
        }
    }
}
