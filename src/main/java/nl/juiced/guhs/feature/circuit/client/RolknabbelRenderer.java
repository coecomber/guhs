package nl.juiced.guhs.feature.circuit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import nl.juiced.guhs.feature.circuit.RolknabbelEntity;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * A rolling kaasknabbel: a big block of kaasknabbels that turns over as it rolls down the Knabbelhelling.
 * 1.1.0: render state + submit (the block model is resolved at extract time, like vanilla's primed TNT).
 */
public class RolknabbelRenderer extends EntityRenderer<RolknabbelEntity, RolknabbelRenderer.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    public static class State extends EntityRenderState {
        final BlockModelRenderState block = new BlockModelRenderState();
        float yaw;
        float rol;
    }

    private final BlockModelResolver blocks;

    public RolknabbelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.55f;
        this.blocks = context.getBlockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RolknabbelEntity knabbel, State state, float partialTick) {
        super.extractRenderState(knabbel, state, partialTick);
        state.yaw = Mth.lerp(partialTick, knabbel.yRotO, knabbel.getYRot());
        state.rol = Mth.lerp(partialTick, knabbel.rolO, knabbel.rol);
        blocks.update(state.block, ModBlocks.BLOCK_OF_KAASKNABBELS.get().defaultBlockState(), DISPLAY);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0, 0.6, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        pose.mulPose(Axis.XP.rotation(state.rol));
        pose.scale(1.15f, 1.15f, 1.15f);
        pose.translate(-0.5, -0.5, -0.5);
        if (!state.block.isEmpty()) {
            state.block.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
