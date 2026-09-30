package nl.juiced.guhs.feature.guhpolder.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpolder.MolentjeBlock;
import nl.juiced.guhs.feature.guhpolder.MolentjeBlockEntity;

/**
 * The guh-molentje's sails (models/block/guh_molentje_wieken.json), turning around the hub in front of the cap. The
 * little mill itself (with its guh face and ears) is the normal block model.
 */
public class MolentjeRenderer implements BlockEntityRenderer<MolentjeBlockEntity> {
    public static final ModelResourceLocation WIEKEN = ModelResourceLocation.standalone(Guhs.id("block/guh_molentje_wieken"));
    /** The hub, in model pixels (see guh_molentje_wieken.json). */
    public static final float HUB_Y = 10f / 16f, HUB_Z = 2.5f / 16f;

    public MolentjeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MolentjeBlockEntity molen, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Direction facing = molen.getBlockState().getValue(MolentjeBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180f));
        pose.translate(-0.5, 0, -0.5);
        pose.translate(0.5, HUB_Y, HUB_Z);
        pose.mulPose(Axis.ZP.rotationDegrees(molen.hoek(partialTick)));
        pose.translate(-0.5, -HUB_Y, -HUB_Z);
        BakedModel wieken = Minecraft.getInstance().getModelManager().getModel(WIEKEN);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.cutout()),
                molen.getBlockState(), wieken, 1f, 1f, 1f, light, overlay);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MolentjeBlockEntity molen) {
        return new AABB(molen.getBlockPos()).inflate(0.5);
    }
}
