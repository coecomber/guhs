package nl.juiced.guhs.feature.mijnhulp;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;

/**
 * 1.7.0 (ronde 44): the Pluisladder, a soft pink roll of rope ladder. Put it against a wall at the edge of a hole (like a
 * ladder) and it unrolls down by itself, a piece every two ticks, until it reaches the floor (or {@link #MAX} pieces).
 * You climb it like a ladder (block tag minecraft:climbable). The top piece ({@link #TOP}) hangs on the wall; the pieces
 * below hang on the one above. Breaking any piece rolls the whole ladder up: you get the one roll back (the top piece's
 * loot, or {@link #playerWillDestroy} when you broke a lower one). Dig the floor away under it and it unrolls further.
 */
public class PluisladderBlock extends LadderBlock {
    public static final MapCodec<LadderBlock> CODEC = simpleCodec(PluisladderBlock::new);
    /** The top piece: hangs on the wall and is the one that drops the roll. */
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    /** At most this many pieces. */
    public static final int MAX = 256;
    private static final int TEMPO = 2;

    public PluisladderBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TOP, true));
    }

    @Override
    public MapCodec<LadderBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TOP);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState s = super.getStateForPlacement(context);
        return s == null ? null : s.setValue(TOP, true);
    }

    /** The top piece needs the wall, a lower piece the piece above it (facing the same way). */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(TOP)) {
            BlockState boven = level.getBlockState(pos.above());
            return boven.is(this) && boven.getValue(FACING) == state.getValue(FACING);
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction richting,
                                     BlockPos buurPos, BlockState buur, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        // lost its hold (the wall, the piece above), or room below: look again in a moment (tick)
        if (!state.canSurvive(level, pos) || richting == Direction.DOWN && kanHangen(level, buurPos)) {
            ticks.scheduleTick(pos, this, richting == Direction.DOWN ? TEMPO : 1);
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oud, boolean piston) {
        super.onPlace(state, level, pos, oud, piston);
        if (!level.isClientSide() && !oud.is(this)) {
            level.scheduleTick(pos, this, TEMPO);
        }
    }

    /** Lost its hold: falls (the top piece gives the roll back); otherwise unrolls one piece further if there is room. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        BlockPos onder = pos.below();
        if (!kanHangen(level, onder) || lengte(level, pos) >= MAX) {
            return;
        }
        boolean water = level.getFluidState(onder).is(Fluids.WATER) && level.getFluidState(onder).isSource();
        level.setBlock(onder, state.setValue(TOP, false).setValue(WATERLOGGED, water), Block.UPDATE_ALL);
        if (random.nextInt(3) == 0) {
            level.playSound(null, onder, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.4f, 1.3f + random.nextFloat() * 0.3f);
        }
    }

    /** Room for a piece: air, or something soft that gives way (grass, snow layer, water), not lava. */
    private static boolean kanHangen(LevelReader level, BlockPos p) {
        if (p.getY() < level.getMinY() || !level.isInsideBuildHeight(p)) {
            return false;
        }
        BlockState s = level.getBlockState(p);
        if (s.isAir()) {
            return true;
        }
        return s.canBeReplaced() && (s.getFluidState().isEmpty() || s.getFluidState().is(Fluids.WATER) && s.getFluidState().isSource())
                && !(s.getBlock() instanceof PluisladderBlock);
    }

    /** How many pieces hang above this one, plus one (counts up to {@link #MAX}). */
    private int lengte(LevelReader level, BlockPos pos) {
        int n = 1;
        BlockPos p = pos.above();
        while (n < MAX && level.getBlockState(p).is(this)) {
            n++;
            p = p.above();
        }
        return n;
    }

    /**
     * Rolls the whole ladder up: every piece above and below (in this column, facing the same way) goes without loot; the
     * player gets the roll back (from the top piece's loot when that is the one broken, here otherwise).
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            Direction kant = state.getValue(FACING);
            for (int richting : new int[]{1, -1}) {
                if (richting > 0 && state.getValue(TOP)) {
                    continue;       // (the top piece itself: nothing of this ladder above it)
                }
                BlockPos p = pos.above(richting);
                for (int i = 0; i < MAX; i++) {
                    BlockState s = level.getBlockState(p);
                    if (!s.is(this) || s.getValue(FACING) != kant || richting < 0 && s.getValue(TOP)) {
                        break;      // (below: another roll's top piece is another ladder)
                    }
                    level.setBlock(p, s.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                            : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    if (s.getValue(TOP)) {
                        break;      // (above: up to and with this ladder's top piece)
                    }
                    p = p.above(richting);
                }
            }
            if (!state.getValue(TOP) && !player.isCreative()) {
                popResource(level, pos, new ItemStack(this));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
