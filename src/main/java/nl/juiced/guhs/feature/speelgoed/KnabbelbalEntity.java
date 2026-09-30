package nl.juiced.guhs.feature.speelgoed;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.registry.ModItems;

/**
 * De Knabbelbal: a fluffy pink ball with guh ears, a little guh face and a kaasknabbel inside (you can see it through the
 * round window in its tummy). It rolls, bounces off walls, floats in water. Guhs nudge it around ({@link KnabbelbalSpel})
 * until the knabbel pops out, and then they eat it; players kick it (left-click), nudge it (right-click), fill it with a
 * kaasknabbel (right-click with kaas knabbels) and pick it up (sneak + right-click with an empty hand).
 */
public class KnabbelbalEntity extends Entity {
    public static final float SIZE = 0.5f;
    public static final double GRAVITY = 0.05, FRICTION = 0.93, AIR = 0.985, BOUNCE = 0.55, SCHOP = 0.8;
    private static final EntityDataAccessor<Boolean> VOL = SynchedEntityData.defineId(KnabbelbalEntity.class, EntityDataSerializers.BOOLEAN);

    /** Nudges since it was filled (the more, the likelier the knabbel pops out). */
    private int duwtjes;
    private int duwRust;

    // client: smooth movement and rolling
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    public float roll, oRoll, rollYaw;

    public KnabbelbalEntity(EntityType<? extends KnabbelbalEntity> type, Level level) {
        super(type, level);
    }

    public static KnabbelbalEntity maak(ServerLevel level, Vec3 pos, boolean vol) {
        KnabbelbalEntity bal = new KnabbelbalEntity(SpeelgoedFeature.KNABBELBAL.get(), level);
        bal.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360, 0);
        bal.setVol(vol);
        level.addFreshEntity(bal);
        return bal;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(VOL, true);
    }

    public boolean isVol() {
        return entityData.get(VOL);
    }

    public void setVol(boolean vol) {
        entityData.set(VOL, vol);
        duwtjes = 0;
    }

    public boolean rolt() {
        return getDeltaMovement().horizontalDistanceSqr() > 0.0004 || !onGround();
    }

    // =====================================================================================================================
    // pushing it around
    // =====================================================================================================================

    /**
     * A nudge (by a guh's snoet, a player's foot...): speed in that direction (horizontal), a little hop. A guh's nudge may
     * make the knabbel pop out of a full ball: then the knabbel item is returned (it lies next to the ball).
     */
    @Nullable
    public ItemEntity duw(Vec3 richting, double kracht, @Nullable Entity wie) {
        Vec3 r = new Vec3(richting.x, 0, richting.z);
        if (r.lengthSqr() < 1e-6) {
            r = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
        }
        r = r.normalize().scale(kracht);
        setDeltaMovement(r.x, Math.max(getDeltaMovement().y, 0.12 + kracht * 0.2), r.z);
        hasImpulse = true;
        duwRust = 6;
        level().playSound(null, blockPosition(), SpeelgoedFeature.BAL.get(), SoundSource.NEUTRAL, 0.7f, 1.1f + random.nextFloat() * 0.3f);
        if (isVol() && wie != null && !level().isClientSide()) {
            duwtjes++;
            int kans = wie instanceof Player ? 5 : Math.max(1, 5 - duwtjes);
            if (random.nextInt(kans) == 0) {
                return plop();
            }
        }
        return null;
    }

    /** The knabbel pops out: a kaasknabbel lies next to the ball, the ball is empty. */
    @Nullable
    public ItemEntity plop() {
        if (!isVol() || !(level() instanceof ServerLevel sl)) {
            return null;
        }
        setVol(false);
        ItemEntity knabbel = new ItemEntity(sl, getX(), getY() + 0.3, getZ(), new ItemStack(ModItems.KAAS_KNABBELS.get()));
        knabbel.setDeltaMovement((random.nextDouble() - 0.5) * 0.15, 0.25, (random.nextDouble() - 0.5) * 0.15);
        knabbel.setPickUpDelay(20);
        sl.addFreshEntity(knabbel);
        sl.playSound(null, blockPosition(), SpeelgoedFeature.PLOP.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.KAAS_KNABBELS.get())), getX(), getY() + 0.3, getZ(),
                8, 0.15, 0.1, 0.15, 0.08);
        return knabbel;
    }

    /** A kick (left-click). */
    public void schop(ServerPlayer player) {
        Vec3 kijk = player.getLookAngle();
        duw(kijk, SCHOP, player);
        setDeltaMovement(getDeltaMovement().add(0, 0.15 + Math.max(0, kijk.y) * 0.4, 0));
        Spelen.geschopt(player, this);
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof ServerPlayer player) {
            schop(player);
        }
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.KAAS_KNABBELS.get()) && !isVol()) {
            stack.consume(1, player);
            setVol(true);
            level().playSound(null, blockPosition(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.NEUTRAL, 1f, 1.3f);
            player.sendOverlayMessage(Component.translatable("gui.guhs.speelgoed.bal.gevuld").withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            ItemStack bal = KnabbelbalItem.stack(isVol());
            if (!player.addItem(bal)) {
                player.drop(bal, false);
            }
            level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
            discard();
            return InteractionResult.CONSUME;
        }
        duw(position().subtract(player.position()), 0.35, player);
        return InteractionResult.CONSUME;
    }

    /** Walking into it rolls it along. */
    @Override
    public void push(Entity other) {
        if (level().isClientSide() || duwRust > 0 || !(other instanceof LivingEntity) || other.isPassenger()) {
            return;
        }
        Vec3 weg = position().subtract(other.position());
        double v = Math.max(0.18, other.getDeltaMovement().horizontalDistance() * 1.6);
        duw(weg, Math.min(0.5, v), null);
    }

    // =====================================================================================================================
    // rolling (server) and drawing it smoothly (client)
    // =====================================================================================================================

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientTick();
            return;
        }
        if (duwRust > 0) {
            duwRust--;
        }
        Vec3 v = getDeltaMovement();
        boolean nat = isInWater();
        if (!onGround() || v.lengthSqr() > 1e-5 || nat) {
            v = nat ? new Vec3(v.x * 0.9, Math.min(0.08, v.y + 0.03), v.z * 0.9) : v.add(0, -GRAVITY, 0);
            Vec3 van = position();
            move(MoverType.SELF, v);
            Vec3 bewogen = position().subtract(van);
            double vx = v.x, vy = v.y, vz = v.z;
            if (horizontalCollision) {
                if (Math.abs(bewogen.x - v.x) > 1e-5) {
                    vx = -v.x * BOUNCE;
                }
                if (Math.abs(bewogen.z - v.z) > 1e-5) {
                    vz = -v.z * BOUNCE;
                }
                if (Math.abs(v.x) + Math.abs(v.z) > 0.12) {
                    level().playSound(null, blockPosition(), SpeelgoedFeature.BAL.get(), SoundSource.NEUTRAL, 0.4f, 1.4f);
                }
            }
            if (verticalCollisionBelow) {
                vy = v.y < -0.25 ? -v.y * 0.4 : 0;
                vx *= FRICTION;
                vz *= FRICTION;
            } else if (verticalCollision) {
                vy = 0;
            } else {
                vx *= AIR;
                vz *= AIR;
            }
            if (onGround() && Math.abs(vy) < 0.01 && vx * vx + vz * vz < 0.00025) {
                vx = vz = 0;
            }
            setDeltaMovement(vx, vy, vz);
        }
        if (getY() < level().getMinY() - 16) {
            discard();
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpSteps = steps;
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    private void clientTick() {
        if (lerpSteps > 0) {
            double d = 1.0 / lerpSteps;
            setPos(getX() + (lerpX - getX()) * d, getY() + (lerpY - getY()) * d, getZ() + (lerpZ - getZ()) * d);
            lerpSteps--;
        }
        oRoll = roll;
        double dx = getX() - xo, dz = getZ() - zo;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 1e-4 && dist < 4) {
            rollYaw = (float) Math.atan2(dx, dz);
            roll += (float) (dist / (SIZE / 2));
        }
    }

    // =====================================================================================================================

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public float maxUpStep() {
        return 0.55f;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return KnabbelbalItem.stack(isVol());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(VOL, !tag.contains("Vol") || tag.getBooleanOr("Vol", false));
        duwtjes = tag.getIntOr("Duwtjes", 0);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Vol", isVol());
        tag.putInt("Duwtjes", duwtjes);
    }
}
