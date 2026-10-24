package nl.juiced.guhs.feature.mijnhulp;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.item.PaxelItem;
import nl.juiced.guhs.item.VadsSchaarItem;
import nl.juiced.guhs.registry.ModArmorMaterials;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.7.0 "Onder de Wol", the mining helpers (design: ontwerp-guhs170-mijnen.md rondes 31, 37-40, 44). Resources:
 * tools/features/mijnhulp.py (+ mijnhulp_tex.py).
 * <ul>
 *   <li>the Vahoege Vadsknakker ({@link VadsknakkerItem}, recipe {@link VadsknakkerRecept}): paxel + shears + hoe in one;</li>
 *   <li>Super vahoege vads: the netherite tier above vahoege vads. A Vahoegietstaaf (4 vahoegiet + 4 vahoege vads-staven) and
 *       the Super-vads-sjabloon upgrade any vahoege vads tool or armour piece on the smithing table, keeping its
 *       enchantments and name (vanilla's smithing_transform copies the components). Netherite stats, still unbreakable,
 *       kept on death, and fire- (so kaasfrituursaus-) proof as an item. The template is found in the chests of the
 *       deepest cave structures (loot table guhs:mijnhulp/super_vads_sjabloon, added there by the structures) and
 *       copied with 7 vahoegiet + 1 guhkristalsteen;</li>
 *   <li>the Wolkenliftje ({@link Wolkenliftje}): jump on it to float up to the next one, sneak to go down;</li>
 *   <li>the Pluisladder ({@link PluisladderBlock}): a roll that unrolls down to the floor by itself;</li>
 *   <li>the Mijnlampje ({@link Mijnlampje}): a guh hat with a lamp, a walking light for your tamed guh;</li>
 *   <li>terraria of any shape ({@link TerrariumRuimte}): terrariumglas and the new terrariumbodem joined to a terrarium
 *       are one living space for its little creatures.</li>
 * </ul>
 */
public final class MijnhulpFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECEPTEN = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Guhs.MODID);

    static {
        RECEPTEN.register("vadsknakker", () -> VadsknakkerRecept.SERIALIZER);
    }

    // =================================================================================================================
    // super vahoege vads
    // =================================================================================================================
    /** Netherite's stats (mining level, speed 9, +4 damage, enchantability 15); the vads repair tag (they never break). */
    public static final ToolMaterial SUPER_TIER = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 9.0f, 4.0f, 15,
            ModArmorMaterials.VADS_REPAIR);
    public static final ResourceKey<EquipmentAsset> SUPER_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Guhs.id("super_vahoege_vads"));
    /** Netherite armour: 3/6/8/3, toughness 3, knockback resistance 0.1 per piece. */
    public static final ArmorMaterial SUPER_VADS;

    static {
        Map<ArmorType, Integer> afweer = new EnumMap<>(ArmorType.class);
        afweer.put(ArmorType.BOOTS, 3);
        afweer.put(ArmorType.LEGGINGS, 6);
        afweer.put(ArmorType.CHESTPLATE, 8);
        afweer.put(ArmorType.HELMET, 3);
        afweer.put(ArmorType.BODY, 11);
        SUPER_VADS = new ArmorMaterial(37, afweer, 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0f, 0.1f, ModArmorMaterials.VADS_REPAIR, SUPER_ASSET);
    }

    private static Item.Properties onbreekbaar() {
        return new Item.Properties().component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    /** Super gear: unbreakable, fire-proof as an item (lava and kaasfrituursaus), shiny name. */
    private static Item.Properties superVads() {
        return onbreekbaar().fireResistant().rarity(Rarity.RARE);
    }

    // --- the Vadsknakker -------------------------------------------------------------------------------------------------
    public static final DeferredItem<VadsknakkerItem> VADSKNAKKER = ITEMS.registerItem("vahoege_vadsknakker", VadsknakkerItem::new,
            () -> VadsknakkerItem.properties(onbreekbaar(), ModArmorMaterials.VADS_TIER, 4.0f, -2.9f));

    // --- the material and the template -------------------------------------------------------------------------------------
    public static final DeferredItem<Item> VAHOEGIETSTAAF = ITEMS.registerItem("vahoegietstaaf", Item::new,
            () -> new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SJABLOON = ITEMS.registerItem("super_vads_sjabloon", Item::new,
            () -> new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON));

    // --- the super gear ------------------------------------------------------------------------------------------------------
    public static final DeferredItem<Item> SUPER_SWORD = ITEMS.registerItem("super_vahoege_vads_sword", Item::new,
            () -> superVads().sword(SUPER_TIER, 3.0f, -2.4f));
    public static final DeferredItem<Item> SUPER_PICKAXE = ITEMS.registerItem("super_vahoege_vads_pickaxe", Item::new,
            () -> superVads().pickaxe(SUPER_TIER, 1.0f, -2.8f));
    public static final DeferredItem<AxeItem> SUPER_AXE = ITEMS.registerItem("super_vahoege_vads_axe",
            props -> new AxeItem(SUPER_TIER, 5.0f, -3.0f, props), MijnhulpFeature::superVads);
    public static final DeferredItem<ShovelItem> SUPER_SHOVEL = ITEMS.registerItem("super_vahoege_vads_shovel",
            props -> new ShovelItem(SUPER_TIER, 1.5f, -3.0f, props), MijnhulpFeature::superVads);
    public static final DeferredItem<HoeItem> SUPER_HOE = ITEMS.registerItem("super_vahoege_vads_hoe",
            props -> new HoeItem(SUPER_TIER, -4.0f, 0.0f, props), MijnhulpFeature::superVads);
    public static final DeferredItem<PaxelItem> SUPER_PAXEL = ITEMS.registerItem("super_vahoege_vads_paxel", PaxelItem::new,
            () -> PaxelItem.properties(superVads(), SUPER_TIER, 4.0f, -2.9f));
    public static final DeferredItem<VadsSchaarItem> SUPER_SHEARS = ITEMS.registerItem("super_vahoege_vads_shears", VadsSchaarItem::new,
            () -> VadsSchaarItem.properties(new Item.Properties().fireResistant().rarity(Rarity.RARE)));
    public static final DeferredItem<VadsknakkerItem> SUPER_VADSKNAKKER = ITEMS.registerItem("super_vahoege_vadsknakker", VadsknakkerItem::new,
            () -> VadsknakkerItem.properties(superVads(), SUPER_TIER, 5.0f, -2.9f));
    public static final DeferredItem<Item> SUPER_HELMET = ITEMS.registerItem("super_vahoege_vads_helmet", Item::new,
            () -> superVads().humanoidArmor(SUPER_VADS, ArmorType.HELMET));
    public static final DeferredItem<Item> SUPER_CHESTPLATE = ITEMS.registerItem("super_vahoege_vads_chestplate", Item::new,
            () -> superVads().humanoidArmor(SUPER_VADS, ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> SUPER_LEGGINGS = ITEMS.registerItem("super_vahoege_vads_leggings", Item::new,
            () -> superVads().humanoidArmor(SUPER_VADS, ArmorType.LEGGINGS));
    public static final DeferredItem<Item> SUPER_BOOTS = ITEMS.registerItem("super_vahoege_vads_boots", Item::new,
            () -> superVads().humanoidArmor(SUPER_VADS, ArmorType.BOOTS));

    /** Every vahoege vads piece and its super version (the smithing recipes, the anvil merge, the tests). */
    public static final Map<Supplier<? extends Item>, Supplier<? extends Item>> OPWAARDERING = new LinkedHashMap<>();

    static {
        OPWAARDERING.put(ModItems.VADS_SWORD, SUPER_SWORD);
        OPWAARDERING.put(ModItems.VADS_PICKAXE, SUPER_PICKAXE);
        OPWAARDERING.put(ModItems.VADS_AXE, SUPER_AXE);
        OPWAARDERING.put(ModItems.VADS_SHOVEL, SUPER_SHOVEL);
        OPWAARDERING.put(ModItems.VADS_HOE, SUPER_HOE);
        OPWAARDERING.put(ModItems.VADS_PAXEL, SUPER_PAXEL);
        OPWAARDERING.put(ModItems.VADS_SHEARS, SUPER_SHEARS);
        OPWAARDERING.put(VADSKNAKKER, SUPER_VADSKNAKKER);
        OPWAARDERING.put(ModItems.VADS_HELMET, SUPER_HELMET);
        OPWAARDERING.put(ModItems.VADS_CHESTPLATE, SUPER_CHESTPLATE);
        OPWAARDERING.put(ModItems.VADS_LEGGINGS, SUPER_LEGGINGS);
        OPWAARDERING.put(ModItems.VADS_BOOTS, SUPER_BOOTS);
    }

    /** The new unbreakable gear of this module (for {@link nl.juiced.guhs.event.VadsAambeeld}: two of them merge on the anvil). */
    public static List<Supplier<? extends Item>> nieuweVads() {
        List<Supplier<? extends Item>> uit = new ArrayList<>(OPWAARDERING.values());
        uit.add(VADSKNAKKER);
        return uit;
    }

    // =================================================================================================================
    // blocks
    // =================================================================================================================
    public static final DeferredBlock<Block> WOLKENLIFTJE = BLOCKS.registerSimpleBlock("wolkenliftje",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f).sound(SoundType.WOOL).lightLevel(s -> 6));
    public static final DeferredBlock<PluisladderBlock> PLUISLADDER = BLOCKS.registerBlock("pluisladder", PluisladderBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.4f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    /** The Mijnlampje's light: invisible, no collision, replaceable, removes itself when no lamp guh is near. No item. */
    public static final DeferredBlock<Mijnlampje.Licht> MIJNLAMP_LICHT = BLOCKS.registerBlock("mijnlampje_licht", Mijnlampje.Licht::new,
            () -> BlockBehaviour.Properties.of().replaceable().noCollision().noOcclusion().noLootTable().instabreak()
                    .lightLevel(s -> Mijnlampje.LICHT).randomTicks().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<TerrariumRuimte.Bodem> TERRARIUMBODEM = BLOCKS.registerBlock("terrariumbodem", TerrariumRuimte.Bodem::new,
            () -> BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GLASS).strength(0.5f));

    public static final DeferredItem<?> WOLKENLIFTJE_ITEM = ITEMS.registerSimpleBlockItem(WOLKENLIFTJE);
    public static final DeferredItem<?> PLUISLADDER_ITEM = ITEMS.registerSimpleBlockItem(PLUISLADDER);
    public static final DeferredItem<?> TERRARIUMBODEM_ITEM = ITEMS.registerSimpleBlockItem(TERRARIUMBODEM);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        RECEPTEN.register(modBus);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
                DispenserBlock.registerBehavior(SUPER_SHEARS.get(), new ShearsDispenseItemBehavior())));
        NeoForge.EVENT_BUS.addListener(Mijnlampje::onTick);
        NeoForge.EVENT_BUS.addListener(Mijnlampje::onLeave);
        NeoForge.EVENT_BUS.addListener(TerrariumRuimte::onBlok);
        // the Mijnlampje is crafted (it is also the reward of the Mijnguh-hulpje questline, see the design)
        nl.juiced.guhs.feature.kleding.KledingBronnen.bron(GuhClothes.MIJNLAMPJE, "crafting");
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToServer(Wolkenliftje.Rit.TYPE, Wolkenliftje.Rit.STREAM_CODEC, Wolkenliftje.Rit::handle);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(VADSKNAKKER.get()));
        output.accept(new ItemStack(VAHOEGIETSTAAF.get()));
        output.accept(new ItemStack(SJABLOON.get()));
        for (Supplier<? extends Item> s : OPWAARDERING.values()) {
            output.accept(new ItemStack(s.get()));
        }
        output.accept(new ItemStack(WOLKENLIFTJE_ITEM.get()));
        output.accept(new ItemStack(PLUISLADDER_ITEM.get()));
        output.accept(new ItemStack(TERRARIUMBODEM_ITEM.get()));
        output.accept(new ItemStack(ModItems.clothingItem(GuhClothes.MIJNLAMPJE)));
    }

    private MijnhulpFeature() {
    }
}
