package nl.juiced.guhs.feature.guhrio;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A moving platform: a little deck (1 to 4 blocks along the lane, half a block thick, its top level with the top of its
 * spot's row) that glides {@code afstand} blocks along the lane or straight up, and back, for ever. Where it is, is
 * worked out from the world's clock on both sides ({@link #plek}), so it never stutters and the player's game can carry
 * the player along exactly (client.BaanBesturing). You stand on it like on a block. Never saved, never hurt.
 */
public class PlatformEntity extends LevelWezen {
    /** Blocks per tick (on average): normal and fast. */
    public static final double TRAAG = 0.05, SNEL = 0.09;
    public static final float DIK = 0.5f;
    private static final EntityDataAccessor<BlockPos> DATA_THUIS = SynchedEntityData.defineId(PlatformEntity.class, EntityDataSerializers.BLOCK_POS);
    /** Bits: 0-1 the lane's direction (Direction 2D), 2 up instead of along, 3 fast, 4-7 the distance (1..12), 8-10 the width (1..4). */
    private static final EntityDataAccessor<Integer> DATA_VORM = SynchedEntityData.defineId(PlatformEntity.class, EntityDataSerializers.INT);

    public PlatformEntity(EntityType<? extends PlatformEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_THUIS, BlockPos.ZERO);
        builder.define(DATA_VORM, 3 << 8 | 4 << 4);
    }

    /** What kind of platform it is (called by its spot before it joins the world). */
    public void zetVorm(BlockPos thuis, Direction langs, boolean omhoog, boolean snel, int afstand, int breed) {
        this.entityData.set(DATA_THUIS, thuis.immutable());
        this.entityData.set(DATA_VORM, langs.get2DDataValue() & 3 | (omhoog ? 4 : 0) | (snel ? 8 : 0) | Math.max(1, Math.min(12, afstand)) << 4
                | Math.max(1, Math.min(4, breed)) << 8);
        this.refreshDimensions();
        Vec3 p = plek(this.level().getGameTime());
        this.setPos(p.x, p.y, p.z);
    }

    public int breed() {
        return this.entityData.get(DATA_VORM) >> 8 & 7;
    }

    public Direction langs() {
        return Direction.from2DDataValue(this.entityData.get(DATA_VORM) & 3);
    }

    public boolean omhoog() {
        return (this.entityData.get(DATA_VORM) & 4) != 0;
    }

    public int afstand() {
        return this.entityData.get(DATA_VORM) >> 4 & 15;
    }

    /** Where it is at game time {@code tijd}: its feet (the middle of the deck's underside). */
    public Vec3 plek(long tijd) {
        int vorm = this.entityData.get(DATA_VORM);
        BlockPos thuis = this.entityData.get(DATA_THUIS);
        Direction d = langs();
        int afstand = afstand(), breed = breed();
        double ronde = 2 * afstand / ((vorm & 8) != 0 ? SNEL : TRAAG);
        double f = (1 - Math.cos(2 * Math.PI * (tijd % (long) Math.max(1, Math.round(ronde))) / Math.max(1, Math.round(ronde)))) / 2 * afstand;
        double langs = (breed - 1) / 2.0 + (omhoog() ? 0 : f);
        return new Vec3(thuis.getX() + 0.5 + d.getStepX() * langs, thuis.getY() + 1 - DIK + (omhoog() ? f : 0), thuis.getZ() + 0.5 + d.getStepZ() * langs);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(Math.max(1, breed()), DIK);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_VORM.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    @Override
    @Nullable
    public InterpolationHandler getInterpolation() {
        return null;                                          // (the clock says where it is, not the server's messages)
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved() || this.entityData.get(DATA_THUIS).equals(BlockPos.ZERO)) {
            return;
        }
        Vec3 p = plek(this.level().getGameTime());
        this.setPos(p.x, p.y, p.z);
    }

    @Override
    protected void serverTick(ServerLevel level) {
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    // --- GuhrioWezen -----------------------------------------------------------------------------------------------------

    @Override
    public boolean draagt() {
        return true;
    }

    @Override
    public boolean stampbaar() {
        return false;
    }

    @Override
    public boolean gevaarlijk() {
        return false;
    }

    @Override
    public void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie) {
    }
}
