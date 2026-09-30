package nl.juiced.guhs.feature.knus;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import nl.juiced.guhs.Guhs;

/**
 * The shared item tags of 2.8 ({@code guhs:knus/<name>}): the owner of a product adds its item to the tag (with
 * {@code h.add_tag} in its generator), consumers only ever use the tag (never another slice's item or class).
 * Phase 1 creates them all (empty, except {@link #LEKKERNIJ} = kaasknabbels + #gebak, {@link #GRIJPTICKETS} = kermisbon).
 */
public final class KnusTags {
    public static final TagKey<Item> KAASMELK = item("kaasmelk"), KNABBELEI = item("knabbelei"), PLUISWOL = item("pluiswol"),
            THEEKRUID = item("theekruid"), KNABBELGRAAN = item("knabbelgraan"), GUHBLOEM = item("guhbloem"), OOGST = item("oogst"),
            GEBAK = item("gebak"), THEE = item("thee"), MARSHMALLOW = item("marshmallow"), LEKKERNIJ = item("lekkernij"),
            GRIJPTICKETS = item("grijptickets"), FEESTTAART = item("feesttaart"), THEESERVIES = item("theeservies"),
            FEESTKAPSELS = item("feestkapsels"), FEESTSLINGERS = item("feestslingers"), FEESTBLOEMEN = item("feestbloemen"),
            STERRENLANTAARNS = item("sterrenlantaarns");
    /** Block tag guhs:knus/knuffels: the plushies (wereldleven) that guhs cuddle. */
    public static final TagKey<Block> KNUFFELS = TagKey.create(Registries.BLOCK, Guhs.id("knus/knuffels"));

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, Guhs.id("knus/" + name));
    }

    private KnusTags() {
    }
}
