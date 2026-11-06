package nl.juiced.guhs.feature.vadsrijk.karakter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.karakter.KarakterPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * T-KARAKTER, client: the character screen and refreshing the names when the badges change. No key (all good keys are taken).
 * U-KARAKTER (findable without commands): a "Ridders" tab in the collection row (Rijdieren / Huisdieren / ...), and a third
 * little button left of the Vadsrijk inventory (under t-opslag's knapzak + materiaaltas buttons).
 */
public final class KarakterClient {
    private KarakterClient() {
    }

    /** Asks the server for the character screen. */
    public static void open() {
        ClientPacketDistributor.sendToServer(new KarakterPayloads.Doe("open", new CompoundTag()));
    }

    public static void init(IEventBus modBus) {
        KarakterPayloads.schermOntvanger = s -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof KarakterSchermClient k) {
                k.update(s.data(), java.util.List.of());
            } else {
                mc.setScreen(new KarakterSchermClient(s.data()));
            }
        });
        KarakterPayloads.badgesOntvangen = () -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                for (Player p : mc.level.players()) {
                    p.refreshDisplayName();
                }
            }
        });
        // U-KARAKTER: the collection tab (first in the row) ...
        nl.juiced.guhs.feature.vadsrijk.client.CollectieTabs.registreer(KarakterSchermClient.TAB, 5,
                () -> Component.translatable("vadsrijk.karakter.tab"), KarakterClient::open);
        // ... and a button left of the inventory, under the two t-opslag buttons (x - 24, y + 4 / + 26)
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!(e.getScreen() instanceof InventoryScreen s) || mc.player == null || !Vadsrijk.in(mc.player) || mc.player.isCreative()) {
                return;
            }
            @SuppressWarnings("removal")
            int x = s.getGuiLeft() - 24, y = s.getGuiTop() + 4 + 44;
            e.addListener(Button.builder(Component.literal("⚔"), b -> open()).bounds(x, y, 20, 20)
                    .tooltip(Tooltip.create(Component.translatable("vadsrijk.karakter.knop.uitleg"))).build());
        });
    }
}
