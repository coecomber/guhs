package nl.juiced.guhs.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/**
 * Guh armour (iron / diamond / netherite). Right-click your tamed guh with it; it shows as a little helmet + side
 * plates in the tier's colour (bones armor_iron / armor_diamond / armor_netherite in guh.geo.json).
 * Protection is the vanilla body-armour value of the material (like horse/wolf armour).
 */
public class GuhArmorItem extends AnimalArmorItem {
    public enum Tier { IRON, DIAMOND, NETHERITE }

    private final Tier tier;

    public GuhArmorItem(Holder<ArmorMaterial> material, Tier tier, Properties properties) {
        super(material, BodyType.CANINE, false, properties);
        this.tier = tier;
    }

    public Tier getTier() {
        return tier;
    }
}
