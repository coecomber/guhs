package nl.juiced.guhs.registry;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * Vahoege Vads: as good as diamond, but unbreakable and kept when you die (see KeepOnDeathHandler).
 * Armour textures: assets/guhs/textures/models/armor/vahoege_vads_layer_1.png / _layer_2.png.
 */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Guhs.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> VAHOEGE_VADS = ARMOR_MATERIALS.register("vahoege_vads", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 3);      // same as diamond
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.BODY, 11);
        return new ArmorMaterial(defense, 10, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(ModItems.VAHOEGE_VADS_INGOT.get()),
                List.of(new ArmorMaterial.Layer(Guhs.id("vahoege_vads"))), 2.0f, 0.0f);
    });

    /** Tool tier: diamond stats (mining level, speed, damage, enchantability). */
    public static final Tier VADS_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0f, 3.0f, 10,
            () -> Ingredient.of(ModItems.VAHOEGE_VADS_INGOT.get()));

    private ModArmorMaterials() {
    }
}
