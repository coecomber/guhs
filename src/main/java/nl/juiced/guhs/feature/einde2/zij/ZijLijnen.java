package nl.juiced.guhs.feature.einde2.zij;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import nl.juiced.guhs.feature.einde2.Einde2Haken;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 zij (CONTRACT_E2_B 2.1, 7.6): the eight side questlines of the Guheinde, all in the group {@code guheinde}, every
 * step per player in main's {@link Verhaallijn} (texts: tools/features/einde2_zij_tekst.py {@code LIJNEN}). The four
 * element places ask their element in their first step ({@link #element}); none has a {@code na}.
 * <ul>
 *   <li>{@code omashguh} (5): koning Bumi-guh's three trials that work otherwise than you think, then the chute race;</li>
 *   <li>{@code westelijke_luchttempel} (4): make the five wind corridors sing, bring the strayed herd home;</li>
 *   <li>{@code vuurtempel_roku} (4): light the five fire locks, the sun beam, talk with Roku-guh;</li>
 *   <li>{@code kyoshi_dorp} (4): the fan dance, the make-up, the foaming fan;</li>
 *   <li>{@code bibliotheek_wan_shi_guh} (4): the owl's rules, a piece of knowledge, a fox guh's book, the planetarium;</li>
 *   <li>{@code leeuwschildpad} (3): over the shell, the raised paw, a knabbel and the one blessing;</li>
 *   <li>{@code mika_kok} (5): the sorry Mika cook of a vesting, his ingredients, the captain distracted, his move to the harbour;</li>
 *   <li>{@code vetbootje} (3): the notice on a vesting's harbour board, the shipyard, your own vetbootje.</li>
 * </ul>
 */
public final class ZijLijnen {
    public static final String GROEP = Einde2Haken.VERHAAL_GROEP;
    public static final String OMASHGUH = "omashguh", LUCHTTEMPEL = "westelijke_luchttempel", VUURTEMPEL = "vuurtempel_roku",
            KYOSHI = "kyoshi_dorp", BIBLIOTHEEK = "bibliotheek_wan_shi_guh", LEEUWSCHILDPAD = "leeuwschildpad", MIKA_KOK = "mika_kok",
            VETBOOTJE = "vetbootje";
    /** Every side line, in the order of the Guhdex and of the FTB section. */
    public static final List<String> ALLE = List.of(OMASHGUH, LUCHTTEMPEL, VUURTEMPEL, KYOSHI, BIBLIOTHEEK, LEEUWSCHILDPAD, MIKA_KOK, VETBOOTJE);

    /** The structure each line plays in (the Mika-kok and the vetbootje begin in a Mika-vesting and end in the harbour). */
    public static String structuur(String lijn) {
        return switch (lijn) {
            case LEEUWSCHILDPAD -> Einde2Ids.LEEUWSCHILDPAD_EILAND;
            case MIKA_KOK, VETBOOTJE -> Einde2Ids.MIKA_VESTING;
            default -> lijn;
        };
    }

    /** The element a line's first step asks for (null: none). */
    public static Einde2Ids.Element element(String lijn) {
        return Einde2Ids.ELEMENT_VAN.get(structuur(lijn));
    }

    private static Doel naar(String structuur) {
        return Doel.structuur(GuheindeFeature.GUHEINDE, structuur, Component.translatable("structure.guhs." + structuur));
    }

    private static VerhaalStand.Nodig nodig(ServerPlayer p, Item item, int n) {
        return Verhaallijn.nodig(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString(), GuhQuests.count(p, item), n);
    }

    public static final Verhaallijn OMASH = Verhaallijn.maak(OMASHGUH, GROEP).stappen(5).icoon("guhs:einde2_zij_kristalsnoep_ring")
            .sleutel((p, stap) -> stap == 0 && !ZijKrachten.geleerd(p, Einde2Ids.Element.AARDE) ? "0_element" : String.valueOf(stap))
            .extraSleutels("0_element")
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:einde2_zij_kristalsnoep_ring", eind(p, OMASHGUH))))
            .doel((p, stap) -> naar(Einde2Ids.OMASHGUH))
            .registreer();

    public static final Verhaallijn LUCHT = Verhaallijn.maak(LUCHTTEMPEL, GROEP).stappen(4).icoon("guhs:einde2_deco_windgong")
            .sleutel((p, stap) -> stap == 0 && !ZijKrachten.geleerd(p, Einde2Ids.Element.LUCHT) ? "0_element" : String.valueOf(stap))
            .extraSleutels("0_element")
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:einde2_deco_windgong", "gui.guhs.einde2_zij.beloning.windgongen", eind(p, LUCHTTEMPEL))))
            .doel((p, stap) -> naar(Einde2Ids.WESTELIJKE_LUCHTTEMPEL))
            .registreer();

    public static final Verhaallijn VUUR = Verhaallijn.maak(VUURTEMPEL, GROEP).stappen(4).icoon("guhs:einde2_deco_eeuwige_vlam")
            .sleutel((p, stap) -> stap == 0 && !ZijKrachten.geleerd(p, Einde2Ids.Element.VUUR) ? "0_element" : String.valueOf(stap))
            .extraSleutels("0_element")
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:einde2_deco_eeuwige_vlam", "gui.guhs.einde2_zij.beloning.vlam", eind(p, VUURTEMPEL))))
            .doel((p, stap) -> naar(Einde2Ids.VUURTEMPEL_ROKU))
            .registreer();

    public static final Verhaallijn KYOSHI_LIJN = Verhaallijn.maak(KYOSHI, GROEP).stappen(4).icoon("guhs:einde2_zij_kyoshi_schmink")
            .sleutel((p, stap) -> stap == 0 && !ZijKrachten.geleerd(p, Einde2Ids.Element.WATER) ? "0_element" : String.valueOf(stap))
            .extraSleutels("0_element")
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:einde2_zij_kyoshi_schmink", "gui.guhs.einde2_zij.beloning.schmink",
                    KyoshiDorp.geschminkt(p))))
            .doel((p, stap) -> naar(Einde2Ids.KYOSHI_DORP))
            .registreer();

    public static final Verhaallijn BIEB = Verhaallijn.maak(BIBLIOTHEEK, GROEP).stappen(4).icoon("minecraft:written_book")
            .nodig((p, stap) -> stap == 1 ? List.of(Verhaallijn.nodig("minecraft:filled_map", "gui.guhs.einde2_zij.nodig.kennis",
                    Bibliotheek.gebracht(p), Bibliotheek.KENNIS_NODIG)) : List.of())
            .beloningen(p -> List.of(Verhaallijn.beloning("minecraft:written_book", "gui.guhs.einde2_zij.beloning.boeken", eind(p, BIBLIOTHEEK))))
            .doel((p, stap) -> naar(Einde2Ids.BIBLIOTHEEK_WAN_SHI_GUH))
            .registreer();

    public static final Verhaallijn SCHILDPAD = Verhaallijn.maak(LEEUWSCHILDPAD, GROEP).stappen(3).icoon("guhs:einde2_zij_zegenschub")
            .nodig((p, stap) -> stap == 2 ? List.of(Verhaallijn.nodig("guhs:kaas_knabbels", GuhQuests.count(p, Leeuwschildpad.knabbel()), 1)) : List.of())
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:einde2_zij_zegenschub", eind(p, LEEUWSCHILDPAD))))
            .doel((p, stap) -> naar(Einde2Ids.LEEUWSCHILDPAD_EILAND))
            .registreer();

    public static final Verhaallijn KOK = Verhaallijn.maak(MIKA_KOK, GROEP).stappen(5).icoon("guhs:einde2_zij_spijtsoep")
            .nodig((p, stap) -> stap == 1 ? MikaKok.nodig(p) : List.of())
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:einde2_zij_spijtsoep", "gui.guhs.einde2_zij.beloning.recept", eind(p, MIKA_KOK))))
            .doel((p, stap) -> naar(stap >= 4 ? Einde2Ids.LEEGTEHAVEN : Einde2Ids.MIKA_VESTING))
            .registreer();

    public static final Verhaallijn BOOTJE = Verhaallijn.maak(VETBOOTJE, GROEP).stappen(3).icoon("guhs:vetbootje_fluitje")
            .nodig((p, stap) -> stap == 1 ? VetbootjeOpdracht.nodig(p) : List.of())
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:vetbootje_fluitje", "gui.guhs.einde2_zij.beloning.vetbootje", eind(p, VETBOOTJE))))
            .doel((p, stap) -> naar(stap >= 1 ? Einde2Ids.LEEGTEHAVEN : Einde2Ids.MIKA_VESTING))
            .registreer();

    private static boolean eind(ServerPlayer p, String lijn) {
        Verhaallijn l = van(lijn);
        return l != null && l.klaar(p);
    }

    /** The line of this id (null: not one of ours). */
    public static Verhaallijn van(String lijn) {
        return switch (lijn) {
            case OMASHGUH -> OMASH;
            case LUCHTTEMPEL -> LUCHT;
            case VUURTEMPEL -> VUUR;
            case KYOSHI -> KYOSHI_LIJN;
            case BIBLIOTHEEK -> BIEB;
            case LEEUWSCHILDPAD -> SCHILDPAD;
            case MIKA_KOK -> KOK;
            case VETBOOTJE -> BOOTJE;
            default -> null;
        };
    }

    /** Every line object (forces the class to load: they register themselves). */
    public static List<Verhaallijn> lijnen() {
        List<Verhaallijn> uit = new ArrayList<>();
        for (String id : ALLE) {
            uit.add(van(id));
        }
        return uit;
    }

    private ZijLijnen() {
    }
}
