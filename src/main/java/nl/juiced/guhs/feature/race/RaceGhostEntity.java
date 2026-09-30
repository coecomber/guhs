package nl.juiced.guhs.feature.race;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * The ghost guh: your best race, driven again next to you (see-through, with a trail of sparkles). The race moves it
 * every tick along the recorded positions; it goes through everything, can't be touched and is never saved.
 * 2.9: a golden one ({@link #isGoud}) drives the world's track record.
 */
public class RaceGhostEntity extends GuhEntity {
    /** 2.9: the golden ghost (the track record of the world, someone else's race) instead of your own. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_GOUD =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RaceGhostEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    public RaceGhostEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GOUD, false);
    }

    public boolean isGoud() {
        return this.entityData.get(DATA_GOUD);
    }

    public void setGoud(boolean goud) {
        this.entityData.set(DATA_GOUD, goud);
    }

    @Override
    protected void registerGoals() {
    }

    /** Only the race moves it: no physics of its own (the client just works out the walking animation). */
    @Override
    public void travel(Vec3 input) {
        this.calculateEntityAnimation(false);
    }

    /** Places it on its next spot of the replay, looking the way it goes. */
    public void glideTo(Vec3 pos, float yaw) {
        this.setDeltaMovement(pos.subtract(this.position()));
        this.setPos(pos);
        this.setYRot(yaw);
        this.yBodyRot = this.yHeadRot = yaw;
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        if (!this.level().isClientSide && !RaceGame.isGhost(this) && this.tickCount > 40) {
            this.discard(); // its race is over
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public void onOwnerTap(Player player) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    protected void dropEquipment() {
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
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void playAmbientSound() {
        // ghosts are quiet
    }
}
