package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * Het Vadsrijk, slice "npc" (CONTRACT_VADSRIJK 1/9): the NPC guhs ({@link Npcs}, {@link RijkGuh}), quest dialogue
 * ({@link QuestMotor}), the questlog ({@link Questlog}, J), the "!"/"?" markers ({@link NpcMarkers}), the zone-1 story
 * ({@link Verhaal}), the secrets ({@link NpcHaken}), the adapters to the other slices ({@link NpcRollen}) and what they may
 * call ({@link NpcApi}). Texts and quests: tools/features/vadsrijk_npc.py.
 */
public final class NpcRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RijkGuh>> GUH = ENTITIES.register("vr_npc_guh",
            () -> EntityType.Builder.<RijkGuh>of(RijkGuh::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f).clientTrackingRange(10)
                    .noSave().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_npc_guh"))));

    /** The quest items (look: vanilla textures, tools/features/vadsrijk_npc.py QUEST_ITEMS). */
    public static final Map<String, DeferredItem<Item>> QUEST_ITEMS = new LinkedHashMap<>();
    static {
        for (String id : new String[] {"grauwe_pluis", "wolletje", "zwamhoedje", "slijmdruppel", "druppelkristal", "kistje", "wortelzwam", "brief",
                "mika_speldje"}) {
            QUEST_ITEMS.put(id, ITEMS.registerSimpleItem("vr_npc_" + id, () -> new Item.Properties().stacksTo(64)));
        }
    }

    private NpcRegistry() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(GUH.get(), GuhEntity.createAttributes().build()));
        QuestMotor.init();
        NpcHaken.registreer();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Npcs.tick(e.getServer());
            NpcHaken.tick(e.getServer());
            NpcMarkers.tick(e.getServer());
            NpcVolg.tick(e.getServer());
            Verhaal.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            Quests.alle();
            Verhaal.registreerArena();
            Verhaal.koppelSpion();
            // klassen: the job advancements now come from our quests (its desk stops showing the "Ceremonie" button); all roles
            // bound directly to the other slices (integration 1.10.0)
            NpcRollen.koppelAlles();
        });
        NeoForge.EVENT_BUS.addListener(NpcHaken::klikBlok);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            NpcMarkers.vergeet(e.getEntity().getUUID());
            NpcVolg.vergeet(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> NpcCommando.register(e));
    }

    public static void payloads(PayloadRegistrar registrar) {
        NpcPayloads.register(registrar);
    }
}
