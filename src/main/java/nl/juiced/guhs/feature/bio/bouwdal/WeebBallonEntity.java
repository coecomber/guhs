package nl.juiced.guhs.feature.bio.bouwdal;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * biomes3 bouw-dal: the balloon of Evivads and Nielsvads. It looks like the guh balloon of the Ballonfestival (the same
 * model and pictures) but flies no route: it waits on the mooring beside het weebhuisje, and when the two leave for Japan
 * it rises straight up with them in the basket and is gone ({@link #stijgOp}). The house ({@link WeebHuis}) puts a new one
 * on the mooring when they are back. Players cannot ride it.
 */
public class WeebBallonEntity extends Entity implements GeoEntity {
    /** How long it rises before it is out of sight and removed (ticks), and how fast (blocks a tick). */
    public static final int STIJG_TICKS = 300;
    public static final double SNELHEID = 0.11;
    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(WeebBallonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_STIJGT = SynchedEntityData.defineId(WeebBallonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation ZWEVEN = RawAnimation.begin().thenLoop("animation.guh_luchtballon.zweven");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int gestegen;

    public WeebBallonEntity(EntityType<? extends WeebBallonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KLEUR, 0);
        builder.define(DATA_STIJGT, false);
    }

    public int kleur() {
        return Mth.clamp(entityData.get(DATA_KLEUR), 0, 3);
    }

    public void setKleur(int kleur) {
        entityData.set(DATA_KLEUR, Math.floorMod(kleur, 4));
    }

    public boolean stijgt() {
        return entityData.get(DATA_STIJGT);
    }

    /** Off it goes: straight up, and away with whoever sits in it. */
    public void stijgOp() {
        entityData.set(DATA_STIJGT, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!stijgt()) {
            return;
        }
        // (both sides move it the same way, so it rises smoothly between the server's position updates)
        gestegen++;
        double vaart = SNELHEID * Math.min(1.0, gestegen / 40.0);
        setPos(getX() + Math.sin(gestegen * 0.02) * 0.012, getY() + vaart, getZ() + 0.01);
        if (!level().isClientSide() && gestegen >= STIJG_TICKS) {
            wegMetAlles();
        }
    }

    /** Removes the balloon and its passengers (they are "in Japan" now: the house brings them back). */
    public void wegMetAlles() {
        for (Entity e : List.copyOf(getPassengers())) {
            e.stopRiding();
            if (!(e instanceof Player)) {
                e.discard();
            }
        }
        discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.weeb.ballon").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
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
    protected boolean canAddPassenger(Entity passenger) {
        return !(passenger instanceof Player) && getPassengers().size() < 2;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int index = Math.max(0, getPassengers().indexOf(passenger));
        Vec3 offset = new Vec3(index == 0 ? 0.3 : -0.3, 0.3, 0.2);
        return offset.yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    /** The guh head floats high above the basket: do not cull it when the basket is off screen (used by the renderer). */
    public AABB cullBox() {
        return getBoundingBox().inflate(3, 0, 3).expandTowards(0, 9, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        tag.putInt("Kleur", kleur());
        tag.putBoolean("Stijgt", stijgt());
        tag.putInt("Gestegen", gestegen);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        setKleur(tag.getIntOr("Kleur", 0));
        entityData.set(DATA_STIJGT, tag.getBooleanOr("Stijgt", false));
        gestegen = tag.getIntOr("Gestegen", 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("zweven", 0, state -> state.setAndContinue(ZWEVEN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
