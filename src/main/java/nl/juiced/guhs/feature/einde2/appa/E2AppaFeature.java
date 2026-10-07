package nl.juiced.guhs.feature.einde2.appa;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;

/**
 * einde2, slice appa: what a Guhvatar keeps (DESIGN "Wat je overhoudt").
 * <ul>
 *   <li>{@link AppaGuhEntity the Appa-guh}: a big six-legged flying bison with a guh face. One per player ({@link Appa}), never
 *       lost: the {@link AppaFluitjeItem Appa-fluitje} always brings him (with his saddle chest and his name). His owner sits
 *       on his head and steers; three friends (players, or the owner's own guhs) ride in the saddle basket; five more seats
 *       are for the team of the story. He also flies somewhere by himself ({@link Appa#vlieg}).</li>
 *   <li>{@link MomoGuhEntity the Momo-guh}: a little winged lemur that lives on its owner's right shoulder ({@link Momo}),
 *       picks up what lies around, and now and then borrows a knabbel.</li>
 * </ul>
 * Nothing in this slice gives either of them in survival: the story does (hook fase B: {@link Appa#geef},
 * {@link Momo#geef}); ops have {@code /guhs einde2 appa ...} ({@link E2AppaEvents}). Both have a Guhdex page.
 * Resources: tools/features/einde2_appa.py (models: einde2_appa_modellen.py).
 */
public final class E2AppaFeature {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    /** Random salt of this slice (build contract 3.1). */
    public static final long SALT = 21400550L;

    /** Big: seen from far (he comes flying in), and his box is about his body, not his head and tail. */
    public static final DeferredHolder<EntityType<?>, EntityType<AppaGuhEntity>> APPA_GUH = ENTITY_TYPES.register("appa_guh",
            () -> EntityType.Builder.of(AppaGuhEntity::new, MobCategory.MISC).sized(2.9f, 2.5f).eyeHeight(1.7f)
                    .clientTrackingRange(16).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("appa_guh"))));
    public static final DeferredHolder<EntityType<?>, EntityType<MomoGuhEntity>> MOMO_GUH = ENTITY_TYPES.register("momo_guh",
            () -> EntityType.Builder.of(MomoGuhEntity::new, MobCategory.MISC).sized(0.45f, 0.7f).eyeHeight(0.5f)
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("momo_guh"))));

    /** Calls your Appa-guh (and, sneaking, your Momo-guh). Craftable, so it is never lost for good either. */
    public static final DeferredItem<AppaFluitjeItem> APPA_FLUITJE = ITEMS.registerItem("appa_fluitje", AppaFluitjeItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    /** Creative only: an Appa-guh / Momo-guh without an owner just stands there and looks cute (Guhdex, screenshots). */
    public static final DeferredItem<SpawnEggItem> APPA_GUH_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "appa_guh_spawn_egg", APPA_GUH);
    public static final DeferredItem<SpawnEggItem> MOMO_GUH_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "momo_guh_spawn_egg", MOMO_GUH);

    // --- sounds (vanilla sounds, pitched, in sounds.json) ---------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> JIP_JIP = sound("einde2_appa.jip_jip");
    public static final DeferredHolder<SoundEvent, SoundEvent> BRUL = sound("einde2_appa.brul");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOMO = sound("einde2_appa.momo");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLUITJE = sound("einde2_appa.fluitje");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(APPA_GUH.get(), AppaGuhEntity.createAttributes().build());
            event.put(MOMO_GUH.get(), MomoGuhEntity.createAttributes().build());
        });
        // he is big: you see him (and fill in his page) from a little further away
        GuhDex.creaturePage(GuhVariant.APPA_GUH, APPA_GUH, 5.0);
        GuhDex.creaturePage(GuhVariant.MOMO_GUH, MOMO_GUH);
        NeoForge.EVENT_BUS.register(E2AppaEvents.class);
    }

    public static void payloads(PayloadRegistrar registrar) {
        E2AppaPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(APPA_FLUITJE.get()));
        output.accept(new ItemStack(APPA_GUH_SPAWN_EGG.get()));
        output.accept(new ItemStack(MOMO_GUH_SPAWN_EGG.get()));
    }

    private E2AppaFeature() {
    }
}
