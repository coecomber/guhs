package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The dance steps of a hula song (3.0): every step sits exactly on the song's beat grid (a whole beat, or an eighth on
 * lastig). Made from the song's sections (the same bars as tools/remix/make_hula.py), one pattern of eight eighths per bar:
 * {@code L} hips left (A), {@code R} hips right (D), {@code U} arms up (W), {@code D} down the knees (S), {@code *} VAHOEG!
 * (space), {@code .} nothing. Makkelijk has only the hips, medium all four, lastig eighths and the VAHOEG!s on the shouts.
 * The same on both sides (the dancer's game shows it, the server judges it).
 */
public final class HulaKaart {
    /** One dance step. */
    public enum Pas {
        LINKS("L", "←"), RECHTS("R", "→"), OMHOOG("U", "↑"), OMLAAG("D", "↓"), VAHOEG("*", "★");

        public final String code, pijl;

        Pas(String code, String pijl) {
            this.code = code;
            this.pijl = pijl;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Pas van(char c) {
            for (Pas p : values()) {
                if (p.code.charAt(0) == c) {
                    return p;
                }
            }
            return null;
        }
    }

    /** One step: its number, its beat (fractional on the eighths) and what to do. */
    public record Noot(int index, double beat, Pas pas) {
    }

    /** A part of the song: from bar `van` (inclusive) to bar `tot` (exclusive), its bars' patterns in turn. */
    private record Deel(int van, int tot, String... patronen) {
    }

    private static final Map<HulaLiedje, List<Noot>> KAARTEN = new EnumMap<>(HulaLiedje.class);

    /** The steps of a song, in time order (the same list every time). */
    public static synchronized List<Noot> van(HulaLiedje liedje) {
        return KAARTEN.computeIfAbsent(liedje, l -> List.copyOf(maak(l)));
    }

    private static List<Deel> delen(HulaLiedje liedje) {
        return switch (liedje) {
            // intro 0-3, verse 4-11, chorus 12-19, steel solo 20-23, chorus 24-29, outro 30-31, the last chord at beat 128
            case ALOHA_NJEG -> List.of(
                    new Deel(3, 4, "....L...", "....R..."),
                    new Deel(4, 12, "L...R...", "R...L...", "L...R...", "L.......", "R...L...", "L...R...", "R...L...", "R...R..."),
                    new Deel(12, 20, "L.R.L...", "R.L.R...", "L...R...", "L.L.R...", "R...L...", "L...R...", "R.R.L...", "L...L..."),
                    new Deel(20, 24, "R.......", "L.......", "R.......", "L...R..."),
                    new Deel(24, 30, "L.R.L...", "R.L.R...", "L...R...", "R...L...", "L.L.R...", "R...R..."),
                    new Deel(30, 32, "L...R...", "L.R.L.R."),
                    new Deel(32, 33, "R......."));
            // intro 0-3, verse 4-15, steel/brass solo 16-27, verse 28-39, outro 40-47, the last chord at beat 192
            case GUHLA_HULA_ROCK -> List.of(
                    new Deel(2, 4, "L...R...", "U...D.U."),
                    new Deel(4, 16, "L.R.U...", "D.U.L.R.", "L.R.U...", "D...U...", "R.L.D...", "U.D.R.L.", "L.R.U...", "D...U...",
                            "U.U.D.D.", "L.R.L.R.", "U.D.U.D.", "L...R..."),
                    new Deel(16, 28, "U...D...", "L.R.L.R.", "U...D...", "R.L.R.L.", "U.D.U...", "L.R.D...", "U...D...", "L.R.L.R.",
                            "UUD.L.R.", "D.U.D.U.", "L.R.UUD.", "L.R.U.D."),
                    new Deel(28, 40, "L.R.U...", "D.U.L.R.", "R.L.D...", "U...D...", "L.R.U...", "D.U.R.L.", "R.L.D...", "U...D...",
                            "U.U.D.D.", "L.R.L.R.", "U.D.U.D.", "L.LRR..."),
                    new Deel(40, 48, "L.R.U...", "D.U.L.R.", "R.L.D...", "U...D.U.", "L.R.U...", "D.U.L.R.", "U.U.U.U.", "L.R.U.D."),
                    new Deel(48, 49, "U......."));
            // intro 0-3 (VAHOEG! on bar 1), A 4-19 (njeg! at the end of every 4th bar), B 20-35, drum break 36-39 (two VAHOEG!s),
            // A 40-51, outro 52-59 (VAHOEG! on bars 55 and 59), the last chord at beat 240
            case VAHOEG_HULA_HOP -> List.of(
                    new Deel(1, 2, "....*..."),
                    new Deel(2, 4, "L.R.L.R.", "U.D.U.D."),
                    new Deel(4, 20, "L.R.LRL.", "U.D.U.D.", "R.L.RLR.", "U.U.DD.*"),
                    new Deel(20, 36, "L.LRR.U.", "D.U.D.U.", "R.RLL.D.", "U.D.UDU.", "LRL.RLR.", "U.D.L.R.", "RLR.LRL.", "UDU.D.U."),
                    new Deel(36, 40, "L.R.L.R.", "U.D.*...", "LRLRLRL.", "U.D.*..."),
                    new Deel(40, 52, "L.R.LRL.", "U.D.U.D.", "R.L.RLR.", "U.U.DD.*"),
                    new Deel(52, 60, "L.R.LRL.", "U.D.U.D.", "R.L.RLR.", "U.D.*...", "LRLRUDUD", "L.R.U.D.", "RLRLDUDU", "U.D.*..."),
                    new Deel(60, 61, "*......."));
        };
    }

    private static List<Noot> maak(HulaLiedje liedje) {
        List<Noot> out = new ArrayList<>();
        for (Deel d : delen(liedje)) {
            for (int maat = d.van; maat < d.tot; maat++) {
                String p = d.patronen[(maat - d.van) % d.patronen.length];
                for (int e = 0; e < 8; e++) {
                    Pas pas = Pas.van(p.charAt(e));
                    if (pas != null) {
                        out.add(new Noot(out.size(), maat * 4 + e * 0.5, pas));
                    }
                }
            }
        }
        return out;
    }

    // --- judging ------------------------------------------------------------------------------------------------------

    /** How well a step was danced. */
    public enum Oordeel {
        VAHOEG(100), NJEG(60), GUH(25), MIS(0);

        public final int punten;

        Oordeel(int punten) {
            this.punten = punten;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** The timing windows (ms, either side of the beat): makkelijk a bit wider. */
    public static int venster(HulaLiedje liedje, Oordeel o) {
        boolean ruim = liedje.niveau == nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK;
        return switch (o) {
            case VAHOEG -> ruim ? 70 : 55;
            case NJEG -> ruim ? 130 : 110;
            case GUH -> ruim ? 190 : 170;
            case MIS -> Integer.MAX_VALUE;
        };
    }

    /** The judgement for a step danced `ms` away from its beat (either way). */
    public static Oordeel oordeel(HulaLiedje liedje, double ms) {
        double a = Math.abs(ms);
        for (Oordeel o : new Oordeel[]{Oordeel.VAHOEG, Oordeel.NJEG, Oordeel.GUH}) {
            if (a <= venster(liedje, o)) {
                return o;
            }
        }
        return Oordeel.MIS;
    }

    /** The points of a step with this combo (the combo multiplies: x1 up to x3 from a streak of 40). */
    public static int punten(Oordeel o, int combo) {
        return (int) Math.round(o.punten * (1 + Math.min(combo, 40) / 20.0));
    }

    /** Schelpjesmunten for a hula score (before the level bonus): nothing below 800, then one per 1500. */
    public static int munten(int score) {
        return score < 800 ? 0 : Math.min(14, 1 + score / 1500);
    }

    private HulaKaart() {
    }
}
