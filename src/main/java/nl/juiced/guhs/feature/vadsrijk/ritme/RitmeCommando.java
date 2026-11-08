package nl.juiced.guhs.feature.vadsrijk.ritme;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;

/**
 * U-LEVEN-RITME dev commands ({@code /guhs vadsrijk ritme ...}, op 2):
 * <ul>
 *   <li>{@code nacht | dag | auto}: force the town schedule (shops close, sleepers, owls, night market, lights, fireflies) without
 *   changing the time; {@code auto} follows the clock again;</li>
 *   <li>{@code status}: the town here, night or day, shopkeepers asleep, beds known, sleepers, owls, markets, boards;</li>
 *   <li>{@code winkels}: which shopkeepers of this town close at night and which stay (and why);</li>
 *   <li>{@code markt}: night + build the night market of this town now;</li>
 *   <li>{@code weer <bui|regenboog|gloed|mist|stop>}: show a weather moment of the zone you are in now (only for you);</li>
 *   <li>{@code knuffel}, {@code knuffel zet <stad> <n>}, {@code knuffel reset}: the knuffelmeter;</li>
 *   <li>{@code krant}: open the newspaper of this town (anywhere in it); {@code krant morgen}: pretend it is a day later (chores and
 *   news change, progress resets); {@code krant vandaag}: back.</li>
 * </ul>
 */
final class RitmeCommando {
    private static final SuggestionProvider<CommandSourceStack> STEDEN = (c, b) -> {
        List<String> l = new ArrayList<>();
        for (Steden.Stad s : Steden.ALLE) {
            l.add(s.id());
        }
        return SharedSuggestionProvider.suggest(l, b);
    };
    private static final SuggestionProvider<CommandSourceStack> WEER = (c, b) -> SharedSuggestionProvider.suggest(
            List.of("bui", "regenboog", "gloed", "mist", "stop"), b);

    private RitmeCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("ritme").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("nacht").executes(c -> forceer(c, Boolean.TRUE)));
        k.then(Commands.literal("dag").executes(c -> forceer(c, Boolean.FALSE)));
        k.then(Commands.literal("auto").executes(c -> forceer(c, null)));
        k.then(Commands.literal("status").executes(RitmeCommando::status));
        k.then(Commands.literal("winkels").executes(RitmeCommando::winkels));
        k.then(Commands.literal("markt").executes(RitmeCommando::markt));
        k.then(Commands.literal("weer").then(Commands.argument("wat", StringArgumentType.word()).suggests(WEER).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String wat = StringArgumentType.getString(c, "wat");
            PacketDistributor.sendToPlayer(p, new RitmePayloads.Forceer("weer:" + wat));
            c.getSource().sendSuccess(() -> Component.translatable(Ritme.P + "dev.weer", wat, Steden.zone(p.getX(), p.getY(), p.getZ())), false);
            return 1;
        })));
        k.then(Commands.literal("knuffel").executes(RitmeCommando::knuffel)
                .then(Commands.literal("reset").executes(c -> {
                    Knuffelmeter.reset(c.getSource().getPlayerOrException());
                    return knuffel(c);
                }))
                .then(Commands.literal("zet").then(Commands.argument("stad", StringArgumentType.word()).suggests(STEDEN)
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 9999)).executes(c -> {
                            String stad = StringArgumentType.getString(c, "stad");
                            if (Steden.van(stad) == null) {
                                c.getSource().sendFailure(Component.translatable(Ritme.P + "dev.geen_stad", stad));
                                return 0;
                            }
                            Knuffelmeter.zet(c.getSource().getPlayerOrException(), stad, IntegerArgumentType.getInteger(c, "n"));
                            return knuffel(c);
                        })))));
        k.then(Commands.literal("krant").executes(RitmeCommando::krant)
                .then(Commands.literal("morgen").executes(c -> {
                    Pluizenblaadje.dagVerschuiving++;
                    return krant(c);
                }))
                .then(Commands.literal("vandaag").executes(c -> {
                    Pluizenblaadje.dagVerschuiving = 0;
                    return krant(c);
                })));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static int forceer(CommandContext<CommandSourceStack> c, Boolean nacht) {
        Rooster.forceer = nacht;
        ServerLevel sl = c.getSource().getLevel();
        if (Boolean.FALSE.equals(nacht)) {
            Rooster.allesWakker(sl);
        }
        String wat = nacht == null ? "auto" : nacht ? "nacht" : "dag";
        for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(p, new RitmePayloads.Forceer(wat));
        }
        c.getSource().sendSuccess(() -> Component.translatable(Ritme.P + "dev." + wat), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel sl = c.getSource().getLevel();
        Steden.Stad s = Steden.op(p.getX(), p.getY(), p.getZ());
        Component stad = s == null ? Component.translatable(Ritme.P + "dev.geen") : Component.translatable(s.naamKey());
        boolean nacht = Rooster.nacht(sl);
        c.getSource().sendSuccess(() -> Component.translatable(Ritme.P + "dev.status", stad, Component.translatable(Ritme.P + (nacht ? "dev.is_nacht" : "dev.is_dag")),
                Ritme.tijd(sl), Rooster.forceer == null ? "auto" : String.valueOf(Rooster.forceer), Steden.zone(p.getX(), p.getY(), p.getZ()),
                Rooster.aantalSlapend(), Rooster.aantalBellen(), Bedden.bekend(), Nachtleven.aantalSlapers(), Nachtleven.aantalUilen(), Nachtmarkt.aantal(),
                Pluizenblaadje.aantalBorden()), false);
        return 1;
    }

    private static int winkels(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Steden.Stad s = Steden.op(p.getX(), p.getY(), p.getZ());
        int n = 0;
        for (NpcDef d : Npcs.alle()) {
            if (d.rol() == null || !Rooster.WINKELROLLEN.contains(d.rol()) || (s != null && Steden.op(d.plek()) != s)) {
                continue;
            }
            boolean dicht = Rooster.magSluiten(d);
            String waarom = dicht ? "sluit" : Rooster.questNpcs().contains(d.id()) ? "quest" : "blijft";
            Bedden.Bed bed = Rooster.bedVan(d.id());
            c.getSource().sendSuccess(() -> Component.translatable(Ritme.P + "dev.winkel." + waarom, Component.translatable(d.naamKey()), d.rol(),
                    bed == null ? "-" : bed.hoofd().toShortString()), false);
            n++;
        }
        int totaal = n;
        c.getSource().sendSuccess(() -> Component.translatable(Ritme.P + "dev.winkels", totaal), false);
        return n;
    }

    private static int markt(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Steden.Stad s = Steden.op(p.getX(), p.getY(), p.getZ());
        if (s == null || s.markt() == null) {
            c.getSource().sendFailure(Component.translatable(Ritme.P + "dev.geen_markt"));
            return 0;
        }
        forceer(c, Boolean.TRUE);
        boolean ok = Nachtmarkt.nu(c.getSource().getLevel(), s);
        c.getSource().sendSuccess(() -> Component.translatable(Ritme.P + (ok ? "dev.markt" : "dev.markt_mislukt"), Component.translatable(s.naamKey()),
                s.markt().get().toShortString()), false);
        return ok ? 1 : 0;
    }

    private static int knuffel(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        c.getSource().sendSuccess(() -> Component.translatable(Ritme.P + "dev.knuffel", Knuffelmeter.totaal(p)), false);
        for (Map.Entry<String, Integer> e : Knuffelmeter.standen(p).entrySet()) {
            Steden.Stad s = Steden.van(e.getKey());
            c.getSource().sendSuccess(() -> Component.literal("  ").append(Component.translatable(s.naamKey())).append(": " + e.getValue()), false);
        }
        return 1;
    }

    private static int krant(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Steden.Stad s = Steden.op(p.getX(), p.getY(), p.getZ());
        if (s == null || s.bord() == null) {
            c.getSource().sendFailure(Component.translatable(Ritme.P + "dev.geen_krant"));
            return 0;
        }
        Pluizenblaadje.open(p, c.getSource().getLevel(), s);
        return 1;
    }
}
