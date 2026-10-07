package nl.juiced.guhs.feature.guhpixel.among.model;

import java.util.ArrayList;
import java.util.List;

/**
 * An NPC Mika. It pretends to do tasks (walks to panels and stands there as long as a real task takes), and pushes a crew
 * guh asleep only when it is alone with exactly one victim and nobody can see either of them. Afterwards it gets away,
 * through a vent when the room has one, and goes "do a task" somewhere else (its alibi). Now and then it sabotages:
 * lights out (then nobody sees far), the Knabbelalarm, or the doors of the room it is hunting in. It is not omniscient: it
 * only "hears" a lonely crew guh within a walking distance, and it never knows who is behind a wall.
 */
public final class MikaBrein extends Brein {
    private enum Stand { KIES, NEP_LOPEN, NEP_WERK, TREUZEL, JAAG, NAAR_LUIK, ZELF_MELDEN }

    private Stand stand = Stand.TREUZEL;
    private int nepPaneel = -1, werkTeller;
    private int doelwit = -1, jaagTeller;
    private int zelfMeld = -1;
    /** The victim of this Mika's last push (it does not "find" that sleeper). */
    public int laatsteSlachtoffer = -1;

    public MikaBrein(Ronde r, Deelnemer ik) {
        super(r, ik);
        wacht = 20 + rng.nextInt(100);
    }

    @Override
    public void vergaderingBegint() {
        super.vergaderingBegint();
        stand = Stand.TREUZEL;
        doelwit = -1;
        zelfMeld = -1;
    }

    @Override
    public void naVergadering() {
        super.naVergadering();
        stand = Stand.TREUZEL;
        laatsteSlachtoffer = -1;
    }

    /** Nobody but the victim can see the Mika or the victim, and the victim's zone holds no other crew. */
    private boolean veilig(Deelnemer prooi) {
        for (Deelnemer w : r.deelnemers) {
            if (w == ik || w == prooi || !w.wakker || w.weg || w.mika()) {
                continue;
            }
            if (r.ziet(w, ik) || r.ziet(w, prooi)) {
                return false;
            }
            if (!r.donker() && w.zone == prooi.zone) {
                return false;
            }
        }
        return true;
    }

    /** No awake crew guh can see the Mika right now. */
    private boolean zietNiemand() {
        for (Deelnemer w : r.deelnemers) {
            if (w != ik && w.wakker && !w.weg && !w.mika() && r.ziet(w, ik)) {
                return false;
            }
        }
        return true;
    }

    private Deelnemer prooi() {
        Deelnemer best = null;
        for (Deelnemer c : r.deelnemers) {
            if (c.mika() || !c.wakker || c.weg || c.zone != ik.zone || !r.ziet(ik, c)) {
                continue;
            }
            if (!veilig(c) && rng.nextDouble() >= r.balans.roekeloos / 20.0) {
                continue;
            }
            if (best == null || ik.afstand(c) < ik.afstand(best)) {
                best = c;
            }
        }
        return best;
    }

    @Override
    protected void denk() {
        saboteer();
        if (stand != Stand.JAAG && stand != Stand.ZELF_MELDEN && ik.duwAfkoel == 0 && r.tick % 5 == 0) {
            Deelnemer p = prooi();
            if (p != null) {
                stand = Stand.JAAG;
                doelwit = p.idx;
                jaagTeller = 0;
                ik.werkt = false;
                stop();
                // the doors of a small room shut behind the two of them
                Schip.Zone zone = r.schip.zone(ik.zone);
                if (zone.kamer() && r.schip.deurenVan(zone.idx()).size() <= 2 && rng.nextDouble() < 0.5) {
                    r.saboteerNpc(ik, Ronde.Sabotage.DEUREN, zone.idx());
                }
            }
        }
        switch (stand) {
            case JAAG -> jaag();
            case ZELF_MELDEN -> {
                Deelnemer slaper = zelfMeld < 0 ? null : r.d(zelfMeld);
                if (slaper == null || !slaper.lichaam) {
                    stand = Stand.KIES;
                } else if (--wacht <= 0) {
                    r.meldNpc(ik, slaper);
                }
            }
            case NAAR_LUIK -> {
                if (beweeg(r.balans.npcSnelheid * 1.2)) {
                    Schip.Luik luik = r.schip.luikIn(ik.zone);
                    if (luik != null && ik.afstand(luik.x() + 0.5, luik.z() + 0.5) < 1.5) {
                        // it peeks through the grates first: only out where no crew guh is in the room
                        List<Schip.Luik> net = new ArrayList<>();
                        for (Schip.Luik l : r.schip.netwerk(luik)) {
                            boolean leeg = true;
                            for (Deelnemer c : r.deelnemers) {
                                leeg &= c.mika() || !c.wakker || c.weg || c.zone != l.kamer();
                            }
                            if (leeg) {
                                net.add(l);
                            }
                        }
                        if (!net.isEmpty() && zietNiemand()) {
                            r.luikNpc(ik, luik, net.get(rng.nextInt(net.size())));
                        }
                    }
                    stand = Stand.KIES;
                }
            }
            case KIES -> kies();
            case NEP_LOPEN -> {
                if (beweeg(r.balans.npcSnelheid)) {
                    if (nepPaneel >= 0) {
                        stand = Stand.NEP_WERK;
                        werkTeller = 100 + rng.nextInt(140);
                        ik.werkt = true;
                    } else {
                        stand = Stand.TREUZEL;
                        wacht = 40 + rng.nextInt(100);
                    }
                }
            }
            case NEP_WERK -> {
                if (nepPaneel >= 0) {
                    kijkNaarPaneel(r.schip.panelen.get(nepPaneel));
                }
                if (--werkTeller <= 0) {
                    ik.werkt = false;
                    stand = Stand.TREUZEL;
                    wacht = 60 + rng.nextInt(200);
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

    private void jaag() {
        Deelnemer p = doelwit < 0 ? null : r.d(doelwit);
        if (p == null || !p.wakker || ik.duwAfkoel > 0 || ++jaagTeller > 200 || !r.ziet(ik, p)) {
            stand = Stand.KIES;
            return;
        }
        if (ik.afstand(p) <= r.balans.duwBereik * 0.85) {
            if (!veilig(p) && rng.nextDouble() >= r.balans.roekeloos) {
                stand = Stand.TREUZEL;      // somebody walked in: act as if nothing was going on
                wacht = 40 + rng.nextInt(60);
                return;
            }
            if (r.duw(ik.idx, p.idx) == Ronde.Antwoord.OK) {
                laatsteSlachtoffer = p.idx;
                gezieneSlapers.add(p.idx);
                naDuw(p);
            } else {
                stand = Stand.KIES;
            }
            return;
        }
        stapNaar(p.x, p.z, r.balans.npcSnelheid * 1.25);
    }

    private void naDuw(Deelnemer slachtoffer) {
        if (r.fase != Ronde.Fase.SPEL) {
            return;
        }
        if (rng.nextDouble() < r.balans.zelfMeldKans) {
            stand = Stand.ZELF_MELDEN;
            zelfMeld = slachtoffer.idx;
            wacht = 50 + rng.nextInt(60);
            return;
        }
        Schip.Luik luik = r.schip.zone(ik.zone).kamer() ? r.schip.luikIn(ik.zone) : null;
        if (luik != null && rng.nextDouble() < r.balans.luikKans && loopNaar(luik.knoop())) {
            stand = Stand.NAAR_LUIK;
            return;
        }
        nepTaak(25);
    }

    private void kies() {
        // nearly ready to push: go where a lonely crew guh is (as far as the Mika can "hear")
        if (ik.duwAfkoel <= 150 && rng.nextDouble() < r.balans.sluwheid) {
            int start = r.schip.dichtsteKnoop(ik.x, ik.z);
            Deelnemer best = null;
            double bestD = r.balans.gehoorAfstand;
            for (Deelnemer c : r.deelnemers) {
                if (c.mika() || !c.wakker || c.weg || c.zone < 0) {
                    continue;
                }
                int samen = 0;
                for (Deelnemer o : r.deelnemers) {
                    if (o != c && o.wakker && !o.weg && !o.mika() && o.zone == c.zone) {
                        samen++;
                    }
                }
                if (samen > 0) {
                    continue;
                }
                double d = r.schip.loopAfstand(start, r.schip.dichtsteKnoop(c.x, c.z));
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }
            if (best != null && loopNaar(r.schip.dichtsteKnoop(best.x, best.z))) {
                nepPaneel = -1;
                stand = Stand.NEP_LOPEN;
                return;
            }
        }
        nepTaak(0);
    }

    /** Walks to a task panel (at least this far away) to pretend. */
    private void nepTaak(double minAfstand) {
        int start = r.schip.dichtsteKnoop(ik.x, ik.z);
        List<Schip.Paneel> kan = new ArrayList<>();
        for (Schip.Paneel p : r.schip.panelen) {
            if (p.soort() == Schip.PaneelSoort.TAAK) {
                double d = r.schip.loopAfstand(start, p.knoop());
                if (d >= minAfstand && d < 70) {
                    kan.add(p);
                }
            }
        }
        if (kan.isEmpty()) {
            stand = Stand.TREUZEL;
            wacht = 60;
            return;
        }
        Schip.Paneel p = kan.get(rng.nextInt(kan.size()));
        if (loopNaar(p.knoop())) {
            nepPaneel = p.idx();
            stand = Stand.NEP_LOPEN;
        } else {
            stand = Stand.TREUZEL;
            wacht = 40;
        }
    }

    private void saboteer() {
        if (r.sabotage != Ronde.Sabotage.GEEN || r.saboteerAfkoel > 0 || rng.nextInt(Math.max(1, r.balans.saboteerGemiddeld)) != 0) {
            return;
        }
        double k = rng.nextDouble();
        if (k < 0.5) {
            r.saboteerNpc(ik, Ronde.Sabotage.LICHT, -1);
        } else if (k < 0.72) {
            r.saboteerNpc(ik, Ronde.Sabotage.ALARM, -1);
        } else {
            List<Schip.Zone> kamers = r.schip.kamers();
            r.saboteerNpc(ik, Ronde.Sabotage.DEUREN, kamers.get(rng.nextInt(kamers.size())).idx());
        }
    }
}
