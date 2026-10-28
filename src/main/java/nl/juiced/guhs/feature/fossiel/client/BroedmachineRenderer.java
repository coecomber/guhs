package nl.juiced.guhs.feature.fossiel.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnifferEggBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.fossiel.BroedmachineBlockEntity;

/**
 * The egg in the Fossiel-broedmachine's glass bell: a little sniffer-like egg that slowly turns (faster when it really
 * broods), cracks as it gets further (the egg's hatch stages 0..2) and wobbles just before it hatches. No egg: an empty bell.
 */
public class BroedmachineRenderer implements BlockEntityRenderer<BroedmachineBlockEntity, BroedmachineRenderer.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    public static class State extends BlockEntityRenderState {
        final BlockModelRenderState ei = new BlockModelRenderState();
        float draai, wiebel;
    }

    private final BlockModelResolver blokken;

    public BroedmachineRenderer(BlockEntityRendererProvider.Context context) {
        this.blokken = context.blockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BroedmachineBlockEntity be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.ei.clear();
        if (be.ei() == null) {
            return;
        }
        float v = be.voortgang();
        int barst = v < 0.4f ? 0 : v < 0.8f ? 1 : 2;
        blokken.update(state.ei, Blocks.SNIFFER_EGG.defaultBlockState().setValue(SnifferEggBlock.HATCH, barst), DISPLAY);
        long nu = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        float t = (nu % 3600) + partialTick;
        state.draai = t * (be.bezig() ? 2.4f : 0.6f);
        state.wiebel = v > 0.9f ? (float) Math.sin(t * 0.9f) * 9f : 0f;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.ei.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0.58, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.draai));
        pose.mulPose(Axis.ZP.rotationDegrees(state.wiebel));
        pose.scale(0.38f, 0.38f, 0.38f);
        pose.translate(-0.5, 0, -0.5);
        state.ei.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BroedmachineBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.2);
    }
}
