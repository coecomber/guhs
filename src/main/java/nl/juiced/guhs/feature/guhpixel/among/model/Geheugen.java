package nl.juiced.guhs.feature.guhpixel.among.model;

import java.util.ArrayList;
import java.util.List;

/**
 * What one guh NPC remembers of a round: whom it saw where and with whom, how long it stood with somebody, what it saw
 * happen (a push, somebody in a vent, somebody beside a sleeper), where it was itself, and how much it suspects everybody.
 * Only things the NPC could really see get in here ({@link Ronde#ziet}); it is not omniscient.
 */
public final class Geheugen {
    private static final int SPOOR = 96;
    /** A sleeper is "fresh" this many ticks: whoever stands beside it then is suspect. */
    public static final int VERS = 100;

    public final int[] gezienTick;
    public final int[] gezienZone;
    /** Who else stood in that zone at that sighting (a bit per participant). */
    public final int[] gezienMet;
    /** Ticks in sight of each other lately (grows 10 per look, shrinks 5). */
    public final int[] samen;
    public final double[] verdenking;
    public final boolean[] zeker;
    /** Where the NPC itself was: one zone per look (every 10 ticks), a ring. */
    private final int[] spoor = new int[SPOOR];
    private int spoorI;

    public int zagDuwDoor = -1;
    public int zagLuikVan = -1;
    /** Who stood right beside a sleeper when this NPC found it. */
    public final List<Integer> bijSlaper = new ArrayList<>();
    /** Who was last seen alone with the sleeper, and where. */
    public int laatstMet = -1, laatstMetZone = -1;

    public Geheugen(int n) {
        gezienTick = new int[n];
        gezienZone = new int[n];
        gezienMet = new int[n];
        samen = new int[n];
        verdenking = new double[n];
        zeker = new boolean[n];
        java.util.Arrays.fill(gezienTick, -100000);
        java.util.Arrays.fill(gezienZone, -1);
        java.util.Arrays.fill(spoor, -1);
    }

    /** One look around (every 10 ticks). */
    public void kijk(Ronde r, Deelnemer ik) {
        spoor[spoorI++ % SPOOR] = ik.zone;
        int n = r.deelnemers.size();
        boolean[] zie = new boolean[n];
        for (Deelnemer o : r.deelnemers) {
            zie[o.idx] = r.ziet(ik, o);
        }
        for (Deelnemer o : r.deelnemers) {
            if (o == ik) {
                continue;
            }
            if (zie[o.idx]) {
                gezienTick[o.idx] = r.tick;
                gezienZone[o.idx] = o.zone;
                int met = 0;
                for (Deelnemer p : r.deelnemers) {
                    if (p != o && p != ik && zie[p.idx] && p.zone == o.zone) {
                        met |= 1 << p.idx;
                    }
                }
                gezienMet[o.idx] = met;
                samen[o.idx] = Math.min(600, samen[o.idx] + 10);
            } else {
                samen[o.idx] = Math.max(0, samen[o.idx] - 5);
            }
        }
    }

    public void zagDuw(int dader) {
        zeker[dader] = true;
        verdenking[dader] = 100;
        zagDuwDoor = dader;
    }

    public void zagLuik(int wie) {
        zeker[wie] = true;
        verdenking[wie] = 100;
        zagLuikVan = wie;
    }

    /** The NPC sees a sleeper for the first time: who stands right beside it? */
    public void zietSlaper(Ronde r, Deelnemer ik, Deelnemer slaper) {
        // only beside a sleeper that fell asleep a moment ago: later on, whoever stands there is just another finder
        boolean vers = r.tick - slaper.lichaamTick <= VERS;
        for (Deelnemer o : r.deelnemers) {
            if (vers && o != ik && o != slaper && r.ziet(ik, o) && Math.hypot(o.x - slaper.lichaamX, o.z - slaper.lichaamZ) <= 4.5) {
                if (!bijSlaper.contains(o.idx)) {
                    bijSlaper.add(o.idx);
                    verdenking[o.idx] += 2.0;
                }
            }
        }
        slaperBekend(r, ik, slaper);
    }

    /** A sleeper was found (by this NPC or told in a meeting): was it last seen alone with one other? */
    public void slaperBekend(Ronde r, Deelnemer ik, Deelnemer slaper) {
        if (laatstMet >= 0 || r.tick - gezienTick[slaper.idx] > 500) {
            return;
        }
        int met = gezienMet[slaper.idx];
        if (met != 0 && Integer.bitCount(met) == 1) {
            int wie = Integer.numberOfTrailingZeros(met);
            if (wie != ik.idx) {
                laatstMet = wie;
                laatstMetZone = gezienZone[slaper.idx];
                verdenking[wie] += 1.4;
            }
        }
    }

    /** The zone this NPC was in most during its last {@code kijken} looks (10 ticks each); -1 unknown. */
    public int waarWasIk(int kijken) {
        int[] tel = new int[64];
        int best = -1, bestN = 0;
        for (int k = 1; k <= Math.min(kijken, SPOOR); k++) {
            int z = spoor[Math.floorMod(spoorI - k, SPOOR)];
            if (z >= 0 && z < 64 && ++tel[z] > bestN) {
                bestN = tel[z];
                best = z;
            }
        }
        return best;
    }

    /** Was this NPC in that zone during its last {@code kijken} looks? */
    public boolean wasIn(int zone, int kijken) {
        for (int k = 1; k <= Math.min(kijken, SPOOR); k++) {
            if (spoor[Math.floorMod(spoorI - k, SPOOR)] == zone) {
                return true;
            }
        }
        return false;
    }

    /** After a meeting: what was not certain fades, the facts of this meeting are used up. */
    public void naVergadering() {
        for (int i = 0; i < verdenking.length; i++) {
            if (!zeker[i]) {
                verdenking[i] *= 0.8;
            }
        }
        bijSlaper.clear();
        laatstMet = laatstMetZone = -1;
        zagDuwDoor = zagLuikVan = -1;
        java.util.Arrays.fill(samen, 0);
    }
}
