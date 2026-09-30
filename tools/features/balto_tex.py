"""
Baltoguh en Nomguh (3.0 Guhverhalen, slice balto) - the textures of the blocks, items and particles: the Nomguh track snow,
the snowy roof tiles, the route marker pole (red and white, with a glowing guh-ear lamp), the medicine chest, the
sneeuwguhspar (bark, the sleepy guh face, dark blue-green needles with snow caps, the sapling), the Baltoguh-beeldje (bronze
and a snowy stone pedestal with a little plaque), the scent trail's paw prints and the white wolf-guh's sparkle.
All painted here (PIL / numpy); textures(h) writes them.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

from features import knuffeldal_tex as ktex

SNEEUW = (244, 247, 253)
SNEEUW_SCHADUW = (214, 224, 242)
IJS = (170, 214, 240)
BAST = (86, 60, 44)
BAST_DONKER = (58, 40, 32)
NAALD = (44, 88, 92)
NAALD_LICHT = (74, 128, 124)
NAALD_DONKER = (28, 60, 70)
ROOD = (214, 58, 66)
ROZE = ktex.PINK
BRONS = (176, 128, 84)
BRONS_LICHT = (222, 178, 118)
BRONS_DONKER = (112, 76, 52)
PATINA = (112, 176, 160)
EYE_DARK = ktex.EYE_DARK


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def noisy(base, var, rng, size=16):
    return ktex.noisy(base, var, rng, size)


def put(img, x, y, c, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), clamp(c) + (a,))


# =====================================================================================================================
# snow: the Nomguh track and the roof tiles
# =====================================================================================================================
def sneeuwspoor_top(rng):
    """Packed snow of the sled track: two runner grooves (a little blue in the shade), paw prints between them."""
    img = noisy((236, 241, 250), 5, rng)
    for x in (3, 12):
        for y in range(16):
            put(img, x, y, (200, 212, 234))
            put(img, x + 1, y, (216, 226, 242))
    for (px, py) in ((7, 2), (8, 7), (7, 12)):             # little guh paw prints (a pad and three toes)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            put(img, px + dx, py + dy + 1, (206, 214, 236))
        for dx in (-1, 1, 2):
            put(img, px + dx, py - 1, (210, 218, 238))
    return img


def sneeuwspoor_side(rng):
    img = noisy((236, 241, 250), 5, rng)
    for x in range(16):
        for y in range(10, 16):
            put(img, x, y, ktex.mix((228, 234, 246), (200, 210, 232), (y - 10) / 8 + rng.random() * 0.1))
    return img


def sneeuwdak_top(rng):
    """Snow on the roof: soft white with the bumps of the tiles under it."""
    img = noisy(SNEEUW, 4, rng)
    for y in (3, 11):
        for x in range(16):
            put(img, x, y, SNEEUW_SCHADUW if (x // 4 + y // 8) % 2 == 0 else (228, 234, 248))
    return img


def sneeuwdak_side(rng):
    """The roof tiles from the side: rounded raspberry-pink tiles, snow piled on the top rows and in the gaps."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            rij = y // 4
            sx = (x + (2 if rij % 2 else 0)) % 8
            base = (206, 86, 112) if (x + (2 if rij % 2 else 0)) // 8 % 2 else (224, 104, 128)
            shade = 0.82 + 0.18 * math.sin(math.pi * sx / 8)
            c = tuple(v * shade for v in base)
            if y % 4 == 3:
                c = (150, 60, 82)
            img.putpixel((x, y), clamp(ch + rng.randint(-5, 5) for ch in c) + (255,))
    for x in range(16):
        dik = 3 + (1 if rng.random() < 0.4 else 0) + (1 if 5 < x < 10 else 0)
        for y in range(dik):
            put(img, x, y, SNEEUW if y < dik - 1 else SNEEUW_SCHADUW)
    return img


# =====================================================================================================================
# the route marker, the medicine chest
# =====================================================================================================================
def routepaal(rng):
    """One sheet: the striped pole (x 0-3), the lamp's glowing ear (x 6-15, y 0-9) and its dark rim (y 10-15)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        c = ROOD if (y // 3) % 2 == 0 else (250, 250, 252)
        for x in range(4):
            put(img, x, y, tuple(v * (0.86 if x == 3 else 1.0) for v in c))
    for y in range(10):
        for x in range(6, 16):
            d = math.hypot(x - 10.5, y - 4.5) / 5.5
            put(img, x, y, ktex.mix((255, 252, 236), (170, 220, 255), min(1, d)))
    for y in range(10, 16):
        for x in range(6, 16):
            put(img, x, y, (60, 64, 80) if (x + y) % 5 else (90, 96, 118))
    return img


def medicijnkist(rng):
    """The little medicine chest: warm wood, brass corners, a white lid band and a pink guh heart with a plus."""
    img = noisy((170, 116, 72), 8, rng)
    for y in range(16):
        for x in range(16):
            if y in (0, 15) or x in (0, 15):
                put(img, x, y, (120, 78, 48))
            if y in (4, 5):
                put(img, x, y, (246, 240, 232))
    for (x, y) in ((0, 0), (1, 0), (0, 1), (14, 0), (15, 0), (15, 1), (0, 14), (0, 15), (1, 15), (15, 14), (14, 15), (15, 15)):
        put(img, x, y, (232, 190, 90))
    # the heart (two round guh-ear bumps) with a white plus
    heart = [(5, 7), (6, 7), (9, 7), (10, 7), (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8),
             (4, 9), (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9), (11, 9), (5, 10), (6, 10), (7, 10), (8, 10), (9, 10),
             (10, 10), (6, 11), (7, 11), (8, 11), (9, 11), (7, 12), (8, 12)]
    for (x, y) in heart:
        put(img, x, y, (238, 110, 160))
    for (x, y) in ((7, 8), (8, 8), (7, 9), (8, 9), (6, 9), (9, 9), (7, 10), (8, 10)):
        put(img, x, y, (255, 250, 252))
    put(img, 5, 7, (255, 170, 205))
    return img


# =====================================================================================================================
# the sneeuwguhspar
# =====================================================================================================================
def bast(rng, frost=True):
    """Dark reddish bark in long plates, a little frost in the cracks."""
    img = noisy(BAST, 7, rng)
    for x in range(0, 16, 4):
        off = rng.randint(0, 3)
        for y in range(16):
            if (y + off) % 7 < 5:
                put(img, x, y, BAST_DONKER)
    if frost:
        for _ in range(10):
            put(img, rng.randint(0, 15), rng.randint(0, 15), (214, 226, 240))
    return img


def bast_top(rng):
    img = noisy((196, 150, 108), 6, rng)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7:
                put(img, x, y, BAST_DONKER)
            elif int(d) % 3 == 0:
                put(img, x, y, (160, 116, 80))
    return img


def bast_gezicht(rng):
    """The bark with a sleepy guh face: closed curved eyes with lashes, pink blush, a tiny snoet and a little smile."""
    img = bast(rng, frost=False)
    for y in range(4, 13):                               # a lighter oval where the face is (worn smooth)
        for x in range(2, 14):
            if ((x - 7.5) / 6) ** 2 + ((y - 8.5) / 4.8) ** 2 <= 1:
                put(img, x, y, ktex.mix(BAST, (150, 108, 80), 0.6))
    for ex in (3, 9):                                    # sleepy eyes ^^
        for dx, dy in ((0, 7), (1, 8), (2, 8), (3, 7)):
            put(img, ex + dx, dy, EYE_DARK)
        put(img, ex + 1, 9, EYE_DARK)
    for (x, y) in ((2, 10), (3, 10), (12, 10), (13, 10)):
        put(img, x, y, (236, 136, 160))
    put(img, 7, 10, (220, 100, 140))
    put(img, 8, 10, (220, 100, 140))
    for (x, y) in ((6, 11), (7, 12), (8, 12), (9, 11)):
        put(img, x, y, (70, 42, 36))
    return img


def naalden(rng, sneeuw=False):
    """Needles: dark blue-green tufts on a see-through sheet (cutout); with snow: a white cap on the top rows."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for _ in range(90):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        c = rng.choice((NAALD, NAALD, NAALD_LICHT, NAALD_DONKER))
        put(img, x, y, c)
        if rng.random() < 0.6:
            put(img, x + rng.choice((-1, 1)), y + 1, NAALD_DONKER)
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] == 0 and rng.random() < 0.55:
                put(img, x, y, ktex.mix(NAALD, NAALD_DONKER, rng.random()))
    if sneeuw:
        for x in range(16):
            dik = 3 + rng.randint(0, 2)
            for y in range(dik):
                put(img, x, y, SNEEUW if y < dik - 1 else SNEEUW_SCHADUW)
            if rng.random() < 0.3:
                put(img, x, dik, SNEEUW_SCHADUW)
    return img


def sneeuw_top(rng):
    img = noisy(SNEEUW, 4, rng)
    for _ in range(6):
        put(img, rng.randint(0, 15), rng.randint(0, 15), (255, 255, 255))
    return img


def zaailing(rng):
    """A tiny spruce with a snow cap and a sleepy face on its little trunk."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(11, 16):
        put(img, 7, y, BAST)
        put(img, 8, y, BAST_DONKER)
    for i, (y, half) in enumerate(((2, 1), (3, 2), (4, 3), (5, 2), (6, 3), (7, 4), (8, 3), (9, 4), (10, 5))):
        for x in range(8 - half, 8 + half):
            put(img, x, y, NAALD if (x + y) % 3 else NAALD_LICHT)
    for (x, y) in ((7, 1), (8, 1), (6, 2), (7, 2), (8, 2), (9, 2), (5, 4), (10, 4), (4, 7), (11, 7)):
        put(img, x, y, SNEEUW)
    put(img, 7, 13, EYE_DARK)
    put(img, 8, 13, EYE_DARK)
    return img


# =====================================================================================================================
# the Baltoguh-beeldje
# =====================================================================================================================
def brons(rng):
    """Warm polished bronze with a soft sheen and a hint of green patina in the corners."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            t = 0.5 + 0.5 * math.sin((x + y * 0.6) * 0.45)
            c = ktex.mix(BRONS, BRONS_LICHT, t * 0.55)
            if rng.random() < 0.05:
                c = ktex.mix(c, PATINA, 0.5)
            img.putpixel((x, y), clamp(v + rng.randint(-4, 4) for v in c) + (255,))
    return img


def brons_gezicht(rng):
    """The bronze face of the statue: eyes, snoet and the proud little smile, polished lighter."""
    img = brons(rng)
    for ex in (3, 10):
        for dx in range(3):
            for dy in range(3):
                put(img, ex + dx, 6 + dy, BRONS_DONKER)
        put(img, ex + 1, 6, BRONS_LICHT)
    for (x, y) in ((7, 10), (8, 10)):
        put(img, x, y, (92, 60, 44))
    for (x, y) in ((6, 12), (7, 13), (8, 13), (9, 12)):
        put(img, x, y, BRONS_DONKER)
    return img


def sokkel(rng):
    """The pedestal: snowy grey stone blocks, a bronze plaque in the middle of each side."""
    img = noisy((168, 170, 180), 8, rng)
    for x in range(16):
        put(img, x, 0, SNEEUW)
        put(img, x, 1, SNEEUW if rng.random() < 0.7 else SNEEUW_SCHADUW)
    for y in range(16):
        put(img, 0, y, (140, 142, 154))
        put(img, 15, y, (140, 142, 154))
    for y in range(6, 12):
        for x in range(4, 12):
            put(img, x, y, BRONS if 4 < x < 11 and 6 < y < 11 else BRONS_DONKER)
    for x in range(6, 10, 2):
        put(img, x, 8, BRONS_DONKER)
        put(img, x + 1, 9, BRONS_DONKER)
    return img


# =====================================================================================================================
# particles
# =====================================================================================================================
def pootje(i):
    """A glowing paw print (a pad and three toe beans), soft pink-white with a gentle halo."""
    s = 8
    img = Image.new("RGBA", (16 * s, 16 * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    kern = [(255, 214, 232), (255, 236, 244), (236, 246, 255)][i]
    halo = [(255, 170, 205), (255, 196, 222), (170, 214, 255)][i]
    for r, a in ((6.5, 70), (5.5, 120)):
        d.ellipse([(8 - r) * s, (10 - r * 0.8) * s, (8 + r) * s, (10 + r * 0.8) * s], fill=halo + (a,))
    d.ellipse([4.6 * s, 8.2 * s, 11.4 * s, 13.6 * s], fill=kern + (255,))
    for (cx, cy) in ((4.4, 5.6), (8.0, 3.8), (11.6, 5.6)):
        d.ellipse([(cx - 1.6) * s, (cy - 1.8) * s, (cx + 1.6) * s, (cy + 1.8) * s], fill=kern + (255,))
    return img.resize((16, 16), Image.LANCZOS)


def glans(i):
    """A four-pointed white-blue sparkle (sizes 0..3)."""
    s = 8
    img = Image.new("RGBA", (16 * s, 16 * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = [3.0, 4.5, 6.0, 7.2][i]
    c = (236, 246, 255)
    d.polygon([(8 * s, (8 - r) * s), (8.8 * s, 7.2 * s), ((8 + r) * s, 8 * s), (8.8 * s, 8.8 * s), (8 * s, (8 + r) * s),
               (7.2 * s, 8.8 * s), ((8 - r) * s, 8 * s), (7.2 * s, 7.2 * s)], fill=c + (255,))
    d.ellipse([6.6 * s, 6.6 * s, 9.4 * s, 9.4 * s], fill=(255, 255, 255, 255))
    d.ellipse([5 * s, 5 * s, 11 * s, 11 * s], outline=(170, 214, 255, 90), width=s)
    return img.resize((16, 16), Image.LANCZOS)


def textures(h):
    rng = random.Random(20300701)
    save = h.save
    save(sneeuwspoor_top(rng), "block", "nomguh_sneeuwspoor_top.png")
    save(sneeuwspoor_side(rng), "block", "nomguh_sneeuwspoor_side.png")
    save(sneeuwdak_top(rng), "block", "nomguh_sneeuwdak_top.png")
    save(sneeuwdak_side(rng), "block", "nomguh_sneeuwdak_side.png")
    save(routepaal(rng), "block", "nomguh_routepaal.png")
    save(medicijnkist(rng), "block", "nomguh_medicijnkist.png")
    save(bast(rng), "block", "sneeuwguhspar_stam.png")
    save(bast_top(rng), "block", "sneeuwguhspar_stam_top.png")
    save(bast_gezicht(rng), "block", "sneeuwguhspar_gezicht.png")
    save(naalden(random.Random(7011)), "block", "sneeuwguhspar_naalden.png")
    save(naalden(random.Random(7011), sneeuw=True), "block", "sneeuwguhspar_naalden_sneeuw.png")
    save(sneeuw_top(rng), "block", "sneeuwguhspar_naalden_sneeuw_top.png")
    save(zaailing(rng), "block", "sneeuwguhspar_zaailing.png")
    save(brons(rng), "block", "baltoguh_beeldje_brons.png")
    save(brons_gezicht(rng), "block", "baltoguh_beeldje_gezicht.png")
    save(sokkel(rng), "block", "baltoguh_beeldje_sokkel.png")
    for i in range(3):
        save(pootje(i), "particle", f"balto_snuffel_{i}.png")
    for i in range(4):
        save(glans(i), "particle", f"balto_wolfglans_{i}.png")
    h.w(f"{h.A}/particles/balto_snuffel.json", {"textures": [f"guhs:balto_snuffel_{i}" for i in range(3)]})
    h.w(f"{h.A}/particles/balto_wolfglans.json", {"textures": [f"guhs:balto_wolfglans_{i}" for i in range(4)]})
