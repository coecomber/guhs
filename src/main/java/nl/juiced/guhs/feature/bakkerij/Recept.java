package nl.juiced.guhs.feature.bakkerij;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;

/**
 * The recipes of the Knabbelbakkerij: every pastry is one dough ({@link Deeg}), one shape ({@link Vorm}) and one topping
 * ({@link Topping}). The twelve of the receptenboek (Knus tab, collection {@code receptenboek}) and Bakker Korstje's
 * secret one, the {@link #FEESTTAART} (only baked in his game, for the Grote Knusfeest).
 * <p>
 * The same choice screen is used in Korstje's order game and at every knabbeloven ({@link Bakken}).
 */
public enum Recept {
    KNABBELBROODJE(Deeg.KNABBELDEEG, Vorm.BOLLETJE, Topping.KRUIMELS, 56),
    KAASKRAKELING(Deeg.KAASDEEG, Vorm.VLECHTJE, Topping.SUIKER, 60),
    VADSVLAAI(Deeg.ZOETDEEG, Vorm.TAARTJE, Topping.KAAS, 66),
    GUHCROISSANT(Deeg.BLADERDEEG, Vorm.MAANTJE, Topping.KAAS, 52),
    KNABBELKOEKJE(Deeg.KNABBELDEEG, Vorm.PLAATJE, Topping.SUIKER, 44),
    KAASBOLLETJE(Deeg.KAASDEEG, Vorm.BOLLETJE, Topping.KAAS, 50),
    PLUISMUFFIN(Deeg.ZOETDEEG, Vorm.BOLLETJE, Topping.GLAZUUR, 54),
    THEETAARTJE(Deeg.BLADERDEEG, Vorm.TAARTJE, Topping.SUIKER, 62),
    KNABBELTOMPOUCE(Deeg.BLADERDEEG, Vorm.PLAATJE, Topping.GLAZUUR, 58),
    VADSDONUT(Deeg.ZOETDEEG, Vorm.RINGETJE, Topping.GLAZUUR, 48),
    GUHWAFEL(Deeg.ZOETDEEG, Vorm.PLAATJE, Topping.SUIKER, 46),
    STERRENKOEKJE(Deeg.KNABBELDEEG, Vorm.STERRETJE, Topping.GLAZUUR, 42),
    /** Bakker Korstje's special order for the Grote Knusfeest (not in the receptenboek, never from a knabbeloven). */
    FEESTTAART(Deeg.ZOETDEEG, Vorm.TAARTJE, Topping.GLAZUUR, 70),
    /** 2.8.1 Piep: the roze guh koek (not in the receptenboek; only for players who learned it, feature.piep.ReceptItem). */
    ROZE_GUH_KOEK(Deeg.ZOETDEEG, Vorm.PLAATJE, Topping.GLAZUUR, 64);

    /** The twelve of the receptenboek, in book order. */
    public static final List<Recept> BOEK = List.of(KNABBELBROODJE, KAASKRAKELING, VADSVLAAI, GUHCROISSANT, KNABBELKOEKJE, KAASBOLLETJE,
            PLUISMUFFIN, THEETAARTJE, KNABBELTOMPOUCE, VADSDONUT, GUHWAFEL, STERRENKOEKJE);

    public final Deeg deeg;
    public final Vorm vorm;
    public final Topping topping;
    /** How many ticks the oven's pointer needs to run from cold to burnt (bigger bakes are slower, so a bit easier). */
    public final int bakTicks;

    Recept(Deeg deeg, Vorm vorm, Topping topping, int bakTicks) {
        this.deeg = deeg;
        this.vorm = vorm;
        this.topping = topping;
        this.bakTicks = bakTicks;
    }

    /** The item id (lower case): also the receptenboek entry. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component naam() {
        return Component.translatable("item.guhs." + id());
    }

    public boolean inBoek() {
        return this != FEESTTAART && this != ROZE_GUH_KOEK;
    }

    /** The recipe of this combination, or null ("dat is geen recept... njeg"). */
    @Nullable
    public static Recept van(Deeg deeg, Vorm vorm, Topping topping) {
        for (Recept r : values()) {
            if (r.deeg == deeg && r.vorm == vorm && r.topping == topping) {
                return r;
            }
        }
        return null;
    }

    @Nullable
    public static Recept byId(String id) {
        for (Recept r : values()) {
            if (r.id().equals(id)) {
                return r;
            }
        }
        return null;
    }

    @Nullable
    public static Recept byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : null;
    }

    /** The three choices of the baking screen. The lang keys are gui.guhs.bakkerij.&lt;kind&gt;.&lt;id&gt;. */
    public enum Deeg {
        KNABBELDEEG, KAASDEEG, ZOETDEEG, BLADERDEEG;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component naam() {
            return Component.translatable("gui.guhs.bakkerij.deeg." + id());
        }

        @Nullable
        public static Deeg byIndex(int i) {
            return i >= 0 && i < values().length ? values()[i] : null;
        }
    }

    public enum Vorm {
        BOLLETJE, VLECHTJE, TAARTJE, MAANTJE, PLAATJE, RINGETJE, STERRETJE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component naam() {
            return Component.translatable("gui.guhs.bakkerij.vorm." + id());
        }

        @Nullable
        public static Vorm byIndex(int i) {
            return i >= 0 && i < values().length ? values()[i] : null;
        }
    }

    public enum Topping {
        KAAS, SUIKER, GLAZUUR, KRUIMELS;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component naam() {
            return Component.translatable("gui.guhs.bakkerij.topping." + id());
        }

        @Nullable
        public static Topping byIndex(int i) {
            return i >= 0 && i < values().length ? values()[i] : null;
        }
    }

    /**
     * How well it came out of the oven, by where the pointer was (0..100 over {@link #bakTicks}): too early it's raw,
     * a golden window is perfect, too late it's burnt.
     */
    public enum Kwaliteit {
        RAUW, GOED, PERFECT, AANGEBRAND;

        public static final int GOED_VAN = 55, PERFECT_VAN = 70, PERFECT_TOT = 82, GOED_TOT = 90;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component naam() {
            return Component.translatable("gui.guhs.bakkerij.kwaliteit." + id());
        }

        /** Quality at pointer position p (0..100+). */
        public static Kwaliteit bij(int p) {
            if (p < GOED_VAN) {
                return RAUW;
            } else if (p < PERFECT_VAN) {
                return GOED;
            } else if (p < PERFECT_TOT) {
                return PERFECT;
            } else if (p < GOED_TOT) {
                return GOED;
            }
            return AANGEBRAND;
        }

        /** Quality after this many ticks in the oven for a recipe that needs {@code bakTicks} ticks for the whole bar. */
        public static Kwaliteit na(int ticks, int bakTicks) {
            return bij(punt(ticks, bakTicks));
        }

        /** The pointer position (0..100, may go past 100) after this many ticks. */
        public static int punt(int ticks, int bakTicks) {
            return Math.max(0, ticks) * 100 / Math.max(1, bakTicks);
        }

        @Nullable
        public static Kwaliteit byIndex(int i) {
            return i >= 0 && i < values().length ? values()[i] : null;
        }
    }
}
