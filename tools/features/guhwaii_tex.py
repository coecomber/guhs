"""
Guhwai'i (3.0, slice guhwaii): the block, item, painting and poster textures (all drawn here, 16 px per block unless said).

  palm        the guh-palm's bark (scaly bands), its rings on top, the guh face on the trunk (and winking), the fronds, the
              sprouting coconut, the planks
  kokosnoot   green, half-ripe and brown coconuts (their three "eyes" make a little guh face), the item icon, the kokosmelk bottle
  bloemen     roze hibiscus, plumeria, paradijsvogelbloem, orchidee (cross textures)
  eitjes      the Schilly-eitjes (pale green with dark-green spots) and their cracks
  scanner     626's vadsigheid-scanner: the scan plate, the alien metal, the screen with the meter
  poster      THE picture: VADSIGHEIDSNIVEAU breaking through to ONBEREKENBAAR VAHOEG (poster 64 px, painting 2x2 128 px)
  ukelele     the little ukelele (wood, sound hole, strings)
A tiny 3x5 pixel font writes the Dutch words on the poster and the screen.
"""
import math
import random

import numpy as np
from PIL import Image

# --- colours ------------------------------------------------------------------------------------------------------------
BARK = (170, 128, 82)
BARK_DARK = (120, 84, 50)
BARK_LIGHT = (206, 166, 112)
FROND = (84, 176, 74)
FROND_DARK = (46, 120, 52)
FROND_LIGHT = (150, 220, 104)
KOKOS_GROEN = (112, 162, 64)
KOKOS_BRUIN = (122, 80, 46)
KOKOS_HALF = (150, 136, 60)
EYE = (34, 24, 52)
BLUSH = (255, 150, 188)
WHITE = (255, 255, 255)
SCHERM = (10, 22, 46)
CYAN = (95, 224, 255)
METER = [(92, 224, 138), (139, 224, 92), (196, 224, 92), (240, 216, 92), (248, 176, 76), (248, 136, 76), (248, 106, 106),
         (248, 92, 156), (232, 92, 216), (180, 92, 248)]
ROZE = (255, 140, 200)


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def img(size=16, colour=(0, 0, 0, 0)):
    return Image.new("RGBA", (size, size), colour)


def noisy(base, var, rng, size=16):
    a = np.zeros((size, size, 4), np.uint8)
    for y in range(size):
        for x in range(size):
            a[y, x, :3] = clamp(c + rng.randint(-var, var) for c in base)
            a[y, x, 3] = 255
    return Image.fromarray(a)


def put(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), clamp(c[:3]) + ((255,) if len(c) == 3 else (int(c[3]),)))


def disc(im, cx, cy, r, c):
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                put(im, x, y, c)


# --- a 3x5 pixel font ---------------------------------------------------------------------------------------------------------
FONT = {
    "A": ["010", "101", "111", "101", "101"], "B": ["110", "101", "110", "101", "110"], "D": ["110", "101", "101", "101", "110"],
    "E": ["111", "100", "110", "100", "111"], "G": ["011", "100", "101", "101", "011"], "H": ["101", "101", "111", "101", "101"],
    "I": ["111", "010", "010", "010", "111"], "J": ["001", "001", "001", "101", "010"], "K": ["101", "101", "110", "101", "101"],
    "L": ["100", "100", "100", "100", "111"], "M": ["10001", "11011", "10101", "10001", "10001"], "N": ["1001", "1101", "1011", "1001", "1001"],
    "O": ["010", "101", "101", "101", "010"], "R": ["110", "101", "110", "101", "101"], "S": ["011", "100", "010", "001", "110"],
    "T": ["111", "010", "010", "010", "010"], "U": ["101", "101", "101", "101", "111"], "V": ["101", "101", "101", "101", "010"],
    "W": ["101", "101", "111", "111", "101"], "!": ["1", "1", "1", "0", "1"], " ": ["00", "00", "00", "00", "00"],
    "6": ["011", "100", "111", "101", "111"], "2": ["110", "001", "010", "100", "111"], "%": ["101", "001", "010", "100", "101"],
    "?": ["110", "001", "010", "000", "010"], "C": ["011", "100", "100", "100", "011"], "P": ["110", "101", "110", "100", "100"],
}


def tekst_breedte(s, schaal=1):
    return sum((len(FONT[ch][0]) + 1) * schaal for ch in s) - schaal


def tekst(im, s, x, y, c, schaal=1, schaduw=None):
    for ch in s:
        g = FONT[ch]
        for gy, row in enumerate(g):
            for gx, bit in enumerate(row):
                if bit == "1":
                    for sx in range(schaal):
                        for sy in range(schaal):
                            if schaduw:
                                put(im, x + gx * schaal + sx + schaal, y + gy * schaal + sy + schaal, schaduw)
                            put(im, x + gx * schaal + sx, y + gy * schaal + sy, c)
        x += (len(g[0]) + 1) * schaal


def midden_tekst(im, s, y, c, schaal=1, schaduw=None):
    tekst(im, s, (im.width - tekst_breedte(s, schaal)) // 2, y, c, schaal, schaduw)


# --- a guh face (the palm's, the coconut's) -------------------------------------------------------------------------------
def gezicht(im, x0, y0, knipoog=False, blij=True, klein=False):
    """A guh face in a 10x7 box at (x0, y0): two big round eyes with a shine, blush, a little smile."""
    for i, ex in enumerate((x0 + 1, x0 + 6)):
        if knipoog and i == 1:
            for dx, dy in ((0, 2), (1, 3), (2, 3), (3, 2)):
                put(im, ex + dx, y0 + dy, EYE)
            continue
        for dy in range(4):
            for dx in range(3 if klein else 3):
                put(im, ex + dx, y0 + dy, EYE)
        put(im, ex, y0, WHITE)
        put(im, ex + 2, y0 + 3, (64, 132, 214))
    put(im, x0, y0 + 5, BLUSH)
    put(im, x0 + 9, y0 + 5, BLUSH)
    if blij:
        for dx, dy in ((3, 5), (4, 6), (5, 6), (6, 5)):
            put(im, x0 + dx, y0 + dy, (150, 72, 116))


# =====================================================================================================================
# the guh-palm
# =====================================================================================================================
def palm_stam(rng):
    """Scaly bark: overlapping horizontal bands, each a bit darker at its lower edge (like real palm trunks)."""
    im = noisy(BARK, 8, rng)
    for y in range(16):
        band = y % 4
        for x in range(16):
            c = im.getpixel((x, y))[:3]
            if band == 3:
                c = clamp(v * 0.7 for v in c)
            elif band == 0:
                c = clamp(v * 1.12 for v in c)
            if (x + (y // 4) * 3) % 8 == 0 and band in (1, 2):
                c = clamp(v * 0.85 for v in c)
            put(im, x, y, c)
    return im


def palm_stam_top(rng):
    im = noisy(BARK_LIGHT, 6, rng)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7:
                put(im, x, y, clamp(v * 0.7 for v in BARK))
            elif int(d) % 3 == 0:
                put(im, x, y, clamp(v * 0.85 for v in BARK_LIGHT))
    return im


def palm_gezicht(rng, knipoog=False):
    im = palm_stam(random.Random(rng.random()))
    # a lighter oval round the face (the palm "smiles" out of its bark)
    for y in range(3, 14):
        for x in range(2, 14):
            if ((x - 7.5) / 6.2) ** 2 + ((y - 8) / 5.6) ** 2 <= 1:
                put(im, x, y, clamp(v * 1.08 + 6 for v in im.getpixel((x, y))[:3]))
    gezicht(im, 3, 5, knipoog=knipoog)
    return im


def palm_blad(rng):
    """Fronds: long leaflets fanning from a midrib, see-through between them (cutout)."""
    im = img()
    for y in range(16):
        for x in range(16):
            # diagonal leaflets
            if (x + y) % 4 in (0, 1) or (x - y) % 5 == 0:
                c = FROND if (x + y) % 4 == 0 else FROND_DARK
                if rng.random() < 0.15:
                    c = FROND_LIGHT
                put(im, x, y, clamp(v + rng.randint(-8, 8) for v in c))
    for x in range(16):     # the midrib
        put(im, x, 7, (170, 200, 90))
        put(im, x, 8, FROND_DARK)
    return im


def kiemplant(rng):
    """A coconut in the sand with a green sprout (cross texture)."""
    im = img()
    for y in range(9, 16):
        for x in range(4, 12):
            if ((x - 7.5) / 4) ** 2 + ((y - 12.5) / 3.6) ** 2 <= 1:
                put(im, x, y, clamp(v + rng.randint(-10, 10) for v in KOKOS_BRUIN))
    put(im, 6, 12, EYE)
    put(im, 9, 12, EYE)
    put(im, 5, 14, BLUSH)
    put(im, 10, 14, BLUSH)
    for y in range(3, 10):
        put(im, 8, y, FROND_DARK)
    for i, (x, y) in enumerate(((7, 4), (6, 3), (5, 3), (4, 4), (9, 5), (10, 4), (11, 4), (12, 5), (7, 6), (6, 6), (9, 7), (10, 7))):
        put(im, x, y, FROND if i % 2 else FROND_LIGHT)
    return im


def planken(rng):
    im = noisy((214, 178, 120), 7, rng)
    for y in range(16):
        for x in range(16):
            if y % 4 == 3 or (x + (y // 4) * 5) % 8 == 7:
                put(im, x, y, clamp(v * 0.78 for v in im.getpixel((x, y))[:3]))
    return im


# =====================================================================================================================
# kokosnoot, kokosmelk
# =====================================================================================================================
def kokos(rng, kleur):
    """A hairy coconut skin; its three 'eyes' are a little guh face (two eyes and a mouth), with blush."""
    im = noisy(kleur, 14, rng)
    for i in range(40):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        put(im, x, y, clamp(v * 0.75 for v in kleur))
    gezicht(im, 3, 5)
    return im


def kokos_item(rng):
    im = img()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) / 6.4) ** 2 + ((y - 8.5) / 6.2) ** 2
            if d <= 1:
                c = clamp(v * (1.15 - 0.3 * (y / 16)) + rng.randint(-10, 10) for v in KOKOS_BRUIN)
                put(im, x, y, c)
    for x in range(3, 13):
        put(im, x, 2 if x in (6, 7, 8, 9) else 3, clamp(v * 0.7 for v in KOKOS_BRUIN))
    gezicht(im, 3, 6)
    # a little green leaf on top
    for (x, y) in ((8, 1), (9, 0), (10, 0), (10, 1)):
        put(im, x, y, FROND)
    return im


def kokosmelk(rng):
    """A glass bottle of white coconut milk with a coconut-brown band and a pink straw."""
    im = img()
    glas = (206, 232, 246, 200)
    for y in range(4, 16):
        for x in range(4, 12):
            if y == 4 and x in (4, 11):
                continue
            put(im, x, y, glas)
    for y in range(6, 15):
        for x in range(5, 11):
            put(im, x, y, clamp(v - rng.randint(0, 8) for v in (250, 246, 236)))
    for x in range(5, 11):
        put(im, x, 10, KOKOS_BRUIN)
        put(im, x, 11, clamp(v * 0.8 for v in KOKOS_BRUIN))
    for y in range(1, 5):
        put(im, 6, y, (255, 120, 180))
    put(im, 6, 1, (255, 170, 210))
    put(im, 7, 1, (255, 120, 180))
    put(im, 9, 7, WHITE)
    return im


# =====================================================================================================================
# flowers
# =====================================================================================================================
def _stengel(im, x, y0, y1, c=FROND_DARK):
    for y in range(y0, y1):
        put(im, x, y, c)


def hibiscus(rng):
    """Big pink hibiscus: five round petals, a dark-pink heart and a long yellow stamen with a pink tip."""
    im = img()
    _stengel(im, 8, 9, 16)
    for (x, y) in ((6, 13), (5, 12), (10, 12), (11, 11)):
        put(im, x, y, FROND)
    petal = (255, 120, 180)
    for a in range(5):
        ang = a / 5 * math.tau - math.pi / 2
        cx, cy = 8 + math.cos(ang) * 3.2, 6 + math.sin(ang) * 3.0
        disc(im, cx, cy, 2.4, clamp(v + rng.randint(-8, 8) for v in petal))
    disc(im, 8, 6, 1.6, (210, 40, 110))
    for (x, y) in ((9, 5), (10, 4), (11, 3)):
        put(im, x, y, (255, 226, 90))
    put(im, 12, 2, (255, 70, 140))
    return im


def plumeria(rng):
    """Plumeria (frangipani): white star petals with a sunny yellow heart; a small bunch of them."""
    im = img()
    _stengel(im, 7, 9, 16)
    _stengel(im, 10, 11, 16)
    for (cx, cy, r) in ((6, 6, 3.4), (11, 9, 2.6)):
        for a in range(5):
            ang = a / 5 * math.tau
            for t in np.linspace(0, r, 6):
                put(im, round(cx + math.cos(ang + 0.3) * t), round(cy + math.sin(ang + 0.3) * t), (252, 250, 240))
                put(im, round(cx + math.cos(ang) * t * 0.8), round(cy + math.sin(ang) * t * 0.8), (252, 248, 236))
        disc(im, cx, cy, 1.1, (255, 214, 70))
    return im


def paradijsbloem(rng):
    """Paradijsvogelbloem: an orange crest over a blue-purple 'beak' on a green stalk (it looks like a little bird)."""
    im = img()
    _stengel(im, 6, 8, 16)
    for x in range(6, 14):                 # the green sheath (the bird's beak)
        put(im, x, 8 + (x - 6) // 4, FROND_DARK)
        put(im, x, 9 + (x - 6) // 4, FROND)
    for (x, y) in ((7, 7), (8, 6), (9, 5), (10, 4), (11, 3), (8, 5), (9, 4), (10, 3), (11, 2), (12, 2), (9, 6), (10, 5), (12, 3)):
        put(im, x, y, (255, 150, 40) if (x + y) % 2 else (255, 186, 60))
    for (x, y) in ((10, 7), (11, 6), (12, 6), (13, 7)):
        put(im, x, y, (90, 90, 220))
    return im


def orchidee(rng):
    """A purple orchid: two flowers on an arching stem, a darker lip with a yellow spot."""
    im = img()
    for y in range(6, 16):
        put(im, 5 + (15 - y) // 4, y, FROND_DARK)
    for (cx, cy) in ((8, 5), (11, 9)):
        for (dx, dy) in ((-2, 0), (2, 0), (0, -2), (-1, -1), (1, -1), (-1, 1), (1, 1), (-2, -1), (2, -1)):
            put(im, cx + dx, cy + dy, (206, 120, 240))
        put(im, cx, cy, (150, 60, 190))
        put(im, cx, cy + 1, (150, 60, 190))
        put(im, cx, cy - 1, (255, 220, 90))
    for (x, y) in ((4, 12), (3, 13), (6, 14), (7, 13)):
        put(im, x, y, FROND)
    return im


# =====================================================================================================================
# eggs
# =====================================================================================================================
def eitje(rng, barst=0):
    im = noisy((214, 236, 196), 6, rng)
    for _ in range(12):
        x, y = rng.randint(0, 14), rng.randint(0, 14)
        put(im, x, y, (92, 158, 96))
        if rng.random() < 0.5:
            put(im, x + 1, y, (120, 180, 110))
    if barst >= 1:
        for (x, y) in ((7, 2), (8, 3), (7, 4), (8, 5), (9, 6), (8, 7)):
            put(im, x, y, (60, 70, 60))
    if barst >= 2:
        for (x, y) in ((3, 8), (4, 9), (5, 9), (6, 10), (11, 9), (12, 10), (11, 11), (12, 12)):
            put(im, x, y, (60, 70, 60))
    return im


# =====================================================================================================================
# the scanner
# =====================================================================================================================
def scanner_plaat(rng):
    """The scan plate from above: round cyan rings on dark alien metal, a guh-paw print in the middle."""
    im = noisy((70, 86, 120), 5, rng)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 6.2 < d < 7.3 or 3.6 < d < 4.4:
                put(im, x, y, CYAN)
            elif d <= 3.6:
                put(im, x, y, (40, 120, 170))
    for (x, y) in ((7, 8), (8, 8), (7, 9), (8, 9), (5, 6), (10, 6), (6, 5), (9, 5)):
        put(im, x, y, (200, 246, 255))
    return im


def scanner_metaal(rng):
    """626's capsule metal: blue-grey panels with rivets and a thin cyan light line."""
    im = noisy((104, 122, 156), 6, rng)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                put(im, x, y, (72, 86, 116))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(im, x, y, (190, 200, 220))
    for x in range(3, 13):
        put(im, x, 8, CYAN)
    return im


def scanner_scherm(rng, size=32):
    """The screen: dark with the meter, the colour steps climbing, the needle and 'VADS' on top."""
    im = Image.new("RGBA", (size, size), SCHERM + (255,))
    s = size // 16
    for y in range(size):
        if y % 3 == 0:
            for x in range(size):
                put(im, x, y, (16, 32, 60))
    # frame
    for x in range(size):
        put(im, x, 0, CYAN)
        put(im, x, size - 1, CYAN)
    for y in range(size):
        put(im, 0, y, CYAN)
        put(im, size - 1, y, CYAN)
    # the meter: a column in the middle, 10 colour steps
    bx0, bx1 = size // 2 - 3 * s, size // 2 + 3 * s
    seg = (size - 7 * s) / 10
    for i, c in enumerate(METER):
        y1 = round(size - 3 * s - i * seg)
        y0 = round(size - 3 * s - (i + 1) * seg)
        for y in range(y0, y1):
            for x in range(bx0, bx1):
                put(im, x, y, c)
    # the needle over the top, a spark
    for x in range(bx0 - 2 * s, bx1 + 2 * s):
        put(im, x, 3 * s, WHITE)
    for (dx, dy) in ((0, -1), (1, -2), (-1, -2), (2, -1), (-2, -1)):
        put(im, size // 2 + dx * s, 3 * s + dy * s, (255, 230, 120))
    return im


# =====================================================================================================================
# the poster / painting: ONBEREKENBAAR VAHOEG
# =====================================================================================================================
def poster(size=64, rng=None):
    """The famous picture: a screen with VADSIGHEIDSNIVEAU, the meter broken through the top in a burst of sparks, a happy
    guh head peeking in, and ONBEREKENBAAR VAHOEG!!! in big pink letters."""
    rng = rng or random.Random(3001)
    s = size // 64
    im = Image.new("RGBA", (size, size), (14, 26, 54, 255))
    for y in range(0, size, 2 * s):
        for x in range(size):
            put(im, x, y, (18, 34, 66))
    for x in range(size):
        for k in range(s):
            put(im, x, k, CYAN)
            put(im, x, size - 1 - k, CYAN)
    for y in range(size):
        for k in range(s):
            put(im, k, y, CYAN)
            put(im, size - 1 - k, y, CYAN)
    midden_tekst(im, "VADSIGHEIDS", 3 * s, CYAN, s)
    midden_tekst(im, "NIVEAU", 9 * s, CYAN, s)
    # the meter (left of the middle), broken through its top
    bx0, bx1, top, bottom = 10 * s, 20 * s, 22 * s, 50 * s
    for i, c in enumerate(METER):
        y1 = bottom - i * (bottom - top) // 10
        y0 = y1 - (bottom - top) // 10 + s
        for y in range(y0, y1):
            for x in range(bx0, bx1):
                put(im, x, y, c)
    for y in range(top - s, bottom + s):
        for k in range(s):
            put(im, bx0 - s - k, y, WHITE)
            put(im, bx1 + k, y, WHITE)
    for x in range(bx0 - 2 * s, bx1 + 2 * s):      # the bottom
        for k in range(s):
            put(im, x, bottom + s + k, WHITE)
    # the burst: sparks in meter colours out of the top
    cx, cy = (bx0 + bx1) // 2, top - 2 * s
    for k in range(26):
        a = -math.pi * (0.1 + 0.8 * rng.random())
        r = (4 + rng.random() * 12) * s
        x, y = cx + math.cos(a) * r, cy + math.sin(a) * r
        c = METER[k % 10]
        for dx in range(s):
            for dy in range(s):
                put(im, int(x) + dx, int(y) + dy, c)
    for k in range(-3, 4):                          # the needle, flying off
        for dy in range(s):
            put(im, cx + k * s + s * 3, cy - 12 * s + k * s // 2 + dy, WHITE)
    # a happy guh head peeking in on the right
    gx, gy, R = 44 * s, 36 * s, 9 * s
    for y in range(gy - R - 3 * s, gy + R):
        for x in range(gx - R - s, gx + R + s):
            d = ((x - gx) / (R + 0.5)) ** 2 + ((y - gy) / (R * 0.86 + 0.5)) ** 2
            if d <= 1:
                put(im, x, y, (250, 178, 214))
    for ex in (-1, 1):
        disc(im, gx + ex * 0.62 * R, gy - 0.8 * R, 0.3 * R, (250, 178, 214))
        disc(im, gx + ex * 0.62 * R, gy - 0.8 * R, 0.15 * R, (238, 120, 170))
        disc(im, gx + ex * 0.42 * R, gy - 0.05 * R, 0.22 * R, EYE)
        disc(im, gx + ex * 0.42 * R - 0.08 * R, gy - 0.12 * R, 0.07 * R + 0.3, WHITE)
        disc(im, gx + ex * 0.66 * R, gy + 0.32 * R, 0.12 * R, BLUSH)
    for k in range(-2, 3):
        put(im, gx + k * s // 1, gy + int(0.42 * R) + (abs(k) == 2) * -s, (150, 72, 116))
    # the words
    midden_tekst(im, "ONBEREKENBAAR", 53 * s, ROZE, s, schaduw=(60, 20, 60))
    midden_tekst(im, "VAHOEG!!!", 58 * s, (255, 224, 112), s, schaduw=(60, 20, 60))
    return im


# =====================================================================================================================
# the ukelele
# =====================================================================================================================
def ukelele_hout(rng):
    im = noisy((226, 162, 96), 8, rng)
    for y in range(16):
        for x in range(16):
            if (y + x // 5) % 5 == 0:
                put(im, x, y, clamp(v * 0.9 for v in im.getpixel((x, y))[:3]))
    return im


def ukelele_voor(rng):
    """The front of the body: light wood, a dark round sound hole with a pink hibiscus sticker, the bridge and 4 strings."""
    im = ukelele_hout(rng)
    disc(im, 7.5, 6.5, 2.4, (50, 30, 20))
    disc(im, 11.5, 11.5, 1.6, (255, 120, 180))
    put(im, 11, 11, (210, 40, 110))
    for x in range(5, 11):
        put(im, x, 12, (90, 56, 30))
    for x in (6, 7, 8, 9):
        for y in range(0, 12):
            put(im, x, y, (240, 236, 220))
    return im


def ukelele_hals(rng):
    im = noisy((96, 62, 36), 6, rng)
    for y in range(0, 16, 3):
        for x in range(16):
            put(im, x, y, (200, 200, 210))
    return im


def textures(h):
    rng = random.Random(30111)
    save = h.save
    save(palm_stam(rng), "block", "guhwaii_palm_stam.png")
    save(palm_stam_top(rng), "block", "guhwaii_palm_stam_top.png")
    save(palm_gezicht(random.Random(30112)), "block", "guhwaii_palm_gezicht.png")
    save(palm_gezicht(random.Random(30112), knipoog=True), "block", "guhwaii_palm_gezicht_knipoog.png")
    save(palm_blad(rng), "block", "guhwaii_palm_blad.png")
    save(kiemplant(rng), "block", "guhwaii_palm_kiemplant.png")
    save(planken(rng), "block", "guhwaii_palm_planken.png")
    for naam, kleur in (("groen", KOKOS_GROEN), ("half", KOKOS_HALF), ("bruin", KOKOS_BRUIN)):
        save(kokos(random.Random(30113), kleur), "block", f"kokosnoot_{naam}.png")
    save(kokos_item(random.Random(30114)), "item", "kokosnoot.png")
    save(kokosmelk(random.Random(30115)), "item", "kokosmelk.png")
    save(hibiscus(random.Random(30116)), "block", "roze_hibiscus.png")
    save(plumeria(random.Random(30117)), "block", "guhwaii_plumeria.png")
    save(paradijsbloem(random.Random(30118)), "block", "guhwaii_paradijsbloem.png")
    save(orchidee(random.Random(30119)), "block", "guhwaii_orchidee.png")
    for i in range(3):
        save(eitje(random.Random(30120), i), "block", f"schilly_eitje_{i}.png")
    save(scanner_plaat(random.Random(30121)), "block", "vadsigheid_scanner_plaat.png")
    save(scanner_metaal(random.Random(30122)), "block", "vadsigheid_scanner_metaal.png")
    save(scanner_scherm(random.Random(30123)), "block", "vadsigheid_scanner_scherm.png")
    save(poster(64), "block", "vadsigheid_poster.png")
    save(poster(128), "painting", "vadsigheidsniveau.png")
    save(ukelele_hout(random.Random(30124)), "item", "guhwaii_ukelele_hout.png")
    save(ukelele_voor(random.Random(30124)), "item", "guhwaii_ukelele_voor.png")
    save(ukelele_hals(random.Random(30125)), "item", "guhwaii_ukelele_hals.png")
