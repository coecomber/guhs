package nl.juiced.guhs.feature.vadsrijk.klassen;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * Every world spot the klassen slice knows about (SLICE_BRIEF: one class). The klassen logic itself is position-free: the
 * ordetempel NPC (npc slice) calls {@link KlasseBalie#open} and the ceremony plays around the player wherever they stand.
 * These are only the expected spots (POI_VERZOEKEN.md, section klassen) for the dev command and for integration to check.
 */
public final class KlassenPlekken {
    /** The ordetempel on the highest hat of Zwamzoet (placeholder until POI_VADSRIJK.md): the class desk + the ceremony circle. */
    public static final BlockPos ORDETEMPEL = WereldPlekken.plek("ordetempel_altaar");   // (real map, integration)
    /** The klasse-tempeltje in Knuffelkamp (first class choice), placeholder. */
    public static final BlockPos KLASSE_TEMPELTJE = WereldPlekken.plek("tempeltje");

    private KlassenPlekken() {
    }
}
