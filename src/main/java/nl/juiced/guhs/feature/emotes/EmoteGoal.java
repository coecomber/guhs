package nl.juiced.guhs.feature.emotes;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * While a (standing) guh does an emote it stays where it is and looks where the emote wants: down while sleeping, munching
 * or being shy, at the player it waves at. Fleeing and panicking (priority 1) still go first: that ends the emote.
 * A sitting guh doesn't need this goal (it doesn't move anyway), so sitting and emoting go together.
 */
public class EmoteGoal extends Goal {
    private final GuhEntity guh;

    public EmoteGoal(GuhEntity guh) {
        this.guh = guh;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return guh.emotes.current() != null && !guh.isOrderedToSit();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        guh.getNavigation().stop();
    }

    @Override
    public void tick() {
        guh.getNavigation().stop();
        Emote emote = guh.emotes.current();
        if (emote == null) {
            return;
        }
        switch (emote) {
            case SLAPEN, SMAKKEN, VERLEGEN, VERDRIETJE -> {
                // head down (the renderer turns the head bone to where the guh looks)
                Vec3 ahead = guh.position().add(Vec3.directionFromRotation(0, guh.yBodyRot).scale(2 + guh.getBbWidth()));
                double omlaag = emote == Emote.SLAPEN ? 1.2 : emote == Emote.VERDRIETJE ? 0.7 : 0.4;
                guh.getLookControl().setLookAt(ahead.x, guh.getY() - omlaag, ahead.z, 10f, 20f);
            }
            // 2.10: blowing hearts, the bff-knuffel, a cheer: at the player it is for (else its owner close by)
            case HARTJES, BFF_KNUFFEL, VAHOEG, KNUFFELDANSJE, KNUFFELEN -> {
                if (guh.emotes.lookTarget() != null && guh.level().getPlayerByUUID(guh.emotes.lookTarget()) instanceof Player p) {
                    guh.getLookControl().setLookAt(p, 30f, 30f);
                } else if ((emote == Emote.HARTJES || emote == Emote.BFF_KNUFFEL) && guh.getOwner() != null && guh.distanceTo(guh.getOwner()) < 16) {
                    guh.getLookControl().setLookAt(guh.getOwner(), 30f, 30f);
                }
            }
            case ZWAAIEN -> {
                if (guh.emotes.lookTarget() != null && guh.level().getPlayerByUUID(guh.emotes.lookTarget()) instanceof Player p) {
                    guh.getLookControl().setLookAt(p, 30f, 30f);
                } else if (guh.getOwner() != null && guh.distanceTo(guh.getOwner()) < 16) {
                    guh.getLookControl().setLookAt(guh.getOwner(), 30f, 30f);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void stop() {
        // pushed aside by something more important (fleeing, panicking): then the emote is over too
        // (1.2.5: getting on turns the goals off - the pet you give when you get on goes on; GuhEmotes ends it when you ride off)
        if (guh.emotes.current() != null && !guh.isOrderedToSit() && !(guh.emotes.current() == Emote.AAIEN && guh.isVehicle())) {
            guh.emotes.stop();
        }
    }
}
