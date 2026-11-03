package nl.juiced.guhs.feature.erfenis.kaart.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The map's order carts (pixel art like {@link DorpIcoon}): a little wooden cart with sacks and eggs, wheels that turn while
 * it drives; the daily star cart is gold with a twinkling star. Plus the badges on the neighbour villages (open orders,
 * the star) and the dotted cart road between your village and a neighbour.
 */
final class KarIcoon {
    private static final String[] KAR = {
            "..e.z..",
            ".zezze.",
            "WwwwwwW",
            ".WwwwW.",
            ".o...o."};
    private static final String[] KAR2 = {   // (the wheels a quarter turn on)
            "..e.z..",
            ".zezze.",
            "WwwwwwW",
            ".WwwwW.",
            ".+...+."};
    private static final String[] STER = {
            "..y..",
            ".yyy.",
            "yyyyy",
            ".yyy.",
            ".y.y."};

    private KarIcoon() {
    }

    /** One cart centred on (sx, sy); {@code links}: driving to the left (mirrored); {@code rijdt}: wheels turn and it bobs. */
    static void teken(GuiGraphicsExtractor g, int sx, int sy, int u, boolean ster, boolean links, boolean rijdt, long tijd) {
        String[] p = rijdt && (tijd / 160) % 2 == 0 ? KAR2 : KAR;
        int w = 7, h = 5;
        int x0 = sx - w * u / 2, y0 = sy - h * u / 2 - (rijdt && (tijd / 240) % 2 == 0 ? Math.max(1, u / 2) : 0);
        g.fill(x0, y0 + h * u, x0 + w * u, y0 + h * u + Math.max(1, u / 2), 0x50302040);   // shadow
        int hout = ster ? 0xFFF2C14E : 0xFFC08A5A, houtDonker = ster ? 0xFFB8862A : 0xFF8A5A3C;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char c = p[y].charAt(links ? w - 1 - x : x);
                int k = switch (c) {
                    case 'w' -> hout;
                    case 'W' -> houtDonker;
                    case 'o', '+' -> 0xFF5A4660;
                    case 'e' -> 0xFFFFF4E0;
                    case 'z' -> ster ? 0xFFFFE27A : 0xFFE8C9A0;
                    default -> 0;
                };
                if (k != 0) {
                    g.fill(x0 + x * u, y0 + y * u, x0 + (x + 1) * u, y0 + (y + 1) * u, k);
                }
            }
        }
        if (ster && (tijd / 300) % 3 != 0) {
            int su = Math.max(1, u / 2);
            patroon(g, STER, sx - 5 * su / 2, y0 - 6 * su, su, 0xFFFFD54A);
        }
    }

    /** A gold star badge (the village with today's star order), centred on (sx, sy). */
    static void sterBadge(GuiGraphicsExtractor g, int sx, int sy, int u, long tijd) {
        int r = 5 * u / 2 + 1;
        g.fill(sx - r, sy - r, sx + r + 1, sy + r + 1, 0xC0302040);
        patroon(g, STER, sx - 5 * u / 2, sy - 5 * u / 2, u, (tijd / 400) % 2 == 0 ? 0xFFFFD54A : 0xFFFFE89A);
    }

    /** A round pink badge with a number (open orders of that neighbour). */
    static void getalBadge(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int sx, int sy, int n) {
        String t = Integer.toString(n);
        int w = Math.max(9, font.width(t) + 4);
        g.fill(sx - w / 2, sy - 4, sx + w / 2 + 1, sy + 6, 0xFF7A2848);
        g.fill(sx - w / 2 + 1, sy - 5, sx + w / 2, sy + 7, 0xFF7A2848);
        g.fill(sx - w / 2 + 1, sy - 4, sx + w / 2, sy + 6, 0xFFF26E9A);
        g.text(font, t, sx - font.width(t) / 2 + 1, sy - 3, 0xFFFFFFFF, false);
    }

    /** A dotted road from (x0, y0) to (x1, y1). */
    static void weg(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int kleur) {
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
        int stappen = (int) (len / 4);
        for (int i = 2; i < stappen - 1; i++) {
            if (i % 2 == 0) {
                continue;
            }
            int x = Math.round(x0 + dx * i / stappen), y = Math.round(y0 + dy * i / stappen);
            g.fill(x - 1, y - 1, x + 1, y + 1, kleur);
        }
    }

    private static void patroon(GuiGraphicsExtractor g, String[] p, int x0, int y0, int u, int kleur) {
        for (int y = 0; y < p.length; y++) {
            for (int x = 0; x < p[y].length(); x++) {
                if (p[y].charAt(x) != '.') {
                    g.fill(x0 + x * u, y0 + y * u, x0 + (x + 1) * u, y0 + (y + 1) * u, kleur);
                }
            }
        }
    }
}
