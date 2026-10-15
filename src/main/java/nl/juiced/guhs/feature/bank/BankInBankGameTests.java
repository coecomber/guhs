package nl.juiced.guhs.feature.bank;

import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.menu.BankGuhMenu;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModDataComponents;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.BankContents;
import nl.juiced.guhs.storage.Nbt;

/**
 * 1.4.2 (fixes, bug 4): a Bank Guh ITEM with something in its stomach, stored in another Bank Guh, can be taken out again.
 * The screen sends the item back as it got it (a copy that went over the network); before 1.4.2 that copy never matched
 * the stored one (BankContents.Entry compared its ItemStack by identity), so the stomach said "none of those".
 */
public class BankInBankGameTests {
    private static final String KAMER = "bank_test_kamer";

    /** What the client sends back: the stack after a trip over the network (a new object, equal content). */
    private static ItemStack overHetNet(ServerLevel level, ItemStack stack) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }

    @GuhTest(template = KAMER, batch = "bank_in_bank")
    public static void bankInBankEruit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.BANK_GUH.get());
        BankGuhBlockEntity bank = (BankGuhBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(2, 2, 2)));
        // a picked-up Bank Guh with a stomach of its own (and an empty one, like a fresh one from the uitvinder-guh)
        ItemStack vol = new ItemStack(ModItems.BANK_GUH.get());
        vol.set(ModDataComponents.BANK_CONTENTS.get(), new BankContents(List.of(new BankContents.Entry(new ItemStack(Items.COBBLESTONE), 200),
                new BankContents.Entry(new ItemStack(Items.DIAMOND), 3))));
        ItemStack leeg = new ItemStack(ModItems.BANK_GUH.get());
        helper.assertTrue(bank.getStorage().insert(vol.copy()).isEmpty() && bank.getStorage().insert(leeg.copy()).isEmpty(), "both go in");
        helper.assertTrue(bank.getStorage().count(overHetNet(level, vol)) == 1, "the stomach knows the full Bank Guh in a copy that went over the network");
        // the same item twice is one kind (it did not merge before either)
        ItemStack tweede = overHetNet(level, vol);
        helper.assertTrue(bank.getStorage().insert(tweede).isEmpty() && bank.getStorage().count(vol) == 2, "a second one stacks on the first");
        helper.assertTrue(bank.getStorage().snapshot().entries().size() == 2, "two kinds: the full and the empty Bank Guh");

        ServerPlayer speler = GuhMockPlayer.of(helper);
        speler.setGameMode(GameType.SURVIVAL);
        speler.getInventory().clearContent();
        BankGuhMenu menu = new BankGuhMenu(1, speler.getInventory(), bank);
        // a click on the full Bank Guh in the grid: on the cursor (a Bank Guh does not stack: one at a time)
        menu.handleAction(BankGuhMenu.Action.TAKE, overHetNet(level, bank.getStorage().snapshot().entries().get(0).item()), 0, false);
        ItemStack hand = menu.getCarried();
        helper.assertTrue(hand.is(ModItems.BANK_GUH.get()) && hand.getCount() == 1, "a full Bank Guh is on the cursor: " + hand);
        BankContents mee = hand.get(ModDataComponents.BANK_CONTENTS.get());
        helper.assertTrue(mee != null && mee.totalItems() == 203, "with its stomach: " + mee);
        helper.assertTrue(bank.getStorage().count(vol) == 1, "one left in the bank");
        menu.setCarried(ItemStack.EMPTY);
        // shift-clicks: the other full one and the empty one into the inventory
        menu.handleAction(BankGuhMenu.Action.TAKE, overHetNet(level, vol), 0, true);
        menu.handleAction(BankGuhMenu.Action.TAKE, overHetNet(level, leeg), 0, true);
        helper.assertTrue(speler.getInventory().countItem(ModItems.BANK_GUH.get()) == 2 && bank.getStorage().snapshot().entries().isEmpty(),
                "both went to the inventory, the bank is empty");
        // saved and loaded (the codec makes new objects too): still found
        bank.getStorage().insert(hand);
        BankGuhBlockEntity kopie = new BankGuhBlockEntity(bank.getBlockPos(), bank.getBlockState());
        kopie.loadWithComponents(Nbt.input(level.registryAccess(), bank.saveWithoutMetadata(level.registryAccess())));
        helper.assertTrue(kopie.getStorage().count(overHetNet(level, vol)) == 1, "after saving and loading it is still found");
        helper.assertTrue(kopie.getStorage().extract(overHetNet(level, vol), 1).getCount() == 1, "and taken out");
        level.removePlayerImmediately(speler, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }
}
