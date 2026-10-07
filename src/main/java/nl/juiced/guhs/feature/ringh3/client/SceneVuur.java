package nl.juiced.guhs.feature.ringh3.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ringh3.BarbecuerogEntity;
import nl.juiced.guhs.feature.ringh3.Scenes;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.client.CutsceneSpeler;

/**
 * bbq2 (ring-h3): the fire and the light of the bridge scene that the verhaal engine can't draw ({@link Scenes#STOTEN}, written
 * in tools/features/ring_h3_scene.py as {@code s.stoot(...)}). The engine scatters a number of vanilla particles round a point;
 * this draws particles that are made to measure: scaled, aimed, short-lived.
 * <ul>
 *   <li>{@link #LICHT}: the staff / the shield of light: one ball of light, sparks that fly off it and are gone within half
 *       a second (nothing hangs in the air in front of the demon), and for a few ticks real light on everything round it;</li>
 *   <li>{@link #RING}: a ring of fire and dust low over the stone (his landing, the staff on the bridge, the crack);</li>
 *   <li>{@link #ZWEEP}: the lash of fire that comes up out of the chasm;</li>
 *   <li>{@link #VUUR}: the column of fire where he goes into the deep.</li>
 * </ul>
 * Where the scene plays in the world is read from two of its own actors (the Barbecuerog and Guhdalf: {@link #vind}), so a
 * replay from the Guhdex works the same. Everything is driven by the scene's tick, nothing by the frame rate.
 */
final class SceneVuur {
    static final int LICHT = 1, RING = 2, ZWEEP = 3, VUUR = 4;

    /** Something that burns for a while: kind, where (relative to the scene's anchor), strength, ticks it has burnt. */
    private static final class Brand {
        final int soort;
        final Vec3 rel;
        final double kracht;
        int tik;

        Brand(int soort, Vec3 rel, double kracht) {
            this.soort = soort;
            this.rel = rel;
            this.kracht = kracht;
        }
    }

    private static final List<Brand> BRANDT = new ArrayList<>();
    /** The lights of the bursts: where, and how many ticks they still shine. */
    private static final List<Object[]> LAMPEN = new ArrayList<>();
    @Nullable
    private static BlockPos anker;
    private static Rotation draai = Rotation.NONE;
    private static int vorige = -1;

    private SceneVuur() {
    }

    static void wis() {
        BRANDT.clear();
        LAMPEN.clear();
        anker = null;
        vorige = -1;
    }

    /** The anchor block of the bridge scene that plays, and how it is turned (null: not found yet). */
    @Nullable
    static BlockPos anker() {
        return anker;
    }

    static Rotation draai() {
        return draai;
    }

    /** A point of the scene script (relative to its anchor) in the world. */
    static Vec3 wereld(Vec3 rel) {
        return Cutscene.wereld(anker == null ? BlockPos.ZERO : anker, draai, rel);
    }

    /** (every client tick) */
    static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            wis();
            return;
        }
        lampen(level);
        if (CutsceneSpeler.scene() != Scenes.BRUG) {
            BRANDT.clear();
            anker = null;
            vorige = -1;
            return;
        }
        int t = CutsceneSpeler.tijd();
        if (t < vorige) {
            // (the scene started again)
            BRANDT.clear();
            anker = null;
        }
        if (anker == null && !vind(level, t)) {
            vorige = t;
            return;
        }
        if (t != vorige) {
            // (every tick of the script once, also the ones a slow client skipped)
            for (int tik = Math.max(vorige + 1, t - 3); tik <= t; tik++) {
                for (double[] s : Scenes.STOTEN) {
                    if ((int) s[0] == tik) {
                        begin(level, (int) s[1], new Vec3(s[2], s[3], s[4]), s[5]);
                    }
                }
            }
            vorige = t;
            for (Iterator<Brand> it = BRANDT.iterator(); it.hasNext(); ) {
                Brand b = it.next();
                if (!brand(level, b)) {
                    it.remove();
                }
                b.tik++;
            }
        }
    }

    /**
     * Finds the copy of the mine the scene plays in from where its actors stand: the Barbecuerog and Guhdalf are where the
     * script has them at tick t, for exactly one of the four ways the template can be turned.
     */
    private static boolean vind(ClientLevel level, int t) {
        Entity rog = null, guhdalf = null;
        for (Entity e : level.entitiesForRendering()) {
            if (!CutsceneSpeler.isActeur(e)) {
                continue;
            }
            if (e instanceof BarbecuerogEntity) {
                rog = e;
            } else if (e instanceof GuhNpcEntity npc && npc.getKind() == GuhNpcEntity.Kind.GUHDALF) {
                guhdalf = e;
            }
        }
        if (rog == null || guhdalf == null) {
            return false;
        }
        Vec3 relRog = Scenes.BRUG.plek("rog", t), relGuhdalf = Scenes.BRUG.plek("guhdalf", t);
        for (Rotation r : Rotation.values()) {
            Vec3 a = rog.position().subtract(Cutscene.wereld(BlockPos.ZERO, r, relRog));
            BlockPos blok = BlockPos.containing(a.x + 0.5, a.y + 0.5, a.z + 0.5);
            if (Cutscene.wereld(blok, r, relGuhdalf).distanceToSqr(guhdalf.position()) < 1.5 * 1.5) {
                anker = blok;
                draai = r;
                return true;
            }
        }
        return false;
    }

    // =====================================================================================================================
    // particles made to measure
    // =====================================================================================================================

    /** How much of the particles the player's own setting leaves (all 1, decreased 1/2, minimal 1/4). */
    static double maat() {
        int stand = Minecraft.getInstance().options.particles().get().ordinal();
        return stand == 0 ? 1.0 : stand == 1 ? 0.5 : 0.25;
    }

    /** One particle with its own size, life, speed and (0 = its own) colour. */
    @Nullable
    static Particle deeltje(ParticleOptions soort, double x, double y, double z, double vx, double vy, double vz, float schaal, int leven, int rgb) {
        Particle d = Minecraft.getInstance().particleEngine.createParticle(soort, x, y, z, vx, vy, vz);
        if (d == null) {
            return null;
        }
        d.setParticleSpeed(vx, vy, vz);
        if (schaal != 1f) {
            d.scale(schaal);
        }
        if (leven > 0) {
            d.setLifetime(leven);
        }
        if (rgb != 0 && d instanceof SingleQuadParticle q) {
            q.setColor((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
        }
        return d;
    }

    private static void begin(ClientLevel level, int soort, Vec3 rel, double kracht) {
        RandomSource r = level.getRandom();
        Vec3 p = wereld(rel);
        double m = maat();
        switch (soort) {
            case LICHT -> {
                // one ball of light, sparks flying off it in every direction: fast, and gone
                deeltje(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFF2D0), p.x, p.y, p.z, 0, 0, 0, (float) (0.25 + kracht * 0.55), 0, 0);
                // (the sparks are the little bright grains of a glow squid, dyed warm white: a firework's spark is a great
                // white star, a handful of those hanging before the lens was the "snow" this replaces)
                int n = (int) (44 * kracht * m);
                for (int i = 0; i < n; i++) {
                    Vec3 v = bol(r).scale(0.5 + r.nextDouble() * 1.0 * Math.min(1.4, 0.6 + kracht));
                    deeltje(ParticleTypes.GLOW, p.x, p.y, p.z, v.x, v.y, v.z, 0.45f + r.nextFloat() * 0.45f, 3 + r.nextInt(6), 0xFFEFC8);
                }
                if (kracht >= 0.6) {
                    lamp(level, BlockPos.containing(p.x, p.y + 0.5, p.z), 4 + (int) (kracht * 4));
                }
            }
            case RING -> {
                // grains of fire skitter away over the stone in every direction, dust rolls after them
                int n = (int) (90 * kracht * m);
                for (int i = 0; i < n; i++) {
                    double hoek = r.nextDouble() * Math.PI * 2, snel = 0.25 + r.nextDouble() * 0.6 * kracht;
                    deeltje(ParticleTypes.SMALL_FLAME, p.x, p.y + r.nextDouble() * 0.2, p.z, Math.cos(hoek) * snel, 0.01 + r.nextDouble() * 0.09, Math.sin(hoek) * snel,
                            0.35f + r.nextFloat() * 0.4f, 9 + r.nextInt(12), 0);
                }
                for (int i = 0; i < (int) (14 * kracht * m); i++) {
                    double hoek = r.nextDouble() * Math.PI * 2, snel = 0.1 + r.nextDouble() * 0.2;
                    deeltje(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x, p.y + 0.2, p.z, Math.cos(hoek) * snel, 0.02 + r.nextDouble() * 0.04, Math.sin(hoek) * snel,
                            0.7f + r.nextFloat() * 0.6f, 40 + r.nextInt(20), 0x2A201C);
                }
            }
            case VUUR -> {
                // a ball of orange light out of the deep, and for a moment real light on the walls of the chasm
                deeltje(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFF9A30), p.x, p.y + 2.0, p.z, 0, 0, 0, (float) (1.4 * kracht), 0, 0);
                lamp(level, BlockPos.containing(p.x, p.y + 3.0, p.z), 12);
                BRANDT.add(new Brand(soort, rel, kracht));
            }
            default -> BRANDT.add(new Brand(soort, rel, kracht));
        }
    }

    /** One tick of something that burns for a while; false: it is out. */
    private static boolean brand(ClientLevel level, Brand b) {
        RandomSource r = level.getRandom();
        double m = maat();
        if (b.soort == ZWEEP) {
            // the lash: a line of fire from what it holds, west and down into the chasm (it sags like a rope), burning along
            // its whole length; it comes up in three ticks, holds for a second and lets go
            int duur = (int) (24 * b.kracht);
            double ver = Math.min(1.0, (b.tik + 1) / 3.0);
            for (int i = 0; i < (int) (26 * m); i++) {
                double f = r.nextDouble() * ver, x = f * 8.5, zak = f * 1.6 + f * f * 10.0;
                Vec3 p = wereld(b.rel.add(-x, -zak, Math.sin(f * 9 + b.tik * 0.7) * 0.2));
                deeltje(i % 4 == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, p.x, p.y, p.z, 0, 0.012, 0, 1.5f + r.nextFloat() * 1.3f, 4 + r.nextInt(4), 0);
            }
            if (b.tik % 2 == 0) {
                Vec3 p = wereld(b.rel);
                deeltje(ParticleTypes.LAVA, p.x, p.y, p.z, 0, 0, 0, 1f, 0, 0);
            }
            return b.tik < duur;
        }
        if (b.soort == VUUR) {
            // ... then sparks fountain up out of the deep, lava spits, and black smoke climbs after it
            int duur = 30;
            double sterk = b.tik < 6 ? 1.0 : Math.max(0.0, 1.0 - (b.tik - 6) / 24.0);
            for (int i = 0; i < (int) (40 * sterk * m) + 1; i++) {
                Vec3 p = wereld(b.rel.add((r.nextDouble() - 0.5) * 5.0, r.nextDouble() * 2.0, (r.nextDouble() - 0.5) * 5.0));
                deeltje(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, (r.nextDouble() - 0.5) * 0.16, 0.4 + r.nextDouble() * 0.9 * sterk, (r.nextDouble() - 0.5) * 0.16,
                        0.4f + r.nextFloat() * 0.5f, 12 + r.nextInt(14), 0);
            }
            for (int i = 0; i < (int) (5 * sterk * m); i++) {
                Vec3 p = wereld(b.rel.add((r.nextDouble() - 0.5) * 5.0, 0.5, (r.nextDouble() - 0.5) * 5.0));
                deeltje(ParticleTypes.LAVA, p.x, p.y, p.z, 0, 0, 0, 1.6f, 0, 0);
            }
            for (int i = 0; i < (int) (3 * m) + 1; i++) {
                Vec3 p = wereld(b.rel.add((r.nextDouble() - 0.5) * 6.0, 1.0 + r.nextDouble() * 3.0, (r.nextDouble() - 0.5) * 6.0));
                deeltje(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x, p.y, p.z, 0, 0.14 + r.nextDouble() * 0.16, 0, 1.3f + r.nextFloat() * 0.9f, 50 + r.nextInt(30), 0x1A1210);
            }
            return b.tik < duur;
        }
        return false;
    }

    private static Vec3 bol(RandomSource r) {
        double u = r.nextDouble() * 2 - 1, hoek = r.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - u * u);
        return new Vec3(Math.cos(hoek) * s, u, Math.sin(hoek) * s);
    }

    // =====================================================================================================================
    // the light of a burst (a light block of this game alone, for a few ticks)
    // =====================================================================================================================

    private static void lamp(ClientLevel level, BlockPos waar, int ticks) {
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos p = waar.above(dy);
            BlockState s = level.getBlockState(p);
            if (s.isAir()) {
                level.setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 19);
                LAMPEN.add(new Object[] {level, p.immutable(), ticks});
                return;
            }
        }
    }

    private static void lampen(ClientLevel level) {
        for (Iterator<Object[]> it = LAMPEN.iterator(); it.hasNext(); ) {
            Object[] l = it.next();
            int over = (Integer) l[2] - 1;
            l[2] = over;
            if (l[0] != level) {
                it.remove();
            } else if (over <= 0) {
                BlockPos p = (BlockPos) l[1];
                if (level.getBlockState(p).is(Blocks.LIGHT)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 19);
                }
                it.remove();
            }
        }
    }
}
