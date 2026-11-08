package nl.juiced.guhs.feature.vadsrijk.zone6.wezens;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan;

/**
 * The boss MUSIC HOOKS of zone 6 for u-audio2's pipeline ({@code tools/vadsrijk/geluid}: {@code stukken.py} + {@code GeluidPlan.BAAS_THEMAS}).
 * Every zone-6 boss names its own theme id {@code guhs:vr.geluid.baas.<thema>} through {@code VadsrijkBaas.muziek()}; the pipeline
 * then plays that theme, its last-phase layer ({@code _fase}) and its fanfare. Until a theme is rendered (not yet in
 * {@link GeluidPlan#BAAS_THEMAS}), the boss asks for a related EXISTING theme ({@link #TIJDELIJK}), so there is never silence.
 * <p>The wanted themes (nl: "oed, qanun, darbuka en ney-fluit, guh-zoet"; the caravan song of Oma Dadel is the leitmotif) are listed
 * with a suggested spec in NOTITIES_U_Z6WEZENS ("Muziek").
 */
public final class Z6Muziek {
    /** baasId -> theme id (after vr.geluid.baas.). */
    public static final Map<String, String> THEMA = new LinkedHashMap<>();
    /** theme id -> an existing theme that plays until it is rendered. */
    public static final Map<String, String> TIJDELIJK = new LinkedHashMap<>();

    static {
        thema("wormelien", "wormelien", "zeeslurf");
        thema("zoutkolos", "zoutkolos", "brokkel");
        thema("astrolabiumwachter", "sterrenwacht", "spiegelzucht");
        thema("glimmertje", "glimmertje", "mika_spion");
        thema("sterrenguh", "verzonken_sterrenguh", "vahoegnir");
    }

    private static void thema(String baas, String thema, String tijdelijk) {
        THEMA.put(baas, thema);
        TIJDELIJK.put(thema, tijdelijk);
    }

    private Z6Muziek() {
    }

    /** The theme this boss plays now: its own when rendered, else the stand-in. */
    public static String thema(String baasId) {
        String t = THEMA.getOrDefault(baasId, "algemeen");
        if (GeluidPlan.BAAS_THEMAS.contains(t)) {
            return t;
        }
        String s = TIJDELIJK.getOrDefault(t, "algemeen");
        return GeluidPlan.BAAS_THEMAS.contains(s) ? s : "algemeen";
    }

    /** The music id for {@code VadsrijkBaas.muziek()}. */
    public static Identifier id(String baasId) {
        return Guhs.id(GeluidPlan.baasId(thema(baasId)));
    }
}
