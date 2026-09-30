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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import nl.juiced.guhs.feature.Protected;

/** Water and lava from outside don't flow into a protected guh building (see {@link Protected}). */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @Inject(method = "canSpreadTo", at = @At("HEAD"), cancellable = true)
    private void guhs$keepOutOfBuildings(BlockGetter level, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos,
            BlockState toBlockState, FluidState toFluidState, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof Level world && !world.isClientSide && Protected.keepsFluidOut(world, fromPos, toPos)) {
            cir.setReturnValue(false);
        }
    }
}
