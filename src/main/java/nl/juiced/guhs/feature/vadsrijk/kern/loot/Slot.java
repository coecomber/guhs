package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * Where a piece of gear goes. WAPEN = the main hand; HOOFD..VOETEN = the normal armour slots; RING, KETTING, OORBEL = the
 * jewellery slots of {@code kern.Sieraden} (no vanilla slot: kept in the player data, Vadsrijk only). All count only in the dimension.
 */
public enum Slot {
    WAPEN(EquipmentSlot.MAINHAND), HOOFD(EquipmentSlot.HEAD), BORST(EquipmentSlot.CHEST), BENEN(EquipmentSlot.LEGS), VOETEN(EquipmentSlot.FEET),
    RING(null), KETTING(null), OORBEL(null);

    /** The jewellery slots, in HUD order. */
    public static final Slot[] SIERADEN = {RING, KETTING, OORBEL};

    @javax.annotation.Nullable
    public final EquipmentSlot vanilla;

    Slot(EquipmentSlot vanilla) {
        this.vanilla = vanilla;
    }

    public boolean harnas() {
        return this != WAPEN && vanilla != null;
    }

    public boolean sieraad() {
        return vanilla == null;
    }

    public String key() {
        return "vadsrijk.kern.slot." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
