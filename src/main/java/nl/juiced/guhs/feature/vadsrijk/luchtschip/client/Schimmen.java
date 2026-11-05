package nl.juiced.guhs.feature.vadsrijk.luchtschip.client;

import java.util.List;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SkyDraw;
import nl.juiced.guhs.world.GuhTime;
import org.joml.Vector4f;

/**
 * T-LUCHTSCHIP (W-3 "Zicht", WERELDINDELING §5.1 "verre schimmen"): client-side silhouettes of the other zones at their REAL
 * place and size, drawn in the sky layer so they are visible far beyond the render distance (also on the server, which sends
 * only its own view distance of real chunks).
 * <ul>
 *   <li>each silhouette is one or two standing textured quads (billboard art from tools/features/vadsrijk_t_luchtschip.py:
 *       De Boom, the Splinterpieken round Kandijkom, the Kaaszeppelinwerf, Moeder Wolkwal with Hoogvads on her back, side + front);</li>
 *   <li>the quads are the real world corners, scaled towards the camera onto the sky dome (uniform scaling about the eye keeps
 *       direction and apparent size exact), so they sit exactly where the real thing is;</li>
 *   <li>drawn after the sky and before the terrain: whatever real land is loaded in front covers them; when the thing itself comes
 *       within the render distance its silhouette fades away (the real chunks take over);</li>
 *   <li>hazy and cheap: 2-4 quads each, one texture, tinted by the time of day.</li>
 * </ul>
 * Stand ({@link #stand}): 0 off, 1 normal, 2 always at full strength (dev: {@code /guhs vadsrijk luchtschip schimmen altijd}).
 */
public final class Schimmen {
    /** A silhouette: centre x/z, bottom/top y, half length along its axis, half width across, axis yaw (degrees, 0 = along x), textures. */
    record Schim(String id, double x, double z, double y0, double y1, double lang, double breed, float as, Identifier zij, Identifier voor) {
    }

    private static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/luchtschip/schim_" + naam + ".png");
    }

    static final List<Schim> ALLE = List.of(
            // De Boom (zone 3): trunk at (1404, -576), the crown up to ~y 330, roots island r ~240
            new Schim("boom", 1404, -576, 30, 335, 250, 250, 0f, tex("boom"), tex("boom")),
            // the Splinterpieken round Kandijkom (zone 2)
            new Schim("pieken", 704, -1121, 55, 240, 280, 260, 0f, tex("pieken"), tex("pieken")),
            // the Kaaszeppelinwerf (zone 3, its own floating island)
            new Schim("werf", 1655, -827, 140, 235, 75, 75, 0f, tex("werf"), tex("werf")),
            // Moeder Wolkwal with Hoogvads (zone 5), floating over the east of zone 4: head x 2440, tail x 3140, axis z -520
            new Schim("hoogvads", 2790, -520, 160, 335, 360, 130, 0f, tex("hoogvads_zij"), tex("hoogvads_voor")));

    /** 0 off, 1 normal, 2 always + full strength. */
    static int stand = 1;

    private Schimmen() {
    }

    /** Do the silhouettes replace the old fixed-azimuth landmarks of {@code id} (boom, pieken, hoogvads)? (hook for the zone skies) */
    public static boolean vervangt(String id) {
        return stand != 0;
    }

    static void teken(RenderLevelStageEvent.AfterSky e) {
        if (stand == 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        double r = mc.options.getEffectiveRenderDistance() * 16.0;
        long tijd = GuhTime.timeOfDay(mc.level);
        float nacht = tijd >= 13000 && tijd <= 23000 ? 1f : tijd > 12000 && tijd < 13000 ? (tijd - 12000) / 1000f : tijd > 23000 ? 1f - (tijd - 23000) / 1000f : 0f;
        float licht = Mth.lerp(nacht, 0.92f, 0.32f);
        for (Schim s : ALLE) {
            double dx = s.x() - cam.x, dz = s.z() - cam.z;
            double d = Math.sqrt(dx * dx + dz * dz);
            double rand = Math.max(1, d - s.lang() * 0.6);
            float a;
            if (stand == 2) {
                a = 1f;
            } else {
                a = (float) (Mth.clamp((rand - r * 0.82) / (r * 0.35 + 24), 0, 1) * Mth.clamp((2600 - d) / 600, 0, 1)) * Mth.lerp(nacht, 0.78f, 0.55f);
            }
            if (a < 0.02f || d < 24) {
                continue;
            }
            double ymid = (s.y0() + s.y1()) / 2;
            double afst = Math.sqrt(d * d + (ymid - cam.y) * (ymid - cam.y));
            float k = (float) (90.0 / Math.max(afst, 1));
            Vector4f kleur = new Vector4f(licht, licht * 0.97f, licht * 1.04f, a);
            double as = Math.toRadians(s.as());
            Vec3 langs = new Vec3(Math.cos(as), 0, Math.sin(as)), dwars = new Vec3(-Math.sin(as), 0, Math.cos(as));
            if (s.zij() == s.voor()) {
                // a round thing: one quad facing the camera (cylindrical billboard), half width = its radius
                Vec3 naarCam = new Vec3(-dx, 0, -dz).normalize();
                Vec3 rechts = new Vec3(-naarCam.z, 0, naarCam.x);
                SkyDraw.drawNow("Vadsrijk schim " + s.id(), SkyDraw.CELESTIAL_TRANSLUCENT, kwad(s, cam, k, rechts.scale(s.lang())), SkyDraw.texture(s.zij()), kleur,
                        e.getModelViewMatrix());
            } else {
                // a long thing: the side quad along its axis and the front quad across it, each faded by how much it faces you
                Vec3 kijk = new Vec3(dx, 0, dz).normalize();
                float zijA = (float) Mth.clamp(Math.abs(kijk.dot(dwars)) * 1.4, 0, 1), voorA = (float) Mth.clamp(Math.abs(kijk.dot(langs)) * 1.4, 0, 1);
                if (zijA > 0.02f) {
                    SkyDraw.drawNow("Vadsrijk schim " + s.id(), SkyDraw.CELESTIAL_TRANSLUCENT, kwad(s, cam, k, langs.scale(s.lang())), SkyDraw.texture(s.zij()),
                            new Vector4f(kleur.x, kleur.y, kleur.z, a * zijA), e.getModelViewMatrix());
                }
                if (voorA > 0.02f) {
                    SkyDraw.drawNow("Vadsrijk schim " + s.id() + " voor", SkyDraw.CELESTIAL_TRANSLUCENT, kwad(s, cam, k, dwars.scale(s.breed())), SkyDraw.texture(s.voor()),
                            new Vector4f(kleur.x, kleur.y, kleur.z, a * voorA), e.getModelViewMatrix());
                }
            }
        }
    }

    /** A standing quad through the silhouette's centre spanning +-half horizontally, y0..y1, in camera-relative sky space (scale k). */
    private static MeshData kwad(Schim s, Vec3 cam, float k, Vec3 half) {
        Vec3 mid = new Vec3(s.x() - cam.x, 0, s.z() - cam.z);
        double lo = s.y0() - cam.y, hi = s.y1() - cam.y;
        Vec3[] p = {mid.subtract(half).add(0, lo, 0), mid.add(half).add(0, lo, 0), mid.add(half).add(0, hi, 0), mid.subtract(half).add(0, hi, 0)};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int[] o : new int[][] {{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            for (int i : o) {
                b.addVertex((float) (p[i].x * k), (float) (p[i].y * k), (float) (p[i].z * k)).setUv(uv[i][0], uv[i][1]);
            }
        }
        return b.buildOrThrow();
    }
}
