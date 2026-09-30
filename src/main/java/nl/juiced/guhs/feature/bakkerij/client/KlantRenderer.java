package nl.juiced.guhs.feature.bakkerij.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.bakkerij.BakkerijFeature;
import nl.juiced.guhs.feature.bakkerij.BakkerijKlant;
import nl.juiced.guhs.feature.bakkerij.Recept;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

import net.minecraft.client.renderer.rendertype.RenderTypes;
/**
 * A customer guh: the guh model (without clothes or variant extras) in one of a few fur colours - the feestklant is a
 * golden guh - with its order bubble over its head: a white speech bubble with the pastry it wants and a patience bar
 * underneath (green, then yellow, then red). When you bring the wrong thing the bubble shakes.
 */
public class KlantRenderer extends GeoEntityRenderer<BakkerijKlant> {
    private static final Identifier[] VACHTEN = {tex("guh"), tex("guh_mint"), tex("guh_choco"), tex("guh_snow"), tex("guh_starry"),
            tex("guh_pluisguh"), tex("guh_kaasmoerasguh")};
    private static final Identifier FEEST = tex("guh_golden");
    private static final Identifier BUBBEL = Guhs.id("textures/entity/bakkerij_bubbel.png");

    private static Identifier tex(String name) {
        return Guhs.id("textures/entity/" + name + ".png");
    }

    public KlantRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<BakkerijKlant>(Guhs.id("guh"), true) {
            @Override
            public Identifier getTextureResource(BakkerijKlant klant) {
                int i = klant.uiterlijk();
                return i >= 0 && i < VACHTEN.length ? VACHTEN[i] : FEEST;
            }
        });
        this.shadowRadius = 0.35f;
    }

    @Override
    public void preRender(PoseStack poseStack, BakkerijKlant klant, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        for (GeoBone bone : model.topLevelBones()) {
            verberg(bone);
        }
        super.preRender(poseStack, klant, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    /** No clothes, saddle, armour or variant extras on a customer. */
    private static void verberg(GeoBone bone) {
        String name = bone.getName();
        if (name.equals("saddle") || name.startsWith("armor_") || GuhVariant.VARIANT_BONES.stream().anyMatch(name::startsWith)) {
            bone.setHidden(true);
        }
        for (GeoBone child : bone.getChildBones()) {
            verberg(child);
        }
    }

    @Override
    public void render(BakkerijKlant klant, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        super.render(klant, entityYaw, partialTick, pose, buffers, packedLight);
        Recept recept = klant.recept();
        if (recept == null || !klant.bezet()) {
            return;
        }
        pose.pushPose();
        float bob = Mth.sin((klant.tickCount + partialTick) * 0.12f) * 0.04f;
        pose.translate(0, klant.getBbHeight() + 0.7 + bob, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        if (klant.neeTimer() > 0) {
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin((klant.neeTimer() - partialTick) * 1.4f) * 14f));
        }
        int light = LightCoordsUtil.FULL_BRIGHT;
        VertexConsumer bubbel = buffers.getBuffer(RenderTypes.entityTranslucent(BUBBEL));
        // the bubble: 24 x 24 of the 32 x 32 texture, its little tail pointing down to the guh
        quad(bubbel, pose, -0.36f, -0.3f, 0.36f, 0.42f, 0, 0, 24 / 32f, 24 / 32f, 0xFFFFFFFF, light, 0f);
        // the patience bar under the pastry (the white corner of the texture, tinted)
        float g = klant.geduld();
        int kleur = g > 0.5f ? 0xFF6CD66C : g > 0.25f ? 0xFFF2C94C : 0xFFE8555A;
        float u = 28 / 32f, v = 28 / 32f;
        quad(bubbel, pose, -0.25f, -0.16f, 0.25f, -0.11f, u, v, u + 1 / 32f, v + 1 / 32f, 0xFF503040, light, 0.01f);
        quad(bubbel, pose, -0.24f, -0.155f, -0.24f + 0.48f * g, -0.115f, u, v, u + 1 / 32f, v + 1 / 32f, kleur, light, 0.02f);
        // the pastry itself: its item sprite, flat in the bubble
        ItemStack stack = new ItemStack(BakkerijFeature.bakje(recept));
        TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer().getModel(stack, klant.level(), null, 0).getParticleIcon();
        VertexConsumer item = buffers.getBuffer(RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        quad(item, pose, -0.2f, -0.08f, 0.2f, 0.32f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), 0xFFFFFFFF, light, 0.03f);
        pose.popPose();
    }

    /** A flat quad facing the camera (x right, y up), both sides. */
    private static void quad(VertexConsumer vc, PoseStack pose, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1,
                             int argb, int light, float z) {
        PoseStack.Pose last = pose.last();
        vc.addVertex(last, x0, y1, z).setColor(argb).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
        vc.addVertex(last, x0, y0, z).setColor(argb).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
        vc.addVertex(last, x1, y0, z).setColor(argb).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
        vc.addVertex(last, x1, y1, z).setColor(argb).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
    }
}
