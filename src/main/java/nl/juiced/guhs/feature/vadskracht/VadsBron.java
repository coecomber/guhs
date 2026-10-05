package nl.juiced.guhs.feature.vadskracht;

import javax.annotation.Nullable;

/** A source of vadskracht (a Guhrad, a Knuffelgenerator...). See {@link VadsKnoop}. */
public interface VadsBron extends VadsKnoop {
    /** The kind, for the cap per net ({@link BronSoort#max}); null = a source without a cap (the test source). */
    @Nullable
    BronSoort vadsSoort();

    /** VK per second it can give NOW (0: no guh in the wheel, nobody dancing...). Asked at every evaluation. */
    int vadsAanbod();

    /** Told at every evaluation: does this source count, or is it one too many of its kind in this net? */
    default void vadsTelt(boolean teltMee) {
    }
}
