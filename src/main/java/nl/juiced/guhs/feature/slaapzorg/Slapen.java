package nl.juiced.guhs.feature.slaapzorg;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;
import nl.juiced.guhs.feature.wereldleven.Dagritme;

/**
 * 1.8.0 (slaapzorg): the shared bits of a guh's sleep: the tag of sleeping spots, "is it asleep?", and the sung sleep
 * ({@link #slaapIn}: on the spot, until the morning at night or a short nap by day) that the Muziekdoosje, the Mobiel and
 * the test command use. Never required, never a punishment: a guh that isn't put to bed simply goes on as always.
 */
public final class Slapen {
    /** Every block a guh may sleep in by itself at night (the nest has its own goal; agent A adds the doos and kattenhuisje). */
    public static final TagKey<Block> SLAAPPLEKKEN = TagKey.create(Registries.BLOCK, Guhs.id("guh_slaapplekken"));

    /** Persistent data: asleep (sung or put down) until this game time; where it sleeps in a spot; the day it last had breakfast. */
    static final String SLAAP_TOT = "guhs_slaapzorg_slaap_tot", PLEK = "guhs_slaapzorg_plek", ONTBIJT_DAG = "guhs_slaapzorg_ontbijt",
            INSLAAP_OM = "guhs_slaapzorg_inslaap_om", SMAKKEN_OM = "guhs_slaapzorg_smakken_om";

    /** A nap by day (sung asleep in the afternoon), in ticks. */
    public static final int DUTJE = 20 * 60;
    /** How far a lullaby (Muziekdoosje) reaches; the Mobiel above a bed is closer. */
    public static final double LIEDJE_BEREIK = 8, MOBIEL_BEREIK = 6;

    private Slapen() {
    }

    /** The part of the day for this guh (the wereldleven day rhythm's, so the game tests can set it too). */
    @Nullable
    public static Dagdeel dagdeel(GuhEntity guh) {
        return Dagritme.dagdeel(guh);
    }

    /** Is this guh asleep right now (in a nest, a sleeping spot, sung asleep, or doing the SLAPEN emote)? */
    public static boolean slaapt(GuhEntity guh) {
        return GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT) || SleepInNestGoal.isAsleep(guh) || guh.emotes.current() == Emote.SLAPEN
                || isIngezongen(guh) || SlaapplekGoal.slaaptInPlek(guh);
    }

    /** Sung asleep (or put to sleep by the command) and not woken yet. */
    public static boolean isIngezongen(GuhEntity guh) {
        return guh.getPersistentData().getLongOr(SLAAP_TOT, 0L) > guh.level().getGameTime();
    }

    /** May this block be slept in? */
    public static boolean isSlaapplek(BlockState state) {
        return state.is(SLAAPPLEKKEN) && !state.is(VadswoudFeature.GUHNESTJE.get());
    }

    /** Where a guh lies in this spot (the top of its collision shape, at most half a block up). */
    public static double ligHoogte(Level level, BlockPos pos) {
        var shape = level.getBlockState(pos).getCollisionShape(level, pos);
        return pos.getY() + (shape.isEmpty() ? 0.0 : Math.min(0.5, shape.max(net.minecraft.core.Direction.Axis.Y)));
    }

    /** Ticks until the next morning (dayTime 23500), at least a minute. */
    static int totDeOchtend(Level level) {
        long t = Math.floorMod(nl.juiced.guhs.world.GuhTime.dayTime(level), 24000L);
        long tot = t < 23500 ? 23500 - t : 24000 - t + 23500;
        return (int) Math.max(1200, Math.min(tot, 13000));
    }

    /**
     * Sung asleep: on the spot, until the morning when it is evening or night, a short nap ({@link #DUTJE}) by day. A busy,
     * riding, fighting or ordered-to-sit-by-nobody guh just yawns. Returns whether it fell asleep.
     */
    public static boolean slaapIn(GuhEntity guh) {
        if (guh.level().isClientSide() || guh.isPassenger() || guh.isVehicle() || guh.getTarget() != null || guh.isInWater()
                || GuhHooks.isBezig(guh)) {
            return false;
        }
        Dagdeel d = dagdeel(guh);
        int ticks = d == Dagdeel.AVOND || d == Dagdeel.NACHT ? totDeOchtend(guh.level()) : DUTJE;
        guh.getPersistentData().putLong(SLAAP_TOT, guh.level().getGameTime() + ticks);
        guh.getPersistentData().remove(INSLAAP_OM);
        guh.emotes.stop();
        guh.getNavigation().stop();
        GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, true);
        guh.setInSittingPose(true);
        if (guh.level() instanceof ServerLevel sl) {
            sl.sendParticles(VadswoudFeature.GUH_ZZZ.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.1, 0.05, 0.1, 0.0);
        }
        return true;
    }

    /** Falls asleep a little later (after its yawn): the lullaby's guhs, one by one. */
    public static void slaapStraksIn(GuhEntity guh, int ticks) {
        guh.getPersistentData().putLong(INSLAAP_OM, guh.level().getGameTime() + ticks);
    }

    /** Wakes a sung-asleep guh (and opens its eyes); the morning yawn if {@code gapen}. */
    public static void wordWakker(GuhEntity guh, boolean gapen) {
        boolean was = guh.getPersistentData().contains(SLAAP_TOT) || GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT);
        guh.getPersistentData().remove(SLAAP_TOT);
        guh.getPersistentData().remove(INSLAAP_OM);
        SlaapplekGoal.wek(guh);
        GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, false);
        if (!guh.isOrderedToSit()) {
            guh.setInSittingPose(false);
        }
        if (was && gapen) {
            guh.emotes.start(Emote.GAPEN, false, nl.juiced.guhs.feature.emotes.GuhEmotes.Source.SELF);
        }
    }

    /** A tame guh never sleeps on when its owner walks far away (it follows, like in the nests). */
    static boolean baasWeg(GuhEntity guh) {
        if (!guh.isTame()) {
            return false;
        }
        LivingEntity owner = guh.getOwner();
        return owner == null || owner.level() != guh.level() || owner.distanceTo(guh) > SleepInNestGoal.OWNER_NEAR;
    }
}
