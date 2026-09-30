package nl.juiced.guhs.feature.guhwaiispellen.client;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhwaiispellen.SurfPlankEntity;
import nl.juiced.guhs.feature.guhwaiispellen.SurfSim;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The surfplankje (geckolib/models/entity/guhwaiispellen_surfplank.geo.json: a long rounded board, pink with a turquoise stripe, a
 * hibiscus and a little guh face on the nose, a fin underneath), turned along the ride and tilted with the face of the
 * wave. On Lilo-guh's board Lilo-guh herself sits and rides along (a copy of her, only drawn, never in the world).
 */
public class SurfPlankRenderer extends GeoEntityRenderer<SurfPlankEntity, EntityRenderState> {
    /** Yaw, tilt (degrees) and PLONS (1 = upside down) of this frame. */
    private static final DataTicket<float[]> HOUDING = DataTicket.create("guhs_surfplank_houding", float[].class);
    /** Lilo-guh riding along on her own board (a render state of the drawn copy). */
    private static final DataTicket<EntityRenderState> LILO = DataTicket.create("guhs_surfplank_lilo", EntityRenderState.class);

    private final EntityRenderDispatcher dispatcher;
    private final Map<SurfPlankEntity, GuhNpcEntity> lilos = new WeakHashMap<>();

    public SurfPlankRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Guhs.id("guhwaiispellen_surfplank")));
        this.dispatcher = context.getEntityRenderDispatcher();
        this.shadowRadius = 0.4f;
    }

    static float yaw(SurfPlankEntity bord, float pt) {
        return bord.eigen ? Mth.rotLerp(pt, bord.eigenYawO, bord.eigenYaw) : Mth.rotLerp(pt, bord.yRotO, bord.getYRot());
    }

    static float helling(SurfPlankEntity bord, float pt) {
        return bord.eigen ? Mth.lerp(pt, bord.eigenHellingO, bord.eigenHelling) : Mth.lerp(pt, bord.xRotO, bord.getXRot());
    }

    @Override
    public void addRenderData(SurfPlankEntity bord, @Nullable Void related, EntityRenderState state, float partialTick) {
        float yaw = yaw(bord, partialTick);
        boolean plons = bord.fase() == SurfSim.Fase.PLONS;
        state.addGeckolibData(HOUDING, new float[]{yaw, helling(bord, partialTick), plons ? 1f : 0f});
        if (!bord.isLilo() || plons) {
            return;
        }
        GuhNpcEntity lilo = lilos.computeIfAbsent(bord, b -> {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(b.level(), EntitySpawnReason.TRIGGERED);
            if (npc != null) {
                npc.setKind(GuhNpcEntity.Kind.LILO_GUH);
                npc.setCustomNameVisible(false);
            }
            return npc;
        });
        if (lilo == null) {
            return;
        }
        lilo.setYRot(yaw);
        lilo.yRotO = yaw;
        lilo.yBodyRot = lilo.yBodyRotO = lilo.yHeadRot = lilo.yHeadRotO = yaw;
        lilo.tickCount = bord.tickCount;
        EntityRenderState liloState = dispatcher.extractEntity(lilo, partialTick);
        liloState.lightCoords = state.lightCoords;          // (the copy stands nowhere: use the board's light)
        liloState.shadowPieces.clear();
        state.addGeckolibData(LILO, liloState);
    }

    /** Turned along the ride and tilted with the wave (replaces GeckoLib's own turning, like 1.0.0). */
    @Override
    protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
        float[] h = info.getGeckolibData(HOUDING);
        if (h == null) {
            return;
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - h[0]));
        poseStack.mulPose(Axis.XP.rotationDegrees(-h[1]));
        if (h[2] > 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(160f));                    // (upside down after a PLONS)
        }
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        EntityRenderState lilo = state.getGeckolibData(LILO);
        if (lilo != null) {
            dispatcher.submit(lilo, camera, 0, SurfPlankEntity.STAAN, 0, poseStack, collector);
        }
    }
}
