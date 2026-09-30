package nl.juiced.guhs.feature.guhpolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 2.10.1: the weather in the Guhpolder. It may snow there, but the snow does NOT pile up and water does NOT freeze from the
 * weather: the polder keeps exactly the snow and ice that its worldgen and buildings put there themselves (the
 * Elf-Guhjestocht's canal is packed ice/ice from the template, which weather never touched anyway). Vanilla elsewhere is
 * untouched: only the guhs:guhpolder biome says no ({@code mixin.ServerLevelMixin} on ServerLevel.tickPrecipitation).
 * <p>
 * On the client the vanilla snow curtain is left out there too ({@code mixin.client.LevelRendererMixin}); the soft,
 * sparse guh-sneeuw particles take its place ({@code feature.guhpolder.client.GuhSneeuw}).
 */
public final class GuhpolderWeer {
    /** 3.0: the biomes the weather leaves alone (biome tag guhs:geen_weer; the Guhpolder is in it, the Sneeuwguhtoendra not). */
    public static final net.minecraft.tags.TagKey<Biome> GEEN_WEER = net.minecraft.tags.TagKey.create(Registries.BIOME, nl.juiced.guhs.Guhs.id("geen_weer"));

    /** Does the weather leave this spot alone (no snow layers, no freezing)? True in the biomes of {@link #GEEN_WEER} (the Guhpolder). */
    public static boolean geenWeer(Level level, BlockPos pos) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
        return level.getBiome(top).is(GEEN_WEER);
    }

    /** Is this biome one the weather leaves alone (the Guhpolder...; by its registry holder, so it works for any Biome instance)? */
    public static boolean isGuhpolder(Level level, Biome biome) {
        var registry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        return registry.getResourceKey(biome).flatMap(registry::getHolder).map(h -> h.is(GEEN_WEER)).orElse(false);
    }

    private GuhpolderWeer() {
    }
}
