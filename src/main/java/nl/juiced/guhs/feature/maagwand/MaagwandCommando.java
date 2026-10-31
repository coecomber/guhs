package nl.juiced.guhs.feature.maagwand;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /guhs maagwand: info, laag &lt;1-5&gt;, teller &lt;n&gt;, reset. For testing. */
final class MaagwandCommando {
    private MaagwandCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("maagwand").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("info").executes(c -> info(c.getSource())))
                .then(Commands.literal("reset").executes(c -> zet(c.getSource(), Maagwand.MIN, 0)))
                .then(Commands.literal("laag").then(Commands.argument("laag", IntegerArgumentType.integer(Maagwand.MIN, Maagwand.MAX))
                        .executes(c -> zet(c.getSource(), IntegerArgumentType.getInteger(c, "laag"), 0))))
                .then(Commands.literal("teller").then(Commands.argument("teller", IntegerArgumentType.integer(0))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            return zet(c.getSource(), Maagwand.laag(p), IntegerArgumentType.getInteger(c, "teller"));
                        })))));
    }

    private static int zet(CommandSourceStack source, int laag, int teller) throws CommandSyntaxException {
        Maagwand.zet(source.getPlayerOrException(), laag, teller);
        return info(source);
    }

    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int laag = Maagwand.laag(p);
        source.sendSuccess(() -> Component.literal("Maagwand: laag " + laag + ", teller " + Maagwand.teller(p)), false);
        return laag;
    }
}
