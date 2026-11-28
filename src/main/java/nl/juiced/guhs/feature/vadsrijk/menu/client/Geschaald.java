package nl.juiced.guhs.feature.vadsrijk.menu.client;

/**
 * U-TABS: a screen of the Vadsrijk-menu that may be drawn scaled down (small window / GUI scale 4). The screen lays itself out in a
 * LOGICAL size (set {@code width/height} to {@link nl.juiced.guhs.feature.vadsrijk.menu.MenuIndeling.Kader#lw()}/{@code lh()} in init); {@link MenuClient} then draws
 * it inside a scaled pose (tooltips included) and hands it mouse coordinates divided by the scale. 1 = no scaling; 1.10.24: above 1
 * = scaled up (a big window: bigger text and buttons).
 */
public interface Geschaald {
    float menuSchaal();

    /**
     * Knapzak-kader: the rectangle this screen really covers on the window (gui pixels, NOT logical ones): {x, y, w, h}, or null
     * for "the whole window" (the default). Only the container tabs (knapzak, materiaaltas) give one: next to it another mod
     * (JEI's item list) may draw and take the mouse, so outside this rectangle {@link MenuClient} leaves mouse events alone
     * and hands the screen the real mouse position, and compat/jei tells JEI this rectangle instead of the logical panel.
     */
    @javax.annotation.Nullable
    default int[] echtVlak() {
        return null;
    }
}
