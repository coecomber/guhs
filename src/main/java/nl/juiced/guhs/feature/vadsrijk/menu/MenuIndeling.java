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
     * panel then works in a LOGICAL screen of {@code lw x lh} (= sw/schaal x sh/schaal) and is drawn scaled down.
     * left/top/w/h = the panel (title band included); the main tab row sits above top.
     */
    public record Kader(float schaal, int lw, int lh, int left, int top, int w, int h) {
    }

    public static Kader kader(int sw, int sh) {
        return kader(sw, sh, MIN_W, MIN_H);
    }

    /** The frame for a panel that needs at least minW x minH (e.g. a tall skill tree): bigger than the others only then. */
    public static Kader kader(int sw, int sh, int minW, int minH) {
        int mw = Math.max(MIN_W, minW), mh = Math.max(MIN_H, minH), xw = Math.max(MAX_W, mw), xh = Math.max(MAX_H, mh);
        int nodigW = mw + 2 * MARGE, nodigH = mh + TABS_H + 2 * MARGE + 4;
        float s = Math.min(1f, Math.min(sw / (float) nodigW, sh / (float) nodigH));
        int lw = (int) Math.floor(sw / s), lh = (int) Math.floor(sh / s);
        int w = Math.max(mw, Math.min(xw, lw - 2 * MARGE - 8));
        int h = Math.max(mh, Math.min(xh, lh - 2 * MARGE - TABS_H - 4));
        int left = (lw - w) / 2;
        int top = Math.max(MARGE + TABS_H + 2, (lh - h + TABS_H) / 2);
        return new Kader(s, lw, lh, left, top, w, h);
    }
}
