package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.registry.ModItems;

/**
 * Every recipe of the 4 making ambachten (and the tier per zone, A3). Data in code (the website export {@code /guhs vadsrijk
 * ambacht export} writes them as JSON): id, ambacht, level, tier, ingredients (each a list of material ids that may be used, any
 * quality; the better the quality, the higher the Vadsigheid) and what comes out:
 * <ul>
 *   <li>{@code eten:<id>}, {@code drank:<id>}: a dish/potion (x {@link Recept#aantal});</li>
 *   <li>{@code gear:<soort>}: Maatwerk (choose 2 bonuses), {@code meester:<soort>}: Meesterwerk jewellery (3 bonuses + a property,
 *   needs a boss core);</li>
 *   <li>{@code slijp:<0|1>}: Brouwen cuts 3 crystals of one kind one quality up (0 ruw -> geslepen, 1 geslepen -> stralend);</li>
 *   <li>{@code kern:<id>}: 5 kernsplinters -> a boss core;</li>
 *   <li>{@code post:<id>}: cosmetics/deco that go HOME by the Post (A2: decoration only, never power).</li>
 * </ul>
 * Names: {@code vadsrijk.ambacht.recept.<id>}.
 */
public final class Recepten {
    public record Ingredient(List<String> opties, int aantal) {
        public Predicate<ItemStack> past() {
            return s -> {
                Materialen.Materiaal m = Materialen.van(s);
                if (m != null) {
                    return opties.contains(m.id());
                }
                for (String o : opties) {
                    if (Materialen.KERN.containsKey(o) && s.is(Materialen.KERN.get(o).get())) {
                        return true;
                    }
                    if (o.equals("kernsplinter") && s.is(Materialen.KERNSPLINTER.get())) {
                        return true;
                    }
                }
                return false;
            };
        }
    }

    public record Recept(String id, Ambacht ambacht, int level, int tier, List<Ingredient> in, String uit, int aantal) {
        public String soort() {
            return uit.substring(0, uit.indexOf(':'));
        }

        public String doel() {
            return uit.substring(uit.indexOf(':') + 1);
        }

        public String naamKey() {
            return "vadsrijk.ambacht.recept." + id;
        }

        public int eenheden() {
            int n = 0;
            for (Ingredient i : in) {
                n += i.aantal();
            }
            return soort().equals("slijp") ? 3 : n;
        }

        /** Gear you choose bonuses on. */
        public boolean kiest() {
            return soort().equals("gear") || soort().equals("meester");
        }

        public int keuzes() {
            return soort().equals("meester") ? 3 : soort().equals("gear") ? 2 : 0;
        }
    }

    public static final Map<String, Recept> ALLE = new LinkedHashMap<>();
    /** The cosmetics/deco of {@code post:<id>}. */
    public static final Map<String, Supplier<ItemStack>> POST = new LinkedHashMap<>();
    static final List<String> VIS1 = List.of("kloddervisje", "maanbaarsje", "plonsguppie"), VIS2 = List.of("spiegelforel", "sterrenzalm", "sneeuwsnoek"),
            VIS3 = List.of("wortelpaling", "lantaarnvis", "honingkarper"), HONING = List.of("bloesemhoning", "schemerhoning", "wortelhoning");

    static {
        POST.put("kokschort", () -> new ItemStack(ModItems.clothingItem(GuhClothes.GRILL_SCHORT)));
        POST.put("vissershoedje", () -> new ItemStack(ModItems.clothingItem(GuhClothes.VISSERSHOEDJE)));
        POST.put("tuinhoedje", () -> new ItemStack(ModItems.clothingItem(GuhClothes.TUINHOEDJE)));
        POST.put("imkershoed", () -> new ItemStack(ModItems.clothingItem(GuhClothes.STRAW_HAT)));
        POST.put("mijnhelm", () -> new ItemStack(ModItems.clothingItem(GuhClothes.KAASMIJN_HELM)));
        POST.put("vaandel", () -> new ItemStack(EcoSpul.VADSRIJK_VAANDEL.get()));
        POST.put("klodderknuffel", () -> new ItemStack(EcoSpul.KLODDERREUS_KNUFFEL.get()));
        POST.put("opperzwambeeld", () -> new ItemStack(EcoSpul.OPPERZWAM_BEELD.get()));
        POST.put("mini_koning", () -> new ItemStack(EcoSpul.MINI_KONING_KLODDER.get()));

        // ---- Koken ----------------------------------------------------------------------------------------------------------
        r("klodderpudding", Ambacht.KOKEN, 1, 1, "eten:klodderpudding", 2, i(2, "klodderklaver"), i(1, VIS1));
        r("visstoofje", Ambacht.KOKEN, 5, 1, "eten:visstoofje", 2, i(2, VIS1), i(1, "zwamsteel"));
        r("verzamelaarshapje", Ambacht.KOKEN, 10, 1, "eten:verzamelaarshapje", 2, i(2, "klodderklaver"), i(1, "pluiskatoen"), i(1, VIS1));
        r("kandijsoep", Ambacht.KOKEN, 20, 2, "eten:kandijsoep", 2, i(2, "ijsbloem"), i(1, VIS2), i(1, "kristalden"));
        r("forelbroodje", Ambacht.KOKEN, 25, 2, "eten:forelbroodje", 2, i(2, "spiegelforel"), i(1, "ijsbloem"));
        r("ridderontbijt", Ambacht.KOKEN, 30, 2, "eten:ridderontbijt", 2, i(2, VIS2), i(1, "ijsbloem"), i(2, "klodderklaver"));
        r("honingpannenkoek", Ambacht.KOKEN, 35, 3, "eten:honingpannenkoek", 2, i(1, HONING), i(2, "lichtkeverkruid"));
        r("wortelstamppot", Ambacht.KOKEN, 38, 3, "eten:wortelstamppot", 2, i(2, "wortelpaling"), i(1, "lichtkeverkruid"), i(1, "schemerhout"));
        r("feestmaal", Ambacht.KOKEN, 42, 3, "eten:feestmaal", 1, i(2, VIS3), i(2, HONING), i(2, "lichtkeverkruid"), i(1, "schemerhout"));
        // ---- Brouwen --------------------------------------------------------------------------------------------------------
        r("vadsdrankje", Ambacht.BROUWEN, 1, 1, "drank:vadsdrankje", 2, i(2, "klodderklaver"));
        r("energiedrankje", Ambacht.BROUWEN, 5, 1, "drank:energiedrankje", 2, i(1, "klodderklaver"), i(1, "zwamsteel"));
        r("slijp_geslepen", Ambacht.BROUWEN, 15, 2, "slijp:0", 1);
        r("grote_vadsdrank", Ambacht.BROUWEN, 20, 2, "drank:grote_vadsdrank", 2, i(2, "ijsbloem"), i(1, "klodderklaver"));
        r("grote_energiedrank", Ambacht.BROUWEN, 22, 2, "drank:grote_energiedrank", 2, i(1, "ijsbloem"), i(1, VIS2));
        r("reuzen_vadsdrank", Ambacht.BROUWEN, 35, 3, "drank:reuzen_vadsdrank", 2, i(2, "lichtkeverkruid"), i(1, HONING));
        r("reuzen_energiedrank", Ambacht.BROUWEN, 37, 3, "drank:reuzen_energiedrank", 2, i(1, "lichtkeverkruid"), i(1, "roze_hars"));
        r("slijp_stralend", Ambacht.BROUWEN, 40, 3, "slijp:1", 1, i(1, "glinstergruis"));
        r("honingelixer", Ambacht.BROUWEN, 42, 3, "drank:honingelixer", 2, i(1, "koninginnengelei"), i(2, HONING));
        // ---- Smeden: Maatwerk weapons + helm + borstplaat; Meesterwerk jewellery; cores; deco -------------------------------
        for (String s : List.of("zwaard", "boog", "staf", "helm", "borstplaat")) {
            r(s + "_t1", Ambacht.SMEDEN, 10, 1, "gear:" + s, 1, i(6, "pluisijzer"), i(2, "zwamsteel"));
            r(s + "_t2", Ambacht.SMEDEN, 25, 2, "gear:" + s, 1, i(6, "kandijzilver"), i(2, "kristalden"));
            r(s + "_t3", Ambacht.SMEDEN, 40, 3, "gear:" + s, 1, i(6, "amberkoper"), i(2, "schemerhout"), i(1, "roze_hars"));
        }
        for (String s : List.of("ring", "ketting", "oorbel")) {
            r(s + "_t1", Ambacht.SMEDEN, 15, 1, "meester:" + s, 1, i(4, "pluisijzer"), i(1, "glinstergruis"), i(1, "zwamkern"));
            r(s + "_t2", Ambacht.SMEDEN, 30, 2, "meester:" + s, 1, i(4, "kandijzilver"), i(2, "glinstergruis"), i(1, "splinterkern"));
            r(s + "_t3", Ambacht.SMEDEN, 45, 3, "meester:" + s, 1, i(4, "amberkoper"), i(3, "glinstergruis"), i(1, "wortelkern"));
        }
        r("kern_zwam", Ambacht.SMEDEN, 15, 1, "kern:zwamkern", 1, i(5, "kernsplinter"));
        r("kern_splinter", Ambacht.SMEDEN, 30, 2, "kern:splinterkern", 1, i(5, "kernsplinter"));
        r("kern_wortel", Ambacht.SMEDEN, 45, 3, "kern:wortelkern", 1, i(5, "kernsplinter"));
        r("post_mijnhelm", Ambacht.SMEDEN, 12, 1, "post:mijnhelm", 1, i(4, "pluisijzer"), i(1, "glinstergruis"));
        r("post_opperzwambeeld", Ambacht.SMEDEN, 32, 2, "post:opperzwambeeld", 1, i(6, "kandijzilver"), i(1, "oerbast"));
        r("post_mini_koning", Ambacht.SMEDEN, 44, 3, "post:mini_koning", 1, i(6, "amberkoper"), i(2, "glinstergruis"));
        // ---- Naaien: Maatwerk broek + schoentjes; cosmetics and deco by post --------------------------------------------------
        for (String s : List.of("broek", "schoentjes")) {
            r(s + "_t1", Ambacht.NAAIEN, 10, 1, "gear:" + s, 1, i(6, "pluiskatoen"), i(1, "klodderklaver"));
            r(s + "_t2", Ambacht.NAAIEN, 25, 2, "gear:" + s, 1, i(6, "kristalvlas"), i(1, "kandijzilver"));
            r(s + "_t3", Ambacht.NAAIEN, 40, 3, "gear:" + s, 1, i(6, "schemerzijde"), i(1, "amberkoper"), i(1, "bijenwas"));
        }
        r("post_kokschort", Ambacht.NAAIEN, 5, 1, "post:kokschort", 1, i(5, "pluiskatoen"));
        r("post_vaandel", Ambacht.NAAIEN, 12, 1, "post:vaandel", 1, i(4, "pluiskatoen"), i(2, "zwamsteel"));
        r("post_vissershoedje", Ambacht.NAAIEN, 18, 1, "post:vissershoedje", 1, i(4, "pluiskatoen"), i(1, VIS1));
        r("post_tuinhoedje", Ambacht.NAAIEN, 24, 2, "post:tuinhoedje", 1, i(4, "kristalvlas"), i(1, "ijsbloem"));
        r("post_imkershoed", Ambacht.NAAIEN, 36, 3, "post:imkershoed", 1, i(4, "schemerzijde"), i(2, "bijenwas"));
        r("post_klodderknuffel", Ambacht.NAAIEN, 44, 3, "post:klodderknuffel", 1, i(6, "schemerzijde"), i(4, "pluiskatoen"), i(1, "gouden_klaver"));
    }

    private Recepten() {
    }

    private static Ingredient i(int n, String... opties) {
        return new Ingredient(List.of(opties), n);
    }

    private static Ingredient i(int n, List<String> opties) {
        return new Ingredient(opties, n);
    }

    private static void r(String id, Ambacht a, int level, int tier, String uit, int aantal, Ingredient... in) {
        ALLE.put(id, new Recept(id, a, level, tier, List.of(in), uit, aantal));
    }

    @Nullable
    public static Recept van(String id) {
        return ALLE.get(id);
    }

    public static List<Recept> van(Ambacht a) {
        List<Recept> l = new ArrayList<>();
        for (Recept r : ALLE.values()) {
            if (r.ambacht() == a) {
                l.add(r);
            }
        }
        return l;
    }

    /** What the recipe gives (one, for icons/previews; gear: an unrolled icon). */
    public static ItemStack toon(Recept r) {
        return switch (r.soort()) {
            case "eten" -> new ItemStack(Eten.ITEM.get(r.doel()).get(), r.aantal());
            case "drank" -> new ItemStack(Brouwsels.ITEM.get(r.doel()).get(), r.aantal());
            case "gear", "meester" -> new ItemStack(Maatwerk.ITEM.get(r.doel()).get());
            case "slijp" -> Kristallen.stack("rozekwarts", Integer.parseInt(r.doel()) + 1, 1);
            case "kern" -> new ItemStack(Materialen.KERN.get(r.doel()).get());
            case "post" -> POST.get(r.doel()).get();
            default -> ItemStack.EMPTY;
        };
    }
}
