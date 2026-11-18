package nl.juiced.guhs.feature.guhwinter.spellen.race;

import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Doek;

/**
 * Little pixel sprites for the Guhwinter screen games, written as rows of letters (one letter = one colour, '.' = empty)
 * and drawn as rectangles (a run of the same colour in a row is one rectangle), so they need no texture.
 */
public final class Pix {
    /** One sprite: its rows and its palette (letter -> colour). */
    public record Sprite(String[] rijen, String letters, int[] kleuren) {
        public int breed() {
            return rijen[0].length();
        }

        public int hoog() {
            return rijen.length;
        }

        int kleur(char c) {
            int i = letters.indexOf(c);
            return i < 0 ? 0 : kleuren[i];
        }
    }

    private static final String L_GUH = "kprRwbgfPs";
    private static final int[] K_GUH = {0xFF3A1E2E, 0xFFFF9EC4, 0xFFE0384C, 0xFFFF6A7E, 0xFFFFFFFF, 0xFF4A7BE0, 0xFFFFD34E, 0xFFFFF4C0,
            0xFFFFC2DA, 0xFF2A1A26};

    /** The Pakjesguh from behind: beret with a feather, a round pink back, white collar, a striped suit; two steps. */
    public static final Sprite GUH_A = new Sprite(new String[] {
            "......ff....",
            "...kkkkf....",
            "..krrRrkf...",
            ".krrrrrrk...",
            ".kkkkkkkk...",
            "kpppppppppk.",
            "kpPpppppppk.",
            "kppppppppsk.",
            "kwwwwwwwwwk.",
            "kbbggbbggbk.",
            "kbbggbbggbk.",
            ".kbggbbggk..",
            "..kkkkkkk...",
            "..kk...kk...",
            "..kk........"}, L_GUH, K_GUH);
    public static final Sprite GUH_B = new Sprite(new String[] {
            "......ff....",
            "...kkkkf....",
            "..krrRrkf...",
            ".krrrrrrk...",
            ".kkkkkkkk...",
            "kpppppppppk.",
            "kpPpppppppk.",
            "kppppppppsk.",
            "kwwwwwwwwwk.",
            "kbbggbbggbk.",
            "kbbggbbggbk.",
            ".kbggbbggk..",
            "..kkkkkkk...",
            "..kk...kk...",
            ".......kk..."}, L_GUH, K_GUH);
    /** Ducking: squashed flat under the washing line. */
    public static final Sprite GUH_BUK = new Sprite(new String[] {
            "....kkkkff..",
            "..kkrrRrrkf.",
            ".kkkkkkkkkk.",
            "kpPpppppppk.",
            "kwwwwwwwwwk.",
            "kbbggbbggbk.",
            ".kkkkkkkkk..",
            "..kk...kk..."}, L_GUH, K_GUH);
    /** Bumped into something: plop, on its back, beret off, dizzy. */
    public static final Sprite GUH_PLOF = new Sprite(new String[] {
            "..g.....g...",
            "...kkkkkk...",
            "..kppppppk..",
            ".kpkpppkpk..",
            ".kppkppkpk..",
            ".kpkpppkpk..",
            ".kppppppppk.",
            "kwwwwwwwwwk.",
            "kbbggbbggbk.",
            ".kkkkkkkkk..",
            "..........rr",
            ".........rrr"}, L_GUH, K_GUH);

    /** Ozovads, Sinterklaas' white horse, from behind (the guh sits on top). */
    public static final Sprite OZOVADS = new Sprite(new String[] {
            "....kk....kk....",
            "...kwwk..kwwk...",
            "...kwwwkkwwwk...",
            "..kwwwwwwwwwwk..",
            ".kwwwrrrrrrwwwk.",
            ".kwwrrgggggrwwk.",
            ".kwwrrrrrrrrwwk.",
            ".kwwwwwwwwwwwwk.",
            ".kWwwwwwwwwwwWk.",
            "..kwwwkssswwwk..",
            "..kwwk.sss.kwwk.",
            "..kWWk.sss.kWWk.",
            "..kkkk..s..kkkk."}, "kwWrgs", new int[] {0xFF3A1E2E, 0xFFFFFFFF, 0xFFD0D8E8, 0xFFE0384C, 0xFFFFD34E, 0xFFB8BCC8});

    /** A pepernoot: small, round, light brown with darker specks. */
    public static final Sprite PEPERNOOT = new Sprite(new String[] {
            ".hbb.",
            "hbbBb",
            "bbbbb",
            "bBbbB",
            ".bbb."}, "hbB", new int[] {0xFFE8B27A, 0xFFC98A4B, 0xFF8E5A2B});

    /** The chocoladeletter pick-up (the magnet): an S of chocolate with a golden edge. */
    public static final Sprite LETTER = new Sprite(new String[] {
            ".gggggg.",
            "gccccccg",
            "gccggggg",
            "gcccccg.",
            ".gccccg.",
            ".gggccg.",
            "gggggccg",
            "gccccccg",
            ".gggggg."}, "gc", new int[] {0xFFFFD34E, 0xFF5A3420});

    /** The Ozovads pick-up: a golden horseshoe. */
    public static final Sprite HOEFIJZER = new Sprite(new String[] {
            "gg....gg",
            "gG....Gg",
            "gg....gg",
            "gG....Gg",
            "gg....gg",
            ".gg..gg.",
            ".gGggGg.",
            "..gggg.."}, "gG", new int[] {0xFFFFD34E, 0xFFB08A2A});

    /** Draws a sprite with its top left corner at x, y. */
    public static void teken(Doek d, Sprite s, int x, int y) {
        for (int r = 0; r < s.rijen.length; r++) {
            String rij = s.rijen[r];
            int k = 0;
            while (k < rij.length()) {
                char c = rij.charAt(k);
                if (c == '.' || c == ' ') {
                    k++;
                    continue;
                }
                int start = k;
                while (k < rij.length() && rij.charAt(k) == c) {
                    k++;
                }
                int kleur = s.kleur(c);
                if (kleur != 0) {
                    d.rect(x + start, y + r, k - start, 1, kleur);
                }
            }
        }
    }

    private Pix() {
    }
}
