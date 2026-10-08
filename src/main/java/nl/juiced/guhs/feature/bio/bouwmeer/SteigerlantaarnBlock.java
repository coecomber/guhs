package nl.juiced.guhs.feature.bio.bouwmeer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * De steigerlantaarn: a small white lantern with a blossom-pink cap on a short foot, for the end of a jetty. It lights by
 * itself at dusk and goes out at dawn ({@link Klok#lampAan}). Nothing ticks: a random tick looks at the clock, and the
 * lantern of a botenhuisje is also set by its mooring while a player is near ({@link MeerpaalBlock#zorg}), so that one
 * switches right at dusk. A reward of the visser-guh and craftable; stands on anything, needs no support.
 */
public class SteigerlantaarnBlock extends Block {
    public static final MapCodec<SteigerlantaarnBlock> CODEC = simpleCodec(SteigerlantaarnBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int LICHT = 14;
    private static final VoxelShape VORM = Shapes.or(Block.box(6, 0, 6, 10, 5, 10), Block.box(4, 5, 4, 12, 13, 12), Block.box(3, 13, 3, 13, 16, 13));

    public SteigerlantaarnBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(LIT, Klok.lampAan(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        bijwerken(state, level, pos);
    }

    /** Lights or douses the lantern for the time of day; true when it changed. */
    public boolean bijwerken(BlockState state, ServerLevel level, BlockPos pos) {
        boolean aan = level.dimensionType().hasFixedTime() || Klok.lampAan(level, pos);
        if (state.getValue(LIT) == aan) {
            return false;
        }
        level.setBlock(pos, state.setValue(LIT, aan), Block.UPDATE_ALL);
        return true;
    }
}
