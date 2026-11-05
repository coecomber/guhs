package nl.juiced.guhs.feature.vadsrijk.items.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.items.Drankjesriem;
import nl.juiced.guhs.feature.vadsrijk.items.Erfstukken;
import nl.juiced.guhs.feature.vadsrijk.items.ItemsPayloads;
import nl.juiced.guhs.feature.vadsrijk.items.Sets;
import nl.juiced.guhs.feature.vadsrijk.items.Vangnet;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/**
 * T-ITEMS client: the drankjesriem (keys Y / I / O = slot 1 / 2 / 3, three slots left of the hotbar with the key and the
 * potion cooldown; in the inventory screen a column of 3 slots right of the jewellery column: click with a potion on the
 * cursor = in, click empty-handed = out, shift-click a potion in the inventory = into the belt), and the Erfstuk tooltip
 * lines (Vertrouwd, grown, smid's patience, set glove/cape).
 */
public final class ItemsClient {
    public static final KeyMapping[] RIEM = {
            new KeyMapping("key.guhs.vr_t_drankje1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, KernClient.CATEGORIE),
            new KeyMapping("key.guhs.vr_t_drankje2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, KernClient.CATEGORIE),
            new KeyMapping("key.guhs.vr_t_drankje3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, KernClient.CATEGORIE)};
    private static final int MAAT = 18;
    private static List<ItemStack> riem = leeg();

    private ItemsClient() {
    }

    private static List<ItemStack> leeg() {
        return new ArrayList<>(Collections.nCopies(Drankjesriem.VAKKEN, ItemStack.EMPTY));
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            for (KeyMapping k : RIEM) {
                e.register(k);
            }
        });
        ItemsPayloads.riemOntvanger = r -> Minecraft.getInstance().execute(() -> {
            List<ItemStack> l = new ArrayList<>(r.items());
            while (l.size() < Drankjesriem.VAKKEN) {
                l.add(ItemStack.EMPTY);
            }
            riem = l;
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            boolean binnen = mc.player != null && Vadsrijk.in(mc.player) && mc.screen == null;
            for (int i = 0; i < RIEM.length; i++) {
                while (RIEM[i].consumeClick()) {
                    if (binnen) {
                        ClientPacketDistributor.sendToServer(new ItemsPayloads.RiemActie(0, i));
                    }
                }
            }
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("vr_t_items_riem"), ItemsClient::hud));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Foreground e) -> scherm(e));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> klik(e));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> riem = leeg());
        RijkItem.TOOLTIP_EXTRA.add(ItemsClient::tooltip);
    }

    // ---- HUD ----------------------------------------------------------------------------------------------------------------

    private static void hud(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Vadsrijk.in(mc.player) || mc.options.hideGui || riem.stream().allMatch(ItemStack::isEmpty)) {
            return;
        }
        Font f = mc.font;
        int x0 = g.guiWidth() / 2 - 91 - 32 - Drankjesriem.VAKKEN * 21, y = g.guiHeight() - 21;
        for (int i = 0; i < Drankjesriem.VAKKEN; i++) {
            int x = x0 + i * 21;
            ItemStack s = riem.get(i);
            g.fill(x - 1, y - 1, x + 19, y + 19, 0xFF6A3A5A);
            g.fill(x, y, x + 18, y + 18, 0xE0281A24);
            if (!s.isEmpty()) {
                g.item(s, x + 1, y + 1);
                g.itemDecorations(f, s, x + 1, y + 1);
                float cd = mc.player.getCooldowns().getCooldownPercent(s, dt.getGameTimeDeltaPartialTick(true));
                if (cd > 0) {
                    int h = (int) Math.ceil(18 * cd);
                    g.fill(x, y + 18 - h, x + 18, y + 18, 0xA0000000);
                }
            }
            String toets = RIEM[i].getTranslatedKeyMessage().getString();
            if (toets.length() > 2) {
                toets = toets.substring(0, 2);
            }
            g.text(f, toets, x + 1, y - 9, 0xFFFFD6EB, true);
        }
    }

    // ---- the inventory screen -------------------------------------------------------------------------------------------------

    private static boolean binnen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && Vadsrijk.in(mc.player);
    }

    /** Right of kern's jewellery column (SieradenScherm: leftPos + 179). */
    private static int x(InventoryScreen s) {
        return s.getLeftPos() + 176 + 3 + MAAT + 4;
    }

    private static int y(InventoryScreen s, int i) {
        return s.getTopPos() + 18 + i * (MAAT + 2);
    }

    private static int vak(InventoryScreen s, double mx, double my) {
        int x = x(s);
        for (int i = 0; i < Drankjesriem.VAKKEN; i++) {
            int y = y(s, i);
            if (mx >= x && mx < x + MAAT && my >= y && my < y + MAAT) {
                return i;
            }
        }
        return -1;
    }

    private static void scherm(ScreenEvent.Render.Foreground e) {
        if (!(e.getScreen() instanceof InventoryScreen s) || !binnen()) {
            return;
        }
        GuiGraphicsExtractor g = e.getGuiGraphics();
        int x = x(s);
        if (x + MAAT > s.width) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        g.text(mc.font, Component.translatable("vadsrijk.items.riem.kort"), x + 2, s.getTopPos() + 7, 0xFFFFD6EB, true);
        int hover = vak(s, e.getMouseX(), e.getMouseY());
        for (int i = 0; i < Drankjesriem.VAKKEN; i++) {
            int y = y(s, i);
            g.fill(x - 1, y - 1, x + MAAT + 1, y + MAAT + 1, 0xFF5A6A3A);
            g.fill(x, y, x + MAAT, y + MAAT, i == hover ? 0xFF4E5A3A : 0xFF2E3324);
            ItemStack it = riem.get(i);
            if (!it.isEmpty()) {
                g.item(it, x + 1, y + 1);
                g.itemDecorations(mc.font, it, x + 1, y + 1);
            } else {
                g.centeredText(mc.font, RIEM[i].getTranslatedKeyMessage(), x + MAAT / 2, y + 5, 0xFF8E9A7A);
            }
        }
        if (hover >= 0) {
            ItemStack it = riem.get(hover);
            List<Component> regels = new ArrayList<>();
            if (!it.isEmpty()) {
                regels.addAll(net.minecraft.client.gui.screens.Screen.getTooltipFromItem(mc, it));
                regels.add(Component.translatable("vadsrijk.items.riem.uitleg_vol", RIEM[hover].getTranslatedKeyMessage()).withStyle(ChatFormatting.DARK_AQUA));
            } else {
                regels.add(Component.translatable("vadsrijk.items.riem.uitleg_leeg", hover + 1, RIEM[hover].getTranslatedKeyMessage()));
            }
            g.setTooltipForNextFrame(mc.font, regels, java.util.Optional.empty(), e.getMouseX(), e.getMouseY());
        }
    }

    private static void klik(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!(e.getScreen() instanceof InventoryScreen s) || !binnen()) {
            return;
        }
        net.minecraft.world.inventory.Slot onder = s.getHoveredSlot();
        if (e.getMouseButtonEvent().hasShiftDown() && onder != null && onder.container instanceof net.minecraft.world.entity.player.Inventory
                && onder.getContainerSlot() < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE && Drankjesriem.isDrankje(onder.getItem())) {
            ClientPacketDistributor.sendToServer(new ItemsPayloads.RiemActie(2, onder.getContainerSlot()));
            e.setCanceled(true);
            return;
        }
        int i = vak(s, e.getMouseX(), e.getMouseY());
        if (i >= 0) {
            ClientPacketDistributor.sendToServer(new ItemsPayloads.RiemActie(1, i));
            e.setCanceled(true);
        }
    }

    // ---- tooltip lines --------------------------------------------------------------------------------------------------------

    private static void tooltip(Uitrusting u, java.util.function.Consumer<Component> t) {
        int lv = Erfstukken.vertrouwd(u);
        int xp = u.erf().xp();
        if (lv >= Erfstukken.MAX_VERTROUWD) {
            t.accept(Component.translatable("vadsrijk.items.tip.vertrouwd_max", Math.round((Erfstukken.factor(u) - 1) * 100))
                    .withStyle(ChatFormatting.GOLD));
        } else if (xp > 0) {
            t.accept(Component.translatable("vadsrijk.items.tip.vertrouwd", lv, xp, Erfstukken.DREMPELS[lv], Math.round((Erfstukken.factor(u) - 1) * 100))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (u.erf().groei() > 0) {
            t.accept(Component.translatable("vadsrijk.items.tip.gegroeid", "I".repeat(Math.min(5, u.erf().groei())) + (u.erf().groei() > 5 ? "+" : ""))
                    .withStyle(ChatFormatting.GOLD));
        } else if (u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH || u.zeldzaamheid() == Zeldzaamheid.GUHDDELIJK) {
            t.accept(Component.translatable("vadsrijk.items.tip.kan_groeien").withStyle(ChatFormatting.DARK_GRAY));
        }
        double pity = Vangnet.pityBonus(u);
        if (pity > 0) {
            t.accept(Component.translatable("vadsrijk.items.tip.geduld", Math.round(pity * 100)).withStyle(ChatFormatting.YELLOW));
        }
        String set = Sets.setVan(u.type());
        if (set != null) {
            t.accept(Component.translatable("vadsrijk.items.tip.set", Component.translatable("vadsrijk.items.set." + set))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
