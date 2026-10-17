package nl.juiced.guhs.feature.einde2.spverhaal;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 8): the dev command group {@code /guhs einde2 sponsguh ...} (a dev run only, plain
 * literals, operators):
 * <ul>
 *   <li>{@code stand}: both lines (step, begun, done), the gate, the side lines' steps;</li>
 *   <li>{@code ga <lijn> <stap>}: {@link SpVerhaalDev#zetTot}, then to the step's default place, then the step owner's own start
 *       ({@link SpStappen#ga}) when it has one;</li>
 *   <li>{@code klaar}: completes your current step when it is still a stand-in ({@link SpStandIn#voltooi});</li>
 *   <li>{@code wis}: forgets both lines (the Guhvatar story stays);</li>
 *   <li>{@code poort}: what the gate and the two sluiers answer for you; {@code poort uitleg}: the three gate lines now.</li>
 * </ul>
 * Each slice hangs its own literal under this group ({@link #groep}): {@code /guhs einde2 sponsguh <slice> ...}, e.g.
 * {@code /guhs einde2 sponsguh d1start scene welkom} (CONTRACT_E2_C 8.2). Brigadier merges the trees.
 */
public final class SpCommando {
    private SpCommando() {
    }

    /** True in a dev run (every Sponsguh dev command checks this first). */
    public static boolean dev() {
        return !net.neoforged.fml.loading.FMLEnvironment.isProduction();
    }

    /**
     * Registers {@code /guhs einde2 sponsguh <sub>} (operators): a slice calls this from its own RegisterCommandsEvent listener
     * with its own literal, only when {@link #dev()}.
     */
    public static void groep(RegisterCommandsEvent event, LiteralArgumentBuilder<CommandSourceStack> sub) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("sponsguh").then(sub))));
    }

    static void register(RegisterCommandsEvent event) {
        if (!dev()) {
            return;
        }
        groep(event, Commands.literal("stand").executes(c -> stand(c.getSource())));
        groep(event, Commands.literal("ga").then(Commands.argument("lijn", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(SpVerhaal.LIJNEN, b))
                .then(Commands.argument("stap", IntegerArgumentType.integer(0))
                        .executes(c -> ga(c.getSource(), StringArgumentType.getString(c, "lijn"), IntegerArgumentType.getInteger(c, "stap"))))));
        groep(event, Commands.literal("klaar").executes(c -> klaar(c.getSource())));
        groep(event, Commands.literal("wis").executes(c -> {
            SpVerhaalDev.wis(c.getSource().getPlayerOrException());
            c.getSource().sendSuccess(() -> Component.literal("Sponsguh: both lines forgotten"), false);
            return 1;
        }));
        groep(event, Commands.literal("poort").executes(c -> poort(c.getSource()))
                .then(Commands.literal("uitleg").executes(c -> SpPoort.vertel(c.getSource().getPlayerOrException(), Long.MAX_VALUE / 2) ? 1 : 0)));
    }

    private static int stand(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        StringBuilder sb = new StringBuilder("Sponsguh: open=" + SpVerhaal.open(p) + " magBeginnen=" + SpVerhaal.magBeginnen(p));
        for (String lijn : SpVerhaal.LIJNEN) {
            Verhaallijn l = SpVerhaal.lijn(lijn);
            int s = l.stap(p);
            sb.append("\n ").append(lijn).append(": stap ").append(s).append("/").append(l.stappen()).append(l.begonnen(p) ? " begun" : "")
                    .append(l.klaar(p) ? " DONE" : "").append(s < l.stappen() ? (SpStappen.isEcht(lijn, s) ? " (real)" : " (stand-in)") : "");
        }
        for (String lijn : SpVerhaal.ZIJLIJNEN) {
            Verhaallijn l = SpVerhaal.zijlijn(lijn);
            sb.append("\n ").append(lijn).append(l == null ? ": not registered" : ": stap " + l.stap(p) + "/" + l.stappen());
        }
        sb.append("\n reisDoel=").append(SpVerhaal.reisDoel(p)).append(" recept=").append(SpVerhaal.kentRecept(p));
        String uit = sb.toString();
        src.sendSuccess(() -> Component.literal(uit), false);
        return 1;
    }

    private static int ga(CommandSourceStack src, String lijn, int stap) throws CommandSyntaxException {
        if (!SpVerhaal.LIJNEN.contains(lijn)) {
            src.sendFailure(Component.literal("not a Sponsguh line: " + lijn + " (" + SpVerhaal.LIJNEN + ")"));
            return 0;
        }
        ServerPlayer p = src.getPlayerOrException();
        Verhaallijn l = SpVerhaal.lijn(lijn);
        int s = Math.min(stap, l.stappen());
        SpVerhaalDev.zetTot(p, lijn, s);
        boolean daar = SpVerhaalDev.naarPlek(p, lijn, s);
        boolean start = s < l.stappen() && SpStappen.ga(p, lijn, s);
        src.sendSuccess(() -> Component.literal("Sponsguh: " + lijn + " at step " + s + (daar ? ", at its place" : ", place not known (yet)")
                + (start ? ", started by its slice" : SpStappen.isEcht(lijn, s) ? "" : ", stand-in")), false);
        return 1;
    }

    private static int klaar(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        for (String lijn : SpVerhaal.LIJNEN) {
            if (SpVerhaal.actief(p, lijn)) {
                int s = SpVerhaal.stap(p, lijn);
                boolean ok = SpStandIn.voltooi(p, lijn, s);
                src.sendSuccess(() -> Component.literal(ok ? "Sponsguh: " + lijn + " step " + s + " done (stand-in)"
                        : "Sponsguh: " + lijn + " step " + s + " is a real step: play it, or use its slice's ga"), false);
                return ok ? 1 : 0;
            }
        }
        src.sendFailure(Component.literal("Sponsguh: no line on (is the gate open? /guhs einde2 sponsguh stand)"));
        return 0;
    }

    private static int poort(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        String uit = "Sponsguh gate: open=" + SpVerhaal.open(p) + ", sluier bikinibodem open=" + Sluiers.open(p, SpVerhaal.BIKINIBODEM)
                + ", schelpenstad open=" + Sluiers.open(p, SpVerhaal.SCHELPENSTAD) + ", passes as creative op=" + Sluiers.passeert(p)
                + ", Appa rows=" + SpVerhaal.gebouwen(p);
        src.sendSuccess(() -> Component.literal(uit), false);
        return 1;
    }

    /** The lines in table order (for suggestions of the slices). */
    public static List<String> lijnen() {
        return SpVerhaal.LIJNEN;
    }
}
