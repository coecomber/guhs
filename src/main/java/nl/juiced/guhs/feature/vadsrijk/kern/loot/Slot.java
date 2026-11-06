package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * Where a piece of gear goes. WAPEN = the main hand; HOOFD..VOETEN = the normal armour slots; RING, KETTING, OORBEL = the
 * jewellery slots of {@code kern.Sieraden} (no vanilla slot: kept in the player data, Vadsrijk only). All count only in the dimension.
 * <p>T-ITEMS (itemisatie 2.0, R4/R5): four more virtual slots, kept exactly like the jewellery: RING2 (a second ring: a ring
 * base type has slot RING and fits both, see {@link #past}), ARMBAND, HANDSCHOENEN and CAPE (real gear with stats; the look of
 * the cape comes from the wardrobe). {@link #sieraad()} means "a virtual slot of kern.Sieraden" for all of them.
 */
public enum Slot {
    WAPEN(EquipmentSlot.MAINHAND), HOOFD(EquipmentSlot.HEAD), BORST(EquipmentSlot.CHEST), BENEN(EquipmentSlot.LEGS), VOETEN(EquipmentSlot.FEET),
    RING(null), KETTING(null), OORBEL(null),
    RING2(null), ARMBAND(null), HANDSCHOENEN(null), CAPE(null);   // T-ITEMS

    /** The virtual slots, in screen order (T-ITEMS: + RING2, ARMBAND, HANDSCHOENEN, CAPE). */
    public static final Slot[] SIERADEN = {RING, RING2, KETTING, OORBEL, ARMBAND, HANDSCHOENEN, CAPE};

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

    /** T-ITEMS: may a piece whose base type has slot {@code itemSlot} be worn in this slot? (A ring fits RING and RING2.) */
    public boolean past(Slot itemSlot) {
        return this == itemSlot || (this == RING2 && itemSlot == RING);
    }

    public String key() {
        return "vadsrijk.kern.slot." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
