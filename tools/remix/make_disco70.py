"""70's disco version (the disco's standard song): -> ze_hangen_disco70.ogg"""
import numpy as np
from guhmix import *  # noqa: F401,F403
from lyrics import CHOP, HOOK, LINES, ZE_HANGEN_3X, ZE_HANGEN_3X_2, ZIN, ZIN_2

BPM = 110.0
BEAT = 60 / BPM
BAR = 4 * BEAT
S16 = BEAT / 4

VERSE = [(45, [57, 60, 64, 67, 71]), (45, [57, 60, 64, 67, 71]), (50, [54, 57, 60, 64]), (50, [54, 57, 60, 64])]
CHORUS = [(41, [57, 60, 64, 65]), (43, [55, 59, 62, 64]), (40, [55, 59, 62, 64]), (45, [55, 57, 60, 64])]
SECTIONS = [("intro", 8), ("vers", 8), ("refrein", 8), ("break", 4), ("vers", 8), ("refrein", 8), ("outro", 6)]
TOTAL = sum(b for _, b in SECTIONS)
tr = Tracks(TOTAL * BAR + 4)

K, SN, H, HO, TB, CH, CL = kick(), snare(), hat(), hat(True), tambourine(), conga(True), conga(False)
BASS_PAT = [(0, 0, 1.0), (2, 12, 0.7), (3, 0, 0.5), (4, 0, 0.9), (6, 12, 0.8), (8, 0, 1.0), (10, 12, 0.7),
            (11, 10, 0.6), (12, 0, 0.9), (13, 12, 0.5), (14, 7, 0.8), (15, 12, 0.6)]
GTR_PAT = [2, 6, 7, 10, 14, 15]
CONGA_PAT = [(3, True), (6, False), (7, True), (11, True), (14, False), (15, True)]

starts = {}
bar0 = 0
for sec, bars in SECTIONS:
    starts.setdefault(sec, []).append(bar0)
    for b in range(bars):
        t0 = (bar0 + b) * BAR
        root, chord = (CHORUS if sec == "refrein" else VERSE)[b % 4]
        early = sec == "intro" and b < 4
        full = sec in ("vers", "refrein") or (sec == "outro" and b < 4)
        for q in range(4):
            tb = t0 + q * BEAT
            if not (sec == "break" and b < 2):
                tr.add("drums", K, tb, 0.9)
                tr.duck(tb, 0.45, BEAT)
            if q in (1, 3) and not early and sec != "break":
                tr.add("drums", SN, tb, 0.55)
            tr.add("drums", HO, tb + BEAT / 2, 0.9 if full else 0.6)
            if full or sec == "break":
                tr.add("drums", H, tb + S16, 0.5)
                tr.add("drums", H, tb + 3 * S16, 0.5)
        for s in range(16):
            if not early or s % 2 == 0:
                tr.add("perc", TB, t0 + s * S16, 0.9 if s % 4 == 2 else 0.55)
        if sec != "intro":
            for s, hi in CONGA_PAT:
                tr.add("perc", CH if hi else CL, t0 + s * S16, 0.8)
        if not early:
            for s, iv, acc in BASS_PAT:
                tr.add("bass", bass_note(root + iv, S16 * 1.6, acc), t0 + s * S16, 0.55 * (0.7 + 0.3 * acc))
        if sec in ("vers", "break", "outro") or (sec == "intro" and b >= 4):
            for s in (0, 6, 10):
                for m in chord:
                    tr.add("keys", rhodes(m, S16 * (4 if s == 0 else 3), 0.8), t0 + s * S16, 0.09)
        if sec != "break" and not early:
            for k, s in enumerate(GTR_PAT):
                tr.add("gtr", sum(pluck(m + 12, 0.09, 0.9, 0.97) for m in chord[:3]) / 3, t0 + s * S16, 0.9)
        if sec in ("intro", "refrein", "outro") or (sec == "vers" and b >= 4):
            tr.add("str", strings([m + 12 for m in chord[:4]], BAR, 0.6 if sec == "intro" else 0.25,
                                  5000 if sec == "refrein" else 3000), t0, 0.33 if sec == "refrein" else 0.2)
        if sec == "refrein":
            tr.add("brass", brass([m + 12 for m in chord[1:4]], S16 * 2), t0 + 6 * S16, 0.28)
            tr.add("brass", brass([m + 12 for m in chord[1:4]], S16 * 3), t0 + 14 * S16, 0.32)
    bar0 += bars

for st in starts["refrein"] + starts["break"]:
    tr.add("str", string_run(57, 81, BAR * 0.5), st * BAR - BAR * 0.5, 0.35)
bs = starts["break"][0] * BAR
for k in range(16):
    tr.add("drums", SN, bs + 3 * BAR + k * S16, 0.15 + 0.4 * k / 16)

# vocals: every line complete; the chorus = the whole song text
i0 = starts["intro"][0] * BAR
for q in (0, 2, 3):
    sing(tr, CHOP, i0 + 4 * BAR + q * BEAT, 0.8)
sing(tr, HOOK, i0 + 6 * BAR, 0.9)
for vs in starts["vers"]:
    for i, seg in enumerate(LINES):
        sing(tr, seg, (vs + 2 * i) * BAR)
for rs in starts["refrein"]:
    sing(tr, ZIN, rs * BAR)
    sing(tr, ZE_HANGEN_3X, (rs + 2) * BAR)
    sing(tr, ZIN_2, (rs + 4) * BAR)
    sing(tr, ZE_HANGEN_3X_2, (rs + 6) * BAR)
sing(tr, ZIN, bs)
for q in (0, 1, 2, 2.5, 3, 3.5):
    sing(tr, CHOP, bs + 2 * BAR + q * BEAT, 0.7)
os_ = starts["outro"][0] * BAR
sing(tr, ZIN, os_)
sing(tr, ZE_HANGEN_3X, os_ + 2 * BAR, 0.9)

vox = vocal_chain(tr.get("vox"), "disco", BEAT)
g = tr.get
music = (g("drums") * 0.85 + g("perc") * 0.7 + (g("bass") * 0.9 + g("keys") + g("str")) * tr.side
         + g("gtr") * 0.55 + g("brass"))
room = reverb(g("str") + g("keys") + g("brass") + g("drums") * 0.2, 2.2, 0.6, 5000) * 0.18
mix = music + room + vox * 1.7
iend = int(4 * BAR * SR)
mix[:iend] = sweep_lp(mix[:iend], lambda t: 400 * (16000 / 400) ** (t / (4 * BAR)))
mix = master(mix[: int((TOTAL * BAR + 2.5) * SR)], fade_out=4 * BAR)
write_ogg(os.path.join(HERE, "ze_hangen_disco70.ogg"), mix)
