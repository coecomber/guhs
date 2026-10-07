package nl.juiced.guhs.feature.einde2;

import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import nl.juiced.guhs.Guhs;

/**
 * einde2 (the Guheinde expansion): every id the slices share, in one place: the biomes of the outer islands, their tags,
 * the big places (the four story structures, the eight side places, the two places of the Sponsguh story) and the height
 * bands of the dimension. Complete from
 * the skeleton on: a slice reads it, nobody adds to it. The same numbers and names live in tools/features/einde2_kit.py
 * (the tags and structures themselves come from tools/features/einde2.py and the slices' modules).
 */
public final class Einde2Ids {
    // --- biomes -----------------------------------------------------------------------------------------------------------
    /** The main island and the void ring around it (and every chunk made before einde2). */
    public static final ResourceKey<Biome> GUHEINDE = biome("guheinde");
    public static final ResourceKey<Biome> GATENKAASHEUVELS = biome("gatenkaasheuvels");
    public static final ResourceKey<Biome> KORSTWOESTIJN = biome("korstwoestijn");
    public static final ResourceKey<Biome> KRUIMELWOLKEN = biome("kruimelwolken");
    public static final ResourceKey<Biome> SCHIMMELKAASVLAKTE = biome("schimmelkaasvlakte");
    public static final ResourceKey<Biome> KRISTALKAASKLIPPEN = biome("kristalkaasklippen");
    public static final ResourceKey<Biome> KNABBELKOORBOS = biome("knabbelkoorbos");
    /** The six biomes of the outer islands: the two common ones first, then the four rare ones. */
    public static final List<ResourceKey<Biome>> BUITEN = List.of(GATENKAASHEUVELS, KORSTWOESTIJN, KRUIMELWOLKEN, SCHIMMELKAASVLAKTE,
            KRISTALKAASKLIPPEN, KNABBELKOORBOS);

    /** The four elements of the Guhvatar story, each with the rare biome its animal lives in. */
    public enum Element {
        LUCHT(KRUIMELWOLKEN), WATER(SCHIMMELKAASVLAKTE), AARDE(KRISTALKAASKLIPPEN), VUUR(KNABBELKOORBOS);

        private final ResourceKey<Biome> biome;

        Element(ResourceKey<Biome> biome) {
            this.biome = biome;
        }

        public ResourceKey<Biome> biome() {
            return biome;
        }
    }

    // --- tags -------------------------------------------------------------------------------------------------------------
    public static final TagKey<Biome> BUITENEILANDEN = biomes("buiteneilanden");
    public static final TagKey<Biome> GEWOON = biomes("gewoon");
    public static final TagKey<Biome> ZELDZAAM = biomes("zeldzaam");
    public static final TagKey<Biome> LUCHT = biomes("lucht");
    public static final TagKey<Biome> WATER = biomes("water");
    public static final TagKey<Biome> AARDE = biomes("aarde");
    public static final TagKey<Biome> VUUR = biomes("vuur");
    /** What the creatures of the outer islands spawn on and the features grow on (kaaskorst + the six surface blocks). */
    public static final TagKey<Block> EILANDGROND = TagKey.create(Registries.BLOCK, Guhs.id("einde2/eilandgrond"));
    public static final TagKey<Block> WOLK = TagKey.create(Registries.BLOCK, Guhs.id("einde2/wolk"));
    /** The four story structures / the eight side places (structure tags). */
    public static final TagKey<Structure> VERHAAL = TagKey.create(Registries.STRUCTURE, Guhs.id("einde2/verhaal"));
    public static final TagKey<Structure> ZIJ = TagKey.create(Registries.STRUCTURE, Guhs.id("einde2/zij"));
    /** The two places of the Sponsguh story (structure tag). */
    public static final TagKey<Structure> SPONSGUH = TagKey.create(Registries.STRUCTURE, Guhs.id("einde2/sponsguh"));

    // --- structures -------------------------------------------------------------------------------------------------------
    public static final String ZUIDELIJKE_LUCHTTEMPEL = "zuidelijke_luchttempel", NOORDELIJKE_WATERSTAM = "noordelijke_waterstam",
            BA_SING_GUH = "ba_sing_guh", VUURNATIE_HOOFDSTAD = "vuurnatie_hoofdstad";
    public static final String OMASHGUH = "omashguh", WESTELIJKE_LUCHTTEMPEL = "westelijke_luchttempel", VUURTEMPEL_ROKU = "vuurtempel_roku",
            KYOSHI_DORP = "kyoshi_dorp", BIBLIOTHEEK_WAN_SHI_GUH = "bibliotheek_wan_shi_guh", LEEUWSCHILDPAD_EILAND = "leeuwschildpad_eiland",
            LEEGTEHAVEN = "leegtehaven", GUHVLEUGEL_RINGENRACE = "guhvleugel_ringenrace";
    public static final String MIKA_VESTING = "mika_vesting";
    /** The Sponsguh story: the town deep under the islands and the place its road leads to (the deep band). */
    public static final String BIKINIBODEM = "bikinibodem", SCHELPENSTAD = "schelpenstad";
    /** The four story structures, in chapter order. */
    public static final List<String> VERHAAL_STRUCTUREN = List.of(ZUIDELIJKE_LUCHTTEMPEL, NOORDELIJKE_WATERSTAM, BA_SING_GUH, VUURNATIE_HOOFDSTAD);
    /** The eight side places (the Superkompas tab "einde" lists them). */
    public static final List<String> ZIJ_STRUCTUREN = List.of(OMASHGUH, WESTELIJKE_LUCHTTEMPEL, VUURTEMPEL_ROKU, KYOSHI_DORP, BIBLIOTHEEK_WAN_SHI_GUH,
            LEEUWSCHILDPAD_EILAND, LEEGTEHAVEN, GUHVLEUGEL_RINGENRACE);
    /** The two places of the Sponsguh story, in the order of the story (Schelpenstad stands next to the one Bikinibodem). */
    public static final List<String> SPONS_STRUCTUREN = List.of(BIKINIBODEM, SCHELPENSTAD);
    /** Every guhs:leegte_bouw of einde2: the story structures, the side places, the Sponsguh pair (fourteen). */
    public static final List<String> LEEGTE_STRUCTUREN = java.util.stream.Stream.of(VERHAAL_STRUCTUREN, ZIJ_STRUCTUREN, SPONS_STRUCTUREN)
            .flatMap(List::stream).toList();
    /** Side place -> the element its resident asks for (the others ask for none); see {@link Einde2Haken#ELEMENT_GELEERD}. */
    public static final Map<String, Element> ELEMENT_VAN = Map.of(OMASHGUH, Element.AARDE, WESTELIJKE_LUCHTTEMPEL, Element.LUCHT,
            VUURTEMPEL_ROKU, Element.VUUR, KYOSHI_DORP, Element.WATER);

    // --- the height bands of the Guheinde (y 0-255) ------------------------------------------------------------------------
    /** The high band: only guhs:leegte_bouw structures (the big places, each with its own floating island). */
    public static final int HOOG_MIN = 134, HOOG_MAX = 253;
    /**
     * The deep band: inside the natural band, but only in a spot where the Guheinde has no terrain at all (the two places of
     * the Sponsguh story; see LeegteBouwStructure's {@code leeg} and {@code ruim}).
     */
    public static final int LAAG_MIN = 4, LAAG_MAX = 117;
    /** The top of the terrain and of every biome feature. */
    public static final int NATUUR_MAX = 119;
    /** The top of the buildings that stand on an island (Mika-vesting, terugpoort, decoration). */
    public static final int EILANDBOUW_MAX = 127;
    /** Within this many blocks of 0,0 the biome stays guhs:guheinde (the main island and the void ring). */
    public static final int STRAAL_MIDDEN = 960;

    public static ResourceKey<Structure> structuur(String id) {
        return ResourceKey.create(Registries.STRUCTURE, Guhs.id(id));
    }

    private static ResourceKey<Biome> biome(String id) {
        return ResourceKey.create(Registries.BIOME, Guhs.id(id));
    }

    private static TagKey<Biome> biomes(String id) {
        return TagKey.create(Registries.BIOME, Guhs.id("einde2/" + id));
    }

    private Einde2Ids() {
    }
}
