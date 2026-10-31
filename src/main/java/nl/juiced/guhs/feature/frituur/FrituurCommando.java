package nl.juiced.guhs.feature.frituur;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Op test commands: {@code /guhs snack leer <id>}, {@code /guhs snack alles}, {@code /guhs snack lijst}. */
final class FrituurCommando {
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("snack")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("leer").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SnackRecepten.ALLE, b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            SnackRecepten.leer(p, StringArgumentType.getString(c, "id"));
                            return lijst(p);
                        })))
                .then(Commands.literal("alles").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    SnackRecepten.ALLE.forEach(id -> SnackRecepten.leer(p, id));
                    return lijst(p);
                }))
                .then(Commands.literal("lijst").executes(c -> lijst(c.getSource().getPlayerOrException())))));
    }

    private static int lijst(ServerPlayer p) {
        p.sendSystemMessage(Component.literal("Snackrecepten: " + SnackRecepten.bekend(p)));
        return 1;
    }

    private FrituurCommando() {
    }
}
