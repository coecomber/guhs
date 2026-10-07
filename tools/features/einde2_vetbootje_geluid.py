"""
einde2, slice vetbootje - the sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/einde2_vetbootje/:

  fluitje1-2   the vetfluitje: a fat, slightly sliding toot with a happy second note (a whistle that was in the frying pan)
  aankomst     the vetbootje gliding in: a soft whoosh that ends in a little wooden bonk and a drip
  kraak1-2     the hull creaking while it sails
  verf         a brush slapping paint on (the scheepswerf)
  brander1-2   the balloon's burner: a short roar
  vouw1-2      balloon cloth rustling (unfolding, folding)

Not part of make_resources.py (the OGGs are committed like the other sounds; einde2_vetbootje.sounds(h) lists them in
sounds.json). Needs numpy, scipy and soundfile:
    python tools/features/einde2_vetbootje_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "einde2_vetbootje")
# sound event (einde2_vetbootje.<name>) -> its files; einde2_vetbootje.py reads this for sounds.json
BESTANDEN = {"fluitje": ["fluitje1", "fluitje2"], "aankomst": ["aankomst"], "kraak": ["kraak1", "kraak2"], "verf": ["verf"],
             "brander": ["brander1", "brander2"], "vouw": ["vouw1", "vouw2"]}


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


def toot(f0, f1, dur, r):
    """One note of the whistle: a breathy tone sliding from f0 to f1 with the pea rattling in it."""
    t = t_(dur)
    f = f0 + (f1 - f0) * (t / dur) ** 0.6
    fase = 2 * np.pi * np.cumsum(f) / SR
    triller = 1 + 0.25 * np.sin(2 * np.pi * 27 * t)                 # the pea
    toon = (np.sin(fase) + 0.35 * np.sin(2 * fase) + 0.12 * np.sin(3 * fase)) * triller
    adem = band(r.normal(0, 1, len(t)), f0 * 0.8, f1 * 2.2) * 0.25
    return (toon + adem) * env(len(t), 0.015, dur * 0.35, 1.5)


def fluitje(seed, hoog):
    r = np.random.default_rng(seed)
    a = toot(1250 * hoog, 1180 * hoog, 0.28, r)                      # a fat toot that sags a little (vet in the whistle)
    b = toot(1500 * hoog, 1720 * hoog, 0.34, r)                      # and a happy one going up
    x = np.concatenate([a, np.zeros(int(0.04 * SR)), b])
    return norm(low(x, 6000), 0.6)


def aankomst(seed):
    r = np.random.default_rng(seed)
    dur = 0.9
    n = int(dur * SR)
    t = t_(dur)
    ruis = r.normal(0, 1, n)
    x = np.zeros(n)
    blok = 2048
    for i in range(0, n, blok):                                      # a whoosh coming down
        f = 1500 - 1100 * min(1, i / n)
        seg = band(ruis[max(0, i - 4096):i + blok], f * 0.6, f * 1.7)
        x[i:i + blok] = seg[-min(blok, n - i):]
    x *= np.sin(np.pi * np.minimum(1, t / 0.7)) ** 2 * 0.5
    bonk_t = t_(0.18)
    bonk = np.sin(2 * np.pi * 170 * bonk_t) * np.exp(-bonk_t * 26) + 0.4 * np.sin(2 * np.pi * 410 * bonk_t) * np.exp(-bonk_t * 40)
    start = int(0.62 * SR)
    x[start:start + len(bonk)] += bonk * 0.9
    drup_t = t_(0.09)
    drup = np.sin(2 * np.pi * (900 + 2600 * drup_t / 0.09) * drup_t) * np.exp(-drup_t * 30)
    start = int(0.8 * SR)
    x[start:start + len(drup)] += drup[:n - start] * 0.35
    return norm(x, 0.6)


def kraak(dur, seed, f0):
    """Wood creaking: a slow stick-slip (a low pulse train whose rate wanders) through a woody resonance."""
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = t_(dur)
    tempo = f0 * (1 + 0.5 * np.sin(2 * np.pi * 1.3 * t + r.random() * 6)) * (0.6 + 0.4 * t / dur)
    fase = np.cumsum(tempo) / SR
    pulsen = (np.diff(np.floor(fase), prepend=0) > 0).astype(float) * (0.6 + 0.4 * r.random(n))
    x = band(pulsen, 280, 1400) * 6 + band(pulsen, 1800, 3200) * 2
    x += band(r.normal(0, 0.02, n), 300, 900)
    return norm(x * env(n, 0.05, dur * 0.4), 0.5)


def verf(seed):
    r = np.random.default_rng(seed)
    parts = []
    for i, (dur, f) in enumerate(((0.16, 900), (0.2, 700), (0.24, 1100))):   # three wet strokes
        n = int(dur * SR)
        veeg = band(r.normal(0, 1, n), f * 0.5, f * 3) * np.sin(np.pi * t_(dur) / dur) ** 1.5
        klets = low(r.normal(0, 1, n), 500) * np.exp(-t_(dur) * 30) * 1.5      # the slap at the start
        parts += [veeg + klets, np.zeros(int(0.05 * SR))]
    return norm(np.concatenate(parts), 0.55)


def brander(dur, seed):
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    t = t_(dur)
    loei = low(r.normal(0, 1, n), 420, 3) * 2.2 + band(r.normal(0, 1, n), 500, 2600) * 0.6
    sis = high(r.normal(0, 1, n), 5000) * 0.12
    flakker = 0.8 + 0.2 * np.sin(2 * np.pi * 17 * t + r.random() * 6) * np.sin(2 * np.pi * 3.1 * t)
    return norm((loei * flakker + sis) * env(n, 0.06, dur * 0.45, 1.5), 0.6)


def vouw(dur, seed):
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    x = np.zeros(n)
    for _ in range(9):                                               # little rustles of cloth, one over the other
        d = 0.08 + r.random() * 0.14
        m = int(d * SR)
        start = int(r.random() * (dur - d) * SR)
        f = 1500 + r.random() * 3500
        x[start:start + m] += band(r.normal(0, 1, m), f * 0.5, min(f * 2.2, 15000)) * np.sin(np.pi * t_(d)[:m] / d) ** 2 * (0.4 + r.random() * 0.6)
    plof = low(r.normal(0, 1, n), 260) * np.exp(-t_(dur) * 9) * 0.8
    return norm(x + plof, 0.5)


def main():
    save("fluitje1", fluitje(2140650, 1.0))
    save("fluitje2", fluitje(2140651, 1.08))
    save("aankomst", aankomst(2140652))
    save("kraak1", kraak(0.9, 2140653, 38))
    save("kraak2", kraak(0.7, 2140654, 52))
    save("verf", verf(2140655))
    save("brander1", brander(0.8, 2140656))
    save("brander2", brander(0.7, 2140657))
    save("vouw1", vouw(0.7, 2140658))
    save("vouw2", vouw(0.6, 2140659))


if __name__ == "__main__":
    main()
