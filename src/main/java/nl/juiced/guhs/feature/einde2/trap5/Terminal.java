package nl.juiced.guhs.feature.einde2.trap5;

import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.bank.BankAdressen;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.BankSleutelItem;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.menu.BankGuhMenu;

/**
 * The Guhterminal ({@code guhs:guhterminal}): ONLY a block (no tablet, DESIGN 6). On vadskracht, linked with the Banksleutel
 * to a Bank Guh (click the bank with the key, then the terminal), it opens THAT bank's own screen from anywhere, also from
 * another dimension: searching, sorting, taking and putting in, with the bank's own rules (the cap of
 * {@code BankStorage.CAP} per kind until the bank has its upgrade). While it is open the terminal must keep its vadskracht
 * and the player must stay at the terminal ({@link Menu#stillValid}); the bank's chunk is kept awake meanwhile.
 * <p>
 * The bank screen opened from a terminal gets one extra button (client): "Bestellen", the list of everything the
 * Bestelguhs of the same bank can make ({@link Bestelbalie}).
 */
public final class Terminal {
    private Terminal() {
    }

    public static class Block extends MachineBlock {
        public static final MapCodec<Block> CODEC = simpleCodec(Block::new);

        public Block(Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BlockEntityImpl(pos, state);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            if (!stack.is(BankFeature.BANK_SLEUTEL.get())) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof BlockEntityImpl t) {
                t.sleutel(speler, stack);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof BlockEntityImpl t) {
                t.open(speler);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class BlockEntityImpl extends MachineBlockEntity {
        @Nullable
        private UUID bank;
        boolean oefen;

        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.TERMINAL_BE.get(), pos, state, T5Getallen.TERMINAL, 1);
        }

        @Nullable
        public UUID bank() {
            return bank;
        }

        public void koppel(@Nullable UUID id) {
            bank = id;
            sync();
        }

        @Override
        protected boolean kanWerken() {
            return bank != null;
        }

        @Override
        protected boolean isVol() {
            return false;
        }

        @Override
        protected void werk() {
        }

        @Override
        protected boolean isInvoer(int vak) {
            return false;
        }

        @Override
        protected boolean isUitvoer(int vak) {
            return false;
        }

        /** Where the linked bank stands (null: not linked, or the bank stands nowhere). */
        @Nullable
        GlobalPos adres() {
            return bank != null && level instanceof ServerLevel server ? BankAdressen.van(server.getServer()).plek(bank) : null;
        }

        void sleutel(ServerPlayer player, ItemStack sleutel) {
            UUID id = BankSleutelItem.bank(sleutel);
            if (id == null) {
                player.sendOverlayMessage(Component.translatable("block.guhs.guhterminal.sleutel_leeg"));
                return;
            }
            koppel(id);
            GlobalPos plek = adres();
            player.sendOverlayMessage(plek == null ? Component.translatable("block.guhs.guhterminal.gekoppeld.weg")
                    : Component.translatable("block.guhs.guhterminal.gekoppeld", plek.pos().getX(), plek.pos().getY(), plek.pos().getZ(),
                    GuhVolger.dimensie(plek.dimension())));
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.3f);
            }
        }

        /** A player clicks the terminal: the bank's own screen, or what is in the way. */
        void open(ServerPlayer player) {
            if (oefen) {
                player.sendSystemMessage(Component.translatable("block.guhs.guhterminal.oefen").withStyle(ChatFormatting.AQUA));
                return;
            }
            if (bank == null) {
                player.sendOverlayMessage(Component.translatable("block.guhs.guhterminal.los"));
                return;
            }
            if (!heeftKracht()) {
                player.sendOverlayMessage(Component.translatable("block.guhs.guhterminal.slaapt"));
                return;
            }
            BankGuhBlockEntity doel = BankAdressen.zoek(player.level().getServer(), bank, true);
            if (doel == null) {
                player.sendOverlayMessage(Component.translatable("block.guhs.guhterminal.bank_weg"));
                return;
            }
            T5Payloads.stuur(player, new T5Payloads.TerminalOpen(worldPosition));
            BlockPos terminal = worldPosition;
            ResourceKey<Level> dim = player.level().dimension();
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("block.guhs.guhterminal");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                    return new Menu(id, inventory, doel, dim, terminal);
                }
            }, doel.getBlockPos());
            level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.3f, 1.8f);
            if (eigenaar() == null || eigenaar().equals(player.getUUID())) {
                E2Trap5.meld(player, E2Trap5.P_TERMINAL);
            }
        }

        @Override
        public void vadsRegels(Consumer<Component> regels) {
            GlobalPos plek = adres();
            regels.accept(bank == null ? Component.translatable("block.guhs.guhterminal.los").withStyle(ChatFormatting.GOLD)
                    : plek == null ? Component.translatable("block.guhs.guhterminal.gekoppeld.weg").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("block.guhs.guhterminal.gekoppeld", plek.pos().getX(), plek.pos().getY(), plek.pos().getZ(),
                    GuhVolger.dimensie(plek.dimension())).withStyle(ChatFormatting.AQUA));
        }

        @Override
        protected void opslaan(ValueOutput uit) {
            uit.storeNullable("Bank", UUIDUtil.CODEC, bank);
            uit.putBoolean("Oefen", oefen);
        }

        @Override
        protected void laden(ValueInput in) {
            bank = in.read("Bank", UUIDUtil.CODEC).orElse(null);
            oefen = in.getBooleanOr("Oefen", false);
        }
    }

    /**
     * The bank's own menu, opened from a terminal: the same slots and the same actions, but it stays open as long as the
     * player stands at the TERMINAL (and the terminal has vadskracht), not at the bank, and the bank's chunk is kept awake.
     */
    public static class Menu extends BankGuhMenu {
        private final BankGuhBlockEntity bank;
        private final ResourceKey<Level> dim;
        private final BlockPos terminal;
        private int wakker;

        public Menu(int containerId, Inventory inventory, BankGuhBlockEntity bank, ResourceKey<Level> dim, BlockPos terminal) {
            super(containerId, inventory, bank);
            this.bank = bank;
            this.dim = dim;
            this.terminal = terminal;
        }

        @Override
        public void broadcastChanges() {
            super.broadcastChanges();
            if (++wakker >= 100) {
                wakker = 0;
                BankAdressen.houdWakker(bank);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            if (bank.isRemoved() || player.level().dimension() != dim
                    || player.distanceToSqr(terminal.getX() + 0.5, terminal.getY() + 0.5, terminal.getZ() + 0.5) > 64) {
                return false;
            }
            return player.level().getBlockEntity(terminal) instanceof BlockEntityImpl t && t.heeftKracht();
        }
    }
}
