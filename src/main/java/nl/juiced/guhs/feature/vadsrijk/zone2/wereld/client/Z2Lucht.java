package nl.juiced.guhs.feature.vadsrijk.zone2.wereld.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SkyDraw;
import nl.juiced.guhs.world.GuhTime;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The sky of zone 2 "De Splinterpieken" (VOORSTEL_ZONE2 concept A), drawn as an overlay after the vanilla sky (and after zone 1's
 * {@code WereldLucht}, which keeps its sun glow, second moon and guh stars, but skips its own horizon in zone 2):
 * <ul>
 *   <li>by day a pale ice sky: a pale veil over the pink dimension sky, whitest at the horizon;</li>
 *   <li>mother-of-pearl clouds drifting slowly round the horizon;</li>
 *   <li>at night a green-pink aurora (northern lights) in waving curtains over the north;</li>
 *   <li>a big pale moon low in the north-north-east, breaking through a silhouette of crystal spires;</li>
 *   <li>the giant tree of zone 3 far away on the horizon.</li>
 * </ul>
 * The strength fades in over the first 200 blocks north of the zone-2 map edge (an area-based overlay, like zone 1's sky).
 * Textures: tools/features/vadsrijk_z2_wereld.py (textures/vadsrijk/wereld/z2_*.png).
 */
final class Z2Lucht {
    private static final Identifier BAND = tex("z2_band"), PAREL0 = tex("z2_parelwolk_0"), PAREL1 = tex("z2_parelwolk_1"),
            POOL = tex("z2_poollicht"), MAAN = tex("z2_maan"), SPITSEN = tex("z2_spitsen"), BOOM = Guhs.id("textures/vadsrijk/wereld/lucht_verte_2.png");
    /** Clouds: azimuth, height (deg), half width, half height, texture 0/1, drift speed. */
    private static final float[][] WOLKEN = {{20f, 14f, 30f, 9f, 0, 1f}, {95f, 20f, 26f, 8f, 1, -0.7f}, {160f, 11f, 34f, 10f, 0, 0.5f},
            {230f, 18f, 28f, 8f, 1, -1.1f}, {290f, 24f, 22f, 7f, 0, 0.8f}, {335f, 9f, 32f, 9f, 1, -0.5f}};

    private Z2Lucht() {
    }

    private static Identifier tex(String n) {
        return Guhs.id("textures/vadsrijk/wereld/" + n + ".png");
    }

    /** 0 outside zone 2, 1 inside (soft over 200 blocks south of its tile range). */
    static float sterkte(double z) {
        double rand = -1728;          // south edge of the zone-2 tiles
        return (float) Mth.clamp((rand + 40 - z) / 200.0, 0, 1);
    }

    static float nachtFactor(long tijd) {
        if (tijd >= 13500 && tijd <= 22500) {
            return 1f;
        }
        if (tijd > 12000 && tijd < 13500) {
            return (tijd - 12000) / 1500f;
        }
        if (tijd > 22500) {
            return 1f - (tijd - 22500) / 1500f;
        }
        return 0f;
    }

    static void teken(RenderLevelStageEvent.AfterSky e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        float s = sterkte(mc.player.getZ());
        if (s <= 0.01f) {
            return;
        }
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float t = mc.level.getGameTime() + pt;
        float nacht = nachtFactor(GuhTime.timeOfDay(mc.level));
        float dag = 1f - nacht;
        Matrix4fc mv = e.getModelViewMatrix();
        // 1. the pale ice veil (a cap overhead + a band at the horizon)
        if (dag > 0.01f) {
            SkyDraw.drawNow("Z2 ice veil", SkyDraw.CELESTIAL_TRANSLUCENT, kap(), SkyDraw.texture(BAND), new Vector4f(0.88f, 0.94f, 1f, 0.32f * dag * s), mv);
        }
        Vector4f bandKleur = new Vector4f(Mth.lerp(nacht, 0.92f, 0.16f), Mth.lerp(nacht, 0.96f, 0.2f), Mth.lerp(nacht, 1f, 0.34f), (0.62f * dag + 0.4f * nacht) * s);
        SkyDraw.drawNow("Z2 horizon band", SkyDraw.CELESTIAL_TRANSLUCENT, band(-6f, 34f), SkyDraw.texture(BAND), bandKleur, mv);
        // 2. the giant tree (zone 3) far away
        // [zone 3] the tree is south-east of zone 2 (x 2300, z 0)
        SkyDraw.drawNow("Z2 giant tree", SkyDraw.CELESTIAL_TRANSLUCENT, kwad(136f, 9f, 30f, 22f, 1f), SkyDraw.texture(BOOM),
                new Vector4f(0.78f, 0.86f, 0.95f, (0.42f + 0.18f * dag) * s), mv);
        // 3. the moon through the crystal spires (night), then the spires in front of it
        if (nacht > 0.02f) {
            SkyDraw.drawNow("Z2 big moon", SkyDraw.CELESTIAL_TRANSLUCENT, kwad(16f, 11f, 12f, 12f, 1f), SkyDraw.texture(MAAN),
                    new Vector4f(1f, 1f, 1f, 0.95f * nacht * s), mv);
        }
        SkyDraw.drawNow("Z2 crystal spires", SkyDraw.CELESTIAL_TRANSLUCENT, kwad(14f, 8f, 40f, 17f, 1f), SkyDraw.texture(SPITSEN),
                new Vector4f(Mth.lerp(nacht, 0.86f, 0.42f), Mth.lerp(nacht, 0.9f, 0.46f), Mth.lerp(nacht, 0.98f, 0.62f), (0.55f + 0.35f * nacht) * s), mv);
        // 4. mother-of-pearl clouds
        for (float[] w : WOLKEN) {
            float az = w[0] + t * 0.0015f * w[5];
            SkyDraw.drawNow("Z2 pearl cloud", SkyDraw.CELESTIAL_TRANSLUCENT, kwad(az, w[1], w[2], w[3], 1f), SkyDraw.texture(w[4] == 0 ? PAREL0 : PAREL1),
                    new Vector4f(1f, 1f, 1f, (0.75f * dag + 0.22f * nacht) * s), mv);
        }
        // 5. aurora: three waving curtains over the north, additive
        if (nacht > 0.02f) {
            for (int k = 0; k < 3; k++) {
                SkyDraw.drawNow("Z2 aurora", RenderPipelines.CELESTIAL, gordijn(t, k), SkyDraw.texture(POOL),
                        new Vector4f(1f, 1f, 1f, nacht * s * (0.75f - k * 0.18f)), mv);
            }
        }
    }

    /** A direction on the sky: az degrees (0 = north = -z, clockwise), h degrees above the horizon, at distance d. */
    private static Vector3f dir(float az, float h, float d) {
        double a = Math.toRadians(az), hh = Math.toRadians(h);
        return new Vector3f((float) (Math.sin(a) * Math.cos(hh)), (float) Math.sin(hh), (float) (-Math.cos(a) * Math.cos(hh))).mul(d);
    }

    /** A camera-facing quad at (az, h): half width hw, half height hh (blocks at distance 90). */
    private static MeshData kwad(float az, float h, float hw, float hh, float vMax) {
        Vector3f mid = dir(az, h, 90f);
        Vector3f rechts = new Vector3f(mid).cross(0, 1, 0).normalize(hw);
        Vector3f op = new Vector3f(rechts).cross(mid).normalize(hh);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Vector3f[] p = {new Vector3f(mid).sub(rechts).sub(op), new Vector3f(mid).add(rechts).sub(op), new Vector3f(mid).add(rechts).add(op),
                new Vector3f(mid).sub(rechts).add(op)};
        float[][] uv = {{0, vMax}, {1, vMax}, {1, 0}, {0, 0}};
        for (int[] o : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            for (int i : o) {
                b.addVertex(p[i].x, p[i].y, p[i].z).setUv(uv[i][0], uv[i][1]);
            }
        }
        return b.buildOrThrow();
    }

    /** A ring round the horizon from h0 to h1 degrees (texture v = 1 at the bottom, 0 at the top: the band fades upward). */
    private static MeshData band(float h0, float h1) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        int n = 48;
        for (int i = 0; i < n; i++) {
            float a0 = 360f * i / n, a1 = 360f * (i + 1) / n;
            Vector3f p0 = dir(a0, h0, 95f), p1 = dir(a1, h0, 95f), p2 = dir(a1, h1, 95f), p3 = dir(a0, h1, 95f);
            b.addVertex(p0.x, p0.y, p0.z).setUv(0, 1);
            b.addVertex(p1.x, p1.y, p1.z).setUv(1, 1);
            b.addVertex(p2.x, p2.y, p2.z).setUv(1, 0);
            b.addVertex(p3.x, p3.y, p3.z).setUv(0, 0);
            b.addVertex(p3.x, p3.y, p3.z).setUv(0, 0);
            b.addVertex(p2.x, p2.y, p2.z).setUv(1, 0);
            b.addVertex(p1.x, p1.y, p1.z).setUv(1, 1);
            b.addVertex(p0.x, p0.y, p0.z).setUv(0, 1);
        }
        return b.buildOrThrow();
    }

    /** The veil overhead: a flat square at y 100 using the solid bottom row of the band texture. */
    private static MeshData kap() {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float s = 160f;
        float[][] p = {{-s, -s}, {s, -s}, {s, s}, {-s, s}};
        for (int[] o : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            for (int i : o) {
                b.addVertex(p[i][0], 100f, p[i][1]).setUv(0.5f, 0.98f);
            }
        }
        return b.buildOrThrow();
    }

    /** An aurora curtain: a ribbon from azimuth 285 through north to 75, its foot waving slowly. */
    private static MeshData gordijn(float t, int k) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        int n = 40;
        float start = 285f + k * 12f, span = 150f - k * 20f;
        for (int i = 0; i < n; i++) {
            float f0 = (float) i / n, f1 = (float) (i + 1) / n;
            float a0 = start + span * f0, a1 = start + span * f1;
            float g0 = 22f + k * 7f + 5f * Mth.sin(a0 * 0.09f + t * 0.012f + k), g1 = 22f + k * 7f + 5f * Mth.sin(a1 * 0.09f + t * 0.012f + k);
            float hoog = 26f - k * 4f;
            float u0 = f0, u1 = f1;
            Vector3f p0 = dir(a0, g0, 92f), p1 = dir(a1, g1, 92f), p2 = dir(a1, g1 + hoog, 92f), p3 = dir(a0, g0 + hoog, 92f);
            b.addVertex(p0.x, p0.y, p0.z).setUv(u0, 1);
            b.addVertex(p1.x, p1.y, p1.z).setUv(u1, 1);
            b.addVertex(p2.x, p2.y, p2.z).setUv(u1, 0);
            b.addVertex(p3.x, p3.y, p3.z).setUv(u0, 0);
            b.addVertex(p3.x, p3.y, p3.z).setUv(u0, 0);
            b.addVertex(p2.x, p2.y, p2.z).setUv(u1, 0);
            b.addVertex(p1.x, p1.y, p1.z).setUv(u1, 1);
            b.addVertex(p0.x, p0.y, p0.z).setUv(u0, 1);
        }
        return b.buildOrThrow();
    }
}
