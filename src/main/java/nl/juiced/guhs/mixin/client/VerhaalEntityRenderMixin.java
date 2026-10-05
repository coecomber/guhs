package nl.juiced.guhs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.feature.verhaal.client.CutsceneSpeler;

/**
 * bbq2 (verhaal engine): while a cutscene plays, the viewer's real player (the scene has a stand-in) and the real
 * entities near the scene (the NPC whose actor is playing) are not drawn for that viewer: the question "should this
 * entity be rendered?" is answered with no for them ({@link CutsceneSpeler#verborgen}). Nothing changes while no scene
 * plays.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class VerhaalEntityRenderMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void guhs$verborgenTijdensCutscene(E entity, Frustum culler, double camX, double camY, double camZ,
            CallbackInfoReturnable<Boolean> cir) {
        if (CutsceneSpeler.actief() && CutsceneSpeler.verborgen(entity)) {
            cir.setReturnValue(false);
        }
    }
}
