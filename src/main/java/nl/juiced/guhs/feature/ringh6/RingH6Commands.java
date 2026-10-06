package nl.juiced.guhs.feature.ringh6;

import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Smikagol;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * bbq2 (ring-h6): the op commands {@code /guhs ringh6 ...} (dev checks and the AutoCheck script
 * tools/autocheck/bbq2_ring-h6.txt; plain literal texts: nobody but an op ever reads them).
 * <pre>
 * /guhs ringh6 stand              where the player is in the chapter, the mountain they are on, the ring's weight
 * /guhs ringh6 stap &lt;0-7&gt;        the story up to chapter 5 is done and this chapter stands at that step (7 = done)
 * /guhs ringh6 ga &lt;plek&gt;         to a named spot of the mountain the player is on (kamp, richel_1, start_2, spleet, rand ...)
 * /guhs ringh6 kool               a coal over the player's head
 * /guhs ringh6 graai              Smikagol grabs now
 * /guhs ringh6 thuis              where the flight home would put this player down
 * /guhs ringh6 bouw               (dev runs only: it builds) a try-out Frituurberg with its base camp where the player stands
 * /guhs ringh6 testberg           (dev runs only) the small test mountain's spots count from the block under the player
 * </pre>
 * The scenes: {@code /guhs verhaal scene ringh6_frituur|ringh6_vlucht|ringh6_feest}, the card: {@code /guhs verhaal kaart ring_h6}.
 */
final class RingH6Commands {
    static void register(RegisterCommandsEvent event) {
        var cmd = Commands.literal("ringh6").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        cmd.then(Commands.literal("stand").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Berg.Kopie berg = Berg.zoek(p);
            Verhaallijn lijn = RingH6Feature.LIJN;
            return zeg(c, "step " + lijn.stap(p) + "/" + lijn.stappen() + (lijn.aanDeBeurt(p) ? "" : " (chapter 5 is not done)") + ", fried " + lijn.vlag(p, Finale.GEFRITUURD)
                    + ", mountain " + (berg == null ? "none here" : "anchor " + berg.anker().toShortString() + " turned " + berg.draai()
                    + ", height " + String.format(Locale.ROOT, "%.2f", berg.hoogte(p.getY())))
                    + ", ring weight " + String.format(Locale.ROOT, "%.2f", Ring.zwaarte(p)) + ", coals hit " + Kolen.geraaktAantal(p)
                    + ", Smikagol " + (Smikagol.van(p) == null ? "-" : Klim.verblind(p) ? "blinded" : "lurking") + ", home " + Thuis.thuis(p));
        }));
        cmd.then(Commands.literal("stap").then(Commands.argument("n", IntegerArgumentType.integer(0, 7)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = IntegerArgumentType.getInteger(c, "n");
            Ring.lijn(1).begin(p);
            for (int i = 1; i <= 5; i++) {
                Ring.lijn(i).zet(p, Ring.lijn(i).stappen());
            }
            Verhaallijn lijn = RingH6Feature.LIJN;
            if (n < lijn.stap(p)) {
                lijn.wis(p);
            }
            lijn.begin(p);
            lijn.zet(p, n);
            if (!Verteller.gezien(p, Klim.KAART) && n > 0) {
                Verteller.toon(p, Klim.KAART, null);
            }
            return zeg(c, "chapter 6 stands at step " + lijn.stap(p));
        })));
        cmd.then(Commands.literal("ga").then(Commands.argument("plek", StringArgumentType.word()).suggests((c, b) -> {
            Berg.echt().plekken().keySet().stream().sorted().forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Berg.Kopie berg = Berg.zoek(p);
            String plek = StringArgumentType.getString(c, "plek");
            if (berg == null || !berg.g().plekken().containsKey(plek)) {
                return zeg(c, berg == null ? "you are not on a Frituurberg" : "no such spot: " + plek);
            }
            BlockPos daar = berg.wereld(plek);
            Duwtje.terug(p, p.level().dimension(), Vec3.atBottomCenterOf(daar), p.getYRot());
            return zeg(c, plek + " = " + daar.toShortString());
        })));
        cmd.then(Commands.literal("kool").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return zeg(c, Kolen.laatVallen(p.level(), p.position(), p) != null ? "a coal comes down" : "no room above you for a coal");
        }));
        cmd.then(Commands.literal("graai").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (Smikagol.van(p) == null) {
                Smikagol.roep(p, null);
            }
            Klim.zetGraaiNu(p, true);
            return zeg(c, "Smikagol grabs");
        }));
        cmd.then(Commands.literal("thuis").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            BlockPos plek = Thuis.plek(p);
            return zeg(c, "home: " + (plek == null ? "no Guhmensie here" : plek.toShortString()) + ", saved: " + Thuis.thuis(p));
        }));
        if (!FMLEnvironment.isProduction()) {
            cmd.then(Commands.literal("bouw").executes(c -> {
                // a try-out mountain (the very tiles worldgen places, not turned) with its base camp where the player stands
                ServerPlayer p = c.getSource().getPlayerOrException();
                Berg.Gegevens g = Berg.echt();
                BlockPos hier = p.blockPosition();
                nl.juiced.guhs.feature.paleizen.PaleisProef.bouw(p.level(), Berg.STRUCTUUR, g.plek("kamp"), hier, true);
                Berg.wisTest(p.level());
                Berg.Kopie berg = Berg.zetTest(p.level(), g, hier.subtract(g.plek("kamp")));
                return zeg(c, "a try-out Frituurberg, anchor " + berg.anker().toShortString() + "; the camp is where you stand");
            }));
            cmd.then(Commands.literal("testberg").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Berg.wisTest(p.level());
                Berg.Kopie berg = Berg.zetTest(p.level(), Berg.test(), p.blockPosition().below(2));
                return zeg(c, "the test mountain's spots count from " + berg.anker().toShortString() + " (place template guhs:ringh6_test_berg there)");
            }));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(cmd));
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal("[ringh6] " + tekst), false);
        return 1;
    }

    private RingH6Commands() {
    }
}
