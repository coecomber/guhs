"""
einde2, slice audio - the sound files (mono OGG Vorbis, 44.1 kHz) in assets/guhs/sounds/einde2_audio/ (CONTRACT_E2_B 6.3).

Two jobs, both run by hand (NOT part of make_resources.py: it needs scipy and soundfile; the OGGs are committed as binaries
and einde2_audio.py only lists them in sounds.json):

    python tools/features/einde2_audio_geluid.py            the pieces the mod makes itself (fixed seeds: the same every run)
    python tools/features/einde2_audio_geluid.py --muziek <bron>
                                                             the real pieces of the licence ledger einde2_audio_licenties.json,
                                                             from the files downloaded once into <bron>
                                                             (guhs_workeinde2/scratch/audio/bron/): mono, 44.1 kHz, the same
                                                             loudness, trimmed with a fade where the ledger says "knip"

Made here (own compositions; NEVER a melody of the series or any other copyrighted work):
  moment_lu_ten         Miroh's lullaby: an own slow lullaby in a minor pentatonic scale on one breathy bamboo flute (xiao),
                        3/4, 72 BPM; the timing of the sung lines is LuTenLied.java (and REGELS below)
  deuntje_*             the tunes: theehuis (guzheng, cosy), thee_ting, pai_sho (calm), luchtscooter, momo, slee, monorail,
                        glijbaan, and the three rhythm tunes dansfeest (100 BPM), waaierdans (90), ritmeduel (80) whose first
                        beat is the first sample (the games count from tick 0)
  stinger_*             kolen (the cabbage-coal joke), toneel_fanfare (a proud and slightly wrong fanfare), feestje
                        ("Happy Birthday", public domain melody, on a toy piano and a kazoo), mijn_eer, opgelost, tegel
  geest_*               the Geestenwereld: koi_zang, koh_kraak, koh_stilte, lantaarn_tinkel, klok, geestentij, maannacht, reiger
"""
import json
import os
import sys

import numpy as np

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "einde2_audio")
LEDGER = os.path.join("tools", "features", "einde2_audio_licenties.json")
# loudness of the real pieces: RMS target (dBFS) and the soft ceiling
DOEL_RMS_DB = -20.0
PLAFOND = 0.89
# Vorbis: libsndfile's compression level (0 = best, 1 = smallest); mono at 0.8 is about 62 kbit/s
NIVEAU_MUZIEK = 0.8
NIVEAU_EIGEN = 0.45


# =====================================================================================================================
# helpers
# =====================================================================================================================
def _sf():
    import soundfile
    return soundfile


def _sig():
    import scipy.signal
    return scipy.signal


def save(name, x, niveau=NIVEAU_EIGEN):
    os.makedirs(OUT, exist_ok=True)
    x = np.clip(np.asarray(x, dtype=np.float32), -1.0, 1.0)
    sf = _sf()
    # in blocks: libsndfile's Vorbis writer falls over on one huge write
    with sf.SoundFile(os.path.join(OUT, name + ".ogg"), "w", SR, 1, format="OGG", subtype="VORBIS", compression_level=niveau) as f:
        for i in range(0, len(x), 32768):
            f.write(x[i:i + 32768])
    print(f"wrote {name:28s} {len(x) / SR:7.2f}s  {os.path.getsize(os.path.join(OUT, name + '.ogg')) / 1024:8.0f} KB")


def t_(dur):
    return np.arange(int(round(dur * SR))) / SR


def env(n, attack, release, curve=2.0):
    e = np.ones(n)
    a, r = max(1, min(n, int(attack * SR))), max(1, min(n, int(release * SR)))
    e[:a] = np.linspace(0, 1, a)
    e[-r:] *= np.linspace(1, 0, r) ** curve
    return e


def norm(x, gain=0.8):
    return (x / (np.max(np.abs(x)) + 1e-9) * gain).astype(np.float32)


def band(x, lo, hi, order=2):
    s = _sig()
    b, a = s.butter(order, [lo / (SR / 2), min(hi, SR / 2 - 100) / (SR / 2)], btype="band")
    return s.lfilter(b, a, x)


def low(x, f, order=2):
    s = _sig()
    b, a = s.butter(order, f / (SR / 2), btype="low")
    return s.lfilter(b, a, x)


def high(x, f, order=2):
    s = _sig()
    b, a = s.butter(order, f / (SR / 2), btype="high")
    return s.lfilter(b, a, x)


def galm(x, tijd=1.8, nat=0.25, seed=1):
    """A soft room: convolution with a decaying noise tail (mono)."""
    r = np.random.default_rng(seed)
    n = int(tijd * SR)
    staart = r.normal(0, 1, n) * np.exp(-np.arange(n) / (SR * tijd / 6.0))
    staart = low(staart, 5000)
    staart[0] = 0
    staart /= np.sqrt(np.sum(staart ** 2)) + 1e-9
    nat_sig = np.zeros(len(x) + n)
    nat_sig[:len(x) + n - 1] = _sig().fftconvolve(x, staart)
    droog = np.concatenate([x, np.zeros(n)])
    return droog + nat * nat_sig * 3.0


def mix(lengte, *delen):
    """Mixes (offset_seconds, signal) parts into a buffer of `lengte` seconds."""
    uit = np.zeros(int(round(lengte * SR)))
    for off, sig in delen:
        i = int(round(off * SR))
        if i >= len(uit):
            continue
        j = min(len(uit), i + len(sig))
        uit[i:j] += sig[:j - i]
    return uit


# note names -> Hz (A4 = 440)
_NOOT = {"C": -9, "C#": -8, "Db": -8, "D": -7, "D#": -6, "Eb": -6, "E": -5, "F": -4, "F#": -3, "Gb": -3, "G": -2, "G#": -1,
         "Ab": -1, "A": 0, "A#": 1, "Bb": 1, "B": 2}


def hz(noot):
    naam, octaaf = noot[:-1], int(noot[-1])
    return 440.0 * 2 ** ((_NOOT[naam] + 12 * (octaaf - 4)) / 12)


# =====================================================================================================================
# instruments (synthesised)
# =====================================================================================================================
def pluk(f, dur, seed, helder=0.5, demping=0.996):
    """A plucked string (Karplus-Strong): guzheng / pipa colour by brightness."""
    r = np.random.default_rng(seed)
    n = int(dur * SR)
    p = max(2, int(SR / f))
    buf = r.uniform(-1, 1, p)
    buf = low(buf, 800 + 6000 * helder, 1) if p > 8 else buf
    uit = np.zeros(n)
    vorige = 0.0
    for i in range(n):
        v = buf[i % p]
        uit[i] = v
        nieuw = demping * (helder * v + (1 - helder) * 0.5 * (v + vorige))
        vorige = v
        buf[i % p] = nieuw
    uit *= env(n, 0.002, min(dur * 0.4, 0.6), 1.5)
    return uit


def pluk_snel(f, dur, seed, helder=0.5, verval=1.2):
    """A cheaper plucked tone (harmonics with their own decay): for long fast parts."""
    t = t_(dur)
    r = np.random.default_rng(seed)
    x = np.zeros_like(t)
    for k in range(1, 9):
        a = (1.0 / k) * (helder ** (k - 1) * 0.9 + 0.1) * (1 + 0.1 * r.normal())
        x += a * np.sin(2 * np.pi * f * k * t * (1 + 0.0007 * k * k)) * np.exp(-t * verval * (1 + 0.6 * k))
    aanslag = high(r.normal(0, 1, len(t)), 2000) * np.exp(-t * 120) * 0.15
    return (x + aanslag) * env(len(t), 0.002, min(0.08, dur / 3), 1.0)


def fluit(f, dur, seed, adem=0.18, vibrato=5.2, diepte=0.006, glij_van=None, glij_tijd=0.12):
    """A breathy bamboo flute (xiao / dizi): sine + a little 2nd and 3rd, breath noise, vibrato that comes in late."""
    t = t_(dur)
    r = np.random.default_rng(seed)
    vib_env = np.clip((t - 0.35) / 0.6, 0, 1)
    fr = f * (1 + diepte * vib_env * np.sin(2 * np.pi * vibrato * t + r.uniform(0, 6)))
    if glij_van:
        g = np.clip(t / glij_tijd, 0, 1)
        fr = fr * (glij_van / f) ** (1 - g)
    fase = 2 * np.pi * np.cumsum(fr) / SR
    toon = np.sin(fase) + 0.22 * np.sin(2 * fase) + 0.07 * np.sin(3 * fase)
    lucht = band(r.normal(0, 1, len(t)), f * 0.9, f * 3.5) * adem
    zwel = env(len(t), min(0.12, dur * 0.3), min(0.35, dur * 0.4), 1.6) * (0.85 + 0.15 * np.sin(np.pi * np.clip(t / dur, 0, 1)))
    return (toon + lucht) * zwel


def klok(f, dur, seed=1, helder=1.0):
    """A bell (inharmonic partials, long decay)."""
    t = t_(dur)
    partials = [(0.5, 0.6, 0.8), (1.0, 1.0, 1.0), (1.19, 0.5, 1.4), (1.56, 0.35, 1.8), (2.0, 0.4, 2.2), (2.74, 0.2 * helder, 3.0),
                (3.76, 0.12 * helder, 4.0), (5.4, 0.06 * helder, 6.0)]
    x = np.zeros_like(t)
    for mul, a, v in partials:
        x += a * np.sin(2 * np.pi * f * mul * t) * np.exp(-t * v / max(0.2, dur / 4))
    return x * env(len(t), 0.003, 0.05, 1)


def belletje(f, dur=0.6, seed=1):
    """A small bright bell / ting (FM)."""
    t = t_(dur)
    mod = np.sin(2 * np.pi * f * 3.5 * t) * 2.2 * np.exp(-t * 9)
    return np.sin(2 * np.pi * f * t + mod) * np.exp(-t * 6) * env(len(t), 0.001, 0.05)


def trom(f0, dur, seed, ruis=0.3, val=0.5):
    """A drum (taiko / tom): a falling sine + a little noise."""
    t = t_(dur)
    r = np.random.default_rng(seed)
    fr = f0 * (1 + val * np.exp(-t * 30))
    x = np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.exp(-t * (4.0 / dur))
    x += low(r.normal(0, 1, len(t)), 1500) * np.exp(-t * 40) * ruis
    return x * env(len(t), 0.001, 0.03)


def tik(dur=0.08, seed=1, hoog=3000):
    """A woodblock / click."""
    t = t_(dur)
    r = np.random.default_rng(seed)
    x = np.sin(2 * np.pi * hoog * 0.4 * t) * np.exp(-t * 90) + band(r.normal(0, 1, len(t)), hoog * 0.6, hoog * 1.6) * np.exp(-t * 70) * 0.6
    return x


def hihat(dur=0.06, seed=1):
    t = t_(dur)
    r = np.random.default_rng(seed)
    return high(r.normal(0, 1, len(t)), 7000) * np.exp(-t * 60)


def kazoo(f, dur, seed=1):
    """A buzzing kazoo-ish voice (for the birthday stinger)."""
    t = t_(dur)
    fr = f * (1 + 0.012 * np.sin(2 * np.pi * 6 * t))
    fase = 2 * np.pi * np.cumsum(fr) / SR
    x = np.sign(np.sin(fase)) * 0.4 + 0.6 * ((fase / np.pi) % 2 - 1)
    x = band(x, 300, 3500)
    return x * env(len(t), 0.02, 0.06)


def koper(f, dur, seed=1, scheef=0.0):
    """A brass-ish note (sawtooth, filtered by its own swell)."""
    t = t_(dur)
    fr = f * (1 + scheef * np.exp(-t * 4)) * (1 + 0.004 * np.sin(2 * np.pi * 5.5 * t))
    fase = np.cumsum(fr) / SR
    x = 2 * (fase % 1) - 1
    zwel = env(len(t), 0.04, 0.08, 1)
    helder = low(x, 1500, 1) * 0.6 + low(x, 4000, 1) * 0.4 * zwel
    return helder * zwel


def strijk(f, dur, seed=1, vibrato=5.5):
    """A bowed string (erhu-ish): a soft saw with vibrato and a slow bow."""
    t = t_(dur)
    vib = np.clip((t - 0.25) / 0.5, 0, 1)
    fr = f * (1 + 0.008 * vib * np.sin(2 * np.pi * vibrato * t))
    fase = np.cumsum(fr) / SR
    x = 2 * (fase % 1) - 1
    x = band(x, f * 0.8, 3500)
    return x * env(len(t), 0.12, 0.2, 1.5)


def melodie(noten, bpm, inst, seed=1, start=0.0, **kw):
    """(note or None, beats) list -> (offset, signal) parts for mix(): each note rings a little longer than its beats."""
    tel = 60.0 / bpm
    t = start
    delen = []
    for i, (noot, tellen) in enumerate(noten):
        if noot:
            dur = tellen * tel
            delen.append((t, inst(hz(noot), dur * 1.15 + 0.05, seed + i, **kw)))
        t += tellen * tel
    return delen, t


# =====================================================================================================================
# Miroh's lullaby (own melody, own lyrics: LuTenLied.java; D minor pentatonic: D F G A C)
# =====================================================================================================================
LU_TEN_BPM = 72
# (note, beats) in 3/4; one bar = 3 beats = 2.5 s. Written for this mod: rocking, falling phrases that rest on D.
LU_TEN_INTRO = [("A4", 2), ("G4", 1), ("F4", 2), ("D4", 1), ("F4", 1), ("G4", 1), ("A4", 1), ("D4", 3)]
LU_TEN_COUPLET = [
    ("D4", 1), ("F4", 1), ("G4", 1), ("A4", 2), ("C5", 1),          # line 1 (2 bars)
    ("A4", 2), ("G4", 1), ("F4", 3),                                # line 2
    ("A4", 1), ("C5", 1), ("D5", 1), ("C5", 2), ("A4", 1),          # line 3
    ("G4", 1), ("F4", 1), ("D4", 1), ("C4", 1), ("D4", 2),          # line 4
]
LU_TEN_TUSSEN = [("C5", 2), ("A4", 1), ("G4", 3), ("A4", 1), ("G4", 1), ("F4", 1), ("G4", 3)]
LU_TEN_SLOT = [("A4", 2), ("G4", 1), ("F4", 2), ("D4", 1), ("F4", 1), ("G4", 1), ("A4", 1), ("G4", 3), ("F4", 2), ("D4", 1), ("D4", 6)]
# intro (bars 0-3), verse (4-11), interlude (12-15), verse again (16-23), the hummed end (24-30): 93 beats, 77.5 s
LU_TEN = LU_TEN_INTRO + LU_TEN_COUPLET + LU_TEN_TUSSEN + LU_TEN_COUPLET + LU_TEN_SLOT
# where the sung lines start (bar index from 0) and how many bars they last: LuTenLied.java holds the same numbers
REGELS = [(4, 2), (6, 2), (8, 2), (10, 2), (16, 2), (18, 2), (20, 2), (22, 2)]


def lu_ten():
    delen, eind = melodie(LU_TEN, LU_TEN_BPM, fluit, seed=700, adem=0.16, vibrato=4.6, diepte=0.007)
    # soft slides into the notes: each part starts with a little glide from the note before (done by fluit's glij_van)
    x = mix(eind + 3.0, *delen)
    x = galm(x, tijd=3.2, nat=0.35, seed=7)
    x = x[:int((eind + 4.0) * SR)]
    x *= env(len(x), 0.6, 3.0, 1.2)
    return norm(x, 0.33)        # about -21 dB RMS: as loud as the real moments


# =====================================================================================================================
# the tunes
# =====================================================================================================================
def theehuis():
    """Cosy guzheng in G major pentatonic, gentle rolling arpeggios and a little tune; 24 s, loops."""
    bpm = 84
    tel = 60 / bpm
    delen = []
    akkoorden = [["G3", "D4", "G4", "B4"], ["E3", "B3", "E4", "G4"], ["C3", "G3", "C4", "E4"], ["D3", "A3", "D4", "F#4"]]
    t = 0.0
    for herhaal in range(2):
        for a in akkoorden:
            for i, n in enumerate(a + a[2:0:-1]):
                delen.append((t + i * tel / 2, pluk_snel(hz(n), 1.6, 10 + i, helder=0.55, verval=1.4) * 0.5))
            t += 4 * tel
    wijs = [("D5", 1), ("E5", 0.5), ("D5", 0.5), ("B4", 1), ("A4", 1), ("G4", 1.5), ("A4", 0.5), ("B4", 2), (None, 1),
            ("E5", 1), ("D5", 0.5), ("B4", 0.5), ("A4", 1), ("G4", 1), ("A4", 2), (None, 2),
            ("B4", 1), ("D5", 1), ("E5", 1), ("G5", 1), ("E5", 1.5), ("D5", 0.5), ("B4", 2),
            ("A4", 1), ("B4", 0.5), ("A4", 0.5), ("G4", 1), ("E4", 1), ("G4", 3), (None, 1)]
    w, _ = melodie(wijs, bpm, lambda f, d, s: pluk_snel(f, d + 0.6, s, helder=0.75, verval=1.0) * 0.8, seed=40, start=4 * tel)
    lengte = 32 * tel
    x = mix(lengte + 1.5, *(delen + w))
    x = galm(x, 1.4, 0.2, 3)[:int(lengte * SR)]
    return norm(x * env(len(x), 0.01, 0.4, 1), 0.7)


def thee_ting():
    x = belletje(hz("E6"), 1.4) * 0.8 + belletje(hz("B6"), 1.4) * 0.35
    x = mix(1.6, (0, x), (0.12, belletje(hz("E7"), 0.8) * 0.15))
    return norm(galm(x, 0.8, 0.2, 4)[:int(1.8 * SR)], 0.6)


def pai_sho():
    """Calm: sparse low plucks and soft bells in A minor pentatonic; 20 s, loops."""
    bpm = 60
    tel = 1.0
    noten = [("A3", 2), ("E4", 1), ("D4", 1), ("C4", 2), ("A3", 2), ("G3", 2), ("A3", 1), ("C4", 1), ("D4", 2), ("E4", 2),
             ("A3", 2), ("E4", 1), ("G4", 1), ("E4", 2), ("D4", 2)]
    d, eind = melodie(noten, bpm, lambda f, dd, s: pluk_snel(f, dd + 1.5, s, helder=0.4, verval=0.8) * 0.7, seed=60)
    bellen = [(i * 5.0 + 2.5, belletje(hz(n), 2.0) * 0.12) for i, n in enumerate(["E6", "A5", "D6", "E6"])]
    x = mix(20 + 2, *(d + bellen))
    x = galm(x, 2.0, 0.3, 5)[:int(20 * SR)]
    return norm(x * env(len(x), 0.02, 0.6, 1), 0.65)


def ritme_tune(bpm, maten, instr, bas, wijs, seed, swing=0.0, trommen=True, zwaar=False):
    """A tune on a fixed beat: the first beat is sample 0 (the rhythm games count from tick 0 of the sound)."""
    tel = 60.0 / bpm
    lengte = maten * 4 * tel
    delen = []
    for b in range(maten * 4):
        t = b * tel
        if trommen:
            if b % 4 == 0 or (zwaar and b % 4 == 2):
                delen.append((t, trom(58 if zwaar else 70, 0.5, seed + b, ruis=0.25) * (0.9 if zwaar else 0.75)))
            if b % 2 == 1:
                delen.append((t, tik(0.1, seed + b, 2200) * 0.35))
            delen.append((t + tel / 2 + swing * tel, hihat(0.05, seed + b) * 0.12))
    bas_d, _ = melodie(bas * (maten * 4 // max(1, sum(n[1] for n in bas))), bpm,
                       lambda f, d, s: pluk_snel(f, d, s, helder=0.25, verval=2.0) * 0.6, seed=seed + 300)
    wijs_d, _ = melodie(wijs * max(1, (maten * 4) // max(1, int(sum(n[1] for n in wijs)))), bpm, instr, seed=seed + 600)
    x = mix(lengte, *(delen + bas_d + wijs_d))
    x = galm(x, 0.9, 0.12, seed)[:int(lengte * SR)]
    return norm(x * env(len(x), 0.001, 0.15, 1), 0.75)


def dansfeest():
    """The secret dance party in the Fire Nation cave: 100 BPM, 16 bars, a stomping drum and a bright pipa-ish tune."""
    bas = [("D2", 1), ("D2", 1), ("F2", 1), ("G2", 1), ("A2", 1), ("A2", 1), ("G2", 1), ("F2", 1)]
    wijs = [("D5", 0.5), ("F5", 0.5), ("G5", 1), ("A5", 0.5), ("G5", 0.5), ("F5", 1), ("D5", 0.5), ("C5", 0.5), ("D5", 1), (None, 1),
            ("A4", 0.5), ("C5", 0.5), ("D5", 1), ("F5", 1), ("G5", 0.5), ("F5", 0.5), ("D5", 2), (None, 1)]
    return ritme_tune(100, 16, lambda f, d, s: pluk_snel(f, d + 0.2, s, helder=0.85, verval=2.5) * 0.7, bas, wijs, 900, zwaar=True)


def waaierdans():
    """The Kyoshi fan dance: 90 BPM, 16 bars, elegant; soft drum, flute tune."""
    bas = [("A2", 2), ("E3", 2), ("G2", 2), ("D3", 2)]
    wijs = [("E5", 1), ("G5", 1), ("A5", 2), ("G5", 1), ("E5", 1), ("D5", 2), ("C5", 1), ("D5", 1), ("E5", 1), ("G5", 1), ("E5", 4)]
    return ritme_tune(90, 16, lambda f, d, s: fluit(f, d, s, adem=0.12) * 0.55, bas, wijs, 1100)


def ritmeduel():
    """Piandao's rhythm duel: 80 BPM, 16 bars, taiko and a calling flute."""
    bas = [("E2", 1), (None, 1), ("E2", 1), ("G2", 1)]
    wijs = [("E5", 1), (None, 1), ("G5", 1), ("A5", 1), ("B5", 2), ("A5", 1), (None, 1), ("G5", 1), ("E5", 1), ("D5", 1), ("E5", 1), (None, 4)]
    return ritme_tune(80, 16, lambda f, d, s: fluit(f, d, s, adem=0.2) * 0.5, bas, wijs, 1300, zwaar=True)


def loopje(bpm, maten, wijs, bas, inst, seed, extra=None):
    """A short looping minigame tune: drums light, bass, tune; `extra(t, beat)` adds a part per beat."""
    tel = 60.0 / bpm
    lengte = maten * 4 * tel
    delen = []
    for b in range(maten * 4):
        t = b * tel
        if b % 2 == 0:
            delen.append((t, trom(90, 0.25, seed + b, ruis=0.2) * 0.5))
        else:
            delen.append((t, tik(0.08, seed + b, 2600) * 0.3))
        if extra:
            delen += extra(t, b)
    nb = int(sum(n[1] for n in bas))
    bas_d, _ = melodie(bas * max(1, (maten * 4) // nb), bpm, lambda f, d, s: pluk_snel(f, d, s, helder=0.3, verval=2.2) * 0.55, seed=seed + 200)
    nw = int(sum(n[1] for n in wijs))
    wijs_d, _ = melodie(wijs * max(1, (maten * 4) // nw), bpm, inst, seed=seed + 400)
    x = mix(lengte, *(delen + bas_d + wijs_d))
    x = galm(x, 0.7, 0.1, seed)[:int(lengte * SR)]
    return norm(x * env(len(x), 0.001, 0.1, 1), 0.72)


def luchtscooter():
    wijs = [("G5", 0.5), ("A5", 0.5), ("B5", 0.5), ("D6", 0.5), ("B5", 1), ("A5", 0.5), ("G5", 0.5), ("E5", 1), ("G5", 1),
            ("A5", 0.5), ("B5", 0.5), ("A5", 0.5), ("G5", 0.5), ("E5", 1), ("D5", 1)]
    bas = [("G2", 1), ("D3", 1), ("E3", 1), ("D3", 1)]
    return loopje(132, 8, wijs, bas, lambda f, d, s: fluit(f, d, s, adem=0.1) * 0.5, 1500)


def momo():
    """Chasing Momo: quick, cheeky, staccato plucks running up and down."""
    wijs = [("E5", 0.25), ("G5", 0.25), ("A5", 0.25), ("G5", 0.25), ("E5", 0.5), ("D5", 0.5), ("E5", 0.25), ("G5", 0.25), ("B5", 0.5),
            ("A5", 0.5), (None, 0.5), ("C6", 0.25), ("B5", 0.25), ("A5", 0.25), ("G5", 0.25), ("E5", 0.5), ("D5", 0.5), ("E5", 1)]
    bas = [("A2", 0.5), ("A2", 0.5), ("E3", 0.5), ("A2", 0.5)]
    return loopje(144, 8, wijs, bas, lambda f, d, s: pluk_snel(f, min(d, 0.3) + 0.15, s, helder=0.9, verval=6) * 0.7, 1700)


def slee():
    """Sledding: sleigh-bell shakes on every beat, a bright running tune."""
    def bellen(t, b):
        return [(t + i * 0.06, belletje(hz(n), 0.25, b) * 0.08) for i, n in enumerate(["A6", "E7", "C#7"])]
    wijs = [("A5", 0.5), ("B5", 0.5), ("C#6", 1), ("E6", 1), ("C#6", 0.5), ("B5", 0.5), ("A5", 1), ("F#5", 1), ("E5", 2)]
    bas = [("A2", 1), ("E3", 1), ("D3", 1), ("E3", 1)]
    return loopje(126, 8, wijs, bas, lambda f, d, s: pluk_snel(f, d + 0.2, s, helder=0.7, verval=2) * 0.6, 1900, extra=bellen)


def monorail():
    """The earth-bent monorail: a chugging rhythm and a low striding tune."""
    def tjoek(t, b):
        return [(t + 0.25 * 60 / 112, tik(0.07, b, 1200) * 0.25)]
    wijs = [("D4", 1), ("F4", 0.5), ("G4", 0.5), ("A4", 1), ("G4", 1), ("F4", 1), ("D4", 1), ("C4", 1), ("D4", 1)]
    bas = [("D2", 1), ("D2", 1), ("A2", 1), ("C3", 1)]
    return loopje(112, 8, wijs, bas, lambda f, d, s: strijk(f, d, s) * 0.45, 2100, extra=tjoek)


def glijbaan():
    """The chute race of Omashguh: whooshing glides up and down between bouncy notes; 12 s."""
    lengte = 12.0
    delen = []
    for i in range(6):
        t0 = i * 2.0
        n = int(1.2 * SR)
        tt = np.arange(n) / SR
        fr = 400 * 2 ** (np.sin(np.pi * tt / 1.2) * (1.5 if i % 2 == 0 else -1.0))
        glij = np.sin(2 * np.pi * np.cumsum(fr) / SR) * env(n, 0.05, 0.3) * 0.25
        delen.append((t0, glij))
        for j, nn in enumerate(["C5", "E5", "G5"] if i % 2 == 0 else ["G5", "E5", "C5"]):
            delen.append((t0 + 1.25 + j * 0.2, pluk_snel(hz(nn), 0.5, i * 3 + j, helder=0.8, verval=4) * 0.5))
        delen.append((t0, trom(80, 0.3, i, 0.2) * 0.4))
    x = mix(lengte, *delen)
    return norm(galm(x, 0.6, 0.1, 9)[:int(lengte * SR)] * env(int(lengte * SR), 0.001, 0.2), 0.7)


# =====================================================================================================================
# the stingers
# =====================================================================================================================
def kolen():
    """'MIJN KOLEN!': a cart tipping over (a crash of wooden knocks), then a long sad sliding trombone."""
    delen = [(0.0 + i * 0.07, tik(0.12, i, 900 + 300 * (i % 3)) * (0.7 - i * 0.06)) for i in range(8)]
    delen.append((0.0, trom(60, 0.6, 3, 0.6) * 0.8))
    for i, (f0, f1) in enumerate([(hz("Bb3"), hz("A3")), (hz("A3"), hz("Ab3")), (hz("Ab3"), hz("G3"))]):
        delen.append((0.75 + i * 0.45, koper(f0, 0.5 if i < 2 else 1.4, i, scheef=(f1 / f0 - 1) * -1)))
    x = mix(3.2, *delen)
    return norm(galm(x, 0.8, 0.15, 11)[:int(3.4 * SR)], 0.8)


def toneel_fanfare():
    """The Sintelstrand players' proud fanfare: grand, and the last note one step too far (on purpose)."""
    noten = [("C4", 0.5), ("C4", 0.25), ("C4", 0.25), ("G4", 1), ("E4", 0.5), ("G4", 0.5), ("C5", 1.5), ("Db5", 1.5)]
    d, eind = melodie(noten, 110, lambda f, dd, s: (koper(f, dd, s) + 0.6 * koper(f * 1.5, dd, s + 9)) * 0.5, seed=1500)
    d.append((0.0, trom(55, 0.8, 1, 0.4)))
    d.append((eind - 1.5, klok(hz("C3"), 2.0, 1) * 0.3))
    x = mix(eind + 1.0, *d)
    return norm(galm(x, 1.2, 0.2, 12), 0.8)


def feestje():
    """'Happy Birthday' (melody public domain) on a toy piano and a kazoo, a bit too fast: the party in the wrong dimension."""
    hb = [("G4", 0.75), ("G4", 0.25), ("A4", 1), ("G4", 1), ("C5", 1), ("B4", 2),
          ("G4", 0.75), ("G4", 0.25), ("A4", 1), ("G4", 1), ("D5", 1), ("C5", 2),
          ("G4", 0.75), ("G4", 0.25), ("G5", 1), ("E5", 1), ("C5", 1), ("B4", 1), ("A4", 1),
          ("F5", 0.75), ("F5", 0.25), ("E5", 1), ("C5", 1), ("D5", 1), ("C5", 2)]
    piano, eind = melodie(hb, 168, lambda f, d, s: (belletje(f, d + 0.3, s) * 0.5 + pluk_snel(f, d + 0.3, s, 0.9, 3) * 0.5), seed=1600)
    kz, _ = melodie(hb, 168, lambda f, d, s: kazoo(f, d * 0.9, s) * 0.35, seed=1650)
    toet = (eind, kazoo(hz("C6"), 0.6, 1) * 0.5)
    x = mix(eind + 1.2, *(piano + kz + [toet]))
    return norm(galm(x, 0.7, 0.1, 13), 0.8)


def mijn_eer():
    """'MIJN EER!': a dramatic drum roll and a hit with a low brass stab that slides down."""
    delen = [(i * 0.045, trom(95 + (i % 3) * 8, 0.2, i, 0.4) * (0.2 + i * 0.02)) for i in range(26)]
    delen.append((1.2, trom(45, 1.6, 2, 0.5) * 1.0))
    delen.append((1.2, (koper(hz("D3"), 1.6, 5, scheef=-0.06) + koper(hz("A3"), 1.6, 6, scheef=-0.06) * 0.7) * 0.8))
    delen.append((1.2, klok(hz("D2"), 2.2, 3) * 0.3))
    x = mix(3.2, *delen)
    return norm(galm(x, 1.4, 0.2, 14), 0.85)


def opgelost():
    """A puzzle solved: a rising pentatonic run of bells into a warm chord."""
    delen = [(i * 0.09, belletje(hz(n), 1.0, i) * 0.4) for i, n in enumerate(["C5", "D5", "E5", "G5", "A5", "C6"])]
    for n in ["C4", "E4", "G4", "C5"]:
        delen.append((0.55, pluk_snel(hz(n), 2.2, 3, helder=0.5, verval=0.7) * 0.35))
    x = mix(2.6, *delen)
    return norm(galm(x, 1.2, 0.25, 15), 0.7)


def tegel():
    """A Pai Sho tile laid down: a wooden clack and a soft ting."""
    x = mix(0.9, (0, tik(0.1, 1, 1500) * 0.9), (0.01, low(tik(0.15, 2, 600), 1200) * 0.6), (0.03, belletje(hz("A6"), 0.8) * 0.15))
    return norm(galm(x, 0.5, 0.12, 16)[:int(1.0 * SR)], 0.7)


# =====================================================================================================================
# the Geestenwereld
# =====================================================================================================================
def koi_zang():
    """The singing spirit koi: two slow gliding whale-like tones, high and airy."""
    lengte = 5.0
    t = t_(lengte)
    f = 520 * 2 ** (0.35 * np.sin(2 * np.pi * t / 5.0) + 0.1 * np.sin(2 * np.pi * t * 0.9))
    a = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.3 * np.sin(4 * np.pi * np.cumsum(f) / SR)
    f2 = f * 1.5
    b = np.sin(2 * np.pi * np.cumsum(f2) / SR) * 0.4
    x = (a + b) * env(len(t), 0.8, 1.5, 1.4)
    return norm(galm(x, 2.5, 0.4, 17), 0.6)


def koh_kraak():
    """Koh's creaking: a dry crackling, like a beetle shell turning."""
    r = np.random.default_rng(18)
    lengte = 1.6
    delen = []
    t = 0.0
    while t < lengte - 0.1:
        delen.append((t, band(r.normal(0, 1, int(0.02 * SR)), 900, 4000) * r.uniform(0.3, 1.0)))
        t += r.uniform(0.015, 0.07)
    x = mix(lengte, *delen) * env(int(lengte * SR), 0.05, 0.4)
    return norm(galm(x, 0.9, 0.15, 18), 0.7)


def koh_stilte():
    """Koh's icy silence: a thin high tone and a low hum swell up together and then stop dead."""
    lengte = 3.4
    t = t_(lengte)
    zwel = np.clip(t / 2.8, 0, 1) ** 2
    x = (np.sin(2 * np.pi * 2637 * t) * 0.25 + np.sin(2 * np.pi * 55 * t) * 0.8 + np.sin(2 * np.pi * 82.4 * t) * 0.3) * zwel
    x[int(2.9 * SR):] = 0
    x *= env(len(t), 0.01, 0.01)
    return norm(x, 0.6)


def lantaarn_tinkel():
    """Lantern spirits tinkling: a few tiny bells, scattered."""
    r = np.random.default_rng(19)
    noten = ["E6", "G6", "A6", "C7", "D7"]
    delen = [(r.uniform(0, 0.9), belletje(hz(noten[i % 5]), 0.8, i) * r.uniform(0.2, 0.5)) for i in range(7)]
    x = mix(1.8, *delen)
    return norm(galm(x, 1.0, 0.3, 19), 0.6)


def klok_geest():
    """The Geestenklok: one deep temple bell with a long hum."""
    x = mix(7.0, (0, klok(hz("D2"), 7.0, 1, helder=0.8)), (0, 0.4 * klok(hz("D3"), 5.0, 2, helder=0.5)))
    return norm(galm(x, 3.0, 0.3, 20), 0.8)


def geestentij():
    """The spirit tide: a wide whooshing swell, rising and falling twice."""
    r = np.random.default_rng(21)
    lengte = 6.5
    t = t_(lengte)
    ruis = r.normal(0, 1, len(t))
    golf = (np.sin(np.pi * t / lengte * 2) ** 2)
    x = band(ruis, 200, 1800) * golf + 0.2 * np.sin(2 * np.pi * 110 * t) * golf
    return norm(galm(x * env(len(t), 0.5, 0.8), 2.0, 0.3, 21), 0.6)


def maannacht():
    """A moon night: a shimmering high pad of slowly beating tones."""
    lengte = 6.5
    t = t_(lengte)
    x = np.zeros_like(t)
    for f, a in [(hz("A5"), 0.4), (hz("E6"), 0.3), (hz("A6"), 0.2), (hz("C#6"), 0.15)]:
        x += a * np.sin(2 * np.pi * f * t) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.4 * t + f))
        x += a * 0.6 * np.sin(2 * np.pi * (f * 1.003) * t)
    return norm(galm(x * env(len(t), 1.5, 2.0, 1.4), 2.5, 0.35, 22), 0.55)


def reiger():
    """The heron spirit: a soft breathy two-note call that bends down."""
    a = fluit(hz("B5"), 0.6, 1, adem=0.5, glij_van=hz("D6"), glij_tijd=0.3)
    b = fluit(hz("F#5"), 0.9, 2, adem=0.5, glij_van=hz("A5"), glij_tijd=0.5)
    x = mix(2.2, (0, a), (0.7, b))
    return norm(galm(x, 1.5, 0.35, 23), 0.6)


EIGEN = {
    "moment_lu_ten": lu_ten,
    "deuntje_theehuis": theehuis, "deuntje_thee_ting": thee_ting, "deuntje_pai_sho": pai_sho, "deuntje_luchtscooter": luchtscooter,
    "deuntje_momo": momo, "deuntje_slee": slee, "deuntje_monorail": monorail, "deuntje_glijbaan": glijbaan,
    "deuntje_dansfeest": dansfeest, "deuntje_waaierdans": waaierdans, "deuntje_ritmeduel": ritmeduel,
    "stinger_kolen": kolen, "stinger_toneel_fanfare": toneel_fanfare, "stinger_feestje": feestje, "stinger_mijn_eer": mijn_eer,
    "stinger_opgelost": opgelost, "stinger_tegel": tegel,
    "geest_koi_zang": koi_zang, "geest_koh_kraak": koh_kraak, "geest_koh_stilte": koh_stilte, "geest_lantaarn_tinkel": lantaarn_tinkel,
    "geest_klok": klok_geest, "geest_geestentij": geestentij, "geest_maannacht": maannacht, "geest_reiger": reiger,
}


# =====================================================================================================================
# the real pieces
# =====================================================================================================================
def muziek(bron, alleen=None):
    sf = _sf()
    s = _sig()
    ledger = json.load(open(LEDGER, encoding="utf-8"))
    for stuk in ledger["stukken"]:
        uit = stuk["naam"].replace(".", "_")
        if alleen and uit not in alleen:
            continue
        pad = os.path.join(bron, stuk["bestand"])
        x, sr = sf.read(pad, dtype="float32", always_2d=True)
        x = x.mean(axis=1)
        if sr != SR:
            x = s.resample_poly(x, SR, sr).astype(np.float32)
        knip = stuk.get("knip")
        if knip:
            x = x[int(knip[0] * SR):int(knip[1] * SR)]
            x = x * env(len(x), 0.01, 6.0, 1.3).astype(np.float32)
        # trim digital silence at both ends (keeps 0.1 s)
        stil = np.where(np.abs(x) > 1e-3)[0]
        if len(stil):
            x = x[max(0, stil[0] - int(0.1 * SR)):min(len(x), stil[-1] + int(0.1 * SR))]
        # loudness: the RMS of the loudest half of the piece to the target, then a soft ceiling
        blok = int(0.4 * SR)
        rms = np.sqrt(np.array([np.mean(x[i:i + blok] ** 2) for i in range(0, len(x) - blok, blok)]) + 1e-12)
        luid = np.sqrt(np.mean(np.sort(rms)[len(rms) // 2:] ** 2))
        x = x * (10 ** (DOEL_RMS_DB / 20) / luid)
        boven = np.abs(x) > PLAFOND * 0.8
        x[boven] = np.sign(x[boven]) * (PLAFOND * 0.8 + (PLAFOND * 0.2) * np.tanh((np.abs(x[boven]) - PLAFOND * 0.8) / (PLAFOND * 0.2)))
        save(uit, x, NIVEAU_MUZIEK)


def main(argv):
    if len(argv) > 1 and argv[1] == "--muziek":
        if len(argv) < 3:
            raise SystemExit("usage: einde2_audio_geluid.py --muziek <map met de bronbestanden> [naam ...]")
        muziek(argv[2], set(argv[3:]) or None)
        return
    alleen = set(argv[1:])
    for naam, maak in EIGEN.items():
        if not alleen or naam in alleen:
            save(naam, maak())


if __name__ == "__main__":
    main(sys.argv)
