package nl.juiced.guhs.feature.vadskracht;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
 * The face of a guh machine (every machine has one): asleep without vadskracht, happy while it has vadskracht, surprised
 * when it is full (its output is blocked). The block property is {@link MachineBlock#SNOET}; the three faces are painted by
 * tools/features/vadskracht.py ({@code snoet}, {@code machine}).
 */
public enum Snoet implements StringRepresentable {
    SLAAPT, WERKT, VOL;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The face that goes with this: no vadskracht = asleep, else surprised when full, else happy. */
    public static Snoet van(boolean kracht, boolean vol) {
        return !kracht ? SLAAPT : vol ? VOL : WERKT;
    }
}
