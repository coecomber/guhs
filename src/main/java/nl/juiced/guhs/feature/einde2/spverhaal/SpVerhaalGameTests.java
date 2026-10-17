package nl.juiced.guhs.feature.einde2.spverhaal;

import java.util.List;
import java.util.Map;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.feature.einde2.Einde2Haken;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.reizen.E2Reizen;
import nl.juiced.guhs.feature.einde2.verhaal.E2Hervat;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.taal.NlTekst;

/**
 * einde2 "Sponsguh", the skelet (CONTRACT_E2_C 8.3): the gate (shut before the Guhvatar story is done, open after it, open for
 * a guest of a host who did it), the whole chain of both lines walked with the stand-ins (idempotent: a step never moves twice),
 * what the chain derives (the recipe hook, Appa's rows, "Verder met het verhaal", the big story), and that every default place,
 * step text and reserved variant exists. Holds before and after the wave: a step a slice owns is walked with its line's
 * {@code zet} instead of the stand-in.
 */
public class SpVerhaalGameTests {
    private static final String BATCH = "einde2_sp_verhaal";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    /** One step on: the stand-in when it is one, else the line itself (a real step of a slice). */
    private static boolean verder(ServerPlayer p, String lijn, int stap) {
        return SpStappen.isEcht(lijn, stap) ? SpVerhaal.lijn(lijn).verder(p, stap) : SpStandIn.voltooi(p, lijn, stap);
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void spVerhaalKeten(GameTestHelper helper) {
        ServerPlayer p = speler(helper), gast = speler(helper);
        String bb = SpVerhaal.BIKINIBODEM, sp = SpVerhaal.SCHELPENSTAD;
        try {
            // before the Guhvatar story: shut, with the friendly lines once per while
            helper.assertTrue(!SpVerhaal.magBeginnen(p) && !SpVerhaal.open(p) && Sluiers.isVerborgen(p, bb) && Sluiers.isVerborgen(p, sp)
                    && SpVerhaal.gebouwen(p).isEmpty() && !E2Verhaal.bekendeGebouwen(p, false).contains(bb) && SpVerhaal.reisDoel(p) == null,
                    "before the Guhvatar story: both places shut");
            helper.assertTrue(SpPoort.vertel(p, 1000) && !SpPoort.vertel(p, 1000 + SpPoort.RUST - 1) && SpPoort.vertel(p, 1000 + SpPoort.RUST),
                    "the gate's three lines at most once per " + SpPoort.RUST + " ticks");
            helper.assertTrue("gui.guhs.einde2_sp_verhaal.poort.balk".equals(Sluiers.BERICHTEN.get(bb)) && Sluiers.BERICHTEN.get(sp) != null
                    && Sluiers.BERICHTEN.get(E2Verhaal.structuurVan(E2Verhaal.LUCHT)) == null, "the Sponsguh line over the hotbar, Guhdalf's elsewhere");
            // a guest of a host who did the Guhvatar story may in
            SpVerhaalDev.guhvatarKlaar(gast);
            SpVerhaal.gastheer = q -> q == p ? gast : null;
            helper.assertTrue(SpVerhaal.open(p) && Sluiers.open(p, bb) && !SpVerhaal.magBeginnen(p), "the guest with the host: open");
            SpVerhaal.gastheer = E2Reizen::gastheer;
            helper.assertTrue(!SpVerhaal.open(p), "the host gone: shut again");
            // the Guhvatar story done: open, Appa knows Bikinibodem (not the meditation stones), part 1 can begin
            SpVerhaalDev.guhvatarKlaar(p);
            helper.assertTrue(SpVerhaal.open(p) && Sluiers.open(p, bb) && Sluiers.open(p, sp) && SpVerhaal.gebouwen(p).equals(List.of(bb))
                    && E2Verhaal.bekendeGebouwen(p, false).contains(bb) && !E2Verhaal.bekendeGebouwen(p, true).contains(bb),
                    "the Guhvatar story done: open, Bikinibodem on Appa's list");
            helper.assertTrue(E2Hervat.huidig(p) == null, "not begun: no story to go on with");
            SpVerhaal.RECEPT_LIJN.begin(p);
            helper.assertTrue(E2Hervat.huidig(p) == SpVerhaal.RECEPT_LIJN && E2Hervat.isEinde2(SpVerhaal.RECEPT_LIJN)
                    && SpVerhaal.reisDoel(p).equals(bb) && !SpVerhaal.actief(p, SpVerhaal.KROON), "part 1 begun: it is the story now");
            // part 1, every step, each exactly once
            for (int s = 0; s < SpVerhaal.R_STAPPEN; s++) {
                helper.assertTrue(SpVerhaal.kentRecept(p) == (s > SpVerhaal.R_TERUG), "the recipe known only after step " + SpVerhaal.R_TERUG + " (at " + s + ")");
                helper.assertTrue(verder(p, SpVerhaal.RECEPT, s) && !verder(p, SpVerhaal.RECEPT, s) && SpVerhaal.stap(p, SpVerhaal.RECEPT) == s + 1,
                        "part 1 step " + s + " moves on once");
            }
            helper.assertTrue(SpVerhaal.receptKlaar(p) && Einde2Haken.KNABBELBURGER_RECEPT.test(p) && !Einde2Haken.KNABBELBURGER_RECEPT.test(gast)
                    && Einde2Haken.VERHAAL_KLAAR.test(p, SpVerhaal.RECEPT), "part 1 done: the grill bakes for this player");
            helper.assertTrue(SpVerhaal.KROON_LIJN.aanDeBeurt(p) && SpVerhaal.actief(p, SpVerhaal.KROON) && E2Hervat.huidig(p) == SpVerhaal.KROON_LIJN,
                    "part 2 follows part 1");
            // part 2
            for (int s = 0; s < SpVerhaal.K_STAPPEN; s++) {
                String doel = SpVerhaal.reisDoel(p);
                helper.assertTrue(doel.equals(s >= SpVerhaal.K_SLEUTEL && s <= SpVerhaal.K_HASSELGUH ? sp : bb), "part 2 step " + s + ": Appa's place " + doel);
                helper.assertTrue(SpVerhaal.gebouwen(p).contains(sp) == (s >= SpVerhaal.K_RIT), "Schelpenstad on Appa's list from the drive on (at " + s + ")");
                helper.assertTrue(verder(p, SpVerhaal.KROON, s) && !verder(p, SpVerhaal.KROON, s), "part 2 step " + s + " moves on once");
            }
            GroteVerhalen.GrootVerhaal groot = GroteVerhalen.van(SpVerhaal.GROOT);
            helper.assertTrue(SpVerhaal.klaar(p) && groot != null && groot == SpVerhaal.grootVerhaal() && groot.klaar(p) && !groot.klaar(gast)
                    && groot.wereld() == GroteVerhalen.Wereld.GUHEINDE && groot.lijnen().equals(SpVerhaal.LIJNEN) && groot.structuren().equals(List.of(bb))
                    && GroteVerhalen.wereldVan(SpVerhaal.KROON) == GroteVerhalen.Wereld.GUHEINDE, "the whole story: the big story of the Guhpad is done");
            helper.assertTrue(E2Hervat.huidig(p) == null && SpVerhaal.reisDoel(p) == null && Einde2Haken.groteVerhalenKlaar(p) >= 2,
                    "everything done: no story on, both parts count");
            // the dev state helpers
            SpVerhaalDev.zetTot(p, SpVerhaal.KROON, SpVerhaal.K_DENNIS);
            helper.assertTrue(SpVerhaal.receptKlaar(p) && SpVerhaal.stap(p, SpVerhaal.KROON) == SpVerhaal.K_DENNIS, "zetTot: part 1 done, part 2 at Dennis");
            SpVerhaalDev.wis(p);
            helper.assertTrue(SpVerhaal.stap(p, SpVerhaal.RECEPT) == 0 && !SpVerhaal.RECEPT_LIJN.begonnen(p) && SpVerhaal.open(p), "wiped: the gate stays open");
        } finally {
            SpVerhaal.gastheer = E2Reizen::gastheer;
            for (ServerPlayer q : List.of(p, gast)) {
                SpVerhaalDev.wis(q);
                for (String l : E2Verhaal.HOOFDSTUKKEN) {
                    E2VerhaalDev.wis(q, l);
                }
                helper.getLevel().removePlayerImmediately(q, Entity.RemovalReason.DISCARDED);
            }
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void spVerhaalPlekkenEnTeksten(GameTestHelper helper) {
        // every default place is a marker of the build it names
        for (Map.Entry<String, List<SpStappen.Plek>> e : SpStappen.STANDIN.entrySet()) {
            Verhaallijn l = SpVerhaal.lijn(e.getKey());
            helper.assertTrue(e.getValue().size() == l.stappen(), e.getKey() + ": a default place for every step");
            for (int s = 0; s < l.stappen(); s++) {
                SpStappen.Plek plek = SpStappen.standIn(e.getKey(), s);
                helper.assertTrue(plek == null ? s == SpVerhaal.R_BRIEF && l == SpVerhaal.RECEPT_LIJN
                        : Plekken.van(plek.structuur()).containsKey(plek.plek()), e.getKey() + " step " + s + ": the marker " + plek);
                String base = "gui.guhs.verhalen." + e.getKey() + ".";
                for (String k : List.of("stap." + s, "nu." + s, "waar." + s, "kort." + s)) {
                    helper.assertTrue(NlTekst.has(base + k), "the text " + base + k);
                }
            }
            for (String v : l.extraSleutels()) {
                helper.assertTrue(Integer.parseInt(v.substring(0, v.indexOf('_'))) < l.stappen() && NlTekst.has("gui.guhs.verhalen." + e.getKey() + ".nu." + v)
                        && NlTekst.has("gui.guhs.verhalen." + e.getKey() + ".waar." + v), e.getKey() + ": the variant " + v);
            }
            helper.assertTrue(l.extraSleutels().equals(SpStappen.VARIANTEN.get(e.getKey())) && NlTekst.has("gui.guhs.verhalen." + e.getKey() + ".naam")
                    && NlTekst.has("gui.guhs.verhalen." + e.getKey() + ".nu.klaar"), e.getKey() + ": name, done, variants");
        }
        for (String k : List.of("poort.balk", "poort.1", "poort.2", "poort.3", "standin.stap", "standin.klaar", "standin.lijn_klaar")) {
            helper.assertTrue(NlTekst.has(SpVerhaal.P + k), "the text " + SpVerhaal.P + k);
        }
        helper.assertTrue(NlTekst.has("gui.guhs.verhalen.kop." + SpVerhaal.GROEP) && NlTekst.has("gui.guhs.guhpad.verhaal." + SpVerhaal.GROOT),
                "the heading and the big story's name");
        helper.succeed();
    }
}
