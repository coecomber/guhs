package nl.juiced.guhs.feature.hemel.client;

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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.hemel.KnuffelhartBlockEntity;

/**
 * The Knuffelhart itself (models/block/knuffelhart_hart.json) floating under its glass dome, full bright, turning gently to
 * and fro and bobbing. Once it beats for you it goes "ba-dum" ({@link #SLAG} ticks per beat, two bumps) with a soft pink
 * glow around it (knuffelhart_gloed.json); while it still sleeps it only breathes slowly, a little smaller.
 * <p>
 * 1.1.0: standalone models + {@code submitBlockModel} (cutout / translucent block sheet as before). (1.0.0 passed a grey
 * colour for the sleeping heart, but the model has no tint index, so it never showed: dropped.)
 */
public class KnuffelhartRenderer implements BlockEntityRenderer<KnuffelhartBlockEntity, KnuffelhartRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> HART = new StandaloneModelKey<>(() -> "guhs:block/knuffelhart_hart");
    public static final StandaloneModelKey<BlockStateModelPart> GLOED = new StandaloneModelKey<>(() -> "guhs:block/knuffelhart_gloed");
    /** Ticks per heartbeat (about 50 beats a minute: a calm, happy heart). */
    public static final int SLAG = 24;
    /** The middle of the heart in model pixels. */
    private static final float MX = 8.5f / 16f, MY = 9f / 16f, MZ = 8f / 16f;

    public static class State extends BlockEntityRenderState {
        boolean wakker;
        float tijd;
    }

    public KnuffelhartRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Mod bus (client): the heart and its glow, which are no block states of their own. */
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(HART, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/knuffelhart_hart")));
        event.register(GLOED, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/knuffelhart_gloed")));
    }

    /** The heart's size now: ba-dum (awake) or slow breathing (asleep). */
    public static float klop(float tijd, boolean wakker) {
        if (!wakker) {
            return 0.9f + 0.03f * Mth.sin(tijd * 0.08f);
        }
        float f = (tijd % SLAG) / SLAG;
        float ba = (float) Math.exp(-Math.pow((f - 0.05f) / 0.05f, 2)) * 0.16f;
        float dum = (float) Math.exp(-Math.pow((f - 0.25f) / 0.06f, 2)) * 0.1f;
        return 1f + ba + dum;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KnuffelhartBlockEntity hart, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(hart, state, partialTick, camera, breakProgress);
        state.wakker = HemelClient.klopt();
        state.tijd = hart.tijd + partialTick;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float tijd = state.tijd;
        float schaal = klop(tijd, state.wakker);
        var models = Minecraft.getInstance().getModelManager();
        pose.pushPose();
        pose.translate(MX, MY + Mth.sin(tijd * 0.05f) * 0.03f, MZ);
        pose.mulPose(Axis.YP.rotationDegrees(Mth.sin(tijd * 0.03f) * 28f));
        pose.scale(schaal, schaal, schaal);
        pose.translate(-MX, -MY, -MZ);
        collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(models.getStandaloneModel(HART)),
                BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        if (state.wakker) {
            float gloed = 1f + (schaal - 1f) * 1.8f;
            pose.translate(MX, MY, MZ);
            pose.scale(gloed, gloed, gloed);
            pose.translate(-MX, -MY, -MZ);
            collector.submitBlockModel(pose, Sheets.translucentBlockSheet(), List.of(models.getStandaloneModel(GLOED)),
                    BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(KnuffelhartBlockEntity hart) {
        return new AABB(hart.getBlockPos()).inflate(0.5);
    }
}
