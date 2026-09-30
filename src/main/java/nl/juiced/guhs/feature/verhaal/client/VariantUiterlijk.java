package nl.juiced.guhs.feature.verhaal.client;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * 3.0 (Guhverhalen), client only: the look of one guh variant, hooked into GuhRenderer so the owner slice never edits it.
 * Register from the owner's {@code client.<Name>Client.init} with {@link #zet}.
 * <ul>
 *   <li>{@link Uiterlijk#texture}: another fur texture (null: the variant's own {@code guh_<id>.png}; sleeping eyes are added
 *       by the renderer when {@code guh_slaap/<same>} exists);</li>
 *   <li>{@link Uiterlijk#glow}: an emissive layer (full bright, like the starry guh's stars), e.g. Guhtwo's purple glow;</li>
 *   <li>{@link Uiterlijk#botten}: move/rotate bones every frame (after the animations), e.g. a tail that sways;</li>
 *   <li>{@link Uiterlijk#extra}: draw extra things after the guh (entity space, feet at 0,0,0; not scaled), e.g. the 626's
 *       extra arms holding things, particles.</li>
 * </ul>
 */
public final class VariantUiterlijk {
    public interface Uiterlijk {
        @Nullable
        default ResourceLocation texture(GuhEntity guh) {
            return null;
        }

        @Nullable
        default ResourceLocation glow(GuhEntity guh) {
            return null;
        }

        default void botten(GuhEntity guh, Function<String, Optional<GeoBone>> bot, float partialTick) {
        }

        default void extra(GuhEntity guh, PoseStack pose, MultiBufferSource buffers, int light, float partialTick) {
        }
    }

    private static final Map<GuhVariant, Uiterlijk> UITERLIJK = java.util.Collections.synchronizedMap(new EnumMap<>(GuhVariant.class));

    public static void zet(GuhVariant v, Uiterlijk u) {
        UITERLIJK.put(v, u);
    }

    @Nullable
    public static Uiterlijk van(GuhVariant v) {
        return UITERLIJK.get(v);
    }

    // --- called by GuhRenderer ---------------------------------------------------------------------------------------

    @Nullable
    public static ResourceLocation texture(GuhEntity guh) {
        Uiterlijk u = UITERLIJK.get(guh.getVariant());
        return u == null ? null : u.texture(guh);
    }

    @Nullable
    public static ResourceLocation glow(GuhEntity guh) {
        Uiterlijk u = UITERLIJK.get(guh.getVariant());
        return u == null ? null : u.glow(guh);
    }

    public static void botten(GuhEntity guh, Function<String, Optional<GeoBone>> bot, float partialTick) {
        Uiterlijk u = UITERLIJK.get(guh.getVariant());
        if (u != null) {
            u.botten(guh, bot, partialTick);
        }
    }

    public static void extra(GuhEntity guh, PoseStack pose, MultiBufferSource buffers, int light, float partialTick) {
        Uiterlijk u = UITERLIJK.get(guh.getVariant());
        if (u != null) {
            pose.pushPose();
            u.extra(guh, pose, buffers, light, partialTick);
            pose.popPose();
        }
    }

    private VariantUiterlijk() {
    }
}
