"""
Op Safari! round 3, slice J2: Safari FM, the radio in the safari truck - three short cheerful tunes (about 9 s each), made
with the helpers of safari_geluid.py (S6), into assets/guhs/sounds/safari/fm_1..3.ogg. The ads between the tunes are
text (the action bar: gui.guhs.safari.fm.reclame.*), the events are registered by safari_rit.py (sounds.json) and
feature/safari/rit/SafariRitten.

Not part of make_resources.py (the OGGs are committed). Needs numpy, scipy and soundfile:
    python tools/features/safari_fm_geluid.py        (from the project root)
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import safari_geluid as g  # noqa: E402

# (bpm, chords (3 notes, Hz), tune, the "shaker" on or off): a marimba calypso, a bouncy ukulele-ish polka, a sleepy waltz-ish
LIEDJES = [
    (132, [(392, 494, 587), (523, 659, 784), (440, 523, 659), (392, 494, 587)],
     [784, 880, 988, 784, 659, 784, 880, 659, 587, 659, 784, 587, 523, 587, 659, 784], True),
    (150, [(349, 440, 523), (466, 587, 698), (523, 659, 784), (349, 440, 523)],
     [698, 784, 880, 1047, 880, 784, 698, 587, 698, 880, 784, 698, 659, 698, 784, 698], True),
    (104, [(330, 415, 494), (440, 554, 659), (370, 466, 554), (330, 415, 494)],
     [659, 740, 831, 988, 831, 740, 659, 554, 494, 554, 659, 740, 831, 740, 659, 494], False),
]


def pluk(f, dur=0.35):
    """A plucked string (ukulele-ish): a few harmonics with a fast decay."""
    t = g.t_(dur)
    return sum(w * np.sin(2 * np.pi * f * (i + 1) * t) for i, w in enumerate((1.0, 0.45, 0.25, 0.12))) * np.exp(-t * 9)


def liedje(bpm, akkoorden, melodie, shaker):
    tel = 60 / bpm
    maten = 8
    delen = []
    for m in range(maten):
        ak = akkoorden[m % len(akkoorden)]
        for b in range(4):
            s = (m * 4 + b) * tel
            delen.append((s, g.toon(ak[0] / 4, tel * 0.85, harm=(1, 0.4), a=0.01, r=0.15), 0.4))      # bass
            delen.append((s + tel / 2, sum(pluk(f, 0.3) for f in ak) / 3, 0.22))                   # off-beat strum
            if shaker:
                sh = g.band(g.ruis(0.05), 5000, 9000) * np.exp(-g.t_(0.05) * 60)
                delen.append((s, sh, 0.12))
                delen.append((s + tel / 2, sh, 0.08))
        for k in range(2):                                                                         # the tune: marimba
            n = melodie[(m * 2 + k) % len(melodie)]
            delen.append(((m * 4 + k * 2) * tel, g.marimba(n, 0.5), 0.5))
            delen.append(((m * 4 + k * 2 + 1) * tel, g.marimba(n * 1.122, 0.4), 0.3))
    # the ident at the end: "Sa-fa-ri F-M!" as three rising bells and a happy honk
    eind = maten * 4 * tel
    for i, f in enumerate((784, 988, 1175)):
        delen.append((eind + i * 0.16, g.marimba(f, 0.5), 0.55))
    delen.append((eind + 0.55, g.toon(330, 0.25, glij=0.12, harm=(1, 0.6, 0.4, 0.2)), 0.35))
    return g.mix(eind + 1.2, *delen)


def main():
    for i, (bpm, ak, mel, sh) in enumerate(LIEDJES, 1):
        g.save(f"fm_{i}", liedje(bpm, ak, mel, sh), peak=0.7)


if __name__ == "__main__":
    main()
