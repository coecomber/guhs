package nl.juiced.guhs.feature.elftocht.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.elftocht.ElftochtFeature;
import nl.juiced.guhs.feature.elftocht.ElftochtSchaatsen;

/**
 * The guh-schaatsen on your feet: while you hold them, a skate (pink boot, shiny blade with a curl at the front) is
 * drawn under each leg, moving with it. The 3D skate is the item's "head" view (models/item/guh_schaatsen.json, a
 * neoforge:separate_transforms model: flat in the hand and in the inventory, a real skate here).
 * (1.1.0: the skate's item render state is made at extract time, {@link #extract}, and kept in the player's render state.)
 */
public class SchaatsLaag extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** The skate to draw under the legs (only set while the player holds the skates and is visible). */
    static final ContextKey<ItemStackRenderState> SCHAATS = new ContextKey<>(Guhs.id("elftocht_schaats"));

    public SchaatsLaag(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    /** Extract time (render state modifier): the player is still here. */
    static void extract(Player player, AvatarRenderState state) {
        if (!ElftochtSchaatsen.houdtSchaatsen(player) || player.isInvisible()) {
            return;
        }
        ItemStackRenderState schaats = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(schaats, new ItemStack(ElftochtFeature.SCHAATSEN.get()),
                ItemDisplayContext.HEAD, player);
        state.setRenderData(SCHAATS, schaats);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        ItemStackRenderState schaats = state.getRenderData(SCHAATS);
        if (schaats == null || schaats.isEmpty()) {
            return;
        }
        PlayerModel model = getParentModel();
        for (ModelPart leg : new ModelPart[]{model.leftLeg, model.rightLeg}) {
            poseStack.pushPose();
            model.root().translateAndRotate(poseStack);
            leg.translateAndRotate(poseStack);
            // model space: y points down from the hip; the sole is 12 px (0.75) down. Flip to item space (y up, z forward)
            poseStack.translate(0.0, 0.75, 0.0);
            poseStack.scale(1f, -1f, -1f);
            poseStack.translate(0.0, 0.5 - 2.0 / 16.0, 0.0);
            schaats.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
