package nl.juiced.guhs.feature.techsaus;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * De Sausslang: a thin hose for sauce. It joins to the hoses next to it and to everything that may hold fluid (a Sauspomp, a
 * Sausvat, a sauce machine, a cauldron, a tank of another mod), in all six directions. It holds nothing itself and needs no
 * vadskracht: pumps push through it, machines slurp through it ({@link Slangen}). Resources: tools/features/tech_vloeistof.py
 * (a multipart blockstate: a knot in the middle and an arm per joined side).
 */
public class SausslangBlock extends Block {
    public static final MapCodec<SausslangBlock> CODEC = simpleCodec(SausslangBlock::new);
    private static final Map<Direction, BooleanProperty> KANTEN = new EnumMap<>(Map.of(
            Direction.NORTH, BlockStateProperties.NORTH, Direction.EAST, BlockStateProperties.EAST, Direction.SOUTH, BlockStateProperties.SOUTH,
            Direction.WEST, BlockStateProperties.WEST, Direction.UP, BlockStateProperties.UP, Direction.DOWN, BlockStateProperties.DOWN));
    private static final VoxelShape KNOOP = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMEN = new EnumMap<>(Map.of(
            Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5), Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16),
            Direction.WEST, Block.box(0, 5, 5, 5, 11, 11), Direction.EAST, Block.box(11, 5, 5, 16, 11, 11),
            Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11), Direction.UP, Block.box(5, 11, 5, 11, 16, 11)));
    private final VoxelShape[] vormen = new VoxelShape[64];

    public SausslangBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty p : KANTEN.values()) {
            state = state.setValue(p, false);
        }
        registerDefaultState(state);
        for (int i = 0; i < 64; i++) {
            VoxelShape vorm = KNOOP;
            for (Direction kant : Direction.values()) {
                if ((i & 1 << kant.ordinal()) != 0) {
                    vorm = Shapes.or(vorm, ARMEN.get(kant));
                }
            }
            vormen[i] = vorm.optimize();
        }
    }

    /** The property that says whether the hose is joined on this side. */
    public static BooleanProperty kant(Direction kant) {
        return KANTEN.get(kant);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.NORTH, BlockStateProperties.EAST, BlockStateProperties.SOUTH, BlockStateProperties.WEST,
                BlockStateProperties.UP, BlockStateProperties.DOWN);
    }

    /** Does a hose here join to what is on this side: another hose, or anything that may hold fluid? */
    public static boolean verbindt(LevelReader level, BlockPos pos, Direction kant) {
        BlockPos buur = pos.relative(kant);
        if (level.getBlockState(buur).getBlock() instanceof SausslangBlock) {
            return true;
        }
        return level instanceof Level echt && echt.isLoaded(buur) && echt.getCapability(Capabilities.Fluid.BLOCK, buur, kant.getOpposite()) != null;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction kant : Direction.values()) {
            state = state.setValue(kant(kant), verbindt(context.getLevel(), context.getClickedPos(), kant));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (level instanceof Level echt) {
            Slangen.veranderd(echt);
            return state.setValue(kant(direction), verbindt(level, pos, direction));
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        Slangen.veranderd(level);
        if (!oldState.is(this) && !level.isClientSide()) {
            // however it got here (/setblock, a structure, code): it joins what is around it
            BlockState goed = state;
            for (Direction kant : Direction.values()) {
                goed = goed.setValue(kant(kant), verbindt(level, pos, kant));
            }
            if (goed != state) {
                level.setBlock(pos, goed, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Slangen.veranderd(level);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int i = 0;
        for (Direction kant : Direction.values()) {
            if (state.getValue(kant(kant))) {
                i |= 1 << kant.ordinal();
            }
        }
        return vormen[i];
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState uit = state;
        for (Direction kant : Direction.Plane.HORIZONTAL) {
            uit = uit.setValue(kant(rotation.rotate(kant)), state.getValue(kant(kant)));
        }
        return uit;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState uit = state;
        for (Direction kant : Direction.Plane.HORIZONTAL) {
            uit = uit.setValue(kant(mirror.mirror(kant)), state.getValue(kant(kant)));
        }
        return uit;
    }
}
