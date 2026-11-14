package nl.juiced.guhs.feature.vadsrijk.dungeon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonNet;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * 1.10.3 (user request): an always-available way out of a dungeon. While the player is a member of a dungeon group (the server
 * says so, {@link DungeonNet.Lid}), the inventory gets a "Dungeon verlaten" button at its top right; it asks first (a confirm
 * screen, so it is never clicked by accident), then the server puts the player outside the main door and out of the group
 * (DungeonRij.verlaat, the same as the door window's button).
 */
public final class DungeonVerlaatKnop {
    private DungeonVerlaatKnop() {
    }

    public static void installeer() {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            String dungeon = DungeonNet.LID;
            if (!(e.getScreen() instanceof InventoryScreen s) || mc.player == null || !Vadsrijk.in(mc.player) || dungeon.isEmpty()) {
                return;
            }
            int x = s.getGuiLeft() + s.getXSize() + 4, y = s.getGuiTop() + 4;
            e.addListener(Button.builder(Component.translatable("vadsrijk.dungeon.inv.knop"), b -> vraag(mc, s, dungeon))
                    .bounds(x, y, 96, 20).tooltip(Tooltip.create(Component.translatable("vadsrijk.dungeon.inv.tip"))).build());
        });
    }

    static void vraag(Minecraft mc, InventoryScreen terug, String dungeon) {
        mc.setScreen(new ConfirmScreen(ja -> {
            if (ja) {
                ClientPacketDistributor.sendToServer(new DungeonNet.Actie("verlaat", dungeon));
                mc.setScreen(null);
            } else {
                mc.setScreen(terug);
            }
        }, Component.translatable("vadsrijk.dungeon.inv.vraag"), Component.translatable("vadsrijk.dungeon.inv.uitleg"),
                Component.translatable("vadsrijk.dungeon.inv.ja"), Component.translatable("vadsrijk.dungeon.inv.nee")));
    }
}
