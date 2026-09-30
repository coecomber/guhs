package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import nl.juiced.guhs.feature.guhpolder.GuhpolderWeer;

/** 2.10.1: no snow piling up and no ice forming from the weather in the Guhpolder (see {@link GuhpolderWeer}). */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tickPrecipitation", at = @At("HEAD"), cancellable = true)
    private void guhs$geenPoldersneeuw(BlockPos pos, CallbackInfo ci) {
        if (GuhpolderWeer.geenWeer((ServerLevel) (Object) this, pos)) {
            ci.cancel();
        }
    }
}
