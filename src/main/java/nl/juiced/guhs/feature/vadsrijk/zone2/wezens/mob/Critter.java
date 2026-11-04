package nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The zone-2 critters (ronde 14 "critters" for atmosphere): snow hares that hop away from you, ice birds that flit low over the
 * snow and land now and then, crystal moths that flutter (glowing) at night. Passive, untouchable, no loot, not saved for long:
 * they leave when nobody is near ({@link nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2Critters} keeps a few around players).
 */
public class Critter extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation LOOP = RawAnimation.begin().thenLoop("loop");
    private static final RawAnimation VLIEG = RawAnimation.begin().thenLoop("vlieg");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final boolean vliegt;
    private double hoek;
    private int landen;

    public Critter(EntityType<? extends PathfinderMob> type, Level level, boolean vliegt) {
        super(type, level);
        this.vliegt = vliegt;
        if (vliegt) {
            setNoGravity(true);
        }
    }

    public static AttributeSupplier.Builder attributen(double snelheid) {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 4).add(Attributes.MOVEMENT_SPEED, snelheid);
    }

    public boolean vliegt() {
        return vliegt;
    }

    @Override
    protected void registerGoals() {
        if (vliegt) {
            return;
        }
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 7f, 1.3, 1.7));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isNoGravity() {
        return vliegt && landen <= 0;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    /** Untouchable (only /kill works). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurtServer(level, source, damage);
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return true;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (tickCount % 100 == 0 && sl.getNearestPlayer(getX(), getY(), getZ(), 80, false) == null) {
            discard();
            return;
        }
        if (!vliegt) {
            return;
        }
        if (landen > 0) {
            landen--;
            if (onGround()) {
                setDeltaMovement(0, getDeltaMovement().y, 0);
            }
            Player p = sl.getNearestPlayer(this, 5);
            if (p != null) {
                landen = 0;
            }
            return;
        }
        if (tickCount % 50 == 0) {
            hoek += (getRandom().nextDouble() - 0.5) * 2.4;
            if (getRandom().nextInt(6) == 0) {
                landen = 60 + getRandom().nextInt(80);
            }
        }
        Player p = sl.getNearestPlayer(this, 4);
        if (p != null) {
            Vec3 weg = position().subtract(p.position()).multiply(1, 0, 1);
            if (weg.lengthSqr() > 1.0E-4) {
                hoek = Math.atan2(weg.z, weg.x);
            }
        }
        int g = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ());
        double hoog = g + 1.5 + Math.sin(tickCount * 0.05) * 0.8;
        double snel = 0.08;
        Vec3 v = new Vec3(Math.cos(hoek) * snel, Math.max(-0.08, Math.min(0.08, (hoog - getY()) * 0.1)), Math.sin(hoek) * snel);
        setDeltaMovement(getDeltaMovement().scale(0.7).add(v.scale(0.5)));
        setYRot((float) Math.toDegrees(Math.atan2(-v.x, v.z)));
        setYBodyRot(getYRot());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Critter>("main", 4, state -> {
            Critter c = state.animatable();
            if (c.vliegt && !c.onGround()) {
                return state.setAndContinue(VLIEG);
            }
            return state.setAndContinue(state.isMoving() ? LOOP : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
