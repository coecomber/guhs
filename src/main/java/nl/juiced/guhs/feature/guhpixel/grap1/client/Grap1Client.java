package nl.juiced.guhs.feature.guhpixel.grap1.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpixel.grap1.Grap1Slice;

/**
 * Client side of the guhpixel slice "grap1": the Bedwars team guhs are drawn as normal guhs (model, nightcap, sleeping
 * eyes) and the three lobby NPCs get their own models. Everything else of the three games is server-driven (titles, boss bars, particles, display entities), so there
 * is no screen and no HUD of its own here.
 */
public final class Grap1Client {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(Grap1Slice.TEAMGUH.get(), GuhRenderer::new));
        // the three lobby NPCs have their own models: a headset, an armour of pillows, a parachute pack with goggles
        for (GuhNpcEntity.Kind kind : new GuhNpcEntity.Kind[] {GuhNpcEntity.Kind.SKYBLOK_GUH, GuhNpcEntity.Kind.BEDWARS_GUH, GuhNpcEntity.Kind.VADSNITE_GUH}) {
            SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("entity/guh_npc_" + kind.id()));
        }
    }

    private Grap1Client() {
    }
}
