"""
biomes3 slice "bouw-wolk2" - the sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/bio_bouw_wolk2/:

  snurk1-3    the giant snoring: a long soft rumbling breath in, a whistling "pfff" out
  grom1-2     the giant stirring: a low sleepy "...njeg?" (two vowels sliding up, breathy)
  nies1-2     the giant sneezing: a breath drawn in, a soft burst of air (a gust, not a bang)
  rommel1-3   the forge cloud: a far, soft roll of thunder
  hamer1-2    the smid-guh's hammer: two small bright taps on an anvil

All soft: nothing here is loud. Not part of make_resources.py (the OGGs are committed like the other sounds;
bio_bouw_wolk2.py only writes sounds.json).
Needs numpy, scipy and soundfile:    python tools/features/bio_bouw_wolk2_geluid.py        (from the project root)
"""
import os

import numpy as np
try:                                    # (only needed to MAKE the sounds; the wiki build runs without them)
    import soundfile as sf
    from scipy.signal import butter, lfilter
except ImportError:
    sf = butter = lfilter = None

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "bio_bouw_wolk2")


def band(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def low(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="low")
    return lfilter(b, a, x)


def env(n, attack, release, curve=2.0):
    e = np.ones(n)
    a, r = max(1, min(n, int(attack * SR))), max(1, min(n, int(release * SR)))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] *= np.linspace(1, 0, r) ** curve
    return e


def norm(x, gain=0.8):
    return (x / (np.max(np.abs(x)) + 1e-9) * gain).astype(np.float32)


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    sf.write(os.path.join(OUT, name + ".ogg"), x, SR, format="OGG", subtype="VORBIS")
    print("wrote", name, f"{len(x) / SR:.2f}s")


def snurk(seed, toon=58.0):
    r = np.random.default_rng(seed)
    n_in, n_uit = int(1.5 * SR), int(1.3 * SR)
    t = np.arange(n_in) / SR
    # in: a rattling low purr (a pulse train through a soft filter) with breath over it
    puls = (np.sin(2 * np.pi * (toon + 6 * np.sin(2 * np.pi * 0.7 * t)) * t) > 0.6).astype(float)
    adem_in = low(puls, 420) * 0.9 + 0.5 * band(r.normal(0, 1, n_in), 180, 900)
    adem_in *= env(n_in, 0.45, 0.35, 1.5)
    # out: a long whistling puff that falls in pitch
    t2 = np.arange(n_uit) / SR
    fluit = np.sin(2 * np.pi * np.cumsum(900 - 420 * t2 / 1.3) / SR) * 0.12
    adem_uit = (band(r.normal(0, 1, n_uit), 600, 2600) * 0.5 + fluit) * env(n_uit, 0.2, 0.9, 2.0)
    stil = np.zeros(int(0.18 * SR))
    return norm(np.concatenate([adem_in, stil, adem_uit]), 0.6)


def klinker(n, f0, f1, formant, seed):
    """A breathy sung vowel sliding from f0 to f1 with one formant."""
    r = np.random.default_rng(seed)
    t = np.arange(n) / SR
    f = f0 + (f1 - f0) * (t / t[-1]) ** 1.4
    fase = 2 * np.pi * np.cumsum(f) / SR
    x = sum(np.sin(k * fase) / k for k in range(1, 9))
    x = band(x, formant * 0.6, formant * 1.5) + 0.25 * low(x, 300)
    return x + 0.12 * band(r.normal(0, 1, n), 300, 1800)


def grom(seed, toon=84.0):
    n1, n2 = int(0.5 * SR), int(0.7 * SR)
    a = klinker(n1, toon * 0.9, toon, 480, seed) * env(n1, 0.12, 0.2)           # "...nje"
    b = klinker(n2, toon, toon * 1.5, 760, seed + 1) * env(n2, 0.08, 0.45, 1.6)  # "...eg?" going up
    r = np.random.default_rng(seed + 2)
    voor = band(r.normal(0, 1, int(0.35 * SR)), 150, 700) * env(int(0.35 * SR), 0.2, 0.15) * 0.4   # a breath first
    return norm(np.concatenate([voor, a, b]), 0.62)


def nies(seed):
    r = np.random.default_rng(seed)
    n1, n2 = int(0.9 * SR), int(1.1 * SR)
    t = np.arange(n1) / SR
    # "ha... ha...": three little breaths in, each higher
    hap = np.zeros(n1)
    for i, start in enumerate((0.0, 0.3, 0.58)):
        m = int(0.24 * SR)
        s0 = int(start * SR)
        stuk = klinker(m, 150 + 40 * i, 190 + 50 * i, 900, seed + i) * env(m, 0.05, 0.15)
        hap[s0:s0 + m] += stuk[:max(0, min(m, n1 - s0))] * (0.5 + 0.2 * i)
    # "tsjoe": a burst of air, bright at first and then a long soft gust
    ruis = r.normal(0, 1, n2)
    t2 = np.arange(n2) / SR
    stoot = band(ruis, 1800, 7000) * np.exp(-t2 * 16) * 1.2 + band(ruis, 250, 1500) * env(n2, 0.02, 0.95, 1.8) * 0.8
    return norm(np.concatenate([hap, stoot]), 0.72)


def rommel(seed, lengte=3.2):
    r = np.random.default_rng(seed)
    n = int(lengte * SR)
    t = np.arange(n) / SR
    ruis = low(r.normal(0, 1, n), 110, 3)
    golf = 0.55 + 0.45 * np.sin(2 * np.pi * (0.9 + 0.5 * r.random()) * t + r.random() * 6) * np.sin(2 * np.pi * 2.3 * t + r.random() * 6)
    x = ruis * golf * env(n, 0.5, lengte * 0.6, 1.4)
    x += 0.3 * low(r.normal(0, 1, n), 60, 3) * env(n, 0.9, lengte * 0.5)
    return norm(x, 0.5)


def hamer(seed):
    r = np.random.default_rng(seed)
    uit = np.zeros(int(0.62 * SR))
    for start, sterk in ((0.0, 1.0), (0.26, 0.7)):
        n = int(0.34 * SR)
        t = np.arange(n) / SR
        x = sum(a * np.sin(2 * np.pi * f * (1 + 0.004 * r.random()) * t) * np.exp(-t * d)
                for f, a, d in ((1870, 1.0, 16), (2810, 0.6, 22), (4150, 0.35, 30), (620, 0.3, 40)))
        x += 0.4 * band(r.normal(0, 1, n), 2000, 8000) * np.exp(-t * 90)
        s0 = int(start * SR)
        uit[s0:s0 + n] += x[:len(uit) - s0] * sterk
    return norm(uit, 0.5)


if __name__ == "__main__":
    for i in range(3):
        save(f"snurk{i + 1}", snurk(8901 + i, 54.0 + 5 * i))
        save(f"rommel{i + 1}", rommel(8931 + i, 2.8 + 0.5 * i))
    for i in range(2):
        save(f"grom{i + 1}", grom(8911 + i * 5, 80.0 + 10 * i))
        save(f"nies{i + 1}", nies(8921 + i * 5))
        save(f"hamer{i + 1}", hamer(8941 + i))
