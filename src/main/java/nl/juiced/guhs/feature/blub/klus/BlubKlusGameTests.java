package nl.juiced.guhs.feature.blub.klus;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubTrekking;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeGoal;
import nl.juiced.guhs.feature.huisje.Klus;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.kamperen.KamperenFeature;
import nl.juiced.guhs.feature.klusjes.KlusjesGameTests;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the Blub's four chores (1.8.0 ronde 3b), one each, from a real Guhhuisje (template klusjes_test_tuin, the
 * helpers of KlusjesGameTests): only a Blub can do them (a guh can't), the Bellenbesproeier's bubbles make a crop grow, the
 * Knabbelbezorger brings a knabbel from the chest to a tame guh, the Slaapliedjeszanger makes the huisje's guh uitgerust in
 * the evening, the Bubbelbadmeester foams up a Blub-bubbelbadje (and a guh in the foam gets fris en blij).
 */
public class BlubKlusGameTests {
    private static final String TUIN = "klusjes_test_tuin";

    static BlubGuhEntity blub(GameTestHelper helper, Huisje h, ServerPlayer owner, BlockPos at, String klus) {
        BlubGuhEntity b = helper.spawn(BlubFeature.BLUBGUH.get(), at);
        b.zetWaarden(new BlubTrekking.Waarden(40, BlubTrekking.Soort.BLAUW, 0, 1f));
        b.tame(owner);
        KlusjesGameTests.alleen(helper, h, b, klus);
        return b;
    }

    static void alleenBlub(GameTestHelper helper, String id, BlubGuhEntity b) {
        Klus k = Klusjes.van(id);
        GuhEntity guh = helper.spawn(nl.juiced.guhs.registry.ModEntities.GUH.get(), new BlockPos(3, 2, 3));
        helper.assertTrue(k != null && k.kan(b) && !k.kan(guh) && "blub".equals(k.doeners()), id + ": only a Blub (\"alleen Blubs\")");
        guh.discard();
    }

    @GuhTest(template = TUIN, batch = "blub_klus_bellen")
    public static void blubKlusBellen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        Huisje h = KlusjesGameTests.huisje(helper, p);
        BlockPos gewas = new BlockPos(15, 2, 15);
        helper.setBlock(gewas.below(), Blocks.FARMLAND);
        helper.setBlock(gewas, Blocks.WHEAT);
        BlubGuhEntity b = blub(helper, h, p, new BlockPos(13, 2, 13), BellenKlus.ID);
        alleenBlub(helper, BellenKlus.ID, b);
        KlusStand stand = Klusjes.van(BellenKlus.ID).stand(level, h);
        helper.assertTrue(stand.staat() == KlusStand.Staat.JA, "a growing crop to sprinkle: " + stand);
        BlockPos abs = helper.absolutePos(gewas);
        helper.assertTrue(BellenKlus.besproei(level, abs, 100) && ((CropBlock) Blocks.WHEAT).getAge(level.getBlockState(abs)) >= 1,
                "the bubbles make it grow a little");
        KlusjesGameTests.weg(helper, h, p);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = "blub_klus_bezorgen", timeoutTicks = 1200)
    public static void blubKlusBezorgen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        Huisje h = KlusjesGameTests.huisje(helper, p);
        ChestBlockEntity kist = KlusjesGameTests.kist(helper);
        kist.setItem(0, new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
        GuhEntity guh = helper.spawn(nl.juiced.guhs.registry.ModEntities.GUH.get(), new BlockPos(16, 2, 15));
        guh.tame(p);
        guh.setOrderedToSit(true);
        BlubGuhEntity b = blub(helper, h, p, new BlockPos(13, 2, 13), BezorgKlus.ID);
        alleenBlub(helper, BezorgKlus.ID, b);
        helper.assertTrue(Klusjes.van(BezorgKlus.ID).stand(level, h).staat() == KlusStand.Staat.JA, "knabbels in the chest and a tame guh");
        helper.succeedWhen(() -> {
            helper.assertTrue(guh.getPersistentData().getLongOr(BezorgKlus.GEVOERD_TOT, 0L) > level.getGameTime(), "the guh got a knabbel");
            int over = 0;
            for (int i = 0; i < kist.getContainerSize(); i++) {
                over += kist.getItem(i).is(ModItems.KAAS_KNABBELS.get()) ? kist.getItem(i).getCount() : 0;
            }
            helper.assertTrue(over == 4, "one knabbel from the chest (the rest back): " + over);
            KlusjesGameTests.weg(helper, h, p);
        });
    }

    @GuhTest(template = TUIN, batch = "blub_klus_zingen", timeoutTicks = 1000)
    public static void blubKlusZingen(GameTestHelper helper) {
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        Huisje h = KlusjesGameTests.huisje(helper, p);
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.AVOND);
        GuhEntity guh = KlusjesGameTests.bewoner(helper, h, p, new BlockPos(8, 2, 13), ZingKlus.ID);   // (a guh can't: it just listens)
        BlubGuhEntity b = blub(helper, h, p, new BlockPos(13, 2, 13), ZingKlus.ID);
        alleenBlub(helper, ZingKlus.ID, b);
        helper.assertTrue(Klusjes.van(ZingKlus.ID).stand(helper.getLevel(), h).staat() == KlusStand.Staat.JA, "evening: time to sing");
        helper.succeedWhen(() -> {
            helper.assertTrue(guh.hasEffect(KamperenFeature.UITGERUST), "the huisje's guh is uitgerust");
            helper.assertTrue(b.getPersistentData().getLongOr(ZingKlus.GEZONGEN_DAG, -1L) >= 0, "sung once today");
            KlusjesGameTests.weg(helper, h, p);
        });
    }

    @GuhTest(template = TUIN, batch = "blub_klus_bad", timeoutTicks = 1000)
    public static void blubKlusBad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        Huisje h = KlusjesGameTests.huisje(helper, p);
        BlockPos badje = new BlockPos(15, 2, 15);
        helper.setBlock(badje, BlubFeature.BADJE.get());
        BlubGuhEntity b = blub(helper, h, p, new BlockPos(13, 2, 13), BadKlus.ID);
        alleenBlub(helper, BadKlus.ID, b);
        helper.assertTrue(Klusjes.van(BadKlus.ID).stand(level, h).staat() == KlusStand.Staat.JA, "a bath without foam");
        helper.succeedWhen(() -> {
            helper.assertTrue(level.getBlockState(helper.absolutePos(badje)).getValue(BlubBadjeBlock.SCHUIM), "the Blub foamed up the bath");
            GuhEntity guh = helper.spawn(nl.juiced.guhs.registry.ModEntities.GUH.get(), badje);
            guh.tame(p);
            BlubBadjeBlock.badderen(level, helper.absolutePos(badje), guh);
            helper.assertTrue(guh.hasEffect(BlubFeature.FRIS_EN_BLIJ), "a guh in the foam: fris en blij");
            KlusjesGameTests.weg(helper, h, p);
        });
    }
}
