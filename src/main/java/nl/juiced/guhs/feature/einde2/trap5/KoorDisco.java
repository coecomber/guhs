package nl.juiced.guhs.feature.einde2.trap5;

import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.einde2.biomeguhs.Biomeguhs;

/**
 * The Koorguh on the Disco-dynamo (CONTRACT_E2_B 7.12, C-A 7 "Koorguh counts extra on the Disco-dynamo"): a Koorguh sings
 * along while it dances, so it counts as two dancers ({@link Biomeguhs#discoFactor}).
 * <p>
 * hook fase C (merge step): main's {@code techbron.DiscoDynamoBlockEntity} is nobody's in this wave (CONTRACT_E2_B 1.1), so
 * it cannot ask this yet. The one change it needs, in {@code kijk(...)}: count the dancers with
 * {@code nu = KoorDisco.dansers(groep.bezig(), nu)} right after {@code groep.kijk(...)} (the result is still capped by
 * {@code DISCO_MAX_GUHS} in {@code vadsAanbod}). See the Deviations of CONTRACT_E2_B.
 */
public final class KoorDisco {
    private KoorDisco() {
    }

    /** How many dancers these guhs count for (a Koorguh twice); never fewer than {@code minstens}. */
    public static int dansers(Iterable<? extends GuhEntity> guhs, int minstens) {
        int n = 0;
        for (GuhEntity g : guhs) {
            n += Biomeguhs.discoFactor(g);
        }
        return Math.max(n, minstens);
    }
}
