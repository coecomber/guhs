package nl.juiced.guhs.feature.guhpixel.among.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * One round of Among Guhs as pure rules: who is where, who is awake, the tasks, the sabotage, the meetings and who wins.
 * No Minecraft in here: the session (AmongSessie) feeds the positions and actions of the real players in and turns
 * {@link #gebeurtenissen} into the world; {@link Simulatie} plays whole rounds with NPCs only.
 * <p>
 * Participants 0..echteSpelers-1 are the real players, the rest are guh NPCs with a {@link Brein}. Nobody is ever hurt:
 * "out" means pushed asleep (or voted out), after which a participant goes on as a droomguh and still finishes tasks.
 * <p>
 * Winning: the crew when every crew task is done or no Mika is awake; the Mika when the awake Mikas are at least as many
 * as the awake crew, or when the Knabbelalarm runs out.
 */
public final class Ronde {
    public enum Fase { SPEL, VERGADERING, KLAAR }

    public enum Sabotage { GEEN, LICHT, ALARM, DEUREN }

    public enum Einde { TAKEN, MIKAS_WEG, OVERMACHT, ALARM, VERLATEN }

    public enum Antwoord { OK, AFKOEL, TE_VER, NIET_NU, MAG_NIET }

    public final Schip schip;
    public final Balans balans;
    public final Random rng;
    public final List<Deelnemer> deelnemers = new ArrayList<>();
    /** What happened since the session last looked (the session empties it). */
    public final List<Gebeurtenis> gebeurtenissen = new ArrayList<>();
    public int tick;
    public Fase fase = Fase.SPEL;
    public Sabotage sabotage = Sabotage.GEEN;
    public int sabotageTeller;
    public final boolean[] alarmVast = new boolean[2];
    public int deurKamer = -1;
    public final Set<Integer> dichteDeuren = new HashSet<>();
    public int saboteerAfkoel, knopAfkoel;
    public Vergadering vergadering;
    /** The last finished meeting (its result). */
    public Vergadering vorigeVergadering;
    public Deelnemer.Rol winnaar;
    public Einde einde;
    public int aantalVergaderingen, aantalDuwen;

    /**
     * @param echteSpelers  how many of the participants are real players (indices 0..n-1)
     * @param gedwongenMika null, or the indices that must be the Mikas (tests, the dev command)
     */
    public Ronde(Schip schip, Balans balans, long seed, int echteSpelers, int[] gedwongenMika) {
        this.schip = schip;
        this.balans = balans;
        this.rng = new Random(seed);
        int n = Math.max(balans.deelnemers, echteSpelers);
        List<Kleur> kleuren = new ArrayList<>(List.of(Kleur.values()));
        Collections.shuffle(kleuren, rng);
        for (int i = 0; i < n; i++) {
            Deelnemer d = new Deelnemer(i, i >= echteSpelers);
            d.kleur = kleuren.get(i % kleuren.size());
            deelnemers.add(d);
        }
        if (gedwongenMika != null) {
            for (int m : gedwongenMika) {
                d(m).rol = Deelnemer.Rol.MIKA;
            }
        } else {
            List<Integer> lot = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                lot.add(i);
            }
            Collections.shuffle(lot, rng);
            for (int i = 0; i < Math.min(balans.mikas, n - 1); i++) {
                d(lot.get(i)).rol = Deelnemer.Rol.MIKA;
            }
        }
        for (Deelnemer d : deelnemers) {
            List<Schip.Taak> pool = new ArrayList<>(schip.taken);
            Collections.shuffle(pool, rng);
            for (int i = 0; i < Math.min(balans.takenPerGuh, pool.size()); i++) {
                d.taken.add(new TaakStand(pool.get(i)));
            }
            naarStoel(d);
            if (d.mika()) {
                d.duwAfkoel = balans.duwAfkoelStart;
            }
        }
        for (Deelnemer d : deelnemers) {
            if (d.npc) {
                d.brein = d.mika() ? new MikaBrein(this, d) : new CrewBrein(this, d);
            }
        }
        knopAfkoel = balans.knopAfkoel;
        saboteerAfkoel = balans.saboteerAfkoelStart;
    }

    public Deelnemer d(int idx) {
        return deelnemers.get(idx);
    }

    void meld(Gebeurtenis.Soort soort, int a, int b, int c) {
        gebeurtenissen.add(new Gebeurtenis(soort, a, b, c));
    }

    private void naarStoel(Deelnemer d) {
        Schip.Plek stoel = schip.stoelen.get(d.idx % schip.stoelen.size());
        d.x = stoel.x();
        d.z = stoel.z();
        d.zone = schip.zoneOp(d.x, d.z);
        Schip.Zone kantine = schip.zone(d.zone);
        d.yaw = (float) Math.toDegrees(Math.atan2(-(kantine.middenX() - d.x), kantine.middenZ() - d.z));
    }

    public boolean donker() {
        return sabotage == Sabotage.LICHT;
    }

    /** How far this participant sees now (the crew sees little when the lights are out). */
    public double zicht(Deelnemer kijker) {
        return donker() && !kijker.mika() ? balans.zichtDonker : balans.zicht;
    }

    /** Can this participant see that spot: the same zone, or just through an open door into the next. */
    public boolean ziet(Deelnemer kijker, double x, double z, int zone) {
        if (kijker.zone < 0 || zone < 0) {
            return false;
        }
        double afstand = kijker.afstand(x, z);
        if (kijker.zone == zone) {
            return afstand <= zicht(kijker);
        }
        if (!schip.naast(kijker.zone, zone) || (sabotage == Sabotage.DEUREN && (deurKamer == zone || deurKamer == kijker.zone))) {
            return false;
        }
        return afstand <= Math.min(zicht(kijker), balans.zichtDeur);
    }

    public boolean ziet(Deelnemer kijker, Deelnemer doel) {
        return kijker != doel && doel.wakker && !doel.weg && ziet(kijker, doel.x, doel.z, doel.zone);
    }

    public boolean deurDicht(int knoop) {
        if (dichteDeuren.isEmpty()) {
            return false;
        }
        for (Schip.Deur deur : schip.deuren) {
            if (deur.knoop() == knoop) {
                return dichteDeuren.contains(deur.idx());
            }
        }
        return false;
    }

    /** One game tick. */
    public void tick() {
        if (fase == Fase.KLAAR) {
            return;
        }
        tick++;
        if (fase == Fase.VERGADERING) {
            vergadering.tick();
            return;
        }
        for (Deelnemer d : deelnemers) {
            if (d.duwAfkoel > 0) {
                d.duwAfkoel--;
            }
            if (d.luikAfkoel > 0) {
                d.luikAfkoel--;
            }
        }
        if (saboteerAfkoel > 0) {
            saboteerAfkoel--;
        }
        if (knopAfkoel > 0) {
            knopAfkoel--;
        }
        if (sabotage == Sabotage.ALARM && --sabotageTeller <= 0) {
            klaar(Deelnemer.Rol.MIKA, Einde.ALARM);
            return;
        }
        if (sabotage == Sabotage.DEUREN && --sabotageTeller <= 0) {
            eindeSabotage(false, -1);
        }
        for (Deelnemer d : deelnemers) {
            if (d.npc && fase == Fase.SPEL) {
                d.brein.tick();
            }
        }
        if (fase == Fase.SPEL && tick % 10 == 0) {
            for (Deelnemer d : deelnemers) {
                if (d.npc && d.wakker) {
                    d.brein.geheugen.kijk(this, d);
                }
            }
        }
    }

    /** The session tells where a real player is. */
    public void zetPositie(int idx, double x, double z, float yaw) {
        Deelnemer d = d(idx);
        d.x = x;
        d.z = z;
        d.yaw = yaw;
        int zone = schip.zoneOp(x, z);
        if (zone >= 0) {
            d.zone = zone;
        }
    }

    // --- pushing, reporting, the button ---------------------------------------------------------------------------------

    /** Mika {@code idx} pushes {@code doelIdx} asleep. */
    public Antwoord duw(int idx, int doelIdx) {
        if (fase != Fase.SPEL) {
            return Antwoord.NIET_NU;
        }
        Deelnemer a = d(idx), b = d(doelIdx);
        if (a == b || !a.mika() || !a.wakker || a.weg || b.mika() || !b.wakker || b.weg) {
            return Antwoord.MAG_NIET;
        }
        if (a.duwAfkoel > 0) {
            return Antwoord.AFKOEL;
        }
        if (a.afstand(b) > balans.duwBereik + 0.01 || (a.zone != b.zone && !schip.naast(a.zone, b.zone))) {
            return Antwoord.TE_VER;
        }
        for (Deelnemer w : deelnemers) {
            if (w != a && w != b && w.npc && w.wakker && !w.mika() && (ziet(w, a) || ziet(w, b))) {
                w.brein.geheugen.zagDuw(a.idx);
            }
        }
        b.wakker = false;
        b.werkPaneel = -1;
        b.lichaam = true;
        b.lichaamX = b.x;
        b.lichaamZ = b.z;
        b.lichaamZone = b.zone;
        b.lichaamTick = tick;
        if (b.brein != null) {
            b.brein.inSlaap();
        }
        a.duwAfkoel = balans.duwAfkoel;
        for (Deelnemer m : deelnemers) {
            if (m != a && m.mika()) {
                m.duwAfkoel = Math.max(m.duwAfkoel, (int) (balans.duwAfkoel * balans.duwAfkoelSamen));
            }
        }
        a.duwen++;
        aantalDuwen++;
        meld(Gebeurtenis.Soort.DUW, a.idx, b.idx, b.zone);
        controleerWinst();
        return Antwoord.OK;
    }

    /** Who (awake crew, players included) can see this push happen right now. */
    public List<Deelnemer> getuigen(Deelnemer dader, Deelnemer slachtoffer) {
        List<Deelnemer> uit = new ArrayList<>();
        for (Deelnemer w : deelnemers) {
            if (w != dader && w != slachtoffer && w.wakker && !w.weg && !w.mika() && (ziet(w, dader) || ziet(w, slachtoffer))) {
                uit.add(w);
            }
        }
        return uit;
    }

    /** A real player reports a sleeper (must stand within 5 blocks of it). */
    public Antwoord meld(int idx, int slaperIdx) {
        Deelnemer d = d(idx), slaper = d(slaperIdx);
        if (fase != Fase.SPEL || !slaper.lichaam) {
            return Antwoord.NIET_NU;
        }
        if (!d.wakker || d.weg) {
            return Antwoord.MAG_NIET;
        }
        if (d.afstand(slaper.lichaamX, slaper.lichaamZ) > 5.0) {
            return Antwoord.TE_VER;
        }
        beginVergadering(d, slaper);
        return Antwoord.OK;
    }

    void meldNpc(Deelnemer d, Deelnemer slaper) {
        if (fase == Fase.SPEL && d.wakker && slaper.lichaam) {
            beginVergadering(d, slaper);
        }
    }

    /** May the button be pressed now by this participant? */
    public Antwoord knopKan(Deelnemer d) {
        if (fase != Fase.SPEL || sabotage == Sabotage.ALARM) {
            return Antwoord.NIET_NU;
        }
        if (!d.wakker || d.weg || d.knopGebruikt) {
            return Antwoord.MAG_NIET;
        }
        return knopAfkoel > 0 ? Antwoord.AFKOEL : Antwoord.OK;
    }

    /** A real player presses the emergency button (once per round each). */
    public Antwoord knop(int idx) {
        Deelnemer d = d(idx);
        Antwoord a = knopKan(d);
        if (a != Antwoord.OK) {
            return a;
        }
        if (d.afstand(schip.knopX + 0.5, schip.knopZ + 0.5) > 5.0) {
            return Antwoord.TE_VER;
        }
        d.knopGebruikt = true;
        beginVergadering(d, null);
        return Antwoord.OK;
    }

    boolean knopNpc(Deelnemer d) {
        if (knopKan(d) != Antwoord.OK) {
            return false;
        }
        d.knopGebruikt = true;
        beginVergadering(d, null);
        return true;
    }

    private void beginVergadering(Deelnemer oproeper, Deelnemer slaper) {
        if (sabotage == Sabotage.ALARM || sabotage == Sabotage.DEUREN) {
            eindeSabotage(false, -1);
        }
        int[] waar = new int[deelnemers.size()];
        for (Deelnemer d : deelnemers) {
            waar[d.idx] = d.zone;
        }
        meld(slaper != null ? Gebeurtenis.Soort.GEMELD : Gebeurtenis.Soort.KNOP, oproeper.idx, slaper == null ? -1 : slaper.idx, 0);
        fase = Fase.VERGADERING;
        aantalVergaderingen++;
        for (Deelnemer d : deelnemers) {
            d.uitspraken = 0;
            d.werkPaneel = -1;
        }
        Vergadering v = new Vergadering(this, oproeper.idx, slaper == null ? -1 : slaper.idx, waar);
        vergadering = v;
        for (Deelnemer d : deelnemers) {
            d.lichaam = false;      // every sleeper is carried to the Slaapzaal
            if (d.wakker && !d.weg) {
                naarStoel(d);
            }
            if (d.brein != null) {
                d.brein.vergaderingBegint();
            }
        }
        meld(Gebeurtenis.Soort.VERGADERING, oproeper.idx, slaper == null ? -1 : slaper.idx, 0);
    }

    void naVergadering() {
        Vergadering v = vergadering;
        vorigeVergadering = v;
        vergadering = null;
        fase = Fase.SPEL;
        if (v.weg >= 0) {
            Deelnemer weg = d(v.weg);
            weg.wakker = false;
            weg.weggestemd = true;
            if (weg.brein != null) {
                weg.brein.inSlaap();
            }
            meld(Gebeurtenis.Soort.UIT, weg.idx, 1, 0);
        }
        int mikaNr = 0;
        for (Deelnemer d : deelnemers) {
            if (d.mika()) {
                d.duwAfkoel = balans.duwAfkoelVergadering + (int) (mikaNr++ * balans.duwAfkoel * balans.duwAfkoelSamen * 0.5);
            }
            if (d.brein != null && d.wakker) {
                d.brein.naVergadering();
            }
        }
        knopAfkoel = balans.knopAfkoel;
        saboteerAfkoel = Math.max(saboteerAfkoel, balans.saboteerAfkoelStart);
        meld(Gebeurtenis.Soort.VERDER, 0, 0, 0);
        controleerWinst();
    }

    /** A statement of a real player in the meeting. */
    public boolean uitspraak(int idx, Uitspraak.Soort soort, int over, int zone) {
        return fase == Fase.VERGADERING && vergadering.zegSpeler(idx, soort, over, zone);
    }

    /** A vote of a real player ({@link Vergadering#OVERSLAAN} to skip). */
    public boolean stem(int idx, int doel) {
        return fase == Fase.VERGADERING && vergadering.stem(idx, doel);
    }

    // --- tasks ----------------------------------------------------------------------------------------------------------

    /** The task of this participant that wants this panel now (null: none). A Mika gets one of its fake tasks. */
    public TaakStand taakBij(int idx, int paneelIdx) {
        Deelnemer d = d(idx);
        for (TaakStand t : d.taken) {
            if (!t.klaar() && t.paneel() == paneelIdx) {
                return t;
            }
        }
        return null;
    }

    /** A real player starts working at a panel; null when it has nothing to do there. */
    public TaakStand beginTaak(int idx, int paneelIdx) {
        Deelnemer d = d(idx);
        if (fase != Fase.SPEL || d.weg || (d.mika() && !d.wakker)) {
            return null;
        }
        TaakStand t = taakBij(idx, paneelIdx);
        if (t == null || !bijPaneel(d, schip.panelen.get(paneelIdx))) {
            return null;
        }
        d.werkPaneel = paneelIdx;
        d.werkSinds = tick;
        return t;
    }

    /** A real player says the step at this panel is done; checked: started here, long enough ago, still standing there. */
    public boolean taakKlaar(int idx, int paneelIdx, int minTicks) {
        Deelnemer d = d(idx);
        if (fase != Fase.SPEL || d.weg || d.werkPaneel != paneelIdx || tick - d.werkSinds < minTicks) {
            return false;
        }
        TaakStand t = taakBij(idx, paneelIdx);
        if (t == null || !bijPaneel(d, schip.panelen.get(paneelIdx))) {
            return false;
        }
        d.werkPaneel = -1;
        if (d.mika()) {
            return true;        // a Mika only pretends
        }
        stapKlaar(d, t);
        return true;
    }

    public boolean bijPaneel(Deelnemer d, Schip.Paneel p) {
        return d.afstand(p.bx() + 0.5, p.bz() + 0.5) <= 5.0;
    }

    void taakStapNpc(Deelnemer d, TaakStand t) {
        if (fase == Fase.SPEL && !d.mika() && !t.klaar()) {
            stapKlaar(d, t);
        }
    }

    private void stapKlaar(Deelnemer d, TaakStand t) {
        t.stap++;
        if (t.klaar()) {
            d.takenKlaar++;
        }
        meld(Gebeurtenis.Soort.TAAK, d.idx, t.klaar() ? 1 : 0, 0);
        controleerWinst();
    }

    /** Task steps done by the crew, and all of them (the bar of the whole crew). */
    public int stappenKlaar() {
        int n = 0;
        for (Deelnemer d : deelnemers) {
            if (!d.mika() && !d.weg) {
                for (TaakStand t : d.taken) {
                    n += Math.min(t.stap, t.taak.panelen().length);
                }
            }
        }
        return n;
    }

    public int stappenTotaal() {
        int n = 0;
        for (Deelnemer d : deelnemers) {
            if (!d.mika() && !d.weg) {
                for (TaakStand t : d.taken) {
                    n += t.taak.panelen().length;
                }
            }
        }
        return n;
    }

    // --- sabotage and vents -----------------------------------------------------------------------------------------------

    /** A real Mika sabotages ({@code kamer} only for the doors). */
    public Antwoord saboteer(int idx, Sabotage s, int kamer) {
        Deelnemer d = d(idx);
        if (fase != Fase.SPEL) {
            return Antwoord.NIET_NU;
        }
        if (!d.mika() || d.weg || s == Sabotage.GEEN) {
            return Antwoord.MAG_NIET;
        }
        if (sabotage != Sabotage.GEEN || saboteerAfkoel > 0) {
            return Antwoord.AFKOEL;
        }
        if (s == Sabotage.DEUREN && (kamer < 0 || kamer >= schip.zones.size() || !schip.zone(kamer).kamer())) {
            return Antwoord.MAG_NIET;
        }
        begin(d, s, kamer);
        return Antwoord.OK;
    }

    boolean saboteerNpc(Deelnemer d, Sabotage s, int kamer) {
        if (fase != Fase.SPEL || sabotage != Sabotage.GEEN || saboteerAfkoel > 0) {
            return false;
        }
        begin(d, s, kamer);
        return true;
    }

    private void begin(Deelnemer d, Sabotage s, int kamer) {
        sabotage = s;
        deurKamer = -1;
        if (s == Sabotage.ALARM) {
            sabotageTeller = balans.alarmTijd;
            alarmVast[0] = alarmVast[1] = false;
        } else if (s == Sabotage.DEUREN) {
            sabotageTeller = balans.deurTijd;
            deurKamer = kamer;
            for (Schip.Deur deur : schip.deurenVan(kamer)) {
                dichteDeuren.add(deur.idx());
            }
        }
        meld(Gebeurtenis.Soort.SABOTAGE, s.ordinal(), kamer, d.idx);
        for (Deelnemer o : deelnemers) {
            if (o.npc && !o.mika()) {
                o.brein.sabotage(s);
            }
        }
    }

    private void eindeSabotage(boolean hersteld, int door) {
        Sabotage was = sabotage;
        if (was == Sabotage.GEEN) {
            return;
        }
        sabotage = Sabotage.GEEN;
        dichteDeuren.clear();
        deurKamer = -1;
        saboteerAfkoel = was == Sabotage.DEUREN ? balans.saboteerAfkoel / 2 : balans.saboteerAfkoel;
        meld(Gebeurtenis.Soort.SABOTAGE_KLAAR, was.ordinal(), hersteld ? 1 : 0, door);
    }

    /** A real player fixed something at this panel (the light panel, an alarm panel); false when there was nothing to fix. */
    public boolean herstel(int idx, int paneelIdx) {
        Deelnemer d = d(idx);
        Schip.Paneel p = schip.panelen.get(paneelIdx);
        if (fase != Fase.SPEL || d.weg || !bijPaneel(d, p)) {
            return false;
        }
        return herstelNpc(d, p);
    }

    /** Is there something to fix at this panel right now? */
    public boolean teHerstellen(int paneelIdx) {
        Schip.Paneel p = schip.panelen.get(paneelIdx);
        if (sabotage == Sabotage.LICHT) {
            return p == schip.lichtPaneel;
        }
        int welke = schip.alarmPanelen.indexOf(p);
        return sabotage == Sabotage.ALARM && welke >= 0 && !alarmVast[welke];
    }

    boolean herstelNpc(Deelnemer d, Schip.Paneel p) {
        if (fase != Fase.SPEL || !teHerstellen(p.idx())) {
            return false;
        }
        if (sabotage == Sabotage.LICHT) {
            eindeSabotage(true, d.idx);
            return true;
        }
        int welke = schip.alarmPanelen.indexOf(p);
        alarmVast[welke] = true;
        meld(Gebeurtenis.Soort.ALARM_PANEEL, welke, d.idx, 0);
        if (alarmVast[0] && alarmVast[1]) {
            eindeSabotage(true, d.idx);
        }
        return true;
    }

    /** A real Mika crawls from one vent to another of the same network. */
    public Antwoord luik(int idx, int vanLuik, int naarLuik) {
        Deelnemer d = d(idx);
        if (fase != Fase.SPEL) {
            return Antwoord.NIET_NU;
        }
        if (!d.mika() || !d.wakker || d.weg || vanLuik < 0 || naarLuik < 0 || vanLuik >= schip.luiken.size() || naarLuik >= schip.luiken.size()
                || vanLuik == naarLuik) {
            return Antwoord.MAG_NIET;
        }
        Schip.Luik van = schip.luiken.get(vanLuik), naar = schip.luiken.get(naarLuik);
        if (!van.netwerk().equals(naar.netwerk())) {
            return Antwoord.MAG_NIET;
        }
        if (d.luikAfkoel > 0) {
            return Antwoord.AFKOEL;
        }
        if (d.afstand(van.x() + 0.5, van.z() + 0.5) > 4.0) {
            return Antwoord.TE_VER;
        }
        luikNpc(d, van, naar);
        return Antwoord.OK;
    }

    void luikNpc(Deelnemer d, Schip.Luik van, Schip.Luik naar) {
        zienLuik(d);
        d.x = naar.x() + 0.5;
        d.z = naar.z() + 0.5;
        d.zone = naar.kamer();
        d.luikAfkoel = balans.luikAfkoel;
        zienLuik(d);
        meld(Gebeurtenis.Soort.LUIK, d.idx, van.idx(), naar.idx());
    }

    private void zienLuik(Deelnemer d) {
        for (Deelnemer w : deelnemers) {
            if (w != d && w.npc && w.wakker && !w.mika() && ziet(w, d)) {
                w.brein.geheugen.zagLuik(d.idx);
            }
        }
    }

    // --- leaving and winning ------------------------------------------------------------------------------------------------

    /** A real player left the game: it counts for nothing any more. */
    public void verlaat(int idx) {
        Deelnemer d = d(idx);
        if (d.weg) {
            return;
        }
        d.weg = true;
        d.wakker = false;
        d.lichaam = false;
        meld(Gebeurtenis.Soort.UIT, idx, 2, 0);
        if (fase != Fase.KLAAR) {
            controleerWinst();
        }
    }

    public int wakker(Deelnemer.Rol rol) {
        int n = 0;
        for (Deelnemer d : deelnemers) {
            if (d.wakker && !d.weg && d.rol == rol) {
                n++;
            }
        }
        return n;
    }

    private void controleerWinst() {
        if (fase == Fase.KLAAR) {
            return;
        }
        int crew = wakker(Deelnemer.Rol.CREW), mika = wakker(Deelnemer.Rol.MIKA);
        if (mika == 0) {
            klaar(Deelnemer.Rol.CREW, Einde.MIKAS_WEG);
        } else if (mika >= crew) {
            klaar(Deelnemer.Rol.MIKA, Einde.OVERMACHT);
        } else if (fase == Fase.SPEL && stappenTotaal() > 0 && stappenKlaar() >= stappenTotaal()) {
            klaar(Deelnemer.Rol.CREW, Einde.TAKEN);
        }
    }

    private void klaar(Deelnemer.Rol wie, Einde waardoor) {
        if (sabotage != Sabotage.GEEN) {
            Sabotage was = sabotage;
            sabotage = Sabotage.GEEN;
            dichteDeuren.clear();
            deurKamer = -1;
            meld(Gebeurtenis.Soort.SABOTAGE_KLAAR, was.ordinal(), 0, -1);
        }
        fase = Fase.KLAAR;
        winnaar = wie;
        einde = waardoor;
        meld(Gebeurtenis.Soort.EINDE, wie.ordinal(), waardoor.ordinal(), 0);
    }

    /** (Dev, tests) a meeting right now, called by this participant, whatever the cooldown. */
    public boolean devVergadering(int idx) {
        Deelnemer d = d(idx);
        if (fase != Fase.SPEL || !d.wakker || d.weg) {
            return false;
        }
        beginVergadering(d, null);
        return true;
    }

    /** (Dev, tests) every task of this participant is done. */
    public void devTakenKlaar(int idx) {
        Deelnemer d = d(idx);
        if (fase != Fase.SPEL || d.mika()) {
            return;
        }
        for (TaakStand t : d.taken) {
            while (!t.klaar() && fase == Fase.SPEL) {
                stapKlaar(d, t);
            }
        }
    }

    /** Stops the round without a winner (everybody left). */
    public void breekAf() {
        if (fase != Fase.KLAAR) {
            fase = Fase.KLAAR;
            einde = Einde.VERLATEN;
        }
    }
}
