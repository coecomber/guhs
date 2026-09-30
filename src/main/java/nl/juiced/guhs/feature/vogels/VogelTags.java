package nl.juiced.guhs.feature.vogels;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import nl.juiced.guhs.Guhs;

/** The item tags of the birds' food (tools/features/vogels.py writes them). */
public final class VogelTags {
    /** Seeds: the pluisvinkje's favourite, and what goes into a bird feeder. */
    public static final TagKey<Item> ZAADJES = TagKey.create(Registries.ITEM, Guhs.id("vogels/zaadjes"));
    /** Berries: the kaasmeesje's favourite (knabbelbessen!). */
    public static final TagKey<Item> BESSEN = TagKey.create(Registries.ITEM, Guhs.id("vogels/bessen"));
    /** Fish and bread: what the zeemeeuwtjes want. Mijn! Mijn! */
    public static final TagKey<Item> VISJES = TagKey.create(Registries.ITEM, Guhs.id("vogels/visjes"));
    /** What the guh-uiltje likes: kaasknabbels. */
    public static final TagKey<Item> UILTJESHAPJES = TagKey.create(Registries.ITEM, Guhs.id("vogels/uiltjeshapjes"));

    private VogelTags() {
    }
}
