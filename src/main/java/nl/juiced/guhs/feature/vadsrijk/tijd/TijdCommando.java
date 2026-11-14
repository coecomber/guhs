package nl.juiced.guhs.feature.vadsrijk.tijd;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.ritme.Rooster;
import nl.juiced.guhs.world.GuhTime;

/**
 * 1.10.1 (p1101-tijd), op 2: {@code /guhs vadsrijk tijd} (status), {@code tijd cyclus <minuten>}, {@code tijd dag <0.3-0.9>},
 * {@code tijd zet dag|nacht}, {@code tijd herlaad} (re-read config/guhs-vadsrijk-tijd.properties).
 */
final class TijdCommando {
    static final String P = "vadsrijk.tijd.";

    private TijdCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("tijd").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(TijdCommando::status);
        k.then(Commands.literal("cyclus").then(Commands.argument("minuten", DoubleArgumentType.doubleArg(TijdConfig.MIN_CYCLUS, TijdConfig.MAX_CYCLUS))
                .executes(c -> {
                    TijdConfig.zet(DoubleArgumentType.getDouble(c, "minuten"), TijdConfig.dagDeel(), true);
                    VadsrijkTijd.pas(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.translatable(P + "cyclus.gezet", TijdConfig.tekst(TijdConfig.cyclus()), dagMin(), nachtMin()), true);
                    return 1;
                })));
        k.then(Commands.literal("dag").then(Commands.argument("deel", DoubleArgumentType.doubleArg(TijdConfig.MIN_DAG, TijdConfig.MAX_DAG))
                .executes(c -> {
                    TijdConfig.zet(TijdConfig.cyclus(), DoubleArgumentType.getDouble(c, "deel"), true);
                    VadsrijkTijd.pas(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.translatable(P + "dag.gezet", TijdConfig.tekst(TijdConfig.dagDeel()), dagMin(), nachtMin()), true);
                    return 1;
                })));
        k.then(Commands.literal("zet")
                .then(Commands.literal("dag").executes(c -> zet(c, false)))
                .then(Commands.literal("nacht").executes(c -> zet(c, true))));
        k.then(Commands.literal("herlaad").executes(c -> {
            TijdConfig.lees();
            VadsrijkTijd.pas(c.getSource().getServer());
            c.getSource().sendSuccess(() -> Component.translatable(P + "herlaad", TijdConfig.BESTAND), true);
            return status(c);
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static String dagMin() {
        return TijdConfig.tekst(VadsrijkTijd.dagTicks(TijdConfig.cyclus(), TijdConfig.dagDeel()) / VadsrijkTijd.TICKS_PER_MINUUT);
    }

    private static String nachtMin() {
        return TijdConfig.tekst(VadsrijkTijd.nachtTicks(TijdConfig.cyclus(), TijdConfig.dagDeel()) / VadsrijkTijd.TICKS_PER_MINUUT);
    }

    private static int zet(CommandContext<CommandSourceStack> c, boolean nacht) {
        if (!VadsrijkTijd.zet(c.getSource().getServer(), nacht)) {
            c.getSource().sendFailure(Component.translatable(P + "geen_klok"));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable(P + (nacht ? "zet.nacht" : "zet.dag")), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        MinecraftServer server = c.getSource().getServer();
        long nu = VadsrijkTijd.nu(server);
        if (nu < 0) {
            c.getSource().sendFailure(Component.translatable(P + "geen_klok"));
            return 0;
        }
        ServerLevel vr = Vadsrijk.level(server);
        boolean nacht = vr != null ? Rooster.nacht(vr) : nu % VadsrijkTijd.DAG >= 13000 && nu % VadsrijkTijd.DAG < 23000;
        long ow = GuhTime.timeOfDay(server.overworld());
        c.getSource().sendSuccess(() -> Component.translatable(P + "status", TijdConfig.tekst(TijdConfig.cyclus()), dagMin(), nachtMin(),
                TijdConfig.tekst(TijdConfig.dagDeel())), false);
        c.getSource().sendSuccess(() -> Component.translatable(P + "status.nu", VadsrijkTijd.uur(nu),
                Component.translatable(P + (nacht ? "is_nacht" : "is_dag")), Math.floorDiv(nu, VadsrijkTijd.DAG), Math.floorMod(nu, VadsrijkTijd.DAG),
                TijdConfig.tekst(server.clockManager().getRate(VadsrijkTijd.klok(server).orElseThrow())), VadsrijkTijd.uur(ow)), false);
        c.getSource().sendSuccess(() -> Component.translatable(P + "status.config", TijdConfig.BESTAND), false);
        return 1;
    }
}
