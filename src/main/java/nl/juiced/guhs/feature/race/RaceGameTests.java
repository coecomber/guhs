package nl.juiced.guhs.feature.race;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Tests of the guhrace: a whole race on the real racebaan (no own items needed, rings in order, prizes, records, the
 * ghost, cleanup of the rental guh), the protection, the shop, the race guh itself and the time maths.
 */
public class RaceGameTests {
    private static final String TRACK = "guh_racebaan";
    private static final String EMPTY = "empty";
    /** Its own batch: the big racebaan templates would otherwise shift where the other tests are laid out. */
    private static final String BATCH = "guhrace";

    private static GuhNpcEntity raceguh(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(),
                n -> n.getKind() == GuhNpcEntity.Kind.RACEGUH);
        helper.assertTrue(npcs.size() == 1, "the Raceguh is in the pit stop: " + npcs.size());
        return npcs.get(0);
    }

    private static ServerPlayer racer(GameTestHelper helper, GuhNpcEntity npc) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(npc.getX(), npc.getY(), npc.getZ() - 2);
        return player;
    }

    private static RaceGuhEntity mount(GameTestHelper helper, Player player) {
        helper.assertTrue(player.getVehicle() instanceof RaceGuhEntity, "the racer rides a race guh: " + player.getVehicle());
        return (RaceGuhEntity) player.getVehicle();
    }

    /** Drives the race guh through a ring: from just above it, down into it (one race tick each). */
    private static void through(RaceGame game, GuhNpcEntity npc, RaceGuhEntity mount, int gate) {
        Vec3 c = game.track().centre(gate);
        mount.teleportTo(c.x, c.y + 6, c.z);
        game.tick(npc);
        mount.teleportTo(c.x, c.y, c.z);
        game.tick(npc);
    }

    private static void leave(GameTestHelper helper, Player... players) {
        for (Player p : players) {
            helper.getLevel().removePlayerImmediately((ServerPlayer) p, Entity.RemovalReason.DISCARDED);
        }
    }

    @GuhTest(batch = BATCH, template = TRACK, timeoutTicks = 200)
    public static void raceThreeLapsForPrizesAndARecord(GameTestHelper helper) {
        GuhNpcEntity npc = raceguh(helper);
        ServerPlayer player = racer(helper, npc);
        helper.assertTrue(player.getInventory().isEmpty(), "the racer brings nothing");
        RaceRole.action(npc, player, RaceRole.START);
        RaceGame game = RaceGame.of(npc);
        if (game != null) {
            game.trustTeleports();                                 // (the test moves the race guh by teleporting it)
        }
        helper.assertTrue(game != null && RaceGame.isRacing(player), "the race is on");
        RaceGuhEntity mount = mount(helper, player);
        helper.assertTrue(mount.isFrozen(), "the race guh waits for the countdown");
        helper.assertTrue(player.getInventory().isEmpty(), "nothing to carry around: the race guh is all you need");
        helper.assertTrue(game.track().gates.size() == 6, "6 rings on the racebaan: " + game.track().gates.size());
        helper.assertTrue(game.track().gates.get(0).contains(game.track().startPos()) == false, "the start is past the finish ring");
        player.getFoodData().setFoodLevel(3);
        player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fall(), 6f);
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "racers can't get hurt");

        // one at a time: somebody else has to wait
        ServerPlayer other = racer(helper, npc);
        RaceRole.action(npc, other, RaceRole.START);
        helper.assertTrue(RaceGame.of(npc) == game && !RaceGame.isRacing(other) && other.getVehicle() == null, "one race at a time");

        game.skipCountdown(npc);
        helper.assertTrue(!mount.isFrozen() && game.isRacing(), "VAHOEG! off we go");
        game.tick(npc);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 20, "racers never get hungry");
        through(game, npc, mount, 2);
        helper.assertTrue(game.next() == 1, "skipping ring 1 doesn't count: " + game.next());
        for (int lap = 0; lap < RaceGame.LAPS; lap++) {
            for (int gate : new int[]{1, 2, 3, 4, 5, 0}) {
                through(game, npc, mount, gate);
                if (lap < RaceGame.LAPS - 1 || gate != 0) {
                    helper.assertTrue(RaceGame.of(npc) == game && game.lap() == lap + (gate == 0 ? 1 : 0), "lap " + lap + " ring " + gate);
                }
            }
        }
        helper.assertTrue(RaceGame.of(npc) == null && !RaceGame.isRacing(player), "3 laps: finished");
        helper.assertTrue(mount.isRemoved() && player.getVehicle() == null, "the race guh went back to the pit stop");
        int prizes = GuhQuests.count(player, RaceFeature.RACEPRIJSJE.get());
        helper.assertTrue(prizes == RaceGame.Medal.GOUD.prizes + RaceGame.FIRST_PRIZES, "gold (6) + the first race bag (4): " + prizes);
        helper.assertTrue(GuhQuests.count(player, ModItems.KAAS_KNABBELS.get()) == 16, "and kaasknabbels for the first race");
        int best = RaceRecords.best(player);
        helper.assertTrue(best > 0 && RaceRecords.bestLap(player) > 0 && RaceRecords.races(player) == 1, "a record: " + best);
        helper.assertTrue(RaceRecords.splits(player).length == 3 * 6, "a time for every ring: " + RaceRecords.splits(player).length);
        helper.assertTrue(RaceRecords.ghost(player).length >= 3, "the race was recorded for the ghost");
        helper.assertTrue(Scorebord.top(player.level().getServer(), RaceRole.BOARD_TOTAL).stream().anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == best),
                "on the world's top 3 of race times");
        helper.assertTrue(Scorebord.top(player.level().getServer(), RaceRole.BOARD_LAP).stream().anyMatch(e -> e.player().equals(player.getUUID())
                && e.score() == RaceRecords.bestLap(player)), "and of the fastest laps");
        helper.assertTrue(RaceRole.trackRecord(player.level().getServer()) <= best, "the track record is at least that fast");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, npc.getBoundingBox().inflate(1, 4, 1),
                d -> d.entityTags().contains(Scorebord.TAG)).isEmpty(), "the top-3 board floats above the Raceguh");
        helper.assertTrue(player.distanceToSqr(npc) < 16, "back at the Raceguh");
        helper.assertTrue(player.getHealth() == player.getMaxHealth() && player.getAbilities().mayBuild, "an ordinary player again");
        leave(helper, player, other);
        helper.succeed();
    }

    @GuhTest(batch = BATCH, template = TRACK, timeoutTicks = 200)
    public static void raceGhostDrivesYourBestRace(GameTestHelper helper) {
        GuhNpcEntity npc = raceguh(helper);
        ServerPlayer player = racer(helper, npc);
        // an earlier best race: straight ahead from the start, half a block per sample
        int[] recording = new int[3 * 200];
        for (int i = 0; i < 200; i++) {
            recording[i * 3 + 2] = i * 8;
        }
        RaceRecords.save(player, 3000, 1000, new int[18], recording);
        RaceRole.action(npc, player, RaceRole.START);
        RaceGame game = RaceGame.of(npc);
        if (game != null) {
            game.trustTeleports();                                 // (the test moves the race guh by teleporting it)
        }
        helper.assertTrue(game != null && game.ghost() != null, "the ghost races along");
        Entity ghost = helper.getLevel().getEntity(game.ghost());
        helper.assertTrue(ghost instanceof RaceGhostEntity && !ghost.isPickable(), "a ghost guh you can't touch");
        game.skipCountdown(npc);
        for (int i = 0; i < 20; i++) {
            game.tick(npc);
        }
        Vec3 expected = game.track().toWorld(new Vec3(0, 0, 5));
        helper.assertTrue(ghost.position().distanceTo(expected) < 0.6, "after 20 ticks the ghost is 5 blocks ahead: " + ghost.position() + " / " + expected);
        RaceGuhEntity mount = mount(helper, player);
        RaceGame.playerGone(player);  // (logging out)
        helper.assertTrue(RaceGame.of(npc) == null && !RaceGame.isRacing(player), "logging out ends the race");
        helper.assertTrue(ghost.isRemoved() && mount.isRemoved() && player.getVehicle() == null, "the race guh and the ghost are gone");
        helper.assertTrue(RaceRecords.best(player) == 3000, "an unfinished race changes no records");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(batch = BATCH, template = TRACK, timeoutTicks = 300)
    public static void raceBackToTheRingAndGettingOffEndsIt(GameTestHelper helper) {
        GuhNpcEntity npc = raceguh(helper);
        ServerPlayer player = racer(helper, npc);
        RaceRole.action(npc, player, RaceRole.START);
        RaceGame game = RaceGame.of(npc);
        if (game != null) {
            game.trustTeleports();                                 // (the test moves the race guh by teleporting it)
        }
        RaceGuhEntity mount = mount(helper, player);
        game.skipCountdown(npc);
        Vec3 start = mount.position();
        mount.teleportTo(start.x, start.y - 15, start.z);             // fell off the world (or the bridge...)
        game.tick(npc);
        helper.assertTrue(game.resets() == 1 && mount.position().distanceTo(start) < 1 && player.getVehicle() == mount,
                "back to the last ring, still riding: " + mount.position());
        for (int i = 0; i < RaceGame.GRACE_TICKS; i++) {                   // (2.10: a moment to catch up after a reset)
            game.tick(npc);
        }
        Vec3 grass = helper.absoluteVec(new Vec3(48.5, 4, 60.5));        // a shortcut over the grass of the infield
        mount.teleportTo(grass.x, grass.y, grass.z);
        for (int i = 0; i <= RaceGame.OFF_TRACK_TICKS; i++) {
            mount.setOnGround(true);
            game.tick(npc);
        }
        helper.assertTrue(game.resets() == 2 && mount.position().distanceTo(start) < 1, "no shortcuts: back to the last ring");
        player.stopRiding();
        for (int i = 0; i <= RaceGame.OFF_GRACE; i++) {
            helper.assertTrue(RaceGame.of(npc) == game, "5 seconds to hop back on (" + i + ")");
            game.tick(npc);
        }
        helper.assertTrue(RaceGame.of(npc) == null && !RaceGame.isRacing(player), "got off for too long: race over");
        helper.assertTrue(mount.isRemoved(), "the race guh is taken back");
        helper.assertTrue(GuhQuests.count(player, RaceFeature.RACEPRIJSJE.get()) == 0 && RaceRecords.best(player) < 0, "no prizes for giving up");
        helper.assertTrue(player.getInventory().isEmpty(), "nothing left in your pockets");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(batch = BATCH, template = TRACK, timeoutTicks = 200)
    public static void raceShoosWildGuhsAndNeverHangsAround(GameTestHelper helper) {
        GuhNpcEntity npc = raceguh(helper);
        ServerPlayer player = racer(helper, npc);
        RaceRole.action(npc, player, RaceRole.START);
        RaceGame game = RaceGame.of(npc);
        if (game != null) {
            game.trustTeleports();                                 // (the test moves the race guh by teleporting it)
        }
        RaceGuhEntity mount = mount(helper, player);
        game.skipCountdown(npc);
        // a wild guh wanders onto the road ahead, a named (someone's) guh stands next to it
        Vec3 ahead = game.track().toWorld(new Vec3(0, 0, 4)), next = game.track().toWorld(new Vec3(2, 0, 6));
        nl.juiced.guhs.entity.GuhEntity wild = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        nl.juiced.guhs.entity.GuhEntity named = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        wild.snapTo(ahead.x, ahead.y, ahead.z, 0, 0);
        named.snapTo(next.x, next.y, next.z, 0, 0);
        named.setCustomName(net.minecraft.network.chat.Component.literal("Gerrit"));
        helper.getLevel().addFreshEntity(wild);
        helper.getLevel().addFreshEntity(named);
        for (int i = 0; i < 20; i++) {
            game.tick(npc);
        }
        helper.assertTrue(wild.isRemoved(), "the wild guh on the racebaan poofed away");
        helper.assertTrue(!named.isRemoved(), "a named guh stays");
        named.discard();
        // teleported far away: the race guh and the Raceguh stop ticking -> the race still ends
        game.stallForTest(100);
        RaceGame.checkStale(player);
        helper.assertTrue(RaceGame.of(npc) == null && !RaceGame.isRacing(player) && mount.isRemoved(), "a race that stopped ticking is over");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(batch = BATCH, template = TRACK, timeoutTicks = 200)
    public static void racebaanIsProtected(GameTestHelper helper) {
        GuhNpcEntity npc = raceguh(helper);
        RaceTrack track = RaceTrack.of(npc);
        helper.assertTrue(track != null && track.facing == Direction.EAST, "the Raceguh finds her track");
        BlockPos road = BlockPos.containing(track.startPos()).below();
        Player walker = helper.makeMockPlayer(GameType.SURVIVAL);
        Player builder = helper.makeMockPlayer(GameType.CREATIVE);
        helper.assertTrue(RaceProtection.denied(walker, road), "no digging in the racebaan");
        helper.assertTrue(RaceProtection.denied(walker, BlockPos.containing(track.centre(3))), "not in the guh head either");
        helper.assertTrue(!RaceProtection.denied(builder, road), "builders in creative may");
        helper.assertTrue(!RaceProtection.protectedAt(helper.getLevel(), road.offset(400, 0, 400)), "far away is not protected");
        helper.succeed();
    }

    @GuhTest(batch = BATCH, template = EMPTY)
    public static void raceguhSellsTheJockeyOutfit(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 1, 2));
        npc.setKind(GuhNpcEntity.Kind.RACEGUH);
        var offers = npc.getOffers();
        helper.assertTrue(offers.size() == 3, "three pieces: " + offers.size());
        List<GuhClothes> sold = offers.stream().map(o -> ((nl.juiced.guhs.item.GuhClothingItem) o.getResult().getItem()).getClothes()).toList();
        helper.assertTrue(sold.containsAll(List.of(GuhClothes.JOCKEY_PET, GuhClothes.JOCKEY_JASJE, GuhClothes.RACEBRIL)), "the jockey outfit: " + sold);
        helper.assertTrue(offers.stream().allMatch(o -> o.getCostA().is(RaceFeature.RACEPRIJSJE.get()) && o.getCostB().isEmpty()
                && o.getMaxUses() == Integer.MAX_VALUE), "for raceprijsjes only, never sold out");
        helper.assertTrue(GuhClothes.JOCKEY_PET.slot == GuhClothes.Slot.HEAD && GuhClothes.JOCKEY_JASJE.slot == GuhClothes.Slot.BODY
                && GuhClothes.RACEBRIL.slot == GuhClothes.Slot.EYES, "a cap, a jacket and goggles");
        helper.succeed();
    }

    @GuhTest(batch = BATCH, template = EMPTY, timeoutTicks = 120)
    public static void raceGuhBoostsOnAPadAndNeverStaysBehind(GameTestHelper helper) {
        RaceGuhEntity guh = RaceFeature.RACE_GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(guh != null && !guh.getType().canSerialize(), "a race guh is never saved");
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 1, 2.5));
        guh.snapTo(at.x, at.y, at.z, 0, 0);
        guh.setUpForRace();
        helper.getLevel().addFreshEntity(guh);
        helper.assertTrue(guh.isSaddled() && guh.isInvulnerable() && !guh.isTame(), "saddled, can't be hurt, and not yours");
        helper.setBlock(new BlockPos(2, 1, 2), RaceFeature.RACE_PAD.get());
        helper.assertTrue(guh.onPad(), "it feels the VAHOEG pad");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.zza = 1;
        guh.setFrozen(true);
        helper.assertTrue(guh.getRiddenInput(player, Vec3.ZERO).equals(Vec3.ZERO), "frozen during the countdown");
        guh.setFrozen(false);
        guh.getRiddenInput(player, Vec3.ZERO);
        helper.assertTrue(guh.getRaceSpeed() >= RaceGuhEntity.BOOST_SPEED, "VAHOEG: a boost");
        guh.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getVehicle() == null, "only its racer can get on");
        helper.assertTrue(!guh.isFood(new ItemStack(ModItems.KAAS_KNABBELS.get())), "no taming a race guh with kaasknabbels");
        // without a race it trots back to the pit stop by itself
        helper.succeedWhen(() -> helper.assertTrue(guh.isRemoved(), "a race guh without a race goes away"));
    }

    @GuhTest(batch = BATCH, template = EMPTY)
    public static void raceTimesMedalsAndTheTrackFrame(GameTestHelper helper) {
        helper.assertTrue(RaceRecords.time(0).equals("0:00.00") && RaceRecords.time(1234).equals("1:01.70") && RaceRecords.time(-1).equals("-"),
                "race times: " + RaceRecords.time(1234));
        helper.assertTrue(RaceRecords.delta(-8).equals("-0.40") && RaceRecords.delta(25).equals("+1.25"), "differences: " + RaceRecords.delta(25));
        helper.assertTrue(RaceGame.Medal.of(20 * 60) == RaceGame.Medal.GOUD && RaceGame.Medal.of(20 * 80) == RaceGame.Medal.ZILVER
                && RaceGame.Medal.of(20 * 100) == RaceGame.Medal.BRONS && RaceGame.Medal.of(20 * 200) == RaceGame.Medal.FINISH, "medals by time");
        helper.assertTrue(RaceGame.Medal.GOUD.prizes > RaceGame.Medal.ZILVER.prizes && RaceGame.Medal.ZILVER.prizes > RaceGame.Medal.BRONS.prizes
                && RaceGame.Medal.BRONS.prizes > RaceGame.Medal.FINISH.prizes && RaceGame.Medal.FINISH.prizes > 0, "faster = more raceprijsjes");
        // a ghost recorded on one track drives the same on a turned one
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            RaceTrack track = new RaceTrack(new BlockPos(10, 64, 10), facing, List.of(), new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1));
            Vec3 p = new Vec3(13.25, 65, 4.5);
            helper.assertTrue(track.toWorld(track.toLocal(p)).distanceTo(p) < 1e-6, "there and back again (" + facing + ")");
            helper.assertTrue(track.toWorld(new Vec3(0, 0, 2)).distanceTo(track.startPos().add(facing.getStepX() * 2, 0, facing.getStepZ() * 2)) < 1e-6,
                    "forward is the way the start faces (" + facing + ")");
            BlockPos corner = RaceTrack.templateToWorld(new BlockPos(0, 0, 0), facing, RaceTrack.TEMPLATE_START.east(3));
            helper.assertTrue(corner.equals(new BlockPos(facing.getStepX() * 3, 0, facing.getStepZ() * 3)), "the template turns with the start");
        }
        // records: the first race is a record, a slower one isn't (but the races count)
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(RaceRecords.save(player, 2000, 600, new int[]{1}, new int[]{1, 2, 3}) && RaceRecords.best(player) == 2000, "first record");
        helper.assertTrue(!RaceRecords.save(player, 2100, 500, new int[]{2}, new int[]{4, 5, 6}) && RaceRecords.best(player) == 2000
                && RaceRecords.bestLap(player) == 500 && RaceRecords.races(player) == 2 && RaceRecords.ghost(player)[0] == 1, "slower: no new ghost, but a best lap");
        helper.succeed();
    }

    /** The rider's game moves the race guh, so the server checks it: no false start, no flying around the track. */
    @GuhTest(batch = BATCH, template = TRACK, timeoutTicks = 200)
    public static void raceGuhCantCheat(GameTestHelper helper) {
        GuhNpcEntity npc = raceguh(helper);
        ServerPlayer player = racer(helper, npc);
        RaceRole.action(npc, player, RaceRole.START);
        RaceGame game = RaceGame.of(npc);
        helper.assertTrue(game != null, "the race is on");
        RaceGuhEntity mount = mount(helper, player);
        Vec3 start = game.track().startPos();
        mount.teleportTo(start.x + 4, start.y, start.z);                 // off before VAHOEG!
        game.tick(npc);
        helper.assertTrue(mount.position().distanceTo(start) < 0.1, "the race guh waits on the line: " + mount.position());
        game.skipCountdown(npc);
        game.tick(npc);
        mount.teleportTo(start.x + 8, start.y, start.z);                 // 8 blocks in one tick
        game.tick(npc);
        helper.assertTrue(game.resets() == 1 && mount.position().distanceTo(start) < 1, "way too fast: back to the last ring");
        RaceGame.playerGone(player);
        leave(helper, player);
        helper.succeed();
    }

    /**
     * 2.10: no more "Oepsie!" loop. After a reset the rider's game may still send a few old positions (down where it fell):
     * during the grace that doesn't send it back again, and more resets soon after one don't fill the chat.
     */
    @GuhTest(batch = BATCH, template = TRACK, timeoutTicks = 300)
    public static void raceResetHasAGraceAndNoChatSpam(GameTestHelper helper) {
        GuhNpcEntity npc = raceguh(helper);
        ServerPlayer player = racer(helper, npc);
        RaceRole.action(npc, player, RaceRole.START);
        RaceGame game = RaceGame.of(npc);
        helper.assertTrue(game != null, "the race is on");
        RaceGuhEntity mount = mount(helper, player);
        game.skipCountdown(npc);
        game.tick(npc);
        Vec3 start = mount.position();
        Vec3 hole = new Vec3(start.x, start.y - 15, start.z);
        mount.teleportTo(hole.x, hole.y, hole.z);                         // fell off
        game.tick(npc);
        helper.assertTrue(game.resets() == 1 && game.grace() == RaceGame.GRACE_TICKS && mount.position().distanceTo(start) < 1,
                "back on the road, with a grace: " + mount.position());
        helper.assertTrue(game.resetChats() == 1, "one message in the chat");
        // the rider's game still sends the old spot a few times (without the speed check: that jump is no cheat)
        for (int i = 0; i < 5; i++) {
            mount.teleportTo(hole.x, hole.y, hole.z);
            game.tick(npc);
        }
        helper.assertTrue(game.resets() == 1 && RaceGame.of(npc) == game, "late old positions don't reset it again: " + game.resets());
        mount.teleportTo(start.x, start.y, start.z);                      // the rider's game caught up
        mount.setOnGround(true);
        while (game.grace() > 0) {
            game.tick(npc);
        }
        game.tick(npc);
        helper.assertTrue(game.resets() == 1, "and after the grace it drives on");
        mount.teleportTo(hole.x, hole.y, hole.z);                         // fell off again, right away
        game.tick(npc);
        helper.assertTrue(game.resets() == 2 && game.resetChats() == 1, "a second fall so soon: only above the hotbar ("
                + game.resetChats() + " chat messages)");
        // a safe spot only: never in the air over a jump or in the saus
        BlockPos road = BlockPos.containing(start).below();
        helper.assertTrue(RaceGame.safeSpot(helper.getLevel(), start.add(0, 2, 0)) != null
                && Math.abs(RaceGame.safeSpot(helper.getLevel(), start.add(0, 2, 0)).y - start.y) < 0.01, "on the road is a safe spot");
        helper.assertTrue(RaceGame.safeSpot(helper.getLevel(), start.add(0, 30, 0)) == null, "high in the air is not");
        helper.assertTrue(helper.getLevel().getBlockState(road).is(RaceGame.TRACK_BLOCKS), "(the start is on the road)");
        RaceGame.playerGone(player);
        leave(helper, player);
        helper.succeed();
    }

    // --- 2.9: levels, the golden ghost -------------------------------------------------------------------------------------

    /** Lastig on the old racebaan: its own boards and records, 50 % more raceprijsjes, Mika-pikkers along the track. */
    @GuhTest(batch = "guhrace_lastig", template = TRACK, timeoutTicks = 200)
    public static void raceLastigHasItsOwnBoardsAndMikaPikkers(GameTestHelper helper) {
        GuhNpcEntity npc = raceguh(helper);
        ServerPlayer player = racer(helper, npc);
        RaceRole.action(npc, player, RaceRole.START_LASTIG);
        RaceGame game = RaceGame.of(npc);
        helper.assertTrue(game != null && game.niveau() == nl.juiced.guhs.feature.spelen.Niveau.LASTIG && game.baan() == RaceBaan.RACEBAAN,
                "a lastig race on the racebaan");
        game.trustTeleports();
        helper.assertTrue(RaceGame.isRacing(player) && nl.juiced.guhs.feature.Minigames.RACE.equals(nl.juiced.guhs.feature.Minigames.playing(player)),
                "it's the old racebaan's game");
        RaceGuhEntity mount = mount(helper, player);
        helper.assertTrue(mount.niveau() == nl.juiced.guhs.feature.spelen.Niveau.LASTIG, "a lastig race guh");
        int pikkers = helper.getLevel().getEntitiesOfClass(nl.juiced.guhs.feature.circuit.MikaPikkerEntity.class, helper.getBounds().inflate(8),
                Entity::isAlive).size();
        helper.assertTrue(pikkers == 3, "lastig: 3 Mika-pikkers along the racebaan: " + pikkers);
        boolean silver = false;
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(99, 12, 99)))) {
            var state = helper.getLevel().getBlockState(pos);
            if (state.is(RaceFeature.RACE_PAD.get()) && !state.getValue(RaceBlocks.LASTIG)) {
                silver = true;
                break;
            }
        }
        helper.assertTrue(silver, "the racebaan has silver pads (they don't work on lastig)");
        game.skipCountdown(npc);
        for (int lap = 0; lap < RaceGame.LAPS; lap++) {
            for (int gate : new int[]{1, 2, 3, 4, 5, 0}) {
                through(game, npc, mount, gate);
            }
        }
        helper.assertTrue(RaceGame.of(npc) == null, "finished");
        int prizes = GuhQuests.count(player, RaceFeature.RACEPRIJSJE.get());
        helper.assertTrue(prizes == nl.juiced.guhs.feature.spelen.Niveau.LASTIG.munten(RaceGame.Medal.GOUD.prizes) + RaceGame.FIRST_PRIZES,
                "gold on lastig (9) + the first race bag (4): " + prizes);
        helper.assertTrue(RaceRecords.best(player, "_racebaan_lastig") > 0 && RaceRecords.best(player) < 0, "a lastig record, the medium one untouched");
        helper.assertTrue(Scorebord.top(player.level().getServer(), "race_total_lastig").stream().anyMatch(e -> e.player().equals(player.getUUID()))
                && Scorebord.top(player.level().getServer(), "race_lap_lastig").stream().anyMatch(e -> e.player().equals(player.getUUID())), "on the lastig boards");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(nl.juiced.guhs.feature.circuit.MikaPikkerEntity.class, helper.getBounds().inflate(8),
                Entity::isAlive).isEmpty(), "the Mika-pikkers go away with the race");
        leave(helper, player);
        helper.succeed();
    }

    /** Makkelijk ("wide rails"): longer off the road before it counts, and then only back a little way. */
    @GuhTest(batch = "guhrace_makkelijk", template = TRACK, timeoutTicks = 300)
    public static void raceMakkelijkGoesBackOnlyALittle(GameTestHelper helper) {
        GuhNpcEntity npc = raceguh(helper);
        ServerPlayer player = racer(helper, npc);
        RaceRole.action(npc, player, RaceRole.START_MAKKELIJK);
        RaceGame game = RaceGame.of(npc);
        helper.assertTrue(game != null && game.niveau() == nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK, "a makkelijk race");
        game.trustTeleports();
        RaceGuhEntity mount = mount(helper, player);
        game.skipCountdown(npc);
        Vec3 start = game.track().startPos();
        for (int i = 0; i < 70; i++) {                                    // along the start straight
            Vec3 p = game.track().toWorld(new Vec3(0, 0, i * 0.4));
            mount.teleportTo(p.x, p.y, p.z);
            mount.setOnGround(true);
            game.tick(npc);
        }
        Vec3 grass = helper.absoluteVec(new Vec3(48.5, 4, 60.5));
        mount.teleportTo(grass.x, grass.y, grass.z);
        for (int i = 0; i <= RaceGame.OFF_TRACK_TICKS; i++) {
            mount.setOnGround(true);
            game.tick(npc);
        }
        helper.assertTrue(game.resets() == 0, "makkelijk: a moment on the grass is fine");
        for (int i = 0; i <= RaceGame.OFF_TRACK_TICKS_MAKKELIJK; i++) {
            mount.setOnGround(true);
            game.tick(npc);
        }
        helper.assertTrue(game.resets() == 1, "too long on the grass: back");
        Vec3 now = mount.position();
        helper.assertTrue(now.distanceTo(start) > 3 && helper.getLevel().getBlockState(BlockPos.containing(now).below()).is(RaceGame.TRACK_BLOCKS),
                "back only a little way, on the road (not to the last ring): " + now);
        RaceGame.playerGone(player);
        leave(helper, player);
        helper.succeed();
    }

    /** The golden ghost: the world's track record (someone else's) drives along in gold, unless you switch it off. */
    @GuhTest(batch = "guhrace_goud", template = TRACK, timeoutTicks = 200)
    public static void raceGoldenGhostDrivesTheTrackRecord(GameTestHelper helper) {
        GuhNpcEntity npc = raceguh(helper);
        ServerPlayer holder = racer(helper, npc);
        ServerPlayer player = racer(helper, npc);
        int[] recording = new int[3 * 200];
        for (int i = 0; i < 200; i++) {
            recording[i * 3 + 2] = i * 8;                                   // straight ahead, half a block per sample
        }
        RaceGeesten.forget(player.level().getServer(), RaceRole.BOARD_TOTAL);
        helper.assertTrue(RaceGeesten.offer(holder, RaceRole.BOARD_TOTAL, 1500, recording), "a track record with its race");
        helper.assertTrue(!RaceGeesten.offer(holder, RaceRole.BOARD_TOTAL, 1600, recording), "a slower race doesn't replace it");
        RaceRole.action(npc, player, RaceRole.START);
        RaceGame game = RaceGame.of(npc);
        helper.assertTrue(game != null && game.goudGhost() != null && game.goudTicks() == 1500, "the golden ghost races along");
        Entity goud = helper.getLevel().getEntity(game.goudGhost());
        helper.assertTrue(goud instanceof RaceGhostEntity g && g.isGoud() && !goud.isPickable(), "a golden ghost guh you can't touch");
        game.trustTeleports();
        game.skipCountdown(npc);
        for (int i = 0; i < 20; i++) {
            game.tick(npc);
        }
        Vec3 expected = game.track().toWorld(new Vec3(0, 0, 5));
        helper.assertTrue(goud.position().distanceTo(expected) < 0.6, "after 20 ticks the golden ghost is 5 blocks ahead: " + goud.position());
        RaceGame.playerGone(player);
        helper.assertTrue(goud.isRemoved(), "the golden ghost goes with the race");
        player.teleportTo(npc.getX(), npc.getY(), npc.getZ() - 2);
        RaceRole.action(npc, player, RaceRole.GOUD);
        helper.assertTrue(!RaceRecords.goudOn(player), "switched off");
        RaceRole.action(npc, player, RaceRole.START);
        RaceGame again = RaceGame.of(npc);
        helper.assertTrue(again != null && again.goudGhost() == null, "no golden ghost when it's off");
        RaceGame.playerGone(player);
        // your own record is no golden ghost for yourself
        holder.teleportTo(npc.getX(), npc.getY(), npc.getZ() - 2);
        RaceRole.action(npc, holder, RaceRole.START);
        RaceGame own = RaceGame.of(npc);
        helper.assertTrue(own != null && own.goudGhost() == null, "the record holder doesn't race their own golden ghost");
        RaceGame.playerGone(holder);
        RaceGeesten.forget(player.level().getServer(), RaceRole.BOARD_TOTAL);
        leave(helper, player, holder);
        helper.succeed();
    }

    /** The race guh per level: lastig turns after the mouse and ignores the silver pads; calmer on makkelijk. */
    @GuhTest(batch = BATCH, template = EMPTY)
    public static void raceGuhPerLevel(GameTestHelper helper) {
        RaceGuhEntity guh = RaceFeature.RACE_GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 1, 2.5));
        guh.snapTo(at.x, at.y, at.z, 0, 0);
        guh.setUpForRace();
        helper.getLevel().addFreshEntity(guh);
        helper.setBlock(new BlockPos(2, 1, 2), RaceFeature.RACE_PAD.get().defaultBlockState().setValue(RaceBlocks.LASTIG, false));
        helper.assertTrue(guh.onPad(), "a silver pad works on medium");
        guh.setNiveau(nl.juiced.guhs.feature.spelen.Niveau.LASTIG);
        helper.assertTrue(!guh.onPad(), "but not on lastig");
        helper.setBlock(new BlockPos(2, 1, 2), RaceFeature.RACE_PAD.get());
        helper.assertTrue(guh.onPad(), "a golden pad works on lastig too");
        helper.assertTrue(RaceGuhEntity.turnTowards(0, 90) == RaceGuhEntity.LASTIG_TURN && RaceGuhEntity.turnTowards(10, 12) == 12
                && RaceGuhEntity.turnTowards(170, -170) == 170 + RaceGuhEntity.LASTIG_TURN, "lastig turns after your mouse, a bit at a time");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.zza = 1;
        helper.setBlock(new BlockPos(2, 1, 2), net.minecraft.world.level.block.Blocks.AIR);
        float[] top = new float[3];
        for (nl.juiced.guhs.feature.spelen.Niveau n : nl.juiced.guhs.feature.spelen.Niveau.values()) {
            guh.setNiveau(n);
            for (int i = 0; i < 60; i++) {
                guh.getRiddenInput(player, Vec3.ZERO);
            }
            top[n.ordinal()] = guh.getRaceSpeed();
            guh.feel(RaceGuhEntity.SCHOK_BOTS);
        }
        helper.assertTrue(top[0] < top[1] && top[1] < top[2], "calmer on makkelijk, faster on lastig: " + java.util.Arrays.toString(top));
        guh.feel(RaceGuhEntity.SCHOK_BOOST);
        helper.assertTrue(guh.boostTicks() == RaceGuhEntity.BOOST_TICKS, "a boost from the server");
        guh.feel(RaceGuhEntity.SCHOK_PIK);
        helper.assertTrue(guh.boostTicks() == 0 && guh.getRaceSpeed() < RaceGuhEntity.TOP[2], "a Mika pinched it away");
        guh.discard();
        helper.succeed();
    }
}
