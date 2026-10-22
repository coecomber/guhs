package nl.juiced.guhs.feature.safari;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /guhs safari} (ops): {@code /guhs safari} to the park's entrance, {@code terug} back to where you were,
 * {@code herbouw} clears and stamps the whole park again (and then refills it), {@code dieren} removes every animal and
 * fills all homes anew, {@code status} says how far the park and the animals are.
 */
final class SafariCommando {
    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> safari = Commands.literal("safari").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> SafariToegang.naarPark(ctx.getSource().getPlayerOrException()) ? 1 : 0);
        safari.then(Commands.literal("terug").executes(ctx -> SafariToegang.terug(ctx.getSource().getPlayerOrException()) ? 1 : 0));
        safari.then(Commands.literal("herbouw").executes(ctx -> {
            MinecraftServer server = ctx.getSource().getServer();
            ParkData.vergeet();
            SafariOpslag.get(server).dierenVoor = 0;
            ParkBouwer.begin(server, true);
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.guhs.safari.herbouw"), true);
            return 1;
        }));
        safari.then(Commands.literal("dieren").executes(ctx -> {
            Populatie.begin(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.guhs.safari.dieren", ParkData.get().plekken.size()), true);
            return 1;
        }));
        safari.then(Commands.literal("kaartwand").executes(ctx -> {             // round 5: hang the maps again
            int n = Kaartwand.opnieuw(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.guhs.safari.kaartwand", n), true);
            return n;
        }));
        safari.then(Commands.literal("status").executes(ctx -> {
            SafariOpslag s = SafariOpslag.get(ctx.getSource().getServer());
            ParkData d = ParkData.get();
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.guhs.safari.status", d.versie, s.gebouwd, s.bezig == 0 ? "-" : Long.bitCount(s.klaar) + "/36",
                    s.generatie, Populatie.bezig() ? "bezig" : "klaar", d.testPlateau ? "testplateau" : "templates"), false);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(safari));
    }

    private SafariCommando() {
    }
}
