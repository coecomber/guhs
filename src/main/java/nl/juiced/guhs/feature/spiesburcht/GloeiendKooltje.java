package nl.juiced.guhs.feature.spiesburcht;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A glowing ember (gloeiend kooltje, from a Vonk-Mika) or a big burning coal (brandend kooltje, from the Aangebrande
 * Mika). Hurts and singes whoever it hits, but it never sets blocks on fire and never breaks anything: it just puffs
 * out in a cloud of sparks.
 */
public class GloeiendKooltje extends Fireball {
    public GloeiendKooltje(EntityType<? extends GloeiendKooltje> type, Level level) {
        super(type, level);
        setItem(new ItemStack(SpiesburchtFeature.GLOEIEND_KOOLTJE_ITEM.get()));
    }

    public GloeiendKooltje(EntityType<? extends GloeiendKooltje> type, LivingEntity owner, Vec3 direction, Level level) {
        super(type, owner, direction, level);
        setItem(new ItemStack(SpiesburchtFeature.GLOEIEND_KOOLTJE_ITEM.get()));
    }

    /** The big slow coals of the Aangebrande Mika. */
    public boolean isBig() {
        return getType() == SpiesburchtFeature.BRANDEND_KOOLTJE.get();
    }

    public float damage() {
        return isBig() ? 5.0f : 4.0f;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel) {
            Entity target = result.getEntity();
            Entity owner = getOwner();
            if (owner != null && (target == owner || target.getType() == owner.getType())) {
                return;                  // Mikas don't burn each other
            }
            int fire = target.getRemainingFireTicks();
            target.igniteForSeconds(isBig() ? 3.0f : 2.0f);
            DamageSource source = damageSources().fireball(this, owner);
            if (!target.hurt(source, damage())) {
                target.setRemainingFireTicks(fire);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), isBig() ? 6 : 2, 0.2, 0.2, 0.2, 0.0);
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), isBig() ? 12 : 4, 0.25, 0.25, 0.25, 0.02);
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return isBig() ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected float getInertia() {
        return isBig() ? 0.99f : super.getInertia();
    }
}
