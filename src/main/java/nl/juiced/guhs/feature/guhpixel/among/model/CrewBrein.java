package nl.juiced.guhs.feature.guhpixel.among.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A crew guh: walks from room to room doing its tasks (each takes a believable while, with dawdling and little strolls in
 * between), reports a sleeper it finds, walks to the emergency button when it saw something it is sure of (a push it could
 * not report, somebody in a vent), and helps to fix a sabotage. As a droomguh it calmly finishes its tasks.
 */
public final class CrewBrein extends Brein {
    private enum Stand { KIES, LOPEN, WERK, ZWERVEN, TREUZEL }

    private Stand stand = Stand.TREUZEL;
    private TaakStand bezig;
    private int werkTeller;
    private int meldSlaper = -1, meldOver;
    private int herstelPaneel = -1, herstelTeller, herkans;
    private boolean naarKnop;

    public CrewBrein(Ronde r, Deelnemer ik) {
        super(r, ik);
        wacht = 20 + rng.nextInt(120);
    }

    @Override
    protected void slaperGezien(Deelnemer slaper) {
        if (meldSlaper < 0) {
            meldSlaper = slaper.idx;
            meldOver = 10 + rng.nextInt(30);
        }
    }

    @Override
    public void sabotage(Ronde.Sabotage s) {
        if (!ik.wakker) {
            return;
        }
        herkans = 200;
        if (s == Ronde.Sabotage.LICHT && rng.nextDouble() < r.balans.lichtReageerKans) {
            herstelPaneel = r.schip.lichtPaneel.idx();
        } else if (s == Ronde.Sabotage.ALARM && rng.nextDouble() < r.balans.alarmReageerKans) {
            herstelPaneel = alarmPaneel(rng.nextDouble() < 0.25);
        }
        herstelTeller = 0;
    }

    /** The nearer alarm panel that is not fixed yet (or the other one). */
    private int alarmPaneel(boolean andere) {
        int start = r.schip.dichtsteKnoop(ik.x, ik.z);
        Schip.Paneel a = r.schip.alarmPanelen.get(0), b = r.schip.alarmPanelen.get(1);
        boolean aDichter = r.schip.loopAfstand(start, a.knoop()) <= r.schip.loopAfstand(start, b.knoop());
        int kies = aDichter != andere ? 0 : 1;
        if (r.alarmVast[kies]) {
            kies = 1 - kies;
        }
        return r.schip.alarmPanelen.get(kies).idx();
    }

    @Override
    public void vergaderingBegint() {
        super.vergaderingBegint();
        stand = Stand.TREUZEL;
        meldSlaper = -1;
        herstelPaneel = -1;
        naarKnop = false;
    }

    @Override
    public void naVergadering() {
        super.naVergadering();
        stand = Stand.TREUZEL;
    }

    @Override
    public void inSlaap() {
        super.inSlaap();
        stand = Stand.KIES;
        meldSlaper = -1;
        herstelPaneel = -1;
        naarKnop = false;
    }

    @Override
    protected void denk() {
        if (meldSlaper >= 0) {
            Deelnemer slaper = r.d(meldSlaper);
            if (!slaper.lichaam) {
                meldSlaper = -1;
            } else {
                kijkNaar(slaper.lichaamX, slaper.lichaamZ);
                if (--meldOver <= 0) {
                    r.meldNpc(ik, slaper);
                }
                return;
            }
        }
        if (herstel()) {
            return;
        }
        if (!naarKnop && !ik.knopGebruikt && (geheugen.zagLuikVan >= 0 || geheugen.zagDuwDoor >= 0)) {
            naarKnop = true;
            ik.werkt = false;
            stand = Stand.KIES;
            if (!loopNaar(r.schip.knopKnoop)) {
                naarKnop = false;
            }
        }
        if (naarKnop) {
            if (beweeg(r.balans.npcSnelheid * 1.15)) {
                if (r.knopNpc(ik)) {
                    naarKnop = false;
                } else if (ik.knopGebruikt) {
                    naarKnop = false;
                }
            }
            return;
        }
        taken(false);
    }

    @Override
    protected void droom() {
        taken(true);
    }

    /** Fixing a sabotage; true while this guh is busy with it. */
    private boolean herstel() {
        if (r.sabotage != Ronde.Sabotage.LICHT && r.sabotage != Ronde.Sabotage.ALARM) {
            herstelPaneel = -1;
            return false;
        }
        if (herstelPaneel < 0) {
            if (--herkans <= 0) {
                herkans = 200;
                if (r.sabotage == Ronde.Sabotage.LICHT && rng.nextDouble() < 0.25) {
                    herstelPaneel = r.schip.lichtPaneel.idx();
                } else if (r.sabotage == Ronde.Sabotage.ALARM && rng.nextDouble() < 0.5) {
                    herstelPaneel = alarmPaneel(false);
                }
                herstelTeller = 0;
            }
            if (herstelPaneel < 0) {
                return false;
            }
        }
        Schip.Paneel p = r.schip.panelen.get(herstelPaneel);
        if (r.sabotage == Ronde.Sabotage.ALARM) {
            int welke = r.schip.alarmPanelen.indexOf(p);
            if (welke < 0 || r.alarmVast[welke]) {
                int ander = 1 - Math.max(0, welke);
                if (r.alarmVast[ander]) {
                    herstelPaneel = -1;
                    return false;
                }
                herstelPaneel = r.schip.alarmPanelen.get(ander).idx();
                herstelTeller = 0;
                stop();
                p = r.schip.panelen.get(herstelPaneel);
            }
        }
        ik.werkt = false;
        stand = Stand.KIES;
        if (doel != p.knoop() || !onderweg()) {
            Schip.Knoop k = r.schip.knopen.get(p.knoop());
            if (ik.afstand(k.x(), k.z()) > 0.3) {
                if (doel != p.knoop() || !onderweg()) {
                    if (!loopNaar(p.knoop())) {
                        return true;
                    }
                }
            }
        }
        if (beweeg(r.balans.npcSnelheid * 1.25)) {
            kijkNaarPaneel(p);
            if (++herstelTeller >= r.balans.herstelTijd) {
                r.herstelNpc(ik, p);
                herstelPaneel = -1;
            }
        }
        return true;
    }

    private void taken(boolean droom) {
        switch (stand) {
            case KIES -> {
                bezig = kiesTaak();
                if (bezig == null) {
                    if (droom) {
                        return;
                    }
                    zwerf();
                } else if (loopNaar(r.schip.panelen.get(bezig.paneel()).knoop())) {
                    stand = Stand.LOPEN;
                } else {
                    stand = Stand.TREUZEL;
                    wacht = 40;
                }
            }
            case LOPEN -> {
                if (bezig == null || bezig.klaar()) {
                    stand = Stand.KIES;
                } else if (beweeg(r.balans.npcSnelheid)) {
                    stand = Stand.WERK;
                    werkTeller = (int) (bezig.taak.duur() * (0.8 + rng.nextDouble() * 0.4));
                    ik.werkt = true;
                }
            }
            case WERK -> {
                if (bezig == null || bezig.klaar()) {
                    ik.werkt = false;
                    stand = Stand.KIES;
                    return;
                }
                kijkNaarPaneel(r.schip.panelen.get(bezig.paneel()));
                if (--werkTeller <= 0) {
                    ik.werkt = false;
                    r.taakStapNpc(ik, bezig);
                    bezig = null;
                    wacht = droom ? 40 : r.balans.treuzelMin + rng.nextInt(r.balans.treuzelMax - r.balans.treuzelMin);
                    stand = Stand.TREUZEL;
                    if (!droom && rng.nextDouble() < r.balans.zwerfKans) {
                        zwerf();
                    }
                }
            }
            case ZWERVEN -> {
                if (beweeg(r.balans.npcSnelheid * 0.85)) {
                    stand = Stand.TREUZEL;
                }
            }
            case TREUZEL -> {
                if (--wacht <= 0) {
                    stand = Stand.KIES;
                }
            }
            default -> {
            }
        }
    }

    /** The next task: one of the two nearest that are not done. */
    private TaakStand kiesTaak() {
        List<TaakStand> open = new ArrayList<>();
        for (TaakStand t : ik.taken) {
            if (!t.klaar()) {
                open.add(t);
            }
        }
        if (open.isEmpty()) {
            return null;
        }
        int start = r.schip.dichtsteKnoop(ik.x, ik.z);
        open.sort(Comparator.comparingDouble(t -> r.schip.loopAfstand(start, r.schip.panelen.get(t.paneel()).knoop())));
        return open.size() > 1 && rng.nextDouble() < 0.3 ? open.get(1) : open.get(0);
    }

    /** A little stroll: to another spot in this room or to a room nearby. */
    private void zwerf() {
        int start = r.schip.dichtsteKnoop(ik.x, ik.z);
        List<Schip.Zone> kamers = r.schip.kamers();
        int naar = -1;
        for (int poging = 0; poging < 6 && naar < 0; poging++) {
            Schip.Zone z = kamers.get(rng.nextInt(kamers.size()));
            int hub = r.schip.hub(z.idx());
            if (r.schip.loopAfstand(start, hub) < 48) {
                naar = hub;
            }
        }
        if (naar >= 0 && loopNaar(naar)) {
            stand = Stand.ZWERVEN;
            if (wacht <= 0) {
                wacht = 120 + rng.nextInt(300);
            }
        } else {
            stand = Stand.TREUZEL;
            wacht = Math.max(wacht, 60);
        }
    }
}
