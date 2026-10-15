package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jspecify.annotations.Nullable;

/**
 * Density function {@code guhs:bio_terrein}: the Guhmensie's final density with the terrain of the three biomes laid over
 * it. {@code input} is the final density as it was; {@code ruis} the noises of {@link BioModel#RUIS}, in that order.
 * <p>
 * Outside our regions (and below y {@link BioModel#ONDER}) it returns the input untouched. Inside, per block: solid up to
 * the model's top block of the column and in its floating islands, blended with the input across the rim. It stands
 * OUTSIDE the {@code interpolated} wrapper on purpose: it is evaluated per block, so rock faces and river banks are
 * exact (the interpolation would smear a cliff over a cell of 8 blocks). The model caches per chunk, so that costs a
 * lookup per block.
 */
public record BioTerreinFunctie(DensityFunction input, List<DensityFunction.NoiseHolder> ruis, @Nullable BioModel model) implements DensityFunction {
    public static final MapCodec<BioTerreinFunctie> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(BioTerreinFunctie::input),
            DensityFunction.NoiseHolder.CODEC.listOf().fieldOf("ruis").forGetter(BioTerreinFunctie::ruis)
    ).apply(i, (input, ruis) -> new BioTerreinFunctie(input, ruis, null)));
    public static final KeyDispatchDataCodec<BioTerreinFunctie> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);
    /** Density per block of height, as the Guhmensie's own beds use. */
    private static final double PER_BLOK = 0.025;

    @Override
    public double compute(FunctionContext c) {
        BioModel m = model;
        if (m == null) {
            return input.compute(c);
        }
        int x = c.blockX(), z = c.blockZ();
        Kaart k = m.kaart(x >> 4, z >> 4);
        if (k.leeg) {
            return input.compute(c);
        }
        int i = (x & 15) | (z & 15) << 4;
        float b = k.meng[i];
        int y = c.blockY();
        if (b <= 0f || y < BioModel.ONDER) {
            return input.compute(c);
        }
        if (k.inSpan(i, y)) {
            return 1.0;
        }
        double mijn = (k.hoogte[i] + 0.5 - y) * PER_BLOK;
        mijn = mijn > 1 ? 1 : mijn < -1 ? -1 : mijn;
        if (b >= 1f) {
            return mijn;
        }
        return input.compute(c) * (1 - b) + mijn * b;
    }

    @Override
    public void fillArray(double[] output, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(output, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        Bedraad b = bedraad(ruis, model, visitor);
        return visitor.apply(new BioTerreinFunctie(input.mapAll(visitor), b.ruis, b.model));
    }

    record Bedraad(List<NoiseHolder> ruis, @Nullable BioModel model) {
    }

    /** Lets the visitor wire the noises; the model follows them (the same model as long as the noises are the same). */
    static Bedraad bedraad(List<NoiseHolder> ruis, @Nullable BioModel model, Visitor visitor) {
        List<NoiseHolder> nieuw = new ArrayList<>(ruis.size());
        boolean zelfde = true;
        for (NoiseHolder h : ruis) {
            NoiseHolder n = visitor.visitNoise(h);
            zelfde &= n == h || n.noise() == h.noise();
            nieuw.add(n);
        }
        return new Bedraad(List.copyOf(nieuw), zelfde && model != null ? model : BioModel.van(nieuw));
    }

    @Override
    public double minValue() {
        return Math.min(input.minValue(), -1.0);
    }

    @Override
    public double maxValue() {
        return Math.max(input.maxValue(), 1.0);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
