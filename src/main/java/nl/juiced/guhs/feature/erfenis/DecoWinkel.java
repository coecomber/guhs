package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * De Erfenis, the decowinkel (deco.md "Winkel", ronde 4/15): what you can buy with Guhlden in the overview, per category,
 * with its price and the Hoeve level it opens on. Three kinds:
 * <ul>
 *   <li>{@link Soort#PAD}: a path kind (vormen {@code pad.json}), drawn per tile ({@link Paden}); the price is per tile.</li>
 *   <li>{@link Soort#HEK}: a 1x1 hekje (a vormen deco piece of soort "hek"), also drawn per tile; every tile is one deco piece.</li>
 *   <li>{@link Soort#STUK}: an ordinary deco piece: bought into the decoschuurtje, or bought and placed in one go.</li>
 * </ul>
 * Special pieces (orders, finds, Sherlock, Burenhulp, the makelaar) are never sold: they only come into the decoschuurtje
 * ({@link ErfenisDeco#geef}). Taking deco away is always free and keeps it (decoschuurtje / path tiles in {@link Base#padVoorraad});
 * nothing is ever sold back (ronde 15). Lang: {@code erfenis.decowinkel.cat.<categorie>}, paths {@code erfenis.pad.<soort>},
 * pieces {@code erfenis.deco.<id>}.
 */
public final class DecoWinkel {
    public enum Soort { PAD, HEK, STUK }

    public enum Categorie {
        PADEN, HEKJES, BLOEMEN, ZITJES, LICHT, WATER, BEELDEN, BOERDERIJ, BIJZONDER;

        public String naamKey() {
            return "erfenis.decowinkel.cat." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** One thing in the shop: its id (path kind or vormen deco id), category, price in Guhlden (per tile for PAD/HEK), from Hoeve level, icon item. */
    public record Stuk(String id, Categorie cat, Soort soort, int prijs, int vanaf, String icoon) {
        public String naamKey() {
            return soort == Soort.PAD ? "erfenis.pad." + id : "erfenis.deco." + id;
        }

        /** Drawn per tile (paths and hekjes). */
        public boolean getrokken() {
            return soort != Soort.STUK;
        }
    }

    private static final Map<String, Stuk> STUKKEN = new LinkedHashMap<>();

    private static void s(Categorie c, Soort soort, String id, int prijs, int vanaf, String icoon) {
        STUKKEN.put(id, new Stuk(id, c, soort, prijs, vanaf, icoon));
    }

    static {
        Categorie c = Categorie.PADEN;
        s(c, Soort.PAD, "zandpaadje", 1, 1, "minecraft:dirt_path");
        s(c, Soort.PAD, "knarspad", 2, 1, "minecraft:gravel");
        s(c, Soort.PAD, "stapsteentjes", 3, 2, "minecraft:polished_andesite");
        s(c, Soort.PAD, "klinkguhpad", 4, 3, "guhs:knuffelklinkers");
        s(c, Soort.PAD, "pootjespad", 6, 4, "guhs:tegel_pootjes");
        s(c, Soort.PAD, "kinderkopjes", 6, 5, "minecraft:cobblestone");
        s(c, Soort.PAD, "bloesemblaadjespad", 8, 6, "minecraft:pink_petals");
        s(c, Soort.PAD, "vadsgoudpad", 12, 7, "minecraft:gold_block");
        s(c, Soort.PAD, "sterrenpad", 20, 9, "guhs:glimtegel");
        c = Categorie.HEKJES;
        s(c, Soort.HEK, "wilgenhekje", 3, 1, "minecraft:bamboo_fence");
        s(c, Soort.HEK, "guhhekje_wit", 5, 2, "guhs:guh_hekje_wit");
        s(c, Soort.HEK, "guhhekje_roze", 6, 3, "guhs:guh_hekje_roze");
        s(c, Soort.HEK, "guhhekje_mint", 6, 3, "guhs:guh_hekje_mint");
        s(c, Soort.HEK, "guhhekje_hemelsblauw", 6, 3, "guhs:guh_hekje_hemelsblauw");
        s(c, Soort.HEK, "mosmuurtje", 8, 4, "minecraft:mossy_cobblestone_wall");
        s(c, Soort.HEK, "knuffelhaag", 10, 5, "minecraft:flowering_azalea");
        s(c, Soort.HEK, "vadshekje", 15, 7, "guhs:roosterijzer_tralies");
        s(c, Soort.STUK, "boogpoortje", 60, 3, "guhs:guh_poortje_wit");
        c = Categorie.BLOEMEN;
        s(c, Soort.STUK, "bloembakje", 15, 1, "guhs:seizoensbloembak");
        s(c, Soort.STUK, "tulpguhsperkje", 60, 2, "minecraft:pink_tulip");
        s(c, Soort.STUK, "zonnebloemrij", 90, 3, "minecraft:sunflower");
        s(c, Soort.STUK, "kaasbloemenperk", 150, 4, "guhs:kaasbloem");
        s(c, Soort.STUK, "rozenboog", 400, 5, "minecraft:rose_bush");
        s(c, Soort.STUK, "guhbloesempje", 700, 6, "guhs:guhbloesem_leaves");
        s(c, Soort.STUK, "wolkenwilg", 2500, 8, "guhs:pluisbolletje");
        c = Categorie.ZITJES;
        s(c, Soort.STUK, "boomstamkrukje", 20, 1, "minecraft:oak_log");
        s(c, Soort.STUK, "guhbankje_hout", 50, 2, "guhs:guh_bankje_hout");
        s(c, Soort.STUK, "guhbankje_roze", 80, 3, "guhs:guh_bankje_roze");
        s(c, Soort.STUK, "picknicktafel", 250, 4, "guhs:picknickeilandje_mand");
        s(c, Soort.STUK, "schommelbank", 900, 6, "guhs:lampion_roze");
        c = Categorie.LICHT;
        s(c, Soort.STUK, "lantaarntje", 40, 2, "minecraft:lantern");
        s(c, Soort.STUK, "guhlantaarnpaal", 120, 4, "guhs:guh_lantaarnpaal");
        s(c, Soort.STUK, "lichtjesslinger", 200, 5, "guhs:vlaggetjes");
        s(c, Soort.STUK, "vadsstraatlantaarn", 600, 7, "guhs:vadslamp");
        s(c, Soort.STUK, "kristalsterlamp", 1500, 9, "guhs:guh_kristal_lamp");
        c = Categorie.WATER;
        s(c, Soort.STUK, "drinkbak", 30, 1, "guhs:guh_wastobbe");
        s(c, Soort.STUK, "pompguh", 150, 3, "minecraft:cauldron");
        s(c, Soort.STUK, "badeendjesvijver", 500, 4, "guhs:guh_waterlelie");
        s(c, Soort.STUK, "spuitguhfontein", 1800, 6, "minecraft:water_bucket");
        s(c, Soort.STUK, "kristalfontein", 6000, 9, "guhs:guh_kristal_cluster");
        c = Categorie.BEELDEN;
        s(c, Soort.STUK, "kaboeter", 70, 2, "minecraft:red_candle");
        s(c, Soort.STUK, "gouden_guh", 4000, 8, "minecraft:gold_block");
        c = Categorie.BOERDERIJ;
        s(c, Soort.STUK, "hooiguh", 10, 1, "guhs:roze_hooibaal");
        s(c, Soort.STUK, "kruiguhwagen", 25, 1, "minecraft:composter");
        s(c, Soort.STUK, "hooibalenstapel", 45, 2, "minecraft:hay_block");
        s(c, Soort.STUK, "vogelverschriknjeg", 60, 2, "minecraft:wheat");
        s(c, Soort.STUK, "melkbussen", 40, 3, "guhs:knabbelspelen_kaasmelkfles");
        s(c, Soort.STUK, "vogelhuisje_paal", 60, 3, "guhs:vogelhuisje");
        s(c, Soort.STUK, "windvaantje", 180, 4, "guhs:guh_molentje");
        s(c, Soort.STUK, "hooiwagen", 450, 5, "minecraft:cherry_trapdoor");
        c = Categorie.BIJZONDER;
        s(c, Soort.STUK, "guhballon", 50, 3, "guhs:mini_luchtballon");
        s(c, Soort.STUK, "reuzengatenkaas", 300, 4, "guhs:gatenkaas");
        s(c, Soort.STUK, "hangmat", 1000, 6, "minecraft:pink_carpet");
    }

    private DecoWinkel() {
    }

    /** Everything in the shop, in catalogue order. */
    public static List<Stuk> alle() {
        return List.copyOf(STUKKEN.values());
    }

    /** The shop entry of a path kind or deco id, or null (a special piece: not for sale). */
    @Nullable
    public static Stuk stuk(String id) {
        return id == null ? null : STUKKEN.get(id);
    }

    public static List<Stuk> van(Categorie c) {
        List<Stuk> l = new ArrayList<>();
        for (Stuk s : STUKKEN.values()) {
            if (s.cat() == c) {
                l.add(s);
            }
        }
        return Collections.unmodifiableList(l);
    }

    /** Is this a path kind? */
    public static boolean isPad(String id) {
        Stuk s = stuk(id);
        return s != null && s.soort() == Soort.PAD;
    }

    /** Is this a 1x1 hekje (drawn per tile, erased with the gum)? */
    public static boolean isHek(String id) {
        Stuk s = stuk(id);
        return s != null && s.soort() == Soort.HEK;
    }

    /** Can one of these be bought now (unlocked, money or a Decobon)? Null = yes. The Decobon only buys ordinary pieces. */
    @Nullable
    public static Base.Fout kanKopen(Base b, String id, boolean bon, int bonnen) {
        Stuk s = stuk(id);
        if (s == null) {
            return new Base.Fout("erfenis.decowinkel.fout.niet_te_koop");
        }
        if (b.hoeveLevel() < s.vanaf()) {
            return new Base.Fout("erfenis.fout.slot", Integer.toString(s.vanaf()));
        }
        if (bon) {
            if (s.soort() != Soort.STUK) {
                return new Base.Fout("erfenis.decowinkel.fout.bon_pad");
            }
            return bonnen > 0 ? null : new Base.Fout("erfenis.decowinkel.fout.geen_bon");
        }
        return b.guhlden >= s.prijs() ? null : new Base.Fout("erfenis.fout.guhlden", Integer.toString(s.prijs()));
    }

    /**
     * Buys one piece into the decoschuurtje (paying Guhlden; with {@code bon} the caller has taken the Decobon already).
     * Null = bought.
     */
    @Nullable
    public static Base.Fout koop(Base b, String id, boolean bon, int bonnen) {
        Base.Fout f = kanKopen(b, id, bon, bonnen);
        if (f != null) {
            return f;
        }
        if (!bon) {
            b.guhlden -= stuk(id).prijs();
        }
        b.decoOpslag.merge(id, 1, Integer::sum);
        return null;
    }
}
