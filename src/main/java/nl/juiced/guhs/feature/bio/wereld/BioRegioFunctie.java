package nl.juiced.guhs.feature.bio.wereld;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jspecify.annotations.Nullable;

/**
 * Density function {@code guhs:bio_regio}: 1 where the terrain model says a column belongs to one of our biomes
 * ({@code soort}: "klaterdal", "bloesemmeertje" or "wolkenweide"), from y {@link BioModel#BIOME_ONDER} up; 0 elsewhere.
 * tools/features/bio_wereld.py adds it, times a large shift, to the multi-noise temperature, humidity and erosion of the
 * Guhmensie, which moves the climate to the biome's own entry. A hard step, so biome and terrain agree exactly and no
 * other biome can appear in a ring around ours.
 */
public record BioRegioFunctie(String soort, List<DensityFunction.NoiseHolder> ruis, byte code, @Nullable BioModel model) implements DensityFunction {
    public static final MapCodec<BioRegioFunctie> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("soort").forGetter(BioRegioFunctie::soort),
            DensityFunction.NoiseHolder.CODEC.listOf().fieldOf("ruis").forGetter(BioRegioFunctie::ruis)
    ).apply(i, (soort, ruis) -> new BioRegioFunctie(soort, ruis, code(soort), null)));
    public static final KeyDispatchDataCodec<BioRegioFunctie> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    static byte code(String soort) {
        return switch (soort) {
            case "klaterdal" -> Kaart.DAL;
            case "bloesemmeertje" -> Kaart.MEER;
            case "wolkenweide" -> Kaart.WEIDE;
            default -> throw new IllegalArgumentException("guhs:bio_regio: unknown soort " + soort);
        };
    }

    @Override
    public double compute(FunctionContext c) {
        BioModel m = model;
        if (m == null || c.blockY() < BioModel.BIOME_ONDER) {
            return 0.0;
        }
        int x = c.blockX(), z = c.blockZ();
        Kaart k = m.kaart(x >> 4, z >> 4);
        return !k.leeg && k.soort[(x & 15) | (z & 15) << 4] == code ? 1.0 : 0.0;
    }

    @Override
    public void fillArray(double[] output, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(output, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        BioTerreinFunctie.Bedraad b = BioTerreinFunctie.bedraad(ruis, model, visitor);
        return visitor.apply(new BioRegioFunctie(soort, b.ruis(), code, b.model()));
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
