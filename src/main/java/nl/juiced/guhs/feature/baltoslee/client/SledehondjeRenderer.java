package nl.juiced.guhs.feature.baltoslee.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.baltoslee.SledehondjeEntity;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

/**
 * A guh-sledehondje (geo/entity/baltoslee_sledehondje.geo.json): its legs, ears, tail and tongue are moved by hand from its
 * {@link SledehondjeEntity#loop}, {@link SledehondjeEntity#fase}, {@link SledehondjeEntity#zit} (no animation file): a bouncy
 * little gallop, flapping ears, a wagging tail; sitting with its tongue out when the sled stands still.
 */
public class SledehondjeRenderer extends GeoEntityRenderer<SledehondjeEntity> {
    private static final Identifier[] TEXTUREN = {Guhs.id("textures/entity/baltoslee_sledehondje.png"),
            Guhs.id("textures/entity/baltoslee_sledehondje_steele.png"), Guhs.id("textures/entity/baltoslee_sledehondje.png")};

    public SledehondjeRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.22f;
    }

    @Override
    protected void applyRotations(SledehondjeEntity dog, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - dog.getYRot()));
    }

    static final class Model extends DefaultedEntityGeoModel<SledehondjeEntity> {
        Model() {
            super(Guhs.id("baltoslee_sledehondje"));
        }

        @Override
        public Identifier getTextureResource(SledehondjeEntity dog) {
            return TEXTUREN[Mth.clamp(dog.soort, 0, TEXTUREN.length - 1)];
        }

        @Override
        public void setCustomAnimations(SledehondjeEntity dog, long instanceId, AnimationTest<SledehondjeEntity> state) {
            float f = dog.fase, a = 0.95f * dog.loop, zit = dog.zit;
            float t = (float) state.getAnimationTick() + (dog.getId() * 13 % 40);
            float lopen = 1 - zit;
            zet("been_lv", Mth.sin(f) * a * lopen);
            zet("been_rv", Mth.sin(f + 0.7f) * a * lopen);
            zet("been_la", Mth.sin(f + Mth.PI) * a * lopen + zit * -1.35f);
            zet("been_ra", Mth.sin(f + Mth.PI + 0.7f) * a * lopen + zit * -1.35f);
            getBone("lijf").ifPresent(b -> {
                b.setRotX(zit * 0.45f + Mth.cos(f * 2) * 0.05f * a);
                b.setPosY(Math.abs(Mth.sin(f)) * 0.9f * a * lopen - zit * 1.2f);
            });
            getBone("kop").ifPresent(b -> {
                b.setRotX(-zit * 0.35f + Mth.sin(f * 2) * 0.06f * a);
                b.setRotZ(zit > 0.5f ? Mth.sin(t * 0.05f) * 0.12f : 0);
            });
            float oor = -0.5f * a + Mth.sin(f * 2) * 0.15f * a;
            getBone("oor_l").ifPresent(b -> b.setRotX(oor));
            getBone("oor_r").ifPresent(b -> b.setRotX(oor));
            float kwispel = dog.blij > 0 ? 0.35f : 0.18f;
            getBone("staart").ifPresent(b -> {
                b.setRotY(Mth.sin(t * kwispel) * (0.5f + 0.3f * zit));
                b.setRotX(0.3f * a);
            });
            getBone("tong").ifPresent(b -> b.setHidden(zit < 0.5f && a < 0.6f));
            getBone("belletje").ifPresent(b -> {
                b.setHidden(dog.soort != SledehondjeEntity.KOP);
                b.setRotX(Mth.sin(f * 2) * 0.4f * a);
            });
        }

        private void zet(String bot, float rotX) {
            getBone(bot).ifPresent(b -> b.setRotX(rotX));
        }
    }
}
