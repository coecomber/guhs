"""
Guhs 1.7.0 "Onder de Wol": the music and ambience of the Guhmension's caves, synthesised (numpy) and written ONCE into
assets/guhs/sounds/onderwol/ (vorbis encoding is not byte-for-byte stable, so an existing file is never rewritten; delete
it, or pass --opnieuw, to make it again).

The user found the vanilla cave sounds (minecraft:ambient.cave, the eerie rumbles and whispers) scary and nervous: "totaal
niet toepasselijk bij de guhs mod". So under the wool everything is cheerful, cute and soothing:

  muziek_<grot>.ogg   every cave biome its own little song (about 80-100 s, mono, streamed), all in a major key, built from
                      consonant chords and pentatonic tunes (music box, celesta, marimba, kalimba, pizzicato, harp, soft
                      pads), with a soft cave echo. See MUZIEK below for each one's character.
  sfeer_druppels      ambience loops (seamless, 18 s): tuned water drops plinking into little pools / a soft fluffy breeze
  sfeer_zacht         with a few fluff tinkles / the slow breathing of sleeping guhs / faint bells echoing far away.
  sfeer_slaap         (Unpitched or very short sounds only, so they never clash with the music's key.)
  sfeer_belletjes
  plink1-4            the "mood" sound (what vanilla plays as the scary cave noise in the dark): one or two drops plinking
  tingel1-3           a tiny crystal bell arpeggio (an addition: now and then)
  (giechel / snurkje are events on the mod's own guh voice and the vanilla fox snore, like knuffeldal.kruimel_giechel and
  huisje.snurk.)

And then it patches the audio of every Guhmension biome (data/guhs/dimension/guhmension.json plus the cave biomes in
GROTTEN): no minecraft:ambient.cave anywhere any more (the mood becomes guhs:onderwol.plink), and every cave biome gets its
own loop, addition and music. A cave biome file that does not exist yet is skipped silently (rerun later). Both biome
shapes are handled: the 26.1 "attributes" shape and the old 1.21.1 "effects" shape (mc26.py converts that one later).

Standalone, from the worktree root:

    PYTHONPATH=tools python tools/features/onderwol_geluid.py            (--opnieuw: rewrite the ogg files too)

or from a generator: onderwol_geluid.build(h) with h.A, h.D, h.patch_json and h.lang (the make_resources helper), AFTER
all the biome files are written (the older biome modules still write ambient.cave as their mood).
Needs numpy, scipy and PyAV, like the mod's other sound scripts.
"""
import json
import os
import sys

import numpy as np
try:                                    # (only needed to MAKE the sounds)
    from scipy.signal import butter, lfilter
except ImportError:
    butter = lfilter = None

SR = 44100
MAP = "onderwol"
RMS_MUZIEK = 0.075          # klaterdal/muziek.ogg is 0.060 (sparse), bloesemmeertje 0.079: about the same loudness
PIEK = 0.62

# ----------------------------------------------------------------------------------------------------------------------
# the cave biomes: (loop, addition, addition chance, music min delay, music max delay)
GROTTEN = {
    "gatenkaasgrotten":  ("druppels", "giechel", 0.0009),
    "guh_kristalmijn":   ("druppels", "tingel", 0.0012),
    "glitterspelonk":    ("belletjes", "tingel", 0.0010),
    "pluisgrot":         ("zacht", "giechel", 0.0009),
    "kristalpegelgrot":  ("druppels", "tingel", 0.0012),
    "knuffelwortelgrot": ("zacht", "giechel", 0.0010),
    "slaapgrot":         ("slaap", "snurkje", 0.0014),
    "belletjesgrot":     ("belletjes", "tingel", 0.0015),
}
MUZIEK_DELAY = (1200, 4800)     # (the Klaterdal: 3600-9600; in a cave the friendly music comes back a bit sooner)
MOOD = {"sound": "guhs:onderwol.plink", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}
ENG = "minecraft:ambient.cave"

TEKSTEN = {   # key: (nl, en)
    "subtitles.guhs.onderwol.plink": ("Druppeltje plinkt", "Droplet plinks"),
    "subtitles.guhs.onderwol.tingel": ("Kristalbelletje tingelt", "Crystal bell tinkles"),
    "subtitles.guhs.onderwol.giechel": ("Verre guh giechelt", "Distant guh giggles"),
    "subtitles.guhs.onderwol.snurkje": ("Guh snurkt zachtjes", "Guh snores softly"),
    "subtitles.guhs.onderwol.sfeer.druppels": ("Druppels tikken", "Drops drip"),
    "subtitles.guhs.onderwol.sfeer.zacht": ("Zacht grotbriesje", "Soft cave breeze"),
    "subtitles.guhs.onderwol.sfeer.slaap": ("Guhs ademen zachtjes", "Guhs breathe softly"),
    "subtitles.guhs.onderwol.sfeer.belletjes": ("Belletjes galmen", "Little bells echo"),
}


# ----------------------------------------------------------------------------------------------------------------------
# small synth kit

def _t(sec):
    return np.arange(int(SR * sec)) / SR


def _plak(out, geluid, start, volume):
    s = int(round(start * SR))
    if s >= len(out) or s < 0:
        return
    e = min(len(out), s + len(geluid))
    out[s:e] += geluid[:e - s] * volume


def _laag(x, f, orde=2):
    b, a = butter(orde, min(0.99, f / (SR / 2)), btype="low")
    return lfilter(b, a, x)


def _hoog(x, f, orde=2):
    b, a = butter(orde, f / (SR / 2), btype="high")
    return lfilter(b, a, x)


def _hz(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def _partials(f, lengte, delen, aanslag=0.002):
    """A sum of sine partials (ratio, amplitude, decay per second); partials above 7 kHz are left out (no harsh highs)."""
    t = _t(lengte)
    s = np.zeros_like(t)
    for r, a, d in delen:
        if f * r < 7000:
            s += a * np.sin(2 * np.pi * f * r * t) * np.exp(-t * d)
    return s * np.minimum(1, t / aanslag)


def speeldoos(f, lengte=2.6):        # a music box tine: pure, a little bright ping that dies fast
    return _partials(f, lengte, ((1, 1.0, 1.6), (2, 0.18, 4.5), (3.0, 0.05, 9), (4.1, 0.04, 16)), 0.0015)


def celesta(f, lengte=2.4):
    return _partials(f, lengte, ((1, 1.0, 2.0), (2, 0.30, 4.0), (3, 0.08, 7.0)), 0.002)


def glock(f, lengte=2.2):            # glockenspiel / little crystal bell
    return _partials(f, lengte, ((1, 1.0, 2.2), (2.76, 0.22, 5.0), (5.4, 0.05, 11)), 0.0015)


def bel(f, lengte=4.0):              # a round bell with a long ring
    return _partials(f, lengte, ((1, 1.0, 0.9), (2.0, 0.35, 1.8), (2.76, 0.18, 2.6), (4.07, 0.06, 5)), 0.002)


def marimba(f, lengte=1.4):
    s = _partials(f, lengte, ((1, 1.0, 4.0), (3.93, 0.22, 14), (9.9, 0.04, 30)), 0.003)
    rng = np.random.default_rng(int(f * 7))
    tik = _laag(rng.normal(0, 1, len(s)), 1800) * np.exp(-_t(lengte) * 220) * 0.10
    return s + tik


def kalimba(f, lengte=1.8):
    return _partials(f, lengte, ((1, 1.0, 2.6), (2, 0.06, 6), (5.9, 0.10, 20)), 0.002)


def pizz(f, lengte=0.9):             # pizzicato string
    return _partials(f, lengte, tuple((k, 1.0 / k ** 1.2, 5 + 3.5 * k) for k in range(1, 7)), 0.004)


def harp(f, lengte=3.0):
    return _partials(f, lengte, tuple((k, 1.0 / k ** 1.6, 1.0 + 0.9 * k) for k in range(1, 7)), 0.003)


def vibes(f, lengte=2.8):
    t = _t(lengte)
    s = _partials(f, lengte, ((1, 1.0, 1.2), (4.0, 0.08, 6)), 0.003)
    return s * (1 - 0.18 * (0.5 - 0.5 * np.cos(2 * np.pi * 4.8 * t)))


def bas(f, lengte=1.6):              # a soft round bass
    return _partials(f, lengte, ((1, 1.0, 2.2), (2, 0.28, 4.0), (3, 0.06, 7)), 0.012)


def fluit(f, lengte):
    """A soft flute: a breathy sine with a gentle vibrato that swells in and out."""
    t = _t(lengte)
    rng = np.random.default_rng(int(f))
    vib = 1 + 0.004 * np.sin(2 * np.pi * 5.0 * t) * np.minimum(1, t / 0.6)
    fase = 2 * np.pi * np.cumsum(f * vib) / SR
    s = np.sin(fase) + 0.15 * np.sin(2 * fase) + 0.04 * np.sin(3 * fase)
    adem = _laag(rng.normal(0, 1, len(t)), 2400) * 0.05
    env = np.minimum(1, t / 0.12) * np.minimum(1, np.maximum(0, lengte - t) / 0.25)
    return (s + adem) * env


def pad(freqs, lengte):
    """A warm soft "ooh" pad over a chord: slightly detuned sines, slow swell in and out."""
    t = _t(lengte)
    s = np.zeros_like(t)
    for f in freqs:
        for d in (-0.0025, 0.0, 0.0025):
            s += np.sin(2 * np.pi * f * (1 + d) * t + 7 * f * d) + 0.12 * np.sin(4 * np.pi * f * (1 + d) * t)
    s /= max(1, len(freqs) * 3)
    env = np.minimum(1, t / min(0.9, lengte / 3)) * np.minimum(1, np.maximum(0, lengte - t) / min(1.0, lengte / 3))
    return s * env


def tok(f=820, lengte=0.12):         # a little wooden "tok"
    t = _t(lengte)
    return np.sin(2 * np.pi * f * t * (1 - 0.15 * t / lengte)) * np.exp(-t * 55) * np.minimum(1, t / 0.003)


def druppel(f, lengte=0.5):
    """A water drop: a short sine that slides up quickly (the "bloop"), then a soft tuned ring."""
    t = _t(lengte)
    glij = f * (0.72 + 0.28 * (1 - np.exp(-t * 60)))
    fase = 2 * np.pi * np.cumsum(glij) / SR
    return np.sin(fase) * np.exp(-t * 11) * np.minimum(1, t / 0.002) + 0.25 * np.sin(2 * fase) * np.exp(-t * 30)


def _kam(x, n, g):
    """y[i] = x[i] + g * y[i - n], block by block (an IIR of order n through lfilter would be far too slow)."""
    y = x.copy()
    for s in range(n, len(y), n):
        e = min(len(y), s + n)
        y[s:e] += g * y[s - n:e - n]
    return y


def _allpass(x, n, g):
    """y[i] = -g x[i] + x[i - n] + g y[i - n]"""
    y = -g * x
    y[n:] += x[:-n]
    for s in range(n, len(y), n):
        e = min(len(y), s + n)
        y[s:e] += g * y[s - n:e - n]
    return y


def galm(x, nat=0.28, groot=1.0):
    """A friendly cave room: a small Schroeder reverb (four combs, two all-passes), mixed in softly."""
    nat_sig = np.zeros_like(x)
    for ms, g in ((29.7, 0.80), (37.1, 0.79), (41.1, 0.78), (43.7, 0.77)):
        nat_sig += _kam(x, int(SR * ms * groot / 1000), g)
    for ms, g in ((5.0, 0.7), (1.7, 0.7)):
        nat_sig = _allpass(nat_sig, int(SR * ms / 1000), g)
    nat_sig = _laag(nat_sig, 5000)
    return x + nat * nat_sig / 4


def echo(x, tijd, terug, keer=4):
    y = x.copy()
    n = int(SR * tijd)
    for k in range(1, keer + 1):
        if n * k < len(x):
            y[n * k:] += x[:-n * k] * terug ** k
    return y


def _meester(x, rms=RMS_MUZIEK, piek=PIEK, intro=1.0, outro=5.0, op_piek=False):
    x = _laag(x, 9000)
    x = x - np.mean(x)
    if op_piek:                                # (ambience: the loudest drop/bell sets the level, the rest stays soft)
        x *= piek / max(1e-9, np.max(np.abs(x)))
        return x
    x *= rms / max(1e-9, np.sqrt(np.mean(x ** 2)))
    x = piek * np.tanh(x / piek)               # (soft: only the very loudest peaks are rounded off)
    n = int(SR * intro)
    x[:n] *= np.linspace(0, 1, n)
    n = int(SR * outro)
    x[-n:] *= np.linspace(1, 0, n) ** 1.5
    return x


# ----------------------------------------------------------------------------------------------------------------------
# a tiny composer: major keys, consonant chords, pentatonic tunes

MAJEUR = (0, 2, 4, 5, 7, 9, 11)
PENTA = {0, 1, 2, 4, 5}             # do re mi sol la


def _midi(tonica, graad):
    return tonica + MAJEUR[graad % 7] + 12 * (graad // 7)


def _akkoord(grondtoon):
    return (grondtoon, grondtoon + 2, grondtoon + 4)


RITMES = {
    4: ([1, 1, 1, 1], [0.5, 0.5, 1, 1, 1], [1, 0.5, 0.5, 2], [1.5, 0.5, 1, 1], [2, 1, 1], [0.5, 0.5, 0.5, 0.5, 1, 1],
        [1, 1, 2], [0.5, 0.5, 1, 0.5, 0.5, 1]),
    3: ([1, 1, 1], [2, 1], [1, 0.5, 0.5, 1], [0.5, 0.5, 1, 1], [1.5, 0.5, 1]),
}
SLOT = {4: [2, 2], 3: [3]}


def melodie(rng, akkoorden, maat, hoog=7, motief_herhaal=True):
    """A tune over the chords (one chord root per bar): strong beats on chord tones, the rest stepping through the
    pentatonic, a rhythm motif that comes back every two bars, a half cadence halfway and home (do) at the end.
    Returns [(beat, length, scale degree)]."""
    n = len(akkoorden)
    motief = [list(RITMES[maat][rng.integers(len(RITMES[maat]))]) for _ in range(2)]
    noten, graad, tel = [], hoog, 0.0
    richting = 1
    for i, ak in enumerate(akkoorden):
        if i == n - 1:
            ritme = SLOT[maat]
        elif motief_herhaal:
            ritme = motief[i % 2]
        else:
            ritme = list(RITMES[maat][rng.integers(len(RITMES[maat]))])
        pos = 0.0
        for j, lengte in enumerate(ritme):
            sterk = pos == 0 or (maat == 4 and pos == 2)
            tonen = [g for g in range(hoog - 5, hoog + 7) if (g - ak) % 7 in (0, 2, 4)]
            if i == n - 1 and j == len(ritme) - 1:
                # (home on do over the I chord; over another chord, e.g. the V that leads back into the A part, its root)
                doel = [g for g in range(hoog - 5, hoog + 7) if (g - ak) % 7 == 0]
                graad = min(doel, key=lambda g: abs(g - graad))
            elif i == n // 2 - 1 and j == len(ritme) - 1:
                doel = [g for g in tonen if g % 7 in (1, 4, 6)] or tonen
                graad = min(doel, key=lambda g: abs(g - graad) + (0 if g % 7 == 1 else 0.5))
            elif sterk:
                kandidaten = sorted(tonen, key=lambda g: abs(g - graad) + rng.uniform(0, 1.6))
                graad = kandidaten[0] if kandidaten[0] != graad or rng.random() < 0.3 else kandidaten[1]
            else:
                if rng.random() < 0.25:
                    richting = -richting
                stap = richting * (1 if rng.random() < 0.8 else 2)
                nieuw = graad + stap
                while nieuw % 7 not in PENTA and (nieuw - ak) % 7 not in (0, 2, 4):
                    nieuw += richting
                graad = nieuw
                if graad > hoog + 5:
                    richting = -1
                elif graad < hoog - 4:
                    richting = 1
            noten.append((tel + pos, lengte, graad))
            pos += lengte
        tel += maat
    return noten


class Stuk:
    """One song: a key, a tempo, a meter, and a buffer to play notes into."""

    def __init__(self, tonica, bpm, maat):
        self.tonica, self.tel, self.maat = tonica, 60.0 / bpm, maat
        self.out = np.zeros(int(SR * 150))
        self.eind = 0.0

    def sec(self, tel):
        return tel * self.tel

    def noot(self, instr, graad, tel, volume, octaaf=0, **kw):
        _plak(self.out, instr(_hz(_midi(self.tonica, graad) + 12 * octaaf), **kw), self.sec(tel), volume)

    def tune(self, noten, instr, start, volume, octaaf=0, swing=0.0, staccato=None):
        for tel, lengte, graad in noten:
            t = tel + (swing if (tel % 1) == 0.5 else 0)
            kw = {}
            if instr is fluit:
                kw["lengte"] = max(0.25, self.sec(lengte) * 0.95)
            self.noot(instr, graad, start + t, volume * (1.0 if (tel % 1) == 0 else 0.82), octaaf, **kw)

    def arpeggio(self, akkoorden, instr, start, volume, patroon=(0, 1, 2, 3, 2, 1, 0, 1), stap=0.5, octaaf=0):
        for i, ak in enumerate(akkoorden):
            tonen = [ak, ak + 2, ak + 4, ak + 7]
            for k, p in enumerate(patroon):
                if k * stap >= self.maat:
                    break
                self.noot(instr, tonen[p], start + i * self.maat + k * stap, volume * (1.0 if k == 0 else 0.8), octaaf)

    def bas(self, akkoorden, instr, start, volume, tellen=(0,), octaaf=-2, quint=False):
        for i, ak in enumerate(akkoorden):
            for k, t in enumerate(tellen):
                g = ak + 4 if (quint and k % 2 == 1) else ak
                self.noot(instr, g, start + i * self.maat + t, volume, octaaf)

    def hum(self, akkoorden, start, volume, octaaf=-1):
        for i, ak in enumerate(akkoorden):
            freqs = [_hz(_midi(self.tonica, g) + 12 * octaaf) for g in _akkoord(ak)]
            _plak(self.out, pad(freqs, self.sec(self.maat) + 0.6), self.sec(start + i * self.maat), volume)

    def akkoordslag(self, akkoorden, instr, start, volume, tellen, octaaf=0):
        for i, ak in enumerate(akkoorden):
            for t in tellen:
                for g in _akkoord(ak):
                    self.noot(instr, g, start + i * self.maat + t, volume / 2.2, octaaf)

    def klaar(self, nat=0.3, groot=1.3, extra_echo=None):
        x = self.out
        if extra_echo:
            x = echo(x, *extra_echo)
        x = galm(x, nat, groot)
        x = x[:int(SR * (self.sec(self.eind) + 4.5))]     # (the last chord rings out under the fade)
        return _meester(x)


# chord progressions (one root degree per bar): I=0 ii=1 iii=2 IV=3 V=4 vi=5
P_POP = [0, 5, 3, 4]
P_PAD = [0, 3, 0, 4]
P_LIEF = [0, 4, 5, 3]
P_DROOM = [0, 2, 3, 0]
P_BRUG = [3, 0, 3, 4]
P_BRUG2 = [5, 3, 0, 4]
P_SLOT = [3, 4, 0, 0]


def _vorm(stuk, plan, spelen, slot_instr=None, slot_vol=0.4):
    """plan: [(chords of the section, label)]; spelen(label, chords, start) plays one section. The last section is the
    ending: the lead instrument (slot_instr) gives a last high do at its third bar."""
    tel = 0
    for akk, label in plan:
        spelen(label, akk, tel)
        if label == "slot" and slot_instr:
            stuk.noot(slot_instr, 7, tel + 2 * stuk.maat, slot_vol)
            stuk.noot(slot_instr, 4, tel + 2 * stuk.maat + 0.02, slot_vol * 0.5)
        tel += len(akk) * stuk.maat
    stuk.eind = tel


# ----------------------------------------------------------------------------------------------------------------------
# the eight songs

def muziek_gatenkaasgrotten():
    """Hopping from cheese hole to cheese hole: a bouncy marimba tune with a light swing, pizzicato bass on 1 and 3,
    a soft wooden tok on the off-beats and a music box answering. F major, 104 bpm."""
    rng = np.random.default_rng(170001)
    s = Stuk(65, 104, 4)
    a = P_POP + [0, 5, 4, 0]
    b = P_BRUG + [5, 3, 1, 4]
    ma, mb = melodie(rng, a, 4), melodie(rng, b, 4)

    def sectie(label, akk, t):
        s.bas(akk, pizz, t, 0.55, tellen=(0, 2), quint=True)
        s.akkoordslag(akk, marimba, t, 0.35, tellen=(1, 3), octaaf=-1)
        for i in range(len(akk) * 4):
            if label != "intro":
                _plak(s.out, tok(780 + 120 * (i % 2)), s.sec(t + i + 0.5), 0.10)
        if label == "A":
            s.tune(ma, marimba, t, 0.55, swing=0.08)
        elif label == "A2":
            s.tune(ma, marimba, t, 0.5, swing=0.08)
            s.tune(ma, speeldoos, t, 0.22, octaaf=1, swing=0.08)
        elif label == "B":
            s.tune(mb, speeldoos, t, 0.40, swing=0.08)
            s.hum(akk, t, 0.12)
    _vorm(s, [(P_POP, "intro"), (a, "A"), (a, "A2"), (b, "B"), (a, "A2"), (P_SLOT, "slot")], sectie, marimba, 0.5)
    return s.klaar(nat=0.26, groot=1.2)


def muziek_guh_kristalmijn():
    """The happy little mining song: a kalimba tune over a steady soft bass, glockenspiel sparkles like tiny pickaxe
    tinks on the off-beats, and a warm hum in the middle part. G major, 96 bpm."""
    rng = np.random.default_rng(170002)
    s = Stuk(67, 96, 4)
    a = P_PAD + [0, 3, 4, 0]
    b = P_LIEF + [3, 0, 1, 4]
    ma, mb = melodie(rng, a, 4), melodie(rng, b, 4, hoog=9)

    def sectie(label, akk, t):
        s.bas(akk, bas, t, 0.55, tellen=(0, 1.5, 2), quint=True)
        s.arpeggio(akk, kalimba, t, 0.22, patroon=(0, 2, 1, 2, 0, 2, 1, 2), octaaf=-1)
        if label != "intro":
            for i, ak in enumerate(akk):
                for k in (1.5, 3.5):
                    s.noot(glock, ak + 4, t + i * 4 + k, 0.07, octaaf=1)
        if label == "A":
            s.tune(ma, kalimba, t, 0.6)
        elif label == "B":
            s.tune(mb, celesta, t, 0.42)
            s.hum(akk, t, 0.14)
        elif label == "A2":
            s.tune(ma, kalimba, t, 0.55)
            s.tune(ma, glock, t, 0.12, octaaf=1)
    _vorm(s, [(P_PAD, "intro"), (a, "A"), (b, "B"), (P_PAD, "intro"), (a, "A2"), (P_SLOT, "slot")], sectie, kalimba, 0.5)
    return s.klaar(nat=0.3, groot=1.3)


def muziek_glitterspelonk():
    """Sparkles everywhere: rippling celesta arpeggios in sixteenths over a soft dreamy pad, a slow glockenspiel tune
    floating on top, and a vibraphone answer. E major, 84 bpm."""
    rng = np.random.default_rng(170003)
    s = Stuk(64, 84, 4)
    a = P_DROOM + [0, 2, 4, 0]
    b = [3, 4, 2, 5, 3, 4, 0, 0]
    ma, mb = melodie(rng, a, 4, hoog=9), melodie(rng, b, 4, hoog=7)

    def sectie(label, akk, t):
        s.hum(akk, t, 0.28)
        s.bas(akk, bas, t, 0.35, tellen=(0,))
        s.arpeggio(akk, celesta, t, 0.16, patroon=(0, 1, 2, 3, 2, 1, 0, 1, 0, 1, 2, 3, 2, 1, 0, 1), stap=0.25)
        if label == "A":
            s.tune(ma, glock, t, 0.32)
        elif label == "B":
            s.tune(mb, vibes, t, 0.45)
    _vorm(s, [(P_DROOM, "intro"), (a, "A"), (b, "B"), (a, "A"), (P_SLOT, "slot")], sectie, glock, 0.3)
    return s.klaar(nat=0.36, groot=1.5)


def muziek_pluisgrot():
    """Soft as fluff: a gentle waltz, a breathy flute tune over harp chords (bass on one, chord on two and three) and a
    cushion of warm pad. C major, 3/4, 76 bpm."""
    rng = np.random.default_rng(170004)
    s = Stuk(60, 76, 3)
    a = [0, 3, 0, 4, 0, 3, 4, 0]
    b = [5, 3, 0, 4, 5, 1, 4, 4]
    ma, mb = melodie(rng, a, 3), melodie(rng, b, 3)

    def sectie(label, akk, t):
        s.bas(akk, harp, t, 0.45, tellen=(0,), octaaf=-2)
        s.akkoordslag(akk, harp, t, 0.30, tellen=(1, 2), octaaf=-1)
        s.hum(akk, t, 0.16)
        if label == "A":
            s.tune(ma, fluit, t, 0.30)
        elif label == "B":
            s.tune(mb, celesta, t, 0.38)
        elif label == "A2":
            s.tune(ma, fluit, t, 0.28)
            s.tune(ma, speeldoos, t, 0.14, octaaf=1)
    _vorm(s, [([0, 3, 0, 4], "intro"), (a, "A"), (b, "B"), ([0, 3, 0, 4], "intro"), (a, "A2"), ([3, 4, 0, 0], "slot")], sectie,
          celesta, 0.3)
    return s.klaar(nat=0.3, groot=1.4)


def muziek_kristalpegelgrot():
    """Drip, drop, plink: a music-box tune with little falling "drip" notes in pizzicato between the phrases, a bouncy
    pizzicato bass and a soft marimba off-beat. Bb major, 92 bpm."""
    rng = np.random.default_rng(170005)
    s = Stuk(70, 92, 4)
    a = P_LIEF + [0, 3, 4, 0]
    b = [3, 4, 2, 5, 1, 4, 0, 4]
    ma, mb = melodie(rng, a, 4, hoog=7), melodie(rng, b, 4, hoog=9)

    def druppels(t, ak):
        for k, g in enumerate((ak + 7, ak + 4, ak + 2)):
            s.noot(pizz, g, t + 3 + k * 0.33, 0.20, octaaf=1)

    def sectie(label, akk, t):
        s.bas(akk, pizz, t, 0.55, tellen=(0, 2), octaaf=-2, quint=True)
        s.akkoordslag(akk, marimba, t, 0.22, tellen=(1, 3), octaaf=-1)
        if label == "A":
            s.tune(ma, speeldoos, t, 0.50)
            for i in (1, 3, 5):
                druppels(t + i * 4, akk[i])
        elif label == "B":
            s.tune(mb, celesta, t, 0.40)
            s.hum(akk, t, 0.12)
        elif label == "intro":
            for i in range(len(akk)):
                druppels(t + i * 4, akk[i])
    _vorm(s, [(P_LIEF, "intro"), (a, "A"), (b, "B"), (a, "A"), (P_SLOT, "slot")], sectie, speeldoos, 0.45)
    return s.klaar(nat=0.32, groot=1.4)


def muziek_knuffelwortelgrot():
    """Among the cuddly roots: a warm, lazy groove, kalimba tune with a soft flute duet in the second half, marimba bass
    and a gentle shaker of soft toks. D major, 88 bpm."""
    rng = np.random.default_rng(170006)
    s = Stuk(62, 88, 4)
    a = P_LIEF + [0, 4, 3, 0]
    b = [3, 0, 3, 4, 5, 3, 1, 4]
    ma, mb = melodie(rng, a, 4), melodie(rng, b, 4, hoog=9)

    def sectie(label, akk, t):
        s.bas(akk, marimba, t, 0.55, tellen=(0, 2.5), octaaf=-2, quint=True)
        s.hum(akk, t, 0.20)
        for i in range(len(akk) * 4):
            _plak(s.out, tok(620, 0.1), s.sec(t + i + 0.5), 0.06)
        if label == "A":
            s.tune(ma, kalimba, t, 0.60)
        elif label == "B":
            s.tune(mb, fluit, t, 0.26)
            s.arpeggio(akk, kalimba, t, 0.14, patroon=(0, 1, 2, 1, 0, 1, 2, 1))
        elif label == "A2":
            s.tune(ma, kalimba, t, 0.55)
            s.tune(ma, fluit, t, 0.14, octaaf=1)
    _vorm(s, [(P_LIEF, "intro"), (a, "A"), (b, "B"), (a, "A2"), (P_SLOT, "slot")], sectie, kalimba, 0.5)
    return s.klaar(nat=0.3, groot=1.3)


def muziek_slaapgrot():
    """A lullaby for the sleeping guhs: a slow music-box waltz, very soft pad and harp, a celesta that sings the tune
    once more an octave higher, and long gentle rests. F major, 3/4, 62 bpm."""
    rng = np.random.default_rng(170007)
    s = Stuk(65, 62, 3)
    a = [0, 3, 0, 4, 0, 3, 4, 0]
    b = [3, 0, 1, 4, 3, 0, 4, 0]
    ma, mb = melodie(rng, a, 3, hoog=7), melodie(rng, b, 3, hoog=7)

    def sectie(label, akk, t):
        s.hum(akk, t, 0.24)
        s.bas(akk, harp, t, 0.30, tellen=(0,), octaaf=-2)
        s.akkoordslag(akk, harp, t, 0.14, tellen=(1,), octaaf=-1)
        if label == "A":
            s.tune(ma, speeldoos, t, 0.50)
        elif label == "B":
            s.tune(mb, speeldoos, t, 0.45)
        elif label == "A2":
            s.tune(ma, celesta, t, 0.32, octaaf=1)
            s.tune(ma, speeldoos, t, 0.30)
    _vorm(s, [([0, 3, 0, 4], "intro"), (a, "A"), (b, "B"), (a, "A2"), ([3, 4, 0, 0], "slot")], sectie, speeldoos, 0.4)
    return s.klaar(nat=0.38, groot=1.6)


def muziek_belletjesgrot():
    """The singing bell cave: a call-and-response of round bells and a vibraphone, every note echoing back from the
    crystal walls (a tempo-synced echo), over slow bass bells and a soft pad. C major pentatonic, 80 bpm."""
    rng = np.random.default_rng(170008)
    s = Stuk(60, 80, 4)
    a = P_PAD + [5, 3, 4, 0]
    b = P_DROOM + [3, 0, 1, 4]
    ma, mb = melodie(rng, a, 4, hoog=7), melodie(rng, b, 4, hoog=9)

    def vraag_antwoord(noten, t, instr1, instr2, vol):
        for tel, lengte, graad in noten:
            maat = int(tel // 4)
            instr = instr1 if maat % 2 == 0 else instr2
            s.noot(instr, graad, t + tel, vol)

    def sectie(label, akk, t):
        s.hum(akk, t, 0.18)
        s.bas(akk, bel, t, 0.30, tellen=(0,), octaaf=-2)
        if label == "A":
            vraag_antwoord(ma, t, bel, vibes, 0.40)
        elif label == "B":
            vraag_antwoord(mb, t, glock, bel, 0.30)
        elif label == "intro":
            s.arpeggio(akk, glock, t, 0.14, patroon=(0, 1, 2, 3), stap=1.0, octaaf=1)
    _vorm(s, [(P_PAD, "intro"), (a, "A"), (b, "B"), (a, "A"), (P_SLOT, "slot")], sectie, bel, 0.4)
    return s.klaar(nat=0.4, groot=1.6, extra_echo=(s.sec(0.75), 0.32, 3))


MUZIEK = {
    "gatenkaasgrotten": muziek_gatenkaasgrotten,
    "guh_kristalmijn": muziek_guh_kristalmijn,
    "glitterspelonk": muziek_glitterspelonk,
    "pluisgrot": muziek_pluisgrot,
    "kristalpegelgrot": muziek_kristalpegelgrot,
    "knuffelwortelgrot": muziek_knuffelwortelgrot,
    "slaapgrot": muziek_slaapgrot,
    "belletjesgrot": muziek_belletjesgrot,
}

# ----------------------------------------------------------------------------------------------------------------------
# ambience

PENTA_HOOG = [_hz(m) for m in (84, 86, 88, 91, 93, 96)]          # C6 D6 E6 G6 A6 C7: drops and bells, always friendly


def _naadloos(out, lengte, overlap):
    k = int(SR * overlap)
    kop, staart = out[:k].copy(), out[-k:].copy()
    f = np.linspace(0, 1, k)
    out[:k] = kop * np.sqrt(f) + staart * np.sqrt(1 - f)
    return out[:int(SR * lengte)]


def _lucht(rng, n, kantel=260, adem=9.0):
    """A very soft breath of air (no low rumble: everything under 120 Hz is filtered out); adem must divide the loop."""
    t = np.arange(n) / SR
    return _hoog(_laag(rng.normal(0, 1, n), kantel + 300), 120) * (0.6 + 0.4 * np.sin(2 * np.pi * t / adem)) * 0.25


def sfeer_druppels():
    lengte, overlap = 18.0, 2.0
    rng = np.random.default_rng(170101)
    n = int(SR * (lengte + overlap))
    out = _lucht(rng, n) * 0.9
    for _ in range(int(lengte * 1.3)):
        f = PENTA_HOOG[rng.integers(5)] * (0.5 if rng.random() < 0.4 else 1.0)
        _plak(out, druppel(f), rng.uniform(0, lengte + overlap - 0.6), rng.uniform(0.05, 0.16))
    out = galm(out, 0.5, 1.6)
    out = _naadloos(out, lengte, overlap)
    return _meester(out, piek=0.32, intro=0.0, outro=0.0, op_piek=True)


def sfeer_zacht():
    lengte, overlap = 18.0, 2.0
    rng = np.random.default_rng(170102)
    n = int(SR * (lengte + overlap))
    t = np.arange(n) / SR
    ruis = rng.normal(0, 1, n)
    bries = (_laag(ruis, 700) - _laag(ruis, 150)) * (0.5 + 0.5 * np.sin(2 * np.pi * t / 6.0) ** 2)
    out = bries * 0.5 + _lucht(rng, n, 200, 11.0) * 0.6
    for _ in range(int(lengte * 0.45)):
        f = PENTA_HOOG[rng.integers(6)]
        _plak(out, celesta(f, 1.6), rng.uniform(0, lengte + overlap - 1.6), rng.uniform(0.03, 0.06))
    out = galm(out, 0.4, 1.4)
    out = _naadloos(out, lengte, overlap)
    return _meester(out, piek=0.26, intro=0.0, outro=0.0, op_piek=True)


def sfeer_slaap():
    lengte, overlap = 18.0, 2.0       # (breathing every 4.5 s: four breaths exactly fit the loop)
    rng = np.random.default_rng(170103)
    n = int(SR * (lengte + overlap))
    t = np.arange(n) / SR
    fase = (t % 4.5) / 4.5
    adem = np.where(fase < 0.42, np.sin(np.pi * fase / 0.42) ** 2, 0.55 * np.sin(np.pi * np.clip((fase - 0.42) / 0.5, 0, 1)) ** 2)
    ruis = rng.normal(0, 1, n)
    out = (_laag(ruis, 900) - _laag(ruis, 180)) * adem * 0.7 + _lucht(rng, n, 160, 13) * 0.4
    for tijd in (3.1, 12.4):
        _plak(out, speeldoos(PENTA_HOOG[rng.integers(5)], 2.0), tijd, 0.03)
    out = galm(out, 0.3, 1.3)
    out = _naadloos(out, lengte, overlap)
    return _meester(out, piek=0.24, intro=0.0, outro=0.0, op_piek=True)


def sfeer_belletjes():
    lengte, overlap = 18.0, 3.0
    rng = np.random.default_rng(170104)
    n = int(SR * (lengte + overlap))
    out = _lucht(rng, n, 220, 10.0) * 0.5
    for _ in range(int(lengte * 0.7)):
        f = PENTA_HOOG[rng.integers(6)]
        _plak(out, glock(f, 2.0) if rng.random() < 0.6 else bel(f / 2, 3.0), rng.uniform(0, lengte + overlap - 3.0),
              rng.uniform(0.03, 0.07))
    out = echo(out, 0.42, 0.35, 4)
    out = galm(out, 0.5, 1.7)
    out = _naadloos(out, lengte, overlap)
    return _meester(out, piek=0.30, intro=0.0, outro=0.0, op_piek=True)


def plink(nr):
    rng = np.random.default_rng(170200 + nr)
    out = np.zeros(int(SR * 2.5))
    tijd = 0.02
    for _ in range(1 + nr % 2):
        _plak(out, druppel(PENTA_HOOG[rng.integers(5)] * (0.5 if nr == 3 else 1.0)), tijd, rng.uniform(0.7, 1.0))
        tijd += rng.uniform(0.25, 0.6)
    out = galm(out, 0.6, 1.6)
    out[-int(SR * 0.3):] *= np.linspace(1, 0, int(SR * 0.3))
    return out / max(1e-9, np.max(np.abs(out))) * 0.40


def tingel(nr):
    rng = np.random.default_rng(170300 + nr)
    out = np.zeros(int(SR * 3.5))
    noten = [(0, 2, 4, 5), (5, 4, 2), (1, 2, 4, 5, 3)][nr]
    tijd = 0.03
    for i, k in enumerate(noten):
        _plak(out, glock(PENTA_HOOG[k], 2.4), tijd, rng.uniform(0.6, 1.0) * 0.85 ** i)
        tijd += rng.uniform(0.11, 0.24)
    out = galm(out, 0.5, 1.5)
    out[-int(SR * 0.4):] *= np.linspace(1, 0, int(SR * 0.4))
    return out / max(1e-9, np.max(np.abs(out))) * 0.38


GELUIDEN = {f"muziek_{b}": m for b, m in MUZIEK.items()}
GELUIDEN.update({"sfeer_druppels": sfeer_druppels, "sfeer_zacht": sfeer_zacht, "sfeer_slaap": sfeer_slaap,
                 "sfeer_belletjes": sfeer_belletjes})
GELUIDEN.update({f"plink{i + 1}": (lambda i=i: plink(i)) for i in range(4)})
GELUIDEN.update({f"tingel{i + 1}": (lambda i=i: tingel(i)) for i in range(3)})


# ----------------------------------------------------------------------------------------------------------------------
# writing

def _ogg(path, sig):
    """Mono vorbis with PyAV (as tools/features/bio_wereld_dal_geluid.py does)."""
    import fractions

    import av
    c = av.open(path, "w", format="ogg")
    st = c.add_stream("libvorbis", rate=SR)
    st.layout = "mono"
    data = sig.astype(np.float32)[None, :]
    pts = 0
    for i in range(0, data.shape[1], 1024):
        fr = av.AudioFrame.from_ndarray(np.ascontiguousarray(data[:, i:i + 1024]), format="fltp", layout="mono")
        fr.sample_rate = SR
        fr.pts = pts
        fr.time_base = fractions.Fraction(1, SR)
        pts += fr.samples
        for pk in st.encode(fr):
            c.mux(pk)
    for pk in st.encode(None):
        c.mux(pk)
    c.close()


def schrijf(A, opnieuw=False):
    """Writes the missing ogg files (an existing one only with opnieuw)."""
    gemaakt = []
    for naam, maak in GELUIDEN.items():
        path = os.path.join(A, "sounds", MAP, f"{naam}.ogg")
        if opnieuw or not os.path.exists(path):
            os.makedirs(os.path.dirname(path), exist_ok=True)
            _ogg(path + ".tmp", maak())
            os.replace(path + ".tmp", path)
            gemaakt.append(naam)
    return gemaakt


def geluid_events():
    d = {}
    for b in MUZIEK:
        d[f"{MAP}.muziek.{b}"] = {"sounds": [{"name": f"guhs:{MAP}/muziek_{b}", "stream": True, "volume": 0.6}]}
    for s in ("druppels", "zacht", "slaap", "belletjes"):
        d[f"{MAP}.sfeer.{s}"] = {"sounds": [{"name": f"guhs:{MAP}/sfeer_{s}", "stream": True, "volume": 0.5}],
                                 "subtitle": f"subtitles.guhs.{MAP}.sfeer.{s}"}
    d[f"{MAP}.plink"] = {"sounds": [{"name": f"guhs:{MAP}/plink{i}", "volume": 0.55} for i in range(1, 5)],
                         "subtitle": f"subtitles.guhs.{MAP}.plink"}
    d[f"{MAP}.tingel"] = {"sounds": [{"name": f"guhs:{MAP}/tingel{i}", "volume": 0.5} for i in range(1, 4)],
                          "subtitle": f"subtitles.guhs.{MAP}.tingel"}
    d[f"{MAP}.giechel"] = {"sounds": [{"name": "guhs:entity.guh.ambient", "type": "event", "pitch": 1.6, "volume": 0.3},
                                      {"name": "guhs:entity.guh.ambient", "type": "event", "pitch": 1.8, "volume": 0.25}],
                           "subtitle": f"subtitles.guhs.{MAP}.giechel"}
    d[f"{MAP}.snurkje"] = {"sounds": [{"name": "minecraft:entity.fox.sleep", "type": "event", "pitch": 1.3, "volume": 0.3},
                                      {"name": "minecraft:entity.cat.purr", "type": "event", "pitch": 0.8, "volume": 0.25}],
                           "subtitle": f"subtitles.guhs.{MAP}.snurkje"}
    return d


def guhmension_biomen(D):
    pad = os.path.join(D, "dimension", "guhmension.json")
    with open(pad, encoding="utf-8") as f:
        dim = json.load(f)
    ids = [b["biome"] for b in dim["generator"]["biome_source"]["biomes"]]
    namen = sorted({i.split(":", 1)[1] for i in ids if i.startswith("guhs:")})
    return sorted(set(namen) | set(GROTTEN))


def _patch_attributes(d, naam):
    """The 26.1 shape. Returns True when something changed."""
    voor = json.dumps(d, sort_keys=True)
    attrs = d.setdefault("attributes", {})
    amb = attrs.get("minecraft:audio/ambient_sounds")
    grot = GROTTEN.get(naam)
    if grot:
        loop, add, kans = grot
        amb = dict(amb or {})
        amb["loop"] = f"guhs:{MAP}.sfeer.{loop}"
        amb["mood"] = dict(MOOD)
        amb["additions"] = {"sound": f"guhs:{MAP}.{add}", "tick_chance": kans}
        attrs["minecraft:audio/ambient_sounds"] = amb
        bm = attrs.get("minecraft:audio/background_music") or {}
        bm = dict(bm)
        bm["default"] = {"sound": f"guhs:{MAP}.muziek.{naam}", "min_delay": MUZIEK_DELAY[0], "max_delay": MUZIEK_DELAY[1]}
        bm.setdefault("creative", {"sound": "minecraft:music.creative", "min_delay": 12000, "max_delay": 24000})
        attrs["minecraft:audio/background_music"] = bm
        # (no "silence" in a cave: a biome may have muted its music; ours must play)
        if attrs.get("minecraft:audio/music_volume") == 0.0:
            del attrs["minecraft:audio/music_volume"]
    elif amb:
        if amb.get("loop") == ENG:
            del amb["loop"]
        m = amb.get("mood")
        if isinstance(m, dict) and m.get("sound") == ENG:
            amb["mood"] = {**m, "sound": MOOD["sound"]}
        ad = amb.get("additions")
        if isinstance(ad, dict) and ad.get("sound") == ENG:
            del amb["additions"]
        elif isinstance(ad, list):
            amb["additions"] = [a for a in ad if a.get("sound") != ENG]
    return json.dumps(d, sort_keys=True) != voor


def _patch_effects(d, naam):
    """The old 1.21.1 shape (effects.ambient_sound / mood_sound / additions_sound / music), before mc26.py converts it."""
    voor = json.dumps(d, sort_keys=True)
    eff = d.get("effects", {})
    grot = GROTTEN.get(naam)
    if grot:
        loop, add, kans = grot
        eff["ambient_sound"] = f"guhs:{MAP}.sfeer.{loop}"
        eff["mood_sound"] = dict(MOOD)
        eff["additions_sound"] = {"sound": f"guhs:{MAP}.{add}", "tick_chance": kans}
        eff["music"] = {"sound": f"guhs:{MAP}.muziek.{naam}", "min_delay": MUZIEK_DELAY[0], "max_delay": MUZIEK_DELAY[1],
                        "replace_current_music": False}
    else:
        if eff.get("ambient_sound") == ENG:
            del eff["ambient_sound"]
        m = eff.get("mood_sound")
        if isinstance(m, dict) and m.get("sound") == ENG:
            eff["mood_sound"] = {**m, "sound": MOOD["sound"]}
        a = eff.get("additions_sound")
        if isinstance(a, dict) and a.get("sound") == ENG:
            del eff["additions_sound"]
    return json.dumps(d, sort_keys=True) != voor


def patch_biomen(D, patch_json):
    """Patches the audio of every Guhmension biome that exists. Returns (changed, skipped)."""
    veranderd, overgeslagen = [], []
    for naam in guhmension_biomen(D):
        pad = os.path.join(D, "worldgen", "biome", f"{naam}.json")
        if not os.path.exists(pad):
            overgeslagen.append(naam)
            continue
        with open(pad, encoding="utf-8") as f:
            d = json.load(f)
        oud = "attributes" not in d and "sky_color" in d.get("effects", {})
        nu = _patch_effects if oud else _patch_attributes
        if nu(json.loads(json.dumps(d)), naam):
            patch_json(pad, lambda d, naam=naam, nu=nu: nu(d, naam))
            veranderd.append(naam)
    return veranderd, overgeslagen


def check(A, D):
    problemen = []
    for naam in guhmension_biomen(D):
        pad = os.path.join(D, "worldgen", "biome", f"{naam}.json")
        if os.path.exists(pad):
            with open(pad, encoding="utf-8") as f:
                tekst = f.read()
            json.loads(tekst)
            if ENG in tekst:
                problemen.append(f"{naam}: still {ENG}")
            if naam in GROTTEN and f"guhs:{MAP}.muziek.{naam}" not in tekst:
                problemen.append(f"{naam}: no music of its own")
    for naam in GELUIDEN:
        if not os.path.exists(os.path.join(A, "sounds", MAP, f"{naam}.ogg")):
            problemen.append(f"missing sound {MAP}/{naam}.ogg")
    with open(os.path.join(A, "sounds.json"), encoding="utf-8") as f:
        sj = json.load(f)
    for k in geluid_events():
        if k not in sj:
            problemen.append(f"sounds.json misses {k}")
    if problemen:
        raise SystemExit("onderwol_geluid self-check failed:\n  " + "\n  ".join(problemen))


def build(h, opnieuw=False):
    """From a generator (h: A, D, patch_json, lang). Call it after all biome files are written."""
    schrijf(h.A, opnieuw)
    h.patch_json(f"{h.A}/sounds.json", lambda d: d.update(geluid_events()))
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    patch_biomen(h.D, h.patch_json)
    check(h.A, h.D)


def _direct():
    R = os.path.join("src", "main", "resources")
    A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")

    def w(path, obj):
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def patch_json(path, fn):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        voor = json.dumps(data, sort_keys=True)
        fn(data)
        if json.dumps(data, sort_keys=True) != voor:
            w(path, data)

    gemaakt = schrijf(A, "--opnieuw" in sys.argv)
    patch_json(f"{A}/sounds.json", lambda d: d.update(geluid_events()))
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        patch_json(os.path.join(A, "lang", taal + ".json"), lambda d, i=i: d.update({k: v[i] for k, v in TEKSTEN.items()}))
    # the English overlay (tools/lang/apply_en.py and the full generation rebuild en_us from nl_nl + this)
    overlay = os.path.join("tools", "lang", "en", "c191_onderwol_geluid.json")
    w(overlay, {k: v[1] for k, v in TEKSTEN.items()})
    veranderd, overgeslagen = patch_biomen(D, patch_json)
    check(A, D)
    print(f"onderwol_geluid: {len(gemaakt)} ogg written, biomes patched: {', '.join(veranderd) or '-'}"
          + (f"; not there yet: {', '.join(overgeslagen)}" if overgeslagen else ""))


if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    _direct()
