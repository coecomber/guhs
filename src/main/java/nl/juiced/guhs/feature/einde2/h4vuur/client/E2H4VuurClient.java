package nl.juiced.guhs.feature.einde2.h4vuur.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.h4vuur.E2H4VuurFeature;
import nl.juiced.guhs.feature.einde2.h4vuur.LuchtschipEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * einde2 phase B, slice h4_vuur, client side: the airship (GeckoLib), the fire balls (light only: they draw their own
 * sparks), the Guhvatar statue ({@link BeeldRenderer}) and the layer on players in disguise or in the Guhvatar state
 * ({@link LookLaag}).
 */
public final class E2H4VuurClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(E2H4VuurFeature.LUCHTSCHIP.get(), LuchtschipRenderer::new);
            event.registerEntityRenderer(E2H4VuurFeature.VUURBAL.get(), NoopRenderer::new);
            event.registerBlockEntityRenderer(E2H4VuurFeature.BEELD_BE.get(), BeeldRenderer::new);
        });
        modBus.addListener(E2H4VuurClient::lagen);
    }

    private static void lagen(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new LookLaag(renderer));
            }
        }
    }

    /** An airship of Mikai's fleet: drawn 2.5 times its model size, leaning as it sinks. */
    public static class LuchtschipRenderer extends GeoEntityRenderer<LuchtschipEntity, EntityRenderState> {
        /** {yaw, lean} */
        private static final DataTicket<float[]> DRAAI = DataTicket.create("guhs_einde2_h4_vuur_luchtschip_draai", float[].class);
        public static final float SCHAAL = 2.5f;

        public LuchtschipRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<LuchtschipEntity>(Guhs.id("einde2_h4_vuur_luchtschip")));
            this.shadowRadius = 0f;
        }

        @Override
        protected AABB getBoundingBoxForCulling(LuchtschipEntity schip) {
            return schip.getBoundingBox().inflate(8, 4, 8);
        }

        @Override
        public void addRenderData(LuchtschipEntity schip, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(DRAAI, new float[] {Mth.rotLerp(partialTick, schip.yRotO, schip.getYRot()), Mth.lerp(partialTick, schip.hellingO, schip.helling)});
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            float[] d = info.getGeckolibData(DRAAI);
            if (d == null) {
                return;
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - d[0]));
            poseStack.mulPose(Axis.XP.rotationDegrees(d[1]));
            poseStack.scale(SCHAAL, SCHAAL, SCHAAL);
        }
    }

    private E2H4VuurClient() {
    }
}
