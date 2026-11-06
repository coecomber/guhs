package nl.juiced.guhs.feature.vadsrijk.zone4.verhaal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;

/**
 * What the other night-round-T slices may call or listen to (server side). Events a zone-4 quest waits for go through the shared
 * {@code NpcApi.meld(p, soort, waarde)}:
 * <ul>
 *   <li>bosses (t-z4wezens or a boss slice): {@code meld(p, "z4_zeeslurf", "opgevrolijkt")} (world boss in grotto 1 of the Koraalbaai),
 *       {@code meld(p, "z4_vliegervanger", "verslagen")} (Kootje, the small dungeon in the wreck). Kills of entity ids
 *       "vr_z4_*zeeslurf*" / "vr_z4_*vliegervanger*" / "vr_z4_*kootje*" are also seen here. Bind {@code z4_zeeslurf} /
 *       {@code z4_vliegervanger} (NpcRollen) to START your fight when the player arrives; without a binding the story has a short
 *       dialogue instead;</li>
 *   <li>t-ambacht (Vissen): {@code meld(p, "z4_vis", "gevangen")} per fish caught in zone 4 (vanilla rod fishing is counted here too);</li>
 *   <li>t-z4boot: bind {@code z4_zeilboot_geef} (give/unlock the boat, z4_hq02 step 1) and {@code z4_zeilboot} (the dock NPC's service);</li>
 *   <li>t-luchtschip: bind {@code z4_luchtlijn} (arg "lampionhaven": fly line C), {@code z4_cliffhanger} (the first real flight up
 *       to Hoogvads after z4_hq07); read {@link #luchtschipNaarHoogvads} or listen to {@link #ZONE4_KLAAR} to open line D;</li>
 *   <li>t-z4wereld / vfx (optional): bind {@code z4_wolkwal_zakt} (lower the whale for this player in the finale), {@code z4_feest}
 *       (lanterns on for the party).</li>
 * </ul>
 */
public final class Z4VerhaalApi {
    /** Called once per player when zone 4 is done (the cliffhanger at the Opstijgpier starts). */
    public static final List<Consumer<ServerPlayer>> ZONE4_KLAAR = new CopyOnWriteArrayList<>();

    private Z4VerhaalApi() {
    }

    public static boolean zone4Klaar(ServerPlayer p) {
        return Questlog.vlag(p, Z4Verhaal.VLAG_ZONE4);
    }

    /** Is the airship line Opstijgpier -> Hoogvads (line D) open for this player? */
    public static boolean luchtschipNaarHoogvads(ServerPlayer p) {
        return Questlog.vlag(p, Z4Verhaal.VLAG_Z5_LUCHTSCHIP);
    }

    /** Has this player got the sailboat from Botenguh Zeiltje (z4_hq02)? */
    public static boolean heeftZeilboot(ServerPlayer p) {
        return Questlog.vlag(p, "z4_zeilboot");
    }

    /** How many of the 12 Knuffeltij knuffels this player cheered up. */
    public static int knuffeltij(ServerPlayer p) {
        return Z4Haken.knuffeltijTeller(p);
    }

    static void zetZone4Klaar(ServerPlayer p) {
        Questlog.zetVlag(p, Z4Verhaal.VLAG_Z5_LUCHTSCHIP, true);
        if (!zone4Klaar(p)) {
            Questlog.zetVlag(p, Z4Verhaal.VLAG_ZONE4, true);
            ZONE4_KLAAR.forEach(h -> h.accept(p));
        }
    }
}
