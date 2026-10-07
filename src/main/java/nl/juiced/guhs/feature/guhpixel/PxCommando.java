package nl.juiced.guhs.feature.guhpixel;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The commands of Guhpixel.
 * <p>
 * For everybody: {@code /lobby}, {@code /hub} and {@code /l}. Not unlocked: a funny refusal that points to the
 * Guh-internetcafé; in a game: leave it, to the lobby; in the lobby: back home; elsewhere: to the lobby.
 * <p>
 * For gamemasters (and the server console, and the AutoCheck scripts), under {@code /guhs px}:
 * {@code lobby} (unlock + enter) · {@code huis} · {@code ontgrendel <aan|uit>} · {@code muntjes <zet|geef> <n>} ·
 * {@code muntjes wis} · {@code dagpot reset} · {@code klok <uren>} (skip forward; decimals allowed) ·
 * {@code klok herstel|toon} · {@code speel <soortId> [opties-snbt]} · {@code stop} · {@code grap <id> <stap n|klaar|reset>} ·
 * {@code film <id|alles|wis>} · {@code kluis <toon|herstel>} · {@code arena <lijst|ruim>} · {@code herbouw} (the lobby) ·
 * {@code winkel} · {@code zelftest}. A slice adds its own under {@code /guhs px <slice> ...} by registering the same
 * literals (Brigadier merges them).
 */
public final class PxCommando {
    static void register(RegisterCommandsEvent event) {
        for (String naam : List.of("lobby", "hub", "l")) {
            event.getDispatcher().register(Commands.literal(naam).executes(ctx -> lobby(ctx.getSource())));
        }
        LiteralArgumentBuilder<CommandSourceStack> px = Commands.literal("px").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        px.then(Commands.literal("lobby").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Toegang.ontgrendel(p);
            return Toegang.naarLobby(p) ? 1 : 0;
        }));
        px.then(Commands.literal("huis").executes(ctx -> Toegang.naarHuis(ctx.getSource().getPlayerOrException()) ? 1 : 0));
        px.then(Commands.literal("ontgrendel")
                .then(Commands.literal("aan").executes(ctx -> {
                    Toegang.ontgrendel(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("uit").executes(ctx -> {
                    Toegang.vergrendel(ctx.getSource().getPlayerOrException());
                    return 1;
                })));
        px.then(Commands.literal("muntjes")
                .then(Commands.literal("zet").then(Commands.argument("n", IntegerArgumentType.integer(0, 1_000_000)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Muntjes.zet(p, IntegerArgumentType.getInteger(ctx, "n"));
                    return zeg(ctx.getSource(), "saldo " + Muntjes.saldo(p) + ", totaal " + Muntjes.totaal(p) + ", rang " + Muntjes.rang(p));
                })))
                .then(Commands.literal("geef").then(Commands.argument("n", IntegerArgumentType.integer(1, 1_000_000)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Muntjes.geef(p, IntegerArgumentType.getInteger(ctx, "n"));
                    return zeg(ctx.getSource(), "saldo " + Muntjes.saldo(p) + ", totaal " + Muntjes.totaal(p) + ", rang " + Muntjes.rang(p));
                })))
                .then(Commands.literal("wis").executes(ctx -> {
                    Muntjes.wis(ctx.getSource().getPlayerOrException());
                    return zeg(ctx.getSource(), "muntjes gewist");
                })));
        px.then(Commands.literal("dagpot").then(Commands.literal("reset").executes(ctx -> {
            Muntjes.dagpotReset(ctx.getSource().getPlayerOrException());
            return zeg(ctx.getSource(), "dagpotten leeg");
        })));
        px.then(Commands.literal("klok")
                .then(Commands.literal("herstel").executes(ctx -> {
                    Klok.herstel();
                    return zeg(ctx.getSource(), klok());
                }))
                .then(Commands.literal("toon").executes(ctx -> zeg(ctx.getSource(), klok())))
                .then(Commands.argument("uren", DoubleArgumentType.doubleArg(-100000, 100000)).executes(ctx -> {
                    Klok.spoel(Math.round(DoubleArgumentType.getDouble(ctx, "uren") * Klok.UUR));
                    return zeg(ctx.getSource(), klok());
                })));
        px.then(Commands.literal("speel").then(Commands.argument("soort", StringArgumentType.word())
                .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Sessies.soorten().stream().map(SpelSoort::id), b))
                .executes(ctx -> speel(ctx.getSource(), StringArgumentType.getString(ctx, "soort"), new CompoundTag()))
                .then(Commands.argument("opties", CompoundTagArgument.compoundTag())
                        .executes(ctx -> speel(ctx.getSource(), StringArgumentType.getString(ctx, "soort"), CompoundTagArgument.getCompoundTag(ctx, "opties"))))));
        px.then(Commands.literal("stop").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Sessie s = Sessies.van(p);
            if (s == null) {
                ctx.getSource().sendFailure(Component.literal("geen spel bezig"));
                return 0;
            }
            s.stop();
            return 1;
        }));
        px.then(Commands.literal("grap").then(Commands.argument("id", StringArgumentType.word())
                .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Grappen.alle().stream().map(Grap::id), b))
                .then(Commands.literal("stap").then(Commands.argument("n", IntegerArgumentType.integer(0, 99)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(ctx, "id");
                    Grappen.zetStap(p, id, IntegerArgumentType.getInteger(ctx, "n"));
                    return zeg(ctx.getSource(), id + ": stap " + Grappen.stap(p, id));
                })))
                .then(Commands.literal("klaar").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(ctx, "id");
                    boolean eerste = Grappen.voltooi(p, id);
                    return zeg(ctx.getSource(), id + ": klaar (" + (eerste ? "eerste keer" : "opnieuw") + "), " + Grappen.keren(p, id) + " keer");
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    Grappen.reset(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"));
                    return zeg(ctx.getSource(), "voortgang gewist");
                }))));
        px.then(Commands.literal("film").then(Commands.argument("id", StringArgumentType.word())
                .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        java.util.stream.Stream.concat(Films.IDS.stream(), java.util.stream.Stream.of("alles", "wis")), b))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(ctx, "id");
                    if (id.equals("wis")) {
                        Films.wis(p);
                    } else if (id.equals("alles")) {
                        Films.IDS.forEach(f -> Films.ontgrendel(p, f));
                    } else if (Films.IDS.contains(id)) {
                        Films.ontgrendel(p, id);
                    } else {
                        ctx.getSource().sendFailure(Component.literal("onbekende film: " + id));
                        return 0;
                    }
                    return zeg(ctx.getSource(), "films: " + Films.aantal(p) + " / " + Films.IDS.size());
                })));
        px.then(Commands.literal("kluis")
                .then(Commands.literal("toon").executes(ctx -> zeg(ctx.getSource(), Kluis.toon(ctx.getSource().getPlayerOrException()))))
                .then(Commands.literal("herstel").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Sessies.verlaat(p, Vertrek.VERLATEN);
                    return zeg(ctx.getSource(), Kluis.herstel(p) ? "kluis hersteld" : "geen kluis");
                })));
        px.then(Commands.literal("arena")
                .then(Commands.literal("lijst").executes(ctx -> {
                    List<String> regels = Arenas.lijst(ctx.getSource().getServer());
                    zeg(ctx.getSource(), regels.size() + " cellen, " + Sessies.alle().size() + " spellen bezig");
                    regels.forEach(r -> zeg(ctx.getSource(), r));
                    return regels.size();
                }))
                .then(Commands.literal("ruim").executes(ctx -> zeg(ctx.getSource(), Arenas.ruim(ctx.getSource().getServer()) + " vrije cellen afgeschreven"))));
        px.then(Commands.literal("herbouw").executes(ctx -> {
            ServerLevel level = Guhpixel.level(ctx.getSource().getServer());
            if (level == null) {
                ctx.getSource().sendFailure(Component.literal("de dimensie guhs:guhpixel bestaat niet"));
                return 0;
            }
            Lobby.herbouw(level);
            return zeg(ctx.getSource(), "de lobby wordt over een paar tellen opnieuw gestempeld");
        }));
        px.then(Commands.literal("winkel").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Winkel.open(p);
            return 1;
        }));
        px.then(Commands.literal("gids").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            ModNetworking.sendTo(p, new GuhpixelPayloads.Gids(GidsBlad.stand(p)));
            return zeg(ctx.getSource(), GidsBlad.stand(p).getListOrEmpty("Rijen").size() + " rijen in het Guhdex-blad");
        }));
        px.then(Commands.literal("zelftest").executes(ctx -> {
            List<String> regels = PxZelftest.draai(ctx.getSource().getServer());
            regels.forEach(r -> zeg(ctx.getSource(), r));
            return regels.get(regels.size() - 1).contains(" 0 FOUT") ? 1 : 0;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(px));
    }

    /** /lobby, /hub, /l. */
    private static int lobby(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (!Toegang.heeft(p)) {
            int grap = 1 + p.getRandom().nextInt(4);
            p.sendSystemMessage(Component.translatable("commands.guhs.guhpixel.lobby.op_slot." + grap).withStyle(ChatFormatting.LIGHT_PURPLE));
            p.sendSystemMessage(Component.translatable("commands.guhs.guhpixel.lobby.op_slot").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        if (Sessies.van(p) != null) {
            Sessies.verlaat(p, Vertrek.VERLATEN);
            if (Guhpixel.echt(p.level()) && !Guhpixel.inLobby(p)) {
                return Toegang.naarLobby(p) ? 1 : 0;
            }
            return 1;
        }
        if (Guhpixel.inLobby(p)) {
            return Toegang.naarHuis(p) ? 1 : 0;
        }
        return Toegang.naarLobby(p) ? 1 : 0;
    }

    private static int speel(CommandSourceStack source, String soortId, CompoundTag opties) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        SpelSoort soort = Sessies.soort(soortId);
        if (soort == null) {
            source.sendFailure(Component.literal("onbekend spel: " + soortId + " (bekend: "
                    + String.join(", ", Sessies.soorten().stream().map(SpelSoort::id).toList()) + ")"));
            return 0;
        }
        Toegang.ontgrendel(p);
        if (!Guhpixel.in(p) && !Toegang.naarLobby(p)) {
            return 0;
        }
        return Sessies.start(soort, List.of(p), opties) != null ? 1 : 0;
    }

    private static String klok() {
        return String.format(Locale.ROOT, "klok: dag %d, %.2f uur vooruit gespoeld, nog %.2f uur tot morgen", Klok.dag(),
                Klok.offset() / (double) Klok.UUR, Klok.totMorgen() / (double) Klok.UUR);
    }

    private static int zeg(CommandSourceStack source, String tekst) {
        source.sendSuccess(() -> Component.literal(tekst), false);
        return 1;
    }

    private PxCommando() {
    }
}
