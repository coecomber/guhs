package nl.juiced.guhs.feature.einde2.spd1stad.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.constant.dataticket.DataTicket;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.einde2.spd1stad.E2SpD1StadFeature;
import nl.juiced.guhs.feature.einde2.spd1stad.LesbootEntity;

/**
 * einde2 "Sponsguh", slice sp_d1_stad, client side: the renderer of juf Pufguh's lesbootje. It wears the vetbootje's model and
 * textures read-only (geckolib/models/entity/vetbootje.geo.json, the orange colour layer and the "ster" sail of the
 * vetbootje slice): the boat of a boating school, the same family as the player's own vetbootje. It floats a little above
 * the road, as the boats of Bikinibodem do.
 */
public final class E2SpD1StadClient {
    private E2SpD1StadClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(E2SpD1StadFeature.LESBOOT.get(), LesbootRenderer::new));
    }

    public static class LesbootRenderer extends GeoEntityRenderer<LesbootEntity, EntityRenderState> {
        private static final DataTicket<Float> YAW = DataTicket.create("guhs_einde2_sp_d1_stad_yaw", Float.class);
        private static final Identifier KLEUR = Guhs.id("textures/entity/vetbootje_kleur_orange.png");
        private static final Identifier ZEIL = Guhs.id("textures/entity/vetbootje_zeil_ster.png");
        private static final Identifier GLOED = Guhs.id("textures/entity/vetbootje_gloed.png");

        public LesbootRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<LesbootEntity>(Guhs.id("vetbootje")));
            this.shadowRadius = 0.9f;
            withRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void submitRenderTask(RenderPassInfo<EntityRenderState> info, SubmitNodeCollector collector) {
                    if (!info.willRender()) {
                        return;
                    }
                    getRenderer().submitRenderTasks(info, collector.order(1), RenderTypes.entityCutout(KLEUR));
                    getRenderer().submitRenderTasks(info, collector.order(2), RenderTypes.entityCutout(ZEIL));
                    getRenderer().submitRenderTasks(info, collector.order(3), GuhRenderTypes.eyes(GLOED));
                }
            });
        }

        @Override
        protected AABB getBoundingBoxForCulling(LesbootEntity boot) {
            return boot.getBoundingBox().inflate(1.2, 0, 1.2).expandTowards(0, 2.4, 0);
        }

        @Override
        public void addRenderData(LesbootEntity boot, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(YAW, Mth.rotLerp(partialTick, boot.yRotO, boot.getYRot()));
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            Float yaw = info.getGeckolibData(YAW);
            poseStack.translate(0, 0.2, 0);
            if (yaw != null) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180f - yaw));
            }
        }
    }
}
