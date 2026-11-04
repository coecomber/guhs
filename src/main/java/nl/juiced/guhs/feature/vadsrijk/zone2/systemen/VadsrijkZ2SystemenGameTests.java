package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.klassen.Advancement;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone 2 systemen: the cap and the xp curve to 35, the gem sockets (insert/remove, a gem is never lost, the stats count them,
 * the rolls), the 3rd advancement step at lv 30, the tree rows that open at lv 20/26, and Bevroren's heavy-hit bonus.
 * Run: {@code runGameTestServer -Pgt=VadsrijkZ2Systemen}.
 */
public class VadsrijkZ2SystemenGameTests {
    private static final String BATCH = "vadsrijk_z2_systemen";

    @GuhTest(template = "empty", batch = BATCH)
    public static void levelCap35(GameTestHelper helper) {
        helper.assertTrue(Levels.MAX >= 35, "cap at least 35, is " + Levels.MAX);   // (z3-systemen: 50)
        for (int l = 1; l <= Levels.MAX; l++) {
            helper.assertTrue(Levels.levelVan(Levels.totaalVoor(l)) == l, "level " + l + " round trip");
            helper.assertTrue(l == 1 || Levels.levelVan(Levels.totaalVoor(l) - 1) == l - 1, "one xp short of " + l);
            helper.assertTrue(l == Levels.MAX || Levels.xpVoor(l + 1) > Levels.xpVoor(l), "the curve rises at " + l);
        }
        helper.assertTrue(Levels.levelVan(Long.MAX_VALUE / 4) == Levels.MAX, "capped at " + Levels.MAX);
        helper.assertTrue(Levels.skillpunten(35) - Levels.skillpunten(20) == 15, "lv 21-35 gives 15 points");
        for (Klasse k : Klassen.alle()) {
            helper.assertTrue(k.basis(35).get(Stat.VADS) > k.basis(20).get(Stat.VADS), k.id() + ": stats keep growing after 20");
        }
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.klasse = Klassen.RIDDER;
        Spelers.geefXp(p, Levels.totaalVoor(32), "test");
        helper.assertTrue(Spelers.level(p) == 32, "xp up to lv 32, got " + Spelers.level(p));
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void sokjesNooitKwijt(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Uitrusting u = Rollen.rol(BasisTypes.van(Z2Wapens.KRISTALKLING), 25, Zeldzaamheid.ZELDZAAM, RandomSource.create(1)).metSokjes(List.of("", ""));
        p.getInventory().setItem(0, Rollen.stack(u));
        p.getInventory().setItem(1, Kristallen.stack("rozekwarts", 1, 2));
        double vadsVoor = Rollen.stats(u).get(Stat.VADS);
        // insert
        String[] r = Juwelier.zet(p, 0, 0, 1);
        Uitrusting na = Rollen.van(p.getInventory().getItem(0));
        helper.assertTrue(na != null && na.sokjes().get(0).equals("rozekwarts:1"), "gem in socket 0: " + (na == null ? null : na.sokjes()) + " " + r[0]);
        helper.assertTrue(p.getInventory().getItem(1).getCount() == 1, "one crystal used");
        helper.assertTrue(Math.abs(Rollen.stats(na).get(Stat.VADS) - vadsVoor - 35 * 1.7) < 0.01, "the stats count the gem");
        // a full socket refuses, nothing is used
        Juwelier.zet(p, 0, 0, 1);
        helper.assertTrue(p.getInventory().getItem(1).getCount() == 1, "a full socket takes nothing");
        // the component survives a save
        var enc = Uitrusting.CODEC.encodeStart(NbtOps.INSTANCE, na).getOrThrow();
        helper.assertTrue(Uitrusting.CODEC.parse(NbtOps.INSTANCE, enc).getOrThrow().equals(na), "sockets survive the codec");
        // removing without coins: refused, the gem stays
        Spelers.data(p).munten = 0;
        Juwelier.haal(p, 0, 0);
        helper.assertTrue(Rollen.van(p.getInventory().getItem(0)).sokjes().get(0).equals("rozekwarts:1"), "no coins: the gem stays");
        // with coins: the crystal comes back
        Spelers.data(p).munten = 1000;
        Juwelier.haal(p, 0, 0);
        helper.assertTrue(Rollen.van(p.getInventory().getItem(0)).sokjes().get(0).isEmpty(), "socket empty again");
        helper.assertTrue(Spelers.data(p).munten == 1000 - Juwelier.PRIJS[1], "fee paid");
        int terug = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if ("rozekwarts:1".equals(Kristallen.gem(s))) {
                terug += s.getCount();
            }
        }
        helper.assertTrue(terug == 2, "both crystals are there again, got " + terug);
        // the rolls: zone 1 never more than 1 (Legendary+), zone 2 at most 2
        RandomSource rnd = RandomSource.create(7);
        var bt = BasisTypes.van(Z2Wapens.KRISTALKLING);
        for (int i = 0; i < 300; i++) {
            Zeldzaamheid z = Zeldzaamheid.values()[i % Zeldzaamheid.values().length];
            int z1 = Rollen.rol(bt, 12, z, rnd).sokjes().size(), z2 = Rollen.rol(bt, 28, z, rnd).sokjes().size();
            helper.assertTrue(z1 == (z.ordinal() >= Zeldzaamheid.LEGENDARISCH.ordinal() ? 1 : 0), "zone 1 sockets " + z + ": " + z1);
            helper.assertTrue(z2 <= 2 && (z != Zeldzaamheid.GUHDDELIJK || z2 == 2), "zone 2 sockets " + z + ": " + z2);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void advancementStap3(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.klasse = Klassen.JAGER;
        d.xp.put(Klassen.JAGER, Levels.totaalVoor(29));
        Boom.knopen(d, Klassen.JAGER).add("j_adv1");
        Boom.knopen(d, Klassen.JAGER).add("j_adv2");
        helper.assertTrue(Advancement.stap(p, Klassen.JAGER) == 2 && !Advancement.kanDoen(p), "lv 29: not yet");
        d.xp.put(Klassen.JAGER, Levels.totaalVoor(30));
        helper.assertTrue(Advancement.kanDoen(p), "lv 30: step 3 is open");
        double kritVoor = Spelers.stats(p).get(Stat.KRITIEK);
        helper.assertTrue(Advancement.doe(p), "the ceremony");
        helper.assertTrue(Advancement.stap(p, Klassen.JAGER) == 3, "step 3 done");
        helper.assertTrue(Advancement.titelKey(p).equals("vadsrijk.klassen.titel.pluisjager.3"), "new title " + Advancement.titelKey(p));
        helper.assertTrue(Spelers.stats(p).get(Stat.KRITIEK) > kritVoor, "the adv3 node gives its stats");
        helper.assertTrue(!Advancement.kanDoen(p) && !Advancement.doe(p), "no 4th step before lv 50");   // (z3-systemen: step 4 at 50)
        Boom.knopen(d, Klassen.JAGER).add("j_scherpoog");
        d.respecKlaar = 0;
        helper.assertTrue(Boom.respec(d, Klassen.JAGER, System.currentTimeMillis()) == null, "respec");
        helper.assertTrue(Advancement.stap(p, Klassen.JAGER) == 3, "a respec keeps the advancement");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void boomRijenOpenOpLevel(GameTestHelper helper) {
        for (Klasse k : Klassen.alle()) {
            int nieuw = 0;
            for (BoomKnoop b : k.boom()) {
                if (b.rij() >= Bomen.RIJEN && b.rij() <= 5) {   // (z3-systemen: rows 6/7 are zone 3)
                    nieuw++;
                    helper.assertTrue(b.rij() <= 5 && b.kolom() < Bomen.KOLOMMEN, k.id() + ": outside the grid " + b.id());
                    helper.assertTrue(Boom.MIN_LEVEL.getOrDefault(b.id(), 0) >= 20, k.id() + ": zone-2 node without level gate " + b.id());
                }
            }
            helper.assertTrue(nieuw == 6, k.id() + " should have 6 zone-2 nodes, has " + nieuw);
            // greedy at lv 35: never everything
            Set<String> heeft = new LinkedHashSet<>();
            boolean meer = true;
            while (meer) {
                meer = false;
                for (BoomKnoop b : k.boom()) {
                    if (b.rij() >= 0 && Boom.waaromNiet(k, heeft, 35, b.id()) == null) {
                        heeft.add(b.id());
                        meer = true;
                    }
                }
            }
            helper.assertTrue(Boom.vrij(k, heeft, 35) >= 0, k.id() + ": overspent");
            helper.assertTrue(heeft.size() < 18, k.id() + ": everything fits in 34 points?");
        }
        Klasse r = Klassen.van(Klassen.RIDDER);
        Set<String> heeft = new LinkedHashSet<>(List.of("r_pluisvacht", "r_taai", "r_beschermknuffel", "r_bolwerk"));
        helper.assertTrue("vereist".equals(Boom.waaromNiet(r, Set.of("r_pluisvacht"), 35, "r_ijspantser")), "needs a capstone first");
        helper.assertTrue("level".equals(Boom.waaromNiet(r, heeft, 19, "r_ijspantser")), "row 4 is closed at lv 19");
        helper.assertTrue(Boom.waaromNiet(r, heeft, 20, "r_ijspantser") == null, "row 4 opens at lv 20");
        heeft.add("r_ijspantser");
        helper.assertTrue("level".equals(Boom.waaromNiet(r, heeft, 25, "r_kristalhart")), "row 5 is closed at lv 25");
        helper.assertTrue(Boom.waaromNiet(r, heeft, 26, "r_kristalhart") == null, "row 5 opens at lv 26");
        // the stat node counts
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.klasse = Klassen.RIDDER;
        d.xp.put(Klassen.RIDDER, Levels.totaalVoor(26));
        double voor = Spelers.stats(p).get(Stat.VADS);
        Boom.knopen(d, Klassen.RIDDER).addAll(heeft);
        Boom.knopen(d, Klassen.RIDDER).add("r_kristalhart");
        helper.assertTrue(Spelers.stats(p).get(Stat.VADS) >= voor + 400, "ijspantser + kristalhart count");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void bevrorenZwareKlap(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        var doel = GuhMockPlayer.of(helper);
        var skill = new nl.juiced.guhs.feature.vadsrijk.kern.Schade.Klap(p, doel, 100, nl.juiced.guhs.feature.vadsrijk.kern.Element.GEEN,
                nl.juiced.guhs.feature.vadsrijk.kern.Schade.Soort.MELEE, false, "kussenklap");
        var basis = new nl.juiced.guhs.feature.vadsrijk.kern.Schade.Klap(p, doel, 100, nl.juiced.guhs.feature.vadsrijk.kern.Element.GEEN,
                nl.juiced.guhs.feature.vadsrijk.kern.Schade.Soort.MELEE, false, "basis");
        helper.assertTrue(Bevroren.voorKlap(skill, 100) == 100, "not frozen: nothing extra");
        Statussen.geef(doel, Status.BEVROREN, 100, 1, p);
        helper.assertTrue(Math.abs(Bevroren.voorKlap(skill, 100) - 130) < 0.01, "frozen + skill: +30 %");
        helper.assertTrue(Bevroren.voorKlap(basis, 100) == 100, "a normal basic hit is not heavy");
        helper.assertTrue(Spelers.stats(doel).get(Stat.LOOPSNELHEID) <= Spelers.stats(p).get(Stat.LOOPSNELHEID) - 29, "a frozen player is slower");
        Bevroren.ontdooi((ServerPlayer) doel, 200);
        helper.assertTrue(!Bevroren.heeft(doel), "chocomelk thaws");
        Bevroren.geef(doel, 100, null);
        helper.assertTrue(!Bevroren.heeft(doel), "and keeps you warm");
        helper.succeed();
    }
}
