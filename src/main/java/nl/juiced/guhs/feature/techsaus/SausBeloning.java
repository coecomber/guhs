package nl.juiced.guhs.feature.techsaus;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * What a player gets for the first time a sauce machine did something for them: the hidden advancement
 * {@code guhs:quest/tech_vloeistof_<naam>} (for FTB tasks: {@code fq.adv("tech_vloeistof_<naam>")}) and the visible one
 * {@code guhs:techniek/tech_vloeistof_<naam>} in the tab Guh-technologie. Both come from tools/features/tech_vloeistof.py.
 */
public final class SausBeloning {
    /** A Sauspomp of yours lifted a whole bucket. */
    public static final String GEPOMPT = "gepompt";
    /** You tapped a bucket out of a Sausvat. */
    public static final String GETAPT = "getapt";
    /** A Brouwautomaat brewed (yours, or you took the drankjes out). */
    public static final String GEBROUWEN = "gebrouwen";
    /** A Frituurautomaat fried. */
    public static final String GEFRITUURD = "gefrituurd";
    /** A Grillkoolpers pressed a block of grillkool. */
    public static final String GEPERST = "geperst";

    public static void geef(ServerPlayer player, String naam) {
        GuhAdvancements.grant(player, "tech_vloeistof_" + naam);
        GidsFeature.grant(player, "techniek/tech_vloeistof_" + naam);
    }

    private SausBeloning() {
    }
}
