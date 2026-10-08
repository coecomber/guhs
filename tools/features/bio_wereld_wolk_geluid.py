"""
biomes3 wereld, the Wolkenweide - its two sounds, synthesized (mono OGG, 44.1 kHz):

  assets/guhs/sounds/wolkenweide/muziek   dreamy music, 1.5 minutes: slow warm pads under a sparse music-box melody in a
                                          pentatonic scale, with a long soft echo (the biome plays it now and then)
  assets/guhs/sounds/wolkenweide/sfeer    the ambience loop, 16 s, seamless: soft high wind that swells and sinks, and a
                                          few distant chimes

Not part of make_resources.py (the OGGs are committed like the other sounds; bio_wereld_wolk.py writes sounds.json and
checks that the files are there). Fixed seeds. Needs numpy, scipy and soundfile:
    python tools/features/bio_wereld_wolk_geluid.py        (from the project root)
"""
import os

import numpy as np
try:                                    # (only needed to MAKE the sounds; the wiki build runs without them)
    import soundfile as sf
    from scipy.signal import butter, lfilter
except ImportError:
    sf = butter = lfilter = None

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "wolkenweide")


def band(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def low(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="low")
    return lfilter(b, a, x)


def norm(x, gain):
    return (x / (np.max(np.abs(x)) + 1e-9) * gain).astype(np.float32)


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    # (in pieces: libsndfile's Vorbis writer crashes on one long block)
    with sf.SoundFile(os.path.join(OUT, name + ".ogg"), "w", SR, 1, format="OGG", subtype="VORBIS") as f:
        for i in range(0, len(x), SR):
            f.write(x[i:i + SR])
    print("wrote", name, f"{len(x) / SR:.1f}s")


def hz(noot):
    """MIDI note number -> frequency."""
    return 440.0 * 2 ** ((noot - 69) / 12)


def echo(x, tijden=(0.31, 0.47, 0.73), terug=0.42):
    """A long soft echo: three delay lines that feed back, darkened."""
    y = x.copy()
    for t in tijden:
        d = int(t * SR)
        nat = np.zeros_like(x)
        bron = x.copy()
        for _ in range(6):
            bron = low(np.concatenate([np.zeros(d), bron[:-d]]), 3200) * terug
            nat += bron
        y += nat * 0.5
    return y


def bel(noot, duur, hard=1.0):
    """A music-box / chime note: a clear tone with two soft overtones that die away sooner."""
    n = int(duur * SR)
    t = np.arange(n) / SR
    f = hz(noot)
    x = np.sin(2 * np.pi * f * t) * np.exp(-t / (duur * 0.28))
    x += 0.32 * np.sin(2 * np.pi * f * 2.0 * t) * np.exp(-t / (duur * 0.12))
    x += 0.14 * np.sin(2 * np.pi * f * 3.01 * t) * np.exp(-t / (duur * 0.07))
    aanzet = int(0.004 * SR)
    x[:aanzet] *= np.linspace(0, 1, aanzet)
    return x * hard


def vlak(noten, duur, r):
    """A pad chord: every note three slightly detuned sines, swelling in and out slowly."""
    n = int(duur * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for noot in noten:
        for ontstemd in (-0.06, 0.0, 0.07):
            fase = r.uniform(0, 6.283)
            x += np.sin(2 * np.pi * hz(noot + ontstemd) * t + fase) * (1 + 0.15 * np.sin(2 * np.pi * 0.21 * t + fase))
    env = np.minimum(1, t / 3.0) * np.minimum(1, (duur - t) / 3.5)
    return x * env / len(noten)


def muziek():
    r = np.random.default_rng(26100)
    maat = 8.0
    # C major pentatonic over four gentle chords, three times round
    akkoorden = [(48, 55, 64, 67), (45, 52, 60, 64), (41, 53, 57, 64), (43, 55, 59, 62)] * 3
    schaal = [72, 74, 76, 79, 81, 84, 86, 88]
    totaal = int((len(akkoorden) * maat + 6) * SR)
    pad = np.zeros(totaal)
    mel = np.zeros(totaal)
    for i, ak in enumerate(akkoorden):
        s = vlak(ak, maat + 3.0, r)
        a = int(i * maat * SR)
        pad[a:a + len(s)] += s[:totaal - a]
        # a few notes per bar, mostly on the chord's own notes, leaving room
        tel = 0.0
        vorige = 3
        while tel < maat - 0.5:
            if r.random() < (0.35 if i == 0 else 0.72):
                stap = int(np.clip(vorige + r.integers(-2, 3), 0, len(schaal) - 1))
                vorige = stap
                noot = bel(schaal[stap], 3.2, 0.5 + 0.5 * r.random())
                b = int((i * maat + tel) * SR)
                mel[b:b + len(noot)] += noot[:totaal - b]
            tel += r.choice([1.0, 1.0, 2.0, 1.5, 0.5])
    x = low(pad, 1800) * 0.9 + echo(mel) * 0.33
    uit = int(5 * SR)
    x[-uit:] *= np.linspace(1, 0, uit) ** 2
    return norm(x, 0.62)


def sfeer(duur=16.0):
    r = np.random.default_rng(26101)
    fade = int(1.2 * SR)
    n = int(duur * SR) + fade
    t = np.arange(n) / SR
    w = r.standard_normal(n + SR)
    # the swells are periodic over the loop, so the end runs into the start
    golf = 0.55 + 0.45 * np.sin(2 * np.pi * t / duur) * np.sin(2 * np.pi * t * 2 / duur + 0.9)
    wind = band(w, 180, 900)[SR:] * (0.7 + 0.3 * golf) + band(w, 900, 2600)[SR:] * 0.22 * np.clip(golf, 0, 1)
    x = wind / np.std(wind) * 0.22
    for (wanneer, noot, hard) in ((2.1, 91, 0.10), (2.5, 96, 0.07), (7.4, 88, 0.09), (11.3, 93, 0.08), (11.6, 98, 0.05)):
        b = np.concatenate([bel(noot, 2.6, hard * 2.2), np.zeros(int(2.4 * SR))])
        a = int(wanneer * SR)
        x[a:a + len(b)] += echo(b)[:n - a]
    ramp = np.linspace(0, 1, fade)
    x[:fade] = x[:fade] * np.sqrt(ramp) + x[-fade:] * np.sqrt(1 - ramp)
    return norm(x[:-fade], 0.30)


if __name__ == "__main__":
    if not os.path.isdir(os.path.join("src", "main", "resources")):
        raise SystemExit("run from the project root: python tools/features/bio_wereld_wolk_geluid.py")
    save("muziek", muziek())
    save("sfeer", sfeer())
