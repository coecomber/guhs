package nl.juiced.guhs.feature.vliegdroom;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.spiesburcht.RookguhEntity;

/**
 * 1.8.0 uitbreiding: a Rookguh of the Vliegdroom (entity {@code guhs:droom_rookguh}, drawn as the Rookguh, rosy and well fed).
 * Three kinds ({@link Rol}):
 * <ul>
 *   <li>{@link Rol#STEIGER}: hovers at its steigertje on the Droomplein; a click and it carries you up to the Vliegdroom;</li>
 *   <li>{@link Rol#TERUG}: hovers at the Vliegdroom's landing; a click and it carries you back down to the plein;</li>
 *   <li>{@link Rol#RIJ}: one of the stal; you ride it yourself: it flies where you look (forward), jump = up, sneak = off. Its
 *       rider flies the wolkjesringen-parcours ({@link VliegParcours}).</li>
 * </ul>
 * A carrying flight is the server's (through everything, it never bumps); a ridden one is its rider's (like a horse). Nobody on
 * it: it goes back to its own spot. It can't be hurt, fed, pushed or lost (it never despawns).
 */
public class DroomRookguhEntity extends RookguhEntity {
    public enum Rol { STEIGER, TERUG, RIJ }

    private static final EntityDataAccessor<Integer> DATA_ROL = SynchedEntityData.defineId(DroomRookguhEntity.class, EntityDataSerializers.INT);
    /** How fast a ridden Rookguh flies (blocks a tick), and with sprint. */
    static final double SNELHEID = 0.55, SPRINT = 0.85, STIJGEN = 0.32;
    /** How fast it carries someone. */
    static final double DRAAG = 0.75;

    /** Its own spot (world). */
    private Vec3 thuis = Vec3.ZERO;
    /** Carrying someone to here (world), or null. */
    @Nullable
    private Vec3 doel;
    private float doelYaw;

    public DroomRookguhEntity(EntityType<? extends DroomRookguhEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoGravity(true);
        setInvulnerable(true);
    }

    /** A new one of this kind at its spot (not yet in the world). */
    @Nullable
    public static DroomRookguhEntity maak(ServerLevel level, Rol rol, Vec3 plek, float yaw) {
        DroomRookguhEntity guh = VliegdroomFeature.ROOKGUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.entityData.set(DATA_ROL, rol.ordinal());
        guh.thuis = plek;
        guh.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        guh.setYHeadRot(yaw);
        guh.setYBodyRot(yaw);
        return guh;
    }

    public Rol rol() {
        int i = entityData.get(DATA_ROL);
        return Rol.values()[Mth.clamp(i, 0, Rol.values().length - 1)];
    }

    public Vec3 thuis() {
        return thuis;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ROL, Rol.RIJ.ordinal());
    }

    @Override
    protected void registerGoals() {
        // (none: it hovers, carries, or is ridden)
    }

    // --- riding ---------------------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer p) {
            if (isVehicle()) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.vliegdroom.bezet").withStyle(ChatFormatting.GRAY));
                return InteractionResult.SUCCESS;
            }
            switch (rol()) {
                case RIJ -> {
                    if (p.startRiding(this, true, false)) {
                        p.sendSystemMessage(Component.translatable("gui.guhs.vliegdroom.rij").withStyle(ChatFormatting.AQUA));
                    }
                }
                case STEIGER -> draag(p, VliegPlekken.aankomst(), "gui.guhs.vliegdroom.omhoog");
                case TERUG -> draag(p, VliegPlekken.pleinLanding(), "gui.guhs.vliegdroom.omlaag");
            }
            level().playSound(null, blockPosition(), nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.8f, 1.3f);
        }
        return InteractionResult.SUCCESS;
    }

    /** Carry this player to there (the Vliegdroom, or down to the plein). */
    void draag(ServerPlayer p, Vec3 naar, String tekst) {
        if (!p.startRiding(this, true, false)) {
            return;
        }
        doel = naar;
        doelYaw = p.getYRot();
        noPhysics = true;
        p.sendSystemMessage(Component.translatable(tekst).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 20, 0.8, 0.4, 0.8, 0.02);
        }
    }

    public boolean draagt() {
        return doel != null;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (rol() == Rol.RIJ && doel == null && getFirstPassenger() instanceof Player rider) {
            return rider;
        }
        return null;
    }

    @Override
    public boolean isFlyingVehicle() {
        return true;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0, dimensions.height() * 0.82, 0);
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        float yaw = getYRot() + Mth.wrapDegrees(player.getYRot() - getYRot()) * 0.25f;
        setRot(yaw, 0);
        this.yRotO = this.yBodyRot = this.yHeadRot = yaw;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return new Vec3(player.xxa, 0, player.zza);
    }

    @Override
    public void travel(Vec3 input) {
        if (getControllingPassenger() instanceof Player rider) {
            stuur(rider, input);
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Its rider steers: forward = where they look (also up and down), jump = straight up, sprint = faster. */
    private void stuur(Player rider, Vec3 input) {
        Vec3 look = rider.getLookAngle();
        Vec3 links = new Vec3(look.z, 0, -look.x);
        links = links.lengthSqr() < 1e-6 ? Vec3.ZERO : links.normalize();
        double sp = rider.isSprinting() ? SPRINT : SNELHEID;
        double vooruit = input.z > 0 ? input.z : input.z * 0.4;
        Vec3 want = look.scale(vooruit * sp).add(links.scale(input.x * sp * 0.6));
        if (rider.isJumping()) {
            want = want.add(0, STIJGEN, 0);
        }
        if (getY() > level().getMaxY() - 4 && want.y > 0) {
            want = new Vec3(want.x, 0, want.z);
        }
        Vec3 m = getDeltaMovement().scale(0.85).add(want.scale(0.15));
        setDeltaMovement(m);
        move(MoverType.SELF, m);
        calculateEntityAnimation(true);
        resetFallDistance();
    }

    // --- every tick: carrying, going home, looking at people ------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (doel != null) {
            if (!isVehicle()) {
                doel = null;
                noPhysics = false;
            } else {
                draagStap(level);
                return;
            }
        }
        if (isVehicle()) {
            if (rol() == Rol.RIJ && getFirstPassenger() instanceof ServerPlayer p) {
                VliegParcours.tick(p, level);
            }
            return;
        }
        double ver = position().distanceTo(thuis);
        if (ver > 48) {
            naarHuis(level);
        } else if (ver > 0.3) {
            Vec3 stap = thuis.subtract(position()).normalize().scale(Math.min(0.35, ver));
            noPhysics = true;
            setPos(getX() + stap.x, getY() + stap.y, getZ() + stap.z);
            setYRot((float) (Mth.atan2(stap.z, stap.x) * Mth.RAD_TO_DEG) - 90f);
        } else {
            noPhysics = false;
            if (tickCount % 10 == 0) {
                Player p = level.getNearestPlayer(this, 10);
                if (p != null) {
                    float yaw = (float) (Mth.atan2(p.getZ() - getZ(), p.getX() - getX()) * Mth.RAD_TO_DEG) - 90f;
                    setYRot(yaw);
                    setYBodyRot(yaw);
                    setYHeadRot(yaw);
                }
            }
        }
    }

    /** One tick of carrying: towards the goal, slowing down at the end; there, its rider gets off. */
    private void draagStap(ServerLevel level) {
        Vec3 naar = doel.subtract(position());
        double ver = naar.length();
        if (ver < 1.2) {
            aankomen(level);
            return;
        }
        double sp = Math.min(DRAAG, 0.12 + ver * 0.08);
        Vec3 m = getDeltaMovement().scale(0.7).add(naar.scale(sp / ver).scale(0.3));
        setDeltaMovement(m);
        setPos(getX() + m.x, getY() + m.y, getZ() + m.z);
        if (m.horizontalDistanceSqr() > 1e-4) {
            float yaw = (float) (Mth.atan2(m.z, m.x) * Mth.RAD_TO_DEG) - 90f;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
        if (tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 2, 0.4, 0.2, 0.4, 0.01);
        }
    }

    /** There: its rider stands on the spot, and it floats back home. */
    private void aankomen(ServerLevel level) {
        Entity rider = getFirstPassenger();
        Vec3 plek = doel;
        doel = null;
        ejectPassengers();
        if (rider instanceof ServerPlayer p) {
            p.teleportTo(level, plek.x, plek.y, plek.z, java.util.Set.of(), doelYaw, 0f, false);
            p.resetFallDistance();
            level.playSound(null, p.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
        naarHuis(level);
    }

    /** Back to its own spot at once (with a puff of cloud on both ends). */
    private void naarHuis(ServerLevel level) {
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 12, 0.6, 0.4, 0.6, 0.02);
        noPhysics = false;
        teleportTo(thuis.x, thuis.y, thuis.z);
        setDeltaMovement(Vec3.ZERO);
        level.sendParticles(ParticleTypes.CLOUD, thuis.x, thuis.y + 0.5, thuis.z, 12, 0.6, 0.4, 0.6, 0.02);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof ServerPlayer p) {
            VliegParcours.stop(p.getUUID());
        }
    }

    // --- an easy-going Rookguh ------------------------------------------------------------------------------------------

    @Override
    public void feed(@Nullable ServerPlayer feeder) {
    }

    @Override
    public float plumpness() {
        return 1f;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 1.2f + random.nextFloat() * 0.1f;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Rol", entityData.get(DATA_ROL));
        tag.putDouble("ThuisX", thuis.x);
        tag.putDouble("ThuisY", thuis.y);
        tag.putDouble("ThuisZ", thuis.z);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_ROL, tag.getIntOr("Rol", Rol.RIJ.ordinal()));
        thuis = new Vec3(tag.getDoubleOr("ThuisX", getX()), tag.getDoubleOr("ThuisY", getY()), tag.getDoubleOr("ThuisZ", getZ()));
    }
}
