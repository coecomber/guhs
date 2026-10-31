package nl.juiced.guhs.feature.frituur;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.block.entity.FryingPanBlockEntity;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.techsaus.Frituur;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;

/** Game tests of the frituur line (batch "frituur"): the pan fries on, recipes must be learned, De Vetste Hap and its party. */
public class FrituurGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "frituur";
    private static final BlockPos PAN = new BlockPos(2, 2, 2);

    private static FryingPanBlockEntity pan(GameTestHelper helper, int vet) {
        helper.setBlock(PAN, ModBlocks.FRYING_PAN.get());
        FryingPanBlockEntity pan = (FryingPanBlockEntity) helper.getBlockEntity(PAN, FryingPanBlockEntity.class);
        pan.setCharges(vet);
        return pan;
    }

    private static void klik(GameTestHelper helper, ServerPlayer p, ItemStack hand) {
        BlockPos abs = helper.absolutePos(PAN);
        BlockState state = helper.getLevel().getBlockState(abs);
        p.setItemInHand(InteractionHand.MAIN_HAND, hand);
        state.useItemOn(hand, helper.getLevel(), p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false));
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void panFrituurtDoor(GameTestHelper helper) {
        FryingPanBlockEntity pan = pan(helper, 64);
        ServerPlayer p = GuhMockPlayer.of(helper);
        klik(helper, p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 1));
        helper.assertTrue(p.getInventory().countItem(ModItems.GEFRITUURDE_KAASKNABBELS.get()) == 1, "knabbels still become fried knabbels");
        pan.setCharges(64);
        klik(helper, p, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 3));
        helper.assertTrue(p.getInventory().countItem(FrituurFeature.DUBBEL.get()) == 3, "fried becomes double fried");
        klik(helper, p, new ItemStack(FrituurFeature.DUBBEL.get(), 2));
        helper.assertTrue(p.getInventory().countItem(FrituurFeature.DRIEDUBBEL.get()) == 2, "double becomes triple fried");
        klik(helper, p, new ItemStack(Items.POTATO, 2));
        helper.assertTrue(p.getInventory().countItem(FrituurFeature.PATATJE_KAASSAUS.get()) == 2, "a potato becomes patat");
        helper.assertTrue(pan.getCharges() == 64 - 7, "each one takes one fat: " + pan.getCharges());
        helper.assertTrue(Frituur.resultaat(new ItemStack(Items.POTATO)).is(FrituurFeature.PATATJE_KAASSAUS.get())
                && Frituur.resultaat(new ItemStack(Items.PORKCHOP)).isEmpty(), "the machine fries only what needs no recipe");
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void receptEerstLeren(GameTestHelper helper) {
        FryingPanBlockEntity pan = pan(helper, 64);
        ServerPlayer p = GuhMockPlayer.of(helper);
        ItemStack vlees = new ItemStack(Items.PORKCHOP, 2);
        klik(helper, p, vlees);
        helper.assertTrue(vlees.getCount() == 2 && pan.getCharges() == 64 && !SnackRecepten.kent(p, "frikandel_guhspeciaal"),
                "an unknown recipe does nothing");
        SnackRecepten.leer(p, "frikandel_guhspeciaal");
        SnackRecepten.leer(p, "frikandel_guhspeciaal");
        SnackRecepten.leer(p, "bestaat_niet");
        helper.assertTrue(SnackRecepten.kent(p, "frikandel_guhspeciaal") && SnackRecepten.bekend(p).size() == 1
                && KnusVoortgang.teller(p, SnackRecepten.TELLER_KENNIS) == 1, "learned once, nonsense ignored");
        klik(helper, p, new ItemStack(Items.PORKCHOP, 2));
        helper.assertTrue(p.getInventory().countItem(FrituurFeature.FRIKANDEL.get()) == 2, "now it fries");
        helper.assertTrue(SnackRecepten.ALLE.size() == 8, "eight recipes to learn: " + SnackRecepten.ALLE);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void vetsteHapEnFeestje(GameTestHelper helper) {
        FryingPanBlockEntity pan = pan(helper, 64);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        SnackRecepten.leer(p, "vetste_hap");
        klik(helper, p, new ItemStack(FrituurFeature.DRIEDUBBEL.get(), 16));
        helper.assertTrue(p.getInventory().countItem(FrituurFeature.VETSTE_HAP.get()) == 0, "not without croquettes and frikandels");
        p.getInventory().add(new ItemStack(FrituurFeature.KAASKROKET.get(), 5));
        p.getInventory().add(new ItemStack(FrituurFeature.FRIKANDEL.get(), 4));
        ItemStack hand = new ItemStack(FrituurFeature.DRIEDUBBEL.get(), 17);
        klik(helper, p, hand);
        helper.assertTrue(p.getInventory().countItem(FrituurFeature.VETSTE_HAP.get()) == 1 && hand.getCount() == 1
                && p.getInventory().countItem(FrituurFeature.KAASKROKET.get()) == 1 && p.getInventory().countItem(FrituurFeature.FRIKANDEL.get()) == 0
                && pan.getCharges() == 64 - FrituurRecepten.HAP_VET, "De Vetste Hap takes everything once");
        ItemStack hap = new ItemStack(FrituurFeature.VETSTE_HAP.get());
        hap.getItem().finishUsingItem(hap, helper.getLevel(), p);
        helper.assertTrue(KnusVoortgang.teller(p, SnackRecepten.TELLER_HAP) == 1, "eaten and counted");
        helper.succeed();
    }
}
