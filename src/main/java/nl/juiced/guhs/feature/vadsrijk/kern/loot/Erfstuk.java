package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * T-ITEMS (itemisatie 2.0): the "grows with you" part of one piece of gear, kept inside {@link Uitrusting} (field {@code erf},
 * left out of the saved data while it is {@link #LEEG}, so old items stay byte-identical):
 * <ul>
 *   <li>{@code xp}: "Vertrouwd" item xp (worn gear gets it from kills; the level 0-5 is derived, see feature/vadsrijk/items/Erfstukken);</li>
 *   <li>{@code groei}: how often it "doorgroeide" (ascension; Legendarisch max 2, Guhddelijk unlimited);</li>
 *   <li>{@code pity}: failed star attempts in a row ("Geduld van de smid", only below star 7).</li>
 * </ul>
 * The rules live in feature/vadsrijk/items; this record is only the data.
 */
public record Erfstuk(int xp, int groei, int pity) {
    public static final Erfstuk LEEG = new Erfstuk(0, 0, 0);
    public static final Codec<Erfstuk> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("xp", 0).forGetter(Erfstuk::xp),
            Codec.INT.optionalFieldOf("groei", 0).forGetter(Erfstuk::groei),
            Codec.INT.optionalFieldOf("pity", 0).forGetter(Erfstuk::pity)).apply(i, Erfstuk::new));

    public Erfstuk metXp(int x) {
        return new Erfstuk(x, groei, pity);
    }

    public Erfstuk metGroei(int g) {
        return new Erfstuk(xp, g, pity);
    }

    public Erfstuk metPity(int p) {
        return new Erfstuk(xp, groei, p);
    }
}
