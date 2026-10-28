package nl.juiced.guhs.feature.slaapzorg.client;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.slaapzorg.MobielBlockEntity;
import nl.juiced.guhs.feature.slaapzorg.SlaapBlokken;

/**
 * The Mobiel's hanging moon, stars and little guh (models/block/mobiel_draai.json), turning slowly under the hook, and
 * faster while the lullaby plays. The hook and the bar are the normal block model.
 */
public class MobielRenderer implements BlockEntityRenderer<MobielBlockEntity, MobielRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> DRAAI = new StandaloneModelKey<>(() -> "guhs:block/mobiel_draai");
    /** Degrees per tick, quiet and playing. */
    public static final float RUSTIG = 0.8f, SPEELT = 3.5f;

    public static class State extends BlockEntityRenderState {
        float hoek;
    }

    public MobielRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(DRAAI, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/mobiel_draai")));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MobielBlockEntity mobiel, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(mobiel, state, partialTick, camera, breakProgress);
        boolean speelt = mobiel.getBlockState().hasProperty(SlaapBlokken.Speeldoos.SPEELT) && mobiel.getBlockState().getValue(SlaapBlokken.Speeldoos.SPEELT) > 0;
        long t = mobiel.getLevel() == null ? 0 : mobiel.getLevel().getGameTime();
        state.hoek = ((t % 3600) + partialTick) * (speelt ? SPEELT : RUSTIG) % 360f;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.hoek));
        pose.translate(-0.5, 0, -0.5);
        collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(DRAAI)),
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MobielBlockEntity mobiel) {
        return new AABB(mobiel.getBlockPos()).inflate(0.5);
    }
}
