package nl.juiced.guhs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import nl.juiced.guhs.feature.guhpolder.GuhpolderWeer;

/**
 * 2.10.1: the vanilla snow curtain is not drawn over the Guhpolder; the sparse guh-sneeuw particles
 * (feature.guhpolder.client.GuhSneeuw) fall there instead. Rain anywhere and snow in every other biome stay vanilla.
 * (require = 0: if another mod rewrote the weather rendering, you'd just see both kinds of snow.)
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @WrapOperation(method = "renderSnowAndRain", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation guhs$geenVanillaSneeuwInDePolder(Biome biome, BlockPos pos, Operation<Biome.Precipitation> original) {
        Biome.Precipitation p = original.call(biome, pos);
        var level = Minecraft.getInstance().level;
        if (p == Biome.Precipitation.SNOW && level != null && GuhpolderWeer.isGuhpolder(level, biome)) {
            return Biome.Precipitation.NONE;
        }
        return p;
    }
}
