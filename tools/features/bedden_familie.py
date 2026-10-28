"""
The family beds of 1.8.0 (the renders dromen-ontwerp/bedden/f01..f07, script dromen-ontwerp/_bron/bedden/familie.py) and the
Knuffelnest (the big bed for eight guhs), as boxes (blocks, in the render's room coordinates) and vakjes (where a critter lies).
tools/features/bedden.py turns them into a block model per cell, the block states, and FamilieVormen.java (cells, shapes,
vakjes). The geometry follows the renders' doos() calls one by one; the critters become vakjes.
"""
import math

# a bed: dict(id, nl, en, lore_nl, lore_en, boxes=[(x0, y0, z0, x1, y1, z1, tex, solid, face)], vakjes=[(x, y, z, soort)])


class Bed:
    def __init__(self, id_):
        self.id = id_
        self.boxes = []
        self.vakjes = []

    def doos(self, x0, y0, z0, x1, y1, z1, kleur, *_, solid=True, tex=None, face=None):
        self.boxes.append((min(x0, x1), min(y0, y1), min(z0, z1), max(x0, x1), max(y0, y1), max(z0, z1),
                           tex or kleurnaam(kleur), solid, face))

    def deken(self, x0, y, z0, x1, z1, kleur, dik=0.08):
        self.doos(x0, y, z0, x1, y + dik, z1, kleur, solid=False)

    def ruit(self, x0, y, z0, x1, z1, k1, k2):
        self.doos(x0, y, z0, x1, y + 0.04, z1, k1, solid=False, tex=f"ruit_{hexk(k1)}_{hexk(k2)}")

    def schelp(self, cx, y, cz, r=0.45):
        self.doos(cx - r, y, cz - r * 0.9, cx + r, y + 0.1, cz + r, (255, 190, 200), solid=False, tex="schelp")
        self.doos(cx - r * 0.8, y + 0.1, cz - r, cx + r * 0.8, y + 0.55, cz - r * 0.8, (255, 218, 224), solid=False, tex="schelp")

    def ster(self, x, y, z, s=0.1, vlak="y"):
        if vlak == "y":
            self.doos(x - s, y, z - s, x + s, y + 0.02, z + s, (255, 226, 120), solid=False, tex="ster")
        else:
            self.doos(x - s, y - s, z, x + s, y + s, z + 0.02, (255, 226, 120), solid=False, tex="ster")

    def hartje(self, x, y, z, s=0.12, kleur=(255, 140, 180), vlak="y"):
        if vlak == "y":
            self.doos(x - s, y, z - s, x + s, y + 0.02, z + s, kleur, solid=False, tex="hartje")
        else:
            self.doos(x - s, y - s, z, x + s, y + s, z + 0.02, kleur, solid=False, tex="hartje")

    def guhgezicht(self, x0, y0, x1, y1, z):
        self.doos(x0, y0, z, x1, y1, z + 0.15, (255, 182, 204), tex="gezicht", face="south")

    def pootjes(self, x0, z0, x1, z1, y, h, kleur, d=0.14):
        for xx in (x0, x1 - d):
            for zz in (z0, z1 - d):
                self.doos(xx, y, zz, xx + d, y + h, zz + d, kleur)

    def vak(self, x, y, z, soort):
        self.vakjes.append((x, y, z, soort))


def hexk(k):
    return "%02x%02x%02x" % tuple(k)


def kleurnaam(k):
    return "k" + hexk(k)


def f01():
    b = Bed("guh_hoofdbank")
    x0, x1, z0, z1, y = 1.4, 6.6, 1.3, 3.4, 1
    roze, rand = (255, 196, 216), (250, 236, 224)
    b.doos(x0, y, z0, x1, y + 0.45, z1, rand)
    b.guhgezicht(x0, y + 0.45, x1, y + 2.3, z0 - 0.05)
    vak = (x1 - x0) / 4
    kl = [(255, 214, 230), (200, 226, 255), (250, 240, 214), (190, 236, 220)]
    for i in range(4):
        a = x0 + i * vak
        b.doos(a + 0.06, y + 0.45, z0 + 0.15, a + vak - 0.06, y + 0.62, z1 - 0.05, kl[i])
        if i:
            b.doos(a - 0.05, y + 0.45, z0 + 0.15, a + 0.05, y + 0.95, z1, roze)
    b.doos(x0 - 0.12, y, z0, x0, y + 1.0, z1, roze)
    b.doos(x1, y, z0, x1 + 0.12, y + 1.0, z1, roze)
    b.vak(x0 + vak * 0.5, y + 0.62, z0 + 1.0, "GUH")
    b.deken(x0 + 0.12, y + 0.95, z0 + 1.25, x0 + vak - 0.12, z1 - 0.1, (255, 170, 200))
    b.vak(x0 + vak * 1.5, y + 0.62, z0 + 1.1, "BLUB")
    b.deken(x0 + vak + 0.12, y + 0.9, z0 + 1.45, x0 + 2 * vak - 0.12, z1 - 0.1, (150, 200, 250))
    lx = x0 + vak * 2.5
    b.doos(lx - 0.45, y + 0.62, z0 + 0.5, lx + 0.45, y + 0.82, z1 - 0.2, (236, 214, 176))
    b.doos(lx - 0.4, y + 0.66, z0 + 0.55, lx + 0.4, y + 0.83, z1 - 0.25, (255, 246, 228), solid=False)
    b.vak(lx, y + 0.7, z0 + 1.2, "MUIS")
    b.ruit(lx - 0.38, y + 0.84, z0 + 1.45, lx + 0.38, z1 - 0.28, (240, 120, 150), (255, 230, 236))
    sx = x0 + vak * 3.5
    b.schelp(sx, y + 0.62, z0 + 1.2, 0.5)
    b.vak(sx, y + 0.72, z0 + 1.25, "SCHILLY")
    return b


def f02():
    b = Bed("slaaptreintje")
    y, z0, z1 = 1, 2.0, 3.6
    b.doos(0.6, y, z0 + 0.25, 8.6, y + 0.05, z0 + 0.35, (150, 120, 100), solid=False)
    b.doos(0.6, y, z1 - 0.35, 8.6, y + 0.05, z1 - 0.25, (150, 120, 100), solid=False)
    lx0, lx1 = 6.6, 8.4
    b.doos(lx0, y + 0.25, z0, lx1, y + 1.05, z1, (255, 160, 190))
    b.doos(lx0, y + 1.05, z0, lx0 + 0.75, y + 1.7, z1, (255, 196, 216))
    b.doos(lx0 - 0.05, y + 1.7, z0 - 0.05, lx0 + 0.8, y + 1.8, z1 + 0.05, (220, 120, 160))
    b.doos(lx1 - 0.55, y + 1.05, (z0 + z1) / 2 - 0.15, lx1 - 0.25, y + 1.55, (z0 + z1) / 2 + 0.15, (120, 90, 110))
    for zz in (z0 + 0.25, z1 - 0.5):
        for xx in (lx0 + 0.2, lx1 - 0.6):
            b.doos(xx, y + 0.05, zz, xx + 0.4, y + 0.45, zz + 0.25, (220, 70, 90))
    fx = lx1 + 0.01
    for (zz, yy) in ((z0 + 0.35, y + 0.65), (z1 - 0.55, y + 0.65)):
        b.doos(fx, yy, zz, fx + 0.02, yy + 0.22, zz + 0.2, (40, 30, 60), solid=False)
    b.doos(fx, y + 0.45, (z0 + z1) / 2 - 0.08, fx + 0.02, y + 0.53, (z0 + z1) / 2 + 0.08, (40, 30, 60), solid=False)
    for zz in (z0 + 0.1, z1 - 0.3):
        b.doos(fx, y + 0.5, zz, fx + 0.02, y + 0.58, zz + 0.2, (255, 120, 150), solid=False)
    wag = [(4.6, 6.3, (200, 226, 255), "guh"), (2.6, 4.3, (255, 240, 200), "blub"), (0.8, 2.3, (190, 236, 220), "zee")]
    for (a, c, kleur, wie) in wag:
        b.doos(a, y + 0.25, z0, c, y + 0.7, z1, kleur)
        b.doos(a + 0.08, y + 0.7, z0 + 0.08, c - 0.08, y + 0.82, z1 - 0.08, (255, 250, 244), solid=False)
        for zz in (z0 + 0.2, z1 - 0.45):
            for xx in (a + 0.15, c - 0.55):
                b.doos(xx, y + 0.05, zz, xx + 0.4, y + 0.45, zz + 0.25, (220, 70, 90))
        b.doos(c, y + 0.35, (z0 + z1) / 2 - 0.05, c + 0.3, y + 0.45, (z0 + z1) / 2 + 0.05, (150, 120, 100), solid=False)
        cx = (a + c) / 2
        if wie == "guh":
            b.vak(cx - 0.25, y + 0.82, z0 + 0.8, "GUH")
            b.vak(cx + 0.45, y + 0.82, z0 + 1.0, "MUIS")
            b.deken(a + 0.12, y + 1.05, z0 + 1.0, c - 0.12, z1 - 0.12, (150, 190, 255))
        elif wie == "blub":
            b.vak(cx, y + 0.82, z0 + 0.8, "BLUB")
            b.deken(a + 0.12, y + 1.0, z0 + 1.1, c - 0.12, z1 - 0.12, (255, 210, 120))
        else:
            b.doos(a + 0.1, y + 0.72, z0 + 0.1, c - 0.1, y + 0.8, z1 - 0.1, (120, 200, 230), solid=False, tex="water")
            b.vak(cx - 0.3, y + 0.8, z0 + 0.8, "SCHILLY")
            b.vak(cx + 0.35, y + 0.8, z0 + 0.9, "SCHILLY")
    return b


def f03():
    b = Bed("kussenfort")
    x0, x1, z0, z1, y = 1.3, 5.7, 1.3, 5.6, 1
    kl = [(255, 186, 210), (190, 220, 255), (255, 236, 160), (200, 236, 200), (226, 200, 250)]
    i = 0
    for x in [x0 + k * 0.55 for k in range(8)]:
        b.doos(x, y, z0, x + 0.52, y + 1.0, z0 + 0.45, kl[i % 5])
        i += 1
    for z in [z0 + 0.45 + k * 0.55 for k in range(7)]:
        b.doos(x0, y, z, x0 + 0.45, y + 0.8, z + 0.52, kl[i % 5])
        i += 1
        b.doos(x1 - 0.45, y, z, x1, y + 0.8, z + 0.52, kl[(i + 2) % 5])
        i += 1
    b.doos(x0 + 0.45, y + 1.0, z0, x0 + 1.6, y + 1.4, z0 + 0.45, (255, 250, 240))
    b.doos(x1 - 1.6, y + 1.0, z0, x1 - 0.45, y + 1.4, z0 + 0.45, (255, 250, 240))
    for (fx, fk) in ((x0 + 0.2, (255, 120, 160)), (x1 - 0.25, (120, 180, 255))):
        b.doos(fx, y + 0.8, z0 + 0.15, fx + 0.06, y + 2.2, z0 + 0.21, (230, 220, 210), solid=False)
        b.doos(fx + 0.06, y + 1.8, z0 + 0.15, fx + 0.5, y + 2.15, z0 + 0.2, fk, solid=False)
    b.doos(x0 + 0.45, y, z0 + 0.45, x1 - 0.45, y + 0.15, z1, (255, 240, 246))
    b.vak(x0 + 1.3, y + 0.15, z0 + 1.3, "GUH")
    b.deken(x0 + 0.7, y + 0.5, z0 + 1.65, x0 + 2.0, z0 + 2.4, (255, 170, 200))
    b.vak(x1 - 1.3, y + 0.17, z0 + 1.4, "BLUB")
    b.deken(x1 - 2.0, y + 0.45, z0 + 1.75, x1 - 0.65, z0 + 2.45, (200, 220, 255))
    b.doos(x0 + 0.8, y + 0.15, z1 - 1.6, x0 + 1.9, y + 0.35, z1 - 0.5, (236, 214, 176))
    b.vak(x0 + 1.15, y + 0.35, z1 - 1.1, "MUIS")
    b.vak(x0 + 1.6, y + 0.35, z1 - 0.95, "MUIS")
    b.ruit(x0 + 0.85, y + 0.52, z1 - 0.95, x0 + 1.85, z1 - 0.55, (240, 120, 150), (255, 230, 236))
    b.schelp(x1 - 1.3, y + 0.15, z1 - 1.1, 0.55)
    b.vak(x1 - 1.3, y + 0.25, z1 - 1.05, "SCHILLY")
    for k in range(3):
        b.hartje(x0 + 1.4 + k * 1.1, y + 0.16, z0 + 3.1)
    return b


def f04():
    b = Bed("droomschuitje")
    cx, cz, y = 4.0, 3.0, 1
    maan = (255, 236, 160)
    for i in range(21):
        a = math.radians(-80 + i * 8)
        x, yy = cx + 2.7 * math.sin(a), y + 1.35 - 1.25 * math.cos(a)
        dikte = 0.5 * math.cos(a) + 0.12
        b.doos(x - 0.17, yy - dikte / 2, cz - 1.0, x + 0.17, yy + dikte / 2, cz + 1.0, maan)
    b.doos(cx + 2.35, y + 1.9, cz + 1.0, cx + 2.6, y + 1.94, cz + 1.01, (120, 90, 60), solid=False)
    hout, hout2 = (180, 120, 200), (210, 160, 230)
    b.doos(cx - 1.9, y + 0.55, cz - 0.85, cx + 1.9, y + 1.05, cz + 0.85, hout)
    b.doos(cx - 1.8, y + 1.05, cz - 0.75, cx + 1.8, y + 1.12, cz + 0.75, hout2)
    b.doos(cx - 1.75, y + 1.05, cz - 0.7, cx + 1.75, y + 1.18, cz + 0.7, (255, 248, 250))
    b.vak(cx - 1.15, y + 1.18, cz - 0.15, "GUH")
    b.vak(cx - 0.25, y + 1.18, cz - 0.1, "BLUB")
    b.vak(cx + 0.55, y + 1.18, cz + 0.05, "MUIS")
    b.schelp(cx + 1.3, y + 1.18, cz - 0.1, 0.4)
    b.vak(cx + 1.3, y + 1.28, cz - 0.05, "SCHILLY")
    b.deken(cx - 1.7, y + 1.3, cz + 0.3, cx + 1.7, cz + 0.72, (120, 110, 200))
    for k in range(7):
        b.ster(cx - 1.5 + k * 0.5, y + 1.39, cz + 0.52, 0.08)
    for (sx, hoog) in ((cx - 2.0, 2.8), (cx - 0.6, 3.1), (cx + 0.8, 2.9), (cx + 2.2, 3.2)):
        b.doos(sx - 0.01, y + hoog - 0.2, cz - 1.0, sx + 0.01, y + 3.7, cz - 0.98, (230, 230, 240), solid=False)
        b.ster(sx, y + hoog - 0.3, cz - 0.97, 0.16, vlak="z")
    return b


def f05():
    b = Bed("boomhut_stapelbed")
    cx, cz, y = 3.2, 3.2, 1
    stam = (150, 110, 80)
    b.doos(cx - 0.35, y, cz - 0.35, cx + 0.35, y + 4.6, cz + 0.35, stam)
    for (dy, r, kleur) in ((4.4, 1.4, (150, 210, 130)), (4.9, 1.0, (170, 225, 150))):
        b.doos(cx - r, y + dy, cz - r, cx + r, y + dy + 0.6, cz + r, kleur)
    for (dy, wie) in ((0.25, "zee"), (1.6, "blub"), (2.95, "guh")):
        b.doos(cx - 1.25, y + dy, cz - 1.0, cx + 1.25, y + dy + 0.15, cz + 1.0, (214, 176, 130))
        for (xx, zz) in ((cx - 1.25, cz - 1.0), (cx + 1.13, cz - 1.0), (cx - 1.25, cz + 0.88), (cx + 1.13, cz + 0.88)):
            b.doos(xx, y + dy + 0.15, zz, xx + 0.12, y + dy + 0.5, zz + 0.12, stam)
        b.doos(cx - 1.25, y + dy + 0.42, cz + 0.88, cx + 1.25, y + dy + 0.5, cz + 1.0, stam)
        if wie == "zee":
            b.doos(cx - 1.15, y + dy + 0.15, cz - 0.9, cx + 1.15, y + dy + 0.28, cz + 0.85, (120, 200, 230), solid=False, tex="water")
            b.vak(cx - 0.7, y + dy + 0.28, cz + 0.2, "SCHILLY")
            b.vak(cx + 0.7, y + dy + 0.28, cz + 0.25, "SCHILLY")
        elif wie == "blub":
            b.doos(cx + 0.35, y + dy + 0.15, cz - 0.9, cx + 1.15, y + dy + 0.3, cz + 0.8, (200, 226, 255))
            b.vak(cx + 0.75, y + dy + 0.3, cz + 0.1, "BLUB")
            b.deken(cx + 0.38, y + dy + 0.6, cz + 0.35, cx + 1.12, cz + 0.78, (150, 200, 120))
        else:
            b.doos(cx - 1.15, y + dy + 0.15, cz - 0.9, cx - 0.35, y + dy + 0.3, cz + 0.8, (255, 214, 230))
            b.vak(cx - 0.75, y + dy + 0.3, cz + 0.0, "GUH")
            b.deken(cx - 1.12, y + dy + 0.6, cz + 0.3, cx - 0.38, cz + 0.78, (255, 170, 200))
    hx, hy = cx + 0.35, y + 3.6
    b.doos(hx, hy, cz - 0.35, hx + 0.7, hy + 0.55, cz + 0.35, (255, 236, 200))
    b.doos(hx - 0.05, hy + 0.55, cz - 0.4, hx + 0.75, hy + 0.65, cz + 0.4, (220, 90, 110))
    b.vak(hx + 0.35, hy + 0.02, cz + 0.05, "MUIS")
    for i in range(10):
        b.doos(cx + 1.3, y + 0.2 + i * 0.4, cz - 0.3, cx + 1.36, y + 0.26 + i * 0.4, cz + 0.3, stam, solid=False)
    b.doos(cx + 1.3, y, cz - 0.32, cx + 1.36, y + 4.2, cz - 0.26, stam, solid=False)
    b.doos(cx + 1.3, y, cz + 0.26, cx + 1.36, y + 4.2, cz + 0.32, stam, solid=False)
    return b


def f06():
    b = Bed("quiltbed")
    x0, x1, z0, z1, y = 1.5, 7.5, 1.5, 5.5, 1
    b.pootjes(x0, z0, x1, z1, y, 0.25, (214, 176, 130), 0.2)
    b.doos(x0, y + 0.25, z0, x1, y + 0.55, z1, (255, 250, 244))
    b.doos(x0, y + 0.25, z0 - 0.15, x1, y + 1.6, z0, (214, 176, 130))
    for k, kl in enumerate([(255, 186, 210), (190, 220, 255), (255, 236, 160), (200, 236, 200)]):
        b.hartje(x0 + 0.75 + k * 1.5, y + 1.15, z0 + 0.001, 0.16, kl, vlak="z")
    lapjes = [(x0 + 0.1, z0 + 0.1, x0 + 2.95, z0 + 1.95, (255, 186, 210), (255, 220, 232)),
              (x0 + 3.05, z0 + 0.1, x1 - 0.1, z0 + 1.95, (190, 220, 255), (226, 240, 255)),
              (x0 + 0.1, z0 + 2.05, x0 + 2.95, z1 - 0.1, (255, 236, 160), (255, 246, 210)),
              (x0 + 3.05, z0 + 2.05, x1 - 0.1, z1 - 0.1, (190, 236, 220), (220, 248, 238))]
    for (a, c, d, e, k1, k2) in lapjes:
        b.ruit(a, y + 0.56, c, d, e, k1, k2)
    b.doos(x0 + 0.6, y + 0.6, z0 + 0.2, x0 + 1.4, y + 0.8, z0 + 0.6, (255, 255, 255), solid=False)
    b.vak(x0 + 1.5, y + 0.6, z0 + 0.8, "GUH")
    b.vak(x0 + 4.5, y + 0.62, z0 + 0.9, "BLUB")
    b.vak(x0 + 1.1, y + 0.6, z0 + 3.0, "MUIS")
    b.vak(x0 + 1.9, y + 0.6, z0 + 3.2, "MUIS")
    b.schelp(x0 + 4.5, y + 0.6, z0 + 3.0, 0.5)
    b.vak(x0 + 4.5, y + 0.7, z0 + 3.05, "SCHILLY")
    return b


def f07():
    b = Bed("badkuipbed")
    x0, x1, z0, z1, y = 1.4, 6.6, 1.4, 3.8, 1
    wit, goud = (250, 250, 255), (240, 200, 90)
    for (xx, zz) in ((x0 + 0.2, z0 + 0.2), (x1 - 0.4, z0 + 0.2), (x0 + 0.2, z1 - 0.4), (x1 - 0.4, z1 - 0.4)):
        b.doos(xx, y, zz, xx + 0.22, y + 0.3, zz + 0.22, goud)
    b.doos(x0, y + 0.3, z0, x1, y + 1.2, z0 + 0.15, wit)
    b.doos(x0, y + 0.3, z1 - 0.15, x1, y + 1.2, z1, wit)
    b.doos(x0, y + 0.3, z0, x0 + 0.15, y + 1.5, z1, wit)
    b.doos(x1 - 0.15, y + 0.3, z0, x1, y + 1.2, z1, wit)
    b.doos(x0, y + 0.3, z0, x1, y + 0.45, z1, wit)
    b.doos(x1 - 1.7, y + 0.45, z0 + 0.15, x1 - 0.15, y + 0.75, z1 - 0.15, (130, 200, 235), solid=False, tex="water")
    b.doos(x0 + 0.15, y + 0.45, z0 + 0.15, x1 - 1.7, y + 0.7, z1 - 0.15, (255, 244, 250))
    b.vak(x1 - 1.2, y + 0.75, z0 + 1.0, "SCHILLY")
    b.vak(x1 - 0.6, y + 0.75, z0 + 1.4, "SCHILLY")
    b.vak(x0 + 0.9, y + 0.7, z0 + 0.8, "GUH")
    b.vak(x0 + 2.2, y + 0.7, z0 + 0.9, "BLUB")
    b.vak(x0 + 3.1, y + 0.72, z0 + 1.1, "MUIS")
    for k in range(18):
        bx, bz = x0 + 0.3 + (k * 0.37) % 3.3, z0 + 1.4 + ((k * 7) % 5) * 0.18
        r = 0.12 + (k % 3) * 0.04
        b.doos(bx - r, y + 0.95, bz - r, bx + r, y + 0.95 + 2 * r, bz + r, (255, 255, 255), solid=False, tex="schuim")
    b.doos(x0 - 0.05, y + 1.5, z0 + 0.6, x0 + 0.1, y + 1.65, z1 - 0.6, goud, solid=False)
    b.doos(x1 - 1.0, y + 0.75, z0 + 0.5, x1 - 0.7, y + 0.95, z0 + 0.8, (255, 220, 60), solid=False)
    b.doos(x1 - 0.75, y + 0.8, z0 + 0.6, x1 - 0.62, y + 0.86, z0 + 0.7, (255, 150, 60), solid=False)
    return b


def f08():
    """The Knuffelnest: the big round nest for eight guhs (a rim of pink and cream cushions, guh ears at the back, a soft
    round mattress, a little knabbel bowl in the middle), in the style of the other beds."""
    b = Bed("knuffelnest")
    cx, cz, y = 3.0, 3.0, 1
    for i in range(28):                                          # the rim: a ring of cushions, pink and cream
        a = 2 * math.pi * i / 28
        x, z = cx + 1.78 * math.cos(a), cz + 1.78 * math.sin(a)
        k = (255, 186, 210) if i % 2 == 0 else (255, 236, 226)
        b.doos(x - 0.2, y, z - 0.2, x + 0.2, y + 0.55, z + 0.2, k)
    for (r, h, k) in ((1.75, 0.15, (255, 214, 230)), (1.4, 0.2, (255, 232, 240))):   # the round mattress (three boxes each)
        b.doos(cx - r, y, cz - r * 0.55, cx + r, y + h, cz + r * 0.55, k)
        b.doos(cx - r * 0.55, y, cz - r, cx + r * 0.55, y + h, cz + r, k)
        b.doos(cx - r * 0.82, y, cz - r * 0.82, cx + r * 0.82, y + h, cz + r * 0.82, k)
    for ox in (-1.1, 0.55):                                      # two big guh ears on the back of the rim
        b.doos(cx + ox, y + 0.4, cz - 1.98, cx + ox + 0.55, y + 1.3, cz - 1.8, (255, 186, 210))
        b.doos(cx + ox + 0.1, y + 0.5, cz - 1.8, cx + ox + 0.45, y + 1.2, cz - 1.78, (255, 150, 190), solid=False)
    b.guhgezicht(cx - 0.55, y + 0.4, cx + 0.55, y + 1.05, cz - 1.95)   # a little guh face on the back
    b.doos(cx - 0.22, y + 0.2, cz - 0.22, cx + 0.22, y + 0.32, cz + 0.22, (236, 214, 176), solid=False)   # the knabbel bowl
    b.doos(cx - 0.16, y + 0.32, cz - 0.16, cx + 0.16, y + 0.36, cz + 0.16, (255, 214, 90), solid=False)
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        b.hartje(cx + 1.0 * math.cos(a), y + 0.205, cz + 1.0 * math.sin(a), 0.1)
    for k in range(8):                                           # eight guhs in a ring, heads to the middle
        a = 2 * math.pi * k / 8 + math.pi / 8
        b.vak(cx + 1.15 * math.cos(a), y + 0.2, cz + 1.15 * math.sin(a), "GUH")
    return b


NAMEN = {  # id -> (Dutch, English, Dutch lore, English lore)
    "guh_hoofdbank": ("Guh-hoofd-bank", "Guh Head Bench",
                      "Een lange bank-bed met een groot guh-gezicht als rugleuning. Iedereen een eigen vakje: guh, Blub, muisje en Schilly.",
                      "A long bench bed with a big guh face as its backrest. Everyone their own nook: guh, Blub, mouse and Schilly."),
    "slaaptreintje": ("Slaaptreintje", "Sleepy Train",
                      "Een locomotief met een guh-snoet en drie coupés. Volgende halte: Dromenland. Instappen, zzz!",
                      "A locomotive with a guh snout and three carriages. Next stop: Dreamland. All aboard, zzz!"),
    "kussenfort": ("Kussenfort", "Pillow Fort",
                   "Muurtjes van kussens, vlaggetjes en een nestje voor iedereen. Het enige fort waar je moet fluisteren.",
                   "Walls of pillows, little flags and a nest for everyone. The only fort where you have to whisper."),
    "droomschuitje": ("Droomschuitje", "Dream Boat",
                      "Een schuitje in de maansikkel, met sterretjes aan touwtjes. Schommelt vanzelf in slaap; zeeziek wordt niemand.",
                      "A little boat in the crescent moon, with stars on strings. It rocks you to sleep; nobody gets seasick."),
    "boomhut_stapelbed": ("Boomhut-stapelbed", "Treehouse Bunk Bed",
                          "Drie verdiepingen en een vogelhuisje: beneden het natst, boven het hoogst, het muisje het knusst.",
                          "Three floors and a birdhouse: the wettest at the bottom, the highest on top, the mouse the cosiest."),
    "quiltbed": ("Quiltbed", "Quilt Bed",
                 "Een groot bed met een eigen lapje per diertje. Wie over de naad rolt, moet een knabbel betalen.",
                 "A big bed with a patch of quilt for every critter. Whoever rolls over the seam owes a nibble."),
    "badkuipbed": ("Badkuip-bed", "Bathtub Bed",
                   "Een oude badkuip vol schuim, met dekentjes van belletjes. De Schilly's houden het hoekje nat.",
                   "An old bathtub full of foam, with blankets of bubbles. The Schillys keep their corner wet."),
    "knuffelnest": ("Knuffelnest", "Cuddle Nest",
                    "Het grote ronde nest voor acht guhs, met oortjes aan de rand. Samen in een kringetje, kopjes naar het knabbelschaaltje.",
                    "The big round nest for eight guhs, with little ears on its rim. All in a circle, heads towards the nibble bowl."),
}

ALLE = [f01, f02, f03, f04, f05, f06, f07, f08]

# the footprint the user picked for each family bed (blocks: x, height (None = keep the render's proportions), z)
MAAT = {"guh_hoofdbank": (5, None, 2), "slaaptreintje": (8, None, 2), "kussenfort": (4, None, 4), "droomschuitje": (4, None, 2),
        "boomhut_stapelbed": (2, 3, 2), "quiltbed": (6, None, 4), "badkuipbed": (5, None, 2), "knuffelnest": (4, None, 4)}
