package nl.juiced.guhs.feature.vadsrijk.klassen.spec;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;

/**
 * U-SPECS (ronde 4, V2/V3): the six specialisations, two per class, chosen ONCE at the first job advancement (class lv 17) and
 * switchable later at a class NPC in town. Style-based: both specs of a class deal good damage solo; support/tank are optional
 * side branches of the tree ({@link SpecBoom}), not specs.
 * <p>{@link #A} = the left spec column of the tree (kolom 0), {@link #B} = the right one (kolom 2).
 * The chosen spec is a hidden tree node ({@link #knoop()}, row -1, cost 0): a respec never removes it, the client sees it in the
 * synced tree data, and the spec branch nodes simply require it (so {@code Boom.waaromNiet} needs no extra rule).
 */
public enum Spec {
    SCHILDRIDDER("schildridder", Klassen.RIDDER, true, 0xFF7AC8, "minecraft:shield"),
    REUZENKUSSEN("reuzenkussen", Klassen.RIDDER, false, 0xFFB86B, "minecraft:white_bed"),
    SCHERPSCHUTTER("scherpschutter", Klassen.JAGER, true, 0x6FD8A8, "minecraft:bow"),
    STRIKKENZETTER("strikkenzetter", Klassen.JAGER, false, 0xC8E66F, "minecraft:tripwire_hook"),
    STERRENWEVER("sterrenwever", Klassen.MAGIER, true, 0xB58CFF, "minecraft:nether_star"),
    ZEGENZANGER("zegenzanger", Klassen.MAGIER, false, 0xFFD24A, "minecraft:note_block");

    public final String id, klasse;
    /** True = spec A (tree column 0), false = spec B (column 2). */
    public final boolean links;
    public final int kleur;
    public final String icoon;

    Spec(String id, String klasse, boolean links, int kleur, String icoon) {
        this.id = id;
        this.klasse = klasse;
        this.links = links;
        this.kleur = kleur;
        this.icoon = icoon;
    }

    /** "r", "j" or "m": the node prefix of the class. */
    public static String prefix(String klasse) {
        return switch (klasse) {
            case Klassen.JAGER -> "j";
            case Klassen.MAGIER -> "m";
            default -> "r";
        };
    }

    /** The hidden tree node that means "this spec is chosen", e.g. {@code r_spec_schildridder}. */
    public String knoop() {
        return prefix(klasse) + "_spec_" + id;
    }

    public String naamKey() {
        return "vadsrijk.klassen.spec." + id;
    }

    /** The title lang key at advancement step 1..3. */
    public String titelKey(int stap) {
        return "vadsrijk.klassen.titel." + id + "." + Math.max(1, Math.min(3, stap));
    }

    /** The other spec of the same class. */
    public Spec ander() {
        for (Spec s : values()) {
            if (s.klasse.equals(klasse) && s != this) {
                return s;
            }
        }
        return this;
    }

    @Nullable
    public static Spec van(@Nullable String id) {
        if (id == null) {
            return null;
        }
        for (Spec s : values()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** Spec A then B of a class. */
    public static List<Spec> vanKlasse(String klasse) {
        List<Spec> l = new ArrayList<>();
        for (Spec s : values()) {
            if (s.klasse.equals(klasse) && s.links) {
                l.add(s);
            }
        }
        for (Spec s : values()) {
            if (s.klasse.equals(klasse) && !s.links) {
                l.add(s);
            }
        }
        return l;
    }

    /** The spec whose node is in this tree node set (null = none chosen yet). */
    @Nullable
    public static Spec uit(String klasse, java.util.Collection<String> knopen) {
        for (Spec s : values()) {
            if (s.klasse.equals(klasse) && knopen.contains(s.knoop())) {
                return s;
            }
        }
        return null;
    }
}
