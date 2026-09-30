package nl.juiced.guhs.feature.guhpolder.client;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guhpolder.GuhpolderFeature;
import nl.juiced.guhs.feature.guhpolder.Pinguh;
import com.geckolib.cache.model.GeoBone;

/**
 * How a Pinguh looks and moves (client only; three small hooks in client.GuhRenderer call this):
 * <ul>
 *   <li>{@link #texture}: its look ({@link Pinguh.Look}: klassiek, keizer, or the grey fluffy chick);</li>
 *   <li>{@link #pose}: the waddle (a side-to-side roll while it walks) and the belly-slide (low on the ice, head up);</li>
 *   <li>{@link #animate}: in the slide its paws stretch out front and back and its flippers spread; walking, the flippers flap.</li>
 * </ul>
 * It slides when it goes fast over slide ice ({@link GuhpolderFeature#GLIJIJS}); the pose blends in and out smoothly.
 */
public final class PinguhRender {
    public static final Identifier KLASSIEK = Guhs.id("textures/entity/guh_pinguh.png");
    public static final Identifier KEIZER = Guhs.id("textures/entity/guh_pinguh_keizer.png");
    public static final Identifier PLUIS = Guhs.id("textures/entity/guh_pinguh_pluis.png");
    /** Faster than this (blocks per tick) on slide ice = a belly-slide. */
    public static final double GLIJ_SNELHEID = 0.09;

    /** Per Pinguh: {slide now, slide a tick ago, the tick it was last updated}. */
    private static final Map<GuhEntity, float[]> GLIJ = new WeakHashMap<>();

    private PinguhRender() {
    }

    /** Its texture, or null when it isn't a Pinguh. */
    @Nullable
    public static Identifier texture(GuhEntity guh) {
        if (guh.getVariant() != GuhVariant.PINGUH) {
            return null;
        }
        return switch (Pinguh.look(guh)) {
            case KLASSIEK -> KLASSIEK;
            case KEIZER -> KEIZER;
            case PLUIS -> PLUIS;
        };
    }

    /** How much it is belly-sliding now (0 = standing/walking, 1 = sliding), smoothed. */
    public static float glij(GuhEntity guh, float partialTick) {
        float[] g = GLIJ.computeIfAbsent(guh, k -> new float[]{0, 0, -1});
        if (g[2] != guh.tickCount) {
            g[2] = guh.tickCount;
            g[1] = g[0];
            double v = Math.sqrt(Mth.square(guh.getX() - guh.xo) + Mth.square(guh.getZ() - guh.zo));
            boolean ijs = guh.level().getBlockState(BlockPos.containing(guh.getX(), guh.getY() - 0.2, guh.getZ())).is(GuhpolderFeature.GLIJIJS);
            float doel = ijs && v > GLIJ_SNELHEID ? 1f : 0f;
            g[0] += (doel - g[0]) * (doel > g[0] ? 0.45f : 0.2f);
        }
        return Mth.lerp(partialTick, g[1], g[0]);
    }

    /** After the body's rotation: the waddle and the slide (in world units, scaled with the guh). */
    public static void pose(GuhEntity guh, PoseStack pose, float partialTick) {
        if (guh.getVariant() != GuhVariant.PINGUH) {
            return;
        }
        float glij = glij(guh, partialTick);
        float size = guh.getScale() * guh.getAgeScale();
        float walk = Math.min(1f, guh.walkAnimation.speed(partialTick) * 2.2f) * (1f - glij);
        if (walk > 0.01f) {
            float t = guh.walkAnimation.position(partialTick);
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 0.9f) * 11f * walk));   // waddle waddle
            pose.mulPose(Axis.YP.rotationDegrees(Mth.sin(t * 0.9f) * 4f * walk));
        }
        if (glij > 0.01f) {
            pose.translate(0, -0.11f * size * glij, 0);                                  // on its belly
            pose.mulPose(Axis.XP.rotationDegrees(5f * glij));                            // head up, VAHOEG
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin((guh.tickCount + partialTick) * 0.25f) * 2.5f * glij));   // a little wobble
        }
    }

    /** After the animations: paws and flippers for the slide, flapping flippers for the waddle. */
    public static void animate(GuhEntity guh, Function<String, Optional<GeoBone>> bones, float partialTick) {
        if (guh.getVariant() != GuhVariant.PINGUH) {
            return;
        }
        float glij = glij(guh, partialTick);
        if (glij > 0.01f) {
            float voor = (float) Math.toRadians(75), achter = (float) Math.toRadians(-75);
            for (String leg : new String[]{"leg_front_left", "leg_front_right"}) {
                bones.apply(leg).ifPresent(b -> b.setRotX(Mth.lerp(glij, b.getRotX(), voor)));
            }
            for (String leg : new String[]{"leg_back_left", "leg_back_right"}) {
                bones.apply(leg).ifPresent(b -> b.setRotX(Mth.lerp(glij, b.getRotX(), achter)));
            }
        }
        float walk = Math.min(1f, guh.walkAnimation.speed(partialTick) * 2.2f) * (1f - glij);
        float flap = walk * Mth.sin(guh.walkAnimation.position(partialTick) * 1.8f) * 0.35f + glij * 1.05f;
        for (String vleugel : new String[]{"pinguh_vleugel_links", "pinguh_vleugel_rechts"}) {
            // (outward is towards the side the flipper sits on, whatever way the model is mirrored)
            bones.apply(vleugel).ifPresent(b -> b.setRotZ(Math.signum(b.getPivotX()) * Math.abs(flap) + b.getInitialSnapshot().getRotZ()));
        }
    }
}
