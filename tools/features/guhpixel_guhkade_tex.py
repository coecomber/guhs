"""
The pictures of the guhpixel slice "guhkade" (drawn pixel by pixel, no dice: the same bytes every run):
  sprites()            the 64 x 64 sprite sheet of the two games (textures/guhkade/sprites.png); SPRITES is the same table as
                       Java's guhkade.spel.Sprite (the module's self-check compares them)
  kast_*(spel)         the cabinet's textures: side, front (coin door), marquee, control panel, the dark screen, the trim
  icoon(spel)          the item picture
"""
from PIL import Image

# name: (u, v, b, h): the same as Sprite.java
SPRITES = {
    "GUH_OP": (0, 0, 14, 12), "GUH_NEER": (14, 0, 14, 12), "GUH_AF": (28, 0, 14, 12),
    "BAL_0": (0, 12, 10, 10), "BAL_1": (10, 12, 10, 10), "BAL_2": (20, 12, 10, 10), "BAL_3": (30, 12, 10, 10),
    "MIKA": (42, 0, 12, 12), "MIKA_BOOS": (42, 12, 12, 12),
    "PILAAR": (0, 22, 22, 8), "KAP": (0, 30, 26, 6), "WOLK": (26, 24, 20, 9),
    "HARTJE": (46, 26, 7, 6), "HARTJE_LEEG": (53, 26, 7, 6),
    "BATJE": (56, 0, 4, 22), "BATJE_MIKA": (60, 0, 4, 22),
}
BLAD, WIT = 64, (63, 63)

ROZE, ROZE_D, ROZE_L = (255, 170, 205, 255), (200, 96, 146, 255), (255, 214, 230, 255)
WIT_K, ZWART, NEUS = (255, 255, 255, 255), (42, 20, 48, 255), (232, 84, 128, 255)
KAAS, KAAS_D, KAAS_L, KAAS_G = (246, 196, 74, 255), (206, 142, 36, 255), (255, 226, 132, 255), (232, 170, 52, 255)
MIKA, MIKA_D, MIKA_L = (176, 128, 156, 255), (92, 52, 84, 255), (214, 166, 190, 255)
PAL = {"p": ROZE, "d": ROZE_D, "o": ROZE_L, "w": WIT_K, "k": ZWART, "n": NEUS, "y": (255, 255, 255, 255), "g": (150, 130, 190, 255),
       "m": MIKA, "e": MIKA_D, "l": MIKA_L, "r": (255, 92, 110, 255), "c": KAAS, "C": KAAS_D, "L": KAAS_L, "G": KAAS_G,
       "x": (120, 92, 130, 255), "h": (255, 96, 140, 255), "H": (255, 190, 210, 255), "s": (90, 60, 100, 255)}

GUH = ["...dd....dd...",
       "..dood..dood..",
       "..dppddddppd..",
       ".dpppppppppd..",
       "dpppppppkppkd.",
       "dpppppppkppkd.",
       "dppppppppnnpd.",
       "dpppwwwwppppd.",
       ".dppwwwwwppd..",
       ".dpppwwwppd...",
       "..ddpppppdd...",
       "....ddddd....."]
# the tiny wing on its back (x, y, colour key), up and down
VLEUGEL_OP = [(0, 1, "g"), (1, 1, "g"), (0, 2, "y"), (1, 2, "y"), (2, 2, "g"), (1, 3, "y"), (2, 3, "y"), (3, 3, "g"), (2, 4, "y"), (3, 4, "y"), (3, 5, "g")]
VLEUGEL_NEER = [(3, 5, "g"), (2, 6, "y"), (3, 6, "y"), (1, 7, "y"), (2, 7, "y"), (3, 7, "g"), (0, 8, "g"), (1, 8, "y"), (2, 8, "g"), (0, 9, "g"), (1, 9, "g")]

BAL = ["..dddddd..",
       ".dppppppd.",
       "dppkppkppd",
       "dpppnnpppd",
       "dppppppppd",
       "dppwwwwppd",
       "dppwwwwppd",
       "dpppwwpppd",
       ".dppppppd.",
       "..dddddd.."]

MIKA_ROWS = [".e........e.",
             "eme......eme",
             "emmeeeeeemme",
             "emmmmmmmmmme",
             "emkkmmmmkkme",
             "emwkmmmmwkme",
             "emmmmllmmmme",
             "emmmlnnlmmme",
             "emmmmllmmmme",
             ".emmmmmmmme.",
             "..emmmmmme..",
             "...eeeeee..."]
MIKA_BOOS_ROWS = [".e........e.",
                  "eme......eme",
                  "emmeeeeeemme",
                  "emkmmmmmmkme",
                  "emmkkmmkkmme",
                  "emwkmmmmwkme",
                  "emmmmllmmmme",
                  "emmmlnnlmmme",
                  "emmmkkkkmmme",
                  ".emmmmmmmme.",
                  "..emmmmmme..",
                  "...eeeeee..."]

WOLK = ["......wwww..........",
        "....wwwwwwww........",
        "...wwwwwwwwww..www..",
        "..wwwwwwwwwwwwwwwww.",
        ".wwwwwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwwwwwww",
        ".wwwwwwwwwwwwwwwwww.",
        "...wwwwwwwwwwwwww..."]

HART = [".hh.hh.",
        "hHhhhhh",
        "hhhhhhh",
        ".hhhhh.",
        "..hhh..",
        "...h..."]
HART_LEEG = [".ss.ss.",
             "s..s..s",
             "s.....s",
             ".s...s.",
             "..s.s..",
             "...s..."]


def _plak(img, rows, x0, y0):
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x0 + x, y0 + y), PAL[c])


def _kaasvlak(img, x0, y0, b, h, rand=True):
    """A piece of kaasknabbel: cheese yellow, a light left edge, a dark right edge and a few holes."""
    for y in range(h):
        for x in range(b):
            c = KAAS
            if x <= 1:
                c = KAAS_L
            elif x >= b - 2:
                c = KAAS_D
            elif (x * 7 + y * 5) % 11 == 0:
                c = KAAS_G
            img.putpixel((x0 + x, y0 + y), c)
    if rand:
        for x in range(b):
            img.putpixel((x0 + x, y0), KAAS_D if x % 2 else KAAS_G)


def sprites():
    img = Image.new("RGBA", (BLAD, BLAD), (0, 0, 0, 0))
    # Flappy Guh: the guh, wings up / down / dizzy on the floor
    for naam, vleugel in (("GUH_OP", VLEUGEL_OP), ("GUH_NEER", VLEUGEL_NEER), ("GUH_AF", ())):
        u, v, _, _ = SPRITES[naam]
        _plak(img, GUH, u, v)
        for x, y, c in vleugel:
            img.putpixel((u + x, v + y), PAL[c])
    u, v, _, _ = SPRITES["GUH_AF"]                 # (dizzy: its eyes are little crosses and its tongue sticks out)
    for x, y in ((8, 4), (8, 5), (11, 4), (11, 5)):
        img.putpixel((u + x, v + y), ROZE)
    for x, y in ((7, 4), (9, 4), (8, 5), (7, 6), (9, 6), (10, 4), (12, 4), (11, 5), (10, 6)):
        img.putpixel((u + x, v + y), ZWART)
    img.putpixel((u + 9, v + 7), NEUS)
    img.putpixel((u + 9, v + 8), NEUS)
    # Mika-Pong: the rolling guh, a quarter turn further each time
    bal = Image.new("RGBA", (10, 10), (0, 0, 0, 0))
    _plak(bal, BAL, 0, 0)
    for i in range(4):
        u, v, _, _ = SPRITES[f"BAL_{i}"]
        img.paste(bal.rotate(-90 * i), (u, v))
    _plak(img, MIKA_ROWS, *SPRITES["MIKA"][:2])
    _plak(img, MIKA_BOOS_ROWS, *SPRITES["MIKA_BOOS"][:2])
    # the kaasknabbel pillar: a slice that stacks, and the wider cap
    u, v, b, h = SPRITES["PILAAR"]
    _kaasvlak(img, u, v, b, h, rand=False)
    for x in range(2, b - 2):                      # (the seam between two knabbels)
        img.putpixel((u + x, v + h - 1), KAAS_G)
    u, v, b, h = SPRITES["KAP"]
    _kaasvlak(img, u, v, b, h)
    for x in range(b):
        img.putpixel((u + x, v + h - 1), KAAS_D)
    _plak(img, WOLK, *SPRITES["WOLK"][:2])
    _plak(img, HART, *SPRITES["HARTJE"][:2])
    _plak(img, HART_LEEG, *SPRITES["HARTJE_LEEG"][:2])
    # the paddles: a kaasknabbel stick for you, a dark one for the Mika
    u, v, b, h = SPRITES["BATJE"]
    _kaasvlak(img, u, v, b, h, rand=False)
    for x in range(b):
        img.putpixel((u + x, v), KAAS_L)
        img.putpixel((u + x, v + h - 1), KAAS_D)
    u, v, b, h = SPRITES["BATJE_MIKA"]
    for y in range(h):
        for x in range(b):
            img.putpixel((u + x, v + y), MIKA_L if x == 0 else MIKA_D if x == b - 1 or y in (0, h - 1) else MIKA)
    img.putpixel(WIT, (255, 255, 255, 255))
    return img


# =====================================================================================================================
# the cabinets
# =====================================================================================================================
THEMA = {
    # body, body dark, body light, accent
    "flappy": ((92, 170, 236), (52, 112, 184), (150, 208, 250), (246, 196, 74)),
    "pong": ((122, 70, 150), (74, 36, 98), (170, 112, 196), (255, 154, 200)),
}
TRIM, TRIM_L = (40, 24, 48), (72, 48, 84)


def _vlak(kleur, donker, licht):
    img = Image.new("RGBA", (16, 16), kleur + (255,))
    for i in range(16):
        img.putpixel((0, i), licht + (255,))
        img.putpixel((i, 0), licht + (255,))
        img.putpixel((15, i), donker + (255,))
        img.putpixel((i, 15), donker + (255,))
    return img


def kast_zij(spel):
    """The side: the body colour with a slanted stripe and a little picture (a guh / a Mika face)."""
    kleur, donker, licht, accent = THEMA[spel]
    img = _vlak(kleur, donker, licht)
    for i in range(1, 15):
        for d in (0, 1):
            x = i + d - 4
            if 1 <= x <= 14:
                img.putpixel((x, 15 - i), accent + (255,))
    rows = GUH if spel == "flappy" else MIKA_ROWS
    klein = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    _plak(klein, rows, 0, 0)
    klein = klein.resize((7, 6), Image.NEAREST)
    img.alpha_composite(klein, (8, 2))
    return img


def kast_voor(spel):
    """The lower front: the coin door with two slots and a grille."""
    kleur, donker, licht, accent = THEMA[spel]
    img = _vlak(kleur, donker, licht)
    for y in range(4, 13):
        for x in range(4, 12):
            rand = x in (4, 11) or y in (4, 12)
            img.putpixel((x, y), (TRIM if rand else TRIM_L) + (255,))
    for x in (6, 9):
        img.putpixel((x, 6), accent + (255,))
        img.putpixel((x, 7), accent + (255,))
        img.putpixel((x, 8), (20, 10, 24, 255))
    for x in range(6, 10):
        img.putpixel((x, 10), (20, 10, 24, 255))
    for x in range(2, 14, 2):
        img.putpixel((x, 14), donker + (255,))
    return img


def kast_bord(spel):
    """The marquee: a lit strip with the game's little heroes."""
    kleur, donker, licht, accent = THEMA[spel]
    img = Image.new("RGBA", (16, 16), (255, 244, 214, 255))
    for i in range(16):
        img.putpixel((i, 0), accent + (255,))
        img.putpixel((i, 15), accent + (255,))
        img.putpixel((0, i), accent + (255,))
        img.putpixel((15, i), accent + (255,))
    if spel == "flappy":
        for x in (2, 12):                       # two cheese pillars and a guh between them
            for y in range(1, 15):
                if not 5 <= y <= 10:
                    img.putpixel((x, y), KAAS)
                    img.putpixel((x + 1, y), KAAS_D)
        for x, y in ((6, 6), (7, 6), (8, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (6, 8), (7, 8), (8, 8), (9, 8), (6, 9), (7, 9), (8, 9)):
            img.putpixel((x, y), ROZE)
        img.putpixel((9, 7), ZWART)
        img.putpixel((5, 6), (255, 255, 255, 255))
    else:
        for y in range(4, 12):                  # two paddles and the rolling guh
            img.putpixel((2, y), KAAS_D)
            img.putpixel((13, y), MIKA_D)
        for x, y in ((7, 6), (8, 6), (6, 7), (7, 7), (8, 7), (9, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9)):
            img.putpixel((x, y), ROZE)
        img.putpixel((7, 7), ZWART)
        img.putpixel((9, 7), ZWART)
    return img


def kast_paneel(spel):
    """The control panel seen from above: dark, with the marks of a stick and two buttons."""
    kleur, donker, licht, accent = THEMA[spel]
    img = _vlak(TRIM_L, TRIM, (96, 70, 108))
    for i in range(1, 15):
        img.putpixel((i, 1), accent + (255,))
    return img


def kast_scherm():
    """The glass of the screen when the renderer is not drawing on it (far away): dark with a faint glow."""
    img = Image.new("RGBA", (16, 16), (26, 12, 30, 255))
    for y in range(16):
        for x in range(16):
            if y % 2 == 0:
                img.putpixel((x, y), (36, 18, 44, 255))
    for x, y in ((3, 3), (4, 3), (3, 4)):
        img.putpixel((x, y), (86, 60, 100, 255))
    return img


def kast_rand():
    return _vlak(TRIM, (24, 12, 30), TRIM_L)


def knop(kleur):
    licht = tuple(min(255, c + 60) for c in kleur)
    donker = tuple(max(0, c - 70) for c in kleur)
    return _vlak(kleur, donker, licht)


def icoon(spel):
    """The item: a little cabinet seen from the front."""
    kleur, donker, licht, accent = THEMA[spel]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(0, 16):
        for x in range(3, 13):
            c = kleur
            if x == 3:
                c = licht
            elif x == 12:
                c = donker
            img.putpixel((x, y), c + (255,))
    for x in range(3, 13):
        img.putpixel((x, 0), TRIM + (255,))
        img.putpixel((x, 15), TRIM + (255,))
    for x in range(4, 12):                       # marquee
        img.putpixel((x, 1), accent + (255,))
        img.putpixel((x, 2), accent + (255,))
    for y in range(4, 9):                        # the screen
        for x in range(4, 12):
            img.putpixel((x, y), (30, 14, 36, 255) if spel == "pong" else (143, 208, 255, 255))
    if spel == "flappy":
        for y in (4, 5, 8):
            img.putpixel((9, y), KAAS)
            img.putpixel((10, y), KAAS_D)
        img.putpixel((6, 6), ROZE)
        img.putpixel((7, 6), ROZE)
        img.putpixel((6, 7), ROZE)
    else:
        for y in (5, 6):
            img.putpixel((4, y), KAAS)
            img.putpixel((11, y + 1), MIKA_L)
        img.putpixel((7, 6), ROZE)
        img.putpixel((8, 6), ROZE)
    for x in range(2, 14):                       # the control shelf
        img.putpixel((x, 10), TRIM_L + (255,))
        img.putpixel((x, 11), TRIM + (255,))
    img.putpixel((5, 9), (255, 92, 110, 255))
    img.putpixel((9, 9), accent + (255,))
    for x in (6, 9):                             # coin slots
        img.putpixel((x, 13), accent + (255,))
    return img
