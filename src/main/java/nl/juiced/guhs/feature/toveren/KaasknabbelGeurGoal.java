package nl.juiced.guhs.feature.toveren;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import org.jspecify.annotations.Nullable;

/**
 * Kaasknabbelgeur: a wild guh smells the kaasknabbels on a player's armour (the enchantment on any piece) within
 * {@link #BEREIK} blocks, walks to them and follows them while they stay within {@link #LOS} blocks. Added to every guh
 * when it joins the level ({@link Spreuken#onJoin}), at the priority of the guh's own kaasknabbel tempt goal. Tamed guhs,
 * sitting ones, residents of the Knuffeldal, leashed ones and riders are left alone.
 */
public class KaasknabbelGeurGoal extends Goal {
    public static final double BEREIK = 16.0, LOS = 24.0;
    private static final double DICHTBIJ = 2.5, SNELHEID = 1.1;
    private final GuhEntity guh;
    private @Nullable Player speler;
    private int opnieuw;

    public KaasknabbelGeurGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Makes sure this guh has the goal once. */
    public static void zorgVoor(GuhEntity guh) {
        for (WrappedGoal w : guh.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof KaasknabbelGeurGoal) {
                return;
            }
        }
        guh.goalSelector.addGoal(4, new KaasknabbelGeurGoal(guh));
    }

    private boolean wild() {
        return !guh.isTame() && !guh.isOrderedToSit() && guh.mayWander() && !guh.isLeashed() && !guh.isPassenger() && !guh.isVehicle()
                && !GuhHooks.isBewoner(guh);
    }

    /** Does this player smell of kaasknabbels? */
    static boolean ruikt(Player p) {
        return p.isAlive() && !p.isSpectator() && Spreuken.niveau(p, Spreuken.KAASKNABBELGEUR) > 0;
    }

    @Override
    public boolean canUse() {
        if (!wild()) {
            return false;
        }
        Player beste = null;
        double bestD = BEREIK * BEREIK;
        for (Player p : guh.level().players()) {
            double d = p.distanceToSqr(guh);
            if (d < bestD && ruikt(p)) {
                beste = p;
                bestD = d;
            }
        }
        speler = beste;
        return beste != null;
    }

    @Override
    public boolean canContinueToUse() {
        return speler != null && wild() && speler.level() == guh.level() && speler.distanceToSqr(guh) < LOS * LOS && ruikt(speler);
    }

    @Override
    public void start() {
        opnieuw = 0;
    }

    @Override
    public void stop() {
        speler = null;
        guh.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (speler == null) {
            return;
        }
        guh.getLookControl().setLookAt(speler, 10.0F, guh.getMaxHeadXRot());
        if (--opnieuw > 0) {
            return;
        }
        opnieuw = adjustedTickDelay(10);
        if (guh.distanceToSqr(speler) > DICHTBIJ * DICHTBIJ) {
            guh.getNavigation().moveTo(speler, SNELHEID);
        } else {
            guh.getNavigation().stop();
        }
    }
}
