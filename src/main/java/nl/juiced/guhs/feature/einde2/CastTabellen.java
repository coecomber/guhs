package nl.juiced.guhs.feature.einde2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.castschurk.CastSchurk;
import nl.juiced.guhs.feature.einde2.castteam.CastTeam;
import nl.juiced.guhs.feature.einde2.castvolk.CastVolk;

/**
 * einde2: the three cast tables of the Guhvatar story ({@link CastTeam}, {@link CastVolk}, {@link CastSchurk}; CONTRACT_E2
 * 11.2 H) seen as one, for what the merge step built on top of them (11.7): the residents ({@link Bewoners}), the see-through
 * and glowing kinds (client: SittingGuhRenderers.NPC_DOORZICHTIG / NPC_GLOED), the named animations on demand
 * ({@link GuhNpcEntity#speel}) and the dev rows ({@link Einde2Toon}). It only reads the tables; the slices own them.
 */
public final class CastTabellen {
    /** The three groups, in the order of the merge (and of the dev command). */
    public enum Groep {
        TEAM, VOLK, SCHURK;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private CastTabellen() {
    }

    /** Every kind of one table, in the order of its Kind block. */
    public static List<GuhNpcEntity.Kind> kinds(Groep groep) {
        return switch (groep) {
            case TEAM -> CastTeam.kinds();
            case VOLK -> CastVolk.kinds();
            case SCHURK -> CastSchurk.kinds();
        };
    }

    /** Every kind of the three tables. */
    public static List<GuhNpcEntity.Kind> kinds() {
        List<GuhNpcEntity.Kind> alle = new ArrayList<>();
        for (Groep g : Groep.values()) {
            alle.addAll(kinds(g));
        }
        return Collections.unmodifiableList(alle);
    }

    /** The table a kind belongs to (null: not a kind of the einde2 cast). */
    @Nullable
    public static Groep groep(GuhNpcEntity.Kind kind) {
        if (CastTeam.rij(kind) != null) {
            return Groep.TEAM;
        }
        if (CastVolk.kinds().contains(kind)) {
            return Groep.VOLK;
        }
        if (CastSchurk.rij(kind) != null) {
            return Groep.SCHURK;
        }
        return null;
    }

    public static boolean isCast(GuhNpcEntity.Kind kind) {
        return groep(kind) != null;
    }

    public static GuhNpcEntity.Kind basis(GuhNpcEntity.Kind kind) {
        return switch (moet(kind)) {
            case TEAM -> CastTeam.basis(kind);
            case VOLK -> CastVolk.basis(kind);
            case SCHURK -> CastSchurk.basis(kind);
        };
    }

    public static int mix(GuhNpcEntity.Kind kind) {
        return switch (moet(kind)) {
            case TEAM -> CastTeam.mix(kind);
            case VOLK -> CastVolk.mix(kind);
            case SCHURK -> CastSchurk.mix(kind);
        };
    }

    public static boolean doorzichtig(GuhNpcEntity.Kind kind) {
        Groep g = groep(kind);
        return g != null && switch (g) {
            case TEAM -> CastTeam.doorzichtig(kind);
            case VOLK -> CastVolk.doorzichtig(kind);
            case SCHURK -> CastSchurk.doorzichtig(kind);
        };
    }

    /** Then textures/entity/npc_&lt;kind&gt;_glowmask.png exists. */
    public static boolean gloed(GuhNpcEntity.Kind kind) {
        Groep g = groep(kind);
        return g != null && switch (g) {
            case TEAM -> CastTeam.gloed(kind);
            case VOLK -> CastVolk.gloed(kind);
            case SCHURK -> CastSchurk.gloed(kind);
        };
    }

    /** The names besides idle (the first is "praat"); empty for a kind that is not of the cast. */
    public static List<String> animaties(GuhNpcEntity.Kind kind) {
        Groep g = groep(kind);
        if (g == null) {
            return List.of();
        }
        return switch (g) {
            case TEAM -> CastTeam.animaties(kind);
            case VOLK -> CastVolk.animaties(kind);
            case SCHURK -> CastSchurk.animaties(kind);
        };
    }

    /**
     * Ticks of a named animation: 0 = a pose that lasts (houding), -1 = the kind's file does not hold it ("idle" too: idle
     * is what plays when nothing does). Never throws.
     */
    public static int duur(GuhNpcEntity.Kind kind, String naam) {
        Groep g = groep(kind);
        if (g == null || !animaties(kind).contains(naam)) {
            return -1;
        }
        return switch (g) {
            case TEAM -> CastTeam.duur(kind, naam);
            case VOLK -> CastVolk.duur(kind, naam);
            case SCHURK -> CastSchurk.duur(kind, naam);
        };
    }

    /** (Einde2.register) Lets every kind of the cast play its named animations on demand (M3). */
    static void registreer() {
        for (GuhNpcEntity.Kind kind : kinds()) {
            GuhNpcEntity.ANIMATIE_DUUR.put(kind, naam -> duur(kind, naam));
        }
    }

    private static Groep moet(GuhNpcEntity.Kind kind) {
        Groep g = groep(kind);
        if (g == null) {
            throw new IllegalArgumentException(kind + " is not a kind of the einde2 cast");
        }
        return g;
    }
}
