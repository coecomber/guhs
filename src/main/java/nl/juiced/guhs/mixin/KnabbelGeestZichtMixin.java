package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.feature.knabbelvelden.Geesten;

/**
 * 1.6.2: the stand-ins of retired maatjes in De Eeuwige Knabbelvelden are invisible entities that everybody may see: a
 * creature that is invisible but not invisible TO you is drawn see-through (vanilla and GeckoLib's GeoEntityRenderer both
 * do that), which is exactly the ghostly look the retired guhs have. So every maatje model works without its own code.
 */
@Mixin(Entity.class)
public abstract class KnabbelGeestZichtMixin {
    @Inject(method = "isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
    private void guhs$geestDoorzichtig(Player player, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.isInvisible() && !(self instanceof nl.juiced.guhs.entity.GuhEntity) && Geesten.isGeest(self)) {
            cir.setReturnValue(false);
        }
    }
}
