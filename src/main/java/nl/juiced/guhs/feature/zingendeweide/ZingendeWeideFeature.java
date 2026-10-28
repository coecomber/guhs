package nl.juiced.guhs.feature.zingendeweide;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.vadswoud.VadshoutBlocks;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * 1.8.0 (Guhdromenland): De Zingende Weide, the rare Blub biome of the Guhmension (ontwerp-guhs180-dromen.md, renders
 * dromen-ontwerp/weide/). Resources: tools/features/zingendeweide.py (blocks, the biome and its region, surface, features
 * and structures).
 * <ul>
 *   <li>Salmon clay with striped cliffs, seagreen grass and stone, lief pink and light pink growth, singing flowers,
 *       shellsand, pastel crystals; the wolkjeshout wood set (white and pink leaves, the sleepy guh face, signs and
 *       hanging signs) with its trees ({@link WeideFeatures}).</li>
 *   <li>Bubbelsap ({@link Bubbelsap}): a shiny rainbow fluid; standing in it makes you bubbly. Blubs love bathing in it.</li>
 *   <li>The five parts of the meadow ({@link WeideDeel}): the wild Blubs (they only spawn here) follow its kind.</li>
 *   <li>Three structures ({@link WeideBouw}): the Koorkom (wild Blubs sing their lullaby there at dusk:
 *       {@link KoorkomPodium}), the Bubbelsap-poeltjes with their bath hut and Badmeester Bubbels ({@link BadGuh}),
 *       and the Guhwand.</li>
 *   <li>The sky: trillertjes by day, rainbow light pillars at night (client/WeideLucht).</li>
 * </ul>
 */
public final class ZingendeWeideFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Guhs.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Guhs.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Guhs.MODID);

    public static final ResourceKey<Biome> ZINGENDE_WEIDE = ResourceKey.create(Registries.BIOME, Guhs.id("zingende_weide"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> BOOM = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("wolkjeshout_boom"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> REUZENBOOM = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("wolkjeshout_reuzenboom"));

    public static final BlockSetType WOLKJESHOUT_SET = BlockSetType.register(new BlockSetType("guhs_wolkjeshout"));
    /** (the signs' textures: assets/guhs/textures/entity/signs/wolkjeshout.png and .../signs/hanging/wolkjeshout.png) */
    public static final WoodType WOLKJESHOUT_WOOD = WoodType.register(new WoodType("guhs:wolkjeshout", WOLKJESHOUT_SET));
    /** One sapling: a little wolkjesboom; four in a square: a giant one. */
    public static final TreeGrower GROWER = new TreeGrower("guhs:wolkjeshout", Optional.of(REUZENBOOM), Optional.of(BOOM), Optional.empty());

    private static BlockBehaviour.Properties hout() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PLANKS).mapColor(MapColor.QUARTZ);
    }

    private static BlockBehaviour.Properties klei() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.TERRACOTTA_PINK);
    }

    private static BlockBehaviour.Properties steen() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_CYAN);
    }

    // --- the ground ----------------------------------------------------------------------------------------------------
    public static final DeferredBlock<WeideBlokken.Gras> ZALMKLEI_GRAS = BLOCKS.registerBlock("zalmklei_gras", WeideBlokken.Gras::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<WeideBlokken.Gras> ZEEGROEN_GRAS = BLOCKS.registerBlock("zeegroen_gras", WeideBlokken.Gras::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).mapColor(MapColor.COLOR_CYAN));
    public static final DeferredBlock<Block> ZALMKLEI = BLOCKS.registerSimpleBlock("zalmklei", ZingendeWeideFeature::klei);
    public static final DeferredBlock<Block> ZALMKLEI_STREEP_A = BLOCKS.registerSimpleBlock("zalmklei_streep_a", ZingendeWeideFeature::klei);
    public static final DeferredBlock<Block> ZALMKLEI_STREEP_B = BLOCKS.registerSimpleBlock("zalmklei_streep_b", ZingendeWeideFeature::klei);
    public static final DeferredBlock<ColoredFallingBlock> SCHILDJESZAND = BLOCKS.registerBlock("schildjeszand",
            p -> new ColoredFallingBlock(new ColorRGBA(0xE8D2A0), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SAND));
    public static final DeferredBlock<Block> ZEEGROEN_STEEN = BLOCKS.registerSimpleBlock("zeegroen_steen", ZingendeWeideFeature::steen);
    public static final DeferredBlock<StairBlock> ZEEGROEN_STEEN_TRAP = BLOCKS.registerBlock("zeegroen_steen_trap",
            p -> new StairBlock(ZEEGROEN_STEEN.get().defaultBlockState(), p), ZingendeWeideFeature::steen);
    public static final DeferredBlock<SlabBlock> ZEEGROEN_STEEN_PLAAT = BLOCKS.registerBlock("zeegroen_steen_plaat", SlabBlock::new, ZingendeWeideFeature::steen);
    public static final DeferredBlock<WallBlock> ZEEGROEN_STEEN_MUUR = BLOCKS.registerBlock("zeegroen_steen_muur", WallBlock::new, () -> steen().forceSolidOn());
    public static final DeferredBlock<Block> ZEEGROEN_BAKSTENEN = BLOCKS.registerSimpleBlock("zeegroen_bakstenen", ZingendeWeideFeature::steen);
    public static final DeferredBlock<StairBlock> ZEEGROEN_BAKSTENEN_TRAP = BLOCKS.registerBlock("zeegroen_bakstenen_trap",
            p -> new StairBlock(ZEEGROEN_BAKSTENEN.get().defaultBlockState(), p), ZingendeWeideFeature::steen);
    public static final DeferredBlock<SlabBlock> ZEEGROEN_BAKSTENEN_PLAAT = BLOCKS.registerBlock("zeegroen_bakstenen_plaat", SlabBlock::new,
            ZingendeWeideFeature::steen);
    public static final DeferredBlock<WallBlock> ZEEGROEN_BAKSTENEN_MUUR = BLOCKS.registerBlock("zeegroen_bakstenen_muur", WallBlock::new,
            () -> steen().forceSolidOn());
    public static final DeferredBlock<Block> GEPOLIJSTE_ZEEGROEN_STEEN = BLOCKS.registerSimpleBlock("gepolijste_zeegroen_steen", ZingendeWeideFeature::steen);
    public static final DeferredBlock<StairBlock> GEPOLIJSTE_ZEEGROEN_STEEN_TRAP = BLOCKS.registerBlock("gepolijste_zeegroen_steen_trap",
            p -> new StairBlock(GEPOLIJSTE_ZEEGROEN_STEEN.get().defaultBlockState(), p), ZingendeWeideFeature::steen);
    public static final DeferredBlock<SlabBlock> GEPOLIJSTE_ZEEGROEN_STEEN_PLAAT = BLOCKS.registerBlock("gepolijste_zeegroen_steen_plaat",
            SlabBlock::new, ZingendeWeideFeature::steen);

    // --- the growth (lief roze and lief licht roze) ------------------------------------------------------------------------
    public static final DeferredBlock<WeideBlokken.KortGras> ROZE_GRAS = BLOCKS.registerBlock("weide_roze_gras",
            p -> new WeideBlokken.KortGras(() -> ZingendeWeideFeature.HOOG_ROZE_GRAS.get(), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<WeideBlokken.KortGras> LICHTROZE_GRAS = BLOCKS.registerBlock("weide_lichtroze_gras",
            p -> new WeideBlokken.KortGras(() -> ZingendeWeideFeature.HOOG_LICHTROZE_GRAS.get(), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<WeideBlokken.HoogGras> HOOG_ROZE_GRAS = BLOCKS.registerBlock("weide_hoog_roze_gras", WeideBlokken.HoogGras::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<WeideBlokken.HoogGras> HOOG_LICHTROZE_GRAS = BLOCKS.registerBlock("weide_hoog_lichtroze_gras",
            WeideBlokken.HoogGras::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<FlowerBlock> ZINGBLOEMETJE_ROZE = bloem("roze");
    public static final DeferredBlock<FlowerBlock> ZINGBLOEMETJE_GEEL = bloem("geel");
    public static final DeferredBlock<FlowerBlock> ZINGBLOEMETJE_BLAUW = bloem("blauw");

    private static DeferredBlock<FlowerBlock> bloem(String kleur) {
        return BLOCKS.registerBlock("zingbloemetje_" + kleur, p -> new WeideBlokken.Zingbloemetje(MobEffects.JUMP_BOOST, 5f, p),
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CORNFLOWER));
    }

    // --- crystals ------------------------------------------------------------------------------------------------------------
    public static final List<String> KLEUREN = List.of("cyaan", "lila", "roze");
    public static final DeferredBlock<WeideBlokken.Kristal> KRISTAL_CYAAN = kristal("cyaan");
    public static final DeferredBlock<WeideBlokken.Kristal> KRISTAL_LILA = kristal("lila");
    public static final DeferredBlock<WeideBlokken.Kristal> KRISTAL_ROZE = kristal("roze");
    public static final DeferredBlock<WeideBlokken.Kristalletjes> KRISTALLETJES_CYAAN = kristalletjes("cyaan");
    public static final DeferredBlock<WeideBlokken.Kristalletjes> KRISTALLETJES_LILA = kristalletjes("lila");
    public static final DeferredBlock<WeideBlokken.Kristalletjes> KRISTALLETJES_ROZE = kristalletjes("roze");

    private static DeferredBlock<WeideBlokken.Kristal> kristal(String kleur) {
        return BLOCKS.registerBlock("weidekristal_" + kleur, WeideBlokken.Kristal::new,
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).noOcclusion().lightLevel(s -> 6));
    }

    private static DeferredBlock<WeideBlokken.Kristalletjes> kristalletjes(String kleur) {
        return BLOCKS.registerBlock("weidekristalletjes_" + kleur, p -> new WeideBlokken.Kristalletjes(7f, 10f, p),
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel(s -> 5));
    }

    // --- the wolkjeshout wood set ---------------------------------------------------------------------------------------------
    public static final DeferredBlock<RotatedPillarBlock> GESTRIPT = BLOCKS.registerBlock("wolkjeshout_gestript", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_CHERRY_LOG).mapColor(MapColor.QUARTZ));
    public static final DeferredBlock<RotatedPillarBlock> GESTRIPT_HOUT = BLOCKS.registerBlock("wolkjeshout_gestript_hout", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_CHERRY_WOOD).mapColor(MapColor.QUARTZ));
    public static final DeferredBlock<VadshoutBlocks.Log> STAM = BLOCKS.registerBlock("wolkjeshout_stam",
            p -> new VadshoutBlocks.Log(() -> GESTRIPT.get(), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LOG).mapColor(MapColor.COLOR_GRAY));
    public static final DeferredBlock<VadshoutBlocks.Log> HOUT = BLOCKS.registerBlock("wolkjeshout_hout",
            p -> new VadshoutBlocks.Log(() -> GESTRIPT_HOUT.get(), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_WOOD).mapColor(MapColor.COLOR_GRAY));
    public static final DeferredBlock<WeideBlokken.Gezicht> GEZICHT = BLOCKS.registerBlock("wolkjeshout_gezicht", WeideBlokken.Gezicht::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LOG).mapColor(MapColor.COLOR_GRAY));
    public static final DeferredBlock<Block> PLANKEN = BLOCKS.registerSimpleBlock("wolkjeshout_planken", ZingendeWeideFeature::hout);
    public static final DeferredBlock<StairBlock> TRAP = BLOCKS.registerBlock("wolkjeshout_trap",
            p -> new StairBlock(PLANKEN.get().defaultBlockState(), p), ZingendeWeideFeature::hout);
    public static final DeferredBlock<SlabBlock> PLAAT = BLOCKS.registerBlock("wolkjeshout_plaat", SlabBlock::new, ZingendeWeideFeature::hout);
    public static final DeferredBlock<FenceBlock> HEK = BLOCKS.registerBlock("wolkjeshout_hek", FenceBlock::new, ZingendeWeideFeature::hout);
    public static final DeferredBlock<FenceGateBlock> POORT = BLOCKS.registerBlock("wolkjeshout_poort",
            p -> new FenceGateBlock(WOLKJESHOUT_WOOD, p), () -> hout().forceSolidOn());
    public static final DeferredBlock<DoorBlock> DEUR = BLOCKS.registerBlock("wolkjeshout_deur",
            p -> new DoorBlock(WOLKJESHOUT_SET, p), () -> hout().strength(3f).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<TrapDoorBlock> LUIK = BLOCKS.registerBlock("wolkjeshout_luik",
            p -> new TrapDoorBlock(WOLKJESHOUT_SET, p), () -> hout().strength(3f).noOcclusion().isValidSpawn((s, l, pos, e) -> false));
    public static final DeferredBlock<ButtonBlock> KNOP = BLOCKS.registerBlock("wolkjeshout_knop",
            p -> new ButtonBlock(WOLKJESHOUT_SET, 30, p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_BUTTON));
    public static final DeferredBlock<PressurePlateBlock> DRUKPLAAT = BLOCKS.registerBlock("wolkjeshout_drukplaat",
            p -> new PressurePlateBlock(WOLKJESHOUT_SET, p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PRESSURE_PLATE));
    public static final DeferredBlock<StandingSignBlock> BORD = BLOCKS.registerBlock("wolkjeshout_bord",
            p -> new StandingSignBlock(WOLKJESHOUT_WOOD, p), () -> hout().forceSolidOn().noCollision().strength(1f));
    public static final DeferredBlock<WallSignBlock> WANDBORD = BLOCKS.registerBlock("wolkjeshout_wandbord",
            p -> new WallSignBlock(WOLKJESHOUT_WOOD, p), () -> hout().forceSolidOn().noCollision().strength(1f));
    public static final DeferredBlock<CeilingHangingSignBlock> HANGBORD = BLOCKS.registerBlock("wolkjeshout_hangbord",
            p -> new CeilingHangingSignBlock(WOLKJESHOUT_WOOD, p), () -> hout().forceSolidOn().noCollision().strength(1f));
    public static final DeferredBlock<WallHangingSignBlock> WANDHANGBORD = BLOCKS.registerBlock("wolkjeshout_wandhangbord",
            p -> new WallHangingSignBlock(WOLKJESHOUT_WOOD, p), () -> hout().forceSolidOn().noCollision().strength(1f));
    public static final DeferredBlock<WeideBlokken.Bladeren> BLADEREN = BLOCKS.registerBlock("wolkjeshout_bladeren", WeideBlokken.Bladeren::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).mapColor(MapColor.SNOW));
    public static final DeferredBlock<WeideBlokken.Bladeren> BLADEREN_ROZE = BLOCKS.registerBlock("wolkjeshout_bladeren_roze", WeideBlokken.Bladeren::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<WeideBlokken.Zaailing> ZAAILING = BLOCKS.registerBlock("wolkjeshout_zaailing", WeideBlokken.Zaailing::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SAPLING));
    public static final DeferredBlock<HangingMossBlock> SLIERT = BLOCKS.registerBlock("wolkjes_sliert", HangingMossBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_HANGING_MOSS).mapColor(MapColor.SNOW));

    // --- bubbelsap, the podium, the towel ---------------------------------------------------------------------------------------
    public static final DeferredHolder<FluidType, FluidType> BUBBELSAP_TYPE = FLUID_TYPES.register("bubbelsap", Bubbelsap::type);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BUBBELSAP = FLUIDS.register("bubbelsap",
            () -> new BaseFlowingFluid.Source(Bubbelsap.properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> BUBBELSAP_STROOM = FLUIDS.register("bubbelsap_stroom",
            () -> new BaseFlowingFluid.Flowing(Bubbelsap.properties()));
    public static final DeferredBlock<Bubbelsap.Blok> BUBBELSAP_BLOK = BLOCKS.registerBlock("bubbelsap", p -> new Bubbelsap.Blok(BUBBELSAP.get(), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 4)
                    .postProcess((state, level, pos) -> pos));    // (placed by a feature: tick once when the chunk is done, so it flows)
    public static final DeferredItem<BucketItem> BUBBELSAP_EMMER = ITEMS.registerItem("bubbelsap_emmer",
            p -> new BucketItem(BUBBELSAP.get(), p), () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
    public static final DeferredHolder<MobEffect, Bubbelsap.Bubbelig> BUBBELIG = EFFECTS.register("bubbelig", Bubbelsap.Bubbelig::new);

    public static final DeferredBlock<KoorkomPodium> KOORKOM_PODIUM = BLOCKS.registerBlock("koorkom_podium", KoorkomPodium::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_CYAN));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KoorkomPodium.Entity>> KOORKOM_BE = BLOCK_ENTITIES.register("koorkom_podium",
            () -> new BlockEntityType<>(KoorkomPodium.Entity::new, KOORKOM_PODIUM.get()));
    public static final DeferredItem<WeideBlokken.Handdoekje> HANDDOEKJE = ITEMS.registerItem("handdoekje", WeideBlokken.Handdoekje::new,
            () -> new Item.Properties().stacksTo(16));

    // --- worldgen ------------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> F_PILAAR = FEATURES.register("weide_pilaar", WeideFeatures.Pilaar::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> F_BOOG = FEATURES.register("weide_boog", WeideFeatures.Boog::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> F_REUZENBOOM = FEATURES.register("weide_reuzenboom", WeideFeatures.Reuzenboom::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> F_BOOMPJE = FEATURES.register("weide_boompje", WeideFeatures.Boompje::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> F_BOOMPJES = FEATURES.register("weide_boompjes", WeideFeatures.Boompjes::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> F_KRISTALLEN = FEATURES.register("weide_kristallen", WeideFeatures.Kristallen::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> F_POELTJE = FEATURES.register("weide_poeltje", WeideFeatures.Poeltje::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> F_HOLLETJE = FEATURES.register("weide_holletje", WeideFeatures.Holletje::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> F_SCHILDROTS = FEATURES.register("weide_schildrots", WeideFeatures.Schildrots::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> F_KLOOF = FEATURES.register("weide_kloof", WeideFeatures.Kloof::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> F_BEGROEIING = FEATURES.register("weide_begroeiing", WeideFeatures.Begroeiing::new);
    public static final DeferredHolder<StructureType<?>, StructureType<WeideBouw.Structuur>> BOUW = STRUCTURE_TYPES.register("weide_bouw",
            () -> () -> WeideBouw.Structuur.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BOUW_STUK = PIECES.register("weide_bouw",
            () -> (StructurePieceType.ContextlessType) WeideBouw.Stuk::new);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TRILLERTJE = PARTICLES.register("trillertje", () -> new SimpleParticleType(false));

    /** NPC kind of Badmeester Bubbels (the bath hut of the poeltjes). */
    public static final GuhNpcEntity.Kind BADGUH = GuhNpcEntity.Kind.BUBBELBADGUH;

    static {
        for (DeferredBlock<?> block : List.of(ZALMKLEI_GRAS, ZEEGROEN_GRAS, ZALMKLEI, ZALMKLEI_STREEP_A, ZALMKLEI_STREEP_B, SCHILDJESZAND, ZEEGROEN_STEEN,
                ZEEGROEN_STEEN_TRAP, ZEEGROEN_STEEN_PLAAT, ZEEGROEN_STEEN_MUUR, ZEEGROEN_BAKSTENEN, ZEEGROEN_BAKSTENEN_TRAP, ZEEGROEN_BAKSTENEN_PLAAT,
                ZEEGROEN_BAKSTENEN_MUUR, GEPOLIJSTE_ZEEGROEN_STEEN, GEPOLIJSTE_ZEEGROEN_STEEN_TRAP, GEPOLIJSTE_ZEEGROEN_STEEN_PLAAT, ROZE_GRAS,
                LICHTROZE_GRAS, HOOG_ROZE_GRAS, HOOG_LICHTROZE_GRAS, ZINGBLOEMETJE_ROZE, ZINGBLOEMETJE_GEEL, ZINGBLOEMETJE_BLAUW, KRISTAL_CYAAN,
                KRISTAL_LILA, KRISTAL_ROZE, KRISTALLETJES_CYAAN, KRISTALLETJES_LILA, KRISTALLETJES_ROZE, STAM, GESTRIPT, HOUT, GESTRIPT_HOUT, GEZICHT,
                PLANKEN, TRAP, PLAAT, HEK, POORT, DEUR, LUIK, KNOP, DRUKPLAAT, BLADEREN, BLADEREN_ROZE, ZAAILING, SLIERT, KOORKOM_PODIUM)) {
            ITEMS.registerSimpleBlockItem(block);
        }
        ITEMS.registerItem("wolkjeshout_bord", p -> new SignItem(BORD.get(), WANDBORD.get(), p), p -> p.stacksTo(16).useBlockDescriptionPrefix());
        ITEMS.registerItem("wolkjeshout_hangbord", p -> new HangingSignItem(HANGBORD.get(), WANDHANGBORD.get(), p),
                p -> p.stacksTo(16).useBlockDescriptionPrefix());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        FEATURES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        PIECES.register(modBus);
        PARTICLES.register(modBus);
        EFFECTS.register(modBus);
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        modBus.addListener(ZingendeWeideFeature::setup);
        modBus.addListener((BlockEntityTypeAddBlocksEvent event) -> {
            event.modify(BlockEntityType.SIGN, BORD.get(), WANDBORD.get());
            event.modify(BlockEntityType.HANGING_SIGN, HANGBORD.get(), WANDHANGBORD.get());
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> WeideCommando.register(event));
        NpcRollen.zet(BADGUH, new BadGuh());
        // the Superkompas' biome tab: under the Guhmension, with a wolkjesboompje as its icon
        nl.juiced.guhs.feature.bio.BiomeLijst.biome("guhmension", "zingende_weide");
        nl.juiced.guhs.feature.bio.kompas.BiomeIconen.zet("zingende_weide", "guhs:wolkjeshout_zaailing");
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            for (Block log : List.of(STAM.get(), GESTRIPT.get(), HOUT.get(), GESTRIPT_HOUT.get(), GEZICHT.get())) {
                fire.setFlammable(log, 5, 5);
            }
            for (Block plank : List.of(PLANKEN.get(), TRAP.get(), PLAAT.get(), HEK.get(), POORT.get())) {
                fire.setFlammable(plank, 5, 20);
            }
            for (Block b : List.of(BLADEREN.get(), BLADEREN_ROZE.get())) {
                fire.setFlammable(b, 30, 60);
            }
            for (Block b : List.of(ROZE_GRAS.get(), LICHTROZE_GRAS.get(), HOOG_ROZE_GRAS.get(), HOOG_LICHTROZE_GRAS.get(), SLIERT.get(),
                    ZINGBLOEMETJE_ROZE.get(), ZINGBLOEMETJE_GEEL.get(), ZINGBLOEMETJE_BLAUW.get())) {
                fire.setFlammable(b, 60, 100);
            }
        });
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var block : List.of(ZALMKLEI_GRAS, ZEEGROEN_GRAS, ZALMKLEI, ZALMKLEI_STREEP_A, ZALMKLEI_STREEP_B, SCHILDJESZAND, ZEEGROEN_STEEN, ZEEGROEN_STEEN_TRAP,
                ZEEGROEN_STEEN_PLAAT, ZEEGROEN_STEEN_MUUR, ZEEGROEN_BAKSTENEN, ZEEGROEN_BAKSTENEN_TRAP, ZEEGROEN_BAKSTENEN_PLAAT, ZEEGROEN_BAKSTENEN_MUUR,
                GEPOLIJSTE_ZEEGROEN_STEEN, GEPOLIJSTE_ZEEGROEN_STEEN_TRAP, GEPOLIJSTE_ZEEGROEN_STEEN_PLAAT, ROZE_GRAS, LICHTROZE_GRAS, HOOG_ROZE_GRAS,
                HOOG_LICHTROZE_GRAS, ZINGBLOEMETJE_ROZE, ZINGBLOEMETJE_GEEL, ZINGBLOEMETJE_BLAUW, KRISTAL_CYAAN, KRISTAL_LILA, KRISTAL_ROZE,
                KRISTALLETJES_CYAAN, KRISTALLETJES_LILA, KRISTALLETJES_ROZE, STAM, GESTRIPT, HOUT, GESTRIPT_HOUT, GEZICHT, PLANKEN, TRAP, PLAAT, HEK, POORT,
                DEUR, LUIK, KNOP, DRUKPLAAT, BLADEREN, BLADEREN_ROZE, ZAAILING, SLIERT, KOORKOM_PODIUM)) {
            output.accept(new ItemStack(block.get()));
        }
        output.accept(new ItemStack(BORD.get().asItem()));
        output.accept(new ItemStack(HANGBORD.get().asItem()));
        output.accept(new ItemStack(BUBBELSAP_EMMER.get()));
        output.accept(new ItemStack(HANDDOEKJE.get()));
    }

    private ZingendeWeideFeature() {
    }
}
