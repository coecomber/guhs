package nl.juiced.guhs.feature.spiesburcht;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

import net.minecraft.core.UUIDUtil;
/**
 * De Rookguh: a sad, skinny cloud of smoke with a guh face, drifting through the Rookdelta and the Houtskoolvlakte
 * (the ghast of the Barbecuether). The Mikas took all its kaasknabbels... It is ALWAYS peaceful: nothing you do makes
 * it angry, and it can't be hurt (you feed a Rookguh, you don't hit it).
 * <p>
 * Feed it {@link #NEEDED} kaasknabbels (right-click, or throw them near it: it swoops down to eat them). With every
 * knabbel it gets rounder and rosier; with the last one it shouts VAHOEG! and floats up home, and everyone who fed it
 * gets a saved Rookguh on their count ({@link SpiesburchtStats}).
 */
public class RookguhEntity extends FlyingMob implements GeoEntity {
    public static final int NEEDED = 6;
    /** Ticks of floating up (through the ceiling) before it's home. */
    public static final int HOMEWARD_TICKS = 70;
    private static final EntityDataAccessor<Integer> DATA_FED = SynchedEntityData.defineId(RookguhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_VAHOEG = SynchedEntityData.defineId(RookguhEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("animation.rookguh.float");
    private static final RawAnimation HOME = RawAnimation.begin().thenLoop("animation.rookguh.vahoeg");
    private static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.rookguh.eat");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> feeders = new HashSet<>();
    private int homeward;

    public RookguhEntity(EntityType<? extends RookguhEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FloatMoveControl(this);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.1).add(Attributes.FLYING_SPEED, 0.1);
    }

    /** Now and then on the floor of the Rookdelta / Houtskoolvlakte (they float up from there); also in peaceful. */
    public static boolean checkRookguhSpawnRules(EntityType<RookguhEntity> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason) || reason == EntitySpawnReason.STRUCTURE) {
            return true;
        }
        return random.nextInt(6) == 0 && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                && level.getEntitiesOfClass(RookguhEntity.class, new AABB(pos).inflate(40)).size() < 3;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FED, 0);
        builder.define(DATA_VAHOEG, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new EatThrownKnabbelsGoal());
        this.goalSelector.addGoal(2, new HopeForKnabbelsGoal());
        this.goalSelector.addGoal(5, new DriftGoal());
        this.goalSelector.addGoal(7, new LookGoal());
    }

    // --- feeding ---------------------------------------------------------------------------------------------------------

    public int fed() {
        return this.entityData.get(DATA_FED);
    }

    public boolean isVahoeg() {
        return this.entityData.get(DATA_VAHOEG);
    }

    /** 0 = skinny and grey, 1 = round and rosy (the renderer uses this). */
    public float plumpness() {
        return Math.min(1f, fed() / (float) NEEDED);
    }

    public static boolean isKnabbel(ItemStack stack) {
        return stack.is(ModItems.KAAS_KNABBELS.get()) || stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isKnabbel(stack) || isVahoeg()) {
            if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND && !isVahoeg()) {
                player.sendOverlayMessage(Component.translatable("quest.guhs.rookguh.honger", NEEDED - fed()).withStyle(ChatFormatting.GRAY));
            }
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide()) {
            stack.consume(1, player);
            feed(player instanceof ServerPlayer sp ? sp : null);
        }
        return InteractionResult.SUCCESS;
    }

    /** One kaasknabbel for the Rookguh (from this player, or from nobody in particular). */
    public void feed(@Nullable ServerPlayer feeder) {
        if (isVahoeg() || !(level() instanceof ServerLevel level)) {
            return;
        }
        if (feeder != null) {
            feeders.add(feeder.getUUID());
        }
        int fed = fed() + 1;
        this.entityData.set(DATA_FED, fed);
        this.setPersistenceRequired();
        this.triggerAnim("action", "eat");
        this.playSound(ModSounds.GUH_EAT.get(), 1.2f, 0.8f + fed * 0.06f);
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() * 0.7, getZ(), 2 + fed / 2, 0.5, 0.3, 0.5, 0.02);
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + getBbHeight() * 0.5, getZ(), 6, 0.6, 0.4, 0.6, 0.01);
        if (fed >= NEEDED) {
            startHomeward(level);
        } else if (feeder != null) {
            feeder.sendOverlayMessage(Component.translatable("quest.guhs.rookguh.gevoerd", NEEDED - fed).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** VAHOEG! It's fat and happy again and floats up home; everyone who fed it saved a Rookguh. */
    private void startHomeward(ServerLevel level) {
        this.entityData.set(DATA_VAHOEG, true);
        this.homeward = 0;
        this.playSound(ModSounds.GUH_HAPPY.get(), 2.0f, 1.0f);
        this.playSound(SoundEvents.PLAYER_LEVELUP, 0.8f, 1.6f);
        Component shout = Component.translatable("quest.guhs.rookguh.vahoeg").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32))) {
            p.sendSystemMessage(Component.translatable("chat.type.text", getDisplayName(), shout));
        }
        for (UUID id : feeders) {
            if (level.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer p) {
                SpiesburchtStats.rookguhSaved(p);
            }
        }
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 1, getZ(), 16, 0.9, 0.6, 0.9, 0.05);
    }

    @Override
    public void tick() {
        super.tick();
        if (isVahoeg()) {
            this.noPhysics = true;
            this.setDeltaMovement(new Vec3(0, 0.08 + homeward * 0.004, 0));
            this.setYRot(this.getYRot() + 9f);
            this.yBodyRot = this.getYRot();
            if (level() instanceof ServerLevel level) {
                homeward++;
                if (homeward % 3 == 0) {
                    level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 2, 0.4, 0.2, 0.4, 0.0);
                    level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 1, 0.5, 0.2, 0.5, 0.0);
                }
                if (homeward >= HOMEWARD_TICKS) {
                    level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.8, getZ(), 40, 1.0, 0.8, 1.0, 0.06);
                    level.sendParticles(ParticleTypes.FIREWORK, getX(), getY() + 0.8, getZ(), 25, 0.6, 0.6, 0.6, 0.12);
                    level.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.NEUTRAL, 1.0f, 1.2f);
                    this.discard();
                }
            }
        } else if (level().isClientSide() && this.random.nextInt(3) == 0) {
            // a little trail of smoke (thinner the hungrier it is)
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.6), getY() + 0.2, getRandomZ(0.6), 0, -0.02, 0);
        }
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    // --- always peaceful ------------------------------------------------------------------------------------------------

    /** You feed a Rookguh, you don't hit it: nothing hurts it (only /kill and the void). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (source.getEntity() instanceof ServerPlayer player && !level().isClientSide()) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.rookguh.niet_slaan").withStyle(ChatFormatting.GRAY));
            ((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.8, getZ(), 6, 0.5, 0.4, 0.5, 0.02);
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return fed() == 0 && feeders.isEmpty();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GUH_AMBIENT.get();
    }

    @Override
    public float getVoicePitch() {
        return isVahoeg() ? 1.3f : 0.7f + fed() * 0.05f;          // a low, sad little voice
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GUH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GUH_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    // --- saving ----------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fed", fed());
        ListTag list = new ListTag();
        feeders.forEach(id -> list.add(new net.minecraft.nbt.IntArrayTag(UUIDUtil.uuidToIntArray(id))));
        tag.put("Feeders", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_FED, Math.min(NEEDED - 1, tag.getIntOr("Fed", 0)));
        feeders.clear();
        for (Tag t : tag.getListOrEmpty("Feeders")) {
            feeders.add(UUIDUtil.uuidFromIntArray(((net.minecraft.nbt.IntArrayTag) t).getAsIntArray()));
        }
    }

    // --- GeckoLib --------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, this::mainAnimation));
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.STOP).triggerableAnim("eat", EAT));
    }

    private PlayState mainAnimation(AnimationTest<RookguhEntity> state) {
        return state.setAndContinue(isVahoeg() ? HOME : FLOAT);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // --- AI ----------------------------------------------------------------------------------------------------------------

    /** Floats towards where it wants to be, like a ghast (a bit slower: it's weak from hunger). */
    static class FloatMoveControl extends MoveControl {
        private final RookguhEntity guh;
        private int floatDuration;

        FloatMoveControl(RookguhEntity guh) {
            super(guh);
            this.guh = guh;
        }

        @Override
        public void tick() {
            if (guh.isVahoeg() || this.operation != MoveControl.Operation.MOVE_TO) {
                return;
            }
            if (this.floatDuration-- <= 0) {
                this.floatDuration += guh.getRandom().nextInt(5) + 2;
                Vec3 to = new Vec3(this.wantedX - guh.getX(), this.wantedY - guh.getY(), this.wantedZ - guh.getZ());
                double length = to.length();
                if (length < 0.3) {
                    this.operation = MoveControl.Operation.WAIT;
                    return;
                }
                to = to.normalize();
                if (canReach(to, Mth.ceil(length))) {
                    guh.setDeltaMovement(guh.getDeltaMovement().add(to.scale(0.07 * this.speedModifier)));
                } else {
                    this.operation = MoveControl.Operation.WAIT;
                }
            }
        }

        private boolean canReach(Vec3 dir, int length) {
            AABB box = guh.getBoundingBox();
            for (int i = 1; i < Math.min(length, 12); i++) {
                box = box.move(dir);
                if (!guh.level().noCollision(guh, box)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Drift around (never far up or down: it stays between the floor and the smoky ceiling). */
    class DriftGoal extends Goal {
        DriftGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isVahoeg()) {
                return false;
            }
            MoveControl control = getMoveControl();
            if (!control.hasWanted()) {
                return true;
            }
            double d = new Vec3(control.getWantedX(), control.getWantedY(), control.getWantedZ()).distanceToSqr(position());
            return d < 1.0 || d > 1600.0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            RandomSource r = getRandom();
            double x = getX() + (r.nextFloat() * 2 - 1) * 12;
            double y = getY() + (r.nextFloat() * 2 - 1) * 5;
            double z = getZ() + (r.nextFloat() * 2 - 1) * 12;
            // stay a few blocks above the ground (its tentacles hang almost three blocks down)
            BlockPos below = BlockPos.containing(x, y, z);
            int air = 0;
            while (air < 8 && level().getBlockState(below.below(air + 1)).isAir()) {
                air++;
            }
            if (air < 3) {
                y += 3;
            } else if (air >= 8) {
                y -= 2;
            }
            getMoveControl().setWantedPosition(x, y, z, 1.0);
        }
    }

    /** It floats over to a player holding kaasknabbels (hopeful eyes) and waits in front of them. */
    class HopeForKnabbelsGoal extends Goal {
        @Nullable
        private Player player;

        HopeForKnabbelsGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isVahoeg()) {
                return false;
            }
            player = level().getNearestPlayer(RookguhEntity.this, 12);
            return player != null && !player.isSpectator() && (isKnabbel(player.getMainHandItem()) || isKnabbel(player.getOffhandItem()));
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (player == null) {
                return;
            }
            getLookControl().setLookAt(player, 30f, 30f);
            if (distanceToSqr(player) > 16) {         // (it's a big ghast now: it waits a few blocks away, eyes level with yours)
                getMoveControl().setWantedPosition(player.getX(), player.getEyeY() - getEyeHeight(), player.getZ(), 1.0);
            } else {
                setDeltaMovement(getDeltaMovement().scale(0.7));
            }
        }
    }

    /** Knabbels on the ground (thrown for it): it swoops down and eats them one by one. */
    class EatThrownKnabbelsGoal extends Goal {
        @Nullable
        private ItemEntity food;

        EatThrownKnabbelsGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isVahoeg()) {
                return false;
            }
            List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(12, 8, 12),
                    e -> e.isAlive() && isKnabbel(e.getItem()));
            food = items.isEmpty() ? null : items.stream().min((a, b) -> Double.compare(a.distanceToSqr(RookguhEntity.this),
                    b.distanceToSqr(RookguhEntity.this))).orElse(null);
            return food != null;
        }

        @Override
        public boolean canContinueToUse() {
            return food != null && food.isAlive() && !isVahoeg();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (food == null) {
                return;
            }
            getLookControl().setLookAt(food, 30f, 30f);
            if (distanceToSqr(food) < 2.6 || getBoundingBox().inflate(0.6).intersects(food.getBoundingBox())) {
                ItemStack stack = food.getItem();
                ServerPlayer thrower = food.getOwner() instanceof ServerPlayer p ? p : null;
                stack.shrink(1);
                if (stack.isEmpty()) {
                    food.discard();
                } else {
                    food.setItem(stack);
                }
                feed(thrower);
            } else {
                getMoveControl().setWantedPosition(food.getX(), food.getY() + 0.3, food.getZ(), 1.4);
            }
        }
    }

    /** Looks where it floats (or at whoever has knabbels). */
    class LookGoal extends Goal {
        LookGoal() {
            setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !isVahoeg();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 v = getDeltaMovement();
            if (v.horizontalDistanceSqr() > 1.0E-4) {
                setYRot(-((float) Mth.atan2(v.x, v.z)) * Mth.RAD_TO_DEG);
                yBodyRot = getYRot();
            }
        }
    }
}
