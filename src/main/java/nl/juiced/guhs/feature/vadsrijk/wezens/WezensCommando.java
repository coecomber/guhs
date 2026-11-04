package nl.juiced.guhs.feature.vadsrijk.wezens;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
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
import nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht;
import nl.juiced.guhs.feature.vadsrijk.wezens.loot.SetBonussen;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.RijkWezen;

/**
 * Dev command {@code /guhs vadsrijk wezens ...} (op level 2):
 * <pre>
 *   spawn &lt;soort&gt; [level] [elite]   a creature in front of you (elite = random affixes)
 *   affix &lt;soort&gt; &lt;affix&gt; [level]  an elite with exactly this affix
 *   draak [straal]                   Vahoegnir flies a circle over you (and roars)
 *   overvlucht                       a straight, low flyover past you (the opening-cutscene look)
 *   kampklodder                      the Kampklodder at your feet (moves an existing one)
 *   set klodder|zwam [level]         the full set (rare) in your inventory
 *   loot &lt;soort&gt; [level] [elite]    rolls that creature's table for you
 *   opruimen                         removes all creatures within 64 blocks
 *   spawners                         how many spawners there are
 * </pre>
 */
public final class WezensCommando {
    private WezensCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("wezens").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(WezensRegistry.SOORTEN.keySet(), b))
                        .executes(c -> spawn(c, 0, false, null))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 20)).executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), false, null))
                                .then(Commands.argument("elite", BoolArgumentType.bool())
                                        .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite"), null))))))
                .then(Commands.literal("affix").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(WezensRegistry.SOORTEN.keySet(), b))
                        .then(Commands.argument("affix", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("gloeiend", "snel", "splitsend", "schildend"), b))
                                .executes(c -> spawn(c, 0, true, StringArgumentType.getString(c, "affix")))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 20))
                                        .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), true, StringArgumentType.getString(c, "affix")))))))
                .then(Commands.literal("draak").executes(c -> draak(c, 60))
                        .then(Commands.argument("straal", IntegerArgumentType.integer(20, 300)).executes(c -> draak(c, IntegerArgumentType.getInteger(c, "straal")))))
                .then(Commands.literal("overvlucht").executes(WezensCommando::overvlucht))
                .then(Commands.literal("kampklodder").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Kampklodder.plaats(p.level(), p.blockPosition());
                    return 1;
                }))
                .then(Commands.literal("set").then(Commands.argument("set", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("klodder", "zwam"), b))
                        .executes(c -> set(c, 5))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 20)).executes(c -> set(c, IntegerArgumentType.getInteger(c, "level"))))))
                .then(Commands.literal("loot").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(WezensRegistry.SOORTEN.keySet(), b))
                        .executes(c -> loot(c, 5, false))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 20)).executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), false))
                                .then(Commands.argument("elite", BoolArgumentType.bool())
                                        .executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite")))))))
                .then(Commands.literal("opruimen").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    List<RijkWezen> l = p.level().getEntitiesOfClass(RijkWezen.class, new AABB(p.blockPosition()).inflate(64));
                    l.forEach(net.minecraft.world.entity.Entity::discard);
                    c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.wezens.cmd.opgeruimd", l.size()), false);
                    return l.size();
                }))
                .then(Commands.literal("spawners").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.wezens.cmd.spawners", WezensSpawners.aantalOppervlak(),
                            WezensSpawners.aantalVloer()), false);
                    // live numbers (fix round 1): awake spawners, alive per area, plan per area, entities in the level
                    ServerLevel vr = nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.level(c.getSource().getServer());
                    int alles = 0, wezens = 0;
                    if (vr != null) {
                        for (var e : vr.getAllEntities()) {
                            alles++;
                            if (e instanceof RijkWezen) {
                                wezens++;
                            }
                        }
                    }
                    String tekst = "Wakker: " + nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners.laatstActief + " spawners, levend " + nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners.laatstLevend
                            + " " + nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners.LAATST_PER_GEBIED + " | plan " + WezensSpawners.PLAN + " | entiteiten " + alles
                            + " (wezens " + wezens + ")";
                    c.getSource().sendSuccess(() -> Component.literal(tekst), false);
                    com.mojang.logging.LogUtils.getLogger().info("[leven] {}", tekst);
                    return 1;
                }))));
    }

    private static int spawn(CommandContext<CommandSourceStack> c, int level, boolean elite, String affix) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String soort = StringArgumentType.getString(c, "soort");
        var type = WezensRegistry.SOORTEN.get(soort);
        if (type == null) {
            c.getSource().sendFailure(Component.translatable("vadsrijk.wezens.cmd.onbekend", soort));
            return 0;
        }
        ServerLevel sl = p.level();
        VadsrijkMob m = type.get().create(sl, EntitySpawnReason.COMMAND);
        if (m == null) {
            return 0;
        }
        Vec3 voor = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(4));
        m.snapTo(voor.x, p.getY(), voor.z, p.getYRot() + 180, 0);
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
            af.add(alle[sl.getRandom().nextInt(alle.length)]);
        }
        int lvl = level > 0 ? level : 3;
        m.stelIn(lvl, null, elite, af);
        sl.addFreshEntity(m);
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.wezens.cmd.gespawnd", m.getType().getDescription(), lvl, elite ? " elite " + af : ""), false);
        return 1;
    }

    private static int draak(CommandContext<CommandSourceStack> c, int straal) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        DraakVlucht.cirkel(p.level(), p.position(), straal, p.getY() + 45, 30 * 20);
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.wezens.cmd.draak"), false);
        return 1;
    }

    private static int overvlucht(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Vec3 kijk = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 opzij = new Vec3(-kijk.z, 0, kijk.x);
        Vec3 o = p.position();
        List<Vec3> pad = new ArrayList<>();
        pad.add(o.add(opzij.scale(-140)).add(kijk.scale(40)).add(0, 70, 0));
        pad.add(o.add(opzij.scale(-50)).add(kijk.scale(25)).add(0, 35, 0));
        pad.add(o.add(kijk.scale(18)).add(0, 22, 0));
        pad.add(o.add(opzij.scale(60)).add(kijk.scale(30)).add(0, 38, 0));
        pad.add(o.add(opzij.scale(150)).add(kijk.scale(70)).add(0, 80, 0));
        var d = DraakVlucht.vlieg(p.level(), pad, 220, false);
        // the roar right when it passes overhead (heard everywhere)
        if (d != null) {
            Later.plan(p.level(), 95, () -> DraakVlucht.brul(d, true));
        }
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.wezens.cmd.draak"), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> c, int level) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String set = StringArgumentType.getString(c, "set");
        List<String> stukken = "zwam".equals(set) ? SetBonussen.ZWAMSET : SetBonussen.KLODDERSET;
        for (String s : stukken) {
            ItemStack st = Rollen.stack(Rollen.rol(BasisTypes.van(s), level, Zeldzaamheid.ZELDZAAM, p.getRandom()));
            if (!p.getInventory().add(st)) {
                p.drop(st, false);
            }
        }
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.wezens.cmd.set", set, level), false);
        return 1;
    }

    private static int loot(CommandContext<CommandSourceStack> c, int level, boolean elite) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String soort = StringArgumentType.getString(c, "soort");
        LootTabel t = LootTabellen.van("wezens:" + soort + (elite ? "_elite" : ""));
        if (t == null) {
            c.getSource().sendFailure(Component.translatable("vadsrijk.wezens.cmd.onbekend", soort));
            return 0;
        }
        LootTabel.Resultaat r = t.rol(level, p.getRandom());
        LootBundel.laatVallen(p, p.position().add(p.getLookAngle().scale(2)).add(0, 0.5, 0), r, elite ? 1 : 0);
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.wezens.cmd.loot", soort, level, r.items().size(), r.munten()), false);
        return 1;
    }

    /** A tiny server-tick scheduler for the command's delayed roar. */
    static final class Later {
        private record Taak(ServerLevel level, long wanneer, Runnable doe) {
        }

        private static final List<Taak> TAKEN = new ArrayList<>();

        static void plan(ServerLevel level, int ticks, Runnable r) {
            TAKEN.add(new Taak(level, level.getGameTime() + ticks, r));
        }

        static void tick() {
            TAKEN.removeIf(t -> {
                if (t.level().getGameTime() >= t.wanneer()) {
                    t.doe().run();
                    return true;
                }
                return false;
            });
        }
    }
}
