package nl.juiced.guhs.feature.vadsrijk.wezens.draak;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;

/**
 * The flyovers of Vahoegnir (API for the npc slice's opening cutscene and for the far circles of wereld/kern):
 * <pre>
 *   DraakVlucht.vlieg(level, List.of(a, b, c, d), 200);          // a smooth spline through the points in 10 s, roars on the way
 *   DraakVlucht.vlieg(level, pad, ticks, false);                  // the same without the roar
 *   DraakVlucht.cirkel(level, midden, 120, 150, 600);             // one slow circle (radius 120, at y 150) in 30 s
 *   DraakVlucht.brul(draak, true);                                // an extra roar heard by every player in the level
 * </pre>
 * The path is a Catmull-Rom spline through all the points (start and end included), with uniform time per segment; the dragon
 * faces along the path and leans into climbs/dives. Keep the path within ~150 blocks of the watching players: the server only
 * tracks/ticks entities in loaded chunks (it is moved from here every tick anyway, and it is never saved).
 */
public final class DraakVlucht {
    private record Vlucht(Vahoegnir draak, List<Vec3> pad, int duur, long start, boolean brult, boolean[] gebruld) {
    }

    private static final List<Vlucht> ACTIEF = new ArrayList<>();

    private DraakVlucht() {
    }

    /** A flyover along this path in {@code ticks} ticks, with a roar at about a third of the way. */
    @Nullable
    public static Vahoegnir vlieg(ServerLevel level, List<Vec3> pad, int ticks) {
        return vlieg(level, pad, ticks, true);
    }

    @Nullable
    public static Vahoegnir vlieg(ServerLevel level, List<Vec3> pad, int ticks, boolean brul) {
        if (pad.size() < 2 || ticks <= 0) {
            return null;
        }
        Vahoegnir d = WezensRegistry.DRAAK.get().create(level, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        List<Vec3> p = List.copyOf(pad);
        Vec3 a = punt(p, 0), b = punt(p, 0.01);
        d.snapTo(a.x, a.y, a.z, yaw(b.subtract(a)), 0);
        level.addFreshEntity(d);
        ACTIEF.add(new Vlucht(d, p, ticks, level.getGameTime(), brul, new boolean[1]));
        level.playSound(null, a.x, a.y, a.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 6f, 0.6f);
        return d;
    }

    /** One circle around {@code midden} (radius, height y) in {@code ticks} ticks, starting at a random angle. */
    @Nullable
    public static Vahoegnir cirkel(ServerLevel level, Vec3 midden, double straal, double hoogte, int ticks) {
        List<Vec3> pad = new ArrayList<>();
        double begin = level.getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; i <= 16; i++) {
            double a = begin + i * Math.PI * 2 / 16;
            pad.add(new Vec3(midden.x + Math.cos(a) * straal, hoogte + Math.sin(i * 0.9) * 6, midden.z + Math.sin(a) * straal));
        }
        return vlieg(level, pad, ticks, true);
    }

    /** The roar: the jaw opens, a deep growl (loud; {@code overal} = every player in the level hears it, wherever they are). */
    public static void brul(Vahoegnir d, boolean overal) {
        if (!(d.level() instanceof ServerLevel sl)) {
            return;
        }
        d.triggerAnim("brul", "brul");
        sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 10f, 0.72f);
        if (overal) {
            for (ServerPlayer p : sl.players()) {
                if (p.distanceTo(d) > 120) {
                    Vec3 r = d.position().subtract(p.position()).normalize().scale(16);
                    sl.playSound(null, p.getX() + r.x, p.getY() + r.y, p.getZ() + r.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.2f, 0.72f);
                }
            }
        }
    }

    /** Every server tick (WezensRegistry). */
    public static void tick() {
        Iterator<Vlucht> it = ACTIEF.iterator();
        while (it.hasNext()) {
            Vlucht v = it.next();
            Vahoegnir d = v.draak();
            if (d.isRemoved() || !(d.level() instanceof ServerLevel sl)) {
                it.remove();
                continue;
            }
            long t = sl.getGameTime() - v.start();
            if (t > v.duur()) {
                d.discard();
                it.remove();
                continue;
            }
            double f = t / (double) v.duur();
            Vec3 pos = punt(v.pad(), f), verder = punt(v.pad(), Math.min(1, f + 0.004));
            Vec3 r = verder.subtract(pos);
            if (r.lengthSqr() < 1.0E-6) {
                r = pos.subtract(punt(v.pad(), Math.max(0, f - 0.004)));
            }
            float yaw = yaw(r);
            float pitch = (float) Math.toDegrees(-Math.atan2(r.y, Math.sqrt(r.x * r.x + r.z * r.z)));
            d.snapTo(pos.x, pos.y, pos.z, yaw, Math.max(-35, Math.min(35, pitch)));
            if (v.brult() && !v.gebruld()[0] && f > 0.33) {
                v.gebruld()[0] = true;
                brul(d, false);
            }
            if (t % 32 == 0 && d.klimt()) {
                sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 5f, 0.7f);
            }
        }
    }

    public static int actief() {
        return ACTIEF.size();
    }

    static float yaw(Vec3 r) {
        return (float) Math.toDegrees(Math.atan2(-r.x, r.z));
    }

    /** The point at f (0..1) on the Catmull-Rom spline through all the points (uniform per segment). */
    public static Vec3 punt(List<Vec3> p, double f) {
        int n = p.size() - 1;
        double s = Math.max(0, Math.min(1, f)) * n;
        int i = Math.min(n - 1, (int) Math.floor(s));
        double t = s - i;
        Vec3 p0 = p.get(Math.max(0, i - 1)), p1 = p.get(i), p2 = p.get(i + 1), p3 = p.get(Math.min(n, i + 2));
        double t2 = t * t, t3 = t2 * t;
        return new Vec3(cr(p0.x, p1.x, p2.x, p3.x, t, t2, t3), cr(p0.y, p1.y, p2.y, p3.y, t, t2, t3), cr(p0.z, p1.z, p2.z, p3.z, t, t2, t3));
    }

    private static double cr(double a, double b, double c, double d, double t, double t2, double t3) {
        return 0.5 * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3);
    }
}
