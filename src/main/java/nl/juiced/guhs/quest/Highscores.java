package nl.juiced.guhs.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.beauty.BeautyFeature;
import nl.juiced.guhs.feature.beauty.BeautyShow;
import nl.juiced.guhs.feature.disco.DiscoBlocks;
import nl.juiced.guhs.feature.disco.DiscoGame;
import nl.juiced.guhs.feature.golf.GolfFeature;
import nl.juiced.guhs.feature.golf.GolfGame;
import nl.juiced.guhs.feature.meppen.MepGame;
import nl.juiced.guhs.feature.meppen.MeppenFeature;
import nl.juiced.guhs.feature.race.RaceFeature;
import nl.juiced.guhs.feature.race.RaceRecords;
import nl.juiced.guhs.feature.race.RaceRole;
import nl.juiced.guhs.feature.smul.SmulFeature;
import nl.juiced.guhs.feature.smul.SmulGame;
import nl.juiced.guhs.feature.vissen.VisSoort;
import nl.juiced.guhs.feature.vissen.VisWedstrijd;
import nl.juiced.guhs.feature.vissen.VissenFeature;
import nl.juiced.guhs.network.MaagPayloads;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Highscores page of the Guhdex: per minigame (board of the {@link Scorebord}) your own best score - kept for every
 * player, also when it never made the world's top 3 - and the server record (place 1 of the board) with its holder.
 * Personal bests are saved in the player's own data ({@link GuhQuests#saved}); {@link Scorebord#submit} keeps them up to
 * date. Scores that were saved before this page existed (the games' own personal records) still count.
 * <p>
 * The format lambdas and legacy lookups only run on the server; the icons are also used by the client screen.
 */
public final class Highscores {
    /** The player data compound with the personal bests: board name -> best score. */
    public static final String KEY = "guhs_highscores";

    /**
     * One line on the page: id (lang gui.guhs.highscores.game.&lt;id&gt;), the Scorebord board, whether lower is better
     * (times, strokes), how the game itself shows a score, the icon, and the game's own older personal record (a value
     * that means "none" is fine: see {@link #valid}).
     */
    public record Game(String id, String board, boolean lowerIsBetter, IntFunction<String> format, Supplier<Item> icon,
                       ToIntFunction<ServerPlayer> legacy) {
    }

    public static final List<Game> GAMES = List.of(
            new Game("beauty", BeautyShow.SCOREBORD, false, s -> s + " / " + BeautyShow.ROUNDS * BeautyShow.MAX_ROUND,
                    () -> BeautyFeature.SHOWROZET.get(), p -> BeautyShow.best(p)),
            new Game("race", RaceRole.BOARD_TOTAL, true, t -> RaceRecords.time(t), () -> RaceFeature.RACEPRIJSJE.get(), p -> RaceRecords.best(p)),
            new Game("race_lap", RaceRole.BOARD_LAP, true, t -> RaceRecords.time(t), () -> Items.CLOCK, p -> RaceRecords.bestLap(p)),
            new Game("meppen", MepGame.BOARD, false, s -> MepGame.points(s), () -> MeppenFeature.MEPMUNT.get(), p -> MepGame.best(p)),
            new Game("disco", DiscoGame.BOARD, false, n -> n + " ♫", () -> DiscoBlocks.DISCOMUNT.get(), p -> DiscoGame.best(p)),
            new Game("golf", GolfGame.BOARD, true, t -> t + " (" + GolfGame.rel(t - GolfGame.TOTAL_PAR) + ")",
                    () -> GolfFeature.GOLFBALLETJE.get(), p -> GolfGame.best(p)),
            new Game("smul", SmulGame.BOARD, false, s -> s + " pt", () -> SmulFeature.SMULMUNT.get(), p -> SmulGame.best(p)),
            new Game("vissen", VisWedstrijd.BOARD_POINTS, false, s -> s + " pt", () -> VissenFeature.VISBON.get(), p -> VisWedstrijd.best(p)),
            new Game("vissen_zwaarste", VisWedstrijd.BOARD_HEAVIEST, false, g -> VisSoort.kg(g).getString(), () -> VissenFeature.vis(VisSoort.GOUDEN_GUHVIS),
                    p -> VisWedstrijd.heaviest(p)),
            new Game("verstop_makkelijk", "verstop_makkelijk", true, t -> VerstopGame.time(t), () -> ModItems.VERSTOPGUHTICKET.get(),
                    p -> VerstopGame.best(p, VerstopGame.Level.MAKKELIJK)),
            new Game("verstop_medium", "verstop_medium", true, t -> VerstopGame.time(t), () -> ModItems.VERSTOPGUHTICKET.get(),
                    p -> VerstopGame.best(p, VerstopGame.Level.MEDIUM)),
            new Game("verstop_moeilijk", "verstop_moeilijk", true, t -> VerstopGame.time(t), () -> ModItems.VERSTOPGUHTICKET.get(),
                    p -> VerstopGame.best(p, VerstopGame.Level.MOEILIJK)),
            // --- 2.8 (Knuffeldal): the boards are the ids; the coins come from their features (air until they exist) ---
            new Game("bakkerij", "bakkerij", false, s -> s + " pt", coin("bakmunt"), p -> 0),
            new Game("creche", "creche", false, s -> s + " pt", coin("speenmunt"), p -> 0),
            new Game("kapper", "kapper", false, s -> s + " pt", coin("krulmunt"), p -> 0),
            new Game("glijbaan_roze_trechter", "glijbaan_roze_trechter", false, s -> s + " pt", coin("eendjesmunt"), p -> 0),
            new Game("glijbaan_glimtunnel", "glijbaan_glimtunnel", false, s -> s + " pt", coin("eendjesmunt"), p -> 0),
            new Game("glijbaan_grote_plons", "glijbaan_grote_plons", false, s -> s + " pt", coin("eendjesmunt"), p -> 0),
            // --- 2.9 (De Grote Guhspelen): makkelijk / lastig of the classics (the rows above stay medium), grouped per game ---
            new Game("beauty_makkelijk", BeautyShow.SCOREBORD + "_makkelijk", false, s -> s + " / " + BeautyShow.ROUNDS * BeautyShow.MAX_ROUND,
                    () -> BeautyFeature.SHOWROZET.get(), p -> 0),
            new Game("beauty_lastig", BeautyShow.SCOREBORD + "_lastig", false, s -> s + " / " + BeautyShow.ROUNDS * BeautyShow.MAX_ROUND,
                    () -> BeautyFeature.SHOWROZET.get(), p -> 0),
            new Game("race_makkelijk", RaceRole.BOARD_TOTAL + "_makkelijk", true, t -> RaceRecords.time(t), () -> RaceFeature.RACEPRIJSJE.get(), p -> -1),
            new Game("race_lastig", RaceRole.BOARD_TOTAL + "_lastig", true, t -> RaceRecords.time(t), () -> RaceFeature.RACEPRIJSJE.get(), p -> -1),
            new Game("race_lap_makkelijk", RaceRole.BOARD_LAP + "_makkelijk", true, t -> RaceRecords.time(t), () -> Items.CLOCK, p -> -1),
            new Game("race_lap_lastig", RaceRole.BOARD_LAP + "_lastig", true, t -> RaceRecords.time(t), () -> Items.CLOCK, p -> -1),
            new Game("meppen_makkelijk", MepGame.BOARD + "_makkelijk", false, s -> MepGame.points(s), () -> MeppenFeature.MEPMUNT.get(), p -> 0),
            new Game("meppen_lastig", MepGame.BOARD + "_lastig", false, s -> MepGame.points(s), () -> MeppenFeature.MEPMUNT.get(), p -> 0),
            new Game("golf_makkelijk", GolfGame.BOARD + "_makkelijk", true, t -> t + " (" + GolfGame.rel(t - nl.juiced.guhs.feature.golf.GolfBanen.totalPar(nl.juiced.guhs.feature.spelen.Niveau.MAKKELIJK)) + ")",
                    () -> GolfFeature.GOLFBALLETJE.get(), p -> -1),
            new Game("golf_lastig", GolfGame.BOARD + "_lastig", true, t -> t + " (" + GolfGame.rel(t - nl.juiced.guhs.feature.golf.GolfBanen.totalPar(nl.juiced.guhs.feature.spelen.Niveau.LASTIG)) + ")",
                    () -> GolfFeature.GOLFBALLETJE.get(), p -> -1),
            new Game("smul_makkelijk", SmulGame.BOARD + "_makkelijk", false, s -> s + " pt", () -> SmulFeature.SMULMUNT.get(), p -> 0),
            new Game("smul_lastig", SmulGame.BOARD + "_lastig", false, s -> s + " pt", () -> SmulFeature.SMULMUNT.get(), p -> 0),
            new Game("vissen_makkelijk", VisWedstrijd.BOARD_POINTS + "_makkelijk", false, s -> s + " pt", () -> VissenFeature.VISBON.get(), p -> 0),
            new Game("vissen_lastig", VisWedstrijd.BOARD_POINTS + "_lastig", false, s -> s + " pt", () -> VissenFeature.VISBON.get(), p -> 0),
            new Game("vissen_zwaarste_makkelijk", VisWedstrijd.BOARD_HEAVIEST + "_makkelijk", false, g -> VisSoort.kg(g).getString(),
                    () -> VissenFeature.vis(VisSoort.GOUDEN_GUHVIS), p -> 0),
            new Game("vissen_zwaarste_lastig", VisWedstrijd.BOARD_HEAVIEST + "_lastig", false, g -> VisSoort.kg(g).getString(),
                    () -> VissenFeature.vis(VisSoort.GOUDEN_GUHVIS), p -> 0),
            // the disco: makkelijk = Vadsige Tango, the row "disco" = medium (the remix), lastig = Mika-Mambo, and the bonus song
            new Game("disco_makkelijk", DiscoGame.BOARD + "_makkelijk", false, n -> n + " ♫", () -> DiscoBlocks.DISCOMUNT.get(), p -> 0),
            new Game("disco_lastig", DiscoGame.BOARD + "_lastig", false, n -> n + " ♫", () -> DiscoBlocks.DISCOMUNT.get(), p -> 0),
            new Game("disco_boogie", DiscoGame.BOARD + "_boogie", false, n -> n + " ♫", () -> DiscoBlocks.DISCOMUNT.get(), p -> 0),
            // --- 2.9: the new games (board = id; T = time in ticks, lower is better; P = points) ---
            points("sjoelen", "sjoelschijfje"),
            time("doolhof_makkelijk", "doolhofknabbel"), time("doolhof_medium", "doolhofknabbel"), time("doolhof_lastig", "doolhofknabbel"),
            points("katapult_makkelijk", "katapultster"), points("katapult_medium", "katapultster"), points("katapult_lastig", "katapultster"),
            points("spelen_knabbelhappen", "spelenlintje"), time("spelen_zaklopen", "spelenlintje"), points("spelen_blikgooien", "spelenlintje"),
            time("spelen_eierlopen", "spelenlintje"), time("spelen_spijkerpoepen", "spelenlintje"), points("spelen_guhguhtje_prik", "spelenlintje"),
            points("spelen_zeskamp", "spelenlintje"),
            time("elfguhjestocht", "elfstempel"),
            time("circuit_regenboog_makkelijk", "circuitbeker"), time("circuit_regenboog_makkelijk_ronde", "circuitbeker"),
            time("circuit_regenboog_medium", "circuitbeker"), time("circuit_regenboog_medium_ronde", "circuitbeker"),
            time("circuit_regenboog_lastig", "circuitbeker"), time("circuit_regenboog_lastig_ronde", "circuitbeker"),
            time("circuit_vads_makkelijk", "circuitbeker"), time("circuit_vads_makkelijk_ronde", "circuitbeker"),
            time("circuit_vads_medium", "circuitbeker"), time("circuit_vads_medium_ronde", "circuitbeker"),
            time("circuit_vads_lastig", "circuitbeker"), time("circuit_vads_lastig_ronde", "circuitbeker"),
            time("circuit_kaasberg_makkelijk", "circuitbeker"), time("circuit_kaasberg_makkelijk_ronde", "circuitbeker"),
            time("circuit_kaasberg_medium", "circuitbeker"), time("circuit_kaasberg_medium_ronde", "circuitbeker"),
            time("circuit_kaasberg_lastig", "circuitbeker"), time("circuit_kaasberg_lastig_ronde", "circuitbeker"),
            // --- 3.0 (Guhverhalen): the Nomguh sledesprint (balto-slee), surfing and hula (guhwaii-spellen) ---
            time("sledesprint_makkelijk", "sledebelletje"), time("sledesprint_medium", "sledebelletje"), time("sledesprint_lastig", "sledebelletje"),
            points("surfen_makkelijk", "schelpjesmunt"), points("surfen_medium", "schelpjesmunt"), points("surfen_lastig", "schelpjesmunt"),
            points("hula_makkelijk", "schelpjesmunt"), points("hula_medium", "schelpjesmunt"), points("hula_lastig", "schelpjesmunt"),
            // --- bbq2 (Super Guhrio): the whole castle in one go and the six levels (feature/guhrio/GuhrioKasteel submits them) ---
            time("guhrio_kasteel", "guhrio_vadsmunt"),
            time("guhrio_1_1", "guhrio_munt"), time("guhrio_1_2", "guhrio_munt"), time("guhrio_2_1", "guhrio_munt"),
            time("guhrio_2_2", "guhrio_munt"), time("guhrio_3_1", "guhrio_munt"), time("guhrio_3_2", "guhrio_munt"));

    /** A 2.9 row with points ("N pt", higher is better; board = id, icon = the game's coin). */
    private static Game points(String id, String coin) {
        return new Game(id, id, false, s -> s + " pt", coin(coin), p -> 0);
    }

    /** A 2.9 row with a time in ticks ({@link #tijd}, lower is better; board = id, icon = the game's coin). */
    private static Game time(String id, String coin) {
        return new Game(id, id, true, Highscores::tijd, coin(coin), p -> -1);
    }

    /** A time in ticks as m:ss.t (tenths of a second), e.g. 1234 ticks = "1:01.7"; "-" for none (below 0). */
    public static String tijd(int ticks) {
        if (ticks < 0) {
            return "-";
        }
        int tienden = ticks / 2;   // (a tick is 0.05 s)
        return String.format(java.util.Locale.ROOT, "%d:%02d.%d", tienden / 600, tienden / 10 % 60, tienden % 10);
    }

    /** An icon by registry lookup (a 2.8 coin that another feature registers). */
    private static Supplier<Item> coin(String id) {
        return () -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(nl.juiced.guhs.Guhs.id(id));
    }

    @Nullable
    public static Game game(String id) {
        return GAMES.stream().filter(g -> g.id().equals(id)).findFirst().orElse(null);
    }

    @Nullable
    public static Game byBoard(String board) {
        return GAMES.stream().filter(g -> g.board().equals(board)).findFirst().orElse(null);
    }

    /** Is a is better than b? */
    public static boolean better(int a, int b, boolean lowerIsBetter) {
        return lowerIsBetter ? a < b : a > b;
    }

    /** A real score (not the "none" of the games' own records: -1 for times, 0 for points)? */
    public static boolean valid(int score, boolean lowerIsBetter) {
        return lowerIsBetter ? score >= 0 : score > 0;
    }

    /**
     * A score is handed in (from {@link Scorebord#submit}): the new personal best when it's better than the old one.
     * Returns true when it was.
     */
    public static boolean remember(ServerPlayer player, String board, int score, boolean lowerIsBetter) {
        CompoundTag all = GuhQuests.saved(player).getCompoundOrEmpty(KEY);
        if (all.contains(board) && !better(score, all.getIntOr(board, 0), lowerIsBetter)) {
            return false;
        }
        all.putInt(board, score);
        GuhQuests.saved(player).put(KEY, all);
        return true;
    }

    /** Your best score on a board (null: never played). Also looks at the game's own older record and the world's top 3. */
    @Nullable
    public static Integer personalBest(ServerPlayer player, Game game) {
        Integer best = null;
        CompoundTag all = GuhQuests.saved(player).getCompoundOrEmpty(KEY);
        if (all.contains(game.board())) {
            best = all.getIntOr(game.board(), 0);
        }
        int old = game.legacy().applyAsInt(player);
        if (valid(old, game.lowerIsBetter()) && (best == null || better(old, best, game.lowerIsBetter()))) {
            best = old;
        }
        for (Scorebord.Entry e : Scorebord.top(player.level().getServer(), game.board())) {
            if (e.player().equals(player.getUUID()) && (best == null || better(e.score(), best, game.lowerIsBetter()))) {
                best = e.score();
            }
        }
        return best;
    }

    /** The server record: place 1 of the board (or null). */
    @Nullable
    public static Scorebord.Entry record(MinecraftServer server, Game game) {
        List<Scorebord.Entry> top = Scorebord.top(server, game.board());
        return top.isEmpty() ? null : top.get(0);
    }

    /** 1.2.0: a score as the reader's client shows it (the heaviest fish in kg per language; everything else is neutral). */
    public static net.minecraft.network.chat.Component tekst(Game game, int score) {
        return game.board().startsWith(VisWedstrijd.BOARD_HEAVIEST) ? VisSoort.kg(score) : net.minecraft.network.chat.Component.literal(game.format().apply(score));
    }

    /** The page for this player, formatted the way each game shows its scores. */
    public static List<MaagPayloads.HighscoreRow> rows(ServerPlayer player) {
        List<MaagPayloads.HighscoreRow> rows = new ArrayList<>();
        for (Game game : GAMES) {
            Integer best = personalBest(player, game);
            Scorebord.Entry record = record(player.level().getServer(), game);
            rows.add(new MaagPayloads.HighscoreRow(game.id(), best != null, best != null ? tekst(game, best) : net.minecraft.network.chat.Component.empty(),
                    record == null ? net.minecraft.network.chat.Component.empty() : tekst(game, record.score()), record == null ? "" : record.name()));
        }
        return rows;
    }

    /** Sends the page to the player (on opening the Guhdex, and whenever one of their scores changes). */
    public static void sync(ServerPlayer player) {
        ModNetworking.sendTo(player, new MaagPayloads.HighscoresData(rows(player)));
    }

    /** A new server record: everyone's page changes. */
    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
    }

    private Highscores() {
    }
}
