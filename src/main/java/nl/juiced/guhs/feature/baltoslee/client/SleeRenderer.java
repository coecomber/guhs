package nl.juiced.guhs.feature.baltoslee.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.baltoslee.RitRoute;
import nl.juiced.guhs.feature.baltoslee.SledehondjeEntity;
import nl.juiced.guhs.feature.baltoslee.SleeBaan;
import nl.juiced.guhs.feature.baltoslee.SleeEntity;
import nl.juiced.guhs.feature.baltoslee.SleeRijden;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

/**
 * The Nomguh sled (geo/entity/baltoslee_slee.geo.json): turned along its track, tilted on slopes, leaning into the bends,
 * the medicine chest on board after the berghut; in front the team ({@link Span}): four guh-sledehondjes two by two and the
 * lead (Baltoguh on the medicine ride, a lead dog with a bell in the race), roped to the sled. Steele-Mika's sled is darker,
 * pulled by his own dogs, with Steele-Mika sitting in it.
 */
public class SleeRenderer extends GeoEntityRenderer<SleeEntity> {
    private static final Identifier TEX = Guhs.id("textures/entity/baltoslee_slee.png");
    private static final Identifier TEX_STEELE = Guhs.id("textures/entity/baltoslee_slee_steele.png");
    /** Where the team runs: blocks ahead of the sled's middle, and sideways (the last one is the lead). */
    static final double[][] PLEKKEN = {{2.0, -0.4}, {2.0, 0.4}, {3.05, -0.4}, {3.05, 0.4}, {4.3, 0}};

    private final EntityRenderDispatcher dispatcher;

    public SleeRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.7f;
        this.dispatcher = context.getEntityRenderDispatcher();
    }

    static final class Model extends DefaultedEntityGeoModel<SleeEntity> {
        Model() {
            super(Guhs.id("baltoslee_slee"));
        }

        @Override
        public Identifier getTextureResource(SleeEntity sled) {
            return sled.soort() == SleeEntity.STEELE ? TEX_STEELE : TEX;
        }

        @Override
        public void setCustomAnimations(SleeEntity sled, long instanceId, AnimationTest<SleeEntity> state) {
            getBone("kist").ifPresent(b -> b.setHidden(!sled.kist()));
            getBone("musher").ifPresent(b -> b.setHidden(true));
            float t = (float) state.getAnimationTick();
            float v = (float) Math.min(1, sled.v / SleeRijden.KRUIS);
            getBone("bellen").ifPresent(b -> b.setRotZ(Mth.sin(t * 0.9f) * 0.25f * v));
            getBone("lantaarn").ifPresent(b -> b.setRotX(Mth.sin(t * 0.45f) * 0.12f * v));
        }
    }

    /** The drawn s (between two ticks; a new leg starts fresh). */
    static double tekenS(SleeEntity sled, float pt) {
        return sled.been != sled.beenO ? sled.s : Mth.lerp(pt, sled.sO, sled.s);
    }

    static double tekenLat(SleeEntity sled, float pt) {
        return sled.been != sled.beenO ? sled.lat : Mth.lerp(pt, sled.latO, sled.lat);
    }

    @Override
    protected void applyRotations(SleeEntity sled, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        RitRoute r = sled.route();
        if (r == null) {
            return;
        }
        SleeBaan b = r.baan(sled.been);
        double s = tekenS(sled, partialTick);
        Vec3 t = b.richting(s);
        float doelYaw = (float) Math.toDegrees(Math.atan2(-t.x, t.z));
        sled.tekenYaw = Float.isNaN(sled.tekenYaw) ? doelYaw : Mth.rotLerp(0.25f, sled.tekenYaw, doelYaw);
        float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(t.y, -1, 1)));
        float doelLean = (float) Mth.clamp(-b.bocht(s) * sled.v * 900, -14, 14);
        if (r.zone(sled.been, RitRoute.Soort.IJSBRUG, s) != null) {
            doelLean += (float) Mth.clamp(sled.latV * 60, -8, 8);
        }
        sled.lean = Mth.lerp(0.1f, sled.lean, doelLean);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - sled.tekenYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sled.lean));
        if (sled.fase() == SleeEntity.VAST) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin((sled.tickCount + partialTick) * 1.7f) * 3f));   // (digging out: it wobbles)
        }
    }

    @Override
    public void render(SleeEntity sled, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(sled, entityYaw, partialTick, pose, buffers, light);
        RitRoute r = sled.route();
        if (r == null) {
            return;
        }
        boolean steele = sled.soort() == SleeEntity.STEELE;
        boolean balto = sled.soort() == SleeEntity.TOCHT;
        Span span = Span.van(sled, balto ? 4 : 5, steele ? SledehondjeEntity.STEELE : SledehondjeEntity.GEWOON, balto, steele);
        int fase = sled.fase();
        span.tik(sled.level().getGameTime(), (float) sled.v);
        Vec3 origin = new Vec3(Mth.lerp(partialTick, sled.xo, sled.getX()), Mth.lerp(partialTick, sled.yo, sled.getY()), Mth.lerp(partialTick, sled.zo, sled.getZ()));
        SleeBaan b = r.baan(sled.been);
        double s = tekenS(sled, partialTick), lat = tekenLat(sled, partialTick);
        float zit = fase == SleeEntity.WACHT || fase == SleeEntity.PAUZE || fase == SleeEntity.KLAAR || sled.v < 0.02 && fase != SleeEntity.VAST ? 1f : 0f;
        float loop = fase == SleeEntity.VAST ? 0.8f : (float) Math.min(1, sled.v / SleeRijden.KRUIS);
        float legs = (float) (s * 2.2 + sled.been * 50) + (fase == SleeEntity.VAST ? (sled.tickCount + partialTick) * 0.9f : 0);
        boolean blij = fase == SleeEntity.PAUZE && sled.pauze() == SleeEntity.RUST;
        Vec3 t0 = b.richting(s);
        Vec3 haak = origin.add(new Vec3(t0.x, 0, t0.z).normalize().scale(1.15)).add(0, 0.42, 0);
        for (int i = 0; i < PLEKKEN.length; i++) {
            boolean lead = i == PLEKKEN.length - 1;
            Vec3 doel = spot(b, s + PLEKKEN[i][0], lat * 0.6 + PLEKKEN[i][1]);
            Vec3 dir = b.richting(Math.min(b.lengte, s + PLEKKEN[i][0]));
            float doelYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
            if (lead && zit > 0.5f) {
                doelYaw += 25;                                  // (Baltoguh looks back at you while he waits)
            }
            Vec3 at = span.naar(i, doel, doelYaw);
            Vec3 harnas = at.add(0, 0.34, 0);
            Span.touw(haak, harnas, origin, pose, buffers, light);
            if (lead) {
                if (balto) {
                    span.leider(at, origin, dispatcher, partialTick, pose, buffers, light);
                } else {
                    span.hond(i, at, origin, loop, legs, zit, blij, dispatcher, partialTick, pose, buffers, light);
                }
            } else {
                span.hond(i, at, origin, loop, legs, zit, blij, dispatcher, partialTick, pose, buffers, light);
            }
        }
        if (steele) {
            Vec3 seat = origin.add(new Vec3(t0.x, 0, t0.z).normalize().scale(0.1)).add(0, 0.28, 0);
            span.steele(seat, origin, sled.tekenYaw, dispatcher, partialTick, pose, buffers, light);
        }
    }

    /** The spot s blocks along the track (past its end: straight on in its last direction). */
    static Vec3 spot(SleeBaan b, double s, double lat) {
        if (s <= b.lengte) {
            return b.op(s, lat);
        }
        Vec3 end = b.op(b.lengte, lat);
        Vec3 d = b.richting(b.lengte);
        return end.add(new Vec3(d.x, 0, d.z).normalize().scale(s - b.lengte));
    }
}
