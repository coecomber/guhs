package nl.juiced.guhs.feature.spiesburcht;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.registry.ModEntities;

import net.minecraft.resources.Identifier;
/**
 * De Spiesburcht (slice 2 of 2.7.0): the creatures, buildings, brewing and the boss of the Guhbarbecuether.
 * <ul>
 *   <li>{@link RookguhEntity}: a sad little ghast with a guh face (3/4 of a ghast, always peaceful): feed it kaasknabbels until
 *       it's VAHOEG and it floats home; every player keeps count of the Rookguhs they saved ({@link SpiesburchtStats}).</li>
 *   <li>{@link NetherMikaRuil}: the greedy Nether-Mikas trade vahoege vads ingots for loot (piglin bartering), and leave
 *       you alone while you wear vads (unless you hit them).</li>
 *   <li>{@link VonkMikaEntity} (blaze, drops grillspiesen), {@link KnekelMikaEntity} (wither skeleton, now and then a
 *       verkoolde mikakop), {@link AangebrandeMikaEntity} (the wither: a T of ash with three heads, in any dimension).</li>
 *   <li>The structures guhs:spiesburcht (fortress) and guhs:mika_grillpaleis (bastion), {@link BurchtStructure}.</li>
 *   <li>The {@link GuhbrouwketelBlock} (brewing with grillspiespoeder: the Guhdrankjes) and the {@link KnabbelbakenBlock}
 *       (a beacon for you and your guhs), and the Asguh variant ({@link SpiesburchtEvents}).</li>
 * </ul>
 * Resources: tools/features/spiesburcht*.py.
 */
public final class SpiesburchtFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Guhs.MODID);

    public static final ResourceKey<Biome> ASDAL = ResourceKey.create(Registries.BIOME, Guhs.id("asdal"));
    /** The blocks a Knabbelbaken's pyramid can be made of (vads, kaas and knabbel blocks; other slices add theirs). */
    public static final TagKey<Block> BAKEN_BASIS = TagKey.create(Registries.BLOCK, Guhs.id("knabbelbaken_basis"));
    /** The T under the three verkoolde mikakoppen (as_blok). */
    public static final TagKey<Block> AANGEBRAND_BASIS = TagKey.create(Registries.BLOCK, Guhs.id("aangebrande_mika_basis"));

    // --- creatures -------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<RookguhEntity>> ROOKGUH = ENTITY_TYPES.register("rookguh",
            () -> EntityType.Builder.of(RookguhEntity::new, MobCategory.CREATURE).sized(3.0f, 3.0f).eyeHeight(2.2f).fireImmune()
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("rookguh"))));
    public static final DeferredHolder<EntityType<?>, EntityType<VonkMikaEntity>> VONK_MIKA = ENTITY_TYPES.register("vonk_mika",
            () -> EntityType.Builder.of(VonkMikaEntity::new, MobCategory.MONSTER).sized(0.8f, 1.8f).eyeHeight(1.35f).fireImmune().notInPeaceful()
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vonk_mika"))));
    public static final DeferredHolder<EntityType<?>, EntityType<KnekelMikaEntity>> KNEKEL_MIKA = ENTITY_TYPES.register("knekel_mika",
            () -> EntityType.Builder.of(KnekelMikaEntity::new, MobCategory.MONSTER).sized(0.7f, 2.4f).eyeHeight(2.1f).fireImmune().notInPeaceful()
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("knekel_mika"))));
    public static final DeferredHolder<EntityType<?>, EntityType<AangebrandeMikaEntity>> AANGEBRANDE_MIKA = ENTITY_TYPES.register("aangebrande_mika",
            () -> EntityType.Builder.of(AangebrandeMikaEntity::new, MobCategory.MONSTER).sized(1.6f, 3.0f).eyeHeight(2.5f).fireImmune().notInPeaceful()
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("aangebrande_mika"))));
    /** The Vonk-Mika's glowing ember. */
    public static final DeferredHolder<EntityType<?>, EntityType<GloeiendKooltje>> GLOEIEND_KOOLTJE = ENTITY_TYPES.register("gloeiend_kooltje",
            () -> EntityType.Builder.<GloeiendKooltje>of(GloeiendKooltje::new, MobCategory.MISC).sized(0.3125f, 0.3125f)
                    .clientTrackingRange(4).updateInterval(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("gloeiend_kooltje"))));
    /** The Aangebrande Mika's big burning coal (slow: you can dodge it). */
    public static final DeferredHolder<EntityType<?>, EntityType<GloeiendKooltje>> BRANDEND_KOOLTJE = ENTITY_TYPES.register("brandend_kooltje",
            () -> EntityType.Builder.<GloeiendKooltje>of(GloeiendKooltje::new, MobCategory.MISC).sized(0.6f, 0.6f)
                    .clientTrackingRange(6).updateInterval(5).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("brandend_kooltje"))));

    // --- blocks ----------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<MikakopBlock> VERKOOLDE_MIKAKOP = BLOCKS.registerBlock("verkoolde_mikakop", MikakopBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.BONE_BLOCK)
                    .instrument(NoteBlockInstrument.WITHER_SKELETON).pushReaction(PushReaction.DESTROY).noOcclusion());
    public static final DeferredBlock<MikakopBlock.Wall> VERKOOLDE_MIKAKOP_MUUR = BLOCKS.registerBlock("verkoolde_mikakop_muur", MikakopBlock.Wall::new,
            () -> BlockBehaviour.Properties.of().overrideDescription("block.guhs.verkoolde_mikakop").mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.BONE_BLOCK)
                    .instrument(NoteBlockInstrument.WITHER_SKELETON).pushReaction(PushReaction.DESTROY).noOcclusion());
    public static final DeferredBlock<GuhbrouwketelBlock> GUHBROUWKETEL = BLOCKS.registerBlock("guhbrouwketel", GuhbrouwketelBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.5f).requiresCorrectToolForDrops().sound(SoundType.METAL)
                    .noOcclusion().lightLevel(s -> s.getValue(GuhbrouwketelBlock.LIT) ? 11 : 1));
    public static final DeferredBlock<KnabbelbakenBlock> KNABBELBAKEN = BLOCKS.registerBlock("knabbelbaken", KnabbelbakenBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0f).sound(SoundType.GLASS).lightLevel(s -> 15)
                    .noOcclusion().isRedstoneConductor((s, l, p) -> false).instrument(NoteBlockInstrument.HAT));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhbrouwketelBlockEntity>> GUHBROUWKETEL_BE =
            BLOCK_ENTITY_TYPES.register("guhbrouwketel", () -> new BlockEntityType<>(GuhbrouwketelBlockEntity::new, GUHBROUWKETEL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnabbelbakenBlockEntity>> KNABBELBAKEN_BE =
            BLOCK_ENTITY_TYPES.register("knabbelbaken", () -> new BlockEntityType<>(KnabbelbakenBlockEntity::new, KNABBELBAKEN.get()));

    // --- items -----------------------------------------------------------------------------------------------------------
    public static final DeferredItem<StandingAndWallBlockItem> VERKOOLDE_MIKAKOP_ITEM = ITEMS.registerItem("verkoolde_mikakop",
            p -> new StandingAndWallBlockItem(VERKOOLDE_MIKAKOP.get(), VERKOOLDE_MIKAKOP_MUUR.get(), Direction.DOWN, p),
            () -> new Item.Properties().rarity(Rarity.UNCOMMON).equippableUnswappable(net.minecraft.world.entity.EquipmentSlot.HEAD));
    public static final DeferredItem<BlockItem> GUHBROUWKETEL_ITEM = ITEMS.registerSimpleBlockItem(GUHBROUWKETEL);
    public static final DeferredItem<BlockItem> KNABBELBAKEN_ITEM = ITEMS.registerSimpleBlockItem(KNABBELBAKEN, () -> new Item.Properties().rarity(Rarity.RARE));
    /** The Vonk-Mika's skewer (the blaze rod). */
    public static final DeferredItem<Item> GRILLSPIES = ITEMS.registerSimpleItem("grillspies");
    /** Ground skewer (the blaze powder): the fuel of the Guhbrouwketel. */
    public static final DeferredItem<Item> GRILLSPIESPOEDER = ITEMS.registerSimpleItem("grillspiespoeder");
    /** The Aangebrande Mika's star (the nether star): the heart of a Knabbelbaken. */
    public static final DeferredItem<Item> GLOEISTER = ITEMS.registerSimpleItem("gloeister",
            () -> new Item.Properties().rarity(Rarity.EPIC).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    /** (What a flying glowing ember looks like.) */
    public static final DeferredItem<Item> GLOEIEND_KOOLTJE_ITEM = ITEMS.registerSimpleItem("gloeiend_kooltje");
    public static final DeferredItem<GuhdrankjeItem> DRANKJE_VAN_VAHOEGHEID = ITEMS.registerItem("drankje_van_vahoegheid",
            p -> new GuhdrankjeItem(Brouwsel.VAHOEGHEID, p), () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<GuhdrankjeItem> ROOKLOOPDRANKJE = ITEMS.registerItem("rookloopdrankje",
            p -> new GuhdrankjeItem(Brouwsel.ROOKLOOP, p), () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<GuhdrankjeItem> SLUIPKNABBELDRANKJE = ITEMS.registerItem("sluipknabbeldrankje",
            p -> new GuhdrankjeItem(Brouwsel.SLUIPKNABBEL, p), () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<GuhdrankjeItem> GUHSPRONGDRANKJE = ITEMS.registerItem("guhsprongdrankje",
            p -> new GuhdrankjeItem(Brouwsel.GUHSPRONG, p), () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> ROOKGUH_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "rookguh_spawn_egg", ROOKGUH);
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> VONK_MIKA_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "vonk_mika_spawn_egg", VONK_MIKA);
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> KNEKEL_MIKA_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "knekel_mika_spawn_egg", KNEKEL_MIKA);
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> AANGEBRANDE_MIKA_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "aangebrande_mika_spawn_egg", AANGEBRANDE_MIKA);

    public static final List<DeferredItem<? extends Item>> CREATIVE = List.of(VERKOOLDE_MIKAKOP_ITEM, GUHBROUWKETEL_ITEM, KNABBELBAKEN_ITEM,
            GRILLSPIES, GRILLSPIESPOEDER, GLOEISTER, DRANKJE_VAN_VAHOEGHEID, ROOKLOOPDRANKJE, SLUIPKNABBELDRANKJE, GUHSPRONGDRANKJE,
            ROOKGUH_SPAWN_EGG, VONK_MIKA_SPAWN_EGG, KNEKEL_MIKA_SPAWN_EGG, AANGEBRANDE_MIKA_SPAWN_EGG);

    // --- the buildings -----------------------------------------------------------------------------------------------------
    public static final DeferredHolder<StructureType<?>, StructureType<BurchtStructure>> BURCHT_TYPE =
            STRUCTURE_TYPES.register("burcht", () -> () -> BurchtStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BURCHT_PIECE =
            STRUCTURE_PIECES.register("burcht_stuk", () -> (StructurePieceType.StructureTemplateType) BurchtStructure.Piece::new);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECES.register(modBus);
        modBus.addListener(SpiesburchtFeature::attributes);
        modBus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, SpiesburchtFeature::spawnPlacements);   // (after the guh's own)
        NetherMikaRuil.register();
        SpiesburchtEvents.register();
        // 2.10.1: the Rookguh's own Guhdex page (it shows how many Rookguhs you saved, see SpiesburchtStats)
        nl.juiced.guhs.quest.GuhDex.creaturePage(nl.juiced.guhs.entity.GuhVariant.ROOKGUH, ROOKGUH);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(ROOKGUH.get(), RookguhEntity.createAttributes().build());
        event.put(VONK_MIKA.get(), VonkMikaEntity.createAttributes().build());
        event.put(KNEKEL_MIKA.get(), KnekelMikaEntity.createAttributes().build());
        event.put(AANGEBRANDE_MIKA.get(), AangebrandeMikaEntity.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ROOKGUH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                RookguhEntity::checkRookguhSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(VONK_MIKA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(KNEKEL_MIKA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(AANGEBRANDE_MIKA.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> reason != net.minecraft.world.entity.EntitySpawnReason.NATURAL
                        && reason != net.minecraft.world.entity.EntitySpawnReason.CHUNK_GENERATION, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // guhs in the dark ash valley: the Asguh (a guh normally needs light to be born)
        event.register(ModEntities.GUH.get(), SpiesburchtEvents::asguhMaySpawn, RegisterSpawnPlacementsEvent.Operation.OR);
    }

    public static void payloads(PayloadRegistrar registrar) {
        SpiesburchtStats.payloads(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<? extends Item> item : CREATIVE) {
            output.accept(new ItemStack(item.get()));
        }
    }

    @Nullable
    public static NpcRole role() {
        return null;
    }

    private SpiesburchtFeature() {
    }
}
