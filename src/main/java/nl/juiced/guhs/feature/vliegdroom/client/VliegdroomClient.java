package nl.juiced.guhs.feature.vliegdroom.client;

import javax.annotation.Nullable;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vliegdroom.DroomRookguhEntity;
import nl.juiced.guhs.feature.vliegdroom.VliegdroomFeature;

/**
 * Client side of 1.8.0's Vliegdroom: the dream Rookguh drawn with the Rookguh's own model, big, round and a little more
 * dreamy-lilac than the Reis-Rookguh.
 */
public final class VliegdroomClient {
    private VliegdroomClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(VliegdroomFeature.ROOKGUH.get(), Renderer::new));
    }

    static class Renderer extends GeoEntityRenderer<DroomRookguhEntity, LivingEntityRenderState> {
        /** How big the body is drawn (blocks; the wild Rookguh: 3.375, the Reis-Rookguh 2.0). */
        static final float SCHAAL = 2.2f;

        Renderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<DroomRookguhEntity>(Guhs.id("rookguh")));
            this.shadowRadius = 0.8f;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            bones.ifPresent("rook", b -> {
                b.setScaleX(1.1f);
                b.setScaleZ(1.1f);
                b.setScaleY(1.05f);
            });
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            super.scaleModelForRender(info, widthScale * SCHAAL, heightScale * SCHAAL);
        }

        @Override
        public int getRenderColor(DroomRookguhEntity guh, @Nullable Void related, float partialTick) {
            return ARGB.colorFromFloat(1.0f, 0.97f, 0.86f, 1.0f);
        }
    }
}
