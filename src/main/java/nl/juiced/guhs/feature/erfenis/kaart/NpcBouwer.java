package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.erfenis.Plot;

/**
 * De Erfenis, wereldkaart: builds and grows the building plan of an NPC village. Pure data (no world), deterministic for a
 * {@link Random}. The plan follows the base rules: exactly one Hoeve (in the middle), every footprint inside the plot and
 * free of the others, per type at most {@code maxAantal(hoeve)} and every level at most {@code maxLevel(hoeve)}
 * ({@link #fouten} checks that). The core stamps such a plan as a real base ({@code Base.npc(..., List<Plan>, ...)}).
 */
public final class NpcBouwer {
    /** Keep the outermost tiles free (the path around the plot). */
    private static final int RAND = 1;

    /** What one growth step did (for the news). */
    public record Groei(Soort soort, @Nullable KaartGebouwType type, int level) {
        public enum Soort { NIKS, NIEUW, VERBETER, HOEVE }
    }

    private NpcBouwer() {
    }

    /** The plot size in tiles (the core's). */
    public static int plot() {
        return Plot.MAAT;
    }

    /** The Hoeve's corner: the middle of the plot. */
    public static int midden() {
        return plot() / 2 - KaartGebouwType.HOEVE.breedte / 2;
    }

    // =====================================================================================================================
    // a new village
    // =====================================================================================================================

    /** A fresh village plan at this Hoeve level: the Hoeve in the middle and the other types partly filled in around it. */
    public static List<KaartGebouw> maak(int hoeve, Random rng) {
        int h = KaartGebouwType.clamp(hoeve);
        List<KaartGebouw> plan = new ArrayList<>();
        plan.add(new KaartGebouw(KaartGebouwType.HOEVE, h, midden(), midden(), rng.nextInt(4)));
        for (KaartGebouwType t : KaartGebouwType.values()) {
            int max = t.maxAantal(h);
            if (t == KaartGebouwType.HOEVE || max == 0) {
                continue;
            }
            int n = Math.max(1, (int) Math.round(max * (0.5 + rng.nextDouble() * 0.5)));
            for (int i = 0; i < n; i++) {
                if (!zetNieuw(plan, t, Math.max(1, Math.min(t.maxLevel(h), h - rng.nextInt(3))), rng)) {
                    break;
                }
            }
        }
        return plan;
    }

    /** Places one new building of type t at a free spot (a tile of room around it). False: no room. */
    private static boolean zetNieuw(List<KaartGebouw> plan, KaartGebouwType t, int level, Random rng) {
        int rot = t.vierkant() ? rng.nextInt(4) : 2 * rng.nextInt(2);
        int w = rot % 2 == 0 ? t.breedte : t.diepte, d = rot % 2 == 0 ? t.diepte : t.breedte;
        int p = plot();
        if (w + 2 * RAND > p || d + 2 * RAND > p) {
            return false;
        }
        for (int poging = 0; poging < 300; poging++) {
            int x = RAND + rng.nextInt(p - 2 * RAND - w + 1), z = RAND + rng.nextInt(p - 2 * RAND - d + 1);
            if (vrij(plan, x - 1, z - 1, w + 2, d + 2, true) && vrij(plan, x, z, w, d, false)) {
                plan.add(new KaartGebouw(t, level, x, z, rot));
                return true;
            }
        }
        return false;
    }

    // =====================================================================================================================
    // growing
    // =====================================================================================================================

    /** One growth step within the Hoeve level: a new building (when allowed and there is room) or one upgrade. In place. */
    public static Groei groei(List<KaartGebouw> plan, Random rng) {
        int h = hoeveLevel(plan);
        List<KaartGebouwType> nieuw = new ArrayList<>();
        for (KaartGebouwType t : KaartGebouwType.values()) {
            if (t != KaartGebouwType.HOEVE && aantal(plan, t) < t.maxAantal(h)) {
                nieuw.add(t);
            }
        }
        List<Integer> verbeter = new ArrayList<>();
        for (int i = 0; i < plan.size(); i++) {
            KaartGebouw g = plan.get(i);
            if (g.type() != KaartGebouwType.HOEVE && g.level() < g.type().maxLevel(h)) {
                verbeter.add(i);
            }
        }
        if (!nieuw.isEmpty() && (verbeter.isEmpty() || rng.nextInt(5) < 2)) {
            KaartGebouwType t = nieuw.get(rng.nextInt(nieuw.size()));
            if (zetNieuw(plan, t, 1, rng)) {
                return new Groei(Groei.Soort.NIEUW, t, 1);
            }
        }
        if (verbeter.isEmpty()) {
            return new Groei(Groei.Soort.NIKS, null, 0);
        }
        int i = verbeter.get(rng.nextInt(verbeter.size()));
        KaartGebouw g = plan.get(i);
        plan.set(i, g.metLevel(g.level() + 1));
        return new Groei(Groei.Soort.VERBETER, g.type(), g.level() + 1);
    }

    /**
     * The Hoeve to {@code doel}: going up keeps the layout (and a few growth steps fill the new room); going down (only a
     * reset base or a migrated POC village) plans the village anew at that level.
     */
    public static Groei zetHoeve(List<KaartGebouw> plan, int doel, Random rng) {
        int h = hoeveLevel(plan), naar = KaartGebouwType.clamp(doel);
        boolean heeftHoeve = index(plan, KaartGebouwType.HOEVE) >= 0;
        if (naar == h && heeftHoeve) {
            return new Groei(Groei.Soort.NIKS, null, 0);
        }
        if (naar < h || !heeftHoeve) {
            plan.clear();
            plan.addAll(maak(naar, rng));
            return new Groei(Groei.Soort.HOEVE, KaartGebouwType.HOEVE, naar);
        }
        int i = index(plan, KaartGebouwType.HOEVE);
        plan.set(i, plan.get(i).metLevel(naar));
        for (int s = 0; s < 3; s++) {
            groei(plan, rng);
        }
        return new Groei(Groei.Soort.HOEVE, KaartGebouwType.HOEVE, naar);
    }

    // =====================================================================================================================
    // what follows from a plan
    // =====================================================================================================================

    public static int hoeveLevel(List<KaartGebouw> plan) {
        for (KaartGebouw g : plan) {
            if (g.type() == KaartGebouwType.HOEVE) {
                return Math.max(1, g.level());
            }
        }
        return 1;
    }

    /** Knusheid = the points on the map. */
    public static int knusheid(List<KaartGebouw> plan) {
        int k = 0;
        for (KaartGebouw g : plan) {
            k += g.type().knus(g.level());
        }
        return k;
    }

    public static int aantal(List<KaartGebouw> plan, KaartGebouwType t) {
        int n = 0;
        for (KaartGebouw g : plan) {
            if (g.type() == t) {
                n++;
            }
        }
        return n;
    }

    private static int index(List<KaartGebouw> plan, KaartGebouwType t) {
        for (int i = 0; i < plan.size(); i++) {
            if (plan.get(i).type() == t) {
                return i;
            }
        }
        return -1;
    }

    /** Free of every building (and, unless {@code buitenMag}, inside the plot minus its rim). */
    public static boolean vrij(List<KaartGebouw> plan, int x, int z, int w, int d, boolean buitenMag) {
        if (!buitenMag && (x < RAND || z < RAND || x + w > plot() - RAND || z + d > plot() - RAND)) {
            return false;
        }
        for (KaartGebouw g : plan) {
            if (g.overlapt(x, z, w, d)) {
                return false;
            }
        }
        return true;
    }

    /** Everything wrong with a plan (empty = a valid base): one Hoeve, inside the plot, no overlaps, limits per Hoeve level. */
    public static List<String> fouten(List<KaartGebouw> plan) {
        List<String> f = new ArrayList<>();
        if (aantal(plan, KaartGebouwType.HOEVE) != 1) {
            f.add("hoeves: " + aantal(plan, KaartGebouwType.HOEVE));
        }
        int h = hoeveLevel(plan);
        if (h > KaartGebouwType.MAX_HOEVE) {
            f.add("hoeve level " + h);
        }
        for (int i = 0; i < plan.size(); i++) {
            KaartGebouw g = plan.get(i);
            if (g.x() < 0 || g.z() < 0 || g.x() + g.breedte() > plot() || g.z() + g.diepte() > plot()) {
                f.add("buiten: " + g);
            }
            if (g.level() < 1 || g.level() > g.type().maxLevel(h)) {
                f.add("level: " + g + " (hoeve " + h + ")");
            }
            if (!g.type().vierkant() && g.rot() % 2 != 0) {
                f.add("scheef gedraaid: " + g);
            }
            for (int j = 0; j < i; j++) {
                if (plan.get(j).overlapt(g.x(), g.z(), g.breedte(), g.diepte())) {
                    f.add("overlap: " + plan.get(j) + " / " + g);
                }
            }
        }
        for (KaartGebouwType t : KaartGebouwType.values()) {
            if (aantal(plan, t) > t.maxAantal(h)) {
                f.add("te veel " + t.id + ": " + aantal(plan, t) + " > " + t.maxAantal(h));
            }
        }
        return f;
    }
}
