package nl.juiced.guhs.feature.einde2.biomes;

import java.util.List;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * einde2, slice biomes: the biome source {@code guhs:guheinde_biomes} of the Guheinde (data/guhs/dimension/guheinde.json).
 * <p>
 * Within {@code straal_midden} blocks of 0,0 everything is {@code midden}: the main island and the ring of nothing around
 * it stay guhs:guheinde (the cut lies in the void, where nobody sees it). Beyond it two slow noises decide, the
 * temperature and the vegetation of the dimension's noise router (guhs:guheinde_streek and guhs:guheinde_zeldzaam): the
 * sign of the first picks one of the two common biomes, and where the second is far from zero ({@code drempel}) a rare
 * biome lies inside that common one, picked by the two signs. The noises are read at y 0: a column is one biome from the
 * void to the sky, so whatever floats high above an island has that island's biome.
 */
public class GuheindeBiomeSource extends BiomeSource {
    public static final MapCodec<GuheindeBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.fieldOf("midden").forGetter(s -> s.midden),
            Codec.INT.fieldOf("straal_midden").forGetter(s -> s.straalMidden),
            Biome.CODEC.listOf().fieldOf("gewoon").forGetter(s -> s.gewoon),
            Biome.CODEC.listOf().fieldOf("zeldzaam").forGetter(s -> s.zeldzaam),
            Codec.DOUBLE.fieldOf("drempel").forGetter(s -> s.drempel)
    ).apply(i, GuheindeBiomeSource::new));

    private final Holder<Biome> midden;
    private final int straalMidden;
    private final List<Holder<Biome>> gewoon;
    private final List<Holder<Biome>> zeldzaam;
    private final double drempel;

    public GuheindeBiomeSource(Holder<Biome> midden, int straalMidden, List<Holder<Biome>> gewoon, List<Holder<Biome>> zeldzaam, double drempel) {
        if (gewoon.size() != 2 || zeldzaam.size() != 4) {
            throw new IllegalArgumentException("guhs:guheinde_biomes takes two common and four rare biomes");
        }
        this.midden = midden;
        this.straalMidden = straalMidden;
        this.gewoon = List.copyOf(gewoon);
        this.zeldzaam = List.copyOf(zeldzaam);
        this.drempel = drempel;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(Stream.of(this.midden), Stream.concat(this.gewoon.stream(), this.zeldzaam.stream()));
    }

    /**
     * Which of the seven (0 = midden, 1-2 = gewoon, 3-6 = zeldzaam) for a spot this far from 0,0 with these two noise
     * values: the whole rule in one pure function.
     */
    public static int kies(long afstandKwadraat, int straalMidden, double streek, double zeldzaam, double drempel) {
        if (afstandKwadraat < (long) straalMidden * straalMidden) {
            return 0;
        }
        if (Math.abs(zeldzaam) > drempel) {
            return zeldzaam > 0 ? (streek > 0 ? 3 : 4) : (streek > 0 ? 5 : 6);
        }
        return streek > 0 ? 1 : 2;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int x = QuartPos.toBlock(quartX), z = QuartPos.toBlock(quartZ);
        long afstand = (long) x * x + (long) z * z;
        if (afstand < (long) this.straalMidden * this.straalMidden) {
            return this.midden;
        }
        DensityFunction.SinglePointContext plek = new DensityFunction.SinglePointContext(x, 0, z);
        int k = kies(afstand, this.straalMidden, sampler.temperature().compute(plek), sampler.humidity().compute(plek), this.drempel);
        return k == 0 ? this.midden : k <= 2 ? this.gewoon.get(k - 1) : this.zeldzaam.get(k - 3);
    }
}
