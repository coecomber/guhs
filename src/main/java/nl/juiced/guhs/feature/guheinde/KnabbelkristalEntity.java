package nl.juiced.guhs.feature.guheinde;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.registry.ModItems;
import org.joml.Vector3f;

/**
 * A knabbelkristal: a floating crystal stuffed with the kaasknabbels Opper-Mika stole. On top of the kaaspilaren they
 * heal him (a beam); smash one and the knabbels rain back down (and his Enderguh gets a little more vahoeg). The four on
 * the knabbelsokkels call Opper-Mika back. Uses the end crystal's model (client: KnabbelkristalRenderer).
 */
public class KnabbelkristalEntity extends EndCrystal {
    public KnabbelkristalEntity(EntityType<? extends EndCrystal> type, Level level) {
        super(type, level);
    }

    public KnabbelkristalEntity(Level level, double x, double y, double z) {
        this(GuheindeFeature.KNABBELKRISTAL_ENTITY.get(), level);
        setPos(x, y, z);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || source.getEntity() instanceof OpperMikaEntity || source.getEntity() instanceof HongerigeEnderguhEntity) {
            return false;
        }
        if (!isRemoved() && !level().isClientSide()) {
            smash(source);
        }
        return true;
    }

    @Override
    public void kill() {
        if (!level().isClientSide() && !isRemoved()) {
            smash(damageSources().generic());
        }
    }

    /** Pop! The stolen knabbels fly out, a harmless puff, and the fight hears about it. */
    public void smash(DamageSource source) {
        remove(Entity.RemovalReason.KILLED);
        ServerLevel level = (ServerLevel) level();
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.5f, 1.3f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 2f, 0.8f);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.82f, 0.3f), 2f), getX(), getY() + 1, getZ(), 60, 1.2, 1.2, 1.2, 0.2);
        for (int i = 0; i < 6; i++) {
            ItemEntity knabbel = new ItemEntity(level, getX(), getY() + 1, getZ(), new ItemStack(ModItems.KAAS_KNABBELS.get()));
            knabbel.setDeltaMovement(random.nextGaussian() * 0.25, 0.4 + random.nextDouble() * 0.3, random.nextGaussian() * 0.25);
            level.addFreshEntity(knabbel);
        }
        GuheindeGevecht fight = GuheindeGevecht.of(level);
        if (fight != null) {
            fight.onCrystalSmashed(this, source);
        }
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(GuheindeFeature.KNABBELKRISTAL.get());
    }
}
