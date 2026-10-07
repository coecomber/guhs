package nl.juiced.guhs.feature.guhpixel.among.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The head of a guh NPC: walking over the ship's graph, looking around ({@link Geheugen}), noticing sleepers. What it
 * wants is in the two subclasses: {@link CrewBrein} (tasks, reporting, the button, fixing sabotage) and {@link MikaBrein}
 * (faking tasks, pushing when nobody looks, vents, sabotage, alibis).
 */
public abstract class Brein {
    protected final Ronde r;
    protected final Deelnemer ik;
    protected final Random rng;
    public final Geheugen geheugen;

    private List<Integer> pad;
    private int padI;
    protected int doel = -1;
    private int herpad;
    protected int wacht;
    protected final Set<Integer> gezieneSlapers = new HashSet<>();

    protected Brein(Ronde r, Deelnemer ik) {
        this.r = r;
        this.ik = ik;
        this.rng = r.rng;
        this.geheugen = new Geheugen(r.deelnemers.size());
    }

    public final void tick() {
        if (r.fase != Ronde.Fase.SPEL || ik.weg) {
            return;
        }
        if (!ik.wakker) {
            droom();
            return;
        }
        for (Deelnemer d : r.deelnemers) {
            if (d.lichaam && !gezieneSlapers.contains(d.idx) && r.ziet(ik, d.lichaamX, d.lichaamZ, d.lichaamZone)) {
                gezieneSlapers.add(d.idx);
                geheugen.zietSlaper(r, ik, d);
                slaperGezien(d);
                if (r.fase != Ronde.Fase.SPEL) {
                    return;
                }
            }
        }
        denk();
    }

    /** Every tick while awake and the round runs. */
    protected abstract void denk();

    /** Every tick as a droomguh. */
    protected void droom() {
    }

    protected void slaperGezien(Deelnemer slaper) {
    }

    /** A sabotage started. */
    public void sabotage(Ronde.Sabotage s) {
    }

    public void vergaderingBegint() {
        pad = null;
        doel = -1;
        wacht = 0;
        ik.werkt = false;
    }

    public void naVergadering() {
        pad = null;
        doel = -1;
        wacht = 20 + rng.nextInt(80);
        gezieneSlapers.clear();
        geheugen.naVergadering();
    }

    public void inSlaap() {
        pad = null;
        doel = -1;
        ik.werkt = false;
    }

    /** Plans the walk to a node; false when there is no way now (closed doors). */
    protected boolean loopNaar(int knoop) {
        int start = r.schip.dichtsteKnoop(ik.x, ik.z);
        List<Integer> p = r.schip.pad(start, knoop, ik.wakker ? r.dichteDeuren : Set.of());
        if (p == null) {
            return false;
        }
        pad = new ArrayList<>(p.size() + 1);
        Schip.Knoop s = r.schip.knopen.get(start);
        if (Math.hypot(s.x() - ik.x, s.z() - ik.z) > 0.05) {
            pad.add(start);
        }
        pad.addAll(p);
        padI = 0;
        doel = knoop;
        return true;
    }

    protected boolean onderweg() {
        return pad != null;
    }

    protected void stop() {
        pad = null;
        doel = -1;
    }

    /** One step along the planned walk; true when it is done (or there is none). */
    protected boolean beweeg(double snelheid) {
        if (pad == null) {
            return true;
        }
        if (padI >= pad.size()) {
            pad = null;
            return true;
        }
        int volgende = pad.get(padI);
        if (ik.wakker && r.deurDicht(volgende)) {
            if (--herpad <= 0) {
                herpad = 20;
                int bestemming = doel;
                if (bestemming >= 0) {
                    loopNaar(bestemming);
                }
            }
            return false;
        }
        Schip.Knoop k = r.schip.knopen.get(volgende);
        if (stapNaar(k.x(), k.z(), snelheid)) {
            padI++;
            if (padI >= pad.size()) {
                pad = null;
                return true;
            }
        }
        return false;
    }

    /** One step straight to a point; true when the NPC stands on it. */
    protected boolean stapNaar(double x, double z, double snelheid) {
        double dx = x - ik.x, dz = z - ik.z, len = Math.hypot(dx, dz);
        boolean er = len <= snelheid;
        if (er) {
            ik.x = x;
            ik.z = z;
        } else {
            ik.x += dx / len * snelheid;
            ik.z += dz / len * snelheid;
        }
        if (len > 1e-4) {
            ik.yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        }
        int zone = r.schip.zoneOp(ik.x, ik.z);
        if (zone >= 0) {
            ik.zone = zone;
        }
        return er;
    }

    protected void kijkNaar(double x, double z) {
        double dx = x - ik.x, dz = z - ik.z;
        if (Math.hypot(dx, dz) > 1e-4) {
            ik.yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        }
    }

    protected void kijkNaarPaneel(Schip.Paneel p) {
        kijkNaar(p.bx() + 0.5, p.bz() + 0.5);
    }
}
