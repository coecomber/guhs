package nl.juiced.guhs.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.Guhs;

/**
 * The Guhmension itself is fully data-driven, see data/guhs/:
 * dimension/guhmension.json, dimension_type/guhmension.json, worldgen/noise_settings/guhmension.json,
 * worldgen/biome/guh_fields.json.
 */
public final class ModDimensions {
    public static final ResourceKey<Level> GUHMENSION = ResourceKey.create(Registries.DIMENSION, Guhs.id("guhmension"));

    private ModDimensions() {
    }
}
