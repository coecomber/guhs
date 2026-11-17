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
    // --- 1.10.6, the smart Mika of Lastig (Balans.slimmeMika) ---
    /** The lonely crew guh it is on its way to (-1: none), and the vent it wants to crawl out of (-1: any empty one). */
    private int prooiDoel = -1, luikDoel = -1;
    /** Away from this spot (the sleeper) when it picks a vent to come out of. */
    private double wegX = Double.NaN, wegZ;

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
        prooiDoel = luikDoel = -1;
        wegX = Double.NaN;
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

    private boolean slim() {
        return r.balans.slimmeMika;
    }

    /** Another awake Mika that is nearly ready to push is closer to this crew guh (that one is its prey). */
    private boolean andereMikaDichter(Deelnemer c) {
        for (Deelnemer m : r.deelnemers) {
            if (m != ik && m.mika() && m.wakker && !m.weg && m.duwAfkoel <= 150 && m.afstand(c) < ik.afstand(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean andereMikaIn(int kamer) {
        for (Deelnemer m : r.deelnemers) {
            if (m != ik && m.mika() && m.wakker && !m.weg && m.zone == kamer) {
                return true;
            }
        }
        return false;
    }

    /** The vent nearest to this node on the walking graph, null when none is within {@code max}. */
    private Schip.Luik luikBij(int start, double max) {
        Schip.Luik best = null;
        double bestD = max;
        for (Schip.Luik l : r.schip.luiken) {
            double d = r.schip.loopAfstand(start, l.knoop());
            if (d < bestD) {
                bestD = d;
                best = l;
            }
        }
        return best;
    }

    /** No awake crew guh in that room, except perhaps the one the Mika is after. */
    private boolean leegOpProoiNa(int kamer, int prooi) {
        for (Deelnemer c : r.deelnemers) {
            if (!c.mika() && c.wakker && !c.weg && c.zone == kamer && c.idx != prooi) {
                return false;
            }
        }
        return true;
    }

    /** Smart: the lights go out right before it reaches its prey (the crew then sees almost nothing). */
    private void lichtUitVoorDuw() {
        if (prooiDoel < 0 || ik.duwAfkoel > 60 || r.sabotage != Ronde.Sabotage.GEEN || r.saboteerAfkoel > 0) {
            return;
        }
        Deelnemer p = r.d(prooiDoel);
        if (!p.wakker || p.weg || ik.afstand(p) > 16) {
            return;
        }
        r.saboteerNpc(ik, Ronde.Sabotage.LICHT, -1);
    }

    @Override
    protected void denk() {
        if (slim() && r.tick % 5 == 0) {
            lichtUitVoorDuw();
        }
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
                if (zone.kamer() && r.schip.deurenVan(zone.idx()).size() <= 2 && (slim() || rng.nextDouble() < 0.5)) {
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
                if (slim()) {
                    if (beweeg(r.balans.npcSnelheid * 1.2)) {
                        slimLuik();
                    }
                    return;
                }
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

    /**
     * Smart, at a vent: peek through the grates and crawl out of the right one: to its prey (a room with nobody else in
     * it), or after a push far away from the sleeper, preferably beside a task panel (it is "working" there at once).
     */
    private void slimLuik() {
        Schip.Luik luik = null;
        for (Schip.Luik l : r.schip.luiken) {
            if (ik.afstand(l.x() + 0.5, l.z() + 0.5) < 1.5) {
                luik = l;
            }
        }
        int doel = luikDoel;
        luikDoel = -1;
        stand = Stand.KIES;
        if (luik == null || !zietNiemand()) {
            wegX = Double.NaN;
            return;
        }
        Schip.Luik uit = null;
        if (doel >= 0) {
            Schip.Luik d = r.schip.luiken.get(doel);
            if (prooiDoel >= 0 && d != luik && d.netwerk().equals(luik.netwerk()) && leegOpProoiNa(d.kamer(), prooiDoel)) {
                // the lights out first: the prey must not see it crawl out
                if (!r.donker() && r.sabotage == Ronde.Sabotage.GEEN && r.saboteerAfkoel == 0) {
                    r.saboteerNpc(ik, Ronde.Sabotage.LICHT, -1);
                }
                if (!r.ziet(r.d(prooiDoel), d.x() + 0.5, d.z() + 0.5, d.kamer())) {
                    uit = d;
                }
            }
            if (uit == null) {
                prooiDoel = -1;     // the prey is not alone any more: stay out of the vents
                return;
            }
        }
        if (uit == null) {
            int vanKnoop = Double.isNaN(wegX) ? luik.knoop() : r.schip.dichtsteKnoop(wegX, wegZ);
            double best = -1;
            for (Schip.Luik l : r.schip.netwerk(luik)) {
                if (l == luik || !leegOpProoiNa(l.kamer(), -1)) {
                    continue;
                }
                boolean paneel = false;
                for (Schip.Paneel p : r.schip.panelen) {
                    paneel |= p.soort() == Schip.PaneelSoort.TAAK && p.kamer() == l.kamer();
                }
                double score = r.schip.loopAfstand(vanKnoop, l.knoop()) + (paneel ? 25 : 0) + rng.nextDouble() * 10;
                if (score > best) {
                    best = score;
                    uit = l;
                }
            }
        }
        wegX = Double.NaN;
        if (uit == null) {
            return;
        }
        r.luikNpc(ik, luik, uit);
        if (prooiDoel < 0) {
            nepTaakIn(ik.zone);
        }
    }

    /** Pretends a task in this room (the nearest task panel there), or anywhere when the room has none. */
    private void nepTaakIn(int kamer) {
        int start = r.schip.dichtsteKnoop(ik.x, ik.z);
        Schip.Paneel best = null;
        double bestD = Double.MAX_VALUE;
        for (Schip.Paneel p : r.schip.panelen) {
            if (p.soort() == Schip.PaneelSoort.TAAK && p.kamer() == kamer) {
                double d = r.schip.loopAfstand(start, p.knoop());
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
        }
        if (best != null && loopNaar(best.knoop())) {
            nepPaneel = best.idx();
            stand = Stand.NEP_LOPEN;
        } else {
            nepTaak(0);
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
        if (slim()) {
            prooiDoel = -1;
            // the Knabbelalarm right away: nobody can press the button, and the crew runs to the panels
            if (r.sabotage == Ronde.Sabotage.GEEN && r.saboteerAfkoel == 0 && rng.nextDouble() < 0.5) {
                r.saboteerNpc(ik, Ronde.Sabotage.ALARM, -1);
            }
            Schip.Luik luik = r.schip.zone(ik.zone).kamer() ? r.schip.luikIn(ik.zone) : null;
            if (luik != null && loopNaar(luik.knoop())) {
                wegX = slachtoffer.lichaamX;
                wegZ = slachtoffer.lichaamZ;
                luikDoel = -1;
                stand = Stand.NAAR_LUIK;
                return;
            }
            nepTaak(25);
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
        if (slim()) {
            prooiDoel = -1;
            if (ik.duwAfkoel <= 150 && slimJacht()) {
                return;
            }
            nepTaak(0);
            return;
        }
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

    /**
     * Smart hunting: the nearest lonely crew guh it can "hear" that the other Mika is not closer to, on foot or through
     * the vents (when that is shorter); false when there is none.
     */
    private boolean slimJacht() {
        if (rng.nextDouble() >= Math.min(1.0, r.balans.sluwheid + 0.15)) {
            return false;
        }
        int start = r.schip.dichtsteKnoop(ik.x, ik.z);
        Schip.Luik eigen = luikBij(start, 20);
        Deelnemer best = null;
        Schip.Luik bestLuik = null;
        double bestD = r.balans.gehoorAfstand;
        for (Deelnemer c : r.deelnemers) {
            if (c.mika() || !c.wakker || c.weg || c.zone < 0 || !leegOpProoiNa(c.zone, c.idx) || andereMikaDichter(c)) {
                continue;
            }
            int cKnoop = r.schip.dichtsteKnoop(c.x, c.z);
            double d = r.schip.loopAfstand(start, cKnoop);
            Schip.Luik via = null;
            Schip.Luik daar = r.schip.zone(c.zone).kamer() ? r.schip.luikIn(c.zone) : null;
            boolean lichtKan = r.donker() || (r.sabotage == Ronde.Sabotage.GEEN && r.saboteerAfkoel == 0);
            if (lichtKan && eigen != null && daar != null && daar != eigen && daar.netwerk().equals(eigen.netwerk())) {
                double v = r.schip.loopAfstand(start, eigen.knoop()) + 6 + r.schip.loopAfstand(daar.knoop(), cKnoop);
                if (v < d) {
                    d = v;
                    via = daar;
                }
            }
            if (d < bestD) {
                bestD = d;
                best = c;
                bestLuik = via;
            }
        }
        if (best == null) {
            return false;
        }
        prooiDoel = best.idx;
        if (bestLuik != null && loopNaar(eigen.knoop())) {
            luikDoel = bestLuik.idx();
            wegX = Double.NaN;
            stand = Stand.NAAR_LUIK;
            return true;
        }
        if (loopNaar(r.schip.dichtsteKnoop(best.x, best.z))) {
            nepPaneel = -1;
            stand = Stand.NEP_LOPEN;
            return true;
        }
        prooiDoel = -1;
        return false;
    }

    /** Walks to a task panel (at least this far away) to pretend. */
    private void nepTaak(double minAfstand) {
        int start = r.schip.dichtsteKnoop(ik.x, ik.z);
        List<Schip.Paneel> kan = new ArrayList<>();
        for (Schip.Paneel p : r.schip.panelen) {
            if (p.soort() == Schip.PaneelSoort.TAAK && !(slim() && andereMikaIn(p.kamer()))) {
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
