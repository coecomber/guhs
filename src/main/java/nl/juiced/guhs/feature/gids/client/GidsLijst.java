package nl.juiced.guhs.feature.gids.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A scrolling list of rows of different heights inside a box (2.9, the gids screens): the mouse wheel and the scroll bar
 * on the right move it, rows are clipped at the edges, a row can take a click and give a tooltip. The rows draw
 * themselves ({@link Regel}); the list only knows their heights.
 */
public final class GidsLijst {
    /** How far one notch of the mouse wheel scrolls. */
    public static final int STAP = 18;
    public static final int BALK = 5;

    /** One row: its height, how it draws, what a click does, its tooltip (all relative to its own top-left corner). */
    public interface Regel {
        int hoogte();

        void teken(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY, boolean hover);

        /** A click at (mx, my) inside the row; true = handled. */
        default boolean klik(double mx, double my, int x, int y, int w) {
            return false;
        }

        /** The tooltip at the mouse (null: none). */
        @Nullable
        default List<Component> tip(double mx, double my, int x, int y, int w) {
            return null;
        }
    }

    private final List<Regel> regels = new ArrayList<>();
    private int x, y, w, h;
    private double scroll;
    private boolean slepen;
    /** True while totaal() measures the rows. */
    private boolean meten;

    public void plaats(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        scroll = Mth.clamp(scroll, 0, max());
    }

    /** New rows; the scroll position stays (as far as it still fits). */
    public void zet(List<Regel> nieuw) {
        regels.clear();
        regels.addAll(nieuw);
        scroll = Mth.clamp(scroll, 0, max());
    }

    public List<Regel> regels() {
        return regels;
    }

    public int totaal() {
        if (meten) {
            return 0;   // (a row asked rijBreedte() while being measured: see rijBreedte)
        }
        meten = true;
        try {
            int t = 0;
            for (Regel r : regels) {
                t += r.hoogte();
            }
            return t;
        } finally {
            meten = false;
        }
    }

    public int max() {
        return Math.max(0, totaal() - h);
    }

    public double scroll() {
        return scroll;
    }

    public void scrollNaar(double to) {
        scroll = Mth.clamp(to, 0, max());
    }

    /** The width the rows get (the scroll bar takes the right edge when it's needed). */
    public int rijBreedte() {
        if (meten) {
            // rows that wrap text ask for the width while they are being measured: measure them as if the scroll bar
            // is there (never narrower than drawn, so wrapped text always fits; no endless max() -> hoogte() loop)
            return w - BALK - 3;
        }
        return max() > 0 ? w - BALK - 3 : w;
    }

    public boolean binnen(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void teken(GuiGraphics g, int mouseX, int mouseY, int balkKleur, int balkAchter) {
        int rw = rijBreedte();
        boolean muisErin = binnen(mouseX, mouseY);
        g.enableScissor(x, y, x + w, y + h);
        int ry = y - (int) Math.round(scroll);
        for (Regel r : regels) {
            int rh = r.hoogte();
            if (ry + rh > y && ry < y + h) {
                boolean hover = muisErin && mouseX < x + rw && mouseY >= ry && mouseY < ry + rh;
                r.teken(g, x, ry, rw, mouseX, mouseY, hover);
            }
            ry += rh;
        }
        g.disableScissor();
        if (max() > 0) {
            int bx = x + w - BALK;
            g.fill(bx, y, bx + BALK, y + h, balkAchter);
            int knop = Math.max(14, h * h / Math.max(1, totaal()));
            int ky = y + (int) Math.round((h - knop) * (scroll / max()));
            g.fill(bx, ky, bx + BALK, ky + knop, balkKleur);
            g.fill(bx, ky, bx + 1, ky + knop, 0x40FFFFFF);
        }
    }

    /** The mouse wheel. */
    public boolean wiel(double mx, double my, double delta) {
        if (!binnen(mx, my) || max() == 0) {
            return false;
        }
        scrollNaar(scroll - delta * STAP);
        return true;
    }

    public boolean klik(double mx, double my, int button) {
        if (!binnen(mx, my) || button != 0) {
            return false;
        }
        if (max() > 0 && mx >= x + w - BALK - 1) {
            slepen = true;
            sleep(my);
            return true;
        }
        int ry = y - (int) Math.round(scroll);
        for (Regel r : regels) {
            int rh = r.hoogte();
            if (my >= ry && my < ry + rh) {
                return r.klik(mx, my, x, ry, rijBreedte());
            }
            ry += rh;
        }
        return false;
    }

    public boolean sleep(double my) {
        if (!slepen) {
            return false;
        }
        int knop = Math.max(14, h * h / Math.max(1, totaal()));
        double frac = (my - y - knop / 2.0) / Math.max(1, h - knop);
        scrollNaar(frac * max());
        return true;
    }

    public void los() {
        slepen = false;
    }

    @Nullable
    public List<Component> tip(double mx, double my) {
        if (!binnen(mx, my) || mx >= x + rijBreedte()) {
            return null;
        }
        int ry = y - (int) Math.round(scroll);
        for (Regel r : regels) {
            int rh = r.hoogte();
            if (my >= ry && my < ry + rh) {
                return r.tip(mx, my, x, ry, rijBreedte());
            }
            ry += rh;
        }
        return null;
    }
}
