package nl.juiced.guhs.feature.guheinde;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import org.joml.Vector3f;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * The starved Enderguh that Opper-Mika rides: a huge Enderguh, grey and thin because the Mika's never give it a single
 * kaasknabbel. It can't be hurt (you don't hit a guh!). Every knabbelkristal you smash makes it a bit more vahoeg (its
 * colours come back, {@link #getVahoeg}); with all of them gone it's worn out and lands on the Knabbelberg: feed it a
 * kaasknabbel and VAHOEG! it throws Opper-Mika off. Flies by itself (no pathfinding, straight through the air like the
 * ender dragon), see {@link Toestand}.
 */
public class HongerigeEnderguhEntity extends PathfinderMob implements GeoEntity {
    public static final float SCALE = 3.2f;
    /** Vahoeg levels: 0 = grey and starved ... MAX_VAHOEG = all colours back. */
    public static final int MAX_VAHOEG = 8;

    /** What it's doing. */
    public enum Toestand { CIRKEL, DUIK, LANDEN, UITGEPUT, VRIJ }

    private static final EntityDataAccessor<Integer> DATA_VAHOEG = SynchedEntityData.defineId(HongerigeEnderguhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOESTAND = SynchedEntityData.defineId(HongerigeEnderguhEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.guh.fly");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.guh.ender_flap");
    private static final RawAnimation REST = RawAnimation.begin().thenLoop("animation.guh.ender_rest");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** The middle of the fight (the Knabbelberg's plateau) and where it lands. Set by GuheindeGevecht. */
    private BlockPos center = BlockPos.ZERO;
    private BlockPos perch = BlockPos.ZERO;
    private int stateTicks;
    private double angle;
    @Nullable
    private Player diveTarget;
    private int messageCooldown;

    public HongerigeEnderguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.guhs.hongerige_enderguh"));
        // the SCALE attribute (SCALE x the size in GuheindeFeature) only reaches the hitbox after a refresh: without
        // this it kept a little 0.9x0.8 guh box inside a model of almost four blocks
        this.refreshDimensions();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.SCALE, SCALE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VAHOEG, 0);
        builder.define(DATA_TOESTAND, Toestand.CIRKEL.ordinal());
    }

    public int getVahoeg() {
        return this.entityData.get(DATA_VAHOEG);
    }

    public void setVahoeg(int vahoeg) {
        this.entityData.set(DATA_VAHOEG, Math.max(0, Math.min(MAX_VAHOEG, vahoeg)));
    }

    public Toestand getToestand() {
        return Toestand.values()[this.entityData.get(DATA_TOESTAND)];
    }

    public void setToestand(Toestand toestand) {
        this.entityData.set(DATA_TOESTAND, toestand.ordinal());
        this.stateTicks = 0;
    }

    public void setArena(BlockPos center, BlockPos perch) {
        this.center = center;
        this.perch = perch;
    }

    public BlockPos perch() {
        return perch;
    }

    /** A knabbelkristal was smashed: a bit of colour comes back. */
    public void addVahoeg() {
        setVahoeg(getVahoeg() + 1);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(new DustParticleOptions(0xFF8CBF /* 1, 0.55, 0.75 */, 2f), getX(), getY() + getBbHeight() / 2, getZ(), 40,
                    getBbWidth() / 2, getBbHeight() / 2, getBbWidth() / 2, 0.1);
            this.playSound(ModSounds.GUH_HAPPY.get(), 2f, 0.6f);
        }
    }

    /** All crystals are gone: too tired to fly, it lands on the Knabbelberg and waits for a snack. */
    public void exhaust() {
        setVahoeg(Math.max(getVahoeg(), MAX_VAHOEG - 1));
        setToestand(Toestand.UITGEPUT);
    }

    // ------------------------------------------------------------------------------------------------------------
    // flying
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        this.setNoGravity(true);
        if (messageCooldown > 0) {
            messageCooldown--;
        }
    }

    @Override
    public void travel(Vec3 input) {
        // (moves itself in customServerAiStep; clients just follow the server's position)
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        this.calculateEntityAnimation(true);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        stateTicks++;
        double top = center.getY() + 22;
        switch (getToestand()) {
            case CIRKEL -> {
                angle += 0.012;
                double r = 38;
                flyTo(new Vec3(center.getX() + Math.cos(angle) * r, top + Math.sin(angle * 3) * 4, center.getZ() + Math.sin(angle) * r), 0.55);
                if (stateTicks > 240 && this.random.nextInt(60) == 0) {
                    Player target = this.level().getNearestPlayer(center.getX(), center.getY(), center.getZ(), 90, p -> !((Player) p).isCreative() && !p.isSpectator());
                    if (target != null && this.random.nextInt(5) < 3) {
                        diveTarget = target;
                        setToestand(Toestand.DUIK);
                    } else {
                        setToestand(Toestand.LANDEN);
                    }
                }
            }
            case DUIK -> {
                if (diveTarget == null || !diveTarget.isAlive() || stateTicks > 200) {
                    setToestand(Toestand.CIRKEL);
                    break;
                }
                flyTo(diveTarget.position().add(0, 2.5, 0), 0.8);
                if (this.distanceToSqr(diveTarget) < 16) {
                    bodySlam();
                    setToestand(Toestand.CIRKEL);
                }
            }
            case LANDEN, UITGEPUT -> {
                Vec3 spot = Vec3.atBottomCenterOf(perch).add(0, 1, 0);
                if (this.position().distanceToSqr(spot) > 1.5) {
                    flyTo(spot, getToestand() == Toestand.UITGEPUT ? 0.35 : 0.5);
                } else {
                    this.setDeltaMovement(Vec3.ZERO);
                    this.setPos(spot);
                }
                if (getToestand() == Toestand.UITGEPUT) {
                    if (stateTicks % 40 == 0 && this.level() instanceof ServerLevel level) {
                        level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + getBbHeight(), getZ(), 6, 0.6, 0.2, 0.6, 0.01);
                        this.playSound(ModSounds.GUH_AMBIENT.get(), 1.5f, 0.5f);
                    }
                    if (stateTicks % 300 == 60 && this.getFirstPassenger() instanceof OpperMikaEntity mika) {
                        mika.shout("gui.guhs.guheinde.opper.lui");
                    }
                } else if (stateTicks > 200) {
                    setToestand(Toestand.CIRKEL);
                }
            }
            case VRIJ -> {
                angle += 0.02;
                flyTo(new Vec3(center.getX() + Math.cos(angle) * 20, top + 14, center.getZ() + Math.sin(angle) * 20), 0.5);
                if (stateTicks % 20 == 0 && this.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 2, 1, 0.5, 1, 0);
                }
            }
        }
    }

    private void flyTo(Vec3 to, double speed) {
        Vec3 d = to.subtract(this.position());
        double len = d.length();
        Vec3 want = len < 0.01 ? Vec3.ZERO : d.scale(Math.min(speed, len) / len);
        Vec3 motion = this.getDeltaMovement().scale(0.8).add(want.scale(0.2));
        this.setDeltaMovement(motion);
        if (motion.horizontalDistanceSqr() > 0.001) {
            float yaw = (float) (Math.atan2(-motion.x, motion.z) * 180 / Math.PI);
            this.setYRot(OpperMikaEntity.lerpYaw(this.getYRot(), yaw, 6f));
            this.yBodyRot = this.yHeadRot = this.getYRot();
        }
    }

    /** Swooping through the players: a big wobbly push (it's a plush guh, it hardly hurts). */
    private void bodySlam() {
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2),
                e -> e instanceof Player p && !p.isCreative() && !p.isSpectator())) {
            e.hurtOrSimulate(this.damageSources().mobAttack(this), 3f);
            Vec3 away = e.position().subtract(this.position()).normalize();
            e.knockback(2.0, -away.x, -away.z);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.5, 0));
            e.hurtMarked = true;
        }
        this.playSound(SoundEvents.WIND_CHARGE_BURST.value(), 2f, 0.6f);
    }

    public boolean isFlying() {
        Toestand t = getToestand();
        return !((t == Toestand.LANDEN || t == Toestand.UITGEPUT) && this.position().distanceToSqr(Vec3.atBottomCenterOf(perch).add(0, 1, 0)) < 2.5);
    }

    // ------------------------------------------------------------------------------------------------------------
    // players: no hitting the guh; feeding it when it's worn out
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer player && messageCooldown <= 0) {
            messageCooldown = 60;
            player.sendOverlayMessage(Component.translatable("gui.guhs.guheinde.enderguh.niet_meppen").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel serverLevel, DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean knabbel = stack.is(ModItems.KAAS_KNABBELS.get()) || stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get());
        if (getToestand() != Toestand.UITGEPUT) {
            player.sendOverlayMessage(Component.translatable(getToestand() == Toestand.VRIJ ? "gui.guhs.guheinde.enderguh.blij"
                    : "gui.guhs.guheinde.enderguh.bang").withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.CONSUME;
        }
        if (!knabbel) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.guheinde.enderguh.honger").withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.CONSUME;
        }
        stack.consume(1, player);
        feed((ServerPlayer) player);
        return InteractionResult.CONSUME;
    }

    /** VAHOEG! A kaasknabbel at last: all colours back, and Opper-Mika goes flying. */
    public void feed(@Nullable ServerPlayer feeder) {
        setVahoeg(MAX_VAHOEG);
        setToestand(Toestand.VRIJ);
        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 20, 2, 1, 2, 0);
        level.sendParticles(new DustParticleOptions(0xFF8CBF /* 1, 0.55, 0.75 */, 3f), getX(), getY() + 1, getZ(), 80, 2, 1.5, 2, 0.2);
        this.playSound(ModSounds.GUH_HAPPY.get(), 3f, 0.7f);
        this.playSound(SoundEvents.PLAYER_LEVELUP, 2f, 0.8f);
        Entity rider = this.getFirstPassenger();
        if (rider instanceof OpperMikaEntity mika) {
            mika.stopRiding();
            Vec3 away = new Vec3(this.random.nextGaussian(), 0, this.random.nextGaussian()).normalize();
            mika.setDeltaMovement(away.x * 1.2, 1.0, away.z * 1.2);
            mika.hurtMarked = true;
            mika.shout("gui.guhs.guheinde.opper.eraf");
        }
        GuheindeGevecht fight = GuheindeGevecht.of(level);
        if (fight != null) {
            fight.onEnderguhFed(this, feeder);
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return null; // it flies by itself; Opper-Mika just holds on
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof OpperMikaEntity && this.getPassengers().isEmpty();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Vahoeg", getVahoeg());
        tag.putString("Toestand", getToestand().name());
        tag.putLong("Center", center.asLong());
        tag.putLong("Perch", perch.asLong());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setVahoeg(tag.getIntOr("Vahoeg", 0));
        try {
            setToestand(Toestand.valueOf(tag.getStringOr("Toestand", "")));
        } catch (IllegalArgumentException e) {
            setToestand(Toestand.CIRKEL);
        }
        center = BlockPos.of(tag.getLongOr("Center", 0L));
        perch = BlockPos.of(tag.getLongOr("Perch", 0L));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GUH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GUH_HURT.get();
    }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    // --- GeckoLib: the guh model with its ender wings ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 5, state -> state.setAndContinue(isFlying() ? FLY : IDLE)));
        controllers.add(new AnimationController<>("wings", 4, state -> state.setAndContinue(isFlying() ? FLAP : REST)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
