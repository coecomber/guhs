package nl.juiced.guhs.feature.mijnspellen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import nl.juiced.guhs.feature.mijnspellen.kegelgrot.KegelEntity;
import nl.juiced.guhs.feature.mijnspellen.kegelgrot.KegelgrotFeature;

/**
 * A pin, the wool ball or the Mika-blokje of the Kegelgrot: the block model of the deco pin (kegeltje, a bit smaller,
 * lying down the way it fell), of the ball (rolling) or of the Mika-blokje.
 */
public class KegelRenderer extends EntityRenderer<KegelEntity, KegelRenderer.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
    /** The pins on the lane are smaller than the deco pin. */
    private static final float KEGEL_SCHAAL = 0.6f;

    public static class State extends EntityRenderState {
        final BlockModelRenderState block = new BlockModelRenderState();
        int soort;
        boolean om;
        float yaw, rol;
    }

    public KegelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KegelEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        Minecraft.getInstance().getBlockModelResolver().update(state.block, entity.model(), DISPLAY);
        state.soort = entity.getType() == KegelgrotFeature.BAL.get() ? 1 : entity.getType() == KegelgrotFeature.SCHUIF.get() ? 2 : 0;
        state.om = entity.om();
        state.yaw = entity.getYRot(partialTick);
        state.rol = entity.rol();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        if (state.soort == 0) {
            if (state.om) {
                pose.translate(0, 0.12, 0);
                pose.mulPose(Axis.XP.rotationDegrees(90));
            }
            pose.scale(KEGEL_SCHAAL, KEGEL_SCHAAL, KEGEL_SCHAAL);
            pose.translate(-0.5, 0, -0.5);
        } else if (state.soort == 1) {
            pose.translate(0, 0.2, 0);
            pose.mulPose(Axis.XP.rotation(state.rol % Mth.TWO_PI));
            pose.translate(-0.5, -0.2, -0.5);
        } else {
            pose.translate(-0.5, 0, -0.5);
        }
        state.block.submitMultiLayer(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
