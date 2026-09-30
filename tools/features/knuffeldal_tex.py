"""
Het Knuffeldal (2.8) - the textures of its blocks, items and particles (PIL / numpy, all painted here; vanilla
textures only as a base to recolour). textures(h) writes them; the face painter is shared with the town's block art.
"""
import math
import random

import numpy as np
from PIL import Image

PINK = (246, 170, 204)
PINK_LIGHT = (255, 214, 232)
PINK_DARK = (214, 118, 162)
CREAM = (255, 236, 226)
LILAC = (205, 170, 222)

EYE_DARK = (34, 24, 52)
EYE_RING = (64, 132, 214)
EYE_RING_LIGHT = (104, 206, 232)
WHITE = (255, 255, 255)
BLUSH = (255, 150, 188)
NOSE = (232, 112, 164)
MOUTH = (150, 72, 116)


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def noisy(base, var, rng, size=16):
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = rng.randint(-var, var)
            img.putpixel((x, y), clamp(ch + v for ch in base) + (255,))
    return img


def mix(a, b, t):
    return clamp(a[i] * (1 - t) + b[i] * t for i in range(3))


# =====================================================================================================================
# guh faces (the guh's own style: big glossy eyes with a blue ring and two highlights, a tiny snoet, blush)
# =====================================================================================================================
def paint_face(img, mood, x0=0, y0=0):
    """A guh face on a 16x16 texture: 0 happy, 1 sleepy, 2 surprised, 3 vads (squinting, tongue, big blush)."""
    px = img.load()

    def put(x, y, c):
        if 0 <= x0 + x < img.width and 0 <= y0 + y < img.height:
            px[x0 + x, y0 + y] = tuple(c[:3]) + (255,)

    for ex in (2, 9):
        if mood == 1:                                   # sleepy: closed, curved lids with lashes
            for dx, dy in ((0, 7), (1, 8), (2, 8), (3, 8), (4, 7)):
                put(ex + dx, dy, EYE_DARK)
        elif mood == 3:                                 # vads: happy ^^
            for dx, dy in ((0, 8), (1, 7), (2, 6), (3, 7), (4, 8)):
                put(ex + dx, dy, EYE_DARK)
        else:
            big = mood == 2
            rows = range(4, 10) if big else range(5, 10)
            for y in rows:
                for dx in range(5):
                    cx, cy = 2.0, (6.5 if big else 7.0)
                    d = math.hypot(dx - cx, (y - cy) * 1.05)
                    if d <= 2.6:
                        c = EYE_DARK
                        if d > 1.5 and y >= cy:
                            c = EYE_RING_LIGHT if d > 2.1 else EYE_RING
                        put(ex + dx, y, c)
            put(ex + 1, (5 if big else 6), WHITE)         # the two highlights
            put(ex + 3, (8 if big else 8), (220, 230, 255))
            for dx in range(5):                          # the lashes (the top line)
                put(ex + dx, (3 if big else 4), EYE_DARK) if dx in (1, 2, 3) else None
    blush = [(1, 11), (2, 11), (13, 11), (14, 11)]
    if mood == 3:
        blush += [(1, 12), (2, 12), (13, 12), (14, 12), (0, 11), (15, 11)]
    for x, y in blush:
        put(x, y, BLUSH)
    put(7, 11, NOSE)
    put(8, 11, NOSE)
    if mood == 2:                                       # "o"
        for x, y in ((7, 13), (8, 13), (6, 14), (9, 14), (7, 15), (8, 15)):
            put(x, y, MOUTH)
    else:
        for x, y in ((6, 12), (7, 13), (8, 13), (9, 12)):
            put(x, y, MOUTH)
        if mood == 3:
            put(7, 14, (240, 110, 150))
            put(8, 14, (240, 110, 150))


# =====================================================================================================================
# blocks
# =====================================================================================================================
def knuffelsteen(rng):
    """Soft pink-cream stone: big rounded blocks with light edges (a cosy brick)."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            row = y // 8
            off = 4 if row % 2 else 0
            bx = (x + off) % 8
            by = y % 8
            c = mix((250, 206, 216), CREAM, 0.45)
            if by == 7 or bx == 7:
                c = (226, 168, 190)                      # the mortar
            elif by == 0 or bx == 0:
                c = mix(c, WHITE, 0.35)                  # the light edge
            v = rng.randint(-5, 5)
            px[x, y] = clamp(ch + v for ch in c) + (255,)
    return img


def pluisdak(rng):
    """Fluffy roof tiles: rows of pink scallops with a white fluffy rim."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 2 if row % 2 else 0
            cx = ((x + off) % 4) - 1.5
            cy = y % 4
            d = math.hypot(cx, (cy - 0.5) * 1.3)
            c = (238, 120, 170) if d < 2.1 else (208, 88, 142)
            if cy == 3:
                c = (255, 236, 244)                      # the fluff between the rows
            v = rng.randint(-6, 6)
            px[x, y] = clamp(ch + v for ch in c) + (255,)
    return img


def klinkers(rng):
    """Paving stones in pink, white and cream, now and then a little heart."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    colours = [(242, 176, 200), (252, 232, 236), (246, 206, 214), (234, 150, 184)]
    cells = {}
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 2 if row % 2 else 0
            key = ((x + off) // 4, row)
            if key not in cells:
                cells[key] = colours[rng.randrange(len(colours))]
            c = cells[key]
            if y % 4 == 3 or (x + off) % 4 == 3:
                c = (196, 132, 160)
            v = rng.randint(-4, 4)
            px[x, y] = clamp(ch + v for ch in c) + (255,)
    for (x, y) in ((5, 1), (7, 1), (4, 2), (5, 2), (6, 2), (7, 2), (8, 2), (5, 3), (6, 3), (7, 3), (6, 4)):
        px[x, y + 8] = (232, 92, 142, 255)
    return img


def knuffelgras_top(rng):
    img = noisy((248, 172, 206), 8, rng)
    px = img.load()
    for _ in range(26):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = (255, 222, 238, 255) if rng.random() < 0.7 else (232, 140, 184, 255)
    for _ in range(3):
        x, y = rng.randrange(1, 15), rng.randrange(1, 15)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
            px[x + dx, y + dy] = (255, 244, 250, 255)   # little white fluff balls
    return img


def knuffelgras_side(rng, wool):
    img = wool.copy()
    px = img.load()
    for x in range(16):
        depth = 3 + (1 if (x * 7) % 5 < 2 else 0) + (1 if x % 4 == 1 else 0)
        for y in range(depth):
            v = rng.randint(-8, 8)
            px[x, y] = clamp(ch + v for ch in ((250, 180, 212) if y < depth - 1 else (255, 214, 232))) + (255,)
    return img


def pluisgras(rng):
    """Tufts of pink fluff on thin stalks (a cross plant)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for tuft in range(5):
        x = 2 + tuft * 3 + rng.randint(-1, 1)
        top = rng.randint(2, 7)
        for y in range(top + 3, 16):
            px[x, y] = (226, 150, 186, 255)
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if dx * dx + dy * dy <= 5 and 0 <= x + dx < 16 and 0 <= top + dy < 16:
                    c = (255, 226, 240) if dx * dx + dy * dy <= 2 else (248, 180, 212)
                    px[x + dx, top + dy] = c + (255,)
    return img


def guhpaddenstoel(rng):
    """A little mushroom with a pink cap, white dots and a tiny guh face on the stem."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(4, 10):
        w = [3, 5, 6, 6, 6, 5][y - 4]
        for x in range(8 - w, 8 + w):
            c = (238, 96, 156) if y < 9 else (206, 70, 128)
            px[x, y] = c + (255,)
    for (x, y) in ((5, 5), (9, 5), (7, 6), (11, 7), (4, 7), (8, 8)):
        px[x, y] = (255, 246, 250, 255)
    for y in range(10, 16):
        for x in range(6, 10):
            px[x, y] = (250, 238, 230, 255)
    px[6, 12] = px[9, 12] = (30, 24, 40, 255)          # eyes
    px[7, 13] = px[8, 13] = (240, 140, 180, 255)       # snoet
    return img


def seizoen_plant(seizoen, rng):
    """The plants in a seizoensbloembak (a cross texture) per season."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    stems = (96, 170, 96)
    if seizoen == "winter":
        for tree in (4, 11):
            for y in range(4, 16):
                w = (y - 4) // 3 + 1
                for x in range(tree - w, tree + w + 1):
                    if 0 <= x < 16:
                        px[x, y] = ((60, 120, 90) if (x + y) % 3 else (250, 250, 255)) + (255,)
            px[tree, 3] = (255, 220, 90, 255)            # a star on top
        for (x, y) in ((2, 9), (13, 8), (6, 12), (9, 6)):
            px[x, y] = (255, 120, 150, 255)              # little lights
        return img
    for i in range(6):
        x = 1 + i * 2 + rng.randint(0, 2)
        top = rng.randint(3, 8)
        for y in range(top + 2, 16):
            px[x, y] = stems + (255,)
        if seizoen == "lente":
            petals = [(255, 196, 222), (255, 236, 244), (246, 140, 190)]
        elif seizoen == "zomer":
            petals = [(255, 214, 60), (255, 176, 40), (255, 240, 120)]
        else:
            petals = [(232, 110, 40), (206, 72, 40), (250, 170, 50)]
        c = petals[i % 3]
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1)):
            if 0 <= x + dx < 16 and 0 <= top + dy < 16:
                px[x + dx, top + dy] = (c if (dx, dy) != (0, 0) else (255, 240, 200 if seizoen != "herfst" else 120)) + (255,)
    if seizoen == "herfst":                               # a little pumpkin
        for y in range(11, 15):
            for x in range(9, 14):
                px[x, y] = ((236, 136, 30) if x % 2 else (214, 110, 20)) + (255,)
        px[11, 10] = (80, 120, 40, 255)
    if seizoen == "zomer":                                # a sunflower head
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if dx * dx + dy * dy <= 5:
                    px[8 + dx, 5 + dy] = ((110, 70, 30) if dx * dx + dy * dy <= 1 else (255, 206, 40)) + (255,)
    return img


def slinger(seizoen):
    """A garland: a string along the top with little triangular flags (seasonal colours); transparent elsewhere."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    colours = {"lente": [(255, 190, 220), (190, 235, 210), (255, 244, 180), (214, 196, 250)],
               "zomer": [(255, 214, 60), (255, 120, 150), (120, 210, 250), (255, 150, 60)],
               "herfst": [(214, 96, 40), (240, 170, 50), (160, 70, 50), (250, 206, 120)],
               "winter": [(255, 255, 255), (170, 210, 250), (255, 200, 220), (200, 230, 255)]}[seizoen]
    for x in range(16):
        y = 1 + int(round(1.5 * math.sin(x / 15 * math.pi)))
        px[x, y] = (120, 90, 90, 255)
    for i, x0 in enumerate((0, 4, 8, 12)):
        c = colours[i % 4]
        top = 2 + int(round(1.5 * math.sin((x0 + 2) / 15 * math.pi)))
        for dy in range(6):
            for dx in range(4):
                if abs(dx - 1.5) <= (5 - dy) * 0.35:
                    px[x0 + dx, top + dy] = c + (255,)
        if seizoen == "winter":
            px[x0 + 1, top + 7 if top + 7 < 16 else 15] = (255, 230, 120, 255)   # a little light
    return img


def bladeren(rng):
    """Autumn leaves in a heap (the bladerhoopje)."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    cs = [(222, 108, 40), (240, 160, 50), (196, 70, 44), (250, 206, 90), (170, 90, 50)]
    for y in range(16):
        for x in range(16):
            c = cs[rng.randrange(len(cs))] if rng.random() < 0.7 else cs[(x // 3 + y // 3) % len(cs)]
            v = rng.randint(-8, 8)
            px[x, y] = clamp(ch + v for ch in c) + (255,)
    return img


def sneeuw(rng, face=False):
    img = noisy((246, 248, 255), 5, rng)
    if face:
        px = img.load()
        for (x, y) in ((4, 5), (5, 5), (4, 6), (5, 6), (10, 5), (11, 5), (10, 6), (11, 6)):
            px[x, y] = (30, 30, 40, 255)                 # coal eyes
        px[4, 5] = px[10, 5] = (120, 130, 150, 255)
        for (x, y) in ((7, 8), (8, 8), (8, 9)):
            px[x, y] = (246, 140, 60, 255)               # a knabbel nose
        for (x, y) in ((2, 9), (3, 9), (12, 9), (13, 9)):
            px[x, y] = (255, 170, 200, 255)              # blush
        for (x, y) in ((6, 11), (7, 12), (8, 12), (9, 11)):
            px[x, y] = (40, 40, 50, 255)
    return img


def tafelkleed():
    """A pink-and-white checked tablecloth with a lace border."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = (255, 250, 252) if (x // 4 + y // 4) % 2 else (246, 150, 192)
            if (x // 2 + y // 2) % 2 and (x // 4 + y // 4) % 2 == 0:
                c = (238, 128, 176)
            px[x, y] = c + (255,)
    return img


def tafelrand():
    img = tafelkleed()
    px = img.load()
    for x in range(16):
        for y in range(12, 16):
            px[x, y] = ((255, 255, 255) if (x + y) % 2 else (255, 232, 242)) + (255,)
    return img


def hapjes(rng):
    """The top of a laid table: plates with cake, knabbels and cups (seen from above)."""
    img = tafelkleed()
    px = img.load()
    for cx, cy, kind in ((4, 4, "taart"), (11, 5, "knabbels"), (5, 11, "thee"), (12, 12, "taart")):
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                d = math.hypot(dx, dy)
                if d <= 3.2:
                    px[cx + dx, cy + dy] = (255, 255, 255, 255)
                if d <= 2.2:
                    c = {"taart": (250, 190, 210) if (dx + dy) % 2 else (255, 240, 244), "knabbels": (250, 196, 50),
                         "thee": (200, 120, 70)}[kind]
                    px[cx + dx, cy + dy] = c + (255,)
        if kind == "taart":
            px[cx, cy - 1] = (220, 40, 70, 255)
    return img


def oorkonde():
    """The Knus-oorkonde: a golden frame around a certificate with a guh face and a pink ribbon."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            inner = x in (1, 14) or y in (1, 14)
            c = (214, 160, 40) if edge else (250, 212, 90) if inner else (255, 250, 236)
            px[x, y] = c + (255,)
    face = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    paint_face(face, 0)
    small = face.resize((10, 10), Image.NEAREST)
    for y in range(10):
        for x in range(10):
            p = small.getpixel((x, y))
            if p[3]:
                px[3 + x, 2 + y] = p
    for x in range(3, 13):
        px[x, 12] = (160, 140, 130, 255)
    for (x, y) in ((11, 11), (12, 11), (11, 12), (12, 12), (11, 13), (13, 13), (12, 14)):
        px[x, y] = (238, 90, 150, 255)                  # the ribbon
    return img


# =====================================================================================================================
# items and particles
# =====================================================================================================================
def lijstje():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(1, 15):
        for x in range(3, 13):
            px[x, y] = (255, 250, 240, 255)
    for x in range(3, 13):
        px[x, 1] = px[x, 14] = (220, 200, 180, 255)
    for y in (4, 7, 10):
        px[4, y] = (238, 90, 150, 255)                  # little pink ticks
        for x in range(6, 12):
            px[x, y] = (150, 130, 130, 255)
    for (x, y) in ((6, 0), (7, 0), (8, 0), (9, 0), (7, 1), (8, 1)):
        px[x, y] = (238, 90, 150, 255)                  # the ribbon on top
    return img


def kopje_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, (y - 9) * 1.1)
            if d <= 6.2:
                px[x, y] = ((236, 242, 255) if d > 5 else (252, 252, 255)) + (255,)
    for (cx, cy) in ((3, 3), (12, 3)):                  # ears
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if dx * dx + dy * dy <= 4:
                    px[cx + dx, cy + dy] = ((250, 250, 255) if dx * dx + dy * dy > 1 else (255, 200, 220)) + (255,)
    for (x, y) in ((5, 8), (10, 8)):
        px[x, y] = (30, 30, 40, 255)
    px[7, 10] = px[8, 10] = (246, 140, 60, 255)
    px[3, 11] = px[12, 11] = (255, 170, 200, 255)
    return img


def particle_pluisje(i):
    col = [(255, 214, 234), (255, 240, 248), (248, 180, 214), (255, 226, 200)][i]
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for y in range(8):
        for x in range(8):
            d = math.dist((x + 0.5, y + 0.5), (4, 4))
            if d < 3.6:
                a = int(255 * max(0.0, 1 - (d / 3.6) ** 2))
                img.putpixel((x, y), clamp(ch + (255 - ch) * max(0.0, 1 - d / 1.8) for ch in col) + (a,))
    return img


def particle_kruimel(i):
    col = [(222, 170, 90), (246, 206, 120), (190, 130, 70)][i]
    img = Image.new("RGBA", (4, 4), (0, 0, 0, 0))
    for (x, y) in [((1, 1), (2, 1), (1, 2), (2, 2)), ((0, 1), (1, 1), (2, 1), (1, 2), (2, 2), (1, 0)),
                   ((1, 1), (2, 1), (1, 2), (3, 2), (2, 2))][i]:
        img.putpixel((x, y), clamp(c - (20 if y == 2 else 0) for c in col) + (255,))
    return img


def particle_bloesem(i):
    col = [(255, 190, 222), (255, 236, 246), (246, 150, 196)][i]
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for (x, y) in ((3, 1), (4, 1), (2, 2), (3, 2), (4, 2), (5, 2), (2, 3), (3, 3), (4, 3), (5, 3), (3, 4), (4, 4), (4, 5)):
        img.putpixel((x, y), col + (255,))
    img.putpixel((3, 2), (255, 250, 250, 255))
    return img


def particle_sneeuw(i):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    pts = [(3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (1, 3), (2, 3), (4, 3), (5, 3), (2, 2), (4, 4), (2, 4), (4, 2)] if i == 0 else \
        [(3, 2), (4, 2), (2, 3), (3, 3), (4, 3), (5, 3), (3, 4), (4, 4)]
    for (x, y) in pts:
        img.putpixel((x, y), (250, 252, 255, 255))
    return img


def textures(h):
    rng = random.Random(28001)
    save = h.save
    wool = h.vanilla("block/pink_wool")
    save(knuffelgras_top(rng), "block", "knuffelgras_top.png")
    save(knuffelgras_side(rng, wool), "block", "knuffelgras_side.png")
    save(pluisgras(rng), "block", "pluisgras.png")
    save(h.ramp(h.vanilla("block/cherry_log"), (214, 170, 190), (255, 244, 248)), "block", "pluizenboom_stam.png")
    save(h.ramp(h.vanilla("block/cherry_log_top"), (220, 150, 180), (255, 236, 244)), "block", "pluizenboom_stam_top.png")
    leaves = h.ramp(h.vanilla("block/cherry_leaves"), (236, 128, 180), (255, 230, 244))
    save(leaves, "block", "pluizenboom_bladeren.png")
    save(h.ramp(h.vanilla("block/cherry_sapling"), (200, 120, 160), (255, 220, 236)), "block", "pluizenboom_zaailing.png")
    save(guhpaddenstoel(rng), "block", "guhpaddenstoel.png")
    hoed = h.recolour(h.vanilla("block/red_mushroom_block"), hue=0.92, sat=0.7, val=1.1)
    save(hoed, "block", "guhpaddenstoel_hoed.png")
    save(h.ramp(h.vanilla("block/mushroom_stem"), (226, 206, 214), (255, 248, 250)), "block", "guhpaddenstoel_steel.png")
    steen = knuffelsteen(rng)
    save(steen, "block", "knuffelsteen.png")
    for mood in range(4):
        face = steen.copy()
        paint_face(face, mood)
        save(face, "block", f"knuffelsteen_gezicht_{mood}.png")
    save(pluisdak(rng), "block", "pluisdak.png")
    save(klinkers(rng), "block", "knuffelklinkers.png")
    save(tafelkleed(), "block", "feestbuffettafel_top.png")
    save(tafelrand(), "block", "feestbuffettafel_rand.png")
    save(hapjes(rng), "block", "feestbuffettafel_hapjes.png")
    save(oorkonde(), "block", "knus_oorkonde.png")
    save(h.ramp(h.vanilla("block/gold_block"), (200, 140, 30), (255, 230, 120)), "block", "knus_oorkonde_lijst.png")
    for s in ("lente", "zomer", "herfst", "winter"):
        save(seizoen_plant(s, random.Random(28100 + ["lente", "zomer", "herfst", "winter"].index(s))), "block", f"seizoensbloembak_{s}.png")
        save(slinger(s), "block", f"seizoensslinger_{s}.png")
    bak = steen.copy()
    px = bak.load()
    for (x, y) in ((6, 6), (7, 6), (9, 6), (10, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7), (6, 8), (7, 8), (8, 8),
                   (9, 8), (10, 8), (7, 9), (8, 9), (9, 9), (8, 10)):
        px[x - 1, y] = (238, 110, 160, 255)            # a heart on the flower box
    save(bak, "block", "seizoensbloembak_zijkant.png")
    save(h.ramp(h.vanilla("block/dirt"), (110, 70, 60), (170, 120, 100)), "block", "seizoensbloembak_aarde.png")
    save(noisy((250, 252, 255), 4, rng), "block", "seizoensbloembak_sneeuw.png")
    save(bladeren(rng), "block", "bladerhoopje.png")
    save(sneeuw(rng), "block", "sneeuwpopguh.png")
    save(sneeuw(rng, face=True), "block", "sneeuwpopguh_gezicht.png")
    # items
    save(lijstje(), "item", "knusfeestlijstje.png")
    save(kopje_icon(), "item", "sneeuwguhkopje.png")
    # particles
    for name, n, painter in (("pluisje", 4, particle_pluisje), ("kruimel", 3, particle_kruimel), ("bloesemblaadje", 3, particle_bloesem),
                             ("sneeuwvlokje", 2, particle_sneeuw)):
        for i in range(n):
            save(painter(i), "particle", f"{name}_{i}.png")
        h.w(f"{h.A}/particles/{name}.json", {"textures": [f"guhs:{name}_{i}" for i in range(n)]})
