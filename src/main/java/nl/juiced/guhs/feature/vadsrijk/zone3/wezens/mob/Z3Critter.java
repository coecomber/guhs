package nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob;

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
 * The zone-3 critters (ZONE3_PLAN §1 "leven"): honey bees (the existing guh_bee look) buzzing round the crown and the branches,
 * spore moths and lantern fireflies that glow (fireflies at night everywhere, always in the dark below), root mice scurrying over
 * the ground and through the trunk. Passive, untouchable, no loot. They flee when you run at them (a sprinting player scares them
 * from twice as far). Flyers hover over the LOCAL floor (under the crown, in caves), never the heightmap. They leave when nobody
 * is within 80 blocks.
 */
public class Z3Critter extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final boolean vliegt;
    private final RawAnimation idle, loop, vlieg;
    private double hoek;
    private int landen;

    public Z3Critter(EntityType<? extends PathfinderMob> type, Level level, boolean vliegt, boolean bij) {
        super(type, level);
        this.vliegt = vliegt;
        if (bij) {
            RawAnimation a = RawAnimation.begin().thenLoop("animation.guh_bee.fly");
            idle = loop = vlieg = a;
        } else {
            idle = RawAnimation.begin().thenLoop("idle");
            loop = RawAnimation.begin().thenLoop("loop");
            vlieg = RawAnimation.begin().thenLoop("vlieg");
        }
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
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 10f, 1.3, 1.8, e -> e instanceof Player p && p.isSprinting()));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 5f, 1.2, 1.6));
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

    /** The floor under a point (from 3 above down 24), or the point's own y - 2. */
    private double vloer(Vec3 v) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int x = net.minecraft.util.Mth.floor(v.x), z = net.minecraft.util.Mth.floor(v.z);
        for (int y = net.minecraft.util.Mth.floor(v.y) + 3; y > v.y - 24; y--) {
            m.set(x, y - 1, z);
            if (!level().getBlockState(m).getCollisionShape(level(), m).isEmpty() || !level().getFluidState(m).isEmpty()) {
                return y;
            }
        }
        return v.y - 2;
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
            if (sl.getNearestPlayer(this, 5) != null) {
                landen = 0;
            }
            return;
        }
        if (tickCount % 50 == 0) {
            hoek += (getRandom().nextDouble() - 0.5) * 2.4;
            if (getRandom().nextInt(7) == 0) {
                landen = 60 + getRandom().nextInt(80);
            }
        }
        Player p = sl.getNearestPlayer(this, 4);
        Player renner = sl.getNearestPlayer(this, 9);
        if (p == null && renner != null && renner.isSprinting()) {
            p = renner;
        }
        double snel = 0.08;
        if (p != null) {
            Vec3 weg = position().subtract(p.position()).multiply(1, 0, 1);
            if (weg.lengthSqr() > 1.0E-4) {
                hoek = Math.atan2(weg.z, weg.x);
                snel = 0.16;
            }
        }
        double hoog = vloer(position()) + 1.6 + Math.sin(tickCount * 0.05 + getId()) * 0.8;
        Vec3 v = new Vec3(Math.cos(hoek) * snel, Math.max(-0.08, Math.min(0.08, (hoog - getY()) * 0.1)), Math.sin(hoek) * snel);
        setDeltaMovement(getDeltaMovement().scale(0.7).add(v.scale(0.5)));
        if (horizontalCollision) {
            hoek += Math.PI * 0.7;
        }
        setYRot((float) Math.toDegrees(Math.atan2(-v.x, v.z)));
        setYBodyRot(getYRot());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Z3Critter>("main", 4, state -> {
            Z3Critter c = state.animatable();
            if (c.vliegt && !c.onGround()) {
                return state.setAndContinue(c.vlieg);
            }
            return state.setAndContinue(state.isMoving() ? c.loop : c.idle);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
