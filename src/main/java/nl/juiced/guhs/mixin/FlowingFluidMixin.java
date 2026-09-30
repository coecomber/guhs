package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import nl.juiced.guhs.feature.Protected;

/** Water and lava from outside don't flow into a protected guh building (see {@link Protected}). */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    // 26.1: canSpreadTo is gone; canMaybePassThrough is the shared check of downward spread, sideways spread and slope search
    @Inject(method = "canMaybePassThrough", at = @At("HEAD"), cancellable = true)
    private void guhs$keepOutOfBuildings(BlockGetter level, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos,
            BlockState toBlockState, FluidState toFluidState, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof Level world && !world.isClientSide() && Protected.keepsFluidOut(world, fromPos, toPos)) {
            cir.setReturnValue(false);
        }
    }
}
