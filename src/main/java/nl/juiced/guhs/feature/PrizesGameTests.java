package nl.juiced.guhs.feature;

import java.util.List;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.beauty.BeautyShow;
import nl.juiced.guhs.feature.disco.DiscoBlocks;
import nl.juiced.guhs.feature.disco.DiscoGame;
import nl.juiced.guhs.feature.golf.GolfGame;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeReis;
import nl.juiced.guhs.feature.meppen.MepGame;
import nl.juiced.guhs.feature.race.RaceGame;
import nl.juiced.guhs.feature.smul.SmulGame;
import nl.juiced.guhs.feature.vissen.VisWedstrijd;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.VerstopGame;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhPortalForcer;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Prizes of the minigames: whatever doesn't fit in your pockets drops in front of you, every reward is (at least) one
 * more than before 2.7.0, and a Guhdex for everyone who steps into the Guhmensie through a portal without one.
 */
public class PrizesGameTests {
    private static final String EMPTY = "empty";

    private static ServerPlayer player(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void fillPockets(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getNonEquipmentItems().size(); i++) {
            player.getInventory().getNonEquipmentItems().set(i, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    /** How many of an item lie on the ground around the player. */
    private static int onGround(ServerPlayer player, Item item) {
        return player.level().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(4), e -> e.getItem().is(item))
                .stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void clearGround(ServerPlayer player) {
        player.level().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(4)).forEach(Entity::discard);
    }

    /** Full pockets: the prize lands on the ground in front of you; with a little room, the rest does. */
    @GuhTest(template = EMPTY)
    public static void prizesDropWhenPocketsAreFull(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        clearGround(p);
        fillPockets(p);
        helper.assertTrue(Minigames.give(p, new ItemStack(DiscoBlocks.DISCOMUNT.get(), 5)), "didn't fit");
        helper.assertTrue(GuhQuests.count(p, DiscoBlocks.DISCOMUNT.get()) == 0 && onGround(p, DiscoBlocks.DISCOMUNT.get()) == 5,
                "all 5 discomunten on the ground: " + onGround(p, DiscoBlocks.DISCOMUNT.get()));
        clearGround(p);
        p.getInventory().getNonEquipmentItems().set(7, new ItemStack(ModItems.VERSTOPGUHTICKET.get(), 62));
        helper.assertTrue(Minigames.give(p, new ItemStack(ModItems.VERSTOPGUHTICKET.get(), 5)), "only partly fits");
        helper.assertTrue(GuhQuests.count(p, ModItems.VERSTOPGUHTICKET.get()) == 64 && onGround(p, ModItems.VERSTOPGUHTICKET.get()) == 3,
                "2 in the pockets, 3 on the ground");
        clearGround(p);
        p.getInventory().clearContent();
        helper.assertTrue(!Minigames.give(p, new ItemStack(ModItems.VERSTOPGUHTICKET.get(), 5))
                && GuhQuests.count(p, ModItems.VERSTOPGUHTICKET.get()) == 5 && onGround(p, ModItems.VERSTOPGUHTICKET.get()) == 0, "room: all in the pockets");
        leave(helper, p);
        helper.succeed();
    }

    /** A Guhdex when you come in without one (on the ground with full pockets), not a second one when you have it. */
    @GuhTest(template = EMPTY)
    public static void guhdexForNewcomersOnlyOnce(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        clearGround(p);
        helper.assertTrue(GuhDex.giveOnArrival(p) && GuhQuests.count(p, ModItems.GUHDEX.get()) == 1, "a Guhdex");
        helper.assertTrue(!GuhDex.giveOnArrival(p) && GuhQuests.count(p, ModItems.GUHDEX.get()) == 1, "not a second one");
        p.getInventory().clearContent();
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.GUHDEX.get()));
        helper.assertTrue(!GuhDex.giveOnArrival(p), "one in your other hand counts too");
        p.getInventory().clearContent();
        fillPockets(p);
        helper.assertTrue(GuhDex.giveOnArrival(p) && onGround(p, ModItems.GUHDEX.get()) == 1, "full pockets: it lies in front of you");
        clearGround(p);
        leave(helper, p);
        helper.succeed();
    }

    /**
     * The portals into the Guhmensie hand it out (the guh portal, and back from the Guheinde). Needs the Guhmension
     * dimension, so like the other portal tests this runs on the dev server (/test runall).
     */
    @GuhTest(template = EMPTY, timeoutTicks = 400, required = false)
    public static void guhmensiePortalsGiveAGuhdex(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        ServerLevel overworld = helper.getLevel();
        TeleportTransition in = GuhPortalForcer.getDestination(overworld, p, p.blockPosition());
        helper.assertTrue(in != null && in.newLevel().dimension() == ModDimensions.GUHMENSION, "the guh portal leads into the Guhmensie (no Guhmension dimension? run this on the dev server)");
        in.postTeleportTransition().onTransition(p);
        helper.assertTrue(GuhQuests.count(p, ModItems.GUHDEX.get()) == 1, "a Guhdex on arrival");
        in.postTeleportTransition().onTransition(p);
        helper.assertTrue(GuhQuests.count(p, ModItems.GUHDEX.get()) == 1, "and not a second one");
        p.getInventory().clearContent();
        ServerLevel guheinde = overworld.getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (guheinde != null) {
            CompoundTag back = new CompoundTag();
            back.putDouble("X", 0.5);
            back.putDouble("Y", 80);
            back.putDouble("Z", 0.5);
            GuhQuests.saved(p).put(GuheindeReis.TERUG, back);
            TeleportTransition home = GuheindeReis.portalDestination(guheinde, p, BlockPos.ZERO);
            helper.assertTrue(home != null && home.newLevel().dimension() == ModDimensions.GUHMENSION, "the Guheinde portal leads back");
            home.postTeleportTransition().onTransition(p);
            helper.assertTrue(GuhQuests.count(p, ModItems.GUHDEX.get()) == 1, "back from the Guheinde: a Guhdex too");
            GuhQuests.saved(p).remove(GuheindeReis.TERUG);
        }
        leave(helper, p);
        helper.succeed();
    }

    /** Every reward rule of every minigame gives (at least) one more than before 2.7.0; the anti-AFK zeros stay zero. */
    @GuhTest(template = EMPTY)
    public static void everyRewardIsOneMore(GameTestHelper helper) {
        // beauty: rosettes per round (by the round's points), the finish bonus, the first show
        helper.assertTrue(BeautyShow.finishBonus(4, 40) == 1 + 1 && BeautyShow.finishBonus(4, BeautyShow.GOOD_SHOW) == 3 + 2
                && BeautyShow.finishBonus(0, 80) == 0, "beauty finish bonus 1/3 -> 2/5 (naked: 0)");
        int[] roundPoints = {3, 16, 22, 27};
        int[] oldRosettes = {1, 2, 3, 4};
        for (int i = 0; i < roundPoints.length; i++) {
            int[] scores = {roundPoints[i] / 3, roundPoints[i] / 3, roundPoints[i] - 2 * (roundPoints[i] / 3)};
            var verdict = new nl.juiced.guhs.feature.beauty.BeautyJury.Verdict(scores, 1, 1, 2, 0, false, 0, false);
            helper.assertTrue(verdict.rosettes() == oldRosettes[i] + 1, "beauty round of " + roundPoints[i] + ": " + verdict.rosettes());
        }
        helper.assertTrue(BeautyShow.FIRST_ROSETTES == 3 + 1, "beauty first show");
        // race
        int[] oldMedals = {5, 3, 2, 1};
        for (RaceGame.Medal m : RaceGame.Medal.values()) {
            helper.assertTrue(m.prizes == oldMedals[m.ordinal()] + 1, "race medal " + m + ": " + m.prizes);
        }
        helper.assertTrue(RaceGame.RECORD_PRIZES == 1 + 1 && RaceGame.FIRST_PRIZES == 3 + 1, "race record bonus and first bag");
        // meppen
        for (int score = 0; score <= 4000; score += 10) {
            helper.assertTrue(MepGame.coins(score, 3) == Math.min(10, 1 + score / 300) + 1, "meppen " + score + ": " + MepGame.coins(score, 3));
        }
        helper.assertTrue(MepGame.coins(500, 0) == 0 && MepGame.FIRST_BONUS == 3 + 1, "meppen: nothing without whacking, first game bonus");
        // disco
        for (int score = 0; score <= 30; score++) {
            int old = score < 3 ? 0 : (score - 1) / 2 + (score >= 10 ? 1 : 0) + (score >= 15 ? 2 : 0);
            int extra = score < 3 ? 0 : 1 + (score >= 10 ? 1 : 0) + (score >= 15 ? 1 : 0);
            helper.assertTrue(DiscoGame.coins(score) == old + extra, "disco " + score + ": " + DiscoGame.coins(score));
        }
        helper.assertTrue(DiscoGame.FIRST_COINS == 3 + 1, "disco first dance");
        // golf
        for (int hole = 0; hole < GolfGame.HOLES; hole++) {
            for (int strokes = 1; strokes <= GolfGame.MAX_STROKES; strokes++) {
                int diff = strokes - GolfGame.PAR[hole];
                int old = strokes == 1 ? 5 : diff <= -2 ? 4 : diff == -1 ? 3 : diff == 0 ? 2 : diff == 1 ? 1 : 0;
                helper.assertTrue(GolfGame.balletjes(strokes, hole, true) == old + 1, "golf hole " + hole + " in " + strokes);
            }
            helper.assertTrue(GolfGame.balletjes(GolfGame.MAX_STROKES, hole, false) == 0, "not holed: nothing");
        }
        helper.assertTrue(GolfGame.roundBonus(3, false) == 2 + 1 && GolfGame.roundBonus(0, false) == 5 + 2 && GolfGame.roundBonus(-1, true) == 7 + 3
                && GolfGame.FIRST_BALLS == 5 + 1, "golf round bonus (2 +3 par +2 record) and first round");
        // smul
        int[] steps = {25, 50, 80, 120, 170, 230};
        for (int score = 0; score <= 300; score++) {
            int old = 0;
            if (score > 0) {
                old = 1;
                for (int step : steps) {
                    old += score >= step ? 1 : 0;
                }
            }
            helper.assertTrue(SmulGame.munten(score) == (score > 0 ? old + 1 : 0), "smul " + score + ": " + SmulGame.munten(score));
        }
        helper.assertTrue(SmulGame.FIRST_BONUS == 3 + 1, "smul first game");
        // vissen
        for (int points = 0; points <= 1000; points += 5) {
            int oldEnd = points <= 0 ? 0 : Math.min(12, 1 + points / 60);
            int oldEarly = Math.min(12, points / 60);
            helper.assertTrue(VisWedstrijd.bonnen(points) == (points > 0 ? oldEnd + 1 : 0), "vissen " + points + ": " + VisWedstrijd.bonnen(points));
            helper.assertTrue(VisWedstrijd.earlyBonnen(points) == (points > 0 ? oldEarly + 1 : 0), "vissen early " + points);
        }
        helper.assertTrue(VisWedstrijd.BONUS == 2 + 1 && VisWedstrijd.FIRST_BONNEN == 3 + 1, "vissen win/record bonus and first contest");
        // verstop
        int[] oldTickets = {1, 2, 4};
        for (VerstopGame.Level level : VerstopGame.Level.values()) {
            helper.assertTrue(level.tickets == oldTickets[level.ordinal()] + 1, "verstop " + level.id() + ": " + level.tickets);
        }
        helper.assertTrue(VerstopGame.QUICK_BONUS == 1 + 1, "verstop quick bonus");
        helper.assertTrue(List.of(VerstopGame.Level.values()).size() == 3, "three levels");
        helper.succeed();
    }
}
