package nl.juiced.guhs.feature.barbecuether;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoulSandBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;

/**
 * De Guhbarbecuether (slice 1 of 2.7.0): a Nether parody, 1:8 from the Guhmensie. Data-driven dimension
 * (data/guhs/dimension/barbecuether.json, tools/features/barbecuether.py) with five biomes, a sea of kaasfrituursaus
 * (its lava), a block set, the grillkool portal and its Aanmaakblokje, the barbecue pits (barbecueput) and the Grillguh.
 * <p>
 * The block ids here are fixed: the other 2.7 slices build on them.
 */
public final class BarbecuetherFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Guhs.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Guhs.MODID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);

    public static final ResourceKey<Level> BARBECUETHER = ResourceKey.create(Registries.DIMENSION, Guhs.id("barbecuether"));
    public static final GuhNpcEntity.Kind GRILLGUH_KIND = GuhNpcEntity.Kind.GRILLGUH;

    // --- the frying sauce -----------------------------------------------------------------------------------------------
    public static final DeferredHolder<FluidType, FluidType> KAASFRITUURSAUS_TYPE = FLUID_TYPES.register("kaasfrituursaus", Kaasfrituursaus::createType);
    public static final DeferredHolder<Fluid, FlowingFluid> KAASFRITUURSAUS = FLUIDS.register("kaasfrituursaus", Kaasfrituursaus.Source::new);
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_KAASFRITUURSAUS = FLUIDS.register("flowing_kaasfrituursaus", Kaasfrituursaus.Flowing::new);
    public static final DeferredBlock<LiquidBlock> KAASFRITUURSAUS_BLOCK = BLOCKS.registerBlock("kaasfrituursaus",
            p -> new Kaasfrituursaus.SausBlock(KAASFRITUURSAUS.get(), p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA).mapColor(MapColor.COLOR_ORANGE).lightLevel(s -> 15));
    public static final DeferredItem<BucketItem> KAASFRITUURSAUS_BUCKET = ITEMS.registerItem("kaasfrituursaus_bucket",
            p -> new BucketItem(KAASFRITUURSAUS.get(), p), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    // --- stone: houtskoolsteen (netherrack) and its bricks (nether bricks) ------------------------------------------------
    public static final DeferredBlock<Block> HOUTSKOOLSTEEN = BLOCKS.registerBlock("houtskoolsteen", BarbecueBlocks.Houtskoolsteen::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERRACK).mapColor(MapColor.COLOR_BLACK));
    public static final DeferredBlock<Block> HOUTSKOOLSTEEN_STENEN = BLOCKS.registerSimpleBlock("houtskoolsteen_stenen",
            BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICKS).mapColor(MapColor.COLOR_BLACK));
    public static final DeferredBlock<StairBlock> HOUTSKOOLSTEEN_STENEN_TRAP = BLOCKS.registerBlock("houtskoolsteen_stenen_trap",
            p -> new StairBlock(HOUTSKOOLSTEEN_STENEN.get().defaultBlockState(), p), BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICK_STAIRS).mapColor(MapColor.COLOR_BLACK));
    public static final DeferredBlock<SlabBlock> HOUTSKOOLSTEEN_STENEN_PLAAT = BLOCKS.registerBlock("houtskoolsteen_stenen_plaat", SlabBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICK_SLAB).mapColor(MapColor.COLOR_BLACK));
    public static final DeferredBlock<WallBlock> HOUTSKOOLSTEEN_STENEN_MUUR = BLOCKS.registerBlock("houtskoolsteen_stenen_muur", WallBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICK_WALL).mapColor(MapColor.COLOR_BLACK));
    /** The fence: a stone fence like the nether brick fence (not wooden: it doesn't burn and doesn't join wooden fences). */
    public static final DeferredBlock<FenceBlock> HOUTSKOOLSTEEN_STENEN_HEK = BLOCKS.registerBlock("houtskoolsteen_stenen_hek", FenceBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICK_FENCE).mapColor(MapColor.COLOR_BLACK));
    public static final DeferredBlock<Block> GEBARSTEN_HOUTSKOOLSTEEN_STENEN = BLOCKS.registerSimpleBlock("gebarsten_houtskoolsteen_stenen",
            BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_NETHER_BRICKS).mapColor(MapColor.COLOR_BLACK));
    /** Chiseled houtskoolsteen bricks with a guh face (for the pits and the other slices' buildings). */
    public static final DeferredBlock<Block> GEBEITELDE_HOUTSKOOLSTEEN_STENEN = BLOCKS.registerSimpleBlock("gebeitelde_houtskoolsteen_stenen",
            BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_NETHER_BRICKS).mapColor(MapColor.COLOR_BLACK));

    // --- grill iron: roosterijzer (blackstone / basalt) ------------------------------------------------------------------
    public static final DeferredBlock<Block> ROOSTERIJZER = BLOCKS.registerSimpleBlock("roosterijzer",
            BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE));
    public static final DeferredBlock<RotatedPillarBlock> ROOSTERIJZER_PILAAR = BLOCKS.registerBlock("roosterijzer_pilaar", RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.BASALT));
    public static final DeferredBlock<Block> GEPOLIJST_ROOSTERIJZER = BLOCKS.registerSimpleBlock("gepolijst_roosterijzer",
            BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE));
    public static final DeferredBlock<IronBarsBlock> ROOSTERIJZER_TRALIES = BLOCKS.registerBlock("roosterijzer_tralies", IronBarsBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).mapColor(MapColor.COLOR_BLACK));

    // --- glow, ash --------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<Block> GLOEIKOOL = BLOCKS.registerSimpleBlock("gloeikool",
            BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE).mapColor(MapColor.COLOR_ORANGE));
    /** Grey ash: slows you down like soul sand (and blue ash fire burns on it). */
    public static final DeferredBlock<SoulSandBlock> AS_BLOK = BLOCKS.registerBlock("as_blok", SoulSandBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_SAND).mapColor(MapColor.COLOR_LIGHT_GRAY));
    public static final DeferredBlock<Block> AS_AARDE = BLOCKS.registerSimpleBlock("as_aarde",
            BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_SOIL).mapColor(MapColor.COLOR_GRAY));

    // --- the forests: saté (crimson) and sausage (warped) ------------------------------------------------------------------
    public static final DeferredBlock<RotatedPillarBlock> SATE_STAM = BLOCKS.registerBlock("sate_stam", RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_STEM).mapColor(MapColor.WOOD));
    public static final DeferredBlock<Block> PINDASAUS_NYLIUM = BLOCKS.registerBlock("pindasaus_nylium", p -> new BarbecueBlocks.GrillNylium(false, p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_NYLIUM).mapColor(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<RotatedPillarBlock> WORST_STAM = BLOCKS.registerBlock("worst_stam", RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.WARPED_STEM).mapColor(MapColor.COLOR_BROWN));
    public static final DeferredBlock<Block> MOSTERD_NYLIUM = BLOCKS.registerBlock("mosterd_nylium", p -> new BarbecueBlocks.GrillNylium(true, p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WARPED_NYLIUM).mapColor(MapColor.COLOR_YELLOW));
    public static final DeferredBlock<Block> SATE_VLEES = BLOCKS.registerSimpleBlock("sate_vlees",
            BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_WART_BLOCK).mapColor(MapColor.COLOR_BROWN));
    public static final DeferredBlock<Block> MOSTERD_BLOK = BLOCKS.registerSimpleBlock("mosterd_blok",
            BlockBehaviour.Properties.ofFullCopy(Blocks.WARPED_WART_BLOCK).mapColor(MapColor.COLOR_YELLOW));
    public static final DeferredBlock<Block> UIENLICHT = BLOCKS.registerSimpleBlock("uienlicht",
            BlockBehaviour.Properties.ofFullCopy(Blocks.SHROOMLIGHT).mapColor(MapColor.SAND));
    public static final DeferredBlock<Block> PINDASCHEUTJES = BLOCKS.registerBlock("pindascheutjes", BarbecueBlocks.GrillPlant::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_ROOTS).mapColor(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<Block> MOSTERDSCHEUTJES = BLOCKS.registerBlock("mosterdscheutjes", BarbecueBlocks.GrillPlant::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.WARPED_ROOTS).mapColor(MapColor.COLOR_YELLOW));
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATE_GEKWEEKT = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("sate_spies_gekweekt"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> WORST_GEKWEEKT = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("braadworst_gekweekt"));
    /** Little saté skewer sprout: bone meal on pindasaus nylium grows a giant saté skewer. */
    public static final DeferredBlock<NetherFungusBlock> SATE_ZWAMMETJE = BLOCKS.registerBlock("sate_zwammetje",
            p -> new NetherFungusBlock(SATE_GEKWEEKT, PINDASAUS_NYLIUM.get(), p), BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_FUNGUS));
    /** Little sausage sprout: bone meal on mosterd nylium grows a giant sausage. */
    public static final DeferredBlock<NetherFungusBlock> WORST_ZWAMMETJE = BLOCKS.registerBlock("worst_zwammetje",
            p -> new NetherFungusBlock(WORST_GEKWEEKT, MOSTERD_NYLIUM.get(), p), BlockBehaviour.Properties.ofFullCopy(Blocks.WARPED_FUNGUS));
    public static final DeferredBlock<Block> SMEULKOOLTJES = BLOCKS.registerBlock("smeulkooltjes", BarbecueBlocks.Smeulkooltjes::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_ROOTS).mapColor(MapColor.COLOR_ORANGE).lightLevel(s -> 7));
    public static final DeferredBlock<Block> PINDASAUSPLASJE = BLOCKS.registerBlock("pindasausplasje", BarbecueBlocks.Pindasausplasje::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.1f).sound(SoundType.HONEY_BLOCK).speedFactor(0.4f)
                    .jumpFactor(0.5f).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> ROOKGAT = BLOCKS.registerBlock("rookgat", BarbecueBlocks.Rookgat::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE).noOcclusion().lightLevel(s -> 5));
    public static final DeferredBlock<RotatedPillarBlock> VERKOOLD_GUHBOT = BLOCKS.registerBlock("verkoold_guhbot", RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.BONE_BLOCK).mapColor(MapColor.COLOR_BLACK));
    /** Houtskoolsteen with kaasknabbels baked into it (the nether gold ore): drops kaasknabbels. */
    public static final DeferredBlock<DropExperienceBlock> HOUTSKOOLSTEEN_KAASKNABBELERTS = BLOCKS.registerBlock("houtskoolsteen_kaasknabbelerts",
            p -> new DropExperienceBlock(UniformInt.of(0, 1), p), BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_GOLD_ORE).mapColor(MapColor.COLOR_BLACK));

    // --- the portal -------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<Block> GRILLKOOL = BLOCKS.registerSimpleBlock("grillkool",
            BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM));
    public static final DeferredBlock<GrillPortalBlock> BARBECUETHER_PORTAAL = BLOCKS.registerBlock("barbecuether_portaal", GrillPortalBlock::new,
            BlockBehaviour.Properties.of().noCollission().strength(-1.0f).sound(SoundType.GLASS).lightLevel(s -> 12)
                    .pushReaction(PushReaction.BLOCK).noLootTable().mapColor(MapColor.COLOR_ORANGE));
    public static final DeferredHolder<PoiType, PoiType> PORTAAL_POI = POI_TYPES.register("barbecuether_portaal",
            () -> new PoiType(ImmutableSet.copyOf(BARBECUETHER_PORTAAL.get().getStateDefinition().getPossibleStates()), 0, 1));

    // --- items ------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<AanmaakblokjeItem> AANMAAKBLOKJE = ITEMS.registerItem("aanmaakblokje", AanmaakblokjeItem::new,
            new Item.Properties().durability(64));
    public static final DeferredItem<Item> GLOEIKOOLGRUIS = ITEMS.registerSimpleItem("gloeikoolgruis");
    public static final DeferredItem<GrillReceptItem> GRILLGUH_RECEPT = ITEMS.registerItem("grillguh_recept", GrillReceptItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> KAASKNABBELSATE = ITEMS.registerSimpleItem("kaasknabbelsate",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final DeferredItem<Item> GEGRILDE_KAASKNABBELSATE = ITEMS.registerSimpleItem("gegrilde_kaasknabbelsate",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build()));
    public static final DeferredItem<Item> GUHBRAADWORST = ITEMS.registerSimpleItem("guhbraadworst",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.7f)
                    .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0), 0.5f).build()));
    public static final DeferredItem<BlockItem> GRILLKOOL_ITEM;

    /** Every block that has an item (in creative-tab order). */
    public static final List<DeferredBlock<?>> BLOCK_ITEMS = List.of(HOUTSKOOLSTEEN, HOUTSKOOLSTEEN_STENEN, HOUTSKOOLSTEEN_STENEN_TRAP,
            HOUTSKOOLSTEEN_STENEN_PLAAT, HOUTSKOOLSTEEN_STENEN_MUUR, HOUTSKOOLSTEEN_STENEN_HEK, GEBARSTEN_HOUTSKOOLSTEEN_STENEN,
            GEBEITELDE_HOUTSKOOLSTEEN_STENEN, ROOSTERIJZER, ROOSTERIJZER_PILAAR, GEPOLIJST_ROOSTERIJZER, ROOSTERIJZER_TRALIES, GLOEIKOOL,
            AS_BLOK, AS_AARDE, SATE_STAM, PINDASAUS_NYLIUM, WORST_STAM, MOSTERD_NYLIUM, SATE_VLEES, MOSTERD_BLOK, UIENLICHT, PINDASCHEUTJES,
            MOSTERDSCHEUTJES, SATE_ZWAMMETJE, WORST_ZWAMMETJE, SMEULKOOLTJES, PINDASAUSPLASJE, ROOKGAT, VERKOOLD_GUHBOT,
            HOUTSKOOLSTEEN_KAASKNABBELERTS, GRILLKOOL);

    static {
        DeferredItem<BlockItem> grillkool = null;
        for (DeferredBlock<?> block : BLOCK_ITEMS) {
            DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(block);
            if (block == GRILLKOOL) {
                grillkool = item;
            }
        }
        GRILLKOOL_ITEM = grillkool;
    }

    // --- worldgen -----------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SATE_SPIES = FEATURES.register("sate_spies", BarbecueWorldgen.SateSpies::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> BRAADWORST = FEATURES.register("braadworst", BarbecueWorldgen.Braadworst::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ROOSTERPILAREN = FEATURES.register("roosterpilaren", BarbecueWorldgen.Roosterpilaren::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GLOEIKOOL_KLOMP = FEATURES.register("gloeikool_klomp", BarbecueWorldgen.GloeikoolKlomp::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GUH_FOSSIEL = FEATURES.register("verkoold_guhfossiel", BarbecueWorldgen.GuhFossiel::new);
    public static final DeferredHolder<StructureType<?>, StructureType<BarbecuePutStructure>> BARBECUEPUT_TYPE =
            STRUCTURE_TYPES.register("barbecueput", () -> () -> BarbecuePutStructure.CODEC);

    private static final Grillguh GRILLGUH = new Grillguh();

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        POI_TYPES.register(modBus);
        FEATURES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(Kaasfrituursaus::registerInteractions));
        NeoForge.EVENT_BUS.addListener(BarbecuetherEvents::onUseBlock);
        NeoForge.EVENT_BUS.addListener(BarbecuetherEvents::onDrops);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> BarbecuetherEvents.TEST_CAMPS.clear());
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<?> block : BLOCK_ITEMS) {
            output.accept(new ItemStack(block.get()));
        }
        for (var item : List.of(KAASFRITUURSAUS_BUCKET, AANMAAKBLOKJE, GLOEIKOOLGRUIS, GRILLGUH_RECEPT, KAASKNABBELSATE,
                GEGRILDE_KAASKNABBELSATE, GUHBRAADWORST)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    @Nullable
    public static NpcRole role() {
        return GRILLGUH;
    }

    private BarbecuetherFeature() {
    }
}
