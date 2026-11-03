package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * De Leverkist ({@code guhs:leverkist}, ronde 3 + 7): a little treasure chest with a mailbox flag, for your real base. What
 * you send home from the village ("Naar huis sturen") comes in by itself ({@link Leverkisten}). 27 slots, a normal chest
 * screen; hoppers and pipes take out (and may put in). The flag ({@link #POST}) is up while there is something inside.
 * The one who placed it owns it (a chest placed by a command is claimed by the first right-click). Several chests: they
 * fill in the order they were placed.
 */
public final class Leverkist {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POST = BooleanProperty.create("post");
    public static final int SLOTS = 27;

    private Leverkist() {
    }

    public static class Block extends BaseEntityBlock {
        public static final MapCodec<Block> CODEC = simpleCodec(Block::new);
        private static final VoxelShape VORM = net.minecraft.world.level.block.Block.box(1, 0, 1, 15, 13, 15);

        public Block(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POST, false));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
            b.add(FACING, POST);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BlockEntityImpl(pos, state);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (placer instanceof Player p && level.getBlockEntity(pos) instanceof BlockEntityImpl kist) {
                kist.zetEigenaar(p.getUUID());
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof BlockEntityImpl kist) {
                if (kist.eigenaar == null) {
                    kist.zetEigenaar(player.getUUID());
                    player.sendSystemMessage(Component.translatable("erfenis.koppeling.leverkist.van_jou").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.6f, 1.3f);
                player.openMenu(kist);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
            Containers.updateNeighboursAfterDestroy(state, level, pos);
        }

        @Override
        protected boolean hasAnalogOutputSignal(BlockState state) {
            return true;
        }

        @Override
        protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
            return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
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

    public static class BlockEntityImpl extends BaseContainerBlockEntity {
        private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        @Nullable
        UUID eigenaar;

        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(KoppelingFeature.LEVERKIST_BE.get(), pos, state);
        }

        @Nullable
        public UUID eigenaar() {
            return eigenaar;
        }

        /** (also tests) gives the chest an owner and puts it on the end of that owner's list. */
        public void zetEigenaar(UUID wie) {
            eigenaar = wie;
            setChanged();
            if (level instanceof ServerLevel server) {
                KoppelingData.get(server.getServer()).voegKistToe(wie, GlobalPos.of(server.dimension(), worldPosition));
            }
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel server && eigenaar != null) {
                KoppelingData.get(server.getServer()).voegKistToe(eigenaar, GlobalPos.of(server.dimension(), worldPosition));
            }
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);   // (the contents fall out)
            if (level instanceof ServerLevel server) {
                KoppelingData.get(server.getServer()).haalKistWeg(GlobalPos.of(server.dimension(), pos));
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (level != null && !level.isClientSide()) {
                BlockState s = getBlockState();
                boolean vol = !isEmpty();
                if (s.hasProperty(POST) && s.getValue(POST) != vol) {
                    level.setBlock(worldPosition, s.setValue(POST, vol), 3);
                    if (vol) {
                        level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.5f, 1.6f);
                    }
                }
            }
        }

        @Override
        public int getContainerSize() {
            return SLOTS;
        }

        @Override
        protected NonNullList<ItemStack> getItems() {
            return items;
        }

        @Override
        protected void setItems(NonNullList<ItemStack> items) {
            this.items = items;
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.guhs.leverkist");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
            return ChestMenu.threeRows(id, inventory, this);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            ContainerHelper.saveAllItems(output, items);
            output.storeNullable("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, items);
            eigenaar = input.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        }
    }
}
