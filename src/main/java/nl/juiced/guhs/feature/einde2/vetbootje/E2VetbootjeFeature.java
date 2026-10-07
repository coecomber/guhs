package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * einde2, slice vetbootje: the player's own vetbootje and the personal heteluchtballon.
 * <ul>
 *   <li>{@link VetbootjeEntity}: a small, slow flying ship with a Mika sail, a chest and two cushions for guhs; one per
 *       player ({@link Vetbootje}: the public API for the questline and the shipyard of phase B), never lost (its looks and
 *       chest live in the {@link Vloot}, the vetfluitje brings a new body);</li>
 *   <li>{@link HeteluchtballonEntity}: the small balloon of the tier-5 end reward ({@link Heteluchtballon}: the public API
 *       for phase C), drifting on the wind of the dimension;</li>
 *   <li>the items ({@link VetbootjeItems}), the scheepswerf menu and the two payloads ({@link WerfMenu}), the op commands
 *       ({@link VetbootjeCommando}).</li>
 * </ul>
 * No guh pages and no quests here: phase B and C hang theirs on the hidden advancements quest/einde2_vetbootje_gekregen and
 * quest/einde2_vetbootje_ballon. Resources: tools/features/einde2_vetbootje.py (+ _modellen, _geluid).
 */
public final class E2VetbootjeFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<VetbootjeEntity>> VETBOOTJE = ENTITY_TYPES.register("vetbootje",
            () -> EntityType.Builder.<VetbootjeEntity>of(VetbootjeEntity::new, MobCategory.MISC).sized(1.6f, 0.9f).eyeHeight(0.5f).fireImmune()
                    .clientTrackingRange(10).updateInterval(3).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vetbootje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<HeteluchtballonEntity>> HETELUCHTBALLON = ENTITY_TYPES.register("heteluchtballon",
            () -> EntityType.Builder.<HeteluchtballonEntity>of(HeteluchtballonEntity::new, MobCategory.MISC).sized(1.3f, 1.1f).eyeHeight(0.6f).fireImmune()
                    .clientTrackingRange(12).updateInterval(3).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("heteluchtballon"))));

    public static final DeferredItem<VetbootjeItems.Vetfluitje> VETBOOTJE_FLUITJE = ITEMS.registerItem("vetbootje_fluitje", VetbootjeItems.Vetfluitje::new,
            () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<VetbootjeItems.Werfboekje> VETBOOTJE_WERFBOEKJE = ITEMS.registerItem("vetbootje_werfboekje", VetbootjeItems.Werfboekje::new,
            () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<VetbootjeItems.Ballon> HETELUCHTBALLON_ITEM = ITEMS.registerItem("heteluchtballon", VetbootjeItems.Ballon::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    public static final DeferredHolder<MenuType<?>, MenuType<WerfMenu>> WERF_MENU = MENUS.register("einde2_vetbootje_werf",
            () -> IMenuTypeExtension.create(WerfMenu::client));

    public static final DeferredHolder<SoundEvent, SoundEvent> FLUITJE = sound("einde2_vetbootje.fluitje");
    public static final DeferredHolder<SoundEvent, SoundEvent> AANKOMST = sound("einde2_vetbootje.aankomst");
    public static final DeferredHolder<SoundEvent, SoundEvent> KRAAK = sound("einde2_vetbootje.kraak");
    public static final DeferredHolder<SoundEvent, SoundEvent> VERF = sound("einde2_vetbootje.verf");
    public static final DeferredHolder<SoundEvent, SoundEvent> BRANDER = sound("einde2_vetbootje.brander");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOUW = sound("einde2_vetbootje.vouw");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        MENUS.register(modBus);
        SOUNDS.register(modBus);
        NeoForge.EVENT_BUS.addListener(VetbootjeCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
        WerfMenu.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(VETBOOTJE_FLUITJE.get()));
        output.accept(new ItemStack(VETBOOTJE_WERFBOEKJE.get()));
        output.accept(new ItemStack(HETELUCHTBALLON_ITEM.get()));       // (a loose one: it belongs to nobody)
    }

    private E2VetbootjeFeature() {
    }
}
