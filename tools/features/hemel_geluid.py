"""
3.0 (Guhverhalen), slice hemel: the sounds of the Hemelkapelletje, synthesised (numpy) and written ONCE (vorbis encoding
isn't byte-for-byte stable, so an existing file is never rewritten; delete it to make it again):

  hemel/muziek.ogg    the chapel's soft music (a ~48 s loop): a harp and a celesta playing a slow lullaby in F major over a
                      warm pad, little bells on top (the Knuffelhart plays it near you, looping)
  hemel/hartklop.ogg  the Knuffelhart's soft "ba-dum" with a tiny shimmer
  hemel/terug.ogg     a guh comes back from the wolkjes: a rising harp glissando, a bell chord and glitter
  hemel/ster.ogg      a Herinnering star twinkles (hugging it, bringing it to the heart)
"""
import os

import numpy as np

SR = 44100


def _t(sec):
    return np.arange(int(SR * sec)) / SR


def _plak(out, geluid, start, volume):
    s = int(start * SR)
    if s < 0:
        geluid = geluid[-s:]
        s = 0
    if s >= len(out):
        return
    e = min(len(out), s + len(geluid))
    out[s:e] += geluid[:e - s] * volume


def _harp(f, lengte=2.2):
    """A plucked harp string: a few decaying harmonics with a soft attack."""
    t = _t(lengte)
    s = np.zeros_like(t)
    for k, a in ((1, 1.0), (2, 0.45), (3, 0.22), (4, 0.1), (5, 0.05)):
        s += a * np.sin(2 * np.pi * f * k * t) * np.exp(-t * (2.2 + k * 1.1))
    return s * np.minimum(1, t / 0.004)


def _celesta(f, lengte=1.6):
    """A celesta / music-box note: a pure tone with a bright inharmonic ping."""
    t = _t(lengte)
    s = np.sin(2 * np.pi * f * t) * np.exp(-t * 3.2) + 0.35 * np.sin(2 * np.pi * f * 4.0 * t) * np.exp(-t * 9)
    s += 0.12 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 14)
    return s * np.minimum(1, t / 0.003)


def _pad(freqs, lengte):
    """A warm, slowly breathing pad (soft sines with a little detune)."""
    t = _t(lengte)
    s = np.zeros_like(t)
    for f in freqs:
        for d in (-0.6, 0.6):
            s += np.sin(2 * np.pi * (f + d) * t + d)
    swell = np.minimum(1, t / 0.9) * np.minimum(1, (lengte - t) / 0.9)
    return s * swell / (2 * len(freqs))


def _glitter(out, van, tot, n, seed, vol=0.06):
    rng = np.random.default_rng(seed)
    for _ in range(n):
        t = _t(0.05)
        _plak(out, np.sin(2 * np.pi * rng.uniform(3400, 6200) * t) * np.exp(-t * 90), rng.uniform(van, tot), vol)


def _klaar(out, peak=0.8):
    n = len(out)
    out *= np.minimum(1, (n - np.arange(n)) / (0.08 * SR))
    return out / max(1e-6, np.abs(out).max()) * peak


def _nf(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def muziek():
    """A slow lullaby in F major, 72 BPM, 14 bars of 3/4 (~ 35 s) played twice with a softer second time, loops cleanly."""
    bpm = 72
    beat = 60 / bpm
    bar = 3 * beat
    # chords per bar (root position, midi) and the melody (bar, beat, midi, length in beats)
    chords = [(53, 57, 60), (50, 53, 57), (46, 50, 53), (48, 52, 55), (53, 57, 60), (45, 48, 52), (46, 50, 53), (48, 52, 55),
              (50, 53, 57), (46, 50, 53), (41, 45, 48), (48, 52, 55), (46, 50, 53), (48, 52, 55)]
    melody = [(0, 0, 72, 2), (0, 2, 74, 1), (1, 0, 76, 2), (1, 2, 72, 1), (2, 0, 74, 3), (3, 0, 72, 1), (3, 1, 70, 1), (3, 2, 67, 1),
              (4, 0, 69, 2), (4, 2, 72, 1), (5, 0, 76, 2), (5, 2, 74, 1), (6, 0, 74, 1), (6, 1, 72, 1), (6, 2, 70, 1), (7, 0, 72, 3),
              (8, 0, 77, 2), (8, 2, 76, 1), (9, 0, 74, 2), (9, 2, 72, 1), (10, 0, 69, 2), (10, 2, 72, 1), (11, 0, 70, 1),
              (11, 1, 69, 1), (11, 2, 67, 1), (12, 0, 70, 2), (12, 2, 74, 1), (13, 0, 72, 3)]
    rondes = 2
    lengte = rondes * len(chords) * bar
    out = np.zeros(int(SR * lengte) + SR)
    for r in range(rondes):
        t0 = r * len(chords) * bar
        zacht = 1.0 if r == 0 else 0.8
        for i, ch in enumerate(chords):
            start = t0 + i * bar
            _plak(out, _pad([_nf(m - 12) for m in ch], bar + 0.4), start - 0.1, 0.22 * zacht)
            # the harp: a rolled arpeggio up and down each bar
            notes = [ch[0] - 12, ch[0], ch[1], ch[2], ch[0] + 12, ch[2]]
            for k, m in enumerate(notes):
                _plak(out, _harp(_nf(m)), start + k * beat / 2, 0.16 * zacht)
        for (b, bt, m, ln) in melody:
            start = t0 + b * bar + bt * beat
            _plak(out, _celesta(_nf(m + (12 if r == 1 else 0)), max(1.2, ln * beat + 0.6)), start, 0.3 * zacht)
        _glitter(out, t0 + 1, t0 + len(chords) * bar - 1, 40, 1001 + r, 0.03)
    out = out[:int(SR * lengte)]
    out[-int(SR * 0.05):] *= np.linspace(1, 0, int(SR * 0.05))
    out[:int(SR * 0.05)] *= np.linspace(0, 1, int(SR * 0.05))
    return out / max(1e-6, np.abs(out).max()) * 0.7


def hartklop():
    out = np.zeros(int(SR * 0.9))
    for s, v in ((0.0, 1.0), (0.2, 0.75)):
        t = _t(0.16)
        _plak(out, np.sin(2 * np.pi * (62 + 46 * np.exp(-t * 28)) * t) * np.exp(-t * 24), s, v)
    _glitter(out, 0.25, 0.7, 5, 1011, 0.05)
    return _klaar(out, 0.85)


def terug():
    out = np.zeros(int(SR * 3.2))
    for k, m in enumerate((53, 57, 60, 65, 69, 72, 77, 81, 84)):     # the glissando up
        _plak(out, _harp(_nf(m), 2.4), 0.02 + k * 0.07, 0.4)
    for m in (77, 81, 84, 89):                                          # the bell chord
        _plak(out, _celesta(_nf(m), 2.4), 0.72, 0.3)
    _plak(out, _pad([_nf(m) for m in (53, 57, 60, 65)], 2.4), 0.6, 0.35)
    _glitter(out, 0.7, 2.8, 30, 1012, 0.07)
    return _klaar(out)


def ster():
    out = np.zeros(int(SR * 1.2))
    for k, m in enumerate((84, 88, 91, 96)):
        _plak(out, _celesta(_nf(m), 1.0), k * 0.06, 0.4)
    _glitter(out, 0.1, 0.9, 12, 1013, 0.08)
    return _klaar(out, 0.7)


GELUIDEN = {"muziek": muziek, "hartklop": hartklop, "terug": terug, "ster": ster}


def schrijf(h):
    """Writes the missing ogg files (never rewrites an existing one)."""
    for naam, maak in GELUIDEN.items():
        path = os.path.join(h.A, "sounds", "hemel", f"{naam}.ogg")
        if not os.path.exists(path):
            os.makedirs(os.path.dirname(path), exist_ok=True)
            _ogg(path + ".tmp", maak())
            os.replace(path + ".tmp", path)


def _ogg(path, sig):
    """Mono vorbis with PyAV (like tools/remix/guhmix.write_ogg; libsndfile crashes on long vorbis files)."""
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
