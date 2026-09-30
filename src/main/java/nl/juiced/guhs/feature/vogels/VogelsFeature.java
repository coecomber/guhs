package nl.juiced.guhs.feature.vogels;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
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
import nl.juiced.guhs.quest.GuhDex;

/**
 * 3.0 (Guhverhalen), slice vogels: De vogeltjes (DESIGN_30 §6). Four little birds make the Guhmensie livelier. They are
 * birds, not guhs, but each has a guh twist (glossy guh eyes, a blush, two round guh ears):
 * <ul>
 *   <li>{@link PluisvinkjeEntity}: round pink-white finches in flocks in the Guhvelden, the Roze pluisjes and the Guhweides
 *   (on the guhbloesem trees); now and then they drop a {@link #PLUISVEERTJE} (the hemel quest asks one), more often when
 *   you give them seeds or they eat at a {@link VoerhuisjeBlock}.</li>
 *   <li>{@link KaasmeesjeEntity}: cheese-striped tits in the Vadswoud and the Kaasvlakte; they hang upside down under leaves
 *   and peck at knabbelbessen.</li>
 *   <li>{@link GuhUiltjeEntity}: little owls in the Guhpieken and the Vadswoud; asleep on a perch by day, at night they fly
 *   with glowing eyes and call "Oehoe-njeg"; their head turns all the way round to watch you.</li>
 *   <li>{@link ZeemeeuwtjeEntity}: gulls on the Guhzee coasts and Guhwai'i; hold some fish and they flock around you:
 *   "Mijn! Mijn!".</li>
 * </ul>
 * All of them are always friendly (never a target, never an attack). Sneak to get close; a startled bird flies up.
 * Resources: tools/features/vogels.py (+ vogels_modellen.py, vogels_geluid.py).
 */
public final class VogelsFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    /** A soft pink-white fluff feather, dropped by the Pluisvinkjes now and then (the hemel quest asks one). */
    public static final DeferredItem<Item> PLUISVEERTJE = ITEMS.register("pluisveertje", () -> new Pluisveertje(new Item.Properties()));

    // --- the birds ---------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<PluisvinkjeEntity>> PLUISVINKJE = ENTITY_TYPES.register("pluisvinkje",
            () -> EntityType.Builder.of(PluisvinkjeEntity::new, MobCategory.CREATURE).sized(0.4f, 0.5f).eyeHeight(0.4f)
                    .clientTrackingRange(8).build(Guhs.id("pluisvinkje").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<KaasmeesjeEntity>> KAASMEESJE = ENTITY_TYPES.register("kaasmeesje",
            () -> EntityType.Builder.of(KaasmeesjeEntity::new, MobCategory.CREATURE).sized(0.4f, 0.5f).eyeHeight(0.4f)
                    .clientTrackingRange(8).build(Guhs.id("kaasmeesje").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<GuhUiltjeEntity>> GUH_UILTJE = ENTITY_TYPES.register("guh_uiltje",
            () -> EntityType.Builder.of(GuhUiltjeEntity::new, MobCategory.CREATURE).sized(0.5f, 0.8f).eyeHeight(0.6f)
                    .clientTrackingRange(8).build(Guhs.id("guh_uiltje").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ZeemeeuwtjeEntity>> ZEEMEEUWTJE = ENTITY_TYPES.register("zeemeeuwtje",
            () -> EntityType.Builder.of(ZeemeeuwtjeEntity::new, MobCategory.CREATURE).sized(0.5f, 0.6f).eyeHeight(0.5f)
                    .clientTrackingRange(8).build(Guhs.id("zeemeeuwtje").toString()));

    public static final DeferredItem<DeferredSpawnEggItem> PLUISVINKJE_SPAWN_EGG = ITEMS.registerItem("pluisvinkje_spawn_egg",
            p -> new DeferredSpawnEggItem(PLUISVINKJE, 0xFFE2EE, 0xF696BE, p));
    public static final DeferredItem<DeferredSpawnEggItem> KAASMEESJE_SPAWN_EGG = ITEMS.registerItem("kaasmeesje_spawn_egg",
            p -> new DeferredSpawnEggItem(KAASMEESJE, 0xFAD654, 0xB896D6, p));
    public static final DeferredItem<DeferredSpawnEggItem> GUH_UILTJE_SPAWN_EGG = ITEMS.registerItem("guh_uiltje_spawn_egg",
            p -> new DeferredSpawnEggItem(GUH_UILTJE, 0xC48E88, 0xFFE27A, p));
    public static final DeferredItem<DeferredSpawnEggItem> ZEEMEEUWTJE_SPAWN_EGG = ITEMS.registerItem("zeemeeuwtje_spawn_egg",
            p -> new DeferredSpawnEggItem(ZEEMEEUWTJE, 0xFCFCFA, 0xB2BCCC, p));

    // --- the bird feeder -------------------------------------------------------------------------------------------------
    public static final DeferredBlock<VoerhuisjeBlock> VOERHUISJE = BLOCKS.registerBlock("vogels_voerhuisje", VoerhuisjeBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f).sound(SoundType.WOOD).noOcclusion().randomTicks());

    static {
        ITEMS.registerItem("vogels_voerhuisje", p -> new VoerhuisjeBlock.Item(VOERHUISJE.get(), p));
    }

    // --- sounds and particles --------------------------------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> TJIEP = sound("vogels.pluisvinkje");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEES = sound("vogels.kaasmeesje");
    public static final DeferredHolder<SoundEvent, SoundEvent> OEHOE = sound("vogels.guh_uiltje");
    public static final DeferredHolder<SoundEvent, SoundEvent> KLIEW = sound("vogels.zeemeeuwtje");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIJN_MIJN = sound("vogels.mijn_mijn");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLADDER = sound("vogels.fladder");

    /** A tiny pink feather that drifts down, swaying. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VEERTJE = PARTICLES.register("vogels_veertje",
            () -> new SimpleParticleType(false));

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        PARTICLES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(PLUISVINKJE.get(), Vogeltje.createAttributes(3.0).build());
            event.put(KAASMEESJE.get(), Vogeltje.createAttributes(3.0).build());
            event.put(GUH_UILTJE.get(), Vogeltje.createAttributes(5.0).build());
            event.put(ZEEMEEUWTJE.get(), Vogeltje.createAttributes(5.0).build());
        });
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> {
            event.register(PLUISVINKJE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, VogelSpawns::check,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(KAASMEESJE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, VogelSpawns::check,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(GUH_UILTJE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, VogelSpawns::check,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ZEEMEEUWTJE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, VogelSpawns::check,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        GuhDex.creaturePage(GuhVariant.PLUISVINKJE, PLUISVINKJE, 8);
        GuhDex.creaturePage(GuhVariant.KAASMEESJE, KAASMEESJE, 8);
        GuhDex.creaturePage(GuhVariant.GUH_UILTJE, GUH_UILTJE, 8);
        GuhDex.creaturePage(GuhVariant.ZEEMEEUWTJE, ZEEMEEUWTJE, 8);
        NeoForge.EVENT_BUS.register(VogelsEvents.class);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(PLUISVEERTJE.get()));
        output.accept(new ItemStack(VOERHUISJE.get()));
        for (var egg : List.of(PLUISVINKJE_SPAWN_EGG, KAASMEESJE_SPAWN_EGG, GUH_UILTJE_SPAWN_EGG, ZEEMEEUWTJE_SPAWN_EGG)) {
            output.accept(new ItemStack(egg.get()));
        }
    }

    private VogelsFeature() {
    }
}
