package nl.juiced.guhs.feature.vadsrijk.zone6.verhaal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;

/**
 * What the other slices may call or listen to (server side) about zone 6 "Het Zijdeland". Events a zone-6 quest waits for go
 * through the shared {@code NpcApi.meld(p, soort, waarde)}.
 * <ul>
 *   <li><b>u-z6raid</b> (De Sterrendovende Toren): the raid is open for a player once {@link #torenOpen} (= {@code zone6_klaar}, the
 *       end of z6_hq10). At the raid's end call {@link #ringelingOpgevrolijkt} for every player who should get the ending (the
 *       cheer-up scene, the Keizer's ring, the title "Vriend van Ringeling", then flash F4; once per player, later clears just set
 *       nothing new). A kill of an entity {@code guhs:vr_z6_*ringeling*} counts by itself too; u-z6raid calls
 *       {@code Z6Verhaal.ringelingOpgevrolijkt(p)} by reflection. The Spiegelvizier = <b>Meester Glans</b> (see {@link #SPIEGELVIZIER}).</li>
 *   <li><b>the dungeon</b> (De Verzonken Sterrenwacht, hq08): register a starter in {@link #KERKER} (true = "I took over"); at its
 *       end call {@link #kompasGevonden} (or kill {@code guhs:vr_z6_*sterrenguh*}). Without one a fallback scene gives the compass.</li>
 *   <li><b>u-z6wezens</b>: field boss Koningin Wormelien: kill/cheer up {@code guhs:vr_z6_*wormelien*} or {@code meld(p,
 *       "z6_baas", "wormelien")} (u-z6wezens' meld). The bultguh: {@code zone6.bultguh.Bultguh.geef(p, "naturel")}, else {@code Rijdieren.geef(p,
 *       "bultguh")} at Stalmeester Hobbel (waits with a flag until the species is in the tree).</li>
 *   <li><b>u-tapijt</b>: we call {@code tapijt.Tapijt.geef(p)} at the end of z6_hq06 (reflection) and give the looks with
 *       {@code Bron.api("z6_kwastjes")} / {@code Bron.api("z6_eerste_karavaan")}.</li>
 *   <li><b>u-z6wereld</b>: our POI ids in {@link Z6VerhaalPlekken} (a generated {@code zone6.wereld.Z6WereldPlekken.plek(id)} wins by
 *       reflection), campfire blocks at {@link Z6VerhaalPlekken#KAMPVUREN}, the five beacons at {@link Z6VerhaalPlekken#BAKEN}.</li>
 *   <li><b>the airship</b>: {@link Z6Lijn} adds line E at runtime (unless a line "z5_z6" exists already) and its gate.</li>
 * </ul>
 */
public final class Z6VerhaalApi {
    /** Fires once per player when zone 6's main story is done (z6_hq10: Moeder Bron, the sealed tower). */
    public static final List<Consumer<ServerPlayer>> ZONE6_KLAAR = new CopyOnWriteArrayList<>();
    /** Fires once per player when Ringeling is cheered up (after the raid). */
    public static final List<Consumer<ServerPlayer>> RINGELING = new CopyOnWriteArrayList<>();
    /** Dungeon starters for hq08 at the entrance of De Verzonken Sterrenwacht: return true when you took over. */
    public static final List<Predicate<ServerPlayer>> KERKER = new CopyOnWriteArrayList<>();

    /** The Spiegelvizier (aligned with u-z6raid): Meester Glans, a Mika illusionist and Ringeling's Knokkel, disguised as the palace's mirror polisher. */
    public static final String SPIEGELVIZIER = "Meester Glans", SPIEGELVIZIER_VERMOMMING = "Meester Glans, de spiegelpoetser";

    private Z6VerhaalApi() {
    }

    public static boolean zone6Klaar(ServerPlayer p) {
        return Questlog.vlag(p, Z6Verhaal.VLAG_ZONE6);
    }

    /** May this player enter the raid (the story of zone 6 is done)? */
    public static boolean torenOpen(ServerPlayer p) {
        return zone6Klaar(p);
    }

    /** Is airship line E open for this player (the great swim seen; the level is checked separately: {@link #lijnESlot})? */
    public static boolean lijnEOpen(ServerPlayer p) {
        return Questlog.vlag(p, Z6Verhaal.VLAG_LIJN_E);
    }

    /** Null when this player may fly line E to the Zijdeland now, else the lang key of why not (arg: the player's level). */
    public static String lijnESlot(ServerPlayer p) {
        return Z6Verhaal.slot(p);
    }

    /** Fly this player to Rozenkloof (the first time: the arrival film). */
    public static void naarZijdeland(ServerPlayer p) {
        Z6Verhaal.vlieg(p);
    }

    /** The raid's end: Ringeling is cheered up and joins (scene + F4 when the player is free; once). */
    public static void ringelingOpgevrolijkt(ServerPlayer p) {
        boolean nieuw = !Questlog.vlag(p, Z6Verhaal.VLAG_RINGELING);
        Z6Verhaal.ringeling(p);
        if (nieuw) {
            RINGELING.forEach(h -> h.accept(p));
        }
    }

    public static boolean ringelingVriend(ServerPlayer p) {
        return Questlog.vlag(p, Z6Verhaal.VLAG_RINGELING);
    }

    /** The dungeon's end (hq08): the Sterrenkompas. */
    public static void kompasGevonden(ServerPlayer p) {
        Z6Verhaal.kompas(p);
    }

    /** How many of the five star beacons burn for this player. */
    public static int bakens(ServerPlayer p) {
        return Z6Puzzels.aantal(p);
    }

    /** Has the Spiegelvizier been unmasked for this player? */
    public static boolean ontmaskerd(ServerPlayer p) {
        return Questlog.klaar(p, Z6Haken.HQ09) || Z6Haken.doelKlaar(p, Z6Haken.HQ09, 2);
    }

    /** Is Moeder Bron awake (cheered up in hq10)? */
    public static boolean bronWakker(ServerPlayer p) {
        return Questlog.klaar(p, Z6Haken.HQ10) || Z6Haken.doelKlaar(p, Z6Haken.HQ10, 4);
    }

    /** Starts the dungeon for hq08: a registered starter, a known class by reflection, else false (the fallback scene). */
    static boolean startKerker(ServerPlayer p) {
        for (Predicate<ServerPlayer> s : KERKER) {
            if (s.test(p)) {
                return true;
            }
        }
        return Z6Verhaal.reflectie(p, "sterrenwacht", "zone6.wezens.baas.Z6Bazen#start");   // (u-z6wezens: builds the dungeon, takes the party in)
    }
}
