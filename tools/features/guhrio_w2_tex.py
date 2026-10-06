"""
Super Guhrio, world 2 (bbq2 guhrio-w2) - the textures of the cellar's own blocks, all drawn here (16 x 16):

  keldergrond / keldersteen   the engine's ground slabs and bricks in the teal of an old underground level
  warppijp (+ boven), warpbuis  a warp pipe: gold with a purple swirl in its mouth, a purple body
  eislot                      the lock of the egg gate: a steel plate with an egg-shaped keyhole
  nest (boven, zij)           the warm nest: a little stove with a bed of straw
  broedplek                   the item picture of the invisible hatching spot (an egg with a crack)
"""
import math

import numpy as np
from PIL import Image


def _vlak(kleur, var, seed, w=16, h=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((h, w, 4), np.uint8)
    n = rng.normal(0, var, (h, w))
    for c in range(3):
        a[..., c] = np.clip(kleur[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _px(a, x, y, kleur):
    if 0 <= y < a.shape[0] and 0 <= x < a.shape[1]:
        a[y, x, :3] = kleur[:3]
        a[y, x, 3] = 255


def keldergrond():
    """The engine's ground (slabs with dark seams and a light edge), teal."""
    a = _vlak((40, 126, 138), 4, 22101)
    donker, licht = (12, 52, 66), (128, 212, 216)
    for (x0, y0, x1, y1) in ((0, 0, 9, 7), (10, 0, 15, 4), (10, 5, 15, 11), (0, 8, 5, 15), (6, 8, 9, 11), (6, 12, 15, 15)):
        for x in range(x0, x1 + 1):
            _px(a, x, y0, licht)
            _px(a, x, y1, donker)
        for y in range(y0, y1 + 1):
            _px(a, x0, y, licht)
            _px(a, x1, y, donker)
    return Image.fromarray(a)


def keldersteen():
    """Bricks, four rows, every other row shifted: blue-teal with dark joints."""
    a = _vlak((34, 104, 142), 5, 22102)
    voeg = (10, 34, 56)
    for y in (3, 7, 11, 15):
        a[y, :, :3] = voeg
    for rij, y0 in enumerate((0, 4, 8, 12)):
        a[y0, :, :3] = np.clip(a[y0, :, :3].astype(int) + 30, 0, 255)       # a light top to every brick
        for x in ((7, 15) if rij % 2 == 0 else (3, 11)):
            for y in range(y0, y0 + 3):
                _px(a, x, y, voeg)
    return Image.fromarray(a)


def warppijp():
    """The side of a warp pipe's mouth: gold, round (a light stripe left, a dark one right), a thick rim, a purple band."""
    a = _vlak((244, 190, 48), 3, 22103)
    licht, donker, rand = (255, 240, 160), (170, 104, 14), (112, 62, 8)
    for y in range(16):
        for x, k in ((2, licht), (3, licht), (5, (252, 214, 96)), (12, donker), (13, donker)):
            _px(a, x, y, k)
    for y in (6, 7, 8):
        a[y, :, :3] = (150, 70, 200) if y != 7 else (196, 120, 240)
    a[0, :, :3] = rand
    a[15, :, :3] = rand
    a[14, :, :3] = donker
    a[:, 0, :3] = rand
    a[:, 15, :3] = rand
    return Image.fromarray(a)


def warppijp_boven():
    """Looking into a warp pipe: a gold rim round a purple swirl."""
    a = _vlak((244, 190, 48), 3, 22104)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.6:
                a[y, x, :3] = (112, 62, 8)
            elif d < 5.6:
                hoek = math.atan2(y - 7.5, x - 7.5)
                r = math.hypot(x - 7.5, y - 7.5)
                golf = math.sin(hoek * 2 + r * 0.9)
                a[y, x, :3] = (204, 140, 250) if golf > 0.5 else (120, 54, 176) if golf > -0.3 else (54, 20, 92)
    for x, y in ((7, 7), (8, 8)):
        _px(a, x, y, (255, 240, 255))
    return Image.fromarray(a)


def warpbuis():
    """The body of a warp pipe: purple, a little narrower than its mouth."""
    a = _vlak((132, 60, 190), 3, 22105)
    for y in range(16):
        for x, k in ((2, (206, 150, 250)), (3, (206, 150, 250)), (5, (170, 104, 226)), (12, (70, 26, 112)), (13, (70, 26, 112))):
            _px(a, x, y, k)
    a[:, 0, :3] = (24, 22, 30)
    a[:, 15, :3] = (24, 22, 30)
    a[:, 1, :3] = (46, 14, 78)
    a[:, 14, :3] = (46, 14, 78)
    return Image.fromarray(a)


EI = ["...####...",
      "..######..",
      ".########.",
      ".########.",
      "##########",
      "##########",
      "##########",
      "##########",
      ".########.",
      ".########.",
      "..######..",
      "...####..."]


def _ei(a, x0, y0, wit=(250, 250, 244), vlek=(96, 190, 78), schaduw=(206, 212, 200)):
    """Guhshi's egg, 10 x 12: white with green spots."""
    for r, rij in enumerate(EI):
        for c, ch in enumerate(rij):
            if ch == "#":
                _px(a, x0 + c, y0 + r, schaduw if c >= 7 or r >= 10 else wit)
    for (c, r) in ((2, 3), (3, 3), (2, 4), (6, 2), (6, 6), (7, 6), (6, 7), (7, 7), (3, 8), (4, 8), (3, 9)):
        _px(a, x0 + c, y0 + r, vlek)


def eislot():
    """The egg lock: a steel plate with rivets and an egg-shaped keyhole."""
    a = _vlak((120, 130, 146), 4, 22106)
    a[0, :, :3] = (190, 198, 210)
    a[:, 0, :3] = (190, 198, 210)
    a[15, :, :3] = (54, 60, 74)
    a[:, 15, :3] = (54, 60, 74)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        _px(a, x, y, (60, 66, 80))
        _px(a, x, y - 1, (214, 220, 230))
    for r, rij in enumerate(EI):                                # a dark rim round the egg
        for c, ch in enumerate(rij):
            if ch == "#":
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    _px(a, 3 + c + dx, 2 + r + dy, (40, 44, 56))
    _ei(a, 3, 2)
    for x, y in ((7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (8, 10), (7, 10)):   # the keyhole
        _px(a, x, y, (30, 30, 40))
    _px(a, 6, 6, (30, 30, 40))
    _px(a, 9, 6, (30, 30, 40))
    return Image.fromarray(a)


def nest_boven():
    """The nest from above: a ring of straw round a warm hollow."""
    a = _vlak((222, 180, 70), 12, 22107)
    rng = np.random.default_rng(22108)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 4.6:
                a[y, x, :3] = (250, 150, 60) if d < 2.4 else (236, 120, 44) if d < 3.6 else (150, 92, 30)
            elif rng.random() < 0.3:
                a[y, x, :3] = (250, 224, 130) if rng.random() < 0.5 else (170, 124, 40)
    return Image.fromarray(a)


def nest_zij():
    """The nest from the side: a dark little stove with a glowing grate, straw hanging over its edge."""
    a = _vlak((58, 56, 66), 4, 22109)
    a[5, :, :3] = (110, 108, 122)
    a[15, :, :3] = (26, 24, 30)
    a[:, 0, :3] = (96, 94, 108)
    a[:, 15, :3] = (30, 28, 36)
    for x in range(3, 13):                                       # the grate: fire behind bars
        for y in range(8, 13):
            a[y, x, :3] = (255, 190, 60) if (x + y) % 3 == 0 else (240, 110, 30) if y < 11 else (200, 60, 20)
    for x in (5, 8, 11):
        for y in range(8, 13):
            _px(a, x - 1, y, (34, 32, 40))
    for x in range(2, 14):
        _px(a, x, 7, (34, 32, 40))
        _px(a, x, 13, (34, 32, 40))
    rng = np.random.default_rng(22110)
    for x in range(16):                                          # straw
        hoog = 4 + int(rng.integers(0, 2))
        for y in range(hoog):
            a[y, x, :3] = (250, 224, 130) if rng.random() < 0.35 else (222, 180, 70) if rng.random() < 0.7 else (170, 124, 40)
    return Image.fromarray(a)


def broedplek():
    """The hatching spot's picture: the egg with a crack."""
    a = np.zeros((16, 16, 4), np.uint8)
    _ei(a, 3, 2)
    for x, y in ((3, 7), (4, 6), (5, 7), (6, 6), (7, 7), (8, 6), (9, 7), (10, 6), (11, 7), (12, 6)):
        _px(a, x, y, (60, 50, 40))
    return Image.fromarray(a)


def save(h):
    h.save(keldergrond(), "block", "guhriow2_keldergrond.png")
    h.save(keldersteen(), "block", "guhriow2_keldersteen.png")
    h.save(warppijp(), "block", "guhriow2_warppijp.png")
    h.save(warppijp_boven(), "block", "guhriow2_warppijp_boven.png")
    h.save(warpbuis(), "block", "guhriow2_warpbuis.png")
    h.save(eislot(), "block", "guhriow2_eislot.png")
    h.save(nest_boven(), "block", "guhriow2_nest_boven.png")
    h.save(nest_zij(), "block", "guhriow2_nest_zij.png")
    h.save(broedplek(), "item", "guhriow2_broedplek.png")
