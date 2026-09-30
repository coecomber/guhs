"""
3.0 (Guhverhalen), slice waterdiertjes - the sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/waterdiertjes/:

  blub1-3     the guhxolotl: a soft bubbly "blub" with a tiny squeak on top
  kwak1-3     mama guh-eendje: a round, nasal little quack ("kwak-njeg")
  piep1-3     the kuikentjes: a high, bright peep that slides up
  ting1-2     the glimguhtje: a soft glassy chime
  zoem        the lieveheersbeestje taking off: a tiny purring buzz

Not part of make_resources.py (the OGGs are committed like the other sounds; waterdiertjes.py only writes sounds.json).
Needs numpy, scipy and soundfile:    python tools/features/waterdiertjes_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "waterdiertjes")
FILES = ["blub1", "blub2", "blub3", "kwak1", "kwak2", "kwak3", "piep1", "piep2", "piep3", "ting1", "ting2", "zoem"]


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


def blub(pitch, seed):
    """A bubble: a sine whose pitch rises quickly as it pops (bubbles do that), plus a tiny squeak."""
    r = np.random.default_rng(seed)
    dur = 0.32
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = pitch * (1 + 1.8 * (t / dur) ** 1.5)
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = np.sin(ph) * env(n, 0.005, 0.22, 2.5)
    sq_n = int(0.09 * SR)
    st = np.arange(sq_n) / SR
    sq = np.sin(2 * np.pi * np.cumsum(2400 + 900 * st / 0.09 + 60 * np.sin(2 * np.pi * 30 * st)) / SR) * env(sq_n, 0.01, 0.05)
    start = int(0.14 * SR)
    x[start:start + sq_n] += 0.35 * sq
    x += 0.04 * band(r.normal(0, 1, n), 300, 1500) * env(n, 0.002, 0.2)
    return norm(x, 0.7)


def kwak(pitch, seed, dur=0.28):
    """A duck's quack: a buzzy sawtooth through two nasal formants, a quick pitch drop at the end."""
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = pitch * (1.08 - 0.25 * (t / dur) ** 2) * (1 + 0.01 * np.sin(2 * np.pi * 7 * t))
    ph = np.cumsum(f) / SR
    saw = 2 * (ph - np.floor(ph + 0.5))
    x = band(saw, 700, 1300) * 1.0 + band(saw, 2000, 3000) * 0.55 + 0.25 * low(saw, 600)
    x += 0.05 * band(r.normal(0, 1, n), 1500, 4000)
    x *= env(n, 0.012, 0.1, 1.6)
    return norm(x, 0.75)


def piep(pitch, seed):
    """A duckling's peep: a pure, bright tone sliding up and then a little down."""
    dur = 0.16
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = pitch * (1 + 0.35 * np.sin(np.pi * t / dur))
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.25 * np.sin(4 * np.pi * np.cumsum(f) / SR)
    x *= env(n, 0.01, 0.07, 1.5)
    return norm(x, 0.6)


def ting(base, seed):
    """A soft glassy chime: a few inharmonic partials, each decaying at its own speed."""
    dur = 1.1
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for mul, amp, dec in ((1.0, 1.0, 2.6), (2.76, 0.35, 4.5), (5.4, 0.18, 7.0), (8.9, 0.08, 10.0)):
        x += amp * np.sin(2 * np.pi * base * mul * t) * np.exp(-dec * t)
    x *= env(n, 0.004, 0.2)
    return norm(x, 0.45)


def zoem(seed):
    """The tiny buzz of a ladybird's wings as it takes off."""
    r = np.random.default_rng(seed)
    dur = 0.5
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = 190 + 40 * t / dur
    ph = np.cumsum(f) / SR
    x = np.sign(np.sin(2 * np.pi * ph)) * 0.6 + 0.2 * r.normal(0, 1, n)
    x = band(x, 200, 2200)
    x *= (0.6 + 0.4 * np.sin(2 * np.pi * 23 * t)) * env(n, 0.05, 0.25)
    return norm(x, 0.35)


def main():
    save("blub1", blub(520, 1))
    save("blub2", blub(610, 2))
    save("blub3", blub(460, 3))
    save("kwak1", kwak(330, 11))
    save("kwak2", kwak(300, 12, 0.24))
    save("kwak3", kwak(360, 13, 0.3))
    save("piep1", piep(3100, 21))
    save("piep2", piep(3400, 22))
    save("piep3", piep(2900, 23))
    save("ting1", ting(1320, 31))
    save("ting2", ting(1568, 32))
    save("zoem", zoem(41))


if __name__ == "__main__":
    main()
