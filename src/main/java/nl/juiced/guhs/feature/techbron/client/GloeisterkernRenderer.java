package nl.juiced.guhs.feature.techbron.client;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.techbron.BronBlock;
import nl.juiced.guhs.feature.techbron.GloeisterkernBlock;
import nl.juiced.guhs.feature.vadskracht.Snoet;

/**
 * The gloeister in the cage of a Gloeisterkern (the model {@code gloeisterkern_ster}: a star of a little cube with six
 * points): it turns, tilted, bobs a little and always shines at full brightness. In a kern that does not count in its net
 * (a second one) it turns slowly. The foot and the cage are the block's own model.
 */
public class GloeisterkernRenderer implements BlockEntityRenderer<GloeisterkernBlock.Kern, GloeisterkernRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> STER = new StandaloneModelKey<>(() -> "guhs:block/gloeisterkern_ster");
    /** The middle of the cage, in model pixels. */
    private static final float MIDDEN = 11f / 16f;

    public static class State extends BlockEntityRenderState {
        float tijd;
        boolean slaapt;
    }

    public GloeisterkernRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Mod bus (client): the star, which is no block state of its own. */
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(STER, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/gloeisterkern_ster")));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GloeisterkernBlock.Kern kern, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(kern, state, partialTick, camera, breakProgress);
        state.tijd = kern.getLevel() == null ? 0 : (kern.getLevel().getGameTime() % 7200L) + partialTick;
        state.slaapt = kern.getBlockState().getValue(BronBlock.SNOET) == Snoet.SLAAPT;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float tempo = state.slaapt ? 0.6f : 4f;
        pose.pushPose();
        pose.translate(0.5, MIDDEN + (state.slaapt ? 0f : 0.02f * (float) Math.sin(state.tijd * 0.1f)), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.tijd * tempo));
        pose.mulPose(Axis.XP.rotationDegrees(35f));
        pose.mulPose(Axis.ZP.rotationDegrees(35f));
        pose.translate(-0.5, -0.5, -0.5);   // (the star is modelled around the middle of the block)
        collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(STER)),
                BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
