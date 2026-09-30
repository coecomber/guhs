package nl.juiced.guhs.feature.kaasmijn;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * Game tests of the kaasmijn: the loaner pickaxe (you need nothing of your own), mining veins and their regrowth, the
 * protection, the kaaskluis, the cart dispenser and the Mijnguh's shop, and the mine template itself.
 * (A test marks its little area as "a mine" with {@link KaasmijnProtection#TEST_AREAS}.)
 */
public class KaasmijnGameTests {
    private static final String EMPTY = "empty";

    private static AABB mine(GameTestHelper helper) {
        // (only the test's own 5x4x5 room: a bigger area could protect the blocks of a neighbouring test)
        BlockPos corner = helper.absolutePos(BlockPos.ZERO);
        AABB area = new AABB(corner.getX(), corner.getY(), corner.getZ(), corner.getX() + 5, corner.getY() + 4, corner.getZ() + 5);
        KaasmijnProtection.TEST_AREAS.add(area);
        return area;
    }

    private static ServerPlayer miner(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static GuhNpcEntity mijnguh(GameTestHelper helper) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.MIJNGUH);
        BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3));
        npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    private static void done(GameTestHelper helper, AABB area, ServerPlayer... players) {
        KaasmijnProtection.TEST_AREAS.remove(area);
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void mijnguhLendsAPickaxeAndYouMineCheese(GameTestHelper helper) {
        AABB area = mine(helper);
        try {
            ServerPlayer player = miner(helper);
            GuhNpcEntity npc = mijnguh(helper);
            helper.assertTrue(player.getInventory().isEmpty(), "you come with empty paws");
            KaasmijnFeature.role().talk(npc, player);
            helper.assertTrue(LeenhouweelItem.count(player) == 1, "the Mijnguh lends a pickaxe");
            helper.assertTrue(GuhQuests.saved(player).getBooleanOr(Mijnguh.MET_KEY, false), "and remembers you");
            helper.assertTrue(player.getMainHandItem().getItem() instanceof LeenhouweelItem, "it's in your hand");
            KaasmijnFeature.role().talk(npc, player);
            helper.assertTrue(LeenhouweelItem.count(player) == 1, "one at a time");

            BlockPos vein = new BlockPos(2, 1, 2);
            helper.setBlock(vein, KaasmijnFeature.KAASADER.get());
            helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(vein)), "the vein can be mined");
            helper.assertBlockPresent(KaasmijnFeature.UITGEMIJNDE_KAASADER.get(), vein);
            long chunks = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(KaasmijnFeature.KAASBROK.get()))
                    .stream().mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(chunks >= 1, "it drops kaasbrokken: " + chunks);
            helper.assertTrue(!player.gameMode.destroyBlock(helper.absolutePos(vein)), "a mined-out vein can't be broken");

            helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
            helper.assertTrue(!player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(3, 1, 1))), "the mine itself can't be broken");
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(3, 1, 1));

            KaasmijnProtection.TEST_AREAS.remove(area);        // walking out of the mine
            helper.assertTrue(LeenhouweelItem.takeBack(player) == 1 && LeenhouweelItem.count(player) == 0, "the pickaxe goes back");
            npc.discard();
            done(helper, area, player);
        } finally {
            KaasmijnProtection.TEST_AREAS.remove(area);   // (also when an assertion fails: no stray mine)
        }
    }

    @GuhTest(template = EMPTY)
    public static void minedOutVeinsGrowBack(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, KaasmijnFeature.UITGEMIJNDE_GOUDEN_KAASADER.get());
        var random = helper.getLevel().getRandom();
        for (int i = 0; i < 400 && !helper.getBlockState(pos).is(KaasmijnFeature.GOUDEN_KAASADER.get()); i++) {
            helper.getBlockState(pos).randomTick(helper.getLevel(), helper.absolutePos(pos), random);
        }
        helper.assertBlockPresent(KaasmijnFeature.GOUDEN_KAASADER.get(), pos);
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void theMineCantBeBrokenOrBuiltIn(GameTestHelper helper) {
        AABB area = mine(helper);
        try {
            ServerPlayer player = miner(helper);
            BlockPos stone = helper.absolutePos(new BlockPos(1, 1, 3));
            helper.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
            helper.assertTrue(breakCanceled(helper, stone, player), "no breaking the walls");
            BlockPos vein = helper.absolutePos(new BlockPos(3, 1, 3));
            helper.getLevel().setBlockAndUpdate(vein, KaasmijnFeature.DIEPE_KAASADER.get().defaultBlockState());
            helper.assertTrue(breakCanceled(helper, vein, player), "no mining veins with bare paws (it would waste them)");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.WOODEN_PICKAXE));
            helper.assertTrue(!breakCanceled(helper, vein, player), "any pickaxe will do");
            var place = new BlockEvent.EntityPlaceEvent(net.neoforged.neoforge.common.util.BlockSnapshot.create(helper.getLevel().dimension(),
                    helper.getLevel(), stone.above()), Blocks.AIR.defaultBlockState(), player);
            NeoForge.EVENT_BUS.post(place);
            helper.assertTrue(place.isCanceled(), "no building");
            KaasmijnProtection.TEST_AREAS.remove(area);
            helper.assertTrue(!breakCanceled(helper, stone, player), "outside the mine it's just stone");
            done(helper, area, player);
        } finally {
            KaasmijnProtection.TEST_AREAS.remove(area);   // (also when an assertion fails: no stray mine)
        }
    }

    /** The sitting guh miners stay put: no taming, leashing or undressing them (and they can't be hurt). */
    @GuhTest(template = EMPTY)
    public static void theGuhMinersCantBeTakenAway(GameTestHelper helper) {
        ServerPlayer player = miner(helper);
        var guh = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3));
        guh.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        guh.addTag(KaasmijnProtection.MINER_TAG);
        helper.getLevel().addFreshEntity(guh);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.LEAD));
        var event = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(player,
                net.minecraft.world.InteractionHand.MAIN_HAND, guh);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.isCanceled(), "a miner can't be leashed or tamed");
        guh.removeTag(KaasmijnProtection.MINER_TAG);
        var other = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(player,
                net.minecraft.world.InteractionHand.MAIN_HAND, guh);
        NeoForge.EVENT_BUS.post(other);
        helper.assertTrue(!other.isCanceled(), "any other guh can");
        guh.discard();
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    private static boolean breakCanceled(GameTestHelper helper, BlockPos pos, ServerPlayer player) {
        var event = new BreakBlockEvent(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), player);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    @GuhTest(template = EMPTY)
    public static void kaaskluisOpensForGoudkaas(GameTestHelper helper) {
        AABB area = mine(helper);
        try {
            ServerPlayer player = miner(helper);
            BlockPos pos = new BlockPos(2, 1, 2);
            helper.setBlock(pos, KaasmijnFeature.KAASKLUIS.get());
            player.getInventory().add(new ItemStack(KaasmijnFeature.GOUDKAAS.get(), 1));
            helper.assertTrue(!KaaskluisBlock.open(player, helper.absolutePos(pos)), "one goudkaas is not enough");
            helper.assertTrue(GuhQuests.count(player, KaasmijnFeature.GOUDKAAS.get()) == 1, "and you keep it");
            player.getInventory().add(new ItemStack(KaasmijnFeature.GOUDKAAS.get(), 2));
            helper.assertTrue(KaaskluisBlock.open(player, helper.absolutePos(pos)), "two open the vault");
            helper.assertTrue(GuhQuests.count(player, KaasmijnFeature.GOUDKAAS.get()) == 1, "it costs two");
            int treasure = 0;
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (!stack.isEmpty() && !stack.is(KaasmijnFeature.GOUDKAAS.get())) {
                    treasure += stack.getCount();
                }
            }
            helper.assertTrue(treasure >= 3, "a treasure: " + treasure);
            done(helper, area, player);
        } finally {
            KaasmijnProtection.TEST_AREAS.remove(area);   // (also when an assertion fails: no stray mine)
        }
    }

    @GuhTest(template = EMPTY)
    public static void karretjesautomaatPutsOneCartOnTheRails(GameTestHelper helper) {
        AABB area = mine(helper);
        try {
            ServerPlayer player = miner(helper);
            helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.RAIL);
            helper.setBlock(new BlockPos(2, 1, 3), KaasmijnFeature.KARRETJESAUTOMAAT.get());
            BlockPos dispenser = helper.absolutePos(new BlockPos(2, 1, 3));
            helper.assertTrue(KarretjesautomaatBlock.dispense(helper.getLevel(), dispenser, player), "a cart");
            helper.assertTrue(!KarretjesautomaatBlock.dispense(helper.getLevel(), dispenser, player), "not a second one on top of it");
            var carts = helper.getLevel().getEntitiesOfClass(AbstractMinecart.class, area);
            helper.assertTrue(carts.size() == 1, "one cart: " + carts.size());
            AbstractMinecart cart = carts.get(0);
            helper.assertTrue(cart.isInvulnerable() && !cart.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 100f),
                    "it can't be broken for a free minecart");
            // riding it counts for the Kaasexpress quest
            player.startRiding(cart, true, true);
            for (int i = 0; i < KaasmijnFeature.RIDE_SECONDS; i++) {
                KaasmijnFeature.rideSecond(player);
            }
            helper.assertTrue(GuhQuests.saved(player).getIntOr(KaasmijnFeature.RIDE_KEY, 0) == KaasmijnFeature.RIDE_SECONDS, "half a minute in the Kaasexpress");
            helper.assertTrue(KarretjesautomaatBlock.cleanupCarts(helper.getLevel(), area, KarretjesautomaatBlock.IDLE_TICKS) == 0, "a cart with someone in it stays");
            player.stopRiding();
            helper.assertTrue(KarretjesautomaatBlock.cleanupCarts(helper.getLevel(), area, KarretjesautomaatBlock.IDLE_TICKS / 2) == 0, "an empty one waits a while");
            helper.assertTrue(KarretjesautomaatBlock.cleanupCarts(helper.getLevel(), area, KarretjesautomaatBlock.IDLE_TICKS / 2) == 1, "and then goes away");
            done(helper, area, player);
        } finally {
            KaasmijnProtection.TEST_AREAS.remove(area);   // (also when an assertion fails: no stray mine)
        }
    }

    @GuhTest(template = EMPTY)
    public static void mijnguhSellsTheMinerOutfit(GameTestHelper helper) {
        GuhNpcEntity npc = mijnguh(helper);
        var offers = npc.getOffers();
        for (GuhClothes clothes : new GuhClothes[]{GuhClothes.KAASMIJN_HELM, GuhClothes.KAASMIJN_OVERALL, GuhClothes.KAASMIJN_ZAKDOEK}) {
            helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(ModItems.clothingItem(clothes))
                    && o.getBaseCostA().is(KaasmijnFeature.KAASBROK.get())), "the outfit is for kaasbrokken: " + clothes.id());
        }
        helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(KaasmijnFeature.KAASHOUWEEL.get())), "his own pickaxe is for sale too");
        helper.assertTrue(offers.stream().noneMatch(o -> o.getResult().is(KaasmijnFeature.LEENHOUWEEL.get())), "the loaner is not");
        npc.discard();
        helper.succeed();
    }

    /** The outfit is only sold by the Mijnguh (not in the treasure chests), and a vein never drops itself: cheese ore stays in the mine. */
    @GuhTest(template = EMPTY)
    public static void outfitAndVeinsOnlyInTheMine(GameTestHelper helper) {
        var level = helper.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE, Guhs.id("chests/kaasmijn_schat")));
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 1, 1)))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        for (int i = 0; i < 200; i++) {
            for (ItemStack stack : table.getRandomItems(params)) {
                helper.assertFalse(stack.getItem() instanceof nl.juiced.guhs.item.GuhClothingItem, "no outfit in the treasure: " + stack);
            }
        }
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        for (var vein : java.util.List.of(KaasmijnFeature.KAASADER.get(), KaasmijnFeature.DIEPE_KAASADER.get(), KaasmijnFeature.GOUDEN_KAASADER.get())) {
            var drops = net.minecraft.world.level.block.Block.getDrops(vein.defaultBlockState(), level, pos, null, null,
                    new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
            helper.assertTrue(!drops.isEmpty() && drops.stream().noneMatch(d -> d.getItem() instanceof net.minecraft.world.item.BlockItem),
                    "a vein drops cheese, never itself: " + drops);
        }
        helper.succeed();
    }

    /** The mine as generated: the Mijnguh at his counter, veins, a closed powered loop with carts, the way down, the vault. */
    @GuhTest(template = "kaasmijn", timeoutTicks = 100)
    public static void theMineTemplateIsComplete(GameTestHelper helper) {
        var npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(1),
                n -> n.getKind() == GuhNpcEntity.Kind.MIJNGUH);
        helper.assertTrue(npcs.size() == 1, "one Mijnguh: " + npcs.size());
        BlockPos npcPos = npcs.get(0).blockPosition();
        helper.assertTrue(helper.getLevel().getBlockState(npcPos.below()).isSolid(), "he sits on the floor");
        // the template's coordinates (see tools/features/kaasmijn.py), counted from where the Mijnguh sits (57, 35, 55)
        BlockPos o = npcPos.offset(-57, -35, -55);
        int l1 = 20, l2 = 6, g = 34;
        var level = helper.getLevel();
        helper.assertTrue(level.getBlockState(o.offset(25, l1 + 1, 10)).is(Blocks.POWERED_RAIL)
                && level.getBlockState(o.offset(25, l1 + 1, 10)).getValue(PoweredRailBlock.POWERED), "the Kaasexpress has power: " + level.getBlockState(o.offset(25, l1 + 1, 10)));
        helper.assertTrue(level.getBlockState(o.offset(10, l1 + 1, 10)).is(Blocks.RAIL), "a bend in the loop");
        helper.assertTrue(level.getBlockState(o.offset(44, l1 + 1, 86)).is(KaasmijnFeature.KARRETJESAUTOMAAT.get()), "a cart dispenser at the station");
        helper.assertTrue(level.getBlockState(o.offset(5, l2 + 1, 70)).is(KaasmijnFeature.KAASKLUIS.get()), "the kaaskluis");
        helper.assertTrue(level.getBlockState(o.offset(55, l2, 37)).is(Blocks.SOUL_SAND), "the bubble lift");
        helper.assertTrue(level.getBlockState(o.offset(51, l2, 37)).is(Blocks.WATER), "water under the VAHOEG-sprong");
        helper.assertTrue(level.getBlockState(o.offset(39, g, 44)).is(Blocks.SPRUCE_STAIRS), "the stairs down");
        int veins = 0, gold = 0;
        for (BlockPos p : BlockPos.betweenClosed(o, o.offset(95, g, 95))) {
            var state = helper.getLevel().getBlockState(p);
            if (state.getBlock() instanceof KaasaderBlock) {
                veins++;
                gold += state.is(KaasmijnFeature.GOUDEN_KAASADER.get()) ? 1 : 0;
            }
        }
        helper.assertTrue(veins > 100 && gold > 5, "cheese veins: " + veins + ", gold: " + gold);
        var carts = helper.getLevel().getEntitiesOfClass(AbstractMinecart.class, helper.getBounds().inflate(1));
        helper.assertTrue(carts.size() >= 3 && carts.stream().allMatch(Entity::isInvulnerable), "carts, and you can't break them: " + carts.size());
        var miners = helper.getLevel().getEntitiesOfClass(nl.juiced.guhs.entity.GuhEntity.class,
                helper.getBounds().inflate(1), KaasmijnProtection::isMiner);
        helper.assertTrue(miners.size() >= 10 && miners.stream().allMatch(Entity::isInvulnerable), "guh miners, safe from harm: " + miners.size());
        helper.succeed();
    }
}
