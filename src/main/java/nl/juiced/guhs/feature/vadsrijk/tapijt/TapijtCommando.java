package nl.juiced.guhs.feature.vadsrijk.tapijt;

import java.util.Locale;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalans;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk;

/**
 * U-TAPIJT dev command (op 2), {@code /guhs vadsrijk tapijt ...}:
 * {@code geef} (the zone-6 unlock), {@code rijd} (give + ride it right here), {@code tune} (all numbers), {@code tune <sleutel>
 * <waarde>} (live, saved in the balans file, sent to every player), {@code tune reset} (all defaults), {@code status} (speed,
 * height, hover goal of the carpet you ride), {@code looks} (every carpet look is yours), {@code reset} (forget the carpet).
 */
final class TapijtCommando {
    private TapijtCommando() {
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("tapijt").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        b.then(Commands.literal("geef").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return zeg(c, Tapijt.geef(p) ? "vliegend tapijt gegeven (met roze kussen en gouden kwastjes)" : "had het vliegend tapijt al");
        }));
        b.then(Commands.literal("rijd").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return Tapijt.rijd(p) != null ? zeg(c, "zweven maar! W = vaart maken, loslaten = uitglijden, S = remmen, A/D = opzij, spatie = hupje")
                    : zeg(c, "kon het tapijt hier niet roepen (alleen in het Vadsrijk, niet in een baasarena)");
        }));
        b.then(Commands.literal("tune").executes(TapijtCommando::lijst)
                .then(Commands.literal("reset").executes(c -> {
                    for (String k : TapijtBalans.SLEUTELS) {
                        KlasseBalans.zet(KlasseBalans.TAPIJT, k, TapijtBalans.standaard(k), false);
                    }
                    KlasseBalans.zet(KlasseBalans.TAPIJT, "topfactor", TapijtBalans.standaard("topfactor"), true);
                    return zeg(c, "alle tapijtgetallen terug naar standaard");
                }))
                .then(Commands.argument("sleutel", StringArgumentType.word())
                        .suggests((c, sb) -> SharedSuggestionProvider.suggest(TapijtBalans.SLEUTELS, sb))
                        .executes(c -> {
                            String k = StringArgumentType.getString(c, "sleutel");
                            return zeg(c, TapijtBalans.bestaat(k) ? k + " = " + getal(TapijtBalans.get(k)) + " (standaard " + getal(TapijtBalans.standaard(k)) + ")"
                                    : "onbekend: " + k + " (" + String.join(", ", TapijtBalans.SLEUTELS) + ")");
                        })
                        .then(Commands.argument("waarde", DoubleArgumentType.doubleArg()).executes(c -> {
                            String k = StringArgumentType.getString(c, "sleutel");
                            double v = DoubleArgumentType.getDouble(c, "waarde");
                            String fout = KlasseBalans.zet(KlasseBalans.TAPIJT, k, v, true);
                            if (fout != null) {
                                return zeg(c, fout);
                            }
                            return zeg(c, k + " = " + getal(TapijtBalans.get(k)) + (Math.abs(TapijtBalans.get(k) - v) > 1e-9 ? " (begrensd)" : "")
                                    + ": opgeslagen, binnen een seconde bij alle spelers");
                        }))));
        b.then(Commands.literal("status").executes(TapijtCommando::status));
        b.then(Commands.literal("looks").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Tapijt.geef(p);
            int n = 0;
            for (Uiterlijk.Variant v : Uiterlijk.alle()) {
                if (v.soort() == Tapijt.SOORT && Aankleden.geef(p, v, false)) {
                    n++;
                }
            }
            return zeg(c, n + " tapijtlooks erbij (N, Vliegend tapijt, Aankleden)");
        }));
        b.then(Commands.literal("reset").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            nl.juiced.guhs.feature.vadsrijk.npc.Questlog.zetVlag(p, Tapijt.VLAG, false);
            nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).extra("economie").getCompoundOrEmpty("rijdieren").remove(Tapijt.SOORT.id());
            return zeg(c, "vliegend tapijt vergeten (de looks in je garderobe blijven)");
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(b));
    }

    private static String getal(double v) {
        return String.format(Locale.ROOT, "%.4g", v);
    }

    private static int lijst(CommandContext<CommandSourceStack> c) {
        StringBuilder s = new StringBuilder("tapijt:");
        for (String k : TapijtBalans.SLEUTELS) {
            double v = TapijtBalans.get(k);
            s.append(' ').append(k).append('=').append(getal(v));
            if (Math.abs(v - TapijtBalans.standaard(k)) > 1e-9) {
                s.append('*');
            }
        }
        return zeg(c, s + "   (* = niet standaard; /guhs vadsrijk tapijt tune <sleutel> <waarde>)");
    }

    private static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        zeg(c, "tapijt: " + Tapijt.heeft(p) + ", questvlag: " + nl.juiced.guhs.feature.vadsrijk.npc.Questlog.vlag(p, Tapijt.VLAG)
                + (Tapijt.heeft(p) ? ", level " + Rijdieren.level(p, Tapijt.SOORT) : ""));
        if (p.getVehicle() instanceof TapijtEntity t) {
            double grond = t.grondVoetafdruk(t.getX(), t.getZ());
            zeg(c, String.format(Locale.ROOT, "rijdt: top %.3f b/t (%.1f blok/s), hoogte boven grond %.2f, zweefdoel %.2f, uitglijden vanaf top %.0f blokken",
                    t.top(), t.top() * 20, Double.isNaN(grond) ? Double.NaN : t.getY() - grond, t.doel(),
                    TapijtFysica.uitglijAfstand(t.top(), TapijtBalans.nu())));
        }
        return 1;
    }
}
