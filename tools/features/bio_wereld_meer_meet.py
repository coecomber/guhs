"""
biomes3 wereld, the Bloesemmeertje: tools for LOOKING at and MEASURING the lake without a client. Not in FEATURES.

  python tools/features/bio_wereld_meer_meet.py kaart <bio_meer_*.txt> [...]
      draws the model maps the game test bioWereldMeerVorm writes with GUHS_BIO_MEER_KAART set (run/bio_meer_<seed>_<n>.txt):
      <name>.png from above (bed colour by depth, land, sand, stone, crowns, petal streaks, structure spots) and
      <name>_diepte.png (depth only).
  python tools/features/bio_wereld_meer_meet.py regio <world dir> <x0> <z0> <x1> <z1> <out prefix> [alles | x0,z0,x1,z1]
      measures and draws the generated BLOCKS of that box from the dev server's region files: see bio_wereld_meer_regio.py.
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

BED = {1: (224, 212, 170), 2: (226, 226, 220), 3: (150, 204, 226), 4: (74, 176, 214), 5: (40, 150, 170), 6: (24, 132, 142), 7: (21, 119, 136),
       8: (18, 104, 120), 9: (200, 0, 0)}
WATER = (79, 214, 210)


def onder_water(bed, diepte):
    """A bed colour seen through d blocks of the lake's water (a rough picture of the game's look)."""
    t = min(0.75, 0.30 + 0.06 * diepte)
    return tuple(int(b * (1 - t) + w * t * 0.9) for b, w in zip(bed, WATER))


def kaart(pad):
    regels = open(pad, encoding="utf-8").read().split("\n")
    _, x0, z0, w, h = regels[0].split()
    x0, z0, w, h = int(x0), int(z0), int(w), int(h)
    S = 2
    boven, diep = Image.new("RGB", (w, h), (40, 30, 40)), Image.new("RGB", (w, h), (0, 0, 0))
    for z in range(h):
        rij = regels[1 + z]
        for x, c in enumerate(rij):
            if c == " ":
                k, d = (60, 50, 60), (30, 30, 30)
            elif c in "abcd":
                v = "abcd".index(c)
                k, d = (240 - v * 9, 170 - v * 14, 200 - v * 8), (60, 60, 60)
            elif c == "~":
                k, d = (120, 220, 225), (60, 60, 90)
            elif c.isdigit():
                n = int(c)
                k = onder_water(BED[n], n)
                d = (255 - n * 28, 255 - n * 22, 255 - n * 8) if n <= 8 else (255, 0, 0)
            elif c == "o":
                k, d = (236, 232, 228), (120, 120, 120)
            elif c in "sz":
                k, d = (232, 218, 168), (90, 80, 50)
            elif c == "E":
                k, d = (246, 176, 208), (70, 70, 70)
            elif c == "H":
                k, d = (252, 196, 222), (110, 110, 110)
            else:   # 'e' island beach height without sand, '.' shore land
                k, d = (242, 164, 198), (70, 70, 70)
            boven.putpixel((x, z), k)
            diep.putpixel((x, z), d)
    boven = boven.resize((w * S, h * S), Image.NEAREST).convert("RGBA")
    laag = Image.new("RGBA", boven.size, (0, 0, 0, 0))
    dr = ImageDraw.Draw(laag)
    for r in regels[1 + h:]:
        p = r.split()
        if not p:
            continue
        if p[0] == "blaadje":
            x, z = (int(p[1]) - x0) * S, (int(p[2]) - z0) * S
            dr.rectangle([x, z, x + S - 1, z + S - 1], fill=(255, 225, 238, 230))
    for r in regels[1 + h:]:
        p = r.split()
        if p and p[0] == "boom":
            kx, kz, kr = (float(p[7]) - x0) * S, (float(p[8]) - z0) * S, float(p[9]) * S
            kleur = (226, 96, 160, 150) if p[3] != "3" else (200, 60, 140, 170)
            dr.ellipse([kx - kr, kz - kr, kx + kr, kz + kr], fill=kleur)
            x, z = (int(p[1]) - x0) * S, (int(p[2]) - z0) * S
            dr.rectangle([x, z, x + S - 1, z + S - 1], fill=(90, 50, 60, 255))
        elif p and p[0] == "plek":
            x, z = (int(p[2]) - x0) * S, (int(p[3]) - z0) * S
            kleur = (255, 240, 60, 255) if p[1] == "meer_eiland" else (60, 255, 120, 255)
            dr.rectangle([x - 2, z - 2, x + 3, z + 3], outline=kleur)
    boven = Image.alpha_composite(boven, laag).convert("RGB")
    uit = pad[:-4]
    boven.save(uit + ".png")
    diep.resize((w * S, h * S), Image.NEAREST).save(uit + "_diepte.png")
    print(uit + ".png", boven.size)


if __name__ == "__main__":
    if len(sys.argv) >= 3 and sys.argv[1] == "kaart":
        for p in sys.argv[2:]:
            kaart(p)
    elif len(sys.argv) >= 8 and sys.argv[1] == "regio":
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        import bio_wereld_meer_regio
        bio_wereld_meer_regio.regio(sys.argv[2], int(sys.argv[3]), int(sys.argv[4]), int(sys.argv[5]), int(sys.argv[6]), sys.argv[7],
                                    sys.argv[8] if len(sys.argv) > 8 else None)
    else:
        print(__doc__)
