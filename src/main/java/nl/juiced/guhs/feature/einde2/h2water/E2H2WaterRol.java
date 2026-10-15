package nl.juiced.guhs.feature.einde2.h2water;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

import static nl.juiced.guhs.feature.einde2.h2water.E2H2Water.*;

/**
 * einde2 h2_water: what the characters of the Waterstam say and do when you click them (CONTRACT_E2_B 4.4: every spot
 * {@code noordelijke_waterstam/*} is this chapter's). Two things start the story by a click: Meester Pakguh at his step
 * ({@code E2H2Water.pakguh}) and Yue-guh at hers ({@code E2H2Water.yue}); a guard of the ship clicked during a sneak part
 * catches you. Everybody else says a line that fits where the player is in the chapter ({@link #REGELS}: the first rule
 * whose steps fit, a random one of the rules with the same steps).
 */
final class E2H2WaterRol implements NpcRole {
    private static final String K = "quest.guhs.einde2_h2_water.rol.";

    /** A line for the steps van..tot (inclusive; {@code STAPPEN} = the chapter is done). */
    record Regel(int van, int tot, String key) {
    }

    private static List<Regel> r(Object... v) {
        List<Regel> uit = new ArrayList<>();
        for (int i = 0; i < v.length; i += 3) {
            uit.add(new Regel((Integer) v[i], (Integer) v[i + 1], (String) v[i + 2]));
        }
        return uit;
    }

    static final Map<String, List<Regel>> REGELS = Map.ofEntries(
            Map.entry("pakguh", r(AANKOMST, PAKGUH, K + "pakguh.0", ZWEEP, ZWEEP, K + "pakguh.zweep", GOLFOEFENING, GOLFOEFENING, K + "pakguh.golf",
                    IJSBRUG, IJSBRUG, K + "pakguh.ijsbrug", SLEE, YUE, K + "pakguh.vrij", HAVEN, STORM, K + "pakguh.beleg", STAPPEN, STAPPEN, K + "pakguh.klaar")),
            Map.entry("katara", r(AANKOMST, PAKGUH, K + "katara.0", ZWEEP, IJSBRUG, K + "katara.training", SLEE, SLEE, K + "katara.slee",
                    GONDEL, GONDEL, K + "katara.gondel", YUE, ONTSNAP, K + "katara.yue", MAANVIS, VIS_TERUG, K + "katara.beleg",
                    GOLF, STORM, K + "katara.golf", STAPPEN, STAPPEN, K + "katara.klaar")),
            Map.entry("sokka", r(AANKOMST, IJSBRUG, K + "sokka.0", SLEE, SLEE, K + "sokka.slee", GONDEL, GONDEL, K + "sokka.gondel", YUE, YUE, K + "sokka.yue",
                    HAVEN, STORM, K + "sokka.beleg", STAPPEN, STAPPEN, K + "sokka.klaar")),
            Map.entry("yue", r(AANKOMST, GONDEL, K + "yue.0", HAVEN, VIS_TERUG, K + "yue.beleg", GOLF, STAPPEN, K + "yue.0")),
            Map.entry("hoofdman", r(AANKOMST, GONDEL, K + "hoofdman.0", YUE, VIS_TERUG, K + "hoofdman.beleg", GOLF, STAPPEN, K + "hoofdman.na")),
            Map.entry("poortwachter", r(AANKOMST, STORM, K + "poortwachter.0", STAPPEN, STAPPEN, K + "poortwachter.klaar")),
            Map.entry("zhao", r(AANKOMST, STAPPEN, K + "zhao.0", AANKOMST, STAPPEN, K + "zhao.1")),
            Map.entry("wacht", r(AANKOMST, STAPPEN, K + "wacht.0")),
            Map.entry("bewoner", r(AANKOMST, GONDEL, K + "bewoner.0", AANKOMST, GONDEL, K + "bewoner.1", YUE, VIS_TERUG, K + "bewoner.beleg",
                    GOLF, STAPPEN, K + "bewoner.na", GOLF, STAPPEN, K + "bewoner.na2")),
            Map.entry("storm_zuka", r(AANKOMST, STAPPEN, K + "storm_zuka.0", AANKOMST, STAPPEN, K + "storm_zuka.1")),
            Map.entry("yue_maan", r(AANKOMST, STAPPEN, K + "yue_maan.0", AANKOMST, STAPPEN, K + "yue_maan.1")));

    /** The spot this role is for (the marker name; the numbered spots share one: "bewoner", "wacht"). */
    private final String plek;

    E2H2WaterRol(String plek) {
        this.plek = plek;
    }

    /** Registers a role for every spot of the chapter's characters (E2H2WaterFeature.register). */
    static void registreer() {
        String s = STRUCTUUR + "/";
        NpcRollen.zet(GuhNpcEntity.Kind.PAKGUH, s + "pakguh", new E2H2WaterRol("pakguh"));
        NpcRollen.zet(GuhNpcEntity.Kind.KATARA_GUH, s + "katara", new E2H2WaterRol("katara"));
        NpcRollen.zet(GuhNpcEntity.Kind.SOKKA_GUH, s + "sokka", new E2H2WaterRol("sokka"));
        NpcRollen.zet(GuhNpcEntity.Kind.YUE_GUH, s + "yue", new E2H2WaterRol("yue"));
        NpcRollen.zet(GuhNpcEntity.Kind.ARNOOK_GUH, s + "hoofdman", new E2H2WaterRol("hoofdman"));
        NpcRollen.zet(GuhNpcEntity.Kind.WATERSTAM_WACHTER, s + "poortwachter", new E2H2WaterRol("poortwachter"));
        NpcRollen.zet(GuhNpcEntity.Kind.ZHAO_MIKA, s + "zhao", new E2H2WaterRol("zhao"));
        for (int i = 1; i <= 5; i++) {
            NpcRollen.zet(GuhNpcEntity.Kind.VUURNATIE_MIKA, s + "sluip_wacht_" + i, new E2H2WaterRol("wacht"));
        }
        for (int i = 1; i <= 8; i++) {
            NpcRollen.zet(GuhNpcEntity.Kind.WATERSTAM_GUH, s + "bewoner_" + i, new E2H2WaterRol("bewoner"));
        }
        NpcRollen.zet(GuhNpcEntity.Kind.ZUKA_BALLING, Toneel.PLEK_ZUKA, new E2H2WaterRol("storm_zuka"));
        NpcRollen.zet(GuhNpcEntity.Kind.YUE_GUH_MAAN, Toneel.PLEK_MAAN, new E2H2WaterRol("yue_maan"));
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        Verhaallijn l = lijn();
        int stap = l.aanDeBeurt(player) ? l.stap(player) : AANKOMST;
        npc.speel("praat");
        // the clicks that move the story
        if (plek.equals("pakguh") && stap == PAKGUH && pakguh(player)) {
            return;
        }
        if (plek.equals("yue") && stap == YUE && yue(player)) {
            return;
        }
        if (plek.equals("wacht") && (stap == ONTSNAP || stap == VIS_HALEN)) {
            Sluipen.gezien(player, new Sluipen.Wacht(1, npc.getEyePosition(), npc.getYRot()));
            return;
        }
        String key = regel(plek, stap, player.getRandom().nextInt(2));
        if (key != null) {
            GuhQuests.say(player, npc, key);
        }
    }

    /** The line of this spot for this step (keuze picks between two rules with the same steps); null: none. */
    static String regel(String plek, int stap, int keuze) {
        List<Regel> regels = REGELS.get(plek);
        if (regels == null) {
            return null;
        }
        List<Regel> passend = new ArrayList<>();
        for (Regel r : regels) {
            if (stap >= r.van() && stap <= r.tot()) {
                if (!passend.isEmpty() && (passend.get(0).van() != r.van() || passend.get(0).tot() != r.tot())) {
                    break;
                }
                passend.add(r);
            } else if (!passend.isEmpty()) {
                break;
            }
        }
        return passend.isEmpty() ? null : passend.get(Math.floorMod(keuze, passend.size())).key();
    }
}
