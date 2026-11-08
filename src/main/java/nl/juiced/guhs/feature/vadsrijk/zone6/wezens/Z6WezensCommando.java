package nl.juiced.guhs.feature.vadsrijk.zone6.wezens;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Toegang;
import nl.juiced.guhs.feature.vadsrijk.zone6.bultguh.Bultguh;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Astrolabiumwachter;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Glimmertje;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Sterrenguh;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Wormelien;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Z6Baas;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Z6Bazen;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Z6BazenLoot;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Zoutkolos;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.loot.Z6Loot;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.loot.Z6Sets;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Ruine;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Z6Wezen;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Zout;

/**
 * Dev command {@code /guhs vadsrijk z6wezens ...} (op level 2), see SPIEK_U_Z6WEZENS.md:
 * <pre>
 *   spawn &lt;soort&gt; [level] [elite]     one creature 4 blocks in front of you
 *   alle [level]                      every kind in a row on a fresh sand platform (no AI)
 *   tp &lt;plek&gt;                         to an area / boss / the dungeon / the cave
 *   baas &lt;baas&gt;                       resets that boss (field bosses also at the wrong time of day) and takes you there
 *   fase &lt;n&gt;                          the nearest zone-6 boss to phase n
 *   mechaniek &lt;naam&gt;                  forces a mechanic on the nearest boss / creature
 *   bouw kerker|arenas                builds the dungeon rooms / sand floors under the field-boss arenas
 *   set &lt;set&gt; [level] | sieraden | wapens | kristallen | items | huisdieren | sterrenstof
 *   loot &lt;soort&gt; [level] [elite] | kist &lt;baas&gt;
 *   bultguh [vacht]                   gives the Bultguh (in that coat) and puts you on it
 *   plekken | spawners | muziek | opruimen
 * </pre>
 */
public final class Z6WezensCommando {
    private Z6WezensCommando() {
    }

    private static final List<String> MECHANIEKEN = List.of("duik", "duinspuug", "korst", "zoutgolf", "kristallen", "zing", "ringzwaai", "lijn",
            "doof", "hart", "spiegel", "slangduik", "ringpot");

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam);
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("z6wezens").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        b.then(dev("spawn").then(Commands.argument("soort", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(Z6WezensRegistry.SOORTEN.keySet(), s))
                .executes(c -> spawn(c, 0, false))
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 80)).executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), false))
                        .then(Commands.argument("elite", BoolArgumentType.bool())
                                .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite")))))));
        b.then(dev("alle").executes(c -> alle(c, 64)).then(Commands.argument("level", IntegerArgumentType.integer(1, 80))
                .executes(c -> alle(c, IntegerArgumentType.getInteger(c, "level")))));
        b.then(dev("tp").then(Commands.argument("plek", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(Z6WezensPlekken.tpPlekken().keySet(), s)).executes(Z6WezensCommando::tp)));
        b.then(dev("baas").then(Commands.argument("baas", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(Z6WezensRegistry.BAZEN.keySet(), s)).executes(Z6WezensCommando::baas)));
        b.then(dev("fase").then(Commands.argument("n", IntegerArgumentType.integer(2, 4)).executes(Z6WezensCommando::fase)));
        b.then(dev("mechaniek").then(Commands.argument("naam", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(MECHANIEKEN, s)).executes(Z6WezensCommando::mechaniek)));
        b.then(dev("bouw").then(Commands.literal("kerker").executes(c -> {
            ServerLevel sl = rijk(c);
            int n = Z6Bouw.sterrenwacht(sl, true);
            return zeg(c, "De Verzonken Sterrenwacht gebouwd (" + n + " blokken). Naar binnen: /guhs vadsrijk z6wezens tp kerker");
        })).then(Commands.literal("arenas").executes(c -> zeg(c, "Zandvloeren onder de veldbaas-arena's: " + Z6Bouw.arenaVloeren(rijk(c))
                + " blokken (verdwijnen bij de volgende herstart)."))));
        b.then(dev("set").then(Commands.argument("set", StringArgumentType.word()).suggests((c, s) -> SharedSuggestionProvider.suggest(Z6Sets.SETS, s))
                .executes(c -> set(c, 64)).then(Commands.argument("level", IntegerArgumentType.integer(1, 80))
                        .executes(c -> set(c, IntegerArgumentType.getInteger(c, "level"))))));
        b.then(dev("sieraden").executes(c -> lijst(c, Z6Loot.SIERADEN, 64)));
        b.then(dev("wapens").executes(c -> lijst(c, Z6Loot.WAPENS, 64)).then(Commands.argument("level", IntegerArgumentType.integer(1, 80))
                .executes(c -> lijst(c, Z6Loot.WAPENS, IntegerArgumentType.getInteger(c, "level")))));
        b.then(dev("kristallen").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (String k : Z6Loot.KRISTALLEN) {
                for (int q = 0; q < 3; q++) {
                    p.getInventory().add(Kristallen.stack(k, q, 1));
                }
            }
            return zeg(c, "Rozenkwarts en Sterrenzout in ruw, geslepen en stralend.");
        }));
        b.then(dev("sterrenstof").executes(c -> {
            c.getSource().getPlayerOrException().getInventory().add(new ItemStack(Z6WezensRegistry.STERRENSTOF.get(), 16));
            return zeg(c, "16 Sterrenstof.");
        }));
        b.then(dev("items").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Z6BazenLoot.Benoemd x : Z6BazenLoot.BENOEMD) {
                p.getInventory().add(Z6BazenLoot.maak(x, 66, p.getRandom()));
            }
            for (String t : Z6BazenLoot.MET_TROFEE) {
                p.getInventory().add(Z6BazenLoot.trofee(t));
            }
            p.getInventory().add(new ItemStack(Z6WezensRegistry.STERRENKOMPAS.get()));
            return zeg(c, "Alle zone-6-baasitems, de 3 trofeeën en het Sterrenkompas.");
        }));
        b.then(dev("huisdieren").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Huisdier h : Z6WezensRegistry.HUISDIEREN) {
                Huisdieren.geef(p, h.id());
            }
            return zeg(c, "Alle 8 zone-6-huisdieren (druk P).");
        }));
        b.then(dev("loot").then(Commands.argument("soort", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(Z6WezensRegistry.SOORTEN.keySet(), s))
                .executes(c -> loot(c, 64, false))
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 80)).executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), false))
                        .then(Commands.argument("elite", BoolArgumentType.bool())
                                .executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite")))))));
        b.then(dev("kist").then(Commands.argument("baas", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(Z6BazenLoot.BAZEN, s)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String baas = StringArgumentType.getString(c, "baas");
                    BaasKist.plaats(p.level(), p, BlockPos.containing(p.position().add(p.getLookAngle().multiply(2, 0, 2))),
                            Z6BazenLoot.kist(baas, p, 66, p.getRandom()));
                    return zeg(c, "Een buitkist van " + baas + " voor jou.");
                })));
        b.then(dev("bultguh").executes(c -> bultguh(c, "naturel")).then(Commands.argument("vacht", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(Bultguh.VACHTEN, s)).executes(c -> bultguh(c, StringArgumentType.getString(c, "vacht")))));
        b.then(dev("plekken").executes(c -> {
            StringBuilder sb = new StringBuilder("Zone-6-plekken (" + (Z6WezensPlekken.echt() ? "echte POI van u-z6wereld" : "PLACEHOLDERS") + "):");
            for (Map.Entry<String, BlockPos> e : Z6WezensPlekken.POI.entrySet()) {
                sb.append("\n ").append(e.getKey()).append(" ").append(e.getValue().toShortString());
            }
            return zeg(c, sb.toString());
        }));
        b.then(dev("spawners").executes(c -> zeg(c, spawnersTekst())));
        b.then(dev("muziek").executes(c -> {
            StringBuilder sb = new StringBuilder("Baasmuziek zone 6 (eigen thema-id -> speelt nu):");
            for (var e : Z6Muziek.THEMA.entrySet()) {
                sb.append("\n ").append(e.getKey()).append(": vr.geluid.baas.").append(e.getValue()).append(" -> ").append(Z6Muziek.thema(e.getKey()));
            }
            return zeg(c, sb.toString());
        }));
        b.then(dev("opruimen").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return zeg(c, opruimen(p.level(), p.blockPosition(), 64) + " zone-6-wezens opgeruimd.");
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(b));
    }

    private static ServerLevel rijk(CommandContext<CommandSourceStack> c) {
        return Vadsrijk.level(c.getSource().getServer());
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static int fout(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendFailure(Component.literal(s));
        return 0;
    }

    public static String spawnersTekst() {
        return Z6WezensSpawners.ALLE.size() + " zone-6-spawners, max levend per gebied " + Z6WezensSpawners.MAX_PER_GEBIED + " (totaal "
                + Z6WezensSpawners.maxTotaal() + "); plekken: " + (Z6WezensPlekken.echt() ? "echte POI van u-z6wereld" : "PLACEHOLDERS (nog geen POI_Z6)")
                + ". Geen wezens in Rozenkloof, Saffraanpoort, het karavaankamp en aan de voet van de toren; Zandgeestjes alleen 's nachts.";
    }

    public static int opruimen(ServerLevel sl, BlockPos pos, int r) {
        List<Entity> l = new ArrayList<>(sl.getEntitiesOfClass(Z6Wezen.class, new AABB(pos).inflate(r)));
        l.forEach(Entity::discard);
        return l.size();
    }

    /** Makes one creature at a spot (level, elite with these affixes). */
    public static Entity maak(ServerLevel sl, String soort, Vec3 pos, float yaw, int level, boolean elite, EnumSet<Affix> af) {
        var holder = Z6WezensRegistry.SOORTEN.get(soort);
        if (holder == null) {
            return null;
        }
        EntityType<?> type = holder.get();
        Entity e = type.create(sl, EntitySpawnReason.COMMAND);
        if (e == null) {
            return null;
        }
        e.snapTo(pos.x, pos.y, pos.z, yaw, 0);
        if (e instanceof VadsrijkMob m) {
            m.stelIn(level, null, elite, af);
        }
        sl.addFreshEntity(e);
        return e;
    }

    private static int spawn(CommandContext<CommandSourceStack> c, int level, boolean elite) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String soort = StringArgumentType.getString(c, "soort");
        EnumSet<Affix> af = EnumSet.noneOf(Affix.class);
        if (elite) {
            Affix[] alle = Affix.values();
            af.add(alle[p.getRandom().nextInt(alle.length)]);
        }
        int lvl = level > 0 ? level : 64;
        Vec3 voor = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(4));
        Entity e = maak(p.level(), soort, new Vec3(voor.x, p.getY(), voor.z), p.getYRot() + 180, lvl, elite, af);
        if (e == null) {
            return fout(c, "Dat ken ik niet: " + soort + ". Kies uit " + Z6WezensRegistry.SOORTEN.keySet());
        }
        return zeg(c, soort + " gemaakt (level " + lvl + (elite ? ", elite " + af : "") + ").");
    }

    private static int alle(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel sl = p.level();
        opruimen(sl, p.blockPosition(), 64);
        List<String> namen = new ArrayList<>(Z6WezensRegistry.SOORTEN.keySet());
        int y = p.getBlockY() - 1;
        int x0 = p.getBlockX() - namen.size() * 3 / 2, z0 = p.getBlockZ() + 5;
        for (int x = x0 - 2; x <= x0 + namen.size() * 3 + 1; x++) {
            for (int z = z0 - 7; z <= z0 + 3; z++) {
                sl.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.SAND.defaultBlockState());
                for (int h = 1; h <= 5; h++) {
                    sl.setBlockAndUpdate(new BlockPos(x, y + h, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        int i = 0;
        for (String n : namen) {
            Entity e = maak(sl, n, new Vec3(x0 + i * 3 + 0.5, y + 1, z0 + 0.5), 180, level, false, EnumSet.noneOf(Affix.class));
            if (e instanceof net.minecraft.world.entity.Mob m) {
                m.setNoAi(true);
                if (e instanceof Z6Wezen w) {
                    w.zetHouding(0);   // (the Zandvlooi out of the sand, the Ruïnewachter awake: so you see them)
                }
            }
            i++;
        }
        return zeg(c, namen.size() + " wezens op een rij gezet (level " + level + "). Sla ze om de roze plof te zien.");
    }

    private static int tp(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String w = StringArgumentType.getString(c, "plek");
        BlockPos b = Z6WezensPlekken.tpPlekken().get(w);
        if (b == null) {
            return fout(c, "Onbekende plek: " + w + ". Kies uit " + Z6WezensPlekken.tpPlekken().keySet());
        }
        ServerLevel sl = rijk(c);
        if (Z6WezensPlekken.inKerker(b.getX(), b.getY(), b.getZ())) {
            Z6Bouw.sterrenwacht(sl);
        }
        sl.getChunk(b);
        if (!Z6WezensPlekken.inKerker(b.getX(), b.getY(), b.getZ())) {
            b = new BlockPos(b.getX(), Math.max(b.getY(), sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, b.getX(), b.getZ())), b.getZ());
        }
        p.teleportTo(sl, b.getX() + 0.5, b.getY() + 0.1, b.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
        return zeg(c, "Naar " + w + " (" + b.toShortString() + ")" + (Z6WezensPlekken.echt() ? "" : " (placeholder-plek)") + ".");
    }

    private static int baas(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String baas = StringArgumentType.getString(c, "baas");
        String id = "z6wezens:" + baas;
        Arena a = Arenas.van(id);
        if (a == null) {
            return fout(c, "Onbekende baas: " + baas + ". Kies uit " + Z6WezensRegistry.BAZEN.keySet());
        }
        ServerLevel sl = rijk(c);
        Z3Toegang.vergeet(p);
        if (baas.equals("wormelien")) {
            Z6Bazen.forceerWormelien = true;
        } else if (baas.equals("zoutkolos")) {
            Z6Bazen.forceerZoutkolos = true;
        } else {
            Z6Bouw.sterrenwacht(sl);
        }
        sl.getChunk(a.centrum());
        Arenas.reset(sl.getServer(), id);
        BlockPos d = a.deur();
        p.teleportTo(sl, d.getX() + 0.5, d.getY() + 0.1, d.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
        return zeg(c, baas + " staat zo klaar (volle Vads) in arena " + id + " rond " + a.centrum().toShortString() + " (straal " + a.straal() + ")."
                + (Z6WezensPlekken.echt() ? "" : " Placeholder-plek: zonder kaart eerst /guhs vadsrijk z6wezens bouw arenas."));
    }

    @SuppressWarnings("unchecked")
    private static <T> T dichtste(ServerPlayer p, Class<T> soort) {
        return (T) p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, p.getBoundingBox().inflate(64),
                e -> soort.isInstance(e) && !(e instanceof VadsrijkMob m && m.geplofd())).stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p))).orElse(null);
    }

    private static int fase(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Z6Baas z = dichtste(p, Z6Baas.class);
        if (z == null) {
            return fout(c, "Geen zone-6-baas in de buurt.");
        }
        int n = IntegerArgumentType.getInteger(c, "n");
        double[] f = z instanceof Astrolabiumwachter ? new double[] {0.75, 0.5, 0.25} : z instanceof Glimmertje ? new double[] {0.5} : new double[] {0.6, 0.3};
        while (z.fase() < n && !z.geplofd() && z.fase() - 1 < f.length) {
            double doel = z.maxVads() * f[z.fase() - 1] - 1;
            if (z.vads() > doel) {
                z.heal(doel - z.vads());
            }
            int voor = z.fase();
            z.krijgSchade(1, null);
            if (z.fase() == voor) {
                break;   // (immune: a mechanic waits)
            }
        }
        return zeg(c, "Fase nu " + z.fase());
    }

    private static int mechaniek(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel sl = p.level();
        String m = StringArgumentType.getString(c, "naam");
        switch (m) {
            case "duik", "duinspuug", "korst" -> {
                Wormelien w = dichtste(p, Wormelien.class);
                if (w == null) {
                    return fout(c, "Geen Koningin Wormelien in de buurt (/guhs vadsrijk z6wezens baas wormelien).");
                }
                if (m.equals("duik")) {
                    w.duik(sl, p);
                } else if (m.equals("duinspuug")) {
                    w.duinspuug(sl);
                } else {
                    w.korstOp(sl);
                }
            }
            case "zoutgolf", "kristallen", "zing" -> {
                Zoutkolos k = dichtste(p, Zoutkolos.class);
                if (k == null) {
                    return fout(c, "Geen Zoutkolos in de buurt (/guhs vadsrijk z6wezens baas zoutkolos).");
                }
                if (m.equals("zoutgolf")) {
                    k.zoutgolven(sl);
                } else if (m.equals("kristallen")) {
                    k.kristallenGooien(sl);
                } else {
                    k.zing(sl);
                }
            }
            case "ringzwaai", "lijn" -> {
                Astrolabiumwachter a = dichtste(p, Astrolabiumwachter.class);
                if (a == null) {
                    return fout(c, "Geen Astrolabiumwachter in de buurt (/guhs vadsrijk z6wezens baas astrolabiumwachter).");
                }
                if (m.equals("ringzwaai")) {
                    a.ringzwaai(sl);
                } else {
                    a.lijnUit(sl);
                }
            }
            case "doof" -> {
                Glimmertje g = dichtste(p, Glimmertje.class);
                if (g == null) {
                    return fout(c, "Geen Kootje Glimmertje in de buurt (/guhs vadsrijk z6wezens baas glimmertje).");
                }
                for (Z3Doel d : g.schalen()) {
                    g.doof(sl, d, 0);
                }
                return zeg(c, "Alle vuurschalen gedoofd (" + g.uit() + "/4). Rechtsklik ze 2x om ze weer aan te steken.");
            }
            case "hart" -> {
                Sterrenguh s = dichtste(p, Sterrenguh.class);
                if (s == null) {
                    return fout(c, "Geen Verzonken Sterrenguh in de buurt (/guhs vadsrijk z6wezens baas sterrenguh).");
                }
                s.hart(sl);
            }
            case "spiegel" -> {
                Zout.Spiegelslang s = dichtste(p, Zout.Spiegelslang.class);
                if (s == null) {
                    return fout(c, "Geen Spiegelslang in de buurt (/guhs vadsrijk z6wezens spawn spiegelslang).");
                }
                s.spiegel(sl);
            }
            case "slangduik" -> {
                Zout.Zoutslang s = dichtste(p, Zout.Zoutslang.class);
                if (s == null) {
                    return fout(c, "Geen Zoutslang in de buurt (/guhs vadsrijk z6wezens spawn zoutslang).");
                }
                s.duik(sl);
            }
            case "ringpot" -> {
                Ruine.MikaRingdrager r = dichtste(p, Ruine.MikaRingdrager.class);
                if (r == null) {
                    return fout(c, "Geen Mika-Ringdrager in de buurt (/guhs vadsrijk z6wezens spawn mika_ringdrager).");
                }
                r.pot(sl, p);
            }
            default -> {
                return fout(c, "Onbekend: " + m + ". Kies uit " + MECHANIEKEN);
            }
        }
        return zeg(c, "Mechaniek " + m + " gestart.");
    }

    private static void geef(ServerPlayer p, String type, int level, Zeldzaamheid z) {
        if (BasisTypes.van(type) == null) {
            return;
        }
        ItemStack st = Rollen.stack(Rollen.rol(BasisTypes.van(type), level, z, p.getRandom()));
        if (!p.getInventory().add(st)) {
            p.drop(st, false);
        }
    }

    private static int set(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String set = StringArgumentType.getString(c, "set");
        if (!Z6Sets.SETS.contains(set)) {
            return fout(c, "Onbekende set: " + set + ". Kies uit " + Z6Sets.SETS);
        }
        Zeldzaamheid z = set.equals(Z6Sets.DUINWANDELAAR) || set.equals(Z6Sets.DUINZIJDE) ? Zeldzaamheid.ZELDZAAM : Zeldzaamheid.EPISCH;
        for (String s : Z6Sets.stukkenVan(set)) {
            geef(p, s, level, z);
        }
        return zeg(c, "Set " + set + " gegeven (level " + level + "). Handschoenen/cape/armband draag je in je sieradenvakken.");
    }

    private static int lijst(CommandContext<CommandSourceStack> c, List<String> types, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        for (String s : types) {
            geef(p, s, level, Zeldzaamheid.ZELDZAAM);
        }
        return zeg(c, types.size() + " stuks gegeven (level " + level + "): " + types);
    }

    private static int loot(CommandContext<CommandSourceStack> c, int level, boolean elite) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String soort = StringArgumentType.getString(c, "soort");
        LootTabel t = LootTabellen.van("z6wezens:" + soort + (elite ? "_elite" : ""));
        if (t == null) {
            return fout(c, "Geen tabel voor " + soort);
        }
        LootTabel.Resultaat r = t.rol(level, p.getRandom());
        LootBundel.laatVallen(p, p.position().add(p.getLookAngle().scale(2)).add(0, 0.5, 0), r, elite ? 1 : 0);
        return zeg(c, "Loot van " + soort + " (level " + level + "): " + r.items().size() + " items, " + r.munten() + " brons.");
    }

    private static int bultguh(CommandContext<CommandSourceStack> c, String vacht) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Bultguh.geef(p, vacht);
        if (vacht.equals("sterrennacht")) {
            nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.geef(p, "vacht:bultguh:sterrennacht");
            nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.kies(p, RijdierSoort.BULTGUH, nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.As.VACHT, vacht);
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        Rijdieren.kies(p, RijdierSoort.BULTGUH);
        Rijdieren.roep(p, RijdierSoort.BULTGUH);
        return zeg(c, "De Bultguh (" + vacht + ") is van jou en je zit erop. Een tweede speler: rechtsklik de Bultguh terwijl jij erop zit.");
    }

    /** (tests) */
    static VadsrijkBaas baasIn(ServerLevel sl, String arena) {
        return Arenas.baas(sl, arena);
    }
}
