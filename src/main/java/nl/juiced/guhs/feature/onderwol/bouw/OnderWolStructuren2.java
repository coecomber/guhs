package nl.juiced.guhs.feature.onderwol.bouw;

import java.util.List;
import java.util.Map;

import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * 1.7.0 "Onder de Wol", underground buildings part 2 (design: ontwerp-guhs170-mijnen.md chapter 2.4, rounds 4-20): eight
 * "af en toe" guhs:grot_bouw structures made by tools/features/onderwol_structuren2.py (no Java of their own beyond this:
 * the framework is {@link OnderWolBouw}). The Kaasmakerij and the Kristalkwekerij are also side buildings of the
 * Mijnguhdorp (pool guhs:onderwol/mijnguhdorp/zij). The Badhuis De Warme Knabbel has the only new clothes: the
 * {@link GuhClothes#HANDDOEK_TULBAND} (and the existing badjasje) in its chests.
 */
public final class OnderWolStructuren2 {
    /** The structures of this slice, all in the Superkompas tab "ondergrond". */
    public static final List<String> STRUCTUREN = List.of("verlaten_sledestation", "kaasmakerij", "verzonken_guhhuisje", "reuzenguh_fossiel",
            "kristalkwekerij", "verdwaalde_bouwploeg", "grotguh_holen", "badhuis");

    /** The side buildings of the Mijnguhdorp (its pool guhs:onderwol/mijnguhdorp/zij). */
    public static final List<String> DORP_ZIJ = List.of("kaasmakerij", "kristalkwekerij");

    /** The talking guhs: plek -> (kind, number of lines quest.guhs.onderwol_bouw.&lt;plek&gt;.&lt;1..n&gt;). */
    public static final Map<String, OnderWolBouw.Praat> PRAAT = Map.of(
            "onderwol_station", new OnderWolBouw.Praat(GuhNpcEntity.Kind.WISSELGUH, 4),
            "onderwol_kaas", new OnderWolBouw.Praat(GuhNpcEntity.Kind.KRAAMGUH, 4),
            "onderwol_huisje", new OnderWolBouw.Praat(GuhNpcEntity.Kind.OMAGUH, 4),
            "onderwol_fossiel", new OnderWolBouw.Praat(GuhNpcEntity.Kind.ARCHEOLOOGGUH, 4),
            "onderwol_kweker", new OnderWolBouw.Praat(GuhNpcEntity.Kind.PEPERTELERGUH, 4),
            "onderwol_bouwploeg", new OnderWolBouw.Praat(GuhNpcEntity.Kind.TIMMERGUH, 4),
            "onderwol_grotguh", new OnderWolBouw.Praat(GuhNpcEntity.Kind.KAMPBAASGUH, 4),
            "onderwol_badmeester", new OnderWolBouw.Praat(GuhNpcEntity.Kind.WACHTERGUH, 4),
            "onderwol_barguh", new OnderWolBouw.Praat(GuhNpcEntity.Kind.KRAAMGUH, 4));

    /** The source of the towel turban (the Kleding tab). */
    public static final String KLEDINGBRON = "loot_badhuis";

    public static void register() {
        PRAAT.forEach((plek, p) -> NpcRollen.zet(p.kind(), plek, OnderWolBouw.rol(plek, p.regels())));
        for (String s : STRUCTUREN) {
            SuperkompasItem.voegToe("ondergrond", s);
        }
        KledingBronnen.bron(GuhClothes.HANDDOEK_TULBAND, KLEDINGBRON, KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.in_badhuis_de_warme_knabbel"));
    }

    private OnderWolStructuren2() {
    }
}
