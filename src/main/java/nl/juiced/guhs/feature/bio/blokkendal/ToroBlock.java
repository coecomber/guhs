package nl.juiced.guhs.feature.bio.blokkendal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.world.GuhTime;

/**
 * De stenen guh-lantaarn (toro): one block, a stone lantern with guh ears on its cap and a little guh face as its window.
 * It lights by itself at dusk and goes out at dawn. No ticking: a random tick looks at the clock (so the lanterns along a
 * path come on one by one, like a lamplighter passing), and a lantern that switches tells the lanterns within a few
 * blocks to look too, so one garden does not stay half lit.
 */
public class ToroBlock extends Block {
    public static final MapCodec<ToroBlock> CODEC = simpleCodec(ToroBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Lamp time in day ticks: from dusk to dawn. */
    public static final int NACHT_VAN = 12300, NACHT_TOT = 23700;
    /** How far a switching lantern nudges the others (blocks, sideways; two up and down). */
    private static final int BUREN = 5;
    private static final VoxelShape SHAPE = Shapes.or(Block.column(10, 0, 2), Block.column(4, 2, 7), Block.column(8, 7, 12), Block.column(12, 12, 14),
            Block.column(6, 14, 16));

    public ToroBlock(Properties properties) {
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
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(LIT, nacht(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        bijwerken(state, level, pos, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        bijwerken(state, level, pos, random);
    }

    /** Lights or douses this lantern for the time of day; when it changed, the lanterns nearby follow a moment later. */
    public boolean bijwerken(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean aan = nacht(level, pos);
        if (state.getValue(LIT) == aan) {
            return false;
        }
        level.setBlock(pos, state.setValue(LIT, aan), Block.UPDATE_ALL);
        for (BlockPos other : BlockPos.betweenClosed(pos.offset(-BUREN, -2, -BUREN), pos.offset(BUREN, 2, BUREN))) {
            BlockState s = level.getBlockState(other);
            if (s.is(this) && s.getValue(LIT) != aan) {
                level.scheduleTick(other.immutable(), this, 10 + random.nextInt(30));
            }
        }
        return true;
    }

    /**
     * Is it lamp time here? The overworld's clock (every dimension of the mod follows it); a dimension whose time stands
     * still keeps its lanterns burning.
     */
    public static boolean nacht(Level level, BlockPos pos) {
        Boolean test = Klok.gezet(pos);
        if (test != null) {
            return test;
        }
        if (level.dimensionType().hasFixedTime()) {
            return true;
        }
        long t = GuhTime.timeOfDay(level);
        return t >= NACHT_VAN && t < NACHT_TOT;
    }

    /** Game tests set their own day or night inside their own box (the real clock is shared by every test). */
    public static final class Klok {
        private static final Map<AABB, Boolean> GEZET = new ConcurrentHashMap<>();

        public static void zet(AABB box, @Nullable Boolean nacht) {
            if (nacht == null) {
                GEZET.remove(box);
            } else {
                GEZET.put(box, nacht);
            }
        }

        @Nullable
        static Boolean gezet(BlockPos pos) {
            if (GEZET.isEmpty()) {
                return null;
            }
            for (var e : GEZET.entrySet()) {
                if (e.getKey().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                    return e.getValue();
                }
            }
            return null;
        }

        private Klok() {
        }
    }
}
