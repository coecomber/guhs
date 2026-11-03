package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Arrays;
import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.KaartFeature;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.Ondergrond;

/**
 * /guhs vadsrijk — for everyone: (no argument) in or out; {@code kist} found items back; {@code party uitnodigen|accepteer|verlaat}.
 * Dev (op 2): {@code proeftuin}, {@code level <n>}, {@code xp <n>}, {@code munten <n>}, {@code loot <zeldzaamheid> [type] [level]},
 * {@code baas reset [arena]}, {@code tips reset}, {@code info}, {@code vol} (Vads/energy/ult full, cooldowns off).
 * Other slices register their own {@code /guhs vadsrijk <word>} subtrees (brigadier merges the nodes).
 */
public final class VadsrijkCommando {
    private VadsrijkCommando() {
    }

    /** The start of every slice's command tree: {@code guhs vadsrijk}. Add your .then(...) and register it. */
    public static LiteralArgumentBuilder<CommandSourceStack> wortel(LiteralArgumentBuilder<CommandSourceStack> vadsrijkKind) {
        return Commands.literal("guhs").then(Commands.literal("vadsrijk").then(vadsrijkKind));
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> v = Commands.literal("vadsrijk").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (Vadsrijk.in(p)) {
                terug(p);
            } else {
                naarBinnen(p, false);
            }
            return 1;
        });
        v.then(Commands.literal("kist").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = Inventaris.haalGevonden(p);
            c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.kern.cmd.kist", n, Inventaris.gevondenAantal(p, Vadsrijk.in(p))), false);
            return n;
        }));
        v.then(Commands.literal("party")
                .then(Commands.literal("uitnodigen").then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException(), doel = EntityArgument.getPlayer(c, "speler");
                    String r = Partijen.nodigUit(p, doel);
                    c.getSource().sendSuccess(() -> Component.translatable(r, doel.getGameProfile().name()), false);
                    if (r.endsWith("uitgenodigd")) {
                        doel.sendSystemMessage(Component.translatable("vadsrijk.kern.party.uitnodiging", p.getGameProfile().name()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    return 1;
                })))
                .then(Commands.literal("accepteer").executes(c -> {
                    String r = Partijen.accepteer(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> Component.translatable(r), false);
                    return 1;
                }))
                .then(Commands.literal("verlaat").executes(c -> {
                    Partijen.verlaat(c.getSource().getPlayerOrException().getUUID());
                    c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.kern.party.weg"), false);
                    return 1;
                })));
        v.then(dev("proeftuin").executes(c -> {
            naarBinnen(c.getSource().getPlayerOrException(), true);
            return 1;
        }));
        v.then(dev("level").then(Commands.argument("n", IntegerArgumentType.integer(1, Levels.MAX)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Spelers.zetLevel(p, IntegerArgumentType.getInteger(c, "n"));
            c.getSource().sendSuccess(() -> Component.literal("Level " + Spelers.level(p)), false);
            return 1;
        })));
        v.then(dev("xp").then(Commands.argument("n", IntegerArgumentType.integer(1, 1_000_000)).executes(c -> {
            Spelers.geefXp(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"), "dev");
            return 1;
        })));
        v.then(dev("munten").then(Commands.argument("n", IntegerArgumentType.integer(0, 100_000_000)).executes(c -> {
            Spelers.geefMunten(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"));
            return 1;
        })));
        v.then(dev("vol").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            SpelerData d = Spelers.data(p);
            d.vads = Spelers.maxVads(p);
            d.energie = Spelers.maxEnergie(p);
            d.ultLading = 100;
            d.klaarOp.clear();
            Statussen.wisAlles(p);
            Spelers.sync(p);
            return 1;
        }));
        v.then(dev("tips").then(Commands.literal("reset").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Tips.vergeet(p);
            Spelers.data(p).gezien.clear();
            c.getSource().sendSuccess(() -> Component.literal("Tips en baas-intro's vergeten."), false);
            return 1;
        })));
        v.then(dev("loot").then(Commands.argument("zeldzaamheid", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Zeldzaamheid.values()).map(z -> z.name().toLowerCase(Locale.ROOT)), b))
                .executes(c -> loot(c.getSource(), StringArgumentType.getString(c, "zeldzaamheid"), null, -1))
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(BasisTypes.alle().stream().map(BasisType::id), b))
                        .executes(c -> loot(c.getSource(), StringArgumentType.getString(c, "zeldzaamheid"), StringArgumentType.getString(c, "type"), -1))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, Levels.MAX))
                                .executes(c -> loot(c.getSource(), StringArgumentType.getString(c, "zeldzaamheid"), StringArgumentType.getString(c, "type"),
                                        IntegerArgumentType.getInteger(c, "level")))))));
        v.then(dev("baas").then(Commands.literal("reset").executes(c -> baasReset(c.getSource(), null))
                .then(Commands.argument("arena", StringArgumentType.greedyString()).suggests((c, b) -> SharedSuggestionProvider.suggest(Arenas.ids(), b))
                        .executes(c -> baasReset(c.getSource(), StringArgumentType.getString(c, "arena"))))));
        v.then(dev("info").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            SpelerData d = Spelers.data(p);
            c.getSource().sendSuccess(() -> Component.literal("binnen=" + d.binnen + " opgeborgen=" + d.opgeborgen.size() + " rijk=" + d.rijkInventaris.size()
                    + " gevonden=" + d.gevonden.size() + " klasse=" + d.klasse + " lv=" + d.level() + " xp=" + d.xp(d.klasse) + " vads=" + (int) Spelers.vads(p) + "/"
                    + (int) Spelers.maxVads(p) + " munten=" + Spelers.munten(d.munten) + " stats=" + Spelers.stats(p)), false);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(v));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static int loot(CommandSourceStack s, String zeld, String type, int level) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        Zeldzaamheid z = Zeldzaamheid.van(zeld);
        BasisType t = type == null ? BasisTypes.alle().stream().skip(p.getRandom().nextInt(Math.max(1, BasisTypes.alle().size()))).findFirst().orElse(null)
                : BasisTypes.van(type);
        if (t == null) {
            s.sendFailure(Component.literal("Onbekend type: " + type));
            return 0;
        }
        var r = new nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel.Resultaat(
                java.util.List.of(Rollen.stack(Rollen.rol(t, level > 0 ? level : Spelers.level(p), z, p.getRandom()))), 0);
        nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel.laatVallen(p, p.position().add(p.getLookAngle().multiply(2, 0, 2)).add(0, 1, 0), r, 1);
        return 1;
    }

    private static int baasReset(CommandSourceStack s, String arena) {
        String id = arena != null ? arena : Arenas.ids().isEmpty() ? "" : Arenas.ids().get(0);
        boolean ok = Arenas.reset(s.getServer(), id);
        s.sendSuccess(() -> Component.literal(ok ? "Baas van " + id + " komt meteen terug." : "Onbekende arena: " + id), false);
        return ok ? 1 : 0;
    }

    // ---- in and out -----------------------------------------------------------------------------------------------------

    /** Into the dimension (remembering where you were): the arrival on the map if it exists, else the kern proeftuin. */
    public static void naarBinnen(ServerPlayer p, boolean proeftuin) {
        MinecraftServer server = p.level().getServer();
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        SpelerData d = Spelers.data(p);
        if (!Vadsrijk.in(p)) {
            CompoundTag t = new CompoundTag();
            t.putString("dim", p.level().dimension().identifier().toString());
            t.putDouble("x", p.getX());
            t.putDouble("y", p.getY());
            t.putDouble("z", p.getZ());
            t.putFloat("yaw", p.getYRot());
            t.putFloat("pitch", p.getXRot());
            d.terug = t;
        }
        boolean kaart = !proeftuin && sl.getStructureManager().get(KaartFeature.tegelId(Math.floorDiv(Gebieden.AANKOMST.getX(), KaartFeature.TEGEL),
                Math.floorDiv(Gebieden.AANKOMST.getZ(), KaartFeature.TEGEL))).isPresent();
        net.minecraft.core.BlockPos a = kaart ? Gebieden.AANKOMST : (d.checkpoint != null && Ondergrond.proeftuin(d.checkpoint) ? d.checkpoint : Ondergrond.KAMP);
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(a), Vec3.ZERO, 180f, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        KernEvents.verzoen(p);
    }

    /** Back to where the player was before (or their respawn point). */
    public static void terug(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        SpelerData d = Spelers.data(p);
        TeleportTransition tr = null;
        if (d.terug != null) {
            Identifier id = Identifier.tryParse(d.terug.getStringOr("dim", ""));
            ServerLevel l = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (l != null && l.dimension() != Vadsrijk.DIM) {
                tr = new TeleportTransition(l, new Vec3(d.terug.getDoubleOr("x", 0), d.terug.getDoubleOr("y", 64), d.terug.getDoubleOr("z", 0)), Vec3.ZERO,
                        d.terug.getFloatOr("yaw", 0), d.terug.getFloatOr("pitch", 0), TeleportTransition.DO_NOTHING);
            }
        }
        if (tr == null) {
            tr = p.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        }
        d.terug = null;
        p.teleport(tr);
        p.resetFallDistance();
        KernEvents.verzoen(p);
    }

    static Relative[] geen() {
        return new Relative[0];
    }
}
