package nl.juiced.guhs.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.menu.BankGuhMenu;
import nl.juiced.guhs.registry.ModBlocks;

/** 1.2.5: JEI's "+" in the Bank Guh fills the crafting grid from the bank first, then from the player's inventory. */
public class BankJeiGameTests {
    private static final String EMPTY = "empty";

    /** A stick recipe: planks in grid slots 0 and 3 (oak or birch). */
    private static List<List<ItemStack>> stokken() {
        List<List<ItemStack>> k = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            k.add(i == 0 || i == 3 ? List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.BIRCH_PLANKS)) : List.of());
        }
        return k;
    }

    @GuhTest(template = EMPTY, batch = "bankjei")
    public static void bankEerstDanInventaris(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.BANK_GUH.get());
        BankGuhBlockEntity bank = helper.getBlockEntity(pos, BankGuhBlockEntity.class);
        bank.getStorage().insert(new ItemStack(Items.BIRCH_PLANKS, 1));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(Items.BIRCH_PLANKS, 5));
        BankGuhMenu menu = new BankGuhMenu(1, player.getInventory(), bank);
        menu.vulGrid(stokken(), false);
        ItemStack a = menu.getSlot(BankGuhMenu.GRID_START).getItem(), b = menu.getSlot(BankGuhMenu.GRID_START + 3).getItem();
        helper.assertTrue(a.is(Items.BIRCH_PLANKS) && a.getCount() == 1 && b.is(Items.BIRCH_PLANKS) && b.getCount() == 1,
                "one plank in slots 0 and 3: " + a + " " + b);
        helper.assertTrue(bank.getStorage().count(new ItemStack(Items.BIRCH_PLANKS)) == 0, "the bank's plank went first");
        helper.assertTrue(player.getInventory().countItem(Items.BIRCH_PLANKS) == 4, "then one from the inventory");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = "bankjei2")
    public static void bankShiftVultStapels(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.BANK_GUH.get());
        BankGuhBlockEntity bank = helper.getBlockEntity(pos, BankGuhBlockEntity.class);
        bank.getStorage().insert(new ItemStack(Items.OAK_PLANKS, 200));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BankGuhMenu menu = new BankGuhMenu(1, player.getInventory(), bank);
        menu.vulGrid(stokken(), true);
        ItemStack a = menu.getSlot(BankGuhMenu.GRID_START).getItem(), b = menu.getSlot(BankGuhMenu.GRID_START + 3).getItem();
        helper.assertTrue(a.getCount() == 64 && b.getCount() == 64, "shift: full stacks " + a + " " + b);
        helper.assertTrue(bank.getStorage().count(new ItemStack(Items.OAK_PLANKS)) == 72, "72 left in the bank");
        // a second "+" puts the grid back first, then fills again
        menu.vulGrid(stokken(), false);
        helper.assertTrue(menu.getSlot(BankGuhMenu.GRID_START).getItem().getCount() == 1
                && bank.getStorage().count(new ItemStack(Items.OAK_PLANKS)) == 198, "the old grid went back to the bank");
        helper.succeed();
    }
}
