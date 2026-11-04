package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.Arrays;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * G-MOUNTS dev command (op 2): {@code /guhs vadsrijk mount geef <soort> [kleur]}, {@code alle}, {@code reset}, {@code starter}
 * (opens the zone-3 starter choice), {@code scherm} / {@code stal} (the mount screen without / with the shop), {@code rijd <soort>
 * [kleur]} (unlock + pick + sit on it at once).
 */
final class RijdierCommando {
    private RijdierCommando() {
    }

    private static void geef(ServerPlayer p, RijdierSoort s, String kleur) {
        if (s.kleuren()) {
            PluimKleur k = PluimKleur.vanOfNull(kleur);
            Rijdieren.geefKleur(p, k == null ? PluimKleur.ROZE : k);
        } else {
            Rijdieren.geef(p, s);
        }
    }

    private static int geef(CommandContext<CommandSourceStack> c, String kleur, boolean rijd) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        RijdierSoort s = RijdierSoort.van(StringArgumentType.getString(c, "soort"));
        if (s == null) {
            c.getSource().sendFailure(Component.literal("onbekend rijdier"));
            return 0;
        }
        geef(p, s, kleur);
        if (rijd) {
            PluimKleur k = PluimKleur.vanOfNull(kleur);
            Rijdieren.kies(p, s, s.kleuren() ? (k == null ? Rijdieren.kleur(p, s) : k) : null);
            Rijdieren.vergeet(p);
            if (p.isPassenger()) {
                p.stopRiding();
            }
            Rijdieren.roep(p, s);
        }
        return 1;
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> m = Commands.literal("mount").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        var soorten = Commands.argument("soort", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(RijdierSoort.values()).map(RijdierSoort::id), b));
        var kleuren = Commands.argument("kleur", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(PluimKleur.values()).map(PluimKleur::id), b));
        m.then(Commands.literal("geef").then(soorten.executes(c -> geef(c, "", false))
                .then(kleuren.executes(c -> geef(c, StringArgumentType.getString(c, "kleur"), false)))));
        m.then(Commands.literal("rijd").then(Commands.argument("soort", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(RijdierSoort.values()).map(RijdierSoort::id), b))
                .executes(c -> geef(c, "", true))
                .then(Commands.argument("kleur", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(PluimKleur.values()).map(PluimKleur::id), b))
                        .executes(c -> geef(c, StringArgumentType.getString(c, "kleur"), true)))));
        m.then(Commands.literal("alle").executes(c -> {
            Rijdieren.alles(c.getSource().getPlayerOrException());
            return 1;
        }));
        m.then(Commands.literal("reset").executes(c -> {
            Rijdieren.reset(c.getSource().getPlayerOrException());
            c.getSource().sendSuccess(() -> Component.literal("rijdieren gewist"), false);
            return 1;
        }));
        m.then(Commands.literal("starter").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (!Rijdieren.kiesStarter(p)) {
                c.getSource().sendFailure(Component.literal("starter al gekozen (eerst /guhs vadsrijk mount reset)"));
                return 0;
            }
            return 1;
        }));
        m.then(Commands.literal("scherm").executes(c -> {
            Stal.overzicht(c.getSource().getPlayerOrException());
            return 1;
        }));
        m.then(Commands.literal("stal").executes(c -> {
            Stal.open(c.getSource().getPlayerOrException());
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(m));
    }
}
