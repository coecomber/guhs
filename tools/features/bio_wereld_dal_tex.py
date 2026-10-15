"""
biomes3 wereld, the Klaterdal: the textures of the biome's own blocks (called from bio_wereld_dal.build), drawn pixel by
pixel with fixed seeds (bio_lib.rng):

  klaterdal_mos         the soft ground cover: a pale mint moss with lighter and darker tufts and a few pink flecks. The
                        bright vanilla moss green clashed with the pink ground in the sketches; this green is the pale
                        jade of the guh-bamboe, pushed lighter, so it sits IN the pastel palette (also the carpet's top)
  klaterdal_riet        the reed of the river banks: thin cream-green stalks with soft pink plumes (a cross model)
  klaterdal_bonsaiblad  the leaf pads of the crooked little trees: pale green with tiny blossoms
"""
import os

import numpy as np
from PIL import Image

from features import bio_lib as lib

MOS = {"basis": (188, 220, 194), "licht": (208, 233, 210), "donker": (166, 204, 176), "diep": (150, 190, 164), "vlek": (246, 200, 218)}
RIET = {"stengel": (196, 214, 170), "licht": (220, 232, 196), "donker": (164, 190, 146), "pluim": (244, 170, 200), "pluimlicht": (252, 208, 224),
        "pluimdonker": (222, 136, 174)}
BLAD = {"basis": (170, 212, 172), "licht": (196, 228, 190), "donker": (140, 192, 150), "diep": (118, 172, 134), "bloem": (253, 232, 240),
        "hart": (246, 178, 206)}


def _golf(rng, cellen, n=16):
    """Smooth tileable noise 0..1 (value noise on a wrapped grid)."""
    g = rng.random((cellen, cellen))
    uit = np.zeros((n, n))
    for y in range(n):
        for x in range(n):
            fx, fy = x * cellen / n, y * cellen / n
            x0, y0 = int(fx) % cellen, int(fy) % cellen
            x1, y1 = (x0 + 1) % cellen, (y0 + 1) % cellen
            tx, ty = fx - int(fx), fy - int(fy)
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            uit[y, x] = (g[y0, x0] * (1 - tx) + g[y0, x1] * tx) * (1 - ty) + (g[y1, x0] * (1 - tx) + g[y1, x1] * tx) * ty
    return uit


def mos():
    rng = lib.rng("klaterdal_mos")
    groot, klein = _golf(rng, 3), _golf(rng, 6)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = 0.6 * groot[y, x] + 0.4 * klein[y, x] + rng.normal(0, 0.05)
            k = MOS["diep"] if v < 0.30 else MOS["donker"] if v < 0.44 else MOS["basis"] if v < 0.62 else MOS["licht"]
            img.putpixel((x, y), k + (255,))
    # a few pink flecks (fallen petals pressed into the moss), never two close together
    gezet = []
    while len(gezet) < 4:
        x, y = int(rng.integers(0, 16)), int(rng.integers(0, 16))
        if all(abs(x - a) + abs(y - b) > 4 for a, b in gezet):
            gezet.append((x, y))
            img.putpixel((x, y), MOS["vlek"] + (255,))
    return img


def riet():
    rng = lib.rng("klaterdal_riet")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # (x of the foot, height, lean, with a plume)
    for x, hoog, lean, pluim in ((3, 13, -1, True), (5, 9, 0, False), (7, 15, 0, True), (9, 11, 1, False), (11, 14, 1, True), (13, 8, 0, False)):
        for i in range(hoog):
            y = 15 - i
            px = x + (lean if i > hoog * 0.55 else 0)
            k = RIET["donker"] if i < 2 else RIET["licht"] if (i + x) % 5 == 0 else RIET["stengel"]
            if 0 <= px < 16 and (not pluim or i < hoog - 3):
                img.putpixel((px, y), k + (255,))
        if pluim:
            top = 15 - hoog + 1
            px = x + lean
            for dy, breed in ((0, 0), (1, 1), (2, 1), (3, 0)):
                for dx in range(-breed, breed + 1):
                    qx, qy = px + dx, top + dy
                    if 0 <= qx < 16 and 0 <= qy < 16:
                        k = RIET["pluimlicht"] if dx <= 0 and dy <= 1 else RIET["pluimdonker"] if dy == 3 else RIET["pluim"]
                        img.putpixel((qx, qy), k + (255,))
        else:
            # a leaf blade to the side
            y = 15 - hoog + 2
            zij = 1 if rng.random() < 0.5 else -1
            for d in (1, 2):
                if 0 <= x + zij * d < 16:
                    img.putpixel((x + zij * d, y - d + 1), RIET["licht"] + (255,))
    return img


def bonsaiblad():
    rng = lib.rng("klaterdal_bonsaiblad")
    veld = _golf(rng, 5)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = veld[y, x] + rng.normal(0, 0.07)
            if v < 0.26:
                continue                      # a gap between the leaves
            k = BLAD["diep"] if v < 0.36 else BLAD["donker"] if v < 0.48 else BLAD["basis"] if v < 0.66 else BLAD["licht"]
            img.putpixel((x, y), k + (255,))
    for (x, y) in ((3, 4), (11, 2), (7, 9), (13, 12), (2, 13)):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
            k = BLAD["hart"] if (dx, dy) == (0, 0) else BLAD["bloem"]
            img.putpixel(((x + dx) % 16, (y + dy) % 16), k + (255,))
    return img


def build(h):
    d = os.path.join(h.A, "textures", "block")
    os.makedirs(d, exist_ok=True)
    for naam, maak in (("klaterdal_mos", mos), ("klaterdal_riet", riet), ("klaterdal_bonsaiblad", bonsaiblad)):
        maak().save(os.path.join(d, naam + ".png"))
