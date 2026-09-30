"""
3.0 (Guhverhalen), slice vogels: the birds' sounds, synthesized (mono OGG, 44.1 kHz) into assets/guhs/sounds/vogels/:

  pluisvinkje1-3   a soft, high "tjiep-tjiep" (two or three little chirps that glide up)
  kaasmeesje1-2    the great tit's song, cheese style: "tsjie-tsjie-bee, tsjie-tsjie-bee"
  guh_uiltje1-2    a soft, hollow "oe-hoe... njeg" (two low hoots and a little rising squeak at the end)
  zeemeeuwtje1-2   a gull's "kliew-kliew" (a nasal, falling cry)
  mijn_mijn1-2     "Mijn! Mijn!": a squawky gull voice saying "mijn" twice (m - ij - n, formants gliding from 'e' to 'ie')
  fladder1-2       wings flapping when a bird takes off: a few soft whooshes

Not part of make_resources.py (the OGGs are committed like the other sounds; the merge copies them). Needs numpy, scipy and
soundfile:   python tools/features/vogels_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "vogels")
NAMES = {"pluisvinkje": 3, "kaasmeesje": 2, "guh_uiltje": 2, "zeemeeuwtje": 2, "mijn_mijn": 2, "fladder": 2}


def band(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def env(n, attack, release, curve=2.0):
    e = np.ones(n)
    a, r = max(1, int(attack * SR)), max(1, int(release * SR))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] *= np.linspace(1, 0, r) ** curve
    return e


def norm(x, gain=0.8):
    return (x / (np.max(np.abs(x)) + 1e-9) * gain).astype(np.float32)


def silence(s):
    return np.zeros(int(s * SR))


def sweep(f0, f1, dur, harm=(1.0,), vib=0.0, vib_f=0.0, curve=1.0):
    """A tone gliding from f0 to f1 (curve shapes the glide), with some harmonics and vibrato."""
    n = int(dur * SR)
    t = np.linspace(0, 1, n)
    f = f0 + (f1 - f0) * t ** curve
    if vib:
        f = f * (1 + vib * np.sin(2 * np.pi * vib_f * np.arange(n) / SR))
    ph = 2 * np.pi * np.cumsum(f) / SR
    return sum(a * np.sin((i + 1) * ph) for i, a in enumerate(harm))


def chirp(f0, f1, dur, rng):
    x = sweep(f0, f1, dur, (1.0, 0.18, 0.05), curve=0.7) * env(int(dur * SR), 0.006, dur * 0.6)
    return x * (1 + 0.02 * rng.standard_normal(len(x)))


def pluisvinkje(seed):
    rng = np.random.default_rng(seed)
    parts = []
    for _ in range(2 + seed % 2):
        f0 = 3400 + rng.uniform(-300, 300)
        parts += [chirp(f0, f0 * 1.45, 0.07 + rng.uniform(0, 0.03), rng), silence(0.05 + rng.uniform(0, 0.04))]
    return norm(np.concatenate(parts + [silence(0.1)]), 0.6)


def kaasmeesje(seed):
    rng = np.random.default_rng(seed)
    parts = []
    for _ in range(2):
        for f in (5200, 5200):        # tsjie tsjie
            parts += [chirp(f + rng.uniform(-150, 150), f * 0.8, 0.06, rng) * 0.8, silence(0.05)]
        parts += [sweep(3000, 2900, 0.16, (1.0, 0.1)) * env(int(0.16 * SR), 0.01, 0.08) * 0.7, silence(0.12)]   # bee
    return norm(np.concatenate(parts), 0.6)


def hoot(f, dur):
    n = int(dur * SR)
    x = sweep(f * 1.05, f * 0.95, dur, (1.0, 0.25, 0.08), vib=0.01, vib_f=6)
    breath = band(np.random.default_rng(3).standard_normal(n), f * 0.8, f * 3) * 0.08
    return (x + breath) * env(n, 0.05, dur * 0.5, 1.5)


def guh_uiltje(seed):
    rng = np.random.default_rng(seed)
    f = 420 + rng.uniform(-30, 30)
    squeak = sweep(900, 1500, 0.14, (1.0, 0.3)) * env(int(0.14 * SR), 0.01, 0.07) * 0.35   # njeg!
    return norm(np.concatenate([hoot(f, 0.28), silence(0.12), hoot(f * 0.92, 0.5), silence(0.16), squeak, silence(0.1)]), 0.7)


def gull_cry(f0, f1, dur, rng, nasal=0.5):
    """A gull voice: a buzzy pulse train through a couple of formant bands (nasal, a bit rough)."""
    n = int(dur * SR)
    t = np.linspace(0, 1, n)
    f = f0 + (f1 - f0) * t ** 0.8
    f = f * (1 + 0.03 * np.sin(2 * np.pi * 28 * np.arange(n) / SR))      # a gull's rattle
    ph = np.cumsum(f) / SR
    pulses = (np.mod(ph, 1.0) < 0.18).astype(float) - 0.18                # a thin, buzzy source
    x = band(pulses, 900, 1600) * 1.0 + band(pulses, 2200, 3600) * 0.7 + band(pulses, 300, 600) * nasal
    x += band(rng.standard_normal(n), 2000, 5000) * 0.05
    return x * env(n, 0.02, dur * 0.4)


def zeemeeuwtje(seed):
    rng = np.random.default_rng(seed)
    parts = []
    for i in range(2 + seed % 2):
        parts += [gull_cry(1200 - i * 60, 700, 0.22, rng), silence(0.08)]
    return norm(np.concatenate(parts), 0.7)


def formant_voice(f0s, formants, dur, rng):
    """A tiny formant synthesizer: glottal-ish pulses at f0 (a list of glide points), through formant resonators whose
    frequencies glide along `formants` (a list of (F1, F2, F3) points)."""
    n = int(dur * SR)
    t = np.linspace(0, 1, n)
    f0 = np.interp(t, np.linspace(0, 1, len(f0s)), f0s)
    f0 = f0 * (1 + 0.025 * np.sin(2 * np.pi * 24 * np.arange(n) / SR))
    ph = np.cumsum(f0) / SR
    src = (np.mod(ph, 1.0) < 0.3) * 1.0 - 0.3 + 0.03 * rng.standard_normal(n)
    out = np.zeros(n)
    block = 512
    for k in range(3):
        fk = np.interp(t, np.linspace(0, 1, len(formants)), [f[k] for f in formants])
        amp = (1.0, 0.7, 0.35)[k]
        for s in range(0, n, block):
            fc = float(fk[min(n - 1, s + block // 2)])
            bw = 90 + fc * 0.08
            lo, hi = max(60, fc - bw), min(SR / 2 - 100, fc + bw)
            seg = band(src[max(0, s - 2048):s + block], lo, hi)
            out[s:s + block] += amp * seg[-len(out[s:s + block]):]
    return out


def mijn(rng, pitch=1.0):
    """'Mijn!': m (closed, nasal hum) -> ij ('e' gliding to 'ie') -> n (nasal again), squawky and high like a gull."""
    dur = 0.42
    f0s = [720 * pitch, 900 * pitch, 980 * pitch, 900 * pitch, 760 * pitch]
    formants = [(280, 1300, 2500), (320, 1500, 2600), (560, 1850, 2700), (480, 2100, 2900), (330, 2350, 3100), (300, 1600, 2600)]
    x = formant_voice(f0s, formants, dur, rng)
    n = len(x)
    e = env(n, 0.02, 0.1, 1.5)
    t = np.linspace(0, 1, n)
    e *= np.clip(0.35 + (t - 0.12) * 6, 0.35, 1.0) * np.clip(1.0 - (t - 0.72) * 2.2, 0.35, 1.0)   # the m and n are softer
    return np.tanh(x * e * 3.0)                       # a little rough, like a gull


def mijn_mijn(seed):
    rng = np.random.default_rng(seed)
    p = 1.0 + (seed % 2) * 0.08
    return norm(np.concatenate([mijn(rng, p), silence(0.1), mijn(rng, p * 1.08), silence(0.08)]), 0.75)


def fladder(seed):
    rng = np.random.default_rng(seed)
    parts = []
    for i in range(4):
        d = 0.07
        x = band(rng.standard_normal(int(d * SR)), 300, 2500) * env(int(d * SR), 0.01, 0.05)
        parts += [x * (1 - i * 0.15), silence(0.035)]
    return norm(np.concatenate(parts), 0.5)


def main():
    os.makedirs(OUT, exist_ok=True)
    makers = {"pluisvinkje": pluisvinkje, "kaasmeesje": kaasmeesje, "guh_uiltje": guh_uiltje, "zeemeeuwtje": zeemeeuwtje,
              "mijn_mijn": mijn_mijn, "fladder": fladder}
    for name, count in NAMES.items():
        for i in range(1, count + 1):
            x = makers[name](i)
            sf.write(os.path.join(OUT, f"{name}{i}.ogg"), x, SR, format="OGG", subtype="VORBIS")
            print("wrote", f"{name}{i}", f"{len(x) / SR:.2f}s")


if __name__ == "__main__":
    main()
