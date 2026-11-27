package nl.juiced.guhs.feature.vadsrijk.menu.client;

/**
 * U-TABS: a screen of the Vadsrijk-menu that may be drawn scaled down (small window / GUI scale 4). The screen lays itself out in a
 * LOGICAL size (set {@code width/height} to {@link nl.juiced.guhs.feature.vadsrijk.menu.MenuIndeling.Kader#lw()}/{@code lh()} in init); {@link MenuClient} then draws
 * it inside a scaled pose (tooltips included) and hands it mouse coordinates divided by the scale. 1 = no scaling; 1.10.24: above 1
 * = scaled up (a big window: bigger text and buttons).
 */
public interface Geschaald {
    float menuSchaal();
}
