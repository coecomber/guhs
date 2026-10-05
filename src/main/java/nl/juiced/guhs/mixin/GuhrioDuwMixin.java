package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;

/**
 * bbq2 (Super Guhrio): players in a level are not pushed, not by each other and not by anything else. They all stand on
 * the same one-block line, so the game's soft shoving of entities that overlap ({@code Entity#push(Entity)}) would have
 * them push each other along the lane (and a Guhmba would shove you instead of sending you back to your flag). Works on
 * both sides: the player's own game is the one that would move the player.
 */
@Mixin(Entity.class)
public abstract class GuhrioDuwMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void guhs$nietDuwenInEenLevel(Entity entity, CallbackInfo ci) {
        if (((Object) this instanceof Player p && GuhrioSpel.speelt(p)) || (entity instanceof Player q && GuhrioSpel.speelt(q))) {
            ci.cancel();
        }
    }
}
