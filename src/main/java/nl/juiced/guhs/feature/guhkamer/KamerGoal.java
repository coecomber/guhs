package nl.juiced.guhs.feature.guhkamer;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.huisje.Speelgoed;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * A guest of the Guhkamer (priority 3, MOVE + LOOK): it stays in the room (no following or teleporting to its owner),
 * pads around, and now and then plays with a toy in the room. Guests that live in a Guhhuisje in the room are the
 * huisje's (HuisjeGoal).
 */
public class KamerGoal extends Goal {
    private final GuhEntity guh;
    @Nullable
    private KlusTaak taak;
    private int taakTicks;
    private long volgende;

    public KamerGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean mag() {
        return Guhkamer.isGast(guh) && !Huisjes.isBewoner(guh) && !guh.isOrderedToSit() && !guh.isPassenger() && !guh.isLeashed()
                && guh.getOwnerUUID() != null;
    }

    @Override
    public boolean canUse() {
        if (!(guh.level() instanceof ServerLevel level) || !mag()) {
            return false;
        }
        // still really a guest of its owner's room, there? (picked up and let go elsewhere: not any more)
        Guhkamer.Plek p = Guhkamer.plek(level.getServer(), guh.getOwnerUUID());
        GuhkamerData.Kamer k = GuhkamerData.get(level.getServer()).vind(guh.getOwnerUUID());
        if (p == null || k == null || p.level() != level || !k.gasten.containsKey(guh.getUUID())) {
            Guhkamer.markeer(guh, false);
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return mag();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(guh.level() instanceof ServerLevel level)) {
            return;
        }
        Guhkamer.Plek p = Guhkamer.plek(level.getServer(), guh.getOwnerUUID());
        GuhkamerData.Kamer k = GuhkamerData.get(level.getServer()).vind(guh.getOwnerUUID());
        if (p == null || k == null || k.breedte <= 0 || p.level() != level) {
            return;
        }
        AABB box = Guhkamer.binnen(p.midden(), k.breedte, k.hoogte);
        if (!box.inflate(1).contains(guh.position())) {         // (somehow outside the room: back in)
            stopTaak();
            Vec3 m = Guhkamer.vrijePlek(p, k);
            guh.teleportTo(m.x, m.y, m.z);
            return;
        }
        if (taak != null) {
            GuhHooks.bezig(guh, 40);
            boolean verder;
            try {
                verder = taak.tick();
            } catch (RuntimeException e) {
                verder = false;
            }
            if (!verder || ++taakTicks > taak.maxTicks()) {
                stopTaak();
            }
            return;
        }
        long nu = level.getGameTime();
        if (nu < volgende || !guh.getNavigation().isDone()) {
            return;
        }
        volgende = nu + 80 + guh.getRandom().nextInt(160);
        if (!guh.isBaby() || guh.getRandom().nextBoolean()) {
            if (guh.getRandom().nextInt(3) == 0) {     // a toy in the room?
                taak = Speelgoed.willekeurig(level, guh, p.midden(), k.breedte / 2 + 1);
                if (taak != null) {
                    taakTicks = 0;
                    GuhHooks.bezig(guh, 40);
                    BandVlaggen.zet(guh, BandVlaggen.SPEELT, true);
                    return;
                }
            }
        }
        Vec3 doel = LandRandomPos.getPos(guh, 8, 3);
        if (doel != null && box.contains(doel)) {
            guh.getNavigation().moveTo(doel.x, doel.y, doel.z, 0.8);
        }
    }

    private void stopTaak() {
        if (taak != null) {
            KlusTaak t = taak;
            taak = null;
            try {
                t.stop();
            } catch (RuntimeException ignored) {
                // (a toy that broke off: nothing to clean up)
            }
            if (!guh.isPassenger()) {
                BandVlaggen.zet(guh, BandVlaggen.SPEELT, false);
                GuhHooks.bezig(guh, 0);
            }
        }
    }

    @Override
    public void stop() {
        stopTaak();
        guh.getNavigation().stop();
    }

    /** (tests) the room's middle this guest belongs to. */
    @Nullable
    BlockPos midden() {
        if (!(guh.level() instanceof ServerLevel level) || guh.getOwnerUUID() == null) {
            return null;
        }
        Guhkamer.Plek p = Guhkamer.plek(level.getServer(), guh.getOwnerUUID());
        return p == null ? null : p.midden();
    }
}
