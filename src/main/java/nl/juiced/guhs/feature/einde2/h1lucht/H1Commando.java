package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.Momo;
import nl.juiced.guhs.feature.einde2.verhaal.E2Hervat;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * einde2 phase B, slice h1_lucht (CONTRACT_E2_B 8): the dev command of chapter 1 (a dev run only, plain literals).
 * <ul>
 *   <li>{@code /guhs einde2 h1 ga <stap>}: puts you at that step of {@value E2Verhaal#LUCHT} (Opper-Mika won, the line wiped
 *       and set; Appa and Momo where the story has given them), teleports you to its place and starts what it starts. Steps:
 *       {@code kaasbol vlucht hal luchtbal scooter zweven momo stal deur zuka vlucht_zuka klaar} (or 0..10); {@code ga kaasbol} for
 *       a player who is in the story only takes them to the kaasbol (nothing wiped: fix round 1);</li>
 *   <li>{@code /guhs einde2 h1 scene <naam>}: forgets and plays that scene where you would see it;</li>
 *   <li>{@code /guhs einde2 h1 wis}: forgets the whole chapter (the scenes and the card too);</li>
 *   <li>{@code /guhs einde2 h1 goal}: (luchtbal) a goal for you.</li>
 * </ul>
 */
public final class H1Commando {
    /** The step names of {@code ga}, in order (index = step); then the two extra ones. */
    static final List<String> STAPPEN = List.of("kaasbol", "vlucht", "hal", "luchtbal", "scooter", "zweven", "momo", "stal", "deur", "zuka");
    static final String VLUCHT_ZUKA = "vlucht_zuka", KLAAR = "klaar";

    private H1Commando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            return;
        }
        List<String> namen = new ArrayList<>(STAPPEN);
        namen.add(VLUCHT_ZUKA);
        namen.add(KLAAR);
        List<String> scenes = H1Scenes.ALLE.stream().map(s -> s.id().substring("einde2_h1_lucht_".length())).toList();
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("h1")
                        .then(Commands.literal("ga").then(Commands.argument("stap", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(namen, b))
                                .executes(c -> ga(c.getSource(), StringArgumentType.getString(c, "stap")))))
                        .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(scenes, b))
                                .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                        .then(Commands.literal("wis").executes(c -> wis(c.getSource())))
                        .then(Commands.literal("goal").executes(c -> {
                            Luchtbal.goal(c.getSource().getPlayerOrException());
                            return 1;
                        })))));
    }

    private static int fout(CommandSourceStack src, String tekst) {
        src.sendFailure(Component.literal(tekst));
        return 0;
    }

    /** Stops whatever of this chapter runs for the player (minigames, actors, the ball). */
    static void opruimen(ServerPlayer p) {
        Luchtbal.stop(p);
        Training.vergeet(p);
        MomoJacht.stop(p);
        Ontsnapping.stop(p);
        Kaasbol.weg(p);
        Tempel.vergeet(p.getUUID());
    }

    static int ga(CommandSourceStack src, String naam) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        int stap;
        if (naam.equals(VLUCHT_ZUKA)) {
            stap = E2H1Lucht.ZUKA;
        } else if (naam.equals(KLAAR)) {
            stap = E2H1Lucht.STAPPEN;
        } else if (STAPPEN.contains(naam)) {
            stap = STAPPEN.indexOf(naam);
        } else {
            try {
                stap = Math.max(0, Math.min(E2H1Lucht.STAPPEN, Integer.parseInt(naam)));
            } catch (NumberFormatException e) {
                return fout(src, "unknown step " + naam + " (" + STAPPEN + ", " + VLUCHT_ZUKA + ", " + KLAAR + ")");
            }
        }
        ServerLevel g = E2H1Lucht.guheinde(p.level().getServer());
        if (g == null) {
            return fout(src, "no Guheinde");
        }
        if (stap > E2H1Lucht.KAASBOL && !Plekken.bekend(g, E2H1Lucht.STRUCTUUR)) {
            Plekken.zoekVooruit(g);
            return fout(src, "the spot of the Zuidelijke Luchttempel is still being worked out: try again in a few seconds");
        }
        if (stap == E2H1Lucht.KAASBOL && E2Hervat.kanVerder(p)) {
            // fix round 1 (playtest 1 item 11): "ga kaasbol" was the way back after dying, and it wiped the chapter. A player who is
            // in the story keeps everything: only the trip to the kaasbol, whose click brings them back to their step.
            BlockPos bol = Kaasbol.plek(g);
            int x = bol != null ? bol.getX() - 6 : GuheindeGevecht.ARRIVAL_X, z = bol != null ? bol.getZ() - 4 : GuheindeGevecht.ARRIVAL_Z;
            int y = bol != null ? bol.getY() : g.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            p.teleportTo(g, x + 0.5, y + 0.1, z + 0.5, Set.of(), -60f, 0f, true);
            net.minecraft.server.level.ServerPlayer s = p;
            src.sendSuccess(() -> Component.literal("h1: you are in the story (" + E2Hervat.huidig(s).id() + " step " + E2Hervat.huidig(s).stap(s)
                    + "): nothing wiped. Click the kaasbol to go back to your step. A fresh start: /guhs einde2 h1 wis, then ga kaasbol."), false);
            return 1;
        }
        opruimen(p);
        if (stap == E2H1Lucht.STAPPEN) {
            E2VerhaalDev.zetTot(p, E2Verhaal.LUCHT, E2H1Lucht.ZUKA);
            Appa.geef(p);
            Momo.geef(p);
            Tempel.afronden(p);
            src.sendSuccess(() -> Component.literal("h1: the chapter done (rewards, flag geleerd, the flight to the Waterstam)"), false);
            return 1;
        }
        E2VerhaalDev.zetTot(p, E2Verhaal.LUCHT, stap);
        E2H1Lucht.LIJN.begin(p);
        if (stap >= E2H1Lucht.VLUCHT) {
            Appa.geef(p);
        }
        if (stap > E2H1Lucht.MOMO) {
            Momo.geef(p);
        }
        if (stap > E2H1Lucht.STAL) {
            E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_ZADEL, true);
            E2H1Lucht.LIJN.teller(p, E2H1Lucht.T_APPELS, 3);
        }
        switch (stap) {
            case E2H1Lucht.KAASBOL -> {
                BlockPos bol = Kaasbol.plek(g);
                int x = bol != null ? bol.getX() - 6 : GuheindeGevecht.ARRIVAL_X, z = bol != null ? bol.getZ() - 4 : GuheindeGevecht.ARRIVAL_Z;
                int y = bol != null ? bol.getY() : g.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                p.teleportTo(g, x + 0.5, y + 0.1, z + 0.5, Set.of(), -60f, 0f, true);
            }
            case E2H1Lucht.VLUCHT -> Kaasbol.vlieg(p);
            case E2H1Lucht.HAL -> E2H1Lucht.zetNaar(p, "poort", 180f);
            case E2H1Lucht.LUCHTBAL -> E2H1Lucht.zetNaar(p, "taartenbank", 90f);
            case E2H1Lucht.SCOOTER -> E2H1Lucht.zetNaar(p, "scooter_start", 6, 0, 0, 90f);   // (fix round 1: next to the start)
            case E2H1Lucht.ZWEVEN -> Training.naarBalkon(p);
            case E2H1Lucht.MOMO -> {
                E2H1Lucht.zetNaar(p, "zweef_landing", 180f);
                MomoJacht.jat(p);
            }
            case E2H1Lucht.STAL -> E2H1Lucht.zetNaar(p, "rustpunt_4", 0f);
            case E2H1Lucht.DEUR -> E2H1Lucht.zetNaar(p, "werkplaatsdeur", -6, 0, 0, -90f);
            default -> {
                if (naam.equals(VLUCHT_ZUKA)) {
                    E2H1Lucht.zetNaar(p, "steiger", -90f);
                    E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_SCHIP, true);
                    Ontsnapping.toonSchip(p);
                    Ontsnapping.start(p);
                } else {
                    E2H1Lucht.zetNaar(p, "rustpunt_5", -90f);
                }
            }
        }
        int s = stap;
        src.sendSuccess(() -> Component.literal("h1: at step " + s + " (" + (naam.equals(VLUCHT_ZUKA) ? VLUCHT_ZUKA : STAPPEN.get(s)) + ")"), false);
        return 1;
    }

    static int scene(CommandSourceStack src, String naam) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Cutscene s = Cutscene.van("einde2_h1_lucht_" + naam);
        if (s == null) {
            return fout(src, "no scene " + naam);
        }
        ServerLevel g = E2H1Lucht.guheinde(p.level().getServer());
        String plek = switch (naam) {
            case "aankomst" -> "aankomst_speler";
            case "hal" -> "voetstuk_leeg";
            case "momo" -> "momo_eind";
            case "zuka_steiger" -> "steiger";
            case "geleerd" -> "appa_kamp";
            default -> null;
        };
        BlockPos anker;
        if (naam.equals("knipoog_sjokkel")) {
            anker = g == null ? null : Training.knipoogAnker(g);
        } else if (plek != null) {
            anker = g == null ? null : E2H1Lucht.plek(g, plek);
        } else {
            KaasbolEntity bol = Kaasbol.van(p);
            anker = bol != null ? bol.blockPosition() : p.blockPosition().offset(4, 0, 3);
        }
        if (anker == null) {
            return fout(src, "the temple's spot is not known yet");
        }
        if (plek != null && p.level() != g) {
            return fout(src, "go to the Guheinde first (the scene plays at the temple)");
        }
        Cutscenes.vergeet(p, s.id());
        if (naam.equals("zuka_steiger")) {
            E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_SCHIP, true);
            Ontsnapping.toonSchip(p);
        }
        Cutscenes.speel(p, s, anker, Rotation.NONE, null);
        return 1;
    }

    static int wis(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        opruimen(p);
        E2VerhaalDev.wis(p, E2Verhaal.LUCHT);
        for (Cutscene s : H1Scenes.ALLE) {
            Cutscenes.vergeet(p, s.id());
        }
        Verteller.vergeet(p, E2H1Lucht.KAART);
        src.sendSuccess(() -> Component.literal("h1: chapter 1 forgotten"), false);
        return 1;
    }
}
