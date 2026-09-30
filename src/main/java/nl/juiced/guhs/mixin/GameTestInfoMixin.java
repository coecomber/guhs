package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;

/**
 * 1.1.0 (MC 26.1): keeps the 1.0.0 gametest coordinates. In 1.21.1 a test's template was placed one block above its
 * structure block and {@code GameTestHelper#absolutePos} counted from the structure block, so relative y 1 was the
 * template's bottom layer. In 26.1 the helper counts from the template's corner itself. All ~750 Guhs tests were written
 * for the old origin, so for guhs tests the origin is moved one block down again ({@code getTestOrigin} is only used
 * by {@code GameTestHelper}'s absolute/relative conversions).
 */
@Mixin(GameTestInfo.class)
public abstract class GameTestInfoMixin {
    @Shadow
    public abstract Identifier id();

    @ModifyReturnValue(method = "getTestOrigin", at = @At("RETURN"))
    private BlockPos guhs$oudeTestOorsprong(BlockPos origin) {
        return Guhs.MODID.equals(this.id().getNamespace()) ? origin.below() : origin;
    }
}
