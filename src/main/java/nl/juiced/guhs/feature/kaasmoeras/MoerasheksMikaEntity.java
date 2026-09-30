package nl.juiced.guhs.feature.kaasmoeras;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.registry.ModSounds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * De Moerasheks-Mika: a witch parody (a Mika in a big pointy hat) who lives in her paalhut in the kaasmoeras. She keeps
 * her distance and throws vadsverdrijvende drankjes ({@link VadsverdrijvendDrankjeEntity}: a short, weak debuff); when
 * she's hurt she nibbles her own moeraskaas to heal. She drops moeraskaas (a brewing ingredient for the guhbrouwketel).
 */
public class MoerasheksMikaEntity extends Monster implements RangedAttackMob, GeoEntity {
    public static final float HEALTH = 26f;
    /** Ticks between two drankjes, and the range she throws from. */
    public static final int THROW_INTERVAL = 60;
    public static final float THROW_RANGE = 10f;
    /** She nibbles moeraskaas below this much health, at most once per {@link #NIBBLE_COOLDOWN} ticks. */
    public static final float NIBBLE_BELOW = 0.5f;
    public static final int NIBBLE_COOLDOWN = 400;
    private static final int MAX_NEARBY = 1;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.moerasheks_mika.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.moerasheks_mika.walk");
    private static final RawAnimation THROW = RawAnimation.begin().thenPlay("animation.moerasheks_mika.throw");
    private static final RawAnimation NIBBLE = RawAnimation.begin().thenPlay("animation.moerasheks_mika.nibble");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int nibbleCooldown;
    private int tauntCooldown;

    public MoerasheksMikaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 18.0);
    }

    /** Rare natural spawns in the dark kaasmoeras (the hut has its own), never more than one around. */
    public static boolean checkMoerasheksSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level, EntitySpawnReason spawnType,
                                                     BlockPos pos, RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        if (EntitySpawnReason.isSpawner(spawnType) || spawnType == EntitySpawnReason.STRUCTURE) {
            return true;
        }
        return random.nextInt(4) == 0 && level.getEntitiesOfClass(MoerasheksMikaEntity.class, new AABB(pos).inflate(48)).size() < MAX_NEARBY;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new NibbleGoal());
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, THROW_INTERVAL, THROW_RANGE));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        Vec3 v = target.getDeltaMovement();
        double dx = target.getX() + v.x - getX();
        double dy = target.getEyeY() - 1.1 - getY();
        double dz = target.getZ() + v.z - getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        VadsverdrijvendDrankjeEntity drankje = new VadsverdrijvendDrankjeEntity(level(), this);
        drankje.setItem(new net.minecraft.world.item.ItemStack(KaasmoerasFeature.VADSVERDRIJVEND_DRANKJE.get()));
        drankje.setXRot(drankje.getXRot() + 20f);
        drankje.shoot(dx, dy + flat * 0.2, dz, 0.75f, 8f);
        if (!isSilent()) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_THROW, getSoundSource(), 1f, 1.2f + random.nextFloat() * 0.3f);
        }
        level().addFreshEntity(drankje);
        triggerAnim("action", "throw");
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof ServerPlayer player && getTarget() != target && tauntCooldown <= 0) {
            tauntCooldown = 200;
            player.sendSystemMessage(Component.translatable("quest.guhs.kaasmoeras.heks" + random.nextInt(4))
                    .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        }
        super.setTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide()) {
            if (nibbleCooldown > 0) {
                nibbleCooldown--;
            }
            if (tauntCooldown > 0) {
                tauntCooldown--;
            }
        } else if (random.nextInt(12) == 0) {
            // a faint swampy shimmer around her hat
            level().addParticle(ParticleTypes.WITCH, getRandomX(0.5), getY() + getBbHeight() * 0.95, getRandomZ(0.5), 0, 0.02, 0);
        }
    }

    /** Nibbling her own moeraskaas: heals her (and makes her burp). */
    public boolean nibble() {
        if (nibbleCooldown > 0) {
            return false;
        }
        nibbleCooldown = NIBBLE_COOLDOWN;
        addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        heal(4f);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT, getSoundSource(), 1f, 0.8f);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_BURP, getSoundSource(), 0.8f, 0.7f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(BorrelendeKaassausBlock.CHEESE_DUST, getX(), getY() + 1, getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        }
        triggerAnim("action", "nibble");
        return true;
    }

    private class NibbleGoal extends Goal {
        NibbleGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return nibbleCooldown <= 0 && getHealth() < getMaxHealth() * NIBBLE_BELOW;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            getNavigation().stop();
            nibble();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !isPersistenceRequired() && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(3) == 0 ? SoundEvents.WITCH_CELEBRATE : ModSounds.MIKA_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MIKA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MIKA_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return 0.8f + random.nextFloat() * 0.1f;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 4, state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "action", 2, state -> com.geckolib.animation.object.PlayState.STOP)
                .triggerableAnim("throw", THROW).triggerableAnim("nibble", NIBBLE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
