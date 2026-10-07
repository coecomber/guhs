"""
biomes3 wereld: draws the dump of the dev command `/guhs bio wereld kaart <x> <z> <chunks>` (a file bio_kaart_<x>_<z>.txt
in the server directory) as two pictures next to it: the top blocks from above, shaded by height, and the heights alone.
Not in FEATURES; a tool for looking at generated terrain without a client:

    python tools/features/bio_wereld_kaart.py <path to bio_kaart_x_z.txt> [more files]
"""
import sys

from PIL import Image

KLEUR = {"water": (70, 170, 210), "sand": (228, 214, 160), "pink_wool": (242, 160, 196), "knuffelsteen": (236, 232, 228),
         "gladde_knuffelsteen": (240, 238, 234), "calcite": (225, 225, 220), "white_wool": (250, 250, 250), "wolkenblok_wit": (250, 250, 250),
         "wolkenblok_roze": (250, 205, 225), "pink_terracotta": (170, 90, 90), "wolkenlift": (150, 200, 250), "wolkenstroom": (180, 220, 255),
         "air": (0, 0, 0), "guhbloesem_leaves": (236, 120, 180), "guhbloesem_log": (110, 70, 80), "glowstone": (255, 230, 80),
         "red_wool": (220, 30, 30), "grass_block": (120, 190, 90), "snow_block": (245, 250, 255), "gatenkaas": (240, 200, 90)}


def teken(pad):
    rijen = [r.split() for r in open(pad, encoding="utf-8") if not r.startswith("#")]
    xs = [int(r[0]) for r in rijen]
    zs = [int(r[1]) for r in rijen]
    x0, z0, w, h = min(xs), min(zs), max(xs) - min(xs) + 1, max(zs) - min(zs) + 1
    boven, hoogte = Image.new("RGB", (w, h)), Image.new("RGB", (w, h))
    tops = {}
    for r in rijen:
        tops[(int(r[0]), int(r[1]))] = int(r[2])
    for r in rijen:
        x, z, top, blok, vloer = int(r[0]), int(r[1]), int(r[2]), r[3], int(r[4])
        k = KLEUR.get(blok)
        if k is None:
            k = (255, 0, 255) if "concrete" in blok else (200, 150, 170) if "bloem" in blok or "gras" in blok else (160, 160, 160)
        if blok == "water":
            d = min(8, top - vloer)
            k = tuple(int(c * (1 - 0.07 * d)) for c in k)
        else:
            # shade: light from the north-west, and brighter with height
            licht = 1.0 + 0.012 * (top - 60) + 0.10 * max(-2, min(2, top - tops.get((x - 1, z - 1), top)))
            k = tuple(max(0, min(255, int(c * licht))) for c in k)
        boven.putpixel((x - x0, z - z0), k)
        g = max(0, min(255, (top - 40) * 2))
        hoogte.putpixel((x - x0, z - z0), (g, g, 255 - g // 2) if blok == "water" else (g, g, g))
    schaal = max(1, 900 // max(w, h))
    for naam, beeld in (("boven", boven), ("hoogte", hoogte)):
        uit = pad[:-4] + f"_{naam}.png"
        beeld.resize((w * schaal, h * schaal), Image.NEAREST).save(uit)
        print(uit)


if __name__ == "__main__":
    for pad in sys.argv[1:]:
        teken(pad)
