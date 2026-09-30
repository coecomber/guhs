package nl.juiced.guhs.feature.speelgoed;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.huisje.Speelgoed;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Tamed guhs play with the toys when a player is near (priority 4, before following the owner): now and then (and right
 * away when you kick the knabbelbal their way) a free, wandering guh picks a toy within {@link #BEREIK} blocks and plays
 * with it. Residents of a Guhhuisje don't need this: their huisje sends them to play at random (HuisjeGoal).
 */
public class SpeelGoal extends Goal {
    public static final int BEREIK = 12, SPELER = 16;
    /** Persistent data: not again before this game time. */
    static final String RUST = "guhs_speelgoed_rust";
    /** Tests: always try (no dice, no rest, no player needed). */
    public static final java.util.Set<java.util.UUID> TEST_ALTIJD = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final GuhEntity guh;
    @Nullable
    private KlusTaak taak;
    private int taakTicks;

    public SpeelGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean vrij() {
        return guh.getType() == ModEntities.GUH.get() && guh.isTame() && !Huisjes.isBewoner(guh) && !guh.isOrderedToSit() && !guh.isPassenger()
                && !guh.isVehicle() && !guh.isLeashed() && guh.mayWander() && !guh.isInWater();
    }

    @Override
    public boolean canUse() {
        if (!(guh.level() instanceof ServerLevel level) || !vrij() || GuhHooks.isBezig(guh)) {
            return false;
        }
        long nu = level.getGameTime();
        var data = guh.getPersistentData();
        // invited: its player kicked the ball
        if (data.getLongOr(Spelen.BAL_TOT, 0L) > nu) {
            Entity e = level.getEntity(data.getIntOr(Spelen.BAL, 0));
            data.remove(Spelen.BAL_TOT);
            if (e instanceof KnabbelbalEntity bal && bal.distanceToSqr(guh) < 20 * 20) {
                taak = new KnabbelbalSpel(guh, level, bal);
                return true;
            }
        }
        boolean test = TEST_ALTIJD.contains(guh.getUUID());
        if (!test) {
            if ((guh.tickCount + guh.getId()) % 40 != 0 || data.getLongOr(RUST, 0L) > nu) {
                return false;
            }
            Player p = level.getNearestPlayer(guh, SPELER);
            if (p == null || p.isSpectator() || guh.getRandom().nextInt(4) != 0) {
                return false;
            }
        }
        taak = Speelgoed.willekeurig(level, guh, guh.blockPosition(), BEREIK);
        if (taak == null) {
            data.putLong(RUST, nu + 200);
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return taak != null && !guh.isOrderedToSit() && !guh.isLeashed();
    }

    @Override
    public void start() {
        taakTicks = 0;
        GuhHooks.bezig(guh, 40);
        BandVlaggen.zet(guh, BandVlaggen.SPEELT, true);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (taak == null) {
            return;
        }
        GuhHooks.bezig(guh, 40);
        boolean verder;
        try {
            verder = taak.tick();
        } catch (RuntimeException e) {
            com.mojang.logging.LogUtils.getLogger().warn("Speelgoed taak failed", e);
            verder = false;
        }
        if (!verder || ++taakTicks > taak.maxTicks()) {
            eindig();
        }
    }

    private void eindig() {
        KlusTaak t = taak;
        taak = null;
        if (t != null) {
            try {
                t.stop();
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Speelgoed taak stop failed", e);
            }
        }
    }

    @Override
    public void stop() {
        eindig();
        if (!guh.isPassenger()) {   // (on a swing the seat keeps it busy and switches the flag off)
            BandVlaggen.zet(guh, BandVlaggen.SPEELT, false);
            GuhHooks.bezig(guh, 0);
        }
        guh.getPersistentData().putLong(RUST, guh.level().getGameTime() + 600 + guh.getRandom().nextInt(900));
    }

    /** The running session (tests). */
    @Nullable
    public KlusTaak taak() {
        return taak;
    }
}
