package nl.juiced.guhs.feature.einde2.bouwlucht;

import java.util.function.Predicate;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.Einde2Haken;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * einde2, slice bouw_lucht: two commands for looking at what a later phase will do with the temple, in a dev run only
 * (they change blocks of the one temple of the world and a rule for everybody, so they never exist on the official world):
 * <ul>
 * <li>{@code /guhs einde2 bouw_lucht ballon <0..5>}: takes the Vredesballon away and puts down this stage of it (0: nothing);
 * the chunks of the building site must be loaded (stand in the temple);</li>
 * <li>{@code /guhs einde2 bouw_lucht deur <0|1>}: 1 lets everybody walk through the closed door of the Werktuigguh, 0 puts
 * back the rule there was.</li>
 * </ul>
 */
public final class E2BouwLuchtCommando {
    private static final int STADIA = 5;
    private static Predicate<Player> oud;

    private E2BouwLuchtCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("bouw_lucht")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ballon").then(Commands.argument("stadium", IntegerArgumentType.integer(0, STADIA))
                        .executes(c -> ballon(c.getSource(), IntegerArgumentType.getInteger(c, "stadium")))))
                .then(Commands.literal("deur").then(Commands.argument("open", IntegerArgumentType.integer(0, 1))
                        .executes(c -> deur(c.getSource(), IntegerArgumentType.getInteger(c, "open") == 1)))))));
    }

    private static int ballon(CommandSourceStack source, int stadium) {
        ServerLevel level = source.getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (level == null || !Plekken.ruimLaag(level, Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, "vredesballon_" + STADIA)) {
            source.sendFailure(Component.literal("The building site is not there or not loaded: stand in the Zuidelijke Luchttempel first."));
            return 0;
        }
        if (stadium > 0 && !Plekken.plaatsLaag(level, Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, "vredesballon_" + stadium)) {
            source.sendFailure(Component.literal("Stage " + stadium + " did not go down."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Vredesballon: stage " + stadium + " of " + STADIA), true);
        return 1;
    }

    private static int deur(CommandSourceStack source, boolean open) {
        if (open && oud == null) {
            oud = Einde2Haken.WERKPLAATS_OPEN;
            Einde2Haken.WERKPLAATS_OPEN = p -> true;
        } else if (!open && oud != null) {
            Einde2Haken.WERKPLAATS_OPEN = oud;
            oud = null;
        }
        source.sendSuccess(() -> Component.literal("The door of the workshop lets " + (open ? "everybody" : "nobody but who the hook names") + " through"), true);
        return 1;
    }
}
