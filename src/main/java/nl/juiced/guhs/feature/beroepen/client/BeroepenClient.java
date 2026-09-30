package nl.juiced.guhs.feature.beroepen.client;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.beroepen.BeroepenFeature;
import nl.juiced.guhs.feature.beroepen.KnabbeldiefMikaEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

/**
 * Client side of the beroepen: the four characters' own models (Blusguh's fire helmet and jacket, Vahoegsma's cap and
 * magnifying glass, Snotneus-guh's head mirror and red nose, Bob's helmet, vest and hammer) and the Knabbeldief-Mika (the
 * Mika's model with a mask and a loot sack). Block render types come from the block models.
 */
public final class BeroepenClient {
    public static void init(IEventBus modBus) {
        for (GuhNpcEntity.Kind kind : new GuhNpcEntity.Kind[] {GuhNpcEntity.Kind.BRANDWEERGUH, GuhNpcEntity.Kind.POLITIEGUH,
                GuhNpcEntity.Kind.APOTHEKERGUH, GuhNpcEntity.Kind.BOUWVAKKERGUH}) {
            SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("geo/entity/guh_npc_" + kind.id() + ".geo.json"));
        }
        modBus.addListener(BeroepenClient::renderers);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BeroepenFeature.KNABBELDIEF_MIKA.get(), context -> new GeoEntityRenderer<KnabbeldiefMikaEntity>(context,
                new DefaultedEntityGeoModel<KnabbeldiefMikaEntity>(Guhs.id("knabbeldief_mika"), true) {
                    @Override
                    public Identifier getAnimationResource(KnabbeldiefMikaEntity mika) {
                        return Guhs.id("animations/entity/guh.animation.json");
                    }
                }) {
            {
                this.shadowRadius = 0.3f;
            }
        });
    }

    private BeroepenClient() {
    }
}
