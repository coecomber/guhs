package nl.juiced.guhs.feature.vadsrijk.toetsen;

import java.util.ArrayList;
import java.util.List;

/**
 * U-TOETSEN: the layout of the key overview (pure maths, no client classes, so a gametest can check it). Given the lines (group
 * headers + key lines, each with its natural width) and the free room right of the pause buttons, it picks a text scale and a
 * number of columns so everything fits. Order of tries: scale 1 / 0.75 / 0.5 (never below {@code minSchaal}), each with 1, 2, 3
 * columns; first without cutting text, then with cut descriptions (column at least {@link #MIN_KOLOM} wide). Nothing fits =
 * {@code null}: the caller shows the "Toetsen ▸" button instead.
 * <p>All sizes in the result are VIRTUAL (unscaled) pixels relative to the panel's top-left; screen = panel + virtual * schaal.
 */
public final class ToetsenIndeling {
    public static final int RAND = 5;          // inner padding
    public static final int TITEL_H = 13;      // the title line
    public static final int VOET_H = 11;       // the "klik = wijzigen" line
    public static final int KOP_H = 13;        // a group header
    public static final int REGEL_H = 11;      // a key line
    public static final int KOLOM_GAT = 8;     // gap between columns
    public static final int MIN_KOLOM = 96;    // a column is never narrower (descriptions get cut)
    public static final float[] SCHALEN = {1f, 0.75f, 0.5f};

    /** One line: a group header ({@code kop}) or a key; {@code breedte} = its natural width without cutting. */
    public record Regel(boolean kop, int breedte) {
    }

    /** Where line i goes: column + virtual x/y inside the panel. */
    public record Plek(int kolom, int x, int y) {
    }

    /** The chosen layout. {@code breedte}/{@code hoogte} = the whole panel in virtual px; {@code kolomBreedte} per column. */
    public record Uitkomst(float schaal, int kolommen, int kolomBreedte, int breedte, int hoogte, boolean gesneden, List<Plek> plekken) {
        public int schermBreedte() {
            return (int) Math.ceil(breedte * schaal);
        }

        public int schermHoogte() {
            return (int) Math.ceil(hoogte * schaal);
        }
    }

    private ToetsenIndeling() {
    }

    private static int hoogte(Regel r) {
        return r.kop() ? KOP_H : REGEL_H;
    }

    /** The best layout in a box of {@code maxB} x {@code maxH} screen px, or null when nothing fits. */
    public static Uitkomst kies(List<Regel> regels, int maxB, int maxH, float minSchaal, int titelBreedte) {
        if (regels.isEmpty() || maxB <= 0 || maxH <= 0) {
            return null;
        }
        int breedst = titelBreedte;
        for (Regel r : regels) {
            breedst = Math.max(breedst, r.breedte());
        }
        for (boolean snijden : new boolean[] {false, true}) {
            for (float s : SCHALEN) {
                if (s < minSchaal - 1e-4f) {
                    continue;
                }
                int vb = (int) Math.floor(maxB / s);
                int vh = (int) Math.floor(maxH / s);
                for (int k = 1; k <= 3; k++) {
                    int ruimte = (vb - 2 * RAND - (k - 1) * KOLOM_GAT) / k;
                    int kolomB = snijden ? Math.min(breedst, ruimte) : breedst;
                    if (kolomB > ruimte || kolomB < (snijden ? MIN_KOLOM : 1)) {
                        continue;
                    }
                    List<Plek> plekken = verdeel(regels, k, kolomB, vh - 2 * RAND - TITEL_H - VOET_H);
                    if (plekken == null) {
                        continue;
                    }
                    int gebruikt = 1, inhoud = 0;
                    for (int i = 0; i < plekken.size(); i++) {
                        Plek p = plekken.get(i);
                        gebruikt = Math.max(gebruikt, p.kolom() + 1);
                        inhoud = Math.max(inhoud, p.y() + hoogte(regels.get(i)) - RAND - TITEL_H);
                    }
                    int b = 2 * RAND + gebruikt * kolomB + (gebruikt - 1) * KOLOM_GAT;
                    b = Math.max(b, Math.min(vb, titelBreedte + 2 * RAND));
                    int h = 2 * RAND + TITEL_H + inhoud + VOET_H;
                    return new Uitkomst(s, gebruikt, kolomB, b, h, kolomB < breedst, plekken);
                }
            }
        }
        return null;
    }

    /**
     * Fills the columns top to bottom; a header never ends a column (it moves along with its first key). The columns are balanced:
     * the column height is the smallest that still fits everything in {@code k} columns. Null if it does not fit.
     */
    static List<Plek> verdeel(List<Regel> regels, int k, int kolomB, int maxInhoud) {
        int totaal = 0;
        for (Regel r : regels) {
            totaal += hoogte(r);
        }
        if (maxInhoud <= 0) {
            return null;
        }
        // balance: try growing column heights from total/k up to the max
        for (int hoog = Math.min(maxInhoud, (totaal + k - 1) / k); hoog <= maxInhoud; hoog++) {
            List<Plek> uit = probeer(regels, k, kolomB, hoog);
            if (uit != null) {
                return uit;
            }
        }
        return null;
    }

    private static List<Plek> probeer(List<Regel> regels, int k, int kolomB, int hoog) {
        List<Plek> uit = new ArrayList<>();
        int kolom = 0, y = 0;
        for (int i = 0; i < regels.size(); i++) {
            Regel r = regels.get(i);
            int h = hoogte(r);
            int nodig = h;
            if (r.kop() && i + 1 < regels.size() && !regels.get(i + 1).kop()) {
                nodig += REGEL_H;   // keep the header with its first key
            }
            if (y > 0 && y + nodig > hoog) {
                kolom++;
                y = 0;
            }
            if (kolom >= k || y + h > hoog) {
                return null;
            }
            uit.add(new Plek(kolom, RAND + kolom * (kolomB + KOLOM_GAT), RAND + TITEL_H + y));
            y += h;
        }
        return uit;
    }
}
