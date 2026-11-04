package nl.juiced.guhs.feature.vadsrijk.client;

import net.neoforged.bus.api.IEventBus;

/** Het Vadsrijk, the client: kern's HUD/keys/scenes/renderers, then every slice's client class. */
public final class VadsrijkClient {
    private VadsrijkClient() {
    }

    public static void init(IEventBus modBus) {
        nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.klassen.client.KlassenClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.wezens.client.WezensClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.bazen.client.BazenClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.wereld.client.WereldClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client.SfeerClient.init(modBus);   // fix round 1: atmosphere
        nl.juiced.guhs.feature.vadsrijk.npc.client.NpcClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.economie.client.EconomieClient.init(modBus);
    }
}
