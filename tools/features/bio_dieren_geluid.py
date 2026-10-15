"""
biomes3 slice "dieren" - the sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/bio_dieren/:

  koi_hap1-3        a koi eating at the surface: a soft, round "plop" with a tiny bubble on top
  koi_strooi1-2     a handful of koivoer landing on the water: a sprinkle of very small plips
  schaapje_bleh1-3  the wolkenschaapje: a small, airy "meh-eh" (a soft wobble, breathy, high: a lamb made of cloud)
  schaapje_pluis1-2 its fluff coming off: a soft "pfff" puff

All soft and short: nothing here is loud. Not part of make_resources.py (the OGGs are committed like the other sounds;
bio_dieren.py only writes sounds.json).
Needs numpy, scipy and soundfile:    python tools/features/bio_dieren_geluid.py        (from the project root)
"""
import os

import numpy as np
try:                                    # (only needed to MAKE the sounds; the wiki build runs without them)
    import soundfile as sf
    from scipy.signal import butter, lfilter
except ImportError:
    sf = butter = lfilter = None

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "bio_dieren")
FILES = ["koi_hap1", "koi_hap2", "koi_hap3", "koi_strooi1", "koi_strooi2", "schaapje_bleh1", "schaapje_bleh2", "schaapje_bleh3",
         "schaapje_pluis1", "schaapje_pluis2"]


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


def plop(pitch, seed, dur=0.2):
    """A drop falling back into water: a sine that slides down fast and then up a little, very round."""
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = pitch * (1.0 - 0.55 * np.minimum(1, t / 0.05)) * (1 + 1.4 * np.maximum(0, t - 0.05) / dur)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * env(n, 0.004, dur * 0.75, 2.4)
    x += 0.05 * band(r.normal(0, 1, n), 400, 2400) * env(n, 0.002, dur * 0.6)
    return x


def hap(pitch, seed):
    """A koi's snap: one soft plop and a small, higher bubble right after."""
    a = plop(pitch, seed, 0.2)
    b = plop(pitch * 1.9, seed + 1, 0.1) * 0.4
    x = np.zeros(int(0.3 * SR))
    x[:len(a)] += a
    start = int(0.13 * SR)
    x[start:start + len(b)] += b
    return norm(low(x, 5000), 0.55)


def strooi(seed):
    """A sprinkle: a dozen tiny plips over a third of a second, each its own pitch."""
    r = np.random.default_rng(seed)
    n = int(0.5 * SR)
    x = np.zeros(n)
    for _ in range(14):
        p = plop(r.uniform(900, 1900), int(r.integers(0, 1 << 30)), r.uniform(0.04, 0.08)) * r.uniform(0.3, 1.0)
        start = int(r.uniform(0, 0.33) * SR)
        x[start:start + len(p)] += p[:n - start]
    return norm(low(x, 6000), 0.4)


def bleh(pitch, seed, dur=0.5):
    """A cloud lamb: a breathy saw through two soft formants, with the wobble of a bleat ("meh-eh-eh"), fading out."""
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    wobble = 1 + 0.045 * np.sin(2 * np.pi * 11 * t) * np.minimum(1, t / 0.12)
    f = pitch * (1.04 - 0.1 * t / dur) * wobble
    ph = np.cumsum(f) / SR
    saw = 2 * (ph - np.floor(ph + 0.5))
    x = band(saw, 800, 1500) * 1.0 + band(saw, 2300, 3300) * 0.35 + 0.2 * low(saw, 700)
    x *= 0.75 + 0.25 * np.sin(2 * np.pi * 11 * t + 1.0)        # the bleat's tremolo
    x += 0.12 * band(r.normal(0, 1, n), 1500, 5000) * env(n, 0.05, dur * 0.5)   # air
    x *= env(n, 0.03, dur * 0.45, 1.6)
    return norm(low(x, 6500), 0.5)


def pluis(seed, dur=0.45):
    """Pfff: soft filtered noise that opens and closes, a cloud letting go."""
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = band(r.normal(0, 1, n), 500, 3200) * (0.4 + 0.6 * np.sin(np.pi * t / dur) ** 2)
    x += 0.3 * np.sin(2 * np.pi * np.cumsum(520 - 260 * t / dur) / SR) * env(n, 0.01, dur * 0.7)
    x *= env(n, 0.03, dur * 0.6, 1.8)
    return norm(low(x, 4500), 0.4)


def main():
    save("koi_hap1", hap(620, 1))
    save("koi_hap2", hap(540, 3))
    save("koi_hap3", hap(700, 5))
    save("koi_strooi1", strooi(11))
    save("koi_strooi2", strooi(12))
    save("schaapje_bleh1", bleh(640, 21))
    save("schaapje_bleh2", bleh(700, 22, 0.42))
    save("schaapje_bleh3", bleh(590, 23, 0.56))
    save("schaapje_pluis1", pluis(31))
    save("schaapje_pluis2", pluis(32, 0.38))


if __name__ == "__main__":
    main()
