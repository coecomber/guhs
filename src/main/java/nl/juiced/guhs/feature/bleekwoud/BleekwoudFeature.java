package nl.juiced.guhs.feature.bleekwoud;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableFeaturePlacerBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadswoud.VadshoutBlocks;
import nl.juiced.guhs.quest.GuhDex;

/**
 * Het Bleekwoud (1.2.8): the mod's own take on the Pale Garden. A rare, silent forest in the Guhmension where all the pink
 * has drained away. tools/features/bleekwoud.py makes the resources (biome, worldgen, the two structures, textures...).
 * <ul>
 *   <li>The wood set bleekhout_* (with a sleepy guh face in the bark, and a sign), bleekmos (block, carpet, hanging moss).</li>
 *   <li>The Krakend Guhhartje ({@link GuhhartjeBlock}): asleep by day, awake at night between two bleekhout logs; then it calls
 *       its own Kraakguh ({@link KraakguhEntity}): a wooden guh that only moves when nobody looks, and gives you a wooden hug.
 *       The soured heart (verzuurd_guhhartje) calls a Kraak-Mika ({@link KraakMikaEntity}) instead, which shoves.</li>
 *   <li>Kaashars (cheese resin) drips on the trunk when the creature is hit: the clump, the block, harsstenen (bricks).</li>
 *   <li>The oogbloempje: a little flower with a guh eye that is closed by day and opens at night.</li>
 *   <li>Two small structures: de Bleke Open Plek and het Houthakkershutje (with the woodcutter's diary: {@link Dagboek}).</li>
 * </ul>
 * Hard rule: guhs never hurt. The Kraakguh only hugs (a short Slowness), the Kraak-Mika only shoves.
 */
public final class BleekwoudFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS = DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, Guhs.MODID);

    public static final ResourceKey<Biome> BLEEKWOUD = ResourceKey.create(Registries.BIOME, Guhs.id("bleekwoud"));
    public static final ResourceKey<Structure> OPEN_PLEK = ResourceKey.create(Registries.STRUCTURE, Guhs.id("bleke_open_plek"));
    public static final ResourceKey<Structure> HUTJE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("houthakkershutje"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> BOOM = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("bleekhout_boom"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> BOOMPJE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("bleekhout_boompje"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOS_BONEMEAL = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("bleekmos_bonemeal"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOS_PLEK = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("bleekmos_plek"));

    /** The logs a heart needs on both ends, and that cheese resin drips on. */
    public static final TagKey<Block> STAMMEN = TagKey.create(Registries.BLOCK, Guhs.id("bleekhout_stammen"));
    /** Every kind of Mika (another 1.2.8 slice fills it; the Kraak-Mika is in it). */
    public static final TagKey<EntityType<?>> MIKAS = TagKey.create(Registries.ENTITY_TYPE, Guhs.id("mikas"));

    public static final BlockSetType BLEEKHOUT_SET = BlockSetType.register(new BlockSetType("guhs_bleekhout"));
    /** (registered: the sign's texture is assets/guhs/textures/entity/signs/bleekhout.png) */
    public static final WoodType BLEEKHOUT_WOOD = WoodType.register(new WoodType("guhs:bleekhout", BLEEKHOUT_SET));
    /** One sapling: a little bleekhout tree; four in a square: a big one with a thick trunk (never with a heart). */
    public static final TreeGrower GROWER = new TreeGrower("guhs:bleekhout", Optional.of(BOOM), Optional.of(BOOMPJE), Optional.empty());

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_PLANKS);
    }

    private static BlockBehaviour.Properties hars() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().sound(SoundType.RESIN_BRICKS).strength(1.5f, 6.0f);
    }

    // --- the wood set ---------------------------------------------------------------------------------------------------
    public static final DeferredBlock<RotatedPillarBlock> BLEEKHOUT_GESTRIPT = BLOCKS.registerBlock("bleekhout_gestript", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_PALE_OAK_LOG));
    public static final DeferredBlock<VadshoutBlocks.Log> BLEEKHOUT_STAM = BLOCKS.registerBlock("bleekhout_stam",
            p -> new VadshoutBlocks.Log(() -> BLEEKHOUT_GESTRIPT.get(), p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_LOG));
    public static final DeferredBlock<BleekwoudBlocks.Gezicht> BLEEKHOUT_GEZICHT = BLOCKS.registerBlock("bleekhout_gezicht", BleekwoudBlocks.Gezicht::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_LOG).mapColor(MapColor.QUARTZ));   // (a log's map colour asks for its axis)
    public static final DeferredBlock<Block> BLEEKHOUT_PLANKEN = BLOCKS.registerSimpleBlock("bleekhout_planken", () -> wood());
    public static final DeferredBlock<StairBlock> BLEEKHOUT_TRAP = BLOCKS.registerBlock("bleekhout_trap",
            p -> new StairBlock(BLEEKHOUT_PLANKEN.get().defaultBlockState(), p), () -> wood());
    public static final DeferredBlock<SlabBlock> BLEEKHOUT_PLAAT = BLOCKS.registerBlock("bleekhout_plaat", SlabBlock::new, () -> wood());
    public static final DeferredBlock<FenceBlock> BLEEKHOUT_HEK = BLOCKS.registerBlock("bleekhout_hek", FenceBlock::new, () -> wood());
    public static final DeferredBlock<FenceGateBlock> BLEEKHOUT_POORT = BLOCKS.registerBlock("bleekhout_poort",
            p -> new FenceGateBlock(BLEEKHOUT_WOOD, p), () -> wood().forceSolidOn());
    public static final DeferredBlock<DoorBlock> BLEEKHOUT_DEUR = BLOCKS.registerBlock("bleekhout_deur",
            p -> new DoorBlock(BLEEKHOUT_SET, p), () -> wood().strength(3f).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<TrapDoorBlock> BLEEKHOUT_LUIK = BLOCKS.registerBlock("bleekhout_luik",
            p -> new TrapDoorBlock(BLEEKHOUT_SET, p), () -> wood().strength(3f).noOcclusion().isValidSpawn((s, l, pos, e) -> false));
    public static final DeferredBlock<StandingSignBlock> BLEEKHOUT_BORD = BLOCKS.registerBlock("bleekhout_bord",
            p -> new StandingSignBlock(BLEEKHOUT_WOOD, p), () -> wood().forceSolidOn().noCollision().strength(1f));
    public static final DeferredBlock<WallSignBlock> BLEEKHOUT_WANDBORD = BLOCKS.registerBlock("bleekhout_wandbord",
            p -> new WallSignBlock(BLEEKHOUT_WOOD, p), () -> wood().forceSolidOn().noCollision().strength(1f));
    public static final DeferredBlock<BleekwoudBlocks.Bladeren> BLEEKHOUT_BLADEREN = BLOCKS.registerBlock("bleekhout_bladeren",
            BleekwoudBlocks.Bladeren::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_LEAVES));
    public static final DeferredBlock<BleekwoudBlocks.Zaailing> BLEEKHOUT_ZAAILING = BLOCKS.registerBlock("bleekhout_zaailing",
            BleekwoudBlocks.Zaailing::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_SAPLING));

    // --- the forest floor ---------------------------------------------------------------------------------------------------
    /** (bonemeal spreads it, with carpets and hanging tufts, like pale moss) */
    public static final DeferredBlock<BonemealableFeaturePlacerBlock> BLEEKMOS = BLOCKS.registerBlock("bleekmos",
            p -> new BonemealableFeaturePlacerBlock(MOS_BONEMEAL, p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_MOSS_BLOCK));
    public static final DeferredBlock<BleekmosTapijtBlock> BLEEKMOS_TAPIJT = BLOCKS.registerBlock("bleekmos_tapijt", BleekmosTapijtBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_MOSS_CARPET));
    public static final DeferredBlock<HangingMossBlock> BLEEK_HANGMOS = BLOCKS.registerBlock("bleek_hangmos", HangingMossBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_HANGING_MOSS));

    // --- the hearts -----------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<GuhhartjeBlock> KRAKEND_GUHHARTJE = BLOCKS.registerBlock("krakend_guhhartje",
            p -> new GuhhartjeBlock(false, p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CREAKING_HEART));
    public static final DeferredBlock<GuhhartjeBlock> VERZUURD_GUHHARTJE = BLOCKS.registerBlock("verzuurd_guhhartje",
            p -> new GuhhartjeBlock(true, p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CREAKING_HEART).mapColor(MapColor.COLOR_LIGHT_GREEN));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhhartjeBlockEntity>> GUHHARTJE_BE = BLOCK_ENTITIES.register("krakend_guhhartje",
            () -> new BlockEntityType<>(GuhhartjeBlockEntity::new, KRAKEND_GUHHARTJE.get(), VERZUURD_GUHHARTJE.get()));

    // --- kaashars (cheese resin) ------------------------------------------------------------------------------------------------
    public static final DeferredBlock<MultifaceBlock> KAASHARS = BLOCKS.registerBlock("kaashars", MultifaceBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.RESIN_CLUMP));
    public static final DeferredBlock<Block> KAASHARS_BLOK = BLOCKS.registerSimpleBlock("kaashars_blok",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.RESIN_BLOCK));
    public static final DeferredBlock<Block> HARSSTENEN = BLOCKS.registerSimpleBlock("harsstenen", () -> hars());
    public static final DeferredBlock<StairBlock> HARSSTENEN_TRAP = BLOCKS.registerBlock("harsstenen_trap",
            p -> new StairBlock(HARSSTENEN.get().defaultBlockState(), p), () -> hars());
    public static final DeferredBlock<SlabBlock> HARSSTENEN_PLAAT = BLOCKS.registerBlock("harsstenen_plaat", SlabBlock::new, () -> hars());
    public static final DeferredBlock<WallBlock> HARSSTENEN_MUUR = BLOCKS.registerBlock("harsstenen_muur", WallBlock::new, () -> hars().forceSolidOn());
    public static final DeferredBlock<Block> GEBEITELDE_HARSSTENEN = BLOCKS.registerSimpleBlock("gebeitelde_harsstenen", () -> hars());

    // --- the oogbloempje (closed by day, open at night) ----------------------------------------------------------------------------
    public static final DeferredBlock<BleekwoudBlocks.Oogbloempje> OOGBLOEMPJE = BLOCKS.registerBlock("oogbloempje",
            p -> new BleekwoudBlocks.Oogbloempje(false, p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CLOSED_EYEBLOSSOM));
    public static final DeferredBlock<BleekwoudBlocks.Oogbloempje> OPEN_OOGBLOEMPJE = BLOCKS.registerBlock("open_oogbloempje",
            p -> new BleekwoudBlocks.Oogbloempje(true, p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OPEN_EYEBLOSSOM));
    public static final DeferredBlock<BleekwoudBlocks.OogbloemPot> POT_OOGBLOEMPJE = BLOCKS.registerBlock("potted_oogbloempje",
            p -> new BleekwoudBlocks.OogbloemPot(OOGBLOEMPJE, p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_POPPY).randomTicks());
    public static final DeferredBlock<BleekwoudBlocks.OogbloemPot> POT_OPEN_OOGBLOEMPJE = BLOCKS.registerBlock("potted_open_oogbloempje",
            p -> new BleekwoudBlocks.OogbloemPot(OPEN_OOGBLOEMPJE, p), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_POPPY).randomTicks());

    // --- items ----------------------------------------------------------------------------------------------------------
    /** A cheese resin clump, smelted: four make harsstenen. */
    public static final DeferredItem<Item> HARSSTEEN = ITEMS.registerSimpleItem("harssteen", () -> new Item.Properties());

    static {
        for (DeferredBlock<?> block : List.of(BLEEKHOUT_STAM, BLEEKHOUT_GESTRIPT, BLEEKHOUT_GEZICHT, BLEEKHOUT_PLANKEN, BLEEKHOUT_TRAP, BLEEKHOUT_PLAAT,
                BLEEKHOUT_HEK, BLEEKHOUT_POORT, BLEEKHOUT_DEUR, BLEEKHOUT_LUIK, BLEEKHOUT_BLADEREN, BLEEKHOUT_ZAAILING, BLEEKMOS, BLEEKMOS_TAPIJT,
                BLEEK_HANGMOS, KRAKEND_GUHHARTJE, VERZUURD_GUHHARTJE, KAASHARS, KAASHARS_BLOK, HARSSTENEN, HARSSTENEN_TRAP, HARSSTENEN_PLAAT,
                HARSSTENEN_MUUR, GEBEITELDE_HARSSTENEN, OOGBLOEMPJE, OPEN_OOGBLOEMPJE)) {
            ITEMS.registerSimpleBlockItem(block);
        }
        ITEMS.registerItem("bleekhout_bord", p -> new SignItem(BLEEKHOUT_BORD.get(), BLEEKHOUT_WANDBORD.get(), p),
                p -> p.stacksTo(16).useBlockDescriptionPrefix());
    }

    // --- the creatures -------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<KraakguhEntity>> KRAAKGUH = ENTITY_TYPES.register("kraakguh",
            () -> EntityType.Builder.of(KraakguhEntity::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("kraakguh"))));
    /** (a Mika: a monster like the others, but allowed in peaceful: its heart decides when it is there) */
    public static final DeferredHolder<EntityType<?>, EntityType<KraakMikaEntity>> KRAAK_MIKA = ENTITY_TYPES.register("kraak_mika",
            () -> EntityType.Builder.<KraakMikaEntity>of(KraakMikaEntity::new, MobCategory.MONSTER).sized(0.9f, 0.8f).eyeHeight(0.55f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("kraak_mika"))));
    public static final DeferredItem<SpawnEggItem> KRAAKGUH_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "kraakguh_spawn_egg", KRAAKGUH);
    public static final DeferredItem<SpawnEggItem> KRAAK_MIKA_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "kraak_mika_spawn_egg", KRAAK_MIKA);

    // --- tree decorators -------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<BleekwoudBoom.Hangmos>> HANGMOS_DECORATOR =
            TREE_DECORATORS.register("bleek_hangmos", () -> new TreeDecoratorType<>(BleekwoudBoom.Hangmos.CODEC));
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<BleekwoudBoom.Hartje>> HARTJE_DECORATOR =
            TREE_DECORATORS.register("krakend_guhhartje", () -> new TreeDecoratorType<>(BleekwoudBoom.Hartje.CODEC));

    // --- sounds (tools/features/bleekwoud.py: built from vanilla's creaking and guh sounds) ---------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> HART_KLOP = sound("bleekwoud.hart.klop");
    public static final DeferredHolder<SoundEvent, SoundEvent> HART_WAKKER = sound("bleekwoud.hart.wakker");
    public static final DeferredHolder<SoundEvent, SoundEvent> HART_AU = sound("bleekwoud.hart.au");
    public static final DeferredHolder<SoundEvent, SoundEvent> KRAAK = sound("bleekwoud.kraakguh.kraak");
    public static final DeferredHolder<SoundEvent, SoundEvent> KRAAK_BEVRIES = sound("bleekwoud.kraakguh.bevries");
    public static final DeferredHolder<SoundEvent, SoundEvent> KRAAK_AU = sound("bleekwoud.kraakguh.au");
    public static final DeferredHolder<SoundEvent, SoundEvent> KRAAK_KNUFFEL = sound("bleekwoud.kraakguh.knuffel");
    public static final DeferredHolder<SoundEvent, SoundEvent> KRAAK_VERKRUIMEL = sound("bleekwoud.kraakguh.verkruimel");
    public static final DeferredHolder<SoundEvent, SoundEvent> OOGJE_OPEN = sound("bleekwoud.oogbloempje.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> OOGJE_DICHT = sound("bleekwoud.oogbloempje.dicht");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        TREE_DECORATORS.register(modBus);
        modBus.addListener(BleekwoudFeature::setup);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(KRAAKGUH.get(), KraakguhEntity.createAttributes().build());
            event.put(KRAAK_MIKA.get(), KraakMikaEntity.createKraakAttributes().build());
        });
        modBus.addListener((BlockEntityTypeAddBlocksEvent event) ->
                event.modify(BlockEntityType.SIGN, BLEEKHOUT_BORD.get(), BLEEKHOUT_WANDBORD.get()));
        NeoForge.EVENT_BUS.register(BleekwoudEvents.class);
        GuhDex.creaturePage(GuhVariant.KRAAKGUH, KRAAKGUH, 8.0);
        GuhDex.creaturePage(GuhVariant.KRAAK_MIKA, KRAAK_MIKA, 8.0);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            for (Block log : List.of(BLEEKHOUT_STAM.get(), BLEEKHOUT_GESTRIPT.get(), BLEEKHOUT_GEZICHT.get())) {
                fire.setFlammable(log, 5, 5);
            }
            for (Block planks : List.of(BLEEKHOUT_PLANKEN.get(), BLEEKHOUT_TRAP.get(), BLEEKHOUT_PLAAT.get(), BLEEKHOUT_HEK.get(), BLEEKHOUT_POORT.get())) {
                fire.setFlammable(planks, 5, 20);
            }
            fire.setFlammable(BLEEKHOUT_BLADEREN.get(), 30, 60);
            fire.setFlammable(BLEEKMOS.get(), 5, 100);
            fire.setFlammable(BLEEKMOS_TAPIJT.get(), 5, 100);
            fire.setFlammable(BLEEK_HANGMOS.get(), 5, 100);
            fire.setFlammable(OOGBLOEMPJE.get(), 60, 100);
            fire.setFlammable(OPEN_OOGBLOEMPJE.get(), 60, 100);
            FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;
            pot.addPlant(OOGBLOEMPJE.getId(), POT_OOGBLOEMPJE);
            pot.addPlant(OPEN_OOGBLOEMPJE.getId(), POT_OPEN_OOGBLOEMPJE);
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var block : List.of(BLEEKHOUT_STAM, BLEEKHOUT_GESTRIPT, BLEEKHOUT_GEZICHT, BLEEKHOUT_PLANKEN, BLEEKHOUT_TRAP, BLEEKHOUT_PLAAT, BLEEKHOUT_HEK,
                BLEEKHOUT_POORT, BLEEKHOUT_DEUR, BLEEKHOUT_LUIK, BLEEKHOUT_BORD, BLEEKHOUT_BLADEREN, BLEEKHOUT_ZAAILING, BLEEKMOS, BLEEKMOS_TAPIJT,
                BLEEK_HANGMOS, OOGBLOEMPJE, OPEN_OOGBLOEMPJE, KRAKEND_GUHHARTJE, VERZUURD_GUHHARTJE, KAASHARS, KAASHARS_BLOK)) {
            output.accept(new ItemStack(block.get()));
        }
        output.accept(new ItemStack(HARSSTEEN.get()));
        for (var block : List.of(HARSSTENEN, HARSSTENEN_TRAP, HARSSTENEN_PLAAT, HARSSTENEN_MUUR, GEBEITELDE_HARSSTENEN)) {
            output.accept(new ItemStack(block.get()));
        }
        output.accept(new ItemStack(KRAAKGUH_SPAWN_EGG.get()));
        output.accept(new ItemStack(KRAAK_MIKA_SPAWN_EGG.get()));
    }

    /** A block item for {@link BlockItem} lookups in the tests. */
    public static Item item(DeferredBlock<?> block) {
        return block.get().asItem();
    }

    private BleekwoudFeature() {
    }
}
