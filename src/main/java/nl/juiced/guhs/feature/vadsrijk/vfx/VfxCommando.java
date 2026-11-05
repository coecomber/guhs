package nl.juiced.guhs.feature.vadsrijk.vfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;

/**
 * {@code /guhs vadsrijk vfx ...} (T-VFX): {@code niveau vol|zacht|uit} (everyone: your own VFX level), and for ops:
 * {@code demo <wat>} (every effect at your feet), {@code aanval <id>} (start a bouwdoos attack on the nearest boss),
 * {@code aanvallen} (list), {@code anim <naam>} (play an animation on the nearest boss), {@code woede} (soft enrage now),
 * {@code stop}, {@code spawn <baas>} (the right zone command for that boss).
 */
final class VfxCommando {
    static final List<String> DEMOS = List.of("cirkel", "kegel", "lijn", "donut", "veilig", "kruis", "deeltjes", "golf", "straal", "flits", "spoor", "zuil",
            "cast", "pose", "alles");
    /** boss key -> the existing dev command(s) that put you in front of it (without the leading slash). */
    static final Map<String, List<String>> SPAWN = Map.ofEntries(
            Map.entry("klodderreus", List.of("summon guhs:vr_klodderreus ~ ~ ~6")),
            Map.entry("koning_klodder", List.of("guhs vadsrijk baas reset bazen:koning_klodder", "guhs vadsrijk bazen tp koning")),
            Map.entry("opperzwam", List.of("guhs vadsrijk baas reset bazen:opperzwam", "guhs vadsrijk bazen tp opperzwam")),
            Map.entry("galmwolf", List.of("guhs vadsrijk baas reset z2bazen:galmwolf", "guhs vadsrijk z2bazen tp galmwolf")),
            Map.entry("spiegelzucht", List.of("guhs vadsrijk baas reset z2bazen:spiegelzucht", "guhs vadsrijk z2bazen tp spiegelzucht")),
            Map.entry("boormeester", List.of("guhs vadsrijk z2bazen boor ontgrendel", "guhs vadsrijk z2bazen tp boor")),
            Map.entry("kruinroofvogel", List.of("guhs vadsrijk z3bazen spawn kruinroofvogel")),
            Map.entry("taleggio", List.of("guhs vadsrijk z3bazen spawn taleggio")),
            Map.entry("brokkel", List.of("guhs vadsrijk z3bazen spawn brokkel")),
            Map.entry("roquefort", List.of("guhs vadsrijk z3bazen spawn roquefort")),
            Map.entry("piepvel", List.of("guhs vadsrijk z3bazen spawn piepvel")),
            Map.entry("wortelwurger", List.of("guhs vadsrijk z3bazen spawn wortelwurger")),
            Map.entry("nestwachter", List.of("guhs vadsrijk z3bazen spawn nestwachter")),
            Map.entry("bleu", List.of("guhs vadsrijk z3bazen spawn bleu")),
            Map.entry("vahoegnir", List.of("guhs vadsrijk z3bazen vahoegnir reset", "guhs vadsrijk z3bazen spawn vahoegnir")));

    private VfxCommando() {
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> v = Commands.literal("vfx");
        v.then(Commands.literal("niveau").then(Commands.argument("niveau", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("vol", "zacht", "uit"), b)).executes(c -> {
                    String n = StringArgumentType.getString(c, "niveau").toLowerCase(java.util.Locale.ROOT);
                    if (!List.of("vol", "zacht", "uit").contains(n)) {
                        return zeg(c, "Kies vol, zacht of uit.");
                    }
                    Vfx.niveau(c.getSource().getPlayerOrException(), n.toUpperCase(java.util.Locale.ROOT));
                    return zeg(c, "VFX-niveau: " + n + " (alleen voor jou; telegraphs blijven altijd zichtbaar).");
                })));
        v.then(dev("demo").then(Commands.argument("wat", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(DEMOS, b))
                .executes(c -> demo(c, StringArgumentType.getString(c, "wat")))));
        v.then(dev("aanval").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            List<String> ids = new ArrayList<>();
            BaasProfielen.alle().values().forEach(l -> l.forEach(a -> ids.add(a.id())));
            return SharedSuggestionProvider.suggest(ids, b);
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            VadsrijkBaas baas = dichtste(p);
            Aanval a = BaasProfielen.aanval(StringArgumentType.getString(c, "id"));
            if (baas == null) {
                return zeg(c, "Geen baas binnen 64 blokken. Eerst: /guhs vadsrijk vfx spawn <baas>");
            }
            if (a == null) {
                return zeg(c, "Onbekende aanval. Lijst: /guhs vadsrijk vfx aanvallen");
            }
            int duur = BaasVfx.start(baas, (ServerLevel) baas.level(), a);
            return zeg(c, a.id() + " gestart op " + BaasVfx.sleutel(baas) + " (" + duur / 20.0 + " s)" + (a.onderbreekbaar()
                    ? ". Onderbreekbaar: doe " + Math.round(a.onderbreek * 100) + "% van zijn max Vads schade tijdens de cast (of verdoof hem)." : "."));
        })));
        v.then(dev("aanvallen").executes(c -> {
            StringBuilder s = new StringBuilder("Bouwdoos-aanvallen per baas:");
            BaasProfielen.alle().forEach((baas, l) -> {
                s.append("\n").append(baas).append(": ");
                for (Aanval a : l) {
                    s.append(a.id()).append(a.minFase() > 1 ? " (fase " + a.minFase() + "+)" : "").append(a.onderbreekbaar() ? " [onderbreek]" : "").append("  ");
                }
            });
            return zeg(c, s.toString());
        }));
        v.then(dev("anim").then(Commands.argument("naam", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(BaasAnims.STANDAARD, b)).executes(c -> {
                    VadsrijkBaas baas = dichtste(c.getSource().getPlayerOrException());
                    if (baas == null) {
                        return zeg(c, "Geen baas binnen 64 blokken.");
                    }
                    String n = StringArgumentType.getString(c, "naam");
                    BaasAnims.speel(baas, n);
                    Vfx.pose((ServerLevel) baas.level(), baas, n.replace("vfx_", ""), 30);
                    return zeg(c, "Animatie " + n + " op " + BaasVfx.sleutel(baas));
                })));
        v.then(dev("woede").executes(c -> {
            VadsrijkBaas baas = dichtste(c.getSource().getPlayerOrException());
            if (baas == null) {
                return zeg(c, "Geen baas binnen 64 blokken.");
            }
            BaasVfx.Staat s = BaasVfx.staat(baas);
            s.gevechtStart = baas.level().getGameTime() - BaasVfx.WOEDE_NA - 1;
            return zeg(c, "Woede (soft enrage) komt binnen 1 s: aanvallen 2x zo vaak, rode rook.");
        }));
        v.then(dev("stop").executes(c -> {
            VadsrijkBaas baas = dichtste(c.getSource().getPlayerOrException());
            if (baas != null) {
                BaasVfx.stop(baas, (ServerLevel) baas.level());
            }
            return zeg(c, "Gestopt.");
        }));
        v.then(dev("spawn").then(Commands.argument("baas", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(SPAWN.keySet(), b))
                .executes(c -> {
                    List<String> cmds = SPAWN.get(StringArgumentType.getString(c, "baas"));
                    if (cmds == null) {
                        return zeg(c, "Onbekende baas. Kies: " + String.join(", ", SPAWN.keySet()));
                    }
                    for (String cmd : cmds) {
                        c.getSource().getServer().getCommands().performPrefixedCommand(c.getSource(), cmd);
                    }
                    return 1;
                })));
        event.getDispatcher().register(VadsrijkCommando.wortel(v));
    }

    @Nullable
    static VadsrijkBaas dichtste(ServerPlayer p) {
        VadsrijkBaas best = null;
        double d = Double.MAX_VALUE;
        for (VadsrijkBaas b : p.level().getEntitiesOfClass(VadsrijkBaas.class, new AABB(p.blockPosition()).inflate(64), b -> !b.geplofd())) {
            double x = b.distanceToSqr(p);
            if (x < d) {
                d = x;
                best = b;
            }
        }
        return best;
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    /** Every effect at your feet (decals face where you look). */
    private static int demo(CommandContext<CommandSourceStack> c, String wat) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel sl = (ServerLevel) p.level();
        Vec3 voet = new Vec3(p.getX(), Math.floor(p.getY() + 0.01), p.getZ());
        double kijk = Math.toRadians(p.getYRot() + 90);
        Vec3 voor = voet.add(Math.cos(kijk) * 6, 0, Math.sin(kijk) * 6);
        boolean alles = wat.equals("alles");
        int g = Vfx.nieuweGroep();
        if (alles || wat.equals("cirkel")) {
            Vfx.decal(sl, Vorm.cirkel(3), voor, 0, 60, 0xFF2A2A, g);
        }
        if (alles || wat.equals("kegel")) {
            Vfx.decal(sl, Vorm.kegel(10, 70), voet, kijk, 60, 0xFF2A2A, g);
        }
        if (alles || wat.equals("lijn")) {
            Vfx.decal(sl, Vorm.lijn(14, 3), voet, kijk + 0.8, 60, 0xA040FF, g);
        }
        if (alles || wat.equals("donut")) {
            Vfx.decal(sl, Vorm.donut(4, 9), voet, 0, 70, 0xFF2A2A, g);
        }
        if (alles || wat.equals("veilig")) {
            Vfx.decal(sl, Vorm.veilig(3), voor, 0, 80, 0xFF5FC8, g);
        }
        if (alles || wat.equals("kruis")) {
            Vfx.decal(sl, Vorm.kruis(10, 2.5), voor, kijk, 60, 0xFFD62A, g);
        }
        if (alles || wat.equals("deeltjes")) {
            int i = 0;
            for (Deeltje d : Deeltje.values()) {
                double a = Math.PI * 2 * i++ / Deeltje.values().length;
                Vfx.deeltjes(sl, d, voet.add(Math.cos(a) * 4, 1.5, Math.sin(a) * 4), 14, d == Deeltje.CONFETTI ? 0xFFFFFF : 0xFF8AD0, 0.4, 0.12);
            }
        }
        if (alles || wat.equals("golf")) {
            Vfx.golf(sl, voet, 8, 0xFF8AD0, 16);
        }
        if (alles || wat.equals("straal")) {
            Vfx.straal(sl, voet.add(0, 1, 0), voor.add(0, 1, 0).add(Math.cos(kijk) * 6, 0, Math.sin(kijk) * 6), 1.2, 0xFFE0A0, 30);
        }
        if (alles || wat.equals("flits")) {
            Vfx.flits(sl, voor.add(0, 1, 0), 3, 0xFFFFFF);
        }
        if (alles || wat.equals("spoor")) {
            Vfx.spoor(sl, p, 0xFF8AD0, 100, 0.6);
        }
        if (alles || wat.equals("zuil")) {
            Vfx.zuil(sl, voor, 12, 0xFFE0A0, 60);
        }
        if ((alles || wat.equals("cast")) && dichtste(p) != null) {
            Vfx.cast(dichtste(p), "vadsrijk.vfx.aanval.demo", 80, true);
        }
        if ((alles || wat.equals("pose")) && dichtste(p) != null) {
            Vfx.pose(sl, dichtste(p), "windup", 40);
        }
        return zeg(c, "Demo: " + wat + (wat.equals("cast") || wat.equals("pose") ? " (op de dichtstbijzijnde baas)" : ""));
    }
}
