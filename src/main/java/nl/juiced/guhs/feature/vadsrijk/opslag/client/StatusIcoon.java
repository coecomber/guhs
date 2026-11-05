package nl.juiced.guhs.feature.vadsrijk.opslag.client;

import java.util.Map;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;

/**
 * T-OPSLAG: little pixel icons for the statuses (no textures needed: an 8x8 glyph per status, drawn with fills, so they stay
 * sharp at every GUI scale). A status of a later slice without a glyph gets its first letter.
 */
public final class StatusIcoon {
    private static final Map<Status, String[]> GLYPH = Map.of(
            Status.BRANDEND, new String[] {"...#....", "..##....", "..###.#.", ".#####..", ".######.", "########", ".######.", "..####.."},
            Status.GESPOREN, new String[] {"...#....", "...#....", "..###...", "..###...", ".#####..", ".##.##..", ".#####..", "..###..."},
            Status.PLAKKERIG, new String[] {"........", "..####..", ".######.", "########", "########", "#.#..#.#", "#.#..#..", "..#....."},
            Status.VERDOOFD, new String[] {"#...#...", ".#.#....", "..#..#.#", ".#.#..#.", "#...##.#", "...#....", "..#.#...", ".#...#.."},
            Status.VAHOEG, new String[] {"...##...", "..####..", ".######.", "########", "...##...", "...##...", "...##...", "...##..."},
            Status.KNUFFELSCHILD, new String[] {"########", "#......#", "#.#..#.#", "#.####.#", "#..##..#", ".#....#.", "..#..#..", "...##..."},
            Status.SNELPOOTJES, new String[] {".#...#..", "###.###.", ".#...#..", "........", "..###...", ".#####..", ".#####..", "..###..."},
            Status.DUIZELIG, new String[] {".#####..", "#.....#.", "#.###.#.", "#.#.#.#.", "#.#...#.", "#.####..", "#.......", ".######."},
            Status.BEVROREN, new String[] {"...#....", ".#.#.#..", "..###...", "#######.", "..###...", ".#.#.#..", "...#....", "........"});
    private static final Map<Status, Integer> KLEUR = Map.of(
            Status.BRANDEND, 0xFFFF8A2A, Status.GESPOREN, 0xFF7AD05A, Status.PLAKKERIG, 0xFFB6E36A, Status.VERDOOFD, 0xFFFFE27A,
            Status.VAHOEG, 0xFFFF5A7A, Status.KNUFFELSCHILD, 0xFFFF9AD5, Status.SNELPOOTJES, 0xFF8CE0FF, Status.DUIZELIG, 0xFFB98CFF,
            Status.BEVROREN, 0xFF9EE3FF);

    private StatusIcoon() {
    }

    public static int kleur(Status s) {
        return KLEUR.getOrDefault(s, 0xFFFFFFFF);
    }

    /**
     * One icon: a z x z tile (z >= 10) with a buff (green) / debuff (red) rim, the glyph, a dark sweep for the time used up
     * ({@code rest} 0..1 = part still left) and, when {@code seconden} >= 0, the seconds left in small print.
     */
    public static void teken(GuiGraphicsExtractor g, Font f, Status s, int x, int y, int z, float rest, int seconden) {
        g.fill(x - 1, y - 1, x + z + 1, y + z + 1, s.buff ? 0xFF2E7A3A : 0xFF8A2A3A);
        g.fill(x, y, x + z, y + z, 0xE0201418);
        String[] gl = GLYPH.get(s);
        int k = kleur(s);
        if (gl != null) {
            int px = Math.max(1, (z - 2) / 8), ox = x + (z - 8 * px) / 2, oy = y + (z - 8 * px) / 2;
            for (int r = 0; r < 8; r++) {
                String rij = gl[r];
                for (int c = 0; c < 8; c++) {
                    if (rij.charAt(c) == '#') {
                        g.fill(ox + c * px, oy + r * px, ox + (c + 1) * px, oy + (r + 1) * px, k);
                    }
                }
            }
        } else {
            String l = s.name().substring(0, 1);
            g.text(f, l, x + z / 2 - f.width(l) / 2, y + (z - 8) / 2, k, true);
        }
        if (rest < 1) {
            int weg = (int) (z * (1 - Math.max(0, rest)));
            g.fill(x, y, x + z, y + weg, 0x90000000);
        }
        if (seconden >= 0) {
            String t = seconden >= 60 ? (seconden / 60) + "m" : String.valueOf(seconden);
            g.pose().pushMatrix();
            g.pose().translate(x + z + 0.5f, y + z - 3.5f);
            g.pose().scale(0.5f, 0.5f);
            g.text(f, t, -f.width(t), 0, 0xFFFFFFFF, true);
            g.pose().popMatrix();
        }
    }
}
