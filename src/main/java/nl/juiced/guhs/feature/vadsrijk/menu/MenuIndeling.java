package nl.juiced.guhs.feature.vadsrijk.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * U-TABS: the side-neutral part of the Vadsrijk-menu (no client classes, so gametests can check it): the tab registry and the
 * frame geometry. Use the client facade {@code menu.client.VadsrijkMenu} to register tabs.
 */
public final class MenuIndeling {
    /** Height of the main tab row above the frame. */
    public static final int TABS_H = 20;
    /** Height of the title band (and of the sub-tab row inside it). */
    public static final int BAND = 18;
    /** The smallest logical frame every panel can count on (the screen scales down below this) and the largest it grows to. */
    public static final int MIN_W = 430, MIN_H = 250, MAX_W = 560, MAX_H = 330;
    static final int MARGE = 6;

    /**
     * One tab. {@code ouder} = null for a main tab, else the main tab it belongs under. {@code open} shows its panel (often:
     * ask the server, which sends the data that opens the screen).
     */
    public record Tab(String id, Supplier<ItemStack> icoon, Supplier<Component> naam, Runnable open, int volgorde, @Nullable String ouder) {
    }

    private static final List<Tab> TABS = new ArrayList<>();

    private MenuIndeling() {
    }

    // ---- registry -------------------------------------------------------------------------------------------------------------

    /** Adds or replaces (same id) a tab. */
    public static synchronized Tab zet(Tab t) {
        TABS.removeIf(o -> o.id().equals(t.id()));
        TABS.add(t);
        TABS.sort(Comparator.comparingInt(Tab::volgorde));
        return t;
    }

    /** Adds a main tab only when the id is not there yet (the old CollectieTabs shim). */
    public static synchronized void alsNieuw(String id, int volgorde, Supplier<Component> naam, Runnable open) {
        if (vind(id) == null) {
            zet(new Tab(id, () -> new ItemStack(net.minecraft.world.item.Items.BOOK), naam, open, volgorde, null));
        }
    }

    /** (tests) removes a tab. */
    public static synchronized void weg(String id) {
        TABS.removeIf(o -> o.id().equals(id));
    }

    public static synchronized List<Tab> alle() {
        return List.copyOf(TABS);
    }

    public static synchronized List<Tab> hoofdtabs() {
        return TABS.stream().filter(t -> t.ouder() == null).toList();
    }

    /** The sub-tabs of a main tab (empty = none; else the main tab first, then its sub-tabs). */
    public static synchronized List<Tab> subtabs(String hoofd) {
        List<Tab> l = new ArrayList<>();
        for (Tab t : TABS) {
            if (hoofd.equals(t.ouder())) {
                l.add(t);
            }
        }
        if (l.isEmpty()) {
            return l;
        }
        Tab h = vind(hoofd);
        if (h != null) {
            l.add(0, h);
        }
        return l;
    }

    @Nullable
    public static synchronized Tab vind(String id) {
        for (Tab t : TABS) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }

    /** The main tab a (sub-)tab id belongs to. */
    public static String hoofdVan(String id) {
        Tab t = vind(id);
        return t != null && t.ouder() != null ? t.ouder() : id;
    }

    // ---- geometry -------------------------------------------------------------------------------------------------------------

    /**
     * The frame on a screen of (gui-scaled) size sw x sh. {@code schaal} &lt; 1 when even the smallest frame does not fit: the
     * panel then works in a LOGICAL screen of {@code lw x lh} (= sw/schaal x sh/schaal) and is drawn scaled down; 1.10.24:
     * {@code schaal} &gt; 1 when even the largest frame leaves room: the same, drawn scaled up (bigger text and buttons).
     * left/top/w/h = the panel (title band included); the main tab row sits above top.
     */
    public record Kader(float schaal, int lw, int lh, int left, int top, int w, int h) {
    }

    /** 1.10.24: the largest a screen is scaled up (2 = GUI scale 2 looks like GUI scale 4). */
    public static final float MAX_SCHAAL = 2f;
    /**
     * 1.10.24: the menu frame is only scaled up while it still gets at least this logical size (comfortable, above MIN). 1.10.27: was
     * 520 x 300; the frame now takes a smaller share of the window, so the comfortable size went down with it.
     */
    public static final int RUIM_W = 480, RUIM_H = 280;
    /**
     * 1.10.27 ("gebruikt net wat te veel van het scherm"): the largest share of the window the menu takes: the frame of the width, the
     * tab row + frame of the height (was about 0.875 x 0.955 at 1080p; a panel that needs more than this still gets what it needs).
     */
    public static final float AANDEEL_W = 0.80f, AANDEEL_H = 0.86f;
    /** 1.10.27: a loose screen that grows with the window takes at most this share of the height (no tab row above it). */
    public static final float LOS_H = 0.82f;
    /** 1.10.27: a loose screen of a fixed size is only scaled up while it stays within this share of the window (both ways). */
    public static final float LOS_MAX = 0.86f;

    /**
     * 1.10.24: the scale-up factor: the largest k / guiScale (k whole, so the REAL pixel scale guiScale x s stays a whole number and
     * the pixel font stays crisp; 1.45 made uneven letters) at which the logical screen sw/s x sh/s still holds needW x needH; at most
     * {@link #MAX_SCHAAL}; 1 when even the next step does not fit.
     */
    public static float opschaal(int sw, int sh, int needW, int needH, int guiScale) {
        int gs = Math.max(1, guiScale);
        float best = 1f;
        for (int k = gs + 1; k <= Math.round(gs * MAX_SCHAAL); k++) {
            float s = k / (float) gs;
            if (sw / s < needW || sh / s < needH) {
                break;
            }
            best = s;
        }
        return best;
    }

    public static Kader kader(int sw, int sh) {
        return kader(sw, sh, MIN_W, MIN_H);
    }

    /** The frame for a panel that needs at least minW x minH (e.g. a tall skill tree): bigger than the others only then. */
    public static Kader kader(int sw, int sh, int minW, int minH) {
        return kader(sw, sh, minW, minH, true, 1);
    }

    /**
     * 1.10.24: {@code opschalen} false = never scaled UP (container screens: JEI and other overlays place their item list from the
     * screen's own, unscaled panel rectangle, so a scaled-up knapzak got JEI's list drawn through it). {@code guiScale}: the
     * window's GUI scale (the scale-up keeps the real pixel scale whole). 1.10.27: the frame takes at most
     * {@link #AANDEEL_W} x {@link #AANDEEL_H} of the window (never less than the panel needs).
     */
    public static Kader kader(int sw, int sh, int minW, int minH, boolean opschalen, int guiScale) {
        int mw = Math.max(MIN_W, minW), mh = Math.max(MIN_H, minH), xw = Math.max(MAX_W, mw), xh = Math.max(MAX_H, mh);
        int nodigW = mw + 2 * MARGE, nodigH = mh + TABS_H + 2 * MARGE + 4;
        float s = Math.min(1f, Math.min(sw / (float) nodigW, sh / (float) nodigH));
        if (s >= 1f && opschalen) {
            // 1.10.24 ("maak interfaces groter en gebruik meer ruimte"): with room to spare the frame is drawn scaled UP
            // (the share of the window counts for the comfortable size; a panel that needs more, like the skill tree, only has to fit)
            s = opschaal(sw, sh, Math.max(Math.max(RUIM_W, mw) + 2 * MARGE + 8, (int) Math.ceil(RUIM_W / AANDEEL_W)),
                    Math.max(Math.max(RUIM_H, mh) + TABS_H + 2 * MARGE + 4, (int) Math.ceil((RUIM_H + TABS_H) / AANDEEL_H)), guiScale);
        }
        int lw = (int) Math.floor(sw / s), lh = (int) Math.floor(sh / s);
        int w = Math.max(mw, Math.min(xw, Math.min(lw - 2 * MARGE - 8, (int) (lw * AANDEEL_W))));
        int h = Math.max(mh, Math.min(xh, Math.min(lh - 2 * MARGE - TABS_H - 4, (int) (lh * AANDEEL_H) - TABS_H)));
        int left = (lw - w) / 2;
        int top = Math.max(MARGE + TABS_H + 2, (lh - h + TABS_H) / 2);
        return new Kader(s, lw, lh, left, top, w, h);
    }

    /**
     * 1.10.27: the place of a LOOSE screen (smid, winkel, werkplaats, ...: no tab row) on a screen of (gui-scaled) size sw x sh. The
     * panel is minW x minH; with maxW/maxH above that it grows with the window, up to {@link #AANDEEL_W} x {@link #LOS_H} of it.
     * Scaled up in whole real pixels ({@link #opschaal}) while the panel stays within {@link #LOS_MAX} of the window, scaled down
     * (at least x0.5) when the window is smaller than the panel. {@code top} = the top of the panel.
     */
    public static Kader los(int sw, int sh, int minW, int minH, int maxW, int maxH, int guiScale) {
        float past = Math.min((sw - 16) / (float) minW, (sh - 20) / (float) minH);
        float s = past >= 1f ? opschaal(sw, sh, Math.max(minW + 16, (int) Math.ceil(minW / LOS_MAX)), Math.max(minH + 20, (int) Math.ceil(minH / LOS_MAX)), guiScale)
                : Math.max(0.5f, past);
        int lw = (int) Math.floor(sw / s), lh = (int) Math.floor(sh / s);
        int w = Math.max(minW, Math.min(maxW, Math.min(lw - 16, (int) (lw * AANDEEL_W))));
        int h = Math.max(minH, Math.min(maxH, Math.min(lh - 20, (int) (lh * LOS_H))));
        return new Kader(s, lw, lh, (lw - w) / 2, Math.max(8, (lh - h) / 2), w, h);
    }
}
