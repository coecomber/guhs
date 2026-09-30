package nl.juiced.guhs.entity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * Mika: the evil guh. Same model as a guh but with an evil face and devil tail (assets/guhs/geo/entity/mika.geo.json).
 * Hostile and chases you, but its "attacks" only shove you hard. 50 HP, drops Mika's vet.
 * Big Mika (NBT {@code Boss:1}, the guardian of challenging guh caves) is different: it really hurts, has a boss bar,
 * lots of HP and drops a big reward.
 * Lives in Evil Mika homes and challenging guh caves, and (very rarely) wanders Mika's biome.
 */
public class MikaEntity extends Monster implements GeoEntity {
    public static final float HEALTH = 50f;
    /** Natural spawning keeps at most this many Mikas near each other. */
    private static final int MAX_NEARBY = 2;
    public static final float BOSS_HEALTH = 200f;
    public static final double BOSS_DAMAGE = 9.0;

    private boolean boss;
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.guhs.big_mika"),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_10);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guh.walk");
    private static final RawAnimation POUNCE = RawAnimation.begin().thenPlay("animation.guh.happy");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public MikaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        // wild Mikas show "Mika" above their heads (a name tag can still rename them)
        this.setCustomName(net.minecraft.network.chat.Component.translatable("entity.guhs.mika"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** Rare natural spawns in Mika's biome, any light level, never more than a few together. */
    public static boolean checkMikaSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        if (EntitySpawnReason.isSpawner(spawnType) || spawnType == EntitySpawnReason.STRUCTURE) {
            return true;
        }
        return random.nextInt(25) == 0 && level.getEntitiesOfClass(MikaEntity.class, new AABB(pos).inflate(48)).size() < MAX_NEARBY;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnType != EntitySpawnReason.STRUCTURE) { // structures (e.g. Big Mika) keep their own size
            this.getAttribute(Attributes.SCALE).setBaseValue(0.8 + this.random.nextDouble() * 0.6);
            this.refreshDimensions();
        }
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    /** Mika is all bark and no bite: it shoves you away hard with an evil squeak, but never hurts. Big Mika does hurt. */
    @Override
    public boolean doHurtTarget(Entity target) {
        this.swing(this.getUsedItemHand());
        this.triggerAnim("action", "pounce");
        this.playSound(ModSounds.MIKA_HURT.get(), 1f, this.getVoicePitch());
        boolean hurt = boss && super.doHurtTarget(target);
        if (target instanceof LivingEntity living) {
            living.knockback(boss ? 1.8 : 1.2, Mth.sin(this.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(this.getYRot() * Mth.DEG_TO_RAD));
            living.setDeltaMovement(living.getDeltaMovement().add(0, boss ? 0.45 : 0.3, 0)); // a little hop backwards
            living.hurtMarked = true; // make sure players get the push
        }
        return boss ? hurt : true;
    }

    public boolean isBoss() {
        return boss;
    }

    /** Turns this Mika into Big Mika (also done when loading {@code Boss:1} from a structure). */
    public void makeBoss() {
        this.boss = true;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BOSS_HEALTH);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BOSS_DAMAGE);
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.8);
        this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32.0);
        this.setCustomName(Component.translatable("entity.guhs.big_mika"));
        this.setPersistenceRequired();
        this.xpReward = 60;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Boss", boss);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        boolean hadHealth = tag.keySet().contains("Health");
        super.readAdditionalSaveData(tag);
        if (tag.getBooleanOr("Boss", false)) {
            makeBoss();
            if (!hadHealth) {
                this.setHealth(BOSS_HEALTH);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (boss) {
            bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (boss) {
            bossBar.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (boss) { // Big Mika's reward
            this.spawnAtLocation(new ItemStack(ModItems.MIKA_VET.get(), 6));
            this.spawnAtLocation(new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get(), 3));
            this.spawnAtLocation(new ItemStack(ModItems.SUPERKOMPAS.get()));
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    // --- sounds: the guh sounds made evil (lower + distorted, see tools/make_mika_sounds.py) ---

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MIKA_AMBIENT.get();
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WOOL_STEP, 0.15f, 0.9f);
    }

    // --- GeckoLib (re-uses the guh animations) ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainAnimation));
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.STOP)
                .triggerableAnim("pounce", POUNCE));
    }

    private PlayState mainAnimation(AnimationTest<MikaEntity> state) {
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
