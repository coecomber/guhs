package nl.juiced.guhs.feature.droomwezens;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.8.0 Guhdromenland, bouwplan C "droomwezens": the dream NPCs ({@link DroomNpc}: Snoes, the Zandguh, the Wekkerguh, the
 * Rommelguh, Oma-guh knitting, the kussengevecht-guh, with the pillow they throw: {@link KussenProjectiel}), the
 * {@link SterrenvisjeEntity sterrenvisje} and the {@link GlasRestjeBlock glas met een restje}. Test command:
 * /guhs droomwezens spawn &lt;naam&gt; ({@link DroomwezensCommando}). Resources: tools/features/droomwezens.py.
 */
public final class DroomwezensFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SterrenvisjeEntity>> STERRENVISJE = ENTITY_TYPES.register("sterrenvisje",
            () -> EntityType.Builder.of(SterrenvisjeEntity::new, MobCategory.WATER_AMBIENT).sized(0.35f, 0.3f).eyeHeight(0.18f)
                    .clientTrackingRange(6).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("sterrenvisje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<KussenProjectiel>> KUSSEN_PROJECTIEL = ENTITY_TYPES.register("kussen_projectiel",
            () -> EntityType.Builder.<KussenProjectiel>of(KussenProjectiel::new, MobCategory.MISC).sized(0.4f, 0.4f)
                    .clientTrackingRange(4).updateInterval(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("kussen_projectiel"))));

    public static final DeferredItem<SpawnEggItem> STERRENVISJE_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "sterrenvisje_spawn_egg", STERRENVISJE);
    public static final DeferredItem<MobBucketItem> STERRENVISJE_EMMER = ITEMS.registerItem("sterrenvisje_emmer",
            p -> new MobBucketItem(STERRENVISJE.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, p), () -> new Item.Properties().stacksTo(1));

    public static final DeferredBlock<GlasRestjeBlock> GLAS_RESTJE = BLOCKS.registerBlock("glas_restje", GlasRestjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3f).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredItem<GlasRestjeBlock.GlasItem> GLAS_RESTJE_ITEM = ITEMS.registerItem("glas_restje",
            p -> new GlasRestjeBlock.GlasItem(GLAS_RESTJE.get(), p), Item.Properties::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> KUSSEN = geluid("kussen");
    public static final DeferredHolder<SoundEvent, SoundEvent> RINKEL = geluid("rinkel");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLOKJE = geluid("slokje");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String id) {
        return SOUNDS.register("droomwezens." + id, () -> SoundEvent.createVariableRangeEvent(Guhs.id("droomwezens." + id)));
    }

    /** The named animations of the two kinds with their own animation file (ticks; the rest of the file: the sitting guh's). */
    static int duur(GuhNpcEntity.Kind kind, String naam) {
        return switch (naam) {
            case "rinkel" -> kind == GuhNpcEntity.Kind.WEKKERGUH ? 20 : -1;
            case "gooi" -> kind == GuhNpcEntity.Kind.KUSSENGEVECHT_GUH ? 14 : -1;
            case "happy" -> 16;
            default -> -1;
        };
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        DroomNpc.rollen();
        for (GuhNpcEntity.Kind k : DroomNpc.KINDS) {
            GuhNpcEntity.ANIMATIE_DUUR.put(k, naam -> duur(k, naam));
        }
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(STERRENVISJE.get(), SterrenvisjeEntity.createAttributes().build()));
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> event.register(STERRENVISJE.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AbstractFish::checkSurfaceWaterAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> DroomwezensCommando.register(event));
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(STERRENVISJE_SPAWN_EGG.get()));
        output.accept(new ItemStack(STERRENVISJE_EMMER.get()));
        output.accept(new ItemStack(GLAS_RESTJE_ITEM.get()));
    }

    private DroomwezensFeature() {
    }
}
