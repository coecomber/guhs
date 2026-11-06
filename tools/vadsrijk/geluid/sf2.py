"""
T-GELUID: a small, pure-numpy SoundFont 2 player (no fluidsynth needed): reads an .sf2 and renders MIDI notes to a mono buffer.

What it does (enough for soft, cosy music; NOT a full SF2 implementation):
  * presets -> preset zones -> instruments -> instrument zones -> samples (global zones, key/velocity ranges, additive preset gens);
  * sample offsets, loops (modes 1 and 3 loop; 0/2 play once), root key / scale tuning / coarse + fine tune / pitch correction;
  * the volume envelope (delay, attack, hold, decay, sustain, release; keynum->hold/decay), initial attenuation (EMU 0.4 factor),
    the default velocity curve (40 log10 (v/127) dB) and velocity->filter (-2400 cents at velocity 0);
  * the low-pass filter (initialFilterFc + Q) with the mod envelope's filter sweep (crossfade between the open and closed cutoff);
  * the vibrato LFO (vibLfoToPitch, delay, frequency);
  * 4-point cubic interpolation.
Not done: modulators other than the defaults, chorus, the mod LFO, exclusive classes, pitch envelopes (rarely matter for this music).

    sf = SF2(path); stem = sf.stem(bank, program); buf = stem.note(key, vel, seconds, sr)
"""
import struct

import numpy as np
from scipy.signal import lfilter

GEN_DEFAULTS = {8: 13500, 9: 0, 21: -12000, 22: 0, 23: -12000, 24: 0, 25: -12000, 26: -12000, 27: -12000, 28: -12000, 29: 0,
                30: -12000, 33: -12000, 34: -12000, 35: -12000, 36: -12000, 37: 0, 38: -12000, 56: 100, 58: -1, 46: -1, 47: -1}
RANGE_GENS = (43, 44)
SIGNED = True


def tc(v):
    """Timecents -> seconds."""
    return 2.0 ** (v / 1200.0)


class Sample:
    __slots__ = ("name", "start", "end", "sloop", "eloop", "rate", "pitch", "corr", "typ")


class Zone:
    def __init__(self):
        self.gens = {}
        self.klo, self.khi, self.vlo, self.vhi = 0, 127, 0, 127


class SF2:
    def __init__(self, pad):
        with open(pad, "rb") as f:
            data = f.read()
        if data[:4] != b"RIFF" or data[8:12] != b"sfbk":
            raise ValueError("not a SoundFont 2 file: " + pad)
        self.chunks = {}
        self._lijst(data, 12, len(data))
        raw = self.chunks["smpl"]
        self.smpl = np.frombuffer(raw, dtype="<i2").astype(np.float32) / 32768.0
        self._pdta()
        self._cache = {}

    def _lijst(self, data, a, b):
        while a + 8 <= b:
            cid = data[a:a + 4].decode("latin-1")
            n = struct.unpack("<I", data[a + 4:a + 8])[0]
            if cid == "LIST":
                self._lijst(data, a + 12, a + 8 + n)
            else:
                self.chunks[cid] = data[a + 8:a + 8 + n]
            a += 8 + n + (n & 1)

    def _recs(self, cid, fmt):
        raw = self.chunks[cid]
        size = struct.calcsize(fmt)
        return [struct.unpack(fmt, raw[i:i + size]) for i in range(0, len(raw) - size + 1, size)]

    def _pdta(self):
        self.phdr = self._recs("phdr", "<20sHHHIII")
        self.pbag = self._recs("pbag", "<HH")
        self.pgen = self._recs("pgen", "<HH")
        self.inst = self._recs("inst", "<20sH")
        self.ibag = self._recs("ibag", "<HH")
        self.igen = self._recs("igen", "<HH")
        self.samples = []
        for r in self._recs("shdr", "<20sIIIIIBbHH"):
            s = Sample()
            s.name = r[0].split(b"\0")[0].decode("latin-1")
            s.start, s.end, s.sloop, s.eloop, s.rate, s.pitch, s.corr, _, s.typ = r[1:]
            self.samples.append(s)

    def presets(self):
        """[(bank, program, name)] (without the terminal EOP record)."""
        return [(r[2], r[1], r[0].split(b"\0")[0].decode("latin-1")) for r in self.phdr[:-1]]

    @staticmethod
    def _gen_val(oper, amt):
        if oper in RANGE_GENS:
            return (amt & 0xFF, amt >> 8)
        if oper in (41, 53, 54):
            return amt
        return amt if amt < 32768 else amt - 65536

    def _zones(self, bags, gens, b0, b1, link):
        zones = []
        glob = None
        for bi in range(b0, b1):
            g0, g1 = bags[bi][0], bags[bi + 1][0]
            z = Zone()
            for oper, amt in gens[g0:g1]:
                v = self._gen_val(oper, amt)
                if oper == 43:
                    z.klo, z.khi = v
                elif oper == 44:
                    z.vlo, z.vhi = v
                else:
                    z.gens[oper] = v
            if link not in z.gens:
                if bi == b0:
                    glob = z
                continue
            zones.append(z)
        return glob, zones

    def stem(self, bank, program):
        key = (bank, program)
        if key not in self._cache:
            self._cache[key] = Stem(self, bank, program)
        return self._cache[key]


class Region:
    """One playable layer: an instrument zone (merged with its global zone) + the preset zone's additive gens."""

    def __init__(self, sf, igen, pgen, klo, khi, vlo, vhi):
        self.sf = sf
        self.g = dict(GEN_DEFAULTS)
        self.g.update(igen)
        for k, v in pgen.items():
            if k in (41, 53, 54, 58, 46, 47, 0, 1, 2, 3, 4, 12, 45, 50, 57):
                continue
            self.g[k] = self.g.get(k, 0) + v
        self.klo, self.khi, self.vlo, self.vhi = klo, khi, vlo, vhi
        self.s = sf.samples[self.g[53]]


class Stem:
    def __init__(self, sf, bank, program):
        self.sf = sf
        idx = None
        for i, r in enumerate(sf.phdr[:-1]):
            if r[1] == program and r[2] == bank:
                idx = i
                break
        if idx is None:
            for i, r in enumerate(sf.phdr[:-1]):
                if r[1] == program and r[2] == 0:
                    idx = i
                    break
        if idx is None:
            raise KeyError(f"preset {bank}:{program} not in soundfont")
        r = sf.phdr[idx]
        self.naam = r[0].split(b"\0")[0].decode("latin-1")
        pglob, pzones = sf._zones(sf.pbag, sf.pgen, r[3], sf.phdr[idx + 1][3], 41)
        self.regions = []
        for pz in pzones:
            pg = dict(pglob.gens) if pglob else {}
            pg.update(pz.gens)
            pklo, pkhi, pvlo, pvhi = pz.klo, pz.khi, pz.vlo, pz.vhi
            if pglob:
                pklo, pkhi = max(pklo, pglob.klo), min(pkhi, pglob.khi)
            ins = sf.inst[pz.gens[41]]
            iglob, izones = sf._zones(sf.ibag, sf.igen, ins[1], sf.inst[pz.gens[41] + 1][1], 53)
            for iz in izones:
                ig = dict(iglob.gens) if iglob else {}
                ig.update(iz.gens)
                klo, khi = max(pklo, iz.klo), min(pkhi, iz.khi)
                vlo, vhi = max(pvlo, iz.vlo), min(pvhi, iz.vhi)
                if klo > khi or vlo > vhi:
                    continue
                self.regions.append(Region(sf, ig, pg, klo, khi, vlo, vhi))

    def note(self, key, vel, dur, sr, release_max=4.0, vib_extra=0.0):
        """Render one note (key 0-127, velocity 1-127, held dur seconds) -> float32 mono buffer incl. the release tail."""
        out = None
        for rg in self.regions:
            if rg.klo <= key <= rg.khi and rg.vlo <= vel <= rg.vhi:
                b = _render(rg, key, vel, dur, sr, release_max, vib_extra)
                if b is None:
                    continue
                if out is None:
                    out = b
                else:
                    if len(b) > len(out):
                        b, out = out, b
                    out[:len(b)] += b
        return out


def _render(rg, key, vel, dur, sr, release_max, vib_extra):
    g, s, sf = rg.g, rg.s, rg.sf
    if s.typ & 0x8000:   # ROM sample
        return None
    start = s.start + g.get(0, 0) + 32768 * g.get(4, 0)
    end = s.end + g.get(1, 0) + 32768 * g.get(12, 0)
    sloop = s.sloop + g.get(2, 0) + 32768 * g.get(45, 0)
    eloop = s.eloop + g.get(3, 0) + 32768 * g.get(50, 0)
    mode = g.get(54, 0) & 3
    looping = mode in (1, 3) and eloop - sloop > 8
    root = g[58] if g[58] >= 0 else s.pitch
    k = g[46] if g[46] >= 0 else key
    cents = (k - root) * g[56] + g.get(51, 0) * 100 + g.get(52, 0) + s.corr
    step = 2.0 ** (cents / 1200.0) * s.rate / sr
    # ---- volume envelope (cB attenuation timeline)
    delay = tc(g[33])
    attack = tc(g[34])
    hold = tc(g[35] + g.get(39, 0) * (60 - key))
    decay = tc(g[36] + g.get(40, 0) * (60 - key))
    sus = min(1440, max(0, g[37]))
    release = min(release_max, tc(g[38]))
    if delay < 0.002:
        delay = 0.0
    total = dur + release
    n = int(total * sr) + 1
    t = np.arange(n, dtype=np.float64) / sr
    att = np.empty(n)   # amplitude (linear)
    # attack (linear amplitude), hold, decay (linear in dB down to sustain)
    a_end = delay + attack
    h_end = a_end + hold
    d_len = decay * (sus / 1000.0)
    amp = np.zeros(n)
    m = (t >= delay) & (t < a_end)
    amp[m] = (t[m] - delay) / max(attack, 1e-6)
    m = (t >= a_end) & (t < h_end)
    amp[m] = 1.0
    m = t >= h_end
    cb = np.minimum((t[m] - h_end) / max(decay, 1e-6) * 1000.0, sus)
    amp[m] = 10.0 ** (-cb / 200.0)
    # release from the level at note-off
    ri = int(dur * sr)
    if ri < n:
        lvl = amp[ri] if ri < n else 0.0
        lvl_cb = -200.0 * np.log10(max(lvl, 1e-6))
        tr = t[ri:] - dur
        rcb = lvl_cb + tr / max(release, 1e-6) * 1000.0
        amp[ri:] = 10.0 ** (-rcb / 200.0)
    # ---- sample positions (with vibrato)
    vib = g.get(6, 0) + vib_extra
    if vib:
        f = 8.176 * 2.0 ** (g[24] / 1200.0)
        vd = tc(g[23])
        lfo = np.sin(2 * np.pi * f * np.maximum(t - vd, 0)) * (t > vd) * np.minimum(np.maximum(t - vd, 0) / 0.4, 1.0)
        steps = step * 2.0 ** (vib * lfo / 1200.0)
        pos = np.concatenate(([0.0], np.cumsum(steps[:-1]))) + start
    else:
        pos = start + step * np.arange(n, dtype=np.float64)
    if looping:
        L = eloop - sloop
        over = pos >= eloop
        pos[over] = sloop + np.mod(pos[over] - sloop, L)
    else:
        stop = np.searchsorted(pos, end - 2)
        if stop < n:
            n = max(stop, 2)
            pos, amp, t = pos[:n], amp[:n], t[:n]
    data = sf.smpl
    i = np.floor(pos).astype(np.int64)
    fr = (pos - i).astype(np.float32)
    i0 = np.clip(i - 1, 0, len(data) - 1)
    i1 = np.clip(i, 0, len(data) - 1)
    i2 = np.clip(i + 1, 0, len(data) - 1)
    i3 = np.clip(i + 2, 0, len(data) - 1)
    if looping:
        # wrap the interpolation neighbours at the loop end
        i2 = np.where(i + 1 >= eloop, i + 1 - (eloop - sloop), i2)
        i3 = np.where(i + 2 >= eloop, i + 2 - (eloop - sloop), i3)
    y0, y1, y2, y3 = data[i0], data[i1], data[i2], data[i3]
    c1 = 0.5 * (y2 - y0)
    c2 = y0 - 2.5 * y1 + 2.0 * y2 - 0.5 * y3
    c3 = 0.5 * (y3 - y0) + 1.5 * (y1 - y2)
    y = ((c3 * fr + c2) * fr + c1) * fr + y1
    # ---- filter (+ velocity -> cutoff, + mod envelope sweep)
    fc = g[8] - 2400.0 * (1.0 - vel / 127.0)
    menv_amt = g.get(11, 0)
    q_db = max(0, g[9]) / 10.0
    if fc < 13400 or menv_amt:
        y_closed = _lp(y, fc, q_db, sr)
        if menv_amt:
            y_open = _lp(y, fc + menv_amt, q_db, sr)
            menv = _modenv(g, t, dur, key)
            y = y_closed * (1 - menv) + y_open * menv
        else:
            y = y_closed
    # ---- gain: attenuation (EMU 0.4 factor) + the velocity curve
    atten = 0.4 * max(0, g.get(48, 0))
    gain = 10.0 ** (-atten / 200.0) * (vel / 127.0) ** 2
    out = (y * amp[:len(y)] * gain).astype(np.float32)
    # cut the silent tail
    thr = 10 ** (-90 / 20)
    nz = np.nonzero(np.abs(amp[:len(y)]) > thr)[0]
    if len(nz):
        out = out[:nz[-1] + 1]
    return out


def _lp(y, cents, q_db, sr):
    f = 8.176 * 2.0 ** (cents / 1200.0)
    f = min(max(f, 20.0), sr * 0.45)
    if f >= 19500:
        return y
    q = max(0.5, 10 ** (q_db / 20.0) * 0.7071)
    w0 = 2 * np.pi * f / sr
    alpha = np.sin(w0) / (2 * q)
    cw = np.cos(w0)
    b = np.array([(1 - cw) / 2, 1 - cw, (1 - cw) / 2])
    a = np.array([1 + alpha, -2 * cw, 1 - alpha])
    return lfilter(b / a[0], a / a[0], y).astype(np.float32)


def _modenv(g, t, dur, key):
    d = tc(g[25])
    a = tc(g[26])
    h = tc(g[27] + g.get(31, 0) * (60 - key))
    dc = tc(g[28] + g.get(32, 0) * (60 - key))
    sus = 1.0 - min(1000, max(0, g[29])) / 1000.0
    r = tc(g[30])
    e = np.zeros(len(t))
    m = (t >= d) & (t < d + a)
    e[m] = (t[m] - d) / max(a, 1e-6)
    m = (t >= d + a) & (t < d + a + h)
    e[m] = 1.0
    m = t >= d + a + h
    e[m] = np.maximum(sus, 1.0 - (t[m] - d - a - h) / max(dc, 1e-6) * (1 - sus))
    ri = np.searchsorted(t, dur)
    if ri < len(t):
        lvl = e[ri]
        e[ri:] = np.maximum(0.0, lvl - (t[ri:] - dur) / max(r, 1e-6))
    return e.astype(np.float32)
