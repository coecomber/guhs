"""
Het Snuffeleiland - the island tune, synthesized (mono OGG Vorbis, 48 kHz, like the other guh record) into
assets/guhs/sounds/music_disc_snuffeleiland.ogg. The ONE file behind the ONE sound event guhs:music_disc.snuffeleiland
(the music disc in a jukebox, and very softly the Guhstation's window).

OUR OWN composition, written here as notes; nothing is sampled, converted or transcribed. "Pootjes in het zand": a puppy
trotting along the beach.

  key      G major              tempo  116 BPM, 4/4, a light shuffle (the off-beat eighths come a little late)
  form     A (8 bars) - A' (8) - B (8) - A'' (8) - tag (4) = 36 bars = 74.5 s; the tag's last bar leads back into A and the
           tails of the end are folded onto the start, so the file loops without a seam
  voices   marimba   the melody (A, A', A'', tag) and the answers in B
           whistle   doubles the melody an octave up from A' on, leads in B
           glock     sparkles on top in A''
           ukulele   plucked strings (Karplus-Strong), the off-beat chords
           bass      a soft round bass on one and three, with little walks
           percussion a shaker on the eighths, a soft thump on one and three, a woodblock, a rim tick on two and four
  mix      a short room, gentle compression by tanh, peak 0.89

Not part of make_resources.py (the OGG is committed like the other sounds; sounds.json, the jukebox song and the disc's
item come from tools/features/snuffel.py). Deterministic (fixed seed), but the Vorbis encoder is not byte-for-byte
reproducible: after a re-run use `python tools/keep_unchanged.py --keep <the ogg>`. Needs numpy, scipy and soundfile:
    python tools/features/snuffel_geluid.py [output.ogg]        (from the project root)

Want another tune on your own machine? A resource pack overrides it: put your own OGG at
assets/guhs/sounds/music_disc_snuffeleiland.ogg in the pack (see the wiki page of the Guhstation).
"""
import os
import sys

import numpy as np
import soundfile as sf
from scipy.signal import butter, fftconvolve, lfilter

SR = 48000
BPM = 116.0
BEAT = 60.0 / BPM
SWING = 0.58          # where the off-beat eighth falls inside its beat (0.5 = straight)
BARS = 36
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "music_disc_snuffeleiland.ogg")
rng = np.random.default_rng(141)

NOTES = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def midi(name):
    """'F#4' -> 66."""
    letter, rest = name[0], name[1:]
    sharp = rest.startswith("#")
    return 12 * (int(rest[1:] if sharp else rest) + 1) + NOTES[letter] + (1 if sharp else 0)


def hz(m):
    return 440.0 * 2 ** ((m - 69) / 12.0)


def tijd(beat):
    """Seconds of a position in beats, with the shuffle on the off-beat eighths."""
    heel = np.floor(beat)
    frac = beat - heel
    if abs(frac - 0.5) < 1e-6:
        frac = SWING
    return (heel + frac) * BEAT


# =====================================================================================================================
# the tune (written from scratch): one string per bar, "note:beats", r = rest; every bar is four beats
# =====================================================================================================================
A_KOP = [
    "D5:.5 B4:.5 G4:.5 B4:.5 D5:1 E5:.5 D5:.5",          # G    the trot
    "B4:1.5 r:.5 G4:.5 A4:.5 B4:1",                      # G
    "C5:.5 E5:.5 G5:1 E5:.5 C5:.5 E5:1",                 # C    a little jump
    "D5:1.5 B4:.5 G4:2",                                 # G
]
A1 = A_KOP + [
    "E5:.5 E5:.5 G5:.5 E5:.5 B4:1 r:.5 B4:.5",           # Em   sniff, sniff
    "C5:.5 D5:.5 E5:.5 C5:.5 A4:1 G4:1",                 # C
    "F#4:.5 A4:.5 D5:1 C5:.5 A4:.5 F#4:1",               # D
    "A4:1 r:1 D4:.5 F#4:.5 A4:.5 C5:.5",                 # D    running up to the start again
]
A2 = A_KOP + [
    "E5:.5 E5:.5 G5:.5 E5:.5 B4:1 r:.5 B4:.5",           # Em
    "C5:.5 E5:.5 G5:1 F#5:.5 E5:.5 D5:1",                # C D
    "G5:1.5 D5:.5 B4:1 G4:1",                            # G
    "G4:2 r:2",                                          # G
]
B = [
    "E5:1 G5:.5 E5:.5 C5:1 r:1",                         # C    the whistle calls...
    "E5:.5 G5:.5 A5:1 G5:.5 E5:.5 C5:1",                 # C
    "D5:1 G5:.5 D5:.5 B4:1 r:1",                         # G
    "D5:.5 E5:.5 G5:1 E5:.5 D5:.5 B4:1",                 # G
    "C5:1 E5:.5 C5:.5 A4:1 r:1",                         # Am
    "A4:.5 C5:.5 E5:.5 A5:.5 G5:1 E5:1",                 # Am
    "F#5:1 A5:.5 F#5:.5 D5:1 A4:1",                      # D
    "A4:.5 C5:.5 D5:.5 F#5:.5 A5:1 r:.5 D5:.5",          # D7
]
B_ANTWOORD = [  # ...and the marimba answers in the gaps
    "r:3 G4:.5 C5:.5", "r:4", "r:3 G4:.5 B4:.5", "r:4", "r:3 E4:.5 A4:.5", "r:4", "r:4", "r:4",
]
TAG = [
    "B4:.5 D5:.5 G5:1 D5:.5 B4:.5 G4:1",                 # G
    "C5:.5 E5:.5 G5:1 E5:.5 C5:.5 E5:1",                 # C
    "D5:1 B4:1 A4:.5 F#4:.5 A4:1",                       # G D
    "G4:1 B4:.5 D5:.5 G5:1 r:1",                         # G    and round we go
]
MELODIE = A1 + A2 + B + A2 + TAG

# two chords per bar (half bars)
AKK_A1 = ["G G", "G G", "C C", "G G", "Em Em", "C C", "D D", "D D7"]
AKK_A2 = ["G G", "G G", "C C", "G G", "Em Em", "C D", "G G", "G D7"]
AKK_B = ["C C", "C C", "G G", "G G", "Am Am", "Am Am", "D D", "D7 D7"]
AKK_TAG = ["G G", "C C", "G D", "G D7"]
AKKOORDEN = [c for bar in AKK_A1 + AKK_A2 + AKK_B + AKK_A2[:7] + ["G G"] + AKK_TAG for c in bar.split()]

# the ukulele's four strings (g' c' e' a') per chord, and the bass's root and fifth
UKE = {"G": ["G4", "D4", "G4", "B4"], "C": ["G4", "C4", "E4", "C5"], "Em": ["G4", "E4", "G4", "B4"], "Am": ["A4", "C4", "E4", "A4"],
       "D": ["A4", "D4", "F#4", "A4"], "D7": ["A4", "D4", "F#4", "C5"]}
BAS = {"G": ("G2", "D3"), "C": ("C3", "G2"), "Em": ("E2", "B2"), "Am": ("A2", "E3"), "D": ("D3", "A2"), "D7": ("D3", "A2")}


def lees(bars):
    """[(beat, midi, beats)] of a list of bars."""
    uit = []
    for i, bar in enumerate(bars):
        pos = 0.0
        for stuk in bar.split():
            naam, lengte = stuk.split(":")
            lengte = float(lengte)
            if naam != "r":
                uit.append((i * 4 + pos, midi(naam), lengte))
            pos += lengte
        assert abs(pos - 4.0) < 1e-6, f"bar {i} is {pos} beats: {bar}"
    return uit


# =====================================================================================================================
# the instruments (soft envelopes, sines and plucked strings: no square waves)
# =====================================================================================================================
def filt(x, soort, f, order=2):
    b, a = butter(order, np.asarray(f) / (SR / 2), btype=soort)
    return lfilter(b, a, x)


def marimba(m, dur, vel=1.0):
    """A wooden bar: the fundamental, the bar's fourth harmonic, a soft mallet tap."""
    n = int((min(dur, 1.2) + 0.5) * SR)
    t = np.arange(n) / SR
    f = hz(m)
    s = np.sin(2 * np.pi * f * t) * np.exp(-t / 0.32)
    s += 0.32 * np.sin(2 * np.pi * 4 * f * t) * np.exp(-t / 0.07)
    s += 0.10 * np.sin(2 * np.pi * 9.2 * f * t) * np.exp(-t / 0.025)
    tik = filt(rng.normal(0, 1, n), "band", [900, 3200]) * np.exp(-t / 0.006) * 0.25
    return (s + tik) * np.minimum(1.0, t / 0.003) * vel


def glock(m, dur, vel=1.0):
    n = int(1.1 * SR)
    t = np.arange(n) / SR
    f = hz(m)
    s = np.sin(2 * np.pi * f * t) * np.exp(-t / 0.4) + 0.25 * np.sin(2 * np.pi * 2.76 * f * t) * np.exp(-t / 0.15)
    return s * np.minimum(1.0, t / 0.002) * vel


def fluit(m, dur, vel=1.0):
    """A whistle: a sine with a little scoop into the note, a late vibrato and a breath of air."""
    lang = max(dur * BEAT * 0.92, 0.12)
    n = int((lang + 0.12) * SR)
    t = np.arange(n) / SR
    f = hz(m)
    scoop = 1.0 - 0.035 * np.exp(-t / 0.03)
    vib = 1.0 + 0.006 * np.sin(2 * np.pi * 5.6 * t) * np.clip((t - 0.16) / 0.2, 0, 1)
    fase = 2 * np.pi * np.cumsum(f * scoop * vib) / SR
    s = np.sin(fase) + 0.06 * np.sin(2 * fase)
    lucht = filt(rng.normal(0, 1, n), "band", [f * 0.9, min(f * 1.15, SR * 0.45)]) * 0.5
    env = np.minimum(1.0, t / 0.045) * np.clip((lang + 0.10 - t) / 0.10, 0, 1)
    return (s + lucht) * env * vel


def snaar(m, dur, vel=1.0):
    """A nylon string (Karplus-Strong, period by period): bright at the pluck, mellow right after."""
    f = hz(m)
    periode = int(round(SR / f))
    n = int(min(dur, 1.4) * SR)
    buf = filt(rng.uniform(-1, 1, periode), "low", 4500)
    buf -= buf.mean()
    stukken = []
    for _ in range(n // periode + 1):
        stukken.append(buf)
        buf = 0.5 * (buf + np.roll(buf, 1)) * 0.992
    s = np.concatenate(stukken)[:n]
    t = np.arange(n) / SR
    return s * np.minimum(1.0, t / 0.002) * np.clip((n / SR - t) / 0.03, 0, 1) * vel


def bas(m, dur, vel=1.0):
    lang = dur * BEAT
    n = int((lang + 0.08) * SR)
    t = np.arange(n) / SR
    f = hz(m)
    s = np.sin(2 * np.pi * f * t) + 0.28 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / 0.12) + 0.08 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t / 0.06)
    env = np.minimum(1.0, t / 0.012) * (0.55 + 0.45 * np.exp(-t / 0.25)) * np.clip((lang + 0.06 - t) / 0.06, 0, 1)
    return s * env * vel


def plof(vel=1.0):
    """A soft thump (a paw in the sand)."""
    n = int(0.22 * SR)
    t = np.arange(n) / SR
    fase = 2 * np.pi * np.cumsum(48 + 70 * np.exp(-t / 0.025)) / SR
    return np.sin(fase) * np.exp(-t / 0.07) * np.minimum(1.0, t / 0.004) * vel


def schud(vel=1.0, lang=0.05):
    n = int(0.12 * SR)
    t = np.arange(n) / SR
    return filt(rng.normal(0, 1, n), "band", [5200, 11000]) * np.minimum(1.0, t / 0.008) * np.exp(-t / lang) * vel


def blokje(f=1250.0, vel=1.0):
    n = int(0.12 * SR)
    t = np.arange(n) / SR
    return (np.sin(2 * np.pi * f * t) + 0.4 * np.sin(2 * np.pi * f * 2.4 * t)) * np.exp(-t / 0.022) * np.minimum(1.0, t / 0.001) * vel


def tik(vel=1.0):
    n = int(0.06 * SR)
    t = np.arange(n) / SR
    return (filt(rng.normal(0, 1, n), "band", [1800, 5000]) * 0.6 + np.sin(2 * np.pi * 820 * t)) * np.exp(-t / 0.012) * vel


# =====================================================================================================================
# the arrangement
# =====================================================================================================================
class Spoor:
    """One voice: a buffer with room for the tails after the last bar."""

    def __init__(self):
        self.x = np.zeros(int((BARS * 4 * BEAT + 4.0) * SR))

    def zet(self, beat, klank, later=0.0):
        i = int((tijd(beat) + later) * SR)
        self.x[i:i + len(klank)] += klank[:len(self.x) - i]


def sectie(bar):
    return "A1" if bar < 8 else "A2" if bar < 16 else "B" if bar < 24 else "A3" if bar < 32 else "TAG"


def arrangeer():
    mar, flu, glo, uke, bs, perc = Spoor(), Spoor(), Spoor(), Spoor(), Spoor(), Spoor()
    # --- the melody -------------------------------------------------------------------------------------------------
    for beat, m, lengte in lees(MELODIE):
        deel = sectie(int(beat // 4))
        accent = 1.0 if beat % 1 == 0 else 0.84
        if deel == "B":
            flu.zet(beat, fluit(m, lengte, 0.95 * accent))
        else:
            mar.zet(beat, marimba(m, lengte * BEAT, accent))
            if deel != "A1":
                flu.zet(beat, fluit(m + 12, lengte, 0.42 * accent))
            if deel == "A3" and beat % 1 == 0:
                glo.zet(beat, glock(m + 24, lengte, 0.5))
    for beat, m, lengte in lees(B_ANTWOORD):
        mar.zet(16 * 4 + beat, marimba(m, lengte * BEAT, 0.85))
        mar.zet(16 * 4 + beat, marimba(m + 12, lengte * BEAT, 0.45))
    # --- the ukulele: a soft pluck on the beat, the chord on the off-beats --------------------------------------------
    for half, akkoord in enumerate(AKKOORDEN):
        bar = half // 2
        if sectie(bar) == "TAG" and bar == BARS - 1 and half % 2 == 1:
            continue                                         # (the last half bar breathes)
        tonen = [midi(n) for n in UKE[akkoord]]
        for k in range(2):                                   # two beats per half bar
            beat = half * 2 + k
            uke.zet(beat, snaar(tonen[1], 0.5, 0.5))         # the low string on the beat
            for j, m in enumerate(tonen):                    # the strum on the "and", string after string
                uke.zet(beat + 0.5, snaar(m, 0.45, 0.62), later=j * 0.011)
            if k == 1 and sectie(bar) in ("A2", "A3", "TAG"):   # a little extra up-stroke
                for j, m in enumerate(reversed(tonen[1:])):
                    uke.zet(beat, snaar(m, 0.3, 0.3), later=j * 0.008)
    # --- the bass: root on one, fifth on three, a walk into a new chord ---------------------------------------------
    for half, akkoord in enumerate(AKKOORDEN):
        bar = half // 2
        grond, kwint = (midi(n) for n in BAS[akkoord])
        volgende = AKKOORDEN[(half + 1) % len(AKKOORDEN)]
        zelfde_bar = AKKOORDEN[half ^ 1] == akkoord
        toon = grond if half % 2 == 0 or not zelfde_bar else kwint
        if bar == BARS - 1 and half % 2 == 1:
            bs.zet(half * 2, bas(grond, 1.0, 0.8))
            continue
        bs.zet(half * 2, bas(toon, 1.4, 1.0))
        if volgende != akkoord and half % 2 == 1:            # walk up (or down) to the next root
            doel = midi(BAS[volgende][0])
            bs.zet(half * 2 + 1.5, bas(doel - 2 if doel > toon else doel + 2, 0.45, 0.7))
        elif sectie(bar) != "A1":
            bs.zet(half * 2 + 1.5, bas(toon, 0.4, 0.5))
    # --- percussion ---------------------------------------------------------------------------------------------------
    for bar in range(BARS):
        deel = sectie(bar)
        for achtste in range(8):
            beat = bar * 4 + achtste * 0.5
            af = achtste % 2 == 1
            if bar > 0 or achtste >= 4:                      # the shaker comes in half a bar late
                perc.zet(beat, schud(0.55 if af else 0.3, 0.035 if af else 0.02))
        if deel != "A1" or bar >= 4:
            perc.zet(bar * 4, plof(0.9))
            perc.zet(bar * 4 + 2, plof(0.7))
        if deel in ("A2", "A3", "TAG"):
            perc.zet(bar * 4 + 1, tik(0.3))
            perc.zet(bar * 4 + 3, tik(0.36))
        if deel in ("B", "A3"):                              # a woodblock pattern: tok . tok-tok
            for b_, f, v in ((0.5, 1250, 0.5), (2.5, 1250, 0.4), (3.0, 1560, 0.5)):
                perc.zet(bar * 4 + b_, blokje(f, v))
        if bar % 8 == 7 and bar != BARS - 1:                 # a little fill into the next part
            for b_, f in ((3.0, 1250), (3.5, 1560)):
                perc.zet(bar * 4 + b_, blokje(f, 0.6))
    return {"marimba": (mar.x, 0.55), "fluit": (flu.x, 0.20), "glock": (glo.x, 0.10), "ukulele": (filt(uke.x, "high", 180), 0.44),
            "bas": (bs.x, 0.28), "percussie": (perc.x, 0.30)}


def kamer(x):
    """A short, light room."""
    n = int(0.9 * SR)
    t = np.arange(n) / SR
    ir = filt(np.random.default_rng(7).normal(0, 1, n), "band", [300, 6000]) * np.exp(-t / 0.22)
    ir[:int(0.012 * SR)] = 0
    nat = fftconvolve(x, ir)[:len(x)]
    return x + nat / np.max(np.abs(nat)) * np.max(np.abs(x)) * 0.11


def maak():
    sporen = arrangeer()
    mix = sum(x * vol / (np.max(np.abs(x)) or 1.0) for x, vol in sporen.values())
    mix = kamer(mix)
    # the loop: everything after the last bar (tails, the room) folds onto the start
    lengte = int(round(BARS * 4 * BEAT * SR))
    lus = mix[:lengte].copy()
    staart = mix[lengte:]
    lus[:len(staart)] += staart
    lus = filt(lus, "high", 35)
    lus = lus / np.max(np.abs(lus))
    lus = np.tanh(lus * 1.2) / np.tanh(1.2)                 # gentle glue
    return lus / np.max(np.abs(lus)) * 0.89


def main():
    uit = sys.argv[1] if len(sys.argv) > 1 else OUT
    x = maak()
    os.makedirs(os.path.dirname(uit) or ".", exist_ok=True)
    # (in blocks: libsndfile's Vorbis encoder crashes on a whole song handed over at once)
    with sf.SoundFile(uit, "w", samplerate=SR, channels=1, format="OGG", subtype="VORBIS") as f:
        data = x.astype(np.float32)
        for i in range(0, len(data), SR):
            f.write(data[i:i + SR])
    print(f"wrote {uit}: {len(x) / SR:.2f} s, {BARS} bars at {BPM:g} BPM, peak {np.max(np.abs(x)):.2f}, rms {np.sqrt(np.mean(x ** 2)):.3f}")


if __name__ == "__main__":
    main()
