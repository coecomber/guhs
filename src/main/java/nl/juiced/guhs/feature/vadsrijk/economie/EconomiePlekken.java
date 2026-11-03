package nl.juiced.guhs.feature.vadsrijk.economie;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * Every coordinate the economie slice uses (SLICE_BRIEF). The economie NPCs themselves (smidguh, winkelguhs, marktmeester,
 * stalmeester) are the npc slice's; these are the spots of the real map (WereldPlekken / POI_VADSRIJK.md v1, integration), used by the dev
 * command {@code /guhs vadsrijk eco ga <plek>}.
 */
public final class EconomiePlekken {
    /** The smidse (interior centre, POI_VADSRIJK v0 section 5). */
    public static final BlockPos SMID = WereldPlekken.plek("gebouw.smidse.1.binnen");
    /** The kruidenier (drankjes). */
    public static final BlockPos DRANKJES = WereldPlekken.plek("gebouw.kruidenier.1.binnen");
    /** The uitrustingwinkel. */
    public static final BlockPos UITRUSTING = WereldPlekken.plek("gebouw.uitrusting.1.binnen");
    /** The veilinghuis (marktmeester). */
    public static final BlockPos MARKT = WereldPlekken.plek("gebouw.veilinghuis.1.binnen");
    /** The kleermaker = the cosmetica-ruil ("Thuisbezorger"). */
    public static final BlockPos COSMETICA = WereldPlekken.plek("gebouw.kleermaker.1.binnen");
    /** The stal + stalmeester. */
    public static final BlockPos STAL = WereldPlekken.plek("stal.binnen");

    private EconomiePlekken() {
    }
}
