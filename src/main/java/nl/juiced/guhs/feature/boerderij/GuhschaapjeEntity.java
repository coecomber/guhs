package nl.juiced.guhs.feature.boerderij;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Het guhschaapje: a round cloud of pink-white pluiswol with a guh face, guh ears and little legs. When it is content it
 * sheds a tuft of pluiswol (you see it shorn for the rest of the day, then its wool is fluffy again). Bleh!
 */
public class GuhschaapjeEntity extends BoerderijDier {
    public GuhschaapjeEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    @Override
    public String soort() {
        return "guhschaapje";
    }

    @Override
    protected void geefProduct(@Nullable ServerPlayer player) {
        int n = 2 + getRandom().nextInt(2);
        ItemEntity wol = new ItemEntity(level(), getX(), getY() + 0.8, getZ(), new ItemStack(BoerderijFeature.PLUISWOL.get(), n));
        wol.setDefaultPickUpDelay();
        wol.setDeltaMovement((getRandom().nextDouble() - 0.5) * 0.1, 0.25, (getRandom().nextDouble() - 0.5) * 0.1);
        level().addFreshEntity(wol);
        ((ServerLevel) level()).sendParticles(BoerderijFeature.WOLPLUKJE.get(), getX(), getY() + 0.8, getZ(), 20, 0.4, 0.3, 0.4, 0.05);
        level().playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 0.8f, 1.3f);
        if (player != null) {
            BoerderijVoortgang.product(player, BoerderijVoortgang.Product.PLUISWOL, n);
        }
    }

    @Override
    protected void speelGeluid() {
        playSound(BoerderijFeature.SCHAAPJE_BLEH.get(), 0.8f, getVoicePitch());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return BoerderijFeature.SCHAAPJE_BLEH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return (isBaby() ? 1.7f : 1.35f) + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return BoerderijFeature.GUHSCHAAPJE.get().create(level);
    }
}
