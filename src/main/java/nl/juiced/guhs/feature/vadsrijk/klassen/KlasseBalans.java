package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Vaardigheden;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * g-klassen: the balance knobs per class, in {@code config/guhs-vadsrijk-balans.toml} (server side, written with the defaults
 * the first time). Change them live with {@code /guhs vadsrijk balans <klasse> <sleutel> <waarde>} (also writes the file),
 * {@code balans toon}, {@code balans herlaad}; an edited file is picked up by itself within 5 seconds (no restart).
 * <p>Per class ({@code [knuffelridder]}, {@code [pluisjager]}, {@code [vadsmagier]}):
 * <ul>
 *   <li>{@code schade}: all damage the class deals (basic attacks and skills), x;</li>
 *   <li>{@code skillschade}: skill damage on top of that (everything that isn't a basic attack), x;</li>
 *   <li>{@code vads}, {@code verdediging}: max Vads / Verdediging while that class is active, x;</li>
 *   <li>{@code cooldowns}, {@code energie}: skill cooldowns / energy costs, x;</li>
 *   <li>{@code melee_reductie}: flat damage reduction (0..0.9) while this class is active with a melee weapon in the hand
 *   (Knuffelridder 0.20: melee is tougher);</li>
 *   <li>{@code vads_per_klasselevel}: extra max Vads per class level above 1 (Knuffelridder 4).</li>
 * </ul>
 * {@code [algemeen]}: {@code xp_schaal_generiek}, {@code xp_schaal_klasse} (the two xp curves, x).
 */
public final class KlasseBalans {
    public static final String BESTAND = "guhs-vadsrijk-balans.toml";
    public static final String ALGEMEEN = "algemeen";
    public static final List<String> SLEUTELS = List.of("schade", "skillschade", "vads", "verdediging", "cooldowns", "energie", "melee_reductie",
            "vads_per_klasselevel");
    public static final List<String> ALGEMEEN_SLEUTELS = List.of("xp_schaal_generiek", "xp_schaal_klasse");
    /** g-int: g-balans' kern {@code Balans} section in the same file ({@code [mobs] schade}, default 1.3). */
    public static final String MOBS = "mobs";
    public static final List<String> MOBS_SLEUTELS = List.of("schade");
    /** I-MOUNTS: the mount knobs ({@code [rijdieren]}; pushed to economie's {@code RijdierSoort.Balans}). */
    public static final String RIJDIEREN = "rijdieren";
    public static final List<String> RIJDIEREN_SLEUTELS = List.of("snelheid", "sprong", "groei_snelheid", "groei_sprong");

    private static final Map<String, Map<String, Double>> WAARDEN = new LinkedHashMap<>();
    private static long gelezen = Long.MIN_VALUE;
    private static int tik;
    /** Tests: never read/write the file. */
    static boolean zonderBestand;

    private KlasseBalans() {
    }

    /** The default of a key for a class. */
    public static double standaard(String klasse, String sleutel) {
        return switch (sleutel) {
            case "melee_reductie" -> Klassen.RIDDER.equals(klasse) ? 0.20 : 0;
            case "vads_per_klasselevel" -> Klassen.RIDDER.equals(klasse) ? 4 : 0;
            case "groei_snelheid" -> nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.GROEI_SNELHEID;
            case "groei_sprong" -> nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.GROEI_SPRONG;
            case "schade" -> MOBS.equals(klasse) ? nl.juiced.guhs.feature.vadsrijk.kern.Balans.MOB_SCHADE_STANDAARD : 1.0;
            default -> 1.0;
        };
    }

    private static List<String> secties() {
        List<String> l = new ArrayList<>();
        l.add(ALGEMEEN);
        l.add(MOBS);
        l.add(RIJDIEREN);
        for (var k : Klassen.alle()) {
            l.add(k.id());
        }
        return l;
    }

    private static List<String> sleutels(String sectie) {
        return ALGEMEEN.equals(sectie) ? ALGEMEEN_SLEUTELS : MOBS.equals(sectie) ? MOBS_SLEUTELS
                : RIJDIEREN.equals(sectie) ? RIJDIEREN_SLEUTELS : SLEUTELS;
    }

    public static synchronized double get(String klasse, String sleutel) {
        Map<String, Double> m = WAARDEN.get(klasse);
        Double v = m == null ? null : m.get(sleutel);
        return v != null ? v : standaard(klasse, sleutel);
    }

    /** Null when it worked, else what's wrong (unknown class/key). Writes the file when {@code opslaan}. */
    @Nullable
    public static synchronized String zet(String klasse, String sleutel, double waarde, boolean opslaan) {
        if (!secties().contains(klasse)) {
            return "onbekende klasse: " + klasse + " (" + String.join(", ", secties()) + ")";
        }
        if (!sleutels(klasse).contains(sleutel)) {
            return "onbekende sleutel: " + sleutel + " (" + String.join(", ", sleutels(klasse)) + ")";
        }
        WAARDEN.computeIfAbsent(klasse, x -> new LinkedHashMap<>()).put(sleutel, waarde);
        pasToe();
        if (opslaan) {
            schrijf();
        }
        return null;
    }

    /** Back to all defaults (tests). */
    public static synchronized void standaardAlles() {
        WAARDEN.clear();
        pasToe();
    }

    private static void pasToe() {
        Levels.SCHAAL_GENERIEK = Math.max(0.05, get(ALGEMEEN, "xp_schaal_generiek"));
        Levels.SCHAAL_KLASSE = Math.max(0.05, get(ALGEMEEN, "xp_schaal_klasse"));
        nl.juiced.guhs.feature.vadsrijk.kern.Balans.zetMobSchade(get(MOBS, "schade"));
        nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.Balans.zet(get(RIJDIEREN, "snelheid"), get(RIJDIEREN, "sprong"),
                get(RIJDIEREN, "groei_snelheid"), get(RIJDIEREN, "groei_sprong"));
    }

    // ---- the file ---------------------------------------------------------------------------------------------------------

    static Path pad() {
        return FMLPaths.CONFIGDIR.get().resolve(BESTAND);
    }

    /** Reads the file (writes it with the defaults when it isn't there). Returns a short report. */
    public static synchronized String laad() {
        if (zonderBestand) {
            return "zonder bestand";
        }
        Path p = pad();
        try {
            if (!Files.exists(p)) {
                schrijf();
                return "nieuw bestand met standaardwaarden: " + p;
            }
            WAARDEN.clear();
            String sectie = null;
            int n = 0;
            for (String regel : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                String r = regel.strip();
                int h = r.indexOf('#');
                if (h >= 0) {
                    r = r.substring(0, h).strip();
                }
                if (r.isEmpty()) {
                    continue;
                }
                if (r.startsWith("[") && r.endsWith("]")) {
                    sectie = r.substring(1, r.length() - 1).strip().replace("\"", "");
                    continue;
                }
                int is = r.indexOf('=');
                if (sectie == null || is < 0) {
                    continue;
                }
                String sl = r.substring(0, is).strip();
                try {
                    double v = Double.parseDouble(r.substring(is + 1).strip());
                    if (secties().contains(sectie) && sleutels(sectie).contains(sl)) {
                        WAARDEN.computeIfAbsent(sectie, x -> new LinkedHashMap<>()).put(sl, v);
                        n++;
                    }
                } catch (NumberFormatException e) {
                    LogUtils.getLogger().warn("Vadsrijk balans: '{}' in [{}] is geen getal", regel, sectie);
                }
            }
            pasToe();
            gelezen = Files.getLastModifiedTime(p).toMillis();
            return n + " waarden gelezen uit " + p;
        } catch (IOException e) {
            LogUtils.getLogger().warn("Vadsrijk balans: could not read {}", p, e);
            return "kon " + p + " niet lezen: " + e.getMessage();
        }
    }

    private static void schrijf() {
        if (zonderBestand) {
            return;
        }
        StringBuilder b = new StringBuilder();
        b.append("# Guhs Het Vadsrijk: balans per klasse. Live aanpassen: /guhs vadsrijk balans <klasse> <sleutel> <waarde>\n");
        b.append("# (of dit bestand bewerken: binnen 5 seconden opnieuw gelezen; /guhs vadsrijk balans herlaad kan ook).\n");
        b.append("# schade/skillschade/vads/verdediging/cooldowns/energie zijn vermenigvuldigers (1.0 = normaal).\n");
        b.append("# melee_reductie = minder schade (0.20 = 20 %) zolang deze klasse actief is met een melee-wapen.\n");
        b.append("# vads_per_klasselevel = extra max Vads per klasseniveau boven 1.\n");
        b.append("# [mobs] schade = vermenigvuldiger op de klappen van gewone wezens en elites (geen bazen); 1.3 = +30 %.\n");
        b.append("# [rijdieren] snelheid/sprong = vermenigvuldigers op alle rijdieren; groei_snelheid = extra snelheid per rijdierlevel\n");
        b.append("# (0.015 = +1,5 %), groei_sprong = extra sprongkracht per rijdierlevel (0.01 = ongeveer +0,07 blok).\n");
        for (String s : secties()) {
            b.append('\n').append('[').append(s).append("]\n");
            for (String k : sleutels(s)) {
                b.append(k).append(" = ").append(String.format(Locale.ROOT, "%s", get(s, k))).append('\n');
            }
        }
        Path p = pad();
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, b.toString(), StandardCharsets.UTF_8);
            gelezen = Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            LogUtils.getLogger().warn("Vadsrijk balans: could not write {}", p, e);
        }
    }

    /** Every 5 s: re-read the file when it changed on disk. */
    static void tick() {
        if (zonderBestand || ++tik % 100 != 0) {
            return;
        }
        try {
            Path p = pad();
            if (Files.exists(p) && Files.getLastModifiedTime(p).toMillis() != gelezen) {
                LogUtils.getLogger().info("Vadsrijk balans: {}", laad());
            }
        } catch (IOException e) {
            // ignore: next time
        }
    }

    /** All values, one line per section. */
    public static synchronized List<String> toon() {
        List<String> l = new ArrayList<>();
        for (String s : secties()) {
            StringBuilder b = new StringBuilder("[" + s + "] ");
            for (String k : sleutels(s)) {
                b.append(k).append('=').append(String.format(Locale.ROOT, "%.2f", get(s, k))).append(' ');
            }
            l.add(b.toString().strip());
        }
        return l;
    }

    // ---- the hooks --------------------------------------------------------------------------------------------------------

    static boolean basisaanval(@Nullable String skill) {
        return skill == null || skill.startsWith("basis") || skill.equals("kettingbolt");
    }

    /** Is the weapon you hold now (and may use) a melee weapon? */
    static boolean melee(ServerPlayer p) {
        Uitrusting u = Spelers.wapen(p);
        return u != null && Aanvalstempo.schadeSoort(u.basisType()) == Schade.Soort.MELEE;
    }

    /** The damage factor on a hit (attacker's class multipliers, defender's melee reduction). Pure for the tests. */
    public static double klapFactor(@Nullable String bronKlasse, @Nullable String skill, @Nullable String doelKlasse, boolean doelMelee) {
        double f = 1;
        if (bronKlasse != null) {
            f *= get(bronKlasse, "schade");
            if (!basisaanval(skill)) {
                f *= get(bronKlasse, "skillschade");
            }
        }
        if (doelKlasse != null && doelMelee) {
            f *= 1 - Math.max(0, Math.min(0.9, get(doelKlasse, "melee_reductie")));
        }
        return f;
    }

    static void installeer() {
        pasToe();
        Spelers.STATS_NA.add(KlasseBalans::stats);
        Schade.VOOR_KLAP.add((k, n) -> {
            String bron = k.bron() instanceof ServerPlayer bp ? Spelers.klasse(bp).id() : null;
            String doel = null;
            boolean melee = false;
            if (k.doel() instanceof ServerPlayer dp && k.soort() != Schade.Soort.WAAR) {
                doel = Spelers.klasse(dp).id();
                melee = melee(dp);
            }
            return bron == null && doel == null ? n : n * klapFactor(bron, k.skillId(), doel, melee);
        });
        Vaardigheden.COOLDOWN_MOD.add((p, v) -> {
            double f = get(Spelers.klasse(p).id(), "cooldowns");
            return f == 1 ? null : (float) f;
        });
        Vaardigheden.ENERGIE_MOD.add((p, v) -> {
            double f = get(Spelers.klasse(p).id(), "energie");
            return f == 1 ? null : (float) f;
        });
    }

    /** Vads per class level, then the vads/verdediging multipliers of the active class. */
    static void stats(ServerPlayer p, Stats s) {
        String k = Spelers.klasse(p).id();
        double perLv = get(k, "vads_per_klasselevel");
        if (perLv != 0) {
            s.plus(Stat.VADS, perLv * (Spelers.level(p, k) - 1));
        }
        double v = get(k, "vads"), d = get(k, "verdediging");
        if (v != 1) {
            s.zet(Stat.VADS, s.get(Stat.VADS) * v);
        }
        if (d != 1) {
            s.zet(Stat.VERDEDIGING, s.get(Stat.VERDEDIGING) * d);
        }
    }
}
