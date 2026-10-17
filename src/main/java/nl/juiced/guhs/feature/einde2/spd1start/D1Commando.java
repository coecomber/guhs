package nl.juiced.guhs.feature.einde2.spd1start;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.spverhaal.SpCommando;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 8.2), slice sp_d1_start: dev commands (a dev run only, operators), under the skelet's group:
 * <ul>
 *   <li>{@code /guhs einde2 sponsguh d1start scene <naam>}: forgets the scene and plays it at its own place (the player is put
 *       there first; {@code brief} plays where you stand). Never moves a step;</li>
 *   <li>{@code d1start kaart}: the narrator card of part 1 again;</li>
 *   <li>{@code d1start brief}: the bubble letter right now (step 0 only);</li>
 *   <li>{@code d1start spel bakken}: to the grill and the game at once (step 4 only); {@code d1start spel bestelling}: the
 *       order on the grill done by itself (the next one comes); {@code d1start spel stop}.</li>
 * </ul>
 * Jumping to a step is the skelet's {@code /guhs einde2 sponsguh ga sponsguh_recept <0..4>} (it runs this slice's starts).
 */
final class D1Commando {
    private D1Commando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (!SpCommando.dev()) {
            return;
        }
        SpCommando.groep(event, Commands.literal("d1start")
                .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(D1Scenes.ALLE.stream().map(D1Commando::naam), b))
                        .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("kaart").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Verteller.vergeet(p, E2SpD1Start.KAART);
                    return Verteller.toon(p, E2SpD1Start.KAART, null) ? 1 : 0;
                }))
                .then(Commands.literal("brief").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (E2SpD1Start.stap(p) != SpVerhaal.R_BRIEF) {
                        c.getSource().sendFailure(Component.literal("d1start: not at step 0 (/guhs einde2 sponsguh ga sponsguh_recept 0)"));
                        return 0;
                    }
                    Bubbelbrief.nu(p);
                    boolean ok = Bubbelbrief.zetNu(p);
                    c.getSource().sendSuccess(() -> Component.literal("d1start: bubble " + (ok ? "here" : "could not be made")), false);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("spel")
                        .then(Commands.literal("bakken").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            if (E2SpD1Start.stap(p) != SpVerhaal.R_BAKKEN) {
                                c.getSource().sendFailure(Component.literal("d1start: not at step 4 (/guhs einde2 sponsguh ga sponsguh_recept 4)"));
                                return 0;
                            }
                            E2SpD1Start.zetNaar(p, "grill_speler", 180f);
                            Bakken.start(p);
                            c.getSource().sendSuccess(() -> Component.literal("d1start: grill game " + (Bakken.bezig(p) ? "on" : "NOT started (place unknown?)")), false);
                            return 1;
                        }))
                        .then(Commands.literal("bestelling").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            boolean ok = Bakken.maakBestelling(p);
                            c.getSource().sendSuccess(() -> Component.literal("d1start: " + (ok ? "order done" : "no game on")), false);
                            return ok ? 1 : 0;
                        }))
                        .then(Commands.literal("stop").executes(c -> {
                            Bakken.stop(c.getSource().getPlayerOrException());
                            return 1;
                        }))));
    }

    private static String naam(Cutscene s) {
        return s.id().substring("einde2_sp_d1_start_".length());
    }

    /** The marker each scene is anchored at (brief: the player's own spot). */
    private static String anker(String naam) {
        return switch (naam) {
            case "aankomst" -> "aankomst_speler";
            case "welkom" -> "bubbel_onder";
            case "sollicitatie" -> "krabguh";
            case "bakken_uitleg", "bakken_klaar" -> "grill_speler";
            default -> null;
        };
    }

    private static int scene(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Cutscene s = D1Scenes.ALLE.stream().filter(x -> naam(x).equals(naam)).findFirst().orElse(null);
        if (s == null) {
            src.sendFailure(Component.literal("d1start: no scene " + naam + " (" + D1Scenes.ALLE.stream().map(D1Commando::naam).toList() + ")"));
            return 0;
        }
        String marker = anker(naam);
        BlockPos anker;
        if (marker == null) {
            anker = p.blockPosition();
        } else {
            ServerLevel g = E2SpD1Start.guheinde(p.level().getServer());
            anker = E2SpD1Start.plek(g, marker);
            if (anker == null || !E2SpD1Start.zetNaar(p, marker, p.getYRot())) {
                src.sendFailure(Component.literal("d1start: Bikinibodem's spot is not known yet (try again in a moment)"));
                return 0;
            }
        }
        Cutscenes.vergeet(p, s.id());
        boolean ok = Cutscenes.speel(p, s, anker, Rotation.NONE, null);
        src.sendSuccess(() -> Component.literal("d1start: scene " + naam + (ok ? " playing (" + s.duur() / 20.0 + " s without the clicks)" : " could not start")), false);
        return ok ? 1 : 0;
    }
}
