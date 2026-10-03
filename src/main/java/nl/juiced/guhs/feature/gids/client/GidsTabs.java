package nl.juiced.guhs.feature.gids.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Icon tabs along the top of a gids screen (2.9), like the creative inventory's: a row of little tabs with an item icon,
 * the chosen one lighter and joined to the page below it, the name as a tooltip on hover (the screen draws it, see
 * {@link #onder}). Used by the Guhdex and the Superkompas, each with its own colours ({@link Stijl}).
 */
public final class GidsTabs {
    public static final int W = 24, H = 22, GAP = 2;

    /** Colours: the frame line, an idle tab, a hovered tab, the chosen tab (= the page colour), the shine. */
    public record Stijl(int rand, int tab, int hover, int actief, int glans) {
    }

    public static final Stijl GUHDEX = new Stijl(0xFFD27A9C, 0xFFF3C6D6, 0xFFF9DCE7, 0xFFFFF4F8, 0x80FFFFFF);
    public static final Stijl SUPERKOMPAS = new Stijl(0xFFF7D27A, 0xFF4A2A3A, 0xFF6A3E54, 0xFF301A26, 0x30FFFFFF);

    /** Draws the tabs from (x, y) to the right; the chosen tab reaches 1 pixel lower (it merges into the page). */
    public static void teken(GuiGraphicsExtractor g, int x, int y, List<ItemStack> icons, int actief, double mouseX, double mouseY, Stijl s) {
        teken(g, x, y, icons, actief, mouseX, mouseY, s, W, GAP);
    }

    /** The same, with tabs of width w and gap pixels between them (1.2.6: the Guhdex has seven tabs, a little narrower). */
    public static void teken(GuiGraphicsExtractor g, int x, int y, List<ItemStack> icons, int actief, double mouseX, double mouseY, Stijl s,
            int w, int gap) {
        for (int i = 0; i < icons.size(); i++) {
            int tx = x + i * (w + gap);
            boolean on = i == actief;
            boolean hover = !on && binnen(tx, y, w, mouseX, mouseY);
            int top = on ? y : y + 2;
            int bottom = y + H + (on ? 1 : 0);
            // a rounded tab: the frame colour around it, the corners cut off
            g.fill(tx + 1, top, tx + w - 1, top + 1, s.rand());
            g.fill(tx, top + 1, tx + 1, bottom, s.rand());
            g.fill(tx + w - 1, top + 1, tx + w, bottom, s.rand());
            g.fill(tx + 1, top + 1, tx + w - 1, bottom, on ? s.actief() : hover ? s.hover() : s.tab());
            g.fill(tx + 2, top + 1, tx + w - 2, top + 2, s.glans());
            int iy = top + (bottom - top - 16) / 2 + (on ? 0 : 1);
            g.item(icons.get(i), tx + (w - 16) / 2, iy);
        }
    }

    /** The tab under the mouse (-1: none). */
    public static int onder(int x, int y, int count, double mouseX, double mouseY) {
        return onder(x, y, count, mouseX, mouseY, W, GAP);
    }

    public static int onder(int x, int y, int count, double mouseX, double mouseY, int w, int gap) {
        for (int i = 0; i < count; i++) {
            if (binnen(x + i * (w + gap), y, w, mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    /** The width of a row of tabs. */
    public static int breedte(int count) {
        return breedte(count, W, GAP);
    }

    public static int breedte(int count, int w, int gap) {
        return count * w + Math.max(0, count - 1) * gap;
    }

    private static boolean binnen(int tx, int y, int w, double mouseX, double mouseY) {
        return mouseX >= tx && mouseX < tx + w && mouseY >= y && mouseY < y + H;
    }

    private GidsTabs() {
    }
}
