package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

/**
 * De Erfenis, wereldkaart: builds and grows the building plan of an NPC village. Pure data (no world), deterministic for a
 * {@link Random}. The plan follows the base rules: exactly one stadhuis, every footprint inside the 64x64 plot and free of
 * the others, per type at most {@code maxAantal(stadhuis)} and every level at most {@code maxLevel(stadhuis)}
 * ({@link #fouten} checks that). The core stamps such a plan as a real base ({@code Base.npc(..., List<Plan>, ...)}).
 */
public final class NpcBouwer {
    /** The stadhuis' corner: the middle of the plot, as the core's own bases. */
    public static final int MIDDEN = 30;
    /** Keep the outermost tiles free (the path around the plot). */
    private static final int RAND = 1;

    /** What one growth step did (for the news). {@code type} null = nothing could grow. */
    public record Groei(Soort soort, @Nullable KaartGebouwType type, int level, int aantal) {
        public enum Soort { NIKS, NIEUW, VERBETER, STADHUIS }
    }

    private NpcBouwer() {
    }

    // =====================================================================================================================
    // a new village
    // =====================================================================================================================

    /** A fresh village plan at this stadhuis level: a ring of walls (with a gate), and the other types partly filled in. */
    public static List<KaartGebouw> maak(int stadhuisLevel, Random rng) {
        int th = Math.max(1, Math.min(KaartGebouwType.MAX_STADHUIS, stadhuisLevel));
        List<KaartGebouw> plan = new ArrayList<>();
        plan.add(new KaartGebouw(KaartGebouwType.STADHUIS, th, MIDDEN, MIDDEN, rng.nextInt(4)));
        // walls: (a part of) a ring around the middle, the gate to the south
        int r = 9 + th, muren = KaartGebouwType.MUUR.maxAantal(th) * (5 + rng.nextInt(6)) / 10;
        int muurLevel = Math.max(1, KaartGebouwType.MUUR.maxLevel(th) - rng.nextInt(2));
        for (int[] p : ring(r)) {
            if (aantal(plan, KaartGebouwType.MUUR) >= muren) {
                break;
            }
            if (!(Math.abs(p[0] - 32) <= 1 && p[1] == 32 + r) && vrij(plan, p[0], p[1], 1)) {
                plan.add(new KaartGebouw(KaartGebouwType.MUUR, muurLevel, p[0], p[1], 0));
            }
        }
        for (KaartGebouwType t : new KaartGebouwType[] {KaartGebouwType.MOLEN, KaartGebouwType.SCHUURTJE, KaartGebouwType.BOOMPJE}) {
            int max = t.maxAantal(th);
            int n = max == 0 ? 0 : Math.max(t == KaartGebouwType.MOLEN ? 1 : 0, (int) Math.round(max * (0.4 + rng.nextDouble() * 0.6)));
            for (int i = 0; i < n; i++) {
                int[] plek = vrijePlek(plan, t.maat, rng, t == KaartGebouwType.BOOMPJE ? 0 : r - 1);
                if (plek == null) {
                    break;
                }
                int lvl = 1 + rng.nextInt(Math.max(1, t.maxLevel(th)));
                plan.add(new KaartGebouw(t, lvl, plek[0], plek[1], rng.nextInt(4)));
            }
        }
        return plan;
    }

    /** The tiles of a square ring of radius r around the plot's middle (32, 32), north side first, then the sides, south last. */
    private static List<int[]> ring(int r) {
        List<int[]> ring = new ArrayList<>();
        for (int i = -r; i <= r; i++) {
            ring.add(new int[] {32 + i, 32 - r});
        }
        for (int i = -r + 1; i < r; i++) {
            ring.add(new int[] {32 - r, 32 + i});
            ring.add(new int[] {32 + r, 32 + i});
        }
        for (int i = -r; i <= r; i++) {
            ring.add(new int[] {32 + i, 32 + r});
        }
        return ring;
    }

    // =====================================================================================================================
    // growing
    // =====================================================================================================================

    /**
     * One growth step: a new building, an upgrade, or (more likely the fuller the village is) the stadhuis one level up.
     * Changes {@code plan} in place; returns what happened.
     */
    public static Groei groei(List<KaartGebouw> plan, Random rng) {
        int th = stadhuisLevel(plan);
        double vol = volheid(plan);
        List<KaartGebouwType> nieuw = new ArrayList<>();
        for (KaartGebouwType t : KaartGebouwType.values()) {
            if (t != KaartGebouwType.STADHUIS && aantal(plan, t) < t.maxAantal(th)) {
                nieuw.add(t);
            }
        }
        List<Integer> verbeter = new ArrayList<>();
        for (int i = 0; i < plan.size(); i++) {
            KaartGebouw g = plan.get(i);
            if (g.type() != KaartGebouwType.STADHUIS && g.level() < g.type().maxLevel(th)) {
                verbeter.add(i);
            }
        }
        double wStad = th < KaartGebouwType.MAX_STADHUIS ? 0.3 + 6 * vol * vol : 0;
        double wNieuw = nieuw.isEmpty() ? 0 : 2.5;
        double wVerbeter = verbeter.isEmpty() ? 0 : 2;
        double totaal = wStad + wNieuw + wVerbeter;
        if (totaal <= 0) {
            return new Groei(Groei.Soort.NIKS, null, 0, 0);
        }
        double k = rng.nextDouble() * totaal;
        if (k < wStad) {
            int i = index(plan, KaartGebouwType.STADHUIS);
            plan.set(i, plan.get(i).metLevel(th + 1));
            return new Groei(Groei.Soort.STADHUIS, KaartGebouwType.STADHUIS, th + 1, 1);
        }
        if (k < wStad + wNieuw) {
            KaartGebouwType t = nieuw.get(rng.nextInt(nieuw.size()));
            if (t == KaartGebouwType.MUUR) {
                int n = muurStukje(plan, th, rng);
                if (n > 0) {
                    return new Groei(Groei.Soort.NIEUW, t, 1, n);
                }
            } else {
                int[] plek = vrijePlek(plan, t.maat, rng, 0);
                if (plek != null) {
                    plan.add(new KaartGebouw(t, 1, plek[0], plek[1], rng.nextInt(4)));
                    return new Groei(Groei.Soort.NIEUW, t, 1, 1);
                }
            }
            if (verbeter.isEmpty()) {
                return new Groei(Groei.Soort.NIKS, null, 0, 0);
            }
        }
        if (verbeter.isEmpty()) {
            return new Groei(Groei.Soort.NIKS, null, 0, 0);
        }
        // upgrade: walls go up together (a whole stretch at once), anything else one at a time
        int i = verbeter.get(rng.nextInt(verbeter.size()));
        KaartGebouw g = plan.get(i);
        int naar = g.level() + 1;
        if (g.type() == KaartGebouwType.MUUR) {
            int n = 0;
            for (int j = 0; j < plan.size(); j++) {
                KaartGebouw m = plan.get(j);
                if (m.type() == KaartGebouwType.MUUR && m.level() == g.level()) {
                    plan.set(j, m.metLevel(naar));
                    n++;
                }
            }
            return new Groei(Groei.Soort.VERBETER, KaartGebouwType.MUUR, naar, n);
        }
        plan.set(i, g.metLevel(naar));
        return new Groei(Groei.Soort.VERBETER, g.type(), naar, 1);
    }

    /** A stretch of up to 6 walls: next to an existing wall when there is one (the ring grows on), else on a fresh ring. */
    private static int muurStukje(List<KaartGebouw> plan, int th, Random rng) {
        int ruimte = KaartGebouwType.MUUR.maxAantal(th) - aantal(plan, KaartGebouwType.MUUR);
        int doel = Math.min(ruimte, 2 + rng.nextInt(5));
        int gezet = 0;
        for (int[] p : ring(9 + th)) {
            if (gezet >= doel) {
                break;
            }
            if (!(Math.abs(p[0] - 32) <= 1 && p[1] == 32 + 9 + th) && vrij(plan, p[0], p[1], 1)) {
                plan.add(new KaartGebouw(KaartGebouwType.MUUR, 1, p[0], p[1], 0));
                gezet++;
            }
        }
        for (int poging = 0; gezet < doel && poging < 200; poging++) {   // the ring is full or blocked: loose bits of wall
            int x = RAND + rng.nextInt(KaartGebouwType.PLOT - 2 * RAND), z = RAND + rng.nextInt(KaartGebouwType.PLOT - 2 * RAND);
            if (vrij(plan, x, z, 1)) {
                plan.add(new KaartGebouw(KaartGebouwType.MUUR, 1, x, z, 0));
                gezet++;
            }
        }
        return gezet;
    }

    // =====================================================================================================================
    // what follows from a plan
    // =====================================================================================================================

    public static int stadhuisLevel(List<KaartGebouw> plan) {
        for (KaartGebouw g : plan) {
            if (g.type() == KaartGebouwType.STADHUIS) {
                return Math.max(1, g.level());
            }
        }
        return 1;
    }

    /** Knusheid = the points on the map: what every building gives (the same sum as the core's Base.knusheid()). */
    public static int knusheid(List<KaartGebouw> plan) {
        int k = 0;
        for (KaartGebouw g : plan) {
            k += g.type().knus(g.level());
        }
        return k;
    }

    /** 0..1: how much of what the stadhuis level allows has been built (counts and levels). */
    public static double volheid(List<KaartGebouw> plan) {
        int th = stadhuisLevel(plan);
        double heeft = 0, kan = 0;
        for (KaartGebouwType t : KaartGebouwType.values()) {
            if (t == KaartGebouwType.STADHUIS) {
                continue;
            }
            int max = t.maxAantal(th), ml = Math.max(1, t.maxLevel(th));
            kan += max * ml;
            for (KaartGebouw g : plan) {
                if (g.type() == t) {
                    heeft += Math.min(g.level(), ml);
                }
            }
        }
        return kan == 0 ? 1 : Math.min(1, heeft / kan);
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

    public static boolean vrij(List<KaartGebouw> plan, int x, int z, int maat) {
        if (x < RAND || z < RAND || x + maat > KaartGebouwType.PLOT - RAND || z + maat > KaartGebouwType.PLOT - RAND) {
            return false;
        }
        for (KaartGebouw g : plan) {
            if (g.overlapt(x, z, maat)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A free spot for a footprint: inside the wall ring ({@code binnen} &gt; 0: within that radius of the middle, with a tile of
     * space around it so a village stays walkable) or anywhere on the plot (0). Null: no room found.
     */
    @Nullable
    private static int[] vrijePlek(List<KaartGebouw> plan, int maat, Random rng, int binnen) {
        for (int poging = 0; poging < 300; poging++) {
            int x, z;
            if (binnen > 0 && poging < 200) {
                x = 32 - binnen + 1 + rng.nextInt(Math.max(1, 2 * binnen - maat - 1));
                z = 32 - binnen + 1 + rng.nextInt(Math.max(1, 2 * binnen - maat - 1));
                if (!vrij(plan, x - 1, z - 1, maat + 2)) {
                    continue;
                }
            } else {
                x = RAND + rng.nextInt(KaartGebouwType.PLOT - 2 * RAND - maat + 1);
                z = RAND + rng.nextInt(KaartGebouwType.PLOT - 2 * RAND - maat + 1);
            }
            if (vrij(plan, x, z, maat)) {
                return new int[] {x, z};
            }
        }
        return null;
    }

    /** Everything wrong with a plan (empty = a valid base): one stadhuis, inside the plot, no overlaps, limits per stadhuis level. */
    public static List<String> fouten(List<KaartGebouw> plan) {
        List<String> f = new ArrayList<>();
        if (aantal(plan, KaartGebouwType.STADHUIS) != 1) {
            f.add("stadhuizen: " + aantal(plan, KaartGebouwType.STADHUIS));
        }
        int th = stadhuisLevel(plan);
        if (th > KaartGebouwType.MAX_STADHUIS) {
            f.add("stadhuis level " + th);
        }
        for (int i = 0; i < plan.size(); i++) {
            KaartGebouw g = plan.get(i);
            if (g.x() < 0 || g.z() < 0 || g.x() + g.type().maat > KaartGebouwType.PLOT || g.z() + g.type().maat > KaartGebouwType.PLOT) {
                f.add("buiten: " + g);
            }
            if (g.level() < 1 || g.level() > g.type().maxLevel(th)) {
                f.add("level: " + g + " (stadhuis " + th + ")");
            }
            for (int j = 0; j < i; j++) {
                if (plan.get(j).overlapt(g.x(), g.z(), g.type().maat)) {
                    f.add("overlap: " + plan.get(j) + " / " + g);
                }
            }
        }
        for (KaartGebouwType t : KaartGebouwType.values()) {
            if (aantal(plan, t) > t.maxAantal(th)) {
                f.add("te veel " + t.id + ": " + aantal(plan, t) + " > " + t.maxAantal(th));
            }
        }
        return f;
    }
}
