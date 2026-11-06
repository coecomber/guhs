package nl.juiced.guhs.feature.vadsrijk.hud;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * U-HUD commands:
 * <ul>
 *   <li>{@code /emote <naam>} (everyone): play an emote ({@code /emote stop} ends a loop emote);</li>
 *   <li>{@code /guhs vadsrijk hud bewerker}: open the HUD editor (players: pause menu button or the client command /hudbewerker);</li>
 *   <li>{@code /guhs vadsrijk hud party nep [aantal] [seconden]}: fake party members for testing alone ({@code nep 0} = off);</li>
 *   <li>{@code /guhs vadsrijk hud emote <naam> [speler]}: let a player emote (e.g. a mock/second player).</li>
 * </ul>
 */
final class UHudCommando {
    private static final SuggestionProvider<CommandSourceStack> NAMEN = (c, b) -> {
        java.util.List<String> l = new java.util.ArrayList<>();
        for (Emote e : Emote.values()) {
            l.add(e.id);
        }
        l.add("stop");
        return SharedSuggestionProvider.suggest(l, b);
    };

    private UHudCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("emote")
                .then(Commands.argument("naam", StringArgumentType.word()).suggests(NAMEN).executes(c -> emote(c, c.getSource().getPlayerOrException()))));
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("hud").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("bewerker").executes(c -> {
            PacketDistributor.sendToPlayer(c.getSource().getPlayerOrException(), new UHudPayloads.OpenBewerker());
            return 1;
        }));
        k.then(Commands.literal("party").then(Commands.literal("nep").executes(c -> nep(c, 4, 300))
                .then(Commands.argument("aantal", IntegerArgumentType.integer(0, 4)).executes(c -> nep(c, IntegerArgumentType.getInteger(c, "aantal"), 300))
                        .then(Commands.argument("seconden", IntegerArgumentType.integer(5, 3600))
                                .executes(c -> nep(c, IntegerArgumentType.getInteger(c, "aantal"), IntegerArgumentType.getInteger(c, "seconden")))))));
        k.then(Commands.literal("emote").then(Commands.argument("naam", StringArgumentType.word()).suggests(NAMEN)
                .executes(c -> emote(c, c.getSource().getPlayerOrException()))
                .then(Commands.argument("speler", EntityArgument.player()).executes(c -> emote(c, EntityArgument.getPlayer(c, "speler"))))));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static int emote(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        String naam = StringArgumentType.getString(c, "naam");
        if ("stop".equals(naam)) {
            Emotes.stop(p);
            return 1;
        }
        Emote e = Emote.van(naam);
        if (e == null) {
            c.getSource().sendFailure(Component.translatable("vadsrijk.uhud.emote.onbekend", naam));
            return 0;
        }
        Emotes.speel(p, e, true);
        return 1;
    }

    private static int nep(CommandContext<CommandSourceStack> c, int aantal, int sec) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        PartyFrames.nep(p, aantal, sec);
        c.getSource().sendSuccess(() -> Component.literal(aantal <= 0 ? "Nep-party uit." : "Nep-party: " + aantal + " leden, " + sec + " s."), false);
        return 1;
    }
}
