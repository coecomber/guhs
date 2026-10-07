package nl.juiced.guhs.feature.guhpixel.kantoor.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.client.GuhPop;
import nl.juiced.guhs.feature.guhpixel.kantoor.BureautjeBlock;
import nl.juiced.guhs.feature.guhpixel.kantoor.BureautjeBlockEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;

/**
 * The guh that "works" at a Bureautje: a stand-in built from the looks the block entity holds (the real guh is safe in
 * the saved data), lying across the keyboard with its eyes shut, shrunk to fit the little desk. The desk itself is the
 * normal block model.
 */
public class BureautjeRenderer implements BlockEntityRenderer<BureautjeBlockEntity, BureautjeRenderer.State> {
    /** How long (in blocks) the guh looks on the desk, whatever its real size. */
    private static final float LENGTE = 0.66f;
    private static final float GUH_MODEL_LENGTE = 23f / 16f;
    /** On the keyboard: model pixels (the desk top is 8 high, the keyboard 1). */
    private static final float Y = 9f / 16f, Z = (7.5f - 8f) / 16f;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        @Nullable
        EntityRenderState guh;
        float fit = 1f;
    }

    private final EntityRenderDispatcher entityRenderer;

    public BureautjeRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BureautjeBlockEntity bureau, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(bureau, state, partialTick, camera, breakProgress);
        state.facing = bureau.getBlockState().getValue(BureautjeBlock.FACING);
        state.guh = null;
        if (!bureau.bezet()) {
            bureau.weergave = null;
            bureau.weergaveVan = null;
            return;
        }
        if (bureau.weergave == null || !bureau.guh().equals(bureau.weergaveVan)) {
            GuhEntity nieuw = GuhPop.van(bureau.looks());
            if (nieuw != null) {
                nieuw.setInSittingPose(true);
                GuhHooks.zet(nieuw, SleepInNestGoal.OOGJES_DICHT, true);
            }
            bureau.weergave = nieuw;
            bureau.weergaveVan = bureau.guh();
        }
        GuhEntity guh = bureau.weergave;
        if (guh != null) {
            state.fit = LENGTE / (GUH_MODEL_LENGTE * Math.max(0.2f, guh.getScale() * guh.getAgeScale()));
            // (across the desk: its side towards whoever stands at the stool)
            guh.setYRot(90f);
            guh.yBodyRot = guh.yBodyRotO = guh.yHeadRot = guh.yHeadRotO = 90f;
            guh.setXRot(0);
            state.guh = entityRenderer.extractEntity(guh, partialTick);
            state.guh.lightCoords = state.lightCoords;
            state.guh.shadowPieces.clear();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.guh == null) {
            return;
        }
        poseStack.pushPose();
        // turned like the block model (models face north by default)
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180f));
        poseStack.translate(0, Y, Z);
        poseStack.scale(state.fit, state.fit, state.fit);
        entityRenderer.submit(state.guh, camera, 0, 0, 0, poseStack, collector);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BureautjeBlockEntity bureau) {
        return new AABB(bureau.getBlockPos()).inflate(0.5, 0, 0.5).expandTowards(0, 1, 0);
    }
}
