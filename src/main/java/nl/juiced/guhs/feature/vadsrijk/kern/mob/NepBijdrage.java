package nl.juiced.guhs.feature.vadsrijk.kern.mob;

import java.util.HashMap;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * W-PARTY: a nep-speler companion's hit counts as help from the real player it runs with (its owner), so that player gets the
 * normal xp + personal loot when the companion lands the last hit. The nep-speler itself never gets anything. (In this package
 * for access to {@link VadsrijkMob}'s protected contribution maps; no change to VadsrijkMob itself.)
 */
public final class NepBijdrage {
    private NepBijdrage() {
    }

    /** Call BEFORE the companion's damage goes through (a killing hit reads the helpers at once). */
    public static void namens(VadsrijkMob m, ServerPlayer eigenaar, double n) {
        if (m.geplofd() || n <= 0) {
            return;
        }
        m.bijdrage.merge(eigenaar.getUUID(), n, Double::sum);
        m.klasseBijdrage.computeIfAbsent(eigenaar.getUUID(), x -> new HashMap<>()).merge(Spelers.data(eigenaar).klasse, n, Double::sum);
    }
}
