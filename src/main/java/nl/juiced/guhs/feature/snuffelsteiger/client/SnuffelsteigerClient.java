package nl.juiced.guhs.feature.snuffelsteiger.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.snuffel.client.HondRenderer;
import nl.juiced.guhs.feature.snuffelsteiger.SnuffelsteigerFeature;
import nl.juiced.guhs.feature.snuffelsteiger.SteigerBoot;

/**
 * Client side of the dock of Het Snuffeleiland: the dock's dogs are drawn by the kern's own dog renderer (the approved
 * models; the sick puppy as the viewer's own puppy), and Kapitein Zoutsnoet's boat by {@link BootRenderer}
 * (geckolib/models/entity/steiger_boot.geo.json, made by tools/features/snuffel_steiger_modellen.py).
 */
public final class SnuffelsteigerClient {
    private SnuffelsteigerClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(SnuffelsteigerFeature.STEIGER_BEWONER.get(), HondRenderer::new);
            event.registerEntityRenderer(SnuffelsteigerFeature.STEIGER_BOOT.get(), BootRenderer::new);
        });
    }

    /** The boat: turned the way it looks, rolling gently on the water, and hard when its scene says "storm". */
    public static class BootRenderer extends GeoEntityRenderer<SteigerBoot, EntityRenderState> {
        /** {time, how many degrees it rolls} */
        private static final DataTicket<float[]> WIEG = DataTicket.create("guhs_steiger_wieg", float[].class);

        public BootRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<SteigerBoot>(Guhs.id("steiger_boot")));
            this.shadowRadius = 1.2f;
        }

        @Override
        public void addRenderData(SteigerBoot boot, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, boot.yRotO, boot.getYRot()));
            state.addGeckolibData(WIEG, new float[] {boot.tickCount + partialTick + boot.getId() * 7, boot.stormt() ? 7.5f : 1.4f});
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            super.applyRotations(info, poseStack, nativeScale);
            float[] w = info.getGeckolibData(WIEG);
            if (w != null) {
                float snel = w[1] > 3 ? 2.6f : 1f;
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(w[0] * 0.06f * snel) * w[1]));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(w[0] * 0.047f * snel) * w[1] * 0.6f));
            }
        }
    }
}
