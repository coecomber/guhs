package nl.juiced.guhs.feature.vadsrijk.zone4.wezens;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot.Z4Loot;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot.Z4Sets;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.mob.Z4Wezen;

/**
 * Dev command {@code /guhs vadsrijk z4wezens ...} (op level 2):
 * <pre>
 *   spawn &lt;soort&gt; [level] [elite]   one creature 4 blocks in front of you
 *   alle [level]                    every kind in a row on a fresh sand platform (no AI; the AutoCheck line-up)
 *   tp &lt;eiland&gt;                     to an island with creatures (koraalbaai, vliegereiland, vuurtoreneiland, mysterie, rotsjes, wrak)
 *   set schelpen|lampion [level]    the full set (rare)
 *   sieraden [level]                the 3 jewellery pieces
 *   wapens [level]                  the 6 class weapons
 *   kristallen                      both crystals in all 3 qualities
 *   loot &lt;soort&gt; [level] [elite]    rolls that creature's table for you
 *   opruimen                        removes all zone-4 creatures within 64 blocks
 *   spawners                        the spawners per island (+ whether the spots are real POI or placeholders)
 *   herlaad                         re-reads the spots (after t-z4wereld's POI arrived) and rebuilds the spawners
 * </pre>
 */
public final class Z4WezensCommando {
    private Z4WezensCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("z4wezens").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z4WezensRegistry.SOORTEN.keySet(), b))
                        .executes(c -> spawn(c, 0, false))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 60)).executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), false))
                                .then(Commands.argument("elite", BoolArgumentType.bool())
                                        .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite")))))))
                .then(Commands.literal("alle").executes(c -> alle(c, 52))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 60)).executes(c -> alle(c, IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("tp").then(Commands.argument("eiland", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z4WezensPlekken.eilanden().keySet(), b)).executes(Z4WezensCommando::tp)))
                .then(Commands.literal("set").then(Commands.argument("set", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z4Sets.SETS, b))
                        .executes(c -> set(c, 52))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 60)).executes(c -> set(c, IntegerArgumentType.getInteger(c, "level"))))))
                .then(Commands.literal("sieraden").executes(c -> lijst(c, sieraden(), 52))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 60)).executes(c -> lijst(c, sieraden(), IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("wapens").executes(c -> lijst(c, Z4Loot.WAPENS, 52))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 60)).executes(c -> lijst(c, Z4Loot.WAPENS, IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("kristallen").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    for (String k : Z4Loot.KRISTALLEN) {
                        for (int q = 0; q < 3; q++) {
                            p.getInventory().add(Kristallen.stack(k, q, 1));
                        }
                    }
                    return zeg(c, "Parelkristal en Zeeglas in ruw, geslepen en stralend.");
                }))
                .then(Commands.literal("loot").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Z4WezensRegistry.SOORTEN.keySet(), b))
                        .executes(c -> loot(c, 52, false))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 60)).executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), false))
                                .then(Commands.argument("elite", BoolArgumentType.bool())
                                        .executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level"), BoolArgumentType.getBool(c, "elite")))))))
                .then(Commands.literal("opruimen").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int n = opruimen(p.level(), p.blockPosition(), 64);
                    return zeg(c, n + " zone-4-wezens opgeruimd.");
                }))
                .then(Commands.literal("spawners").executes(c -> zeg(c, spawnersTekst())))
                .then(Commands.literal("herlaad").executes(c -> {
                    Z4WezensSpawners.registreer();
                    return zeg(c, "Spawners opnieuw opgebouwd. " + spawnersTekst());
                }))));
    }

    private static List<String> sieraden() {
        return Z4Loot.STUKKEN.entrySet().stream().filter(e -> e.getValue().sieraad()).map(java.util.Map.Entry::getKey).toList();
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    public static String spawnersTekst() {
        return Z4WezensSpawners.ALLE.size() + " zone-4-spawners, max levend per eiland " + Z4WezensSpawners.MAX_PER_EILAND + " (totaal "
                + Z4WezensSpawners.maxTotaal() + "); plekken: " + (Z4WezensPlekken.echt ? "echte POI van t-z4wereld" : "PLACEHOLDERS (nog geen POI_Z4)")
                + ". Geen wezens op Lampionhaven, het Knuffeltij-strand, het Strandfeest-, Bloemen- en Schildpadeiland en de Pierkop.";
    }

    public static int opruimen(ServerLevel sl, BlockPos pos, int r) {
        List<Entity> l = new ArrayList<>(sl.getEntitiesOfClass(Z4Wezen.class, new AABB(pos).inflate(r)));
        l.forEach(Entity::discard);
        return l.size();
    }

    /** Makes one creature at a spot (level, elite with these affixes). */
    public static Entity maak(ServerLevel sl, String soort, Vec3 pos, float yaw, int level, boolean elite, EnumSet<Affix> af) {
        var holder = Z4WezensRegistry.SOORTEN.get(soort);
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
        int lvl = level > 0 ? level : 52;
        Vec3 voor = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(4));
        Entity e = maak(p.level(), soort, new Vec3(voor.x, p.getY(), voor.z), p.getYRot() + 180, lvl, elite, af);
        if (e == null) {
            c.getSource().sendFailure(Component.literal("Dat ken ik niet: " + soort + ". Kies uit " + Z4WezensRegistry.SOORTEN.keySet()));
            return 0;
        }
        return zeg(c, soort + " gemaakt (level " + lvl + (elite ? ", elite " + af : "") + ").");
    }

    /** The line-up: a sand platform at your feet level, every kind 3 blocks apart (no AI). */
    private static int alle(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel sl = p.level();
        opruimen(sl, p.blockPosition(), 64);
        List<String> namen = new ArrayList<>(Z4WezensRegistry.SOORTEN.keySet());
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
            }
            i++;
        }
        return zeg(c, namen.size() + " wezens op een rij gezet (level " + level + "). Sla ze om de roze plof te zien.");
    }

    private static int tp(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String e = StringArgumentType.getString(c, "eiland");
        BlockPos b = Z4WezensPlekken.eilanden().get(e);
        if (b == null) {
            c.getSource().sendFailure(Component.literal("Onbekend eiland: " + e + ". Kies uit " + Z4WezensPlekken.eilanden().keySet()));
            return 0;
        }
        ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
        sl.getChunk(b);
        p.teleportTo(sl, b.getX() + 0.5, b.getY() + 0.1, b.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
        return zeg(c, "Op " + e + " (" + b.getX() + " " + b.getY() + " " + b.getZ() + ")" + (Z4WezensPlekken.echt ? "" : " (placeholder-plek)") + ".");
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
        if (!Z4Sets.SETS.contains(set)) {
            c.getSource().sendFailure(Component.literal("Onbekende set: " + set + ". Kies uit " + Z4Sets.SETS));
            return 0;
        }
        for (String s : Z4Sets.stukkenVan(set)) {
            geef(p, s, level);
        }
        return zeg(c, "Set " + set + " gegeven (level " + level + ").");
    }

    private static int lijst(CommandContext<CommandSourceStack> c, List<String> types, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        for (String s : types) {
            geef(p, s, level);
        }
        return zeg(c, types.size() + " stuks gegeven (level " + level + "): " + types);
    }

    private static int loot(CommandContext<CommandSourceStack> c, int level, boolean elite) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String soort = StringArgumentType.getString(c, "soort");
        LootTabel t = LootTabellen.van("z4wezens:" + soort + (elite ? "_elite" : ""));
        if (t == null) {
            c.getSource().sendFailure(Component.literal("Geen tabel voor " + soort));
            return 0;
        }
        LootTabel.Resultaat r = t.rol(level, p.getRandom());
        LootBundel.laatVallen(p, p.position().add(p.getLookAngle().scale(2)).add(0, 0.5, 0), r, elite ? 1 : 0);
        return zeg(c, "Loot van " + soort + " (level " + level + "): " + r.items().size() + " items, " + r.munten() + " brons.");
    }
}
