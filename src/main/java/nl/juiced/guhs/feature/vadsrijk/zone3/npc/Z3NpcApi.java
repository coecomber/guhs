package nl.juiced.guhs.feature.vadsrijk.zone3.npc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;

/**
 * What the other zone-3 slices may call or listen to (server side). Events a zone-3 quest waits for go through the shared
 * {@code NpcApi.meld(p, soort, waarde)}:
 * <ul>
 *   <li>z3-bazen: {@code meld(p, "z3_taleggio" | "z3_brokkel" | "z3_roquefort" | "z3_piepvel" | "z3_wortelwurger" | "z3_bleu" | "z3_imker",
 *       "verslagen")}, {@code meld(p, "z3_nestwachter", "opgevrolijkt")}, {@code meld(p, "z3_vahoegnir", "opgevrolijkt")} (kills are
 *       also seen here via Haken.VERSLAGEN when the entity id is "vr_z3_..." and holds the boss name);</li>
 *   <li>z3-wereld (optional): {@code meld(p, "z3_lantaarn", "ring")}, {@code meld(p, "z3_ketel", "noord" | "moeras")},
 *       {@code meld(p, "z3_grotlantaarn", "aan")} when its own blocks do it (we also count them by walking up).</li>
 * </ul>
 * Bindings (NpcRollen) called when present: {@code z3_luchtlijn}, {@code z3_bladrovers}, {@code z3_vahoegnir}, {@code z3_ring_aan},
 * {@code z3_ketel_uit}, {@code z3_grotlantaarn}, {@code z3_honinglift(_open)}, {@code z3_imker}, {@code z3_terugkleur}, {@code premiebord}.
 */
public final class Z3NpcApi {
    public static final List<Consumer<ServerPlayer>> ZONE3_KLAAR = new CopyOnWriteArrayList<>();

    private Z3NpcApi() {
    }

    public static boolean zone3Klaar(ServerPlayer p) {
        return Questlog.vlag(p, Z3Verhaal.VLAG_ZONE3);
    }

    static void zetZone3Klaar(ServerPlayer p) {
        if (!zone3Klaar(p)) {
            Questlog.zetVlag(p, Z3Verhaal.VLAG_ZONE3, true);
            ZONE3_KLAAR.forEach(h -> h.accept(p));
        }
    }

    /** Does this player carry the Nestlantaarn (given halfway up the Lantaarntoren, z3_hq10)? */
    public static boolean nestlantaarn(ServerPlayer p) {
        return Questlog.vlag(p, "z3_nestlantaarn") || Questlog.klaar(p, Z3Haken.HQ10);
    }

    /** Is the Schorsstad lit for this player (all 7 rings of z3_hq10)? */
    public static boolean lantaarnsAan(ServerPlayer p) {
        return Questlog.klaar(p, Z3Haken.HQ10) || Z3Haken.doelKlaar(p, Z3Haken.HQ10, 0);
    }

    /** Has the Honinglift opened for this player (z3_hq09)? */
    public static boolean honinglift(ServerPlayer p) {
        return Questlog.vlag(p, "z3_honinglift") || Questlog.klaar(p, Z3Haken.HQ09);
    }
}
