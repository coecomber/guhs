package nl.juiced.guhs.feature.einde2.geestenwereld;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.Plekken;

/**
 * einde2, slice geestenwereld: the dev commands (a dev run only, plain literals; the story's way in is phase B):
 * <ul>
 *   <li>{@code /guhs einde2 geest ga}: to the arrival in the Meditatiecirkel (your body's spot is remembered);</li>
 *   <li>{@code /guhs einde2 geest plek <zone>}: to a zone (meditatiecirkel, gefluisterwoud, koh_grot, lotus_ruines,
 *       top_voorgangers, hemelvijver, theepaviljoen, fang_pad), or to any other marker of the build by its id;</li>
 *   <li>{@code /guhs einde2 geest terug}: back to your body (what a meditatiesteen does);</li>
 *   <li>{@code /guhs einde2 geest plekken}: the markers of the build near you (16 blocks), with their world positions;</li>
 *   <li>{@code /guhs einde2 geest laag <naam> aan|uit}: puts an overlay of the build into the world for everyone, or takes it
 *       away (pad_8, woud_bloei, lantaarnboom_licht; to look at them: the story shows them per player, geest_spel).</li>
 * </ul>
 */
public final class GeestCommando {
    private GeestCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("geest")
                        .then(Commands.literal("ga").executes(c -> ga(c.getSource(), Geestenwereld.AANKOMST)))
                        .then(Commands.literal("plek").then(Commands.argument("plek", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(namen(), b))
                                .executes(c -> ga(c.getSource(), naarMarker(StringArgumentType.getString(c, "plek"))))))
                        .then(Commands.literal("terug").executes(c -> terug(c.getSource())))
                        .then(Commands.literal("plekken").executes(c -> plekken(c.getSource())))
                        .then(Commands.literal("laag").then(Commands.argument("laag", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Geestenwereld.LAGEN, b))
                                .then(Commands.literal("aan").executes(c -> laag(c.getSource(), StringArgumentType.getString(c, "laag"), false)))
                                .then(Commands.literal("uit").executes(c -> laag(c.getSource(), StringArgumentType.getString(c, "laag"), true))))))));
    }

    /** The zones first, then every other marker. */
    private static List<String> namen() {
        List<String> uit = new ArrayList<>(Geestenwereld.ZONES);
        for (String id : Plekken.van(Geestenwereld.NAAM).keySet()) {
            if (!id.startsWith("plek_")) {
                uit.add(id);
            }
        }
        return uit;
    }

    /** A zone name means its plek_ marker. */
    static String naarMarker(String naam) {
        return Geestenwereld.ZONES.contains(naam) ? "plek_" + naam : naam;
    }

    private static int ga(CommandSourceStack source, String plek) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (!Geestenwereld.stuur(p, plek)) {
            source.sendFailure(Component.literal("Geestenwereld: no dimension or no marker '" + plek + "'"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Geestenwereld: " + plek + " " + Geestenwereld.wereld(plek).toShortString()), false);
        return 1;
    }

    private static int terug(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return Geestenwereld.terug(source.getPlayerOrException()) ? 1 : 0;
    }

    private static int laag(CommandSourceStack source, String naam, boolean weg) {
        var level = Geestenwereld.level(source.getServer());
        if (level == null || !Geestenwereld.laag(level, naam, weg)) {
            source.sendFailure(Component.literal("Geestenwereld: no overlay '" + naam + "' (or its chunks are not loaded: stand near it)"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Geestenwereld: overlay " + naam + (weg ? " taken away" : " put down")), false);
        return 1;
    }

    private static int plekken(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int n = 0;
        for (Plekken.Plek plek : Plekken.van(Geestenwereld.NAAM).values()) {
            var at = Geestenwereld.OORSPRONG.offset(plek.pos());
            if (at.distSqr(p.blockPosition()) <= 16 * 16) {
                n++;
                source.sendSuccess(() -> Component.literal("  " + plek.id() + " (" + plek.soort() + ") " + at.toShortString()), false);
            }
        }
        int totaal = n;
        source.sendSuccess(() -> Component.literal("Geestenwereld: " + totaal + " markers within 16 blocks"), false);
        return n;
    }
}
