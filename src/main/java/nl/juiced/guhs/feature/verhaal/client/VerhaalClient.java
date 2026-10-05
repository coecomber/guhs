package nl.juiced.guhs.feature.verhaal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.util.TriState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.verhaal.VerhaalPayloads;
import nl.juiced.guhs.feature.verhaal.VerhaalSync;

/**
 * 3.0 (Guhverhalen), client side of the fundament. The per-variant looks live in {@link VariantUiterlijk} (the owner slices
 * register them); the talking screen is the 2.8 PraatScherm (knuffeldal client), which now also shows scenes.
 * <p>
 * bbq2: the client side of the verhaal engine: {@link CutsceneSpeler} (camera cutscenes), {@link VertelScherm} (narrator
 * cards), {@link VerhaalHud} (the black bars and subtitles, the objective line) and {@link SluierRook} (the smoke of
 * Guhdalfs sluier). While a cutscene plays every other HUD layer, the hand and the name tags are off, clicks do nothing
 * and no inventory, chat or mod screen opens (Esc still gives the pause menu).
 */
public final class VerhaalClient {
    public static final net.minecraft.resources.Identifier LAAG_CUTSCENE = Guhs.id("verhaal_cutscene"), LAAG_DOEL = Guhs.id("verhaal_doel");

    public static void init(IEventBus modBus) {
        VariantUiterlijk.hook();
        // --- bbq2: the verhaal engine ---
        VerhaalPayloads.speelOntvanger = CutsceneSpeler::start;
        VerhaalPayloads.kaartOntvanger = VertelScherm::toon;
        VerhaalPayloads.stopOntvanger = p -> {
            CutsceneSpeler.stop(false);
            VertelScherm.sluit();
        };
        VerhaalPayloads.rookOntvanger = SluierRook::ontvang;
        VerhaalSync.Client.vernieuwd = () -> {
            if (Minecraft.getInstance().screen instanceof nl.juiced.guhs.client.screen.GuhDexScreen dex) {
                dex.verhalenVernieuwd();
            }
        };
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) -> event.registerSpriteSet(
                nl.juiced.guhs.feature.verhaal.VerhaalFeature.SLUIERROOK.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new SluierRook.Wolk(level, x, y, z, dx, dy, dz, sprites.get(random))));
        modBus.addListener((RegisterGuiLayersEvent event) -> {
            event.registerAboveAll(LAAG_DOEL, VerhaalHud::doel);
            event.registerAboveAll(LAAG_CUTSCENE, VerhaalHud::cutscene);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> CutsceneSpeler.voorTick());
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            CutsceneSpeler.tick();
            VertelScherm.bewaak();
            SluierRook.tick();
        });
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeCameraAngles event) -> CutsceneSpeler.hoeken(event));
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Pre event) -> {
            if (CutsceneSpeler.actief() && !event.getName().equals(LAAG_CUTSCENE)) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderHandEvent event) -> {
            if (CutsceneSpeler.actief()) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderNameTagEvent.CanRender event) -> {
            if (CutsceneSpeler.actief()) {
                event.setCanRender(TriState.FALSE);
            }
        });
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered event) -> {
            if (CutsceneSpeler.actief()) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening event) -> {
            if (CutsceneSpeler.actief() && geweerd(event.getNewScreen())) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            CutsceneSpeler.stop(false);
            VertelScherm.sluit();
            SluierRook.wis();
        });
    }

    /** A screen that may not open while a cutscene plays: the inventory and other containers, chat, every screen of this mod. */
    private static boolean geweerd(Screen scherm) {
        return scherm instanceof AbstractContainerScreen<?> || scherm instanceof ChatScreen
                || scherm != null && scherm.getClass().getName().startsWith("nl.juiced.guhs.") && !(scherm instanceof VertelScherm);
    }

    /** Tells the common side whether the local player is watching something ({@code Cutscenes.bezig} on the client). */
    static void zetBezig() {
        nl.juiced.guhs.feature.verhaal.Cutscenes.zetClientBezig(CutsceneSpeler.actief() || VertelScherm.actief());
    }

    private VerhaalClient() {
    }
}
