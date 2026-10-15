package nl.juiced.guhs.feature.einde2.h2water.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.item.Items;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.h2water.E2H2WaterPayloads;

/**
 * Koh took your face (the test of chapter 2 that you failed): for a while a blank, pale plate covers the face of the player
 * (a white carpet laid upright over the front of the head: no eyes, no mouth). Everybody who got the message sees it, the
 * player too in third person. The item render state is made at extract time ({@link #extract}) as the skates of the
 * Elftocht do.
 */
public class BlancoLaag extends RenderLayer<AvatarRenderState, PlayerModel> {
    static final ContextKey<ItemStackRenderState> BLANCO = new ContextKey<>(Guhs.id("einde2_h2_water_blanco"));

    public BlancoLaag(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    /** Extract time: the player is still here. */
    static void extract(Player player, AvatarRenderState state) {
        if (player.isInvisible() || !E2H2WaterPayloads.Client.blanco(player.getUUID(), player.level().getGameTime())) {
            return;
        }
        ItemStackRenderState plaat = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(plaat, new ItemStack(Items.WHITE_CARPET), ItemDisplayContext.NONE, player);
        state.setRenderData(BLANCO, plaat);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        ItemStackRenderState plaat = state.getRenderData(BLANCO);
        if (plaat == null || plaat.isEmpty()) {
            return;
        }
        PlayerModel model = getParentModel();
        poseStack.pushPose();
        model.root().translateAndRotate(poseStack);
        model.head.translateAndRotate(poseStack);
        // model space: the head cube runs from y -8 to 0 px (y down) and its face is at z -4 px
        poseStack.translate(0.0, -4.0 / 16.0, -4.25 / 16.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.scale(0.56f, 0.56f, 0.56f);
        poseStack.translate(-0.5, -1.0 / 32.0, -0.5);
        plaat.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
