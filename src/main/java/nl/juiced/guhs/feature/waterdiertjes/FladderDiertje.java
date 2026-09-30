package nl.juiced.guhs.feature.waterdiertjes;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * The little flying critters of the waterdiertjes slice (knabbelvlindertje, glimguhtje, lieveheersbeestje): an ambient
 * creature that flutters like the kaasmot (steered velocity, no pathfinding, no fall damage, not pushable) and can
 * <b>land</b> on a block ({@link #kiesLandplek()}, on top of its outline: {@link #landPunt}): it sits there (synced {@link #zit()}, the "zit" animation) for a while,
 * then flutters on. It flees a little from players that run at it, and never hurts anyone.
 */
public abstract class FladderDiertje extends AmbientCreature implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_ZIT = SynchedEntityData.defineId(FladderDiertje.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    protected BlockPos doel;
    /** Where it sits (the block it sits ON), and until when. */
    @Nullable
    protected BlockPos landplek;
    protected int zitTicks, zoekTicks, rustTicks;

    protected FladderDiertje(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ZIT, false);
    }

    public boolean zit() {
        return entityData.get(DATA_ZIT);
    }

    protected void setZit(boolean zit) {
        if (zit != zit()) {
            entityData.set(DATA_ZIT, zit);
        }
    }

    // --- what the kind decides -----------------------------------------------------------------------------------------

    /** A block to land on near here (the critter sits on top of its outline), or null. Called every few seconds. */
    @Nullable
    protected abstract BlockPos kiesLandplek();

    /** Still a good spot to sit on? */
    protected abstract boolean goedeLandplek(BlockPos pos);

    /** How fast it flutters (blocks/tick-ish) and how long it sits (ticks). */
    protected double snelheid() {
        return 0.3;
    }

    protected int zitDuur() {
        return 200 + random.nextInt(400);
    }

    /** Every tick while it sits (the lieveheersbeestje helps its tuintje). */
    protected void terwijlZit() {
    }

    /** Whether it may land at all right now (the glimguhtje doesn't, the vlindertje sleeps on its flower at night). */
    protected boolean magLanden() {
        return true;
    }

    // --- flying --------------------------------------------------------------------------------------------------------

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (zit()) {
            setDeltaMovement(Vec3.ZERO);
        } else {
            setDeltaMovement(getDeltaMovement().multiply(1.0, 0.6, 1.0));
        }
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);
        if (zit()) {
            if (landplek == null || --zitTicks <= 0 || !goedeLandplek(landplek) || schrikt()) {
                opstijgen();
            } else {
                terwijlZit();
                return;
            }
        }
        if (rustTicks > 0) {
            rustTicks--;
        }
        if (landplek == null && rustTicks <= 0 && magLanden() && --zoekTicks <= 0) {
            zoekTicks = 40 + random.nextInt(40);
            landplek = kiesLandplek();
        }
        if (landplek != null) {
            Vec3 op = landPunt(landplek);
            if (position().distanceToSqr(op) < 0.35 * 0.35) {
                landen(op);
                return;
            }
            if (!goedeLandplek(landplek) || position().distanceToSqr(op) > 20 * 20) {
                landplek = null;
            } else {
                vliegNaar(op.add(0, 0.05, 0), snelheid() * 0.8, true);
                return;
            }
        }
        if (doel != null && (!level().isEmptyBlock(doel) || doel.getY() <= level().getMinY())) {
            doel = null;
        }
        if (doel == null || random.nextInt(30) == 0 || doel.closerToCenterThan(position(), 1.5)) {
            doel = volgendDoel();
        }
        vliegNaar(Vec3.atBottomCenterOf(doel).add(0, 0.1, 0), snelheid(), false);
    }

    /** Where it sits on the landing block: on top of its outline (a flower's petals, a flower pot's rim, a leaf...). */
    public Vec3 landPunt(BlockPos p) {
        net.minecraft.world.phys.shapes.VoxelShape shape = level().getBlockState(p).getShape(level(), p);
        double top = shape.isEmpty() ? 0.05 : shape.max(net.minecraft.core.Direction.Axis.Y);
        return new Vec3(p.getX() + 0.5, p.getY() + top, p.getZ() + 0.5);
    }

    /** Where to flutter next when it isn't landing: a little random hop around (kinds may prefer their own spots). */
    protected BlockPos volgendDoel() {
        BlockPos grond = level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, blockPosition());
        int boven = Mth.clamp(grond.getY() + 1 + random.nextInt(3), getBlockY() - 2, getBlockY() + 2);
        return new BlockPos(getBlockX() + random.nextInt(7) - random.nextInt(7), boven, getBlockZ() + random.nextInt(7) - random.nextInt(7));
    }

    protected void vliegNaar(Vec3 to, double speed, boolean precies) {
        double dx = to.x - getX(), dy = to.y - getY(), dz = to.z - getZ();
        Vec3 v = getDeltaMovement();
        double k = precies ? Math.min(1.0, Math.sqrt(dx * dx + dz * dz) * 2) : 1.0;       // (slows down to land exactly)
        Vec3 nv = v.add((Math.signum(dx) * speed * k - v.x) * 0.1, (Math.signum(dy) * 0.6 * (precies ? Math.min(1.0, Math.abs(dy) * 3) : 1) - v.y) * 0.1,
                (Math.signum(dz) * speed * k - v.z) * 0.1);
        setDeltaMovement(nv);
        if (nv.horizontalDistanceSqr() > 1e-4) {
            float yaw = (float) (Mth.atan2(nv.z, nv.x) * 180.0 / Math.PI) - 90.0f;
            setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.5f);
        }
        this.zza = 0.5f;
    }

    protected void landen(Vec3 op) {
        setPos(op.x, op.y, op.z);
        setDeltaMovement(Vec3.ZERO);
        setZit(true);
        zitTicks = zitDuur();
    }

    protected void opstijgen() {
        setZit(false);
        landplek = null;
        rustTicks = 100 + random.nextInt(200);
        setDeltaMovement(0, 0.15, 0);
    }

    /** A player running or sprinting close by makes it flutter up. */
    protected boolean schrikt() {
        Player p = level().getNearestPlayer(this, 2.5);
        return p != null && !p.isSpectator() && (p.isSprinting() || p.getDeltaMovement().horizontalDistanceSqr() > 0.02);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Zit", zit());
        if (landplek != null) {
            tag.putLong("Landplek", landplek.asLong());
            tag.putInt("ZitTicks", zitTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getLong("Landplek").isPresent()) {
            landplek = BlockPos.of(tag.getLongOr("Landplek", 0L));
            zitTicks = tag.getIntOr("ZitTicks", 0);
            setZit(tag.getBooleanOr("Zit", false));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
