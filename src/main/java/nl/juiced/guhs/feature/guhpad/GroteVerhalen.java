package nl.juiced.guhs.feature.guhpad;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.balto.BaltoVerhaal;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhwaii.Ohana;
import nl.juiced.guhs.feature.hemel.HemelQuest;
import nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ringh2.Guhvendel;
import nl.juiced.guhs.feature.ringh3.Mijn;
import nl.juiced.guhs.feature.ringh4.Boomstad;
import nl.juiced.guhs.feature.ringh5.RingH5Feature;
import nl.juiced.guhs.feature.ringh6.Berg;
import nl.juiced.guhs.feature.ringsausuman.Toren;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Het Guhpad (DESIGN_VERHALENPAD A): the ONE registry of the "big stories", each with the world it belongs to. Everything
 * of the Guhpad reads it: the locks on the Knabbelring, the Guhbarbecuether and the Guheinde ({@link Guhpad}), the path
 * map and the per-world sections of the Guhdex tab Verhalen (client.GuhpadTab), the counter "Verhalen gevolgd: n van m",
 * the Superkompas option "Mijn verhaal" ({@link GuhpadKompas}) and the lock quests of the FTB chapter group "Het Guhpad"
 * (tools/features/guhpad.py reads this file in its self-check: keep a story's id and its two worlds on the line where it is made).
 * <p>
 * How each story says "finished" for a player (always per player, read from that story's own progress):
 * <ul>
 *   <li>Balto: {@code BaltoVerhaal.stap >= KLAAR}; Mewtwo: {@code MewtwoVoortgang.stap >= KLAAR}; the hemelkapelletje:
 *       {@code HemelQuest.klopt} (the Knuffelhart beats); Lilo &amp; Stitch: {@code Ohana.stap >= KLAAR};</li>
 *   <li>Het Snuffeleiland: the Verhaallijn {@value #SNUFFELEILAND} is done (its first rank; that slice registers the line.
 *       While no such line is registered the story does not exist: it is not asked for and not counted);</li>
 *   <li>the Knabbelring: {@code Ring.klaar} (chapter 6, ring_h6, done); Super Guhrio: the line "guhrio" is done.</li>
 * </ul>
 * A later update adds its own with {@link #registreer} (the big Guheinde stories: world {@link Wereld#GUHEINDE}) and says
 * where its other questlines belong with {@link #zetGroep} / {@link #zetLijn}.
 */
public final class GroteVerhalen {
    /** The four stops of the Guhpad, in order. */
    public enum Wereld {
        GUHMENSIE("guhs:block_of_kaasknabbels"), BARBECUETHER("guhs:grillkool"), GUHEINDE("guhs:oog_van_vadsig"), ECHT("minecraft:ender_eye");

        private final String icoon;

        Wereld(String icoon) {
            this.icoon = icoon;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The item that draws this stop. */
        public String icoon() {
            return icoon;
        }

        /** "De Guhmensie", "De Guhbarbecuether", "Het Guheinde", "Het echte Guheinde". */
        public MutableComponent naam() {
            return Component.translatable("gui.guhs.guhpad.wereld." + id());
        }

        /** The dimension of this stop (null: the real Guheinde, which does not exist yet). */
        @Nullable
        public ResourceKey<Level> dimensie() {
            return switch (this) {
                case GUHMENSIE -> ModDimensions.GUHMENSION;
                case BARBECUETHER -> BarbecuetherFeature.BARBECUETHER;
                case GUHEINDE -> GuheindeFeature.GUHEINDE;
                case ECHT -> null;
            };
        }

        /** The stop a dimension belongs to (null: none of ours, e.g. the overworld). */
        @Nullable
        public static Wereld van(ResourceKey<Level> dim) {
            for (Wereld w : values()) {
                if (w.dimensie() == dim) {
                    return w;
                }
            }
            return null;
        }
    }

    /**
     * A big story: its id, the world it belongs to on the Guhpad, the world where it begins (where its structures stand:
     * the Knabbelring belongs to the Guhbarbecuether but begins at Guhdalf's camp in the Guhmensie), the item that draws it,
     * the structures where it begins (structure ids without namespace, for "Mijn verhaal"), the questlines of the Guhdex
     * tab Verhalen that are this story, whether it exists in this game, and whether a player has really finished it.
     */
    public record GrootVerhaal(String id, Wereld wereld, Wereld begin, String icoon, List<String> structuren, List<String> lijnen,
                               BooleanSupplier bestaat, Predicate<ServerPlayer> afgerond) {
        public GrootVerhaal {
            structuren = List.copyOf(structuren);
            lijnen = List.copyOf(lijnen);
        }

        /** The story's name in a list ("Baltoguh en Nomguh"). */
        public MutableComponent naam() {
            return Component.translatable(naamSleutel());
        }

        public String naamSleutel() {
            return "gui.guhs.guhpad.verhaal." + id;
        }

        /**
         * (1.4.1) The places of this story the Superkompas lists under it, in story order: where it begins, then what
         * {@link GroteVerhalen#voegPlek(String, String)} added (the Knabbelring: one place per chapter and the Toren).
         */
        public List<String> plekken() {
            List<String> out = new ArrayList<>(structuren);
            for (String extra : PLEKKEN.getOrDefault(id, List.of())) {
                if (!out.contains(extra)) {
                    out.add(extra);
                }
            }
            return out;
        }

        /** Is the story in this game (always, except a story whose own update is not installed yet)? */
        public boolean isEr() {
            return STAND_IN.containsKey(id) || bestaat.getAsBoolean();
        }

        /** Has this player finished it (or did an op say so, {@link GroteVerhalen#zetGedaan})? Never true for a story that isn't there. */
        public boolean klaar(ServerPlayer p) {
            if (!isEr()) {
                return false;
            }
            return GuhQuests.saved(p).getBooleanOr(GEDAAN + id, false) || bestaat.getAsBoolean() && afgerond.test(p);
        }
    }

    /** The fixed id of the Snuffeleiland story (DESIGN_VERHALENPAD C, "Fixed ids"): its Verhaallijn and its group. */
    public static final String SNUFFELEILAND = "snuffeleiland";
    /** GuhQuests.saved: {@code guhs_guhpad_gedaan_<id>} = an op marked this story as finished for the player. */
    public static final String GEDAAN = "guhs_guhpad_gedaan_";

    private static final List<GrootVerhaal> ALLE = new CopyOnWriteArrayList<>();
    /** Which world the questlines of a Verhaallijn group belong to, and single questlines that differ from their group. */
    private static final Map<String, Wereld> GROEPEN = new ConcurrentHashMap<>(), LIJNEN = new ConcurrentHashMap<>();
    /** (1.4.1) The Superkompas places of a big story besides where it begins, and the places of a world that belong to no big story. */
    private static final Map<String, List<String>> PLEKKEN = new ConcurrentHashMap<>();
    private static final Map<Wereld, List<String>> WERELD_PLEKKEN = new ConcurrentHashMap<>();
    /** (game tests) stories that count as "there" although their own update is not installed. */
    private static final Map<String, Boolean> STAND_IN = new ConcurrentHashMap<>();

    public static final GrootVerhaal BALTO = groot("balto", Wereld.GUHMENSIE, Wereld.GUHMENSIE, "guhs:baltoguh_beeldje", List.of("nomguh"), List.of("balto"),
            p -> BaltoVerhaal.stap(p) >= BaltoVerhaal.KLAAR);
    public static final GrootVerhaal MEWTWO = groot("mewtwo", Wereld.GUHMENSIE, Wereld.GUHMENSIE, "guhs:mewtwo_labnotitie", List.of("kloon_eiland"), List.of("mewtwo"),
            p -> MewtwoVoortgang.stap(p) >= MewtwoVoortgang.KLAAR);
    public static final GrootVerhaal HEMEL = groot("hemel", Wereld.GUHMENSIE, Wereld.GUHMENSIE, "guhs:pluisveertje", List.of("hemelkapelletje"), List.of("hemel"),
            HemelQuest::klopt);
    public static final GrootVerhaal GUHWAII = groot("guhwaii", Wereld.GUHMENSIE, Wereld.GUHMENSIE, "guhs:kokosnoot", List.of("guhwaii_ohana"), List.of("guhwaii"),
            p -> Ohana.stap(p) >= Ohana.KLAAR);
    public static final GrootVerhaal SNUFFEL = registreer(new GrootVerhaal(SNUFFELEILAND, Wereld.GUHMENSIE, Wereld.GUHMENSIE, "minecraft:bone",
            List.of("steigerhuisje"), List.of(SNUFFELEILAND), () -> Verhaallijnen.van(SNUFFELEILAND) != null, p -> lijnKlaar(SNUFFELEILAND, p)));
    public static final GrootVerhaal KNABBELRING = groot("knabbelring", Wereld.BARBECUETHER, Wereld.GUHMENSIE, "guhs:knabbelring", List.of("knabbelgouw"),
            List.of("ring_h1", "ring_h2", "ring_h3", "ring_h4", "ring_h5", "ring_h6"), Ring::klaar);
    public static final GrootVerhaal GUHRIO = groot("guhrio", Wereld.BARBECUETHER, Wereld.BARBECUETHER, "guhs:guhrio_vadsmunt", List.of(GuhrioKasteel.STRUCTUUR),
            List.of("guhrio"), p -> GuhrioKasteel.LIJN.klaar(p));

    static {
        // where the questlines of the Guhdex tab Verhalen belong (a group or line that is not named: the Guhmensie)
        for (String groep : List.of("knabbelring", "guhrio", "techniek", "barbecue")) {
            GROEPEN.put(groep, Wereld.BARBECUETHER);
        }
        LIJNEN.put("guheinde", Wereld.GUHEINDE);   // (the Opper-Mika line, one of the older adventures)
        // 1.4.1: the Superkompas tab Verhalen lists every story's places per world. The Knabbelring: the place of each
        // chapter after the Knabbelgouw, and the Toren van Sausuman (Guhdalfs sluier decides per player what shows as "???")
        for (String plek : List.of(Guhvendel.STRUCTUUR, Mijn.STRUCTUUR, Boomstad.STRUCTUUR, RingH5Feature.STRUCTUUR, Berg.STRUCTUUR, Toren.STRUCTUUR)) {
            voegPlek(KNABBELRING.id(), plek);
        }
        // the Guheinde has no big story of its own yet: the way in (the Opper-Mika adventure) and the way back
        voegPlek(Wereld.GUHEINDE, "knabbelkelder");
        voegPlek(Wereld.GUHEINDE, "guheinde_terugpoort");
    }

    private static GrootVerhaal groot(String id, Wereld wereld, Wereld begin, String icoon, List<String> structuren, List<String> lijnen,
                                      Predicate<ServerPlayer> afgerond) {
        return registreer(new GrootVerhaal(id, wereld, begin, icoon, structuren, lijnen, () -> true, afgerond));
    }

    private static boolean lijnKlaar(String id, ServerPlayer p) {
        Verhaallijn l = Verhaallijnen.van(id);
        return l != null && l.klaar(p);
    }

    /**
     * Adds a big story (from your Feature.register; common code, both sides). It then counts for "Verhalen gevolgd", shows
     * on the path map, is asked for by the lock of the next world, and "Mijn verhaal" can point at its structures. Lang:
     * {@code gui.guhs.guhpad.verhaal.<id>}; hidden advancement {@code guhs:quest/guhpad_klaar_<id>} (bbq2.verborgen).
     * <pre>
     * GroteVerhalen.registreer(new GroteVerhalen.GrootVerhaal("guhvatar", GroteVerhalen.Wereld.GUHEINDE, GroteVerhalen.Wereld.GUHEINDE,
     *         "guhs:...", List.of("luchttempel"), List.of("guhvatar"), () -> true, p -> LIJN.klaar(p)));
     * </pre>
     */
    public static GrootVerhaal registreer(GrootVerhaal v) {
        synchronized (ALLE) {
            if (van(v.id()) != null) {
                throw new IllegalStateException("The big story " + v.id() + " is registered twice");
            }
            ALLE.add(v);
        }
        return v;
    }

    /**
     * (1.4.1) Adds a place (a guhs structure id without namespace) to a big story: the Superkompas tab Verhalen lists it
     * under that story, after the places that are there (from your Feature.register; common code, both sides). Lang
     * structure.guhs.&lt;id&gt; (+ .tooltip). A place behind Guhdalfs sluier shows as "???" until it is open for the player.
     */
    public static void voegPlek(String verhaal, String structuur) {
        PLEKKEN.compute(verhaal, (k, oud) -> metPlek(oud, structuur));
    }

    /** (1.4.1) Adds a place that belongs to no big story to a world of the Superkompas tab Verhalen (both sides). */
    public static void voegPlek(Wereld wereld, String structuur) {
        WERELD_PLEKKEN.compute(wereld, (k, oud) -> metPlek(oud, structuur));
    }

    private static List<String> metPlek(@Nullable List<String> oud, String structuur) {
        List<String> nieuw = new ArrayList<>(oud == null ? List.of() : oud);
        if (!nieuw.contains(structuur)) {
            nieuw.add(structuur);
        }
        return List.copyOf(nieuw);
    }

    /** (1.4.1) The places of this world that belong to no big story, in the order they were added. */
    public static List<String> plekken(Wereld wereld) {
        return WERELD_PLEKKEN.getOrDefault(wereld, List.of());
    }

    /** (1.4.1) Is this structure a place of a big story of this game? */
    public static boolean isVerhaalPlek(@Nullable String structuur) {
        if (structuur == null) {
            return false;
        }
        for (GrootVerhaal v : alle()) {
            if (v.plekken().contains(structuur)) {
                return true;
            }
        }
        return false;
    }

    /** (1.4.1) Is this structure a place of a big story of this game, or of a world (so the Superkompas may look for it)? */
    public static boolean isPlek(@Nullable String structuur) {
        return isVerhaalPlek(structuur) || structuur != null && WERELD_PLEKKEN.values().stream().anyMatch(l -> l.contains(structuur));
    }

    /** The questlines of this Verhaallijn group are shown under this world in the Guhdex (both sides). */
    public static void zetGroep(String groep, Wereld wereld) {
        GROEPEN.put(groep, wereld);
    }

    /** This one questline is shown under this world, whatever its group says (both sides). */
    public static void zetLijn(String lijn, Wereld wereld) {
        LIJNEN.put(lijn, wereld);
    }

    /** Every big story that is registered, the worlds in order, each world's stories in the order they registered. */
    public static List<GrootVerhaal> geregistreerd() {
        List<GrootVerhaal> out = new ArrayList<>();
        for (Wereld w : Wereld.values()) {
            for (GrootVerhaal v : ALLE) {
                if (v.wereld() == w) {
                    out.add(v);
                }
            }
        }
        return out;
    }

    /** The big stories of this game ({@link GrootVerhaal#isEr}), in the same order. */
    public static List<GrootVerhaal> alle() {
        return geregistreerd().stream().filter(GrootVerhaal::isEr).toList();
    }

    /** The big stories of one world that are in this game. */
    public static List<GrootVerhaal> van(Wereld wereld) {
        return alle().stream().filter(v -> v.wereld() == wereld).toList();
    }

    @Nullable
    public static GrootVerhaal van(@Nullable String id) {
        for (GrootVerhaal v : ALLE) {
            if (v.id().equals(id)) {
                return v;
            }
        }
        return null;
    }

    /** The big story a questline of the Guhdex belongs to (null: it is a small one). */
    @Nullable
    public static GrootVerhaal vanLijn(String lijn) {
        for (GrootVerhaal v : ALLE) {
            if (v.lijnen().contains(lijn)) {
                return v;
            }
        }
        return null;
    }

    /**
     * The world a questline of the Guhdex tab Verhalen is shown under: its big story's world, else what {@link #zetLijn} /
     * {@link #zetGroep} said (its group is the Verhaallijn's group; the hard-coded older lines have none), else the Guhmensie.
     */
    public static Wereld wereldVan(String lijn) {
        Wereld w = LIJNEN.get(lijn);
        if (w != null) {
            return w;
        }
        GrootVerhaal v = vanLijn(lijn);
        if (v != null) {
            return v.wereld();
        }
        Verhaallijn l = Verhaallijnen.van(lijn);
        w = l == null ? null : GROEPEN.get(l.groep());
        return w == null ? Wereld.GUHMENSIE : w;
    }

    /** How many big stories this player has finished ("Verhalen gevolgd: n van m"). */
    public static int gevolgd(ServerPlayer p) {
        int n = 0;
        for (GrootVerhaal v : alle()) {
            if (v.klaar(p)) {
                n++;
            }
        }
        return n;
    }

    /** How many big stories there are in this game. */
    public static int totaal() {
        return alle().size();
    }

    /**
     * (ops: {@code /guhs guhpad gedaan}; game tests) marks a big story as finished for this player, or takes that mark away
     * again. The story's own progress is not touched: a real finish stays a finish.
     */
    public static void zetGedaan(ServerPlayer p, GrootVerhaal v, boolean gedaan) {
        if (gedaan) {
            GuhQuests.saved(p).putBoolean(GEDAAN + v.id(), true);
        } else {
            GuhQuests.saved(p).remove(GEDAAN + v.id());
        }
    }

    /**
     * (game tests) lets a story whose own update is not installed count as "there" (it can then only be finished with
     * {@link #zetGedaan}); false takes the stand-in away. Returns whether a stand-in was set before.
     */
    public static boolean standIn(GrootVerhaal v, boolean aan) {
        return aan ? STAND_IN.put(v.id(), true) != null : STAND_IN.remove(v.id()) != null;
    }

    private GroteVerhalen() {
    }
}
