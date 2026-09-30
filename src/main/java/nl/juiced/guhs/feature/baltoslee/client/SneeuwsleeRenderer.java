package nl.juiced.guhs.feature.baltoslee.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.baltoslee.SledehondjeEntity;
import nl.juiced.guhs.feature.baltoslee.SneeuwsleeEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * Your own sneeuwslee: the sled model with its pink-and-blue blanket and little hearts, four guh-sledehondjes in front
 * (two by two, roped to it) that swing along through the bends; they sit down and wait when there's no snow.
 * <p>
 * 1.1.0 (GeckoLib 5 / MC 26.1): look, turn and team are worked out at extract time, the team is submitted with the sled.
 */
public class SneeuwsleeRenderer extends GeoEntityRenderer<SneeuwsleeEntity, EntityRenderState> {
    private static final Identifier TEX = Guhs.id("textures/entity/sneeuwslee.png");
    private static final double[][] PLEKKEN = {{2.0, -0.4}, {2.0, 0.4}, {3.05, -0.4}, {3.05, 0.4}};

    /** Yaw and lean of this frame (degrees). */
    private static final DataTicket<float[]> DRAAI = DataTicket.create("guhs_sneeuwslee_draai", float[].class);

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
        public Identifier getTextureResource(GeoRenderState state) {
            return TEX;
        }
    }

    @Override
    public void addRenderData(SneeuwsleeEntity sled, @Nullable Void related, EntityRenderState state, float partialTick) {
        float v = sled.getoondeSnelheid;
        state.addGeckolibData(SleeRenderer.UITERLIJK, new SleeRenderer.Uiterlijk(false, false, Math.min(1, v / 0.3f), sled.tickCount + partialTick));
        float yaw = Mth.rotLerp(partialTick, sled.yRotO, sled.getYRot());
        state.addGeckolibData(DRAAI, new float[]{yaw, Mth.lerp(partialTick, sled.leanO, sled.lean)});
        // the team (was drawn in render())
        List<Span.Lid> leden = new ArrayList<>();
        List<Vec3[]> touwen = new ArrayList<>();
        Span span = Span.van(sled, 4, SledehondjeEntity.GEWOON, false, false);
        Vec3 origin = new Vec3(Mth.lerp(partialTick, sled.xo, sled.getX()), Mth.lerp(partialTick, sled.yo, sled.getY()), Mth.lerp(partialTick, sled.zo, sled.getZ()));
        Vec3 fwd = Vec3.directionFromRotation(0, yaw);
        Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
        float zit = !sled.opSneeuwGezien() || v < 0.015f ? 1f : 0f;
        float loop = Math.min(1f, v / 0.3f);
        Vec3 haak = origin.add(fwd.scale(1.15)).add(0, 0.42, 0);
        for (int i = 0; i < PLEKKEN.length; i++) {
            Vec3 doel = origin.add(fwd.scale(PLEKKEN[i][0])).add(side.scale(PLEKKEN[i][1]));
            int grond = sled.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, Mth.floor(doel.x), Mth.floor(doel.z));
            doel = new Vec3(doel.x, Mth.clamp(grond, origin.y - 1.5, origin.y + 1.5), doel.z);
            Vec3 at = span.naar(i, doel, yaw);
            touwen.add(new Vec3[]{haak.subtract(origin), at.add(0, 0.34, 0).subtract(origin)});
            Span.Lid lid = span.hond(i, at, origin, loop, sled.afstand * 2.2f, zit, false, dispatcher, partialTick, state.lightCoords);
            if (lid != null) {
                leden.add(lid);
            }
        }
        state.addGeckolibData(SleeRenderer.TEAM, new SleeRenderer.Team(leden, touwen));
    }

    @Override
    protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
        float[] d = info.getGeckolibData(DRAAI);
        if (d == null) {
            return;
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - d[0]));
        poseStack.mulPose(Axis.ZP.rotationDegrees(d[1]));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info, BoneSnapshots bones) {
        SleeRenderer.Uiterlijk u = info.getGeckolibData(SleeRenderer.UITERLIJK);
        if (u != null) {
            SleeRenderer.bonen(bones, true, u.v(), u.tijd());
        }
    }

    @Override
    public void submit(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        SleeRenderer.Team team = state.getGeckolibData(SleeRenderer.TEAM);
        if (team != null) {
            Span.touwen(team.touwen(), state.lightCoords, pose, collector);
            Span.submit(team.leden(), dispatcher, camera, pose, collector);
        }
    }
}
