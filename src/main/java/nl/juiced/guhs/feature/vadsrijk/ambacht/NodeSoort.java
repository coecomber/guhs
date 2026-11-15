package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * 1.10.4 (Ambachten 9 -> 4): the KIND of a gathering spot, apart from the ambacht that levels on it. Verzamelen has four kinds
 * (ore, wood, herbs, hives: the old Mijnen, Houthakken, Plukken and Imkeren, each with its own minigame and look), Vissen two
 * (fishing spots and coral dives). A material belongs to one kind ({@link Materialen.Materiaal#soort}), a gathering group too
 * ({@link AmbachtPlekken.Groep#soort}). Phase 2 (real shared blocks) keys its blocks on kind + tier, so keep this list clean.
 * <p>{@link #oud}: the id of the old ambacht this kind came from (minigame texts {@code vadsrijk.ambacht.mini.<oud>}).
 * Names: {@code vadsrijk.ambacht.soort.<id>}.
 */
public enum NodeSoort {
    ERTS("erts", Ambacht.VERZAMELEN, "mijnen", AmbachtPlekken.Zoek.STEEN),
    HOUT("hout", Ambacht.VERZAMELEN, "houthakken", AmbachtPlekken.Zoek.BOOM),
    KRUID("kruid", Ambacht.VERZAMELEN, "plukken", AmbachtPlekken.Zoek.GROND),
    KORF("korf", Ambacht.VERZAMELEN, "imkeren", AmbachtPlekken.Zoek.BLOEM),
    VIS("vis", Ambacht.VISSEN, "vissen", AmbachtPlekken.Zoek.WATER),
    DUIK("duik", Ambacht.VISSEN, "duiken", AmbachtPlekken.Zoek.KORAAL);

    public final String id;
    /** The ambacht that gets the xp (and whose level gates the spot). */
    public final Ambacht ambacht;
    /** The old (pre-1.10.4) word of this kind: the minigame text key. */
    public final String oud;
    /** How spots of this kind are found in the terrain by default. */
    public final AmbachtPlekken.Zoek zoek;

    NodeSoort(String id, Ambacht ambacht, String oud, AmbachtPlekken.Zoek zoek) {
        this.id = id;
        this.ambacht = ambacht;
        this.oud = oud;
        this.zoek = zoek;
    }

    public String naamKey() {
        return "vadsrijk.ambacht.soort." + id;
    }

    @Nullable
    public static NodeSoort van(String id) {
        String s = id.toLowerCase(Locale.ROOT);
        for (NodeSoort n : values()) {
            if (n.id.equals(s) || n.oud.equals(s)) {
                return n;
            }
        }
        return null;
    }
}
