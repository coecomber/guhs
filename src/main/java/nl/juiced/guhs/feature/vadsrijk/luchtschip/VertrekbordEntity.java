package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * T-LUCHTSCHIP: one line of text of a harbour's departure board (an invisible marker whose name tag is the text; the board is
 * three of them stacked: line + destination, the countdown, how to board). {@link Luchtschepen} spawns them when someone is near,
 * rewrites the countdown every second, and removes them again. Never saved, never hit, never pushed.
 */
public class VertrekbordEntity extends Entity {
    public VertrekbordEntity(EntityType<? extends VertrekbordEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setCustomNameVisible(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    /**
     * U-LUCHTSCHIP: the board can be clicked (right or left): "may I fly from here, and if not, why not?". The click box (2.4 x 0.32,
     * {@code LuchtschipRegistry}) lies over the text line: the entity stands {@link #ONDER} higher than before and its name tag
     * hangs that much lower (nameTagOffset), so the text did not move.
     */
    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    /** U-LUCHTSCHIP: how much higher than the old marker the entity stands now (see {@link #isPickable}). */
    public static final double ONDER = 0.6;
    /** (server) the harbour of this board line. */
    @javax.annotation.Nullable
    public String haven;

    @Override
    public net.minecraft.world.InteractionResult interact(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.Vec3 location) {
        if (!level().isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp && hand == net.minecraft.world.InteractionHand.MAIN_HAND
                && haven != null) {
            Luchtschepen.bordKlik(sp, haven);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }
}
