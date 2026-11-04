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
        nl.juiced.guhs.feature.vadsrijk.zone2.systemen.client.Z2SystemenClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.zone2.wezens.client.Z2WezensClient.init(modBus);   // zone 2 (z2-wezens)
        nl.juiced.guhs.feature.vadsrijk.momenten.client.MomentenClient.init(modBus);   // MOMENTEN-HOOK (fix round 1: wave + caravan)
        nl.juiced.guhs.feature.vadsrijk.zone2.bazen.client.Z2BazenClient.init(modBus);
        UitrustingTips.init();   // fix round 2 (g-ui): jewellery how-to, "Los stuk", set piece lists
        nl.juiced.guhs.feature.vadsrijk.huisdieren.client.HuisdierClient.init(modBus);   // G-PETS
        nl.juiced.guhs.feature.vadsrijk.stadsmuziek.client.StadsMuziekSpeler.init();   // I-AUDIO: town music + town ambience
        nl.juiced.guhs.feature.vadsrijk.zone3.systemen.client.Z3SystemenClient.init(modBus);   // zone 3 (z3-systemen)
    }
}
