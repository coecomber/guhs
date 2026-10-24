package nl.juiced.guhs.feature.golf;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.7.0 "Rondje Guhmension": the back 9 of the guh golf course. The rewards and pars (no world needed), the new ball
 * physics on the little test lanes (golf_testbaan: ijsvilt and kaasmodder, the bank corner, the looping), the golf shop,
 * and whole rounds on the 18-hole course (guh_golfbaan18): locked until the front 9 went at par, all 18 holes and the
 * moving obstacles found, the trophy.
 */
public class Golf18GameTests {
    private static final String LANES = "golf_testbaan", COURSE18 = "guh_golfbaan18";
    private static final String BATCH = "guhs_golf18";

    private static GolfBallEntity ball(GameTestHelper helper, double x, double z) {
        return GolfBallEntity.create(helper.getLevel(), helper.absoluteVec(new Vec3(x, 3, z)), null, null);   // (the template sits 1 up)
    }

    private static double rx(GameTestHelper helper, GolfBallEntity b) {
        return b.getX() - helper.absolutePos(BlockPos.ZERO).getX();
    }

    private static GuhNpcEntity golfguh(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(),
                n -> n.getKind() == GuhNpcEntity.Kind.GOLFGUH);
        helper.assertTrue(npcs.size() == 1, "the Golfguh is in her clubhouse: " + npcs.size());
        return npcs.get(0);
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }

    private static void leave(GameTestHelper helper, ServerPlayer player) {
        GolfGame.leave(player);
        helper.getLevel().removePlayerImmediately(player, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    // --- the numbers ------------------------------------------------------------------------------------------------------

    /** The pars of voorstel_achterste9.md (back 9: 25 / 38 / 47; all 18: 43 / 65 / 83) and the bigger rewards of the back 9. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void golf18ParsAndRewards(GameTestHelper helper) {
        int[] achter = new int[3];
        for (Niveau n : Niveau.values()) {
            for (int h = GolfGame.HOLES; h < GolfGame.MAX_HOLES; h++) {
                achter[n.ordinal()] += GolfBanen.par(n, h);
            }
        }
        helper.assertTrue(achter[0] == 25 && achter[1] == 38 && achter[2] == 47, "back 9 par " + java.util.Arrays.toString(achter));
        helper.assertTrue(GolfGame.totalPar18(Niveau.MAKKELIJK) == 43 && GolfGame.totalPar18(Niveau.MEDIUM) == 65
                && GolfGame.totalPar18(Niveau.LASTIG) == 83, "18 holes par 43 / 65 / 83");
        helper.assertTrue(GolfBanen.balletjesAchter(1, 4, true) == 8 && GolfBanen.balletjesAchter(2, 4, true) == 6
                && GolfBanen.balletjesAchter(3, 4, true) == 5 && GolfBanen.balletjesAchter(4, 4, true) == 4
                && GolfBanen.balletjesAchter(5, 4, true) == 2 && GolfBanen.balletjesAchter(7, 4, true) == 1
                && GolfBanen.balletjesAchter(8, 4, false) == 0, "back 9: 8 / 6 / 5 / 4 / 2 / 1");
        helper.assertTrue(GolfBanen.bonus18(0, false) == 13 && GolfBanen.bonus18(0, true) == 17 && GolfBanen.bonus18(3, false) == 5,
                "18-hole bonus: 5, +8 at par, +4 for a record");
        helper.assertTrue(GolfGame.HOLE_NAMES.length == GolfGame.MAX_HOLES, "a name for every hole");
        helper.assertTrue(GolfGame.Ronde.of(GolfGame.START) == GolfGame.Ronde.VOOR && GolfGame.Ronde.of(GolfGame.START_ACHTER) == GolfGame.Ronde.ACHTER
                && GolfGame.Ronde.of(GolfGame.START_ALLE) == GolfGame.Ronde.ALLE, "the screen's buttons");
        // the template turned: hole 1's cup and the tee of hole 10 keep their distance (a quarter turn clockwise)
        BlockPos cup = new BlockPos(100, 64, 100);
        BlockPos west = GolfBanen.inWereld18(GolfBanen.TEE10_18, cup, Direction.WEST), north = GolfBanen.inWereld18(GolfBanen.TEE10_18, cup, Direction.NORTH);
        int dx = GolfBanen.TEE10_18[0] - GolfBanen.CUP1_18[0], dz = GolfBanen.TEE10_18[2] - GolfBanen.CUP1_18[2];
        helper.assertTrue(west.equals(cup.offset(dx, GolfBanen.TEE10_18[1] - GolfBanen.CUP1_18[1], dz)), "unturned: " + west);
        helper.assertTrue(north.equals(cup.offset(-dz, GolfBanen.TEE10_18[1] - GolfBanen.CUP1_18[1], dx)), "turned: " + north);
        helper.succeed();
    }

    /** The new floors: ijsvilt slides, kaasmodder sticks; both are course, as are the rail and the looping. The bank corner's slant. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void golf18FloorsAndBankCorner(GameTestHelper helper) {
        var ijs = Golf18.IJSVILT.get().defaultBlockState();
        var modder = nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature.KAASMODDER.get().defaultBlockState();
        helper.assertTrue(GolfBallEntity.friction(ijs) > GolfBallEntity.FRICTION && GolfBallEntity.friction(modder) < GolfBallEntity.FRICTION,
                "ice slides, mud sticks");
        for (var state : List.of(ijs, modder, Golf18.SCHUIFRAIL.get().defaultBlockState(), Golf18.LOOPING.get().defaultBlockState())) {
            helper.assertTrue(GolfBallEntity.isCourse(state), "the ball rolls on " + state);
        }
        helper.assertTrue(!GolfBallEntity.isCourse(Golf18.SCHUIFGUH.get().defaultBlockState()), "not on a sliding block");
        Vec3 n = Golf18Blocks.Bankhoek.normal(Direction.NORTH);          // (the corner is north-east: the slant looks south-west)
        helper.assertTrue(n.x < 0 && n.z > 0 && Math.abs(n.length() - 1) < 1e-6, "the slant of a north bank corner: " + n);
        helper.assertTrue(GolfGame.arms(BlockPos.ZERO.above(3), true).size() == 24 && GolfGame.arms(BlockPos.ZERO.above(3), false).size() == 16
                && GolfGame.arms(BlockPos.ZERO.above(3), true).stream().allMatch(p -> p.getY() == 2), "the turning arms, one block under the hub");
        helper.succeed();
    }

    /** A ball rolling east into a bank corner (corner south-east) goes off north. */
    @GuhTest(template = LANES, timeoutTicks = 100, batch = BATCH)
    public static void golf18BankCornerSendsTheBallOffAtAnAngle(GameTestHelper helper) {
        helper.setBlock(new BlockPos(12, 2, 2), Golf18.BANKHOEK.get().defaultBlockState().setValue(Golf18Blocks.Bankhoek.FACING, Direction.EAST));
        GolfBallEntity b = ball(helper, 4.5, 2.5);
        double[] minZ = {99};
        for (int t = 3; t < 60; t++) {
            helper.runAfterDelay(t, () -> minZ[0] = Math.min(minZ[0], b.getZ() - helper.absolutePos(BlockPos.ZERO).getZ()));
        }
        helper.runAfterDelay(2, () -> b.hit(new Vec3(0.6, 0, 0)));
        helper.runAfterDelay(62, () -> {
            helper.assertTrue(minZ[0] < 1.9, "it went north off the slant: " + minZ[0]);
            helper.assertTrue(rx(helper, b) < 13, "and didn't roll through the corner: " + rx(helper, b));
            b.discard();
            helper.succeed();
        });
    }

    /** Fast enough onto the looping and the ball goes round and on; too slow and it rolls back. */
    @GuhTest(template = LANES, timeoutTicks = 160, batch = BATCH)
    public static void golf18LoopingFastRoundSlowBack(GameTestHelper helper) {
        var loop = Golf18.LOOPING.get().defaultBlockState().setValue(Golf18Blocks.Looping.FACING, Direction.EAST);
        helper.setBlock(new BlockPos(8, 1, 2), loop);
        GolfBallEntity fast = ball(helper, 3.5, 2.5);
        boolean[] looped = {false};
        for (int t = 3; t < 50; t++) {
            helper.runAfterDelay(t, () -> looped[0] |= fast.isLooping());
        }
        helper.runAfterDelay(2, () -> fast.hit(new Vec3(1.0, 0, 0)));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(looped[0], "round the looping");
            helper.assertTrue(rx(helper, fast) > 9, "and on its way: " + rx(helper, fast));
            fast.discard();
            GolfBallEntity slow = ball(helper, 6.5, 2.5);
            helper.runAfterDelay(2, () -> slow.hit(new Vec3(0.2, 0, 0)));
            helper.runAfterDelay(70, () -> {
                helper.assertTrue(!slow.isLooping() && rx(helper, slow) < 8.5, "too slow: back it comes " + rx(helper, slow));
                slow.discard();
                helper.succeed();
            });
        });
    }

    /** The golf shop: the outfit as before, building blocks 4 for 1, deco a bit dearer, the cart 64, the golf shoes; never the trophy. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void golf18ShopSellsBlocksAndDeco(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 2, 2));
        npc.setKind(GuhNpcEntity.Kind.GOLFGUH);
        var offers = npc.getOffers();
        helper.assertTrue(offers.stream().allMatch(o -> o.getCostA().is(GolfFeature.GOLFBALLETJE.get())), "all for golfballetjes");
        for (var item : Golf18.RUITJES_ITEMS) {
            helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(item.get()) && o.getResult().getCount() == 4 && o.getCostA().getCount() == 1),
                    "4 " + item.getId() + " for 1");
        }
        helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(GolfFeature.VILT_ITEM.get()) && o.getResult().getCount() == 4), "golf felt too");
        helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(Golf18.GOLFKARRETJE_ITEM.get()) && o.getCostA().getCount() == 64), "the cart: 64");
        for (Item deco : List.of(Golf18.GOLFREK_ITEM.get(), Golf18.GOLF_VLAGGETJE_ITEM.get(), Golf18.GOLFBAL_TEE_ITEM.get(),
                ModItems.clothingItem(GuhClothes.GOLFSCHOENTJES))) {
            helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(deco) && o.getCostA().getCount() > 1), "sells " + deco);
        }
        helper.assertTrue(offers.stream().noneMatch(o -> o.getResult().is(Golf18.GOLFTROFEE_ITEM.get())), "the trophy is not for sale");
        helper.succeed();
    }

    // --- the 18-hole course -------------------------------------------------------------------------------------------------

    /**
     * On the 18-hole course: the back 9 are locked for a new golfer; the front 9 at par opens them. Then a round of the back
     * 9 starts on hole 10, and the Golfguh found all 18 holes and the moving obstacles, which move.
     */
    @GuhTest(template = COURSE18, timeoutTicks = 200, batch = BATCH)
    public static void golf18FrontNineAtParOpensTheBackNine(GameTestHelper helper) {
        GuhNpcEntity npc = golfguh(helper);
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        player.snapTo(npc.getX(), npc.getY(), npc.getZ() + 2);
        GolfGame game = GolfGame.of(npc);
        GolfGame.action(npc, player, GolfGame.START_ACHTER);
        helper.assertTrue(!game.isRunning(), "the back 9 are locked for a new golfer");
        helper.assertTrue(game.heeftAchter(), "but the course has them");
        for (Niveau n : Niveau.values()) {
            for (int h = 0; h < GolfGame.MAX_HOLES; h++) {
                helper.assertTrue(game.tee(n, h) != null && game.cup(h) != null, "hole " + (h + 1) + " has a " + n.id() + " tee and a cup");
            }
        }
        var obstakels = game.testObstakels();
        helper.assertTrue(!obstakels.get(0).isEmpty() && !obstakels.get(1).isEmpty() && obstakels.get(2).size() == 1 && obstakels.get(3).size() == 7,
                "sliding blocks A/B, one turning hub, seven snapping teeth: " + obstakels.stream().map(List::size).toList());
        BlockPos a = obstakels.get(0).get(0), bpos = obstakels.get(1).get(0);
        game.moveObstacles(helper.getLevel());
        boolean aNow = helper.getLevel().getBlockState(a).is(Golf18.SCHUIFGUH.get());
        helper.assertTrue(helper.getLevel().getBlockState(bpos).is(Golf18.SCHUIFGUH.get()) != aNow, "a guh block in one position only");
        game.moveObstacles(helper.getLevel());
        helper.assertTrue(helper.getLevel().getBlockState(a).is(Golf18.SCHUIFGUH.get()) != aNow
                && helper.getLevel().getBlockState(bpos).is(Golf18.SCHUIFGUH.get()) == aNow, "the guh blocks slid over");
        GolfGame.action(npc, player, GolfGame.START);
        helper.assertTrue(game.isRunning() && game.ronde() == GolfGame.Ronde.VOOR, "the front 9");
        game.testSkipCountdown();
        helper.runAfterDelay(5, () -> {
            for (int hole = 0; hole < GolfGame.HOLES; hole++) {
                game.testHoleOut(npc, player, GolfGame.PAR[hole]);
            }
            helper.assertTrue(!game.isRunning(), "9 holes: done");
            helper.assertTrue(GolfGame.achterOpen(player), "at par: the back 9 are open now");
            GolfGame.action(npc, player, GolfGame.START_ACHTER);
            helper.assertTrue(game.isRunning() && game.ronde() == GolfGame.Ronde.ACHTER && game.hole() == 9, "a round of the back 9, from hole 10");
            helper.assertTrue(player.position().distanceTo(Vec3.atCenterOf(game.tee(9))) < 3, "you're on the tee of hole 10");
            GolfBallEntity b = game.ball(helper.getLevel());
            helper.assertTrue(b != null && b.blockPosition().equals(game.tee(9).above()), "and so is your ball");
            leave(helper, player);
            helper.succeed();
        });
    }

    /** All 18 at par: the back 9 pay more, the 18-hole bonus, an 18-hole record, the golf trophy with your name (only once). */
    @GuhTest(template = COURSE18, timeoutTicks = 200, batch = BATCH)
    public static void golf18AllEighteenAtParGivesTheTrophy(GameTestHelper helper) {
        GuhNpcEntity npc = golfguh(helper);
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        player.snapTo(npc.getX(), npc.getY(), npc.getZ() + 2);
        GuhQuests.saved(player).putBoolean(GolfGame.OPEN, true);
        GolfGame game = GolfGame.of(npc);
        GolfGame.action(npc, player, GolfGame.START_ALLE);
        helper.assertTrue(game.isRunning() && game.ronde() == GolfGame.Ronde.ALLE && game.totalPar() == 65, "all 18, par 65: " + game.totalPar());
        game.testSkipCountdown();
        helper.runAfterDelay(5, () -> {
            for (int hole = 0; hole < GolfGame.MAX_HOLES; hole++) {
                game.testHoleOut(npc, player, game.par(hole));
            }
            helper.assertTrue(!game.isRunning(), "18 holes: done");
            // front 9 x 3 (par) + back 9 x 4 (par) + 13 (the 18-hole bonus: 5 + 8 at par) + 6 (the very first round)
            helper.assertTrue(count(player, GolfFeature.GOLFBALLETJE.get()) == 82, "golfballetjes: " + count(player, GolfFeature.GOLFBALLETJE.get()));
            helper.assertTrue(GolfGame.best(player, GolfGame.BEST18, Niveau.MEDIUM) == 65, "an 18-hole record of 65");
            helper.assertTrue(GolfGame.best(player) == -1, "not a 9-hole record");
            helper.assertTrue(count(player, Golf18.GOLFTROFEE_ITEM.get()) == 1, "the golf trophy");
            var trofee = player.getInventory().getItem(player.getInventory().findSlotMatchingItem(new net.minecraft.world.item.ItemStack(Golf18.GOLFTROFEE_ITEM.get())));
            helper.assertTrue(trofee.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME) != null
                    && trofee.getOrDefault(net.minecraft.core.component.DataComponents.LORE, net.minecraft.world.item.component.ItemLore.EMPTY).lines().size() == 2,
                    "with your name and the record on it");
            helper.assertTrue(nl.juiced.guhs.quest.Scorebord.top(helper.getLevel().getServer(), GolfGame.BOARD_18).stream()
                    .anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == 65), "on the 18-hole top 3");
            GolfGame.action(npc, player, GolfGame.START_ALLE);
            game.testSkipCountdown();
            helper.runAfterDelay(5, () -> {
                for (int hole = 0; hole < GolfGame.MAX_HOLES; hole++) {
                    game.testHoleOut(npc, player, game.par(hole));
                }
                helper.assertTrue(count(player, Golf18.GOLFTROFEE_ITEM.get()) == 1, "the trophy only once");
                leave(helper, player);
                helper.succeed();
            });
        });
    }
}
