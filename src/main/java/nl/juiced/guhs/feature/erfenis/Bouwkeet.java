package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.DyeColor;

/**
 * De Erfenis: the Bouwkeet blocks of the Bouwblokkenwerkplaats (balans 3.3; ids from tools/features/bouwhulp_blokken.py).
 * Five sets, one more every two levels: L1 pastel bricks + wooden vertical plates/corners; L3 + plaster, tiles; L5 + knuffel
 * blocks, wallpaper, roof tiles, posts/beams; L7 (vadskracht) + stained glass, frames, shutters, gutter, portholes, chimney,
 * dormers; L9 (vadskracht) + knuffel patterns, lamp posts, benches, bird house, door mat. Without vadskracht the workshop
 * offers the first three sets. Blocks that don't exist (another branch) are left out.
 */
public final class Bouwkeet {
    public static final String STANDAARD = "guhs:pastelbaksteen_roze";
    static final String[] KLEUREN = {"vanille", "wolkje", "muisgrijs", "drop", "mokka", "framboos", "perzik", "boter", "pistache", "salie", "aqua",
            "hemelsblauw", "korenbloem", "lavendel", "orchidee", "roze"};
    static final String[] HOUT = {"eiken", "sparren", "berken", "jungle", "acacia", "donkereiken", "mangrove", "kersen", "bleekeiken", "bamboe",
            "karmozijn", "verwrongen"};

    private static List<List<String>> sets;

    private Bouwkeet() {
    }

    /** The five sets (each only its own new blocks), built once from the registry. */
    public static synchronized List<List<String>> sets() {
        if (sets == null) {
            List<List<String>> s = new ArrayList<>();
            List<String> a = new ArrayList<>();
            for (String k : KLEUREN) {
                a.add("pastelbaksteen_" + k);
            }
            for (String h : HOUT) {
                a.add(h + "_vplaat");
                a.add(h + "_hoekje");
            }
            s.add(a);
            List<String> b = new ArrayList<>();
            for (String k : KLEUREN) {
                b.add("pleister_" + k);
            }
            for (String t : new String[] {"guhgezicht", "hartjes", "pootjes", "dambord", "visgraat"}) {
                b.add("tegel_" + t);
            }
            s.add(b);
            List<String> c = new ArrayList<>();
            for (String k : new String[] {"roze", "lavendel", "aqua", "hemelsblauw", "boter", "perzik", "vanille", "wolkje"}) {
                c.add("knuffelblok_" + k);
            }
            for (String p : new String[] {"bloemetjes", "streepjes", "wolkjes", "guhtjes"}) {
                c.add("behang_" + p + "_roze");
                c.add("behang_" + p + "_mint");
            }
            for (String d : new String[] {"roze", "kersrood", "hemelsblauw", "mint", "lavendel", "perzik"}) {
                c.add("dakpan_" + d);
            }
            for (String h : HOUT) {
                c.add(h + "_paal");
                c.add(h + "_balk");
            }
            s.add(c);
            List<String> d = new ArrayList<>();
            for (String g : new String[] {"guh", "hartje", "bloem", "regenboog"}) {
                d.add("glasinlood_" + g);
            }
            for (String k : new String[] {"wit", "roze", "mint", "hemelsblauw"}) {
                d.add("guh_kozijn_" + k);
                d.add("guh_luik_" + k);
            }
            d.add("dakgoot");
            for (String p : new String[] {"koper", "zilver", "goud"}) {
                d.add("patrijspoort_" + p);
            }
            d.add("guh_schoorsteen");
            d.add("dakkapel_roze");
            d.add("dakkapel_wit");
            s.add(d);
            List<String> e = new ArrayList<>(List.of("knuffelblok_zebra", "knuffelblok_giraffe", "knuffelblok_tijger", "knuffelblok_panda",
                    "guh_lantaarnpaal", "guh_bankje_hout", "guh_bankje_roze", "vogelhuisje", "deurmat_welkom"));
            s.add(e);
            List<List<String>> klaar = new ArrayList<>();
            for (List<String> set : s) {
                List<String> echt = new ArrayList<>();
                for (String id : set) {
                    if (Goed.item("guhs:" + id) != null) {
                        echt.add("guhs:" + id);
                    }
                }
                klaar.add(List.copyOf(echt));
            }
            sets = List.copyOf(klaar);
        }
        return sets;
    }

    /** How many sets a workshop of this level has (with or without its vadskracht). */
    public static int aantalSets(int level, boolean vk) {
        int n = level <= 2 ? 1 : level <= 4 ? 2 : level <= 6 ? 3 : level <= 8 ? 4 : 5;
        return vk ? n : Math.min(3, n);
    }

    /** Every block this workshop may make now. */
    public static List<String> beschikbaar(Gebouw g, Base b) {
        Boolean vk = Productie.vadskracht(b).get(g.id);
        int n = aantalSets(Math.max(1, g.level), vk == null || vk);
        List<String> l = new ArrayList<>();
        List<List<String>> s = sets();
        for (int i = 0; i < Math.min(n, s.size()); i++) {
            l.addAll(s.get(i));
        }
        if (l.isEmpty()) {
            l.add(STANDAARD);
        }
        return l;
    }

    /** The blocks it really makes: its selection, minus what it may not (any more); never empty. */
    public static List<String> geldig(Gebouw g, Base b) {
        List<String> mag = beschikbaar(g, b);
        List<String> l = new ArrayList<>();
        for (String s : g.selectie) {
            if (mag.contains(s)) {
                l.add(s);
            }
        }
        if (l.isEmpty()) {
            l.add(mag.get(0));
        }
        return l;
    }

    /** The candle item of a colour choice (0 = plain, 1..16 = the dye colours). */
    public static String kaars(int kleur) {
        if (kleur <= 0 || kleur > 16) {
            return "minecraft:candle";
        }
        return "minecraft:" + DyeColor.byId(kleur - 1).getSerializedName() + "_candle";
    }

    /** (tests) the set sizes. */
    public static Map<Integer, Integer> grootte() {
        Map<Integer, Integer> m = new java.util.TreeMap<>();
        for (int i = 0; i < sets().size(); i++) {
            m.put(i + 1, sets().get(i).size());
        }
        return m;
    }
}
