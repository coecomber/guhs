package nl.juiced.guhs.feature.piep;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.Heightmap;
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
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.sterrenwacht.Buiten;

/**
 * Piep (2.8.1): four new little creatures and a pink cake, between Knuffeldal (2.8) and 2.9.
 * <ul>
 *   <li>The {@link PieppiepmuisjeEntity pieppiepmuisje}: a tiny plush mouse that scurries and peeps. Tame it with a
 *       kaasknabbel; tamed it follows you, rides on your shoulder ({@link Schouder}), can be picked up as the
 *       {@link MuisjeItem pieppiepmuisje_item}, and plays {@link Verstoppertje verstoppertje}. It lives only in the lieve
 *       guh buildings of the Guhmensie: {@link PiepSpawns#maybeAddMuisje} (about one per ten structure guhs).</li>
 *   <li>{@link PoepschillyEntity Poepschilly}: a green sea turtle plush on the guhzee coasts. Tamed, use it on a tamed guh:
 *       it crawls in, cleans the guh from the inside and pops out again; the guh is "Fris van binnen"
 *       ({@link PiepEffecten.FrisVanBinnen}).</li>
 *   <li>The {@link RozeGuhKoekBlock roze guh koek}: food ("Lief kijken", {@link PiepEffecten.LiefKijken}) and a placeable koekje
 *       on a saucer with 4 bites. Its recipe ({@link ReceptItem}) is learned per player and baked in the Knabbelbakkerij
 *       (Recept.ROZE_GUH_KOEK).</li>
 *   <li>The {@link KaasknabbelNest kaasknabbel-nest}: a rare kaaskorst hill with 3 waves of {@link BozeKaasknabbelEntity boze
 *       kaasknabbels} and the {@link BozeOppernabbelEntity Boze Oppernabbel}; they were just zieli...</li>
 *   <li>The Guhdex section "piep" on the Knus tab ({@link PiepVoortgang}).</li>
 *   <li>(2.9, piepmenu) Each of the three maatjes has its own little owner menu ({@link PiepMenu}: rondvadsen, volg mij,
 *       rename, pick up, a few settings of its own, see {@link PiepInstelling}) and can be picked up into an item that keeps
 *       everything ({@link PiepDierItem}: pieppiepmuisje_item, poepschilly_item, schilly_item).</li>
 * </ul>
 * tools/features/piep.py makes the resources (models in piep_modellen.py, the nest in piep_nest.py).
 */
public final class PiepFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Guhs.MODID);

    public static final ResourceKey<Structure> KAASKNABBEL_NEST = ResourceKey.create(Registries.STRUCTURE, Guhs.id("kaasknabbel_nest"));
    /** Blocks a muisje can hide in for verstoppertje (flower pots, baskets, barrels...). */
    public static final TagKey<Block> VERSTOPPLEKKEN = TagKey.create(Registries.BLOCK, Guhs.id("piep/verstopplekken"));

    // --- the roze guh koek -------------------------------------------------------------------------------------------------
    public static final DeferredBlock<RozeGuhKoekBlock> ROZE_GUH_KOEK = BLOCKS.registerBlock("roze_guh_koek", RozeGuhKoekBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5f).sound(SoundType.WOOL).noOcclusion()
                    .instrument(NoteBlockInstrument.BELL).pushReaction(PushReaction.DESTROY));
    public static final FoodProperties KOEK_ETEN = new FoodProperties.Builder().nutrition(6).saturationModifier(0.7f).alwaysEdible().build();
    public static final DeferredItem<RozeGuhKoekBlock.KoekItem> ROZE_GUH_KOEK_ITEM = ITEMS.registerItem("roze_guh_koek",
            p -> new RozeGuhKoekBlock.KoekItem(ROZE_GUH_KOEK.get(), p), new Item.Properties().stacksTo(16).food(KOEK_ETEN).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<ReceptItem> ROZE_GUH_KOEK_RECEPT = ITEMS.registerItem("roze_guh_koek_recept", ReceptItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    // --- the creatures -------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<PieppiepmuisjeEntity>> PIEPPIEPMUISJE = ENTITY_TYPES.register("pieppiepmuisje",
            () -> EntityType.Builder.of(PieppiepmuisjeEntity::new, MobCategory.CREATURE).sized(0.38f, 0.32f).eyeHeight(0.22f)
                    .clientTrackingRange(8).build(Guhs.id("pieppiepmuisje").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<PoepschillyEntity>> POEPSCHILLY = ENTITY_TYPES.register("poepschilly",
            () -> EntityType.Builder.of(PoepschillyEntity::new, MobCategory.CREATURE).sized(0.55f, 0.32f).eyeHeight(0.2f)
                    .clientTrackingRange(10).build(Guhs.id("poepschilly").toString()));
    /** Schilly: Poepschilly's look-alike, the minihoofdje-bestie of the guhs (no poetsbeurt). */
    public static final DeferredHolder<EntityType<?>, EntityType<SchillyEntity>> SCHILLY = ENTITY_TYPES.register("schilly",
            () -> EntityType.Builder.of(SchillyEntity::new, MobCategory.CREATURE).sized(0.55f, 0.32f).eyeHeight(0.2f)
                    .clientTrackingRange(10).build(Guhs.id("schilly").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<BozeKaasknabbelEntity>> BOZE_KAASKNABBEL = ENTITY_TYPES.register("boze_kaasknabbel",
            () -> EntityType.Builder.of(BozeKaasknabbelEntity::new, MobCategory.MONSTER).sized(0.4f, 0.55f).eyeHeight(0.4f)
                    .clientTrackingRange(10).build(Guhs.id("boze_kaasknabbel").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<BozeOppernabbelEntity>> BOZE_OPPERNABBEL = ENTITY_TYPES.register("boze_oppernabbel",
            () -> EntityType.Builder.of(BozeOppernabbelEntity::new, MobCategory.MONSTER).sized(0.9f, 1.6f).eyeHeight(1.2f)
                    .clientTrackingRange(12).fireImmune().build(Guhs.id("boze_oppernabbel").toString()));

    public static final DeferredItem<MuisjeItem> PIEPPIEPMUISJE_ITEM = ITEMS.registerItem("pieppiepmuisje_item", MuisjeItem::new,
            new Item.Properties().stacksTo(1));
    /** A picked-up Poepschilly / Schilly (the whole turtle in the item; right-click a block to put it down). */
    public static final DeferredItem<PiepDierItem> POEPSCHILLY_ITEM = ITEMS.registerItem("poepschilly_item",
            p -> new PiepDierItem(() -> POEPSCHILLY.get(), p), new Item.Properties().stacksTo(1));
    public static final DeferredItem<PiepDierItem> SCHILLY_ITEM = ITEMS.registerItem("schilly_item",
            p -> new PiepDierItem(() -> SCHILLY.get(), p), new Item.Properties().stacksTo(1));
    public static final DeferredItem<DeferredSpawnEggItem> PIEPPIEPMUISJE_SPAWN_EGG = ITEMS.registerItem("pieppiepmuisje_spawn_egg",
            p -> new DeferredSpawnEggItem(PIEPPIEPMUISJE, 0x2A2233, 0xECE4D0, p));
    public static final DeferredItem<DeferredSpawnEggItem> POEPSCHILLY_SPAWN_EGG = ITEMS.registerItem("poepschilly_spawn_egg",
            p -> new DeferredSpawnEggItem(POEPSCHILLY, 0x70803E, 0xEEE4D0, p));
    public static final DeferredItem<DeferredSpawnEggItem> SCHILLY_SPAWN_EGG = ITEMS.registerItem("schilly_spawn_egg",
            p -> new DeferredSpawnEggItem(SCHILLY, 0x86A04A, 0x6A5E50, p));
    public static final DeferredItem<DeferredSpawnEggItem> BOZE_KAASKNABBEL_SPAWN_EGG = ITEMS.registerItem("boze_kaasknabbel_spawn_egg",
            p -> new DeferredSpawnEggItem(BOZE_KAASKNABBEL, 0xF5A93A, 0x5C2A10, p));
    public static final DeferredItem<DeferredSpawnEggItem> BOZE_OPPERNABBEL_SPAWN_EGG = ITEMS.registerItem("boze_oppernabbel_spawn_egg",
            p -> new DeferredSpawnEggItem(BOZE_OPPERNABBEL, 0xEC8428, 0xFACE48, p));

    // --- effects, particles, sounds -----------------------------------------------------------------------------------------
    public static final DeferredHolder<MobEffect, MobEffect> FRIS_VAN_BINNEN = MOB_EFFECTS.register("fris_van_binnen", PiepEffecten.FrisVanBinnen::new);
    public static final DeferredHolder<MobEffect, MobEffect> LIEF_KIJKEN = MOB_EFFECTS.register("lief_kijken", PiepEffecten.LiefKijken::new);
    public static final DeferredHolder<MobEffect, MobEffect> BESTIES = MOB_EFFECTS.register("besties", PiepEffecten.Besties::new);
    /** A little "piep!" note from a hidden muisje. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PIEPJE = PARTICLES.register("piepje", () -> new SimpleParticleType(false));
    /** A sparkle of a guh that is fris van binnen. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FRIS_SPARKEL = PARTICLES.register("fris_sparkel", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> PIEP = sound("entity.pieppiepmuisje.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIEP_AU = sound("entity.pieppiepmuisje.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHILLY_POETS = sound("entity.poepschilly.poets");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHILLY_PLOP = sound("entity.poepschilly.plop");
    public static final DeferredHolder<SoundEvent, SoundEvent> KNABBEL_BOOS = sound("entity.boze_kaasknabbel.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> KNABBEL_ZIELI = sound("entity.boze_kaasknabbel.zieli");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** The nest is protected like the other loose buildings (doors and chests still work). */
    public static final Buiten.Bescherming NEST_BESCHERMING = new Buiten.Bescherming("kaasknabbel_nest", "gui.guhs.piep.nest_beschermd");

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        MOB_EFFECTS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(PIEPPIEPMUISJE.get(), PieppiepmuisjeEntity.createAttributes().build());
            event.put(POEPSCHILLY.get(), PoepschillyEntity.createAttributes().build());
            event.put(SCHILLY.get(), PoepschillyEntity.createAttributes().build());
            event.put(BOZE_KAASKNABBEL.get(), BozeKaasknabbelEntity.createAttributes().build());
            event.put(BOZE_OPPERNABBEL.get(), BozeOppernabbelEntity.createAttributes().build());
        });
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> {
            event.register(POEPSCHILLY.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, spawnType, pos, random) -> PoepschillyEntity.checkSpawn(level, spawnType, pos),
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(SCHILLY.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, spawnType, pos, random) -> PoepschillyEntity.checkSpawn(level, spawnType, pos),
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(PIEPPIEPMUISJE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, spawnType, pos, random) -> spawnType != net.minecraft.world.entity.MobSpawnType.NATURAL,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        NEST_BESCHERMING.register();
        NeoForge.EVENT_BUS.register(PiepEvents.class);
        GuhHooks.klik(PoepschillyEntity::klikOpGuh);
        GuhHooks.klik(PiepEffecten::liefKijkenTemmen);
        PiepVoortgang.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
        PiepPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var item : List.of(ROZE_GUH_KOEK_ITEM, ROZE_GUH_KOEK_RECEPT, PIEPPIEPMUISJE_SPAWN_EGG, POEPSCHILLY_SPAWN_EGG, SCHILLY_SPAWN_EGG,
                BOZE_KAASKNABBEL_SPAWN_EGG, BOZE_OPPERNABBEL_SPAWN_EGG)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private PiepFeature() {
    }
}
