package nl.juiced.guhs.feature.ring;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * bbq2 (ring-kern): the op commands {@code /guhs ring ...} (dev checks and the AutoCheck script
 * tools/autocheck/bbq2_ring-kern.txt; the texts are plain literals: nobody but an op ever reads them).
 * <pre>
 * /guhs ring stand                 where the player is in the story, the ring, the rest point, Sam, Smikagol, the hunt
 * /guhs ring hoofdstuk &lt;0-7&gt;       sets the story: every chapter before n is done (0 = nothing started, 7 = all done)
 * /guhs ring wis                   forgets the whole story of the player
 * /guhs ring geef                  the ring and the three gifts
 * /guhs ring om | af               puts the ring on / takes it off
 * /guhs ring zwaar &lt;0-100&gt;         the ring's weight for a minute
 * /guhs ring negen | negen weg     starts / ends a hunt of the Nine;  /guhs ring patrouille: a rider on a round right here
 * /guhs ring sam | sam weg | sam draag      Sam-guh joins / leaves / carries
 * /guhs ring smikagol | smikagol weg | maatje      the guide comes / leaves; the buddy (once per player)
 * /guhs ring rustpunt | terug      this spot is the rest point / back to the rest point
 * /guhs ring beloning | feest      the end rewards (needs the story done) / the daily treat
 * /guhs ring cast &lt;kind&gt;           a real cast character on this spot
 * </pre>
 */
final class RingCommands {
    static void register(RegisterCommandsEvent event) {
        var ring = Commands.literal("ring").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        ring.then(Commands.literal("stand").executes(c -> stand(c)));
        ring.then(Commands.literal("hoofdstuk").then(Commands.argument("n", IntegerArgumentType.integer(0, 7)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = IntegerArgumentType.getInteger(c, "n");
            if (n == 0) {
                Ring.wis(p);
            } else {
                Ring.lijn(1).begin(p);
                for (int i = 1; i < n && i <= 6; i++) {
                    Verhaallijn l = Ring.lijn(i);
                    l.zet(p, l.stappen());
                }
            }
            return zeg(c, "story: chapter " + Ring.hoofdstuk(p));
        })));
        ring.then(Commands.literal("wis").executes(c -> {
            Ring.wis(c.getSource().getPlayerOrException());
            return zeg(c, "the story of the Knabbelring is forgotten");
        }));
        ring.then(Commands.literal("geef").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Ring.geef(p);
            Gaven.geef(p);
            return zeg(c, "the ring and the three gifts");
        }));
        ring.then(Commands.literal("om").executes(c -> zeg(c, "ring on: " + Ring.doeOm(c.getSource().getPlayerOrException(), true))));
        ring.then(Commands.literal("af").executes(c -> zeg(c, "ring on: " + Ring.doeOm(c.getSource().getPlayerOrException(), false))));
        ring.then(Commands.literal("zwaar").then(Commands.argument("procent", IntegerArgumentType.integer(0, 100)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Ring.zetZwaarte(p, IntegerArgumentType.getInteger(c, "procent") / 100.0, 1200);
            return zeg(c, "weight of the ring: " + Ring.zwaarte(p));
        })));
        ring.then(Commands.literal("negen").executes(c -> zeg(c, "riders: " + Negen.jaag(c.getSource().getPlayerOrException())))
                .then(Commands.literal("weg").executes(c -> {
                    Negen.einde(c.getSource().getPlayerOrException());
                    return zeg(c, "the hunt is over");
                })));
        ring.then(Commands.literal("patrouille").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            BlockPos hier = p.blockPosition();
            Negen.patrouille(p.level(), List.of(hier.east(3), hier.east(11), hier.east(11).south(6), hier.east(3).south(6)));
            return zeg(c, "a rider rides a round east of here");
        }));
        ring.then(Commands.literal("sam").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Sam.roep(p);
            return zeg(c, "Sam-guh: " + (Sam.van(p) != null ? "here" : "not here (not on the trip, or not a world of the story)"));
        }).then(Commands.literal("weg").executes(c -> {
            Sam.stuurWeg(c.getSource().getPlayerOrException());
            return zeg(c, "Sam-guh went home");
        })).then(Commands.literal("draag").executes(c -> zeg(c, "carried: " + Sam.draag(c.getSource().getPlayerOrException())))));
        ring.then(Commands.literal("smikagol").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return zeg(c, "Smikagol: " + (Smikagol.roep(p, null) != null ? "here" : "could not come"));
        }).then(Commands.literal("weg").executes(c -> {
            Smikagol.stuurWeg(c.getSource().getPlayerOrException());
            return zeg(c, "Smikagol left");
        })));
        ring.then(Commands.literal("maatje").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return zeg(c, Smikagol.maakMaatje(p) != null ? "your own Smikagol" : "called: " + Smikagol.roepMaatje(p));
        }));
        ring.then(Commands.literal("rustpunt").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Ring.rustpunt(p, p.position(), p.getYRot());
            return zeg(c, "this is the rest point");
        }));
        ring.then(Commands.literal("terug").executes(c -> zeg(c, "back: " + Ring.terugNaarRustpunt(c.getSource().getPlayerOrException(), Ring.NEGEN, null))));
        ring.then(Commands.literal("beloning").executes(c -> zeg(c, "rewards given: " + RingBeloning.geef(c.getSource().getPlayerOrException()))));
        ring.then(Commands.literal("feest").executes(c -> zeg(c, "treat given: " + RingBeloning.feest(c.getSource().getPlayerOrException(), null))));
        ring.then(Commands.literal("cast").then(Commands.argument("kind", StringArgumentType.word()).suggests((c, b) -> {
            Cast.KINDS.forEach(k -> b.suggest(k.id()));
            return b.buildFuture();
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String naam = StringArgumentType.getString(c, "kind").toLowerCase(Locale.ROOT);
            for (GuhNpcEntity.Kind kind : Cast.KINDS) {
                if (kind.id().equals(naam)) {
                    Cast.zet(p.level(), kind, p.position(), p.getYRot() + 180f, null);
                    return zeg(c, kind.id() + " stands here");
                }
            }
            return zeg(c, "no such cast kind: " + naam);
        })));
        event.getDispatcher().register(Commands.literal("guhs").then(ring));
    }

    private static int stand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Rustpunten.Punt punt = Ring.rustpunt(p);
        StringBuilder s = new StringBuilder("chapter " + Ring.hoofdstuk(p) + " (");
        for (int i = 1; i <= 7; i++) {
            Verhaallijn l = Ring.lijn(i);
            s.append(i == 7 ? "S" : i).append(":").append(l.stap(p)).append("/").append(l.stappen()).append(i < 7 ? " " : "");
        }
        s.append("), ring ").append(Ring.heeft(p) ? Ring.om(p) ? "ON" : "carried" : "none").append(", weight ").append(String.format(Locale.ROOT, "%.2f", Ring.zwaarte(p)));
        s.append(", rest point ").append(punt == null ? "none" : BlockPos.containing(punt.plek()).toShortString() + " " + punt.dim().identifier().getPath());
        s.append(" (").append(Rustpunt.aantal(p)).append(" fires), Sam ").append(Sam.van(p) != null ? Sam.doet(Sam.van(p)).isEmpty() ? "follows" : Sam.doet(Sam.van(p)) : "-");
        s.append(", Smikagol ").append(Smikagol.van(p) != null ? "guide" : "-").append(Smikagol.heeftMaatje(p) ? " +buddy" : "");
        s.append(", hunt ").append(Negen.wordtGejaagd(p) ? Negen.ruiters(p).size() + " riders" : "-").append(", rock ").append(Gaven.isRots(p));
        s.append(", rewards ").append(RingBeloning.gegeven(p));
        return zeg(c, s.toString());
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal("[ring] " + tekst), false);
        return 1;
    }

    private RingCommands() {
    }
}
