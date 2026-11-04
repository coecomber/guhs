package nl.juiced.guhs.feature.vadsrijk.zone2.wezens;

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
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Loot;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Sets;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Critter;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Z2Wezen;

/**
 * Dev command {@code /guhs vadsrijk z2wezens ...} (op level 2):
 * <pre>
 *   spawn &lt;soort&gt; [level] [elite]     a creature (or critter) 4 blocks in front of you
 *   affix &lt;soort&gt; &lt;affix&gt; [level]    an elite with exactly this affix
 *   alle [level]                      every creature + critter in a row on a fresh stone platform (the AutoCheck line-up)
 *   bevroren [ticks]                  freeze yourself (test the status)
 *   set kristal|mika [level]          the full set (rare) in your inventory
 *   sieraden [level]                  the 3 jewellery pieces
 *   loot &lt;soort&gt; [level] [elite]      rolls that creature's table for you
 *   opruimen                          removes all zone-2 creatures and critters within 64 blocks
 *   spawners                          how many spawners, max alive per area
 * </pre>
 */
public final class Z2WezensCommando {
    private Z2WezensCommando() {
    }

    private static List<String> alleNamen() {
        List<String> l = new ArrayList<>(Z2WezensRegistry.SOORTEN.keySet());
        l.addAll(Z2WezensRegistry.CRITTERS.keySet());
        return l;
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("z2wezens").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(alleNamen(), b))
                        .executes(c -> spawn(c, 0, false, null))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 40)).executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), false, null))
                                .then(Commands.argument("elite", BoolArgumentType.bool())
                                        .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite"), null))))))
                .then(Commands.literal("affix").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z2WezensRegistry.SOORTEN.keySet(), b))
                        .then(Commands.argument("affix", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("gloeiend", "snel", "splitsend", "schildend"), b))
                                .executes(c -> spawn(c, 0, true, StringArgumentType.getString(c, "affix")))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 40))
                                        .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), true, StringArgumentType.getString(c, "affix")))))))
                .then(Commands.literal("alle").executes(c -> alle(c, 25))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 40)).executes(c -> alle(c, IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("bevroren").executes(c -> bevroren(c, 100))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 2000)).executes(c -> bevroren(c, IntegerArgumentType.getInteger(c, "ticks")))))
                .then(Commands.literal("set").then(Commands.argument("set", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of(Z2Sets.KRISTAL, Z2Sets.MIKA), b))
                        .executes(c -> set(c, 25))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 40)).executes(c -> set(c, IntegerArgumentType.getInteger(c, "level"))))))
                .then(Commands.literal("sieraden").executes(c -> sieraden(c, 25))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 40)).executes(c -> sieraden(c, IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("loot").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z2WezensRegistry.SOORTEN.keySet(), b))
                        .executes(c -> loot(c, 25, false))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 40)).executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), false))
                                .then(Commands.argument("elite", BoolArgumentType.bool())
                                        .executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite")))))))
                .then(Commands.literal("opruimen").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int n = opruimen(p.level(), p.blockPosition(), 64);
                    c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z2wezens.cmd.opgeruimd", n), false);
                    return n;
                }))
                .then(Commands.literal("spawners").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z2wezens.cmd.spawners", Z2WezensSpawners.ALLE.size(),
                            String.valueOf(Z2WezensSpawners.MAX_PER_GEBIED)), false);
                    return 1;
                }))));
    }

    public static int opruimen(ServerLevel sl, BlockPos pos, int r) {
        List<Entity> l = new ArrayList<>(sl.getEntitiesOfClass(Z2Wezen.class, new AABB(pos).inflate(r)));
        l.addAll(sl.getEntitiesOfClass(Critter.class, new AABB(pos).inflate(r)));
        l.forEach(Entity::discard);
        return l.size();
    }

    /** Makes one creature or critter at a spot (level, elite with these affixes). */
    public static Entity maak(ServerLevel sl, String soort, Vec3 pos, float yaw, int level, boolean elite, EnumSet<Affix> af) {
        var monster = Z2WezensRegistry.SOORTEN.get(soort);
        var critter = Z2WezensRegistry.CRITTERS.get(soort);
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
        int lvl = level > 0 ? level : 25;
        Vec3 voor = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(4));
        Entity e = maak(p.level(), soort, new Vec3(voor.x, p.getY(), voor.z), p.getYRot() + 180, lvl, elite, af);
        if (e == null) {
            c.getSource().sendFailure(Component.translatable("vadsrijk.z2wezens.cmd.onbekend", soort));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z2wezens.cmd.gespawnd", e.getType().getDescription(), lvl, elite ? " elite " + af : ""),
                false);
        return 1;
    }

    /** The line-up: a snow platform at your feet level, 4 blocks ahead, every kind 3 blocks apart (no AI, hidden ones shown). */
    private static int alle(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel sl = p.level();
        opruimen(sl, p.blockPosition(), 64);
        List<String> namen = alleNamen();
        int y = p.getBlockY() - 1;
        int x0 = p.getBlockX() - namen.size() * 3 / 2, z0 = p.getBlockZ() + 4;
        // a solid platform (also under the hidden ones: they need ground to hide in)
        for (int x = x0 - 2; x <= x0 + namen.size() * 3 + 1; x++) {
            for (int z = z0 - 6; z <= z0 + 3; z++) {
                sl.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.SNOW_BLOCK.defaultBlockState());
                for (int h = 1; h <= 4; h++) {
                    sl.setBlockAndUpdate(new BlockPos(x, y + h, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        int i = 0;
        for (String n : namen) {
            Entity e = maak(sl, n, new Vec3(x0 + i * 3 + 0.5, y + 1, z0 + 0.5), 180, level, false, EnumSet.noneOf(Affix.class));
            if (e instanceof Z2Wezen w) {
                w.setNoAi(true);
                w.zetHouding(0);   // show the hidden ones too
            } else if (e instanceof net.minecraft.world.entity.Mob m) {
                m.setNoAi(true);
            }
            i++;
        }
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z2wezens.cmd.alle", namen.size()), false);
        return namen.size();
    }

    private static int bevroren(CommandContext<CommandSourceStack> c, int ticks) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Bevroren.geef(p, ticks, null);
        return 1;
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
        for (String s : Z2Sets.MIKA.equals(set) ? Z2Sets.MIKASET : Z2Sets.KRISTALSET) {
            geef(p, s, level);
        }
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z2wezens.cmd.set", set, level), false);
        return 1;
    }

    private static int sieraden(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Z2Loot.STUKKEN.keySet().stream().filter(Z2Loot::sieraad).forEach(s -> geef(p, s, level));
        return 1;
    }

    private static int loot(CommandContext<CommandSourceStack> c, int level, boolean elite) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String soort = StringArgumentType.getString(c, "soort");
        LootTabel t = LootTabellen.van("z2wezens:" + soort + (elite ? "_elite" : ""));
        if (t == null) {
            c.getSource().sendFailure(Component.translatable("vadsrijk.z2wezens.cmd.onbekend", soort));
            return 0;
        }
        LootTabel.Resultaat r = t.rol(level, p.getRandom());
        LootBundel.laatVallen(p, p.position().add(p.getLookAngle().scale(2)).add(0, 0.5, 0), r, elite ? 1 : 0);
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.z2wezens.cmd.loot", soort, level, r.items().size(), r.munten()), false);
        return 1;
    }
}
