package nl.juiced.guhs.feature.einde2.spverhaal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.feature.einde2.Einde2Haken;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.reizen.E2Reizen;
import nl.juiced.guhs.feature.einde2.verhaal.E2Hervat;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 2, 3, 4), the skelet: the story state of the second story of the Guheinde, Sponsguh
 * Kaasbroek (DESIGN 5b), read by every Sponsguh slice. All progress lives in main's {@link Verhaallijn}s (per player); this
 * class registers the two story lines with every step as a STAND-IN ({@link SpStappen}, {@link SpStandIn}), and derives the
 * rest:
 * <ul>
 *   <li>the gate ({@link #open}): Bikinibodem and Schelpenstad open for a player once the Guhvatar story is done
 *       ({@link E2Verhaal#finaleKlaar}), or for the guest of such a host. Until then Guhdalfs sluier of main
 *       ({@link Sluiers}) stands round both places, with the Sponsguh message ({@link Sluiers#BERICHTEN}) and a friendly
 *       explanation in the chat when the player comes near ({@link SpPoort});</li>
 *   <li>the line {@value #RECEPT} (part 1, "Het geheime recept") is begun by itself as soon as the gate opens, so it shows
 *       in the Guhdex at once (its step 0 is the bubble letter of sp_d1_start); {@value #KROON} (part 2) follows it
 *       ({@code na});</li>
 *   <li>the big story {@value #GROOT} of the Guhpad, the group heading {@value #GROEP} of the Verhalen tab, the hooks of
 *       {@link Einde2Haken} (the Knabbelburger recipe), the place of Bikinibodem in Appa's menu
 *       ({@link E2Verhaal#EXTRA_GEBOUWEN}) and the story's place in "Verder met het verhaal" ({@link E2Hervat}).</li>
 * </ul>
 * Step numbers are fixed here and never renumbered (FTB tasks and saves name them). Texts: tools/features/einde2_sp_verhaal.py
 * (the step texts come from each step owner's module). Dev: {@link SpCommando}, {@link SpVerhaalDev}.
 */
public final class SpVerhaal {
    /** The two story lines (ids reserved in {@link Einde2Haken#GROTE_VERHALEN}). */
    public static final String RECEPT = "sponsguh_recept", KROON = "sponsguh_kroon";
    public static final List<String> LIJNEN = List.of(RECEPT, KROON);
    /** The side lines of the town (ids reserved in {@link Einde2Haken#KLEINE_VERHALEN}; registered by sp_zij). */
    public static final String SANDY = "sandy_boomkoepel", GUHLANDER = "vliegende_guhlander", ZEEMEERMAN = "zeemeerman";
    public static final List<String> ZIJLIJNEN = List.of(SANDY, GUHLANDER, ZEEMEERMAN);
    /** The Verhaallijn group (heading "Sponsguh Kaasbroek" in the Guhdex tab Verhalen) and the id of the big story. */
    public static final String GROEP = "sponsguh", GROOT = "sponsguh";
    /** The two places. */
    public static final String BIKINIBODEM = Einde2Ids.BIKINIBODEM, SCHELPENSTAD = Einde2Ids.SCHELPENSTAD;
    /** How far (blocks, every side) the sluier reaches round each of the two builds. */
    public static final int SLUIER_RAND = 8;
    /** The lang prefix of the skelet. */
    public static final String P = "gui.guhs.einde2_sp_verhaal.";

    // =====================================================================================================================
    // the steps (CONTRACT_E2_C 2.2; owner in brackets). NEVER renumber.
    // =====================================================================================================================
    /** Part 1, {@value #RECEPT}: 11 steps, done at {@value #R_STAPPEN}. */
    public static final int R_BRIEF = 0,          // sp_d1_start: the bubble letter (scene brief)
            R_REIS = 1,                            // sp_d1_start: to Bikinibodem (Appa / compass), arrival on the island (scene aankomst)
            R_BUBBEL = 2,                          // sp_d1_start: down the bubbelstroom (scene welkom)
            R_SOLLICITATIE = 3,                    // sp_d1_start: Meneer Krabguh's job interview (scene sollicitatie)
            R_BAKKEN = 4,                          // sp_d1_start: bake Knabbelburgers (minigame)
            R_KWALLEN = 5,                         // sp_d1_stad: jellyfishing with Patguh (scene patguh, minigame, wink 7)
            R_EXAMEN = 6,                          // sp_d1_stad: the boating exam (scene pufguh, minigame, wink 5)
            R_PLAN_Z = 7,                          // sp_d1_recept: back to the restaurant: scene plan_z (the theft)
            R_VETEMMER = 8,                        // sp_d1_recept: sneak through De Vetemmer to the recipe (wink 6, scene recept)
            R_TERUG = 9,                           // sp_d1_recept: the recipe back to Krabguh (scene feest)
            R_BELLENKOM = 10,                      // sp_d1_recept: the drum on the stage, scene zoete_zege; line done
            R_STAPPEN = 11;
    /** Part 2, {@value #KROON}: 12 steps, done at {@value #K_STAPPEN}. */
    public static final int K_MANAGER = 0,        // sp_d2_heen: Krabguh picks a manager (scene manager)
            K_IJSSALON = 1,                        // sp_d2_heen: the Guhfy Guhber (scene ijssalon, wink 8)
            K_KROON = 2,                           // sp_d2_heen: the crown is gone, Krabguh frozen (scene bevroren)
            K_RIT = 3,                             // sp_d2_heen: drive the Knabbelburgermobiel to De Ruige Kroeg (scene kroeg)
            K_SLEUTEL = 4,                         // sp_d2_heen: the key back in De Ruige Kroeg (stand still, scene baby)
            K_SNOR = 5,                            // sp_d2_weg: the moustaches at the trench (wink 9, scene snor)
            K_LOOPGRAAF = 6,                       // sp_d2_weg: through the loopgraaf (scene monsters)
            K_DENNIS = 7,                          // sp_d2_weg: Dennis and the moustache (scene dennis_snor, chase, scene laars)
            K_CYCLOOP = 8,                         // sp_d2_weg: the souvenir shop (scene cycloop, the tear, scene sproeier)
            K_HASSELGUH = 9,                       // sp_d2_finale: David Hasselguh carries you home (scene hasselguh, ride)
            K_EMMERS = 10,                         // sp_d2_finale: free the town of Plankmika's bucket helmets
            K_GUHFY = 11,                          // sp_d2_finale: the stage: scene guhfy_guhber; line done
            K_STAPPEN = 12;

    /** Part 1. */
    public static final Verhaallijn RECEPT_LIJN = Verhaallijn.maak(RECEPT, GROEP).stappen(R_STAPPEN).icoon("guhs:knabbelburger")
            .doelregel(true).sleutel((p, s) -> SpStappen.sleutel(RECEPT, p, s)).extraSleutels(SpStappen.varianten(RECEPT))
            .nodig((p, s) -> SpStappen.nodig(RECEPT, p, s)).beloningen(p -> SpStappen.beloningen(RECEPT, p))
            .doel((p, s) -> SpStappen.doel(RECEPT, p, s)).registreer();
    /** Part 2 (after part 1). */
    public static final Verhaallijn KROON_LIJN = Verhaallijn.maak(KROON, GROEP).stappen(K_STAPPEN).icoon("guhs:neptuguh_spawn_egg")
            .doelregel(true).na(RECEPT).sleutel((p, s) -> SpStappen.sleutel(KROON, p, s)).extraSleutels(SpStappen.varianten(KROON))
            .nodig((p, s) -> SpStappen.nodig(KROON, p, s)).beloningen(p -> SpStappen.beloningen(KROON, p))
            .doel((p, s) -> SpStappen.doel(KROON, p, s)).registreer();

    /** (tests) has this player finished the Guhvatar story; the real answer is E2Verhaal's. */
    static volatile Predicate<ServerPlayer> guhvatarKlaar = E2Verhaal::finaleKlaar;
    /** (tests) who a player is the guest of; the real answer is reizen's. */
    static volatile Function<ServerPlayer, ServerPlayer> gastheer = E2Reizen::gastheer;

    @Nullable
    private static GroteVerhalen.GrootVerhaal groot;

    private SpVerhaal() {
    }

    // =====================================================================================================================
    // the gate
    // =====================================================================================================================

    /** May the Sponsguh story begin for this player himself: the Guhvatar story done (the finale)? */
    public static boolean magBeginnen(ServerPlayer p) {
        return guhvatarKlaar.test(p);
    }

    /** May this player into Bikinibodem and Schelpenstad: the story may begin for them, or for the host they travel with. */
    public static boolean open(ServerPlayer p) {
        if (magBeginnen(p)) {
            return true;
        }
        ServerPlayer host = gastheer.apply(p);
        return host != null && host != p && magBeginnen(host);
    }

    // =====================================================================================================================
    // the lines
    // =====================================================================================================================

    /** The registered line of this id (one of {@link #LIJNEN}). */
    public static Verhaallijn lijn(String id) {
        return switch (id) {
            case RECEPT -> RECEPT_LIJN;
            case KROON -> KROON_LIJN;
            default -> throw new IllegalArgumentException("not a Sponsguh story line: " + id);
        };
    }

    /** A side line of the town (sp_zij registers them; null before that). */
    @Nullable
    public static Verhaallijn zijlijn(String id) {
        return nl.juiced.guhs.feature.verhaal.Verhaallijnen.van(id);
    }

    public static int stap(ServerPlayer p, String lijn) {
        return lijn(lijn).stap(p);
    }

    /** Is the line on for the player: not done, begun or open (the line before it done)? */
    public static boolean actief(ServerPlayer p, String lijn) {
        return E2Hervat.actief(p, lijn(lijn));
    }

    /** Has the player done part 1 / part 2 / the whole story? */
    public static boolean receptKlaar(ServerPlayer p) {
        return RECEPT_LIJN.klaar(p);
    }

    public static boolean klaar(ServerPlayer p) {
        return KROON_LIJN.klaar(p);
    }

    /** Does this player know the secret Knabbelburger recipe: they brought it back to Krabguh (step {@value #R_TERUG} passed)? */
    public static boolean kentRecept(Player p) {
        return p instanceof ServerPlayer sp && RECEPT_LIJN.stap(sp) > R_TERUG;
    }

    /**
     * The place where this player's Sponsguh story goes on (for Appa's gold row / "Verder", CONTRACT_E2_C 4.4):
     * {@value #SCHELPENSTAD} for the steps of part 2 that play there ({@link #K_RIT} after leaving town .. {@link #K_HASSELGUH}),
     * else {@value #BIKINIBODEM}; null when the gate is shut or no Sponsguh line is on.
     */
    @Nullable
    public static String reisDoel(ServerPlayer p) {
        if (!open(p)) {
            return null;
        }
        if (actief(p, RECEPT)) {
            return BIKINIBODEM;
        }
        if (actief(p, KROON)) {
            int s = stap(p, KROON);
            return s >= K_SLEUTEL && s <= K_HASSELGUH ? SCHELPENSTAD : BIKINIBODEM;
        }
        return null;
    }

    /** The big story of the Guhpad (null before {@link #register}). */
    @Nullable
    public static GroteVerhalen.GrootVerhaal grootVerhaal() {
        return groot;
    }

    // =====================================================================================================================
    // register
    // =====================================================================================================================

    /** From Einde2.register (common code, both sides), right after E2Verhaal.register(). */
    public static void register() {
        RECEPT_LIJN.id();
        KROON_LIJN.id();
        GroteVerhalen.zetGroep(GROEP, GroteVerhalen.Wereld.GUHEINDE);
        groot = GroteVerhalen.registreer(new GroteVerhalen.GrootVerhaal(GROOT, GroteVerhalen.Wereld.GUHEINDE, GroteVerhalen.Wereld.GUHEINDE,
                "guhs:knabbelburger", List.of(BIKINIBODEM), LIJNEN, () -> true, SpVerhaal::klaar));   // (a big story needs a place: GuhpadGameTests)
        for (String s : Einde2Ids.SPONS_STRUCTUREN) {
            Sluiers.registreer(s, SLUIER_RAND, SpVerhaal::open);
            Sluiers.BERICHTEN.put(s, P + "poort.balk");
        }
        Einde2Haken.KNABBELBURGER_RECEPT = SpVerhaal::kentRecept;
        // Appa's menu (and the meditation stones never: those list only the Guhvatar side places) know Bikinibodem as soon as
        // the gate is open, also before the first visit; Schelpenstad from part 2's road trip on
        E2Verhaal.EXTRA_GEBOUWEN.add(SpVerhaal::gebouwen);
        // "Verder met het verhaal": the Sponsguh lines come after the five chapters
        E2Hervat.EXTRA_GROEPEN.add(GROEP);
        E2Hervat.NA_HOOFDSTUKKEN.addAll(LIJNEN);
        SpStappen.registreer();
        NeoForge.EVENT_BUS.addListener(SpVerhaal::opTick);
        NeoForge.EVENT_BUS.addListener(SpVerhaal::opUitloggen);
        NeoForge.EVENT_BUS.addListener(SpCommando::register);
    }

    /** The extra rows of Appa's menu ({@link E2Verhaal#EXTRA_GEBOUWEN}): the Sponsguh places this player may fly to. */
    static List<String> gebouwen(ServerPlayer p) {
        List<String> uit = new ArrayList<>();
        if (!open(p)) {
            return uit;
        }
        uit.add(BIKINIBODEM);
        if (receptKlaar(p) && stap(p, KROON) >= K_RIT) {
            uit.add(SCHELPENSTAD);
        }
        return uit;
    }

    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().isClientSide()) {
            return;
        }
        if ((p.tickCount + p.getId()) % 100 == 13 && magBeginnen(p) && !RECEPT_LIJN.begonnen(p)) {
            RECEPT_LIJN.begin(p);                       // the gate opened: part 1 shows in the Guhdex from now on
        }
        if ((p.tickCount + p.getId()) % 20 == 5) {
            SpPoort.tick(p);
        }
        if ((p.tickCount + p.getId()) % 10 == 0) {
            SpStandIn.tick(p);
        }
    }

    private static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        SpPoort.vergeet(event.getEntity().getUUID());
        SpStandIn.vergeet(event.getEntity().getUUID());
    }
}
