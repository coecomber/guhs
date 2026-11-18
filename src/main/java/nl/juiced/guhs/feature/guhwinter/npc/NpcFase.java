package nl.juiced.guhs.feature.guhwinter.npc;

import java.util.Locale;

/**
 * Guhwinter (npc slice): which guhs stand on the Winterplein. A small view of kern's {@code GuhwinterFase} (VOOR, SINT and
 * PAKJESAVOND are all {@link #SINT} here, AFGELOPEN is {@link #WEG}); {@link NpcFaseBron} turns one into the other.
 */
public enum NpcFase {
    /** Until 6 Dec 00:00: Sinterklaas, his pakjesguhs and Ozovads are here too. */
    SINT,
    /** 6 Dec to 25 Dec: the Sint has left, opa Wijnand has come. */
    KERST,
    /** 26 Dec to 2 Jan: oliebollen and good intentions. */
    OUDJAAR,
    /** From 3 Jan 2027: everyone has gone home. */
    WEG;

    /** The lang key part of this phase ("sint", "kerst", "oudjaar"; "weg" has no lines). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
