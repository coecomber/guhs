package nl.juiced.guhs.feature.speelgoed;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.knus.GuhHooks;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * The invisible seat of a toy (glijbaantje, wip, schommel): one per seat, with a guh or a player on it. Where it is comes
 * from the toy ({@link ToestelBlock#zitPlekWereld}) at every tick, on server and client alike (so the ride is smooth
 * without position packets). A guh's ride ends by itself after a while (then it gets its SPEELGOED hearts, see
 * {@link Spelen#gespeeld}); a player gets off with sneak (on the glijbaantje: at the bottom). Saved with its rider, so a
 * guh on a swing is never lost when the chunk unloads.
 */
public class ZitjeEntity extends Entity {
    private static final EntityDataAccessor<BlockPos> TOESTEL = SynchedEntityData.defineId(ZitjeEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> PLEK = SynchedEntityData.defineId(ZitjeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> START = SynchedEntityData.defineId(ZitjeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RONDJES = SynchedEntityData.defineId(ZitjeEntity.class, EntityDataSerializers.INT);

    /** Server: when a guh's ride is over (game time; 0 = when its rider gets off / the path ends). */
    private long eind;
    /** Server: the player who last pushed this seat (hearts go to the owner anyway). */
    @Nullable
    private UUID duwer;
    private boolean klaar;

    public ZitjeEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    /** Puts rijder on seat plek of the toy at toestel: a guh for duur ticks (rondjes: laps on the glijbaantje). */
    @Nullable
    public static ZitjeEntity zet(ServerLevel level, BlockPos toestel, int plek, LivingEntity rijder, int duur, int rondjes) {
        BlockState state = level.getBlockState(toestel);
        if (!(state.getBlock() instanceof ToestelBlock t) || ToestelBlock.zitje(level, toestel, plek) != null) {
            return null;
        }
        ZitjeEntity z = new ZitjeEntity(SpeelgoedFeature.ZITJE.get(), level);
        z.entityData.set(TOESTEL, toestel.immutable());
        z.entityData.set(PLEK, plek);
        z.entityData.set(START, (int) level.getGameTime());
        z.entityData.set(RONDJES, Math.max(1, rondjes));
        z.eind = duur > 0 ? level.getGameTime() + duur : 0;
        Vec3 p = t.zitPlekWereld(level, toestel, state, z, level.getGameTime());
        if (p == null) {
            return null;
        }
        z.snapTo(p.x, p.y, p.z, t.kijkYaw(level, toestel, state, z, level.getGameTime()), 0);
        level.addFreshEntity(z);
        rijder.stopRiding();
        if (!rijder.startRiding(z, true, true)) {
            z.discard();
            return null;
        }
        if (rijder instanceof GuhEntity g) {
            GuhHooks.bezig(g, 40);
            BandVlaggen.zet(g, BandVlaggen.SPEELT, true);
        }
        return z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TOESTEL, BlockPos.ZERO);
        builder.define(PLEK, 0);
        builder.define(START, 0);
        builder.define(RONDJES, 1);
    }

    public BlockPos toestel() {
        return entityData.get(TOESTEL);
    }

    public int plek() {
        return entityData.get(PLEK);
    }

    /** The game time (as an int) this ride started. */
    public int start() {
        return entityData.get(START);
    }

    public int rondjes() {
        return entityData.get(RONDJES);
    }

    public void duwer(ServerPlayer player) {
        duwer = player.getUUID();
    }

    @Nullable
    public UUID duwer() {
        return duwer;
    }

    /** Ticks since the ride started (with the partial tick). */
    public float rit(float tijd) {
        return tijd - start();
    }

    @Nullable
    public LivingEntity rijder() {
        return getFirstPassenger() instanceof LivingEntity e ? e : null;
    }

    // =====================================================================================================================

    @Override
    public void tick() {
        super.tick();
        BlockState state = level().getBlockState(toestel());
        if (!(state.getBlock() instanceof ToestelBlock t)) {
            if (!level().isClientSide()) {
                klaar(false);
            }
            return;
        }
        long nu = level().getGameTime();
        Vec3 p = t.zitPlekWereld(level(), toestel(), state, this, nu);
        if (p == null) {
            if (!level().isClientSide()) {
                klaar(true);
            }
            return;
        }
        setPos(p.x, p.y, p.z);
        setYRot(t.kijkYaw(level(), toestel(), state, this, nu));
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel sl) {
            LivingEntity r = rijder();
            if (r == null) {
                if (tickCount > 5) {
                    klaar(false);
                }
                return;
            }
            if (r instanceof GuhEntity g) {
                GuhHooks.bezig(g, 40);
            }
            t.rijdt(sl, toestel(), state, this, r);
            if (eind > 0 && nu >= eind) {
                klaar(true);
            }
        }
    }

    /** The ride is over: a guh gets its hearts (when it rode to the end), everyone gets off, the seat goes. */
    public void klaar(boolean beloon) {
        if (klaar || level().isClientSide()) {
            return;
        }
        klaar = true;
        LivingEntity r = rijder();
        BlockState state = level().getBlockState(toestel());
        if (r != null) {
            if (r instanceof GuhEntity g) {
                BandVlaggen.zet(g, BandVlaggen.SPEELT, false);
                GuhHooks.bezig(g, 0);
            }
            if (beloon && r instanceof Mob m && state.getBlock() instanceof ToestelBlock t && level() instanceof ServerLevel sl) {
                ServerPlayer d = duwer == null ? null : sl.getServer().getPlayerList().getPlayer(duwer);
                Spelen.gespeeld(m, t.speeltje(), d);
            }
        }
        ejectPassengers();
        discard();
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && getPassengers().isEmpty()) {
            if (passenger instanceof GuhEntity g) {
                BandVlaggen.zet(g, BandVlaggen.SPEELT, false);
            }
            if (!klaar) {
                klaar = true;
                discard();
            }
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return Vec3.ZERO;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        passenger.setYRot(getYRot());
        passenger.setYHeadRot(getYRot());
        if (passenger instanceof LivingEntity l) {
            l.setYBodyRot(getYRot());
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockState state = level().getBlockState(toestel());
        if (state.getBlock() instanceof ToestelBlock t) {
            return t.uitstap(toestel(), state, plek());
        }
        return position();
    }

    @Override
    public Component getName() {
        BlockState state = level().getBlockState(toestel());
        return state.getBlock() instanceof ToestelBlock ? state.getBlock().getName() : super.getName();
    }

    // the client computes the position itself (see tick): the server's position packets are not needed
    // (1.1.0: was an empty lerpTo override; a handler that ignores the target does the same)
    private final net.minecraft.world.entity.InterpolationHandler interpolation = new net.minecraft.world.entity.InterpolationHandler(this, 0) {
        @Override
        public void interpolateTo(net.minecraft.world.phys.Vec3 position, float yRot, float xRot) {
        }
    };

    @Override
    public net.minecraft.world.entity.InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        (tag).read("Toestel", BlockPos.CODEC).ifPresent(p -> entityData.set(TOESTEL, p));
        entityData.set(PLEK, tag.getIntOr("Plek", 0));
        entityData.set(START, tag.getIntOr("Start", 0));
        entityData.set(RONDJES, Math.max(1, tag.getIntOr("Rondjes", 0)));
        eind = tag.getLongOr("Eind", 0L);
        if (tag.read("Duwer", UUIDUtil.CODEC).isPresent()) {
            duwer = tag.read("Duwer", UUIDUtil.CODEC).orElseThrow();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        tag.store("Toestel", BlockPos.CODEC, toestel());
        tag.putInt("Plek", plek());
        tag.putInt("Start", start());
        tag.putInt("Rondjes", rondjes());
        tag.putLong("Eind", eind);
        if (duwer != null) {
            tag.store("Duwer", UUIDUtil.CODEC, duwer);
        }
    }
}
