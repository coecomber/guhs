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
 * Every recipe of the 2 making ambachten (1.10.4: Smeden = smeden + naaien, Koken = koken + brouwen; the tier per zone, A3). Data in code (the website export {@code /guhs vadsrijk
 * ambacht export} writes them as JSON): id, ambacht, level, tier, ingredients (each a list of material ids that may be used, any
 * quality; the better the quality, the higher the Vadsigheid) and what comes out:
 * <ul>
 *   <li>{@code eten:<id>}, {@code drank:<id>}: a dish/potion (x {@link Recept#aantal});</li>
 *   <li>{@code sieraad:<soort>}: Maatwerk jewellery (choose 2 bonuses), {@code meester:<soort>}: Meesterwerk jewellery (3 bonuses + a
 *   property, needs a boss core); ({@code gear:<soort>}, Maatwerk weapons/armour, was removed in 1.10.4);</li>
 *   <li>{@code verhaal:<id>}: a story item ({@link #VERHAAL}); {@code item:<id>}: an existing item ({@link #WAREN}: the buff potions);</li>
 *   <li>{@code slijp:<0|1>}: Koken cuts 3 crystals of one kind one quality up (0 ruw -> geslepen, 1 geslepen -> stralend);</li>
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

        /** Gear you choose bonuses on (1.10.4: "sieraad" = Maatwerk jewellery; "gear" only for old data). */
        public boolean kiest() {
            return soort().equals("gear") || soort().equals("sieraad") || soort().equals("meester");
        }

        public int keuzes() {
            return soort().equals("meester") ? 3 : kiest() ? 2 : 0;
        }
    }

    public static final Map<String, Recept> ALLE = new LinkedHashMap<>();
    /** The cosmetics/deco of {@code post:<id>}. */
    public static final Map<String, Supplier<ItemStack>> POST = new LinkedHashMap<>();
    static final List<String> VIS1 = List.of("kloddervisje", "maanbaarsje", "plonsguppie"),
            VIS1_HONING = List.of("kloddervisje", "maanbaarsje", "plonsguppie", "klaverhoning"), VIS2 = List.of("spiegelforel", "sterrenzalm", "sneeuwsnoek"),
            VIS3 = List.of("wortelpaling", "lantaarnvis", "honingkarper"), HONING = List.of("bloesemhoning", "schemerhoning", "wortelhoning");
    /** U-AMBACHT2: the zone-4/5 fish and honey (any kind of the tier). */
    static final List<String> VIS4 = List.of("koraalbaars", "maanrog", "regenboogmakreel"), VIS5 = List.of("wolkenvis", "sterrenspiering", "donderforel"),
            HONING4 = List.of("lampionhoning", "duinhoning"), HONING5 = List.of("wolkenhoning", "hemelhoning");
    /** 1.10.4: the zone-6 fish (T6, Het Zijdeland). */
    static final List<String> VIS6 = List.of("oasekarper", "sterrenschubvis", "zandaal");
    /**
     * 1.10.4: the jewellery Smeden makes (Maatwerk + Meesterwerk). Per tier: ring = 5 ore + 1 wood, ketting = 4 ore + 2 fibre, oorbel =
     * 4 ore + 1 herb, armband = 3 ore + 2 wood + 1 fibre (from the tier's level + 0..3); Meesterwerk = 4 ore + glinstergruis + the core
     * (at {@link #MEESTER_LEVEL}). Written out line by line below (the wiki reads the literal lines).
     */
    public static final List<String> SIERADEN = List.of("ring", "ketting", "oorbel", "armband");
    /** 1.10.4: the Smeden level of each tier's Meesterwerk (and boss core from splinters); index = tier (the recipe lines match). */
    public static final int[] MEESTER_LEVEL = {0, 8, 18, 28, 37, 46, 54};
    /** 1.10.4: the removed Maatwerk gear (no recipes any more; items and base types stay registered so nothing breaks). */
    public static final List<String> GESCHRAPT = List.of("zwaard", "boog", "staf", "helm", "borstplaat", "broek", "schoentjes");
    /**
     * 1.10.4: story items Smeden can make ({@code verhaal:<id>}: a recipe kind for the story steps of phase 2, e.g. the temple key).
     * Empty now; fill with {@link #verhaalRecept}.
     */
    public static final Map<String, Supplier<ItemStack>> VERHAAL = new LinkedHashMap<>();
    /** 1.10.4: existing items Koken can make ({@code item:<id>}): the buff potions the shops no longer sell. */
    public static final Map<String, Supplier<ItemStack>> WAREN = new LinkedHashMap<>();

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
        POST.put("vlieger", () -> new ItemStack(Materialen.VLIEGER.get()));   // U-AMBACHT2
        WAREN.put("snelpootjesdrankje", () -> new ItemStack(EcoSpul.SNELPOOTJESDRANKJE.get()));   // 1.10.4: no longer in the shops
        WAREN.put("vahoegdrankje", () -> new ItemStack(EcoSpul.VAHOEGDRANKJE.get()));

        // ---- Koken (1.10.4: koken + brouwen; levels on the new tier scale T1 1, T2 12, T3 22, T4 32, T5 40, T6 48) --------------
        //   (1.10.4: the islet's klaverhoning may stand in for the fish of the pudding and the mushroom stalk of the energy potion)
        r("klodderpudding", Ambacht.KOKEN, 1, 1, "eten:klodderpudding", 2, i(2, "klodderklaver"), i(1, VIS1_HONING));
        r("vadsdrankje", Ambacht.KOKEN, 1, 1, "drank:vadsdrankje", 2, i(2, "klodderklaver"));
        r("visstoofje", Ambacht.KOKEN, 3, 1, "eten:visstoofje", 2, i(2, VIS1), i(1, "zwamsteel"));
        r("energiedrankje", Ambacht.KOKEN, 3, 1, "drank:energiedrankje", 2, i(1, "klodderklaver"), i(1, "zwamsteel", "klaverhoning"));
        r("snelpootjesdrankje", Ambacht.KOKEN, 4, 1, "item:snelpootjesdrankje", 2, i(2, "klodderklaver"), i(1, VIS1));
        r("vahoegdrankje", Ambacht.KOKEN, 5, 1, "item:vahoegdrankje", 2, i(1, "klodderklaver"), i(1, "pluiskatoen"), i(1, "zwamsteel"));
        r("verzamelaarshapje", Ambacht.KOKEN, 6, 1, "eten:verzamelaarshapje", 2, i(2, "klodderklaver"), i(1, "pluiskatoen"), i(1, VIS1));
        r("slijp_geslepen", Ambacht.KOKEN, 9, 2, "slijp:0", 1);
        r("kandijsoep", Ambacht.KOKEN, 12, 2, "eten:kandijsoep", 2, i(2, "ijsbloem"), i(1, VIS2), i(1, "kristalden"));
        r("grote_vadsdrank", Ambacht.KOKEN, 12, 2, "drank:grote_vadsdrank", 2, i(2, "ijsbloem"), i(1, "klodderklaver"));
        r("grote_energiedrank", Ambacht.KOKEN, 13, 2, "drank:grote_energiedrank", 2, i(1, "ijsbloem"), i(1, VIS2));
        r("forelbroodje", Ambacht.KOKEN, 15, 2, "eten:forelbroodje", 2, i(2, "spiegelforel"), i(1, "ijsbloem"));
        r("ridderontbijt", Ambacht.KOKEN, 19, 2, "eten:ridderontbijt", 2, i(2, VIS2), i(1, "ijsbloem"), i(2, "klodderklaver"));
        r("honingpannenkoek", Ambacht.KOKEN, 22, 3, "eten:honingpannenkoek", 2, i(1, HONING), i(2, "lichtkeverkruid"));
        r("reuzen_vadsdrank", Ambacht.KOKEN, 22, 3, "drank:reuzen_vadsdrank", 2, i(2, "lichtkeverkruid"), i(1, HONING));
        r("reuzen_energiedrank", Ambacht.KOKEN, 23, 3, "drank:reuzen_energiedrank", 2, i(1, "lichtkeverkruid"), i(1, "roze_hars"));
        r("wortelstamppot", Ambacht.KOKEN, 24, 3, "eten:wortelstamppot", 2, i(2, "wortelpaling"), i(1, "lichtkeverkruid"), i(1, "schemerhout"));
        r("slijp_stralend", Ambacht.KOKEN, 25, 3, "slijp:1", 1, i(1, "glinstergruis"));
        r("feestmaal", Ambacht.KOKEN, 27, 3, "eten:feestmaal", 1, i(2, VIS3), i(2, HONING), i(2, "lichtkeverkruid"), i(1, "schemerhout"));
        r("honingelixer", Ambacht.KOKEN, 27, 3, "drank:honingelixer", 2, i(1, "koninginnengelei"), i(2, HONING));
        // ---- Smeden (1.10.4: smeden + naaien = jewellery from T1; no weapons or armour any more: base gear comes from loot) ------
        //   Maatwerk jewellery (Episch, choose 2 bonuses) from the tier's ore + wood/fibre/herb; Meesterwerk (Legendarisch, choose 3 +
        //   a property) with a boss core. The best piece of a tier is ~10 % above that zone's jewellery drops (Maatwerk.normaliseer).
        r("ring_t1", Ambacht.SMEDEN, 1, 1, "sieraad:ring", 1, i(5, "pluisijzer"), i(1, "zwamsteel"));
        r("ketting_t1", Ambacht.SMEDEN, 2, 1, "sieraad:ketting", 1, i(4, "pluisijzer"), i(2, "pluiskatoen"));
        r("oorbel_t1", Ambacht.SMEDEN, 3, 1, "sieraad:oorbel", 1, i(4, "pluisijzer"), i(1, "klodderklaver"));
        r("armband_t1", Ambacht.SMEDEN, 4, 1, "sieraad:armband", 1, i(3, "pluisijzer"), i(2, "zwamsteel"), i(1, "pluiskatoen"));
        r("meester_ring_t1", Ambacht.SMEDEN, 8, 1, "meester:ring", 1, i(4, "pluisijzer"), i(1, "glinstergruis"), i(1, "zwamkern"));
        r("meester_ketting_t1", Ambacht.SMEDEN, 8, 1, "meester:ketting", 1, i(4, "pluisijzer"), i(1, "glinstergruis"), i(1, "zwamkern"));
        r("meester_oorbel_t1", Ambacht.SMEDEN, 8, 1, "meester:oorbel", 1, i(4, "pluisijzer"), i(1, "glinstergruis"), i(1, "zwamkern"));
        r("meester_armband_t1", Ambacht.SMEDEN, 8, 1, "meester:armband", 1, i(4, "pluisijzer"), i(1, "glinstergruis"), i(1, "zwamkern"));
        r("ring_t2", Ambacht.SMEDEN, 12, 2, "sieraad:ring", 1, i(5, "kandijzilver"), i(1, "kristalden"));
        r("ketting_t2", Ambacht.SMEDEN, 13, 2, "sieraad:ketting", 1, i(4, "kandijzilver"), i(2, "kristalvlas"));
        r("oorbel_t2", Ambacht.SMEDEN, 14, 2, "sieraad:oorbel", 1, i(4, "kandijzilver"), i(1, "ijsbloem"));
        r("armband_t2", Ambacht.SMEDEN, 15, 2, "sieraad:armband", 1, i(3, "kandijzilver"), i(2, "kristalden"), i(1, "kristalvlas"));
        r("meester_ring_t2", Ambacht.SMEDEN, 18, 2, "meester:ring", 1, i(4, "kandijzilver"), i(2, "glinstergruis"), i(1, "splinterkern"));
        r("meester_ketting_t2", Ambacht.SMEDEN, 18, 2, "meester:ketting", 1, i(4, "kandijzilver"), i(2, "glinstergruis"), i(1, "splinterkern"));
        r("meester_oorbel_t2", Ambacht.SMEDEN, 18, 2, "meester:oorbel", 1, i(4, "kandijzilver"), i(2, "glinstergruis"), i(1, "splinterkern"));
        r("meester_armband_t2", Ambacht.SMEDEN, 18, 2, "meester:armband", 1, i(4, "kandijzilver"), i(2, "glinstergruis"), i(1, "splinterkern"));
        r("ring_t3", Ambacht.SMEDEN, 22, 3, "sieraad:ring", 1, i(5, "amberkoper"), i(1, "schemerhout"));
        r("ketting_t3", Ambacht.SMEDEN, 23, 3, "sieraad:ketting", 1, i(4, "amberkoper"), i(2, "schemerzijde"));
        r("oorbel_t3", Ambacht.SMEDEN, 24, 3, "sieraad:oorbel", 1, i(4, "amberkoper"), i(1, "lichtkeverkruid"));
        r("armband_t3", Ambacht.SMEDEN, 25, 3, "sieraad:armband", 1, i(3, "amberkoper"), i(2, "schemerhout"), i(1, "schemerzijde"));
        r("meester_ring_t3", Ambacht.SMEDEN, 28, 3, "meester:ring", 1, i(4, "amberkoper"), i(3, "glinstergruis"), i(1, "wortelkern"));
        r("meester_ketting_t3", Ambacht.SMEDEN, 28, 3, "meester:ketting", 1, i(4, "amberkoper"), i(3, "glinstergruis"), i(1, "wortelkern"));
        r("meester_oorbel_t3", Ambacht.SMEDEN, 28, 3, "meester:oorbel", 1, i(4, "amberkoper"), i(3, "glinstergruis"), i(1, "wortelkern"));
        r("meester_armband_t3", Ambacht.SMEDEN, 28, 3, "meester:armband", 1, i(4, "amberkoper"), i(3, "glinstergruis"), i(1, "wortelkern"));
        r("ring_t4", Ambacht.SMEDEN, 32, 4, "sieraad:ring", 1, i(5, "lampionbrons"), i(1, "drijfhout"));
        r("ketting_t4", Ambacht.SMEDEN, 33, 4, "sieraad:ketting", 1, i(4, "lampionbrons"), i(2, "zeegaren"));
        r("oorbel_t4", Ambacht.SMEDEN, 34, 4, "sieraad:oorbel", 1, i(4, "lampionbrons"), i(1, "zeekraal"));
        r("armband_t4", Ambacht.SMEDEN, 35, 4, "sieraad:armband", 1, i(3, "lampionbrons"), i(2, "drijfhout"), i(1, "zeegaren"));
        r("meester_ring_t4", Ambacht.SMEDEN, 37, 4, "meester:ring", 1, i(4, "lampionbrons"), i(3, "glinstergruis"), i(2, "roze_koraal"), i(1, "lampionkern"));
        r("meester_ketting_t4", Ambacht.SMEDEN, 37, 4, "meester:ketting", 1, i(4, "lampionbrons"), i(3, "glinstergruis"), i(2, "roze_koraal"), i(1, "lampionkern"));
        r("meester_oorbel_t4", Ambacht.SMEDEN, 37, 4, "meester:oorbel", 1, i(4, "lampionbrons"), i(3, "glinstergruis"), i(2, "roze_koraal"), i(1, "lampionkern"));
        r("meester_armband_t4", Ambacht.SMEDEN, 37, 4, "meester:armband", 1, i(4, "lampionbrons"), i(3, "glinstergruis"), i(2, "roze_koraal"), i(1, "lampionkern"));
        r("ring_t5", Ambacht.SMEDEN, 40, 5, "sieraad:ring", 1, i(5, "hemelzilver"), i(1, "wolkenhout"));
        r("ketting_t5", Ambacht.SMEDEN, 41, 5, "sieraad:ketting", 1, i(4, "hemelzilver"), i(2, "wolkenwol"));
        r("oorbel_t5", Ambacht.SMEDEN, 42, 5, "sieraad:oorbel", 1, i(4, "hemelzilver"), i(1, "wolkbes"));
        r("armband_t5", Ambacht.SMEDEN, 43, 5, "sieraad:armband", 1, i(3, "hemelzilver"), i(2, "wolkenhout"), i(1, "wolkenwol"));
        r("meester_ring_t5", Ambacht.SMEDEN, 46, 5, "meester:ring", 1, i(4, "hemelzilver"), i(4, "glinstergruis"), i(1, "wolkkern"));
        r("meester_ketting_t5", Ambacht.SMEDEN, 46, 5, "meester:ketting", 1, i(4, "hemelzilver"), i(4, "glinstergruis"), i(1, "wolkkern"));
        r("meester_oorbel_t5", Ambacht.SMEDEN, 46, 5, "meester:oorbel", 1, i(4, "hemelzilver"), i(4, "glinstergruis"), i(1, "wolkkern"));
        r("meester_armband_t5", Ambacht.SMEDEN, 46, 5, "meester:armband", 1, i(4, "hemelzilver"), i(4, "glinstergruis"), i(1, "wolkkern"));
        r("kern_zwam", Ambacht.SMEDEN, 8, 1, "kern:zwamkern", 1, i(5, "kernsplinter"));
        r("kern_splinter", Ambacht.SMEDEN, 18, 2, "kern:splinterkern", 1, i(5, "kernsplinter"));
        r("kern_wortel", Ambacht.SMEDEN, 28, 3, "kern:wortelkern", 1, i(5, "kernsplinter"));
        r("kern_lampion", Ambacht.SMEDEN, 37, 4, "kern:lampionkern", 1, i(5, "kernsplinter"));
        r("kern_wolk", Ambacht.SMEDEN, 46, 5, "kern:wolkkern", 1, i(5, "kernsplinter"));
        // 1.10.4: T6 (zone 6, Het Zijdeland): sterrengoud + date palm wood / silk thread / saffron; Meesterwerk with incense resin
        // (made only at the zone-6 teachers in Saffraanpoort: Werkplaats.magHier)
        r("ring_t6", Ambacht.SMEDEN, 48, 6, "sieraad:ring", 1, i(5, "sterrengoud"), i(1, "dadelpalmhout"));
        r("ketting_t6", Ambacht.SMEDEN, 49, 6, "sieraad:ketting", 1, i(4, "sterrengoud"), i(2, "zijdedraad"));
        r("oorbel_t6", Ambacht.SMEDEN, 50, 6, "sieraad:oorbel", 1, i(4, "sterrengoud"), i(1, "saffraan"));
        r("armband_t6", Ambacht.SMEDEN, 51, 6, "sieraad:armband", 1, i(3, "sterrengoud"), i(2, "dadelpalmhout"), i(1, "zijdedraad"));
        r("meester_ring_t6", Ambacht.SMEDEN, 54, 6, "meester:ring", 1, i(4, "sterrengoud"), i(4, "glinstergruis"), i(2, "wierookhars"), i(1, "sterrenkern"));
        r("meester_ketting_t6", Ambacht.SMEDEN, 54, 6, "meester:ketting", 1, i(4, "sterrengoud"), i(4, "glinstergruis"), i(2, "wierookhars"), i(1, "sterrenkern"));
        r("meester_oorbel_t6", Ambacht.SMEDEN, 54, 6, "meester:oorbel", 1, i(4, "sterrengoud"), i(4, "glinstergruis"), i(2, "wierookhars"), i(1, "sterrenkern"));
        r("meester_armband_t6", Ambacht.SMEDEN, 54, 6, "meester:armband", 1, i(4, "sterrengoud"), i(4, "glinstergruis"), i(2, "wierookhars"), i(1, "sterrenkern"));
        r("kern_sterren", Ambacht.SMEDEN, 54, 6, "kern:sterrenkern", 1, i(5, "kernsplinter"));
        // cosmetics and deco that go home by the Post (what the old smidse and naaiatelier made)
        r("post_kokschort", Ambacht.SMEDEN, 3, 1, "post:kokschort", 1, i(5, "pluiskatoen"));
        r("post_mijnhelm", Ambacht.SMEDEN, 7, 1, "post:mijnhelm", 1, i(4, "pluisijzer"), i(1, "glinstergruis"));
        r("post_vaandel", Ambacht.SMEDEN, 7, 1, "post:vaandel", 1, i(4, "pluiskatoen"), i(2, "zwamsteel"));
        r("post_vissershoedje", Ambacht.SMEDEN, 11, 1, "post:vissershoedje", 1, i(4, "pluiskatoen"), i(1, VIS1));
        r("post_tuinhoedje", Ambacht.SMEDEN, 15, 2, "post:tuinhoedje", 1, i(4, "kristalvlas"), i(1, "ijsbloem"));
        r("post_opperzwambeeld", Ambacht.SMEDEN, 20, 2, "post:opperzwambeeld", 1, i(6, "kandijzilver"), i(1, "oerbast"));
        r("post_imkershoed", Ambacht.SMEDEN, 23, 3, "post:imkershoed", 1, i(4, "schemerzijde"), i(2, "bijenwas"));
        r("post_mini_koning", Ambacht.SMEDEN, 27, 3, "post:mini_koning", 1, i(6, "amberkoper"), i(2, "glinstergruis"));
        r("post_klodderknuffel", Ambacht.SMEDEN, 27, 3, "post:klodderknuffel", 1, i(6, "schemerzijde"), i(4, "pluiskatoen"), i(1, "gouden_klaver"));
        // kite building (zone 4's Vliegereiland): a guh-kite for home, sewn on kite bamboe
        r("post_vlieger", Ambacht.SMEDEN, 35, 4, "post:vlieger", 1, i(3, "zeegaren"), i(2, "vliegerbamboe"));

        // ---- U-AMBACHT2: T4 (zone 4, De Lampionkust) and T5 (zone 5, Hoogvads) Koken ------------------------------------------------
        // (T4/T5 are made in Hoogvads' Ambachtsgilde; T4 Koken also at Kok Garnaal's kitchen in Lampionhaven: see Werkplaats.magHier)
        r("zeekraalsalade", Ambacht.KOKEN, 32, 4, "eten:zeekraalsalade", 2, i(2, "zeekraal"), i(1, VIS4));
        r("zee_vadsdrank", Ambacht.KOKEN, 32, 4, "drank:zee_vadsdrank", 2, i(2, "zeekraal"), i(1, "zeesponsje"));
        r("koraalvissoep", Ambacht.KOKEN, 34, 4, "eten:koraalvissoep", 2, i(2, VIS4), i(1, "zeekraal"), i(1, "roze_koraal"));
        r("zee_energiedrank", Ambacht.KOKEN, 35, 4, "drank:zee_energiedrank", 2, i(1, "zeekraal"), i(1, HONING4));
        r("zeemanshapje", Ambacht.KOKEN, 37, 4, "eten:zeemanshapje", 2, i(2, "zeekraal"), i(1, HONING4), i(1, VIS4));
        r("lampionspiesjes", Ambacht.KOKEN, 38, 4, "eten:lampionspiesjes", 2, i(2, "koraalbaars"), i(1, HONING4), i(1, "vliegerbamboe"));
        r("wolkbessentaart", Ambacht.KOKEN, 40, 5, "eten:wolkbessentaart", 2, i(3, "wolkbes"), i(1, HONING5));
        r("wolken_vadsdrank", Ambacht.KOKEN, 40, 5, "drank:wolken_vadsdrank", 2, i(2, "wolkbes"), i(1, HONING5));
        r("donderstoofpot", Ambacht.KOKEN, 43, 5, "eten:donderstoofpot", 2, i(2, "donderforel"), i(1, "wolkbes"), i(1, "wolkenhout"));
        r("wolken_energiedrank", Ambacht.KOKEN, 43, 5, "drank:wolken_energiedrank", 2, i(1, "wolkbes"), i(1, VIS5));
        r("hemelfeestmaal", Ambacht.KOKEN, 46, 5, "eten:hemelfeestmaal", 1, i(2, VIS5), i(2, HONING5), i(2, "wolkbes"), i(1, "wolkenhout"));
        r("hemelelixer", Ambacht.KOKEN, 47, 5, "drank:hemelelixer", 2, i(1, "koninginnengelei"), i(2, HONING5), i(1, "wolkbes"));
        // ---- 1.10.4: T6 (zone 6, Het Zijdeland) Koken: saffron, date palm and the oasis fish (only at the zone-6 teachers) --------
        r("saffraanrijst", Ambacht.KOKEN, 48, 6, "eten:saffraanrijst", 2, i(3, "saffraan"), i(1, VIS6));
        r("saffraan_vadsdrank", Ambacht.KOKEN, 48, 6, "drank:saffraan_vadsdrank", 2, i(2, "saffraan"), i(1, "saffraanhoning"));
        r("dadelstoofpot", Ambacht.KOKEN, 51, 6, "eten:dadelstoofpot", 2, i(2, "oasekarper"), i(1, "saffraan"), i(1, "dadelpalmhout"));
        r("saffraan_energiedrank", Ambacht.KOKEN, 51, 6, "drank:saffraan_energiedrank", 2, i(1, "saffraan"), i(1, VIS6));
        r("sterrenfeestmaal", Ambacht.KOKEN, 54, 6, "eten:sterrenfeestmaal", 1, i(2, VIS6), i(2, "saffraanhoning"), i(2, "saffraan"), i(1, "dadelpalmhout"));
        r("sterrenelixer", Ambacht.KOKEN, 56, 6, "drank:sterrenelixer", 2, i(1, "koninginnengelei"), i(2, "saffraanhoning"), i(1, "wierookhars"));
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

    /** An ingredient: {@code n} of any of these material ids (any quality). For other slices' recipes. */
    public static Ingredient ingredient(int n, String... opties) {
        return i(n, opties);
    }

    /**
     * 1.10.4: a story-item recipe (phase 2: e.g. the temple key, made at the smidse): recipe {@code id} of {@code a} from level
     * {@code level}, tier {@code tier}, giving {@code item} (into the bag, never by post). Name: {@code vadsrijk.ambacht.recept.<id>}.
     */
    public static Recept verhaalRecept(String id, Ambacht a, int level, int tier, Supplier<ItemStack> item, Ingredient... in) {
        VERHAAL.put(id, item);
        Recept r = new Recept(id, a, level, tier, List.of(in), "verhaal:" + id, 1);
        ALLE.put(id, r);
        return r;
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
            case "gear", "sieraad", "meester" -> new ItemStack(Maatwerk.ITEM.get(r.doel()).get());
            case "verhaal" -> VERHAAL.containsKey(r.doel()) ? VERHAAL.get(r.doel()).get() : ItemStack.EMPTY;
            case "item" -> WAREN.containsKey(r.doel()) ? WAREN.get(r.doel()).get().copyWithCount(r.aantal()) : ItemStack.EMPTY;
            case "slijp" -> Kristallen.stack("rozekwarts", Integer.parseInt(r.doel()) + 1, 1);
            case "kern" -> new ItemStack(Materialen.KERN.get(r.doel()).get());
            case "post" -> POST.get(r.doel()).get();
            default -> ItemStack.EMPTY;
        };
    }
}
