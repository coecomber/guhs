package nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The other Spiegelburcht blocks: the light source (a crystal eye that shines one way) and the crystal lock (glows when lit). */
public final class KristalBlokken {
    private KristalBlokken() {
    }

    /** 0 = north, 1 = east, 2 = south, 3 = west (the beam directions of {@link Lichtstraal}). */
    public static Direction richting(int r) {
        return switch (r & 3) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    /** A light source: shines a beam in its FACING direction (the burcht/hall logic traces it). */
    public static class Lichtbron extends Block {
        public static final MapCodec<Lichtbron> CODEC = simpleCodec(Lichtbron::new);
        public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

        public Lichtbron(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        // Y-INT: turns along with structures (DraaiGameTests)
        @Override
        protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
            return nl.juiced.guhs.block.Draaien.facing(state, FACING, rotation);
        }

        @Override
        protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
            return nl.juiced.guhs.block.Draaien.facing(state, FACING, mirror);
        }
    }

    /** A crystal lock: LIT while a beam ends on it. */
    public static class Slot extends Block {
        public static final MapCodec<Slot> CODEC = simpleCodec(Slot::new);
        public static final BooleanProperty LIT = BlockStateProperties.LIT;
        private static final VoxelShape VORM = Block.box(2, 0, 2, 14, 14, 14);

        public Slot(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(LIT, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(LIT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return VORM;
        }
    }
}
