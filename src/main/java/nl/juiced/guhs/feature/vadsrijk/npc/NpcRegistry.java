package nl.juiced.guhs.feature.vadsrijk.npc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Het Vadsrijk, slice "npc" (CONTRACT_VADSRIJK 1): NPCs, dialogue, the questlog (SpelerData.extra("npc")), the "!" markers, story cutscenes (kern.scene.RijkScenes).
 * Put this slice's DeferredRegisters, listeners, commands ({@code kern.VadsrijkCommando#wortel}) and payloads behind these two methods.
 * Ids: see the contract's prefix table. Stub until the npc slice fills it.
 */
public final class NpcRegistry {
    private NpcRegistry() {
    }

    public static void register(IEventBus modBus) {
    }

    public static void payloads(PayloadRegistrar registrar) {
    }
}
