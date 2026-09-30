package nl.juiced.guhs.feature.verhaal.client;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;

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
 * 1.1.0 (GeckoLib 5): every method runs at <b>extract</b> time (the guh is there, the model is not). {@code botten} and
 * {@code extra} read the guh, compute their values and hand the render-time work to the frame:
 * {@code frame.bones(bones -> bones.ifPresent("tail", b -> b.setRotY(b.getRotY() + sway)))} (was {@code bot.apply("tail")}) and
 * {@code frame.extra((pose, collector, light) -> item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0))}
 * (was drawing with the pose and buffers; make item states now with {@code GuhRenderer.itemState}). Never capture the guh
 * itself in those lambdas. Bone rotations are relative to the model's own rotation (see MIGRATION_NOTES 4.6b).
 */
public final class VariantUiterlijk {
    public interface Uiterlijk {
        @Nullable
        default Identifier texture(GuhEntity guh) {
            return null;
        }

        @Nullable
        default Identifier glow(GuhEntity guh) {
            return null;
        }

        /** Bone moves for this frame: call {@code frame.bones(..)} with values computed now. */
        default void botten(GuhEntity guh, GuhRenderFrame frame, float partialTick) {
        }

        /** Extra things for this frame: call {@code frame.extra(..)} (entity space, feet at 0,0,0, not rotated/scaled). */
        default void extra(GuhEntity guh, GuhRenderFrame frame, float partialTick) {
        }
    }

    /** Priority of a story variant's texture in the {@link GuhRenderFrame} (the Pinguh's own look wins with 20). */
    public static final int TEXTURE_PRIORITY = 10;

    private static final Map<GuhVariant, Uiterlijk> UITERLIJK = java.util.Collections.synchronizedMap(new EnumMap<>(GuhVariant.class));
    private static boolean hooked;

    public static void zet(GuhVariant v, Uiterlijk u) {
        UITERLIJK.put(v, u);
        hook();
    }

    @Nullable
    public static Uiterlijk van(GuhVariant v) {
        return UITERLIJK.get(v);
    }

    /** Hooks the looks into the GuhRenderer (once; also called from {@link VerhaalClient#init}). */
    static synchronized void hook() {
        if (hooked) {
            return;
        }
        hooked = true;
        GuhRenderer.hook(VariantUiterlijk::extract);
    }

    // --- called by GuhRenderer (extract time) ------------------------------------------------------------------------

    private static void extract(GuhEntity guh, float partialTick, GuhRenderFrame frame) {
        Uiterlijk u = UITERLIJK.get(frame.variant);
        if (u == null) {
            return;
        }
        Identifier tex = u.texture(guh);
        if (tex != null) {
            frame.texture(tex, TEXTURE_PRIORITY);
        }
        Identifier glow = u.glow(guh);
        if (glow != null) {
            frame.glow(glow);
        }
        u.botten(guh, frame, partialTick);
        u.extra(guh, frame, partialTick);
    }

    private VariantUiterlijk() {
    }
}
