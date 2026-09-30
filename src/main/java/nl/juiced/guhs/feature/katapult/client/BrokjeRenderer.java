package nl.juiced.guhs.feature.katapult.client;

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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.katapult.KatapultBrokjeEntity;

/**
 * A tumbling piece of a Mika fort: its block, turning over as it flies (each piece its own way).
 * <p>
 * 1.1.0: render state + submit; the block model is resolved at extract time (like a minecart's block).
 */
public class BrokjeRenderer extends EntityRenderer<KatapultBrokjeEntity, BrokjeRenderer.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    public static class State extends EntityRenderState {
        final BlockModelRenderState block = new BlockModelRenderState();
        boolean zichtbaar;
        boolean draait;
        int seed;
    }

    public BrokjeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KatapultBrokjeEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        BlockState block = entity.getBlockState();
        state.zichtbaar = block.getRenderShape() == RenderShape.MODEL;
        if (state.zichtbaar) {
            Minecraft.getInstance().getBlockModelResolver().update(state.block, block, DISPLAY);
        }
        state.draait = entity.getDeltaMovement().length() > 0.05;
        state.seed = entity.getId();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.zichtbaar) {
            return;
        }
        {
            pose.pushPose();
            pose.translate(0, 0.5, 0);
            float age = state.ageInTicks;
            if (state.draait) {
                int seed = state.seed;
                pose.mulPose(Axis.XP.rotationDegrees(age * (8 + seed % 7)));
                pose.mulPose(Axis.ZP.rotationDegrees(age * (5 + seed % 5) * (seed % 2 == 0 ? 1 : -1)));
            }
            pose.translate(-0.5, -0.5, -0.5);
            state.block.submitMultiLayer(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }
}
