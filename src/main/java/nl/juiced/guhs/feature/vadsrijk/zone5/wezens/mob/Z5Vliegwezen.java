package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;

/**
 * A zone-5 creature that floats or flies by code (the zone-2 Vliegwezen on {@link Z5Wezen}): every tick {@link #stuur} picks where
 * it wants to be and it glides there, never lower than {@link #minHoogte} above the floor under it (a local floor: it works under
 * the crown and in the caves, not the heightmap). Idle: it drifts around. Gravity comes back after the defeat.
 */
public abstract class Z5Vliegwezen extends Z5Wezen {
    protected double hoek;

    protected Z5Vliegwezen(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new Aggro(this));
    }

    @Override
    public boolean isNoGravity() {
        return !geplofd();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected String loopAnim() {
        return "vlieg";
    }

    protected double minHoogte() {
        return 1.6;
    }

    protected double snelheid() {
        return 0.14;
    }

    @Override
    protected boolean gebruiktNavigatie() {
        return false;
    }

    /** Where it wants to be this tick (target: the player it fights, or null). */
    @Nullable
    protected abstract Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel);

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || geplofd() || isNoAi() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (stil()) {
            setDeltaMovement(getDeltaMovement().scale(0.5));
            return;
        }
        LivingEntity doel = geldig(getTarget()) ? getTarget() : null;
        if (doel == null && getTarget() != null) {
            setTarget(null);
        }
        Vec3 wil = stuur(sl, doel);
        if (wil == null) {
            Vec3 thuis = thuisDoel();
            if (tickCount % 60 == 0) {
                hoek = getRandom().nextDouble() * Math.PI * 2;
            }
            wil = thuis != null ? thuis : tickCount % 60 >= 40 ? position() : position().add(Math.cos(hoek) * 2, 0, Math.sin(hoek) * 2);
            double g = grond(position());
            wil = new Vec3(wil.x, Math.max(g + minHoogte() + 0.5, Math.min(wil.y, g + minHoogte() + 3)), wil.z);
        }
        double g = grond(wil);
        Vec3 naar = new Vec3(wil.x - getX(), Math.max(wil.y, g + minHoogte()) - getY(), wil.z - getZ());
        double snel = snelheid() * (affixen.contains(Affix.SNEL) ? 1.4 : 1) * (Statussen.heeft(this, Status.BEVROREN) ? 0.7 : 1);
        Vec3 v = naar.lengthSqr() > snel * snel ? naar.normalize().scale(snel) : naar;
        setDeltaMovement(getDeltaMovement().scale(0.6).add(v.scale(0.5)));
        if (doel != null) {
            lookAt(doel, 30, 30);
        } else if (naar.horizontalDistanceSqr() > 0.01) {
            setYRot((float) Math.toDegrees(Math.atan2(-naar.x, naar.z)));
        }
        setYBodyRot(getYRot());
    }
}
