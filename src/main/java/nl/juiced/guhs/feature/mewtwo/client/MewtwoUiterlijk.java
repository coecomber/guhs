package nl.juiced.guhs.feature.mewtwo.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import nl.juiced.guhs.feature.verhaal.client.VariantUiterlijk;

import net.minecraft.client.renderer.rendertype.RenderTypes;
/**
 * The Guhtwo's look on the client: it floats (the flag ZWEEFT lifts the whole model a little, bobbing; less when it
 * sits, hardly when you ride it), a soft purple glow on the ground under it, glowing purple eyes and tail bulb (the glow
 * layer; not the eyes while it sleeps), a slowly swaying tail, and the funny x2 double chomp when it eats (two quick nods).
 */
public final class MewtwoUiterlijk implements VariantUiterlijk.Uiterlijk {
    private static final Identifier GLOED = Guhs.id("textures/entity/guh_mewtwo_gloed.png");
    private static final Identifier GLOED_SLAAP = Guhs.id("textures/entity/guh_mewtwo_gloed_slaap.png");
    private static final Identifier ZWEEFGLOED = Guhs.id("textures/entity/mewtwo_zweefgloed.png");
    /** How high it floats (model pixels, before its size): walking/standing, sitting, ridden. */
    public static final float ZWEEF = 7f, ZWEEF_ZIT = 3f, ZWEEF_RIJ = 0.8f;
    /** When each Guhtwo last ate (entity id -> client game time): the x2 chomp. */
    private static final Map<Integer, Long> HAPPEN = new ConcurrentHashMap<>();
    private static final int HAP_TICKS = 16;

    static void hap(int guhId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            HAPPEN.put(guhId, mc.level.getGameTime());
        }
    }

    /** How high this guh floats now (model pixels). */
    public static float hoogte(GuhEntity guh, float pt) {
        if (!GuhHooks.heeft(guh, VerhaalVlaggen.ZWEEFT)) {
            return 0f;
        }
        if (guh.isVehicle()) {
            return ZWEEF_RIJ;
        }
        if (guh.isInSittingPose() || GuhRenderer.slaapt(guh)) {
            return ZWEEF_ZIT;
        }
        return ZWEEF + Mth.sin((guh.tickCount + pt + guh.getId() * 13) * 0.08f) * 1.2f;
    }

    @Override
    public Identifier glow(GuhEntity guh) {
        return GuhRenderer.slaapt(guh) ? GLOED_SLAAP : GLOED;
    }

    @Override
    public void botten(GuhEntity guh, GuhRenderFrame frame, float pt) {
        // 1.1.0: everything is worked out now (extract time); the bone moves only use these values
        float h = hoogte(guh, pt);
        float t = guh.tickCount + pt;
        float staartY = Mth.sin(t * 0.05f + guh.getId()) * 0.28f, staartX = 0.12f + Mth.sin(t * 0.07f) * 0.06f;
        float nod = -1f;
        Long hap = HAPPEN.get(guh.getId());
        if (hap != null && guh.level() != null) {
            float d = guh.level().getGameTime() - hap + pt;
            if (d > HAP_TICKS || d < 0) {
                HAPPEN.remove(guh.getId());
            } else {
                // two quick chomps: the head nods down twice, the cheeks puff (the head a little wider)
                nod = Mth.sin(d / HAP_TICKS * Mth.PI * 4) * 0.35f;
            }
        }
        float knik = nod;
        frame.bones(bones -> {
            bones.ifPresent("root", b -> b.setTranslateY(h));
            bones.ifPresent("mewtwo_staart", b -> {
                b.setRotY(staartY);
                b.setRotX(staartX);
            });
            bones.ifPresent("head", b -> b.setScaleX(1f));
            if (knik >= -0.5f) {
                bones.ifPresent("head", b -> {
                    b.setRotX(b.getRotX() + Math.max(0, knik));
                    b.setScaleX(1f + Math.max(0, knik) * 0.25f);
                });
            }
        });
    }

    @Override
    public void extra(GuhEntity guh, GuhRenderFrame frame, float pt) {
        float h = hoogte(guh, pt);
        if (h < 2f || guh.isInvisible()) {
            return;
        }
        // a soft purple glow on the ground under the floating guh, breathing slowly
        float r = 0.55f * guh.getScale() * guh.getAgeScale() * (1f + Mth.sin((guh.tickCount + pt) * 0.08f) * 0.06f);
        int a = (int) (150 + 40 * Mth.sin((guh.tickCount + pt) * 0.08f));
        int kleur = (a << 24) | 0xFFFFFF;
        frame.extra((pose, collector, light) -> collector.submitCustomGeometry(pose, nl.juiced.guhs.client.GuhRenderTypes.eyes(ZWEEFGLOED), (p, vc) -> {
            float y = 0.03f;
            vc.addVertex(p, -r, y, -r).setColor(kleur).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p, 0, 1, 0);
            vc.addVertex(p, -r, y, r).setColor(kleur).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p, 0, 1, 0);
            vc.addVertex(p, r, y, r).setColor(kleur).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p, 0, 1, 0);
            vc.addVertex(p, r, y, -r).setColor(kleur).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p, 0, 1, 0);
        }));
    }
}
