package nl.juiced.guhs.feature.kermisritten;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * An invisible seat of a kermis ride, or (index -1) the click box of its ticket post. It places itself every tick from the
 * controller's time: both sides work out the same spot, so the client ignores the server's positions and riders glide.
 * Never saved: the controller makes new ones ({@link KermisritBlockEntity#zorgVoorStoelen}).
 */
public class KermisritStoel extends Entity {
    private static final EntityDataAccessor<BlockPos> DATA_THUIS = SynchedEntityData.defineId(KermisritStoel.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> DATA_INDEX = SynchedEntityData.defineId(KermisritStoel.class, EntityDataSerializers.INT);

    /** Server: true while the ride lets its riders out; only then may they get off during a ride. */
    boolean uitstappen;

    private final InterpolationHandler interpolatie = new InterpolationHandler(this, 0) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            if (rit() == null) {
                setPos(position);
            }
        }
    };

    public KermisritStoel(EntityType<? extends KermisritStoel> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_THUIS, BlockPos.ZERO);
        builder.define(DATA_INDEX, 0);
    }

    void zet(BlockPos thuis, int index, Vec3 plek, KermisritSoort soort) {
        entityData.set(DATA_THUIS, thuis.immutable());
        entityData.set(DATA_INDEX, index);
        setPos(plek);
        if (index < 0) {
            setCustomName(Component.translatable("gui.guhs.kermisrit.loket", Component.translatable("block.guhs." + soort.blokId())));
            setCustomNameVisible(true);
        }
    }

    public BlockPos thuis() {
        return entityData.get(DATA_THUIS);
    }

    public int index() {
        return entityData.get(DATA_INDEX);
    }

    @Nullable
    public KermisritBlockEntity rit() {
        return level().getBlockEntity(thuis()) instanceof KermisritBlockEntity rit ? rit : null;
    }

    @Override
    public void tick() {
        super.tick();
        KermisritBlockEntity rit = rit();
        if (rit != null) {
            setPos(rit.wereldPlek(index(), rit.tijd(0)));
        } else if (!level().isClientSide() && level().hasChunkAt(thuis())) {
            ejectPassengers();            // the controller is gone
            discard();
        }
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolatie;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer speler) {
            KermisritBlockEntity rit = rit();
            if (rit != null) {
                rit.klik(speler, index());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    /** A moving seat is no target: the ride goes too fast to click, and riders can still reach past theirs. */
    @Override
    public boolean isPickable() {
        if (isRemoved()) {
            return false;
        }
        KermisritBlockEntity rit = rit();
        return index() < 0 || (rit != null && !rit.rijdt());
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return index() >= 0 && getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0, 0.1, 0);
    }

    /** Sneaking off at rest: you step out at the gate, not into thin air. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        KermisritBlockEntity rit = rit();
        return rit != null ? rit.uitstapPlek() : super.getDismountLocationForPassenger(passenger);
    }

    /** Off the ride high up after all (a teleport, ...): you float down. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        KermisritBlockEntity rit = rit();
        if (!level().isClientSide() && !uitstappen && rit != null && rit.rijdt() && passenger instanceof LivingEntity levend) {
            levend.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false));
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
