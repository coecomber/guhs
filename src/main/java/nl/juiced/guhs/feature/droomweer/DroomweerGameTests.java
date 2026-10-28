package nl.juiced.guhs.feature.droomweer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.feature.weer.WeerFeature;
import nl.juiced.guhs.feature.weer.WeerUnlocks;
import nl.juiced.guhs.feature.weer.Weertje;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/** 1.8.0 playtest 2: the droomkistje (the droomweer's weerpotje the first time, droomzand after that, once a day). */
public class DroomweerGameTests {
    private static final String BATCH = "droomweer", KAMER = "vadskracht_test_kamer";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        WeerUnlocks.wis(p);
        GuhQuests.saved(p).remove(DroomKistje.DAG);
        GuhQuests.saved(p).remove(DroomKistje.OOIT);
        return p;
    }

    static int tel(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        return n;
    }

    /** First open: the droomweer's potje (then it unlocks like any potje); the same day: "come back tomorrow". */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void kistjeGeeftDroomweer(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.assertFalse(WeerUnlocks.heeft(p, Weertje.DROOM), "the droomweer starts locked");
        helper.assertTrue(DroomKistje.open(p, pos) == DroomKistje.Uitkomst.DROOMWEER, "the first time: the droomweer");
        Item potje = WeerFeature.potje(Weertje.DROOM);
        helper.assertTrue(tel(p.getInventory(), potje) == 1, "the droomweer's weerpotje");
        helper.assertTrue(DroomKistje.open(p, pos) == DroomKistje.Uitkomst.MORGEN, "the same day: come back tomorrow");
        helper.assertTrue(tel(p.getInventory(), potje) == 1, "and nothing more");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(potje));
        WeerFeature.POTJES.get(Weertje.DROOM).get().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(WeerUnlocks.heeft(p, Weertje.DROOM), "the potje unlocks the droomweer");
        WeerUnlocks.wis(p);
        helper.succeed();
    }

    /** Once a day: the next day droomzand and a little something, never the potje again. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void kistjeEenKeerPerDag(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        long vandaag = Band.dag(helper.getLevel().getServer());
        DroomKistje.open(p, pos);
        p.getInventory().clearContent();
        GuhQuests.saved(p).putLong(DroomKistje.DAG, vandaag - 1);           // as if that was yesterday
        helper.assertTrue(DroomKistje.open(p, pos) == DroomKistje.Uitkomst.CADEAUTJE, "the next day: a present");
        helper.assertTrue(tel(p.getInventory(), DroomblokkenFeature.DROOMZAND.get()) >= 2, "droomzand");
        helper.assertTrue(tel(p.getInventory(), WeerFeature.potje(Weertje.DROOM)) == 0, "not the potje again");
        helper.assertTrue(DroomKistje.open(p, pos) == DroomKistje.Uitkomst.MORGEN, "and once a day only");
        WeerUnlocks.wis(p);
        helper.succeed();
    }
}
