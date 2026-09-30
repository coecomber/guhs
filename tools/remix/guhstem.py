"""The guh choir: cuts the mod's own guh (and Mika) noises into syllables, measures their pitch and sings them on notes.

The new disco songs (Vadsige Tango, Mika-Mambo, Njeg-Njeg Boogie) have no words: their "vocals" are the guhs' own
sounds (assets/guhs/sounds/guh_ambient*.ogg, mika_ambient*.ogg), cut into syllables, re-pitched to the melody (tuned
to the band, A ~ 450 Hz like the remix) and placed on the beat.

    python guhstem.py            prints the syllable table (for choosing syllables by ear/eye)
"""
import os

import av
import numpy as np
from scipy.signal import resample_poly

from guhmix import SR, A4, filt, adsr

HERE = os.path.dirname(os.path.abspath(__file__))
SOUNDS = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "guhs", "sounds"))


def load(name):
    """A mod sound (e.g. 'guh_ambient3') as mono float at SR."""
    c = av.open(os.path.join(SOUNDS, name + ".ogg"))
    s = c.streams.audio[0]
    rate = s.rate
    x = np.concatenate([fr.to_ndarray().reshape(fr.to_ndarray().shape[0], -1).mean(0) for fr in c.decode(s)]).astype(np.float64)
    c.close()
    if rate != SR:
        from math import gcd
        g = gcd(SR, rate)
        x = resample_poly(x, SR // g, rate // g)
    return x


def yin(frame, sr=SR, fmin=110, fmax=1100, thr=0.15):
    """Fundamental of one frame (Hz) with YIN, or 0 when unvoiced."""
    n = len(frame)
    tmax = min(n // 2, int(sr / fmin))
    tmin = int(sr / fmax)
    x = frame - frame.mean()
    d = np.zeros(tmax + 1)
    for tau in range(1, tmax + 1):                    # the difference function (frames are short: exact is fine)
        a, b = x[:n - tau], x[tau:]
        d[tau] = np.dot(a - b, a - b)
    cmnd = np.ones(tmax + 1)
    run = np.cumsum(d[1:])
    cmnd[1:] = d[1:] * np.arange(1, tmax + 1) / np.maximum(run, 1e-12)
    for tau in range(tmin, tmax):
        if cmnd[tau] < thr:
            while tau + 1 < tmax and cmnd[tau + 1] < cmnd[tau]:
                tau += 1
            # parabolic interpolation
            if 1 <= tau < tmax:
                y0, y1, y2 = cmnd[tau - 1], cmnd[tau], cmnd[tau + 1]
                den = y0 - 2 * y1 + y2
                shift = 0.5 * (y0 - y2) / den if abs(den) > 1e-12 else 0
            else:
                shift = 0
            return sr / (tau + shift)
    return 0.0


def pitch_track(x, hop=0.01, win=0.04):
    h, w = int(hop * SR), int(win * SR)
    out = []
    for i in range(0, max(1, len(x) - w), h):
        fr = x[i:i + w]
        if np.sqrt(np.mean(fr ** 2)) < 0.02:
            out.append(0.0)
        else:
            out.append(yin(fr))
    return np.array(out)


def syllables(x, thr=0.12, min_len=0.07, min_gap=0.035):
    """(start, end) sample ranges of the loud parts of a clip."""
    w = int(0.01 * SR)
    env = np.sqrt(np.convolve(x ** 2, np.ones(w) / w, "same"))
    on = env > thr * env.max()
    segs, i, n = [], 0, len(x)
    while i < n:
        if on[i]:
            j = i
            while j < n and (on[j] or (j + int(min_gap * SR) < n and on[j:j + int(min_gap * SR)].any())):
                j += 1
            if (j - i) / SR >= min_len:
                segs.append((max(0, i - int(0.008 * SR)), min(n, j + int(0.02 * SR))))
            i = j
        i += 1
    return segs


class Syllable:
    def __init__(self, clip, a, b, x):
        self.clip, self.a, self.b = clip, a, b
        self.x = x[a:b].copy()
        tr = pitch_track(self.x)
        v = tr[tr > 0]
        self.voiced = len(v) / max(1, len(tr))
        self.f0 = float(np.median(v)) if len(v) else 0.0
        self.spread = float(np.std(1200 * np.log2(v / self.f0))) if len(v) > 2 else 999.0
        self.dur = len(self.x) / SR

    @property
    def midi(self):
        return 69 + 12 * np.log2(self.f0 / A4) if self.f0 else 0

    def __repr__(self):
        return f"{self.clip}[{self.a / SR:.2f}-{self.b / SR:.2f}] {self.dur:.2f}s f0={self.f0:.0f} midi={self.midi:.1f} voiced={self.voiced:.2f} spread={self.spread:.0f}c"


_CACHE = {}


def library(prefix="guh_ambient", count=16):
    key = (prefix, count)
    if key not in _CACHE:
        out = []
        for k in range(1, count + 1):
            name = f"{prefix}{k}"
            x = load(name)
            x = x / (np.abs(x).max() + 1e-9)
            for a, b in syllables(x):
                s = Syllable(name, a, b, x)
                if s.f0 and s.voiced > 0.4 and s.spread < 350:
                    out.append(s)
        _CACHE[key] = out
    return _CACHE[key]


def pick(lib, max_dur=None, min_dur=0.08, low=None, high=None):
    """Steady voiced syllables in a pitch range (MIDI), best (steadiest) first."""
    c = [s for s in lib if s.dur >= min_dur and (max_dur is None or s.dur <= max_dur)
         and (low is None or s.midi >= low) and (high is None or s.midi <= high)]
    return sorted(c, key=lambda s: s.spread)


def _contour(syl):
    """The syllable's pitch deviation from its median (cents) per sample, unvoiced parts = 0 (cached)."""
    if getattr(syl, "_dev", None) is None:
        tr = pitch_track(syl.x)
        dev = np.where(tr > 0, 1200 * np.log2(np.maximum(tr, 1) / syl.f0), np.nan)
        idx = np.arange(len(dev))
        ok = ~np.isnan(dev)
        dev = np.interp(idx, idx[ok], dev[ok]) if ok.any() else np.zeros(len(dev))
        dev = np.clip(dev, -400, 400)
        t = (idx * 0.01 + 0.02) * SR
        syl._dev = np.interp(np.arange(len(syl.x)), t, dev)
    return syl._dev


def sing_note(syl, midi, dur=None, flat=0.85, glide=0.0):
    """The syllable sung on a MIDI note (tuned band) by varispeed: the read speed follows the pitch contour so that
    the guh's swoop is flattened (flat=1: dead straight, 0: its own swoop), glide = semitones to scoop up from.
    Optionally cut/faded to dur seconds."""
    dev = _contour(syl)
    x = syl.x
    n = len(x)
    t_in = np.arange(n)
    scoop = glide * np.clip(1 - t_in / (0.06 * SR), 0, 1) if glide else 0.0
    r = 2 ** ((midi - syl.midi) / 12 - flat * dev / 1200 - scoop / 12)   # read speed per input sample
    out_pos = np.concatenate([[0.0], np.cumsum(1 / r)])[:n]              # output index where each input sample lands
    m = int(out_pos[-1])
    y = np.interp(np.arange(m), out_pos, x)
    if dur is not None:
        n = int(dur * SR)
        if len(y) > n:
            y = y[:n]
        y = y * adsr(len(y), 0.004, 0.05, 1.0, min(0.06, len(y) / SR / 3))
    return filt(y, "high", 120)


def onset_of(sig, thr=0.2):
    w = max(1, int(0.004 * SR))
    env = np.convolve(np.abs(sig), np.ones(w) / w, "same")
    idx = np.where(env > thr * env.max())[0]
    return idx[0] / SR if len(idx) else 0.0


def place(tr, sig, t, gain=1.0, bus="vox"):
    """Puts a sung syllable so that its attack lands on time t."""
    tr.add(bus, sig, t - onset_of(sig), gain)


if __name__ == "__main__":
    for pre in ("guh_ambient", "mika_ambient"):
        lib = library(pre)
        print(pre, len(lib), "syllables")
        for s in sorted(lib, key=lambda s: s.midi):
            print("  ", s)


class Choir:
    """Picks, per note, a steady syllable close to the note (little re-pitching = natural), rotating for variety."""

    def __init__(self, prefix="guh_ambient", count=16, max_shift=13, flat=0.85):
        self.lib = [s for s in library(prefix, count) if s.spread < 240 or s.voiced > 0.9]
        self.max_shift, self.flat, self.k = max_shift, flat, 0

    def note(self, midi, dur=None, glide=0.0, short=False):
        c = [s for s in self.lib if abs(s.midi - midi) <= self.max_shift and (not short or s.dur < 0.32)] or self.lib
        c = sorted(c, key=lambda s: abs(s.midi - midi) * 0.15 + s.spread / 100)[:4]
        s = c[self.k % len(c)]
        self.k += 1
        return sing_note(s, midi, dur, self.flat, glide)
