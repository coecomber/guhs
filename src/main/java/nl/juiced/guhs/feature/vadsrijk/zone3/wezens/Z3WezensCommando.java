package nl.juiced.guhs.feature.vadsrijk.zone3.wezens;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.loot.Z3Loot;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.loot.Z3Sets;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Z3Critter;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Z3Wezen;

/**
 * Dev command {@code /guhs vadsrijk z3wezens ...} (op level 2):
 * <pre>
 *   spawn &lt;soort&gt; [level] [elite]     a creature, add or critter 4 blocks in front of you
 *   affix &lt;soort&gt; &lt;affix&gt; [level]    an elite with exactly this affix
 *   alle [level]                      every creature + critter in a row on a fresh platform (the AutoCheck line-up)
 *   contract &lt;id&gt;                     a contract elite for you (and your party) in front of you (not at its lair)
 *   jacht &lt;id&gt;                        what the bounty board does: Z3Contracten.spawnElite (at its lair)
 *   set bloesem|schors|wortel [level] the full set (rare) in your inventory
 *   sieraden [level]                  the 3 jewellery pieces
 *   loot &lt;soort&gt; [level] [elite]      rolls that creature's table for you
 *   licht                             is your spot lit (the soft lantern rule)?
 *   opruimen                          removes all zone-3 creatures and critters within 64 blocks
 *   spawners                          how many spawners, max alive per layer
 * </pre>
 */
public final class Z3WezensCommando {
    private Z3WezensCommando() {
    }

    private static List<String> alleNamen() {
        List<String> l = new ArrayList<>(Z3WezensRegistry.SOORTEN.keySet());
        l.addAll(Z3WezensRegistry.ADDS.keySet());
        l.addAll(Z3WezensRegistry.CRITTERS.keySet());
        return l;
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("z3wezens").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(alleNamen(), b))
                        .executes(c -> spawn(c, 0, false, null))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 60)).executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), false, null))
                                .then(Commands.argument("elite", BoolArgumentType.bool())
                                        .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite"), null))))))
                .then(Commands.literal("affix").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z3WezensRegistry.SOORTEN.keySet(), b))
                        .then(Commands.argument("affix", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("gloeiend", "snel", "splitsend", "schildend"), b))
                                .executes(c -> spawn(c, 0, true, StringArgumentType.getString(c, "affix")))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 60))
                                        .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), true, StringArgumentType.getString(c, "affix")))))))
                .then(Commands.literal("alle").executes(c -> alle(c, 40))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 60)).executes(c -> alle(c, IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("contract").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z3Contracten.IDS, b)).executes(c -> contract(c, false))))
                .then(Commands.literal("jacht").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z3Contracten.IDS, b)).executes(c -> contract(c, true))))
                .then(Commands.literal("set").then(Commands.argument("set", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z3Sets.SETS, b))
                        .executes(c -> set(c, 40))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 60)).executes(c -> set(c, IntegerArgumentType.getInteger(c, "level"))))))
                .then(Commands.literal("sieraden").executes(c -> sieraden(c, 40))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 60)).executes(c -> sieraden(c, IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("loot").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z3WezensRegistry.SOORTEN.keySet(), b))
                        .executes(c -> loot(c, 40, false))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 60)).executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), false))
                                .then(Commands.argument("elite", BoolArgumentType.bool())
                                        .executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite")))))))
                .then(Commands.literal("licht").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean licht = Lantaarnlicht.inLicht(p.level(), p.position().add(0, 1, 0));
                    boolean duister = Z3WezensPlekken.duister(p.getX(), p.getY(), p.getZ());
                    c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z3wezens.cmd.licht", licht ? "ja" : "nee", duister ? "ja" : "nee",
                            Lantaarnlicht.draagtLicht(p) ? "ja" : "nee"), false);
                    return 1;
                }))
                .then(Commands.literal("opruimen").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int n = opruimen(p.level(), p.blockPosition(), 64);
                    c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z3wezens.cmd.opgeruimd", n), false);
                    return n;
                }))
                .then(Commands.literal("spawners").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z3wezens.cmd.spawners", Z3WezensSpawners.ALLE.size(),
                            String.valueOf(Z3WezensSpawners.MAX_PER_GEBIED)), false);
                    return 1;
                }))));
    }

    public static int opruimen(ServerLevel sl, BlockPos pos, int r) {
        List<Entity> l = new ArrayList<>(sl.getEntitiesOfClass(Z3Wezen.class, new AABB(pos).inflate(r)));
        l.addAll(sl.getEntitiesOfClass(Z3Critter.class, new AABB(pos).inflate(r)));
        l.forEach(Entity::discard);
        return l.size();
    }

    /** Makes one creature, add or critter at a spot (level, elite with these affixes). */
    public static Entity maak(ServerLevel sl, String soort, Vec3 pos, float yaw, int level, boolean elite, EnumSet<Affix> af) {
        var monster = Z3WezensRegistry.SOORTEN.containsKey(soort) ? Z3WezensRegistry.SOORTEN.get(soort) : Z3WezensRegistry.ADDS.get(soort);
        var critter = Z3WezensRegistry.CRITTERS.get(soort);
        EntityType<?> type = monster != null ? monster.get() : critter != null ? critter.get() : null;
        if (type == null) {
            return null;
        }
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

    private static int spawn(CommandContext<CommandSourceStack> c, int level, boolean elite, String affix) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String soort = StringArgumentType.getString(c, "soort");
        EnumSet<Affix> af = EnumSet.noneOf(Affix.class);
        if (affix != null) {
            try {
                af.add(Affix.valueOf(affix.toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                c.getSource().sendFailure(Component.literal(affix + "?"));
                return 0;
            }
        } else if (elite) {
            Affix[] alle = Affix.values();
            af.add(alle[p.getRandom().nextInt(alle.length)]);
        }
        int lvl = level > 0 ? level : 40;
        Vec3 voor = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(4));
        Entity e = maak(p.level(), soort, new Vec3(voor.x, p.getY(), voor.z), p.getYRot() + 180, lvl, elite, af);
        if (e == null) {
            c.getSource().sendFailure(Component.translatable("vadsrijk.z3wezens.cmd.onbekend", soort));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z3wezens.cmd.gespawnd", e.getType().getDescription(), lvl, elite ? " elite " + af : ""),
                false);
        return 1;
    }

    private static int contract(CommandContext<CommandSourceStack> c, boolean bijHol) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(c, "id");
        Z3Contracten.Doelwit d = Z3Contracten.DOELWITTEN.get(id);
        if (d == null) {
            c.getSource().sendFailure(Component.translatable("vadsrijk.z3wezens.cmd.onbekend", id));
            return 0;
        }
        Z3Wezen w;
        if (bijHol) {
            w = Z3Contracten.spawnElite(p, id);
        } else {
            Vec3 voor = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(6));
            w = Z3Contracten.maak(p.level(), d, BlockPos.containing(voor.x, p.getY(), voor.z), List.of(p.getUUID()));
        }
        if (w == null) {
            c.getSource().sendFailure(Component.translatable("vadsrijk.z3wezens.cmd.onbekend", id));
            return 0;
        }
        BlockPos b = w.blockPosition();
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z3wezens.cmd.contract", Component.translatable("vadsrijk.z3wezens.contract." + id),
                b.getX(), b.getY(), b.getZ()), false);
        return 1;
    }

    /** The line-up: a platform at your feet level, 4 blocks ahead, every kind 3 blocks apart (no AI, hidden ones shown). */
    private static int alle(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel sl = p.level();
        opruimen(sl, p.blockPosition(), 64);
        List<String> namen = alleNamen();
        int y = p.getBlockY() - 1;
        int x0 = p.getBlockX() - namen.size() * 3 / 2, z0 = p.getBlockZ() + 5;
        for (int x = x0 - 2; x <= x0 + namen.size() * 3 + 1; x++) {
            for (int z = z0 - 7; z <= z0 + 3; z++) {
                sl.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.MOSS_BLOCK.defaultBlockState());
                for (int h = 1; h <= 5; h++) {
                    sl.setBlockAndUpdate(new BlockPos(x, y + h, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        int i = 0;
        for (String n : namen) {
            Entity e = maak(sl, n, new Vec3(x0 + i * 3 + 0.5, y + 1, z0 + 0.5), 180, level, false, EnumSet.noneOf(Affix.class));
            if (e instanceof Z3Wezen w) {
                w.setNoAi(true);
                w.zetHouding(0);   // show the hidden ones too
            } else if (e instanceof net.minecraft.world.entity.Mob m) {
                m.setNoAi(true);
            }
            i++;
        }
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z3wezens.cmd.alle", namen.size()), false);
        return namen.size();
    }

    private static void geef(ServerPlayer p, String type, int level) {
        ItemStack st = Rollen.stack(Rollen.rol(BasisTypes.van(type), level, Zeldzaamheid.ZELDZAAM, p.getRandom()));
        if (!p.getInventory().add(st)) {
            p.drop(st, false);
        }
    }

    private static int set(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String set = StringArgumentType.getString(c, "set");
        if (!Z3Sets.SETS.contains(set)) {
            c.getSource().sendFailure(Component.translatable("vadsrijk.z3wezens.cmd.onbekend", set));
            return 0;
        }
        for (String s : Z3Sets.stukkenVan(set)) {
            geef(p, s, level);
        }
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z3wezens.cmd.set", set, level), false);
        return 1;
    }

    private static int sieraden(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Z3Loot.STUKKEN.keySet().stream().filter(Z3Loot::sieraad).forEach(s -> geef(p, s, level));
        return 1;
    }

    private static int loot(CommandContext<CommandSourceStack> c, int level, boolean elite) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String soort = StringArgumentType.getString(c, "soort");
        LootTabel t = LootTabellen.van("z3wezens:" + soort + (elite ? "_elite" : ""));
        if (t == null) {
            c.getSource().sendFailure(Component.translatable("vadsrijk.z3wezens.cmd.onbekend", soort));
            return 0;
        }
        LootTabel.Resultaat r = t.rol(level, p.getRandom());
        LootBundel.laatVallen(p, p.position().add(p.getLookAngle().scale(2)).add(0, 0.5, 0), r, elite ? 1 : 0);
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z3wezens.cmd.loot", soort, level, r.items().size(), r.munten()), false);
        return 1;
    }
}
