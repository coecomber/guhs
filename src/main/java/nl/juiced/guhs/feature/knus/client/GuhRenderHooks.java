package nl.juiced.guhs.feature.knus.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import nl.juiced.guhs.entity.GuhEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Extra render passes on every guh (2.8, client only), so features never edit the GuhRenderer: register a {@link Laag}
 * from your client init. They are called from one GeoRenderLayer that the GuhRenderer adds last (after the clothes),
 * e.g. to draw a sparkle, pyjamas, an ice-cream hat or blushing cheeks when a {@code GuhHooks} flag is set.
 * A layer that hides or shows bones must put them back as it found them (see GuhClothesLayer).
 */
public final class GuhRenderHooks {
    @FunctionalInterface
    public interface Laag {
        void render(GeoRenderer<GuhEntity> renderer, PoseStack pose, GuhEntity guh, BakedGeoModel model, MultiBufferSource buffers,
                    float partialTick, int light, int overlay);
    }

    private static final List<Laag> LAGEN = new CopyOnWriteArrayList<>();

    public static void laag(Laag laag) {
        LAGEN.add(laag);
    }

    /** The one layer the GuhRenderer adds: runs every registered {@link Laag}. */
    public static class HookLayer extends GeoRenderLayer<GuhEntity> {
        public HookLayer(GeoRenderer<GuhEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, GuhEntity guh, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            for (Laag laag : LAGEN) {
                poseStack.pushPose();
                try {
                    laag.render(getRenderer(), poseStack, guh, model, bufferSource, partialTick, packedLight, packedOverlay);
                } finally {
                    poseStack.popPose();
                }
            }
        }
    }

    private GuhRenderHooks() {
    }
}
