package nl.juiced.guhs.feature.guheinde.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guheinde.HongerigeEnderguhEntity;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

/**
 * The starved Enderguh: the guh model with only its ender bones (wings, horns, spikes), textured
 * textures/entity/guh_hongerig_&lt;vahoeg&gt;.png: grey at 0, back to all its Vahoege Enderguh colours at the top.
 */
public class HongerigeEnderguhRenderer extends GeoEntityRenderer<HongerigeEnderguhEntity> {
    private static final Identifier[] TEXTURES = new Identifier[HongerigeEnderguhEntity.MAX_VAHOEG + 1];

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = Guhs.id("textures/entity/guh_hongerig_" + i + ".png");
        }
    }

    public HongerigeEnderguhRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<HongerigeEnderguhEntity>(Guhs.id("guh"), true) {
            @Override
            public Identifier getTextureResource(HongerigeEnderguhEntity guh) {
                return TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, guh.getVahoeg()))];
            }

            @Override
            public Identifier getAnimationResource(HongerigeEnderguhEntity guh) {
                return Guhs.id("animations/entity/guh.animation.json");
            }
        });
        this.shadowRadius = 0.45f * HongerigeEnderguhEntity.SCALE;
    }

    @Override
    public void preRender(PoseStack poseStack, HongerigeEnderguhEntity guh, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        for (GeoBone bone : model.topLevelBones()) {
            showBones(bone);
        }
        super.preRender(poseStack, guh, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    /** Only the ender bones of the variant bones; no saddle, no armour. */
    private static void showBones(GeoBone bone) {
        String name = bone.getName();
        if (name.equals("saddle") || name.startsWith("armor_")) {
            bone.setHidden(true);
        } else if (GuhVariant.VARIANT_BONES.stream().anyMatch(name::startsWith)) {
            bone.setHidden(!GuhVariant.ENDER.shows(name));
            bone.setChildrenHidden(false);
        }
        for (GeoBone child : bone.getChildBones()) {
            showBones(child);
        }
    }
}
