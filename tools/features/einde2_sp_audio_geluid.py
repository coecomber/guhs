"""
einde2 "Sponsguh", slice sp_audio - the sound files (mono OGG Vorbis, 44.1 kHz) in assets/guhs/sounds/einde2_sp_audio/
(CONTRACT_E2_C 6, 7.9). Run by hand, NOT part of make_resources.py (it needs scipy and soundfile; the OGGs are committed as
binaries in a commit that is not a gen: commit; einde2_sp_audio.py only lists them in sounds.json):

    python tools/features/einde2_sp_audio_geluid.py [naam ...]       the pieces the mod makes itself (fixed seeds)
    python tools/features/einde2_sp_audio_geluid.py --muziek <bron> [naam ...]
                                                                      the real pieces of einde2_sp_audio_licenties.json,
                                                                      from the files downloaded once into <bron>
                                                                      (guhs_workeinde2/scratch/sp_audio/bron/)

Made here (own compositions: NEVER a melody, lyric, recording or "sounds like" copy of the series, its film or its songs):
  moment_zoete_zege     "Zoete Knabbelzege": a power ballad on piano and organ that turns into a stadium anthem with drums,
                        guitars, a choir and the clapping town; one key change up for the last lines (100 BPM)
  moment_guhfy_guhber   "Ik ben een Guhfy Guhber": a rock song (150 BPM, E major, a guitar riff, a shouted bridge)
  moment_monsters       the trench monsters' sweet 3/4 waltz in low voices with a music box (120 BPM; the scene roars first)
                        (the sung lines of all three: einde2_sp_audio_liederen.py; the voice is a wordless synth voice,
                        the words are captions)
  deuntje_*             the minigame tunes: bakken, kwallen, examen, sluipen, trommel (112 BPM, first beat = sample 0),
                        kroeg (96 BPM, first beat = sample 0), ijssalon, emmers; every tune loops seamlessly
  stinger_*             verteller (a French musette flourish for the narrator's card), klarinet (Octo, off key), karen
                        (her beeps), bevries, snor (a jaw-harp boing and a ta-daa), opgelost, mislukt (a soft spring boing,
                        never sad), lach (a giggly guh laugh, "njehehe")
"""
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import einde2_audio_geluid as g  # noqa: E402  (the helpers of the Guhvatar audio: env, band, low, high, galm, pluk_snel, ...)
import einde2_sp_audio_liederen as liederen  # noqa: E402

SR = g.SR
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "einde2_sp_audio")
LEDGER = os.path.join("tools", "features", "einde2_sp_audio_licenties.json")
DOEL_RMS_DB = -20.0     # the loudness of every long piece (the real ones and the own ones alike)
PLAFOND = 0.89
NIVEAU_MUZIEK = 0.85    # libsndfile Vorbis level for the real pieces (mono, about 55 kbit/s)
NIVEAU_EIGEN = 0.5


def save(name, x, niveau=NIVEAU_EIGEN):
    sf = g._sf()
    os.makedirs(OUT, exist_ok=True)
    x = np.clip(np.asarray(x, dtype=np.float32), -1.0, 1.0)
    with sf.SoundFile(os.path.join(OUT, name + ".ogg"), "w", SR, 1, format="OGG", subtype="VORBIS", compression_level=niveau) as f:
        for i in range(0, len(x), 32768):
            f.write(x[i:i + 32768])
    print(f"wrote {name:28s} {len(x) / SR:7.2f}s  {os.path.getsize(os.path.join(OUT, name + '.ogg')) / 1024:8.0f} KB")


def luid(x, doel=DOEL_RMS_DB):
    """Loudness as the real pieces: the RMS of the loudest half to the target, then a soft ceiling."""
    x = np.asarray(x, dtype=np.float64)
    blok = int(0.4 * SR)
    rms = np.sqrt(np.array([np.mean(x[i:i + blok] ** 2) for i in range(0, max(1, len(x) - blok), blok)]) + 1e-12)
    hard = np.sqrt(np.mean(np.sort(rms)[len(rms) // 2:] ** 2))
    x = x * (10 ** (doel / 20) / hard)
    k = PLAFOND * 0.8
    boven = np.abs(x) > k
    x[boven] = np.sign(x[boven]) * (k + (PLAFOND * 0.2) * np.tanh((np.abs(x[boven]) - k) / (PLAFOND * 0.2)))
    return x.astype(np.float32)


def rond(x, lengte):
    """A seamless loop of `lengte` seconds: the tail after the end is folded back onto the start."""
    n = int(round(lengte * SR))
    uit = np.array(x[:n], dtype=np.float64)
    if len(uit) < n:
        uit = np.concatenate([uit, np.zeros(n - len(uit))])
    staart = x[n:]
    while len(staart):
        k = min(n, len(staart))
        uit[:k] += staart[:k]
        staart = staart[k:]
    return uit


# =====================================================================================================================
# notes and chords
# =====================================================================================================================
_NAAM = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8, "Ab": 8,
         "A": 9, "A#": 10, "Bb": 10, "B": 11}


def midi(noot):
    naam, octaaf = noot[:-1], int(noot[-1])
    return 12 * (octaaf + 1) + _NAAM[naam]


def mhz(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def hz(noot, transpose=0):
    return mhz(midi(noot) + transpose)


def akkoord(naam, octaaf=3, transpose=0):
    """'Am' -> the midi notes of the chord (root in `octaaf`): major, m (minor), 7, m7, sus4, 5 (power)."""
    for q, iv in (("m7", [0, 3, 7, 10]), ("sus4", [0, 5, 7]), ("7", [0, 4, 7, 10]), ("m", [0, 3, 7]), ("5", [0, 7, 12]), ("", [0, 4, 7])):
        if naam.endswith(q) and naam[:len(naam) - len(q)] in _NAAM:
            root = 12 * (octaaf + 1) + _NAAM[naam[:len(naam) - len(q)]] + transpose
            return [root + i for i in iv]
    raise ValueError(naam)


# =====================================================================================================================
# instruments (all synthesised; numpy, one note at a time)
# =====================================================================================================================
def t_(dur):
    return np.arange(max(1, int(round(dur * SR)))) / SR


def additief(fr, n_max, helling=1.0, oneven=False):
    """A band-limited saw (or square when oneven) on a frequency curve `fr` (Hz per sample)."""
    fase = 2 * np.pi * np.cumsum(fr) / SR
    f0 = float(np.max(fr))
    x = np.zeros_like(fase)
    for k in range(1, n_max + 1):
        if oneven and k % 2 == 0:
            continue
        if k * f0 > 9000:
            break
        x += np.sin(k * fase) / k ** helling
    return x


def uke(f, dur, seed=1):
    """A ukulele string: bright, short, nylon."""
    return g.pluk_snel(f, dur + 0.25, seed, helder=0.7, verval=3.2) * 0.8


def strum(noten, t0, dur, seed, omhoog=False, spreid=0.014, inst=uke, sterk=1.0):
    """A strum: the notes of a chord one after another (down: low to high)."""
    rij = sorted(noten, reverse=omhoog)
    return [(t0 + i * spreid, inst(mhz(m), dur, seed + i) * sterk * (0.85 if omhoog else 1.0)) for i, m in enumerate(rij)]


def marimba(f, dur, seed=1):
    t = t_(dur + 0.6)
    x = np.sin(2 * np.pi * f * t) * np.exp(-t * 4.5) + 0.3 * np.sin(2 * np.pi * f * 4 * t) * np.exp(-t * 18) \
        + 0.12 * np.sin(2 * np.pi * f * 9.9 * t) * np.exp(-t * 40)
    return x * g.env(len(t), 0.002, 0.05)


def glock(f, dur, seed=1):
    t = t_(dur + 1.2)
    x = np.sin(2 * np.pi * f * t) * np.exp(-t * 2.5) + 0.25 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 6) \
        + 0.1 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t * 12)
    return x * g.env(len(t), 0.001, 0.05)


def muziekdoos(f, dur, seed=1):
    t = t_(dur + 1.5)
    x = np.sin(2 * np.pi * f * t) * np.exp(-t * 2.0) + 0.4 * np.sin(2 * np.pi * f * 3.01 * t) * np.exp(-t * 5) \
        + 0.15 * np.sin(2 * np.pi * f * 5.98 * t) * np.exp(-t * 9)
    return x * g.env(len(t), 0.001, 0.08)


def steel(f, dur, seed=1, glij_van=None):
    """A lap steel guitar: a warm tone that slides into the note and swells with a slow vibrato."""
    t = t_(dur + 0.2)
    fr = np.full(len(t), f, dtype=np.float64)
    if glij_van:
        gl = np.clip(t / 0.18, 0, 1)
        fr = f * (glij_van / f) ** (1 - gl)
    fr = fr * (1 + 0.006 * np.clip((t - 0.3) / 0.4, 0, 1) * np.sin(2 * np.pi * 4.8 * t))
    x = additief(fr, 8, helling=1.6)
    x = g.low(x, 2500)
    return x * g.env(len(t), 0.04, 0.25, 1.3) * np.exp(-t * 0.6)


def tuba(f, dur, seed=1):
    t = t_(dur)
    fr = f * (1 + 0.003 * np.sin(2 * np.pi * 5 * t))
    x = additief(fr, 12, helling=1.1)
    return g.low(x, 700) * g.env(len(t), 0.03, 0.06, 1)


def klarinet(f, dur, seed=1, adem=0.06, vib=0.004):
    t = t_(dur)
    r = np.random.default_rng(seed)
    fr = f * (1 + vib * np.clip((t - 0.25) / 0.4, 0, 1) * np.sin(2 * np.pi * 5.2 * t))
    x = additief(fr, 15, helling=1.2, oneven=True) + 0.15 * additief(fr, 6, helling=2.0)
    x = g.low(x, 3200) + g.band(r.normal(0, 1, len(t)), f, f * 4) * adem
    return x * g.env(len(t), 0.025, 0.06, 1)


def fagot(f, dur, seed=1):
    t = t_(dur)
    x = additief(np.full(len(t), f), 14, helling=1.0)
    x = g.band(x, f * 0.9, 1800)
    return x * g.env(len(t), 0.02, 0.05, 1)


def accordeon(f, dur, seed=1):
    """Two reeds a few cents apart (the musette shimmer) and a bellows swell."""
    t = t_(dur)
    x = additief(np.full(len(t), f), 12, helling=1.3, oneven=True) * 0.6 + additief(np.full(len(t), f * 1.0045), 12, helling=1.3, oneven=True) * 0.5 \
        + additief(np.full(len(t), f * 0.9958), 12, helling=1.3, oneven=True) * 0.4
    x = g.low(x, 3800)
    return x * g.env(len(t), 0.03, 0.08, 1) * (0.9 + 0.1 * np.sin(2 * np.pi * 0.8 * t))


def orgel(f, dur, seed=1, helder=0.5):
    t = t_(dur)
    x = np.zeros_like(t)
    for mul, a in ((0.5, 0.5), (1, 1.0), (2, 0.6 * helder + 0.2), (3, 0.3 * helder), (4, 0.25 * helder), (8, 0.1 * helder)):
        x += a * np.sin(2 * np.pi * f * mul * t)
    leslie = 1 + 0.12 * np.sin(2 * np.pi * 5.8 * t)
    return x * leslie * g.env(len(t), 0.015, 0.06, 1)


def piano(f, dur, seed=1, sterk=1.0):
    t = t_(dur + 0.8)
    r = np.random.default_rng(seed)
    x = np.zeros_like(t)
    for k in range(1, 12):
        if k * f > 9000:
            break
        a = (1.0 / k ** 1.1) * (0.6 + 0.4 * sterk)
        x += a * np.sin(2 * np.pi * f * k * np.sqrt(1 + 0.0004 * k * k) * t) * np.exp(-t * (0.9 + 0.5 * k) * (1 + f / 900))
    hamer = g.band(r.normal(0, 1, len(t)), 1000, 5000) * np.exp(-t * 200) * 0.05
    los = np.ones_like(t)
    n = int(dur * SR)
    if n < len(t):
        los[n:] = np.exp(-(t[n:] - dur) * 12)
    return (x + hamer) * los * g.env(len(t), 0.002, 0.02, 1)


def gitaar(noten, dur, seed=1, gain=7.0, demp=False):
    """A distorted electric guitar playing these midi notes together (a power chord), palm muted when demp."""
    t = t_(dur)
    x = np.zeros_like(t)
    for i, m in enumerate(noten):
        f = mhz(m)
        for d in (-0.003, 0.003):
            x += additief(np.full(len(t), f * (1 + d)), 30, helling=1.0) * 0.5
    x = np.tanh(x * gain / max(1, len(noten)))
    x = g.low(g.high(x, 90), 1600 if demp else 3400)
    e = g.env(len(t), 0.004, 0.05 if demp else 0.12, 1)
    if demp:
        e *= np.exp(-t * 9)
    return x * e


def bas(f, dur, seed=1):
    t = t_(dur)
    x = additief(np.full(len(t), f), 10, helling=1.2) * 0.6 + np.sin(2 * np.pi * f * t) * 0.8
    x = g.low(x, 900)
    return x * g.env(len(t), 0.004, 0.05, 1) * (0.7 + 0.3 * np.exp(-t * 3))


def pizz(f, dur, seed=1):
    return g.pluk_snel(f, min(dur, 0.5) + 0.3, seed, helder=0.35, verval=5.0)


def kick(seed=1, sterk=1.0):
    t = t_(0.45)
    fr = 48 + 110 * np.exp(-t * 28)
    x = np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.exp(-t * 7)
    x += g.high(np.random.default_rng(seed).normal(0, 1, len(t)), 2500) * np.exp(-t * 300) * 0.25
    return x * sterk


def snare(seed=1, sterk=1.0):
    t = t_(0.3)
    r = np.random.default_rng(seed)
    x = g.band(r.normal(0, 1, len(t)), 1200, 8000) * np.exp(-t * 16) * 0.8 + np.sin(2 * np.pi * 190 * t) * np.exp(-t * 25) * 0.6
    return x * sterk


def clap(seed=1, sterk=1.0):
    t = t_(0.25)
    r = np.random.default_rng(seed)
    x = np.zeros_like(t)
    for d in (0.0, 0.011, 0.022):
        i = int(d * SR)
        x[i:] += g.band(r.normal(0, 1, len(t) - i), 900, 5000) * np.exp(-t[:len(t) - i] * 30)
    return x * 0.5 * sterk


def crash(seed=1, sterk=1.0, dur=2.2):
    t = t_(dur)
    x = g.high(np.random.default_rng(seed).normal(0, 1, len(t)), 4500) * np.exp(-t * 2.2)
    return x * sterk * 0.5


def hat(seed=1, sterk=1.0, open_=False):
    t = t_(0.35 if open_ else 0.06)
    return g.high(np.random.default_rng(seed).normal(0, 1, len(t)), 7500) * np.exp(-t * (12 if open_ else 70)) * sterk * 0.4


def shaker(seed=1, sterk=1.0):
    t = t_(0.09)
    return g.band(np.random.default_rng(seed).normal(0, 1, len(t)), 4000, 11000) * g.env(len(t), 0.03, 0.05) * sterk * 0.3


def tom(f, seed=1, sterk=1.0):
    return g.trom(f, 0.5, seed, ruis=0.2, val=0.4) * sterk


def stokjes(seed=1):
    return g.tik(0.06, seed, 3300) * 0.6


# the voice: a band-limited source through three formants (a wordless singer; the words are captions)
KLINKERS = {
    "a": ((800, 80, 1.0), (1150, 90, 0.5), (2900, 120, 0.25)),
    "o": ((450, 70, 1.0), (800, 80, 0.55), (2830, 100, 0.1)),
    "e": ((400, 60, 1.0), (1700, 90, 0.4), (2600, 120, 0.28)),
    "oe": ((325, 50, 1.0), (700, 60, 0.3), (2530, 170, 0.05)),
    "i": ((290, 60, 1.0), (2150, 90, 0.32), (2950, 110, 0.25)),
}


def formant(x, klinker, schaal=1.0):
    s = g._sig()
    uit = np.zeros_like(x)
    for f, bw, a in KLINKERS[klinker]:
        f = f * schaal
        b, a_ = s.iirpeak(f / (SR / 2), max(1.0, f / bw))
        uit += a * s.lfilter(b, a_, x)
    return uit


def stem(f, dur, seed=1, klinker="a", glij_van=None, vib=0.012, adem=0.08, schaal=1.0, helder=1.0):
    """One sung note: a glide in from the note before, vibrato that comes late, a vowel."""
    t = t_(dur)
    r = np.random.default_rng(seed)
    fr = np.full(len(t), f, dtype=np.float64)
    if glij_van:
        gl = np.clip(t / 0.06, 0, 1)
        fr = f * (glij_van / f) ** (1 - gl)
    vibr = np.clip((t - 0.22) / 0.3, 0, 1)
    fr = fr * (1 + vib * vibr * np.sin(2 * np.pi * 5.4 * t + r.uniform(0, 6))) * (1 + 0.002 * r.normal(0, 1))
    bron = additief(fr, 60, helling=1.0 + 0.4 * (1 - helder))
    bron += r.normal(0, 1, len(t)) * adem
    x = formant(bron, klinker, schaal)
    return x * g.env(len(t), 0.03, min(0.12, dur * 0.4), 1.2)


def koor(midis, dur, seed=1, klinker="a", stemmen=3, schaal=1.0, transpose=0):
    """A small choir: per note a few detuned voices that start a hair apart."""
    r = np.random.default_rng(seed)
    uit = np.zeros(int(round((dur + 0.05) * SR)))
    for m in midis:
        for v in range(stemmen):
            f = mhz(m + transpose) * (1 + r.uniform(-0.006, 0.006))
            d = int(r.uniform(0, 0.03) * SR)
            x = stem(f, dur, seed + v * 7 + m, klinker, vib=0.01, adem=0.1, schaal=schaal)
            uit[d:d + len(x)] += x[:len(uit) - d]
    return uit / max(1, stemmen)


# =====================================================================================================================
# a little sequencer
# =====================================================================================================================
class Stuk:
    """Collects (seconds, signal, volume) parts on a beat grid and mixes them."""

    def __init__(self, bpm, maat=4):
        self.tel = 60.0 / bpm
        self.maat = maat
        self.delen = []

    def zet(self, tel, sig, vol=1.0):
        self.delen.append((tel * self.tel, sig, vol))

    def mix(self, lengte_tellen, staart=3.0):
        n = int(round((lengte_tellen * self.tel + staart) * SR))
        uit = np.zeros(n)
        for off, sig, vol in self.delen:
            i = int(round(off * SR))
            if i >= n:
                continue
            j = min(n, i + len(sig))
            uit[i:j] += sig[:j - i] * vol
        return uit

    def seconden(self, tellen):
        return tellen * self.tel


def melodie_op(stuk, noten, start, inst, vol=1.0, seed=1, legato=1.0, glij=False, klinkers=None, transpose=0):
    """Puts a (note or None, beats) line on the grid from beat `start`; returns the beat after it."""
    t = start
    vorige = None
    for i, (n, tellen) in enumerate(noten):
        if n:
            f = hz(n, transpose)
            dur = tellen * stuk.tel * legato + 0.04
            kw = {}
            if glij:
                kw["glij_van"] = vorige
            if klinkers:
                kw["klinker"] = klinkers[i % len(klinkers)]
            stuk.zet(t, inst(f, dur, seed + i, **kw), vol)
            vorige = f
        t += tellen
    return t


# =====================================================================================================================
# the songs
# =====================================================================================================================
ZOETE_MELODIE = {
    "1": [("E4", 1), ("G4", 1), ("G4", 1), ("A4", .5), ("G4", .5), ("E4", 1.5), ("D4", .5), ("C4", 1), (None, 1)],
    "2": [("F4", 1), ("A4", 1), ("A4", 1), ("C5", .5), ("A4", .5), ("G4", 2), (None, 2)],
    "3": [("A4", 1), ("A4", .5), ("B4", .5), ("C5", 1), ("E5", 1), ("D5", 1.5), ("C5", .5), ("A4", 1.5), (None, .5)],
    "4": [("F4", 1), ("A4", 1), ("D5", 1), ("C5", .5), ("B4", .5), ("G4", 3), (None, 1)],
    "5": [("C5", 1), ("C5", 1), ("D5", 1), ("E5", 1), ("F5", 1), ("E5", 1), ("D5", 1), ("G4", 1)],
    "6": [("G5", 1.5), ("E5", .5), ("E5", 1), ("D5", 1), ("C5", 1), ("D5", 1), ("E5", 1), ("D5", 1)],
    "7": [("C5", 1), ("E5", 1), ("A5", 1.5), ("G5", .5), ("F5", 1), ("E5", 1), ("C5", 1), (None, 1)],
    "8": [("G5", 1.5), ("E5", .5), ("E5", 1), ("D5", 1), ("C5", 1), ("D5", 1), ("E5", 1), ("G5", 1)],
    "9": [("A5", 1.5), ("G5", .5), ("F5", 1), ("E5", 1), ("D5", 1), ("E5", .5), ("D5", .5), ("C5", 2)],
    # the last lines one whole tone up (D major)
    "10": [("A5", 1.5), ("F#5", .5), ("F#5", 1), ("E5", 1), ("D5", 1), ("E5", 1), ("F#5", 1), ("E5", 1)],
    "11": [("B5", 1.5), ("A5", .5), ("G5", 1), ("F#5", 1), ("E5", 1), ("F#5", .5), ("E5", .5), ("D5", 10)],
}
# chords per bar (26 bars): intro 2, verse 8, build 2, chorus 8, the key change 6
ZOETE_AKKOORDEN = ["C", "G", "C", "Em", "F", "C", "Am", "F", "Dm", "G", "F", "G",
                   "C", "G", "Am", "F", "C", "G", "F", "G", "D", "A", "G", "A", "D", "D"]


def zoete_zege():
    _, _, bpm, maat, maten, regels = liederen.LIEDEREN["zoete_zege"]
    s = Stuk(bpm, maat)
    start = {k: b for k, b, _, _ in regels}
    seed = 3000
    for bar, ak in enumerate(ZOETE_AKKOORDEN):
        b0 = bar * 4
        noten = akkoord(ak, 3)
        hoog = akkoord(ak, 4)
        if bar < 12:
            # the ballad: piano in eighths (a rolling figure), an organ pad, a soft bass from bar 2 on
            figuur = [noten[0], noten[1], noten[2], hoog[0], noten[2], noten[1], hoog[0], noten[2]]
            for i, m in enumerate(figuur):
                s.zet(b0 + i * 0.5, piano(mhz(m), 0.6, seed + bar * 8 + i, 0.6), 0.32 if bar < 10 else 0.4)
            s.zet(b0, koor(hoog, 4 * s.tel, seed + bar, "oe", 1) if bar >= 2 else orgel(mhz(hoog[0]), 4 * s.tel, seed, 0.2), 0.12)
            s.zet(b0, orgel(mhz(noten[0]), 4 * s.tel, seed + bar, 0.3) + orgel(mhz(noten[1]), 4 * s.tel, seed + bar, 0.3)
                  + orgel(mhz(noten[2]), 4 * s.tel, seed + bar, 0.3), 0.06)
            if bar >= 2:
                s.zet(b0, bas(mhz(noten[0] - 12), 4 * s.tel * 0.95, seed + bar), 0.35)
            if bar >= 10:
                # the build: a snare roll and toms that grow, the bass in eighths
                for i in range(16):
                    tel = b0 + i * 0.25
                    s.zet(tel, snare(seed + bar * 20 + i, 0.25 + 0.6 * ((bar - 10) * 16 + i) / 32), 0.5)
                for i in range(8):
                    s.zet(b0 + i * 0.5, bas(mhz(noten[0] - 12), 0.28, seed + i), 0.4)
                if bar == 11:
                    for i, f in enumerate((180, 150, 120, 95)):
                        s.zet(b0 + 2 + i * 0.5, tom(f, seed + i), 0.7)
        else:
            # the anthem: drums, power chords, the bass in eighths, the choir, the town clapping
            kracht = 1.0 if bar < 20 else 1.15
            s.zet(b0, kick(seed + bar, 1.0), 0.9 * kracht)
            s.zet(b0 + 1.5, kick(seed + bar + 1, 0.7), 0.7 * kracht)
            s.zet(b0 + 2, snare(seed + bar, 1.0), 0.75 * kracht)
            if bar < 24:
                s.zet(b0 + 2.5, kick(seed + bar + 2, 0.6), 0.6)
            for i in range(8):
                s.zet(b0 + i * 0.5, hat(seed + bar * 8 + i, 0.8 if i % 2 == 0 else 0.5), 0.35)
                s.zet(b0 + i * 0.5, bas(mhz(noten[0] - 12), 0.3, seed + i), 0.42)
            if bar in (12, 20, 24):
                s.zet(b0, crash(seed + bar, 1.0, 3.0), 0.6)
            kracht_g = [noten[0], noten[0] + 7, noten[0] + 12]
            s.zet(b0, gitaar(kracht_g, 4 * s.tel, seed + bar, gain=6), 0.18 * kracht)
            s.zet(b0, koor(hoog, 4 * s.tel, seed + bar * 3, "a", 3), 0.18 * kracht)
            s.zet(b0, orgel(mhz(hoog[0]), 4 * s.tel, seed, 0.7) + orgel(mhz(hoog[1]), 4 * s.tel, seed, 0.7), 0.06)
            if bar >= 16:
                for tel in (1, 3):
                    s.zet(b0 + tel, clap(seed + bar * 4 + tel, 1.0), 0.45)
    # the voice
    for k, regel in ZOETE_MELODIE.items():
        b = start[k]
        luidheid = 0.6 if int(k) < 6 else 0.85
        melodie_op(s, regel, b, lambda f, d, sd, **kw: stem(f, d, sd, vib=0.014, **kw), luidheid, seed=3500 + int(k) * 20,
                   glij=True, klinkers=["a", "e", "o", "a", "i", "o"])
        if int(k) >= 6:
            # the town sings the chorus with you (an octave lower, a small choir)
            melodie_op(s, regel, b, lambda f, d, sd, **kw: koor([round(69 + 12 * np.log2(f / 440.0))], d, sd, "a", 3, transpose=-12),
                       0.28, seed=3700 + int(k) * 20)
    # the end: the last chord rings, a roll, a big hit
    eind = maten * maat
    for i in range(12):
        s.zet(eind - 4 + i * 0.25, snare(seed + 900 + i, 0.3 + i * 0.05), 0.5)
    s.zet(eind - 1, crash(seed + 999, 1.2, 3.5), 0.7)
    s.zet(eind - 1, kick(seed + 998, 1.2), 1.0)
    x = s.mix(eind, staart=0.0)
    x = g.galm(x, 1.8, 0.22, 31)[:len(x)]
    x *= g.env(len(x), 0.05, 1.5, 1.0)
    return luid(x)


GUHFY_MELODIE = {
    "1": [("B4", .5), ("B4", .5), ("B4", .5), ("G#4", .5), ("B4", 1), ("C#5", 1), ("B4", .5), ("G#4", .5), ("E4", 1), ("F#4", 1), (None, 1)],
    "2": [("B4", .5), ("B4", .5), ("C#5", .5), ("B4", .5), ("G#4", 1), ("E4", 1), ("F#4", .5), ("G#4", .5), ("F#4", 2), (None, 1)],
    "3": [("C#5", .5), ("C#5", .5), ("E5", 1), ("C#5", 1), ("B4", 1), ("G#4", .5), ("A4", .5), ("B4", 2), (None, 1)],
    "4": [("A4", .5), ("A4", .5), ("A4", .5), ("B4", .5), ("C#5", 1), ("B4", 1), ("A4", .5), ("F#4", .5), ("B4", 2), (None, 1)],
    "5": [("E5", 1), ("E5", .5), ("E5", .5), ("F#5", 1), ("G#5", 1.5), ("E5", .5), ("C#5", 2), (None, 1)],
    "6": [("D#5", .5), ("D#5", .5), ("D#5", .5), ("E5", .5), ("F#5", 1), ("E5", .5), ("D#5", .5), ("B4", 1), ("E5", 2), (None, 1)],
    "7": [("E5", 1), ("E5", .5), ("E5", .5), ("F#5", 1), ("G#5", 1.5), ("B5", .5), ("A5", 2), (None, 1)],
    "8": [("G#5", .5), ("F#5", .5), ("E5", .5), ("F#5", .5), ("G#5", 1), ("F#5", 1), ("D#5", 1), ("E5", 2), (None, 1)],
    "9": [("G5", 1), ("G5", 1), ("E5", 1), (None, 1), ("A5", 1), ("A5", 1), ("F#5", 1), (None, 1)],
    "10": [("E5", .5), ("E5", .5), ("E5", .5), ("E5", .5), ("G5", 1), ("A5", 1), ("B5", 4)],
}
for _a, _b in (("11", "5"), ("12", "6"), ("13", "7"), ("14", "8")):
    GUHFY_MELODIE[_a] = list(GUHFY_MELODIE[_b])
GUHFY_MELODIE["14"] = GUHFY_MELODIE["14"][:-2] + [("E5", 10)]
# chords per bar (34): intro riff 4, verse 8, chorus 8, bridge 4, chorus 8, end 2
GUHFY_AKKOORDEN = (["E", "E", "E", "E"] + ["E", "A", "E", "B", "A", "E", "F#m", "B"]
                   + ["E", "A", "B", "E", "E", "A", "B", "E"] + ["C", "D", "C", "D"]
                   + ["E", "A", "B", "E", "E", "A", "B", "E"] + ["E", "E"])
# the riff of the intro (root notes in eighths, midi; 2 bars, played twice)
GUHFY_RIFF = ["E2", "E2", "G2", "E2", "A2", "E2", "B2", "A2", "E2", "E2", "G2", "E2", "D3", "C3", "B2", "G2"]


def guhfy_guhber():
    _, _, bpm, maat, maten, regels = liederen.LIEDEREN["guhfy_guhber"]
    s = Stuk(bpm, maat)
    start = {k: b for k, b, _, _ in regels}
    seed = 5000
    for bar, ak in enumerate(GUHFY_AKKOORDEN):
        b0 = bar * 4
        noten = akkoord(ak, 2)
        root = noten[0] if noten[0] >= midi("E2") else noten[0] + 12
        power = [root, root + 7, root + 12]
        intro = bar < 4
        brug = 20 <= bar < 24
        eind = bar >= 32
        # drums: a driving rock beat (kick 1 and 3 + the "and" of 3, snare 2 and 4, eighth hats); fills at section ends
        if not eind:
            for tel in (0, 2, 2.5):
                s.zet(b0 + tel, kick(seed + bar * 3 + int(tel * 2), 1.0), 0.85)
            for tel in (1, 3):
                s.zet(b0 + tel, snare(seed + bar * 2 + tel, 1.0), 0.7)
            for i in range(8):
                s.zet(b0 + i * 0.5, hat(seed + bar * 8 + i, 0.8 if i % 2 == 0 else 0.45, open_=(brug and i % 2 == 1)), 0.32)
            if bar in (3, 11, 19, 23, 31):
                for i, f in enumerate((210, 180, 150, 120, 100, 90, 80, 75)):
                    s.zet(b0 + 2 + i * 0.25, tom(f, seed + bar + i, 0.9), 0.55)
        if bar in (0, 4, 12, 20, 24, 32):
            s.zet(b0, crash(seed + bar, 1.0, 2.5), 0.55)
        # the guitar: the riff in the intro, palm-muted eighths in the verse, big open chords in the chorus
        if intro:
            for i in range(8):
                m = midi(GUHFY_RIFF[(bar % 2) * 8 + i])
                s.zet(b0 + i * 0.5, gitaar([m, m + 7, m + 12], 0.5 * s.tel, seed + bar * 8 + i, gain=8, demp=(i % 2 == 1)), 0.3)
                s.zet(b0 + i * 0.5, bas(mhz(m), 0.45 * s.tel, seed + i), 0.45)
        elif 4 <= bar < 12:
            for i in range(8):
                s.zet(b0 + i * 0.5, gitaar(power, 0.5 * s.tel, seed + bar * 8 + i, gain=8, demp=True), 0.32)
                s.zet(b0 + i * 0.5, bas(mhz(root), 0.45 * s.tel, seed + i), 0.45)
        elif eind:
            if bar == 32:
                s.zet(b0, gitaar(power, 8 * s.tel, seed + bar, gain=8), 0.34)
                s.zet(b0, bas(mhz(root), 8 * s.tel, seed), 0.45)
                for i in range(16):
                    s.zet(b0 + 4 + i * 0.25, tom(200 - i * 7, seed + 700 + i, 0.6 + i * 0.03), 0.5)
                s.zet(b0 + 7.5, crash(seed + 801, 1.3, 3.5), 0.7)
                s.zet(b0 + 7.5, kick(seed + 802, 1.3), 1.0)
        else:
            for tel in (0, 2):
                s.zet(b0 + tel, gitaar(power, 2 * s.tel, seed + bar * 2 + tel, gain=8), 0.25)
            for i in range(8):
                s.zet(b0 + i * 0.5, bas(mhz(root), 0.45 * s.tel, seed + i), 0.45)
            if not brug:
                s.zet(b0, orgel(mhz(noten[0] + 24), 4 * s.tel, seed, 0.8) + orgel(mhz(noten[1] + 24), 4 * s.tel, seed, 0.8)
                      + orgel(mhz(noten[2] + 24), 4 * s.tel, seed, 0.8), 0.05)
    # the voice and the shouting town
    for k, regel in GUHFY_MELODIE.items():
        b = start[k]
        refrein = k in ("5", "7", "11", "13")
        melodie_op(s, regel, b, lambda f, d, sd, **kw: stem(f, d, sd, vib=0.01, adem=0.12, helder=1.0, **kw), 0.9,
                   seed=5500 + int(k) * 20, glij=True, klinkers=["a", "e", "oe", "a", "i", "e"])
        if refrein or k in ("9", "10") or int(k) >= 11:
            melodie_op(s, regel, b, lambda f, d, sd, **kw: koor([round(69 + 12 * np.log2(f / 440.0))], d, sd, "a", 3, transpose=-12),
                       0.32, seed=5700 + int(k) * 20)
    totaal = maten * maat
    x = s.mix(totaal, staart=0.0)
    x = g.galm(x, 1.2, 0.14, 51)[:len(x)]
    x *= g.env(len(x), 0.005, 1.2, 1.0)
    return luid(x)


MONSTER_MELODIE = {
    "1": [("C5", 1), ("A4", 1), ("F4", 1), ("A4", 2), ("C5", 1), ("E5", 2), ("D5", 1), ("C5", 3)],
    "2": [("D5", 1), ("Bb4", 1), ("G4", 1), ("A4", 2), ("F4", 1), ("G4", 2), ("A4", 1), ("G4", 3)],
    "3": [("C5", 1), ("A4", 1), ("F4", 1), ("D5", 2), ("Bb4", 1), ("C5", 2), ("A4", 1), ("F4", 3)],
    "4": [("D5", 1), ("D5", 1), ("D5", 1), ("E5", 2), ("C5", 1), ("A4", 1), ("G4", 1), ("F4", 1), ("F4", 3)],
}
# chords per bar (20): the hummed music-box start 2, four lines of 4 bars, the hummed end 2
MONSTER_AKKOORDEN = (["F", "C"] + ["F", "C", "F", "F", "Bb", "F", "C", "C", "F", "Bb", "F", "Dm", "Bb", "C", "F", "F"]
                     + ["F", "F"])
# the start: one monster hums a little phrase (the scene's monsters roared before; the song itself is all sweetness)
MONSTER_NEURIE = [("F4", 1), ("A4", 1), ("C5", 1), ("Bb4", 1.5), ("A4", .5), ("G4", 1)]


def brul(dur=2.9, seed=1):
    """A big friendly monster roar: low rough voices sliding down, noise through an open 'a'."""
    t = t_(dur)
    r = np.random.default_rng(seed)
    uit = np.zeros_like(t)
    for f0, a in ((92, 1.0), (61, 0.8), (138, 0.4)):
        fr = f0 * (1.25 - 0.35 * t / dur) * (1 + 0.03 * g.low(r.normal(0, 1, len(t)), 30) * 20)
        uit += a * additief(fr, 60, helling=0.9)
    uit = np.tanh(uit * 1.5)
    uit = formant(uit, "a", 0.7) + formant(r.normal(0, 1, len(t)), "a", 0.7) * 0.6
    return uit * g.env(len(t), 0.25, 0.9, 1.4)


def monsters():
    _, _, bpm, maat, maten, regels = liederen.LIEDEREN["monsters"]
    s = Stuk(bpm, maat)
    start = {k: b for k, b, _, _ in regels}
    seed = 7000
    melodie_op(s, MONSTER_NEURIE, 0, lambda f, d, sd, **kw: stem(f, d, sd, vib=0.02, adem=0.16, schaal=0.8, **kw), 0.5,
               seed=seed + 50, glij=True, klinkers=["oe"], transpose=-12)
    for bar, ak in enumerate(MONSTER_AKKOORDEN):
        b0 = bar * 3
        noten = akkoord(ak, 3)
        # the waltz: a low pizzicato root on 1, music-box chord tones on 2 and 3, a soft pad of humming monsters
        s.zet(b0, pizz(mhz(noten[0] - 12), 0.8, seed + bar), 0.55)
        for i, tel in enumerate((1, 2)):
            for j, m in enumerate(noten[1:]):
                s.zet(b0 + tel + j * 0.02, muziekdoos(mhz(m + 12), 0.4, seed + bar * 5 + i + j), 0.12)
        if bar >= 2:
            s.zet(b0, koor([noten[0], noten[2]], 3 * s.tel, seed + bar * 3, "oe", 2, schaal=0.85), 0.16)
            s.zet(b0, g.belletje(hz("A6"), 0.3, bar) if bar % 4 == 3 else np.zeros(1), 0.05)
    # the monsters sing the tune an octave down, a second voice another octave lower; the music box plays it high
    for k, regel in MONSTER_MELODIE.items():
        b = start[k]
        melodie_op(s, regel, b, lambda f, d, sd, **kw: stem(f, d, sd, vib=0.018, adem=0.14, schaal=0.82, **kw), 0.55,
                   seed=7500 + int(k) * 20, glij=True, klinkers=["oe", "o", "a", "oe"], transpose=-12)
        melodie_op(s, regel, b, lambda f, d, sd, **kw: stem(f, d, sd, vib=0.02, adem=0.2, schaal=0.7, **kw), 0.32,
                   seed=7600 + int(k) * 20, glij=True, klinkers=["o", "oe"], transpose=-24)
        melodie_op(s, regel, b, lambda f, d, sd: muziekdoos(f, d, sd), 0.16, seed=7700 + int(k) * 20, transpose=12)
    # the hummed end and a happy grumble
    b = start["slot"]
    s.zet(b, koor([midi("F3"), midi("A3"), midi("C4")], 5.5 * s.tel, seed + 99, "oe", 2, schaal=0.85), 0.3)
    s.zet(b + 4, brul(1.3, seed + 5) * 0.5, 0.35)
    totaal = maten * maat
    x = s.mix(totaal, staart=0.0)
    x = g.galm(x, 2.4, 0.3, 71)[:len(x)]
    x *= g.env(len(x), 0.01, 1.2, 1.0)
    return luid(x)


# =====================================================================================================================
# the tunes (every one loops; the BPM of trommel and kroeg is fixed in SpAudio.BPM, first beat = sample 0)
# =====================================================================================================================
def deuntje(bpm, maten, maat, vul, seed, galm_=(0.9, 0.12)):
    """A looping tune of `maten` bars: `vul(stuk, bar, b0)` puts the parts of one bar."""
    s = Stuk(bpm, maat)
    for bar in range(maten):
        vul(s, bar, bar * maat)
    lengte = s.seconden(maten * maat)
    x = s.mix(maten * maat, staart=2.0)
    x = g.galm(x, galm_[0], galm_[1], seed)
    return luid(rond(x, lengte), -21.0)


def bakken():
    """The grill: a busy, happy ukulele strum with a marimba tune and a shaker (G major, 120 BPM, 16 bars)."""
    ak = ["G", "C", "D", "G", "Em", "C", "D", "D"] * 2
    wijs = [("B4", .5), ("D5", .5), ("G5", 1), ("E5", .5), ("D5", .5), ("B4", 1),
            ("C5", .5), ("E5", .5), ("G5", 1), ("E5", 1), ("C5", 1),
            ("D5", .5), ("F#5", .5), ("A5", 1), ("F#5", .5), ("E5", .5), ("D5", 1),
            ("B4", .5), ("A4", .5), ("G4", 1), ("A4", .5), ("B4", .5), ("D5", 1)]

    def vul(s, bar, b0):
        n = akkoord(ak[bar], 4)
        for i, (tel, omhoog) in enumerate(((0, False), (1, False), (1.5, True), (2.5, True), (3, False), (3.5, True))):
            for t0, sig in strum(n, 0, 0.3, 100 + bar * 10 + i, omhoog):
                s.zet(b0 + tel + t0 / s.tel, sig, 0.28)
        s.zet(b0, tuba(mhz(akkoord(ak[bar], 2)[0]), 0.5 * s.tel, bar), 0.5)
        s.zet(b0 + 2, tuba(mhz(akkoord(ak[bar], 2)[2] - 12), 0.5 * s.tel, bar), 0.45)
        for i in range(8):
            s.zet(b0 + i * 0.5, shaker(bar * 8 + i, 1.0 if i % 2 else 0.6), 0.6)
        if bar % 4 == 0:
            melodie_op(s, wijs, b0, marimba, 0.45, seed=200 + bar)
    return deuntje(120, 16, 4, vul, 81)


def kwallen():
    """Jellyfishing: bouncy marimba and glockenspiel, a whistle tune, light hand drums (D major, 128 BPM, 16 bars)."""
    ak = ["D", "G", "A", "D", "D", "G", "A", "A"] * 2
    wijs = [("A5", .5), ("F#5", .5), ("A5", .5), ("B5", .5), ("A5", 1), ("F#5", 1),
            ("G5", .5), ("E5", .5), ("G5", .5), ("A5", .5), ("G5", 1), ("E5", 1),
            ("F#5", .5), ("A5", .5), ("D6", 1), ("C#6", .5), ("B5", .5), ("A5", 1),
            ("G5", .5), ("F#5", .5), ("E5", 1), ("C#5", 1), ("D5", 1)]

    def fluitje(f, d, sd):
        return g.fluit(f, d, sd, adem=0.06, vibrato=6.0, diepte=0.01) * 0.8

    def vul(s, bar, b0):
        n = akkoord(ak[bar], 4)
        for i, m in enumerate([n[0], n[1], n[2], n[1], n[0] + 12, n[2], n[1], n[2]]):
            s.zet(b0 + i * 0.5, marimba(mhz(m), 0.3, bar * 8 + i), 0.35)
        s.zet(b0, marimba(mhz(akkoord(ak[bar], 2)[0]), 0.8, bar), 0.5)
        s.zet(b0 + 2, marimba(mhz(akkoord(ak[bar], 2)[2]), 0.8, bar), 0.4)
        for tel, f in ((0, 160), (1.5, 210), (2, 160), (3, 210), (3.5, 240)):
            s.zet(b0 + tel, tom(f, bar * 5 + int(tel * 2), 0.5), 0.35)
        if bar % 4 == 0:
            melodie_op(s, wijs, b0, fluitje if bar % 8 == 0 else glock, 0.4 if bar % 8 == 0 else 0.3, seed=300 + bar)
    return deuntje(128, 16, 4, vul, 82)


def examen():
    """The boating exam: a nervous oompah tuba and a clarinet that keeps tripping over a chromatic note (Bb, 112 BPM)."""
    ak = ["Bb", "F", "Bb", "F", "Eb", "Bb", "C", "F"] * 2
    wijs = [("D5", .5), ("C#5", .5), ("D5", .5), ("F5", .5), ("Bb5", 1), ("A5", .5), ("Ab5", .5),
            ("G5", .5), ("F#5", .5), ("G5", .5), ("C5", .5), ("F5", 2),
            ("Eb5", .5), ("D5", .5), ("Eb5", .5), ("G5", .5), ("Bb5", 1), ("G5", 1),
            ("F5", .5), ("E5", .5), ("F5", .5), ("A4", .5), ("Bb4", 2)]

    def vul(s, bar, b0):
        n = akkoord(ak[bar], 3)
        for tel, m in ((0, n[0] - 12), (1, None), (2, n[2] - 24), (3, None)):
            if m is not None:
                s.zet(b0 + tel, tuba(mhz(m), 0.6 * s.tel, bar), 0.6)
            else:
                for j, mm in enumerate(n):
                    s.zet(b0 + tel, piano(mhz(mm + 12), 0.25, bar * 4 + j, 0.5), 0.12)
        s.zet(b0 + 1, snare(bar, 0.4), 0.25)
        s.zet(b0 + 3, snare(bar + 50, 0.4), 0.25)
        if bar % 4 == 0:
            melodie_op(s, wijs, b0, lambda f, d, sd: klarinet(f, d * 0.85, sd), 0.4, seed=400 + bar)
    return deuntje(112, 16, 4, vul, 83)


def sluipen():
    """Sneaking: tiptoe pizzicato, a bassoon on the off-beats and a few glockenspiel blinks (D minor, 96 BPM, 16 bars)."""
    ak = ["Dm", "Dm", "A", "Dm", "Gm", "Dm", "A", "A"] * 2
    wijs = [("D4", .5), (None, .5), ("F4", .5), (None, .5), ("A4", .5), (None, .5), ("G#4", .5), ("A4", .5),
            ("Bb4", .5), (None, .5), ("A4", .5), (None, .5), ("G4", .5), ("F4", .5), ("E4", 1)]

    def vul(s, bar, b0):
        n = akkoord(ak[bar], 3)
        for i, m in enumerate([n[0], n[2], n[1], n[2]]):
            s.zet(b0 + i, pizz(mhz(m - 12 if i == 0 else m), 0.3, bar * 4 + i), 0.55)
        for tel in (0.5, 2.5):
            s.zet(b0 + tel, fagot(mhz(n[0] - 12), 0.18, bar), 0.18)
        if bar % 2 == 0:
            melodie_op(s, wijs, b0, lambda f, d, sd: fagot(f, min(d, 0.25), sd), 0.3, seed=500 + bar)
        if bar % 4 == 3:
            s.zet(b0 + 3.5, glock(hz("A6"), 0.3, bar), 0.15)
        s.zet(b0, hat(bar, 0.4), 0.2)
    return deuntje(96, 16, 4, vul, 84, (1.1, 0.15))


def trommel():
    """The drum game: a clear pulse from sample 0 (a soft kick on every beat, sticks in bar 1, claps on 2 and 4), organ and
    bass under it (C major, 112 BPM, 16 bars). No drum melody: the player is the drummer."""
    ak = ["C", "C", "F", "G", "C", "Am", "F", "G"] * 2

    def vul(s, bar, b0):
        n = akkoord(ak[bar], 4)
        for tel in range(4):
            s.zet(b0 + tel, kick(bar * 4 + tel, 0.7), 0.6)
            if bar == 0:
                s.zet(b0 + tel, stokjes(tel), 0.6)
        if bar > 0:
            for tel in (1, 3):
                s.zet(b0 + tel, clap(bar * 2 + tel, 0.8), 0.35)
            s.zet(b0, orgel(mhz(n[0]), 4 * s.tel, bar, 0.5) + orgel(mhz(n[1]), 4 * s.tel, bar, 0.5) + orgel(mhz(n[2]), 4 * s.tel, bar, 0.5), 0.07)
            for i in range(4):
                s.zet(b0 + i, bas(mhz(akkoord(ak[bar], 2)[0]), 0.9 * s.tel, bar * 4 + i), 0.4)
            for i in range(8):
                s.zet(b0 + i * 0.5, shaker(bar * 8 + i, 0.8), 0.4)
    return deuntje(112, 16, 4, vul, 85, (0.7, 0.08))


def kroeg():
    """De Ruige Kroeg: a lazy honky-tonk shuffle on a slightly out-of-tune piano, an upright bass and brushes (A, 96 BPM,
    16 bars, first beat = sample 0; the stand-still game stops it while the Mikas look)."""
    ak = ["A7", "A7", "D7", "A7", "E7", "D7", "A7", "E7"] * 2

    def honky(f, d, sd):
        return piano(f, d, sd, 0.7) + piano(f * 1.006, d, sd + 1, 0.7) * 0.6

    def vul(s, bar, b0):
        n = akkoord(ak[bar], 3)
        for tel in range(4):
            # the shuffle: a long-short swing (2/3 + 1/3 of a beat)
            s.zet(b0 + tel, bas(mhz(n[[0, 2, 3, 2][tel]] - 12), 0.6 * s.tel, bar * 4 + tel), 0.45)
            s.zet(b0 + tel, shaker(bar * 4 + tel, 0.5), 0.3)
            s.zet(b0 + tel + 2 / 3, shaker(bar * 4 + tel + 99, 0.9), 0.35)
            if tel in (1, 3):
                for j, m in enumerate(n[1:]):
                    s.zet(b0 + tel, honky(mhz(m + 12), 0.25, bar * 8 + j), 0.12)
        if bar % 2 == 0:
            riff = [("E5", 2 / 3), ("F#5", 1 / 3), ("A5", 2 / 3), ("C6", 1 / 3), ("A5", 1), ("E5", 2 / 3), ("D5", 1 / 3), ("C#5", 1)]
            melodie_op(s, riff, b0 + 4 if bar % 4 == 2 else b0, honky, 0.2, seed=600 + bar,
                       transpose=0 if ak[bar].startswith("A") else (5 if ak[bar].startswith("D") else 7))
    return deuntje(96, 16, 4, vul, 86, (0.8, 0.1))


def ijssalon():
    """The Guhfy Guhber ice-cream parlour: a sweet music-box waltz over a soft organ (F major, 3/4, 120 BPM, 16 bars)."""
    ak = ["F", "C", "Bb", "F", "Dm", "Bb", "C", "C"] * 2
    wijs = [("A5", 1), ("C6", 1), ("A5", 1), ("G5", 2), ("E5", 1), ("F5", 1), ("A5", 1), ("C6", 1), ("Bb5", 3),
            ("G5", 1), ("Bb5", 1), ("G5", 1), ("F5", 2), ("D5", 1), ("E5", 1), ("G5", 1), ("Bb5", 1), ("A5", 3)]

    def vul(s, bar, b0):
        n = akkoord(ak[bar], 3)
        s.zet(b0, orgel(mhz(n[0]), 3 * s.tel, bar, 0.2) + orgel(mhz(n[1] + 12), 3 * s.tel, bar, 0.2), 0.06)
        s.zet(b0, pizz(mhz(n[0] - 12), 0.5, bar), 0.4)
        for tel in (1, 2):
            for j, m in enumerate(n[1:]):
                s.zet(b0 + tel, muziekdoos(mhz(m + 12), 0.3, bar * 6 + tel + j), 0.1)
        if bar % 8 == 0:
            melodie_op(s, wijs, b0, muziekdoos, 0.32, seed=700 + bar)
    return deuntje(120, 16, 3, vul, 87, (1.4, 0.2))


def emmers():
    """Plankmika's bucket regime: a toy march in minor (snare, tuba oompah, a toy trumpet) that is a little bit silly
    (A minor, 120 BPM, 16 bars)."""
    ak = ["Am", "E", "Am", "E", "Dm", "Am", "E", "Am"] * 2
    wijs = [("A4", .75), ("A4", .25), ("C5", 1), ("E5", 1), ("A5", 1), ("G#5", .75), ("F5", .25), ("E5", 2), (None, 1),
            ("D5", .75), ("D5", .25), ("F5", 1), ("A5", 1), ("G#5", .5), ("F5", .5), ("E5", 1.5), ("B4", .5), ("A4", 2), (None, 1)]

    def trompet(f, d, sd):
        return g.koper(f, d, sd) * 0.7

    def vul(s, bar, b0):
        n = akkoord(ak[bar], 3)
        s.zet(b0, tuba(mhz(n[0] - 12), 0.5 * s.tel, bar), 0.55)
        s.zet(b0 + 2, tuba(mhz(n[2] - 24), 0.5 * s.tel, bar + 1), 0.5)
        for tel in (1, 3):
            for j, m in enumerate(n):
                s.zet(b0 + tel, trompet(mhz(m + 12), 0.25, bar * 3 + j), 0.07)
        for i, (tel, st) in enumerate(((0, 1.0), (0.75, 0.4), (1, 0.6), (2, 1.0), (2.75, 0.4), (3, 0.6), (3.5, 0.5))):
            s.zet(b0 + tel, snare(bar * 8 + i, st * 0.6), 0.4)
        if bar % 4 == 0:
            melodie_op(s, wijs, b0, trompet, 0.3, seed=800 + bar)
    return deuntje(120, 16, 4, vul, 88)


# =====================================================================================================================
# the stingers
# =====================================================================================================================
def kort(x, gain=0.75, galm_=(0.8, 0.15), seed=1):
    return g.norm(g.galm(x, galm_[0], galm_[1], seed), gain)


def verteller():
    """The narrator's title card: a little French musette flourish on an accordion (C major), with an oom-pah under it."""
    s = Stuk(132)
    loop = [("G4", .25), ("C5", .25), ("E5", .25), ("G5", .25), ("F5", .25), ("E5", .25), ("D5", .25), ("F5", .25), ("E5", 2)]
    melodie_op(s, loop, 0, accordeon, 0.6, seed=901)
    s.zet(2, accordeon(hz("C5"), 1.6 * 60 / 132 * 2) + accordeon(hz("G4"), 1.6 * 60 / 132 * 2) * 0.7, 0.3)
    s.zet(0, tuba(hz("C2"), 0.3), 0.5)
    s.zet(1, tuba(hz("G2"), 0.3), 0.45)
    s.zet(2, tuba(hz("C2"), 0.8), 0.55)
    x = s.mix(4.5, staart=0.6)
    return kort(x * g.env(len(x), 0.002, 0.4, 1), 0.75, (1.0, 0.18), 91)


def klarinet_stinger():
    """Octo's clarinet: a proud little run up... and then a squeak and a sour note that droops."""
    s = Stuk(150)
    melodie_op(s, [("C5", .5), ("D5", .5), ("E5", .5), ("F5", .5), ("G5", 1)], 0, klarinet, 0.7, seed=911)
    t = t_(0.18)
    piep = additief(np.full(len(t), hz("G6") * 1.03), 8, helling=0.7, oneven=True) * g.env(len(t), 0.005, 0.04)
    s.zet(3, piep, 0.5)
    tz = t_(1.2)
    zuur = klarinet(hz("A5") * 0.97, 1.2, 913) * 1.0
    fr = hz("A5") * 0.97 * (1 - 0.06 * np.clip((tz - 0.4) / 0.8, 0, 1))
    zuur = g.low(additief(fr, 15, helling=1.2, oneven=True), 3000) * g.env(len(tz), 0.03, 0.3, 1)
    s.zet(3.4, zuur, 0.6)
    x = s.mix(6.4, staart=0.4)
    return kort(x, 0.45, (0.6, 0.12), 92)


def karen():
    """Karen's beeps: two bright computer blips and a low, satisfied boop."""
    def blip(f, d):
        t = t_(d)
        return g.low(np.sign(np.sin(2 * np.pi * f * t)), 5000) * g.env(len(t), 0.004, 0.02)
    x = g.mix(0.9, (0.0, blip(1318, 0.08) * 0.5), (0.12, blip(1760, 0.08) * 0.5), (0.3, blip(440, 0.22) * 0.6))
    return kort(x, 0.38, (0.3, 0.05), 93)


def bevries():
    """Freezing: a glassy shimmer that falls fast, ice crackling, a soft thud of frost."""
    r = np.random.default_rng(94)
    delen = [(i * 0.045, g.belletje(hz("D7") * 2 ** (-i / 8), 0.6, i) * 0.35) for i in range(16)]
    t = t_(1.6)
    kraak = g.high(r.normal(0, 1, len(t)), 3000) * (r.uniform(0, 1, len(t)) > 0.985) * 2.0 * np.exp(-t * 1.5)
    delen.append((0.5, kraak))
    delen.append((0.7, g.trom(55, 0.7, 2, 0.5) * 0.5))
    x = g.mix(2.4, *delen)
    return kort(x, 0.75, (1.4, 0.25), 94)


def snor():
    """The moustache: a twangy jaw-harp boing and a tiny kazoo 'ta-daa'."""
    t = t_(0.8)
    f0 = 110
    bron = additief(np.full(len(t), f0), 60, helling=0.8)
    # the jaw harp's mouth: a formant that sweeps up and down fast
    s_ = g._sig()
    x = np.zeros_like(t)
    for i in range(0, len(t), 512):
        fc = 600 + 900 * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t[i]))
        b, a = s_.iirpeak(fc / (SR / 2), 6)
        x[i:i + 512] = s_.lfilter(b, a, bron[i:i + 512])
    x *= np.exp(-t * 3.5) * g.env(len(t), 0.003, 0.1)
    taa = g.mix(0.9, (0.0, g.kazoo(hz("G5"), 0.12, 1) * 0.4), (0.15, g.kazoo(hz("C6"), 0.5, 2) * 0.45))
    y = g.mix(1.8, (0.0, x * 0.9), (0.75, taa))
    return kort(y, 0.7, (0.5, 0.1), 95)


def opgelost():
    """Solved: an ukulele strum up and a glockenspiel that climbs to a bright C."""
    delen = strum(akkoord("C", 4), 0.0, 0.8, 960, True, 0.02)
    delen += [(0.15 + i * 0.08, glock(hz(n), 0.6, i) * 0.35) for i, n in enumerate(["C6", "E6", "G6", "C7"])]
    x = g.mix(2.0, *delen)
    return kort(x, 0.7, (0.9, 0.15), 96)


def mislukt():
    """A miss: a soft springy 'boing' and a little wooden bonk. Never sad."""
    t = t_(0.9)
    fr = 260 * (1 + 0.25 * np.exp(-t * 4) * np.sin(2 * np.pi * 11 * t)) * (1 - 0.1 * t)
    x = np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.exp(-t * 3.5) * g.env(len(t), 0.004, 0.1)
    y = g.mix(1.1, (0.0, g.tik(0.12, 1, 700) * 0.4), (0.03, x * 0.8))
    return kort(y, 0.6, (0.4, 0.1), 97)


def lach():
    """A giggly guh laugh, 'njehehehe': quick voiced bursts that fall, then a squeak that rises."""
    s = Stuk(60)
    t0 = 0.0
    n = 7
    for i in range(n):
        f = 640 * (1 - 0.045 * i)
        d = 0.085
        s.zet(t0, stem(f, d, 980 + i, "e" if i else "i", vib=0.0, adem=0.25, schaal=1.15), 0.8)
        t0 += 0.13 - 0.004 * i
    tt = t_(0.35)
    piep = stem(700, 0.35, 999, "i", vib=0.0, adem=0.2, schaal=1.2)
    s.zet(t0 + 0.05, piep * np.linspace(0.6, 1.0, len(piep)), 0.6)
    x = s.mix(t0 + 0.6, staart=0.3)
    return kort(x, 0.7, (0.4, 0.08), 98)


# name of the file (without .ogg) -> what makes it; the order of SpAudio's lists
EIGEN = {
    "moment_zoete_zege": zoete_zege, "moment_guhfy_guhber": guhfy_guhber, "moment_monsters": monsters,
    "deuntje_bakken": bakken, "deuntje_kwallen": kwallen, "deuntje_examen": examen, "deuntje_sluipen": sluipen,
    "deuntje_trommel": trommel, "deuntje_kroeg": kroeg, "deuntje_ijssalon": ijssalon, "deuntje_emmers": emmers,
    "stinger_verteller": verteller, "stinger_klarinet": klarinet_stinger, "stinger_karen": karen, "stinger_bevries": bevries,
    "stinger_snor": snor, "stinger_opgelost": opgelost, "stinger_mislukt": mislukt, "stinger_lach": lach,
}


# =====================================================================================================================
# the real pieces (the ledger)
# =====================================================================================================================
def muziek(bron, alleen=None):
    sf = g._sf()
    s = g._sig()
    for stuk in json.load(open(LEDGER, encoding="utf-8"))["stukken"]:
        uit = stuk["naam"].replace(".", "_")
        if alleen and uit not in alleen:
            continue
        x, sr = sf.read(os.path.join(bron, stuk["bestand"]), dtype="float32", always_2d=True)
        x = x.mean(axis=1)
        if sr != SR:
            x = s.resample_poly(x, SR, sr).astype(np.float32)
        knip = stuk.get("knip")
        if knip:
            x = x[int(knip[0] * SR):int(knip[1] * SR)]
            x = x * g.env(len(x), 0.01, 6.0, 1.3).astype(np.float32)
        stil = np.where(np.abs(x) > 1e-3)[0]
        if len(stil):
            x = x[max(0, stil[0] - int(0.1 * SR)):min(len(x), stil[-1] + int(0.1 * SR))]
        save(uit, luid(x), NIVEAU_MUZIEK)


def main(argv):
    if len(argv) > 1 and argv[1] == "--muziek":
        if len(argv) < 3:
            raise SystemExit("usage: einde2_sp_audio_geluid.py --muziek <map met de bronbestanden> [naam ...]")
        muziek(argv[2], set(argv[3:]) or None)
        return
    alleen = set(argv[1:])
    for naam, maak in EIGEN.items():
        if not alleen or naam in alleen:
            save(naam, maak())


if __name__ == "__main__":
    main(sys.argv)
