package nl.juiced.guhs.feature.bio.dieren;

import java.util.List;
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
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;

/**
 * biomes3 slice "dieren": the animals of the three new Guhmensie biomes.
 * <ul>
 *   <li>{@link KoiEntity de koi}: a calm school fish in guh style, five colours, in the ponds of the Klaterdal and the
 *       Bloesemmeertje. Comes to the surface for {@link KoivoerItem koivoer}, can be fed (hearts; now and then a kleintje,
 *       capped per pond) and carried in a {@link KoiEmmerItem koi-emmer} to your own pond, where it stays.</li>
 *   <li>{@link WolkenschaapjeEntity het wolkenschaapje}: a fluffy little sheep that floats just above the ground of the
 *       Wolkenweide. Shears give wolkenpluis (it grows back); knabbelvoer or a lead makes it yours.</li>
 *   <li>The existing kikkerguh on lily pads ({@link KikkerBlad}): sits on a leaf, hops into the water when you come
 *       close, croaks in the evening.</li>
 *   <li>The biome guhs ({@link BiomeGuhs}): bloesemguh (Bloesemmeertje), tanukiguh (Klaterdal) and the wolkguh, now also
 *       wild in the Wolkenweide; their evening nap by the water.</li>
 * </ul>
 * Nothing here piles up: every wild one a spawner brings is a come-and-go animal ({@link DierenRegels}), only what you
 * keep (a bucket, a lead, knabbelvoer, a name) is saved. Resources: tools/features/bio_dieren.py.
 */
public final class DierenSlice {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- entities ---------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<KoiEntity>> KOI = ENTITY_TYPES.register("koi",
            () -> EntityType.Builder.of(KoiEntity::new, MobCategory.WATER_AMBIENT).sized(0.6f, 0.4f).eyeHeight(0.26f)
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("koi"))));
    public static final DeferredHolder<EntityType<?>, EntityType<WolkenschaapjeEntity>> WOLKENSCHAAPJE = ENTITY_TYPES.register("wolkenschaapje",
            () -> EntityType.Builder.of(WolkenschaapjeEntity::new, MobCategory.CREATURE).sized(0.75f, 1.15f).eyeHeight(0.8f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("wolkenschaapje"))));

    // --- items ------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<KoiEmmerItem> KOI_EMMER = ITEMS.registerItem("koi_emmer", KoiEmmerItem::new,
            () -> new Item.Properties().stacksTo(1).component(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY));
    public static final DeferredItem<KoivoerItem> KOIVOER = ITEMS.registerItem("koivoer", KoivoerItem::new, () -> new Item.Properties());
    public static final DeferredItem<SpawnEggItem> KOI_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "koi_spawn_egg", KOI);
    public static final DeferredItem<SpawnEggItem> WOLKENSCHAAPJE_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "wolkenschaapje_spawn_egg", WOLKENSCHAAPJE);

    // --- sounds (tools/features/bio_dieren_geluid.py) ---------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> KOI_HAP = geluid("koi.hap");
    public static final DeferredHolder<SoundEvent, SoundEvent> KOI_STROOI = geluid("koi.strooi");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHAAPJE_BLEH = geluid("wolkenschaapje.bleh");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHAAPJE_PLUIS = geluid("wolkenschaapje.pluis");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** The two creature pages of this slice (bonus pages: GuhDex.EXTRA). */
    public static final List<GuhVariant> PAGINAS = List.of(GuhVariant.KOI, GuhVariant.WOLKENSCHAAPJE);

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(KOI.get(), KoiEntity.createAttributes().build());
            event.put(WOLKENSCHAAPJE.get(), WolkenschaapjeEntity.createAttributes().build());
        });
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> {
            event.register(KOI.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    KoiEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(WOLKENSCHAAPJE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    WolkenschaapjeEntity::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        GuhDex.creaturePage(GuhVariant.KOI, KOI, 5);
        GuhDex.creaturePage(GuhVariant.WOLKENSCHAAPJE, WOLKENSCHAAPJE, 4);
        NeoForge.EVENT_BUS.register(DierenEvents.class);
        BiomeGuhs.hooks();
        DierenEvents.zelftest();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var item : List.of(KOIVOER, KOI_EMMER, KOI_SPAWN_EGG, WOLKENSCHAAPJE_SPAWN_EGG)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private DierenSlice() {
    }
}
