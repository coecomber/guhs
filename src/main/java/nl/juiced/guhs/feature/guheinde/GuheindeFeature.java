package nl.juiced.guhs.feature.guheinde;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.resources.Identifier;
/**
 * Het Guheinde: the endgame, a parody of the End and the Ender Dragon. Opper-Mika steals all the kaasknabbels of the
 * guh kingdom (without knabbels a guh is never vahoeg) and keeps them in the Guheinde.
 * <ul>
 *   <li>The Koningguh tells you the story ({@link GuheindeEvents#talkToKoning}); Mika's cry Mika-tranen; an Oog van
 *       Vadsig ({@link OogVanVadsigItem}) flies to the nearest Knabbelkelder (4 in a ring underground in the Guhmension).</li>
 *   <li>In the Knabbelkelder: Mika-larfjes, cells with magere guhs (feed them!), a library and the open portal room with
 *       12 knabbelportaalframes ({@link KnabbelportaalframeBlock}).</li>
 *   <li>The Guheinde ({@link GuheindeReis}): an island of kaaskorst with the Knabbelberg and kaaspilaren with
 *       knabbelkristallen ({@link KnabbelkristalEntity}): the stolen knabbels, that heal Opper-Mika.</li>
 *   <li>Opper-Mika ({@link OpperMikaEntity}) rides a starved Enderguh ({@link HongerigeEnderguhEntity}); the fight is run
 *       by {@link GuheindeGevecht}. Rewards: a tamed Vahoege Enderguh (later an Enderguh-ei), the Knabbelkroon, the
 *       knabbelschat, Knabbelpoorten to the outer islands with Mika-vestingen and the vetschip with the Guhvleugels.</li>
 * </ul>
 * Resources: tools/features/guheinde.py (+ guheinde_bouw.py for the structures).
 */
public final class GuheindeFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);

    public static final ResourceKey<Level> GUHEINDE = ResourceKey.create(Registries.DIMENSION, Guhs.id("guheinde"));
    public static final ResourceKey<Structure> KNABBELKELDER = ResourceKey.create(Registries.STRUCTURE, Guhs.id("knabbelkelder"));
    public static final ResourceKey<Structure> MIKA_VESTING = ResourceKey.create(Registries.STRUCTURE, Guhs.id("mika_vesting"));
    /** What an Oog van Vadsig looks for (data/guhs/tags/worldgen/structure/oog_van_vadsig_located.json). */
    public static final TagKey<Structure> OOG_LOCATED = TagKey.create(Registries.STRUCTURE, Guhs.id("oog_van_vadsig_located"));

    public static boolean isGuheinde(Level level) {
        return level.dimension() == GUHEINDE;
    }

    // --- blocks: the stone of the Guheinde, the Mika-vesting, the portal ---------------------------------------------
    private static BlockBehaviour.Properties korst() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).mapColor(MapColor.COLOR_YELLOW);
    }

    private static BlockBehaviour.Properties mikaSteen() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.PURPUR_BLOCK).mapColor(MapColor.COLOR_PURPLE);
    }

    private static BlockBehaviour.Properties unbreakable() {
        return BlockBehaviour.Properties.of().strength(-1f, 3600000f).noLootTable().isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<net.minecraft.world.level.block.Block> KAASKORST = BLOCKS.registerSimpleBlock("kaaskorst", () -> korst());
    public static final DeferredBlock<net.minecraft.world.level.block.Block> KAASKORST_STENEN = BLOCKS.registerSimpleBlock("kaaskorst_stenen",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE_BRICKS).mapColor(MapColor.COLOR_YELLOW));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> GEBARSTEN_KAASKORST_STENEN = BLOCKS.registerSimpleBlock("gebarsten_kaaskorst_stenen",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE_BRICKS).mapColor(MapColor.COLOR_YELLOW));
    public static final DeferredBlock<StairBlock> KAASKORST_STENEN_TRAP = BLOCKS.registerBlock("kaaskorst_stenen_trap",
            p -> new StairBlock(KAASKORST_STENEN.get().defaultBlockState(), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE_BRICK_STAIRS));
    public static final DeferredBlock<SlabBlock> KAASKORST_STENEN_PLAAT = BLOCKS.registerBlock("kaaskorst_stenen_plaat", SlabBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE_BRICK_SLAB));
    public static final DeferredBlock<WallBlock> KAASKORST_STENEN_MUUR = BLOCKS.registerBlock("kaaskorst_stenen_muur", WallBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE_BRICK_WALL));
    /** Kaaskorststenen with bites in them: a Mika-larfje lives inside. */
    public static final DeferredBlock<LarfjeBlock> AANGEVRETEN_KAASKORST_STENEN = BLOCKS.registerBlock("aangevreten_kaaskorst_stenen", LarfjeBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE_BRICKS).mapColor(MapColor.COLOR_YELLOW).destroyTime(0.4f).explosionResistance(0.75f));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> MIKA_STEEN = BLOCKS.registerSimpleBlock("mika_steen", () -> mikaSteen());
    public static final DeferredBlock<RotatedPillarBlock> MIKA_STEEN_PILAAR = BLOCKS.registerBlock("mika_steen_pilaar", RotatedPillarBlock::new, () -> mikaSteen());
    public static final DeferredBlock<StairBlock> MIKA_STEEN_TRAP = BLOCKS.registerBlock("mika_steen_trap",
            p -> new StairBlock(MIKA_STEEN.get().defaultBlockState(), p), () -> mikaSteen());
    public static final DeferredBlock<SlabBlock> MIKA_STEEN_PLAAT = BLOCKS.registerBlock("mika_steen_plaat", SlabBlock::new, () -> mikaSteen());

    public static final DeferredBlock<KnabbelportaalframeBlock> KNABBELPORTAALFRAME = BLOCKS.registerBlock("knabbelportaalframe",
            KnabbelportaalframeBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_PORTAL_FRAME).mapColor(MapColor.COLOR_YELLOW).lightLevel(s -> 3));
    public static final DeferredBlock<GuheindePortaalBlock> GUHEINDE_PORTAAL = BLOCKS.registerBlock("guheinde_portaal", GuheindePortaalBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_PORTAL).mapColor(MapColor.COLOR_PINK).lightLevel(s -> 15));
    /** Where you put a knabbelkristal to call Opper-Mika back (four around the terugportaal on the Knabbelberg). */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> KNABBELSOKKEL = BLOCKS.registerSimpleBlock("knabbelsokkel",
            () -> unbreakable().mapColor(MapColor.GOLD).lightLevel(s -> 7).sound(SoundType.STONE));
    /** The door of Opper-Mika's knabbelschat: gone after the first win. */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> KNABBELSLOT = BLOCKS.registerSimpleBlock("knabbelslot",
            () -> unbreakable().mapColor(MapColor.COLOR_ORANGE).sound(SoundType.METAL));
    public static final DeferredBlock<KnabbelpoortBlock> KNABBELPOORT = BLOCKS.registerBlock("knabbelpoort", KnabbelpoortBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_GATEWAY).mapColor(MapColor.COLOR_PINK).lightLevel(s -> 15));
    public static final DeferredBlock<EnderguhEiBlock> ENDERGUH_EI = BLOCKS.registerBlock("enderguh_ei", EnderguhEiBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3f, 9f).lightLevel(s -> 3).noOcclusion().randomTicks()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<nl.juiced.guhs.block.GuhWorkstationBlock> OPPER_MIKATROFEE = BLOCKS.registerBlock("opper_mikatrofee",
            nl.juiced.guhs.block.GuhWorkstationBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2.5f).sound(SoundType.METAL).noOcclusion());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnabbelpoortBlock.Entity>> KNABBELPOORT_BE = BLOCK_ENTITY_TYPES.register("knabbelpoort",
            () -> new BlockEntityType<>(KnabbelpoortBlock.Entity::new, KNABBELPOORT.get()));

    // --- the Knabbelkroon: a helmet with the protection of vahoege vads ------------------------------------------------
    /** 1.1.0: the crown on your head is the equipment asset guhs:knabbelkroon (assets/guhs/equipment/knabbelkroon.json). */
    public static final ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> KROON_ASSET =
            ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Guhs.id("knabbelkroon"));
    /** 1.1.0: the Guhvleugels on your back (equipment asset guhs:guhvleugels with a "wings" layer). */
    public static final ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> VLEUGELS_ASSET =
            ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Guhs.id("guhvleugels"));
    /** Same numbers as 1.0.0's armour material (helmet 4, toughness 3, knockback resistance 0.1, durability x45, enchantability 25). */
    public static final ArmorMaterial KROON_MATERIAL = new ArmorMaterial(45, java.util.Map.of(
            ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 9, ArmorType.LEGGINGS, 7, ArmorType.BOOTS, 4, ArmorType.BODY, 11),
            25, SoundEvents.ARMOR_EQUIP_GOLD, 3.0f, 0.1f, nl.juiced.guhs.registry.ModArmorMaterials.VADS_REPAIR, KROON_ASSET);

    // --- items -----------------------------------------------------------------------------------------------------------
    public static final DeferredItem<Item> MIKA_TRAAN = ITEMS.registerSimpleItem("mika_traan", () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<OogVanVadsigItem> OOG_VAN_VADSIG = ITEMS.registerItem("oog_van_vadsig", OogVanVadsigItem::new,
            () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<KnabbelkristalItem> KNABBELKRISTAL = ITEMS.registerItem("knabbelkristal", KnabbelkristalItem::new,
            () -> new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<KnabbelkroonItem> KNABBELKROON = ITEMS.registerItem("knabbelkroon",
            KnabbelkroonItem::new, () -> new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                    .humanoidArmor(KROON_MATERIAL, ArmorType.HELMET).repairable(ModItems.VAHOEGE_VADS_INGOT.get()));
    public static final DeferredItem<GuhvleugelsItem> GUHVLEUGELS = ITEMS.registerItem("guhvleugels", GuhvleugelsItem::new,
            () -> new Item.Properties().durability(540).rarity(Rarity.EPIC)
                    .component(net.minecraft.core.component.DataComponents.GLIDER, net.minecraft.util.Unit.INSTANCE)
                    .component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
                            net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.CHEST)
                                    .setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA).setAsset(VLEUGELS_ASSET).setDamageOnHurt(false).build())
                    .repairable(ModItems.MIKA_VET.get()));

    static {
        for (DeferredBlock<?> block : List.of(KAASKORST, KAASKORST_STENEN, GEBARSTEN_KAASKORST_STENEN, KAASKORST_STENEN_TRAP, KAASKORST_STENEN_PLAAT,
                KAASKORST_STENEN_MUUR, AANGEVRETEN_KAASKORST_STENEN, MIKA_STEEN, MIKA_STEEN_PILAAR, MIKA_STEEN_TRAP, MIKA_STEEN_PLAAT,
                KNABBELPORTAALFRAME, KNABBELSOKKEL, KNABBELSLOT)) {
            ITEMS.registerSimpleBlockItem(block);
        }
        ITEMS.registerItem("enderguh_ei", p -> new BlockItem(ENDERGUH_EI.get(), p), () -> new Item.Properties().rarity(Rarity.EPIC));
        ITEMS.registerItem("opper_mikatrofee", p -> new BlockItem(OPPER_MIKATROFEE.get(), p), () -> new Item.Properties().rarity(Rarity.EPIC));
    }

    // --- entities ----------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<OpperMikaEntity>> OPPER_MIKA = ENTITY_TYPES.register("opper_mika",
            () -> EntityType.Builder.of(OpperMikaEntity::new, MobCategory.MONSTER).sized(0.9f, 0.8f).eyeHeight(0.55f).fireImmune().notInPeaceful()
                    .clientTrackingRange(16).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("opper_mika"))));
    public static final DeferredHolder<EntityType<?>, EntityType<HongerigeEnderguhEntity>> HONGERIGE_ENDERGUH = ENTITY_TYPES.register("hongerige_enderguh",
            () -> EntityType.Builder.of(HongerigeEnderguhEntity::new, MobCategory.MISC).sized(1.1f, 0.9f).eyeHeight(0.55f).fireImmune()
                    .passengerAttachments(new Vec3(0, 0.62, -0.2)).clientTrackingRange(16).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("hongerige_enderguh"))));
    public static final DeferredHolder<EntityType<?>, EntityType<KnabbelkristalEntity>> KNABBELKRISTAL_ENTITY = ENTITY_TYPES.register("knabbelkristal",
            () -> EntityType.Builder.<KnabbelkristalEntity>of(KnabbelkristalEntity::new, MobCategory.MISC).sized(2f, 2f).fireImmune()
                    .clientTrackingRange(16).updateInterval(Integer.MAX_VALUE).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("knabbelkristal"))));
    public static final DeferredHolder<EntityType<?>, EntityType<MikaLarfjeEntity>> MIKA_LARFJE = ENTITY_TYPES.register("mika_larfje",
            () -> EntityType.Builder.of(MikaLarfjeEntity::new, MobCategory.MONSTER).sized(0.4f, 0.35f).eyeHeight(0.22f).notInPeaceful()
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("mika_larfje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<OogVanVadsigEntity>> OOG_ENTITY = ENTITY_TYPES.register("oog_van_vadsig",
            () -> EntityType.Builder.<OogVanVadsigEntity>of(OogVanVadsigEntity::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(4).updateInterval(4).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("oog_van_vadsig"))));
    public static final DeferredHolder<EntityType<?>, EntityType<MikaVetbalEntity>> MIKA_VETBAL = ENTITY_TYPES.register("mika_vetbal",
            () -> EntityType.Builder.<MikaVetbalEntity>of(MikaVetbalEntity::new, MobCategory.MISC).sized(0.5f, 0.5f)
                    .clientTrackingRange(8).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("mika_vetbal"))));

    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> MIKA_LARFJE_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "mika_larfje_spawn_egg", MIKA_LARFJE);   // 1.0.0 colours 0xE8C25A / 0x8C1428 (26.1: no tint)

    // --- the structure type of the outer islands (the Mika-vestingen) -------------------------------------------------
    public static final DeferredHolder<StructureType<?>, StructureType<GuheindeEilandStructure>> EILAND_STRUCTURE = STRUCTURE_TYPES.register("guheinde_eiland",
            () -> () -> GuheindeEilandStructure.CODEC);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(OPPER_MIKA.get(), OpperMikaEntity.createAttributes().build());
            event.put(HONGERIGE_ENDERGUH.get(), HongerigeEnderguhEntity.createAttributes().build());
            event.put(MIKA_LARFJE.get(), MikaLarfjeEntity.createLarfjeAttributes().build());
        });
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> event.register(MIKA_LARFJE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        GuheindeEvents.register();
        GuheindeReis.register();
        GuheindeGevecht.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var item : List.of(OOG_VAN_VADSIG, MIKA_TRAAN, KNABBELKRISTAL, KNABBELKROON, GUHVLEUGELS, MIKA_LARFJE_SPAWN_EGG)) {
            output.accept(new ItemStack(item.get()));
        }
        for (var block : List.of(KAASKORST, KAASKORST_STENEN, GEBARSTEN_KAASKORST_STENEN, KAASKORST_STENEN_TRAP, KAASKORST_STENEN_PLAAT,
                KAASKORST_STENEN_MUUR, MIKA_STEEN, MIKA_STEEN_PILAAR, MIKA_STEEN_TRAP, MIKA_STEEN_PLAAT, KNABBELPORTAALFRAME, ENDERGUH_EI, OPPER_MIKATROFEE)) {
            output.accept(new ItemStack(block.get()));
        }
    }

    @Nullable
    public static NpcRole role() {
        return null;
    }

    private GuheindeFeature() {
    }
}
