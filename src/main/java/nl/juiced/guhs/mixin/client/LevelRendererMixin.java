package nl.juiced.guhs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import nl.juiced.guhs.feature.guhpolder.GuhpolderWeer;

/**
 * 2.10.1: the vanilla snow curtain is not drawn over the Guhpolder; the sparse guh-sneeuw particles
 * (feature.guhpolder.client.GuhSneeuw) fall there instead. Rain anywhere and snow in every other biome stay vanilla.
 * <p>
 * 1.1.0 (MC 26.1): {@code LevelRenderer#renderSnowAndRain} is gone; the weather columns are now collected in
 * {@link WeatherEffectRenderer#extractRenderState}, which asks the private {@code getPrecipitationAt(Level, BlockPos)} per
 * column (as does {@code tickRainParticles}, which only cares about RAIN). Turning SNOW into NONE there for polder biomes
 * gives exactly the old behaviour. (The class keeps its old name so guhs.mixins.json does not change.)
 */
@Mixin(WeatherEffectRenderer.class)
public abstract class LevelRendererMixin {
    @ModifyReturnValue(method = "getPrecipitationAt", at = @At("RETURN"))
    private Biome.Precipitation guhs$geenVanillaSneeuwInDePolder(Biome.Precipitation p, Level level, BlockPos pos) {
        if (p == Biome.Precipitation.SNOW && GuhpolderWeer.isGuhpolder(level, level.getBiome(pos).value())) {
            return Biome.Precipitation.NONE;
        }
        return p;
    }
}
