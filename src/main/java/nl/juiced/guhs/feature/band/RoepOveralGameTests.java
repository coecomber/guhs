package nl.juiced.guhs.feature.band;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.6.2: "Roep naar mij" works everywhere ({@link Roepen}, {@link UitItem}): a picked-up guh comes out of a chest (also
 * from a shulker box in it) and the chest is empty after; a guh in a Guh Wheel comes off it (the wheel stops); a guh in
 * the pockets comes out of them; and never two of the same guh (an item copy of a guh that is in the world stays an item,
 * only the real one comes). (Template huisje_test_tuin: 24 x 24 grass at y 0.)
 */
public class RoepOveralGameTests {
    private static final String TUIN = "huisje_test_tuin";
    private static final String BATCH = "roep_overal";

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    /** A tamed guh of this player, picked up: the item (the entity is gone). */
    private static ItemStack opgepakt(GameTestHelper helper, ServerPlayer owner, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(owner);
        Band.bijwerken(guh);
        return PickedUpGuhItem.pickUp(guh);
    }

    private static UUID id(ItemStack stack) {
        return GuhVolger.wie(stack).id();
    }

    /** How many guhs with this UUID are in the level (never more than one). */
    private static int hoeveel(ServerLevel level, UUID id) {
        return level.getEntity(id) == null ? 0 : 1;
    }

    private static void bijSpeler(GameTestHelper helper, ServerPlayer p, UUID id, String wat) {
        Entity e = helper.getLevel().getEntity(id);
        helper.assertTrue(e instanceof GuhEntity && !e.isRemoved(), wat + ": the guh is there");
        double d = e.distanceTo(p);
        helper.assertTrue(d <= 3.6, wat + ": next to the player (" + d + ")");
        Plek nu = GuhVolger.plek(p.level().getServer(), p.getUUID(), id);
        helper.assertTrue(nu.soort() == PlekSoort.BIJ_JOU, wat + ": bij jou (" + nu + ")");
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 400)
    public static void roepUitKist(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        ItemStack stack = opgepakt(helper, p, new BlockPos(4, 1, 4));
        UUID id = id(stack);
        BlockPos kist = new BlockPos(4, 1, 6);
        helper.setBlock(kist, Blocks.CHEST);
        ChestBlockEntity be = helper.getBlockEntity(kist, ChestBlockEntity.class);
        be.setItem(5, stack.copy());
        GuhVolger.item(stack, PlekSoort.ITEM_KIST, helper.getLevel().dimension(), helper.absolutePos(kist), "Kist", helper.getLevel().getGameTime());
        helper.assertTrue(hoeveel(helper.getLevel(), id) == 0, "only the item is there");
        Roepen.Uitkomst u = Roepen.roep(p, id);
        helper.assertTrue(u == Roepen.Uitkomst.OK || u == Roepen.Uitkomst.BEZIG, "it comes: " + u);
        helper.runAfterDelay(60, () -> {
            bijSpeler(helper, p, id, "out of the chest");
            helper.assertTrue(be.getItem(5).isEmpty(), "the chest doesn't hold it any more");
            helper.assertTrue(hoeveel(helper.getLevel(), id) == 1, "exactly one");
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 400)
    public static void roepUitShulkerInKist(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        ItemStack stack = opgepakt(helper, p, new BlockPos(4, 1, 4));
        UUID id = id(stack);
        ItemStack doos = new ItemStack(net.minecraft.world.item.Items.PINK_SHULKER_BOX);
        java.util.List<ItemStack> inhoud = new java.util.ArrayList<>(java.util.Collections.nCopies(27, ItemStack.EMPTY));
        inhoud.set(3, stack.copy());
        doos.set(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(inhoud));
        BlockPos kist = new BlockPos(6, 1, 6);
        helper.setBlock(kist, Blocks.CHEST);
        ChestBlockEntity be = helper.getBlockEntity(kist, ChestBlockEntity.class);
        be.setItem(0, doos);
        GuhVolger.item(stack, PlekSoort.ITEM_KIST, helper.getLevel().dimension(), helper.absolutePos(kist), "Kist", helper.getLevel().getGameTime());
        Roepen.roep(p, id);
        helper.runAfterDelay(60, () -> {
            bijSpeler(helper, p, id, "out of the shulker box in the chest");
            ItemStack nu = be.getItem(0);
            helper.assertTrue(nu.is(net.minecraft.world.item.Items.PINK_SHULKER_BOX), "the box stays in the chest");
            helper.assertTrue(nu.get(net.minecraft.core.component.DataComponents.CONTAINER).nonEmptyItemCopyStream().findAny().isEmpty(),
                    "and is empty now");
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 400)
    public static void roepUitGuhrad(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        ItemStack stack = opgepakt(helper, p, new BlockPos(4, 1, 4));
        UUID id = id(stack);
        BlockPos rad = new BlockPos(8, 1, 8);
        helper.setBlock(rad, ModBlocks.GUH_WHEEL.get().defaultBlockState().setValue(GuhWheelBlock.RUNNING, true));
        GuhWheelBlockEntity wiel = helper.getBlockEntity(rad, GuhWheelBlockEntity.class);
        wiel.insert(PickedUpGuhItem.guhData(stack));
        GuhVolger.item(stack, PlekSoort.GUHWIEL, helper.getLevel().dimension(), helper.absolutePos(rad), "", helper.getLevel().getGameTime());
        Roepen.roep(p, id);
        helper.runAfterDelay(60, () -> {
            bijSpeler(helper, p, id, "off the Guh Wheel");
            helper.assertTrue(!wiel.hasGuh(), "the wheel is empty");
            helper.assertTrue(!helper.getBlockState(rad).getValue(GuhWheelBlock.RUNNING), "and stopped");
            weg(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 200)
    public static void roepUitZakken(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        ItemStack stack = opgepakt(helper, p, new BlockPos(4, 1, 4));
        UUID id = id(stack);
        p.getInventory().add(stack.copy());
        GuhVolger.inZakken(stack, p);
        helper.assertTrue(Roepen.roep(p, id) == Roepen.Uitkomst.OK, "out of the pockets at once");
        bijSpeler(helper, p, id, "out of the pockets");
        boolean nogItem = false;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            nogItem |= UitItem.isDeze(p.getInventory().getItem(i), id);
        }
        helper.assertTrue(!nogItem, "the item is gone from the pockets");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 200)
    public static void roepNooitTwee(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 1, 4));
        guh.tame(p);
        Band.bijwerken(guh);
        UUID id = guh.getUUID();
        // a stray item copy of the very same guh in a chest (a dupe from elsewhere): calling must not make a second guh
        net.minecraft.nbt.CompoundTag data = nl.juiced.guhs.storage.Nbt.saveWithoutId(guh);
        data.putString("id", "guhs:guh");
        ItemStack kopie = PickedUpGuhItem.of(data);
        BlockPos kist = new BlockPos(6, 1, 4);
        helper.setBlock(kist, Blocks.CHEST);
        helper.getBlockEntity(kist, ChestBlockEntity.class).setItem(0, kopie);
        helper.assertTrue(Roepen.roep(p, id) == Roepen.Uitkomst.OK, "the real one comes");
        helper.runAfterDelay(40, () -> {
            bijSpeler(helper, p, id, "the real guh");
            int n = helper.getLevel().getEntitiesOfClass(GuhEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40),
                    g -> g.getUUID().equals(id)).size();
            helper.assertTrue(n == 1, "exactly one guh with its UUID: " + n);
            // and releasing the copy's data on purpose gives the real one, not a second
            GuhEntity weer = Roepen.verschijn(p, PickedUpGuhItem.guhData(kopie));
            helper.assertTrue(weer != null && weer.getUUID().equals(id) && weer == helper.getLevel().getEntity(id), "verschijn never makes a twin");
            weg(helper, p);
            helper.succeed();
        });
    }

    /** 1.6.3: a picked-up guh on the ground never burns or despawns; when its item is destroyed it goes to the wolkjes. */
    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 200)
    public static void kapotItemNaarDeWolkjes(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        ItemStack stack = opgepakt(helper, p, new BlockPos(4, 1, 4));
        UUID id = id(stack);
        helper.assertTrue(stack.has(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT), "it does not burn");
        BlockPos abs = helper.absolutePos(new BlockPos(4, 1, 6));
        net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),
                abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, stack.copy());
        helper.getLevel().addFreshEntity(item);
        item.getItem().getItem().onDestroyed(item);
        item.discard();
        helper.assertTrue(Wolkjes.isDood(helper.getLevel().getServer(), p.getUUID(), id), "in the wolkjes");
        helper.assertTrue(GuhVolger.plek(p.level().getServer(), p.getUUID(), id).soort() == PlekSoort.IN_DE_WOLKJES, "'waar is hij' says so");
        GuhEntity terug = Wolkjes.terug(helper.getLevel(), p, id, p.position());
        helper.assertTrue(terug != null && terug.getUUID().equals(id) && hoeveel(helper.getLevel(), id) == 1, "the Knuffelhart brings the same guh back");
        terug.discard();
        weg(helper, p);
        helper.succeed();
    }

    /** 1.6.3: called, its spot on the ground is loaded but the item is gone (burned before 1.6.3): to the wolkjes. */
    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = Roepen.WACHT + 200)
    public static void verlorenItemNaarDeWolkjes(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(20, 1, 20));
        ItemStack stack = opgepakt(helper, p, new BlockPos(4, 1, 4));
        UUID id = id(stack);
        GuhVolger.item(stack, PlekSoort.ITEM_GROND, helper.getLevel().dimension(), helper.absolutePos(new BlockPos(4, 1, 6)), "", helper.getLevel().getGameTime());
        Roepen.Uitkomst u = Roepen.roep(p, id);
        helper.assertTrue(u == Roepen.Uitkomst.BEZIG, "it is looked for: " + u);
        helper.runAfterDelay(Roepen.WACHT + 20, () -> {
            helper.assertTrue(Wolkjes.isDood(helper.getLevel().getServer(), p.getUUID(), id), "in the wolkjes");
            GuhEntity terug = Wolkjes.terug(helper.getLevel(), p, id, p.position());
            helper.assertTrue(terug != null && terug.getUUID().equals(id) && terug.isTame(), "the Knuffelhart brings it back");
            terug.discard();
            weg(helper, p);
            helper.succeed();
        });
    }
}
