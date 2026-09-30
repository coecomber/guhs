"""
Baltoguh en Nomguh (3.0 Guhverhalen, slice balto) - the sounds, synthesized (mono OGG, 44.1 kHz) into
assets/guhs/sounds/balto/:

  huil1-2      Baltoguh's howl: a wolf's rising, singing "auuuhoe" with a guh's squeaky end (made of the mod's own guh
               voice, pitched down and stretched, over a soft sung tone with vibrato)
  gak1-3       Boris the goose: a short nasal honk (a buzzy tone through a "nose" band filter, a little drop in pitch)
  belletjes    sled bells: a bright jingle of tiny bells in a trotting rhythm
  hatsjoe1-2   a baby guh's sneeze: a tiny in-breath "ha-ha-" and a soft "tsjoe!" with a squeak (nothing hurts!)
  snuffel      sniff-sniff-sniff: three short snuffles through a nose
  wind1-2      a gust of the snow storm: a swelling, whistling rush of wind

Not part of make_resources.py (the OGGs are committed, like the other sounds). Needs numpy, scipy and soundfile:
    python tools/features/balto_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter, resample

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "balto")
GUH = os.path.join("src", "main", "resources", "assets", "guhs", "sounds")
rng = np.random.default_rng(20300701)


def band(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def low(x, hi, order=2):
    b, a = butter(order, hi / (SR / 2), btype="low")
    return lfilter(b, a, x)


def high(x, lo, order=2):
    b, a = butter(order, lo / (SR / 2), btype="high")
    return lfilter(b, a, x)


def norm(x, peak=0.8):
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak


def fade(x, a=0.01, b=0.08):
    n = len(x)
    t = np.arange(n) / SR
    e = np.minimum(1, t / a) * np.minimum(1, np.maximum(0, (n / SR) - t) / b)
    return x * e


def save(name, x, peak=0.8):
    os.makedirs(OUT, exist_ok=True)
    sf.write(os.path.join(OUT, name + ".ogg"), norm(fade(x), peak).astype(np.float32), SR, format="OGG", subtype="VORBIS")
    print("wrote", name, f"{len(x) / SR:.2f}s")


def toon(f, dur, harm=(1.0, 0.5, 0.25, 0.12), vib=0.0, vib_hz=5.5):
    """A sung tone following the frequency curve f(t) (Hz, an array or a function of t)."""
    n = int(SR * dur)
    t = np.arange(n) / SR
    freq = f(t) if callable(f) else np.full(n, f)
    freq = freq * (1 + vib * np.sin(2 * np.pi * vib_hz * t))
    ph = 2 * np.pi * np.cumsum(freq) / SR
    return sum(a * np.sin(ph * (k + 1)) for k, a in enumerate(harm))


def guh_stem(path, pitch):
    x, sr = sf.read(path)
    if x.ndim > 1:
        x = x.mean(axis=1)
    if sr != SR:
        x = resample(x, int(len(x) * SR / sr))
    return resample(x, int(len(x) / pitch))


# =====================================================================================================================
def huil(variant):
    dur = 2.6 if variant == 1 else 2.2
    top, eind = (720, 560) if variant == 1 else (780, 620)

    def f(t):
        # a slow rise to the top, a long sung middle, a soft fall at the end
        return np.interp(t, [0, 0.35, 0.9, dur - 0.5, dur], [360, top, top * 0.97, eind, eind * 0.85])
    x = toon(f, dur, harm=(1.0, 0.45, 0.22, 0.1, 0.05), vib=0.018, vib_hz=5.2)
    x = band(x, 250, 3200)
    breath = band(rng.normal(0, 1, len(x)), 600, 2400) * 0.06
    t = np.arange(len(x)) / SR
    env = np.minimum(1, t / 0.25) * np.clip((dur - t) / 0.6, 0, 1)
    x = (x + breath) * env
    # the guh's own squeak at the end: njeg!
    stem = guh_stem(os.path.join(GUH, f"guh_ambient{3 if variant == 1 else 7}.ogg"), 1.25)[: int(SR * 0.45)]
    y = np.concatenate([x, np.zeros(int(SR * 0.05))])
    start = len(y) - int(SR * 0.35)
    out = np.zeros(start + len(stem))
    out[:len(y)] += y
    out[start:start + len(stem)] += norm(stem, 0.45)
    return out


def gak(variant):
    dur = [0.26, 0.2, 0.32][variant]
    f0 = [420, 460, 390][variant]
    x = toon(lambda t: f0 * (1.05 - 0.12 * t / dur), dur, harm=(1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.35, 0.3))
    x = band(x, 700, 2600, 3) + band(x, 2800, 4200, 2) * 0.3
    t = np.arange(len(x)) / SR
    env = np.minimum(1, t / 0.015) * np.exp(-np.maximum(0, t - dur * 0.6) / 0.05)
    return x * env


def belletjes():
    n = int(SR * 1.8)
    out = np.zeros(n)
    t0 = 0.0
    while t0 < 1.55:
        for _ in range(rng.integers(3, 6)):
            f = rng.uniform(2600, 4800)
            d = int(SR * rng.uniform(0.18, 0.4))
            s = int(SR * (t0 + rng.uniform(0, 0.03)))
            tt = np.arange(d) / SR
            ping = (np.sin(2 * np.pi * f * tt) + 0.5 * np.sin(2 * np.pi * f * 2.76 * tt) + 0.25 * np.sin(2 * np.pi * f * 5.4 * tt))
            ping *= np.exp(-tt / rng.uniform(0.05, 0.12)) * rng.uniform(0.4, 1.0)
            e = min(n, s + d)
            out[s:e] += ping[: e - s]
        t0 += 0.19 if int(t0 / 0.19) % 2 == 0 else 0.13    # a trotting rhythm
    return high(out, 1500)


def hatsjoe(variant):
    # "ha-ha-": two tiny breathy in-breaths with a high little voice
    parts = []
    for i, f in enumerate((880, 990) if variant == 1 else (940,)):
        d = 0.16
        v = toon(lambda t: f * (1 + 0.15 * t / d), d, harm=(1.0, 0.3, 0.1)) * 0.35
        b = band(rng.normal(0, 1, len(v)), 1500, 5000) * 0.5
        t = np.arange(len(v)) / SR
        parts.append((v + b) * np.sin(np.pi * t / d) ** 2)
        parts.append(np.zeros(int(SR * 0.06)))
    # "tsjoe!": a burst of soft noise and a squeaky "choo"
    d = 0.3
    n = int(SR * d)
    t = np.arange(n) / SR
    burst = band(rng.normal(0, 1, n), 2500, 8000) * np.exp(-t / 0.05)
    choo = toon(lambda tt: 1150 * (1 - 0.35 * tt / d), d, harm=(1.0, 0.4, 0.15)) * np.exp(-t / 0.12) * 0.6
    parts.append(burst + choo)
    return np.concatenate(parts)


def snuffel():
    parts = []
    for i in range(3):
        d = 0.09 + 0.02 * (i == 2)
        n = int(SR * d)
        t = np.arange(n) / SR
        x = band(rng.normal(0, 1, n), 900 + 200 * i, 4000) * np.sin(np.pi * t / d) ** 1.5
        parts.append(x)
        parts.append(np.zeros(int(SR * 0.07)))
    return np.concatenate(parts)


def wind(variant):
    dur = 3.4 if variant == 1 else 2.8
    n = int(SR * dur)
    t = np.arange(n) / SR
    x = rng.normal(0, 1, n)
    swell = np.sin(np.pi * t / dur) ** 1.6
    # a whistle that wanders, over a rushing band
    rush = band(x, 250, 1400) * 0.9 + band(x, 1400, 4000) * 0.25
    fw = 520 + 180 * np.sin(2 * np.pi * t / dur * (1.3 if variant == 1 else 0.9))
    fluit = np.sin(2 * np.pi * np.cumsum(fw) / SR) * 0.06 * swell
    return (rush * swell + fluit) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.8 * t))


def main():
    save("huil1", huil(1))
    save("huil2", huil(2))
    for i in range(3):
        save(f"gak{i + 1}", gak(i))
    save("belletjes", belletjes())
    save("hatsjoe1", hatsjoe(1), 0.6)
    save("hatsjoe2", hatsjoe(2), 0.6)
    save("snuffel", snuffel(), 0.6)
    save("wind1", wind(1), 0.7)
    save("wind2", wind(2), 0.7)


if __name__ == "__main__":
    main()
