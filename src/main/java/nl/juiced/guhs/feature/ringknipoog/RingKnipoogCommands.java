package nl.juiced.guhs.feature.ringknipoog;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * bbq2 (ring-knipogen): the op commands {@code /guhs ringknipoog ...} (dev checks and the AutoCheck script
 * tools/autocheck/bbq2_ring-knipogen.txt; plain literal texts: nobody but an op ever reads them).
 * <pre>
 * /guhs ringknipoog stand      which of the eight winks the player has seen
 * /guhs ringknipoog vergeet    the player has seen none of them (they all play again when their moment comes)
 * /guhs ringknipoog speel &lt;naam&gt;   plays a wink now, seen or not, in the frame of the building the player stands in
 *                              (baltoguh: Guhvendel, kloon: the mine, spiegel: the tree city, stitch: a Ringenbakker within
 *                              ten blocks, boris: the Frituurberg; sjokkel: on the player's own spot; kistje: the ?-block
 *                              of the player's level, else the block three above their feet). Nothing of the story moves.
 * </pre>
 * A wink is a registered cutscene too: {@code /guhs verhaal scene ringknipoog_<naam> [draai]} plays it anchored on the block
 * the player stands in.
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
        for (Cutscene knipoog : Knipogen.alle()) {
            String naam = knipoog.id().substring("ringknipoog_".length());
            wortel.then(Commands.literal("speel").then(Commands.literal(naam).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Knipogen.Plek plek = Knipogen.plek(p, knipoog);
                boolean speelt = plek != null && Cutscenes.speel(p, knipoog, plek.anker(), plek.draai(), null);
                String uit = plek == null ? "knipogen: the building of " + naam + " is not here"
                        : naam + " at " + plek.anker().toShortString() + " " + plek.draai() + (speelt ? " (" + knipoog.duur() + " ticks)" : ": already watching something");
                c.getSource().sendSuccess(() -> Component.literal(uit), false);
                return speelt ? 1 : 0;
            })));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(wortel));
    }

    private RingKnipoogCommands() {
    }
}
