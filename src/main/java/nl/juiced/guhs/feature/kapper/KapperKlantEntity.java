package nl.juiced.guhs.feature.kapper;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.registry.ModSounds;

import net.minecraft.world.level.storage.ValueOutput;
/**
 * A customer of the kappersshow (kapper_klant, no Guhdex page): a guh that comes in, sits down in the kappersstoel and
 * shows you the picture of the hairstyle it wants. It isn't anybody's: it can't be tamed, fed, dressed, hurt, pushed or
 * leashed, and it's never saved (a leftover of a show that's over poofs away). Right-click it to open the knip screen.
 */
public class KapperKlantEntity extends GuhEntity {
    /** The Kapper Krulletje whose show this customer belongs to (server only). */
    @Nullable
    UUID kapper;
    /** Leaving: counts down, then it poofs (0: not leaving). */
    int weg;

    public KapperKlantEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10f));
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.guhs.kapper_klant");
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    /** Right-click: the knip screen of the show it belongs to. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide() && player instanceof ServerPlayer sp) {
            KappersShow.klik(this, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onOwnerTap(Player player) {
    }

    @Override
    public void onLaunchPressed(Player rider) {
    }

    /** Off it goes: a happy hop off the chair, then a poof. */
    void vertrek(int ticks) {
        weg = ticks;
        setOrderedToSit(false);
        setInSittingPose(false);
        setNoGravity(false);
        triggerAnim("action", "happy");
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (weg > 0 && --weg == 0) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.POOF, getX(), getY() + 0.4, getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            this.playSound(ModSounds.GUH_HAPPY.get(), 0.6f, 1.4f);
            this.discard();
            return;
        }
        if (this.tickCount > 40 && this.tickCount % 20 == 0 && weg == 0 && !KappersShow.hoortBij(this)) {
            this.discard();   // a leftover of a show that's over (or never was)
        }
    }

    // --- nobody's, never hurt, never saved -------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(ServerLevel serverLevel, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    protected void dropEquipment(ServerLevel serverLevel) {
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
        tag.putBoolean("KapperKlant", true);
    }
}
