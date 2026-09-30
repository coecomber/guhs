package nl.juiced.guhs.feature.vadswoud;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.NpcRole;

/**
 * Het Vadswoud (2.7, slice 5): a misty mint forest of giant guh trees in the Guhmension, the tree-house village and
 * the guh families. tools/features/vadswoud.py makes the resources (biome, worldgen, the village template...).
 * <ul>
 *   <li>The wood set vadshout_* ({@link VadshoutBlocks}), the giant guh tree ({@link ReuzenguhboomFeature}) with little
 *       guh faces in its bark ({@link VadshoutBlocks.Gezicht}), knabbelbessen ({@link KnabbelbessenstruikBlock}),
 *       rope ({@code vadstouw}) and the guh nest ({@link GuhnestjeBlock}).</li>
 *   <li>Guh families ({@link GuhGezin}): wild guhs in the Guhmension spawn as a family, babies walk in a line behind their
 *       parent ({@link FollowFamilyLineGoal}), everyone (tame guhs too) sleeps in a nest at night ({@link SleepInNestGoal}),
 *       and babies are easier to tame.</li>
 *   <li>The Boswachterguh ({@link Boswachterguh}) and the Knabbelplukker ({@link Knabbelplukker}) in the boomhutdorp.</li>
 * </ul>
 */
public final class VadswoudFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    public static final ResourceKey<Biome> VADSWOUD = ResourceKey.create(Registries.BIOME, Guhs.id("vadswoud"));
    public static final ResourceKey<Structure> BOOMHUTDORP = ResourceKey.create(Registries.STRUCTURE, Guhs.id("boomhutdorp"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> REUZENGUHBOOM_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("reuzenguhboom"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> VADSHOUT_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("vadshout_boom"));

    public static final BlockSetType VADSHOUT_SET = BlockSetType.register(new BlockSetType("guhs_vadshout"));
    public static final WoodType VADSHOUT_WOOD = new WoodType("guhs_vadshout", VADSHOUT_SET);
    /** One sapling: a vadshout tree; four in a square: a giant guh tree. */
    public static final TreeGrower GROWER = new TreeGrower("guhs:vadshout", Optional.of(REUZENGUHBOOM_TREE), Optional.of(VADSHOUT_TREE), Optional.empty());

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).mapColor(MapColor.COLOR_PINK);
    }

    // --- the wood set ---------------------------------------------------------------------------------------------------
    public static final DeferredBlock<RotatedPillarBlock> VADSHOUT_GESTRIPT = BLOCKS.registerBlock("vadshout_gestript", RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_DARK_OAK_LOG).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<VadshoutBlocks.Log> VADSHOUT_STAM = BLOCKS.registerBlock("vadshout_stam",
            p -> new VadshoutBlocks.Log(() -> VADSHOUT_GESTRIPT.get(), p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG).mapColor(MapColor.TERRACOTTA_PINK));
    public static final DeferredBlock<VadshoutBlocks.Gezicht> VADSHOUT_GEZICHT = BLOCKS.registerBlock("vadshout_gezicht", VadshoutBlocks.Gezicht::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG).mapColor(MapColor.TERRACOTTA_PINK));
    public static final DeferredBlock<Block> VADSHOUT_PLANKEN = BLOCKS.registerSimpleBlock("vadshout_planken", wood());
    public static final DeferredBlock<StairBlock> VADSHOUT_TRAP = BLOCKS.registerBlock("vadshout_trap",
            p -> new StairBlock(VADSHOUT_PLANKEN.get().defaultBlockState(), p), wood());
    public static final DeferredBlock<SlabBlock> VADSHOUT_PLAAT = BLOCKS.registerBlock("vadshout_plaat", SlabBlock::new, wood());
    public static final DeferredBlock<FenceBlock> VADSHOUT_HEK = BLOCKS.registerBlock("vadshout_hek", FenceBlock::new, wood());
    public static final DeferredBlock<FenceGateBlock> VADSHOUT_POORT = BLOCKS.registerBlock("vadshout_poort",
            p -> new FenceGateBlock(VADSHOUT_WOOD, p), wood().forceSolidOn());
    public static final DeferredBlock<DoorBlock> VADSHOUT_DEUR = BLOCKS.registerBlock("vadshout_deur",
            p -> new DoorBlock(VADSHOUT_SET, p), wood().strength(3f).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<TrapDoorBlock> VADSHOUT_LUIK = BLOCKS.registerBlock("vadshout_luik",
            p -> new TrapDoorBlock(VADSHOUT_SET, p), wood().strength(3f).noOcclusion().isValidSpawn((s, l, pos, e) -> false));
    public static final DeferredBlock<VadshoutBlocks.Leaves> VADSHOUT_BLADEREN = BLOCKS.registerBlock("vadshout_bladeren", VadshoutBlocks.Leaves::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA_LEAVES).mapColor(MapColor.COLOR_LIGHT_GREEN));
    public static final DeferredBlock<VadshoutBlocks.Sapling> VADSHOUT_ZAAILING = BLOCKS.registerBlock("vadshout_zaailing", VadshoutBlocks.Sapling::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_SAPLING).mapColor(MapColor.COLOR_LIGHT_GREEN));

    // --- the forest floor, the nest, the bush, the rope -----------------------------------------------------------------
    public static final DeferredBlock<Block> VADSMOS = BLOCKS.registerSimpleBlock("vadsmos",
            BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK).mapColor(MapColor.COLOR_LIGHT_GREEN));
    public static final DeferredBlock<GuhnestjeBlock> GUHNESTJE = BLOCKS.registerBlock("guhnestje", GuhnestjeBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6f).sound(SoundType.GRASS).noOcclusion()
                    .ignitedByLava());
    public static final DeferredBlock<KnabbelbessenstruikBlock> KNABBELBESSENSTRUIK = BLOCKS.registerBlock("knabbelbessenstruik",
            KnabbelbessenstruikBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH).mapColor(MapColor.COLOR_LIGHT_GREEN));
    public static final DeferredBlock<ChainBlock> VADSTOUW = BLOCKS.registerBlock("vadstouw", ChainBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.8f).sound(SoundType.WOOL).noOcclusion().ignitedByLava());

    // --- items ----------------------------------------------------------------------------------------------------------
    public static final DeferredItem<VadsItems.Bessen> KNABBELBESSEN = ITEMS.registerItem("knabbelbessen",
            p -> new VadsItems.Bessen(KNABBELBESSENSTRUIK.get(), p),
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).build()));
    public static final DeferredItem<Item> KNABBELBESSENTAARTJE = ITEMS.registerSimpleItem("knabbelbessentaartje",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.6f).build()));

    static {
        for (DeferredBlock<?> block : List.of(VADSHOUT_STAM, VADSHOUT_GESTRIPT, VADSHOUT_PLANKEN, VADSHOUT_TRAP, VADSHOUT_PLAAT, VADSHOUT_HEK,
                VADSHOUT_POORT, VADSHOUT_DEUR, VADSHOUT_LUIK, VADSHOUT_BLADEREN, VADSMOS, VADSTOUW)) {
            ITEMS.registerSimpleBlockItem(block);
        }
        ITEMS.registerItem("vadshout_zaailing", p -> new VadsItems.LoreBlock(VADSHOUT_ZAAILING.get(), p));
        ITEMS.registerItem("vadshout_gezicht", p -> new VadsItems.LoreBlock(VADSHOUT_GEZICHT.get(), p));
        ITEMS.registerItem("guhnestje", p -> new VadsItems.LoreBlock(GUHNESTJE.get(), p));
    }

    // --- worldgen, the nest's point of interest, particles ------------------------------------------------------------------
    public static final DeferredHolder<Feature<?>, ReuzenguhboomFeature> REUZENGUHBOOM = FEATURES.register("reuzenguhboom",
            () -> new ReuzenguhboomFeature(NoneFeatureConfiguration.CODEC));
    /** Guhs find the nests near them at night. */
    public static final DeferredHolder<PoiType, PoiType> NEST_POI = POI_TYPES.register("guhnestje",
            () -> new PoiType(ImmutableSet.copyOf(GUHNESTJE.get().getStateDefinition().getPossibleStates()), 0, 1));
    /** Glowing fluff drifting through the Vadswoud (the biome's ambient particle). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VADSPLUISJE = PARTICLES.register("vadspluisje",
            () -> new SimpleParticleType(false));
    /** Little mint leaves falling from the vadshout leaves. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VADSBLAADJE = PARTICLES.register("vadsblaadje",
            () -> new SimpleParticleType(false));
    /** The Zzz of a guh asleep in its nest. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUH_ZZZ = PARTICLES.register("nestje_zzz",
            () -> new SimpleParticleType(false));

    private static final Boswachterguh BOSWACHTERGUH = new Boswachterguh();
    private static final Knabbelplukker KNABBELPLUKKER = new Knabbelplukker();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        POI_TYPES.register(modBus);
        PARTICLES.register(modBus);
        modBus.addListener(VadswoudFeature::setup);
        NeoForge.EVENT_BUS.register(GuhGezin.class);
        // (the eyes open again when a guh was saved asleep and loads awake)
        nl.juiced.guhs.feature.knus.GuhHooks.tick(guh -> {
            if (guh.tickCount % 20 == 0 && nl.juiced.guhs.feature.knus.GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT) && !SleepInNestGoal.isAsleep(guh)) {
                nl.juiced.guhs.feature.knus.GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, false);
            }
        });
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            for (Block log : List.of(VADSHOUT_STAM.get(), VADSHOUT_GESTRIPT.get(), VADSHOUT_GEZICHT.get())) {
                fire.setFlammable(log, 5, 5);
            }
            for (Block planks : List.of(VADSHOUT_PLANKEN.get(), VADSHOUT_TRAP.get(), VADSHOUT_PLAAT.get(), VADSHOUT_HEK.get(), VADSHOUT_POORT.get())) {
                fire.setFlammable(planks, 5, 20);
            }
            fire.setFlammable(VADSHOUT_BLADEREN.get(), 30, 60);
            fire.setFlammable(GUHNESTJE.get(), 60, 20);
            fire.setFlammable(VADSTOUW.get(), 15, 60);
            fire.setFlammable(KNABBELBESSENSTRUIK.get(), 60, 100);
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var block : List.of(VADSHOUT_STAM, VADSHOUT_GESTRIPT, VADSHOUT_GEZICHT, VADSHOUT_PLANKEN, VADSHOUT_TRAP, VADSHOUT_PLAAT, VADSHOUT_HEK,
                VADSHOUT_POORT, VADSHOUT_DEUR, VADSHOUT_LUIK, VADSHOUT_BLADEREN, VADSHOUT_ZAAILING, VADSMOS, GUHNESTJE, VADSTOUW)) {
            output.accept(new ItemStack(block.get()));
        }
        output.accept(new ItemStack(KNABBELBESSEN.get()));
        output.accept(new ItemStack(KNABBELBESSENTAARTJE.get()));
    }

    /** The Boswachterguh (wood, saplings, nests, the ranger outfit). */
    @Nullable
    public static NpcRole boswachterguh() {
        return BOSWACHTERGUH;
    }

    /** The Knabbelplukker (trades knabbelbessen; the picker's outfit). */
    @Nullable
    public static NpcRole knabbelplukker() {
        return KNABBELPLUKKER;
    }

    /** A block item for {@link BlockItem} lookups in the tests. */
    public static Item item(DeferredBlock<?> block) {
        return block.get().asItem();
    }

    private VadswoudFeature() {
    }
}
