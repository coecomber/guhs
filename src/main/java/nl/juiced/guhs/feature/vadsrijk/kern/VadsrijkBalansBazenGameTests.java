package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Map;
import java.util.Set;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import nl.juiced.guhs.feature.vadsrijk.economie.SterRegels;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotBaas;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotBazen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.MobBalans;
import nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalans;
import nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.hart.BaronBleu;
import nl.juiced.guhs.feature.vadsrijk.zone4.bazen.Vliegervanger;
import nl.juiced.guhs.feature.vadsrijk.zone4.bazen.Z4BazenRegistry;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Balans-bazen: {@code [bazen] leven_vanaf_zone4} (x5 max Vads for every boss of zone 4+, not zones 1-3), {@code [bazen]
 * enrage_vanaf_zone4} (their enrage x2), {@code [gevecht] ster_schade} (+10 stars = x1.25 instead of x1.55), the config versie 7 and
 * a saved boss keeping its HP fraction. Run: {@code runGameTestServer -Pgt=VadsrijkBalansBazen}.
 */
public class VadsrijkBalansBazenGameTests {
    private static final String BATCH = "vadsrijk_balans_bazen";

    private static <T extends VadsrijkBaas> T baas(ServerLevel sl, EntityType<T> type, int lv) {
        T b = type.create(sl, EntitySpawnReason.COMMAND);
        b.stelIn(lv, null, false, Set.of());
        return b;
    }

    /** The zone-4 cave boss gets exactly 5x the Vads of the zone-3 one at the same level (same HP formula); the knob is live. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zone4BaasVijfKeerLeven(GameTestHelper helper) {
        KlasseBalans.standaardAlles();
        ServerLevel sl = helper.getLevel();
        try {
            helper.assertTrue(Math.abs(Balans.bazenLevenZone(4) - 5.0) < 1e-9 && Balans.bazenLevenZone(3) == 1 && Balans.bazenLevenZone(0) == 1,
                    "default: x5 from zone 4, x1 below");
            GrotBaas koraal = baas(sl, GrotBazen.KORAALGROT.get(), 50), wortel = baas(sl, GrotBazen.WORTELGROT.get(), 50);
            helper.assertTrue(koraal.baasZone() == 4 && wortel.baasZone() == 3, "cave zones: " + koraal.baasZone() + " / " + wortel.baasZone());
            helper.assertTrue(Math.abs(koraal.maxVads() / wortel.maxVads() - 5.0) < 1e-4, "zone 4 = 5x zone 3 at lv 50: " + koraal.maxVads() + " vs " + wortel.maxVads());
            helper.assertTrue(Math.abs(koraal.maxVadsZonderZone() - wortel.maxVads()) < 0.5 && wortel.maxVadsZonderZone() == wortel.maxVads(),
                    "maxVadsZonderZone drops only the zone factor");
            // a zone-4 dungeon boss: its own formula x the boss factor x 5
            Vliegervanger v = baas(sl, Z4BazenRegistry.VLIEGERVANGER.get(), 56);
            double verwacht = (7600 + 280 * 56) * MobBalans.baas(56) * 5;
            helper.assertTrue(v.baasZone() == 4 && Math.abs(v.maxVads() - verwacht) < 1, "Kootje lv 56: " + verwacht + ", got " + v.maxVads());
            v.schaal(2);
            helper.assertTrue(Math.abs(v.maxVads() - verwacht * 1.75) < 1, "2 players: +75 % on top: " + v.maxVads());
            // a zone-3 boss is untouched
            BaronBleu bleu = baas(sl, Z3BazenRegistry.BLEU.get(), 50);
            helper.assertTrue(bleu.baasZone() == 0 && bleu.maxVadsZonderZone() == bleu.maxVads(), "zone 3 bosses: no zone factor");
            // live knob
            KlasseBalans.zet(KlasseBalans.BAZEN, "leven_vanaf_zone4", 1.0, false);
            koraal.stelIn(50, null, false, Set.of());
            helper.assertTrue(Math.abs(koraal.maxVads() - wortel.maxVads()) < 0.5, "leven_vanaf_zone4 1.0: as before: " + koraal.maxVads());
            koraal.discard();
            wortel.discard();
            v.discard();
            bleu.discard();
            helper.succeed();
        } finally {
            KlasseBalans.standaardAlles();
        }
    }

    /** Zone-4+ bosses: the enrage (Z3Baas) and the soft woede (BaasVfx) x2; zones 1-3 unchanged. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void enrageLaterVanafZone4(GameTestHelper helper) {
        KlasseBalans.standaardAlles();
        ServerLevel sl = helper.getLevel();
        Vliegervanger v = baas(sl, Z4BazenRegistry.VLIEGERVANGER.get(), 56);
        BaronBleu bleu = baas(sl, Z3BazenRegistry.BLEU.get(), 50);
        GrotBaas koraal = baas(sl, GrotBazen.KORAALGROT.get(), 56), wortel = baas(sl, GrotBazen.WORTELGROT.get(), 48);
        helper.assertTrue(v.enrageTijd() == 20 * 60 * 8 * 2, "Kootje: 8 min x2 = 16 min, got " + v.enrageTijd() / 20 + " s");
        helper.assertTrue(bleu.enrageTijd() == 20 * 60 * 7, "zone 3: 7 min as before, got " + bleu.enrageTijd() / 20 + " s");
        helper.assertTrue(BaasVfx.woedeNa(koraal) == BaasVfx.WOEDE_NA * 2L && BaasVfx.woedeNa(wortel) == BaasVfx.WOEDE_NA, "woede x2 from zone 4");
        v.discard();
        bleu.discard();
        koraal.discard();
        wortel.discard();
        helper.succeed();
    }

    /** The star curve: +10 = x1.25 (was x1.55), +5 = x1.11, the shape kept; ster_schade 0.55 = the old curve. Weapons follow. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void sterCurveMilder(GameTestHelper helper) {
        KlasseBalans.standaardAlles();
        try {
            helper.assertTrue(SterRegels.schade(0) == 1 && Math.abs(SterRegels.schade(10) - 1.25) < 1e-9, "+10 = x1.25: " + SterRegels.schade(10));
            helper.assertTrue(Math.abs(SterRegels.schade(5) - (1 + 0.25 * 0.25 / 0.55)) < 1e-9, "+5 = x1.114: " + SterRegels.schade(5));
            for (int n = 1; n <= SterRegels.MAX; n++) {
                helper.assertTrue(SterRegels.schade(n) > SterRegels.schade(n - 1), "every star still adds: " + n);
            }
            Uitrusting u = Rollen.rol(BasisTypes.van("z3baas_hartwortel"), 49, Zeldzaamheid.LEGENDARISCH, RandomSource.create(7));
            helper.assertTrue(Math.abs(Rollen.wapenSchade(u.metSterren(10)) / Rollen.wapenSchade(u) - 1.25) < 1e-9, "a +10 staff hits x1.25");
            KlasseBalans.zet(KlasseBalans.GEVECHT, "ster_schade", 0.55, false);
            helper.assertTrue(Math.abs(SterRegels.schade(10) - 1.55) < 1e-9 && Math.abs(SterRegels.schade(5) - 1.25) < 1e-9, "0.55 = the old x1.55 / x1.25");
            helper.succeed();
        } finally {
            KlasseBalans.standaardAlles();
        }
    }

    /** Config versie 6 (live) -> 7: nothing moves, the new keys get written; versie 7 is left alone. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void configVersie7(GameTestHelper helper) {
        try {
            String r = KlasseBalans.testMigreer(Map.of("schade", 2.028, "leven", 1.75, "versie", 6.0));
            helper.assertTrue(r != null && r.contains("leven_vanaf_zone4") && KlasseBalans.get(KlasseBalans.MOBS, "versie") == 7, "6 -> 7: " + r);
            helper.assertTrue(KlasseBalans.toon().stream().anyMatch(t -> t.equals("[bazen] leven=3.74 schade=1.40 leven_vanaf_zone4=5.00 enrage_vanaf_zone4=2.00"))
                    && KlasseBalans.toon().stream().anyMatch(t -> t.startsWith("[gevecht] ") && t.endsWith("ster_schade=0.25")), "balans toon: " + KlasseBalans.toon());
            r = KlasseBalans.testMigreer(Map.of("schade", 2.028, "leven", 1.75, "versie", 7.0));
            helper.assertTrue(r == null, "versie 7: nothing to do: " + r);
            helper.succeed();
        } finally {
            KlasseBalans.standaardAlles();
        }
    }

    /** A saved boss keeps its HP fraction when its max Vads changed; a save from before vr_max starts full (phase 1). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void opslagHoudtVerhouding(GameTestHelper helper) {
        KlasseBalans.standaardAlles();
        ServerLevel sl = helper.getLevel();
        try {
            GrotBaas b = baas(sl, GrotBazen.KORAALGROT.get(), 56);
            b.krijgSchade(b.maxVads() * 0.3, null);
            helper.assertTrue(Math.abs(b.vads() / b.maxVads() - 0.7) < 1e-3, "at 70 %");
            TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, sl.registryAccess());
            b.saveWithoutId(out);
            CompoundTag tag = out.buildResult();
            KlasseBalans.zet(KlasseBalans.BAZEN, "leven_vanaf_zone4", 2.5, false);
            GrotBaas kopie = GrotBazen.KORAALGROT.get().create(sl, EntitySpawnReason.LOAD);
            kopie.load(TagValueInput.create(ProblemReporter.DISCARDING, sl.registryAccess(), tag));
            helper.assertTrue(Math.abs(kopie.maxVads() - b.maxVads() / 2) < 1 && Math.abs(kopie.vads() / kopie.maxVads() - 0.7) < 1e-3,
                    "half the max, still 70 %: " + kopie.vads() + " / " + kopie.maxVads());
            CompoundTag oud = tag.copy();
            oud.remove("vr_max");
            GrotBaas oudeKopie = GrotBazen.KORAALGROT.get().create(sl, EntitySpawnReason.LOAD);
            oudeKopie.load(TagValueInput.create(ProblemReporter.DISCARDING, sl.registryAccess(), oud));
            helper.assertTrue(oudeKopie.vads() == oudeKopie.maxVads() && oudeKopie.fase() == 1, "an old save starts full: " + oudeKopie.vads() + " / " + oudeKopie.maxVads());
            b.discard();
            kopie.discard();
            oudeKopie.discard();
            helper.succeed();
        } finally {
            KlasseBalans.standaardAlles();
        }
    }
}
