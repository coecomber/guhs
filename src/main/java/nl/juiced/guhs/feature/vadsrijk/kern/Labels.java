package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * X-LABELS: the safety net against raw keys. Many Vadsrijk texts are keys BUILT from an id ({@code "vadsrijk.npc.naam." + id});
 * when a new id has no text yet, Minecraft would show the key itself ("vadsrijk.npc.naam.kk_poort_zwamzoet"). The client's
 * language ({@code GuhsTaal.GuhsLanguage}) asks {@link #terugval} instead for a missing {@code vadsrijk.*} key:
 * <ol>
 *   <li>the generic key of the same family: drop {@code _<suffix>} parts of an id, last segment first
 *   ({@code naam.kk_poort_zwamzoet} -> {@code naam.kk_poort} "Kwartiermeester"; {@code praat.kk_poort_zwamzoet.2} ->
 *   {@code praat.kk_poort.2});</li>
 *   <li>a line number that does not exist -> line 1 ({@code praat.x.4} -> {@code praat.x.1});</li>
 *   <li>else the id made readable ("Iets heel raars"), never the key.</li>
 * </ol>
 * The real names are still added (VadsrijkAlleLabelsGameTests fails on every missing one); this only keeps a forgotten one from
 * looking broken.
 */
public final class Labels {
    /** What a raw key looks like: a.b(.c...), lower case, digits and _ only. */
    public static final Pattern SLEUTEL = Pattern.compile("[a-z0-9_]+(?:\\.[a-z0-9_]+)+");
    /** Segments that say WHICH text of a thing it is, not which thing: skipped when making the id readable. */
    private static final Set<String> SOORT = Set.of("naam", "titel", "tekst", "onder", "uitleg", "kort", "rol", "bron", "lang", "hallo",
            "hint", "regel", "plek", "npc", "praat", "fase", "lijn", "haven", "tab", "q", "doel");

    private Labels() {
    }

    public static boolean isSleutel(String s) {
        return s != null && SLEUTEL.matcher(s).matches();
    }

    /**
     * The text to show for a missing key: a sibling's text, or a readable id. {@code zoek} answers a key's text (null: missing).
     * Never returns the key itself.
     */
    public static String terugval(String key, Function<String, String> zoek) {
        String[] d = key.split("\\.");
        // 1. the generic key: strip _suffixes of one segment at a time, from the last segment to the third
        for (int i = d.length - 1; i >= 2; i--) {
            String seg = d[i];
            while (seg.lastIndexOf('_') > 0) {
                seg = seg.substring(0, seg.lastIndexOf('_'));
                String[] c = d.clone();
                c[i] = seg;
                String v = zoek.apply(String.join(".", c));
                if (v != null) {
                    return v;
                }
            }
        }
        // 2. a line that does not exist: the first one
        if (d.length > 2 && d[d.length - 1].matches("\\d+")) {
            for (String n : new String[] {"1", "0"}) {
                String[] c = d.clone();
                c[c.length - 1] = n;
                String v = zoek.apply(String.join(".", c));
                if (v != null && !c[c.length - 1].equals(d[d.length - 1])) {
                    return v;
                }
            }
        }
        // 3. the id, readable
        for (int i = d.length - 1; i >= 1; i--) {
            String seg = d[i];
            if (seg.isEmpty() || seg.matches("\\d+") || SOORT.contains(seg)) {
                continue;
            }
            seg = seg.replaceFirst("^z\\d_", "").replace('_', ' ').strip();
            if (!seg.isEmpty()) {
                return seg.substring(0, 1).toUpperCase(Locale.ROOT) + seg.substring(1);
            }
        }
        return "?";
    }
}
