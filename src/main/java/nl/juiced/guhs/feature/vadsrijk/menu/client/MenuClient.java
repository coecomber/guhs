package nl.juiced.guhs.feature.vadsrijk.menu.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;

/**
 * U-TABS, the client of the Vadsrijk-menu: the menu key (H: opens the menu on the tab you saw last, Karakter the first time), the
 * scaling of {@link Geschaald} screens (drawn in a scaled pose with logical mouse coordinates, clicks/drags/scrolls re-sent with
 * logical coordinates) and the registration of all tabs ({@link MenuTabs}).
 */
public final class MenuClient {
    public static final KeyMapping MENU = new KeyMapping("key.guhs.vr_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, KernClient.CATEGORIE);
    private static boolean bezig;

    private MenuClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(MENU));
        MenuTabs.registreer();
        nl.juiced.guhs.feature.vadsrijk.menu.MenuServer.ontvanger = a -> Minecraft.getInstance().execute(() -> MenuTabs.ontvang(a));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            boolean binnen = mc.player != null && Vadsrijk.in(mc.player) && mc.screen == null;
            while (MENU.consumeClick()) {
                if (binnen) {
                    VadsrijkMenu.open(null);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ScreenEvent.Render.Pre e) -> {
            float s = schaal(e.getScreen());
            if (s == 1f || bezig) {   // 1.10.24: also scaled up (s > 1)
                return;
            }
            e.setCanceled(true);
            GuiGraphicsExtractor g = e.getGuiGraphics();
            bezig = true;
            g.pose().pushMatrix();
            try {
                g.pose().scale(s, s);
                // Knapzak-kader: next to a container tab (over JEI's list) the screen gets the mouse as it is: JEI draws its hover and
                // tooltip from it. Not with an item on the cursor: that is drawn at the mouse and must stay under it.
                boolean echt = buiten(e.getScreen(), e.getMouseX(), e.getMouseY())
                        && !(e.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> c && !c.getMenu().getCarried().isEmpty());
                e.getScreen().extractRenderStateWithTooltipAndSubtitles(g, echt ? e.getMouseX() : (int) (e.getMouseX() / s),
                        echt ? e.getMouseY() : (int) (e.getMouseY() / s), e.getPartialTick());
            } finally {
                g.pose().popMatrix();
                bezig = false;
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseButtonPressed.Pre e) -> {
            float s = schaal(e.getScreen());
            if (s != 1f && !bezig && !buiten(e.getScreen(), e.getMouseButtonEvent().x(), e.getMouseButtonEvent().y())) {
                e.setCanceled(true);
                bezig = true;
                try {
                    e.getScreen().mouseClicked(klein(e.getMouseButtonEvent(), s), e.isDoubleClick());
                } finally {
                    bezig = false;
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseButtonReleased.Pre e) -> {
            float s = schaal(e.getScreen());
            if (s != 1f && !bezig && !buiten(e.getScreen(), e.getMouseButtonEvent().x(), e.getMouseButtonEvent().y())) {
                e.setCanceled(true);
                bezig = true;
                try {
                    e.getScreen().mouseReleased(klein(e.getMouseButtonEvent(), s));
                } finally {
                    bezig = false;
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseDragged.Pre e) -> {
            float s = schaal(e.getScreen());
            if (s != 1f && !bezig && !buiten(e.getScreen(), e.getMouseButtonEvent().x(), e.getMouseButtonEvent().y())) {
                e.setCanceled(true);
                bezig = true;
                try {
                    e.getScreen().mouseDragged(klein(e.getMouseButtonEvent(), s), e.getDragX() / s, e.getDragY() / s);
                } finally {
                    bezig = false;
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseScrolled.Pre e) -> {
            float s = schaal(e.getScreen());
            if (s != 1f && !bezig && !buiten(e.getScreen(), e.getMouseX(), e.getMouseY())) {
                e.setCanceled(true);
                bezig = true;
                try {
                    e.getScreen().mouseScrolled(e.getMouseX() / s, e.getMouseY() / s, e.getScrollDeltaX(), e.getScrollDeltaY());
                } finally {
                    bezig = false;
                }
            }
        });
    }

    /**
     * Knapzak-kader: is this (real) mouse position outside the rectangle a container tab really covers? There the event is not
     * re-sent with logical coordinates: it goes on as it is, first to other mods (JEI's item list: clicks, the wheel), then to
     * the screen, which sees a click outside its panel (an item on the cursor is dropped there, as in any container).
     */
    private static boolean buiten(Screen s, double x, double y) {
        int[] v = s instanceof Geschaald g ? g.echtVlak() : null;
        return v != null && (x < v[0] || y < v[1] || x >= v[0] + v[2] || y >= v[1] + v[3]);
    }

    private static float schaal(Screen s) {
        return s instanceof Geschaald g ? g.menuSchaal() : 1f;
    }

    private static MouseButtonEvent klein(MouseButtonEvent e, float s) {
        return new MouseButtonEvent(e.x() / s, e.y() / s, e.buttonInfo());
    }

    /** The frame for the current window (real gui-scaled size, also when the screen already switched to its logical size). */
    public static nl.juiced.guhs.feature.vadsrijk.menu.MenuIndeling.Kader kader() {
        var w = Minecraft.getInstance().getWindow();
        return VadsrijkMenu.kader(w.getGuiScaledWidth(), w.getGuiScaledHeight());
    }
}
