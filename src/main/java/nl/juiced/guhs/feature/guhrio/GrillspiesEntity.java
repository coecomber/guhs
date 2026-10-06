package nl.juiced.guhs.feature.guhrio;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A turning grill spit: a glowing skewer of {@link #lengte} blocks that swings round its hub (the block
 * {@link GuhrioStukken.GrillspiesPlek}) in the plane of the lane, like a clock hand. Where it is, is worked out from the
 * world's clock on both sides ({@link #hoek}), so it turns smoothly and everybody sees the same. Touching the skewer
 * sends you back to your flag (or costs your power-up); it only shoves. Never saved, never hurt.
 */
public class GrillspiesEntity extends LevelWezen {
    /** Degrees per tick: normal and fast. */
    public static final float TRAAG = 4.5f, SNEL = 7.5f;
    /** How thick the skewer is for touching. */
    public static final double DIK = 0.3;
    private static final EntityDataAccessor<Integer> DATA_LENGTE = SynchedEntityData.defineId(GrillspiesEntity.class, EntityDataSerializers.INT);
    /** Bits: 0-1 the lane's direction (Direction 2D), 2 against the clock, 3 fast, 4-5 the phase (quarter turns). */
    private static final EntityDataAccessor<Integer> DATA_VORM = SynchedEntityData.defineId(GrillspiesEntity.class, EntityDataSerializers.INT);

    public GrillspiesEntity(EntityType<? extends GrillspiesEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LENGTE, 3);
        builder.define(DATA_VORM, 0);
    }

    /** What kind of spit it is (called by its spot before it joins the world). */
    public void zetVorm(int lengte, Direction langs, boolean tegenKlok, boolean snel, int fase) {
        this.entityData.set(DATA_LENGTE, Math.max(1, Math.min(8, lengte)));
        this.entityData.set(DATA_VORM, langs.get2DDataValue() & 3 | (tegenKlok ? 4 : 0) | (snel ? 8 : 0) | (fase & 3) << 4);
        this.refreshDimensions();
    }

    public int lengte() {
        return this.entityData.get(DATA_LENGTE);
    }

    /** The way "further along the lane" points at the hub. */
    public Direction langs() {
        return Direction.from2DDataValue(this.entityData.get(DATA_VORM) & 3);
    }

    /**
     * The skewer's angle (degrees) at this moment: 0 points further along the lane, 90 straight up. With the clock (seen
     * from the camera of a "rechts" lane) the angle goes down.
     */
    public float hoek(float partial) {
        int vorm = this.entityData.get(DATA_VORM);
        float per = ((vorm & 8) != 0 ? SNEL : TRAAG) * ((vorm & 4) != 0 ? 1f : -1f);
        double t = (this.level().getGameTime() % 7200) + partial;
        return (float) ((t * per + (vorm >> 4 & 3) * 90.0) % 360.0);
    }

    /** The middle of the hub. */
    public Vec3 naaf() {
        return new Vec3(getX(), getY() + lengte() + 0.5, getZ());
    }

    /** The spot on the skewer {@code r} blocks from the hub, now. */
    public Vec3 punt(double r, float partial) {
        double a = Math.toRadians(hoek(partial));
        Direction d = langs();
        Vec3 n = naaf();
        double langs = Math.cos(a) * r;
        return new Vec3(n.x + d.getStepX() * langs, n.y + Math.sin(a) * r, n.z + d.getStepZ() * langs);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float maat = lengte() * 2 + 1;
        return EntityDimensions.fixed(maat, maat);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_LENGTE.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    @Override
    @Nullable
    public InterpolationHandler getInterpolation() {
        return null;                                          // (it never moves: only its angle does)
    }

    @Override
    public void zetBaan(@Nullable GuhrioSpel.Actief actief, Baan baan, @Nullable BlockPos thuis) {
        super.zetBaan(actief, baan, thuis);
    }

    @Override
    protected void serverTick(ServerLevel level) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }

    // --- GuhrioWezen -----------------------------------------------------------------------------------------------------

    @Override
    public boolean raaktVak(AABB vak) {
        return raaktVak(vak, DIK);
    }

    private boolean raaktVak(AABB vak, double dik) {
        AABB ruim = vak.inflate(dik);
        int n = lengte();
        for (double r = 0.75; r <= n + 0.01; r += 0.25) {
            if (ruim.contains(punt(r, 1f))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean stampbaar() {
        return false;
    }

    @Override
    public boolean gevaarlijk() {
        return true;
    }

    @Override
    public void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        raakt(player, sessie);
    }

    @Override
    public void raakt(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        // (the player's game saw it; the server's clock may be a tick or two off, so it looks with a wider skewer)
        if (raaktVak(player.getBoundingBox(), 1.4)) {
            GuhrioSpel.geraakt(player, sessie);
        }
    }

    @Override
    public boolean schild(@Nullable ServerPlayer schopper) {
        return true;                                          // (a shell slides under / through it)
    }
}
