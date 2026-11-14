package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * 1.10.3 (live bug "water waar het niet hoort" in De Kristalgrot): plain ice melts into water at block light above 11, and the
 * hand-built Vadsrijk map has ice next to lanterns (the Kristalgrot's lake rim and icicles under sea lanterns). In Het Vadsrijk
 * ice never melts: the map stays as it was built.
 */
@Mixin(IceBlock.class)
public abstract class VadsrijkIjsMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void guhs$nietSmeltenInVadsrijk(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (Vadsrijk.in(level)) {
            ci.cancel();
        }
    }
}
