package nl.juiced.guhs.feature.blub;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.boerderij.BoerderijDier;
import nl.juiced.guhs.feature.piep.PiepDierItem;

/**
 * 1.8.0 (Guhdromenland), TEST BUILD: the Blub-guh ({@link BlubGuhEntity}) with its Blub-Guh-Meter ({@link BlubTrekking}),
 * its menu ({@link BlubMenu}) and test commands ({@link BlubCommando}: /guhs blub ...).
 * <p>
 * Wild Blubs only show (and can be tamed) once you helped Snoes ({@link BlubSnoes}); tamed ones always.
 * <p>
 * Spawns: TIJDELIJK in the Wolkenweide and the Bloemenvallei of the Guhmension (biome modifier
 * data/guhs/neoforge/biome_modifier/blubguh_tijdelijk.json, from tools/features/blub.py: {@code SPAWNS}), until
 * Guhdromenland exists; then that one file moves to the dream dimension. Resources: tools/features/blub.py.
 */
public final class BlubFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BlubGuhEntity>> BLUBGUH = ENTITY_TYPES.register("blubguh",
            () -> EntityType.Builder.of(BlubGuhEntity::new, MobCategory.CREATURE).sized(0.5f, 0.5f).eyeHeight(0.36f)
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("blubguh"))));

    /** Picked up (the whole Blub in the item, meter and all). */
    public static final DeferredItem<PiepDierItem> BLUBGUH_ITEM = ITEMS.registerItem("blubguh_item",
            p -> new PiepDierItem(() -> BLUBGUH.get(), p), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<SpawnEggItem> BLUBGUH_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "blubguh_spawn_egg", BLUBGUH);


    // --- ronde 3b: the Blub's own chores (feature/blub/klus) -------------------------------------------------------------------
    /** The Blub-bubbelbadje: a little tub; a Blub splashes in it, foam (Bubbelbadmeester) makes guhs fris en blij. */
    public static final net.neoforged.neoforge.registries.DeferredBlock<nl.juiced.guhs.feature.blub.klus.BlubBadjeBlock> BADJE = BLOCKS.registerBlock(
            "blub_badje", nl.juiced.guhs.feature.blub.klus.BlubBadjeBlock::new, () -> net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_PINK).strength(1.0f)
                    .sound(net.minecraft.world.level.block.SoundType.STONE).noOcclusion());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> BADJE_ITEM = ITEMS.registerSimpleBlockItem(BADJE);
    /** Only the chore icons in the huisje screen (no creative tab): bellen, knabbels, liedje. */
    public static final java.util.Map<String, DeferredItem<Item>> KLUS_ICONEN = new java.util.LinkedHashMap<>();

    static {
        for (String k : java.util.List.of("bellen", "knabbels", "liedje")) {
            KLUS_ICONEN.put(k, ITEMS.registerSimpleItem("blubguh_klus_" + k));
        }
    }

    public static final DeferredHolder<net.minecraft.world.effect.MobEffect, nl.juiced.guhs.feature.blub.klus.FrisEnBlijEffect> FRIS_EN_BLIJ =
            EFFECTS.register("fris_en_blij", nl.juiced.guhs.feature.blub.klus.FrisEnBlijEffect::new);

    public static void register(IEventBus modBus) {
        BlubGeluid.values();                                  // (its sound events register themselves in SOUNDS)
        BlubSnoes.events();
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        EFFECTS.register(modBus);
        ENTITY_TYPES.register(modBus);
        nl.juiced.guhs.feature.blub.klus.BlubKlusjes.registreer();
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(BLUBGUH.get(), BlubGuhEntity.createAttributes().build()));
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> event.register(BLUBGUH.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (t, level, spawnType, pos, random) -> BoerderijDier.checkDierSpawnRules(level, spawnType, pos),
                RegisterSpawnPlacementsEvent.Operation.REPLACE));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> BlubCommando.register(event));
    }

    public static void payloads(PayloadRegistrar registrar) {
        BlubMenu.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BLUBGUH_SPAWN_EGG.get()));
        output.accept(new ItemStack(BADJE_ITEM.get()));
    }

    private BlubFeature() {
    }
}
