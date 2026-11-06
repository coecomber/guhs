package nl.juiced.guhs.feature.vadsrijk.zone5.wolkwal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Figurant;

/**
 * T-WOLKWAL: Moeder Wolkwal swims. The city and the whale stay static blocks (and the walkable fin/fluke cloud platforms of
 * t-z5wezens stay where they are); on top of that, only near players:
 * <ul>
 *   <li>the TIPS of both pectoral fins and both fluke lobes move: a non-colliding GeckoLib prop ({@link Z3Figurant}, no hitbox,
 *       never saved, 1 model pixel = 1 block, textured with the real whale-skin blocks) sits on the static tip and waves slowly
 *       (fins "wuif" 4 s, flukes "slag" 5 s; the outer segment lags behind). Spawned when a player is within {@link #ZICHT},
 *       gone again beyond {@link #WEG}: at most 4 entities;</li>
 *   <li>cloud wisps trail behind the tips (she swims west, so they drift east);</li>
 *   <li>every {@link #SPUIT_ELKE} ticks both blowholes on her head spout: cloud plumes rising along the static spout column, a
 *       puff at the top, droplets, a soft "pfffsh" ({@link #spuit}).</li>
 * </ul>
 * Particles only go to players within {@link #DEELTJES} blocks. The positions come from the static map
 * (tools/vadsrijk/zone5/kit/wolkwal.py: fin tip local (~47, ~150, +-190), fluke tip (~-300, ~237, +-125), blowholes (182, 216, +-9),
 * game = (2702 - x, y - 20, -520 - z)).
 */
public final class WolkwalBeweging {
    public static final int ZICHT = 192, WEG = 240, DEELTJES = 160, SPUIT_ELKE = 400, SPUIT_DUUR = 50;
    public static volatile boolean aan = true;

    /** A moving tip: id, model (wolkwal/...), animation, where it sits (the pivot on the static tip), which way it points. */
    public record Stuk(String id, String model, String anim, Vec3 pos, float yaw, double lengte) {
        /** Where the tip ends (for the wisps). */
        public Vec3 eind() {
            double a = Math.toRadians(yaw);
            return pos.add(-Math.sin(a) * lengte, -2, Math.cos(a) * lengte);
        }
    }

    public static final List<Stuk> STUKKEN = List.of(
            new Stuk("zuidvin", "vintip", "wuif", new Vec3(2652.0, 130.0, -709.0), -128f, 17),
            new Stuk("noordvin", "vintip", "wuif", new Vec3(2652.0, 130.0, -331.0), -52f, 17),
            new Stuk("staart_zuid", "staarttip", "slag", new Vec3(3001.0, 216.5, -645.0), -145f, 12),
            new Stuk("staart_noord", "staarttip", "slag", new Vec3(3001.0, 216.5, -395.0), -35f, 12));
    /** The two blowholes (the static spout column stands on them). */
    public static final List<Vec3> SPUITGATEN = List.of(new Vec3(2520.5, 216.0, -528.5), new Vec3(2520.5, 216.0, -510.5));
    /** Model scale: 1 model pixel = 1 block. */
    static final float SCHAAL = 16f;

    private static final Map<String, Z3Figurant> NU = new HashMap<>();
    private static long spuitTot = -1;

    private WolkwalBeweging() {
    }

    public static int aantal() {
        NU.values().removeIf(f -> f == null || f.isRemoved());
        return NU.size();
    }

    static void tick(ServerLevel sl) {
        long t = sl.getGameTime();
        List<ServerPlayer> spelers = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.getX() > 2300 && p.getX() < 3200 && p.getZ() > -900 && p.getZ() < -150) {
                spelers.add(p);
            }
        }
        if (!aan || spelers.isEmpty()) {
            if (!NU.isEmpty()) {
                wis();
            }
            return;
        }
        if (t % 20 == 0) {
            stukken(sl, spelers);
        }
        RandomSource r = sl.getRandom();
        if (t % 8 == 0) {
            for (Stuk s : STUKKEN) {
                Vec3 e = s.eind();
                for (ServerPlayer p : spelers) {
                    if (p.position().distanceToSqr(e) < DEELTJES * DEELTJES) {
                        // a wisp drifting east (she swims west), now and then a second smaller one
                        deeltje(sl, p, ParticleTypes.CLOUD, e.add(r.nextGaussian() * 1.5, r.nextGaussian(), r.nextGaussian() * 1.5), 0.14, 0.01, 0);
                        if (r.nextInt(3) == 0) {
                            deeltje(sl, p, ParticleTypes.WHITE_ASH, e.add(r.nextGaussian() * 3, r.nextGaussian(), r.nextGaussian() * 3), 0.1, 0, 0);
                        }
                    }
                }
            }
        }
        if (t % SPUIT_ELKE == 0) {
            spuitTot = t + SPUIT_DUUR;
            for (Vec3 g : SPUITGATEN) {
                sl.playSound(null, BlockPos.containing(g), SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 3f, 0.5f);
            }
        }
        if (t <= spuitTot && t % 2 == 0) {
            spuitTick(sl, spelers, (int) (SPUIT_DUUR - (spuitTot - t)), r);
        }
    }

    /** Spout now (dev command / test). */
    public static void spuit(ServerLevel sl) {
        spuitTot = sl.getGameTime() + SPUIT_DUUR;
        for (Vec3 g : SPUITGATEN) {
            sl.playSound(null, BlockPos.containing(g), SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 3f, 0.5f);
        }
    }

    public static boolean spuitNu(long t) {
        return t <= spuitTot;
    }

    private static void spuitTick(ServerLevel sl, List<ServerPlayer> spelers, int k, RandomSource r) {
        for (Vec3 g : SPUITGATEN) {
            List<ServerPlayer> wie = spelers.stream().filter(p -> p.position().distanceToSqr(g) < DEELTJES * DEELTJES).toList();
            if (wie.isEmpty()) {
                continue;
            }
            double kracht = k < 10 ? 1.0 : 1.0 - (k - 10) / 50.0;   // a strong burst, then it calms down
            for (ServerPlayer p : wie) {
                for (int i = 0; i < 10; i++) {
                    double a = r.nextDouble() * Math.PI * 2, rr = 2 + r.nextDouble() * 3;
                    Vec3 v = g.add(Math.cos(a) * rr, r.nextDouble() * 24, Math.sin(a) * rr);
                    deeltje(sl, p, ParticleTypes.CLOUD, v, Math.cos(a) * 0.08, (0.5 + r.nextDouble() * 0.5) * kracht, Math.sin(a) * 0.08);
                }
                if (k % 4 == 0) {
                    for (int i = 0; i < 14; i++) {
                        Vec3 v = g.add(r.nextGaussian() * 6, 30 + r.nextDouble() * 18, r.nextGaussian() * 6);
                        deeltje(sl, p, ParticleTypes.CLOUD, v, r.nextGaussian() * 0.12, 0.05, r.nextGaussian() * 0.12);
                    }
                }
                for (int i = 0; i < 3; i++) {
                    Vec3 v = g.add(r.nextGaussian() * 5, 10 + r.nextDouble() * 25, r.nextGaussian() * 5);
                    sl.sendParticles(p, ParticleTypes.FALLING_WATER, true, false, v.x, v.y, v.z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    private static void deeltje(ServerLevel sl, ServerPlayer p, ParticleOptions soort, Vec3 v, double vx, double vy, double vz) {
        // count 0: (vx, vy, vz) is the velocity
        sl.sendParticles(p, soort, true, false, v.x, v.y, v.z, 0, vx, vy, vz, 1.0);
    }

    private static void stukken(ServerLevel sl, List<ServerPlayer> spelers) {
        for (Stuk s : STUKKEN) {
            double best = Double.MAX_VALUE;
            for (ServerPlayer p : spelers) {
                best = Math.min(best, p.position().distanceToSqr(s.pos()));
            }
            Z3Figurant f = NU.get(s.id());
            boolean er = f != null && !f.isRemoved();
            if (!er && best < ZICHT * ZICHT && sl.isLoaded(BlockPos.containing(s.pos()))) {
                Z3Figurant n = Z3Figurant.maak(sl, s.pos(), s.yaw(), "vadsrijk/zone5/wolkwal/" + s.model(),
                        "textures/vadsrijk/zone5/wolkwal/" + s.model() + ".png", "vadsrijk/zone5/wolkwal/wolkwal", s.anim(), SCHAAL);
                if (n != null) {
                    NU.put(s.id(), n);
                }
            } else if (er && best > WEG * WEG) {
                f.discard();
                NU.remove(s.id());
            }
        }
    }

    public static void wis() {
        for (Z3Figurant f : NU.values()) {
            if (f != null && !f.isRemoved()) {
                f.discard();
            }
        }
        NU.clear();
    }
}
