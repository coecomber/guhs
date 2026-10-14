package nl.juiced.guhs.feature.einde2.castschurk;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2, slice cast_schurk (CONTRACT_E2 11.6): the Mika's of the Guhvatar story and its creatures, as far as they EXIST:
 * a body, a name and two lines of small talk. No story logic (phase B), no Guhdex page, no natural spawn, no loot.
 * <ul>
 *   <li>twelve {@link GuhNpcEntity.Kind}s (Vuurheer Mikai in three looks, Azula-, Ty Lee-, Zhao-, Long Feng- and Dai Li-Mika, the
 *       Vuurnatie soldiers in three mix sets, the warden, the sorry cook, the fortress captain): the table {@link CastSchurk},
 *       the default role {@link CastSchurkRol} (a role phase B registers for a spot wins over it);</li>
 *   <li>nine creatures as entity types on {@link CastWezen} (the table {@link Wezens}), each with a creative-only spawn egg.</li>
 * </ul>
 * Dev run only: {@code /guhs einde2 cast_schurk toon | weg} ({@link E2CastSchurkCommando}).
 * Resources: tools/features/einde2_cast_schurk.py (models: einde2_cast_schurk_modellen, _mikas, _wezens; texts: _tekst).
 */
public final class E2CastSchurkFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    /** Random salt of this slice (CONTRACT_E2 11.1). */
    public static final long SALT = 21402050L;

    private static final Map<Wezens, DeferredHolder<EntityType<?>, ? extends EntityType<? extends CastWezen>>> TYPES = new EnumMap<>(Wezens.class);
    private static final List<DeferredItem<SpawnEggItem>> EIEREN = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<CastWezen>> DRAAKGUH = wezen(Wezens.DRAAKGUH, CastWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<CastWezen>> BOSCO = wezen(Wezens.BOSCO, CastWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<CastWezen>> WAN_SHI_GUH = wezen(Wezens.WAN_SHI_GUH, CastWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<VosguhEntity>> VOSGUH = wezen(Wezens.VOSGUH, VosguhEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<CastWezen>> LEEUWSCHILDPAD_GUH = wezen(Wezens.LEEUWSCHILDPAD_GUH, CastWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<CastWezen>> FLOPSIE = wezen(Wezens.FLOPSIE, CastWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<CastWezen>> LUCHTBIZON_GUH = wezen(Wezens.LUCHTBIZON_GUH, CastWezen::new);
    public static final DeferredHolder<EntityType<?>, EntityType<CastVisEntity>> MAANVIS = wezen(Wezens.MAANVIS, CastVisEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<CastVisEntity>> OCEAANVIS = wezen(Wezens.OCEAANVIS, CastVisEntity::new);

    /** One creature: its entity type (hitbox from the table) and its spawn egg {@code <id>_spawn_egg}. */
    private static <T extends CastWezen> DeferredHolder<EntityType<?>, EntityType<T>> wezen(Wezens w, EntityType.EntityFactory<T> maker) {
        String id = w.id();
        DeferredHolder<EntityType<?>, EntityType<T>> type = ENTITY_TYPES.register(id, () -> {
            EntityType.Builder<T> b = EntityType.Builder.of(maker, MobCategory.MISC).sized(w.breed, w.hoog).eyeHeight(w.hoog * 0.85f)
                    .clientTrackingRange(w == Wezens.DRAAKGUH || w == Wezens.LEEUWSCHILDPAD_GUH ? 12 : 8);
            if (w == Wezens.DRAAKGUH || w == Wezens.LEEUWSCHILDPAD_GUH) {
                b = b.fireImmune();
            }
            return b.build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id)));
        });
        TYPES.put(w, type);
        EIEREN.add(ModItems.spawnEgg(ITEMS, id + "_spawn_egg", type));
        return type;
    }

    /** The entity type of a creature. */
    public static EntityType<? extends CastWezen> type(Wezens w) {
        return TYPES.get(w).get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            for (Wezens w : Wezens.values()) {
                event.put(type(w), CastWezen.createAttributes().build());
            }
        });
        for (GuhNpcEntity.Kind kind : CastSchurk.kinds()) {
            NpcRollen.zet(kind, CastSchurkRol.ROL);              // hook fase B: a role for a spot wins over this default
        }
        NeoForge.EVENT_BUS.addListener(E2CastSchurkCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<SpawnEggItem> ei : EIEREN) {
            output.accept(new ItemStack(ei.get()));
        }
    }

    private E2CastSchurkFeature() {
    }
}
