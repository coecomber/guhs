package nl.juiced.guhs.feature.vadsrijk.geluid;

import java.util.List;

/**
 * U-AUDIO2: the little NPC voices of Het Vadsrijk. No new recordings: they are the EXISTING guh sounds of the Guhs mod (the
 * guh_ambient takes, Mika's takes, the pieppiepmuisje squeak and the Guhxolotl blub), grouped as sound events in sounds.json
 * by the generator {@code tools/features/vadsrijk_u_audio2.py}:
 * <ul>
 *   <li>{@code groet}: a happy "guh!" (the vahoeg/guh takes) when a conversation opens or you click an NPC;</li>
 *   <li>{@code vraag}: a curious "oeh?/njeg?" when the page asks something;</li>
 *   <li>{@code dank}: a giggle when the NPC is glad (a quest handed in, a thank-you page);</li>
 *   <li>{@code mika}: the Mika takes (for Mika speakers);</li>
 *   <li>{@code piep} / {@code blub}: now and then for flavour instead of the guh (blub more often on the coast of zone 4).</li>
 * </ul>
 * The pitch is per NPC (its name), so every guh keeps its own voice.
 */
public final class StemPlan {
    public static final List<String> SOORTEN = List.of("groet", "vraag", "dank", "mika", "piep", "blub");

    private StemPlan() {
    }

    public static String id(String soort) {
        return "vr.stem." + soort;
    }

    /** The pitch of one NPC: stable per name (0.82 .. 1.40); a "Toon" from the dialogue data shifts it a little. */
    public static float toon(String naam) {
        int h = naam == null ? 0 : naam.hashCode();
        return 0.82f + Math.floorMod(h * 31 + 7, 59) / 100f;
    }

    /**
     * Which voice to play: wat = groet/vraag/dank, stem = the dialogue voice (guh / mika / verteller / speler / draak / wezen),
     * factie = z1..z5/..., r = a random number 0..1. Null = no voice (the narrator and your own lines are silent).
     */
    public static String kies(String wat, String stem, String factie, double r) {
        if ("verteller".equals(stem) || "speler".equals(stem)) {
            return null;
        }
        if ("mika".equals(stem)) {
            return "mika";
        }
        double blub = "z4".equals(factie) ? 0.15 : 0.04;
        if (r < blub) {
            return "blub";
        }
        if (r < blub + 0.07) {
            return "piep";
        }
        return SOORTEN.contains(wat) ? wat : "groet";
    }
}
