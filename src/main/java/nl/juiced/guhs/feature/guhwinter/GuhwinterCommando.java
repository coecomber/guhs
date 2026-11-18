package nl.juiced.guhs.feature.guhwinter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.guhwinter.kern.Momenten;
import nl.juiced.guhs.feature.guhwinter.kern.Winterplein;

/**
 * 1.10.9 Guhwinter: {@code /guhs winter ...}.
 * <ul>
 *   <li>everyone: {@code /guhs winter} or {@code /guhs winter waar} (the chat hint): the Winterplein goes in your Reisguh
 *       list, and the way there;</li>
 *   <li>op 2: {@code status} (phase, date, next moments, listeners), {@code datum <yyyy-MM-dd HH:mm>|echt} (the dev
 *       override, saved; the clock keeps running from there), {@code moment opnieuw <id>} (forget that a listener fired).</li>
 * </ul>
 * Other slices add their own subcommands with {@link #voegToe} (from their Feature.register), e.g.
 * {@code GuhwinterCommando.voegToe(ctx -> GuhwinterCommando.dev("inrichten").executes(...))}.
 */
public final class GuhwinterCommando {
    private static final String P = "guhwinter.kern.cmd.";
    private static final List<Function<CommandBuildContext, ? extends ArgumentBuilder<CommandSourceStack, ?>>> SUBS = new CopyOnWriteArrayList<>();

    private GuhwinterCommando() {
    }

    /** A subcommand under {@code /guhs winter}; give it its own requirement (see {@link #dev}). Call before the server starts. */
    public static void voegToe(Function<CommandBuildContext, ? extends ArgumentBuilder<CommandSourceStack, ?>> sub) {
        SUBS.add(sub);
    }

    /** A literal that only ops (level 2) can use. */
    public static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(GuhwinterCommando::opCommandos);
    }

    static void opCommandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> w = Commands.literal("winter").executes(GuhwinterCommando::waar);
        w.then(Commands.literal("waar").executes(GuhwinterCommando::waar));
        w.then(dev("status").executes(GuhwinterCommando::status));
        w.then(dev("datum")
                .then(Commands.literal("echt").executes(c -> {
                    Guhwinter.zetEcht(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.translatable(P + "datum.echt", tekst(Guhwinter.nu(c.getSource().getServer())),
                            Component.translatable(Guhwinter.fase(c.getSource().getServer()).sleutel())), true);
                    return 1;
                }))
                .then(Commands.argument("datum", StringArgumentType.greedyString())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Guhwinter.MOMENTEN.values().stream().map(t -> t.format(Guhwinter.DATUM)), b))
                        .executes(GuhwinterCommando::datum)));
        w.then(dev("moment").then(Commands.literal("opnieuw").then(Commands.argument("id", StringArgumentType.greedyString())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Guhwinter.momenten().stream().map(Momenten.Luisteraar::id), b))
                .executes(c -> {
                    String id = StringArgumentType.getString(c, "id").strip();
                    boolean was = Guhwinter.vergeetMoment(c.getSource().getServer(), id);
                    c.getSource().sendSuccess(() -> Component.translatable(P + (was ? "moment.opnieuw" : "moment.nooit"), id), true);
                    return was ? 1 : 0;
                }))));
        for (var sub : SUBS) {
            w.then(sub.apply(event.getBuildContext()));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(w));
    }

    private static int waar(CommandContext<CommandSourceStack> c) {
        MinecraftServer s = c.getSource().getServer();
        if (!Guhwinter.bezig(s)) {
            c.getSource().sendSuccess(() -> Component.translatable(P + "waar.voorbij").withStyle(ChatFormatting.AQUA), false);
            return 0;
        }
        BlockPos pos = Winterplein.zorg(s);
        if (c.getSource().getEntity() instanceof ServerPlayer p) {
            Winterplein.ontdek(p);
        }
        BlockPos plek = pos == null ? Winterplein.STANDAARD : pos;
        c.getSource().sendSuccess(() -> Component.translatable(P + "waar", Component.translatable("guhwinter.kern.reis.winterplein")
                .withStyle(ChatFormatting.AQUA), plek.getX(), plek.getZ()).withStyle(ChatFormatting.WHITE), false);
        return 1;
    }

    private static int datum(CommandContext<CommandSourceStack> c) {
        MinecraftServer s = c.getSource().getServer();
        String tekst = StringArgumentType.getString(c, "datum");
        LocalDateTime d = Guhwinter.leesDatum(tekst);
        if (d == null) {
            c.getSource().sendFailure(Component.translatable(P + "datum.fout", tekst));
            return 0;
        }
        Guhwinter.zetDatum(s, d);
        c.getSource().sendSuccess(() -> Component.translatable(P + "datum.gezet", tekst(Guhwinter.nu(s)), Component.translatable(Guhwinter.fase(s).sleutel())), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        MinecraftServer s = c.getSource().getServer();
        ZonedDateTime nu = Guhwinter.nu(s);
        GuhwinterData d = GuhwinterData.get(s);
        c.getSource().sendSuccess(() -> Component.translatable(P + "status", Component.translatable(Guhwinter.fase(s).sleutel()).withStyle(ChatFormatting.AQUA),
                tekst(nu), Component.translatable(P + (Guhwinter.nepDatum(s) ? "status.nep" : "status.echt"))), false);
        int getoond = 0;
        for (var e : Guhwinter.MOMENTEN.entrySet()) {
            if (e.getValue().isAfter(nu) && getoond++ < 5) {
                c.getSource().sendSuccess(() -> Component.translatable(P + "status.moment", tekst(e.getValue()), e.getKey(), duur(Duration.between(nu, e.getValue()))), false);
            }
        }
        if (getoond == 0) {
            c.getSource().sendSuccess(() -> Component.translatable(P + "status.geen"), false);
        }
        List<Momenten.Luisteraar> alle = Guhwinter.momenten();
        long klaar = alle.stream().filter(l -> d.gevuurd(l.id())).count();
        c.getSource().sendSuccess(() -> Component.translatable(P + "status.luisteraars", alle.size(), klaar), false);
        for (Momenten.Luisteraar l : alle) {
            boolean v = d.gevuurd(l.id());
            c.getSource().sendSuccess(() -> Component.literal((v ? "  [x] " : "  [ ] ") + l.id()).withStyle(v ? ChatFormatting.GRAY : ChatFormatting.WHITE), false);
        }
        return 1;
    }

    static String tekst(ZonedDateTime t) {
        return t.withZoneSameInstant(Guhwinter.ZONE).format(Guhwinter.DATUM);
    }

    static String duur(Duration d) {
        long min = Math.max(0, d.toMinutes());
        return min >= 1440 ? (min / 1440) + "d " + (min % 1440) / 60 + "h" : min >= 60 ? (min / 60) + "h " + (min % 60) + "m" : min + "m";
    }
}
