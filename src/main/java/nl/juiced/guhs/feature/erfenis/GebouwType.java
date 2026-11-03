package nl.juiced.guhs.feature.erfenis;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * De Erfenis ("Een opknapper!"): every kind of building (the 33 contract ids), data-driven. The numbers come from
 * erfenis-voorstellen/balans.md with the lead's overrides (CONTRACT_OPKNAPPER.md): every building has 10 levels (Mammoetwei 3),
 * the max level of any building is the current Hoeve level.
 * <ul>
 *   <li>{@link #breedte} x {@link #diepte}: the footprint before rotation (x by z); a quarter turn swaps them ({@link #b}, {@link #d}).</li>
 *   <li>{@link #opent}: the Hoeve level at which it first may be built; {@link #maxAantal(int)}: how many per Hoeve level (balans 2.3).</li>
 *   <li>{@link #klasse}: the cost class (balans 3.1: A = T1, B = T2, C = T3; S = its own table, see {@link Prijzen}).</li>
 *   <li>production, recipes and workplaces: {@link Productie}; costs and build times: {@link Prijzen}.</li>
 * </ul>
 * Lang: {@code erfenis.gebouw.<id>} and {@code erfenis.gebouw.<id>.uitleg} (tools/features/erfenis.py).
 */
public enum GebouwType {
    // ---- kern
    HOEVE("hoeve", 11, 11, Familie.KERN, 1, 10, 'S', "minecraft:bell", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    OPSLAGSCHUUR("opslagschuur", 7, 7, Familie.KERN, 1, 10, 'A', "minecraft:barrel", 1, 1, 1, 2, 2, 2, 3, 3, 3, 3),
    TIMMERSCHUUR("timmerschuur", 6, 6, Familie.KERN, 3, 10, 'S', "minecraft:crafting_table", 0, 0, 1, 1, 1, 1, 1, 1, 1, 1),
    // ---- dieren
    KIPPENHOK("kippenhok", 8, 10, Familie.DIEREN, 1, 10, 'A', "minecraft:egg", 1, 1, 1, 2, 2, 2, 3, 3, 3, 3),
    SCHAAPSKOOI("schaapskooi", 10, 12, Familie.DIEREN, 1, 10, 'A', "minecraft:white_wool", 1, 1, 1, 2, 2, 2, 2, 3, 3, 3),
    KONIJNENREN("konijnenren", 8, 8, Familie.DIEREN, 3, 10, 'B', "minecraft:rabbit_hide", 0, 0, 1, 1, 1, 2, 2, 2, 2, 2),
    EENDENVIJVER("eendenvijver", 10, 10, Familie.DIEREN, 3, 10, 'B', "minecraft:lily_pad", 0, 0, 1, 1, 1, 1, 2, 2, 2, 2),
    KOEIENSTAL("koeienstal", 12, 12, Familie.DIEREN, 3, 10, 'B', "minecraft:milk_bucket", 0, 0, 1, 1, 1, 2, 2, 2, 2, 2),
    BIJENHOF("bijenhof", 8, 8, Familie.DIEREN, 5, 10, 'C', "minecraft:beehive", 0, 0, 0, 0, 1, 1, 1, 2, 2, 2),
    MAMMOETWEI("mammoetwei", 16, 16, Familie.DIEREN, 9, 3, 'S', "minecraft:brown_wool", 0, 0, 0, 0, 0, 0, 0, 0, 1, 1),
    // ---- akkers
    AKKER("akker", 8, 8, Familie.AKKERS, 1, 10, 'A', "minecraft:wheat", 1, 2, 2, 3, 3, 4, 4, 5, 5, 6),
    BOOMGAARD("boomgaard", 10, 10, Familie.AKKERS, 5, 10, 'C', "minecraft:apple", 0, 0, 0, 0, 1, 1, 1, 2, 2, 2),
    // ---- verwerking
    MOLEN("molen", 7, 7, Familie.VERWERKING, 3, 10, 'B', "minecraft:sugar", 0, 0, 1, 1, 1, 1, 1, 1, 1, 1),
    BAKKERIJ("bakkerij", 7, 7, Familie.VERWERKING, 3, 10, 'B', "minecraft:bread", 0, 0, 1, 1, 1, 1, 1, 2, 2, 2),
    WEVERIJ("weverij", 7, 7, Familie.VERWERKING, 3, 10, 'B', "minecraft:loom", 0, 0, 1, 1, 1, 1, 2, 2, 2, 2),
    KAASMAKERIJ("kaasmakerij", 7, 7, Familie.VERWERKING, 5, 10, 'C', "guhs:kaas_knabbels", 0, 0, 0, 0, 1, 1, 1, 1, 1, 1),
    KAARSENMAKERIJ("kaarsenmakerij", 6, 6, Familie.VERWERKING, 7, 10, 'S', "minecraft:candle", 0, 0, 0, 0, 0, 0, 1, 1, 1, 1),
    // ---- bulkblokken
    BOSJE("bosje", 10, 10, Familie.BULK, 1, 10, 'A', "minecraft:oak_log", 1, 1, 1, 1, 2, 2, 2, 2, 2, 2),
    HOUTZAGERIJ("houtzagerij", 7, 7, Familie.BULK, 1, 10, 'A', "minecraft:oak_planks", 1, 1, 1, 1, 1, 2, 2, 2, 2, 2),
    GROEVE("groeve", 10, 10, Familie.BULK, 3, 10, 'B', "minecraft:cobblestone", 0, 0, 1, 1, 1, 2, 2, 2, 2, 2),
    STEENBAKKERIJ("steenbakkerij", 7, 7, Familie.BULK, 3, 10, 'B', "minecraft:bricks", 0, 0, 1, 1, 1, 1, 2, 2, 2, 2),
    ZANDKUIL("zandkuil", 8, 8, Familie.BULK, 5, 10, 'C', "minecraft:sand", 0, 0, 0, 0, 1, 1, 1, 2, 2, 2),
    GLASBLAZERIJ("glasblazerij", 7, 7, Familie.BULK, 5, 10, 'C', "minecraft:glass", 0, 0, 0, 0, 1, 1, 1, 1, 2, 2),
    BOUWBLOKKENWERKPLAATS("bouwblokkenwerkplaats", 9, 9, Familie.BULK, 5, 10, 'C', "guhs:pastelbaksteen_roze", 0, 0, 0, 0, 1, 1, 1, 1, 1, 2),
    // ---- dorp
    GUHHUTJE("guhhutje", 5, 5, Familie.DORP, 1, 10, 'A', "minecraft:red_bed", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
    MARKTPLEIN("marktplein", 9, 9, Familie.DORP, 1, 10, 'S', "minecraft:minecart", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    SPEELTUIN("speeltuin", 8, 8, Familie.DORP, 3, 10, 'B', "minecraft:slime_ball", 0, 0, 1, 1, 1, 1, 1, 1, 1, 1),
    GUHPENSION("guhpension", 8, 8, Familie.DORP, 3, 10, 'B', "minecraft:pink_bed", 0, 0, 1, 1, 1, 1, 1, 1, 1, 1),
    DETECTIVEBUREAU("detectivebureau", 6, 6, Familie.DORP, 3, 10, 'S', "minecraft:spyglass", 0, 0, 1, 1, 1, 1, 1, 1, 1, 1),
    DORPSCAFE("dorpscafe", 8, 8, Familie.DORP, 5, 10, 'S', "minecraft:cake", 0, 0, 0, 0, 1, 1, 1, 1, 1, 1),
    MUZIEKTENT("muziektent", 9, 9, Familie.DORP, 5, 10, 'S', "minecraft:note_block", 0, 0, 0, 0, 1, 1, 1, 1, 1, 1),
    // ---- koppeling met de hoofdwereld
    LOKET("loket", 5, 5, Familie.KOPPELING, 6, 10, 'S', "minecraft:chest", 0, 0, 0, 0, 0, 1, 1, 1, 1, 1),
    VADSMAST("vadsmast", 4, 4, Familie.KOPPELING, 7, 10, 'S', "minecraft:lightning_rod", 0, 0, 0, 0, 0, 0, 1, 1, 1, 1);

    /** The families of the shop bar (lang {@code erfenis.familie.<id>}). */
    public enum Familie {
        KERN, DIEREN, AKKERS, VERWERKING, BULK, DORP, KOPPELING;

        public String naamKey() {
            return "erfenis.familie." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final int MAX_HOEVE = 10;

    public final String id;
    public final int breedte, diepte;
    /** (POC compatibility for the world map) the larger side. */
    public final int maat;
    public final Familie familie;
    public final int opent;
    private final int top;
    public final char klasse;
    private final String icoonId;
    private final int[] aantal;

    GebouwType(String id, int breedte, int diepte, Familie familie, int opent, int top, char klasse, String icoonId, int... aantal) {
        this.id = id;
        this.breedte = breedte;
        this.diepte = diepte;
        this.maat = Math.max(breedte, diepte);
        this.familie = familie;
        this.opent = opent;
        this.top = top;
        this.klasse = klasse;
        this.icoonId = icoonId;
        this.aantal = aantal;
    }

    /** Footprint along x for this rotation (a quarter turn swaps the sides). */
    public int b(int rot) {
        return (rot & 1) == 0 ? breedte : diepte;
    }

    /** Footprint along z for this rotation. */
    public int d(int rot) {
        return (rot & 1) == 0 ? diepte : breedte;
    }

    /** The highest level this type has at all (10; Mammoetwei 3). */
    public int topLevel() {
        return top;
    }

    /** How many of these a base may have at this Hoeve level. */
    public int maxAantal(int hoeve) {
        return aantal[clamp(hoeve) - 1];
    }

    /** The highest level these may have at this Hoeve level (= the Hoeve level; Mammoetwei: H9 2, H10 3). */
    public int maxLevel(int hoeve) {
        if (this == HOEVE) {
            return top;
        }
        if (this == MAMMOETWEI) {
            return hoeve >= 10 ? 3 : hoeve >= 9 ? 2 : 0;
        }
        return Math.min(top, clamp(hoeve));
    }

    /** Knusheid this building gives at this level (deco.md: 2 per level, the Hoeve 10 per level). */
    public int knus(int level) {
        if (level <= 0) {
            return 0;
        }
        return this == HOEVE ? 10 * level : 2 * level;
    }

    /** The first Hoeve level at which one more of these may be placed beyond {@code heeft}, or -1 (never). */
    public int ontgrendeltAantal(int heeft) {
        for (int h = 1; h <= MAX_HOEVE; h++) {
            if (aantal[h - 1] > heeft) {
                return h;
            }
        }
        return -1;
    }

    /** The first Hoeve level at which these may reach {@code level}, or -1. */
    public int ontgrendeltLevel(int level) {
        for (int h = 1; h <= MAX_HOEVE; h++) {
            if (maxLevel(h) >= level) {
                return h;
            }
        }
        return -1;
    }

    /** A rough height of the building at this level (ghosts and mouse picking in the overview; the real shape comes from GebouwVormen). */
    public int hoogte(int level) {
        if (level <= 0) {
            return 1;
        }
        int basis = switch (familie) {
            case AKKERS -> 2;
            case DIEREN -> this == EENDENVIJVER ? 3 : 4;
            default -> 5;
        };
        if (this == HOEVE) {
            basis = 7;
        } else if (this == VADSMAST) {
            basis = 8;
        }
        return basis + level / 2;
    }

    /** Is this a building that makes goods (it has a buffer to collect)? */
    public boolean maakt() {
        return !Productie.recepten(this).isEmpty();
    }

    /** Area (tiles) for the size classes of balans 3.1: small &lt;= 6x6, large &gt;= 10x10. */
    public int oppervlak() {
        return breedte * diepte;
    }

    public ItemStack icoon() {
        Item i = Goed.item(icoonId);
        return new ItemStack(i == null ? Items.PAPER : i);
    }

    public String naamKey() {
        return "erfenis.gebouw." + id;
    }

    public String uitlegKey() {
        return "erfenis.gebouw." + id + ".uitleg";
    }

    @Nullable
    public static GebouwType van(String id) {
        for (GebouwType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        // (POC names: a base saved by the 1.9 proof of concept loads with its Hoeve and storage shed)
        return switch (id) {
            case "stadhuis" -> HOEVE;
            case "schuurtje" -> OPSLAGSCHUUR;
            default -> null;
        };
    }

    private static int clamp(int hoeve) {
        return Math.max(1, Math.min(MAX_HOEVE, hoeve));
    }
}
