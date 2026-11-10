package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;

/**
 * Y-FTB: no FTB Quests reward is ever claimed inside the Vadsrijk (feature/vadsrijk/ftb/FtbClaimRegel). Every claim of FTB Quests
 * 26.1.2 (the claim button, "claim all", auto-claim, team rewards, change-progress) ends in {@code TeamData.claimReward(ServerPlayer,
 * Reward, boolean, long)}; refused at its head, nothing is marked claimed and nothing is given, so the reward waits until the player
 * is home. FTB Quests is optional: @Pseudo and require = 0, without FTB Quests this mixin does nothing. (The descriptor was checked
 * with javap against ftb-quests-neoforge 26.1.2.8.)
 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbquests.quest.TeamData", remap = false)
public abstract class FtbClaimMixin {
    @Inject(method = "claimReward(Lnet/minecraft/server/level/ServerPlayer;Ldev/ftb/mods/ftbquests/quest/reward/Reward;ZJ)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void guhs$nietInHetVadsrijk(ServerPlayer player, @Coerce Object reward, boolean notify, long date, CallbackInfo ci) {
        if (nl.juiced.guhs.feature.vadsrijk.ftb.FtbClaimRegel.weiger(player)) {
            ci.cancel();
        }
    }
}
