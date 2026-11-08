package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * U-OPSLAGVIP: the price curve of the BOUGHT storage expansions, section {@code [opslag]} of
 * {@code config/guhs-vadsrijk-balans.toml} (read/written by klassen's {@code KlasseBalans}, live reload).
 * <ul>
 *   <li>{@code max_koopbaar}: how many expansions per storage can be bought with coins (default 1: only the FIRST extra knapzak
 *   tab and the FIRST extra bank tab). Every further one shows as a locked "Binnenkort!" tab. Raise it later: no code needed.</li>
 *   <li>{@code knapzak_basis}, {@code knapzak_factor}: price of the n-th bought knapzak tab (n = 0, 1, ...) = basis x factor^n
 *   (brons; default 2500 = 25 zilver, the old price, x4 per step).</li>
 *   <li>{@code bank_basis}, {@code bank_factor}: same for the bank (default 1000 = 10 zilver, the old first price, x4).</li>
 * </ul>
 * The factors are placeholders: the user picks the real exponential numbers later.
 */
public final class OpslagBalans {
    private static final Map<String, double[]> KNOPPEN = new LinkedHashMap<>();

    static {
        knop("max_koopbaar", 1, 0, 10);
        knop("knapzak_basis", 2500, 0, 1e12);
        knop("knapzak_factor", 4, 1, 100);
        knop("bank_basis", 1000, 0, 1e12);
        knop("bank_factor", 4, 1, 100);
    }

    public static final List<String> SLEUTELS = List.copyOf(KNOPPEN.keySet());
    private static final Map<String, Double> WAARDEN = new java.util.concurrent.ConcurrentHashMap<>();

    private OpslagBalans() {
    }

    private static void knop(String k, double standaard, double min, double max) {
        KNOPPEN.put(k, new double[] {standaard, min, max});
    }

    public static double standaard(String k) {
        double[] d = KNOPPEN.get(k);
        return d == null ? 1.0 : d[0];
    }

    public static double get(String k) {
        Double v = WAARDEN.get(k);
        return v != null ? v : standaard(k);
    }

    /** Sets one knob in memory (clamped). Use KlasseBalans.zet(...) to also write the file. */
    public static void zet(String k, double v) {
        double[] d = KNOPPEN.get(k);
        if (d != null) {
            WAARDEN.put(k, Math.max(d[1], Math.min(d[2], v)));
        }
    }

    /** From KlasseBalans (file / command): every knob at once. */
    public static void zet(ToDoubleFunction<String> bron) {
        for (String k : SLEUTELS) {
            zet(k, bron.applyAsDouble(k));
        }
    }

    /** Back to the defaults (tests). */
    public static void standaardAlles() {
        WAARDEN.clear();
    }

    /** How many expansions per storage may be bought with coins. */
    public static int maxKoopbaar() {
        return (int) Math.round(get("max_koopbaar"));
    }

    /** Price (brons) of the n-th bought expansion (n = 0 for the first): basis x factor^n, rounded. */
    public static long prijs(String opslag, int n) {
        double basis = get(opslag + "_basis"), factor = get(opslag + "_factor");
        double p = basis * Math.pow(factor, Math.max(0, n));
        return p >= Long.MAX_VALUE / 2.0 ? Long.MAX_VALUE / 2 : Math.round(p);
    }

    public static long knapzakPrijs(int n) {
        return prijs("knapzak", n);
    }

    public static long bankPrijs(int n) {
        return prijs("bank", n);
    }
}
