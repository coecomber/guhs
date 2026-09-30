package nl.juiced.guhs.feature.mewtwo;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The kloon-eiland's smaller blocks:
 * <ul>
 *   <li>{@link Tankwand}: the invisible glass of the kloontank around its controller (a click = a click on the tank);</li>
 *   <li>{@link Notitieplek}: a little stack of lab notes (nummer 1..6): each player takes its note once;</li>
 *   <li>{@link Onderdelenkist}: a parts crate (soort 1..4): each player takes its part once (while the professor asks);</li>
 *   <li>{@link Knabbelschaal}: the grote knabbelschaal on the arena (the big meal);</li>
 *   <li>{@link Deco}: the guh-computer and the rack of test tubes; {@link Papieren}: messy papers on the floor.</li>
 * </ul>
 * The quest spots sparkle for you while you still need them (client: {@link MewtwoStand}).
 */
public final class MewtwoBlokken {
    public static final IntegerProperty NUMMER = IntegerProperty.create("nummer", 1, 6);
    public static final IntegerProperty SOORT = IntegerProperty.create("soort", 1, 4);
    public static final VoxelShape COMPUTER_VORM = Shapes.or(Block.box(3, 0, 4, 13, 12, 13), Block.box(3, 0, 0.5, 13, 1, 4.5));
    public static final VoxelShape BUISJES_VORM = Block.box(2, 0, 5, 14, 9.5, 11);
    private static final VoxelShape NOTITIE_VORM = Block.box(3, 0, 3, 13, 2.6, 13);
    private static final VoxelShape KIST_VORM = Block.box(1.5, 0, 1.5, 14.5, 11.5, 14.5);
    private static final VoxelShape SCHAAL_VORM = Shapes.or(Block.box(5, 0, 5, 11, 5, 11), Block.box(0, 5, 0, 16, 12, 16));
    private static final VoxelShape PAPIER_VORM = Block.box(0, 0, 0, 16, 0.5, 16);

    private MewtwoBlokken() {
    }

    // =================================================================================================================
    /** The invisible glass of the kloontank (full collision, no model: the tank's renderer draws the glass). */
    public static class Tankwand extends Block {
        public Tankwand(Properties p) {
            super(p);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
            return true;
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1f;
        }

        /** The tank's controller next to / under this part (within one block sideways, two down), or null. */
        public static BlockPos tank(Level level, BlockPos pos) {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -2, -1), pos.offset(1, 0, 1))) {
                if (level.getBlockState(p).is(MewtwoFeature.KLOONTANK.get())) {
                    return p.immutable();
                }
            }
            return null;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            BlockPos tank = tank(level, pos);
            if (tank == null) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer sp) {
                MewtwoVerhaal.klikTank(sp, tank);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                                  BlockHitResult hit) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
    }

    // =================================================================================================================
    /** A block that faces the player who placed it (the base of the quest spots and the deco). */
    public abstract static class Gericht extends HorizontalDirectionalBlock {
        protected Gericht(Properties p) {
            super(p);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }
    }

    // =================================================================================================================
    /** A stack of lab notes: its note (nummer) for each player once; sparkles while you haven't got it. */
    public static class Notitieplek extends Gericht {
        public static final MapCodec<Notitieplek> CODEC = simpleCodec(Notitieplek::new);

        public Notitieplek(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(NUMMER, 1));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, NUMMER);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return NOTITIE_VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                MewtwoVerhaal.vindNotitie(sp, pos, state.getValue(NUMMER));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0 && !MewtwoStand.heeftNotitie(state.getValue(NUMMER))) {
                level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.3,
                        pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
            }
        }
    }

    // =================================================================================================================
    /** A parts crate: its part (soort) for each player once, while the professor asks for them; sparkles until then. */
    public static class Onderdelenkist extends Gericht {
        public static final MapCodec<Onderdelenkist> CODEC = simpleCodec(Onderdelenkist::new);

        public Onderdelenkist(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SOORT, 1));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, SOORT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return KIST_VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                MewtwoVerhaal.vindOnderdeel(sp, pos, state.getValue(SOORT));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0 && MewtwoStand.zoektOnderdeel(state.getValue(SOORT))) {
                level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.8,
                        pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
            }
        }
    }

    // =================================================================================================================
    /** The grote knabbelschaal: the double portion for the big meal goes in here. */
    public static class Knabbelschaal extends Block {
        public Knabbelschaal(Properties p) {
            super(p);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SCHAAL_VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                MewtwoVerhaal.klikSchaal(sp, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                                  BlockHitResult hit) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(4) == 0 && MewtwoStand.stap() == MewtwoVoortgang.MAALTIJD) {
                level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8, pos.getY() + 0.9,
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8, 0, 0.02, 0);
            }
        }
    }

    // =================================================================================================================
    /** A decorative lab block with its own shape (the guh-computer, the test tubes). */
    public static class Deco extends Gericht {
        private final VoxelShape vorm;
        public static final MapCodec<Deco> CODEC = simpleCodec(p -> new Deco(p, Shapes.block()));

        public Deco(Properties p, VoxelShape vorm) {
            super(p);
            this.vorm = vorm;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return vorm;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (this == MewtwoFeature.REAGEERBUISJES.get() && random.nextInt(8) == 0) {
                level.addParticle(MewtwoFeature.BUBBEL.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.6,
                        pos.getZ() + 0.5, 0, 0.02, 0);
            }
        }
    }

    // =================================================================================================================
    /** Messy papers on the floor (a thin layer; you walk through them). */
    public static class Papieren extends Gericht {
        public static final MapCodec<Papieren> CODEC = simpleCodec(Papieren::new);

        public Papieren(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return PAPIER_VORM;
        }
    }
}
