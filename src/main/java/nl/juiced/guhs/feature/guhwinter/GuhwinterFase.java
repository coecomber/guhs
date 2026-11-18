package nl.juiced.guhs.feature.guhwinter;

import java.time.ZonedDateTime;

/**
 * 1.10.9 Guhwinter: the phase of the event at a moment (Europe/Amsterdam), see {@link Guhwinter#fase}.
 * <ul>
 *   <li>{@link #VOOR}: before {@link Guhwinter#START}; counts as Sint everywhere ({@link #sint()}, {@link #bezig()});</li>
 *   <li>{@link #SINT}: until 5 Dec 18:59;</li>
 *   <li>{@link #PAKJESAVOND}: 5 Dec 19:00 until 6 Dec 00:00 (still Sint content);</li>
 *   <li>{@link #KERST}: 6 Dec until 26 Dec 00:00;</li>
 *   <li>{@link #OUDJAAR}: 26 Dec until 3 Jan 00:00;</li>
 *   <li>{@link #AFGELOPEN}: from 3 Jan 2027 00:00 (the plaza is an empty frame, the event items melt).</li>
 * </ul>
 */
public enum GuhwinterFase {
    VOOR, SINT, PAKJESAVOND, KERST, OUDJAAR, AFGELOPEN;

    /** The phase at this moment (any zone: it is compared as an instant). */
    public static GuhwinterFase op(ZonedDateTime t) {
        if (t.isBefore(Guhwinter.START)) {
            return VOOR;
        }
        if (t.isBefore(Guhwinter.PAKJESAVOND)) {
            return SINT;
        }
        if (t.isBefore(Guhwinter.KERST)) {
            return PAKJESAVOND;
        }
        if (t.isBefore(Guhwinter.OUDJAAR)) {
            return KERST;
        }
        if (t.isBefore(Guhwinter.EINDE)) {
            return OUDJAAR;
        }
        return AFGELOPEN;
    }

    /** The Sint part (VOOR counts as Sint, so the dev client before release shows it too). */
    public boolean sint() {
        return this == VOOR || this == SINT || this == PAKJESAVOND;
    }

    public boolean kerst() {
        return this == KERST;
    }

    public boolean oudjaar() {
        return this == OUDJAAR;
    }

    /** Is the event on (everything but AFGELOPEN; VOOR counts as Sint)? */
    public boolean bezig() {
        return this != AFGELOPEN;
    }

    /** Lang key of its name: guhwinter.kern.fase.&lt;name&gt;. */
    public String sleutel() {
        return "guhwinter.kern.fase." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
