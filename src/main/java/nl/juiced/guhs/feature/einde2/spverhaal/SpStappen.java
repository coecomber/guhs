package nl.juiced.guhs.feature.einde2.spverhaal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Doel;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 3), the skelet: the seam between the skelet's two story lines and the slices that own
 * their steps. The skelet registers {@link SpVerhaal#RECEPT_LIJN} and {@link SpVerhaal#KROON_LIJN} once, with every step as a
 * STAND-IN; a slice takes over ITS steps from its {@code register} (common code) with these calls, never with a line of its
 * own:
 * <pre>
 * SpStappen.echt(SpVerhaal.RECEPT, SpVerhaal.R_BAKKEN);                      // the stand-in stops for this step
 * SpStappen.doel(SpVerhaal.RECEPT, SpVerhaal.R_BAKKEN, (p, s) -&gt; Doel.plek(...));   // compass + objective line (optional)
 * SpStappen.nodig(SpVerhaal.RECEPT, SpVerhaal.R_BAKKEN, (p, s) -&gt; List.of(...)); // the Guhdex "nodig" list (optional)
 * SpStappen.sleutel(SpVerhaal.RECEPT, SpVerhaal.R_VETEMMER, (p, s) -&gt; vlag ? "8_b" : "8");  // a reserved variant (optional)
 * SpStappen.ga(SpVerhaal.RECEPT, SpVerhaal.R_BAKKEN, p -&gt; Bakken.start(p));      // what /guhs einde2 sponsguh ga does (optional)
 * SpStappen.beloningen(SpVerhaal.RECEPT, p -&gt; List.of(...));                  // only the owner of the LAST step of the line
 * </pre>
 * A step moves on only with {@code SpVerhaal.lijn(lijn).verder(p, STAP)} (exactly from that step: idempotent); the next step's
 * owner hears it with {@code lijn.opStap(...)}. The default place of every step ({@link #STANDIN}) is a marker of the two
 * builds: the stand-in completes a step when the player stands there, and it is the step's {@link Doel} until its owner
 * gives another. Variant sleutels are reserved per step ({@link #VARIANTEN}): their texts are the owner's.
 */
public final class SpStappen {
    /** A place of a step: a marker of one of the two builds ("aankomst" = Appa's landing spot), or null = anywhere. */
    public record Plek(String structuur, String plek) {
    }

    /** The default place of every step (the stand-in's goal, the default Doel). Index = step. */
    public static final Map<String, List<Plek>> STANDIN = Map.of(
            SpVerhaal.RECEPT, List.of(
                    new Plek("", ""),                                            // 0 BRIEF: anywhere (the bubble letter comes to you)
                    new Plek(SpVerhaal.BIKINIBODEM, "aankomst"),                  // 1 REIS
                    new Plek(SpVerhaal.BIKINIBODEM, "bubbel_onder"),              // 2 BUBBEL
                    new Plek(SpVerhaal.BIKINIBODEM, "krabguh"),                   // 3 SOLLICITATIE
                    new Plek(SpVerhaal.BIKINIBODEM, "grill_speler"),              // 4 BAKKEN
                    new Plek(SpVerhaal.BIKINIBODEM, "kwallen_start"),             // 5 KWALLEN
                    new Plek(SpVerhaal.BIKINIBODEM, "examen_start"),              // 6 EXAMEN
                    new Plek(SpVerhaal.BIKINIBODEM, "kluis"),                     // 7 PLAN_Z
                    new Plek(SpVerhaal.BIKINIBODEM, "receptkluis"),               // 8 VETEMMER
                    new Plek(SpVerhaal.BIKINIBODEM, "krabguh"),                   // 9 TERUG
                    new Plek(SpVerhaal.BIKINIBODEM, "podium_trommel")),           // 10 BELLENKOM
            SpVerhaal.KROON, List.of(
                    new Plek(SpVerhaal.BIKINIBODEM, "krabguh"),                   // 0 MANAGER
                    new Plek(SpVerhaal.BIKINIBODEM, "ijs_speler"),                // 1 IJSSALON
                    new Plek(SpVerhaal.BIKINIBODEM, "neptuguh_troon"),            // 2 KROON
                    new Plek(SpVerhaal.SCHELPENSTAD, "kroeg_parkeer"),            // 3 RIT
                    new Plek(SpVerhaal.SCHELPENSTAD, "sleutel"),                  // 4 SLEUTEL
                    new Plek(SpVerhaal.SCHELPENSTAD, "snor_plek"),                // 5 SNOR
                    new Plek(SpVerhaal.SCHELPENSTAD, "loopgraaf_rand_oost"),      // 6 LOOPGRAAF
                    new Plek(SpVerhaal.SCHELPENSTAD, "rustpunt_3"),               // 7 DENNIS
                    new Plek(SpVerhaal.SCHELPENSTAD, "droogtafel_speler"),        // 8 CYCLOOP
                    new Plek(SpVerhaal.SCHELPENSTAD, "hasselguh_opstap"),         // 9 HASSELGUH
                    new Plek(SpVerhaal.BIKINIBODEM, "podium_sponsguh"),           // 10 EMMERS
                    new Plek(SpVerhaal.BIKINIBODEM, "podium_trommel")));          // 11 GUHFY

    /**
     * The variant sleutels reserved per line (CONTRACT_E2_C 2.3): "{@code <stap>_b}" = the second half of that step (a minigame
     * running, the recipe in your pocket, ...). Their nu/waar/kort texts come with the owner's step texts.
     */
    public static final Map<String, List<String>> VARIANTEN = Map.of(
            SpVerhaal.RECEPT, List.of("4_b", "5_b", "6_b", "8_b", "10_b"),
            SpVerhaal.KROON, List.of("3_b", "4_b", "6_b", "7_b", "8_b", "10_b"));

    private static final Set<String> ECHT = ConcurrentHashMap.newKeySet();
    private static final Map<String, BiFunction<ServerPlayer, Integer, Doel>> DOEL = new ConcurrentHashMap<>();
    private static final Map<String, BiFunction<ServerPlayer, Integer, List<VerhaalStand.Nodig>>> NODIG = new ConcurrentHashMap<>();
    private static final Map<String, BiFunction<ServerPlayer, Integer, String>> SLEUTEL = new ConcurrentHashMap<>();
    private static final Map<String, Consumer<ServerPlayer>> GA = new ConcurrentHashMap<>();
    private static final Map<String, Function<ServerPlayer, List<VerhaalStand.Beloning>>> BELONINGEN = new ConcurrentHashMap<>();

    private SpStappen() {
    }

    private static String k(String lijn, int stap) {
        return lijn + "#" + stap;
    }

    // =====================================================================================================================
    // what a slice calls (from its register; common code)
    // =====================================================================================================================

    /** This step is built for real: the stand-in leaves it alone. */
    public static void echt(String lijn, int stap) {
        ECHT.add(k(lijn, stap));
    }

    public static boolean isEcht(String lijn, int stap) {
        return ECHT.contains(k(lijn, stap));
    }

    /** The compass target and objective line of this step (default: its {@link #STANDIN} marker). */
    public static void doel(String lijn, int stap, BiFunction<ServerPlayer, Integer, Doel> doel) {
        DOEL.put(k(lijn, stap), doel);
    }

    /** What this step needs (the Guhdex list). */
    public static void nodig(String lijn, int stap, BiFunction<ServerPlayer, Integer, List<VerhaalStand.Nodig>> nodig) {
        NODIG.put(k(lijn, stap), nodig);
    }

    /** The sleutel of this step for this player: its number, or one of the step's reserved {@link #VARIANTEN}. */
    public static void sleutel(String lijn, int stap, BiFunction<ServerPlayer, Integer, String> sleutel) {
        SLEUTEL.put(k(lijn, stap), sleutel);
    }

    /** What the dev command {@code ga} starts once it has set the state and put the player at the step's place. */
    public static void ga(String lijn, int stap, Consumer<ServerPlayer> start) {
        GA.put(k(lijn, stap), start);
    }

    /** The rewards the Guhdex shows for the line (only the owner of the line's last step sets this). */
    public static void beloningen(String lijn, Function<ServerPlayer, List<VerhaalStand.Beloning>> beloningen) {
        BELONINGEN.put(lijn, beloningen);
    }

    // =====================================================================================================================
    // what the lines ask
    // =====================================================================================================================

    static String[] varianten(String lijn) {
        return VARIANTEN.getOrDefault(lijn, List.of()).toArray(String[]::new);
    }

    static String sleutel(String lijn, ServerPlayer p, int stap) {
        BiFunction<ServerPlayer, Integer, String> s = SLEUTEL.get(k(lijn, stap));
        String uit = s == null ? null : s.apply(p, stap);
        return uit == null ? String.valueOf(stap) : uit;
    }

    static List<VerhaalStand.Nodig> nodig(String lijn, ServerPlayer p, int stap) {
        BiFunction<ServerPlayer, Integer, List<VerhaalStand.Nodig>> n = NODIG.get(k(lijn, stap));
        return n == null ? List.of() : n.apply(p, stap);
    }

    static List<VerhaalStand.Beloning> beloningen(String lijn, ServerPlayer p) {
        Function<ServerPlayer, List<VerhaalStand.Beloning>> b = BELONINGEN.get(lijn);
        return b == null ? List.of() : b.apply(p);
    }

    @Nullable
    static Doel doel(String lijn, ServerPlayer p, int stap) {
        BiFunction<ServerPlayer, Integer, Doel> d = DOEL.get(k(lijn, stap));
        if (d != null) {
            return d.apply(p, stap);
        }
        Plek plek = standIn(lijn, stap);
        ServerLevel g = p.level().getServer() == null ? null : p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (plek == null || g == null || !Plekken.bekend(g, plek.structuur())) {
            return plek == null ? null
                    : Doel.structuur(GuheindeFeature.GUHEINDE, plek.structuur(), Component.translatable("structure.guhs." + plek.structuur()));
        }
        BlockPos pos = BlockPos.containing(wereld(g, plek));
        return Doel.plek(GuheindeFeature.GUHEINDE, pos, Component.translatable("gui.guhs.verhalen." + lijn + ".waar." + sleutel(lijn, p, stap)));
    }

    /** Runs the {@code ga} start of a step (dev command); false when the step's owner gave none. */
    static boolean ga(ServerPlayer p, String lijn, int stap) {
        Consumer<ServerPlayer> start = GA.get(k(lijn, stap));
        if (start == null) {
            return false;
        }
        start.accept(p);
        return true;
    }

    // =====================================================================================================================
    // places
    // =====================================================================================================================

    /** The default place of a step (null: anywhere, or no such step). */
    @Nullable
    public static Plek standIn(String lijn, int stap) {
        List<Plek> l = STANDIN.get(lijn);
        if (l == null || stap < 0 || stap >= l.size() || l.get(stap).structuur().isEmpty()) {
            return null;
        }
        return l.get(stap);
    }

    /** The world spot of a place (the middle of its block, on its floor; "aankomst" = Appa's landing spot); null: not known yet. */
    @Nullable
    public static Vec3 wereld(ServerLevel guheinde, Plek plek) {
        if (!Plekken.bekend(guheinde, plek.structuur())) {
            return null;
        }
        if (plek.plek().equals("aankomst")) {
            return Plekken.aankomst(guheinde, plek.structuur());
        }
        BlockPos b = Plekken.wereld(guheinde, plek.structuur(), plek.plek());
        return b == null ? null : Vec3.atBottomCenterOf(b);
    }

    /** From SpVerhaal.register: the stand-in's step talk. */
    static void registreer() {
        for (String lijn : SpVerhaal.LIJNEN) {
            SpVerhaal.lijn(lijn).opStap((p, oud, nieuw) -> SpStandIn.nieuweStap(p, lijn, nieuw));
        }
    }
}
