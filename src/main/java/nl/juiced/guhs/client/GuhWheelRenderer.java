package nl.juiced.guhs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * Draws the spinning ring (models/block/guh_wheel_ring.json) and the guh running inside it.
 * The stand itself is the normal block model (models/block/guh_wheel.json).
 */
public class GuhWheelRenderer implements BlockEntityRenderer<GuhWheelBlockEntity> {
    public static final ModelResourceLocation RING_MODEL = ModelResourceLocation.standalone(Guhs.id("block/guh_wheel_ring"));
    public static final ModelResourceLocation STAND_MODEL = ModelResourceLocation.standalone(Guhs.id("block/guh_wheel_stand"));
    /** How much bigger than the item model the placed wheel is (about 3 blocks wide and tall). */
    public static final float SIZE = 2.0f;
    /** Centre of the ring in model pixels (see guh_wheel_ring.json). */
    private static final float HUB_Y = 14f / 16f;
    /** Inside bottom of the ring, where the guh's feet go. */
    private static final float FLOOR_Y = 5.75f / 16f;
    /** How long (in blocks) the guh should look inside the wheel, whatever its real size. */
    private static final float GUH_LENGTH_IN_WHEEL = 0.75f;
    private static final float GUH_MODEL_LENGTH = 23f / 16f;

    private final EntityRenderDispatcher entityRenderer;

    public GuhWheelRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(GuhWheelBlockEntity wheel, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Direction facing = wheel.getBlockState().getValue(GuhWheelBlock.FACING);
        float spin = wheel.getSpin(partialTick);

        poseStack.pushPose();
        // rotate the whole thing like a block model (models face north by default), and make it big
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180f));
        poseStack.scale(SIZE, SIZE, SIZE);
        poseStack.translate(-0.5, 0, -0.5);
        BakedModel stand = Minecraft.getInstance().getModelManager().getModel(STAND_MODEL);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                buffer.getBuffer(RenderType.cutout()), wheel.getBlockState(), stand, 1f, 1f, 1f, packedLight, packedOverlay);

        // spinning ring around its hub (the wheel turns around the model's Z axis)
        poseStack.pushPose();
        poseStack.translate(0.5, HUB_Y, 0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        poseStack.translate(-0.5, -HUB_Y, -0.5);
        BakedModel ring = Minecraft.getInstance().getModelManager().getModel(RING_MODEL);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                buffer.getBuffer(RenderType.cutout()), wheel.getBlockState(), ring, 1f, 1f, 1f, packedLight, packedOverlay);
        poseStack.popPose();

        // the guh, running "forwards" along the ring, shrunk (or grown) to fit
        GuhEntity guh = wheel.getDisplayGuh();
        if (guh != null) {
            float fit = GUH_LENGTH_IN_WHEEL / (GUH_MODEL_LENGTH * guh.getScale() * guh.getAgeScale());
            poseStack.pushPose();
            poseStack.translate(0.5, FLOOR_Y, 0.5);
            poseStack.scale(fit, fit, fit);
            guh.setYRot(90f);
            guh.yBodyRot = guh.yBodyRotO = guh.yHeadRot = guh.yHeadRotO = 90f;
            guh.setXRot(0);
            entityRenderer.setRenderShadow(false);
            entityRenderer.render(guh, 0, 0, 0, 90f, partialTick, poseStack, buffer, packedLight);
            entityRenderer.setRenderShadow(Minecraft.getInstance().options.entityShadows().get());
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(GuhWheelBlockEntity wheel) {
        return true; // the ring sticks out above the block
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(GuhWheelBlockEntity wheel) {
        return new net.minecraft.world.phys.AABB(wheel.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 3, 0);
    }
}
