package nl.juiced.guhs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import nl.juiced.guhs.feature.weer.client.WeerClient;

/**
 * 1.8.0: the Weer vadser's zones decide what falls on their columns (rain, snow in cold biomes, or nothing), also in the
 * Guhmension's biomes that never rain by themselves; outside the zones vanilla decides (feature.weer.client.WeerClient#neerslag).
 */
@Mixin(WeatherEffectRenderer.class)
public abstract class WeerNeerslagMixin {
    @ModifyReturnValue(method = "getPrecipitationAt", at = @At("RETURN"))
    private Biome.Precipitation guhs$weerVadserZones(Biome.Precipitation p, Level level, BlockPos pos) {
        Biome.Precipitation zone = WeerClient.neerslag(level, pos);
        return zone != null ? zone : p;
    }
}
