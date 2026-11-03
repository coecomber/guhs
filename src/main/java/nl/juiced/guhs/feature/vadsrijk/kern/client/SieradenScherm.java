package nl.juiced.guhs.feature.vadsrijk.kern.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;

/**
 * The 3 jewellery slots (Ring, Ketting, Oorbel) as a small column on the right of the inventory screen, only in the dimension.
 * Hover = the item's tooltip; click = take it off (the server checks). Putting one on = right-click it with the item in hand.
 * (Right side: the Maagwand tongue already sits on the left.)
 */
final class SieradenScherm {
    private static List<ItemStack> items = new ArrayList<>(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
    private static final int MAAT = 18;

    private SieradenScherm() {
    }

    static void init() {
        KernPayloads.sieradenOntvanger = s -> items = new ArrayList<>(s.items());
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post e) -> teken(e));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> {
            if (!(e.getScreen() instanceof InventoryScreen s) || !binnen()) {
                return;
            }
            int i = vak(s, e.getMouseX(), e.getMouseY());
            if (i >= 0) {
                if (!items.get(i).isEmpty()) {
                    ClientPacketDistributor.sendToServer(new KernPayloads.SieraadAf(i));
                }
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> items = new ArrayList<>(List.of(ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY)));
    }

    private static boolean binnen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && Vadsrijk.in(mc.player);
    }

    private static int x(InventoryScreen s) {
        return s.getGuiLeft() + 176 + 3;
    }

    private static int y(InventoryScreen s, int i) {
        return s.getGuiTop() + 8 + i * (MAAT + 2);
    }

    private static int vak(InventoryScreen s, double mx, double my) {
        int x = x(s);
        for (int i = 0; i < Slot.SIERADEN.length; i++) {
            int y = y(s, i);
            if (mx >= x && mx < x + MAAT && my >= y && my < y + MAAT) {
                return i;
            }
        }
        return -1;
    }

    private static void teken(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof InventoryScreen s) || !binnen()) {
            return;
        }
        GuiGraphicsExtractor g = e.getGuiGraphics();
        int x = x(s);
        if (x + MAAT > s.width) {
            return;
        }
        int hover = vak(s, e.getMouseX(), e.getMouseY());
        for (int i = 0; i < Slot.SIERADEN.length; i++) {
            int y = y(s, i);
            g.fill(x - 1, y - 1, x + MAAT + 1, y + MAAT + 1, 0xFF6A3A5A);
            g.fill(x, y, x + MAAT, y + MAAT, i == hover ? 0xFF5A3A4E : 0xFF3A2433);
            ItemStack it = i < items.size() ? items.get(i) : ItemStack.EMPTY;
            if (!it.isEmpty()) {
                g.item(it, x + 1, y + 1);
            } else {
                String l = Component.translatable(Slot.SIERADEN[i].key()).getString().substring(0, 1);
                g.centeredText(Minecraft.getInstance().font, l, x + MAAT / 2, y + 5, 0xFF9A7A8E);
            }
        }
        if (hover >= 0) {
            ItemStack it = items.get(hover);
            if (!it.isEmpty()) {
                g.setTooltipForNextFrame(Minecraft.getInstance().font, it, e.getMouseX(), e.getMouseY());
            } else {
                g.setTooltipForNextFrame(Minecraft.getInstance().font, Component.translatable("vadsrijk.kern.sieraad.leeg",
                        Component.translatable(Slot.SIERADEN[hover].key())), e.getMouseX(), e.getMouseY());
            }
        }
    }
}
