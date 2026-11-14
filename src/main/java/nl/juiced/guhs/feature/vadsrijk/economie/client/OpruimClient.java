package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.Opruimen;
import nl.juiced.guhs.feature.vadsrijk.economie.Waarde;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * 1.10.1 "Opruimen", the client side outside the screens:
 * <ul>
 *   <li>gear tooltips: the shop value, what salvaging gives, and the lock (the better/worse line itself is U-SPECS' SpecClient);</li>
 *   <li>key L over a piece of Vadsrijk gear in the inventory: lock / unlock it (server checks everything again);</li>
 *   <li>a "⚒" button left of the inventory: the Opruimen screen (salvage + lock, from anywhere in the Vadsrijk).</li>
 * </ul>
 */
public final class OpruimClient {
    public static final int SLOT_TOETS = GLFW.GLFW_KEY_L;

    private OpruimClient() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> tooltip(e.getItemStack(), e.getToolTip()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.KeyPressed.Pre e) -> {
            if (e.getKeyEvent().key() != SLOT_TOETS || !(e.getScreen() instanceof AbstractContainerScreen<?> s) || !binnen()) {
                return;
            }
            Slot onder = s.getHoveredSlot();
            if (onder != null && onder.container instanceof Inventory && Rollen.van(onder.getItem()) != null) {
                CompoundTag a = new CompoundTag();
                a.putInt("slot", onder.getContainerSlot());
                ClientPacketDistributor.sendToServer(new EcoPayloads.Doe(EcoPayloads.OPRUIM, "vergrendel_inv", a));
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!(e.getScreen() instanceof InventoryScreen s) || mc.player == null || !Vadsrijk.in(mc.player) || mc.player.isCreative()) {
                return;
            }
            // (column left of the inventory, 22 apart: opslag ▤ +4 / ✿ +26, karakter ⚔ +48, bond ⚑ +70, this ⚒ +92)
            int x = s.getGuiLeft() - 24, y = s.getGuiTop() + 4 + 88;
            e.addListener(Button.builder(Component.literal("⚒"),
                    b -> ClientPacketDistributor.sendToServer(new EcoPayloads.Doe(EcoPayloads.OPRUIM, "open", new CompoundTag())))
                    .bounds(x, y, 20, 20).tooltip(Tooltip.create(Component.translatable(Opruimen.K + "knop.inventaris"))).build());
        });
    }

    private static boolean binnen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && Vadsrijk.in(mc.player);
    }

    static void tooltip(ItemStack s, List<Component> t) {
        Uitrusting u = Rollen.van(s);
        if (u == null || u.basisType() == null) {
            return;
        }
        t.add(Component.translatable("vadsrijk.economie.tooltip.waarde", Spelers.munten(Waarde.gear(u))).withStyle(ChatFormatting.GOLD));
        ItemStack uit = Opruimen.opbrengst(u);
        t.add(Component.translatable(Opruimen.K + "tooltip.ontmantel", uit.getCount() + "x " + uit.getHoverName().getString())
                .withStyle(ChatFormatting.DARK_AQUA));
        if (Opruimen.vergrendeld(s)) {
            t.add(Component.translatable(Opruimen.K + "tooltip.slot").withStyle(ChatFormatting.YELLOW));
        } else if (binnen() && Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>) {
            t.add(Component.translatable(Opruimen.K + "tooltip.slot_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
