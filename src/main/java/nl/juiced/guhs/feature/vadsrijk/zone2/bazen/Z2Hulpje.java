package nl.juiced.guhs.feature.vadsrijk.zone2.bazen;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * A boss add: the Galmwolf's Sneeuwknabbelaar pups (crystal, shatter) and the Boormeester's Mika-mijnwerkers (flee in a smoke
 * puff). It hunts the boss's fighters itself (the boss hands it a target, so creative/far players work too), lives only while its
 * boss does, is never saved.
 */
public class Z2Hulpje extends VadsrijkMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID baas;
    /** The fleeing Mika on his little cart after the Boormeester's defeat: no fighting, rides off, vanishes. */
    public boolean vluchter;
    private Vec3 vluchtRichting = Vec3.ZERO;

    public Z2Hulpje(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public boolean mika() {
        return getType() == Z2BazenRegistry.MIJNWERKER.get();
    }

    public void koppel(Z2Baas b) {
        baas = b.getUUID();
        stelIn(Math.max(1, b.mobLevel() - 4), mika() ? Element.VUUR : Element.WATER, false, Set.of());
        zetOntvadst(false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
    }

    @Override
    protected double basisVads(int level) {
        return mika() ? 220 + 30 * level : 160 + 24 * level;
    }

    @Override
    protected double basisSchade(int level) {
        return mika() ? 6 + 1.3 * level : 5 + 1.2 * level;
    }

    @Override
    public int xp() {
        return 20 + mobLevel() * 2;
    }

    @Nullable
    @Override
    public String lootTabel() {
        return null;
    }

    @Nullable
    private Z2Baas baas() {
        return baas != null && level() instanceof ServerLevel sl && sl.getEntity(baas) instanceof Z2Baas b && !b.geplofd() ? b : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl) || geplofd()) {
            return;
        }
        if (vluchter) {
            var kar = getVehicle();
            if (kar != null) {   // the cart rolls off by itself (no rails needed), a smoke trail behind
                kar.setPos(kar.getX() + vluchtRichting.x, kar.getY(), kar.getZ() + vluchtRichting.z);
                kar.setDeltaMovement(Vec3.ZERO);
                if (tickCount % 2 == 0) {
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, kar.getX(), kar.getY() + 0.6, kar.getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            if (tickCount > 70) {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.6, 0.6, 0.6, 0.03);
                if (getVehicle() != null) {
                    getVehicle().discard();
                }
                discard();
            }
            return;
        }
        Z2Baas b = baas();
        if (tickCount > 20 && b == null) {
            if (mika()) {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 15, 0.3, 0.4, 0.3, 0.02);
            } else {
                Z2Baas.scherven(sl, position().add(0, 0.4, 0), 0.8, 0.7);
            }
            discard();
            return;
        }
        if (b != null && tickCount % 10 == 0) {
            ServerPlayer doel = b.zDoel();
            if (doel != null && (getTarget() != doel)) {
                setTarget(doel.isCreative() ? null : doel);
                if (doel.isCreative()) {
                    getNavigation().moveTo(doel, 1.15);
                }
            }
        }
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        Z2Baas b = baas();
        if (vluchter || (b != null && bron instanceof ServerPlayer p && !b.zDoetMee(p))) {
            return;
        }
        super.krijgSchade(n, bron);
    }

    @Override
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
        for (ServerPlayer p : helpers) {
            Spelers.geefXpGedeeld(p, xp(), "mob");
        }
    }

    @Override
    protected void opPlof() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (mika()) {   // a Mika flees back to the fort in a smoke puff
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 25, 0.4, 0.5, 0.4, 0.04);
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.6, getZ(), 12, 0.3, 0.3, 0.3, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8f, 1.6f);
            discard();
        } else {
            Z2Baas.scherven(sl, position().add(0, 0.4, 0), 0.8, 0.7);
            discard();
        }
    }

    /** Drives off on a little cart (the defeated Boormeester's flight). */
    public void rijWeg(Vec3 richtingPerTick) {
        vluchter = true;
        vluchtRichting = richtingPerTick;
        tickCount = 0;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        boolean m = mika();
        RawAnimation idle = RawAnimation.begin().thenLoop(m ? "animation.guh.idle" : "idle");
        RawAnimation walk = RawAnimation.begin().thenLoop(m ? "animation.guh.walk" : "walk");
        controllers.add(new AnimationController<>("beweeg", 3, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }
}
