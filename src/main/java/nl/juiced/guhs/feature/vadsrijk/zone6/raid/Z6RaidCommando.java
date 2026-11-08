package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;

/**
 * U-Z6RAID dev commands, {@code /guhs vadsrijk z6raid ...} (gamemaster): see SPIEK_U_Z6RAID.md.
 * bouw | tp &lt;wachtzaal|poort|1..4&gt; | start &lt;n&gt; | verdieping &lt;1..4&gt; | baas &lt;1..4&gt; | wipe | stop | klaar | fase &lt;n&gt; | mech &lt;wat&gt; |
 * loot [baas] | items | sterren aan|uit | info
 */
public final class Z6RaidCommando {
    private static final List<String> PLEKKEN = List.of("wachtzaal", "poort", "1", "2", "3", "4");
    private static final List<String> MECH = List.of("zandloper", "omdraaien", "pilaren", "verstop", "ontmasker", "fout", "uit", "lens", "bakens", "val",
            "schop");

    private Z6RaidCommando() {
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static ServerLevel rijk(CommandContext<CommandSourceStack> c) {
        return Vadsrijk.level(c.getSource().getServer());
    }

    private static Z6RaidBaas dichtste(ServerPlayer p) {
        return p.level().getEntitiesOfClass(Z6RaidBaas.class, p.getBoundingBox().inflate(48), b -> !b.geplofd() && b.echteBaas()).stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p))).orElse(null);
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("z6raid");
        b.then(dev("bouw").executes(c -> zeg(c, Z6RaidBouw.plaats(rijk(c)) ? "Binnentoren geplaatst rond " + Z6RaidPlekken.anker().toShortString()
                : "Template ontbreekt (draai tools/vadsrijk/zone6/raid/binnentoren.py en herbouw).")));
        b.then(dev("tp").then(Commands.argument("waar", StringArgumentType.word()).suggests((c, s) -> SharedSuggestionProvider.suggest(PLEKKEN, s)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String w = StringArgumentType.getString(c, "waar");
            if (Z6RaidData.get(c.getSource().getServer()).bouwVersie < Z6RaidBouw.VERSIE) {
                Z6RaidBouw.plaats(rijk(c));
            }
            Vec3 v = switch (w) {
                case "poort" -> Vec3.atBottomCenterOf(Z6RaidPlekken.poort()).add(-3, 0, 0);
                case "wachtzaal" -> Vec3.atBottomCenterOf(Z6RaidPlekken.midden(0).offset(8, 0, 0));
                default -> Vec3.atBottomCenterOf(Z6RaidPlekken.landing(Math.max(1, Math.min(4, Integer.parseInt(w)))));
            };
            Z6Raid.naar(p, v, -90);
            return zeg(c, "Naar " + w + ".");
        })));
        b.then(dev("start").then(Commands.argument("n", IntegerArgumentType.integer(1, 10)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel sl = rijk(c);
            if (Z6RaidData.get(sl.getServer()).bouwVersie < Z6RaidBouw.VERSIE) {
                Z6RaidBouw.plaats(sl);
            }
            Z6Raid.wisAlles();
            Z6Raid.naar(p, Vec3.atBottomCenterOf(Z6RaidPlekken.midden(0)), -90);
            int n = IntegerArgumentType.getInteger(c, "n");
            Z6Raid.Groep g = Z6Raid.devStart(sl, p, n);
            return zeg(c, "Raid gestart met " + g.n + " spelers (jij + " + (g.n - 1) + " nepguhs): de bazen schalen x" + String.format("%.2f", Z6Schaal.factor(g.n))
                    + " Vads, mechanieken " + Z6Schaal.mech(g.n) + ".");
        })));
        b.then(dev("verdieping").then(Commands.argument("k", IntegerArgumentType.integer(1, 4)).executes(c -> {
            ServerLevel sl = rijk(c);
            Z6Raid.Groep g = Z6Raid.groep();
            if (g == null) {
                return zeg(c, "Geen raid bezig: eerst /guhs vadsrijk z6raid start <n>.");
            }
            int k = IntegerArgumentType.getInteger(c, "k");
            for (int i = 0; i <= 3; i++) {
                Z6RaidBouw.poort(sl, i, i >= k || i == 0);
            }
            for (int i = 1; i <= 4; i++) {
                Arenas.reset(sl.getServer(), Z6Raid.arenaId(i));
            }
            g.verdieping = k;
            g.gevechtStart = -1;
            for (ServerPlayer p : Z6Raid.leden(sl)) {
                Z6Raid.naar(p, Vec3.atBottomCenterOf(Z6RaidPlekken.landing(k)), -90);
            }
            return zeg(c, "Naar verdieping " + k + " (" + Z6Raid.BAZEN[k] + "): de trappen eronder staan open.");
        })));
        b.then(dev("baas").then(Commands.argument("k", IntegerArgumentType.integer(1, 4)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int k = IntegerArgumentType.getInteger(c, "k");
            Z6Raid.DEV_SPAWN[k] = true;
            Z6Raid.DEV_MEE.add(p.getUUID());
            Arenas.reset(c.getSource().getServer(), Z6Raid.arenaId(k));
            Z6Raid.naar(p, Vec3.atBottomCenterOf(Z6RaidPlekken.landing(k)), -90);
            return zeg(c, "Baas " + Z6Raid.BAZEN[k] + " staat zo klaar zonder groep (schaalt voor 2). Loop de zaal in (creatief vecht mee).");
        })));
        b.then(dev("wipe").executes(c -> {
            Z6Raid.Groep g = Z6Raid.groep();
            if (g == null) {
                return zeg(c, "Geen raid bezig.");
            }
            Z6Raid.wipe(rijk(c), g);
            return zeg(c, "Wipe! Verdieping " + g.verdieping + " is terug bij af (wipes: " + g.wipes + ").");
        }));
        b.then(dev("stop").executes(c -> {
            Z6Raid.stop(rijk(c));
            Z6Raid.wisAlles();
            return zeg(c, "Raid gestopt; de toren is vrij (rij leeg).");
        }));
        b.then(dev("klaar").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel sl = rijk(c);
            Z6Raid.Groep g = Z6Raid.groep();
            if (g == null) {
                g = Z6Raid.devStart(sl, p, 2);
            }
            Z6Raid.naar(p, Vec3.atBottomCenterOf(Z6RaidPlekken.midden(4).offset(0, 0, 8)), 180);
            Z6Raid.klaar(sl, g);
            return zeg(c, "Clear! Kisten, sterren en de haak voor u-z6verhaal (" + Z6RaidHaken.geroepen + "x geroepen).");
        }));
        b.then(dev("fase").then(Commands.argument("n", IntegerArgumentType.integer(2, 3)).executes(c -> {
            Z6RaidBaas z = dichtste(c.getSource().getPlayerOrException());
            if (z == null) {
                return zeg(c, "Geen raidbaas in de buurt.");
            }
            int n = IntegerArgumentType.getInteger(c, "n");
            double[] f = z instanceof Ringeling ? new double[] {0.6, 0.25} : z instanceof Zandwachter ? new double[] {0.5, 0.25} : new double[] {0.2};
            while (z.fase() < n && z.fase() - 1 < f.length && !z.geplofd()) {
                double doel = z.maxVads() * f[z.fase() - 1] - 1;
                if (z.vads() > doel) {
                    z.heal(doel - z.vads());
                }
                int voor = z.fase();
                z.krijgSchade(1, null);
                if (z.fase() == voor) {
                    break;
                }
            }
            return zeg(c, "Fase nu " + z.fase());
        })));
        b.then(dev("mech").then(Commands.argument("wat", StringArgumentType.word()).suggests((c, s) -> SharedSuggestionProvider.suggest(MECH, s)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel sl = rijk(c);
            Z6RaidBaas z = dichtste(p);
            String w = StringArgumentType.getString(c, "wat");
            if (z == null) {
                return zeg(c, "Geen raidbaas in de buurt.");
            }
            switch (w) {
                case "zandloper" -> {
                    if (z instanceof Zandwachter zw) {
                        zw.startZandloper(sl);
                    }
                }
                case "omdraaien" -> {
                    if (z instanceof Zandwachter zw) {
                        zw.omdraaien(sl);
                    }
                }
                case "pilaren" -> {
                    if (z instanceof Zandwachter zw) {
                        zw.pilaren(sl);
                    }
                }
                case "verstop" -> {
                    if (z instanceof Spiegelvizier s) {
                        s.verstop(sl);
                    }
                }
                case "ontmasker", "fout" -> {
                    if (z instanceof Spiegelvizier s && s.verstopt()) {
                        int echt = s.echtePlek();
                        s.ontmasker(sl, w.equals("ontmasker") ? echt : (echt + 1) % s.plekken().size());
                    }
                }
                case "uit" -> {
                    if (z instanceof Sterrenbeeld s) {
                        for (Sterrenbeeld x : s.alle()) {
                            x.krijgSchade(x.vads() + 1, p);
                        }
                    }
                }
                case "lens" -> {
                    if (z instanceof Ringeling r) {
                        r.lens(sl);
                    }
                }
                case "bakens" -> {
                    if (z instanceof Ringeling r) {
                        for (Z3Doel d : r.bakens()) {
                            if (d.stand() == 0) {
                                r.steekAan(sl, d);
                            }
                        }
                    }
                }
                case "val" -> {
                    if (z instanceof Ringeling r) {
                        r.val(sl);
                    }
                }
                case "schop" -> {
                    if (z instanceof Ringeling r) {
                        r.schop(sl, List.of(p));
                    }
                }
                default -> {
                    return zeg(c, "Kies uit " + MECH);
                }
            }
            return zeg(c, w + " bij " + z.baasId() + ".");
        })));
        b.then(dev("loot").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            BaasKist.plaats((ServerLevel) p.level(), p, BlockPos.containing(p.position().add(p.getLookAngle().multiply(2, 0, 2))),
                    Z6RaidLoot.clearKist(p, Z6Raid.groep(), p.getRandom()));
            return zeg(c, "Een raidkist (clear) voor jou.");
        }).then(Commands.literal("baas").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            BaasKist.plaats((ServerLevel) p.level(), p, BlockPos.containing(p.position().add(p.getLookAngle().multiply(2, 0, 2))),
                    Z6RaidLoot.baasKist("zandwachter", p, p.getRandom()));
            return zeg(c, "Een kleine baaskist (verdieping 1-3).");
        })).then(Commands.literal("zeldzaam").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Z6RaidLoot.vleugels(p);
            nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren.geef(p, Z6RaidLoot.HUISDIER);
            p.getInventory().add(new ItemStack(Z6RaidLoot.HALSTER.get()));
            p.getInventory().add(Z6RaidLoot.teenring(p.getRandom()));
            return zeg(c, "Vleugels (Kledingkast), Sterrenpluisje, Sterrennacht-halster en Ringelings Teenring.");
        })));
        b.then(dev("items").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (int i = 0; i < Z6RaidLoot.STUKKEN.size(); i++) {
                p.getInventory().add(Z6RaidLoot.setStuk(i, p.getRandom()));
            }
            for (String id : List.of("z6raid_ringbreker", "z6raid_komeetboog", "z6raid_sterrenstaf")) {
                var t = nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes.van(id);
                if (t != null) {
                    p.getInventory().add(nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.stack(nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.rol(t, 70,
                            nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid.LEGENDARISCH, p.getRandom())));
                }
            }
            p.getInventory().add(new ItemStack(Z6RaidLoot.REPLICA.get()));
            return zeg(c, "De Sterrenzijde-set, de 3 raidwapens en de Ringreplica.");
        }));
        b.then(dev("sterren").then(Commands.literal("aan").executes(c -> zeg(c, Z6RaidBouw.sterren(rijk(c), true) + " sterren aan.")))
                .then(Commands.literal("uit").executes(c -> zeg(c, Z6RaidBouw.sterren(rijk(c), false) + " sterren uit."))));
        b.then(dev("info").executes(c -> {
            Z6Raid.Groep g = Z6Raid.groep();
            ServerLevel sl = rijk(c);
            StringBuilder s = new StringBuilder("Toren: ");
            if (g == null) {
                s.append("vrij");
            } else {
                s.append(g.n).append(" spelers, verdieping ").append(Math.min(4, g.verdieping)).append(g.klaar() ? " (klaar)" : "").append(", wipes ").append(g.wipes)
                        .append(", tijd ").append(Z6Raid.tijd(sl.getGameTime() - g.start)).append(", leden ").append(g.namen.values());
            }
            s.append(". Rij: ").append(Z6Raid.RIJ.size()).append(" groep(en). Template v").append(Z6RaidData.get(sl.getServer()).bouwVersie).append(", anker ")
                    .append(Z6RaidPlekken.anker().toShortString()).append(", clears ").append(Z6RaidData.get(sl.getServer()).clears);
            for (int k = 1; k <= 4; k++) {
                VadsrijkBaas bb = Arenas.baas(sl, Z6Raid.arenaId(k));
                s.append("\n ").append(k).append(" ").append(Z6Raid.BAZEN[k]).append(": ")
                        .append(bb == null ? "-" : String.format("%.0f/%.0f fase %d", bb.vads(), bb.maxVads(), bb.fase()))
                        .append(k <= 3 ? (Z6RaidBouw.poortOpen(sl, k) ? ", trap open" : ", trap dicht") : "");
            }
            return zeg(c, s.toString());
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(b));
    }
}
