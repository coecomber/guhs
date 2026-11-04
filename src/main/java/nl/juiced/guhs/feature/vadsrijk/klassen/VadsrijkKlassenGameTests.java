package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Het Vadsrijk klassen: only the risky logic. The skill tree point maths (19 points at lv 20, requirements incl. "a|b",
 * the either/or variants, you can't take everything, advancement nodes are free), the respec cooldown of one real hour
 * (advancements survive it), and switching classes keeps every class's own level (and gives a starter weapon only once).
 * Run: {@code runGameTestServer -Pgt=VadsrijkKlassen}.
 */
public class VadsrijkKlassenGameTests {
    private static final String BATCH = "vadsrijk_klassen";

    @GuhTest(template = "empty", batch = BATCH)
    public static void skillboomPuntenRekenen(GameTestHelper helper) {
        helper.assertTrue(Levels.skillpunten(20) == 19, "lv 20 should give 19 points, got " + Levels.skillpunten(20));
        for (Klasse k : Klassen.alle()) {
            int zichtbaar = 0, totaal = 0;
            for (BoomKnoop b : k.boom()) {
                if (b.rij() >= Bomen.RIJEN) {
                    continue;   // z2-systemen: the zone-2 rows 4/5 are tested in VadsrijkZ2SystemenGameTests
                }
                if (b.rij() >= 0) {
                    zichtbaar++;
                    totaal += b.kosten();
                    helper.assertTrue(b.rij() < Bomen.RIJEN && b.kolom() < Bomen.KOLOMMEN, k.id() + ": node outside the grid " + b.id());
                } else {
                    helper.assertTrue(b.kosten() == 0, k.id() + ": an advancement node must be free: " + b.id());
                }
            }
            helper.assertTrue(zichtbaar == 12, k.id() + " should have 12 nodes, has " + zichtbaar);
            // greedily learn everything that can be learned at lv 20: never everything (the variant pair + not enough points)
            Set<String> heeft = new LinkedHashSet<>();
            boolean meer = true;
            while (meer) {
                meer = false;
                for (BoomKnoop b : k.boom()) {
                    if (b.rij() >= 0 && b.rij() < Bomen.RIJEN && Boom.waaromNiet(k, heeft, 20, b.id()) == null) {
                        heeft.add(b.id());
                        meer = true;
                    }
                }
            }
            helper.assertTrue(heeft.size() < 12, k.id() + ": everything fits in 19 points?");
            helper.assertTrue(Boom.vrij(k, heeft, 20) >= 0, k.id() + ": overspent " + Boom.vrij(k, heeft, 20));
            helper.assertTrue(totaal > 19 + 2, k.id() + ": the tree costs only " + totaal);
            // advancement nodes cost nothing and are not in the spent points
            Set<String> metAdv = new HashSet<>(heeft);
            metAdv.add(Bomen.advKnoop(k.id(), 1));
            metAdv.add(Bomen.advKnoop(k.id(), 2));
            helper.assertTrue(Boom.besteed(k, metAdv) == Boom.besteed(k, heeft), k.id() + ": advancement nodes cost points");
            helper.assertTrue("onbekend".equals(Boom.waaromNiet(k, Set.of(), 20, Bomen.advKnoop(k.id(), 1))), "advancement nodes can't be bought");
        }
        Klasse j = Klassen.van(Klassen.JAGER);
        helper.assertTrue(j != null, "no Pluisjager");
        helper.assertTrue("punten".equals(Boom.waaromNiet(j, Set.of(), 1, "j_scherpoog")), "lv 1 has no points");
        helper.assertTrue(Boom.waaromNiet(j, Set.of(), 2, "j_scherpoog") == null, "lv 2 can learn a row-0 node");
        helper.assertTrue("vereist".equals(Boom.waaromNiet(j, Set.of(), 20, "j_vuurregen")), "row 1 needs its row-0 node");
        helper.assertTrue("punten".equals(Boom.waaromNiet(j, Set.of("j_scherpoog"), 2, "j_vuurregen")), "2-point node with 0 left");
        helper.assertTrue(Boom.waaromNiet(j, Set.of("j_scherpoog"), 4, "j_vuurregen") == null, "1 + 2 points at lv 4");
        helper.assertTrue("uitgesloten".equals(Boom.waaromNiet(j, Set.of("j_scherpoog", "j_vuurregen"), 20, "j_kleefregen")), "variants exclude each other");
        helper.assertTrue("al".equals(Boom.waaromNiet(j, Set.of("j_scherpoog"), 20, "j_scherpoog")), "twice");
        Klasse r = Klassen.van(Klassen.RIDDER);
        helper.assertTrue(r != null && r.boom().stream().filter(b -> b.rij() < Bomen.RIJEN && !b.id().endsWith("_adv3")).count() == 14,   // z2-systemen: zone-2 rows + adv3 not counted
                "the Knuffelridder adapter has no tree");
        helper.assertTrue(Boom.waaromNiet(r, Set.of("r_knuffelspier", "r_slijmkussen"), 20, "r_kussenstorm") == null, "a|b requirement (b)");
        helper.assertTrue(Boom.waaromNiet(r, Set.of("r_knuffelspier", "r_vuurkussen"), 20, "r_kussenstorm") == null, "a|b requirement (a)");
        helper.assertTrue("vereist".equals(Boom.waaromNiet(r, Set.of("r_knuffelspier"), 20, "r_kussenstorm")), "a|b requirement (none)");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void respecAfkoeltijd(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        String k = Klassen.MAGIER;
        Set<String> knopen = Boom.knopen(d, k);
        helper.assertTrue("leeg".equals(Boom.respec(d, k, 1000)), "nothing to respec");
        helper.assertTrue(d.respecKlaar == 0, "an empty respec must not start the cooldown");
        knopen.add("m_wijs");
        knopen.add("m_glitterblink");
        knopen.add("m_adv1");
        long t = 5_000_000L;
        helper.assertTrue(Boom.respec(d, k, t) == null, "first respec");
        helper.assertTrue(knopen.size() == 1 && knopen.contains("m_adv1"), "respec keeps only the advancement: " + knopen);
        helper.assertTrue(d.respecKlaar == t + Boom.RESPEC_MS, "cooldown = 1 hour");
        knopen.add("m_wijs");
        helper.assertTrue("cooldown".equals(Boom.respec(d, k, t + 10 * 60 * 1000)), "10 min later: still waiting");
        helper.assertTrue(Boom.respecOver(d, t + 10 * 60 * 1000) == 50 * 60, "50 min left");
        helper.assertTrue(knopen.contains("m_wijs"), "a refused respec changed the tree");
        helper.assertTrue(Boom.respec(d, k, t + Boom.RESPEC_MS) == null, "after an hour: free again");
        helper.assertTrue(!knopen.contains("m_wijs") && knopen.contains("m_adv1"), "second respec");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void klasseWisselenHoudtLevels(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.klasse = Klassen.RIDDER;
        d.xp.put(Klassen.RIDDER, Levels.totaalVoor(7));
        Boom.knopen(d, Klassen.RIDDER).add("r_pluisvacht");
        helper.assertTrue(Spelers.level(p) == 7, "ridder lv 7");
        helper.assertTrue(KlasseBalie.wissel(p, Klassen.JAGER), "switch to jager");
        helper.assertTrue(Spelers.level(p) == 1 && d.klasse.equals(Klassen.JAGER), "a new class starts at lv 1");
        helper.assertTrue(bogen(p) == 1, "one starter bow, got " + bogen(p));
        helper.assertTrue(KlasseBalie.gekozen(p), "switching counts as having chosen");
        d.xp.put(Klassen.JAGER, Levels.totaalVoor(4) + 5);
        helper.assertTrue(Spelers.level(p) == 4, "jager lv 4");
        helper.assertTrue(!Spelers.heeftKnoop(p, "r_pluisvacht"), "the ridder's tree leaked into the jager");
        helper.assertTrue(KlasseBalie.wissel(p, Klassen.RIDDER), "back to ridder");
        helper.assertTrue(Spelers.level(p) == 7 && Spelers.level(p, Klassen.JAGER) == 4, "levels per class kept");
        helper.assertTrue(Spelers.heeftKnoop(p, "r_pluisvacht"), "the ridder's tree kept");
        helper.assertTrue(!KlasseBalie.wissel(p, Klassen.RIDDER), "switching to the same class does nothing");
        helper.assertTrue(!KlasseBalie.wissel(p, "bestaatniet"), "unknown class");
        helper.assertTrue(KlasseBalie.wissel(p, Klassen.JAGER) && bogen(p) == 1, "the starter bow only once");
        helper.assertTrue(Spelers.vads(p) <= Spelers.maxVads(p) && Spelers.vads(p) > 0, "vads within the new max");
        helper.succeed();
    }

    private static int bogen(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            Uitrusting u = Rollen.van(s);
            if (u != null && u.type().equals(Wapens.PLUISBOOG)) {
                n++;
            }
        }
        return n;
    }
}
