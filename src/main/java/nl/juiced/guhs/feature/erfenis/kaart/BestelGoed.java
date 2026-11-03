package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * De Erfenis, bestellingen: the goods a neighbour village can ask for, with their value (balans.md section 5) and the tier
 * from which they show up in orders (balans.md section 4: T1 raw, T2 + meel/brood/stof/melk, T3 + kaas/honing/glas/koek,
 * T4 + kaarsen/knuffelstof/Bouwkeet-blokken, T5 + lantaarns/reuzenwol). Ids are the contract's {@code Goed} ids.
 * Never in orders: kaasknabbel (only for home), glasscherf, pluis, balk, gladsteen; never main-world items.
 * The icon is only for the order screen (a vanilla or guhs item that looks like it).
 */
public enum BestelGoed {
    EI("ei", 1, 1, "minecraft:egg"),
    VEER("veer", 1, 1, "minecraft:feather"),
    WOL("wol", 2, 1, "minecraft:white_wool"),
    GRAAN("graan", 1, 1, "minecraft:wheat"),
    WORTEL("wortel", 1, 1, "minecraft:carrot"),
    BLOEM("bloem", 1, 1, "minecraft:poppy"),
    STAM("stam", 1, 1, "minecraft:oak_log"),
    PLANK("plank", 0.3, 1, "minecraft:oak_planks"),
    MEEL("meel", 3, 2, "minecraft:sugar"),
    BROOD("brood", 8, 2, "minecraft:bread"),
    STOF("stof", 6, 2, "minecraft:white_carpet"),
    MELK("melk", 2, 2, "minecraft:milk_bucket"),
    ANGORA("angora", 3, 2, "minecraft:pink_wool"),
    EENDENEI("eendenei", 2, 2, "minecraft:blue_egg"),
    DONS("dons", 4, 2, "minecraft:string"),
    MEST("mest", 1, 2, "minecraft:bone_meal"),
    ROZEGRAAN("rozegraan", 1, 2, "minecraft:pink_petals"),
    STEEN("steen", 0.5, 2, "minecraft:cobblestone"),
    BAKSTEEN("baksteen", 2, 2, "minecraft:brick"),
    KLEI("klei", 1, 2, "minecraft:clay_ball"),
    KAAS("kaas", 9, 3, "guhs:kaasbrok"),
    HONING("honing", 4, 3, "minecraft:honey_bottle"),
    WAS("was", 3, 3, "minecraft:honeycomb"),
    GLAS("glas", 2, 3, "minecraft:glass"),
    KOEK("koek", 14, 3, "minecraft:cookie"),
    WORTELCAKE("wortelcake", 16, 3, "minecraft:cake"),
    KNABBELBES("knabbelbes", 2, 3, "minecraft:sweet_berries"),
    FRUIT("fruit", 2, 3, "minecraft:apple"),
    PLUISBOLLETJE("pluisbolletje", 2, 3, "minecraft:dandelion"),
    ZAND("zand", 0.5, 3, "minecraft:sand"),
    KAARS("kaars", 8, 4, "minecraft:candle"),
    KNUFFELSTOF("knuffelstof", 9, 4, "minecraft:pink_carpet"),
    BOUWBLOK("bouwblok", 5, 4, "guhs:bouwkeet"),
    KNABBELKOEK("knabbelkoek", 14, 4, "minecraft:pumpkin_pie"),
    ROZEMEEL("rozemeel", 3, 4, "minecraft:pink_dye"),
    LANTAARN("lantaarn", 15, 5, "minecraft:lantern"),
    REUZENWOL("reuzenwol", 30, 5, "minecraft:brown_wool");

    public final String id;
    public final double waarde;
    public final int tier;
    private final String icoon;

    BestelGoed(String id, double waarde, int tier, String icoon) {
        this.id = id;
        this.waarde = waarde;
        this.tier = tier;
        this.icoon = icoon;
    }

    /** Lang key of the Dutch plural ("12 eieren"); the kaart slice's own key, so the order texts read well. */
    public String naamKey() {
        return "erfenis.kaart.goed." + id;
    }

    public ItemStack icoon() {
        Item i = BuiltInRegistries.ITEM.getOptional(Identifier.parse(icoon)).orElse(Items.BUNDLE);
        return new ItemStack(i == Items.AIR ? Items.BUNDLE : i);
    }

    /** Every good of exactly this tier. */
    public static List<BestelGoed> vanTier(int tier) {
        List<BestelGoed> l = new ArrayList<>();
        for (BestelGoed g : values()) {
            if (g.tier == tier) {
                l.add(g);
            }
        }
        return l;
    }

    /** Every good up to (and including) this tier. */
    public static List<BestelGoed> totTier(int tier) {
        List<BestelGoed> l = new ArrayList<>();
        for (BestelGoed g : values()) {
            if (g.tier <= tier) {
                l.add(g);
            }
        }
        return l;
    }

    @Nullable
    public static BestelGoed van(String id) {
        for (BestelGoed g : values()) {
            if (g.id.equals(id)) {
                return g;
            }
        }
        return null;
    }
}
