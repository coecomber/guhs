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

    @Override
    public boolean isPickable() {
        return false;
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
