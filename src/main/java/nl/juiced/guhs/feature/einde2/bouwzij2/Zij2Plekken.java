package nl.juiced.guhs.feature.einde2.bouwzij2;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;

/**
 * The markers of the five places of this slice that come in numbered groups or as a course, read from the sidecars
 * ({@link Plekken}): what a later phase asks for instead of counting ids itself. Everything is in TEMPLATE coordinates
 * unless a level is passed in (then: the one guaranteed copy of that world).
 */
public final class Zij2Plekken {
    /** The five places of this slice. */
    public static final List<String> PLEKKEN = List.of(Einde2Ids.KYOSHI_DORP, Einde2Ids.BIBLIOTHEEK_WAN_SHI_GUH, Einde2Ids.LEEUWSCHILDPAD_EILAND,
            Einde2Ids.LEEGTEHAVEN, Einde2Ids.GUHVLEUGEL_RINGENRACE);

    /** The markers {@code <naam>_1}, {@code <naam>_2}, ... (or {@code _01}, {@code _02}, ...) of a place, in order. */
    public static List<Plekken.Plek> groep(String structuur, String naam) {
        List<Plekken.Plek> out = new ArrayList<>();
        for (int i = 1; ; i++) {
            Plekken.Plek plek = Plekken.plek(structuur, naam + "_" + i);
            if (plek == null) {
                plek = Plekken.plek(structuur, naam + "_" + (i < 10 ? "0" : "") + i);
            }
            if (plek == null) {
                return out;
            }
            out.add(plek);
        }
    }

    // hook fase B: the race walks this list (ring i is passed when a racer crosses its plane within straal() of pos())
    /** The rings of the Guhvleugel-ringenrace in the order they are flown: {@code ring_01} first. */
    public static List<Plekken.Plek> ringen() {
        return groep(Einde2Ids.GUHVLEUGEL_RINGENRACE, "ring");
    }

    // hook fase B: the traders of the Leegtehaven (their entities stand on these spots; trading is a later phase)
    /** The four stalls of the market of the Leegtehaven: where each Ender-Mika trader stands. */
    public static List<Plekken.Plek> handelaars() {
        return groep(Einde2Ids.LEEGTEHAVEN, "handelaar");
    }

    // hook fase B: where new Guhboeken appear
    /** The golden cabinets of the Bibliotheek van Wan Shi Guh, from the top hall down to the uilenzaal. */
    public static List<Plekken.Plek> guhboekKasten() {
        return groep(Einde2Ids.BIBLIOTHEEK_WAN_SHI_GUH, "guhboek_kast");
    }

    /** A marker of the one guaranteed copy of a place in this world (null: no such marker, or no copy). */
    @Nullable
    public static BlockPos wereld(ServerLevel guheinde, String structuur, String plek) {
        return Plekken.wereld(guheinde, structuur, plek);
    }

    private Zij2Plekken() {
    }
}
