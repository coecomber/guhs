package nl.juiced.guhs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.MikaEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Mika: geo/entity/mika.geo.json + textures/entity/mika.png, animated with the guh animations. */
public class MikaRenderer extends GeoEntityRenderer<MikaEntity> {
    private static final float BASE_SHADOW = 0.45f;

    public MikaRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<MikaEntity>(Guhs.id("mika"), true).withAltAnimations(Guhs.id("guh")));
        this.shadowRadius = BASE_SHADOW;
    }

    @Override
    public void render(MikaEntity mika, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.shadowRadius = BASE_SHADOW * mika.getScale();
        super.render(mika, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
