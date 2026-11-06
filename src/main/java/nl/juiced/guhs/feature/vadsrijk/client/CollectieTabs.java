package nl.juiced.guhs.feature.vadsrijk.client;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu;

/**
 * (fix round 3, i-ui) The old tab row on top of the collection screens. U-TABS: replaced by the Vadsrijk-menu
 * ({@link VadsrijkMenu}); this class only forwards, so older calls keep working. New code: use {@link VadsrijkMenu#tab}.
 */
public final class CollectieTabs {
    /** Height of the tab row above the panel. */
    public static final int HOOGTE = VadsrijkMenu.TABS_H;

    private CollectieTabs() {
    }

    /** Adds a menu tab when that id is not registered yet (MenuTabs registers the real ones with icons and keys). */
    public static void registreer(String id, int volgorde, Supplier<Component> naam, Runnable open) {
        VadsrijkMenu.alsNieuw(id, volgorde, naam, open);
    }

    public static boolean zichtbaar() {
        return !VadsrijkMenu.hoofdtabs().isEmpty();
    }

    public static void teken(GuiGraphicsExtractor g, Font font, int left, int top, int w, @Nullable String actief, int mx, int my) {
        VadsrijkMenu.tekenTabs(g, font, left, top, w, actief == null ? "" : actief, mx, my);
    }

    public static boolean klik(double mx, double my, int left, int top, int w, @Nullable String actief) {
        return VadsrijkMenu.klik(Minecraft.getInstance().font, mx, my, left, top, w, 0, actief == null ? "" : actief);
    }
}
