package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

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
 * A prop for the zone-3 boss moments: any GeckoLib model with any texture and animation, moved by the server (no physics, no
 * hitbox, never saved). The Keizer's long grey arm (flash F1), the caged and freed pieppiepmuisjes, the egg and the hatchling,
 * Graaf Piepvel kneeling in his shirt and Hofschrijver Krabbel (flash F2), the Schimmelkolos, the turned-good Nestwachter.
 * {@link #anim}: a loop name, or "1:name" to play once and hold.
 */
public class Z3Figurant extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(Z3Figurant.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEXTUUR = SynchedEntityData.defineId(Z3Figurant.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIMBESTAND = SynchedEntityData.defineId(Z3Figurant.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIM = SynchedEntityData.defineId(Z3Figurant.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SCHAAL = SynchedEntityData.defineId(Z3Figurant.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> VERBORGEN = SynchedEntityData.defineId(Z3Figurant.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, RawAnimation> anims = new HashMap<>();
    /** Ticks to live (-1 = until removed). */
    public int leven = -1;
    /** Where it walks to by itself (a freed mouse running off), with this speed per tick. */
    @Nullable
    public Vec3 doel;
    public double snelheid = 0.25;

    public Z3Figurant(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    /** Spawns a prop: model id (geckolib/models/entity/&lt;model&gt;), texture path, animation file id, start animation, scale. */
    @Nullable
    public static Z3Figurant maak(ServerLevel sl, Vec3 waar, float yaw, String model, String textuur, String animBestand, String anim, float schaal) {
        Z3Figurant f = Z3BazenRegistry.FIGURANT.get().create(sl, EntitySpawnReason.EVENT);
        if (f == null) {
            return null;
        }
        f.entityData.set(MODEL, model);
        f.entityData.set(TEXTUUR, textuur);
        f.entityData.set(ANIMBESTAND, animBestand);
        f.entityData.set(ANIM, anim);
        f.entityData.set(SCHAAL, schaal);
        f.snapTo(waar.x, waar.y, waar.z, yaw, 0);
        sl.addFreshEntity(f);
        return f;
    }

    /** A pieppiepmuisje (bald = still cold and sad, else its fur is back). */
    @Nullable
    public static Z3Figurant muisje(ServerLevel sl, Vec3 waar, boolean kaal) {
        return maak(sl, waar, sl.getRandom().nextFloat() * 360, "pieppiepmuisje", kaal ? "textures/vadsrijk/z3bazen/muisje_kaal.png" : "textures/entity/pieppiepmuisje.png",
                "pieppiepmuisje", kaal ? "idle" : "blij", 1.0f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(MODEL, "pieppiepmuisje");
        b.define(TEXTUUR, "textures/entity/pieppiepmuisje.png");
        b.define(ANIMBESTAND, "pieppiepmuisje");
        b.define(ANIM, "idle");
        b.define(SCHAAL, 1f);
        b.define(VERBORGEN, "");
    }

    public String model() {
        return entityData.get(MODEL);
    }

    public String textuur() {
        return entityData.get(TEXTUUR);
    }

    public void zetTextuur(String t) {
        entityData.set(TEXTUUR, t);
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

    public void zetSchaal(float s) {
        entityData.set(SCHAAL, s);
    }

    /** Comma-separated bone names the renderer hides. */
    public String verborgen() {
        return entityData.get(VERBORGEN);
    }

    public void verberg(String botten) {
        entityData.set(VERBORGEN, botten);
    }

    /** Server: put it here (and face where it goes). */
    public void zet(Vec3 v) {
        Vec3 d = v.subtract(position());
        if (d.horizontalDistanceSqr() > 1e-4) {
            setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        }
        setPos(v.x, v.y, v.z);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (doel != null) {
            Vec3 d = doel.subtract(position());
            double l = d.length();
            if (l < snelheid) {
                zet(doel);
                doel = null;
            } else {
                zet(position().add(d.scale(snelheid / l)));
            }
        }
        if (leven >= 0 && --leven <= 0) {
            discard();
        }
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
        // U-GROOTTE (FPS): only the giant props (Wolkwal fin tips, statues: scale >= 2) are seen from afar; the townsfolk
        // (hundreds in Hoogvads, each a full animated guh model) only within 96 blocks (the server removes them at 80 anyway).
        double grens = schaal() >= 2 ? 256 : 96;
        return d < grens * grens;
    }

    public AABB getBoundingBoxForCulling() {
        double s = Math.max(1, schaal()) * 6;
        return getBoundingBox().inflate(s, s, s);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    private RawAnimation raw(String a) {
        return anims.computeIfAbsent(a, k -> k.startsWith("1:") ? RawAnimation.begin().thenPlayAndHold(k.substring(2)) : RawAnimation.begin().thenLoop(k));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Z3Figurant>("anim", 4, s -> s.setAndContinue(raw(s.animatable().anim()))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
