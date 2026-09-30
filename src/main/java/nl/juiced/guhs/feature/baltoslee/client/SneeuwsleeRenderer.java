package nl.juiced.guhs.feature.baltoslee.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.baltoslee.SledehondjeEntity;
import nl.juiced.guhs.feature.baltoslee.SneeuwsleeEntity;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

/**
 * Your own sneeuwslee: the sled model with its pink-and-blue blanket and little hearts, four guh-sledehondjes in front
 * (two by two, roped to it) that swing along through the bends; they sit down and wait when there's no snow.
 */
public class SneeuwsleeRenderer extends GeoEntityRenderer<SneeuwsleeEntity> {
    private static final Identifier TEX = Guhs.id("textures/entity/sneeuwslee.png");
    private static final double[][] PLEKKEN = {{2.0, -0.4}, {2.0, 0.4}, {3.05, -0.4}, {3.05, 0.4}};

    private final EntityRenderDispatcher dispatcher;

    public SneeuwsleeRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.7f;
        this.dispatcher = context.getEntityRenderDispatcher();
    }

    static final class Model extends DefaultedEntityGeoModel<SneeuwsleeEntity> {
        Model() {
            super(Guhs.id("baltoslee_slee"));
        }

        @Override
        public Identifier getTextureResource(SneeuwsleeEntity sled) {
            return TEX;
        }

        @Override
        public void setCustomAnimations(SneeuwsleeEntity sled, long instanceId, AnimationTest<SneeuwsleeEntity> state) {
            getBone("kist").ifPresent(b -> b.setHidden(true));
            getBone("musher").ifPresent(b -> b.setHidden(true));
            float t = (float) state.getAnimationTick();
            float v = Math.min(1, sled.getoondeSnelheid / 0.3f);
            getBone("bellen").ifPresent(b -> b.setRotZ(Mth.sin(t * 0.9f) * 0.25f * v));
            getBone("lantaarn").ifPresent(b -> b.setRotX(Mth.sin(t * 0.45f) * 0.12f * v));
        }
    }

    @Override
    protected void applyRotations(SneeuwsleeEntity sled, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - Mth.rotLerp(partialTick, sled.yRotO, sled.getYRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, sled.leanO, sled.lean)));
    }

    @Override
    public void render(SneeuwsleeEntity sled, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(sled, entityYaw, partialTick, pose, buffers, light);
        Span span = Span.van(sled, 4, SledehondjeEntity.GEWOON, false, false);
        Vec3 origin = new Vec3(Mth.lerp(partialTick, sled.xo, sled.getX()), Mth.lerp(partialTick, sled.yo, sled.getY()), Mth.lerp(partialTick, sled.zo, sled.getZ()));
        float yaw = Mth.rotLerp(partialTick, sled.yRotO, sled.getYRot());
        Vec3 fwd = Vec3.directionFromRotation(0, yaw);
        Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
        float v = sled.getoondeSnelheid;
        float zit = !sled.opSneeuwGezien() || v < 0.015f ? 1f : 0f;
        float loop = Math.min(1f, v / 0.3f);
        Vec3 haak = origin.add(fwd.scale(1.15)).add(0, 0.42, 0);
        for (int i = 0; i < PLEKKEN.length; i++) {
            Vec3 doel = origin.add(fwd.scale(PLEKKEN[i][0])).add(side.scale(PLEKKEN[i][1]));
            int grond = sled.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, Mth.floor(doel.x), Mth.floor(doel.z));
            doel = new Vec3(doel.x, Mth.clamp(grond, origin.y - 1.5, origin.y + 1.5), doel.z);
            Vec3 at = span.naar(i, doel, yaw);
            Span.touw(haak, at.add(0, 0.34, 0), origin, pose, buffers, light);
            span.hond(i, at, origin, loop, sled.afstand * 2.2f, zit, false, dispatcher, partialTick, pose, buffers, light);
        }
    }
}
