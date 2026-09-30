"""
Makes Mika's "evil" sounds from the normal guh sounds: lower, distorted, a bit of ring-mod wobble, some bit-crush
grit and a short dark echo. Writes assets/guhs/sounds/mika_*.ogg (mono Ogg Vorbis).

Requires: numpy, scipy, soundfile.   Run from the project root:  python tools/make_mika_sounds.py
"""
import glob
import os
from fractions import Fraction

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter, resample_poly

SOUNDS = os.path.join("src", "main", "resources", "assets", "guhs", "sounds")


def evil(x, rate):
    # 1. a bit lower (like slowing the tape down)
    frac = Fraction(0.78).limit_denominator(100)
    x = resample_poly(x, frac.numerator, frac.denominator)
    t = np.arange(len(x)) / rate
    # 2. growl: overdrive / soft clipping
    x = np.tanh(x / (np.max(np.abs(x)) + 1e-9) * 4.0)
    # 3. wobble: ring modulation at 38 Hz mixed in
    x = 0.65 * x + 0.35 * x * np.sin(2 * np.pi * 38 * t)
    # 4. grit: bit-crush to 6 bits
    x = np.round(x * 32) / 32
    # 5. darker: cut the highest fizz
    b, a = butter(2, 3800 / (rate / 2), btype="low")
    x = lfilter(b, a, x)
    # 6. short echo
    d = int(0.11 * rate)
    out = np.concatenate([x, np.zeros(d * 2)])
    out[d:d + len(x)] += 0.35 * x
    out[2 * d:2 * d + len(x)] += 0.15 * x
    return (out / (np.max(np.abs(out)) + 1e-9) * 0.9).astype(np.float32)


def convert(src, name):
    audio, rate = sf.read(src, dtype="float32", always_2d=True)
    out = evil(audio.mean(axis=1), rate)
    sf.write(os.path.join(SOUNDS, name + ".ogg"), out, rate, format="OGG", subtype="VORBIS")
    print(f"{os.path.basename(src)} -> {name}.ogg ({len(out) / rate:.2f}s)")


if __name__ == "__main__":
    ambient = sorted(glob.glob(os.path.join(SOUNDS, "guh_ambient*.ogg")),
                     key=lambda p: int(os.path.basename(p)[len("guh_ambient"):-4]))
    for i, path in enumerate(ambient, start=1):
        convert(path, f"mika_ambient{i}")
    convert(os.path.join(SOUNDS, "guh_hurt1.ogg"), "mika_hurt1")
    convert(os.path.join(SOUNDS, "guh_hurt2.ogg"), "mika_hurt2")
    convert(os.path.join(SOUNDS, "guh_death.ogg"), "mika_death")
