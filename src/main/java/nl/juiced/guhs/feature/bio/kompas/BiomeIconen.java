package nl.juiced.guhs.feature.bio.kompas;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * biomes3: the little icon of each biome in the Superkompas tab Biomes: item ids, the first one that exists wins (so an
 * icon may name an item of a slice or an update that is not merged yet, with an older item behind it). A biome without an
 * icon of its own gets its section's. Another update gives its biome an icon with one line in its Feature.register:
 * {@code BiomeIconen.zet("korstwoestijn", "guhs:kaaskorst")}.
 */
public final class BiomeIconen {
    private static final Map<String, List<String>> ICONEN = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> SECTIES = new ConcurrentHashMap<>();

    static {
        zet("guh_fields", "guhs:guhbloemetje");
        zet("knabbel_crumbs", "guhs:kaas_knabbels");
        zet("pink_puffs", "guhs:roze_gras");
        zet("mikas_biome", "guhs:mika_steen");
        zet("kaas_flats", "guhs:kaasbrok");
        zet("guh_meadows", "guhs:roze_guhbloem");
        zet("guh_peaks", "guhs:knuffelsteen");
        zet("vads_cliffs", "guhs:vahoege_vads");
        zet("guh_sea", "guhs:kaasvis");
        zet("guh_kristalmijn", "guhs:guh_kristal_cluster");
        zet("gatenkaasgrotten", "guhs:gatenkaas");
        zet("kaasmoeras", "guhs:moeraskaas");
        zet("vadswoud", "guhs:vadshout_zaailing");
        zet("diepe_guhzee", "guhs:reuzenschelp");
        zet("knuffeldal", "guhs:knuffelgras");
        zet("guhpolder", "guhs:guh_molentje");
        zet("sneeuwguhtoendra", "guhs:sneeuwguhspar_zaailing");
        zet("guhwaii", "guhs:kokosnoot");
        zet("bleekwoud", "guhs:bleekhout_zaailing");
        // the three new ones: things of the other biomes3 slices, an older item until those are merged
        zet("bloesemmeertje", "guhs:drijvende_bloesemblaadjes", "guhs:guhbloesem_sapling");
        zet("klaterdal", "guhs:toro", "guhs:esdoorn_zaailing", "minecraft:bamboo");
        zet("wolkenweide", "guhs:wolkenblok_wit", "guhs:wolkensuikerspin");
        zet("houtskoolvlakte", "guhs:houtskoolsteen");
        zet("asdal", "guhs:as_blok");
        zet("satebos", "guhs:sate_zwammetje");
        zet("worstenwoud", "guhs:worst_zwammetje");
        zet("rookdelta", "guhs:rookgat");
        zet("guheinde", "guhs:kaaskorst");
        sectie("guhmension", "guhs:knuffelgras");
        sectie("barbecuether", "guhs:houtskoolsteen");
        sectie("guheinde", "guhs:kaaskorst");
    }

    /** The icon of a biome (guhs biome id without namespace): item ids with namespace, the first that exists is used. */
    public static void zet(String biome, String... items) {
        ICONEN.put(biome, List.of(items));
    }

    /** The icon of the biomes of a section that have none of their own. */
    public static void sectie(String sectie, String... items) {
        SECTIES.put(sectie, List.of(items));
    }

    /** Has this biome an icon of its own (one whose item exists)? */
    public static boolean heeft(String biome) {
        return eerste(ICONEN.get(biome)) != null;
    }

    /** The icon of a biome: its own, else its section's, else a map. Never empty. */
    public static ItemStack icoon(String biome) {
        Item item = eerste(ICONEN.get(biome));
        if (item == null) {
            var sectie = BiomesMenu.sectieVan(biome);
            item = sectie == null ? null : eerste(SECTIES.get(sectie.id()));
        }
        return new ItemStack(item == null ? Items.MAP : item);
    }

    @Nullable
    private static Item eerste(@Nullable List<String> ids) {
        if (ids != null) {
            for (String id : ids) {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
                if (item != Items.AIR) {
                    return item;
                }
            }
        }
        return null;
    }

    private BiomeIconen() {
    }
}
