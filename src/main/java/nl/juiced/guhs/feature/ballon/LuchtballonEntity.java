package nl.juiced.guhs.feature.ballon;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * The guh_luchtballon: a hot-air balloon shaped like a big guh head (ears, eyes, blush and all) with a wicker basket.
 * It waits on its ballonsteiger ("home"); Kapitein Wolkje takes you on a round flight ({@link #stijgOp}): a fixed
 * route ({@link BallonRoute}), no steering, past two viewpoints, back down onto the steiger. You can't jump out halfway
 * (BallonEvents: "Njeg! Blijf lekker in het mandje").
 * <p>
 * Both sides move the balloon themselves along the same path (home, turn, route and the terrain lifts are synced), so
 * the flight is smooth; the server sends how far it is now and then, and the client only snaps to that when it's far off.
 */
public class LuchtballonEntity extends Entity implements GeoEntity {
    public static final int KLEUREN = 4;
    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(LuchtballonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_VLIEGT = SynchedEntityData.defineId(LuchtballonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ROUTE = SynchedEntityData.defineId(LuchtballonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_AFSTAND = SynchedEntityData.defineId(LuchtballonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<BlockPos> DATA_THUIS = SynchedEntityData.defineId(LuchtballonEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(LuchtballonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> DATA_LIFT = SynchedEntityData.defineId(LuchtballonEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean heeftThuis;
    /** A festival decoration (template NBT "Deco": 1b): it just floats there, tied down; Kapitein Wolkje never takes it. */
    private boolean deco;
    private double afstand;
    @Nullable
    private BallonRoute.Pad pad;
    /** Server: which guide texts of this flight were said already. */
    private int fase;
    /** Server: the Kapitein Wolkje who stepped in (he's invisible at his kiosk meanwhile). */
    @Nullable
    private UUID kapitein;
    /** Server: true while the passengers are let out (landing): only then may they get off. */
    boolean uitstappen;

    public LuchtballonEntity(EntityType<? extends LuchtballonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KLEUR, 0);
        builder.define(DATA_VLIEGT, false);
        builder.define(DATA_ROUTE, 0);
        builder.define(DATA_AFSTAND, 0f);
        builder.define(DATA_THUIS, BlockPos.ZERO);
        builder.define(DATA_YAW, 0f);
        builder.define(DATA_LIFT, new CompoundTag());
    }

    // --- state ------------------------------------------------------------------------------------------------------------

    public boolean isDeco() {
        return deco;
    }

    public int kleur() {
        return Mth.clamp(entityData.get(DATA_KLEUR), 0, KLEUREN - 1);
    }

    public void setKleur(int kleur) {
        entityData.set(DATA_KLEUR, Math.floorMod(kleur, KLEUREN));
    }

    public boolean vliegt() {
        return entityData.get(DATA_VLIEGT);
    }

    public BallonRoute route() {
        return BallonRoute.byIndex(entityData.get(DATA_ROUTE));
    }

    public double afstand() {
        return afstand;
    }

    /** Where it waits: the middle of its block, on the floor. */
    public Vec3 thuis() {
        BlockPos b = entityData.get(DATA_THUIS);
        return new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
    }

    public BlockPos thuisBlok() {
        return entityData.get(DATA_THUIS);
    }

    /** The festival's turn (the balloon's yaw when the structure put it there). */
    public float routeYaw() {
        return entityData.get(DATA_YAW);
    }

    public void setThuis(BlockPos pos, float yaw) {
        entityData.set(DATA_THUIS, pos.immutable());
        entityData.set(DATA_YAW, yaw);
        heeftThuis = true;
    }

    @Nullable
    public BallonRoute.Pad pad() {
        if (pad == null && vliegt()) {
            pad = route().pad(thuis(), routeYaw(), lifts(), BallonVlucht.schaal());
        }
        return pad;
    }

    private float[] lifts() {
        ListTag list = entityData.get(DATA_LIFT).getListOrEmpty("L");
        float[] out = new float[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.getFloatOr(i, 0.0F);
        }
        return out;
    }

    @Nullable
    public UUID kapitein() {
        return kapitein;
    }

    // --- flying -----------------------------------------------------------------------------------------------------------

    /**
     * Server: take off with this player on this route (Kapitein Wolkje steps in too). Every control point is lifted
     * so the path stays {@value BallonRoute#VRIJ_BOVEN_GROND} blocks above the ground there and halfway to the next.
     */
    public boolean stijgOp(ServerPlayer player, BallonRoute route, @Nullable GuhNpcEntity kapiteinNpc) {
        if (vliegt() || deco || level().isClientSide()) {
            return false;
        }
        Vec3 home = thuis();
        float yaw = routeYaw();
        float[] lift = new float[route.punten()];
        for (int i = 2; i < route.punten() - 2; i++) {
            double sc = BallonVlucht.schaal();
            Vec3 p = home.add(route.draai(i, yaw).scale(sc));
            Vec3 half = home.add(route.draai(i, yaw).add(route.draai(i + 1, yaw)).scale(0.5 * sc));
            int grond = Math.max(hoogte(p), hoogte(half));
            lift[i] = (float) Math.max(0, grond + BallonRoute.VRIJ_BOVEN_GROND - p.y);
        }
        ListTag list = new ListTag();
        for (float f : lift) {
            list.add(FloatTag.valueOf(f));
        }
        CompoundTag tag = new CompoundTag();
        tag.put("L", list);
        entityData.set(DATA_LIFT, tag);
        entityData.set(DATA_ROUTE, route.ordinal());
        afstand = 0;
        entityData.set(DATA_AFSTAND, 0f);
        pad = null;
        fase = 0;
        entityData.set(DATA_VLIEGT, true);
        if (kapiteinNpc != null) {
            kapitein = kapiteinNpc.getUUID();
            kapiteinNpc.setInvisible(true);
        }
        player.startRiding(this, true);
        BallonVlucht.vertrek(player, this);
        return true;
    }

    private int hoogte(Vec3 p) {
        BlockPos b = BlockPos.containing(p);
        if (!level().hasChunkAt(b)) {
            return (int) thuis().y;
        }
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, b.getX(), b.getZ());
    }

    /** Server: back home (landed, or broken off): passengers out on the steiger, the Kapitein back at his kiosk. */
    public void land(boolean geslaagd) {
        if (level().isClientSide()) {
            return;
        }
        BallonRoute route = route();
        List<Entity> passagiers = List.copyOf(getPassengers());
        entityData.set(DATA_VLIEGT, false);
        pad = null;
        afstand = 0;
        entityData.set(DATA_AFSTAND, 0f);
        Vec3 home = thuis();
        setPos(home);
        uitstappen = true;
        try {
            ejectPassengers();
        } finally {
            uitstappen = false;
        }
        for (Entity e : passagiers) {
            Vec3 uit = home.add(Vec3.directionFromRotation(0, routeYaw()).scale(-1.8));
            e.teleportTo(uit.x, home.y + 0.1, uit.z);
            e.resetFallDistance();
            if (e instanceof ServerPlayer player) {
                BallonVlucht.geland(player, this, route, geslaagd);
            }
        }
        kapiteinTerug();
    }

    private void kapiteinTerug() {
        if (kapitein != null && level() instanceof ServerLevel server && server.getEntity(kapitein) instanceof GuhNpcEntity npc) {
            npc.setInvisible(false);
        }
        kapitein = null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!heeftThuis && !level().isClientSide()) {
            setThuis(blockPosition(), getYRot());
        }
        if (!vliegt()) {
            Vec3 home = thuis();
            if (!level().isClientSide() && heeftThuis && position().distanceToSqr(home) > 0.01) {
                setPos(home);
            }
            return;
        }
        BallonRoute.Pad p = pad();
        if (p == null) {
            return;
        }
        afstand = Math.min(p.lengte(), afstand + p.snelheid(afstand) * BallonVlucht.tempo());
        Vec3 at = p.op(afstand);
        setPos(at);
        Vec3 dir = p.richting(afstand);
        if (dir.horizontalDistanceSqr() > 1e-4) {
            float doel = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
            setYRot(Mth.approachDegrees(getYRot(), doel, 1.5f));
        }
        if (level().isClientSide()) {
            if (random.nextInt(dir.y > 0.02 ? 3 : 9) == 0) {       // the burner
                level().addParticle(ParticleTypes.SMALL_FLAME, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 2.6,
                        getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0.05, 0);
            }
            if (random.nextInt(14) == 0) {
                level().addParticle(BallonFeature.BALLONWOLKJE.get(), getX() + (random.nextDouble() - 0.5) * 10, getY() + random.nextDouble() * 6,
                        getZ() + (random.nextDouble() - 0.5) * 10, 0.02, 0, 0);
            }
            return;
        }
        if (tickCount % 20 == 0) {
            entityData.set(DATA_AFSTAND, (float) afstand);
        }
        if (dir.y > 0.02 && tickCount % 30 == 0) {
            level().playSound(null, this, BallonFeature.BRANDER.get(), SoundSource.NEUTRAL, 0.7f, 0.9f + random.nextFloat() * 0.2f);
        }
        if (tickCount % 90 == 0) {
            level().playSound(null, this, BallonFeature.WIND.get(), SoundSource.AMBIENT, 0.4f, 1f);
        }
        fase = BallonVlucht.onderweg(this, p, afstand, fase);
        if (getPassengers().isEmpty() && afstand > 2) {
            land(false);                // nobody in it any more: straight home
        } else if (afstand >= p.lengte() - 1e-6) {
            land(true);
        }
    }

    /** The client follows the path itself; it only takes the server's distance when it's far off. */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!level().isClientSide()) {
            return;
        }
        if (key == DATA_VLIEGT || key == DATA_LIFT || key == DATA_ROUTE || key == DATA_THUIS || key == DATA_YAW) {
            pad = null;
            if (key == DATA_VLIEGT && vliegt()) {
                afstand = entityData.get(DATA_AFSTAND);
            }
        } else if (key == DATA_AFSTAND && vliegt()) {
            double server = entityData.get(DATA_AFSTAND);
            if (Math.abs(server - afstand) > 3) {
                afstand = server;
            }
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (!vliegt()) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
        }
    }

    // --- riding -------------------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide() && !vliegt() && player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.ballon.praat_met_wolkje")
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild && !vliegt() && player.isShiftKeyDown()) {
            kapiteinTerug();
            discard();              // (creative: sneak-hit removes it)
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int index = Math.max(0, getPassengers().indexOf(passenger));
        Vec3 offset = new Vec3(index == 0 ? 0.25 : -0.3, 0.3, 0.25);
        return offset.yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof Player) {
            float turn = Mth.wrapDegrees(getYRot() - yRotO);
            if (Math.abs(turn) < 45) {
                passenger.setYRot(passenger.getYRot() + turn);
                passenger.setYHeadRot(passenger.getYHeadRot() + turn);
            }
        }
    }

    /** Out of the basket high in the air after all (a teleport, ...): you float down instead of falling. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && vliegt() && passenger instanceof net.minecraft.world.entity.LivingEntity living) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 400, 0, false, false));
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    /** It's big (the guh head floats high above the basket): don't cull it as soon as the basket is off-screen. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3, 0, 3).expandTowards(0, 9, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    // --- saving -------------------------------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kleur", kleur());
        tag.putBoolean("Deco", deco);
        if (heeftThuis) {
            tag.store("Thuis", BlockPos.CODEC, thuisBlok());
            tag.putFloat("RouteYaw", routeYaw());
        }
        // (a flight in progress isn't saved: after a restart the balloon is home again)
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setKleur(tag.getIntOr("Kleur", 0));
        deco = tag.getBooleanOr("Deco", false);
        (tag).read("Thuis", BlockPos.CODEC).ifPresent(pos -> setThuis(pos, tag.getFloatOr("RouteYaw", 0.0F)));
    }

    // --- GeckoLib -----------------------------------------------------------------------------------------------------------

    private static final RawAnimation ZWEVEN = RawAnimation.begin().thenLoop("animation.guh_luchtballon.zweven");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("zweven", 0, state -> state.setAndContinue(ZWEVEN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
