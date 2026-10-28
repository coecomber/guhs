package nl.juiced.guhs.feature.vadskracht;

import java.util.Locale;

import net.minecraft.network.chat.Component;

/**
 * The kinds of vadskracht sources. Per net only the first {@link #max} sources of a kind count (highest output first, then
 * the lowest position); the others do nothing and the hover readout says so. The plural name is
 * {@code gui.guhs.vadskracht.soort.<id>}, the singular {@code ...<id>.enkel} (tools/features/vadskracht.py).
 */
public enum BronSoort {
    GUHRAD(16), KNUFFELGENERATOR(2), DISCO_DYNAMO(2), BLUBKACHELTJE(4), GLOEISTERKERN(1),
    // einde2 tier 5 (CONTRACT_E2_B 1.2 and 9 item 7, owner trap5_machines, texts in tools/features/einde2_trap5_machines.py)
    ENDERGUHRAD(1), KOMEETKERN(1),
    // 1.8.0 (vadskracht, feature/vadsbasis): the Knabbelketel (not part of the Guhraden's afnemende opbrengst)
    KNABBELKETEL(4);

    /** How many of this kind count in one net. */
    public final int max;

    BronSoort(int max) {
        this.max = max;
    }

    /**
     * 1.8.0: how many of this kind count in FULL (the strongest first); the next ones count for less and less
     * ({@link #deel}). For most kinds that is all of {@link #max}; the Guhraden have afnemende opbrengst.
     */
    public int vol() {
        return this == GUHRAD ? VadsGetallen.GUHRAD_VOL : max;
    }

    /**
     * 1.8.0: the part (per mille) the source on this place counts for (0 = the strongest): 1000 for the first {@link #vol},
     * then {@link VadsGetallen#GUHRAD_AFNAME_PROCENT}% of the one before; 0 from {@link #max} on.
     */
    public int deel(int plaats) {
        if (plaats >= max) {
            return 0;
        }
        if (plaats < vol()) {
            return 1000;
        }
        double f = Math.pow(VadsGetallen.GUHRAD_AFNAME_PROCENT / 100.0, plaats - vol() + 1);
        return (int) Math.round(f * 1000);
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
