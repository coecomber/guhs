package nl.juiced.guhs.feature.onderwol;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density function {@code guhs:grotten}: passes its argument on untouched. It only marks the caves' term in the
 * Guhmension's final density, so a quick height probe ({@link nl.juiced.guhs.world.ZoekHoogte}) can leave the caves out.
 */
public record GrottenFunctie(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<GrottenFunctie> DATA_CODEC = DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").xmap(GrottenFunctie::new,
            GrottenFunctie::argument);
    public static final KeyDispatchDataCodec<GrottenFunctie> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return argument.compute(context);
    }

    @Override
    public void fillArray(double[] output, ContextProvider contextProvider) {
        argument.fillArray(output, contextProvider);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new GrottenFunctie(argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return argument.minValue();
    }

    @Override
    public double maxValue() {
        return argument.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
