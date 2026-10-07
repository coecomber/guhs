"""
Super Guhrio wereld 1 (bbq2, slice guhrio-w1) - the textures of the binnentuin: the painted garden the two levels are
built from. All 16 x 16, drawn pixel by pixel like a stage set that somebody painted with a broad brush (flat colours, a
light edge on top, a dark one below, a few visible strokes): grass on earth, a hedge, a cloud (one with a happy snoet),
the sky, a hill (one with the eyes hills have in the old games), four little plants, and the icons of the two invisible
pieces (the tip and the secret).
"""
import math

import numpy as np
from PIL import Image

GRAS, GRAS_LICHT, GRAS_DONKER = (104, 204, 74), (170, 236, 112), (52, 142, 60)
AARDE, AARDE_LICHT, AARDE_DONKER = (176, 112, 64), (212, 150, 96), (122, 70, 40)
HEG, HEG_LICHT, HEG_DONKER = (62, 164, 74), (120, 208, 96), (36, 112, 60)
LUCHT, LUCHT_LICHT = (132, 200, 246), (164, 218, 250)
WOLK, WOLK_SCHADUW, WOLK_RAND = (250, 252, 255), (206, 226, 246), (150, 184, 226)
WOLK_VER, WOLK_VER_LICHT = (196, 228, 250), (218, 238, 252)
LOOF, LOOF_LICHT, LOOF_DONKER = (70, 148, 122), (98, 172, 140), (52, 122, 108)
HEUVEL, HEUVEL_LICHT, HEUVEL_DONKER = (128, 206, 98), (176, 232, 130), (88, 168, 82)
OOG = (38, 30, 44)


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


def _streken(a, kleur, seed, aantal, lang=(2, 5)):
    """Short horizontal brush strokes (they wrap, so the block tiles)."""
    rng = np.random.default_rng(seed)
    for _ in range(aantal):
        x, y, n = int(rng.integers(0, 16)), int(rng.integers(0, 16)), int(rng.integers(lang[0], lang[1] + 1))
        for k in range(n):
            _px(a, (x + k) % 16, y, kleur)


def _patroon(a, x0, y0, rijen, kleuren):
    for r, rij in enumerate(rijen):
        for c, ch in enumerate(rij):
            if ch in kleuren:
                _px(a, x0 + c, y0 + r, kleuren[ch])


def aarde():
    """Painted earth: warm brown with a few lighter and darker dabs and a pebble or two."""
    a = _vlak(AARDE, 3, 9401)
    _streken(a, AARDE_LICHT, 9402, 5, lang=(1, 3))
    _streken(a, AARDE_DONKER, 9403, 6, lang=(1, 2))
    for x, y in ((3, 4), (11, 9), (6, 13)):
        _px(a, x, y, AARDE_DONKER)
        _px(a, x + 1, y, AARDE_DONKER)
        _px(a, x, y - 1, AARDE_LICHT)
    return Image.fromarray(a)


def gras_zij():
    """The side of the lawn: a fringe of grass hanging over the earth."""
    a = np.array(aarde())
    rand = [4, 5, 4, 3, 4, 5, 5, 4, 3, 4, 5, 4, 4, 3, 4, 5]        # how deep the fringe hangs in each column
    for x in range(16):
        for y in range(rand[x]):
            _px(a, x, y, GRAS_LICHT if y == 0 else GRAS)
        _px(a, x, rand[x], GRAS_DONKER)
    for x in (2, 7, 12):                                            # a blade that hangs a little lower
        _px(a, x, rand[x] + 1, GRAS_DONKER)
    return Image.fromarray(a)


def gras_boven():
    a = _vlak(GRAS, 3, 9404)
    _streken(a, GRAS_LICHT, 9405, 9)
    _streken(a, GRAS_DONKER, 9406, 6, lang=(1, 3))
    return Image.fromarray(a)


def heg():
    """A clipped hedge: round dabs of leaves, light from the top left."""
    a = _vlak(HEG, 3, 9407)
    for cx, cy in ((3, 3), (11, 2), (7, 8), (14, 10), (2, 12), (10, 14)):
        for dx, dy in ((0, -2), (-1, -2), (-2, -1), (-2, 0)):
            _px(a, (cx + dx) % 16, (cy + dy) % 16, HEG_LICHT)
        for dx, dy in ((2, 1), (2, 2), (1, 2), (0, 2), (-1, 2)):
            _px(a, (cx + dx) % 16, (cy + dy) % 16, HEG_DONKER)
    return Image.fromarray(a)


def lucht():
    """The painted sky: one calm blue with a few pale strokes."""
    a = _vlak(LUCHT, 1.5, 9408)
    _streken(a, LUCHT_LICHT, 9409, 4, lang=(3, 6))
    return Image.fromarray(a)


def loof():
    """Leaves far away (the painted wall's trees and hedges): a cooler, bluer green than the hedge you can stand on."""
    a = _vlak(LOOF, 2, 9413)
    for cx, cy in ((4, 4), (12, 3), (8, 10), (15, 12), (2, 13)):
        for dx, dy in ((0, -1), (-1, -1), (-1, 0)):
            _px(a, (cx + dx) % 16, (cy + dy) % 16, LOOF_LICHT)
        for dx, dy in ((1, 1), (0, 1)):
            _px(a, (cx + dx) % 16, (cy + dy) % 16, LOOF_DONKER)
    return Image.fromarray(a)


def wolk():
    """A cloud you can stand on: bright white, a firm blue shadow along the bottom."""
    a = _vlak(WOLK, 1, 9410)
    for x in range(16):
        diep = 3 if x % 5 in (1, 2, 3) else 2
        for y in range(16 - diep, 16):
            _px(a, x, y, WOLK_SCHADUW)
        _px(a, x, 15, WOLK_RAND)
        _px(a, x, 0, (255, 255, 255))
    _streken(a, WOLK_SCHADUW, 9411, 3, lang=(2, 4))
    return Image.fromarray(a)


def wolk_ver(snoet=False):
    """A cloud far away on the painted sky: pale, soft; the snoet is a guh's: two tall eyes, a little smile, a blush."""
    a = _vlak(WOLK_VER, 1, 9414)
    _streken(a, WOLK_VER_LICHT, 9415, 6, lang=(3, 6))
    if snoet:
        for x in (5, 10):
            for y in (5, 6, 7):
                _px(a, x, y, OOG)
            _px(a, x, 5, (90, 84, 110))
        _patroon(a, 6, 9, ["#..#", ".##."], {"#": OOG})
        for x in (3, 12):                                           # blushing cheeks
            _px(a, x, 9, (255, 176, 196))
            _px(a, x, 10, (255, 176, 196))
    return Image.fromarray(a)


def heuvel(ogen=False):
    """A painted hill: fresh green with slanting strokes; with eyes it is one of the hills that watch you walk by."""
    a = _vlak(HEUVEL, 2, 9412)
    for k in range(-16, 16, 5):
        for i in range(16):
            x, y = (k + i) % 16, i
            if (i // 3) % 2 == 0:
                _px(a, x, y, HEUVEL_LICHT if (k // 5) % 2 == 0 else HEUVEL_DONKER)
    if ogen:
        for x in (5, 10):
            for y in range(4, 11):
                _px(a, x, y, OOG)
                _px(a, x + 1, y, OOG)
            _px(a, x, 4, (255, 255, 255))
    return Image.fromarray(a)


PLANTEN = (
    # red knabbelbloem, yellow one, a daisy, a tuft of grass
    ([".......rr.......", "......rrrr......", ".....rryyrr.....", ".....rryyrr.....", "......rrrr......", ".......rr.......",
      ".......g........", ".......g..gg....", "...gg..g.gg.....", "....gg.gg.......", ".....gggg.......", "......gg........",
      "......gg........", "......gg........", "......gg........", "......gg........"], {"r": (232, 64, 70), "y": (255, 214, 72), "g": (58, 150, 64)}),
    ([".......yy.......", "......yyyy......", ".....yyooyy.....", ".....yyooyy.....", "......yyyy......", ".......yy.......",
      "........g.......", "....gg..g.......", ".....gg.g..gg...", ".......gg.gg....", ".......gggg.....", "........gg......",
      "........gg......", "........gg......", "........gg......", "........gg......"], {"y": (255, 220, 84), "o": (236, 138, 40), "g": (58, 150, 64)}),
    (["................", "................", "................", "......w.w.......", ".....wwwww......", "......wyw.......",
      ".....wwwww......", "......w.w.......", ".......g........", "....g..g........", ".....g.g..g.....", "......gg.g......",
      ".......ggg......", ".......gg.......", ".......gg.......", ".......gg......."], {"w": (252, 252, 252), "y": (255, 214, 72), "g": (58, 150, 64)}),
    (["................", "................", "................", "................", "................", "................",
      "................", "................", "................", "...l......l.....", "...g..l...g..l..", "..lg..g..lg..g..",
      "..gg.lg..gg.lg..", ".lgg.gg.lgg.gg..", ".ggglgg.ggglgg..", ".ggggggggggggg.."], {"g": GRAS_DONKER, "l": GRAS}),
)


def plant(soort):
    """One of the four little plants that stand on the lawn (a cross: you see it from both sides)."""
    a = np.zeros((16, 16, 4), np.uint8)
    rijen, kleuren = PLANTEN[soort]
    _patroon(a, 0, 0, rijen, kleuren)
    return Image.fromarray(a)


def tip_icoon():
    """The tip in a builder's hand: a speech balloon with an exclamation mark."""
    a = np.zeros((16, 16, 4), np.uint8)
    _patroon(a, 1, 1, [".############.", "#wwwwwwwwwwww#", "#wwwwwrrwwwww#", "#wwwwwrrwwwww#", "#wwwwwrrwwwww#", "#wwwwwrrwwwww#",
                       "#wwwwwwwwwwww#", "#wwwwwrrwwwww#", "#wwwwwwwwwwww#", ".###ww#######.", "...#w#........", "...##........."],
             {"#": OOG, "w": (255, 255, 255), "r": (232, 64, 70)})
    return Image.fromarray(a)


def geheim_icoon():
    """The secret in a builder's hand: a golden star with a keyhole."""
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            hoek = math.atan2(dy, dx)
            straal = 4.2 + 3.2 * math.cos(5 * (hoek + math.pi / 2)) ** 2 * 0.9
            d = math.hypot(dx, dy)
            if d <= straal:
                _px(a, x, y, (255, 214, 72) if d < straal - 1.1 else (196, 132, 30))
    _patroon(a, 6, 5, [".##.", "####", ".##.", ".##.", ".##."], {"#": OOG})
    return Image.fromarray(a)
