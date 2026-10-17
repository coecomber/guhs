package nl.juiced.guhs.feature.einde2.spd1start;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaalDev;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.taal.NlTekst;

/**
 * einde2 "Sponsguh", slice sp_d1_start (CONTRACT_E2_C 8.3): the core of steps 0-4. The headless test server has no Guheinde,
 * so the places are not asked here; what is tested is the state machine around them.
 * <ul>
 *   <li>{@code einde2SpD1StartGrill}: the rules of the grill game (five orders, a wrong part restarts only that order), then the
 *       game for a mock player on the test floor: five crates, the variant 4_b, a wrong part changes no counter, five orders
 *       end it and the line goes on to step 5 (the lesson and the end scene play as a mock plays them);</li>
 *   <li>{@code einde2SpD1StartBriefEnBaan}: the bubble letter is one player's own and a click on it moves only their step (0 to
 *       1), another player's click does nothing; the job gives the cap once and moves 3 to 4; the clicks rule (sp_zij's
 *       characters from step 3 on); every step has its hint; every line and speaker name of the scenes has a text.</li>
 * </ul>
 */
public class E2SpD1StartGameTests {
    private static final String BATCH = "einde2_sp_d1_start";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer q : spelers) {
            Bakken.vergeet(q.getUUID());
            Bubbelbrief.vergeet(q.getUUID());
            StapHint.vergeet(q.getUUID());
            SpVerhaalDev.wis(q);
            for (String l : E2Verhaal.HOOFDSTUKKEN) {
                E2VerhaalDev.wis(q, l);
            }
            helper.getLevel().removePlayerImmediately(q, Entity.RemovalReason.DISCARDED);
        }
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2SpD1StartGrill(GameTestHelper helper) {
        // the rules alone
        GrillSpel r = new GrillSpel(0);
        List<GrillSpel.Deel> eerste = r.bestelling();
        helper.assertTrue(r.leg(eerste.get(0)) == GrillSpel.Uitslag.GOED && r.gelegd() == 1, "the first bun goes on");
        GrillSpel.Deel fout = eerste.get(1) == GrillSpel.Deel.AUGURK ? GrillSpel.Deel.KAAS : GrillSpel.Deel.AUGURK;
        helper.assertTrue(r.leg(fout) == GrillSpel.Uitslag.FOUT && r.gelegd() == 0 && r.klaar() == 0 && r.fouten() == 1,
                "a wrong part: the burger flops, this order starts over, nothing else changes");
        for (int i = 0; i < GrillSpel.AANTAL; i++) {
            List<GrillSpel.Deel> b = r.bestelling();
            for (int j = 0; j < b.size(); j++) {
                GrillSpel.Uitslag u = r.leg(b.get(j));
                GrillSpel.Uitslag verwacht = j < b.size() - 1 ? GrillSpel.Uitslag.GOED
                        : i < GrillSpel.AANTAL - 1 ? GrillSpel.Uitslag.BESTELLING_KLAAR : GrillSpel.Uitslag.ALLES_KLAAR;
                helper.assertTrue(u == verwacht, "order " + (i + 1) + " part " + (j + 1) + ": " + u + ", expected " + verwacht);
            }
            helper.assertTrue(r.klaar() == i + 1, "orders done: " + r.klaar());
        }
        helper.assertTrue(r.allesKlaar() && r.leg(GrillSpel.Deel.BROODJE) == GrillSpel.Uitslag.NIETS, "five orders: the game is won");
        helper.assertTrue(new GrillSpel(2).klaar() == 2, "after a break the orders done stay done");

        // the game for a player
        ServerPlayer p = speler(helper);
        SpVerhaalDev.zetTot(p, SpVerhaal.RECEPT, SpVerhaal.R_BAKKEN);
        BlockPos gs = helper.absolutePos(new BlockPos(3, 2, 3));
        p.snapTo(gs.getX() + 0.5, gs.getY(), gs.getZ() + 0.5);
        List<BlockPos> kratten = new ArrayList<>();
        for (net.minecraft.world.phys.Vec3 k : Bakken.KRATTEN_REL) {
            kratten.add(gs.offset((int) k.x, (int) k.y, (int) k.z));
        }
        Bakken.Spel s = Bakken.start(p, helper.getLevel(), gs, kratten);
        helper.assertTrue(s != null && Bakken.bezig(p) && SpVerhaal.RECEPT_LIJN.sleutel(p).equals("4_b"), "the game is on: the variant 4_b");
        long dozen = helper.getLevel().getEntitiesOfClass(Interaction.class, new net.minecraft.world.phys.AABB(gs).inflate(6),
                e -> D1Tijdelijk.is(e) && p.getUUID().toString().equals(e.getPersistentData().getStringOr(Zicht.ALLEEN, ""))).size();
        helper.assertTrue(dozen == GrillSpel.Deel.values().length, "five crates to click, seen by this player only (" + dozen + ")");
        GrillSpel.Deel eersteDeel = s.regels.volgende();
        helper.assertTrue(Bakken.leg(p, eersteDeel == GrillSpel.Deel.AUGURK ? GrillSpel.Deel.KAAS : GrillSpel.Deel.AUGURK) == GrillSpel.Uitslag.FOUT
                && s.regels.klaar() == 0 && SpVerhaal.RECEPT_LIJN.teller(p, E2SpD1Start.T_BESTELD) == 0
                && SpVerhaal.stap(p, SpVerhaal.RECEPT) == SpVerhaal.R_BAKKEN, "a wrong part: nothing counted, the step stays");
        for (int i = 0; i < GrillSpel.AANTAL; i++) {
            helper.assertTrue(Bakken.maakBestelling(p), "order " + (i + 1) + " can be made");
        }
        helper.assertTrue(s.klaar && SpVerhaal.RECEPT_LIJN.teller(p, E2SpD1Start.T_BESTE_TIJD) > 0, "five orders: done, with a time as a score");
        helper.succeedWhen(() -> {
            helper.assertTrue(SpVerhaal.stap(p, SpVerhaal.RECEPT) == SpVerhaal.R_BAKKEN + 1, "after the end scene the line is at step 5");
            helper.assertTrue(Bakken.spel(p) == null && SpVerhaal.RECEPT_LIJN.teller(p, E2SpD1Start.T_BESTELD) == 0,
                    "the game's things and its counter are gone");
            weg(helper, p);
        });
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public static void einde2SpD1StartBriefEnBaan(GameTestHelper helper) {
        ServerPlayer p = speler(helper), q = speler(helper);
        SpVerhaalDev.zetTot(p, SpVerhaal.RECEPT, SpVerhaal.R_BRIEF);
        SpVerhaalDev.zetTot(q, SpVerhaal.RECEPT, SpVerhaal.R_BRIEF);
        // the bubble is p's own; q's click does nothing, p's click pops it
        helper.assertTrue(Bubbelbrief.zetNu(p) && Bubbelbrief.heeft(p) && !Bubbelbrief.heeft(q), "p has a bubble, q not");
        Interaction knop = Bubbelbrief.knop(p);
        helper.assertTrue(knop != null && p.getUUID().toString().equals(knop.getPersistentData().getStringOr(Zicht.ALLEEN, "")),
                "only p sees it");
        helper.assertTrue(!E2SpD1StartFeature.klik(q, knop) && Bubbelbrief.heeft(p), "q's click is not taken");
        helper.assertTrue(E2SpD1StartFeature.klik(p, knop) && !Bubbelbrief.heeft(p), "p's click pops it");
        // the job: the cap once, step 3 -> 4
        SpVerhaalDev.zetTot(q, SpVerhaal.RECEPT, SpVerhaal.R_SOLLICITATIE);
        D1Stad.aangenomen(q);
        D1Stad.aangenomen(q);
        int petjes = GuhQuests.count(q, ModItems.clothingItem(GuhClothes.SPONSGUH_KNABBELPETJE));
        helper.assertTrue(SpVerhaal.stap(q, SpVerhaal.RECEPT) == SpVerhaal.R_BAKKEN && petjes == 1, "hired: step 4 and one cap (" + petjes + ")");
        // the clicks: Sandy-guh is this slice's before step 3, sp_zij's from step 3 on
        SponsguhWezen sandy = E2SponsguhFeature.SANDY_GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(sandy != null && sandy.cast() == Cast.SANDY_GUH, "a Sandy-guh");
        SpVerhaalDev.zetTot(q, SpVerhaal.RECEPT, SpVerhaal.R_BUBBEL);
        helper.assertTrue(D1Stad.klik(sandy, q), "step 2: Sandy-guh's small talk is this slice's");
        SpVerhaalDev.zetTot(q, SpVerhaal.RECEPT, SpVerhaal.R_SOLLICITATIE);
        helper.assertTrue(!D1Stad.klik(sandy, q), "step 3: Sandy-guh is sp_zij's");
        for (Cast c : D1Stad.PRAAT) {
            for (int stap : new int[] {SpVerhaal.R_REIS, SpVerhaal.R_BAKKEN}) {
                String key = D1Stad.praat(c, stap, sandy);
                helper.assertTrue(key != null && NlTekst.has(key), "small talk of " + c + " at step " + stap + ": " + key);
            }
        }
        // every step of the slice is real, has its hint and its texts; every line of the scenes has a text
        for (int s = SpVerhaal.R_BRIEF; s <= SpVerhaal.R_BAKKEN; s++) {
            helper.assertTrue(SpStappen.isEcht(SpVerhaal.RECEPT, s) && StapHint.hint(SpVerhaal.RECEPT, s) != null, "step " + s + ": real, with a hint");
        }
        for (Cutscene sc : D1Scenes.ALLE) {
            helper.assertTrue(NlTekst.has(sc.titelKey()), "the title of " + sc.id());
            for (Cutscene.Zeg z : sc.zinnen()) {
                helper.assertTrue(NlTekst.has(sc.tekstKey(z.key())), "the line " + sc.id() + "." + z.key());
                helper.assertTrue(z.spreker().isEmpty() || sc.acteur(z.spreker()) != null || NlTekst.has(sc.tekstKey("naam." + z.spreker())),
                        "the speaker of " + sc.id() + "." + z.key());
            }
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(SpVerhaal.stap(p, SpVerhaal.RECEPT) == SpVerhaal.R_REIS && SpVerhaal.stap(q, SpVerhaal.RECEPT) == SpVerhaal.R_SOLLICITATIE,
                    "after the scene brief only p is at step 1");
            weg(helper, p, q);
        });
    }
}
