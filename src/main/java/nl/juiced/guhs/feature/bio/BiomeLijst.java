package nl.juiced.guhs.feature.bio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The data of the Superkompas tab "Biomes": its sections (one per dimension, in menu order) and the biomes of each. Pure
 * data, the same on both sides; the kompas slice builds the tab, the search and the lock on top of it, and
 * {@link Bezocht} follows exactly these dimensions and biomes.
 * <p>
 * Another update adds a biome with ONE line in its own Feature.register (common code, both sides):
 * {@code BiomeLijst.biome("guheinde", "korstwoestijn")}, and a dimension with {@link #sectie(Sectie)}. Texts: the section
 * name gui.guhs.superkompas.sectie.&lt;id&gt;, its padlock hover gui.guhs.superkompas.sectie.&lt;id&gt;.slot, a biome's name
 * biome.guhs.&lt;id&gt; (exists for every biome).
 */
public final class BiomeLijst {
    /**
     * A section: its id, the dimension it lists (null: a teaser that never opens and lists nothing), and an advancement
     * that proves an older visit (a player who was there before this update; null: none).
     */
    public record Sectie(String id, @Nullable ResourceKey<Level> dimensie, @Nullable Identifier bewijs) {
        public boolean teaser() {
            return dimensie == null;
        }
    }

    private static final Map<String, Sectie> SECTIES = new LinkedHashMap<>();
    private static final Map<String, List<String>> BIOMES = new LinkedHashMap<>();

    static {
        sectie(new Sectie("guhmension", ModDimensions.GUHMENSION, Guhs.id("guhmension/enter_guhmension")));
        sectie(new Sectie("barbecuether", BarbecuetherFeature.BARBECUETHER, Guhs.id("barbecuether/binnen")));
        sectie(new Sectie("guheinde", GuheindeFeature.GUHEINDE, Guhs.id("guheinde/guheinde_binnen")));
        sectie(new Sectie("echte_guheinde", null, null));
        // every biome of data/guhs/dimension/<id>.json, in that order
        for (String b : List.of("guh_fields", "knabbel_crumbs", "pink_puffs", "mikas_biome", "kaas_flats", "guh_meadows", "guh_peaks", "vads_cliffs",
                "guh_sea", "guh_kristalmijn", "gatenkaasgrotten", "kaasmoeras", "vadswoud", "diepe_guhzee", "knuffeldal", "guhpolder", "sneeuwguhtoendra",
                "guhwaii", "bleekwoud")) {
            biome("guhmension", b);
        }
        // biomes3: the three new ones
        biome("guhmension", "bloesemmeertje");
        biome("guhmension", "klaterdal");
        biome("guhmension", "wolkenweide");
        for (String b : List.of("houtskoolvlakte", "asdal", "satebos", "worstenwoud", "rookdelta")) {
            biome("barbecuether", b);
        }
        biome("guheinde", "guheinde");
    }

    /** Adds a section after the ones there (a duplicate id: nothing happens). */
    public static synchronized void sectie(Sectie sectie) {
        if (SECTIES.putIfAbsent(sectie.id(), sectie) == null) {
            BIOMES.put(sectie.id(), new ArrayList<>());
        }
    }

    /** Adds a guhs biome (id without namespace) to a section, after the ones there; a duplicate does nothing. */
    public static synchronized void biome(String sectie, String biome) {
        Sectie s = SECTIES.get(sectie);
        if (s == null || s.teaser()) {
            throw new IllegalArgumentException("The Superkompas tab Biomes has no section " + sectie + " that lists biomes");
        }
        List<String> lijst = BIOMES.get(sectie);
        if (!lijst.contains(biome)) {
            lijst.add(biome);
        }
    }

    /** The sections in menu order. */
    public static synchronized List<Sectie> secties() {
        return List.copyOf(SECTIES.values());
    }

    /** The section with this id, or null. */
    @Nullable
    public static synchronized Sectie sectie(String id) {
        return SECTIES.get(id);
    }

    /** The biomes of a section (ids without namespace), in order; empty for a teaser or an unknown id. */
    public static synchronized List<String> biomes(String sectie) {
        return List.copyOf(BIOMES.getOrDefault(sectie, List.of()));
    }

    /** The section that lists this dimension, or null. */
    @Nullable
    public static synchronized Sectie van(ResourceKey<Level> dimensie) {
        for (Sectie s : SECTIES.values()) {
            if (dimensie.equals(s.dimensie())) {
                return s;
            }
        }
        return null;
    }

    /** Is this biome (id without namespace) listed in this section? */
    public static synchronized boolean heeft(String sectie, String biome) {
        return BIOMES.getOrDefault(sectie, List.of()).contains(biome);
    }

    public static ResourceKey<Biome> sleutel(String biome) {
        return ResourceKey.create(Registries.BIOME, Guhs.id(biome));
    }

    private BiomeLijst() {
    }
}
