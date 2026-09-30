package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import nl.juiced.guhs.world.WildeDieren;

/**
 * 1.1.2: a come-and-go animal ({@link WildeDieren#isKomEnGa}: a wild Guhs animal, critter or Mika that a spawner brought
 * while playing) is never written to disk: when its chunk unloads it is simply gone, like when you walk away from it.
 * Only entities with the {@link WildeDieren#KOM_EN_GA} tag are affected (Guhs sets it on its own mob types only).
 */
@Mixin(Entity.class)
public abstract class EntitySaveMixin {
    @Inject(method = "shouldBeSaved()Z", at = @At("HEAD"), cancellable = true)
    private void guhs$komEnGaNietOpslaan(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Mob mob && WildeDieren.isKomEnGa(mob)) {
            cir.setReturnValue(false);
        }
    }
}
