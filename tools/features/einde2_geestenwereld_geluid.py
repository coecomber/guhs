"""
einde2, slice geestenwereld - the sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/einde2_geestenwereld/:

  sfeer          the loop of the Geestenwereld (24 s, seamless): a soft pad on D and A that breathes, a far airy wind and,
                 now and then, a glassy chime
  gefluister1-3  a whisper in the leaves (breath through a few vowel formants), for the biome's additions
  meditatie      a clear temple bell: your soul goes back to your body
  tegel          a stone tile that turns a quarter (the lotus puzzle)

Not part of make_resources.py (the OGGs are committed like the other sounds; the wiki's CI has only numpy and Pillow).
Needs numpy, scipy and soundfile:
    python tools/features/einde2_geestenwereld_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "einde2_geestenwereld")
rng = np.random.default_rng(21500074)


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
    # (in pieces: libsndfile's vorbis writer can crash on one long buffer)
    with sf.SoundFile(os.path.join(OUT, name + ".ogg"), "w", SR, 1, format="OGG", subtype="VORBIS") as f:
        for i in range(0, len(x), 4096):
            f.write(x[i:i + 4096])
    print("wrote", name, f"{len(x) / SR:.2f}s")


def sfeer(seconden=24.0):
    """Every part repeats a whole number of times in the loop, so the end runs into the start without a seam."""
    n = int(seconden * SR)
    t = np.arange(n) / SR
    pad = np.zeros(n)
    # a D - A - E - F# pad (a sus chord: neither happy nor sad), each voice slightly detuned and breathing slowly
    for f, adem in ((146.83, 2), (220.0, 3), (329.63, 4), (369.99, 5), (587.33, 3)):
        for detune in (-0.25, 0.25):
            ff = round((f + detune) * seconden) / seconden            # (a whole number of cycles in the loop)
            pad += np.sin(2 * np.pi * ff * t) * (0.6 + 0.4 * np.sin(2 * np.pi * adem * t / seconden + f))
    pad = low(np.concatenate([pad, pad]), 1400)[n:]                    # (filtered over two rounds: no click at the seam)
    # wind: noise through a slowly moving band
    ruis = rng.normal(0, 1, 2 * n)
    wind = band(ruis, 300, 900)[n:] * (0.5 + 0.5 * np.sin(2 * np.pi * 2 * t / seconden))
    # chimes: a few soft glassy notes on fixed spots in the loop
    chimes = np.zeros(n)
    for start, f in ((2.0, 1174.66), (7.5, 1479.98), (13.0, 1318.51), (19.0, 1760.0)):
        s = int(start * SR)
        m = int(3.5 * SR)
        tt = np.arange(m) / SR
        toon = (np.sin(2 * np.pi * f * tt) + 0.4 * np.sin(2 * np.pi * f * 2.76 * tt)) * np.exp(-tt * 1.6)
        chimes[s:s + m] += toon[:n - s]
    mix = pad * 0.55 + wind * 0.6 * np.max(np.abs(pad)) / (np.max(np.abs(wind)) + 1e-9) + chimes * 0.18 * np.max(np.abs(pad))
    return norm(mix, 0.45)


def gefluister(seed, lengte):
    r = np.random.default_rng(seed)
    n = int(lengte * SR)
    t = np.arange(n) / SR
    adem = r.normal(0, 1, n)
    stem = sum(band(adem, f * 0.9, f * 1.1) * g for f, g in ((700, 1.0), (1150, 0.7), (2500, 0.4), (3300, 0.25)))
    lettergrepen = np.clip(np.sin(2 * np.pi * (3.2 + r.random()) * t) * 1.3, 0, 1) ** 2
    return norm(stem * lettergrepen * env(n, 0.15, lengte * 0.4), 0.5)


def meditatie():
    n = int(3.0 * SR)
    t = np.arange(n) / SR
    bel = sum(np.sin(2 * np.pi * 523.25 * k * t) * np.exp(-t * (0.9 + k * 0.6)) * g
              for k, g in ((1, 1.0), (2.01, 0.5), (2.76, 0.35), (5.4, 0.15)))
    return norm(bel * env(n, 0.003, 1.0, 1.2), 0.7)


def tegel():
    n = int(0.35 * SR)
    t = np.arange(n) / SR
    schraap = band(rng.normal(0, 1, n), 400, 2400) * np.exp(-t * 9)
    klik = np.sin(2 * np.pi * 180 * t) * np.exp(-np.maximum(t - 0.22, 0) * 40) * (t > 0.22)
    return norm(schraap * 0.6 + klik, 0.7)


if __name__ == "__main__":
    save("sfeer", sfeer())
    for i, (s, l) in enumerate(((501, 1.6), (502, 2.1), (503, 1.3)), 1):
        save(f"gefluister{i}", gefluister(s, l))
    save("meditatie", meditatie())
    save("tegel", tegel())
