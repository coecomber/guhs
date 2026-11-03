package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A tiny server-side planner for the klassen slice: run something a few ticks from now (waves of Pluizenregen, the echo of
 * the Dubbelklap, cooldown/energy tweaks right after kern paid for a skill, the ceremony's light beams). Not saved: everything
 * planned is a few seconds long at most.
 */
public final class Later {
    private record Taak(int[] over, Runnable wat) {
    }

    private static final List<Taak> TAKEN = new ArrayList<>();
    private static final List<Taak> NIEUW = new ArrayList<>();

    private Later() {
    }

    /** Runs {@code wat} after {@code ticks} server ticks (0 = at the end of this tick). */
    public static void na(int ticks, Runnable wat) {
        NIEUW.add(new Taak(new int[] {Math.max(0, ticks)}, wat));
    }

    static void tick() {
        TAKEN.addAll(NIEUW);
        NIEUW.clear();
        Iterator<Taak> it = TAKEN.iterator();
        while (it.hasNext()) {
            Taak t = it.next();
            if (t.over()[0]-- <= 0) {
                it.remove();
                try {
                    t.wat().run();
                } catch (RuntimeException e) {
                    com.mojang.logging.LogUtils.getLogger().warn("Vadsrijk klassen: a planned effect failed", e);
                }
            }
        }
    }

    static void wis() {
        TAKEN.clear();
        NIEUW.clear();
    }
}
