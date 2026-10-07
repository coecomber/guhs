package nl.juiced.guhs.feature.guhpixel.guhkade.spel;

/**
 * A 3 x 5 pixel font made of rectangles (digits, capitals and a few signs), so the cabinet's screen can show a score and
 * a few words without a real font: the same on the game screen and on the block.
 */
public final class PixelFont {
    private static final String TEKENS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-!?.:+ ";
    /** Per glyph 15 bits, row by row from the top, the left pixel first. (The O is square like the 0: a round one read as a smudge with its shadow.) */
    private static final int[] GLYPHS = {
            0b111_101_101_101_111, 0b010_110_010_010_111, 0b111_001_111_100_111, 0b111_001_111_001_111, 0b101_101_111_001_001,
            0b111_100_111_001_111, 0b111_100_111_101_111, 0b111_001_010_010_010, 0b111_101_111_101_111, 0b111_101_111_001_111,
            0b010_101_111_101_101, 0b110_101_110_101_110, 0b011_100_100_100_011, 0b110_101_101_101_110, 0b111_100_110_100_111,
            0b111_100_110_100_100, 0b011_100_101_101_011, 0b101_101_111_101_101, 0b111_010_010_010_111, 0b001_001_001_101_010,
            0b101_101_110_101_101, 0b100_100_100_100_111, 0b101_111_111_101_101, 0b110_101_101_101_101, 0b111_101_101_101_111,
            0b110_101_110_100_100, 0b010_101_101_111_011, 0b110_101_110_101_101, 0b011_100_010_001_110, 0b111_010_010_010_010,
            0b101_101_101_101_111, 0b101_101_101_101_010, 0b101_101_111_111_101, 0b101_101_010_101_101, 0b101_101_010_010_010,
            0b111_001_010_100_111, 0b000_000_111_000_000, 0b010_010_010_000_010, 0b111_001_010_000_010, 0b000_000_000_000_010,
            0b000_010_000_010_000, 0b000_010_111_010_000, 0b000_000_000_000_000};

    /** The width of a text in game pixels at this size (schaal 1 = 3 x 5 letters, one pixel apart). */
    public static int breedte(String tekst, int schaal) {
        return tekst.isEmpty() ? 0 : (tekst.length() * 4 - 1) * schaal;
    }

    public static void teken(Doek d, String tekst, int x, int y, int schaal, int argb) {
        for (int i = 0; i < tekst.length(); i++) {
            int n = TEKENS.indexOf(Character.toUpperCase(tekst.charAt(i)));
            if (n < 0) {
                n = TEKENS.indexOf('?');
            }
            int g = GLYPHS[n];
            for (int rij = 0; rij < 5; rij++) {
                // (a run of pixels in a row becomes one rectangle)
                int kol = 0;
                while (kol < 3) {
                    if ((g >> (14 - rij * 3 - kol) & 1) == 0) {
                        kol++;
                        continue;
                    }
                    int start = kol;
                    while (kol < 3 && (g >> (14 - rij * 3 - kol) & 1) != 0) {
                        kol++;
                    }
                    d.rect(x + (i * 4 + start) * schaal, y + rij * schaal, (kol - start) * schaal, schaal, argb);
                }
            }
        }
    }

    /** Centred on x, with a shadow. */
    public static void midden(Doek d, String tekst, int x, int y, int schaal, int argb) {
        int links = x - breedte(tekst, schaal) / 2;
        teken(d, tekst, links + schaal, y + schaal, schaal, 0xFF2A1430);
        teken(d, tekst, links, y, schaal, argb);
    }

    private PixelFont() {
    }
}
