package nl.juiced.guhs.entity;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/** The goals behind some of the guh personalities (see {@link GuhPersonality}). */
public final class PersonalityGoals {
    private PersonalityGoals() {
    }

    private static boolean free(GuhEntity guh) {
        return !guh.isOrderedToSit() && guh.mayWander() && !guh.isVehicle() && !guh.isPassenger() && guh.getTarget() == null;
    }

    /** PLAYFUL: every now and then a burst of happy running around. */
    public static class Zoomies extends Goal {
        private final GuhEntity guh;
        private int runs;

        public Zoomies(GuhEntity guh) {
            this.guh = guh;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return guh.getPersonality() == GuhPersonality.PLAYFUL && free(guh) && guh.getRandom().nextInt(900) == 0;
        }

        @Override
        public void start() {
            runs = 3 + guh.getRandom().nextInt(3);
            guh.triggerAnim("action", "happy");
            guh.playSound(ModSounds.GUH_HAPPY.get(), 1f, guh.getVoicePitch());
            next();
        }

        private void next() {
            Vec3 target = DefaultRandomPos.getPos(guh, 7, 2);
            if (target != null) {
                guh.getNavigation().moveTo(target.x, target.y, target.z, 1.6);
            }
            runs--;
        }

        @Override
        public boolean canContinueToUse() {
            return free(guh) && (runs > 0 || !guh.getNavigation().isDone());
        }

        @Override
        public void tick() {
            if (guh.getNavigation().isDone() && runs > 0) {
                if (guh.getRandom().nextInt(3) == 0) {
                    guh.getJumpControl().jump();
                }
                next();
            }
        }
    }

    /** CURIOUS: walks up to a nearby player and has a good look at them. */
    public static class Curious extends Goal {
        private final GuhEntity guh;
        @Nullable
        private Player player;
        private int lookTicks;

        public Curious(GuhEntity guh) {
            this.guh = guh;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (guh.getPersonality() != GuhPersonality.CURIOUS || !free(guh) || guh.getRandom().nextInt(500) != 0) {
                return false;
            }
            player = guh.level().getNearestPlayer(guh, 14);
            return player != null && !player.isSpectator() && guh.distanceTo(player) > 3;
        }

        @Override
        public void start() {
            lookTicks = 60;
        }

        @Override
        public boolean canContinueToUse() {
            return player != null && player.isAlive() && free(guh) && lookTicks > 0 && guh.distanceTo(player) < 20;
        }

        @Override
        public void stop() {
            player = null;
            guh.getNavigation().stop();
        }

        @Override
        public void tick() {
            guh.getLookControl().setLookAt(player, 30f, 30f);
            if (guh.distanceTo(player) > 2.5 + guh.getBbWidth() / 2) {
                guh.getNavigation().moveTo(player, 1.0);
            } else {
                guh.getNavigation().stop();
                lookTicks--;
            }
        }
    }

    /** VADSIG: sniffs out kaas knabbels lying on the ground and eats them. */
    public static class EatDroppedSnacks extends Goal {
        private final GuhEntity guh;
        @Nullable
        private ItemEntity snack;
        private int sniffDelay;

        public EatDroppedSnacks(GuhEntity guh) {
            this.guh = guh;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean isSnack(ItemEntity item) {
            return item.isAlive() && (item.getItem().is(ModItems.KAAS_KNABBELS.get()) || item.getItem().is(ModItems.GEFRITUURDE_KAASKNABBELS.get()));
        }

        @Override
        public boolean canUse() {
            // sniff about every 10 ticks (canUse itself only runs every other tick, so no tickCount % 10 here)
            if (guh.getPersonality() != GuhPersonality.VADSIG || !free(guh) || --sniffDelay > 0) {
                return false;
            }
            sniffDelay = 5;
            List<ItemEntity> items = guh.level().getEntitiesOfClass(ItemEntity.class, guh.getBoundingBox().inflate(10), this::isSnack);
            snack = items.stream().min((a, b) -> Double.compare(guh.distanceToSqr(a), guh.distanceToSqr(b))).orElse(null);
            return snack != null;
        }

        @Override
        public boolean canContinueToUse() {
            return snack != null && isSnack(snack) && free(guh);
        }

        @Override
        public void stop() {
            snack = null;
        }

        @Override
        public void tick() {
            if (snack == null) {
                return;                                        // (1.1.1: eaten already)
            }
            guh.getLookControl().setLookAt(snack, 30f, 30f);
            if (guh.distanceTo(snack) > 1.2 + guh.getBbWidth() / 2) {
                guh.getNavigation().moveTo(snack, 1.2);
                return;
            }
            boolean fried = snack.getItem().is(ModItems.GEFRITUURDE_KAASKNABBELS.get());
            snack.getItem().shrink(1);
            if (snack.getItem().isEmpty()) {
                snack.discard();
            }
            guh.heal(fried ? guh.getMaxHealth() : 10f);
            guh.playSound(ModSounds.GUH_EAT.get(), 1f, guh.getVoicePitch());
            guh.triggerAnim("action", "happy");
            snack = null;
        }
    }
}
