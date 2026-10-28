package nl.juiced.guhs.feature.rozegraan;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandEvents;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.klusjes.FarmenKlus;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the roze graan (batch "rozegraan"): the pink grass gives roze zaadjes (never wheat seeds), the crop grows in
 * four stages and a resident's harvest plants it again, and guhs and Blubs take the roze broodje like a kaasknabbel.
 */
public class RozegraanGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "rozegraan";

    @GuhTest(template = TUIN, batch = BATCH)
    public static void zaadjesUitRozeGras(GameTestHelper helper) {
        BlockState gras = BuiltInRegistries.BLOCK.getValue(Identifier.parse("guhs:weide_roze_gras")).defaultBlockState();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        int zaadjes = 0;
        for (int i = 0; i < 300; i++) {
            List<ItemStack> drops = Block.getDrops(gras, helper.getLevel(), pos, null);
            for (ItemStack d : drops) {
                helper.assertTrue(!d.is(Items.WHEAT_SEEDS), "the pink grass never drops wheat seeds");
                if (d.is(RozegraanFeature.ZAADJES.get())) {
                    zaadjes++;
                }
            }
        }
        helper.assertTrue(zaadjes > 0, "the pink grass drops roze zaadjes (" + zaadjes + " in 300)");
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void graanGroeitEnWordtGeoogst(GameTestHelper helper) {
        BlockPos akker = new BlockPos(2, 1, 2), plant = akker.above();
        helper.setBlock(akker, Blocks.FARMLAND);
        RozeGraanBlock graan = RozegraanFeature.GRAAN_PLANT.get();
        helper.setBlock(plant, graan.getStateForAge(0));
        helper.assertTrue(graan.getMaxAge() == 3 && !graan.isMaxAge(helper.getBlockState(plant)), "four stages, starts small");
        BlockPos abs = helper.absolutePos(plant);
        for (int i = 0; i < 10 && !graan.isMaxAge(helper.getBlockState(plant)); i++) {
            graan.growCrops(helper.getLevel(), abs, helper.getBlockState(plant));
        }
        helper.assertTrue(graan.isMaxAge(helper.getBlockState(plant)), "it ripens");
        List<ItemStack> oogst = FarmenKlus.oogst(helper.getLevel(), abs, null);
        helper.assertTrue(oogst.stream().anyMatch(s -> s.is(RozegraanFeature.GRAAN.get())), "the harvest has roze graan");
        helper.assertTrue(helper.getBlockState(plant).is(graan) && graan.getAge(helper.getBlockState(plant)) == 0, "and it is planted again");
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void guhsEnBlubsLustenHetBroodje(GameTestHelper helper) {
        ItemStack broodje = new ItemStack(RozegraanFeature.BROODJE.get());
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 2, 2));
        BlubGuhEntity blub = helper.spawn(BlubFeature.BLUBGUH.get(), new BlockPos(4, 2, 4));
        helper.assertTrue(guh.isFood(broodje), "a guh takes the roze broodje like a kaasknabbel");
        helper.assertTrue(blub.isVoer(broodje), "so does a Blub");
        helper.assertTrue(BandEvents.isSnack(new ItemStack(RozegraanFeature.KOEKJE.get())), "the knabbelkoekje is a snack");
        helper.assertTrue(broodje.has(net.minecraft.core.component.DataComponents.FOOD), "and you can eat it yourself");
        guh.discard();
        blub.discard();
        helper.succeed();
    }
}
