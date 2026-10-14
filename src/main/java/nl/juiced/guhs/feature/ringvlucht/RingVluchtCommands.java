package nl.juiced.guhs.feature.ringvlucht;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 1.4.2 (rookguh), op commands for dev checks and AutoCheck scripts (literal English, like the other dev commands):
 * <pre>
 * /guhs ringvlucht lijst              the places you may fly to (the next one first) and which stops you reached
 * /guhs ringvlucht waar &lt;stop&gt;       where that stop is for you and where the Rookguh would put you down (loads chunks)
 * /guhs ringvlucht vlieg &lt;stop&gt;      fly there (only to a stop you reached), as the Rookguh's screen does
 * /guhs ringvlucht rookguh &lt;stop&gt;    a Reis-Rookguh of that stop at your feet (not saved by Bezetting; for looking at)
 * </pre>
 */
final class RingVluchtCommands {
    static void register(RegisterCommandsEvent event) {
        var cmd = Commands.literal("ringvlucht").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        cmd.then(Commands.literal("lijst").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            StringBuilder b = new StringBuilder("to: ");
            for (Haltes.Halte h : Vlucht.bestemmingen(p, null)) {
                b.append(h.id()).append(h == Vlucht.volgende(p) ? " (next)" : "").append(", ");
            }
            b.append(" | stops: ");
            for (Haltes.Halte h : Haltes.ALLE) {
                b.append(h.id()).append(Haltes.bereikt(p, h) ? "+" : "-").append(Haltes.klaar(p, h) ? "R " : " ");
            }
            return zeg(c, b.toString());
        }));
        cmd.then(Commands.literal("waar").then(Commands.argument("halte", StringArgumentType.word()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Haltes.Halte h = Haltes.van(StringArgumentType.getString(c, "halte"));
            if (h == null) {
                return zeg(c, "no such stop");
            }
            Vlucht.Doelplek d = Vlucht.doelplek(p, h);
            if (d == null) {
                return zeg(c, "not known (yet)");
            }
            BlockPos plek = Landing.zoek(d.level(), d.ingangen(), d.afstand(), d.doos(), pos -> Vlucht.magLanden(p, d.level(), pos));
            List<String> ingangen = d.ingangen().stream().map(i -> i.plek().toShortString() + " out " + i.dx() + "," + i.dz()).toList();
            return zeg(c, h.id() + " in " + d.level().dimension().identifier() + ": entrances " + ingangen + ", box " + d.doos() + ", lands at "
                    + (plek == null ? "NOWHERE" : plek.toShortString()));
        })));
        cmd.then(Commands.literal("vlieg").then(Commands.argument("halte", StringArgumentType.word()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Haltes.Halte h = Haltes.van(StringArgumentType.getString(c, "halte"));
            if (h == null || !Haltes.bereikt(p, h)) {
                return zeg(c, "not a stop you reached");
            }
            return zeg(c, Vlucht.vlieg(p, h, null) ? "flying" : "not flying");
        })));
        cmd.then(Commands.literal("rookguh").then(Commands.argument("halte", StringArgumentType.word()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Haltes.Halte h = Haltes.van(StringArgumentType.getString(c, "halte"));
            if (h == null) {
                return zeg(c, "no such stop");
            }
            Vec3 plek = p.position().add(p.getLookAngle().multiply(2, 0, 2)).add(0, 1, 0);
            VluchtRookguhEntity r = VluchtRookguhEntity.maak(p.level(), plek, h, p.getYRot() + 180);
            if (r != null) {
                p.level().addFreshEntity(r);
            }
            return zeg(c, r == null ? "could not make one" : "a Rookguh of " + h.id());
        })));
        event.getDispatcher().register(Commands.literal("guhs").then(cmd));
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal("[ringvlucht] " + tekst), false);
        return 1;
    }

    private RingVluchtCommands() {
    }
}
