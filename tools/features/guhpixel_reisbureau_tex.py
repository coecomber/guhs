"""
Reisbureau "De Vadsvakantie": the drawn textures. The two souvenir pictures ("Huize Lingsesdijk 86": a Dutch dike house
with a guh asleep in the garden; "Thuis is het ook vads": a guh on the couch), the "Balkonië" sign, the glass of the snow
globes and the bottle, the animated snow and the turning golden globe, the counter's logo, and the item icons (the
Reisstempel, the twenty-one ansichtkaarten: biomes3 added four).
"""
import json
import math
import os

import numpy as np
from PIL import Image

N = "reisbureau"

FONT = {   # 3 x 5
    "A": ("010", "101", "111", "101", "101"), "B": ("110", "101", "110", "101", "110"), "D": ("110", "101", "101", "101", "110"),
    "E": ("111", "100", "110", "100", "111"), "I": ("111", "010", "010", "010", "111"), "K": ("101", "101", "110", "101", "101"),
    "L": ("100", "100", "100", "100", "111"), "N": ("101", "111", "111", "111", "101"), "O": ("111", "101", "101", "101", "111"),
    "R": ("110", "101", "110", "101", "101"), "S": ("011", "100", "010", "001", "110"), "V": ("101", "101", "101", "101", "010"),
    "T": ("111", "010", "010", "010", "010"), "8": ("111", "101", "111", "101", "111"), "6": ("111", "100", "111", "101", "111"),
}


def _nieuw(w, hgt, kleur=(0, 0, 0, 0)):
    a = np.zeros((hgt, w, 4), np.uint8)
    a[..., :] = kleur
    return a


def _rect(a, x0, y0, x1, y1, kleur):
    kleur = tuple(kleur) + ((255,) if len(kleur) == 3 else ())
    a[max(0, y0):max(0, y1), max(0, x0):max(0, x1)] = kleur


def _px(a, x, y, kleur):
    if 0 <= y < a.shape[0] and 0 <= x < a.shape[1]:
        a[y, x] = tuple(kleur) + ((255,) if len(kleur) == 3 else ())


def _tekst(a, x, y, tekst, kleur):
    for ch in tekst:
        if ch == " ":
            x += 2
            continue
        for dy, rij in enumerate(FONT[ch]):
            for dx, bit in enumerate(rij):
                if bit == "1":
                    _px(a, x + dx, y + dy, kleur)
        x += 4
    return x


def _guh_slaapt(a, x, y, roze=(255, 150, 196), donker=(222, 98, 150)):
    """A little pink guh asleep (about 9 x 5), lying with its head to the left; x, y = its left bottom."""
    for dy, (x0, x1) in enumerate(((1, 9), (0, 10), (0, 10), (1, 9), (2, 8))):
        _rect(a, x + x0, y - dy, x + x1, y - dy + 1, roze)
    _px(a, x + 1, y - 5, roze)
    _px(a, x + 3, y - 5, roze)
    _px(a, x + 1, y - 4, donker)
    _px(a, x + 3, y - 4, donker)
    _px(a, x + 1, y - 2, (58, 28, 60))
    _px(a, x + 2, y - 2, (58, 28, 60))
    _px(a, x + 4, y - 2, (58, 28, 60))
    _px(a, x + 5, y - 2, (58, 28, 60))
    _px(a, x + 3, y - 1, donker)
    _px(a, x + 9, y - 3, donker)


def _zzz(a, x, y, kleur=(255, 255, 255)):
    for (dx, dy) in ((0, 0), (1, 0), (1, 1), (0, 2), (1, 2), (3, -3), (4, -3), (4, -2), (3, -1), (4, -1)):
        _px(a, x + dx, y + dy, kleur)


def beeld_lingsesdijk():
    """Huize Lingsesdijk 86: a house on the dike under a big Dutch sky, a guh asleep in the garden."""
    a = _nieuw(32, 32)
    for y in range(32):                                       # the sky
        t = y / 20
        a[y, :, :3] = (int(120 + 70 * min(1, t)), int(180 + 50 * min(1, t)), 250)
        a[y, :, 3] = 255
    for (cx, cy, w) in ((5, 4, 7), (22, 6, 8), (13, 2, 5)):   # clouds
        _rect(a, cx, cy, cx + w, cy + 2, (255, 255, 255))
        _rect(a, cx + 1, cy - 1, cx + w - 2, cy, (255, 255, 255))
    _rect(a, 27, 2, 30, 5, (255, 236, 130))                   # the sun
    # the dike: a green bank rising to the left, the road on top
    for x in range(32):
        top = 17 + int(round(3 * (x / 31)))
        _rect(a, x, top, x + 1, 32, (96, 170, 84))
        _px(a, x, top, (128, 196, 104))
    _rect(a, 0, 16, 14, 18, (150, 146, 150))
    _rect(a, 0, 16, 14, 17, (176, 172, 176))
    # the house: white walls, an orange roof, a green door, the number 86
    _rect(a, 5, 9, 17, 17, (250, 246, 236))
    for i in range(6):
        _rect(a, 4 + i, 8 - i, 18 - i, 9 - i, (226, 110, 60))
    _rect(a, 13, 2, 15, 6, (160, 80, 60))                     # chimney
    _px(a, 14, 1, (236, 236, 240))
    _px(a, 15, 0, (236, 236, 240))
    _rect(a, 7, 12, 10, 17, (52, 110, 70))
    _px(a, 9, 14, (250, 208, 70))
    _rect(a, 12, 11, 16, 14, (130, 190, 240))
    _rect(a, 12, 12, 16, 13, (250, 246, 236))
    _px(a, 14, 11, (250, 246, 236))
    _px(a, 14, 13, (250, 246, 236))
    _rect(a, 10, 5, 12, 7, (130, 190, 240))
    # the garden: a fence, flowers, the sign "86", the sleeping guh
    for x in range(15, 32, 2):
        _rect(a, x, 22, x + 1, 25, (250, 246, 236))
    _rect(a, 15, 23, 32, 24, (250, 246, 236))
    _rect(a, 0, 26, 32, 32, (110, 184, 92))
    for (x, y, k) in ((3, 29, (255, 226, 90)), (7, 27, (255, 255, 255)), (12, 30, (255, 150, 196)), (28, 30, (255, 226, 90)), (30, 27, (255, 255, 255))):
        _px(a, x, y, k)
    _rect(a, 1, 20, 10, 27, (250, 246, 236))                  # the sign
    _rect(a, 5, 27, 6, 30, (150, 104, 62))
    _tekst(a, 2, 21, "86", (58, 28, 60))
    _guh_slaapt(a, 16, 29)
    _zzz(a, 24, 23)
    return Image.fromarray(a)


def beeld_balkonie():
    """Thuis is het ook vads: a guh on the couch at home, 24 hours long."""
    a = _nieuw(32, 32)
    _rect(a, 0, 0, 32, 22, (250, 222, 190))                   # the wall
    for x in range(0, 32, 4):
        _rect(a, x, 0, x + 1, 22, (244, 210, 174))
    _rect(a, 0, 22, 32, 32, (176, 124, 84))                   # the floor
    _rect(a, 0, 22, 32, 23, (130, 84, 52))
    _rect(a, 2, 3, 12, 12, (150, 104, 62))                    # a window with the sun
    _rect(a, 3, 4, 11, 11, (150, 210, 250))
    _rect(a, 7, 4, 8, 11, (150, 104, 62))
    _rect(a, 3, 7, 11, 8, (150, 104, 62))
    _rect(a, 8, 5, 10, 7, (255, 236, 130))
    _rect(a, 22, 2, 29, 9, (250, 208, 70))                    # a little frame: the same picture again
    _rect(a, 23, 3, 28, 8, (130, 190, 240))
    _rect(a, 23, 6, 28, 8, (96, 170, 84))
    _rect(a, 25, 4, 27, 6, (250, 246, 236))
    _rect(a, 3, 15, 29, 24, (92, 130, 214))                   # the couch
    _rect(a, 3, 12, 29, 17, (70, 104, 190))
    _rect(a, 1, 14, 5, 25, (70, 104, 190))
    _rect(a, 27, 14, 31, 25, (70, 104, 190))
    _rect(a, 3, 25, 5, 27, (104, 68, 40))
    _rect(a, 27, 25, 29, 27, (104, 68, 40))
    _rect(a, 6, 16, 12, 20, (255, 198, 224))                  # a cushion
    _guh_slaapt(a, 11, 21)
    _zzz(a, 20, 12, (255, 255, 255))
    _rect(a, 22, 20, 26, 22, (244, 228, 196))                 # a plate with a kaasknabbel
    _rect(a, 23, 19, 25, 20, (250, 206, 84))
    return Image.fromarray(a)


def bordje_balkonie():
    """The sign "BALKONIË" (the top half of the texture is the sign's face)."""
    a = _nieuw(32, 32, (255, 0, 255, 255))
    _rect(a, 0, 0, 32, 16, (246, 236, 214))
    for x in range(32):
        if x % 7 == 3:
            _rect(a, x, 0, x + 1, 16, (236, 222, 196))
    _tekst(a, 0, 6, "BALKONI", (40, 110, 150))
    x = 28
    for dy, rij in enumerate(FONT["E"]):
        for dx, bit in enumerate(rij):
            if bit == "1":
                _px(a, x + dx, 6 + dy, (40, 110, 150))
    _px(a, 28, 4, (40, 110, 150))
    _px(a, 30, 4, (40, 110, 150))
    _rect(a, 1, 1, 4, 4, (255, 210, 80))                      # a little sun
    _px(a, 5, 2, (255, 210, 80))
    _px(a, 2, 5, (255, 210, 80))
    for x in range(0, 32, 4):                                 # waves at the bottom
        _rect(a, x, 13, x + 2, 14, (92, 170, 226))
        _rect(a, x + 2, 14, x + 4, 15, (92, 170, 226))
    return Image.fromarray(a)


def logo():
    """The counter's front: a suitcase under a sun on pink (the top 24 rows of the texture are used)."""
    a = _nieuw(32, 32, (255, 0, 255, 255))
    _rect(a, 0, 0, 32, 24, (255, 222, 236))
    for i in range(0, 32, 4):
        _rect(a, i, 0, i + 2, 24, (255, 232, 242))
    _rect(a, 23, 2, 29, 8, (255, 214, 90))                    # sun
    for (x, y) in ((21, 5), (30, 5), (26, 0), (26, 9), (22, 1), (30, 1), (22, 9), (30, 9)):
        _px(a, x, y, (255, 190, 70))
    _rect(a, 5, 10, 21, 21, (150, 96, 58))                    # the suitcase
    _rect(a, 5, 10, 21, 11, (176, 120, 74))
    _rect(a, 8, 10, 10, 21, (104, 68, 40))
    _rect(a, 16, 10, 18, 21, (104, 68, 40))
    _rect(a, 10, 7, 16, 8, (104, 68, 40))
    _rect(a, 10, 8, 11, 10, (104, 68, 40))
    _rect(a, 15, 8, 16, 10, (104, 68, 40))
    _rect(a, 11, 13, 15, 17, (255, 150, 196))                 # a guh sticker
    _px(a, 11, 12, (255, 150, 196))
    _px(a, 14, 12, (255, 150, 196))
    _px(a, 12, 14, (58, 28, 60))
    _px(a, 14, 14, (58, 28, 60))
    _rect(a, 19, 17, 21, 19, (255, 226, 90))
    _rect(a, 5, 14, 7, 16, (92, 170, 226))
    _tekst(a, 1, 1, "REIS", (212, 78, 140))
    return Image.fromarray(a)


def glas():
    a = _nieuw(16, 16, (196, 230, 250, 46))
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            a[y, x] = (214, 240, 255, 120)
    for i in range(5):
        a[2 + i, 4 - i if 4 - i >= 1 else 1] = (255, 255, 255, 170)
        a[3 + i, 5 - i if 5 - i >= 1 else 1] = (255, 255, 255, 110)
    a[11, 12] = a[12, 11] = (255, 255, 255, 120)
    return Image.fromarray(a)


def sneeuw(frames=16):
    """The glass with falling snow: 16 frames under each other (the .mcmeta makes it an animation)."""
    basis = np.asarray(glas()).copy()
    rng = np.random.default_rng(8611)
    vlokken = [(int(rng.integers(1, 15)), int(rng.integers(0, 16)), int(rng.integers(1, 3))) for _ in range(14)]
    strip = np.zeros((16 * frames, 16, 4), np.uint8)
    for f in range(frames):
        a = basis.copy()
        for i, (x, y0, snelheid) in enumerate(vlokken):
            y = (y0 + f * snelheid) % 16
            xx = x + (1 if (f + i) % 8 < 2 and x < 14 else 0)
            if 1 <= y <= 14:
                a[y, xx] = (255, 255, 255, 235)
        strip[f * 16:(f + 1) * 16] = a
    return Image.fromarray(strip)


def draai(frames=16):
    """The golden globe's side: darker golden continents that scroll sideways (16 frames = one turn)."""
    rng = np.random.default_rng(8612)
    land = np.zeros((16, 32), bool)
    for (cx, cy, r) in ((5, 6, 3.4), (9, 10, 2.6), (19, 5, 3.0), (24, 9, 3.6), (28, 4, 1.8), (14, 13, 1.6)):
        for y in range(16):
            for x in range(32):
                dx = min(abs(x - cx), 32 - abs(x - cx))
                if math.hypot(dx, (y - cy) * 1.2) <= r + rng.normal(0, 0.25):
                    land[y, x] = True
    strip = np.zeros((16 * frames, 16, 4), np.uint8)
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                is_land = land[y, (x + f * 2) % 32]
                rand = 1.0 - 0.22 * (abs(x - 7.5) / 7.5) ** 2 - 0.12 * (abs(y - 7.5) / 7.5) ** 2
                basis = (206, 150, 36) if is_land else (250, 208, 70)
                if not is_land and (x + y + f) % 9 == 0:
                    basis = (255, 238, 150)
                strip[f * 16 + y, x] = tuple(int(c * rand) for c in basis) + (255,)
    return Image.fromarray(strip)


def stempel():
    a = _nieuw(16, 16)
    _rect(a, 6, 1, 10, 3, (176, 120, 74))
    _rect(a, 5, 2, 11, 6, (150, 96, 58))
    _rect(a, 7, 6, 9, 9, (130, 84, 52))
    _rect(a, 4, 9, 12, 11, (104, 68, 40))
    _rect(a, 3, 11, 13, 13, (70, 60, 70))
    _rect(a, 3, 13, 13, 14, (255, 110, 170))
    for (x, y) in ((6, 2), (7, 2), (5, 3)):
        _px(a, x, y, (206, 164, 112))
    for (x, y) in ((1, 15), (2, 15), (4, 15), (7, 15), (8, 15), (11, 15), (13, 15), (14, 15)):
        _px(a, x, y, (255, 150, 196, 200))
    return Image.fromarray(a)


# the little picture on each ansichtkaart: (sky, ground, accent)
KAART_KLEUREN = {
    "lingsesdijk": ((150, 206, 250), (96, 170, 84), (226, 110, 60)), "kaasmarkt": ((250, 232, 170), (176, 124, 84), (250, 206, 84)),
    "vadswoud": ((150, 196, 150), (60, 120, 70), (220, 255, 140)), "knuffeldal": ((255, 214, 232), (134, 196, 104), (246, 244, 240)),
    "guhwaii": ((120, 210, 250), (240, 214, 150), (255, 150, 196)), "barbecuether": ((250, 170, 110), (160, 70, 50), (255, 236, 130)),
    "efteguh": ((170, 190, 250), (96, 150, 84), (214, 62, 58)), "guhkenhof": ((170, 220, 250), (255, 150, 196), (255, 226, 90)),
    "nomguh": ((190, 220, 250), (246, 248, 252), (70, 110, 210)), "guhrijs": ((250, 200, 210), (150, 150, 170), (250, 206, 84)),
    "camping": ((60, 60, 120), (60, 110, 70), (244, 140, 50)), "guhnetie": ((250, 214, 160), (92, 170, 226), (40, 36, 46)),
    "kaasmaan": ((30, 26, 60), (250, 206, 84), (246, 244, 240)), "wereldreis": ((130, 190, 240), (70, 140, 74), (250, 208, 70)),
    "cruise": ((150, 206, 250), (70, 110, 210), (246, 244, 240)), "balkonie": ((250, 222, 190), (92, 130, 214), (255, 150, 196)),
    "om_de_hoek": ((200, 226, 250), (150, 150, 158), (128, 124, 130)),
    # biomes3
    "bloesemmeertje": ((255, 214, 232), (95, 211, 214), (255, 150, 196)), "klaterdal": ((190, 230, 240), (134, 196, 104), (212, 58, 60)),
    "wolkenweide": ((150, 196, 250), (255, 232, 242), (246, 244, 240)), "japan": ((255, 214, 232), (134, 196, 104), (150, 170, 220)),
}


def kaart(bid):
    lucht, grond, accent = KAART_KLEUREN[bid]
    a = _nieuw(16, 16)
    _rect(a, 1, 3, 15, 13, (250, 246, 236))
    _rect(a, 1, 12, 15, 13, (224, 216, 200))
    _rect(a, 2, 4, 9, 11, lucht)                              # the picture
    _rect(a, 2, 8, 9, 11, grond)
    _rect(a, 4, 6, 7, 9, accent)
    _px(a, 5, 5, accent)
    _px(a, 7, 9, (255, 150, 196))                             # a tiny guh in the picture
    _px(a, 8, 9, (255, 150, 196))
    if bid == "japan":                                        # biomes3: the three of them: the guh, Evivads and Nielsvads at the mountain
        _rect(a, 2, 4, 9, 11, lucht)
        _rect(a, 2, 9, 9, 11, grond)
        _rect(a, 6, 6, 9, 9, accent)                          # the mountain, with snow on top
        _px(a, 6, 6, lucht)
        _px(a, 8, 6, lucht)
        _px(a, 7, 5, (246, 248, 252))
        _px(a, 7, 6, (246, 248, 252))
        for (x, top, haar, lijf) in ((3, 7, (250, 226, 120), (255, 150, 196)), (5, 6, (170, 118, 74), (70, 110, 210))):
            _px(a, x, top, haar)                              # Evivads (blond, a little shorter) and Nielsvads (light brown)
            _px(a, x, top + 1, (250, 214, 190))
            for y in range(top + 2, 10):
                _px(a, x, y, lijf)
        _px(a, 7, 9, (255, 150, 196))                         # the guh, in front
        _px(a, 8, 9, (255, 150, 196))
        _px(a, 7, 8, (255, 150, 196))
    _rect(a, 12, 4, 14, 6, (214, 62, 58))                     # the stamp
    _px(a, 12, 4, (255, 214, 90))
    for y in (7, 9, 11):                                      # the address lines
        _rect(a, 10, y, 14, y + 1, (150, 150, 158))
    _px(a, 0, 3, (0, 0, 0, 0))
    return Image.fromarray(a)


def _anim(h, naam, frametime):
    with open(os.path.join(h.TEX, "block", f"{naam}.png.mcmeta"), "w", encoding="utf-8") as f:
        json.dump({"animation": {"frametime": frametime}}, f, indent=2)
        f.write("\n")


def build(h):
    h.save(beeld_lingsesdijk(), "block", f"{N}_beeld_lingsesdijk.png")
    h.save(beeld_balkonie(), "block", f"{N}_beeld_balkonie.png")
    h.save(bordje_balkonie(), "block", f"{N}_bordje_balkonie.png")
    h.save(logo(), "block", f"{N}_logo.png")
    h.save(glas(), "block", f"{N}_glas.png")
    h.save(sneeuw(), "block", f"{N}_sneeuw.png")
    _anim(h, f"{N}_sneeuw", 3)
    h.save(draai(), "block", f"{N}_draai.png")
    _anim(h, f"{N}_draai", 3)
    h.save(stempel(), "item", f"{N}_stempel.png")
    h.item_model(f"{N}_stempel")
    for bid in KAART_KLEUREN:
        h.save(kaart(bid), "item", f"{N}_kaart_{bid}.png")
        h.item_model(f"{N}_kaart_{bid}")
