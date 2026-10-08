"""
biomes3 wereld, the Bloesemmeertje - its sounds, synthesized (OGG Vorbis, 44.1 kHz):

  assets/guhs/sounds/bloesemmeertje/muziek1, muziek2   the lake's own calm music (stereo, about 100 s each): slow soft
                                                       plucked notes on a five-note scale over a warm pad, a lot of air
  assets/guhs/sounds/bloesemmeertje/ambient            the ambience loop (mono, 20 s, seamless): gentle lapping water
                                                       and a light wind

Not part of make_resources.py (the OGGs are committed like the other sounds; bio_wereld_meer.py writes sounds.json and
checks that the files are there). Fixed seeds. Needs numpy, scipy and soundfile:
    python tools/features/bio_wereld_meer_geluid.py        (from the project root)
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, fftconvolve, lfilter

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "bloesemmeertje")


def band(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def low(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="low")
    return lfilter(b, a, x)


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    x = x.astype(np.float32)
    # (in pieces: libsndfile's Vorbis writer falls over on one long buffer)
    with sf.SoundFile(os.path.join(OUT, name + ".ogg"), "w", SR, 1 if x.ndim == 1 else x.shape[1], format="OGG", subtype="VORBIS") as f:
        for a in range(0, len(x), SR // 2):
            f.write(x[a:a + SR // 2])
    print("wrote", name, f"{len(x) / SR:.1f}s", "peak %.2f" % float(np.max(np.abs(x))))


def hz(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def pluk(midi, dur, r):
    """A soft mallet / kalimba note: a sine with two quiet overtones, a rounded attack, a long decay."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = hz(midi) * (1 + r.uniform(-0.0015, 0.0015))
    x = np.sin(2 * np.pi * f * t) + 0.28 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 5) + 0.10 * np.sin(2 * np.pi * 3.01 * f * t) * np.exp(-t * 9)
    return x * (1 - np.exp(-t / 0.006)) * np.exp(-t / (dur * 0.28))


def pad(midis, dur, r):
    """A warm chord that swells in and out: detuned sines, slowly moving."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for m in midis:
        for det in (-0.004, 0.0, 0.004):
            f = hz(m) * (1 + det)
            x += np.sin(2 * np.pi * f * t + r.uniform(0, 6.28)) * (0.8 + 0.2 * np.sin(2 * np.pi * r.uniform(0.07, 0.19) * t + r.uniform(0, 6.28)))
    env = np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 1.5
    return low(x, 900) * env / (3 * len(midis))


def galm(n_sec, seed, links):
    """An impulse response of a soft large room (decaying noise), a little different left and right."""
    r = np.random.default_rng(seed + (0 if links else 1))
    n = int(n_sec * SR)
    t = np.arange(n) / SR
    ir = r.standard_normal(n) * np.exp(-t / (n_sec * 0.22))
    ir[: int(0.012 * SR)] = 0
    return low(ir, 3800) / np.sqrt(np.sum(ir ** 2) + 1e-9)


def muziek(seed, akkoorden, toonladder, dur=100.0):
    """akkoorden: pad chords of 8 s each (midi notes), repeated; toonladder: the melody's notes (midi)."""
    r = np.random.default_rng(seed)
    n = int((dur + 8) * SR)
    droog = np.zeros((n, 2))
    # the pad
    t0 = 1.0
    i = 0
    while t0 < dur - 9:
        p = pad(akkoorden[i % len(akkoorden)], 11.0, r) * 0.55
        a = int(t0 * SR)
        droog[a:a + len(p), 0] += p
        droog[a:a + len(p), 1] += np.roll(p, 37)
        t0 += 8.0
        i += 1
    # the melody: short phrases with long rests, a slow walk over the scale
    tijd, idx = 3.0, len(toonladder) // 2
    while tijd < dur - 8:
        for _ in range(int(r.integers(2, 6))):
            idx = int(np.clip(idx + r.choice([-2, -1, -1, 1, 1, 2]), 0, len(toonladder) - 1))
            lang = float(r.choice([1.0, 1.0, 1.5, 2.0]))
            noot = pluk(toonladder[idx], 4.0, r) * r.uniform(0.16, 0.26)
            pan = r.uniform(0.3, 0.7)
            a = int((tijd + r.uniform(-0.02, 0.02)) * SR)
            droog[a:a + len(noot), 0] += noot * (1 - pan) * 1.6
            droog[a:a + len(noot), 1] += noot * pan * 1.6
            if r.random() < 0.3:
                # a quiet echo an octave up
                hoog = pluk(toonladder[idx] + 12, 3.0, r) * 0.06
                b = a + int(0.5 * lang * SR)
                droog[b:b + len(hoog), 0] += hoog * pan
                droog[b:b + len(hoog), 1] += hoog * (1 - pan)
            tijd += lang
        tijd += float(r.uniform(2.5, 6.0))
    nat = np.stack([fftconvolve(droog[:, 0], galm(3.2, seed, True))[:n], fftconvolve(droog[:, 1], galm(3.2, seed, False))[:n]], axis=1)
    x = droog * 0.75 + nat * 0.55
    uit = int(6 * SR)
    x[-uit:] *= np.linspace(1, 0, uit)[:, None] ** 2
    inn = int(0.5 * SR)
    x[:inn] *= np.linspace(0, 1, inn)[:, None]
    return x / (np.max(np.abs(x)) + 1e-9) * 0.62


def ambient(dur=20.0):
    """Lapping water at a calm shore and a light wind; the end runs into the start."""
    r = np.random.default_rng(4906)
    fade = int(1.5 * SR)
    n = int(dur * SR) + fade
    t = np.arange(n) / SR
    w = r.standard_normal(n + SR)
    # the wind: a low soft rush with a slow swell that is periodic over the loop
    wind = band(w, 120, 520)[SR:] * (0.55 + 0.45 * np.sin(2 * np.pi * t * 1 / dur + 0.7) * np.sin(2 * np.pi * t * 3 / dur))
    hoog = band(w, 900, 2600)[SR:] * 0.12 * (0.5 + 0.5 * np.sin(2 * np.pi * t * 2 / dur + 2.1))
    # the water: small laps, each a short band-noise swell that slides down in pitch a little
    water = np.zeros(n)
    w2 = r.standard_normal(n + SR)
    helder = band(w2, 500, 2400)[SR:]
    dof = band(w2, 220, 900)[SR:]
    tijd = 0.4
    while tijd < dur - 0.2:
        lang = float(r.uniform(0.7, 1.5))
        a, m = int(tijd * SR), int(lang * SR)
        tt = np.arange(m) / m
        env = np.sin(np.pi * tt) ** 2 * (1 - 0.5 * tt) * r.uniform(0.5, 1.0)
        water[a:a + m] += (helder[a:a + m] * (1 - tt) * 0.7 + dof[a:a + m]) * env[: len(water[a:a + m])]
        tijd += float(r.uniform(1.1, 2.4))
    x = wind * 0.55 + hoog + water * 0.9
    ramp = np.linspace(0, 1, fade)
    x[:fade] = x[:fade] * np.sqrt(ramp) + x[-fade:] * np.sqrt(1 - ramp)
    x = x[:-fade]
    return x / (np.max(np.abs(x)) + 1e-9) * 0.4


if __name__ == "__main__":
    # D major, five notes: D E F# A B
    ladder = [62, 64, 66, 69, 71, 74, 76, 78, 81, 83]
    save("muziek1", muziek(5101, [[50, 57, 62, 66], [47, 54, 59, 62], [43, 50, 59, 62], [45, 52, 57, 64]], ladder))
    # the same scale from G: softer, more open
    save("muziek2", muziek(5102, [[43, 50, 55, 59], [50, 57, 62, 64], [52, 59, 64, 67], [45, 52, 57, 62]], [l - 0 for l in ladder[1:9]], dur=92.0))
    save("ambient", ambient())
