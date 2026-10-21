package nl.juiced.guhs.feature.safari.verblijf;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.safari.SafariDier;

/** Op Safari! step 2 (S5): the verblijfsbord, the Ooievaarsbus and the toys. */
public final class VerblijfBlokken {

    // =====================================================================================================================
    /** The verblijfsbord: a zoo sign on a post (drawn by GeckoLib); right-click opens its menu (others may look). */
    public static class Bord extends BaseEntityBlock {
        public static final MapCodec<Bord> CODEC = simpleCodec(Bord::new);
        private static final VoxelShape NZ = Block.box(1, 0, 6, 15, 16, 10), OW = Block.box(6, 0, 1, 10, 16, 15);

        public Bord(Properties properties) {
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
            Direction d = state.getValue(HorizontalDirectionalBlock.FACING);
            return d.getAxis() == Direction.Axis.Z ? NZ : OW;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new VerblijfsbordBlockEntity(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide() ? null : createTickerHelper(type, VerblijfFeature.BORD_BE.get(), VerblijfsbordBlockEntity::tick);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof VerblijfsbordBlockEntity be) {
                String soort = VerblijfsbordItem.soort(stack);
                be.plaats(sl, placer instanceof ServerPlayer sp ? sp : null, soort);
                if (placer instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.translatable(be.terrarium() ? "gui.guhs.verblijf.terrarium.welkom" : "gui.guhs.verblijf.welkom",
                            VerblijfsbordItem.naam(soort)));
                }
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof VerblijfsbordBlockEntity be) {
                VerblijfPayloads.open(sp, be);
            }
            return InteractionResult.SUCCESS;
        }

        /** It drops itself with its species (the family goes back to the park: the block entity sees to that). */
        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            return List.of(VerblijfsbordItem.bord(be instanceof VerblijfsbordBlockEntity b ? b.soort() : ""));
        }

        @Override
        protected ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return VerblijfsbordItem.bord(level.getBlockEntity(pos) instanceof VerblijfsbordBlockEntity b ? b.soortClient() : "");
        }
    }

    // =====================================================================================================================
    /**
     * Round 4 (L4): the terrarium, what a terrariumbord (a small Kriebelhuis creature's board) becomes at home: a glass tank
     * with a wooden frame (a plain block model), the creatures staying put inside ({@link SafariDier#zetVast}). It works
     * like the board (the same block entity: right-click opens the menu; food in hand feeds a creature inside).
     */
    public static class Terrarium extends Bord {
        public static final MapCodec<Terrarium> CODEC = simpleCodec(Terrarium::new);
        private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 16, 16);

        public Terrarium(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
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
            return new VerblijfsbordBlockEntity(VerblijfFeature.TERRARIUM_BE.get(), pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide() ? null : createTickerHelper(type, VerblijfFeature.TERRARIUM_BE.get(), VerblijfsbordBlockEntity::tick);
        }

        /** Food (or anything) in hand goes to a creature inside (the glass is in the way of a click on it). */
        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
            if (stack.isEmpty() || stack.getItem() instanceof VerblijfsbordItem) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof VerblijfsbordBlockEntity be) {
                for (ThuisDier d : be.dieren(sl)) {
                    InteractionResult r = d.interact(player, hand, d.position());
                    if (r.consumesAction()) {
                        return r;
                    }
                }
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
    }

    // =====================================================================================================================
    /** The Ooievaarsbus: friends' storks put their parcels in it; its owner opens it like a dispenser (3x3). */
    public static class Bus extends BaseEntityBlock {
        public static final MapCodec<Bus> CODEC = simpleCodec(Bus::new);
        private static final VoxelShape VORM = Block.box(3, 0, 4, 13, 16, 12);

        public Bus(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
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
            return new OoievaarsbusBlockEntity(pos, state);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (level instanceof ServerLevel sl && placer instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof OoievaarsbusBlockEntity be) {
                be.zetEigenaar(sp.getUUID(), sp.getGameProfile().name());
                Ooievaar.Bussen.get(sl.getServer()).zet(sp.getUUID(), sp.getGameProfile().name(), level.dimension(), pos);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof OoievaarsbusBlockEntity be) {
                boolean mag = be.eigenaar() == null || be.eigenaar().equals(sp.getUUID())
                        || sp.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
                if (!mag) {
                    sp.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.bus.niet_van_jou", be.eigenaarNaam()));
                    return InteractionResult.SUCCESS;
                }
                sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new DispenserMenu(id, inv, be.inhoud()),
                        Component.translatable("gui.guhs.verblijf.bus.titel", be.eigenaarNaam())));
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =====================================================================================================================
    /** A toy for the animals at home (speelbal, glijbaantje, krabboom, schommelband): a decorative block they play near. */
    public static class Speeltje extends Block {
        private final VoxelShape vorm;

        public Speeltje(Properties properties, VoxelShape vorm) {
            super(properties);
            this.vorm = vorm;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return vorm;
        }
    }

    private VerblijfBlokken() {
    }
}
