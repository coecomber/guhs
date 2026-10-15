package nl.juiced.guhs.feature.ringh3;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * bbq2 (ring-h3): the op commands {@code /guhs ringh3 ...} (dev checks and the AutoCheck script
 * tools/autocheck/bbq2_ring-h3.txt; the texts are plain literals: nobody but an op ever reads them).
 * <pre>
 * /guhs ringh3 stand            the mine around you, where you are in it, your step, your chase, your bridge
 * /guhs ringh3 stap &lt;0-7&gt;       your story: chapters 1 and 2 done, ring_h3 at this step (0 also forgets it first)
 * /guhs ringh3 ga &lt;plek&gt;        to a spot of the mine around you (poort, hefbomen, put, gang, hal, rand, brug, oever, oost)
 * /guhs ringh3 deur &lt;naam&gt;      opens a door (west, valhek, geheim, oost)
 * /guhs ringh3 rog [weg]        wakes your Barbecuerog where the chase begins / sends him away
 * /guhs ringh3 brug kapot|heel  the bridge as you see it
 * /guhs ringh3 scene emmer|brug plays a scene anchored in the mine around you (no step is taken)
 * /guhs ringh3 bouw             (dev runs only) builds a copy of the mine so that you stand on its west forecourt
 * </pre>
 */
final class RingH3Commands {
    private static final Map<String, BlockPos> PLEKKEN = new LinkedHashMap<>();

    static {
        PLEKKEN.put("plein", new BlockPos(28, Plekken.BOVEN, 62));
        PLEKKEN.put("poort", Plekken.POORT_BUITEN);
        PLEKKEN.put("hefbomen", new BlockPos(42, Plekken.MIDDEL, 38));
        PLEKKEN.put("put", new BlockPos(26, Plekken.MIDDEL, 35));
        PLEKKEN.put("gang", new BlockPos(16, Plekken.MIDDEL, 35));
        PLEKKEN.put("hal", Plekken.HAL_INGANG.north(2));
        PLEKKEN.put("rand", new BlockPos(44, Plekken.DIEP, 14));
        PLEKKEN.put("brug", Plekken.BRUG_RAND_WEST.west(1));
        PLEKKEN.put("oever", new BlockPos(79, Plekken.DIEP, 14));
        PLEKKEN.put("oost", new BlockPos(66, Plekken.BOVEN, 62));
    }

    private RingH3Commands() {
    }

    static void register(RegisterCommandsEvent event) {
        var h3 = Commands.literal("ringh3").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        h3.then(Commands.literal("stand").executes(RingH3Commands::stand));
        h3.then(Commands.literal("stap").then(Commands.argument("n", IntegerArgumentType.integer(0, 7)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = IntegerArgumentType.getInteger(c, "n");
            Ring.lijn(1).begin(p);
            for (int i = 1; i <= 2; i++) {
                Verhaallijn l = Ring.lijn(i);
                l.zet(p, l.stappen());
            }
            Verhaallijn lijn = RingH3Feature.LIJN;
            if (n < lijn.stap(p)) {
                lijn.wis(p);
            }
            lijn.begin(p);
            lijn.zet(p, n);
            Achtervolging.stop(p);
            Brug.heel(p);
            return zeg(c, "ring_h3: step " + lijn.stap(p) + " of " + lijn.stappen());
        })));
        h3.then(Commands.literal("ga").then(Commands.argument("plek", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(PLEKKEN.keySet(), b)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Mijn m = Mijn.bij(p.level(), p.blockPosition());
                    BlockPos lokaal = PLEKKEN.get(StringArgumentType.getString(c, "plek"));
                    if (m == null || lokaal == null) {
                        return zeg(c, m == null ? "no copy of the mine within 48 blocks" : "no such spot: " + PLEKKEN.keySet());
                    }
                    Vec3 plek = m.midden(lokaal);
                    p.teleportTo(plek.x, plek.y, plek.z);
                    return zeg(c, "at " + lokaal.toShortString() + " of the template");
                })));
        h3.then(Commands.literal("deur").then(Commands.argument("naam", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(Deuren.Deur.values()).map(d -> d.naam), b)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Mijn m = Mijn.bij(p.level(), p.blockPosition());
                    String naam = StringArgumentType.getString(c, "naam");
                    for (Deuren.Deur d : Deuren.Deur.values()) {
                        if (m != null && d.naam.equals(naam)) {
                            Deuren.open(m, d);
                            return zeg(c, "the door " + naam + " is open for " + Deuren.OPEN_TICKS + " ticks");
                        }
                    }
                    return zeg(c, m == null ? "no copy of the mine within 48 blocks" : "no such door");
                })));
        h3.then(Commands.literal("rog").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Mijn m = Mijn.bij(p.level(), p.blockPosition());
            if (m == null) {
                return zeg(c, "no copy of the mine within 48 blocks");
            }
            Achtervolging.stop(p);
            return zeg(c, "Barbecuerog: " + (Achtervolging.start(p, m) != null) + " (he only stays while your step is 5)");
        }).then(Commands.literal("weg").executes(c -> {
            Achtervolging.stop(c.getSource().getPlayerOrException());
            return zeg(c, "the chase is over");
        })));
        h3.then(Commands.literal("brug").then(Commands.literal("kapot").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Mijn m = Mijn.bij(p.level(), p.blockPosition());
            if (m != null) {
                Brug.breek(p, m, Brug.KAPOT_TICKS);
            }
            return zeg(c, m == null ? "no copy of the mine within 48 blocks" : "your bridge is broken for a minute");
        })).then(Commands.literal("heel").executes(c -> {
            Brug.heel(c.getSource().getPlayerOrException());
            return zeg(c, "your bridge is whole");
        })));
        h3.then(Commands.literal("scene").then(Commands.argument("welke", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.List.of("emmer", "brug"), b)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Mijn m = Mijn.bij(p.level(), p.blockPosition());
                    if (m == null) {
                        return zeg(c, "no copy of the mine within 48 blocks");
                    }
                    boolean brug = StringArgumentType.getString(c, "welke").equals("brug");
                    Achtervolging.stop(p);
                    boolean speelt = Cutscenes.speel(p, brug ? Scenes.BRUG : Scenes.EMMER, m.wereld(brug ? Plekken.BRUG_ANKER : Plekken.PUT), m.draai(), null);
                    return zeg(c, "scene: " + speelt);
                })));
        if (!FMLEnvironment.isProduction()) {
            h3.then(Commands.literal("bouw").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                BlockPos nul = p.blockPosition().offset(-28, -Plekken.BOVEN, -62);
                if (nul.getY() < p.level().getMinY() + 1) {
                    return zeg(c, "stand at least " + (Plekken.BOVEN + 1) + " blocks above the bottom of the world");
                }
                return zeg(c, MijnProef.bouw(p.level(), nul) != null ? "a copy of the mine: its block (0, 0, 0) is at " + nul.toShortString()
                        : "the template guhs:knabbelmoria is missing");
            }));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(h3));
    }

    private static int stand(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Mijn m = Mijn.bij(p.level(), p.blockPosition());
        Verhaallijn lijn = RingH3Feature.LIJN;
        StringBuilder s = new StringBuilder("ring_h3: step " + lijn.stap(p) + " of " + lijn.stappen() + (lijn.aanDeBeurt(p) ? "" : " (ring_h2 is not done)"));
        if (m == null) {
            s.append("; no copy of the mine within 48 blocks");
        } else {
            s.append("; mine at ").append(m.nul().toShortString()).append(" turned ").append(m.draai()).append(", you are at ")
                    .append(m.lokaal(p.blockPosition()).toShortString()).append(" of the template; doors:");
            for (Deuren.Deur d : Deuren.Deur.values()) {
                s.append(' ').append(d.naam).append(Deuren.isOpen(m, d) ? " open" : " closed");
            }
        }
        s.append("; chase: ").append(Achtervolging.van(p) != null).append("; bridge broken for you: ").append(Brug.isKapot(p));
        return zeg(c, s.toString());
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal(tekst), false);
        return 1;
    }
}
