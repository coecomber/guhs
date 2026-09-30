package nl.juiced.guhs.entity;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.slee.SleePath;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The guh sled: rides along sled rails ({@link SleePath}), forwards or backwards, at 3 speeds. The rider controls it
 * with a little panel (right-click while riding). At the end of the line it stops and turns around.
 * <p>
 * Both sides move the sled themselves (the path is the same on both), so riding is smooth; the server sends its
 * piece and position along it now and then to keep them together. Locked sleds (Guhland) can't be taken.
 */
public class GuhSleeEntity extends Entity implements GeoEntity {
    public static final float[] SPEEDS = {0.25f, 0.5f, 0.85f};

    private static final EntityDataAccessor<BlockPos> DATA_PIECE = SynchedEntityData.defineId(GuhSleeEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> DATA_ON_RAIL = SynchedEntityData.defineId(GuhSleeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_T = SynchedEntityData.defineId(GuhSleeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_FORWARD = SynchedEntityData.defineId(GuhSleeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING = SynchedEntityData.defineId(GuhSleeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SPEED = SynchedEntityData.defineId(GuhSleeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LOCKED = SynchedEntityData.defineId(GuhSleeEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private SleePath.Piece piece;
    private double t;

    public GuhSleeEntity(EntityType<? extends GuhSleeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PIECE, BlockPos.ZERO);
        builder.define(DATA_ON_RAIL, false);
        builder.define(DATA_T, 0f);
        builder.define(DATA_FORWARD, true);
        builder.define(DATA_RUNNING, false);
        builder.define(DATA_SPEED, 1);
        builder.define(DATA_LOCKED, false);
    }

    // --- state ----------------------------------------------------------------------------------------------------

    public boolean isRunning() {
        return entityData.get(DATA_RUNNING);
    }

    public int getSpeed() {
        return entityData.get(DATA_SPEED);
    }

    public boolean isForward() {
        return entityData.get(DATA_FORWARD);
    }

    public boolean isLocked() {
        return entityData.get(DATA_LOCKED);
    }

    public void setLocked(boolean locked) {
        entityData.set(DATA_LOCKED, locked);
    }

    public void setRunning(boolean running) {
        entityData.set(DATA_RUNNING, running);
        sync();
    }

    public void setSpeed(int speed) {
        entityData.set(DATA_SPEED, Mth.clamp(speed, 1, SPEEDS.length));
    }

    public void reverse() {
        entityData.set(DATA_FORWARD, !isForward());
        sync();
    }

    /** Puts the sled on a piece of rail (at the point nearest to where it is), facing the way `look` points. */
    public void putOn(SleePath.Piece newPiece, Vec3 near, Vec3 look) {
        piece = newPiece;
        t = newPiece.closest(near);
        entityData.set(DATA_FORWARD, newPiece.at(t).heading().dot(look) >= 0);
        entityData.set(DATA_ON_RAIL, true);
        sync();
        moveAlong(0);
    }

    @Nullable
    public SleePath.Piece getPiece() {
        return piece;
    }

    public double getT() {
        return t;
    }

    private void sync() {
        if (!level().isClientSide) {
            entityData.set(DATA_ON_RAIL, piece != null);
            if (piece != null) {
                entityData.set(DATA_PIECE, piece.anchor());
            }
            entityData.set(DATA_T, (float) t);
        }
    }

    /**
     * The client takes the server's piece and spot, unless it's riding along close to it anyway (the server's news is
     * always a little old, so snapping to it every time would make the ride jerky).
     */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!level().isClientSide) {
            return;
        }
        if (key == DATA_ON_RAIL && !entityData.get(DATA_ON_RAIL)) {
            piece = null;
        } else if (key == DATA_T && entityData.get(DATA_ON_RAIL)) {
            SleePath.Piece server = SleePath.Piece.of(level(), entityData.get(DATA_PIECE));
            if (server == null) {
                return;
            }
            double serverT = entityData.get(DATA_T);
            if (piece == null || !isRunning() || server.at(serverT).pos().distanceToSqr(position()) > 16) {
                piece = server;
                t = serverT;
            }
        }
    }

    // --- moving ---------------------------------------------------------------------------------------------------

    /** Client only: how far the sled leans into a bend right now (eased, in degrees). */
    public float lean;

    /** Client only: the four guhs pulling the sled (drawn by the renderer, never added to the world). */
    public final java.util.List<GuhEntity> pullers = new java.util.ArrayList<>();

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (GuhEntity guh : pullers) { // keep them walking (their animations run on their own tick count)
                guh.tickCount++;
                guh.walkAnimation.update(isRunning() && piece != null ? Math.min(1f, SPEEDS[getSpeed() - 1] * 2.5f) : 0f, 0.4f);
            }
        }
        if (piece != null && SleePath.Piece.of(level(), piece.anchor()) == null) {
            piece = null; // the rail was broken
            entityData.set(DATA_RUNNING, false);
            sync();
        }
        if (piece == null) {
            if (!level().isClientSide && tickCount % 10 == 0) {
                snapToRail();
            }
            return;
        }
        moveAlong(isRunning() ? SPEEDS[getSpeed() - 1] * slopeFactor() : 0);
        if (!level().isClientSide && tickCount % 20 == 0) {
            sync();
        }
        if (isRunning() && tickCount % 16 == 0 && isVehicle()) {
            playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.25f, 1.6f + random.nextFloat() * 0.3f);
        }
    }

    /** Like a real coaster: faster going down, slower going up (the same on both sides, so they stay together). */
    private double slopeFactor() {
        if (piece == null) {
            return 1;
        }
        double up = piece.at(t).heading().y * (isForward() ? 1 : -1);
        return Mth.clamp(1 - 0.75 * up, 0.45, 1.9);
    }

    /** Pieces gone by since the last finish line (a lap has to be a real lap). */
    private int piecesSinceFinish;

    /** Server: the sled came onto a new piece. Over a finish line = a lap for everyone riding along. */
    private void enteredPiece() {
        piecesSinceFinish++;
        if (level().getBlockEntity(piece.anchor()) instanceof nl.juiced.guhs.block.entity.SleeRailBlockEntity rail && rail.isFinish()) {
            if (piecesSinceFinish >= 6) {
                for (Entity passenger : getPassengers()) {
                    if (passenger instanceof net.minecraft.server.level.ServerPlayer player) {
                        nl.juiced.guhs.quest.KermisRides.lap(player, this);
                    }
                }
            }
            piecesSinceFinish = 0;
        }
    }

    /** Moves `distance` blocks along the track (onto the next pieces if needed) and puts the sled there. */
    private void moveAlong(double distance) {
        boolean forward = isForward();
        while (distance > 0 && piece != null) {
            double len = piece.shape().length();
            double nt = t + (forward ? distance : -distance) / len;
            if (nt >= 0 && nt <= 1) {
                t = nt;
                break;
            }
            distance = (forward ? nt - 1 : -nt) * len;
            SleePath.Next next = SleePath.next(level(), piece, forward);
            if (next == null) { // end of the line: stop, and next time go back
                t = forward ? 1 : 0;
                if (!level().isClientSide) {
                    entityData.set(DATA_RUNNING, false);
                    entityData.set(DATA_FORWARD, !forward);
                    sync();
                    playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 1.2f);
                }
                break;
            }
            piece = next.piece();
            forward = next.forward();
            t = forward ? 0 : 1;
            if (!level().isClientSide) {
                enteredPiece();
            }
            entityData.set(DATA_FORWARD, forward); // both sides: the client follows the track by itself
            sync();
        }
        if (piece == null) {
            return;
        }
        SleePath.Point p = piece.at(t);
        Vec3 heading = isForward() ? p.heading() : p.heading().reverse();
        setPos(p.pos());
        setYRot((float) Math.toDegrees(Math.atan2(-heading.x, heading.z)));
        setXRot((float) -Math.toDegrees(Math.asin(Mth.clamp(heading.y, -1, 1))));
    }

    /**
     * The point `distance` blocks further along the track (in the direction the sled faces), without moving the sled:
     * where the guhs pulling it walk. Past the end of the line it just goes straight on.
     */
    @Nullable
    public SleePath.Point pointAhead(double distance) {
        SleePath.Piece pc = piece;
        double tt = t;
        boolean fw = isForward();
        for (int guard = 0; pc != null && guard < 8; guard++) {
            double len = pc.shape().length();
            double nt = tt + (fw ? distance : -distance) / len;
            if (nt >= 0 && nt <= 1) {
                SleePath.Point p = pc.at(nt);
                return new SleePath.Point(p.pos(), fw ? p.heading() : p.heading().reverse());
            }
            distance = (fw ? nt - 1 : -nt) * len;
            SleePath.Next next = SleePath.next(level(), pc, fw);
            if (next == null) {
                SleePath.Point end = pc.at(fw ? 1 : 0);
                Vec3 h = fw ? end.heading() : end.heading().reverse();
                return new SleePath.Point(end.pos().add(h.scale(distance)), h);
            }
            pc = next.piece();
            fw = next.forward();
            tt = fw ? 0 : 1;
        }
        return null;
    }

    /** A sled put down by a structure (Guhland) finds its rail by itself. */
    private void snapToRail() {
        BlockPos base = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-2, -2, -2), base.offset(2, 1, 2))) {
            SleePath.Piece found = SleePath.Piece.of(level(), pos);
            if (found != null) {
                putOn(found, position(), Vec3.directionFromRotation(0, getYRot()));
                return;
            }
        }
    }

    /** The client moves the sled itself; position packets from the server would only make it shake. */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (piece == null) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
        }
    }

    // --- riding ---------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (isLocked()) {
                player.displayClientMessage(Component.translatable("entity.guhs.guh_slee.locked").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!level().isClientSide && !isVehicle()) {
                pickUp(player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide && canAddPassenger(player)) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void pickUp(Player player) {
        if (!player.getAbilities().instabuild) {
            ItemStack stack = new ItemStack(ModItems.GUH_SLEE.get());
            if (!player.addItem(stack)) {
                spawnAtLocation(stack);
            }
        }
        playSound(SoundEvents.WOOD_BREAK, 1f, 1.2f);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source)) {
            return false;
        }
        if (source.getEntity() instanceof Player player && !isLocked() && !isVehicle()) {
            pickUp(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!level().isClientSide && passenger instanceof Player player) {
            player.displayClientMessage(Component.translatable("entity.guhs.guh_slee.hint").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /** Getting out halfway the coaster: you float down instead of falling. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide && passenger instanceof net.minecraft.world.entity.LivingEntity living
                && level().getBlockState(blockPosition().below(2)).isAir()) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 160, 0, false, false));
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int index = Math.max(0, getPassengers().indexOf(passenger));
        // the second passenger sits behind the first
        Vec3 offset = new Vec3(0, dimensions.height() * 0.45, index == 0 ? 0.15 : -0.55);
        return offset.yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof Player) {
            // turn along with the sled in the bends (like in a boat)
            float turn = Mth.wrapDegrees(getYRot() - yRotO);
            if (Math.abs(turn) < 90) {
                passenger.setYRot(passenger.getYRot() + turn);
                passenger.setYHeadRot(passenger.getYHeadRot() + turn);
            }
        } else {
            passenger.setYRot(getYRot());
            passenger.setYHeadRot(getYRot());
            passenger.setYBodyRot(getYRot());
        }
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.GUH_SLEE.get());
    }

    // --- saving ---------------------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (piece != null) {
            tag.put("Piece", NbtUtils.writeBlockPos(piece.anchor()));
            tag.putDouble("T", t);
        }
        tag.putBoolean("Forward", isForward());
        tag.putBoolean("Running", isRunning());
        tag.putInt("Speed", getSpeed());
        tag.putBoolean("Locked", isLocked());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_FORWARD, !tag.contains("Forward") || tag.getBoolean("Forward"));
        entityData.set(DATA_RUNNING, tag.getBoolean("Running"));
        setSpeed(tag.contains("Speed") ? tag.getInt("Speed") : 1);
        setLocked(tag.getBoolean("Locked"));
        pendingPiece = NbtUtils.readBlockPos(tag, "Piece").orElse(null);
        t = tag.getDouble("T");
    }

    /** The piece from the save, looked up once the chunk around it is there. */
    @Nullable
    private BlockPos pendingPiece;

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (pendingPiece != null && !level().isClientSide) {
            piece = SleePath.Piece.of(level(), pendingPiece);
            pendingPiece = null;
            sync();
        }
    }

    // --- GeckoLib -------------------------------------------------------------------------------------------------

    private static final software.bernie.geckolib.animation.RawAnimation BLINK =
            software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.guh_slee.knopjes");

    /** The buttons on the dashboard blink while the sled stands still: click them (right-click while riding)! */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, "knopjes", 0,
                state -> isRunning() ? software.bernie.geckolib.animation.PlayState.STOP : state.setAndContinue(BLINK)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
