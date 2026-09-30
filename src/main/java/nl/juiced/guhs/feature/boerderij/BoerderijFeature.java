package nl.juiced.guhs.feature.boerderij;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
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
 * De Guhboerderij (2.8, slice boerderij): the farm of Boerin Hooibaal in the guhweides and the kaasvlakte, and its three
 * cuddly animals. tools/features/boerderij.py makes the resources (models, textures, the farm template...).
 * <ul>
 *   <li>The animals ({@link BoerderijDier}): the {@link GuhschaapjeEntity guhschaapje}, the
 *       {@link KnabbelkippetjeEntity knabbelkippetje} and the {@link GuhkoeEntity guhkoe}. Always lief and passive. Pet
 *       them (empty hand), brush them ({@code guhborstel}) and feed them ({@code knabbelvoer}): an animal cared for in two
 *       of the three ways on one day is content ("blij") and gives its product: pluiswol, a knabbelei (into a nearby
 *       {@link KippennestjeBlock kippennestje}) or kaasmelk (with an empty bottle). One product per animal per day.</li>
 *   <li>The {@link GuhVoerbakBlock guh_voerbak}: fill it with knabbelvoer and the animals around eat from it by themselves.</li>
 *   <li>Boerin Hooibaal ({@link Hooibaal}, NPC kind BOERINNEGUH): a daily chore (klusje) and a shop.</li>
 *   <li>The products go into the shared tags {@code guhs:knus/pluiswol}, {@code knabbelei} and {@code kaasmelk} (bakery,
 *       tea house, creche...).</li>
 *   <li>Knus tab section "boerderij" ({@link BoerderijVoortgang}), Guhdex pages for the three animals, the farm is protected
 *       ({@link BoerderijProtection}).</li>
 * </ul>
 */
public final class BoerderijFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final ResourceKey<Structure> GUHBOERDERIJ = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guhboerderij"));

    // --- blocks ------------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<net.minecraft.world.level.block.Block> PLUISWOLBLOK = BLOCKS.registerSimpleBlock("pluiswolblok",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<GuhVoerbakBlock> GUH_VOERBAK = BLOCKS.registerBlock("guh_voerbak", GuhVoerbakBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.2f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredBlock<KippennestjeBlock> KIPPENNESTJE = BLOCKS.registerBlock("kippennestje", KippennestjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5f).sound(SoundType.GRASS).noOcclusion().ignitedByLava());

    // --- items -------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<BoerderijItems.Lore> PLUISWOL = ITEMS.registerItem("pluiswol", BoerderijItems.Lore::new, () -> new Item.Properties());
    public static final DeferredItem<BoerderijItems.Lore> KNABBELEI = ITEMS.registerItem("knabbelei", BoerderijItems.Lore::new,
            () -> new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f).build()));
    public static final DeferredItem<BoerderijItems.Kaasmelk> KAASMELK = ITEMS.registerItem("kaasmelk", BoerderijItems.Kaasmelk::new,
            new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)
                    .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible().usingConvertsTo(Items.GLASS_BOTTLE).build()));
    public static final DeferredItem<BoerderijItems.Lore> GUHBORSTEL = ITEMS.registerItem("guhborstel", BoerderijItems.Lore::new,
            () -> new Item.Properties().durability(96));
    public static final DeferredItem<BoerderijItems.Lore> KNABBELVOER = ITEMS.registerItem("knabbelvoer", BoerderijItems.Lore::new, () -> new Item.Properties());

    static {
        ITEMS.registerSimpleBlockItem(PLUISWOLBLOK);
        ITEMS.registerItem("guh_voerbak", p -> new BoerderijItems.LoreBlock(GUH_VOERBAK.get(), p));
        ITEMS.registerItem("kippennestje", p -> new BoerderijItems.LoreBlock(KIPPENNESTJE.get(), p));
    }

    // --- the animals -------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<GuhschaapjeEntity>> GUHSCHAAPJE = ENTITY_TYPES.register("guhschaapje",
            () -> EntityType.Builder.of(GuhschaapjeEntity::new, MobCategory.CREATURE).sized(0.9f, 1.1f).eyeHeight(0.85f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guhschaapje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<KnabbelkippetjeEntity>> KNABBELKIPPETJE = ENTITY_TYPES.register("knabbelkippetje",
            () -> EntityType.Builder.of(KnabbelkippetjeEntity::new, MobCategory.CREATURE).sized(0.5f, 0.65f).eyeHeight(0.5f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("knabbelkippetje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<GuhkoeEntity>> GUHKOE = ENTITY_TYPES.register("guhkoe",
            () -> EntityType.Builder.of(GuhkoeEntity::new, MobCategory.CREATURE).sized(1.0f, 1.35f).eyeHeight(1.15f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guhkoe"))));

    public static final DeferredItem<DeferredSpawnEggItem> GUHSCHAAPJE_SPAWN_EGG = ITEMS.registerItem("guhschaapje_spawn_egg",
            p -> new DeferredSpawnEggItem(GUHSCHAAPJE, 0xFFF0F6, 0xF08CB4, p));
    public static final DeferredItem<DeferredSpawnEggItem> KNABBELKIPPETJE_SPAWN_EGG = ITEMS.registerItem("knabbelkippetje_spawn_egg",
            p -> new DeferredSpawnEggItem(KNABBELKIPPETJE, 0xF7C83C, 0xF08CB4, p));
    public static final DeferredItem<DeferredSpawnEggItem> GUHKOE_SPAWN_EGG = ITEMS.registerItem("guhkoe_spawn_egg",
            p -> new DeferredSpawnEggItem(GUHKOE, 0xFFF4DC, 0xE8A93A, p));

    // --- particles and sounds --------------------------------------------------------------------------------------------
    /** A little tuft of pluiswol (brushing a guhschaapje). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WOLPLUKJE = PARTICLES.register("wolplukje", () -> new SimpleParticleType(false));
    /** A drop of kaasmelk. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MELKDRUPPEL = PARTICLES.register("melkdruppel", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> SCHAAPJE_BLEH = sound("boerderij.schaapje_bleh");
    public static final DeferredHolder<SoundEvent, SoundEvent> KIPPETJE_TOK = sound("boerderij.kippetje_tok");
    public static final DeferredHolder<SoundEvent, SoundEvent> KOE_MOEH = sound("boerderij.koe_moeh");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    private static final Hooibaal HOOIBAAL = new Hooibaal();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener(BoerderijFeature::setup);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(GUHSCHAAPJE.get(), GuhschaapjeEntity.createAttributes().build());
            event.put(KNABBELKIPPETJE.get(), KnabbelkippetjeEntity.createAttributes().build());
            event.put(GUHKOE.get(), GuhkoeEntity.createAttributes().build());
        });
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> {
            for (var type : List.of(GUHSCHAAPJE, KNABBELKIPPETJE, GUHKOE)) {
                register(event, type.get());
            }
        });
        BoerderijProtection.register();
        nl.juiced.guhs.feature.Protected.add(BoerderijProtection::inBoerderij);
        BoerderijVoortgang.register();
        GuhDex.creaturePage(GuhVariant.GUHSCHAAPJE, GUHSCHAAPJE);
        GuhDex.creaturePage(GuhVariant.KNABBELKIPPETJE, KNABBELKIPPETJE);
        GuhDex.creaturePage(GuhVariant.GUHKOE, GUHKOE);
    }

    @SuppressWarnings("unchecked")
    private static <T extends BoerderijDier> void register(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, spawnType, pos, random) -> BoerderijDier.checkDierSpawnRules(level, spawnType, pos), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            fire.setFlammable(PLUISWOLBLOK.get(), 30, 60);
            fire.setFlammable(KIPPENNESTJE.get(), 60, 20);
            fire.setFlammable(GUH_VOERBAK.get(), 5, 20);
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var item : List.of(PLUISWOL, KNABBELEI, KAASMELK, GUHBORSTEL, KNABBELVOER)) {
            output.accept(new ItemStack(item.get()));
        }
        for (var block : List.of(PLUISWOLBLOK, GUH_VOERBAK, KIPPENNESTJE)) {
            output.accept(new ItemStack(block.get()));
        }
        for (var egg : List.of(GUHSCHAAPJE_SPAWN_EGG, KNABBELKIPPETJE_SPAWN_EGG, GUHKOE_SPAWN_EGG)) {
            output.accept(new ItemStack(egg.get()));
        }
    }

    /** Boerin Hooibaal (BOERINNEGUH): daily chores and her shop. */
    @Nullable
    public static NpcRole role() {
        return HOOIBAAL;
    }

    private BoerderijFeature() {
    }
}
