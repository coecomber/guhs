package nl.juiced.guhs.feature.vadsrijk.stadsleven;

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
 * U-LEVEN-STAD: one small town prop moved by the server (vr_sl_prop): a pieppiepmuisje, a light-blue guh-blob, a duckling, a
 * little bird, the ice-cream cart, or a moving part of the playground (the seesaw plank, a swing, the merry-go-round). Any
 * Guhs GeckoLib model + texture + animation (synced), a render scale, and a synced tilt ({@link #kantel}, degrees, around the
 * model's x axis at its origin: the seesaw and the swings). No physics, no hitbox, never hurt, never saved (noSave type), only
 * sent to players who did not switch Stadsleven off.
 */
public class LevenProp extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(LevenProp.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEXTUUR = SynchedEntityData.defineId(LevenProp.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIMBESTAND = SynchedEntityData.defineId(LevenProp.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIM = SynchedEntityData.defineId(LevenProp.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SCHAAL = SynchedEntityData.defineId(LevenProp.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> KANTEL = SynchedEntityData.defineId(LevenProp.class, EntityDataSerializers.FLOAT);
    /** Render LOD class: 0 = a small critter (short range), 1 = playground/cart (long range). */
    private static final EntityDataAccessor<Byte> GROOT = SynchedEntityData.defineId(LevenProp.class, EntityDataSerializers.BYTE);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, RawAnimation> anims = new HashMap<>();
    /** Client: the tilt of the previous tick (for smooth interpolation). */
    public float kantelO;

    public LevenProp(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Nullable
    static LevenProp maak(ServerLevel sl, Vec3 waar, float yaw, String model, String textuur, String animBestand, String anim, float schaal,
            boolean groot) {
        LevenProp f = StadslevenRegistry.PROP.get().create(sl, EntitySpawnReason.EVENT);
        if (f == null) {
            return null;
        }
        f.entityData.set(MODEL, model);
        f.entityData.set(TEXTUUR, textuur);
        f.entityData.set(ANIMBESTAND, animBestand);
        f.entityData.set(ANIM, anim);
        f.entityData.set(SCHAAL, schaal);
        f.entityData.set(GROOT, (byte) (groot ? 1 : 0));
        f.snapTo(waar.x, waar.y, waar.z, yaw, 0);
        f.addTag(StadsLeven.TAG);
        StadsLeven.EIGEN.add(f.getUUID());
        if (!sl.addFreshEntity(f)) {
            StadsLeven.EIGEN.remove(f.getUUID());
            return null;
        }
        return f;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(MODEL, "pieppiepmuisje");
        b.define(TEXTUUR, "textures/entity/pieppiepmuisje.png");
        b.define(ANIMBESTAND, "pieppiepmuisje");
        b.define(ANIM, "idle");
        b.define(SCHAAL, 1f);
        b.define(KANTEL, 0f);
        b.define(GROOT, (byte) 0);
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
        if (!a.equals(entityData.get(ANIM))) {
            entityData.set(ANIM, a);
        }
    }

    public float schaal() {
        return entityData.get(SCHAAL);
    }

    public boolean groot() {
        return entityData.get(GROOT) != 0;
    }

    public float kantel() {
        return entityData.get(KANTEL);
    }

    public void kantel(float graden) {
        entityData.set(KANTEL, graden);
    }

    @Override
    public void tick() {
        kantelO = kantel();
        super.tick();
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
        return StadsLeven.ziet(player);
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
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    public AABB cullDoos() {
        return getBoundingBox().inflate(groot() ? 3 : 1);
    }

    private RawAnimation raw(String a) {
        return anims.computeIfAbsent(a, k -> k.startsWith("1:") ? RawAnimation.begin().thenPlayAndHold(k.substring(2)) : RawAnimation.begin().thenLoop(k));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<LevenProp>("anim", 4, s -> s.animatable().anim().isEmpty()
                ? com.geckolib.animation.object.PlayState.STOP : s.setAndContinue(raw(s.animatable().anim()))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
