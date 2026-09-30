package nl.juiced.guhs.registry;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.BankGuhBlock;
import nl.juiced.guhs.block.FryingPanBlock;
import nl.juiced.guhs.block.GuhPortalBlock;
import nl.juiced.guhs.block.GuhSpawnerBlock;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.GuhWheelPartBlock;
import nl.juiced.guhs.block.GuhWireBlock;
import nl.juiced.guhs.block.KaasknabbelBlock;

/**
 * "Kaasknabbel ores": vanilla blocks with kaas knabbels stuck in them.
 * Textures are the vanilla block + a cheese puff overlay (see assets/guhs/models/block/kaasknabbel_ore_base.json),
 * so they automatically follow whatever resource pack you use for the base block.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);

    public static final DeferredBlock<Block> KAASKNABBEL_STONE = BLOCKS.registerBlock("kaasknabbel_stone",
            props -> new DropExperienceBlock(UniformInt.of(0, 2), props),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE));

    public static final DeferredBlock<Block> KAASKNABBEL_DEEPSLATE = BLOCKS.registerBlock("kaasknabbel_deepslate",
            props -> new DropExperienceBlock(UniformInt.of(0, 2), props),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE));

    public static final DeferredBlock<Block> KAASKNABBEL_DIRT = BLOCKS.registerBlock("kaasknabbel_dirt",
            props -> new DropExperienceBlock(UniformInt.of(0, 1), props),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT));

    public static final DeferredBlock<Block> KAASKNABBEL_COBBLESTONE = BLOCKS.registerBlock("kaasknabbel_cobblestone",
            props -> new DropExperienceBlock(UniformInt.of(0, 2), props),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE));

    /** Fully made of kaas knabbels. Build a nether-portal-shaped frame out of these to open a portal to the Guhmension. */
    public static final DeferredBlock<Block> BLOCK_OF_KAASKNABBELS = BLOCKS.registerBlock("block_of_kaasknabbels",
            KaasknabbelBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.8f).sound(SoundType.WART_BLOCK)
                    .instrument(NoteBlockInstrument.BANJO));

    public static final DeferredBlock<Block> GUH_PORTAL = BLOCKS.registerBlock("guh_portal",
            GuhPortalBlock::new,
            BlockBehaviour.Properties.of().noCollission().strength(-1f).sound(SoundType.GLASS).lightLevel(s -> 11)
                    .pushReaction(PushReaction.BLOCK).noLootTable().mapColor(MapColor.COLOR_PINK));

    /** Guh-coloured ore deep in the Guhmension. Needs an iron pickaxe; drops Vahoege Vads. */
    // --- guh village job sites (one per guh profession, see ModVillagers) ------------------------------------------
    public static final DeferredBlock<nl.juiced.guhs.block.GuhWorkstationBlock> KNABBELBAK = BLOCKS.registerBlock("knabbelbak",
            nl.juiced.guhs.block.GuhWorkstationBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.5f).sound(SoundType.WOOD));
    public static final DeferredBlock<nl.juiced.guhs.block.GuhWorkstationBlock> NAAITAFEL = BLOCKS.registerBlock("naaitafel",
            nl.juiced.guhs.block.GuhWorkstationBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.5f).sound(SoundType.WOOD));
    public static final DeferredBlock<nl.juiced.guhs.block.GuhWorkstationBlock> VADSAAMBEELD = BLOCKS.registerBlock("vadsaambeeld",
            nl.juiced.guhs.block.GuhWorkstationBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.5f).sound(SoundType.WOOD));
    public static final DeferredBlock<nl.juiced.guhs.block.GuhWorkstationBlock> BUIZENBANK = BLOCKS.registerBlock("buizenbank",
            nl.juiced.guhs.block.GuhWorkstationBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.5f).sound(SoundType.WOOD));
    public static final DeferredBlock<nl.juiced.guhs.block.GuhWorkstationBlock> MIKATROFEE = BLOCKS.registerBlock("mikatrofee",
            nl.juiced.guhs.block.GuhWorkstationBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.5f).sound(SoundType.WOOD));

    public static final DeferredBlock<Block> COMPRESSED_SUPER_VAHOEGE_VADS = BLOCKS.registerBlock("compressed_super_vahoege_vads",
            props -> new DropExperienceBlock(UniformInt.of(3, 7), props),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(4.5f, 6f).requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST));

    /** Fry kaas knabbels with Mika's vet. */
    public static final DeferredBlock<Block> FRYING_PAN = BLOCKS.registerBlock("frying_pan",
            FryingPanBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f).sound(SoundType.LANTERN).noOcclusion());

    /** Mineable spawner found in hamster houses (drops itself when mined with a pickaxe). */
    public static final DeferredBlock<Block> GUH_SPAWNER = BLOCKS.registerBlock("guh_spawner",
            GuhSpawnerBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(5f).requiresCorrectToolForDrops()
                    .sound(SoundType.METAL).noOcclusion());

    /** Pink running wheel: put a tamed guh in it for endless redstone power. */
    public static final DeferredBlock<Block> GUH_WHEEL = BLOCKS.registerBlock("guh_wheel",
            GuhWheelBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f).sound(SoundType.STONE).noOcclusion());

    /** The invisible rest of the big guh wheel. */
    public static final DeferredBlock<Block> GUH_WHEEL_PART = BLOCKS.registerBlock("guh_wheel_part",
            GuhWheelPartBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f).sound(SoundType.STONE).noOcclusion()
                    .noLootTable().pushReaction(PushReaction.BLOCK));

    /** Redstone wire that never loses strength. */
    public static final DeferredBlock<Block> GUH_WIRE = BLOCKS.registerBlock("guh_wire",
            GuhWireBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollission().instabreak()
                    .pushReaction(PushReaction.DESTROY));

    /** Bank Guh: infinite storage in a vadsige guh's stomach (reward of the Hungry Guh quest). */
    public static final DeferredBlock<Block> BANK_GUH = BLOCKS.registerBlock("bank_guh",
            BankGuhBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.5f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));

    /** The kaas saus fluid block (flows like a slow, thick water). */
    public static final DeferredBlock<LiquidBlock> KAAS_SAUS = BLOCKS.registerBlock("kaas_saus",
            props -> new LiquidBlock(ModFluids.KAAS_SAUS.get(), props),
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.COLOR_ORANGE)
                    // placed by a structure (fountains): tick once when the chunk is finished, so the saus starts flowing
                    .hasPostProcess((state, level, pos) -> true));

    // --- the guh stomachs (guhmaag dimension): walls you can't break, portals, stomach acid ----------------------------
    private static BlockBehaviour.Properties unbreakable(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(-1f, 3600000f).noLootTable().sound(sound)
                .isValidSpawn((s, l, p, e) -> false);
    }

    public static final DeferredBlock<Block> MAAGWAND = BLOCKS.registerSimpleBlock("maagwand", () -> unbreakable(MapColor.COLOR_PINK, SoundType.SLIME_BLOCK));
    public static final DeferredBlock<Block> MAAGBODEM = BLOCKS.registerSimpleBlock("maagbodem", () -> unbreakable(MapColor.COLOR_RED, SoundType.SLIME_BLOCK));
    public static final DeferredBlock<Block> TONG = BLOCKS.registerSimpleBlock("tong", () -> unbreakable(MapColor.COLOR_RED, SoundType.WOOL));
    public static final DeferredBlock<Block> TAND = BLOCKS.registerSimpleBlock("tand", () -> unbreakable(MapColor.SNOW, SoundType.BONE_BLOCK));
    /** Half-digested kaasknabbels (found in every stomach). */
    public static final DeferredBlock<Block> VERTEERDE_KAASKNABBELS = BLOCKS.registerSimpleBlock("verteerde_kaasknabbels",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6f).sound(SoundType.SLIME_BLOCK));
    public static final DeferredBlock<nl.juiced.guhs.block.MaagPortalBlock> MAAG_PORTAL = BLOCKS.registerBlock("maag_portal",
            nl.juiced.guhs.block.MaagPortalBlock::new,
            BlockBehaviour.Properties.of().noCollission().strength(-1f).sound(SoundType.SLIME_BLOCK).lightLevel(s -> 9)
                    .pushReaction(PushReaction.BLOCK).noLootTable().mapColor(MapColor.COLOR_RED));
    /** Guh stomach acid: just for the atmosphere (it doesn't hurt). */
    public static final DeferredBlock<LiquidBlock> MAAGZUUR = BLOCKS.registerBlock("maagzuur",
            props -> new LiquidBlock(ModFluids.MAAGZUUR.get(), props),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.COLOR_LIGHT_GREEN).lightLevel(s -> 4));

    // --- guh sled rails: a 4x4 piece = one anchor block (drawn by its block entity) + 15 invisible parts ---------------
    public static final DeferredBlock<nl.juiced.guhs.block.SleeRailBlock> SLEE_RAIL = BLOCKS.registerBlock("slee_rail",
            nl.juiced.guhs.block.SleeRailBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f).sound(SoundType.WOOD).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, e) -> false));
    public static final DeferredBlock<nl.juiced.guhs.block.SleeRailPartBlock> SLEE_RAIL_PART = BLOCKS.registerBlock("slee_rail_part",
            nl.juiced.guhs.block.SleeRailPartBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f).sound(SoundType.WOOD).noOcclusion()
                    .noLootTable().pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, e) -> false));

    // --- guh crystals (crystal mines, deep under the Guhmension) and the guh sea -----------------------------------
    public static final DeferredBlock<nl.juiced.guhs.block.GuhKristalClusterBlock> GUH_KRISTAL_CLUSTER = BLOCKS.registerBlock("guh_kristal_cluster",
            nl.juiced.guhs.block.GuhKristalClusterBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).forceSolidOn().noOcclusion().strength(1.5f)
                    .sound(SoundType.AMETHYST_CLUSTER).lightLevel(s -> 7).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> GUH_KRISTAL_BLOK = BLOCKS.registerSimpleBlock("guh_kristal_blok",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f).sound(SoundType.AMETHYST)
                    .lightLevel(s -> 10).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> GUH_KRISTAL_LAMP = BLOCKS.registerSimpleBlock("guh_kristal_lamp",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.6f).sound(SoundType.GLASS).lightLevel(s -> 15));
    public static final DeferredBlock<Block> GUH_KRISTALSTEEN = BLOCKS.registerSimpleBlock("guh_kristalsteen",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_PINK).strength(1.2f).sound(SoundType.CALCITE)
                    .requiresCorrectToolForDrops());
    public static final DeferredBlock<net.minecraft.world.level.block.WaterlilyBlock> GUH_WATERLELIE = BLOCKS.registerBlock("guh_waterlelie",
            net.minecraft.world.level.block.WaterlilyBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD).mapColor(MapColor.COLOR_PINK));

    // --- guh bees and guh slimes -----------------------------------------------------------------------------------
    public static final DeferredBlock<nl.juiced.guhs.block.KnabbelkorfBlock> KNABBELKORF = BLOCKS.registerBlock("knabbelkorf",
            nl.juiced.guhs.block.KnabbelkorfBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEEHIVE).mapColor(MapColor.COLOR_YELLOW));
    public static final DeferredBlock<nl.juiced.guhs.block.RozeSlijmBlock> ROZE_SLIJMBLOK = BLOCKS.registerBlock("roze_slijmblok",
            nl.juiced.guhs.block.RozeSlijmBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SLIME_BLOCK).mapColor(MapColor.COLOR_PINK));

    // --- food, furniture and deco (2.0.0) ---------------------------------------------------------------------------
    public static final DeferredBlock<nl.juiced.guhs.block.GuhDecoBlocks.GuhTaart> GUH_TAART = BLOCKS.registerBlock("guh_taart",
            nl.juiced.guhs.block.GuhDecoBlocks.GuhTaart::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CAKE).mapColor(MapColor.COLOR_PINK));

    private static BlockBehaviour.Properties furniture() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava();
    }

    private static BlockBehaviour.Properties soft() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.6f).sound(SoundType.WOOL).noOcclusion().ignitedByLava();
    }

    public static final DeferredBlock<nl.juiced.guhs.block.GuhFurnitureBlock> GUH_STOEL = BLOCKS.registerBlock("guh_stoel",
            p -> new nl.juiced.guhs.block.GuhFurnitureBlock(p, 0.5, new double[]{2, 7, 2, 14, 9, 14}, new double[]{2, 9, 12, 14, 20, 14},
                    new double[]{2, 0, 2, 4, 7, 4}, new double[]{12, 0, 2, 14, 7, 4}, new double[]{2, 0, 12, 4, 7, 14},
                    new double[]{12, 0, 12, 14, 7, 14}), () -> furniture());
    public static final DeferredBlock<nl.juiced.guhs.block.GuhFurnitureBlock> GUH_TAFEL = BLOCKS.registerBlock("guh_tafel",
            p -> new nl.juiced.guhs.block.GuhFurnitureBlock(p, -1, new double[]{0, 13, 0, 16, 16, 16}, new double[]{1, 0, 1, 4, 13, 4},
                    new double[]{12, 0, 1, 15, 13, 4}, new double[]{1, 0, 12, 4, 13, 15}, new double[]{12, 0, 12, 15, 13, 15}), () -> furniture());
    public static final DeferredBlock<nl.juiced.guhs.block.GuhFurnitureBlock> GUH_BANK = BLOCKS.registerBlock("guh_bank",
            p -> new nl.juiced.guhs.block.GuhFurnitureBlock(p, 0.5, new double[]{0, 0, 1, 16, 8, 15}, new double[]{0, 8, 11, 16, 16, 15},
                    new double[]{0, 8, 1, 2, 12, 11}, new double[]{14, 8, 1, 16, 12, 11}), () -> furniture());
    public static final DeferredBlock<nl.juiced.guhs.block.GuhDecoBlocks.Kast> GUH_KAST = BLOCKS.registerBlock("guh_kast",
            nl.juiced.guhs.block.GuhDecoBlocks.Kast::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).mapColor(MapColor.COLOR_PINK));
    public static final java.util.Map<String, DeferredBlock<net.minecraft.world.level.block.LanternBlock>> LAMPIONNEN = new java.util.LinkedHashMap<>();
    public static final java.util.Map<net.minecraft.world.item.DyeColor, DeferredBlock<nl.juiced.guhs.block.GuhFurnitureBlock>> ZITZAKKEN =
            new java.util.EnumMap<>(net.minecraft.world.item.DyeColor.class);
    /** A lampgion that went out (a verstopguh hint: no guhs left in this room). */
    public static final DeferredBlock<net.minecraft.world.level.block.LanternBlock> LAMPION_UIT = BLOCKS.registerBlock("lampion_uit",
            net.minecraft.world.level.block.LanternBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).sound(SoundType.WOOL)
                    .mapColor(MapColor.COLOR_PINK).lightLevel(s -> 0).noLootTable());
    public static final java.util.Map<net.minecraft.world.item.DyeColor, DeferredBlock<nl.juiced.guhs.block.GuhFurnitureBlock>> KUSSENS =
            new java.util.EnumMap<>(net.minecraft.world.item.DyeColor.class);

    static {
        for (String colour : new String[]{"roze", "geel", "mint"}) {
            LAMPIONNEN.put(colour, BLOCKS.registerBlock("lampion_" + colour, net.minecraft.world.level.block.LanternBlock::new,
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).sound(SoundType.WOOL).mapColor(MapColor.COLOR_PINK)));
        }
        for (net.minecraft.world.item.DyeColor colour : net.minecraft.world.item.DyeColor.values()) {
            ZITZAKKEN.put(colour, BLOCKS.registerBlock(colour.getName() + "_zitzak", p -> new nl.juiced.guhs.block.GuhFurnitureBlock(p, 0.4,
                    new double[]{1, 0, 1, 15, 7, 15}, new double[]{2, 7, 9, 14, 13, 15}), () -> soft().mapColor(colour.getMapColor())));
            KUSSENS.put(colour, BLOCKS.registerBlock(colour.getName() + "_kussen", p -> new nl.juiced.guhs.block.GuhFurnitureBlock(p, 0.2,
                    new double[]{2, 0, 2, 14, 4, 14}), () -> soft().mapColor(colour.getMapColor())));
        }
    }

    public static final DeferredBlock<nl.juiced.guhs.block.GuhDecoBlocks.Vlaggetjes> VLAGGETJES = BLOCKS.registerBlock("vlaggetjes",
            nl.juiced.guhs.block.GuhDecoBlocks.Vlaggetjes::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollission().instabreak().sound(SoundType.WOOL).noOcclusion());

    // guh flowers (+ in a pot)
    private static DeferredBlock<nl.juiced.guhs.block.GuhDecoBlocks.GuhBloem> flower(String name, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return BLOCKS.registerBlock(name, p -> new nl.juiced.guhs.block.GuhDecoBlocks.GuhBloem(effect, 7f, p),
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_TULIP));
    }

    private static DeferredBlock<net.minecraft.world.level.block.FlowerPotBlock> potted(String name, DeferredBlock<? extends Block> flower) {
        return BLOCKS.registerBlock("potted_" + name, p -> new net.minecraft.world.level.block.FlowerPotBlock(
                () -> (net.minecraft.world.level.block.FlowerPotBlock) Blocks.FLOWER_POT, flower, p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_PINK_TULIP));
    }

    public static final DeferredBlock<nl.juiced.guhs.block.GuhDecoBlocks.GuhBloem> KAASBLOEM = flower("kaasbloem", net.minecraft.world.effect.MobEffects.SATURATION);
    public static final DeferredBlock<nl.juiced.guhs.block.GuhDecoBlocks.GuhBloem> GUHOORTJES = flower("guhoortjes", net.minecraft.world.effect.MobEffects.REGENERATION);
    public static final DeferredBlock<nl.juiced.guhs.block.GuhDecoBlocks.GuhBloem> ROZE_GUHBLOEM = flower("roze_guhbloem", net.minecraft.world.effect.MobEffects.NIGHT_VISION);
    public static final DeferredBlock<nl.juiced.guhs.block.GuhDecoBlocks.GuhBloem> KNABBELROOS = flower("knabbelroos", net.minecraft.world.effect.MobEffects.ABSORPTION);
    public static final DeferredBlock<net.minecraft.world.level.block.FlowerPotBlock> POTTED_KAASBLOEM = potted("kaasbloem", KAASBLOEM);
    public static final DeferredBlock<net.minecraft.world.level.block.FlowerPotBlock> POTTED_GUHOORTJES = potted("guhoortjes", GUHOORTJES);
    public static final DeferredBlock<net.minecraft.world.level.block.FlowerPotBlock> POTTED_ROZE_GUHBLOEM = potted("roze_guhbloem", ROZE_GUHBLOEM);
    public static final DeferredBlock<net.minecraft.world.level.block.FlowerPotBlock> POTTED_KNABBELROOS = potted("knabbelroos", KNABBELROOS);

    // the knabbelboer's farm
    public static final DeferredBlock<nl.juiced.guhs.block.GuhDecoBlocks.RozeGras> ROZE_GRAS = BLOCKS.registerBlock("roze_gras",
            nl.juiced.guhs.block.GuhDecoBlocks.RozeGras::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<nl.juiced.guhs.block.GuhDecoBlocks.Kaasknabbelplant> KAASKNABBELPLANT = BLOCKS.registerBlock("kaasknabbelplant",
            nl.juiced.guhs.block.GuhDecoBlocks.Kaasknabbelplant::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).mapColor(MapColor.COLOR_YELLOW));
    public static final DeferredBlock<nl.juiced.guhs.block.GuhWorkstationBlock> ZAADBAK = BLOCKS.registerBlock("zaadbak",
            nl.juiced.guhs.block.GuhWorkstationBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.5f).sound(SoundType.WOOD));

    // --- the guh blossom tree ---------------------------------------------------------------------------------------
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> GUHBLOESEM_LOG = BLOCKS.registerBlock("guhbloesem_log",
            net.minecraft.world.level.block.RotatedPillarBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LOG));
    // --- the guh castle ---------------------------------------------------------------------------------------------------
    public static final DeferredBlock<nl.juiced.guhs.block.KoningsTroonBlock> KONINGSTROON = BLOCKS.registerBlock("koningstroon",
            nl.juiced.guhs.block.KoningsTroonBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PLANKS).noOcclusion().strength(3f));
    // --- the verstopguh house ------------------------------------------------------------------------------------------
    public static final DeferredBlock<nl.juiced.guhs.block.VerstopBlocks.Eenrichtingsglas> EENRICHTINGSGLAS = BLOCKS.registerBlock("eenrichtingsglas",
            nl.juiced.guhs.block.VerstopBlocks.Eenrichtingsglas::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_STAINED_GLASS));
    public static final DeferredBlock<nl.juiced.guhs.block.VerstopBlocks.Marker> VERSTOPPLEK = BLOCKS.registerBlock("verstopplek",
            nl.juiced.guhs.block.VerstopBlocks.Marker::new, BlockBehaviour.Properties.of().noCollission().noLootTable().strength(-1f, 3600000f)
                    .noOcclusion().isValidSpawn((s, l, p, e) -> false));
    public static final DeferredBlock<nl.juiced.guhs.block.VerstopBlocks.Marker> VERSTOPSTART = BLOCKS.registerBlock("verstopstart",
            nl.juiced.guhs.block.VerstopBlocks.Marker::new, BlockBehaviour.Properties.of().noCollission().noLootTable().strength(-1f, 3600000f)
                    .noOcclusion().isValidSpawn((s, l, p, e) -> false));
    public static final DeferredBlock<nl.juiced.guhs.block.VerstopBlocks.Uitgang> VERSTOPUITGANG = BLOCKS.registerBlock("verstopuitgang",
            nl.juiced.guhs.block.VerstopBlocks.Uitgang::new, BlockBehaviour.Properties.of().noCollission().noLootTable().strength(-1f, 3600000f)
                    .lightLevel(s -> 11).noOcclusion());
    public static final DeferredBlock<Block> GUHBLOESEM_PLANKS = BLOCKS.registerSimpleBlock("guhbloesem_planks",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PLANKS));
    public static final DeferredBlock<nl.juiced.guhs.block.GuhBloesemBlocks.Leaves> GUHBLOESEM_LEAVES = BLOCKS.registerBlock("guhbloesem_leaves",
            nl.juiced.guhs.block.GuhBloesemBlocks.Leaves::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES));
    public static final DeferredBlock<nl.juiced.guhs.block.GuhBloesemBlocks.Sapling> GUHBLOESEM_SAPLING = BLOCKS.registerBlock("guhbloesem_sapling",
            nl.juiced.guhs.block.GuhBloesemBlocks.Sapling::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SAPLING));

    /** Our flowers fit in a normal flower pot. */
    public static void registerPots() {
        var pot = (net.minecraft.world.level.block.FlowerPotBlock) Blocks.FLOWER_POT;
        pot.addPlant(KAASBLOEM.getId(), POTTED_KAASBLOEM);
        pot.addPlant(GUHOORTJES.getId(), POTTED_GUHOORTJES);
        pot.addPlant(ROZE_GUHBLOEM.getId(), POTTED_ROZE_GUHBLOEM);
        pot.addPlant(KNABBELROOS.getId(), POTTED_KNABBELROOS);
    }

    private ModBlocks() {
    }
}
