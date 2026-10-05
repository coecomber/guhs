package nl.juiced.guhs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;
import nl.juiced.guhs.feature.guhrio.client.BaanCamera;

/**
 * bbq2 (Super Guhrio): the side view of a level. After the game has worked out its own camera for this frame
 * ({@code Camera#alignWithEntity}: first or third person, pushed closer by whatever is behind you), a player in a level
 * gets the lane's camera instead ({@link BaanCamera#stand}): beside the lane, looking straight at it. Outside a level
 * nothing changes. (The events can turn the camera but not put it somewhere, and the third-person camera is always
 * pulled in by blocks; a side view needs to stand exactly where the level says.)
 */
@Mixin(Camera.class)
public abstract class GuhrioCameraMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot, float roll);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void guhs$guhrioZijaanzicht(float partialTicks, CallbackInfo ci) {
        BaanCamera.Stand stand = BaanCamera.stand(partialTicks);
        if (stand != null) {
            this.setRotation(stand.yaw(), stand.pitch(), 0f);
            this.setPosition(stand.x(), stand.y(), stand.z());
            this.detached = true;
        }
    }
}
