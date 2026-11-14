package nl.juiced.guhs.feature.vadsrijk.gilden.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.gilden.BondTags;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * U-GILDEN, client: refresh the names when the bond tags change, and a third small button ("⚑", under t-opslag's two) left of the
 * inventory screen in the Vadsrijk that opens the bond panel ({@code /bond}). No key: the good keys are all taken.
 */
public final class BondClient {
    private BondClient() {
    }

    public static void init(IEventBus modBus) {
        BondTags.ontvangen = () -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                for (Player p : mc.level.players()) {
                    p.refreshDisplayName();
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!(e.getScreen() instanceof InventoryScreen s) || mc.player == null || !Vadsrijk.in(mc.player) || mc.player.isCreative()) {
                return;
            }
            int x = s.getGuiLeft() - 24, y = s.getGuiTop() + 4 + 66;   // release-1.10.1: under the ⚔ (+44), was on top of it
            e.addListener(Button.builder(Component.literal("⚑"), b -> {
                if (mc.getConnection() != null) {
                    mc.getConnection().sendCommand("bond");
                }
                mc.setScreen(null);
            }).bounds(x, y, 20, 20).tooltip(Tooltip.create(Component.translatable("vadsrijk.gilden.knop.inventaris"))).build());
        });
    }
}
