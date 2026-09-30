package nl.juiced.guhs.feature.creche;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
/**
 * guhs:creche_babyguh (a prop, no Guhdex page): a tiny baby guh with a pacifier that crawls out of its crib during Juf
 * Knuffel's minigame ({@link CrecheGame}). Right-click it to pick it up (it sits in your arms, wiggling), then put it back
 * in an empty guh_wiegje. It can't be hurt, never fights, and isn't saved: it only lives during a game.
 */
public class CrecheBabyguh extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> GEDRAGEN = SynchedEntityData.defineId(CrecheBabyguh.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation KRUIP = RawAnimation.begin().thenLoop("animation.guh.walk");
    private static final RawAnimation STIL = RawAnimation.begin().thenLoop("animation.guh.idle");
    private static final RawAnimation WIEBEL = RawAnimation.begin().thenLoop("animation.guh.emote_dansen");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** The player carrying it (server). */
    @Nullable
    UUID drager;
    /** The Juf Knuffel whose game it belongs to (server). */
    @Nullable
    UUID juf;
    /** When it crawled out (game time). */
    long uitSinds;

    public CrecheBabyguh(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GEDRAGEN, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // crawl around (slowly, and on and on: babies never sit still)
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.85, 12) {
            @Override
            public boolean canUse() {
                return !isGedragen() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isGedragen() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public boolean isGedragen() {
        return this.entityData.get(GEDRAGEN);
    }

    @Nullable
    public UUID drager() {
        return drager;
    }

    /** Picked up by a player (or put down: null). */
    void draag(@Nullable ServerPlayer player) {
        this.drager = player == null ? null : player.getUUID();
        this.entityData.set(GEDRAGEN, player != null);
        this.setNoGravity(player != null);
        this.getNavigation().stop();
        if (player == null) {
            this.uitSinds = level().getGameTime();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (drager != null) {
            Player p = level().getPlayerByUUID(drager);
            if (p == null || !p.isAlive() || p.level() != level()) {
                draag(null);
            } else {
                // in the player's arms, a little in front of them (two babies: one on each side)
                Vec3 look = Vec3.directionFromRotation(0, p.getYRot());
                Vec3 side = new Vec3(-look.z, 0, look.x).scale(CrecheGame.armZijde(this, p) * 0.35);
                Vec3 at = p.position().add(look.scale(0.5)).add(side).add(0, 0.75, 0);
                setPos(at.x, at.y, at.z);
                setYRot(p.getYRot());
                setYHeadRot(p.getYHeadRot());
                yBodyRot = p.getYRot();
                setDeltaMovement(Vec3.ZERO);
                fallDistance = 0;
            }
        }
        if ((tickCount + getId()) % 20 == 0 && !CrecheGame.hoortErbij(this)) {
            discard();   // (its game is over, or the server restarted: babies only live during a game)
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            CrecheGame.pakOp(this, sp);
        }
        return InteractionResult.SUCCESS;
    }

    // --- never hurt, never saved, never despawns on its own --------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide() && source.getEntity() instanceof Player) {
            level().playSound(null, this, CrecheFeature.BABYGIECHEL.get(), SoundSource.NEUTRAL, 0.8f, 1.2f + random.nextFloat() * 0.3f);
        }
        return !isInvulnerableTo(source) && super.hurt(source, amount);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !isGedragen();
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        if (!isGedragen()) {
            super.doPush(entity);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return CrecheFeature.BABYGIECHEL.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public float getVoicePitch() {
        return 1.5f + random.nextFloat() * 0.3f;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
    }

    /** Where it would like to lie: the crib it came from (server; null when it came from nowhere). */
    @Nullable
    BlockPos wieg;

    // --- GeckoLib ------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 4, state -> {
            if (isGedragen()) {
                return state.setAndContinue(WIEBEL);
            }
            return state.setAndContinue(state.isMoving() ? KRUIP : STIL);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
