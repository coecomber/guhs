package nl.juiced.guhs.feature.verhaal;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Mob;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;

/** 3.0: the registry of {@link VariantGedrag}s (one per variant; the last {@link #zet} wins). */
public final class VariantGedragen {
    private static final Map<GuhVariant, VariantGedrag> GEDRAGEN = java.util.Collections.synchronizedMap(new EnumMap<>(GuhVariant.class));

    public static void zet(GuhVariant v, VariantGedrag g) {
        GEDRAGEN.put(v, g);
    }

    @Nullable
    public static VariantGedrag van(GuhVariant v) {
        return GEDRAGEN.get(v);
    }

    /** (tests) take a registered behaviour away again. */
    public static void weg(GuhVariant v) {
        GEDRAGEN.remove(v);
    }

    /** The behaviour of this guh's variant, or null. */
    @Nullable
    public static VariantGedrag van(GuhEntity guh) {
        return GEDRAGEN.get(guh.getVariant());
    }

    /** How much a chore helper carries per trip: its variant's draagFactor (1 for anything that isn't a guh). */
    public static int draagFactor(Mob m) {
        if (m instanceof GuhEntity guh) {
            VariantGedrag g = van(guh);
            return g == null ? 1 : Math.max(1, g.draagFactor(guh));
        }
        return 1;
    }

    /** The VOEREN heart factor of this guh (1 without a behaviour). */
    public static int voerFactor(GuhEntity guh) {
        VariantGedrag g = van(guh);
        return g == null ? 1 : Math.max(1, g.voerFactor(guh));
    }

    private VariantGedragen() {
    }
}
