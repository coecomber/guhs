package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B, slice geest_spel: the dev commands (CONTRACT_E2_B 8; a dev run only, plain literals):
 * <ul>
 *   <li>{@code /guhs einde2 geestspel ga <stap>}: sets the state for that step (chapter progress, flags, tiles, lanterns ...),
 *       takes you to its marker in the Geestenwereld and starts what the step starts ({@link #STAPPEN});</li>
 *   <li>{@code /guhs einde2 geestspel scene <reiger|wan|hei_bai_bloei|geestenklok|maannacht|pakguh_uitleg>}: forget the scene and play it;</li>
 *   <li>{@code /guhs einde2 geestspel wis}: forgets everything of this slice for you;</li>
 *   <li>{@code /guhs einde2 geestspel stand}: your seven lines, tiles, lanterns, secrets.</li>
 * </ul>
 * In a tree without the chapter slices (this slice alone) the chapter lines it reads are not registered; the command then
 * registers one-step stand-ins of {@code guhvatar_water} / {@code guhvatar_finale} so every step can still be played (a dev
 * run only; in the merged mod the real lines are there and nothing is registered here).
 */
public final class GeestSpelCommando {
    public static final List<String> STAPPEN = List.of("binnen", "lotus", "hof", "bumi", "pakguh", "piandao", "jeong_jeong", "miroh", "leden",
            "voetstukken", "reiger", "archief", "lantaarns", "lantaarns_7", "hei_bai", "eikel", "koh", "chakra", "chakra_7", "zicht_reiger",
            "zicht_vosje", "zicht_koh", "maannacht", "geestentij", "klok", "voetstappen", "spiegel", "moeras", "wan_glimp", "pad_8", "wan",
            "snelreis", "kussen", "bloei", "alles");
    public static final List<String> SCENES = List.of("reiger", "wan", "hei_bai_bloei", "geestenklok", "maannacht", "pakguh_uitleg");

    private GeestSpelCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("geestspel")
                        .then(Commands.literal("ga").then(Commands.argument("stap", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(STAPPEN, b))
                                .executes(c -> ga(c.getSource(), StringArgumentType.getString(c, "stap")))))
                        .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(SCENES, b))
                                .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                        .then(Commands.literal("wis").executes(c -> wis(c.getSource())))
                        .then(Commands.literal("stand").executes(c -> stand(c.getSource()))))));
    }

    /** (this slice alone) one-step stand-ins of the chapter lines it reads, so the dev steps work without h2/h4. */
    static void standIns() {
        for (String l : List.of(E2Verhaal.LUCHT, E2Verhaal.WATER, E2Verhaal.AARDE, E2Verhaal.VUUR, E2Verhaal.FINALE)) {
            if (Verhaallijnen.van(l) == null) {
                Verhaallijn.maak(l, E2Verhaal.GROEP).stappen(1).registreer();
            }
        }
    }

    /** The geest flag of chapter 2 (the Geestenwereld's first zones). */
    static void geest(ServerPlayer p) {
        standIns();
        E2VerhaalDev.zetTot(p, E2Verhaal.WATER, 0);
        E2VerhaalDev.vlag(p, E2Verhaal.WATER, E2Verhaal.V_GEEST, true);
    }

    /** The finale's witte_lotus flag (the White Lotus, the Top, Fang's path). */
    static void lotus(ServerPlayer p) {
        standIns();
        E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, 0);
        E2VerhaalDev.vlag(p, E2Verhaal.FINALE, E2Verhaal.V_WITTE_LOTUS, true);
    }

    /** The whole Guhvatar story done. */
    static void verhaal(ServerPlayer p) {
        standIns();
        E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, 0);
        E2VerhaalDev.vlag(p, E2Verhaal.FINALE, E2Verhaal.V_WITTE_LOTUS, true);
        E2VerhaalDev.klaar(p, E2Verhaal.FINALE);
    }

    private static void tegels(ServerPlayer p, boolean witteLotus) {
        for (String m : GeestLijnen.MEESTERS) {
            GeestLijnen.witteLotus().vlag(p, "tegel_" + m, true);
        }
        if (witteLotus) {
            GeestLijnen.witteLotus().vlag(p, "tegel_witte_lotus", true);
        }
        for (String l : WitteLotus.LEDEN) {
            GeestLijnen.witteLotus().vlag(p, "lid_" + l, true);
        }
    }

    private static boolean naar(ServerPlayer p, String marker) {
        if (GeestPlek.pos(marker) == null) {
            p.sendSystemMessage(Component.literal("geest_spel: the build has no marker '" + marker + "' (yet); you stay here"));
            return false;
        }
        return Geestenwereld.stuur(p, marker);
    }

    private static int ga(CommandSourceStack source, String stap) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Verhaallijn lotusLijn = GeestLijnen.witteLotus();
        switch (stap) {
            case "binnen" -> {
                geest(p);
                GeestToegang.stuur(p, Geestenwereld.AANKOMST);
            }
            case "lotus" -> {
                lotus(p);
                lotusLijn.wis(p);
                naar(p, "plek_lotus_ruines");
            }
            case "hof" -> {
                lotus(p);
                lotusLijn.wis(p);
                lotusLijn.zet(p, GeestLijnen.L_HOF);
                naar(p, "plek_lotus_ruines");
            }
            case "bumi", "pakguh", "piandao", "jeong_jeong", "miroh" -> {
                lotus(p);
                lotusLijn.wis(p);
                lotusLijn.zet(p, GeestLijnen.L_TEGELS);
                naar(p, stap.equals("piandao") ? "ritme_plek" : "proef_" + stap + "_start");
                Proeven.praatMet(p, stap, WitteLotus.echo(p, stap));
            }
            case "leden" -> {
                lotus(p);
                lotusLijn.wis(p);
                lotusLijn.zet(p, GeestLijnen.L_LEDEN);
                tegels(p, false);
                naar(p, WitteLotus.STICHTER);
            }
            case "voetstukken" -> {
                lotus(p);
                lotusLijn.wis(p);
                lotusLijn.zet(p, GeestLijnen.L_VOETSTUKKEN);
                tegels(p, true);
                naar(p, "stenen_lotus");
            }
            case "reiger" -> {
                lotus(p);
                lotusLijn.wis(p);
                lotusLijn.zet(p, GeestLijnen.L_VOETSTUKKEN);
                tegels(p, true);
                for (String m : GeestLijnen.TEGELS) {
                    lotusLijn.vlag(p, "voetstuk_" + m, true);
                }
                naar(p, "stenen_lotus");
                WitteLotus.ontwaak(p);
            }
            case "archief" -> {
                lotus(p);
                lotusLijn.wis(p);
                lotusLijn.zet(p, GeestLijnen.L_ARCHIEF);
                tegels(p, true);
                naar(p, "archief_ingang");
            }
            case "lantaarns" -> {
                verhaal(p);
                GeestLijnen.lantaarns().wis(p);
                naar(p, "verdwaalde_lantaarn_1");
            }
            case "lantaarns_7" -> {
                verhaal(p);
                GeestLijnen.lantaarns().wis(p);
                for (int n = 1; n <= 7; n++) {
                    GeestLijnen.lantaarns().vlag(p, "thuis_" + n, true);
                }
                GeestLijnen.lantaarns().zet(p, 7);
                naar(p, "verdwaalde_lantaarn_8");
            }
            case "hei_bai" -> {
                verhaal(p);
                GeestLijnen.heiBai().wis(p);
                naar(p, "plek_gefluisterwoud");
            }
            case "eikel" -> {
                verhaal(p);
                GeestLijnen.heiBai().wis(p);
                GeestLijnen.heiBai().zet(p, GeestLijnen.H_EIKEL);
                GeestSpel.geef(p, E2GeestSpelFeature.EIKEL.get(), 1);
                naar(p, "eikel_plek");
            }
            case "koh" -> {
                standIns();
                E2VerhaalDev.zetTot(p, E2Verhaal.AARDE, 0);
                GeestLijnen.koh().wis(p);
                naar(p, "koh_raadsel");
            }
            case "chakra" -> {
                verhaal(p);
                GeestLijnen.chakra().wis(p);
                naar(p, "guru_pathik");
            }
            case "chakra_7" -> {
                verhaal(p);
                GeestLijnen.chakra().wis(p);
                GeestLijnen.chakra().zet(p, 6);
                naar(p, "chakra_7");
            }
            case "zicht_reiger", "zicht_vosje", "zicht_koh" -> {
                geest(p);
                if (!Geestenwereld.in(p)) {
                    GeestToegang.stuur(p, Geestenwereld.AANKOMST);
                }
                String s = stap.substring("zicht_".length());
                GeestLijnen.verschijningen().vlag(p, s, false);
                Verschijningen.verschijn(p, s);
            }
            case "maannacht", "geestentij" -> {
                geest(p);
                if (stap.equals("maannacht")) {
                    GeestLijnen.verschijningen().vlag(p, "maan", false);
                    naar(p, "plek_hemelvijver");
                } else if (!Geestenwereld.in(p)) {
                    GeestToegang.stuur(p, Geestenwereld.AANKOMST);
                }
                var level = p.level().getServer().getLevel(Geestenwereld.DIM);
                if (level != null) {
                    Verschijningen.begin(level, stap.equals("maannacht"));
                }
            }
            case "klok" -> {
                verhaal(p);
                GuhQuests.saved(p).remove(GeestSpel.DATA + "klok");
                Cutscenes.vergeet(p, GeestScenes.KLOK);
                naar(p, "geestenklok");
            }
            case "voetstappen" -> {
                verhaal(p);
                GuhQuests.saved(p).remove(Mysterie.GEHEIMEN);
                naar(p, "voetstappen_1");
            }
            case "spiegel" -> {
                verhaal(p);
                if (naar(p, "spiegelvijver_1")) {
                    Snelreis.spiegel(p);
                }
            }
            case "moeras" -> {
                verhaal(p);
                naar(p, "moeras_visioen");
            }
            case "wan_glimp" -> {
                geest(p);
                if (!Geestenwereld.in(p)) {
                    GeestToegang.stuur(p, Geestenwereld.AANKOMST);
                }
                Mysterie.wanGlimp(p, true);
            }
            case "pad_8" -> {
                verhaal(p);
                GeestLijnen.wan().wis(p);
                GeestLijnen.wan().zet(p, GeestLijnen.W_PAD);
                naar(p, "pad_8_anker");
            }
            case "wan" -> {
                verhaal(p);
                GeestLijnen.wan().wis(p);
                GeestLijnen.wan().zet(p, GeestLijnen.W_EILAND);
                Cutscenes.vergeet(p, GeestScenes.WAN);
                naar(p, "wan_onthulling");
            }
            case "snelreis" -> {
                verhaal(p);
                naar(p, "meditatiesteen_meditatiecirkel");
                Snelreis.lijst(p, null);
            }
            case "kussen" -> {
                verhaal(p);
                GeestSpel.geef(p, E2GeestSpelFeature.MEDITEERKUSSEN_ITEM.get(), 1);
            }
            case "bloei" -> {
                verhaal(p);
                for (int n = 1; n <= GeestLijnen.LANTAARNS_N; n++) {
                    GeestLijnen.lantaarns().vlag(p, "thuis_" + n, true);
                }
                GeestLijnen.lantaarns().zet(p, GeestLijnen.LANTAARNS_N);
                GeestLijnen.heiBai().zet(p, GeestLijnen.H_KLAAR);
                Mysterie.bloeiCheck(p);
                naar(p, "plek_gefluisterwoud");
            }
            case "alles" -> {
                verhaal(p);
                tegels(p, true);
                for (String m : GeestLijnen.TEGELS) {
                    lotusLijn.vlag(p, "voetstuk_" + m, true);
                }
                lotusLijn.zet(p, GeestLijnen.L_KLAAR);
                for (int n = 1; n <= GeestLijnen.LANTAARNS_N; n++) {
                    GeestLijnen.lantaarns().vlag(p, "thuis_" + n, true);
                }
                GeestLijnen.lantaarns().zet(p, GeestLijnen.LANTAARNS_N);
                GeestLijnen.heiBai().zet(p, GeestLijnen.H_KLAAR);
                GeestLijnen.koh().zet(p, GeestLijnen.RAADSELS_N);
                GeestLijnen.chakra().zet(p, GeestLijnen.CHAKRAS_N);
                for (String s : GeestLijnen.ZELDZAAM) {
                    GeestLijnen.verschijningen().vlag(p, s, true);
                }
                GeestLijnen.verschijningen().zet(p, GeestLijnen.VERSCHIJNINGEN_N);
                GeestLijnen.wan().zet(p, GeestLijnen.W_KLAAR);
                Mysterie.bloeiCheck(p);
                GeestToegang.stuur(p, Geestenwereld.AANKOMST);
            }
            default -> {
                source.sendFailure(Component.literal("geest_spel: unknown step " + stap + "; steps: " + String.join(", ", STAPPEN)));
                return 0;
            }
        }
        nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
        source.sendSuccess(() -> Component.literal("geest_spel: step " + stap), false);
        return 1;
    }

    private static int scene(CommandSourceStack source, String naam) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        String id = "einde2_geest_spel_" + naam;
        if (GeestScenes.van(id) == null) {
            source.sendFailure(Component.literal("geest_spel: no scene " + id));
            return 0;
        }
        Cutscenes.vergeet(p, id);
        naar(p, GeestScenes.anker(id));
        p.level().getServer().execute(() -> GeestScenes.speel(p, id, null));
        return 1;
    }

    private static int wis(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        for (String l : GeestLijnen.ALLE) {
            Verhaallijn lijn = GeestLijnen.lijn(l);
            if (lijn != null) {
                lijn.wis(p);
            }
        }
        CompoundTag saved = GuhQuests.saved(p);
        for (String k : List.copyOf(saved.keySet())) {
            if (k.startsWith(GeestSpel.DATA)) {
                saved.remove(k);
            }
        }
        for (String s : GeestScenes.ALLE) {
            Cutscenes.vergeet(p, s);
        }
        GeestSpel.weg(p);
        source.sendSuccess(() -> Component.literal("geest_spel: everything forgotten"), false);
        return 1;
    }

    private static int stand(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        for (String l : GeestLijnen.ALLE) {
            Verhaallijn lijn = GeestLijnen.lijn(l);
            if (lijn != null) {
                source.sendSuccess(() -> Component.literal(l + ": " + lijn.stap(p) + "/" + lijn.stappen()), false);
            }
        }
        StringBuilder t = new StringBuilder("tegels:");
        for (String m : GeestLijnen.TEGELS) {
            t.append(' ').append(m).append(GeestLijnen.heeftTegel(p, m) ? "+" : "-");
        }
        source.sendSuccess(() -> Component.literal(t.toString()), false);
        StringBuilder z = new StringBuilder("zones:");
        for (String zone : Geestenwereld.ZONES) {
            z.append(' ').append(zone).append(GeestToegang.zoneOpen(p, zone) ? "+" : "-");
        }
        z.append(" archief").append(GeestToegang.zoneOpen(p, GeestToegang.ARCHIEF) ? "+" : "-");
        z.append(" eiland_8").append(GeestToegang.zoneOpen(p, GeestToegang.EILAND_8) ? "+" : "-");
        source.sendSuccess(() -> Component.literal(z.toString()), false);
        source.sendSuccess(() -> Component.literal("geheimen: " + GuhQuests.saved(p).getIntOr(Mysterie.GEHEIMEN, 0) + "/7, gloed: " + Chakras.gloeit(p)), false);
        return 1;
    }
}
