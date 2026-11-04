"""
Skill icons for the Vadsrijk HUD and the skill-tree screen (32x32 pixel art, one per skill): the 4 skills (R/V/G/Z) of each of
the 3 classes plus the Knuffelsprong (C). Every icon: a rounded badge in the class colour (dark rim, light top edge) with a
symbol on it; the symbol gets a 1-px dark outline so it reads on any background. Drawn without anti-aliasing (crisp pixels).

Run from the project root:  python tools/vadsrijk/maak_skill_iconen.py
Writes src/main/resources/assets/guhs/textures/gui/vadsrijk/skill/<skill id>.png
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw

OUT = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "gui", "vadsrijk", "skill")
N = 32
RAND = (42, 16, 34, 255)

# badge colours per class: (dark, mid, light)
RIDDER = ((120, 30, 70), (214, 72, 132), (255, 150, 196))
JAGER = ((34, 92, 52), (74, 160, 92), (150, 220, 140))
MAGIER = ((70, 36, 120), (132, 82, 200), (196, 160, 255))
SPRONG = ((30, 78, 130), (70, 150, 220), (150, 210, 255))

WIT = (255, 255, 255, 255)
ROZE = (255, 122, 200, 255)
LICHTROZE = (255, 194, 232, 255)
HART = (255, 70, 130, 255)
GOUD = (255, 210, 74, 255)
LICHTGOUD = (255, 240, 160, 255)
HOUT = (150, 96, 52, 255)
GRIJS = (200, 200, 214, 255)
GROEN = (120, 230, 120, 255)
BLAUW = (140, 210, 255, 255)
ORANJE = (255, 150, 60, 255)


def badge(kleur):
    """The round-cornered tile with a vertical gradient."""
    donker, mid, licht = kleur
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    px = img.load()
    for y in range(N):
        for x in range(N):
            # rounded square with corner radius 6
            cx = min(max(x, 6), N - 7)
            cy = min(max(y, 6), N - 7)
            if (x - cx) ** 2 + (y - cy) ** 2 > 6.3 ** 2:
                continue
            rand = (x - cx) ** 2 + (y - cy) ** 2 > 5.0 ** 2 or x in (0, N - 1) or y in (0, N - 1)
            if rand:
                px[x, y] = RAND
                continue
            t = y / (N - 1)
            c = tuple(int(mid[i] * (1 - t) + donker[i] * t) for i in range(3))
            if y <= 2:
                c = licht
            px[x, y] = (*c, 255)
    return img


def omlijn(symbool):
    """A 1-px dark outline around everything drawn on the symbol layer."""
    a = np.array(symbool)
    alpha = a[:, :, 3] > 0
    rand = np.zeros_like(alpha)
    for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        rand |= np.roll(np.roll(alpha, dy, 0), dx, 1)
    rand &= ~alpha
    out = a.copy()
    out[rand] = RAND
    return Image.fromarray(out)


def samen(kleur, teken):
    img = badge(kleur)
    s = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    teken(ImageDraw.Draw(s), s)
    img.alpha_composite(omlijn(s))
    return img


def hartje(d, x, y, k=HART, groot=1):
    """A small pixel heart with its top-left at (x, y); groot=1 is 7x6, groot=2 is 9x8."""
    if groot == 1:
        rijen = [".kk.kk.", "kkkkkkk", "kkkkkkk", ".kkkkk.", "..kkk..", "...k..."]
    else:
        rijen = [".kkk.kkk.", "kkkkkkkkk", "kkkkkkkkk", "kkkkkkkkk", ".kkkkkkk.", "..kkkkk..", "...kkk...", "....k...."]
    for j, r in enumerate(rijen):
        for i, c in enumerate(r):
            if c == "k":
                d.point((x + i, y + j), fill=k)


def ster(d, cx, cy, r, k, binnen=0.45):
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        rr = r if i % 2 == 0 else r * binnen
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    d.polygon(pts, fill=k)


def kussen(d, x0, y0, x1, y1, k=WIT, schaduw=LICHTROZE):
    d.rounded_rectangle((x0, y0, x1, y1), radius=3, fill=k)
    d.line((x0 + 2, y1 - 1, x1 - 2, y1 - 1), fill=schaduw)
    # the four little corner tufts
    for (x, y) in ((x0 - 1, y0 - 1), (x1 + 1, y0 - 1), (x0 - 1, y1 + 1), (x1 + 1, y1 + 1)):
        d.point((x, y), fill=k)


def pijl(d, x0, y0, x1, y1, schacht=HOUT, punt=GRIJS, veer=ROZE):
    d.line((x0, y0, x1, y1), fill=schacht, width=1)
    hoek = math.atan2(y1 - y0, x1 - x0)
    for s in (-1, 1):
        a = hoek + math.pi + s * 0.6
        d.line((x1, y1, x1 + 3 * math.cos(a), y1 + 3 * math.sin(a)), fill=punt)
    d.point((x1, y1), fill=punt)
    for s in (-1, 1):
        a = hoek + math.pi + s * 0.7
        d.line((x0, y0, x0 + 2.5 * math.cos(a), y0 + 2.5 * math.sin(a)), fill=veer)


# ---- the icons ----------------------------------------------------------------------------------------------------------------

def kussenklap(d, s):
    # a swoosh arc and a pillow at its end
    d.arc((4, 6, 28, 30), start=200, end=330, fill=LICHTROZE, width=2)
    d.arc((7, 9, 25, 27), start=210, end=320, fill=WIT, width=1)
    kussen(d, 13, 11, 25, 20)
    hartje(d, 16, 13, ROZE)
    for (x, y) in ((6, 22), (8, 25), (5, 18)):
        d.point((x, y), fill=WIT)


def stormsprong(d, s):
    # a jump arc, a landing shockwave and a lightning bolt
    d.arc((4, 6, 26, 30), start=190, end=350, fill=LICHTROZE, width=1)
    d.ellipse((14, 22, 28, 27), outline=WIT)
    d.ellipse((17, 23, 25, 26), outline=LICHTROZE)
    d.polygon([(14, 4), (8, 15), (13, 15), (10, 24), (19, 11), (14, 11), (18, 4)], fill=GOUD)
    d.line((14, 5, 11, 12), fill=LICHTGOUD)


def knuffelschild(d, s):
    d.polygon([(8, 6), (24, 6), (24, 16), (16, 27), (8, 16)], fill=GRIJS)
    d.polygon([(10, 8), (22, 8), (22, 16), (16, 24), (10, 16)], fill=(230, 230, 240, 255))
    d.line((8, 6, 24, 6), fill=WIT)
    hartje(d, 12, 11, HART, 2)


def vahoege_plof(d, s):
    # a big pillow coming down with stars and impact lines
    kussen(d, 7, 6, 25, 18)
    hartje(d, 12, 8, ROZE, 2)
    for x in (9, 16, 23):
        d.line((x, 21, x, 26), fill=WIT)
    d.line((5, 27, 27, 27), fill=LICHTROZE)
    ster(d, 5, 9, 3, GOUD)
    ster(d, 27, 22, 3, GOUD)


def pluizenregen(d, s):
    # a fluffy cloud with three arrows raining down
    d.ellipse((5, 4, 15, 12), fill=WIT)
    d.ellipse((11, 2, 23, 12), fill=WIT)
    d.ellipse((19, 5, 28, 12), fill=WIT)
    d.rectangle((7, 9, 26, 12), fill=WIT)
    d.line((8, 12, 25, 12), fill=LICHTROZE)
    for x in (9, 16, 23):
        pijl(d, x + 2, 15, x - 1, 27)


def rolsalto(d, s):
    # a looping arrow (the flip) with speed lines
    d.arc((6, 6, 26, 26), start=40, end=330, fill=LICHTGOUD, width=3)
    d.polygon([(22, 6), (28, 12), (20, 13)], fill=LICHTGOUD)
    d.ellipse((13, 13, 19, 19), fill=WIT)
    for y in (10, 16, 22):
        d.line((2, y, 5, y), fill=WIT)


def knuffelpijl(d, s):
    hartje(d, 10, 9, HART, 2)
    hartje(d, 11, 10, (255, 120, 170, 255), 1)
    pijl(d, 4, 26, 26, 5, schacht=GOUD, punt=WIT, veer=ROZE)


def sterrenschot(d, s):
    # a laser beam with a big star at the front
    d.line((3, 28, 22, 9), fill=LICHTGOUD, width=3)
    d.line((3, 28, 22, 9), fill=WIT, width=1)
    ster(d, 22, 9, 8, GOUD)
    ster(d, 22, 9, 4, LICHTGOUD)
    for (x, y) in ((7, 9), (10, 5), (26, 24)):
        d.point((x, y), fill=WIT)


def roze_vuurbal(d, s):
    # a pink fireball flying to the upper right with a flame tail
    d.polygon([(3, 29), (12, 17), (15, 22)], fill=(255, 90, 160, 255))
    d.polygon([(6, 26), (13, 15), (18, 18)], fill=ROZE)
    d.ellipse((12, 5, 27, 20), fill=(255, 90, 160, 255))
    d.ellipse((14, 7, 25, 18), fill=ROZE)
    d.ellipse((17, 9, 23, 15), fill=LICHTROZE)
    d.point((20, 11), fill=WIT)
    d.point((21, 11), fill=WIT)


def blink(d, s):
    # a dashed trail and a sparkle where you appear
    for i, x in enumerate(range(4, 16, 4)):
        d.line((x, 24 - i * 3, x + 2, 23 - i * 3), fill=LICHTROZE)
    ster(d, 21, 11, 9, WIT, binnen=0.25)
    ster(d, 21, 11, 5, BLAUW, binnen=0.3)
    d.point((21, 11), fill=WIT)
    ster(d, 8, 8, 3, LICHTROZE, binnen=0.3)


def vadsbron(d, s):
    # a glowing heal circle with a plus and rising bubbles
    d.ellipse((4, 18, 28, 28), fill=(120, 230, 160, 255))
    d.ellipse((7, 20, 25, 26), fill=(190, 255, 200, 255))
    d.rectangle((14, 5, 18, 17), fill=WIT)
    d.rectangle((10, 9, 22, 13), fill=WIT)
    d.rectangle((15, 6, 17, 16), fill=GROEN)
    d.rectangle((11, 10, 21, 12), fill=GROEN)
    for (x, y) in ((6, 9), (25, 6), (26, 14)):
        d.ellipse((x - 1, y - 1, x + 1, y + 1), fill=(190, 255, 200, 255))


def meteoorguh(d, s):
    # a big guh-meteor (round, with a little face) with a fiery tail
    d.polygon([(2, 4), (14, 13), (10, 18)], fill=ORANJE)
    d.polygon([(4, 2), (17, 10), (13, 15)], fill=GOUD)
    d.ellipse((11, 10, 28, 27), fill=(255, 100, 150, 255))
    d.ellipse((13, 12, 26, 25), fill=ROZE)
    d.point((17, 17), fill=RAND)
    d.point((22, 17), fill=RAND)
    d.line((18, 21, 21, 21), fill=RAND)
    d.point((15, 14), fill=LICHTROZE)


def knuffelsprong(d, s):
    # a big leap arc with a heart on top
    d.arc((4, 12, 28, 40), start=190, end=350, fill=WIT, width=2)
    for (x, y) in ((5, 26), (27, 26)):
        d.ellipse((x - 2, y - 1, x + 2, y + 1), fill=LICHTROZE)
    hartje(d, 12, 4, HART, 2)
    for (x, y) in ((8, 9), (24, 9)):
        d.point((x, y), fill=WIT)


ICONEN = {
    "kussenklap": (RIDDER, kussenklap),
    "stormsprong": (RIDDER, stormsprong),
    "knuffelschild": (RIDDER, knuffelschild),
    "vahoege_plof": (RIDDER, vahoege_plof),
    "pluizenregen": (JAGER, pluizenregen),
    "rolsalto": (JAGER, rolsalto),
    "knuffelpijl": (JAGER, knuffelpijl),
    "sterrenschot": (JAGER, sterrenschot),
    "roze_vuurbal": (MAGIER, roze_vuurbal),
    "blink": (MAGIER, blink),
    "vadsbron": (MAGIER, vadsbron),
    "meteoorguh": (MAGIER, meteoorguh),
    "knuffelsprong": (SPRONG, knuffelsprong),
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for naam, (kleur, teken) in ICONEN.items():
        samen(kleur, teken).save(os.path.join(OUT, naam + ".png"))
    print(f"{len(ICONEN)} skill icons -> {OUT}")


if __name__ == "__main__":
    main()
