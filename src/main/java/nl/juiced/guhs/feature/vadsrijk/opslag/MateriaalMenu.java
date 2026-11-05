package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.storage.BankContents;

/**
 * T-OPSLAG: the Materiaaltas screen's menu: only the player's inventory as real slots; the bag itself is a virtual grid that the
 * client draws from {@link OpslagPayloads.TasInhoud} and acts on through {@link OpslagPayloads.TasActie}. Every action is done here,
 * on the server, against {@link OpslagData#materialen}: take (to the cursor or, with shift, into the inventory; whatever does not
 * fit goes straight back), put the cursor stack in, shift-click a material in your inventory to store it, store all, auto on/off.
 */
public final class MateriaalMenu extends AbstractContainerMenu {
    public static final int NEEM = 0, STOP_HAND = 1, ALLES = 2, AUTO = 3, KNAPZAK = 4;
    public static final int GRID_X = 8, GRID_Y = 30, KOLOMMEN = 9, RIJEN = 5, INV_Y = GRID_Y + RIJEN * 18 + 26;

    @Nullable
    private final OpslagData data;
    @Nullable
    private final Predicate<ServerPlayer> geldig;
    private final Player speler;
    private int verzonden = -1;
    // client
    private BankContents inhoud = BankContents.EMPTY;
    private boolean auto = true;

    public MateriaalMenu(int id, Inventory inv, @Nullable OpslagData data, @Nullable Predicate<ServerPlayer> geldig) {
        super(OpslagRegistry.TAS_MENU.get(), id);
        this.data = data;
        this.geldig = geldig;
        this.speler = inv.player;
        addStandardInventorySlots(inv, 8, INV_Y);
    }

    public static boolean open(ServerPlayer p) {
        if (!Opslag.magOpenen(p)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.opslag.alleen_rijk").withStyle(ChatFormatting.GRAY));
            return false;
        }
        OpslagData d = Opslag.data(p);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new MateriaalMenu(id, inv, d, Opslag::magOpenen),
                Component.translatable("vadsrijk.opslag.tas")));
        Tips.toon(p, "vadsrijk.opslag.tip.tas");
        return true;
    }

    // ---- client side ---------------------------------------------------------------------------------------------------------

    public BankContents inhoud() {
        return inhoud;
    }

    public boolean auto() {
        return auto;
    }

    public void zetInhoud(BankContents b, boolean auto) {
        this.inhoud = b;
        this.auto = auto;
    }

    // ---- server side ---------------------------------------------------------------------------------------------------------

    /** The bag as the screen shows it (sorted by name). */
    public static BankContents momentopname(OpslagData d) {
        List<BankContents.Entry> l = new ArrayList<>();
        for (OpslagData.Materiaal m : d.materialen) {
            if (m.aantal > 0) {
                l.add(new BankContents.Entry(m.soort.copy(), m.aantal));
            }
        }
        return new BankContents(l);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (data != null && speler instanceof ServerPlayer sp && data.tasVersie != verzonden) {
            verzonden = data.tasVersie;
            PacketDistributor.sendToPlayer(sp, new OpslagPayloads.TasInhoud(containerId, momentopname(data), !data.autoUit));
        }
    }

    /** An action from the screen (server side, already on the main thread). */
    public void actie(ServerPlayer p, int actie, ItemStack soort, int knop, boolean shift) {
        if (data == null || !stillValid(p)) {
            return;
        }
        switch (actie) {
            case NEEM -> {
                if (!getCarried().isEmpty()) {
                    stopHand(p, knop);
                    break;
                }
                long heeft = Materiaaltas.tel(p, soort);
                if (heeft <= 0) {
                    break;
                }
                int max = soort.getMaxStackSize();
                int n = knop == 1 ? (int) Math.max(1, (Math.min(heeft, max) + 1) / 2) : max;
                ItemStack uit = Materiaaltas.haalUit(data, soort, n);
                if (shift) {
                    p.getInventory().add(uit);
                    if (!uit.isEmpty()) {
                        Materiaaltas.terug(data, uit);   // (no room: straight back)
                    }
                } else {
                    setCarried(uit);
                }
            }
            case STOP_HAND -> stopHand(p, knop);
            case ALLES -> Materiaaltas.allesOpbergen(p, data);
            case AUTO -> {
                data.autoUit = !data.autoUit;
                data.tasVersie++;
            }
            case KNAPZAK -> p.level().getServer().execute(() -> Knapzak.open(p));
            default -> {
            }
        }
        broadcastChanges();
    }

    private void stopHand(ServerPlayer p, int knop) {
        ItemStack c = getCarried();
        if (c.isEmpty()) {
            return;
        }
        if (!Materiaaltas.isMateriaal(c)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.opslag.tas.geen_materiaal").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (knop == 1) {
            ItemStack een = c.split(1);
            Materiaaltas.stop(data, een);
        } else {
            ItemStack alles = c.copy();
            setCarried(ItemStack.EMPTY);
            Materiaaltas.stop(data, alles);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (data != null && slot.hasItem() && Materiaaltas.isMateriaal(slot.getItem()) && player instanceof ServerPlayer sp && stillValid(sp)) {
            ItemStack weg = slot.getItem().copy();
            slot.setByPlayer(ItemStack.EMPTY);
            Materiaaltas.stop(data, weg);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (geldig == null) {
            return true;
        }
        return player instanceof ServerPlayer sp && geldig.test(sp);
    }
}
