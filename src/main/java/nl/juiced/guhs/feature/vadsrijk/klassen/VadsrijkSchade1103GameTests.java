package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonus;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Erfstuk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2WezensRegistry;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.3 schade-audit (SCHADE_1103_AUDIT.md): the two real lv-35 builds of the live server (Coecomber's Pluisjager with De
 * Schaduwloze, Luc_Ermeling's Vadsmagier with the Spiegelstaf, gear copied 1:1 from their save files of 2026-11-14) through the
 * real damage pipeline ({@code Spelers.stats}, {@code Spelers.klapSchade}, {@code Schade.doe} with every VOOR_KLAP hook) against a
 * zone-2 creature and the Galmwolf. Asserts the design time-to-kill: a normal zone-2 mob of lv 28 takes 3+ basic shots on
 * average, the zone boss lasts 45+ s solo on basic shots. Logs the factor breakdown ("SCHADE1103" lines). Run:
 * {@code runGameTestServer -Pgt=VadsrijkSchade1103}.
 */
public class VadsrijkSchade1103GameTests {
    private static final String BATCH = "vadsrijk_schade1103";

    /** A piece exactly as in a save file: type, rarity, level, Vertrouwd xp, element and its rolled bonuses (stat name, value pairs). */
    static ItemStack stuk(String type, Zeldzaamheid z, int lv, int erfXp, Element el, Object... bon) {
        List<Bonus> b = new ArrayList<>();
        for (int i = 0; i + 1 < bon.length; i += 2) {
            b.add(new Bonus(Stat.van((String) bon[i]), ((Number) bon[i + 1]).doubleValue()));
        }
        return Rollen.stack(new Uitrusting(type, z, lv, b, el, 0, 0, false, false, 1L, List.of(), new Erfstuk(erfXp, 0, 0)));
    }

    static void klasse(ServerPlayer p, String klasse, int lv, String boom) {
        SpelerData d = Spelers.data(p);
        d.klasse = klasse;
        d.generiekXp = Levels.totaalVoorGeneriek(lv);
        d.xp.put(klasse, Levels.totaalVoor(lv));
        d.boom.put(klasse, new LinkedHashSet<>(List.of(boom.split(","))));
    }

    /** Coecomber, live 2026-11-14: Pluisjager 35 (Scherpschutter, step 1), De Schaduwloze Legendarisch lv 30 in hand. */
    static void coecomber(ServerPlayer p) {
        klasse(p, Klassen.JAGER, 35, "j_snelle_pees,j_lichtvoetig,j_doorboor,j_pijlenkoker,j_spec_scherpschutter,j_adv1,j_scherpoog,j_spanpees,"
                + "j_valkenoog,j_vallende_ster,j_sluipschutter,j_jachtroes,j_jachtinstinct");
        Element g = Element.GEEN;
        p.setItemInHand(InteractionHand.MAIN_HAND, stuk("z2baas_schaduwloze", Zeldzaamheid.LEGENDARISCH, 30, 11, Element.NATUUR,
                "kans_op_effect", 7.0, "levensroof", 3.9, "behendigheid", 12.0, "kritiek", 3.5, "wijsheid", 13.0));
        p.setItemSlot(EquipmentSlot.HEAD, stuk("kristalkroon", Zeldzaamheid.EPISCH, 35, 10, g, "kritiekschade", 14.2, "aanvalssnelheid", 8.5, "vads", 57.0,
                "loopsnelheid", 4.0));
        p.setItemSlot(EquipmentSlot.FEET, stuk("kristalschoentjes", Zeldzaamheid.EPISCH, 35, 45, g, "aanvalssnelheid", 6.7, "elementschade", 12.5,
                "kritiekschade", 16.6, "elementweerstand", 11.4));
        p.setItemSlot(EquipmentSlot.CHEST, stuk("buitvest", Zeldzaamheid.ZELDZAAM, 25, 295, g, "wijsheid", 10.0, "healkracht", 7.3));
        p.setItemSlot(EquipmentSlot.LEGS, stuk("schotsbroek", Zeldzaamheid.ZELDZAAM, 35, 66, g, "aanvalssnelheid", 9.4, "kracht", 14.0, "elementweerstand", 12.4));
        Sieraden.zet(p, Slot.KETTING, stuk("spiegelketting", Zeldzaamheid.GEWOON, 25, 45, g, "kritiek", 3.8));
        Sieraden.zet(p, Slot.RING, stuk("roze_ring", Zeldzaamheid.EPISCH, 20, 668, g, "behendigheid", 9.0, "elementschade", 7.9, "verdediging_negeren", 4.9));
        Sieraden.zet(p, Slot.HANDSCHOENEN, stuk("kristalhandschoenen", Zeldzaamheid.GEWOON, 28, 10, g, "kracht", 12.0));
        Sieraden.zet(p, Slot.RING2, stuk("roze_ring", Zeldzaamheid.LEGENDARISCH, 19, 599, g, "kritiek", 3.1, "verdediging", 5.96, "levensroof", 3.7,
                "kritiekschade", 13.3));
        Sieraden.zet(p, Slot.OORBEL, stuk("pluisoorbel", Zeldzaamheid.ZELDZAAM, 19, 389, g, "wijsheid", 8.0, "kracht", 9.0));
        Sieraden.zet(p, Slot.CAPE, stuk("buitcape", Zeldzaamheid.GEWOON, 29, 45, g, "kracht", 12.0));
        Sieraden.zet(p, Slot.ARMBAND, stuk("kristalarmband", Zeldzaamheid.ZELDZAAM, 35, 58, g, "vads", 58.0, "behendigheid", 14.9, "healkracht", 12.6));
    }

    /** Luc_Ermeling, live 2026-11-14: Vadsmagier 35 (Zegenzanger, step 1), the Spiegelstaf (Roze, Legendarisch lv 30) in hand. */
    static void luc(ServerPlayer p) {
        klasse(p, Klassen.MAGIER, 35, "m_zachte_handjes,m_warme_handen,m_bronschild,m_wijs,m_hartenbron,m_glitterblink,m_energievonk,m_tijdsprong,"
                + "m_kristalgeest,m_spec_zegenzanger,m_adv1,m_zangstem,m_heldere_bron,m_drievuur,m_manastroom,m_grote_bron,m_kettingbolt,m_koorkracht");
        Element g = Element.GEEN;
        p.setItemInHand(InteractionHand.MAIN_HAND, stuk("z2baas_spiegelstaf", Zeldzaamheid.LEGENDARISCH, 30, 38, Element.ROZE,
                "verdediging_negeren", 5.5, "elementweerstand", 12.1, "vads", 53.0, "aanvalssnelheid", 7.1));
        p.setItemSlot(EquipmentSlot.HEAD, stuk("kristalkroon", Zeldzaamheid.LEGENDARISCH, 23, 80, g, "kracht", 10.0, "kritiek", 2.7, "elementweerstand", 7.8,
                "behendigheid", 10.0));
        p.setItemSlot(EquipmentSlot.FEET, stuk("kristalschoentjes", Zeldzaamheid.ZELDZAAM, 35, 123, g, "wijsheid", 13.0, "vads", 56.0));
        p.setItemSlot(EquipmentSlot.CHEST, stuk("baas_wortelwambuis", Zeldzaamheid.LEGENDARISCH, 19, 1000, Element.NATUUR, "healkracht", 8.0, "kracht", 9.0,
                "kritiek", 3.4, "elementweerstand", 7.5, "aanvalssnelheid", 7.4));
        p.setItemSlot(EquipmentSlot.LEGS, stuk("kristalbeenstukken", Zeldzaamheid.EPISCH, 35, 74, g, "behendigheid", 14.9, "wijsheid", 14.0, "vads", 58.0,
                "kracht", 13.0));
        Sieraden.zet(p, Slot.KETTING, stuk("knuffelketting", Zeldzaamheid.EPISCH, 35, 123, g, "aanvalssnelheid", 6.8, "wijsheid", 14.0, "kritiekschade", 15.9,
                "kritiek", 4.3));
        Sieraden.zet(p, Slot.RING, stuk("kristalring", Zeldzaamheid.EPISCH, 35, 30, g, "loopsnelheid", 4.8, "kritiek", 4.4, "bazenschade", 8.4,
                "kritiekschade", 13.0));
        Sieraden.zet(p, Slot.HANDSCHOENEN, stuk("ijswantjes", Zeldzaamheid.ZELDZAAM, 33, 407, g, "elementschade", 11.9, "kracht", 13.0, "kritiek", 3.6));
        Sieraden.zet(p, Slot.RING2, stuk("roze_ring", Zeldzaamheid.GEWOON, 12, 786, g, "kracht", 5.0));
        Sieraden.zet(p, Slot.OORBEL, stuk("pluisoorbel", Zeldzaamheid.ZELDZAAM, 25, 436, g, "verdediging", 7.0, "wijsheid", 10.0, "aanvalssnelheid", 5.8));
        Sieraden.zet(p, Slot.CAPE, stuk("galmcape", Zeldzaamheid.ZELDZAAM, 35, 113, g, "elementschade", 10.3, "behendigheid", 14.9, "kritiek", 3.7));
        Sieraden.zet(p, Slot.ARMBAND, stuk("rozenarmbandje", Zeldzaamheid.LEGENDARISCH, 19, 568, g, "verdediging", 5.0, "vads", 34.0, "levensroof", 3.8,
                "cooldownreductie", 4.3, "kracht", 8.0));
    }

    static void leeg(ServerPlayer p) {
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        for (EquipmentSlot es : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            p.setItemSlot(es, ItemStack.EMPTY);
        }
        for (Slot s : Slot.SIERADEN) {
            Sieraden.zet(p, s, ItemStack.EMPTY);
        }
        SpelerData d = Spelers.data(p);
        d.boom.clear();
        d.klasse = Klassen.RIDDER;
    }

    /** A creature + its max Vads at the measured level; the entity itself is then made a lv-70 elite so no measured shot defeats it. */
    record Doel(VadsrijkMob m, double vads) {
    }

    static Doel mob(GameTestHelper helper, net.minecraft.world.entity.EntityType<? extends VadsrijkMob> type, int lv, Element el, int afstand) {
        ServerLevel sl = helper.getLevel();
        BlockPos b = helper.absolutePos(new BlockPos(1 + afstand, 1, 1));
        VadsrijkMob m = type.create(sl, EntitySpawnReason.COMMAND);
        m.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0, 0);
        m.stelIn(lv, el, false, Set.of());
        double vads = m.maxVads();
        m.stelIn(70, el, true, Set.of());   // (the player's damage doesn't depend on the creature's level)
        m.setNoAi(true);
        sl.addFreshEntity(m);
        return new Doel(m, vads);
    }

    /** One basic attack of the player's weapon on {@code m} through Schade.doe (crit forced on/off); the mob is healed first (opening shot) or set to 60 %. */
    static double schot(ServerPlayer p, VadsrijkMob m, boolean krit, boolean vol) {
        m.heal(m.maxVads() * 2);
        if (!vol) {
            m.krijgSchade(m.maxVads() * 0.4, null);
        }
        Schade.Soort soort = Aanvalstempo.schadeSoort(Spelers.wapen(p).basisType());
        return Schade.doe(new Schade.Klap(p, m, Spelers.klapSchade(p), Spelers.wapenElement(p), soort, false, "basis", krit)).schade();
    }

    /** Expected damage of a basic attack (crit chance x crit hit, not full HP). */
    static double gemiddeld(ServerPlayer p, VadsrijkMob m) {
        double kans = Math.min(100, Spelers.stats(p).get(Stat.KRITIEK)) / 100.0;
        return (1 - kans) * schot(p, m, false, false) + kans * schot(p, m, true, false);
    }

    static String f(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    static void log(String s) {
        LogUtils.getLogger().info("SCHADE1103 {}", s);
    }

    /** Logs the factor breakdown of a build (for the audit) and returns {avg per shot on the mob, seconds per shot, mob TTK s, boss TTK s}. */
    static double[] meet(GameTestHelper helper, ServerPlayer p, String naam, Doel mobDoel, Doel baasDoel, int mobLv, int baasLv) {
        VadsrijkMob mob = mobDoel.m(), baas = baasDoel.m();
        double mobVads = mobDoel.vads(), baasVads = baasDoel.vads();
        Stats s = Spelers.stats(p);
        Uitrusting w = Spelers.wapen(p);
        Schade.Soort soort = Aanvalstempo.schadeSoort(w.basisType());
        double hoofd = switch (soort) {
            case AFSTAND -> s.get(Stat.BEHENDIGHEID);
            case MAGIE -> s.get(Stat.WIJSHEID);
            default -> s.get(Stat.KRACHT);
        };
        double sec = Aanvalstempo.seconden(p, s.get(Stat.AANVALSSNELHEID));
        double gem = gemiddeld(p, mob);
        double gemBaas = gemiddeld(p, baas);
        double[] r = {gem, sec, mobVads / gem * sec, baasVads / gemBaas * sec, schot(p, mob, false, false)};
        log(naam + ": rating " + f(Rollen.wapenSchade(w)) + " wapenSchade " + f(Spelers.wapenSchade(p)) + " klapSchade " + f(Spelers.klapSchade(p))
                + " hoofd " + f(hoofd) + " (x" + f(Schade.hoofdFactor(hoofd)) + ") krit " + f(s.get(Stat.KRITIEK)) + "% kritiekschade " + f(s.get(Stat.KRITIEKSCHADE))
                + "% (x" + f(Schade.kritiekFactor(s)) + ") elementschade " + f(s.get(Stat.ELEMENTSCHADE)) + "% aanvalssnelheid " + f(s.get(Stat.AANVALSSNELHEID))
                + "% -> " + f(sec) + " s/schot, negeren " + f(s.get(Stat.VERDEDIGING_NEGEREN)) + " bazenschade " + f(s.get(Stat.BAZENSCHADE)));
        log(naam + ": vs " + mob.getType().toShortString() + " lv " + mobLv + " (" + mob.element() + ", " + f(mobVads) + " Vads): normaal "
                + f(schot(p, mob, false, false)) + ", krit " + f(schot(p, mob, true, false)) + ", openingsschot krit " + f(schot(p, mob, true, true)) + ", gemiddeld "
                + f(gem) + " = " + f(mobVads / gem) + " schoten = " + f(r[2]) + " s");
        log(naam + ": vs " + baas.getType().toShortString() + " lv " + baasLv + " (" + f(baasVads) + " Vads): gemiddeld " + f(gemBaas) + " -> "
                + f(r[3]) + " s solo (alleen basisaanvallen)");
        return r;
    }

    /**
     * The audit case: Coecomber's real build. Design (NOTITIES_FIX_GEVECHT / klassen.Balans): basic attacks never one-shot a normal
     * zone mob of the zone's level; ~3-5 basic shots per normal mob; the zone boss ~60-90 s solo with skills (so 45+ s on basics).
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void coecomberBoogTijdTotVerslaan(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Doel mob = mob(helper, Z2WezensRegistry.SNEEUWKNABBELAAR.get(), 28, Element.WATER, 4);
        Doel splinter = mob(helper, Z2WezensRegistry.SPLINTERLING.get(), 28, Element.GEEN, 5);
        Doel baas = mob(helper, Z2BazenRegistry.GALMWOLF.get(), 23, Element.WATER, 7);
        try {
            coecomber(p);
            helper.assertTrue(Spelers.wapen(p) != null && Spelers.klasseLevel(p) == 35 && Spelers.level(p) == 35, "build set up");
            double[] r = meet(helper, p, "Coecomber", mob, baas, 28, 23);
            double[] r2 = meet(helper, p, "Coecomber", splinter, baas, 28, 23);
            double ontwerp = Balans.dps(Klassen.JAGER, Wapens.PLUISBOOG, 35);
            log("ontwerp Balans.dps(jager, pluisboog, 35) = " + f(ontwerp) + " (Gewoon, geen gear, met skills); echt basis-dps " + f(r2[0] / r2[1]));
            // before the schade-audit: ~1225 per shot on the neutral mob (1.1 shots, 1.5 s), ~1837 on the Galmwolf (16 s solo)
            helper.assertTrue(r[4] < mob.vads(), "a non-crit basic shot doesn't one-shot an element-weak zone-2 mob (lv 28): " + f(r[4]) + " vs " + f(mob.vads()));
            helper.assertTrue(splinter.vads() / r2[0] >= 1.8 && r2[2] >= 3.0, "a neutral zone-2 mob (lv 28) takes ~2+ basic shots and 3+ s: "
                    + f(splinter.vads() / r2[0]) + " shots, " + f(r2[2]) + " s");
            helper.assertTrue(r[3] >= 45, "the Galmwolf (lv 23) lasts 45+ s solo on basic shots: " + f(r[3]) + " s");
            helper.succeed();
        } finally {
            leeg(p);
            mob.m().discard();
            splinter.m().discard();
            baas.m().discard();
        }
    }

    /** The same for Luc_Ermeling's mage (basic bolt; the skills are checked by the model tests). */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void lucStafTijdTotVerslaan(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Doel mob = mob(helper, Z2WezensRegistry.SPLINTERLING.get(), 28, Element.GEEN, 4);
        Doel baas = mob(helper, Z2BazenRegistry.GALMWOLF.get(), 23, Element.WATER, 7);
        try {
            luc(p);
            helper.assertTrue(Spelers.wapen(p) != null && Spelers.klasseLevel(p) == 35, "build set up");
            double[] r = meet(helper, p, "Luc", mob, baas, 28, 23);
            helper.assertTrue(mob.vads() / r[0] >= 1.3, "a zone-2 mob (lv 28, versteend: Roze x1.5) is no one-bolt kill on average: " + f(mob.vads() / r[0])
                    + " bolts, " + f(r[2]) + " s");
            helper.assertTrue(r[3] >= 35, "the Galmwolf lasts 35+ s solo on basic bolts (Roze x1.5 on the ontvadst boss): " + f(r[3]) + " s");
            helper.succeed();
        } finally {
            leeg(p);
            mob.m().discard();
            baas.m().discard();
        }
    }

    /**
     * The [gevecht] rendement: stats up to the class base / the main-stat model count in full, the rest at the knob; Kritiek is
     * capped; a player without gear (the balance model of Balans.dps) loses nothing.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void rendementAlleenBovenModel(GameTestHelper helper) {
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.kern.Balans.rendement(50, 126, 0.5) == 50
                && Math.abs(nl.juiced.guhs.feature.vadsrijk.kern.Balans.rendement(226, 126, 0.5) - 176) < 1e-9, "below the base: all; above: half");
        ServerPlayer p = GuhMockPlayer.of(helper);
        try {
            klasse(p, Klassen.JAGER, 35, "j_spec_scherpschutter");
            Stats s = Spelers.stats(p);
            Stats b = Klassen.ofRidder(Klassen.JAGER).basis(35);
            helper.assertTrue(s.get(Stat.BEHENDIGHEID) == b.get(Stat.BEHENDIGHEID) && s.get(Stat.KRITIEKSCHADE) == b.get(Stat.KRITIEKSCHADE)
                    && s.get(Stat.AANVALSSNELHEID) == 0, "no gear, no tree: the class stats untouched");
            coecomber(p);
            s = Spelers.stats(p);
            helper.assertTrue(s.get(Stat.KRITIEK) <= nl.juiced.guhs.feature.vadsrijk.kern.Balans.KRITIEK_MAX_STANDAARD + 1e-9, "Kritiek capped: " + s.get(Stat.KRITIEK));
            // Coecomber's raw totals: Behendigheid 197.2, Kritiekschade 125.3 %, Aanvalssnelheid 47.7 % (SCHADE_1103_AUDIT.md)
            helper.assertTrue(Math.abs(s.get(Stat.BEHENDIGHEID) - (126 + (197.2 - 126) * 0.35)) < 0.5, "Behendigheid above the model at 35 %: " + s.get(Stat.BEHENDIGHEID));
            helper.assertTrue(Math.abs(s.get(Stat.KRITIEKSCHADE) - (10 + 115.31 * 0.4)) < 0.5, "Kritiekschade above the class base at 40 %: " + s.get(Stat.KRITIEKSCHADE));
            helper.assertTrue(Math.abs(s.get(Stat.AANVALSSNELHEID) - 47.7 * 0.4) < 0.5, "Aanvalssnelheid at 40 %: " + s.get(Stat.AANVALSSNELHEID));
            helper.succeed();
        } finally {
            leeg(p);
        }
    }
}
