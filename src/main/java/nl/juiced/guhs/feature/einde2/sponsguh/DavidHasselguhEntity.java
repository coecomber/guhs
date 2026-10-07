package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.RijGuard;

/**
 * einde2, slice sponsguh_wezens: David Hasselguh, a giant guh who lies flat and swims through the void mist as if it were
 * the sea (about nine blocks long; he floats, he never walks). He carries a player on his back to a goal ({@link Hasselguh},
 * the ride API): the server moves him along a gentle arc, nobody can get off or fall off on the way, and at the goal he
 * sets everybody down. Up to two more riders sit behind the player ({@link #zetMee}: the cast).
 * <p>
 * hook fase B: the way home at the end of the Sponsguh story.
 */
public class DavidHasselguhEntity extends SponsguhWezen {
    private static final EntityDataAccessor<Boolean> DATA_ONDERWEG = SynchedEntityData.defineId(DavidHasselguhEntity.class, EntityDataSerializers.BOOLEAN);
    /** Blocks per tick at full speed, and how far from the goal he slows down. */
    public static final double SNELHEID = 0.55, REM_AFSTAND = 6.0;
    public static final int PLAATSEN = 3;

    /** Where he swims to, where he began, and where his player stood before getting on (a ride that is broken off ends there). */
    @Nullable
    private Vec3 doel, start, terug;
    @Nullable
    private UUID ruiter;
    private double boog;
    /** Made for one ride ({@link Hasselguh#draag} found no Hasselguh nearby): he swims off afterwards. */
    private boolean tijdelijk;
    private int wegOver = -1;
    /** Server: true while the riders are set down: only then may they get off. */
    boolean uitstappen;

    public DavidHasselguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ONDERWEG, false);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Tijdelijk", tijdelijk);
        // (a ride in progress is not saved: after a restart he floats where he was and the player stands at the start again)
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        tijdelijk = tag.getBooleanOr("Tijdelijk", false);
        if (tijdelijk) {
            wegOver = 40;
        }
    }

    /** Is he carrying somebody to a goal right now? */
    public boolean onderweg() {
        return entityData.get(DATA_ONDERWEG);
    }

    @Nullable
    public UUID ruiter() {
        return ruiter;
    }

    void zetTijdelijk() {
        tijdelijk = true;
    }

    @Override
    protected boolean magLopen() {
        return false;                                                        // (he floats in place)
    }

    @Override
    protected String beweging(boolean beweegt) {
        return onderweg() || houding().equals("zwem") ? "zwem" : "idle";
    }

    // --- the ride ------------------------------------------------------------------------------------------------------------

    /** Server: takes this player on his back and swims to doel. False: he is busy, or the player can not get on. */
    boolean vertrek(ServerPlayer player, Vec3 naar) {
        if (level().isClientSide() || onderweg() || !isAlive()) {
            return false;
        }
        Vec3 stond = player.position();
        if (player.getVehicle() != this) {
            player.stopRiding();
            if (!RijGuard.mag(player, this) || !player.startRiding(this, true, true)) {
                return false;
            }
        }
        doel = naar;
        start = position();
        terug = stond;
        ruiter = player.getUUID();
        boog = Math.min(12.0, start.distanceTo(naar) * 0.12);
        wegOver = -1;
        entityData.set(DATA_ONDERWEG, true);
        level().playSound(null, blockPosition(), E2SponsguhFeature.HASSELGUH_PLONS.get(), SoundSource.NEUTRAL, 1.2f, 0.8f);
        return true;
    }

    /**
     * Up to two more riders behind the player (the cast: Sponsguh and Patguh ride home too). False: no seat left, or it would
     * make a riding loop.
     */
    public boolean zetMee(Entity e) {
        if (level().isClientSide() || getPassengers().size() >= PLAATSEN || e.getVehicle() == this || !RijGuard.mag(e, this)) {
            return false;
        }
        return e.startRiding(this, true, true);
    }

    /** Server: the ride ends here and now: everybody off, the player at {@code waar} (the goal, or the start when broken off). */
    void zetAf(boolean aangekomen) {
        if (level().isClientSide() || !onderweg()) {
            return;
        }
        Vec3 waar = aangekomen ? (doel == null ? position() : doel) : (terug == null ? position() : terug);
        List<Entity> passagiers = List.copyOf(getPassengers());
        UUID wie = ruiter;
        entityData.set(DATA_ONDERWEG, false);
        doel = null;
        start = null;
        terug = null;
        ruiter = null;
        uitstappen = true;
        try {
            ejectPassengers();
        } finally {
            uitstappen = false;
        }
        int n = 0;
        for (Entity e : passagiers) {
            boolean speler = e.getUUID().equals(wie);
            double opzij = speler ? 0 : (n++ == 0 ? 1.2 : -1.2);
            e.teleportTo(waar.x + opzij, waar.y + 0.1, waar.z);
            e.resetFallDistance();
            e.setDeltaMovement(Vec3.ZERO);
        }
        level().playSound(null, blockPosition(), E2SponsguhFeature.HASSELGUH_PLONS.get(), SoundSource.NEUTRAL, 1.0f, 1.1f);
        if (tijdelijk) {
            wegOver = 80;
        }
        if (aangekomen) {
            for (Entity e : passagiers) {
                if (e instanceof ServerPlayer p && p.getUUID().equals(wie)) {
                    Hasselguh.AANGEKOMEN.forEach(h -> h.accept(p));          // hook fase B: the story hears the landing
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        if (!onderweg()) {
            if (wegOver > 0 && --wegOver == 0) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 1, getZ(), 30, 2.0, 0.6, 2.0, 0.02);
                discard();                                                   // (he was only here for the ride)
            }
            return;
        }
        Entity rijder = getFirstPassenger();
        if (doel == null || start == null || rijder == null || !rijder.getUUID().equals(ruiter)) {
            zetAf(false);                                                    // (the rider is gone: no ride)
            return;
        }
        Vec3 vlak = new Vec3(doel.x - getX(), 0, doel.z - getZ());
        double rest = vlak.length();
        double heel = Math.max(1.0, Math.hypot(doel.x - start.x, doel.z - start.z));
        if (rest < 0.6 && Math.abs(doel.y - rugHoogte() - getY()) < 1.0) {
            zetAf(true);
            return;
        }
        double stap = Math.min(rest, SNELHEID * Mth.clamp(rest / REM_AFSTAND, 0.25, 1.0));
        Vec3 naar = rest < 1.0e-4 ? Vec3.ZERO : vlak.scale(stap / rest);
        // the height: a straight line from start to goal with a soft arc on top; his back ends just under the goal
        double t = Mth.clamp(1.0 - (rest - stap) / heel, 0.0, 1.0);
        double y = Mth.lerp(t, start.y, doel.y - rugHoogte()) + Math.sin(Math.PI * t) * boog;
        y = getY() + Mth.clamp(y - getY(), -0.4, 0.4);
        setPos(getX() + naar.x, y, getZ() + naar.z);
        if (rest > 0.5) {
            float richting = (float) (Mth.atan2(vlak.z, vlak.x) * Mth.RAD_TO_DEG) - 90f;
            setYRot(Mth.approachDegrees(getYRot(), richting, 6f));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
        if (tickCount % 6 == 0) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 0.4, getZ(), 4, 1.4, 0.3, 1.4, 0.01);
        }
    }

    /** How high above his feet his back is (where the riders sit). */
    public double rugHoogte() {
        return 1.55;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < PLAATSEN;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return null;                                                         // (he swims himself: nobody steers)
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int index = Math.max(0, getPassengers().indexOf(passenger));
        Vec3 plek = new Vec3(index == 2 ? -0.9 : index == 1 ? 0.9 : 0, rugHoogte(), index == 0 ? 0.6 : -1.4);
        return plek.yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
