package nl.juiced.guhs.feature.oudescenes;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * bbq2 (oude-scenes): what a scene shows besides the verhaal engine's own script, drawn by this slice's client
 * (client.OudeScenesClient) for the viewer only: nobody else gets snow, rain, night or lightning from somebody's scene.
 * <ul>
 *   <li>{@code weer}: keyframes {tick, zicht, sneeuw, regen, donder, nacht}, linear in between. zicht = how many blocks far
 *       you still see in the white (0: no fog of ours); sneeuw, regen, donder 0..1; nacht above 0.5 = midnight, full moon;</li>
 *   <li>{@code flitsen}: the ticks at which the sky flashes (lightning);</li>
 *   <li>{@code stromen}: emitters of particles, see {@link Stroom}.</li>
 * </ul>
 * The numbers come from tools/features/oude_scenes_scene.py ({@code weer_op_tick}, {@code flits}, {@code stroom}).
 */
public record Effecten(float[][] weer, int[] flitsen, List<Stroom> stromen) {
    public static final int ZICHT = 1, SNEEUW = 2, REGEN = 3, DONDER = 4, NACHT = 5;

    /**
     * From tick {@code van} up to {@code tot}: {@code perTick} particles every tick at a point that glides from
     * {@code begin} to {@code eind} (relative to the scene's anchor, template coordinates), scattered {@code spreiding}
     * blocks around it, flying off with {@code snelheid} (blocks per tick, in template directions).
     */
    public record Stroom(int van, int tot, Supplier<? extends ParticleOptions> deeltje, Vec3 begin, Vec3 eind, float perTick, double spreiding, Vec3 snelheid) {
        public Vec3 op(double t) {
            return begin.lerp(eind, Mth.clamp((t - van) / Math.max(1.0, tot - van), 0.0, 1.0));
        }
    }

    /** The value of a channel ({@link #ZICHT} ...) at tick t: linear between the keyframes, the nearest one outside them. */
    public float kanaal(double t, int kanaal) {
        if (weer.length == 0) {
            return 0f;
        }
        if (t <= weer[0][0]) {
            return weer[0][kanaal];
        }
        for (int i = 0; i + 1 < weer.length; i++) {
            float[] a = weer[i], b = weer[i + 1];
            if (t < b[0]) {
                return Mth.lerp((float) ((t - a[0]) / Math.max(1f, b[0] - a[0])), a[kanaal], b[kanaal]);
            }
        }
        return weer[weer.length - 1][kanaal];
    }

    /** Does the scene use this channel at all? */
    public boolean heeft(int kanaal) {
        for (float[] rij : weer) {
            if (rij[kanaal] > 0f) {
                return true;
            }
        }
        return false;
    }
}
