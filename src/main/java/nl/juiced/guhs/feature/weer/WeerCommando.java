package nl.juiced.guhs.feature.weer;

import java.util.Arrays;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.Minigames;

/**
 * 1.8.0 test build, op commands for the Weer vadser (permission level 2; merged into the /guhs tree):
 * <ul>
 *   <li>{@code /guhs weer potjes}: one of every weerpotje;</li>
 *   <li>{@code /guhs weer ontgrendel alles|<weertje>} and {@code /guhs weer reset} (forget every unlock);</li>
 *   <li>{@code /guhs weer zone <weertje>}: a test zone right here (no block, no vadskracht);</li>
 *   <li>{@code /guhs weer uit}: every test zone of this dimension away.</li>
 * </ul>
 */
public final class WeerCommando {
    private WeerCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("weer").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("potjes").executes(c -> potjes(c.getSource())))
                .then(Commands.literal("ontgrendel")
                        .then(Commands.argument("weertje", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("alles"),
                                        Arrays.stream(Weertje.values()).filter(Weertje::ontgrendelbaar).map(Weertje::id)), b))
                                .executes(c -> ontgrendel(c.getSource(), StringArgumentType.getString(c, "weertje")))))
                .then(Commands.literal("reset").executes(c -> reset(c.getSource())))
                .then(Commands.literal("zone")
                        .then(Commands.argument("weertje", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Weertje.values())
                                        .filter(w -> w != Weertje.NORMAAL).map(Weertje::id), b))
                                .executes(c -> zone(c.getSource(), StringArgumentType.getString(c, "weertje")))))
                .then(Commands.literal("uit").executes(c -> uit(c.getSource())))));
    }

    private static int potjes(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int n = 0;
        for (Weertje w : Weertje.values()) {
            if (w.ontgrendelbaar()) {
                Minigames.give(p, new ItemStack(WeerFeature.potje(w)));
                n++;
            }
        }
        int aantal = n;
        source.sendSuccess(() -> Component.literal("Weerpotjes: " + aantal), false);
        return n;
    }

    private static int ontgrendel(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (id.equals("alles")) {
            for (Weertje w : Weertje.values()) {
                WeerUnlocks.voegToe(p, w);
            }
            source.sendSuccess(() -> Component.literal("Alle guh-weertjes ontgrendeld"), false);
            return 1;
        }
        Weertje w = Weertje.byId(id);
        if (w == null) {
            source.sendFailure(Component.literal("Onbekend weertje: " + id));
            return 0;
        }
        WeerUnlocks.voegToe(p, w);
        source.sendSuccess(() -> Component.literal("Ontgrendeld: " + w.id()), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) throws CommandSyntaxException {
        WeerUnlocks.wis(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("Alle weertjes en eenmalige potjes vergeten"), false);
        return 1;
    }

    private static int zone(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Weertje w = Weertje.byId(id);
        if (w == null || w == Weertje.NORMAAL) {
            source.sendFailure(Component.literal("Onbekend weertje: " + id));
            return 0;
        }
        WeerZones.wisTest(p.level());
        WeerZones.testZone(p.level(), p.blockPosition(), w);
        source.sendSuccess(() -> Component.literal("Testzone " + w.id() + " hier (straal " + WeerZones.STRAAL + ", zoals met Wolkenantenne III); /guhs weer uit = weg"), false);
        return 1;
    }

    private static int uit(CommandSourceStack source) {
        int n = WeerZones.wisTest(source.getLevel());
        source.sendSuccess(() -> Component.literal("Testzones weg: " + n), false);
        return n;
    }
}
