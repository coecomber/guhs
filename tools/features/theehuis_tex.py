"""
Het Knabbelthee-huisje (2.8) - the textures of its blocks, items, particles and the gezellig effect, painted here (PIL).
"""
import math
import random

from PIL import Image

from features import knuffeldal_tex as kt
from features.creche_tex import grid

GOUD_D, GOUD = (184, 128, 40), (246, 200, 70)
ROZE, ROZE_D, ROZE_L = (246, 150, 196), (214, 108, 158), (255, 214, 232)
BLAUW, BLAUW_L = (140, 200, 240), (200, 230, 252)
PORS, PORS_D = (252, 250, 246), (206, 208, 218)

# the teas: (tea colour, a highlight)
THEE = {"knabbelthee": ((196, 132, 60), (236, 186, 96)), "kaasmelkthee": ((246, 222, 150), (255, 244, 200)),
        "theekruidthee": ((120, 178, 90), (170, 214, 130)), "guhbloementhee": ((238, 130, 176), (255, 190, 220))}


def kopje(soort):
    thee, licht = THEE[soort]
    img = grid([
        "................",
        ".....s...s......",
        "......s...s.....",
        ".....s...s......",
        "................",
        "..gggggggggg....",
        "..gttllttttg.hh.",
        "..gPPPPPPPPg..h.",
        "..gPPrPPPrPghhh.",
        "...gPPPPPPg.....",
        "...gPPPPPPg.....",
        "....gPPPPg......",
        ".bbbbbbbbbbbb...",
        "..BBBBBBBBBB....",
        "................",
        "................"], {"s": (230, 236, 246), "g": PORS_D, "t": thee, "l": licht, "P": PORS, "r": ROZE, "h": PORS_D,
                             "b": GOUD, "B": PORS_D})
    if soort == "knabbelthee":
        img.putpixel((7, 6), (250, 206, 110, 255))                 # a floating kaasknabbel crumb
    elif soort == "guhbloementhee":
        img.putpixel((6, 6), (255, 255, 255, 255))                 # a little flower on top
    elif soort == "theekruidthee":
        img.putpixel((8, 6), (90, 150, 60, 255))
    return img


def theeservies():
    return grid([
        "................",
        "......gYg.......",
        ".....gPPPg......",
        "..s.gPPrPPg.....",
        ".sPgPPPPPPPgh...",
        "..sgPrPPPrPg.h..",
        "...gPPPPPPPghh..",
        "....gPPPPPg.....",
        "...YYYYYYYYY....",
        "................",
        ".gggg....gggg...",
        ".gttgh...gttgh..",
        ".gPPg.h..gPPg.h.",
        "..gg.h....gg.h..",
        "YYYYYY..YYYYYY..",
        "................"], {"g": PORS_D, "Y": GOUD, "P": PORS, "r": ROZE, "h": PORS_D, "s": PORS_D, "t": (196, 132, 60)})


def tafelkleed(rng):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (255, 250, 252)
            if (x % 6 == 2 and y % 6 == 2) or (x % 6 == 5 and y % 6 == 5):
                c = ROZE if (x + y) % 4 else BLAUW
            if (x % 6 in (1, 3) and y % 6 == 2) or (y % 6 in (1, 3) and x % 6 == 2):
                c = ROZE_L
            if x in (0, 15) or y in (0, 15):
                c = ROZE_L if (x + y) % 2 else (255, 255, 255)
            img.putpixel((x, y), kt.clamp(ch + rng.randint(-3, 3) for ch in c) + (255,))
    return img


def tafelrand(rng):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            bottom = 13 + int(round(1.5 * math.cos(x * math.pi / 2)))
            if y <= bottom:
                c = (255, 250, 252) if y < bottom - 1 else ROZE
                if y == 2 or y == 3:
                    c = ROZE_L
                img.putpixel((x, y), kt.clamp(ch + rng.randint(-3, 3) for ch in c) + (255,))
    return img


def pot(rng):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = PORS
            if y in (6, 7, 8):
                c = ROZE if (x + (y == 7)) % 4 else (255, 255, 255)
            if y in (0, 15):
                c = GOUD
            if y in (3, 11) and x % 4 == 1:
                c = BLAUW
            img.putpixel((x, y), kt.clamp(ch + rng.randint(-3, 3) for ch in c) + (255,))
    return img


def deksel(rng, face):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), kt.clamp(ch + rng.randint(-4, 4) for ch in (250, 180, 210)) + (255,))
    if face:
        kt.paint_face(img, 0)
    return img


def stoom(i):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    pts = [[(3, 7), (3, 6), (4, 5), (4, 4), (3, 3), (3, 2), (4, 1)], [(4, 7), (4, 6), (3, 5), (3, 4), (4, 3), (4, 2), (3, 1), (3, 0)],
           [(3, 6), (4, 5), (5, 4), (4, 3), (3, 2), (2, 1)]][i]
    for (x, y) in pts:
        img.putpixel((x, y), (250, 250, 255, 220))
        if x + 1 < 8:
            img.putpixel((x + 1, y), (240, 244, 255, 120))
    return img


def hartje(i):
    col = [(246, 110, 170), (255, 160, 200)][i]
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for (x, y) in ((1, 1), (2, 1), (4, 1), (5, 1), (0, 2), (1, 2), (2, 2), (3, 2), (4, 2), (5, 2), (6, 2), (0, 3), (1, 3), (2, 3), (3, 3),
                   (4, 3), (5, 3), (6, 3), (1, 4), (2, 4), (3, 4), (4, 4), (5, 4), (2, 5), (3, 5), (4, 5), (3, 6)):
        img.putpixel((x, y), col + (255,))
    img.putpixel((1, 2), (255, 230, 240, 255))
    return img


def effect_icon():
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    heart = [".XX..XX.", "XXXXXXXX", "XXXXXXXX", "XXXXXXXX", ".XXXXXX.", "..XXXX..", "...XX..."]
    for dy, row in enumerate(heart):
        for dx, ch in enumerate(row):
            if ch == "X":
                for sy in (0, 1):
                    for sx in (0, 1):
                        img.putpixel((1 + dx * 2 + sx, 2 + dy * 2 + sy), (246, 110, 170, 255))
    for (x, y) in ((4, 4), (5, 4), (4, 5)):
        img.putpixel((x, y), (255, 220, 236, 255))
    cup = kopje("knabbelthee").resize((10, 10), Image.NEAREST)
    img.alpha_composite(cup, (8, 8))
    return img


def textures(h):
    rng = random.Random(28401)
    save = h.save
    for soort in THEE:
        save(kopje(soort), "item", f"{soort}.png")
    save(theeservies(), "item", "feest_theeservies.png")
    save(tafelkleed(rng), "block", "theetafel_kleed.png")
    save(tafelrand(rng), "block", "theetafel_rand.png")
    save(pot(rng), "block", "theepotje.png")
    save(deksel(rng, False), "block", "theepotje_deksel.png")
    save(deksel(rng, True), "block", "theepotje_gezicht.png")
    for i in range(3):
        save(stoom(i), "particle", f"theestoom_{i}.png")
    h.w(f"{h.A}/particles/theestoom.json", {"textures": [f"guhs:theestoom_{i}" for i in range(3)]})
    for i in range(2):
        save(hartje(i), "particle", f"gezellig_hartje_{i}.png")
    h.w(f"{h.A}/particles/gezellig_hartje.json", {"textures": [f"guhs:gezellig_hartje_{i}" for i in range(2)]})
    save(effect_icon(), "mob_effect", "gezellig.png")
