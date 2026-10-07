package nl.juiced.guhs.feature.einde2.appa;

import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice appa: the Momo-guh, a little winged lemur with enormous round guh ears (its own model). Most of the time
 * there is no entity at all: he lives on his owner's right shoulder ({@link Momo}: saved with the player, drawn by a layer
 * on the player). This entity is Momo in the world:
 * <ul>
 *   <li>at home ({@link #isThuis}): he sits where his owner put him and stays there;</li>
 *   <li>on his way ({@code los}): he flutters to his owner and hops onto the shoulder when he gets there;</li>
 *   <li>without an owner (the creative spawn egg, a template): he sits and looks cute.</li>
 * </ul>
 * Nothing hurts him. Like the Appa-guh he carries the number of the Momo he is ({@link Momo#meld}).
 */
public class MomoGuhEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> DATA_EIGENAAR = SynchedEntityData.defineId(MomoGuhEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_THUIS = SynchedEntityData.defineId(MomoGuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_VLIEGT = SynchedEntityData.defineId(MomoGuhEntity.class, EntityDataSerializers.BOOLEAN);
    /** He is in the world at least this long before he hops onto the shoulder (so you see him come). */
    public static final int MIN_LOS = 30;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int gen;
    /** Server: ticks he has been on his way. */
    private int los;
    /** Client: this copy is drawn on a shoulder (sit pose). */
    private boolean opSchouder;

    public MomoGuhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_EIGENAAR, "");
        builder.define(DATA_THUIS, false);
        builder.define(DATA_VLIEGT, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Nullable
    public UUID eigenaar() {
        String s = entityData.get(DATA_EIGENAAR);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isEigenaar(@Nullable Entity e) {
        return e != null && e.getUUID().equals(eigenaar());
    }

    void zetEigenaar(UUID eigenaar, int gen) {
        entityData.set(DATA_EIGENAAR, eigenaar.toString());
        this.gen = gen;
    }

    public int gen() {
        return gen;
    }

    /** Does he sit at home (he stays where he is)? */
    public boolean isThuis() {
        return entityData.get(DATA_THUIS);
    }

    void setThuis(boolean thuis) {
        entityData.set(DATA_THUIS, thuis);
        los = 0;
        if (thuis) {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    /** (Client) this copy is drawn on a shoulder. */
    public void opSchouder(boolean ja) {
        opSchouder = ja;
    }

    @Nullable
    ServerPlayer baas() {
        UUID e = eigenaar();
        return e == null || level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(e);
    }

    // --- every tick -----------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (tickCount % 10 == 5) {
            Momo.meld(this);
            if (isRemoved()) {
                return;
            }
        }
        boolean vliegt = eigenaar() != null && !isThuis() && naarBaas();
        if (vliegt != entityData.get(DATA_VLIEGT)) {
            entityData.set(DATA_VLIEGT, vliegt);
        }
    }

    /** On his way: flutters to his owner's shoulder and hops on. True while he is flying. */
    private boolean naarBaas() {
        ServerPlayer baas = baas();
        if (baas == null || !baas.isAlive() || baas.isSpectator()) {
            return false;                         // (he waits where he is)
        }
        if (baas.level() != level()) {
            Momo.opSchouder(baas, this);          // his owner left for another dimension: he caught up
            return false;
        }
        los++;
        Vec3 doel = baas.position().add(0, baas.getBbHeight() * 0.8, 0);
        Vec3 naar = doel.subtract(position());
        double ver = naar.length();
        if (ver > 40) {                           // too far to flutter: he pops up next to them
            Vec3 bij = baas.position().add(new Vec3(0, 1.2, -1.5).yRot(-baas.getYRot() * Mth.DEG_TO_RAD));
            snapTo(bij.x, bij.y, bij.z, baas.getYRot(), 0);
            return true;
        }
        if (ver < 1.3 && los >= MIN_LOS) {
            Momo.opSchouder(baas, this);
            return false;
        }
        Vec3 want = ver < 1.3 ? Vec3.ZERO : naar.normalize().scale(Math.min(0.42, 0.08 + ver * 0.06));
        Vec3 m = getDeltaMovement().scale(0.7).add(want.scale(0.3));
        setDeltaMovement(m);
        if (m.horizontalDistanceSqr() > 1e-4) {
            float yaw = (float) Math.toDegrees(Math.atan2(-m.x, m.z));
            setRot(yaw, 0);
            this.yBodyRot = this.yHeadRot = yaw;
        }
        return true;
    }

    @Override
    public void travel(Vec3 input) {
        if (!level().isClientSide() && entityData.get(DATA_VLIEGT)) {
            move(MoverType.SELF, getDeltaMovement());      // (the flutter of naarBaas: no gravity)
            calculateEntityAnimation(true);
            resetFallDistance();
            return;
        }
        super.travel(Vec3.ZERO);
    }

    // --- clicks ---------------------------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        UUID eigenaar = eigenaar();
        boolean baas = isEigenaar(player);
        if (player.getItemInHand(hand).is(Items.NAME_TAG) && baas) {
            return InteractionResult.PASS;                     // (the name tag names him; Momo.meld remembers the name)
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (eigenaar == null) {
            kwetter();
            zeg(sp, Component.translatable("gui.guhs.einde2_appa.momo.wild"));
        } else if (!baas) {
            kwetter();
            zeg(sp, Component.translatable("gui.guhs.einde2_appa.momo.niet_van_jou", Appa.naamVan(level().getServer(), eigenaar)));
        } else if (player.isSecondaryUseActive()) {
            Momo.opSchouder(sp, this);
        } else {
            kwetter();
            triggerAnim("actie", "blij");
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.9, getZ(), 3, 0.2, 0.15, 0.2, 0.02);
            }
            zeg(sp, Component.translatable("gui.guhs.einde2_appa.momo.aai", getDisplayName()));
        }
        return InteractionResult.SUCCESS;
    }

    private static void zeg(ServerPlayer p, Component tekst) {
        p.sendOverlayMessage(tekst.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public void kwetter() {
        playSound(E2AppaFeature.MOMO.get(), 0.9f, 0.9f + random.nextFloat() * 0.3f);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return E2AppaFeature.MOMO.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    // --- he is never hurt or lost ----------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);     // (/kill: Momo.roep makes him again)
        }
        if (eigenaar() == null && source.getEntity() instanceof ServerPlayer p && p.getAbilities().instabuild && p.isShiftKeyDown()) {
            discard();                                          // (creative: sneak-hit removes one without an owner)
        }
        return false;
    }

    @Override
    protected void onBelowWorld() {
        setPos(getX(), level().getMinY() + 4, getZ());
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        Momo.vergeet(this);
        super.remove(reason);
    }

    // --- saving ---------------------------------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Eigenaar", entityData.get(DATA_EIGENAAR));
        tag.putInt("Gen", gen);
        tag.putBoolean("Thuis", isThuis());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_EIGENAAR, tag.getStringOr("Eigenaar", ""));
        gen = tag.getIntOr("Gen", 0);
        entityData.set(DATA_THUIS, tag.getBooleanOr("Thuis", false));
    }

    // --- GeckoLib -------------------------------------------------------------------------------------------------------------

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ZIT = RawAnimation.begin().thenLoop("zit");
    private static final RawAnimation VLIEG = RawAnimation.begin().thenLoop("vlieg");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MomoGuhEntity>("beweeg", 3, state -> state.setAndContinue(
                opSchouder || isThuis() ? ZIT : entityData.get(DATA_VLIEGT) ? VLIEG : IDLE)));
        controllers.add(new AnimationController<MomoGuhEntity>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("jat", RawAnimation.begin().thenPlay("jat")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
