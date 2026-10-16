package nl.juiced.guhs.feature.einde2.ballon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.einde2.ballon.BallonActeur;
import nl.juiced.guhs.feature.einde2.ballon.E2BallonFeature;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

/**
 * einde2 phase C, slice trap5_ballon, client side: the renderer of the Vredesballon actor (its model drawn at a third and
 * scaled by 3, the flame glowing, never culled while any of it can be on screen). The statuette is a plain block model.
 */
public final class E2BallonClient {
    /** (tools/features/einde2_trap5_ballon_model.py SCHAAL) */
    public static final float SCHAAL = 3f;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(E2BallonFeature.VREDESBALLON_ACTEUR.get(), BallonRenderer::new));
    }

    public static class BallonRenderer extends GeoEntityRenderer<BallonActeur, EntityRenderState> {
        private static final DataTicket<Float> YAW = DataTicket.create("guhs_einde2_trap5_ballon_yaw", Float.class);
        private static final Identifier GLOED = Guhs.id("textures/entity/einde2_trap5_ballon_vredesballon_gloed.png");

        public BallonRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<BallonActeur>(Guhs.id("einde2_trap5_ballon_vredesballon")));
            withScale(SCHAAL);
            this.shadowRadius = 0f;
            withRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void submitRenderTask(RenderPassInfo<EntityRenderState> info, SubmitNodeCollector collector) {
                    if (info.willRender()) {
                        getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(GLOED));
                    }
                }
            });
        }

        /** The envelope is ten blocks wide and floats twelve above the basket: cull by the whole balloon. */
        @Override
        protected AABB getBoundingBoxForCulling(BallonActeur ballon) {
            return ballon.getBoundingBox().inflate(7, 0, 7).expandTowards(0, 36, 0);
        }

        @Override
        public void addRenderData(BallonActeur ballon, @javax.annotation.Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(YAW, Mth.rotLerp(partialTick, ballon.yRotO, ballon.getYRot()));
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            Float yaw = info.getGeckolibData(YAW);
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - (yaw == null ? 0f : yaw)));
        }
    }

    private E2BallonClient() {
    }
}
