package nl.juiced.guhs.feature.baltoslee.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.baltoslee.RitRoute;
import nl.juiced.guhs.feature.baltoslee.SledehondjeEntity;
import nl.juiced.guhs.feature.baltoslee.SleeBaan;
import nl.juiced.guhs.feature.baltoslee.SleeEntity;
import nl.juiced.guhs.feature.baltoslee.SleeRijden;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * The Nomguh sled (geckolib/models/entity/baltoslee_slee.geo.json): turned along its track, tilted on slopes, leaning into
 * the bends, the medicine chest on board after the berghut; in front the team ({@link Span}): four guh-sledehondjes two by
 * two and the lead (Baltoguh on the medicine ride, a lead dog with a bell in the race), roped to the sled. Steele-Mika's sled
 * is darker, pulled by his own dogs, with Steele-Mika sitting in it.
 * <p>
 * 1.1.0 (GeckoLib 5 / MC 26.1): turn, look and team are worked out at extract time ({@link #addRenderData}); the team is
 * submitted after the sled like vanilla's passengers ({@link Span#submit}).
 */
public class SleeRenderer extends GeoEntityRenderer<SleeEntity, EntityRenderState> {
    private static final Identifier TEX = Guhs.id("textures/entity/baltoslee_slee.png");
    private static final Identifier TEX_STEELE = Guhs.id("textures/entity/baltoslee_slee_steele.png");
    /** Where the team runs: blocks ahead of the sled's middle, and sideways (the last one is the lead). */
    static final double[][] PLEKKEN = {{2.0, -0.4}, {2.0, 0.4}, {3.05, -0.4}, {3.05, 0.4}, {4.3, 0}};

    /** The sled's look this frame: Steele's, the chest on board, how fast (0..1, bells and lantern swing), its clock. */
    record Uiterlijk(boolean steele, boolean kist, float v, float tijd) {
    }

    /** The team this frame: members (extracted render states) and ropes (relative to the drawn sled). */
    record Team(List<Span.Lid> leden, List<Vec3[]> touwen) {
    }

    static final DataTicket<Uiterlijk> UITERLIJK = DataTicket.create("guhs_baltoslee_slee", Uiterlijk.class);
    /** Yaw, pitch, lean and the digging wobble (degrees), or absent: no turn at all (no route yet). */
    private static final DataTicket<float[]> DRAAI = DataTicket.create("guhs_baltoslee_draai", float[].class);
    static final DataTicket<Team> TEAM = DataTicket.create("guhs_baltoslee_team", new TypeToken<Team>() {});

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
        public Identifier getTextureResource(GeoRenderState state) {
            Uiterlijk u = state.getGeckolibData(UITERLIJK);
            return u != null && u.steele() ? TEX_STEELE : TEX;
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
    public void addRenderData(SleeEntity sled, @Nullable Void related, EntityRenderState state, float partialTick) {
        state.addGeckolibData(UITERLIJK, new Uiterlijk(sled.soort() == SleeEntity.STEELE, sled.kist(),
                (float) Math.min(1, sled.v / SleeRijden.KRUIS), sled.tickCount + partialTick));
        RitRoute r = sled.route();
        if (r == null) {
            return;
        }
        draai(sled, r, state, partialTick);
        state.addGeckolibData(TEAM, team(sled, r, state.lightCoords, partialTick));
    }

    /** Turned along its track, tilted on slopes, leaning into the bends (was applyRotations; eases once per frame). */
    private static void draai(SleeEntity sled, RitRoute r, EntityRenderState state, float partialTick) {
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
        float wiebel = sled.fase() == SleeEntity.VAST ? Mth.sin((sled.tickCount + partialTick) * 1.7f) * 3f : 0f;   // (digging out: it wobbles)
        state.addGeckolibData(DRAAI, new float[]{sled.tekenYaw, pitch, sled.lean, wiebel});
    }

    @Override
    protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
        float[] d = info.getGeckolibData(DRAAI);
        if (d == null) {
            return;
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - d[0]));
        poseStack.mulPose(Axis.XP.rotationDegrees(d[1]));
        poseStack.mulPose(Axis.ZP.rotationDegrees(d[2]));
        if (d[3] != 0f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(d[3]));
        }
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info, BoneSnapshots bones) {
        Uiterlijk u = info.getGeckolibData(UITERLIJK);
        if (u == null) {
            return;
        }
        bonen(bones, !u.kist(), u.v(), u.tijd());
    }

    /** The chest (hidden unless on board), no musher bone (the player stands there), bells and lantern swinging with the speed. */
    static void bonen(BoneSnapshots bones, boolean kistWeg, float v, float t) {
        bones.ifPresent("kist", b -> b.skipRender(kistWeg).skipChildrenRender(kistWeg));
        bones.ifPresent("musher", b -> b.skipRender(true).skipChildrenRender(true));
        bones.ifPresent("bellen", b -> b.setRotZ(Mth.sin(t * 0.9f) * 0.25f * v));
        bones.ifPresent("lantaarn", b -> b.setRotX(Mth.sin(t * 0.45f) * 0.12f * v));
    }

    /** The team in front (was drawn in render(); now extracted here and submitted with the sled). */
    private Team team(SleeEntity sled, RitRoute r, int light, float partialTick) {
        List<Span.Lid> leden = new ArrayList<>();
        List<Vec3[]> touwen = new ArrayList<>();
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
            touwen.add(new Vec3[]{haak.subtract(origin), harnas.subtract(origin)});
            Span.Lid lid = lead && balto ? span.leider(at, origin, dispatcher, partialTick, light)
                    : span.hond(i, at, origin, loop, legs, zit, blij, dispatcher, partialTick, light);
            if (lid != null) {
                leden.add(lid);
            }
        }
        if (steele) {
            Vec3 seat = origin.add(new Vec3(t0.x, 0, t0.z).normalize().scale(0.1)).add(0, 0.28, 0);
            Span.Lid lid = span.steele(seat, origin, sled.tekenYaw, dispatcher, partialTick, light);
            if (lid != null) {
                leden.add(lid);
            }
        }
        return new Team(leden, touwen);
    }

    @Override
    public void submit(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        Team team = state.getGeckolibData(TEAM);
        if (team != null) {
            Span.touwen(team.touwen(), state.lightCoords, pose, collector);
            Span.submit(team.leden(), dispatcher, camera, pose, collector);
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
