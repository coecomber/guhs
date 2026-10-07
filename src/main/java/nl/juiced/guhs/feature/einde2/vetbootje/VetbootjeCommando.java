package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.Arrays;
import java.util.Locale;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Op commands of the vetbootje slice (testing, the autocheck, a server admin helping a player; they hang next to the other
 * {@code /guhs einde2 ...} commands):
 * <ul>
 *   <li>{@code /guhs einde2 vetbootje geef [speler]}: gives the vetbootje ({@link Vetbootje#geef});</li>
 *   <li>{@code /guhs einde2 vetbootje roep}: as the vetfluitje;</li>
 *   <li>{@code /guhs einde2 vetbootje stel <kleur> <zeil>}: paints your boat (dye colour name, sail name);</li>
 *   <li>{@code /guhs einde2 vetbootje leen}: a boat that belongs to nobody, where you stand;</li>
 *   <li>{@code /guhs einde2 vetbootje ballon [speler]}: gives the heteluchtballon ({@link Heteluchtballon#geef}), or a new
 *       copy when they had one ({@link Heteluchtballon#opnieuw}).</li>
 * </ul>
 */
final class VetbootjeCommando {
    private static final String T = "gui.guhs.einde2_vetbootje.commando.";

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("vetbootje")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("geef").executes(c -> geef(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("speler", EntityArgument.player()).executes(c -> geef(c.getSource(), EntityArgument.getPlayer(c, "speler")))))
                .then(Commands.literal("roep").executes(c -> roep(c.getSource())))
                .then(Commands.literal("stel").then(Commands.argument("kleur", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(DyeColor.values()).map(DyeColor::getName), b))
                        .then(Commands.argument("zeil", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Zeil.values()).map(Zeil::id), b))
                                .executes(c -> stel(c.getSource(), StringArgumentType.getString(c, "kleur"), StringArgumentType.getString(c, "zeil"))))))
                .then(Commands.literal("leen").executes(c -> leen(c.getSource())))
                .then(Commands.literal("ballon").executes(c -> ballon(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("speler", EntityArgument.player()).executes(c -> ballon(c.getSource(), EntityArgument.getPlayer(c, "speler"))))))));
    }

    private static int geef(CommandSourceStack source, ServerPlayer player) {
        VetbootjeEntity boot = Vetbootje.geef(player);
        source.sendSuccess(() -> Component.translatable(T + "geef", player.getDisplayName(), boot.naam()), true);
        return 1;
    }

    private static int roep(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        VetbootjeEntity boot = Vetbootje.roep(player);
        if (boot == null) {
            source.sendFailure(Component.translatable(T + "geen"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(T + "roep", boot.naam()), false);
        return 1;
    }

    private static int stel(CommandSourceStack source, String kleur, String zeil) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DyeColor dye = DyeColor.byName(kleur.toLowerCase(Locale.ROOT), null);
        Zeil z = Arrays.stream(Zeil.values()).filter(v -> v.id().equals(zeil.toLowerCase(Locale.ROOT))).findFirst().orElse(null);
        if (dye == null || z == null || !Vetbootje.heeft(player)) {
            source.sendFailure(Component.translatable(T + "geen"));
            return 0;
        }
        Vetbootje.stel(player, dye, z, Vetbootje.blad(player).naam);
        source.sendSuccess(() -> Component.translatable(T + "stel", dye.getName(), z.id()), false);
        return 1;
    }

    private static int leen(CommandSourceStack source) {
        VetbootjeEntity boot = Vetbootje.leen(source.getLevel(), source.getPosition().add(0, 0.3, 0), source.getRotation().y, Vetbootje.STANDAARD_KLEUR, Zeil.MIKA, null);
        if (boot == null) {
            source.sendFailure(Component.translatable(T + "geen"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(T + "leen"), false);
        return 1;
    }

    private static int ballon(CommandSourceStack source, ServerPlayer player) {
        if (Heteluchtballon.heeft(player)) {
            Heteluchtballon.opnieuw(player);
            source.sendSuccess(() -> Component.translatable(T + "ballon_opnieuw", player.getDisplayName()), true);
        } else {
            Heteluchtballon.geef(player);
            source.sendSuccess(() -> Component.translatable(T + "ballon", player.getDisplayName()), true);
        }
        return 1;
    }

    private VetbootjeCommando() {
    }
}
