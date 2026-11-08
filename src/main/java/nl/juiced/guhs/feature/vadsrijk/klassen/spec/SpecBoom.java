package nl.juiced.guhs.feature.vadsrijk.klassen.spec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.logging.LogUtils;

import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;

/**
 * U-SPECS: the skill trees re-laid as <b>a shared trunk + 2 spec branches + an optional side branch</b> (ronde 4, V3). Four
 * columns, read from left to right:
 * <pre>
 *   kolom 0          kolom 1        kolom 2          kolom 3
 *   spec A           STAM (trunk)   spec B           zijtak (tank / steun / heel)
 *   Schildridder     everyone       Reuzenkussen     Tank       (Knuffelridder)
 *   Scherpschutter   everyone       Strikkenzetter   Steun      (Pluisjager)
 *   Sterrenwever     everyone       Zegenzanger      Heel       (Vadsmagier)
 * </pre>
 * Rows keep their cost and level gate (row 0: 1 point; 1-2: 2; 3: 3; 4: 2 at lv 20; 5: 3 at lv 26; 6: 4 at lv 40-45; 7: 4 at
 * lv 48). A spec column's first node needs the hidden spec node ({@link Spec#knoop}), so it opens at the step-1 ceremony
 * (class lv 17); the other spec's column stays locked. Before lv 17 the trunk + side branch hold exactly 16 points.
 * <p>Runs once after every slice added its nodes ({@code VadsrijkFeature.register}, last line): node ids (and so every effect)
 * stay the same, only place, cost, level gate and requirements change; the zone-2/3 rows and the old capstones become parts of
 * the branches. New nodes: a few stat nodes so every branch is a real path, and four effect keystones ({@link SpecEffecten}).
 */
public final class SpecBoom {
    public static final int KOLOMMEN = 4;
    /** Column -> what it is (lang key suffix {@code vadsrijk.klassen.tak.<x>}). */
    public static final String[] TAK = {"spec_a", "stam", "spec_b", "zij"};

    private record Plek(String id, int rij, int kolom, String vereist) {
    }

    private static final Map<String, List<Plek>> PLEKKEN = new LinkedHashMap<>();
    private static final Map<String, BoomKnoop> NIEUW = new LinkedHashMap<>();
    private static boolean klaar;

    private SpecBoom() {
    }

    static {
        String r = Klassen.RIDDER, j = Klassen.JAGER, m = Klassen.MAGIER;
        // ---- Knuffelridder: Schildridder | stam | Reuzenkussen | Tank ----------------------------------------------------------
        stat(r, "r_schildarm", Stats.van(Stat.VERDEDIGING, 6, Stat.KRACHT, 4), "minecraft:shield");
        stat(r, "r_vaste_hand", Stats.van(Stat.KRITIEK, 5, Stat.AANVALSSNELHEID, 8), "minecraft:iron_sword");
        stat(r, "r_schildmeester", Stats.van(Stat.VERDEDIGING, 10, Stat.KRACHT, 6, Stat.KRITIEK, 4), "minecraft:iron_block");
        stat(r, "r_schildmuur", Stats.van(Stat.VERDEDIGING, 14, Stat.KRITIEKSCHADE, 15), "minecraft:deepslate_bricks");
        effect(r, "r_onbreekbaar", "minecraft:obsidian");
        stat(r, "r_zware_zwaai", Stats.van(Stat.KRACHT, 8, Stat.KRITIEKSCHADE, 12), "minecraft:heavy_core");
        stat(r, "r_reuzenkracht", Stats.van(Stat.KRACHT, 10, Stat.VADS, 120), "minecraft:white_wool");
        effect(r, "r_reuzenplof", "minecraft:slime_block");
        effect(r, "r_paladijnsaura", "minecraft:golden_apple");
        plekken(r,
                "r_schildarm 0 0 r_spec_schildridder", "r_knuffelspier 0 1 -", "r_aanloop 0 2 r_spec_reuzenkussen", "r_pluisvacht 0 3 -",
                "r_slijmkussen 1 0 r_schildarm", "r_taai 1 1 r_knuffelspier", "r_vuurkussen 1 2 r_aanloop", "r_doornknuffel 1 3 r_pluisvacht",
                "r_vaste_hand 2 0 r_slijmkussen", "r_schokgolf 2 1 r_taai", "r_zware_zwaai 2 2 r_vuurkussen", "r_beschermknuffel 2 3 r_doornknuffel",
                "r_schildmeester 3 0 r_vaste_hand", "r_kussenstorm 3 1 r_schokgolf", "r_reuzenkussen 3 2 r_zware_zwaai", "r_bolwerk 3 3 r_beschermknuffel",
                "r_ijsklap 4 0 r_schildmeester", "r_ijspantser 4 1 r_kussenstorm", "r_reuzenkracht 4 2 r_reuzenkussen", "r_onverzettelijk 4 3 r_bolwerk",
                "r_schildmuur 5 0 r_ijsklap", "r_kristalhart 5 1 r_ijspantser", "r_galmstoot 5 2 r_reuzenkracht", "r_ijzeren_knuffel 5 3 r_onverzettelijk",
                "r_lantaarnschild 6 0 r_schildmuur", "r_afstijgstorm 6 2 r_galmstoot", "r_schorshuid 6 3 r_ijzeren_knuffel",
                "r_onbreekbaar 7 0 r_lantaarnschild", "r_drakenkussen 7 1 r_kristalhart", "r_reuzenplof 7 2 r_afstijgstorm", "r_paladijnsaura 7 3 r_schorshuid");
        // ---- Pluisjager: Scherpschutter | stam | Strikkenzetter | Steun ------------------------------------------------------
        stat(j, "j_spanpees", Stats.van(Stat.KRITIEKSCHADE, 15, Stat.BEHENDIGHEID, 3), "minecraft:bow");
        stat(j, "j_sluipschutter", Stats.van(Stat.KRITIEK, 6, Stat.KRITIEKSCHADE, 20), "minecraft:target");
        stat(j, "j_strikdraad", Stats.van(Stat.BEHENDIGHEID, 4, Stat.AANVALSSNELHEID, 8), "minecraft:tripwire_hook");
        stat(j, "j_valstrikken", Stats.van(Stat.BEHENDIGHEID, 6, Stat.KRITIEK, 5, Stat.AANVALSSNELHEID, 6), "minecraft:cobweb");
        effect(j, "j_strikkenweb", "minecraft:string");
        stat(j, "j_pijlenkoker", Stats.van(Stat.BEHENDIGHEID, 5, Stat.KRITIEK, 4), "minecraft:tipped_arrow");
        stat(j, "j_zorgzaam", Stats.van(Stat.HEALKRACHT, 20, Stat.VADS, 150), "minecraft:golden_apple");
        plekken(j,
                "j_scherpoog 0 0 j_spec_scherpschutter", "j_snelle_pees 0 1 -", "j_strikdraad 0 2 j_spec_strikkenzetter", "j_lichtvoetig 0 3 -",
                "j_spanpees 1 0 j_scherpoog", "j_doorboor 1 1 j_snelle_pees", "j_vuurregen 1 2 j_strikdraad", "j_kleefregen 1 3 j_lichtvoetig",
                "j_valkenoog 2 0 j_spanpees", "j_pijlenkoker 2 1 j_doorboor", "j_plakbom 2 2 j_vuurregen", "j_hartjespijl 2 3 j_kleefregen",
                "j_vallende_ster 3 0 j_valkenoog", "j_jachtroes 3 1 j_pijlenkoker", "j_valstrikken 3 2 j_plakbom", "j_pluizenmantel 3 3 j_hartjespijl",
                "j_sluipschutter 4 0 j_vallende_ster", "j_jachtinstinct 4 1 j_jachtroes", "j_ijspijl 4 2 j_valstrikken", "j_poolvacht 4 3 j_pluizenmantel",
                "j_sterrenoog 5 0 j_sluipschutter", "j_windloper 5 1 j_jachtinstinct", "j_splinterregen 5 2 j_ijspijl", "j_zorgzaam 5 3 j_poolvacht",
                "j_zweefschot 6 0 j_sterrenoog", "j_sporenzoeker 6 2 j_splinterregen", "j_lichtpijl 6 3 j_zorgzaam",
                "j_sterrenzwerm 7 0 j_zweefschot", "j_strikkenweb 7 2 j_sporenzoeker");
        // ---- Vadsmagier: Sterrenwever | stam | Zegenzanger | Heel ------------------------------------------------------------
        stat(m, "m_sterrenvonk", Stats.van(Stat.ELEMENTSCHADE, 10, Stat.WIJSHEID, 3), "minecraft:blaze_rod");
        stat(m, "m_zangstem", Stats.van(Stat.WIJSHEID, 4, Stat.COOLDOWNREDUCTIE, 4), "minecraft:note_block");
        stat(m, "m_koorkracht", Stats.van(Stat.WIJSHEID, 8, Stat.VADS, 100), "minecraft:jukebox");
        effect(m, "m_slotlied", "minecraft:music_disc_otherside");
        stat(m, "m_warme_handen", Stats.van(Stat.HEALKRACHT, 10, Stat.VADS, 60), "minecraft:poppy");
        stat(m, "m_hartenbron", Stats.van(Stat.HEALKRACHT, 15, Stat.WIJSHEID, 4), "minecraft:heart_of_the_sea");
        plekken(m,
                "m_elementkenner 0 0 m_spec_sterrenwever", "m_wijs 0 1 -", "m_zangstem 0 2 m_spec_zegenzanger", "m_zachte_handjes 0 3 -",
                "m_reuzenbal 1 0 m_elementkenner", "m_glitterblink 1 1 m_wijs", "m_drievuur 1 2 m_zangstem", "m_warme_handen 1 3 m_zachte_handjes",
                "m_sterrenvonk 2 0 m_reuzenbal", "m_energievonk 2 1 m_glitterblink", "m_grote_bron 2 2 m_drievuur", "m_bronschild 2 3 m_warme_handen",
                "m_meteoorregen 3 0 m_sterrenvonk", "m_tijdsprong 3 1 m_energievonk", "m_kettingbolt 3 2 m_grote_bron", "m_hartenbron 3 3 m_bronschild",
                "m_ijsbollen 4 0 m_meteoorregen", "m_kristalgeest 4 1 m_tijdsprong", "m_koorkracht 4 2 m_kettingbolt", "m_heldere_bron 4 3 m_hartenbron",
                "m_ijsnova 5 0 m_ijsbollen", "m_manastroom 5 1 m_kristalgeest", "m_sterrenwijsheid 5 2 m_koorkracht",
                "m_lantaarnvlam 6 0 m_ijsnova", "m_wortelband 6 2 m_sterrenwijsheid", "m_blinkspoor 6 3 m_heldere_bron",
                "m_meteoorzwerm 7 0 m_lantaarnvlam", "m_slotlied 7 2 m_wortelband");
    }

    private static void stat(String klasse, String id, Stats bonus, String icoon) {
        Bomen.ICOON.put(id, icoon);
        NIEUW.put(id, new BoomKnoop(id, klasse, 1, List.of(), 0, 0, bonus, null));
    }

    private static void effect(String klasse, String id, String icoon) {
        Bomen.ICOON.put(id, icoon);
        NIEUW.put(id, new BoomKnoop(id, klasse, 1, List.of(), 0, 0, null, id));
    }

    private static void plekken(String klasse, String... regels) {
        List<Plek> l = new ArrayList<>();
        for (String r : regels) {
            String[] d = r.split(" ");
            l.add(new Plek(d[0], Integer.parseInt(d[1]), Integer.parseInt(d[2]), d[3]));
        }
        PLEKKEN.put(klasse, l);
    }

    /** The cost of a row (as before: 1, 2, 2, 3 in zone 1; 2, 3 in zone 2; 4, 4 in zone 3). */
    public static int kosten(int rij) {
        return switch (rij) {
            case 0 -> 1;
            case 3, 5 -> 3;
            case 6, 7, 8, 9 -> 4;   // (V-INT: rows 8/9 = the zone-6 rows)
            default -> 2;
        };
    }

    /** The class level a row needs (rows 0-3: none; spec columns wait for the spec node instead). */
    static int minLevel(int rij, String id) {
        return switch (rij) {
            case 4 -> 20;
            case 5 -> 26;
            case 6 -> Math.max(40, Boom.MIN_LEVEL.getOrDefault(id, 42));
            case 7 -> 48;
            default -> 0;
        };
    }

    /** Re-lays the three trees (once; idempotent). Call after every slice added its nodes. */
    public static synchronized void herindeel() {
        if (klaar) {
            return;
        }
        klaar = true;
        for (Map.Entry<String, List<Plek>> e : PLEKKEN.entrySet()) {
            Klasse k = Klassen.van(e.getKey());
            if (k == null) {
                continue;
            }
            List<BoomKnoop> boom = k.boom();
            for (Spec s : Spec.vanKlasse(k.id())) {
                Bomen.ICOON.put(s.knoop(), s.icoon);
                if (Boom.knoop(k, s.knoop()) == null) {
                    boom.add(new BoomKnoop(s.knoop(), k.id(), 0, List.of(), -1, s.links ? 0 : 2, null, s.knoop()));
                }
            }
            for (Plek p : e.getValue()) {
                BoomKnoop oud = Boom.knoop(k, p.id());
                if (oud == null) {
                    oud = NIEUW.get(p.id());
                    if (oud == null) {
                        LogUtils.getLogger().warn("U-SPECS: tree node {} is missing", p.id());
                        continue;
                    }
                    boom.add(oud);
                }
                BoomKnoop nieuw = new BoomKnoop(oud.id(), k.id(), kosten(p.rij()), p.vereist().equals("-") ? List.of() : List.of(p.vereist()),
                        p.rij(), p.kolom(), oud.bonus(), oud.variant());
                int lv = minLevel(p.rij(), p.id());
                if (lv > 0) {
                    Boom.MIN_LEVEL.put(p.id(), lv);
                } else {
                    Boom.MIN_LEVEL.remove(p.id());
                }
                boom.set(boom.indexOf(oud), nieuw);
            }
        }
        zijdeRijen();   // V-INT
        // the old either/or pairs now sit in different branches: only the Pluisjager's pair could meet (Vuurregen in
        // Strikkenzetter, Kleefregen in the Steun branch), and a Strikkenzetter may take both
        Bomen.nietUitgesloten("j_vuurregen", "j_kleefregen");
    }

    /**
     * V-INT (cap 70, u-z6verhaal): room for the 69 points of level 70. Every column gets its gaps in rows 6-7 filled with a stat
     * node, plus row 8 (lv 62) and row 9 (lv 66), 4 points each, so a spec + trunk + side branch hold more than 69 points (a real
     * choice up to the cap, nothing left unspent). Ids {@code <p>_z6_<kolom>_<rij>}; names from tools/features/vadsrijk_v_int.py.
     */
    private static void zijdeRijen() {
        Object[][] thema = {   // per column: the stat pair of its theme (spec A, trunk, spec B, side)
                {Stat.KRITIEK, Stat.KRITIEKSCHADE}, {Stat.VADS, Stat.AANVALSSNELHEID}, {Stat.KRITIEKSCHADE, Stat.AANVALSSNELHEID},
                {Stat.VERDEDIGING, Stat.HEALKRACHT}};
        for (String klasse : List.of(Klassen.RIDDER, Klassen.JAGER, Klassen.MAGIER)) {
            Klasse k = Klassen.van(klasse);
            if (k == null) {
                continue;
            }
            String pre = Spec.prefix(klasse);
            Stat hoofd = switch (klasse) {
                case Klassen.JAGER -> Stat.BEHENDIGHEID;
                case Klassen.MAGIER -> Stat.WIJSHEID;
                default -> Stat.KRACHT;
            };
            List<BoomKnoop> boom = k.boom();
            for (int kolom = 0; kolom < KOLOMMEN; kolom++) {
                for (int rij = 6; rij <= 9; rij++) {
                    BoomKnoop al = null;
                    for (BoomKnoop b : boom) {
                        if (b.rij() == rij && b.kolom() == kolom) {
                            al = b;
                        }
                    }
                    if (al != null) {
                        continue;
                    }
                    BoomKnoop onder = null;   // the node right below in this column (the requirement)
                    for (BoomKnoop b : boom) {
                        if (b.kolom() == kolom && b.rij() >= 0 && b.rij() < rij && (onder == null || b.rij() > onder.rij())) {
                            onder = b;
                        }
                    }
                    if (onder == null) {
                        continue;
                    }
                    String id = pre + "_z6_" + kolom + "_" + rij;
                    int n = rij - 5;   // 1..4: a bit more per row
                    Object[] t = thema[kolom];
                    Object[] waarden = kolom == 1 && t[0] == Stat.VADS
                            ? new Object[] {hoofd, 4 + 2 * n, Stat.VADS, 60 + 30 * n}
                            : new Object[] {hoofd, 4 + 2 * n, t[0], ((Stat) t[0]) == Stat.KRITIEK ? 2 + n : 8 + 4 * n, t[1], 4 + 2 * n};
                    int kosten = kosten(rij);
                    boom.add(new BoomKnoop(id, klasse, kosten, List.of(onder.id()), rij, kolom, Stats.van(waarden), null));
                    Bomen.ICOON.put(id, rij >= 8 ? "minecraft:amethyst_cluster" : "minecraft:glow_berries");
                    Boom.MIN_LEVEL.put(id, switch (rij) {
                        case 6 -> 42;
                        case 7 -> 48;
                        case 8 -> 62;
                        default -> 66;
                    });
                }
            }
        }
    }

    /** Which column a node is in (-1 = hidden or unknown). */
    public static int kolom(Klasse k, String id) {
        BoomKnoop b = Boom.knoop(k, id);
        return b == null || b.rij() < 0 ? -1 : b.kolom();
    }
}
