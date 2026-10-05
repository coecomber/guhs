package nl.juiced.guhs.feature.guhpixel.among.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * One meeting: first talking ({@link Stap#BESPREKEN}), then voting ({@link Stap#STEMMEN}; statements still count), then
 * the result ({@link Stap#UITSLAG}).
 * <p>
 * The statement system: every statement ({@link Uitspraak}) is weighed by every crew NPC ({@link #weeg}) against what
 * that NPC saw itself. A claim that fits makes the accused more suspect, a claim the listener knows to be wrong makes the
 * SPEAKER suspect. Crew NPCs say what they really saw (sometimes forgetting it, sometimes mixing somebody up); an NPC
 * Mika lies plausibly (a room it really was in before, blame on whoever stood nearest or is accused already). A player
 * picks the same ready-made statements, true or not, and is weighed exactly like an NPC.
 */
public final class Vergadering {
    public enum Stap { BESPREKEN, STEMMEN, UITSLAG }

    public static final int NIET_GESTEMD = -2, OVERSLAAN = -1;

    private final Ronde r;
    private final Random rng;
    public final int oproeper;
    /** The sleeper that was reported (-1: the button) and the zone it lay in. */
    public final int slaper, slaperZone;
    public Stap stap = Stap.BESPREKEN;
    public int teller;
    public final List<Uitspraak> uitspraken = new ArrayList<>();
    public final int[] stemmen;
    /** The zone each participant says it was in (-1: said nothing). */
    public final int[] claim;
    public final int[] beschuldigd;
    public int weg = -1;
    public boolean wegMika, gelijk;
    private int uitslagStart;

    /** Where everybody really was when the meeting was called. */
    private final int[] waar;
    private final int startTick;
    private final int[] spreek1, spreek2, stemTick;
    private final int[] klopNiet;
    private final boolean[] beschuldigdDoorMij;

    Vergadering(Ronde r, int oproeper, int slaper, int[] waar) {
        this.r = r;
        this.rng = r.rng;
        this.oproeper = oproeper;
        this.slaper = slaper;
        this.slaperZone = slaper < 0 ? -1 : r.d(slaper).lichaamZone;
        this.waar = waar;
        this.startTick = r.tick;
        int n = r.deelnemers.size();
        stemmen = new int[n];
        claim = new int[n];
        beschuldigd = new int[n];
        spreek1 = new int[n];
        spreek2 = new int[n];
        stemTick = new int[n];
        klopNiet = new int[n];
        beschuldigdDoorMij = new boolean[n * n];
        Arrays.fill(stemmen, NIET_GESTEMD);
        Arrays.fill(claim, -1);
        Arrays.fill(klopNiet, -1);
        Balans b = r.balans;
        for (Deelnemer d : r.deelnemers) {
            spreek1[d.idx] = 40 + rng.nextInt(Math.max(1, b.bespreekTijd - 80));
            // (the reactions and the votes fall in the first part of the voting time)
            spreek2[d.idx] = b.bespreekTijd + 20 + rng.nextInt(Math.max(1, Math.min(160, b.stemTijd / 4)));
            stemTick[d.idx] = spreek2[d.idx] + 20 + rng.nextInt(Math.max(1, Math.min(140, b.stemTijd / 4)));
            if (d.npc && d.wakker && !d.mika()) {
                // a little prejudice (and red is always a bit sus)
                Geheugen g = d.brein.geheugen;
                for (Deelnemer o : r.deelnemers) {
                    if (o != d && !g.zeker[o.idx]) {
                        g.verdenking[o.idx] += rng.nextDouble() * b.ruis + (o.kleur == Kleur.ROOD ? 0.15 : 0);
                    }
                }
                if (slaper >= 0) {
                    g.slaperBekend(r, d, r.d(slaper));
                }
            }
        }
        zeg(new Uitspraak(oproeper, slaper >= 0 ? Uitspraak.Soort.GEVONDEN : Uitspraak.Soort.KNOP, slaper, slaperZone, rng.nextInt(3), 0));
    }

    public int stemTijdOver() {
        return stap == Stap.STEMMEN ? r.balans.bespreekTijd + r.balans.stemTijd - teller : stap == Stap.BESPREKEN ? r.balans.bespreekTijd - teller : 0;
    }

    void tick() {
        teller++;
        Balans b = r.balans;
        if (stap == Stap.BESPREKEN) {
            for (Deelnemer d : r.deelnemers) {
                if (d.npc && d.wakker && spreek1[d.idx] == teller) {
                    eerste(d);
                }
            }
            if (teller >= b.bespreekTijd) {
                stap = Stap.STEMMEN;
                r.meld(Gebeurtenis.Soort.STEMMEN, 0, 0, 0);
            }
        } else if (stap == Stap.STEMMEN) {
            boolean allen = true;
            for (Deelnemer d : r.deelnemers) {
                if (!d.wakker || d.weg) {
                    continue;
                }
                if (d.npc && spreek2[d.idx] == teller) {
                    tweede(d);
                }
                if (d.npc && stemTick[d.idx] == teller) {
                    stem(d.idx, d.mika() ? mikaStem(d) : crewStem(d));
                }
                allen &= stemmen[d.idx] != NIET_GESTEMD;
            }
            if ((allen && teller >= b.bespreekTijd + 60) || teller >= b.bespreekTijd + b.stemTijd) {
                uitslag();
            }
        } else if (teller >= uitslagStart + b.uitslagTijd) {
            r.naVergadering();
        }
    }

    /** A vote (target, {@link #OVERSLAAN}); false when it does not count. */
    public boolean stem(int wie, int doel) {
        Deelnemer d = r.d(wie);
        if (stap != Stap.STEMMEN || !d.wakker || d.weg || stemmen[wie] != NIET_GESTEMD) {
            return false;
        }
        if (doel != OVERSLAAN && (doel < 0 || doel >= stemmen.length || !r.d(doel).wakker || r.d(doel).weg)) {
            return false;
        }
        stemmen[wie] = doel;
        r.meld(Gebeurtenis.Soort.STEM, wie, 0, 0);
        return true;
    }

    /** A statement of a player; false when it is not allowed. */
    public boolean zegSpeler(int wie, Uitspraak.Soort soort, int over, int zone) {
        Deelnemer d = r.d(wie);
        if (stap == Stap.UITSLAG || !d.wakker || d.weg || !soort.kiesbaar() || d.uitspraken >= r.balans.maxUitspraken) {
            return false;
        }
        if (soort.overIemand() && (over < 0 || over >= stemmen.length || over == wie || r.d(over).weg)) {
            return false;
        }
        if (soort.metZone() && (zone < 0 || zone >= r.schip.zones.size())) {
            return false;
        }
        d.uitspraken++;
        zeg(new Uitspraak(wie, soort, soort.overIemand() ? over : -1, soort.metZone() ? zone : -1, rng.nextInt(3), teller));
        return true;
    }

    private void zeg(Uitspraak u) {
        uitspraken.add(u);
        int s = u.spreker(), x = u.wie();
        switch (u.soort()) {
            case WAAR_IK -> claim[s] = u.zone();
            case DUW, LUIK, BIJ_SLAPER, VERDENK, KLOPT_NIET -> {
                if (x >= 0 && !beschuldigdDoorMij[s * stemmen.length + x]) {
                    beschuldigdDoorMij[s * stemmen.length + x] = true;
                    beschuldigd[x]++;
                }
            }
            default -> {
            }
        }
        for (Deelnemer l : r.deelnemers) {
            if (l.npc && l.wakker && !l.mika() && l.idx != s) {
                weeg(l, u);
            }
        }
        r.meld(Gebeurtenis.Soort.UITSPRAAK, uitspraken.size() - 1, 0, 0);
    }

    /** The ticks even a running player needs between the middles of two zones (minus a little slack). */
    private int reistijd(int zoneA, int zoneB) {
        Schip.Zone a = r.schip.zone(zoneA), b = r.schip.zone(zoneB);
        return (int) (Math.hypot(a.middenX() - b.middenX(), a.middenZ() - b.middenZ()) / 0.3) - 20;
    }

    private boolean zelfdeOfNaast(int a, int b) {
        return a == b || r.schip.naast(a, b);
    }

    /** The listener was with this participant right before the meeting. */
    private boolean alibi(Geheugen g, int wie) {
        return startTick - g.gezienTick[wie] <= 200 && g.samen[wie] >= 150;
    }

    /** How a crew NPC takes a statement. */
    void weeg(Deelnemer luisteraar, Uitspraak u) {
        Geheugen g = luisteraar.brein.geheugen;
        int s = u.spreker(), x = u.wie(), ik = luisteraar.idx;
        double geloof = g.zeker[s] ? 0.1 : Math.max(0.25, Math.min(1.0, 1.0 - 0.25 * Math.max(0, g.verdenking[s])));
        switch (u.soort()) {
            case DUW, LUIK -> {
                if (x == ik) {
                    g.verdenking[s] += 4.0;
                } else if (!g.zeker[x]) {
                    if (alibi(g, x)) {
                        g.verdenking[s] += 2.0;
                    } else {
                        g.verdenking[x] += 3.0 * geloof;
                    }
                }
            }
            case BIJ_SLAPER -> {
                if (x == ik) {
                    g.verdenking[s] += 0.8;
                } else if (alibi(g, x)) {
                    g.verdenking[s] += 1.0;
                } else {
                    g.verdenking[x] += 1.6 * geloof;
                }
            }
            case GEZIEN -> {
                if (x != ik && x >= 0 && slaper >= 0 && zelfdeOfNaast(u.zone(), slaperZone) && !alibi(g, x)) {
                    g.verdenking[x] += (u.zone() == slaperZone ? 1.0 : 0.3) * geloof;
                }
            }
            case WAAR_IK -> {
                // "I was in <zone> when the meeting was called": wrong when this listener saw the speaker so short ago and so far
                // from there that nobody could have walked it
                int zone = u.zone(), gezien = g.gezienZone[s];
                if (gezien >= 0 && gezien != zone && startTick - g.gezienTick[s] < reistijd(gezien, zone)) {
                    g.verdenking[s] += 2.5;
                    klopNiet[ik] = s;
                }
            }
            case STA_IN -> {
                if (x != ik && x >= 0 && !g.zeker[x]) {
                    g.verdenking[x] -= 1.2 * geloof;
                }
            }
            case VERDENK -> {
                if (x == ik) {
                    g.verdenking[s] += 0.8;
                } else if (x >= 0 && !alibi(g, x)) {
                    g.verdenking[x] += 0.7 * geloof;
                }
            }
            case KLOPT_NIET -> {
                if (x == ik) {
                    g.verdenking[s] += 1.5;
                } else if (x >= 0) {
                    g.verdenking[x] += 1.5 * geloof;
                }
            }
            case SUS -> {
                if (x != ik && x >= 0) {
                    g.verdenking[x] += 0.15;
                }
            }
            default -> {
            }
        }
    }

    private boolean geldig(int wie) {
        return wie >= 0 && r.d(wie).wakker && !r.d(wie).weg;
    }

    /** Sometimes an NPC mixes somebody up with another participant it saw lately. */
    private int misschienVergist(Deelnemer d, int wie) {
        if (rng.nextDouble() >= r.balans.vergisKans) {
            return wie;
        }
        Geheugen g = d.brein.geheugen;
        List<Integer> kan = new ArrayList<>();
        for (Deelnemer o : r.deelnemers) {
            if (o != d && o.idx != wie && geldig(o.idx) && startTick - g.gezienTick[o.idx] <= 600) {
                kan.add(o.idx);
            }
        }
        return kan.isEmpty() ? wie : kan.get(rng.nextInt(kan.size()));
    }

    /** The first statement of an NPC. */
    private void eerste(Deelnemer d) {
        if (d.mika()) {
            mikaEerste(d);
            return;
        }
        Geheugen g = d.brein.geheugen;
        Balans b = r.balans;
        int v = rng.nextInt(3);
        if (g.zagDuwDoor >= 0 && rng.nextDouble() >= b.vergeetKans * 0.3) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.DUW, g.zagDuwDoor, -1, v, teller));
        } else if (g.zagLuikVan >= 0 && rng.nextDouble() >= b.vergeetKans * 0.3) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.LUIK, g.zagLuikVan, -1, v, teller));
        } else if (slaper >= 0 && !g.bijSlaper.isEmpty() && rng.nextDouble() >= b.vergeetKans && d.idx != oproeper) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.BIJ_SLAPER, misschienVergist(d, g.bijSlaper.get(0)), slaperZone, v, teller));
        } else if (slaper >= 0 && g.laatstMet >= 0 && rng.nextDouble() >= b.vergeetKans) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.GEZIEN, misschienVergist(d, g.laatstMet), g.laatstMetZone, v, teller));
        } else {
            double k = rng.nextDouble();
            int vriend = -1;
            for (Deelnemer o : r.deelnemers) {
                if (o != d && geldig(o.idx) && alibi(g, o.idx) && (vriend < 0 || g.samen[o.idx] > g.samen[vriend])) {
                    vriend = o.idx;
                }
            }
            if (k < 0.45 && waar[d.idx] >= 0) {
                zeg(new Uitspraak(d.idx, Uitspraak.Soort.WAAR_IK, -1, waar[d.idx], v, teller));
            } else if (k < 0.70 && vriend >= 0) {
                zeg(new Uitspraak(d.idx, Uitspraak.Soort.STA_IN, vriend, -1, v, teller));
            } else if (k < 0.90) {
                List<Integer> gezien = new ArrayList<>();
                for (Deelnemer o : r.deelnemers) {
                    if (o != d && geldig(o.idx) && startTick - g.gezienTick[o.idx] <= 400 && g.gezienZone[o.idx] >= 0) {
                        gezien.add(o.idx);
                    }
                }
                if (gezien.isEmpty()) {
                    zeg(new Uitspraak(d.idx, Uitspraak.Soort.NIKS, -1, -1, v, teller));
                } else {
                    int wie = gezien.get(rng.nextInt(gezien.size()));
                    zeg(new Uitspraak(d.idx, Uitspraak.Soort.GEZIEN, misschienVergist(d, wie), g.gezienZone[wie], v, teller));
                }
            } else if (k < 0.95) {
                zeg(new Uitspraak(d.idx, Uitspraak.Soort.NIKS, -1, -1, v, teller));
            } else {
                int rood = -1;
                for (Deelnemer o : r.deelnemers) {
                    if (o != d && o.kleur == Kleur.ROOD && geldig(o.idx)) {
                        rood = o.idx;
                    }
                }
                zeg(rood >= 0 ? new Uitspraak(d.idx, Uitspraak.Soort.SUS, rood, -1, v, teller) : new Uitspraak(d.idx, Uitspraak.Soort.NIKS, -1, -1, v, teller));
            }
        }
    }

    /** The second statement of an NPC: a reaction to what was said. */
    private void tweede(Deelnemer d) {
        if (d.mika()) {
            mikaTweede(d);
            return;
        }
        Geheugen g = d.brein.geheugen;
        int v = rng.nextInt(3);
        if (klopNiet[d.idx] >= 0 && geldig(klopNiet[d.idx])) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.KLOPT_NIET, klopNiet[d.idx], g.gezienZone[klopNiet[d.idx]], v, teller));
            return;
        }
        int best = meestVerdacht(d);
        if (best >= 0 && g.verdenking[best] >= r.balans.twijfelDrempel && !beschuldigdDoorMij[d.idx * stemmen.length + best] && rng.nextDouble() < 0.6) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.VERDENK, best, -1, v, teller));
        } else if (claim[d.idx] < 0 && waar[d.idx] >= 0 && rng.nextDouble() < 0.5) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.WAAR_IK, -1, waar[d.idx], v, teller));
        }
    }

    private int meestVerdacht(Deelnemer d) {
        Geheugen g = d.brein.geheugen;
        int best = -1;
        for (Deelnemer o : r.deelnemers) {
            if (o != d && geldig(o.idx) && (best < 0 || g.verdenking[o.idx] > g.verdenking[best])) {
                best = o.idx;
            }
        }
        return best;
    }

    private int crewStem(Deelnemer d) {
        Geheugen g = d.brein.geheugen;
        int best = meestVerdacht(d);
        if (best < 0) {
            return OVERSLAAN;
        }
        double v = g.verdenking[best];
        // with few guhs left awake the crew dares to vote on less
        int wakker = r.wakker(Deelnemer.Rol.CREW) + r.wakker(Deelnemer.Rol.MIKA);
        double moed = Math.max(0.55, Math.min(1.0, 0.55 + 0.45 * (wakker - 4) / 4.0));
        if (g.zeker[best] || v >= r.balans.stemDrempel * moed || (v >= r.balans.twijfelDrempel * moed && rng.nextDouble() < 0.5)) {
            return best;
        }
        return OVERSLAAN;
    }

    /** Who accused this participant first (-1: nobody). */
    private int aanklager(int wie) {
        for (Uitspraak u : uitspraken) {
            if (u.wie() == wie && u.spreker() != wie && (u.soort() == Uitspraak.Soort.DUW || u.soort() == Uitspraak.Soort.LUIK
                    || u.soort() == Uitspraak.Soort.BIJ_SLAPER || u.soort() == Uitspraak.Soort.VERDENK || u.soort() == Uitspraak.Soort.KLOPT_NIET)) {
                return u.spreker();
            }
        }
        return -1;
    }

    /** The room an NPC Mika says it was in: one it really visited lately, away from the sleeper; the truth when somebody saw it. */
    private int nepZone(Deelnemer d) {
        Geheugen g = d.brein.geheugen;
        for (Deelnemer o : r.deelnemers) {
            if (o != d && geldig(o.idx) && !o.mika() && startTick - g.gezienTick[o.idx] <= 120) {
                return waar[d.idx];       // somebody was there: stay close to the truth
            }
        }
        List<Integer> kan = new ArrayList<>();
        for (Schip.Zone z : r.schip.kamers()) {
            if (z.idx() != slaperZone && !r.schip.naast(z.idx(), slaperZone) && g.wasIn(z.idx(), 90)) {
                kan.add(z.idx());
            }
        }
        if (kan.isEmpty()) {
            for (Schip.Zone z : r.schip.kamers()) {
                if (z.idx() != slaperZone) {
                    kan.add(z.idx());
                }
            }
        }
        return kan.get(rng.nextInt(kan.size()));
    }

    private void mikaEerste(Deelnemer d) {
        int v = rng.nextInt(3);
        int aanklager = aanklager(d.idx);
        if (aanklager >= 0 && geldig(aanklager)) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.VERDENK, aanklager, -1, v, teller));
        } else if (rng.nextDouble() < 0.75) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.WAAR_IK, -1, nepZone(d), v, teller));
        } else {
            List<Integer> crew = new ArrayList<>();
            for (Deelnemer o : r.deelnemers) {
                if (o != d && geldig(o.idx) && !o.mika()) {
                    crew.add(o.idx);
                }
            }
            if (!crew.isEmpty()) {
                zeg(new Uitspraak(d.idx, Uitspraak.Soort.STA_IN, crew.get(rng.nextInt(crew.size())), -1, v, teller));
            }
        }
    }

    /** The crew guh the Mikas would like to see voted out: the most accused one. */
    private int zondebok(Deelnemer d) {
        int best = -1;
        for (Deelnemer o : r.deelnemers) {
            if (o != d && geldig(o.idx) && !o.mika() && beschuldigd[o.idx] > 0 && (best < 0 || beschuldigd[o.idx] > beschuldigd[best])) {
                best = o.idx;
            }
        }
        return best;
    }

    private void mikaTweede(Deelnemer d) {
        int v = rng.nextInt(3);
        int aanklager = aanklager(d.idx);
        if (aanklager >= 0 && geldig(aanklager) && !beschuldigdDoorMij[d.idx * stemmen.length + aanklager]) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.VERDENK, aanklager, -1, v, teller));
            return;
        }
        if (rng.nextDouble() >= r.balans.beschuldigKans) {
            return;
        }
        int bok = zondebok(d);
        if (bok >= 0) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.VERDENK, bok, -1, v, teller));
            return;
        }
        if (slaper >= 0) {
            // whoever really was nearest to the sleeper
            for (Deelnemer o : r.deelnemers) {
                if (o != d && geldig(o.idx) && !o.mika() && zelfdeOfNaast(waar[o.idx], slaperZone)) {
                    zeg(new Uitspraak(d.idx, Uitspraak.Soort.GEZIEN, o.idx, slaperZone, v, teller));
                    return;
                }
            }
        }
        if (geldig(oproeper) && !r.d(oproeper).mika() && oproeper != d.idx && rng.nextDouble() < 0.4) {
            zeg(new Uitspraak(d.idx, Uitspraak.Soort.VERDENK, oproeper, -1, v, teller));
        }
    }

    private int mikaStem(Deelnemer d) {
        // only along with the crew: a Mika never starts a vote on its own, that would stand out
        int bok = zondebok(d);
        if (bok < 0) {
            return OVERSLAAN;
        }
        int crew = 0;
        for (Deelnemer o : r.deelnemers) {
            if (!o.mika() && o.idx != bok && beschuldigdDoorMij[o.idx * stemmen.length + bok]) {
                crew++;
            }
        }
        return crew >= r.balans.mikaStemtMee ? bok : OVERSLAAN;
    }

    private void uitslag() {
        int n = stemmen.length;
        int[] tel = new int[n];
        int overslaan = 0;
        for (Deelnemer d : r.deelnemers) {
            if (!d.wakker || d.weg) {
                continue;
            }
            if (stemmen[d.idx] >= 0) {
                tel[stemmen[d.idx]]++;
            } else {
                overslaan++;
            }
        }
        int best = -1;
        boolean dubbel = false;
        for (int i = 0; i < n; i++) {
            if (tel[i] > 0 && (best < 0 || tel[i] > tel[best])) {
                best = i;
                dubbel = false;
            } else if (best >= 0 && tel[i] == tel[best]) {
                dubbel = true;
            }
        }
        gelijk = best >= 0 && (dubbel || tel[best] == overslaan);
        weg = best >= 0 && !dubbel && tel[best] > overslaan && geldig(best) ? best : -1;
        if (weg >= 0) {
            wegMika = r.d(weg).mika();
        }
        stap = Stap.UITSLAG;
        uitslagStart = teller;
        r.meld(Gebeurtenis.Soort.UITSLAG, weg, wegMika ? 1 : 0, gelijk ? 1 : 0);
    }

    /** How many votes each participant got (the last entry: skipped). */
    public int[] telling() {
        int[] tel = new int[stemmen.length + 1];
        for (Deelnemer d : r.deelnemers) {
            if (stemmen[d.idx] >= 0) {
                tel[stemmen[d.idx]]++;
            } else if (d.wakker && !d.weg) {
                tel[stemmen.length]++;
            }
        }
        return tel;
    }
}
