package nl.juiced.guhs.feature.erfenis.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** De Erfenis (POC): the overview's little drawing kit in the guh palette (cream, pink, plum): rounded panels, buttons, bars. */
final class Teken {
    static final int CREME = 0xF2FFF6FA, CREME_VOL = 0xFFFFF6FA, ROZE = 0xFFF7B6CB, ROZE_DONKER = 0xFFD27A9C, DONKER = 0xFF7A2848,
            TEKST = 0xFF5A3448, ZACHT = 0xFFB0708A, GROEN = 0xFF6CC08A, GROEN_DONKER = 0xFF3F9A62, ROOD = 0xFFE06C7E, GRIJS = 0xFFC9B9C1,
            GOUD = 0xFFF2C14E, WIT = 0xFFFFFFFF;

    private Teken() {
    }

    /** A filled rectangle with the corners rounded off by r pixels (1..3). */
    static void rond(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int kleur, int r) {
        r = Math.max(0, Math.min(r, 4));
        if (x1 - x0 < 2 * r || y1 - y0 < 2 * r) {
            g.fill(x0, y0, x1, y1, kleur);
            return;
        }
        g.fill(x0 + r, y0, x1 - r, y1, kleur);
        int[] in = INSPRONG[Math.min(r, INSPRONG.length - 1)];
        for (int i = 0; i < r; i++) {
            g.fill(x0 + i, y0 + in[i], x0 + i + 1, y1 - in[i], kleur);
            g.fill(x1 - i - 1, y0 + in[i], x1 - i, y1 - in[i], kleur);
        }
    }

    /** Per corner radius: how far each side column starts in (a pixel-art circle). */
    private static final int[][] INSPRONG = {{}, {1}, {2, 1}, {3, 1, 1}, {4, 2, 1, 1}};

    /** A cream panel with a pink border and a soft shadow. */
    static void paneel(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        rond(g, x0 + 1, y0 + 2, x1 + 1, y1 + 2, 0x40501830, 4);
        rond(g, x0, y0, x1, y1, ROZE_DONKER, 4);
        rond(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, CREME_VOL, 3);
    }

    /** A small chip panel (the top bar). */
    static void chip(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        rond(g, x0, y0 + 1, x1, y1 + 1, 0x40501830, 3);
        rond(g, x0, y0, x1, y1, ROZE, 3);
        rond(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, CREME, 3);
    }

    /** A button: pink (or the given colour), lighter under the mouse, grey when it can't. Returns whether the mouse is on it. */
    static boolean knop(GuiGraphicsExtractor g, Font font, Component tekst, int x0, int y0, int x1, int y1, int mx, int my, boolean kan, int kleur) {
        boolean op = mx >= x0 && mx < x1 && my >= y0 && my < y1;
        int basis = !kan ? GRIJS : kleur;
        int rand = !kan ? 0xFFA898A0 : donkerder(basis);
        rond(g, x0, y0 + 1, x1, y1 + 1, rand, 3);
        rond(g, x0, y0, x1, y1, op && kan ? lichter(basis) : basis, 3);
        g.fill(x0 + 3, y0 + 1, x1 - 3, y0 + 2, 0x50FFFFFF);
        int tw = font.width(tekst);
        g.text(font, tekst, (x0 + x1 - tw) / 2, (y0 + y1 - 8) / 2 + 1, kan ? WIT : 0xFFF4EEF1, kan);
        return op;
    }

    /** A progress bar: track and fill. */
    static void balk(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, float deel, int kleur) {
        rond(g, x0, y0, x1, y1, 0xFFEAD3DD, 1);
        int w = Math.round((x1 - x0 - 2) * Math.max(0, Math.min(1, deel)));
        if (w > 0) {
            rond(g, x0 + 1, y0 + 1, x0 + 1 + w, y1 - 1, kleur, 1);
        }
    }

    static int lichter(int argb) {
        int r = argb >> 16 & 0xFF, gg = argb >> 8 & 0xFF, b = argb & 0xFF;
        return argb & 0xFF000000 | Math.min(255, r + 24) << 16 | Math.min(255, gg + 24) << 8 | Math.min(255, b + 24);
    }

    static int donkerder(int argb) {
        int r = argb >> 16 & 0xFF, gg = argb >> 8 & 0xFF, b = argb & 0xFF;
        return argb & 0xFF000000 | r * 3 / 4 << 16 | gg * 3 / 4 << 8 | b * 3 / 4;
    }

    /** m:ss (or h:mm:ss) of a number of millis, rounded up. */
    static String tijd(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        if (s >= 3600) {
            return String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
        }
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
