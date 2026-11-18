package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A little card hanging around the kerstboom ({@code guhs:guhwinter_boomkaartje}): shows one submitted kerstboom drawing,
 * gently swinging. Never saved (the tree's {@link KerstboomTekeningen} keeps a few around while players are near and swaps
 * the drawings every half minute); can't be hit; right-click = draw your own kerstboom drawing.
 */
public class BoomKaartje extends Entity {
    private static final EntityDataAccessor<String> DATA_ID = SynchedEntityData.defineId(BoomKaartje.class, EntityDataSerializers.STRING);

    public BoomKaartje(EntityType<? extends BoomKaartje> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_ID, "");
    }

    public void zetTekening(UUID id) {
        String s = id.toString();
        if (!s.equals(entityData.get(DATA_ID))) {
            entityData.set(DATA_ID, s);
        }
    }

    @Nullable
    public UUID tekeningId() {
        String s = entityData.get(DATA_ID);
        try {
            return s.isEmpty() ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void tick() {
        // it just hangs there: no movement, no physics
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity source) {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
            KerstboomTekeningen.open(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
