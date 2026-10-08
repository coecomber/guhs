"""
biomes3 wereld, the Klaterdal: its music and ambience, synthesised (numpy) and written ONCE into
assets/guhs/sounds/klaterdal/ (vorbis encoding is not byte-for-byte stable, so an existing file is never rewritten; delete
it to make it again):

  muziek.ogg       the valley's own calm music (about 100 s): a koto-like plucked string (a sharp pluck, a bright ring that
                   dies away, a little bend into some notes) playing slow phrases in the "in" scale with long rests, over a
                   very soft bamboo-flute line and low drone notes. Slow, sparse, never loud.
  sfeer.ogg        the ambience loop (16 s, seamless): the river babbling over stones (soft filtered water noise with many
                   small "bloop" bubbles) and a breath of wind
  windgong1-3.ogg  a wind chime: a few glass-bright notes of the same scale ringing into each other, each different

Called from bio_wereld_dal.build (schrijf). Needs numpy, scipy and PyAV, like the mod's other sound scripts.
"""
import os

import numpy as np
try:                                    # (only needed to MAKE the sounds; the wiki build runs without them)
    from scipy.signal import butter, lfilter
except ImportError:
    butter = lfilter = None

SR = 44100
# the "in" scale on D (D Eb G A Bb), two and a half octaves: calm, a little wistful, unmistakably Japanese
TOON = {"D3": 146.83, "A3": 220.0, "Bb3": 233.08, "D4": 293.66, "Eb4": 311.13, "G4": 392.0, "A4": 440.0, "Bb4": 466.16, "D5": 587.33, "Eb5": 622.25,
        "G5": 783.99, "A5": 880.0, "Bb5": 932.33, "D6": 1174.66}


def _t(sec):
    return np.arange(int(SR * sec)) / SR


def _plak(out, geluid, start, volume):
    s = int(start * SR)
    if s >= len(out) or s < 0:
        return
    e = min(len(out), s + len(geluid))
    out[s:e] += geluid[:e - s] * volume


def _laag(x, f):
    b, a = butter(2, f / (SR / 2), btype="low")
    return lfilter(b, a, x)


def _koto(f, lengte=3.2, buig=0.0, hard=1.0):
    """A plucked koto string: a short bright pluck, harmonics that die away the faster the higher they are, a slightly
    stretched tuning (a stiff string), and optionally a bend up into the note (the left hand pressing the string)."""
    t = _t(lengte)
    toon = f * (1 - buig * np.exp(-t * 9))
    fase = 2 * np.pi * np.cumsum(toon) / SR
    s = np.zeros_like(t)
    for k, a in ((1, 1.0), (2, 0.62), (3, 0.40), (4, 0.26), (5, 0.17), (6, 0.10), (7, 0.06), (9, 0.03)):
        s += a * np.sin(fase * k * (1 + 0.0006 * k * k)) * np.exp(-t * (1.1 + 0.55 * k))
    pluk = np.random.default_rng(int(f * 10)).normal(0, 1, len(t)) * np.exp(-t * 160) * 0.35 * hard
    return (s + pluk) * np.minimum(1, t / 0.0015)


def _fluit(f, lengte):
    """A soft bamboo flute: a breathy sine with a slow vibrato that swells in and out."""
    t = _t(lengte)
    rng = np.random.default_rng(int(f))
    vib = 1 + 0.004 * np.sin(2 * np.pi * 4.6 * t) * np.minimum(1, t / 0.8)
    s = np.sin(2 * np.pi * f * vib * t) + 0.18 * np.sin(2 * np.pi * 2 * f * vib * t) + 0.05 * np.sin(2 * np.pi * 3 * f * vib * t)
    adem = _laag(rng.normal(0, 1, len(t)), 2600) * 0.10
    env = np.minimum(1, t / 0.5) * np.minimum(1, (lengte - t) / 0.9)
    return (s + adem) * env


def _galm(x, wachttijden=((0.137, 0.30), (0.211, 0.22), (0.293, 0.16), (0.419, 0.10))):
    """A small room: a few soft echoes."""
    y = x.copy()
    for sec, v in wachttijden:
        n = int(sec * SR)
        y[n:] += x[:-n] * v
    return y


def muziek():
    lengte = 100.0
    out = np.zeros(int(SR * lengte))
    # phrases: (time within the phrase, note, loudness, bend)
    zin_a = [(0.0, "D4", 1.0, 0), (1.2, "A4", 0.8, 0), (2.1, "Bb4", 0.9, 0.03), (3.6, "A4", 0.7, 0), (4.4, "G4", 0.8, 0), (6.0, "D4", 0.9, 0)]
    zin_b = [(0.0, "A4", 0.9, 0), (0.9, "D5", 1.0, 0), (2.4, "Eb5", 0.8, 0.04), (3.3, "D5", 0.8, 0), (4.5, "Bb4", 0.7, 0), (5.4, "A4", 0.9, 0),
             (7.2, "G4", 0.6, 0), (8.1, "A4", 0.8, 0)]
    zin_c = [(0.0, "G4", 0.8, 0), (0.75, "A4", 0.7, 0), (1.5, "D5", 1.0, 0), (3.3, "A4", 0.7, 0), (4.2, "Bb4", 0.8, 0.03), (5.7, "G4", 0.7, 0),
             (6.6, "Eb4", 0.8, 0.04), (7.8, "D4", 1.0, 0)]
    zin_d = [(0.0, "D5", 0.9, 0), (0.6, "G5", 0.8, 0), (1.2, "A5", 0.9, 0), (2.7, "G5", 0.6, 0), (3.3, "D5", 0.8, 0), (4.8, "Eb5", 0.7, 0.04),
             (5.7, "D5", 0.8, 0), (7.5, "A4", 0.9, 0)]
    # a run of quick soft notes down the strings (the koto's "sararin"), used twice
    loopje = [(i * 0.11, n, 0.45, 0) for i, n in enumerate(("D6", "Bb5", "A5", "G5", "Eb5", "D5", "Bb4", "A4"))]
    slot = [(0.0, "A4", 0.8, 0), (1.5, "G4", 0.7, 0), (3.0, "D4", 1.0, 0), (3.05, "A3", 0.6, 0), (6.0, "D4", 0.5, 0)]
    plan = [(2.0, zin_a), (12.5, zin_b), (25.0, zin_c), (36.5, loopje), (40.0, zin_d), (52.0, zin_a), (61.5, zin_c), (73.0, loopje), (76.0, zin_b),
            (88.0, slot)]
    for start, zin in plan:
        for tijd, noot, hard, buig in zin:
            _plak(out, _koto(TOON[noot], buig=buig, hard=hard), start + tijd, 0.20 * hard)
    # low strings under the turns of the phrases
    for tijd, noot in ((2.0, "D3"), (12.5, "A3"), (25.0, "D3"), (40.0, "D3"), (52.0, "D3"), (61.5, "A3"), (76.0, "D3"), (88.0, "D3")):
        _plak(out, _koto(TOON[noot], lengte=5.0, hard=0.5), tijd, 0.16)
    # the flute, far away
    for tijd, noot, duur in ((8.5, "A4", 3.6), (20.0, "D5", 4.2), (33.0, "Bb4", 3.0), (47.5, "A4", 4.0), (68.0, "D5", 4.4), (83.5, "G4", 3.4)):
        _plak(out, _fluit(TOON[noot], duur), tijd, 0.045)
    out = _galm(out)
    n = int(SR * 1.5)
    out[:n] *= np.linspace(0, 1, n)
    out[-int(SR * 5):] *= np.linspace(1, 0, int(SR * 5))
    return out / max(1e-9, np.max(np.abs(out))) * 0.55


def sfeer():
    lengte, overlap = 16.0, 2.0
    rng = np.random.default_rng(20261008)
    n = int(SR * (lengte + overlap))
    t = np.arange(n) / SR
    ruis = rng.normal(0, 1, n)
    # the river: band-limited water noise that swells a little
    water = (_laag(ruis, 2400) - _laag(ruis, 420)) * (0.75 + 0.25 * np.sin(2 * np.pi * t / 5.3) * np.sin(2 * np.pi * t / 3.1 + 1))
    out = water * 0.5
    # the babble: many small bubbles, each a short sine that slides up
    for _ in range(int(lengte * 13)):
        start = rng.uniform(0, lengte + overlap - 0.2)
        f0 = rng.uniform(380, 1500)
        duur = rng.uniform(0.035, 0.11)
        tt = _t(duur)
        bel = np.sin(2 * np.pi * (f0 * tt + f0 * 0.9 * tt * tt / duur)) * np.sin(np.pi * tt / duur) ** 2
        _plak(out, bel, start, rng.uniform(0.04, 0.16))
    # a breath of wind underneath
    out += _laag(rng.normal(0, 1, n), 260) * (0.5 + 0.5 * np.sin(2 * np.pi * t / 8.0 + 2)) * 1.4
    # a seamless loop: cross-fade the tail into the head
    k = int(SR * overlap)
    kop, staart = out[:k].copy(), out[-k:].copy()
    f = np.linspace(0, 1, k)
    out[:k] = kop * np.sqrt(f) + staart * np.sqrt(1 - f)
    out = out[:int(SR * lengte)]
    return out / max(1e-9, np.max(np.abs(out))) * 0.32


def _klok(f, lengte=3.4):
    """One glass tube of the chime: a pure tone with the bright inharmonic partials of a struck tube."""
    t = _t(lengte)
    s = np.sin(2 * np.pi * f * t) * np.exp(-t * 1.6)
    s += 0.42 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 3.4)
    s += 0.20 * np.sin(2 * np.pi * f * 5.40 * t) * np.exp(-t * 6.5)
    s += 0.08 * np.sin(2 * np.pi * f * 8.93 * t) * np.exp(-t * 11)
    return s * np.minimum(1, t / 0.002)


def windgong(nr):
    rng = np.random.default_rng(700 + nr)
    out = np.zeros(int(SR * 6.0))
    noten = [("D6", "A5", "G5", "Bb5"), ("A5", "D6", "Eb5", "G5", "A5"), ("G5", "Bb5", "D6")][nr]
    tijd = 0.05
    for i, noot in enumerate(noten):
        _plak(out, _klok(TOON[noot] * 2), tijd, rng.uniform(0.5, 1.0) * (0.85 ** i))
        tijd += rng.uniform(0.16, 0.62)
    out = _galm(out, ((0.17, 0.18), (0.31, 0.10)))
    return out / max(1e-9, np.max(np.abs(out))) * 0.42


GELUIDEN = {"muziek": muziek, "sfeer": sfeer, "windgong1": lambda: windgong(0), "windgong2": lambda: windgong(1), "windgong3": lambda: windgong(2)}


def schrijf(h):
    """Writes the missing ogg files (never rewrites an existing one)."""
    for naam, maak in GELUIDEN.items():
        path = os.path.join(h.A, "sounds", "klaterdal", f"{naam}.ogg")
        if not os.path.exists(path):
            os.makedirs(os.path.dirname(path), exist_ok=True)
            _ogg(path + ".tmp", maak())
            os.replace(path + ".tmp", path)


def _ogg(path, sig):
    """Mono vorbis with PyAV (as tools/features/hemel_geluid.py does)."""
    import fractions

    import av
    c = av.open(path, "w", format="ogg")
    st = c.add_stream("libvorbis", rate=SR)
    st.layout = "mono"
    data = sig.astype(np.float32)[None, :]
    pts = 0
    for i in range(0, data.shape[1], 1024):
        fr = av.AudioFrame.from_ndarray(np.ascontiguousarray(data[:, i:i + 1024]), format="fltp", layout="mono")
        fr.sample_rate = SR
        fr.pts = pts
        fr.time_base = fractions.Fraction(1, SR)
        pts += fr.samples
        for pk in st.encode(fr):
            c.mux(pk)
    for pk in st.encode(None):
        c.mux(pk)
    c.close()
