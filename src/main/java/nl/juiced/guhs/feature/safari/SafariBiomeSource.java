package nl.juiced.guhs.feature.safari;

import java.util.Map;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * The biome source {@code guhs:safari_zones} of the safari dimension (data/guhs/dimension/safaripark.json): the biome of a
 * column is the one of its park zone ({@code zones}: zone name -&gt; biome) from {@link ParkData#biomeZone}; the decor ring takes
 * the biome of the nearest park zone, so grass and foliage colours run on to the horizon. A zone without its own entry in
 * the map gets {@code decor}.
 */
public class SafariBiomeSource extends BiomeSource {
    public static final MapCodec<SafariBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Biome.CODEC).fieldOf("zones").forGetter(s -> s.zones),
            Biome.CODEC.fieldOf("decor").forGetter(s -> s.decor)
    ).apply(i, SafariBiomeSource::new));

    private final Map<String, Holder<Biome>> zones;
    private final Holder<Biome> decor;
    private Holder<Biome>[] perIndex;

    public SafariBiomeSource(Map<String, Holder<Biome>> zones, Holder<Biome> decor) {
        this.zones = Map.copyOf(zones);
        this.decor = decor;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(zones.values().stream(), Stream.of(decor)).distinct();
    }

    @SuppressWarnings("unchecked")
    private Holder<Biome>[] perIndex() {
        if (perIndex == null) {
            ParkData d = ParkData.get();
            Holder<Biome>[] a = new Holder[d.zones.size()];
            for (int i = 0; i < a.length; i++) {
                a[i] = zones.getOrDefault(d.zones.get(i), decor);
            }
            perIndex = a;
        }
        return perIndex;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int zone = ParkData.get().biomeZone(QuartPos.toBlock(quartX), QuartPos.toBlock(quartZ));
        Holder<Biome>[] a = perIndex();
        return zone < 0 || zone >= a.length ? decor : a[zone];
    }
}
