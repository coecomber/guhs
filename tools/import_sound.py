"""
Converts any audio file (e.g. a WhatsApp voice note, which is Opus) into a Minecraft-ready sound:
mono Ogg Vorbis, leading/trailing silence trimmed, peak-normalised, optionally pitched up/down.

Usage (from the project root):
    python tools/import_sound.py "path/to/input.ogg" guh_ambient1 [--pitch 1.2]

--pitch 1.2 = 20% higher (and a bit shorter), 0.8 = lower. The result is written to
src/main/resources/assets/guhs/sounds/<name>.ogg. Remember to list new names in sounds.json.

Requires: numpy, scipy, soundfile
"""
import argparse
import os
from fractions import Fraction

import numpy as np
import soundfile as sf
from scipy.signal import resample_poly

OUT_DIR = os.path.join("src", "main", "resources", "assets", "guhs", "sounds")


def trim_silence(x, rate, threshold_db=-40.0, pad_s=0.03):
    level = np.abs(x)
    threshold = np.max(level) * 10 ** (threshold_db / 20)
    loud = np.where(level > threshold)[0]
    if len(loud) == 0:
        return x
    pad = int(pad_s * rate)
    return x[max(0, loud[0] - pad):min(len(x), loud[-1] + pad)]


def convert(src, name, pitch=1.0):
    audio, rate = sf.read(src, dtype="float32", always_2d=True)
    x = audio.mean(axis=1)  # mono, so Minecraft plays it at the guh's position
    before = len(x) / rate
    x = trim_silence(x, rate)
    if pitch != 1.0:
        # play the samples faster at the same rate -> higher pitch (like speeding up a tape)
        frac = Fraction(pitch).limit_denominator(100)
        x = resample_poly(x, frac.denominator, frac.numerator).astype(np.float32)
    fade = min(len(x) // 4, int(0.01 * rate))  # tiny fades avoid clicks
    if fade > 0:
        x[:fade] *= np.linspace(0, 1, fade)
        x[-fade:] *= np.linspace(1, 0, fade)
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.9
    out = os.path.join(OUT_DIR, name + ".ogg")
    sf.write(out, x, rate, format="OGG", subtype="VORBIS")
    print(f"{os.path.basename(src)} -> {name}.ogg  ({before:.2f}s -> {len(x) / rate:.2f}s{', pitch x' + str(pitch) if pitch != 1 else ''})")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("input")
    parser.add_argument("name")
    parser.add_argument("--pitch", type=float, default=1.0)
    args = parser.parse_args()
    convert(args.input, args.name, args.pitch)
