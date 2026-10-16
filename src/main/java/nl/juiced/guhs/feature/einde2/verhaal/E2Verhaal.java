package nl.juiced.guhs.feature.einde2.verhaal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.einde2.Einde2Haken;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Ontdekt;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.reizen.E2Reizen;
import nl.juiced.guhs.feature.guheinde.Enderguhs;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B (CONTRACT_E2_B 2, 3, 4.2): the story state of the Guhvatar story, read by every slice. All progress
 * lives in main's {@link Verhaallijn}s (per player, {@code GuhQuests.saved}); this class keeps nothing of its own and only
 * DERIVES: which chapter a player is in, which story structure is open (the veil), who learnt which element, whether the
 * finale is done, which buildings a player knows. The chapter slices register the five lines ({@link #HOOFDSTUKKEN});
 * everybody else finds them only by their id.
 * <p>
 * {@link #register} (once, from {@code Einde2.register}, both sides): the group "guheinde" of the Guhdex tab Verhalen,
 * the ONE big story {@value #GROOT} of the Guhpad, Guhdalfs sluier round the four story structures (plus, once their spots
 * are known, a box grown 48 blocks sideways in the high band), and the hooks of {@link Einde2Haken}.
 * Texts: tools/features/einde2_verhaal.py. Dev: {@link E2VerhaalCommando}, {@link E2VerhaalDev}.
 */
public final class E2Verhaal {
    /** The five lines of the Guhvatar story, in playing order (owners: h1, h2, h3, h4, h4). */
    public static final String LUCHT = "guhvatar_lucht", WATER = "guhvatar_water", AARDE = "guhvatar_aarde", VUUR = "guhvatar_vuur",
            FINALE = "guhvatar_finale";
    public static final List<String> HOOFDSTUKKEN = List.of(LUCHT, WATER, AARDE, VUUR, FINALE);
    /** The Verhaallijn group of every einde2 line (the heading "Verhalen van het Guheinde"), and the id of the big story. */
    public static final String GROEP = "guheinde", GROOT = "guhvatar";
    /** The cross-slice flags (CONTRACT_E2_B 2.2), set only by their owner. */
    public static final String V_GELEERD = "geleerd", V_GEEST = "geest", V_ZUKA_TEAM = "zuka_team", V_WITTE_LOTUS = "witte_lotus";
    /** How far (blocks, sideways) the veil reaches round a story structure, besides its own box. */
    public static final int SLUIER_RAND = 48;
    /** The side places that belong to the Guhvatar story (the fast travel of the meditation stones lists only these). */
    public static final List<String> GUHVATAR_ZIJ = List.of(Einde2Ids.OMASHGUH, Einde2Ids.WESTELIJKE_LUCHTTEMPEL, Einde2Ids.VUURTEMPEL_ROKU,
            Einde2Ids.KYOSHI_DORP, Einde2Ids.BIBLIOTHEEK_WAN_SHI_GUH, Einde2Ids.LEEUWSCHILDPAD_EILAND);

    /** (tests) step counts of lines that are not registered (yet): they are then read straight from main's keys. */
    static final Map<String, Integer> STAND_IN = new ConcurrentHashMap<>();
    /** (tests) who a player is the guest of; the real answer is reizen's. */
    static volatile Function<ServerPlayer, ServerPlayer> gastheer = E2Reizen::gastheer;

    /** The Guheinde levels whose veils have been put round the guaranteed copies (a level of a new server start: again). */
    private static final Map<ServerLevel, Set<String>> GEZET = Collections.synchronizedMap(new WeakHashMap<>());

    private E2Verhaal() {
    }

    // =====================================================================================================================
    // the lines
    // =====================================================================================================================

    /** The registered line (null: its slice has not registered it). */
    @Nullable
    public static Verhaallijn lijn(String id) {
        return Verhaallijnen.van(id);
    }

    private static String sleutel(String lijn, String wat) {
        return "guhs_" + lijn + "_" + wat;
    }

    /** The player's step in this line (0 when the line is not registered). */
    public static int stap(ServerPlayer p, String lijn) {
        Verhaallijn l = lijn(lijn);
        if (l != null) {
            return l.stap(p);
        }
        Integer n = STAND_IN.get(lijn);
        return n == null ? 0 : Math.max(0, Math.min(n, GuhQuests.saved(p).getIntOr(sleutel(lijn, "stap"), 0)));
    }

    /** Has the player finished this line (never for a line that is not registered)? */
    public static boolean klaar(ServerPlayer p, String lijn) {
        Verhaallijn l = lijn(lijn);
        if (l != null) {
            return l.klaar(p);
        }
        Integer n = STAND_IN.get(lijn);
        return n != null && stap(p, lijn) >= n;
    }

    /** Has the player started this line (a step done, or its first talk)? */
    public static boolean begonnen(ServerPlayer p, String lijn) {
        Verhaallijn l = lijn(lijn);
        if (l != null) {
            return l.begonnen(p);
        }
        return STAND_IN.containsKey(lijn) && (stap(p, lijn) > 0 || GuhQuests.saved(p).getBooleanOr(sleutel(lijn, "begonnen"), false));
    }

    /** A flag of this line for this player (CONTRACT_E2_B 2.2). */
    public static boolean vlag(ServerPlayer p, String lijn, String vlag) {
        Verhaallijn l = lijn(lijn);
        if (l != null) {
            return l.vlag(p, vlag);
        }
        return STAND_IN.containsKey(lijn) && GuhQuests.saved(p).getBooleanOr(sleutel(lijn, "v_" + vlag), false);
    }

    /** May the Guhvatar story begin for this player: Opper-Mika beaten (by themselves, at least once)? */
    public static boolean magBeginnen(ServerPlayer p) {
        return Enderguhs.heeftVerslagen(p);
    }

    /** Has this player finished the whole story (the finale)? Tier 5 asks this. */
    public static boolean finaleKlaar(ServerPlayer p) {
        return klaar(p, FINALE);
    }

    /** Has this player learnt this element: the chapter's flag {@value #V_GELEERD}, or the chapter done. */
    public static boolean elementGeleerd(ServerPlayer p, Einde2Ids.Element e) {
        String lijn = switch (e) {
            case LUCHT -> LUCHT;
            case WATER -> WATER;
            case AARDE -> AARDE;
            case VUUR -> VUUR;
        };
        return vlag(p, lijn, V_GELEERD) || klaar(p, lijn);
    }

    // =====================================================================================================================
    // the structures
    // =====================================================================================================================

    /** The story structure of a chapter line (the finale plays in the capital too). */
    public static String structuurVan(String lijn) {
        return switch (lijn) {
            case LUCHT -> Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL;
            case WATER -> Einde2Ids.NOORDELIJKE_WATERSTAM;
            case AARDE -> Einde2Ids.BA_SING_GUH;
            case VUUR, FINALE -> Einde2Ids.VUURNATIE_HOOFDSTAD;
            default -> throw new IllegalArgumentException("not a chapter of the Guhvatar story: " + lijn);
        };
    }

    /** The chapter line of a story structure (the capital: {@link #VUUR}); null for any other structure. */
    @Nullable
    public static String lijnVan(String structuur) {
        return switch (structuur) {
            case Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL -> LUCHT;
            case Einde2Ids.NOORDELIJKE_WATERSTAM -> WATER;
            case Einde2Ids.BA_SING_GUH -> AARDE;
            case Einde2Ids.VUURNATIE_HOOFDSTAD -> VUUR;
            default -> null;
        };
    }

    /**
     * Is this structure unlocked for this player HIMSELF (CONTRACT_E2_B 2.3): the air temple once {@link #LUCHT} has begun,
     * each later one once the chapter before it is done. Any structure that is not a story structure: always.
     */
    public static boolean ontgrendeld(ServerPlayer p, String structuur) {
        return switch (structuur) {
            case Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL -> begonnen(p, LUCHT);
            case Einde2Ids.NOORDELIJKE_WATERSTAM -> klaar(p, LUCHT);
            case Einde2Ids.BA_SING_GUH -> klaar(p, WATER);
            case Einde2Ids.VUURNATIE_HOOFDSTAD -> klaar(p, AARDE);
            default -> true;
        };
    }

    /** May this player in: unlocked for them, or for the host they travel with as a guest ({@code E2Reizen.gastheer}). */
    public static boolean open(ServerPlayer p, String structuur) {
        if (ontgrendeld(p, structuur)) {
            return true;
        }
        ServerPlayer host = gastheer.apply(p);
        return host != null && host != p && ontgrendeld(host, structuur);
    }

    /** The story structures this player has unlocked himself, in chapter order. */
    public static List<String> ontgrendeld(ServerPlayer p) {
        List<String> uit = new ArrayList<>();
        for (String s : Einde2Ids.VERHAAL_STRUCTUREN) {
            if (ontgrendeld(p, s)) {
                uit.add(s);
            }
        }
        return uit;
    }

    /**
     * The buildings this player knows (Appa's whistle list, the fast travel of the meditation stones): the story structures
     * they unlocked, then the Guheinde buildings they discovered ({@link Einde2Ontdekt}); with {@code alleenGuhvatar} only
     * the discovered side places of the Guhvatar story ({@link #GUHVATAR_ZIJ}). No doubles; story order first.
     */
    public static List<String> bekendeGebouwen(ServerPlayer p, boolean alleenGuhvatar) {
        List<String> uit = new ArrayList<>(ontgrendeld(p));
        Set<String> ontdekt = Einde2Ontdekt.ontdekt(p);
        List<String> volgorde = new ArrayList<>(Einde2Ids.LEEGTE_STRUCTUREN);
        for (String s : ontdekt) {
            if (!volgorde.contains(s)) {
                volgorde.add(s);
            }
        }
        for (String s : volgorde) {
            if (!ontdekt.contains(s) || uit.contains(s) || Einde2Ids.VERHAAL_STRUCTUREN.contains(s)) {
                continue;
            }
            if (!alleenGuhvatar || GUHVATAR_ZIJ.contains(s)) {
                uit.add(s);
            }
        }
        return uit;
    }

    // =====================================================================================================================
    // register
    // =====================================================================================================================

    /** The big story of the Guhpad (null before {@link #register}). */
    @Nullable
    private static GroteVerhalen.GrootVerhaal groot;

    @Nullable
    public static GroteVerhalen.GrootVerhaal grootVerhaal() {
        return groot;
    }

    /** From Einde2.register (common code, both sides), after Bewoners.register(). */
    public static void register() {
        GroteVerhalen.zetGroep(GROEP, GroteVerhalen.Wereld.GUHEINDE);
        groot = GroteVerhalen.registreer(new GroteVerhalen.GrootVerhaal(GROOT, GroteVerhalen.Wereld.GUHEINDE, GroteVerhalen.Wereld.GUHEINDE,
                "guhs:guhvatar_beeld", Einde2Ids.VERHAAL_STRUCTUREN, HOOFDSTUKKEN, () -> true, p -> klaar(p, FINALE)));
        for (String s : Einde2Ids.VERHAAL_STRUCTUREN) {
            Sluiers.registreer(s, 0, p -> open(p, s));
        }
        Einde2Haken.ELEMENT_GELEERD = E2Verhaal::elementGeleerd;
        Einde2Haken.VERBORGEN = Sluiers::isVerborgen;
        Einde2Haken.VERHAAL_KLAAR = (p, id) -> klaar(p, id);
        NeoForge.EVENT_BUS.addListener(E2Verhaal::opTick);
        NeoForge.EVENT_BUS.addListener(E2VerhaalCommando::register);
    }

    /** Puts the wide veil round each story structure as soon as the spot of its guaranteed copy is known. */
    private static void opTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 != 7) {
            return;
        }
        ServerLevel guheinde = event.getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (guheinde == null) {
            return;
        }
        Set<String> gezet = GEZET.computeIfAbsent(guheinde, l -> ConcurrentHashMap.newKeySet());
        for (String s : Einde2Ids.VERHAAL_STRUCTUREN) {
            if (gezet.contains(s) || !Plekken.bekend(guheinde, s)) {
                continue;
            }
            gezet.add(s);
            BoundingBox doos = sluierDoos(guheinde, s);
            if (doos != null) {
                Sluiers.zetPlek(guheinde, s, doos);
            }
        }
    }

    /** The box of the veil: the build's box grown {@link #SLUIER_RAND} in x and z, its bottom never below the high band. */
    @Nullable
    public static BoundingBox sluierDoos(ServerLevel guheinde, String structuur) {
        BoundingBox b = Plekken.doos(guheinde, structuur);
        if (b == null) {
            return null;
        }
        return new BoundingBox(b.minX() - SLUIER_RAND, Math.max(b.minY(), Einde2Ids.HOOG_MIN), b.minZ() - SLUIER_RAND,
                b.maxX() + SLUIER_RAND, Math.max(b.maxY(), Einde2Ids.HOOG_MIN), b.maxZ() + SLUIER_RAND);
    }

    /** (dev, tests) every saved key of a line of this player (main's format). */
    static void wisSleutels(ServerPlayer p, String lijn) {
        CompoundTag saved = GuhQuests.saved(p);
        String prefix = "guhs_" + lijn + "_";
        for (String k : List.copyOf(saved.keySet())) {
            if (k.startsWith(prefix)) {
                saved.remove(k);
            }
        }
    }
}
