package nl.juiced.guhs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhSleeEntity;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.slee.SleePath;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * The guh sled: geo/entity/guh_slee.geo.json, turned along the track and tilted on slopes, pulled by four little guhs
 * (two by two) that walk along the rails in front of it.
 */
public class GuhSleeRenderer extends GeoEntityRenderer<GuhSleeEntity> {
    /** Where the pullers walk: blocks ahead of the sled's middle, and sideways. */
    private static final double[][] PULLERS = {{1.55, -0.38}, {1.55, 0.38}, {2.55, -0.38}, {2.55, 0.38}};
    private static final float PULLER_SCALE = 0.6f;

    private final EntityRenderDispatcher dispatcher;

    public GuhSleeRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Guhs.id("guh_slee")));
        this.shadowRadius = 0.6f;
        this.dispatcher = context.getEntityRenderDispatcher();
    }

    /** GeckoLib only turns living entities by themselves: turn the sled to face along the track, and tilt it on slopes. */
    @Override
    protected void applyRotations(GuhSleeEntity sled, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(sled, poseStack, ageInTicks, Mth.rotLerp(partialTick, sled.yRotO, sled.getYRot()), partialTick, nativeScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, sled.xRotO, sled.getXRot())));
        // lean into the bends like a coaster car (the model's +x is its right side)
        float turn = Mth.wrapDegrees(sled.getYRot() - sled.yRotO);
        float lean = Mth.lerp(0.35f, sled.lean, Mth.clamp(turn * 2.5f, -25f, 25f));
        sled.lean = lean;
        poseStack.mulPose(Axis.ZP.rotationDegrees(-lean));
    }

    @Override
    public void render(GuhSleeEntity sled, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(sled, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        if (sled.getPiece() == null) {
            return;
        }
        if (sled.pullers.isEmpty()) {
            for (int i = 0; i < PULLERS.length; i++) {
                GuhEntity guh = ModEntities.GUH.get().create(sled.level(), EntitySpawnReason.TRIGGERED);
                if (guh == null) {
                    return;
                }
                guh.setGuhScale(PULLER_SCALE);
                guh.hideName = true;
                guh.wear(GuhClothes.RED_BOWTIE);
                sled.pullers.add(guh);
            }
        }
        // the sled is drawn where it is between two ticks; the path is followed from where it is now
        Vec3 drawnAt = new Vec3(Mth.lerp(partialTick, sled.xo, sled.getX()), Mth.lerp(partialTick, sled.yo, sled.getY()),
                Mth.lerp(partialTick, sled.zo, sled.getZ()));
        Vec3 behind = sled.position().subtract(drawnAt);
        for (int i = 0; i < PULLERS.length; i++) {
            SleePath.Point p = sled.pointAhead(PULLERS[i][0]);
            if (p == null) {
                continue;
            }
            Vec3 flat = new Vec3(p.heading().x, 0, p.heading().z).normalize();
            Vec3 side = new Vec3(-flat.z, 0, flat.x);
            Vec3 at = p.pos().add(side.scale(PULLERS[i][1])).subtract(0, SleePath.RIDE_HEIGHT - 0.1, 0).subtract(behind).subtract(drawnAt);
            float yaw = (float) Math.toDegrees(Math.atan2(-flat.x, flat.z));
            GuhEntity guh = sled.pullers.get(i);
            guh.setYRot(yaw);
            guh.yRotO = yaw;
            guh.yBodyRot = guh.yBodyRotO = guh.yHeadRot = guh.yHeadRotO = yaw;
            dispatcher.render(guh, at.x, at.y, at.z, yaw, partialTick, poseStack, bufferSource, packedLight);
        }
    }
}
