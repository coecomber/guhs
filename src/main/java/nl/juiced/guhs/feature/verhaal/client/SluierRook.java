package nl.juiced.guhs.feature.verhaal.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.VerhaalPayloads;

/**
 * bbq2 (verhaal engine): the smoke of Guhdalfs sluier ({@link Sluiers}), for the viewer only. The server tells which walls
 * stand near the player and are still closed for them ({@code guhs:verhaal_sluiers}); here thick smoke swirls up along
 * those walls within {@link #ZICHT} blocks (always drawn, also with particles on "minimal": it is what tells you where
 * you can't go yet). A wall that leaves the list (the sluier opened for this player) dissolves in a puff.
 */
public final class SluierRook {
    /** How far from the viewer the wall is drawn (blocks). */
    public static final double ZICHT = 40;
    /** Smoke clouds per block of wall per tick (each is some three blocks wide and lives about five seconds: they overlap into a wall). */
    private static final double DICHTHEID = 0.2;
    /** How far below and above the viewer's eyes the wall is drawn at most (it is as high as the sluier's box). */
    private static final double ONDER = 22, HOOG = 26;
    /** Smoke clouds per square block of the lid per tick (only drawn for a viewer who could look in over the wall). */
    private static final double DEKSEL = 0.008;

    private static List<Sluiers.Zone> zones = List.of();
    /** Walls that just opened: {zone, ticks left of the puff}. */
    private static final List<Object[]> LOST = new ArrayList<>();

    private SluierRook() {
    }

    /**
     * One cloud of the wall ({@code guhs:verhaal_sluierrook}): a big, slowly turning puff of grey-purple smoke that fades in,
     * drifts and fades out. Big on purpose: a few hundred of them make a wall you can't see through.
     */
    static final class Wolk extends net.minecraft.client.particle.SingleQuadParticle {
        private final float draaiSnelheid, sterkte;

        Wolk(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
            this.xd = dx;
            this.yd = dy;
            this.zd = dz;
            this.lifetime = 80 + random.nextInt(50);
            this.quadSize = 1.5f + random.nextFloat() * 0.9f;
            this.gravity = 0f;
            this.friction = 1f;
            this.hasPhysics = false;
            this.roll = random.nextFloat() * (float) (Math.PI * 2);
            this.oRoll = roll;
            this.draaiSnelheid = (random.nextFloat() - 0.5f) * 0.03f;
            this.sterkte = 0.72f + random.nextFloat() * 0.2f;
            float grijs = 0.5f + random.nextFloat() * 0.22f;
            this.rCol = grijs * 0.98f;
            this.gCol = grijs * 0.93f;
            this.bCol = Math.min(1f, grijs * 1.08f);
            this.alpha = 0f;
        }

        @Override
        public void tick() {
            super.tick();
            oRoll = roll;
            roll += draaiSnelheid;
            float t = (float) age / lifetime;
            alpha = sterkte * (t < 0.15f ? t / 0.15f : t > 0.7f ? Math.max(0f, (1f - t) / 0.3f) : 1f);
        }

        @Override
        public net.minecraft.client.particle.SingleQuadParticle.Layer getLayer() {
            return net.minecraft.client.particle.SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** The walls drawn now (for checks). */
    public static List<Sluiers.Zone> zones() {
        return zones;
    }

    static void ontvang(VerhaalPayloads.Rook p) {
        List<Sluiers.Zone> nieuw = new ArrayList<>();
        for (Tag t : p.data().getListOrEmpty("Zones")) {
            if (t instanceof CompoundTag c) {
                nieuw.add(new Sluiers.Zone(c.getStringOr("S", ""), c.getIntOr("X0", 0), c.getIntOr("Y0", 0), c.getIntOr("Z0", 0), c.getIntOr("X1", 0),
                        c.getIntOr("Y1", 0), c.getIntOr("Z1", 0)));
            }
        }
        for (Sluiers.Zone oud : zones) {
            if (!nieuw.contains(oud)) {
                LOST.add(new Object[] {oud, 30});
            }
        }
        zones = List.copyOf(nieuw);
    }

    static void wis() {
        zones = List.of();
        LOST.clear();
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            wis();
            return;
        }
        if (mc.isPaused() || zones.isEmpty() && LOST.isEmpty()) {
            return;
        }
        Vec3 oog = mc.gameRenderer.getMainCamera().position();
        RandomSource r = level.getRandom();
        for (Sluiers.Zone z : zones) {
            muur(level, r, z, oog, false);
        }
        for (int i = LOST.size() - 1; i >= 0; i--) {
            Object[] l = LOST.get(i);
            muur(level, r, (Sluiers.Zone) l[0], oog, true);
            int over = (Integer) l[1] - 1;
            if (over <= 0) {
                LOST.remove(i);
            } else {
                l[1] = over;
            }
        }
    }

    /** One tick of smoke along the four sides of a wall, as far as they are near the viewer. */
    private static void muur(ClientLevel level, RandomSource r, Sluiers.Zone z, Vec3 oog, boolean lostOp) {
        double x0 = z.x0(), x1 = z.x1() + 1, z0 = z.z0(), z1 = z.z1() + 1;
        // from the floor of the box to its top, as far as that is near the viewer's eyes
        double onder = Math.max(z.y0(), oog.y - ONDER), boven = Math.min(z.y1() + 1, oog.y + HOOG);
        if (boven - onder > 1) {
            zijde(level, r, oog, x0, x1, z0, true, onder, boven, lostOp);
            zijde(level, r, oog, x0, x1, z1, true, onder, boven, lostOp);
            zijde(level, r, oog, z0, z1, x0, false, onder, boven, lostOp);
            zijde(level, r, oog, z0, z1, x1, false, onder, boven, lostOp);
        }
        // the lid: for whoever stands or flies high enough to look in over the wall
        double top = z.y1() + 1;
        if (!lostOp && oog.y > top - 8 && oog.y < top + 64) {
            double ax = Math.max(x0, oog.x - ZICHT), bx = Math.min(x1, oog.x + ZICHT), az = Math.max(z0, oog.z - ZICHT), bz = Math.min(z1, oog.z + ZICHT);
            if (bx > ax && bz > az) {
                double aantal = (bx - ax) * (bz - az) * DEKSEL;
                int n = (int) aantal + (r.nextDouble() < aantal - (int) aantal ? 1 : 0);
                for (int i = 0; i < n; i++) {
                    level.addAlwaysVisibleParticle(nl.juiced.guhs.feature.verhaal.VerhaalFeature.SLUIERROOK.get(), true, ax + r.nextDouble() * (bx - ax),
                            top + r.nextDouble() * 1.5, az + r.nextDouble() * (bz - az), (r.nextDouble() - 0.5) * 0.04, 0.002, (r.nextDouble() - 0.5) * 0.04);
                }
            }
        }
    }

    /** A side from a to b along x (langsX) or z, at the fixed other coordinate vast, between the heights onder and boven. */
    private static void zijde(ClientLevel level, RandomSource r, Vec3 oog, double a, double b, double vast, boolean langsX, double onder, double boven,
            boolean lostOp) {
        double langs = langsX ? oog.x : oog.z, dwars = langsX ? oog.z : oog.x;
        if (Math.abs(vast - dwars) > ZICHT) {
            return;
        }
        double van = Math.max(a, langs - ZICHT), tot = Math.min(b, langs + ZICHT);
        if (tot <= van) {
            return;
        }
        // (DICHTHEID is for a wall of twenty blocks high: a higher one gets as many clouds per square block)
        double aantal = (tot - van) * (lostOp ? 0.5 : DICHTHEID * (boven - onder) / 20.0);
        int n = (int) aantal + (r.nextDouble() < aantal - (int) aantal ? 1 : 0);
        for (int i = 0; i < n; i++) {
            double s = van + r.nextDouble() * (tot - van);
            // (thinner far away: every second cloud beyond 24 blocks)
            if (!lostOp && Math.abs(s - langs) + Math.abs(vast - dwars) > 24 && r.nextBoolean()) {
                continue;
            }
            double y = onder + r.nextDouble() * (boven - onder);
            double x = langsX ? s : vast + (r.nextDouble() - 0.5) * 0.8, zz = langsX ? vast + (r.nextDouble() - 0.5) * 0.8 : s;
            // it swirls: along the wall, a little in and out, slowly up
            double draai = (r.nextDouble() - 0.5) * 0.05, wiebel = (r.nextDouble() - 0.5) * 0.01;
            double vx = langsX ? draai : wiebel, vz = langsX ? wiebel : draai;
            if (lostOp) {
                level.addAlwaysVisibleParticle(r.nextInt(3) == 0 ? ParticleTypes.CLOUD : ParticleTypes.POOF, true, x, y, zz, vx, 0.12, vz);
            } else {
                ParticleOptions p = r.nextInt(12) == 0 ? ParticleTypes.LARGE_SMOKE : nl.juiced.guhs.feature.verhaal.VerhaalFeature.SLUIERROOK.get();
                level.addAlwaysVisibleParticle(p, true, x, y, zz, vx, 0.004 + r.nextDouble() * 0.012, vz);
            }
        }
    }
}
