package nl.juiced.guhs.feature.einde2.bouwaarde;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * einde2, slice bouw_aarde: {@code /guhs einde2 bouw_aarde koolkraam <1-3> heel|kapot|weg}: puts the koolman's stall on one
 * of its three spots in Ba Sing Guh (for everybody), for looking at the two overlays (tools/autocheck/einde2_bouw_aarde.txt).
 * It changes blocks of the world, so it only exists in a dev run (never on the official world).
 */
public final class E2BouwAardeCommando {
    private E2BouwAardeCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        var nr = Commands.argument("nr", IntegerArgumentType.integer(1, Koolkraam.SPOTS));
        for (Koolkraam.Staat staat : Koolkraam.Staat.values()) {
            nr.then(Commands.literal(staat.name().toLowerCase(java.util.Locale.ROOT))
                    .executes(c -> koolkraam(c.getSource(), IntegerArgumentType.getInteger(c, "nr"), staat)));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("bouw_aarde")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("koolkraam").then(nr)))));
    }

    private static int koolkraam(CommandSourceStack source, int nr, Koolkraam.Staat staat) {
        boolean ok = Koolkraam.zet(source.getLevel(), nr, staat);
        source.sendSuccess(() -> Component.literal(ok ? "koolkraam " + nr + ": " + staat + " (" + Koolkraam.hoek(source.getLevel(), nr) + ")"
                : "no Ba Sing Guh in this dimension (go to the Guheinde), or its chunks are not loaded"), false);
        return ok ? 1 : 0;
    }
}
