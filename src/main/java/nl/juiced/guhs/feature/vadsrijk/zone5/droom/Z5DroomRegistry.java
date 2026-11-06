package nl.juiced.guhs.feature.vadsrijk.zone5.droom;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5Finale;

/**
 * Het Vadsrijk zone 5, the DROOM phase of the finale (T-DROOM): <b>de Grauwe Droom van Moeder Wolkwal</b>, the part-2 deelbaas
 * (a victim: cheered up, never killed). Registers the boss + its two mechanic objects (the lullaby notes and the grey tendrils),
 * the named loot + trophy ({@link DroomLoot}), the pet Droomwalvisje, the dream area, the server tick ({@link Z5Droom#tick}), the
 * dev command {@code /guhs vadsrijk droom ...} and THE hook: {@code Z5Finale.registreer(Fase.DROOM, ...)}.
 * Ids {@code guhs:vr_z5_baas_grauwe_droom}, {@code guhs:vr_z5_baas_wiegnoot}, {@code guhs:vr_z5_baas_grauwe_rank}; lang
 * {@code vadsrijk.z5droom.*}; generator {@code tools/features/vadsrijk_t_droom.py}.
 */
public final class Z5DroomRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    private static <T extends Monster> DeferredHolder<EntityType<?>, EntityType<T>> mob(String id, EntityType.EntityFactory<T> f, float b, float h) {
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(b, h).eyeHeight(h * 0.6f).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    /** The boss (the id has "droom" in it: t-z5verhaal's kill hook also sees it). */
    public static final DeferredHolder<EntityType<?>, EntityType<GrauweDroom>> GRAUWE_DROOM = mob("vr_z5_baas_grauwe_droom", GrauweDroom::new, 4.2f, 2.6f);
    /** The four lullaby notes (right-click in the right order). No "droom" in these ids (the story's kill hook!). */
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> WIEGNOOT = mob("vr_z5_baas_wiegnoot", Z3Doel::new, 1.0f, 1.6f);
    /** A grey tendril of the Keizer's grip (hit it to cut it). */
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> GRAUWE_RANK = mob("vr_z5_baas_grauwe_rank", Z3Doel::new, 0.9f, 2.6f);

    static {
        DroomLoot.TROFEE.getId();   // (initialises DroomLoot: the named items register on ITEMS too)
    }

    /** The pet: the cheered-up dream as a tiny pink cloud whale (G-PETS: the first kill unlocks it). */
    public static final Huisdier PET_DROOMWALVISJE = Huisdieren.registreer(new Huisdier("droomwalvisje", "guhs:vr_z5_baas_grauwe_droom",
            "vadsrijk/zone5/droom/grauwe_droom", "textures/vadsrijk/zone5/droom/grauwe_droom_roze.png", "vadsrijk/zone5/wezens/huisdier", 0.16f,
            "textures/vadsrijk/zone5/droom/grauwe_droom_gloed.png"));

    /** THE hook into the story (Z5Finale.Fase.DROOM). Public for the gametests (another test may overwrite the registration). */
    public static final Z5Finale.Start HAAK = p -> Z5Droom.start(p, false);

    private Z5DroomRegistry() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(GRAUWE_DROOM.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64)
                    .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FLYING_SPEED, 0.3).build());
            for (var t : List.of(WIEGNOOT, GRAUWE_RANK)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
            }
        });
        DroomLoot.typen();
        aanmelden();
        gebied();
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerAboutToStartEvent e) -> gebied());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> Z5Droom.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(Z5DroomCommando::register);
    }

    /** Registers the DROOM phase with the story (also from the gametests, where another test may have replaced it). */
    public static void aanmelden() {
        Z5Finale.registreer(Z5Finale.Fase.DROOM, HAAK);
    }

    /** The dream cloud is an area of its own (name card, lv 60), in front of everything else. */
    public static void gebied() {
        Gebieden.voegVooraan(Z5Droom.GEBIED);
    }
}
