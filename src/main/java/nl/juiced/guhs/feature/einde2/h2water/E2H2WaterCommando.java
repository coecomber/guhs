package nl.juiced.guhs.feature.einde2.h2water;

import java.util.Set;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.Momo;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.reizen.E2Team;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 h2_water, dev run only (CONTRACT_E2_B 8; plain literals, {@code !FMLEnvironment.isProduction()}):
 * <ul>
 *   <li>{@code /guhs einde2 h2 ga <stap>}: jumps to ANY step of the chapter (a name of {@link E2H2Water#STAPNAMEN}): the
 *       story is set as a real playthrough leaves it ({@code E2VerhaalDev.zetTot}, the flags of the steps before it),
 *       Appa and Momo are yours, the team stands at the camp, and you are put where that step starts (in the Waterstam, the
 *       brig, the oasis, the Geestenwereld or on the snow plain);</li>
 *   <li>{@code /guhs einde2 h2 scene <naam>}: forgets a scene and plays it at its own anchor;</li>
 *   <li>{@code /guhs einde2 h2 wis}: forgets everything of the chapter;</li>
 *   <li>{@code /guhs einde2 h2 stand}: your step, flags and counters.</li>
 * </ul>
 */
final class E2H2WaterCommando {
    private E2H2WaterCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("h2")
                        .then(Commands.literal("ga").then(Commands.argument("stap", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(E2H2Water.STAPNAMEN, b))
                                .executes(c -> ga(c.getSource(), StringArgumentType.getString(c, "stap")))))
                        .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(E2H2WaterScenes.alle().stream().map(s -> s.id().substring("einde2_h2_water_".length())), b))
                                .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                        .then(Commands.literal("wis").executes(c -> wis(c.getSource())))
                        .then(Commands.literal("stand").executes(c -> stand(c.getSource()))))));
    }

    /** Where each step starts: a marker of the Waterstam (or "geest:<marker>" in the Geestenwereld). */
    static String start(int stap) {
        return switch (stap) {
            case E2H2Water.AANKOMST -> "uitzicht";
            case E2H2Water.PAKGUH, E2H2Water.ZWEEP -> "katara";
            case E2H2Water.GOLFOEFENING -> "ijsbrug_a";
            case E2H2Water.IJSBRUG -> "ijsbrug_a";
            case E2H2Water.SLEE -> "slee_start";
            case E2H2Water.GONDEL -> "gondel_start";
            case E2H2Water.YUE -> "paleis_poort";
            case E2H2Water.HAVEN -> "rustpunt_1";
            case E2H2Water.ONTSNAP -> "cel";
            case E2H2Water.MAANVIS -> "oase_poort";
            case E2H2Water.MEDITEER, E2H2Water.VIS_TERUG -> "yue_maan_kant";
            case E2H2Water.KOH -> "geest:plek_koh_grot";
            case E2H2Water.HEMELVIJVER -> "geest:plek_hemelvijver";
            case E2H2Water.VIS_HALEN -> "rustpunt_2";
            case E2H2Water.GOLF -> "golf_plek";
            case E2H2Water.STORM -> "sneeuwstorm_start";
            default -> "aankomst_speler";
        };
    }

    private static int ga(CommandSourceStack source, String naam) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int stap = E2H2Water.STAPNAMEN.indexOf(naam);
        if (stap < 0) {
            source.sendFailure(Component.literal("No step " + naam + "; steps: " + E2H2Water.STAPNAMEN));
            return 0;
        }
        E2H2Water.Oord o = E2H2Water.oord(source.getServer());
        if (o == null) {
            source.sendFailure(Component.literal("The spot of the Waterstam is not known yet (the search runs now): try again in a moment"));
            return 0;
        }
        E2H2Water.wis(p);
        E2VerhaalDev.zetTot(p, E2Verhaal.WATER, Math.min(stap, E2H2Water.STAPPEN));
        Verhaallijn l = E2H2Water.lijn();
        // the flags a real playthrough would have left at this step
        for (int i = 1; i <= E2H2Water.DOELEN && stap > E2H2Water.ZWEEP; i++) {
            l.vlag(p, E2H2Water.V_DOEL + i, true);
        }
        if (stap > E2H2Water.MEDITEER) {
            l.vlag(p, E2Verhaal.V_GEEST, true);
        }
        if (stap == E2H2Water.VIS_TERUG) {
            l.vlag(p, E2H2Water.V_VIS, true);
        }
        if (stap > E2H2Water.GOLF) {
            l.vlag(p, E2H2Water.V_VLOOT_WEG, true);
        }
        if (stap >= E2H2Water.STAPPEN) {
            l.vlag(p, E2Verhaal.V_GELEERD, true);
        }
        if (!Appa.heeft(p)) {
            Appa.geef(p);
        }
        if (!Momo.heeft(p)) {
            Momo.geef(p);
        }
        // where that step starts
        String waar = start(stap);
        if (waar.startsWith("geest:")) {
            if (!Geestenwereld.stuur(p, waar.substring("geest:".length()))) {
                source.sendFailure(Component.literal("No Geestenwereld in this world"));
            }
        } else {
            Vec3 plek = plek(o, waar);
            if (plek == null) {
                source.sendFailure(Component.literal("No marker " + waar + " in this world's Waterstam (an old build?)"));
                return 0;
            }
            p.teleportTo(o.level(), plek.x, plek.y, plek.z, Set.of(), p.getYRot(), 0f, true);
            E2Team.zetInStructuur(p, E2H2Water.STRUCTUUR);
        }
        source.sendSuccess(() -> Component.literal("guhvatar_water is now at step " + stap + " (" + naam + ")"), false);
        return 1;
    }

    /** A spot to stand for a step: a marker, the cell in the brig, or beside the pond. */
    static Vec3 plek(E2H2Water.Oord o, String waar) {
        if (waar.equals("cel")) {
            return Sluipen.cel(o);
        }
        if (waar.equals("yue_maan_kant")) {
            Vec3 m = o.midden("yue_maan");
            return m == null ? null : m.add(-1.5, 0, 5.5);
        }
        Vec3 m = o.midden(waar);
        if (m != null && (waar.equals("katara") || waar.equals("paleis_poort"))) {
            return m.add(0, 0, 3);
        }
        return m;
    }

    private static int scene(CommandSourceStack source, String naam) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Cutscene s = Cutscene.van("einde2_h2_water_" + naam);
        if (s == null) {
            source.sendFailure(Component.literal("No scene " + naam));
            return 0;
        }
        Cutscenes.vergeet(p, s.id());
        BlockPos anker = anker(source, s);
        if (anker == null) {
            source.sendFailure(Component.literal("Its anchor is not in this world"));
            return 0;
        }
        if (s == E2H2WaterScenes.KOH || s == E2H2WaterScenes.LA_WOEDE || s == E2H2WaterScenes.KOH_RAAD || s == E2H2WaterScenes.GEEST_AANKOMST) {
            ServerLevel geest = Geestenwereld.level(source.getServer());
            if (geest != null && p.level() != geest) {
                Geestenwereld.stuur(p, s == E2H2WaterScenes.LA_WOEDE ? "plek_hemelvijver" : s == E2H2WaterScenes.GEEST_AANKOMST ? Geestenwereld.AANKOMST
                        : "plek_koh_grot");
                // (a moment later, once the client has the Geestenwereld)
                nl.juiced.guhs.feature.einde2.verhaal.StapHint.later(p, 40, q -> {
                    BlockPos a = anker(source, s);
                    if (a != null) {
                        Cutscenes.speel(q, s, a, Rotation.NONE, null);
                    }
                });
                source.sendSuccess(() -> Component.literal("To the Geestenwereld; " + s.id() + " plays in a moment"), false);
                return 1;
            }
        }
        boolean ok = Cutscenes.speel(p, s, anker, Rotation.NONE, null);
        source.sendSuccess(() -> Component.literal(ok ? "Playing " + s.id() : "Busy: not now"), false);
        return ok ? 1 : 0;
    }

    /** The anchor of a scene in this world. */
    static BlockPos anker(CommandSourceStack source, Cutscene s) {
        if (s == E2H2WaterScenes.KOH || s == E2H2WaterScenes.KOH_RAAD) {
            return Geestenwereld.wereld("koh");
        }
        if (s == E2H2WaterScenes.GEEST_AANKOMST) {
            return Geestenwereld.wereld(Geestenwereld.AANKOMST);
        }
        if (s == E2H2WaterScenes.LA_WOEDE) {
            return Geestenwereld.wereld("plek_hemelvijver");
        }
        E2H2Water.Oord o = E2H2Water.oord(source.getServer());
        if (o == null) {
            return null;
        }
        String marker;
        if (s == E2H2WaterScenes.AANKOMST) {
            marker = "aankomst_speler";
        } else if (s == E2H2WaterScenes.PAKGUH) {
            marker = "pakguh";
        } else if (s == E2H2WaterScenes.BLAUWE_GEEST) {
            marker = "maanvis_kooi";
        } else if (s == E2H2WaterScenes.GOLF) {
            marker = "golf_plek";
        } else if (s == E2H2WaterScenes.STORM) {
            marker = "sneeuwstorm_start";
        } else if (s == E2H2WaterScenes.TEAM) {
            marker = "sneeuwstorm_vind";
        } else if (s == E2H2WaterScenes.WELKOM) {
            marker = "aankomst_speler";
        } else if (s == E2H2WaterScenes.LES_ZWEEP || s == E2H2WaterScenes.LES_GOLF) {
            marker = "pakguh";
        } else if (s == E2H2WaterScenes.LES_IJSBRUG) {
            marker = "ijsbrug_a";
        } else if (s == E2H2WaterScenes.OTTERS) {
            marker = "ijsbrug_b";
        } else if (s == E2H2WaterScenes.MAANFEEST) {
            marker = "slee_finish";
        } else if (s == E2H2WaterScenes.PALEIS) {
            marker = "yue";
        } else if (s == E2H2WaterScenes.KADE) {
            marker = "sluip_start";
        } else if (s == E2H2WaterScenes.VIS_AAN_BOORD) {
            marker = "rustpunt_2";
        } else if (s == E2H2WaterScenes.KNIPOOG_BALTO || s == E2H2WaterScenes.KNIPOOG_SJOKKEL) {
            var punten = o.punten(s == E2H2WaterScenes.KNIPOOG_BALTO ? "knipoog_balto_camera" : "knipoog_sjokkel_camera");
            return punten.isEmpty() ? null : punten.get(punten.size() - 1);
        } else {
            marker = "yue_maan";
        }
        return o.wereld(marker);
    }

    private static int wis(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        E2H2Water.wis(p);
        source.sendSuccess(() -> Component.literal("Chapter 2 (guhvatar_water) forgotten"), false);
        return 1;
    }

    private static int stand(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Verhaallijn l = E2H2Water.lijn();
        int stap = l.stap(p);
        String vlaggen = "doelen " + E2H2Water.doelenGeraakt(p) + "/" + E2H2Water.DOELEN + ", golven " + l.teller(p, E2H2Water.T_GOLVEN)
                + ", brug " + l.vlag(p, E2H2Water.V_BRUG) + ", vis " + l.vlag(p, E2H2Water.V_VIS) + ", vloot weg " + l.vlag(p, E2H2Water.V_VLOOT_WEG)
                + ", geest " + l.vlag(p, E2Verhaal.V_GEEST) + ", geleerd " + l.vlag(p, E2Verhaal.V_GELEERD) + ", in de Waterstam " + E2H2Water.binnen(p)
                + ", Koh-proef " + Geestreis.bezig(p);
        source.sendSuccess(() -> Component.literal("guhvatar_water: step " + stap + " (" + E2H2Water.STAPNAMEN.get(Math.min(stap, E2H2Water.STAPNAMEN.size() - 1))
                + "), open " + l.aanDeBeurt(p) + "; " + vlaggen), false);
        return 1;
    }
}
