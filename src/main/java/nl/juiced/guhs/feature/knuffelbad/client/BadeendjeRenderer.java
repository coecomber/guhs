package nl.juiced.guhs.feature.knuffelbad.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knuffelbad.BadeendjeEntity;
import nl.juiced.guhs.feature.knuffelbad.Eendsoort;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

/**
 * A rubber duck (geckolib/models/entity/badeendje.geo.json), in its kind's colours (textures/entity/badeendje_&lt;kind&gt;.png) with the
 * kind's little extras (a cap, a snorkel, guh ears...). It bobs and turns slowly; the glowing kinds shine in the dark
 * (and every duck is a bit easier to see in the star tunnel). The rider's own game hides a duck as soon as it's picked up.
 * <p>
 * 1.1.0 (GeckoLib 5): kind, bobbing time, spin and yaw are copied into the render state at extract time.
 */
public class BadeendjeRenderer extends GeoEntityRenderer<BadeendjeEntity, EntityRenderState> {
    static final DataTicket<Eendsoort> SOORT = DataTicket.create("guhs_badeendje_soort", Eendsoort.class);
    /** Bobbing time (age + a per-duck offset), spin (degrees, only on a ride) and the duck's yaw. */
    static final DataTicket<float[]> BEWEGING = DataTicket.create("guhs_badeendje_beweging", float[].class);

    public BadeendjeRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<BadeendjeEntity>(Guhs.id("badeendje")) {
            @Override
            public void addAdditionalStateData(BadeendjeEntity duck, @Nullable Object related, GeoRenderState state) {
                state.addGeckolibData(SOORT, duck.getSoort());
            }

            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                Eendsoort soort = state.getGeckolibData(SOORT);
                return soort == null ? super.getTextureResource(state) : Guhs.id("textures/entity/badeendje_" + soort.id() + ".png");
            }
        });
        this.shadowRadius = 0.2f;
        withRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void submitRenderTask(RenderPassInfo<EntityRenderState> info, SubmitNodeCollector collector) {
                Eendsoort soort = info.getGeckolibData(SOORT);
                if (soort != null && soort.glimt && info.willRender()) {
                    // (the eyes render type is full bright, like the old re-render with FULL_BRIGHT)
                    getRenderer().submitRenderTasks(info, collector.order(1),
                            nl.juiced.guhs.client.GuhRenderTypes.eyes(Guhs.id("textures/entity/badeendje_" + soort.id() + "_glow.png")));
                }
            }
        });
    }

    /** The rider's own game hides a duck as soon as it's picked up. */
    @Override
    public boolean shouldRender(BadeendjeEntity duck, Frustum culler, double camX, double camY, double camZ) {
        return !duck.lokaalGepakt && super.shouldRender(duck, culler, camX, camY, camZ);
    }

    @Override
    public void addRenderData(BadeendjeEntity duck, @Nullable Void related, EntityRenderState state, float partialTick) {
        float t = duck.tickCount + partialTick + duck.getId() * 13;
        float spin = duck.vanRit() ? (float) Math.sin(t * 0.05) * 25f : 0f;
        state.addGeckolibData(BEWEGING, new float[]{t, spin, duck.getYRot()});
    }

    @Override
    public void extractRenderState(BadeendjeEntity duck, EntityRenderState state, float partialTick) {
        super.extractRenderState(duck, state, partialTick);
        // never pitch dark: a duck in the star tunnel should still be found
        int block = Math.max(LightCoordsUtil.block(state.lightCoords), 7);
        state.lightCoords = LightCoordsUtil.pack(block, LightCoordsUtil.sky(state.lightCoords));
    }

    @Override
    protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
        float[] b = info.getGeckolibData(BEWEGING);
        if (b == null) {
            super.applyRotations(info, poseStack, nativeScale);
            return;
        }
        float t = b[0];
        poseStack.translate(0, Math.sin(t * 0.12) * 0.05, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - b[2] + b[1]));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(t * 0.09) * 6f));
    }

    /** The extra bones (eend_*) only for the kinds that have them. */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info, BoneSnapshots bones) {
        Eendsoort soort = info.getGeckolibData(SOORT);
        if (soort == null) {
            return;
        }
        for (GeoBone bone : info.model().boneLookup().get().values()) {
            String name = bone.name();
            if (name.startsWith("eend_")) {
                boolean hidden = soort.bones.stream().noneMatch(name::startsWith);
                bones.get(bone).skipRender(hidden).skipChildrenRender(hidden);
            }
        }
    }

    @Override
    public float getMotionAnimThreshold(BadeendjeEntity animatable) {
        return Mth.EPSILON;
    }
}
