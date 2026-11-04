package nl.juiced.guhs.feature.vadsrijk.zone2.npc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;

/**
 * What the other zone-2 slices may call or listen to (server side). Events a zone-2 quest waits for go through the shared
 * {@link NpcApi#meld}:
 * <ul>
 *   <li>{@code meld(p, "edelsteen", "ingezet")} (z2-systemen: a crystal went into a socket at the juweliersguh);</li>
 *   <li>{@code meld(p, "z2_boormeester", "verslagen")} (z2-bazen; we also see it ourselves through Haken.VERSLAGEN: an entity whose
 *       id contains "boormeester");</li>
 *   <li>{@code meld(p, "z2", "draak_geholpen")} (our own dragon scene).</li>
 * </ul>
 * Story state: {@link #zone2Klaar} / {@link #ZONE2_KLAAR} (the end of the zone-2 story); {@link #boormeesterMag} (all 5 clues found:
 * z2-bazen may open the hidden boorhal; we also call its {@code Boormeester.ontgrendel(p)} once, see {@link Z2Rollen}).
 */
public final class Z2NpcApi {
    public static final List<Consumer<ServerPlayer>> ZONE2_KLAAR = new CopyOnWriteArrayList<>();

    private Z2NpcApi() {
    }

    public static boolean zone2Klaar(ServerPlayer p) {
        return Questlog.vlag(p, Z2Verhaal.VLAG_ZONE2);
    }

    static void zetZone2Klaar(ServerPlayer p) {
        if (!zone2Klaar(p)) {
            Questlog.zetVlag(p, Z2Verhaal.VLAG_ZONE2, true);
            ZONE2_KLAAR.forEach(h -> h.accept(p));
        }
    }

    /** Has this player found all 5 clues to the Mika-boormeester? */
    public static boolean boormeesterMag(ServerPlayer p) {
        return Questlog.map(p, Z2Haken.AANWIJZINGEN).size() >= Z2NpcPlekken.AANWIJZING.size();
    }

    /** Is this petrified guh still crystal for this player (z2-wezens/wereld may show a crystal shell per player)? */
    public static boolean nogVersteend(ServerPlayer p, String npc) {
        return !Z2Haken.bevrijd(p, npc);
    }
}
