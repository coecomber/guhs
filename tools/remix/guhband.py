"""Extra instruments for the new disco songs (tango, mambo, boogie), on top of guhmix (which stays exactly as it was for
the approved 70's remix). Everything is synthesised; the vocals come from guhstem (the guhs' own noises)."""
import numpy as np

from guhmix import SR, A4, adsr, expdec, filt, mf, rng, saw, sweep_lp, compress, reverb

NOTE = {"C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5, "F#": 6, "Gb": 6, "G": 7, "G#": 8, "Ab": 8,
        "A": 9, "A#": 10, "Bb": 10, "B": 11}
QUALITY = {"": (0, 4, 7), "m": (0, 3, 7), "7": (0, 4, 7, 10), "m7": (0, 3, 7, 10), "maj7": (0, 4, 7, 11), "6": (0, 4, 7, 9),
           "m6": (0, 3, 7, 9), "dim7": (0, 3, 6, 9), "7b9": (0, 4, 7, 10, 13), "9": (0, 4, 7, 10, 14)}


def chord(name, octave=4):
    """'Dm' -> (root midi in the bass octave, [chord tones around middle C])."""
    root = name[:2] if len(name) > 1 and name[1] in "#b" else name[:1]
    q = name[len(root):]
    r = NOTE[root]
    bass = 36 + r
    base = 12 * (octave + 1) + r
    tones = [base + i for i in QUALITY[q]]
    tones = [t - 12 if t > 12 * (octave + 1) + 9 else t for t in tones]   # keep them in a comfortable range
    return bass, sorted(tones)


# --- tango ---------------------------------------------------------------------------------------------------------------
def bandoneon(notes, dur, swell=0.0, bright=2600, vel=1.0):
    """Free-reed bandoneon: two slightly detuned narrow pulse reeds per note (the musette 'shimmer'), bellows envelope;
    swell > 0 gives the tango 'arrastre' (starts soft, pushes into the next downbeat)."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    s = np.zeros(n)
    for m in notes:
        f = mf(m)
        for cents, pw in ((-5, 0.18), (6, 0.22)):
            ph = (t * f * 2 ** (cents / 1200) + rng.random()) % 1
            s += np.where(ph < pw, 1.0, 0.0) - pw
    s /= max(1, len(notes)) ** 0.8
    s = filt(filt(s, "low", bright), "high", 160)
    s = s + filt(s, "band", [900, 1400]) * 0.6                            # the reed box
    env = adsr(n, 0.03, 0.08, 0.85, min(0.08, dur / 3))
    if swell:
        env *= (0.25 + 0.75 * (t / max(dur, 1e-3)) ** 1.6) ** swell
    return s * env * vel


def pizz(m, dur=0.35, vel=1.0):
    """Pizzicato double bass: a plucked string with a woody body."""
    from guhmix import pluck
    s = pluck(m, dur, 0.25, 0.992)
    s = s + np.sin(2 * np.pi * mf(m) * np.arange(len(s)) / SR) * expdec(len(s), 0.12) * 0.6
    return filt(s, "low", 1800) * vel


def golpe():
    """The knock on the bandoneon/violin body that marks the tango's 1 and 3."""
    n = int(0.12 * SR)
    t = np.arange(n) / SR
    s = np.sin(2 * np.pi * (90 + 80 * np.exp(-t / 0.01)) * t) * expdec(n, 0.04)
    s += filt(rng.normal(0, 1, n), "band", [300, 1500]) * expdec(n, 0.01) * 0.4
    return s * 0.9


def chicharra():
    """The violinist's scratch below the bridge ('chicharra'): a rasping noise burst."""
    n = int(0.16 * SR)
    s = filt(rng.normal(0, 1, n), "band", [1500, 5000]) * (1 + np.sign(np.sin(2 * np.pi * 70 * np.arange(n) / SR)))
    return s * adsr(n, 0.01, 0.05, 0.6, 0.05) * 0.25


def violin(m, dur, slide_from=None, vel=1.0):
    from guhmix import fiddle
    s = fiddle(m, dur, slide_from)
    return filt(s, "low", 6000) * vel


# --- latin ---------------------------------------------------------------------------------------------------------------
def cowbell(accent=1.0):
    n = int(0.25 * SR)
    t = np.arange(n) / SR
    s = (np.sign(np.sin(2 * np.pi * 587 * t)) + np.sign(np.sin(2 * np.pi * 845 * t))) * 0.5
    s = filt(s, "band", [500, 3500]) * (expdec(n, 0.06) * 0.8 + expdec(n, 0.012) * 0.6)
    return s * 0.30 * accent


def clave():
    n = int(0.12 * SR)
    t = np.arange(n) / SR
    return np.sin(2 * np.pi * 2500 * t) * expdec(n, 0.018) * 0.35


def guiro(length=0.2):
    n = int(length * SR)
    t = np.arange(n) / SR
    teeth = (np.sin(2 * np.pi * 55 * t) > 0.6).astype(float)
    s = filt(rng.normal(0, 1, n), "band", [2500, 7000]) * (0.3 + teeth)
    return s * adsr(n, 0.01, 0.05, 0.8, 0.03) * 0.10


def timbale(high=True, rim=False):
    n = int(0.3 * SR)
    t = np.arange(n) / SR
    f = 520 if high else 390
    s = np.sin(2 * np.pi * f * t) * expdec(n, 0.15) + 0.5 * np.sin(2 * np.pi * f * 2.3 * t) * expdec(n, 0.06)
    s += filt(rng.normal(0, 1, n), "band", [2000, 8000]) * expdec(n, 0.008 if not rim else 0.02) * (0.8 if rim else 0.4)
    return s * 0.3


def trumpet(notes, dur, vel=1.0, fall=False):
    """A small brass section (trumpets + trombone), punchy mambo stabs; fall = a little drop at the end."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    s = np.zeros(n)
    for m in notes:
        mm = np.full(n, float(m))
        if fall:
            k = int(0.6 * n)
            mm[k:] -= np.linspace(0, 3, n - k)
        vib = 0.12 * np.sin(2 * np.pi * 5.5 * t) * np.clip(t / 0.3, 0, 1)
        for dt in (-7, 0, 6):
            f = A4 * 2 ** ((mm + vib - 69) / 12) * 2 ** (dt / 1200)
            s += 2 * ((np.cumsum(f) / SR + rng.random()) % 1) - 1
    s /= (3 * len(notes)) ** 0.8
    s = sweep_lp(s, lambda tt: 900 + 4200 * min(1, tt / 0.03) * (0.55 + 0.45 * np.exp(-tt / 0.2)))
    return s * adsr(n, 0.012, 0.08, 0.8, 0.04) * vel


def piano_hit(notes, dur, vel=0.8):
    from guhmix import piano
    return sum(piano(m, dur, vel) for m in notes) / max(1, len(notes)) ** 0.6


# --- boogie --------------------------------------------------------------------------------------------------------------
def organ(notes, dur, vel=1.0, leslie=6.5):
    n = int(dur * SR)
    t = np.arange(n) / SR
    s = np.zeros(n)
    for m in notes:
        f = mf(m)
        for mul, a in ((0.5, 0.6), (1, 1.0), (2, 0.6), (3, 0.3), (4, 0.25)):
            s += a * np.sin(2 * np.pi * f * mul * t + rng.random() * 6)
    s /= max(1, len(notes)) ** 0.8 * 2.5
    s *= 1 + 0.18 * np.sin(2 * np.pi * leslie * t)
    return s * adsr(n, 0.01, 0.05, 0.9, 0.05) * vel


def clav(m, dur=0.15, vel=1.0):
    """Funky clavinet pluck."""
    n = int(dur * SR)
    s = saw(mf(m), n) * 0.6 + np.sign(np.sin(2 * np.pi * mf(m) * np.arange(n) / SR)) * 0.4
    s = sweep_lp(s, lambda tt: 700 + 5500 * np.exp(-tt / 0.03))
    return filt(s, "high", 300) * expdec(n, 0.12) * adsr(n, 0.001, 0.02, 1.0, 0.02) * vel * 0.6


def synth_bass(m, dur, vel=1.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    s = saw(mf(m), n, 0.0) * 0.5 + np.sin(2 * np.pi * mf(m) * t)
    s = sweep_lp(s, lambda tt: 200 + 1800 * np.exp(-tt / 0.07))
    return np.tanh(s * 1.3) * adsr(n, 0.003, 0.1, 0.7, 0.03) * vel


# --- mixing --------------------------------------------------------------------------------------------------------------
REF_RMS = 0.29   # loudness of the approved remix (ze_hangen_disco70.ogg), so all four songs play equally loud


def master_rms(mix, fade_out, target=REF_RMS, drive=1.25):
    """Master like guhmix.master, but ending at the same loudness as the remix (RMS over the whole song)."""
    mix = compress(mix, thr=0.5, ratio=2.0, att=0.01, rel=0.2)
    mix = mix / np.max(np.abs(mix))
    for _ in range(6):
        cur = np.tanh(mix * drive) / np.tanh(drive)
        rms = np.sqrt(np.mean(cur ** 2))
        mix = mix * (target / max(rms, 1e-6)) ** 0.9
    out = np.tanh(mix * drive) / np.tanh(drive)
    fo = int(fade_out * SR)
    out[-fo:] *= np.linspace(1, 0, fo) ** 1.5
    peak = np.max(np.abs(out))
    return out * min(1.0, 0.92 / peak)
