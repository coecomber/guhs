package nl.juiced.guhs.feature.erfenis.sherlock.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.erfenis.sherlock.SherlockPayloads;

/**
 * The client of the Sherlock story: the Detectivebureau screen, and the looks of the cast (their own geo models, Sherlock's
 * bubble pipe, Morris and Pim on the Mika's body with its animations; tools/features/erfenis_sherlock.py makes them).
 */
public final class SherlockClient {
    private SherlockClient() {
    }

    public static void init(IEventBus modBus) {
        SherlockPayloads.bordOntvanger = b -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof BureauScherm s) {
                s.update(b.staat());
            } else {
                mc.setScreen(new BureauScherm(b.staat()));
            }
        };
        for (Kind kind : new Kind[] {Kind.SHERLOCK_GUH, Kind.DOKTER_VADSON, Kind.MAKELAAR_GUHRRIT, Kind.GUHRRIT_VAKANTIE, Kind.MORRIS_MIKA, Kind.PIM_MIKA,
                Kind.OEHOE, Kind.KNABBELHAMER}) {
            SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("entity/guh_npc_" + kind.id()));
        }
        // Sherlock blows bubbles; the two Mikas move like Mikas
        SittingGuhRenderers.NPC_ANIMATIES.put(Kind.SHERLOCK_GUH, Guhs.id("entity/guh_npc_sherlock_guh"));
        SittingGuhRenderers.NPC_ANIMATIES.put(Kind.MORRIS_MIKA, Guhs.id("entity/guh_npc_morris_mika"));
        SittingGuhRenderers.NPC_ANIMATIES.put(Kind.PIM_MIKA, Guhs.id("entity/guh_npc_pim_mika"));
    }
}
