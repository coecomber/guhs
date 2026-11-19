"""
1.10.10 Guhwinter, slice hoentjes - the sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/guhwinter_hoentjes/:

  waterhoen_roep1-3    the waterhoentje's "krrr-uk!": a short purring rattle, then a bright little "uk" (a moorhen's call,
                       pitched up and softened: cute, not loud)
  waterhoen_piep1-2    hurt: a high squeaky peep
  alpinehoen_roep1-3   the alpine hoentje's croaky "arr-arr-ka-ka-ka": nasal pulses that speed up (a ptarmigan's rattle,
                       made small and round)
  alpinehoen_piep1-2   hurt: a soft muffled peep (it's all fluff)
  noway                the NOWAY jingle: a quick sparkly rising arpeggio
  noway_extreem        the even more NOWAY jingle: faster, higher, with a little "whoop" slide and a sparkle on top

Not part of make_resources.py (the OGGs are committed like the other sounds; guhwinter_hoentjes.py only writes sounds.json).
Needs numpy, scipy and soundfile:    python tools/features/guhwinter_hoentjes_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "guhwinter_hoentjes")


def band(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def low(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="low")
    return lfilter(b, a, x)


def env(n, attack, release, curve=2.0):
    e = np.ones(n)
    a, r = max(1, int(attack * SR)), max(1, int(release * SR))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] *= np.linspace(1, 0, r) ** curve
    return e


def norm(x, gain=0.8):
    return (x / (np.max(np.abs(x)) + 1e-9) * gain).astype(np.float32)


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    sf.write(os.path.join(OUT, name + ".ogg"), x, SR, format="OGG", subtype="VORBIS")
    print("wrote", name, f"{len(x) / SR:.2f}s")


def saw(f):
    ph = np.cumsum(f) / SR
    return 2 * (ph - np.floor(ph + 0.5))


def put(x, y, at):
    i = int(at * SR)
    n = min(len(y), len(x) - i)
    x[i:i + n] += y[:n]


# --- the waterhoentje -------------------------------------------------------------------------------------------------------
def krr(pitch, dur, seed):
    """A purring rattle: a buzzy tone, amplitude-gated ~38 times a second, through a soft nasal formant."""
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = pitch * (1 + 0.04 * np.sin(2 * np.pi * 5 * t))
    x = band(saw(f), 900, 2400) + 0.3 * low(saw(f), 700)
    gate = 0.5 + 0.5 * np.sign(np.sin(2 * np.pi * (36 + r.uniform(-3, 3)) * t))
    x *= low(gate, 300) * env(n, 0.01, 0.05, 1.5)
    return x


def uk(pitch, dur):
    """A bright little "uk": the pitch jumps up and drops, short."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = pitch * (1.25 - 0.35 * (t / dur))
    x = band(saw(f), 1000, 3200) + 0.4 * np.sin(2 * np.pi * np.cumsum(f) / SR)
    return x * env(n, 0.004, dur * 0.7, 2.2)


def waterhoen_roep(pitch, seed):
    x = np.zeros(int(0.5 * SR))
    put(x, krr(pitch, 0.2, seed), 0.0)
    put(x, uk(pitch * 1.6, 0.11), 0.24)
    if seed % 2:
        put(x, uk(pitch * 1.7, 0.09) * 0.7, 0.37)
    return norm(x, 0.7)


def piep(pitch, dur, seed, zacht=False):
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = pitch * (1 + 0.5 * np.sin(np.pi * t / dur)) * (1 + 0.02 * np.sin(2 * np.pi * 30 * t))
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.3 * np.sin(4 * np.pi * np.cumsum(f) / SR)
    if zacht:
        x = low(x, 2400) + 0.05 * low(r.normal(0, 1, n), 1500)
    return norm(x * env(n, 0.006, dur * 0.5, 1.8), 0.6)


# --- the alpine hoentje -----------------------------------------------------------------------------------------------------
def kak(pitch, dur):
    """One croaky little pulse: a nasal buzz with a quick pitch drop."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = pitch * (1.1 - 0.2 * (t / dur))
    x = band(saw(f), 500, 1500) * 1.0 + band(saw(f), 1800, 2800) * 0.4 + 0.3 * low(saw(f), 500)
    return x * env(n, 0.005, dur * 0.6, 1.6)


def alpinehoen_roep(pitch, seed):
    r = np.random.default_rng(seed)
    x = np.zeros(int(0.85 * SR))
    put(x, kak(pitch * 0.9, 0.13), 0.0)                    # "arr"
    put(x, kak(pitch * 0.92, 0.12), 0.17)                  # "arr"
    at, gap = 0.36, 0.085
    for i in range(3 + seed % 3):                          # "ka-ka-ka(-ka)", faster and faster
        put(x, kak(pitch * (1.05 + 0.03 * i) * r.uniform(0.98, 1.02), 0.05), at)
        at += gap
        gap *= 0.86
    return norm(x, 0.7)


# --- the NOWAY jingles ------------------------------------------------------------------------------------------------------
def bel(freq, dur, gain=1.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = np.sin(2 * np.pi * freq * t) + 0.35 * np.sin(2 * np.pi * freq * 2.01 * t) + 0.12 * np.sin(2 * np.pi * freq * 3.02 * t)
    return x * np.exp(-t * 7) * env(n, 0.003, 0.05) * gain


def noway(extreem=False):
    x = np.zeros(int((1.2 if extreem else 0.9) * SR))
    notes = [1046.5, 1318.5, 1568.0, 2093.0] if not extreem else [1174.7, 1480.0, 1760.0, 2349.3, 2960.0]
    step = 0.075 if not extreem else 0.055
    for i, f in enumerate(notes):
        put(x, bel(f, 0.45, 0.8 if i < len(notes) - 1 else 1.0), i * step)
    if extreem:
        n = int(0.25 * SR)                                 # a little "whoop" slide up
        t = np.arange(n) / SR
        f = 600 * (1 + 2.2 * (t / 0.25) ** 1.5)
        put(x, 0.35 * np.sin(2 * np.pi * np.cumsum(f) / SR) * env(n, 0.02, 0.08), 0.0)
        for i, f in enumerate((3520.0, 4186.0, 3520.0, 4186.0)):   # sparkles on top
            put(x, bel(f, 0.2, 0.35), 0.35 + i * 0.06)
    return norm(x, 0.65)


def main():
    for i, (p, s) in enumerate(((520, 1), (560, 2), (490, 3)), 1):
        save(f"waterhoen_roep{i}", waterhoen_roep(p, s))
    save("waterhoen_piep1", piep(1500, 0.14, 11))
    save("waterhoen_piep2", piep(1650, 0.12, 12))
    for i, (p, s) in enumerate(((420, 21), (450, 22), (400, 23)), 1):
        save(f"alpinehoen_roep{i}", alpinehoen_roep(p, s))
    save("alpinehoen_piep1", piep(1250, 0.16, 31, zacht=True))
    save("alpinehoen_piep2", piep(1350, 0.13, 32, zacht=True))
    save("noway", noway())
    save("noway_extreem", noway(True))


if __name__ == "__main__":
    main()
