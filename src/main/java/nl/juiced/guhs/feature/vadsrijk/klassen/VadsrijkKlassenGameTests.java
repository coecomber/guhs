package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Knuffelsprong;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.ProefKlodder;
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

    // ---- fix round 1 (gevecht) ------------------------------------------------------------------------------------------

    /** Attack speed is a weapon stat; a melee click before the interval does nothing (no spam hits). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void aanvalstempoHoudtInterval(GameTestHelper helper) {
        helper.assertTrue(Math.abs(Aanvalstempo.seconden(BasisTypes.van(Wapens.PLUISBOOG)) - 1.1) < 1e-9, "boog 1.1 s");
        helper.assertTrue(Math.abs(Aanvalstempo.seconden(BasisTypes.van(Wapens.SNOEPKRUISBOOG)) - 2.0) < 1e-9, "kruisboog 2.0 s");
        helper.assertTrue(Math.abs(Aanvalstempo.seconden(BasisTypes.van("pluishamer")) - 3.5) < 1e-9, "pluishamer 3.5 s");
        helper.assertTrue(Math.abs(Aanvalstempo.seconden(BasisTypes.van(Wapens.ZWAMSTOKJE)) - 0.9) < 1e-9, "toverstok 0.9 s");
        for (BasisType t : BasisTypes.alle()) {
            if (t.slot() == Slot.WAPEN) {
                helper.assertTrue(Aanvalstempo.SOORT.containsKey(t.soort()) || Aanvalstempo.TYPE.containsKey(t.id()), "no interval for " + t.id());
            }
        }
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        SpelerData d = Spelers.data(p);
        // ranged: a jager with a pluisboog shoots every 22 ticks, a crossbow every 40
        d.klasse = Klassen.JAGER;
        p.setItemInHand(InteractionHand.MAIN_HAND, Rollen.stack(Rollen.rol(BasisTypes.van(Wapens.PLUISBOOG), 1, Zeldzaamheid.GEWOON, p.getRandom())));
        helper.assertTrue(Basisaanval.interval(p) == 22, "boog interval " + Basisaanval.interval(p));
        p.setItemInHand(InteractionHand.MAIN_HAND, Rollen.stack(Rollen.rol(BasisTypes.van(Wapens.SNOEPKRUISBOOG), 1, Zeldzaamheid.GEWOON, p.getRandom())));
        helper.assertTrue(Basisaanval.interval(p) == 40, "kruisboog interval " + Basisaanval.interval(p));
        // melee: the pluishamer hits once, a second click right after does nothing, even with a full vanilla cooldown
        d.klasse = Klassen.RIDDER;
        p.setItemInHand(InteractionHand.MAIN_HAND, Rollen.stack(Rollen.rol(BasisTypes.van("pluishamer"), 1, Zeldzaamheid.GEWOON, p.getRandom())));
        helper.assertTrue(Spelers.wapen(p) != null, "the ridder holds his hammer");
        ProefKlodder m = helper.spawn(KernRegistry.PROEFKLODDER.get(), 2, 1, 2);
        m.stelIn(1, Element.GEEN, false, Set.of());
        double vol = m.vads();
        volleKracht(p);
        helper.assertTrue(Schade.basisaanval(p, m), "first hammer hit");
        double na1 = m.vads();
        helper.assertTrue(na1 < vol, "the hit did damage");
        volleKracht(p);
        helper.assertTrue(!Schade.basisaanval(p, m) && m.vads() == na1, "a second click within 3.5 s must do nothing");
        Schade.VOLGENDE_KLAP.remove(p.getUUID());
        p.resetAttackStrengthTicker();
        helper.assertTrue(!Schade.basisaanval(p, m) && m.vads() == na1, "without the vanilla cooldown charged: nothing");
        helper.succeed();
    }

    /** The DPS ratio melee 100 / ranged 70 / magic 85, and no basic hit one-shots a normal zone mob of the same level. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void dpsTabelKlopt(GameTestHelper helper) {
        for (int lv : new int[] {1, 5, 10, 15, 20}) {
            double r = Balans.dps(Klassen.RIDDER, "kussenzwaard", lv);
            double j = Balans.dps(Klassen.JAGER, Wapens.PLUISBOOG, lv);
            double m = Balans.dps(Klassen.MAGIER, Wapens.ROZENSTAF, lv);
            helper.assertTrue(j / r > 0.65 && j / r < 0.75, "lv " + lv + ": ranged/melee = " + j / r);
            helper.assertTrue(m / r > 0.80 && m / r < 0.90, "lv " + lv + ": magic/melee = " + m / r);
            // the weakest normal zone-1 mob (wezens: 30 Vads at lv 1, +30 % per level); neutral element, no crit
            double mob = 30 * (1 + 0.3 * (lv - 1));
            for (BasisType t : BasisTypes.alle()) {
                if (t.slot() != Slot.WAPEN || t.klasse() == null || t.id().startsWith("baas_") || t.id().startsWith("z2baas_")) {   // (named boss weapons: never Gewoon)
                    continue;
                }
                Klasse k = Klassen.ofRidder(t.klasse());
                nl.juiced.guhs.feature.vadsrijk.kern.Stats s = k.basis(lv).kopie().plus(t.statsOp(lv));
                double hoofd = switch (Aanvalstempo.schadeSoort(t)) {
                    case AFSTAND -> s.get(Stat.BEHENDIGHEID);
                    case MAGIE -> s.get(Stat.WIJSHEID);
                    default -> s.get(Stat.KRACHT);
                };
                Uitrusting u = Rollen.rol(t, lv, Zeldzaamheid.GEWOON, helper.getLevel().getRandom());
                double klap = Rollen.klapSchade(u) * (1 + 0.02 * hoofd);
                helper.assertTrue(klap < mob, "lv " + lv + ": one " + t.id() + " hit (" + (int) klap + ") one-shots a " + (int) mob + " Vads mob");
            }
        }
        helper.succeed();
    }

    /** The Knuffelsprong never gives fall damage; a normal fall does. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void knuffelsprongGeenValschade(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        SpelerData d = Spelers.data(p);
        d.vads = Spelers.maxVads(p);
        Knuffelsprong.beschermTot(p, helper.getLevel().getGameTime() + 100);
        helper.assertTrue(Knuffelsprong.beschermd(p), "protected after a leap");
        helper.assertTrue(Knuffelsprong.valSchade(p, 12) == 0 && Spelers.vads(p) == Spelers.maxVads(p), "fall damage during the leap");
        Knuffelsprong.beschermTot(p, helper.getLevel().getGameTime() - 1);
        helper.assertTrue(!Knuffelsprong.beschermd(p), "protection ends");
        helper.assertTrue(Knuffelsprong.valSchade(p, 12) > 0 && Spelers.vads(p) < Spelers.maxVads(p), "a normal fall still hurts");
        helper.succeed();
    }

    private static void volleKracht(ServerPlayer p) {
        try {
            java.lang.reflect.Field f = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("attackStrengthTicker");
            f.setAccessible(true);
            f.setInt(p, 1000);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
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
