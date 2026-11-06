package nl.juiced.guhs.feature.vadsrijk.ruiters;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;

/**
 * U-RUITERS: the Pluimguh of a riding guh (vr_ruiter_pluim). The real mount (same model, colours, animations, pattern + tack layer
 * via the economie renderer) but nobody's: no AI, no gravity, no collisions, never hurt, never saved; {@link Ruiters} sets its
 * position every tick along its route ({@link RuiterRoute}), so it can't get stuck. Its rider is a {@link RuiterGuh} passenger.
 * Client: a little dust from its feet while running and now and then a puff of feathers in its own colour (only near you).
 */
public class RuiterPluim extends RijdierEntity {
    public RuiterPluim(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setNoAi(true);
        this.noPhysics = true;
    }

    @Override
    public RijdierSoort soort() {
        return RijdierSoort.PLUIMGUH;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            stofjes();
        }
    }

    /** Client: dust while running, a feather puff in its colour now and then. LOD: only within 24 blocks of the local player. */
    private void stofjes() {
        if (level().getNearestPlayer(this, 24) == null) {
            return;
        }
        double dx = getX() - xo, dz = getZ() - zo;
        double v = Math.sqrt(dx * dx + dz * dz);
        if (v > 0.12 && random.nextInt(3) == 0) {
            Vec3 achter = Vec3.directionFromRotation(0, getYRot()).scale(-0.5);
            level().addParticle(ParticleTypes.POOF, getX() + achter.x + (random.nextDouble() - 0.5) * 0.6, getY() + 0.05,
                    getZ() + achter.z + (random.nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
        }
        if (random.nextInt(v > 0.12 ? 50 : 160) == 0) {
            int kleur = kleur().kleur & 0xFFFFFF;
            for (int i = 0; i < 6; i++) {
                level().addParticle(new DustParticleOptions(kleur, 1.2f), getX() + (random.nextDouble() - 0.5) * 1.2,
                        getY() + 1.4 + random.nextDouble() * 0.8, getZ() + (random.nextDouble() - 0.5) * 1.2,
                        (random.nextDouble() - 0.5) * 0.1, 0.02, (random.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0, ZADEL, 0.05).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    /** The guh's feet on the saddle (the saddle top of the Pluimguh model is ~2.0 blocks up). */
    static final double ZADEL = 1.98;

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof RuiterGuh;
    }

    @Override
    public net.minecraft.world.entity.LivingEntity getControllingPassenger() {
        return null;   // nobody steers: Ruiters moves it
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !level().isClientSide() && player instanceof ServerPlayer sp) {
            Ruiters.klik(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void checkDespawn() {
    }
}
