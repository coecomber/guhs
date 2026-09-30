package nl.juiced.guhs.feature.waterdiertjes;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
/**
 * 3.0 (Guhverhalen), slice waterdiertjes: the water and insect critters of the Guhmensie (DESIGN_30 §6). All lief, all
 * guh-inspired, none of them a guh:
 * <ul>
 *   <li>{@link GuhxolotlEntity de guhxolotl}: a vanilla-like axolotl with a guh head, five colours (rare gold), tameable with
 *       a guhvisje, scooped into a {@link GuhxolotlEmmertje}, a piep-maatje that can live in a guhhuisje;</li>
 *   <li>{@link GuhEendjeEntity het guh-eendje}: a mama duck with a rijtje kuikentjes on the ponds and the Guhzee;</li>
 *   <li>{@link KnabbelvlindertjeEntity het knabbelvlindertje}: butterflies around the flowers by day (four colours);</li>
 *   <li>{@link GlimguhtjeEntity het glimguhtje}: glowing fireflies at night over the Kaasmoeras and the Guhweides;</li>
 *   <li>{@link LieveheersbeestjeEntity het lieveheersbeestje}: ladybirds with guh spots that help guhtuintjes grow a tikje.</li>
 * </ul>
 * Each has a counting Guhdex page, spawns via the biome modifiers {@code waterdiertjes_*} (tools/features/waterdiertjes.py)
 * and advancements in the Diertjes tab ({@link WaterdiertjesEvents}).
 */
public final class WaterdiertjesFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- entities ---------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<GuhxolotlEntity>> GUHXOLOTL = ENTITY_TYPES.register("guhxolotl",
            () -> EntityType.Builder.of(GuhxolotlEntity::new, MobCategory.AXOLOTLS).sized(0.75f, 0.42f).eyeHeight(0.25f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guhxolotl"))));
    public static final DeferredHolder<EntityType<?>, EntityType<GuhEendjeEntity>> GUH_EENDJE = ENTITY_TYPES.register("guh_eendje",
            () -> EntityType.Builder.of(GuhEendjeEntity::new, MobCategory.CREATURE).sized(0.5f, 0.65f).eyeHeight(0.55f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guh_eendje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<KnabbelvlindertjeEntity>> KNABBELVLINDERTJE = ENTITY_TYPES.register("knabbelvlindertje",
            () -> EntityType.Builder.of(KnabbelvlindertjeEntity::new, MobCategory.AMBIENT).sized(0.4f, 0.3f).eyeHeight(0.15f)
                    .clientTrackingRange(6).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("knabbelvlindertje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<GlimguhtjeEntity>> GLIMGUHTJE = ENTITY_TYPES.register("glimguhtje",
            () -> EntityType.Builder.of(GlimguhtjeEntity::new, MobCategory.AMBIENT).sized(0.25f, 0.25f).eyeHeight(0.15f)
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("glimguhtje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<LieveheersbeestjeEntity>> LIEVEHEERSBEESTJE = ENTITY_TYPES.register("lieveheersbeestje",
            () -> EntityType.Builder.of(LieveheersbeestjeEntity::new, MobCategory.AMBIENT).sized(0.25f, 0.2f).eyeHeight(0.1f)
                    .clientTrackingRange(6).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("lieveheersbeestje"))));

    // --- items ------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<GuhxolotlEmmertje> GUHXOLOTL_EMMERTJE = ITEMS.registerItem("guhxolotl_emmertje",
            GuhxolotlEmmertje::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<DeferredSpawnEggItem> GUHXOLOTL_SPAWN_EGG = ITEMS.registerItem("guhxolotl_spawn_egg",
            p -> new DeferredSpawnEggItem(GUHXOLOTL, 0xF8B2D0, 0xD63A84, p));
    public static final DeferredItem<DeferredSpawnEggItem> GUH_EENDJE_SPAWN_EGG = ITEMS.registerItem("guh_eendje_spawn_egg",
            p -> new DeferredSpawnEggItem(GUH_EENDJE, 0xFFF8EC, 0xFA963C, p));
    public static final DeferredItem<DeferredSpawnEggItem> KNABBELVLINDERTJE_SPAWN_EGG = ITEMS.registerItem("knabbelvlindertje_spawn_egg",
            p -> new DeferredSpawnEggItem(KNABBELVLINDERTJE, 0xFCD656, 0xC4A8F0, p));
    public static final DeferredItem<DeferredSpawnEggItem> GLIMGUHTJE_SPAWN_EGG = ITEMS.registerItem("glimguhtje_spawn_egg",
            p -> new DeferredSpawnEggItem(GLIMGUHTJE, 0xFAE2EC, 0xD8F070, p));
    public static final DeferredItem<DeferredSpawnEggItem> LIEVEHEERSBEESTJE_SPAWN_EGG = ITEMS.registerItem("lieveheersbeestje_spawn_egg",
            p -> new DeferredSpawnEggItem(LIEVEHEERSBEESTJE, 0xDE2834, 0x1E1822, p));

    // --- sounds (tools/features/waterdiertjes_geluid.py) ------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> BLUB = geluid("waterdiertjes.guhxolotl_blub");
    public static final DeferredHolder<SoundEvent, SoundEvent> KWAK = geluid("waterdiertjes.eendje_kwak");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIEP = geluid("waterdiertjes.kuiken_piep");
    public static final DeferredHolder<SoundEvent, SoundEvent> TING = geluid("waterdiertjes.glimguhtje_ting");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZOEM = geluid("waterdiertjes.lieveheersbeestje_zoem");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** The five Guhdex pages of this slice (they count for "alles verzameld"). */
    public static final List<GuhVariant> PAGINAS = List.of(GuhVariant.GUHXOLOTL, GuhVariant.GUH_EENDJE, GuhVariant.KNABBELVLINDERTJE,
            GuhVariant.GLIMGUHTJE, GuhVariant.LIEVEHEERSBEESTJE);

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(GUHXOLOTL.get(), GuhxolotlEntity.createAttributes().build());
            event.put(GUH_EENDJE.get(), GuhEendjeEntity.createAttributes().build());
            event.put(KNABBELVLINDERTJE.get(), FladderDiertje.createAttributes().build());
            event.put(GLIMGUHTJE.get(), FladderDiertje.createAttributes().build());
            event.put(LIEVEHEERSBEESTJE.get(), FladderDiertje.createAttributes().build());
        });
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> {
            event.register(GUHXOLOTL.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    GuhxolotlEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(GUH_EENDJE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    GuhEendjeEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(KNABBELVLINDERTJE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    KnabbelvlindertjeEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(GLIMGUHTJE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    GlimguhtjeEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(LIEVEHEERSBEESTJE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    LieveheersbeestjeEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        GuhDex.creaturePage(GuhVariant.GUHXOLOTL, GUHXOLOTL, 4);
        GuhDex.creaturePage(GuhVariant.GUH_EENDJE, GUH_EENDJE, 5);
        GuhDex.creaturePage(GuhVariant.KNABBELVLINDERTJE, KNABBELVLINDERTJE, 6);
        GuhDex.creaturePage(GuhVariant.GLIMGUHTJE, GLIMGUHTJE, 6);
        GuhDex.creaturePage(GuhVariant.LIEVEHEERSBEESTJE, LIEVEHEERSBEESTJE, 5);
        NeoForge.EVENT_BUS.register(WaterdiertjesEvents.class);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GUHXOLOTL_EMMERTJE.get()));
        for (var egg : List.of(GUHXOLOTL_SPAWN_EGG, GUH_EENDJE_SPAWN_EGG, KNABBELVLINDERTJE_SPAWN_EGG, GLIMGUHTJE_SPAWN_EGG, LIEVEHEERSBEESTJE_SPAWN_EGG)) {
            output.accept(new ItemStack(egg.get()));
        }
    }

    private WaterdiertjesFeature() {
    }
}
