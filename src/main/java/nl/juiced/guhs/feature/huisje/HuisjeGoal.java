package nl.juiced.guhs.feature.huisje;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.feature.wereldleven.Dagritme;

/**
 * The home base of a Guhhuisje resident (2.10; priority 3, MOVE + LOOK; on guhs through GuhHooks.doelen, on maatjes when
 * they join the level). Residents stay within {@link Huisjes#BEREIK} of their huisje (no follow/teleport to the owner).
 * <ul>
 *   <li>night (the overworld clock, also in fixed-time dimensions): walk to the door and go inside
 *       ({@link Huisjes#naarBinnen}: hidden, zzz at the windows);</li>
 *   <li>morning: come out of the door with a yawn;</li>
 *   <li>day: every {@link Klus#wacht()} ticks try a random chore that is switched on for it and run its
 *       {@link KlusTaak} (claiming the guh with GuhHooks.bezig); when there is nothing to do, 1 in 6 plays with a toy
 *       nearby ({@link Speelgoed#willekeurig}); else it wanders around its home. Speed x {@link Band#klusSnelheid}.</li>
 * </ul>
 * Babies and sitting guhs do no chores (sitting ones stay where they sit).
 */
public class HuisjeGoal extends Goal {
    /** Tests: the day part per huisje controller position (instead of the clock). */
    public static final Map<BlockPos, Dagdeel> TEST_DAGDEEL = new ConcurrentHashMap<>();

    private final PathfinderMob mob;
    @Nullable
    private KlusTaak taak;
    /** 1.3.2: what the running task is: a chore's id, "" for a toy (the note on its bed in the room says it). */
    private String taakKlus = "";
    private int taakTicks;
    private long volgendeKlus, volgendeWandel;
    @Nullable
    private Huisje huisje;
    private long huisjeTot;

    public HuisjeGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Makes sure a maatje (or any mob) has this goal once. */
    public static void zorgVoor(PathfinderMob mob) {
        for (WrappedGoal w : mob.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof HuisjeGoal) {
                return;
            }
        }
        mob.goalSelector.addGoal(3, new HuisjeGoal(mob));
    }

    private boolean mag() {
        if (!Huisjes.isBewoner(mob) || mob.isPassenger() || mob.isVehicle() || mob.isLeashed()) {
            return false;
        }
        if (Huisjes.isBinnen(mob)) {
            return true;
        }
        if (mob instanceof TamableAnimal t && t.isOrderedToSit()) {
            return false;
        }
        if (mob instanceof PiepMaatje m && m.isBezig()) {
            return false;
        }
        return taak != null || !(mob instanceof GuhEntity g) || !GuhHooks.isBezig(g);   // (another activity claimed it)
    }

    @Override
    public boolean canUse() {
        return mag();
    }

    @Override
    public boolean canContinueToUse() {
        return mag();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Pushed aside by a more important goal (an emote the chore itself started, a fright...): the chore only pauses and
     * goes on when this goal runs again (its maxTicks still counts). No longer a resident (or sitting): it really stops.
     */
    @Override
    public void stop() {
        if (!mag() || Huisjes.isBinnen(mob)) {
            stopTaak();
        }
        huisje = null;   // (1.2.7: never trust the cached huisje after a stop)
        mob.getNavigation().stop();
    }

    @Nullable
    private Huisje thuis() {
        long nu = mob.level().getGameTime();
        if (huisje == null || nu >= huisjeTot) {
            huisje = Huisjes.thuisVan(mob);
            huisjeTot = nu + 100;
        }
        return huisje;
    }

    /** The day part for this huisje: the overworld clock (a fixed-time dimension like the guhmaag has none of its own). */
    static Dagdeel dagdeel(ServerLevel level, Huisje h) {
        Dagdeel test = TEST_DAGDEEL.get(h.pos());
        if (test != null) {
            return test;
        }
        return Dagdeel.van(nl.juiced.guhs.world.GuhTime.dayTime(level.getServer().overworld()));
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        // 1.2.7: moved out during this tick ("Uit huis" on a sleeping guh): the goal still gets one more tick before
        // canContinueToUse stops it, and the cached huisje would put the guh back "inside" a house it no longer has
        if (!Huisjes.isBewoner(mob)) {
            huisje = null;
            return;
        }
        Huisje h = thuis();
        if (h == null) {
            Huisjes.ontruim(mob, null);   // (its huisje is gone)
            return;
        }
        boolean binnen = Huisjes.isBinnen(mob);
        boolean nacht = dagdeel(level, h) == Dagdeel.NACHT;
        if (binnen) {
            Huisjes.houdBinnen(mob);
            if (!nacht) {
                Huisjes.naarBuiten(mob, h, true);
            }
            return;
        }
        if (nacht) {
            stopTaak();
            BlockPos d = h.deur();
            if (mob.distanceToSqr(Vec3.atBottomCenterOf(d)) < 2.5) {
                Huisjes.naarBinnen(mob, h);
            } else if (mob.getNavigation().isDone() || mob.tickCount % 40 == 0) {
                if (!mob.getNavigation().moveTo(d.getX() + 0.5, d.getY(), d.getZ() + 0.5, 1.0)) {
                    if (mob.distanceToSqr(Vec3.atBottomCenterOf(d)) < 64) {
                        Huisjes.naarBinnen(mob, h);   // (can't walk the last bit: in it goes anyway)
                    }
                }
            }
            return;
        }
        if (mob instanceof GuhEntity g && Dagritme.slaapt(g)) {
            Dagritme.wakker(g, false);
        }
        KlusTaak lopend = taak;   // (1.1.1: a local copy: the task may end the goal while it ticks)
        if (lopend != null) {
            if (mob instanceof GuhEntity g) {
                GuhHooks.bezig(g, 40);
            }
            boolean verder;
            try {
                verder = lopend.tick();
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Huisje taak failed", e);
                verder = false;
            }
            if (!verder || ++taakTicks > lopend.maxTicks()) {
                stopTaak();
            }
            return;
        }
        float snel = Band.klusSnelheid(mob);
        if (!h.inGebied(mob.blockPosition())) {
            if (mob.getNavigation().isDone() || mob.tickCount % 40 == 0) {
                BlockPos d = h.deur();
                if (!mob.getNavigation().moveTo(d.getX() + 0.5, d.getY(), d.getZ() + 0.5, 1.0 * snel)) {
                    Vec3 stap = LandRandomPos.getPosTowards(mob, 10, 7, Vec3.atBottomCenterOf(d));
                    if (stap != null) {
                        mob.getNavigation().moveTo(stap.x, stap.y, stap.z, 1.0 * snel);   // (too far for one path: a step towards home)
                    } else if (mob.distanceToSqr(Vec3.atBottomCenterOf(d)) > 48 * 48) {
                        mob.teleportTo(d.getX() + 0.5, d.getY(), d.getZ() + 0.5);   // (lost far away: home it goes)
                    }
                }
            }
            return;
        }
        long nu = level.getGameTime();
        if (nu >= volgendeKlus && !mob.isBaby()) {
            volgendeKlus = nu + 60 + mob.getRandom().nextInt(60);
            if (zoekWerk(level, h)) {
                return;
            }
        }
        if (mob.getNavigation().isDone() && nu >= volgendeWandel) {
            volgendeWandel = nu + 80 + mob.getRandom().nextInt(160);
            Vec3 doel = LandRandomPos.getPos(mob, 8, 4);
            if (doel != null && h.inGebied(BlockPos.containing(doel))) {
                mob.getNavigation().moveTo(doel.x, doel.y, doel.z, 0.8 * snel);
            }
        }
    }

    /** A chore (random order, only the ones switched on for it and that it can do), or 1 in 6 a toy. */
    private boolean zoekWerk(ServerLevel level, Huisje h) {
        List<Klus> klussen = new ArrayList<>();
        for (Klus k : Klusjes.alle()) {
            try {
                if (h.klusAan(mob, k.id()) && k.kan(mob) && klaarVoor(k, level.getGameTime())) {
                    klussen.add(k);
                }
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Klus {} failed", k.id(), e);
            }
        }
        Collections.shuffle(klussen, new Random(mob.getRandom().nextLong()));
        for (Klus k : klussen) {
            KlusTaak t;
            try {
                t = k.zoek(level, h, mob);
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Klus {} failed", k.id(), e);
                t = null;
            }
            geprobeerd(k, level.getGameTime());
            if (t != null) {
                start(t);
                taakKlus = k.id();
                return true;
            }
        }
        if (mob.getRandom().nextInt(6) == 0) {
            KlusTaak t = Speelgoed.willekeurig(level, mob, h.pos(), Huisjes.BEREIK);
            if (t != null) {
                start(t);
                return true;
            }
        }
        return false;
    }

    /** Per chore: not before its wait time (shorter when blij). */
    private boolean klaarVoor(Klus k, long nu) {
        return mob.getPersistentData().getCompoundOrEmpty("guhs_huisje_klus").getLongOr(k.id(), 0L) <= nu;
    }

    private void geprobeerd(Klus k, long nu) {
        var tag = mob.getPersistentData().getCompoundOrEmpty("guhs_huisje_klus");
        tag.putLong(k.id(), nu + (long) (k.wacht() / Band.klusSnelheid(mob)));
        mob.getPersistentData().put("guhs_huisje_klus", tag);
    }

    private void start(KlusTaak t) {
        taak = t;
        taakKlus = "";
        taakTicks = 0;
        if (mob instanceof GuhEntity g) {
            GuhHooks.bezig(g, 40);
        }
    }

    private void stopTaak() {
        if (taak != null) {
            KlusTaak t = taak;
            taak = null;
            try {
                t.stop();
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Huisje taak stop failed", e);
            }
            if (mob instanceof GuhEntity g) {
                GuhHooks.bezig(g, 0);
            }
        }
    }

    /** The running task (tests). */
    @Nullable
    public KlusTaak taak() {
        return taak;
    }

    /** 1.3.2: what this resident is doing right now: null = nothing, "" = playing with a toy, else the chore's id. */
    @Nullable
    public static String bezigMet(net.minecraft.world.entity.Mob mob) {
        for (WrappedGoal w : mob.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof HuisjeGoal g) {
                return g.taak == null ? null : g.taakKlus;
            }
        }
        return null;
    }
}
