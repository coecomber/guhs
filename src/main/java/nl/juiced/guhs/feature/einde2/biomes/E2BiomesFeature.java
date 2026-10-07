package nl.juiced.guhs.feature.einde2.biomes;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * einde2, slice biomes: the six biomes of the outer islands of the Guheinde. Two common ones (Gatenkaasheuvels,
 * Korstwoestijn) and four rare, small ones (Kruimelwolken, Schimmelkaasvlakte, Kristalkaasklippen, Knabbelkoorbos), laid out
 * by {@link GuheindeBiomeSource}; their blocks and plants ({@link E2BiomesBlokken}), their worldgen features
 * ({@link E2BiomesWorldgen}), the kristalkaas (the ore of the outer islands) and the knabbelkoor chain: the plant
 * ({@link Knabbelkoor}), its fruit that flashes you a few blocks away ({@link KnabbelkoorvruchtItem}), the popped fruit, the
 * building blocks made of it and the lichtstaafje.
 * <p>
 * The terrain itself is untouched: the biomes only change the surface, and everything else comes from features. The
 * main island keeps the biome guhs:guheinde. Resources: tools/features/einde2_biomes.py (textures: einde2_biomes_tex.py).
 * <p>
 * The block and item fields are the ids the other einde2 slices build with (build contract 3.4): do not rename them.
 */
public final class E2BiomesFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister.create(Registries.BIOME_SOURCE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    private static BlockBehaviour.Properties korst(MapColor kleur) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).mapColor(kleur);
    }

    private static BlockBehaviour.Properties stenen(MapColor kleur) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE_BRICKS).mapColor(kleur);
    }

    private static BlockBehaviour.Properties plantje(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).noCollision().instabreak().sound(SoundType.GRASS).replaceable()
                .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY);
    }

    // --- Gatenkaasheuvels: korstgras and kruimelrots ------------------------------------------------------------------------
    public static final DeferredBlock<Block> KORSTGRAS = BLOCKS.registerBlock("korstgras", p -> new E2BiomesBlokken.Plantje(12, 10, p),
            () -> plantje(MapColor.COLOR_YELLOW));
    public static final DeferredBlock<Block> KRUIMELROTS = BLOCKS.registerSimpleBlock("kruimelrots",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE).mapColor(MapColor.TERRACOTTA_ORANGE));
    public static final DeferredBlock<Block> KRUIMELROTS_STENEN = BLOCKS.registerSimpleBlock("kruimelrots_stenen",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.TERRACOTTA_ORANGE));
    public static final DeferredBlock<StairBlock> KRUIMELROTS_STENEN_TRAP = trap("kruimelrots_stenen_trap", KRUIMELROTS_STENEN);
    public static final DeferredBlock<SlabBlock> KRUIMELROTS_STENEN_PLAAT = plaat("kruimelrots_stenen_plaat", KRUIMELROTS_STENEN);
    public static final DeferredBlock<WallBlock> KRUIMELROTS_STENEN_MUUR = muur("kruimelrots_stenen_muur", KRUIMELROTS_STENEN);

    // --- Korstwoestijn: cracked crust, crumb sand and its dunes -----------------------------------------------------------
    public static final DeferredBlock<Block> GEBARSTEN_KORST = BLOCKS.registerSimpleBlock("gebarsten_korst", () -> korst(MapColor.TERRACOTTA_YELLOW));
    /** Falls like sand; you sink in a little and walk slowly (harmless). */
    public static final DeferredBlock<Block> KRUIMELZAND = BLOCKS.registerBlock("kruimelzand",
            p -> new E2BiomesBlokken.Kruimelzand(new ColorRGBA(0xFFE6C47C), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.SAND).speedFactor(0.6f));
    /** One to eight layers of crumb sand: the dunes. */
    public static final DeferredBlock<Block> KRUIMELZANDLAAG = BLOCKS.registerBlock("kruimelzandlaag", E2BiomesBlokken.Kruimelzandlaag::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.SNARE).strength(0.3f).sound(SoundType.SAND)
                    .replaceable().forceSolidOff().pushReaction(PushReaction.DESTROY)
                    .isViewBlocking((s, l, pos) -> s.getValue(E2BiomesBlokken.Kruimelzandlaag.LAYERS) >= 8));
    public static final DeferredBlock<Block> DORRE_KORSTSTRUIK = BLOCKS.registerBlock("dorre_korststruik", p -> new E2BiomesBlokken.Plantje(12, 13, p),
            () -> plantje(MapColor.TERRACOTTA_BROWN));

    // --- Kruimelwolken: clouds ---------------------------------------------------------------------------------------------
    /** You sink slowly through it (no fall damage); it carries whoever {@link Wolken#draagt} names. */
    public static final DeferredBlock<Block> KRUIMELWOLK = BLOCKS.registerBlock("kruimelwolk", E2BiomesBlokken.Kruimelwolk::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.2f).sound(SoundType.WOOL).noOcclusion().dynamicShape()
                    .isRedstoneConductor((s, l, pos) -> false).isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)
                    .isValidSpawn((s, l, pos, e) -> false).pushReaction(PushReaction.DESTROY));
    /** Pressed cloud: solid, a building block you never hurt yourself on. */
    public static final DeferredBlock<Block> DICHTE_KRUIMELWOLK = BLOCKS.registerBlock("dichte_kruimelwolk", E2BiomesBlokken.DichteWolk::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6f).sound(SoundType.WOOL).instrument(NoteBlockInstrument.GUITAR));

    // --- Schimmelkaasvlakte: blue cheese and its glowing mushrooms ----------------------------------------------------------
    public static final DeferredBlock<Block> SCHIMMELKAAS = BLOCKS.registerSimpleBlock("schimmelkaas", () -> korst(MapColor.QUARTZ));
    public static final DeferredBlock<Block> SCHIMMELKAAS_STENEN = BLOCKS.registerSimpleBlock("schimmelkaas_stenen", () -> stenen(MapColor.QUARTZ));
    public static final DeferredBlock<StairBlock> SCHIMMELKAAS_STENEN_TRAP = trap("schimmelkaas_stenen_trap", SCHIMMELKAAS_STENEN);
    public static final DeferredBlock<SlabBlock> SCHIMMELKAAS_STENEN_PLAAT = plaat("schimmelkaas_stenen_plaat", SCHIMMELKAAS_STENEN);
    public static final DeferredBlock<WallBlock> SCHIMMELKAAS_STENEN_MUUR = muur("schimmelkaas_stenen_muur", SCHIMMELKAAS_STENEN);
    public static final DeferredBlock<Block> SCHIMMELPADDENSTOEL = BLOCKS.registerBlock("schimmelpaddenstoel", p -> new E2BiomesBlokken.Plantje(8, 9, p),
            () -> plantje(MapColor.COLOR_CYAN).sound(SoundType.FUNGUS).lightLevel(s -> 9));
    public static final DeferredBlock<Block> SCHIMMELHOED = BLOCKS.registerSimpleBlock("schimmelhoed",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 10));
    public static final DeferredBlock<RotatedPillarBlock> SCHIMMELSTEEL = BLOCKS.registerBlock("schimmelsteel", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MUSHROOM_STEM).mapColor(MapColor.QUARTZ));

    // --- Kristalkaasklippen: old cliff cheese and the kristalkaas ----------------------------------------------------------
    public static final DeferredBlock<Block> KLIPKAAS = BLOCKS.registerSimpleBlock("klipkaas", () -> korst(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<Block> KLIPKAAS_STENEN = BLOCKS.registerSimpleBlock("klipkaas_stenen", () -> stenen(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<StairBlock> KLIPKAAS_STENEN_TRAP = trap("klipkaas_stenen_trap", KLIPKAAS_STENEN);
    public static final DeferredBlock<SlabBlock> KLIPKAAS_STENEN_PLAAT = plaat("klipkaas_stenen_plaat", KLIPKAAS_STENEN);
    public static final DeferredBlock<WallBlock> KLIPKAAS_STENEN_MUUR = muur("klipkaas_stenen_muur", KLIPKAAS_STENEN);
    /** Klipkaas with kristalkaas grown into it: the ore of the outer islands (drops kristalkaas; fortune counts). */
    public static final DeferredBlock<DropExperienceBlock> KRISTALKAAS_ERTS = BLOCKS.registerBlock("kristalkaas_erts",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
            () -> korst(MapColor.COLOR_ORANGE).strength(4.0f, 9.0f).lightLevel(s -> 4));
    /** A golden, glowing cluster on the cliffs (grows on any side of a block). */
    public static final DeferredBlock<Block> KRISTALKAAS_KRISTAL = BLOCKS.registerBlock("kristalkaas_kristal", p -> new E2BiomesBlokken.Kristal(p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).mapColor(MapColor.GOLD).lightLevel(s -> 12));
    public static final DeferredBlock<Block> KRISTALKAAS_BLOK = BLOCKS.registerSimpleBlock("kristalkaas_blok",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.GOLD).lightLevel(s -> 10));

    // --- Knabbelkoorbos: the singing knabbelkoor and what you build of it ----------------------------------------------------
    public static final DeferredBlock<Block> KOORKORST = BLOCKS.registerSimpleBlock("koorkorst", () -> korst(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<Block> KNABBELKOORPLANT = BLOCKS.registerBlock("knabbelkoorplant", Knabbelkoor.Plant::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHORUS_PLANT).mapColor(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<Block> KNABBELKOORBLOEM = BLOCKS.registerBlock("knabbelkoorbloem", Knabbelkoor.Bloem::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHORUS_FLOWER).mapColor(MapColor.COLOR_MAGENTA).lightLevel(s -> 6));
    public static final DeferredBlock<Block> KNABBELKOORBLOK = BLOCKS.registerSimpleBlock("knabbelkoorblok",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PURPUR_BLOCK).mapColor(MapColor.COLOR_MAGENTA));
    public static final DeferredBlock<RotatedPillarBlock> KNABBELKOORPILAAR = BLOCKS.registerBlock("knabbelkoorpilaar", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PURPUR_PILLAR).mapColor(MapColor.COLOR_MAGENTA));
    public static final DeferredBlock<StairBlock> KNABBELKOORBLOK_TRAP = trap("knabbelkoorblok_trap", KNABBELKOORBLOK);
    public static final DeferredBlock<SlabBlock> KNABBELKOORBLOK_PLAAT = plaat("knabbelkoorblok_plaat", KNABBELKOORBLOK);
    /** The end rod of the Guheinde: a popped fruit on a stick of guhkristal. */
    public static final DeferredBlock<Block> LICHTSTAAFJE = BLOCKS.registerBlock("lichtstaafje", p -> new E2BiomesBlokken.Lichtstaafje(p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.END_ROD).mapColor(MapColor.COLOR_PINK).lightLevel(s -> 14));

    // --- items ---------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<Item> KRISTALKAAS = ITEMS.registerItem("kristalkaas", Item::new, () -> new Item.Properties());
    public static final DeferredItem<Item> KRISTALKAAS_BROKJE = ITEMS.registerItem("kristalkaas_brokje", Item::new, () -> new Item.Properties());
    /** Eating it flashes you a few blocks away; now and then a Kaasmijt pops out. */
    public static final DeferredItem<Item> KNABBELKOORVRUCHT = ITEMS.registerItem("knabbelkoorvrucht", KnabbelkoorvruchtItem::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).alwaysEdible().build()).useCooldown(1.0f));
    public static final DeferredItem<Item> GEPOFTE_KNABBELKOORVRUCHT = ITEMS.registerItem("gepofte_knabbelkoorvrucht", Item::new, () -> new Item.Properties());

    /** Every block (each has an item), in creative-tab order. */
    public static final List<DeferredBlock<? extends Block>> BLOCK_ITEMS = List.of(KORSTGRAS, KRUIMELROTS, KRUIMELROTS_STENEN, KRUIMELROTS_STENEN_TRAP,
            KRUIMELROTS_STENEN_PLAAT, KRUIMELROTS_STENEN_MUUR, GEBARSTEN_KORST, KRUIMELZAND, KRUIMELZANDLAAG, DORRE_KORSTSTRUIK, KRUIMELWOLK,
            DICHTE_KRUIMELWOLK, SCHIMMELKAAS, SCHIMMELKAAS_STENEN, SCHIMMELKAAS_STENEN_TRAP, SCHIMMELKAAS_STENEN_PLAAT, SCHIMMELKAAS_STENEN_MUUR,
            SCHIMMELPADDENSTOEL, SCHIMMELHOED, SCHIMMELSTEEL, KLIPKAAS, KLIPKAAS_STENEN, KLIPKAAS_STENEN_TRAP, KLIPKAAS_STENEN_PLAAT,
            KLIPKAAS_STENEN_MUUR, KRISTALKAAS_ERTS, KRISTALKAAS_KRISTAL, KRISTALKAAS_BLOK, KOORKORST, KNABBELKOORPLANT, KNABBELKOORBLOEM,
            KNABBELKOORBLOK, KNABBELKOORPILAAR, KNABBELKOORBLOK_TRAP, KNABBELKOORBLOK_PLAAT, LICHTSTAAFJE);

    static {
        for (DeferredBlock<? extends Block> block : BLOCK_ITEMS) {
            ITEMS.registerSimpleBlockItem(block);
        }
    }

    // --- worldgen (configured and placed in data/guhs/worldgen as einde2_biomes_*, see the python module) ----------------------
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> KAASGAT = FEATURES.register("einde2_kaasgat", E2BiomesWorldgen.Kaasgat::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SAUSMEERTJE = FEATURES.register("einde2_sausmeertje", E2BiomesWorldgen.Sausmeertje::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> KRUIMELROTS_KEI = FEATURES.register("einde2_kruimelrots", E2BiomesWorldgen.Kei::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> DUIN = FEATURES.register("einde2_duin", E2BiomesWorldgen.Duin::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> OASE = FEATURES.register("einde2_oase", E2BiomesWorldgen.Oase::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> KRUIMELEILANDJE = FEATURES.register("einde2_kruimeleilandje", E2BiomesWorldgen.Kruimeleilandje::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> WOLK = FEATURES.register("einde2_wolk", E2BiomesWorldgen.Wolk::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SCHIMMELREUS = FEATURES.register("einde2_schimmelreus", E2BiomesWorldgen.Schimmelreus::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> VIJVER = FEATURES.register("einde2_vijver", E2BiomesWorldgen.Vijver::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> KRISTALSPITS = FEATURES.register("einde2_kristalspits", E2BiomesWorldgen.Kristalspits::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> KNABBELKOOR = FEATURES.register("einde2_knabbelkoor", () -> new E2BiomesWorldgen.KnabbelkoorBoom(false));
    /** The same plant where a knabbelkoorbloem stands (bone meal on a young flower, and for whoever wants to plant one from data). */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> KNABBELKOOR_GEKWEEKT = FEATURES.register("einde2_knabbelkoor_gekweekt", () -> new E2BiomesWorldgen.KnabbelkoorBoom(true));
    public static final ResourceKey<ConfiguredFeature<?, ?>> GEKWEEKT = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("einde2_biomes_knabbelkoor_gekweekt"));

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<GuheindeBiomeSource>> BIOME_SOURCE =
            BIOME_SOURCES.register("guheinde_biomes", () -> GuheindeBiomeSource.CODEC);

    // --- sounds --------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> KNABBELKOOR_ZING = sound("einde2_biomes.knabbelkoor_zing");
    public static final DeferredHolder<SoundEvent, SoundEvent> VRUCHT_FLITS = sound("einde2_biomes.vrucht_flits");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOLK_PLOF = sound("einde2_biomes.wolk_plof");

    private static DeferredBlock<StairBlock> trap(String id, DeferredBlock<Block> van) {
        return BLOCKS.registerBlock(id, p -> new StairBlock(van.get().defaultBlockState(), p), () -> BlockBehaviour.Properties.ofFullCopy(van.get()));
    }

    private static DeferredBlock<SlabBlock> plaat(String id, DeferredBlock<Block> van) {
        return BLOCKS.registerBlock(id, SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(van.get()));
    }

    private static DeferredBlock<WallBlock> muur(String id, DeferredBlock<Block> van) {
        return BLOCKS.registerBlock(id, WallBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(van.get()).forceSolidOn());
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        BIOME_SOURCES.register(modBus);
        SOUNDS.register(modBus);
        NeoForge.EVENT_BUS.addListener(E2BiomesCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<? extends Block> block : BLOCK_ITEMS) {
            output.accept(new ItemStack(block.get()));
        }
        for (DeferredItem<Item> item : List.of(KRISTALKAAS_BROKJE, KRISTALKAAS, KNABBELKOORVRUCHT, GEPOFTE_KNABBELKOORVRUCHT)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private E2BiomesFeature() {
    }
}
