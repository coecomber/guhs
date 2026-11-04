package nl.juiced.guhs.feature.vadsrijk.biomen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * Fix round 2 "biomen": one biome per Vadsrijk area. Registers the chunk generator {@code guhs:vadsrijk_kaart}
 * ({@link VadsrijkGenerator}) and the biome source {@code guhs:vadsrijk_gebieden} ({@link VadsrijkBiomeSource}); the biomes
 * themselves are data (tools/features/vadsrijk_biomen.py).
 */
public final class BiomenRegistry {
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister.create(Registries.BIOME_SOURCE, Guhs.MODID);
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, Guhs.MODID);

    static {
        BIOME_SOURCES.register("vadsrijk_gebieden", () -> VadsrijkBiomeSource.CODEC);
        GENERATORS.register("vadsrijk_kaart", () -> VadsrijkGenerator.CODEC);
    }

    private BiomenRegistry() {
    }

    public static void register(IEventBus modBus) {
        BIOME_SOURCES.register(modBus);
        GENERATORS.register(modBus);
    }
}
