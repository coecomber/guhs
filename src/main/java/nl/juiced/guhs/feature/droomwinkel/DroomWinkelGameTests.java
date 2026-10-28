package nl.juiced.guhs.feature.droomwinkel;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/** 1.8.0 expansion: the droomwinkeltje (buying with droomzand, change from a droomzandblok, too dear, once-only offers). */
public class DroomWinkelGameTests {
    private static final String BATCH = "droomwinkel", KAMER = "vadskracht_test_kamer";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        GuhQuests.saved(p).remove(DroomWinkel.GEKOCHT);
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

    /** A maanlampje (3 droomzand) with 5 loose droomzand: bought, 2 left; with nothing left: too dear. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void kopenMetDroomzand(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Item zand = DroomblokkenFeature.DROOMZAND.get();
        DroomWinkel.Aanbod lampje = DroomWinkel.van("maanlampje_maan");
        helper.assertTrue(lampje != null, "the maanlampje is on sale");
        p.getInventory().add(new ItemStack(zand, 5));
        helper.assertTrue(DroomWinkel.koop(p, "maanlampje_maan") == DroomWinkel.Uitkomst.OK, "bought");
        helper.assertTrue(tel(p.getInventory(), zand) == 5 - lampje.prijs(), "paid with droomzand");
        helper.assertTrue(tel(p.getInventory(), DroomWinkel.item("guhs:maanlampje_maan")) == 1, "the maanlampje is in the inventory");
        p.getInventory().clearContent();
        helper.assertTrue(DroomWinkel.koop(p, "maanlampje_maan") == DroomWinkel.Uitkomst.TE_DUUR, "no droomzand: too dear");
        helper.assertTrue(DroomWinkel.koop(p, "bestaat_niet") == DroomWinkel.Uitkomst.ONBEKEND, "an unknown offer");
        helper.succeed();
    }

    /** A droomzandblok counts 9 and gives change; a once-only weerpotje can't be bought twice. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void wisselgeldEnMax(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Item zand = DroomblokkenFeature.DROOMZAND.get();
        p.getInventory().add(new ItemStack(DroomblokkenFeature.DROOMZANDBLOK.get().asItem(), 1));
        helper.assertTrue(DroomWinkel.saldo(p) == 9, "a block counts 9");
        helper.assertTrue(DroomWinkel.koop(p, "doos_knabbels") == DroomWinkel.Uitkomst.OK, "a doos (2) paid with a block");
        helper.assertTrue(tel(p.getInventory(), zand) == 7, "the change: 7 droomzand");
        p.getInventory().clearContent();
        p.getInventory().add(new ItemStack(zand, 64));
        p.getInventory().add(new ItemStack(zand, 64));
        DroomWinkel.Aanbod potje = DroomWinkel.van("weerpotje_bellen");
        helper.assertTrue(potje != null && potje.max() == 1, "the weerpotje is once only");
        helper.assertTrue(DroomWinkel.koop(p, "weerpotje_bellen") == DroomWinkel.Uitkomst.OK, "the first weerpotje");
        helper.assertTrue(DroomWinkel.koop(p, "weerpotje_bellen") == DroomWinkel.Uitkomst.MAX, "not twice");
        helper.assertTrue(DroomWinkel.van("blub_knuffel_wild") != null, "the wild Blub-knuffel is on sale");
        helper.succeed();
    }
}
