package nl.juiced.guhs.feature.smul.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import nl.juiced.guhs.feature.smul.SmulHapje;

/** Falling food: its item, big and slowly spinning (the golden smulknabbel glows in the dark). */
public class SmulHapjeRenderer extends EntityRenderer<SmulHapje> {
    private final ItemRenderer items;

    public SmulHapjeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
        this.shadowRadius = 0.3f;
    }

    @Override
    public void render(SmulHapje hapje, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float spin = (hapje.tickCount + partialTick) * 5f + hapje.getId() * 37f;
        pose.translate(0, 0.35 + (hapje.hasLanded() ? 0 : Mth.sin(spin * 0.05f) * 0.05f), 0);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.scale(1.7f, 1.7f, 1.7f);
        int l = hapje.soort() == SmulHapje.Soort.GOUD ? LightTexture.FULL_BRIGHT : light;
        items.renderStatic(hapje.stack(), ItemDisplayContext.GROUND, l, OverlayTexture.NO_OVERLAY, pose, buffers, hapje.level(), hapje.getId());
        pose.popPose();
        super.render(hapje, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SmulHapje hapje) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
