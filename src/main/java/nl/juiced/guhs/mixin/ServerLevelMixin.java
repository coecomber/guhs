package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import nl.juiced.guhs.feature.guhpolder.GuhpolderWeer;
import nl.juiced.guhs.feature.huisje.Binnen;

/** 2.10.1: no snow piling up and no ice forming from the weather in the Guhpolder (see {@link GuhpolderWeer}). */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tickPrecipitation", at = @At("HEAD"), cancellable = true)
    private void guhs$geenPoldersneeuw(BlockPos pos, CallbackInfo ci) {
        if (GuhpolderWeer.geenWeer((ServerLevel) (Object) this, pos)) {
            ci.cancel();
        }
    }

    /**
     * 1.3.2: the rooms inside the Guhhuisjes never skip a night by themselves: who sleeps in a logeerbedje there is counted
     * with the players of the world the huisje stands in (feature/huisje/BinnenSlaap). Without a sleeping list this
     * dimension has no sleepers, so vanilla neither wakes them nor announces anything.
     */
    @Inject(method = "updateSleepingPlayerList", at = @At("HEAD"), cancellable = true)
    private void guhs$huisjeBinnenSlaap(CallbackInfo ci) {
        if (((ServerLevel) (Object) this).dimension() == Binnen.DIM) {
            ci.cancel();
        }
    }
}
