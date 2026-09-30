package nl.juiced.guhs.feature.guhwaiispellen.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhwaiispellen.SurfPlankEntity;
import nl.juiced.guhs.feature.guhwaiispellen.SurfSim;
import nl.juiced.guhs.registry.ModEntities;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * The surfplankje (geo/entity/guhwaiispellen_surfplank.geo.json: a long rounded board, pink with a turquoise stripe, a
 * hibiscus and a little guh face on the nose, a fin underneath), turned along the ride and tilted with the face of the
 * wave. On Lilo-guh's board Lilo-guh herself sits and rides along (a copy of her, only drawn, never in the world).
 */
public class SurfPlankRenderer extends GeoEntityRenderer<SurfPlankEntity> {
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
    protected void applyRotations(SurfPlankEntity bord, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - yaw(bord, partialTick)));
        poseStack.mulPose(Axis.XP.rotationDegrees(-helling(bord, partialTick)));
        if (bord.fase() == SurfSim.Fase.PLONS) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(160f));                    // (upside down after a PLONS)
        }
    }

    @Override
    public void render(SurfPlankEntity bord, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        super.render(bord, entityYaw, partialTick, poseStack, buffers, light);
        if (!bord.isLilo() || bord.fase() == SurfSim.Fase.PLONS) {
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
        float yaw = yaw(bord, partialTick);
        lilo.setYRot(yaw);
        lilo.yRotO = yaw;
        lilo.yBodyRot = lilo.yBodyRotO = lilo.yHeadRot = lilo.yHeadRotO = yaw;
        lilo.tickCount = bord.tickCount;
        poseStack.pushPose();
        dispatcher.render(lilo, 0, SurfPlankEntity.STAAN, 0, yaw, partialTick, poseStack, buffers, light);
        poseStack.popPose();
    }
}
