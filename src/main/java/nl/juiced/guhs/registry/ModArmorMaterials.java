package nl.juiced.guhs.registry;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.Guhs;

/**
 * Vahoege Vads: as good as diamond, but unbreakable and kept when you die (see KeepOnDeathHandler).
 * 26.1: armour materials are plain records (no registry any more); the armour texture comes from the equipment asset
 * {@code assets/guhs/equipment/vahoege_vads.json} (textures/entity/equipment/humanoid[_leggings]/vahoege_vads.png).
 */
public final class ModArmorMaterials {
    /** Items that repair Vads gear (the ingot; also set directly on the items, see ModItems#vadsRepair). */
    public static final TagKey<Item> VADS_REPAIR = ItemTags.create(Guhs.id("vahoege_vads_repair"));

    public static final ResourceKey<EquipmentAsset> VAHOEGE_VADS_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Guhs.id("vahoege_vads"));

    public static final ArmorMaterial VAHOEGE_VADS;

    static {
        Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
        defense.put(ArmorType.BOOTS, 3);      // same as diamond
        defense.put(ArmorType.LEGGINGS, 6);
        defense.put(ArmorType.CHESTPLATE, 8);
        defense.put(ArmorType.HELMET, 3);
        defense.put(ArmorType.BODY, 11);
        VAHOEGE_VADS = new ArmorMaterial(33, defense, 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0f, 0.0f, VADS_REPAIR, VAHOEGE_VADS_ASSET);
    }

    /** Tool material: diamond stats (mining level, speed, damage, enchantability). */
    public static final ToolMaterial VADS_TIER = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0f, 3.0f, 10, VADS_REPAIR);

    /**
     * 26.1: there is no armour material registry any more. Kept as a no-op so {@code Guhs} keeps compiling until its
     * {@code ARMOR_MATERIALS.register(modBus)} line is removed (request to B).
     */
    @Deprecated
    public static final NoRegistry ARMOR_MATERIALS = new NoRegistry();

    public static final class NoRegistry {
        public void register(IEventBus bus) {
        }
    }

    private ModArmorMaterials() {
    }
}
