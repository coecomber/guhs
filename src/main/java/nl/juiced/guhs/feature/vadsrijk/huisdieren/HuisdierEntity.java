package nl.juiced.guhs.feature.vadsrijk.huisdieren;

import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * G-PETS: a pet walking along with its owner (see {@link Huisdieren}). Tiny, never targetable, never hurt, never in the way (not
 * pickable: arrows, spells and clicks go through it; no pushing), never saved in the world (the owner's data respawns it). It hops
 * after its owner and teleports along (with a little poof) when it falls behind or gets stuck. The look (model, texture, animation,
 * scale) comes from its {@link Huisdier} (synced id).
 */
public class HuisdierEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> DATA_SOORT = SynchedEntityData.defineId(HuisdierEntity.class, EntityDataSerializers.STRING);
    /** T-RIJDIER: extra render size (Vahoegje grows with the story: rijdier.Vahoegje). */
    private static final EntityDataAccessor<Float> DATA_GROOTTE = SynchedEntityData.defineId(HuisdierEntity.class, EntityDataSerializers.FLOAT);

    /** Teleport along beyond this distance (blocks). */
    public static final double TELEPORT = 12;
    /** Starts walking after its owner beyond this distance. */
    public static final double VOLG = 2.6;

    /** Client only: a preview in the pet screen (always idle). */
    public boolean voorbeeld;
    /** Client only: a preview of a pet you don't have yet: drawn as a dark silhouette. */
    public boolean silhouet;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID eigenaar;
    private int vast;

    public HuisdierEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributen() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.STEP_HEIGHT, 1.05)
                .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.SAFE_FALL_DISTANCE, 256).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** U-HUIZEN: other code that drives a pet itself (true = handled, skip the follow-the-owner logic and the "no owner" cleanup). */
    public static final java.util.List<java.util.function.Predicate<HuisdierEntity>> ZELFSTANDIG = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DATA_SOORT, "");
        b.define(DATA_GROOTTE, 1f);
    }

    public String soortId() {
        return entityData.get(DATA_SOORT);
    }

    @Nullable
    public Huisdier soort() {
        return Huisdieren.van(soortId());
    }

    /** T-RIJDIER: the extra render size (1 = normal). */
    public float grootte() {
        return entityData.get(DATA_GROOTTE);
    }

    public void zetGrootte(float f) {
        if (Math.abs(grootte() - f) > 0.001f) {
            entityData.set(DATA_GROOTTE, f);
        }
    }

    public void zetSoort(String id) {
        entityData.set(DATA_SOORT, id);
    }

    /** (fix round 3) Its own Dutch name ("Mini Koning Klodder") wherever a name is shown, never the raw type key. */
    @Override
    protected Component getTypeName() {
        Huisdier h = soort();
        return h != null ? Component.translatable(h.naamKey()) : super.getTypeName();
    }

    /** No always-on nameplate: the client shows "&lt;pet&gt; van &lt;player&gt;" only while you look at it (vanilla named-mob style, see HuisdierClient). */
    @Override
    public boolean shouldShowName() {
        return false;
    }

    void zet(String id, UUID eigenaar) {
        zetSoort(id);
        this.eigenaar = eigenaar;
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    // ---- following -------------------------------------------------------------------------------------------------------------

    /** A free spot right behind the player (or the player's own spot). */
    static Vec3 plekBij(Player p) {
        // beside the player, a little behind (so it shows next to you, not hidden behind your back), else the other side, else behind
        Vec3 f = Vec3.directionFromRotation(0, p.getYRot());
        double zx = -f.z, zz = f.x;
        for (double[] d : new double[][] {{-f.x * 0.5 + zx, -f.z * 0.5 + zz}, {-f.x * 0.5 - zx, -f.z * 0.5 - zz}, {-f.x * 1.3, -f.z * 1.3}}) {
            Vec3 v = new Vec3(p.getX() + d[0], p.getY(), p.getZ() + d[1]);
            AABB box = new AABB(v.x - 0.2, v.y, v.z - 0.2, v.x + 0.2, v.y + 0.45, v.z + 0.2);
            BlockPos onder = BlockPos.containing(v.x, v.y - 0.5, v.z);
            if (p.level().noCollision(box) && !p.level().getBlockState(onder).getCollisionShape(p.level(), onder).isEmpty()) {
                return v;
            }
        }
        return p.position();
    }

    @Nullable
    private ServerPlayer baas() {
        if (eigenaar == null || !(level() instanceof ServerLevel sl)) {
            return null;
        }
        return sl.getPlayerByUUID(eigenaar) instanceof ServerPlayer p ? p : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        for (java.util.function.Predicate<HuisdierEntity> z : ZELFSTANDIG) {   // U-HUIZEN: a pet that roams a house garden on its own
            if (z.test(this)) {
                return;
            }
        }
        ServerPlayer p = baas();
        if (p == null || Huisdieren.uit(p) != this) {
            discard();   // (no owner here, or an old copy: the manager makes a new one)
            return;
        }
        double d = distanceTo(p);
        boolean verder = d > TELEPORT || getY() < level().getMinY() + 2 || (d > 5 && vast > 60) || (isInWater() && d > 6);
        if (verder && !p.isSpectator()) {
            teleporteer(p);
            return;
        }
        getLookControl().setLookAt(p, 30, 30);
        if (tickCount % 8 == 0) {
            if (d > VOLG) {
                getNavigation().moveTo(p, d > 6 ? 1.35 : 1.0);
            } else {
                getNavigation().stop();
            }
        }
        // stuck: far-ish and hardly moving
        double dx = getX() - xo, dz = getZ() - zo;
        vast = d > 4 && dx * dx + dz * dz < 0.0004 ? vast + 1 : 0;
        // a happy little hop now and then while waiting
        if (d <= VOLG && onGround() && random.nextInt(220) == 0) {
            jumpFromGround();
        }
    }

    /** Jumps to its owner with a little poof (also used by tests). */
    void teleporteer(ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        Huisdieren.poefBij(sl, position().add(0, 0.2, 0), 0.4);
        Vec3 v = plekBij(p);
        getNavigation().stop();
        snapTo(v.x, v.y, v.z, p.getYRot(), 0);
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0;
        vast = 0;
        Huisdieren.poefBij(sl, v.add(0, 0.2, 0), 0.4);
    }

    // ---- never in the way, never hurt, never saved ------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
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
    protected void pushEntities() {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
    }

    // ---- GeckoLib ---------------------------------------------------------------------------------------------------------------------

    private static final RawAnimation STIL = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation HUP = RawAnimation.begin().thenLoop("hop");
    private static final RawAnimation LUCHT = RawAnimation.begin().thenLoop("sprong");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<HuisdierEntity>("beweeg", 3, state -> {
            if (voorbeeld) {
                return state.setAndContinue(STIL);
            }
            boolean lucht = !onGround() && !isInWater() && Math.abs(getY() - yo) > 0.02;
            double dx = getX() - xo, dz = getZ() - zo;
            boolean loopt = dx * dx + dz * dz > 0.0002 || state.isMoving();
            return state.setAndContinue(lucht ? LUCHT : loopt ? HUP : STIL);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
