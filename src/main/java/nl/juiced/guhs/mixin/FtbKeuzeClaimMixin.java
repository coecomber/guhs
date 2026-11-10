package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;

/**
 * Y-FTB: the one FTB Quests claim that does not pass {@code TeamData.claimReward} (see {@link FtbClaimMixin}): picking a choice
 * reward. ClaimChoiceRewardMessage.handle runs the claim in a lambda (ChoiceReward, picked Reward, message, ServerPlayer, TeamData)
 * that marks it claimed and gives it; refused at its head, the choice stays open. Optional like FtbClaimMixin (@Pseudo, require = 0;
 * the lambda name was checked with javap against ftb-quests-neoforge 26.1.2.8).
 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbquests.net.ClaimChoiceRewardMessage", remap = false)
public abstract class FtbKeuzeClaimMixin {
    @Inject(method = "lambda$handle$1(Ldev/ftb/mods/ftbquests/quest/reward/ChoiceReward;Ldev/ftb/mods/ftbquests/quest/reward/Reward;"
            + "Ldev/ftb/mods/ftbquests/net/ClaimChoiceRewardMessage;Lnet/minecraft/server/level/ServerPlayer;Ldev/ftb/mods/ftbquests/quest/TeamData;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void guhs$nietInHetVadsrijk(@Coerce Object keuze, @Coerce Object beloning, @Coerce Object bericht, ServerPlayer player,
            @Coerce Object team, CallbackInfo ci) {
        if (nl.juiced.guhs.feature.vadsrijk.ftb.FtbClaimRegel.weiger(player)) {
            ci.cancel();
        }
    }
}
