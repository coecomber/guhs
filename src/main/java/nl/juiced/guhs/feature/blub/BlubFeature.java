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
 * Spawns: TIJDELIJK in the Wolkenweide and the Bloemenvallei of the Guhmension (biome modifier
 * data/guhs/neoforge/biome_modifier/blubguh_tijdelijk.json, from tools/features/blub.py: {@code SPAWNS}), until
 * Guhdromenland exists; then that one file moves to the dream dimension. Resources: tools/features/blub.py.
 */
public final class BlubFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BlubGuhEntity>> BLUBGUH = ENTITY_TYPES.register("blubguh",
            () -> EntityType.Builder.of(BlubGuhEntity::new, MobCategory.CREATURE).sized(0.62f, 0.62f).eyeHeight(0.45f)
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("blubguh"))));

    /** Picked up (the whole Blub in the item, meter and all). */
    public static final DeferredItem<PiepDierItem> BLUBGUH_ITEM = ITEMS.registerItem("blubguh_item",
            p -> new PiepDierItem(() -> BLUBGUH.get(), p), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<SpawnEggItem> BLUBGUH_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "blubguh_spawn_egg", BLUBGUH);

    /** TIJDELIJK: the muisje's peeps, higher (sounds.json). */
    public static final DeferredHolder<SoundEvent, SoundEvent> PIEP = SOUNDS.register("blubguh.piep",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("blubguh.piep")));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
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
    }

    private BlubFeature() {
    }
}
