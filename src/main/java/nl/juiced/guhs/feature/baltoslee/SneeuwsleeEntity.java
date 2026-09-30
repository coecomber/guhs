package nl.juiced.guhs.feature.baltoslee;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.registry.ModItems;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Your own sneeuwslee (guhs:sneeuwslee, from the item {@link SneeuwsleeItem}): a pink-and-blue sled pulled by four
 * guh-sledehondjes, to ride through the snowy biomes after the Nomguh story. You stand on the runners: W = "Hup, hup!",
 * S = brake, A/D steer. It only runs on snow and ice ({@link #SNEEUW}); on grass or stone the dogs sit down and wait for
 * snow ("De sledehondjes willen sneeuw onder hun pootjes, njeg!"). Sneak + right-click (the owner) puts it back in your pocket.
 * <p>
 * Driven like a boat: the rider's game moves it and tells the server (vanilla's vehicle packets); without a rider the server
 * lets it glide to a stop. Feeding the dogs a kaasknabbel makes them very happy (hearts, a little "woef-njeg").
 */
public class SneeuwsleeEntity extends Entity implements GeoEntity {
    /** The blocks the dogs run on: snow (layers, blocks, powder snow) and ice. */
    public static final TagKey<Block> SNEEUW = TagKey.create(Registries.BLOCK, Guhs.id("baltoslee/sneeuw"));
    /** Blocks per tick: flat out on snow, gliding on its own, off the snow. */
    public static final double TOP = 0.55, LANGZAAM = 0.045;
    public static final int METERS_VOOR_ADVANCEMENT = 200;

    private static final EntityDataAccessor<Optional<UUID>> DATA_EIGENAAR = SynchedEntityData.defineId(SneeuwsleeEntity.class,
            EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_OP_SNEEUW = SynchedEntityData.defineId(SneeuwsleeEntity.class, EntityDataSerializers.BOOLEAN);

    /** The rider's keys (set by the client). */
    @Nullable
    public static Supplier<SleeRijden.Invoer> invoer;
    /** Every client tick (bells, runners, snow spray). */
    @Nullable
    public static java.util.function.Consumer<SneeuwsleeEntity> clientTick;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Forward speed (blocks per tick). */
    public double snelheid;
    private int geenSneeuwBericht, lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYRot;
    private Vec3 vorige;

    // --- client only: for drawing ---
    public final Vec3[] honden = new Vec3[4];
    public final float[] hondYaw = new float[4];
    public float lean, leanO;
    public int loopTik;
    public float getoondeSnelheid, afstand;

    public SneeuwsleeEntity(EntityType<? extends SneeuwsleeEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DATA_EIGENAAR, Optional.empty());
        b.define(DATA_OP_SNEEUW, true);
    }

    public void zetEigenaar(@Nullable UUID id) {
        entityData.set(DATA_EIGENAAR, Optional.ofNullable(id));
    }

    @Nullable
    public UUID eigenaar() {
        return entityData.get(DATA_EIGENAAR).orElse(null);
    }

    /** Are the dogs on snow or ice (synced: they sit down when not)? */
    public boolean opSneeuwGezien() {
        return entityData.get(DATA_OP_SNEEUW);
    }

    /** Is there snow or ice under the sled (or the snow layer it stands in)? */
    public boolean opSneeuw() {
        BlockPos p = blockPosition();
        BlockState in = level().getBlockState(p);
        if (in.is(SNEEUW)) {
            return true;
        }
        BlockState onder = level().getBlockState(p.below());
        if (onder.is(SNEEUW)) {
            return true;
        }
        return level().getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ())).is(SNEEUW);
    }

    // --- driving ---------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (lerpSteps > 0 && !isControlledByLocalInstance()) {
            double f = 1.0 / lerpSteps;
            setPos(getX() + (lerpX - getX()) * f, getY() + (lerpY - getY()) * f, getZ() + (lerpZ - getZ()) * f);
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYRot - getYRot()) * (float) f);
            lerpSteps--;
        }
        if (isControlledByLocalInstance()) {
            SleeRijden.Invoer in = getControllingPassenger() instanceof Player && level().isClientSide && invoer != null ? invoer.get() : SleeRijden.Invoer.NIKS;
            rijd(in);
        } else if (level().isClientSide) {
            setDeltaMovement(Vec3.ZERO);
        }
        if (!level().isClientSide) {
            serverTick();
        } else {
            loopTik++;
            Vec3 d = getDeltaMovement();
            getoondeSnelheid = (float) Math.sqrt(d.x * d.x + d.z * d.z);
            if (!isControlledByLocalInstance()) {
                getoondeSnelheid = (float) Math.sqrt((getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo));
            }
            afstand += getoondeSnelheid;
            if (clientTick != null) {
                clientTick.accept(this);
            }
        }
    }

    /**
     * One tick of driving with these keys: faster on snow, slow elsewhere, turning, gravity, stepping up snow layers. (The
     * rider's game calls it every tick; tests call it on the server.)
     */
    public void rijd(SleeRijden.Invoer in) {
        boolean sneeuw = opSneeuw();
        double max = sneeuw ? TOP : LANGZAAM;
        double vooruit = Mth.clamp(in.vooruit(), -1, 1);
        if (vooruit < -0.1) {
            snelheid = Math.max(0, snelheid - 0.03);
        } else if (vooruit > 0.1) {
            snelheid = snelheid < max ? Math.min(max, snelheid + (sneeuw ? 0.012 : 0.004)) : Math.max(max, snelheid - 0.05);
        } else {
            snelheid *= sneeuw ? 0.985 : 0.7;                        // (on snow it glides on and on)
        }
        if (!sneeuw && snelheid > LANGZAAM) {
            snelheid = Math.max(LANGZAAM, snelheid * 0.75);
        }
        double draai = Mth.clamp(in.stuur(), -1, 1) * (2.2 + 5.5 * Math.min(1, snelheid / 0.3));
        setYRot(getYRot() + (float) draai);
        leanO = lean;
        lean += (float) ((-draai * 2.5 - lean) * 0.2);
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        double vy = onGround() ? -0.04 : Math.max(-0.9, getDeltaMovement().y - 0.08);
        setDeltaMovement(dir.x * snelheid, vy, dir.z * snelheid);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision) {
            snelheid *= 0.4;
        }
    }

    private void serverTick() {
        boolean sneeuw = opSneeuw();
        if (sneeuw != opSneeuwGezien()) {
            entityData.set(DATA_OP_SNEEUW, sneeuw);
        }
        Vec3 nu = position();
        if (getControllingPassenger() instanceof ServerPlayer rijder) {
            double gereden = vorige == null ? 0 : Math.sqrt(Math.pow(nu.x - vorige.x, 2) + Math.pow(nu.z - vorige.z, 2));
            if (sneeuw && gereden > 0.02 && gereden < 2) {
                CompoundTag d = SleeRit.data(rijder);
                float meters = d.getFloat("Meters") + (float) gereden;
                d.putFloat("Meters", meters);
                if (meters >= METERS_VOOR_ADVANCEMENT) {
                    SleeRit.adv(rijder, "balto_slee_eigen");
                }
            }
            if (!sneeuw && gereden < 0.1 && tickCount - geenSneeuwBericht > 80) {
                geenSneeuwBericht = tickCount;
                rijder.displayClientMessage(Component.translatable("gui.guhs.baltoslee.eigen.geen_sneeuw").withStyle(ChatFormatting.AQUA), true);
            }
        }
        vorige = nu;
    }

    @Override
    public boolean isControlledByLocalInstance() {
        return getControllingPassenger() instanceof Player p ? p.isLocalPlayer() : !level().isClientSide;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 10;
    }

    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    // --- riding --------------------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        return dir.scale(-SleeEntity.ACHTER).add(0, SleeEntity.OP_DE_LATTEN, 0);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof LivingEntity living) {
            living.setYBodyRot(getYRot());
            float d = Mth.wrapDegrees(passenger.getYRot() - getYRot());
            float c = Mth.clamp(d, -110f, 110f);
            passenger.yRotO += c - d;
            passenger.setYRot(passenger.getYRot() + c - d);
            passenger.setYHeadRot(passenger.getYRot());
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide && (player.getUUID().equals(eigenaar()) || eigenaar() == null || player.hasPermissions(2))) {
                ejectPassengers();
                if (!player.getAbilities().instabuild || !player.getInventory().contains(new ItemStack(BaltoSleeFeature.SNEEUWSLEE.get()))) {
                    ItemStack item = new ItemStack(BaltoSleeFeature.SNEEUWSLEE.get());
                    if (!player.getInventory().add(item)) {
                        spawnAtLocation(item);
                    }
                }
                level().playSound(null, blockPosition(), BaltoSleeFeature.BELLEN.get(), SoundSource.PLAYERS, 0.7f, 1.4f);
                discard();
            } else if (!level().isClientSide) {
                player.displayClientMessage(Component.translatable("gui.guhs.baltoslee.eigen.niet_van_jou").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.is(ModItems.KAAS_KNABBELS.get())) {
            if (!level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX() + Vec3.directionFromRotation(0, getYRot()).x * 2.5, getY() + 0.8,
                        getZ() + Vec3.directionFromRotation(0, getYRot()).z * 2.5, 6, 0.8, 0.3, 0.8, 0);
                level().playSound(null, blockPosition(), BaltoSleeFeature.WOEF.get(), SoundSource.NEUTRAL, 1f, 1.2f);
                player.displayClientMessage(Component.translatable("gui.guhs.baltoslee.eigen.knabbel").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!isVehicle() && !player.isPassenger()) {
            if (!level().isClientSide) {
                player.startRiding(this);
                if (player instanceof ServerPlayer sp && !sp.getPersistentData().getBoolean("guhs_baltoslee_uitleg")) {
                    sp.getPersistentData().putBoolean("guhs_baltoslee_uitleg", true);
                    sp.sendSystemMessage(Component.translatable("gui.guhs.baltoslee.eigen.uitleg").withStyle(ChatFormatting.AQUA));
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
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

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(BaltoSleeFeature.SNEEUWSLEE.get());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Eigenaar")) {
            zetEigenaar(tag.getUUID("Eigenaar"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID e = eigenaar();
        if (e != null) {
            tag.putUUID("Eigenaar", e);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
