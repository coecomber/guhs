package nl.juiced.guhs.feature.speelgoed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.speelgoed.SchommelBlock;
import nl.juiced.guhs.feature.speelgoed.ToestelBlock;
import nl.juiced.guhs.feature.speelgoed.ToestelBlockEntity;
import nl.juiced.guhs.feature.speelgoed.WipBlock;

/**
 * The moving part of a wip (the plank, model guh_wip_plank) or a schommel (the seat on its ropes, model
 * guh_schommel_zitje): drawn turned around its pivot by the swing angle of the {@link ToestelBlockEntity} (the same
 * angle the seats use, so riders stay on it). The models are in the toy's local pixels, facing north.
 */
public class ToestelRenderer implements BlockEntityRenderer<ToestelBlockEntity> {
    public static final ModelResourceLocation PLANK = ModelResourceLocation.standalone(Guhs.id("block/guh_wip_plank"));
    public static final ModelResourceLocation ZITJE = ModelResourceLocation.standalone(Guhs.id("block/guh_schommel_zitje"));

    public ToestelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ToestelBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        ModelResourceLocation model;
        Vec3 spil;
        if (state.getBlock() instanceof WipBlock) {
            model = PLANK;
            spil = WipBlock.SPIL;
        } else if (state.getBlock() instanceof SchommelBlock) {
            model = ZITJE;
            spil = SchommelBlock.SPIL;
        } else {
            return;
        }
        float tijd = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partialTick;
        float hoek = be.hoek(tijd);
        Direction facing = state.getValue(ToestelBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
        pose.translate(-0.5, 0, -0.5);
        pose.translate(spil.x, spil.y, spil.z);
        pose.mulPose(Axis.XP.rotation(-hoek));
        pose.translate(-spil.x, -spil.y, -spil.z);
        BakedModel baked = Minecraft.getInstance().getModelManager().getModel(model);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.cutout()),
                state, baked, 1f, 1f, 1f, packedLight, packedOverlay);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ToestelBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ToestelBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 2.5, 0);
    }
}
