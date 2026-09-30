package nl.juiced.guhs.feature.katapult.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.katapult.KatapultBrokjeEntity;

/** A tumbling piece of a Mika fort: its block, turning over as it flies (each piece its own way). */
public class BrokjeRenderer extends EntityRenderer<KatapultBrokjeEntity> {

    public BrokjeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(KatapultBrokjeEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        BlockState state = entity.getBlockState();
        if (state.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        pose.pushPose();
        pose.translate(0, 0.5, 0);
        double speed = entity.getDeltaMovement().length();
        float age = entity.tickCount + partialTick;
        if (speed > 0.05) {
            int seed = entity.getId();
            pose.mulPose(Axis.XP.rotationDegrees(age * (8 + seed % 7)));
            pose.mulPose(Axis.ZP.rotationDegrees(age * (5 + seed % 5) * (seed % 2 == 0 ? 1 : -1)));
        }
        pose.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(KatapultBrokjeEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
