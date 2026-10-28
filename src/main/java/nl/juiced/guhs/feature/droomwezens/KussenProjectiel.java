package nl.juiced.guhs.feature.droomwezens;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * A pillow thrown by a kussengevecht-guh ({@link DroomNpc}): it plops on whoever it hits (no damage at all, a tiny push and a
 * puff of feathers); a kussengevecht-guh that gets hit cheers ("happy").
 */
public class KussenProjectiel extends ThrowableItemProjectile {
    public KussenProjectiel(EntityType<? extends KussenProjectiel> type, Level level) {
        super(type, level);
    }

    public KussenProjectiel(Level level, LivingEntity werper) {
        super(DroomwezensFeature.KUSSEN_PROJECTIEL.get(), werper, level, new ItemStack(kussenItem()));
    }

    /** The guhkussen it looks like (the pink one). */
    static Item kussenItem() {
        return BuiltInRegistries.ITEM.getOptional(Guhs.id("pink_kussen")).orElse(Items.PINK_WOOL);
    }

    @Override
    protected Item getDefaultItem() {
        return kussenItem();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (result instanceof EntityHitResult hit && hit.getEntity() != getOwner()) {
            var e = hit.getEntity();
            e.push(getDeltaMovement().x * 0.15, 0.08, getDeltaMovement().z * 0.15);
            if (e instanceof GuhNpcEntity npc && npc.getKind() == GuhNpcEntity.Kind.KUSSENGEVECHT_GUH) {
                npc.speel("happy");
            }
        }
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(
                new ItemStack(Items.FEATHER))), getX(), getY(), getZ(), 10, 0.25, 0.25, 0.25, 0.08);
        server.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 4, 0.15, 0.15, 0.15, 0.01);
        server.playSound(null, getX(), getY(), getZ(), DroomwezensFeature.KUSSEN.get(), SoundSource.NEUTRAL, 0.8f, 0.9f + random.nextFloat() * 0.3f);
        discard();
    }
}
