package nl.juiced.guhs.feature.vadsrijk.klassen.boom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;

/**
 * The three skill trees (ronde 12: ~12 nodes per class, every node really useful; 19 points at lv 20, so you can't take all).
 * Layout per class, 4 rows x 3 columns:
 * <ul>
 *   <li>row 0 (1 point): two stat passives and a skill tweak;</li>
 *   <li>row 1 (2 points): the two variants of the R skill (you pick ONE of them) and a G/V tweak;</li>
 *   <li>row 2 (2 points): stronger tweaks and support;</li>
 *   <li>row 3 (3 points): the capstones.</li>
 * </ul>
 * Everything bought costs 3 + 4 + 6 + 9 = 22 points (the variant pair counts once), so at the cap you still leave one capstone
 * or two smaller nodes out. Job advancement (lv 10/20) adds hidden nodes {@code <x>_adv1}/{@code <x>_adv2} (row -1, cost 0):
 * they are never refunded by a respec and carry the advancement's stat bonus.
 * <p>{@code vereist} entries may hold alternatives "a|b" (one of them is enough).
 */
public final class Bomen {
    /** U-SPECS: 4 columns (spec A, trunk, spec B, side branch; see klassen.spec.SpecBoom, which re-lays these nodes). */
    public static final int RIJEN = 4, KOLOMMEN = 4;

    /** Node id -> the node ids it excludes (the two R variants). */
    static final Map<String, Set<String>> UITGESLOTEN = new HashMap<>();
    /** Node id -> an item id for its icon in the tree screen. */
    public static final Map<String, String> ICOON = new HashMap<>();

    private Bomen() {
    }

    public static List<BoomKnoop> ridder() {
        String k = Klassen.RIDDER;
        List<BoomKnoop> l = new ArrayList<>();
        l.add(stat(k, "r_pluisvacht", 1, 0, 0, Stats.van(Stat.VADS, 80, Stat.VERDEDIGING, 5), "minecraft:pink_wool"));
        l.add(stat(k, "r_knuffelspier", 1, 0, 1, Stats.van(Stat.KRACHT, 5, Stat.KRITIEKSCHADE, 10), "minecraft:iron_ingot"));
        l.add(var(k, "r_aanloop", 1, 0, 2, List.of(), "minecraft:feather"));
        l.add(var(k, "r_vuurkussen", 2, 1, 0, List.of("r_knuffelspier"), "minecraft:blaze_powder"));
        l.add(var(k, "r_slijmkussen", 2, 1, 1, List.of("r_knuffelspier"), "minecraft:slime_ball"));
        l.add(var(k, "r_doornknuffel", 2, 1, 2, List.of("r_pluisvacht"), "minecraft:sweet_berries"));
        l.add(var(k, "r_schokgolf", 2, 2, 0, List.of("r_aanloop"), "minecraft:mace"));
        l.add(var(k, "r_taai", 2, 2, 1, List.of("r_pluisvacht"), "minecraft:turtle_scute"));
        l.add(var(k, "r_beschermknuffel", 2, 2, 2, List.of("r_doornknuffel"), "minecraft:shield"));
        l.add(var(k, "r_reuzenkussen", 3, 3, 0, List.of("r_schokgolf"), "minecraft:white_bed"));
        l.add(var(k, "r_kussenstorm", 3, 3, 1, List.of("r_vuurkussen|r_slijmkussen"), "minecraft:wind_charge"));
        l.add(var(k, "r_bolwerk", 3, 3, 2, List.of("r_beschermknuffel"), "minecraft:totem_of_undying"));
        l.add(adv(k, "r_adv1", Stats.van(Stat.VERDEDIGING, 6, Stat.KRACHT, 3)));
        l.add(adv(k, "r_adv2", Stats.van(Stat.VADS, 120, Stat.HEALKRACHT, 10)));
        sluitUit("r_vuurkussen", "r_slijmkussen");
        return l;
    }

    public static List<BoomKnoop> jager() {
        String k = Klassen.JAGER;
        List<BoomKnoop> l = new ArrayList<>();
        l.add(stat(k, "j_scherpoog", 1, 0, 0, Stats.van(Stat.KRITIEK, 6, Stat.KRITIEKSCHADE, 10), "minecraft:spyglass"));
        l.add(stat(k, "j_lichtvoetig", 1, 0, 1, Stats.van(Stat.LOOPSNELHEID, 10, Stat.VADS, 40), "minecraft:rabbit_foot"));
        l.add(stat(k, "j_snelle_pees", 1, 0, 2, Stats.van(Stat.AANVALSSNELHEID, 15), "minecraft:string"));
        l.add(var(k, "j_vuurregen", 2, 1, 0, List.of("j_scherpoog"), "minecraft:blaze_powder"));
        l.add(var(k, "j_kleefregen", 2, 1, 1, List.of("j_scherpoog"), "minecraft:slime_ball"));
        l.add(var(k, "j_doorboor", 2, 1, 2, List.of("j_snelle_pees"), "minecraft:spectral_arrow"));
        l.add(var(k, "j_valkenoog", 2, 2, 0, List.of("j_scherpoog"), "minecraft:ender_eye"));
        l.add(var(k, "j_plakbom", 2, 2, 1, List.of("j_lichtvoetig"), "minecraft:honey_bottle"));
        l.add(var(k, "j_hartjespijl", 2, 2, 2, List.of("j_lichtvoetig"), "minecraft:glistering_melon_slice"));
        l.add(var(k, "j_vallende_ster", 3, 3, 0, List.of("j_valkenoog"), "minecraft:nether_star"));
        l.add(var(k, "j_pluizenmantel", 3, 3, 1, List.of("j_plakbom"), "minecraft:phantom_membrane"));
        l.add(var(k, "j_jachtroes", 3, 3, 2, List.of("j_doorboor"), "minecraft:crossbow"));
        l.add(adv(k, "j_adv1", Stats.van(Stat.BEHENDIGHEID, 4, Stat.LOOPSNELHEID, 5)));
        l.add(adv(k, "j_adv2", Stats.van(Stat.KRITIEK, 8, Stat.KRITIEKSCHADE, 15)));
        sluitUit("j_vuurregen", "j_kleefregen");
        return l;
    }

    public static List<BoomKnoop> magier() {
        String k = Klassen.MAGIER;
        List<BoomKnoop> l = new ArrayList<>();
        l.add(stat(k, "m_wijs", 1, 0, 0, Stats.van(Stat.WIJSHEID, 6), "minecraft:book"));
        l.add(stat(k, "m_elementkenner", 1, 0, 1, Stats.van(Stat.ELEMENTSCHADE, 12), "minecraft:prismarine_crystals"));
        l.add(stat(k, "m_zachte_handjes", 1, 0, 2, Stats.van(Stat.HEALKRACHT, 15, Stat.VADS, 40), "minecraft:pink_petals"));
        l.add(var(k, "m_reuzenbal", 2, 1, 0, List.of("m_elementkenner"), "minecraft:fire_charge"));
        l.add(var(k, "m_drievuur", 2, 1, 1, List.of("m_elementkenner"), "minecraft:firework_star"));
        l.add(var(k, "m_glitterblink", 2, 1, 2, List.of("m_wijs"), "minecraft:ender_pearl"));
        l.add(var(k, "m_energievonk", 2, 2, 0, List.of("m_wijs"), "minecraft:glowstone_dust"));
        l.add(var(k, "m_grote_bron", 2, 2, 1, List.of("m_zachte_handjes"), "minecraft:beacon"));
        l.add(var(k, "m_bronschild", 2, 2, 2, List.of("m_zachte_handjes"), "minecraft:amethyst_shard"));
        l.add(var(k, "m_kettingbolt", 3, 3, 0, List.of("m_energievonk"), "minecraft:lightning_rod"));
        l.add(var(k, "m_meteoorregen", 3, 3, 1, List.of("m_reuzenbal|m_drievuur"), "minecraft:magma_block"));
        l.add(var(k, "m_tijdsprong", 3, 3, 2, List.of("m_glitterblink"), "minecraft:clock"));
        l.add(adv(k, "m_adv1", Stats.van(Stat.WIJSHEID, 4, Stat.VADS, 40)));
        l.add(adv(k, "m_adv2", Stats.van(Stat.COOLDOWNREDUCTIE, 10, Stat.ELEMENTSCHADE, 8)));
        sluitUit("m_reuzenbal", "m_drievuur");
        return l;
    }

    private static BoomKnoop stat(String klasse, String id, int kosten, int rij, int kolom, Stats bonus, String icoon) {
        ICOON.put(id, icoon);
        return new BoomKnoop(id, klasse, kosten, List.of(), rij, kolom, bonus, null);
    }

    private static BoomKnoop var(String klasse, String id, int kosten, int rij, int kolom, List<String> vereist, String icoon) {
        ICOON.put(id, icoon);
        return new BoomKnoop(id, klasse, kosten, vereist, rij, kolom, null, id);
    }

    private static BoomKnoop adv(String klasse, String id, Stats bonus) {
        ICOON.put(id, "minecraft:nether_star");
        return new BoomKnoop(id, klasse, 0, List.of(), -1, id.endsWith("1") ? 0 : 1, bonus, id);
    }

    private static void sluitUit(String a, String b) {
        UITGESLOTEN.computeIfAbsent(a, x -> new java.util.HashSet<>()).add(b);
        UITGESLOTEN.computeIfAbsent(b, x -> new java.util.HashSet<>()).add(a);
    }

    /** U-SPECS: two nodes no longer exclude each other (they sit in different branches now). */
    public static void nietUitgesloten(String a, String b) {
        Set<String> x = UITGESLOTEN.get(a), y = UITGESLOTEN.get(b);
        if (x != null) {
            x.remove(b);
        }
        if (y != null) {
            y.remove(a);
        }
    }

    public static Set<String> uitgesloten(String id) {
        return UITGESLOTEN.getOrDefault(id, Set.of());
    }

    /** The hidden advancement node of this class for step 1 or 2. */
    public static String advKnoop(String klasse, int stap) {
        String p = switch (klasse) {
            case Klassen.JAGER -> "j";
            case Klassen.MAGIER -> "m";
            default -> "r";
        };
        return p + "_adv" + stap;
    }
}
