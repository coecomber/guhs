package nl.juiced.guhs.feature.knuffelbad;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;

/**
 * The rubber ducks on the slides: the plain yellow one and the twelve special ones of the badeendjes collection (Knus
 * tab). Some only float on one slide; the golden one is very rare. {@link #bones} are the extra bones of the duck model
 * that the kind shows (a cap, a snorkel, guh ears...), {@link #glimt}: it glows in the dark.
 */
public enum Eendsoort {
    NORMAAL(10, null, false),
    VADSEENDJE(25, null, false, "eend_buikje"),
    KAASEENDJE(25, null, false),
    GUHEENDJE(25, null, false, "eend_oren"),
    BADMEESTEREENDJE(25, null, false, "eend_petje", "eend_fluitje"),
    PLUISEENDJE(25, Glijbaan.ROZE_TRECHTER, false, "eend_kuifje"),
    TRECHTEREENDJE(25, Glijbaan.ROZE_TRECHTER, false),
    STERRENEENDJE(25, Glijbaan.GLIMTUNNEL, true),
    GLIMEENDJE(25, Glijbaan.GLIMTUNNEL, true),
    MAANEENDJE(25, Glijbaan.GLIMTUNNEL, true, "eend_slaapmuts"),
    DUIKEENDJE(25, Glijbaan.GROTE_PLONS, false, "eend_snorkel", "eend_bril"),
    PLONSEENDJE(25, Glijbaan.GROTE_PLONS, false, "eend_kroontje"),
    GOUDEN_EENDJE(50, null, true, "eend_kroontje");

    /** The special ones (the badeendjes collection), in page order. */
    public static final List<Eendsoort> SPECIAAL = List.of(values()).subList(1, values().length);
    /** Every ride has a chance on a special duck at each spot; the golden one at most once, rarely. */
    public static final float KANS_SPECIAAL = 0.08f, KANS_GOUD = 0.015f;

    public final int punten;
    @Nullable
    public final Glijbaan alleenOp;
    public final boolean glimt;
    public final List<String> bones;

    Eendsoort(int punten, @Nullable Glijbaan alleenOp, boolean glimt, String... bones) {
        this.punten = punten;
        this.alleenOp = alleenOp;
        this.glimt = glimt;
        this.bones = List.of(bones);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean speciaal() {
        return this != NORMAAL;
    }

    /** Can it float on this slide? */
    public boolean op(Glijbaan baan) {
        return alleenOp == null || alleenOp == baan;
    }

    public Component naam() {
        return Component.translatable("gui.guhs.knus.badeendjes." + id());
    }

    public static Eendsoort byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : NORMAAL;
    }

    @Nullable
    public static Eendsoort byId(String id) {
        for (Eendsoort e : values()) {
            if (e.id().equals(id)) {
                return e;
            }
        }
        return null;
    }
}
