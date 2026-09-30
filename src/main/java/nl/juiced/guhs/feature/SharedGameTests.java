package nl.juiced.guhs.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bibliotheek.BibliotheekProtection;
import nl.juiced.guhs.feature.disco.DiscoGame;
import nl.juiced.guhs.feature.golf.GolfFeature;
import nl.juiced.guhs.feature.smul.SmulFeature;
import nl.juiced.guhs.feature.smul.SmulGame;
import nl.juiced.guhs.menu.BankGuhMenu;
import nl.juiced.guhs.menu.GuhWardrobeMenu;
import nl.juiced.guhs.quest.VerstopGame;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Tests of what the minigames and guh buildings share: one game at a time, no free healing, loaned items stay loaned,
 * and no fire or floods against a protected building.
 */
public class SharedGameTests {
    private static final String EMPTY = "empty";

    private static ServerPlayer survivor(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, BlockPos at) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        BlockPos pos = helper.absolutePos(at);
        npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    /** While you seek in the verstopguh house, no other game starts; a seeker who is gone isn't protected any more. */
    @GuhTest(template = "verstopguh_huis", timeoutTicks = 200)
    public static void oneGameAtATimeAndSeekersDropOut(GameTestHelper helper) {
        var npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(100),
                n -> n.getKind() == GuhNpcEntity.Kind.VERSTOPGUHTJE);
        helper.assertTrue(npcs.size() == 1, "Verstopguhtje is on the roof");
        GuhNpcEntity host = npcs.get(0);
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(host.getX() + 1, host.getY(), host.getZ());
        VerstopGame.action(host, player, VerstopGame.START);
        helper.assertTrue(VerstopGame.isSeeking(player) && Minigames.VERSTOP.equals(Minigames.playing(player)), "seeking");
        helper.assertTrue(Minigames.busyElsewhere(player, Minigames.SMUL) && !Minigames.busyElsewhere(player, Minigames.VERSTOP),
                "busy for the other games, not for this one");
        BlockPos near = BlockPos.containing(player.position()).subtract(helper.absolutePos(BlockPos.ZERO));
        GuhNpcEntity smulguh = npc(helper, GuhNpcEntity.Kind.SMULGUH, near.east());
        GuhNpcEntity dj = npc(helper, GuhNpcEntity.Kind.DJGUH, near.west());
        helper.assertTrue(!SmulGame.start(smulguh, player) && !SmulGame.isPlaying(player), "no smulling while seeking");
        helper.assertTrue(player.getInventory().countItem(SmulFeature.SMULSCHAAL.get()) == 0, "and no bowl");
        helper.assertTrue(!DiscoGame.of(dj).start(dj, player) && !DiscoGame.isDancing(player), "no dancing either");
        smulguh.discard();
        dj.discard();

        Vec3 inside = player.position();
        player.teleportTo(inside.x + 300, inside.y, inside.z);                    // (a Reisguh far away: the house stops ticking)
        player.tickCount = 20;                                                   // (seekers are checked every second)
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(!VerstopGame.isSeeking(player), "far away: no longer protected");
        player.teleportTo(inside.x, inside.y, inside.z);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(VerstopGame.isSeeking(player), "back in the house (still in the game): protected again");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
            helper.assertTrue(!VerstopGame.isSeeking(player) && Minigames.playing(player) == null, "logged out: nobody's protected");
            VerstopGame.walkOut(player, host.blockPosition());
            helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            helper.succeed();
        });
    }

    /** Starting a game doesn't heal or feed you; during the game you just don't get any worse. */
    @GuhTest(template = EMPTY)
    public static void gamesDontHealYouForFree(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        player.setHealth(6f);
        player.getFoodData().setFoodLevel(4);
        Minigames.startKeeping(player);
        Minigames.keep(player);
        helper.assertTrue(player.getHealth() == 6f && player.getFoodData().getFoodLevel() == 4, "no free healing: " + player.getHealth());
        player.setHealth(2f);
        player.getFoodData().setFoodLevel(1);
        Minigames.keep(player);
        helper.assertTrue(player.getHealth() == 6f && player.getFoodData().getFoodLevel() == 4, "but no worse than at the start");
        Minigames.forget(player);
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    private static void bankAction(BankGuhMenu menu, BankGuhMenu.Action action) {
        try {
            menu.handleAction(action, ItemStack.EMPTY, 0, false);
        } catch (RuntimeException notSent) {
            // (afterwards it sends the stomach's contents: a mock player can't receive that)
        }
    }

    /** A loaned golf club doesn't go into a guh's backpack, the Bank Guh or an item frame. */
    @GuhTest(template = EMPTY)
    public static void loanedItemsStayLoaned(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        ItemStack club = new ItemStack(GolfFeature.GOLFCLUB.get());
        helper.assertTrue(Features.isLoaned(club) && !Features.isLoaned(new ItemStack(Items.COOKIE)), "the club is loaned, a cookie isn't");

        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 3));
        guh.wear(GuhClothes.GUH_BACKPACK);
        GuhWardrobeMenu wardrobe = new GuhWardrobeMenu(0, player.getInventory(), guh);
        helper.assertTrue(!wardrobe.getSlot(GuhWardrobeMenu.PACK_START).mayPlace(club), "not in the backpack");
        helper.assertTrue(wardrobe.getSlot(GuhWardrobeMenu.PACK_START).mayPlace(new ItemStack(Items.COOKIE)), "a cookie is fine");

        BlockPos pos = new BlockPos(1, 1, 3);
        helper.setBlock(pos, ModBlocks.BANK_GUH.get());
        BankGuhBlockEntity bank = (BankGuhBlockEntity) helper.getBlockEntity(pos);
        BankGuhMenu menu = new BankGuhMenu(1, player.getInventory(), bank);
        menu.setCarried(club.copy());
        bankAction(menu, BankGuhMenu.Action.DEPOSIT_CARRIED);
        helper.assertTrue(menu.getCarried().is(GolfFeature.GOLFCLUB.get()), "the Bank Guh doesn't take it from the cursor");
        menu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(9, club.copy());
        bankAction(menu, BankGuhMenu.Action.DEPOSIT_INVENTORY);
        menu.quickMoveStack(player, BankGuhMenu.INV_START);
        helper.assertTrue(player.getInventory().getItem(9).is(GolfFeature.GOLFCLUB.get()), "nor from the inventory");
        helper.assertTrue(bank.getStorage().count(club) == 0, "nothing loaned in the stomach");
        helper.assertTrue(!menu.getSlot(BankGuhMenu.GRID_START).mayPlace(club), "not even in its crafting grid");

        player.setItemInHand(InteractionHand.MAIN_HAND, club.copy());
        ItemFrame frame = new ItemFrame(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), Direction.NORTH);
        var hang = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, frame);
        NeoForge.EVENT_BUS.post(hang);
        helper.assertTrue(hang.isCanceled(), "and not in an item frame");
        guh.discard();
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /** No fire against a protected building: lighting one next to it is refused, and fire that appears there goes out. */
    @GuhTest(template = EMPTY)
    public static void noFireAgainstAProtectedBuilding(GameTestHelper helper) {
        BlockPos a = helper.absolutePos(new BlockPos(3, 1, 0)), b = helper.absolutePos(new BlockPos(4, 3, 4));
        BoundingBox building = BoundingBox.fromCorners(a, b);
        BibliotheekProtection.TEST_AREAS.add(building);
        try {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                }
            }
            helper.assertTrue(Protected.at(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 2)))
                    && !Protected.at(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))), "the building is protected, next to it isn't");
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.FIRE);
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 1, 2));
            helper.setBlock(new BlockPos(0, 1, 2), Blocks.FIRE);                     // (3 away: that one burns on)
            helper.assertBlockPresent(Blocks.FIRE, new BlockPos(0, 1, 2));
            helper.setBlock(new BlockPos(0, 1, 2), Blocks.AIR);

            ServerPlayer player = survivor(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
            BlockPos floor = helper.absolutePos(new BlockPos(0, 0, 2));
            var light = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, floor,
                    new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false));
            NeoForge.EVENT_BUS.post(light);
            helper.assertTrue(light.isCanceled(), "no flint and steel within 3 blocks of it");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
            var pour = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, floor,
                    new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false));
            NeoForge.EVENT_BUS.post(pour);
            helper.assertTrue(pour.isCanceled(), "no lava bucket either");
            player.setGameMode(GameType.CREATIVE);
            var builder = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, floor,
                    new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false));
            NeoForge.EVENT_BUS.post(builder);
            helper.assertTrue(!builder.isCanceled(), "builders in creative may");
            helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        } finally {
            BibliotheekProtection.TEST_AREAS.remove(building);
        }
        helper.succeed();
    }

    /** Water poured outside doesn't flow into a protected building. */
    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void noFloodingAProtectedBuilding(GameTestHelper helper) {
        BoundingBox building = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(3, 1, 2)), helper.absolutePos(new BlockPos(4, 2, 2)));
        BibliotheekProtection.TEST_AREAS.add(building);
        for (int x = 0; x < 5; x++) {                    // a channel along z = 2
            helper.setBlock(new BlockPos(x, 0, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 1, 3), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(0, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.WATER);
        helper.runAfterDelay(40, () -> {
            try {
                helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 2)).getFluidState().isSource() == false
                        && !helper.getBlockState(new BlockPos(2, 1, 2)).getFluidState().isEmpty(), "the water flows up to the building");
                helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 2)).getFluidState().isEmpty(), "but not into it");
            } finally {
                for (int x = 1; x < 5; x++) {
                    helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);   // (no water left to run off once the building is gone)
                }
                BibliotheekProtection.TEST_AREAS.remove(building);
            }
            helper.succeed();
        });
    }
}
