package nl.juiced.guhs.feature.guhpixel.reisbureau;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import nl.juiced.guhs.feature.guhpixel.Klok;

/**
 * The twenty destinations of Reisbureau "De Vadsvakantie" (five per duration: 1, 2, 8 and 24 real hours; biomes3 added the
 * last one of each duration: the three new biomes and "Japan, met Evivads en Nielsvads") and the
 * proefreisje of the questline (five minutes, a postcard, no souvenir). Ids, minutes and the chance on the rare souvenir
 * (in %) are the same as in tools/features/guhpixel_reisbureau_tekst.py (its self-check reads this file: keep one constant
 * per line). Texts: {@code gui.guhs.reisbureau.bestemming.<id>} (+ {@code .plek}, {@code .uitleg}), the postcard
 * {@code book.guhs.reisbureau.kaart.<id>}.
 */
public enum Bestemming {
    LINGSESDIJK("lingsesdijk", 60, 5),
    KAASMARKT("kaasmarkt", 60, 5),
    VADSWOUD("vadswoud", 60, 5),
    KNUFFELDAL("knuffeldal", 60, 5),
    BLOESEMMEERTJE("bloesemmeertje", 60, 5),   // biomes3
    GUHWAII("guhwaii", 120, 8),
    BARBECUETHER("barbecuether", 120, 8),
    EFTEGUH("efteguh", 120, 8),
    GUHKENHOF("guhkenhof", 120, 8),
    KLATERDAL("klaterdal", 120, 8),   // biomes3
    NOMGUH("nomguh", 480, 15),
    GUHRIJS("guhrijs", 480, 15),
    CAMPING("camping", 480, 15),
    GUHNETIE("guhnetie", 480, 15),
    WOLKENWEIDE("wolkenweide", 480, 15),   // biomes3
    KAASMAAN("kaasmaan", 1440, 30),
    WERELDREIS("wereldreis", 1440, 30),
    CRUISE("cruise", 1440, 30),
    BALKONIE("balkonie", 1440, 30),
    JAPAN("japan", 1440, 30),   // biomes3: the guh tags along with Evivads and Nielsvads
    OM_DE_HOEK("om_de_hoek", 5, 0);

    /** The four durations of the daily offer, in minutes. */
    public static final int[] DUREN = {60, 120, 480, 1440};
    /** The real destinations (not the proefreisje). */
    public static final List<Bestemming> ECHT;

    static {
        List<Bestemming> echt = new ArrayList<>();
        for (Bestemming b : values()) {
            if (!b.isProef()) {
                echt.add(b);
            }
        }
        ECHT = List.copyOf(echt);
    }

    private final String id;
    private final int minuten, kans;

    Bestemming(String id, int minuten, int kans) {
        this.id = id;
        this.minuten = minuten;
        this.kans = kans;
    }

    public String id() {
        return id;
    }

    public int minuten() {
        return minuten;
    }

    /** The chance on the rare souvenir instead of the common one, in %. */
    public int kans() {
        return kans;
    }

    public long duurMs() {
        return minuten * (Klok.UUR / 60);
    }

    public boolean isProef() {
        return this == OM_DE_HOEK;
    }

    public MutableComponent naam() {
        return Component.translatable("gui.guhs.reisbureau.bestemming." + id);
    }

    /** Fits after "Op vakantie in ...". */
    public MutableComponent plek() {
        return Component.translatable("gui.guhs.reisbureau.bestemming." + id + ".plek");
    }

    public MutableComponent uitleg() {
        return Component.translatable("gui.guhs.reisbureau.bestemming." + id + ".uitleg");
    }

    /** The common souvenir (null for the proefreisje). */
    @Nullable
    public Block souvenir() {
        return isProef() ? null : ReisbureauSlice.blok("souvenir_" + id);
    }

    /** The rare souvenir (null for the proefreisje). */
    @Nullable
    public Block zeldzaam() {
        return isProef() ? null : ReisbureauSlice.blok("zeldzaam_" + id);
    }

    /** This destination's ansichtkaart. */
    public Item kaart() {
        return ReisbureauSlice.KAARTEN.get(this).get();
    }

    @Nullable
    public static Bestemming vanId(String id) {
        for (Bestemming b : values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }

    /** The destinations of one duration, in table order. */
    public static List<Bestemming> metDuur(int minuten) {
        List<Bestemming> out = new ArrayList<>();
        for (Bestemming b : ECHT) {
            if (b.minuten == minuten) {
                out.add(b);
            }
        }
        return out;
    }
}
