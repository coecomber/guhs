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

    // --- 1.10.6: the long memory of Lastig (Balans.langGeheugen) ---
    /** The last {@value #LIJN} sightings of everybody (tick and zone, a ring per participant; a new entry when the zone changes). */
    static final int LIJN = 8;
    private final int[][] lijnTick, lijnZone;
    private final int[] lijnI;
    /** What this NPC saw for sure: 1 a push, 2 out of a vent (kept after the meeting, said again in every next one). */
    public final int[] zekerSoort;
    /** Caught lying (a room it cannot have been in, a push of this very NPC): stays suspect and is hardly believed. */
    public final boolean[] leugenaar;
    /** Sleepers whose innocence this NPC already used (a bit per participant). */
    private long onschuldVerwerkt;

    public Geheugen(int n) {
        gezienTick = new int[n];
        gezienZone = new int[n];
        gezienMet = new int[n];
        samen = new int[n];
        verdenking = new double[n];
        zeker = new boolean[n];
        lijnTick = new int[n][LIJN];
        lijnZone = new int[n][LIJN];
        lijnI = new int[n];
        zekerSoort = new int[n];
        leugenaar = new boolean[n];
        for (int[] l : lijnZone) {
            java.util.Arrays.fill(l, -1);
        }
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
                int vorige = lijnZone[o.idx][Math.floorMod(lijnI[o.idx] - 1, LIJN)];
                if (vorige != o.zone) {
                    lijnTick[o.idx][lijnI[o.idx] % LIJN] = r.tick;
                    lijnZone[o.idx][lijnI[o.idx] % LIJN] = o.zone;
                    lijnI[o.idx]++;
                }
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
        zekerSoort[dader] = 1;
    }

    public void zagLuik(int wie) {
        zeker[wie] = true;
        verdenking[wie] = 100;
        zagLuikVan = wie;
        if (zekerSoort[wie] == 0) {
            zekerSoort[wie] = 2;
        }
    }

    /** Was this participant seen in that zone during the last {@code ticks} (any of its last sightings)? */
    public boolean gezienIn(int wie, int zone, int nu, int ticks) {
        for (int k = 0; k < LIJN; k++) {
            if (lijnZone[wie][k] == zone && nu - lijnTick[wie][k] <= ticks) {
                return true;
            }
        }
        return false;
    }

    /** 1.10.6 (long memory): a sleeper was found: everybody seen in its room lately is a little suspect. */
    public void slaperTijdlijn(Ronde r, Deelnemer ik, Deelnemer slaper) {
        for (Deelnemer o : r.deelnemers) {
            if (o != ik && o != slaper && o.wakker && !zeker[o.idx] && gezienIn(o.idx, slaper.lichaamZone, r.tick, 500)) {
                verdenking[o.idx] += 0.5;
            }
        }
    }

    /** A statement of this speaker turned out to be a lie (long memory: it stays a liar). */
    public void betrapt(int wie, boolean lang) {
        if (lang) {
            leugenaar[wie] = true;
        }
    }

    /**
     * 1.10.6 (long memory), at the start of a meeting: every guh that was pushed asleep is innocent, so whoever accused it in
     * an earlier meeting (the shared logbook of the round) becomes suspect, and whom it accused itself becomes a little suspect.
     */
    public void onschuldigen(Ronde r, Deelnemer ik) {
        for (Deelnemer o : r.deelnemers) {
            if (o.idx >= 64 || o.wakker || o.weg || o.weggestemd || o.mika() || (onschuldVerwerkt & (1L << o.idx)) != 0) {
                continue;
            }
            onschuldVerwerkt |= 1L << o.idx;
            for (Deelnemer s : r.deelnemers) {
                if (s != ik && s != o && s.wakker && (r.logBeschuldigt[s.idx] & (1L << o.idx)) != 0 && !zeker[s.idx]) {
                    verdenking[s.idx] += 0.8;
                }
            }
            for (Deelnemer x : r.deelnemers) {
                if (x != ik && x.wakker && x.idx < 64 && (r.logBeschuldigt[o.idx] & (1L << x.idx)) != 0 && !zeker[x.idx]) {
                    verdenking[x.idx] += 0.5;
                }
            }
        }
    }

    /**
     * 1.10.6 (long memory), the result of a vote: an innocent guh voted out makes its accusers suspect (and what it said
     * itself believable); a Mika voted out makes whoever vouched for it suspect and its accusers trusted.
     */
    public void leerVanUitslag(Ronde r, Deelnemer ik, int weg, boolean wegMika) {
        if (weg < 0 || weg >= 64) {
            return;
        }
        long bit = 1L << weg;
        for (Deelnemer s : r.deelnemers) {
            if (s == ik || s.idx == weg || !s.wakker || s.idx >= 64 || zeker[s.idx]) {
                continue;
            }
            long sBit = 1L << s.idx;
            boolean beschuldigde = (r.logBeschuldigt[s.idx] & bit) != 0, stondIn = (r.logInstaan[s.idx] & bit) != 0;
            if (wegMika) {
                if (stondIn) {
                    verdenking[s.idx] += 2.0;
                }
                if (beschuldigde) {
                    verdenking[s.idx] -= 1.0;
                }
                if ((r.logInstaan[weg] & sBit) != 0) {
                    verdenking[s.idx] += 0.8;      // the Mika vouched for it: a partner?
                }
                if ((r.logBeschuldigt[weg] & sBit) != 0) {
                    verdenking[s.idx] -= 0.6;
                }
            } else {
                if (beschuldigde) {
                    verdenking[s.idx] += 1.2;
                }
                if ((r.logBeschuldigt[weg] & sBit) != 0) {
                    verdenking[s.idx] += 0.5;      // an innocent guh's words weigh a bit more now
                }
                if ((r.logInstaan[weg] & sBit) != 0) {
                    verdenking[s.idx] -= 0.6;
                }
            }
        }
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
    public void naVergadering(double verval) {
        for (int i = 0; i < verdenking.length; i++) {
            if (!zeker[i] && !leugenaar[i]) {
                verdenking[i] *= verval;
            }
        }
        bijSlaper.clear();
        laatstMet = laatstMetZone = -1;
        zagDuwDoor = zagLuikVan = -1;
        java.util.Arrays.fill(samen, 0);
    }
}
