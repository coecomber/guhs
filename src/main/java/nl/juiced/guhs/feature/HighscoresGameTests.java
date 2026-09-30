package nl.juiced.guhs.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.golf.GolfGame;
import nl.juiced.guhs.feature.race.RaceRecords;
import nl.juiced.guhs.feature.vissen.VisSoort;
import nl.juiced.guhs.network.MaagPayloads;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.quest.VerstopGame;

/**
 * The Highscores page of the Guhdex: your personal best per minigame (kept, never made worse, also outside the top 3),
 * lower-is-better games, the server record, the formats and the payload.
 */
public class HighscoresGameTests {
    private static final String EMPTY = "empty";

    private static ServerPlayer player(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** A board of its own for this test (the real boards are shared with the other tests). */
    private static Highscores.Game testGame(boolean lowerIsBetter) {
        String board = "test_highscore_" + UUID.randomUUID().toString().substring(0, 8);
        return new Highscores.Game(board, board, lowerIsBetter, s -> s + " pt", () -> Items.STONE, p -> lowerIsBetter ? -1 : 0);
    }

    /** Your best goes up with a better score and never down with a worse one; also for a player outside the top 3. */
    @GuhTest(template = EMPTY)
    public static void personalBestOnlyImproves(GameTestHelper helper) {
        Highscores.Game game = testGame(false);
        ServerPlayer a = player(helper), b = player(helper), c = player(helper), d = player(helper);
        helper.assertTrue(Highscores.personalBest(d, game) == null, "never played: nothing");
        Scorebord.submit(a, game.board(), 90, false);
        Scorebord.submit(b, game.board(), 80, false);
        Scorebord.submit(c, game.board(), 70, false);
        int place = Scorebord.submit(d, game.board(), 10, false);
        helper.assertTrue(place == 0 && Scorebord.top(helper.getLevel().getServer(), game.board()).stream()
                .noneMatch(e -> e.player().equals(d.getUUID())), "d isn't in the top 3");
        helper.assertTrue(Integer.valueOf(10).equals(Highscores.personalBest(d, game)), "but d's own best is kept: " + Highscores.personalBest(d, game));
        Scorebord.submit(d, game.board(), 5, false);
        helper.assertTrue(Integer.valueOf(10).equals(Highscores.personalBest(d, game)), "a worse score doesn't overwrite it");
        Scorebord.submit(d, game.board(), 40, false);
        helper.assertTrue(Integer.valueOf(40).equals(Highscores.personalBest(d, game)), "a better one does");
        helper.assertTrue(GuhQuests.saved(d).getCompoundOrEmpty(Highscores.KEY).getIntOr(game.board(), 0) == 40, "saved with the player");
        leave(helper, a, b, c, d);
        helper.succeed();
    }

    /** Times and strokes: lower is better, for your own best and for the server record. */
    @GuhTest(template = EMPTY)
    public static void lowerIsBetterForTimes(GameTestHelper helper) {
        Highscores.Game game = testGame(true);
        ServerPlayer a = player(helper), b = player(helper);
        Scorebord.submit(a, game.board(), 1500, true);
        Scorebord.submit(a, game.board(), 1700, true);
        helper.assertTrue(Integer.valueOf(1500).equals(Highscores.personalBest(a, game)), "a slower race doesn't count");
        Scorebord.submit(a, game.board(), 1400, true);
        helper.assertTrue(Integer.valueOf(1400).equals(Highscores.personalBest(a, game)), "a faster one does");
        Scorebord.submit(b, game.board(), 1450, true);
        Scorebord.Entry record = Highscores.record(helper.getLevel().getServer(), game);
        helper.assertTrue(record != null && record.player().equals(a.getUUID()) && record.score() == 1400, "the fastest has the record");
        Scorebord.submit(b, game.board(), 0, true);
        helper.assertTrue(Integer.valueOf(0).equals(Highscores.personalBest(b, game)), "0 is a real time (lower is better)");
        helper.assertTrue(Highscores.valid(0, true) && !Highscores.valid(-1, true) && !Highscores.valid(0, false) && Highscores.valid(1, false),
                "what counts as 'played' in the games' own records");
        leave(helper, a, b);
        helper.succeed();
    }

    /** The server record is place 1 of the board, with the holder's name; a new record takes over. */
    @GuhTest(template = EMPTY)
    public static void serverRecordHasTheBestAndItsName(GameTestHelper helper) {
        Highscores.Game game = testGame(false);
        ServerPlayer a = player(helper), b = player(helper);
        helper.assertTrue(Highscores.record(helper.getLevel().getServer(), game) == null, "no record yet");
        Scorebord.submit(a, game.board(), 120, false);
        Scorebord.submit(b, game.board(), 100, false);
        Scorebord.Entry record = Highscores.record(helper.getLevel().getServer(), game);
        helper.assertTrue(record != null && record.score() == 120 && record.name().equals(a.getGameProfile().name()), "a has it");
        Scorebord.submit(b, game.board(), 150, false);
        record = Highscores.record(helper.getLevel().getServer(), game);
        helper.assertTrue(record != null && record.score() == 150 && record.name().equals(b.getGameProfile().name()), "now b has it");
        leave(helper, a, b);
        helper.succeed();
    }

    /** Every minigame has a line, formatted the way the game does; the payload survives the trip to the client. */
    @GuhTest(template = EMPTY)
    public static void pageFormatsAndPayload(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        List<MaagPayloads.HighscoreRow> rows = Highscores.rows(p);
        helper.assertTrue(rows.size() == Highscores.GAMES.size(), "a line per game");
        Set<String> ids = new HashSet<>();
        Set<String> boards = new HashSet<>();
        for (Highscores.Game g : Highscores.GAMES) {
            helper.assertTrue(ids.add(g.id()) && boards.add(g.board()), "unique: " + g.id());
        }
        for (String id : List.of("beauty", "race", "race_lap", "meppen", "disco", "golf", "smul", "vissen", "vissen_zwaarste",
                "verstop_makkelijk", "verstop_medium", "verstop_moeilijk")) {
            helper.assertTrue(Highscores.game(id) != null, "a line for " + id);
        }
        helper.assertTrue(rows.stream().noneMatch(MaagPayloads.HighscoreRow::played), "a new player never played anything");
        helper.assertTrue(Highscores.game("race").lowerIsBetter() && Highscores.game("golf").lowerIsBetter()
                && Highscores.game("verstop_moeilijk").lowerIsBetter() && !Highscores.game("smul").lowerIsBetter()
                && !Highscores.game("vissen_zwaarste").lowerIsBetter(), "times and strokes: lower is better");
        helper.assertTrue(Highscores.game("race").format().apply(1234).equals(RaceRecords.time(1234)), "race time");
        helper.assertTrue(Highscores.game("golf").format().apply(GolfGame.TOTAL_PAR - 2).equals((GolfGame.TOTAL_PAR - 2) + " (-2)"), "golf strokes");
        helper.assertTrue(Highscores.game("vissen_zwaarste").format().apply(1500).equals(VisSoort.kg(1500)) && VisSoort.kg(1500).equals("1,50 kg"), "kg");
        helper.assertTrue(Highscores.game("verstop_medium").format().apply(20 * 75).equals("1:15"), "verstop time");
        helper.assertTrue(Highscores.game("meppen").format().apply(230).equals("230 pt"), "meppen points");
        helper.assertTrue(Highscores.game("beauty").format().apply(60).equals("60 / 90"), "beauty total");
        // the games' own older records count too (from before the Highscores page)
        GuhQuests.saved(p).putInt("guhs_verstop_best_" + VerstopGame.Level.MAKKELIJK.id(), 600);
        helper.assertTrue(Integer.valueOf(600).equals(Highscores.personalBest(p, Highscores.game("verstop_makkelijk"))), "an older verstop record");
        MaagPayloads.HighscoreRow row = Highscores.rows(p).stream().filter(r -> r.game().equals("verstop_makkelijk")).findFirst().orElseThrow();
        helper.assertTrue(row.played() && row.best().equals("0:30"), "shown as 0:30: " + row.best());
        // the payload
        MaagPayloads.HighscoresData data = new MaagPayloads.HighscoresData(List.of(
                new MaagPayloads.HighscoreRow("race", true, "1:02.5", "0:59.1", "Juiced"),
                new MaagPayloads.HighscoreRow("disco", false, "", "", "")));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        MaagPayloads.HighscoresData.STREAM_CODEC.encode(buf, data);
        MaagPayloads.HighscoresData back = MaagPayloads.HighscoresData.STREAM_CODEC.decode(buf);
        helper.assertTrue(back.equals(data) && back.rows().get(0).hasRecord() && !back.rows().get(1).hasRecord(), "the payload survives: " + back);
        CompoundTag none = GuhQuests.saved(p).getCompoundOrEmpty(Highscores.KEY);
        helper.assertTrue(none.isEmpty(), "looking doesn't save anything");
        leave(helper, p);
        helper.succeed();
    }
}
