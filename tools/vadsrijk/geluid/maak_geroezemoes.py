"""
U-GEROEZEMOES: the crowd murmur beds of Het Vadsrijk ("geroezemoes"), made ONLY from the existing guh takes of the Guhs mod
(guh_ambient1..16, piep_ambient, the Guhxolotl blub). No synthesised voices: many little guhs, each with their own pitch and
distance, chatter in phrases with pauses (turn taking); the far ones are muffled (low-pass + more room), so it stays an
unintelligible soft bed. Now and then a clearer "njeg", a squeak or a blub is mixed in (the "druk" and "markt" beds).

    assets/guhs/sounds/vadsrijk/geroezemoes/bed_rustig.ogg   few guhs, far away      (quiet streets, night)
    assets/guhs/sounds/vadsrijk/geroezemoes/bed_druk.ogg     a busy street
    assets/guhs/sounds/vadsrijk/geroezemoes/bed_markt.ogg    a full market square

Each bed is an exact-length circular mix (every event wraps around, the room reverb too), so the loop has no seam.
Mono 32 kHz OGG, encoded at <= ~48 kbps like u-krimp. The client (geluid.client.GeroezemoesSpeler) crossfades the three.

Run from the project root:  python tools/vadsrijk/geluid/maak_geroezemoes.py
"""
import os
import sys

import numpy as np
import soundfile as sf
from scipy.signal import butter, lfilter, resample_poly, fftconvolve

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
from render import galm_ir  # noqa: E402

SR = 32000
LUS = 30.0
N = int(LUS * SR)
SND = os.path.join("src", "main", "resources", "assets", "guhs", "sounds")
OUT = os.path.join(SND, "vadsrijk", "geroezemoes")
MAX_KBPS = 50

# the bioscoop mapping of the takes: guh/vahoeg, oeh, njeg, lach
GUH = [3, 4, 7, 10, 11, 16, 2, 13, 14, 15]
OEH = [1, 8]
NJEG = [5, 9]
LACH = [6, 12]

# (voices, accents njeg/piep/blub/lach, rms dB, how far the crowd is: 0 near .. 1 far)
BEDDEN = {
    "rustig": dict(stemmen=6, njeg=0, piep=0, blub=0, lach=1, db=-31, ver=(0.55, 1.0), praat=0.35),
    "druk": dict(stemmen=13, njeg=1, piep=1, blub=0, lach=2, db=-30, ver=(0.4, 1.0), praat=0.5),
    "markt": dict(stemmen=22, njeg=2, piep=1, blub=1, lach=3, db=-29, ver=(0.3, 1.0), praat=0.6),
}


def filt(x, f, soort="low", order=2):
    if isinstance(f, (list, tuple)):
        b, a = butter(order, [f[0] / (SR / 2), f[1] / (SR / 2)], btype="band")
    else:
        b, a = butter(order, f / (SR / 2), btype=soort)
    return lfilter(b, a, x)


def lees(pad):
    x, sr = sf.read(pad, dtype="float64", always_2d=True)
    x = x.mean(axis=1)
    if sr != SR:
        g = np.gcd(sr, SR)
        x = resample_poly(x, SR // g, sr // g)
    x = filt(x, 120, "high")
    nz = np.nonzero(np.abs(x) > 0.02 * np.max(np.abs(x)))[0]
    if len(nz):
        x = x[max(0, nz[0] - 160): nz[-1] + 320]
    return x / (np.max(np.abs(x)) + 1e-9)


def lettergrepen(x, min_s=0.07):
    """Split a take into syllable-ish grains (on dips of the 10 ms envelope); short ones are dropped."""
    h = int(0.01 * SR)
    m = len(x) // h
    if m < 3:
        return [x]
    e = np.sqrt(np.mean(x[: m * h].reshape(m, h) ** 2, axis=1))
    aan = e > 0.12 * e.max()
    uit, start = [], None
    for i, a in enumerate(list(aan) + [False]):
        if a and start is None:
            start = i
        elif not a and start is not None:
            a0, a1 = max(0, start * h - h), min(len(x), i * h + 2 * h)
            if (a1 - a0) / SR >= min_s:
                uit.append(x[a0:a1])
            start = None
    return uit or [x]


def zacht(x, ms=6):
    n = min(len(x) // 2, int(ms / 1000 * SR))
    if n > 0:
        x = x.copy()
        x[:n] *= np.linspace(0, 1, n)
        x[-n:] *= np.linspace(1, 0, n)
    return x


def toon(x, f):
    """Pitch by resampling (faster + higher, the guh way): factor f > 1 = higher."""
    if abs(f - 1) < 0.005:
        return x
    p, q = int(round(1000 / f)), 1000
    return resample_poly(x, p, q)


def plak(buf, x, t):
    idx = (int(t * SR) + np.arange(len(x))) % len(buf)
    np.add.at(buf, idx, x)


def circ_galm(x, sec, nat, seed=4):
    y = fftconvolve(x, galm_ir(SR, sec, 4200, seed))
    wet = y[: len(x)].copy()
    staart = y[len(x):]
    for a in range(0, len(staart), len(x)):
        d = staart[a:a + len(x)]
        wet[: len(d)] += d
    return x + wet * nat


def cfilt(x, f, soort="low"):
    """filt on a circular buffer (it runs over a wrapped second first, so the loop point stays seamless)."""
    if len(x) != N:
        return filt(x, f, soort)
    return filt(np.concatenate([x[-SR:], x]), f, soort)[SR:]


def afstand(x, d):
    """d 0 (near) .. 1 (far): quieter and more muffled."""
    x = cfilt(x, 3800 - 2900 * d) if d > 0.05 else x
    return x * (1.0 / (1 + 3.2 * d))


class Takes:
    def __init__(self):
        self.take = {n: lees(os.path.join(SND, f"guh_ambient{n}.ogg")) for n in range(1, 17)}
        self.grein = []   # (take nr, grain)
        for n, x in self.take.items():
            if n in NJEG or n in LACH:
                continue   # the recognisable ones are kept for the accents
            for g in lettergrepen(x):
                if len(g) / SR <= 0.6:
                    self.grein.append((n, g))
        self.piep = [lees(os.path.join(SND, f"piep_ambient{n}.ogg")) for n in (1, 2, 3, 5)]
        self.blub = [lees(os.path.join(SND, "waterdiertjes", f"blub{n}.ogg")) for n in (1, 2, 3)]


def stem(r, t: Takes, praat):
    """One little guh: phrases of 2-6 syllables (own pitch), then a pause while the others talk."""
    uit = np.zeros(N)
    f0 = r.uniform(0.82, 1.38)
    tijd = r.uniform(0, LUS)
    eind = tijd + LUS - 0.5
    while tijd < eind:
        for _ in range(int(r.integers(2, 7))):
            _, g = t.grein[int(r.integers(0, len(t.grein)))]
            g = zacht(toon(g, f0 * r.uniform(0.95, 1.06)) * r.uniform(0.55, 1.0))
            plak(uit, g, tijd)
            tijd += len(g) / SR * r.uniform(0.7, 0.95) + r.uniform(0.0, 0.08)
        tijd += r.uniform(0.5, 2.6) / praat
    return uit


def accent(r, x, d, pitch):
    return afstand(zacht(toon(x, pitch)), d)


def bed(naam, seed):
    c = BEDDEN[naam]
    r = np.random.default_rng(seed)
    t = Takes()
    mix = np.zeros(N)
    for _ in range(c["stemmen"]):
        d = r.uniform(*c["ver"])
        mix += afstand(stem(r, t, c["praat"]), d)
    # accents: a clearer njeg / giggle / squeak / blub somewhere at mid distance (the live one-shots do the near ones)
    for _ in range(c["njeg"]):
        plak(mix, accent(r, t.take[NJEG[int(r.integers(0, 2))]], r.uniform(0.3, 0.45), r.uniform(0.95, 1.2)) * 0.9, r.uniform(0, LUS))
    for _ in range(c["lach"]):
        plak(mix, accent(r, t.take[LACH[int(r.integers(0, 2))]], r.uniform(0.45, 0.7), r.uniform(1.0, 1.25)) * 0.7, r.uniform(0, LUS))
    for _ in range(c["piep"]):
        plak(mix, accent(r, t.piep[int(r.integers(0, len(t.piep)))], r.uniform(0.35, 0.5), r.uniform(0.95, 1.1)) * 0.6, r.uniform(0, LUS))
    for _ in range(c["blub"]):
        plak(mix, accent(r, t.blub[int(r.integers(0, len(t.blub)))], r.uniform(0.35, 0.5), r.uniform(0.9, 1.1)) * 0.6, r.uniform(0, LUS))
    # a faint room tone of shuffling feet (low, soft), circular by construction (noise is filtered on a wrapped copy)
    vloer = r.standard_normal(N + SR)
    vloer = filt(filt(vloer, (180, 900)), 900)[SR:]
    mix += vloer / (np.max(np.abs(vloer)) + 1e-9) * 0.015 * c["stemmen"] ** 0.5
    mix = circ_galm(mix, 1.3, 0.55)
    mix = cfilt(mix, 6500)
    mix -= np.mean(mix)
    rms = np.sqrt(np.mean(mix ** 2)) + 1e-12
    mix *= 10 ** (c["db"] / 20) / rms
    return np.tanh(mix / 0.7) * 0.7


def bewaar(pad, x):
    """Vorbis, the first quality level that lands on <= MAX_KBPS (the u-krimp rule)."""
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    x = np.clip(x, -0.98, 0.98).astype(np.float32)
    for niveau in (0.5, 0.6, 0.7, 0.8, 0.85, 0.9, 0.95, 0.97, 1.0):
        with sf.SoundFile(pad, "w", SR, 1, format="OGG", subtype="VORBIS", compression_level=niveau) as f:
            for a in range(0, len(x), SR // 2):
                f.write(x[a:a + SR // 2])
        kbps = os.path.getsize(pad) * 8 / 1000 / LUS
        if kbps <= MAX_KBPS:
            break
    return os.path.getsize(pad), kbps


def main(alleen=()):
    totaal = 0
    for i, naam in enumerate(BEDDEN):
        if alleen and naam not in alleen:
            continue
        x = bed(naam, 7100 + i)
        naad = abs(float(x[0]) - float(x[-1]))
        grootte, kbps = bewaar(os.path.join(OUT, f"bed_{naam}.ogg"), x)
        totaal += grootte
        print(f"  bed_{naam}: {grootte // 1024} kB, {kbps:.0f} kbps, piek {np.max(np.abs(x)):.2f}, naad {naad:.4f}")
    print(f"geroezemoes: {totaal / 1024 / 1024:.2f} MB")


if __name__ == "__main__":
    main(tuple(sys.argv[1:]))
