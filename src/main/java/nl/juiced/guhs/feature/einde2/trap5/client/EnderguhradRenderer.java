package nl.juiced.guhs.feature.einde2.trap5.client;

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
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.einde2.trap5.Enderguhrad;
import nl.juiced.guhs.feature.techbron.BronBlock;

/**
 * Draws the Enderguhrad: the Guhrad's stand and ring (models {@code guhs:block/enderguhrad_stand} / {@code _ring}, children of
 * the Guhrad's models in ender colours, tools/features/einde2_trap5_machines.py) three times as big, turning, and the Vahoege
 * Enderguh in the middle of it, flying its rounds with its ears flapping in the wind. The kern block's own model is only the
 * low plinth with the snoet.
 */
public class EnderguhradRenderer implements BlockEntityRenderer<Enderguhrad.BlockEntityImpl, EnderguhradRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> RING = new StandaloneModelKey<>(() -> "guhs:block/enderguhrad_ring");
    public static final StandaloneModelKey<BlockStateModelPart> STAND = new StandaloneModelKey<>(() -> "guhs:block/enderguhrad_stand");
    /** How much bigger than the Guhrad's item model (the Guhrad itself is drawn at 2). */
    public static final float SIZE = 3.0f;
    /** The ring's hub in model pixels (as the Guhrad's ring). */
    private static final float HUB_Y = 14f / 16f;
    /** How big the guh looks in the wheel (blocks). */
    private static final float GUH_LENGTE = 1.6f, GUH_MODEL_LENGTE = 23f / 16f;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float draai;
        float tijd;
        @Nullable
        EntityRenderState guh;
        float fit = 1f;
    }

    private final EntityRenderDispatcher entities;

    public EnderguhradRenderer(BlockEntityRendererProvider.Context context) {
        this.entities = context.entityRenderer();
    }

    public static void modellen(ModelEvent.RegisterStandalone event) {
        event.register(RING, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/enderguhrad_ring")));
        event.register(STAND, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/enderguhrad_stand")));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Enderguhrad.BlockEntityImpl rad, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(rad, state, partialTick, camera, breakProgress);
        state.facing = rad.getBlockState().getValue(BronBlock.FACING);
        state.draai = rad.draai(partialTick);
        state.guh = null;
        GuhEntity guh = rad.toonGuh();
        if (guh != null) {
            state.fit = GUH_LENGTE / (GUH_MODEL_LENGTE * guh.getScale() * guh.getAgeScale());
            state.tijd = guh.tickCount + partialTick;
            guh.setYRot(90f);
            guh.yBodyRot = guh.yBodyRotO = guh.yHeadRot = guh.yHeadRotO = 90f;
            guh.setXRot(0);
            state.guh = entities.extractEntity(guh, partialTick);
            state.guh.lightCoords = state.lightCoords;
            state.guh.shadowPieces.clear();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        var models = Minecraft.getInstance().getModelManager();
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180f));
        poseStack.scale(SIZE, SIZE, SIZE);
        poseStack.translate(-0.5, 0, -0.5);
        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), List.of(models.getStandaloneModel(STAND)),
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.pushPose();
        poseStack.translate(0.5, HUB_Y, 0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.draai));
        poseStack.translate(-0.5, -HUB_Y, -0.5);
        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), List.of(models.getStandaloneModel(RING)),
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        poseStack.popPose();
        if (state.guh != null) {
            // the Enderguh hovers in the middle of the wheel (at its hub, blocks up), bobbing a little as it flies its rounds
            poseStack.pushPose();
            poseStack.translate(0.5, HUB_Y * SIZE - 0.45 + 0.08 * Math.sin(state.tijd * 0.15), 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180f));
            poseStack.scale(state.fit, state.fit, state.fit);
            entities.submit(state.guh, camera, 0, 0, 0, poseStack, collector);
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(Enderguhrad.BlockEntityImpl rad) {
        return new AABB(rad.getBlockPos()).inflate(3, 0, 3).expandTowards(0, 5, 0);
    }
}
