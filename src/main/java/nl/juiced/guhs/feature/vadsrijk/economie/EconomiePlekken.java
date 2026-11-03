package nl.juiced.guhs.feature.vadsrijk.economie;

import net.minecraft.core.BlockPos;

/**
 * Every coordinate the economie slice uses (SLICE_BRIEF). The economie NPCs themselves (smidguh, winkelguhs, marktmeester,
 * stalmeester) are the npc slice's; these are the spots of the wereld slice's POI_VADSRIJK.md (v0 section 5), used by the dev
 * command {@code /guhs vadsrijk eco ga <plek>}.
 */
public final class EconomiePlekken {
    private static final int Y = 67;
    /** The smidse (interior centre, POI_VADSRIJK v0 section 5). */
    public static final BlockPos SMID = new BlockPos(-44, Y, 49);
    /** The kruidenier (drankjes). */
    public static final BlockPos DRANKJES = new BlockPos(-38, Y, 71);
    /** The uitrustingwinkel. */
    public static final BlockPos UITRUSTING = new BlockPos(-29, Y, 18);
    /** The veilinghuis (marktmeester). */
    public static final BlockPos MARKT = new BlockPos(43, Y, 49);
    /** The kleermaker = the cosmetica-ruil ("Thuisbezorger"). */
    public static final BlockPos COSMETICA = new BlockPos(38, Y, 71);
    /** The stal + stalmeester. */
    public static final BlockPos STAL = new BlockPos(-146, Y, 2);

    private EconomiePlekken() {
    }
}
