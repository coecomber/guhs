"""
1.10 fix round 2 (I-AUDIO): the town music and the town ambience beds of Het Vadsrijk, synthesized (OGG Vorbis, mono).

  assets/guhs/sounds/vadsrijk/stad/<stad>_1, <stad>_2   two sweet tunes per town (about 80-100 s, each ends on the tonic
                                                        with a reverb tail; the client plays them one after the other):
      zwamzoet    cosy mushroom-town waltz        music box / celesta, plucked bass, soft chord plucks, warm pad
      knuffelkamp morning-camp folk               plucked guitar arpeggios, a little flute, a shaker, a soft drum
      haven       gentle harbour shanty (6/8)     flute + celesta, plucked bass, a soft concertina pad, a frame drum
      kandijkom   winter music box                music box + glockenspiel, sleigh bells, a soft pad
      honingtak   honey waltz (zone 3, K-INT)     music box + pluck, pizzicato, honey-drip glockenspiel, a little bee hum
  assets/guhs/sounds/vadsrijk/stad/bed_<naam>           seamless 20 s ambience loops, quiet under the tunes (fix round L: no
                                                        wind anymore): golfjes (soft water lapping at the jetty), briesje (a
                                                        sweet garden bed: birdsong + distant chimes), haardvuur (a warm
                                                        crackling hearth + soft sleigh bells)

Not part of make_resources.py: the OGGs are committed like the other sounds; vadsrijk_stadsmuziek.py writes sounds.json and
checks the files. Fixed seeds, so a rerun gives the same music. Needs numpy, scipy and soundfile:
    python tools/features/vadsrijk_stadsmuziek_geluid.py        (from the project root)
"""
import os

import numpy as np
try:                                    # only needed to MAKE the sounds
    import soundfile as sf
    from scipy.signal import butter, fftconvolve, lfilter
except ImportError:
    sf = butter = fftconvolve = lfilter = None

SR = 44100
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "vadsrijk", "stad")


# ---------------------------------------------------------------- helpers

def hz(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def low(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="low")
    return lfilter(b, a, x)


def high(x, f, order=2):
    b, a = butter(order, f / (SR / 2), btype="high")
    return lfilter(b, a, x)


def band(x, lo, hi, order=2):
    b, a = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def save(name, x, kwaliteit=0.55):
    os.makedirs(OUT, exist_ok=True)
    x = np.clip(x, -0.98, 0.98).astype(np.float32)
    pad = os.path.join(OUT, name + ".ogg")
    # (in pieces: libsndfile's Vorbis writer falls over on one long buffer); compression_level 0..1 = best..smallest
    with sf.SoundFile(pad, "w", SR, 1, format="OGG", subtype="VORBIS", compression_level=kwaliteit) as f:
        for a in range(0, len(x), SR // 2):
            f.write(x[a:a + SR // 2])
    rms = 20 * np.log10(np.sqrt(np.mean(x ** 2)) + 1e-9)
    print(f"wrote {name:16s} {len(x) / SR:5.1f}s  peak {np.max(np.abs(x)):.2f}  rms {rms:5.1f} dB  {os.path.getsize(pad) // 1024} kB")


def niveau(x, rms_db, piek=0.85):
    """Scale to an RMS level, then soft-limit the peaks (so every tune is equally gentle)."""
    x = x - np.mean(x)
    x = x * (10 ** (rms_db / 20) / (np.sqrt(np.mean(x ** 2)) + 1e-12))
    return np.tanh(x / piek) * piek


def galm(sec, seed, helder=4200):
    r = np.random.default_rng(seed)
    n = int(sec * SR)
    t = np.arange(n) / SR
    ir = r.standard_normal(n) * np.exp(-t / (sec * 0.2))
    ir[: int(0.015 * SR)] = 0
    ir = low(ir, helder)
    return ir / np.sqrt(np.sum(ir ** 2) + 1e-9)


def env_ar(n, a, d):
    t = np.arange(n) / SR
    return (1 - np.exp(-t / max(a, 1e-4))) * np.exp(-t / d)


# ---------------------------------------------------------------- instruments (all soft: sines, filtered plucks, no hard edges)

def speeldoos(m, dur, r):
    """Music box / celesta: a bright sine with a quick shimmer of overtones, long soft decay."""
    n = int((dur + 1.6) * SR)
    t = np.arange(n) / SR
    f = hz(m) * (1 + r.uniform(-0.001, 0.001))
    x = (np.sin(2 * np.pi * f * t)
         + 0.35 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 6)
         + 0.12 * np.sin(2 * np.pi * 4.02 * f * t) * np.exp(-t * 14)
         + 0.05 * np.sin(2 * np.pi * 5.4 * f * t) * np.exp(-t * 22))
    return x * env_ar(n, 0.002, 0.55 + 0.25 * (72 / max(m, 40)))


def glock(m, dur, r):
    n = int((dur + 1.4) * SR)
    t = np.arange(n) / SR
    f = hz(m)
    x = np.sin(2 * np.pi * f * t) + 0.25 * np.sin(2 * np.pi * 2.76 * f * t) * np.exp(-t * 9) + 0.08 * np.sin(2 * np.pi * 5.4 * f * t) * np.exp(-t * 18)
    return x * env_ar(n, 0.001, 0.7)


def pluk(m, dur, r, helder=0.5, demping=0.996):
    """Karplus-Strong plucked string, block-vectorised per period, warm (low-passed) and soft."""
    f = hz(m)
    N = max(2, int(round(SR / f)))
    n = int((dur + 0.8) * SR)
    y = np.zeros(n + N + 1)
    ruis = r.uniform(-1, 1, N)
    ruis = np.convolve(ruis, np.ones(3) / 3, mode="same") * (0.5 + helder)
    y[:N] = ruis
    i = N
    while i < n + N:
        j = min(i + N, n + N)
        a = y[i - N:j - N]
        b = y[i - N - 1:j - N - 1] if i - N - 1 >= 0 else np.concatenate(([0.0], y[i - N:j - N - 1]))
        y[i:j] = demping * 0.5 * (a + b)
        i = j
    x = y[N:N + n]
    t = np.arange(n) / SR
    x = x * np.minimum(1, t / 0.004) * np.where(t > dur, np.exp(-(t - dur) / 0.25), 1)
    return low(x, 3000)


def fluit(m, dur, r, vibrato=0.006):
    """A little flute: sine + a few soft harmonics, a breathy start, a delayed gentle vibrato."""
    n = int((dur + 0.25) * SR)
    t = np.arange(n) / SR
    f = hz(m)
    vib = 1 + vibrato * np.sin(2 * np.pi * 5.0 * t) * np.clip((t - 0.18) / 0.3, 0, 1)
    fase = 2 * np.pi * np.cumsum(f * vib) / SR
    x = np.sin(fase) + 0.18 * np.sin(2 * fase) + 0.06 * np.sin(3 * fase)
    adem = band(r.standard_normal(n), f * 0.8, min(f * 3.5, SR / 2 - 100)) * 0.06
    att = np.clip(t / 0.06, 0, 1) ** 1.5
    rel = np.clip((dur + 0.25 - t) / 0.25, 0, 1)
    return (x + adem * (0.4 + 0.6 * np.exp(-t * 6))) * att * rel * 0.8


def pad(midis, dur, r, kleur=900):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for m in midis:
        for det in (-0.004, 0.0, 0.004):
            x += np.sin(2 * np.pi * hz(m) * (1 + det) * t + r.uniform(0, 6.28))
    env = np.clip(t / 0.4, 0, 1) * np.clip((dur - t) / 0.5, 0, 1)
    return low(x, kleur) * env / (3 * len(midis))


def accordeon(midis, dur, r):
    """A soft concertina chord: band-limited reedy tone (a few harmonics), heavily low-passed, swelling bellows."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for m in midis:
        for det in (-0.003, 0.003):
            f = hz(m) * (1 + det)
            for k, a in ((1, 1.0), (2, 0.45), (3, 0.25), (4, 0.12), (5, 0.06)):
                x += a * np.sin(2 * np.pi * k * f * t)
    blaas = 0.75 + 0.25 * np.sin(np.pi * np.clip(t / dur, 0, 1))
    env = np.clip(t / 0.12, 0, 1) * np.clip((dur - t) / 0.15, 0, 1)
    return low(x, 1400) * env * blaas / (4 * len(midis))


def bas(m, dur, r):
    n = int((dur + 0.4) * SR)
    t = np.arange(n) / SR
    f = hz(m)
    x = np.sin(2 * np.pi * f * t) + 0.25 * np.sin(4 * np.pi * f * t) * np.exp(-t * 4)
    return x * env_ar(n, 0.006, 0.6) * np.where(t > dur, np.exp(-(t - dur) / 0.08), 1)


def shaker(r, sterk=1.0):
    n = int(0.09 * SR)
    t = np.arange(n) / SR
    return high(r.standard_normal(n), 5500) * np.sin(np.pi * t / t[-1]) ** 2 * 0.25 * sterk


def trommel(r, sterk=1.0):
    """A soft frame drum: a falling low sine plus a little skin noise."""
    n = int(0.45 * SR)
    t = np.arange(n) / SR
    f = 95 + 60 * np.exp(-t * 30)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 9)
    x += low(r.standard_normal(n), 900) * np.exp(-t * 40) * 0.25
    return x * 0.7 * sterk


def tik(r, sterk=1.0):
    n = int(0.06 * SR)
    t = np.arange(n) / SR
    return band(r.standard_normal(n), 1800, 4500) * np.exp(-t * 70) * 0.3 * sterk


def sleebel(r, sterk=1.0):
    """Sleigh bells: a cluster of tiny high inharmonic bells, slightly scattered in time."""
    n = int(0.35 * SR)
    x = np.zeros(n)
    t = np.arange(n) / SR
    for _ in range(7):
        f = r.uniform(3800, 7200)
        o = int(r.uniform(0, 0.03) * SR)
        tt = t[: n - o]
        x[o:] += np.sin(2 * np.pi * f * tt) * np.exp(-tt * r.uniform(14, 24)) * r.uniform(0.4, 1.0)
    x += band(r.standard_normal(n), 6000, 11000) * np.exp(-t * 25) * 0.3
    return x * 0.12 * sterk


def leg(buf, x, at):
    a = max(0, int(at * SR))
    if a >= len(buf):
        return
    e = min(len(buf), a + len(x))
    buf[a:e] += x[: e - a]


# ---------------------------------------------------------------- composition

MAJ = [0, 2, 4, 5, 7, 9, 11]
GRADEN = {"I": 0, "ii": 1, "iii": 2, "IV": 3, "V": 4, "vi": 5}


def akkoord(toon, graad, octaaf):
    """The triad on a scale degree, as midi numbers; octaaf as in C4 = 60 (the key's tonic of that octave is the base)."""
    d = GRADEN[graad]
    return [toon % 12 + (octaaf + 1) * 12 + MAJ[(d + k) % 7] + 12 * ((d + k) // 7) for k in (0, 2, 4)]


def toonladder(toon, lo, hi):
    return [m for m in range(lo, hi + 1) if (m - toon) % 12 in MAJ]


def melodie(r, toon, akkoorden, ritmes, maat, lo, hi, slot_lang=True, start=None):
    """A singable line over the bars: each bar starts on a chord tone near the previous note, then steps through the scale
    with an arch over the 4-bar phrase; every phrase ends long on a chord tone, the last one on the tonic."""
    ladder = toonladder(toon, lo, hi)
    noten = []
    vorig = start if start is not None else ladder[len(ladder) // 2]
    for b, gr in enumerate(akkoorden):
        tonen = [m for m in ladder if (m - toon) % 12 in [(x - toon) % 12 for x in akkoord(toon, gr, 0)]]
        laatste_frase = b % 4 == 3
        einde = b == len(akkoorden) - 1
        ritme = [(0, maat)] if einde else ([(0, maat - 1 if maat == 3 else maat - 2)] + ([(maat - 1, 1)] if maat == 3 else [(maat - 2, 2)])
                                           if laatste_frase and slot_lang else ritmes[r.integers(len(ritmes))])
        richting = 1 if (b % 4) < 2 else -1
        for i, (s, d) in enumerate(ritme):
            if i == 0:
                kand = sorted(tonen, key=lambda m: abs(m - vorig) + (0 if (m - vorig) * richting >= 0 else 1.5))
                p = kand[0] if r.random() < 0.75 else kand[min(1, len(kand) - 1)]
                if einde and gr == "I":
                    p = min([m for m in ladder if (m - toon) % 12 == 0], key=lambda m: abs(m - vorig))
            else:
                idx = ladder.index(min(ladder, key=lambda m: abs(m - vorig)))
                stap = richting * (1 if r.random() < 0.7 else 2)
                if r.random() < 0.2:
                    stap = -stap
                idx = int(np.clip(idx + stap, 0, len(ladder) - 1))
                p = ladder[idx]
            noten.append((b * maat + s, d, p))
            vorig = p
    return noten


class Stuk:
    def __init__(self, toon, maat, bpm, seed):
        self.toon, self.maat, self.bpm = toon, maat, bpm
        self.r = np.random.default_rng(seed)
        self.tel = 60.0 / bpm

    def compose(self, secties, ritmes, lo, hi):
        """secties: a list of (name, [degrees per bar]); a name that comes back reuses its melody (A ... A)."""
        cache, akk, mel = {}, [], []
        b0 = 0
        vorig = None
        for naam, graden in secties:
            if naam not in cache:
                cache[naam] = melodie(self.r, self.toon, graden, ritmes, self.maat, lo, hi, start=vorig)
            for (s, d, p) in cache[naam]:
                mel.append((b0 * self.maat + s, d, p))
            vorig = cache[naam][-1][2]
            akk += graden
            b0 += len(graden)
        # the very last bar: the tonic, held
        akk.append("I")
        tonic = min([m for m in range(lo, hi + 1) if (m - self.toon) % 12 == 0], key=lambda m: abs(m - vorig))
        mel.append((b0 * self.maat, self.maat, tonic))
        self.akk, self.mel = akk, mel
        self.lengte = (len(akk) * self.maat) * self.tel + 4.0
        return self

    def buf(self):
        return np.zeros(int(self.lengte * SR))

    def t(self, tel):
        # a hint of swing/humanising: tiny timing jitter
        return tel * self.tel + self.r.uniform(-0.006, 0.006)


def mix(stem, ir_sec, seed, nat=0.32, helder=4200):
    ir = galm(ir_sec, seed, helder)
    nat_sig = fftconvolve(stem, ir)[: len(stem)]
    return stem + nat * nat_sig


# ---------------------------------------------------------------- the towns

def zwamzoet(n, seed, toon, bpm):
    """Cosy mushroom-town waltz: music box melody, bass on 1, soft chord plucks on 2 and 3, a warm pad underneath."""
    A = ["I", "vi", "IV", "V", "I", "vi", "ii", "V"]
    B = ["IV", "V", "iii", "vi", "ii", "V", "I", "V"] if n == 1 else ["vi", "IV", "I", "V", "vi", "ii", "IV", "V"]
    C = ["I", "IV", "I", "V", "vi", "IV", "V", "I"]
    s = Stuk(toon, 3, bpm, seed).compose([("A", A), ("A", A), ("B", B), ("C", C), ("A", A), ("C", C)],
                                          [[(0, 2), (2, 1)], [(0, 1), (1, 1), (2, 1)], [(0, 1.5), (1.5, 0.5), (2, 1)], [(0, 3)]], 72, 88)
    r = s.r
    mel, beg = s.buf(), s.buf()
    for (tel, d, p) in s.mel:
        leg(mel, speeldoos(p, d * s.tel, r) * 0.5, s.t(tel))
        if n == 2 and r.random() < 0.5:
            leg(mel, speeldoos(p + 12, d * s.tel, r) * 0.12, s.t(tel) + 0.01)
    for b, gr in enumerate(s.akk):
        tonen = akkoord(toon, gr, 4)
        leg(beg, bas(akkoord(toon, gr, 2)[0], s.tel * 1.6, r) * 0.4, s.t(b * 3))
        if b < len(s.akk) - 1:
            for k in (1, 2):
                for m in tonen:
                    leg(beg, pluk(m, s.tel * 0.8, r, helder=0.2) * 0.09, s.t(b * 3 + k))
        leg(beg, pad([m - 12 for m in tonen], s.tel * 3.2, r, 700) * 0.18, b * 3 * s.tel)
    return niveau(mix(mel + beg, 2.4, seed), -21)


def knuffelkamp(n, seed, toon, bpm):
    """Morning-camp folk: plucked guitar in eighths, a little flute, a shaker and a soft drum."""
    A = ["I", "IV", "I", "V", "I", "IV", "V", "I"]
    B = ["vi", "IV", "I", "V", "vi", "IV", "ii", "V"] if n == 1 else ["IV", "I", "V", "vi", "IV", "I", "ii", "V"]
    s = Stuk(toon, 4, bpm, seed).compose([("A", A), ("B", B), ("A", A), ("B", B), ("A", A)],
                                          [[(0, 2), (2, 1), (3, 1)], [(0, 1), (1, 1), (2, 2)], [(0, 1.5), (1.5, 0.5), (2, 1), (3, 1)], [(0, 3), (3, 1)]],
                                          74, 88)
    r = s.r
    mel, beg, slag = s.buf(), s.buf(), s.buf()
    for (tel, d, p) in s.mel:
        leg(mel, fluit(p, d * s.tel * 0.95, r) * 0.42, s.t(tel))
    patroon = [0, 2, 1, 2, 0, 2, 1, 2] if n == 1 else [0, 1, 2, 1, 0, 2, 1, 2]
    for b, gr in enumerate(s.akk):
        tonen = akkoord(toon, gr, 3)
        laatste = b == len(s.akk) - 1
        leg(beg, bas(akkoord(toon, gr, 2)[0], s.tel * 1.8, r) * 0.34, s.t(b * 4))
        if not laatste:
            leg(beg, bas(akkoord(toon, gr, 2)[2] - 12, s.tel * 1.6, r) * 0.24, s.t(b * 4 + 2))
            for k, v in enumerate(patroon):
                leg(beg, pluk(tonen[v] + (12 if k == 4 else 0), s.tel * 0.9, r, helder=0.45) * 0.17, s.t(b * 4 + k * 0.5))
            for k in range(8):
                leg(slag, shaker(r, 1.0 if k % 2 else 0.55), s.t(b * 4 + k * 0.5))
            if b % 2 == 0:
                leg(slag, trommel(r, 0.4), s.t(b * 4))
        else:
            for i, m in enumerate(tonen + [tonen[0] + 12]):
                leg(beg, pluk(m, s.tel * 3, r, helder=0.45) * 0.17, s.t(b * 4) + i * 0.06)
    return niveau(mix(mel + beg + slag * 0.7, 1.8, seed, nat=0.25), -21)


def haven(n, seed, toon, bpm):
    """A gentle harbour shanty in 6/8 (counted in dotted quarters): flute + a celesta echo, plucked bass, a concertina pad."""
    A = ["I", "IV", "I", "V", "I", "IV", "V", "I"]
    B = ["IV", "I", "ii", "V", "IV", "I", "V", "I"] if n == 1 else ["vi", "IV", "I", "V", "vi", "ii", "V", "V"]
    # bars of 2 dotted-quarter beats, written in eighths: maat = 6 eighths
    s = Stuk(toon, 6, bpm * 3, seed).compose([("A", A), ("B", B), ("A", A), ("B", B), ("A", A)],
                                              [[(0, 3), (3, 2), (5, 1)], [(0, 2), (2, 1), (3, 3)], [(0, 1), (1, 1), (2, 1), (3, 3)],
                                               [(0, 3), (3, 3)]], 69, 84)
    r = s.r
    mel, beg, slag = s.buf(), s.buf(), s.buf()
    for (tel, d, p) in s.mel:
        leg(mel, fluit(p, d * s.tel * 0.95, r, vibrato=0.008) * 0.4, s.t(tel))
        if r.random() < (0.35 if n == 1 else 0.6):
            leg(mel, speeldoos(p + 12, d * s.tel, r) * 0.1, s.t(tel) + 0.02)
    for b, gr in enumerate(s.akk):
        tonen = akkoord(toon, gr, 4)
        laatste = b == len(s.akk) - 1
        leg(beg, accordeon([m - 12 for m in tonen], s.tel * 6.05 * (1.6 if laatste else 1), r) * 0.42, b * 6 * s.tel)
        leg(beg, pluk(akkoord(toon, gr, 2)[0], s.tel * 2.5, r, helder=0.3) * 0.32, s.t(b * 6))
        if not laatste:
            leg(beg, pluk(akkoord(toon, gr, 2)[2], s.tel * 2.5, r, helder=0.3) * 0.25, s.t(b * 6 + 3))
            leg(slag, trommel(r, 0.55), s.t(b * 6))
            leg(slag, tik(r, 0.6), s.t(b * 6 + 3))
            leg(slag, tik(r, 0.3), s.t(b * 6 + 5))
    return niveau(mix(mel + beg + slag * 0.6, 2.2, seed, nat=0.3, helder=3600), -21)


def kandijkom(n, seed, toon, bpm):
    """Winter music box: the tune on a music box, a glockenspiel counter-arpeggio, sleigh bells, a soft pad."""
    maat = 3 if n == 1 else 4
    A = ["I", "vi", "ii", "V", "I", "vi", "IV", "V"]
    B = ["IV", "I", "IV", "V", "iii", "vi", "ii", "V"]
    C = ["vi", "iii", "IV", "I", "ii", "V", "I", "I"]
    ritmes = ([[(0, 2), (2, 1)], [(0, 1), (1, 1), (2, 1)], [(0, 1), (1, 0.5), (1.5, 0.5), (2, 1)]] if maat == 3 else
              [[(0, 1), (1, 1), (2, 2)], [(0, 2), (2, 1), (3, 1)], [(0, 1), (1, 0.5), (1.5, 0.5), (2, 1), (3, 1)], [(0, 3), (3, 1)]])
    s = Stuk(toon, maat, bpm, seed).compose([("A", A), ("B", B), ("A", A), ("C", C), ("A", A)], ritmes, 76, 91)
    r = s.r
    mel, beg, bel = s.buf(), s.buf(), s.buf()
    for (tel, d, p) in s.mel:
        leg(mel, speeldoos(p, d * s.tel, r) * 0.5, s.t(tel))
    for b, gr in enumerate(s.akk):
        tonen = akkoord(toon, gr, 5)
        laatste = b == len(s.akk) - 1
        leg(beg, pad(akkoord(toon, gr, 3), s.tel * (maat + 0.3) * (2 if laatste else 1), r, 800) * 0.22, b * maat * s.tel)
        leg(beg, bas(akkoord(toon, gr, 2)[0] + 12, s.tel * 1.5, r) * 0.32, s.t(b * maat))
        if not laatste:
            for k in range(maat):
                leg(beg, glock(tonen[(k + b) % 3], s.tel * 0.5, r) * 0.06, s.t(b * maat + k + 0.5))
                leg(bel, sleebel(r, 1.0 if k == 0 else 0.55), s.t(b * maat + k))
                if n == 2:
                    leg(bel, sleebel(r, 0.3), s.t(b * maat + k + 0.5))
    return niveau(mix(mel + beg + bel, 2.8, seed, nat=0.36, helder=5000), -21.5)


def zoem(m, dur, r):
    """(K-INT) A little honey bee: a soft buzzy hum (a sine with a few low-passed harmonics and a fast tiny wobble)."""
    n = int((dur + 0.2) * SR)
    t = np.arange(n) / SR
    f = hz(m) * (1 + 0.012 * np.sin(2 * np.pi * 23 * t))
    fase = 2 * np.pi * np.cumsum(f) / SR
    x = np.sin(fase) + 0.3 * np.sin(2 * fase) + 0.15 * np.sin(3 * fase)
    att = np.clip(t / 0.08, 0, 1) * np.where(t > dur, np.exp(-(t - dur) / 0.08), 1)
    return low(x * att, 1400)


def honingtak(n, seed, toon, bpm):
    """(K-INT) Honingtak, the beekeepers' town on the branch: a sweet honey waltz. Music box + plucked melody, a soft bee hum
    that buzzes along on the first beat now and then, pizzicato chords on 2 and 3, a glockenspiel sprinkle like dripping honey."""
    A = ["I", "iii", "IV", "V", "I", "vi", "ii", "V"]
    B = ["IV", "I", "ii", "V", "vi", "IV", "ii", "V"] if n == 1 else ["vi", "iii", "IV", "I", "ii", "V", "I", "V"]
    C = ["I", "IV", "vi", "V", "IV", "ii", "V", "I"]
    s = Stuk(toon, 3, bpm, seed).compose([("A", A), ("B", B), ("A", A), ("C", C), ("A", A)],
                                          [[(0, 2), (2, 1)], [(0, 1), (1, 1), (2, 1)], [(0, 1.5), (1.5, 0.5), (2, 1)], [(0, 1), (1, 2)]], 72, 89)
    r = s.r
    mel, beg, bij = s.buf(), s.buf(), s.buf()
    for (tel, d, p) in s.mel:
        leg(mel, speeldoos(p, d * s.tel, r) * 0.42, s.t(tel))
        leg(mel, pluk(p, d * s.tel, r, helder=0.35) * 0.16, s.t(tel) + 0.005)
    for b, gr in enumerate(s.akk):
        tonen = akkoord(toon, gr, 4)
        laatste = b == len(s.akk) - 1
        leg(beg, bas(akkoord(toon, gr, 2)[0], s.tel * 1.5, r) * 0.36, s.t(b * 3))
        leg(beg, pad([m - 12 for m in tonen], s.tel * 3.2 * (1.8 if laatste else 1), r, 650) * 0.16, b * 3 * s.tel)
        if not laatste:
            for k in (1, 2):
                for m in tonen:
                    leg(beg, pluk(m, s.tel * 0.6, r, helder=0.15) * 0.08, s.t(b * 3 + k))
            if r.random() < 0.3:   # honey drips: a short glockenspiel run down
                for i, m in enumerate(sorted(tonen, reverse=True)):
                    leg(beg, glock(m + 12, s.tel * 0.3, r) * 0.05, s.t(b * 3 + 2) + i * 0.09)
            if b % 4 == (1 if n == 1 else 3):   # a little bee buzzes along
                leg(bij, zoem(tonen[0] + 12, s.tel * 1.6, r) * 0.05, s.t(b * 3))
    return niveau(mix(mel + beg + bij, 2.4, seed, nat=0.3, helder=4600), -21.5)


# ---------------------------------------------------------------- ambience beds (seamless loops: filtered in the frequency domain)

def lus_ruis(n, r, lo, hi):
    spec = np.fft.rfft(r.standard_normal(n))
    f = np.fft.rfftfreq(n, 1 / SR)
    vorm = np.where((f >= lo) & (f <= hi), 1.0, 0.0)
    vorm = np.convolve(vorm, np.hanning(301) / np.sum(np.hanning(301)), mode="same")
    x = np.fft.irfft(spec * vorm / np.sqrt(np.maximum(f, 30) / 100), n)
    return x / (np.sqrt(np.mean(x ** 2)) + 1e-12)


def golf(t, perioden, r):
    """A slow, positive swell that repeats exactly over the loop (integer periods)."""
    x = np.zeros_like(t)
    L = t[-1] + 1 / SR
    for k in perioden:
        x += np.sin(2 * np.pi * k * t / L + r.uniform(0, 6.28))
    return (x - x.min()) / (x.max() - x.min())


def leg_rond(buf, x, at):
    """Add x at time 'at', wrapping around the end (so the loop stays seamless)."""
    a = int(at * SR) % len(buf)
    for i in range(0, len(x), len(buf)):
        stuk = x[i:i + len(buf)]
        e = a + len(stuk)
        if e <= len(buf):
            buf[a:e] += stuk
        else:
            k = len(buf) - a
            buf[a:] += stuk[:k]
            buf[:len(stuk) - k] += stuk[k:]


def galm_rond(x, sec, seed, nat, helder=4200):
    """Reverb by circular convolution: the tail of the loop's end rings on at its start."""
    ir = galm(sec, seed, helder)
    n = len(x)
    ir = np.concatenate([ir, np.zeros(max(0, n - len(ir)))])[:n]
    return x + nat * np.fft.irfft(np.fft.rfft(x) * np.fft.rfft(ir), n)


def tjilp(r, f0, dur, sweep):
    """One little bird chirp: a quick soft sine glide with a touch of trill."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = f0 * (1 + sweep * (t / dur)) * (1 + 0.02 * np.sin(2 * np.pi * r.uniform(25, 45) * t))
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.15 * np.sin(4 * np.pi * np.cumsum(f) / SR)
    return x * np.sin(np.pi * t / dur) ** 2


def vogeltjes(n, r, aantal, sterk=1.0):
    buf = np.zeros(n)
    for _ in range(aantal):
        at, f0, kort = r.uniform(0, n / SR), r.uniform(2600, 4600), r.uniform(0.06, 0.13)
        sweep, vol = r.choice([-0.25, 0.2, 0.35, -0.1]), r.uniform(0.25, 1.0) * sterk
        for k in range(int(r.integers(2, 6))):          # a little phrase of 2-5 chirps
            leg_rond(buf, tjilp(r, f0 * (1 + r.uniform(-0.06, 0.08)), kort, sweep) * vol, at + k * kort * r.uniform(1.3, 1.9))
    return low(buf, 7000)


def belletjes_verte(n, r, aantal, toon=72):
    """Soft distant chimes: single pentatonic glockenspiel notes, low-passed, far away."""
    buf = np.zeros(n)
    penta = [0, 2, 4, 7, 9, 12, 14, 16]
    for _ in range(aantal):
        leg_rond(buf, glock(toon + int(r.choice(penta)), 0.5, r) * r.uniform(0.4, 1.0), r.uniform(0, n / SR))
    return low(buf, 3200)


def bed_golfjes(seed):
    """The harbour: soft water lapping against the jetty (separate little laps + droplets, no wind), a distant chime."""
    r = np.random.default_rng(seed)
    n = 20 * SR
    t = np.arange(n) / SR
    water = low(lus_ruis(n, r, 80, 500), 600) * (0.35 + 0.65 * golf(t, [3, 5], r)) * 0.25
    laps = np.zeros(n)
    for _ in range(34):
        k = int(r.uniform(0.35, 0.7) * SR)
        tt = np.arange(k) / SR
        lap = band(r.standard_normal(k), 150, r.uniform(700, 1100)) * (1 - np.exp(-tt / 0.07)) * np.exp(-tt / r.uniform(0.12, 0.22))
        leg_rond(laps, lap * r.uniform(0.5, 1.0), r.uniform(0, 20))
    plip = np.zeros(n)
    for _ in range(14):
        k = int(0.05 * SR)
        tt = np.arange(k) / SR
        f = r.uniform(500, 900) * (1 + 2.5 * tt / 0.05)
        leg_rond(plip, np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * tt / 0.05) * r.uniform(0.15, 0.35), r.uniform(0, 20))
    bel = belletjes_verte(n, r, 3, toon=74) * 0.25
    x = galm_rond(water + laps + plip + bel, 1.4, seed, 0.25, helder=3000)
    return niveau(x, -34)


def bed_briesje(seed):
    """Zwamzoet + Knuffelkamp: a sweet garden bed (gentle birdsong, soft distant chimes, a faint warm hum), no wind."""
    r = np.random.default_rng(seed)
    n = 20 * SR
    t = np.arange(n) / SR
    vogels = vogeltjes(n, r, 11)
    bel = belletjes_verte(n, r, 6) * 0.45
    warm = low(lus_ruis(n, r, 90, 260), 300) * (0.6 + 0.4 * golf(t, [2], r)) * 0.08
    x = galm_rond(vogels + bel + warm, 1.8, seed, 0.3, helder=5000)
    return niveau(x, -35)


def bed_haardvuur(seed):
    """Kandijkom: a warm crackling hearth with soft sleigh bells now and then (no cold wind)."""
    r = np.random.default_rng(seed)
    n = 20 * SR
    t = np.arange(n) / SR
    gloed = low(lus_ruis(n, r, 50, 300), 350) * (0.75 + 0.25 * golf(t, [3, 7], r)) * 0.4
    knap = np.zeros(n)
    for _ in range(150):
        k = int(r.uniform(150, 700))
        tt = np.arange(k) / SR
        leg_rond(knap, r.standard_normal(k) * np.exp(-tt * r.uniform(200, 600)) * r.uniform(0.3, 1.0) ** 2, r.uniform(0, 20))
    for _ in range(10):                                        # a few deeper, cosy log pops
        k = int(0.04 * SR)
        tt = np.arange(k) / SR
        leg_rond(knap, low(r.standard_normal(k), 1800) * np.exp(-tt * 90) * 1.5, r.uniform(0, 20))
    knap = low(band(knap, 500, 5000), 4000) * 1.6
    bellen = np.zeros(n)
    for _ in range(4):                                         # a soft jingle: 3-6 shakes
        at = r.uniform(0, 20)
        for k in range(int(r.integers(3, 7))):
            leg_rond(bellen, sleebel(r, r.uniform(0.5, 1.0)), at + k * 0.16)
    x = galm_rond(gloed + knap + bellen * 1.8, 1.2, seed, 0.2, helder=4500)
    return niveau(x, -33)


TUNES = {
    "zwamzoet_1": lambda: zwamzoet(1, 101, 65, 104),     # F major
    "zwamzoet_2": lambda: zwamzoet(2, 102, 70, 96),      # Bb major
    "knuffelkamp_1": lambda: knuffelkamp(1, 201, 67, 100),  # G major
    "knuffelkamp_2": lambda: knuffelkamp(2, 202, 62, 92),   # D major
    "haven_1": lambda: haven(1, 301, 62, 46),            # D major, 6/8 at 46 dotted quarters... (bpm * 3 eighths)
    "haven_2": lambda: haven(2, 302, 67, 42),            # G major
    "kandijkom_1": lambda: kandijkom(1, 401, 63, 96),   # Eb major waltz
    "kandijkom_2": lambda: kandijkom(2, 402, 68, 84),    # Ab major, 4/4
    "honingtak_1": lambda: honingtak(1, 601, 67, 100),   # (K-INT) G major honey waltz
    "honingtak_2": lambda: honingtak(2, 602, 64, 92),    # (K-INT) E major, slower
}
BEDDEN = {"bed_golfjes": lambda: bed_golfjes(501), "bed_briesje": lambda: bed_briesje(502), "bed_haardvuur": lambda: bed_haardvuur(503)}


def main():
    import sys
    alleen = sys.argv[1:]   # (K-INT) optional: only these names, e.g. honingtak_1 honingtak_2
    for naam, maak in {**TUNES, **BEDDEN}.items():
        if alleen and naam not in alleen:
            continue
        save(naam, maak(), 0.6 if naam.startswith("bed_") else 0.75)


if __name__ == "__main__":
    main()
