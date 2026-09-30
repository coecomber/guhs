package nl.juiced.guhs.feature.kaasmoeras;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * A thrown vadsverdrijvend drankje: shatters where it lands and makes everything close by a little onvahoeg
 * ({@link OnvahoegEffect}, {@link #SECONDS} seconds). Mikas (and the Moerasheks herself) are immune: it's their recipe.
 */
public class VadsverdrijvendDrankjeEntity extends ThrowableItemProjectile {
    public static final int SECONDS = 8;
    public static final double RADIUS = 3.0;

    public VadsverdrijvendDrankjeEntity(EntityType<? extends VadsverdrijvendDrankjeEntity> type, Level level) {
        super(type, level);
    }

    public VadsverdrijvendDrankjeEntity(Level level, LivingEntity thrower) {
        super(KaasmoerasFeature.DRANKJE.get(), thrower, level, new net.minecraft.world.item.ItemStack(KaasmoerasFeature.VADSVERDRIJVEND_DRANKJE.get()));
    }

    @Override
    protected Item getDefaultItem() {
        return KaasmoerasFeature.VADSVERDRIJVEND_DRANKJE.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            splash(server);
            discard();
        }
    }

    /** Everyone within {@link #RADIUS} blocks (except Mikas) gets the effect; returns how many. */
    public int splash(ServerLevel server) {
        int hit = 0;
        AABB box = getBoundingBox().inflate(RADIUS, RADIUS * 0.66, RADIUS);
        for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, box)) {
            if (living instanceof MikaEntity || living instanceof MoerasheksMikaEntity || !living.isAffectedByPotions()
                    || distanceToSqr(living) > RADIUS * RADIUS * 2) {
                continue;
            }
            living.addEffect(new MobEffectInstance(KaasmoerasFeature.ONVAHOEG, SECONDS * 20, 0), getEffectSource());
            hit++;
            if (living instanceof ServerPlayer player) {
                GuhAdvancements.grant(player, "kaasmoeras_onvahoeg");
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("quest.guhs.kaasmoeras.onvahoeg")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
            }
        }
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(getItem())),
                getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.1);
        server.sendParticles(net.minecraft.core.particles.SpellParticleOption.create(ParticleTypes.EFFECT, -1, 1.0f), getX(), getY() + 0.3, getZ(),
                30, 1.2, 0.4, 1.2, 0.05);
        server.sendParticles(BorrelendeKaassausBlock.CHEESE_DUST, getX(), getY() + 0.3, getZ(), 20, 1.2, 0.4, 1.2, 0.02);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1f, 0.9f + random.nextFloat() * 0.2f);
        return hit;
    }
}
