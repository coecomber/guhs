package nl.juiced.guhs.feature.vadsrijk.zone5.verhaal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcHaken;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPatch;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone 5 "Hoogvads", slice t-z5verhaal (few, focused): (1) every zone-5 quest step names WHO and WHERE (an explicit target with a
 * "where" text), every NPC has a spot/name/where text inside Hoogvads, and the detective data is complete; (2) the whole main line
 * "Verraad in de stad" walked through its real hooks (interrogations by clicking the suspect + asking the right question, soup for
 * the refugees, a WRONG then the right accusation on the board, the finale fallback, the lullaby, the cliffhanger) up to
 * {@code zone5_klaar}; (3) the finale hook interface (a registered fight wins over the fallback); (4) the airship gate to Hoogvads.
 */
public class Z5VerhaalGameTests {
    private static final String B = "vadsrijk_z5_verhaal";

    private static List<Quest> z5() {
        List<Quest> l = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (q.id().startsWith("z5_")) {
                l.add(q);
            }
        }
        return l;
    }

    @GuhTest(template = "empty", batch = B)
    public static void elkeStapWieEnWaar(GameTestHelper helper) {
        Language t = Language.getInstance();
        List<Quest> z5 = z5();
        long hoofd = z5.stream().filter(Quest::hoofd).count(), zij = z5.stream().filter(q -> q.soort() == Quest.Soort.ZIJ).count();
        helper.assertTrue(hoofd == 12 && zij >= 10, "zone 5: 12 main (1.10.4: + 2 craft steps) and ~10 side quests: " + hoofd + "/" + zij);
        helper.assertTrue(Quests.van("hq01_welkom") != null && Quests.van("z3_hq01_boom") != null, "zones 1-3 still load");
        List<Quest> alle = new ArrayList<>(Quests.alle().values());
        helper.assertTrue(alle.indexOf(Quests.van(Z5Haken.HQ01)) > alle.indexOf(Quests.van("z4_hq07_opstijgpier")), "zone 5 comes after zone 4 in the questlog");   // (T-INT)
        for (String id : Z5VerhaalPlekken.NPC.keySet()) {
            NpcDef d = Npcs.van(id);
            helper.assertTrue(d != null && d.plek() != null, "NPC " + id + " is registered with a spot");
            helper.assertTrue(t.has(d.naamKey()) && t.has("vadsrijk.npc.plek.npc." + id) && t.has("vadsrijk.npc.praat." + id + ".1"),
                    "NPC " + id + ": name + where + lines");
            helper.assertTrue(Z5VerhaalPlekken.zone5(d.plek().getX(), d.plek().getY(), d.plek().getZ()), "NPC " + id + " stands in Hoogvads");
        }
        for (Quest q : z5) {
            for (String wie : List.of(q.gever(), q.inleveren())) {
                helper.assertTrue(Npcs.van(wie) != null && t.has("vadsrijk.npc.plek.npc." + wie), q.id() + ": who (" + wie + ") and where");
            }
            helper.assertTrue(t.has(q.titelKey()) && t.has(q.tekstKey()) && t.has(q.sleutel() + ".bezig"), q.id() + ": texts");
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                helper.assertTrue(t.has(q.doelKey(i)), q.id() + " step " + i + ": what");
                helper.assertTrue(!d.plek().isEmpty() && QuestPlek.pos(helper.getLevel(), d.plek()) != null, q.id() + " step " + i + ": a target (" + d.plek() + ")");
                helper.assertTrue(t.has(QuestPlek.tekstKey(d.plek())), q.id() + " step " + i + ": where text " + QuestPlek.tekstKey(d.plek()));
            }
        }
        // the detective data
        helper.assertTrue(Z5Speurwerk.BEWIJS.size() == 10 && Z5Speurwerk.VERHOREN.size() == 7 && Z5Speurwerk.VERDACHTEN.size() == 4,
                "10 clues, 7 interrogations, 4 suspects");
        helper.assertTrue(Z5Speurwerk.VERDACHTEN.values().stream().filter(Z5Speurwerk.Verdachte::verrader).count() == 1, "one traitor");
        for (String b : Z5Speurwerk.BEWIJS) {
            helper.assertTrue(t.has(Z5Verhaal.Z + "bewijs." + b + ".titel") && t.has(Z5Verhaal.Z + "bewijs." + b), "clue " + b + ": texts");
        }
        for (Z5Speurwerk.Verhoor v : Z5Speurwerk.VERHOREN.values()) {
            for (int i = 0; i < v.intro().size(); i++) {
                helper.assertTrue(t.has(Z5Verhaal.Z + "verhoor." + v.id() + ".intro." + i), v.id() + " intro " + i);
            }
            for (int k = 0; k < v.vragen().size(); k++) {
                helper.assertTrue(t.has(Z5Verhaal.Z + "verhoor." + v.id() + ".vraag." + k), v.id() + " question " + k);
                for (int i = 0; i < v.vragen().get(k).size(); i++) {
                    helper.assertTrue(t.has(Z5Verhaal.Z + "verhoor." + v.id() + ".antwoord." + k + "." + i), v.id() + " answer " + k + "." + i);
                }
            }
        }
        for (String id : Z5VerhaalPlekken.UITKIJK_IDS) {
            helper.assertTrue(t.has("vadsrijk.wereld.uitkijk." + id) && Z5VerhaalPlekken.poi(id) != null, "lookout " + id);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 600)
    public static void hoofdlijnViaApi(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        Z5VerhaalRegistry.klaarzetten(p);
        helper.assertTrue(Questlog.actief(p, Z5Haken.HQ01), "zone 4 done: z5_hq01 starts by itself");
        List<Quest> hoofd = z5().stream().filter(Quest::hoofd).toList();
        helper.assertTrue(hoofd.stream().map(Quest::id).filter(id -> !id.matches("z5_hq[0-9]+[ab]_.*")).toList().equals(Z5Haken.HOOFD), "the main quests in story order (1.10.4: craft steps hqNNa/b in between)");
        String vorige = "z4_hq07_opstijgpier";   // (T-INT: zone 4's last main quest)
        int x = 0;
        for (Quest q : hoofd) {
            helper.assertTrue(q.na().contains(vorige), q.id() + " follows " + vorige);
            vorige = q.id();
            if (Questlog.status(p, q.id()) == 0) {
                Questlog.neemAan(p, q);
            }
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (Questlog.doelKlaar(p, q, i) || d.type() == Quest.Doel.Type.PRAAT && d.doel().equals(q.inleveren())) {
                    continue;   // (already counted by an earlier hook, or the hand-in click does it)
                }
                doe(helper, p, q, d, x++);
                helper.assertTrue(Questlog.doelKlaar(p, q, i), q.id() + " step " + i + " (" + d.type() + " " + d.doel() + ") counted");
            }
            Praat.vergeet(p);
            RijkGuh inl = guh(helper, q.inleveren(), x++);
            QuestMotor.klik(p, inl);
            helper.assertTrue(Questlog.klaar(p, q.id()), q.id() + " handed in at " + q.inleveren());
            QuestMotor.afronden(p);   // (the dialogue closes: the quest action runs)
            inl.discard();
        }
        helper.assertTrue(Z5VerhaalApi.zone5Klaar(p), "the lullaby set zone5_klaar");
        helper.assertTrue(Z5Speurwerk.aantal(p) == 10, "all 10 clues in the Speurdersboekje: " + Z5Speurwerk.aantal(p));
        helper.assertTrue(Questlog.map(p, "z5_brieven").getBooleanOr("ringvinger", false), "the Grauwe brief to the Ringvinger");
        helper.assertTrue(Z5VerhaalApi.ontmaskerd(p), "the spy was unmasked");
        schoon(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void finaleHaak(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        Z5VerhaalRegistry.naarStap(p, 8);
        NpcApi.meld(p, "z5_spion", "x");   // (noise: nothing)
        Quest q = Quests.van(Z5Haken.HQ08);
        Questlog.tel(p, Quest.Doel.Type.GEBIED, d -> d == q.doelen().get(0), 1);
        helper.assertTrue(Z5Finale.volgende(p) == Z5Finale.Fase.SPION, "after the blowhole: the spy is next");
        List<String> gestart = new ArrayList<>();
        Z5Finale.registreer(Z5Finale.Fase.SPION, s -> gestart.add("spion"));
        Z5Finale.registreer(Z5Finale.Fase.DROOM, s -> gestart.add("droom"));
        try {
            Z5Finale.start(p);
            helper.assertTrue(gestart.equals(List.of("spion")) && !Z5Verhaal.bezig(p), "a registered fight wins over the fallback scene");
            Z5Finale.spionVerslagen(p);
            helper.assertTrue(gestart.equals(List.of("spion", "droom")) && Z5Finale.volgende(p) == Z5Finale.Fase.DROOM, "the dream starts after the spy");
            Z5Finale.droomOpgevrolijkt(p);
            helper.assertTrue(Z5Finale.volgende(p) == null && Questlog.allesKlaar(p, q), "both phases counted: back to the queen");
        } finally {
            Z5Finale.vergeetAlles();
        }
        schoon(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void luchtschipNaarHoogvads(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        Questlog.wisAlles(p);
        for (String vlag : List.of("zone1_klaar", "zone2_klaar", "zone3_klaar")) {
            Questlog.zetVlag(p, vlag, true);
        }
        Spelers.zetLevel(p, 56);
        helper.assertTrue(QuestMotor.reisSlot(p, "hoogvads") != null && !QuestMotor.reisDoelen(p, "luchtschipper").contains("hoogvads"),
                "before zone 4: Hoogvads is locked and not on the captains' lists");
        Questlog.zetVlag(p, Z5Verhaal.VLAG_ZONE4, true);
        helper.assertTrue(QuestMotor.reisSlot(p, "hoogvads") == null && QuestMotor.reisDoelen(p, "luchtschipper").contains("hoogvads"),
                "after zone 4 at lv 56: the line is open");
        helper.assertTrue(QuestMotor.reisDoelen(p, "z5_havenmeester").containsAll(Z5VerhaalRegistry.HAVEN_REIZEN), "the havenmeester flies everywhere");
        Spelers.zetLevel(p, Z5Verhaal.MIN_LEVEL - 5);
        helper.assertTrue(QuestMotor.reisSlot(p, "hoogvads") != null, "below the line's level: locked");
        schoon(p);
        Questlog.wisAlles(p);
        helper.succeed();
    }

    /**
     * The telescoop fix (after 1.10.25, side quest z5_s04_oog "Het Oog meten"; run: {@code -Pgt=Z5VerhaalGameTests}). On live a
     * player stood at "climb: done, measure: open" for good: the shared poi ticker (NpcHaken.plekken, a wider radius) had counted the
     * climb before zone 5's own ticker saw it, so the measurement at the spot never came; and the spot was the FOOT of the tower stair,
     * with no telescope anywhere. Now: (1) the spot is the dome room on top and the patch sterrenwacht_telescoop puts a telescope
     * there, idempotent; (2) the shared ticker leaves zone-5 spots alone; (3) without a telescope standing there, being in the room
     * measures, also for who was stuck; (4) with the telescope, a click on it measures; (5) a find the old race swallowed is given
     * the next time you stand at its spot.
     */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void telescoopInDeSterrenwacht(GameTestHelper helper) throws Exception {
        net.minecraft.server.level.ServerLevel l = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        Quest q = Quests.van(Z5Haken.OOG);
        BlockPos kamer = Z5VerhaalPlekken.STERRENWACHT, tel = Z5VerhaalPlekken.TELESCOOP;
        Language t = Language.getInstance();

        // 1. the spot is the dome room (the lookout on top of the tower), the telescope stands in it, the patch puts it exactly there
        helper.assertTrue(q != null && q.doelen().size() == 2 && q.doelen().get(0).doel().equals("poi:z5_sterrenwacht")
                && q.doelen().get(1).doel().equals("z5:oog_gemeten"), "z5_s04_oog: climb, then measure");
        BlockPos top = Z5VerhaalPlekken.wereld("universiteit_sterrenwacht_top"), voet = Z5VerhaalPlekken.wereld("universiteit_sterrenwacht");
        helper.assertTrue(top != null && kamer.equals(top) && kamer.equals(Z5VerhaalPlekken.poi("z5_sterrenwacht"))
                && kamer.equals(QuestPlek.pos(null, "poi:z5_sterrenwacht")), "the quest spot (and the tracker) is the dome room on top: " + kamer);
        helper.assertTrue(voet != null && kamer.getY() - voet.getY() >= 40, "not the foot of the stair any more: " + voet + " -> " + kamer);
        helper.assertTrue(tel.getY() == kamer.getY() + 1 && Math.abs(tel.getX() - kamer.getX()) <= 4 && Math.abs(tel.getZ() - kamer.getZ()) <= 4,
                "the telescope stands in the dome room: " + tel);
        WereldPatch.Patch patch = WereldPatch.laad("sterrenwacht_telescoop").orElse(null);
        helper.assertTrue(patch != null && patch.dozen().size() == 2 && patch.cellen() == 2
                && patch.dim().identifier().toString().equals("guhs:vadsrijk"), "the patch sterrenwacht_telescoop loads: a pedestal and the telescope, in the Vadsrijk");
        helper.assertTrue(patch.dozen().get(0).min().equals(tel.below()) && patch.dozen().get(1).min().equals(tel)
                && patch.dozen().get(1).blok().getBlock() instanceof nl.juiced.guhs.feature.sterrenwacht.TelescoopBlock,
                "the patch puts the telescope at Z5VerhaalPlekken.TELESCOOP, on its pedestal");
        for (String key : List.of(q.doelKey(0), q.doelKey(1), "vadsrijk.npc.plek.poi.z5_sterrenwacht", Z5Verhaal.Z + "oog.gemeten", Z5Verhaal.Z + "oog.telescoop",
                Z5Verhaal.Z + "oog.kijk")) {
            helper.assertTrue(t.has(key), "text " + key);
        }
        helper.assertTrue(!t.getOrDefault(q.doelKey(1)).contains("als je er staat"), "the step no longer says 'klik als je er staat'");

        Vec3 was = p.position();
        java.nio.file.Path backup = null;
        try {
            // 2. the shared ticker leaves the spot alone (it used to count the climb and swallow the measurement); zone 5 counts it
            Questlog.neemAan(p, q);
            p.setPos(kamer.getX() + 0.5, kamer.getY(), kamer.getZ() + 0.5);
            NpcHaken.robotPlekken(p);
            helper.assertTrue(!Questlog.doelKlaar(p, q, 0) && !Questlog.doelKlaar(p, q, 1), "the shared poi ticker does not count a zone-5 spot");
            helper.assertTrue(!Z5Haken.telescoopStaat(l), "(no telescope in this world yet)");
            Z5Haken.stap(p, l);
            helper.assertTrue(Questlog.allesKlaar(p, q), "no telescope standing: being in the dome room counts the climb AND the measurement");

            // 3. the state of the live report: climb counted, measurement open; standing far below does nothing, in the room it counts
            Questlog.vergeet(p, q.id());
            Questlog.neemAan(p, q);
            Questlog.tel(p, Quest.Doel.Type.GEBIED, d -> d == q.doelen().get(0), 1);
            helper.assertTrue(Questlog.doelKlaar(p, q, 0) && !Questlog.doelKlaar(p, q, 1), "stuck as on live: [1, 0]");
            p.setPos(voet.getX() + 0.5, voet.getY(), voet.getZ() + 0.5);
            Z5Haken.stap(p, l);
            helper.assertTrue(!Questlog.doelKlaar(p, q, 1), "at the foot of the stair nothing is measured");
            p.setPos(kamer.getX() + 0.5, kamer.getY(), kamer.getZ() + 0.5);
            Z5Haken.stap(p, l);
            helper.assertTrue(Questlog.allesKlaar(p, q), "who was stuck gets the measurement in the dome room");

            // 4. the patch on an existing world: places both blocks once, a second run changes nothing; then the CLICK measures
            l.getChunk(tel);
            for (BlockPos b : BlockPos.betweenClosed(tel.offset(-1, -2, -1), tel.offset(1, 1, 1))) {
                l.setBlock(b, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
            l.setBlock(tel.below(2), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2 | 16);
            WereldPatch.Uitslag proef = WereldPatch.toepassen(l, patch, false);
            helper.assertTrue(proef.fout() == null && proef.plaatsen() == 2 && !Z5Haken.telescoopStaat(l), "dry run: 2 to place, nothing placed: " + proef.fout());
            WereldPatch.Uitslag echt = WereldPatch.toepassen(l, patch, true);
            helper.assertTrue(echt.fout() == null && echt.plaatsen() == 2 && echt.backup() != null, "--echt places 2 blocks with a backup: " + echt.fout());
            backup = l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data").resolve(echt.backup());
            helper.assertTrue(Z5Haken.telescoopStaat(l), "the telescope stands");
            WereldPatch.Uitslag nog = WereldPatch.toepassen(l, patch, true);
            helper.assertTrue(nog.fout() == null && nog.plaatsen() == 0 && nog.alGoed() == 2 && nog.backup() == null, "a second run changes nothing");

            Questlog.vergeet(p, q.id());
            Questlog.neemAan(p, q);
            Z5Haken.stap(p, l);
            helper.assertTrue(Questlog.doelKlaar(p, q, 0) && !Questlog.doelKlaar(p, q, 1), "with the telescope: the room counts the climb, the measurement waits for the click");
            helper.assertTrue(!Z5Haken.telescoopKlik(p, l, tel.below()) && !Questlog.doelKlaar(p, q, 1), "a click on another block is not ours");
            helper.assertTrue(Z5Haken.telescoopKlik(p, l, tel) && Questlog.allesKlaar(p, q), "a click on the telescope measures the Eye");
            helper.assertTrue(Z5Haken.telescoopKlik(p, l, tel) && Questlog.allesKlaar(p, q), "another click is just a look");
            // (a click before the room was counted counts the climb too)
            Questlog.vergeet(p, q.id());
            Questlog.neemAan(p, q);
            helper.assertTrue(Z5Haken.telescoopKlik(p, l, tel) && Questlog.allesKlaar(p, q), "the click counts the climb too");
            Questlog.vergeet(p, q.id());

            // 5. a find the old race swallowed: the lockers counted (as the shared ticker did), no clue, no letter -> given at the spot
            Quest mol = Quests.van(Z5Haken.HQ03);
            Questlog.neemAan(p, mol);
            for (Quest.Doel d : mol.doelen()) {
                if (d.doel().equals("poi:z5_kastje")) {
                    Questlog.tel(p, d.type(), x -> x == d, 1);
                }
            }
            helper.assertTrue(Z5Haken.geweest(p, "z5_kastje") && !Z5Speurwerk.heeft(p, "bevelbrief"), "the lockers were counted without their clue");
            BlockPos kastje = Z5VerhaalPlekken.poi("z5_kastje");
            p.setPos(kastje.getX() + 0.5, kastje.getY(), kastje.getZ() + 0.5);
            Z5Haken.stap(p, l);
            helper.assertTrue(Z5Speurwerk.heeft(p, "bevelbrief") && !Z5Haken.eenmaligNog(p, "bevelbrief"), "back at the lockers: the clue and the letter after all");
        } finally {
            p.setPos(was.x, was.y, was.z);
            for (int dy = 0; dy <= 2; dy++) {
                l.setBlock(tel.below(dy), Blocks.AIR.defaultBlockState(), 2 | 16);
            }
            if (backup != null) {
                java.nio.file.Files.deleteIfExists(backup);
            }
            schoon(p);
        }
        helper.succeed();
    }

    // ---- helpers ------------------------------------------------------------------------------------------------------------------

    private static void schoon(ServerPlayer p) {
        Z5VerhaalRegistry.vergeet(p);
        Praat.vergeet(p);
        QuestMotor.vergeet(p);
    }

    /** Completes one objective through the hook it really uses in game. */
    private static void doe(GameTestHelper helper, ServerPlayer p, Quest q, Quest.Doel d, int x) {
        switch (d.type()) {
            case PRAAT -> {
                RijkGuh g = guh(helper, d.doel(), x);
                QuestMotor.klik(p, g);
                QuestMotor.afronden(p);
                g.discard();
            }
            case GEBIED -> {
                op(helper, p, Z5VerhaalPlekken.poi(d.doel().substring(4)));
                Z5Verhaal.spoel(p);   // (a scene the spot started: the finale fallback, the lullaby, the cliffhanger)
            }
            case VERZAMEL -> nl.juiced.guhs.feature.vadsrijk.npc.Questlog.geef(p, new net.minecraft.world.item.ItemStack(nl.juiced.guhs.feature.vadsrijk.npc.Questlog.item(d.doel()), d.n()));   // 1.10.4: craft gather steps
            case MELD -> meld(helper, p, q, d, x);
            default -> helper.fail(q.id() + ": no test path for " + d.type());
        }
    }

    private static void meld(GameTestHelper helper, ServerPlayer p, Quest q, Quest.Doel d, int x) {
        String[] s = d.doel().split(":", 2);
        switch (s[0]) {
            case "z5_verhoor" -> {
                Z5Speurwerk.Verhoor v = Z5Speurwerk.VERHOREN.get(s[1]);
                RijkGuh g = guh(helper, v.npc(), x);
                QuestMotor.klik(p, g);   // (KLIK_VOOR opens the interrogation)
                for (int k = 0; k < v.vragen().size(); k++) {
                    if (k != v.sleutel()) {
                        helper.assertTrue(Z5Speurwerk.vraagVoorTest(p, k), v.id() + ": the interrogation is open");
                        helper.assertTrue(!Z5Speurwerk.heeft(p, v.bewijs()), v.id() + ": a side question gives no clue");
                    }
                }
                helper.assertTrue(Z5Speurwerk.vraagVoorTest(p, v.sleutel()) && Z5Speurwerk.heeft(p, v.bewijs()), v.id() + ": the right question gives " + v.bewijs());
                Praat.vergeet(p);
                g.discard();
            }
            case "z5_soep" -> {
                for (int i = 1; i <= 4; i++) {
                    RijkGuh g = guh(helper, "z5_vluchteling_" + i, x + i);
                    QuestMotor.klik(p, g);
                    QuestMotor.klik(p, g);   // (twice: the second bowl does not count)
                    g.discard();
                }
                Praat.vergeet(p);
            }
            case "z5_beschuldigd" -> {
                helper.assertTrue(!Z5Speurwerk.beschuldig(p, null, "gouddraad") && !Questlog.doelKlaar(p, q, 0), "a wrong accusation counts nothing");
                helper.assertTrue(Z5Speurwerk.beschuldig(p, null, "zachtpels"), "the Chancellor is the traitor");
                Praat.vergeet(p);
            }
            case "z5_zilverstaart", "z5_droom" -> {
                Z5Verhaal.spoel(p);   // (the finale fallback started at the blowhole: both phases)
                if (!Questlog.doelKlaar(p, q, q.doelen().indexOf(d))) {
                    op(helper, p, Z5VerhaalPlekken.SPUITGAT);
                    Z5Verhaal.spoel(p);
                }
            }
            default -> {
                switch (d.doel()) {
                    case "z5:inspecteur" -> {
                        RijkGuh g = guh(helper, "z5_inspecteur", x);
                        QuestMotor.klik(p, g);
                        Praat.vergeet(p);
                        g.discard();
                    }
                    case "z5:aangekomen" -> {
                        Z5Verhaal.vlieg(p);
                        helper.assertTrue(Z5Verhaal.speelt(p, "aankomst"), "the first flight plays the arrival film");
                        Z5Verhaal.spoel(p);
                    }
                    case "z5:herinnering", "z5:ontmaskerd", "z5:wolkwal_wakker", "z5:cliffhanger" -> {
                        Praat.vergeet(p);
                        if (!Z5Verhaal.bezig(p)) {
                            BlockPos b = switch (d.doel()) {
                                case "z5:wolkwal_wakker" -> Z5VerhaalPlekken.BALKON_OOG;
                                case "z5:cliffhanger" -> Z5VerhaalPlekken.poi("z5_pier_oost");
                                default -> Z5VerhaalPlekken.LAAN;
                            };
                            op(helper, p, b);
                        }
                        helper.assertTrue(Questlog.doelKlaar(p, q, q.doelen().indexOf(d)) || Z5Verhaal.bezig(p), d.doel() + ": its scene plays");
                        Z5Verhaal.spoel(p);
                    }
                    default -> NpcApi.meld(p, s[0], s[1]);
                }
            }
        }
    }

    /** Stands the player at b for one check of the zone-5 hooks, then back (no far chunks for the test level). */
    private static void op(GameTestHelper helper, ServerPlayer p, BlockPos b) {
        Vec3 was = p.position();
        p.setPos(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        Z5Haken.stap(p, helper.getLevel());
        p.setPos(was.x, was.y, was.z);
    }

    private static RijkGuh guh(GameTestHelper helper, String id, int x) {
        RijkGuh g = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(1 + x % 3, 1, 1 + (x / 3) % 3));
        g.stelIn(Npcs.van(id));
        return g;
    }
}
