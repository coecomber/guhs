package nl.juiced.guhs.feature.erfenis.kaart.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import nl.juiced.guhs.feature.erfenis.kaart.KaartDorp;

/**
 * The little pixel-art village icons of the map, by Hoeve tier (2 levels each): T1 a hutje, T2 a huisje, T3 a dorpje
 * (two houses), T4 a torendorp, T5 a kasteel. Roofs in the village's flag colour, a pedestal in the marker colour (own / player / NPC),
 * a flag from level 2. Far out (zoom &lt; 10) a village is a small coloured dot.
 */
final class DorpIcoon {
    private static final String[] KLEIN = {
            ".ooo.",
            "orrro",
            "oRRRo",
            "owdwo",
            "ooooo"};
    private static final String[] HUT = {
            "..ooo..",
            ".orrro.",
            "orrrrro",
            "oRRRRRo",
            ".owbwo.",
            ".owdwo.",
            ".ooooo."};
    private static final String[] TOREN = {
            "..o..",
            ".oro.",
            "orrro",
            "oRRRo",
            ".sSs.",
            ".sbs.",
            ".sSs.",
            ".sds.",
            ".ooo."};
    private static final String[] KASTEEL = {
            ".o.......o.",
            "oro.....oro",
            "oRo.ooo.oRo",
            "sSsorrrosSs",
            "sbsoRRRosbs",
            "sSswwwwwsSs",
            "sSswbdbwsSs",
            "sSswwdwwsSs",
            "SSSSSSSSSSS"};

    /** One part of a composed icon: a pattern at an offset (in icon pixels). */
    private record Deel(String[] patroon, int ox, int oy) {
    }

    private static final Deel[][] PER_LEVEL = {
            {new Deel(KLEIN, 0, 0)},
            {new Deel(HUT, 0, 0)},
            {new Deel(HUT, 0, 0), new Deel(KLEIN, 6, 3)},
            {new Deel(TOREN, 0, 0), new Deel(HUT, 4, 2)},
            {new Deel(KASTEEL, 0, 0)}};
    private static final int[][] MAAT = {{5, 5}, {7, 7}, {11, 8}, {11, 9}, {11, 9}};

    private DorpIcoon() {
    }

    private static int lvl(KaartDorp d) {
        return (Math.max(1, Math.min(10, d.hoeve())) + 1) / 2 - 1;   // (the Hoeve's tier: 2 levels per icon)
    }

    private static int eenheid(float zoom) {
        return zoom < 10 ? 0 : zoom < 20 ? 1 : 2;
    }

    /** Half the icon's height on screen (where the name label goes). */
    static int halveHoogte(KaartDorp d, float zoom) {
        int u = eenheid(zoom);
        return u == 0 ? 3 : (MAAT[lvl(d)][1] + 2) * u / 2 + u;
    }

    static void teken(GuiGraphicsExtractor g, KaartDorp d, int sx, int sy, float zoom, int soort, boolean eigen, boolean gekozen,
            boolean hover, long tijd) {
        int u = eenheid(zoom);
        boolean knipper = (tijd / 400) % 2 == 0;
        if (u == 0) {   // far out: a dot
            int s = 1 + (lvl(d) + 2) / 2;
            if (eigen || gekozen || hover) {
                int r = s + 2 + (eigen && knipper ? 1 : 0);
                g.fill(sx - r, sy - r, sx + r + 1, sy + r + 1, gekozen ? 0xFFFFFFFF : soort);
            }
            g.fill(sx - s - 1, sy - s - 1, sx + s + 2, sy + s + 2, 0xFF5A4660);
            g.fill(sx - s, sy - s, sx + s + 1, sy + s + 1, soort);
            if (s >= 2) {
                g.fill(sx - s + 1, sy - s + 1, sx + s, sy + s, d.kleur());
            }
            return;
        }
        int l = lvl(d);
        int w = MAAT[l][0], h = MAAT[l][1];
        int x0 = sx - w * u / 2, y0 = sy - h * u / 2;
        // shadow, pedestal in the marker colour
        g.fill(x0 - u, y0 + h * u, x0 + (w + 1) * u, y0 + (h + 1) * u + u, 0x50302040);
        g.fill(x0 - u, y0 + h * u - u, x0 + (w + 1) * u, y0 + h * u + u, soort);
        if (eigen) {   // your own village glows a little
            int r = (knipper ? 3 : 2) * u;
            omlijn(g, x0 - r, y0 - r, x0 + w * u + r, y0 + h * u + r + u, 0xC0FFE27A);
        }
        int kleur = d.kleur(), donker = KaartLand.mix(kleur, 0xFF302040, 0.28f);
        for (Deel deel : PER_LEVEL[l]) {
            patroon(g, deel.patroon(), x0 + deel.ox() * u, y0 + deel.oy() * u, u, kleur, donker);
        }
        if (d.hoeve() >= 3) {   // a flag on top, right
            int fx = x0 + (w - 2) * u, fy = y0 - 4 * u;
            g.fill(fx, fy, fx + u, y0 + u, 0xFF5A4660);
            g.fill(fx + u, fy, fx + 4 * u, fy + 2 * u, kleur);
            g.fill(fx + u, fy + 2 * u, fx + 4 * u, fy + 2 * u + Math.max(1, u / 2), donker);
        }
        if (d.nieuw() && knipper) {   // a sparkle: new or just grew
            int nx = x0 - u, ny = y0 - u;
            g.fill(nx - u, ny, nx + 2 * u, ny + u, 0xFFFFF27A);
            g.fill(nx, ny - u, nx + u, ny + 2 * u, 0xFFFFF27A);
        }
        if (gekozen || hover) {
            int r = 2 * u + 1;
            omlijn(g, x0 - r, y0 - r - 3 * u, x0 + w * u + r, y0 + h * u + r + u, gekozen ? (knipper ? 0xFFFFFFFF : 0xFFFFE27A) : 0xB0FFFFFF);
        }
    }

    private static void omlijn(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int c) {
        g.fill(x0, y0, x1, y0 + 1, c);
        g.fill(x0, y1 - 1, x1, y1, c);
        g.fill(x0, y0, x0 + 1, y1, c);
        g.fill(x1 - 1, y0, x1, y1, c);
    }

    private static void patroon(GuiGraphicsExtractor g, String[] p, int x0, int y0, int u, int dak, int dakDonker) {
        for (int y = 0; y < p.length; y++) {
            String rij = p[y];
            for (int x = 0; x < rij.length(); x++) {
                int c = switch (rij.charAt(x)) {
                    case 'o' -> 0xFF5A4660;
                    case 'r' -> dak;
                    case 'R' -> dakDonker;
                    case 'w' -> 0xFFFFF4E0;
                    case 'd' -> 0xFF8A5A3C;
                    case 'b' -> 0xFF8CC8F0;
                    case 's' -> 0xFFEFEAF6;
                    case 'S' -> 0xFFC4BAD6;
                    case 'g' -> 0xFFFFD54A;
                    default -> 0;
                };
                if (c != 0) {
                    g.fill(x0 + x * u, y0 + y * u, x0 + (x + 1) * u, y0 + (y + 1) * u, c);
                }
            }
        }
    }
}
