"""
Op Safari! (1.6.0), test round 4 (design ronde 47): the tune of the pinguïnparade, synthesized (mono OGG, 44.1 kHz) into
assets/guhs/sounds/safari/parade/:

  parade1-4     four phrases of a cheerful little march (120 bpm, two bars each, exactly 4.0 s): a flute (a soft sine
                with breath and a little vibrato) over a drum (bass drum on 1 and 3, a snappy snare on 2 and 4, a roll
                into the next phrase). momenten/Parade plays them one after the other at the tamboer-guh (FRASE = 80
                ticks), so the order 1-2-3-4 makes the whole tune: A, B, A', end.

Not part of make_resources.py (the OGGs are committed, like the other sounds). Needs numpy, scipy and soundfile:
    python tools/features/safari_parade_geluid.py        (from the project root)
The sound events: feature/safari/spullen/SafariGeluid.PARADE, sounds.json by tools/features/safari_quests.py (sounds).
"""
import os

import numpy as np

SR = 44100
BPM = 120
BEAT = 60 / BPM
FRASE = 8 * BEAT                       # two bars of 4/4: 4.0 s
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "safari", "parade")
rng = np.random.default_rng(20261021)

# note names -> semitones from C5 (the flute sits around C5-A5)
NOTEN = {"G4": -5, "A4": -3, "B4": -1, "C5": 0, "D5": 2, "E5": 4, "F5": 5, "G5": 7, "A5": 9, "B5": 11, "C6": 12}

# the melody per phrase: (note, beats); "-" is a rest. Every phrase is 8 beats.
MELODIE = [
    [("C5", 0.5), ("E5", 0.5), ("G5", 1), ("G5", 0.5), ("A5", 0.5), ("G5", 1),
     ("E5", 0.5), ("G5", 0.5), ("F5", 0.5), ("D5", 0.5), ("E5", 1), ("C5", 1)],
    [("D5", 0.5), ("E5", 0.5), ("F5", 1), ("A5", 0.5), ("G5", 0.5), ("F5", 1),
     ("E5", 0.5), ("D5", 0.5), ("C5", 0.5), ("D5", 0.5), ("E5", 1), ("G4", 1)],
    [("C5", 0.5), ("E5", 0.5), ("G5", 1), ("G5", 0.5), ("A5", 0.5), ("C6", 1),
     ("B5", 0.5), ("A5", 0.5), ("G5", 0.5), ("E5", 0.5), ("F5", 1), ("D5", 1)],
    [("E5", 0.5), ("F5", 0.5), ("G5", 0.5), ("E5", 0.5), ("F5", 0.5), ("D5", 0.5), ("E5", 1),
     ("D5", 0.5), ("B4", 0.5), ("C5", 2.5), ("-", 0.5)],
]


def _sig():
    import scipy.signal
    return scipy.signal


def band(x, lo, hi, order=2):
    b, a = _sig().butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return _sig().lfilter(b, a, x)


def low(x, f, order=2):
    b, a = _sig().butter(order, f / (SR / 2), btype="low")
    return _sig().lfilter(b, a, x)


def env(n, a, r):
    e = np.ones(n)
    ka, kr = max(1, int(a * SR)), max(1, int(r * SR))
    e[:ka] = np.linspace(0, 1, ka)
    e[-kr:] *= np.linspace(1, 0, kr)
    return e


def fluit(f0, dur):
    """A flute tone: sine with a soft 2nd and 3rd harmonic, a little delayed vibrato and some breath."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    vib = 1 + 0.006 * np.sin(2 * np.pi * 5.5 * t) * np.clip(t / 0.15, 0, 1)
    fase = 2 * np.pi * np.cumsum(f0 * vib) / SR
    x = np.sin(fase) + 0.22 * np.sin(2 * fase) + 0.06 * np.sin(3 * fase)
    adem = band(rng.standard_normal(n), f0 * 0.9, min(f0 * 3, 15000)) * 0.05
    adem[: int(0.04 * SR)] *= 3                    # the "tu" of the tongue at the start
    return (x + adem) * env(n, 0.02, min(0.08, dur * 0.3))


def basdrum(n):
    t = np.arange(n) / SR
    f = 55 + 90 * np.exp(-t * 30)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 9)


def snare(n, sterk=1.0):
    t = np.arange(n) / SR
    ruis = band(rng.standard_normal(n), 1500, 9000) * np.exp(-t * 22)
    toon = np.sin(2 * np.pi * 190 * t) * np.exp(-t * 30) * 0.5
    return (ruis + toon) * sterk


def frase(i):
    n = int(FRASE * SR)
    uit = np.zeros(n)
    # the flute
    pos = 0.0
    for noot, tel in MELODIE[i]:
        dur = tel * BEAT
        if noot != "-":
            f0 = 523.25 * 2 ** (NOTEN[noot] / 12)
            x = fluit(f0, dur * 0.92)
            a = int(pos * SR)
            uit[a:a + len(x)] += 0.55 * x[: n - a]
        pos += dur
    # the drum: bass on 1 and 3, snare on 2 and 4 (a little ghost note between), a roll at the end of phrases 2 and 3
    trom = np.zeros(n)
    for beat in range(8):
        a = int(beat * BEAT * SR)
        k = min(int(0.4 * SR), n - a)
        if beat % 2 == 0:
            trom[a:a + k] += 0.9 * basdrum(k)
        else:
            trom[a:a + k] += 0.55 * snare(k)
        g = int((beat + 0.75) * BEAT * SR)
        if beat % 2 == 1 and g < n:
            kg = min(int(0.15 * SR), n - g)
            trom[g:g + kg] += 0.18 * snare(kg)
    if i in (1, 2):
        for s in range(8):
            a = int((7 * BEAT + s * BEAT / 8) * SR)
            kk = min(int(0.1 * SR), n - a)
            trom[a:a + kk] += (0.2 + 0.04 * s) * snare(kk)
    if i == 3:                                     # the end: one big bass + snare on the last beat
        a = int(6 * BEAT * SR)
        k = min(int(0.6 * SR), n - a)
        trom[a:a + k] += 0.8 * basdrum(k) + 0.6 * snare(k)
    uit += 0.7 * low(trom, 9000)
    return uit


def save(name, x, peak=0.8):
    import soundfile as sf
    path = os.path.join(OUT, name + ".ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    x = x * env(len(x), 0.002, 0.02)
    data = (x / max(1e-9, np.max(np.abs(x))) * peak).astype(np.float32)
    with sf.SoundFile(path, "w", SR, 1, format="OGG", subtype="VORBIS") as f:   # in blocks: one big write overflows
        for i in range(0, len(data), 8192):                                    # libvorbis' stack on Windows
            f.write(data[i:i + 8192])
    print("wrote", name, f"{len(x) / SR:.2f}s")


if __name__ == "__main__":
    for i in range(4):
        save(f"parade{i + 1}", frase(i), 0.7)
