package nl.juiced.guhs.feature.einde2.mikas;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
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
 * einde2, slice mikas: the three little creatures of the outer islands that are no biome guhs, and the box one of them gives.
 * <ul>
 *   <li>The Ender-Mika ({@link EnderMikaEntity}): a tall, thin, purple-black Mika that teleports, is shy when you look at
 *       it, and steals ONE knabbel out of your hand, which it always gives back. Drops a Flitskruimel now and then.</li>
 *   <li>The Kaasmijt ({@link KaasmijtEntity}): a tiny mite of cheese crumbs that sometimes pops out of an eaten
 *       knabbelkoorvrucht ({@link Kaasmijten#uitVrucht}) and tickles you: the hik ({@link HikEffect}). Ender-Mika's forget
 *       everything and follow it.</li>
 *   <li>The Schuilguh ({@link SchuilguhEntity}): a lief guh in a kaasdoos, stuck to a wall, a ceiling or the floor. A kusje
 *       lets you float for a moment; a kaasknabbel gets you a doosschil, once a day.</li>
 *   <li>The Schuildoos ({@link SchuildoosBlock}): two doosschillen around a chest. A box of 27 slots that keeps what is in
 *       it when you pick it up.</li>
 * </ul>
 * Hard rules: the Schuilguh is a guh and so always lief; the Ender-Mika is a Mika and so it only shoves, and a knabbel it
 * took is never lost. Resources: tools/features/einde2_mikas.py (models: einde2_mikas_modellen.py).
 */
public final class E2MikasFeature {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Guhs.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    // --- the creatures (hitbox and eye height of the tall Mika: build contract 3.3) --------------------------------------------
    /** (a Mika: a monster like the others; allowed in peaceful, it never hurts anyway) */
    public static final DeferredHolder<EntityType<?>, EntityType<EnderMikaEntity>> ENDER_MIKA = ENTITY_TYPES.register("ender_mika",
            () -> EntityType.Builder.<EnderMikaEntity>of(EnderMikaEntity::new, MobCategory.MONSTER).sized(0.6f, 2.9f).eyeHeight(2.6f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("ender_mika"))));
    public static final DeferredHolder<EntityType<?>, EntityType<KaasmijtEntity>> KAASMIJT = ENTITY_TYPES.register("kaasmijt",
            () -> EntityType.Builder.<KaasmijtEntity>of(KaasmijtEntity::new, MobCategory.CREATURE).sized(0.35f, 0.3f).eyeHeight(0.18f)
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("kaasmijt"))));
    public static final DeferredHolder<EntityType<?>, EntityType<SchuilguhEntity>> SCHUILGUH = ENTITY_TYPES.register("schuilguh",
            () -> EntityType.Builder.<SchuilguhEntity>of(SchuilguhEntity::new, MobCategory.CREATURE).sized(1.0f, 1.0f).eyeHeight(0.5f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("schuilguh"))));

    // --- items ---------------------------------------------------------------------------------------------------------------
    /** What an Ender-Mika leaves behind now and then: a crumb that still flickers. (hook fase C: a material of tier 5.) */
    public static final DeferredItem<Item> FLITSKRUIMEL = ITEMS.registerItem("flitskruimel", Lore::new, () -> new Item.Properties());
    /** A piece of a Schuilguh's box, pushed out for a knabbel. (hook fase C: a material of tier 5.) */
    public static final DeferredItem<Item> DOOSSCHIL = ITEMS.registerItem("doosschil", Lore::new, () -> new Item.Properties());
    public static final DeferredItem<SpawnEggItem> ENDER_MIKA_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "ender_mika_spawn_egg", ENDER_MIKA);
    public static final DeferredItem<SpawnEggItem> KAASMIJT_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "kaasmijt_spawn_egg", KAASMIJT);
    public static final DeferredItem<SpawnEggItem> SCHUILGUH_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "schuilguh_spawn_egg", SCHUILGUH);

    // --- the Schuildoos -----------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<SchuildoosBlock> SCHUILDOOS = BLOCKS.registerBlock("schuildoos", SchuildoosBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS).strength(1.5f)
                    .sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<SchuildoosItem> SCHUILDOOS_ITEM = ITEMS.registerItem("schuildoos",
            p -> new SchuildoosItem(SCHUILDOOS.get(), p), p -> p.stacksTo(1).useBlockDescriptionPrefix());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SchuildoosBlockEntity>> SCHUILDOOS_BE = BLOCK_ENTITIES.register("schuildoos",
            () -> new BlockEntityType<>(SchuildoosBlockEntity::new, SCHUILDOOS.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<SchuildoosMenu>> SCHUILDOOS_MENU = MENUS.register("schuildoos",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new SchuildoosMenu(id, inventory)));

    // --- the hik -----------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<MobEffect, HikEffect> HIK = EFFECTS.register("hik", HikEffect::new);

    /** The purple-and-cheese sparks of an Ender-Mika that flits away (and of a Kaasmijt that pops out of a fruit). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLITS = PARTICLES.register("einde2_mikas_flits",
            () -> new SimpleParticleType(false));

    // --- sounds (tools/features/einde2_mikas.py: built from vanilla's and the guh / Mika sounds) -------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> ENDER_FLITS = sound("einde2_mikas.ender_mika.flits");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENDER_JAT = sound("einde2_mikas.ender_mika.jat");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENDER_BETRAPT = sound("einde2_mikas.ender_mika.betrapt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENDER_ZWIJMEL = sound("einde2_mikas.ender_mika.zwijmel");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIJT_PIEP = sound("einde2_mikas.kaasmijt.piep");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIJT_KIETEL = sound("einde2_mikas.kaasmijt.kietel");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIJT_PLOP = sound("einde2_mikas.kaasmijt.plop");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIK_GELUID = sound("einde2_mikas.hik");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHUIL_OPEN = sound("einde2_mikas.schuilguh.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHUIL_DICHT = sound("einde2_mikas.schuilguh.dicht");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHUIL_KUSJE = sound("einde2_mikas.schuilguh.kusje");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHUIL_SCHIL = sound("einde2_mikas.schuilguh.schil");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOS_OPEN = sound("einde2_mikas.schuildoos.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOS_DICHT = sound("einde2_mikas.schuildoos.dicht");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        EFFECTS.register(modBus);
        SOUNDS.register(modBus);
        PARTICLES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(ENDER_MIKA.get(), EnderMikaEntity.createEnderAttributes().build());
            event.put(KAASMIJT.get(), KaasmijtEntity.createAttributes().build());
            event.put(SCHUILGUH.get(), SchuilguhEntity.createAttributes().build());
        });
        // the Ender-Mika comes by itself on the outer islands (data/guhs/neoforge/biome_modifier/einde2_mikas_ender_mika.json);
        // Kaasmijten and Schuilguhs never do (a fruit, a building, an egg)
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> event.register(ENDER_MIKA.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EnderMikaEntity::checkEnderSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        NeoForge.EVENT_BUS.addListener(E2MikasCommando::register);
        GuhDex.creaturePage(GuhVariant.ENDER_MIKA, ENDER_MIKA, 8.0);      // (it flits away when you look at it: 8 blocks will do)
        GuhDex.creaturePage(GuhVariant.KAASMIJT, KAASMIJT);
        GuhDex.creaturePage(GuhVariant.SCHUILGUH, SCHUILGUH);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(FLITSKRUIMEL.get()));
        output.accept(new ItemStack(DOOSSCHIL.get()));
        output.accept(new ItemStack(SCHUILDOOS_ITEM.get()));
        output.accept(new ItemStack(ENDER_MIKA_SPAWN_EGG.get()));
        output.accept(new ItemStack(KAASMIJT_SPAWN_EGG.get()));
        output.accept(new ItemStack(SCHUILGUH_SPAWN_EGG.get()));
    }

    /** A plain item with one grey line under its name (lang {@code <item key>.lore}). */
    static class Lore extends Item {
        Lore(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    private E2MikasFeature() {
    }
}
