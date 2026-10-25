package nl.juiced.guhs.feature.ringvlucht;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.spiesburcht.RookguhEntity;

/**
 * 1.4.2 (rookguh): the Reis-Rookguh (entity {@code guhs:ringvlucht_rookguh}): a well-fed, rosy Rookguh of the big family that
 * lifted everybody off the Frituurberg. It hovers at the exit of a structure of the Knabbelring ({@link Haltes}: one per
 * copy, kept there by {@code Bezetting}, also at copies that were generated before 1.4.2) and only exists for players whose
 * story part of that structure is done (ring-kern's {@link Zicht}): so it is one shared NPC that answers every player for
 * themselves. A click opens the choice of where to fly ({@link Vlucht#klik}).
 * <p>
 * It never moves (no goals, it can't be pushed), can't be fed, hurt or tamed, and never despawns. Drawn with the Rookguh's own
 * model, a bit smaller (client.RingVluchtClient).
 */
public class VluchtRookguhEntity extends RookguhEntity {
    /** The stop this Rookguh belongs to (Haltes id). */
    private String halte = "";

    public VluchtRookguhEntity(EntityType<? extends VluchtRookguhEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    /** The Rookguh of this stop at this spot (not yet in the world), only seen by players for whom the stop's story part is done. */
    @Nullable
    public static VluchtRookguhEntity maak(ServerLevel level, Vec3 plek, Haltes.Halte h, float yaw) {
        VluchtRookguhEntity guh = RingVluchtFeature.ROOKGUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.halte = h.id();
        guh.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        guh.setYHeadRot(yaw);
        guh.setYBodyRot(yaw);
        guh.setInvulnerable(true);
        Zicht.alleenBij(guh, h.lijn(), h.verhaallijn().stappen(), 99);
        return guh;
    }

    /**
     * 1.7.1: the Rookguh of an extra place ({@link RookPlatform#GUHRIO}: the forecourt of the Super Guhrio castle,
     * {@link RookPlatform#PLATFORM}: hovering above a Rookguh-platform at {@code blok}): no stop of the story, there for everyone.
     */
    @Nullable
    public static VluchtRookguhEntity maakExtra(ServerLevel level, Vec3 plek, String id, @Nullable BlockPos blok) {
        VluchtRookguhEntity guh = RingVluchtFeature.ROOKGUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.halte = id;
        guh.blok = blok;
        guh.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
        guh.setInvulnerable(true);
        guh.setNoGravity(true);
        return guh;
    }

    /** The platform block of a platform's Rookguh (null for every other one). */
    @Nullable
    private BlockPos blok;

    /** The stop of this Rookguh (null: none, a stray one, or the Rookguh of an extra place). */
    @Nullable
    public Haltes.Halte halte() {
        return Haltes.van(halte);
    }

    /** Its stop id, or {@link RookPlatform#GUHRIO} / {@link RookPlatform#PLATFORM} (1.7.1). */
    public String halteId() {
        return halte;
    }

    @Nullable
    public BlockPos blok() {
        return blok;
    }

    @Override
    protected void registerGoals() {
        // (none: it hovers where it is and looks at whoever comes near)
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        if (RookPlatform.PLATFORM.equals(halte) && tickCount % 40 == 0
                && (blok == null || !level().getBlockState(blok).is(RingVluchtFeature.PLATFORM.get()))) {
            discard();                                        // (1.7.1: its platform is gone)
            return;
        }
        if (tickCount % 10 == 0) {
            Player p = level().getNearestPlayer(this, 10);
            if (p != null) {
                double dx = p.getX() - getX(), dz = p.getZ() - getZ();
                float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
                setYRot(yaw);
                setYBodyRot(yaw);
                setYHeadRot(yaw);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer p) {
            if (RookPlatform.isExtra(halte)) {
                RookPlatform.klikRookguh(p, this);           // (1.7.1: the castle's, a platform's)
            } else {
                Vlucht.klik(p, this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** A Reis-Rookguh has eaten plenty (no knabbels wasted, no saved Rookguh to farm). */
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
        return 1.15f + random.nextFloat() * 0.1f;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Halte", halte);
        if (blok != null) {
            tag.putLong("Blok", blok.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        halte = tag.getStringOr("Halte", "");
        long b = tag.getLongOr("Blok", Long.MIN_VALUE);
        blok = b == Long.MIN_VALUE ? null : BlockPos.of(b);
    }
}
