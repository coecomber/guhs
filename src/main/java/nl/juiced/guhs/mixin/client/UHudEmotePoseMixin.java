package nl.juiced.guhs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import nl.juiced.guhs.feature.vadsrijk.hud.client.EmotePose;

/**
 * U-HUD (emotes): after the humanoid pose is set up, a player that plays an emote gets its arm/head/leg rotations
 * ({@link EmotePose#pas}). HumanoidModel (not PlayerModel) so the armour models on a player follow the same pose. Only
 * players with an emote are touched (their render state carries {@link EmotePose#STAAT}).
 */
@Mixin(HumanoidModel.class)
public abstract class UHudEmotePoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void guhs$emotePose(HumanoidRenderState state, CallbackInfo ci) {
        if (state instanceof AvatarRenderState a) {
            EmotePose.pas((HumanoidModel<?>) (Object) this, a);
        }
    }
}
