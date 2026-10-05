package nl.juiced.guhs.feature.guhpixel.among;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.among.model.Balans;
import nl.juiced.guhs.feature.guhpixel.among.model.Deelnemer;
import nl.juiced.guhs.feature.guhpixel.among.model.Schip;
import nl.juiced.guhs.feature.guhpixel.among.model.Simulatie;

/**
 * The dev commands of Among Guhs (gamemasters, the console, AutoCheck scripts), under {@code /guhs px among}:
 * {@code speel [normaal|lastig] [mika|crew]} (a round alone, right now, with that role) · {@code wachtrij} (join the queue
 * from anywhere in the lobby) · {@code rollen} (who is the Mika) · {@code vergader} (a meeting now) · {@code taken} (your
 * tasks are done) · {@code einde} (stop the round) · {@code sim [rondes]} (headless NPC rounds: the win rates) ·
 * {@code cijfers wis}.
 */
public final class AmongCommando {
    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> among = Commands.literal("among");
        for (String niveau : List.of("normaal", "lastig")) {
            LiteralArgumentBuilder<CommandSourceStack> n = Commands.literal(niveau).executes(ctx -> speel(ctx.getSource(), niveau.equals("lastig"), ""));
            for (String rol : List.of("mika", "crew")) {
                n.then(Commands.literal(rol).executes(ctx -> speel(ctx.getSource(), niveau.equals("lastig"), rol)));
            }
            among.then(Commands.literal("speel").then(n));
        }
        among.then(Commands.literal("speel").executes(ctx -> speel(ctx.getSource(), false, "")));
        among.then(Commands.literal("wachtrij").executes(ctx -> AmongWachtrij.erbij(ctx.getSource().getPlayerOrException()) ? 1 : 0));
        among.then(Commands.literal("rollen").executes(ctx -> {
            AmongSessie s = sessie(ctx.getSource());
            if (s == null) {
                return 0;
            }
            for (Deelnemer d : s.ronde.deelnemers) {
                ctx.getSource().sendSuccess(() -> Component.empty().append(s.naam(d.idx))
                        .append(Component.literal(": " + d.rol + (d.wakker ? "" : " (slaapt)") + ", taken " + d.takenKlaar + "/" + d.taken.size())), false);
            }
            return 1;
        }));
        among.then(Commands.literal("vergader").executes(ctx -> {
            AmongSessie s = sessie(ctx.getSource());
            if (s == null) {
                return 0;
            }
            boolean ok = s.ronde.devVergadering(s.idx(ctx.getSource().getPlayerOrException()));
            s.verwerkNu();
            return ok ? 1 : 0;
        }));
        among.then(Commands.literal("taken").executes(ctx -> {
            AmongSessie s = sessie(ctx.getSource());
            if (s == null) {
                return 0;
            }
            s.ronde.devTakenKlaar(s.idx(ctx.getSource().getPlayerOrException()));
            s.verwerkNu();
            return 1;
        }));
        among.then(Commands.literal("einde").executes(ctx -> {
            AmongSessie s = sessie(ctx.getSource());
            if (s == null) {
                return 0;
            }
            s.forceerEinde();
            return 1;
        }));
        among.then(Commands.literal("sim").executes(ctx -> sim(ctx.getSource(), 200))
                .then(Commands.argument("rondes", IntegerArgumentType.integer(1, 5000)).executes(ctx -> sim(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "rondes")))));
        among.then(Commands.literal("cijfers").then(Commands.literal("wis").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            CompoundTag t = AmongBeloning.cijfers(p);
            for (String sleutel : List.copyOf(t.keySet())) {
                t.remove(sleutel);
            }
            PxData.vuil(p.level().getServer());
            return 1;
        })));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("px")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(among)));
    }

    private static AmongSessie sessie(CommandSourceStack bron) throws CommandSyntaxException {
        if (Sessies.van(bron.getPlayerOrException()) instanceof AmongSessie s) {
            return s;
        }
        bron.sendFailure(Component.literal("Je zit niet in een ronde Among Guhs."));
        return null;
    }

    private static int speel(CommandSourceStack bron, boolean lastig, String rol) throws CommandSyntaxException {
        ServerPlayer p = bron.getPlayerOrException();
        CompoundTag opties = new CompoundTag();
        opties.putBoolean("Lastig", lastig);
        if (!rol.isEmpty()) {
            opties.putString("Rol", rol);
        }
        return Sessies.start(AmongSlice.SPEL, List.of(p), opties) != null ? 1 : 0;
    }

    private static int sim(CommandSourceStack bron, int rondes) {
        Schip schip = Schip.standaard();
        String normaal = Simulatie.draai(schip, Balans.normaal(), rondes, 1).tekst("Normaal");
        String lastig = Simulatie.draai(schip, Balans.lastig(), rondes, 2).tekst("Lastig");
        bron.sendSuccess(() -> Component.literal("[among-sim] " + normaal), false);
        bron.sendSuccess(() -> Component.literal("[among-sim] " + lastig), false);
        return 1;
    }

    private AmongCommando() {
    }
}
