package nl.juiced.guhs.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.feature.GuhCrystalCaveFeature;
import nl.juiced.guhs.world.feature.GuhPoolFeature;

/** World generation features (placed by the JSON in data/guhs/worldgen). */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);

    public static final DeferredHolder<Feature<?>, GuhPoolFeature> GUH_POOL = FEATURES.register("guh_pool",
            () -> new GuhPoolFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, GuhCrystalCaveFeature> GUH_CRYSTAL_CAVE = FEATURES.register("guh_crystal_cave",
            () -> new GuhCrystalCaveFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }
}
