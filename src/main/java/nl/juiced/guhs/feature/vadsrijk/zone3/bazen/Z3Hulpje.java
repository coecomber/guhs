package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * A zone-3 boss add: Mika-dekmatrozen (D1, flee in smoke), schimmelbolletjes (D2, burst in spores) and Vahoegnir's grauwe
 * kloddertjes (they don't fight: they roll to the egg; cheer them up with the pink plof before they get there, zone-1 style).
 * Lives only while its boss does, never saved.
 */
public class Z3Hulpje extends VadsrijkMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID baas;
    /** Kloddertjes roll here (the egg). */
    @Nullable
    public Vec3 eiDoel;

    public Z3Hulpje(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public String soort() {
        return getType() == Z3BazenRegistry.MATROOS.get() ? "matroos" : getType() == Z3BazenRegistry.KLODDERTJE.get() ? "kloddertje" : "schimmelbol";
    }

    public static Z3Hulpje maak(ServerLevel sl, EntityType<Z3Hulpje> type, Z3Baas b, Vec3 waar) {
        Z3Hulpje h = type.create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (h != null) {
            h.snapTo(waar.x, waar.y, waar.z, sl.getRandom().nextFloat() * 360, 0);
            h.baas = b.getUUID();
            h.stelIn(Math.max(1, Math.min(50, b.mobLevel()) - 4), h.soort().equals("matroos") ? Element.VUUR : h.soort().equals("schimmelbol") ? Element.WATER : Element.GEEN, false, Set.of());
            h.zetOntvadst(false);
            sl.addFreshEntity(h);
            sl.sendParticles(ParticleTypes.POOF, waar.x, waar.y + 0.4, waar.z, 10, 0.3, 0.3, 0.3, 0.03);
        }
        return h;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
    }

    @Override
    protected double basisVads(int level) {
        return switch (soort()) {
            case "matroos" -> 260 + 34 * level;
            case "kloddertje" -> 90 + 14 * level;
            default -> 200 + 28 * level;
        };
    }

    @Override
    protected double basisSchade(int level) {
        return soort().equals("kloddertje") ? 0 : 6 + 1.3 * level;
    }

    @Override
    public int xp() {
        return 25 + mobLevel() * 2;
    }

    @Nullable
    @Override
    public String lootTabel() {
        return null;
    }

    @Nullable
    public Z3Baas baas() {
        return baas != null && level() instanceof ServerLevel sl && sl.getEntity(baas) instanceof Z3Baas b && !b.geplofd() ? b : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl) || geplofd()) {
            return;
        }
        Z3Baas b = baas();
        if (tickCount > 20 && b == null) {
            opPlof();
            discard();
            return;
        }
        if (b == null) {
            return;
        }
        if (soort().equals("kloddertje")) {
            setTarget(null);
            if (eiDoel != null) {
                if (tickCount % 10 == 0) {
                    getNavigation().moveTo(eiDoel.x, eiDoel.y, eiDoel.z, 0.75);
                }
                if (tickCount % 4 == 0) {
                    sl.sendParticles(new DustParticleOptions(0x8A8A96, 1.2f), getX(), getY() + 0.3, getZ(), 2, 0.2, 0.1, 0.2, 0);
                }
                if (position().distanceToSqr(eiDoel) < 2.2 * 2.2 && b instanceof Vahoegnir v) {
                    v.kloddertjeBijEi(sl, this);
                    discard();
                }
            }
            return;
        }
        if (tickCount % 10 == 0) {
            ServerPlayer doel = b.zDoel();
            if (doel != null && getTarget() != doel) {
                setTarget(doel.isCreative() ? null : doel);
                if (doel.isCreative()) {
                    getNavigation().moveTo(doel, 1.15);
                }
            }
        }
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        Z3Baas b = baas();
        if (b != null && bron instanceof ServerPlayer p && !b.zDoetMee(p)) {
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
        switch (soort()) {
            case "matroos" -> {
                Z3Baas.rook(sl, position(), 0.8);
                discard();
            }
            case "kloddertje" -> super.opPlof();   // the zone-1 pink plof: cheered up!
            default -> {
                sl.sendParticles(new DustParticleOptions(0x7A8AA0, 1.8f), getX(), getY() + 0.4, getZ(), 30, 0.6, 0.4, 0.6, 0.02);
                for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(1.5))) {
                    Statussen.geef(p, Status.GESPOREN, 60, Math.max(1, mobLevel() * 0.3), this);
                }
                discard();
            }
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        boolean m = getType() == Z3BazenRegistry.MATROOS.get();
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
