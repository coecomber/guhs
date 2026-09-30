"""Mika-Mambo (~140 BPM, G minor): the disco's hard song (lastig). -> sounds/disco/mika_mambo.ogg

A fast, cheeky mambo: 2-3 son clave, cowbell on every beat (the game's beat), conga tumbao, guiro, timbale fills,
an anticipated tumbao bass, a piano montuno, a brass section with mambo riffs, and the Mika's as the coro (their
giggly noises, sung on the riff) with the guhs answering "guh!". Beat 0 is at t = 0; 52 bars (the game loops after 208 beats).
"""
import os
import sys

import numpy as np

from guhmix import SR, Tracks, filt, reverb, delay, kick, hat, conga, piano, write_ogg
from guhband import cowbell, clave, guiro, timbale, trumpet, chord, master_rms
from guhstem import Choir, place, library

BPM = 140.0
BEAT = 60 / BPM
BAR = 4 * BEAT
S16 = BEAT / 4

A = ["Gm", "Cm", "D7", "Gm"] * 2
B = ["Eb", "D7", "Cm", "D7"] * 2
SECTIONS = [("intro", ["Gm", "Cm", "D7", "Gm"]), ("a", A), ("b", B), ("break", ["Gm", "Gm", "D7", "D7"]), ("a", A), ("b", B),
            ("montuno", A), ("coda", ["Gm", "Cm", "D7", "Gm"])]
BARS = sum(len(c) for _, c in SECTIONS)

RIFF_POS, RIFF_LEN = [0, 3, 6, 10], [2, 2, 3, 4]
RIFF_TOP = {"Gm": [74, 74, 77, 74], "Cm": [75, 75, 79, 75], "D7": [78, 78, 81, 78], "Eb": [79, 79, 82, 79]}
RIFF_END = [79, 77, 74, 70]
# the Mika coro in the b-part: per bar [(16th, midi, 16ths)]
CORO = [[(0, 79, 2), (2, 79, 2), (4, 77, 2), (6, 75, 6)],
        [(0, 74, 3), (3, 72, 3), (6, 74, 4)],
        [(0, 75, 2), (2, 75, 2), (4, 74, 2), (6, 72, 6)],
        [(0, 74, 3), (3, 78, 3), (6, 81, 6)]]
MONTUNO = [(0, 12), (2, 7), (3, 3), (5, 7), (6, 12), (8, 3), (10, 7), (11, 12), (13, 7), (14, 3)]   # (16th, interval above root)
CONGA = [(0, False, 0.35), (4, True, 1.0), (8, False, 0.35), (12, False, 0.9), (14, False, 1.0)]


def build():
    tr = Tracks(BARS * BAR + 3.0)
    mika = Choir("mika_ambient", max_shift=15)
    guh = Choir("guh_ambient")
    giggles = sorted(library("mika_ambient"), key=lambda s: s.dur)
    names = [s for s, _ in SECTIONS]
    bar0 = 0
    for si, (sec, chords) in enumerate(SECTIONS):
        for b, name in enumerate(chords):
            t0 = (bar0 + b) * BAR
            bar_no = bar0 + b
            root, tones = chord(name)
            if b + 1 < len(chords):
                nxt = chords[b + 1]
            elif si + 1 < len(SECTIONS):
                nxt = SECTIONS[si + 1][1][0]
            else:
                nxt = name
            nroot, _ = chord(nxt)
            last = sec == "coda" and b == len(chords) - 1
            if last:
                # the mambo ending: hits on 1 and on the and-of-2, a Mika squeal, done
                for pos, acc in ((0, 1.0), (6, 1.3)):
                    tb = t0 + pos * S16
                    tr.add("brass", trumpet([m + 12 for m in tones] + [tones[-1] + 12], S16 * 3, acc), tb, 0.5)
                    tr.add("bass", _bass(root, S16 * 3), tb, 0.8)
                    tr.add("keys", sum(piano(m, 0.5, 0.9) for m in tones + [tones[0] + 12]) / 3, tb, 0.5)
                    tr.add("drums", kick(1.0), tb, 0.9)
                    tr.add("perc", timbale(True, True), tb, 1.0)
                    tr.duck(tb, 0.3, BEAT)
                place(tr, mika.note(79, 0.5), t0 + 6 * S16, 1.0)
                continue
            percussion = True
            quiet = sec == "intro" and b < 2
            # --- the beat: cowbell on every beat (accents on 1 and 3), a soft kick, clave 2-3 ---
            for q in range(4):
                tb = t0 + q * BEAT
                tr.add("perc", cowbell(1.0 if q % 2 == 0 else 0.7), tb, 0.9)
                if not quiet:
                    tr.add("drums", kick(0.5, 0.25, 52), tb, 0.5 if q % 2 == 0 else 0.35)
                    tr.duck(tb, 0.3, BEAT * 0.8)
                    tr.add("drums", hat(), tb + BEAT / 2, 0.35)
            for pos in ((4, 8) if bar_no % 2 == 0 else (0, 6, 12)):
                tr.add("perc", clave(), t0 + pos * S16, 0.9)
            if percussion and not quiet:
                for pos, high, acc in CONGA:
                    tr.add("perc", conga(high), t0 + pos * S16, 0.9 * acc)
                for pos, length in ((0, 0.18), (6, 0.08), (8, 0.18), (14, 0.08)):
                    tr.add("perc", guiro(length), t0 + pos * S16, 0.9)
            if sec == "b":            # cascara on the timbale shell
                for pos in (0, 3, 6, 8, 10, 12, 14):
                    tr.add("perc", timbale(True, True), t0 + pos * S16, 0.35)
            if b == len(chords) - 1 and sec not in ("coda",):     # a timbale fill into the next part
                for k, pos in enumerate(range(8, 16)):
                    tr.add("perc", timbale(k % 2 == 0), t0 + pos * S16, 0.45 + 0.05 * k)
            if sec == "break":
                continue_break(tr, t0, b, root, tones, giggles, mika, guh)
                continue
            # --- the tumbao bass: the fifth on the and-of-2, the NEXT chord's root on 4 (anticipated) ---
            if not quiet:
                tr.add("bass", _bass(root + 7 if root + 7 < 48 else root - 5, S16 * 6), t0 + 6 * S16, 0.75)
                tr.add("bass", _bass(nroot, S16 * 4 + BEAT), t0 + 12 * S16, 0.85)
            # --- the piano montuno ---
            for pos, iv in MONTUNO:
                m = root + 24 + iv
                tr.add("keys", piano(m, S16 * 1.6, 0.75) + piano(m + 12, S16 * 1.6, 0.55), t0 + pos * S16, 0.18)
            # --- brass ---
            if (sec == "a" and b >= 4) or sec == "montuno" or (sec == "b" and b >= 4):
                tops = RIFF_END if (b % 4 == 3 and name == "Gm") else RIFF_TOP.get(name, RIFF_TOP["Gm"])
                for pos, ln, top in zip(RIFF_POS, RIFF_LEN, tops):
                    voices = [top, top - 3 if name in ("Gm", "Cm") else top - 4, top - 7]
                    tr.add("brass", trumpet(voices, S16 * ln * 0.9, 1.0, fall=(pos == 10 and b % 4 == 3)), t0 + pos * S16, 0.62)
            # --- the Mika coro and the guhs' answer ---
            if sec == "b" and (b < 4 or b % 2 == 0):
                for pos, m, ln in CORO[b % 4]:
                    place(tr, mika.note(m, S16 * ln * 0.95, glide=0.4), t0 + pos * S16, 0.95)
                place(tr, guh.note(root + 24 if root + 24 < 70 else root + 12, S16 * 2, short=True), t0 + 14 * S16, 0.8)
            if sec == "montuno":
                if b % 2 == 0:        # Mika calls...
                    for pos, m, ln in CORO[(b // 2) % 4][:3]:
                        place(tr, mika.note(m, S16 * ln * 0.95, glide=0.3), t0 + pos * S16, 0.85)
                else:                 # ...the guhs answer, lief and a bit vads
                    for pos, iv in ((0, 12), (3, 7), (6, 12)):
                        place(tr, guh.note(root + 12 + iv if root + 12 + iv < 74 else root + iv, S16 * 2.5, short=True), t0 + pos * S16, 0.85)
            if sec == "intro" and b == 3:
                place(tr, mika.note(81, 0.4, glide=2), t0 + 8 * S16, 0.9)     # "hihi!": here come the Mika's
        bar0 += len(chords)

    g = tr.get
    vox = filt(g("vox"), "high", 150)
    vox = vox + delay(vox, BEAT * 0.75, taps=2, fb=0.35, gain=0.14) + reverb(vox, 1.4, 0.45) * 0.25
    music = g("drums") * 0.8 + g("perc") * 0.85 + (g("bass") * 0.8 + g("keys") + g("brass")) * tr.side
    room = reverb(g("brass") + g("keys") * 0.6 + g("perc") * 0.2, 1.8, 0.55, 6000) * 0.22
    mix = music + room + vox * 1.2
    for name in ("drums", "perc", "bass", "keys", "brass", "vox"):
        print(f"  {name:6s} rms {np.sqrt(np.mean(g(name) ** 2)):.4f}")
    return master_rms(mix, fade_out=0.8)


def _bass(m, dur):
    from guhmix import bass_note
    return bass_note(m, dur, 0.8, 900, 0.08) * 0.9


def continue_break(tr, t0, b, root, tones, giggles, mika, guh):
    """The break: only percussion; the Mika's giggle (as they are, unpitched), then a brass push into the b-part."""
    if b < 2:
        for k, pos in enumerate((2, 7, 10) if b == 0 else (0, 5, 11)):
            s = giggles[(b * 3 + k) % len(giggles)]
            place(tr, s.x * 0.9, t0 + pos * S16, 0.8)
    if b == 2:
        for pos, m in ((0, 74), (3, 74), (6, 78), (10, 81)):
            place(tr, mika.note(m, S16 * 2.5, glide=0.5), t0 + pos * S16, 0.9)
    if b == 3:
        for pos in (0, 3, 6, 10, 12, 14):
            tr.add("brass", trumpet([tones[-1] + 12, tones[1] + 12, tones[0] + 12], S16 * 1.5, 1.0), t0 + pos * S16, 0.34)
        place(tr, guh.note(69, 0.35, short=True), t0 + 12 * S16, 0.8)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main",
                                                             "resources", "assets", "guhs", "sounds", "disco", "mika_mambo.ogg")
    write_ogg(os.path.normpath(out), build())
    print(f"  {BARS} bars, {BARS * 4} beats at {BPM} BPM")
