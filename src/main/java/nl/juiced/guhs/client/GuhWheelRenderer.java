package nl.juiced.guhs.client;

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
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * Draws the spinning ring (models/block/guh_wheel_ring.json) and the guh running inside it.
 * The stand itself is the normal block model (models/block/guh_wheel.json).
 * <p>
 * 1.1.0: extra block models are NeoForge standalone models ({@link StandaloneModelKey} registered in
 * {@link ModelEvent.RegisterStandalone}, fetched with {@code ModelManager#getStandaloneModel}) and submitted with
 * {@code submitBlockModel}; the guh is extracted as its own entity render state.
 */
public class GuhWheelRenderer implements BlockEntityRenderer<GuhWheelBlockEntity, GuhWheelRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> RING_MODEL = new StandaloneModelKey<>(() -> "guhs:block/guh_wheel_ring");
    public static final StandaloneModelKey<BlockStateModelPart> STAND_MODEL = new StandaloneModelKey<>(() -> "guhs:block/guh_wheel_stand");
    /** How much bigger than the item model the placed wheel is (about 3 blocks wide and tall). */
    public static final float SIZE = 2.0f;
    /** Centre of the ring in model pixels (see guh_wheel_ring.json). */
    private static final float HUB_Y = 14f / 16f;
    /** Inside bottom of the ring, where the guh's feet go. */
    private static final float FLOOR_Y = 5.75f / 16f;
    /** How long (in blocks) the guh should look inside the wheel, whatever its real size. */
    private static final float GUH_LENGTH_IN_WHEEL = 0.75f;
    private static final float GUH_MODEL_LENGTH = 23f / 16f;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float spin;
        @Nullable
        EntityRenderState guh;
        float fit = 1f;
    }

    private final EntityRenderDispatcher entityRenderer;

    public GuhWheelRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    /** Mod bus (client): the ring and stand models, which are no block state of their own. */
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(RING_MODEL, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/guh_wheel_ring")));
        event.register(STAND_MODEL, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/guh_wheel_stand")));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GuhWheelBlockEntity wheel, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(wheel, state, partialTick, camera, breakProgress);
        state.facing = wheel.getBlockState().getValue(GuhWheelBlock.FACING);
        state.spin = wheel.getSpin(partialTick);
        state.guh = null;
        GuhEntity guh = wheel.getDisplayGuh();
        if (guh != null) {
            // the guh, running "forwards" along the ring, shrunk (or grown) to fit
            state.fit = GUH_LENGTH_IN_WHEEL / (GUH_MODEL_LENGTH * guh.getScale() * guh.getAgeScale());
            guh.setYRot(90f);
            guh.yBodyRot = guh.yBodyRotO = guh.yHeadRot = guh.yHeadRotO = 90f;
            guh.setXRot(0);
            state.guh = entityRenderer.extractEntity(guh, partialTick);
            state.guh.lightCoords = state.lightCoords;
            state.guh.shadowPieces.clear();                   // (was setRenderShadow(false))
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        var models = Minecraft.getInstance().getModelManager();
        poseStack.pushPose();
        // rotate the whole thing like a block model (models face north by default), and make it big
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180f));
        poseStack.scale(SIZE, SIZE, SIZE);
        poseStack.translate(-0.5, 0, -0.5);
        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), List.of(models.getStandaloneModel(STAND_MODEL)),
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        // spinning ring around its hub (the wheel turns around the model's Z axis)
        poseStack.pushPose();
        poseStack.translate(0.5, HUB_Y, 0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.spin));
        poseStack.translate(-0.5, -HUB_Y, -0.5);
        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), List.of(models.getStandaloneModel(RING_MODEL)),
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

        if (state.guh != null) {
            poseStack.pushPose();
            poseStack.translate(0.5, FLOOR_Y, 0.5);
            poseStack.scale(state.fit, state.fit, state.fit);
            entityRenderer.submit(state.guh, camera, 0, 0, 0, poseStack, collector);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true; // the ring sticks out above the block
    }

    @Override
    public AABB getRenderBoundingBox(GuhWheelBlockEntity wheel) {
        return new AABB(wheel.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 3, 0);
    }
}
