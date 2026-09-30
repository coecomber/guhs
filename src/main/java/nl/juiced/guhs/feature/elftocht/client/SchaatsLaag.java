package nl.juiced.guhs.feature.elftocht.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.elftocht.ElftochtFeature;
import nl.juiced.guhs.feature.elftocht.ElftochtSchaatsen;

/**
 * The guh-schaatsen on your feet: while you hold them, a skate (pink boot, shiny blade with a curl at the front) is
 * drawn under each leg, moving with it. The 3D skate is the item's "head" view (models/item/guh_schaatsen.json, a
 * neoforge:separate_transforms model: flat in the hand and in the inventory, a real skate here).
 */
public class SchaatsLaag extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public SchaatsLaag(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ElftochtSchaatsen.houdtSchaatsen(player) || player.isInvisible()) {
            return;
        }
        ItemStack schaats = new ItemStack(ElftochtFeature.SCHAATSEN.get());
        for (ModelPart leg : new ModelPart[]{getParentModel().leftLeg, getParentModel().rightLeg}) {
            poseStack.pushPose();
            leg.translateAndRotate(poseStack);
            // model space: y points down from the hip; the sole is 12 px (0.75) down. Flip to item space (y up, z forward)
            poseStack.translate(0.0, 0.75, 0.0);
            poseStack.scale(1f, -1f, -1f);
            poseStack.translate(0.0, 0.5 - 2.0 / 16.0, 0.0);
            Minecraft.getInstance().getItemRenderer().renderStatic(player, schaats, ItemDisplayContext.HEAD, false, poseStack, buffer,
                    player.level(), light, OverlayTexture.NO_OVERLAY, player.getId());
            poseStack.popPose();
        }
    }
}
