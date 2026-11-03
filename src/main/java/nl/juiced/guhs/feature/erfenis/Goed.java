package nl.juiced.guhs.feature.erfenis;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * De Erfenis: the goods of the village storage (contract ids; Dutch names in lang {@code guhs.erfenis.goed.<id>}).
 * <ul>
 *   <li>{@link #waarde}: the reckoning value of balans.md section 5 (an order pays ~3x this in Guhlden).</li>
 *   <li>{@link #icoon}: the item drawn for it in the overview (an item id, resolved at runtime; a vanilla fallback).</li>
 *   <li>{@link #thuisItem}: what it becomes when it is sent home with the Leverkist (null: it stays in the village). Round 17:
 *   basic blocks only go home as raw material (logs, clay, sand, glass shards); wool and Bouwkeet blocks finished.</li>
 *   <li>{@link #capPerUur}: the Leverkist safety net per hour (balans 9.3/9.4; saves up to 8 hours); 0 = no cap. Wool is the
 *   accepted exception.</li>
 * </ul>
 */
public enum Goed {
    EI("ei", 1, "minecraft:egg", "minecraft:egg", 0),
    VEER("veer", 1, "minecraft:feather", "minecraft:feather", 0),
    WOL("wol", 2, "minecraft:white_wool", "minecraft:white_wool", 0),
    PLUIS("pluis", 2, "minecraft:white_dye", null, 0),
    ANGORA("angora", 3, "minecraft:rabbit_hide", null, 0),
    MEST("mest", 1, "minecraft:bone_meal", "minecraft:bone_meal", 0),
    DONS("dons", 4, "minecraft:white_carpet", null, 0),
    EENDENEI("eendenei", 2, "minecraft:blue_egg", null, 0),
    MELK("melk", 2, "minecraft:milk_bucket", "minecraft:milk_bucket", 0),
    HONING("honing", 4, "minecraft:honey_bottle", "minecraft:honey_bottle", 0),
    WAS("was", 3, "minecraft:honeycomb", "minecraft:honeycomb", 0),
    REUZENWOL("reuzenwol", 30, "minecraft:brown_wool", null, 0),
    GRAAN("graan", 1, "minecraft:wheat", "minecraft:wheat", 0),
    ROZEGRAAN("rozegraan", 1, "minecraft:pink_petals", null, 0),
    WORTEL("wortel", 1, "minecraft:carrot", "minecraft:carrot", 0),
    BLOEM("bloem", 1, "minecraft:pink_tulip", "minecraft:pink_tulip", 0),
    PLUISBOLLETJE("pluisbolletje", 2, "guhs:pluisbolletje", "guhs:pluisbolletje", 0),
    KNABBELBES("knabbelbes", 2, "guhs:knabbelbessen", "guhs:knabbelbessen", 0),
    FRUIT("fruit", 2, "minecraft:apple", "minecraft:apple", 0),
    STAM("stam", 1, "minecraft:oak_log", "minecraft:oak_log", 150),
    STEEN("steen", 0.5, "minecraft:cobblestone", null, 0),
    KLEI("klei", 1, "minecraft:clay_ball", "minecraft:clay_ball", 900),
    ZAND("zand", 0.5, "minecraft:sand", "minecraft:sand", 900),
    PLANK("plank", 0.3, "minecraft:oak_planks", null, 0),
    BALK("balk", 1, "minecraft:stripped_oak_log", null, 0),
    BAKSTEEN("baksteen", 2, "minecraft:bricks", null, 0),
    GLADSTEEN("gladsteen", 1, "minecraft:smooth_stone", null, 0),
    GLAS("glas", 2, "minecraft:glass", null, 0),
    GLASSCHERF("glasscherf", 1, "guhs:glasscherf", "guhs:glasscherf", 180),
    MEEL("meel", 3, "minecraft:sugar", null, 0),
    ROZEMEEL("rozemeel", 3, "minecraft:pink_dye", null, 0),
    BROOD("brood", 8, "minecraft:bread", "minecraft:bread", 0),
    KOEK("koek", 14, "minecraft:cookie", "minecraft:cookie", 0),
    WORTELCAKE("wortelcake", 16, "minecraft:pumpkin_pie", null, 0),
    KNABBELKOEK("knabbelkoek", 14, "guhs:knabbelbessentaartje", null, 0),
    STOF("stof", 6, "minecraft:white_carpet", null, 0),
    KNUFFELSTOF("knuffelstof", 9, "minecraft:pink_wool", null, 0),
    KAAS("kaas", 9, "guhs:gatenkaas", "guhs:gatenkaas", 0),
    KAASKNABBEL("kaasknabbel", 2, "guhs:kaas_knabbels", "guhs:kaas_knabbels", 80),
    KAARS("kaars", 8, "minecraft:candle", "minecraft:candle", 0),
    LANTAARN("lantaarn", 15, "minecraft:lantern", "minecraft:lantern", 0),
    BOUWBLOK("bouwblok", 5, "guhs:pastelbaksteen_roze", "*", 500);

    /** The id in saved data, lang keys and the ErfenisEconomie API. */
    public final String id;
    public final double waarde;
    private final String icoonId;
    @Nullable
    public final String thuisItem;
    /** Leverkist cap per hour (0 = none). */
    public final int capPerUur;

    Goed(String id, double waarde, String icoonId, @Nullable String thuisItem, int capPerUur) {
        this.id = id;
        this.waarde = waarde;
        this.icoonId = icoonId;
        this.thuisItem = thuisItem;
        this.capPerUur = capPerUur;
    }

    /** May this go home with the Leverkist ("Naar huis sturen")? */
    public boolean naarHuis() {
        return thuisItem != null;
    }

    public String naamKey() {
        return "guhs.erfenis.goed." + id;
    }

    public ItemStack icoon() {
        Item i = item(icoonId);
        return new ItemStack(i == null ? Items.PAPER : i);
    }

    /** An item by id, or null when it doesn't exist (a slice that isn't merged yet, a typo). */
    @Nullable
    public static Item item(String id) {
        Identifier r = Identifier.tryParse(id);
        if (r == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(r).orElse(null);
    }

    @Nullable
    public static Goed van(String id) {
        for (Goed g : values()) {
            if (g.id.equals(id)) {
                return g;
            }
        }
        return null;
    }
}
