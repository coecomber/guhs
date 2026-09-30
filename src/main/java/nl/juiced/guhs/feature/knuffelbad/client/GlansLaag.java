package nl.juiced.guhs.feature.knuffelbad.client;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;

import net.minecraft.client.renderer.rendertype.RenderTypes;
/**
 * A freshly washed guh (GuhHooks.GLANZEND, for a day after the Knuffelbad's washing ritual): soft pink-and-white glints
 * glide over its fluffy fur (the model drawn again with a moving shimmer), and little sparkles twinkle around it.
 */
final class GlansLaag {
    private static final Identifier GLANS = Guhs.id("textures/entity/guh_glans.png");

    private GlansLaag() {
    }

    /**
     * 1.1.0 (GeckoLib 5): a {@link GuhRenderer} hook; the shimmer is one more model pass (energy swirl, same colour as
     * before) on the bones the guh itself shows ({@link #zelfdeBotten}: 1.0.0 re-rendered the model with the main pass's
     * hidden bones).
     */
    static void extract(GuhEntity guh, float partialTick, GuhRenderFrame frame) {
        if (!GuhHooks.heeft(guh, GuhHooks.GLANZEND) || guh.isInvisible()) {
            return;
        }
        float t = (guh.tickCount + partialTick) * 0.006f;
        RenderType type = RenderTypes.energySwirl(GLANS, t % 1f, (t * 0.6f) % 1f);
        frame.pass(type, 0xFFD9C4D2, false, name -> zelfdeBotten(frame, name));
    }

    /**
     * The bones the guh's own model pass shows (GuhRenderer#applyVisibility: saddle, armour, variant bones under clothes);
     * the saddle and armour bones have no child bones.
     */
    static boolean zelfdeBotten(GuhRenderFrame frame, String name) {
        if (name.equals("saddle")) {
            return frame.saddled;
        }
        if (name.startsWith("armor_")) {
            if (frame.armour == null) {
                return false;
            }
            String own = "armor_" + frame.armour.name().toLowerCase(Locale.ROOT);
            return name.equals(own) || name.equals(own + "_body");
        }
        if (GuhVariant.VARIANT_BONES.stream().anyMatch(name::startsWith)) {
            for (GuhClothes.Slot slot : frame.clothes.keySet()) {
                if (GuhClothes.slotBones(slot).stream().anyMatch(name::startsWith)) {
                    return false;
                }
            }
            return frame.variant.shows(name);
        }
        return true;
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
