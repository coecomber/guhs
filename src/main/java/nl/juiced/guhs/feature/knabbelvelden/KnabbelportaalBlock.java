package nl.juiced.guhs.feature.knabbelvelden;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Knabbelportaal: a 2 x 2 x 1 frame of kaasknabbel blocks with a swirling pink-and-yellow cheese cloud in the middle
 * (an animated texture per part, tools/features/knabbelvelden.py). One item places all four parts ({@link #DEEL}: 0 = lower
 * left, 1 = lower right, 2 = upper left, 3 = upper right, along {@link #AS}); breaking one breaks the whole portal (one
 * item drops). Right-click any part: the big Knabbelvelden screen ({@link Pensioen#open}).
 */
public class KnabbelportaalBlock extends Block {
    public static final MapCodec<KnabbelportaalBlock> CODEC = simpleCodec(KnabbelportaalBlock::new);
    public static final EnumProperty<Direction.Axis> AS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty DEEL = IntegerProperty.create("deel", 0, 3);
    private static final VoxelShape LANGS_X = Block.box(0, 0, 4, 16, 16, 12), LANGS_Z = Block.box(4, 0, 0, 12, 16, 16);
    private static final DustParticleOptions ROZE = new DustParticleOptions(0xFF8FCB, 1.0f), GEEL = new DustParticleOptions(0xFFE07A, 0.9f);

    public KnabbelportaalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AS, Direction.Axis.X).setValue(DEEL, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AS, DEEL);
    }

    /** The direction "to the right" along the portal. */
    static Direction langs(Direction.Axis as) {
        return as == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    /** The lower left part of the portal this part belongs to. */
    public static BlockPos basis(BlockState state, BlockPos pos) {
        int deel = state.getValue(DEEL);
        return pos.relative(langs(state.getValue(AS)), -(deel & 1)).below(deel >> 1);
    }

    static BlockPos deelPos(BlockPos basis, Direction.Axis as, int deel) {
        return basis.relative(langs(as), deel & 1).above(deel >> 1);
    }

    /** Puts a whole portal with its lower left part at basis (world generation of the plots, tests). */
    public static void zet(Level level, BlockPos basis, Direction.Axis as) {
        BlockState s = KnabbelveldenFeature.PORTAAL.get().defaultBlockState().setValue(AS, as);
        for (int deel = 0; deel < 4; deel++) {
            level.setBlock(deelPos(basis, as, deel), s.setValue(DEEL, deel), 3);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis as = context.getHorizontalDirection().getClockWise().getAxis();
        BlockPos basis = context.getClickedPos();
        for (int deel = 1; deel < 4; deel++) {
            BlockPos p = deelPos(basis, as, deel);
            if (!context.getLevel().getWorldBorder().isWithinBounds(p) || p.getY() >= context.getLevel().getMaxY()
                    || !context.getLevel().getBlockState(p).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState().setValue(AS, as).setValue(DEEL, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Direction.Axis as = state.getValue(AS);
        for (int deel = 1; deel < 4; deel++) {
            level.setBlock(deelPos(pos, as, deel), state.setValue(DEEL, deel), 3);
        }
    }

    /** Are all four parts there? */
    private static boolean heel(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos basis = basis(state, pos);
        Direction.Axis as = state.getValue(AS);
        for (int deel = 0; deel < 4; deel++) {
            BlockState s = level.getBlockState(deelPos(basis, as, deel));
            if (!(s.getBlock() instanceof KnabbelportaalBlock) || s.getValue(AS) != as || s.getValue(DEEL) != deel) {
                return false;
            }
        }
        return true;
    }

    /** A part that went missing takes the rest with it (only the part you broke drops the portal). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return neighborState.isAir() && !heel(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AS) == Direction.Axis.X ? LANGS_X : LANGS_Z;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            Pensioen.open(sp, basis(state, pos));
        }
        return InteractionResult.SUCCESS;
    }

    /** The cheese cloud breathes out little pink and yellow motes (from the middle of the portal). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(DEEL) != 0 || random.nextInt(2) != 0) {
            return;
        }
        Direction l = langs(state.getValue(AS));
        double x = pos.getX() + 0.5 + l.getStepX() * 0.5 + (random.nextDouble() - 0.5) * 1.4 * Math.abs(l.getStepX());
        double z = pos.getZ() + 0.5 + l.getStepZ() * 0.5 + (random.nextDouble() - 0.5) * 1.4 * Math.abs(l.getStepZ());
        double y = pos.getY() + 0.2 + random.nextDouble() * 1.6;
        level.addParticle(random.nextBoolean() ? ROZE : GEEL, x, y, z, 0, 0.02, 0);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
