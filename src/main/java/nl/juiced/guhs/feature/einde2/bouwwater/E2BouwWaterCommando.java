package nl.juiced.guhs.feature.einde2.bouwwater;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * einde2, slice bouw_water: {@code /guhs einde2 bouw_water vloot aan|uit}, for looking at the fleet of the Vuurnatie in
 * front of the ice wall (the overlay {@code noordelijke_waterstam/laag_vloot}, put down for everybody with
 * {@link Plekken#plaatsLaag} and taken away again with {@link Plekken#ruimLaag}; tools/autocheck/einde2_bouw_water.txt uses
 * it). The chunks of the sea in front of the wall must be loaded: stand on the Golfkansel
 * ({@code /guhs einde2 plek noordelijke_waterstam golf_plek}).
 * <p>
 * It changes blocks in the world, so it only exists in a dev run (never on the official world).
 */
public final class E2BouwWaterCommando {
    public static final String VLOOT = "vloot";

    private E2BouwWaterCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("bouw_water")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal(VLOOT)
                        .then(Commands.literal("aan").executes(c -> vloot(c.getSource(), true)))
                        .then(Commands.literal("uit").executes(c -> vloot(c.getSource(), false)))))));
    }

    private static int vloot(CommandSourceStack source, boolean aan) {
        ServerLevel level = source.getServer().getLevel(GuheindeFeature.GUHEINDE);
        boolean gelukt = level != null && (aan ? Plekken.plaatsLaag(level, Einde2Ids.NOORDELIJKE_WATERSTAM, VLOOT)
                : Plekken.ruimLaag(level, Einde2Ids.NOORDELIJKE_WATERSTAM, VLOOT));
        if (!gelukt) {
            source.sendFailure(Component.literal("Njeg: de vloot kan hier niet " + (aan ? "komen" : "weg") + " (geen Waterstam in deze wereld, of de zee is niet geladen)."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(aan ? "De vloot van de Vuurnatie ligt voor de muur." : "De vloot is weg."), false);
        return 1;
    }
}
