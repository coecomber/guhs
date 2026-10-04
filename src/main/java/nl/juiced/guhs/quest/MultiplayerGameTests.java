package nl.juiced.guhs.quest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.block.SleeRailBlock;
import nl.juiced.guhs.block.entity.SleeRailBlockEntity;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhSleeEntity;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.slee.SleePath;

/**
 * 1.2.7: the older minigames with any number of players on one server: the kermis keeps its sleds (a rider who logs out
 * gets off, the sled rides home, missing sleds come back, the station heals its finish line, the coaster can't be broken),
 * several customers shop with one character at once, and a verstopguh game doesn't keep the house occupied for ever.
 */
public class MultiplayerGameTests {
    private static ServerPlayer survivor(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static List<GuhSleeEntity> sleds(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(GuhSleeEntity.class, helper.getBounds().inflate(1), s -> s.isAlive() && s.isLocked());
    }

    /**
     * The real guh kermis. A rider who logs out gets off first (the sled stays in the world), the sled then rides on to the
     * station by itself and parks there; a missing sled is made again by the Kermis-guh; the station piece gets its finish
     * line back (also when it was broken); and survival players can't break the coaster.
     */
    @GuhTest(template = "guh_kermis", timeoutTicks = 2400, batch = "mp_kermis")
    public static void kermisKeepsItsSledsAndCantBeBroken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB b = helper.getBounds();
        BoundingBox box = new BoundingBox((int) b.minX, (int) b.minY, (int) b.minZ, (int) b.maxX, (int) b.maxY, (int) b.maxZ);
        // (the template stands one block above the test's own origin: take the origin where the station piece really is)
        Kermis.Area found = null;
        for (int dy = 0; dy <= 1 && found == null; dy++) {
            Kermis.Area tryArea = new Kermis.Area(box, helper.absolutePos(new BlockPos(0, dy, 0)), Rotation.NONE);
            SleePath.Placement st = Kermis.station(level, tryArea);
            if (st != null && level.getBlockState(st.anchor()).getBlock() instanceof SleeRailBlock) {
                found = tryArea;
            }
        }
        helper.assertTrue(found != null, "the kermis template is where the test expects it");
        Kermis.Area area = found;
        Kermis.TEST_AREAS.put(box, area);
        ServerPlayer player = survivor(helper);
        GuhSleeEntity[] sled = {null, null};
        int[] stage = {0};
        helper.runAfterDelay(30, () -> {
            List<GuhSleeEntity> all = sleds(helper);
            helper.assertTrue(all.size() == 2 && all.stream().allMatch(s -> s.getPiece() != null && s.atStation()), "both sleds stand at the station: " + all.size());
            SleePath.Placement station = Kermis.station(level, area);
            helper.assertTrue(station != null && level.getBlockState(station.anchor()).getBlock() instanceof SleeRailBlock
                    && level.getBlockEntity(station.anchor()) instanceof SleeRailBlockEntity rail && rail.isFinish(), "the station piece is found: " + station
                    + " " + (station == null ? null : level.getBlockState(station.anchor()) + " / " + level.getBlockEntity(station.anchor())
                    + " finish " + (level.getBlockEntity(station.anchor()) instanceof SleeRailBlockEntity r2 && r2.isFinish())) + " origin " + area.origin());
            // protection: not for survival players, fine for builders in creative
            helper.assertTrue(Kermis.protectedAt(level, station.anchor()) && Kermis.denied(player, station.anchor()), "no breaking the coaster in survival");
            ServerPlayer builder = GuhMockPlayer.of(helper);
            helper.assertTrue(!Kermis.denied(builder, station.anchor()), "a builder in creative may");
            leave(helper, builder);
            helper.assertTrue(!Kermis.protectedAt(level, station.anchor().offset(0, 0, -400)), "and it ends at the fence");
            sled[0] = all.get(0);
            sled[1] = all.get(1);
            player.snapTo(sled[0].position());
            player.startRiding(sled[0], true, true);
            sled[0].setSpeed(3);
            sled[0].setRunning(true);
            stage[0] = 1;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(stage[0] >= 1, "started");
            if (stage[0] == 1) {
                helper.assertTrue(!sled[0].atStation(), "the sled is out on the track");
                // the rider logs out half way: off the sled, which stays in the world and goes home
                NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
                if (player.getVehicle() != null || !sled[0].isAlive() || !sled[0].isHoming() || !sled[0].isRunning()) {
                    throw new IllegalStateException("logging out: the rider gets off and the sled rides home: vehicle " + player.getVehicle()
                            + ", homing " + sled[0].isHoming() + ", running " + sled[0].isRunning());
                }
                stage[0] = 2;
            }
            helper.assertTrue(!sled[0].isHoming() && !sled[0].isRunning() && sled[0].atStation(), "the empty sled parks at the station");
            helper.assertTrue(sled[0].distanceTo(sled[1]) > 1.5, "next to the other one, not in it: " + sled[0].distanceTo(sled[1]));
            GuhNpcEntity npc = level.getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(), n -> n.getKind() == GuhNpcEntity.Kind.KERMIS_GUH).get(0);
            // a sled is gone (left with a rider before 1.2.7): the Kermis-guh makes a new one, after a few looks
            sled[1].discard();
            Kermis.look(npc, level, area);
            helper.assertTrue(sleds(helper).size() == 1, "not at the first look");
            Kermis.look(npc, level, area);
            Kermis.look(npc, level, area);
            List<GuhSleeEntity> now = sleds(helper);
            helper.assertTrue(now.size() == 2 && now.stream().allMatch(s -> s.getPiece() != null && s.atStation()), "two sleds at the station again: " + now.size());
            helper.assertTrue(now.get(0).distanceTo(now.get(1)) > 1.5, "each on its own spot");
            // a third one (the old sled comes back with its rider): one too many goes
            GuhSleeEntity extra = ModEntities.GUH_SLEE.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            extra.setLocked(true);
            extra.snapTo(now.get(0).position());
            level.addFreshEntity(extra);
            Kermis.look(npc, level, area);
            helper.assertTrue(sleds(helper).size() == 2, "never more than two: " + sleds(helper).size());
            // the finish line heals: the flag, and the whole piece
            SleePath.Placement station = Kermis.station(level, area);
            ((SleeRailBlockEntity) level.getBlockEntity(station.anchor())).setFinish(false);
            Kermis.look(npc, level, area);
            helper.assertTrue(((SleeRailBlockEntity) level.getBlockEntity(station.anchor())).isFinish(), "the station is the finish line again");
            level.destroyBlock(station.anchor(), false);
            helper.assertTrue(level.getBlockState(station.anchor()).isAir(), "(the station piece is broken)");
            Kermis.look(npc, level, area);
            helper.assertTrue(level.getBlockEntity(station.anchor()) instanceof SleeRailBlockEntity rail && rail.isFinish(), "and the piece itself comes back");
            Kermis.TEST_AREAS.remove(box);
            leave(helper, player);
        });
    }

    /** Two customers at one character's shop at the same time: each has their own counter, and both can buy. */
    @GuhTest(template = "empty", batch = "mp_winkel")
    public static void twoCustomersShopAtOnce(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 2, 2));
        npc.setKind(GuhNpcEntity.Kind.KERMIS_GUH);
        ServerPlayer een = survivor(helper), twee = survivor(helper);
        een.snapTo(npc.getX() + 1.5, npc.getY(), npc.getZ());
        twee.snapTo(npc.getX(), npc.getY(), npc.getZ() + 1.5);
        npc.openShop(een);
        npc.openShop(twee);
        helper.assertTrue(een.containerMenu instanceof MerchantMenu && twee.containerMenu instanceof MerchantMenu, "both have the shop open");
        MerchantMenu m1 = (MerchantMenu) een.containerMenu, m2 = (MerchantMenu) twee.containerMenu;
        helper.assertTrue(m1.stillValid(een) && m2.stillValid(twee), "and both stay open");
        helper.assertTrue(m1.getOffers().size() == npc.getOffers().size() && m2.getOffers().size() == npc.getOffers().size(), "the same offers");
        for (MerchantMenu m : new MerchantMenu[] {m1, m2}) {
            ServerPlayer p = m == m1 ? een : twee;
            m.getSlot(0).set(new ItemStack(ModItems.KERMISBON.get(), 2));
            helper.assertTrue(m.getSlot(2).getItem().is(ModItems.clothingItem(GuhClothes.KERMIS_STRIK)), "2 kermisbonnen: the strik is on the counter");
            m.quickMoveStack(p, 2);
            helper.assertTrue(GuhQuests.count(p, ModItems.clothingItem(GuhClothes.KERMIS_STRIK)) == 1 && GuhQuests.count(p, ModItems.KERMISBON.get()) == 0,
                    "bought it");
        }
        een.snapTo(npc.getX() + 20, npc.getY(), npc.getZ());
        helper.assertTrue(!m1.stillValid(een) && m2.stillValid(twee), "walking away closes your own counter only");
        een.closeContainer();
        twee.closeContainer();
        npc.discard();
        leave(helper, een, twee);
        helper.succeed();
    }

    /**
     * Verstopguh with more players: a second player who picks another level joins the running game (at its level), a
     * seeker who stands still for minutes drops out, and after half an hour the game is over whatever happens.
     */
    @GuhTest(template = "verstopguh_huis", timeoutTicks = 200, batch = "mp_verstop")
    public static void verstopGameNeverKeepsTheHouseForEver(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(100),
                n -> n.getKind() == GuhNpcEntity.Kind.VERSTOPGUHTJE);
        helper.assertTrue(npcs.size() == 1, "Verstopguhtje is on the roof");
        GuhNpcEntity host = npcs.get(0);
        ServerPlayer een = GuhMockPlayer.of(helper), twee = GuhMockPlayer.of(helper);
        een.snapTo(host.getX() + 1, host.getY(), host.getZ());
        twee.snapTo(host.getX() - 1, host.getY(), host.getZ());
        VerstopGame.action(host, een, VerstopGame.START);
        VerstopGame game = host.verstop;
        helper.assertTrue(game.isRunning() && game.isPlaying(een), "a game on makkelijk");
        VerstopGame.action(host, twee, VerstopGame.START + 2);
        helper.assertTrue(game.isPlaying(twee) && VerstopGame.isSeeking(twee), "the second player joins the running game");
        // the first seeker walks around, the second one stands still for more than three minutes
        host.tickCount = 20;
        game.tick(host);
        game.testClocks(0, twee, VerstopGame.IDLE_TICKS + 40);
        een.snapTo(een.getX() + 3, een.getY(), een.getZ());
        host.tickCount = 40;
        game.tick(host);
        helper.assertTrue(game.isRunning() && game.isPlaying(een) && !game.isPlaying(twee) && !VerstopGame.isSeeking(twee),
                "standing still for minutes: out; the one who is seeking goes on");
        // half an hour later the game is over
        game.testClocks(VerstopGame.MAX_TICKS + 40, null, 0);
        host.tickCount = 60;
        game.tick(host);
        helper.assertTrue(!game.isRunning() && !VerstopGame.isSeeking(een), "after half an hour the house is free again");
        VerstopGame.action(host, twee, VerstopGame.START + 1);
        helper.assertTrue(game.isRunning() && game.isPlaying(twee), "and the next one can start a game");
        VerstopGame.walkOut(twee, host.blockPosition());
        leave(helper, een, twee);
        helper.succeed();
    }
}
