package nl.juiced.guhs.feature.knabbelspelen;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.doolhof.Anker;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Spijkerpoepen: a big knabbelspijker dangles on a string behind your guh belt. It swings along with every step and
 * turn (a little pendulum, {@link #slinger}). Back up to a kaasmelk bottle, hold still, and crouch to lower the spijker
 * into its neck: plonk! Three bottles in your lane, time is the score.
 */
public final class Spijkerpoepen implements Wedstrijd.Spel {
    public static final Spijkerpoepen SPEL = new Spijkerpoepen();
    /** The string plus the spijker (blocks), the belt heights (standing / crouching), how far behind you the belt is. */
    public static final double LENGTE = 0.6, RIEM = 0.75, RIEM_GEBUKT = 0.4, ACHTER = 0.35;
    /** The pendulum: spring, damping, the widest swing. */
    public static final double VEER = 0.1, DEMPING = 0.035, MAX_ZWIEP = 0.7;
    /** In the neck: this close (horizontally), this calm, this many ticks in a row. */
    public static final double HALS = 0.22, RUSTIG = 0.07;
    public static final int STIL_TICKS = 8;

    static final class Staat {
        Vec3 riem, riem1, riem2;
        double dx, dz, vx, vz;
        int fles, stil;
        @Nullable
        SpelDing ding;
    }

    static Staat staat(Wedstrijd.Deelnemer d) {
        if (d.staat instanceof Staat s) {
            return s;
        }
        Staat s = new Staat();
        d.staat = s;
        return s;
    }

    @Override
    public void klaarzetten(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        Staat s = staat(d);
        w.naarStart(d, p, level, 0);
        for (int i = 0; i < Speelvelden.FLES_U.length; i++) {
            level.setBlock(fles(w.anker, d.baan, i), KnabbelspelenFeature.KAASMELKFLES.get().defaultBlockState(), 2);
        }
        SpelDing ding = KnabbelspelenFeature.DING.get().create(level, EntitySpawnReason.TRIGGERED);
        if (ding != null) {
            ding.soort(SpelDing.SPIJKER);
            ding.spel = w.npcId;
            ding.baan = d.baan;
            ding.eigenaar(p.getId());
            Vec3 tip = riem(p).add(0, -LENGTE, 0);
            ding.snapTo(tip.x, tip.y, tip.z, 0, 0);
            level.addFreshEntity(ding);
            s.ding = ding;
        }
        s.riem = s.riem1 = s.riem2 = riem(p);
    }

    static BlockPos fles(Anker a, int k, int i) {
        return Speelvelden.blok(a, Onderdeel.SPIJKERPOEPEN, k, Speelvelden.FLES_U[i], 0, Speelvelden.G);
    }

    /** Your guh belt: a bit behind you, lower when you crouch. */
    public static Vec3 riem(Player p) {
        double yaw = Math.toRadians(p.getYRot());
        Vec3 achter = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw)).scale(ACHTER);
        return p.position().add(achter).add(0, p.isCrouching() ? RIEM_GEBUKT : RIEM, 0);
    }

    /** One tick of the pendulum: the belt's jolt pushes the spijker, the spring pulls it back under the belt. */
    static void slinger(Staat s, Vec3 riem) {
        s.riem2 = s.riem1;
        s.riem1 = s.riem;
        s.riem = riem;
        double ax = s.riem.x - 2 * s.riem1.x + s.riem2.x, az = s.riem.z - 2 * s.riem1.z + s.riem2.z;
        if (Math.abs(ax) > 0.5 || Math.abs(az) > 0.5) {             // (a teleport: no jolt)
            ax = az = 0;
        }
        s.vx = s.vx * (1 - DEMPING) - s.dx * VEER - ax;
        s.vz = s.vz * (1 - DEMPING) - s.dz * VEER - az;
        s.dx += s.vx;
        s.dz += s.vz;
        double r = Math.hypot(s.dx, s.dz);
        if (r > MAX_ZWIEP) {
            s.dx *= MAX_ZWIEP / r;
            s.dz *= MAX_ZWIEP / r;
        }
    }

    static Vec3 tip(Staat s) {
        return s.riem.add(s.dx, -LENGTE, s.dz);
    }

    @Override
    public void tick(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level, int t) {
        Staat s = staat(d);
        slinger(s, riem(p));
        Vec3 tip = tip(s);
        if (s.ding != null && s.ding.isAlive()) {
            s.ding.setPos(tip.x, tip.y, tip.z);
        }
        if (s.fles < Speelvelden.FLES_U.length) {
            BlockPos f = fles(w.anker, d.baan, s.fles);
            boolean erin = Math.hypot(tip.x - (f.getX() + 0.5), tip.z - (f.getZ() + 0.5)) < HALS && tip.y < f.getY() + 0.875
                    && Math.hypot(s.vx, s.vz) < RUSTIG;
            s.stil = erin ? s.stil + 1 : 0;
            if (s.stil >= STIL_TICKS) {
                plonk(w, d, p, level, s);
                if (s.fles >= Speelvelden.FLES_U.length) {
                    w.klaar(d, p, t, false);
                    return;
                }
            }
        }
        if (t % 4 == 0) {
            int zwiep = (int) Math.min(5, Math.round(Math.hypot(s.dx, s.dz) * 10));
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.spijker.bar", nl.juiced.guhs.quest.Highscores.tijd(t),
                    Math.min(s.fles + 1, Speelvelden.FLES_U.length), Speelvelden.FLES_U.length, "~".repeat(zwiep) + "|" + "~".repeat(zwiep))
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    /** Plonk! The spijker is in the bottle: it fills up, on to the next one. */
    static void plonk(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level, Staat s) {
        BlockPos f = fles(w.anker, d.baan, s.fles);
        level.setBlock(f, KnabbelspelenFeature.KAASMELKFLES.get().defaultBlockState().setValue(KnabbelspelenBlocks.KaasmelkFles.VOL, true), 2);
        level.playSound(null, f, KnabbelspelenFeature.PLONK.get(), SoundSource.PLAYERS, 1f, 1f + 0.15f * s.fles);
        level.sendParticles(ParticleTypes.SPLASH, f.getX() + 0.5, f.getY() + 1.0, f.getZ() + 0.5, 12, 0.15, 0.1, 0.15, 0.05);
        s.fles++;
        s.stil = 0;
        p.sendSystemMessage(Component.translatable("quest.guhs.knabbelspelen.spijker.plonk", s.fles, Speelvelden.FLES_U.length)
                .withStyle(ChatFormatting.GREEN));
    }

    @Override
    public void einde(Wedstrijd w, Wedstrijd.Deelnemer d, @Nullable ServerPlayer p, ServerLevel level) {
        Staat s = staat(d);
        if (s.ding != null) {
            s.ding.discard();
            s.ding = null;
        }
    }

    private Spijkerpoepen() {
    }
}
