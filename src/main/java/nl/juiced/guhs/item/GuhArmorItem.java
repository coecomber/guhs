package nl.juiced.guhs.item;

import net.minecraft.world.item.Item;

/**
 * Guh armour (iron / diamond / netherite). Right-click your tamed guh with it; it shows as a little helmet + side
 * plates in the tier's colour (bones armor_iron / armor_diamond / armor_netherite in guh.geo.json).
 * Protection is the vanilla body-armour value of the material (like horse/wolf armour).
 * 26.1: AnimalArmorItem is gone - the armour value and the body slot come from item components (see ModItems#guhArmor).
 */
public class GuhArmorItem extends Item {
    public enum Tier { IRON, DIAMOND, NETHERITE }

    private final Tier tier;

    public GuhArmorItem(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public Tier getTier() {
        return tier;
    }
}
