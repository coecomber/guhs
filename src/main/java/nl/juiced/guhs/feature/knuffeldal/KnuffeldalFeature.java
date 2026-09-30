package nl.juiced.guhs.feature.knuffeldal;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhDex;

import net.minecraft.resources.Identifier;
/**
 * Het Knuffeldal (2.8, phase 1): a small, soft pink valley in the Guhmensie with exactly one town in its middle, and the
 * Grote Knusfeest. tools/features/knuffeldal.py (+ knuffeldal_*.py) makes the resources.
 * <ul>
 *   <li>The biome guhs:knuffeldal (its own noise guhs:guhmension_knuffel, flattened towards the middle): knuffelgras,
 *       pluisgras, pluizenbomen, guh-paddenstoelen; the Pluisguh variant is born here ({@link KnuffeldalEvents}).</li>
 *   <li>The town {@code knuffeldal_stadje} ({@link KnuffeldalStadjeStructure}): the plein with the guh fountain,
 *       Burgemeester Vadsema ({@link Burgemeester}), Opa Guh on his bench by the campfire, the feestbuffet, seasonal
 *       decoration, 4+1 building slots (feature.knus.PleinSlot), six guh houses with residents, and Cocotje's house
 *       ({@link Cocotje}). The whole town is protected ({@link KnuffeldalProtection}).</li>
 *   <li>The Grote Knusfeest: six feesttaakjes (feature.knus.Knusfeest), Kruimel-Mika's that steal on the way and leave
 *       a crumb trail ({@link KruimelMikaEntity}), the finale ({@link Feestbuffet}: knus_oorkonde, the title
 *       Knuffelburgemeester, the burgemeesterssjerp) and, after that, the seasonal Knusfeest ({@link KnusfeestEvenement}).</li>
 *   <li>The seasons in the town ({@link Seizoensactiviteiten}): bloesemkransjes, zonnehoedjes, bladerhoopjes,
 *       sneeuwpopguhs and sjaaltjes, and the plein's decoration follows the season.</li>
 * </ul>
 */
public final class KnuffeldalFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);

    public static final ResourceKey<Biome> KNUFFELDAL = ResourceKey.create(Registries.BIOME, Guhs.id("knuffeldal"));
    public static final ResourceKey<Structure> STADJE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("knuffeldal_stadje"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> PLUIZENBOOM = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("pluizenboom"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> REUZE_GUHPADDENSTOEL =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("reuze_guhpaddenstoel"));
    public static final TreeGrower PLUIZENBOOM_GROWER = new TreeGrower("guhs:pluizenboom", Optional.empty(), Optional.of(PLUIZENBOOM), Optional.empty());

    /** The town's structure type: exactly one town on the peak of every Knuffeldal. */
    public static final DeferredHolder<StructureType<?>, StructureType<KnuffeldalStadjeStructure>> STADJE_TYPE =
            STRUCTURE_TYPES.register("knuffeldal_stadje", () -> () -> KnuffeldalStadjeStructure.CODEC);

    private static BlockBehaviour.Properties steen() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_SANDSTONE).mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f);
    }

    private static BlockBehaviour.Properties dak() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.9f).sound(SoundType.WOOL).ignitedByLava();
    }

    // --- the biome ----------------------------------------------------------------------------------------------------
    public static final DeferredBlock<KnuffeldalBlocks.Knuffelgras> KNUFFELGRAS = BLOCKS.registerBlock("knuffelgras", KnuffeldalBlocks.Knuffelgras::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.6f).sound(SoundType.WOOL));
    public static final DeferredBlock<KnuffeldalBlocks.Pluisgras> PLUISGRAS = BLOCKS.registerBlock("pluisgras", KnuffeldalBlocks.Pluisgras::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.COLOR_PINK).sound(SoundType.WOOL));
    public static final DeferredBlock<RotatedPillarBlock> PLUIZENBOOM_STAM = BLOCKS.registerBlock("pluizenboom_stam", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LOG).mapColor(MapColor.TERRACOTTA_WHITE));
    public static final DeferredBlock<KnuffeldalBlocks.Bladeren> PLUIZENBOOM_BLADEREN = BLOCKS.registerBlock("pluizenboom_bladeren",
            KnuffeldalBlocks.Bladeren::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).mapColor(MapColor.COLOR_PINK).sound(SoundType.WOOL));
    public static final DeferredBlock<KnuffeldalBlocks.Zaailing> PLUIZENBOOM_ZAAILING = BLOCKS.registerBlock("pluizenboom_zaailing",
            KnuffeldalBlocks.Zaailing::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SAPLING).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<KnuffeldalBlocks.Guhpaddenstoel> GUHPADDENSTOEL = BLOCKS.registerBlock("guhpaddenstoel",
            KnuffeldalBlocks.Guhpaddenstoel::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollission().instabreak()
                    .sound(SoundType.GRASS).lightLevel(s -> 3).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<HugeMushroomBlock> GUHPADDENSTOEL_HOED = BLOCKS.registerBlock("guhpaddenstoel_hoed", HugeMushroomBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.RED_MUSHROOM_BLOCK).mapColor(MapColor.COLOR_PINK).lightLevel(s -> 6));
    public static final DeferredBlock<HugeMushroomBlock> GUHPADDENSTOEL_STEEL = BLOCKS.registerBlock("guhpaddenstoel_steel", HugeMushroomBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MUSHROOM_STEM).mapColor(MapColor.TERRACOTTA_WHITE));

    // --- the building palette (for every 2.8 building) ---------------------------------------------------------------------
    public static final DeferredBlock<Block> KNUFFELSTEEN = BLOCKS.registerSimpleBlock("knuffelsteen", () -> steen());
    public static final DeferredBlock<StairBlock> KNUFFELSTEEN_TRAP = BLOCKS.registerBlock("knuffelsteen_trap",
            p -> new StairBlock(KNUFFELSTEEN.get().defaultBlockState(), p), () -> steen());
    public static final DeferredBlock<SlabBlock> KNUFFELSTEEN_PLAAT = BLOCKS.registerBlock("knuffelsteen_plaat", SlabBlock::new, () -> steen());
    public static final DeferredBlock<WallBlock> KNUFFELSTEEN_MUUR = BLOCKS.registerBlock("knuffelsteen_muur", WallBlock::new, () -> steen().forceSolidOn());
    public static final DeferredBlock<KnuffeldalBlocks.Gezicht> KNUFFELSTEEN_GEZICHT = BLOCKS.registerBlock("knuffelsteen_gezicht",
            KnuffeldalBlocks.Gezicht::new, () -> steen());
    public static final DeferredBlock<Block> PLUISDAK = BLOCKS.registerSimpleBlock("pluisdak", () -> dak());
    public static final DeferredBlock<StairBlock> PLUISDAK_TRAP = BLOCKS.registerBlock("pluisdak_trap",
            p -> new StairBlock(PLUISDAK.get().defaultBlockState(), p), () -> dak());
    public static final DeferredBlock<SlabBlock> PLUISDAK_PLAAT = BLOCKS.registerBlock("pluisdak_plaat", SlabBlock::new, () -> dak());
    public static final DeferredBlock<Block> KNUFFELKLINKERS = BLOCKS.registerSimpleBlock("knuffelklinkers",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).mapColor(MapColor.COLOR_PINK));

    // --- the town, the seasons, the Knusfeest --------------------------------------------------------------------------------
    public static final DeferredBlock<KnuffeldalBlocks.Feestbuffettafel> FEESTBUFFETTAFEL = BLOCKS.registerBlock("feestbuffettafel",
            KnuffeldalBlocks.Feestbuffettafel::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f).sound(SoundType.WOOD)
                    .noOcclusion().ignitedByLava());
    public static final DeferredBlock<KnuffeldalBlocks.KnusOorkonde> KNUS_OORKONDE = BLOCKS.registerBlock("knus_oorkonde",
            KnuffeldalBlocks.KnusOorkonde::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.0f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<KnuffeldalBlocks.SeizoensBloembak> SEIZOENSBLOEMBAK = BLOCKS.registerBlock("seizoensbloembak",
            KnuffeldalBlocks.SeizoensBloembak::new, () -> steen().noOcclusion().randomTicks());
    public static final DeferredBlock<KnuffeldalBlocks.SeizoensSlinger> SEIZOENSSLINGER = BLOCKS.registerBlock("seizoensslinger",
            KnuffeldalBlocks.SeizoensSlinger::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.2f).sound(SoundType.WOOL)
                    .noCollission().noOcclusion().randomTicks().lightLevel(s -> s.getValue(KnuffeldalBlocks.SEIZOEN) == nl.juiced.guhs.feature.knus.Seizoen.WINTER ? 7 : 0));
    public static final DeferredBlock<KnuffeldalBlocks.Bladerhoopje> BLADERHOOPJE = BLOCKS.registerBlock("bladerhoopje",
            KnuffeldalBlocks.Bladerhoopje::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.2f).sound(SoundType.GRASS)
                    .noCollission().noOcclusion().randomTicks().pushReaction(PushReaction.DESTROY).ignitedByLava());
    public static final DeferredBlock<KnuffeldalBlocks.Sneeuwpopguh> SNEEUWPOPGUH = BLOCKS.registerBlock("sneeuwpopguh",
            KnuffeldalBlocks.Sneeuwpopguh::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SNOW_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY));

    // --- items -------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<KnuffeldalItems.Knusfeestlijstje> KNUSFEESTLIJSTJE = ITEMS.registerItem("knusfeestlijstje",
            KnuffeldalItems.Knusfeestlijstje::new, () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<KnuffeldalItems.Sneeuwguhkopje> SNEEUWGUHKOPJE = ITEMS.registerItem("sneeuwguhkopje",
            KnuffeldalItems.Sneeuwguhkopje::new, () -> new Item.Properties().stacksTo(16));

    // --- the Kruimel-Mika --------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<KruimelMikaEntity>> KRUIMEL_MIKA = ENTITY_TYPES.register("kruimel_mika",
            () -> EntityType.Builder.of(KruimelMikaEntity::new, MobCategory.CREATURE).sized(0.9f, 0.8f).eyeHeight(0.55f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse("guhs:kruimel_mika"))));
    public static final DeferredItem<DeferredSpawnEggItem> KRUIMEL_MIKA_SPAWN_EGG = ITEMS.registerItem("kruimel_mika_spawn_egg",
            p -> new DeferredSpawnEggItem(KRUIMEL_MIKA, 0xE9B478, 0x9A3A5A, p));

    static {
        for (DeferredBlock<?> block : List.of(KNUFFELGRAS, PLUIZENBOOM_STAM, PLUIZENBOOM_BLADEREN, GUHPADDENSTOEL_HOED, GUHPADDENSTOEL_STEEL,
                KNUFFELSTEEN, KNUFFELSTEEN_TRAP, KNUFFELSTEEN_PLAAT, KNUFFELSTEEN_MUUR, PLUISDAK, PLUISDAK_TRAP, PLUISDAK_PLAAT, KNUFFELKLINKERS,
                SEIZOENSSLINGER)) {
            ITEMS.registerSimpleBlockItem(block);
        }
        for (DeferredBlock<?> block : List.of(PLUISGRAS, PLUIZENBOOM_ZAAILING, GUHPADDENSTOEL, KNUFFELSTEEN_GEZICHT, FEESTBUFFETTAFEL,
                SEIZOENSBLOEMBAK, BLADERHOOPJE, SNEEUWPOPGUH)) {
            ITEMS.registerItem(block.getId().getPath(), p -> new KnuffeldalItems.LoreBlock(block.get(), p));
        }
        ITEMS.registerItem("knus_oorkonde", p -> new KnuffeldalItems.LoreBlock(KNUS_OORKONDE.get(), p), () -> new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));
    }

    // --- particles and sounds -------------------------------------------------------------------------------------------------
    /** A soft pink fluff drifting through the Knuffeldal. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLUISJE = PARTICLES.register("pluisje", () -> new SimpleParticleType(false));
    /** A cookie crumb: the Kruimel-Mika's crumb trail. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KRUIMEL = PARTICLES.register("kruimel", () -> new SimpleParticleType(true));
    /** A pink blossom petal (spring). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOESEMBLAADJE = PARTICLES.register("bloesemblaadje",
            () -> new SimpleParticleType(false));
    /** A little snowflake (winter). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SNEEUWVLOKJE = PARTICLES.register("sneeuwvlokje",
            () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> KRUIMEL_GIECHEL = sound("knuffeldal.kruimel_giechel");
    public static final DeferredHolder<SoundEvent, SoundEvent> FEESTBEL = sound("knuffeldal.feestbel");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEIZOEN_GELUID = sound("knuffeldal.seizoen");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    private static final Burgemeester BURGEMEESTER = new Burgemeester();
    private static final Cocotje COCOTJE = new Cocotje();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        modBus.addListener(KnuffeldalFeature::setup);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(KRUIMEL_MIKA.get(), KruimelMikaEntity.createAttributes().build()));
        KnuffeldalProtection.register();
        nl.juiced.guhs.feature.Protected.add(KnuffeldalProtection::inStadje);
        NeoForge.EVENT_BUS.register(KnuffeldalEvents.class);
        KnuffeldalEvents.hooks();
        KnuffeldalVoortgang.register();
        Seizoensactiviteiten.register();
        KruimelMikaEntity.register();
        GuhDex.creaturePage(GuhVariant.KRUIMEL_MIKA, KRUIMEL_MIKA);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            fire.setFlammable(PLUIZENBOOM_STAM.get(), 5, 5);
            fire.setFlammable(PLUIZENBOOM_BLADEREN.get(), 30, 60);
            fire.setFlammable(PLUISGRAS.get(), 60, 100);
            fire.setFlammable(BLADERHOOPJE.get(), 60, 100);
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        KnuffeldalPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var block : List.of(KNUFFELGRAS, PLUISGRAS, PLUIZENBOOM_STAM, PLUIZENBOOM_BLADEREN, PLUIZENBOOM_ZAAILING, GUHPADDENSTOEL,
                GUHPADDENSTOEL_HOED, GUHPADDENSTOEL_STEEL, KNUFFELSTEEN, KNUFFELSTEEN_TRAP, KNUFFELSTEEN_PLAAT, KNUFFELSTEEN_MUUR,
                KNUFFELSTEEN_GEZICHT, PLUISDAK, PLUISDAK_TRAP, PLUISDAK_PLAAT, KNUFFELKLINKERS, SEIZOENSBLOEMBAK, SEIZOENSSLINGER, BLADERHOOPJE,
                SNEEUWPOPGUH, FEESTBUFFETTAFEL, KNUS_OORKONDE)) {
            output.accept(new ItemStack(block.get()));
        }
        output.accept(new ItemStack(KNUSFEESTLIJSTJE.get()));
        output.accept(new ItemStack(SNEEUWGUHKOPJE.get()));
        output.accept(new ItemStack(KRUIMEL_MIKA_SPAWN_EGG.get()));
    }

    /** Burgemeester Vadsema: the Grote Knusfeest and the seasonal Knusfeest. */
    @Nullable
    public static NpcRole burgemeester() {
        return BURGEMEESTER;
    }

    /** Cocotje: "WEET JIJ WAAR ZE ZIJN??????" */
    @Nullable
    public static NpcRole cocotje() {
        return COCOTJE;
    }

    private KnuffeldalFeature() {
    }
}
