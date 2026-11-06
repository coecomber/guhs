package nl.juiced.guhs.feature.vadsrijk.zone4.dieren;

import javax.annotation.Nullable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A guh kite of the Lampionkust (T-Z4DIEREN): a diamond with guh ears and a face, a ribbon tail, on a string down to a
 * little post. The entity itself just hangs at its spot (no AI, no physics, no ticking logic, never saved:
 * {@link Z4DierenRegistry} puts them up near players from {@link Z4DierenPlekken#VLIEGERS}); all the bobbing and swaying in the
 * wind is done by the client renderer, with a level of detail (far away: no tail, a 2-piece string).
 */
public class GuhVlieger extends Entity {
    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(GuhVlieger.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_KLEUR2 = SynchedEntityData.defineId(GuhVlieger.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_GROOTTE = SynchedEntityData.defineId(GuhVlieger.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_AX = SynchedEntityData.defineId(GuhVlieger.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_AY = SynchedEntityData.defineId(GuhVlieger.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_AZ = SynchedEntityData.defineId(GuhVlieger.class, EntityDataSerializers.FLOAT);

    /** Which map kite this is (index in VLIEGERS; -1 = a loose one from a dev command or test). */
    public int index = -1;

    public GuhVlieger(EntityType<? extends GuhVlieger> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KLEUR, 0xFF78AA);
        builder.define(DATA_KLEUR2, 0xFFFFFF);
        builder.define(DATA_GROOTTE, 3f);
        builder.define(DATA_AX, 0f);
        builder.define(DATA_AY, 0f);
        builder.define(DATA_AZ, 0f);
    }

    /** Sets everything from a map kite (also its position). */
    public void zet(Z4DierenPlekken.Vlieger v) {
        setPos(v.x(), v.y(), v.z());
        entityData.set(DATA_KLEUR, v.kleur());
        entityData.set(DATA_KLEUR2, v.kleur2());
        entityData.set(DATA_GROOTTE, v.grootte());
        // the anchor as an offset from the kite (floats stay precise far from 0, e.g. in gametests)
        entityData.set(DATA_AX, (float) (v.ax() - v.x()));
        entityData.set(DATA_AY, (float) (v.ay() - v.y()));
        entityData.set(DATA_AZ, (float) (v.az() - v.z()));
    }

    public int kleur() {
        return entityData.get(DATA_KLEUR);
    }

    public int kleur2() {
        return entityData.get(DATA_KLEUR2);
    }

    public float grootte() {
        return entityData.get(DATA_GROOTTE);
    }

    /** Where the string is tied (world position). */
    public Vec3 anker() {
        return position().add(entityData.get(DATA_AX), entityData.get(DATA_AY), entityData.get(DATA_AZ));
    }

    @Override
    public void tick() {
        // nothing to do: it hangs at its spot, the client makes it dance
    }

    @Override
    public boolean isPickable() {
        return false;
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /** Kite + string + post (used by its renderer). */
    public AABB getBoundingBoxForCulling() {
        Vec3 a = anker();
        double g = grootte() + 2;
        return new AABB(Math.min(a.x, getX()) - g, Math.min(a.y, getY()) - g - 1, Math.min(a.z, getZ()) - g,
                Math.max(a.x, getX()) + g, Math.max(a.y, getY()) + g, Math.max(a.z, getZ()) + g);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }
}
