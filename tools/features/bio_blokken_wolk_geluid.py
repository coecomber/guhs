"""
biomes3 slice "blokken-wolk" - the sounds, synthesized (mono OGG, 44.1 kHz):

  assets/guhs/sounds/wolkenblok/stap1-4     a step on cloud: a soft low "pff", hardly any attack
  assets/guhs/sounds/wolkenblok/plaats1-2   cloud put down: a puff that settles
  assets/guhs/sounds/wolkenblok/breek1-2    cloud blown apart: a longer airy whoosh, a few fine crackles
  assets/guhs/sounds/waterval/ruis          the rush at the foot of a big waterfall (a seamless loop of 6 s: the game
                                            sets its place and its volume, WatervalEffecten.java)

Not part of make_resources.py (the OGGs are committed like the other sounds; bio_blokken_wolk.py writes sounds.json and
checks that the files are there). Fixed seeds. Needs numpy, scipy and soundfile:
    python tools/features/bio_blokken_wolk_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds")


def band(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def low(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="low")
    return lfilter(b, a, x)


def norm(x, gain):
    return (x / (np.max(np.abs(x)) + 1e-9) * gain).astype(np.float32)


def save(map_, name, x):
    os.makedirs(os.path.join(OUT, map_), exist_ok=True)
    sf.write(os.path.join(OUT, map_, name + ".ogg"), x, SR, format="OGG", subtype="VORBIS")
    print("wrote", map_, name, f"{len(x) / SR:.2f}s")


def pluf(seed, dur, hoog, aanzet, staart, gain, knisper=0.0):
    """A puff of air: band noise with a rounded attack and a soft tail, darker towards the end."""
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    ruis = r.standard_normal(n + SR)
    helder = band(ruis, 250, hoog)[SR:]
    dof = low(ruis, 420)[SR:] * 2.2
    env = (1 - np.exp(-t / aanzet)) * np.exp(-t / staart)
    x = (helder * np.exp(-t / (staart * 0.6)) + dof * 0.9) * env
    if knisper:
        tik = (r.random(n) < 0.004) * r.uniform(0.3, 1.0, n)
        x += band(tik, 2500, 9000) * knisper * np.exp(-t / (staart * 1.2)) * 6
    uit = int(0.02 * SR)
    x[-uit:] *= np.linspace(1, 0, uit)
    return norm(x, gain)


def ruis(dur=6.0):
    """Falling water from a little way off: a soft broad rush with a slow swell, no splashes; the end runs into the start."""
    r = np.random.default_rng(8314)
    fade = int(0.6 * SR)
    n = int(dur * SR) + fade
    t = np.arange(n) / SR
    w = r.standard_normal(n + SR)
    laag = band(w, 90, 700)[SR:] * 1.0
    midden = band(w, 700, 3200)[SR:] * 0.42
    hoog = band(w, 3200, 8000)[SR:] * 0.10
    # the swell is periodic over the loop, so it joins up
    golf = 0.86 + 0.14 * np.sin(2 * np.pi * t * 2 / dur) * np.sin(2 * np.pi * t * 5 / dur + 1.3)
    x = (laag + midden * golf + hoog * golf) * (0.94 + 0.06 * np.sin(2 * np.pi * t * 3 / dur))
    ramp = np.linspace(0, 1, fade)
    x[:fade] = x[:fade] * np.sqrt(ramp) + x[-fade:] * np.sqrt(1 - ramp)
    return norm(x[:-fade], 0.55)


if __name__ == "__main__":
    if not os.path.isdir(os.path.join("src", "main", "resources")):
        raise SystemExit("run from the project root: python tools/features/bio_blokken_wolk_geluid.py")
    for i, (hoog, staart) in enumerate(((1500, 0.050), (1300, 0.060), (1700, 0.045), (1400, 0.055)), 1):
        save("wolkenblok", f"stap{i}", pluf(8300 + i, 0.22, hoog, 0.012, staart, 0.42))
    for i, (hoog, staart) in enumerate(((1800, 0.07), (1600, 0.08)), 1):
        save("wolkenblok", f"plaats{i}", pluf(8310 + i, 0.32, hoog, 0.008, staart, 0.55))
    for i, (hoog, staart) in enumerate(((2600, 0.13), (2300, 0.15)), 1):
        save("wolkenblok", f"breek{i}", pluf(8320 + i, 0.5, hoog, 0.02, staart, 0.6, knisper=0.25))
    save("waterval", "ruis", ruis())
