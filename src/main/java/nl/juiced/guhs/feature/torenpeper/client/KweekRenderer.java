package nl.juiced.guhs.feature.torenpeper.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.torenpeper.KweekbakBlock;

/**
 * Draws a kweekbak the way the LOCAL player has it: the trough with their own plant of that kind in it, at the stage it has
 * reached by now ({@link TorenpeperClient#groei}). The block itself is invisible in the world's own drawing
 * ({@link KweekbakBlock}); what you see is always this: the block's model for the state {@code groei} = 0..4. Nothing in
 * the world changes, so any number of players grow their peppers in the same three troughs. (The same way the
 * Fossiel-opgraving draws its sand and its stand.)
 */
public class KweekRenderer implements BlockEntityRenderer<KweekbakBlock.Bak, KweekRenderer.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    public static class State extends BlockEntityRenderState {
        final BlockModelRenderState blok = new BlockModelRenderState();
    }

    private final BlockModelResolver blokken;

    public KweekRenderer(BlockEntityRendererProvider.Context context) {
        this.blokken = context.blockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KweekbakBlock.Bak be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.blok.clear();
        BlockState bs = be.getBlockState();
        if (bs.getBlock() instanceof KweekbakBlock) {
            blokken.update(state.blok, bs.setValue(KweekbakBlock.GROEI, TorenpeperClient.groei(bs.getValue(KweekbakBlock.SOORT))), DISPLAY);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.blok.isEmpty()) {
            state.blok.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    /** (the plant stands on the trough: it reaches into the block above) */
    @Override
    public AABB getRenderBoundingBox(KweekbakBlock.Bak be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1.0, 0);
    }
}
