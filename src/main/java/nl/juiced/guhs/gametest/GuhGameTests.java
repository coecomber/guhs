package nl.juiced.guhs.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.GuhWireBlock;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.block.entity.FryingPanBlockEntity;
import nl.juiced.guhs.entity.QuestGuhEntity;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModDataComponents;
import nl.juiced.guhs.storage.BankContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModPoiTypes;
import nl.juiced.guhs.world.GuhPortalForcer;
import nl.juiced.guhs.world.ModDimensions;

/**
 * In-game tests. Run all of them headless with:  gradlew runGameTestServer
 * (or in a dev world with  /test runall).
 */
public class GuhGameTests {
    private static final String EMPTY = "empty";
    private static final String PORTAL_ROOM = "portal_room";
    private static final String WIRE_ROOM = "wire_room";
    private static final String SLED_ROOM = "sled_room";
    private static final String COASTER_ROOM = "coaster_room";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    @GuhTest(template = EMPTY)
    public static void wildGuhHasRandomSizeAndWildHealth(GameTestHelper helper) {
        for (int i = 0; i < 20; i++) {
            GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
            guh.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(guh.blockPosition()), EntitySpawnReason.NATURAL, null);
            float scale = guh.getScale();
            helper.assertTrue(scale >= GuhEntity.MIN_SCALE && scale <= GuhEntity.MAX_SCALE, "scale out of range: " + scale);
            helper.assertTrue(Math.abs(guh.getBbWidth() - 0.9f * scale) < 0.01f, "hitbox does not follow scale");
            helper.assertTrue(guh.getMaxHealth() == GuhEntity.WILD_HEALTH, "wild guh should have 25 hp");
            guh.discard();
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void tamingGivesThousandHealth(GameTestHelper helper) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        guh.tame(player);
        helper.assertTrue(guh.getMaxHealth() == GuhEntity.TAMED_HEALTH, "tamed max health should be 1000");
        helper.assertTrue(guh.getHealth() == GuhEntity.TAMED_HEALTH, "taming should fully heal");
        helper.assertTrue(player.getUUID().equals(guh.getOwnerUUID()), "player should own the guh");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void gravityToggleSquishesAndStopsJumping(GameTestHelper helper) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        float height = guh.getBbHeight();
        guh.setGravityEnabled(true);
        helper.assertTrue(guh.getBbHeight() < height, "gravity should flatten the hitbox");
        guh.setTeleportEnabled(false);
        helper.assertTrue(!guh.isTeleportEnabled(), "teleport toggle");
        guh.setGravityEnabled(false);
        helper.assertTrue(Math.abs(guh.getBbHeight() - height) < 0.001f, "hitbox should restore");
        helper.succeed();
    }

    /** Builds a 4x5 frame (2x3 inside) at x=1..4, y=1..5, z=1. The last block placed should light the portal. */
    private static void buildFrame(GameTestHelper helper) {
        for (int x = 1; x <= 4; x++) {
            for (int y = 1; y <= 5; y++) {
                if (x == 1 || x == 4 || y == 1 || y == 5) {
                    helper.setBlock(new BlockPos(x, y, 1), ModBlocks.BLOCK_OF_KAASKNABBELS.get());
                }
            }
        }
    }

    @GuhTest(template = PORTAL_ROOM)
    public static void portalOpensWhenFrameIsCompletedAndClosesWhenBroken(GameTestHelper helper) {
        buildFrame(helper);
        for (int x = 2; x <= 3; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.assertBlockPresent(ModBlocks.GUH_PORTAL.get(), new BlockPos(x, y, 1));
            }
        }
        BlockPos portal = helper.absolutePos(new BlockPos(2, 2, 1));
        helper.assertTrue(helper.getLevel().getPoiManager().getType(portal).map(t -> t.is(ModPoiTypes.GUH_PORTAL.getKey())).orElse(false),
                "portal blocks should be registered as POI");

        helper.setBlock(new BlockPos(1, 3, 1), net.minecraft.world.level.block.Blocks.AIR);
        for (int x = 2; x <= 3; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.assertBlockNotPresent(ModBlocks.GUH_PORTAL.get(), new BlockPos(x, y, 1));
            }
        }
        helper.succeed();
    }

    /**
     * Needs the Guhmension to exist. The headless GameTest server (runGameTestServer) only creates a flat overworld,
     * so this test is optional there - run it in a real world with  /test run guhs:guhgametests.portalroundtriptoguhmension
     */
    @GuhTest(template = PORTAL_ROOM, required = false)
    public static void portalRoundTripToGuhmension(GameTestHelper helper) {
        buildFrame(helper);
        BlockPos portalPos = helper.absolutePos(new BlockPos(2, 2, 1));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 2, 1));

        TeleportTransition there = GuhPortalForcer.getDestination(helper.getLevel(), guh, portalPos);
        helper.assertTrue(there != null && there.newLevel().dimension() == ModDimensions.GUHMENSION, "should lead to the Guhmension");
        ServerLevel guhmension = there.newLevel();
        helper.assertTrue(guhmension.getBlockState(BlockPos.containing(there.pos())).is(ModBlocks.GUH_PORTAL.get()),
                "should arrive inside a (new) guh portal");
        helper.assertTrue(guhmension.getBlockState(BlockPos.containing(there.pos()).below()).is(ModBlocks.BLOCK_OF_KAASKNABBELS.get()),
                "exit portal should have a kaasknabbel frame");
        // the new portal must be on the surface, not buried: open sky right above its top frame
        helper.assertTrue(guhmension.canSeeSky(BlockPos.containing(there.pos()).above(4)),
                "exit portal should be built on the surface, got y=" + there.pos().y);

        // coming back from the Guhmension at the same x/z should find our original portal again
        TeleportTransition back = GuhPortalForcer.getDestination(guhmension, guh, BlockPos.containing(there.pos()));
        helper.assertTrue(back != null && back.newLevel().dimension() == Level.OVERWORLD, "should lead back to the overworld");
        helper.assertTrue(back.pos().distanceTo(Vec3.atBottomCenterOf(portalPos)) < 3, "should come back through the original portal, got " + back.pos());
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void kaasknabbelBlocksDropKaasKnabbels(GameTestHelper helper) {
        Block[] ores = {ModBlocks.KAASKNABBEL_STONE.get(), ModBlocks.KAASKNABBEL_DEEPSLATE.get(),
                ModBlocks.KAASKNABBEL_DIRT.get(), ModBlocks.KAASKNABBEL_COBBLESTONE.get()};
        for (int i = 0; i < ores.length; i++) {
            BlockPos pos = new BlockPos(i + 1, 1, 1);
            helper.setBlock(pos, ores[i]);
            helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        }
        helper.succeedWhen(() -> {
            for (int i = 0; i < ores.length; i++) {
                helper.assertItemEntityPresent(ModItems.KAAS_KNABBELS.get(), new BlockPos(i + 1, 1, 1), 1.0);
            }
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Saddles, fried knabbels, Mika
    // ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void ridingNeedsASaddle(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        guh.setGuhScale(1.5f);
        guh.tame(player);

        guh.onOwnerTap(player);
        helper.assertTrue(player.getVehicle() == null && guh.isOrderedToSit(), "without a saddle a tap should only sit");
        guh.toggleSit();

        helper.assertTrue(guh.isSaddleable(), "big tamed guh should take a saddle");
        guh.equipSaddle(new ItemStack(Items.SADDLE), null);
        guh.onOwnerTap(player);
        helper.assertTrue(player.getVehicle() == guh, "with a saddle a tap should ride");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void friedKnabbelsHealToFull(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        guh.tame(player);
        guh.setHealth(10f);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 3));
        guh.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(guh.getHealth() == GuhEntity.TAMED_HEALTH, "should be at full health, is " + guh.getHealth());
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "should use one fried knabbel");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void mikaIsAggressiveButHarmless(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        MikaEntity mika = helper.spawn(ModEntities.MIKA.get(), POS);
        helper.assertTrue(mika.getMaxHealth() == MikaEntity.HEALTH, "Mika should have 50 hp");
        float before = player.getHealth();
        mika.doHurtTarget(player);
        helper.assertTrue(player.getHealth() == before, "Mika should not deal damage");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Frying pan
    // ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void fryingPanFriesUpTo64PerVet(GameTestHelper helper) {
        BlockPos pan = new BlockPos(2, 1, 2);
        helper.setBlock(pan, ModBlocks.FRYING_PAN.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 10));
        helper.useBlock(pan, player);
        helper.assertTrue(player.getMainHandItem().getCount() == 10, "no fat, so nothing should be fried");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MIKA_VET.get(), 1));
        helper.useBlock(pan, player);
        FryingPanBlockEntity be = (FryingPanBlockEntity) helper.getBlockEntity(pan);
        helper.assertTrue(be.getCharges() == 64, "one vet should give 64 charges, got " + be.getCharges());

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 74));
        helper.useBlock(pan, player);
        int fried = player.getInventory().countItem(ModItems.GEFRITUURDE_KAASKNABBELS.get());
        helper.assertTrue(fried == 64, "should have fried exactly 64, got " + fried);
        helper.assertTrue(player.getInventory().countItem(ModItems.KAAS_KNABBELS.get()) == 10, "knabbels beyond the fat should stay raw");
        helper.assertTrue(be.getCharges() == 0, "pan should be empty");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Guh wheel + guh wire
    // ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void guhWheelPowersRedstoneWhileAGuhRuns(GameTestHelper helper) {
        BlockPos wheelPos = new BlockPos(2, 1, 2);
        BlockPos lamp = new BlockPos(3, 1, 2);
        helper.setBlock(wheelPos, ModBlocks.GUH_WHEEL.get());
        helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(1, 1, 1));
        guh.tame(player);

        // pick the guh up and put it in the wheel
        player.setItemInHand(InteractionHand.MAIN_HAND, PickedUpGuhItem.pickUp(guh));
        helper.useBlock(wheelPos, player);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the picked-up guh should have gone into the wheel");
        helper.assertBlockProperty(wheelPos, GuhWheelBlock.RUNNING, true);
        helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, true);

        // take it out again: you get the picked-up guh back
        helper.useBlock(wheelPos, player);
        helper.assertTrue(player.getInventory().countItem(ModItems.PICKED_UP_GUH.get()) == 1, "should get the guh back as an item");
        helper.assertBlockProperty(wheelPos, GuhWheelBlock.RUNNING, false);
        helper.succeedWhen(() -> helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, false));
    }

    /** 40 blocks of guh wire: a lamp at the far end still gets full power (redstone dust fades out after 15). */
    @GuhTest(template = WIRE_ROOM, timeoutTicks = 60)
    public static void guhWireCarriesFullPowerOver40Blocks(GameTestHelper helper) {
        int length = 40;
        for (int x = 0; x <= length + 1; x++) {
            helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE);
        }
        for (int x = 1; x <= length; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), ModBlocks.GUH_WIRE.get());
        }
        BlockPos lamp = new BlockPos(length + 1, 1, 1);
        helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.REDSTONE_BLOCK);

        helper.assertBlockProperty(new BlockPos(length, 1, 1), GuhWireBlock.POWERED, true);
        helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, true);
        // and it switches off again: the wire must not keep itself powered
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.AIR);
        helper.assertBlockProperty(new BlockPos(length, 1, 1), GuhWireBlock.POWERED, false);
        helper.succeedWhen(() -> helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, false));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Structures: the templates load and contain what they should
    // ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = "hamster_house", timeoutTicks = 40)
    public static void hamsterHouseHasGiantGuhAndSpawner(GameTestHelper helper) {
        // (test coordinates are one higher than the template coordinates in tools/make_structures.py)
        helper.assertBlockPresent(ModBlocks.GUH_SPAWNER.get(), new BlockPos(24, 2, 22));
        helper.succeedWhen(() -> helper.assertTrue(
                helper.getEntities(ModEntities.GUH.get()).stream().anyMatch(g -> g.getScale() > 6.5f),
                "the giant guh (about 10 blocks long) should be there"));
    }

    @GuhTest(template = "evil_mika_home", timeoutTicks = 40)
    public static void evilMikaHomeHasMikas(GameTestHelper helper) {
        helper.succeedWhen(() -> helper.assertTrue(helper.getEntities(ModEntities.MIKA.get()).size() >= 2, "Mikas should live here"));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Picking up guhs, name tags, the Hungry Guh quest, the Bank Guh
    // ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void sneakTapPicksUpAndItemPutsBack(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        guh.tame(player);
        guh.setGuhScale(1.7f);
        guh.setCustomName(Component.literal("Pluisje"));
        player.setShiftKeyDown(true);
        guh.onOwnerTap(player);
        helper.assertTrue(guh.isRemoved(), "sneak-tap should pick the guh up");
        ItemStack item = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.PICKED_UP_GUH.get())) {
                item = player.getInventory().getItem(i);
            }
        }
        helper.assertTrue(item.getHoverName().getString().contains("Pluisje"), "the item should carry the name");

        var placed = PickedUpGuhItem.release(helper.getLevel(), PickedUpGuhItem.guhData(item),
                helper.absolutePos(POS).getX() + 0.5, helper.absolutePos(POS).getY(), helper.absolutePos(POS).getZ() + 0.5, 0);
        helper.assertTrue(placed instanceof GuhEntity g && Math.abs(g.getGuhScale() - 1.7f) < 0.01f
                && player.getUUID().equals(g.getOwnerUUID()), "the placed guh should keep its size and owner");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void namedGuhsAndMikasShowTheirName(GameTestHelper helper) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        helper.assertTrue(guh.shouldShowName() && guh.getDisplayName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr
                && tr.getKey().equals("entity.guhs.guh"), "an unnamed guh shows 'Guh'");
        ItemStack tag = new ItemStack(Items.NAME_TAG);
        tag.set(DataComponents.CUSTOM_NAME, Component.literal("Vadsig"));
        tag.interactLivingEntity(helper.makeMockPlayer(GameType.SURVIVAL), guh, InteractionHand.MAIN_HAND);
        helper.assertTrue(guh.hasCustomName() && guh.shouldShowName(), "a name-tagged guh shows its name");
        MikaEntity mika = helper.spawn(ModEntities.MIKA.get(), POS);
        helper.assertTrue(mika.shouldShowName() && mika.hasCustomName(), "Mika always shows 'Mika'");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void hungryGuhTradesTenFriedKnabbelsForABankGuh(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        QuestGuhEntity quest = helper.spawn(ModEntities.QUEST_GUH.get(), POS);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 9));
        quest.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!quest.isRemoved() && player.getMainHandItem().getCount() == 9, "9 is not enough");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 12));
        quest.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(quest.isRemoved(), "the Hungry Guh should leave after the trade");
        helper.assertTrue(player.getInventory().countItem(ModItems.GEFRITUURDE_KAASKNABBELS.get()) == 2, "10 knabbels should be taken");
        helper.assertTrue(player.getInventory().countItem(ModItems.BANK_GUH.get()) == 1, "should get a Bank Guh");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void bankGuhStoresInfinitelyAndKeepsItWhenBroken(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.BANK_GUH.get());
        BankGuhBlockEntity bank = (BankGuhBlockEntity) helper.getBlockEntity(pos);
        for (int i = 0; i < 1000; i++) {
            bank.getStorage().insert(new ItemStack(Items.COBBLESTONE, 64));
        }
        bank.getStorage().insert(new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(bank.getStorage().count(new ItemStack(Items.COBBLESTONE)) == 64_000, "64,000 cobblestone should fit");
        ItemStack taken = bank.getStorage().extract(new ItemStack(Items.COBBLESTONE), 64);
        helper.assertTrue(taken.getCount() == 64 && bank.getStorage().count(new ItemStack(Items.COBBLESTONE)) == 63_936, "taking a stack");

        // break it: the item keeps the stomach, placing it again restores everything
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        helper.succeedWhen(() -> {
            var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(2));
            helper.assertTrue(!drops.isEmpty(), "Bank Guh should drop");
            ItemStack dropped = drops.get(0).getItem();
            BankContents contents = dropped.get(ModDataComponents.BANK_CONTENTS.get());
            helper.assertTrue(contents != null && contents.totalItems() == 63_939, "the dropped Bank Guh should keep its items");
        });
    }

    @GuhTest(template = WIRE_ROOM)
    public static void guhWireConnectsLikeRedstoneDust(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.GUH_WIRE.get());
        helper.assertBlockProperty(new BlockPos(1, 1, 1), GuhWireBlock.NORTH, RedstoneSide.SIDE); // alone: a cross
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.GUH_WIRE.get());
        helper.setBlock(new BlockPos(3, 1, 1), ModBlocks.GUH_WIRE.get());
        // in a row: a straight east-west line, not a cross
        helper.assertBlockProperty(new BlockPos(2, 1, 1), GuhWireBlock.EAST, RedstoneSide.SIDE);
        helper.assertBlockProperty(new BlockPos(2, 1, 1), GuhWireBlock.WEST, RedstoneSide.SIDE);
        helper.assertBlockProperty(new BlockPos(2, 1, 1), GuhWireBlock.NORTH, RedstoneSide.NONE);
        helper.assertBlockProperty(new BlockPos(2, 1, 1), GuhWireBlock.SOUTH, RedstoneSide.NONE);
        helper.succeed();
    }

    @GuhTest(template = "guh_picnic", timeoutTicks = 40)
    public static void guhPicnicHasTheHungryGuh(GameTestHelper helper) {
        helper.succeedWhen(() -> helper.assertEntityPresent(ModEntities.QUEST_GUH.get()));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Kaas saus, the big wheel, the new structures
    // ------------------------------------------------------------------------------------------------------------

    /** Kaas saus spreads as far as water, but 3x slower (water 5 ticks per step, kaas saus 15, lava 30). */
    @GuhTest(template = WIRE_ROOM, timeoutTicks = 200)
    public static void kaasSausFlowsSlowerThanWater(GameTestHelper helper) {
        var level = helper.getLevel();
        int saus = nl.juiced.guhs.registry.ModFluids.KAAS_SAUS.get().getTickDelay(level);
        int water = net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level);
        int lava = net.minecraft.world.level.material.Fluids.LAVA.getTickDelay(level);
        helper.assertTrue(water < saus && saus < lava, "kaas saus should be between water and lava: " + water + " < " + saus + " < " + lava);
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.KAAS_SAUS.get());
        helper.succeedWhen(() -> helper.assertTrue(
                level.getFluidState(helper.absolutePos(new BlockPos(5, 1, 1))).getFluidType() == nl.juiced.guhs.registry.ModFluids.KAAS_SAUS_TYPE.get(),
                "kaas saus should flow a few blocks"));
    }

    @GuhTest(template = WIRE_ROOM, timeoutTicks = 20)
    public static void bigWheelTakesUpThreeByThree(GameTestHelper helper) {
        for (int x = 0; x < 6; x++) {
            helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE);
        }
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ITEMS.getEntries().stream()
                .filter(i -> i.get() == ModBlocks.GUH_WHEEL.get().asItem()).findFirst().orElseThrow().get()));
        helper.placeAt(player, player.getMainHandItem(), new BlockPos(2, 0, 1), net.minecraft.core.Direction.UP);
        helper.assertBlockPresent(ModBlocks.GUH_WHEEL.get(), new BlockPos(2, 1, 1));
        int parts = 0;
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                if (helper.getBlockState(new BlockPos(x, y, 1)).is(ModBlocks.GUH_WHEEL_PART.get())) {
                    parts++;
                }
            }
        }
        helper.assertTrue(parts == 8, "the big wheel should place 8 parts, placed " + parts);
        helper.destroyBlock(new BlockPos(1, 3, 1));
        helper.succeedWhen(() -> helper.assertBlockNotPresent(ModBlocks.GUH_WHEEL.get(), new BlockPos(2, 1, 1)));
    }

    @GuhTest(template = "hamster_house_large", timeoutTicks = 60)
    public static void largeHamsterHouseHasTheMegaGuh(GameTestHelper helper) {
        helper.succeedWhen(() -> helper.assertTrue(
                helper.getEntities(ModEntities.GUH.get()).stream().anyMatch(g -> g.getScale() > 9f),
                "the mega guh (~14 blocks) should be there"));
    }

    @GuhTest(template = "hamster_house_medium", timeoutTicks = 60)
    public static void mediumHamsterHouseHasABigGuh(GameTestHelper helper) {
        helper.succeedWhen(() -> helper.assertTrue(
                helper.getEntities(ModEntities.GUH.get()).stream().anyMatch(g -> g.getScale() > 7f), "big guh"));
    }

    @GuhTest(template = "cheese_fountain", timeoutTicks = 40)
    public static void cheeseFountainHasKaasSaus(GameTestHelper helper) {
        helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(8, 2, 5)))
                .getFluidType() == nl.juiced.guhs.registry.ModFluids.KAAS_SAUS_TYPE.get(), "the basin should be full of kaas saus"));
    }

    @GuhTest(template = "guh_caves/central_room", timeoutTicks = 40)
    public static void guhCaveCentralRoomLoads(GameTestHelper helper) {
        helper.succeedWhen(() -> helper.assertTrue(helper.getEntities(ModEntities.GUH.get()).size() >= 2, "guhs live in the cave"));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Guh menu settings: behaviour, sounds, armour
    // ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = WIRE_ROOM, timeoutTicks = 200)
    public static void aggressiveGuhAttacksMobsInItsRadius(GameTestHelper helper) {
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        Player player = helper.makeMockServerPlayerInLevel(); // a real (online) owner, so the guh doesn't just sit
        BlockPos near = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 1));
        guh.tame(player);
        guh.setTeleportEnabled(false);
        guh.setBehavior(GuhEntity.Behavior.AGGRESSIVE);
        guh.setAttackRadius(10);
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.HUSK, new BlockPos(8, 1, 1));
        zombie.setNoAi(true);
        helper.succeedWhen(() -> helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "the aggressive guh should attack the husk; target=" + guh.getTarget() + " dist=" + guh.distanceTo(zombie) + " nav=" + guh.getNavigation().isDone()));
    }

    @GuhTest(template = WIRE_ROOM, timeoutTicks = 100)
    public static void passiveGuhLeavesMobsAlone(GameTestHelper helper) {
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        Player player = helper.makeMockServerPlayerInLevel(); // a real (online) owner, so the guh doesn't just sit
        BlockPos near = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 1));
        guh.tame(player);
        guh.setTeleportEnabled(false);
        guh.setBehavior(GuhEntity.Behavior.PASSIVE);
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.HUSK, new BlockPos(4, 1, 1));
        zombie.setNoAi(true);
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth() && guh.getTarget() == null, "a passive guh does nothing");
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY)
    public static void guhSoundAndBehaviourSettings(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        helper.assertTrue(guh.getAmbientSoundInterval() == GuhEntity.WILD_SOUND_INTERVAL, "wild guhs: 20% fewer sounds");
        guh.tame(player);
        guh.setSoundFrequency(4);
        helper.assertTrue(guh.getAmbientSoundInterval() == GuhEntity.SOUND_INTERVALS[4], "very often");
        guh.setAttackRadius(99);
        helper.assertTrue(guh.getAttackRadius() == GuhEntity.MAX_ATTACK_RADIUS, "radius is capped at 20");
        guh.setSoundsEnabled(false);
        guh.setBehavior(GuhEntity.Behavior.NEUTRAL);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        guh.saveWithoutId(tag);
        GuhEntity copy = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        copy.load(tag);
        helper.assertTrue(!copy.areSoundsEnabled() && copy.getBehavior() == GuhEntity.Behavior.NEUTRAL && copy.getSoundFrequency() == 4,
                "settings should be saved");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void guhArmourGivesProtectionAndShowsItsTier(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        guh.tame(player);
        int before = guh.getArmorValue();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIAMOND_GUH_ARMOR.get()));
        guh.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(guh.getArmorTier() == nl.juiced.guhs.item.GuhArmorItem.Tier.DIAMOND, "should wear diamond armour");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the armour should be used up");
        helper.succeedWhen(() -> helper.assertTrue(guh.getArmorValue() > before, "armour should protect: " + guh.getArmorValue()));
    }

    // ------------------------------------------------------------------------------------------------------------
    // 1.6: Big Mika, Vahoege Vads, new structures
    // ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void mikaShovesHard(GameTestHelper helper) {
        MikaEntity mika = helper.spawn(ModEntities.MIKA.get(), POS);
        net.minecraft.world.entity.monster.Husk husk = helper.spawn(net.minecraft.world.entity.EntityType.HUSK, POS.east());
        float before = husk.getHealth();
        mika.doHurtTarget(husk);
        helper.assertTrue(husk.getHealth() == before, "a normal Mika should not hurt");
        helper.assertTrue(husk.getDeltaMovement().horizontalDistance() > 0.5, "Mika should shove hard: " + husk.getDeltaMovement());
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void bigMikaReallyHurts(GameTestHelper helper) {
        MikaEntity mika = helper.spawn(ModEntities.MIKA.get(), POS);
        mika.makeBoss();
        mika.setHealth(mika.getMaxHealth());
        helper.assertTrue(mika.isBoss() && mika.getMaxHealth() == MikaEntity.BOSS_HEALTH, "Big Mika should have 200 hp");
        net.minecraft.world.entity.monster.Husk husk = helper.spawn(net.minecraft.world.entity.EntityType.HUSK, POS.east());
        float before = husk.getHealth();
        mika.doHurtTarget(husk);
        helper.assertTrue(husk.getHealth() <= before - 5, "Big Mika should deal real damage: " + (before - husk.getHealth()));
        // a Big Mika saved and loaded stays Big Mika
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        mika.saveWithoutId(tag);
        MikaEntity copy = ModEntities.MIKA.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        copy.load(tag);
        helper.assertTrue(copy.isBoss() && copy.getMaxHealth() == MikaEntity.BOSS_HEALTH, "Big Mika should survive a reload");
        helper.succeed();
    }

    @GuhTest(template = "challenging_guh_caves/dungeon_hall", timeoutTicks = 40)
    public static void dungeonHallHasBigMikaAndTreasure(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getEntities(ModEntities.MIKA.get()).stream().anyMatch(MikaEntity::isBoss), "Big Mika guards the hall");
            helper.assertBlockPresent(Blocks.CHEST, new BlockPos(11, 3, 12));
            helper.assertBlockPresent(Blocks.SPAWNER, new BlockPos(5, 2, 12));
        });
    }

    @GuhTest(template = "guh_statue", timeoutTicks = 40)
    public static void guhStatueIsBuilt(GameTestHelper helper) {
        int[] solid = {0};
        BlockPos.betweenClosed(0, 2, 0, 39, 26, 49).forEach(p -> {
            if (!helper.getBlockState(p).isAir()) solid[0]++;
        });
        helper.assertTrue(solid[0] > 2000, "the statue should be a big pile of blocks: " + solid[0]);
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void newStructureTemplatesExist(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        for (String name : new String[]{"hamster_house_extra_extra_large", "grand_cheese_fountain", "guh_statue",
                "challenging_guh_caves/dungeon_hall", "challenging_guh_caves/tube_straight", "challenging_guh_caves/tube_junction",
                "challenging_guh_caves/tube_end", "challenging_guh_caves/spawner_room", "challenging_guh_caves/parkour_room",
                "challenging_guh_caves/mika_den", "mini_picnic", "block_guh", "giant_kaasknabbel", "kaasknabbel_arch",
                "giant_cake", "quartz_statue", "guh_fossil", "guhramid", "guh_village/layout_a", "guh_village/layout_b"}) {
            helper.assertTrue(manager.get(Guhs.id(name)).isPresent(), "missing structure template " + name);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void vadsOreDropsVadsAndVadsGearIsUnbreakable(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.COMPRESSED_SUPER_VAHOEGE_VADS.get());
        var drops = Block.getDrops(helper.getBlockState(POS), helper.getLevel(), helper.absolutePos(POS), null, null,
                new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.stream().anyMatch(d -> d.is(ModItems.VAHOEGE_VADS.get())), "the ore should drop vahoege vads: " + drops);
        helper.assertTrue(helper.getBlockState(POS).requiresCorrectToolForDrops()
                && new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(helper.getBlockState(POS))
                && !new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(helper.getBlockState(POS)), "needs an iron pickaxe");
        ItemStack sword = new ItemStack(ModItems.VADS_SWORD.get());
        helper.assertTrue(sword.has(DataComponents.UNBREAKABLE), "vads tools never break");
        helper.assertTrue(new ItemStack(ModItems.VADS_CHESTPLATE.get()).has(DataComponents.UNBREAKABLE), "vads armour never breaks");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void vadsGearIsKeptOnDeath(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(3, new ItemStack(ModItems.VADS_PICKAXE.get()));
        player.getInventory().setItem(4, new ItemStack(Items.DIAMOND_PICKAXE));
        nl.juiced.guhs.event.KeepOnDeathHandler.onDeath(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(
                player, player.damageSources().generic()));
        helper.assertTrue(player.getInventory().getItem(3).isEmpty(), "vads gear is set aside before the death drops");
        helper.assertTrue(player.getInventory().getItem(4).is(Items.DIAMOND_PICKAXE), "other items drop as usual");
        Player respawned = helper.makeMockPlayer(GameType.SURVIVAL);
        nl.juiced.guhs.event.KeepOnDeathHandler.onRespawnCopy(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(
                respawned, player, true));
        helper.assertTrue(respawned.getInventory().getItem(3).is(ModItems.VADS_PICKAXE.get()), "vads gear comes back in its slot");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void vadsPaxelIsPickaxeAxeAndShovel(GameTestHelper helper) {
        ItemStack paxel = new ItemStack(ModItems.VADS_PAXEL.get());
        for (Block block : new Block[]{Blocks.STONE, Blocks.OBSIDIAN, Blocks.OAK_LOG, Blocks.DIRT, Blocks.SAND,
                ModBlocks.COMPRESSED_SUPER_VAHOEGE_VADS.get()}) {
            helper.assertTrue(paxel.getDestroySpeed(block.defaultBlockState()) > 1f, "the paxel should dig " + block);
            helper.assertTrue(paxel.isCorrectToolForDrops(block.defaultBlockState()), "the paxel should harvest " + block);
        }
        helper.assertTrue(paxel.has(DataComponents.UNBREAKABLE), "the paxel never breaks");
        helper.assertTrue(paxel.is(nl.juiced.guhs.event.KeepOnDeathHandler.KEEP_ON_DEATH), "the paxel is kept on death");
        helper.succeed();
    }

    @GuhTest(template = WIRE_ROOM, timeoutTicks = 200)
    public static void secretNoteGuhWalksUpAndHandsOverBork(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(3.5, 1, 1.5)));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(14, 1, 1));
        guh.setSecretNote(true);
        helper.succeedWhen(() -> {
            helper.assertFalse(guh.hasSecretNote(), "the guh should have handed over its note");
            ItemStack note = player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(Items.PAPER)).findFirst().orElse(ItemStack.EMPTY);
            helper.assertFalse(note.isEmpty(), "the player should get a paper");
            helper.assertTrue(note.get(DataComponents.LORE).lines().get(0).getString().equals("bork"), "the note says bork");
        });
    }

    @GuhTest(template = "giant_kaasknabbel", timeoutTicks = 40)
    public static void importedKaasknabbelKeepsItsSign(GameTestHelper helper) {
        // the build from the "Guh structures" world: sign at world (-14, -59, 23) -> template (22, 2, 5) -> test (22, 3, 5)
        helper.succeedWhen(() -> {
            var sign = helper.getBlockEntity(new BlockPos(22, 3, 5));
            helper.assertTrue(sign instanceof net.minecraft.world.level.block.entity.SignBlockEntity, "the sign should be there");
            String line = ((net.minecraft.world.level.block.entity.SignBlockEntity) sign).getFrontText().getMessage(1, false).getString();
            helper.assertTrue(line.equals("Ik had zn honger"), "the sign should keep its text: " + line);
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Guh variants
    // ------------------------------------------------------------------------------------------------------------

    private static String nameKey(net.minecraft.network.chat.Component name) {
        return name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr ? tr.getKey() : name.getString();
    }

    @GuhTest(template = EMPTY)
    public static void guhsOutsideTheGuhmensionAreNormalAndCalledGuh(GameTestHelper helper) {
        for (int i = 0; i < 400; i++) {
            GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
            guh.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(guh.blockPosition()), EntitySpawnReason.NATURAL, null);
            helper.assertTrue(guh.getVariant() == nl.juiced.guhs.entity.GuhVariant.NORMAL, "variants only spawn in the Guhmension");
            helper.assertFalse(guh.hasSecretNote(), "the note guh only spawns in the Guhmension");
            helper.assertTrue(guh.shouldShowName() && nameKey(guh.getDisplayName()).equals("entity.guhs.guh"), "a normal guh is called Guh");
            guh.discard();
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void variantsAreRareAndSurviveSaving(GameTestHelper helper) {
        var random = net.minecraft.util.RandomSource.create(42);
        int normal = 0;
        java.util.Set<nl.juiced.guhs.entity.GuhVariant> seen = java.util.EnumSet.noneOf(nl.juiced.guhs.entity.GuhVariant.class);
        for (int i = 0; i < 100_000; i++) {
            var v = nl.juiced.guhs.entity.GuhVariant.roll(random);
            seen.add(v);
            if (v == nl.juiced.guhs.entity.GuhVariant.NORMAL) normal++;
        }
        helper.assertTrue(normal > 85_000, "normal guhs should be by far the most common: " + normal);
        helper.assertTrue(seen.size() == java.util.Arrays.stream(nl.juiced.guhs.entity.GuhVariant.values()).filter(v -> v.weight > 0 || v == nl.juiced.guhs.entity.GuhVariant.NORMAL).count(), "every variant can turn up (except brococolief, the ender guh: Guh Peaks only, the Koningguh: castle only, the Wolkguh: floating islands only...): " + seen);
        helper.assertFalse(seen.contains(nl.juiced.guhs.entity.GuhVariant.BROCOCOLIEF), "brococolief only comes with the note");

        GuhEntity bronto = helper.spawn(ModEntities.GUH.get(), POS);
        bronto.setVariant(nl.juiced.guhs.entity.GuhVariant.BRONTOSAURUS);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        bronto.saveWithoutId(tag);
        GuhEntity copy = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        copy.load(tag);
        helper.assertTrue(copy.getVariant() == nl.juiced.guhs.entity.GuhVariant.BRONTOSAURUS, "the variant should be saved");
        helper.assertTrue(nameKey(copy.getDisplayName()).equals("entity.guhs.guh.brontosaurus"), "shows its variant name");
        copy.setCustomName(net.minecraft.network.chat.Component.literal("Henk"));
        helper.assertTrue(copy.getDisplayName().getString().equals("Henk"), "a name tag still wins");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void babiesLookLikeAParent(GameTestHelper helper) {
        GuhEntity mum = helper.spawn(ModEntities.GUH.get(), POS);
        GuhEntity dad = helper.spawn(ModEntities.GUH.get(), POS.east());
        mum.setVariant(nl.juiced.guhs.entity.GuhVariant.MINT);
        dad.setVariant(nl.juiced.guhs.entity.GuhVariant.BROCOCOLIEF);
        for (int i = 0; i < 20; i++) {
            GuhEntity baby = (GuhEntity) mum.getBreedOffspring(helper.getLevel(), dad);
            var v = baby.getVariant();
            helper.assertTrue(v == nl.juiced.guhs.entity.GuhVariant.MINT || v == nl.juiced.guhs.entity.GuhVariant.NORMAL,
                    "a baby is mint like mum, or normal (brococolief is one of a kind): " + v);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sitting still & personalities
    // ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = WIRE_ROOM, timeoutTicks = 120)
    public static void sittingGuhIgnoresKaasKnabbels(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(4.5, 1, 1.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(14, 1, 1));
        guh.tame(player);
        guh.setTeleportEnabled(false);
        guh.setOrderedToSit(true);
        guh.setInSittingPose(true);
        // measure sideways only, from when it has settled on the floor
        Vec3[] start = new Vec3[1];
        helper.runAfterDelay(10, () -> start[0] = guh.position());
        helper.runAfterDelay(100, () -> {
            double moved = guh.position().subtract(start[0]).horizontalDistance();
            helper.assertTrue(moved < 0.05, "a sitting guh should not move: " + moved);
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY)
    public static void personalitiesDoSomething(GameTestHelper helper) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        guh.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(guh.blockPosition()), EntitySpawnReason.NATURAL, null);
        helper.assertTrue(guh.hasPersonality(), "every guh gets a personality");
        guh.setPersonality(nl.juiced.guhs.entity.GuhPersonality.CHATTY);
        int chatty = guh.getAmbientSoundInterval();
        guh.setPersonality(nl.juiced.guhs.entity.GuhPersonality.LAZY);
        helper.assertTrue(guh.getAmbientSoundInterval() == chatty * 2, "chatty guhs talk twice as often");
        double lazy = guh.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        guh.setPersonality(nl.juiced.guhs.entity.GuhPersonality.PLAYFUL);
        helper.assertTrue(guh.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > lazy, "playful guhs are faster than lazy ones");
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        guh.saveWithoutId(tag);
        GuhEntity copy = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        copy.load(tag);
        helper.assertTrue(copy.getPersonality() == nl.juiced.guhs.entity.GuhPersonality.PLAYFUL, "the personality is saved");
        helper.succeed();
    }

    @GuhTest(template = WIRE_ROOM, timeoutTicks = 200)
    public static void vadsigGuhEatsKnabbelsOffTheFloor(GameTestHelper helper) {
        for (int x = 0; x < 14; x++) {                  // the wire room has no floor of its own
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.PINK_WOOL);
            }
        }
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 1));
        guh.setPersonality(nl.juiced.guhs.entity.GuhPersonality.VADSIG);
        var snack = helper.spawnItem(ModItems.KAAS_KNABBELS.get(), 9.5f, 1f, 1.5f);
        helper.succeedWhen(() -> helper.assertFalse(snack.isAlive(), "the vadsig guh should eat the knabbel"));
    }

    @GuhTest(template = "guhramid", timeoutTicks = 60)
    public static void guhramidHasItsMummyGoldenGuhAndTreasure(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            var guhs = helper.getEntities(ModEntities.GUH.get());
            helper.assertTrue(guhs.stream().anyMatch(g -> g.getVariant() == nl.juiced.guhs.entity.GuhVariant.GOLDEN && g.isOrderedToSit()),
                    "a golden guh sits in the secret room");
            helper.assertTrue(guhs.stream().anyMatch(g -> g.getVariant() == nl.juiced.guhs.entity.GuhVariant.SNOW && g.hasCustomName()),
                    "the mummy guh sits on the dais");
            helper.assertBlockPresent(Blocks.CHEST, new BlockPos(12, 2, 12));
            helper.assertBlockPresent(Blocks.CHEST, new BlockPos(20, 2, 6));
            helper.assertBlockPresent(Blocks.TNT, new BlockPos(20, 1, 23));
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Guh villages, clothes
    // ------------------------------------------------------------------------------------------------------------

    private static net.minecraft.world.entity.npc.Villager guhVillager(GameTestHelper helper, BlockPos pos, net.minecraft.world.entity.npc.VillagerProfession profession) {
        var villager = helper.spawn(net.minecraft.world.entity.EntityType.VILLAGER, pos);
        villager.setVillagerData(villager.getVillagerData().setType(nl.juiced.guhs.registry.ModVillagers.GUH.get()).setProfession(profession));
        return villager;
    }

    @GuhTest(template = EMPTY)
    public static void guhmensionBiomesHaveGuhVillagers(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
        for (String b : new String[]{"guh_fields", "kaas_flats", "guh_peaks", "mikas_biome"}) {
            var holder = biomes.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, Guhs.id(b)));
            helper.assertTrue(net.minecraft.world.entity.npc.VillagerType.byBiome(holder) == nl.juiced.guhs.registry.ModVillagers.GUH.get(),
                    "villagers born in " + b + " should be guh villagers");
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void guhProfessionsHaveTheirTrades(GameTestHelper helper) {
        var temmer = guhVillager(helper, POS, nl.juiced.guhs.registry.ModVillagers.VADS_TEMMER.get());
        helper.assertTrue(temmer.getOffers().stream().anyMatch(o -> o.getResult().is(ModItems.GUH_SPAWN_EGG.get())),
                "the vads temmer sells guh spawn eggs: " + temmer.getOffers().size());
        for (var profession : new net.minecraft.world.entity.npc.VillagerProfession[]{nl.juiced.guhs.registry.ModVillagers.GUH_KLEERMAKER.get(),
                nl.juiced.guhs.registry.ModVillagers.VADSSMID.get(), nl.juiced.guhs.registry.ModVillagers.HAMSTERBOUWER.get(),
                nl.juiced.guhs.registry.ModVillagers.MIKA_JAGER.get()}) {
            var villager = guhVillager(helper, POS.east(), profession);
            helper.assertFalse(villager.getOffers().isEmpty(), profession + " should have trades");
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void guhVillagersDropVanillaJobs(GameTestHelper helper) {
        var villager = guhVillager(helper, POS, net.minecraft.world.entity.npc.VillagerProfession.LIBRARIAN);
        helper.succeedWhen(() -> helper.assertTrue(villager.getVillagerData().getProfession() == net.minecraft.world.entity.npc.VillagerProfession.NONE,
                "a guh villager doesn't become a librarian"));
    }

    @GuhTest(template = WIRE_ROOM, timeoutTicks = 1200, batch = "guh_villager_baan")   // own batch: other tests' job blocks nearby
    public static void guhVillagerTakesAGuhJob(GameTestHelper helper) {
        // (this room has no floor of its own: the knabbelbak goes on the ground, at y 0)
        helper.setBlock(new BlockPos(6, 0, 1), ModBlocks.KNABBELBAK.get());
        var villager = guhVillager(helper, new BlockPos(3, 1, 1), net.minecraft.world.entity.npc.VillagerProfession.NONE);
        helper.succeedWhen(() -> helper.assertTrue(villager.getVillagerData().getProfession() == nl.juiced.guhs.registry.ModVillagers.VADS_TEMMER.get(),
                "the villager should become a vads temmer at the knabbelbak"));
    }

    @GuhTest(template = EMPTY)
    public static void guhsWearClothes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        guh.tame(player);
        // 2.9: a piece you haven't unlocked doesn't go on (hold right-click to unlock it first); an unlocked one goes on
        // straight away and the item stays yours
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PARTY_HAT.get()));
        guh.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(guh.getClothes(nl.juiced.guhs.entity.GuhClothes.Slot.HEAD) == null, "not unlocked yet: nothing goes on");
        nl.juiced.guhs.feature.kleding.KledingUnlocks.voegToe(player, nl.juiced.guhs.entity.GuhClothes.PARTY_HAT);
        guh.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(guh.getClothes(nl.juiced.guhs.entity.GuhClothes.Slot.HEAD) == nl.juiced.guhs.entity.GuhClothes.PARTY_HAT, "the party hat is on");
        helper.assertTrue(player.getMainHandItem().is(ModItems.PARTY_HAT.get()), "the hat item stays in the player's hand (it's an unlock)");
        guh.wear(nl.juiced.guhs.entity.GuhClothes.RED_BOWTIE);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        guh.saveWithoutId(tag);
        GuhEntity copy = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        copy.load(tag);
        helper.assertTrue(copy.getClothes(nl.juiced.guhs.entity.GuhClothes.Slot.NECK) == nl.juiced.guhs.entity.GuhClothes.RED_BOWTIE, "clothes are saved");
        var off = copy.takeOffClothes();
        helper.assertTrue(off.size() == 2 && !copy.isWearingClothes(), "taking clothes off tells what it wore: " + off);
        // guhs saved as the old "rain" variant now wear the rain outfit
        net.minecraft.nbt.CompoundTag old = new net.minecraft.nbt.CompoundTag();
        copy.saveWithoutId(old);
        old.putString("Variant", "rain");
        GuhEntity rain = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        rain.load(old);
        helper.assertTrue(rain.getVariant() == nl.juiced.guhs.entity.GuhVariant.NORMAL
                && rain.getClothes(nl.juiced.guhs.entity.GuhClothes.Slot.BODY) == nl.juiced.guhs.entity.GuhClothes.RAINCOAT, "old rain guhs wear a raincoat");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void mikaMepperHitsMikasHard(GameTestHelper helper) {
        MikaEntity mika = helper.spawn(ModEntities.MIKA.get(), POS);
        var husk = helper.spawn(net.minecraft.world.entity.EntityType.HUSK, POS.east());
        var mepper = ModItems.MIKA_MEPPER.get();
        var source = helper.getLevel().damageSources().generic();
        helper.assertTrue(mepper.getAttackDamageBonus(mika, 6f, source) == 24f, "five times the damage on Mika");
        helper.assertTrue(new ItemStack(mepper).has(DataComponents.UNBREAKABLE), "the Mika-mepper never breaks");
        helper.assertTrue(mepper.getAttackDamageBonus(husk, 6f, source) == 0f, "normal damage on others");
        helper.succeed();
    }

    @GuhTest(template = "guh_village/layout_a", timeoutTicks = 60)
    public static void guhVillageHasGuhVillagers(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            var villagers = helper.getEntities(net.minecraft.world.entity.EntityType.VILLAGER);
            helper.assertTrue(villagers.size() >= 9, "a guh village has at least 9 villagers: " + villagers.size());
            helper.assertTrue(villagers.stream().allMatch(v -> v.getVillagerData().getType() == nl.juiced.guhs.registry.ModVillagers.GUH.get()),
                    "all of them guh villagers");
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // 2.0: wardrobe + backpack, launching
    // ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void wardrobeBackpackKeepsItsContents(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        guh.tame(player);
        nl.juiced.guhs.feature.kleding.KledingUnlocks.ontgrendel(player, nl.juiced.guhs.entity.GuhClothes.GUH_BACKPACK);
        nl.juiced.guhs.feature.kleding.KledingUnlocks.ontgrendel(player, nl.juiced.guhs.entity.GuhClothes.SUNGLASSES);
        java.util.List<nl.juiced.guhs.entity.GuhClothes> outfit = new java.util.ArrayList<>(java.util.Collections.nCopies(6, null));
        outfit.set(nl.juiced.guhs.menu.GuhWardrobeMenu.index(nl.juiced.guhs.entity.GuhClothes.Slot.BACK), nl.juiced.guhs.entity.GuhClothes.GUH_BACKPACK);
        outfit.set(nl.juiced.guhs.menu.GuhWardrobeMenu.index(nl.juiced.guhs.entity.GuhClothes.Slot.EYES), nl.juiced.guhs.entity.GuhClothes.SUNGLASSES);
        nl.juiced.guhs.feature.kleding.KledingKast.kleed(player, guh, outfit);
        helper.assertTrue(guh.hasBackpack() && guh.getClothes(nl.juiced.guhs.entity.GuhClothes.Slot.EYES) == nl.juiced.guhs.entity.GuhClothes.SUNGLASSES,
                "the wardrobe dresses the guh from your unlocks");
        var wardrobe = new nl.juiced.guhs.menu.GuhWardrobeMenu(0, player.getInventory(), guh);
        helper.assertTrue(wardrobe.getSlot(nl.juiced.guhs.menu.GuhWardrobeMenu.PACK_START).mayPlace(new ItemStack(Items.DIAMOND)), "the backpack takes things");
        guh.getBackpack().setItem(3, new ItemStack(Items.DIAMOND, 5));
        java.util.List<nl.juiced.guhs.entity.GuhClothes> niets = java.util.Collections.nCopies(6, null);
        nl.juiced.guhs.feature.kleding.KledingKast.kleed(player, guh, niets);
        helper.assertTrue(guh.hasBackpack() && guh.getClothes(nl.juiced.guhs.entity.GuhClothes.Slot.EYES) == null,
                "a backpack with things in it stays on (the rest comes off)");
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        guh.saveWithoutId(tag);
        GuhEntity copy = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        copy.load(tag);
        helper.assertTrue(copy.getBackpack().getItem(3).getCount() == 5 && copy.hasBackpack(), "the backpack contents are saved");
        guh.getBackpack().removeItemNoUpdate(3);
        nl.juiced.guhs.feature.kleding.KledingKast.kleed(player, guh, niets);
        helper.assertTrue(!guh.hasBackpack() && !wardrobe.getSlot(nl.juiced.guhs.menu.GuhWardrobeMenu.PACK_START).mayPlace(new ItemStack(Items.DIAMOND)),
                "an empty backpack comes off; then nothing goes in");
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().noneMatch(st -> st.getItem() instanceof nl.juiced.guhs.item.GuhClothingItem),
                "nothing comes back as an item");
        helper.succeed();
    }

    @GuhTest(template = WIRE_ROOM, timeoutTicks = 400)
    public static void launchedGuhFliesUntilItHitsAWall(GameTestHelper helper) {
        for (int y = 1; y <= 2; y++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(new BlockPos(20, y, z), Blocks.PINK_WOOL);
            }
        }
        Player player = helper.makeMockServerPlayerInLevel();
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 1));
        guh.setGuhScale(1.5f);
        guh.tame(player);
        guh.equipSaddle(new ItemStack(Items.SADDLE), null);
        player.snapTo(guh.getX(), guh.getY(), guh.getZ(), -90f, 0f); // looking towards +x, the wall
        player.startRiding(guh, true);
        double startX = guh.getX();
        guh.onLaunchPressed(player);
        helper.assertTrue(guh.getLaunchState() == GuhEntity.LAUNCH_CHARGING, "right-click starts sucking in air");
        helper.runAfterDelay(GuhEntity.LAUNCH_CHARGE_TICKS + 5, () -> helper.assertTrue(
                guh.getLaunchState() == GuhEntity.LAUNCH_FLYING || guh.getX() > startX + 3, "after 4 seconds it shoots off"));
        helper.succeedWhen(() -> {
            helper.assertTrue(guh.getLaunchState() == GuhEntity.LAUNCH_NONE && guh.getLaunchCooldown() > 0, "it plofs against the wall");
            helper.assertTrue(guh.getX() > startX + 8, "it flew towards the wall: " + (guh.getX() - startX));
            helper.assertTrue(guh.getHealth() == guh.getMaxHealth(), "the plof doesn't hurt");
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // 2.0.0: the guh stomach, the questline, the Guhdex and the sled
    // ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void stomachIsBuiltAndPrivateStomachsStayClosed(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer owner = (net.minecraft.server.level.ServerPlayer) helper.makeMockServerPlayerInLevel();
        net.minecraft.server.level.ServerPlayer visitor = (net.minecraft.server.level.ServerPlayer) helper.makeMockServerPlayerInLevel();
        // (the headless test server has no datapack dimensions; in a real world the stomach gets built too)
        ServerLevel maagLevel = nl.juiced.guhs.world.MaagManager.level(helper.getLevel().getServer());
        nl.juiced.guhs.world.GuhWorldData.Maag maag = maagLevel != null ? nl.juiced.guhs.world.MaagManager.ensureMaag(maagLevel, owner)
                : nl.juiced.guhs.world.GuhWorldData.get(owner.level().getServer()).createMaag(owner.getUUID(), "owner");
        if (maagLevel != null) {
            BlockPos c = nl.juiced.guhs.world.MaagManager.center(maag.index);
            helper.assertTrue(maagLevel.getBlockState(c.offset(-3, 0, 0)).is(ModBlocks.MAAG_PORTAL.get()), "the 'to the mouth' portal is there");
            helper.assertTrue(maagLevel.getBlockState(c.offset(2, 0, 0)).is(ModBlocks.MAAG_PORTAL.get()), "the 'to the intestines' portal is there");
            helper.assertTrue(maagLevel.getBlockState(c.below()).is(ModBlocks.MAAGBODEM.get()), "it has a stomach floor");
        }
        helper.assertTrue(maag.size == 48, "a new stomach is 48 wide");
        helper.assertTrue(maag.mayVisit(visitor.getUUID()), "stomachs are public by default");
        helper.assertTrue(!maag.mayBuild(visitor.getUUID()) && maag.mayBuild(owner.getUUID()), "only the owner builds");
        maag.access = nl.juiced.guhs.world.GuhWorldData.Access.PRIVATE;
        helper.assertTrue(!maag.mayVisit(visitor.getUUID()) && maag.mayVisit(owner.getUUID()), "private: only the owner");
        maag.access = nl.juiced.guhs.world.GuhWorldData.Access.WHITELIST;
        maag.whitelist.put(visitor.getUUID(), new nl.juiced.guhs.world.GuhWorldData.WhitelistEntry("visitor", true));
        helper.assertTrue(maag.mayVisit(visitor.getUUID()) && maag.mayBuild(visitor.getUUID()), "whitelisted with build rights");
        maag.access = nl.juiced.guhs.world.GuhWorldData.Access.PUBLIC;
        maag.whitelist.clear();
        if (maagLevel != null) {
            helper.assertTrue(nl.juiced.guhs.world.MaagManager.grow(maagLevel, maag) && maag.size == 64, "the dentist can make it 64 wide");
        }
        leave(helper, owner, visitor);
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void vadsThreeTimesInARowFreesGuhbert(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(2.5, 1, 2.5)));
        nl.juiced.guhs.entity.MikaBaasEntity mika = helper.spawn(ModEntities.MIKA_BAAS.get(), POS);
        var p = nl.juiced.guhs.world.GuhWorldData.get(player.level().getServer()).player(player.getUUID());
        p.maagQuest = 1;
        nl.juiced.guhs.quest.GuhQuests.playRps(player, mika, nl.juiced.guhs.quest.GuhQuests.Rps.VADS);
        nl.juiced.guhs.quest.GuhQuests.playRps(player, mika, nl.juiced.guhs.quest.GuhQuests.Rps.STEEN);
        helper.assertTrue(p.rpsStreak == 0, "losing (Mika always counters steen) starts over");
        for (int i = 0; i < nl.juiced.guhs.quest.GuhQuests.RPS_WINS_NEEDED; i++) {
            nl.juiced.guhs.quest.GuhQuests.playRps(player, mika, nl.juiced.guhs.quest.GuhQuests.Rps.VADS);
        }
        helper.assertTrue(p.maagQuest == 2, "Guhbert is free: on to the lost cake");
        var guhberts = helper.getLevel().getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(4),
                g -> g.isTame() && g.isOwnedBy(player) && g.hasCustomName() && "Guhbert".equals(g.getCustomName().getString()));
        helper.assertTrue(guhberts.size() == 1, "Guhbert follows you");
        helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.count(player, ModItems.TAARTKRUIMELS.get()) == 1, "you get the cake crumbs");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void guhdexMilestoneGivesItsRewardOnce(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) helper.makeMockServerPlayerInLevel();
        var p = nl.juiced.guhs.world.GuhWorldData.get(player.level().getServer()).player(player.getUUID());
        nl.juiced.guhs.quest.GuhDex.claim(player, 0);
        helper.assertTrue(player.getInventory().isEmpty(), "not enough guhs seen yet");
        for (int i = 0; i < 5; i++) {
            p.seen.add(nl.juiced.guhs.quest.GuhDex.ENTRIES.get(i));
        }
        nl.juiced.guhs.quest.GuhDex.claim(player, 0);
        nl.juiced.guhs.quest.GuhDex.claim(player, 0);
        helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.count(player, ModItems.clothingItem(nl.juiced.guhs.entity.GuhClothes.HEART_GLASSES)) == 1,
                "5 seen: heart glasses, only once");
        leave(helper, player);
        helper.succeed();
    }

    /** Mock players stay in the world after a test; take them out so they don't confuse other tests (e.g. the note guh). */
    private static void leave(GameTestHelper helper, Player... players) {
        for (Player p : players) {
            helper.getLevel().removePlayerImmediately((net.minecraft.server.level.ServerPlayer) p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    private static nl.juiced.guhs.slee.SleePath.Piece rail(GameTestHelper helper, nl.juiced.guhs.slee.SleePath.Placement placement) {
        helper.assertTrue(nl.juiced.guhs.item.SleeRailItem.canPlace(helper.getLevel(), placement, null), "room for " + placement);
        nl.juiced.guhs.item.SleeRailItem.place(helper.getLevel(), placement);
        return nl.juiced.guhs.slee.SleePath.Piece.of(helper.getLevel(), placement.anchor());
    }

    /** Lays a track: the first piece at `start` facing north, then each next piece on the end of the one before. */
    private static java.util.List<nl.juiced.guhs.slee.SleePath.Piece> track(GameTestHelper helper, BlockPos start, Object... shapes) {
        java.util.List<nl.juiced.guhs.slee.SleePath.Piece> pieces = new java.util.ArrayList<>();
        for (Object o : shapes) {
            boolean down = o instanceof String str && str.startsWith("down");
            nl.juiced.guhs.slee.SleePath.Shape shape = !down ? (nl.juiced.guhs.slee.SleePath.Shape) o : "down".equals(o)
                    ? nl.juiced.guhs.slee.SleePath.Shape.SLOPE : nl.juiced.guhs.slee.SleePath.Shape.valueOf(((String) o).substring(5));
            nl.juiced.guhs.slee.SleePath.Placement placement;
            if (pieces.isEmpty()) {
                placement = nl.juiced.guhs.slee.SleePath.fresh(helper.absolutePos(start), net.minecraft.core.Direction.NORTH, shape);
            } else {
                nl.juiced.guhs.slee.SleePath.Piece last = pieces.get(pieces.size() - 1);
                // the loose end of the last piece is the one away from the piece before it
                Vec3 away = pieces.size() == 1 ? last.at(1).pos() : farEnd(last, pieces.get(pieces.size() - 2));
                placement = nl.juiced.guhs.slee.SleePath.attachTo(last, away, shape, down);
            }
            pieces.add(rail(helper, placement));
        }
        return pieces;
    }

    private static Vec3 farEnd(nl.juiced.guhs.slee.SleePath.Piece piece, nl.juiced.guhs.slee.SleePath.Piece before) {
        Vec3 a = piece.at(0).pos(), b = piece.at(1).pos();
        double da = Math.min(a.distanceToSqr(before.at(0).pos()), a.distanceToSqr(before.at(1).pos()));
        double db = Math.min(b.distanceToSqr(before.at(0).pos()), b.distanceToSqr(before.at(1).pos()));
        return da > db ? a : b;
    }

    private static nl.juiced.guhs.entity.GuhSleeEntity sledOn(GameTestHelper helper, nl.juiced.guhs.slee.SleePath.Piece piece) {
        nl.juiced.guhs.entity.GuhSleeEntity sled = ModEntities.GUH_SLEE.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 start = piece.at(0.05).pos();
        sled.snapTo(start);
        sled.putOn(piece, start, piece.at(0).heading());
        helper.getLevel().addFreshEntity(sled);
        return sled;
    }

    @GuhTest(template = SLED_ROOM, timeoutTicks = 200)
    public static void sledFollowsTheTrackAroundACurveAndStopsAtTheEnd(GameTestHelper helper) {
        var s = nl.juiced.guhs.slee.SleePath.Shape.STRAIGHT;
        var pieces = track(helper, new BlockPos(4, 1, 20), s, nl.juiced.guhs.slee.SleePath.Shape.CURVE_RIGHT, s, s);
        nl.juiced.guhs.entity.GuhSleeEntity sled = sledOn(helper, pieces.get(0));
        sled.setSpeed(3);
        sled.setRunning(true);
        helper.succeedWhen(() -> {
            helper.assertTrue(!sled.isRunning(), "stops at the end of the line");
            helper.assertTrue(sled.getPiece() != null && sled.getPiece().anchor().equals(pieces.get(3).anchor()), "on the last piece");
            helper.assertTrue(sled.getX() > pieces.get(0).at(0).pos().x + 8, "it went round the curve (east): " + sled.getX());
            helper.assertTrue(!sled.isForward(), "next time it goes back");
        });
    }

    @GuhTest(template = SLED_ROOM, timeoutTicks = 200)
    public static void sledGoesUpAndDownSlopes(GameTestHelper helper) {
        var s = nl.juiced.guhs.slee.SleePath.Shape.STRAIGHT;
        var pieces = track(helper, new BlockPos(6, 1, 23), s, nl.juiced.guhs.slee.SleePath.Shape.SLOPE, "down", s);
        nl.juiced.guhs.entity.GuhSleeEntity sled = sledOn(helper, pieces.get(0));
        double startY = sled.getY();
        double[] top = {startY};
        sled.setSpeed(2);
        sled.setRunning(true);
        helper.onEachTick(() -> top[0] = Math.max(top[0], sled.getY()));
        helper.succeedWhen(() -> {
            helper.assertTrue(!sled.isRunning() && sled.getPiece() != null && sled.getPiece().anchor().equals(pieces.get(3).anchor()),
                    "reaches the end of the track");
            helper.assertTrue(top[0] > startY + 3.5, "went up the hill: " + (top[0] - startY));
            helper.assertTrue(Math.abs(sled.getY() - startY) < 0.01, "and back down");
        });
    }

    // --- 2.1.0: the coaster pieces and the guh kermis ---------------------------------------------------------------

    private static final Object[] COASTER = {nl.juiced.guhs.slee.SleePath.Shape.STRAIGHT, nl.juiced.guhs.slee.SleePath.Shape.DROP,
            nl.juiced.guhs.slee.SleePath.Shape.STRAIGHT, "down_DROP", nl.juiced.guhs.slee.SleePath.Shape.STRAIGHT,
            nl.juiced.guhs.slee.SleePath.Shape.SPIRAL_RIGHT, nl.juiced.guhs.slee.SleePath.Shape.SPIRAL_LEFT,
            nl.juiced.guhs.slee.SleePath.Shape.STRAIGHT, "down_DROP", nl.juiced.guhs.slee.SleePath.Shape.JUMP,
            nl.juiced.guhs.slee.SleePath.Shape.STRAIGHT};

    @GuhTest(template = COASTER_ROOM)
    public static void coasterPiecesJoinUpBothWays(GameTestHelper helper) {
        var pieces = track(helper, new BlockPos(8, 1, 48), COASTER);
        // forwards from the first piece to the last, and back again, over the jump's gap too
        var at = pieces.get(0);
        boolean forward = true;
        for (int i = 1; i < pieces.size(); i++) {
            var next = nl.juiced.guhs.slee.SleePath.next(helper.getLevel(), at, forward);
            helper.assertTrue(next != null && next.piece().anchor().equals(pieces.get(i).anchor()), "piece " + i + " follows " + at.shape());
            at = next.piece();
            forward = next.forward();
        }
        helper.assertTrue(nl.juiced.guhs.slee.SleePath.next(helper.getLevel(), at, forward) == null, "the end of the line");
        for (int i = pieces.size() - 2; i >= 0; i--) {
            var back = nl.juiced.guhs.slee.SleePath.next(helper.getLevel(), at, !forward);
            helper.assertTrue(back != null && back.piece().anchor().equals(pieces.get(i).anchor()), "and back to piece " + i);
            at = back.piece();
            forward = !back.forward();
        }
        for (var p : pieces) { // the ends of every piece line up with the next one's
            helper.assertTrue(p.shape().length() > 3, p.shape() + " has a length");
        }
        helper.succeed();
    }

    @GuhTest(template = COASTER_ROOM, timeoutTicks = 400)
    public static void sledRidesTheWholeCoaster(GameTestHelper helper) {
        var pieces = track(helper, new BlockPos(8, 1, 48), COASTER);
        nl.juiced.guhs.entity.GuhSleeEntity sled = sledOn(helper, pieces.get(0));
        double startY = sled.getY();
        double[] top = {startY};
        boolean[] flew = {false};
        var jump = pieces.get(9);
        sled.setSpeed(3);
        sled.setRunning(true);
        helper.onEachTick(() -> {
            top[0] = Math.max(top[0], sled.getY());
            if (sled.getPiece() != null && sled.getPiece().anchor().equals(jump.anchor()) && sled.getY() > startY + 2.5) {
                flew[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!sled.isRunning() && sled.getPiece() != null && sled.getPiece().anchor().equals(pieces.get(pieces.size() - 1).anchor()),
                    "rides all the way to the end");
            helper.assertTrue(top[0] > startY + 7.5, "up the drop and the corkscrews: " + (top[0] - startY));
            helper.assertTrue(flew[0], "flies over the jump");
            helper.assertTrue(Math.abs(sled.getY() - startY) < 0.01, "and lands on the rails");
        });
    }

    @GuhTest(template = COASTER_ROOM, timeoutTicks = 600)
    public static void kermisLapGivesAKermisbon(GameTestHelper helper) {
        var s = nl.juiced.guhs.slee.SleePath.Shape.STRAIGHT;
        var c = nl.juiced.guhs.slee.SleePath.Shape.CURVE_RIGHT;
        var pieces = track(helper, new BlockPos(8, 1, 20), s, c, s, c, s, c, s, c);
        helper.assertTrue(nl.juiced.guhs.slee.SleePath.next(helper.getLevel(), pieces.get(7), true) != null, "the loop is closed");
        ((nl.juiced.guhs.block.entity.SleeRailBlockEntity) helper.getLevel().getBlockEntity(pieces.get(0).anchor())).setFinish(true);
        nl.juiced.guhs.entity.GuhSleeEntity sled = sledOn(helper, pieces.get(1));
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(sled.position());
        player.startRiding(sled, true);
        sled.setSpeed(3);
        sled.setRunning(true);
        helper.succeedWhen(() -> {
            int bonnen = nl.juiced.guhs.quest.GuhQuests.count(player, ModItems.KERMISBON.get());
            helper.assertTrue(bonnen >= 4, "first lap: 1 + 2 extra, second lap: 1 more (" + bonnen + ")");
            helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.count(player, ModItems.GUH_BALLON.get()) == 3, "the prize bag only once");
            leave(helper, player);
        });
    }

    @GuhTest(template = EMPTY)
    public static void enderGuhFliesAndLovesFriedKnabbels(GameTestHelper helper) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 2, 2));
        guh.setVariant(nl.juiced.guhs.entity.GuhVariant.ENDER);
        helper.assertTrue(guh.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation, "an ender guh flies");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 1));
        guh.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(guh.isTame(), "one fried knabbel tames it");
        GuhEntity normal = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 2, 4));
        helper.assertTrue(!(normal.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation), "other guhs walk");
        helper.succeed();
    }

    // --- 2.3.0: the guh castle -------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void emptyRoyalThroneGetsANewKing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.KONINGSTROON.get().defaultBlockState().setValue(nl.juiced.guhs.block.KoningsTroonBlock.ROYAL, true));
        var throne = (nl.juiced.guhs.block.KoningsTroonBlock.Entity) helper.getBlockEntity(pos);
        BlockPos abs = helper.absolutePos(pos);
        throne.setLastKing(helper.getLevel().getGameTime() - 100);
        throne.check(helper.getLevel(), abs, helper.getBlockState(pos));
        helper.assertTrue(kings(helper).isEmpty(), "not yet: the throne has only been empty for a moment");
        throne.setLastKing(helper.getLevel().getGameTime() - nl.juiced.guhs.block.KoningsTroonBlock.NEW_KING_AFTER - 1);
        throne.check(helper.getLevel(), abs, helper.getBlockState(pos));
        var kings = kings(helper);
        helper.assertTrue(kings.size() == 1, "after 3 days a new Koningguh sits on the throne");
        GuhEntity king = kings.get(0);
        helper.assertTrue(king.isInSittingPose() && king.getGuhScale() > 1.5f, "a big king, sitting");
        helper.assertTrue(king.getClothes(nl.juiced.guhs.entity.GuhClothes.Slot.HEAD) == nl.juiced.guhs.entity.GuhClothes.KONING_KROON
                && king.getClothes(nl.juiced.guhs.entity.GuhClothes.Slot.BODY) == nl.juiced.guhs.entity.GuhClothes.KONING_MANTEL
                && king.getClothes(nl.juiced.guhs.entity.GuhClothes.Slot.NECK) == nl.juiced.guhs.entity.GuhClothes.KONING_KETTING, "in his royal outfit");
        throne.check(helper.getLevel(), abs, helper.getBlockState(pos));
        helper.assertTrue(kings(helper).size() == 1, "one king at a time");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void gateGuardsOnlyLetFriendsIn(GameTestHelper helper) {
        var level = helper.getLevel();
        nl.juiced.guhs.entity.GuhNpcEntity[] guards = new nl.juiced.guhs.entity.GuhNpcEntity[2];
        // the guards stand outside the 5-deep room: keep their chunks ticking (the mock player doesn't load chunks)
        java.util.Set<net.minecraft.world.level.ChunkPos> forced = new java.util.HashSet<>();
        for (int i = 0; i < 2; i++) {
            net.minecraft.world.level.ChunkPos c = new net.minecraft.world.level.ChunkPos(helper.absolutePos(new BlockPos(1 + i * 6, 2, 8)));
            if (level.setChunkForced(c.x(), c.z(), true)) {
                forced.add(c);
            }
            level.getChunk(c.x(), c.z());
        }
        for (int i = 0; i < 2; i++) {
            guards[i] = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
            guards[i].setKind(nl.juiced.guhs.entity.GuhNpcEntity.Kind.POORTWACHTER);
            BlockPos p = helper.absolutePos(new BlockPos(1 + i * 6, 2, 8));
            guards[i].snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0f, 0f);    // looking out (+z): the castle is behind them (-z)
            level.addFreshEntity(guards[i]);
        }
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos inside = helper.absolutePos(new BlockPos(4, 2, 5));
        player.teleportTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
        // (the guards look every 5 ticks, once they tick: wait for it rather than counting ticks)
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(player.getZ() > inside.getZ() + 3, "a stranger is put back outside the gate: " + player.getZ()))
                .thenExecute(() -> {
                    nl.juiced.guhs.quest.KasteelPoort.makeFriend(player, guards[0], "word");
                    helper.assertTrue(nl.juiced.guhs.quest.KasteelPoort.isFriend(player), "njeg: a friend of the guhs");
                    player.teleportTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(player.getZ() < inside.getZ() + 1, "friends may pass");
                    leave(helper, player);
                })
                .thenSucceed();
    }

    @GuhTest(template = EMPTY)
    public static void guhPileIsANineByNinePainting(GameTestHelper helper) {
        var level = helper.getLevel();
        var variant = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PAINTING_VARIANT)
                .getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PAINTING_VARIANT, nl.juiced.guhs.Guhs.id("guh_stapel")));
        helper.assertTrue(variant.isPresent() && variant.get().value().width() == 9 && variant.get().value().height() == 9, "the guh pile is 9x9");
        helper.succeed();
    }

    /** Reisguhs (only in the Guhmension, so this runs on a real server: /test runall). */
    @GuhTest(template = EMPTY, required = false, timeoutTicks = 200)
    public static void reisguhsDiscoverRenameAndTravel(GameTestHelper helper) {
        ServerLevel guhmension = helper.getLevel().getServer().getLevel(ModDimensions.GUHMENSION);
        helper.assertTrue(guhmension != null, "the Guhmension exists");
        BlockPos a = new BlockPos(40, 250, 40), b = new BlockPos(120, 250, 40), portal = new BlockPos(200, 250, 40);
        for (BlockPos p : java.util.List.of(a, b, portal)) {
            guhmension.getChunk(p);
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    guhmension.setBlockAndUpdate(p.offset(dx, -1, dz), Blocks.PINK_WOOL.defaultBlockState());
                }
            }
        }
        var ra = nl.juiced.guhs.quest.Reisguh.place(guhmension, a, 0, "A");
        var rb = nl.juiced.guhs.quest.Reisguh.place(guhmension, b, 0, "B");
        net.minecraft.server.level.ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(guhmension,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "reiziger"));
        player.snapTo(a.getX() + 1.5, a.getY(), a.getZ() + 0.5);
        nl.juiced.guhs.quest.Reisguh.talk(ra, player);
        player.snapTo(b.getX() + 1.5, b.getY(), b.getZ() + 0.5);
        nl.juiced.guhs.quest.Reisguh.talk(rb, player);
        helper.assertTrue(nl.juiced.guhs.quest.Reisguh.discovered(player, ra.getUUID()) && nl.juiced.guhs.quest.Reisguh.discovered(player, rb.getUUID()),
                "right-clicking discovers them");
        nl.juiced.guhs.quest.Reisguh.action(rb, player, nl.juiced.guhs.quest.Reisguh.RENAME, "Guhstation");
        helper.assertTrue(rb.getReisName().equals("Guhstation"), "and renames them");
        BlockPos arrive = nl.juiced.guhs.quest.Reisguh.arrivalSpot(guhmension, ra.getUUID());
        helper.assertTrue(arrive != null && arrive.distSqr(ra.blockPosition()) < 16, "travelling lands you right next to the Reisguh: " + arrive);
        nl.juiced.guhs.quest.Reisguh.nearPortal(guhmension, portal);
        nl.juiced.guhs.quest.Reisguh.nearPortal(guhmension, portal);      // (building a portal and arriving both ask)
        helper.assertTrue(guhmension.getEntitiesOfClass(nl.juiced.guhs.entity.GuhNpcEntity.class, new net.minecraft.world.phys.AABB(portal).inflate(8),
                n -> n.getKind() == nl.juiced.guhs.entity.GuhNpcEntity.Kind.REISGUH).size() == 1, "just one Reisguh by a portal");
        helper.assertTrue(!guhmension.getEntitiesOfClass(nl.juiced.guhs.entity.GuhNpcEntity.class, new net.minecraft.world.phys.AABB(portal).inflate(5),
                n -> n.getKind() == nl.juiced.guhs.entity.GuhNpcEntity.Kind.REISGUH).isEmpty(), "a Reisguh within 5 blocks of a new portal");
        for (var n : guhmension.getEntitiesOfClass(nl.juiced.guhs.entity.GuhNpcEntity.class, new net.minecraft.world.phys.AABB(a).inflate(300))) {
            n.discard();
        }
        helper.succeed();
    }

    /** Every structure in the super compass: the compass finds the same one as /locate (dev server: /test runall). */
    @GuhTest(template = EMPTY, required = false, timeoutTicks = 2400)
    public static void superkompasMatchesLocate(GameTestHelper helper) {
        ServerLevel guhmension = helper.getLevel().getServer().getLevel(ModDimensions.GUHMENSION);
        helper.assertTrue(guhmension != null, "the Guhmension exists");
        BlockPos from = new BlockPos(1500, 80, -900);
        StringBuilder report = new StringBuilder();
        for (var category : nl.juiced.guhs.item.SuperkompasItem.CATEGORIES) {
            for (String id : category.structures()) {
                var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, nl.juiced.guhs.Guhs.id(id));
                BlockPos ours = nl.juiced.guhs.item.GuhCompassItem.findCenter(guhmension, key, from);
                var holder = guhmension.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getHolderOrThrow(key);
                var vanilla = guhmension.getChunkSource().getGenerator().findNearestMapStructure(guhmension,
                        net.minecraft.core.HolderSet.direct(holder), from, 100, false);
                if (ours == null && vanilla == null) {
                    continue;   // (not in this dimension at all, e.g. the structures of the Barbecuether)
                }
                if (ours == null || vanilla == null) {
                    report.append(id).append(": ours=").append(ours).append(" locate=").append(vanilla == null ? null : vanilla.getFirst()).append("; ");
                    continue;
                }
                // (the compass looks at every possible spot by distance, so it may find a nearer one than /locate does)
                double dOurs = Math.hypot(ours.getX() - from.getX(), ours.getZ() - from.getZ());
                double dLocate = Math.hypot(vanilla.getFirst().getX() - from.getX(), vanilla.getFirst().getZ() - from.getZ());
                if (dOurs > dLocate + 160) {
                    report.append(id).append(": compass ").append((int) dOurs).append(" blocks, /locate ").append((int) dLocate).append("; ");
                }
            }
        }
        org.slf4j.LoggerFactory.getLogger("guhs").info("superkompas check: {}", report.length() == 0 ? "all agree" : report);
        helper.assertTrue(report.length() == 0, report.toString());
        helper.succeed();
    }

    @GuhTest(template = EMPTY, timeoutTicks = 60)
    public static void twoReisguhsOnOneSpotBecomeOne(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int i = 0; i < 2; i++) {
            var npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            npc.setKind(nl.juiced.guhs.entity.GuhNpcEntity.Kind.REISGUH);
            npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            helper.getLevel().addFreshEntity(npc);
        }
        helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getEntitiesOfClass(nl.juiced.guhs.entity.GuhNpcEntity.class,
                new net.minecraft.world.phys.AABB(at).inflate(2), n -> !n.isRemoved()).size() == 1, "one of the two goes"));
    }

    @GuhTest(template = EMPTY)
    public static void superkompasLooksForWhatYouChoose(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.SUPERKOMPAS.get());
        helper.assertTrue(nl.juiced.guhs.item.SuperkompasItem.chosen(stack) == null, "a new super compass looks for nothing yet");
        helper.assertTrue(nl.juiced.guhs.item.SuperkompasItem.allowed("guh_kasteel") && nl.juiced.guhs.item.SuperkompasItem.allowed("verstopguh_huis"),
                "castles and minigames are in it");
        helper.assertTrue(!nl.juiced.guhs.item.SuperkompasItem.allowed("block_guh") && !nl.juiced.guhs.item.SuperkompasItem.allowed("quartz_statue"),
                "filler builds are not");
        helper.assertTrue(!nl.juiced.guhs.item.SuperkompasItem.allowed("cheese_fountain") && !nl.juiced.guhs.item.SuperkompasItem.allowed("guh_statue"),
                "nor the fountains and the statue");
        nl.juiced.guhs.item.SuperkompasItem.choose(stack, "guh_picnic");
        helper.assertTrue("guh_picnic".equals(nl.juiced.guhs.item.SuperkompasItem.chosen(stack)), "it remembers the choice");
        var registry = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for (var category : nl.juiced.guhs.item.SuperkompasItem.CATEGORIES) {
            for (String id : category.structures()) {
                helper.assertTrue(registry.containsKey(nl.juiced.guhs.Guhs.id(id)), "every structure in the menu exists: " + id);
            }
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void guhdexHasTheReisguhAndTheGateGuard(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        var npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(nl.juiced.guhs.entity.GuhNpcEntity.Kind.REISGUH);
        npc.snapTo(at.getX() + 1.2, at.getY(), at.getZ() + 0.5);
        helper.getLevel().addFreshEntity(npc);
        var data = nl.juiced.guhs.world.GuhWorldData.get(helper.getLevel().getServer());
        nl.juiced.guhs.quest.GuhDex.onPlayerTick(player, data);
        helper.assertTrue(data.player(player.getUUID()).seen.contains(nl.juiced.guhs.entity.GuhVariant.REISGUH), "the Reisguh is in the Guhdex");
        helper.assertTrue(nl.juiced.guhs.quest.GuhDex.ENTRIES.contains(nl.juiced.guhs.entity.GuhVariant.POORTWACHTER)
                && !nl.juiced.guhs.quest.GuhDex.TAMEABLE.contains(nl.juiced.guhs.entity.GuhVariant.POORTWACHTER), "the gate guard too (seen, not tamed)");
        leave(helper, player);
        helper.succeed();
    }

    private static java.util.List<GuhEntity> kings(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(GuhEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(10),
                g -> g.getVariant() == nl.juiced.guhs.entity.GuhVariant.KONING);
    }

    @GuhTest(template = EMPTY)
    public static void koningguhIsTamedWithKnabbelsAndKeepsHisOutfit(GameTestHelper helper) {
        GuhEntity king = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 2, 2));
        nl.juiced.guhs.block.KoningsTroonBlock.makeKing(king);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < 60 && !king.isTame(); i++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get()));
            king.mobInteract(player, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(king.isTame(), "kaas knabbels tame him, like any guh");
        helper.assertTrue(king.getVariant() == nl.juiced.guhs.entity.GuhVariant.KONING
                && king.getClothes(nl.juiced.guhs.entity.GuhClothes.Slot.HEAD) == nl.juiced.guhs.entity.GuhClothes.KONING_KROON, "still the king, crown and all");
        helper.succeed();
    }

    // --- 2.2.0: verstopguh ------------------------------------------------------------------------------------------

    private static nl.juiced.guhs.entity.GuhNpcEntity verstopguhtje(GameTestHelper helper) {
        // this test's own house only: the template is 80 wide, Verstopguhtje sits in the middle of its roof
        var npcs = helper.getLevel().getEntitiesOfClass(nl.juiced.guhs.entity.GuhNpcEntity.class,
                net.minecraft.world.phys.AABB.encapsulatingFullBlocks(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(80, 40, 80))),
                n -> n.getKind() == nl.juiced.guhs.entity.GuhNpcEntity.Kind.VERSTOPGUHTJE);
        helper.assertTrue(npcs.size() == 1, "Verstopguhtje is on the roof");
        return npcs.get(0);
    }

    /** The guhs hidden by this test's own Verstopguhtje (another verstop test may run right next to it). */
    private static java.util.List<GuhEntity> hiddenGuhs(GameTestHelper helper) {
        java.util.UUID npc = verstopguhtje(helper).getUUID();
        return helper.getLevel().getEntitiesOfClass(GuhEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(100),
                g -> npc.equals(g.getHiddenBy()));
    }

    @GuhTest(template = "verstopguh_huis", timeoutTicks = 200)
    public static void verstopguhFindThemAllForTickets(GameTestHelper helper) {
        var npc = verstopguhtje(helper);
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(npc.getX() + 1, npc.getY(), npc.getZ());
        nl.juiced.guhs.quest.VerstopGame.action(npc, player, nl.juiced.guhs.quest.VerstopGame.START); // makkelijk
        helper.assertTrue(npc.verstop.isRunning() && npc.verstop.isPlaying(player), "the game is on");
        var guhs = hiddenGuhs(helper);
        helper.assertTrue(guhs.size() == 5, "5 guhs hidden on easy: " + guhs.size());
        helper.assertTrue(Math.abs(guhs.get(0).getBbWidth() - guhs.get(0).getBbWidth()) < 1e-6 && player.getY() < npc.getY() - 3, "the seeker is inside");
        helper.assertTrue(!guhs.get(0).shouldShowName(), "hidden guhs don't show a name through walls");
        player.getFoodData().setFoodLevel(2);
        player.hurt(helper.getLevel().damageSources().fall(), 5f);
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "seekers can't get hurt");
        helper.assertTrue(nl.juiced.guhs.quest.VerstopGame.isSeeking(player), "the player is seeking");
        for (int i = 0; i < 3; i++) {                     // chat, lights out, a sniffer
            npc.verstop.giveHint(npc, helper.getLevel());
        }
        helper.assertTrue(npc.verstop.lampsOff() > 0, "a room without guhs went dark (its lamps and windows)");
        helper.assertTrue(npc.verstop.sniffers() == 1, "one guh keeps sniffing");
        for (GuhEntity guh : guhs) {
            guh.mobInteract(player, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(!npc.verstop.isRunning(), "all found: the game is over");
        helper.assertTrue(!nl.juiced.guhs.quest.VerstopGame.isSeeking(player), "and no longer protected");
        helper.assertTrue(npc.verstop.lampsOff() == 0, "the lights are back on");
        helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.count(player, ModItems.VERSTOPGUHTICKET.get()) == 4, "2 tickets + 2 for being quick");
        helper.assertTrue(nl.juiced.guhs.quest.VerstopGame.best(player, nl.juiced.guhs.quest.VerstopGame.Level.MAKKELIJK) >= 0, "a record");
        helper.assertTrue(player.getY() >= npc.getY() - 1, "back on the roof");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = "verstopguh_huis", timeoutTicks = 200)
    public static void verstopguhWalkingOutEndsTheGame(GameTestHelper helper) {
        var npc = verstopguhtje(helper);
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(npc.getX() + 1, npc.getY(), npc.getZ());
        nl.juiced.guhs.quest.VerstopGame.action(npc, player, nl.juiced.guhs.quest.VerstopGame.START + 2); // moeilijk
        var guhs = hiddenGuhs(helper);
        helper.assertTrue(guhs.size() == 12, "12 tiny guhs on hard: " + guhs.size());
        helper.assertTrue(guhs.get(0).getBbWidth() < 0.4, "they're small: " + guhs.get(0).getBbWidth());
        // (only this test's own house: a neighbouring verstopguh test has a Tipguh of its own)
        var tipguhs = helper.getLevel().getEntitiesOfClass(nl.juiced.guhs.entity.GuhNpcEntity.class,
                helper.getBounds().inflate(2), n -> n.getKind() == nl.juiced.guhs.entity.GuhNpcEntity.Kind.TIPGUH);
        helper.assertTrue(tipguhs.size() == 1, "there is one Tipguh in the hall");
        nl.juiced.guhs.quest.VerstopGame.askTip(tipguhs.get(0), player);
        helper.assertTrue(!npc.verstop.isTipped() && guhs.stream().noneMatch(g -> g.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)),
                "the first click only warns");
        nl.juiced.guhs.quest.VerstopGame.askTip(tipguhs.get(0), player);
        helper.assertTrue(npc.verstop.isTipped() && guhs.stream().filter(g -> g.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)).count() == 1,
                "the second click lights up one guh");
        nl.juiced.guhs.quest.VerstopGame.askTip(tipguhs.get(0), player);
        helper.assertTrue(guhs.stream().filter(g -> g.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)).count() == 1, "and then wait a minute");
        nl.juiced.guhs.quest.VerstopGame.walkOut(player, player.blockPosition());
        helper.assertTrue(!npc.verstop.isRunning(), "the last seeker walked out: game over");
        helper.assertTrue(hiddenGuhs(helper).stream().allMatch(g -> g.isRemoved()) || hiddenGuhs(helper).isEmpty(), "the hidden guhs went home");
        helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.count(player, ModItems.VERSTOPGUHTICKET.get()) == 0, "no tickets for giving up");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void oneWayGlassHoldsYouUp(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.EENRICHTINGSGLAS.get());
        var state = helper.getBlockState(new BlockPos(2, 1, 2));
        helper.assertTrue(state.isCollisionShapeFullBlock(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))), "you can walk on it");
        helper.assertTrue(!state.canOcclude(), "you can look through it");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void kermisGuhSellsTheOutfitForBonnen(GameTestHelper helper) {
        nl.juiced.guhs.entity.GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 2, 2));
        npc.setKind(nl.juiced.guhs.entity.GuhNpcEntity.Kind.KERMIS_GUH);
        var results = npc.getOffers().stream().map(o -> o.getResult().getItem()).toList();
        for (var piece : java.util.List.of(nl.juiced.guhs.entity.GuhClothes.KERMIS_HOED, nl.juiced.guhs.entity.GuhClothes.KERMIS_JASJE,
                nl.juiced.guhs.entity.GuhClothes.KERMIS_STRIK)) {
            helper.assertTrue(results.contains(ModItems.clothingItem(piece)), "sells " + piece);
        }
        helper.assertTrue(npc.getOffers().stream().allMatch(o -> o.getCostA().is(ModItems.KERMISBON.get())), "for kermisbonnen");
        helper.succeed();
    }

    /** The real guh kermis: its station sleds find the rails, and a whole lap round the coaster gives kermisbonnen. */
    @GuhTest(template = "guh_kermis", timeoutTicks = 1200)
    public static void kermisCoasterGoesAllTheWayRound(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        nl.juiced.guhs.entity.GuhSleeEntity[] sled = {null};
        helper.runAfterDelay(30, () -> {
            // (2.9: only the sleds of this test's own kermis - with -Pgt the other tests' templates can stand closer by)
            var sleds = helper.getLevel().getEntitiesOfClass(nl.juiced.guhs.entity.GuhSleeEntity.class, helper.getBounds().inflate(1));
            helper.assertTrue(sleds.size() == 2 && sleds.stream().allMatch(sl -> sl.getPiece() != null), "both station sleds are on the rails: "
                    + sleds.size() + " " + sleds.stream().map(sl -> sl.getPiece() != null).toList());
            sled[0] = sleds.get(0);
            player.snapTo(sled[0].position());
            player.startRiding(sled[0], true);
            sled[0].setSpeed(3);
            sled[0].setRunning(true);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(sled[0] != null && sled[0].isRunning(), "the sled keeps going (the track is a closed loop)");
            helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.count(player, ModItems.KERMISBON.get()) >= 3, "a lap gives kermisbonnen");
            leave(helper, player);
        });
    }

    @GuhTest(template = EMPTY)
    public static void kermisStructureHasItsCoasterStallAndSleds(GameTestHelper helper) {
        var template = helper.getLevel().getStructureManager().get(nl.juiced.guhs.Guhs.id("guh_kermis")).orElseThrow();
        var tag = template.save(new net.minecraft.nbt.CompoundTag());
        String entities = tag.getListOrEmpty("entities").toString();
        helper.assertTrue(entities.contains("kermis_guh"), "the Kermis-guh is there");
        helper.assertTrue(entities.split("guhs:guh_slee").length - 1 == 2, "two sleds at the station");
        String blocks = tag.getListOrEmpty("blocks").toString();
        helper.assertTrue(blocks.contains("Finish"), "the station is the finish line");
        helper.succeed();
    }

    @GuhTest(template = SLED_ROOM)
    public static void breakingAnyPartBreaksTheWholeRailPiece(GameTestHelper helper) {
        var piece = track(helper, new BlockPos(6, 1, 10), nl.juiced.guhs.slee.SleePath.Shape.SLOPE).get(0);
        var blocks = new nl.juiced.guhs.slee.SleePath.Placement(piece.anchor(), piece.facing(), piece.shape()).blocks();
        helper.assertTrue(blocks.size() == 16, "a piece is 16 blocks");
        helper.getLevel().destroyBlock(blocks.get(9), true);
        for (BlockPos pos : blocks) {
            helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "all of it is gone: " + pos);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // 2.0.0: bees, slimes, Nether Mikas, crystals, food and furniture
    // ------------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void guhBeesNeverGetAngry(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        nl.juiced.guhs.entity.GuhBeeEntity bee = helper.spawn(ModEntities.GUH_BEE.get(), new BlockPos(2, 2, 2));
        bee.hurt(helper.getLevel().damageSources().playerAttack(player), 1f);
        bee.setTarget(player);
        helper.assertTrue(!bee.isAngry() && bee.getTarget() == null, "a guh bee stays friendly, even when hit");
        helper.succeed();
    }

    /** Far from the world's middle, a compass finds the same nearest structure as /locate (and not one near 0,0). */
    @GuhTest(template = EMPTY, timeoutTicks = 400, required = false)
    public static void compassFindsNearestStructureFarAway(GameTestHelper helper) {
        ServerLevel guhmension = helper.getLevel().getServer().getLevel(ModDimensions.GUHMENSION);
        helper.assertTrue(guhmension != null, "the Guhmension exists");
        for (var key : java.util.List.of(nl.juiced.guhs.item.GuhCompassItem.MIKA_KAMP, nl.juiced.guhs.item.GuhCompassItem.GUH_CAVES)) {
            BlockPos from = new BlockPos(24000, 64, -17000);
            BlockPos ours = nl.juiced.guhs.item.GuhCompassItem.findCenter(guhmension, key, from);
            var holder = guhmension.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getHolderOrThrow(key);
            var vanilla = guhmension.getChunkSource().getGenerator().findNearestMapStructure(guhmension,
                    net.minecraft.core.HolderSet.direct(holder), from, 100, false);
            helper.assertTrue(ours != null && vanilla != null, key.identifier() + " is found far from 0,0");
            double dOurs = Math.hypot(ours.getX() - from.getX(), ours.getZ() - from.getZ());
            double dVanilla = Math.hypot(vanilla.getFirst().getX() - from.getX(), vanilla.getFirst().getZ() - from.getZ());
            helper.assertTrue(dOurs <= dVanilla + 96, key.identifier() + " compass: " + (int) dOurs + " blocks, /locate: " + (int) dVanilla);
        }
        helper.succeed();
    }

    /** Guh bees spawn with new chunks on a worldgen thread: they must not touch the world's random there. */
    @GuhTest(template = EMPTY)
    public static void guhBeeMadeOffThreadGetsGoalsLater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        nl.juiced.guhs.entity.GuhBeeEntity[] made = new nl.juiced.guhs.entity.GuhBeeEntity[1];
        Thread worker = new Thread(() -> made[0] = ModEntities.GUH_BEE.get().create(level, EntitySpawnReason.TRIGGERED));
        worker.start();
        try {
            worker.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        nl.juiced.guhs.entity.GuhBeeEntity bee = made[0];
        helper.assertTrue(bee != null && bee.goalSelector.getAvailableGoals().isEmpty(), "no goals yet off the server thread");
        BlockPos abs = helper.absolutePos(new BlockPos(2, 2, 2));
        bee.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        level.addFreshEntity(bee);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!bee.goalSelector.getAvailableGoals().isEmpty(), "the bee gets its goals on its first tick");
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY)
    public static void fullKnabbelkorfGivesKnabbelsAndKaashoning(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.KNABBELKORF.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, 5));
        helper.assertTrue(helper.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BeehiveBlockEntity,
                "the knabbelkorf is a real beehive");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        BlockPos abs = helper.absolutePos(pos);
        helper.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false));
        helper.assertTrue(helper.getBlockState(pos).getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) == 0, "emptied");
        helper.assertItemEntityPresent(ModItems.KAAS_KNABBELS.get(), pos, 2);
        helper.setBlock(pos, helper.getBlockState(pos).setValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, 5));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        helper.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false));
        helper.assertTrue(player.getMainHandItem().is(ModItems.KAASHONING.get()), "a bottle gives kaashoning");
        helper.succeed();
    }

    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void guhSlimesArePeaceful(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.snapTo(helper.absoluteVec(new Vec3(2.5, 1, 2.5)));
        nl.juiced.guhs.entity.GuhSlimeEntity slime = helper.spawn(ModEntities.GUH_SLIME.get(), new BlockPos(2, 1, 3));
        slime.setSize(4, true);
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(player.getHealth() == player.getMaxHealth(), "a big guh slime right next to you doesn't hurt");
            helper.assertTrue(slime.getType().getCategory() == net.minecraft.world.entity.MobCategory.CREATURE, "it's a creature");
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY)
    public static void pinkSlimeBlockIsStickyAndNetherMikasDontBurn(GameTestHelper helper) {
        var state = ModBlocks.ROZE_SLIJMBLOK.get().defaultBlockState();
        helper.assertTrue(state.isStickyBlock() && state.isSlimeBlock(), "sticky for pistons, bouncy like slime");
        helper.assertTrue(ModEntities.NETHER_MIKA.get().fireImmune(), "Nether Mikas are fire-proof");
        helper.assertTrue(!ModEntities.MIKA.get().fireImmune(), "normal Mikas aren't");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void guhCrystalsDropCrystalsAndFishFryInThePan(GameTestHelper helper) {
        var drops = net.minecraft.world.level.block.Block.getDrops(ModBlocks.GUH_KRISTAL_CLUSTER.get().defaultBlockState(), helper.getLevel(),
                helper.absolutePos(POS), null);
        helper.assertTrue(drops.stream().anyMatch(s -> s.is(ModItems.GUH_KRISTAL.get())), "a crystal cluster drops guh crystals");
        helper.setBlock(POS, ModBlocks.FRYING_PAN.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MIKA_VET.get()));
        BlockPos abs = helper.absolutePos(POS);
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
        helper.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUH_VIS.get(), 3));
        helper.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(player.getInventory().countItem(ModItems.GEBAKKEN_GUH_VIS.get()) == 3, "3 fried guh fish");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void youCanSitOnAGuhChair(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.GUH_STOEL.get());
        Player player = helper.makeMockServerPlayerInLevel();
        BlockPos abs = helper.absolutePos(POS);
        player.snapTo(abs.getX() + 0.5, abs.getY() + 1, abs.getZ() + 1.5);
        helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), player,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false));
        helper.assertTrue(player.getVehicle() instanceof nl.juiced.guhs.entity.GuhSeatEntity, "sitting on the chair");
        helper.assertTrue(Math.abs(player.getVehicle().getY() - (abs.getY() + 0.5)) < 0.01, "at seat height");
        player.stopRiding();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(nl.juiced.guhs.entity.GuhSeatEntity.class, new net.minecraft.world.phys.AABB(abs).inflate(2)).isEmpty(),
                    "the seat disappears when you get up");
            leave(helper, player);
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY)
    public static void guhCakeHasSevenBitesAndMilkshakeClearsEffects(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.GUH_TAART.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        BlockPos abs = helper.absolutePos(POS);
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
        for (int i = 0; i < 7; i++) {
            helper.assertBlockPresent(ModBlocks.GUH_TAART.get(), POS);
            helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), player, hit);
            player.getFoodData().setFoodLevel(0);
        }
        helper.assertBlockNotPresent(ModBlocks.GUH_TAART.get(), POS);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200));
        ItemStack shake = new ItemStack(ModItems.KAASKNABBEL_MILKSHAKE.get());
        ItemStack rest = shake.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.getActiveEffects().isEmpty(), "the milkshake clears effects");
        helper.assertTrue(rest.is(Items.GLASS_BOTTLE), "and leaves the bottle");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void ripeKaasknabbelPlantsGiveKnabbelsAndFlowersFitInPots(GameTestHelper helper) {
        var ripe = ModBlocks.KAASKNABBELPLANT.get().defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7);
        var drops = net.minecraft.world.level.block.Block.getDrops(ripe, helper.getLevel(), helper.absolutePos(POS), null);
        helper.assertTrue(drops.stream().anyMatch(s -> s.is(ModItems.KAAS_KNABBELS.get())), "ripe plants give kaas knabbels");
        helper.assertTrue(drops.stream().anyMatch(s -> s.is(ModItems.KAASKNABBELZAADJES.get())), "and seeds");
        var young = ModBlocks.KAASKNABBELPLANT.get().defaultBlockState();
        helper.assertTrue(net.minecraft.world.level.block.Block.getDrops(young, helper.getLevel(), helper.absolutePos(POS), null).stream()
                .noneMatch(s -> s.is(ModItems.KAAS_KNABBELS.get())), "young plants only give their seed back");
        var pots = ((net.minecraft.world.level.block.FlowerPotBlock) Blocks.FLOWER_POT).getFullPotsView();
        helper.assertTrue(pots.containsKey(ModBlocks.KAASBLOEM.getId()), "a kaasbloem fits in a flower pot");
        helper.succeed();
    }

    @GuhTest(template = SLED_ROOM)
    public static void guhBlossomSaplingStandsOnPinkWool(GameTestHelper helper) {
        for (int x = 8; x <= 16; x++) {
            for (int z = 8; z <= 16; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.PINK_WOOL);
            }
        }
        BlockPos pos = new BlockPos(12, 1, 12);
        helper.setBlock(pos, ModBlocks.GUHBLOESEM_SAPLING.get());
        helper.assertTrue(helper.getBlockState(pos).canSurvive(helper.getLevel(), helper.absolutePos(pos)), "it can stand on pink wool");
        // (the tree itself is too big for a test room: it's checked in a real world with /place feature guhs:guhbloesem)
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, Guhs.id("guhbloesem"));
        helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                .getHolder(key).isPresent(), "the guh blossom tree feature is loaded");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void ftbQuestsChapterInstallsOnceAndMergesTheTexts(GameTestHelper helper) {
        try {
            java.nio.file.Path quests = java.nio.file.Files.createTempDirectory("guhs-ftbquests");
            java.nio.file.Path lang = quests.resolve("lang").resolve("en_us.snbt");
            java.nio.file.Files.createDirectories(lang.getParent());
            java.nio.file.Files.writeString(lang, "{\n\tchapter.0123456789ABCDEF.title: \"Other\"\n}\n");
            java.nio.file.Path groups = quests.resolve("chapter_groups.snbt");
            java.nio.file.Files.writeString(groups, "{\n\tchapter_groups: [\n\t\t{ id: \"0123456789ABCDEF\" }\n\t]\n}\n");
            helper.assertTrue(nl.juiced.guhs.compat.FtbQuestsChapter.installInto(quests), "installs the first time");
            java.util.List<String> names = nl.juiced.guhs.compat.FtbQuestsChapter.chapters();
            helper.assertTrue(names.equals(java.util.List.of("guhs_basis", "guhs_guhmensie", "guhs_minigames", "guhs_onderwater", "guhs_maag",
                    "guhs_guheinde", "guhs_barbecuether", "guhs_extra27", "guhs_knuffeldal", "guhs_piep", "guhs_band", "guhs_verhalen", "guhs_diertjes")),
                    "the thirteen chapters (3.0: Guhverhalen, Diertjes van de Guhmensie), in reading order: " + names);
            for (String name : names) {
                helper.assertTrue(java.nio.file.Files.exists(quests.resolve("chapters").resolve(name + ".snbt")), "chapter " + name + " is there");
            }
            String g = java.nio.file.Files.readString(groups);
            helper.assertTrue(g.contains("0123456789ABCDEF") && java.util.regex.Pattern.compile("id: \"475548[0-9A-F]{10}\"").matcher(g).find(),
                    "our group is added next to the pack's own: " + g);
            String text = java.nio.file.Files.readString(lang);
            helper.assertTrue(text.contains("Other") && text.contains("&dGuhs") && text.contains("Hoe kom je hier?") && text.trim().endsWith("}"),
                    "texts merged into the lang file");
            helper.assertTrue(!nl.juiced.guhs.compat.FtbQuestsChapter.installInto(quests), "not again when it's already there");
            helper.assertTrue(java.nio.file.Files.readString(lang).equals(text), "the lang file isn't touched again");
            helper.assertTrue(java.nio.file.Files.readString(groups).equals(g), "the group isn't added twice");
        } catch (java.io.IOException e) {
            helper.fail(e.toString());
        }
        helper.succeed();
    }

    /** Our old single chapter goes (it has the version marker); chapters a pack maker edited (no marker) stay as they are. */
    @GuhTest(template = EMPTY)
    public static void ftbQuestsReplacesOurOldChapterButKeepsPackEdits(GameTestHelper helper) {
        try {
            java.nio.file.Path quests = java.nio.file.Files.createTempDirectory("guhs-ftbquests");
            java.nio.file.Path chapters = quests.resolve("chapters");
            java.nio.file.Files.createDirectories(chapters);
            java.nio.file.Files.writeString(chapters.resolve("guhs.snbt"), "{\n\tguhs_chapter_version: 13\n\tfilename: \"guhs\"\n}\n");
            java.nio.file.Files.writeString(chapters.resolve("guhs_maag.snbt"), "{\n\tfilename: \"guhs_maag\"\n\tpack: \"edited\"\n}\n");
            java.nio.file.Files.writeString(chapters.resolve("guhs_basis.snbt"), "{\n\tguhs_chapter_version: 3\n}\n");
            helper.assertTrue(nl.juiced.guhs.compat.FtbQuestsChapter.installInto(quests), "installs");
            helper.assertTrue(!java.nio.file.Files.exists(chapters.resolve("guhs.snbt")), "our old single chapter is gone");
            helper.assertTrue(java.nio.file.Files.readString(chapters.resolve("guhs_maag.snbt")).contains("edited"), "the pack's edit stays");
            helper.assertTrue(java.nio.file.Files.readString(chapters.resolve("guhs_basis.snbt")).contains("quests: ["), "an older chapter of ours is updated");
            helper.assertTrue(java.nio.file.Files.exists(chapters.resolve("guhs_knuffeldal.snbt")), "the other chapters are installed");
            // an old single chapter the pack edited (no marker): nothing is installed next to it (same quest ids)
            java.nio.file.Path other = java.nio.file.Files.createTempDirectory("guhs-ftbquests");
            java.nio.file.Files.createDirectories(other.resolve("chapters"));
            java.nio.file.Files.writeString(other.resolve("chapters").resolve("guhs.snbt"), "{\n\tfilename: \"guhs\"\n}\n");
            helper.assertTrue(!nl.juiced.guhs.compat.FtbQuestsChapter.installInto(other), "leaves an edited old chapter alone");
            helper.assertTrue(java.nio.file.Files.exists(other.resolve("chapters").resolve("guhs.snbt"))
                    && !java.nio.file.Files.exists(other.resolve("chapters").resolve("guhs_basis.snbt")), "and adds nothing");
        } catch (java.io.IOException e) {
            helper.fail(e.toString());
        }
        helper.succeed();
    }

    /** FTB Quests re-saves our chapters without the marker: then the fingerprint tells an untouched chapter from a pack's edit. */
    private static void ftbQuestsTellsAResaveFromAnEdit(GameTestHelper helper) {
        try {
            String ours = ftbResource("ftbquests/chapters/guhs_basis.snbt");
            String resaved = ours.replaceAll("(?m)^[\\t ]*guhs_chapter_version: \\d+\\R", "");
            String fp = nl.juiced.guhs.compat.FtbQuestsChapter.fingerprint(ours);
            helper.assertTrue(!resaved.contains("guhs_chapter_version") && nl.juiced.guhs.compat.FtbQuestsChapter.fingerprint(resaved).equals(fp),
                    "the same ids and positions without the marker");
            helper.assertTrue(nl.juiced.guhs.compat.FtbQuestsChapter.shouldReplace(resaved, new String[] {"13", fp}, 14), "an older, untouched chapter is updated");
            helper.assertTrue(!nl.juiced.guhs.compat.FtbQuestsChapter.shouldReplace(resaved, new String[] {"14", fp}, 14), "the same version is left alone");
            String moved = resaved.replaceFirst("(?m)^(\\s*)x: ([-0-9.]+)d$", "$1x: 99.5d");
            helper.assertTrue(!nl.juiced.guhs.compat.FtbQuestsChapter.shouldReplace(moved, new String[] {"13", fp}, 14), "a moved quest is a pack's edit");
            helper.assertTrue(!nl.juiced.guhs.compat.FtbQuestsChapter.shouldReplace(resaved, null, 14), "without a record: not ours to replace");
            helper.assertTrue(nl.juiced.guhs.compat.FtbQuestsChapter.shouldReplace(ours.replace("guhs_chapter_version: 19", "guhs_chapter_version: 3"), null, 19),
                    "an older chapter with our marker is updated");
            helper.assertTrue(nl.juiced.guhs.compat.FtbQuestsChapter.oldChapterIsOurs("{\n\tid: \"4755487A3E56DBF4\"\n\tfilename: \"guhs\"\n}\n")
                    && !nl.juiced.guhs.compat.FtbQuestsChapter.oldChapterIsOurs("{\n\tid: \"4755487A3E56DBF4\"\n\tquests: [{ id: \"0123456789ABCDEF\" }]\n}\n"),
                    "the old single chapter is ours unless the pack added its own quests");
            // FTB Quests saves text lists over several lines: our old entries go completely, the pack's stay
            String saved = "{\n\tchapter.0123456789ABCDEF.title: \"Other\"\n\tquest.475548AAAAAAAAAA.quest_desc: [\n\t\t\"a\"\n\t\t\"\"\n\t\t\"b ]\"\n\t]\n"
                    + "\tquest.475548AAAAAAAAAA.title: \"T\"\n\tquest.475548BBBBBBBBBB.quest_desc: [\"one\"]\n\tquest.0123456789ABCDEF.quest_desc: [\n\t\t\"keep\"\n\t]\n}\n";
            String stripped = nl.juiced.guhs.compat.FtbQuestsChapter.stripOurLang(saved);
            helper.assertTrue(stripped.equals("{\n\tchapter.0123456789ABCDEF.title: \"Other\"\n\tquest.0123456789ABCDEF.quest_desc: [\n\t\t\"keep\"\n\t]\n}\n"),
                    "our multi-line texts are removed whole: " + stripped);
        } catch (java.io.IOException e) {
            helper.fail(e.toString());
        }
    }

    /** Every quest is in exactly one chapter, links point to our quests, the pictures exist, only the stomach sizes are locked. */
    @GuhTest(template = EMPTY)
    public static void ftbQuestsChaptersAreComplete(GameTestHelper helper) {
        ftbQuestsTellsAResaveFromAnEdit(helper);
        try {
            java.util.Set<String> quests = new java.util.HashSet<>();
            java.util.List<String> links = new java.util.ArrayList<>();
            int linear = 0;
            java.util.regex.Pattern title = java.util.regex.Pattern.compile("quest\\.(475548[0-9A-F]{10})\\.title: ");
            for (String name : nl.juiced.guhs.compat.FtbQuestsChapter.chapters()) {
                String chapter = ftbResource("ftbquests/chapters/" + name + ".snbt");
                String lang = ftbResource("ftbquests/lang/" + name + ".snbt");
                helper.assertTrue(chapter != null && lang != null, name + " is there");
                helper.assertTrue(chapter.contains("progression_mode: \"flexible\"") && chapter.contains("group: \"475548")
                        && chapter.contains("filename: \"" + name + "\""), name + ": flexible, in the Guhs group");
                java.util.regex.Matcher m = title.matcher(lang);
                int n = 0;
                while (m.find()) {
                    helper.assertTrue(quests.add(m.group(1)) && chapter.contains("id: \"" + m.group(1) + "\""), "quest " + m.group(1) + " once, in " + name);
                    n++;
                }
                helper.assertTrue(n > 0 && lang.contains("Hoe kom je hier?"), name + " has quests and a Hoe kom je hier?");
                java.util.regex.Matcher l = java.util.regex.Pattern.compile("linked_quest: \"([0-9A-F]{16})\"").matcher(chapter);
                while (l.find()) {
                    links.add(l.group(1));
                }
                java.util.regex.Matcher img = java.util.regex.Pattern.compile("image: \"guhs:(textures/ftbquests/[a-z0-9_/]+\\.png)\"").matcher(chapter);
                while (img.find()) {
                    helper.assertTrue(nl.juiced.guhs.compat.FtbQuestsChapter.class.getClassLoader().getResource("assets/guhs/" + img.group(1)) != null,
                            "picture " + img.group(1));
                }
                linear += chapter.split("progression_mode: \"linear\"", -1).length - 1;
                helper.assertTrue(name.equals("guhs_maag") || !chapter.contains("\"linear\""), name + ": nothing locked");
            }
            helper.assertTrue(quests.size() >= 429, "all quests (420 + 9 Hoe kom je hier?): " + quests.size());
            helper.assertTrue(quests.containsAll(links) && !links.isEmpty(), "the links point to our quests");
            helper.assertTrue(linear == 5, "only the five stomach sizes are locked: " + linear);
        } catch (java.io.IOException e) {
            helper.fail(e.toString());
        }
        helper.succeed();
    }

    private static String ftbResource(String path) throws java.io.IOException {
        try (java.io.InputStream in = nl.juiced.guhs.compat.FtbQuestsChapter.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /** The minigame scoreboards keep each player's best once, sorted, 3 places; the floating board shows up (and only once). */
    @GuhTest(template = EMPTY)
    public static void scorebordKeepsTheTop3(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        String board = "test_" + helper.absolutePos(BlockPos.ZERO).asLong();
        int[] times = {500, 300, 400, 200};
        for (int i = 0; i < times.length; i++) {
            net.minecraft.server.level.ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                    new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes((board + i).getBytes()), "zoeker" + i));
            nl.juiced.guhs.quest.Scorebord.submit(p, board, times[i], true);
        }
        net.minecraft.server.level.ServerPlayer first = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes((board + 3).getBytes()), "zoeker3"));
        helper.assertTrue(nl.juiced.guhs.quest.Scorebord.submit(first, board, 900, true) == 0, "a worse score doesn't replace your best");
        java.util.List<nl.juiced.guhs.quest.Scorebord.Entry> top = nl.juiced.guhs.quest.Scorebord.top(level.getServer(), board);
        helper.assertTrue(top.size() == 3 && top.get(0).score() == 200 && top.get(1).score() == 300 && top.get(2).score() == 400,
                "top 3, fastest first: " + top);
        net.minecraft.world.phys.Vec3 at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 3, 1.5));
        net.minecraft.network.chat.Component text = nl.juiced.guhs.quest.Scorebord.text(level.getServer(),
                net.minecraft.network.chat.Component.literal("Top"), java.util.List.of(board), java.util.List.of(), String::valueOf);
        nl.juiced.guhs.quest.Scorebord.show(level, at, "test", text);
        nl.juiced.guhs.quest.Scorebord.show(level, at, "test", text);
        helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,
                new net.minecraft.world.phys.AABB(at, at).inflate(2)).size() == 1, "one floating scoreboard");
        helper.succeed();
    }

    /** The guh record is a jukebox record; the picnic's jukebox holds it and starts playing it (only at the picnic). */
    @GuhTest(template = EMPTY)
    public static void picnicJukeboxPlaysTheGuhRecord(GameTestHelper helper) {
        var disc = new net.minecraft.world.item.ItemStack(ModItems.MUSIC_DISC_ZE_HANGEN.get());
        helper.assertTrue(net.minecraft.world.item.JukeboxSong.fromStack(helper.getLevel().registryAccess(), disc).isPresent(), "a jukebox song");
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.JUKEBOX);
        var jukebox = (net.minecraft.world.level.block.entity.JukeboxBlockEntity) helper.getBlockEntity(pos);
        jukebox.setSongItemWithoutPlaying(disc.copy()); // (marks it as quietly playing: stop that, like a jukebox loaded from a structure)
        jukebox.getSongPlayer().stop(helper.getLevel(), jukebox.getBlockState());
        helper.assertTrue(!nl.juiced.guhs.quest.PicknickMuziek.play(jukebox, false) && !jukebox.getSongPlayer().isPlaying(), "not away from the picnic");
        helper.assertTrue(nl.juiced.guhs.quest.PicknickMuziek.play(jukebox, true) && jukebox.getSongPlayer().isPlaying(), "it plays at the picnic");
        helper.assertTrue(!nl.juiced.guhs.quest.PicknickMuziek.play(jukebox, true), "and isn't started twice");
        var template = helper.getLevel().getStructureManager().get(nl.juiced.guhs.Guhs.id("guh_picnic")).orElseThrow();
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        boolean found = template.filterBlocks(BlockPos.ZERO, settings, net.minecraft.world.level.block.Blocks.JUKEBOX).stream()
                .anyMatch(b -> b.nbt() != null && b.nbt().toString().contains("music_disc_ze_hangen"));
        helper.assertTrue(found, "the picnic has a jukebox with the record in it");
        helper.succeed();
    }
}
