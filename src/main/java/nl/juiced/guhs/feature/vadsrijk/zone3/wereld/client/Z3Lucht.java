package nl.juiced.guhs.feature.vadsrijk.zone3.wereld.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SkyDraw;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;
import nl.juiced.guhs.world.GuhTime;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The sky of zone 3 "De Schemerwortel" (ZONE3_PLAN §1 skybox), an overlay after the vanilla sky and zone 1's WereldLucht (which
 * keeps its sun glow, moon, guh stars and dragon, but skips its far landmarks in zone 3):
 * <ul>
 *   <li>by day a golden honey veil, warmest at the horizon; a pink sun glow low in the west;</li>
 *   <li>towards the journey (EAST): <b>Hoogvads</b>, the floating city of the resistance, on cloud islands above the horizon;
 *       behind it the volcano with black fluff (the Roetfront) and, at the far edge, a thin grey streak (the Grauwe Einder);</li>
 *   <li>at night: fireflies drifting as a second starry sky (Z3Sfeer) and Hoogvads' windows glowing pink.</li>
 * </ul>
 * Under the crown and in the roots the sky is hidden by the blocks themselves; there only the fog colour (Z3WereldClient) works.
 * Textures: tools/features/vadsrijk_z3_wereld.py (z3_hoogvads.png, z3_einder.png) + zone 1's lucht_verte_3 (volcano), zone 2's
 * z2_band (the veil gradient).
 */
final class Z3Lucht {
    private static final Identifier BAND = Guhs.id("textures/vadsrijk/wereld/z2_band.png"), HOOGVADS = tex("z3_hoogvads"),
            HOOGVADS_NACHT = tex("z3_hoogvads_nacht"), EINDER = tex("z3_einder"), VULKAAN = tex("lucht_verte_3");

    private Z3Lucht() {
    }

    private static Identifier tex(String n) {
        return Guhs.id("textures/vadsrijk/wereld/" + n + ".png");
    }

    /** 0 outside zone 3, 1 inside (soft over 200 blocks west of its tile range). */
    static float sterkte(double x, double z) {
        double rand = Z3WereldPlekken.TEGEL_X0 * 64;
        return (float) Mth.clamp((x - rand + 120) / 200.0, 0, 1) * (Math.abs(z) < 1100 ? 1f : 0f);
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
        float s = sterkte(mc.player.getX(), mc.player.getZ());
        if (s <= 0.01f) {
            return;
        }
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float t = mc.level.getGameTime() + pt;
        float nacht = nachtFactor(GuhTime.timeOfDay(mc.level));
        float dag = 1f - nacht;
        Matrix4fc mv = e.getModelViewMatrix();
        // 1. the honey veil: a golden band round the horizon (rose-violet at night)
        Vector4f band = new Vector4f(Mth.lerp(nacht, 1f, 0.42f), Mth.lerp(nacht, 0.8f, 0.24f), Mth.lerp(nacht, 0.45f, 0.5f), (0.55f * dag + 0.35f * nacht) * s);
        SkyDraw.drawNow("Z3 honey band", SkyDraw.CELESTIAL_TRANSLUCENT, band(-6f, 30f, 0f, 360f), SkyDraw.texture(BAND), band, mv);
        // 2. the Grauwe Einder: a thin grey streak at the far eastern edge
        SkyDraw.drawNow("Z3 grey horizon", SkyDraw.CELESTIAL_TRANSLUCENT, band(-1f, 3.5f, 45f, 135f), SkyDraw.texture(EINDER),
                new Vector4f(0.62f, 0.62f, 0.68f, 0.7f * s), mv);
        // 3. the volcano with black fluff behind Hoogvads (south-east)
        SkyDraw.drawNow("Z3 volcano", SkyDraw.CELESTIAL_TRANSLUCENT, kwad(118f, 4.5f, 22f, 11f), SkyDraw.texture(VULKAAN),
                new Vector4f(0.75f, 0.68f, 0.78f, (0.5f + 0.2f * dag) * s), mv);
        // 4. Hoogvads, the floating city (east), bobbing a little; windows glow pink at night
        float bob = Mth.sin(t * 0.004f) * 0.6f;
        SkyDraw.drawNow("Z3 Hoogvads", SkyDraw.CELESTIAL_TRANSLUCENT, kwad(93f, 9.5f + bob, 20f, 13f), SkyDraw.texture(HOOGVADS),
                new Vector4f(Mth.lerp(nacht, 1f, 0.5f), Mth.lerp(nacht, 0.95f, 0.46f), Mth.lerp(nacht, 0.98f, 0.62f), (0.6f + 0.2f * dag) * s), mv);
        if (nacht > 0.02f) {
            SkyDraw.drawNow("Z3 Hoogvads lights", SkyDraw.CELESTIAL_TRANSLUCENT, kwad(93f, 9.5f + bob, 20f, 13f), SkyDraw.texture(HOOGVADS_NACHT),
                    new Vector4f(1f, 1f, 1f, nacht * s), mv);
        }
    }

    private static Vector3f dir(float az, float h, float d) {
        double a = Math.toRadians(az), hh = Math.toRadians(h);
        return new Vector3f((float) (Math.sin(a) * Math.cos(hh)), (float) Math.sin(hh), (float) (-Math.cos(a) * Math.cos(hh))).mul(d);
    }

    private static MeshData kwad(float az, float h, float hw, float hh) {
        Vector3f mid = dir(az, h, 90f);
        Vector3f rechts = new Vector3f(mid).cross(0, 1, 0).normalize(hw);
        Vector3f op = new Vector3f(rechts).cross(mid).normalize(hh);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Vector3f[] p = {new Vector3f(mid).sub(rechts).sub(op), new Vector3f(mid).add(rechts).sub(op), new Vector3f(mid).add(rechts).add(op),
                new Vector3f(mid).sub(rechts).add(op)};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int[] o : new int[][] {{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            for (int i : o) {
                b.addVertex(p[i].x, p[i].y, p[i].z).setUv(uv[i][0], uv[i][1]);
            }
        }
        return b.buildOrThrow();
    }

    /** A ring (part) round the horizon from h0 to h1 degrees, azimuth a0..a1 (v = 1 at the bottom). */
    private static MeshData band(float h0, float h1, float a0_, float a1_) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        int n = 48;
        for (int i = 0; i < n; i++) {
            float a0 = a0_ + (a1_ - a0_) * i / n, a1 = a0_ + (a1_ - a0_) * (i + 1) / n;
            float u0 = (float) i / n, u1 = (float) (i + 1) / n;
            Vector3f p0 = dir(a0, h0, 95f), p1 = dir(a1, h0, 95f), p2 = dir(a1, h1, 95f), p3 = dir(a0, h1, 95f);
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
