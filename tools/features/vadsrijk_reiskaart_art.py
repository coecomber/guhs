"""The Vadsrijk travel map (Reiskaart) art: an illustrated "idea map", not the real terrain (fix round 2, g-ux).

Writes src/main/resources/assets/guhs/textures/gui/vadsrijk/reiskaart.png (768x480): a pastel sea with soft waves, the
starter island Knuffelkamp (bottom left), zone 1 (Zwamzoet + De Haven, mushroom caps), zone 2 (De Splinterpieken, snowy
peaks around the pink Kandijkom geode), zone 3 (De Schemerwortel, LARGER than zone 2, a dusky forest with a giant tree) and
a small far-off zone 4 silhouette. Icons, routes, names, "?" marks and the you-are-here marker are drawn by
npc/client/ReiskaartScherm, which also holds the same spot fractions (PLEKKEN below).

Run from the worktree root: python tools/features/vadsrijk_reiskaart_art.py  (deterministic: fixed seed).
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

W, H = 768, 480
UIT = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "gui", "vadsrijk", "reiskaart.png")

# destination spots as fractions of the image (keep in sync with ReiskaartScherm.PLEKKEN)
PLEKKEN = {
    "knuffelkamp": (0.12, 0.80),
    "zwamzoet": (0.30, 0.50),
    "kandijkom": (0.52, 0.22),
    "schemerwortel": (0.77, 0.56),
    "zone4": (0.93, 0.14),
}

ZEE = (150, 206, 226)
ZEE_DIEP = (126, 186, 214)
GOLF = (196, 232, 242)
PAPIER = (247, 234, 212)
INKT = (74, 58, 85)


def blob(d, cx, cy, rx, ry, kleur, rnd, punten=40, ruw=0.07):
    pts = []
    for i in range(punten):
        a = 2 * math.pi * i / punten
        f = 1 + ruw * (rnd.random() - 0.5) * 2
        pts.append((cx + math.cos(a) * rx * f, cy + math.sin(a) * ry * f))
    d.polygon(pts, fill=kleur)
    return pts


def eiland(img, cx, cy, rx, ry, land, rand, rnd):
    """A soft island: sand rim, then the land colour, slightly blurred edges."""
    laag = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(laag)
    s = rnd.random()
    blob(d, cx, cy, rx * 1.08, ry * 1.1, rand, random.Random(s))
    blob(d, cx, cy, rx, ry, land, random.Random(s))
    laag = laag.filter(ImageFilter.GaussianBlur(1.2))
    img.alpha_composite(laag)


def paddenstoel(d, x, y, s, hoed):
    d.rectangle([x - s * 0.18, y - s * 0.7, x + s * 0.18, y], fill=(244, 232, 214))
    d.pieslice([x - s * 0.6, y - s * 1.25, x + s * 0.6, y - s * 0.25], 180, 360, fill=hoed)
    d.ellipse([x - s * 0.2, y - s * 1.05, x - s * 0.02, y - s * 0.88], fill=(255, 248, 240))


def berg(d, x, y, s, kleur, sneeuw):
    d.polygon([(x - s, y), (x, y - s * 1.4), (x + s, y)], fill=kleur)
    d.polygon([(x - s * 0.35, y - s * 0.9), (x, y - s * 1.4), (x + s * 0.35, y - s * 0.9)], fill=sneeuw)


def boom(d, x, y, s, kleur):
    d.rectangle([x - s * 0.12, y - s * 0.5, x + s * 0.12, y], fill=(96, 70, 88))
    d.ellipse([x - s * 0.55, y - s * 1.3, x + s * 0.55, y - s * 0.35], fill=kleur)


def maak():
    rnd = random.Random(1100)
    img = Image.new("RGBA", (W, H), ZEE + (255,))
    d = ImageDraw.Draw(img)
    # deep-water patches and little waves
    for _ in range(14):
        x, y = rnd.uniform(0, W), rnd.uniform(0, H)
        d.ellipse([x - 70, y - 30, x + 70, y + 30], fill=ZEE_DIEP)
    img = img.filter(ImageFilter.GaussianBlur(18)).convert("RGBA")
    d = ImageDraw.Draw(img)
    for _ in range(140):
        x, y = rnd.uniform(0, W), rnd.uniform(0, H)
        d.arc([x - 6, y - 3, x + 6, y + 3], 200, 340, fill=GOLF, width=1)

    def P(k):
        fx, fy = PLEKKEN[k]
        return fx * W, fy * H

    # zone 4 (far off, a small misty silhouette in the top right)
    x, y = P("zone4")
    eiland(img, x, y, 52, 30, (176, 186, 204, 255), (204, 210, 222, 255), rnd)
    d = ImageDraw.Draw(img)
    for i in range(4):
        berg(d, x - 30 + i * 18, y + 8, 12, (150, 160, 182), (210, 216, 230))
    # zone 3: De Schemerwortel, larger than zone 2 (dusky purple woods, a giant tree)
    x, y = P("schemerwortel")
    eiland(img, x, y, 150, 108, (124, 98, 150, 255), (200, 182, 160, 255), rnd)
    d = ImageDraw.Draw(img)
    for _ in range(46):
        bx, by = x + rnd.uniform(-125, 125), y + rnd.uniform(-80, 85)
        if ((bx - x) / 135) ** 2 + ((by - y) / 95) ** 2 < 1:
            boom(d, bx, by, rnd.uniform(9, 14), (rnd.randint(92, 118), rnd.randint(70, 90), rnd.randint(128, 156)))
    # the giant tree
    d.rectangle([x - 9, y - 40, x + 9, y + 18], fill=(84, 60, 80))
    for dx in (-26, 0, 26):
        d.ellipse([x + dx - 30, y - 84, x + dx + 30, y - 30], fill=(150, 112, 176))
    d.ellipse([x - 34, y - 104, x + 34, y - 56], fill=(168, 128, 192))
    # zone 2: De Splinterpieken (snowy peaks around the pink geode)
    x, y = P("kandijkom")
    eiland(img, x, y, 104, 72, (232, 240, 248, 255), (206, 218, 232, 255), rnd)
    d = ImageDraw.Draw(img)
    for i in range(9):
        a = 2 * math.pi * i / 9
        berg(d, x + math.cos(a) * 66, y + math.sin(a) * 42 + 10, rnd.uniform(14, 20), (170, 186, 210), (255, 255, 255))
    d.ellipse([x - 26, y - 18, x + 26, y + 18], fill=(244, 170, 204))
    d.ellipse([x - 18, y - 12, x + 18, y + 12], fill=(255, 214, 230))
    # zone 1: the mainland (green, mushroom caps)
    x, y = P("zwamzoet")
    eiland(img, x, y, 132, 92, (162, 214, 140, 255), (236, 220, 170, 255), rnd)
    d = ImageDraw.Draw(img)
    for _ in range(26):
        bx, by = x + rnd.uniform(-110, 110), y + rnd.uniform(-70, 75)
        if ((bx - x) / 118) ** 2 + ((by - y) / 82) ** 2 < 1:
            boom(d, bx, by, rnd.uniform(8, 11), (rnd.randint(104, 130), rnd.randint(170, 196), rnd.randint(96, 118)))
    for dx, s, k in ((-22, 26, (236, 96, 120)), (8, 34, (248, 132, 160)), (34, 22, (226, 86, 112))):
        paddenstoel(d, x + dx, y + 14, s, k)
    # the starter island Knuffelkamp (bottom left)
    x, y = P("knuffelkamp")
    eiland(img, x, y, 56, 38, (176, 222, 150, 255), (240, 224, 176, 255), rnd)
    d = ImageDraw.Draw(img)
    for dx in (-18, 0, 18):
        d.polygon([(x + dx - 9, y + 8), (x + dx, y - 8), (x + dx + 9, y + 8)], fill=(250, 196, 120))   # tents
    d.ellipse([x - 4, y + 12, x + 4, y + 18], fill=(255, 150, 70))   # the campfire
    # paper frame
    rand = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rand)
    for i in range(14):
        a = int(170 * (1 - i / 14))
        rd.rectangle([i, i, W - 1 - i, H - 1 - i], outline=PAPIER + (a,))
    rd.rectangle([0, 0, W - 1, H - 1], outline=INKT + (255,), width=2)
    img.alpha_composite(rand)
    os.makedirs(os.path.dirname(UIT), exist_ok=True)
    img.convert("RGB").save(UIT)
    print("reiskaart:", UIT)


if __name__ == "__main__":
    maak()
