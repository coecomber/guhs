package nl.juiced.guhs.feature.campingmarkt;

import java.util.Locale;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * bbq2 (camping-markt): the blocks of the Grillcamping and the Nether-Mika-ruilmarkt that do something. Nothing a player
 * does to them stays: a pitched tent is folded up again after a while ({@link Kamperen}), a split log grows back on the
 * chopping block, the camp fire burns down ({@link Kampvuur}), the scales swing back; all progress is the player's own.
 * <ul>
 *   <li>{@link Kampeerplek}: the sign of a free pitch. Click it with the tent bag: your tent stands.</li>
 *   <li>{@link Haring}: a tent peg. Click it: hammered in.</li>
 *   <li>{@link Hakblok}: the Houthakker-guh's chopping block. Click it: the log on it splits, a new one is put on.</li>
 *   <li>{@link KampvuurBlock}: the big camp fire: wood on it lights it, a roasting stick roasts over it.</li>
 *   <li>{@link Weegschaal}: the scales of the Waag (and the reward: at home it weighs what you hold in your two hands).</li>
 *   <li>{@link Vadsstapel}: a numbered stack of vads on the counter of the Waag: weigh two, stamp the fake one.</li>
 * </ul>
 * Resources: tools/features/camping_markt.py.
 */
public final class CampingmarktBlocks {
    private CampingmarktBlocks() {
    }

    // =================================================================================================================
    // the camping
    // =================================================================================================================

    /** The sign of a pitch: FACING = where the door of a tent pitched here looks, BEZET = a tent stands on it right now. */
    public static class Kampeerplek extends HorizontalDirectionalBlock {
        public static final MapCodec<Kampeerplek> CODEC = simpleCodec(Kampeerplek::new);
        public static final BooleanProperty BEZET = BooleanProperty.create("bezet");
        private static final VoxelShape VORM = Block.box(4, 0, 4, 12, 15, 12);

        public Kampeerplek(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(BEZET, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, BEZET);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            return useWithoutItem(state, level, pos, player, hit);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                Kamperen.zetOp(sp, pos);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** A tent peg: VAST = hammered in. It stands on something solid and goes when its tent is folded up. */
    public static class Haring extends Block {
        public static final MapCodec<Haring> CODEC = simpleCodec(Haring::new);
        public static final BooleanProperty VAST = BooleanProperty.create("vast");
        private static final VoxelShape LOS = Block.box(5, 0, 5, 11, 9, 11), IN = Block.box(5, 0, 5, 11, 4, 11);

        public Haring(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(VAST, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(VAST);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(VAST) ? IN : LOS;
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
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                         BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
            return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            return useWithoutItem(state, level, pos, player, hit);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                Kamperen.slaHaring(sp, pos, state);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** The chopping block: STAM = a log stands on it. A click splits the log; a moment later the next one stands there. */
    public static class Hakblok extends Block {
        public static final MapCodec<Hakblok> CODEC = simpleCodec(Hakblok::new);
        public static final BooleanProperty STAM = BooleanProperty.create("stam");
        /** Ticks until the next log stands on the block. */
        public static final int NIEUW = 14;
        private static final VoxelShape BLOK = Block.box(1, 0, 1, 15, 9, 15), MET_STAM = Shapes.or(BLOK, Block.box(5, 9, 5, 11, 16, 11));

        public Hakblok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(STAM, true));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(STAM);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(STAM) ? MET_STAM : BLOK;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            return useWithoutItem(state, level, pos, player, hit);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer sp && state.getValue(STAM)) {
                server.setBlock(pos, state.setValue(STAM, false), Block.UPDATE_ALL);
                server.scheduleTick(pos, this, NIEUW);
                server.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0f, 0.8f + server.getRandom().nextFloat() * 0.2f);
                server.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LOG.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 0.9,
                        pos.getZ() + 0.5, 14, 0.25, 0.2, 0.25, 0.05);
                Kamperen.gehakt(sp, pos);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!state.getValue(STAM)) {
                level.setBlock(pos, state.setValue(STAM, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7f, 0.9f);
            }
        }
    }

    /**
     * The big camp fire of the camping: BRANDT = it burns (light, flames, crackling). What a click does is in
     * {@link Kampvuur#klik}: wood on it, poke it up, roast over it. While it burns it ticks once a second ({@link Kampvuur#feest}).
     */
    public static class KampvuurBlock extends Block {
        public static final MapCodec<KampvuurBlock> CODEC = simpleCodec(KampvuurBlock::new);
        public static final BooleanProperty BRANDT = BooleanProperty.create("brandt");
        private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 7, 16);

        public KampvuurBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(BRANDT, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BRANDT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                Kampvuur.klik(sp, pos, state, hand);
            } else if (stack.getItem() instanceof RoosterstokItem && state.getValue(BRANDT) && RoosterstokItem.heeftMarshmallow(player)) {
                player.startUsingItem(hand);   // (the client holds the stick over the fire at once; the server decides)
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                Kampvuur.klik(sp, pos, state, InteractionHand.MAIN_HAND);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (state.getValue(BRANDT)) {
                Kampvuur.feest(level, pos);
                level.scheduleTick(pos, this, Kampvuur.FEEST_STAP);
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (!state.getValue(BRANDT)) {
                return;
            }
            double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
            if (random.nextInt(8) == 0) {
                level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.8f + random.nextFloat(), random.nextFloat() * 0.7f + 0.6f, false);
            }
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.7, y + 0.1 + random.nextDouble() * 0.5,
                        z + (random.nextDouble() - 0.5) * 0.7, 0, 0.03 + random.nextDouble() * 0.03, 0);
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.LAVA, x, y + 0.2, z, (random.nextDouble() - 0.5) * 0.1, 0.1, (random.nextDouble() - 0.5) * 0.1);
            }
            if (random.nextInt(2) == 0) {
                level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x + (random.nextDouble() - 0.5) * 0.4, y + 1.0,
                        z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.05, 0.0);
            }
        }
    }

    // =================================================================================================================
    // the market
    // =================================================================================================================

    /** How the scales hang: level, or down on the left or on the right (seen from the front). */
    public enum Stand implements StringRepresentable {
        MIDDEN, LINKS, RECHTS;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The scales. STAND swings back to the middle by itself. In the Waag the stacks of vads are weighed on it
     * ({@link Ruilmarkt#klikStapel}); anywhere a click weighs what you hold in your two hands (the stack with more items in it
     * is the heavier), which is what the reward does at home.
     */
    public static class Weegschaal extends HorizontalDirectionalBlock {
        public static final MapCodec<Weegschaal> CODEC = simpleCodec(Weegschaal::new);
        public static final EnumProperty<Stand> STAND = EnumProperty.create("stand", Stand.class);
        /** Ticks until the scales hang level again. */
        public static final int TERUG = 60;
        private static final VoxelShape VORM = Shapes.or(Block.box(4, 0, 4, 12, 2, 12), Block.box(7, 2, 7, 9, 15, 9), Block.box(0, 4, 5, 16, 15, 11));

        public Weegschaal(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STAND, Stand.MIDDEN));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, STAND);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        /** Lets the scales at {@code pos} tip (or hang level) for a moment; they swing back by themselves. */
        public static void tik(ServerLevel level, BlockPos pos, Stand stand) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof Weegschaal schaal)) {
                return;
            }
            if (state.getValue(STAND) != stand) {
                level.setBlock(pos, state.setValue(STAND, stand), Block.UPDATE_ALL);
            }
            level.scheduleTick(pos, schaal, TERUG);
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.9f, stand == Stand.MIDDEN ? 1.4f : 0.8f);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (state.getValue(STAND) != Stand.MIDDEN) {
                level.setBlock(pos, state.setValue(STAND, Stand.MIDDEN), Block.UPDATE_ALL);
            }
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            return useWithoutItem(state, level, pos, player, hit);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
                if (Ruilmarkt.klikSchaal(sp, pos)) {
                    return InteractionResult.SUCCESS;
                }
                // at home: the main hand lies on the left pan, the other hand on the right one
                int links = sp.getMainHandItem().getCount(), rechts = sp.getOffhandItem().getCount();
                Stand stand = links > rechts ? Stand.LINKS : rechts > links ? Stand.RECHTS : Stand.MIDDEN;
                tik(server, pos, stand);
                String key = links == 0 && rechts == 0 ? "leeg" : stand.getSerializedName();
                sp.sendOverlayMessage(Component.translatable("gui.guhs.campingmarkt.weegschaal." + key, links, rechts).withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** A stack of vads bars on the counter of the Waag, NUMMER 1..5. Which one is the fake is every player's own puzzle. */
    public static class Vadsstapel extends HorizontalDirectionalBlock {
        public static final MapCodec<Vadsstapel> CODEC = simpleCodec(Vadsstapel::new);
        public static final IntegerProperty NUMMER = IntegerProperty.create("nummer", 1, Ruilmarkt.STAPELS);
        private static final VoxelShape VORM = Shapes.or(Block.box(1, 0, 3, 15, 5, 13), Block.box(3, 5, 4, 13, 9, 12));

        public Vadsstapel(Properties properties) {
            super(properties);
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
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                Ruilmarkt.klikStapel(sp, pos, state.getValue(NUMMER), stack);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                Ruilmarkt.klikStapel(sp, pos, state.getValue(NUMMER), ItemStack.EMPTY);
            }
            return InteractionResult.SUCCESS;
        }
    }
}
