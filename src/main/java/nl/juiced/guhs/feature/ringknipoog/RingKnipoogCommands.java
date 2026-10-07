package nl.juiced.guhs.feature.ringknipoog;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.verhaal.Cutscene;

/**
 * bbq2 (ring-knipogen): the op commands {@code /guhs ringknipoog ...} (dev checks and the AutoCheck script
 * tools/autocheck/bbq2_ring-knipogen.txt; plain literal texts: nobody but an op ever reads them).
 * <pre>
 * /guhs ringknipoog stand      which of the seven winks the player has seen
 * /guhs ringknipoog vergeet    the player has seen none of them (they all play again when their moment comes)
 * </pre>
 * A wink is a registered cutscene: {@code /guhs verhaal scene ringknipoog_<naam> [draai]} plays it anchored on the block the
 * player stands on (baltoguh, kistje, spiegel, stitch, boris, sjokkel, kloon).
 */
final class RingKnipoogCommands {
    static void register(RegisterCommandsEvent event) {
        var wortel = Commands.literal("ringknipoog").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        wortel.then(Commands.literal("stand").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            StringBuilder tekst = new StringBuilder("knipogen:");
            int gezien = 0;
            for (Cutscene knipoog : Knipogen.alle()) {
                boolean ja = Knipogen.gezien(p, knipoog);
                gezien += ja ? 1 : 0;
                tekst.append(' ').append(knipoog.id().substring("ringknipoog_".length())).append(ja ? "=gezien" : "=nog niet");
            }
            String uit = tekst + " (" + gezien + " van " + Knipogen.alle().size() + ")";
            c.getSource().sendSuccess(() -> Component.literal(uit), false);
            return gezien;
        }));
        wortel.then(Commands.literal("vergeet").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Knipogen.wis(p);
            c.getSource().sendSuccess(() -> Component.literal("knipogen: " + p.getScoreboardName() + " heeft er geen een gezien"), false);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(wortel));
    }

    private RingKnipoogCommands() {
    }
}
