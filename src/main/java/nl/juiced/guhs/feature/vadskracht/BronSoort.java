package nl.juiced.guhs.feature.vadskracht;

import java.util.Locale;

import net.minecraft.network.chat.Component;

/**
 * The kinds of vadskracht sources. Per net only the first {@link #max} sources of a kind count (highest output first, then
 * the lowest position); the others do nothing and the hover readout says so. The plural name is
 * {@code gui.guhs.vadskracht.soort.<id>}, the singular {@code ...<id>.enkel} (tools/features/vadskracht.py).
 */
public enum BronSoort {
    GUHRAD(4), KNUFFELGENERATOR(2), DISCO_DYNAMO(2), BLUBKACHELTJE(4), GLOEISTERKERN(1);

    /** How many of this kind count in one net. */
    public final int max;

    BronSoort(int max) {
        this.max = max;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The plural name ("Guhraden"). */
    public Component naam() {
        return Component.translatable("gui.guhs.vadskracht.soort." + id());
    }

    /** The name that goes with this many ("1 Gloeisterkern", "4 Guhraden"), for the hover readout. */
    public Component naam(int aantal) {
        return aantal == 1 ? Component.translatable("gui.guhs.vadskracht.soort." + id() + ".enkel") : naam();
    }
}
