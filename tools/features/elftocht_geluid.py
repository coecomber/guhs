"""
De Elf-Guhjestocht (2.9) - the sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/elftocht/:

  glij          the hiss of skate blades on ice (a seamless 2 s loop; the game follows your speed with volume and pitch)
  kras1-3       one stride: the blade bites into the ice (a short bright scrape)
  plof1-2       the Stempelguh's stamp: PLOF (a soft thud with a puff)
  fluit         Schaatsmeester Guhglij's start whistle: a trilled TUUUT
  juich1-3      the audience: a little crowd of guh voices cheering (made of the mod's own guh sounds, pitched up and mixed,
                with clapping)
  finish        a small fanfare (bells and a brass-ish arpeggio) for the finish

Not part of make_resources.py (the OGGs are committed like the other sounds). Needs numpy, scipy and soundfile:
    python tools/features/elftocht_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter, resample

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "elftocht")
GUH = os.path.join("src", "main", "resources", "assets", "guhs", "sounds")
rng = np.random.default_rng(29060)


def band(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def low(x, hi, order=2):
    b, a = butter(order, hi / (SR / 2), btype="low")
    return lfilter(b, a, x)


def env(n, attack, decay):
    t = np.arange(n) / SR
    e = np.minimum(1.0, t / max(attack, 1e-4)) * np.exp(-np.maximum(0, t - attack) / max(decay, 1e-4))
    return e


def norm(x, peak=0.8):
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    sf.write(os.path.join(OUT, name + ".ogg"), norm(x).astype(np.float32), SR, format="OGG", subtype="VORBIS")
    print("wrote", name, f"{len(x) / SR:.2f}s")


def glij():
    n = int(SR * 2.0)
    x = rng.normal(0, 1, n)
    hiss = band(x, 2500, 7500, 2) * 0.8 + band(x, 900, 2400, 2) * 0.35
    t = np.arange(n) / SR
    wobble = 0.8 + 0.2 * np.sin(2 * np.pi * 1.0 * t) + 0.08 * np.sin(2 * np.pi * 3.0 * t)
    y = hiss * wobble
    # seamless: crossfade the end into the start
    f = int(SR * 0.25)
    ramp = np.linspace(0, 1, f)
    y[:f] = y[:f] * ramp + y[-f:] * (1 - ramp)
    return y[:-f]


def kras(seed):
    r = np.random.default_rng(seed)
    n = int(SR * 0.32)
    x = r.normal(0, 1, n)
    y = band(x, 1500 + seed % 3 * 300, 6500, 2) * env(n, 0.01, 0.07)
    y += band(r.normal(0, 1, n), 5000, 12000, 2) * env(n, 0.002, 0.02) * 0.6   # the click of the blade
    return y


def plof(seed):
    r = np.random.default_rng(seed)
    n = int(SR * 0.45)
    t = np.arange(n) / SR
    f = 110 * np.exp(-t * 6) + 55
    thud = np.sin(2 * np.pi * np.cumsum(f) / SR) * env(n, 0.004, 0.09)
    puff = low(r.normal(0, 1, n), 1200) * env(n, 0.01, 0.12) * 0.7
    return thud + puff


def fluit():
    n = int(SR * 1.1)
    t = np.arange(n) / SR
    trill = 1 + 0.03 * np.sign(np.sin(2 * np.pi * 28 * t))
    f = 2750 * trill
    y = np.sin(2 * np.pi * np.cumsum(f) / SR)
    y += 0.3 * np.sin(2 * np.pi * np.cumsum(2 * f) / SR)
    y += band(rng.normal(0, 1, n), 2000, 5000) * 0.25             # breath
    e = np.minimum(1, t / 0.03) * np.minimum(1, np.maximum(0, 1.1 - t) / 0.12)
    return y * e


def guh_stem(path, pitch):
    x, sr = sf.read(path)
    if x.ndim > 1:
        x = x.mean(axis=1)
    if sr != SR:
        x = resample(x, int(len(x) * SR / sr))
    return resample(x, int(len(x) / pitch))


def juich(seed):
    r = np.random.default_rng(seed)
    n = int(SR * 1.6)
    y = np.zeros(n)
    files = [os.path.join(GUH, f"guh_ambient{i}.ogg") for i in range(1, 17)]
    files = [f for f in files if os.path.exists(f)]
    for k in range(7):
        if not files:
            break
        v = guh_stem(files[r.integers(len(files))], 1.15 + r.random() * 0.45)
        start = int(r.random() * 0.4 * SR)
        m = min(len(v), n - start)
        y[start:start + m] += v[:m] * (0.5 + r.random() * 0.5)
    # clapping: little noise bursts
    for _ in range(26):
        at = int(r.random() * (n - SR * 0.05))
        c = int(SR * 0.04)
        y[at:at + c] += band(r.normal(0, 1, c), 800, 4000) * env(c, 0.001, 0.012) * 0.5
    y *= np.minimum(1, np.maximum(0, (n - np.arange(n)) / (SR * 0.3)))
    return y


def finish():
    n = int(SR * 2.0)
    t = np.arange(n) / SR
    y = np.zeros(n)
    notes = [(0.0, 523.25), (0.14, 659.25), (0.28, 783.99), (0.42, 1046.5), (0.7, 783.99), (0.84, 1046.5)]
    for (at, f) in notes:
        s = int(at * SR)
        m = n - s
        tt = np.arange(m) / SR
        tone = np.sign(np.sin(2 * np.pi * f * tt)) * 0.25 + np.sin(2 * np.pi * f * tt) * 0.6
        tone = low(tone, 3500)
        y[s:] += tone * env(m, 0.01, 0.35 if at < 0.7 else 0.9)
    # bells
    for f in (1567.98, 2093.0):
        y += np.sin(2 * np.pi * f * t) * env(n, 0.002, 0.8) * 0.2
    return y


def main():
    save("glij", glij())
    for i in range(1, 4):
        save(f"kras{i}", kras(100 + i))
    for i in range(1, 3):
        save(f"plof{i}", plof(200 + i))
    save("fluit", fluit())
    for i in range(1, 4):
        save(f"juich{i}", juich(300 + i))
    save("finish", finish())


if __name__ == "__main__":
    main()
