package nl.juiced.guhs.feature.campingmarkt.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.campingmarkt.CampingmarktFeature;
import nl.juiced.guhs.feature.campingmarkt.KraamMikaEntity;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * Client side of bbq2 (camping-markt): the three characters' own models (the Kampbaas-guh with his ranger hat and whistle,
 * the Houthakker-guh with his woolly hat and axe, the Marktmeester-Mika with his tall hat, moustache and chain of office)
 * and the stall holders of the market (the Mika's model in an apron and a flat cap, the guh's animations).
 * Models: tools/features/camping_markt_modellen.py.
 */
public final class CampingmarktClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(CampingmarktFeature.KRAAM_MIKA.get(), KraamMikaRenderer::new));
        for (GuhNpcEntity.Kind kind : new GuhNpcEntity.Kind[]{GuhNpcEntity.Kind.KAMPBAASGUH, GuhNpcEntity.Kind.HOUTHAKKERGUH, GuhNpcEntity.Kind.MARKTMEESTER_MIKA}) {
            SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("entity/guh_npc_" + kind.id()));
        }
        // (the Marktmeester has the Mika's bones: his own idle and happy)
        SittingGuhRenderers.NPC_ANIMATIES.put(GuhNpcEntity.Kind.MARKTMEESTER_MIKA, Guhs.id("entity/guh_npc_marktmeester_mika"));
        // the Kampbaas-guh's whistle swings on its cord, the Houthakker-guh's axe rocks on his shoulder
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.KAMPBAASGUH, (npc, tick) -> {
            float t = (float) tick * 0.07f;
            return bones -> bones.ifPresent("kampbaas_fluitje", b -> b.setRotZ(Mth.sin(t) * 0.18f));
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.HOUTHAKKERGUH, (npc, tick) -> {
            float t = (float) tick * 0.05f;
            return bones -> bones.ifPresent("houthakker_bijl", b -> b.setRotX(Mth.sin(t) * 0.06f));
        });
    }

    /** A stall holder: the Mika's model with an apron and a cap (geo campingmarkt_kraam_mika), animated like a guh. */
    public static class KraamMikaRenderer extends GeoEntityRenderer<KraamMikaEntity, LivingEntityRenderState> {
        public KraamMikaRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<KraamMikaEntity>(Guhs.id("campingmarkt_kraam_mika")).withAltAnimations(Guhs.id("guh")));
            this.shadowRadius = 0.45f;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    private CampingmarktClient() {
    }
}
