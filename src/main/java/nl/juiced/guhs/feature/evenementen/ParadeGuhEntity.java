package nl.juiced.guhs.feature.evenementen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.registry.ModSounds;

import net.minecraft.world.level.storage.ValueOutput;
/**
 * A guh of the Vadsparade: the Tamboerguh in front (in the parade outfit) or one of the dressed-up guhs behind it. The
 * parade moves it along its route ({@link Vadsparade}); by itself it does nothing. It isn't anybody's: it can't be tamed,
 * fed, dressed, hurt, pushed or leashed, its clothes never drop, and it's never saved (it poofs when the parade is over).
 */
public class ParadeGuhEntity extends GuhEntity {
    private static final EntityDataAccessor<Boolean> DATA_DRUMMER = SynchedEntityData.defineId(ParadeGuhEntity.class, EntityDataSerializers.BOOLEAN);

    public ParadeGuhEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DRUMMER, false);
    }

    /** No goals: it only walks where the parade puts it. */
    @Override
    protected void registerGoals() {
    }

    public boolean isDrummer() {
        return this.entityData.get(DATA_DRUMMER);
    }

    public void setDrummer(boolean drummer) {
        this.entityData.set(DATA_DRUMMER, drummer);
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable(isDrummer() ? "entity.guhs.tamboerguh" : "entity.guhs.parade_guh");
    }

    /** Only the Tamboerguh wears a name above its head (ten "Paradeguh"s in a row would be a lot of text). */
    @Override
    public boolean shouldShowName() {
        return isDrummer();
    }

    /** Right-click: a happy little VAHOEG hop (a high five with the parade), nothing else. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide()) {
            this.triggerAnim("action", "happy");
            this.playSound(ModSounds.GUH_HAPPY.get(), 0.8f, this.getVoicePitch());
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.NOTE, getX(), getY() + getBbHeight() + 0.3, getZ(), 1, 0.2, 0.1, 0.2, 0.5);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onOwnerTap(Player player) {
    }

    @Override
    public void onLaunchPressed(Player rider) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount > 40 && this.tickCount % 20 == 0 && !Evenementen.owns(this)) {
            this.discard(); // a leftover of a parade that's over (or never was)
        }
    }

    // --- nobody's, never hurt, never saved -------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    protected void dropEquipment() {
        // the parade clothes belong to the parade
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isSaddleable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("ParadeGuh", true);
    }
}
