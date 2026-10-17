package nl.juiced.guhs.feature.einde2.spd1stad;

import java.util.Arrays;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.spverhaal.SpCommando;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaalDev;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 "Sponsguh", slice sp_d1_stad, dev run only (CONTRACT_E2_C 8.2), under {@code /guhs einde2 sponsguh d1stad}:
 * <ul>
 *   <li>{@code scene <naam>}: forgets one of the slice's scenes and plays it at its own anchor (nothing of the story changes);</li>
 *   <li>{@code spel kwallen}: part 1 at step 5 with the net (the scene patguh skipped), at the edge of the Kwallenveld: step in
 *       and catch;</li>
 *   <li>{@code spel examen}: part 1 at step 6 after the scene pufguh, beside the start line: the boat waits, click it;</li>
 *   <li>{@code kwal}: one jelly more in your jar (step 5); {@code stand}: the step, the halves, the counter.</li>
 * </ul>
 * The two steps from their beginning: the generic {@code /guhs einde2 sponsguh ga sponsguh_recept 5} (or {@code 6}) puts you in
 * front of Patguh (juf Pufguh) and the step's scene starts.
 */
final class D1StadCommando {
    private D1StadCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (!SpCommando.dev()) {
            return;
        }
        SpCommando.groep(event, Commands.literal("d1stad")
                .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.asList(D1StadScenes.NAMEN), b))
                        .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("spel").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("kwallen", "examen"), b))
                        .executes(c -> spel(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("kwal").executes(c -> kwal(c.getSource())))
                .then(Commands.literal("stand").executes(c -> stand(c.getSource()))));
    }

    private static int scene(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Cutscene s = D1StadScenes.van(naam);
        if (s == null) {
            src.sendFailure(Component.literal("d1stad: no scene " + naam + " " + Arrays.toString(D1StadScenes.NAMEN)));
            return 0;
        }
        Cutscenes.vergeet(p, s.id());
        D1Stad.speel(p, s, D1StadScenes.anker(naam), q -> {
        });
        src.sendSuccess(() -> Component.literal("d1stad: scene " + s.id() + " (" + s.duur() / 20.0 + " s) at " + D1StadScenes.anker(naam)), false);
        return 1;
    }

    private static int spel(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (naam.equals("kwallen")) {
            SpVerhaalDev.zetTot(p, SpVerhaal.RECEPT, D1Stad.KWALLEN);
            D1Stad.lijn().vlag(p, D1Stad.V_NET, true);
            Kwallen.geefNet(p);
            nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
            E2SpD1StadFeature.naar(p, "kwallen_start", 2.0, -3.0, 135f);
        } else if (naam.equals("examen")) {
            SpVerhaalDev.zetTot(p, SpVerhaal.RECEPT, D1Stad.EXAMEN);
            D1Stad.lijn().vlag(p, D1Stad.V_EXAMEN, true);
            nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
            E2SpD1StadFeature.naar(p, "examen_start", -2.0, 3.0, 180f);
        } else {
            src.sendFailure(Component.literal("d1stad: spel kwallen | examen"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("d1stad: " + naam + " ready"), false);
        return 1;
    }

    private static int kwal(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!D1Stad.aanZet(p, D1Stad.KWALLEN) || !D1Stad.tweedeHelft(p, D1Stad.KWALLEN)) {
            src.sendFailure(Component.literal("d1stad: not catching jellies now (spel kwallen)"));
            return 0;
        }
        Kwallen.tel(p);
        return 1;
    }

    private static int stand(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        var l = D1Stad.lijn();
        String uit = "d1stad: step " + l.stap(p) + " sleutel " + l.sleutel(p) + ", net=" + l.vlag(p, D1Stad.V_NET) + " kwallen="
                + l.teller(p, D1Stad.T_KWALLEN) + "/" + Kwallen.NODIG + ", examen=" + l.vlag(p, D1Stad.V_EXAMEN) + " geslaagd="
                + l.vlag(p, D1Stad.V_GESLAAGD) + ", rit poort " + Examen.spel(p).rit.poort() + " geraakt " + Examen.spel(p).rit.geraakt();
        src.sendSuccess(() -> Component.literal(uit), false);
        return 1;
    }
}
