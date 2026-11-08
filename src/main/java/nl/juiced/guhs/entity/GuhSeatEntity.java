package nl.juiced.guhs.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.block.GuhFurnitureBlock;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/** The invisible thing you sit on when you sit on guh furniture. Gone as soon as you get up (or the seat is broken). */
public class GuhSeatEntity extends Entity {
    public GuhSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** 26.1: hurtServer is abstract in Entity; a seat can't be hurt (1.21.1's Entity#hurt did nothing either). */
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (!isVehicle() || !(level().getBlockState(blockPosition()).getBlock() instanceof GuhFurnitureBlock))) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return Vec3.ZERO;
    }

    /**
     * W-SPEL (1.10.0): a checked spot to stand up to, never inside or under blocks: on top of the seat block when there is room,
     * else the free floor next to it (front first), else straight up from the seat. Vanilla teleports the player there.
     */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockPos stoel = blockPosition();
        Vec3 boven = net.minecraft.world.entity.vehicle.DismountHelper.findSafeDismountLocation(passenger.getType(), level(), stoel, true);
        if (boven != null) {
            return boven;
        }
        net.minecraft.core.Direction voor = net.minecraft.core.Direction.fromYRot(getYRot());
        for (net.minecraft.core.Direction d : new net.minecraft.core.Direction[] {voor, voor.getClockWise(), voor.getCounterClockWise(), voor.getOpposite()}) {
            for (int dy : new int[] {0, 1, -1}) {
                Vec3 v = net.minecraft.world.entity.vehicle.DismountHelper.findSafeDismountLocation(passenger.getType(), level(), stoel.relative(d).above(dy), true);
                if (v != null) {
                    return v;
                }
            }
        }
        double top = stoel.getY() + Math.max(0, level().getBlockState(stoel).getCollisionShape(level(), stoel).max(net.minecraft.core.Direction.Axis.Y));
        return new Vec3(getX(), Math.max(top, getY()), getZ());
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        passenger.setYBodyRot(getYRot());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }
}
