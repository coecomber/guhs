"""
Super Guhrio (bbq2) - the textures of the full engine's pieces (the first pieces' textures are in guhrio.py itself):
the big vadsmunt and its shadow, the switch and the switched blocks, the gate, the grill spit's hub, the hidden block's
icon, the power-ups, Guhshi's egg and head, and the little icons of the invisible spots (so a builder sees what is held).
All 16 x 16, drawn pixel by pixel in the look of the old platform games.
"""
import math

import numpy as np
from PIL import Image


def _leeg(n=16):
    return np.zeros((n, n, 4), np.uint8)


def _vlak(kleur, var, seed, w=16, h=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((h, w, 4), np.uint8)
    n = rng.normal(0, var, (h, w))
    for c in range(3):
        a[..., c] = np.clip(kleur[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _px(a, x, y, kleur, alpha=255):
    if 0 <= y < a.shape[0] and 0 <= x < a.shape[1]:
        a[y, x, :3] = kleur[:3]
        a[y, x, 3] = alpha


def _patroon(a, x0, y0, rijen, kleuren):
    """Rows of characters -> pixels (kleuren: {char: rgb}; '.' is skipped)."""
    for r, rij in enumerate(rijen):
        for c, ch in enumerate(rij):
            if ch in kleuren:
                _px(a, x0 + c, y0 + r, kleuren[ch])


def _rand(a, licht, donker):
    h, w = a.shape[:2]
    a[0, :, :3] = licht
    a[:, 0, :3] = licht
    a[h - 1, :, :3] = donker
    a[:, w - 1, :3] = donker


def vadsmunt(schim=False):
    """The big vadsmunt: a fat gold coin with a guh face on it; its shadow is a grey ring."""
    a = _leeg()
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - 7.5) / 7.4, (y - 7.5) / 7.4)
            if d > 1.0:
                continue
            if schim:
                if d > 0.78:
                    _px(a, x, y, (150, 150, 160), 200)
                elif (x + y) % 3 == 0:
                    _px(a, x, y, (120, 120, 132), 110)
            else:
                _px(a, x, y, (150, 92, 8) if d > 0.86 else (255, 196, 40) if d > 0.66 else (255, 226, 110))
    if not schim:
        gezicht = ["..o....o..",
                   "..o....o..",
                   "..........",
                   "....nn....",
                   ".m..nn..m.",
                   "..mmmmmm.."]
        _patroon(a, 3, 5, gezicht, {"o": (110, 60, 10), "n": (236, 110, 150), "m": (150, 92, 8)})
        for x, y in ((4, 2), (5, 2), (3, 3), (3, 4)):
            _px(a, x, y, (255, 252, 224))                     # a shine
        for x, y in ((2, 1), (13, 1)):                        # two little ears
            _px(a, x, y, (255, 196, 40))
            _px(a, x, y + 1, (150, 92, 8))
    return Image.fromarray(a)


UITROEP = ["..##..",
           "..##..",
           "..##..",
           "..##..",
           "......",
           "..##.."]


def schakelaar(ingedrukt):
    """The switch block: bright blue with a white !; pressed it is dull and the ! is dark."""
    a = _vlak((70, 110, 190) if ingedrukt else (60, 150, 250), 4, 9201 + ingedrukt)
    if ingedrukt:
        _rand(a, (56, 86, 150), (30, 48, 96))
    else:
        _rand(a, (170, 220, 255), (20, 70, 160))
    kleur = (36, 56, 110) if ingedrukt else (255, 255, 255)
    if not ingedrukt:
        _patroon(a, 6, 5, UITROEP, {"#": (20, 70, 160)})
    _patroon(a, 5, 4, UITROEP, {"#": kleur})
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        _px(a, x, y, (20, 70, 160))
    return Image.fromarray(a)


def schakelblok(rood, open_):
    """A switched block: blue (solid while its channel is on) or red (solid while it is off); open = only a dotted outline."""
    basis = (228, 70, 64) if rood else (64, 132, 236)
    licht = tuple(min(255, c + 70) for c in basis)
    donker = tuple(int(c * 0.55) for c in basis)
    if open_:
        a = _leeg()
        for i in range(16):
            if i % 4 < 2:
                for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
                    _px(a, x, y, basis, 220)
        for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
            _px(a, x, y, basis, 120)
        return Image.fromarray(a)
    a = _vlak(basis, 4, 9210 + rood)
    _rand(a, licht, donker)
    for i in range(3, 13):
        _px(a, i, 3, donker)
        _px(a, i, 12, licht)
        _px(a, 3, i, donker)
        _px(a, 12, i, licht)
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        _px(a, x, y, licht)
    return Image.fromarray(a)


def poort():
    """The gate to a level: a doorway of swirling gold stars (a cross model, seen from every side)."""
    a = _leeg()
    for y in range(16):
        for x in range(3, 13):
            rond = y < 3 and (x < 5 or x > 10) and (x - 7.5) ** 2 + (y - 3) ** 2 > 22
            if rond:
                continue
            t = math.sin(x * 1.3 + y * 0.9) + math.sin(y * 1.7 - x * 0.4)
            kleur = (255, 236, 150) if t > 1.0 else (250, 170, 60) if t > 0 else (214, 96, 150) if t > -1.0 else (120, 60, 160)
            _px(a, x, y, kleur, 235)
    for x, y in ((5, 4), (9, 7), (6, 11), (10, 13), (4, 8)):
        _px(a, x, y, (255, 255, 255))
    return Image.fromarray(a)


def naaf():
    """The hub of a grill spit: dark iron with a glowing bolt."""
    a = _vlak((70, 70, 78), 5, 9220)
    _rand(a, (128, 128, 140), (30, 30, 36))
    for y in range(5, 11):
        for x in range(5, 11):
            d = math.hypot(x - 7.5, y - 7.5)
            _px(a, x, y, (255, 200, 80) if d < 1.6 else (220, 90, 30) if d < 2.6 else (44, 44, 50))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        _px(a, x, y, (150, 150, 160))
    return Image.fromarray(a)


def onzichtbaar_icoon():
    """The hidden block as an item: the outline of a ?-block."""
    a = _leeg()
    for i in range(16):
        if i % 2 == 0:
            for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
                _px(a, x, y, (255, 214, 90), 230)
    vraag = ["..####..", ".##..##.", ".....##.", "....##..", "...##...", "........", "...##..."]
    _patroon(a, 4, 4, vraag, {"#": (255, 240, 170)})
    return Image.fromarray(a)


def superknabbel():
    """The Superknabbel: a kaasknabbel wearing a red cap with white dots (and two eyes, of course)."""
    a = _leeg()
    rijen = ["....rrrrrr....",
             "..rrwwrrrrrr..",
             ".rrrwwrrrwwrr.",
             ".rrrrrrrrwwrr.",
             "rrwwrrrrrrrrrr",
             "rrwwrrrrrrrrrr",
             ".dddddddddddd.",
             "..kkkkkkkkkk..",
             "..kkokkkkokk..",
             "..kkokkkkokk..",
             "..kkkkkkkkkk..",
             "...kkkmmkkk...",
             "....kkkkkk...."]
    _patroon(a, 1, 1, rijen, {"r": (228, 48, 44), "w": (255, 255, 255), "d": (150, 28, 30), "k": (250, 206, 96), "o": (60, 36, 20),
                              "m": (200, 130, 40)})
    return Image.fromarray(a)


def vuurpeper():
    """The Vuurpeper: a red pepper with a little flame on top and a smug face."""
    a = _leeg()
    rijen = ["......v.......",
             ".....vyv......",
             "....vyyyv.....",
             ".....vyv..g...",
             "......ggggg...",
             "....rrrrgrr...",
             "...rrrrrrrrr..",
             "..rrorrrrorrr.",
             "..rrorrrrorrr.",
             "..rrrrrrrrrrr.",
             "...rrrmmmrrr..",
             "....rrrrrrr...",
             ".....rrrrr....",
             "......rrr.....",
             ".......d......"]
    _patroon(a, 1, 0, rijen, {"v": (255, 120, 30), "y": (255, 226, 90), "g": (70, 160, 60), "r": (226, 44, 40), "o": (255, 255, 255),
                              "m": (120, 20, 24), "d": (150, 28, 30)})
    return Image.fromarray(a)


def guhshi_ei():
    """Guhshi's egg: white with green spots."""
    a = _leeg()
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - 7.5) / 5.6, (y - 8.4) / 7.2 if y > 8 else (y - 8.4) / 7.9)
            if d <= 1.0:
                _px(a, x, y, (176, 176, 168) if d > 0.86 else (252, 252, 244))
    for cx, cy in ((5, 5), (10, 8), (6, 11), (10, 3)):
        for dx in range(-1, 2):
            for dy in range(-1, 2):
                if abs(dx) + abs(dy) < 2 and a[cy + dy, cx + dx, 3]:
                    _px(a, cx + dx, cy + dy, (96, 196, 80))
    return Image.fromarray(a)


def guhshi_kop():
    """Guhshi waiting: his green head with the big nose, white cheeks and the red crest (an icon)."""
    a = _leeg()
    rijen = ["....rr........",
             "...rggg.......",
             "..rgwowgggg...",
             "..ggwowggggg..",
             ".rggwwwgggggg.",
             ".ggggggggggng.",
             ".ggggggggggng.",
             "..gwwwwggggg..",
             "..wwwwwwggg...",
             "...wwwwww.....",
             "..bb...bb.....",
             ".bbb...bbb...."]
    _patroon(a, 1, 2, rijen, {"g": (108, 194, 74), "w": (252, 252, 244), "o": (30, 30, 40), "r": (226, 52, 48), "n": (50, 110, 44),
                              "b": (240, 140, 40)})
    return Image.fromarray(a)


def plek_icoon(soort):
    """The item pictures of the invisible spots (what a builder holds)."""
    a = _leeg()
    if soort == "schild_mika":
        rijen = ["....gggggg....", "...gggggggg...", "..ggyggggygg..", "..gggggggggg..", ".wwwwwwwwwwww.", "..pppppppppp..", "..ppoppppopp..",
                 "..pppppnnppp..", "...pppppppp...", "..vvv....vvv.."]
        _patroon(a, 1, 3, rijen, {"g": (60, 170, 70), "y": (160, 230, 150), "w": (250, 240, 200), "p": (214, 161, 180), "o": (40, 22, 34),
                                  "n": (236, 110, 150), "v": (112, 72, 88)})
    elif soort == "plof_mika":
        a[2:15, 1:15] = _vlak((120, 122, 132), 6, 9230, 14, 13)
        _rand(a[2:15, 1:15], (176, 178, 190), (60, 60, 70))
        rijen = ["bb......bb", ".bb....bb.", ".ww....ww.", ".wo....ow.", "..........", "....nn....", ".tttttttt.", ".t.t..t.t."]
        _patroon(a, 3, 4, rijen, {"b": (30, 30, 36), "w": (255, 255, 255), "o": (30, 30, 36), "n": (236, 110, 150), "t": (240, 240, 240)})
    elif soort == "hapbloem":
        rijen = ["..rr....rr..", ".rwrr..rrwr.", "rrrrwwwwrrrr", "rrwrw..wrwrr", ".rrrw..wrrr.", "..rrrrrrrr..", ".....gg.....", "..gg.gg.....",
                 "...gggg.gg..", ".....gggg...", ".....gg....."]
        _patroon(a, 2, 2, rijen, {"r": (232, 60, 80), "w": (255, 255, 255), "g": (60, 170, 70)})
    elif soort == "platform":
        a[6:10, 0:16] = _vlak((176, 124, 70), 5, 9231, 16, 4)
        a[6, :, :3] = (226, 176, 110)
        a[9, :, :3] = (96, 60, 30)
        for x in (1, 14):
            for y in range(1, 6):
                _px(a, x, y, (200, 200, 208))
        for x, y in ((6, 12), (7, 13), (8, 13), (9, 12), (4, 12), (11, 12)):
            _px(a, x, y, (255, 226, 110))
    elif soort == "valblok":
        a[6:10, 1:15] = _vlak((150, 150, 158), 6, 9232, 14, 4)
        a[6, 1:15, :3] = (210, 210, 220)
        a[9, 1:15, :3] = (70, 70, 80)
        for x, y in ((4, 7), (5, 8), (9, 7), (10, 8), (11, 7)):
            _px(a, x, y, (60, 60, 70))
        for x, y in ((3, 11), (7, 12), (12, 11), (5, 14), (10, 14)):
            _px(a, x, y, (150, 150, 158))
    return Image.fromarray(a)


def pijp_zij(mond):
    """The side of a pipe that lies down: the same green with the light stripe on top (for the body's x / z axis)."""
    a = _vlak((58, 176, 66), 3, 9240 + mond)
    licht, donker, rand = (150, 236, 140), (24, 104, 40), (14, 70, 30)
    for x in range(16):
        for y, k in ((2, licht), (3, licht), (5, (104, 210, 104)), (12, donker), (13, donker)):
            _px(a, x, y, k)
    a[0, :, :3] = (30, 30, 34)
    a[15, :, :3] = (30, 30, 34)
    a[1, :, :3] = rand
    a[14, :, :3] = rand
    return Image.fromarray(a)
