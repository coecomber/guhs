package nl.juiced.guhs.feature.landdiertjes;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
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
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.boerderij.BoerderijDier;
import nl.juiced.guhs.feature.gatenkaas.GatenkaasFeature;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.quest.GuhDex;

/**
 * 3.0 (Guhverhalen), slice landdiertjes: the little land critters of the Guhmensie (DESIGN_30 §6) and Sjokkel (§3).
 * <ul>
 *   <li>{@link PluisegeltjeEntity pluisegeltje} (Vadswoud; rolls up; sweet berries), {@link GuhKonijntjeEntity guh-konijntje}
 *       (Guhweides, white ones in the Sneeuwguhtoendra; lop ears; carrots / kaasknabbels), {@link PluiseekhoorntjeEntity
 *       pluiseekhoorntje} (Vadswoud and the guhbloesem biomes; knabbel stashes {@link KnabbelvoorraadjeBlock}; your shoulder;
 *       kaasknabbels) and {@link ShuckleEntity Sjokkel} (the kloon-eiland via {@link ShucklePlekjeBlock shuckle-plekjes}, rarely
 *       the Gatenkaasgrotten; hides in its shell; berries -> bessensapje).</li>
 *   <li>All four: tameable, pick-up-able ({@code <id>_item}, {@link PiepDierItem}), Guhhuisje residents, their own little menu
 *       (the piep-maatje menu) with their own big button, a counting Guhdex page.</li>
 *   <li>Sjokkel's chore {@link PolijstenKlus stenen polijsten}: cobblestone from the chest into smooth stone and
 *       {@code landdiertjes_guhsteentje} pebbles (4 of them make a {@code landdiertjes_steentjespad}).</li>
 * </ul>
 * Resources: tools/features/landdiertjes.py (models: landdiertjes_modellen.py).
 */
public final class LanddiertjesFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    /** Random salt of this slice (CONTRACT_30 §2). */
    public static final long SALT = 20301501L;

    /**
     * An invisible, no-collision marker (no item): mewtwo puts 5-8 on the rocky coast of the kloon-eiland; each keeps 1-2
     * Sjokkels around ({@link ShucklePlekjeBlock}).
     */
    public static final DeferredBlock<Block> SHUCKLE_PLEKJE = BLOCKS.register("shuckle_plekje", () -> new ShucklePlekjeBlock(BlockBehaviour.Properties.of()
            .noCollission().noLootTable().strength(-1f, 3600000f).noOcclusion().pushReaction(PushReaction.BLOCK).randomTicks()
            .isValidSpawn((s, l, p, t) -> false)));
    /** A squirrel's stash of kaasknabbels (no item). */
    public static final DeferredBlock<KnabbelvoorraadjeBlock> KNABBELVOORRAADJE = BLOCKS.register("landdiertjes_knabbelvoorraadje",
            () -> new KnabbelvoorraadjeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.3f).sound(SoundType.ROOTED_DIRT)
                    .noCollission().noOcclusion().pushReaction(PushReaction.DESTROY).replaceable()));
    /** A path of polished guhsteentjes (like a carpet). */
    public static final DeferredBlock<CarpetBlock> STEENTJESPAD = BLOCKS.register("landdiertjes_steentjespad",
            () -> new CarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.4f).sound(SoundType.STONE)));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> STEENTJESPAD_ITEM = ITEMS.registerSimpleBlockItem(STEENTJESPAD);

    // --- the critters ------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<PluisegeltjeEntity>> PLUISEGELTJE = ENTITY_TYPES.register("pluisegeltje",
            () -> EntityType.Builder.of(PluisegeltjeEntity::new, MobCategory.CREATURE).sized(0.5f, 0.42f).eyeHeight(0.3f)
                    .clientTrackingRange(8).build(Guhs.id("pluisegeltje").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<GuhKonijntjeEntity>> GUH_KONIJNTJE = ENTITY_TYPES.register("guh_konijntje",
            () -> EntityType.Builder.of(GuhKonijntjeEntity::new, MobCategory.CREATURE).sized(0.45f, 0.55f).eyeHeight(0.42f)
                    .clientTrackingRange(8).build(Guhs.id("guh_konijntje").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<PluiseekhoorntjeEntity>> PLUISEEKHOORNTJE = ENTITY_TYPES.register("pluiseekhoorntje",
            () -> EntityType.Builder.of(PluiseekhoorntjeEntity::new, MobCategory.CREATURE).sized(0.42f, 0.5f).eyeHeight(0.38f)
                    .clientTrackingRange(8).build(Guhs.id("pluiseekhoorntje").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ShuckleEntity>> SHUCKLE = ENTITY_TYPES.register("shuckle",
            () -> EntityType.Builder.of(ShuckleEntity::new, MobCategory.CREATURE).sized(0.62f, 0.55f).eyeHeight(0.35f)
                    .clientTrackingRange(8).build(Guhs.id("shuckle").toString()));

    // --- picked up (the whole critter in the item) -------------------------------------------------------------------------------
    public static final DeferredItem<PiepDierItem> PLUISEGELTJE_ITEM = ITEMS.registerItem("pluisegeltje_item",
            p -> new PiepDierItem(() -> PLUISEGELTJE.get(), p), new Item.Properties().stacksTo(1));
    public static final DeferredItem<PiepDierItem> GUH_KONIJNTJE_ITEM = ITEMS.registerItem("guh_konijntje_item",
            p -> new PiepDierItem(() -> GUH_KONIJNTJE.get(), p), new Item.Properties().stacksTo(1));
    public static final DeferredItem<LanddierItems.EekhoorntjeItem> PLUISEEKHOORNTJE_ITEM = ITEMS.registerItem("pluiseekhoorntje_item",
            LanddierItems.EekhoorntjeItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<PiepDierItem> SHUCKLE_ITEM = ITEMS.registerItem("shuckle_item",
            p -> new PiepDierItem(() -> SHUCKLE.get(), p), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<DeferredSpawnEggItem> PLUISEGELTJE_SPAWN_EGG = ITEMS.registerItem("pluisegeltje_spawn_egg",
            p -> new DeferredSpawnEggItem(PLUISEGELTJE, 0xF2D2C4, 0xB77A6A, p));
    public static final DeferredItem<DeferredSpawnEggItem> GUH_KONIJNTJE_SPAWN_EGG = ITEMS.registerItem("guh_konijntje_spawn_egg",
            p -> new DeferredSpawnEggItem(GUH_KONIJNTJE, 0xF8C6D8, 0xFFF4F8, p));
    public static final DeferredItem<DeferredSpawnEggItem> PLUISEEKHOORNTJE_SPAWN_EGG = ITEMS.registerItem("pluiseekhoorntje_spawn_egg",
            p -> new DeferredSpawnEggItem(PLUISEEKHOORNTJE, 0xE08A4E, 0xFFE6C8, p));
    public static final DeferredItem<DeferredSpawnEggItem> SHUCKLE_SPAWN_EGG = ITEMS.registerItem("shuckle_spawn_egg",
            p -> new DeferredSpawnEggItem(SHUCKLE, 0xD8323A, 0xF6D64A, p));

    // --- Sjokkel's things ----------------------------------------------------------------------------------------------------------
    public static final FoodProperties SAPJE = new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible()
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 0), 1f).build();
    public static final DeferredItem<LanddierItems.BessensapjeItem> BESSENSAPJE = ITEMS.registerItem("landdiertjes_bessensapje",
            LanddierItems.BessensapjeItem::new, new Item.Properties().stacksTo(16).food(SAPJE).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<LanddierItems.LoreItem> GUHSTEENTJE = ITEMS.registerItem("landdiertjes_guhsteentje",
            LanddierItems.LoreItem::new, new Item.Properties());

    // --- sounds (vanilla sounds, pitched, in sounds.json) -----------------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> EGELTJE_SNUF = sound("landdiertjes.egeltje");
    public static final DeferredHolder<SoundEvent, SoundEvent> EGELTJE_ROL = sound("landdiertjes.egeltje.rol");
    public static final DeferredHolder<SoundEvent, SoundEvent> KONIJNTJE = sound("landdiertjes.konijntje");
    public static final DeferredHolder<SoundEvent, SoundEvent> EEKHOORNTJE = sound("landdiertjes.eekhoorntje");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHUCKLE_GELUID = sound("landdiertjes.shuckle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHUCKLE_DICHT = sound("landdiertjes.shuckle.dicht");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHUCKLE_OPEN = sound("landdiertjes.shuckle.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHUCKLE_TIK = sound("landdiertjes.shuckle.tik");
    public static final DeferredHolder<SoundEvent, SoundEvent> POLIJSTEN = sound("landdiertjes.polijsten");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** The one PolijstenKlus (registered in the klusjes registry). */
    public static final PolijstenKlus POLIJSTEN_KLUS = new PolijstenKlus();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(PLUISEGELTJE.get(), PluisegeltjeEntity.createAttributes().build());
            event.put(GUH_KONIJNTJE.get(), GuhKonijntjeEntity.createAttributes().build());
            event.put(PLUISEEKHOORNTJE.get(), PluiseekhoorntjeEntity.createAttributes().build());
            event.put(SHUCKLE.get(), ShuckleEntity.createAttributes().build());
        });
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> {
            for (var type : List.of(PLUISEGELTJE, GUH_KONIJNTJE, PLUISEEKHOORNTJE)) {
                event.register(type.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        (t, level, spawnType, pos, random) -> BoerderijDier.checkDierSpawnRules(level, spawnType, pos),
                        RegisterSpawnPlacementsEvent.Operation.REPLACE);
            }
            event.register(SHUCKLE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, spawnType, pos, random) -> shuckleMagSpawnen(level, spawnType, pos, random),
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        GuhDex.creaturePage(GuhVariant.PLUISEGELTJE, PLUISEGELTJE);
        GuhDex.creaturePage(GuhVariant.GUH_KONIJNTJE, GUH_KONIJNTJE);
        GuhDex.creaturePage(GuhVariant.PLUISEEKHOORNTJE, PLUISEEKHOORNTJE);
        GuhDex.creaturePage(GuhVariant.SHUCKLE, SHUCKLE);
        Klusjes.registreer(POLIJSTEN_KLUS);
        NeoForge.EVENT_BUS.register(LanddiertjesEvents.class);
    }

    /**
     * Sjokkel's natural spawns (the biome modifier puts it only in the Gatenkaasgrotten): rarely, deep down (below y 48) on
     * solid rock/cheese, and never when another Sjokkel is within 32 blocks. Spawn eggs, commands and the plekjes always work.
     */
    public static boolean shuckleMagSpawnen(ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) {
            return true;
        }
        if (pos.getY() > 48 || random.nextInt(6) != 0 || !level.getBiome(pos).is(GatenkaasFeature.GATENKAASGROTTEN)) {
            return false;
        }
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
            return false;
        }
        return level.getEntitiesOfClass(ShuckleEntity.class, new AABB(pos).inflate(32)).isEmpty();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var item : List.of(PLUISEGELTJE_SPAWN_EGG, GUH_KONIJNTJE_SPAWN_EGG, PLUISEEKHOORNTJE_SPAWN_EGG, SHUCKLE_SPAWN_EGG)) {
            output.accept(new ItemStack(item.get()));
        }
        output.accept(new ItemStack(BESSENSAPJE.get()));
        output.accept(new ItemStack(GUHSTEENTJE.get()));
        output.accept(new ItemStack(STEENTJESPAD_ITEM.get()));
    }

    private LanddiertjesFeature() {
    }
}
