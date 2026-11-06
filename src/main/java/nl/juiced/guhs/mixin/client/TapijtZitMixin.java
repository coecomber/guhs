package nl.juiced.guhs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import nl.juiced.guhs.feature.vadsrijk.tapijt.client.TapijtClient;

/**
 * U-TAPIJT (1.10.0, Het Vadsrijk): a rider on the flying carpet sits cross-legged instead of with the legs stuck out forward. After
 * the game posed the player (riding pose), the legs are folded in front: both point forward and swing in toward each other so the
 * feet cross. Only when the render state carries {@link TapijtClient#ZIT} (set by the carpet's render-state modifier).
 */
@Mixin(PlayerModel.class)
public abstract class TapijtZitMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void guhs$kleermakerszit(AvatarRenderState state, CallbackInfo ci) {
        if (state.getRenderData(TapijtClient.ZIT) == null) {
            return;
        }
        HumanoidModel<?> m = (HumanoidModel<?>) (Object) this;
        m.rightLeg.xRot = -1.50f;
        m.rightLeg.yRot = -0.85f;
        m.rightLeg.zRot = 0.10f;
        m.leftLeg.xRot = -1.42f;
        m.leftLeg.yRot = 0.85f;
        m.leftLeg.zRot = -0.10f;
    }
}
