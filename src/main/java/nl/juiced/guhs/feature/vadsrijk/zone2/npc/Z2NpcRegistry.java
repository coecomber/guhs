package nl.juiced.guhs.feature.vadsrijk.zone2.npc;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;

/**
 * Het Vadsrijk zone 2 "De Splinterpieken" (Z2_BRIEF.md), slice npc: the NPC guhs of Kandijkom and the zone ({@link Z2Npcs}), the
 * zone-2 story and travel ({@link Z2Verhaal}), the zone-2 hooks and secrets ({@link Z2Haken}), the roles/adapters ({@link Z2Rollen})
 * and the API for the other zone-2 slices ({@link Z2NpcApi}). It reuses zone 1's quest system (feature/vadsrijk/npc): the quests
 * are data in data/guhs/vadsrijk/npc/quests_z2.json. Texts and quests: tools/features/vadsrijk_z2_npc.py.
 */
public final class Z2NpcRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** The quest items (look: vanilla textures, tools/features/vadsrijk_z2_npc.py QUEST_ITEMS). */
    public static final Map<String, DeferredItem<Item>> QUEST_ITEMS = new LinkedHashMap<>();
    static {
        for (String id : new String[] {"galmscherf", "wantje", "kandijkristal", "sneeuwbal", "gloeikristal", "tandwiel", "boorbeitel", "ijsbloem",
                "ijsvis", "wolpluk", "hartsplinter", "drakensplinter"}) {
            QUEST_ITEMS.put(id, ITEMS.registerSimpleItem("vr_z2_npc_" + id, () -> new Item.Properties().stacksTo(64)));
        }
        QUEST_ITEMS.put("chocomelk", ITEMS.registerSimpleItem("vr_z2_npc_chocomelk",
                () -> new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible().build(),
                        net.minecraft.world.item.component.Consumables.defaultDrink().build())));
    }

    private Z2NpcRegistry() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        Z2Npcs.init();
        Z2Verhaal.init();
        Z2Haken.registreer();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Z2Haken.tick(e.getServer());
            Z2Verhaal.tick(e.getServer());
        });
        // after zone 1's NpcRegistry (it binds all zone-1 roles on ServerStarted): our roles only where nobody bound them yet
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, (ServerStartedEvent e) -> {
            Z2Rollen.koppel();
            gebieden();
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> Z2Commando.register(e));
    }

    /** The zone-2 areas, when z2-wereld did not register them (yet): our fallback boxes from the brief. */
    static void gebieden() {
        for (Z2NpcPlekken.Gebied g : Z2NpcPlekken.GEBIEDEN) {
            if (Gebieden.van(g.id()) == null) {
                Gebieden.voegVooraan(new Gebieden.Gebied(g.id(), g.minX(), g.minZ(), g.maxX(), g.maxZ(), g.minLv(), g.maxLv(), g.stad()));
            }
        }
    }
}
