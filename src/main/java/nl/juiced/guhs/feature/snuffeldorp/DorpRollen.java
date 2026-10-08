package nl.juiced.guhs.feature.snuffeldorp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import nl.juiced.guhs.feature.snuffel.BewonerEntity;
import nl.juiced.guhs.feature.snuffel.Bewoners;
import nl.juiced.guhs.feature.snuffel.Daden;
import nl.juiced.guhs.feature.snuffel.Eiland;
import nl.juiced.guhs.feature.snuffel.Examen;
import nl.juiced.guhs.feature.snuffel.Hondvorm;
import nl.juiced.guhs.feature.snuffel.Keuze;
import nl.juiced.guhs.feature.snuffel.Maatjes;
import nl.juiced.guhs.feature.snuffel.Rang;
import nl.juiced.guhs.feature.snuffel.Snuffel;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * What the ten residents of Snuffeldorp do when a dog talks to them (their keys are the {@code sleutel}s of eiland.json):
 * <ul>
 *   <li>{@code redder} Jutje Kwispel at the strandpoort: welcomes you and sends you to the doctor;</li>
 *   <li>{@code dokter} Dokter Pleisterpoot in his practice: hears your story, sends you to the meadow;</li>
 *   <li>{@code trainer} Meester Truffelneus in the meadow: the three lessons, the exam, the diploma;</li>
 *   <li>{@code bakker}, {@code visser}, {@code juf}, {@code oma}, {@code tuinder}, {@code pup}: each lost something
 *   ({@link Dorp#KLUSSEN}): ask, find, bring back = a good deed;</li>
 *   <li>{@code havenkapitein} Kapitein Zoutsnoet at his boat: sails you home (the scene {@link DorpScenes#AFVAART}), and
 *   gives a new Guhstation to whoever finished the story and lost theirs, and the island's music disc (once) to whoever
 *   finished it before the disc existed. (His key is not {@code kapitein}: that is the
 *   captain at a dock in the Guhmensie, the dock slice's.)</li>
 * </ul>
 * The texts: tools/features/snuffel_dorp_tekst.py. The resident is the same dog for everybody; what it says depends on the
 * player who asks.
 */
public final class DorpRollen {
    public static final String KAPITEIN = "havenkapitein";
    private static final String KAPITEIN_SLEUTEL = "snuffeldorp_kapitein";
    private static final Verhaallijn LIJN = SnuffelFeature.LIJN;

    private DorpRollen() {
    }

    static void init() {
        Bewoners.zetRol("redder", DorpRollen::redder);
        Bewoners.zetRol("dokter", DorpRollen::dokter);
        Bewoners.zetRol("trainer", DorpRollen::trainer);
        for (Dorp.Klus k : Dorp.KLUSSEN) {
            Bewoners.zetRol(k.bewoner(), (npc, p) -> klus(npc, p, k));
        }
        Bewoners.zetRol(KAPITEIN, DorpRollen::kapitein);
        Praat.luister(KAPITEIN_SLEUTEL, (p, spreker, optie) -> {
            if (optie == 1) {
                afvaart(p);
            } else if (optie == 2 && Snuffel.klaar(p)) {
                Snuffel.geefGuhstation(p, true);
                p.sendSystemMessage(Component.translatable(Gesprek.key("kapitein.station", 0)).withStyle(ChatFormatting.AQUA));
            }
        });
    }

    private static String naam(ServerPlayer p) {
        return Keuze.vanOfStandaard(p).naam();
    }

    // --- Jutje Kwispel ----------------------------------------------------------------------------------------------------------

    static void redder(BewonerEntity npc, ServerPlayer p) {
        if (LIJN.klaar(p)) {
            Gesprek.zeg(p, npc, "redder.klaar");
        } else if (LIJN.stap(p) <= SnuffelFeature.STAP_STRAND) {
            Dorp.wakker(p);
            LIJN.verder(p, SnuffelFeature.STAP_STRAND);
            Gesprek.toon(p, npc, "redder.welkom", naam(p));
        } else {
            Gesprek.zeg(p, npc, "redder.dag");
        }
    }

    // --- Dokter Pleisterpoot ----------------------------------------------------------------------------------------------------

    static void dokter(BewonerEntity npc, ServerPlayer p) {
        if (LIJN.klaar(p)) {
            Gesprek.zeg(p, npc, "dokter.klaar");
        } else if (LIJN.stap(p) <= SnuffelFeature.STAP_DOKTER) {
            // (a dog that ran past Jutje still gets its story heard)
            Dorp.wakker(p);
            LIJN.zet(p, SnuffelFeature.STAP_DOKTER);
            LIJN.verder(p, SnuffelFeature.STAP_DOKTER);
            Gesprek.toon(p, npc, "dokter.verhaal", naam(p));
        } else {
            Gesprek.zeg(p, npc, "dokter.dag");
        }
    }

    // --- Meester Truffelneus ----------------------------------------------------------------------------------------------------

    static void trainer(BewonerEntity npc, ServerPlayer p) {
        int stap = LIJN.stap(p);
        if (LIJN.klaar(p)) {
            Gesprek.zeg(p, npc, "trainer.klaar");
        } else if (stap < SnuffelFeature.STAP_LES) {
            Gesprek.zeg(p, npc, "trainer.eerst_dokter");
        } else if (stap == SnuffelFeature.STAP_LES) {
            les(npc, p);
        } else if (stap == SnuffelFeature.STAP_MAATJE) {
            Gesprek.zeg(p, npc, "trainer.kabaal");
        } else if (stap == SnuffelFeature.STAP_DADEN) {
            Gesprek.zeg(p, npc, "trainer.daden", Daden.aantal(p), SnuffelFeature.DADEN_NODIG);
        } else if (stap == SnuffelFeature.STAP_EXAMEN) {
            examen(npc, p);
        } else {
            Gesprek.zeg(p, npc, "trainer.boom");
        }
    }

    /** The lessons: one scent source at a time; the next one only when the dog comes back with the last one found. */
    private static void les(BewonerEntity npc, ServerPlayer p) {
        int les = LIJN.teller(p, Dorp.LES);
        if (les <= 0) {
            LIJN.teller(p, Dorp.LES, 1);
            Gesprek.toon(p, npc, "trainer.les1", naam(p));
        } else if (!LIJN.vlag(p, Dorp.LES_GEVONDEN)) {
            Gesprek.toon(p, npc, "trainer.les" + Math.min(les, Dorp.LESSEN.size()) + "_hint");
        } else if (les < Dorp.LESSEN.size()) {
            LIJN.teller(p, Dorp.LES, les + 1);
            LIJN.vlag(p, Dorp.LES_GEVONDEN, false);
            Gesprek.toon(p, npc, "trainer.les" + (les + 1));
        } else {
            LIJN.verder(p, SnuffelFeature.STAP_LES);
            Gesprek.toon(p, npc, "trainer.lessen_klaar", naam(p));
        }
    }

    /** The exam: start it, a word of courage while it runs, and the diploma once every source is found. */
    private static void examen(BewonerEntity npc, ServerPlayer p) {
        if (LIJN.vlag(p, Dorp.GESLAAGD)) {
            Snuffel.geefDiploma(p);
            LIJN.verder(p, SnuffelFeature.STAP_EXAMEN);
            Maatjes.blij(p);
            Gesprek.toon(p, npc, "trainer.diploma", naam(p), Rang.SNUFFELPUP.regel());
            return;
        }
        Examen.Loop loop = Examen.bezig(p);
        if (loop != null && loop.examen().id().equals(Dorp.EXAMEN)) {
            Gesprek.toon(p, npc, "trainer.examen_hint", loop.gevonden(), loop.examen().bronnen().size());
        } else {
            Snuffel.startExamen(p, Dorp.EXAMEN);
            Gesprek.toon(p, npc, "trainer.examen", naam(p));
        }
    }

    // --- the residents who lost something ---------------------------------------------------------------------------------------

    static void klus(BewonerEntity npc, ServerPlayer p, Dorp.Klus k) {
        String b = k.bewoner();
        if (LIJN.stap(p) < SnuffelFeature.STAP_DADEN) {
            Gesprek.zeg(p, npc, b + ".hallo");
        } else if (Daden.heeft(p, k.daad())) {
            Gesprek.zeg(p, npc, b + ".dank");
        } else if (LIJN.vlag(p, k.gevonden())) {
            // brought back: a good deed. The tree grows with the kern's scene; the thanks come when the scene is over
            Snuffel.goedeDaad(p, k.daad(), null, speler -> Gesprek.zeg(speler, npc, b + ".terug"));
            Maatjes.blij(p);
            if (Dorp.dadenKlaar(p)) {
                p.sendSystemMessage(Component.translatable("gui.guhs.snuffeldorp.daden_klaar").withStyle(ChatFormatting.GOLD));
            }
        } else if (LIJN.vlag(p, k.gevraagd())) {
            Gesprek.toon(p, npc, b + ".hint");
        } else {
            LIJN.vlag(p, k.gevraagd(), true);
            Maatjes.ondeugend(p, 60);
            Gesprek.toon(p, npc, b + ".vraag");
        }
    }

    // --- Kapitein Zoutsnoet -----------------------------------------------------------------------------------------------------

    static void kapitein(BewonerEntity npc, ServerPlayer p) {
        // the island's music disc: at the end of the story it comes with the Guhstation; whoever had finished already gets it here
        if (Snuffel.klaar(p) && Snuffel.geefPlaat(p)) {
            p.sendSystemMessage(Component.translatable(Gesprek.key("kapitein.plaat", 0)).withStyle(ChatFormatting.AQUA));
        }
        if (!Hondvorm.actief(p)) {
            Bewoners.kapitein(npc, p);
        } else if (Snuffel.klaar(p)) {
            Praat.open(p, npc, KAPITEIN_SLEUTEL, Gesprek.key("kapitein.vraag_klaar", 0), new Object[0], new Praat.Optie(1, "gui.guhs.snuffel.optie.naar_huis"),
                    new Praat.Optie(2, "gui.guhs.snuffeldorp.optie.station"), new Praat.Optie(0, "gui.guhs.snuffel.optie.blijven"));
        } else {
            Praat.open(p, npc, KAPITEIN_SLEUTEL, Gesprek.key("kapitein.vraag", 0), new Object[0], new Praat.Optie(1, "gui.guhs.snuffel.optie.naar_huis"),
                    new Praat.Optie(0, "gui.guhs.snuffel.optie.blijven"));
        }
    }

    /** The trip home: the short boat scene on the jetty, then exactly home (without a scene when none can play). */
    static void afvaart(ServerPlayer p) {
        Eiland.Plaats plaats = Eiland.van(p);
        if (plaats == null || !Hondvorm.actief(p)) {
            return;
        }
        Plekken pl = Plekken.van(plaats);
        BlockPos haven = pl == null ? null : pl.wereld(plaats, Plekken.HAVEN);
        if (haven == null || !Cutscenes.speel(p, DorpScenes.AFVAART, haven, Rotation.NONE, Snuffel::naarHuis)) {
            Snuffel.naarHuis(p);
        }
    }
}
