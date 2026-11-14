package nl.juiced.guhs.feature.vadsrijk.tijd;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import net.neoforged.fml.loading.FMLPaths;

/**
 * 1.10.1 (p1101-tijd): {@code config/guhs-vadsrijk-tijd.properties} (server side), the day/night cycle of the Vadsrijk only.
 * <ul>
 *   <li>{@code cyclus_minuten}: real minutes of one whole Vadsrijk day+night ({@link #MIN_CYCLUS}..{@link #MAX_CYCLUS}, default
 *       {@link #STANDAARD_CYCLUS} = vanilla);</li>
 *   <li>{@code dag_deel}: the share of the cycle that is day ({@link #MIN_DAG}..{@link #MAX_DAG}, default {@link #STANDAARD_DAG}
 *       = 2/3 day, 1/3 dusk + night + dawn).</li>
 * </ul>
 * Read at server start (missing file: written with the defaults); live with {@code /guhs vadsrijk tijd cyclus|dag} (saved) or
 * {@code /guhs vadsrijk tijd herlaad} after editing the file. Applied every tick by {@link VadsrijkTijd#pas}.
 */
public final class TijdConfig {
    public static final String BESTAND = "guhs-vadsrijk-tijd.properties";
    public static final double STANDAARD_CYCLUS = 20, MIN_CYCLUS = 1, MAX_CYCLUS = 240;
    public static final double STANDAARD_DAG = 0.667, MIN_DAG = 0.3, MAX_DAG = 0.9;
    private static double cyclus = STANDAARD_CYCLUS, dagDeel = STANDAARD_DAG;
    private static boolean gelezen;

    private TijdConfig() {
    }

    public static double cyclus() {
        if (!gelezen) {
            lees();
        }
        return cyclus;
    }

    public static double dagDeel() {
        if (!gelezen) {
            lees();
        }
        return dagDeel;
    }

    static double klemCyclus(double c) {
        return Double.isNaN(c) ? STANDAARD_CYCLUS : Math.max(MIN_CYCLUS, Math.min(MAX_CYCLUS, c));
    }

    static double klemDag(double d) {
        return Double.isNaN(d) ? STANDAARD_DAG : Math.max(MIN_DAG, Math.min(MAX_DAG, d));
    }

    /** (Re)reads the file; a missing file is written with the current (default) values. */
    public static void lees() {
        gelezen = true;
        Path p = FMLPaths.CONFIGDIR.get().resolve(BESTAND);
        if (!Files.exists(p)) {
            schrijf();
            return;
        }
        Properties pr = new Properties();
        try (Reader r = Files.newBufferedReader(p)) {
            pr.load(r);
            cyclus = klemCyclus(getal(pr.getProperty("cyclus_minuten"), STANDAARD_CYCLUS));
            dagDeel = klemDag(getal(pr.getProperty("dag_deel"), STANDAARD_DAG));
        } catch (IOException e) {
            cyclus = STANDAARD_CYCLUS;
            dagDeel = STANDAARD_DAG;
        }
    }

    private static double getal(String s, double anders) {
        if (s == null) {
            return anders;
        }
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return anders;
        }
    }

    static void schrijf() {
        Properties pr = new Properties();
        pr.setProperty("cyclus_minuten", tekst(cyclus));
        pr.setProperty("dag_deel", tekst(dagDeel));
        Path p = FMLPaths.CONFIGDIR.get().resolve(BESTAND);
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                pr.store(w, "Guhs 1.10.1 Het Vadsrijk: day/night of the Vadsrijk dimension only. cyclus_minuten = real minutes of one day+night"
                        + " (1..240, vanilla 20); dag_deel = share that is day (0.3..0.9; 0.667 = 2/3 day, 1/3 dusk+night+dawn)."
                        + " Live: /guhs vadsrijk tijd cyclus <min> | dag <deel> | herlaad");
            }
        } catch (IOException ignored) {
            // read-only config dir: keep the values in memory
        }
    }

    /** Set (clamped); {@code opslaan} writes the file (the commands), tests keep it in memory. */
    public static void zet(double nieuweCyclus, double nieuwDagDeel, boolean opslaan) {
        gelezen = true;
        cyclus = klemCyclus(nieuweCyclus);
        dagDeel = klemDag(nieuwDagDeel);
        if (opslaan) {
            schrijf();
        }
    }

    /** 20 -> "20", 12.5 -> "12.5", 0.667 -> "0.667". */
    public static String tekst(double d) {
        String s = String.format(Locale.ROOT, "%.3f", d);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }
}
