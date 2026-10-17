package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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

/**
 * einde2 phase B, slice h4_vuur: the dev command of chapter 4 and the finale (CONTRACT_E2_B 8; a dev run only):
 * <ul>
 *   <li>{@code /guhs einde2 h4 ga <stap>}: any step of the two lines ({@link E2H4Vuur#STAPNAMEN}): the state as a real
 *       playthrough leaves it (every chapter before done, the flags, Appa and Momo, the team), you are put on the spot and
 *       what that step starts, starts;</li>
 *   <li>{@code /guhs einde2 h4 scene <naam>}: forget and play one scene at its own spot (no step changes);</li>
 *   <li>{@code /guhs einde2 h4 ronde <1-5>}: in the duel, jump to a round;</li>
 *   <li>{@code /guhs einde2 h4 win}: the minigame or scene of your step as if it went well (the next step);</li>
 *   <li>{@code /guhs einde2 h4 wis}: both lines, the flags and every scene of this slice forgotten.</li>
 * </ul>
 */
public final class E2H4VuurCommando {
    /** Step name -> the marker you are put on (template of the capital). */
    private static final Map<String, String> PLEK = new LinkedHashMap<>();
    /** Scene name -> its anchor marker. */
    private static final Map<String, String> ANKER = new LinkedHashMap<>();

    static {
        String[] plekken = {"aankomst_speler", "rustpunt_3", "dans_speler", "paleistuin", "invasie_strand", "rustpunt_4", "kamp", "kamp", "rustpunt_5",
                "trap_boven", "publiek_speler", "rustpunt_10", "rustpunt_10", "rustpunt_10", "leeuwschildpad_kade", "agni_kai_plein", "duel_speler",
                "kroning", "kroning", "rustpunt_2"};
        for (int i = 0; i < E2H4Vuur.STAPNAMEN.size(); i++) {
            PLEK.put(E2H4Vuur.STAPNAMEN.get(i), plekken[i]);
        }
        String[][] ankers = {{"aankomst", "aankomst_speler"}, {"school", "schoolbank_speler"}, {"zwarte_zon", "rustpunt_2"},
                {"gevangenis", "miroh_cel"}, {"kamp", "appa_kamp"}, {"kokend_rotsblok", "gondel_onder"}, {"draken", "trap_boven"},
                {"toneel", "publiek_speler"}, {"tent", "verzoening_zuka"}, {"top", ""}, {"leeuwschildpad", "leeuwschildpad_rug"},
                {"agni_kai", "agni_kai_midden"}, {"omslag", "duel_speler"}, {"trek_weg", "duel_speler"}, {"kroning", "kroning_zuka"},
                {"aftiteling", "kroning_zuka"}, {"knipoog_stitch", "knipoog_stitch_paneel"}, {"knipoog_sjokkel_finish", "knipoog_sjokkel_finish"},
                // fix3-h4: the why-scenes
                {"plan", "aankomst_speler"}, {"vijver", "brood"}, {"rotsblok_plan", "appa_kamp"}, {"zonnekrijgers", "trap_boven"},
                {"lotus_tegel", "publiek_speler"}, {"vloot_plan", "leeuwschildpad_kade"}};
        for (String[] a : ankers) {
            ANKER.put(a[0], a[1]);
        }
    }

    private E2H4VuurCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("h4")
                        .then(Commands.literal("ga").then(Commands.argument("stap", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(E2H4Vuur.STAPNAMEN, b))
                                .executes(c -> ga(c.getSource(), StringArgumentType.getString(c, "stap")))))
                        .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ANKER.keySet(), b))
                                .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                        .then(Commands.literal("ronde").then(Commands.argument("nr", IntegerArgumentType.integer(1, 5))
                                .executes(c -> ronde(c.getSource(), IntegerArgumentType.getInteger(c, "nr")))))
                        .then(Commands.literal("win").executes(c -> win(c.getSource())))
                        .then(Commands.literal("wis").executes(c -> wis(c.getSource()))))));
    }

    /** Puts the player at any step (also used by the game tests, without the teleport). */
    public static boolean zet(ServerPlayer p, String stap) {
        int i = E2H4Vuur.STAPNAMEN.indexOf(stap);
        if (i < 0) {
            return false;
        }
        E2H4Vuur.wisEigen(p);
        if (i < E2H4Vuur.V_STAPPEN) {
            E2VerhaalDev.wis(p, E2Verhaal.FINALE);
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, i);
            if (i > E2H4Vuur.V_DRAKENDANS) {
                E2H4Vuur.VUUR.vlag(p, E2Verhaal.V_ZUKA_TEAM, true);
                E2H4Vuur.VUUR.vlag(p, E2Verhaal.V_GELEERD, true);
            }
        } else {
            int f = Math.min(E2H4Vuur.F_STAPPEN, i - E2H4Vuur.V_STAPPEN);
            E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, f);
            if (f > E2H4Vuur.F_TOP) {
                E2H4Vuur.FINALE.vlag(p, E2Verhaal.V_WITTE_LOTUS, true);
            }
        }
        nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
        Look.sync(p);
        return true;
    }

    private static int ga(CommandSourceStack source, String stap) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (!zet(p, stap)) {
            source.sendFailure(Component.literal("No step " + stap + "; steps: " + E2H4Vuur.STAPNAMEN));
            return 0;
        }
        ServerLevel g = p.level().getServer().getLevel(nl.juiced.guhs.feature.guheinde.GuheindeFeature.GUHEINDE);
        BlockPos o = g == null ? null : nl.juiced.guhs.feature.einde2.Plekken.oorsprong(g, Plek.STRUCTUUR);
        if (o == null) {
            source.sendSuccess(() -> Component.literal("Step set; the capital is not known yet (go to the Guheinde first)"), false);
            return 1;
        }
        if (Appa.heeft(p) || E2H4Vuur.STAPNAMEN.indexOf(stap) >= E2H4Vuur.STAPNAMEN.indexOf("luchtvloot")) {
            Appa.geef(p);
        }
        if (!Momo.heeft(p)) {
            Momo.geef(p);
        }
        String plek = PLEK.get(stap);
        nl.juiced.guhs.feature.einde2.Plekken.Plek m = nl.juiced.guhs.feature.einde2.Plekken.plek(Plek.STRUCTUUR, plek);
        if (m == null) {
            source.sendFailure(Component.literal("Marker " + plek + " is not in the build"));
            return 0;
        }
        BlockPos tot = m.tot() == null ? m.pos() : m.tot();
        Vec3 naar = new Vec3(o.getX() + (m.pos().getX() + tot.getX()) / 2.0 + 0.5, o.getY() + Math.min(m.pos().getY(), tot.getY()),
                o.getZ() + (m.pos().getZ() + tot.getZ()) / 2.0 + 0.5);
        Plek.breng(p, g, naar, p.getYRot());
        E2Team.zetInStructuur(p, Plek.STRUCTUUR);
        source.sendSuccess(() -> Component.literal("h4: at step " + stap + " (" + E2Verhaal.VUUR + " " + E2H4Vuur.VUUR.stap(p) + "/" + E2H4Vuur.V_STAPPEN
                + ", " + E2Verhaal.FINALE + " " + E2H4Vuur.FINALE.stap(p) + "/" + E2H4Vuur.F_STAPPEN + ")"), false);
        return 1;
    }

    private static int scene(CommandSourceStack source, String naam) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Cutscene s = Cutscene.van(Scenes.ID + naam);
        if (s == null || !ANKER.containsKey(naam)) {
            source.sendFailure(Component.literal("No scene " + naam));
            return 0;
        }
        Cutscenes.vergeet(p, s.id());
        BlockPos anker;
        if (naam.equals("top")) {
            anker = Geestenwereld.in(p) ? Geestenwereld.wereld(Scenes.TOP_ANKER) : null;
            if (anker == null) {
                source.sendFailure(Component.literal("The Top plays in the Geestenwereld: /guhs einde2 geest ga top_voorgangers first"));
                return 0;
            }
        } else {
            anker = Plek.blok(p, ANKER.get(naam));
            if (anker == null && naam.equals("agni_kai")) {
                anker = Plek.blok(p, "kroning_zuka");
            }
        }
        if (anker == null) {
            source.sendFailure(Component.literal("Go to the Guheinde (the capital must be known)"));
            return 0;
        }
        boolean laag = naam.equals("leeuwschildpad") && Lagen.toon(p);
        boolean gestart = Cutscenes.speel(p, s, anker, Rotation.NONE, q -> {
            if (laag) {
                Lagen.weg(q);
            }
        });
        source.sendSuccess(() -> Component.literal(gestart ? "Playing " + s.id() : "Could not start (already watching something?)"), false);
        return gestart ? 1 : 0;
    }

    private static int ronde(CommandSourceStack source, int nr) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (E2H4Vuur.FINALE.stap(p) != E2H4Vuur.F_DUEL) {
            zet(p, "duel");
        }
        Duel.naarRonde(p, nr);
        source.sendSuccess(() -> Component.literal("Duel: round " + Duel.ronde(p)), false);
        return 1;
    }

    private static int win(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int v = E2H4Vuur.VUUR.stap(p), f = E2H4Vuur.FINALE.stap(p);
        if (!E2H4Vuur.VUUR.klaar(p)) {
            switch (v) {
                case E2H4Vuur.V_DANS -> Dansfeest.winVoorTest(p);
                case E2H4Vuur.V_GOEDMAKEN -> {
                    E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_TOPH, true);
                    E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_KATARA, true);
                    E2H4Vuur.VUUR.verder(p, v);
                }
                case E2H4Vuur.V_DRAKENDANS -> Drakendans.winVoorTest(p);
                default -> {
                    Rotsblok.stop(p);
                    E2H4Vuur.VUUR.verder(p, v);
                }
            }
        } else if (!E2H4Vuur.FINALE.klaar(p)) {
            switch (f) {
                case E2H4Vuur.F_TOP -> {
                    E2H4Vuur.FINALE.vlag(p, E2Verhaal.V_WITTE_LOTUS, true);
                    E2H4Vuur.FINALE.verder(p, f);
                }
                case E2H4Vuur.F_VLOOT -> Luchtvloot.winVoorTest(p);
                case E2H4Vuur.F_DUEL -> Duel.winVoorTest(p);
                default -> E2H4Vuur.FINALE.verder(p, f);
            }
        }
        source.sendSuccess(() -> Component.literal("h4: " + E2Verhaal.VUUR + " " + E2H4Vuur.VUUR.stap(p) + ", " + E2Verhaal.FINALE + " "
                + E2H4Vuur.FINALE.stap(p)), false);
        return 1;
    }

    private static int wis(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        E2H4Vuur.wisEigen(p);
        E2VerhaalDev.wis(p, E2Verhaal.VUUR);
        E2VerhaalDev.wis(p, E2Verhaal.FINALE);
        Look.sync(p);
        source.sendSuccess(() -> Component.literal("h4: both lines and every scene of the chapter forgotten"), false);
        return 1;
    }
}
