package nl.juiced.guhs.feature.vadsrijk.zone4.boot;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * T-Z4BOOT dev command (op 2), {@code /guhs vadsrijk boot ...}:
 * {@code geef} (the story reward), {@code fles} (the "Bootje in een fles" item), {@code varen} (give + call it on the nearest water,
 * anywhere: turns on proefvaart), {@code proefvaart aan|uit} (sail outside zone 4), {@code steigers} (list + distance),
 * {@code naar <steiger>} (teleport onto a dock), {@code waar} (current / wind / dock at your spot), {@code variant <id>} (unlock +
 * choose), {@code varianten} (all looks), {@code scherm}, {@code status}, {@code thuis} (pretend you logged out at sea: back to the
 * last dock), {@code reset}.
 */
final class BootCommando {
    private BootCommando() {
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("boot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        b.then(Commands.literal("geef").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return zeg(c, Boot.geef(p) ? "zeilbootje gegeven (questvlag gezet)" : "had het zeilbootje al (questvlag gezet)");
        }));
        b.then(Commands.literal("fles").executes(c -> {
            c.getSource().getPlayerOrException().getInventory().add(new ItemStack(BootSpul.ICOON.get()));
            return zeg(c, "Bootje in een fles in je tas");
        }));
        b.then(Commands.literal("varen").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Boot.geef(p);
            Boot.PROEF.add(p.getUUID());
            if (p.isPassenger()) {
                p.stopRiding();
            }
            return Rijdieren.roep(p, RijdierSoort.ZEILBOOTJE) != null ? zeg(c, "varen maar! (proefvaart aan: ook buiten zone 4)") : 0;
        }));
        b.then(Commands.literal("proefvaart").then(Commands.literal("aan").executes(c -> {
            Boot.PROEF.add(c.getSource().getPlayerOrException().getUUID());
            return zeg(c, "proefvaart aan: je bootje vaart overal op water");
        })).then(Commands.literal("uit").executes(c -> {
            Boot.PROEF.remove(c.getSource().getPlayerOrException().getUUID());
            return zeg(c, "proefvaart uit: alleen de zee van zone 4");
        })));
        b.then(Commands.literal("steigers").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (BootZee.Steiger s : BootZee.steigers()) {
                BlockPos q = s.plek();
                double d = Math.sqrt(p.distanceToSqr(q.getX() + 0.5, q.getY(), q.getZ() + 0.5));
                zeg(c, String.format(Locale.ROOT, "%s%s  %d %d %d  (%.0f blokken)%s", s.id(), s.equals(Boot.laatsteSteiger(p)) ? " *" : "", q.getX(), q.getY(),
                        q.getZ(), d, s.placeholder() ? "  [placeholder]" : ""));
            }
            return 1;
        }));
        b.then(Commands.literal("naar").then(Commands.argument("steiger", StringArgumentType.word())
                .suggests((c, sb) -> SharedSuggestionProvider.suggest(BootZee.steigers().stream().map(BootZee.Steiger::id), sb)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BootZee.Steiger s = BootZee.steiger(StringArgumentType.getString(c, "steiger"));
                    if (s == null) {
                        c.getSource().sendFailure(Component.literal("onbekende steiger"));
                        return 0;
                    }
                    if (p.isPassenger()) {
                        p.stopRiding();
                    }
                    p.teleportTo(p.level(), s.plek().getX() + 0.5, s.plek().getY(), s.plek().getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
                    return zeg(c, "naar steiger " + s.id());
                })));
        b.then(Commands.literal("waar").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            BootFysica.Kringstroom k = BootZee.kring();
            long t = p.level().getGameTime();
            BootZee.Steiger s = BootZee.dichtbij(p.getX(), p.getZ(), BootZee.AANLEG_AFSTAND);
            return zeg(c, String.format(Locale.ROOT,
                    "Kringstroom: %.2f van de ellips (band vanaf %.2f, diepte %.2f), zee zone 4: %s | wind naar %.0f graden, mee %.2f (x%.2f) | steiger: %s",
                    k.r(p.getX(), p.getZ()), k.binnen(), k.diepte(p.getX(), p.getZ()), BootZee.opZee(p.getX(), p.getZ()), BootFysica.windRichting(t),
                    BootFysica.windMee(p.getYRot(), t), BootFysica.windFactor(p.getYRot(), t), s == null ? "geen" : s.id()));
        }));
        b.then(Commands.literal("variant").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, sb) -> SharedSuggestionProvider.suggest(Arrays.stream(BootVariant.values()).map(BootVariant::id), sb)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BootVariant v = BootVariant.van(StringArgumentType.getString(c, "id"));
                    if (v == null) {
                        c.getSource().sendFailure(Component.literal("onbekende variant"));
                        return 0;
                    }
                    Boot.geefVariant(p, v);
                    Boot.kies(p, v);
                    return zeg(c, "gekozen: " + v.id());
                })));
        b.then(Commands.literal("varianten").executes(c -> {
            Boot.alleVarianten(c.getSource().getPlayerOrException());
            return zeg(c, "alle zeilen, wimpels en boegbeelden");
        }));
        b.then(Commands.literal("scherm").executes(c -> {
            Boot.scherm(c.getSource().getPlayerOrException(), "");
            return 1;
        }));
        b.then(Commands.literal("status").executes(BootCommando::status));
        b.then(Commands.literal("thuis").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (p.isPassenger()) {
                p.stopRiding();
            }
            Boot.tag(p).putBoolean("opzee", true);
            return Boot.ingelogd(p) ? zeg(c, "alsof je op zee uitlogde: terug op " + Boot.laatsteSteiger(p).id()) : zeg(c, "(alleen in het Vadsrijk)");
        }));
        b.then(Commands.literal("reset").executes(c -> {
            Boot.reset(c.getSource().getPlayerOrException());
            return zeg(c, "zeilbootje en alles eromheen gewist");
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(b));
    }

    private static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        zeg(c, "boot: " + Boot.heeft(p) + ", questvlag: " + Boot.gekregen(p) + ", op zee: " + Boot.opZee(p) + ", laatste steiger: " + Boot.laatsteSteiger(p).id()
                + ", proefvaart: " + Boot.PROEF.contains(p.getUUID()));
        if (Boot.heeft(p)) {
            zeg(c, "level " + Rijdieren.level(p, RijdierSoort.ZEILBOOTJE) + ", xp " + Rijdieren.xp(p, RijdierSoort.ZEILBOOTJE) + ", zeil "
                    + Boot.gekozen(p, BootVariant.As.ZEIL).kort + ", wimpel " + Boot.gekozen(p, BootVariant.As.WIMPEL).kort + ", boeg "
                    + Boot.gekozen(p, BootVariant.As.BOEG).kort);
        }
        return 1;
    }
}
