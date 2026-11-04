package nl.juiced.guhs.feature.vadsrijk.biomen;

import java.util.Arrays;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import nl.juiced.guhs.Guhs;

/**
 * The biome source {@code guhs:vadsrijk_gebieden} of the Vadsrijk (data/guhs/dimension/vadsrijk.json): the biome of a column is
 * {@code guhs:vadsrijk_<id>} with the id from {@link VadsrijkBiomen} (area boxes, no noise). {@code oud} is the old single biome
 * {@code guhs:vadsrijk}: it is never placed, but stays a possible biome so chunks of older worlds still get their map feature.
 */
public class VadsrijkBiomeSource extends BiomeSource {
    public static final MapCodec<VadsrijkBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryOps.retrieveGetter(Registries.BIOME),
            Biome.CODEC.fieldOf("oud").forGetter(s -> s.oud)
    ).apply(i, i.stable(VadsrijkBiomeSource::new)));

    private final Holder<Biome>[] perIndex;
    private final Holder<Biome> oud;

    @SuppressWarnings("unchecked")
    public VadsrijkBiomeSource(HolderGetter<Biome> biomen, Holder<Biome> oud) {
        this.oud = oud;
        this.perIndex = new Holder[VadsrijkBiomen.IDS.size()];
        for (int i = 0; i < perIndex.length; i++) {
            perIndex[i] = biomen.getOrThrow(ResourceKey.create(Registries.BIOME, Guhs.id("vadsrijk_" + VadsrijkBiomen.IDS.get(i))));
        }
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(Arrays.stream(perIndex), Stream.of(oud)).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        return perIndex[VadsrijkBiomen.index(QuartPos.toBlock(quartX) + 2, QuartPos.toBlock(quartZ) + 2)];
    }

    /** The biome at block x/z (for tests and tools). */
    public Holder<Biome> biomeOp(int x, int z) {
        return perIndex[VadsrijkBiomen.index(x, z)];
    }
}
