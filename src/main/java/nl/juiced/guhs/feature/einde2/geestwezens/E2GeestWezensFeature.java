package nl.juiced.guhs.feature.einde2.geestwezens;

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
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2, slice geestmodellen: the creatures of the Geestenwereld and two last characters, as far as they EXIST: a body,
 * a look, animations, a name and two lines of small talk. No story logic (phase B), no natural spawn, no loot.
 * <ul>
 *   <li>seven creatures as entity types on {@link GeestWezen} (the table {@link GeestWezens}), each with a creative-only spawn
 *       egg: {@link KohEntity Koh}, {@link HeiBaiGuhEntity Hei Bai-guh}, {@link FangEntity Fang}, the spirit koi
 *       {@link GeestKoiEntity Tui and La}, the {@link LibelkonijnguhEntity libelkonijnguh}, the
 *       {@link LantaarngeestEntity lantaarngeest} and the {@link SchaduwgeestEntity schaduwgeest} (the only one that shoves);</li>
 *   <li>two {@link GuhNpcEntity.Kind}s (the table {@link GeestKinds}): the Theegeest of the tea pavilion and the Bestelguh of
 *       tech tier 5, with the default role {@link GeestRol} (a role phase B / C registers for a spot wins over it) and their
 *       named animations on demand ({@code GuhNpcEntity.speel}).</li>
 * </ul>
 * Dev run only: the group {@code geest} of {@code /guhs einde2 toon} ({@link GeestToon}).
 * Resources: tools/features/einde2_geestmodellen.py (models: _wezens, _kinds; texts: _tekst).
 */
public final class E2GeestWezensFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    private static final Map<GeestWezens, DeferredHolder<EntityType<?>, ? extends EntityType<? extends GeestWezen>>> TYPES = new EnumMap<>(GeestWezens.class);
    private static final List<DeferredItem<SpawnEggItem>> EIEREN = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<KohEntity>> KOH = wezen(GeestWezens.KOH, KohEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<HeiBaiGuhEntity>> HEI_BAI_GUH = wezen(GeestWezens.HEI_BAI_GUH, HeiBaiGuhEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<FangEntity>> FANG = wezen(GeestWezens.FANG, FangEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<GeestKoiEntity>> GEEST_KOI = wezen(GeestWezens.GEEST_KOI, GeestKoiEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<LibelkonijnguhEntity>> LIBELKONIJNGUH = wezen(GeestWezens.LIBELKONIJNGUH, LibelkonijnguhEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<LantaarngeestEntity>> LANTAARNGEEST = wezen(GeestWezens.LANTAARNGEEST, LantaarngeestEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SchaduwgeestEntity>> SCHADUWGEEST = wezen(GeestWezens.SCHADUWGEEST, SchaduwgeestEntity::new);

    // --- phase B (CONTRACT_E2_B 7.7): the six new creatures (they replace the skelet's stand-ins under the same ids)
    public static final DeferredHolder<EntityType<?>, EntityType<KennisvosjeEntity>> KENNISVOSJE = wezen(GeestWezens.KENNISVOSJE, KennisvosjeEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<MistreigerEntity>> MISTREIGER = wezen(GeestWezens.MISTREIGER, MistreigerEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<OudeReigerguhEntity>> OUDE_REIGERGUH = wezen(GeestWezens.OUDE_REIGERGUH, OudeReigerguhEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<WanGuhEntity>> WAN_GUH = wezen(GeestWezens.WAN_GUH, WanGuhEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<GeestreusEntity>> GEESTREUS = wezen(GeestWezens.GEESTREUS, GeestreusEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<KijkmaskerEntity>> KIJKMASKER = wezen(GeestWezens.KIJKMASKER, KijkmaskerEntity::new);

    /** One creature: its entity type (hitbox from the table) and its spawn egg {@code <id>_spawn_egg}. */
    private static <T extends GeestWezen> DeferredHolder<EntityType<?>, EntityType<T>> wezen(GeestWezens w, EntityType.EntityFactory<T> maker) {
        String id = w.id();
        int bereik = switch (w) {
            case GEESTREUS -> 16;                                   // (only ever seen far away)
            case KOH, FANG, GEEST_KOI, OUDE_REIGERGUH, WAN_GUH -> 12;
            default -> 8;
        };
        DeferredHolder<EntityType<?>, EntityType<T>> type = ENTITY_TYPES.register(id, () -> EntityType.Builder.of(maker, MobCategory.MISC)
                .sized(w.breed, w.hoog).eyeHeight(w.hoog * 0.85f).clientTrackingRange(bereik).updateInterval(w == GeestWezens.FANG ? 1 : 3)
                .fireImmune().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
        TYPES.put(w, type);
        EIEREN.add(ModItems.spawnEgg(ITEMS, id + "_spawn_egg", type));
        return type;
    }

    /** The entity type of a creature. */
    public static EntityType<? extends GeestWezen> type(GeestWezens w) {
        return TYPES.get(w).get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            for (GeestWezens w : GeestWezens.values()) {
                event.put(type(w), GeestWezen.createAttributes().build());
            }
        });
        for (GuhNpcEntity.Kind kind : GeestKinds.kinds()) {
            NpcRollen.zet(kind, GeestRol.ROL);                      // hook fase B / C: a role for a spot wins over this default
            GuhNpcEntity.ANIMATIE_DUUR.put(kind, naam -> GeestKinds.duur(kind, naam));
        }
        // the Guhdex: the creature pages of the spirits (bonus pages; not Wan-guh, the giants, the masks)
        GuhDex.creaturePage(GuhVariant.KOH, KOH, 6.0);
        GuhDex.creaturePage(GuhVariant.HEI_BAI_GUH, HEI_BAI_GUH);
        GuhDex.creaturePage(GuhVariant.FANG, FANG, 6.0);
        GuhDex.creaturePage(GuhVariant.GEEST_KOI, GEEST_KOI, 10.0);
        GuhDex.creaturePage(GuhVariant.LIBELKONIJNGUH, LIBELKONIJNGUH, 5.0);
        GuhDex.creaturePage(GuhVariant.LANTAARNGEEST, LANTAARNGEEST);
        GuhDex.creaturePage(GuhVariant.SCHADUWGEEST, SCHADUWGEEST);
        GuhDex.creaturePage(GuhVariant.KENNISVOSJE, KENNISVOSJE);
        GuhDex.creaturePage(GuhVariant.MISTREIGER, MISTREIGER, 8.0);
        GuhDex.creaturePage(GuhVariant.OUDE_REIGERGUH, OUDE_REIGERGUH, 8.0);
        // Fang's flight (no getting off halfway; it ends cleanly), the pet that is never lost, the dev command
        NeoForge.EVENT_BUS.addListener(Fang::opAfstappen);
        NeoForge.EVENT_BUS.addListener(Fang::opUitloggen);
        NeoForge.EVENT_BUS.addListener(Fang::opDimensie);
        NeoForge.EVENT_BUS.addListener(Fang::opDood);
        NeoForge.EVENT_BUS.addListener(Libelkonijnguh::opSpelerTick);
        NeoForge.EVENT_BUS.addListener(E2GeestWezensCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<SpawnEggItem> ei : EIEREN) {
            output.accept(new ItemStack(ei.get()));
        }
    }

    private E2GeestWezensFeature() {
    }
}
