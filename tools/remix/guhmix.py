"""Shared building blocks for the guh-song remixes (instruments, effects, vocal cuts, ogg writer)."""
import fractions
import os

import av
import numpy as np
from scipy.signal import butter, fftconvolve, resample_poly, sosfilt, sosfilt_zi

HERE = os.path.dirname(os.path.abspath(__file__))
SR = 48000
rng = np.random.default_rng(2029)
X = np.load(os.path.join(HERE, "orig.npy"))[0].astype(np.float64)

# Where the words are in the original a cappella (seconds). Filled in from the Whisper transcription (lyrics.py).
from lyrics import LINES, ZIN, ZE_HANGEN_3X, HOOK, CHOP  # noqa: E402


def cut(a, b, fade=0.010):
    s = X[int(a * SR):int(b * SR)].copy()
    n = int(fade * SR)
    s[:n] *= np.linspace(0, 1, n)
    s[-n:] *= np.linspace(1, 0, n)
    return s


# --- dsp ----------------------------------------------------------------------------------------------------------
def filt(sig, kind, f, order=2):
    return sosfilt(butter(order, f, kind, fs=SR, output="sos"), sig)


def expdec(n, t):
    return np.exp(-np.arange(n) / SR / t)


def adsr(n, a=0.005, d=0.1, s=0.7, r=0.05):
    e = np.full(n, float(s))
    na = max(1, min(int(a * SR), n))
    e[:na] = np.linspace(0, 1, na)
    nd = max(1, min(int(d * SR), n - na))
    e[na:na + nd] = np.linspace(1, s, nd)
    nr = max(1, min(int(r * SR), n))
    e[-nr:] *= np.linspace(1, 0, nr)
    return e


TUNE_CENTS = 40  # the a cappella sits ~40 cents sharp of A=440: tune the band to the voice (A ≈ 450 Hz)
A4 = 440.0 * 2 ** (TUNE_CENTS / 1200)


def mf(m):
    return A4 * 2 ** ((m - 69) / 12)


def saw(f, n, phase=None):
    t = np.arange(n) / SR
    return 2 * ((t * f + (rng.random() if phase is None else phase)) % 1) - 1


def sweep_lp(sig, cutoff_at):
    """Time-varying low-pass: cutoff_at(seconds) -> Hz, in blocks."""
    out = np.zeros_like(sig)
    zi = None
    for i in range(0, len(sig), 256):
        sos = butter(2, min(20000, max(40, cutoff_at(i / SR))), "low", fs=SR, output="sos")
        if zi is None:
            zi = sosfilt_zi(sos) * 0
        out[i:i + 256], zi = sosfilt(sos, sig[i:i + 256], zi=zi)
    return out


def compress(sig, thr=0.25, ratio=4.0, att=0.003, rel=0.12):
    blk = 64
    m = len(sig) // blk * blk
    peaks = np.abs(sig[:m]).reshape(-1, blk).max(axis=1) if m else np.zeros(1)
    a, r = np.exp(-1 / (att * SR / blk)), np.exp(-1 / (rel * SR / blk))
    e, out = 0.0, np.zeros_like(peaks)
    for i, p in enumerate(peaks):
        c = a if p > e else r
        e = c * e + (1 - c) * p
        out[i] = e
    env = np.full(len(sig), out[-1])
    env[:m] = np.repeat(out, blk)
    gain = np.where(env > thr, (thr + (env - thr) / ratio) / np.maximum(env, 1e-9), 1.0)
    return sig * gain


def reverb(sig, length=1.8, decay=0.5, lp=6000, predelay=0.012):
    n = int(length * SR)
    ir = filt(rng.normal(0, 1, n), "low", lp) * expdec(n, decay)
    ir[: int(predelay * SR)] = 0
    ir /= np.sqrt((ir ** 2).sum())
    return fftconvolve(sig, ir)[: len(sig)]


def delay(sig, t, taps=3, fb=0.5, gain=0.2, lp=3000):
    d = int(t * SR)
    out = np.zeros_like(sig)
    for k in range(1, taps + 1):
        tap = np.zeros_like(sig)
        tap[d * k:] = sig[: -d * k]
        out += filt(tap, "low", lp) * gain * fb ** (k - 1)
    return out


def detune_double(sig, cents=12, ms=18):
    """A second 'take': slightly re-pitched (tape-speed) and delayed copy."""
    ratio = 2 ** (cents / 1200)
    up, down = 1000, int(round(1000 * ratio))
    s = resample_poly(sig, up, down)[: len(sig)]
    s = np.pad(s, (int(ms / 1000 * SR), 0))[: len(sig)]
    return np.pad(s, (0, len(sig) - len(s)))


# --- drums & percussion -------------------------------------------------------------------------------------------
def kick(punch=1.0, length=0.32, low=48):
    n = int(length * SR)
    t = np.arange(n) / SR
    f = low + 90 * np.exp(-t / 0.03)
    s = np.sin(2 * np.pi * np.cumsum(f) / SR) * expdec(n, length / 2)
    s += filt(rng.normal(0, 1, n), "band", [1500, 5000]) * expdec(n, 0.003) * 0.25 * punch
    return np.tanh(s * 1.4)


def snare(clap_mix=0.35, body_hz=190):
    n = int(0.3 * SR)
    t = np.arange(n) / SR
    body = np.sin(2 * np.pi * body_hz * t) * expdec(n, 0.05) * 0.6
    noise = filt(rng.normal(0, 1, n), "band", [1200, 7000]) * expdec(n, 0.11) * 0.55
    clap = np.zeros(n)
    for off, dec in ((0, 0.008), (0.010, 0.008), (0.021, 0.06)):
        o = int(off * SR)
        clap[o:] += filt(rng.normal(0, 1, n - o), "band", [900, 2500]) * expdec(n - o, dec)
    return body + noise + clap * clap_mix


def clap():
    n = int(0.25 * SR)
    e = np.zeros(n)
    for off, dec in ((0, 0.007), (0.009, 0.007), (0.019, 0.07)):
        o = int(off * SR)
        e[o:] += expdec(n - o, dec)
    return filt(rng.normal(0, 1, n), "band", [900, 3000]) * e * 0.6


def hat(open_=False, level=None):
    n = int((0.30 if open_ else 0.045) * SR)
    s = filt(rng.normal(0, 1, n), "high", 7500, 4)
    return s * expdec(n, 0.12 if open_ else 0.010) * (level or (0.30 if open_ else 0.18))


def tambourine():
    n = int(0.12 * SR)
    s = filt(rng.normal(0, 1, n), "band", [6000, 14000], 2)
    return (s + np.sin(2 * np.pi * 7200 * np.arange(n) / SR) * 0.3 * s) * expdec(n, 0.035) * 0.16


def conga(high=True):
    n = int(0.25 * SR)
    t = np.arange(n) / SR
    f0 = (330 if high else 220) * (1 + 0.25 * np.exp(-t / 0.01))
    s = np.sin(2 * np.pi * np.cumsum(f0) / SR) * expdec(n, 0.09)
    s += filt(rng.normal(0, 1, n), "band", [800, 3000]) * expdec(n, 0.004) * 0.3
    return s * 0.35


def brush_snare():
    n = int(0.18 * SR)
    return filt(rng.normal(0, 1, n), "band", [2000, 8000]) * adsr(n, 0.02, 0.05, 0.4, 0.1) * 0.35


# --- tonal instruments --------------------------------------------------------------------------------------------
def pluck(m, dur, bright=0.5, damp=0.996):
    """Karplus-Strong string (vectorised per period)."""
    n = int(dur * SR)
    f = mf(m)
    p = max(2, int(round(SR / f)))
    buf = filt(rng.uniform(-1, 1, p), "low", 1500 + 8000 * bright) if p > 12 else rng.uniform(-1, 1, p)
    out = np.zeros(n + p)
    out[:p] = buf
    i = p
    while i < n + p:
        j = min(n + p, i + p)
        prev = out[i - p:j - p]
        nxt = out[i - p + 1:j - p + 1] if j - p + 1 <= len(out) else np.append(out[i - p + 1:], 0)
        out[i:j] = 0.5 * (prev + nxt[: len(prev)]) * damp
        i = j
    return out[p:p + n] * adsr(n, 0.001, 0.01, 1.0, 0.02)


def bass_note(m, dur, accent=1.0, tone=1600, decay=0.06):
    n = int(dur * SR)
    f = mf(m)
    t = np.arange(n) / SR
    s = 0.6 * saw(f, n, 0.0) + 0.4 * np.sign(np.sin(2 * np.pi * f * t)) + 0.5 * np.sin(2 * np.pi * f * t)
    s = sweep_lp(s, lambda tt: 250 + tone * np.exp(-tt / decay) * accent)
    return s * adsr(n, 0.003, 0.15, 0.6, 0.03)


def sub_bass(m, dur):
    n = int(dur * SR)
    t = np.arange(n) / SR
    s = np.sin(2 * np.pi * mf(m) * t) + 0.2 * np.sin(4 * np.pi * mf(m) * t)
    return np.tanh(s * 1.5) * adsr(n, 0.005, 0.1, 0.9, 0.05)


def rhodes(m, dur, vel=1.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = mf(m)
    idx = 2.2 * expdec(n, 0.25) * vel + 0.3
    s = np.sin(2 * np.pi * f * t + np.sin(2 * np.pi * f * t) * idx)
    s += np.sin(2 * np.pi * f * 14 * t) * expdec(n, 0.02) * 0.08 * vel
    return s * expdec(n, 1.2) * adsr(n, 0.002, 0.05, 1.0, 0.05) * (1 + 0.12 * np.sin(2 * np.pi * 4.5 * t))


def piano(m, dur, vel=0.8):
    """Additive piano-ish tone: inharmonic partials, hammer click, two-stage decay."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = mf(m)
    s = np.zeros(n)
    for k in range(1, 9):
        fk = f * k * np.sqrt(1 + 0.0004 * k * k)
        if fk > 16000:
            break
        amp = (1 / k ** 1.3) * (0.6 + 0.4 * vel)
        s += amp * np.sin(2 * np.pi * fk * t + rng.random()) * expdec(n, (1.6 if k < 3 else 0.6) * (220 / max(f, 110)) ** 0.3)
    s += filt(rng.normal(0, 1, n), "band", [2000, 6000]) * expdec(n, 0.004) * 0.05 * vel
    return s * vel * adsr(n, 0.002, 0.05, 1.0, 0.08)


def strings(notes, dur, attack=0.35, bright=3500, voices=(-14, -8, -3, 0, 4, 9, 15)):
    n = int(dur * SR)
    t = np.arange(n) / SR
    s = np.zeros(n)
    for m in notes:
        f = mf(m)
        for dt in voices:
            vib = 1 + 0.004 * np.sin(2 * np.pi * (5.2 + dt * 0.03) * t + rng.random() * 6)
            s += 2 * ((np.cumsum(f * 2 ** (dt / 1200) * vib) / SR + rng.random()) % 1) - 1
    s /= (len(voices) * len(notes)) ** 0.7
    return filt(filt(s, "low", bright), "high", 180) * adsr(n, attack, 0.2, 0.9, min(0.4, dur / 3))


def string_run(m_from, m_to, dur):
    n = int(dur * SR)
    t = np.arange(n) / SR
    mm = m_from + (m_to - m_from) * (t / dur) ** 1.5
    s = np.zeros(n)
    for dt in (-10, -3, 4, 11):
        f = A4 * 2 ** ((mm - 69) / 12) * 2 ** (dt / 1200)
        s += 2 * ((np.cumsum(f) / SR + rng.random()) % 1) - 1
    return filt(s / 4, "low", 5000) * adsr(n, 0.05, 0.1, 1.0, 0.08)


def brass(notes, dur):
    n = int(dur * SR)
    s = sum(saw(mf(m) * 2 ** (dt / 1200), n) for m in notes for dt in (-6, 0, 7)) / (3 * len(notes))
    s = sweep_lp(s, lambda tt: 900 + 3500 * min(1, tt / 0.04) * np.exp(-tt / 0.25))
    return s * adsr(n, 0.015, 0.1, 0.8, 0.04)


def fiddle(m, dur, slide_from=None):
    """Bowed fiddle: saw with vibrato, bow noise and a body resonance."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    mm = np.full(n, float(m))
    if slide_from is not None:
        k = min(n, int(0.08 * SR))
        mm[:k] = np.linspace(slide_from, m, k)
    vib = 0.25 * np.sin(2 * np.pi * 5.8 * t) * np.clip(t / 0.25, 0, 1)
    f = A4 * 2 ** ((mm + vib - 69) / 12)
    s = 2 * ((np.cumsum(f) / SR) % 1) - 1
    s += filt(rng.normal(0, 1, n), "band", [2000, 6000]) * 0.05
    s = filt(s, "band", [350, 4500]) + filt(s, "band", [2500, 3200]) * 0.6
    return s * adsr(n, 0.04, 0.05, 0.85, 0.06)


def steel(m_from, m_to, dur):
    """Pedal-steel swell: sine + few harmonics, volume swell and a slow slide."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    mm = m_from + (m_to - m_from) * np.clip(t / (dur * 0.35), 0, 1)
    f = A4 * 2 ** ((mm - 69) / 12)
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = np.sin(ph) + 0.35 * np.sin(2 * ph) + 0.15 * np.sin(3 * ph)
    return s * adsr(n, dur * 0.3, 0.1, 0.9, 0.2) * (1 + 0.05 * np.sin(2 * np.pi * 5 * t))


def supersaw(notes, dur, cutoff=3000, attack=0.01):
    n = int(dur * SR)
    s = sum(saw(mf(m) * 2 ** (dt / 1200), n) for m in notes for dt in (-18, -9, 0, 9, 18)) / (5 * len(notes)) ** 0.8
    return filt(s, "low", cutoff) * adsr(n, attack, 0.1, 0.8, 0.08)


def synth_pluck(m, dur=0.2):
    n = int(dur * SR)
    s = saw(mf(m), n) + saw(mf(m) * 1.005, n)
    return sweep_lp(s * 0.5, lambda tt: 300 + 5000 * np.exp(-tt / 0.05)) * expdec(n, 0.18)


# --- mixing -------------------------------------------------------------------------------------------------------
class Tracks:
    def __init__(self, seconds):
        self.n = int(seconds * SR)
        self.bus = {}
        self.side = np.ones(self.n)

    def add(self, name, sig, t, gain=1.0):
        buf = self.bus.setdefault(name, np.zeros(self.n))
        i = int(round(t * SR))
        if i < 0:
            sig, i = sig[-i:], 0
        if i >= self.n:
            return
        j = min(self.n, i + len(sig))
        buf[i:j] += sig[: j - i] * gain

    def duck(self, t, depth=0.45, length=0.5, rel=0.08):
        i = int(t * SR)
        k = int(length * SR)
        d = 1 - depth * expdec(k, rel)
        j = min(self.n, i + k)
        self.side[i:j] = np.minimum(self.side[i:j], d[: j - i])

    def get(self, name):
        return self.bus.get(name, np.zeros(self.n))


def master(mix, fade_out=6.0, drive=1.2):
    mix = compress(mix, thr=0.5, ratio=2.0, att=0.01, rel=0.2)
    mix = np.tanh(mix * drive) / np.tanh(drive)
    fo = int(fade_out * SR)
    mix[-fo:] *= np.linspace(1, 0, fo) ** 1.5
    return mix * 0.9 / np.max(np.abs(mix))


def write_ogg(path, sig):
    c = av.open(path, "w")
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
        for p in st.encode(fr):
            c.mux(p)
    for p in st.encode(None):
        c.mux(p)
    c.close()
    print(path, f"{len(sig) / SR:.1f}s", os.path.getsize(path) // 1024, "KB")


def onset(a, b, thr=0.06):
    """Seconds from a to the first loud sample of the cut (so the first syllable can land on the beat)."""
    s = np.abs(X[int(a * SR):int(b * SR)])
    w = int(0.01 * SR)
    env = np.convolve(s, np.ones(w) / w, "same")
    idx = np.where(env > thr * env.max() / 0.25 * 0.25)[0]
    return idx[0] / SR if len(idx) else 0.0


def sing(tr, seg, t, gain=1.0, bus="vox"):
    """Place a vocal cut so that its first syllable lands exactly on time t."""
    a, b = seg
    tr.add(bus, cut(a, b), t - onset(a, b), gain)


def vocal_chain(v, style="disco", beat=0.5):
    v = filt(v, "high", 110)
    v = compress(v * 2.2, thr=0.22, ratio=3.5)
    if style == "country":
        v = v + filt(filt(v, "high", 1200), "low", 3500) * 0.5          # nasal "twang" presence
        return v + delay(v, 0.11, taps=2, fb=0.35, gain=0.3, lp=4000) + reverb(v, 1.2, 0.3) * 0.12
    if style == "pop":
        v = v + detune_double(v, 9, 16) * 0.45 + detune_double(v, -9, 27) * 0.45
        v = v + filt(v, "high", 5000) * 0.4                              # air
        return v + delay(v, beat * 0.75, taps=2, fb=0.4, gain=0.12) + reverb(v, 2.4, 0.7) * 0.3
    if style == "ballad":
        return v + delay(v, beat, taps=2, fb=0.4, gain=0.1) + reverb(v, 3.5, 1.2, 5000, 0.03) * 0.45
    d = v + filt(detune_double(v, 6, 22), "low", 5000) * 0.35               # disco
    return d + delay(d, beat / 2, taps=3, fb=0.5, gain=0.18) + reverb(d, 1.6, 0.45) * 0.22
