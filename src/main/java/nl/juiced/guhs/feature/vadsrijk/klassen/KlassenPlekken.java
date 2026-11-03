package nl.juiced.guhs.feature.vadsrijk.klassen;

import net.minecraft.core.BlockPos;

/**
 * Every world spot the klassen slice knows about (SLICE_BRIEF: one class). The klassen logic itself is position-free: the
 * ordetempel NPC (npc slice) calls {@link KlasseBalie#open} and the ceremony plays around the player wherever they stand.
 * These are only the expected spots (POI_VERZOEKEN.md, section klassen) for the dev command and for integration to check.
 */
public final class KlassenPlekken {
    /** The ordetempel on the highest hat of Zwamzoet (placeholder until POI_VADSRIJK.md): the class desk + the ceremony circle. */
    public static final BlockPos ORDETEMPEL = new BlockPos(0, 184, -24);
    /** The klasse-tempeltje in Knuffelkamp (first class choice), placeholder. */
    public static final BlockPos KLASSE_TEMPELTJE = new BlockPos(-30, 66, 1040);

    private KlassenPlekken() {
    }
}
