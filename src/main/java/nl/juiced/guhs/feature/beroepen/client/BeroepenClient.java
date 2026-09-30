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
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Client side of the beroepen: the four characters' own models (Blusguh's fire helmet and jacket, Vahoegsma's cap and
 * magnifying glass, Snotneus-guh's head mirror and red nose, Bob's helmet, vest and hammer) and the Knabbeldief-Mika (the
 * Mika's model with a mask and a loot sack). Block render types come from the block models.
 */
public final class BeroepenClient {
    public static void init(IEventBus modBus) {
        for (GuhNpcEntity.Kind kind : new GuhNpcEntity.Kind[] {GuhNpcEntity.Kind.BRANDWEERGUH, GuhNpcEntity.Kind.POLITIEGUH,
                GuhNpcEntity.Kind.APOTHEKERGUH, GuhNpcEntity.Kind.BOUWVAKKERGUH}) {
            SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("entity/guh_npc_" + kind.id()));
        }
        modBus.addListener(BeroepenClient::renderers);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BeroepenFeature.KNABBELDIEF_MIKA.get(), context -> new GeoEntityRenderer<KnabbeldiefMikaEntity, LivingEntityRenderState>(context,
                new DefaultedEntityGeoModel<KnabbeldiefMikaEntity>(Guhs.id("knabbeldief_mika")).withAltAnimations(Guhs.id("guh"))) {
            {
                this.shadowRadius = 0.3f;
            }

            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                DefaultAnimations.hardcodedHeadRotation(info, bones, "head");   // was DefaultedEntityGeoModel(id, true)
            }
        });
    }

    private BeroepenClient() {
    }
}
