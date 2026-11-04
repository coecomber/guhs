package nl.juiced.guhs.feature.vadsrijk.wezens.mob;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;

/**
 * The klodder family: no walking, only hops (towards the target, or a lazy random hop now and then), and a bump attack when
 * close. Subclasses add their trick in {@link #slijmTick}.
 */
public abstract class Slijmwezen extends RijkWezen {
    protected int hopPauze;

    protected Slijmwezen(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new RijkWezen.Aggro(this));
    }

    @Override
    protected boolean gebruiktNavigatie() {
        return false;
    }

    @Override
    protected String loopAnim() {
        return "hop";
    }

    @Override
    public Element standaardElement() {
        return Element.WATER;
    }

    @Override
    protected SoundEvent piepGeluid() {
        return klein() ? SoundEvents.SLIME_SQUISH_SMALL : SoundEvents.SLIME_SQUISH;
    }

    /** Horizontal hop speed towards the target. */
    protected double hopKracht() {
        return 0.32;
    }

    /** Ticks between hops. */
    protected int hopTijd() {
        return 16;
    }

    /** How close it keeps to its target (0 = right on top of it). */
    protected double afstandHouden() {
        return 0;
    }

    /** The trick of the kind (every server tick while it is fighting fit); return true to skip the normal hop this tick. */
    protected boolean slijmTick(LivingEntity doel) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || geplofd() || stil()) {
            return;
        }
        LivingEntity doel = getTarget();
        if (doel != null && (!doel.isAlive() || doel.distanceTo(this) > 32)) {
            setTarget(null);
            doel = null;
        }
        if (doel != null && slijmTick(doel)) {
            return;
        }
        if (hopPauze > 0) {
            hopPauze--;
        }
        if (onGround() && hopPauze <= 0) {
            hop(doel);
        }
        if (doel != null && aanvalPauze <= 0 && distanceTo(doel) < getBbWidth() * 0.5 + 1.1) {
            doHurtTarget((net.minecraft.server.level.ServerLevel) level(), doel);
            aanvalPauze = 24;
        }
    }

    protected void hop(LivingEntity doel) {
        double kracht = hopKracht() * (Statussen.heeft(this, Status.PLAKKERIG) ? 0.5 : 1) * (affixen.contains(nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix.SNEL) ? 1.35 : 1);
        if (doel != null) {
            Vec3 naar = doel.position().subtract(position()).multiply(1, 0, 1);
            double d = naar.length();
            if (afstandHouden() > 0 && d < afstandHouden()) {
                naar = naar.scale(-1);     // too close: hop back a bit
            }
            naar = d < 0.01 ? Vec3.ZERO : naar.normalize().scale(kracht);
            setDeltaMovement(naar.x, 0.42, naar.z);
            lookAt(doel, 30, 30);
            hopPauze = hopTijd();
            playSound(piepGeluid(), 0.4f, 1.0f + getRandom().nextFloat() * 0.4f);
        } else if (thuisDoel() != null) {
            // back to its camp / along its patrol route: a steady hop every hop time
            Vec3 naar = thuisDoel().subtract(position()).multiply(1, 0, 1);
            double a = Math.atan2(naar.z, naar.x);
            double k = opWegTerug() ? 0.3 : 0.22;
            setDeltaMovement(Math.cos(a) * k, 0.38, Math.sin(a) * k);
            setYRot((float) Math.toDegrees(Math.atan2(-Math.cos(a), Math.sin(a))));
            setYBodyRot(getYRot());
            hopPauze = hopTijd() + 4;
        } else if (getRandom().nextInt(5) == 0) {
            double a = getRandom().nextDouble() * Math.PI * 2;
            setDeltaMovement(Math.cos(a) * 0.18, 0.36, Math.sin(a) * 0.18);
            setYRot((float) Math.toDegrees(Math.atan2(-Math.cos(a), Math.sin(a))));
            setYBodyRot(getYRot());
            hopPauze = hopTijd() * 2;
        } else {
            hopPauze = 10;
        }
    }
}
