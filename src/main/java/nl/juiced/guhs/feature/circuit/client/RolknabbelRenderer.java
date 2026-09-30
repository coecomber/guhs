package nl.juiced.guhs.feature.circuit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.juiced.guhs.feature.circuit.RolknabbelEntity;
import nl.juiced.guhs.registry.ModBlocks;

/** A rolling kaasknabbel: a big block of kaasknabbels that turns over as it rolls down the Knabbelhelling. */
public class RolknabbelRenderer extends EntityRenderer<RolknabbelEntity> {
    public RolknabbelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.55f;
    }

    @Override
    public void render(RolknabbelEntity knabbel, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        pose.translate(0, 0.6, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTick, knabbel.yRotO, knabbel.getYRot())));
        pose.mulPose(Axis.XP.rotation(Mth.lerp(partialTick, knabbel.rolO, knabbel.rol)));
        pose.scale(1.15f, 1.15f, 1.15f);
        pose.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(ModBlocks.BLOCK_OF_KAASKNABBELS.get().defaultBlockState(), pose, buffers,
                packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(knabbel, entityYaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(RolknabbelEntity knabbel) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
