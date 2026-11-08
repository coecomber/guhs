package nl.juiced.guhs.feature.vadsrijk.party.nep;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * W-PARTY: the role of a nep-speler, matching the mod's classes: tank = Knuffelridder, heler = Vadsmagier, schade = Pluisjager.
 * {@code deel}: a hit takes this part of the target's max Vads (bosses: a tenth of that); {@code tempo}: ticks between hits;
 * {@code klap}: the largest part of its own health one hit from a mob can take.
 */
public enum NepRol {
    TANK("knuffelridder", 1.0 / 12, 24, 0.04),
    HELER("vadsmagier", 1.0 / 22, 34, 0.07),
    SCHADE("pluisjager", 1.0 / 7, 20, 0.08);

    public final String klasse;
    public final double deel;
    public final int tempo;
    public final double klap;

    NepRol(String klasse, double deel, int tempo, double klap) {
        this.klasse = klasse;
        this.deel = deel;
        this.tempo = tempo;
        this.klap = klap;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Lang key of the role name ("Tank", "Heler", "Schade"). */
    public String key() {
        return "vadsrijk.party.rol." + id();
    }

    public static NepRol van(int i) {
        return i >= 0 && i < values().length ? values()[i] : SCHADE;
    }

    @Nullable
    public static NepRol vanId(String s) {
        for (NepRol r : values()) {
            if (r.id().equalsIgnoreCase(s)) {
                return r;
            }
        }
        return null;
    }
}
