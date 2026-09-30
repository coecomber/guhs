"""
3.0 (Guhverhalen), balto_slee - the sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/baltoslee/:

  glijden1-2   the runners hissing over the snow (a soft, crunchy "shhh")
  bellen1-3    sled bells: a little tingeling
  woef1-3      a guh-sledehondje: a small, happy "woef-njeg"
  windvlaag1-2 a gust of wind: a swelling whoosh
  lawine       an avalanche far up the slope: a deep, rolling rumble
  plof1-2      a soft plof into the snow
  ijs1-2       the ice bridge creaking and ticking
  vuurkorf     a vuurkorf crackling
  fanfare      tingeling-VAHOEG: bells and three happy notes

Not part of make_resources.py (the OGGs are committed like the other sounds). Needs numpy, scipy and soundfile:
    python tools/features/balto_slee_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "baltoslee")
rng = np.random.default_rng(20300801)


def band(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def low(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="low")
    return lfilter(b, a, x)


def high(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="high")
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


def t_(dur):
    return np.arange(int(dur * SR)) / SR


def glijden(dur, seed):
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    ruis = band(r.normal(0, 1, n), 1800, 7000)
    korrel = (r.random(n) < 0.004) * r.normal(0, 3, n)           # the crunch of the snow
    korrel = band(korrel, 2500, 9000)
    golf = 0.75 + 0.25 * np.sin(2 * np.pi * 3.1 * t_(dur))
    return norm((ruis * golf + korrel * 0.6) * env(n, 0.15, 0.35), 0.5)


def bel(freq, dur=0.5, r=rng):
    t = t_(dur)
    partialen = [(1.0, 1.0), (2.76, 0.5), (5.4, 0.25), (8.9, 0.12)]
    x = sum(a * np.sin(2 * np.pi * freq * f * t + r.random() * 6) for f, a in partialen)
    return x * np.exp(-t * 9) * env(len(t), 0.002, 0.05)


def bellen(dur, seed):
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    x = np.zeros(n)
    for _ in range(14):
        start = int(r.random() * (dur - 0.45) * SR)
        b = bel(2300 + r.random() * 1400, 0.45, r) * (0.4 + r.random() * 0.6)
        x[start:start + len(b)] += b[:n - start]
    ruis = high(r.normal(0, 0.05, n), 6000) * np.exp(-t_(dur) * 3)  # the rattle of the little balls inside
    return norm(x + ruis, 0.55)


def woef(pitch, seed, njeg=True):
    r = np.random.default_rng(seed)

    def blaf(f0, dur, klinker):
        t = t_(dur)
        f = f0 * (1 + 0.35 * np.sin(np.pi * t / dur))               # up and down: cute
        fase = 2 * np.pi * np.cumsum(f) / SR
        bron = sum(np.sin(k * fase) / k for k in range(1, 12))       # a soft sawtooth
        uit = sum(band(bron, fc - bw, fc + bw) * g for fc, bw, g in klinker)
        return uit * env(len(t), 0.01, dur * 0.5, 1.5)

    a = blaf(pitch, 0.13, [(700, 200, 1.0), (1300, 250, 0.6), (2600, 400, 0.25)])            # "woef"
    x = [a, np.zeros(int(0.05 * SR))]
    if njeg:
        x.append(blaf(pitch * 1.25, 0.16, [(500, 150, 0.8), (2100, 300, 0.9), (3000, 400, 0.3)]))  # "njeg"
    y = np.concatenate(x)
    y += band(r.normal(0, 0.03, len(y)), 2000, 6000)                 # a little breath
    return norm(y, 0.7)


def windvlaag(dur, seed):
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = t_(dur)
    ruis = r.normal(0, 1, n)
    x = np.zeros(n)
    blok = 2048
    for i in range(0, n, blok):                                      # a band that sweeps up and back down
        f = 350 + 1400 * np.sin(np.pi * min(1, i / n)) ** 1.5
        seg = band(ruis[max(0, i - 4096):i + blok], f * 0.6, f * 1.6)
        x[i:i + blok] = seg[-min(blok, n - i):]
    zwel = np.sin(np.pi * t / dur) ** 2
    return norm(x * zwel, 0.6)


def lawine(dur=3.2):
    n = int(dur * SR)
    t = t_(dur)
    diep = low(rng.normal(0, 1, n), 140, 3)
    rommel = low(rng.normal(0, 1, n), 420) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t) ** 2)
    knars = band((rng.random(n) < 0.002) * rng.normal(0, 2, n), 400, 2500)
    x = diep * 1.6 + rommel * 0.6 + knars * 0.4
    return norm(x * env(n, 0.6, 1.2, 1.5), 0.85)


def plof(dur, seed, diepte):
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = t_(dur)
    dof = low(r.normal(0, 1, n), 900) * np.exp(-t * 18)
    bonk = np.sin(2 * np.pi * diepte * t * (1 - 0.4 * t)) * np.exp(-t * 14)
    kruim = band(r.normal(0, 1, n), 2000, 6000) * np.exp(-t * 30) * 0.3
    return norm(dof + bonk * 0.9 + kruim, 0.75)


def ijs(dur, seed):
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    x = np.zeros(n)
    for _ in range(5):                                               # little ticks and chirps
        s = int(r.random() * (dur - 0.15) * SR)
        f = 1800 + r.random() * 2600
        tt = t_(0.12)
        c = np.sin(2 * np.pi * f * tt * (1 + 2 * tt)) * np.exp(-tt * 40)
        x[s:s + len(c)] += c * (0.4 + r.random() * 0.6)
    kraak = band(r.normal(0, 1, n), 300, 1200) * (np.abs(np.sin(2 * np.pi * 13 * t_(dur))) ** 8) * 0.8
    return norm((x + kraak) * env(n, 0.02, 0.2), 0.6)


def vuurkorf(dur=2.4):
    n = int(dur * SR)
    x = low(rng.normal(0, 0.25, n), 500)                             # the soft roar
    for _ in range(38):
        s = int(rng.random() * (dur - 0.05) * SR)
        c = high(rng.normal(0, 1, int(0.012 * SR)), 1500) * np.exp(-np.arange(int(0.012 * SR)) / SR * 300)
        x[s:s + len(c)] += c * (0.3 + rng.random())
    return norm(x * env(n, 0.2, 0.4), 0.6)


def fanfare():
    parts = [bellen(0.5, 7)]
    noten = [(523.25, 0.14), (659.25, 0.14), (783.99, 0.34)]
    for f, d in noten:
        t = t_(d)
        toon = np.sign(np.sin(2 * np.pi * f * t)) * 0.3 + np.sin(2 * np.pi * f * t) * 0.7
        toon = low(toon, 3500) * env(len(t), 0.01, d * 0.6)
        parts.append(toon * 0.6)
    x = np.concatenate(parts)
    x[:len(bellen(0.9, 8))] += bellen(0.9, 8)[:len(x)] * 0.5
    return norm(x, 0.7)


def main():
    save("glijden1", glijden(1.3, 1))
    save("glijden2", glijden(1.1, 2))
    for i in range(3):
        save(f"bellen{i + 1}", bellen(0.9 + i * 0.1, 10 + i))
    save("woef1", woef(520, 21))
    save("woef2", woef(600, 22))
    save("woef3", woef(470, 23, njeg=False))
    save("windvlaag1", windvlaag(1.6, 31))
    save("windvlaag2", windvlaag(1.3, 32))
    save("lawine", lawine())
    save("plof1", plof(0.5, 41, 90))
    save("plof2", plof(0.45, 42, 120))
    save("ijs1", ijs(0.9, 51))
    save("ijs2", ijs(0.7, 52))
    save("vuurkorf", vuurkorf())
    save("fanfare", fanfare())


if __name__ == "__main__":
    main()
