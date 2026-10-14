package nl.juiced.guhs.feature.park;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;

/**
 * 1.4.2 (Het Knabbelpark): the park's own blocks (models and textures: tools/features/park_modellen.py).
 * <ul>
 *   <li>{@link Lantaarn}: the guh lantern on top of a park lantern post (a cherry fence post): a round lamp with two ears;</li>
 *   <li>{@link Prullenbak}: the litter bin with a guh face; throw something in (right-click with it in your hand) and it
 *       eats one, NJEG (it spits out anything too nice to throw away: tools, named or enchanted things);</li>
 *   <li>{@link Plaquette}: the bronze plaque in front of a statue: right-click to read it on screen ({@link Park#lees});</li>
 *   <li>{@link Beeld}: a statue of the park on its plinth: the real guh model in stone or bronze, drawn on top of the block
 *       by the client ({@code client.BeeldRenderer}); which statue is the block state {@link #BEELD};</li>
 *   <li>{@link EigenBeeld}: the reward: a small statue of YOUR guh (right-click one of your own guhs with it and it takes
 *       its look; the block keeps it, also when broken and placed again).</li>
 * </ul>
 * The park bench is a plain {@code GuhFurnitureBlock} (you can sit on it).
 */
public final class ParkBlokken {
    private ParkBlokken() {
    }

    /** Which statue: its texture, scale and plinth (BeeldRenderer). */
    public enum Soort implements StringRepresentable {
        JOOSTVADSJE, TRAPGUH, LIEKGUH, LUNDE, OPA_KAASGUH;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Soort> BEELD = EnumProperty.create("beeld", Soort.class);
    public static final IntegerProperty NR = IntegerProperty.create("nr", 1, Park.PLAQUETTES);

    // =====================================================================================================================
    /** The guh lantern (light 15) that sits on a lantern post. */
    public static class Lantaarn extends Block {
        public static final MapCodec<Lantaarn> CODEC = simpleCodec(Lantaarn::new);
        private static final VoxelShape VORM = Shapes.or(Block.box(4, 0, 4, 12, 9, 12), Block.box(3, 9, 6, 13, 12, 10));

        public Lantaarn(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }
    }

    // =====================================================================================================================
    /** The guh litter bin: eats one of what you hold, NJEG. */
    public static class Prullenbak extends DecoBlock {
        public static final int ZINNEN = 4;

        public Prullenbak(Properties properties) {
            super(properties, Shapes.or(Block.box(3, 0, 3, 13, 13, 13), Block.box(2, 13, 2, 14, 15, 14)));
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            if (stack.isEmpty()) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.SUCCESS;
            }
            if (teMooi(stack)) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.park.prullenbak.te_mooi").withStyle(ChatFormatting.LIGHT_PURPLE));
                return InteractionResult.SUCCESS;
            }
            stack.consume(1, player);
            server.playSound(null, pos, ParkFeature.NJEG.get(), SoundSource.BLOCKS, 0.9f, 0.9f + server.getRandom().nextFloat() * 0.3f);
            server.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0);
            sp.sendOverlayMessage(Component.translatable("gui.guhs.park.prullenbak." + server.getRandom().nextInt(ZINNEN)).withStyle(ChatFormatting.LIGHT_PURPLE));
            Park.weggegooid(sp);
            return InteractionResult.SUCCESS;
        }

        /** Tools, armour, named, enchanted or loaned things: the bin spits them out again. */
        public static boolean teMooi(ItemStack stack) {
            return stack.isDamageableItem() || stack.has(DataComponents.CUSTOM_NAME) || stack.isEnchanted()
                    || stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, nl.juiced.guhs.Guhs.id("loaned")))
                    || stack.getMaxStackSize() == 1;
        }
    }

    // =====================================================================================================================
    /** The plaque in front of a statue: right-click = read it. */
    public static class Plaquette extends HorizontalDirectionalBlock {
        public static final MapCodec<Plaquette> CODEC = simpleCodec(Plaquette::new);
        private static final Map<Direction, VoxelShape> VORMEN = Shapes.rotateHorizontal(Shapes.or(Block.box(7, 0, 7, 9, 9, 9), Block.box(2, 8, 5, 14, 15, 9)));

        public Plaquette(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(NR, 1));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, NR);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORMEN.get(state.getValue(FACING));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                Park.lees(sp, state.getValue(NR));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;   // (reading works with anything in your hand)
        }
    }

    // =====================================================================================================================
    /** A statue of the park: the plinth is the block, the guh on it is drawn by the client. */
    public static class Beeld extends BaseEntityBlock {
        public static final MapCodec<Beeld> CODEC = simpleCodec(Beeld::new);
        private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 24, 16);

        public Beeld(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH).setValue(BEELD, Soort.JOOSTVADSJE));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HorizontalDirectionalBlock.FACING, BEELD);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BeeldBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.park.beeld." + state.getValue(BEELD).getSerializedName())
                        .withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =====================================================================================================================
    /** The reward: a little bronze statue of your own guh. */
    public static class EigenBeeld extends BaseEntityBlock {
        public static final MapCodec<EigenBeeld> CODEC = simpleCodec(EigenBeeld::new);
        private static final VoxelShape VORM = Block.box(3, 0, 3, 13, 15, 13);

        public EigenBeeld(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HorizontalDirectionalBlock.FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BeeldBlockEntity(pos, state);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (level.getBlockEntity(pos) instanceof BeeldBlockEntity be) {
                be.vanItem(stack);
            }
        }

        /** It drops itself with the look it has (the variant and the guh's name go along on the item). */
        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            ItemStack stack = new ItemStack(ParkFeature.EIGEN_BEELD_ITEM.get());
            if (be instanceof BeeldBlockEntity beeld) {
                beeld.naarItem(stack);
            }
            return List.of(stack);
        }

        @Override
        protected ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            ItemStack stack = new ItemStack(ParkFeature.EIGEN_BEELD_ITEM.get());
            if (level.getBlockEntity(pos) instanceof BeeldBlockEntity beeld) {
                beeld.naarItem(stack);
            }
            return stack;
        }
    }
}
