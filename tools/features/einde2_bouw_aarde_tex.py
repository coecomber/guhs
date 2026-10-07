"""
einde2, slice bouw_aarde: the textures of the six decorative blocks of Ba Sing Guh (called by einde2_bouw_aarde.build).

The city is built of aged cheese like the cliffs it floats over (klipkaas), so its own blocks stay in that family: the big
wall stone is a paler, drier klipkaas; the aardemunt is the green and gold coin of the earth kingdom of the guhs (round,
with a square bite out of it); the kool is the koolman's whole trade; the monorail beam is wall stone with a worn groove;
the catacomb crystal glows green; the tea crate smells of jasmine. Vanilla textures painted from one colour to another
(h.ramp) with our own lines drawn on top; every painter is seeded from a NAME (kit.seed).

    textures(h) -> writes them all; BLOCKS lists what it wrote.
"""
import math
import random

from PIL import Image

import features.einde2_kit as kit

STEEN_DARK, STEEN_LIGHT = (142, 108, 62), (232, 206, 150)
GROEN_DARK, GROEN, GROEN_LIGHT = (28, 84, 52), (58, 132, 78), (120, 190, 120)
GOUD_DARK, GOUD, GOUD_LIGHT = (176, 112, 20), (240, 190, 70), (255, 236, 150)
KOOL_DARK, KOOL, KOOL_LIGHT, KOOL_HART = (62, 118, 44), (118, 176, 70), (176, 220, 118), (226, 244, 176)

BLOCKS = []


def _rng(name):
    return random.Random(kit.seed("tex/bouw_aarde/" + name))


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def muursteen(h):
    """Two courses of big stones: the stone-brick lines of vanilla, in dry klipkaas, with a little moss in the joints."""
    img = h.ramp(h.vanilla("block/stone_bricks"), STEEN_DARK, STEEN_LIGHT)
    r = _rng("muursteen")
    for _ in range(9):
        x, y = r.randrange(16), r.choice((7, 8, 15, 0))
        img.putpixel((x, y), _mix(STEEN_DARK, GROEN, 0.55) + (255,))
    return img


def aardemunt(h):
    """The coin: a golden ring with a square hole on a green field, in a frame of wall stone."""
    r = _rng("aardemunt")
    img = h.ramp(h.vanilla("block/stone_bricks"), STEEN_DARK, STEEN_LIGHT)
    for y in range(16):
        for x in range(16):
            if 1 <= x <= 14 and 1 <= y <= 14:
                d = math.hypot(x - 7.5, y - 7.5)
                veld = _mix(GROEN_DARK, GROEN, 0.35 + 0.5 * r.random())
                if max(abs(x - 7.5), abs(y - 7.5)) <= 1.6:
                    c = GROEN_DARK
                elif d <= 6.3:
                    t = (x + y) / 30.0
                    c = _mix(GOUD_LIGHT, GOUD_DARK, t) if d > 5.3 or max(abs(x - 7.5), abs(y - 7.5)) <= 2.6 else _mix(GOUD_LIGHT, GOUD, 0.3 + t * 0.9)
                else:
                    c = veld
                img.putpixel((x, y), tuple(c) + (255,))
    return img


def kool(h, top):
    """The side of the head (leaves that wrap around it, a pale rib in each) or its top (a rosette around the heart).
    The model only uses the middle 12 x 11 of the side and the middle 12 x 12 of the top; the rest stays clear."""
    r = _rng("kool_top" if top else "kool")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if top:
                if not (2 <= x <= 13 and 2 <= y <= 13):
                    continue
                d = math.hypot(x - 7.5, y - 7.5)
                a = math.atan2(y - 7.5, x - 7.5)
                blad = 0.5 + 0.5 * math.sin(a * 5 + d * 1.3)
                c = _mix(KOOL_HART, KOOL_LIGHT, d / 2.5) if d < 2.5 else _mix(KOOL_LIGHT, KOOL_DARK, min(1.0, (d - 2.5) / 5.0) * 0.6 + blad * 0.4)
            else:
                if not (2 <= x <= 13 and y >= 5):
                    continue
                boog = math.sin((x - 2) / 11.0 * math.pi)
                rib = abs(((x + (y // 3)) % 4) - 1.5) < 0.6
                c = _mix(KOOL_DARK, KOOL, 0.25 + 0.55 * boog + 0.15 * r.random())
                if rib:
                    c = _mix(c, KOOL_LIGHT, 0.6)
                if y == 5:
                    c = _mix(c, KOOL_HART, 0.5)
            img.putpixel((x, y), tuple(c) + (255,))
    return img


def spoor(h, top):
    """The beam: wall stone, and on top two worn grooves along it with an iron strip between them (they run east-west,
    the way the monorail of Ba Sing Guh runs)."""
    img = h.ramp(h.vanilla("block/smooth_stone" if top else "block/smooth_stone_slab_side"), STEEN_DARK, STEEN_LIGHT)
    r = _rng("spoor_top" if top else "spoor")
    if top:
        for x in range(16):
            for y, c in ((5, (96, 70, 40)), (10, (96, 70, 40)), (7, (126, 130, 134)), (8, (88, 92, 98))):
                k = r.randint(-8, 8)
                img.putpixel((x, y), tuple(max(0, min(255, v + k)) for v in c) + (255,))
    else:
        for x in range(16):
            img.putpixel((x, 0), (118, 122, 128, 255))
            img.putpixel((x, 1), (96, 70, 40, 255))
    return img


def groen_kristal(h):
    return h.ramp(h.vanilla("block/amethyst_block"), (18, 108, 70), (196, 255, 214))


def theekist(h, top):
    """A crate of pale planks in a dark frame; on its side a jasmine flower, on its lid a tea leaf."""
    img = h.ramp(h.vanilla("block/birch_planks"), (150, 118, 70), (228, 204, 150))
    rand = (92, 62, 30)
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), rand + (255,))
    if top:
        for x, y in ((7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (5, 6), (6, 6), (9, 6), (10, 6), (5, 7), (10, 7), (6, 8), (9, 8), (6, 9), (7, 9), (8, 9), (9, 9),
                     (7, 10), (8, 10), (8, 11)):
            img.putpixel((x, y), GROEN + (255,))
        for x, y in ((7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8)):
            img.putpixel((x, y), GROEN_LIGHT + (255,))
    else:
        wit, hart = (250, 248, 232), GOUD
        for x, y in ((7, 4), (8, 4), (5, 6), (10, 6), (5, 9), (10, 9), (7, 11), (8, 11), (6, 5), (9, 5), (6, 10), (9, 10), (7, 5), (8, 5), (6, 6), (9, 6),
                     (6, 7), (9, 7), (6, 8), (9, 8), (6, 9), (9, 9), (7, 10), (8, 10), (5, 7), (5, 8), (10, 7), (10, 8)):
            img.putpixel((x, y), wit + (255,))
        for x, y in ((7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9)):
            img.putpixel((x, y), hart + (255,))
        for x in range(4, 12):
            img.putpixel((x, 13), rand + (255,))
    return img


def textures(h):
    BLOCKS.clear()

    def blok(name, img):
        h.save(img, "block", f"einde2_bouw_aarde_{name}.png")
        BLOCKS.append(f"einde2_bouw_aarde_{name}")

    blok("stadssteen", muursteen(h))
    blok("aardemunt", aardemunt(h))
    blok("kool", kool(h, False))
    blok("kool_top", kool(h, True))
    blok("spoor", spoor(h, False))
    blok("spoor_top", spoor(h, True))
    blok("groen_kristal", groen_kristal(h))
    blok("theekist", theekist(h, False))
    blok("theekist_top", theekist(h, True))


def preview(h, path):
    """A contact sheet of everything above (for looking at, not shipped)."""
    import os
    sheet = Image.new("RGBA", (len(BLOCKS) * 72, 72), (58, 30, 52, 255))
    for i, name in enumerate(BLOCKS):
        im = Image.open(os.path.join(h.TEX, "block", f"{name}.png")).convert("RGBA").resize((64, 64), Image.NEAREST)
        sheet.paste(im, (i * 72 + 4, 4), im)
    sheet.save(path)
