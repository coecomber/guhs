package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.server.MinecraftServer;

/** A tiny server-side "do this in N ticks" list (scenes that end in a teleport, door steps). Ticked by {@link WereldRegistry}. */
public final class Later {
    private record Taak(long op, Runnable wat) {
    }

    private static final List<Taak> TAKEN = new ArrayList<>();
    private static long nu;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private Later() {
    }

    public static synchronized void na(int ticks, Runnable wat) {
        TAKEN.add(new Taak(nu + Math.max(1, ticks), wat));
    }

    static void tick(MinecraftServer server) {
        List<Runnable> klaar = new ArrayList<>();
        synchronized (Later.class) {
            nu++;
            for (Iterator<Taak> it = TAKEN.iterator(); it.hasNext(); ) {
                Taak t = it.next();
                if (t.op() <= nu) {
                    klaar.add(t.wat());
                    it.remove();
                }
            }
        }
        for (Runnable r : klaar) {
            try {
                r.run();
            } catch (RuntimeException e) {
                LOGGER.warn("Vadsrijk wereld: uitgestelde taak faalde", e);
            }
        }
    }

    static synchronized void wis() {
        TAKEN.clear();
    }
}
