package nl.juiced.guhs.feature.bio.systemen;

import java.util.List;

import net.minecraft.ChatFormatting;
import nl.juiced.guhs.feature.bio.bouwdal.Cadeaus;
import nl.juiced.guhs.feature.bio.bouwmeer.Visserguh;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * biomes3: the four titles of the three new biomes. Titels.ALLE ends with this list (one line there). Like every title
 * they read saved progress, there is no "has title" flag; names {@code gui.guhs.titels.naam.<id>}, hints
 * {@code gui.guhs.titels.hint.<id>} (tools/features/bio_systemen.py).
 * <ul>
 *   <li>{@link #WEEB} "Weeb": the complete Japan collection of Evivads and Nielsvads (all twelve pieces were given);</li>
 *   <li>{@link #WOLKENTOP} "Hoofd in de wolken": stood on the summit of a stack of cloud islands ({@link Bewijzen#opTop});</li>
 *   <li>{@link #GEZONDHEID} "Gezondheid!": sneezed out of the cloud castle by the giant three times;</li>
 *   <li>{@link #KOIFLUISTERAAR} "Koifluisteraar": finished the visser-guh's lessons at a botenhuisje.</li>
 * </ul>
 */
public final class BioTitels {
    public static final String WEEB = "biosystemen_weeb", WOLKENTOP = "biosystemen_wolkentop", GEZONDHEID = "biosystemen_gezondheid",
            KOIFLUISTERAAR = "biosystemen_koifluisteraar";

    public static final List<Titels.Titel> ALLE = List.of(
            new Titels.Titel(WEEB, "gui.guhs.titels.naam." + WEEB, ChatFormatting.LIGHT_PURPLE, "guhs:japan_geluksguh",
                    p -> (Cadeaus.reeks(p) & Cadeaus.REEKS_VOL) == Cadeaus.REEKS_VOL),
            new Titels.Titel(WOLKENTOP, "gui.guhs.titels.naam." + WOLKENTOP, ChatFormatting.AQUA, "guhs:wolkenblok_wit",
                    p -> GuhQuests.saved(p).getBooleanOr(Bewijzen.TOP_KEY, false)),
            new Titels.Titel(GEZONDHEID, "gui.guhs.titels.naam." + GEZONDHEID, ChatFormatting.YELLOW, "guhs:wolkenkasteeltje_kruimel",
                    p -> Bewijzen.niezen(p) >= Bewijzen.NIEZEN),
            new Titels.Titel(KOIFLUISTERAAR, "gui.guhs.titels.naam." + KOIFLUISTERAAR, ChatFormatting.GOLD, "guhs:koivoer",
                    p -> Visserguh.stap(p) >= Visserguh.KLAAR));

    private BioTitels() {
    }
}
