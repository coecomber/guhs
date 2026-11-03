package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import net.minecraft.world.entity.EquipmentSlot;

/** Where a piece of gear goes. WAPEN = the main hand; the rest = the normal armour slots (counted only in the dimension). */
public enum Slot {
    WAPEN(EquipmentSlot.MAINHAND), HOOFD(EquipmentSlot.HEAD), BORST(EquipmentSlot.CHEST), BENEN(EquipmentSlot.LEGS), VOETEN(EquipmentSlot.FEET);

    public final EquipmentSlot vanilla;

    Slot(EquipmentSlot vanilla) {
        this.vanilla = vanilla;
    }

    public boolean harnas() {
        return this != WAPEN;
    }
}
