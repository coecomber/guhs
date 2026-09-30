"""Checks the four disco songs the game plays (the game flashes the tiles on the beat, so the beat must be where the
game thinks it is): container duration == decoded duration (correct granule), mono 48 kHz, loudness close to the remix,
tempo from the onsets == the song's BPM, and the beat phase at t = 0 (every beat k at k * 60 / BPM seconds).

    python check_songs.py        (exit code 1 on a problem)
"""
import os
import sys

import av
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
DIR = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "guhs", "sounds", "disco"))
# file: (bpm, beats the game loops after) - keep in sync with DiscoLiedje.java
SONGS = {"ze_hangen_disco70": (110.0, 200), "vadsige_tango": (100.0, 144), "mika_mambo": (140.0, 208), "njeg_njeg_boogie": (128.0, 208)}
# 3.0 (guhwaii-spellen): the hula songs of Guhwai'i (make_hula.py; keep in sync with HulaLiedje.java)
SONGS.update({"../guhwaiispellen/aloha_njeg": (88.0, 128), "../guhwaiispellen/guhla_hula_rock": (132.0, 192), "../guhwaiispellen/vahoeg_hula_hop": (160.0, 240)})


def decode(path):
    c = av.open(path)
    s = c.streams.audio[0]
    x = np.concatenate([f.to_ndarray().reshape(-1) if f.to_ndarray().shape[0] == 1 else f.to_ndarray()[0] for f in c.decode(s)])
    info = (s.codec_context.name, s.channels, s.rate, c.duration / 1e6 if c.duration else 0.0)
    c.close()
    return x.astype(np.float64), info


def onset_env(x, sr, hop=0.005):
    h = int(hop * sr)
    frames = len(x) // h
    e = np.sqrt(np.add.reduceat(x[:frames * h] ** 2, np.arange(0, frames * h, h)) / h)
    le = np.log(e + 1e-4)
    d = np.maximum(0, np.diff(le, prepend=le[0]))
    return d, hop


def tempo_and_phase(x, sr, bpm):
    d, hop = onset_env(x, sr)
    # tempo: the best period between 60 and 180 BPM by comb-filter energy
    best, best_bpm = -1, 0
    for b in np.arange(60, 181, 0.5):
        p = 60 / b / hop
        idx = (np.arange(0, len(d) / p - 1) * p).astype(int)
        score = max(d[(idx + o) % len(d)].mean() for o in range(0, int(p), 2))
        if score > best:
            best, best_bpm = score, b
    # phase: where on the song's own grid the onsets sit (0 = on the beat)
    p = 60 / bpm / hop
    scores = []
    for o in range(int(p)):
        idx = (np.arange(0, len(d) / p - 1) * p + o).astype(int)
        scores.append(d[idx[idx < len(d)]].mean())
    phase = int(np.argmax(scores)) * hop
    return best_bpm, phase, 60 / bpm


def main():
    ok = True
    ref = None
    for name, (bpm, loop) in SONGS.items():
        path = os.path.join(DIR, name + ".ogg")
        if not os.path.exists(path):
            print("MISSING", path)
            ok = False
            continue
        x, (codec, ch, rate, dur) = decode(path)
        real = len(x) / rate
        rms = float(np.sqrt(np.mean(x ** 2)))
        ref = ref or rms
        est, phase, beat = tempo_and_phase(x, rate, bpm)
        ratio = est / bpm
        tempo_ok = min(abs(ratio - r) for r in (0.5, 1, 2)) < 0.02
        phase_ok = min(phase, beat - phase) < 0.035
        loop_ok = loop * beat <= real + 0.01
        line_ok = codec == "vorbis" and ch == 1 and rate == 48000 and abs(dur - real) < 0.05 and tempo_ok and phase_ok and loop_ok \
            and 0.7 < rms / ref < 1.35
        ok &= line_ok
        print(f"{'ok ' if line_ok else 'BAD'} {name:20s} {codec} ch={ch} {rate} Hz  header {dur:6.2f}s decoded {real:6.2f}s  rms {rms:.3f}"
              f"  tempo {est:5.1f} (song {bpm})  beat phase {phase * 1000:4.0f} ms  loop {loop} beats = {loop * beat:6.2f}s")
    return ok


if __name__ == "__main__":
    sys.exit(0 if main() else 1)
