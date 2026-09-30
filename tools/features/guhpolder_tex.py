"""
De Guhpolder (2.9) - the textures: its blocks, the guh-molentje, knabbelmeel, the frost glitter and the three Pinguh looks.
All painted here (PIL / numpy); vanilla textures only as a base to recolour. textures(h) writes them.

The Pinguh textures are painted on the guh's own texture (guh.png, 128 x 128 UV at 4 px per unit): the fur is recoloured
(black / grey), then the belly, the face, the beak, the feet and the flippers are painted on the UV faces of the model's
cubes (read from guh.geo.json, so a re-imported model keeps working).
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image

from features import knuffeldal_tex as ktex

ICE = (170, 214, 240)
ICE_LIGHT = (222, 242, 255)
ICE_DARK = (112, 164, 212)
FROST = (238, 248, 255)
PINK = (246, 170, 204)
EYE_DARK = ktex.EYE_DARK


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def rgba(c, a=255):
    return clamp(c) + (a,)


# =====================================================================================================================
# the biome's blocks
# =====================================================================================================================
def rijpgras_top(rng):
    """Frosted guh grass seen from above: pale blue-mint blades under white rime, a few glittering crystals."""
    img = ktex.noisy((196, 222, 222), 7, rng)
    px = img.load()
    for _ in range(40):                                  # blades peeking through the rime
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = rgba((150, 196, 186) if rng.random() < 0.6 else (176, 214, 204))
    for _ in range(38):                                  # the rime
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = rgba((236, 246, 252) if rng.random() < 0.7 else (214, 234, 246))
    for _ in range(4):                                   # glitter
        x, y = rng.randrange(1, 15), rng.randrange(1, 15)
        px[x, y] = (255, 255, 255, 255)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if rng.random() < 0.5:
                px[x + dx, y + dy] = rgba((230, 244, 255))
    return img


def rijpgras_side(rng, wool, snowy=False):
    """The side: the pink guh soil (wool) with frosted grass on top and little icicles hanging over the edge."""
    img = wool.copy()
    px = img.load()
    for x in range(16):
        depth = (7 if snowy else 3) + (1 if (x * 7) % 5 < 2 else 0) + (1 if x % 4 == 1 else 0)
        for y in range(depth):
            v = rng.randint(-6, 6)
            if snowy:
                c = (246, 250, 255) if y < depth - 1 else (224, 236, 248)
            else:
                c = (200, 226, 226) if y < depth - 1 else (230, 244, 252)
                if rng.random() < 0.25:
                    c = (160, 204, 194)
            px[x, y] = rgba((c[0] + v, c[1] + v, c[2] + v))
        if not snowy and x % 5 == 2:                      # an icicle
            for y in range(depth, depth + 2 + (x % 3)):
                if y < 16:
                    px[x, y] = rgba((210, 236, 252))
    return img


def rijpsprietjes(rng):
    """A tuft of thin frosted blades with white tips and a few ice needles (a cross plant)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for i in range(9):
        x = 1 + i * 1.6 + rng.uniform(-0.4, 0.4)
        h = rng.randint(6, 12)
        lean = rng.uniform(-0.25, 0.25)
        for k in range(h):
            xx, yy = int(round(x + lean * k)), 15 - k
            if 0 <= xx < 16 and 0 <= yy < 16:
                t = k / h
                c = (140 + 90 * t, 190 + 50 * t, 186 + 60 * t)
                px[xx, yy] = rgba(c)
        tx, ty = int(round(x + lean * h)), 15 - h
        if 0 <= tx < 16 and 0 <= ty < 16:
            px[tx, ty] = (255, 255, 255, 255)
    for _ in range(4):                                   # ice needles
        x, y = rng.randrange(2, 14), rng.randrange(4, 12)
        px[x, y] = rgba((226, 246, 255))
    return img


def ijsbloempje(rng):
    """A see-through ice-blue flower with a tiny guh face in the middle, on a frosted stem with two leaves."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(9, 16):                                # the stem
        px[8, y] = rgba((120, 180, 176))
    for x, y in ((6, 12), (5, 11), (7, 12), (10, 13), (11, 12), (9, 13)):
        px[x, y] = rgba((150, 206, 196))
    cx, cy = 8, 5.5
    for y in range(0, 11):
        for x in range(2, 15):
            d = math.hypot(x - cx, (y - cy) * 1.1)
            ang = math.atan2(y - cy, x - cx)
            petal = 4.6 + 1.2 * math.cos(ang * 5)
            if d <= petal:
                shade = 1 - d / petal
                c = (150 + 70 * shade, 204 + 40 * shade, 255)
                px[x, y] = rgba(c, 170 + int(60 * shade))
    for y in range(4, 8):                                 # the heart of the flower: a tiny guh face
        for x in range(6, 11):
            px[x, y] = rgba((236, 248, 255), 250)
    px[7, 5] = rgba(EYE_DARK)
    px[9, 5] = rgba(EYE_DARK)
    px[6, 6] = rgba((255, 160, 196))
    px[10, 6] = rgba((255, 160, 196))
    px[8, 7] = rgba((150, 72, 116))
    return img


def polderijs(rng):
    """Polder ice: clear blue, lighter streaks where skates went, a few frozen bubbles and a hairline crack."""
    img = ktex.noisy((150, 196, 234), 5, rng)
    px = img.load()
    for y in range(16):                                   # depth: a soft diagonal shimmer
        for x in range(16):
            if (x + y * 2) % 13 < 2:
                r, g, b, a = px[x, y]
                px[x, y] = rgba((r + 16, g + 14, b + 10))
    for k in range(3):                                    # skate streaks
        y0 = rng.randrange(2, 14)
        for x in range(rng.randrange(0, 4), rng.randrange(11, 16)):
            yy = y0 + (x // 6)
            if yy < 16:
                px[x, yy] = rgba((214, 236, 252))
    for _ in range(3):                                    # bubbles
        x, y = rng.randrange(1, 15), rng.randrange(1, 15)
        px[x, y] = rgba((236, 248, 255))
        if x + 1 < 16:
            px[x + 1, y] = rgba((196, 226, 250))
    x, y = rng.randrange(3, 12), 0
    while y < 16:                                          # a crack
        px[x, y] = rgba((116, 162, 206))
        y += 1
        x = max(0, min(15, x + rng.choice((-1, 0, 0, 1))))
    return img


def knotwilg_stam(rng):
    """Gnarled pollard-willow bark: dark grey-brown with deep vertical furrows and a knot or two."""
    img = ktex.noisy((104, 86, 72), 8, rng)
    px = img.load()
    for x in range(16):
        if x % 4 == 0 or (x % 4 == 1 and rng.random() < 0.4):
            for y in range(16):
                if rng.random() < 0.9:
                    px[x, y] = rgba((66, 54, 46) if rng.random() < 0.8 else (80, 66, 56))
        elif x % 4 == 2:
            for y in range(16):
                if rng.random() < 0.35:
                    px[x, y] = rgba((132, 112, 94))
    for kx, ky in ((5, 4), (11, 11)):                     # knots
        for dx in range(-1, 2):
            for dy in range(-1, 2):
                px[kx + dx, ky + dy] = rgba((58, 46, 40) if (dx, dy) == (0, 0) else (122, 98, 80))
    for _ in range(6):                                    # frost in the furrows
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = rgba((214, 230, 240))
    return img


def knotwilg_stam_top(rng):
    img = ktex.noisy((150, 122, 94), 6, rng)
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7:
                px[x, y] = rgba((80, 64, 54))
            elif int(d) % 2 == 0:
                r, g, b, a = px[x, y]
                px[x, y] = rgba((r - 18, g - 16, b - 12))
    for x, y in ((7, 7), (8, 8), (7, 8), (8, 7)):
        px[x, y] = rgba((236, 244, 250))                   # a pinch of snow in the middle
    return img


def knotwilg_bladeren(rng, sneeuw=False):
    """Thin willow twigs (reddish brown and olive) with silver catkin buds and frost; with sneeuw a snow cap on top."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for i in range(7):
        x = rng.uniform(0, 16)
        lean = rng.uniform(-0.35, 0.35)
        col = rng.choice(((150, 72, 52), (170, 96, 60), (128, 118, 64)))
        for y in range(16):
            xx = int(x + lean * (15 - y)) % 16
            px[xx, y] = rgba(col)
            if rng.random() < 0.12:
                px[(xx + 1) % 16, y] = rgba((214, 216, 204))    # a catkin bud
            elif rng.random() < 0.08:
                px[(xx + rng.choice((-1, 1))) % 16, y] = rgba((236, 246, 252))   # frost
    if sneeuw:
        for x in range(16):
            depth = 3 + (1 if (x * 5) % 7 < 3 else 0)
            for y in range(depth):
                px[x, y] = rgba((246, 250, 255) if y < depth - 1 else (220, 234, 246))
    return img


def sneeuw_top(rng):
    return ktex.noisy((246, 250, 255), 4, rng)


def ijspegelguh_kristal(rng):
    """A glowing ice crystal shaped like a guh ear: a rounded point, icy blue rim, a soft pink-lilac inside (like a real
    guh ear) and a bright highlight."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(1, 16):
        t = (15 - y) / 14                                  # 0 at the bottom, 1 at the tip
        half = 5.6 * math.sqrt(max(0.0, 1 - t ** 1.6))
        for x in range(16):
            dx = abs(x - 7.5)
            if dx <= half:
                edge = half - dx
                if edge < 1.2:
                    c, a = (140, 196, 246), 240
                elif edge < 2.6 and y > 3:
                    c, a = (196, 232, 255), 210
                else:
                    c, a = (236, 204, 246), 190               # the ear's inside, pink-lilac ice
                px[x, y] = rgba(c, a)
    for y in range(4, 11):                                  # highlight
        px[5, y] = rgba((255, 255, 255), 235)
    px[6, 3] = rgba((255, 255, 255), 235)
    return img


# =====================================================================================================================
# the guh-molentje
# =====================================================================================================================
def molentje_voor(rng):
    """The front of the little mill: white-washed boards with a guh face, a little door at the bottom."""
    img = molentje_zijkant(rng, venster=False)
    face = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ktex.paint_face(face, 0)
    fpx, px = face.load(), img.load()
    for y in range(16):                                    # the face, a bit higher (the door goes under it)
        for x in range(16):
            if fpx[x, y][3] and 0 <= y - 3 < 16:
                px[x, y - 3] = fpx[x, y]
    for y in range(12, 16):                                # the door
        for x in range(6, 10):
            px[x, y] = rgba((150, 104, 70) if x in (6, 9) or y == 12 else (182, 128, 86))
    px[8, 14] = rgba((240, 200, 90))
    return img


def molentje_zijkant(rng, venster=True):
    img = ktex.noisy((244, 244, 248), 5, rng)
    px = img.load()
    for y in (3, 7, 11, 15):                               # boards
        for x in range(16):
            px[x, y] = rgba((216, 218, 228))
    for x in range(16):                                    # a delft-blue band
        px[x, 1] = rgba((70, 110, 190))
        if x % 3 == 0:
            px[x, 2] = rgba((110, 150, 214))
    if venster:
        for y in range(6, 10):
            for x in range(6, 10):
                px[x, y] = rgba((120, 170, 220) if (x, y) not in ((6, 6), (9, 6)) else (244, 244, 248))
        for x in range(6, 10):
            px[x, 10] = rgba((150, 104, 70))
    return img


def molentje_dak(rng):
    """The cap: soft pink pluisdak thatch."""
    img = ktex.noisy((236, 150, 188), 8, rng)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x + (y // 2) * 3) % 5 == 0:
                px[x, y] = rgba((210, 118, 160))
    return img


def molentje_wiek(rng):
    """A sail: a wooden lattice with white sail cloth (and a pink stripe)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15) or y % 4 == 0 or x == 7:
                px[x, y] = rgba((150, 106, 72))
            else:
                px[x, y] = rgba((250, 248, 244) if y % 8 < 6 else (255, 190, 214))
    return img


def molentje_hout(rng):
    return ktex.noisy((150, 106, 72), 8, rng)


# =====================================================================================================================
# items and particles
# =====================================================================================================================
def knabbelmeel():
    """A little flour sack tied with a pink ribbon, a guh face on it and golden knabbel flour peeking out."""
    rows = ["................",
            "......rrrr......",
            ".....yyyyyy.....",
            "......pppp......",
            ".....sssssss....",
            "....sssssssss...",
            "...sssssssssss..",
            "...ssessssesss..",
            "...ssessssesss..",
            "...sbsssssssbs..",
            "...sssssmmssss..",
            "...ssssssssssS..",
            "....sssssssssS..",
            ".....SSSSSSSS...",
            "................",
            "................"]
    pal = {"s": (236, 214, 176, 255), "S": (196, 170, 132, 255), "y": (250, 222, 120, 255), "r": (255, 240, 190, 255),
           "p": (240, 120, 170, 255), "e": EYE_DARK + (255,), "b": (255, 160, 196, 255), "m": (150, 72, 116, 255)}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                px[x, y] = pal[ch]
    return img


def glinster(i):
    """Frost glitter: a tiny four-pointed star in three sizes."""
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    px = img.load()
    r = (1, 2, 3)[i]
    for k in range(-r, r + 1):
        a = 255 - abs(k) * 50
        px[3 + k, 3] = (255, 255, 255, a)
        px[3, 3 + k] = (255, 255, 255, a)
    px[3, 3] = (255, 255, 255, 255)
    if i == 2:
        for dx, dy in ((1, 1), (-1, -1), (1, -1), (-1, 1)):
            px[3 + dx, 3 + dy] = (220, 240, 255, 180)
    return img



# =====================================================================================================================
# 2.10.1: guh-sneeuw (the polder's own snowfall particle: client/GuhSneeuw.java)
# =====================================================================================================================
def _zacht(size, teken, scale=8):
    """Draws at `scale` x the size with teken(draw, s) and scales down: soft, anti-aliased edges."""
    from PIL import ImageDraw
    big = Image.new("RGBA", (size * scale, size * scale), (0, 0, 0, 0))
    teken(ImageDraw.Draw(big), scale)
    return big.resize((size, size), Image.LANCZOS)


def sneeuwvlok(i):
    """Four soft flakes (8 x 8): a small and a bigger round puff, a little six-pointed star, a tiny cross-star."""
    def puff(r, kern):
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        px = img.load()
        for y in range(8):
            for x in range(8):
                d = math.hypot(x + 0.5 - 4, y + 0.5 - 4)
                a = max(0.0, 1 - (d / r) ** 2)
                if a > 0:
                    px[x, y] = rgba((250, 252, 255) if d < kern else (232, 242, 255), int(255 * min(1.0, a * 1.35)))
        return img

    def ster(n, lengte, dikte):
        def teken(d, s):
            c = 4 * s
            for k in range(n):
                hoek = math.pi * 2 * k / n + math.pi / 2
                x2, y2 = c + math.cos(hoek) * lengte * s, c + math.sin(hoek) * lengte * s
                d.line([(c, c), (x2, y2)], fill=(248, 252, 255, 255), width=int(dikte * s))
            r = dikte * 0.9 * s
            d.ellipse([c - r, c - r, c + r, c + r], fill=(255, 255, 255, 255))
        return _zacht(8, teken)

    if i == 0:
        return puff(2.2, 1.2)
    if i == 1:
        return puff(3.3, 1.8)
    if i == 2:
        return ster(6, 3.4, 0.9)
    return ster(4, 2.8, 1.0)


def guhkop_sneeuw():
    """The guh head among the flakes (16 x 16): a round pink head with two round ears (lighter inside), two dark dot eyes,
    rosy cheeks and a soft frosty white rim, so it reads as a snowflake-guh against the grey snow sky."""
    roze, oor_binnen, wang, rand = (246, 162, 200), (255, 208, 228), (255, 132, 176), (255, 255, 255)

    def teken(d, s):
        def cirkel(cx, cy, r, kleur):
            d.ellipse([(cx - r) * s, (cy - r) * s, (cx + r) * s, (cy + r) * s], fill=kleur)
        # the frosty rim: the same shapes a bit bigger, white
        oren, hoofd = ((3.8, 4.3), (12.2, 4.3)), (8, 9.9, 5.6)
        for (cx, cy) in oren:
            cirkel(cx, cy, 2.35 + 0.75, rand + (235,))
        cirkel(hoofd[0], hoofd[1], hoofd[2] + 0.75, rand + (235,))
        for (cx, cy) in oren:
            cirkel(cx, cy, 2.35, roze + (255,))
            cirkel(cx, cy, 1.2, oor_binnen + (255,))
        cirkel(*hoofd, roze + (255,))
        # the eyes (with a little shine), the cheeks, a tiny smile
        for ex in (6.0, 10.0):
            d.ellipse([(ex - 0.95) * s, 8.3 * s, (ex + 0.95) * s, 10.6 * s], fill=EYE_DARK + (255,))
            d.ellipse([(ex - 0.25) * s, 8.55 * s, (ex + 0.4) * s, 9.2 * s], fill=(255, 255, 255, 255))
        for wx in (4.3, 11.7):
            cirkel(wx, 11.4, 1.15, wang + (190,))
        d.arc([6.9 * s, 10.6 * s, 9.1 * s, 12.6 * s], 20, 160, fill=EYE_DARK + (255,), width=int(0.6 * s))
    return _zacht(16, teken)

# =====================================================================================================================
# the Pinguh
# =====================================================================================================================
FUR = (195, 160, 205)       # the guh's own fur colour on guh.png (tools/make_guh_variants.py)
PX = 4                      # texture pixels per UV unit


def _faces(geo, bone):
    """{face: (x0, y0, x1, y1)} in texture pixels, per cube of a bone (a list)."""
    out = []
    for b in geo["bones"]:
        if b["name"] != bone:
            continue
        for cube in b.get("cubes", []):
            rects = {"_origin": cube["origin"], "_size": cube["size"]}
            for face, f in cube.get("uv", {}).items():
                (u, v), (w, hh) = f["uv"], f["uv_size"]
                x0, x1 = sorted((u, u + w))
                y0, y1 = sorted((v, v + hh))
                rects[face] = (int(round(x0 * PX)), int(round(y0 * PX)), int(round(x1 * PX)), int(round(y1 * PX)))
            out.append(rects)
    return out


class Painter:
    """Paints colours on the fur pixels of UV rectangles, keeping the fur's own shading."""

    def __init__(self, base, fur_colour, fluff=None):
        self.a = np.asarray(base.convert("RGBA")).astype(np.int32)
        self.diff = self.a[..., :3] - np.array(FUR)
        self.fur = (np.abs(self.diff).sum(-1) < 40) & (self.a[..., 3] > 0)
        self.rng = np.random.default_rng(29)
        self.recolour(np.ones(self.fur.shape, bool), fur_colour, fluff)

    def recolour(self, mask, colour, fluff=None):
        m = mask & self.fur
        new = np.clip(np.array(colour) + self.diff, 0, 255)
        if fluff:
            speck = self.rng.random(m.shape) < fluff
            new = np.where(speck[..., None], np.clip(new + 34, 0, 255), new)
        self.a[..., :3] = np.where(m[..., None], new, self.a[..., :3])

    def rect(self, r, colour, rows=None, fluff=None, anything=False):
        """Colour a rectangle (rows: a (from, to) fraction of its height, 0 = top)."""
        x0, y0, x1, y1 = r
        if rows:
            h = y1 - y0
            y0, y1 = y0 + int(round(rows[0] * h)), y0 + int(round(rows[1] * h))
        mask = np.zeros(self.fur.shape, bool)
        mask[y0:y1, x0:x1] = True
        if anything:
            self.a[y0:y1, x0:x1, :3] = np.array(colour)
            self.a[y0:y1, x0:x1, 3] = 255
            return
        self.recolour(mask, colour, fluff)

    def ellipse(self, r, cx, cy, rx, ry, colour):
        """An ellipse inside a rectangle (cx, cy, rx, ry as fractions of its size)."""
        x0, y0, x1, y1 = r
        w, h = x1 - x0, y1 - y0
        yy, xx = np.mgrid[0:self.fur.shape[0], 0:self.fur.shape[1]]
        mask = ((xx - (x0 + cx * w)) / max(1, rx * w)) ** 2 + ((yy - (y0 + cy * h)) / max(1, ry * h)) ** 2 <= 1
        self.recolour(mask, colour)

    def image(self):
        return Image.fromarray(self.a.astype(np.uint8))


def pinguh(base, geo, look):
    """One Pinguh look on the guh texture: klassiek, keizer or pluis (the grey fluffy chick)."""
    if look == "pluis":
        back, belly, face, feet, beak = (150, 150, 160), (196, 198, 206), (246, 248, 252), (150, 130, 120), (60, 60, 70)
        p = Painter(base, back, fluff=0.08)
    else:
        back = (34, 38, 52) if look == "klassiek" else (30, 36, 56)
        belly, face, feet, beak = (246, 248, 252), (250, 250, 252), (250, 150, 50), (250, 140, 40)
        p = Painter(base, back)
    body = _faces(geo, "body")
    for cube in body:
        p.rect(cube["down"], belly, fluff=0.05 if look == "pluis" else None)
        p.rect(cube["north"], belly, fluff=0.05 if look == "pluis" else None)
        for side in ("east", "west"):
            p.rect(cube[side], belly, rows=(0.5, 1.0), fluff=0.05 if look == "pluis" else None)
        if look == "keizer":
            p.rect(cube["north"], (255, 222, 130), rows=(0.0, 0.4))        # a golden glow on the upper chest
    head = _faces(geo, "head")
    # the face: the big front cube (with the eyes) white under a dark cap; the cubes behind it white at the bottom
    front = min((c for c in head if c["_size"][0] >= 10), key=lambda c: c["_origin"][2])   # the frontmost big cube: the eyes
    for cube in head:
        if cube["_size"][0] <= 4:                             # the snoet: an orange beak
            for k, f in cube.items():
                if not k.startswith("_"):
                    p.rect(f, beak, anything=True)
            continue
        p.rect(cube["down"], face if look != "pluis" else belly)
        p.rect(cube["north"], face, rows=(0.55, 1.0))
    p.ellipse(front["north"], 0.5, 0.62, 0.52, 0.5, face)        # the white mask around the eyes
    if look == "keizer":
        for cube in head:
            for side in ("east", "west"):
                if cube["_size"][0] > 4:
                    p.ellipse(cube[side], 0.3, 0.35, 0.34, 0.3, (255, 196, 60))   # the golden ear patches
                    p.ellipse(cube[side], 0.3, 0.35, 0.18, 0.16, (255, 226, 120))
        # 2.9 polish: the keizer is recognisable from the front too: golden cheek patches on the face beside and under the
        # eyes (like a real emperor penguin's), a thin golden brow over the mask, and gold on the wide head cube that peeks
        # out beside the face.
        gold, gold_light, gold_deep = (255, 196, 60), (255, 228, 130), (246, 160, 40)
        p.ellipse(front["north"], 0.5, 0.2, 0.46, 0.14, gold)                # the brow (only the dark cap takes it)
        p.ellipse(front["north"], 0.5, 0.62, 0.52, 0.5, face)                # (the white mask again: the brow stays a rim)
        for cx in (0.04, 0.96):
            p.ellipse(front["north"], cx, 0.7, 0.11, 0.25, gold_deep)        # the cheek patches
            p.ellipse(front["north"], cx, 0.66, 0.07, 0.17, gold)
            p.ellipse(front["north"], cx, 0.62, 0.035, 0.09, gold_light)
        wide = max(head, key=lambda c: c["_size"][0])
        if wide is not front:
            p.rect(wide["north"], gold, rows=(0.0, 0.75))                     # the strips beside the face
    if look != "pluis":
        blush = (255, 168, 196)
        bx = 0.22 if look == "keizer" else 0.16                           # (a little inward, next to the gold)
        p.ellipse(front["north"], bx, 0.8, 0.1, 0.07, blush)
        p.ellipse(front["north"], 1 - bx, 0.8, 0.1, 0.07, blush)
    for leg in ("leg_back_left", "leg_back_right", "leg_front_left", "leg_front_right"):
        for cube in _faces(geo, leg):
            for k, f in cube.items():
                if not k.startswith("_"):
                    p.rect(f, feet, anything=True)
            p.rect(cube["down"], tuple(int(c * 0.8) for c in feet), anything=True)
    # the flippers (bones pinguh_vleugel_*: one swatch)
    for bone in ("pinguh_vleugel_links", "pinguh_vleugel_rechts"):
        for cube in _faces(geo, bone):
            r = cube["north"]
            flip = np.clip(np.array(back) + np.random.default_rng(3).integers(-6, 7, (r[3] - r[1], r[2] - r[0], 1)), 0, 255)
            if look == "pluis":
                flip = np.clip(flip + (np.random.default_rng(4).random((r[3] - r[1], r[2] - r[0], 1)) < 0.1) * 30, 0, 255)
            p.a[r[1]:r[3], r[0]:r[2], :3] = flip
            p.a[r[1]:r[3], r[0]:r[2], 3] = 255
    return p.image()


def pinguh_textures(h):
    geo = json.load(open(os.path.join(h.A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]
    base = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    for look, name in (("klassiek", "guh_pinguh.png"), ("keizer", "guh_pinguh_keizer.png"), ("pluis", "guh_pinguh_pluis.png")):
        h.save(pinguh(base, geo, look), "entity", name)


# =====================================================================================================================
def textures(h):
    rng = random.Random(29002)
    save = h.save
    wool = h.vanilla("block/pink_wool")
    save(rijpgras_top(rng), "block", "rijpgras_top.png")
    save(rijpgras_side(rng, wool), "block", "rijpgras_side.png")
    save(rijpgras_side(rng, wool, snowy=True), "block", "rijpgras_side_snowy.png")
    save(rijpsprietjes(rng), "block", "rijpsprietjes.png")
    save(ijsbloempje(rng), "block", "guh_ijsbloempje.png")
    save(polderijs(rng), "block", "polderijs.png")
    save(knotwilg_stam(rng), "block", "knotwilg_stam.png")
    save(knotwilg_stam_top(rng), "block", "knotwilg_stam_top.png")
    save(knotwilg_bladeren(random.Random(29010)), "block", "knotwilg_bladeren.png")
    save(knotwilg_bladeren(random.Random(29010), sneeuw=True), "block", "knotwilg_bladeren_sneeuw.png")
    save(sneeuw_top(rng), "block", "knotwilg_bladeren_sneeuw_top.png")
    save(ijspegelguh_kristal(rng), "block", "ijspegelguh_kristal.png")
    save(molentje_voor(rng), "block", "guh_molentje_voor.png")
    save(molentje_zijkant(rng), "block", "guh_molentje_zijkant.png")
    save(molentje_dak(rng), "block", "guh_molentje_dak.png")
    save(molentje_wiek(rng), "block", "guh_molentje_wiek.png")
    save(molentje_hout(rng), "block", "guh_molentje_hout.png")
    save(knabbelmeel(), "item", "knabbelmeel.png")
    for i in range(3):
        save(glinster(i), "particle", f"guhpolder_glinster_{i}.png")
    h.w(f"{h.A}/particles/guhpolder_glinster.json", {"textures": [f"guhs:guhpolder_glinster_{i}" for i in range(3)]})
    # 2.10.1: guh-sneeuw (sprites 0..3 flakes, 4 the guh head: GuhSneeuw.GUHKOP)
    for i in range(4):
        save(sneeuwvlok(i), "particle", f"guh_sneeuw_{i}.png")
    save(guhkop_sneeuw(), "particle", "guh_sneeuw_guhkop.png")
    h.w(f"{h.A}/particles/guh_sneeuw.json", {"textures": [f"guhs:guh_sneeuw_{i}" for i in range(4)] + ["guhs:guh_sneeuw_guhkop"]})
    pinguh_textures(h)
