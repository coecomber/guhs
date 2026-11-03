package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import nl.juiced.guhs.feature.vadsrijk.kern.Stat;

/** One rolled bonus on an item: a stat and its value (percent stats in percent points). */
public record Bonus(Stat stat, double waarde) {
    public static final Codec<Bonus> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(Stat::van, s -> s.name().toLowerCase(Locale.ROOT)).fieldOf("stat").forGetter(Bonus::stat),
            Codec.DOUBLE.fieldOf("waarde").forGetter(Bonus::waarde)).apply(i, Bonus::new));
}
