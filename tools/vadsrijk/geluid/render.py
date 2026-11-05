"""
T-GELUID: MIDI -> mono audio with the SoundFont (sf2.py), plus a soft hall reverb and a gentle master.

Any standard MIDI file works (program changes + CC0 bank select per channel; channel 10 = drums). The composer's own MIDIs
carry a mix sheet in their track names: "<track>|<instrument>|<gain>|<reverb send>"; foreign MIDIs get gain 1, reverb 0.3.

    x = render_midi("stuk.mid", sf, sr=32000, lus=False, galm_s=2.4)
    x = master(x, sr, rms_db=-24)
"""
import numpy as np
from scipy.signal import butter, fftconvolve, lfilter


def galm_ir(sr, sec=2.4, helder=5200, seed=7):
    """A soft hall: a short pre-delay, a few early reflections, then a dark exponentially decaying noise tail."""
    r = np.random.default_rng(seed)
    n = int(sec * sr)
    t = np.arange(n) / sr
    ir = r.standard_normal(n) * np.exp(-6.9 * t / sec)   # -60 dB at 'sec'
    ir[: int(0.02 * sr)] = 0
    for d, g in ((0.011, 0.5), (0.017, 0.35), (0.023, 0.3), (0.031, 0.25)):
        ir[int(d * sr)] += g
    b, a = butter(2, helder / (sr / 2), btype="low")
    ir = lfilter(b, a, ir)
    return ir / np.sqrt(np.sum(ir ** 2) + 1e-12) * 0.6


def lees_midi(pad):
    """-> (notes per channel [(t0, t1, key, vel)], mix per channel {ch: (bank, prog, gain, galm, naam)})."""
    import mido
    mf = mido.MidiFile(pad)
    mix = {}
    for tr in mf.tracks:
        naam, ch, bank, prog = None, None, 0, 0
        for m in tr:
            if m.type == "track_name":
                naam = m.name
            elif m.type == "control_change" and m.control == 0:
                bank, ch = m.value, m.channel
            elif m.type == "program_change":
                prog, ch = m.program, m.channel
            elif m.type == "note_on" and ch is None:
                ch = m.channel
        if ch is None:
            continue
        gain, galm, label = 1.0, 0.3, naam or f"ch{ch}"
        if naam and naam.count("|") == 3:
            label, _, g, w = naam.split("|")
            gain, galm = float(g), float(w)
        mix[ch] = (128 if ch == 9 else bank, prog, gain, galm, label)
    noten = {}
    open_ = {}
    t = 0.0
    for m in mf:
        t += m.time
        if m.type == "note_on" and m.velocity > 0:
            open_.setdefault((m.channel, m.note), []).append((t, m.velocity))
        elif m.type in ("note_off", "note_on"):
            lijst = open_.get((m.channel, m.note))
            if lijst:
                t0, v = lijst.pop(0)
                noten.setdefault(m.channel, []).append((t0, t, m.note, v))
    for ch in noten:
        if ch not in mix:
            mix[ch] = (128 if ch == 9 else 0, 0, 1.0, 0.3, f"ch{ch}")
    return noten, mix


def render_midi(pad, sf, sr=32000, lus=False, galm_s=2.4, helder=5200, stems=False):
    noten, mix = lees_midi(pad)
    eind = max((n[1] for v in noten.values() for n in v), default=0.0)
    lengte = eind + galm_s + 4.0
    N = int(lengte * sr) + 1
    droog = np.zeros(N, np.float64)
    nat = np.zeros(N, np.float64)
    cache = {}
    per = {}
    for ch, lijst in noten.items():
        bank, prog, gain, galm, label = mix[ch]
        stem = sf.stem(bank, prog)
        buf = np.zeros(N, np.float64)
        for t0, t1, key, vel in lijst:
            dur = max(0.03, round((t1 - t0) * 20) / 20)
            vq = int(round(vel / 4) * 4) or 4
            ck = (bank, prog, key, vq, dur)
            x = cache.get(ck)
            if x is None:
                x = stem.note(key, min(127, vq), dur, sr)
                if x is None:
                    x = np.zeros(1, np.float32)
                cache[ck] = x
            a = int(t0 * sr)
            b = min(N, a + len(x))
            buf[a:b] += x[: b - a]
        buf *= gain
        per[label] = buf
        droog += buf
        nat += buf * galm
    nat = fftconvolve(nat, galm_ir(sr, galm_s, helder))[:N]
    y = droog + nat
    if lus:
        # the loop: everything after the loop length (release + reverb tail) folds back onto the start
        L = int(round(eind_lus(pad) * sr))
        staart = y[L:]
        y = y[:L].copy()
        k = min(len(staart), L)
        y[:k] += staart[:k]
    else:
        # trim the silent end
        e = np.nonzero(np.abs(y) > 1e-4)[0]
        if len(e):
            y = y[: e[-1] + int(0.05 * sr)]
    return (y, per) if stems else y


def eind_lus(pad):
    """The loop length in seconds: the end of the last bar (the MIDI's end-of-track, rounded up to whole bars)."""
    import mido
    mf = mido.MidiFile(pad)
    tempo, num, den = 500000, 4, 4
    for m in mf.tracks[0]:
        if m.type == "set_tempo":
            tempo = m.tempo
        elif m.type == "time_signature":
            num, den = m.numerator, m.denominator
    laatste = 0
    for tr in mf.tracks[1:]:
        t = 0
        for m in tr:
            t += m.time
            if m.type == "note_on" and m.velocity > 0:
                laatste = max(laatste, t)
    tpq = mf.ticks_per_beat
    balk = tpq * num * 4 / den
    balken = int(laatste // balk) + 1
    return balken * balk / tpq * tempo / 1e6


def master(x, sr, rms_db=-24.0, piek=0.8, inval=0.01, uitval=0.0, hoog=45, lus=False):
    b, a = butter(2, hoog / (sr / 2), btype="high")
    if lus:
        # a loop: filter as if it already looped once (no filter start-up at the seam), no fades
        voor = min(len(x), sr)
        x = lfilter(b, a, np.concatenate([x[-voor:], x]))[voor:]
        inval = uitval = 0.0
    else:
        x = lfilter(b, a, x)
    x = x - np.mean(x)
    # loudness by the 'active' RMS (ignore near-silence, so sparse pieces are not pumped up)
    blok = int(0.4 * sr)
    if len(x) > blok * 4:
        r = np.sqrt(np.mean(x[: len(x) // blok * blok].reshape(-1, blok) ** 2, axis=1))
        actief = r[r > np.max(r) * 0.1]
        rms = np.sqrt(np.mean(actief ** 2)) if len(actief) else np.sqrt(np.mean(x ** 2))
    else:
        rms = np.sqrt(np.mean(x ** 2))
    x = x * (10 ** (rms_db / 20) / (rms + 1e-12))
    x = np.tanh(x / piek) * piek
    n = int(inval * sr)
    if n:
        x[:n] *= np.linspace(0, 1, n)
    m = int(uitval * sr)
    if m:
        x[-m:] *= np.linspace(1, 0, m) ** 1.5
    return x.astype(np.float32)


def bewaar_ogg(pad, x, sr, kwaliteit=0.6):
    import os

    import soundfile as sf
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    x = np.clip(x, -0.98, 0.98).astype(np.float32)
    with sf.SoundFile(pad, "w", sr, 1, format="OGG", subtype="VORBIS", compression_level=kwaliteit) as f:
        for a in range(0, len(x), sr // 2):
            f.write(x[a:a + sr // 2])
    return os.path.getsize(pad)
