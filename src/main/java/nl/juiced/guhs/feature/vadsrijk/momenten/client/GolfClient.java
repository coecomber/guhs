package nl.juiced.guhs.feature.vadsrijk.momenten.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.momenten.GolfBaan;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.feature.vadsrijk.wereld.client.WereldClient;

/**
 * The recolour wave on this client. While it waits ({@link #wacht}: the flag is set but the cliffhanger still plays) every scar
 * stays grey; while it runs ({@link #start}) a scar is healed once the wave front ({@link GolfBaan#straal}) has passed it. The
 * scar tint lives in the wereld slice's WereldClient (hook {@code WereldClient.golf}); the chunk sections the front crosses are
 * re-meshed as it goes. Near the camera the front is a rolling wall of pink dust, petals and sparkles; creatures it passes cheer
 * (hearts, notes, a squeak).
 */
public final class GolfClient {
    @Nullable
    private static GolfBaan baan;
    private static int t;
    private static int wachtTot;
    private static volatile double straal = -1;
    private static double vorig;
    private static boolean langsSpeler;
    private static final RandomSource R = RandomSource.create();
    private static final int ROZE = 0xFF8FC8, LICHTROZE = 0xFFC8E4, WIT = 0xFFF4FA;

    private GolfClient() {
    }

    static void wacht(int ticks) {
        if (baan != null) {
            return;
        }
        wachtTot = ticks;
        straal = -1;
        WereldClient.golf = pos -> false;
        alles();
    }

    static void start(GolfBaan b) {
        baan = b;
        t = 0;
        wachtTot = 0;
        vorig = 0;
        straal = 0;
        langsSpeler = false;
        WereldClient.golf = GolfClient::heel;
        alles();
    }

    static void stop(boolean herteken) {
        boolean was = baan != null || wachtTot > 0;
        baan = null;
        wachtTot = 0;
        straal = -1;
        WereldClient.golf = null;
        if (was && herteken) {
            alles();
        }
    }

    private static boolean heel(net.minecraft.core.BlockPos pos) {
        GolfBaan b = baan;
        double r = straal;
        if (b == null || r <= 0) {
            return false;
        }
        double dx = pos.getX() + 0.5 - b.tx(), dz = pos.getZ() + 0.5 - b.tz();
        return dx * dx + dz * dz < r * r;
    }

    private static void alles() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null && mc.level != null) {
            mc.levelRenderer.allChanged();
        }
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (baan == null) {
            if (wachtTot > 0 && --wachtTot == 0) {
                stop(true);   // (the wave never came: back to the plain flag)
            }
            return;
        }
        GolfBaan b = baan;
        t++;
        double r = b.straal(t);
        straal = r;
        vuil(mc, b, vorig, r);
        golfwand(mc, b, r);
        juichen(mc, b, vorig, r);
        if (!langsSpeler && r >= b.afstand()) {
            langsSpeler = true;
            Vec3 p = mc.player.position();
            mc.level.playLocalSound(p.x, p.y, p.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 1.2f, 1.2f, false);
            mc.level.playLocalSound(p.x, p.y, p.z, SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 0.8f, 1.6f, false);
            mc.level.playLocalSound(p.x, p.y, p.z, ModSounds.GUH_HAPPY.get(), SoundSource.AMBIENT, 1f, 1.1f, false);
        }
        vorig = r;
        if (t >= GolfBaan.DUUR + 5) {
            // the front is past the zone edge: every scar is healed, and the player's flag says so too (no rebuild needed)
            baan = null;
            straal = -1;
            WereldClient.golf = null;
        }
    }

    /** Re-mesh the chunk sections the front crossed this tick (at their near edge, middle and far edge). */
    private static void vuil(Minecraft mc, GolfBaan b, double r0, double r1) {
        if (r1 <= r0) {
            return;
        }
        int view = mc.options.getEffectiveRenderDistance() + 1;
        int pcx = mc.player.chunkPosition().x(), pcz = mc.player.chunkPosition().z();
        int minS = mc.level.getMinSectionY(), maxS = mc.level.getMaxSectionY();
        int s0 = Math.max(minS, 2), s1 = Math.min(maxS, 13);   // the scars lie between y 32 and 223
        for (int cx = pcx - view; cx <= pcx + view; cx++) {
            for (int cz = pcz - view; cz <= pcz + view; cz++) {
                double x0 = cx * 16 - b.tx(), x1 = x0 + 16, z0 = cz * 16 - b.tz(), z1 = z0 + 16;
                double nx = x0 > 0 ? x0 : x1 < 0 ? -x1 : 0, nz = z0 > 0 ? z0 : z1 < 0 ? -z1 : 0;
                double fx = Math.max(Math.abs(x0), Math.abs(x1)), fz = Math.max(Math.abs(z0), Math.abs(z1));
                double dmin = Math.sqrt(nx * nx + nz * nz), dmax = Math.sqrt(fx * fx + fz * fz), dmid = (dmin + dmax) / 2;
                if (kruist(r0, r1, dmin) || kruist(r0, r1, dmid) || kruist(r0, r1, dmax)) {
                    for (int sy = s0; sy <= s1; sy++) {
                        mc.levelRenderer.setSectionDirty(cx, sy, cz);
                    }
                }
            }
        }
    }

    private static boolean kruist(double r0, double r1, double d) {
        return r0 < d && d <= r1;
    }

    /** The rolling wall near the camera: pink dust rising, cherry petals, a few sparkles and hearts on the crest. */
    private static void golfwand(Minecraft mc, GolfBaan b, double r) {
        ClientLevel lvl = mc.level;
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        double cx = cam.x - b.tx(), cz = cam.z - b.tz(), dc = Math.sqrt(cx * cx + cz * cz);
        double bereik = 140;
        if (r < 1 || Math.abs(dc - r) > bereik) {
            return;
        }
        double hoek = Math.atan2(cz, cx), breed = Math.min(Math.PI, bereik / Math.max(r, 1) * 1.2);
        int n = 70;
        for (int i = 0; i < n; i++) {
            double a = hoek + (R.nextDouble() * 2 - 1) * breed;
            double x = b.tx() + Math.cos(a) * (r + (R.nextDouble() - 0.7) * 3), z = b.tz() + Math.sin(a) * (r + (R.nextDouble() - 0.7) * 3);
            double ddx = x - cam.x, ddz = z - cam.z;
            if (ddx * ddx + ddz * ddz > bereik * bereik) {
                continue;
            }
            int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            if (y <= lvl.getMinY() + 1) {
                continue;
            }
            double hy = y + R.nextDouble() * 4.5;
            double vx = Math.cos(a) * 0.08, vz = Math.sin(a) * 0.08;
            int k = R.nextInt(3) == 0 ? LICHTROZE : R.nextInt(5) == 0 ? WIT : ROZE;
            lvl.addAlwaysVisibleParticle(new DustParticleOptions(k, 2.6f + R.nextFloat() * 1.4f), true, x, hy, z, vx, 0.06, vz);
            if (R.nextInt(3) == 0) {
                lvl.addAlwaysVisibleParticle(ParticleTypes.CHERRY_LEAVES, true, x, hy + 1.5, z, vx, 0.02, vz);
            }
            if (R.nextInt(9) == 0) {
                lvl.addAlwaysVisibleParticle(ParticleTypes.END_ROD, true, x, y + 4 + R.nextDouble() * 2, z, vx * 0.5, 0.05, vz * 0.5);
            }
            if (R.nextInt(25) == 0) {
                lvl.addAlwaysVisibleParticle(ParticleTypes.HEART, true, x, y + 5, z, 0, 0.05, 0);
            }
        }
    }

    /** Creatures and guhs the front passes this tick cheer: hearts, notes and a happy squeak. */
    private static void juichen(Minecraft mc, GolfBaan b, double r0, double r1) {
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        int geluiden = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity) || e instanceof Player || e.distanceToSqr(cam) > 110 * 110) {
                continue;
            }
            double dx = e.getX() - b.tx(), dz = e.getZ() - b.tz(), d = Math.sqrt(dx * dx + dz * dz);
            if (!kruist(r0, r1, d)) {
                continue;
            }
            double top = e.getY() + e.getBbHeight();
            for (int i = 0; i < 4; i++) {
                mc.level.addAlwaysVisibleParticle(ParticleTypes.HEART, true, e.getX() + (R.nextDouble() - 0.5), top + 0.3 + R.nextDouble() * 0.5,
                        e.getZ() + (R.nextDouble() - 0.5), 0, 0.08, 0);
            }
            mc.level.addAlwaysVisibleParticle(ParticleTypes.NOTE, true, e.getX(), top + 0.9, e.getZ(), R.nextDouble(), 0, 0);
            mc.level.addAlwaysVisibleParticle(ParticleTypes.TOTEM_OF_UNDYING, true, e.getX(), top, e.getZ(), 0, 0.3, 0);
            if (geluiden++ < 4) {
                if (e instanceof RijkGuh) {
                    mc.level.playLocalSound(e.getX(), e.getY(), e.getZ(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.9f,
                            1.0f + R.nextFloat() * 0.3f, false);
                } else if (e instanceof VadsrijkMob) {
                    mc.level.playLocalSound(e.getX(), e.getY(), e.getZ(), SoundEvents.SLIME_JUMP_SMALL, SoundSource.NEUTRAL, 1f, 1.8f, false);
                    mc.level.playLocalSound(e.getX(), e.getY(), e.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 1.6f, false);
                } else {
                    mc.level.playLocalSound(e.getX(), e.getY(), e.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.6f, 1.8f, false);
                }
            }
        }
    }
}
