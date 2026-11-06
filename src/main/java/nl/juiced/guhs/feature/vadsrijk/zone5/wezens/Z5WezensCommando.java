package nl.juiced.guhs.feature.vadsrijk.zone5.wezens;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Donderbuik;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Gorgonzola;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Z5Bazen;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Z5BazenLoot;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Zilverstaart;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.loot.Z5Loot;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob.Nest;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob.Proef;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob.Z5Wezen;

/**
 * Dev command {@code /guhs vadsrijk z5wezens ...} (op level 2):
 * <pre>
 *   spawn &lt;soort&gt; [level] [elite]   a creature 4 blocks in front of you
 *   alle [level]                      every creature in a row on a platform (the AutoCheck line-up)
 *   sparren                           a Sparringridder in front of you, bout started
 *   tp &lt;plek&gt;                         proefarena|buikgangen|ketelkamer|schuilnest|zuidvin|noordvin|staartvlakte|donderbuik|wolkwaldroom
 *   bouw buik|nest|vloeren|alles      the stand-in rooms/floors (only into air)
 *   baas &lt;id&gt;                         to the boss's door + arena reset (zilverstaart: the story start Z5Bazen.start)
 *   mechaniek vermom|ontsnap|kleppen|zuig   triggers a mechanic of the nearest zone-5 boss
 *   set [level] | wapens [level]      the Wolkwacht set + jewellery | the 6 weapons + named boss items
 *   loot &lt;soort|baas&gt; [level] [elite]  rolls a table for you
 *   plekken | spawners | opruimen
 * </pre>
 */
public final class Z5WezensCommando {
    public static final List<String> PLEKKEN = List.of("proefarena", "buikgangen", "ketelkamer", "schuilnest", "zuidvin", "noordvin", "staartvlakte",
            "donderbuik", "wolkwaldroom");

    private Z5WezensCommando() {
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("z5wezens").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z5WezensRegistry.SOORTEN.keySet(), b))
                        .executes(c -> spawn(c, 0, false))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 70)).executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), false))
                                .then(Commands.argument("elite", BoolArgumentType.bool())
                                        .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite")))))))
                .then(Commands.literal("alle").executes(c -> alle(c, 58))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 70)).executes(c -> alle(c, IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("sparren").executes(Z5WezensCommando::sparren))
                .then(Commands.literal("tp").then(Commands.argument("plek", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(PLEKKEN, b)).executes(Z5WezensCommando::tp)))
                .then(Commands.literal("bouw").then(Commands.argument("wat", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("buik", "nest", "vloeren", "alles"), b)).executes(Z5WezensCommando::bouw)))
                .then(Commands.literal("baas").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z5WezensRegistry.BAZEN.keySet(), b)).executes(Z5WezensCommando::baas)))
                .then(Commands.literal("mechaniek").then(Commands.argument("wat", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("vermom", "ontsnap", "kleppen", "zuig"), b)).executes(Z5WezensCommando::mechaniek)))
                .then(Commands.literal("set").executes(c -> set(c, 58))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 70)).executes(c -> set(c, IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("wapens").executes(c -> wapens(c, 58))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 70)).executes(c -> wapens(c, IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("loot").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> {
                            List<String> l = new ArrayList<>(Z5WezensRegistry.SOORTEN.keySet());
                            l.addAll(Z5WezensRegistry.BAZEN.keySet());
                            return SharedSuggestionProvider.suggest(l, b);
                        })
                        .executes(c -> loot(c, 58, false))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 70)).executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), false))
                                .then(Commands.argument("elite", BoolArgumentType.bool())
                                        .executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite")))))))
                .then(Commands.literal("plekken").executes(c -> {
                    StringBuilder s = new StringBuilder("Zone-5 gevechtsplekken (placeholders tot t-z5wereld ze levert):");
                    for (Map.Entry<String, BlockPos> e : Z5WezensPlekken.POI.entrySet()) {
                        s.append("\n ").append(e.getKey()).append(": ").append(e.getValue().toShortString());
                    }
                    return zeg(c, s.toString());
                }))
                .then(Commands.literal("spawners").executes(c -> zeg(c, Z5WezensSpawners.ALLE.size() + " zone-5-spawners, samen max " + Z5WezensSpawners.totaalMax()
                        + " levend; per plek " + Z5WezensSpawners.MAX_PER_PLEK)))
                .then(Commands.literal("opruimen").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int n = opruimen(p.level(), p.blockPosition(), 64);
                    return zeg(c, n + " zone-5-wezens opgeruimd.");
                }))));
    }

    public static int opruimen(ServerLevel sl, BlockPos pos, int r) {
        List<Entity> l = new ArrayList<>(sl.getEntitiesOfClass(Z5Wezen.class, new AABB(pos).inflate(r)));
        l.forEach(Entity::discard);
        return l.size();
    }

    /** Makes one creature at a spot. */
    @Nullable
    public static Entity maak(ServerLevel sl, String soort, Vec3 pos, float yaw, int level, boolean elite) {
        var holder = Z5WezensRegistry.SOORTEN.get(soort);
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
            EnumSet<Affix> af = EnumSet.noneOf(Affix.class);
            if (elite) {
                Affix[] alle = Affix.values();
                af.add(alle[sl.getRandom().nextInt(alle.length)]);
            }
            m.stelIn(level, null, elite, af);
        }
        sl.addFreshEntity(e);
        return e;
    }

    private static Vec3 voor(ServerPlayer p, double afstand) {
        Vec3 v = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(afstand));
        return new Vec3(v.x, p.getY(), v.z);
    }

    private static int spawn(CommandContext<CommandSourceStack> c, int level, boolean elite) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String soort = StringArgumentType.getString(c, "soort");
        int lvl = level > 0 ? level : 58;
        Entity e = maak(p.level(), soort, voor(p, 4), p.getYRot() + 180, lvl, elite);
        if (e == null) {
            c.getSource().sendFailure(Component.literal("Onbekend: " + soort));
            return 0;
        }
        return zeg(c, soort + " gemaakt (level " + lvl + (elite ? ", elite" : "") + ").");
    }

    /** The line-up: a platform (only into air) 5 ahead, every kind 3 apart, no AI, the hidden ones shown. */
    private static int alle(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel sl = p.level();
        opruimen(sl, p.blockPosition(), 64);
        List<String> namen = new ArrayList<>(Z5WezensRegistry.SOORTEN.keySet());
        int y = p.getBlockY() - 1;
        int x0 = p.getBlockX() - namen.size() * 3 / 2, z0 = p.getBlockZ() + 5;
        for (int x = x0 - 2; x <= x0 + namen.size() * 3 + 1; x++) {
            for (int z = z0 - 7; z <= z0 + 3; z++) {
                BlockPos b = new BlockPos(x, y, z);
                if (sl.getBlockState(b).isAir()) {
                    sl.setBlockAndUpdate(b, Blocks.WHITE_WOOL.defaultBlockState());
                }
            }
        }
        int i = 0;
        for (String n : namen) {
            Entity e = maak(sl, n, new Vec3(x0 + i * 3 + 0.5, y + 1, z0 + 0.5), 180, level, false);
            if (e instanceof Z5Wezen w) {
                w.setNoAi(true);
                w.zetHouding(0);
            }
            i++;
        }
        return zeg(c, namen.size() + " zone-5-wezens op een rij.");
    }

    private static int sparren(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Entity e = maak(p.level(), "sparringridder", voor(p, 3), p.getYRot() + 180, 56, false);
        if (e instanceof Proef.Sparringridder r) {
            r.begin(p.level(), p);
        }
        return zeg(c, "Sparringridder klaar: ontwijk de rode strook, sla niet op het roze schild.");
    }

    @Nullable
    static BlockPos plek(String naam) {
        return switch (naam) {
            case "proefarena" -> Z5WezensPlekken.PROEFARENA;
            case "buikgangen" -> Z5WezensPlekken.BUIK_AANKOMST;
            case "ketelkamer" -> Z5WezensPlekken.KETEL_DEUR;
            case "schuilnest" -> Z5WezensPlekken.NEST_AANKOMST;
            case "zuidvin" -> Z5WezensPlekken.ZUIDVIN;
            case "noordvin" -> Z5WezensPlekken.NOORDVIN;
            case "staartvlakte" -> Z5WezensPlekken.STAARTVLAKTE;
            case "donderbuik" -> Z5WezensPlekken.DONDERBUIK_DEUR;
            case "wolkwaldroom" -> Z5WezensPlekken.WOLKWALDROOM_DEUR;
            default -> null;
        };
    }

    private static void naar(ServerPlayer p, BlockPos b) {
        ServerLevel sl = nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.level(p.level().getServer());
        sl.getChunk(b);
        p.teleportTo(sl, b.getX() + 0.5, b.getY() + 0.1, b.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
    }

    private static int tp(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String n = StringArgumentType.getString(c, "plek");
        BlockPos b = plek(n);
        if (b == null) {
            c.getSource().sendFailure(Component.literal("Onbekende plek: " + n));
            return 0;
        }
        naar(p, b);
        return zeg(c, "Naar " + n + " (" + b.toShortString() + "). Geen vloer? /guhs vadsrijk z5wezens bouw alles");
    }

    private static int bouw(CommandContext<CommandSourceStack> c) {
        ServerLevel sl = nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.level(c.getSource().getServer());
        String w = StringArgumentType.getString(c, "wat");
        int n = 0;
        if (w.equals("buik") || w.equals("alles")) {
            n += Z5Bouw.buikgangen(sl);
        }
        if (w.equals("nest") || w.equals("alles")) {
            n += Z5Bouw.schuilnest(sl);
        }
        if (w.equals("vloeren") || w.equals("alles")) {
            n += Z5Bouw.vloeren(sl);
        }
        return zeg(c, n + " blokken geplaatst (alleen in lucht).");
    }

    private static int baas(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(c, "id");
        switch (id) {
            case "zilverstaart" -> {
                Arenas.reset(c.getSource().getServer(), Z5Bazen.ARENA_SPION);
                naar(p, Z5WezensPlekken.WOLKWALDROOM_DEUR);
                Z5Bazen.start(p, "spion");
            }
            case "gorgonzola" -> {
                Arenas.reset(c.getSource().getServer(), Z5Bazen.ARENA_GORGONZOLA);
                naar(p, Z5WezensPlekken.KETEL_DEUR);
            }
            case "donderbuik" -> {
                Arenas.reset(c.getSource().getServer(), Z5Bazen.ARENA_DONDERBUIK);
                naar(p, Z5WezensPlekken.DONDERBUIK_DEUR);
            }
            default -> {
                c.getSource().sendFailure(Component.literal("Onbekende baas: " + id));
                return 0;
            }
        }
        return zeg(c, id + ": arena gereset, je staat bij de deur. Loop de arena in.");
    }

    @Nullable
    private static VadsrijkBaas dichtste(ServerPlayer p) {
        return p.level().getEntitiesOfClass(VadsrijkBaas.class, p.getBoundingBox().inflate(64), b -> !b.geplofd()).stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p))).orElse(null);
    }

    private static int mechaniek(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String w = StringArgumentType.getString(c, "wat");
        VadsrijkBaas b = dichtste(p);
        ServerLevel sl = p.level();
        if (b instanceof Zilverstaart z && w.equals("vermom")) {
            z.vermom(sl);
        } else if (b instanceof Zilverstaart z && w.equals("ontsnap")) {
            z.ontsnap(sl);
        } else if (b instanceof Gorgonzola g && w.equals("kleppen")) {
            for (int i = 0; i < Gorgonzola.KLEPPEN; i++) {
                g.sluit(sl, i);
            }
        } else if (b instanceof Donderbuik d && w.equals("zuig")) {
            d.zuigwind(sl);
        } else {
            c.getSource().sendFailure(Component.literal("Geen passende zone-5-baas binnen 64 blokken voor '" + w + "'."));
            return 0;
        }
        return zeg(c, w + " gestart.");
    }

    private static void geef(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) {
            p.drop(st, false);
        }
    }

    private static int set(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        for (String s : Z5Loot.STUKKEN.keySet()) {
            geef(p, Rollen.stack(Rollen.rol(BasisTypes.van(s), level, Zeldzaamheid.ZELDZAAM, p.getRandom())));
        }
        return zeg(c, "Wolkwachtset (" + Z5Loot.WOLKWACHT.size() + " stukken) + sieraden gegeven, level " + level + ".");
    }

    private static int wapens(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        for (String w : Z5Loot.WAPENS) {
            geef(p, Rollen.stack(Rollen.rol(BasisTypes.van(w), level, Zeldzaamheid.EPISCH, p.getRandom())));
        }
        for (Z5BazenLoot.Benoemd b : Z5BazenLoot.BENOEMD) {
            geef(p, Z5BazenLoot.maak(b, level, p.getRandom()));
        }
        for (String b : Z5BazenLoot.BAZEN) {
            geef(p, Z5BazenLoot.trofee(b));
        }
        return zeg(c, "6 klassewapens + " + Z5BazenLoot.BENOEMD.size() + " benoemde baasstukken + 3 trofeeën gegeven.");
    }

    private static int loot(CommandContext<CommandSourceStack> c, int level, boolean elite) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String soort = StringArgumentType.getString(c, "soort");
        LootTabel.Resultaat r;
        if (Z5WezensRegistry.BAZEN.containsKey(soort)) {
            r = Z5BazenLoot.kist(soort, p, level, p.getRandom());
        } else {
            LootTabel t = LootTabellen.van("z5wezens:" + soort + (elite ? "_elite" : ""));
            if (t == null) {
                c.getSource().sendFailure(Component.literal("Onbekend: " + soort));
                return 0;
            }
            r = t.rol(level, p.getRandom());
        }
        LootBundel.laatVallen(p, p.position().add(p.getLookAngle().scale(2)).add(0, 0.5, 0), r, elite ? 1 : 0);
        return zeg(c, "Loot van " + soort + " (level " + level + "): " + r.items().size() + " stukken, " + r.munten() + " brons.");
    }

    /** (tests) unmask helper. */
    static void ontmasker(ServerLevel sl, Nest.VermomdeMika m) {
        m.ontmasker(sl);
    }

    @SuppressWarnings("unused")
    private static boolean isDoel(Entity e) {
        return e instanceof Z3Doel;
    }
}
