package nl.juiced.guhs.feature.erfenis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * De Erfenis: what everything costs and how long it takes (balans.md 2.1, 3.1, 3.4, 6, with the 10-level override of
 * CONTRACT_OPKNAPPER.md: balans level k = our level 2k, an odd level 2k+1 halfway between balans k and k+1, our L1 about
 * half of balans L1). Buildings with their own table in balans 3.4 (3 levels) are spread over 10 levels with their steps at
 * L1 / L5 / L9 (the same levels where their effect steps up).
 */
public final class Prijzen {
    /** A price: Guhlden, village goods, main-world items (item id -> count, handed in at the Loket), building time. */
    public record Prijs(int guhlden, Map<Goed, Integer> goederen, Map<String, Integer> hoofdwereld, long tijdMs) {
        public static final Prijs GRATIS = new Prijs(0, Map.of(), Map.of(), 0);

        public boolean leeg() {
            return guhlden == 0 && goederen.isEmpty() && hoofdwereld.isEmpty();
        }
    }

    /** A goal that must be reached before the Hoeve may go up to a level (balans 2.1). Lang: {@code erfenis.doel.<soort>}. */
    public record Doel(String soort, int n) {
    }

    private static final long MIN = 60_000L;

    private Prijzen() {
    }

    // =====================================================================================================================
    // the 5 -> 10 level mapping
    // =====================================================================================================================

    /** Balans table (5 values, level 1..5) onto our level 1..10: L1 = factor1 x b1, even 2k = bk, odd 2k+1 = (bk + bk+1) / 2. */
    static double tien(double[] b, int level, double factor1) {
        if (level <= 1) {
            return b[0] * factor1;
        }
        if (level % 2 == 0) {
            return b[Math.min(b.length, level / 2) - 1];
        }
        int k = (level - 1) / 2;
        return (b[Math.min(b.length, k) - 1] + b[Math.min(b.length, k + 1) - 1]) / 2.0;
    }

    /** A 3-step table (balans 3.4) over 10 levels: L1 = a, L5 = b, L9 = c, linear in between, L10 = 1.3 c. */
    static double anker(double a, double b, double c, int level) {
        if (level <= 1) {
            return a;
        }
        if (level <= 5) {
            return a + (b - a) * (level - 1) / 4.0;
        }
        if (level <= 9) {
            return b + (c - b) * (level - 5) / 4.0;
        }
        return c * 1.3;
    }

    private static int rond(double v) {
        if (v >= 1000) {
            return (int) (Math.round(v / 50.0) * 50);
        }
        if (v >= 100) {
            return (int) (Math.round(v / 10.0) * 10);
        }
        return (int) Math.max(1, Math.round(v));
    }

    // =====================================================================================================================
    // buildings
    // =====================================================================================================================

    private static final double[] KLASSE_A = {30, 150, 600, 1800, 4000};
    private static final double[] KLASSE_B = {200, 400, 1000, 2400, 5000};
    private static final double[] KLASSE_C = {1000, 1400, 2000, 3400, 6000};
    private static final double[] MARKT = {100, 400, 1500, 3500, 7000};
    private static final double[] KAARSEN = {4000, 5000, 6000, 8000, 10000};
    private static final int[] TIMMER = {500, 800, 1200, 1800, 2600, 4000, 12000, 15000, 18000, 22000};
    private static final int[] MAMMOET = {20000, 30000, 45000};
    /** Build minutes per balans level for small / middle / large footprints. */
    private static final double[][] TIJD = {{2, 15, 40, 75, 120}, {5, 20, 60, 90, 150}, {10, 30, 75, 120, 180}};

    /** The goods a balans level asks of a middle-sized building (balans 3.1). */
    @SuppressWarnings("unchecked")
    private static final Map<Goed, Integer>[] GOED = new Map[] {
            Map.of(Goed.PLANK, 20),
            Map.of(Goed.PLANK, 40, Goed.WOL, 10),
            Map.of(Goed.PLANK, 80, Goed.BAKSTEEN, 40, Goed.STOF, 10),
            Map.of(Goed.PLANK, 150, Goed.BAKSTEEN, 120, Goed.GLAS, 60, Goed.KAAS, 10),
            Map.of(Goed.PLANK, 250, Goed.BAKSTEEN, 250, Goed.GLAS, 120, Goed.KAARS, 20, Goed.BOUWBLOK, 40)};

    /** Small (&lt;= 6x6) 0, middle 1, large (&gt;= 10x10) 2. */
    static int maatKlasse(GebouwType t) {
        return t.oppervlak() <= 36 ? 0 : t.oppervlak() >= 100 ? 2 : 1;
    }

    /** What it costs to bring a building of this type to level {@code naar} (1 = build a new one). */
    public static Prijs gebouw(GebouwType t, int naar) {
        if (t == GebouwType.HOEVE) {
            return hoeve(naar);
        }
        int L = Math.max(1, Math.min(t.topLevel(), naar));
        int f = guhlden(t, L);
        Map<Goed, Integer> g = goederen(t, L);
        Map<String, Integer> hw = hoofdwereld(t, L);
        long tijd = tijdMin(t, L) * MIN;
        return new Prijs(f, g, hw, tijd);
    }

    static int guhlden(GebouwType t, int L) {
        return switch (t) {
            case TIMMERSCHUUR -> TIMMER[L - 1];
            case MARKTPLEIN -> rond(tien(MARKT, L, 0.5));
            case DETECTIVEBUREAU -> rond(anker(400, 2000, 6000, L));
            case DORPSCAFE -> rond(anker(2000, 6000, 14000, L));
            case MUZIEKTENT -> rond(anker(2500, 7000, 16000, L));
            case VADSMAST -> rond(anker(5000, 12000, 25000, L));
            case LOKET -> 3000 + 1500 * (L - 1);
            case KAARSENMAKERIJ -> rond(tien(KAARSEN, L, 1.0));
            case MAMMOETWEI -> MAMMOET[Math.min(3, L) - 1];
            default -> rond(tien(switch (t.klasse) {
                case 'B' -> KLASSE_B;
                case 'C' -> KLASSE_C;
                default -> KLASSE_A;
            }, L, 0.5));
        };
    }

    static Map<Goed, Integer> goederen(GebouwType t, int L) {
        Map<Goed, Double> som = new LinkedHashMap<>();
        if (t == GebouwType.MAMMOETWEI) {
            Map<Goed, Integer> basis = L <= 1 ? GOED[3] : GOED[4];
            basis.forEach((k, v) -> som.put(k, v * (L >= 3 ? 2.0 : 1.5)));
        } else if (L <= 1) {
            GOED[0].forEach((k, v) -> som.put(k, v * 0.5));
        } else if (L % 2 == 0) {
            GOED[L / 2 - 1].forEach((k, v) -> som.put(k, v.doubleValue()));
        } else {
            int k = (L - 1) / 2;
            GOED[k - 1].forEach((g, v) -> som.merge(g, v / 2.0, Double::sum));
            GOED[k].forEach((g, v) -> som.merge(g, v / 2.0, Double::sum));
        }
        double mult = t == GebouwType.MAMMOETWEI ? 1 : switch (maatKlasse(t)) {
            case 0 -> 0.5;
            case 2 -> 1.5;
            default -> 1.0;
        };
        Map<Goed, Integer> uit = new LinkedHashMap<>();
        for (Goed g : Goed.values()) {   // (a fixed order: the panel lists them the same every time)
            Double v = som.get(g);
            if (v != null && v * mult >= 0.5) {
                uit.put(g, rond(v * mult));
            }
        }
        return uit;
    }

    static Map<String, Integer> hoofdwereld(GebouwType t, int L) {
        Map<String, Integer> m = new LinkedHashMap<>();
        switch (t) {
            case HOUTZAGERIJ, STEENBAKKERIJ, GLASBLAZERIJ -> {
                if (L >= 9) {
                    m.put("guhs:vahoege_vads_ingot", L >= 10 ? 8 : 4);
                    m.put("guhs:guh_kristal", L >= 10 ? 16 : 8);
                }
            }
            case BOUWBLOKKENWERKPLAATS -> {
                if (L >= 10) {
                    m.put("guhs:bouwkeet", 1);
                }
                if (L >= 9) {
                    m.put("guhs:guh_kristal", L >= 10 ? 16 : 8);
                    m.put("guhs:pastelbaksteen_roze", L >= 10 ? 32 : 16);
                }
            }
            case TIMMERSCHUUR -> {
                if (L == 7) {
                    m.put("guhs:vahoege_vads_ingot", 8);
                }
            }
            case KAARSENMAKERIJ -> {
                if (L == 1) {
                    m.put("guhs:vahoege_vads_ingot", 8);
                    m.put("guhs:kaashoning", 8);
                }
            }
            case VADSMAST -> {
                if (L == 1) {
                    m.put("guhs:vadsontvanger", 1);
                } else if (L == 5) {
                    m.put("guhs:knabbelbatterij", 1);
                } else if (L == 9) {
                    m.put("guhs:guh_kristal_blok", 4);
                }
            }
            case MAMMOETWEI -> m.put("guhs:fossiel_mammoet", 1);
            default -> {
            }
        }
        return m;
    }

    static long tijdMin(GebouwType t, int L) {
        return switch (t) {
            case TIMMERSCHUUR -> L == 1 ? 20 : L == 7 ? 120 : standaardTijd(t, L);
            case DETECTIVEBUREAU -> Math.round(anker(10, 45, 90, L));
            case DORPSCAFE, MUZIEKTENT -> Math.round(Math.min(240, anker(30, 90, 180, L)));
            case LOKET -> L == 1 ? 20 : standaardTijd(t, L);
            case VADSMAST -> Math.round(Math.min(240, anker(60, 120, 180, L)));
            case MAMMOETWEI -> new long[] {120, 180, 240}[Math.min(3, L) - 1];
            default -> standaardTijd(t, L);
        };
    }

    private static long standaardTijd(GebouwType t, int L) {
        return Math.max(1, Math.round(tien(TIJD[maatKlasse(t)], L, 0.5)));
    }

    // =====================================================================================================================
    // the Hoeve (balans 2.1)
    // =====================================================================================================================

    private static Map<Goed, Integer> g(Object... kv) {
        Map<Goed, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((Goed) kv[i], (Integer) kv[i + 1]);
        }
        return m;
    }

    private static Map<String, Integer> hw(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return m;
    }

    private static final String[] KLEUREN = {"white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow", "lime", "green", "cyan",
            "light_blue", "blue", "purple", "magenta", "pink"};

    private static Map<String, Integer> wol(String... kleuren) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String k : kleuren) {
            m.put("minecraft:" + k + "_wool", 16);
        }
        return m;
    }

    /** The Hoeve's price to reach level {@code naar} (2..10). */
    public static Prijs hoeve(int naar) {
        return switch (naar) {
            case 2 -> new Prijs(50, g(Goed.PLANK, 10, Goed.EI, 5), Map.of(), MIN);
            case 3 -> new Prijs(600, g(Goed.PLANK, 60, Goed.EI, 20, Goed.WOL, 20), Map.of(), 15 * MIN);
            case 4 -> new Prijs(1800, g(Goed.PLANK, 150, Goed.BAKSTEEN, 60, Goed.MEEL, 30, Goed.STOF, 20), Map.of(), 40 * MIN);
            case 5 -> new Prijs(4500, g(Goed.PLANK, 300, Goed.BAKSTEEN, 200, Goed.BROOD, 40, Goed.STOF, 40, Goed.MELK, 30), Map.of(), 75 * MIN);
            case 6 -> new Prijs(9000, g(Goed.PLANK, 500, Goed.BAKSTEEN, 400, Goed.GLAS, 150, Goed.KAAS, 40, Goed.HONING, 40), Map.of(), 105 * MIN);
            case 7 -> {
                Map<String, Integer> m = hw("guhs:guh_kristal", 16, "guhs:vahoege_vads_ingot", 8, "guhs:kaashoning", 4);
                m.putAll(wol("pink", "light_blue", "yellow", "lime"));
                m.put("guhs:pastelbaksteen_roze", 32);
                yield new Prijs(16000, g(Goed.PLANK, 800, Goed.BAKSTEEN, 700, Goed.GLAS, 300, Goed.KAAS, 60, Goed.KOEK, 60), m, 135 * MIN);
            }
            case 8 -> {
                Map<String, Integer> m = hw("guhs:guh_kristal", 32, "guhs:vahoege_vads_ingot", 16, "guhs:zoutkristal", 16, "guhs:kaasbrok", 8);
                m.putAll(wol("pink", "light_blue", "yellow", "lime", "purple", "orange", "cyan", "red"));
                m.put("guhs:knabbelbatterij", 1);
                yield new Prijs(26000, g(Goed.PLANK, 1200, Goed.BAKSTEEN, 1000, Goed.GLAS, 450, Goed.KAARS, 60, Goed.BOUWBLOK, 100), m, 165 * MIN);
            }
            case 9 -> {
                Map<String, Integer> m = hw("guhs:guh_kristal", 64, "guhs:block_of_vahoege_vads", 2, "guhs:goudkaas", 8);
                for (String k : new String[] {"pink", "light_blue", "yellow", "lime"}) {
                    m.put("minecraft:" + k + "_stained_glass", 16);
                }
                m.put("guhs:wolkenpluis", 32);
                m.put("guhs:gloeister", 1);
                yield new Prijs(40000, g(Goed.PLANK, 1800, Goed.BAKSTEEN, 1500, Goed.GLAS, 700, Goed.KAARS, 100, Goed.BOUWBLOK, 200,
                        Goed.KNUFFELSTOF, 100), m, 195 * MIN);
            }
            case 10 -> {
                Map<String, Integer> m = hw("guhs:guh_kristal_blok", 4, "guhs:block_of_vahoege_vads", 4, "guhs:goudkaas", 16);
                m.putAll(wol(KLEUREN));
                m.put("guhs:gloeister", 2);
                m.put("guhs:fossielmijn_fossielbeeldje", 1);
                yield new Prijs(60000, g(Goed.PLANK, 2500, Goed.BAKSTEEN, 2000, Goed.GLAS, 1000, Goed.KAARS, 150, Goed.BOUWBLOK, 400,
                        Goed.REUZENWOL, 30), m, 240 * MIN);
            }
            default -> Prijs.GRATIS;
        };
    }

    /** What must be reached before the Hoeve may go up to {@code naar} (balans 2.1). */
    public static List<Doel> doelen(int naar) {
        return switch (naar) {
            case 2 -> List.of(new Doel("bewoners", 1), new Doel("bestellingen", 1), new Doel("plukjes", 1));
            case 3 -> List.of(new Doel("bewoners", 4), new Doel("bestellingen", 5), new Doel("plukjes", 4));
            case 4 -> List.of(new Doel("bewoners", 8), new Doel("bestellingen", 15), new Doel("dierenverblijven", 3));
            case 5 -> List.of(new Doel("bewoners", 15), new Doel("bestellingen", 35), new Doel("ring1", 16));
            case 6 -> List.of(new Doel("bewoners", 22), new Doel("bestellingen", 70), new Doel("knusheid", 250));
            case 7 -> List.of(new Doel("bewoners", 32), new Doel("bestellingen", 120), new Doel("ring12", 40), new Doel("loket", 1));
            case 8 -> List.of(new Doel("bewoners", 45), new Doel("bestellingen", 200), new Doel("vadskracht", 20));
            case 9 -> List.of(new Doel("bewoners", 60), new Doel("bestellingen", 300), new Doel("plukjes", 72), new Doel("knusheid", 1000));
            case 10 -> List.of(new Doel("bewoners", 75), new Doel("bestellingen", 420), new Doel("kavels", 4), new Doel("knusheid", 2000));
            default -> List.of();
        };
    }

    // =====================================================================================================================
    // clearing and land (balans 6)
    // =====================================================================================================================

    /** Clearing one plukje of this ring (1..3). */
    public static Prijs plukje(int ring) {
        return switch (ring) {
            case 1 -> new Prijs(25, Map.of(), Map.of(), 2 * MIN);
            case 2 -> new Prijs(150, Map.of(), Map.of(), 10 * MIN);
            default -> new Prijs(500, Map.of(), Map.of(), 30 * MIN);
        };
    }

    /** The Hoeve level at which a ring of plukjes may be cleared. */
    public static int ringOpent(int ring) {
        return ring <= 1 ? 1 : ring == 2 ? 3 : 5;
    }

    /** The n-th kavel you buy (1..16): n x 600 Guhlden and n x 20 planks, at once (no builder). */
    public static Prijs kavel(int n) {
        return new Prijs(n * 600, Map.of(Goed.PLANK, n * 20), Map.of(), 0);
    }

    /** How many kavels you may own at this Hoeve level. */
    public static int maxKavels(int hoeve) {
        return switch (Math.max(1, Math.min(10, hoeve))) {
            case 6 -> 2;
            case 7 -> 4;
            case 8 -> 8;
            case 9 -> 12;
            case 10 -> 16;
            default -> 0;
        };
    }
}
