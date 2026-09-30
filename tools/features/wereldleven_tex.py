"""
Het guhleven (2.8, wereldleven) - the textures: the 21 guh plushies, the guh-xylofoon, the grijpmachine (cabinet, glass,
sign, claw), the items (kaasijsjes, fluitje, marshmallow, liedjesboekje, grijpmachine icon), the particles and the
swatches of IJscoguh Tingeling's ice-cream bike. All painted here (PIL); vanilla textures only as a base to recolour.
"""
import colorsys
import math
import random

from PIL import Image

from features import knuffeldal_tex as kt

WHITE = (255, 255, 255)
INK = (58, 28, 48)
PINK = (246, 150, 196)
PINK_LIGHT = (255, 214, 232)
PINK_DARK = (208, 96, 150)


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def mix(a, b, t):
    return clamp(a[i] * (1 - t) + b[i] * t for i in range(3))


def light(c, d):
    return clamp(ch + d for ch in c[:3])


# =====================================================================================================================
# the plushies: one 32 x 32 texture each (regions in UV units of 16, 2 px per unit; see wereldleven_modellen.knuffel)
#   fur [0,0,8,8] · face [8,0,16,8] · ear inside [0,8,4,12] · belly [4,8,8,12] · extra A [8,8,12,12] · extra B [12,8,16,12]
#   paws / snoet [0,12,4,16] · seam [4,12,8,16]
# =====================================================================================================================
VARIANT_FUR = {
    "normal": (195, 160, 205), "mint": (170, 226, 200), "choco": (150, 104, 82), "snow": (240, 238, 244),
    "brontosaurus": (232, 222, 214), "golden": (242, 200, 96), "rainbow": (255, 190, 200), "starry": (58, 40, 104),
    "ghost": (232, 238, 255), "teckel": (195, 160, 205), "brococolief": (195, 160, 205), "ender": (46, 32, 60),
    "koning": (104, 58, 150), "wolk": (224, 238, 255), "zeemeerguh": (236, 170, 214), "mager": (150, 140, 146),
    "vahoege_ender": (236, 180, 70), "kaasmoerasguh": (168, 196, 96), "asguh": (118, 110, 116), "pluisguh": (250, 188, 216),
    "glitter": (255, 214, 120),
    "pinguh": (54, 54, 70),   # (2.9: the Pinguh, black-ish with a white belly and an orange snoet)
}
# the extra parts' colours (A, B)
VARIANT_EXTRA = {
    "brontosaurus": ((214, 190, 130), (236, 214, 214)), "ender": ((88, 38, 128), (22, 16, 30)), "vahoege_ender": ((255, 150, 210), (150, 40, 110)),
    "koning": ((245, 196, 60), (250, 248, 244)), "wolk": ((250, 252, 255), (190, 222, 255)), "zeemeerguh": ((80, 200, 190), (60, 160, 200)),
    "pluisguh": ((255, 246, 250), (255, 214, 232)), "glitter": ((255, 240, 150), (255, 150, 210)), "brococolief": ((255, 64, 190), (255, 140, 214)),
    "golden": ((255, 236, 150), (220, 160, 50)), "teckel": ((180, 140, 190), (160, 120, 170)), "kaasmoerasguh": ((236, 216, 80), (120, 150, 60)),
    "asguh": ((255, 150, 60), (80, 74, 78)),
    "pinguh": ((250, 250, 252), (255, 164, 60)),
}


def fleece(img, x0, y0, w, h, base, rng, var=10, spots=None):
    px = img.load()
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            c = base
            for colour, chance in spots or []:
                if rng.random() < chance:
                    c = colour
            v = rng.randint(-var, var)
            px[x, y] = clamp(ch + v for ch in c[:3]) + (255,)


def knuffel(vid, rng):
    fur = VARIANT_FUR[vid]
    a, b = VARIANT_EXTRA.get(vid, (light(fur, 30), light(fur, -30)))
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    spots = None
    if vid == "starry":
        spots = [((255, 250, 210), 0.05)]
    elif vid == "golden":
        spots = [((255, 250, 220), 0.06)]
    elif vid == "glitter":
        spots = [((255, 255, 255), 0.07), ((255, 160, 220), 0.06), ((160, 230, 255), 0.05)]
    elif vid == "kaasmoerasguh":
        spots = [((236, 216, 80), 0.12)]
    elif vid == "asguh":
        spots = [((70, 64, 70), 0.12)]
    body = fur if vid != "brococolief" else a
    fleece(img, 0, 0, 16, 16, body, rng, 12, spots)
    if vid == "rainbow":
        px = img.load()
        for y in range(16):
            r, g, bb = colorsys.hsv_to_rgb((y // 2) / 8, 0.42, 0.97)
            for x in range(16):
                px[x, y] = clamp((r * 255 + rng.randint(-8, 8), g * 255 + rng.randint(-8, 8), bb * 255 + rng.randint(-8, 8))) + (255,)
    if vid == "brococolief":        # the pink onesie: a zip and two buttons
        px = img.load()
        for y in range(16):
            px[8, y] = (255, 230, 245, 255)
        px[5, 5] = px[5, 10] = (255, 255, 255, 255)
    # the face (its fur, the guh's eyes, snoet and blush; a dopey vads smile on some)
    fleece(img, 16, 0, 16, 16, fur if vid != "rainbow" else (255, 214, 226), rng, 8, spots if vid in ("starry", "golden", "glitter") else None)
    mood = 3 if vid in ("glitter", "pluisguh", "golden", "koning") else 1 if vid in ("mager", "asguh") else 0
    kt.paint_face(img, mood, 16, 0)
    if vid == "asguh":              # glowing cheeks
        px = img.load()
        for (x, y) in ((1, 11), (2, 11), (13, 11), (14, 11), (1, 12), (14, 12)):
            px[16 + x, y] = (255, 170, 70, 255)
    if vid == "ghost":              # see-through (the model is translucent)
        px = img.load()
        for y in range(32):
            for x in range(32):
                r, g, bb, al = px[x, y]
                if al and not (16 <= x and y < 16 and (r < 120 or g < 150)):
                    px[x, y] = (r, g, bb, 170)
    fleece(img, 0, 16, 8, 8, (255, 160, 196), rng, 8)                   # inside the ears
    fleece(img, 8, 16, 8, 8, light(body, 28), rng, 8)                   # belly
    fleece(img, 16, 16, 8, 8, a, rng, 10)                                 # extra A
    fleece(img, 24, 16, 8, 8, b, rng, 10)                                 # extra B
    if vid == "zeemeerguh":         # scales
        px = img.load()
        for y in range(16, 24):
            for x in range(16, 24):
                if (x + (y // 2) * 2) % 4 == 0:
                    px[x, y] = light(a, 40) + (255,)
    if vid in ("ender", "vahoege_ender"):     # wing membranes with ribs
        px = img.load()
        for y in range(16, 24):
            for x in range(16, 24):
                if (x - 16) == (y - 16) or x == 16:
                    px[x, y] = b + (255,)
    fleece(img, 0, 24, 8, 8, (255, 170, 200), rng, 6)                   # paws, snoet
    fleece(img, 8, 24, 8, 8, light(body, -25), rng, 6)                  # seam
    if vid == "ghost":
        px = img.load()
        for y in range(16, 32):
            for x in range(32):
                r, g, bb, al = px[x, y]
                if al:
                    px[x, y] = (r, g, bb, 170)
    return img


# =====================================================================================================================
# the guh-xylofoon
# =====================================================================================================================
BAR_COLOURS = [(255, 111, 125), (255, 162, 90), (255, 216, 78), (126, 217, 87), (79, 209, 197), (91, 157, 255), (168, 121, 242),
               (255, 138, 208)]


def xylofoon_hout(rng):
    img = Image.new("RGBA", (16, 16))
    fleece(img, 0, 0, 16, 16, (238, 160, 196), rng, 8)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (y + (x // 5)) % 5 == 0:
                px[x, y] = (214, 128, 170, 255)
    return img


def xylofoon_staaf(i, rng):
    c = BAR_COLOURS[i]
    img = Image.new("RGBA", (16, 16))
    fleece(img, 0, 0, 16, 16, c, rng, 6)
    px = img.load()
    for y in range(16):
        px[1, y] = light(c, 60) + (255,)
        px[2, y] = light(c, 30) + (255,)
        px[14, y] = light(c, -40) + (255,)
    for (x, y) in ((7, 2), (8, 2), (7, 13), (8, 13)):
        px[x, y] = (80, 60, 70, 255)            # the nails
    return img


def xylofoon_gezicht(rng):
    img = xylofoon_hout(rng)
    kt.paint_face(img, 3)
    return img


def xylofoon_hamer():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = (200, 150, 110, 255) if y >= 6 else (255, 208, 90, 255) if (x + y) % 3 else (255, 236, 150, 255)
    return img


# =====================================================================================================================
# the grijpmachine
# =====================================================================================================================
def grijp_kast(rng):
    img = Image.new("RGBA", (16, 16))
    fleece(img, 0, 0, 16, 16, (246, 136, 184), rng, 6)
    px = img.load()
    for (x, y) in ((3, 3), (11, 4), (6, 9), (13, 12), (2, 13), (9, 14)):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            px[(x + dx) % 16, (y + dy) % 16] = (255, 226, 240, 255)       # polka dots
    for x in range(16):
        px[x, 0] = (255, 200, 225, 255)
        px[x, 15] = (200, 90, 140, 255)
    return img


def grijp_voorkant(rng):
    img = grijp_kast(rng)
    px = img.load()
    for y in range(3, 9):                        # the chute's flap (left, as seen from the front)
        for x in range(2, 7):
            px[x, y] = (60, 30, 50, 255) if y > 3 else (40, 20, 34, 255)
    for x in range(2, 7):
        px[x, 3] = (255, 230, 120, 255)
    for y in range(4, 9):                        # the coin slot, lit
        px[11, y] = (255, 230, 120, 255) if y in (5, 6, 7) else (60, 30, 50, 255)
        px[12, y] = (60, 30, 50, 255)
    kt_heart = ((10, 11), (11, 11), (13, 11), (14, 11), (9, 12), (10, 12), (11, 12), (12, 12), (13, 12), (14, 12), (15, 12),
                (10, 13), (11, 13), (12, 13), (13, 13), (14, 13), (11, 14), (12, 14), (13, 14), (12, 15))
    for (x, y) in kt_heart:
        px[x - 1, y - 1] = (255, 240, 248, 255)
    return img


def grijp_paneel(rng):
    img = Image.new("RGBA", (16, 16))
    fleece(img, 0, 0, 16, 16, (90, 60, 100), rng, 6)
    px = img.load()
    for x in range(16):
        px[x, 0] = px[x, 15] = (246, 136, 184, 255)
    for (x, y) in ((3, 5), (4, 5), (3, 6), (4, 6)):
        px[x, y] = (30, 20, 30, 255)
    return img


def grijp_glas():
    img = Image.new("RGBA", (16, 16), (200, 235, 255, 60))
    px = img.load()
    for i in range(16):
        for d in (0, 1):
            x, y = i, 15 - i + d * 3
            if 0 <= y < 16:
                px[x, y] = (255, 255, 255, 110)
    for i in range(16):
        px[i, 0] = px[i, 15] = px[0, i] = px[15, i] = (255, 255, 255, 90)
    return img


def grijp_frame(rng):
    img = Image.new("RGBA", (16, 16))
    fleece(img, 0, 0, 16, 16, (255, 214, 110), rng, 10)
    px = img.load()
    for y in range(16):
        px[0, y] = (255, 240, 170, 255)
        px[15, y] = (200, 150, 50, 255)
    return img


def grijp_bord(rng):
    """The sign on top: pink with little light bulbs round it and a big happy guh face."""
    img = Image.new("RGBA", (16, 16))
    fleece(img, 0, 0, 16, 16, (255, 190, 222), rng, 6)
    kt.paint_face(img, 3)
    px = img.load()
    for i in range(0, 16, 3):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px[x, y] = (255, 240, 130, 255)
    return img


def grijp_klauw(rng):
    img = Image.new("RGBA", (16, 16))
    fleece(img, 0, 0, 16, 16, (200, 206, 220), rng, 10)
    px = img.load()
    for y in range(16):
        px[0, y] = (240, 244, 255, 255)
        px[15, y] = (130, 136, 150, 255)
    return img


def rood(rng):
    img = Image.new("RGBA", (16, 16))
    fleece(img, 0, 0, 16, 16, (240, 60, 90), rng, 10)
    px = img.load()
    px[4, 4] = px[5, 4] = px[4, 5] = (255, 200, 210, 255)
    return img


# =====================================================================================================================
# items (16 x 16 pixel art)
# =====================================================================================================================
def pixel_art(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                img.putpixel((x, y), tuple(palette[ch]) + (255,) if len(palette[ch]) == 3 else tuple(palette[ch]))
    return img


IJSJE = [
    "................",
    "......ooo.......",
    "....oobbbbo.....",
    "...obbbwbbbo....",
    "..obbwwbbbtbo...",
    "..obbbbbbtbbo...",
    "..obbbbbbbbbo...",
    "...oddddddddo...",
    "...ohhkhhkhho...",
    "....ohkhhkho....",
    "....okhhkhho....",
    ".....ohkhho.....",
    ".....ohhkho.....",
    "......okho......",
    "......oho.......",
    ".......o........",
]
SMAKEN = {
    "roze": ((255, 158, 200), (225, 100, 160), (255, 90, 120)),
    "mint": ((159, 240, 207), (90, 200, 160), (90, 55, 40)),
    "choco": ((150, 95, 64), (100, 58, 40), (255, 240, 200)),
    "bloesem": ((255, 210, 234), (240, 150, 196), (255, 120, 180)),
    "zonnetje": ((255, 216, 90), (240, 170, 40), (255, 120, 60)),
    "appeltaart": ((224, 161, 90), (180, 110, 50), (140, 190, 70)),
    "sneeuw": ((242, 248, 255), (190, 214, 240), (130, 190, 255)),
}


def kaasijsje(smaak):
    bol, dark, top = SMAKEN[smaak]
    pal = {"o": (70, 40, 50), "b": bol, "w": light(bol, 50), "d": dark, "t": top, "h": (232, 180, 110), "k": (190, 130, 70)}
    img = pixel_art(IJSJE, pal)
    px = img.load()
    extra = {"mint": [(5, 4), (8, 5), (10, 3)], "bloesem": [(5, 3), (9, 4)], "zonnetje": [(6, 5), (9, 3), (10, 5)],
             "appeltaart": [(5, 5), (7, 3), (10, 5)], "sneeuw": [(5, 3), (8, 5), (10, 4)], "roze": [(6, 3)], "choco": [(5, 4), (9, 5)]}
    for (x, y) in extra[smaak]:
        px[x, y] = top + (255,)
    if smaak == "zonnetje":        # a little sun on top
        for (x, y) in ((7, 0), (8, 0), (6, 1), (9, 1)):
            px[x, y] = (255, 200, 40, 255)
    if smaak == "sneeuw":
        px[8, 0] = (255, 255, 255, 255)
    return img


def fluitje():
    return pixel_art([
        "................",
        "...pp...........",
        "..p..p..........",
        ".p....p.........",
        ".p.....p........",
        "..p.....p.......",
        "...p.....p......",
        "....p.sssssss...",
        ".....sWWWWWWss..",
        ".....sWggggWWss.",
        ".....sWgkkgWWs..",
        ".....sWWggWWss..",
        "......sssssss...",
        "................",
        "................",
        "................"], {"p": (246, 120, 180), "s": (120, 124, 140), "W": (220, 226, 236), "g": (180, 186, 200), "k": (40, 40, 50)})


def marshmallow():
    return pixel_art([
        "................",
        "..........wwww..",
        ".........wwwwpw.",
        ".........wwpwwp.",
        ".........wppwww.",
        "..........wwww..",
        ".........bb.....",
        "........bb......",
        ".......bb.......",
        "......bb........",
        ".....bb..yy.....",
        "....bb..yyyy....",
        "...bb....yy.....",
        "..bb............",
        ".bb.............",
        "................"], {"w": (255, 246, 250), "p": (255, 190, 220), "b": (150, 100, 60), "y": (250, 200, 70)})


def liedjesboekje():
    return pixel_art([
        "................",
        "..kkkkkkkkkkk...",
        "..kpppppppppk...",
        "..kpwwwwwwwpkl..",
        "..kpw....n.pkl..",
        "..kpw...nn.pkl..",
        "..kpw..n.n.pkl..",
        "..kpw.nn...pkl..",
        "..kpw.nn...pkl..",
        "..kpwwwwwwwpkl..",
        "..kpppppppppkl..",
        "..kpppgpppppkl..",
        "..kppgggppppkl..",
        "..kkkkkkkkkkkl..",
        "...llllllllll...",
        "................"], {"k": (120, 40, 80), "p": (246, 136, 184), "w": (255, 244, 248), "n": (60, 30, 50), "l": (255, 236, 244),
                              "g": (255, 216, 78)})


def grijpmachine_icoon():
    return pixel_art([
        "...kkkkkkkkkk...",
        "...kyyyyyyyyk...",
        "...kkkkkkkkkk...",
        "...kggggggggk...",
        "...kgg.s..ggk...",
        "...kgg.s..ggk...",
        "...kg.sss..gk...",
        "...kg.s.s..gk...",
        "...kgaa.bb.gk...",
        "...kaaabbbccb...",
        "...kkkkkkkkkk...",
        "...kpppppppr k..",
        "...kpddpppppk...",
        "...kpddpppppk...",
        "...kpppppppp k..",
        "...kkkkkkkkkk..."], {"k": (120, 40, 80), "y": (255, 216, 78), "g": (190, 226, 250), "s": (200, 206, 220), "a": (195, 160, 205),
                              "b": (170, 226, 200), "c": (242, 200, 96), "p": (246, 136, 184), "d": (60, 30, 50), "r": (240, 60, 90)})


# =====================================================================================================================
# particles
# =====================================================================================================================
def particle_noot(i):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    pts = [(4, 0), (5, 0), (4, 1), (6, 1), (4, 2), (4, 3), (4, 4), (2, 4), (3, 4), (1, 5), (2, 5), (3, 5), (4, 5), (1, 6), (2, 6), (3, 6), (2, 7)] \
        if i == 0 else [(2, 0), (3, 0), (4, 0), (5, 0), (6, 0), (2, 1), (6, 1), (2, 2), (6, 2), (2, 3), (6, 3), (0, 4), (1, 4), (2, 4), (4, 4),
                        (5, 4), (6, 4), (0, 5), (1, 5), (4, 5), (5, 5), (0, 6), (1, 6), (4, 6), (5, 6)]
    for p in pts:
        img.putpixel(p, (255, 255, 255, 255))
    return img


def particle_hartje(i):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    col = [(255, 120, 170), (255, 170, 205), (255, 90, 140)][i]
    for (x, y) in ((1, 1), (2, 1), (5, 1), (6, 1), (0, 2), (1, 2), (2, 2), (3, 2), (4, 2), (5, 2), (6, 2), (7, 2), (0, 3), (1, 3), (2, 3), (3, 3),
                   (4, 3), (5, 3), (6, 3), (7, 3), (1, 4), (2, 4), (3, 4), (4, 4), (5, 4), (6, 4), (2, 5), (3, 5), (4, 5), (5, 5), (3, 6), (4, 6)):
        img.putpixel((x, y), col + (255,))
    img.putpixel((2, 2), (255, 240, 248, 255))
    return img


def particle_stoom(i):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    r = 2.6 + i * 0.6
    for y in range(8):
        for x in range(8):
            d = math.dist((x + 0.5, y + 0.5), (4, 4))
            if d < r:
                img.putpixel((x, y), (255, 255, 255, int(220 * (1 - d / r))))
    return img


# =====================================================================================================================
# the guh's ice-cream hat, blushing cheeks, the koorstrikje and the ijscopetje (clothes swatches, numpy arrays)
# =====================================================================================================================
def swatch_hoorntje(v, rng):
    a = v.fabric((226, 172, 104), rng, 8)
    a[::6, :] = (180, 124, 64)
    a[:, ::6] = (180, 124, 64)
    return a


def swatch_bolletje(v, rng):
    np = __import__("numpy")
    a = v.fabric((250, 246, 244), rng, 5)
    for y in range(a.shape[0]):
        a[y, :] = np.clip(a[y, :] - (y % 8) * 3, 0, 255)       # soft swirls (tinted by the flavour)
    return a


def swatch_kersje(v, rng):
    a = v.fabric((226, 36, 64), rng, 8)
    a[2:6, 2:6] = (255, 150, 160)
    return a


def swatch_blosjes(v, rng):
    a = v.fabric((255, 128, 176), rng, 6)
    a[4:10, 4:10] = (255, 190, 214)
    return a


def swatch_koorstrikje(v, rng):
    a = v.fabric((140, 90, 220), rng, 8)
    a[:, ::5] = (255, 216, 78)                                     # little gold notes
    return a


def swatch_ijscopet(v, rng):
    return v.stripes((255, 250, 252), (246, 136, 184), rng, 4)


# =====================================================================================================================
# IJscoguh Tingeling's bike: swatches (32 x 32 px each, 8 UV units at 4 px)
# =====================================================================================================================
def bike_swatch(name, rng):
    np = __import__("numpy")
    S = 32
    a = np.zeros((S, S, 4), np.uint8)

    def fill(c, var=8):
        for y in range(S):
            for x in range(S):
                v = rng.randint(-var, var)
                a[y, x] = clamp(ch + v for ch in c) + (255,)

    if name == "wiel":
        for y in range(S):
            for x in range(S):
                d = math.dist((x + 0.5, y + 0.5), (S / 2, S / 2))
                if d <= S / 2:
                    if d > S / 2 - 4:
                        a[y, x] = (70, 60, 72, 255)                 # the tyre
                    elif d < 4:
                        a[y, x] = (255, 216, 78, 255)               # the hub
                    elif abs((math.atan2(y - S / 2, x - S / 2) * 6 / math.pi) % 1) < 0.18:
                        a[y, x] = (230, 230, 240, 255)              # spokes
                    else:
                        a[y, x] = (246, 150, 196, 255) if d < S / 2 - 5 else (255, 200, 225, 255)
    elif name == "band":
        fill((70, 60, 72), 6)
    elif name == "bak":
        for y in range(S):
            for x in range(S):
                a[y, x] = ((255, 250, 252) if (x // 4) % 2 else (246, 136, 184)) + (255,)
        a[:3, :] = (255, 216, 78, 255)
        a[-3:, :] = (200, 90, 140, 255)
    elif name == "bak_voor":
        from PIL import Image as _I
        img = _I.new("RGBA", (16, 16))
        fleece(img, 0, 0, 16, 16, (255, 244, 248), rng, 4)
        kt.paint_face(img, 3)
        img = img.resize((S, S), _I.NEAREST)
        a[:] = np.asarray(img)
        a[:2, :] = (255, 216, 78, 255)
        a[-2:, :] = (246, 136, 184, 255)
    elif name == "deksel":
        fill((255, 250, 252), 4)
        a[::8, :] = (246, 136, 184, 255)
    elif name == "hoorn":
        fill((226, 172, 104), 8)
        a[::5, :] = (180, 124, 64, 255)
        a[:, ::5] = (180, 124, 64, 255)
    elif name in ("bol_roze", "bol_mint", "bol_choco"):
        c = {"bol_roze": (255, 158, 200), "bol_mint": (159, 240, 207), "bol_choco": (150, 95, 64)}[name]
        fill(c, 10)
        for _ in range(12):
            x, y = rng.randrange(S - 2), rng.randrange(S - 2)
            a[y:y + 2, x:x + 2] = clamp(light(c, 50)) + (255,)
    elif name == "frame":
        fill((246, 136, 184), 6)
        a[:, :4] = (255, 190, 222, 255)
    elif name == "zadel":
        fill((150, 90, 110), 8)
    elif name == "handvat":
        fill((255, 120, 180), 6)
    elif name == "bel":
        fill((255, 214, 90), 12)
        a[4:10, 4:10] = (255, 245, 190, 255)
    elif name == "parasol":
        for y in range(S):
            for x in range(S):
                ang = math.atan2(y - S / 2, x - S / 2)
                a[y, x] = ((255, 250, 252) if int((ang + math.pi) / (math.pi / 4)) % 2 else (246, 136, 184)) + (255,)
    elif name == "paal":
        fill((255, 250, 252), 4)
        a[::6, :] = (246, 136, 184, 255)
    elif name == "pet":
        fill((255, 250, 252), 5)
    elif name == "pet_band":
        fill((246, 136, 184), 6)
    else:
        fill((255, 0, 255), 0)
    return a
