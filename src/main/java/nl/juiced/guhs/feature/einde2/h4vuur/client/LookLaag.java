package nl.juiced.guhs.feature.einde2.h4vuur.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.einde2.h4vuur.Look;

/**
 * einde2 phase B, slice h4_vuur: the look of a player in chapter 4 ({@link Look}): the Vuurnatie disguise drawn over the skin
 * (a red tunic with a golden collar, a dark sash, dark trousers and boots, a little topknot with a golden clip; the face
 * stays the player's own), or the glowing eyes of the Guhvatar state. Never an item and never guh clothes: it goes when the
 * step goes. Textures: tools/features/einde2_h4_vuur.py.
 */
public class LookLaag extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final Identifier VERMOMMING = Guhs.id("textures/entity/einde2_h4_vuur_vermomming.png");
    public static final Identifier OGEN = Guhs.id("textures/entity/einde2_h4_vuur_ogen.png");

    public LookLaag(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        Byte look = Look.CLIENT.get(state.id);
        if (look == null || state.isInvisible) {
            return;
        }
        if (look == Look.VERMOMD) {
            coloredCutoutModelCopyLayerRender(getParentModel(), VERMOMMING, poseStack, collector, light, state, -1, 1);
        } else if (look == Look.GLOED) {
            collector.order(1).submitModel(getParentModel(), state, poseStack, GuhRenderTypes.eyes(OGEN), light,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F), -1, null, state.outlineColor, null);
        }
    }
}
