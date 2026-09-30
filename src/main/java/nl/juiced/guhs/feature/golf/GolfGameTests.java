package nl.juiced.guhs.feature.golf;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Guh golf: the ball's physics on the little test lanes (golf_testbaan: a plain lane, one with a cup at x=10, one with
 * a kaassaus ditch at x=10-11), and whole games on the real course (guh_golfbaan).
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class GolfGameTests {
    private static final String LANES = "golf_testbaan", COURSE = "guh_golfbaan";
    /** Their own batch: the big course doesn't crowd the other tests. */
    private static final String BATCH = "guhs_golf";

    private static GolfBallEntity ball(GameTestHelper helper, double x, double z) {
        return GolfBallEntity.create(helper.getLevel(), helper.absoluteVec(new Vec3(x, 3, z)), null, null);   // (the template sits 1 up)
    }

    private static String dbg(GameTestHelper helper, GolfBallEntity b) {
        return " [pos " + b.position().subtract(Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO))) + " v " + b.getDeltaMovement() + " moving "
                + b.isMoving() + " ticks " + b.tickCount + " event " + b.lastEvent() + " removed " + b.isRemoved() + " below "
                + helper.getLevel().getBlockState(b.blockPosition().below()) + "]";
    }

    private static GuhNpcEntity golfguh(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(),
                n -> n.getKind() == GuhNpcEntity.Kind.GOLFGUH);
        helper.assertTrue(npcs.size() == 1, "the Golfguh is in her clubhouse: " + npcs.size());
        return npcs.get(0);
    }

    private static int count(Player player, net.minecraft.world.item.Item item) {
        return player.getInventory().countItem(item);
    }

    private static void leave(GameTestHelper helper, ServerPlayer player) {
        GolfGame.leave(player);
        helper.getLevel().removePlayerImmediately(player, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    // --- the Golfguh's shop ---------------------------------------------------------------------------------------------

    @GameTest(template = "empty", batch = BATCH)
    public static void golfguhSellsTheGolfOutfitForGolfballetjes(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 2, 2));
        npc.setKind(GuhNpcEntity.Kind.GOLFGUH);
        var offers = npc.getOffers();
        var results = offers.stream().map(o -> o.getResult().getItem()).toList();
        for (GuhClothes piece : List.of(GuhClothes.GOLF_PET, GuhClothes.GOLF_ZONNEKLEP, GuhClothes.GOLF_TRUI)) {
            helper.assertTrue(results.contains(ModItems.clothingItem(piece)), "sells " + piece);
        }
        helper.assertTrue(offers.size() == 3 && offers.stream().allMatch(o -> o.getCostA().is(GolfFeature.GOLFBALLETJE.get())), "for golfballetjes");
        helper.assertTrue(offers.stream().mapToInt(o -> o.getCostA().getCount()).sum() == 34, "the whole outfit costs 34 golfballetjes");
        helper.succeed();
    }

    // --- the ball ---------------------------------------------------------------------------------------------------------

    @GameTest(template = LANES, timeoutTicks = 200, batch = BATCH)
    public static void golfBallBouncesOffTheWallAndStops(GameTestHelper helper) {
        GolfBallEntity b = ball(helper, 15.5, 2.5);
        helper.runAfterDelay(2, () -> b.hit(new Vec3(0.5, 0, 0)));
        helper.runAfterDelay(4, () -> helper.assertTrue(b.isMoving(), "it rolls " + dbg(helper, b)));
        helper.succeedWhen(() -> {
            helper.assertTrue(!b.isMoving() && b.lastEvent() == GolfBallEntity.BallEvent.STOPPED, "it stops by itself");
            double x = b.getX() - helper.absolutePos(BlockPos.ZERO).getX();
            helper.assertTrue(x < 15.0, "it bounced back off the end wall: x = " + x);
            helper.assertTrue(Math.abs(b.getY() - helper.absolutePos(BlockPos.ZERO).getY() - 3) < 0.01, "and lies on the felt");
            b.discard();
        });
    }

    @GameTest(template = LANES, timeoutTicks = 200, batch = BATCH)
    public static void golfBallDropsIntoTheCupWhenSlow(GameTestHelper helper) {
        GolfBallEntity b = ball(helper, 4.5, 6.5);
        helper.runAfterDelay(2, () -> b.hit(new Vec3(0.55, 0, 0)));
        helper.succeedWhen(() -> {
            helper.assertTrue(b.isSunk() && b.lastEvent() == GolfBallEntity.BallEvent.HOLED, "in the cup");
            helper.assertTrue(b.blockPosition().equals(helper.absolutePos(new BlockPos(10, 2, 6))), "right in the middle of it");
            b.discard();
        });
    }

    @GameTest(template = LANES, timeoutTicks = 100, batch = BATCH)
    public static void golfBallRollsOverTheCupWhenFast(GameTestHelper helper) {
        GolfBallEntity b = ball(helper, 4.5, 6.5);
        helper.runAfterDelay(2, () -> b.hit(new Vec3(1.3, 0, 0)));
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(!b.isSunk(), "much too fast to drop in" + dbg(helper, b));
            helper.assertTrue(b.getX() - helper.absolutePos(BlockPos.ZERO).getX() > 12, "it rolled right over the cup" + dbg(helper, b));
            b.discard();
            helper.succeed();
        });
    }

    @GameTest(template = LANES, timeoutTicks = 100, batch = BATCH)
    public static void golfBallSplashesIntoTheKaassaus(GameTestHelper helper) {
        GolfBallEntity b = ball(helper, 4.5, 10.5);
        helper.runAfterDelay(2, () -> b.hit(new Vec3(0.6, 0, 0)));
        helper.succeedWhen(() -> {
            helper.assertTrue(b.lastEvent() == GolfBallEntity.BallEvent.SAUS, "plons!" + dbg(helper, b));
            b.discard();
        });
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void golfCourseBlocksAreWhereTheBallMayRoll(GameTestHelper helper) {
        helper.assertTrue(GolfBallEntity.isCourse(GolfFeature.VILT.get().defaultBlockState()), "felt");
        helper.assertTrue(GolfBallEntity.isCourse(net.minecraft.world.level.block.Blocks.CHERRY_STAIRS.defaultBlockState()), "ramps");
        helper.assertTrue(!GolfBallEntity.isCourse(net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState()), "not the grass");
        helper.assertTrue(GolfClubItem.power(0) < 0.1f && Math.abs(GolfClubItem.power(GolfClubItem.CYCLE) - 1f) < 1e-4
                && GolfClubItem.power(3 * GolfClubItem.CYCLE) == 1f && GolfClubItem.power(GolfClubItem.CYCLE / 2) < GolfClubItem.power(GolfClubItem.CYCLE),
                "the power bar charges up and stays full");
        List<BlockPos> plus = GolfGame.sails(BlockPos.ZERO.above(5), Direction.SOUTH, true);
        helper.assertTrue(plus.contains(new BlockPos(0, 2, 0)) && !GolfGame.sails(BlockPos.ZERO.above(5), Direction.SOUTH, false).contains(new BlockPos(0, 2, 0)),
                "the lower sail closes the tunnel only half the time");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void golfClubStaysInTheGame(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack club = new ItemStack(GolfFeature.GOLFCLUB.get());
        helper.assertTrue(!club.getItem().onDroppedByPlayer(club, player), "it can't be dropped");
        helper.assertTrue(!club.getItem().canFitInsideContainerItems(), "nor go into a bundle or a shulker box");
        player.getInventory().add(club);
        player.getInventory().tick();
        helper.assertTrue(count(player, GolfFeature.GOLFCLUB.get()) == 0, "someone who isn't golfing loses it at once");
        leave(helper, player);
        helper.succeed();
    }

    // --- a whole round on the real course ------------------------------------------------------------------------------

    @GameTest(template = COURSE, timeoutTicks = 400, batch = BATCH)
    public static void golfRoundWithAHoleInOne(GameTestHelper helper) {
        GuhNpcEntity npc = golfguh(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(npc.getX(), npc.getY(), npc.getZ() + 2);
        GolfGame.action(npc, player, GolfGame.START);
        GolfGame game = GolfGame.of(npc);
        helper.assertTrue(game.complete() && game.hubs().size() == 1, "all 9 tees and cups and the windmill found");
        for (var n : nl.juiced.guhs.feature.spelen.Niveau.values()) {
            for (int h = 0; h < GolfGame.HOLES; h++) {
                helper.assertTrue(game.tee(n, h) != null, "hole " + (h + 1) + " has a " + n.id() + " tee");
            }
        }
        helper.assertTrue(game.bumpers().isEmpty(), "no lastig bumpers on a medium round");
        helper.assertTrue(game.isPlayedBy(player) && GolfGame.isGolfing(player), "the round is on");
        helper.assertTrue(count(player, GolfFeature.GOLFCLUB.get()) == 1 && player.getMainHandItem().is(GolfFeature.GOLFCLUB.get()),
                "with a club in your hand, without bringing anything");
        BlockPos tee = game.tee(0);
        helper.assertTrue(player.position().distanceTo(Vec3.atCenterOf(tee)) < 3, "you're on the first tee");
        GolfBallEntity b = game.ball(helper.getLevel());
        helper.assertTrue(b != null && b.blockPosition().equals(tee.above()), "and so is your ball");
        player.getFoodData().setFoodLevel(3);
        player.hurt(helper.getLevel().damageSources().fall(), 6f);
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "golfers can't get hurt");
        ServerPlayer other = helper.makeMockServerPlayerInLevel();
        other.moveTo(npc.getX() + 1, npc.getY(), npc.getZ() + 1);
        GolfGame.action(npc, other, GolfGame.START);
        helper.assertTrue(game.isPlayedBy(player) && !GolfGame.isGolfing(other), "one round at a time");
        leave(helper, other);
        game.testSkipCountdown();
        BlockPos hub = game.hubs().get(0);
        boolean[] sails = new boolean[2];                  // the windmill's lower sail: down (tunnel closed) and up (open)
        for (int t = 1; t <= 42; t++) {
            helper.runAfterDelay(t, () -> sails[helper.getLevel().getBlockState(hub.below()).is(GolfFeature.WIEK.get()) ? 0 : 1] = true);
        }
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(game.phase() == GolfGame.Phase.AIM, "the countdown is over");
            // a putt of 2 blocks straight at the cup of hole 1 (the lane goes west)
            Direction way = helper.getLevel().getBlockState(tee).getValue(GolfBlocks.Afslag.FACING);
            BlockPos cup = game.cup(0);
            Vec3 spot = new Vec3(cup.getX() + 0.5 - way.getStepX() * 2, cup.getY() + 1, cup.getZ() + 0.5 - way.getStepZ() * 2);
            b.resetTo(spot);
            player.moveTo(spot.x - way.getStepX() * 1.5, spot.y, spot.z - way.getStepZ() * 1.5, way.toYRot(), 30);
        });
        helper.runAfterDelay(8, () -> GolfGame.swing(player, 0.31f));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(game.score(0) == 1 && game.phase() == GolfGame.Phase.HOLED, "a hole-in-one! " + game.score(0) + " " + game.phase());
            helper.assertTrue(count(player, GolfFeature.GOLFBALLETJE.get()) == 6, "6 golfballetjes for it");
            helper.assertTrue(player.getFoodData().getFoodLevel() == 20, "and nobody gets hungry");
        });
        helper.runAfterDelay(43, () -> {
            helper.assertTrue(sails[0] && sails[1], "the windmill turns: its tunnel is open half the time");
            for (int hole = 1; hole < GolfGame.HOLES; hole++) {
                game.testHoleOut(npc, player, GolfGame.PAR[hole]);
            }
            helper.assertTrue(!game.isRunning() && !GolfGame.isGolfing(player), "9 holes: the round is over");
            helper.assertTrue(count(player, GolfFeature.GOLFCLUB.get()) == 0, "the club went back to the Golfguh");
            helper.assertTrue(GolfGame.best(player) == 26, "a record of 26: " + GolfGame.best(player));
            // 6 (ace) + 8 x 3 (par) + 3 (a whole round) + 4 (at par or better) + 6 (the very first round)
            helper.assertTrue(count(player, GolfFeature.GOLFBALLETJE.get()) == 43, "golfballetjes: " + count(player, GolfFeature.GOLFBALLETJE.get()));
            helper.assertTrue(count(player, ModItems.KAAS_KNABBELS.get()) == 16, "and a present for the first round");
            helper.assertTrue(GuhQuests.saved(player).getInt("guhs_golf_aces") == 1 && GuhQuests.saved(player).getInt("guhs_golf_rounds") == 1, "counted");
            helper.assertTrue(nl.juiced.guhs.quest.Scorebord.top(helper.getLevel().getServer(), GolfGame.BOARD).stream()
                    .anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == 26), "on the world's top 3");
            GolfGame.showScores(npc);
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, npc.getBoundingBox().inflate(4),
                    d -> d.getTags().contains(nl.juiced.guhs.quest.Scorebord.TAG)).isEmpty(), "the top 3 floats above the Golfguh");
            helper.assertTrue(player.distanceTo(npc) < 5, "and you're back at the Golfguh");
            helper.assertTrue(game.ball(helper.getLevel()) == null, "the ball is gone");
            leave(helper, player);
            helper.succeed();
        });
    }

    /**
     * Hole 4: straight into the kaassaus costs a penalty stroke and the ball goes back. Hole 5, De Vahoegschans: a good
     * hit rolls up the ramp and the slime pad throws the ball over the ditch. Then the golfer walks off: game over.
     */
    @GameTest(template = COURSE, timeoutTicks = 400, batch = BATCH)
    public static void golfKaassausVahoegschansAndWalkingAway(GameTestHelper helper) {
        GuhNpcEntity npc = golfguh(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(npc.getX(), npc.getY(), npc.getZ() + 2);
        GolfGame.action(npc, player, GolfGame.START);
        GolfGame game = GolfGame.of(npc);
        game.testSkipCountdown();
        helper.runAfterDelay(5, () -> {
            for (int hole = 0; hole < 3; hole++) {
                game.testHoleOut(npc, player, 3);
            }
            helper.assertTrue(game.hole() == 3 && game.phase() == GolfGame.Phase.AIM, "on to hole 4, the Kaassausmoeras");
            behindTee(helper, player, game.tee(3));
            GolfGame.swing(player, 0.45f);                     // straight into the pond
            helper.assertTrue(game.strokes() == 1 && game.phase() == GolfGame.Phase.ROLLING, "hit");
        });
        helper.runAfterDelay(45, () -> helper.assertTrue(game.phase() == GolfGame.Phase.HAZARD && game.strokes() == 2, "plons: a penalty stroke "
                + game.phase() + " " + game.strokes()));
        BlockPos[] tee = {null};
        helper.runAfterDelay(90, () -> {
            GolfBallEntity b = game.ball(helper.getLevel());
            helper.assertTrue(game.phase() == GolfGame.Phase.AIM && b != null && b.blockPosition().equals(game.tee(3).above()),
                    "and the ball is back on the tee");
            game.testHoleOut(npc, player, 4);
            helper.assertTrue(game.hole() == 4, "on to hole 5, De Vahoegschans");
            tee[0] = game.tee(4);
            behindTee(helper, player, tee[0]);
            GolfGame.swing(player, 0.8f);
        });
        helper.runAfterDelay(200, () -> {
            if (game.hole() > 4) {                              // (straight in: that happens!)
                helper.assertTrue(game.score(4) == 1, "a hole-in-one over the kaassaus");
            } else {
                GolfBallEntity b = game.ball(helper.getLevel());
                Direction way = helper.getLevel().getBlockState(tee[0]).getValue(GolfBlocks.Afslag.FACING);
                double gone = b == null ? 0 : (b.getX() - tee[0].getX() - 0.5) * way.getStepX() + (b.getZ() - tee[0].getZ() - 0.5) * way.getStepZ();
                helper.assertTrue(game.strokes() == 1 && game.phase() != GolfGame.Phase.HAZARD, "no penalty: it flew over the kaassaus "
                        + game.phase() + " " + game.strokes());
                helper.assertTrue(game.phase() == GolfGame.Phase.HOLED || gone > 13.5, "and landed on the other side: " + gone);
            }
            player.moveTo(npc.getX() + 120, npc.getY(), npc.getZ());
        });
        helper.runAfterDelay(204, () -> {
            helper.assertTrue(!game.isRunning() && !GolfGame.isGolfing(player), "walking away ends it");
            helper.assertTrue(count(player, GolfFeature.GOLFCLUB.get()) == 0, "and the club goes back");
            leave(helper, player);
            helper.succeed();
        });
    }

    /** Holing out in the cup of another hole doesn't count: a penalty stroke and the ball goes back. */
    @GameTest(template = COURSE, timeoutTicks = 200, batch = BATCH)
    public static void golfWrongCupCostsAPenalty(GameTestHelper helper) {
        GuhNpcEntity npc = golfguh(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(npc.getX(), npc.getY(), npc.getZ() + 2);
        GolfGame.action(npc, player, GolfGame.START);
        GolfGame game = GolfGame.of(npc);
        game.testSkipCountdown();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(game.hole() == 0 && game.phase() == GolfGame.Phase.AIM, "on hole 1");
            BlockPos cup = game.cup(1);                        // the cup of hole 2 (its lane goes east there)
            Vec3 spot = new Vec3(cup.getX() - 1.5, cup.getY() + 1, cup.getZ() + 0.5);
            game.ball(helper.getLevel()).resetTo(spot);
            player.moveTo(spot.x - 1.5, spot.y, spot.z, Direction.EAST.toYRot(), 30);
        });
        helper.runAfterDelay(8, () -> GolfGame.swing(player, 0.31f));
        helper.runAfterDelay(40, () -> helper.assertTrue(game.hole() == 0 && game.phase() == GolfGame.Phase.HAZARD && game.strokes() == 2
                && game.score(0) == 0, "the wrong cup: a penalty stroke " + game.phase() + " " + game.strokes()));
        helper.runAfterDelay(80, () -> {
            GolfBallEntity b = game.ball(helper.getLevel());
            helper.assertTrue(game.phase() == GolfGame.Phase.AIM && b != null && !b.isSunk(), "and the ball is back out of it");
            leave(helper, player);
            helper.assertTrue(count(player, GolfFeature.GOLFCLUB.get()) == 0 && !GolfGame.isGolfing(player), "leaving takes the club back");
            helper.succeed();
        });
    }

    private static void behindTee(GameTestHelper helper, ServerPlayer player, BlockPos tee) {
        Direction way = helper.getLevel().getBlockState(tee).getValue(GolfBlocks.Afslag.FACING);
        player.moveTo(tee.getX() + 0.5 - way.getStepX() * 1.5, tee.getY() + 1, tee.getZ() + 0.5 - way.getStepZ() * 1.5, way.toYRot(), 30);
    }

    // --- 2.9: makkelijk / medium / lastig -----------------------------------------------------------------------------------

    /** 2.10: the lastig bumpers turn with the course (it is only ever turned, never mirrored), from the cup of the hole. */
    @GameTest(template = "empty", batch = BATCH)
    public static void golfBumpersTurnWithTheCourse(GameTestHelper helper) {
        // hole 1 in the template: cup (12, 1, 84), its medium tee faces west; bumper (27, 83) is 15 east, 1 north of the cup, 1 up
        BlockPos cup = new BlockPos(100, 64, 100);
        List<BlockPos> same = GolfBanen.lastigBumpers(0, cup, Direction.WEST);
        helper.assertTrue(same.get(0).equals(cup.offset(15, 1, -1)), "not turned: as in the template " + same.get(0));
        // a quarter turn clockwise: west becomes north, east (+x) becomes south (+z), north (-z) becomes east (+x)
        List<BlockPos> turned = GolfBanen.lastigBumpers(0, cup, Direction.NORTH);
        helper.assertTrue(turned.get(0).equals(cup.offset(1, 1, 15)), "a quarter turn " + turned.get(0));
        List<BlockPos> half = GolfBanen.lastigBumpers(0, cup, Direction.EAST);
        helper.assertTrue(half.get(0).equals(cup.offset(-15, 1, 1)), "half a turn " + half.get(0));
        // hole 8's cup is on the hill (one up): its bumpers are on the floor next to the hill (at the cup's height)
        helper.assertTrue(GolfBanen.lastigBumpers(7, cup, Direction.WEST).get(0).getY() == cup.getY(), "hole 8: next to the hill");
        int spots = 0;
        for (int h = 0; h < GolfGame.HOLES; h++) {
            spots += GolfBanen.lastigBumpers(h, cup, Direction.SOUTH).size();
        }
        helper.assertTrue(spots == 28, "28 extra bumpers on lastig: " + spots);
        helper.assertTrue(GolfBanen.totalPar(nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK) == 18
                && GolfBanen.totalPar(nl.juiced.guhs.feature.spelen.Niveau.MEDIUM) == 27
                && GolfBanen.totalPar(nl.juiced.guhs.feature.spelen.Niveau.LASTIG) == 36 && GolfGame.TOTAL_PAR == 27, "par 18 / 27 / 36");
        helper.succeed();
    }

    /**
     * Lastig: 6 strokes a hole from the far (red) tees, its own par, extra slime bumpers on the course during the round (gone
     * after it), no wind any more, its own board and record, and half as many golfballetjes more.
     */
    @GameTest(template = COURSE, timeoutTicks = 200, batch = BATCH)
    public static void golfLastigFarTeesBumpersSixStrokes(GameTestHelper helper) {
        GuhNpcEntity npc = golfguh(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(npc.getX(), npc.getY(), npc.getZ() + 2);
        var lastig = nl.juiced.guhs.feature.spelen.Niveau.LASTIG;
        GolfGame.action(npc, player, nl.juiced.guhs.feature.klassiekers.Klassiekers.metNiveau(GolfGame.START, lastig));
        GolfGame game = GolfGame.of(npc);
        helper.assertTrue(game.isPlayedBy(player) && game.niveau() == lastig, "a round on lastig");
        helper.assertTrue(GolfGame.maxSlagen(lastig) == 6 && GolfGame.maxSlagen(nl.juiced.guhs.feature.spelen.Niveau.MEDIUM) == GolfGame.MAX_STROKES
                && GolfGame.maxSlagen(nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK) == 10, "10 / 8 / 6 strokes");
        BlockPos far = game.tee(0), medium = game.tee(nl.juiced.guhs.feature.spelen.Niveau.MEDIUM, 0), cup = game.cup(0);
        helper.assertTrue(far.equals(game.tee(lastig, 0))
                && helper.getLevel().getBlockState(far).getValue(GolfBlocks.NIVEAU) == GolfBlocks.TeeNiveau.LASTIG, "hole 1 from the red lastig tee");
        helper.assertTrue(far.distSqr(cup) > medium.distSqr(cup), "which is further from the cup than the medium one");
        GolfBallEntity b = game.ball(helper.getLevel());
        helper.assertTrue(b != null && b.blockPosition().equals(far.above()), "the ball is on it");
        helper.assertTrue(game.par(0) == 3 && game.totalPar() == 36, "par 3 on the first hole, 36 in all: " + game.totalPar());
        helper.assertTrue(game.bumpers().size() == 28, "28 extra slime bumpers on the course: " + game.bumpers().size());
        List<BlockPos> bumpers = List.copyOf(game.bumpers());
        helper.assertTrue(bumpers.stream().allMatch(p -> helper.getLevel().getBlockState(p).is(nl.juiced.guhs.registry.ModBlocks.ROZE_SLIJMBLOK.get())
                && GolfBallEntity.isCourse(helper.getLevel().getBlockState(p.below()))), "pink slime, right on the course");
        game.testSkipCountdown();
        helper.runAfterDelay(5, () -> {
            for (int hole = 0; hole < GolfGame.HOLES; hole++) {
                game.testHoleOut(npc, player, game.par(hole));
            }
            helper.assertTrue(!game.isRunning(), "9 holes: the round is over");
            helper.assertTrue(bumpers.stream().allMatch(p -> helper.getLevel().getBlockState(p).isAir()) && game.bumpers().isEmpty(),
                    "the Golfguh took her extra bumpers away again");
            // 9 x par (3 balletjes, lastig 5) + the round bonus 3 + 4 at par (7, lastig 11) + 6 for the very first round
            int expected = 9 * lastig.munten(3) + lastig.munten(GolfGame.roundBonus(0, false)) + GolfGame.FIRST_BALLS;
            helper.assertTrue(expected == 62, "the sum: " + expected);
            helper.assertTrue(count(player, GolfFeature.GOLFBALLETJE.get()) == expected, "golfballetjes on lastig: " + count(player, GolfFeature.GOLFBALLETJE.get()));
            helper.assertTrue(GolfGame.best(player, lastig) == 36 && GolfGame.best(player) == -1, "the lastig record (par 36), medium untouched");
            helper.assertTrue(nl.juiced.guhs.quest.Scorebord.top(helper.getLevel().getServer(), "golf_rondje_lastig").stream()
                    .anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == 36), "on the lastig board");
            helper.assertTrue(nl.juiced.guhs.feature.klassiekers.Klassiekers.done(player, "grote_guhspelen/klassiekers_golf_lastig"), "the lastig advancement");
            var adv = player.server.getAdvancements().get(Guhs.id("quest/golf_lastig_par"));
            helper.assertTrue(adv != null && player.getAdvancements().getOrStartProgress(adv).isDone(), "at par from the far tees: golf_lastig_par");
            leave(helper, player);
            helper.succeed();
        });
    }

    /** Makkelijk: the green tees close to the cup, par 2 each, 10 strokes, and the wrong cup (or kaassaus) costs no penalty stroke: the ball just goes back. */
    @GameTest(template = COURSE, timeoutTicks = 200, batch = BATCH)
    public static void golfMakkelijkHasNoPenaltyStrokes(GameTestHelper helper) {
        GuhNpcEntity npc = golfguh(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(npc.getX(), npc.getY(), npc.getZ() + 2);
        GolfGame.action(npc, player, nl.juiced.guhs.feature.klassiekers.Klassiekers.metNiveau(GolfGame.START, nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK));
        GolfGame game = GolfGame.of(npc);
        var makkelijk = nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK;
        helper.assertTrue(game.niveau() == makkelijk, "makkelijk");
        BlockPos near = game.tee(0), medium = game.tee(nl.juiced.guhs.feature.spelen.Niveau.MEDIUM, 0), cup0 = game.cup(0);
        helper.assertTrue(near.equals(game.tee(makkelijk, 0)) && near.distSqr(cup0) < medium.distSqr(cup0), "from the green tee, close to the cup");
        helper.assertTrue(game.par(0) == 2 && game.totalPar() == 18 && game.bumpers().isEmpty(), "par 2 each (18 in all), no extra bumpers");
        game.testSkipCountdown();
        helper.runAfterDelay(5, () -> {
            BlockPos cup = game.cup(1);                        // the cup of hole 2 (its lane goes east there)
            Vec3 spot = new Vec3(cup.getX() - 1.5, cup.getY() + 1, cup.getZ() + 0.5);
            game.ball(helper.getLevel()).resetTo(spot);
            player.moveTo(spot.x - 1.5, spot.y, spot.z, Direction.EAST.toYRot(), 30);
        });
        helper.runAfterDelay(8, () -> GolfGame.swing(player, 0.31f));
        helper.runAfterDelay(40, () -> helper.assertTrue(game.phase() == GolfGame.Phase.HAZARD && game.strokes() == 1,
                "the wrong cup on makkelijk: back, but no penalty stroke " + game.phase() + " " + game.strokes()));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(game.phase() == GolfGame.Phase.AIM && game.strokes() == 1, "and on you go");
            leave(helper, player);
            helper.succeed();
        });
    }
}
