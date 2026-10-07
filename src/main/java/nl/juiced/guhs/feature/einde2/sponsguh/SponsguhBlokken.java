package nl.juiced.guhs.feature.einde2.sponsguh;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.einde2.Einde2Haken;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2, slice sponsguh_wezens: the blocks of the Sponsguh story that DO something: {@link KarenBlock Karen} (Plankmika's
 * computer wife), the {@link GrillBlock grill} and the {@link BesteltafelBlock order table} of De Krokante Knabbel and the
 * {@link BellenblaasBlock bubble blower} for guhs. (The knabbelkoorvrucht-house set and the spooklantaarn are plain vanilla
 * block classes, registered in {@link E2SponsguhFeature}.) Models and textures: tools/features/einde2_sponsguh_wezens_blokken.py.
 */
public final class SponsguhBlokken {
    private SponsguhBlokken() {
    }

    /** A block that looks one way (the four below): placed facing the player, turned and mirrored with a build. */
    public abstract static class Gericht extends Block {
        public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

        protected Gericht(Properties properties) {
            super(properties);
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
    }

    // =====================================================================================================================
    /**
     * Karen: a monitor on a wheeled stand with a face of lines. Block state {@code gezicht} 0-3 = neutraal, blij, boos, zucht
     * ({@link Karen#zetGezicht}); a click makes her say one dry line that fits her face.
     */
    public static class KarenBlock extends Gericht {
        public static final MapCodec<KarenBlock> CODEC = simpleCodec(KarenBlock::new);
        public static final IntegerProperty GEZICHT = IntegerProperty.create("gezicht", 0, 3);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 2, 13), Block.box(7, 2, 7, 9, 6, 9), Block.box(2, 6, 2, 14, 16, 14));

        public KarenBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GEZICHT, Karen.NEUTRAAL));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, GEZICHT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            level.playSound(null, pos, E2SponsguhFeature.KAREN_PIEP.get(), SoundSource.BLOCKS, 0.7f, 0.9f + 0.1f * state.getValue(GEZICHT));
            player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_sponsguh_wezens.karen." + state.getValue(GEZICHT)).withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS;
        }
    }

    // =====================================================================================================================
    /**
     * The grill of De Krokante Knabbel. With a kaasknabbel and a brood in your pockets a click bakes ONE Knabbelburger in about
     * five seconds (it pops out on top), but only for a player who knows the secret recipe
     * ({@link Einde2Haken#KNABBELBURGER_RECEPT}); for anybody else it only sizzles: "het recept is geheim, njeg".
     */
    public static class GrillBlock extends Gericht {
        public static final MapCodec<GrillBlock> CODEC = simpleCodec(GrillBlock::new);
        public static final BooleanProperty BAKT = BooleanProperty.create("bakt");
        public static final int BAKTIJD = 100;
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

        public GrillBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BAKT, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, BAKT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                bak(state, (ServerLevel) level, pos, sp);
            }
            return InteractionResult.SUCCESS;
        }

        /** One try of this player at the grill; true: a burger is baking now. */
        public static boolean bak(BlockState state, ServerLevel level, BlockPos pos, ServerPlayer player) {
            if (state.getValue(BAKT)) {
                zeg(player, "bezig");
                return false;
            }
            Item knabbel = ModItems.KAAS_KNABBELS.get();
            Inventory zakken = player.getInventory();
            if (zakken.countItem(knabbel) < 1 || zakken.countItem(Items.BREAD) < 1) {
                zeg(player, "ingredienten");
                return false;
            }
            level.playSound(null, pos, E2SponsguhFeature.GRILL_SIS.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 8, 0.25, 0.05, 0.25, 0.01);
            if (!Einde2Haken.KNABBELBURGER_RECEPT.test(player)) {
                zeg(player, "geheim");                                       // hook fase B: the story teaches the recipe
                return false;
            }
            if (!player.getAbilities().instabuild) {
                neem(zakken, knabbel);
                neem(zakken, Items.BREAD);
            }
            level.setBlock(pos, state.setValue(BAKT, true), 3);
            level.scheduleTick(pos, state.getBlock(), BAKTIJD);
            return true;
        }

        private static void neem(Inventory zakken, Item item) {
            for (int i = 0; i < zakken.getContainerSize(); i++) {
                ItemStack s = zakken.getItem(i);
                if (s.is(item)) {
                    s.shrink(1);
                    return;
                }
            }
        }

        private static void zeg(ServerPlayer player, String wat) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_sponsguh_wezens.grill." + wat).withStyle(ChatFormatting.GOLD));
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!state.getValue(BAKT)) {
                return;
            }
            level.setBlock(pos, state.setValue(BAKT, false), 3);
            ItemEntity burger = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, new ItemStack(E2SponsguhFeature.KNABBELBURGER.get()));
            burger.setDeltaMovement(0, 0.2, 0);
            burger.setDefaultPickUpDelay();
            level.addFreshEntity(burger);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 0.8f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 5, 0.25, 0.1, 0.25, 0);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(BAKT)) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.85, pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                        0, 0.03, 0);
                if (random.nextInt(8) == 0) {
                    level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.5f, 1.4f, false);
                }
            }
        }
    }

    // =====================================================================================================================
    /** The order table of De Krokante Knabbel: a little table with a bell; a click rings it ({@link Besteltafel#BIJ_BEL}). */
    public static class BesteltafelBlock extends Gericht {
        public static final MapCodec<BesteltafelBlock> CODEC = simpleCodec(BesteltafelBlock::new);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 12, 0, 16, 14, 16), Block.box(6, 0, 6, 10, 12, 10), Block.box(3, 0, 3, 13, 1, 13),
                Block.box(6, 14, 6, 10, 16, 10));

        public BesteltafelBlock(Properties properties) {
            super(properties);
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

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                Besteltafel.bel((ServerLevel) level, pos, sp);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =====================================================================================================================
    /**
     * The bubble blower: a toy for guhs. A click blows a cloud of bubbles; guhs near it come and chase the bubbles when a
     * player is near, as they play with the other toys ({@link BellenSpel}, registered as a toy kind of the Guhhuisje).
     */
    public static class BellenblaasBlock extends Gericht {
        public static final MapCodec<BellenblaasBlock> CODEC = simpleCodec(BellenblaasBlock::new);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 7, 12), Block.box(6, 7, 6, 10, 14, 10));

        public BellenblaasBlock(Properties properties) {
            super(properties);
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

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server) {
                blaas(server, pos, 14);
            }
            return InteractionResult.SUCCESS;
        }

        /** A puff of bubbles out of the blower. */
        public static void blaas(ServerLevel level, BlockPos pos, int aantal) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, aantal, 0.7, 0.5, 0.7, 0.02);
            level.sendParticles(ParticleTypes.BUBBLE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, aantal / 2, 0.4, 0.3, 0.4, 0.05);
            level.playSound(null, pos, E2SponsguhFeature.BELLEN.get(), SoundSource.BLOCKS, 0.6f, 1.1f + level.getRandom().nextFloat() * 0.4f);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(12) == 0) {
                level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 0, 0.04, 0);
            }
        }
    }
}
