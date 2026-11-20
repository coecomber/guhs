package nl.juiced.guhs.feature.slijmhuisje;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.entity.GuhSlimeEntity;
import nl.juiced.guhs.feature.techmachine.PlantagebakDeelBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Slijmhuisje: the 3 x 3 x 3 house, a slime moving in through the item, a slimeball after 45 real seconds on
 * vadskracht, the bowl stopping at 16, and the item keeping the slime's size and name both ways.
 */
public class SlijmhuisjeGameTests {
    private static final String BATCH = "slijmhuisje", TUIN = "techklus_test_tuin";
    private static final BlockPos KERN = new BlockPos(6, 2, 4);   // (the tuin's grass is at y 1)

    private static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    /** The house with its door to the north and a test source in front of it. */
    private static SlijmhuisjeBlockEntity huisje(GameTestHelper helper) {
        helper.setBlock(KERN, SlijmhuisjeFeature.SLIJMHUISJE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        helper.setBlock(KERN.north(), VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 5));
        return (SlijmhuisjeBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(KERN));
    }

    /** A guh slime of this size as the item (the slime itself is gone again). */
    private static ItemStack slijmItem(GameTestHelper helper, int grootte, String naam) {
        GuhSlimeEntity slijm = ModEntities.GUH_SLIME.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(slijm != null, "a guh slime");
        slijm.setSize(grootte, true);
        slijm.setCustomName(Component.literal(naam));
        ItemStack stack = OpgepakteGuhSlimeItem.van(slijm);
        slijm.discard();
        return stack;
    }

    /** Place it, a size-2 slime moves in, and after 45 seconds on vadskracht one slimeball lies in the bowl. */
    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = SlijmhuisjeBlockEntity.TIJD + 200)
    public static void slijmhuisjeMaaktSlijmbal(GameTestHelper helper) {
        SlijmhuisjeBlockEntity huisje = huisje(helper);
        // the house: 26 other blocks, the base pieces know which piece they are
        int delen = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = 0; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockState s = helper.getBlockState(KERN.offset(dx, dy, dz));
                    helper.assertTrue(s.is(SlijmhuisjeFeature.SLIJMHUISJE_DEEL.get()), "a piece of the house at " + dx + "," + dy + "," + dz + ": " + s);
                    gelijk(helper, PlantagebakDeelBlock.Stuk.van(dx, dz - 1), s.getValue(SlijmhuisjeDeelBlock.STUK), "which piece");
                    delen++;
                }
            }
        }
        gelijk(helper, 26, delen, "26 other blocks");
        gelijk(helper, helper.absolutePos(KERN.south().above()), huisje.kussen(), "the cushion is the middle, on top of the base");
        // moving in
        ItemStack stack = slijmItem(helper, 2, "Plakkie");
        helper.assertTrue(huisje.trekIn(stack), "the slime moves in");
        helper.assertTrue(!huisje.trekIn(slijmItem(helper, 1, "Tweede")), "one slime per house");
        helper.assertTrue(!huisje.trekIn(new ItemStack(ModItems.GUH_SLIMEBALL.get())), "a slimeball is no slime");
        gelijk(helper, 2, huisje.grootte(), "it knows the size");
        gelijk(helper, 0, huisje.schaal(), "the bowl is empty");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(huisje.heeftKracht(), "the source powers it");
            helper.assertBlockProperty(KERN, MachineBlock.SNOET, Snoet.WERKT);
            helper.assertTrue(huisje.bezig() && huisje.voortgang() > 0, "it works");
            gelijk(helper, 0, huisje.schaal(), "not yet 45 seconds");
        });
        helper.runAfterDelay(SlijmhuisjeBlockEntity.TIJD + 40, () -> {
            gelijk(helper, 1, huisje.schaal(), "one slimeball after 45 seconds");
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(KERN)) == huisje, "still the same house");
            ItemStack ballen = huisje.leegSchaal();
            helper.assertTrue(ballen.is(ModItems.GUH_SLIMEBALL.get()) && ballen.getCount() == 1, "the bowl gives a guh slimeball: " + ballen);
            gelijk(helper, 0, huisje.schaal(), "and is empty again");
            helper.succeed();
        });
    }

    /** The bowl stops at 16 (the face is content), and the slime comes out with its size and name. */
    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 200)
    public static void slijmhuisjeSchaalVolEnUitpakken(GameTestHelper helper) {
        // the item both ways: a slime of every size comes back with that size and its name
        for (int grootte : new int[] {1, 2, 4}) {
            ItemStack stack = slijmItem(helper, grootte, "Blubbie " + grootte);
            gelijk(helper, grootte, OpgepakteGuhSlimeItem.grootte(stack), "the item knows the size");
            GuhSlimeEntity terug = OpgepakteGuhSlimeItem.naar(stack, helper.getLevel());
            helper.assertTrue(terug != null, "a slime comes out of the item");
            gelijk(helper, grootte, terug.getSize(), "same size back");
            gelijk(helper, "Blubbie " + grootte, terug.getCustomName() == null ? "" : terug.getCustomName().getString(), "same name back");
        }
        SlijmhuisjeBlockEntity huisje = huisje(helper);
        helper.assertTrue(huisje.trekIn(slijmItem(helper, 4, "Dikkie")), "a big one moves in");
        huisje.vakken().set(0, ItemResource.of(ModItems.GUH_SLIMEBALL.get()), SlijmhuisjeBlockEntity.SCHAAL_MAX - 1);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(huisje.heeftKracht(), "the source powers it");
            huisje.voortgang = SlijmhuisjeBlockEntity.TIJD - 3;          // (this test does not wait 45 seconds)
            helper.runAfterDelay(10, () -> {
                gelijk(helper, SlijmhuisjeBlockEntity.SCHAAL_MAX, huisje.schaal(), "the bowl is full");
                helper.assertBlockProperty(KERN, MachineBlock.SNOET, Snoet.VOL);
                huisje.voortgang = SlijmhuisjeBlockEntity.TIJD - 3;
                helper.runAfterDelay(10, () -> {
                    gelijk(helper, SlijmhuisjeBlockEntity.SCHAAL_MAX, huisje.schaal(), "a full bowl stays at 16");
                    gelijk(helper, SlijmhuisjeBlockEntity.TIJD - 3, huisje.voortgang(), "production paused");
                    ItemStack uit = huisje.trekUit();
                    gelijk(helper, 4, OpgepakteGuhSlimeItem.grootte(uit), "out it comes, still size 4");
                    gelijk(helper, "Dikkie", uit.getHoverName().getString(), "with its name");
                    helper.assertTrue(!huisje.heeftSlijm(), "the house is empty");
                    gelijk(helper, SlijmhuisjeBlockEntity.SCHAAL_MAX, huisje.leegSchaal().getCount(), "the bowl gives all 16");
                    helper.succeed();
                });
            });
        });
    }
}
