"""
De beroepen (2.9) - the sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/beroepen/:

  sirene      the fire truck's two-tone Dutch siren: TA-TUU, TA-TUU (a little guh-sized one)
  spuiten     the guh-brandslang spraying: a rushing jet of water
  sissen      a marshmallow fire going out: pssssst, with a last little pop
  hatsjoe1-2  a guhtje's sneeze: "ha... ha... HATSJOE!"
  hamer1-2    Bob's hammer on a dakpan: tik
  bubbel      the mengketel bubbling: blub blub blub

Not part of make_resources.py (the OGGs are committed like the other sounds). Needs numpy, scipy and soundfile:
    python tools/features/beroepen_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "beroepen")
rng = np.random.default_rng(29100)
NAMES = ["sirene", "spuiten", "sissen", "hatsjoe1", "hatsjoe2", "hamer1", "hamer2", "bubbel"]


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


def sirene():
    # the Dutch two-tone: a low and a high tone, each ~0.45 s, twice; a soft square-ish timbre, a slight glide
    parts = []
    for _ in range(2):
        for f0 in (440.0, 587.0):
            n = int(0.45 * SR)
            t = np.arange(n) / SR
            f = f0 * (1 + 0.012 * np.minimum(1, t * 20))
            ph = 2 * np.pi * np.cumsum(f) / SR
            tone = np.sin(ph) + 0.35 * np.sin(3 * ph) + 0.15 * np.sin(5 * ph)
            parts.append(tone * env(n, 0.02, 0.04, 1.0))
    return norm(np.concatenate(parts), 0.6)


def spuiten():
    n = int(0.7 * SR)
    noise = rng.standard_normal(n)
    x = band(noise, 900, 6000) * 0.8 + band(noise, 200, 900) * 0.4
    wobble = 1 + 0.15 * np.sin(2 * np.pi * 9 * np.arange(n) / SR)
    return norm(x * wobble * env(n, 0.05, 0.2, 1.2), 0.6)


def sissen():
    n = int(1.1 * SR)
    t = np.arange(n) / SR
    hiss = band(rng.standard_normal(n), 3000, 9000) * np.exp(-t * 2.2)
    pop_n = int(0.06 * SR)
    pop = np.zeros(n)
    start = int(0.85 * SR)
    pop[start:start + pop_n] = np.sin(2 * np.pi * 330 * np.arange(pop_n) / SR) * np.exp(-np.arange(pop_n) / SR * 60)
    return norm(hiss + pop * 0.6, 0.6)


def hatsjoe(pitch, seed):
    r = np.random.default_rng(seed)
    parts = []
    for i, dur in enumerate((0.22, 0.26)):                       # "ha... ha..." (breathing in, a little higher each time)
        n = int(dur * SR)
        t = np.arange(n) / SR
        f = pitch * (1.0 + 0.15 * i) * (1 + 0.3 * t / dur)
        voice = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.35 + band(r.standard_normal(n), 800, 3000) * 0.5
        parts.append(voice * env(n, 0.03, 0.06, 1.0))
        parts.append(np.zeros(int(0.12 * SR)))
    n = int(0.45 * SR)                                            # "TSJOE!": a burst of noise and a squeaky voice dropping
    t = np.arange(n) / SR
    burst = band(r.standard_normal(n), 1500, 8000) * np.exp(-t * 9)
    f = pitch * 1.6 * np.exp(-t * 2.5)
    voice = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 5) * 0.6
    parts.append((burst + voice) * env(n, 0.005, 0.1, 1.0))
    return norm(np.concatenate(parts), 0.75)


def hamer(pitch, seed):
    r = np.random.default_rng(seed)
    n = int(0.28 * SR)
    t = np.arange(n) / SR
    ring = sum(np.sin(2 * np.pi * pitch * k * t) * np.exp(-t * (30 + 8 * k)) / k for k in (1, 2.7, 5.1))
    thud = low(r.standard_normal(n), 400) * np.exp(-t * 40)
    return norm(ring * 0.6 + thud * 0.9, 0.7)


def bubbel():
    n = int(1.3 * SR)
    x = np.zeros(n)
    for _ in range(9):
        start = rng.integers(0, n - int(0.15 * SR))
        m = int(rng.uniform(0.06, 0.12) * SR)
        t = np.arange(m) / SR
        f = rng.uniform(180, 420) * (1 + 3 * t)
        blub = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 25)
        x[start:start + m] += blub
    return norm(low(x, 2000) * env(n, 0.05, 0.25), 0.6)


if __name__ == "__main__":
    save("sirene", sirene())
    save("spuiten", spuiten())
    save("sissen", sissen())
    save("hatsjoe1", hatsjoe(520, 1))
    save("hatsjoe2", hatsjoe(640, 2))
    save("hamer1", hamer(1150, 3))
    save("hamer2", hamer(980, 4))
    save("bubbel", bubbel())
