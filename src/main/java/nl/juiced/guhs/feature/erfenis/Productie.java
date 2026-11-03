package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * De Erfenis: what the buildings make and the numbers around it (balans.md 2.4, 3.2-3.4, A2/A8/A9; the 10-level mapping of
 * {@link Prijzen#tien}), plus the real-time production itself ({@link #produceer}), which also runs for the hours you were
 * away: {@link Base#bijwerken} walks from event to event (a build finishing, a resident moving in) and lets every building
 * work the stretch in between at a constant rate.
 * <ul>
 *   <li>rate = table(level) x staff factor x (1 + bonus); staff factor: every empty workplace -25%, never under 25%;
 *   bonus: Dorpscafé + Muziektent (+5/10/15%) + logé guhs from the Guhpension (+5% each, max 2 per building).</li>
 *   <li>a building stops when its basket (buffer) is full: 6 hours of production up to L6, 8 hours from L7.</li>
 *   <li>workshops take their inputs from the storage while they work (no inputs: they wait).</li>
 *   <li>vadskracht: the top workshops need VK at the Vadsmast; without enough they work at L6 speed (Mammoetwei and
 *   Kaarsenmakerij sleep).</li>
 * </ul>
 */
public final class Productie {
    private Productie() {
    }

    private static double[] t(double... v) {
        return v;
    }

    private static final double[] HOUTZAGERIJ = t(80, 120, 160, 190, 210), STEENBAKKERIJ = t(30, 45, 68, 100, 135),
            GLASBLAZERIJ = t(16, 24, 36, 50, 64), BOUWBLOKKEN = t(12, 20, 32, 48, 64), MOLEN = t(20, 30, 45, 65, 90),
            BAKKERIJ = t(10, 15, 22, 32, 44), WEVERIJ = t(10, 15, 22, 32, 44), KAAS = t(6, 9, 14, 20, 28), KAARSEN = t(12, 18, 27, 40, 55);

    private static final Map<GebouwType, List<Recept>> RECEPTEN = new EnumMap<>(GebouwType.class);

    static {
        r(GebouwType.KIPPENHOK, Recept.bron("ei", Goed.EI, t(20, 30, 45, 65, 90)).met(Goed.VEER, 0.5));
        r(GebouwType.SCHAAPSKOOI, Recept.bron("wol", Goed.WOL, t(12, 18, 27, 40, 55)));
        r(GebouwType.KONIJNENREN, Recept.bron("angora", Goed.ANGORA, t(10, 15, 22, 32, 44)).met(Goed.MEST, 1.2));
        r(GebouwType.EENDENVIJVER, Recept.bron("dons", Goed.DONS, t(8, 12, 18, 26, 36)).met(Goed.EENDENEI, 1.2));
        r(GebouwType.KOEIENSTAL, Recept.bron("melk", Goed.MELK, t(12, 18, 27, 40, 55)));
        r(GebouwType.BIJENHOF, Recept.bron("honing", Goed.HONING, t(8, 12, 18, 26, 36)).met(Goed.WAS, 1.0));
        r(GebouwType.MAMMOETWEI, Recept.bron("reuzenwol", Goed.REUZENWOL, t(6, 9, 12)));
        r(GebouwType.AKKER, Recept.bron("graan", Goed.GRAAN, t(40, 60, 90, 130, 180)),
                Recept.bron("rozegraan", Goed.ROZEGRAAN, t(24, 36, 54, 78, 108)),
                Recept.bron("wortel", Goed.WORTEL, t(24, 36, 54, 78, 108)),
                Recept.bron("bloem", Goed.BLOEM, t(24, 36, 54, 78, 108)),
                Recept.bron("pluisbolletje", Goed.PLUISBOLLETJE, t(16, 24, 36, 52, 72)));
        r(GebouwType.BOOMGAARD, Recept.bron("knabbelbes", Goed.KNABBELBES, t(12, 18, 27, 40, 55)).met(Goed.FRUIT, 1.0));
        r(GebouwType.BOSJE, Recept.bron("stam", Goed.STAM, t(20, 30, 40, 48, 53)));
        r(GebouwType.GROEVE, Recept.bron("steen", Goed.STEEN, t(30, 45, 68, 100, 135)).met(Goed.KLEI, 1.0 / 3));
        r(GebouwType.ZANDKUIL, Recept.bron("zand", Goed.ZAND, t(30, 45, 68, 100, 135)));
        r(GebouwType.HOUTZAGERIJ, Recept.werk("plank", Goed.PLANK, 4, HOUTZAGERIJ, 1, Goed.STAM, 1),
                Recept.werk("balk", Goed.BALK, 1, HOUTZAGERIJ, 0.25, Goed.STAM, 1));
        r(GebouwType.STEENBAKKERIJ, Recept.werk("baksteen", Goed.BAKSTEEN, 1, STEENBAKKERIJ, 1, Goed.KLEI, 1),
                Recept.werk("steenbaksteen", Goed.BAKSTEEN, 1, STEENBAKKERIJ, 1, Goed.STEEN, 1),
                Recept.werk("gladsteen", Goed.GLADSTEEN, 1, STEENBAKKERIJ, 1, Goed.STEEN, 1));
        r(GebouwType.GLASBLAZERIJ, Recept.werk("glas", Goed.GLAS, 1, GLASBLAZERIJ, 1, Goed.ZAND, 1),
                Recept.werk("glasscherf", Goed.GLASSCHERF, 1, GLASBLAZERIJ, 1, Goed.ZAND, 1));
        r(GebouwType.BOUWBLOKKENWERKPLAATS, Recept.werk("bouwblok", Goed.BOUWBLOK, 1, BOUWBLOKKEN, 1, Goed.PLANK, 1));
        r(GebouwType.MOLEN, Recept.werk("meel", Goed.MEEL, 1, MOLEN, 1, Goed.GRAAN, 2),
                Recept.werk("rozemeel", Goed.ROZEMEEL, 1, MOLEN, 1, Goed.ROZEGRAAN, 2));
        r(GebouwType.BAKKERIJ, Recept.werk("brood", Goed.BROOD, 1, BAKKERIJ, 1, Goed.MEEL, 2, Goed.EI, 1),
                Recept.werk("koek", Goed.KOEK, 1, BAKKERIJ, 1, Goed.MEEL, 1, Goed.EI, 1, Goed.HONING, 1),
                Recept.werk("wortelcake", Goed.WORTELCAKE, 1, BAKKERIJ, 1, Goed.MEEL, 2, Goed.WORTEL, 2, Goed.EI, 1),
                Recept.werk("knabbelkoek", Goed.KNABBELKOEK, 1, BAKKERIJ, 1, Goed.ROZEMEEL, 2, Goed.EI, 1));
        r(GebouwType.WEVERIJ, Recept.werk("stof", Goed.STOF, 1, WEVERIJ, 1, Goed.WOL, 2),
                Recept.werk("knuffelstof", Goed.KNUFFELSTOF, 1, WEVERIJ, 1, Goed.ANGORA, 2),
                Recept.werk("pluiswol", Goed.WOL, 1, WEVERIJ, 1, Goed.PLUIS, 2),
                Recept.werk("wol_uit_reuzenwol", Goed.WOL, 16, WEVERIJ, 16, Goed.REUZENWOL, 1));
        r(GebouwType.KAASMAKERIJ, Recept.werk("kaas", Goed.KAAS, 1, KAAS, 1, Goed.MELK, 3),
                Recept.werk("kaasknabbel", Goed.KAASKNABBEL, 2, KAAS, 2, Goed.KAAS, 1, Goed.MELK, 1).vanaf(3));
        r(GebouwType.KAARSENMAKERIJ, Recept.werk("kaars", Goed.KAARS, 1, KAARSEN, 1, Goed.WAS, 2),
                Recept.werk("lantaarn", Goed.LANTAARN, 1, KAARSEN, 0.5, Goed.WAS, 4, Goed.GLAS, 1));
    }

    private static void r(GebouwType t, Recept... r) {
        RECEPTEN.put(t, List.of(r));
    }

    /** What this type can make (empty: it makes nothing). Index = the building's {@code keuze}. */
    public static List<Recept> recepten(GebouwType t) {
        return RECEPTEN.getOrDefault(t, List.of());
    }

    /** The recipe a building uses now (its choice, or the first one when its choice isn't allowed at its level). */
    public static Recept recept(Gebouw g) {
        List<Recept> l = recepten(g.type);
        if (l.isEmpty()) {
            return null;
        }
        Recept r = g.keuze >= 0 && g.keuze < l.size() ? l.get(g.keuze) : l.get(0);
        return r.minLevel() > Math.max(1, g.level) ? l.get(0) : r;
    }

    /** Output units per hour of this recipe at this level (full staff, no bonus). */
    public static double perUur(GebouwType t, Recept r, int level) {
        if (level <= 0) {
            return 0;
        }
        if (t == GebouwType.MAMMOETWEI) {
            return r.tabel()[Math.min(r.tabel().length, level) - 1];
        }
        return Prijzen.tien(r.tabel(), level, 0.7);
    }

    /** The basket: how many hours of production a building holds before it stops (balans A2: 6 h, from L7 8 h). */
    public static int bufferUren(int level) {
        return level <= 6 ? 6 : 8;
    }

    /** Workplaces (balans A8): animals and fields 1 (2 from L7); workshops and sources 1 / 2 (L5) / 3 (L9). */
    public static int plekken(GebouwType t, int level) {
        if (!t.maakt() || level <= 0) {
            return 0;
        }
        if (t == GebouwType.MAMMOETWEI) {
            return level <= 1 ? 1 : 2;
        }
        return switch (t.familie) {
            case DIEREN, AKKERS -> level <= 6 ? 1 : 2;
            default -> level <= 4 ? 1 : level <= 8 ? 2 : 3;
        };
    }

    /** Every empty workplace -25%, never under 25% (balans A8). */
    public static double werkFactor(int plekken, int bemand) {
        return Math.max(0.25, 1 - 0.25 * Math.max(0, plekken - bemand));
    }

    /** The vadskracht this building needs at this level for full speed (0 = none). */
    public static int vkNodig(GebouwType t, int level) {
        return switch (t) {
            case HOUTZAGERIJ, STEENBAKKERIJ -> level >= 9 ? 5 : 0;
            case GLASBLAZERIJ -> level >= 9 ? 12 : level >= 7 ? 8 : 0;
            case BOUWBLOKKENWERKPLAATS -> level >= 9 ? 25 : level >= 7 ? 15 : 0;
            case KAARSENMAKERIJ -> level >= 1 ? 8 : 0;
            case MAMMOETWEI -> level <= 0 ? 0 : 10 + 5 * Math.min(3, level);
            default -> 0;
        };
    }

    /** Without enough vadskracht these stand still (the mammoths sleep, the candles are out); the others work at L6 speed. */
    public static boolean slaaptZonderVk(GebouwType t) {
        return t == GebouwType.MAMMOETWEI || t == GebouwType.KAARSENMAKERIJ;
    }

    /** The level a workshop works at without its vadskracht. */
    public static final int ZONDER_VK_LEVEL = 6;

    // =====================================================================================================================
    // the effects of the non-production buildings
    // =====================================================================================================================

    private static final int[] HUTJE = {2, 3, 3, 4, 4, 5, 6, 7, 8, 9};
    private static final int[] SPEELTUIN = {1, 1, 2, 2, 3, 4, 6, 7, 10, 12};
    private static final int[] PENSION = {2, 2, 3, 3, 3, 4, 5, 6, 7, 8};
    private static final int[] OPSLAG = {210, 300, 450, 600, 900, 1200, 2100, 3000, 4500, 6000};
    private static final int[] GUHLDEN = {500, 1000, 2500, 5000, 10000, 18000, 30000, 45000, 65000, 100000};
    private static final int[] KARREN = {3, 3, 4, 4, 5, 5, 6, 6, 7, 7};

    private static int uit(int[] tabel, int level) {
        return level <= 0 ? 0 : tabel[Math.min(tabel.length, level) - 1];
    }

    /** Residents a Guhhutje houses (hutje -> boerenhuisje -> rijtjeshuis -> villa). */
    public static int hutjeBewoners(int level) {
        return uit(HUTJE, level);
    }

    /** Extra residents a Speeltuin draws (and it halves the move-in time). */
    public static int speeltuinBewoners(int level) {
        return uit(SPEELTUIN, level);
    }

    /** Logé guhs a Guhpension houses. */
    public static int pensionPlekken(int level) {
        return uit(PENSION, level);
    }

    /** Goods room of one Opslagschuur. */
    public static int opslagschuur(int level) {
        return uit(OPSLAG, level);
    }

    /** Goods room of the Hoeve itself (opa's old shed: so the very start has somewhere to put things). */
    public static final int HOEVE_OPSLAG = 150;

    /** The Guhlden the Hoeve holds at this level. */
    public static int guhldenCap(int hoeve) {
        return uit(GUHLDEN, Math.max(1, hoeve));
    }

    /** Order carts of the Marktplein (the kaart slice uses this for the number of open orders). */
    public static int marktKarren(int level) {
        return uit(KARREN, level);
    }

    /** Dorpscafé / Muziektent: +5% (L1-4), +10% (L5-8), +15% (L9-10). */
    public static int bonusProcent(int level) {
        return level <= 0 ? 0 : level <= 4 ? 5 : level <= 8 ? 10 : 15;
    }

    /** The most VK the Vadsmast takes in: 40 at L1 up to 140 at L10. */
    public static int vadsmastCap(int level) {
        return level <= 0 ? 0 : 40 + Math.round((level - 1) * 100f / 9f);
    }

    /** Builders: 1, a 2nd from Timmerschuur L1, a 3rd from Timmerschuur L7. */
    public static int bouwers(int timmerschuurLevel) {
        return 1 + (timmerschuurLevel >= 1 ? 1 : 0) + (timmerschuurLevel >= 7 ? 1 : 0);
    }

    /** Minutes between two new residents (15, halved with a Speeltuin). */
    public static long intrekMs(boolean speeltuin) {
        return speeltuin ? 450_000L : 900_000L;
    }

    // =====================================================================================================================
    // the production itself
    // =====================================================================================================================

    /** Who works where: the residents spread round-robin over the workplaces (every building one before any gets two). */
    public static Map<Integer, Integer> bemanning(Base b) {
        Map<Integer, Integer> uit = new HashMap<>();
        List<Gebouw> werk = new ArrayList<>();
        for (Gebouw g : b.gebouwen) {
            if (g.level > 0 && plekken(g.type, g.level) > 0) {
                werk.add(g);
            }
        }
        int over = b.bewoners;
        boolean iets = true;
        while (over > 0 && iets) {
            iets = false;
            for (Gebouw g : werk) {
                int nu = uit.getOrDefault(g.id, 0);
                if (over > 0 && nu < plekken(g.type, g.level)) {
                    uit.put(g.id, nu + 1);
                    over--;
                    iets = true;
                }
            }
        }
        return uit;
    }

    /** The vadskracht each building gets (in id order: first come, first served). */
    public static Map<Integer, Boolean> vadskracht(Base b) {
        Map<Integer, Boolean> uit = new HashMap<>();
        int over = b.vkInDorp();
        for (Gebouw g : b.gebouwen) {
            int nodig = vkNodig(g.type, g.level);
            if (nodig > 0) {
                boolean genoeg = over >= nodig;
                if (genoeg) {
                    over -= nodig;
                }
                uit.put(g.id, genoeg);
            }
        }
        return uit;
    }

    /** The bonus (fraction) on this building: café + muziektent + its logé guhs (max 2). */
    public static double bonus(Base b, Gebouw g) {
        return b.dorpsBonus() / 100.0 + 0.05 * Math.min(2, b.logesBij(g.id));
    }

    /** The level a building really works at now (its own, or L6 without vadskracht; 0 = asleep). */
    public static int werkLevel(Gebouw g, Map<Integer, Boolean> vk) {
        if (g.level <= 0) {
            return 0;
        }
        Boolean genoeg = vk.get(g.id);
        if (genoeg == null || genoeg) {
            return g.level;
        }
        return slaaptZonderVk(g.type) ? 0 : Math.min(g.level, ZONDER_VK_LEVEL);
    }

    /** Output units per hour a building makes right now (with staff, bonus and vadskracht); 0 when full or idle. */
    public static double nuPerUur(Base b, Gebouw g) {
        Recept r = recept(g);
        if (r == null) {
            return 0;
        }
        Map<Integer, Boolean> vk = vadskracht(b);
        int wl = werkLevel(g, vk);
        if (wl <= 0 || r.minLevel() > wl) {
            return 0;
        }
        return perUur(g.type, r, wl) * werkFactor(plekken(g.type, g.level), bemanning(b).getOrDefault(g.id, 0)) * (1 + bonus(b, g));
    }

    /** The basket size of a building (output units of its recipe). */
    public static double bufferCap(Gebouw g) {
        Recept r = recept(g);
        return r == null || g.level <= 0 ? 0 : bufferUren(g.level) * perUur(g.type, r, g.level);
    }

    /** Every building works the stretch [t0, t1] (millis) at today's rates. */
    static void produceer(Base b, long t0, long t1) {
        double uren = (t1 - t0) / 3_600_000.0;
        if (uren <= 0) {
            return;
        }
        Map<Integer, Integer> bemand = bemanning(b);
        Map<Integer, Boolean> vk = vadskracht(b);
        int hoeve = b.hoeveLevel();
        for (Gebouw g : b.gebouwen) {
            Recept r = recept(g);
            if (r == null || g.level <= 0) {
                continue;
            }
            int wl = werkLevel(g, vk);
            if (r.minLevel() > wl) {
                r = recepten(g.type).get(0);
            }
            if (wl <= 0) {
                continue;
            }
            double basis = perUur(g.type, r, wl);
            double rate = basis * werkFactor(plekken(g.type, g.level), bemand.getOrDefault(g.id, 0)) * (1 + bonus(b, g));
            double ruimte = bufferUren(g.level) * perUur(g.type, r, g.level) - g.inBuffer(r.uit());
            if (rate <= 0 || ruimte <= 1e-9) {
                continue;
            }
            double opdrachten = Math.min(rate * uren, ruimte) / r.uitN();
            for (Map.Entry<Goed, Integer> e : r.in().entrySet()) {
                opdrachten = Math.min(opdrachten, b.goed(e.getKey()) / e.getValue());
            }
            if (opdrachten <= 1e-9) {
                continue;
            }
            for (Map.Entry<Goed, Integer> e : r.in().entrySet()) {
                b.verander(e.getKey(), -opdrachten * e.getValue());
            }
            double eenheden = opdrachten * r.uitN();
            g.bijBuffer(r.uit(), eenheden);
            for (Map.Entry<Goed, Double> e : r.bij().entrySet()) {
                g.bijBuffer(e.getKey(), eenheden * e.getValue());
            }
            if (g.type == GebouwType.SCHAAPSKOOI && g.level >= 7 && hoeve >= 7 && basis > 0) {
                g.bijBuffer(Goed.PLUIS, eenheden * 10.0 / basis);   // (balans: wolkenschaapjes from H7, +10 pluis an hour)
            }
        }
    }
}
