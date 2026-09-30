"""
Textures of the Barbecuether (slice 1 of 2.7.0): blocks, the frying sauce, the portal, items and the Grillguh.
All painted with PIL / numpy in 16x16 (the fluid and portal: animated strips). Used by barbecuether.py.
"""
import math
import random

import numpy as np
from PIL import Image

# --- palette ---------------------------------------------------------------------------------------------------------
CHAR = (48, 40, 41)          # houtskool: charcoal black with a warm tint
CHAR_LIGHT = (84, 72, 70)
CHAR_DARK = (24, 19, 20)
EMBER = (236, 104, 28)
EMBER_HOT = (255, 190, 70)
BRICK = (64, 54, 56)
MORTAR = (28, 22, 22)
IRON = (42, 42, 48)
IRON_LIGHT = (86, 86, 96)
IRON_DARK = (20, 20, 24)
ASH = (150, 146, 142)
ASH_DARK = (104, 100, 98)
PEANUT = (186, 112, 40)
PEANUT_LIGHT = (222, 156, 72)
PEANUT_DARK = (128, 70, 22)
MEAT = (140, 64, 34)
MEAT_DARK = (78, 32, 18)
SKIN = (168, 78, 44)
SKIN_LIGHT = (208, 118, 70)
MUSTARD = (226, 184, 40)
MUSTARD_LIGHT = (252, 222, 96)
MUSTARD_DARK = (170, 124, 16)
CHEESE = (250, 196, 60)


def clamp(a):
    return np.clip(a, 0, 255).astype(np.uint8)


def img(a):
    """numpy HxWx3 or HxWx4 (float) -> RGBA image."""
    a = np.asarray(a, np.float32)
    if a.shape[-1] == 3:
        a = np.concatenate([a, np.full(a.shape[:2] + (1,), 255, np.float32)], -1)
    return Image.fromarray(clamp(a))


def vnoise(size, cells, seed, octaves=3):
    """Tileable value noise in [0, 1] (size x size), `cells` random points across on the first octave."""
    rng = np.random.default_rng(seed)
    out = np.zeros((size, size), np.float32)
    amp, total = 1.0, 0.0
    for o in range(octaves):
        n = max(2, cells * (2 ** o))
        grid = rng.random((n, n)).astype(np.float32)
        ys = np.arange(size) * n / size
        y0 = np.floor(ys).astype(int)
        fy = ys - y0
        fy = fy * fy * (3 - 2 * fy)
        x0, fx = y0, fy
        a = grid[y0 % n][:, x0 % n]
        b = grid[y0 % n][:, (x0 + 1) % n]
        c = grid[(y0 + 1) % n][:, x0 % n]
        d = grid[(y0 + 1) % n][:, (x0 + 1) % n]
        top = a + (b - a) * fx[None, :]
        bot = c + (d - c) * fx[None, :]
        out += amp * (top + (bot - top) * fy[:, None])
        total += amp
        amp *= 0.5
    return out / total


def ramp(t, stops):
    """Colour ramp: t (HxW in 0..1) through [(pos, colour), ...]."""
    t = np.asarray(t, np.float32)
    out = np.zeros(t.shape + (3,), np.float32)
    pos = [p for p, _ in stops]
    cols = [np.array(c, np.float32) for _, c in stops]
    for i in range(len(stops) - 1):
        m = (t >= pos[i]) & (t <= pos[i + 1])
        f = ((t - pos[i]) / max(1e-6, pos[i + 1] - pos[i]))[..., None]
        out[m] = (cols[i] + (cols[i + 1] - cols[i]) * f)[m]
    out[t < pos[0]] = cols[0]
    out[t > pos[-1]] = cols[-1]
    return out


def grain(a, seed, amount=8):
    rng = np.random.default_rng(seed)
    return a + rng.normal(0, amount, a.shape[:2] + (1,))


# =====================================================================================================================
# stone
# =====================================================================================================================
def houtskoolsteen(seed=1, embers=True):
    """Charcoal rock: lumpy black-brown, lighter crumbs, dark cracks and a few glowing embers (the netherrack look)."""
    n = vnoise(16, 4, seed, 3)
    lumps = vnoise(16, 8, seed + 1, 2)
    a = ramp(n * 0.7 + lumps * 0.3, [(0.0, CHAR_DARK), (0.45, CHAR), (0.75, CHAR_LIGHT), (1.0, (104, 90, 86))])
    a = grain(a, seed, 6)
    rng = random.Random(seed)
    # cracks: short dark wiggles
    for _ in range(4):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(3, 6)):
            a[y % 16, x % 16] = CHAR_DARK
            x += rng.choice((-1, 0, 1))
            y += 1
    if embers:
        for _ in range(rng.randint(3, 5)):
            x, y = rng.randrange(16), rng.randrange(16)
            a[y, x] = EMBER_HOT if rng.random() < 0.3 else EMBER
            if rng.random() < 0.5:
                a[y, (x + 1) % 16] = (150, 60, 20)
    return img(a)


def bricks(seed=2, base=BRICK, mortar=MORTAR, glow=True, rows=4):
    """Bricks like nether bricks: 4 rows of short bricks with dark mortar; embers glow in some joints."""
    rng = random.Random(seed)
    a = np.zeros((16, 16, 3), np.float32)
    h = 16 // rows
    for r in range(rows):
        off = 0 if r % 2 == 0 else 4
        for bx in range(-1, 3):
            x0 = bx * 8 + off
            shade = rng.uniform(-10, 10)
            for y in range(r * h, r * h + h):
                for x in range(x0, x0 + 8):
                    if 0 <= x < 16:
                        edge_top = y == r * h + h - 1
                        edge_side = x == x0 + 7
                        if edge_top or edge_side:
                            a[y, x] = mortar
                        else:
                            hi = 14 if y == r * h else 0
                            a[y, x] = np.array(base) + shade + hi
    a = grain(a, seed, 5)
    if glow:
        for _ in range(3):
            y = rng.randrange(rows) * h + h - 1
            x = rng.randrange(16)
            a[y, x] = EMBER
    return img(a)


def cracked(im, seed=3):
    a = np.asarray(im).astype(np.float32)
    rng = random.Random(seed)
    for _ in range(3):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(5, 9)):
            a[y % 16, x % 16, :3] = MORTAR
            if rng.random() < 0.3:
                a[y % 16, x % 16, :3] = (120, 50, 20)
            x += rng.choice((-1, 1))
            y += rng.choice((0, 1))
    return img(a)


def guh_face_carved(seed=4):
    """Chiseled houtskoolsteen bricks: a frame with a carved guh head (ears, glowing eyes, little mouth)."""
    a = np.asarray(bricks(seed)).astype(np.float32)[..., :3]
    frame = np.array(BRICK) + 10
    a[1:15, 1:15] = np.array(BRICK) - 6
    a[1:15, 1] = a[1:15, 14] = a[1, 1:15] = a[14, 1:15] = frame
    face = ["....aa....aa....", "...abba..abba...", "...abbaaaabba...", "..abbbbbbbbbba..", "..abbbbbbbbbba..",
            ".abbeebbbbeebba.", ".abbeebbbbeebba.", ".abbbbbbbbbbbba.", ".abppbbmmbbppba.", ".abbbbbmmbbbbba.",
            "..abbbbbbbbbba..", "...aaaaaaaaaa..."]
    col = {"a": (22, 17, 18), "b": (74, 64, 64), "e": EMBER_HOT, "p": (120, 60, 50), "m": (30, 20, 20)}
    for y, row in enumerate(face):
        for x, ch in enumerate(row):
            if ch in col:
                a[2 + y, x] = col[ch]
    a = grain(a, seed, 3)
    return img(a)


def roosterijzer(seed=5):
    """Grill iron: dark iron plates with faint grate lines and a bit of heat tint."""
    n = vnoise(16, 4, seed, 3)
    a = ramp(n, [(0, IRON_DARK), (0.5, IRON), (1, IRON_LIGHT)])
    for y in range(16):
        if y % 4 == 1:
            a[y, :] = a[y, :] * 0.75
        if y % 4 == 2:
            a[y, :] = a[y, :] * 1.15
    a = grain(a, seed, 5)
    rng = random.Random(seed)
    for _ in range(4):
        x, y = rng.randrange(16), rng.randrange(16)
        a[y, x] = (110, 60, 40)            # a speck of rust
    return img(a)


def roosterijzer_pilaar(seed=6):
    """The pillar's side: vertical grill bars with deep gaps; the top: a square grate."""
    side = np.zeros((16, 16, 3), np.float32)
    for x in range(16):
        m = x % 4
        c = IRON_DARK if m == 0 else IRON_LIGHT if m == 1 else IRON if m == 2 else (32, 32, 38)
        side[:, x] = c
    side = grain(side, seed, 5)
    rng = random.Random(seed)
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        side[y, x] = side[y, x] * 0.6 + np.array((140, 60, 30)) * 0.4      # heat stains
    top = np.zeros((16, 16, 3), np.float32)
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            top[y, x] = IRON_LIGHT if ring == 0 else IRON if ring % 3 else IRON_DARK
            if ring > 3 and (x % 3 == 0 or y % 3 == 0):
                top[y, x] = (30, 30, 34)
            if ring > 3 and x % 3 and y % 3:
                top[y, x] = (180, 70, 20) if (x + y) % 5 else EMBER      # coals glowing under the grate
    return img(side), img(grain(top, seed + 1, 3))


def gepolijst_roosterijzer(seed=7):
    a = np.zeros((16, 16, 3), np.float32) + IRON
    a[0, :] = a[:, 0] = IRON_LIGHT
    a[15, :] = a[:, 15] = IRON_DARK
    for i in range(3, 13, 3):
        a[i, 2:14] = IRON_DARK
        a[i + 1, 2:14] = np.array(IRON) + 14
    a = grain(a, seed, 3)
    return img(a)


def tralies(seed=8):
    """Grill bars (like iron bars): three thick black bars with a hot orange glow at the bottom."""
    a = np.zeros((16, 16, 4), np.float32)
    for x in (1, 2, 7, 8, 13, 14):
        for y in range(16):
            heat = max(0.0, (y - 9) / 7.0)
            c = np.array(IRON_LIGHT if x in (1, 7, 13) else IRON_DARK, np.float32)
            c = c * (1 - heat) + np.array((200, 80, 20)) * heat
            a[y, x] = (*c, 255)
    for y in (0, 15):
        a[y, :] = (*IRON_DARK, 255)
    return img(a)


# =====================================================================================================================
# glow and ash
# =====================================================================================================================
def gloeikool(seed=9):
    """Glowing coal lumps: black-brown crust with blazing orange-yellow cores."""
    n = vnoise(16, 5, seed, 2)
    core = vnoise(16, 7, seed + 3, 1)
    t = np.clip(core * 1.6 - 0.35 + n * 0.3, 0, 1)
    a = ramp(t, [(0, (40, 20, 12)), (0.35, (120, 40, 12)), (0.6, EMBER), (0.85, EMBER_HOT), (1, (255, 240, 170))])
    return img(grain(a, seed, 4))


def as_blok(seed=10):
    """Grey ash with little guh faces pressed into it (like the faces in soul sand)."""
    n = vnoise(16, 5, seed, 3)
    a = ramp(n, [(0, ASH_DARK), (0.5, ASH), (1, (184, 180, 176))])
    a = grain(a, seed, 7)
    faces = [(3, 4), (10, 10)]
    for fx, fy in faces:
        for (dx, dy) in ((0, 0), (3, 0)):
            a[fy + dy, fx + dx] = (60, 56, 56)
        a[fy - 2, fx - 1] = a[fy - 2, fx + 4] = (90, 86, 84)       # ears
        a[fy + 2, fx + 1:fx + 3] = (70, 64, 64)                     # mouth
    return img(a)


def as_aarde(seed=11):
    n = vnoise(16, 6, seed, 3)
    a = ramp(n, [(0, (54, 50, 50)), (0.5, (86, 80, 78)), (1, (118, 112, 108))])
    a = grain(a, seed, 6)
    rng = random.Random(seed)
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        a[y, x] = (40, 36, 36) if rng.random() < 0.5 else (140, 132, 126)
    return img(a)


def verkoold_guhbot(seed=12):
    """Charred guh bone: the bone block look, in soot-black with ash-grey edges; the top shows the round bone ends."""
    side = np.zeros((16, 16, 3), np.float32)
    for y in range(16):
        for x in range(16):
            side[y, x] = (70, 64, 62) if x in (0, 15) else (44, 40, 40) if (x // 5) % 2 else (56, 51, 50)
    side[:, 5] = side[:, 10] = (26, 22, 22)
    side = grain(side, seed, 5)
    top = np.zeros((16, 16, 3), np.float32) + (30, 27, 27)
    for (cx, cy) in ((4, 4), (11, 4), (4, 11), (11, 11)):
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if d < 3.2:
                    top[y, x] = (80, 74, 72) if d > 2 else (40, 36, 36)
    return img(side), img(grain(top, seed + 1, 3))


# =====================================================================================================================
# the forests
# =====================================================================================================================
def sate_stam(seed=13):
    """The saté skewer 'log': a pale bamboo skewer with charred grill marks; the top shows the cut stick."""
    side = np.zeros((16, 16, 3), np.float32)
    for x in range(16):
        side[:, x] = (214, 180, 120) if x % 5 else (180, 144, 90)
    for y in range(16):
        for x in range(16):
            if (x + y * 2) % 9 == 0:
                side[y, x] = (60, 36, 24)              # grill marks
    for y in (4, 12):
        side[y, :] = (170, 136, 84)                   # bamboo knots
    top = np.zeros((16, 16, 3), np.float32)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            top[y, x] = (200, 164, 108) if int(d) % 3 else (160, 126, 80)
            if d > 7:
                top[y, x] = (120, 80, 50)
    return img(grain(side, seed, 5)), img(grain(top, seed + 1, 4))


def sate_vlees(seed=14):
    """Grilled saté meat: brown chunks with dark grill stripes and peanut sauce dribbled over."""
    n = vnoise(16, 4, seed, 3)
    a = ramp(n, [(0, MEAT_DARK), (0.5, MEAT), (1, (182, 96, 54))])
    for y in range(16):
        for x in range(16):
            if (x + y) % 6 == 0:
                a[y, x] = (40, 18, 10)
    sauce = vnoise(16, 3, seed + 5, 2) > 0.62
    a[sauce] = np.array(PEANUT)
    return img(grain(a, seed, 5))


def mosterd_blok(seed=15):
    n = vnoise(16, 4, seed, 3)
    a = ramp(n, [(0, MUSTARD_DARK), (0.55, MUSTARD), (1, MUSTARD_LIGHT)])
    rng = random.Random(seed)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        a[y, x] = (130, 80, 20)                      # mustard seeds
    return img(grain(a, seed, 4))


def worst_stam(seed=16):
    """Braadworst: glossy brown-red sausage skin with dark grill stripes; the top: a cut through the sausage."""
    side = np.zeros((16, 16, 3), np.float32)
    for x in range(16):
        f = 0.8 + 0.3 * math.sin((x + 2) / 16 * math.pi)
        side[:, x] = np.array(SKIN) * f
    for y in range(16):
        for x in range(16):
            if (x - y) % 8 in (0, 1):
                side[y, x] = (70, 30, 16)
    side[:, 3] = side[:, 3] * 1.25                   # shine
    top = np.zeros((16, 16, 3), np.float32)
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            top[y, x] = (214, 136, 124) if d < 6.5 else SKIN if d < 7.6 else (90, 40, 20)
            if d < 6.5 and rng.random() < 0.12:
                top[y, x] = (240, 210, 200)          # bits of fat
    return img(grain(side, seed, 4)), img(grain(top, seed + 1, 3))


def nylium(top_col, light, dark, seed, speck):
    """A sauce crust on houtskoolsteen: the top all sauce, the side houtskoolsteen with sauce dripping down."""
    n = vnoise(16, 5, seed, 3)
    top = ramp(n, [(0, dark), (0.5, top_col), (1, light)])
    rng = random.Random(seed)
    for _ in range(9):
        x, y = rng.randrange(16), rng.randrange(16)
        top[y, x] = speck
    base = np.asarray(houtskoolsteen(seed + 2)).astype(np.float32)[..., :3]
    side = base.copy()
    for x in range(16):
        depth = 3 + rng.randint(0, 3) + (2 if rng.random() < 0.25 else 0)
        for y in range(depth):
            side[y, x] = top[y, x] if y < depth - 1 else np.array(dark)
    return img(grain(top, seed, 4)), img(side)


def uienlicht(seed=17):
    """Grilled onion rings: glowing golden rings on a dark roasted background (the shroomlight)."""
    a = np.zeros((16, 16, 3), np.float32) + (120, 70, 20)
    for (cx, cy, r) in ((5, 5, 3.6), (11, 10, 3.6), (4, 12, 2.6), (12, 3, 2.4)):
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if abs(d - r) < 1.0:
                    a[y, x] = (255, 214, 110) if d < r else (236, 150, 50)
                elif d < r - 1:
                    a[y, x] = (255, 240, 180)
    return img(grain(a, seed, 5))


def plant(kind, seed):
    """Cross-model plants: 16x16 with transparency."""
    a = np.zeros((16, 16, 4), np.float32)
    rng = random.Random(seed)

    def px(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            a[y, x] = (*c, 255)
    if kind in ("pindascheutjes", "mosterdscheutjes"):
        c1, c2 = ((PEANUT_DARK, PEANUT_LIGHT) if kind == "pindascheutjes" else (MUSTARD_DARK, MUSTARD_LIGHT))
        for x0 in (2, 5, 8, 11, 13):
            h = rng.randint(5, 12)
            x = x0
            for i in range(h):
                px(x, 15 - i, c1 if i < h - 2 else c2)
                if rng.random() < 0.3:
                    x += rng.choice((-1, 1))
            px(x, 15 - h, c2)
            if kind == "pindascheutjes":
                px(x + 1, 15 - h, (230, 190, 120))        # a little peanut on top
    elif kind == "sate_zwammetje":
        for y in range(4, 16):
            px(7, y, (214, 180, 120))
        for y0 in (5, 9):
            for y in range(y0, y0 + 3):
                for x in range(5, 10):
                    px(x, y, MEAT if (x + y) % 3 else MEAT_DARK)
        px(6, 5, PEANUT_LIGHT); px(8, 9, PEANUT_LIGHT)
        px(7, 3, (240, 220, 170))
    elif kind == "worst_zwammetje":
        for y in range(5, 16):
            for x in range(6, 10):
                px(x, y, SKIN if x != 6 else SKIN_LIGHT)
        for x in range(7, 9):
            px(x, 4, SKIN)
        for y in (7, 10, 13):
            px(9, y, (70, 30, 16)); px(8, y + 1, (70, 30, 16))
        px(6, 5, MUSTARD); px(7, 6, MUSTARD); px(8, 5, MUSTARD); px(9, 6, MUSTARD)
    elif kind == "smeulkooltjes":
        for (x0, y0) in ((3, 12), (8, 13), (11, 11), (6, 10)):
            for y in range(y0, min(16, y0 + 3)):
                for x in range(x0, x0 + 3):
                    hot = (x + y) % 3 == 0
                    px(x, y, EMBER_HOT if hot else (60, 28, 16) if y == y0 else EMBER)
        for y in range(5, 10):
            if rng.random() < 0.6:
                px(rng.randint(3, 12), y, (255, 150, 50))     # sparks
    return img(a)


def pindasausplasje(seed=18):
    n = vnoise(16, 3, seed, 2)
    a = ramp(n, [(0, PEANUT_DARK), (0.5, PEANUT), (1, PEANUT_LIGHT)])
    rng = random.Random(seed)
    for _ in range(4):
        x, y = rng.randrange(16), rng.randrange(16)
        a[y, x] = (250, 220, 170)                    # shine
    return img(grain(a, seed, 3))


def rookgat(seed=19):
    """A smoke vent: a grill grate over glowing coals (top), black iron (side)."""
    side, top = roosterijzer_pilaar(seed)
    t = np.asarray(top).astype(np.float32)[..., :3]
    for y in range(16):
        for x in range(16):
            if 2 <= x <= 13 and 2 <= y <= 13:
                t[y, x] = (24, 24, 28) if (x % 3 == 1 or y % 3 == 1) else ((230, 110, 30) if (x * 7 + y) % 4 else (255, 200, 90))
    s = np.asarray(roosterijzer(seed)).astype(np.float32)[..., :3]
    s[:6, :] = s[:6, :] * 0.8
    return img(s), img(t)


def kaasfrituursaus_frames(lava_img):
    """The frying sauce from the vanilla lava animation: deep orange-gold, bright bubbles."""
    a = np.asarray(lava_img).astype(np.float32)
    lum = a[..., :3].mean(-1, keepdims=True) / 255
    dark, mid, light = np.array([176, 70, 10]), np.array([236, 140, 26]), np.array([255, 226, 110])
    t = np.clip((lum - 0.3) / 0.65, 0, 1)
    rgb = np.where(t < 0.5, dark + (mid - dark) * (t * 2), mid + (light - mid) * ((t - 0.5) * 2))
    return Image.fromarray(np.concatenate([rgb, a[..., 3:]], -1).astype(np.uint8))


def grillkool(seed=20):
    """Grillkool (the obsidian): glossy black charcoal briquettes with deep red-orange glints."""
    n = vnoise(16, 4, seed, 3)
    a = ramp(n, [(0, (8, 6, 8)), (0.55, (24, 18, 22)), (0.8, (46, 30, 34)), (1, (70, 40, 40))])
    rng = random.Random(seed)
    # the briquette seams
    for y in range(16):
        for x in range(16):
            if (x + 2 * y) % 11 == 0:
                a[y, x] = (4, 3, 4)
    for _ in range(7):
        x, y = rng.randrange(16), rng.randrange(16)
        a[y, x] = (150, 40, 20) if rng.random() < 0.6 else (230, 100, 30)
        if rng.random() < 0.5:
            a[y, (x + 1) % 16] = (90, 24, 14)
    return img(grain(a, seed, 3))


def portal_frames(frames=32):
    """The portal: swirling flames, orange at the edges, yellow-white in the hot centre, translucent."""
    out = Image.new("RGBA", (16, 16 * frames))
    n1 = vnoise(64, 4, 91, 3)
    n2 = vnoise(64, 6, 92, 2)
    for f in range(frames):
        t = f / frames
        a = np.zeros((16, 16, 4), np.float32)
        for y in range(16):
            for x in range(16):
                # flames rise: sample the noise moving down over time, wrapped
                u = (x * 4 + int(math.sin(t * 6.283 + y * 0.4) * 3)) % 64
                v = (y * 4 + int(t * 64)) % 64
                h = n1[v, u] * 0.7 + n2[(v * 2) % 64, (u + 11) % 64] * 0.3
                h = h * 0.8 + (1 - y / 15) * 0.25
                col = ramp(np.array([[h]]), [(0, (120, 20, 10)), (0.4, (220, 80, 20)), (0.65, (255, 160, 40)),
                                             (0.85, (255, 225, 120)), (1, (255, 250, 220))])[0, 0]
                a[y, x] = (*col, 170 + 70 * min(1, h))
        out.paste(img(a), (0, 16 * f))
    return out


def ore_overlay(base, overlay):
    b = base.copy()
    b.alpha_composite(overlay.convert("RGBA"))
    return b


# =====================================================================================================================
# items
# =====================================================================================================================
ITEM_PAL = {
    ".": (0, 0, 0, 0), "k": (40, 26, 20, 255), "w": (250, 250, 246, 255), "W": (220, 216, 206, 255), "g": (190, 186, 176, 255),
    "o": (236, 104, 28, 255), "O": (255, 190, 70, 255), "y": (255, 236, 150, 255), "r": (200, 40, 30, 255),
    "b": (214, 180, 120, 255), "B": (160, 126, 80, 255), "m": (140, 64, 34, 255), "M": (78, 32, 18, 255),
    "c": (250, 196, 60, 255), "C": (210, 150, 30, 255), "s": (168, 78, 44, 255), "S": (208, 118, 70, 255), "d": (100, 40, 22, 255),
    "u": (226, 184, 40, 255), "p": (238, 141, 173, 255), "P": (190, 90, 130, 255), "a": (120, 110, 104, 255), "x": (24, 20, 20, 255),
}
ICONS = {
    # a white firelighter cube, with a flame on top
    "aanmaakblokje": ["................", "......O.........", ".....OyO........", ".....oOyO.......", "....ooOOo.......",
                      ".....oooo.......", "....kkkkkkkk....", "...kwwwwwwWk....", "..kwwwwwwWWgk...", "..kkkkkkkkWgk...",
                      "..kwwWwwwkWgk...", "..kwwwwwwkggk...", "..kwWwwwwkgk....", "..kwwwwwwkk.....", "..kkkkkkkk......",
                      "................"],
    "gloeikoolgruis": ["................", "................", "................", "......o.........", "...........O....",
                       "........o.......", ".....Oo.....o...", "....oOOo..oo....", "...ooOyOooOOo...", "..xoooOOooooOx..",
                       "..xxooooxoooxx..", "...xxxxxxxxxx...", "................", "................", "................",
                       "................"],
    "grillguh_recept": ["................", "..kkkkkkkkkk....", ".kwWWWWWWWWWk...", ".kwwwwwwwwwwWk..", "..kwgggwwggwwk..",
                        "..kwwwwwwwwwwk..", "..kwgggggwwwwk..", "..kwwwwwwwwwwk..", "..kwggwgggwwwk..", "..kwwwwwwwrrwk..",
                        "..kwgggwwrOrwk..", "..kwwwwwwwrrwk..", ".kWwwwwwwwwwk...", "kWWWWWWWWWWk....", ".kkkkkkkkkkk....",
                        "................"],
    "kaasknabbelsate": ["................", "..............B.", ".............b..", "..........ccb...", ".........cyccb..",
                        ".........ccCb...", ".......ccbC.....", "......cyccb.....", "......ccCb......", "....ccb.C.......",
                        "...cyccb........", "...ccCb.........", "....bb..........", "...b............", "..B.............",
                        "................"],
    "gegrilde_kaasknabbelsate": ["................", "..............B.", ".............b..", "..........Cdb...", ".........CcdCb..",
                                 ".........dCCb...", ".......CdbC.....", "......CcdCb.....", "......dCCb......", "....Cdb.C.......",
                                 "...CcdCb........", "...dCCb.........", "....bb..........", "...b............", "..B.............",
                                 "................"],
    "guhbraadworst": ["................", "................", "................", "............kk..", "..........kkSsk.",
                      "........kkSsdsk.", "......kkSsdssk..", "....kkSsdssuk...", "...kSsdssuuk....", "..kSdssuusk.....",
                      "..ksssuusk......", "..kssdssk.......", "...kkkkk........", "................", "................",
                      "................"],
}
