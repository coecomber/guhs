package nl.juiced.guhs.feature.knuffelbad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.renderer.base.GeoRenderer;

import net.minecraft.client.renderer.rendertype.RenderTypes;
/**
 * A freshly washed guh (GuhHooks.GLANZEND, for a day after the Knuffelbad's washing ritual): soft pink-and-white glints
 * glide over its fluffy fur (the model drawn again with a moving shimmer), and little sparkles twinkle around it.
 */
final class GlansLaag {
    private static final Identifier GLANS = Guhs.id("textures/entity/guh_glans.png");

    private GlansLaag() {
    }

    static void render(GeoRenderer<GuhEntity> renderer, PoseStack pose, GuhEntity guh, BakedGeoModel model, MultiBufferSource buffers,
                       float partialTick, int light, int overlay) {
        if (!GuhHooks.heeft(guh, GuhHooks.GLANZEND) || guh.isInvisible()) {
            return;
        }
        float t = (guh.tickCount + partialTick) * 0.006f;
        RenderType type = RenderTypes.energySwirl(GLANS, t % 1f, (t * 0.6f) % 1f);
        renderer.reRender(model, pose, buffers, guh, type, buffers.getBuffer(type), partialTick, light, OverlayTexture.NO_OVERLAY, 0xFFD9C4D2);
    }

    /** Sparkles round the shiny guhs near you. */
    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        RandomSource r = mc.level.getRandom();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof GuhEntity guh && GuhHooks.heeft(guh, GuhHooks.GLANZEND) && ((guh.tickCount + guh.getId()) % 7 == 0)
                    && guh.distanceToSqr(mc.player) < 32 * 32 && !guh.isInvisible()) {
                double w = guh.getBbWidth() * 0.7, h = guh.getBbHeight();
                mc.level.addParticle(KnuffelbadFeature.GLINSTERING.get(), guh.getX() + (r.nextDouble() - 0.5) * w * 2, guh.getY() + r.nextDouble() * h * 1.1,
                        guh.getZ() + (r.nextDouble() - 0.5) * w * 2, 0, 0.005, 0);
            }
        }
    }
}
