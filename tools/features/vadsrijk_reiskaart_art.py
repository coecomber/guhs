"""The Vadsrijk travel map (Reiskaart) art: a pastel illustrated map of the WHOLE Vadsrijk at its real proportions (1.10.3 rework;
first version fix round 2, g-ux).

Writes src/main/resources/assets/guhs/textures/gui/vadsrijk/reiskaart.png. Every zone sits at its real spot through ONE linear
world -> image mapping that lives in Java (npc/ReiskaartIndeling.java: W, H, X0, Z0, S, Z5_OPTIL and the destinations); this
script READS those numbers (and the land boxes LAND_* of indeling/VadsrijkZones.java) with a regex, so the art and the badges in
npc/client/ReiskaartScherm can never drift apart. Change the mapping there and run this script again.

The landscapes: Knuffelkamp (camp island, tents + campfire), zone 1 Zwamzoet (green, mushrooms, woods, drip caves, the temple),
zone 2 De Splinterpieken (snowy peaks around the pink Kandijkom geode, a frozen lake), zone 3 De Schemerwortel (a dusky root
forest with the giant tree), zone 4 De Lampionkust (sandy islets with lanterns in turquoise shallows), zone 5 Hoogvads (a floating
island with a soft shadow, drawn Z5_OPTIL pixels higher) and zone 6 Het Zijdeland (silk-pink dunes, palms, an oasis, the start of
the mainland running off the east edge). Icons, routes, names, area labels, campfires, mist and "?" marks are drawn by the client.

Run from the worktree root: python tools/features/vadsrijk_reiskaart_art.py  (deterministic: fixed seeds)
Optional: --voorbeeld <dir>  also writes <dir>/reiskaart_kaal.png (the art) and <dir>/reiskaart_mock.png (a rough mock of the
client overlay: badges, labels, campfires, the player marker, zones 4-6 under mist).
"""
import math
import os
import random
import re
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")
UIT = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "gui", "vadsrijk", "reiskaart.png")


def lees_indeling():
    """The mapping + destinations + labels from ReiskaartIndeling.java and the land boxes from VadsrijkZones.java."""
    ri = open(os.path.join(JAVA, "npc", "ReiskaartIndeling.java"), encoding="utf-8").read()
    vz = open(os.path.join(JAVA, "indeling", "VadsrijkZones.java"), encoding="utf-8").read()
    c = {}
    for k in ("W", "H", "X0", "Z0", "Z5_OPTIL"):
        c[k] = int(re.search(r"static final int %s = (-?\d+);" % k, ri)[1])
    c["S"] = float(re.search(r"static final float S = ([\d.]+)f;", ri)[1])
    land = {m[1]: tuple(int(v) for v in m[2].split(",")) for m in re.finditer(r"int\[\] (LAND_\w+) = \{([-\d, ]+)\};", vz)}
    c["land"] = land
    c["plekken"] = [dict(id=m[1], zone=int(m[2]), x=int(m[3]), z=int(m[4]), land=land[m[5]])
                    for m in re.finditer(r'new Plek\("(\w+)", (\d), (-?\d+), (-?\d+), VadsrijkZones\.(LAND_\w+)\)', ri)]
    c["namen"] = [dict(id=m[1], zone=int(m[2]), x=int(m[3]), z=int(m[4]))
                  for m in re.finditer(r'new Naam\("(\w+)", (\d), (-?\d+), (-?\d+)\)', ri)]
    assert len(c["plekken"]) == 7 and c["namen"], "ReiskaartIndeling.java not read"
    return c


C = lees_indeling()
W, H, S = C["W"], C["H"], C["S"]


def px(x):
    return (x - C["X0"]) * S


def py(z, zone=0):
    return (z - C["Z0"]) * S - (C["Z5_OPTIL"] if zone == 5 else 0)


def doos(naam, zone=0):
    x0, z0, x1, z1 = C["land"][naam]
    return px(x0), py(z0, zone), px(x1), py(z1, zone)


ZEE = (150, 206, 226)
ZEE_DIEP = (124, 184, 214)
ONDIEP = (176, 228, 226)
GOLF = (198, 234, 244)
PAPIER = (247, 234, 212)
INKT = (74, 58, 85)
ZAND = (240, 224, 176)


# ---- drawing helpers -----------------------------------------------------------------------------------------------------------
def blob_pts(cx, cy, rx, ry, rnd, punten=64, ruw=0.08, golf=3):
    fase = rnd.random() * 6.28
    pts = []
    for i in range(punten):
        a = 2 * math.pi * i / punten
        f = 1 + ruw * (rnd.random() - 0.5) * 2 * 0.5 + ruw * math.sin(a * golf + fase)
        pts.append((cx + math.cos(a) * rx * f, cy + math.sin(a) * ry * f))
    return pts


def eiland(img, cx, cy, rx, ry, land, rand=ZAND, seed=1, ruw=0.08, golf=3, ondiep=True):
    """A soft island: shallow water halo, a sand rim, then the land colour; slightly blurred edges."""
    if ondiep:
        zachte_laag(img, lambda dd: dd.polygon(blob_pts(cx, cy, rx * 1.16 + 6, ry * 1.18 + 6, random.Random(seed), ruw=ruw, golf=golf), fill=255),
                    5, ONDIEP + (200,))
    laag = Image.new("RGBA", img.size, rand + (0,))
    d = ImageDraw.Draw(laag)
    d.polygon(blob_pts(cx, cy, rx * 1.05 + 2, ry * 1.06 + 2, random.Random(seed), ruw=ruw, golf=golf), fill=rand + (255,))
    d.polygon(blob_pts(cx, cy, rx, ry, random.Random(seed), ruw=ruw, golf=golf), fill=land + (255,))
    laag = laag.filter(ImageFilter.GaussianBlur(0.9))
    img.alpha_composite(laag)


def in_ellips(x, y, cx, cy, rx, ry):
    return ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 < 1


def boom(d, x, y, s, kleur, stam=(96, 70, 88)):
    d.rectangle([x - s * 0.12, y - s * 0.5, x + s * 0.12, y], fill=stam)
    d.ellipse([x - s * 0.55, y - s * 1.3, x + s * 0.55, y - s * 0.35], fill=kleur)
    d.ellipse([x - s * 0.3, y - s * 1.15, x - s * 0.05, y - s * 0.9], fill=tuple(min(255, v + 30) for v in kleur))


def paddenstoel(d, x, y, s, hoed):
    d.rectangle([x - s * 0.18, y - s * 0.7, x + s * 0.18, y], fill=(244, 232, 214))
    d.pieslice([x - s * 0.6, y - s * 1.25, x + s * 0.6, y - s * 0.25], 180, 360, fill=hoed)
    d.ellipse([x - s * 0.2, y - s * 1.05, x - s * 0.02, y - s * 0.88], fill=(255, 248, 240))
    d.ellipse([x + s * 0.15, y - s * 0.95, x + s * 0.3, y - s * 0.8], fill=(255, 248, 240))


def berg(d, x, y, s, kleur, sneeuw):
    d.polygon([(x - s, y), (x, y - s * 1.4), (x + s, y)], fill=kleur)
    d.polygon([(x, y - s * 1.4), (x + s, y), (x + s * 0.3, y)], fill=tuple(max(0, v - 18) for v in kleur))
    d.polygon([(x - s * 0.35, y - s * 0.9), (x, y - s * 1.4), (x + s * 0.35, y - s * 0.9), (x, y - s * 1.0)], fill=sneeuw)


def palm(d, x, y, s):
    d.line([(x, y), (x + s * 0.15, y - s)], fill=(150, 110, 80), width=max(1, int(s * 0.18)))
    tx, ty = x + s * 0.15, y - s
    for a in (-150, -110, -60, -20, 200):
        r = math.radians(a)
        d.line([(tx, ty), (tx + math.cos(r) * s * 0.7, ty + math.sin(r) * s * 0.45)], fill=(110, 178, 110), width=max(1, int(s * 0.2)))


def lampion(d, x, y, r=2):
    d.ellipse([x - r - 2, y - r - 2, x + r + 2, y + r + 2], fill=(255, 236, 170))
    d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 170, 90))


def zachte_laag(img, teken, blur, kleur):
    """A soft-edged shape in one colour: teken() draws the shape (fill=255) into a mask, which is blurred (no dark halo)."""
    masker = Image.new("L", img.size, 0)
    teken(ImageDraw.Draw(masker))
    masker = masker.filter(ImageFilter.GaussianBlur(blur))
    if len(kleur) == 4:
        masker = masker.point(lambda v: v * kleur[3] // 255)
    laag = Image.new("RGBA", img.size, kleur[:3] + (0,))
    laag.putalpha(masker)
    img.alpha_composite(laag)


# ---- the zones -----------------------------------------------------------------------------------------------------------------
def zee(img, rnd):
    d = ImageDraw.Draw(img)
    for _ in range(40):
        x, y = rnd.uniform(0, W), rnd.uniform(0, H)
        d.ellipse([x - 90, y - 36, x + 90, y + 36], fill=ZEE_DIEP)
    img2 = img.filter(ImageFilter.GaussianBlur(24)).convert("RGBA")
    d = ImageDraw.Draw(img2)
    for _ in range(420):
        x, y = rnd.uniform(0, W), rnd.uniform(0, H)
        d.arc([x - 6, y - 3, x + 6, y + 3], 200, 340, fill=GOLF, width=1)
    return img2


def kamp(img, rnd):
    x0, y0, x1, y1 = doos("LAND_KAMP")
    cx, cy, rx, ry = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2, (y1 - y0) / 2
    eiland(img, cx, cy, rx, ry, (176, 222, 150), seed=11)
    d = ImageDraw.Draw(img)
    for _ in range(10):
        bx, by = cx + rnd.uniform(-rx * 0.8, rx * 0.8), cy + rnd.uniform(-ry * 0.7, ry * 0.8)
        if in_ellips(bx, by, cx, cy, rx * 0.85, ry * 0.85) and abs(bx - cx) > 16:
            boom(d, bx, by, rnd.uniform(6, 8), (120, 186, 110))
    for dx in (-13, 0, 13):
        d.polygon([(cx + dx - 7, cy + 4), (cx + dx, cy - 9), (cx + dx + 7, cy + 4)], fill=(250, 196, 120))   # tents
        d.line([(cx + dx, cy - 9), (cx + dx, cy + 4)], fill=(220, 160, 96))
    d.ellipse([cx - 4, cy + 8, cx + 4, cy + 13], fill=(255, 150, 70))   # the campfire
    d.ellipse([cx - 2, cy + 7, cx + 2, cy + 10], fill=(255, 220, 120))
    # the pier (north side, towards De Haven)
    d.rectangle([cx + 3, y0 - 8, cx + 7, y0 + 6], fill=(170, 130, 100))


def zone1(img, rnd):
    x0, y0, x1, y1 = doos("LAND_Z1")
    cx, cy, rx, ry = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2 * 0.98, (y1 - y0) / 2 * 0.98
    eiland(img, cx, cy, rx, ry, (162, 214, 140), seed=21, ruw=0.07, golf=5)
    d = ImageDraw.Draw(img)
    # Klodderweide (east): a lighter meadow with flowers
    zachte_laag(img, lambda dd: dd.ellipse([px(170), py(-280), px(600), py(380)], fill=255), 10, (196, 232, 160, 255))
    d = ImageDraw.Draw(img)
    for _ in range(90):
        bx, by = rnd.uniform(px(190), px(580)), rnd.uniform(py(-260), py(360))
        if in_ellips(bx, by, cx, cy, rx * 0.9, ry * 0.9):
            d.point((bx, by), fill=rnd.choice([(255, 200, 220), (255, 250, 210), (220, 200, 255)]))
    # Druipgrotten (north-west): grey rocks with cave mouths
    for _ in range(26):
        bx, by = rnd.uniform(px(-590), px(-170)), rnd.uniform(py(-590), py(-170))
        if in_ellips(bx, by, cx, cy, rx * 0.88, ry * 0.88):
            s = rnd.uniform(6, 11)
            d.polygon([(bx - s, by), (bx - s * 0.4, by - s * 1.1), (bx + s * 0.5, by - s * 0.9), (bx + s, by)], fill=(168, 160, 176))
            d.ellipse([bx - s * 0.3, by - s * 0.45, bx + s * 0.3, by], fill=(110, 96, 120))
    # Sporenwoud (west) + scattered woods
    for _ in range(150):
        bx, by = cx + rnd.uniform(-rx, rx), cy + rnd.uniform(-ry, ry)
        if not in_ellips(bx, by, cx, cy, rx * 0.86, ry * 0.86):
            continue
        if px(-600) < bx < px(-160) and py(-160) < by < py(400):
            boom(d, bx, by, rnd.uniform(8, 11), (rnd.randint(96, 120), rnd.randint(160, 186), rnd.randint(110, 130)))
        elif rnd.random() < 0.25 and not (px(170) < bx < px(600) and py(-280) < by < py(380)) and not (px(-600) < bx < px(-160) and by < py(-160)):
            boom(d, bx, by, rnd.uniform(7, 10), (rnd.randint(104, 130), rnd.randint(170, 196), rnd.randint(96, 118)))
    # De Zwamtempel (north-east): a little pink temple
    tx, ty = px(380), py(-450)
    d.rectangle([tx - 14, ty - 6, tx + 14, ty + 8], fill=(236, 214, 226))
    d.polygon([(tx - 18, ty - 6), (tx, ty - 20), (tx + 18, ty - 6)], fill=(232, 128, 160))
    for i in range(-2, 3):
        d.rectangle([tx + i * 6 - 1, ty - 4, tx + i * 6 + 1, ty + 8], fill=(255, 246, 250))
    # Zwamzoet: the giant mushrooms in the middle
    zx, zy = px(0), py(0)
    for dx, s, k in ((-26, 24, (236, 96, 120)), (6, 34, (248, 132, 160)), (32, 20, (226, 86, 112)), (-6, 16, (250, 160, 180))):
        paddenstoel(d, zx + dx, zy + 16 + (s < 20) * 6, s, k)
    # De Haven (south): a harbour with a pier
    hx, hy = px(0), py(560)
    d.rectangle([hx - 16, hy - 6, hx + 16, hy + 2], fill=(214, 186, 150))
    d.rectangle([hx - 3, hy, hx + 3, hy + 22], fill=(170, 130, 100))
    for i in (-12, -2, 8):
        d.rectangle([hx + i, hy - 12, hx + i + 7, hy - 6], fill=(244, 170, 150))


def zone2(img, rnd):
    x0, y0, x1, y1 = doos("LAND_Z2")
    cx, cy, rx, ry = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2, (y1 - y0) / 2
    eiland(img, cx, cy, rx, ry, (232, 240, 248), rand=(206, 218, 232), seed=31, ruw=0.1, golf=6)
    d = ImageDraw.Draw(img)
    # the frozen Knuffelmeer (west)
    mx, my = px(530), py(-1010)
    d.ellipse([mx - 20, my - 26, mx + 20, my + 26], fill=(186, 222, 242))
    d.ellipse([mx - 13, my - 18, mx + 6, my + 4], fill=(222, 242, 252))
    # peaks everywhere except the middle
    gx, gy = px(704), py(-1057)
    pieken = []
    for _ in range(70):
        bx, by = cx + rnd.uniform(-rx, rx), cy + rnd.uniform(-ry, ry)
        if in_ellips(bx, by, cx, cy, rx * 0.86, ry * 0.86) and not in_ellips(bx, by, gx, gy, 34, 30) and not in_ellips(bx, by, mx, my, 26, 32):
            pieken.append((by, bx, rnd.uniform(9, 17)))
    for by, bx, s in sorted(pieken):
        berg(d, bx, by, s, (rnd.randint(160, 180), rnd.randint(178, 196), rnd.randint(204, 220)), (255, 255, 255))
    # the pink Kandijkom geode
    d.ellipse([gx - 24, gy - 18, gx + 24, gy + 18], fill=(222, 140, 186))
    d.ellipse([gx - 18, gy - 13, gx + 18, gy + 13], fill=(244, 170, 204))
    d.ellipse([gx - 11, gy - 8, gx + 11, gy + 8], fill=(255, 214, 230))
    for a in range(0, 360, 45):
        r = math.radians(a)
        d.polygon([(gx + math.cos(r) * 14, gy + math.sin(r) * 10), (gx + math.cos(r) * 22, gy + math.sin(r) * 16 - 4),
                   (gx + math.cos(r + 0.25) * 16, gy + math.sin(r + 0.25) * 12)], fill=(250, 196, 222))
    # De Spiegelburcht (north-east): a silver castle
    sx, sy = px(860), py(-1360)
    d.rectangle([sx - 10, sy - 8, sx + 10, sy + 6], fill=(214, 220, 238))
    for dx in (-10, 10):
        d.rectangle([sx + dx - 3, sy - 16, sx + dx + 3, sy + 6], fill=(196, 204, 230))
        d.polygon([(sx + dx - 4, sy - 16), (sx + dx, sy - 23), (sx + dx + 4, sy - 16)], fill=(170, 150, 210))


def zone3(img, rnd):
    x0, y0, x1, y1 = doos("LAND_Z3")
    cx, cy, rx, ry = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2, (y1 - y0) / 2
    eiland(img, cx, cy, rx, ry, (124, 98, 150), rand=(200, 182, 160), seed=41, ruw=0.09, golf=7)
    d = ImageDraw.Draw(img)
    tx, ty = px(1404), py(-576)
    # roots radiating from the trunk
    for i in range(14):
        a = 2 * math.pi * i / 14 + rnd.uniform(-0.15, 0.15)
        pts = [(tx, ty)]
        for k in range(1, 7):
            r = k / 6
            pts.append((tx + math.cos(a + math.sin(k) * 0.2) * rx * 0.85 * r, ty + math.sin(a + math.sin(k) * 0.2) * ry * 0.85 * r))
        d.line(pts, fill=(92, 70, 98), width=3)
    # the dusky woods
    for _ in range(110):
        bx, by = cx + rnd.uniform(-rx, rx), cy + rnd.uniform(-ry, ry)
        if in_ellips(bx, by, cx, cy, rx * 0.86, ry * 0.86) and not in_ellips(bx, by, tx, ty, 46, 40):
            boom(d, bx, by, rnd.uniform(8, 12), (rnd.randint(92, 118), rnd.randint(70, 90), rnd.randint(128, 156)), stam=(70, 50, 70))
    # glowing mushrooms in the dusk
    for _ in range(30):
        bx, by = cx + rnd.uniform(-rx, rx), cy + rnd.uniform(-ry, ry)
        if in_ellips(bx, by, cx, cy, rx * 0.8, ry * 0.8):
            d.ellipse([bx - 1.5, by - 1.5, bx + 1.5, by + 1.5], fill=(190, 255, 220))
    # the giant tree (trunk + crown with honey lanterns)
    zachte_laag(img, lambda dd: dd.ellipse([tx - 52, ty - 20, tx + 52, ty + 26], fill=255), 6, (40, 24, 50, 120))
    d = ImageDraw.Draw(img)
    d.polygon([(tx - 12, ty + 16), (tx - 7, ty - 34), (tx + 7, ty - 34), (tx + 12, ty + 16)], fill=(84, 60, 80))
    for dx, dy, r in ((-30, -48, 26), (30, -48, 26), (0, -62, 32), (-16, -78, 22), (18, -80, 22)):
        d.ellipse([tx + dx - r, ty + dy - r * 0.8, tx + dx + r, ty + dy + r * 0.8], fill=(150, 112, 176))
    d.ellipse([tx - 30, ty - 96, tx + 30, ty - 58], fill=(172, 132, 196))
    for _ in range(14):
        lampion(d, tx + rnd.uniform(-42, 42), ty + rnd.uniform(-90, -36), 1.5)
    # De Kaaszeppelinwerf (north-east): a cheese zeppelin
    zx, zy = px(1643), py(-850)
    d.ellipse([zx - 16, zy - 6, zx + 16, zy + 6], fill=(255, 214, 110))
    for i in (-8, 0, 8):
        d.ellipse([zx + i - 2, zy - 2, zx + i + 1, zy + 1], fill=(230, 180, 80))
    d.rectangle([zx - 4, zy + 6, zx + 4, zy + 9], fill=(150, 110, 90))


def zone4(img, rnd):
    x0, y0, x1, y1 = doos("LAND_Z4")
    # turquoise shallows over the whole coast
    zachte_laag(img, lambda dd: dd.ellipse([x0 - 6, y0 + 4, x1 + 6, y1 - 2], fill=255), 16, ONDIEP + (150,))
    eilanden = [(1846, -470, 30, 26)]   # Lampionhaven
    for n in C["namen"]:
        if n["zone"] == 4:
            eilanden.append((n["x"], n["z"] + 18, rnd.uniform(16, 22), rnd.uniform(12, 17)))
    eilanden += [(2400, -500, 14, 12), (2470, -565, 14, 12), (2165, -700, 9, 7), (2080, -520, 8, 7), (2370, -350, 8, 6)]
    for i, (wx, wz, rx, ry) in enumerate(eilanden):
        cx, cy = px(wx), py(wz)
        eiland(img, cx, cy, rx, ry, (196, 226, 150), rand=(250, 232, 186), seed=50 + i, ruw=0.12, golf=4)
        d = ImageDraw.Draw(img)
        for _ in range(max(1, int(rx / 6))):
            palm(d, cx + rnd.uniform(-rx * 0.6, rx * 0.6), cy + rnd.uniform(-ry * 0.3, ry * 0.5), rnd.uniform(5, 7))
        for _ in range(max(2, int(rx / 5))):
            lampion(d, cx + rnd.uniform(-rx * 0.7, rx * 0.7), cy + rnd.uniform(-ry * 0.6, ry * 0.6), 1.3)
    d = ImageDraw.Draw(img)
    # Lampionhaven: harbour roofs + a big lantern
    hx, hy = px(1846), py(-470)
    for dx in (-14, -4, 6):
        d.rectangle([hx + dx, hy + 2, hx + dx + 8, hy + 9], fill=(250, 214, 190))
        d.polygon([(hx + dx - 1, hy + 2), (hx + dx + 4, hy - 3), (hx + dx + 9, hy + 2)], fill=(236, 120, 110))
    # the lighthouse (Vuurtoreneiland)
    lx, ly = px(2400), py(-500)
    d.rectangle([lx - 2, ly - 12, lx + 2, ly], fill=(255, 250, 250))
    d.rectangle([lx - 2, ly - 8, lx + 2, ly - 6], fill=(236, 96, 110))
    d.ellipse([lx - 4, ly - 16, lx + 4, ly - 10], fill=(255, 230, 140))
    # a little kite (Vliegereiland)
    kx, ky = px(2300), py(-700)
    d.polygon([(kx, ky - 8), (kx + 5, ky - 2), (kx, ky + 4), (kx - 5, ky - 2)], fill=(250, 140, 190))
    d.line([(kx, ky + 4), (kx + 3, ky + 12)], fill=INKT)


def zone5(img, rnd):
    x0, y0, x1, y1 = doos("LAND_Z5")            # the real spot: the shadow
    fx0, fy0, fx1, fy1 = doos("LAND_Z5", 5)     # the floating island
    cx, cy, rx, ry = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2, (y1 - y0) / 2
    zachte_laag(img, lambda dd: dd.ellipse([cx - rx * 0.92, cy - ry * 0.7, cx + rx * 0.92, cy + ry * 0.9], fill=255), 9, (40, 60, 100, 85))
    fcx, fcy = (fx0 + fx1) / 2, (fy0 + fy1) / 2
    laag = Image.new("RGBA", img.size, (176, 160, 196, 0))
    d = ImageDraw.Draw(laag)
    # the rocky underside (a soft wedge under the island)
    d.polygon([(fcx - rx * 0.95, fcy + 2), (fcx - rx * 0.4, fcy + ry * 0.95), (fcx, fcy + ry * 1.25), (fcx + rx * 0.45, fcy + ry * 0.95),
               (fcx + rx * 0.95, fcy + 2)], fill=(176, 160, 196, 255))
    d.polygon([(fcx - rx * 0.6, fcy + ry * 0.4), (fcx, fcy + ry * 1.2), (fcx + rx * 0.3, fcy + ry * 0.7)], fill=(150, 134, 176, 255))
    d.polygon(blob_pts(fcx, fcy, rx, ry * 0.85, random.Random(61), ruw=0.07, golf=5), fill=(250, 248, 255, 255))
    d.polygon(blob_pts(fcx, fcy - 1, rx * 0.93, ry * 0.76, random.Random(61), ruw=0.07, golf=5), fill=(214, 236, 200, 255))
    img.alpha_composite(laag.filter(ImageFilter.GaussianBlur(0.8)))
    d = ImageDraw.Draw(img)
    # cloud puffs around the rim
    for i in range(22):
        a = 2 * math.pi * i / 22
        bx, by = fcx + math.cos(a) * rx * 0.98, fcy + math.sin(a) * ry * 0.82 + 2
        r = rnd.uniform(5, 9)
        d.ellipse([bx - r, by - r * 0.7, bx + r, by + r * 0.7], fill=(255, 255, 255))
    # the white-and-gold town: towers, the cathedral, the palace hill
    for _ in range(26):
        bx, by = fcx + rnd.uniform(-rx * 0.65, rx * 0.4), fcy + rnd.uniform(-ry * 0.45, ry * 0.45)
        h = rnd.uniform(5, 10)
        d.rectangle([bx - 2, by - h, bx + 2, by], fill=(255, 250, 240))
        d.polygon([(bx - 3, by - h), (bx, by - h - 4), (bx + 3, by - h)], fill=(240, 200, 110))
    kx, ky = px(2640), py(-520, 5)
    d.rectangle([kx - 6, ky - 14, kx + 6, ky], fill=(255, 252, 246))
    d.polygon([(kx - 7, ky - 14), (kx, ky - 26), (kx + 7, ky - 14)], fill=(244, 196, 96))
    # Staartvlakte (east): green tail meadow with a few trees
    for _ in range(12):
        bx, by = px(2985) + rnd.uniform(-18, 18), py(-520, 5) + rnd.uniform(-22, 22)
        if in_ellips(bx, by, fcx, fcy, rx * 0.9, ry * 0.75):
            boom(d, bx, by, rnd.uniform(5, 7), (150, 210, 150))


def zone6(img, rnd):
    x0, y0, x1, y1 = doos("LAND_Z6")
    # the mainland: from the west coast it runs off the east edge of the map
    r6 = random.Random(71)
    kust = []
    n = 40
    for i in range(n + 1):   # north coast, west -> east
        t = i / n
        kust.append((x0 + 18 + (W + 20 - x0) * t, y0 + 6 + math.sin(t * 9) * 5 + r6.uniform(-3, 3)))
    kust.append((W + 20, y1 - 6))
    for i in range(n, -1, -1):   # south coast, east -> west
        t = i / n
        kust.append((x0 + 18 + (W + 20 - x0) * t, y1 - 4 + math.sin(t * 7 + 1) * 6 + r6.uniform(-3, 3)))
    west = [(x0 + 6 + math.sin(k) * 6, y1 - (y1 - y0) * k / 8) for k in range(9)]
    pts = kust + west
    zachte_laag(img, lambda dd: dd.polygon(pts, fill=255), 7, ONDIEP + (200,))
    laag = Image.new("RGBA", img.size, (246, 226, 200, 0))
    d = ImageDraw.Draw(laag)
    d.polygon(pts, fill=(246, 226, 200, 255))
    shrink = [((x - (x0 + 300 * S)) * 0.985 + (x0 + 300 * S), (y - (y0 + y1) / 2) * 0.95 + (y0 + y1) / 2) for x, y in pts]
    d.polygon(shrink, fill=(244, 212, 206, 255))
    img.alpha_composite(laag.filter(ImageFilter.GaussianBlur(0.9)))
    d = ImageDraw.Draw(img)
    # Roze Duinen: soft pink dune arcs (south-west half)
    for _ in range(60):
        bx, by = rnd.uniform(px(3260), px(3830)), rnd.uniform(py(-790), py(-480))
        w = rnd.uniform(8, 14)
        d.arc([bx - w, by - 4, bx + w, by + 4], 190, 350, fill=(226, 160, 176), width=2)
    # De Palmenkloven (north-west): ravines and palms
    for _ in range(5):
        bx = rnd.uniform(px(3300), px(3800))
        by = rnd.uniform(py(-1040), py(-830))
        d.line([(bx - 14, by), (bx - 4, by + 3), (bx + 8, by - 2), (bx + 18, by + 2)], fill=(196, 150, 140), width=3)
    for _ in range(26):
        palm(d, rnd.uniform(px(3300), px(3830)), rnd.uniform(py(-1030), py(-820)), rnd.uniform(5, 7))
    # De Zoutvlakte (south-east): a white salt pan
    zachte_laag(img, lambda dd: dd.ellipse([px(3850), py(-730), px(4060), py(-470)], fill=255), 4, (252, 250, 246, 255))
    d = ImageDraw.Draw(img)
    for _ in range(18):
        bx, by = rnd.uniform(px(3880), px(4030)), rnd.uniform(py(-700), py(-500))
        d.line([(bx - 3, by), (bx + 3, by + 1)], fill=(228, 224, 232))
    # Het Ruinedal (north-east): grey ruins
    for _ in range(14):
        bx, by = rnd.uniform(px(3870), px(4170)), rnd.uniform(py(-1050), py(-910))
        h = rnd.uniform(3, 8)
        d.rectangle([bx - 2, by - h, bx + 2, by], fill=(190, 178, 186))
    # De Moederoase: a blue pond with palms
    ox, oy = px(4090), py(-760)
    d.ellipse([ox - 15, oy - 10, ox + 15, oy + 10], fill=(140, 210, 230))
    d.ellipse([ox - 9, oy - 6, ox + 4, oy + 1], fill=(190, 236, 246))
    for a in range(0, 360, 60):
        r = math.radians(a)
        palm(d, ox + math.cos(r) * 20, oy + math.sin(r) * 14 + 4, 6)
    # De Saffraanpoort: a saffron gate
    gx, gy = px(3762), py(-846)
    d.rectangle([gx - 12, gy - 12, gx - 6, gy + 4], fill=(240, 176, 80))
    d.rectangle([gx + 6, gy - 12, gx + 12, gy + 4], fill=(240, 176, 80))
    d.pieslice([gx - 12, gy - 20, gx + 12, gy - 4], 180, 360, fill=(240, 176, 80))
    d.pieslice([gx - 6, gy - 14, gx + 6, gy - 2], 180, 360, fill=(246, 226, 200))
    # the star-dousing tower (far east)
    tx, ty = px(4140), py(-688)
    d.rectangle([tx - 3, ty - 18, tx + 3, ty], fill=(150, 120, 170))
    d.polygon([(tx - 4, ty - 18), (tx, ty - 25), (tx + 4, ty - 18)], fill=(110, 90, 140))
    # silk ribbons in the wind (decoration)
    for _ in range(5):
        bx, by = rnd.uniform(px(3400), px(4100)), rnd.uniform(py(-900), py(-560))
        d.line([(bx, by), (bx + 6, by - 3), (bx + 12, by), (bx + 18, by - 3)], fill=(255, 190, 220), width=2)


def decor(img, rnd):
    """The empty south-east sea: a compass rose, a whale, little boats."""
    d = ImageDraw.Draw(img)
    cx, cy, r = W - 120, H - 110, 46
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=PAPIER, width=2)
    d.ellipse([cx - r * 0.7, cy - r * 0.7, cx + r * 0.7, cy + r * 0.7], outline=(220, 236, 244), width=1)
    for a, l, k in ((-90, 1.0, (242, 169, 196)), (90, 1.0, PAPIER), (0, 0.75, PAPIER), (180, 0.75, PAPIER),
                    (-45, 0.5, (220, 236, 244)), (45, 0.5, (220, 236, 244)), (135, 0.5, (220, 236, 244)), (-135, 0.5, (220, 236, 244))):
        t = math.radians(a)
        q = math.radians(a + 90)
        d.polygon([(cx + math.cos(t) * r * l, cy + math.sin(t) * r * l), (cx + math.cos(q) * 6, cy + math.sin(q) * 6),
                   (cx - math.cos(q) * 6, cy - math.sin(q) * 6)], fill=k)
    d.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], fill=INKT)
    # a pastel whale
    wx, wy = W - 330, H - 70
    d.ellipse([wx - 26, wy - 10, wx + 20, wy + 10], fill=(170, 186, 230))
    d.polygon([(wx + 16, wy - 2), (wx + 34, wy - 12), (wx + 30, wy + 2), (wx + 34, wy + 10)], fill=(170, 186, 230))
    d.ellipse([wx - 16, wy - 4, wx - 12, wy], fill=INKT)
    d.arc([wx - 20, wy - 22, wx - 8, wy - 10], 200, 340, fill=(230, 244, 252), width=2)
    # little boats
    for bx, by in ((620, 560), (900, 420), (380, 640), (1050, 560)):
        d.polygon([(bx - 7, by), (bx + 7, by), (bx + 4, by + 4), (bx - 4, by + 4)], fill=(190, 140, 110))
        d.polygon([(bx, by - 11), (bx, by - 1), (bx + 7, by - 2)], fill=(255, 250, 240))
    # a few fish
    for _ in range(8):
        fx, fy = rnd.uniform(500, W - 40), rnd.uniform(360, H - 30)
        d.ellipse([fx - 3, fy - 1.5, fx + 3, fy + 1.5], fill=(196, 226, 240))


def lijst(img):
    rand = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rand)
    for i in range(14):
        a = int(170 * (1 - i / 14))
        rd.rectangle([i, i, W - 1 - i, H - 1 - i], outline=PAPIER + (a,))
    rd.rectangle([0, 0, W - 1, H - 1], outline=INKT + (255,), width=2)
    img.alpha_composite(rand)


def maak():
    rnd = random.Random(1103)
    img = Image.new("RGBA", (W, H), ZEE + (255,))
    img = zee(img, rnd)
    decor(img, rnd)
    kamp(img, random.Random(1))
    zone1(img, random.Random(2))
    zone2(img, random.Random(3))
    zone3(img, random.Random(4))
    zone4(img, random.Random(5))
    zone6(img, random.Random(7))
    zone5(img, random.Random(6))   # last: it floats above the east of zone 4
    lijst(img)
    return img


# ---- the preview mock (not used by the game) ------------------------------------------------------------------------------------
def mock(img, uit):
    """A rough simulation of ReiskaartScherm's overlay: zones 1-3 open, 4-6 closed (mist + "?"), labels, campfires, you."""
    import json
    nl = json.load(open(os.path.join("src", "main", "resources", "assets", "guhs", "lang", "nl_nl.json"), encoding="utf-8"))
    m = img.copy()
    dicht = {4, 5, 6}
    laag = Image.new("RGBA", m.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(laag)
    for p in C["plekken"]:
        if p["zone"] in dicht:
            x0, z0, x1, z1 = p["land"]
            ax0, ay0, ax1, ay1 = px(x0), py(z0, p["zone"]), px(x1), py(z1, p["zone"])
            cx, cy = (ax0 + ax1) / 2, (ay0 + ay1) / 2
            rx, ry = (ax1 - ax0) / 2 * 1.12 + 6, (ay1 - ay0) / 2 * 1.15 + 6
            d.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=(0x74, 0x6E, 0x80, 0x8A))
            d.ellipse([cx - rx * 0.8, cy - ry * 0.8, cx + rx * 0.8, cy + ry * 0.8], fill=(0x85, 0x7F, 0x90, 0x50))
    m.alpha_composite(laag)
    d = ImageDraw.Draw(m)
    try:
        f = ImageFont.truetype("arial.ttf", 9)
        fb = ImageFont.truetype("arialbd.ttf", 9)
        fq = ImageFont.truetype("arialbd.ttf", 16)
    except OSError:
        f = fb = fq = ImageFont.load_default()

    def B(p):
        return px(p["x"]), py(p["z"], p["zone"])

    pos = {p["id"]: B(p) for p in C["plekken"]}
    routes = [("knuffelkamp", "zwamzoet"), ("zwamzoet", "kandijkom"), ("kandijkom", "schemerwortel"), ("schemerwortel", "lampionkust"),
              ("lampionkust", "hoogvads"), ("hoogvads", "zijdeland")]
    for a, b in routes:
        (x0, y0), (x1, y1) = pos[a], pos[b]
        open_ = a not in ("lampionkust", "hoogvads", "zijdeland") and b not in ("lampionkust", "hoogvads", "zijdeland")
        l = math.hypot(x1 - x0, y1 - y0)
        nx, ny, bocht = -(y1 - y0) / l, (x1 - x0) / l, l * 0.12
        s = 26
        while s < l - 26:
            t = s / l
            boog = math.sin(t * math.pi) * bocht
            x, y = x0 + (x1 - x0) * t + nx * boog, y0 + (y1 - y0) * t + ny * boog
            d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=(255, 255, 255) if open_ else (224, 220, 230))
            s += 8
    for n in C["namen"]:
        if n["zone"] in dicht:
            continue
        t = nl.get("vadsrijk.kern.gebied." + n["id"], n["id"])
        x, y = px(n["x"]), py(n["z"], n["zone"])
        w = d.textlength(t, font=f)
        d.text((x - w / 2 + 1, y - 4), t, font=f, fill=(255, 251, 244))
        d.text((x - w / 2, y - 5), t, font=f, fill=(62, 48, 72))
    for vx, vz in ((-384, 1040), (40, 80), (-300, 200), (300, -100), (-420, -400), (420, -500), (640, -1100), (900, -1000), (1300, -700)):
        x, y = px(vx), py(vz)
        d.rectangle([x - 4, y, x + 3, y + 1], fill=INKT)
        d.rectangle([x - 2, y - 3, x + 1, y - 1], fill=(255, 122, 58))
        d.rectangle([x - 1, y - 6, x, y - 4], fill=(255, 154, 64))
        d.rectangle([x - 1, y - 2, x, y - 1], fill=(255, 224, 112))
    for p in C["plekken"]:
        x, y = pos[p["id"]]
        dicht_ = p["zone"] in dicht
        hier = p["id"] == "kandijkom"
        if hier:
            d.ellipse([x - 17, y - 17, x + 17, y + 17], fill=(255, 213, 74))
        d.ellipse([x - 12, y - 12, x + 12, y + 12], fill=(255, 213, 74) if hier else (154, 147, 162) if dicht_ else (242, 169, 196))
        d.ellipse([x - 10, y - 10, x + 10, y + 10], fill=(110, 104, 120) if dicht_ else (255, 246, 236))
        if dicht_:
            d.text((x - 4, y - 9), "?", font=fq, fill=(255, 255, 255))
        else:
            d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=(242, 120, 160))
        naam = nl.get("vadsrijk.npc.reiskaart.plek." + p["id"], p["id"])
        w = d.textlength(naam, font=fb)
        d.rectangle([x - w / 2 - 3, y + 14, x + w / 2 + 3, y + 25], fill=(64, 56, 72) if dicht_ else (255, 246, 236))
        d.text((x - w / 2, y + 15), naam, font=fb, fill=(216, 210, 222) if dicht_ else INKT)
        if hier:
            t = "Jij bent hier"
            d.text((x - d.textlength(t, font=fb) / 2, y - 27), t, font=fb, fill=(255, 213, 74))
    # you: an arrow near Kandijkom looking north-east
    x, y = px(760), py(-1000)
    d.ellipse([x - 6, y - 6, x + 6, y + 6], fill=(255, 255, 255))
    d.ellipse([x - 5, y - 5, x + 5, y + 5], fill=INKT)
    d.polygon([(x + 3, y - 4), (x - 3, y + 1), (x + 1, y + 1)], fill=(255, 213, 74))
    d.polygon([(x + 4, y - 5), (x - 4, y + 2), (x + 3, y + 3)], fill=(255, 213, 74))
    m.convert("RGB").save(uit)


if __name__ == "__main__":
    kaart = maak()
    os.makedirs(os.path.dirname(UIT), exist_ok=True)
    kaart.convert("RGB").save(UIT, optimize=True)
    print("reiskaart:", UIT, kaart.size, os.path.getsize(UIT) // 1024, "KB")
    if "--voorbeeld" in sys.argv:
        d = sys.argv[sys.argv.index("--voorbeeld") + 1]
        os.makedirs(d, exist_ok=True)
        kaart.convert("RGB").save(os.path.join(d, "reiskaart_kaal.png"))
        mock(kaart, os.path.join(d, "reiskaart_mock.png"))
        print("voorbeeld:", d)
