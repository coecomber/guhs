package nl.juiced.guhs.feature.vadsrijk.wezens;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * De Kampklodder: the Grauwe Klodderreus after you cheered him up (design 9: "hij wordt weer roze en blijft als Kampklodder in
 * het kamp wonen"). A big, passive, pink guh slime that lazily hops around its home spot in Knuffelkamp and can be petted
 * (right click: hearts, a happy wobble, a Dutch "blub"). It can't be hurt. npc/kern place it with {@link #plaats}.
 */
public class Kampklodder extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), HOP = RawAnimation.begin().thenLoop("hop"),
            AAI = RawAnimation.begin().thenPlay("aai");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos thuis;
    private int hopPauze = 40, aaiPauze;

    public Kampklodder(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributen() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.MOVEMENT_SPEED, 0.15)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /**
     * Places the Kampklodder at this spot (its home: it stays within ~8 blocks). If one already lives within 64 blocks it is
     * moved there instead, so calling this twice never makes two.
     */
    @Nullable
    public static Kampklodder plaats(ServerLevel sl, BlockPos pos) {
        for (Kampklodder k : sl.getEntitiesOfClass(Kampklodder.class, new AABB(pos).inflate(64))) {
            k.thuis = pos;
            k.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, k.getYRot(), 0);
            return k;
        }
        Kampklodder k = WezensRegistry.KAMPKLODDER.get().create(sl, EntitySpawnReason.TRIGGERED);
        if (k == null) {
            return null;
        }
        k.thuis = pos;
        k.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.getRandom().nextFloat() * 360, 0);
        sl.addFreshEntity(k);
        sl.sendParticles(ParticleTypes.HEART, k.getX(), k.getY() + 2.5, k.getZ(), 10, 1, 0.5, 1, 0);
        return k;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (thuis == null) {
            thuis = blockPosition();
        }
        if (aaiPauze > 0) {
            aaiPauze--;
        }
        if (onGround() && --hopPauze <= 0) {
            hopPauze = 50 + getRandom().nextInt(80);
            double dx, dz;
            if (thuis.distSqr(blockPosition()) > 64) {
                dx = thuis.getX() + 0.5 - getX();
                dz = thuis.getZ() + 0.5 - getZ();
                double d = Math.sqrt(dx * dx + dz * dz);
                dx = dx / d * 0.25;
                dz = dz / d * 0.25;
            } else {
                double a = getRandom().nextDouble() * Math.PI * 2;
                dx = Math.cos(a) * 0.2;
                dz = Math.sin(a) * 0.2;
            }
            setDeltaMovement(dx, 0.45, dz);
            setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
            setYBodyRot(getYRot());
            playSound(SoundEvents.SLIME_JUMP, 0.6f, 0.7f);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(level() instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        if (aaiPauze > 0) {
            return InteractionResult.SUCCESS;
        }
        aaiPauze = 25;
        triggerAnim("aai", "aai");
        sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.3, getZ(), 6, 0.8, 0.3, 0.8, 0);
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1f, 1.3f);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.6f, 1.6f);
        p.sendOverlayMessage(Component.translatable("vadsrijk.wezens.kampklodder.aai" + (getRandom().nextInt(3) == 0 ? "" : getRandom().nextBoolean() ? "2" : "3")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurtServer(level, source, damage);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Kampklodder>("main", 4, s -> s.setAndContinue(s.animatable().onGround() ? IDLE : HOP)));
        controllers.add(new AnimationController<Kampklodder>("aai", 0, s -> PlayState.STOP).triggerableAnim("aai", AAI));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (thuis != null) {
            out.putLong("vr_thuis", thuis.asLong());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        long t = in.getLongOr("vr_thuis", Long.MIN_VALUE);
        thuis = t == Long.MIN_VALUE ? null : BlockPos.of(t);
    }
}
