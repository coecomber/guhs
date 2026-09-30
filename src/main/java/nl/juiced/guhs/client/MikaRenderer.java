package nl.juiced.guhs.client;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.MikaEntity;

/** Mika: geckolib/models/entity/mika.geo.json + textures/entity/mika.png, animated with the guh animations. */
public class MikaRenderer extends GeoEntityRenderer<MikaEntity, LivingEntityRenderState> {
    private static final float BASE_SHADOW = 0.45f;

    public MikaRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<MikaEntity>(Guhs.id("mika")).withAltAnimations(Guhs.id("guh")));
        this.shadowRadius = BASE_SHADOW;
    }

    /** 1.1.0: was set in render() from mika.getScale(); the render state has the scale now. */
    @Override
    protected float getShadowRadius(LivingEntityRenderState state) {
        return BASE_SHADOW * state.scale;
    }

    /** The "head" bone follows where Mika looks (GeckoLib 4: DefaultedEntityGeoModel(id, true)). */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
    }
}
