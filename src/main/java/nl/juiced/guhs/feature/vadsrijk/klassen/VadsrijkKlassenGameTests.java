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
 * (advancements survive it); g-klassen: the class switch on attack, the xp split, weapon requirements, the balans config, the save migration.
 * Run: {@code runGameTestServer -Pgt=VadsrijkKlassen}.
 */
public class VadsrijkKlassenGameTests {
    private static final String BATCH = "vadsrijk_klassen";

    /**
     * U-SPECS: the re-laid zone-1 rows (0-3): 16 nodes per class in 4 columns; without a spec only the trunk + side branch are
     * open (exactly 16 points = lv 17); the spec column needs the spec node; advancement/spec nodes are free and never bought.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void skillboomPuntenRekenen(GameTestHelper helper) {
        helper.assertTrue(Levels.skillpunten(20) == 19, "lv 20 should give 19 points, got " + Levels.skillpunten(20));
        for (Klasse k : Klassen.alle()) {
            int zichtbaar = 0, zonderSpec = 0;
            for (BoomKnoop b : k.boom()) {
                if (b.rij() >= Bomen.RIJEN) {
                    continue;   // (zone-2/3 rows: VadsrijkZ2/Z3SystemenGameTests, the layout: VadsrijkSpecsGameTests)
                }
                if (b.rij() >= 0) {
                    zichtbaar++;
                    helper.assertTrue(b.kolom() < Bomen.KOLOMMEN, k.id() + ": node outside the grid " + b.id());
                    if (b.kolom() == 1 || b.kolom() == 3) {
                        zonderSpec += b.kosten();
                    }
                } else {
                    helper.assertTrue(b.kosten() == 0, k.id() + ": an advancement/spec node must be free: " + b.id());
                }
            }
            helper.assertTrue(zichtbaar == 16, k.id() + " should have 16 zone-1 nodes, has " + zichtbaar);
            helper.assertTrue(zonderSpec == Levels.skillpunten(Advancement.LV1), k.id() + ": trunk + side cost " + zonderSpec + ", not 16");
            // greedy at lv 20 without a spec: never a spec-column node
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
            for (String id : heeft) {
                int kol = Boom.knoop(k, id).kolom();
                helper.assertTrue(kol == 1 || kol == 3, k.id() + ": " + id + " (spec column) without a spec");
            }
            helper.assertTrue(Boom.vrij(k, heeft, 20) >= 0, k.id() + ": overspent " + Boom.vrij(k, heeft, 20));
            Set<String> metAdv = new HashSet<>(heeft);
            metAdv.add(Bomen.advKnoop(k.id(), 1));
            metAdv.add(Bomen.advKnoop(k.id(), 2));
            helper.assertTrue(Boom.besteed(k, metAdv) == Boom.besteed(k, heeft), k.id() + ": advancement nodes cost points");
            helper.assertTrue("onbekend".equals(Boom.waaromNiet(k, Set.of(), 20, Bomen.advKnoop(k.id(), 1))), "advancement nodes can't be bought");
        }
        Klasse j = Klassen.van(Klassen.JAGER);
        helper.assertTrue(j != null, "no Pluisjager");
        helper.assertTrue("punten".equals(Boom.waaromNiet(j, Set.of(), 1, "j_snelle_pees")), "lv 1 has no points");
        helper.assertTrue(Boom.waaromNiet(j, Set.of(), 2, "j_snelle_pees") == null, "lv 2 can learn a row-0 trunk node");
        helper.assertTrue("vereist".equals(Boom.waaromNiet(j, Set.of(), 20, "j_scherpoog")), "a spec node needs the spec");
        helper.assertTrue(Boom.waaromNiet(j, Set.of("j_spec_scherpschutter"), 20, "j_scherpoog") == null, "with the spec it opens");
        helper.assertTrue("vereist".equals(Boom.waaromNiet(j, Set.of("j_spec_scherpschutter"), 20, "j_strikdraad")), "the other spec stays shut");
        helper.assertTrue("vereist".equals(Boom.waaromNiet(j, Set.of(), 20, "j_doorboor")), "row 1 needs its row-0 node");
        helper.assertTrue("punten".equals(Boom.waaromNiet(j, Set.of("j_snelle_pees"), 2, "j_doorboor")), "2-point node with 0 left");
        helper.assertTrue(Boom.waaromNiet(j, Set.of("j_snelle_pees"), 4, "j_doorboor") == null, "1 + 2 points at lv 4");
        helper.assertTrue(Boom.waaromNiet(j, Set.of("j_spec_strikkenzetter", "j_strikdraad", "j_vuurregen", "j_lichtvoetig"), 20, "j_kleefregen") == null,
                "a Strikkenzetter may take both rains now");
        helper.assertTrue("al".equals(Boom.waaromNiet(j, Set.of("j_snelle_pees"), 20, "j_snelle_pees")), "twice");
        helper.succeed();
    }

    /** U-SPECS: resetting the tree is always free (no cooldown); the spec and the advancements stay. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void respecAfkoeltijd(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        String k = Klassen.MAGIER;
        Set<String> knopen = Boom.knopen(d, k);
        helper.assertTrue("leeg".equals(Boom.respec(d, k, 1000)), "nothing to respec");
        knopen.add("m_wijs");
        knopen.add("m_glitterblink");
        knopen.add("m_adv1");
        knopen.add("m_spec_zegenzanger");
        long t = 5_000_000L;
        helper.assertTrue(Boom.respec(d, k, t) == null, "first respec");
        helper.assertTrue(knopen.size() == 2 && knopen.contains("m_adv1") && knopen.contains("m_spec_zegenzanger"),
                "respec keeps only the advancement and the spec: " + knopen);
        knopen.add("m_wijs");
        helper.assertTrue(Boom.respec(d, k, t + 1) == null, "right after: free again (no cooldown)");
        helper.assertTrue(Boom.respecOver(d, t + 1) == 0, "never a wait");
        helper.assertTrue(!knopen.contains("m_wijs") && knopen.contains("m_adv1"), "second respec");
        helper.succeed();
    }

    // ---- g-klassen (round 2): always all 3 classes, the class is the weapon in your hand, two levels -------------------------

    private static ItemStack wapen(ServerPlayer p, String type, int level) {
        return Rollen.stack(Rollen.rol(BasisTypes.van(type), level, Zeldzaamheid.GEWOON, p.getRandom()));
    }

    /**
     * Holding another class's weapon changes nothing; ATTACKING with it switches the class: its R/V/G on full cooldown, ult 0,
     * the Knuffelsprong untouched. No class weapon: no class (skills off), the last class stays.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void klasseWisseltBijAanval(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.klasse = Klassen.RIDDER;
        long nu = p.level().getGameTime();
        d.klaarOp.put(Knuffelsprong.ID, nu + 7);
        d.ultLading = 100;
        p.setItemInHand(InteractionHand.MAIN_HAND, wapen(p, "kussenzwaard", 1));
        helper.assertTrue(Spelers.voorAanval(p) && d.klasse.equals(Klassen.RIDDER) && d.ultLading == 100, "attacking with your own class's weapon: no switch");
        p.setItemInHand(InteractionHand.MAIN_HAND, wapen(p, Wapens.PLUISBOOG, 1));
        helper.assertTrue(d.klasse.equals(Klassen.RIDDER), "only holding the bow must not switch");
        helper.assertTrue(Klassen.JAGER.equals(Spelers.actieveWapenKlasse(p)), "the bow is a jager weapon");
        helper.assertTrue(Spelers.voorAanval(p), "the attack goes on");
        helper.assertTrue(d.klasse.equals(Klassen.JAGER), "attacking with the bow makes you a Pluisjager");
        helper.assertTrue(d.ultLading == 0, "ult meter at 0 after a switch");
        Klasse j = Klassen.van(Klassen.JAGER);
        for (var v : j.vaardigheden()) {
            long klaar = d.klaarOp.getOrDefault(v.id(), 0L);
            if (v.ult()) {
                continue;
            }
            helper.assertTrue(klaar == nu + nl.juiced.guhs.feature.vadsrijk.kern.Vaardigheden.cooldown(p, v) && klaar > nu,
                    v.id() + " must start on its full cooldown, klaar=" + klaar + " nu=" + nu);
        }
        helper.assertTrue(d.klaarOp.get(Knuffelsprong.ID) == nu + 7, "the Knuffelsprong keeps its own cooldown");
        // and back: the ridder's skills start on full cooldown again
        d.ultLading = 60;
        p.setItemInHand(InteractionHand.MAIN_HAND, wapen(p, "pluishamer", 1));
        helper.assertTrue(Spelers.voorAanval(p) && d.klasse.equals(Klassen.RIDDER) && d.ultLading == 0, "back to ridder on a hammer hit");
        for (var v : Klassen.van(Klassen.RIDDER).vaardigheden()) {
            helper.assertTrue(v.ult() || d.klaarOp.getOrDefault(v.id(), 0L) > nu, v.id() + " on cooldown after switching back");
        }
        // empty hand: a punch, no class weapon (skills off), the last class stays (HUD grey)
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(Spelers.voorAanval(p) && Spelers.actieveWapenKlasse(p) == null && d.klasse.equals(Klassen.RIDDER), "no weapon: punch, last class kept");
        helper.assertTrue(Spelers.wapenKlasse(BasisTypes.van(Wapens.SLIJMBOEK)).equals(Klassen.MAGIER), "a book is a magier weapon");
        helper.succeed();
    }

    /**
     * Xp: all of it to the generic level and to the class of the weapon; a defeat gives the class xp to the class that dealt the
     * most damage to that mob; levels are separate (generic gives Vads/Verdediging, class the main stat).
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void xpGaatNaarGeneriekEnKlasse(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.klasse = Klassen.JAGER;
        Spelers.geefXp(p, 50, "test");
        helper.assertTrue(d.generiekXp == 50 && d.xp(Klassen.JAGER) == 50 && d.xp(Klassen.RIDDER) == 0 && d.xp(Klassen.MAGIER) == 0,
                "50 xp to generic and jager only: g=" + d.generiekXp + " " + d.xp);
        // a mob hit for 10 as jager and 30 as magier: the magier gets the class xp
        ProefKlodder m = helper.spawn(KernRegistry.PROEFKLODDER.get(), 2, 1, 2);
        m.stelIn(1, Element.GEEN, false, Set.of());
        m.krijgSchade(Math.min(10, m.vads() / 4), p);
        d.klasse = Klassen.MAGIER;
        long g0 = d.generiekXp, j0 = d.xp(Klassen.JAGER);
        m.krijgSchade(m.vads() + 5, p);   // (defeat)
        long xp = m.xp();
        helper.assertTrue(d.generiekXp == g0 + xp, "the defeat's xp to generic: " + (d.generiekXp - g0) + " vs " + xp);
        helper.assertTrue(d.xp(Klassen.MAGIER) == xp && d.xp(Klassen.JAGER) == j0, "the class with the most damage (magier) gets it: " + d.xp);
        // the two levels drive different stats
        Spelers.zetGeneriek(p, 10);
        Spelers.zetKlasseLevel(p, Klassen.MAGIER, 2);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Klasse k = Klassen.van(Klassen.MAGIER);
        var s = Spelers.stats(p);
        helper.assertTrue(Math.abs(s.get(Stat.VADS) - KlasseBalans.get(Klassen.MAGIER, "vads") * k.basis(10).get(Stat.VADS)) < 1e-6,
                "Vads from the generic level: " + s.get(Stat.VADS));
        helper.assertTrue(Math.abs(s.get(Stat.WIJSHEID) - k.basis(2).get(Stat.WIJSHEID)) < 1e-6, "Wijsheid from the class level: " + s.get(Stat.WIJSHEID));
        helper.assertTrue(Spelers.level(p) == 10 && Spelers.klasseLevel(p) == 2 && Spelers.level(p, Klassen.RIDDER) == 1, "generic 10, magier 2, ridder 1");
        helper.succeed();
    }

    /** A weapon needs its class's level: held but no attack (and no class switch) until you have it; armour needs the generic level. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void wapenVereistKlasseLevel(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.klasse = Klassen.RIDDER;
        Spelers.zetGeneriek(p, 20);
        Spelers.zetKlasseLevel(p, Klassen.JAGER, 5);
        p.setItemInHand(InteractionHand.MAIN_HAND, wapen(p, Wapens.PLUISBOOG, 8));
        Uitrusting u = Spelers.wapenInHand(p);
        helper.assertTrue(u != null && !Spelers.magWapen(p, u) && Spelers.wapen(p) == null, "jager 5 can't use a lv 8 bow (generic 20 doesn't count)");
        helper.assertTrue(!Spelers.voorAanval(p) && d.klasse.equals(Klassen.RIDDER), "an unmet weapon doesn't attack and doesn't switch");
        Spelers.zetKlasseLevel(p, Klassen.JAGER, 8);
        helper.assertTrue(Spelers.wapen(p) != null && Spelers.voorAanval(p) && d.klasse.equals(Klassen.JAGER), "jager 8: the bow works and switches");
        // armour: the generic level
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, Rollen.stack(Rollen.rol(BasisTypes.van("pluishelm"), 25, Zeldzaamheid.GEWOON, p.getRandom())));
        double zonder = Spelers.stats(p).get(Stat.VERDEDIGING);
        Spelers.zetGeneriek(p, 25);
        double met = Spelers.stats(p).get(Stat.VERDEDIGING);
        Klasse j = Klassen.van(Klassen.JAGER);
        helper.assertTrue(met - zonder > (j.basis(25).get(Stat.VERDEDIGING) - j.basis(20).get(Stat.VERDEDIGING)) * KlasseBalans.get(Klassen.JAGER, "verdediging") + 0.5,
                "a lv 25 helmet counts from generic 25 (" + zonder + " -> " + met + ")");
        helper.succeed();
    }

    /** The balans config: multipliers per class work live (stats, damage, cooldowns), melee reduction, back to defaults. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void balansConfigWerkt(GameTestHelper helper) {
        KlasseBalans.zonderBestand = true;
        try {
            ServerPlayer p = GuhMockPlayer.of(helper);
            SpelerData d = Spelers.data(p);
            d.klasse = Klassen.MAGIER;
            p.setItemInHand(InteractionHand.MAIN_HAND, wapen(p, Wapens.ROZENSTAF, 1));
            double vads = Spelers.maxVads(p);
            helper.assertTrue(KlasseBalans.zet(Klassen.MAGIER, "vads", 2.0, false) == null, "set vads");
            helper.assertTrue(Math.abs(Spelers.maxVads(p) - 2 * vads) < 1e-6, "magier vads x2: " + vads + " -> " + Spelers.maxVads(p));
            Klasse k = Klassen.van(Klassen.MAGIER);
            var v = k.vaardigheden().get(0);
            int cd = nl.juiced.guhs.feature.vadsrijk.kern.Vaardigheden.cooldown(p, v);
            KlasseBalans.zet(Klassen.MAGIER, "cooldowns", 0.5, false);
            int cd2 = nl.juiced.guhs.feature.vadsrijk.kern.Vaardigheden.cooldown(p, v);
            helper.assertTrue(Math.abs(cd2 - cd / 2.0) <= 1, "cooldowns x0.5: " + cd + " -> " + cd2);
            KlasseBalans.zet(Klassen.MAGIER, "schade", 1.5, false);
            KlasseBalans.zet(Klassen.MAGIER, "skillschade", 2.0, false);
            helper.assertTrue(Math.abs(KlasseBalans.klapFactor(Klassen.MAGIER, "basis", null, false) - 1.5) < 1e-9, "basic x1.5");
            helper.assertTrue(Math.abs(KlasseBalans.klapFactor(Klassen.MAGIER, "roze_vuurbal", null, false) - 3.0) < 1e-9, "skill x1.5 x2");
            helper.assertTrue(Math.abs(KlasseBalans.klapFactor(null, "mob", Klassen.RIDDER, true) - 0.8) < 1e-9, "ridder with a melee weapon takes 20 % less");
            helper.assertTrue(KlasseBalans.klapFactor(null, "mob", Klassen.RIDDER, false) == 1, "no melee weapon: no reduction");
            helper.assertTrue(KlasseBalans.zet("bestaatniet", "vads", 1, false) != null && KlasseBalans.zet(Klassen.RIDDER, "onzin", 1, false) != null,
                    "unknown class/key refused");
            KlasseBalans.zet(KlasseBalans.ALGEMEEN, "xp_schaal_generiek", 2.0, false);
            helper.assertTrue(Levels.xpVoorGeneriek(5) == 2 * Levels.xpVoor(5) || Math.abs(Levels.xpVoorGeneriek(5) - 2 * Levels.xpVoor(5)) <= 1,
                    "generic curve x2 on its own");
            KlasseBalans.standaardAlles();
            helper.assertTrue(Math.abs(Spelers.maxVads(p) - vads) < 1e-6 && Levels.xpVoorGeneriek(5) == Levels.xpVoor(5), "defaults again");
            // the Knuffelridder's extra Vads per class level
            d.klasse = Klassen.RIDDER;
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            double r1 = Spelers.maxVads(p);
            Spelers.zetKlasseLevel(p, Klassen.RIDDER, 11);
            helper.assertTrue(Math.abs(Spelers.maxVads(p) - r1 - 10 * KlasseBalans.get(Klassen.RIDDER, "vads_per_klasselevel")) < 1e-6,
                    "ridder +4 Vads per class level: " + r1 + " -> " + Spelers.maxVads(p));
        } finally {
            KlasseBalans.standaardAlles();
            KlasseBalans.zonderBestand = false;
        }
        helper.succeed();
    }

    /** An old save (xp per class only) gets generic xp = the highest class xp; a new save keeps its generic xp. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void opslagMigratie(GameTestHelper helper) {
        var reg = helper.getLevel().registryAccess();
        SpelerData oud = new SpelerData();
        oud.xp.put(Klassen.RIDDER, Levels.totaalVoor(12) + 3);
        oud.xp.put(Klassen.JAGER, Levels.totaalVoor(7));
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, reg);
        oud.serialize(out);
        net.minecraft.nbt.CompoundTag tag = out.buildResult();
        tag.remove("gxp");   // (= a save from before g-klassen)
        SpelerData na = new SpelerData();
        na.deserialize(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, reg, tag));
        helper.assertTrue(na.gemigreerd, "flagged as migrated");
        helper.assertTrue(na.level() == 12 && na.generiekXp == Levels.totaalVoor(12) + 3, "generic = the highest class (12), got " + na.level());
        helper.assertTrue(na.level(Klassen.RIDDER) == 12 && na.level(Klassen.JAGER) == 7, "class levels kept");
        na.generiekXp = Levels.totaalVoorGeneriek(15);
        var out2 = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, reg);
        na.serialize(out2);
        SpelerData weer = new SpelerData();
        weer.deserialize(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, reg, out2.buildResult()));
        helper.assertTrue(!weer.gemigreerd && weer.level() == 15, "a new save keeps its generic level");
        helper.assertTrue(SpelerData.migreerGeneriek(java.util.Map.of("a", Long.MAX_VALUE / 4)) == Levels.totaalVoorGeneriek(Levels.MAX_GENERIEK), "capped");
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
        zonderTempoBonus(p, Wapens.PLUISBOOG);
        helper.assertTrue(Basisaanval.interval(p) == 22, "boog interval " + Basisaanval.interval(p));
        zonderTempoBonus(p, Wapens.SNOEPKRUISBOOG);
        helper.assertTrue(Basisaanval.interval(p) == 40, "kruisboog interval " + Basisaanval.interval(p));
        // melee: the pluishamer hits once, a second click right after does nothing, even with a full vanilla cooldown
        d.klasse = Klassen.RIDDER;
        p.setItemInHand(InteractionHand.MAIN_HAND, Rollen.stack(Rollen.rol(BasisTypes.van("pluishamer"), 1, Zeldzaamheid.GEWOON, p.getRandom())));
        helper.assertTrue(Spelers.wapen(p) != null, "the ridder holds his hammer");
        ProefKlodder m = helper.spawn(KernRegistry.PROEFKLODDER.get(), 2, 1, 2);
        m.stelIn(10, Element.GEEN, false, Set.of());   // (h-melee: enough Vads for three hammer hits)
        double vol = m.vads();
        volleKracht(p);
        helper.assertTrue(Schade.basisaanval(p, m), "first hammer hit");
        double na1 = m.vads();
        helper.assertTrue(na1 < vol, "the hit did damage");
        volleKracht(p);
        helper.assertTrue(!Schade.basisaanval(p, m) && m.vads() == na1, "a second click within 3.5 s must do nothing");
        helper.assertTrue(!Schade.WACHTEND.containsKey(p.getUUID()), "a click 3.5 s early isn't remembered either");
        // h-melee: the server rhythm alone decides (the vanilla ticker is reset by every click, also misses, so it swallowed hits)
        Schade.VOLGENDE_KLAP.remove(p.getUUID());
        p.resetAttackStrengthTicker();
        helper.assertTrue(Schade.basisaanval(p, m) && m.vads() < na1, "ready by the server's rhythm: a hit, whatever the vanilla ticker says");
        Schade.VOLGENDE_KLAP.remove(p.getUUID());
        helper.succeed();
    }

    /** (integration) A rolled weapon may carry a random Aanvalssnelheid bonus (kern Bonussen pool): reroll until it has none. */
    private static void zonderTempoBonus(ServerPlayer p, String type) {
        for (int i = 0; i < 100; i++) {
            p.setItemInHand(InteractionHand.MAIN_HAND, Rollen.stack(Rollen.rol(BasisTypes.van(type), 1, Zeldzaamheid.GEWOON, p.getRandom())));
            if (Spelers.stats(p).get(nl.juiced.guhs.feature.vadsrijk.kern.Stat.AANVALSSNELHEID) == 0) {
                return;
            }
        }
    }

    /** The DPS ratio melee 100 / ranged 70 / magic 85, and no basic hit one-shots a normal zone mob of the same level. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void dpsTabelKlopt(GameTestHelper helper) {
        for (int lv : new int[] {1, 5, 10, 15, 20}) {
            double r = Balans.dps(Klassen.RIDDER, "kussenzwaard", lv);
            double j = Balans.dps(Klassen.JAGER, Wapens.PLUISBOOG, lv);
            double m = Balans.dps(Klassen.MAGIER, Wapens.ROZENSTAF, lv);
            helper.assertTrue(j / r > 0.65 && j / r < 0.75, "lv " + lv + ": ranged/melee = " + j / r);
            // U-SPECS: the two specs of a class stay within 10 % of each other at every advancement step
            for (var kl : new String[] {Klassen.RIDDER, Klassen.JAGER, Klassen.MAGIER}) {
                var specs = nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec.vanKlasse(kl);
                for (int stap = 1; stap <= 3; stap++) {
                    double a = nl.juiced.guhs.feature.vadsrijk.klassen.spec.SpecBalans.factor(specs.get(0), stap);
                    double b2 = nl.juiced.guhs.feature.vadsrijk.klassen.spec.SpecBalans.factor(specs.get(1), stap);
                    helper.assertTrue(a > 1.05 && b2 > 1.05, kl + " step " + stap + ": a spec adds too little (" + a + ", " + b2 + ")");
                    helper.assertTrue(Math.abs(a / b2 - 1) <= 0.10, kl + " step " + stap + ": " + specs.get(0).id + " " + a + " vs "
                            + specs.get(1).id + " " + b2);
                    if (stap > 1) {
                        helper.assertTrue(a > nl.juiced.guhs.feature.vadsrijk.klassen.spec.SpecBalans.factor(specs.get(0), stap - 1)
                                && b2 > nl.juiced.guhs.feature.vadsrijk.klassen.spec.SpecBalans.factor(specs.get(1), stap - 1), kl + ": step " + stap + " adds nothing");
                    }
                }
            }
            helper.assertTrue(m / r > 0.80 && m / r < 0.90, "lv " + lv + ": magic/melee = " + m / r);
            // the weakest normal zone-1 mob (wezens: 30 Vads at lv 1, +30 % per level); neutral element, no crit
            double mob = 30 * (1 + 0.3 * (lv - 1));
            for (BasisType t : BasisTypes.alle()) {
                if (t.slot() != Slot.WAPEN || t.klasse() == null || t.id().startsWith("baas_") || (t.id().length() > 6 && t.id().charAt(0) == 'z' && Character.isDigit(t.id().charAt(1)) && t.id().startsWith("baas_", 2))) {   // (named boss weapons: never Gewoon; K-INT + z3baas_; T-INT: every zone)
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
