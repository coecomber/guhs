"""
T-GELUID: the ambience of Het Vadsrijk (roadmap §5.3): seamless bed loops per area kind and short one-shots.
Synthesised with numpy (birds, crickets, drips, bees, water, leaves) plus a few SoundFont effects (stream, seashore, wind chimes)
from the same GeneralUser GS as the music (sf2.py). No strong wind anywhere (user wish): leaves are a soft high rustle only.

    assets/guhs/sounds/vadsrijk/sfeer/bed_<naam>.ogg    22 s seamless loops (mono 32 kHz)
    assets/guhs/sounds/vadsrijk/sfeer/<soort>_<n>.ogg   one-shots: vogel, druppel, golf, bij, kraak, glinster, krekel

Run from the project root:  python tools/vadsrijk/geluid/maak_sfeer.py
"""
import os
import sys

import numpy as np
from scipy.signal import butter, fftconvolve, lfilter

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
from render import bewaar_ogg, galm_ir  # noqa: E402

SR = 32000
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "vadsrijk", "sfeer")
LUS = 22.0


def filt(x, f, soort="low", order=2):
    if isinstance(f, (list, tuple)):
        b, a = butter(order, [f[0] / (SR / 2), f[1] / (SR / 2)], btype="band")
    else:
        b, a = butter(order, f / (SR / 2), btype=soort)
    return lfilter(b, a, x)


def ruis(r, n, kleur="wit"):
    x = r.standard_normal(n)
    if kleur == "roze":
        x = lfilter([0.049922035, -0.095993537, 0.050612699, -0.004408786], [1, -2.494956002, 2.017265875, -0.522189400], x)
    elif kleur == "bruin":
        x = lfilter([1], [1, -0.995], x)
        x = filt(x, 20, "high")
    return x / (np.max(np.abs(x)) + 1e-9)


def env(n, a, d):
    t = np.arange(n) / SR
    return np.minimum(1, t / max(a, 1e-4)) * np.exp(-t / max(d, 1e-4))


def plak(buf, x, t, gain=1.0, lus=True):
    a = int(t * SR) % len(buf) if lus else int(t * SR)
    n = len(x)
    if lus:
        idx = (a + np.arange(n)) % len(buf)
        np.add.at(buf, idx, x * gain)
    else:
        b = min(len(buf), a + n)
        buf[a:b] += x[: b - a] * gain


def naadloos(x, kruis=2.0):
    """Make a loop seamless: crossfade the extra tail into the head."""
    k = int(kruis * SR)
    n = int(LUS * SR)
    y = x[:n].copy()
    staart = x[n:n + k]
    w = np.linspace(0, 1, len(staart))
    y[:len(staart)] = y[:len(staart)] * w + staart * (1 - w)
    return y


def galm(x, sec=1.6, nat=0.35, seed=3, staart=True):
    """Reverb; staart=True lets the tail ring out (one-shots), False keeps the length (continuous layers)."""
    ir = galm_ir(SR, sec, 5000, seed)
    if staart:
        x = np.concatenate([x, np.zeros(int(sec * SR))])
    return x + fftconvolve(x, ir)[: len(x)] * nat


def niveau(x, db):
    x = x - np.mean(x)
    r = np.sqrt(np.mean(x ** 2)) + 1e-12
    x = x * (10 ** (db / 20) / r)
    return np.tanh(x / 0.9) * 0.9


# ---------------------------------------------------------------- sound atoms

def tjilp(r, basis=None):
    """A sweet little bird: 2-6 quick chirps (sine with a fast pitch sweep + a soft 2nd harmonic)."""
    basis = basis or r.uniform(2800, 4800)
    stukken = []
    for _ in range(r.integers(2, 7)):
        d = r.uniform(0.04, 0.11)
        n = int(d * SR)
        t = np.arange(n) / SR
        op = r.choice([1, -1, 1])
        f = basis * (1 + op * r.uniform(0.15, 0.45) * (t / d)) * (1 + 0.04 * np.sin(2 * np.pi * 60 * t))
        ph = 2 * np.pi * np.cumsum(f) / SR
        x = (np.sin(ph) + 0.18 * np.sin(2 * ph)) * np.clip(np.sin(np.pi * t / d), 0, None) ** 1.5
        stukken.append(x)
        stukken.append(np.zeros(int(r.uniform(0.03, 0.12) * SR)))
        basis *= r.uniform(0.92, 1.08)
    return np.concatenate(stukken)


def triller(r):
    """A trill: many very short chirps."""
    basis = r.uniform(3200, 5200)
    n = int(r.uniform(0.5, 1.1) * SR)
    t = np.arange(n) / SR
    f = basis * (1 + 0.12 * np.sin(2 * np.pi * r.uniform(18, 28) * t)) * (1 - 0.15 * t)
    ph = 2 * np.pi * np.cumsum(f) / SR
    am = (0.5 + 0.5 * np.sin(2 * np.pi * r.uniform(18, 28) * t)) ** 2
    return np.sin(ph) * am * np.clip(np.sin(np.pi * t / t[-1]), 0, None) ** 0.7


def druppel(r):
    """A water drop: a bubble resonance (sine gliding up) with a fast decay."""
    d = r.uniform(0.05, 0.12)
    n = int(d * SR)
    t = np.arange(n) / SR
    f0 = r.uniform(700, 1400)
    f = f0 * (1 + r.uniform(1.2, 2.2) * (t / d) ** 0.6)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / (d * 0.35))
    x[: int(0.002 * SR)] *= np.linspace(0, 1, int(0.002 * SR))
    return x


def krekel(r, f=None, groepen=None):
    """A cricket: groups of 3-4 pulses of a ~4.5 kHz tone."""
    f = f or r.uniform(4200, 5000)
    stuk = []
    for _ in range(groepen or r.integers(3, 7)):
        for _ in range(r.integers(3, 5)):
            n = int(0.018 * SR)
            t = np.arange(n) / SR
            stuk.append(np.sin(2 * np.pi * f * t) * np.sin(np.pi * t / t[-1]))
            stuk.append(np.zeros(int(0.014 * SR)))
        stuk.append(np.zeros(int(r.uniform(0.25, 0.5) * SR)))
    return np.concatenate(stuk)


def bij(r, sec=None):
    """A bee flying by: a buzzy saw-ish tone with vibrato, swelling in and out."""
    sec = sec or r.uniform(1.2, 2.6)
    n = int(sec * SR)
    t = np.arange(n) / SR
    f = r.uniform(190, 260) * (1 + 0.03 * np.sin(2 * np.pi * r.uniform(5, 9) * t) + 0.05 * (t / sec - 0.5))
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = sum(np.sin(k * ph) / k for k in range(1, 9))
    x = filt(x, 1800) * (0.6 + 0.4 * np.sin(2 * np.pi * 30 * t))
    return x * np.clip(np.sin(np.pi * t / sec), 0, None) ** 1.3


def golf(r):
    """One wave: a pink-noise swell, then the hiss of the backwash."""
    sec = r.uniform(3.0, 4.5)
    n = int(sec * SR)
    t = np.arange(n) / SR
    x = ruis(r, n, "roze")
    env1 = np.sin(np.pi * np.minimum(1, t / (sec * 0.45))) ** 2 * np.exp(-np.maximum(0, t - sec * 0.45) / (sec * 0.3))
    lage = filt(x, 900) * env1
    hoge = filt(x, (1800, 7000), order=2) * np.exp(-np.maximum(0, t - sec * 0.35) / (sec * 0.25)) * (t > sec * 0.3)
    return lage * 1.0 + hoge * 0.5


def kraak(r):
    """A soft wooden creak (a branch, a rope): a rough, slowly gliding low tone."""
    sec = r.uniform(0.5, 1.0)
    n = int(sec * SR)
    t = np.arange(n) / SR
    f = r.uniform(140, 220) * (1 + 0.25 * t / sec)
    puls = (np.sin(2 * np.pi * np.cumsum(f) / SR) > 0.7).astype(float)
    x = filt(puls - np.mean(puls), (300, 2500), order=2)
    return x * np.sin(np.pi * t / sec)


def glinster(r, sf=None):
    """A magical shimmer: a few high celesta/glockenspiel notes (from the SoundFont) of a sweet chord."""
    akk = r.choice([[84, 88, 91, 95], [86, 89, 93, 96], [81, 85, 88, 93], [83, 86, 90, 95]])
    stem = sf.stem(0, 8 if r.random() < 0.5 else 9)
    buf = np.zeros(int(2.5 * SR))
    for i, k in enumerate(r.permutation(akk)):
        x = stem.note(int(k), int(r.integers(40, 70)), 0.4, SR)
        plak(buf, x, i * r.uniform(0.07, 0.16), 1.0, lus=False)
    return buf


def sf_bed(sf, bank, prog, key, sec, vel=90):
    stem = sf.stem(bank, prog)
    return stem.note(key, vel, sec, SR, release_max=1.0)


# ---------------------------------------------------------------- beds

def bladeren(r, n, sterkte=0.12):
    """Leaves: soft, high rustle in little swells (no low wind roar)."""
    x = filt(ruis(r, n, "roze"), (2500, 8000))
    t = np.arange(n) / SR
    m = 0.35 + 0.65 * np.clip(np.sin(2 * np.pi * (t / r.uniform(5, 9)) + r.uniform(0, 6)) * 0.5 + 0.5, 0, 1) ** 2
    return x * m * sterkte


def vogels(r, buf, aantal, gain=0.5):
    for _ in range(aantal):
        x = tjilp(r) if r.random() < 0.75 else triller(r)
        plak(buf, galm(x, 0.8, 0.2) * r.uniform(0.3, 1.0), r.uniform(0, LUS), gain)


class Bed:
    """A bed under construction: continuous layers (n + 2.5 s, crossfaded at the end) and events (circular, exactly LUS)."""

    def __init__(self):
        self.n = int((LUS + 2.5) * SR)
        self.cont = np.zeros(self.n)
        self.ev = np.zeros(int(LUS * SR))

    def __iadd__(self, x):
        self.cont[: len(x)] += x[: self.n]
        return self


def circ_galm(x, sec, nat):
    y = fftconvolve(x, galm_ir(SR, sec, 5000, 4))
    wet = y[: len(x)].copy()
    staart = y[len(x):]
    for a in range(0, len(staart), len(x)):
        d = staart[a:a + len(x)]
        wet[: len(d)] += d
    return x + wet * nat


def bed(naam, r, sf):
    b = Bed()
    n = b.n
    if naam == "weide":
        b += bladeren(r, n, 0.08)
        vogels(r, b.ev, 14, 0.45)
        db = -32
    elif naam == "bos":
        b += bladeren(r, n, 0.12)
        vogels(r, b.ev, 18, 0.4)
        for _ in range(2):
            plak(b.ev, kraak(r), r.uniform(0, LUS), 0.12)
        db = -31
    elif naam == "sporen":
        b += bladeren(r, n, 0.05)
        vogels(r, b.ev, 6, 0.3)
        for _ in range(5):
            plak(b.ev, glinster(r, sf), r.uniform(0, LUS), 0.25)
        t = np.arange(n) / SR
        b += (np.sin(2 * np.pi * 220 * t) + 0.5 * np.sin(2 * np.pi * 330 * t)) * 0.008 * (1 + 0.5 * np.sin(2 * np.pi * t / 11))
        db = -33
    elif naam in ("grot", "wortel"):
        b += filt(ruis(r, n, "bruin"), 180) * 0.25
        for _ in range(16 if naam == "grot" else 11):
            plak(b.ev, druppel(r), r.uniform(0, LUS), r.uniform(0.25, 0.7))
        b.cont = galm(b.cont, 2.8 if naam == "grot" else 3.6, 0.6, staart=False)
        b.ev = circ_galm(b.ev, 2.8 if naam == "grot" else 3.6, 0.6)
        db = -33
    elif naam == "tempel":
        t = np.arange(n) / SR
        for f, g in ((110, 1.0), (110.4, 0.8), (165, 0.5), (220.3, 0.35)):
            b += np.sin(2 * np.pi * f * t) * g * 0.02
        b.cont *= 1 + 0.3 * np.sin(2 * np.pi * t / 7.3)
        for _ in range(5):
            plak(b.ev, druppel(r), r.uniform(0, LUS), 0.3)
        b.cont = galm(b.cont, 3.5, 0.6, staart=False)
        b.ev = circ_galm(b.ev, 3.5, 0.6)
        db = -34
    elif naam == "meer":
        x = filt(ruis(r, n, "roze"), (200, 1200))
        t = np.arange(n) / SR
        golfjes = np.clip(np.sin(2 * np.pi * t / 2.7) + 0.6 * np.sin(2 * np.pi * t / 1.9 + 1), 0, None) ** 2
        b += x * golfjes * 0.25
        for _ in range(10):
            plak(b.ev, druppel(r) * 0.5, r.uniform(0, LUS), 0.2)
        vogels(r, b.ev, 8, 0.35)
        db = -31
    elif naam == "berg":
        b += bladeren(r, n, 0.035)
        st = sf_bed(sf, 4, 122, 60, LUS + 2.5, 70)
        b += filt(st, 2500)[: n] * 0.25
        stem = sf.stem(5, 124)
        for _ in range(4):
            x = stem.note(int(r.integers(60, 84)), 60, 1.0, SR)
            if x is not None:
                plak(b.ev, x, r.uniform(0, LUS), 0.35)
        vogels(r, b.ev, 5, 0.3)
        db = -33
    elif naam == "kloof":
        st = sf_bed(sf, 4, 122, 60, LUS + 2.5, 95)
        b += st[:n]
        for _ in range(8):
            plak(b.ev, druppel(r), r.uniform(0, LUS), 0.3)
        vogels(r, b.ev, 4, 0.25)
        db = -30
    elif naam == "kruin":
        b += bladeren(r, n, 0.14)
        vogels(r, b.ev, 16, 0.45)
        for _ in range(2):
            plak(b.ev, kraak(r), r.uniform(0, LUS), 0.1)
        db = -31
    elif naam == "honing":
        for _ in range(9):
            plak(b.ev, bij(r, r.uniform(2, 5)), r.uniform(0, LUS), r.uniform(0.3, 0.7))
        b += bladeren(r, n, 0.04)
        vogels(r, b.ev, 5, 0.3)
        db = -31
    elif naam == "kust":
        for i in range(7):
            plak(b.ev, golf(r), i * (LUS / 7) + r.uniform(-0.5, 0.5), r.uniform(0.6, 1.0))
        st = sf_bed(sf, 0, 122, 60, LUS + 2.5, 70)
        b += st[:n] * 0.3
        db = -30
    elif naam == "krekels":
        for f in (4300, 4550, 4800, 5100, 4650):
            g = r.uniform(0.2, 0.5)
            t0 = r.uniform(0, 2)
            while t0 < LUS + 2:
                x = krekel(r, f * r.uniform(0.99, 1.01), r.integers(4, 9))
                plak(b.ev, x, t0, g)
                t0 += len(x) / SR + r.uniform(0.5, 3)
        b.cont = galm(b.cont, 1.2, 0.25, staart=False)
        b.ev = circ_galm(b.ev, 1.2, 0.25)
        db = -35
    elif naam in ZIJDE_BEDDEN:
        db = zijde_bed(naam, b, r, sf)
    else:
        raise ValueError(naam)
    return niveau(naadloos(b.cont) + b.ev, db)


# ---------------------------------------------------------------- U-Z6MUZIEK: het Zijdeland (zone 6)
# The user dislikes strong wind: the dunes only get a very soft, high sand hiss in slow swells, never a roar.

def belletje(r):
    """A bultguh (camel) bell far away: a small clanky bell (inharmonic partials), struck once or twice by the swaying walk."""
    f0 = r.uniform(520, 760)
    uit = np.zeros(int(2.2 * SR))
    for slag in range(int(r.integers(1, 3))):
        n = int(1.4 * SR)
        t = np.arange(n) / SR
        x = sum(g * np.sin(2 * np.pi * f0 * m * t + r.uniform(0, 6)) * np.exp(-t / (0.5 / m ** 0.6))
                for m, g in ((1, 1.0), (2.32, 0.5), (3.91, 0.3), (5.4, 0.15)))
        x[: int(0.002 * SR)] *= np.linspace(0, 1, int(0.002 * SR))
        plak(uit, x * (1.0 if slag == 0 else 0.6), slag * r.uniform(0.35, 0.6), 1.0, lus=False)
    return uit


def sterretje(r, sf):
    """A star twinkle: two or three very high tinker-bell / celesta notes of a sweet chord."""
    akk = [[93, 97, 100], [95, 98, 102], [91, 95, 98, 103]][int(r.integers(0, 3))]
    stem = sf.stem(0, 112 if r.random() < 0.5 else 8)
    buf = np.zeros(int(2.2 * SR))
    for i, k in enumerate(r.permutation(akk)):
        x = stem.note(int(k), int(r.integers(34, 56)), 0.3, SR)
        if x is not None:
            plak(buf, x, i * r.uniform(0.1, 0.22), 1.0, lus=False)
    return buf


def zoutzang(r):
    """A singing salt crystal: a pure glassy tone with a slow beat (two close sines + a soft fifth), swelling in and out."""
    sec = r.uniform(1.4, 2.4)
    n = int(sec * SR)
    t = np.arange(n) / SR
    f = [880, 987.8, 1046.5, 1174.7, 1318.5][int(r.integers(0, 5))]
    x = np.sin(2 * np.pi * f * t) + np.sin(2 * np.pi * (f + r.uniform(1.5, 3.5)) * t) + 0.25 * np.sin(2 * np.pi * f * 1.5 * t)
    return x * np.sin(np.pi * t / sec) ** 2


def zandruis(r, n, sterkte):
    """Sand hiss: high, soft, in slow swells (a gentle breath over the dunes, no low roar)."""
    x = filt(ruis(r, n, "roze"), (3000, 9000))
    t = np.arange(n) / SR
    m = 0.25 + 0.75 * np.clip(np.sin(2 * np.pi * (t / r.uniform(8, 12)) + r.uniform(0, 6)) * 0.5 + 0.5, 0, 1) ** 3
    return x * m * sterkte


def stem_murmel(r, n):
    """One far-away voice babbling: a soft voiced buzz (pitch 140-320 Hz) through a vowel filter that changes per syllable."""
    uit = np.zeros(n)
    t = 0.0
    sec = n / SR
    f0 = r.uniform(140, 320)
    while t < sec - 0.3:
        if r.random() < 0.25:
            t += r.uniform(0.3, 1.5)   # a pause between words
            continue
        d = r.uniform(0.09, 0.22)
        m = int(d * SR)
        tt = np.arange(m) / SR
        f = f0 * (1 + 0.12 * r.standard_normal()) * (1 + 0.08 * np.sin(np.pi * tt / d))
        ph = 2 * np.pi * np.cumsum(f) / SR
        x = sum(np.sin(k * ph) / k for k in range(1, 12))
        lo = [300, 450, 650, 800][int(r.integers(0, 4))]
        x = filt(x, (lo, lo * r.uniform(2.2, 3.5)), order=1) * np.sin(np.pi * tt / d) ** 1.5
        a = int(t * SR)
        b = min(n, a + m)
        uit[a:b] += x[: b - a]
        t += d * r.uniform(0.85, 1.1)
    return uit


def zijde_bed(naam, b, r, sf):
    n = b.n
    t = np.arange(n) / SR
    if naam == "duinen":
        b += zandruis(r, n, 0.05)
        b += filt(ruis(r, n, "bruin"), 260) * 0.04 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 11.0))   # a faint warm breath
        for _ in range(3):
            plak(b.ev, galm(belletje(r), 1.6, 0.4), r.uniform(0, LUS), r.uniform(0.08, 0.16))
        vogels(r, b.ev, 2, 0.15)
        return -36
    if naam == "oase":
        x = filt(ruis(r, n, "roze"), (200, 1100))
        golfjes = np.clip(np.sin(2 * np.pi * t / 3.1) + 0.6 * np.sin(2 * np.pi * t / 2.2 + 1), 0, None) ** 2
        b += x * golfjes * 0.22
        b += bladeren(r, n, 0.03)
        for _ in range(12):
            plak(b.ev, druppel(r) * 0.5, r.uniform(0, LUS), 0.2)
        vogels(r, b.ev, 10, 0.35)
        return -32
    if naam == "bazaar":
        stemmen = sum(stem_murmel(r, n) * r.uniform(0.4, 1.0) for _ in range(14))
        stemmen = filt(stemmen, 2400) * 0.12
        b += galm(stemmen, 1.4, 0.5, staart=False)
        for _ in range(6):   # pots and cups clinking somewhere
            m = int(0.25 * SR)
            tt = np.arange(m) / SR
            f = r.uniform(1800, 3200)
            x = (np.sin(2 * np.pi * f * tt) + 0.5 * np.sin(2 * np.pi * f * 2.7 * tt)) * np.exp(-tt / 0.05)
            plak(b.ev, galm(x, 1.0, 0.3), r.uniform(0, LUS), r.uniform(0.05, 0.12))
        for _ in range(2):
            plak(b.ev, galm(belletje(r), 1.4, 0.4), r.uniform(0, LUS), 0.08)
        return -32
    if naam == "sterren":
        for f in (4400, 4700, 5000):
            g = r.uniform(0.15, 0.35)
            t0 = r.uniform(0, 2)
            while t0 < LUS + 2:
                x = krekel(r, f * r.uniform(0.99, 1.01), int(r.integers(3, 7)))
                plak(b.ev, x, t0, g)
                t0 += len(x) / SR + r.uniform(1.0, 4.0)
        for _ in range(4):
            plak(b.ev, galm(sterretje(r, sf), 2.0, 0.5), r.uniform(0, LUS), 0.22)
        b += zandruis(r, n, 0.015)
        b.cont = galm(b.cont, 1.4, 0.3, staart=False)
        b.ev = circ_galm(b.ev, 1.6, 0.35)
        return -36
    if naam == "toren":
        # the grey tower: a low sucking drone that breathes inwards (soft; it is the only grey spot of the zone)
        zuig = 0.5 + 0.5 * np.sin(2 * np.pi * t / 5.5) ** 2
        for f, g in ((55, 1.0), (55.3, 0.7), (82.4, 0.4), (110.2, 0.2)):
            b += np.sin(2 * np.pi * f * t * (1 - 0.004 * zuig)) * g * 0.03 * zuig
        b += filt(ruis(r, n, "roze"), (400, 1600)) * 0.025 * zuig ** 2   # the inhale
        b.cont = galm(b.cont, 3.0, 0.5, staart=False)
        return -35
    raise ValueError(naam)


ZIJDE_BEDDEN = ["duinen", "oase", "bazaar", "sterren", "toren"]


LOS = {
    "vogel": lambda r, sf: galm(tjilp(r) if r.random() < 0.6 else triller(r), 0.8, 0.25),
    "druppel": lambda r, sf: galm(druppel(r), 2.2, 0.6),
    "golf": lambda r, sf: golf(r),
    "bij": lambda r, sf: bij(r),
    "kraak": lambda r, sf: galm(kraak(r), 0.8, 0.2),
    "glinster": lambda r, sf: galm(glinster(r, sf), 1.6, 0.4),
    "krekel": lambda r, sf: galm(krekel(r), 0.8, 0.2),
}
AANTAL = {"vogel": 4, "druppel": 4, "golf": 2, "bij": 2, "kraak": 2, "glinster": 3, "krekel": 2}
BEDDEN = ["weide", "bos", "sporen", "grot", "tempel", "meer", "berg", "kloof", "wortel", "kruin", "honing", "kust", "krekels"]
# U-Z6MUZIEK: the Zijdeland beds + one-shots (appended, so the old seeds stay the same)
BEDDEN += ZIJDE_BEDDEN
LOS.update({
    "belletje": lambda r, sf: galm(belletje(r), 1.6, 0.4),
    "sterretje": lambda r, sf: galm(sterretje(r, sf), 2.0, 0.5),
    "zoutzang": lambda r, sf: galm(zoutzang(r), 2.4, 0.5),
})
AANTAL.update({"belletje": 3, "sterretje": 3, "zoutzang": 3})


def main(alleen=()):
    """alleen (U-Z6MUZIEK): only these beds / one-shot kinds, e.g. the Zijdeland ones; default everything."""
    from maak_muziek import sf2_pad
    from sf2 import SF2
    sf = SF2(sf2_pad())
    os.makedirs(OUT, exist_ok=True)
    totaal = 0
    for i, b in enumerate(BEDDEN):
        if alleen and b not in alleen:
            continue
        x = bed(b, np.random.default_rng(500 + i), sf)
        totaal += bewaar_ogg(os.path.join(OUT, f"bed_{b}.ogg"), x, SR, 0.55)
    for j, (soort, maak) in enumerate(LOS.items()):
        if alleen and soort not in alleen:
            continue
        for k in range(1, AANTAL[soort] + 1):
            r = np.random.default_rng(900 + j * 10 + k)
            x = maak(r, sf)
            x = x[: int(np.nonzero(np.abs(x) > 1e-4)[0][-1]) + 400]
            x = np.tanh(x / (np.max(np.abs(x)) + 1e-9) * 0.7 / 0.9) * 0.9
            n = int(0.004 * SR)
            x[:n] *= np.linspace(0, 1, n)
            x[-n:] *= np.linspace(1, 0, n)
            totaal += bewaar_ogg(os.path.join(OUT, f"{soort}_{k}.ogg"), x, SR, 0.5)
    print(f"ambience: {len(BEDDEN)} beds + {sum(AANTAL.values())} one-shots, {totaal // 1024} kB")


if __name__ == "__main__":
    main(tuple(sys.argv[1:]))
