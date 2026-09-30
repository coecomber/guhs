"""
Het Knuffelbad (2.8) - the sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/knuffelbad/:

  plons1-2      a big splash: a deep "bloop" and a burst of water noise
  spetter1-2    a small splash
  glijden       the rush of water under the ring (a seamless loop; the game follows your speed with its volume and pitch)
  piep1-3       a rubber duck squeak
  schuim1-2     foam: lots of tiny bubbles popping
  fohn          the guh-föhn's warm whirr (a loop)
  fluit         Badmeester Bubbel's whistle: TUUUT

Not part of make_resources.py (the OGGs are committed like the other sounds). Needs numpy, scipy and soundfile:
    python tools/features/knuffelbad_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "knuffelbad")
rng = np.random.default_rng(28070)


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


def plons(dur, deep, seed):
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = np.arange(n) / SR
    # the bloop: a falling sine with a quick decay
    f = deep * (1 + 1.8 * np.exp(-t * 18))
    bloop = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 9)
    # the burst of water: band noise with a fast attack and a long crackly tail
    noise = r.standard_normal(n)
    burst = band(noise, 400, 5000) * np.exp(-t * 5)
    crackle = band(noise * (r.random(n) < 0.02), 1500, 9000) * np.exp(-t * 2.5) * 3
    drops = np.zeros(n)
    for _ in range(40):
        s = int(r.uniform(0.05, dur * 0.9) * SR)
        fd = r.uniform(900, 2600)
        m = min(n - s, int(0.05 * SR))
        tt = np.arange(m) / SR
        drops[s:s + m] += np.sin(2 * np.pi * fd * (1 + 3 * tt) * tt) * np.exp(-tt * 70) * r.uniform(0.1, 0.4)
    x = bloop * 1.2 + burst * 0.8 + crackle * 0.4 + drops * np.exp(-t * 1.5)
    return norm(x * env(n, 0.004, 0.25), 0.9)


def glijden(dur=3.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    noise = rng.standard_normal(n + SR)
    body = band(noise, 150, 1400)[SR:] * 0.8 + band(noise, 1400, 6000)[SR:] * 0.35
    # gurgles: slow wobble, periodic over the loop so it joins up
    wob = 0.75 + 0.25 * np.sin(2 * np.pi * t * 3 / dur) * np.sin(2 * np.pi * t * 7 / dur)
    x = body * wob
    # a seamless loop: cross-fade the end into the start
    fade = int(0.25 * SR)
    ramp = np.linspace(0, 1, fade)
    x[:fade] = x[:fade] * ramp + x[-fade:] * (1 - ramp)
    return norm(x[:-fade], 0.7)


def piep(pitch, seed):
    r = np.random.default_rng(seed)
    dur = 0.22
    n = int(dur * SR)
    t = np.arange(n) / SR
    # a rubber duck: a squeezed reed - a high tone that bends up then down, lots of buzz
    f = pitch * (1 + 0.35 * np.sin(np.pi * t / dur)) * (1 + 0.02 * np.sin(2 * np.pi * 38 * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    tone = np.sign(np.sin(ph)) * 0.4 + np.sin(ph) * 0.6 + 0.3 * np.sin(2 * ph)
    breath = band(r.standard_normal(n), 2000, 7000) * 0.15
    x = band(tone, pitch * 0.7, pitch * 5) + breath
    return norm(x * env(n, 0.01, 0.06), 0.75)


def schuim(dur, seed):
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    x = np.zeros(n)
    for _ in range(int(dur * 260)):
        s = int(r.uniform(0, dur - 0.03) * SR)
        f = r.uniform(1800, 6500)
        m = int(r.uniform(0.004, 0.012) * SR)
        tt = np.arange(m) / SR
        x[s:s + m] += np.sin(2 * np.pi * f * (1 + 6 * tt) * tt) * np.exp(-tt * 400) * r.uniform(0.2, 1)
    x += high(r.standard_normal(n), 3000) * 0.05
    return norm(x * env(n, 0.03, 0.2), 0.6)


def fohn(dur=2.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    motor = sum(np.sin(2 * np.pi * 180 * k * t) / k for k in range(1, 6)) * 0.25
    air = band(rng.standard_normal(n), 600, 5000) * 0.9
    x = motor + air
    fade = int(0.2 * SR)
    ramp = np.linspace(0, 1, fade)
    x[:fade] = x[:fade] * ramp + x[-fade:] * (1 - ramp)
    return norm(x[:-fade], 0.55)


def fluit():
    dur = 0.9
    n = int(dur * SR)
    t = np.arange(n) / SR
    # a pea whistle: a pure high tone warbling fast (the pea), a breathy edge
    f = 2900 * (1 + 0.04 * np.sin(2 * np.pi * 28 * t))
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR)
    breath = band(rng.standard_normal(n), 2500, 8000) * 0.2
    x = tone + breath
    return norm(x * env(n, 0.02, 0.15, 1.5), 0.7)


if __name__ == "__main__":
    save("plons1", plons(1.6, 70, 1))
    save("plons2", plons(1.9, 55, 2))
    save("spetter1", plons(0.6, 150, 3))
    save("spetter2", plons(0.5, 190, 4))
    save("glijden", glijden())
    save("piep1", piep(1100, 5))
    save("piep2", piep(1300, 6))
    save("piep3", piep(950, 7))
    save("schuim1", schuim(1.0, 8))
    save("schuim2", schuim(1.3, 9))
    save("fohn", fohn())
    save("fluit", fluit())
