"""Vadsige Tango (~100 BPM, D minor): the disco's easy song (makkelijk). -> sounds/disco/vadsige_tango.ogg

A dramatic little tango for guhs who take it slow: bandoneon with arrastres, pizzicato bass and a marcato piano on
every beat (the game's beat: the tiles flash on it), a habanera middle part with a violin, and the guhs singing the
melody with their own guh noises. Beat 0 is at t = 0; 36 bars of 4 beats (the game loops the song after 144 beats).
"""
import os
import sys

import numpy as np

from guhmix import SR, Tracks, adsr, filt, reverb, delay, kick, strings, piano, write_ogg
from guhband import bandoneon, pizz, golpe, chicharra, violin, chord, piano_hit, master_rms
from guhstem import Choir, place

BPM = 100.0
BEAT = 60 / BPM
BAR = 4 * BEAT
E8 = BEAT / 2
S16 = BEAT / 4

INTRO = ["Dm", "Dm", "A7", "A7"]
A = ["Dm", "Gm", "A7", "Dm", "Dm", "Gm", "A7", "Dm"]
B = ["F", "C7", "C7", "F", "Bb", "Gm", "A7", "A7"]
PUENTE = ["Gm", "A7", "Gm", "A7"]
CODA = ["Dm", "Gm", "A7", "Dm"]
SECTIONS = [("intro", INTRO), ("a", A), ("b", B), ("puente", PUENTE), ("a2", A), ("coda", CODA)]
BARS = sum(len(c) for _, c in SECTIONS)

# the melodies: per bar [(beat, midi, beats)]
MEL_A = [
    [(0, 69, 1.5), (1.5, 70, 0.5), (2, 69, 1), (3, 65, 1)],
    [(0, 67, 1.5), (1.5, 69, 0.5), (2, 70, 1), (3, 74, 1)],
    [(0, 73, 1.5), (1.5, 74, 0.5), (2, 76, 1), (3, 73, 1)],
    [(0, 74, 3), (3, 69, 1)],
    [(0, 69, 0.75), (0.75, 70, 0.25), (1, 69, 0.5), (1.5, 67, 0.5), (2, 65, 1), (3, 62, 1)],
    [(0, 70, 1.5), (1.5, 69, 0.5), (2, 67, 1), (3, 70, 1)],
    [(0, 73, 1), (1, 76, 1), (2, 79, 1), (3, 76, 1)],
    [(0, 74, 2)],
]
MEL_B = [
    [(0, 72, 2), (2, 69, 1), (3, 72, 1)],
    [(0, 70, 1.5), (1.5, 69, 0.5), (2, 67, 1), (3, 64, 1)],
    [(0, 67, 1), (1, 70, 1), (2, 72, 1), (3, 76, 1)],
    [(0, 77, 3), (3, 72, 1)],
    [(0, 74, 1.5), (1.5, 72, 0.5), (2, 70, 1), (3, 74, 1)],
    [(0, 70, 1.5), (1.5, 69, 0.5), (2, 67, 1), (3, 70, 1)],
    [(0, 69, 1), (1, 73, 1), (2, 76, 1), (3, 79, 1)],
    [(0, 81, 2), (2, 79, 0.5), (2.5, 76, 0.5), (3, 73, 1)],
]


def build():
    tr = Tracks(BARS * BAR + 3.5)
    guh = Choir("guh_ambient")
    bar0 = 0
    starts = {}
    for sec, chords in SECTIONS:
        starts[sec] = bar0
        for b, name in enumerate(chords):
            t0 = (bar0 + b) * BAR
            root, tones = chord(name)
            last = sec == "coda" and b == len(chords) - 1
            intro_solo = sec == "intro" and b < 2
            habanera = sec in ("b",)
            if last:
                # the tango ending: "chan... CHAN!" (dominant, then the tonic) and silence
                r5, t5 = chord("A7")
                for beat, (rr, tt, acc) in ((2, (r5, t5, 0.8)), (3, (root, tones, 1.2))):
                    tb = t0 + beat * BEAT
                    tr.add("bass", pizz(rr, 0.5, acc), tb, 0.9)
                    tr.add("bass", pizz(rr - 12, 0.5, acc), tb, 0.5)
                    tr.add("keys", piano_hit([m - 12 for m in tt] + tt, 0.5, 0.9 * acc), tb, 0.55)
                    tr.add("bando", bandoneon(tt + [tt[0] + 12], 0.35, 0, 3200, acc), tb, 0.5)
                    tr.add("drums", golpe(), tb, 0.9 * acc)
                    tr.duck(tb, 0.3, BEAT)
                place(tr, guh.note(tones[0] + 12, 0.45), t0 + 3 * BEAT, 1.1)
                continue
            # --- the beat: golpe on every beat (strong on 1 and 3) and a soft kick; marcato bass and piano ---
            if not intro_solo:
                for q in range(4):
                    tb = t0 + q * BEAT
                    tr.add("drums", golpe(), tb, 0.8 if q % 2 == 0 else 0.45)
                    tr.add("drums", kick(0.3, 0.25, 50), tb, 0.35 if q % 2 == 0 else 0.22)
                    tr.duck(tb, 0.25, BEAT)
                if habanera:        # 3-3-2 over the bar in eighths: 0, 3, 6 (and a pickup on 7)
                    for e, iv, acc in ((0, 0, 1.0), (3, 7, 0.8), (6, 12, 0.9), (7, 7, 0.5)):
                        tr.add("bass", pizz(root + iv, E8 * 1.8, acc), t0 + e * E8, 0.85)
                        if e != 7:
                            tr.add("keys", piano_hit(tones, E8 * 1.5, 0.7 * acc), t0 + e * E8, 0.32)
                else:               # marcato in four
                    for q in range(4):
                        acc = 1.0 if q % 2 == 0 else 0.7
                        iv = 0 if q in (0, 2) else (7 if q == 1 else 12)
                        tr.add("bass", pizz(root + iv, BEAT * 0.8, acc), t0 + q * BEAT, 0.85)
                        tr.add("keys", piano_hit([m - 12 for m in tones[:3]] if q % 2 else tones, BEAT * 0.45, 0.65 * acc),
                               t0 + q * BEAT, 0.3)
            # --- bandoneon: the arrastre chord that swells into every other bar, melody in the a-parts ---
            if sec in ("intro", "b", "puente") or (sec == "a" and b % 2 == 1):
                tr.add("bando", bandoneon([m - 12 for m in tones] + tones[:2], 2 * BEAT, 1.0, 2400, 0.9), t0 + 2 * BEAT, 0.35)
            if sec == "a":
                for beat, m, d in MEL_A[b]:
                    tr.add("bando", bandoneon([m, m - 12], d * BEAT * 0.95, 0, 3000, 1.0), t0 + beat * BEAT, 0.55)
            if sec == "a2":
                for beat, m, d in MEL_A[b]:
                    place(tr, guh.note(m, d * BEAT * 0.98, glide=0.6 if d >= 1 else 0), t0 + beat * BEAT, 1.0)
                    tr.add("bando", bandoneon([m - 12], d * BEAT * 0.95, 0, 2400, 0.8), t0 + beat * BEAT, 0.35)
                tr.add("str", strings(tones, BAR, 0.3, 3200), t0, 0.22)
            if sec == "b":
                for beat, m, d in MEL_B[b]:
                    tr.add("vln", violin(m + 12 if m < 70 else m, d * BEAT * 0.97, (m - 2) if d >= 2 else None, 1.0), t0 + beat * BEAT, 0.35)
                    if b >= 4:      # the guhs join in on the second half
                        place(tr, guh.note(m, d * BEAT * 0.98, glide=0.5 if d >= 1 else 0), t0 + beat * BEAT, 0.85)
                tr.add("str", strings([m - 12 for m in tones], BAR, 0.4, 2400), t0, 0.16)
            if sec == "puente":
                # drama: chicharra scratches on the off-beats, 3-3-2 guh shouts on the chord, a piano run down
                for e in (1, 3, 5, 7):
                    tr.add("drums", chicharra(), t0 + e * E8, 0.9)
                for e, iv in ((0, 12), (3, 7), (6, 12)):
                    place(tr, guh.note(root + 24 + iv if root + 24 + iv < 76 else root + 12 + iv, E8 * 2.2, short=True), t0 + e * E8, 0.9)
                    tr.add("bando", bandoneon(tones, E8 * 2, 0, 3400, 1.0), t0 + e * E8, 0.45)
                for k in range(8):
                    tr.add("keys", piano(tones[-1] + 12 - [0, 2, 3, 5, 7, 8, 10, 12][k], S16 * 2, 0.7), t0 + 2 * BEAT + k * S16, 0.35)
            if sec == "intro":
                if b < 2:           # the bandoneon opens alone: a low chord on the 1, a knock on 1 and 3
                    tr.add("bando", bandoneon([m - 12 for m in tones], 2 * BEAT * 0.95, 0.3, 2000, 0.9), t0, 0.4)
                    tr.add("drums", golpe(), t0, 0.7)
                    tr.add("drums", golpe(), t0 + 2 * BEAT, 0.4)
                if b == 3:
                    place(tr, guh.note(69, 1.2, glide=1.5), t0 + 2 * BEAT, 0.9)     # a guh sighs: "guuuh..."
                if b >= 2:
                    tr.add("str", strings(tones, BAR, 0.8, 2400), t0, 0.18)
            if sec == "coda":
                for beat, m, d in [(0, 74, 1), (1, 73, 1), (2, 74, 2)] if b == 0 else ([(0, 70, 2), (2, 69, 2)] if b == 1 else [(0, 73, 1), (1, 76, 1), (2, 79, 2)]):
                    place(tr, guh.note(m, d * BEAT, glide=0.4), t0 + beat * BEAT, 0.95)
                    tr.add("bando", bandoneon([m, m - 12], d * BEAT * 0.95, 0.4, 3000), t0 + beat * BEAT, 0.45)
            # phrase ends: a cheerful "guh!" on the last off-beat of the a-parts
            if sec in ("a", "a2") and b in (3, 7):
                place(tr, guh.note(tones[-1] + 12 if tones[-1] < 70 else tones[-1], 0.3, short=True), t0 + 3.5 * BEAT, 0.8)
        bar0 += len(chords)

    g = tr.get
    vox = g("vox")
    vox = filt(vox, "high", 140)
    vox = vox + delay(vox, BEAT * 0.75, taps=2, fb=0.4, gain=0.15) + reverb(vox, 1.8, 0.6) * 0.3
    music = g("drums") * 0.8 + (g("bass") * 1.0 + g("keys") * 1.0 + g("bando") + g("vln") + g("str")) * tr.side
    room = reverb(g("bando") + g("vln") + g("str") + g("keys") * 0.5, 2.4, 0.8, 5000) * 0.28
    mix = music + room + vox * 1.25
    for name in ("drums", "bass", "keys", "bando", "vln", "str", "vox"):
        print(f"  {name:6s} rms {np.sqrt(np.mean(g(name) ** 2)):.4f}")
    mix = master_rms(mix, fade_out=0.8)
    return mix


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main",
                                                             "resources", "assets", "guhs", "sounds", "disco", "vadsige_tango.ogg")
    write_ogg(os.path.normpath(out), build())
    print(f"  {BARS} bars, {BARS * 4} beats at {BPM} BPM")
