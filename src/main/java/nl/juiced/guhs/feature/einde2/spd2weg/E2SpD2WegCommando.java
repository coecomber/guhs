package nl.juiced.guhs.feature.einde2.spd2weg;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.spverhaal.SpCommando;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaalDev;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 "Sponsguh", slice sp_d2_weg: the dev commands (a dev run only, operators; CONTRACT_E2_C 8.2), under
 * {@code /guhs einde2 sponsguh d2weg}:
 * <ul>
 *   <li>{@code scene <naam>}: forget and play one of the seven scenes at its own spot (no step changes);</li>
 *   <li>{@code spel jacht} / {@code spel traan}: part 2 set to step 7 / 8 with its variant on, and the minigame starts where
 *       it starts (the east rim of the loopgraaf / the drying table);</li>
 *   <li>{@code win}: the running minigame as if it went well (the gate / the socket), so its closing scene and the next step
 *       follow;</li>
 *   <li>{@code wis}: this slice's flags, minigames, companions and seen scenes forgotten (the step stays).</li>
 * </ul>
 * Jumping to a step is the skelet's {@code /guhs einde2 sponsguh ga sponsguh_kroon <5-8>} (this slice's start of each step).
 */
public final class E2SpD2WegCommando {
    /** Scene name -> its anchor (a marker, or "pad#i"). */
    static final Map<String, String> ANKER = new LinkedHashMap<>();

    static {
        ANKER.put("knipoog_appa", "knipoog_appa_sponsguh");
        ANKER.put("snor", "snor_plek");
        ANKER.put("monsters", D2WegScenes.MONSTERS_ANKER);
        ANKER.put("dennis_snor", "loopgraaf_rand_oost");
        ANKER.put("laars", D2WegScenes.LAARS_ANKER);
        ANKER.put("cycloop", "droogtafel");
        ANKER.put("sproeier", "droogtafel");
    }

    private E2SpD2WegCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (!SpCommando.dev()) {
            return;
        }
        SpCommando.groep(event, Commands.literal("d2weg")
                .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ANKER.keySet(), b))
                        .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("spel").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"jacht", "traan"}, b))
                        .executes(c -> spel(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("win").executes(c -> win(c.getSource())))
                .then(Commands.literal("wis").executes(c -> {
                    E2SpD2Weg.wisEigen(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> Component.literal("sp_d2_weg: flags, games, companions and scenes forgotten"), false);
                    return 1;
                })));
    }

    private static int scene(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Cutscene s = Cutscene.van(D2WegScenes.ID + naam);
        String anker = ANKER.get(naam);
        if (s == null || anker == null) {
            src.sendFailure(Component.literal("no such scene: " + naam + " " + ANKER.keySet()));
            return 0;
        }
        Cutscenes.vergeet(p, s.id());
        boolean ok = E2SpD2Weg.speel(p, s, anker, null);
        src.sendSuccess(() -> Component.literal(ok ? "playing " + s.id() + " (" + s.duur() / 20.0 + " s)" : "could not play " + s.id()
                + " (in the Guheinde, Schelpenstad found, no other scene running?)"), false);
        return ok ? 1 : 0;
    }

    private static int spel(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        E2SpD2Weg.stopAlles(p);
        boolean ok;
        if (naam.equals("jacht")) {
            SpVerhaalDev.zetTot(p, E2SpD2Weg.KROON, E2SpD2Weg.DENNIS);
            E2SpD2Weg.lijn().vlag(p, E2SpD2Weg.V_JACHT, true);
            E2SpD2Weg.breng(p, "loopgraaf_rand_oost", -90f);
            ok = Jacht.start(p, true);
        } else if (naam.equals("traan")) {
            SpVerhaalDev.zetTot(p, E2SpD2Weg.KROON, E2SpD2Weg.CYCLOOP);
            E2SpD2Weg.lijn().vlag(p, E2SpD2Weg.V_DROOG, true);
            E2SpD2Weg.breng(p, "droogtafel_speler", 180f);
            ok = Traan.start(p, true);
        } else {
            src.sendFailure(Component.literal("spel jacht | spel traan"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(ok ? "sp_d2_weg: " + naam + " started" : "sp_d2_weg: " + naam + " could not start (Schelpenstad found?)"), false);
        return ok ? 1 : 0;
    }

    private static int win(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        boolean ok = Jacht.win(p) || Traan.win(p);
        src.sendSuccess(() -> Component.literal(ok ? "sp_d2_weg: won" : "sp_d2_weg: no minigame running"), false);
        return ok ? 1 : 0;
    }
}
