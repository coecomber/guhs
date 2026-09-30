package nl.juiced.guhs.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhSleeEntity;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.slee.SleePath;

/**
 * The guh sled: geckolib/models/entity/guh_slee.geo.json, turned along the track and tilted on slopes, pulled by four
 * little guhs (two by two) that walk along the rails in front of it.
 * <p>
 * 1.1.0: the pullers are drawn like vanilla draws passengers: their render states are extracted with the sled
 * ({@link EntityRenderDispatcher#extractEntity}) and submitted after it ({@link EntityRenderDispatcher#submit}).
 */
public class GuhSleeRenderer extends GeoEntityRenderer<GuhSleeEntity, EntityRenderState> {
    /** Where the pullers walk: blocks ahead of the sled's middle, and sideways. */
    private static final double[][] PULLERS = {{1.55, -0.38}, {1.55, 0.38}, {2.55, -0.38}, {2.55, 0.38}};
    private static final float PULLER_SCALE = 0.6f;

    /** Pitch and lean of this frame (degrees). */
    private static final DataTicket<float[]> TILT = DataTicket.create("guhs_slee_tilt", float[].class);
    private static final DataTicket<List<Puller>> PULLER_STATES = DataTicket.create("guhs_slee_pullers", new TypeToken<List<Puller>>() {});

    private record Puller(EntityRenderState state, Vec3 at) {
    }

    private final EntityRenderDispatcher dispatcher;

    public GuhSleeRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Guhs.id("guh_slee")));
        this.shadowRadius = 0.6f;
        this.dispatcher = context.getEntityRenderDispatcher();
    }

    @Override
    public void addRenderData(GuhSleeEntity sled, @Nullable Void related, EntityRenderState state, float partialTick) {
        // GeckoLib only turns living entities by themselves: face along the track, tilt on slopes
        state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, sled.yRotO, sled.getYRot()));
        // lean into the bends like a coaster car (the model's +x is its right side)
        float turn = Mth.wrapDegrees(sled.getYRot() - sled.yRotO);
        float lean = Mth.lerp(0.35f, sled.lean, Mth.clamp(turn * 2.5f, -25f, 25f));
        sled.lean = lean;
        state.addGeckolibData(TILT, new float[]{Mth.lerp(partialTick, sled.xRotO, sled.getXRot()), lean});
        state.addGeckolibData(PULLER_STATES, pullers(sled, state, partialTick));
    }

    @Override
    protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
        super.applyRotations(info, poseStack, nativeScale);
        float[] tilt = info.getGeckolibData(TILT);
        if (tilt != null) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-tilt[0]));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-tilt[1]));
        }
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        List<Puller> pullers = state.getGeckolibData(PULLER_STATES);
        if (pullers != null) {
            for (Puller p : pullers) {
                dispatcher.submit(p.state(), camera, p.at().x, p.at().y, p.at().z, poseStack, collector);
            }
        }
    }

    /** The four walking guhs of this frame (positions relative to where the sled is drawn). */
    private List<Puller> pullers(GuhSleeEntity sled, EntityRenderState sledState, float partialTick) {
        List<Puller> out = new ArrayList<>(PULLERS.length);
        if (sled.getPiece() == null) {
            return out;
        }
        if (sled.pullers.isEmpty()) {
            for (int i = 0; i < PULLERS.length; i++) {
                GuhEntity guh = ModEntities.GUH.get().create(sled.level(), EntitySpawnReason.TRIGGERED);
                if (guh == null) {
                    return out;
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
            EntityRenderState state = dispatcher.extractEntity(guh, partialTick);
            state.lightCoords = sledState.lightCoords;      // (the fake guh stands nowhere: use the sled's light)
            state.shadowPieces.clear();
            out.add(new Puller(state, at));
        }
        return out;
    }
}
