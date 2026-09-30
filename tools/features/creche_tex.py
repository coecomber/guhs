"""
De Knuffelcreche (2.8) - the textures of its blocks, items and particles, all painted here (PIL). The guh faces come
from the Knuffeldal's own face painter (knuffeldal_tex.paint_face), so every guh face in the town looks the same.
"""
import math
import random

from PIL import Image

from features import knuffeldal_tex as kt

GOLD_D, GOLD = (184, 128, 40), (246, 200, 70)
BLAUW, BLAUW_D, BLAUW_L = (140, 200, 240), (90, 150, 210), (200, 230, 252)
ROZE, ROZE_D, ROZE_L = (246, 150, 196), (214, 108, 158), (255, 214, 232)
WIT, GRIJS = (255, 255, 255), (200, 210, 225)
MELK = (255, 250, 236)


def grid(rows, pal):
    img = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                c = pal[ch]
                img.putpixel((x, y), tuple(c[:3]) + ((c[3],) if len(c) > 3 else (255,)))
    return img


# =====================================================================================================================
# items
# =====================================================================================================================
def speenmunt():
    return grid([
        "................",
        ".....yyyyyy.....",
        "...yyYYYYYYyy...",
        "..yYYYYYYYYYYy..",
        "..yYYbbbbbbYYy..",
        ".yYYbBBBBBBbYYy.",
        ".yYYbBBppBBbYYy.",
        ".yYYbBBppBBbYYy.",
        ".yYYYbBBBBbYYYy.",
        ".yYYYYYwwYYYYYy.",
        "..yYYYwYYwYYYy..",
        "..yYYYwYYwYYYy..",
        "...yyYYwwYYyy...",
        ".....yyyyyy.....",
        "................",
        "................"], {"y": GOLD_D, "Y": GOLD, "b": BLAUW_D, "B": BLAUW, "p": ROZE_D, "w": WIT})


def babyflesje():
    return grid([
        ".......pp.......",
        "......pPPp......",
        "......pPPp......",
        ".....bbbbbb.....",
        ".....bBBBBb.....",
        ".....gGGGGg.....",
        "....gGmmmmGg....",
        "....gmmmmmmg....",
        "....gmm--mmg....",
        "....gmmmmmmg....",
        "....gmmmmmmg....",
        "....gmm--mmg....",
        "....gmmmmmmg....",
        "....gmmmmmmg....",
        "....gggggggg....",
        "................"], {"p": ROZE_D, "P": ROZE, "b": BLAUW_D, "B": BLAUW, "g": GRIJS, "G": (236, 244, 250), "m": MELK,
                             "-": (170, 190, 210)})


def schone_luier():
    return grid([
        "................",
        "................",
        "................",
        "..gggggggggggg..",
        ".gWWWWWWWWWWWWg.",
        ".gWWbWWWWWWbWWg.",
        ".gWWWWWWWWWWWWg.",
        ".gYWWWWbWWWWWYg.",
        "..gWWWWWWWWWWg..",
        "...gWWWbWWWWg...",
        "....gWWWWWWg....",
        ".....gWWWWg.....",
        "......gggg......",
        "................",
        "................",
        "................"], {"g": GRIJS, "W": WIT, "b": BLAUW, "Y": GOLD})


def dekentje_patroon(size=16, rng=None):
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            c = ROZE_L if ((x // 4) + (y // 4)) % 2 == 0 else BLAUW_L
            if x % 4 == 0 or y % 4 == 0:
                c = kt.mix(c, WIT, 0.4)
            v = rng.randint(-4, 4) if rng else 0
            img.putpixel((x, y), kt.clamp(ch + v for ch in c) + (255,))
    return img


def knuffeldekentje():
    return grid([
        "................",
        "................",
        "..PPPBBBPPPBBB..",
        "..PPPBBBPPPBBB..",
        "..BBBPPPBBBPPP..",
        "..BBBPPPBBBPPP..",
        "..PPPBBBPPPBBB..",
        "..ddddddddddddd.",
        "..PPPBBBPPPBBBd.",
        "..BBBPPPBBBPPPd.",
        "..BBBPPPBBBPPP..",
        "..w.w.w.w.w.w...",
        "................",
        "................",
        "................",
        "................"], {"P": ROZE_L, "B": BLAUW_L, "d": (200, 170, 200), "w": WIT})


def feestslingers():
    """The garland: a string along the top, paper flags in pink, blue and yellow, each with two little eyes."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for x in range(16):
        y = 1 + int(round(1.2 * math.sin(math.pi * x / 15)))
        px[x, y] = (150, 110, 90, 255)
    cols = [ROZE, BLAUW, (250, 220, 90)]
    for i, x0 in enumerate((0, 5, 10)):
        c = cols[i]
        for dy in range(9):
            w = max(0, 5 - (dy * 5) // 9)
            for dx in range(w):
                x = x0 + (5 - w) // 2 + dx
                y = 3 + dy
                if 0 <= x < 16:
                    px[x, y] = kt.clamp(ch - (12 if dx == 0 else 0) for ch in c) + (255,)
        px[x0 + 1, 5] = px[x0 + 3, 5] = (40, 30, 50, 255)
        px[x0 + 2, 7] = (238, 90, 140, 255)
    return img


# =====================================================================================================================
# the crib
# =====================================================================================================================
def wieg_hout(rng):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (255, 240, 246) if x % 4 else ROZE_L
            if y in (0, 15):
                c = ROZE
            v = rng.randint(-4, 4)
            img.putpixel((x, y), kt.clamp(ch + v for ch in c) + (255,))
    return img


def wieg_laken(rng):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = WIT
            if (x + 2 * y) % 7 == 0 and y % 3 == 1:
                c = BLAUW_L
            v = rng.randint(-3, 3)
            img.putpixel((x, y), kt.clamp(ch + v for ch in c) + (255,))
    return img


def wieg_hoofdbord(rng):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), kt.clamp(ch + rng.randint(-4, 4) for ch in (250, 196, 220)) + (255,))
    kt.paint_face(img, 1)
    return img


def baby_gezicht(rng, slaapt):
    """A baby guh's face (16x16): soft pink, awake or asleep, with a light blue pacifier on its mouth."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), kt.clamp(ch + rng.randint(-3, 3) for ch in (255, 196, 222)) + (255,))
    kt.paint_face(img, 1 if slaapt else 0)
    px = img.load()
    for x in range(5, 11):
        for y in (12, 13):
            px[x, y] = BLAUW + (255,)
    for x in (7, 8):
        px[x, 14] = GOLD + (255,)
    px[7, 13] = px[8, 13] = ROZE_D + (255,)
    return img


def wens_bubbel(icoon):
    """A thought bubble (white, round) with a little picture of what the baby wants."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, (y - 7) * 1.1)
            if d <= 7.2:
                px[x, y] = ((180, 190, 210) if d > 6.3 else (255, 255, 255)) + (255,)
    for (x, y) in ((3, 14), (4, 14), (2, 15)):
        px[x, y] = (255, 255, 255, 255)
    small = {
        "honger": ["..pp..", ".bbbb.", ".gmmg.", ".gmmg.", ".gmmg.", ".gggg."],
        "luier": ["......", "gwwwwg", "gwbwwg", ".gwwg.", "..gg..", "......"],
        "slaap": ["..yy..", ".yy...", "yy....", "yy....", ".yy...", "..yyy."],
        "liedje": ["...vv.", "...v.v", "...v..", ".vvv..", "vvvv..", ".vv..."],
        "knutselen": ["s.....", "sss...", "prbyp.", ".pbyr.", "..pr..", "......"],
    }[icoon]
    pal = {"p": ROZE_D, "b": BLAUW, "g": GRIJS, "m": MELK, "w": (245, 245, 250), "y": GOLD, "v": (150, 90, 200), "s": (150, 110, 90),
           "r": ROZE}
    for dy, row in enumerate(small):
        for dx, ch in enumerate(row):
            if ch in pal:
                for sy in (0, 1):
                    for sx in (0, 1):
                        x, y = 2 + dx * 2 + sx, 1 + dy * 2 + sy
                        if 0 <= x < 16 and 0 <= y < 16:
                            px[x, y] = tuple(pal[ch]) + (255,)
    return img


def speelkleed(rng):
    """A soft play mat: pastel squares, a pink border, and a big smiling guh face in the middle."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (220, 240, 255) if ((x // 4) + (y // 4)) % 2 else (255, 232, 242)
            if x in (0, 15) or y in (0, 15):
                c = ROZE
            img.putpixel((x, y), kt.clamp(ch + rng.randint(-3, 3) for ch in c) + (255,))
    face = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    kt.paint_face(face, 0)
    for y in range(16):
        for x in range(16):
            p = face.getpixel((x, y))
            if p[3] > 0:
                img.putpixel((x, y), p)
    return img


def sterretje(i):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    col = [(255, 240, 150), (255, 250, 220), (200, 220, 255)][i]
    for (x, y) in ((3, 0), (3, 1), (3, 2), (0, 3), (1, 3), (2, 3), (3, 3), (4, 3), (5, 3), (6, 3), (3, 4), (3, 5), (3, 6), (2, 2), (4, 2),
                   (2, 4), (4, 4)):
        img.putpixel((x, y), col + (255,))
    img.putpixel((3, 3), (255, 255, 255, 255))
    return img


WENSEN = ["knutselen", "honger", "luier", "slaap", "liedje"]


def textures(h):
    rng = random.Random(28301)
    save = h.save
    save(speenmunt(), "item", "speenmunt.png")
    save(babyflesje(), "item", "babyflesje.png")
    save(schone_luier(), "item", "schone_luier.png")
    save(knuffeldekentje(), "item", "knuffeldekentje.png")
    save(feestslingers(), "block", "feestslingers.png")
    save(wieg_hout(rng), "block", "guh_wiegje_hout.png")
    save(wieg_laken(rng), "block", "guh_wiegje_laken.png")
    save(wieg_hoofdbord(rng), "block", "guh_wiegje_hoofdbord.png")
    save(baby_gezicht(rng, False), "block", "guh_wiegje_baby.png")
    save(baby_gezicht(rng, True), "block", "guh_wiegje_baby_slaapt.png")
    save(dekentje_patroon(16, rng), "block", "guh_wiegje_dekentje.png")
    for w in WENSEN:
        save(wens_bubbel(w), "block", f"guh_wiegje_wens_{w}.png")
    save(speelkleed(rng), "block", "speelkleed.png")
    for i in range(3):
        save(sterretje(i), "particle", f"slaapsterretje_{i}.png")
    h.w(f"{h.A}/particles/slaapsterretje.json", {"textures": [f"guhs:slaapsterretje_{i}" for i in range(3)]})
