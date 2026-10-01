package nl.juiced.guhs.feature.kleding;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import nl.juiced.guhs.entity.GuhClothes;

/**
 * Where every clothing piece comes from (2.9): exactly one source per piece. A source id is a group of the Guhdex's
 * Minigames tab (SpelGroepen: beauty, sjoelen, ...) or one of the other sources of CONTRACT_29 §5.5 (kleermaker,
 * loot_picknick, guhdex, beroep_brandweer, ...). Static and the same on both sides: register from your Feature.register.
 * The Kleding tab shows the source (lang gui.guhs.kledingbron.&lt;id&gt;) and, if given, the price.
 */
public final class KledingBronnen {
    /** The known sources in their fixed order (CONTRACT_29 §5.4 + §5.5); sources registered later come after them. */
    public static final List<String> BEKEND = List.of(
            // the Minigames tab groups (SpelGroepen)
            "beauty", "race", "meppen", "disco", "golf", "smul", "vissen", "verstop", "kermis",
            "bakkerij", "creche", "kapper", "knuffelbad", "grijpmachine",
            "sjoelen", "doolhof", "katapult", "knabbelspelen", "elftocht", "circuit",
            // the other sources
            "kleermaker", "loot_picknick", "loot_hamsterhuis", "loot_guhramid", "loot_mikahuis", "loot_grotten", "loot_kasteel",
            "loot_eilanden", "guhdex", "brococolief", "crafting", "vadsparade", "kaasmijn", "bibliotheek", "onderwater", "vadswoud",
            "barbecuether", "knuffeldal", "theehuis", "boerderij", "tuintjes", "sterrenwacht", "ballon", "kamperen", "wereldleven",
            "beroep_brandweer", "beroep_politie", "beroep_apotheek", "beroep_bouw",
            // 3.0 (Guhverhalen)
            "timmerguh", "nomguh", "sledesprint", "mewtwo", "hemel", "guhwaii", "guhwaii_spellen");

    private static final Map<GuhClothes, String> BRON = new EnumMap<>(GuhClothes.class);
    private static final Map<GuhClothes, net.minecraft.network.chat.Component> PRIJS = new EnumMap<>(GuhClothes.class);
    private static final List<String> BRONNEN = new ArrayList<>(BEKEND);

    /** Registers the source of a piece (the same piece again: the last one counts; there should be only one). */
    public static synchronized void bron(GuhClothes c, String bronId) {
        BRON.put(c, bronId);
        if (!BRONNEN.contains(bronId)) {
            BRONNEN.add(bronId);
        }
    }

    /** Registers the source of a piece with its price, e.g. "4 sjoelschijfjes" (shown in the Kleding tab; 1.2.0: a
     *  translatable, see {@link #prijs(String, Object...)}). */
    public static synchronized void bron(GuhClothes c, String bronId, net.minecraft.network.chat.Component prijs) {
        bron(c, bronId);
        PRIJS.put(c, prijs);
    }

    /** The source id of a piece (null: not registered). */
    @Nullable
    public static synchronized String bron(GuhClothes c) {
        return BRON.get(c);
    }

    /** The price text of a piece (null: none given). */
    @Nullable
    public static synchronized net.minecraft.network.chat.Component prijs(GuhClothes c) {
        return PRIJS.get(c);
    }

    /** 1.2.0: a price or hint text: lang gui.guhs.kleding.prijs.munt.&lt;coin&gt; ("%s sjoelschijfjes", the amount as argument)
     *  or gui.guhs.kleding.prijs.hint.&lt;hint&gt; (the Dutch is in tools/features/taal.py). */
    public static net.minecraft.network.chat.Component prijs(String key, Object... args) {
        return net.minecraft.network.chat.Component.translatable(key, args);
    }

    /** All source ids, in registration order (the §5.4/§5.5 ones first); lang gui.guhs.kledingbron.&lt;id&gt;. */
    public static synchronized List<String> bronnen() {
        return List.copyOf(BRONNEN);
    }

    /** The pieces of one source, in enum order. */
    public static synchronized List<GuhClothes> van(String bronId) {
        List<GuhClothes> out = new ArrayList<>();
        for (GuhClothes c : GuhClothes.values()) {
            if (bronId.equals(BRON.get(c))) {
                out.add(c);
            }
        }
        return out;
    }

    /** How many pieces can be unlocked at all (everything but the hair). */
    public static int aantalOntgrendelbaar() {
        return (int) java.util.Arrays.stream(GuhClothes.values()).filter(c -> c.slot != GuhClothes.Slot.HAAR).count();
    }

    private KledingBronnen() {
    }
}
