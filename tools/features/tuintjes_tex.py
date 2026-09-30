"""
De guhtuintjes (2.8) - textures: the pot and the raised bed (each with a guh face on the front), dry and watered soil,
the three guh plants in four growth steps (knabbelplantje, theekruid, guhbloem), the items and the particles.
"""
import math
import random

from PIL import Image

from features.boerderij_tex import art, clamp, guh_gezicht, guh_oogjes, noisy

GROEN = (110, 190, 90)
GROEN_D = (70, 140, 60)
GROEN_L = (160, 220, 120)
MINT = (140, 214, 170)
MINT_D = (90, 170, 130)
GRAAN = (246, 206, 90)
GRAAN_D = (214, 160, 50)
ROZE = (246, 150, 196)
ROZE_D = (214, 96, 150)
WIT = (255, 250, 250)


# =====================================================================================================================
# the pot, the bed, the soil
# =====================================================================================================================
def pot_zijkant(rng):
    img = noisy((240, 150, 180), 6, rng)
    px = img.load()
    for x in range(16):
        px[x, 0] = px[x, 1] = (250, 190, 212, 255)             # the rim
        px[x, 15] = (206, 116, 150, 255)
    for x in range(0, 16, 4):                                   # little dots
        px[x + 1, 8] = (255, 214, 232, 255)
    return img


def pot_voorkant(rng):
    img = pot_zijkant(rng)
    return guh_gezicht(img, 8, 8.5, 4.6)


def aarde(rng, nat):
    base = (96, 64, 50) if nat else (140, 100, 72)
    img = noisy(base, 10, rng)
    px = img.load()
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = clamp(c + (30 if not nat else 18) for c in base) + (255,)
    if nat:
        for _ in range(6):
            x, y = rng.randrange(15), rng.randrange(16)
            px[x, y] = (150, 170, 200, 255)                     # glistening water
            px[x + 1, y] = (120, 140, 170, 255)
    return img


def bak_zijkant(rng):
    img = noisy((214, 150, 120), 7, rng)
    px = img.load()
    for y in (0, 5, 10, 15):
        for x in range(16):
            px[x, y] = (170, 110, 90, 255)
    for (x, y) in ((1, 2), (14, 2), (1, 12), (14, 12)):
        px[x, y] = (120, 90, 80, 255)                           # nails
    return img


def bak_voorkant(rng):
    img = bak_zijkant(rng)
    px = img.load()
    for y in range(3, 14):                                      # a pink painted board with a guh face
        for x in range(2, 14):
            px[x, y] = clamp(c + rng.randint(-5, 5) for c in (240, 170, 196)) + (255,)
    return guh_gezicht(img, 8, 8.5, 4.2)


# =====================================================================================================================
# the plants (cross textures, 16 x 16; growth 0..3)
# =====================================================================================================================
def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def stengel(img, x, y0, y1, c=GROEN_D):
    px = img.load()
    for y in range(y0, y1 + 1):
        px[x, y] = c + (255,)


def blaadje(img, x, y, side, c=GROEN, size=2):
    px = img.load()
    for k in range(size):
        for j in range(k + 1):
            xx, yy = x + side * (k + 1), y - j
            if 0 <= xx < 16 and 0 <= yy < 16:
                px[xx, yy] = c + (255,)


def spruitje():
    img = blank()
    stengel(img, 8, 12, 15)
    blaadje(img, 8, 12, -1, GROEN_L, 2)
    blaadje(img, 8, 12, 1, GROEN, 2)
    return img


def knabbelplantje(stap):
    if stap == 0:
        return spruitje()
    img = blank()
    px = img.load()
    hoog = {1: 8, 2: 4, 3: 2}[stap]
    for x in (5, 8, 11):
        stengel(img, x, hoog + 2, 15)
        blaadje(img, x, 12, -1 if x < 8 else 1, GROEN, 2)
        if stap >= 2:
            blaadje(img, x, 9, 1 if x < 8 else -1, GROEN_L, 2)
        if stap == 3:                                           # golden ears full of little kaasknabbel puffs
            for y in range(hoog, hoog + 6):
                px[x, y] = GRAAN + (255,)
                if y % 2 == 0:
                    px[x - 1, y] = GRAAN_D + (255,)
                    px[x + 1, y] = GRAAN_D + (255,)
            px[x, hoog - 1] = (255, 236, 150, 255)
        elif stap == 2:
            for y in range(hoog, hoog + 3):
                px[x, y] = (200, 214, 110, 255)
    return img


def theekruid(stap):
    if stap == 0:
        return spruitje()
    img = blank()
    px = img.load()
    rng = random.Random(stap * 11)
    n = {1: 10, 2: 26, 3: 40}[stap]
    top = {1: 10, 2: 6, 3: 3}[stap]
    for x in (6, 9):
        stengel(img, x, top + 1, 15, MINT_D)
    for _ in range(n):                                          # a bushy tuft of round mint-green leaves
        x = rng.randint(3, 12)
        y = rng.randint(top, 14)
        if abs(x - 7.5) <= (15 - y) * 0.6 + 1.5:
            px[x, y] = (MINT if rng.random() < 0.6 else MINT_D) + (255,)
    if stap == 3:
        for (x, y) in ((5, 4), (10, 5), (7, 3), (12, 8), (3, 8)):
            px[x, y] = WIT + (255,)
            if 0 <= x + 1 < 16:
                px[x + 1, y] = (255, 230, 240, 255)
    return img


def guhbloem(stap):
    if stap == 0:
        return spruitje()
    img = blank()
    px = img.load()
    stengel(img, 8, {1: 9, 2: 6, 3: 7}[stap], 15)
    blaadje(img, 8, 13, -1, GROEN, 3)
    blaadje(img, 8, 11, 1, GROEN_L, 3)
    if stap == 2:                                               # a pink bud
        for (x, y) in ((7, 4), (8, 4), (7, 5), (8, 5), (8, 3)):
            px[x, y] = ROZE + (255,)
    if stap == 3:                                               # the guhbloem: a round pink flower with a guh face and ear-petals
        for y in range(1, 9):
            for x in range(3, 13):
                d = math.hypot(x - 7.5, (y - 4.5) * 1.1)
                if d <= 3.9:
                    px[x, y] = (ROZE if d > 2.8 else (255, 214, 232)) + (255,)
        for (cx, cy) in ((4, 1), (11, 1)):                      # round petal ears
            for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
                px[cx + dx, cy + dy] = ROZE_D + (255,)
        guh_oogjes(img, xs=(5, 9), y=3, blush=False)
        px[7, 6] = ROZE_D + (255,)
        px[8, 6] = ROZE_D + (255,)
    return img


PLANTS = {"knabbelplantje": knabbelplantje, "theekruid": theekruid, "guhbloem": guhbloem}


# =====================================================================================================================
# items
# =====================================================================================================================
def icon_gieter():
    img = art([
        "................",
        "......kkk.......",
        ".....k...k......",
        "....pppppppp..s.",
        "...pppppppppps..",
        "..pppppppppppsd.",
        "..ppppppppppp...",
        "..ppwpppwpppp...",
        "..ppppPPpppp....",
        "..prppppppr.....",
        "...pppppppp.....",
        "....pppppp......",
    ], {"k": (150, 90, 110), "p": ROZE, "P": ROZE_D, "s": (214, 120, 160), "d": (120, 190, 240), "w": (24, 20, 40), "r": (255, 120, 170)})
    return img


def icon_zaadjes(c1, c2):
    return art([
        "......aaaa......",
        ".....abbbba.....",
        "....abbbbbba....",
        "....abcbbcba....",
        "....abbccbba....",
        "....abcbbcba....",
        "....abbbbbba....",
        "....abbbbbba....",
        ".....aaaaaa.....",
    ], {"a": (170, 120, 80), "b": (236, 214, 170), "c": c1}) if c2 is None else art([
        "......aaaa......",
        ".....abbbba.....",
        "....abbbbbba....",
        "....abcbbdba....",
        "....abbdcbba....",
        "....abcbbdba....",
        "....abbbbbba....",
        "....abbbbbba....",
        ".....aaaaaa.....",
    ], {"a": (170, 120, 80), "b": (236, 214, 170), "c": c1, "d": c2})


def icon_knabbelgraan():
    return art([
        "..........y.....",
        ".........yYy....",
        "........yYyY....",
        ".......yYyY.....",
        "......yYyY......",
        ".....gyYy.......",
        "....g.gy........",
        "...g............",
        "..g.............",
        ".g..............",
    ], {"y": GRAAN, "Y": GRAAN_D, "g": GROEN_D})


def icon_theekruid():
    return art([
        ".........mm.....",
        "........mMmm....",
        ".....mm.mMm.....",
        "....mMmmgm......",
        ".....mmgmm..mm..",
        "......g.mMmmMm..",
        ".....g...mmmm...",
        "....g...........",
        "...g............",
    ], {"m": MINT, "M": MINT_D, "g": GROEN_D})


def icon_guhbloemetje():
    img = art([
        "....RR....RR....",
        "....RppppppR....",
        "...pplllllpp....",
        "...plllllllp....",
        "...plllllllp....",
        "...pplllllpp....",
        "....pppppp......",
        ".......g........",
        ".....ggg........",
        ".......g..gg....",
        ".......ggg......",
        ".......g........",
    ], {"R": ROZE_D, "p": ROZE, "l": (255, 214, 232), "g": GROEN_D})
    return guh_oogjes(img, xs=(5, 8), y=4, blush=False)


def icon_feestboeket():
    img = art([
        "...pp..RR..pp...",
        "..plp.RlR.plp...",
        "...pp..RR..pp...",
        ".RR..pp..RR.....",
        "RlR.plp.RlR.....",
        ".RR..pp..RR.....",
        "...gg.gg.gg.....",
        "....gggggg......",
        ".....yyyy.......",
        "....yYyyYy......",
        ".....gggg.......",
        ".....g..g.......",
    ], {"p": ROZE, "R": (240, 110, 150), "l": (255, 230, 240), "g": GROEN_D, "y": (255, 214, 90), "Y": (240, 150, 60)})
    return img


def particle_druppel(i):
    img = Image.new("RGBA", (4, 4), (0, 0, 0, 0))
    col = [(130, 200, 245), (190, 230, 255)][i]
    for (x, y) in ((1, 0), (1, 1), (2, 1), (0, 2), (1, 2), (2, 2), (1, 3), (2, 3)):
        img.putpixel((x, y), col + (255,))
    return img


def particle_sprankel(i):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    r = [3, 2, 3, 1][i]
    col = [(200, 255, 140), (255, 240, 130), (160, 240, 120), (255, 255, 220)][i]
    for k in range(-r, r + 1):
        img.putpixel((4 + k, 4), col + (255,))
        img.putpixel((4, 4 + k), col + (255,))
    img.putpixel((4, 4), (255, 255, 255, 255))
    return img


def textures(h):
    rng = random.Random(28601)
    save = h.save
    save(pot_zijkant(rng), "block", "guh_bloempot_zijkant.png")
    save(pot_voorkant(rng), "block", "guh_bloempot_voorkant.png")
    save(aarde(rng, False), "block", "tuin_aarde.png")
    save(aarde(rng, True), "block", "tuin_aarde_nat.png")
    save(bak_zijkant(rng), "block", "guh_moestuinbak_zijkant.png")
    save(bak_voorkant(rng), "block", "guh_moestuinbak_voorkant.png")
    for plant, painter in PLANTS.items():
        for stap in range(4):
            save(painter(stap), "block", f"{plant}_{stap}.png")
    save(icon_gieter(), "item", "guh_gieter.png")
    save(icon_zaadjes((200, 150, 60), None), "item", "knabbelzaadjes.png")
    save(icon_zaadjes(MINT_D, None), "item", "theekruidzaadjes.png")
    save(icon_zaadjes(ROZE_D, (120, 80, 60)), "item", "guhbloemzaadjes.png")
    save(icon_knabbelgraan(), "item", "knabbelgraan.png")
    save(icon_theekruid(), "item", "theekruid.png")
    save(icon_guhbloemetje(), "item", "guhbloemetje.png")
    save(icon_feestboeket(), "item", "feestboeket.png")
    for name, n, painter in (("gieterdruppel", 2, particle_druppel), ("groeisprankel", 4, particle_sprankel)):
        for i in range(n):
            save(painter(i), "particle", f"{name}_{i}.png")
        h.w(f"{h.A}/particles/{name}.json", {"textures": [f"guhs:{name}_{i}" for i in range(n)]})
