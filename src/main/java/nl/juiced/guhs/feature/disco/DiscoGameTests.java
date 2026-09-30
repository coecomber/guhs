package nl.juiced.guhs.feature.disco;

import java.util.List;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * GameTests of the Guhdisco: on a small test floor (disco_testvloer: the four 4x4 tile fields and the DJ-guh) and on
 * the real guh_disco. The game is ticked by hand (DiscoGame.tick), so a whole game fits in one test tick.
 */
public class DiscoGameTests {
    private static final String FLOOR = "disco_testvloer";
    /** Where the club sits in the guh_disco grounds (tools/features/disco.py: OX, OZ). */
    private static final int OX = 12, OZ = 6;

    private static GuhNpcEntity dj(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(1),
                n -> n.getKind() == GuhNpcEntity.Kind.DJGUH);
        helper.assertTrue(npcs.size() == 1, "there is one DJ-guh: " + npcs.size());
        return npcs.get(0);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, GuhNpcEntity npc) {
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        player.snapTo(npc.getX(), npc.getY(), npc.getZ() + 1.5);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);   // (mock players start in creative: no damage anyway)
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void tickUntil(GameTestHelper helper, GuhNpcEntity npc, DiscoGame game, DiscoGame.Phase phase) {
        for (int i = 0; i < 3000 && game.phase() != phase; i++) {
            game.tick(npc);
        }
        helper.assertTrue(game.phase() == phase, "reached " + phase + " (still " + game.phase() + ")");
    }

    /** Back to the golden middle of the floor, then the whole sequence right. */
    private static void danceRound(GameTestHelper helper, GuhNpcEntity npc, DiscoGame game, ServerPlayer player) {
        BlockPos c = game.centre();
        player.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        tickUntil(helper, npc, game, DiscoGame.Phase.INPUT);
        for (int colour : game.sequence()) {
            game.press(npc, helper.getLevel(), player, colour);
        }
        helper.assertTrue(game.phase() == DiscoGame.Phase.PAUSE, "the round is danced: " + game.phase());
    }

    private static boolean anyLit(GameTestHelper helper, DiscoGame game) {
        for (int c = 0; c < 4; c++) {
            for (BlockPos pos : game.tilesOf(c)) {
                BlockState state = helper.getLevel().getBlockState(pos);
                if (state.getValue(DiscoTileBlock.LIT)) {
                    return true;
                }
            }
        }
        return false;
    }

    @GuhTest(template = FLOOR, timeoutTicks = 100)
    public static void discoGameWithoutItemsScoresAndRewards(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        DiscoGame game = DiscoGame.of(npc);
        ServerPlayer player = player(helper, npc);
        helper.assertTrue(player.getInventory().isEmpty(), "no items of your own");
        DiscoGame.action(npc, player, DiscoGame.START);
        helper.assertTrue(game.isRunning() && player.getUUID().equals(game.dancer()) && DiscoGame.isDancing(player), "the game is on");
        for (DiscoTileBlock.Kleur kleur : DiscoTileBlock.Kleur.values()) {
            helper.assertTrue(game.tiles(kleur) == 16, "found the 4x4 " + kleur.id() + " tiles: " + game.tiles(kleur));
        }
        helper.assertTrue(game.centre().equals(helper.absolutePos(new BlockPos(6, 2, 9))), "starts in the middle: " + game.centre() + " vs "
                + helper.absolutePos(new BlockPos(6, 2, 9)));
        helper.assertTrue(player.blockPosition().equals(game.centre()), "the dancer went to the middle of the floor");
        // no hunger, no damage
        player.getFoodData().setFoodLevel(2);
        player.hurtOrSimulate(helper.getLevel().damageSources().fall(), 5f);
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "dancers can't get hurt");
        // round 1: the DJ lights the colour, then you step on it (for real: stand on a tile of that colour)
        boolean litDuringShow = false;
        for (int i = 0; i < 3000 && game.phase() != DiscoGame.Phase.INPUT; i++) {
            game.tick(npc);
            litDuringShow |= game.phase() == DiscoGame.Phase.SHOW && anyLit(helper, game);
        }
        helper.assertTrue(litDuringShow, "the tiles flash while the DJ plays");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 20, "and never hungry");
        helper.assertTrue(game.sequence().size() == 1, "round 1: one colour");
        BlockPos tile = game.tilesOf(game.sequence().get(0)).get(5);
        player.snapTo(tile.getX() + 0.5, tile.getY() + 1, tile.getZ() + 0.5);
        player.setOnGround(true);
        game.tick(npc);
        helper.assertTrue(game.phase() == DiscoGame.Phase.PAUSE && game.score() == 1, "stepping on the right tile scores: " + game.phase());
        helper.assertTrue(helper.getLevel().getBlockState(tile).getValue(DiscoTileBlock.LIT), "the tile lights up under your feet");
        // rounds 2 and 3, faster and longer
        danceRound(helper, npc, game, player);
        danceRound(helper, npc, game, player);
        helper.assertTrue(game.score() == 3 && game.sequence().size() == 3, "3 colours danced");
        List<Integer> seq = game.sequence();
        for (int i = 1; i < seq.size(); i++) {
            helper.assertTrue(!seq.get(i).equals(seq.get(i - 1)), "never the same colour twice in a row");
        }
        helper.assertTrue(game.liedje() == DiscoLiedje.DISCO70 && game.stap() == 1, "the standard song, one colour per beat (no speeding up)");
        // round 4: a wrong step ends it
        player.snapTo(game.centre().getX() + 0.5, game.centre().getY(), game.centre().getZ() + 0.5);
        tickUntil(helper, npc, game, DiscoGame.Phase.INPUT);
        game.press(npc, helper.getLevel(), player, (game.sequence().get(0) + 1) % 4);
        helper.assertTrue(game.phase() == DiscoGame.Phase.OVER, "a wrong step: game over");
        tickUntil(helper, npc, game, DiscoGame.Phase.IDLE);
        helper.assertTrue(!DiscoGame.isDancing(player) && game.dancer() == null, "an ordinary player again");
        helper.assertTrue(!anyLit(helper, game), "the tiles went out");
        int coins = GuhQuests.count(player, DiscoBlocks.DISCOMUNT.get());
        helper.assertTrue(coins == DiscoGame.coins(3) + DiscoGame.FIRST_COINS, "2 discomunten for 3 colours + 4 welcome coins: " + coins);
        helper.assertTrue(GuhQuests.count(player, ModItems.KAASKNABBEL_MILKSHAKE.get()) == 1, "and a welcome shake");
        helper.assertTrue(DiscoGame.best(player) == 3, "record: 3");
        helper.assertTrue(nl.juiced.guhs.quest.Scorebord.top(helper.getLevel().getServer(), DiscoGame.BOARD).stream()
                .anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == 3), "on the world's top 3 with 3 colours");
        DiscoGame.showScores(npc);
        net.minecraft.world.phys.Vec3 at = DiscoGame.scorebordPos(npc);
        var boards = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,
                new net.minecraft.world.phys.AABB(at, at).inflate(1.5), d -> d.entityTags().contains(nl.juiced.guhs.quest.Scorebord.TAG));
        helper.assertTrue(boards.size() == 1, "one floating top 3 above the stage: " + boards.size());
        // a second game: worse, so the record stays and there's no second welcome present
        DiscoGame.action(npc, player, DiscoGame.START);
        tickUntil(helper, npc, game, DiscoGame.Phase.INPUT);
        game.press(npc, helper.getLevel(), player, (game.sequence().get(0) + 1) % 4);
        tickUntil(helper, npc, game, DiscoGame.Phase.IDLE);
        helper.assertTrue(DiscoGame.best(player) == 3 && GuhQuests.count(player, DiscoBlocks.DISCOMUNT.get()) == coins, "no coins for 0, record kept");
        helper.assertTrue(nl.juiced.guhs.quest.Scorebord.top(helper.getLevel().getServer(), DiscoGame.BOARD).stream()
                .anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == 3), "a worse game doesn't push you down the board");
        boards.forEach(Entity::discard);
        leave(helper, player);
        helper.succeed();
    }

    /** One wrong step and it's over: no second chances, even deep into a long sequence. */
    @GuhTest(template = FLOOR, timeoutTicks = 100)
    public static void discoOneWrongStepIsGameOver(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        DiscoGame game = DiscoGame.of(npc);
        ServerPlayer player = player(helper, npc);
        helper.assertTrue(game.start(npc, player), "started");
        danceRound(helper, npc, game, player);
        danceRound(helper, npc, game, player);
        player.snapTo(game.centre().getX() + 0.5, game.centre().getY(), game.centre().getZ() + 0.5);
        tickUntil(helper, npc, game, DiscoGame.Phase.INPUT);
        List<Integer> seq = game.sequence();
        game.press(npc, helper.getLevel(), player, seq.get(0));
        game.press(npc, helper.getLevel(), player, (seq.get(1) + 1) % 4);   // right, then one wrong step
        helper.assertTrue(game.phase() == DiscoGame.Phase.OVER, "one wrong step: game over, no lives: " + game.phase());
        game.press(npc, helper.getLevel(), player, seq.get(1));             // (too late to fix it)
        helper.assertTrue(game.phase() == DiscoGame.Phase.OVER && game.score() == 2, "the score stays at the last full round");
        tickUntil(helper, npc, game, DiscoGame.Phase.IDLE);
        helper.assertTrue(DiscoGame.best(player) == 2, "record: 2");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = FLOOR, timeoutTicks = 100)
    public static void discoTooSlowEndsTheGame(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        DiscoGame game = DiscoGame.of(npc);
        ServerPlayer player = player(helper, npc);
        helper.assertTrue(game.start(npc, player), "started");
        tickUntil(helper, npc, game, DiscoGame.Phase.INPUT);
        for (int i = 0; i < DiscoGame.stepTimeout(DiscoLiedje.DISCO70) + 1; i++) {
            game.tick(npc);
        }
        helper.assertTrue(game.phase() == DiscoGame.Phase.OVER, "waited too long: game over");
        tickUntil(helper, npc, game, DiscoGame.Phase.IDLE);
        helper.assertTrue(DiscoGame.best(player) == 0, "no record for 0 colours");
        helper.assertTrue(GuhQuests.count(player, DiscoBlocks.DISCOMUNT.get()) == DiscoGame.FIRST_COINS, "only the welcome coins");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = FLOOR, timeoutTicks = 100)
    public static void discoOneDancerAtATime(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        DiscoGame game = DiscoGame.of(npc);
        ServerPlayer first = player(helper, npc), second = player(helper, npc);
        DiscoGame.action(npc, first, DiscoGame.START);
        DiscoGame.action(npc, second, DiscoGame.START);
        helper.assertTrue(first.getUUID().equals(game.dancer()) && !DiscoGame.isDancing(second), "the second one has to wait");
        helper.assertTrue(second.blockPosition().distSqr(game.centre()) > 4, "and stays where they were");
        DiscoGame.action(npc, first, DiscoGame.STOP);
        helper.assertTrue(!game.isRunning() && !DiscoGame.isDancing(first), "stopped from the screen (" + first.distanceToSqr(npc) + ")");
        helper.assertTrue(game.start(npc, second), "now it's the second one's turn");
        leave(helper, first, second);
        helper.succeed();
    }

    @GuhTest(template = FLOOR, timeoutTicks = 100)
    public static void discoWalkingOffOrLeavingEndsTheGame(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        DiscoGame game = DiscoGame.of(npc);
        ServerPlayer walker = player(helper, npc);
        game.start(npc, walker);
        tickUntil(helper, npc, game, DiscoGame.Phase.SHOW);
        walker.snapTo(walker.getX() + 30, walker.getY(), walker.getZ());
        game.tick(npc);
        helper.assertTrue(!game.isRunning() && !DiscoGame.isDancing(walker), "walked off the floor: over");
        ServerPlayer quitter = player(helper, npc);
        game.start(npc, quitter);
        tickUntil(helper, npc, game, DiscoGame.Phase.SHOW);
        leave(helper, quitter);                                  // logged out / gone
        game.tick(npc);
        helper.assertTrue(!game.isRunning() && !DiscoGame.isDancing(quitter) && !anyLit(helper, game), "gone: the floor is free again");
        // logging out and straight back in (before the DJ-guh noticed): the game is over all the same
        ServerPlayer relogger = player(helper, npc);
        game.start(npc, relogger);
        tickUntil(helper, npc, game, DiscoGame.Phase.SHOW);
        DiscoGame.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(relogger));
        game.tick(npc);
        helper.assertTrue(!game.isRunning() && !DiscoGame.isDancing(relogger), "logged out: the game is over, even if you're back");
        leave(helper, relogger);
        leave(helper, walker);
        helper.succeed();
    }

    /** Nobody breaks or builds in the Guhdisco (only in the Guhmension's real disco), except in creative mode. */
    @GuhTest(template = FLOOR)
    public static void discoIsProtectedExceptInCreative(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        ServerPlayer player = player(helper, npc);
        BlockPos tile = helper.absolutePos(new BlockPos(3, 1, 6));
        helper.assertTrue(!DiscoProtection.denied(player, tile), "outside the Guhmension's disco you may build");
        helper.assertTrue(DiscoProtection.denied(player, true), "in the disco you may not");
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        helper.assertTrue(!DiscoProtection.denied(player, true), "unless you're in creative mode");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = FLOOR, timeoutTicks = 100)
    public static void discoTilesSingOutsideAGame(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        DiscoGame game = DiscoGame.of(npc);
        ServerPlayer player = player(helper, npc);
        game.scan(npc, helper.getLevel());
        BlockPos tile = game.tilesOf(2).get(0);
        player.snapTo(tile.getX() + 0.5, tile.getY() + 1, tile.getZ() + 0.5);
        player.setOnGround(true);
        for (int i = 0; i < 4; i++) {
            game.tick(npc);
        }
        helper.assertTrue(helper.getLevel().getBlockState(tile).getValue(DiscoTileBlock.LIT), "a tile lights up when you walk on it");
        for (int i = 0; i < DiscoGame.FLASH_TICKS + 2; i++) {
            game.tick(npc);
        }
        helper.assertTrue(!helper.getLevel().getBlockState(tile).getValue(DiscoTileBlock.LIT), "and goes out again");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = FLOOR)
    public static void djGuhSellsTheDiscoOutfitForDiscomunten(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        var results = npc.getOffers().stream().map(o -> o.getResult().getItem()).toList();
        for (GuhClothes piece : List.of(GuhClothes.DISCO_GLITTERPAK, GuhClothes.DISCO_AFRO, GuhClothes.DISCO_BRIL, GuhClothes.DISCO_KOPTELEFOONTJE)) {
            helper.assertTrue(results.contains(ModItems.clothingItem(piece)), "sells " + piece);
        }
        helper.assertTrue(GuhClothes.DISCO_KOPTELEFOONTJE.slot == GuhClothes.Slot.OREN, "the headphones go on the ears");
        helper.assertTrue("disco".equals(nl.juiced.guhs.feature.kleding.KledingBronnen.bron(GuhClothes.DISCO_KOPTELEFOONTJE))
                && (DiscoFeature.PRICE_KOPTELEFOONTJE + " discomunten").equals(nl.juiced.guhs.feature.kleding.KledingBronnen.prijs(GuhClothes.DISCO_KOPTELEFOONTJE)),
                "one source: the DJ-guh, for " + DiscoFeature.PRICE_KOPTELEFOONTJE + " discomunten");
        helper.assertTrue(nl.juiced.guhs.feature.spelen.SpelGroepen.kleding("disco").contains(GuhClothes.DISCO_KOPTELEFOONTJE),
                "shown with the disco in the Guhdex");
        helper.assertTrue(npc.getOffers().stream().allMatch(o -> o.getCostA().is(DiscoBlocks.DISCOMUNT.get())), "for discomunten");
        helper.assertTrue(DiscoGame.coins(2) == 0 && DiscoGame.coins(3) == 2 && DiscoGame.coins(10) == 7 && DiscoGame.coins(15) == 13,
                "discomunten by colours danced");
        helper.succeed();
    }

    /** The real Guhdisco: the DJ-guh sits in the mouth of the booth and finds his dance floor. */
    @GuhTest(template = "guh_disco", timeoutTicks = 100)
    public static void guhDiscoHasAWorkingDanceFloor(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        DiscoGame game = DiscoGame.of(npc);
        ServerPlayer player = player(helper, npc);
        helper.assertTrue(game.start(npc, player), "a game starts in the real disco");
        BlockPos nose = helper.absolutePos(new BlockPos(36 + OX, 3, 28 + OZ));
        helper.assertTrue(game.centre().equals(nose), "on the crystal nose: " + game.centre() + " vs " + nose + ", DJ at " + npc.blockPosition());
        for (DiscoTileBlock.Kleur kleur : DiscoTileBlock.Kleur.values()) {
            helper.assertTrue(game.tiles(kleur) == 16, kleur.id() + " tiles: " + game.tiles(kleur));
        }
        helper.assertTrue(!helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(36 + OX, 3, 9 + OZ))).isAir(), "the DJ-guh sits on the stage");
        net.minecraft.world.phys.Vec3 board = DiscoGame.scorebordPos(npc);
        for (int up = 0; up <= 5; up++) {                        // (four songs' top 3: the board is ~5 blocks tall)
            helper.assertTrue(helper.getLevel().getBlockState(BlockPos.containing(board).above(up)).isAir(),
                    "the top 3 of all songs floats in the open above the stage: " + board + " +" + up);
        }
        danceRound(helper, npc, game, player);
        DiscoGame.action(npc, player, DiscoGame.STOP);          // (too far from the DJ: the screen's stop does nothing)
        helper.assertTrue(game.isRunning(), "stop only works next to the DJ-guh");
        game.reset(helper.getLevel());
        leave(helper, player);
        helper.succeed();
    }

    // --- 2.9: the song is the level, the game follows the beat, the music -------------------------------------------------

    /** Dances rounds (checking every row was played on the beat) until the score reaches this length. */
    private static void danceUntil(GameTestHelper helper, GuhNpcEntity npc, DiscoGame game, ServerPlayer player, int length) {
        for (int k = 0; k < 30 && game.score() < length; k++) {
            BlockPos c = game.centre();
            player.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
            tickUntil(helper, npc, game, DiscoGame.Phase.INPUT);
            assertOnTheBeat(helper, game);
            for (int colour : game.sequence()) {
                game.press(npc, helper.getLevel(), player, colour);
            }
            helper.assertTrue(game.phase() == DiscoGame.Phase.PAUSE, "the round is danced: " + game.phase());
        }
        helper.assertTrue(game.score() >= length, "danced " + length + ": " + game.score());
    }

    /** Every colour of the row the DJ just played came on its beat (or half beat), on the nearest server tick. */
    private static void assertOnTheBeat(GameTestHelper helper, DiscoGame game) {
        double tpb = game.liedje().ticksPerBeat();
        List<Integer> at = game.playedAt();
        helper.assertTrue(at.size() == game.sequence().size(), "every colour was played: " + at.size() + "/" + game.sequence().size());
        helper.assertTrue(game.showStart() == Math.floor(game.showStart()) && ((long) game.showStart()) % 2 == 0,
                "a row starts on a whole (even) beat: " + game.showStart());
        for (int i = 0; i < at.size(); i++) {
            double due = (game.showStart() + i * game.stap()) * tpb;
            helper.assertTrue(Math.abs(at.get(i) - due) <= 0.5 + 1e-9, game.liedje() + " colour " + i + " at song tick " + at.get(i)
                    + ", its beat is at " + due);
        }
    }

    @GuhTest(template = FLOOR, timeoutTicks = 200)
    public static void discoColoursFlashExactlyOnTheSongsBeat(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        DiscoGame game = DiscoGame.of(npc);
        ServerPlayer player = player(helper, npc);
        helper.assertTrue(Math.abs(DiscoLiedje.DISCO70.ticksPerBeat() - 1200.0 / 110) < 1e-9, "110 BPM = 10.909 ticks per beat");
        for (DiscoLiedje l : DiscoLiedje.values()) {
            player.snapTo(npc.getX(), npc.getY(), npc.getZ() + 1.5);
            DiscoGame.action(npc, player, DiscoGame.START_LIED + l.ordinal());
            helper.assertTrue(game.isRunning() && game.liedje() == l, "dancing to " + l);
            tickUntil(helper, npc, game, DiscoGame.Phase.INPUT);
            helper.assertTrue(game.showStart() == DiscoGame.FIRST_BEAT, "the first row after the countdown, on beat " + DiscoGame.FIRST_BEAT);
            helper.assertTrue(game.sequence().size() == l.startLengte, l + " starts with " + l.startLengte + " colour(s)");
            assertOnTheBeat(helper, game);
            for (int c : game.sequence()) {
                game.press(npc, helper.getLevel(), player, c);
            }
            // more rounds: the tempo stays the song's, rows get longer, and later rows come on double counts
            danceUntil(helper, npc, game, player, l.dubbelVanaf > 0 ? l.dubbelVanaf : l.startLengte + 3);
            helper.assertTrue(game.stap() == (l.dubbel(game.sequence().size()) ? 0.5 : 1.0), l + ": double counts from " + l.dubbelVanaf);
            helper.assertTrue(l.dubbelVanaf == 0 || game.stap() == 0.5, l + " reached double counts at " + game.sequence().size());
            helper.assertTrue(DiscoGame.stepTimeout(l) == (int) Math.ceil(l.stapBeats * l.ticksPerBeat()), "time per step in beats of the song");
            DiscoGame.action(npc, player, DiscoGame.STOP);
            helper.assertTrue(!game.isRunning(), "stopped");
        }
        helper.assertTrue(DiscoLiedje.TANGO.bpm < DiscoLiedje.DISCO70.bpm && DiscoLiedje.DISCO70.bpm < DiscoLiedje.BOOGIE.bpm
                && DiscoLiedje.BOOGIE.bpm < DiscoLiedje.MAMBO.bpm, "makkelijk slow, lastig fast, the boogie in between");
        leave(helper, player);
        helper.succeed();
    }

    @GuhTest(template = FLOOR, timeoutTicks = 200)
    public static void discoSongIsTheLevelWithItsOwnBoardAndCoins(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        DiscoGame game = DiscoGame.of(npc);
        ServerPlayer player = player(helper, npc);
        helper.assertTrue(DiscoLiedje.of(Niveau.MAKKELIJK) == DiscoLiedje.TANGO && DiscoLiedje.of(Niveau.MEDIUM) == DiscoLiedje.DISCO70
                && DiscoLiedje.of(Niveau.LASTIG) == DiscoLiedje.MAMBO, "song = level");
        helper.assertTrue(DiscoLiedje.DISCO70.board().equals("disco_kleuren") && DiscoLiedje.TANGO.board().equals("disco_kleuren_makkelijk")
                && DiscoLiedje.MAMBO.board().equals("disco_kleuren_lastig") && DiscoLiedje.BOOGIE.board().equals("disco_kleuren_boogie"), "the boards");
        for (DiscoLiedje l : DiscoLiedje.values()) {
            helper.assertTrue(nl.juiced.guhs.quest.Highscores.game(l.highscore()).board().equals(l.board()), "Highscores row of " + l);
        }
        helper.assertTrue(DiscoLiedje.MAMBO.munten(3) == 3 && DiscoLiedje.MAMBO.munten(10) == 11 && DiscoLiedje.TANGO.munten(10) == 7
                && DiscoLiedje.BOOGIE.munten(10) == 7, "lastig +50 % (rounded up), the others as always");
        // Mika-Mambo: the first row already has 3 colours; dance it once and stop -> 3 on the lastig board, coins +50 %
        DiscoGame.action(npc, player, DiscoGame.START_LIED + DiscoLiedje.MAMBO.ordinal());
        danceUntil(helper, npc, game, player, 3);
        helper.assertTrue(game.score() == 3, "3 colours on the Mika-Mambo");
        DiscoGame.action(npc, player, DiscoGame.STOP);
        int coins = GuhQuests.count(player, DiscoBlocks.DISCOMUNT.get());
        helper.assertTrue(coins == 3 + DiscoGame.FIRST_COINS, "3 discomunten (2 + 50 %) + the welcome coins: " + coins);
        helper.assertTrue(DiscoGame.best(player, DiscoLiedje.MAMBO) == 3 && DiscoGame.best(player) == 0, "a record per song");
        helper.assertTrue(nl.juiced.guhs.quest.Scorebord.top(helper.getLevel().getServer(), "disco_kleuren_lastig").stream()
                .anyMatch(e -> e.player().equals(player.getUUID()) && e.score() == 3), "on the Mika-Mambo's top 3");
        helper.assertTrue(nl.juiced.guhs.quest.Scorebord.top(helper.getLevel().getServer(), DiscoGame.BOARD).stream()
                .noneMatch(e -> e.player().equals(player.getUUID())), "not on the standard song's board");
        helper.assertTrue(DiscoGame.gedanst(player).equals(List.of(DiscoLiedje.MAMBO)), "the Mambo counts as danced");
        helper.assertTrue(advancement(helper, player, "grote_guhspelen/disco_mambo") && !advancement(helper, player, "grote_guhspelen/disco_alle_liedjes"),
                "the Mika-Mambo advancement, not yet all songs");
        // the other three songs, 3 colours each: all four danced
        for (DiscoLiedje l : List.of(DiscoLiedje.TANGO, DiscoLiedje.DISCO70, DiscoLiedje.BOOGIE)) {
            player.snapTo(npc.getX(), npc.getY(), npc.getZ() + 1.5);
            DiscoGame.action(npc, player, DiscoGame.START_LIED + l.ordinal());
            danceUntil(helper, npc, game, player, DiscoGame.GEDANST);
            DiscoGame.action(npc, player, DiscoGame.STOP);
            helper.assertTrue(DiscoGame.best(player, l) >= DiscoGame.GEDANST, "record on " + l);
        }
        helper.assertTrue(DiscoGame.gedanst(player).size() == 4, "all four songs danced: " + DiscoGame.gedanst(player));
        helper.assertTrue(advancement(helper, player, "grote_guhspelen/disco_alle_liedjes") && advancement(helper, player, "grote_guhspelen/disco_tango")
                && advancement(helper, player, "grote_guhspelen/disco_boogie") && advancement(helper, player, "grote_guhspelen/disco_disco70"),
                "the song advancements");
        // every reward rule: one colour more never gives fewer coins
        for (DiscoLiedje l : DiscoLiedje.values()) {
            for (int n = 0; n < 20; n++) {
                helper.assertTrue(l.munten(n + 1) >= l.munten(n), l + " coins never go down: " + n);
            }
        }
        leave(helper, player);
        helper.succeed();
    }

    private static boolean advancement(GameTestHelper helper, ServerPlayer player, String name) {
        var holder = helper.getLevel().getServer().getAdvancements().get(nl.juiced.guhs.Guhs.id(name));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    @GuhTest(template = FLOOR, timeoutTicks = 200)
    public static void discoMusicPlaysInTheClubAndStopsWithTheGame(GameTestHelper helper) {
        GuhNpcEntity npc = dj(helper);
        DiscoGame game = DiscoGame.of(npc);
        ServerPlayer player = player(helper, npc);
        game.scan(npc, helper.getLevel());
        for (int i = 0; i < 6; i++) {
            game.tick(npc);
        }
        DiscoMuziek.Luisteraar l = game.muziek().luistert(player.getUUID());
        helper.assertTrue(l != null && l.liedje() == DiscoLiedje.DISCO70, "in the club you hear the remix: " + l);
        // a game: the song of the game, for everyone in the club, from the start of the game (in sync)
        DiscoGame.action(npc, player, DiscoGame.START_LIED + DiscoLiedje.TANGO.ordinal());
        long start = helper.getLevel().getGameTime();
        l = game.muziek().luistert(player.getUUID());
        helper.assertTrue(l != null && l.liedje() == DiscoLiedje.TANGO && l.sinds() == start, "the tango starts with the game: " + l);
        for (int i = 0; i < 12; i++) {
            game.tick(npc);
        }
        helper.assertTrue(game.muziek().luistert(player.getUUID()).sinds() == start, "and isn't started again halfway");
        DiscoGame.action(npc, player, DiscoGame.STOP);
        helper.assertTrue(game.muziek().luistert(player.getUUID()) == null, "the game is over: the music stops");
        for (int i = 0; i < 6; i++) {
            game.tick(npc);
        }
        helper.assertTrue(game.muziek().luistert(player.getUUID()) == null, "the DJ takes a breath first");
        // (the breather is in world ticks: the test ticks the DJ by hand, so wait for the world)
        helper.runAfterDelay(DiscoMuziek.PAUZE + 2, () -> {
            for (int i = 0; i < 6; i++) {
                game.tick(npc);
            }
            DiscoMuziek.Luisteraar again = game.muziek().luistert(player.getUUID());
            helper.assertTrue(again != null && again.liedje() == DiscoLiedje.DISCO70, "then the remix again: " + again);
            player.snapTo(player.getX() + 60, player.getY(), player.getZ());      // walking out of the club
            for (int i = 0; i < 6; i++) {
                game.tick(npc);
            }
            helper.assertTrue(game.muziek().luistert(player.getUUID()) == null, "out of the club: no more music");
            leave(helper, player);
            helper.succeed();
        });
    }

    @GuhTest(template = FLOOR, timeoutTicks = 100)
    public static void discoSongsAreRealTracks(GameTestHelper helper) {
        var sounds = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT;
        for (DiscoLiedje l : DiscoLiedje.values()) {
            helper.assertTrue(sounds.containsKey(nl.juiced.guhs.Guhs.id("disco." + l.id)), "sound event guhs:disco." + l.id);
            helper.assertTrue(DiscoMuziek.LIEDJES.get(l).get().location().equals(nl.juiced.guhs.Guhs.id("disco." + l.id)), "registered " + l);
            double loop = l.loopBeats * 60.0 / l.bpm;
            helper.assertTrue(loop <= l.seconds && l.seconds - loop < 5 && l.loopBeats % 4 == 0, l + ": the loop point is inside the track, on a bar");
            helper.assertTrue(l.seconds > 80 && l.seconds < 120, l + " is about 1:30 - 1:52: " + l.seconds);
        }
        helper.succeed();
    }
}
