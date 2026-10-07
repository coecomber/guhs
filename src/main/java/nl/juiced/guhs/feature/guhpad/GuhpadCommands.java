package nl.juiced.guhs.feature.guhpad;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.GrootVerhaal;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.Wereld;
import nl.juiced.guhs.feature.verhaal.Doel;

/**
 * Het Guhpad: the op commands {@code /guhs guhpad ...} (dev checks, the AutoCheck script tools/autocheck/bbq2_guhpad.txt,
 * and a server admin who has to help a player whose story got stuck; the texts are plain literals: only ops read them).
 * <pre>
 * /guhs guhpad stand [speler]                    every big story (done or not), what each world still asks, the counter,
 *                                                and where "Mijn verhaal" would point
 * /guhs guhpad gedaan &lt;speler&gt; &lt;verhaal|alles&gt;    marks a big story (or all of them) as finished for the player. Only the
 *                                                Guhpad reads the mark: the story itself, its rewards and its title are not
 *                                                touched, and the player can still play it
 * /guhs guhpad vergeet &lt;speler&gt; &lt;verhaal|alles&gt;   takes such a mark away again (a story that was really finished stays finished)
 * </pre>
 */
final class GuhpadCommands {
    private static final String ALLES = "alles";

    static void register(RegisterCommandsEvent event) {
        var pad = Commands.literal("guhpad").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        pad.then(Commands.literal("stand").executes(c -> stand(c, c.getSource().getPlayerOrException()))
                .then(Commands.argument("speler", EntityArgument.player()).executes(c -> stand(c, EntityArgument.getPlayer(c, "speler")))));
        for (boolean gedaan : new boolean[] {true, false}) {
            pad.then(Commands.literal(gedaan ? "gedaan" : "vergeet").then(Commands.argument("speler", EntityArgument.player())
                    .then(Commands.argument("verhaal", StringArgumentType.word()).suggests((c, b) -> {
                        List<String> ids = new ArrayList<>(GroteVerhalen.alle().stream().map(GrootVerhaal::id).toList());
                        ids.add(ALLES);
                        return SharedSuggestionProvider.suggest(ids, b);
                    }).executes(c -> zet(c, EntityArgument.getPlayer(c, "speler"), StringArgumentType.getString(c, "verhaal"), gedaan)))));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(pad));
    }

    private static int zet(CommandContext<CommandSourceStack> c, ServerPlayer p, String id, boolean gedaan) {
        int n = 0;
        for (GrootVerhaal v : GroteVerhalen.alle()) {
            if (id.equals(ALLES) || v.id().equals(id)) {
                GroteVerhalen.zetGedaan(p, v, gedaan);
                n++;
            }
        }
        if (n == 0) {
            c.getSource().sendFailure(Component.literal("[guhpad] no big story \"" + id + "\" (try: "
                    + String.join(", ", GroteVerhalen.alle().stream().map(GrootVerhaal::id).toList()) + ", " + ALLES + ")"));
            return 0;
        }
        GuhpadEvents.kijk(p);
        return zeg(c, p.getScoreboardName() + ": " + n + (gedaan ? " marked as finished" : " mark(s) taken away") + "; followed "
                + GroteVerhalen.gevolgd(p) + " of " + GroteVerhalen.totaal());
    }

    private static int stand(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        StringBuilder s = new StringBuilder(p.getScoreboardName() + ": followed " + GroteVerhalen.gevolgd(p) + " of " + GroteVerhalen.totaal());
        for (Wereld w : Wereld.values()) {
            s.append("\n  ").append(w.id()).append(Guhpad.open(p, w) ? ": open" : ": CLOSED");
            for (GrootVerhaal v : GroteVerhalen.van(w)) {
                s.append("\n    ").append(v.klaar(p) ? "[x] " : "[ ] ").append(v.id());
            }
            List<Guhpad.Eis> mist = Guhpad.ontbreekt(p, w);
            if (!mist.isEmpty()) {
                s.append("\n    still asks: ").append(String.join(", ", mist.stream().map(Guhpad.Eis::sleutel).toList()));
            }
        }
        s.append("\n  may start the Knabbelring: ").append(Guhpad.magKnabbelring(p)).append(", grill portal: ").append(Guhpad.magBarbecuether(p))
                .append(", Guheinde portal: ").append(Guhpad.magGuheinde(p));
        Doel d = GuhpadKompas.doel(p);
        s.append("\n  Mijn verhaal (without a followed questline): ").append(d == null ? "nothing"
                : (d.structuur() != null ? d.structuur() : String.valueOf(d.plek())) + " in " + d.dim().identifier());
        return zeg(c, s.toString());
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal("[guhpad] " + tekst), false);
        return 1;
    }

    private GuhpadCommands() {
    }
}
