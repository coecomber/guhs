package nl.juiced.guhs.feature.safari.kern;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Op Safari! step 2 (S0): wires the shared safari core ({@link SafariSpeler}, {@link SafariMunten}, {@link SafariEvents},
 * {@link SafariSync}) and its dev commands (ops): {@code /guhs safari munten [n]} (show / set your safarimunten) and
 * {@code /guhs safari wis} (forget everything you did in the safari).
 */
public final class SafariKern {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(SafariSync::bijInloggen);
        NeoForge.EVENT_BUS.addListener(SafariSync::bijWereldWissel);
        NeoForge.EVENT_BUS.addListener(SafariKern::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
        SafariSync.payloads(registrar);
    }

    private static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("safari")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("munten")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.guhs.safari.munten", SafariMunten.saldo(p)), false);
                            return 1;
                        })
                        .then(Commands.argument("n", IntegerArgumentType.integer(0)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            SafariMunten.zet(p.level().getServer(), p.getUUID(), IntegerArgumentType.getInteger(ctx, "n"));
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.guhs.safari.munten", SafariMunten.saldo(p)), true);
                            return 1;
                        })))
                .then(Commands.literal("wis").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    SafariSpeler.wis(p.level().getServer(), p.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.guhs.safari.wis"), true);
                    return 1;
                }))));
    }

    private SafariKern() {
    }
}
