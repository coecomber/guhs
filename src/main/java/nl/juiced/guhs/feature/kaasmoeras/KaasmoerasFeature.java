package nl.juiced.guhs.feature.kaasmoeras;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.MudBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
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

/**
 * Het Kaasmoeras: a misty, bubbling swamp biome in the Guhmension (tools/features/kaasmoeras.py makes the biome, the
 * pools and the paalhut).
 * <ul>
 *   <li>Borrelende kaassaus ({@link BorrelendeKaassausBlock}) bounces you up high; modderig kaasgras, kaasmodder,
 *       kaasriet and moerasgras cover the ground; pools with lily pads and knabbelvlotjes (little rafts with loot) are
 *       made by {@link KaasmoerasPoelFeature}.</li>
 *   <li>The Moerasheks-Mika ({@link MoerasheksMikaEntity}) lives in her paalhut and throws vadsverdrijvende drankjes
 *       ({@link VadsverdrijvendDrankjeEntity}: a short {@link OnvahoegEffect}); she drops moeraskaas (a brewing
 *       ingredient).</li>
 *   <li>Kikkerguhs ({@link KikkerguhEntity}, three colours) snap up kaasmotten ({@link KaasmotEntity}, little Mika
 *       moths that steal kaasknabbels) and drop a motknabbel ({@link MotknabbelBlock}) in their own colour.</li>
 *   <li>Wild guhs born here are often a Kaasmoerasguh ({@link KaasmoerasEvents}).</li>
 * </ul>
 */
public final class KaasmoerasFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);

    public static final ResourceKey<Biome> KAASMOERAS = ResourceKey.create(Registries.BIOME, Guhs.id("kaasmoeras"));
    public static final ResourceKey<Structure> HUT = ResourceKey.create(Registries.STRUCTURE, Guhs.id("moerasheks_hut"));

    // --- blocks --------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<BorrelendeKaassausBlock> BORRELENDE_KAASSAUS = BLOCKS.registerBlock("borrelende_kaassaus",
            BorrelendeKaassausBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.6f).sound(SoundType.HONEY_BLOCK)
                    .lightLevel(s -> 6).speedFactor(0.8f).isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.NORMAL));
    public static final DeferredBlock<MudBlock> KAASMODDER = BLOCKS.registerBlock("kaasmodder", MudBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.MUD).mapColor(MapColor.TERRACOTTA_YELLOW));
    public static final DeferredBlock<ModderigKaasgrasBlock> MODDERIG_KAASGRAS = BLOCKS.registerBlock("modderig_kaasgras",
            ModderigKaasgrasBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).mapColor(MapColor.COLOR_YELLOW).randomTicks());
    public static final DeferredBlock<DoublePlantBlock> KAASRIET = BLOCKS.registerBlock("kaasriet", DoublePlantBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS).mapColor(MapColor.COLOR_YELLOW));
    public static final DeferredBlock<MoerasgrasBlock> MOERASGRAS = BLOCKS.registerBlock("moerasgras", MoerasgrasBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.COLOR_YELLOW));
    /** The froglight of the kaasmoeras: one block, three colours (the colour of the kikkerguh that made it). */
    public static final DeferredBlock<MotknabbelBlock> MOTKNABBEL = BLOCKS.registerBlock("motknabbel", MotknabbelBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OCHRE_FROGLIGHT).mapColor(MapColor.COLOR_YELLOW));

    // --- items ----------------------------------------------------------------------------------------------------------
    /** Stinky, runny swamp cheese: the Moerasheks-Mika's treasure and a brewing ingredient (the guhbrouwketel). */
    public static final DeferredItem<Item> MOERASKAAS = ITEMS.registerSimpleItem("moeraskaas", new Item.Properties().rarity(Rarity.UNCOMMON)
            .food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 100, 0), 0.3f).build()));
    public static final DeferredItem<VadsverdrijvendDrankjeItem> VADSVERDRIJVEND_DRANKJE = ITEMS.registerItem("vadsverdrijvend_drankje",
            VadsverdrijvendDrankjeItem::new, new Item.Properties().stacksTo(16));

    // --- the mob effect of the drankje ----------------------------------------------------------------------------------
    public static final DeferredHolder<MobEffect, OnvahoegEffect> ONVAHOEG = EFFECTS.register("onvahoeg", OnvahoegEffect::new);

    // --- entities -------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<KikkerguhEntity>> KIKKERGUH = ENTITY_TYPES.register("kikkerguh",
            () -> EntityType.Builder.of(KikkerguhEntity::new, MobCategory.CREATURE).sized(0.6f, 0.8f).eyeHeight(0.55f)
                    .clientTrackingRange(10).build(Guhs.id("kikkerguh").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<KaasmotEntity>> KAASMOT = ENTITY_TYPES.register("kaasmot",
            () -> EntityType.Builder.of(KaasmotEntity::new, MobCategory.AMBIENT).sized(0.4f, 0.35f).eyeHeight(0.2f)
                    .clientTrackingRange(6).build(Guhs.id("kaasmot").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<MoerasheksMikaEntity>> MOERASHEKS_MIKA = ENTITY_TYPES.register("moerasheks_mika",
            () -> EntityType.Builder.of(MoerasheksMikaEntity::new, MobCategory.MONSTER).sized(0.9f, 1.3f).eyeHeight(0.8f)
                    .clientTrackingRange(10).build(Guhs.id("moerasheks_mika").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<VadsverdrijvendDrankjeEntity>> DRANKJE = ENTITY_TYPES.register("vadsverdrijvend_drankje",
            () -> EntityType.Builder.<VadsverdrijvendDrankjeEntity>of(VadsverdrijvendDrankjeEntity::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(4).updateInterval(10).build(Guhs.id("vadsverdrijvend_drankje").toString()));

    public static final DeferredItem<DeferredSpawnEggItem> KIKKERGUH_SPAWN_EGG = ITEMS.registerItem("kikkerguh_spawn_egg",
            p -> new DeferredSpawnEggItem(KIKKERGUH, 0xF08CB4, 0x9CC84A, p));
    public static final DeferredItem<DeferredSpawnEggItem> KAASMOT_SPAWN_EGG = ITEMS.registerItem("kaasmot_spawn_egg",
            p -> new DeferredSpawnEggItem(KAASMOT, 0x4A2A48, 0xF7C83C, p));
    public static final DeferredItem<DeferredSpawnEggItem> MOERASHEKS_MIKA_SPAWN_EGG = ITEMS.registerItem("moerasheks_mika_spawn_egg",
            p -> new DeferredSpawnEggItem(MOERASHEKS_MIKA, 0x7FA046, 0x4B2A6A, p));

    // --- worldgen -------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<Feature<?>, KaasmoerasPoelFeature> POEL = FEATURES.register("kaasmoeras_poel",
            () -> new KaasmoerasPoelFeature(NoneFeatureConfiguration.CODEC));

    static {
        ITEMS.registerSimpleBlockItem(BORRELENDE_KAASSAUS);
        ITEMS.registerSimpleBlockItem(KAASMODDER);
        ITEMS.registerSimpleBlockItem(MODDERIG_KAASGRAS);
        ITEMS.registerItem("kaasriet", p -> new DoubleHighBlockItem(KAASRIET.get(), p));
        ITEMS.registerSimpleBlockItem(MOERASGRAS);
    }

    public static final DeferredItem<MotknabbelBlock.Item> MOTKNABBEL_ITEM = ITEMS.registerItem("motknabbel",
            p -> new MotknabbelBlock.Item(MOTKNABBEL.get(), p));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        EFFECTS.register(modBus);
        FEATURES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(KIKKERGUH.get(), KikkerguhEntity.createAttributes().build());
            event.put(KAASMOT.get(), KaasmotEntity.createAttributes().build());
            event.put(MOERASHEKS_MIKA.get(), MoerasheksMikaEntity.createAttributes().build());
        });
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> {
            event.register(KIKKERGUH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    KikkerguhEntity::checkKikkerguhSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(KAASMOT.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    KaasmotEntity::checkKaasmotSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(MOERASHEKS_MIKA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    MoerasheksMikaEntity::checkMoerasheksSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        NeoForge.EVENT_BUS.register(KaasmoerasEvents.class);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(MOERASKAAS.get()));
        output.accept(new ItemStack(VADSVERDRIJVEND_DRANKJE.get()));
        for (var block : java.util.List.of(BORRELENDE_KAASSAUS, MODDERIG_KAASGRAS, KAASMODDER, KAASRIET, MOERASGRAS)) {
            output.accept(new ItemStack(block.get()));
        }
        for (MotknabbelBlock.Kleur kleur : MotknabbelBlock.Kleur.values()) {
            output.accept(MotknabbelBlock.stack(kleur, 1));
        }
        for (var egg : java.util.List.of(KIKKERGUH_SPAWN_EGG, KAASMOT_SPAWN_EGG, MOERASHEKS_MIKA_SPAWN_EGG)) {
            output.accept(new ItemStack(egg.get()));
        }
    }

    @Nullable
    public static NpcRole role() {
        return null;
    }

    private KaasmoerasFeature() {
    }
}
