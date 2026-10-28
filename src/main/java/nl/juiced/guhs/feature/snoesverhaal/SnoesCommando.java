package nl.juiced.guhs.feature.snoesverhaal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomReis;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.dromen.DroomVoortgang.Stukje;

/**
 * 1.8.0 op commands for Snoes' story (permission level 2; merged into the /guhs tree; TESTEN_SNOES.md):
 * <ul>
 *   <li>{@code /guhs snoes start}: the story from the beginning: Snoes appears next to you (as if at your bed);</li>
 *   <li>{@code /guhs snoes hoofdstuk <1-5>}: gives that piece (1 liedje, 2 ster, 3 herinnering, 4 bad, 5 droomzand);</li>
 *   <li>{@code /guhs snoes finale}: all five pieces, to the Droomplein: the finale starts there;</li>
 *   <li>{@code /guhs snoes reset}: forgets the story (and the dream progress, and the Blub flag);</li>
 *   <li>{@code /guhs snoes status}: where you are.</li>
 * </ul>
 */
public final class SnoesCommando {
    private SnoesCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("snoes").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("start").executes(c -> start(c.getSource())))
                .then(Commands.literal("hoofdstuk").then(Commands.argument("n", IntegerArgumentType.integer(1, 5))
                        .executes(c -> hoofdstuk(c.getSource(), IntegerArgumentType.getInteger(c, "n")))))
                .then(Commands.literal("finale").executes(c -> finale(c.getSource())))
                .then(Commands.literal("reset").executes(c -> reset(c.getSource())))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))));
    }

    private static int start(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (DroomReis.inDroom(p)) {
            source.sendFailure(Component.literal("Word eerst wakker: Snoes zit bij je bed, niet in je droom."));
            return 0;
        }
        SnoesVerhaal.vergeet(p);
        if (SnoesVerhaal.verschijn(p, p.blockPosition()) == null) {
            source.sendFailure(Component.literal("Geen plekje naast je voor Snoes (ga ergens vrij staan)."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Snoes zit naast je. Praat drie keer met hem (tip: /guhs droom bedtijd aan)."), false);
        return 1;
    }

    private static int hoofdstuk(CommandSourceStack source, int n) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Stukje s = Stukje.values()[n - 1];
        if (!DroomVoortgang.geef(p, s)) {
            source.sendFailure(Component.literal("Dat stukje had je al: " + s.id()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Stukje " + n + " (" + s.id() + ") gegeven: " + DroomVoortgang.aantal(p) + "/5"), false);
        return 1;
    }

    private static int finale(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        for (Stukje s : Stukje.values()) {
            DroomVoortgang.geef(p, s);
        }
        if (DroomReis.inDroom(p)) {
            DroomReis.zweef(p, DroomEilanden.PLEIN);
        } else if (!DroomReis.naarDroom(p, DroomEilanden.PLEIN)) {
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Alle vijf stukjes: de finale begint op het Droomplein."), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        DroomVoortgang.reset(p);          // (its listener forgets the story: SnoesVerhaal.vergeet)
        SnoesVerhaal.vergeet(p);
        source.sendSuccess(() -> Component.literal("Het verhaal van Snoes is vergeten."), false);
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        String s = "Snoes: fase " + SnoesVerhaal.fase(p) + ", stukjes " + DroomVoortgang.aantal(p) + "/5, finale "
                + (DroomVoortgang.finaleKlaar(p) ? "✔" : "·") + ", Bubbels " + (SnoesVerhaal.gids(p) != null ? "in de buurt" : "-");
        source.sendSuccess(() -> Component.literal(s), false);
        return 1;
    }
}
