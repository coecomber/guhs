package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import javax.annotation.Nullable;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * U-LUCHTSCHIP: one click box of the airship (NeoForge multipart, like the ender dragon's parts). The ship has a row of them
 * along the hull, one over the cabin and a row along the balloon ({@link LuchtschipEntity#DELEN}), moved with the ship every tick
 * on both sides, so a right- or left-click anywhere on the model reaches the ship. Only pickable: no collision, no pushing, no
 * damage, arrows fly through, never saved, never sent (the client builds its own parts with the ship).
 */
public class SchipDeel extends PartEntity<LuchtschipEntity> {
    private final EntityDimensions maat;
    /** Ship frame (blocks, +z = bow, y = deck): the middle of the bottom face. */
    final Vec3 plek;

    SchipDeel(LuchtschipEntity schip, Vec3 plek, float breed, float hoog) {
        super(schip);
        this.maat = EntityDimensions.scalable(breed, hoog);
        this.plek = plek;
        refreshDimensions();
        noPhysics = true;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return maat;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        return getParent().interact(player, hand, location);
    }

    @Override
    public boolean isPickable() {
        return !getParent().isRemoved();
    }

    @Override
    public boolean is(Entity other) {
        return this == other || getParent() == other;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }
}
