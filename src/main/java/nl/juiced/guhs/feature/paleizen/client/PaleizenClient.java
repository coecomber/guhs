package nl.juiced.guhs.feature.paleizen.client;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.boerderij.client.BoerderijClient;
import nl.juiced.guhs.feature.paleizen.MopperMikaEntity;
import nl.juiced.guhs.feature.paleizen.PaleizenFeature;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * Client side of bbq2 (paleizen): the Worstzwijntje (the farm animals' renderer: its own model, babies smaller), the
 * neighbours of the Mika-woonblokken (the Mika's model; each number shows its own things: a flat cap, curlers, a
 * nightcap...) and the three characters' own models (Mika-oma with her bun, glasses and knitting needles, the
 * Tolwachter-Mika in uniform, the Stalknecht-guh with his hay fork). Models: tools/features/paleizen_modellen.py.
 */
public final class PaleizenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(PaleizenFeature.WORSTZWIJNTJE.get(), context -> new BoerderijClient.DierRenderer<>(context, "worstzwijntje", 0.4f));
            event.registerEntityRenderer(PaleizenFeature.MOPPER_MIKA.get(), MopperRenderer::new);
        });
        for (GuhNpcEntity.Kind kind : new GuhNpcEntity.Kind[]{GuhNpcEntity.Kind.MIKA_OMA, GuhNpcEntity.Kind.TOLWACHTER_MIKA, GuhNpcEntity.Kind.STALKNECHTGUH}) {
            SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("entity/guh_npc_" + kind.id()));
        }
        // (the two Mika characters have the Mika's bones: their own idle and happy)
        SittingGuhRenderers.NPC_ANIMATIES.put(GuhNpcEntity.Kind.MIKA_OMA, Guhs.id("entity/guh_npc_mika_oma"));
        SittingGuhRenderers.NPC_ANIMATIES.put(GuhNpcEntity.Kind.TOLWACHTER_MIKA, Guhs.id("entity/guh_npc_tolwachter_mika"));
        // the Stalknecht-guh chews on his straw
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.STALKNECHTGUH, (npc, tick) -> {
            float t = (float) tick * 0.08f;
            return bones -> bones.ifPresent("stal_strootje", b -> b.setRotY(Mth.sin(t) * 0.25f));
        });
    }

    /** A neighbour: the Mika's model and the guh's animations; only the things of its own number are drawn. */
    public static class MopperRenderer extends GeoEntityRenderer<MopperMikaEntity, LivingEntityRenderState> {
        static final DataTicket<Integer> NR = DataTicket.create("guhs_paleizen_mopper_nr", Integer.class);

        public MopperRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<MopperMikaEntity>(Guhs.id("paleizen_mopper_mika")).withAltAnimations(Guhs.id("guh")));
            this.shadowRadius = 0.45f;
        }

        @Override
        public void addRenderData(MopperMikaEntity mika, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(NR, mika.getNr());
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            int nr = info.getOrDefaultGeckolibData(NR, 0);
            for (int i = 0; i < MopperMikaEntity.AANTAL; i++) {
                boolean weg = i != nr;
                bones.ifPresent("mopper_" + i, b -> b.skipRender(weg).skipChildrenRender(weg));
            }
        }
    }

    private PaleizenClient() {
    }
}
