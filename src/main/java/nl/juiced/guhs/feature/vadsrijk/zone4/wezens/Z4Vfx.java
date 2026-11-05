package nl.juiced.guhs.feature.vadsrijk.zone4.wezens;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;

/**
 * Zone-4 telegraphs and small effects in ONE place (creatures and bosses): kern's {@link Telegraaf} (red damage, pink safe,
 * purple mechanic, yellow stack; t-vfx turns every Telegraaf into a ground decal on its branch, so after the merge these are
 * decals without a change here) plus a few particle bursts. The boss one-liners {@link #windup}, {@link #cast} and
 * {@link #treffer} call t-vfx's {@code vfx.BaasVfx} BY REFLECTION: before the integration merge they do nothing, after it the
 * bosses get the wind-up pose, the cast bar ("vadsrijk.vfx.aanval.&lt;id&gt;") and the hit pop + shockwave.
 */
public final class Z4Vfx {
    public static final int ROOD = Telegraaf.ROOD, ROZE = Telegraaf.ROZE, PAARS = Telegraaf.PAARS, GEEL = Telegraaf.GEEL;
    /** Sea-foam blue (the Zeeslurf's water marks) and lantern gold. */
    public static final int ZEE = 0x7AD8F0, LAMPION = 0xFFC860;

    private Z4Vfx() {
    }

    private static boolean gezocht;
    private static java.lang.reflect.Method windup, cast, treffer;

    private static void zoek() {
        if (gezocht) {
            return;
        }
        gezocht = true;
        try {
            Class<?> c = Class.forName("nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx");
            Class<?> baas = nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas.class;
            windup = c.getMethod("windup", baas, int.class);
            cast = c.getMethod("cast", baas, String.class, int.class);
            treffer = c.getMethod("treffer", baas, Vec3.class, double.class, int.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            windup = cast = treffer = null;
        }
    }

    private static void roep(java.lang.reflect.Method m, Object... args) {
        if (m == null) {
            return;
        }
        try {
            m.invoke(null, args);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // (the kit changed its signature: no effect, the fight goes on)
        }
    }

    /** t-vfx: the boss crouches for its hit (held for {@code ticks}). */
    public static void windup(nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas b, int ticks) {
        zoek();
        roep(windup, b, ticks);
    }

    /** t-vfx: the cast bar under the boss bar ("&lt;baas&gt; laadt: &lt;aanval&gt;", lang vadsrijk.vfx.aanval.&lt;id&gt;). */
    public static void cast(nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas b, String id, int ticks) {
        zoek();
        roep(cast, b, id, ticks);
    }

    /** t-vfx: the hit (pop animation, a shockwave and a flash at the spot). */
    public static void treffer(nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas b, Vec3 waar, double r, int kleur) {
        zoek();
        roep(treffer, b, waar, r, kleur);
    }

    public static void cirkel(ServerLevel sl, Vec3 c, double r, int ticks, int kleur) {
        Telegraaf.cirkel(sl, c, r, ticks, kleur);
    }

    public static void lijn(ServerLevel sl, Vec3 a, Vec3 b, double breed, int ticks, int kleur) {
        Telegraaf.lijn(sl, a, b, breed, ticks, kleur);
    }

    /** A splash of sea water and foam. */
    public static void plons(ServerLevel sl, Vec3 v, double r) {
        sl.sendParticles(ParticleTypes.SPLASH, v.x, v.y + 0.3, v.z, (int) (20 * r), r * 0.5, 0.2, r * 0.5, 0.2);
        sl.sendParticles(ParticleTypes.BUBBLE_POP, v.x, v.y + 0.3, v.z, (int) (10 * r), r * 0.5, 0.3, r * 0.5, 0.02);
        sl.sendParticles(new DustParticleOptions(0xF2FAFF, 1.4f), v.x, v.y + 0.2, v.z, (int) (8 * r), r * 0.5, 0.1, r * 0.5, 0);
    }

    /** Pink cheer-up confetti (lantern colours). */
    public static void feest(ServerLevel sl, Vec3 v, double r) {
        for (int k : new int[] {0xFF8AD0, 0xFFC860, 0x9AF0C8}) {
            sl.sendParticles(new DustParticleOptions(k, 1.6f), v.x, v.y, v.z, (int) (14 * r), r * 0.6, r * 0.5, r * 0.6, 0);
        }
        sl.sendParticles(ParticleTypes.HEART, v.x, v.y + r * 0.5, v.z, (int) (3 + r * 2), r * 0.4, 0.3, r * 0.4, 0);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, v.x, v.y, v.z, (int) (10 * r), r * 0.5, r * 0.5, r * 0.5, 0.05);
    }

    /** A gust of wind (kites). */
    public static void wind(ServerLevel sl, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        for (double s = 0; s < 1; s += 0.1) {
            Vec3 p = a.add(d.scale(s));
            sl.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.8, p.z, 1, 0.3, 0.3, 0.3, 0.02);
        }
    }
}
