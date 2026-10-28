package nl.juiced.guhs.feature.slaapkleding;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.0 Guhdromenland: the player's droompyjama (muts, jasje, broek, sloffen). Cosmetic (one point of armour per piece,
 * like a soft blanket); the whole set gives a small dream bonus ({@link #draagtSet}):
 * <ul>
 *   <li>the dream fade (falling asleep / waking up) goes faster (DroomsfeerClient);</li>
 *   <li>a caught droomsterretje gives a bit more droomzand (VallendeSter.vang);</li>
 *   <li>in bed the "Samen dromen?" question comes sooner (DroomSlapen).</li>
 * </ul>
 * The armour texture comes from the equipment asset {@code assets/guhs/equipment/droompyjama.json} (tools/features/slaapkleding.py).
 */
public final class Droompyjama {
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Guhs.id("droompyjama"));

    public static final ArmorMaterial MATERIAAL;

    static {
        Map<ArmorType, Integer> bescherming = new EnumMap<>(ArmorType.class);
        bescherming.put(ArmorType.BOOTS, 1);
        bescherming.put(ArmorType.LEGGINGS, 1);
        bescherming.put(ArmorType.CHESTPLATE, 1);
        bescherming.put(ArmorType.HELMET, 1);
        bescherming.put(ArmorType.BODY, 1);
        MATERIAAL = new ArmorMaterial(5, bescherming, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0f, 0.0f, ItemTags.WOOL, ASSET);
    }

    public static final DeferredItem<Item> MUTS = item("droompyjama_muts", ArmorType.HELMET);
    public static final DeferredItem<Item> JASJE = item("droompyjama_jasje", ArmorType.CHESTPLATE);
    public static final DeferredItem<Item> BROEK = item("droompyjama_broek", ArmorType.LEGGINGS);
    public static final DeferredItem<Item> SLOFFEN = item("droompyjama_sloffen", ArmorType.BOOTS);

    /** With the whole set: the dream fade takes this share of its normal time. */
    public static final float FADE_FACTOR = 0.6f;
    /** With the whole set: the bed question comes after this many sleeping ticks (instead of DroomSlapen.VRAAG_TICKS). */
    public static final int VRAAG_TICKS = 4;

    private static DeferredItem<Item> item(String naam, ArmorType type) {
        return SlaapkledingFeature.ITEMS.registerItem(naam, Item::new, () -> new Item.Properties().humanoidArmor(MATERIAAL, type));
    }

    /** Whether this one wears all four pieces of the droompyjama. */
    public static boolean draagtSet(@Nullable LivingEntity e) {
        return e != null && e.getItemBySlot(EquipmentSlot.HEAD).is(MUTS.get()) && e.getItemBySlot(EquipmentSlot.CHEST).is(JASJE.get())
                && e.getItemBySlot(EquipmentSlot.LEGS).is(BROEK.get()) && e.getItemBySlot(EquipmentSlot.FEET).is(SLOFFEN.get());
    }

    /** How many pieces of the set this one wears (for the tooltip). */
    public static int stukken(@Nullable LivingEntity e) {
        if (e == null) {
            return 0;
        }
        int n = 0;
        n += e.getItemBySlot(EquipmentSlot.HEAD).is(MUTS.get()) ? 1 : 0;
        n += e.getItemBySlot(EquipmentSlot.CHEST).is(JASJE.get()) ? 1 : 0;
        n += e.getItemBySlot(EquipmentSlot.LEGS).is(BROEK.get()) ? 1 : 0;
        n += e.getItemBySlot(EquipmentSlot.FEET).is(SLOFFEN.get()) ? 1 : 0;
        return n;
    }

    public static boolean isStuk(Item item) {
        return item == MUTS.get() || item == JASJE.get() || item == BROEK.get() || item == SLOFFEN.get();
    }

    static void laad() {
        // (the static fields register the items with SlaapkledingFeature.ITEMS)
    }

    private Droompyjama() {
    }
}
