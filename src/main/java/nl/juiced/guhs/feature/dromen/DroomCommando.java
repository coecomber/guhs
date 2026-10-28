package nl.juiced.guhs.feature.dromen;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 1.8.0 op commands for Guhdromenland (permission level 2; merged into the /guhs tree; TESTEN_DROMEN.md):
 * <ul>
 *   <li>{@code /guhs droom ga [eiland]}: straight into the dream (home is remembered), or float to that island;</li>
 *   <li>{@code /guhs droom wakker}: wake up;</li>
 *   <li>{@code /guhs droom voortgang}: what this player has; {@code voortgang zet <stukje|finale|snoes|alles>};
 *       {@code voortgang reset};</li>
 *   <li>{@code /guhs droom eilanden opnieuw}: stamp every island again (after a new jar or for testing);</li>
 *   <li>{@code /guhs droom bedtijd aan|uit}: tucking in at any hour (testing by day).</li>
 * </ul>
 */
public final class DroomCommando {
    private DroomCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("droom").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ga").executes(c -> ga(c.getSource(), DroomEilanden.PLEIN))
                        .then(Commands.argument("eiland", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(DroomEilanden.IDS, b))
                                .executes(c -> ga(c.getSource(), StringArgumentType.getString(c, "eiland")))))
                .then(Commands.literal("wakker").executes(c -> wakker(c.getSource())))
                .then(Commands.literal("voortgang").executes(c -> toon(c.getSource()))
                        .then(Commands.literal("zet").then(Commands.argument("wat", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(
                                        java.util.Arrays.stream(DroomVoortgang.Stukje.values()).map(DroomVoortgang.Stukje::id),
                                        java.util.stream.Stream.of("finale", "snoes", "alles")), b))
                                .executes(c -> zet(c.getSource(), StringArgumentType.getString(c, "wat")))))
                        .then(Commands.literal("reset").executes(c -> reset(c.getSource()))))
                .then(Commands.literal("eilanden").then(Commands.literal("opnieuw").executes(c -> opnieuw(c.getSource()))))
                .then(Commands.literal("bedtijd")
                        .then(Commands.literal("aan").executes(c -> bedtijd(c.getSource(), true)))
                        .then(Commands.literal("uit").executes(c -> bedtijd(c.getSource(), false))))));
    }

    private static int ga(CommandSourceStack source, String eiland) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (!DroomEilanden.IDS.contains(eiland)) {
            source.sendFailure(Component.literal("Onbekend eiland: " + eiland + " (" + String.join(", ", DroomEilanden.IDS) + ")"));
            return 0;
        }
        if (DroomReis.inDroom(p)) {
            DroomReis.zweef(p, eiland);
            return 1;
        }
        return DroomReis.naarDroom(p, eiland) ? 1 : 0;
    }

    private static int wakker(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (!DroomReis.wakker(p)) {
            source.sendFailure(Component.literal("Je droomt niet, njeg."));
            return 0;
        }
        return 1;
    }

    private static int toon(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        StringBuilder s = new StringBuilder("Droomvoortgang: ");
        for (DroomVoortgang.Stukje st : DroomVoortgang.Stukje.values()) {
            s.append(st.id()).append(DroomVoortgang.heeft(p, st) ? " ✔ " : " · ");
        }
        s.append("| finale ").append(DroomVoortgang.finaleKlaar(p) ? "✔" : "·");
        s.append(" | snoes geholpen ").append(nl.juiced.guhs.feature.blub.BlubSnoes.heeft(p) ? "✔" : "·");
        source.sendSuccess(() -> Component.literal(s.toString()), false);
        return 1;
    }

    private static int zet(CommandSourceStack source, String wat) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        switch (wat) {
            case "finale" -> DroomVoortgang.zetFinale(p);
            case "snoes" -> DroomVoortgang.snoesGeholpen(p);
            case "alles" -> {
                for (DroomVoortgang.Stukje st : DroomVoortgang.Stukje.values()) {
                    DroomVoortgang.geef(p, st);
                }
            }
            default -> {
                DroomVoortgang.Stukje st = DroomVoortgang.Stukje.van(wat);
                if (st == null) {
                    source.sendFailure(Component.literal("Onbekend: " + wat));
                    return 0;
                }
                DroomVoortgang.geef(p, st);
            }
        }
        return toon(source);
    }

    private static int reset(CommandSourceStack source) throws CommandSyntaxException {
        DroomVoortgang.reset(source.getPlayerOrException());
        return toon(source);
    }

    private static int opnieuw(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(DromenFeature.DIM);
        if (level == null) {
            source.sendFailure(Component.literal("Guhdromenland bestaat niet in deze wereld."));
            return 0;
        }
        DroomEilanden.opnieuw(level);
        source.sendSuccess(() -> Component.literal("Alle droomeilanden opnieuw gestempeld."), true);
        return 1;
    }

    private static int bedtijd(CommandSourceStack source, boolean aan) {
        DroomSlapen.altijd = aan;
        source.sendSuccess(() -> Component.literal("Instoppen kan nu " + (aan ? "altijd" : "alleen 's avonds en 's nachts") + "."), true);
        return 1;
    }
}
