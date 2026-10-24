package nl.juiced.guhs.feature.onderwol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;

/**
 * 1.7.0 "Onder de Wol" (design: ontwerp-guhs170-mijnen.md): the Guhmension gets an underground worth digging in. Under
 * the pink wool lie layers of stone (viltsteen y 45-64, the existing knuffelsteen y 25-45 and guhkristalsteen y 5-25,
 * the unbreakable knuffelnaad below), caves that get bigger the deeper you go, a saus sea in the deep caves of some
 * regions, geodes, vanilla-style winding caves and ravines up to the surface, two cave biomes (the Glitterspelonk and
 * the Pluisgrot), and ores: every vanilla ore and the mod's own (vads, kaasknabbels, kaaskorrels) in a version per layer,
 * guhklei, pigment and the rare vahoegiet.
 * <p>
 * The terrain is data (tools/features/onderwol.py: the layers are surface rules, the caves a term in the final density,
 * the cave biomes a shift of depth and continentalness, the winding caves vanilla's carvers); this class registers the
 * blocks and items and the Java feature of the saus sea ({@link OnderWolWorldgen.Sauszee}), and keeps monsters out of the
 * caves ({@link #onSpawnCheck}). New chunks only: the server world is never reset.
 */
public final class OnderWolFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DICHTHEDEN = DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Guhs.MODID);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<GrottenFunctie>> GROTTEN =
            DICHTHEDEN.register("grotten", () -> GrottenFunctie.DATA_CODEC);
    private static final DeferredRegister<MapCodec<? extends SurfaceRules.RuleSource>> REGELS = DeferredRegister.create(Registries.MATERIAL_RULE, Guhs.MODID);
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.RuleSource>, MapCodec<OnderWolLagen>> LAGEN_REGEL =
            REGELS.register("onderwol_lagen", () -> OnderWolLagen.DATA_CODEC);

    public static final ResourceKey<Biome> GLITTERSPELONK = ResourceKey.create(Registries.BIOME, Guhs.id("glitterspelonk"));
    public static final ResourceKey<Biome> PLUISGROT = ResourceKey.create(Registries.BIOME, Guhs.id("pluisgrot"));

    /** The three stone layers an ore comes in (id suffixes), from top to bottom. */
    public static final List<String> LAGEN = List.of("vilt", "knuffel", "kristal");

    private static BlockBehaviour.Properties vilt() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.0f, 4.0f).sound(SoundType.WOOL);
    }

    /** The stone of a layer, for its ores: viltsteen is soft, guhkristalsteen as hard as deepslate. */
    private static BlockBehaviour.Properties laag(String laag) {
        return switch (laag) {
            case "vilt" -> vilt().strength(2.5f, 4.0f);
            case "knuffel" -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_PINK).strength(3.0f, 3.0f);
            default -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).mapColor(MapColor.COLOR_MAGENTA).strength(4.5f, 3.0f)
                    .sound(SoundType.AMETHYST);
        };
    }

    // --- viltsteen and its family --------------------------------------------------------------------------------------
    public static final DeferredBlock<Block> VILTSTEEN = BLOCKS.registerSimpleBlock("viltsteen", OnderWolFeature::vilt);
    public static final DeferredBlock<Block> GEPOLIJSTE_VILTSTEEN = BLOCKS.registerSimpleBlock("gepolijste_viltsteen", () -> vilt().strength(1.5f, 6.0f));
    public static final DeferredBlock<Block> VILTSTEEN_BAKSTENEN = BLOCKS.registerSimpleBlock("viltsteen_bakstenen", () -> vilt().strength(1.5f, 6.0f));
    public static final DeferredBlock<Block> VILTSTEEN_TEGELS = BLOCKS.registerSimpleBlock("viltsteen_tegels", () -> vilt().strength(1.5f, 6.0f));
    /** A viltsteen block with a guh face carved into it. */
    public static final DeferredBlock<Block> GEBEITELDE_VILTSTEEN = BLOCKS.registerSimpleBlock("gebeitelde_viltsteen", () -> vilt().strength(1.5f, 6.0f));
    public static final DeferredBlock<StairBlock> VILTSTEEN_TRAP = trap("viltsteen_trap", VILTSTEEN);
    public static final DeferredBlock<SlabBlock> VILTSTEEN_PLAAT = plaat("viltsteen_plaat", VILTSTEEN);
    public static final DeferredBlock<WallBlock> VILTSTEEN_MUUR = muur("viltsteen_muur", VILTSTEEN);
    public static final DeferredBlock<StairBlock> GEPOLIJSTE_VILTSTEEN_TRAP = trap("gepolijste_viltsteen_trap", GEPOLIJSTE_VILTSTEEN);
    public static final DeferredBlock<SlabBlock> GEPOLIJSTE_VILTSTEEN_PLAAT = plaat("gepolijste_viltsteen_plaat", GEPOLIJSTE_VILTSTEEN);
    public static final DeferredBlock<StairBlock> VILTSTEEN_BAKSTENEN_TRAP = trap("viltsteen_bakstenen_trap", VILTSTEEN_BAKSTENEN);
    public static final DeferredBlock<SlabBlock> VILTSTEEN_BAKSTENEN_PLAAT = plaat("viltsteen_bakstenen_plaat", VILTSTEEN_BAKSTENEN);
    public static final DeferredBlock<WallBlock> VILTSTEEN_BAKSTENEN_MUUR = muur("viltsteen_bakstenen_muur", VILTSTEEN_BAKSTENEN);
    public static final DeferredBlock<StairBlock> VILTSTEEN_TEGELS_TRAP = trap("viltsteen_tegels_trap", VILTSTEEN_TEGELS);
    public static final DeferredBlock<SlabBlock> VILTSTEEN_TEGELS_PLAAT = plaat("viltsteen_tegels_plaat", VILTSTEEN_TEGELS);

    /** Ronde 27: the pink veins and bands in the knuffelsteen layer. */
    public static final DeferredBlock<Block> ROZE_KNUFFELSTEEN = BLOCKS.registerSimpleBlock("roze_knuffelsteen", () -> laag("knuffel"));
    /** Ronde 27: soft gravel of little felt balls, along brooks and shores; falls like gravel. */
    public static final DeferredBlock<ColoredFallingBlock> VILTKIEZEL = BLOCKS.registerBlock("viltkiezel",
            p -> new ColoredFallingBlock(new ColorRGBA(0xFFD49AB0), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL).mapColor(MapColor.COLOR_PINK).sound(SoundType.WOOL));

    /** The bottom of the Guhmension (y 0-4): the seam of the world, unbreakable like bedrock. */
    public static final DeferredBlock<Block> KNUFFELNAAD = BLOCKS.registerSimpleBlock("knuffelnaad",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).mapColor(MapColor.COLOR_PURPLE));
    /** Pink clay in big lumps in the viltsteen and knuffelsteen: lumps smelt into bricks, the block into terracotta. */
    public static final DeferredBlock<Block> GUHKLEI = BLOCKS.registerSimpleBlock("guhklei",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CLAY).mapColor(MapColor.COLOR_PINK));

    // --- the ores: per kind one block per layer ---------------------------------------------------------------------------
    /** Ore kind -> xp when mined (null: the redstone ore, which lights up like the vanilla one). */
    private static final Map<String, IntProvider> SOORTEN = new LinkedHashMap<>();
    static {
        SOORTEN.put("kolenerts", UniformInt.of(0, 2));
        SOORTEN.put("kopererts", ConstantInt.ZERO);
        SOORTEN.put("ijzererts", ConstantInt.ZERO);
        SOORTEN.put("gouderts", ConstantInt.ZERO);
        SOORTEN.put("lapiserts", UniformInt.of(2, 5));
        SOORTEN.put("redstoneerts", null);
        SOORTEN.put("diamanterts", UniformInt.of(3, 7));
        SOORTEN.put("smaragderts", UniformInt.of(3, 7));
        SOORTEN.put("vadserts", UniformInt.of(1, 3));
        SOORTEN.put("kaasknabbelerts", UniformInt.of(0, 2));
        SOORTEN.put("kaaskorrelerts", UniformInt.of(0, 2));
        SOORTEN.put("pigmenterts", UniformInt.of(0, 2));
    }
    /** Every ore by id ("ijzererts_knuffel", ...), in creative-tab order. */
    public static final Map<String, DeferredBlock<? extends Block>> ERTSEN = new LinkedHashMap<>();
    static {
        for (String soort : SOORTEN.keySet()) {
            for (String laag : LAGEN) {
                String id = soort + "_" + laag;
                IntProvider xp = SOORTEN.get(soort);
                ERTSEN.put(id, xp == null
                        ? BLOCKS.registerBlock(id, RedStoneOreBlock::new, () -> laag(laag).randomTicks().lightLevel(s -> s.getValue(RedStoneOreBlock.LIT) ? 9 : 0))
                        : BLOCKS.registerBlock(id, p -> new DropExperienceBlock(xp, p), () -> laag(laag)));
            }
        }
    }
    /** The rare gem of the deep: only in guhkristalsteen and in the walls of geodes. */
    public static final DeferredBlock<DropExperienceBlock> VAHOEGIET_ERTS = BLOCKS.registerBlock("vahoegiet_erts",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p), () -> laag("kristal").strength(5.0f, 6.0f).lightLevel(s -> 5));

    // --- the cave biomes: Glitterspelonk and Pluisgrot ---------------------------------------------------------------------
    /** A glowing pink moss block (floors of the Glitterspelonk) and its carpet. */
    public static final DeferredBlock<Block> LICHTMOSBLOK = BLOCKS.registerSimpleBlock("lichtmosblok",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK).mapColor(MapColor.COLOR_PINK).lightLevel(s -> 4));
    public static final DeferredBlock<CarpetBlock> LICHTMOS = BLOCKS.registerBlock("lichtmos", CarpetBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_CARPET).mapColor(MapColor.COLOR_PINK).lightLevel(s -> 3));
    public static final DeferredBlock<Block> GLITTERRANK = BLOCKS.registerBlock("glitterrank", OnderWolBlokken.Glitterrank::new,
            () -> rank().lightLevel(s -> 8));
    public static final DeferredBlock<Block> GLITTERRANK_BES = BLOCKS.registerBlock("glitterrank_bes", OnderWolBlokken.GlitterrankBes::new,
            () -> rank().lightLevel(s -> 13));
    public static final DeferredBlock<Block> GLINSTERPADDENSTOEL = BLOCKS.registerBlock("glinsterpaddenstoel",
            p -> new OnderWolBlokken.Grotplantje(10, 8, false, p), () -> plantje(MapColor.COLOR_PURPLE).sound(SoundType.FUNGUS).lightLevel(s -> 7));
    public static final DeferredBlock<Block> PLUISBOLLETJE = BLOCKS.registerBlock("pluisbolletje",
            p -> new OnderWolBlokken.Grotplantje(10, 10, true, p), () -> plantje(MapColor.SNOW).sound(SoundType.WOOL));
    public static final DeferredBlock<Block> PLUISKUSSEN = BLOCKS.registerBlock("pluiskussen", OnderWolBlokken.Pluiskussen::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_WOOL).mapColor(MapColor.SNOW).strength(0.6f));

    private static BlockBehaviour.Properties rank() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollision().instabreak().sound(SoundType.CAVE_VINES)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties plantje(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).noCollision().instabreak().replaceable()
                .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY);
    }

    // --- items ---------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<Item> VAHOEGIET = ITEMS.registerItem("vahoegiet", Item::new, () -> new Item.Properties());
    /** A glittery berry from the glitterrank: a little snack. */
    public static final DeferredItem<Item> GLITTERBES = ITEMS.registerItem("glitterbes", Item::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).build()));

    /** Every block that has an item, in creative-tab order. */
    public static final List<DeferredBlock<? extends Block>> BLOCK_ITEMS;
    static {
        List<DeferredBlock<? extends Block>> l = new ArrayList<>(List.of(VILTSTEEN, GEPOLIJSTE_VILTSTEEN, VILTSTEEN_BAKSTENEN, VILTSTEEN_TEGELS,
                GEBEITELDE_VILTSTEEN, VILTSTEEN_TRAP, VILTSTEEN_PLAAT, VILTSTEEN_MUUR, GEPOLIJSTE_VILTSTEEN_TRAP, GEPOLIJSTE_VILTSTEEN_PLAAT,
                VILTSTEEN_BAKSTENEN_TRAP, VILTSTEEN_BAKSTENEN_PLAAT, VILTSTEEN_BAKSTENEN_MUUR, VILTSTEEN_TEGELS_TRAP, VILTSTEEN_TEGELS_PLAAT,
                ROZE_KNUFFELSTEEN, VILTKIEZEL, KNUFFELNAAD, GUHKLEI));
        l.addAll(ERTSEN.values());
        l.addAll(List.of(VAHOEGIET_ERTS, LICHTMOSBLOK, LICHTMOS, GLITTERRANK, GLITTERRANK_BES, GLINSTERPADDENSTOEL, PLUISBOLLETJE, PLUISKUSSEN));
        BLOCK_ITEMS = List.copyOf(l);
        for (DeferredBlock<? extends Block> block : BLOCK_ITEMS) {
            ITEMS.registerSimpleBlockItem(block);
        }
    }

    // --- worldgen ------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SAUSZEE = FEATURES.register("onderwol_sauszee", OnderWolWorldgen.Sauszee::new);

    private static DeferredBlock<StairBlock> trap(String id, DeferredBlock<Block> van) {
        return BLOCKS.registerBlock(id, p -> new StairBlock(van.get().defaultBlockState(), p), () -> BlockBehaviour.Properties.ofFullCopy(van.get()));
    }

    private static DeferredBlock<SlabBlock> plaat(String id, DeferredBlock<Block> van) {
        return BLOCKS.registerBlock(id, SlabBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(van.get()));
    }

    private static DeferredBlock<WallBlock> muur(String id, DeferredBlock<Block> van) {
        return BLOCKS.registerBlock(id, WallBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(van.get()).forceSolidOn());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        DICHTHEDEN.register(modBus);
        REGELS.register(modBus);
        NeoForge.EVENT_BUS.addListener(OnderWolFeature::onSpawnCheck);
        // the Superkompas tab "Biomes" (section Guhmension)
        nl.juiced.guhs.feature.bio.BiomeLijst.biome("guhmension", "glitterspelonk");
        nl.juiced.guhs.feature.bio.kompas.BiomeIconen.zet("glitterspelonk", "guhs:glitterrank_bes");
        nl.juiced.guhs.feature.bio.BiomeLijst.biome("guhmension", "pluisgrot");
        nl.juiced.guhs.feature.bio.kompas.BiomeIconen.zet("pluisgrot", "guhs:pluiskussen");
    }

    /**
     * No monsters wander up from the caves: in the Guhmension a monster does not spawn by itself under the surface, except
     * in the structures made for them (a Mika camp, the Moerasheks' hut: the ones with a monster spawn override).
     */
    static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != EntitySpawnReason.NATURAL && event.getSpawnType() != EntitySpawnReason.CHUNK_GENERATION
                || event.getEntity().getType().getCategory() != MobCategory.MONSTER
                || !(event.getLevel().getLevel() instanceof ServerLevel level) || level.dimension() != ModDimensions.GUHMENSION) {
            return;
        }
        BlockPos pos = event.getEntity().blockPosition();
        if (onderDeGrond(level, pos) && !inMonsterStructuur(level, pos)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    /** Under the surface: more than {@value #ONDER_OPPERVLAK} blocks below the top of the ground (leaves don't count). */
    public static boolean onderDeGrond(ServerLevel level, BlockPos pos) {
        return pos.getY() < level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - ONDER_OPPERVLAK;
    }

    static final int ONDER_OPPERVLAK = 3;

    static boolean inMonsterStructuur(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, s -> s.value().spawnOverrides().containsKey(MobCategory.MONSTER)).isValid();
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<? extends Block> block : BLOCK_ITEMS) {
            output.accept(new ItemStack(block.get()));
        }
        output.accept(new ItemStack(VAHOEGIET.get()));
        output.accept(new ItemStack(GLITTERBES.get()));
    }

    private OnderWolFeature() {
    }
}
