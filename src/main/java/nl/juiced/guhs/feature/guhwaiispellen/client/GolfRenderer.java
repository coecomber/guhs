package nl.juiced.guhs.feature.guhwaiispellen.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhwaiispellen.SurfGolven;
import nl.juiced.guhs.feature.guhwaiispellen.SurfPlankEntity;
import nl.juiced.guhs.feature.guhwaiispellen.SurfSim;
import nl.juiced.guhs.feature.guhwaiispellen.Surfplek;
import org.joml.Matrix4f;

/**
 * The waves of a surf game, drawn over the water (3.0): every wave of the set ({@link SurfGolven}, the same on every side)
 * as a ribbon along the beach with the wave's profile: a gentle back, a steep turquoise face that gets lighter towards
 * the crest, a white foamy lip, and where it breaks a low bubbling band of whitewater with spray flying off; next to the
 * break the lip curls over (the tube). Only over water (not over the beach), faded out at both ends. Your own game uses
 * its own ride's step (smooth), the others the board's synced step.
 */
public final class GolfRenderer {
    public static final ResourceLocation TEXTUUR = Guhs.id("textures/misc/guhwaiispellen_golf.png");
    /** The profile: blocks from the crest (below 0: the face towards the beach). */
    private static final double[] X = {-5.2, -4.2, -3.4, -2.7, -2.1, -1.6, -1.15, -0.75, -0.4, -0.12, 0.0, 0.35, 0.9, 1.8, 3.0, 4.6, 6.6, 9.0, 12.0, 15.5};
    private static final Map<Long, SurfGolven> GOLVEN = new HashMap<>();
    private static final Map<Long, Boolean> WATER = new HashMap<>();
    private static long waterLevelKey;

    private static SurfGolven golven(SurfPlankEntity bord) {
        long key = ((long) bord.seed() << 2) ^ bord.niveau().ordinal();
        if (GOLVEN.size() > 16) {
            GOLVEN.clear();
        }
        return GOLVEN.computeIfAbsent(key, k -> new SurfGolven(bord.niveau(), bord.seed()));
    }

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        if (level.hashCode() != waterLevelKey || WATER.size() > 40000) {
            WATER.clear();
            waterLevelKey = level.hashCode();
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucent(TEXTUUR);
        boolean iets = false;
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof SurfPlankEntity bord) || bord.isLilo() || bord.distanceToSqr(cam) > 160 * 160) {
                continue;
            }
            SurfGolven golven = golven(bord);
            SurfSim eigen = bord.eigen && bord.getId() == SurfClient.bordId() ? SurfClient.sim() : null;
            double step = (eigen != null ? eigen.step() : bord.stap()) + pt;
            Surfplek.Spot spot = bord.spot();
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);
            VertexConsumer vc = buffers.getBuffer(type);
            int light = LevelRenderer.getLightColor(level, spot.origin().above());
            for (int k = 0; k < golven.golven().size(); k++) {
                if (golven.actief(k, step)) {
                    golf(vc, pose.last().pose(), pose.last(), level, golven, spot, k, step, light);
                    iets = true;
                }
            }
            pose.popPose();
        }
        if (iets) {
            buffers.endBatch(type);
        }
    }

    private static void golf(VertexConsumer vc, Matrix4f m, PoseStack.Pose last, ClientLevel level, SurfGolven golven, Surfplek.Spot spot, int k,
                             double step, int light) {
        double crest = golven.crest(k, step);
        double amp = golven.amp(k, step);
        boolean breekt = golven.breekt(k, step);
        double tijd = step * 0.05;
        int n = (int) (SurfGolven.HALF * 2);
        double[][] ys = new double[n + 1][X.length];
        double[][] schuim = new double[n + 1][X.length];
        boolean[] water = new boolean[n + 1];
        double[] krul = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            double v = -SurfGolven.HALF + i;
            double rand = SurfGolven.clamp((SurfGolven.HALF - Math.abs(v)) / 5.0, 0, 1);
            double d = golven.voorDeBreek(k, step, v);
            double gebroken = breekt ? SurfGolven.clamp((0.8 - d) / 2.4, 0, 1) : 0;          // 0 green .. 1 whitewater
            double a = amp * rand * (1 - 0.55 * gebroken);
            krul[i] = breekt && d >= 0 && d < 6 ? (1 - d / 6) * rand : 0;
            water[i] = isWater(level, spot.wereld(crest, v, 0));
            for (int j = 0; j < X.length; j++) {
                double x = X[j];
                double h = SurfGolven.vorm(a, x);
                h += 0.05 * Math.sin(v * 0.9 + x * 1.3 + tijd * 3) * rand;                   // a little ripple
                if (gebroken > 0 && x > -2.5 && x < 1.5) {
                    h += gebroken * 0.18 * Math.sin(v * 2.3 + tijd * 9 + x * 4) * amp / 2;    // bubbling whitewater
                }
                ys[i][j] = Math.max(0.02, h + 0.03);
                double lip = SurfGolven.clamp(1 - Math.abs(x + 0.1) / 0.9, 0, 1) * SurfGolven.clamp(a / 1.2, 0, 1);
                schuim[i][j] = Math.max(gebroken * SurfGolven.clamp(1 - Math.abs(x + 0.6) / 2.6, 0, 1), lip * 0.8);
            }
        }
        for (int i = 0; i < n; i++) {
            if (!water[i] || !water[i + 1]) {
                continue;
            }
            double v0 = -SurfGolven.HALF + i, v1 = v0 + 1;
            for (int j = 0; j < X.length - 1; j++) {
                Vec3 a = spot.wereld(crest + X[j], v0, ys[i][j]);
                Vec3 b = spot.wereld(crest + X[j], v1, ys[i + 1][j]);
                Vec3 c = spot.wereld(crest + X[j + 1], v1, ys[i + 1][j + 1]);
                Vec3 d = spot.wereld(crest + X[j + 1], v0, ys[i][j + 1]);
                Vec3 nrm = b.subtract(a).cross(d.subtract(a)).normalize();
                if (nrm.y < 0) {
                    nrm = nrm.scale(-1);
                }
                punt(vc, m, last, a, kleur(ys[i][j], amp, schuim[i][j], X[j]), (float) (v0 / 4 + tijd * 0.05), (float) (X[j] / 3 - tijd * 0.2), light, nrm);
                punt(vc, m, last, b, kleur(ys[i + 1][j], amp, schuim[i + 1][j], X[j]), (float) (v1 / 4 + tijd * 0.05), (float) (X[j] / 3 - tijd * 0.2), light, nrm);
                punt(vc, m, last, c, kleur(ys[i + 1][j + 1], amp, schuim[i + 1][j + 1], X[j + 1]), (float) (v1 / 4 + tijd * 0.05),
                        (float) (X[j + 1] / 3 - tijd * 0.2), light, nrm);
                punt(vc, m, last, d, kleur(ys[i][j + 1], amp, schuim[i][j + 1], X[j + 1]), (float) (v0 / 4 + tijd * 0.05),
                        (float) (X[j + 1] / 3 - tijd * 0.2), light, nrm);
            }
            // the lip curling over next to the break: a hood from the crest out over the face
            double kr = Math.min(krul[i], krul[i + 1]);
            if (kr > 0.05) {
                double h0 = ys[i][10], h1 = ys[i + 1][10];
                double[][] hood = {{0.0, 1.0}, {-0.7, 1.08}, {-1.35, 0.92}, {-1.8, 0.62}};
                for (int s = 0; s < hood.length - 1; s++) {
                    Vec3 a = spot.wereld(crest + hood[s][0] * krul[i], v0, h0 * (1 + (hood[s][1] - 1) * krul[i]));
                    Vec3 b = spot.wereld(crest + hood[s][0] * krul[i + 1], v1, h1 * (1 + (hood[s][1] - 1) * krul[i + 1]));
                    Vec3 c = spot.wereld(crest + hood[s + 1][0] * krul[i + 1], v1, h1 * (hood[s + 1][1] * krul[i + 1] + (1 - krul[i + 1]) * 0.98));
                    Vec3 d = spot.wereld(crest + hood[s + 1][0] * krul[i], v0, h0 * (hood[s + 1][1] * krul[i] + (1 - krul[i]) * 0.98));
                    Vec3 nrm = new Vec3(0, 1, 0);
                    int col = s == 0 ? 0xE8F4FFFF : s == 1 ? 0xD8C8F4F8 : 0xC89EE8F0;
                    punt(vc, m, last, a, col, (float) (v0 / 4), (float) (s / 3.0), light, nrm);
                    punt(vc, m, last, b, col, (float) (v1 / 4), (float) (s / 3.0), light, nrm);
                    punt(vc, m, last, c, col, (float) (v1 / 4), (float) ((s + 1) / 3.0), light, nrm);
                    punt(vc, m, last, d, col, (float) (v0 / 4), (float) ((s + 1) / 3.0), light, nrm);
                }
            }
        }
        spray(level, golven, spot, k, step, crest, amp);
    }

    /** Spray off the break and the lip (a few particles per frame: they live longer than a frame anyway). */
    private static void spray(ClientLevel level, SurfGolven golven, Surfplek.Spot spot, int k, double step, double crest, double amp) {
        if (!golven.breekt(k, step) || level.getRandom().nextInt(3) != 0) {
            return;
        }
        var r = level.getRandom();
        double pel = golven.pel(k, step);
        int kant = golven.golf(k).kant();
        double v = pel - kant * r.nextDouble() * 10;
        if (Math.abs(v) < SurfGolven.HALF - 2) {
            Vec3 p = spot.wereld(crest - 0.6, v, amp * 0.5);
            if (isWater(level, spot.wereld(crest, v, 0))) {
                level.addParticle(r.nextBoolean() ? ParticleTypes.CLOUD : ParticleTypes.SPLASH, p.x, p.y, p.z, 0, 0.04, 0);
            }
        }
        Vec3 lip = spot.wereld(crest, pel, amp);
        if (Math.abs(pel) < SurfGolven.HALF - 2 && isWater(level, spot.wereld(crest, pel, 0))) {
            level.addParticle(ParticleTypes.SPLASH, lip.x, lip.y, lip.z, 0, 0.2, 0);
        }
    }

    /** Deep turquoise in the trough, lighter up the face, white foam on the lip and in the whitewater. */
    private static int kleur(double h, double amp, double schuim, double x) {
        double t = amp <= 0 ? 0 : SurfGolven.clamp(h / Math.max(0.3, amp), 0, 1);
        double r = 40 + 60 * t, g = 170 + 55 * t, b = 200 + 35 * t;
        double f = SurfGolven.clamp(schuim, 0, 1);
        r = r + (250 - r) * f;
        g = g + (252 - g) * f;
        b = b + (255 - b) * f;
        double a = 0.62 + 0.25 * t + 0.13 * f;
        if (x > 9) {
            a *= SurfGolven.clamp((15.5 - x) / 6.5, 0, 1);                              // the back fades into the sea
        } else if (x < -4.3) {
            a *= SurfGolven.clamp((x + 5.2) / 0.9, 0, 1);                               // and the foot of the face too
        }
        return ((int) (a * 255) << 24) | ((int) r << 16) | ((int) g << 8) | (int) b;
    }

    private static void punt(VertexConsumer vc, Matrix4f m, PoseStack.Pose last, Vec3 p, int argb, float u, float v, int light, Vec3 n) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
    }

    /** Is there open water at this spot (the calm surface's block)? Remembered per block. */
    private static boolean isWater(ClientLevel level, Vec3 p) {
        BlockPos b = BlockPos.containing(p.x, p.y - 0.5, p.z);
        return WATER.computeIfAbsent(b.asLong(), k -> level.getFluidState(b).is(FluidTags.WATER) && level.getBlockState(b.above()).isAir());
    }

    private GolfRenderer() {
    }
}
