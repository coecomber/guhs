package nl.juiced.guhs.feature.baltoslee.client;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.baltoslee.BaltoSleeFeature;
import nl.juiced.guhs.feature.baltoslee.SledehondjeEntity;
import nl.juiced.guhs.registry.ModEntities;
import org.joml.Matrix4f;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.client.renderer.rendertype.RenderTypes;
/**
 * The team in front of a sled (client only, never in the world): the guh-sledehondjes two by two, the lead (Baltoguh on the
 * medicine ride, a lead dog with a golden bell otherwise) and the ropes from the sled to their harnesses; in Steele-Mika's
 * sled, Steele-Mika himself. Each dog is eased towards its spot, so the team swings nicely through the bends (and around the
 * berghut). One Span per sled, kept as long as the sled exists.
 */
final class Span {
    private static final Identifier TOUW = Guhs.id("textures/entity/baltoslee_touw.png");
    private static final Map<Entity, Span> SPANNEN = new WeakHashMap<>();

    final SledehondjeEntity[] honden;
    @Nullable
    GuhEntity leider;
    @Nullable
    GuhNpcEntity steele;
    final Vec3[] getekend;
    final float[] yaw;
    private long laatsteTik = -1;

    private Span(int n) {
        honden = new SledehondjeEntity[n];
        getekend = new Vec3[6];
        yaw = new float[6];
    }

    static Span van(Entity slee, int honden, int soort, boolean baltoguh, boolean steeleErin) {
        Span s = SPANNEN.get(slee);
        if (s == null || s.honden.length != honden) {
            s = new Span(honden);
            Level level = slee.level();
            for (int i = 0; i < honden; i++) {
                SledehondjeEntity d = BaltoSleeFeature.SLEDEHONDJE.get().create(level, EntitySpawnReason.TRIGGERED);
                if (d == null) {
                    return s;
                }
                d.soort = i == 4 && soort == SledehondjeEntity.GEWOON ? SledehondjeEntity.KOP : soort;   // (a fifth dog is the lead)
                s.honden[i] = d;
            }
            if (baltoguh) {
                GuhEntity g = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
                if (g != null) {
                    g.setVariant(GuhVariant.BALTOGUH);
                    g.setGuhScale(0.72f);
                    g.hideName = true;
                    s.leider = g;
                }
            }
            if (steeleErin) {
                GuhNpcEntity n = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
                if (n != null) {
                    n.setKind(GuhNpcEntity.Kind.STEELE_MIKA);
                    s.steele = n;
                }
            }
            SPANNEN.put(slee, s);
        }
        return s;
    }

    /** Once per game tick: the lead's walk (its animations run on ticks). */
    void tik(long tick, float snelheid) {
        if (tick == laatsteTik) {
            return;
        }
        laatsteTik = tick;
        if (leider != null) {
            leider.tickCount++;
            leider.walkAnimation.update(Math.min(1f, snelheid * 3f), 0.4f, 1f);
        }
        if (steele != null) {
            steele.tickCount++;
        }
    }

    /** Eases the drawn spot of team member i (the lead = 4) towards doel. */
    Vec3 naar(int i, Vec3 doel, float doelYaw) {
        Vec3 p = getekend[i];
        if (p == null || p.distanceToSqr(doel) > 36) {
            p = doel;
            yaw[i] = doelYaw;
        } else {
            p = p.lerp(doel, 0.35);
            yaw[i] = Mth.rotLerp(0.3f, yaw[i], doelYaw);
        }
        getekend[i] = p;
        return p;
    }

    /** One team member of this frame: its extracted render state and where it stands (relative to the drawn sled). */
    record Lid(EntityRenderState state, Vec3 at) {
    }

    /**
     * A dog (loop 0..1, fase = where its legs are, zit 0..1) at world spot at, relative to the drawn sled at origin.
     * 1.1.0: extracted now (render state), submitted with the sled.
     */
    @Nullable
    Lid hond(int i, Vec3 at, Vec3 origin, float loop, float fase, float zit, boolean blij, EntityRenderDispatcher dispatcher, float partialTick, int light) {
        SledehondjeEntity d = honden[i];
        if (d == null) {
            return null;
        }
        d.setYRot(yaw[i]);
        d.yRotO = yaw[i];
        d.loop = loop;
        d.fase = fase + (i % 2) * 0.35f + (i / 2) * 1.3f;
        d.zit = zit;
        d.blij = blij ? 1 : 0;
        return lid(d, at, origin, dispatcher, partialTick, light);
    }

    /** The lead (Baltoguh). */
    @Nullable
    Lid leider(Vec3 at, Vec3 origin, EntityRenderDispatcher dispatcher, float partialTick, int light) {
        if (leider == null) {
            return null;
        }
        float y = yaw[4];
        leider.setYRot(y);
        leider.yRotO = y;
        leider.yBodyRot = leider.yBodyRotO = leider.yHeadRot = leider.yHeadRotO = y;
        return lid(leider, at, origin, dispatcher, partialTick, light);
    }

    /** Steele-Mika sitting in his sled (at is his seat, world). */
    @Nullable
    Lid steele(Vec3 at, Vec3 origin, float y, EntityRenderDispatcher dispatcher, float partialTick, int light) {
        if (steele == null) {
            return null;
        }
        steele.setYRot(y);
        steele.yRotO = y;
        steele.yBodyRot = steele.yBodyRotO = steele.yHeadRot = steele.yHeadRotO = y;
        return lid(steele, at, origin, dispatcher, partialTick, light);
    }

    private static Lid lid(Entity e, Vec3 at, Vec3 origin, EntityRenderDispatcher dispatcher, float partialTick, int light) {
        EntityRenderState state = dispatcher.extractEntity(e, partialTick);
        state.lightCoords = light;                     // (like 1.21.1: drawn with the sled's light)
        state.shadowPieces.clear();                    // (it stands nowhere: no shadow of its own)
        return new Lid(state, at.subtract(origin));
    }

    /** Submits the team members of this frame. */
    static void submit(java.util.List<Lid> leden, EntityRenderDispatcher dispatcher, CameraRenderState camera, PoseStack pose, SubmitNodeCollector collector) {
        for (Lid l : leden) {
            dispatcher.submit(l.state(), camera, l.at().x, l.at().y, l.at().z, pose, collector);
        }
    }

    /** Ropes from a to b (world spots, drawn relative to origin): the segments are worked out at extract time, drawn at submit. */
    static void touwen(java.util.List<Vec3[]> touwen, int light, PoseStack pose, SubmitNodeCollector collector) {
        if (touwen.isEmpty()) {
            return;
        }
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TOUW), (last, vc) -> {
            for (Vec3[] t : touwen) {
                touw(t[0], t[1], vc, last, light);
            }
        });
    }

    /** A rope from a to b (relative to the drawn sled), sagging a little. */
    private static void touw(Vec3 a, Vec3 b, VertexConsumer vc, PoseStack.Pose last, int light) {
        Matrix4f m = last.pose();
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.05) {
            return;
        }
        Vec3 side = new Vec3(-d.z, 0, d.x);
        side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize().scale(0.035);
        Vec3 up = new Vec3(0, 0.035, 0);
        int n = 8;
        for (int k = 0; k < n; k++) {
            double f0 = (double) k / n, f1 = (double) (k + 1) / n;
            Vec3 p0 = a.add(d.scale(f0)).subtract(0, Math.sin(f0 * Math.PI) * 0.12 * Math.min(1, len / 2), 0);
            Vec3 p1 = a.add(d.scale(f1)).subtract(0, Math.sin(f1 * Math.PI) * 0.12 * Math.min(1, len / 2), 0);
            quad(vc, m, last, p0.add(side), p0.subtract(side), p1.subtract(side), p1.add(side), (float) f0, (float) f1, light);
            quad(vc, m, last, p0.add(up), p0.subtract(up), p1.subtract(up), p1.add(up), (float) f0, (float) f1, light);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, PoseStack.Pose last, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float u1, int light) {
        punt(vc, m, last, a, u0, 0, light);
        punt(vc, m, last, b, u0, 1, light);
        punt(vc, m, last, c, u1, 1, light);
        punt(vc, m, last, d, u1, 0, light);
    }

    private static void punt(VertexConsumer vc, Matrix4f m, PoseStack.Pose last, Vec3 p, float u, float v, int light) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(last, 0, 1, 0);
    }
}
