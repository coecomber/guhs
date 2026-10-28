package nl.juiced.guhs.feature.kaaskas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.klusjes.KlusjesGameTests;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.Sauzen;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the Kaaskas and the Guh-sorteerband (batch "kaaskas_*"): the kas makes its crops grow on vadskracht and
 * kaassaus (and not without sauce), the chore Kassier picks a ripe kas and plants it again, and the chore Sorteerder takes
 * what waits at the end of a band into the chest that already holds that kind.
 */
public class KaaskasGameTests {
    private static final String TUIN = "klusjes_test_tuin";

    private static KaaskasBlockEntity kas(GameTestHelper helper, BlockPos kern, boolean kracht) {
        helper.setBlock(kern, KaaskasFeature.KAASKAS.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        if (kracht) {
            helper.setBlock(kern.west(), VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 5));
        }
        return (KaaskasBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(kern));
    }

    private static void akker(GameTestHelper helper, BlockPos plant, int leeftijd) {
        helper.setBlock(plant.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        helper.setBlock(plant, ((CropBlock) Blocks.WHEAT).getStateForAge(leeftijd));
    }

    @GuhTest(template = TUIN, batch = "kaaskas_groei", timeoutTicks = 600)
    public static void kaaskasLaatGroeienMetSausEnKracht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos kern = new BlockPos(5, 2, 5);
        KaaskasBlockEntity kas = kas(helper, kern, true);
        BlockPos[] planten = {new BlockPos(6, 2, 6), new BlockPos(7, 2, 6), new BlockPos(6, 2, 7)};
        for (BlockPos p : planten) {
            akker(helper, p, 0);
        }
        helper.assertTrue(kas.gewassen(level).size() == planten.length, "the kas sees its three plants, saw " + kas.gewassen(level).size());
        helper.assertTrue(!kas.duw(level), "no push without kaassaus");
        kas.saus().zet(Sauzen.kaassaus(), 2 * Sauzen.EMMER);
        helper.assertTrue(kas.duw(level), "with kaassaus a plant grows");
        helper.assertTrue(kas.saus().inhoud() == 2 * Sauzen.EMMER - KaaskasGetallen.SAUS_PER_DUW, "a push costs kaassaus");
        helper.succeedWhen(() -> {
            int groter = 0;
            for (BlockPos p : planten) {
                if (helper.getBlockState(p).getValue(CropBlock.AGE) >= 3) {
                    groter++;
                }
            }
            helper.assertTrue(kas.heeftKracht(), "the kas has vadskracht");
            helper.assertTrue(groter == planten.length, "all three plants grew on the kas' own pushes (" + groter + ")");
        });
    }

    @GuhTest(template = TUIN, batch = "kaaskas_kassier", timeoutTicks = 1600)
    public static void kassierPluktDeKas(GameTestHelper helper) {
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = KlusjesGameTests.kist(helper);
        BlockPos kern = new BlockPos(6, 2, 16);
        KaaskasBlockEntity kas = kas(helper, kern, false);
        kas.zetEigenaar(p.getUUID());
        BlockPos[] planten = {new BlockPos(5, 2, 15), new BlockPos(7, 2, 15), new BlockPos(5, 2, 17), new BlockPos(7, 2, 17)};
        for (BlockPos t : planten) {
            akker(helper, t, 7);
        }
        Huisje h = KlusjesGameTests.huisje(helper, p);
        GuhEntity guh = KlusjesGameTests.bewoner(helper, h, p, new BlockPos(12, 2, 14), KassierKlus.ID);
        helper.succeedWhen(() -> {
            for (BlockPos t : planten) {
                helper.assertTrue(helper.getBlockState(t).is(Blocks.WHEAT) && helper.getBlockState(t).getValue(CropBlock.AGE) < 7,
                        "picked and planted again at " + t + KlusjesGameTests.staat(helper, guh));
            }
            helper.assertTrue(KlusjesGameTests.telKist(kist, s -> s.is(Items.WHEAT)) >= planten.length, "the harvest is in the chest");
            KlusjesGameTests.weg(helper, h, p);
        });
    }

    @GuhTest(template = TUIN, batch = "kaaskas_sorteerder", timeoutTicks = 1600)
    public static void sorteerderZetHetInDeGoedeKist(GameTestHelper helper) {
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = KlusjesGameTests.kist(helper);
        BlockPos doelPlek = new BlockPos(14, 2, 15);
        helper.setBlock(doelPlek, Blocks.CHEST);
        ChestBlockEntity doel = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(doelPlek));
        doel.setItem(0, new ItemStack(Items.CLAY_BALL));
        // a band of three pieces running east; the last one is the end
        for (int x = 6; x <= 8; x++) {
            helper.setBlock(new BlockPos(x, 2, 15), KaaskasFeature.SORTEERBAND.get().defaultBlockState().setValue(SorteerbandBlock.FACING, Direction.EAST));
        }
        helper.assertTrue(SorteerbandBlock.isEind(helper.getLevel(), helper.absolutePos(new BlockPos(8, 2, 15)))
                && !SorteerbandBlock.isEind(helper.getLevel(), helper.absolutePos(new BlockPos(7, 2, 15))), "only the last piece is the end");
        BlockPos start = helper.absolutePos(new BlockPos(6, 2, 15));
        ItemEntity klei = new ItemEntity(helper.getLevel(), start.getX() + 0.5, start.getY() + 0.3, start.getZ() + 0.5, new ItemStack(Items.CLAY_BALL, 5));
        helper.getLevel().addFreshEntity(klei);
        Huisje h = KlusjesGameTests.huisje(helper, p);
        GuhEntity guh = KlusjesGameTests.bewoner(helper, h, p, new BlockPos(12, 2, 14), SorteerderKlus.ID);
        helper.succeedWhen(() -> {
            helper.assertTrue(KlusjesGameTests.telKist(doel, s -> s.is(Items.CLAY_BALL)) == 6,
                    "the clay rode to the end and went into the chest with clay" + KlusjesGameTests.staat(helper, guh));
            helper.assertTrue(KlusjesGameTests.telKist(kist, s -> s.is(Items.CLAY_BALL)) == 0, "not into the other chest");
            KlusjesGameTests.weg(helper, h, p);
        });
    }
}
