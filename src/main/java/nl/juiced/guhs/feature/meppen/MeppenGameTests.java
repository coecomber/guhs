package nl.juiced.guhs.feature.meppen;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.meppen.MepBlocks.Kop;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;

/**
 * GameTests of Mika meppen: starting without own items, scoring and combos, the end of a game (mepmunten, record, the
 * world's top 3 at the scoreboard wall), walking away, the loaned mallet (never kept), the shop and the real hall. Most run in the small test
 * room mika_mep_proefhal (4 holes, the Mepguh and the two scoreboards).
 */
public class MeppenGameTests {
    private static final String ROOM = "mika_mep_proefhal";
    private static final String EMPTY = "empty";
    private static final String HALL = "mika_mep_hal";

    // (1.1.0: 26.1 runs the tests sorted by id, so other meppen rooms stand right next to this one; every search stays
    // inside this test's own area)
    private static GuhNpcEntity mepguh(GameTestHelper helper, int radius) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class,
                new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(radius).intersect(helper.getBounds().inflate(1)), n -> n.getKind() == GuhNpcEntity.Kind.MEPGUH);
        helper.assertTrue(npcs.size() == 1, "one Mepguh: " + npcs.size());
        return npcs.get(0);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, GuhNpcEntity npc) {
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        player.snapTo(npc.getX() + 1, npc.getY(), npc.getZ());
        return player;
    }

    /** Starts a game with the heads under the test's control, and skips the countdown. */
    private static MepGame play(GameTestHelper helper, GuhNpcEntity npc, ServerPlayer player) {
        MepGame.action(npc, player, MepGame.START);
        MepGame game = MepGame.of(npc);
        helper.assertTrue(game.isRunning() && MepGame.isPlaying(player), "the game started");
        game.autoSpawn = false;
        advance(game, npc, MepGame.COUNTDOWN + 1);
        return game;
    }

    private static void advance(MepGame game, GuhNpcEntity npc, int ticks) {
        for (int i = 0; i < ticks && game.isRunning(); i++) {
            game.step(npc);
        }
    }

    /** Pops a head and whacks it (on the head block). */
    private static void whack(MepGame game, GuhNpcEntity npc, ServerPlayer player, int hole, Kop kop) {
        BlockPos pos = game.holes().get(hole);
        game.pop(player.level(), player, pos, kop, 1000);
        game.hit(player, pos.above());
        advance(game, npc, MepGame.BONK_TICKS + 1);
    }

    private static void done(GameTestHelper helper, GuhNpcEntity npc, ServerPlayer... players) {
        MepGame game = MepGame.of(npc);
        if (game.isRunning()) {
            game.abort(null, "gone");
        }
        for (ServerPlayer p : players) {
            MepGame.takeBack(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        return GuhQuests.count(player, item);
    }

    @GuhTest(template = ROOM)
    public static void mepStartsWithoutOwnItems(GameTestHelper helper) {
        GuhNpcEntity npc = mepguh(helper, 16);
        ServerPlayer player = player(helper, npc);
        helper.assertTrue(player.getInventory().isEmpty(), "an empty-handed player");
        MepGame.action(npc, player, MepGame.START);
        MepGame game = MepGame.of(npc);
        game.autoSpawn = false;
        helper.assertTrue(game.isRunning() && MepGame.isPlaying(player), "the game is on");
        helper.assertTrue(game.holes().size() == 4, "the board has 4 holes: " + game.holes().size());
        helper.assertTrue(player.getMainHandItem().is(MeppenFeature.MEP_HAMER.get()), "the Mepguh lends a mallet");
        helper.assertTrue(player.blockPosition().equals(game.stand()), "the player stands in the middle of the board: " + player.blockPosition());
        player.hurt(helper.getLevel().damageSources().fall(), 6f);
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "no damage while playing");
        player.getFoodData().setFoodLevel(3);
        advance(game, npc, 20);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 20, "no hunger while playing");
        helper.assertTrue(game.tickCount() <= MepGame.COUNTDOWN, "still counting down");
        BlockPos hole = game.holes().get(0);
        game.pop(helper.getLevel(), player, hole, Kop.MIKA, 1000);
        game.hit(player, hole.above());
        helper.assertTrue(game.score() == 0, "no whacking during the countdown");
        done(helper, npc, player);
    }

    @GuhTest(template = ROOM)
    public static void mepScoresCombosGoldAndTheGuh(GameTestHelper helper) {
        GuhNpcEntity npc = mepguh(helper, 16);
        ServerPlayer player = player(helper, npc);
        MepGame game = play(helper, npc, player);
        whack(game, npc, player, 0, Kop.MIKA);
        helper.assertTrue(game.score() == MepGame.MIKA_POINTS && game.combo() == 1, "a Mika: 10 points, combo 1: " + game.score());
        helper.assertTrue(!helper.getLevel().getBlockState(game.holes().get(0).above()).is(MeppenFeature.MEP_KOP.get()), "the whacked Mika went down");
        for (int i = 0; i < 4; i++) {
            whack(game, npc, player, (i + 1) % 4, Kop.MIKA);
        }
        helper.assertTrue(game.combo() == 5 && game.multiplier() == 2, "5 in a row: combo x2");
        helper.assertTrue(game.score() == 4 * MepGame.MIKA_POINTS + 2 * MepGame.MIKA_POINTS, "the 5th one counts double: " + game.score());
        whack(game, npc, player, 2, Kop.GOUD);
        helper.assertTrue(game.score() == 60 + 2 * MepGame.GOUD_POINTS, "a golden Mika at x2: +100: " + game.score());
        whack(game, npc, player, 3, Kop.GUH);
        helper.assertTrue(game.score() == 160 - MepGame.GUH_PENALTY && game.combo() == 0, "don't whack the guh: -25 and no combo");
        // a whack through the real event, on the hole under a head: counts, and the board never breaks
        BlockPos hole = game.holes().get(1);
        game.pop(helper.getLevel(), player, hole, Kop.MIKA, 1000);
        var event = new PlayerInteractEvent.LeftClickBlock(player, hole, Direction.UP, PlayerInteractEvent.LeftClickBlock.Action.START);
        MepGame.onLeftClickBlock(event);
        helper.assertTrue(event.isCanceled() && game.score() == 135 + MepGame.MIKA_POINTS && game.combo() == 1, "whacking the hole under a Mika counts: " + event.isCanceled() + " " + game.score() + " " + game.combo());
        advance(game, npc, MepGame.BONK_TICKS + 1);
        game.hit(player, game.holes().get(2));
        helper.assertTrue(game.combo() == 0, "an empty hole: combo gone");
        advance(game, npc, 5);
        // a Mika that gets away breaks the combo, a guh left alone is worth a few points
        whack(game, npc, player, 0, Kop.MIKA);
        int before = game.score();
        game.pop(helper.getLevel(), player, game.holes().get(3), Kop.MIKA, 2);
        game.pop(helper.getLevel(), player, game.holes().get(2), Kop.GUH, 2);
        advance(game, npc, 4);
        helper.assertTrue(game.combo() == 0 && game.score() == before + MepGame.GUH_SPARED && game.headsUp() == 0, "escaped Mika / spared guh");
        // without the mallet in hand nothing counts
        player.getInventory().setSelectedSlot((player.getInventory().getSelectedSlot() + 1) % 9);
        game.pop(helper.getLevel(), player, game.holes().get(1), Kop.MIKA, 1000);
        game.hit(player, game.holes().get(1).above());
        helper.assertTrue(game.score() == before + MepGame.GUH_SPARED, "only the mallet whacks");
        done(helper, npc, player);
    }

    @GuhTest(template = ROOM, timeoutTicks = 200)
    public static void mepTimeUpGivesCoinsAndARecord(GameTestHelper helper) {
        GuhNpcEntity npc = mepguh(helper, 16);
        ServerPlayer player = player(helper, npc);
        MepGame game = play(helper, npc, player);
        for (int i = 0; i < 12; i++) {
            whack(game, npc, player, i % 4, Kop.MIKA);                  // 4x10 + 5x20 + 3x30 = 230
        }
        int score = game.score();
        helper.assertTrue(score == 230, "12 in a row: 230 points: " + score);
        advance(game, npc, MepGame.LENGTH + 10);                      // ... and the time runs out
        helper.assertTrue(!game.isRunning() && !MepGame.isPlaying(player), "time's up");
        helper.assertTrue(count(player, MeppenFeature.MEP_HAMER.get()) == 0, "the mallet went back");
        int coins = 2 + score / MepGame.COIN_POINTS + MepGame.FIRST_BONUS;
        helper.assertTrue(count(player, MeppenFeature.MEPMUNT.get()) == coins, "mepmunten by score + first game bonus: " + count(player, MeppenFeature.MEPMUNT.get()));
        helper.assertTrue(count(player, ModItems.KAAS_KNABBELS.get()) == 8, "and kaasknabbels for the first game");
        helper.assertTrue(MepGame.best(player) == score, "the record is saved");
        helper.assertTrue(Scorebord.top(helper.getLevel().getServer(), MepGame.BOARD).stream()
                .anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == score), "the world's top 3 has it");
        helper.assertTrue(MepGame.hallBest(helper.getLevel().getServer()) >= score, "and the world record is at least that");
        for (BlockPos hole : game.holes()) {
            helper.assertTrue(!helper.getLevel().getBlockState(hole.above()).is(MeppenFeature.MEP_KOP.get()), "the board is empty again");
        }
        var marker = helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16).intersect(helper.getBounds().inflate(1)),
                d -> d.entityTags().contains(MepGame.TAG_TOP));
        helper.assertTrue(!marker.isEmpty(), "a spot for the top 3");
        Display.TextDisplay spot = marker.stream().min(java.util.Comparator.comparingDouble(d -> d.distanceToSqr(npc))).get();
        var top = helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, spot.getBoundingBox().inflate(1.5),
                d -> d.entityTags().contains(Scorebord.TAG));
        helper.assertTrue(top.size() == 1 && nl.juiced.guhs.storage.Nbt.saveWithoutId(top.get(0), new net.minecraft.nbt.CompoundTag()).get("text").toString()
                .contains(player.getGameProfile().name() + "  " + MepGame.points(score)), "the top 3 floats at the scoreboard wall with the score");
        // a second, worse game: no bonus, the record stays
        game = play(helper, npc, player);
        whack(game, npc, player, 0, Kop.MIKA);
        game.finishNow(npc);
        helper.assertTrue(count(player, MeppenFeature.MEPMUNT.get()) == coins + 2, "2 mepmunten for 10 points, no bonus the second time");
        helper.assertTrue(MepGame.best(player) == score, "the record stays");
        helper.assertTrue(Scorebord.top(helper.getLevel().getServer(), MepGame.BOARD).stream()
                .anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == score), "and the top 3 keeps the best score");
        done(helper, npc, player);
    }

    /** Just standing there (letting every guh go) earns nothing: no free mepmunten for AFK players. */
    @GuhTest(template = ROOM)
    public static void mepStandingStillEarnsNoCoins(GameTestHelper helper) {
        GuhNpcEntity npc = mepguh(helper, 16);
        ServerPlayer player = player(helper, npc);
        GuhQuests.saved(player).putBoolean("guhs_mep_first", true);           // (not the first game: no bonus)
        MepGame game = play(helper, npc, player);
        game.pop(player.level(), player, game.holes().get(0), Kop.GUH, 2);
        advance(game, npc, 5);
        helper.assertTrue(game.score() == MepGame.GUH_SPARED, "a guh let go: a few points " + game.score());
        game.finishNow(npc);
        helper.assertTrue(!game.isRunning() && count(player, MeppenFeature.MEPMUNT.get()) == 0, "but no mepmunten without whacking a Mika");
        helper.assertTrue(count(player, MeppenFeature.MEP_HAMER.get()) == 0, "the mallet went back");
        helper.assertTrue(Scorebord.top(helper.getLevel().getServer(), MepGame.BOARD).stream().noneMatch(e -> e.player().equals(player.getUUID())),
                "and no place on the scoreboard");
        done(helper, npc, player);
    }

    @GuhTest(template = ROOM)
    public static void mepWalkingAwayEndsTheGame(GameTestHelper helper) {
        GuhNpcEntity npc = mepguh(helper, 16);
        ServerPlayer player = player(helper, npc);
        MepGame game = play(helper, npc, player);
        whack(game, npc, player, 0, Kop.MIKA);
        player.snapTo(player.getX() + MepGame.LEAVE_RADIUS + 3, player.getY(), player.getZ());
        game.step(npc);
        helper.assertTrue(!game.isRunning() && !MepGame.isPlaying(player), "walking away stops the game");
        helper.assertTrue(count(player, MeppenFeature.MEP_HAMER.get()) == 0 && count(player, MeppenFeature.MEPMUNT.get()) == 0,
                "the mallet goes back, and no mepmunten");
        done(helper, npc, player);
    }

    @GuhTest(template = ROOM)
    public static void mepFullHotbarGetsItsItemBack(GameTestHelper helper) {
        GuhNpcEntity npc = mepguh(helper, 16);
        ServerPlayer player = player(helper, npc);
        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, new ItemStack(Items.DIRT, 5));
        }
        player.getInventory().setSelectedSlot(4);
        player.getInventory().setItem(4, new ItemStack(Items.DIAMOND, 3));
        MepGame.action(npc, player, MepGame.START);
        helper.assertTrue(player.getMainHandItem().is(MeppenFeature.MEP_HAMER.get()), "the mallet goes in your hand");
        helper.assertTrue(count(player, Items.DIAMOND) == 0, "your diamonds are kept safe meanwhile");
        MepGame.action(npc, player, MepGame.STOP);
        helper.assertTrue(!MepGame.of(npc).isRunning(), "stopped");
        helper.assertTrue(player.getInventory().getItem(4).is(Items.DIAMOND) && player.getInventory().getItem(4).getCount() == 3,
                "and the diamonds are back in their slot");
        helper.assertTrue(count(player, MeppenFeature.MEP_HAMER.get()) == 0, "the mallet is gone");
        done(helper, npc, player);
    }

    /** 1.2.7: a mallet that got lost during the game comes back; and whoever plays can't be hurt, so can't hurt others either. */
    @GuhTest(template = ROOM, batch = "mep_hamer_terug")
    public static void mepLostMalletComesBack(GameTestHelper helper) {
        GuhNpcEntity npc = mepguh(helper, 16);
        ServerPlayer player = player(helper, npc);
        play(helper, npc, player);
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
        helper.assertTrue(count(player, MeppenFeature.MEP_HAMER.get()) == 0, "(the mallet is lost)");
        MepGame.hamerTerug(player);
        helper.assertTrue(count(player, MeppenFeature.MEP_HAMER.get()) == 1 && player.getMainHandItem().is(MeppenFeature.MEP_HAMER.get()),
                "a new mallet, in your hand");
        MepGame.hamerTerug(player);
        helper.assertTrue(count(player, MeppenFeature.MEP_HAMER.get()) == 1, "never two");
        helper.assertTrue(nl.juiced.guhs.feature.Minigames.invulnerable(player), "a Mika-mepper can't be hurt, so can't hit others either");
        done(helper, npc, player);
    }

    @GuhTest(template = ROOM)
    public static void mepMalletCantBeKept(GameTestHelper helper) {
        GuhNpcEntity npc = mepguh(helper, 16);
        ServerPlayer player = player(helper, npc);
        MepGame game = play(helper, npc, player);
        ItemStack mallet = player.getMainHandItem().copy();
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
        ItemEntity thrown = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), mallet);
        ItemTossEvent toss = new ItemTossEvent(thrown, player);
        MepGame.onToss(toss);
        helper.assertTrue(toss.isCanceled() && count(player, MeppenFeature.MEP_HAMER.get()) == 1, "throwing it away: it jumps back");
        game.abort(player, "stopped");
        // someone who isn't playing: it disappears from the inventory, and as an item on the ground
        ServerPlayer other = player(helper, npc);
        ItemStack stolen = new ItemStack(MeppenFeature.MEP_HAMER.get());
        other.getInventory().setItem(0, stolen);
        stolen.inventoryTick(helper.getLevel(), other, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        helper.assertTrue(count(other, MeppenFeature.MEP_HAMER.get()) == 0, "not playing: no mallet");
        ItemEntity dropped = new ItemEntity(helper.getLevel(), other.getX(), other.getY(), other.getZ(), new ItemStack(MeppenFeature.MEP_HAMER.get()));
        helper.getLevel().addFreshEntity(dropped);
        dropped.tick();
        helper.assertTrue(dropped.isRemoved(), "a dropped mallet vanishes");
        done(helper, npc, player, other);
    }

    @GuhTest(template = ROOM)
    public static void mepOneGameAtATime(GameTestHelper helper) {
        GuhNpcEntity npc = mepguh(helper, 16);
        ServerPlayer a = player(helper, npc), b = player(helper, npc);
        MepGame game = play(helper, npc, a);
        MepGame.action(npc, b, MepGame.START);
        helper.assertTrue(MepGame.isPlaying(a) && !MepGame.isPlaying(b), "the second player waits (and watches)");
        helper.assertTrue(count(b, MeppenFeature.MEP_HAMER.get()) == 0, "no mallet for the one watching");
        BlockPos hole = game.holes().get(0);
        game.pop(helper.getLevel(), a, hole, Kop.MIKA, 1000);
        game.hit(b, hole.above());
        helper.assertTrue(game.score() == 0, "only the player whacks");
        done(helper, npc, a, b);
    }

    @GuhTest(template = EMPTY)
    public static void mepShopSellsTheMikaHunterOutfit(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(nl.juiced.guhs.registry.ModEntities.GUH_NPC.get(), new BlockPos(2, 1, 2));
        npc.setKind(GuhNpcEntity.Kind.MEPGUH);
        var offers = npc.getOffers();
        for (GuhClothes piece : new GuhClothes[]{GuhClothes.MIKAJAGER_HOED, GuhClothes.MIKAJAGER_VEST, GuhClothes.MIKAMEPPER_MEDAILLE}) {
            MerchantOffer offer = offers.stream().filter(o -> o.getResult().is(ModItems.clothingItem(piece))).findFirst().orElse(null);
            helper.assertTrue(offer != null && offer.getCostA().is(MeppenFeature.MEPMUNT.get()), piece + " is sold for mepmunten");
        }
        helper.assertTrue(GuhClothes.MIKAJAGER_HOED.slot == GuhClothes.Slot.HEAD && GuhClothes.MIKAJAGER_VEST.slot == GuhClothes.Slot.BODY
                && GuhClothes.MIKAMEPPER_MEDAILLE.slot == GuhClothes.Slot.NECK, "a hat, a vest and a medal");
        npc.discard();
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void mepBoardBlocksNeverBreak(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), MeppenFeature.MEP_GAT.get());
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.assertTrue(level.getBlockState(pos).getDestroySpeed(level, pos) < 0, "the holes are unbreakable");
        helper.assertTrue(MeppenFeature.MEP_KOP.get().defaultBlockState().getDestroySpeed(level, pos) < 0, "the heads too");
        helper.succeed();
    }

    @GuhTest(template = HALL, timeoutTicks = 200)
    public static void mepHallHasABoardAndTheMepguh(GameTestHelper helper) {
        GuhNpcEntity npc = mepguh(helper, 110);
        ServerPlayer player = player(helper, npc);
        MepGame.action(npc, player, MepGame.START);
        MepGame game = MepGame.of(npc);
        helper.assertTrue(game.isRunning(), "a game starts in the real hall");
        helper.assertTrue(game.holes().size() == 16, "a 4x4 board: " + game.holes().size());
        BlockPos stand = game.stand();
        helper.assertTrue(stand != null && helper.getLevel().getBlockState(stand.below()).isSolid()
                && helper.getLevel().getBlockState(stand).isAir(), "you stand on the board, between the holes");
        var boards = helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(110).intersect(helper.getBounds().inflate(1)),
                d -> d.entityTags().contains(MepGame.TAG_LIVE) || d.entityTags().contains(MepGame.TAG_TOP));
        helper.assertTrue(boards.size() == 2, "the scoreboard wall: " + boards.size());
        helper.assertTrue(game.holes().stream().map(BlockPos::getX).distinct().count() == 4
                && game.holes().stream().map(BlockPos::getZ).distinct().count() == 4, "the holes are 4 rows of 4");
        // the world's top 3 floats at the scoreboard wall (at the marker), not above the Mepguh
        MepGame.showScores(npc);
        Display.TextDisplay spot = boards.stream().filter(d -> d.entityTags().contains(MepGame.TAG_TOP)).findFirst().orElseThrow();
        var top = helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, spot.getBoundingBox().inflate(1.5),
                d -> d.entityTags().stream().anyMatch(t -> t.startsWith(Scorebord.TAG + ":meppen:")));
        helper.assertTrue(top.size() == 1, "the top 3 at the scoreboard wall: " + top.size());
        helper.assertTrue(spot.distanceToSqr(npc) > 20 * 20, "(the wall is across the hall)");
        done(helper, npc, player);
    }

    // --- 2.9: makkelijk / medium / lastig -----------------------------------------------------------------------------------

    /** Lastig: its own board and record, half as many mepmunten more, and the lastig advancement. Medium stays untouched. */
    @GuhTest(template = ROOM, timeoutTicks = 200)
    public static void mepLastigHasItsOwnBoardAndMoreCoins(GameTestHelper helper) {
        GuhNpcEntity npc = mepguh(helper, 16);
        ServerPlayer player = player(helper, npc);
        var lastig = nl.juiced.guhs.feature.spelen.Niveau.LASTIG;
        MepGame.action(npc, player, nl.juiced.guhs.feature.klassiekers.Klassiekers.metNiveau(MepGame.START, lastig));
        MepGame game = MepGame.of(npc);
        helper.assertTrue(game.isRunning() && game.niveau() == lastig, "a game on lastig: " + game.niveau());
        game.autoSpawn = false;
        advance(game, npc, MepGame.COUNTDOWN + 1);
        for (int i = 0; i < 12; i++) {
            whack(game, npc, player, i % 4, Kop.MIKA);
        }
        int score = game.score();
        advance(game, npc, MepGame.LENGTH + 10);
        helper.assertTrue(!game.isRunning(), "time's up");
        int coins = lastig.munten(MepGame.coins(score, 12)) + MepGame.FIRST_BONUS;
        helper.assertTrue(coins == 3 + MepGame.FIRST_BONUS, "lastig: 2 mepmunten become 3");
        helper.assertTrue(count(player, MeppenFeature.MEPMUNT.get()) == coins, "mepmunten on lastig: " + count(player, MeppenFeature.MEPMUNT.get()));
        helper.assertTrue(MepGame.best(player, lastig) == score && MepGame.best(player) == 0, "the lastig record, medium untouched");
        helper.assertTrue(Scorebord.top(helper.getLevel().getServer(), "meppen_score_lastig").stream()
                .anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == score), "on the lastig board");
        helper.assertTrue(Scorebord.top(helper.getLevel().getServer(), MepGame.BOARD).stream().noneMatch(e -> e.player().equals(player.getUUID())),
                "not on the medium board");
        helper.assertTrue(nl.juiced.guhs.feature.klassiekers.Klassiekers.done(player, "grote_guhspelen/klassiekers_meppen_lastig"),
                "the lastig advancement");
        // the floating board has all three levels
        var spot = helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16).intersect(helper.getBounds().inflate(1)),
                d -> d.entityTags().contains(MepGame.TAG_TOP)).stream().min(java.util.Comparator.comparingDouble(d -> d.distanceToSqr(npc))).orElseThrow();
        String text = nl.juiced.guhs.storage.Nbt.saveWithoutId(helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, spot.getBoundingBox().inflate(1.5),
                d -> d.entityTags().contains(Scorebord.TAG)).get(0)).get("text").toString();
        helper.assertTrue(text.contains("gui.guhs.niveau.makkelijk") && text.contains("gui.guhs.niveau.medium") && text.contains("gui.guhs.niveau.lastig")
                && text.contains(player.getGameProfile().name() + "  " + MepGame.points(score)), "one board, three levels: " + text);
        done(helper, npc, player);
    }

    /** Makkelijk is calmer (slower, longer up, fewer guh decoys, more gold), lastig the other way round. */
    @GuhTest(template = EMPTY)
    public static void mepLevelsHaveTheirOwnTempo(GameTestHelper helper) {
        var m = MepGame.Tempo.of(nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK);
        var n = MepGame.Tempo.of(nl.juiced.guhs.feature.spelen.Niveau.MEDIUM);
        var l = MepGame.Tempo.of(nl.juiced.guhs.feature.spelen.Niveau.LASTIG);
        helper.assertTrue(n.spawnFrom() == 20f && n.spawnTo() == 6f && n.upFrom() == 36f && n.upTo() == 15f && n.gold() == 0.06f, "medium = the old game");
        helper.assertTrue(m.spawnTo() > n.spawnTo() && n.spawnTo() > l.spawnTo(), "heads pop up faster and faster per level");
        helper.assertTrue(m.upTo() > n.upTo() && n.upTo() > l.upTo(), "and stay up shorter");
        helper.assertTrue(m.guhFrom() + m.guhExtra() < n.guhFrom() + n.guhExtra() && n.guhFrom() + n.guhExtra() < l.guhFrom() + l.guhExtra(),
                "more guh decoys on lastig");
        helper.assertTrue(m.gold() > n.gold() && n.gold() > l.gold(), "golden Mika's rarer on lastig");
        helper.succeed();
    }
}
