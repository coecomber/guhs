package nl.juiced.guhs.feature.hemel;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The op command {@code /guhs hemel} (tests, ops and the visual QA): {@code klopt <true|false>} makes the Knuffelhart beat
 * (or sleep) for you without the questline, {@code scherm} walks you to the nearest Knuffelhart (within 24 blocks sideways, 56 up or down) and opens
 * its revive screen.
 */
public final class HemelCommando {
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("hemel").requires(s -> s.hasPermission(2))
                .then(Commands.literal("klopt").then(Commands.argument("ja", BoolArgumentType.bool())
                        .executes(ctx -> klopt(ctx.getSource(), BoolArgumentType.getBool(ctx, "ja")))))
                .then(Commands.literal("scherm").executes(ctx -> scherm(ctx.getSource())))));
    }

    private static int klopt(CommandSourceStack source, boolean ja) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (ja) {
            HemelQuest.wakker(p);
        } else {
            HemelQuest.vergeet(p);
        }
        source.sendSuccess(() -> Component.literal("Knuffelhart " + (ja ? "klopt" : "slaapt") + " voor " + p.getName().getString()), true);
        return 1;
    }

    private static int scherm(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        BlockPos hart = Wolkenhoeder.zoekHart(p.level(), p.blockPosition(), 24, 56);
        if (hart == null) {
            source.sendFailure(Component.literal("Geen Knuffelhart in de buurt"));
            return 0;
        }
        p.teleportTo(p.serverLevel(), hart.getX() + 0.5, hart.getY() - 1, hart.getZ() + 3.5, 180f, 10f);
        Hemel.openScherm(p, hart);
        return 1;
    }

    private HemelCommando() {
    }
}
