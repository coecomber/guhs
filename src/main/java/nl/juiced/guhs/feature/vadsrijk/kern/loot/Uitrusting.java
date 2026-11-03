package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;

/**
 * The rolls of one piece of gear: the data component {@code guhs:vr_uitrusting}. Immutable: change with the {@code met*} methods
 * and set it back on the stack. Economie hooks: {@link #metSterren}, {@link #metVerpluisd}, {@link #metBonussen}, {@link #gebonden}.
 */
public record Uitrusting(String type, Zeldzaamheid zeldzaamheid, int level, List<Bonus> bonussen, Element element, int flavour, int sterren,
        boolean verpluisd, boolean gebonden, long seed) {
    public static final Codec<Uitrusting> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("type").forGetter(Uitrusting::type),
            Codec.STRING.xmap(Zeldzaamheid::van, z -> z.name().toLowerCase(Locale.ROOT)).fieldOf("zeldzaamheid").forGetter(Uitrusting::zeldzaamheid),
            Codec.INT.fieldOf("level").forGetter(Uitrusting::level),
            Bonus.CODEC.listOf().fieldOf("bonussen").forGetter(Uitrusting::bonussen),
            Codec.STRING.xmap(Element::van, e -> e.name().toLowerCase(Locale.ROOT)).optionalFieldOf("element", Element.GEEN).forGetter(Uitrusting::element),
            Codec.INT.optionalFieldOf("flavour", 0).forGetter(Uitrusting::flavour),
            Codec.INT.optionalFieldOf("sterren", 0).forGetter(Uitrusting::sterren),
            Codec.BOOL.optionalFieldOf("verpluisd", false).forGetter(Uitrusting::verpluisd),
            Codec.BOOL.optionalFieldOf("gebonden", false).forGetter(Uitrusting::gebonden),
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(Uitrusting::seed)).apply(i, Uitrusting::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Uitrusting> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public Uitrusting {
        bonussen = List.copyOf(bonussen);
    }

    public Uitrusting metSterren(int s) {
        return new Uitrusting(type, zeldzaamheid, level, bonussen, element, flavour, s, verpluisd, gebonden, seed);
    }

    public Uitrusting metVerpluisd(boolean v) {
        return new Uitrusting(type, zeldzaamheid, level, bonussen, element, flavour, sterren, v, gebonden, seed);
    }

    public Uitrusting metBonussen(List<Bonus> b) {
        return new Uitrusting(type, zeldzaamheid, level, b, element, flavour, sterren, verpluisd, gebonden, seed);
    }

    public Uitrusting metGebonden(boolean g) {
        return new Uitrusting(type, zeldzaamheid, level, bonussen, element, flavour, sterren, verpluisd, g, seed);
    }

    @Nullable
    public BasisType basisType() {
        return BasisTypes.van(type);
    }
}
