package nl.juiced.guhs.feature.einde2.spd1recept;

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
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 "Sponsguh", slice sp_d1_recept: the dev commands (CONTRACT_E2_C 8.2; a dev run only, operators), under
 * {@code /guhs einde2 sponsguh d1recept ...}:
 * <ul>
 *   <li>{@code scene <naam>}: forgets the scene and plays it (plan_z, knipoog_kloon, sluip_uitleg, recept, feest, zoete_zege);</li>
 *   <li>{@code spel sluipen}: part 1 at step 8 with the explanation and the wink seen, at the start of the sneak route;</li>
 *   <li>{@code spel trommel}: part 1 at step 10, behind your drum (the band comes, Octo counts in);</li>
 *   <li>{@code goal}: finishes the minigame you are playing (the sneak: to the safe; the drum: won);</li>
 *   <li>{@code stand}: your step, the slice's flags and what runs.</li>
 * </ul>
 * Jumping to a step is the generic {@code /guhs einde2 sponsguh ga sponsguh_recept <7..10>} (this slice's start of the step).
 */
final class D1rCommando {
    private D1rCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (!SpCommando.dev()) {
            return;
        }
        SpCommando.groep(event, Commands.literal("d1recept")
                .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(D1rScenes.ALLE.keySet(), b))
                        .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("spel")
                        .then(Commands.literal("sluipen").executes(c -> sluipen(c.getSource())))
                        .then(Commands.literal("trommel").executes(c -> trommel(c.getSource()))))
                .then(Commands.literal("goal").executes(c -> goal(c.getSource())))
                .then(Commands.literal("stand").executes(c -> stand(c.getSource()))));
    }

    private static int scene(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        D1rScenes.Scene s = D1rScenes.ALLE.get(naam);
        if (s == null) {
            src.sendFailure(Component.literal("d1recept: no scene " + naam + " (" + D1rScenes.ALLE.keySet() + ")"));
            return 0;
        }
        Cutscenes.vergeet(p, s.id());
        boolean ok = D1rScenes.speel(p, s, null);
        src.sendSuccess(() -> Component.literal("d1recept: " + s.id() + (ok ? " plays (" + s.cutscene().duur() / 20 + " s)" : " could not start")), false);
        return ok ? 1 : 0;
    }

    private static int sluipen(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        SpVerhaalDev.zetTot(p, SpVerhaal.RECEPT, E2SpD1Recept.VETEMMER);
        E2SpD1Recept.vergeet(p);
        Verhaallijn l = E2SpD1Recept.lijn();
        l.vlag(p, E2SpD1Recept.V_UITLEG, true);
        l.vlag(p, E2SpD1Recept.V_KLOON, true);
        l.vlag(p, E2SpD1Recept.V_RECEPT, false);
        boolean ok = E2SpD1Recept.zetOp(p, "sluip_start", 0, 0, 0, -90f);
        src.sendSuccess(() -> Component.literal("d1recept: step 8, the sneak" + (ok ? " (at sluip_start)" : " (Bikinibodem not known yet)")), false);
        return 1;
    }

    private static int trommel(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        SpVerhaalDev.zetTot(p, SpVerhaal.RECEPT, E2SpD1Recept.BELLENKOM);
        E2SpD1Recept.vergeet(p);
        E2SpD1Recept.lijn().vlag(p, E2SpD1Recept.V_GEWONNEN, false);
        boolean ok = E2SpD1Recept.zetOp(p, "d1r_trommel_speler", 0, 0, 0, 0f);
        src.sendSuccess(() -> Component.literal("d1recept: step 10, the drum" + (ok ? " (behind your drum)" : " (Bikinibodem not known yet)")), false);
        return 1;
    }

    private static int goal(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (Sluipen.bezig(p)) {
            boolean ok = E2SpD1Recept.zetOp(p, "receptkluis", -1, -1, 0, -90f);
            src.sendSuccess(() -> Component.literal("d1recept: at the safe" + (ok ? "" : " (not known)")), false);
            return 1;
        }
        if (Trommel.bezig(p)) {
            Trommel.stop(p);
            E2SpD1Recept.trommelGewonnen(p, Trommel.NOTEN);
            src.sendSuccess(() -> Component.literal("d1recept: the drum game is won"), false);
            return 1;
        }
        src.sendFailure(Component.literal("d1recept: no minigame runs (spel sluipen / spel trommel)"));
        return 0;
    }

    private static int stand(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Verhaallijn l = E2SpD1Recept.lijn();
        Sluipen.Voortgang v = Sluipen.van(p);
        Trommel.Ronde r = Trommel.ronde(p);
        String uit = "d1recept: step " + l.stap(p) + " (" + l.sleutel(p) + ")"
                + " uitleg=" + l.vlag(p, E2SpD1Recept.V_UITLEG) + " kloon=" + l.vlag(p, E2SpD1Recept.V_KLOON)
                + " recept=" + l.vlag(p, E2SpD1Recept.V_RECEPT) + " gewonnen=" + l.vlag(p, E2SpD1Recept.V_GEWONNEN)
                + (v == null ? "" : " | sneak: rest " + (v.rust + 1) + "/" + Sluipen.RUSTPUNTEN.size() + ", seen " + v.gezien)
                + (r == null ? "" : " | drum: hits " + r.hits + "/" + Trommel.NODIG + ", bubbles " + r.geweest() + "/" + Trommel.NOTEN)
                + " | actors " + D1rActeurs.alle(p).size();
        src.sendSuccess(() -> Component.literal(uit), false);
        return 1;
    }
}
