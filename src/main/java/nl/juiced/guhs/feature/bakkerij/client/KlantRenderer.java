package nl.juiced.guhs.feature.bakkerij.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.bakkerij.BakkerijFeature;
import nl.juiced.guhs.feature.bakkerij.BakkerijKlant;
import nl.juiced.guhs.feature.bakkerij.Recept;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * A customer guh: the guh model (without clothes or variant extras) in one of a few fur colours - the feestklant is a
 * golden guh - with its order bubble over its head: a white speech bubble with the pastry it wants and a patience bar
 * underneath (green, then yellow, then red). When you bring the wrong thing the bubble shakes.
 * <p>
 * 1.1.0 (GeckoLib 5): the fur and the bubble are copied into render-state tickets at extract time; the bubble is
 * submitted after the model.
 */
public class KlantRenderer extends GeoEntityRenderer<BakkerijKlant, LivingEntityRenderState> {
    private static final Identifier[] VACHTEN = {tex("guh"), tex("guh_mint"), tex("guh_choco"), tex("guh_snow"), tex("guh_starry"),
            tex("guh_pluisguh"), tex("guh_kaasmoerasguh")};
    private static final Identifier FEEST = tex("guh_golden");
    private static final Identifier BUBBEL = Guhs.id("textures/entity/bakkerij_bubbel.png");

    /** Which fur (index into VACHTEN, anything else = the golden feestklant). */
    private static final DataTicket<Integer> VACHT = DataTicket.create("guhs_bakkerij_vacht", Integer.class);
    /** The order bubble of this frame (null: no bubble). */
    private static final DataTicket<Bubbel> BUBBEL_DATA = DataTicket.create("guhs_bakkerij_bubbel", Bubbel.class);

    /** What the bubble shows: height above the feet, shake angle (degrees), patience, the pastry's sprite. */
    private record Bubbel(float hoogte, float schud, float geduld, @Nullable TextureAtlasSprite sprite) {
    }

    private static final net.minecraft.util.RandomSource RANDOM = net.minecraft.util.RandomSource.create();

    private static Identifier tex(String name) {
        return Guhs.id("textures/entity/" + name + ".png");
    }

    public KlantRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<BakkerijKlant>(Guhs.id("guh")) {
            @Override
            public void addAdditionalStateData(BakkerijKlant klant, @Nullable Object related, GeoRenderState state) {
                state.addGeckolibData(VACHT, klant.uiterlijk());
            }

            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                int i = state.getOrDefaultGeckolibData(VACHT, 0);
                return i >= 0 && i < VACHTEN.length ? VACHTEN[i] : FEEST;
            }
        });
        this.shadowRadius = 0.35f;
    }

    @Override
    public void addRenderData(BakkerijKlant klant, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
        Recept recept = klant.recept();
        if (recept == null || !klant.bezet()) {
            return;
        }
        float bob = Mth.sin((klant.tickCount + partialTick) * 0.12f) * 0.04f;
        float schud = klant.neeTimer() > 0 ? Mth.sin((klant.neeTimer() - partialTick) * 1.4f) * 14f : 0f;
        // the pastry's sprite (was the item model's particle icon)
        ItemStackRenderState item = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(item, new ItemStack(BakkerijFeature.bakje(recept)), ItemDisplayContext.GUI,
                klant.level(), klant, klant.getId());
        Material.Baked material = item.pickParticleMaterial(RANDOM);
        state.addGeckolibData(BUBBEL_DATA, new Bubbel(klant.getBbHeight() + 0.7f + bob, schud, klant.geduld(), material == null ? null : material.sprite()));
    }

    /** Head follows the look (was DefaultedEntityGeoModel(id, true)); no clothes, saddle, armour or variant extras on a customer. */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        for (GeoBone bone : info.model().boneLookup().get().values()) {
            String name = bone.name();
            if (name.equals("saddle") || name.startsWith("armor_") || GuhVariant.VARIANT_BONES.stream().anyMatch(name::startsWith)) {
                bones.get(bone).skipRender(true).skipChildrenRender(true);
            }
        }
    }

    @Override
    public void submit(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        Bubbel b = state.getGeckolibData(BUBBEL_DATA);
        if (b == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0, b.hoogte(), 0);
        pose.mulPose(camera.orientation);
        if (b.schud() != 0f) {
            pose.mulPose(Axis.ZP.rotationDegrees(b.schud()));
        }
        int light = LightCoordsUtil.FULL_BRIGHT;
        float g = b.geduld();
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(BUBBEL), (last, bubbel) -> {
            // the bubble: 24 x 24 of the 32 x 32 texture, its little tail pointing down to the guh
            quad(bubbel, last, -0.36f, -0.3f, 0.36f, 0.42f, 0, 0, 24 / 32f, 24 / 32f, 0xFFFFFFFF, light, 0f);
            // the patience bar under the pastry (the white corner of the texture, tinted)
            int kleur = g > 0.5f ? 0xFF6CD66C : g > 0.25f ? 0xFFF2C94C : 0xFFE8555A;
            float u = 28 / 32f, v = 28 / 32f;
            quad(bubbel, last, -0.25f, -0.16f, 0.25f, -0.11f, u, v, u + 1 / 32f, v + 1 / 32f, 0xFF503040, light, 0.01f);
            quad(bubbel, last, -0.24f, -0.155f, -0.24f + 0.48f * g, -0.115f, u, v, u + 1 / 32f, v + 1 / 32f, kleur, light, 0.02f);
        });
        // the pastry itself: its item sprite, flat in the bubble
        TextureAtlasSprite sprite = b.sprite();
        if (sprite != null) {
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(sprite.atlasLocation()), (last, item) ->
                    quad(item, last, -0.2f, -0.08f, 0.2f, 0.32f, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), 0xFFFFFFFF, light, 0.03f));
        }
        pose.popPose();
    }

    /** A flat quad facing the camera (x right, y up), both sides. */
    private static void quad(VertexConsumer vc, PoseStack.Pose last, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1,
                             int argb, int light, float z) {
        vc.addVertex(last, x0, y1, z).setColor(argb).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
        vc.addVertex(last, x0, y0, z).setColor(argb).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
        vc.addVertex(last, x1, y0, z).setColor(argb).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
        vc.addVertex(last, x1, y1, z).setColor(argb).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
    }
}
