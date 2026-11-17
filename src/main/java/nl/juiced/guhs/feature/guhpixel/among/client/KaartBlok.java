package nl.juiced.guhs.feature.guhpixel.among.client;

import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;

/**
 * 1.10.6: no JourneyMap during a round of Among Guhs. Its minimap (with the radar of every guh behind the walls) and its
 * fullscreen map would give everybody away, the Mikas too. While the client is in a round (the round's HUD is there) in
 * the Guhpixel dimension: the fullscreen map does not open (a little message instead) and the minimap is switched off.
 * Afterwards the minimap is as it was.
 * <p>
 * JourneyMap is no compile dependency (as in MinimapStil): by reflection, the minimap's own setting
 * {@code MiniMapProperties.enabled} (of the preset in use; a switched preset is caught on the next tick), set in memory
 * during the round and the old value put back (and saved) afterwards. Without JourneyMap, or if its insides change, this
 * quietly does nothing; the fullscreen map is kept away by its class name either way.
 */
public final class KaartBlok {
    private static final String FULLSCREEN = "journeymap.client.ui.fullscreen.";
    /** The minimap presets we switched off, with their old value. */
    private static final Map<Object, Boolean> UIT = new IdentityHashMap<>();
    private static boolean kapot;

    private KaartBlok() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(KaartBlok::scherm);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> terug());
    }

    /** In a round of Among Guhs right now (this client). */
    static boolean actief() {
        Minecraft mc = Minecraft.getInstance();
        return AmongClient.inRonde() && mc.level != null && mc.level.dimension() == Guhpixel.DIM;
    }

    private static void scherm(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null && event.getNewScreen().getClass().getName().startsWith(FULLSCREEN) && actief()) {
            event.setCanceled(true);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendOverlayMessage(Component.translatable("gui.guhs.among.geen_kaart"));
            }
        }
    }

    private static void tick() {
        if (!actief()) {
            terug();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen.getClass().getName().startsWith(FULLSCREEN)) {
            mc.setScreen(null);         // (opened before the round started)
        }
        Object props = props();
        if (props == null) {
            return;
        }
        try {
            Object veld = props.getClass().getField("enabled").get(props);
            Object aan = veld.getClass().getMethod("get").invoke(veld);
            if (Boolean.TRUE.equals(aan)) {
                UIT.putIfAbsent(props, Boolean.TRUE);
                veld.getClass().getMethod("set", Boolean.class).invoke(veld, Boolean.FALSE);
            } else {
                UIT.putIfAbsent(props, Boolean.FALSE);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            kapot = true;
        }
    }

    /** The round is over (or the client leaves): every switched-off minimap preset as it was. */
    private static void terug() {
        if (UIT.isEmpty()) {
            return;
        }
        for (Map.Entry<Object, Boolean> e : UIT.entrySet()) {
            if (!e.getValue()) {
                continue;
            }
            Object props = e.getKey();
            try {
                Object veld = props.getClass().getField("enabled").get(props);
                veld.getClass().getMethod("set", Boolean.class).invoke(veld, Boolean.TRUE);
                props.getClass().getMethod("save").invoke(props);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
                kapot = true;
            }
        }
        UIT.clear();
    }

    /** JourneyMap's minimap preset in use (null: no JourneyMap). */
    @Nullable
    private static Object props() {
        if (kapot) {
            return null;
        }
        try {
            Class<?> ui = Class.forName("journeymap.client.ui.UIManager");
            Object manager = ui.getField("INSTANCE").get(null);
            Object minimap = manager == null ? null : ui.getMethod("getMiniMap").invoke(manager);
            return minimap == null ? null : minimap.getClass().getMethod("getCurrentMinimapProperties").invoke(minimap);
        } catch (ClassNotFoundException e) {
            kapot = true;       // (no JourneyMap: never look again)
            return null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            kapot = true;
            return null;
        }
    }
}
