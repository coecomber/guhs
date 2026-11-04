package nl.juiced.guhs.feature.vadsrijk.zone3.wereld;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The upward blossom-wind columns of zone 3 (user decision round 18: "wel windkolommen"). Inside a column (radius r round
 * (x, z), from y0 up to the landing's stand y1) a player - or the mount he rides - is lifted (a strong short levitation, so the
 * client moves smoothly, no fall damage); at the top the wind sets him down sideways onto the landing (lx, lz) with slow
 * falling. Only up: going down is the Pluimzweef (gliding), never a column.
 */
public final class Z3Wind {
    /** Levitation amplifier: (amp + 1) * 0.05 blocks/tick upward, so ~18 blocks/s. */
    static final int KRACHT = 17;

    private Z3Wind() {
    }

    static void tick(ServerPlayer p, ServerLevel sl, long tijd) {
        Entity e = p.getVehicle() != null ? p.getVehicle() : p;
        for (Z3WereldPlekken.Windkolom w : Z3WereldPlekken.WINDKOLOMMEN) {
            double dx = e.getX() - (w.x() + 0.5), dz = e.getZ() - (w.z() + 0.5);
            double r = w.r() + 0.6;
            if (dx * dx + dz * dz > r * r || e.getY() < w.y0() - 1.5 || e.getY() > w.y1() + 6) {
                continue;
            }
            if (e.getY() < w.y1() + 1.2) {
                til(p, KRACHT);
                if (e != p && e instanceof LivingEntity le) {
                    til(le, KRACHT);
                }
                p.resetFallDistance();
                e.resetFallDistance();
                if (tijd % 10 == 0) {
                    sl.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.AZALEA_LEAVES_HIT, SoundSource.AMBIENT, 0.6f, 1.4f);
                }
            } else {
                // the top: sideways onto the landing, gently
                Vec3 d = new Vec3(w.lx() + 0.5 - e.getX(), 0, w.lz() + 0.5 - e.getZ());
                if (d.lengthSqr() > 1e-4) {
                    d = d.normalize().scale(0.42);
                }
                e.setDeltaMovement(d.x, 0.12, d.z);
                e.hurtMarked = true;
                p.removeEffect(MobEffects.LEVITATION);
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0, true, false, false));
                if (e != p && e instanceof LivingEntity le) {
                    le.removeEffect(MobEffects.LEVITATION);
                    le.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0, true, false, false));
                }
                p.resetFallDistance();
                e.resetFallDistance();
            }
            return;
        }
    }

    private static void til(LivingEntity e, int kracht) {
        MobEffectInstance oud = e.getEffect(MobEffects.LEVITATION);
        if (oud == null || oud.getDuration() < 4) {
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 8, kracht, true, false, false));
        }
    }

    /** Petals blowing up every column near a player (server particles, seen by everyone nearby). */
    static void deeltjes(ServerLevel sl) {
        for (Z3WereldPlekken.Windkolom w : Z3WereldPlekken.WINDKOLOMMEN) {
            double cx = w.x() + 0.5, cz = w.z() + 0.5;
            boolean dichtbij = false;
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(cx, p.getY(), cz) < 96 * 96) {
                    dichtbij = true;
                    break;
                }
            }
            if (!dichtbij) {
                continue;
            }
            int h = w.y1() - w.y0();
            for (int k = 0; k < 6; k++) {
                double y = w.y0() + sl.getRandom().nextDouble() * h;
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, cx, y, cz, 3, w.r() * 0.5, 2, w.r() * 0.5, 0.0);
                sl.sendParticles(ParticleTypes.GLOW, cx, y, cz, 1, w.r() * 0.4, 2, w.r() * 0.4, 0.0);
            }
            sl.sendParticles(ParticleTypes.CLOUD, cx, w.y0() + 0.3, cz, 2, w.r() * 0.5, 0.1, w.r() * 0.5, 0.06);
        }
    }
}
