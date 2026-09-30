"""Njeg-Njeg Boogie (~128 BPM, F blues): the disco's bonus song (own board). -> sounds/disco/njeg_njeg_boogie.ogg

A 12-bar boogie on a disco beat: four on the floor (the game's beat), claps on 2 and 4, a walking boogie-woogie
left hand doubled by the bass, piano riffs, horns and a Hammond, and the guhs singing "njeg-njeg!" with their own
noises. Beat 0 is at t = 0; 52 bars (the game loops after 208 beats).
"""
import os
import sys

import numpy as np

from guhmix import SR, Tracks, filt, reverb, delay, kick, snare, clap, hat, piano, write_ogg
from guhband import trumpet, organ, synth_bass, chord, master_rms
from guhstem import Choir, place

BPM = 128.0
BEAT = 60 / BPM
BAR = 4 * BEAT
S16 = BEAT / 4

BLUES = ["F7", "F7", "F7", "F7", "Bb7", "Bb7", "F7", "F7", "C7", "Bb7", "F7", "C7"]
SECTIONS = [("intro", ["F7", "F7", "C7", "C7"]), ("chorus1", BLUES), ("chorus2", BLUES), ("break", ["F7", "F7", "C7", "C7"]),
            ("chorus3", BLUES), ("outro", ["C7", "Bb7", "F7", "C7", "C7", "Bb7", "F7", "F7"])]
BARS = sum(len(c) for _, c in SECTIONS)
WALK = [0, 4, 7, 9, 10, 9, 7, 4]                  # the boogie-woogie left hand, in eighths
HOOK = [(0, 12, 2), (2, 12, 2), (6, 10, 2), (8, 7, 5)]   # "njeg-njeg, njeg, guuuh" (16th, interval above the root, 16ths)
TURN = [81, 80, 79, 78]                            # the piano's chromatic turnaround (6ths down), bar 12


def root_of(name):
    return chord(name)[0]


def build():
    tr = Tracks(BARS * BAR + 3.0)
    guh = Choir("guh_ambient")
    bar0 = 0
    for si, (sec, chords) in enumerate(SECTIONS):
        for b, name in enumerate(chords):
            t0 = (bar0 + b) * BAR
            root, tones = chord(name)
            last = sec == "outro" and b == len(chords) - 1
            if last:
                # the ending: a boogie run up, then one big F7 hit with a guh "GUUH!"
                for k, iv in enumerate([0, 4, 7, 9, 10, 12]):
                    tr.add("keys", piano(root + 12 + iv, S16 * 1.5, 0.9), t0 + k * S16, 0.35)
                tb = t0 + 8 * S16
                tr.add("keys", sum(piano(m, 1.2, 0.9) for m in tones + [tones[0] + 12, root + 12]) / 3, tb, 0.55)
                tr.add("brass", trumpet([m + 12 for m in tones], 1.0, 1.1, fall=True), tb, 0.6)
                tr.add("bass", synth_bass(root, 1.0), tb, 0.8)
                tr.add("drums", kick(1.0), tb, 1.0)
                tr.add("drums", snare(0.6), tb, 0.6)
                tr.add("drums", hat(True), tb, 0.8)
                place(tr, guh.note(72, 0.6, glide=1.5), tb, 1.1)
                continue
            intro_solo = sec == "intro" and b < 2
            stop_time = sec == "break" and b < 3
            full = sec in ("chorus2", "chorus3", "outro")
            # --- the beat: four on the floor, claps on 2 and 4, open hats on the off-beats ---
            if not intro_solo:
                for q in range(4):
                    tb = t0 + q * BEAT
                    if not stop_time or q == 0:
                        tr.add("drums", kick(1.0 if q % 2 == 0 else 0.85), tb, 0.8)
                        tr.duck(tb, 0.4, BEAT)
                    if q in (1, 3):
                        tr.add("drums", clap(), tb, 0.8)
                        if not stop_time:
                            tr.add("drums", snare(0.5), tb, 0.35)
                    if not stop_time:
                        tr.add("drums", hat(True), tb + BEAT / 2, 0.55)
                        if full:
                            tr.add("drums", hat(), tb + S16, 0.4)
                            tr.add("drums", hat(), tb + 3 * S16, 0.4)
            else:
                for q in range(4):      # (the piano alone: a soft kick keeps the beat for the dancers)
                    tr.add("drums", kick(0.4, 0.25, 50), t0 + q * BEAT, 0.4)
            # --- the boogie-woogie left hand (and the bass doubling it) ---
            if not stop_time:
                for e, iv in enumerate(WALK):
                    m = root + iv
                    tr.add("keys", piano(m + 12, BEAT / 2 * 0.9, 0.8) + piano(m + 24, BEAT / 2 * 0.9, 0.45), t0 + e * BEAT / 2, 0.26)
                    if not intro_solo:
                        tr.add("bass", synth_bass(m, BEAT / 2 * 0.85, 1.0 if e % 2 == 0 else 0.8), t0 + e * BEAT / 2, 0.55)
            else:
                tr.add("keys", sum(piano(m, 0.6, 0.9) for m in [root + 12] + tones) / 2.5, t0, 0.5)
                tr.add("bass", synth_bass(root, 0.5), t0, 0.8)
                tr.add("brass", trumpet([m + 12 for m in tones[1:]], 0.4, 1.0), t0, 0.5)
            # --- piano right hand: Charleston stabs, the turnaround in bar 12 ---
            if sec != "intro" and not stop_time:
                if sec.startswith("chorus") and b == 11:
                    for k, top in enumerate(TURN):
                        tr.add("keys", piano(top, S16 * 3, 0.8) + piano(top - 9, S16 * 3, 0.6), t0 + k * 2 * S16, 0.3)
                else:
                    for pos in (0, 6, 10) if b % 2 == 0 else (2, 6, 12):
                        tr.add("keys", sum(piano(m + 12, S16 * 1.5, 0.7) for m in tones[1:]) / 2, t0 + pos * S16, 0.28)
            # --- horns and Hammond ---
            if sec == "chorus2" or sec == "outro":
                for pos, ln in ((0, 3), (6, 2)):
                    tr.add("brass", trumpet([m + 12 for m in tones[1:]], S16 * ln, 1.0), t0 + pos * S16, 0.45)
            if sec == "chorus3":
                tr.add("org", organ([m + 12 for m in tones], BAR * 0.98, 0.9), t0, 0.35)
                for pos, iv, ln in HOOK:   # the horns take the hook, the guhs sing along an octave up
                    m = root + 24 + iv if root + 24 + iv < 80 else root + 12 + iv
                    tr.add("brass", trumpet([m, m - 5], S16 * ln * 0.9, 1.0, fall=(ln > 3)), t0 + pos * S16, 0.5)
                    if b % 2 == 0:
                        place(tr, guh.note(m, S16 * ln * 0.95, short=ln < 4), t0 + pos * S16, 0.75)
            # --- the guhs: "njeg-njeg!" ---
            if sec == "chorus2":
                if b % 2 == 0:
                    for pos, iv, ln in HOOK:
                        m = root + 24 + iv if root + 24 + iv < 80 else root + 12 + iv
                        place(tr, guh.note(m, S16 * ln * 0.95, glide=0.5 if ln > 3 else 0, short=ln < 4), t0 + pos * S16, 1.0)
                else:               # and the answer: a little guh giggle-run
                    for k, iv in enumerate((7, 10, 12)):
                        place(tr, guh.note(root + 12 + iv if root + 12 + iv < 80 else root + iv, S16 * 1.8, short=True),
                              t0 + (8 + 2 * k) * S16, 0.7)
            if sec == "break":
                if b < 3:           # stop time: "njeg!" on the and-of-1 and a guh answer on 3
                    place(tr, guh.note(root + 24 + 12 if root + 36 < 80 else root + 24, S16 * 2, short=True), t0 + 2 * S16, 0.9)
                    place(tr, guh.note(root + 24 + 7 if root + 31 < 80 else root + 19, S16 * 3, short=True), t0 + 8 * S16, 0.8)
                else:
                    for k in range(8):   # a drum pickup
                        tr.add("drums", snare(0.4), t0 + (8 + k) * S16, 0.2 + 0.05 * k)
            if sec == "intro" and b == 3:
                place(tr, guh.note(77, 0.3, short=True), t0 + 8 * S16, 0.9)
                place(tr, guh.note(77, 0.3, short=True), t0 + 10 * S16, 0.9)      # "njeg-njeg!"
        bar0 += len(chords)

    g = tr.get
    vox = filt(g("vox"), "high", 150)
    vox = vox + delay(vox, BEAT / 2, taps=3, fb=0.45, gain=0.15) + reverb(vox, 1.5, 0.45) * 0.22
    music = g("drums") * 0.7 + (g("bass") * 0.8 + g("keys") + g("brass") + g("org")) * g_side(tr)
    room = reverb(g("keys") * 0.6 + g("brass") + g("org"), 1.8, 0.5, 6000) * 0.2
    mix = music + room + vox * 1.6
    for name in ("drums", "bass", "keys", "brass", "org", "vox"):
        print(f"  {name:6s} rms {np.sqrt(np.mean(g(name) ** 2)):.4f}")
    return master_rms(mix, fade_out=1.0)


def g_side(tr):
    return tr.side


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main",
                                                             "resources", "assets", "guhs", "sounds", "disco", "njeg_njeg_boogie.ogg")
    write_ogg(os.path.normpath(out), build())
    print(f"  {BARS} bars, {BARS * 4} beats at {BPM} BPM")
