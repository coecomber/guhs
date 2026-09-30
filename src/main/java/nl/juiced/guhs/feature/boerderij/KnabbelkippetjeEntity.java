package nl.juiced.guhs.feature.boerderij;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Het knabbelkippetje: a round cheese-yellow chick with guh ears, big guh eyes and a tiny beak. When it is content it lays
 * a knabbelei: into the nearest kippennestje with room (within {@link BoerderijDier#VOERBAK_AFSTAND} blocks), or else on
 * the ground. It flutters down gently, never takes fall damage. Tok tok, vahoeg!
 */
public class KnabbelkippetjeEntity extends BoerderijDier {
    public KnabbelkippetjeEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public String soort() {
        return "knabbelkippetje";
    }

    @Override
    protected void geefProduct(@Nullable ServerPlayer player) {
        level().playSound(null, this, SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1f, 1.2f);
        BlockPos nest = KippennestjeBlock.vindPlekje(level(), blockPosition(), VOERBAK_AFSTAND);
        if (nest != null && KippennestjeBlock.leg(level(), nest)) {
            ((ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, nest.getX() + 0.5, nest.getY() + 0.5,
                    nest.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.0);
            return;                                           // (the player gets it from the nest: that counts then)
        }
        ItemEntity ei = new ItemEntity(level(), getX(), getY() + 0.3, getZ(), new ItemStack(BoerderijFeature.KNABBELEI.get()));
        ei.setDefaultPickUpDelay();
        level().addFreshEntity(ei);
        if (player != null) {
            BoerderijVoortgang.product(player, BoerderijVoortgang.Product.KNABBELEI, 1);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 v = getDeltaMovement();
        if (!onGround() && v.y < 0) {
            setDeltaMovement(v.multiply(1, 0.6, 1));          // a gentle flutter down
        }
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void speelGeluid() {
        playSound(BoerderijFeature.KIPPETJE_TOK.get(), 0.8f, getVoicePitch());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return BoerderijFeature.KIPPETJE_TOK.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CHICKEN_SOUNDS.get(net.minecraft.world.entity.animal.chicken.ChickenSoundVariants.SoundSet.CLASSIC).adultSounds().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_SOUNDS.get(net.minecraft.world.entity.animal.chicken.ChickenSoundVariants.SoundSet.CLASSIC).adultSounds().deathSound().value();
    }

    @Override
    public float getVoicePitch() {
        return (isBaby() ? 1.8f : 1.4f) + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Override
    protected net.minecraft.core.particles.ParticleOptions borstelDeeltje() {
        return net.minecraft.core.particles.ParticleTypes.WHITE_ASH;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return BoerderijFeature.KNABBELKIPPETJE.get().create(level, EntitySpawnReason.TRIGGERED);
    }
}
