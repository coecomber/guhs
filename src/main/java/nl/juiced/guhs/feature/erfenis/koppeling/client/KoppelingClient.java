package nl.juiced.guhs.feature.erfenis.koppeling.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.erfenis.koppeling.KoppelingPayloads;

/** The koppeling's client side: the makelaarsbrief's screen. */
public final class KoppelingClient {
    private KoppelingClient() {
    }

    public static void init(IEventBus modBus) {
        KoppelingPayloads.briefOntvanger = b -> Minecraft.getInstance().setScreen(new MakelaarsbriefScherm(b.gekocht(), b.knabbels()));
        // 1.9.5: the Leverkist's big-stack menu, drawn by the vanilla (six-row) chest screen
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) -> event.register(
                nl.juiced.guhs.feature.erfenis.koppeling.KoppelingFeature.LEVERKIST_MENU.get(),
                net.minecraft.client.gui.screens.inventory.ContainerScreen::new));
    }
}
