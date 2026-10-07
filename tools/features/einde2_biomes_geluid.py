"""
einde2, slice biomes - the sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/einde2_biomes/:

  zing1-3   a knabbelkoorbloem singing one soft note: "oeh", "aah", "mmm" (the game picks the pitch per flower, so a
            forest of them sounds like a sleepy choir)
  flits     the flash of a knabbelkoorvrucht: floep, and you are somewhere else
  plof1-2   landing in a kruimelwolk: a soft pillow puff

Not part of make_resources.py (the OGGs are committed like the other sounds). Needs numpy, scipy and soundfile:
    python tools/features/einde2_biomes_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "einde2_biomes")
rng = np.random.default_rng(21400150)
NAMES = ["zing1", "zing2", "zing3", "flits", "plof1", "plof2"]


def band(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def low(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="low")
    return lfilter(b, a, x)


def env(n, attack, release, curve=2.0):
    e = np.ones(n)
    a, r = int(attack * SR), int(release * SR)
    e[:a] = np.linspace(0, 1, a) ** 1.5
    e[-r:] *= np.linspace(1, 0, r) ** curve
    return e


def norm(x, gain=0.8):
    return (x / (np.max(np.abs(x)) + 1e-9) * gain).astype(np.float32)


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    sf.write(os.path.join(OUT, name + ".ogg"), x, SR, format="OGG", subtype="VORBIS")
    print("wrote", name, f"{len(x) / SR:.2f}s")


def zing(formants, lengte, breath):
    """One sung note at 392 Hz (the game shifts it): a buzzy voice source through the formants of a vowel, with a slow
    vibrato that comes in late, and a little breath."""
    n = int(lengte * SR)
    t = np.arange(n) / SR
    vib = 1 + 0.012 * np.sin(2 * np.pi * 5.2 * t) * np.clip((t - 0.25) / 0.3, 0, 1)
    phase = 2 * np.pi * np.cumsum(392.0 * vib) / SR
    src = sum(np.sin(k * phase) / k ** 1.15 for k in range(1, 22))
    voice = sum(band(src, f * 0.85, f * 1.15) * g for f, g in formants)
    voice += low(src, 500) * 0.35
    air = band(rng.normal(0, 1, n), 2500, 6000) * breath
    return norm((voice + air * np.max(np.abs(voice))) * env(n, 0.09, lengte * 0.55), 0.6)


def flits():
    n = int(0.42 * SR)
    t = np.arange(n) / SR
    # floep: a quick dip and a rise that overshoots
    f = 520 * np.exp(-t * 9) + 240 + 1500 * np.clip((t - 0.07) / 0.2, 0, 1) ** 2
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.35 * np.sin(4 * np.pi * np.cumsum(f) / SR)
    sparkle = band(rng.normal(0, 1, n), 5000, 11000) * np.exp(-np.maximum(t - 0.2, 0) * 22) * (t > 0.18) * 0.5
    return norm(tone * env(n, 0.008, 0.2, 1.5) + sparkle, 0.7)


def plof(seed):
    r = np.random.default_rng(seed)
    n = int(0.36 * SR)
    t = np.arange(n) / SR
    puff = low(r.normal(0, 1, n), 900) * np.exp(-t * 16)
    bump = np.sin(2 * np.pi * np.cumsum(110 * np.exp(-t * 14) + 48) / SR) * np.exp(-t * 20) * 0.9
    hiss = band(r.normal(0, 1, n), 1500, 5000) * np.exp(-t * 9) * 0.12 * np.clip(t / 0.03, 0, 1)
    return norm(puff / (np.max(np.abs(puff)) + 1e-9) + bump + hiss, 0.7)


if __name__ == "__main__":
    save("zing1", zing([(320, 1.0), (800, 0.5), (2400, 0.12)], 1.1, 0.03))        # oeh
    save("zing2", zing([(760, 1.0), (1150, 0.7), (2700, 0.2)], 1.25, 0.04))       # aah
    save("zing3", zing([(280, 1.0), (1000, 0.15), (2200, 0.05)], 1.0, 0.02))      # mmm
    save("flits", flits())
    save("plof1", plof(21400151))
    save("plof2", plof(21400152))
