package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/**
 * U-LEVEN-STAD: a guh's cat on a roof (vr_sl_kat): the vanilla cat (its own renderer and variants) with a pink collar, no AI,
 * silent (its purring would fill the street), sitting where {@link StadsLeven} put it. Never hurt, never saved (noSave type).
 */
public class LevenKat extends Cat {
    public LevenKat(EntityType<? extends Cat> type, Level level) {
        super(type, level);
        setNoAi(true);
        setSilent(true);
    }

    /** A random coat and the pink collar of a guh's pet (server, right after create). */
    void kleed(ServerLevel sl) {
        sl.registryAccess().lookupOrThrow(Registries.CAT_VARIANT).getRandom(getRandom())
                .ifPresent(v -> applyImplicitComponent(DataComponents.CAT_VARIANT, v));
        setTame(true, false);
        applyImplicitComponent(DataComponents.CAT_COLLAR, DyeColor.PINK);
        setOrderedToSit(true);
        setInSittingPose(true);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !level().isClientSide() && player instanceof ServerPlayer sp) {
            StadsLeven.klikKat(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return StadsLeven.ziet(player);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void checkDespawn() {
    }
}
