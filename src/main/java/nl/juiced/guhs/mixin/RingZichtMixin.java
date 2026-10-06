package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.feature.ring.Zicht;

/**
 * bbq2 (ring-kern): who gets to know about an entity. The entity tracker asks {@code Entity#broadcastToPlayer} for every
 * (entity, player) pair before it tells that player's game the entity exists (and again whenever the player moves):
 * {@link Zicht#magZien} says no for a creature behind a Guhdalfs sluier that is closed for that player (CONTRACT_130 13.9:
 * nothing inside leaks out), for a companion that belongs to another player (everybody has their own Sam-guh) and for a
 * cast member whose scene is not that player's step of the story.
 */
@Mixin(Entity.class)
public abstract class RingZichtMixin {
    @Inject(method = "broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("HEAD"), cancellable = true)
    private void guhs$alleenVoorWieHetMagZien(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!Zicht.magZien(player, (Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
