package nl.juiced.guhs.feature.ringh5;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.KnekelRuiterEntity;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * bbq2 (ring-h5): the op commands {@code /guhs ringh5 ...} (dev checks and the AutoCheck script
 * tools/autocheck/bbq2_ring-h5.txt). Texts are literals: they are for whoever runs the server.
 * <pre>
 * /guhs ringh5 stand               the step of this player, the copy they are at, what the Eye does and where its light is
 * /guhs ringh5 controle            the copy around you (or around where the command runs) against Plekken: every Rustvuurtje, the
 *                                  door, the Eye, the guards, the riders, Boromika
 * /guhs ringh5 stap &lt;0-10&gt;         this chapter starts (chapters 1-4 are finished for you) and jumps to that step
 * /guhs ringh5 wis                 forget this chapter
 * /guhs ringh5 ga &lt;plek&gt;           to a spot of the copy around you: kamp, uitkijk, asveld, slakkenhut, vlakte, holte, laan, hek, poortje, achter, oog
 * /guhs ringh5 blik                the light of the Eye jumps to where you stand and stays five seconds
 * /guhs ringh5 scene &lt;welke&gt;       plays boromika, oog or guhdalf at its own spot of the copy around you (no step is taken)
 * /guhs ringh5 bouw                (dev runs only) a try-out copy with its camp at your feet
 * </pre>
 */
final class RingH5Commands {
    private static final Map<String, BlockPos> GA = new LinkedHashMap<>();

    static {
        GA.put("kamp", Plekken.VUUR_KAMP.offset(1, 0, 2));
        GA.put("uitkijk", Plekken.UITKIJK);
        GA.put("asveld", Plekken.VUUR_UITKIJK.offset(1, 0, 0));
        GA.put("slakkenhut", Plekken.VUUR_SLAKKENHUT.offset(1, 0, 0));
        GA.put("vlakte", Plekken.BLIK_B.get(1));
        GA.put("holte", Plekken.VUUR_HOLTE.offset(-1, 0, 0));
        GA.put("laan", Plekken.ROUTE_LAAN.get(2));
        GA.put("hek", Plekken.ROUTE_HEK.get(0));
        GA.put("poortje", Plekken.VUUR_POORTJE.offset(1, 0, 1));
        GA.put("achter", Plekken.VUUR_ACHTER.offset(1, 0, 1));
        GA.put("oog", Plekken.OOG.offset(0, 0, -14));
    }

    static void register(RegisterCommandsEvent event) {
        var wortel = Commands.literal("ringh5").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        wortel.then(Commands.literal("stand").executes(RingH5Commands::stand));
        wortel.then(Commands.literal("controle").executes(RingH5Commands::controle));
        wortel.then(Commands.literal("stap").then(Commands.argument("n", IntegerArgumentType.integer(0, Hoofdstuk.STAPPEN)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Ring.lijn(1).begin(p);
            for (int i = 1; i <= 4; i++) {
                Verhaallijn l = Ring.lijn(i);
                l.zet(p, l.stappen());
            }
            RingH5Feature.LIJN.zet(p, IntegerArgumentType.getInteger(c, "n"));
            return zeg(c, "chapter 5: step " + RingH5Feature.LIJN.stap(p) + " of " + Hoofdstuk.STAPPEN);
        })));
        wortel.then(Commands.literal("wis").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            RingH5Feature.LIJN.wis(p);
            Hoofdstuk.vergeet(p.getUUID());
            return zeg(c, "chapter 5 is forgotten");
        }));
        wortel.then(Commands.literal("ga").then(Commands.argument("plek", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(GA.keySet(), b)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BlockPos lokaal = GA.get(StringArgumentType.getString(c, "plek").toLowerCase(Locale.ROOT));
                    Terrein t = Terrein.zoek(p.level(), p.blockPosition(), 256);
                    if (lokaal == null || t == null) {
                        return zeg(c, lokaal == null ? "spots: " + String.join(", ", GA.keySet()) : "no copy of the Zwarte Roosterpoort within 256 blocks");
                    }
                    Vec3 daar = t.midden(lokaal);
                    p.teleportTo(daar.x, daar.y, daar.z);
                    return zeg(c, "at " + BlockPos.containing(daar).toShortString() + " (build " + lokaal.toShortString() + ")");
                })));
        wortel.then(Commands.literal("blik").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Terrein t = Terrein.zoek(p.level(), p.blockPosition(), 256);
            OogEntity oog = t == null ? null : Hoofdstuk.oog(p.level(), t);
            if (oog == null) {
                return zeg(c, "no Eye here (is its chunk loaded?)");
            }
            oog.blik().zet(p.position(), 100);
            return zeg(c, "the light is on you; the Eye sees you: " + Blik.vrijZicht(p.level(), oog.kijkpunt(), p, oog));
        }));
        wortel.then(Commands.literal("scene").then(Commands.argument("welke", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("boromika", "oog", "guhdalf"), b)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String welke = StringArgumentType.getString(c, "welke").toLowerCase(Locale.ROOT);
                    Terrein t = Terrein.zoek(p.level(), p.blockPosition(), 256);
                    if (t == null) {
                        return zeg(c, "no copy of the Zwarte Roosterpoort within 256 blocks");
                    }
                    var scene = welke.equals("boromika") ? Scenes.BOROMIKA : welke.equals("oog") ? Scenes.OOG : welke.equals("guhdalf") ? Scenes.GUHDALF : null;
                    if (scene == null) {
                        return zeg(c, "scenes: boromika, oog, guhdalf");
                    }
                    return zeg(c, Hoofdstuk.speel(p, t, scene, null) ? "playing " + scene.id() + " at its own spot (no step is taken)" : "you are watching something already");
                })));
        if (!FMLEnvironment.isProduction()) {
            wortel.then(Commands.literal("bouw").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                PoortProef.bouw(p.level(), Plekken.VUUR_KAMP.offset(1, 0, -3), p.blockPosition(), true);
                return zeg(c, "a try-out copy of the Zwarte Roosterpoort, its camp at your feet (the valley lies towards +z)");
            }));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(wortel));
    }

    private static int stand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel level = p.level();
        Terrein t = Terrein.zoek(level, p.blockPosition(), 256);
        StringBuilder s = new StringBuilder("step " + RingH5Feature.LIJN.stap(p) + "/" + Hoofdstuk.STAPPEN + (Hoofdstuk.bezig(p) ? " (busy)" : ""));
        s.append(", smoke blown away: ").append(RingH5Feature.LIJN.vlag(p, Hoofdstuk.ROOK_WEG));
        if (t == null) {
            return zeg(c, s.append(", no copy within 256 blocks").toString());
        }
        Vec3 l = t.lokaal(p.position());
        s.append(", copy at ").append(t.nul().toShortString()).append(" turned ").append(t.draai().name().toLowerCase(Locale.ROOT));
        s.append(", you are at build ").append(String.format(Locale.ROOT, "%.1f %.1f %.1f", l.x, l.y, l.z));
        OogEntity oog = Hoofdstuk.oog(level, t);
        if (oog == null) {
            return zeg(c, s.append(", the Eye is not loaded").toString());
        }
        Vec3 licht = oog.blik().plek();
        s.append(", Eye ").append(new String[]{"watches", "searches", "sleeps"}[oog.staat()]).append(" zone ").append(oog.blik().zone());
        s.append(", light ").append(licht == null ? "-" : BlockPos.containing(licht).toShortString());
        s.append(", in the light ").append(oog.blik().gezien(p)).append("/").append(Blik.GENADE).append(", ring ").append(oog.blik().focus(p)).append("/").append(Blik.FOCUS);
        s.append(", free line to you: ").append(Blik.vrijZicht(level, oog.kijkpunt(), p, oog));
        return zeg(c, s.toString());
    }

    /** The copy around the player against the numbers of Plekken: proves the coordinates in a real (turned) copy. */
    private static int controle(CommandContext<CommandSourceStack> c) {
        // (from where the command is run, so it also works from the console: execute in <dim> positioned <x y z> run ...)
        ServerLevel level = c.getSource().getLevel();
        Terrein t = Terrein.zoek(level, BlockPos.containing(c.getSource().getPosition()), 256);
        if (t == null) {
            return zeg(c, "no copy of the Zwarte Roosterpoort within 256 blocks (loaded chunks only)");
        }
        int fout = 0;
        StringBuilder s = new StringBuilder("copy at " + t.nul().toShortString() + " turned " + t.draai().name().toLowerCase(Locale.ROOT) + ":");
        List<BlockPos> vuren = List.of(Plekken.VUUR_KAMP, Plekken.VUUR_UITKIJK, Plekken.VUUR_SLAKKENHUT, Plekken.VUUR_HOLTE, Plekken.VUUR_POORTJE, Plekken.VUUR_ACHTER);
        int goed = 0, geladen = 0;
        for (BlockPos vuur : vuren) {
            BlockPos w = t.wereld(vuur);
            if (!level.isLoaded(w)) {
                continue;
            }
            geladen++;
            if (level.getBlockState(w).is(RingFeature.RUSTVUUR.get())) {
                goed++;
            } else {
                fout++;
                s.append(" [no Rustvuurtje at ").append(w.toShortString()).append(": ").append(level.getBlockState(w).getBlock().getDescriptionId()).append("]");
            }
        }
        s.append(" fires ").append(goed).append("/").append(geladen).append(" (of 6 loaded)");
        if (level.isLoaded(t.wereld(Plekken.DEUR.get(0)))) {
            int deur = 0;
            for (BlockPos lokaal : BlockPos.betweenClosed(Plekken.DEUR.get(0), Plekken.DEUR.get(1))) {
                deur += level.getBlockState(t.wereld(lokaal)).isAir() ? 1 : 0;
            }
            s.append(", side door air ").append(deur).append("/9");
            fout += deur == 9 ? 0 : 1;
        }
        AABB doos = t.doos(List.of(BlockPos.ZERO, Plekken.MAAT));
        int ogen = level.getEntitiesOfClass(OogEntity.class, doos, Entity::isAlive).size();
        int wachters = level.getEntitiesOfClass(RoosterwachterEntity.class, doos, Entity::isAlive).size();
        int ruiters = level.getEntitiesOfClass(KnekelRuiterEntity.class, doos, e -> e.isAlive() && !e.isJager()).size();
        int boromika = level.getEntitiesOfClass(GuhNpcEntity.class, doos, e -> e.isAlive() && e.getKind() == GuhNpcEntity.Kind.BOROMIKA).size();
        s.append(", Eye ").append(ogen).append("/1, guards ").append(wachters).append("/3, riders ").append(ruiters).append("/2, Boromika ").append(boromika).append("/1");
        OogEntity oog = Hoofdstuk.oog(level, t);
        if (oog != null) {
            Vec3 licht = oog.blik().plek();
            s.append(", the Eye ").append(new String[]{"watches", "searches", "sleeps"}[oog.staat()]).append(" (light ")
                    .append(licht == null ? "-" : BlockPos.containing(licht).toShortString()).append(")");
        }
        s.append(fout == 0 ? " - the spots are right" : " - " + fout + " WRONG");
        return zeg(c, s.toString());
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal("[ringh5] " + tekst), false);
        return 1;
    }

    private RingH5Commands() {
    }
}
