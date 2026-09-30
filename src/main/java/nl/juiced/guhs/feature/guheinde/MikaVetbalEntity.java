package nl.juiced.guhs.feature.guheinde;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import nl.juiced.guhs.registry.ModItems;
import org.joml.Vector3f;

/** A ball of Mika's vet thrown by Opper-Mika: a little hurt, and a vetplas where it lands. */
public class MikaVetbalEntity extends ThrowableItemProjectile {
    public static final float DAMAGE = 4f;

    public MikaVetbalEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public MikaVetbalEntity(Level level, LivingEntity thrower) {
        super(GuheindeFeature.MIKA_VETBAL.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.MIKA_VET.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof OpperMikaEntity || result.getEntity() instanceof HongerigeEnderguhEntity) {
            return;
        }
        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), DAMAGE);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            if (this.getOwner() instanceof OpperMikaEntity mika) {
                mika.vetplas(result.getLocation());
            }
            ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(new DustParticleOptions(new Vector3f(0.85f, 0.7f, 0.25f), 2f),
                    getX(), getY(), getZ(), 20, 0.4, 0.4, 0.4, 0.05);
            this.discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }
}
