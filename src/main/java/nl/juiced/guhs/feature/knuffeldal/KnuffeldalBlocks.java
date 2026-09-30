package nl.juiced.guhs.feature.knuffeldal;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.knus.Seizoen;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The special blocks of the Knuffeldal (2.8): the fluffy biome (knuffelgras, pluisgras, the pluizenboom, the
 * guh-paddenstoel), the guh faces in knuffelsteen, and the blocks of the town, the seasons and the Knusfeest
 * (seizoensbloembak, seizoensslinger, bladerhoopje, sneeuwpopguh, feestbuffettafel, knus_oorkonde).
 */
public final class KnuffeldalBlocks {
    /** The season a seasonal block shows (updated on random ticks, so it follows the season within a minute or two). */
    public static final EnumProperty<Seizoen> SEIZOEN = EnumProperty.create("seizoen", Seizoen.class);

    private KnuffeldalBlocks() {
    }

    /** Is this a spot where the Knuffeldal plants grow (knuffelgras, dirt, grass...)? */
    static boolean groeigrond(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(KnuffeldalFeature.KNUFFELGRAS.get());
    }

    // =================================================================================================================
    // the biome
    // =================================================================================================================

    /** Knuffelgras: soft pink fluffy grass. Bone meal makes pluisgras and little guh-paddenstoelen grow around it. */
    public static class Knuffelgras extends Block implements BonemealableBlock {
        public Knuffelgras(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return level.getBlockState(pos.above()).isAir();
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            for (int i = 0; i < 24; i++) {
                BlockPos p = pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
                if (level.getBlockState(p).is(KnuffeldalFeature.KNUFFELGRAS.get()) && level.getBlockState(p.above()).isAir()) {
                    Block plant = random.nextInt(8) == 0 ? KnuffeldalFeature.GUHPADDENSTOEL.get() : KnuffeldalFeature.PLUISGRAS.get();
                    level.setBlock(p.above(), plant.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** Pluisgras: a tuft of pink fluff (like short grass); now and then a pluisje floats off. */
    public static class Pluisgras extends BushBlock {
        public static final MapCodec<Pluisgras> CODEC = simpleCodec(Pluisgras::new);
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

        public Pluisgras(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BushBlock> codec() {
            return CODEC;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return groeigrond(state) || super.mayPlaceOn(state, level, pos);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(60) == 0) {
                level.addParticle(KnuffeldalFeature.PLUISJE.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.6, pos.getZ() + random.nextDouble(),
                        0, 0.01, 0);
            }
        }
    }

    /** A little guh-paddenstoel (a pink cap with white dots and a tiny guh face). Bone meal: a huge one grows. */
    public static class Guhpaddenstoel extends BushBlock implements BonemealableBlock {
        public static final MapCodec<Guhpaddenstoel> CODEC = simpleCodec(Guhpaddenstoel::new);
        private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 8, 12);

        public Guhpaddenstoel(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BushBlock> codec() {
            return CODEC;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return groeigrond(state) || state.isFaceSturdy(level, pos, Direction.UP);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return random.nextFloat() < 0.4f;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            var feature = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                    .getHolder(KnuffeldalFeature.REUZE_GUHPADDENSTOEL);
            if (feature.isEmpty()) {
                return;
            }
            level.removeBlock(pos, false);
            if (!feature.get().value().place(level, level.getChunkSource().getGenerator(), random, pos)) {
                level.setBlock(pos, state, Block.UPDATE_ALL);
            }
        }
    }

    /** Pluizenboom leaves: fluffy pink, and now and then a pluisje drifts down. */
    public static class Bladeren extends LeavesBlock {
        public Bladeren(Properties properties) {
            super(properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            super.animateTick(state, level, pos, random);
            if (random.nextInt(18) == 0) {
                BlockPos below = pos.below();
                if (!isFaceFull(level.getBlockState(below).getCollisionShape(level, below), Direction.UP)) {
                    ParticleUtils.spawnParticleBelow(level, pos, random, KnuffeldalFeature.PLUISJE.get());
                }
            }
        }
    }

    /** The pluizenboom sapling: grows on knuffelgras (and on anything sturdy that isn't leaves or logs). */
    public static class Zaailing extends SaplingBlock {
        public Zaailing(Properties properties) {
            super(KnuffeldalFeature.PLUIZENBOOM_GROWER, properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return groeigrond(state) || super.mayPlaceOn(state, level, pos)
                    || state.isFaceSturdy(level, pos, Direction.UP) && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
        }
    }

    // =================================================================================================================
    // the building palette: a guh face in knuffelsteen
    // =================================================================================================================

    /**
     * A guh face in a block of knuffelsteen: it looks the way it faces, in one of four moods (0 happy, 1 sleepy,
     * 2 surprised, 3 vads). Right-click: another mood (outside the protected town).
     */
    public static class Gezicht extends HorizontalDirectionalBlock {
        public static final MapCodec<Gezicht> CODEC = simpleCodec(Gezicht::new);
        public static final IntegerProperty STEMMING = IntegerProperty.create("stemming", 0, 3);

        public Gezicht(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STEMMING, 0));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, STEMMING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(STEMMING, context.getLevel().getRandom().nextInt(4));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.cycle(STEMMING), Block.UPDATE_ALL);
                level.playSound(null, pos, ModSounds.GUH_HAPPY.get(), SoundSource.BLOCKS, 0.5f, 1.4f + level.getRandom().nextFloat() * 0.3f);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =================================================================================================================
    // the seasons
    // =================================================================================================================

    /** Keeps a seasonal block showing the current season (random ticks and when placed). */
    static BlockState metSeizoen(BlockState state, Level level) {
        return state.hasProperty(SEIZOEN) ? state.setValue(SEIZOEN, Seizoen.huidig(level)) : state;
    }

    /**
     * A flower box of knuffelsteen whose plants follow the season: pink blossom in spring, sunflowers in summer, orange
     * leaves and little pumpkins in autumn, snow and little firs in winter. The seasonal activities start here
     * (see {@link Seizoensactiviteiten#bloembak}): braid a bloesemkransje, weave a zonnehoedje, knit a sjaaltje...
     */
    public static class SeizoensBloembak extends Block {
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 10, 16);

        public SeizoensBloembak(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(SEIZOEN, Seizoen.LENTE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(SEIZOEN);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return metSeizoen(defaultBlockState(), context.getLevel());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            BlockState now = metSeizoen(state, level);
            if (now != state) {
                level.setBlock(pos, now, Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.02);
            }
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                                  BlockHitResult hit) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer sp && Seizoensactiviteiten.bloembak(sp, (ServerLevel) level, pos, stack)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                Seizoensactiviteiten.bloembakTip(sp, Seizoen.huidig(level));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /**
     * A garland of little flags hanging from its top (strung along x or z), in the colours of the season: pastel
     * blossoms, sunny yellow, autumn orange, winter white-blue with twinkling lights.
     */
    public static class SeizoensSlinger extends Block {
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
        private static final VoxelShape X = Block.box(0, 6, 7, 16, 16, 9), Z = Block.box(7, 6, 0, 9, 16, 16);

        public SeizoensSlinger(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(SEIZOEN, Seizoen.LENTE).setValue(AXIS, Direction.Axis.X));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(SEIZOEN, AXIS);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return metSeizoen(defaultBlockState(), context.getLevel()).setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            BlockState now = metSeizoen(state, level);
            if (now != state) {
                level.setBlock(pos, now, Block.UPDATE_ALL);
            }
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            Seizoen s = state.getValue(SEIZOEN);
            if (random.nextInt(s == Seizoen.WINTER ? 6 : 30) == 0) {
                var particle = switch (s) {
                    case LENTE -> KnuffeldalFeature.BLOESEMBLAADJE.get();
                    case WINTER -> KnuffeldalFeature.SNEEUWVLOKJE.get();
                    default -> KnuffeldalFeature.PLUISJE.get();
                };
                level.addParticle(particle, pos.getX() + random.nextDouble(), pos.getY() + 0.4, pos.getZ() + random.nextDouble(), 0, -0.01, 0);
            }
        }
    }

    /**
     * A leaf pile. In autumn it is a big soft heap: jump in it (fall into it from 1.5 blocks or more) and the leaves fly
     * up (see {@link Seizoensactiviteiten#gesprongen}); you never get hurt. The rest of the year it's a thin layer of
     * leaves. You walk through it.
     */
    public static class Bladerhoopje extends Block {
        public static final BooleanProperty VOL = BooleanProperty.create("vol");
        private static final VoxelShape HOOP = Block.box(0, 0, 0, 16, 10, 16), LAAG = Block.box(0, 0, 0, 16, 2, 16);

        public Bladerhoopje(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(VOL, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(VOL);
        }

        static boolean volNu(Level level) {
            return Seizoen.huidig(level) == Seizoen.HERFST;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(VOL, volNu(context.getLevel()));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(VOL) ? HOOP : LAAG;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
            return direction == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean vol = volNu(level);
            if (state.getValue(VOL) != vol) {
                level.setBlock(pos, state.setValue(VOL, vol), Block.UPDATE_ALL);
            }
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (state.getValue(VOL)) {
                if (entity.fallDistance >= Seizoensactiviteiten.SPRONG) {
                    if (!level.isClientSide()) {
                        Seizoensactiviteiten.gesprongen((ServerLevel) level, pos, entity);
                    }
                    entity.resetFallDistance();
                }
                entity.makeStuckInBlock(state, new net.minecraft.world.phys.Vec3(0.85, 0.6, 0.85));
            }
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
            entity.causeFallDamage(fallDistance, 0f, level.damageSources().fall());   // (never hurts)
        }
    }

    /**
     * A snow guh (two blocks high): two snow blocks with a sneeuwguhkopje on top become one (see
     * {@link KnuffeldalItems.Sneeuwguhkopje}). It melts back to snowballs and the head when you break it.
     */
    public static class Sneeuwpopguh extends HorizontalDirectionalBlock {
        public static final MapCodec<Sneeuwpopguh> CODEC = simpleCodec(Sneeuwpopguh::new);
        public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
        private static final VoxelShape LOWER = Block.box(1, 0, 1, 15, 16, 15), UPPER = Block.box(3, 0, 3, 13, 11, 13);

        public Sneeuwpopguh(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, HALF);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
            DoubleBlockHalf half = state.getValue(HALF);
            if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)
                    && !neighbour.is(this)) {
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();   // the other half is gone
            }
            return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            BlockPos other = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            if (!level.isClientSide() && level.getBlockState(other).is(this)) {
                level.setBlock(other, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
            return super.playerWillDestroy(level, pos, state, player);
        }
    }

    // =================================================================================================================
    // the Knusfeest
    // =================================================================================================================

    /**
     * The feestbuffettafel: a long table with a pink tablecloth. During a feast it is laid ({@link #GEDEKT}): bring
     * things you baked, grew or poured (#guhs:knus/gebak, oogst, thee, kaasmelk, or kaasknabbels) and they go on it
     * ({@link Feestbuffet#opTafel}).
     */
    public static class Feestbuffettafel extends HorizontalDirectionalBlock {
        public static final MapCodec<Feestbuffettafel> CODEC = simpleCodec(Feestbuffettafel::new);
        public static final BooleanProperty GEDEKT = BooleanProperty.create("gedekt");
        private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 12, 0, 16, 16, 16), Block.box(1, 0, 1, 4, 12, 4),
                Block.box(12, 0, 1, 15, 12, 4), Block.box(1, 0, 12, 4, 12, 15), Block.box(12, 0, 12, 15, 12, 15));

        public Feestbuffettafel(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GEDEKT, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, GEDEKT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                                  BlockHitResult hit) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer sp && Feestbuffet.opTafel(sp, (ServerLevel) level, pos, stack)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable(state.getValue(GEDEKT) ? "gui.guhs.knuffeldal.buffet.gedekt"
                        : "gui.guhs.knuffeldal.buffet.leeg").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(GEDEKT) && random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.getX() + random.nextDouble(), pos.getY() + 1.1, pos.getZ() + random.nextDouble(),
                        0, 0.02, 0);
            }
        }
    }

    /**
     * The Knus-oorkonde: a golden-framed certificate with a guh on it, for the Knuffelburgemeester (the Grote Knusfeest's
     * finale). A trophy to put up at home; right-click it for a proud little sparkle.
     */
    public static class KnusOorkonde extends HorizontalDirectionalBlock {
        public static final MapCodec<KnusOorkonde> CODEC = simpleCodec(KnusOorkonde::new);
        private static final VoxelShape NS = Block.box(1, 0, 6, 15, 15, 10), EW = Block.box(6, 0, 1, 10, 15, 15);

        public KnusOorkonde(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.02);
                server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.6f, 1.6f);
                player.sendOverlayMessage(Component.translatable("gui.guhs.knuffeldal.oorkonde").withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** (for the tests) */
    @Nullable
    static BlockState seizoenVan(BlockState state) {
        return state.hasProperty(SEIZOEN) ? state : null;
    }
}
