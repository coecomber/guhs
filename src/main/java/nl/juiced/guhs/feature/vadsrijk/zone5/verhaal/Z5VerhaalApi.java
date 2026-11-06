package nl.juiced.guhs.feature.vadsrijk.zone5.verhaal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;

/**
 * What the other slices may call or listen to (server side) about zone 5 "Hoogvads". Events a zone-5 quest waits for go through
 * the shared {@code NpcApi.meld(p, soort, waarde)}:
 * <ul>
 *   <li>t-z4verhaal (zone 4's end): set the flag {@code zone4_klaar} ({@code Questlog.zetVlag(p, "zone4_klaar", true)}) and fly
 *       the player up from the Opstijgpier with {@link #naarHoogvads} (or the quest action {@code "z5_vlieg"}). The first flight
 *       plays the arrival film and starts z5_hq01.</li>
 *   <li>t-z5wezens (bosses): {@link Z5Finale} (the hook interface for the finale fight), or {@code meld(p, "z5_zilverstaart",
 *       "verslagen")} + {@code meld(p, "z5_droom", "opgevrolijkt")}.</li>
 *   <li>t-z5wereld (the city): our POI ids in {@link Z5VerhaalPlekken} (generated {@code zone5.wereld.Z5WereldPlekken.plek(id)} wins
 *       by reflection), campfire blocks at {@link Z5VerhaalPlekken#KAMPVUREN}, vitrines next to the two teasers.</li>
 *   <li>t-luchtschip: binding {@code z5_luchtlijn} (arg "hoogvads") for the regular flight after the first.</li>
 *   <li>the professions slice: bindings {@code ambacht_gilde} and {@code ambacht_<koken|brouwen|smeden|naaien|imkeren>} for the
 *       guild's master teachers (until then a "binnenkort" line).</li>
 * </ul>
 */
public final class Z5VerhaalApi {
    /** Fires once per player when zone 5's main story is done (z5_hq09: Moeder Wolkwal awake). */
    public static final List<Consumer<ServerPlayer>> ZONE5_KLAAR = new CopyOnWriteArrayList<>();

    private Z5VerhaalApi() {
    }

    public static boolean zone5Klaar(ServerPlayer p) {
        return Questlog.vlag(p, Z5Verhaal.VLAG_ZONE5);
    }

    /** Fly this player up to Hoogvads (the first time: the arrival film + z5_hq01). */
    public static void naarHoogvads(ServerPlayer p) {
        Z5Verhaal.vlieg(p);
    }

    /** Has the spy been unmasked for this player (z5_hq07 done or its unmasking counted)? */
    public static boolean ontmaskerd(ServerPlayer p) {
        return Questlog.klaar(p, Z5Haken.HQ07) || Z5Haken.doelKlaar(p, Z5Haken.HQ07, 1);
    }
}
