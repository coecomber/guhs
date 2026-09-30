package nl.juiced.guhs.feature.baltoslee.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.baltoslee.SledehondjeEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * A guh-sledehondje (geckolib/models/entity/baltoslee_sledehondje.geo.json): its legs, ears, tail and tongue are moved by
 * hand from its {@link SledehondjeEntity#loop}, {@link SledehondjeEntity#fase}, {@link SledehondjeEntity#zit} (no animation
 * file): a bouncy little gallop, flapping ears, a wagging tail; sitting with its tongue out when the sled stands still.
 * <p>
 * 1.1.0 (GeckoLib 5): the dog's values are copied into a render-state ticket at extract time ({@link Houding}); the bones
 * are moved from it in {@link #adjustModelBonesForRender}.
 */
public class SledehondjeRenderer extends GeoEntityRenderer<SledehondjeEntity, EntityRenderState> {
    private static final Identifier[] TEXTUREN = {Guhs.id("textures/entity/baltoslee_sledehondje.png"),
            Guhs.id("textures/entity/baltoslee_sledehondje_steele.png"), Guhs.id("textures/entity/baltoslee_sledehondje.png")};

    /** One frame of a dog: its look, turn, legs (fase, loop), sitting, happiness and its clock (ticks, for ears and tail). */
    private record Houding(int soort, float yaw, float fase, float loop, float zit, boolean blij, float tijd) {
    }

    private static final DataTicket<Houding> HOUDING = DataTicket.create("guhs_sledehondje", Houding.class);

    public SledehondjeRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.22f;
    }

    @Override
    public void addRenderData(SledehondjeEntity dog, @Nullable Void related, EntityRenderState state, float partialTick) {
        // (a dog is never ticked: its clock is the level's, like GeckoLib 4's render clock was)
        float tijd = dog.level().getGameTime() + partialTick + (dog.getId() * 13 % 40);
        state.addGeckolibData(HOUDING, new Houding(dog.soort, dog.getYRot(), dog.fase, dog.loop, dog.zit, dog.blij > 0, tijd));
    }

    @Override
    protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
        Houding h = info.getGeckolibData(HOUDING);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - (h == null ? 0 : h.yaw())));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info, BoneSnapshots bones) {
        Houding h = info.getGeckolibData(HOUDING);
        if (h == null) {
            return;
        }
        float f = h.fase(), a = 0.95f * h.loop(), zit = h.zit();
        float t = h.tijd();
        float lopen = 1 - zit;
        zet(bones, "been_lv", Mth.sin(f) * a * lopen);
        zet(bones, "been_rv", Mth.sin(f + 0.7f) * a * lopen);
        zet(bones, "been_la", Mth.sin(f + Mth.PI) * a * lopen + zit * -1.35f);
        zet(bones, "been_ra", Mth.sin(f + Mth.PI + 0.7f) * a * lopen + zit * -1.35f);
        bones.ifPresent("lijf", b -> {
            b.setRotX(zit * 0.45f + Mth.cos(f * 2) * 0.05f * a);
            b.setTranslateY(Math.abs(Mth.sin(f)) * 0.9f * a * lopen - zit * 1.2f);
        });
        bones.ifPresent("kop", b -> {
            b.setRotX(-zit * 0.35f + Mth.sin(f * 2) * 0.06f * a);
            b.setRotZ(zit > 0.5f ? Mth.sin(t * 0.05f) * 0.12f : 0);
        });
        float oor = -0.5f * a + Mth.sin(f * 2) * 0.15f * a;
        bones.ifPresent("oor_l", b -> b.setRotX(oor));
        bones.ifPresent("oor_r", b -> b.setRotX(oor));
        float kwispel = h.blij() ? 0.35f : 0.18f;
        bones.ifPresent("staart", b -> {
            b.setRotY(Mth.sin(t * kwispel) * (0.5f + 0.3f * zit));
            b.setRotX(0.3f * a);
        });
        boolean tongWeg = zit < 0.5f && a < 0.6f;
        bones.ifPresent("tong", b -> b.skipRender(tongWeg).skipChildrenRender(tongWeg));
        boolean belWeg = h.soort() != SledehondjeEntity.KOP;
        bones.ifPresent("belletje", b -> {
            b.skipRender(belWeg).skipChildrenRender(belWeg);
            b.setRotX(Mth.sin(f * 2) * 0.4f * a);
        });
    }

    private static void zet(BoneSnapshots bones, String bot, float rotX) {
        bones.ifPresent(bot, b -> b.setRotX(rotX));
    }

    static final class Model extends DefaultedEntityGeoModel<SledehondjeEntity> {
        Model() {
            super(Guhs.id("baltoslee_sledehondje"));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            Houding h = state.getGeckolibData(HOUDING);
            return TEXTUREN[Mth.clamp(h == null ? 0 : h.soort(), 0, TEXTUREN.length - 1)];
        }
    }
}
