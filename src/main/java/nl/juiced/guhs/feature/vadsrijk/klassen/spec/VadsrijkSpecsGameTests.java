package nl.juiced.guhs.feature.vadsrijk.klassen.spec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.klassen.Advancement;
import nl.juiced.guhs.feature.vadsrijk.klassen.Wapens;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-SPECS: the spec model. Tree layout (trunk + 2 spec branches + side branch, one node per cell, requirements stay in their
 * column), the advancement flow (lv 17 spec choice, 35, 58, titles per spec), the switch rules (10 min, fight, desk), the
 * migration of old saves, and the gear compare verdict. The DPS parity of the specs is in {@code VadsrijkKlassenGameTests#dpsTabelKlopt}.
 * Run: {@code runGameTestServer -Pgt=VadsrijkSpecs}.
 */
public class VadsrijkSpecsGameTests {
    private static final String BATCH = "vadsrijk_specs";

    @GuhTest(template = "empty", batch = BATCH)
    public static void specBoomIndeling(GameTestHelper helper) {
        for (Klasse k : Klassen.alle()) {
            List<Spec> specs = Spec.vanKlasse(k.id());
            helper.assertTrue(specs.size() == 2 && specs.get(0).links && !specs.get(1).links, k.id() + ": two specs");
            Map<String, String> cel = new HashMap<>();
            for (BoomKnoop b : k.boom()) {
                if (b.rij() < 0) {
                    helper.assertTrue(b.kosten() == 0, k.id() + ": hidden node with a cost " + b.id());
                    continue;
                }
                helper.assertTrue(b.rij() <= 9 && b.kolom() >= 0 && b.kolom() < SpecBoom.KOLOMMEN, k.id() + ": outside the grid " + b.id());
                String c = b.rij() + "," + b.kolom();
                helper.assertTrue(cel.put(c, b.id()) == null, k.id() + ": two nodes in cell " + c + ": " + b.id() + " / " + cel.get(c));
                helper.assertTrue(b.kosten() == SpecBoom.kosten(b.rij()), k.id() + ": cost of " + b.id());
                for (String v : b.vereist()) {
                    for (String alt : v.split("\\|")) {
                        BoomKnoop o = Boom.knoop(k, alt);
                        helper.assertTrue(o != null, k.id() + ": " + b.id() + " needs an unknown node " + alt);
                        helper.assertTrue(o.rij() < 0 || o.kolom() == b.kolom(), k.id() + ": " + b.id() + " needs a node of another column: " + alt);
                    }
                }
                // a spec column leads back to its spec node; trunk and side never need a spec
                String wortel = wortel(k, b.id(), new HashSet<>());
                if (b.kolom() == 0 || b.kolom() == 2) {
                    helper.assertTrue(specs.get(b.kolom() == 0 ? 0 : 1).knoop().equals(wortel), k.id() + ": " + b.id() + " roots in " + wortel);
                } else {
                    helper.assertTrue(wortel == null, k.id() + ": trunk/side node " + b.id() + " needs a spec (" + wortel + ")");
                }
            }
            helper.assertTrue(cel.size() >= 28, k.id() + ": only " + cel.size() + " nodes");
            for (Spec s : specs) {
                helper.assertTrue(Boom.knoop(k, s.knoop()) != null, k.id() + ": no hidden spec node " + s.knoop());
            }
        }
        helper.succeed();
    }

    private static String wortel(Klasse k, String id, Set<String> gezien) {
        BoomKnoop b = Boom.knoop(k, id);
        if (b == null || !gezien.add(id)) {
            return null;
        }
        if (b.rij() < 0) {
            return id;
        }
        for (String v : b.vereist()) {
            String w = wortel(k, v.split("\\|")[0], gezien);
            if (w != null) {
                return w;
            }
        }
        return null;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void advancementMetSpec(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        String k = Klassen.RIDDER;
        d.klasse = k;
        Advancement.POORTEN = false;
        try {
            d.xp.put(k, Levels.totaalVoor(16));
            helper.assertTrue(!Advancement.kanDoen(p, k), "lv 16: no step 1");
            d.xp.put(k, Levels.totaalVoor(Advancement.LV1));
            helper.assertTrue(Advancement.kanDoen(p, k), "lv 17: step 1 open");
            helper.assertTrue(Advancement.doe(p, k) && Advancement.stap(p, k) == 0 && Specs.open(p, k), "the quest opens the spec choice first");
            helper.assertTrue("nog_niet".equals(Specs.kies(p, Spec.STERRENWEVER)) || Specs.spec(p, Klassen.MAGIER) == null, "not another class's spec");
            helper.assertTrue(Specs.kies(p, Spec.REUZENKUSSEN) == null, "chose Reuzenkussen");
            helper.assertTrue(Advancement.stap(p, k) == 1 && Specs.spec(p, k) == Spec.REUZENKUSSEN && !Specs.open(p, k), "step 1 + spec");
            helper.assertTrue(Advancement.titelKey(p).equals("vadsrijk.klassen.titel.reuzenkussen.1"), "title " + Advancement.titelKey(p));
            helper.assertTrue(Specs.heeft(p, Spec.REUZENKUSSEN, 1) && !Specs.heeft(p, Spec.REUZENKUSSEN, 2) && !Specs.heeft(p, Spec.SCHILDRIDDER, 1),
                    "Specs.heeft per step");
            helper.assertTrue("al".equals(Specs.kies(p, Spec.SCHILDRIDDER)), "the first choice only once");
            helper.assertTrue(!Advancement.kanDoen(p, k), "lv 17: no step 2");
            d.xp.put(k, Levels.totaalVoor(Advancement.LV2));
            helper.assertTrue(Advancement.kanDoen(p, k) && Advancement.doe(p, k) && Advancement.stap(p, k) == 2, "step 2 at 35");
            helper.assertTrue(Advancement.titelKey(p).equals("vadsrijk.klassen.titel.reuzenkussen.2"), "title 2");
            d.xp.put(k, Levels.totaalVoor(Advancement.LV3));
            helper.assertTrue(Advancement.doe(p, k) && Advancement.stap(p, k) == 3, "step 3 at 58");
            helper.assertTrue(!Advancement.kanDoen(p, k) && !Advancement.doe(p, k), "no step 4");
            // the story gates
            Advancement.POORTEN = true;
            ServerPlayer q = GuhMockPlayer.of(helper);
            SpelerData e = Spelers.data(q);
            e.klasse = k;
            Boom.knopen(e, k).add(Spec.SCHILDRIDDER.knoop());
            Boom.knopen(e, k).add("r_adv1");
            e.xp.put(k, Levels.totaalVoor(40));
            helper.assertTrue(!Advancement.kanDoen(q, k), "step 2 waits for the Kaaszeppelinwerf (z3_hq08_werf)");
        } finally {
            Advancement.POORTEN = true;
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void specWisselRegels(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        String k = Klassen.JAGER;
        d.klasse = k;
        d.xp.put(k, Levels.totaalVoor(30));
        long t = 10_000_000L;
        helper.assertTrue("geen".equals(Specs.wissel(p, k, true, t)), "no spec: nothing to switch");
        Set<String> boom = Boom.knopen(d, k);
        boom.addAll(List.of(Spec.SCHERPSCHUTTER.knoop(), "j_adv1", "j_scherpoog", "j_spanpees", "j_snelle_pees", "j_lichtvoetig"));
        helper.assertTrue("balie".equals(Specs.wissel(p, k, false, t)), "only at a class NPC");
        Specs.zetGevecht(p, true);
        helper.assertTrue("gevecht".equals(Specs.wissel(p, k, true, t)), "not in a fight");
        Specs.zetGevecht(p, false);
        double kritVoor = Spelers.stats(p).get(Stat.KRITIEK);
        helper.assertTrue(Specs.wissel(p, k, true, t) == null, "switch at the desk");
        helper.assertTrue(Specs.spec(p, k) == Spec.STRIKKENZETTER, "now Strikkenzetter");
        helper.assertTrue(!boom.contains("j_scherpoog") && !boom.contains("j_spanpees"), "the old spec branch is refunded: " + boom);
        helper.assertTrue(boom.contains("j_snelle_pees") && boom.contains("j_lichtvoetig") && boom.contains("j_adv1"), "trunk, side and steps stay");
        helper.assertTrue(Spelers.stats(p).get(Stat.KRITIEK) < kritVoor, "Valkenblik's crit is gone");
        helper.assertTrue("wacht".equals(Specs.wissel(p, k, true, t + 9 * 60_000L)), "9 minutes later: not yet");
        helper.assertTrue(Specs.wisselOver(p, t + 9 * 60_000L) == 60, "one minute left");
        helper.assertTrue(Specs.wissel(p, k, true, t + 10 * 60_000L) == null, "after 10 minutes: again");
        helper.assertTrue(Specs.spec(p, k) == Spec.SCHERPSCHUTTER, "back");
        // the tree reset keeps the spec
        boom.add("j_scherpoog");
        helper.assertTrue(Boom.respec(d, k, t) == null && Specs.spec(p, k) == Spec.SCHERPSCHUTTER && !boom.contains("j_scherpoog"), "respec keeps the spec");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void migratieOudeSave(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        // an old save: ridder with old steps 1-4 (lv 52), jager with old step 1 at lv 12, magier with old step 1 at lv 18
        d.xp.put(Klassen.RIDDER, Levels.totaalVoor(52));
        d.xp.put(Klassen.JAGER, Levels.totaalVoor(12));
        d.xp.put(Klassen.MAGIER, Levels.totaalVoor(18));
        Boom.knopen(d, Klassen.RIDDER).addAll(List.of("r_adv1", "r_adv2", "r_adv3", "r_adv4", "r_pluisvacht", "r_knuffelspier", "r_vuurkussen"));
        Boom.knopen(d, Klassen.JAGER).addAll(List.of("j_adv1", "j_scherpoog"));
        Boom.knopen(d, Klassen.MAGIER).addAll(List.of("m_adv1", "m_wijs"));
        d.extra("klassen").putInt("spec_v", 1);
        helper.assertTrue(Specs.migreer(d), "something migrated");
        helper.assertTrue(Boom.knopen(d, Klassen.RIDDER).equals(Set.of("r_adv1", "r_adv2")), "old 4 steps -> new 2, nodes refunded: "
                + Boom.knopen(d, Klassen.RIDDER));
        helper.assertTrue(Boom.knopen(d, Klassen.JAGER).isEmpty(), "old step 1 under lv 17 -> nothing yet: " + Boom.knopen(d, Klassen.JAGER));
        helper.assertTrue(Boom.knopen(d, Klassen.MAGIER).equals(Set.of("m_adv1")), "old step 1 at lv 18 -> new step 1");
        helper.assertTrue(Specs.open(p, Klassen.RIDDER) && Specs.open(p, Klassen.MAGIER) && !Specs.open(p, Klassen.JAGER), "the spec choice is open");
        helper.assertTrue(!Specs.migreer(d), "only once");
        d.klasse = Klassen.RIDDER;
        helper.assertTrue(!Advancement.kanDoen(p, Klassen.RIDDER), "no step 3 before the spec is picked");
        helper.assertTrue(Specs.kies(p, Spec.SCHILDRIDDER) == null && Advancement.stap(p, Klassen.RIDDER) == 2, "pick: keeps step 2");
        helper.assertTrue(Advancement.titelKey(p).equals("vadsrijk.klassen.titel.schildridder.2"), "title " + Advancement.titelKey(p));
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void gearVergelijkPijl(GameTestHelper helper) {
        var rnd = helper.getLevel().getRandom();
        var bt = BasisTypes.van(Wapens.PLUISBOOG);
        Uitrusting zwak = Rollen.rol(bt, 5, Zeldzaamheid.GEWOON, rnd), sterk = Rollen.rol(bt, 25, Zeldzaamheid.EPISCH, rnd);
        GearVergelijk.Uitslag op = GearVergelijk.vergelijk(sterk, zwak, Klassen.JAGER), neer = GearVergelijk.vergelijk(zwak, sterk, Klassen.JAGER);
        helper.assertTrue(op.oordeel() == 1 && neer.oordeel() == -1, "lv 25 epic vs lv 5 common: " + op.score() + " / " + neer.score());
        helper.assertTrue(op.regels().stream().anyMatch(v -> v.stat() == null && v.verschil() > 0), "the damage line goes up");
        helper.assertTrue(GearVergelijk.vergelijk(zwak, zwak, Klassen.JAGER).oordeel() == 0, "the same piece: equal");
        helper.assertTrue(GearVergelijk.vergelijk(zwak, null, Klassen.JAGER).oordeel() == 1, "an empty slot: better");
        helper.succeed();
    }
}
