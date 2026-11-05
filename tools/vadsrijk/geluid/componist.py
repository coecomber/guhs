"""
T-GELUID: the little composer. Turns a piece description (key, mode, tempo, meter, chord progression per section, which
instrument plays what) into notes, writes them as a standard MIDI file, and keeps a mix sheet (gain + reverb per track) next to it.

Music rules it follows (so a generated piece sounds like a tune, not like random notes):
  * melodies are built from a ONE-BAR MOTIF that comes back (A A' A cadence | A A'' new cadence), the classic 8-bar period;
  * strong beats always land on a chord tone, weak beats move by step through the scale, big leaps are followed by a step back;
  * bar 4 ends on a half cadence (open), bar 8 on the tonic (closed);
  * accompaniment voicings move to the NEAREST inversion (smooth voice leading), the bass plays roots (and fifths);
  * the "Vadsrijk" leitmotif (do re mi re do | la do sol | do re mi sol la | sol fa re do) can be quoted in 4/4 pieces.
Everything is seeded: the same piece description always gives the same notes.
"""
import random

MODI = {
    "majeur": [0, 2, 4, 5, 7, 9, 11],
    "mineur": [0, 2, 3, 5, 7, 8, 10],
    "dorisch": [0, 2, 3, 5, 7, 9, 10],
    "lydisch": [0, 2, 4, 6, 7, 9, 11],
    "mixo": [0, 2, 4, 5, 7, 9, 10],
    "harm": [0, 2, 3, 5, 7, 8, 11],
}
NOOT = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8, "Ab": 8, "A": 9,
        "A#": 10, "Bb": 10, "B": 11}

# the instruments (GM bank, program) of GeneralUser GS that suit the Guhs: soft, sweet, wooden, glassy
INSTR = {
    "speeldoos": (0, 10), "celesta": (0, 8), "glocken": (0, 9), "vibrafoon": (0, 11), "marimba": (0, 12), "xylofoon": (0, 13),
    "buisklok": (0, 14), "hakkebord": (0, 15), "kalimba": (0, 108), "harp": (0, 46), "gitaar": (0, 24), "ukelele": (8, 24),
    "steelgitaar": (0, 25), "banjo": (0, 105), "koto": (0, 107), "fluit": (0, 73), "piccolo": (0, 72), "blokfluit": (0, 74),
    "panfluit": (0, 75), "ocarina": (0, 79), "fluitje": (0, 78), "klarinet": (0, 71), "hobo": (0, 68), "fagot": (0, 70),
    "accordeon": (0, 21), "strijkers": (0, 49), "snelstrijk": (0, 48), "tremolo": (0, 44), "pizz": (0, 45), "viool": (0, 40),
    "cello": (0, 42), "contrabas": (0, 43), "hoorn": (0, 60), "hoornsolo": (1, 60), "koper": (0, 61), "trompet": (0, 56),
    "trombone": (0, 57), "tuba": (0, 58), "koor": (0, 52), "oohs": (0, 53), "warmpad": (0, 89), "glaspad": (0, 92),
    "halopad": (0, 94), "pauk": (0, 47), "taiko": (0, 116), "orgel": (0, 19), "klavecimbel": (0, 6), "piano": (0, 0),
    "akbas": (0, 32), "steeldrum": (0, 114), "druppels": (0, 102), "kristal": (0, 98), "klokken": (8, 14),
    "slagwerk": (128, 0), "orkestslag": (128, 48),
}


class Stuk:
    """A piece: settings + the notes, per track."""

    def __init__(self, naam, toon="C", modus="majeur", bpm=90, maat=4, samengesteld=False, seed=1, lus=False):
        self.naam = naam
        self.grond = NOOT[toon]
        self.modus = modus
        self.schaal = MODI[modus]
        self.bpm = bpm   # quarter notes per minute (6/8: give the dotted-quarter tempo * 1.5)
        self.maat = maat   # quarter units per bar (4/4 = 4, 3/4 = 3, 6/8 = 3 with samengesteld=True)
        self.samengesteld = samengesteld
        self.rng = random.Random(seed)
        self.lus = lus
        self.sporen = {}   # track -> [instrument, gain, galm, [(start, dur, key, vel)]]
        self.t = 0.0   # the write position (quarter units)
        self.akkoorden = []   # (start, end, pcs, root_pc)

    # ---- tracks -------------------------------------------------------------------------------------------------------
    def spoor(self, naam, instrument, gain=1.0, galm=0.3):
        if naam not in self.sporen:
            self.sporen[naam] = [instrument, gain, galm, []]
        return self.sporen[naam][3]

    def noot(self, spoor, start, dur, key, vel):
        if dur <= 0 or key < 0 or key > 127:
            return
        vel = max(1, min(127, int(vel)))
        self.sporen[spoor][3].append((start, dur, int(key), vel))

    # ---- harmony ------------------------------------------------------------------------------------------------------
    def pc(self, graad):
        """Scale degree (0-based, may be <0 or >6) -> pitch class."""
        o, d = divmod(graad, 7)
        return (self.grond + self.schaal[d]) % 12

    def akkoord(self, tok):
        """'1', '5M', '4m', 'b7', '2m7', '5s4', '1a9', '6' -> (pitch classes, root pc). Diatonic by default."""
        alt = 0
        while tok and tok[0] in "b#":
            alt += -1 if tok[0] == "b" else 1
            tok = tok[1:]
        d = int(tok[0]) - 1
        suf = tok[1:]
        root = (self.pc(d) + alt) % 12
        if alt:
            # a borrowed chord (bVI, bVII, bIII): major triad on the altered root
            iv = [0, 4, 7]
        else:
            iv = [(self.pc(d + 2) - self.pc(d)) % 12, (self.pc(d + 4) - self.pc(d)) % 12]
            iv = [0] + iv
        if "M" in suf and "M7" not in suf:
            iv = [0, 4, 7]
        if suf.startswith("m"):
            iv = [0, 3, 7]
        if "d" in suf:
            iv = [0, 3, 6]
        if "s4" in suf:
            iv = [0, 5, 7]
        if "7" in suf:
            if "M7" in suf:
                iv = iv + [11]
            elif alt or "M" in suf or suf.startswith("m"):
                iv = iv + [10]
            else:
                iv = iv + [(self.pc(d + 6) - self.pc(d)) % 12]
        if "a9" in suf:
            iv = iv + [2]
        return [(root + i) % 12 for i in iv], root

    def zet_akkoorden(self, start, rij):
        """rij: tokens per bar, 'x,y' = two chords in one bar. Returns the list [(start, end, pcs, root)] of this section."""
        lijst = []
        t = start
        for balk in rij.split():
            delen = balk.split(",")
            stap = self.maat / len(delen)
            for d in delen:
                pcs, root = self.akkoord(d)
                lijst.append((t, t + stap, pcs, root))
                t += stap
        self.akkoorden.extend(lijst)
        return lijst

    def akkoord_op(self, t):
        for a in self.akkoorden:
            if a[0] <= t + 1e-6 < a[1]:
                return a
        return self.akkoorden[-1]

    # ---- pitch helpers ------------------------------------------------------------------------------------------------
    def in_schaal(self, k):
        return (k - self.grond) % 12 in self.schaal

    def stap(self, k, n):
        """n diatonic steps from key k (an off-scale k first snaps in the step's direction)."""
        richting = 1 if n >= 0 else -1
        while not self.in_schaal(k):
            k += richting
        for _ in range(abs(n)):
            k += richting
            while not self.in_schaal(k):
                k += richting
        return k

    @staticmethod
    def dichtstbij(k, pcs, voorkeur=0):
        best = None
        for d in range(0, 12):
            for s in ((d, -d) if voorkeur >= 0 else (-d, d)):
                if (k + s) % 12 in pcs:
                    return k + s
        return best if best is not None else k

    def graad_key(self, graad, rond):
        """Scale degree index (0 = tonic, may be negative / > 6) -> the key near 'rond'."""
        o, d = divmod(graad, 7)
        k = self.grond + self.schaal[d] + 12 * o
        # put it in the octave around 'rond'
        while k < rond - 6:
            k += 12
        while k > rond + 6:
            k -= 12
        return k

    # ---- melody -------------------------------------------------------------------------------------------------------
    PATRONEN = {
        4: [[1, 1, 1, 1], [1.5, .5, 1, 1], [1, .5, .5, 2], [2, 1, 1], [.5, .5, 1, 1, 1], [1, 1, 2], [1.5, .5, 2], [1, 1, .5, .5, 1],
            [.5, .5, .5, .5, 2], [1, 2, 1]],
        3: [[2, 1], [1, 1, 1], [1.5, .5, 1], [1, 2], [.5, .5, 1, 1], [1, .5, .5, 1]],
        6: [[1, .5, 1, .5], [1.5, 1, .5], [1, .5, 1.5], [.5, .5, .5, 1, .5], [1.5, .5, .5, .5], [1, .5, .5, .5, .5]],
    }

    def sterk(self, pos):
        if self.samengesteld:
            return abs(pos) < 1e-6 or abs(pos - 1.5) < 1e-6
        if self.maat == 4:
            return abs(pos) < 1e-6 or abs(pos - 2) < 1e-6
        return abs(pos) < 1e-6

    def patronen(self, rustig):
        p = self.PATRONEN[6 if self.samengesteld else self.maat]
        if rustig:
            p = [x for x in p if len(x) <= 3] or p
        return p

    def motief(self, rustig=False):
        ritme = list(self.rng.choice(self.patronen(rustig)))
        vormen = self.VORMEN.get(len(ritme))
        if vormen:
            contour = list(self.rng.choice(vormen))
            if self.rng.random() < 0.5:
                contour = [0] + [-c for c in contour[1:]]   # the mirror shape
        else:
            contour = [0] + [self.rng.choice([-2, -1, -1, -1, 1, 1, 1, 2, 0, 3, -3, 2, -2]) for _ in ritme[1:]]
        return ritme, contour

    # melodic shapes (diatonic steps from note to note): arches, scale runs, a leap filled in by steps, neighbour notes
    VORMEN = {
        1: [[0]],
        2: [[0, 2], [0, -1], [0, 3], [0, 1], [0, 4]],
        3: [[0, 1, 1], [0, 2, -1], [0, 4, -1], [0, 1, -2], [0, -1, 2], [0, 2, 1]],
        4: [[0, 1, 1, -1], [0, 2, -1, -1], [0, -1, 3, -1], [0, 1, -1, -1], [0, 4, -1, -1], [0, 1, 1, 1], [0, -1, 1, 2]],
        5: [[0, 1, 1, 1, -2], [0, 1, -1, 2, -1], [0, 2, -1, -1, -1], [0, -1, -1, 3, -1], [0, 1, 1, -1, -1]],
        6: [[0, 1, 1, 1, -1, -1], [0, 2, -1, 2, -1, -1], [0, -1, -1, 2, 1, -2]],
    }

    def leg_motief(self, spoor, start, motief, begin, vel, lo, hi, legato=0.95):
        """Place a motif from 'begin' (key hint) at 'start'; strong beats snap to chord tones. Returns the last key."""
        ritme, contour = motief
        t = start
        k = None
        for i, (dur, c) in enumerate(zip(ritme, contour)):
            if i == 0:
                k = begin
            else:
                k = self.stap(k, c)
                if k > hi:
                    k = self.stap(k, -2 - abs(c))
                if k < lo:
                    k = self.stap(k, 2 + abs(c))
            pos = (t - start)
            if self.sterk(pos) or dur >= 1.5:
                k = self.dichtstbij(k, self.akkoord_op(t)[2], 1 if k < (lo + hi) / 2 else -1)
            k = max(lo, min(hi, k))
            v = vel + (6 if self.sterk(pos) else 0) + self.rng.randint(-4, 4)
            self.noot(spoor, t, dur * legato, k, v)
            t += dur
        return k

    def cadens(self, spoor, start, vorige, vel, slot, lo, hi):
        """A cadence bar: a short lead-in note, then a long note (half cadence: a chord tone of V; closed: the tonic)."""
        a = self.akkoord_op(start + self.maat - 0.5)
        doel_pcs = [self.grond] if slot else [p for p in a[2]]
        if self.rng.random() < 0.6:
            inloop = self.stap(vorige, self.rng.choice([-1, 1]))
            inloop = max(lo, min(hi, inloop))
            lang = self.maat - 1
            self.noot(spoor, start, 0.95, self.dichtstbij(inloop, self.akkoord_op(start)[2]), vel)
            k = self.dichtstbij(inloop, doel_pcs)
            k = max(lo, min(hi, k)) if self.in_bereik(k, lo, hi) else self.dichtstbij((lo + hi) // 2, doel_pcs)
            self.noot(spoor, start + 1, lang * 0.92, k, vel - 4)
        else:
            k = self.dichtstbij(vorige, doel_pcs)
            if not self.in_bereik(k, lo, hi):
                k = self.dichtstbij((lo + hi) // 2, doel_pcs)
            self.noot(spoor, start, self.maat * 0.9, k, vel)
        return k

    @staticmethod
    def in_bereik(k, lo, hi):
        return lo <= k <= hi

    def periode(self, spoor, start, vel, lo, hi, rustig=False, begin=None, motief=None):
        """An 8-bar period on the chords already set: A A' A half-cadence | A A'' B closed cadence. Returns the motif."""
        m = motief or self.motief(rustig)
        mid = (lo + hi) // 2
        k = begin if begin is not None else self.dichtstbij(mid, self.akkoord_op(start)[2])
        B = self.maat
        plan = ["A", "A1", "A", "H", "A", "A2", "N", "S"]
        eerste = k
        for i, p in enumerate(plan):
            t = start + i * B
            if p == "A":
                k = self.leg_motief(spoor, t, m, self.dichtstbij(eerste if i in (0, 4) else k, self.akkoord_op(t)[2]), vel, lo, hi)
            elif p in ("A1", "A2"):
                seq = 1 if p == "A1" else -1
                variant = (m[0], m[1][:-1] + [self.rng.choice([-1, 1, 2])]) if len(m[1]) > 1 else m
                k = self.leg_motief(spoor, t, variant, self.stap(eerste, seq * self.rng.choice([1, 2])), vel, lo, hi)
            elif p == "N":
                k = self.leg_motief(spoor, t, self.motief(rustig), k, vel + 4, lo, hi)
            else:
                k = self.cadens(spoor, t, k, vel, p == "S", lo, hi)
        return m

    LEIDMOTIEF = [  # (bar, beat, dur, scale degree) — 4 bars of 4/4
        (0, 0, 1, 0), (0, 1, .5, 1), (0, 1.5, 1.5, 2), (0, 3, .5, 1), (0, 3.5, .5, 0),
        (1, 0, 1, -2), (1, 1, 1, 0), (1, 2, 2, -3),
        (2, 0, 1, 0), (2, 1, .5, 1), (2, 1.5, 1.5, 2), (2, 3, .5, 4), (2, 3.5, .5, 5),
        (3, 0, 1, 4), (3, 1, .5, 3), (3, 1.5, .5, 1), (3, 2, 2, 0),
    ]
    LEIDMOTIEF_AKKOORDEN = "1 4,5 1 5,1"

    def leidmotief(self, spoor, start, vel, rond, legato=0.95):
        """The Vadsrijk leitmotif (4 bars of 4/4) from 'start', around key 'rond' (its tonic)."""
        for bar, beat, dur, g in self.LEIDMOTIEF:
            o, d = divmod(g, 7)
            k = rond + self.schaal[d] - self.schaal[0] + 12 * o
            self.noot(spoor, start + bar * 4 + beat, dur * legato, k, vel + (6 if beat == 0 else 0))

    # ---- accompaniment ------------------------------------------------------------------------------------------------
    def voicing(self, pcs, rond, vorige=None, n=3):
        """n chord notes near 'rond' (closest to the previous voicing)."""
        kand = []
        for k in range(rond - 7, rond + 9):
            if k % 12 in pcs:
                kand.append(k)
        best, bestscore = None, 1e9
        import itertools
        for combo in itertools.combinations(kand, min(n, len(kand))):
            if len(set(c % 12 for c in combo)) < min(n, len(set(pcs))):
                continue
            if max(combo) - min(combo) > 14:
                continue
            score = abs(sum(combo) / len(combo) - rond)
            if vorige:
                score = sum(min(abs(c - v) for v in vorige) for c in combo) + 0.3 * score
            if score < bestscore:
                best, bestscore = combo, score
        return list(best) if best else sorted(kand[:n])

    def pad(self, spoor, start, eind, rond, vel, n=3):
        vorige = None
        for a in self.akkoorden:
            if a[0] < start - 1e-6 or a[0] >= eind - 1e-6:
                continue
            v = self.voicing(a[2], rond, vorige, n)
            for k in v:
                self.noot(spoor, a[0], (a[1] - a[0]) * 0.98, k, vel + self.rng.randint(-3, 3))
            vorige = v

    def arpeggio(self, spoor, start, eind, rond, vel, stap=0.5, vorm="op", klinkt=1.6):
        vorige = None
        for a in self.akkoorden:
            if a[0] < start - 1e-6 or a[0] >= eind - 1e-6:
                continue
            v = self.voicing(a[2], rond, vorige, 3)
            vorige = v
            reeks = sorted(v) + [sorted(v)[0] + 12]
            if vorm == "golf":
                reeks = reeks + reeks[-2:0:-1]
            elif vorm == "neer":
                reeks = reeks[::-1]
            t = a[0]
            i = 0
            while t < a[1] - 1e-6:
                k = reeks[i % len(reeks)]
                self.noot(spoor, t, stap * klinkt, k, vel + (5 if i == 0 else 0) + self.rng.randint(-4, 3))
                t += stap
                i += 1

    def bas(self, spoor, start, eind, rond, vel, stijl="heel"):
        for a in self.akkoorden:
            if a[0] < start - 1e-6 or a[0] >= eind - 1e-6:
                continue
            r = self.dichtstbij(rond, [a[3]], -1)
            lengte = a[1] - a[0]
            if stijl == "heel" or lengte < 2:
                self.noot(spoor, a[0], lengte * 0.95, r, vel)
            elif stijl == "kwint":
                half = lengte / 2
                self.noot(spoor, a[0], half * 0.9, r, vel)
                self.noot(spoor, a[0] + half, half * 0.9, r + 7 if r + 7 <= rond + 7 else r - 5, vel - 6)
            elif stijl == "wals":
                self.noot(spoor, a[0], 0.95, r, vel)
                if lengte >= 3:
                    self.noot(spoor, a[0] + 2, 0.9, r + 7 if r + 7 <= rond + 7 else r - 5, vel - 10)
            elif stijl == "kwart":
                t = a[0]
                i = 0
                while t < a[1] - 1e-6:
                    k = r if i % 2 == 0 else (r + 7 if r + 7 <= rond + 7 else r - 5)
                    self.noot(spoor, t, 0.8, k, vel - (0 if i % 2 == 0 else 8))
                    t += 1
                    i += 1

    def walsbegeleiding(self, spoor, start, eind, rond, vel):
        """Beats 2 and 3 (or 2-3-4): short chord plucks."""
        vorige = None
        for a in self.akkoorden:
            if a[0] < start - 1e-6 or a[0] >= eind - 1e-6:
                continue
            v = self.voicing(a[2], rond, vorige, 3)
            vorige = v
            t = a[0] + 1
            while t < a[1] - 1e-6:
                for k in v:
                    self.noot(spoor, t, 0.6, k, vel + self.rng.randint(-3, 3))
                t += 1

    def tegenstem(self, spoor, start, eind, rond, vel):
        """A slow counter line: per chord the 3rd or 7th nearest the previous note (long notes)."""
        k = rond
        for a in self.akkoorden:
            if a[0] < start - 1e-6 or a[0] >= eind - 1e-6:
                continue
            pcs = a[2]
            doel = [pcs[1]] + ([pcs[3]] if len(pcs) > 3 else [])
            k = self.dichtstbij(k, doel)
            if abs(k - rond) > 8:
                k = self.dichtstbij(rond, doel)
            self.noot(spoor, a[0], (a[1] - a[0]) * 0.97, k, vel + self.rng.randint(-3, 3))

    def ostinato(self, spoor, start, eind, rond, vel, figuur=(0, 0, 7, 0, 12, 0, 7, 0), stap=0.5):
        """A driving figure on the chord root (bosses): semitone offsets per step."""
        for a in self.akkoorden:
            if a[0] < start - 1e-6 or a[0] >= eind - 1e-6:
                continue
            r = self.dichtstbij(rond, [a[3]], -1)
            t = a[0]
            i = 0
            while t < a[1] - 1e-6:
                off = figuur[i % len(figuur)]
                k = r + off
                if (k - a[3]) % 12 == 7 and (r + 7) % 12 not in a[2]:
                    k = self.dichtstbij(k, a[2])
                self.noot(spoor, t, stap * 0.8, k, vel + (10 if i % 4 == 0 else 0) + self.rng.randint(-3, 3))
                t += stap
                i += 1

    def slag(self, spoor, start, eind, patroon, maten=None):
        """Percussion: patroon = [(beat, key, vel)] per bar, repeated from start to end."""
        t = start
        while t < eind - 1e-6:
            for beat, key, vel in patroon:
                if t + beat < eind - 1e-6:
                    self.noot(spoor, t + beat, 0.4, key, vel + self.rng.randint(-5, 4))
            t += self.maat

    def lengte(self):
        return max((s + d for v in self.sporen.values() for (s, d, k, _) in v[3]), default=0.0)


# ---- MIDI out -------------------------------------------------------------------------------------------------------------

def schrijf_midi(stuk, pad, tpq=480):
    import mido
    mf = mido.MidiFile(ticks_per_beat=tpq, type=1)
    meta = mido.MidiTrack()
    mf.tracks.append(meta)
    meta.append(mido.MetaMessage("track_name", name=stuk.naam, time=0))
    meta.append(mido.MetaMessage("set_tempo", tempo=int(60_000_000 / stuk.bpm), time=0))
    num, den = (6, 8) if stuk.samengesteld else (stuk.maat, 4)
    meta.append(mido.MetaMessage("time_signature", numerator=num, denominator=den, time=0))
    kanaal = 0
    for naam, (instr, gain, galm, noten) in stuk.sporen.items():
        bank, prog = INSTR[instr]
        drum = bank == 128
        ch = 9 if drum else kanaal
        if not drum:
            kanaal += 1
            if kanaal == 9:
                kanaal = 10
        tr = mido.MidiTrack()
        mf.tracks.append(tr)
        tr.append(mido.MetaMessage("track_name", name=f"{naam}|{instr}|{gain}|{galm}", time=0))
        tr.append(mido.Message("control_change", channel=ch, control=0, value=0 if drum else bank, time=0))
        tr.append(mido.Message("program_change", channel=ch, program=prog, time=0))
        ev = []
        for s, d, k, v in noten:
            ev.append((int(round(s * tpq)), 1, k, v))
            ev.append((int(round((s + d) * tpq)), 0, k, 0))
        ev.sort(key=lambda e: (e[0], e[1]))
        nu = 0
        for tick, aan, k, v in ev:
            tr.append(mido.Message("note_on" if aan else "note_off", channel=ch, note=k, velocity=v if aan else 0, time=tick - nu))
            nu = tick
    mf.save(pad)
