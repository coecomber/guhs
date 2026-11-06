package nl.juiced.guhs.feature.vadsrijk.zone6.wezens;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx;
import nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vfx;

/**
 * Zone-6 telegraphs and effects in ONE place: kern's {@link Telegraaf} (ground decals of the VFX kit: red = damage, pink = safe,
 * purple = mechanic, yellow = stack), the kit's own particles/meshes ({@link Vfx}) and the boss one-liners of {@link BaasVfx}
 * (wind-up pose, cast bar {@code vadsrijk.vfx.aanval.<id>}, hit pop + shockwave). Zijdeland colours: rose sand, saffron gold,
 * turquoise, star-white; grey only for the star-salt crust.
 */
public final class Z6Vfx {
    public static final int ROOD = Telegraaf.ROOD, ROZE = Telegraaf.ROZE, PAARS = Telegraaf.PAARS, GEEL = Telegraaf.GEEL, GRIJS = Telegraaf.GRIJS;
    public static final int ZAND = 0xF2B8B0, SAFFRAAN = 0xFFC040, TURKOOIS = 0x5AD8D0, STER = 0xFFF4C0, ZOUT = 0xC8C4D4;

    private Z6Vfx() {
    }

    public static void cirkel(ServerLevel sl, Vec3 c, double r, int ticks, int kleur) {
        Telegraaf.cirkel(sl, c, r, ticks, kleur);
    }

    public static void lijn(ServerLevel sl, Vec3 a, Vec3 b, double breed, int ticks, int kleur) {
        Telegraaf.lijn(sl, a, b, breed, ticks, kleur);
    }

    /** A cone from {@code punt} in direction {@code dir} (flat), {@code graden} wide, r long. */
    public static void kegel(ServerLevel sl, Vec3 punt, Vec3 dir, double r, double graden, int ticks, int kleur) {
        Telegraaf.kegel(sl, punt, Math.atan2(dir.z, dir.x), r, graden, ticks, kleur);
    }

    public static void donut(ServerLevel sl, Vec3 c, double binnen, double buiten, int ticks, int kleur) {
        Telegraaf.donut(sl, c, binnen, buiten, ticks, kleur);
    }

    public static void veilig(ServerLevel sl, Vec3 c, double r, int ticks) {
        Telegraaf.veilig(sl, c, r, ticks);
    }

    // ---- the boss one-liners (t-vfx's BaasVfx) -------------------------------------------------------------------------------

    public static void windup(VadsrijkBaas b, int ticks) {
        BaasVfx.windup(b, ticks);
    }

    /** The cast bar under the boss bar: "&lt;baas&gt; laadt: &lt;aanval&gt;" (lang vadsrijk.vfx.aanval.&lt;id&gt;). */
    public static void cast(VadsrijkBaas b, String id, int ticks) {
        BaasVfx.cast(b, id, ticks);
    }

    public static void treffer(VadsrijkBaas b, Vec3 waar, double r, int kleur) {
        BaasVfx.treffer(b, waar, r, kleur);
    }

    // ---- particles ---------------------------------------------------------------------------------------------------------------

    /** A puff of rose sand (a creature digs in / jumps out). */
    public static void zand(ServerLevel sl, Vec3 v, double r) {
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), v.x, v.y + 0.2, v.z, (int) (14 * r), r * 0.5, 0.2, r * 0.5, 0.1);
        sl.sendParticles(new DustParticleOptions(ZAND, 1.4f), v.x, v.y + 0.3, v.z, (int) (10 * r), r * 0.5, 0.3, r * 0.5, 0);
    }

    /** Star glitter (sterrenstof, the star lens, a beacon). */
    public static void glinster(ServerLevel sl, Vec3 v, double r) {
        Vfx.deeltjes(sl, Deeltje.STER, v, (int) (10 + 8 * r), STER, r * 0.6, 0.06);
        sl.sendParticles(ParticleTypes.END_ROD, v.x, v.y, v.z, (int) (4 + 4 * r), r * 0.4, r * 0.4, r * 0.4, 0.02);
    }

    /** The grey star-salt crust flaking off a cheered-up creature. */
    public static void zoutKorst(ServerLevel sl, Vec3 v, double w) {
        Vfx.deeltjes(sl, Deeltje.KRISTAL, v, (int) (8 + 6 * w), ZOUT, w * 0.5, 0.08);
    }

    /** Pink-saffron cheer-up confetti (bosses). */
    public static void feest(ServerLevel sl, Vec3 v, double r) {
        Vfx.deeltjes(sl, Deeltje.CONFETTI, v, (int) (20 * r), 0xFF8AD0, r * 0.6, 0.1);
        Vfx.deeltjes(sl, Deeltje.CONFETTI, v, (int) (12 * r), SAFFRAAN, r * 0.6, 0.1);
        Vfx.deeltjes(sl, Deeltje.HART, v.add(0, r * 0.4, 0), (int) (3 + 2 * r), 0xFF7AC8, r * 0.4, 0.04);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, v.x, v.y, v.z, (int) (10 * r), r * 0.5, r * 0.5, r * 0.5, 0.05);
    }

    /** A shockwave ring on the ground. */
    public static void golf(ServerLevel sl, Vec3 v, double r, int kleur) {
        Vfx.golf(sl, v, r, kleur, 14);
    }

    /** A light pillar (a lit beacon / a telescope beam end). */
    public static void zuil(ServerLevel sl, Vec3 v, double h, int kleur, int ticks) {
        Vfx.zuil(sl, v, h, kleur, ticks);
    }

    /** A beam (the telescopes, the Ringdrager's light jar). */
    public static void straal(ServerLevel sl, Vec3 a, Vec3 b, double breed, int kleur, int ticks) {
        Vfx.straal(sl, a, b, breed, kleur, ticks);
    }
}
