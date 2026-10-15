package nl.juiced.guhs.feature.einde2.verhaal;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.verhaal.VerhaalSync;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B (CONTRACT_E2_B 3, 8): putting a player anywhere in the Guhvatar story. Dev commands and game tests only:
 * it writes main's own keys, so the lines, the veil and the Guhdex see exactly what a real playthrough would leave.
 */
public final class E2VerhaalDev {
    private E2VerhaalDev() {
    }

    /**
     * Opper-Mika won, every chapter before {@code lijn} done, {@code lijn} itself wiped and set to {@code stap}. A line that
     * is not a chapter of the story (a side quest, a Geestenwereld line) only gets the win and the wipe + step.
     */
    public static void zetTot(ServerPlayer p, String lijn, int stap) {
        if (GuhQuests.saved(p).getIntOr(GuheindeGevecht.WINS, 0) <= 0) {
            GuhQuests.saved(p).putInt(GuheindeGevecht.WINS, 1);
        }
        int nr = E2Verhaal.HOOFDSTUKKEN.indexOf(lijn);
        for (int i = 0; i < nr; i++) {
            klaar(p, E2Verhaal.HOOFDSTUKKEN.get(i));
        }
        wis(p, lijn);
        zet(p, lijn, stap);
    }

    /** Sets this line to the step (forward from 0: wipe first for a step back). */
    public static void zet(ServerPlayer p, String lijn, int stap) {
        Verhaallijn l = E2Verhaal.lijn(lijn);
        if (l != null) {
            l.zet(p, stap);
            return;
        }
        Integer n = E2Verhaal.STAND_IN.get(lijn);
        if (n != null && stap > 0) {
            GuhQuests.saved(p).putInt("guhs_" + lijn + "_stap", Math.min(n, stap));
        }
    }

    /** Finishes this line (all its steps). */
    public static void klaar(ServerPlayer p, String lijn) {
        Verhaallijn l = E2Verhaal.lijn(lijn);
        Integer n = l != null ? Integer.valueOf(l.stappen()) : E2Verhaal.STAND_IN.get(lijn);
        if (n != null) {
            zet(p, lijn, n);
        }
    }

    /** Sets or clears a flag of this line. */
    public static void vlag(ServerPlayer p, String lijn, String vlag, boolean aan) {
        Verhaallijn l = E2Verhaal.lijn(lijn);
        if (l != null) {
            l.vlag(p, vlag, aan);
            VerhaalSync.sync(p);
        } else if (aan) {
            GuhQuests.saved(p).putBoolean("guhs_" + lijn + "_v_" + vlag, true);
        } else {
            GuhQuests.saved(p).remove("guhs_" + lijn + "_v_" + vlag);
        }
    }

    /** Forgets everything this player did in this line. */
    public static void wis(ServerPlayer p, String lijn) {
        Verhaallijn l = E2Verhaal.lijn(lijn);
        if (l != null) {
            l.wis(p);
        } else {
            E2Verhaal.wisSleutels(p, lijn);
        }
    }
}
