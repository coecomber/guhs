package nl.juiced.guhs.feature.einde2.spd2finale;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.spverhaal.SpCommando;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaalDev;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 8.2), slice sp_d2_finale: the dev commands (a dev run only, operators), under
 * {@code /guhs einde2 sponsguh d2finale}:
 * <ul>
 *   <li>{@code scene <hasselguh|thuis|guhfy_guhber|bedankt>}: forget and play one scene at its own spot (you are put there
 *       first; no step changes);</li>
 *   <li>{@code spel emmers}: step 10 at the landing, the scene skipped, the bucket game on; {@code spel rit}: step 9 on the pier
 *       head, the scene skipped, the ride starts;</li>
 *   <li>{@code vrij}: all six bucket guhs free (step 10: walk into the Bellenkom); {@code klaar}: step 11 as if the song was
 *       played (the line done, the rewards); {@code wis}: this slice's scenes forgotten and its entities gone.</li>
 * </ul>
 * Jumping to a step with its own start: the generic {@code /guhs einde2 sponsguh ga sponsguh_kroon <9|10|11>}.
 */
final class E2SpD2FinaleCommando {
    /** Scene name -> {structure, anchor}. */
    private static final Map<String, String[]> SCENES = new LinkedHashMap<>();

    static {
        SCENES.put("hasselguh", new String[] {SpVerhaal.SCHELPENSTAD, D2fScenes.ANKER_PIER});
        SCENES.put("thuis", new String[] {SpVerhaal.BIKINIBODEM, D2fScenes.ANKER_THUIS});
        SCENES.put("guhfy_guhber", new String[] {SpVerhaal.BIKINIBODEM, D2fScenes.ANKER_PODIUM});
        SCENES.put("bedankt", new String[] {SpVerhaal.BIKINIBODEM, D2fScenes.ANKER_PODIUM});
    }

    private E2SpD2FinaleCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (!SpCommando.dev()) {
            return;
        }
        SpCommando.groep(event, Commands.literal("d2finale")
                .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SCENES.keySet(), b))
                        .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("spel")
                        .then(Commands.literal("emmers").executes(c -> emmers(c.getSource())))
                        .then(Commands.literal("rit").executes(c -> rit(c.getSource()))))
                .then(Commands.literal("vrij").executes(c -> vrij(c.getSource())))
                .then(Commands.literal("klaar").executes(c -> klaar(c.getSource())))
                .then(Commands.literal("wis").executes(c -> wis(c.getSource()))));
    }

    private static Cutscene van(String naam) {
        return Cutscene.van(D2fScenes.ID + naam);
    }

    /** Marks a scene as seen (so the step logic does not play it). */
    private static void gezien(ServerPlayer p, Cutscene s) {
        E2SpD2Finale.gezien(p, s);
    }

    private static boolean naar(ServerPlayer p, String structuur, String plek) {
        BlockPos b = E2SpD2Finale.blok(p, structuur, plek);
        if (b == null) {
            return false;
        }
        p.teleportTo(p.level(), b.getX() + 0.5, b.getY() + 0.05, b.getZ() + 0.5, Set.of(), p.getYRot(), 0f, true);
        p.resetFallDistance();
        return true;
    }

    private static int scene(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        String[] waar = SCENES.get(naam);
        Cutscene s = van(naam);
        if (waar == null || s == null) {
            src.sendFailure(Component.literal("d2finale: no scene " + naam + " (" + SCENES.keySet() + ")"));
            return 0;
        }
        if (!naar(p, waar[0], waar[1])) {
            src.sendFailure(Component.literal("d2finale: " + waar[0] + " is not known yet (go to the Guheinde first)"));
            return 0;
        }
        Cutscenes.vergeet(p, s.id());
        boolean ok = E2SpD2Finale.speel(p, s, waar[0], waar[1], null);
        src.sendSuccess(() -> Component.literal("d2finale: scene " + naam + (ok ? " plays (" + s.duur() / 20 + " s without clicks)" : " could not start")), false);
        return ok ? 1 : 0;
    }

    private static int emmers(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        SpVerhaalDev.zetTot(p, SpVerhaal.KROON, SpVerhaal.K_EMMERS);
        D2fStoet.ruimOp(p);
        gezien(p, D2fScenes.THUIS);
        if (!naar(p, SpVerhaal.BIKINIBODEM, D2fScenes.ANKER_THUIS)) {
            src.sendFailure(Component.literal("d2finale: Bikinibodem is not known yet"));
            return 0;
        }
        D2fEmmers.start(p);
        src.sendSuccess(() -> Component.literal("d2finale: step 10, the bucket game is on (6 guhs between the stadspoort and the Bellenkom)"), false);
        return 1;
    }

    private static int rit(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        SpVerhaalDev.zetTot(p, SpVerhaal.KROON, SpVerhaal.K_HASSELGUH);
        D2fStoet.ruimOp(p);
        gezien(p, D2fScenes.HASSELGUH);
        if (!naar(p, SpVerhaal.SCHELPENSTAD, D2fScenes.ANKER_PIER)) {
            src.sendFailure(Component.literal("d2finale: Schelpenstad is not known yet"));
            return 0;
        }
        boolean ok = D2fRit.vertrek(p);
        src.sendSuccess(() -> Component.literal("d2finale: step 9, the ride " + (ok ? "started" : "could not start")), false);
        return ok ? 1 : 0;
    }

    private static int vrij(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (SpVerhaal.KROON_LIJN.stap(p) != SpVerhaal.K_EMMERS) {
            src.sendFailure(Component.literal("d2finale: you are not at step 10 (use spel emmers)"));
            return 0;
        }
        int n = 0;
        for (int i = 0; i < D2fEmmers.AANTAL; i++) {
            n += D2fEmmers.bevrijd(p, i) ? 1 : 0;
        }
        int m = n;
        src.sendSuccess(() -> Component.literal("d2finale: " + m + " freed; walk into the Bellenkom"), false);
        return 1;
    }

    private static int klaar(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (SpVerhaal.KROON_LIJN.stap(p) != SpVerhaal.K_GUHFY) {
            SpVerhaalDev.zetTot(p, SpVerhaal.KROON, SpVerhaal.K_GUHFY);
        }
        E2SpD2Finale.klaar(p);
        src.sendSuccess(() -> Component.literal("d2finale: part 2 done, rewards given"), false);
        return 1;
    }

    private static int wis(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        D2fStoet.ruimOp(p);
        for (String naam : SCENES.keySet()) {
            Cutscenes.vergeet(p, D2fScenes.ID + naam);
        }
        src.sendSuccess(() -> Component.literal("d2finale: scenes forgotten, entities gone"), false);
        return 1;
    }
}
