"""
De Knabbelbakkerij (2.8) - its pixel art, drawn with a tiny mask painter (shape -> fill, light from the top left, a dark
outline like the vanilla items): the twelve pastries, the feesttaart and the bakmunt (items), the knabbeloven and the
chimney pot (blocks), the knabbelwolkje and meelstofje particles, the baking screen's button sheet and the order bubble.
"""
import math
import random

import numpy as np
from PIL import Image

# ---------------------------------------------------------------------------------------------------------------------
# a tiny painter
# ---------------------------------------------------------------------------------------------------------------------


class Doek:
    def __init__(self, w=16, h=16):
        self.w, self.h = w, h
        self.a = np.zeros((h, w, 4), np.uint8)

    def px(self, x, y, c):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.w and 0 <= y < self.h:
            self.a[y, x] = (*c[:3], c[3] if len(c) > 3 else 255)

    def get(self, x, y):
        return self.a[y, x]

    def mask(self, fn):
        return np.array([[bool(fn(x + 0.5, y + 0.5)) for x in range(self.w)] for y in range(self.h)])

    def vul(self, m, base, light=None, dark=None, outline=None, seed=0, noise=0):
        """Fill mask m with base; highlight the top-left edge, shade the bottom-right, a dark outline around it."""
        rng = random.Random(seed)
        light = light or tuple(min(255, int(v * 1.18 + 12)) for v in base)
        dark = dark or tuple(int(v * 0.72) for v in base)
        for y in range(self.h):
            for x in range(self.w):
                if not m[y, x]:
                    continue
                up = y == 0 or not m[y - 1, x]
                lf = x == 0 or not m[y, x - 1]
                dn = y == self.h - 1 or not m[y + 1, x]
                rt = x == self.w - 1 or not m[y, x + 1]
                c = base
                if dn or rt:
                    c = dark
                elif up or lf:
                    c = light
                if noise:
                    k = rng.randint(-noise, noise)
                    c = tuple(max(0, min(255, v + k)) for v in c)
                self.px(x, y, c)
        if outline:
            self.omlijn(m, outline)
        return m

    def omlijn(self, m, colour):
        for y in range(self.h):
            for x in range(self.w):
                if m[y, x]:
                    continue
                if any(0 <= x + dx < self.w and 0 <= y + dy < self.h and m[y + dy, x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    self.px(x, y, colour)

    def gezichtje(self, cx, cy, gap=4, blush=True):
        """A tiny guh face: two dark eyes with a shine, pink blush under them."""
        for ex in (cx - gap / 2, cx + gap / 2 - 1):
            self.px(ex, cy, (40, 22, 38))
            self.px(ex, cy + 1, (40, 22, 38))
        if blush:
            self.px(cx - gap / 2 - 1, cy + 2, (244, 120, 158))
            self.px(cx + gap / 2, cy + 2, (244, 120, 158))
        self.px(cx - 0.5, cy + 2, (140, 50, 70))

    def img(self):
        return Image.fromarray(self.a)


CRUST = (214, 146, 60)
CRUST_OUT = (98, 50, 22)
CHEESE = (252, 206, 72)
PINK = (244, 138, 184)
PINK_OUT = (150, 50, 96)
SUGAR = (252, 250, 244)
CREAM = (250, 236, 200)


def ellipse(cx, cy, rx, ry):
    return lambda x, y: ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1


def circle(cx, cy, r):
    return ellipse(cx, cy, r, r)


def star(cx, cy, r_out, r_in, points=5, rot=-math.pi / 2):
    pts = []
    for i in range(points * 2):
        r = r_out if i % 2 == 0 else r_in
        a = rot + i * math.pi / points
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))

    def inside(x, y):
        n, c = len(pts), False
        j = n - 1
        for i in range(n):
            (xi, yi), (xj, yj) = pts[i], pts[j]
            if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
                c = not c
            j = i
        return c
    return inside


# ---------------------------------------------------------------------------------------------------------------------
# the pastries (16 x 16 items)
# ---------------------------------------------------------------------------------------------------------------------

def knabbelbroodje():
    d = Doek()
    m = d.vul(d.mask(ellipse(8, 9.5, 6.8, 5.2)), CRUST, outline=CRUST_OUT, seed=1)
    for i, x0 in enumerate((5, 8, 11)):                   # score cuts with cheese in them
        for k in range(3):
            d.px(x0 + k - 1, 6 + k * 0.5 + (i % 2) * 0.5, CHEESE)
    for (x, y) in ((4, 9), (12, 8), (7, 12), (10, 11), (3, 11)):
        d.px(x, y, (230, 110, 30))                          # knabbel crumbs
    d.gezichtje(8, 9.5)
    return d.img()


def kaaskrakeling():
    d = Doek()
    ring = lambda cx, cy, r, t: (lambda x, y: abs(math.dist((x, y), (cx, cy)) - r) <= t)
    m1, m2 = ring(5.3, 8, 3.4, 1.3), ring(10.7, 8, 3.4, 1.3)
    band = lambda x, y: 11 <= y <= 13.4 and 3 <= x <= 13 and abs(x - 8) >= abs(y - 12.2) * 0.2
    m = d.mask(lambda x, y: m1(x, y) or m2(x, y) or band(x, y))
    d.vul(m, (222, 158, 70), outline=CRUST_OUT, seed=2)
    for (x, y) in ((3, 6), (6, 5), (9, 5), (12, 6), (4, 12), (8, 12), (11, 12), (13, 9), (2, 9)):
        if m[y, x]:
            d.px(x, y, SUGAR)
    return d.img()


def vadsvlaai():
    d = Doek()
    outer = ellipse(8, 9, 7.4, 5.6)
    inner = ellipse(8, 8.8, 5.6, 3.9)
    m = d.mask(outer)
    d.vul(m, CRUST, outline=CRUST_OUT, seed=3)
    mi = d.mask(inner)
    d.vul(mi, CHEESE, light=(255, 236, 140), dark=(226, 170, 40), seed=4)
    for y in range(16):                                    # a crust lattice over the cheese
        for x in range(16):
            if mi[y, x] and (x + y) % 4 == 0:
                d.px(x, y, (200, 128, 50))
    d.px(7, 8, (40, 22, 38))
    d.px(9, 8, (40, 22, 38))
    return d.img()


def guhcroissant():
    d = Doek()
    m = d.mask(lambda x, y: circle(8, 9, 6.6)(x, y) and not circle(8, 3.2, 5.0)(x, y))
    d.vul(m, (224, 150, 58), outline=CRUST_OUT, seed=5)
    for x in (4, 7, 9, 12):                                # the rolled segments
        for y in range(16):
            if m[y, x] and y > 8:
                d.px(x, y, (170, 98, 36))
    for (x, y) in ((5, 10), (8, 12), (11, 10), (6, 13), (10, 13)):
        d.px(x, y, CHEESE)
    d.gezichtje(8, 10.5, gap=6)
    return d.img()


def knabbelkoekje():
    d = Doek()
    m = d.mask(circle(8, 8.5, 6.4))
    d.vul(m, (222, 176, 110), outline=CRUST_OUT, seed=6)
    for (x, y) in ((5, 6), (10, 5), (7, 9), (11, 10), (5, 11), (9, 12)):
        d.px(x, y, (200, 100, 30))
        d.px(x + 1, y, (170, 80, 20))
    for (x, y) in ((6, 4), (12, 7), (4, 9), (8, 7), (10, 13), (7, 13)):
        d.px(x, y, SUGAR)
    return d.img()


def kaasbolletje():
    d = Doek()
    m = d.mask(ellipse(8, 9.8, 6.2, 5.2))
    d.vul(m, CRUST, outline=CRUST_OUT, seed=7)
    cap = d.mask(lambda x, y: ellipse(8, 9.8, 6.2, 5.2)(x, y) and y < 8.2 + 1.4 * math.sin(x * 1.3))
    d.vul(cap, CHEESE, light=(255, 238, 150), dark=(228, 172, 40), seed=8)
    for x in (5, 9, 12):                                   # drips
        for y in range(8, 11):
            if m[y, x] and not cap[y, x] and y < 8 + (x % 3) + 1:
                d.px(x, y, CHEESE)
    d.gezichtje(8, 10.5)
    return d.img()


def pluismuffin():
    d = Doek()
    cup = d.mask(lambda x, y: 9 <= y <= 14.5 and abs(x - 8) <= 4.2 + (y - 9) * -0.25)
    d.vul(cup, (250, 250, 250), outline=PINK_OUT, seed=9)
    for y in range(16):
        for x in range(16):
            if cup[y, x] and x % 2 == 0:
                d.px(x, y, PINK)
    top = d.mask(lambda x, y: ellipse(8, 7.5, 6, 4.2)(x, y) or circle(4, 4, 1.6)(x, y) or circle(12, 4, 1.6)(x, y))
    d.vul(top, (248, 164, 200), outline=PINK_OUT, seed=10)
    d.px(4, 4, (255, 210, 225))
    d.px(12, 4, (255, 210, 225))
    d.gezichtje(8, 7)
    return d.img()


def theetaartje():
    d = Doek()
    base = d.mask(lambda x, y: 8 <= y <= 13.5 and abs(x - 8) <= 6.2)
    d.vul(base, CRUST, outline=CRUST_OUT, seed=11)
    for x in range(2, 15, 2):                              # scalloped edge
        d.px(x, 13, (190, 118, 44))
    top = d.mask(ellipse(8, 8, 6.3, 2.4))
    d.vul(top, SUGAR, light=(255, 255, 255), dark=(226, 222, 214), seed=12)
    heart = [(7, 6), (9, 6), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (7, 8), (8, 8), (9, 8), (8, 9)]
    for (x, y) in heart:
        d.px(x, y - 1, (230, 60, 110))
    d.omlijn(d.mask(lambda x, y: base[int(y), int(x)] or top[int(y), int(x)]), CRUST_OUT)
    return d.img()


def knabbeltompouce():
    d = Doek()
    whole = d.mask(lambda x, y: 4 <= y <= 13.5 and 2 <= x <= 14)
    d.vul(whole, CRUST, outline=CRUST_OUT, seed=13)
    for x in range(2, 15):
        for y in (4, 5):
            d.px(x, y, (246, 140, 186) if y == 5 else (255, 190, 214))       # pink glaze
        for y in (8, 9, 10):
            d.px(x, y, CREAM if y != 10 else (240, 214, 150))               # custard
        d.px(x, 7, (200, 128, 50))
        d.px(x, 11, (200, 128, 50))
    d.gezichtje(8, 4.5, gap=4, blush=False)
    return d.img()


def vadsdonut():
    d = Doek()
    m = d.mask(lambda x, y: circle(8, 8.5, 6.6)(x, y) and not circle(8, 8.5, 2.2)(x, y))
    d.vul(m, CRUST, outline=CRUST_OUT, seed=14)
    glaze = d.mask(lambda x, y: circle(8, 8.2, 5.7)(x, y) and not circle(8, 8.5, 2.8)(x, y) and (y < 11 or abs(x - 8) > 3 or (x % 3 == 0)))
    d.vul(glaze, PINK, light=(255, 196, 220), dark=(214, 106, 156), seed=15)
    for (x, y), c in zip(((4, 6), (7, 4), (11, 5), (12, 9), (4, 10), (10, 11)),
                         ((255, 250, 120), (120, 220, 255), (255, 255, 255), (150, 240, 160), (255, 255, 255), (255, 250, 120))):
        d.px(x, y, c)
    return d.img()


def guhwafel():
    d = Doek()
    m = d.mask(lambda x, y: circle(8, 8.5, 6.5)(x, y))
    d.vul(m, (228, 170, 80), outline=CRUST_OUT, seed=16)
    for y in range(16):
        for x in range(16):
            if m[y, x] and (x % 3 == 1 or y % 3 == 1):
                d.px(x, y, (186, 116, 40))
    for (x, y) in ((6, 5), (10, 6), (7, 10), (11, 11), (4, 8), (9, 8)):
        d.px(x, y, SUGAR)
    return d.img()


def sterrenkoekje():
    d = Doek()
    m = d.mask(star(8, 8.8, 7.3, 3.2))
    d.vul(m, (232, 180, 110), outline=CRUST_OUT, seed=17)
    inner = d.mask(star(8, 8.8, 5.2, 2.2))
    d.vul(inner, PINK, light=(255, 200, 224), dark=(222, 116, 164), seed=18)
    d.px(7, 8, (40, 22, 38))
    d.px(9, 8, (40, 22, 38))
    d.px(12, 3, (255, 255, 200))
    d.px(3, 12, (255, 255, 200))
    return d.img()


def feesttaart():
    d = Doek()
    tiers = [((8, 12.2), 6.8, 2.6, (246, 150, 190)), ((8, 8.6), 5.0, 2.0, (252, 248, 240)), ((8, 5.6), 3.2, 1.6, (250, 206, 90))]
    for (cx, cy), w, hh, col in tiers:
        m = d.mask(lambda x, y, cx=cx, cy=cy, w=w, hh=hh: abs(x - cx) <= w and abs(y - cy) <= hh)
        d.vul(m, col, outline=PINK_OUT, seed=int(cy))
        for x in range(int(cx - w) + 1, int(cx + w) + 1, 2):
            d.px(x, cy - hh + 0.5, (255, 255, 255))
    d.px(8, 2, (255, 230, 90))                             # the candle and its flame
    d.px(8, 3, (130, 200, 250))
    d.px(8, 1, (255, 150, 60))
    d.gezichtje(8, 12, gap=6)
    return d.img()


def bakmunt():
    d = Doek()
    m = d.mask(circle(8, 8, 6.9))
    d.vul(m, (246, 196, 62), light=(255, 234, 140), dark=(196, 140, 30), outline=(120, 74, 16), seed=19)
    loaf = d.mask(lambda x, y: ellipse(8, 8.6, 3.8, 2.8)(x, y))
    d.vul(loaf, (214, 140, 50), light=(236, 176, 80), dark=(170, 100, 30), seed=20)
    for x in (6, 8, 10):
        d.px(x, 7, (252, 220, 120))
    d.px(7, 9, (60, 30, 20))
    d.px(9, 9, (60, 30, 20))
    return d.img()


ITEMS = {"knabbelbroodje": knabbelbroodje, "kaaskrakeling": kaaskrakeling, "vadsvlaai": vadsvlaai, "guhcroissant": guhcroissant,
         "knabbelkoekje": knabbelkoekje, "kaasbolletje": kaasbolletje, "pluismuffin": pluismuffin, "theetaartje": theetaartje,
         "knabbeltompouce": knabbeltompouce, "vadsdonut": vadsdonut, "guhwafel": guhwafel, "sterrenkoekje": sterrenkoekje,
         "feesttaart": feesttaart, "bakmunt": bakmunt}


# ---------------------------------------------------------------------------------------------------------------------
# blocks
# ---------------------------------------------------------------------------------------------------------------------

def _steen(seed, base=(236, 150, 160), mortar=(250, 214, 216)):
    """Pink oven bricks."""
    rng = random.Random(seed)
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 4 if row % 2 else 0
            c = mortar if y % 4 == 3 or (x + off) % 8 == 7 else base
            k = rng.randint(-8, 8)
            a[y, x] = (*[max(0, min(255, v + k)) for v in c], 255)
    return a


def oven_voor(aan):
    a = _steen(31)
    d = Doek()
    d.a = a
    # the round oven door (the mouth): dark iron with a window that glows when baking
    door = d.mask(lambda x, y: (y >= 8 and abs(x - 8) <= 4.4) and (y >= 10 or math.dist((x, y), (8, 10)) <= 4.4))
    d.vul(door, (70, 52, 60), light=(96, 76, 84), dark=(48, 34, 40))
    win = d.mask(lambda x, y: math.dist((x, y), (8, 11.5)) <= 2.2)
    d.vul(win, (255, 170, 60) if aan else (60, 40, 46), light=(255, 230, 120) if aan else (80, 60, 66), dark=(230, 110, 40) if aan else (40, 28, 32))
    d.px(8, 15, (230, 190, 90))                            # the handle
    # the face: eyes above the door, blush
    for ex in (4, 11):
        d.px(ex, 4, (40, 22, 38))
        d.px(ex + 1, 4, (40, 22, 38))
        d.px(ex, 5, (40, 22, 38))
        d.px(ex + 1, 5, (40, 22, 38))
        d.px(ex, 4, (250, 250, 250))
    d.px(2, 6, (244, 110, 150))
    d.px(3, 6, (244, 110, 150))
    d.px(12, 6, (244, 110, 150))
    d.px(13, 6, (244, 110, 150))
    return d.img()


def oven_zijkant():
    a = _steen(32)
    for x in range(16):
        a[0, x] = (250, 214, 216, 255)
    return Image.fromarray(a)


def oven_boven():
    a = _steen(33, base=(226, 140, 150))
    return Image.fromarray(a)


def oven_pijp():
    rng = random.Random(34)
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            k = rng.randint(-10, 10)
            a[y, x] = (150 + k, 140 + k, 150 + k, 255) if x % 4 else (110 + k, 100 + k, 112 + k, 255)
    return Image.fromarray(a)


def oven_oor():
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            a[y, x] = (246, 150, 180, 255) if 4 <= x <= 11 and 4 <= y <= 11 else (236, 120, 150, 255)
    return Image.fromarray(a)


def pot_zijkant():
    a = _steen(35, base=(196, 100, 70), mortar=(228, 186, 160))
    d = Doek()
    d.a = a
    d.px(5, 6, (40, 22, 38))
    d.px(10, 6, (40, 22, 38))
    d.px(4, 8, (244, 120, 150))
    d.px(11, 8, (244, 120, 150))
    d.px(7, 8, (120, 40, 50))
    d.px(8, 8, (120, 40, 50))
    return d.img()


def pot_boven():
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            r = math.dist((x + 0.5, y + 0.5), (8, 8))
            a[y, x] = (40, 30, 34, 255) if r < 4.5 else (206, 110, 80, 255) if r < 7 else (180, 90, 64, 255)
    return Image.fromarray(a)


# ---------------------------------------------------------------------------------------------------------------------
# particles, the button sheet, the order bubble
# ---------------------------------------------------------------------------------------------------------------------

def wolkje(seed):
    """A little cloud shaped like a curly kaasknabbel (a cheese puff): cream, a bit of yellow, soft edges."""
    rng = random.Random(seed)
    d = Doek(8, 8)
    blobs = [(3, 4.5, 2.4), (5.2, 3.6, 2.0), (4.2, 2.6, 1.7)] if seed % 2 else [(2.8, 3.8, 2.2), (5, 4.4, 2.3), (4.6, 2.4, 1.6)]
    m = d.mask(lambda x, y: any(math.dist((x, y), (bx, by)) <= r for bx, by, r in blobs))
    d.vul(m, (255, 246, 220), light=(255, 255, 250), dark=(250, 214, 140))
    for _ in range(3):
        x, y = rng.randint(2, 5), rng.randint(2, 5)
        if m[y, x]:
            d.px(x, y, (252, 206, 90))
    d.omlijn(m, (236, 190, 120, 160))
    return d.img()


def meel(seed):
    rng = random.Random(seed)
    d = Doek(4, 4)
    for _ in range(3):
        d.px(rng.randint(0, 3), rng.randint(0, 3), (255, 255, 250, 230))
    d.px(1, 1, (255, 255, 255, 255))
    return d.img()


def knoppen():
    """The baking screen's icons: row 0 the doughs, row 1 the shapes, row 2 the toppings (16 x 16 each)."""
    sheet = Image.new("RGBA", (128, 64), (0, 0, 0, 0))

    def cel(col, row, d):
        sheet.paste(d.img(), (col * 16, row * 16))

    doughs = [((226, 180, 110), [(5, 6), (9, 9), (11, 6)], (200, 100, 30)), ((248, 208, 90), [], None),
              ((248, 176, 200), [(6, 6), (10, 8)], (255, 255, 255)), ((244, 224, 176), [], None)]
    for i, (col, dots, dc) in enumerate(doughs):
        d = Doek()
        if i == 3:                                          # bladerdeeg: layers
            m = d.mask(lambda x, y: 4 <= y <= 12 and 2 <= x <= 13)
            d.vul(m, col, outline=CRUST_OUT)
            for y in (6, 9):
                for x in range(2, 14):
                    d.px(x, y, (220, 190, 130))
        else:
            m = d.mask(ellipse(8, 9, 6, 5))
            d.vul(m, col, outline=CRUST_OUT)
            for (x, y) in dots:
                d.px(x, y, dc)
        cel(i, 0, d)
    dough = (236, 196, 128)
    shapes = [circle(8, 8.5, 5.5),
              lambda x, y: any(math.dist((x, y), (cx, cy)) <= 2.1 for cx, cy in ((4, 4), (6, 6.5), (8, 9), (10, 11.5), (12, 13), (11, 4), (9.5, 6.5), (6.5, 11.5), (5, 13))),
              lambda x, y: 7 <= y <= 13 and abs(x - 8) <= 6 or ellipse(8, 7, 6, 2)(x, y),
              lambda x, y: circle(8, 9, 6)(x, y) and not circle(8, 4, 4.6)(x, y),
              lambda x, y: 6 <= y <= 11 and 2 <= x <= 14,
              lambda x, y: circle(8, 8.5, 6)(x, y) and not circle(8, 8.5, 2.2)(x, y),
              star(8, 8.8, 7, 3)]
    for i, fn in enumerate(shapes):
        d = Doek()
        d.vul(d.mask(fn), dough, outline=CRUST_OUT)
        cel(i, 1, d)
    toppings = [(lambda x, y: ellipse(8, 11, 6, 3.5)(x, y) and y > 7, (250, 200, 60), [(5, 9), (9, 8), (11, 10)], (255, 236, 140)),
                (lambda x, y: ellipse(8, 11, 6, 3.5)(x, y) and y > 7, (250, 250, 246), [(6, 9), (10, 9)], (220, 220, 230)),
                (lambda x, y: (4 <= y <= 8 and 2 <= x <= 13) or (8 < y <= 13 and int(x) in (4, 5, 8, 11, 12) and y < 9 + (int(x) * 3) % 5), (246, 140, 186),
                 [(5, 5), (10, 6)], (255, 200, 225)),
                (lambda x, y: any(math.dist((x, y), (cx, cy)) <= 1.4 for cx, cy in ((4, 10), (7, 12), (10, 9), (12, 12), (8, 7), (5, 6))),
                 (206, 120, 44), [], None)]
    for i, (fn, col, dots, dc) in enumerate(toppings):
        d = Doek()
        d.vul(d.mask(fn), col, outline=(90, 50, 30))
        for (x, y) in dots:
            d.px(x, y, dc)
        cel(i, 2, d)
    return sheet


def bubbel():
    """The order bubble (32 x 32): a white speech bubble in 24 x 24 (body rows 0..19, the tail below), and a white
    corner at 28..31 for the patience bar."""
    a = np.zeros((32, 32, 4), np.uint8)
    for y in range(20):
        for x in range(24):
            r = 4
            cx = min(max(x, r), 23 - r)
            cy = min(max(y, r), 19 - r)
            dd = math.dist((x + 0.5, y + 0.5), (cx + 0.5, cy + 0.5))
            if dd <= r + 0.5:
                edge = dd > r - 0.6 or x in (0, 23) or y in (0, 19)
                a[y, x] = (236, 120, 170, 255) if edge else (255, 252, 248, 240)
    for y in range(20, 24):                                 # the tail, pointing down
        w = 23 - y
        for x in range(12 - w // 2 - 1, 12 + w // 2 + 1):
            if 0 <= x < 24:
                a[y, x] = (236, 120, 170, 255) if x in (12 - w // 2 - 1, 12 + w // 2) or y == 23 else (255, 252, 248, 240)
    for y in range(19, 21):
        for x in range(10, 14):
            a[y, x] = (255, 252, 248, 240)
    a[28:32, 28:32] = (255, 255, 255, 255)
    return Image.fromarray(a)


def build(h):
    for name, fn in ITEMS.items():
        h.save(fn(), "item", f"{name}.png")
    h.save(oven_voor(False), "block", "knabbeloven_voor.png")
    h.save(oven_voor(True), "block", "knabbeloven_voor_aan.png")
    h.save(oven_zijkant(), "block", "knabbeloven_zijkant.png")
    h.save(oven_boven(), "block", "knabbeloven_boven.png")
    h.save(oven_pijp(), "block", "knabbeloven_pijp.png")
    h.save(oven_oor(), "block", "knabbeloven_oor.png")
    h.save(pot_zijkant(), "block", "bakkerij_schoorsteen_zijkant.png")
    h.save(pot_boven(), "block", "bakkerij_schoorsteen_boven.png")
    for i in range(3):
        h.save(wolkje(i), "particle", f"knabbelwolkje_{i}.png")
    for i in range(2):
        h.save(meel(i), "particle", f"meelstofje_{i}.png")
    h.save(knoppen(), "gui", "bakkerij_knoppen.png")
    h.save(bubbel(), "entity", "bakkerij_bubbel.png")
