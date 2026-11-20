"""
1.10.10: the Erfenis Muziekdoosje's dance tune, "Het Guhdansje" (mono OGG Vorbis, 44.1 kHz) into
assets/guhs/sounds/muziekdoosje/dansje.ogg. Run by hand, NOT part of make_resources.py (it needs scipy and soundfile; the
OGG is committed as a binary; erfenis_muziek.py only lists it in sounds.json):

    python tools/features/erfenis_muziek_geluid.py          (from the project root; fixed seeds: the same every run)

An own composition: a sweet, bouncy music-box dance in C major, 128 BPM, 16 bars of 4/4 = exactly 30.0 seconds (the song
lasts ErfenisMuziek.DUUR = 600 ticks), plus a short ring-out of the last chord.
  bars 1-2    intro: music-box arpeggios over a shaker, then a "guh!"
  bars 3-10   the tune (C Am F G, twice) on the music box, a plucky bass hopping between root and octave, a soft
              four-on-the-floor kick with handclaps on 2 and 4, little offbeat chord plinks; a "guh!" at each phrase end
  bars 11-14  the bridge (F G Em G): glockenspiel doubles the tune an octave up, the box an octave down, "guh-guh!"
  bars 15-16  the ending: a run down to the last C, a sparkly glock and one big happy "guh!"
The "guh!"s are the mod's own guh voice (guh_ambient14 and guh_ambient3), pitched up a little; the rest is synthesised
with the instruments of einde2_sp_audio_geluid.py (muziekdoos, glock, pizz, bas, kick, clap, shaker, hat).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import einde2_audio_geluid as g  # noqa: E402  (env, low, high, galm, belletje, ...)
import einde2_sp_audio_geluid as sp  # noqa: E402  (Stuk, melodie_op, akkoord, muziekdoos, glock, pizz, kick, clap, ...)

SR = g.SR
OUT = os.path.join("src", "main", "resources", "assets", "guhs", "sounds", "muziekdoosje")
STEM = os.path.join("src", "main", "resources", "assets", "guhs", "sounds")
BPM = 128
MATEN = 16

AKKOORDEN = ["C", "G",                                   # intro
             "C", "Am", "F", "G", "C", "Am", "F", "G",   # the tune, twice
             "F", "G", "Em", "G",                        # the bridge
             "F", "C"]                                   # the ending (bar 15: F, then G on beat 3)

# the tune, per bar: (note or None, beats); four beats a bar
WIJS = {
    2: [("E5", .5), ("G5", .5), ("C6", 1), ("B5", .5), ("C6", .5), ("G5", 1)],
    3: [("A5", .5), ("C6", .5), ("E6", 1), ("D6", .5), ("C6", .5), ("A5", 1)],
    4: [("F5", .5), ("A5", .5), ("C6", 1), ("A5", .5), ("C6", .5), ("D6", .5), ("C6", .5)],
    5: [("B5", .5), ("A5", .5), ("G5", 1), ("D5", 1), (None, 1)],
    6: [("E5", .5), ("G5", .5), ("C6", 1), ("B5", .5), ("C6", .5), ("G5", 1)],
    7: [("A5", .5), ("C6", .5), ("E6", 1), ("G6", .5), ("E6", .5), ("C6", 1)],
    8: [("A5", .5), ("C6", .5), ("F6", .5), ("E6", .5), ("D6", .5), ("C6", .5), ("A5", 1)],
    9: [("G5", .5), ("B5", .5), ("D6", .5), ("B5", .5), ("C6", 1), (None, 1)],
    10: [("A5", 1), ("A5", .5), ("G5", .5), ("A5", 1), ("C6", 1)],
    11: [("B5", 1), ("B5", .5), ("A5", .5), ("B5", 1), ("D6", 1)],
    12: [("G5", .5), ("B5", .5), ("E6", 1), ("D6", .5), ("B5", .5), ("G5", 1)],
    13: [("B5", .5), ("D6", .5), ("G6", .5), ("F6", .5), ("D6", 1), (None, 1)],
    14: [("F6", .5), ("E6", .5), ("D6", .5), ("C6", .5), ("B5", .5), ("C6", .5), ("D6", 1)],
    15: [("C6", 4)],
}
# "guh!" (bar, beat, which voice, pitch factor, volume)
GUHS = [(1, 3.0, 14, 1.35, 0.55),
        (5, 3.0, 14, 1.35, 0.5), (9, 3.0, 3, 1.12, 0.45),
        (13, 3.0, 14, 1.42, 0.45), (13, 3.5, 14, 1.6, 0.45),
        (15, 1.0, 3, 1.2, 0.6)]


def guh_stem(n, factor):
    """The mod's own guh voice n, pitched up by `factor` (and so a bit shorter), trimmed to the sound, soft edges."""
    sf = g._sf()
    x, sr = sf.read(os.path.join(STEM, f"guh_ambient{n}.ogg"), dtype="float64")
    if x.ndim > 1:
        x = x.mean(axis=1)
    blok = max(1, int(0.01 * sr))
    e = np.array([np.sqrt(np.mean(x[i:i + blok] ** 2)) for i in range(0, len(x) - blok, blok)])
    aan = np.nonzero(e > 0.08 * e.max())[0]
    x = x[max(0, aan[0] - 1) * blok:(aan[-1] + 2) * blok]
    n_uit = int(len(x) * SR / sr / factor)
    y = np.interp(np.linspace(0, len(x) - 1, n_uit), np.arange(len(x)), x)
    y = g.high(y, 120)
    return y / (np.max(np.abs(y)) + 1e-9) * g.env(len(y), 0.004, 0.04, 1.0)


def dansje():
    s = sp.Stuk(BPM, 4)
    for bar in range(MATEN):
        b0 = bar * 4
        ak = AKKOORDEN[bar]
        n = sp.akkoord(ak, 4)
        intro, brug, slot = bar < 2, 10 <= bar < 14, bar >= 14
        # --- the bass: root on the beat, octave on the offbeat (bar 15: F, then G; bar 16: one low C)
        if bar == 15:
            s.zet(b0, sp.pizz(sp.mhz(sp.akkoord("C", 2)[0]), 1.5, 900), 0.55)
            s.zet(b0, sp.bas(sp.mhz(sp.akkoord("C", 1)[0]), 1.2, 901), 0.35)
        elif not intro:
            for tel in range(4):
                naam = "G" if bar == 14 and tel >= 2 else ak
                r = sp.akkoord(naam, 2)[0]
                s.zet(b0 + tel, sp.pizz(sp.mhz(r), 0.3, bar * 8 + tel), 0.5)
                s.zet(b0 + tel + 0.5, sp.pizz(sp.mhz(r + 12), 0.2, bar * 8 + tel + 4), 0.3)
                if tel in (0, 2):
                    s.zet(b0 + tel, sp.bas(sp.mhz(r - 12), 0.25, bar), 0.18)
        # --- the drums: soft and light; a shaker all along, the kick and claps from bar 3
        for i in range(8 if bar < 15 else 2):
            s.zet(b0 + i * 0.5, sp.shaker(bar * 8 + i, 1.0 if i % 2 else 0.55), 0.55)
        if not intro and bar < 15:
            for tel in range(4):
                s.zet(b0 + tel, sp.kick(bar * 4 + tel, 0.9 if tel % 2 == 0 else 0.7), 0.42)
                s.zet(b0 + tel + 0.5, sp.hat(bar * 4 + tel, 0.7), 0.35)
                if tel % 2 == 1:
                    s.zet(b0 + tel, sp.clap(bar * 4 + tel, 0.8), 0.32)
            if bar in (9, 13):   # a little fill into the next part
                for k, tel in enumerate((3.25, 3.5, 3.75)):
                    s.zet(b0 + tel, sp.clap(500 + bar * 3 + k, 0.5 + 0.15 * k), 0.25)
        if bar == 15:
            s.zet(b0, sp.kick(999, 1.0), 0.45)
            s.zet(b0, sp.crash(998, 0.5, 2.0), 0.12)
        # --- the music box: arpeggios in the intro, offbeat chord plinks under the tune
        if intro:
            for i in range(8):
                m = (n + [x + 12 for x in n] + [n[0] + 24])[[0, 1, 2, 3, 4, 5, 6, 5][i]]
                s.zet(b0 + i * 0.5, sp.muziekdoos(sp.mhz(m + 12), 0.45, 30 + bar * 8 + i), 0.22)
        elif bar < 15:
            for tel in range(4):
                naam = "G" if bar == 14 and tel >= 2 else ak
                for j, m in enumerate(sp.akkoord(naam, 4)):
                    s.zet(b0 + tel + 0.5, sp.muziekdoos(sp.mhz(m), 0.25, bar * 40 + tel * 4 + j), 0.07)
        else:
            for j, m in enumerate(sp.akkoord("C", 4) + [sp.midi("C6")]):
                s.zet(b0 + j * 0.12, sp.muziekdoos(sp.mhz(m), 2.5, 950 + j), 0.12)
        # --- the tune
        if bar in WIJS:
            sp.melodie_op(s, WIJS[bar], b0, sp.muziekdoos, 0.36, seed=100 + bar * 10)
            if brug or slot:
                sp.melodie_op(s, WIJS[bar], b0, sp.glock, 0.14, seed=300 + bar * 10, transpose=12)
            if brug:
                sp.melodie_op(s, WIJS[bar], b0, sp.muziekdoos, 0.10, seed=500 + bar * 10, transpose=-12)
        if bar == 15:
            for k, noot in enumerate(("C7", "E7", "G7", "C8")):
                s.zet(b0 + 1 + k * 0.25, g.belletje(sp.hz(noot), 0.9, 960 + k), 0.08)
    # --- the guhs
    for bar, tel, stem, factor, vol in GUHS:
        s.zet(bar * 4 + tel, guh_stem(stem, factor), vol)
    lengte = MATEN * 4
    x = s.mix(lengte, staart=1.0)
    x = g.galm(x, 1.1, 0.12, 7)[:len(x)]
    x *= g.env(len(x), 0.005, 0.9, 1.5)
    return sp.luid(x, -19.0)


def main():
    os.makedirs(OUT, exist_ok=True)
    x = dansje()
    sp.OUT = OUT
    sp.save("dansje", x)


if __name__ == "__main__":
    main()
