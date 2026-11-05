package nl.juiced.guhs.feature.vadsrijk.vriendjes;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * N-EXTRA: one critter around the pink dragon ({@link DraakVriendjes}): any existing Guhs critter model with a texture and
 * animation (synced), moved by the server. No physics, no hitbox, never hurt, never saved. Only sent to the players who can see
 * its dragon (the resting/visiting {@link RozeDraak} is per player: whoever finished zone 3).
 */
public class VriendjeEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(VriendjeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEXTUUR = SynchedEntityData.defineId(VriendjeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIMBESTAND = SynchedEntityData.defineId(VriendjeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIM = SynchedEntityData.defineId(VriendjeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SCHAAL = SynchedEntityData.defineId(VriendjeEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, RawAnimation> anims = new HashMap<>();
    /** Server: its dragon (decides who gets to see it). */
    @Nullable
    Entity draak;

    public VriendjeEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Nullable
    static VriendjeEntity maak(ServerLevel sl, @Nullable Entity draak, Vec3 waar, float yaw, String model, String textuur, String animBestand,
            String anim, float schaal) {
        VriendjeEntity f = DraakVriendjes.VRIENDJE.get().create(sl, EntitySpawnReason.EVENT);
        if (f == null) {
            return null;
        }
        f.draak = draak;
        f.entityData.set(MODEL, model);
        f.entityData.set(TEXTUUR, textuur);
        f.entityData.set(ANIMBESTAND, animBestand);
        f.entityData.set(ANIM, anim);
        f.entityData.set(SCHAAL, schaal);
        f.snapTo(waar.x, waar.y, waar.z, yaw, 0);
        sl.addFreshEntity(f);
        return f;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(MODEL, "pluisvinkje");
        b.define(TEXTUUR, "textures/entity/pluisvinkje.png");
        b.define(ANIMBESTAND, "pluisvinkje");
        b.define(ANIM, "animation.pluisvinkje.idle");
        b.define(SCHAAL, 1f);
    }

    public String model() {
        return entityData.get(MODEL);
    }

    public String textuur() {
        return entityData.get(TEXTUUR);
    }

    public String animBestand() {
        return entityData.get(ANIMBESTAND);
    }

    public String anim() {
        return entityData.get(ANIM);
    }

    public void anim(String a) {
        entityData.set(ANIM, a);
    }

    public float schaal() {
        return entityData.get(SCHAAL);
    }

    /** Server: put it here and face where it goes. */
    public void zet(Vec3 v) {
        Vec3 d = v.subtract(position());
        if (d.horizontalDistanceSqr() > 1e-4) {
            setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        }
        setPos(v.x, v.y, v.z);
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return !(draak instanceof RozeDraak r) || r.broadcastToPlayer(player);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
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
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 96 * 96;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    public AABB cullDoos() {
        return getBoundingBox().inflate(1);
    }

    private RawAnimation raw(String a) {
        return anims.computeIfAbsent(a, k -> k.startsWith("1:") ? RawAnimation.begin().thenPlayAndHold(k.substring(2)) : RawAnimation.begin().thenLoop(k));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<VriendjeEntity>("anim", 4, s -> s.setAndContinue(raw(s.animatable().anim()))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
