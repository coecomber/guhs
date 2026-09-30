package nl.juiced.guhs.feature.huisje.client;

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
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeBlockEntity;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;

/**
 * Draws a Guhhuisje: the guh-head model of its size (models/block/guhhuisje_&lt;maat&gt;_model.json, 32 model pixels
 * wide, made by tools/features/huisje.py) scaled up to the whole footprint (2, 3 or 4 blocks), turned so the snoet (the
 * door) faces the way the huisje was placed.
 */
public class HuisjeRenderer implements BlockEntityRenderer<HuisjeBlockEntity> {
    public static ModelResourceLocation model(HuisjeMaat maat) {
        return ModelResourceLocation.standalone(Guhs.id("block/guhhuisje_" + maat.id() + "_model"));
    }

    public HuisjeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HuisjeBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(be.getBlockState().getBlock() instanceof HuisjeBlock blok)) {
            return;
        }
        HuisjeMaat maat = blok.maat();
        Direction facing = be.getBlockState().getValue(HuisjeBlock.FACING);
        Vec3 m = Huisje.midden(be.getBlockPos(), facing, maat);
        pose.pushPose();
        pose.translate(m.x - be.getBlockPos().getX(), 0, m.z - be.getBlockPos().getZ());
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180f));
        pose.scale(maat.schaal(), maat.schaal(), maat.schaal());
        pose.translate(-0.5, 0, -0.5);
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(model(maat));
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.cutout()),
                be.getBlockState(), model, 1f, 1f, 1f, packedLight, packedOverlay);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(HuisjeBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(HuisjeBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(4, 0, 4).expandTowards(0, 5, 0);
    }
}
