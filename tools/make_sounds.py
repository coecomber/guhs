"""
Generates the synthesized placeholder Guh sounds (hurt + death) as mono OGG files.
The ambient / happy / eat sounds are real recordings (imported with tools/import_sound.py), so this
script no longer touches those.

You don't need this script to replace sounds: just drop your own mono .ogg files over the ones in
src/main/resources/assets/guhs/sounds/ (same names), or add more variants in sounds.json.

Requires: numpy, scipy, soundfile  (pip install numpy scipy soundfile)
Run from the project root:  python tools/make_sounds.py
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds")
rng = np.random.default_rng(1337)


def envelope(n, attack=0.02, release=0.08):
    t = np.linspace(0, 1, n)
    a = max(1, int(attack * SR))
    r = max(1, int(release * SR))
    env = np.ones(n)
    env[:a] = np.linspace(0, 1, a) ** 0.7
    env[-r:] *= np.linspace(1, 0, r) ** 1.5
    return env


def bandpass(x, lo, hi):
    b, a = butter(2, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def voice(duration, f0_curve, formants=((900, 0.9), (1700, 0.6), (3200, 0.25)), breath=0.04, vibrato=6.0):
    """Tiny formant synth: a glottal-ish pulse train pushed through vowel 'uh' formants, pitched up for cuteness."""
    n = int(duration * SR)
    t = np.arange(n) / SR
    f0 = f0_curve(np.linspace(0, 1, n)) * (1 + 0.015 * np.sin(2 * np.pi * vibrato * t))
    phase = np.cumsum(f0) / SR
    src = 2 * (phase % 1.0) - 1  # sawtooth
    src = src + breath * rng.standard_normal(n)
    out = np.zeros(n)
    for freq, gain in formants:
        out += gain * bandpass(src, freq * 0.8, freq * 1.25)
    return out


def finish(x, attack=0.015, release=0.08, gain=0.8):
    x = x * envelope(len(x), attack, release)
    x = x / (np.max(np.abs(x)) + 1e-9) * gain
    pad = np.zeros(int(0.02 * SR))
    return np.concatenate([x, pad]).astype(np.float32)


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    sf.write(os.path.join(OUT, name + ".ogg"), x, SR, format="OGG", subtype="VORBIS")
    print("wrote", name, f"{len(x) / SR:.2f}s")


# hurt: sharp high squeak
save("guh_hurt1", finish(voice(0.14, lambda p: 1100 - 350 * p, breath=0.12), attack=0.004))
save("guh_hurt2", finish(voice(0.12, lambda p: 1250 - 500 * p ** 0.5, breath=0.12), attack=0.004))
# death: long deflating "guuuuh"
save("guh_death", finish(voice(0.7, lambda p: 720 * (1 - 0.55 * p ** 1.3), vibrato=9), release=0.3))
