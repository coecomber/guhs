package nl.juiced.guhs.feature.band;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;

/**
 * The hartjes levels of a tamed guh with its owner (hearts never go down, so a level is never lost). Names exactly as
 * in the design (lang {@code gui.guhs.band.niveau.<id>}).
 */
public enum BandNiveau {
    /** Not a level yet: "op weg naar lieve vadsjes..." (only shown as progress). */
    GEEN(0),
    /** "lieve vadsjes van elkaar" */
    LIEF(100),
    /** "mega lieve vadsjes van elkaar" */
    MEGA(600),
    /** "zielsguh bff 5evr &lt;3" */
    ZIELSGUH(2000);

    private final int drempel;

    BandNiveau(int drempel) {
        this.drempel = drempel;
    }

    /** Hearts needed for this level. */
    public int drempel() {
        return drempel;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component naam() {
        return Component.translatable("gui.guhs.band.niveau." + id());
    }

    /** The level these hearts reach. */
    public static BandNiveau van(int hartjes) {
        BandNiveau best = GEEN;
        for (BandNiveau n : values()) {
            if (hartjes >= n.drempel) {
                best = n;
            }
        }
        return best;
    }

    /** The next level, or null at the top. */
    @Nullable
    public BandNiveau volgende() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }

    public static BandNiveau byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : GEEN;
    }
}
