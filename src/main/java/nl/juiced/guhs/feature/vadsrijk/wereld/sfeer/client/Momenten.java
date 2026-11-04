package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client;

import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.SfeerKlok;

/**
 * Client view of the moments: the day clock ({@link SfeerKlok}) or a forced moment (dev command {@code /guhs vadsrijk sfeer ...},
 * remembered as the game time it was forced at).
 */
final class Momenten {
    static long golfForce = Long.MIN_VALUE, stormForce = Long.MIN_VALUE, buiForce = Long.MIN_VALUE, noorderForce = Long.MIN_VALUE;
    /** This player beat the Opperzwam (server flag): the light pillar from the dome. */
    static volatile boolean opperzwam;
    private static final int GOLF_FORCE_DUUR = 20 * 60 * 5, NOORDER_FORCE_DUUR = 20 * 120;

    private Momenten() {
    }

    static void forceer(Level l, String wat) {
        long nu = l.getGameTime();
        switch (wat) {
            case "golf" -> golfForce = nu;
            case "storm" -> stormForce = nu;
            case "bui" -> buiForce = nu;
            case "noorderlicht" -> noorderForce = nu;
            default -> golfForce = stormForce = buiForce = noorderForce = Long.MIN_VALUE;
        }
    }

    static void wis() {
        golfForce = stormForce = buiForce = noorderForce = Long.MIN_VALUE;
    }

    private static long sinds(Level l, long force) {
        return force == Long.MIN_VALUE ? Long.MAX_VALUE : l.getGameTime() - force;
    }

    /** Ticks since tonight's light wave started, or -1 while the lights are off. */
    static long golfFase(Level l) {
        long f = sinds(l, golfForce);
        if (f >= 0 && f < GOLF_FORCE_DUUR) {
            return f;
        }
        return SfeerKlok.golfFase(l);
    }

    /** The dawn fade of the lights (1 = on). */
    static float golfLicht(Level l) {
        long f = sinds(l, golfForce);
        return f >= 0 && f < GOLF_FORCE_DUUR ? 1f : SfeerKlok.dageraadFactor(l);
    }

    static float storm(Level l) {
        return Math.max(SfeerKlok.storm(l), SfeerKlok.venster(sinds(l, stormForce), 0, SfeerKlok.STORM_DUUR, 160));
    }

    static float bui(Level l) {
        return Math.max(SfeerKlok.bui(l), SfeerKlok.venster(sinds(l, buiForce), 0, SfeerKlok.BUI_DUUR, 100));
    }

    static float regenboog(Level l) {
        return Math.max(SfeerKlok.regenboog(l), SfeerKlok.venster(sinds(l, buiForce), SfeerKlok.BUI_DUUR - 200, SfeerKlok.BOOG_DUUR, 200));
    }

    static float noorderlicht(Level l) {
        return Math.max(SfeerKlok.nacht(l), SfeerKlok.venster(sinds(l, noorderForce), 0, NOORDER_FORCE_DUUR, 60));
    }
}
