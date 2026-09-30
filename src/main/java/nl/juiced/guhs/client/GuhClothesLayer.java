package nl.juiced.guhs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Draws the clothes a guh wears (from the Guh kleermaker): for each worn piece, the model again with only that piece's
 * bones visible and the piece's own texture, so clothes look the same on every guh variant.
 */
public class GuhClothesLayer extends GeoRenderLayer<GuhEntity> {
    public GuhClothesLayer(GeoRenderer<GuhEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, GuhEntity guh, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource,
                       VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!guh.isWearingClothes()) {
            return;
        }
        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
            GuhClothes worn = guh.getClothes(slot);
            if (worn == null) {
                continue;
            }
            // (2.8, kapper: the high part of a hairstyle, bones "*_kruin", hides under a hat)
            boolean kruin = slot != GuhClothes.Slot.HAAR || guh.getClothes(GuhClothes.Slot.HEAD) == null;
            for (GeoBone bone : model.topLevelBones()) {
                onlyShow(bone, worn, kruin);
            }
            RenderType type = RenderType.entityCutoutNoCull(worn.texture());
            // (2.8: hair is tinted with the guh's hair colour, -1 = its own colours)
            int colour = slot == GuhClothes.Slot.HAAR && guh.getHaarkleur() >= 0 ? 0xFF000000 | guh.getHaarkleur() : 0xFFFFFFFF;
            getRenderer().reRender(model, poseStack, bufferSource, guh, type, bufferSource.getBuffer(type), partialTick, packedLight,
                    OverlayTexture.NO_OVERLAY, colour);
        }
        // back to normal for the next frame's main pass
        for (GeoBone bone : model.topLevelBones()) {
            showAll(bone);
        }
        GuhRenderer.applyVisibility(model, guh);
    }

    private static void onlyShow(GeoBone bone, GuhClothes worn, boolean kruin) {
        bone.setHidden(!worn.shows(bone.getName()) || !kruin && bone.getName().endsWith("_kruin"));
        bone.setChildrenHidden(false);
        for (GeoBone child : bone.getChildBones()) {
            onlyShow(child, worn, kruin);
        }
    }

    private static void showAll(GeoBone bone) {
        bone.setHidden(false);
        bone.setChildrenHidden(false);
        for (GeoBone child : bone.getChildBones()) {
            showAll(child);
        }
    }
}
