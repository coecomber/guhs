package nl.juiced.guhs.feature.onderwol.structuren3;

import java.util.List;
import java.util.Map;

import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.onderwol.bouw.OnderWolBouw;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * 1.7.0 "Onder de Wol", underground buildings part 3 (design: ontwerp-guhs170-mijnen.md chapter 2.4, rounds 5-20; the
 * resources: tools/features/onderwol_structuren3.py, on the {@code guhs:grot_bouw} framework of {@link OnderWolBouw}):
 * <ul>
 *   <li>af en toe: the Grotdisco, the Oude wolspinnerij (De Wolclub; also a side building of the Mijnguhdorp), the
 *       Sterrengrot, the Knuffelkuil (the six hidden guhs fell asleep: pet them awake), the Verrassingsfeestje;</li>
 *   <li>zeldzaam: the Oude guhtempel (the Grote Guh with the Gouden Kaasknabbel) and the Oude bankkluis (a failed Mika
 *       break-in).</li>
 * </ul>
 * Here: their Superkompas entries (tab "ondergrond"), their talking guhs ({@link #PRAAT}: an existing kind with
 * {@code RoleData:{guhs_plek:"<plek>"}}, lines {@code quest.guhs.onderwol_bouw.<plek>.<1..n>}) and the source of the party
 * clothes ({@link GuhClothes#FEESTHOEDJE}, {@link GuhClothes#SLINGER_SJAALTJE}: the Verrassingsfeestje's chest).
 */
public final class OnderWolStructuren3 {
    /** The structures of this slice, all in the Superkompas tab "ondergrond". */
    public static final List<String> STRUCTUREN = List.of("grotdisco", "oude_wolspinnerij", "sterrengrot", "knuffelkuil", "verrassingsfeestje",
            "oude_guhtempel", "oude_bankkluis");

    /** The talking guhs: plek -> (kind, number of lines); the same pleks and counts as the python PRAAT. */
    public static final Map<String, OnderWolBouw.Praat> PRAAT = Map.of(
            "onderwol_disco", new OnderWolBouw.Praat(GuhNpcEntity.Kind.KROEGBAASGUH, 5),
            "onderwol_spinnerij", new OnderWolBouw.Praat(GuhNpcEntity.Kind.KNUFFELMAKERGUH, 5),
            "onderwol_sterren", new OnderWolBouw.Praat(GuhNpcEntity.Kind.STERRENWACHT_RUINE_STERRENKIJKER, 4),
            "onderwol_kuil", new OnderWolBouw.Praat(GuhNpcEntity.Kind.GUHKINDJE, 5),
            "onderwol_feestje", new OnderWolBouw.Praat(GuhNpcEntity.Kind.GUHKINDJE, 5),
            "onderwol_tempel", new OnderWolBouw.Praat(GuhNpcEntity.Kind.ARCHEOLOOGGUH, 5),
            "onderwol_bank", new OnderWolBouw.Praat(GuhNpcEntity.Kind.WACHTERGUH, 5));

    /** The clothes of the Verrassingsfeestje (its chest, loot table guhs:chests/onderwol/verrassingsfeestje). */
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.FEESTHOEDJE, GuhClothes.SLINGER_SJAALTJE);
    public static final String KLEDINGBRON = "loot_grotten";

    public static void register(IEventBus modBus) {
        PRAAT.forEach((plek, p) -> NpcRollen.zet(p.kind(), plek, OnderWolBouw.rol(plek, p.regels())));
        for (String s : STRUCTUREN) {
            SuperkompasItem.voegToe("ondergrond", s);
        }
        for (GuhClothes c : KLEDING) {
            KledingBronnen.bron(c, KLEDINGBRON, KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.verrassingsfeestje_onder_de_wol"));
        }
    }

    private OnderWolStructuren3() {
    }
}
