package nl.juiced.guhs.feature.bio.client;

import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.bio.blokkendal.client.BlokkenDalClient;
import nl.juiced.guhs.feature.bio.blokkenwolk.client.BlokkenWolkClient;
import nl.juiced.guhs.feature.bio.bouwdal.client.BouwDalClient;
import nl.juiced.guhs.feature.bio.bouwmeer.client.BouwMeerClient;
import nl.juiced.guhs.feature.bio.bouwwolk1.client.BouwWolk1Client;
import nl.juiced.guhs.feature.bio.bouwwolk2.client.BouwWolk2Client;
import nl.juiced.guhs.feature.bio.dieren.client.DierenClient;
import nl.juiced.guhs.feature.bio.kompas.client.KompasClient;
import nl.juiced.guhs.feature.bio.systemen.client.SystemenClient;
import nl.juiced.guhs.feature.bio.wereld.client.WereldClient;

/** Client side of biomes3: every slice's client entry in table order (the kern has no client code of its own). */
public final class BioClient {
    public static void init(IEventBus modBus) {
        BlokkenDalClient.init(modBus);
        BlokkenWolkClient.init(modBus);
        WereldClient.init(modBus);
        DierenClient.init(modBus);
        KompasClient.init(modBus);
        BouwDalClient.init(modBus);
        BouwMeerClient.init(modBus);
        BouwWolk1Client.init(modBus);
        BouwWolk2Client.init(modBus);
        SystemenClient.init(modBus);
    }

    private BioClient() {
    }
}
