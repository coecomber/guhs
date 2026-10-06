package nl.juiced.guhs.feature.bank;

import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.storage.BankStorage;

/**
 * The Hapluikje's block entity: a guh machine without an inventory of its own. Its item handler ({@link #handler}) is a
 * mouth: whatever is put in goes straight on into the Bank Guh it is linked to ({@link #bank}, set with a Banksleutel),
 * inside the giver's own transaction, however far away that bank stands and in whatever dimension
 * ({@link BankAdressen#zoek} loads the bank's chunk when needed and keeps it loaded for a little while).
 * <p>
 * It takes nothing, and so the giver keeps the items (a hopper stays full, a Knabbelbuis holds on to it, a hand keeps the
 * stack), when: it has no vadskracht ({@link VadsGetallen#HAPLUIKJE} VK), it is not linked, its bank stands nowhere (it
 * is in somebody's pocket), the bank is full of that item (the cap of {@link BankStorage#CAP}; what still fits goes in),
 * or the item is a loaned one. Nothing ever comes out of it.
 * <p>
 * The face: asleep without vadskracht, surprised for a few seconds after it had to refuse something, happy otherwise.
 */
public class HapluikjeBlockEntity extends MachineBlockEntity {
    /** How it is doing; {@link #stand}. */
    public enum Stand {
        /** No vadskracht. */
        SLAAPT,
        /** Not linked to a bank yet. */
        LOS,
        /** Its bank stands nowhere right now. */
        WEG,
        /** Ready: what goes in lands in the bank. */
        KLAAR
    }

    /** The face stays surprised this long after a refusal (ticks). */
    public static final int VERRAST = 60;

    @Nullable
    private UUID bank;
    private long verrastTot;
    private final Bek bek = new Bek();

    public HapluikjeBlockEntity(BlockPos pos, BlockState state) {
        super(BankFeature.HAPLUIKJE_BE.get(), pos, state, VadsGetallen.HAPLUIKJE, 0);
    }

    // =====================================================================================================================
    // the link
    // =====================================================================================================================

    /** The id of the bank this luikje feeds, or null: not linked. */
    @Nullable
    public UUID bank() {
        return bank;
    }

    /** Links this luikje to the bank with this id (one bank per luikje: the old link is gone). */
    public void koppel(@Nullable UUID bank) {
        this.bank = bank;
        sync();
    }

    public Stand stand() {
        if (!heeftKracht()) {
            return Stand.SLAAPT;
        }
        if (bank == null) {
            return Stand.LOS;
        }
        return adres() == null ? Stand.WEG : Stand.KLAAR;
    }

    /** Where the linked bank stands according to the address book (not checked, nothing is loaded), or null. */
    @Nullable
    public GlobalPos adres() {
        return bank != null && level instanceof ServerLevel server ? BankAdressen.van(server.getServer()).plek(bank) : null;
    }

    /** The linked Bank Guh itself, its chunk loaded if need be; null when not linked or when it stands nowhere. */
    @Nullable
    public BankGuhBlockEntity doel() {
        return bank != null && level instanceof ServerLevel server ? BankAdressen.zoek(server.getServer(), bank, true) : null;
    }

    // =====================================================================================================================
    // the mouth
    // =====================================================================================================================

    /** Passes up to this many on to the bank; returns how many it took (the giver keeps the rest). */
    private int hap(ItemResource wat, int aantal, TransactionContext transaction) {
        if (aantal <= 0 || !heeftKracht() || bank == null || wat.test(Features::isLoaned)) {
            return 0;
        }
        BankGuhBlockEntity doel = doel();
        if (doel == null) {
            verrast();
            return 0;
        }
        BankAdressen.houdWakker(doel);
        int erin = doel.handler().insert(wat, aantal, transaction);
        if (erin < aantal) {
            verrast();   // (the bank is full of it)
        }
        return erin;
    }

    private void verrast() {
        if (level != null) {
            verrastTot = level.getGameTime() + VERRAST;
        }
    }

    /** What pipes, hoppers, chore guhs and the Bezorgguhtje get: one always-empty slot that swallows. Never gives. */
    private final class Bek implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return !resource.isEmpty() && !resource.test(Features::isLoaned);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return index == 0 ? hap(resource, amount, transaction) : 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }

    @Override
    public ResourceHandler<ItemResource> handler(@Nullable Direction kant) {
        return bek;
    }

    // =====================================================================================================================
    // by hand
    // =====================================================================================================================

    /** A player holds a Banksleutel against the luikje. */
    public void sleutel(ServerPlayer player, ItemStack sleutel) {
        UUID id = BankSleutelItem.bank(sleutel);
        if (id == null) {
            player.sendOverlayMessage(Component.translatable("block.guhs.hapluikje.sleutel_leeg"));
            return;
        }
        koppel(id);
        GlobalPos plek = adres();
        player.sendOverlayMessage(plek == null ? Component.translatable("block.guhs.hapluikje.gekoppeld.weg")
                : Component.translatable("block.guhs.hapluikje.gekoppeld", plek.pos().getX(), plek.pos().getY(), plek.pos().getZ(),
                GuhVolger.dimensie(plek.dimension())));
        if (level instanceof ServerLevel server) {
            server.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.2f);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                    8, 0.3, 0.2, 0.3, 0.0);
        }
        BankFeature.gekoppeld(player);
    }

    /** A player feeds the luikje what is in this hand: as much as the bank takes goes in, the rest stays in the hand. */
    public void voer(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !zegStand(player, false)) {
            return;
        }
        if (Features.isLoaned(stack)) {
            player.sendOverlayMessage(Component.translatable("block.guhs.hapluikje.geleend"));
            return;
        }
        Component naam = stack.getHoverName();
        ItemStack rest = Kisten.stop(bek, stack);
        int erin = stack.getCount() - rest.getCount();
        if (erin > 0) {
            ItemStack gehapt = stack.copyWithCount(1);
            player.setItemInHand(hand, rest);
            if (level instanceof ServerLevel server) {
                server.playSound(null, worldPosition, SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 0.7f, 1.3f);
                Direction voor = getBlockState().getValue(HapluikjeBlock.FACING);
                server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, gehapt.getItem()), worldPosition.getX() + 0.5 + voor.getStepX() * 0.55,
                        worldPosition.getY() + 0.35, worldPosition.getZ() + 0.5 + voor.getStepZ() * 0.55, 6, 0.1, 0.1, 0.1, 0.03);
            }
            BankFeature.gehapt(player);
        }
        if (rest.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("block.guhs.hapluikje.hap", erin, naam));
        } else if (stand() != Stand.KLAAR) {
            zegStand(player, false);   // (the bank turned out to be gone after all)
        } else {
            BankGuhBlockEntity doel = doel();
            player.sendOverlayMessage(Component.translatable("block.guhs.hapluikje.vol", naam, doel == null ? 0 : doel.getStorage().count(stack),
                    BankStorage.CAP));
        }
    }

    /** A click with an empty hand: says how it is doing. */
    public void vertel(ServerPlayer player) {
        zegStand(player, true);
    }

    /** Says what is wrong (or, with ookGoed, that all is well); returns whether the luikje is ready. */
    private boolean zegStand(ServerPlayer player, boolean ookGoed) {
        switch (stand()) {
            case SLAAPT -> player.sendOverlayMessage(Component.translatable("block.guhs.hapluikje.slaapt", VadsGetallen.HAPLUIKJE));
            case LOS -> player.sendOverlayMessage(Component.translatable("block.guhs.hapluikje.los"));
            case WEG -> player.sendOverlayMessage(Component.translatable("block.guhs.hapluikje.weg"));
            case KLAAR -> {
                GlobalPos plek = adres();
                if (ookGoed && plek != null) {
                    player.sendOverlayMessage(Component.translatable("block.guhs.hapluikje.klaar", plek.pos().getX(), plek.pos().getY(),
                            plek.pos().getZ(), GuhVolger.dimensie(plek.dimension())));
                }
                return true;
            }
        }
        return false;
    }

    // =====================================================================================================================
    // the machine
    // =====================================================================================================================

    /** The hover readout (under the vadskracht lines): which bank it feeds, or what is missing. */
    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (bank == null) {
            regels.accept(Component.translatable("gui.guhs.bank.luikje.los"));
            return;
        }
        GlobalPos plek = adres();
        regels.accept(plek == null ? Component.translatable("gui.guhs.bank.luikje.weg")
                : Component.translatable("gui.guhs.bank.luikje.klaar", plek.pos().getX(), plek.pos().getY(), plek.pos().getZ(),
                GuhVolger.dimensie(plek.dimension())));
    }

    /** It has no work of its own: it only passes on. */
    @Override
    protected boolean kanWerken() {
        return false;
    }

    @Override
    protected void werk() {
    }

    /** "Full" = it just had to refuse something (its bank is full of it, or gone): the surprised face. */
    @Override
    protected boolean isVol() {
        return level != null && level.getGameTime() < verrastTot;
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.storeNullable("Bank", UUIDUtil.CODEC, bank);
    }

    @Override
    protected void laden(ValueInput in) {
        bank = in.read("Bank", UUIDUtil.CODEC).orElse(null);
    }
}
