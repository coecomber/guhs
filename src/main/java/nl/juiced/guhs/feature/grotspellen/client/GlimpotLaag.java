package nl.juiced.guhs.feature.grotspellen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.grotspellen.GrotSpelItems;
import nl.juiced.guhs.feature.grotspellen.GrotSpellenFeature;

/**
 * The glimworm jar on your back while you play Glimwormvangen: a little glass jar on the back of the body, moving with
 * it. The more glimworms are in it the brighter it is (its model per stage, glimpotje_rug in items/), and from the first
 * catch on it is drawn full bright. (The item render state is made at extract time and kept in the render state.)
 */
public class GlimpotLaag extends RenderLayer<AvatarRenderState, PlayerModel> {
    static final ContextKey<ItemStackRenderState> POTJE = new ContextKey<>(Guhs.id("grotspel_potje"));
    static final ContextKey<Integer> STAND = new ContextKey<>(Guhs.id("grotspel_potje_stand"));

    public GlimpotLaag(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    /** Extract time: the player is still here. */
    static void extract(Player player, AvatarRenderState state) {
        int stand = GrotSpellenClient.stand(player);
        if (stand < 0 || player.isInvisible()) {
            return;
        }
        ItemStack stack = new ItemStack(GrotSpellenFeature.GLIMPOTJE_RUG.get());
        GrotSpelItems.zetStand(stack, stand);
        ItemStackRenderState potje = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(potje, stack, ItemDisplayContext.NONE, player);
        state.setRenderData(POTJE, potje);
        state.setRenderData(STAND, stand);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        ItemStackRenderState potje = state.getRenderData(POTJE);
        Integer stand = state.getRenderData(STAND);
        if (potje == null || potje.isEmpty() || stand == null) {
            return;
        }
        PlayerModel model = getParentModel();
        poseStack.pushPose();
        model.root().translateAndRotate(poseStack);
        model.body.translateAndRotate(poseStack);
        // model space: y points down from the neck, the back is +z; the jar sits between the shoulder blades
        poseStack.translate(0.0, 0.42, 0.24);
        poseStack.scale(0.5f, -0.5f, -0.5f);
        potje.submit(poseStack, collector, stand > 0 ? LightCoordsUtil.FULL_BRIGHT : light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
