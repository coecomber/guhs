package nl.juiced.guhs.feature.slaapzorg;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;

/**
 * In the evening guhs get visibly sleepy: a big yawn now and then, nodding off for a moment (the head sinks and jerks back
 * up), a lone zzz. Only when somebody is around to see it. Also the little timers of slaapzorg: falling asleep a moment
 * after a lullaby's yawn, munching the breakfast after the wake-up yawn, and keeping a sleeper's eyes shut.
 */
final class Moeheid {
    static final String KNIK_TOT = "guhs_slaapzorg_knik_tot";
    /** One in this many seconds an evening guh yawns, one in this many it nods off. */
    static final int GAAP_KANS = 40, KNIK_KANS = 30;

    private Moeheid() {
    }

    static void tick(GuhEntity guh) {
        if (!(guh.level() instanceof ServerLevel level)) {
            return;
        }
        long nu = level.getGameTime();
        var data = guh.getPersistentData();
        if (data.getLongOr(KNIK_TOT, 0L) > nu) {
            knik(guh, data.getLongOr(KNIK_TOT, 0L) - nu);
        }
        if ((guh.tickCount + guh.getId()) % 20 != 0) {
            return;
        }
        if ((Slapen.isIngezongen(guh) || SlaapplekGoal.slaaptInPlek(guh)) && !GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT)) {
            GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, true);   // (after the Vadswoud's eye check, in the same tick)
        }
        long om = data.getLongOr(Slapen.INSLAAP_OM, 0L);
        if (om != 0L && om <= nu) {
            data.remove(Slapen.INSLAAP_OM);
            Slapen.slaapIn(guh);
        }
        long smak = data.getLongOr(Slapen.SMAKKEN_OM, 0L);
        if (smak != 0L && smak <= nu) {
            data.remove(Slapen.SMAKKEN_OM);
            if (guh.emotes.start(Emote.SMAKKEN, false, GuhEmotes.Source.SELF)) {
                level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
            }
        }
        if (Slapen.dagdeel(guh) != Dagdeel.AVOND || Slapen.slaapt(guh) || guh.emotes.current() != null || !GuhEmotes.canStart(guh)
                || GuhHooks.isBezig(guh) || !level.hasNearbyAlivePlayer(guh.getX(), guh.getY(), guh.getZ(), 24)) {
            return;
        }
        int r = guh.getRandom().nextInt(GAAP_KANS * KNIK_KANS);
        if (r < KNIK_KANS) {
            guh.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF);
        } else if (r < KNIK_KANS + GAAP_KANS) {
            data.putLong(KNIK_TOT, nu + 36);
        } else if (r < KNIK_KANS + GAAP_KANS + 40) {
            level.sendParticles(VadswoudFeature.GUH_ZZZ.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.1, 0.05, 0.1, 0.0);
        }
    }

    /** Nodding off: the head sinks (looking at the ground just in front) and jerks back up at the end. */
    private static void knik(GuhEntity guh, long rest) {
        guh.getNavigation().stop();
        Vec3 voor = guh.position().add(Vec3.directionFromRotation(0, guh.yBodyRot).scale(0.8));
        double y = rest > 8 ? guh.getY() - 0.6 : guh.getEyeY();
        guh.getLookControl().setLookAt(voor.x, y, voor.z, 6f, 30f);
    }
}
