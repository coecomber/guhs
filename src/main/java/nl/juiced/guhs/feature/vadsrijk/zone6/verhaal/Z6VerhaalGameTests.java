package nl.juiced.guhs.feature.vadsrijk.zone6.verhaal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Lijnen;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Luchtschepen;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone 6 "Het Zijdeland", slice u-z6verhaal (few, focused): (1) every zone-6 quest step names WHO and WHERE, every NPC has a
 * spot/name/where text, every puzzle has its texts; (2) the whole main line "De Sterrenkaravaan" walked through its real hooks
 * (the bultguh at Hobbel, the star puzzles at the beacons and the telescope, the carpet at Knoopje, the scenes, Moeder Bron, the
 * tower) up to {@code zone6_klaar}; (3) the transition: zone 5 done -> the great swim -> line E opens (flag, gate, captains);
 * (4) Ringeling after the raid (the hook for u-z6raid) -> her scene -> F4; (5) the level cap 70.
 */
public class Z6VerhaalGameTests {
    private static final String B = "vadsrijk_z6_verhaal";

    private static List<Quest> z6() {
        List<Quest> l = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (q.id().startsWith("z6_")) {
                l.add(q);
            }
        }
        return l;
    }

    @GuhTest(template = "empty", batch = B)
    public static void elkeStapWieEnWaar(GameTestHelper helper) {
        Language t = Language.getInstance();
        List<Quest> z6 = z6();
        long hoofd = z6.stream().filter(Quest::hoofd).count(), zij = z6.stream().filter(q -> q.soort() == Quest.Soort.ZIJ).count();
        helper.assertTrue(hoofd == 10 && zij == 15, "zone 6: 10 main and 15 side quests: " + hoofd + "/" + zij);
        List<Quest> alle = new ArrayList<>(Quests.alle().values());
        helper.assertTrue(alle.indexOf(Quests.van(Z6Haken.HQ01)) > alle.indexOf(Quests.van(Z6Haken.Z5_LAATSTE)), "zone 6 comes after zone 5 in the questlog");
        for (String id : Z6VerhaalPlekken.NPC.keySet()) {
            NpcDef d = Npcs.van(id);
            helper.assertTrue(d != null && d.plek() != null, "NPC " + id + " is registered with a spot");
            helper.assertTrue(t.has(d.naamKey()) && t.has("vadsrijk.npc.plek.npc." + id) && t.has("vadsrijk.npc.praat." + id + ".1"),
                    "NPC " + id + ": name + where + lines");
            helper.assertTrue(Z6VerhaalPlekken.zone6(d.plek().getX(), d.plek().getY(), d.plek().getZ()), "NPC " + id + " stands in the Zijdeland");
        }
        for (Quest q : z6) {
            for (String wie : List.of(q.gever(), q.inleveren())) {
                helper.assertTrue(Npcs.van(wie) != null && t.has("vadsrijk.npc.plek.npc." + wie), q.id() + ": who (" + wie + ") and where");
            }
            helper.assertTrue(t.has(q.titelKey()) && t.has(q.tekstKey()) && t.has(q.sleutel() + ".bezig"), q.id() + ": texts");
            helper.assertTrue(q.lv() >= 60 && q.lv() <= Levels.MAX, q.id() + ": level 60-70, is " + q.lv());
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                helper.assertTrue(t.has(q.doelKey(i)), q.id() + " step " + i + ": what");
                helper.assertTrue(!d.plek().isEmpty() && QuestPlek.pos(helper.getLevel(), d.plek()) != null, q.id() + " step " + i + ": a target (" + d.plek() + ")");
                helper.assertTrue(t.has(QuestPlek.tekstKey(d.plek())), q.id() + " step " + i + ": where text " + QuestPlek.tekstKey(d.plek()));
            }
        }
        Z6Puzzels.laad();
        helper.assertTrue(Z6Puzzels.PUZZELS.size() >= 14 && Z6Puzzels.BAKEN_PUZZEL.size() == 5, "14 puzzles, 5 beacons");
        for (Z6Puzzels.Puzzel pz : Z6Puzzels.PUZZELS.values()) {
            String k = Z6Verhaal.Z + "puzzel." + pz.id() + ".";
            helper.assertTrue(t.has(k + "titel") && t.has(k + "intro") && t.has(k + "fout") && t.has(k + "klaar"), pz.id() + ": texts");
            for (int s = 0; s < pz.stappen(); s++) {
                helper.assertTrue(t.has(k + s + ".vraag") && t.has(k + s + "." + pz.goed()[s]), pz.id() + " step " + s + ": question + answer");
            }
        }
        for (String s : Z6Verhaal.DEFS.keySet()) {
            for (String r : Z6Verhaal.DEFS.get(s).regels()) {
                helper.assertTrue(t.has(Z6Verhaal.Z + r), "scene " + s + ": " + r);
            }
        }
        for (String id : Z6VerhaalPlekken.UITKIJK_IDS) {
            helper.assertTrue(t.has("vadsrijk.wereld.uitkijk." + id) && Z6VerhaalPlekken.poi(id) != null, "lookout " + id);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 600)
    public static void hoofdlijnViaApi(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        Z6VerhaalRegistry.klaarzetten(p);
        helper.assertTrue(Questlog.actief(p, Z6Haken.HQ01), "line E open: z6_hq01 starts by itself");
        List<Quest> hoofd = z6().stream().filter(Quest::hoofd).toList();
        helper.assertTrue(hoofd.stream().map(Quest::id).toList().equals(Z6Haken.HOOFD), "the main quests in story order");
        String vorige = Z6Haken.Z5_LAATSTE;
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
        helper.assertTrue(Z6VerhaalApi.zone6Klaar(p) && Z6VerhaalApi.torenOpen(p), "z6_hq10 set zone6_klaar: the raid is open");
        helper.assertTrue(Z6Puzzels.aantal(p) == 5, "all five beacons burn: " + Z6Puzzels.aantal(p));
        helper.assertTrue(Z6VerhaalApi.ontmaskerd(p) && Z6VerhaalApi.bronWakker(p), "the Spiegelvizier unmasked, Moeder Bron awake");
        helper.assertTrue(Questlog.map(p, "z6_brieven").getBooleanOr("ringvinger", false), "the Grauwe brief to the Ringvinger");
        helper.assertTrue(!Z6VerhaalApi.ringelingVriend(p), "Ringeling is not cheered up by the story (only by the raid)");
        schoon(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void overgangEnLijnE(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        Questlog.wisAlles(p);
        Spelers.zetLevel(p, 60);
        for (String vlag : List.of("zone1_klaar", "zone2_klaar", "zone3_klaar", "zone4_klaar", "zone5_klaar")) {
            Questlog.zetVlag(p, vlag, true);   // (a real player got here through zones 1-5)
        }
        helper.assertTrue(Lijnen.vanId(Z6Lijn.ID) != null && Lijnen.vanId(Z6Lijn.ID).letter().equals("E"), "line E is in the timetable");
        helper.assertTrue(QuestMotor.reisSlot(p, "zijdeland") != null && !QuestMotor.reisDoelen(p, "z5_havenmeester").contains("zijdeland")
                && Luchtschepen.SLOT.get(Z6Lijn.ID).apply(p, true) != null && Luchtschepen.SLOT.get(Z6Lijn.ID).apply(p, false) == null,
                "before zone 5 is done: line E is closed out (open back), the Zijdeland not on the lists");
        Quest z5 = Quests.van(Z6Haken.Z5_LAATSTE);
        Questlog.neemAan(p, z5);
        Questlog.voltooi(p, z5);
        Z6Haken.uitgesteld(p);
        helper.assertTrue(Z6Verhaal.speelt(p, "wolkwaltocht"), "zone 5 done: the great swim plays");
        Z6Verhaal.spoel(p);
        helper.assertTrue(Z6VerhaalApi.lijnEOpen(p) && Questlog.actief(p, Z6Haken.HQ01) && Questlog.doelKlaar(p, Quests.van(Z6Haken.HQ01), 0),
                "after the swim: line E open, z6_hq01 runs with the swim counted");
        helper.assertTrue(QuestMotor.reisSlot(p, "zijdeland") == null && QuestMotor.reisDoelen(p, "z5_havenmeester").contains("zijdeland")
                && Luchtschepen.SLOT.get(Z6Lijn.ID).apply(p, true) == null, "lv 60: line E and the travel map are open");
        helper.assertTrue(QuestMotor.reisDoelen(p, "z6_zandloper").contains("hoogvads"), "Zandloper flies back to Hoogvads");
        Spelers.zetLevel(p, 59);
        helper.assertTrue(QuestMotor.reisSlot(p, "zijdeland") != null && Luchtschepen.SLOT.get(Z6Lijn.ID).apply(p, true) != null, "below lv 60: closed");
        // the first landing at Rozenkloof (line E's arrival hook) plays the arrival film and counts the arrival
        Spelers.zetLevel(p, 60);
        Luchtschepen.AANKOMST.accept(p, Z6Lijn.ROZENKLOOF);
        helper.assertTrue(Z6Verhaal.speelt(p, "aankomst"), "the first landing plays the arrival film");
        Z6Verhaal.spoel(p);
        helper.assertTrue(Questlog.doelKlaar(p, Quests.van(Z6Haken.HQ01), 1), "the arrival counted");
        schoon(p);
        Questlog.wisAlles(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void ringelingNaDeRaid(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        List<String> gehoord = new ArrayList<>();
        Z6VerhaalApi.RINGELING.add(s -> gehoord.add("ja"));
        try {
            Z6VerhaalApi.ringelingOpgevrolijkt(p);
            helper.assertTrue(Z6VerhaalApi.ringelingVriend(p) && gehoord.size() == 1, "the raid's hook: Ringeling is a friend at once, listeners told");
            Z6Haken.uitgesteld(p);   // (a quiet moment: her scene)
            helper.assertTrue(Z6Verhaal.speelt(p, "ringeling"), "her cheer-up scene plays");
            Z6Verhaal.spoel(p);
            helper.assertTrue(Spelers.data(p).gezien.contains("npc:z6_f4"), "then flash F4 (the Keizer loses her)");
            helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.economie.Cosmetica.heeftTitel(p, "vr_vriend_van_ringeling"), "the title");
            Z6VerhaalApi.ringelingOpgevrolijkt(p);
            helper.assertTrue(gehoord.size() == 1, "a second clear tells nobody again");
            // the veiled lady in Saffraanpoort talks as a friend now
            RijkGuh g = guh(helper, "z6_ringeling", 0);
            helper.assertTrue(Z6Haken.klikVoor(p, g), "Ringeling's own dialogue");
            g.discard();
        } finally {
            Z6VerhaalApi.RINGELING.clear();
        }
        schoon(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void levelCap70(GameTestHelper helper) {
        helper.assertTrue(Levels.MAX == 70 && Levels.MAX_GENERIEK == 70, "the cap is 70");
        for (int l = 1; l <= Levels.MAX; l++) {
            helper.assertTrue(Levels.levelVan(Levels.totaalVoor(l)) == l && Levels.generiekVan(Levels.totaalVoorGeneriek(l)) == l, "level " + l + " round trip");
            helper.assertTrue(l == Levels.MAX || Levels.xpVoor(l + 1) > Levels.xpVoor(l), "the curve rises at " + l);
        }
        double sprong = Levels.xpVoor(61) / (double) Levels.xpVoor(60), ervoor = Levels.xpVoor(60) / (double) Levels.xpVoor(59);
        helper.assertTrue(Math.abs(sprong - ervoor) < 0.01, "smooth past 60: " + sprong + " vs " + ervoor);
        helper.assertTrue(Levels.levelVan(Long.MAX_VALUE / 4) == 70 && Levels.skillpunten(70) == 69, "capped at 70, 69 skill points");
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.zetLevel(p, 70);
        helper.assertTrue(Spelers.level(p) == 70, "a player can be level 70: " + Spelers.level(p));
        Spelers.zetLevel(p, 1);
        helper.succeed();
    }

    // ---- helpers ------------------------------------------------------------------------------------------------------------------

    private static void schoon(ServerPlayer p) {
        Z6VerhaalRegistry.vergeet(p);
        Praat.vergeet(p);
        QuestMotor.vergeet(p);
    }

    /** Completes one objective through the hook it really uses in game. */
    private static void doe(GameTestHelper helper, ServerPlayer p, Quest q, Quest.Doel d, int x) {
        switch (d.type()) {
            case PRAAT -> klik(helper, p, d.doel(), x);
            case GEBIED -> {
                op(helper, p, Z6VerhaalPlekken.poi(d.doel().substring(4)));
                Z6Verhaal.spoel(p);   // (a scene the spot started)
            }
            case MELD -> meld(helper, p, q, d, x);
            case GEHEIM -> {   // the finds of a kind ("teller:z6_cocons"): walk past each
                for (BlockPos b : Z6VerhaalPlekken.VONDST.get(d.doel().substring(7)).values()) {
                    op(helper, p, b);
                }
            }
            default -> helper.fail(q.id() + ": no test path for " + d.type());
        }
    }

    private static void klik(GameTestHelper helper, ServerPlayer p, String npc, int x) {
        Praat.vergeet(p);
        RijkGuh g = guh(helper, npc, x);
        QuestMotor.klik(p, g);
        QuestMotor.afronden(p);
        Z6Verhaal.spoel(p);
        g.discard();
    }

    private static void meld(GameTestHelper helper, ServerPlayer p, Quest q, Quest.Doel d, int x) {
        String[] s = d.doel().split(":", 2);
        switch (d.doel()) {
            case "z6:aangekomen" -> {
                Z6Verhaal.vlieg(p);
                helper.assertTrue(Z6Verhaal.speelt(p, "aankomst"), "the first flight plays the arrival film");
                Z6Verhaal.spoel(p);
            }
            case "z6:bultguh" -> klik(helper, p, "z6_hobbel", x);
            case "z6:geweven" -> klik(helper, p, "z6_knoopje", x);
            case "z6_bron:opgevrolijkt" -> {
                klik(helper, p, "z6_bron", x);
            }
            case "z6_sterrenwacht:kompas" -> {
                // V-INT: with u-z6wezens merged the dungeon is real (the hook starts it instead of the fallback scene), so the
                // compass comes from the dungeon's end; here that end is called directly
                op(helper, p, Z6VerhaalPlekken.KERKER);
                Z6Verhaal.spoel(p);
                if (!Questlog.doelKlaar(p, q, q.doelen().indexOf(d))) {
                    Z6Verhaal.kompas(p);
                }
            }
            default -> {
                if (s[0].equals("z6_baken") || s[0].equals("z6_puzzel") && !s[1].startsWith("afdingen")) {
                    // a puzzle at its spot: it opens by itself, a wrong star restarts it, the right ones solve it
                    String poi = d.plek().substring(4);
                    op(helper, p, Z6VerhaalPlekken.poi(poi));
                    helper.assertTrue(Z6Puzzels.open(p) != null, d.doel() + ": the puzzle opens at " + poi);
                    Z6Puzzels.Puzzel pz = Z6Puzzels.PUZZELS.get(Z6Puzzels.open(p));
                    Z6Puzzels.kies(p, null, (pz.goed()[0] + 1) % pz.opties()[0]);
                    helper.assertTrue(!Questlog.doelKlaar(p, q, q.doelen().indexOf(d)), d.doel() + ": a wrong star counts nothing");
                    helper.assertTrue(Z6Puzzels.losOp(p), d.doel() + ": solved");
                    Z6Verhaal.spoel(p);   // (the vision of Moeder Bron)
                } else if (s[0].equals("z6_puzzel")) {
                    klik(helper, p, "z6_" + s[1].substring("afdingen_".length()), x);
                    helper.assertTrue(Z6Puzzels.losOp(p), d.doel() + ": haggled");
                } else if (d.plek().startsWith("poi:")) {
                    op(helper, p, Z6VerhaalPlekken.poi(d.plek().substring(4)));
                    Z6Verhaal.spoel(p);
                } else {
                    NpcApi.meld(p, s[0], s[1]);
                }
            }
        }
        Praat.vergeet(p);
    }

    /** Stands the player at b for one check of the zone-6 hooks, then back (no far chunks for the test level). */
    private static void op(GameTestHelper helper, ServerPlayer p, BlockPos b) {
        Vec3 was = p.position();
        Praat.vergeet(p);
        Z6Haken.vergeetBij(p);
        p.setPos(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        Z6Haken.stap(p, helper.getLevel());
        p.setPos(was.x, was.y, was.z);
    }

    private static RijkGuh guh(GameTestHelper helper, String id, int x) {
        RijkGuh g = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(1 + x % 3, 1, 1 + (x / 3) % 3));
        g.stelIn(Npcs.van(id));
        return g;
    }
}
