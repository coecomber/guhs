package nl.juiced.guhs.feature.bank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import nl.juiced.guhs.block.BankGuhBlock;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.menu.BankGuhMenu;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModDataComponents;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.BankContents;
import nl.juiced.guhs.storage.BankStorage;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.NlTekst;

/**
 * bbq2 (bank): the cap of 256 per kind of item, banks from before the cap (nothing clamped, nothing lost), every way in
 * keeps the remainder (the screen's actions, shift-click, closing, JEI), the upgrade (use on the bank, kept on block
 * entity + item + loot table), the Bank Guh's item capability (in to the cap, out only when upgraded, transactions), and
 * the Hapluikje with its link key (vadskracht, any distance, another dimension, deposit only, clean refusals).
 */
public class BankGameTests {
    private static final String KAMER = "bank_test_kamer";
    private static final String BATCH = "bank";
    private static final int CAP = BankStorage.CAP;

    private static BlockPos p(int x, int z) {
        return new BlockPos(x, 2, z);
    }

    private static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    private static BankGuhBlockEntity bank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.BANK_GUH.get());
        return (BankGuhBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static HapluikjeBlockEntity luikje(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, BankFeature.HAPLUIKJE.get());
        return (HapluikjeBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static void bron(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 1));
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(p(3, 3));
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static ItemStack kei(int n) {
        return new ItemStack(Items.COBBLESTONE, n);
    }

    /** A stomach as a bank from before the cap could have it: far more than the cap of one kind. */
    private static BankContents oud(long keien) {
        return new BankContents(List.of(new BankContents.Entry(kei(1), keien), new BankContents.Entry(new ItemStack(Items.DIAMOND), 3)));
    }

    // =====================================================================================================================
    // the cap
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void bankCapPerSoort(GameTestHelper helper) {
        BankStorage s = bank(helper, p(2, 2)).getStorage();
        gelijk(helper, (long) CAP, s.room(kei(1)), "an empty bank has room for the cap");
        helper.assertTrue(s.insert(kei(64)).isEmpty() && s.insert(kei(64)).isEmpty() && s.insert(kei(64)).isEmpty(), "three stacks fit");
        ItemStack stapel = kei(64);
        helper.assertTrue(s.insert(stapel).isEmpty(), "the fourth stack makes it exactly the cap");
        gelijk(helper, 64, stapel.getCount(), "the stack that was put in is not changed");
        helper.assertTrue(s.isFull(kei(1)) && s.room(kei(1)) == 0, "full of cobblestone");
        ItemStack rest = s.insert(kei(30));
        helper.assertTrue(rest.is(Items.COBBLESTONE) && rest.getCount() == 30, "full: the whole stack comes back, got " + rest);
        gelijk(helper, 0L, s.insert(kei(1), 1), "not even one more");
        gelijk(helper, (long) CAP, s.count(kei(1)), "exactly the cap inside");
        // other kinds have their own cap; the same item with other components is another kind
        gelijk(helper, 100L, s.insert(new ItemStack(Items.DIRT), 100), "dirt has its own room");
        ItemStack naam = kei(10);
        naam.set(DataComponents.CUSTOM_NAME, Component.literal("Knabbelkei"));
        helper.assertTrue(s.insert(naam).isEmpty() && s.count(naam) == 10 && s.count(kei(1)) == CAP, "a named cobblestone is a kind of its own");
        // taking out makes room again; a partial fit gives back the rest
        gelijk(helper, 10, s.extract(kei(1), 10).getCount(), "ten taken out");
        rest = s.insert(kei(25));
        helper.assertTrue(rest.getCount() == 15 && s.count(kei(1)) == CAP, "ten of the 25 fit, 15 come back, got " + rest);
        // more than a stack at once: only what fits
        gelijk(helper, 156L, s.insert(new ItemStack(Items.DIRT), 1000), "1000 dirt: only up to the cap");
        gelijk(helper, 3, s.snapshot().entries().size(), "three kinds inside");
        helper.succeed();
    }

    /** A bank from before the cap: nothing is clamped or lost, taking out always works, putting in waits for room. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void bankOudeVoorraadBlijft(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = p(2, 2);
        BankGuhBlockEntity be = bank(helper, pos);
        // as it comes out of an old save: the block entity data of 1.2.8 has only "Stomach"
        CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
        BankGuhBlockEntity oudeBank = new BankGuhBlockEntity(be.getBlockPos(), be.getBlockState());
        oudeBank.getStorage().load(oud(64_000));
        CompoundTag oudTag = oudeBank.saveWithoutMetadata(level.registryAccess());
        oudTag.remove("Opgevoerd");
        oudTag.remove("BankId");
        be.loadWithComponents(Nbt.input(level.registryAccess(), oudTag));
        BankStorage s = be.getStorage();
        gelijk(helper, 64_000L, s.count(kei(1)), "64,000 cobblestone from before the cap is all still there");
        helper.assertTrue(!s.isUpgraded() && s.isFull(kei(1)) && s.room(kei(1)) == 0, "no upgrade, so no room for more of it");
        ItemStack rest = s.insert(kei(64));
        helper.assertTrue(rest.getCount() == 64 && s.count(kei(1)) == 64_000, "putting more in is refused, the stack comes back");
        gelijk(helper, 64, s.extract(kei(1), 64).getCount(), "taking out always works");
        gelijk(helper, 63_936L, s.count(kei(1)), "one stack less");
        gelijk(helper, 3L, s.insert(new ItemStack(Items.DIAMOND), 3), "another kind goes in as usual");
        // the screen: shift-take into a full inventory puts everything back (it came out a moment ago: no cap)
        ServerPlayer speler = speler(helper);
        for (int i = 0; i < 36; i++) {
            speler.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        BankGuhMenu menu = new BankGuhMenu(1, speler.getInventory(), be);
        menu.handleAction(BankGuhMenu.Action.TAKE, kei(1), 0, true);
        gelijk(helper, 63_936L, s.count(kei(1)), "a take that fits nowhere loses nothing");
        // only under the cap there is room again
        long eruit = 0;
        while (s.count(kei(1)) > CAP - 6) {
            eruit += s.extract(kei(1), 64_000).getCount();
        }
        s.restore(kei(CAP - 6));
        gelijk(helper, 63_936L, eruit, "everything could be taken out");
        rest = s.insert(kei(10));
        helper.assertTrue(rest.getCount() == 4 && s.count(kei(1)) == CAP, "under the cap again: six fit, four come back, got " + rest);
        // saved, loaded, broken, placed again: an over-full stomach stays as it is
        s.restore(kei(64));
        s.restore(new ItemStack(Items.COBBLESTONE, 40_000));
        long totaal = s.count(kei(1));
        tag = be.saveWithoutMetadata(level.registryAccess());
        BankGuhBlockEntity kopie = new BankGuhBlockEntity(be.getBlockPos(), be.getBlockState());
        kopie.loadWithComponents(Nbt.input(level.registryAccess(), tag));
        gelijk(helper, totaal, kopie.getStorage().count(kei(1)), "saving and loading clamps nothing");
        weg(helper, speler);
        level.destroyBlock(helper.absolutePos(pos), true);
        helper.succeedWhen(() -> {
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2));
            helper.assertTrue(!drops.isEmpty(), "the Bank Guh drops");
            BankContents contents = drops.get(0).getItem().get(ModDataComponents.BANK_CONTENTS.get());
            helper.assertTrue(contents != null && contents.totalItems() == totaal + 6, "the dropped Bank Guh keeps everything: "
                    + (contents == null ? null : contents.totalItems()) + " of " + (totaal + 6));
            drops.forEach(Entity::discard);
        });
    }

    // =====================================================================================================================
    // the screen: every way in keeps the remainder
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void bankSchermHoudtDeRest(GameTestHelper helper) {
        BankGuhBlockEntity be = bank(helper, p(2, 2));
        BankStorage s = be.getStorage();
        s.insert(kei(1), CAP - 6);
        ServerPlayer speler = speler(helper);
        BankGuhMenu menu = new BankGuhMenu(1, speler.getInventory(), be);
        // the cursor: left click puts in what fits, the rest stays on the cursor
        menu.setCarried(kei(64));
        menu.handleAction(BankGuhMenu.Action.DEPOSIT_CARRIED, ItemStack.EMPTY, 0, false);
        helper.assertTrue(s.count(kei(1)) == CAP && menu.getCarried().getCount() == 58, "six went in, 58 stay on the cursor: " + menu.getCarried());
        // right click (one at a time) on a full bank takes nothing
        menu.handleAction(BankGuhMenu.Action.DEPOSIT_CARRIED, ItemStack.EMPTY, 1, false);
        menu.handleAction(BankGuhMenu.Action.TAKE, kei(1), 0, false);   // (a click on the grid while holding something = deposit)
        gelijk(helper, 58, menu.getCarried().getCount(), "a full bank takes nothing from the cursor");
        menu.setCarried(ItemStack.EMPTY);
        // "Alles erin": what the bank is full of stays in the inventory, the rest goes in
        speler.getInventory().setItem(9, kei(64));
        speler.getInventory().setItem(10, new ItemStack(Items.DIRT, 10));
        menu.handleAction(BankGuhMenu.Action.DEPOSIT_INVENTORY, ItemStack.EMPTY, 0, false);
        helper.assertTrue(speler.getInventory().getItem(9).getCount() == 64 && speler.getInventory().getItem(10).isEmpty() && s.count(new ItemStack(Items.DIRT)) == 10,
                "deposit all: the cobblestone stays, the dirt went in");
        // shift-click in the inventory
        menu.quickMoveStack(speler, BankGuhMenu.INV_START);
        gelijk(helper, 64, speler.getInventory().getItem(9).getCount(), "shift-click: the stack the bank is full of stays put");
        gelijk(helper, 10, s.extract(kei(1), 10).getCount(), "(ten out)");
        menu.quickMoveStack(speler, BankGuhMenu.INV_START);
        helper.assertTrue(speler.getInventory().getItem(9).getCount() == 54 && s.count(kei(1)) == CAP, "shift-click: ten fit, 54 stay in the slot");
        speler.getInventory().setItem(9, ItemStack.EMPTY);
        // the crafting grid: "Rooster leeg", shift-click and closing the screen all hand the remainder to the player
        menu.getSlot(BankGuhMenu.GRID_START).set(kei(5));
        menu.getSlot(BankGuhMenu.GRID_START + 1).set(new ItemStack(Items.STICK, 7));
        menu.handleAction(BankGuhMenu.Action.CLEAR_GRID, ItemStack.EMPTY, 0, false);
        helper.assertTrue(!menu.getSlot(BankGuhMenu.GRID_START).hasItem() && !menu.getSlot(BankGuhMenu.GRID_START + 1).hasItem(), "the grid is empty");
        helper.assertTrue(speler.getInventory().countItem(Items.COBBLESTONE) == 5 && s.count(new ItemStack(Items.STICK)) == 7,
                "clear grid: the sticks went into the bank, the cobblestone (full) to the inventory");
        menu.getSlot(BankGuhMenu.GRID_START + 4).set(kei(3));
        menu.quickMoveStack(speler, BankGuhMenu.GRID_START + 4);
        helper.assertTrue(!menu.getSlot(BankGuhMenu.GRID_START + 4).hasItem() && speler.getInventory().countItem(Items.COBBLESTONE) == 8,
                "shift-click in the grid: to the inventory when the bank is full of it");
        menu.getSlot(BankGuhMenu.GRID_START + 8).set(kei(2));
        menu.getSlot(BankGuhMenu.GRID_START + 7).set(new ItemStack(Items.STICK, 1));
        menu.removed(speler);
        helper.assertTrue(speler.getInventory().countItem(Items.COBBLESTONE) == 10 && s.count(new ItemStack(Items.STICK)) == 8,
                "closing: the stick went into the bank, the cobblestone back to the player");
        gelijk(helper, (long) CAP, s.count(kei(1)), "and the bank never went over the cap");
        // JEI's "+": the old grid goes back first; when it fits nowhere the grid is left alone (nothing is overwritten)
        for (int i = 0; i < 36; i++) {
            speler.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        BankGuhMenu vol = new BankGuhMenu(2, speler.getInventory(), be);
        vol.getSlot(BankGuhMenu.GRID_START).set(kei(4));
        List<List<ItemStack>> stokken = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            stokken.add(i == 0 || i == 3 ? List.of(new ItemStack(Items.STICK)) : List.of());
        }
        vol.vulGrid(stokken, false);
        ItemStack inGrid = vol.getSlot(BankGuhMenu.GRID_START).getItem();
        helper.assertTrue(inGrid.is(Items.COBBLESTONE) && inGrid.getCount() == 4 && s.count(new ItemStack(Items.STICK)) == 8,
                "JEI: bank and inventory are full of the old grid, so it stays and nothing is taken: " + inGrid);
        speler.getInventory().clearContent();
        vol.vulGrid(stokken, false);
        helper.assertTrue(vol.getSlot(BankGuhMenu.GRID_START).getItem().is(Items.STICK) && vol.getSlot(BankGuhMenu.GRID_START + 3).getItem().is(Items.STICK)
                && speler.getInventory().countItem(Items.COBBLESTONE) == 4 && s.count(new ItemStack(Items.STICK)) == 6,
                "JEI: with room in the inventory the old grid moves there and the recipe is filled from the bank");
        weg(helper, speler);
        helper.succeed();
    }

    // =====================================================================================================================
    // the upgrade
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void bankUpgradeBlijftBijDeBank(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = p(2, 2), abs = helper.absolutePos(pos);
        BankGuhBlockEntity be = bank(helper, pos);
        be.getStorage().insert(kei(1), 1000);
        gelijk(helper, (long) CAP, be.getStorage().count(kei(1)), "(the cap, before the upgrade)");
        ServerPlayer speler = speler(helper);
        speler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BankFeature.BANK_UPGRADE.get(), 2));
        BlockHitResult klik = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).useItemOn(speler.getMainHandItem(), level, speler, InteractionHand.MAIN_HAND, klik);
        helper.assertTrue(be.isUpgraded() && be.getStorage().isUpgraded(), "the bank is upgraded");
        gelijk(helper, 1, speler.getMainHandItem().getCount(), "one upgrade was used up");
        helper.assertBlockProperty(pos, BankGuhBlock.OPGEVOERD, true);
        helper.assertTrue(level.getBlockEntity(abs) == be, "(the same block entity: the stomach is untouched)");
        gelijk(helper, 100_000L, be.getStorage().insert(kei(1), 100_000), "no cap any more");
        helper.assertTrue(be.getStorage().insert(kei(64)).isEmpty() && be.getStorage().count(kei(1)) == CAP + 100_064, "everything fits");
        level.getBlockState(abs).useItemOn(speler.getMainHandItem(), level, speler, InteractionHand.MAIN_HAND, klik);
        gelijk(helper, 1, speler.getMainHandItem().getCount(), "a bank that has it does not eat a second upgrade");
        UUID id = be.bankId();
        // saved and loaded
        BankGuhBlockEntity kopie = new BankGuhBlockEntity(be.getBlockPos(), be.getBlockState());
        kopie.loadWithComponents(Nbt.input(level.registryAccess(), be.saveWithoutMetadata(level.registryAccess())));
        helper.assertTrue(kopie.isUpgraded() && id.equals(kopie.bankId()), "the upgrade and the id survive saving");
        // broken (the loot table copies the components) and placed again
        weg(helper, speler);
        level.destroyBlock(abs, true);
        // (wait for the drop first; what follows runs ONCE: placing the bank again inside a retried check would put down a
        // new bank at every try and hide the real failure behind a follow-up one)
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2)).isEmpty(),
                "the Bank Guh drops")).thenExecute(() -> {
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2));
            ItemStack item = drops.get(0).getItem();
            helper.assertTrue(item.is(ModItems.BANK_GUH.get()), "a Bank Guh item");
            helper.assertTrue(Boolean.TRUE.equals(item.get(BankFeature.BANK_OPGEVOERD.get())), "the item carries the upgrade");
            helper.assertTrue(id.equals(item.get(BankFeature.BANK_ID.get())), "the item carries the bank's id");
            BankContents contents = item.get(ModDataComponents.BANK_CONTENTS.get());
            helper.assertTrue(contents != null && contents.totalItems() == CAP + 100_064, "and its stomach, over the cap");
            helper.assertTrue(BankAdressen.van(level.getServer()).plek(id) == null, "a bank in a pocket has no address");
            BankGuhBlockEntity terug = bank(helper, p(4, 4));
            terug.applyComponentsFromItemStack(item);
            terug.meld();
            helper.assertTrue(terug.isUpgraded() && id.equals(terug.bankId()) && terug.getStorage().count(kei(1)) == CAP + 100_064,
                    "placed again: upgraded, the same id, everything inside");
            helper.assertBlockProperty(p(4, 4), BankGuhBlock.OPGEVOERD, true);
            drops.forEach(Entity::discard);
        }).thenSucceed();
    }

    // =====================================================================================================================
    // the item capability of the Bank Guh
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void bankCapabilityInAltijdUitAlleenOpgevoerd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BankGuhBlockEntity be = bank(helper, p(2, 2));
        BankStorage s = be.getStorage();
        ResourceHandler<ItemResource> h = Kisten.van(level, helper.absolutePos(p(2, 2)), Direction.UP);
        helper.assertTrue(h != null && h == Kisten.van(level, helper.absolutePos(p(2, 2)), null), "the Bank Guh has the item capability (one handler)");
        // in: up to the cap, the rest comes back
        gelijk(helper, 0, Kisten.stop(h, kei(64)).getCount(), "a stack goes in");
        gelijk(helper, 0, Kisten.stop(h, new ItemStack(Items.COBBLESTONE, 150)).getCount(), "150 more");
        ItemStack rest = Kisten.stop(h, kei(64));
        helper.assertTrue(rest.getCount() == 22 && s.count(kei(1)) == CAP, "42 of the next stack fit, 22 come back, got " + rest);
        helper.assertTrue(!Kisten.past(h, kei(1)) && Kisten.past(h, new ItemStack(Items.DIRT, 64)), "past: full of cobblestone, room for dirt");
        int versie = s.version();
        gelijk(helper, 0, Kisten.stop(h, new ItemStack(Items.DIRT, 64), true).getCount(), "simulated: fits");
        helper.assertTrue(s.count(new ItemStack(Items.DIRT)) == 0 && s.version() == versie, "simulated: nothing went in, the screen need not resync");
        // an aborted transaction puts everything back, a nested one too
        try (Transaction buiten = Transaction.openRoot()) {
            gelijk(helper, 30, h.insert(ItemResource.of(Items.DIRT), 30, buiten), "(30 dirt in an open transaction)");
            try (Transaction binnen = Transaction.open(buiten)) {
                gelijk(helper, 226, h.insert(ItemResource.of(Items.DIRT), 500, binnen), "(only up to the cap, also inside a transaction)");
                gelijk(helper, (long) CAP, s.count(new ItemStack(Items.DIRT)), "(visible while open)");
            }
            gelijk(helper, 30L, s.count(new ItemStack(Items.DIRT)), "the inner transaction was rolled back");
        }
        helper.assertTrue(s.count(new ItemStack(Items.DIRT)) == 0 && s.version() == versie, "the outer one too: nothing changed");
        // loaned things never go in
        ItemStack geleend = geleend();
        if (!geleend.isEmpty()) {
            gelijk(helper, 1, Kisten.stop(h, geleend).getCount(), "a loaned thing is refused");
        }
        // looking is always allowed (a Voorraadmeter), taking only from an upgraded bank
        Kisten.stop(h, new ItemStack(Items.DIRT, 12));
        gelijk(helper, (long) CAP, Kisten.tel(h, st -> st.is(Items.COBBLESTONE)), "tel sees the cobblestone");
        gelijk(helper, 12L, Kisten.tel(h, st -> st.is(Items.DIRT)), "and the dirt");
        helper.assertTrue(Kisten.neem(h, st -> true, 64).isEmpty() && Kisten.neem(h, st -> true, 64, true).isEmpty(), "no upgrade: nothing comes out");
        helper.setBlock(p(4, 2), Blocks.CHEST);
        ResourceHandler<ItemResource> kist = Kisten.van(level, helper.absolutePos(p(4, 2)), null);
        gelijk(helper, 0, Kisten.verplaats(h, kist, st -> true, 1000), "no upgrade: nothing can be moved out");
        gelijk(helper, (long) CAP + 12, be.getStorage().snapshot().totalItems(), "everything is still inside");
        s.setUpgraded(true);
        ItemStack eruit = Kisten.neem(h, st -> st.is(Items.DIRT), 5);
        helper.assertTrue(eruit.is(Items.DIRT) && eruit.getCount() == 5 && s.count(new ItemStack(Items.DIRT)) == 7, "upgraded: five dirt taken");
        // moving everything out walks over the slots: every kind must come along (the slots do not shift)
        s.insert(new ItemStack(Items.STICK), 9);
        s.insert(new ItemStack(Items.APPLE), 4);
        s.insert(new ItemStack(Items.GRAVEL), 20);
        int verplaatst;
        try (Transaction tx = Transaction.openRoot()) {
            verplaatst = ResourceHandlerUtil.move(h, kist, soort -> true, 10_000, tx);
            tx.commit();
        }
        gelijk(helper, CAP + 7 + 9 + 4 + 20, verplaatst, "everything moved to the chest in one go");
        helper.assertTrue(s.snapshot().isEmpty(), "the bank is empty");
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(p(4, 2)));
        helper.assertTrue(chest.countItem(Items.COBBLESTONE) == CAP && chest.countItem(Items.DIRT) == 7 && chest.countItem(Items.STICK) == 9
                && chest.countItem(Items.APPLE) == 4 && chest.countItem(Items.GRAVEL) == 20, "all five kinds are in the chest");
        // an upgraded bank has no cap for pipes either
        gelijk(helper, 0, Kisten.stop(h, new ItemStack(Items.DIRT, 5000)).getCount(), "upgraded: 5000 dirt go in");
        helper.succeed();
    }

    /** Real hoppers: one on top fills the bank up to the cap, one below only gets something from an upgraded bank. */
    @GuhTest(template = KAMER, batch = "bank_trechter", timeoutTicks = 400)
    public static void bankTrechters(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos onder = new BlockPos(3, 2, 3), midden = onder.above(), boven = midden.above();
        helper.setBlock(onder, Blocks.HOPPER);
        BankGuhBlockEntity be = bank(helper, midden);
        be.getStorage().insert(kei(1), CAP - 3);
        helper.setBlock(boven, Blocks.HOPPER);
        HopperBlockEntity in = (HopperBlockEntity) level.getBlockEntity(helper.absolutePos(boven));
        HopperBlockEntity uit = (HopperBlockEntity) level.getBlockEntity(helper.absolutePos(onder));
        in.setItem(0, kei(8));
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(be.getStorage().count(kei(1)) == CAP && in.getItem(0).getCount() == 5,
                    "the hopper on top filled the bank to the cap and keeps the other five: " + be.getStorage().count(kei(1)) + " / " + in.getItem(0));
            helper.assertTrue(uit.isEmpty(), "the hopper below gets nothing from a bank without the upgrade");
            in.setItem(0, ItemStack.EMPTY);
            be.getStorage().setUpgraded(true);
            helper.succeedWhen(() -> helper.assertTrue(uit.countItem(Items.COBBLESTONE) >= 3 && be.getStorage().count(kei(1)) < CAP,
                    "upgraded: the hopper below pulls cobblestone out"));
        });
    }

    private static ItemStack geleend() {
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            ItemStack s = new ItemStack(item);
            if (Features.isLoaned(s)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    // =====================================================================================================================
    // the Hapluikje and the Banksleutel
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = "bank_luikje", timeoutTicks = 400)
    public static void bankHapluikjeHaptVoorZijnBank(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BankGuhBlockEntity be = bank(helper, p(5, 5));
        BankGuhBlockEntity andere = bank(helper, p(5, 1));
        HapluikjeBlockEntity luikje = luikje(helper, p(1, 1));
        ServerPlayer speler = speler(helper);
        ResourceHandler<ItemResource> bek = Kisten.van(level, helper.absolutePos(p(1, 1)), Direction.UP);
        helper.assertTrue(bek != null, "the Hapluikje has the item capability");
        helper.assertTrue(level.getBlockState(helper.absolutePos(p(1, 1))).is(VadsKracht.TOON), "the hover readout works on it");
        // no vadskracht: asleep, refuses
        gelijk(helper, HapluikjeBlockEntity.Stand.SLAAPT, luikje.stand(), "no vadskracht yet");
        gelijk(helper, 5, Kisten.stop(bek, kei(5)).getCount(), "asleep: it takes nothing");
        bron(helper, p(1, 2));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(luikje.heeftKracht(), "vadskracht from the source next to it");
            gelijk(helper, 2, VadsKracht.net(level, helper.absolutePos(p(1, 1))).vraag(), "it asks 2 VK");
            // not linked: refuses
            gelijk(helper, HapluikjeBlockEntity.Stand.LOS, luikje.stand(), "not linked yet");
            gelijk(helper, 5, Kisten.stop(bek, kei(5)).getCount(), "not linked: it takes nothing");
            // a blank key does nothing; a key that knows the bank links the luikje
            ItemStack sleutel = new ItemStack(BankFeature.BANK_SLEUTEL.get());
            luikje.sleutel(speler, sleutel);
            helper.assertTrue(luikje.bank() == null, "a blank key links nothing");
            BlockPos bankAbs = helper.absolutePos(p(5, 5));
            speler.setItemInHand(InteractionHand.MAIN_HAND, sleutel);
            level.getBlockState(bankAbs).useItemOn(sleutel, level, speler, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(bankAbs), Direction.UP, bankAbs, false));
            helper.assertTrue(be.bankId().equals(BankSleutelItem.bank(sleutel)) && sleutel.getCount() == 1, "the key remembers the bank and is not used up");
            BlockPos luikAbs = helper.absolutePos(p(1, 1));
            level.getBlockState(luikAbs).useItemOn(sleutel, level, speler, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(luikAbs), Direction.UP, luikAbs, false));
            helper.assertTrue(be.bankId().equals(luikje.bank()) && sleutel.getCount() == 1, "the luikje is linked; the key stays");
            gelijk(helper, HapluikjeBlockEntity.Stand.KLAAR, luikje.stand(), "ready");
            // in: straight into the bank; the other bank gets nothing
            gelijk(helper, 0, Kisten.stop(bek, kei(40)).getCount(), "40 cobblestone swallowed");
            helper.assertTrue(be.getStorage().count(kei(1)) == 40 && andere.getStorage().snapshot().isEmpty(), "they are in its own bank");
            gelijk(helper, 0, Kisten.stop(bek, kei(3), true).getCount(), "simulated: fits");
            gelijk(helper, 40L, be.getStorage().count(kei(1)), "simulated: nothing went in");
            // deposit only
            helper.assertTrue(Kisten.neem(bek, st -> true, 64).isEmpty() && Kisten.tel(bek, st -> true) == 0, "nothing ever comes out of a luikje");
            be.getStorage().setUpgraded(true);
            helper.assertTrue(Kisten.neem(bek, st -> true, 64).isEmpty(), "not even from an upgraded bank");
            be.getStorage().setUpgraded(false);
            // by hand: what fits goes in, the rest stays in the hand
            be.getStorage().insert(kei(1), CAP - 40 - 10);
            speler.setItemInHand(InteractionHand.MAIN_HAND, kei(64));
            level.getBlockState(luikAbs).useItemOn(speler.getMainHandItem(), level, speler, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(luikAbs), Direction.UP, luikAbs, false));
            helper.assertTrue(be.getStorage().count(kei(1)) == CAP && speler.getMainHandItem().getCount() == 54,
                    "by hand: ten fit, 54 stay in the hand: " + speler.getMainHandItem());
            // full: refuses cleanly, the giver keeps everything, the face is surprised for a moment
            gelijk(helper, 7, Kisten.stop(bek, kei(7)).getCount(), "the bank is full of it: the luikje takes nothing");
            gelijk(helper, 0, Kisten.stop(bek, new ItemStack(Items.DIRT, 7)).getCount(), "(something else still goes in)");
            ItemStack lening = geleend();
            if (!lening.isEmpty()) {
                gelijk(helper, 1, Kisten.stop(bek, lening).getCount(), "a loaned thing is refused");
            }
            // a second luikje on the same bank; a luikje has one bank: linking it again moves it
            HapluikjeBlockEntity tweede = luikje(helper, p(1, 3));
            tweede.sleutel(speler, sleutel);
            ItemStack sleutel2 = new ItemStack(BankFeature.BANK_SLEUTEL.get());
            BankSleutelItem.onthoud(sleutel2, andere);
            luikje.sleutel(speler, sleutel2);
            helper.assertTrue(andere.bankId().equals(luikje.bank()) && be.bankId().equals(tweede.bank()), "one bank per luikje, several luikjes per bank");
            // the hover lines say which bank
            List<Component> regels = new ArrayList<>();
            luikje.vadsRegels(regels::add);
            BlockPos a = andere.getBlockPos();
            helper.assertTrue(regels.size() == 1 && NlTekst.tekst(regels.get(0)).contains(a.getX() + ", " + a.getY() + ", " + a.getZ()),
                    "the readout names the bank's spot: " + regels.stream().map(NlTekst::tekst).toList());
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(tweede.heeftKracht(), "(the second luikje is on the same net)");
                helper.assertBlockProperty(p(1, 1), MachineBlock.SNOET, Snoet.VOL);
                gelijk(helper, 0, Kisten.stop(Kisten.van(level, helper.absolutePos(p(1, 3)), null), new ItemStack(Items.STICK, 2)).getCount(), "the second luikje");
                gelijk(helper, 0, Kisten.stop(bek, new ItemStack(Items.STICK, 3)).getCount(), "the first, now for the other bank");
                helper.assertTrue(be.getStorage().count(new ItemStack(Items.STICK)) == 2 && andere.getStorage().count(new ItemStack(Items.STICK)) == 3,
                        "each luikje fed its own bank");
                // the bank is picked up: it stands nowhere, the luikje refuses and the giver keeps the items
                UUID id = andere.bankId();
                ItemStack opgepakt = new ItemStack(ModItems.BANK_GUH.get());
                opgepakt.applyComponents(andere.collectComponents());
                level.destroyBlock(andere.getBlockPos(), false);
                gelijk(helper, HapluikjeBlockEntity.Stand.WEG, luikje.stand(), "its bank stands nowhere");
                gelijk(helper, 4, Kisten.stop(bek, new ItemStack(Items.STICK, 4)).getCount(), "no bank: it takes nothing");
                regels.clear();
                luikje.vadsRegels(regels::add);
                helper.assertTrue(NlTekst.tekst(regels.get(0)).contains("nergens"), "the readout says so: " + NlTekst.tekst(regels.get(0)));
                // placed again somewhere else: the same bank (its id), so the luikje works again without a new link
                BankGuhBlockEntity terug = bank(helper, p(3, 5));
                terug.applyComponentsFromItemStack(opgepakt);
                terug.meld();
                helper.assertTrue(id.equals(terug.bankId()) && terug.getStorage().count(new ItemStack(Items.STICK)) == 3, "(the same bank, with its stomach)");
                gelijk(helper, 0, Kisten.stop(bek, new ItemStack(Items.STICK, 4)).getCount(), "the luikje found its bank at the new spot");
                gelijk(helper, 7L, terug.getStorage().count(new ItemStack(Items.STICK)), "seven sticks in the moved bank");
                // without vadskracht it stops again
                helper.setBlock(p(1, 2), Blocks.AIR);
                weg(helper, speler);
                helper.succeedWhen(() -> {
                    helper.assertBlockProperty(p(1, 1), MachineBlock.SNOET, Snoet.SLAAPT);
                    gelijk(helper, 2, Kisten.stop(bek, new ItemStack(Items.STICK, 2)).getCount(), "no vadskracht: it takes nothing");
                    gelijk(helper, 7L, terug.getStorage().count(new ItemStack(Items.STICK)), "nothing more went in");
                });
            });
        });
    }

    /** A hopper feeds the luikje: the bank fills to the cap and the hopper keeps what does not fit. */
    @GuhTest(template = KAMER, batch = "bank_luikje_trechter", timeoutTicks = 400)
    public static void bankHapluikjeOnderEenTrechter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BankGuhBlockEntity be = bank(helper, p(5, 5));
        be.getStorage().insert(kei(1), CAP - 4);
        HapluikjeBlockEntity luikje = luikje(helper, p(1, 1));
        luikje.koppel(be.bankId());
        be.meld();
        bron(helper, p(1, 2));
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.HOPPER);
        HopperBlockEntity trechter = (HopperBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 3, 1)));
        trechter.setItem(0, kei(9));
        trechter.setItem(1, new ItemStack(Items.DIRT, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.getStorage().count(kei(1)) == CAP, "the bank is full of cobblestone: " + be.getStorage().count(kei(1)));
            gelijk(helper, 5, trechter.getItem(0).getCount(), "the hopper keeps the five that do not fit");
            helper.assertTrue(be.getStorage().count(new ItemStack(Items.DIRT)) == 2 && trechter.getItem(1).isEmpty(), "the dirt behind it still went through");
        });
    }

    /**
     * Any distance, another dimension: the bank stands in the Nether in a chunk that is not loaded. Asking the luikje what
     * fits loads nothing and keeps nothing loaded (it answers from what the address book remembers of the unloaded bank);
     * only a real delivery loads the chunk; a bank the book knows nothing of (after a server start) comes in the background.
     */
    @GuhTest(template = KAMER, batch = "bank_luikje_ver", timeoutTicks = 4000)
    public static void bankHapluikjeAndereDimensie(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the test server has a Nether");
        BlockPos ver = new BlockPos(5000, 200, -5000);
        nether.setBlock(ver, ModBlocks.BANK_GUH.get().defaultBlockState(), 3);
        BankGuhBlockEntity be = (BankGuhBlockEntity) nether.getBlockEntity(ver);
        be.meld();
        UUID id = be.bankId();
        be.getStorage().insert(kei(1), CAP - 20);
        HapluikjeBlockEntity luikje = luikje(helper, p(1, 1));
        luikje.koppel(id);
        bron(helper, p(1, 2));
        ResourceHandler<ItemResource> bek = Kisten.van(level, helper.absolutePos(p(1, 1)), null);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(luikje.heeftKracht() && !nether.isLoaded(ver) && !BankAdressen.geladen(nether, ver),
                        "vadskracht, and the bank's chunk has unloaded"))
                .thenExecute(() -> {
                    helper.assertTrue(BankAdressen.zoek(level.getServer(), id, false) == null, "(not loaded: without loading there is no bank at hand)");
                    gelijk(helper, HapluikjeBlockEntity.Stand.KLAAR, luikje.stand(), "the address book still knows where it stands");
                    // a question (a transaction that is not committed) is answered from memory: nothing is loaded
                    helper.assertTrue(BankAdressen.van(level.getServer()).schaduw(id) != null, "the book still knows the stomach of the unloaded bank");
                    for (int i = 0; i < 5; i++) {
                        gelijk(helper, 44, Kisten.stop(bek, kei(64), true).getCount(), "asked: 20 of the 64 would fit");
                        helper.assertTrue(Kisten.past(bek, kei(20)) && !Kisten.past(bek, kei(21)) && Kisten.past(bek, new ItemStack(Items.DIRT, 64)),
                                "asked: 20 fit, 21 do not, dirt does");
                    }
                    try (Transaction tx = Transaction.openRoot()) {
                        gelijk(helper, 12, bek.insert(ItemResource.of(kei(1)), 12, tx), "inside one question: 12 of the 20");
                        gelijk(helper, 8, bek.insert(ItemResource.of(kei(1)), 30, tx), "and then only the 8 that are left");
                    }
                    helper.assertTrue(!BankAdressen.geladen(nether, ver) && !nether.isLoaded(ver), "asking what fits did not load the bank's chunk");
                    gelijk(helper, 44, Kisten.stop(bek, kei(64), true).getCount(), "(a question that was thrown away left nothing behind)");
                    // the real delivery
                    ItemStack rest = Kisten.stop(bek, kei(64));
                    gelijk(helper, 44, rest.getCount(), "20 fit in the far bank, 44 come back");
                    helper.assertTrue(nether.isLoaded(ver), "the luikje loaded the bank's chunk");
                    BankGuhBlockEntity daar = (BankGuhBlockEntity) nether.getBlockEntity(ver);
                    helper.assertTrue(daar != null && id.equals(daar.bankId()) && daar.getStorage().count(kei(1)) == CAP, "the bank in the Nether is full now");
                    gelijk(helper, 0, Kisten.stop(bek, new ItemStack(Items.DIRT, 9)).getCount(), "and dirt goes in too");
                    gelijk(helper, 9L, daar.getStorage().count(new ItemStack(Items.DIRT)), "nine dirt in the Nether");
                })
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(nether.isLoaded(ver), "a bank in use stays loaded for a while (no reload per item)"))
                // from here on something asks the luikje every tick (a Richtingstuk, a hopper holding what the bank is full of): that
                // keeps nothing loaded
                .thenWaitUntil(() -> {
                    gelijk(helper, 7, Kisten.stop(bek, kei(7)).getCount(), "full of cobblestone: nothing is taken");
                    helper.assertTrue(Kisten.past(bek, new ItemStack(Items.DIRT, 5)), "(asked: dirt fits)");
                    helper.assertTrue(!nether.isLoaded(ver) && !BankAdressen.geladen(nether, ver), "asked every tick, and still the chunk unloads again");
                })
                .thenExecute(() -> {
                    BankAdressen boek = BankAdressen.van(level.getServer());
                    helper.assertTrue(boek.schaduw(id) != null && boek.schaduw(id).ruimte(ItemResource.of(kei(1))) == 0
                            && boek.schaduw(id).ruimte(ItemResource.of(new ItemStack(Items.DIRT))) == CAP - 9, "the book remembers the full bank");
                    gelijk(helper, 7, Kisten.stop(bek, kei(7)).getCount(), "full of cobblestone: refused, really asked or not");
                    helper.assertTrue(!BankAdressen.geladen(nether, ver), "and nothing was loaded to say so");
                    // after a server start the book remembers nothing: the chunk is fetched in the background, the luikje waits
                    boek.vergeet(id);
                    gelijk(helper, 4, Kisten.stop(bek, new ItemStack(Items.DIRT, 4)).getCount(), "not known what the bank holds: nothing is taken yet");
                    helper.assertTrue(!BankAdressen.geladen(nether, ver), "and the server did not wait for the chunk");
                })
                // (whoever brings something tries again, a tube every 8 ticks: each try asks for the chunk again, so on a test
                // server whose ticks race ahead of the disk the 20 second ticket cannot run out before the chunk is there)
                .thenWaitUntil(() -> {
                    if (!BankAdressen.geladen(nether, ver)) {
                        Kisten.past(bek, new ItemStack(Items.DIRT, 4));
                    }
                    helper.assertTrue(BankAdressen.geladen(nether, ver), "the chunk came in the background");
                })
                .thenExecute(() -> {
                    gelijk(helper, 0, Kisten.stop(bek, new ItemStack(Items.DIRT, 4)).getCount(), "now the dirt goes in");
                    BankGuhBlockEntity daar = (BankGuhBlockEntity) nether.getBlockEntity(ver);
                    gelijk(helper, 13L, daar.getStorage().count(new ItemStack(Items.DIRT)), "thirteen dirt in the Nether");
                })
                .thenWaitUntil(() -> helper.assertTrue(!nether.isLoaded(ver) && !BankAdressen.geladen(nether, ver), "left alone, the chunk unloads again"))
                .thenExecute(() -> {
                    // gone from the far place: the luikje refuses and tidies the address book
                    nether.setBlock(ver, Blocks.AIR.defaultBlockState(), 3 | 256);   // (no side effects: a stale address)
                    gelijk(helper, 3, Kisten.stop(bek, new ItemStack(Items.DIRT, 3)).getCount(), "the bank is gone: nothing is taken");
                    helper.assertTrue(BankAdressen.van(level.getServer()).plek(id) == null, "the stale address is wiped");
                    gelijk(helper, HapluikjeBlockEntity.Stand.WEG, luikje.stand(), "its bank stands nowhere");
                })
                .thenSucceed();
    }
}
