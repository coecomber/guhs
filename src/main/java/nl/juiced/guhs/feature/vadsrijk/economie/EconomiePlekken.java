package nl.juiced.guhs.feature.vadsrijk.economie;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Every coordinate the economie slice uses (SLICE_BRIEF). The economie NPCs themselves (smidguh, winkelguhs, marktmeester,
 * stalmeester) are the npc slice's; these are the PROPOSED spots in Zwamzoet (box -160..160) asked for in POI_VERZOEKEN.md, used
 * by the dev command {@code /guhs vadsrijk eco ga <plek>} until the wereld slice's POI_VADSRIJK.md has the real ones.
 */
public final class EconomiePlekken {
    private static final int Y = Vadsrijk.GROND + 1;
    /** The smidse (smid + anvil), east side of the market square. */
    public static final BlockPos SMID = new BlockPos(28, Y, 6);
    /** The drankjeswinkel and the smidwaren/uitrusting shop, around the square. */
    public static final BlockPos DRANKJES = new BlockPos(-24, Y, 10);
    public static final BlockPos UITRUSTING = new BlockPos(-24, Y, -12);
    /** The marktmeester + the veilinghuis board, on the square ("Beneden: de markt en het plein"). */
    public static final BlockPos MARKT = new BlockPos(0, Y, 24);
    /** The cosmetica-ruil (the "Thuisbezorger" guh), next to the market. */
    public static final BlockPos COSMETICA = new BlockPos(12, Y, 28);
    /** The stal + stalmeester, at the city gate towards the Klodderweide (east). */
    public static final BlockPos STAL = new BlockPos(120, Y, 0);

    private EconomiePlekken() {
    }
}
