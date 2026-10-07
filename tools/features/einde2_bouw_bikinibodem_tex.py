"""
einde2, slice bouw_bikinibodem: the textures of the blocks of Bikinibodem (called by einde2_bouw_bikinibodem.build).

  - zeepbel: a soap film: almost nothing, with slanting bands of pink, sea green and yellow and one glint;
  - bubbelstroom: bubbles on nothing, eight frames that scroll (one texture going up, one going down);
  - bloemwolk: four soft pastel clouds with a flower pressed into each;
  - the eight decorative blocks: gloeikoraal, the pylon, four self-portraits of Octo Tentakel (the long nose, the bald
    dome, the tired eyes), his clarinet, the safe, the till, the camera, the filing cabinet.

Every painter is seeded from a NAME (kit.seed), never from an index.
    textures(h) -> writes them all; BLOCKS lists what it wrote (the self-check of einde2_bouw_bikinibodem reads it).
"""
import math
import random

from PIL import Image

import features.einde2_kit as kit

P = "einde2_bouw_bikinibodem_"
BLOCKS = []


def _rng(name):
    return random.Random(kit.seed("tex/bikinibodem/" + name))


def _mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def _raster(rijen, palet):
    img = Image.new("RGBA", (len(rijen[0]), len(rijen)), (0, 0, 0, 0))
    for y, rij in enumerate(rijen):
        for x, ch in enumerate(rij):
            if ch in palet:
                c = palet[ch]
                img.putpixel((x, y), tuple(c) + ((255,) if len(c) == 3 else ()))
    return img


def zeepbel():
    """A soap film: slanting bands of colour that fade in and out, and a glint in the corner."""
    r = _rng("zeepbel")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    kleuren = [(255, 170, 215), (150, 235, 225), (255, 240, 170), (190, 180, 255)]
    for y in range(16):
        for x in range(16):
            t = ((x + y) % 16) / 16.0 * len(kleuren)
            c = _mix(kleuren[int(t) % 4], kleuren[(int(t) + 1) % 4], t - int(t))
            golf = 0.5 + 0.5 * math.sin((x - y) * math.pi / 8.0)
            a = int(22 + 30 * golf + r.randint(0, 6))
            img.putpixel((x, y), c + (a,))
    for (x, y, a) in ((3, 2, 150), (4, 2, 120), (2, 3, 130), (2, 4, 90), (5, 3, 60), (12, 12, 70), (13, 11, 50)):
        img.putpixel((x, y), (255, 255, 255, a))
    return img


def bubbelstroom(omhoog):
    """Eight frames of bubbles that drift (two pixels a frame), on nothing: 16 x 128."""
    r = _rng("bubbelstroom")
    bellen = [(r.randrange(16), r.randrange(16), r.choice((1, 1, 2, 2, 3))) for _ in range(7)]
    img = Image.new("RGBA", (16, 128), (0, 0, 0, 0))
    for f in range(8):
        for (bx, by, straal) in bellen:
            cy = (by - f * 2 * (1 if omhoog else -1)) % 16
            for dy in range(-straal, straal + 1):
                for dx in range(-straal, straal + 1):
                    d = math.hypot(dx, dy)
                    if d > straal + 0.3:
                        continue
                    x, y = (bx + dx) % 16, (cy + dy) % 16
                    rand = d > straal - 0.8
                    c = (215, 245, 255, 210) if rand else (150, 215, 255, 70)
                    if (dx, dy) == (-max(0, straal - 1), -max(0, straal - 1)) and straal > 1:
                        c = (255, 255, 255, 240)
                    img.putpixel((x, f * 16 + y), c)
    return img


BLOEM_STEMPEL = ["..#.#..",
                 ".#####.",
                 "###o###",
                 ".#ooo#.",
                 "###o###",
                 ".#####.",
                 "..#.#.."]
WOLK_KLEUR = [((255, 190, 220), (255, 228, 240), (236, 130, 180)),      # roze
              ((176, 236, 214), (224, 250, 240), (96, 196, 168)),       # zeegroen
              ((255, 236, 160), (255, 248, 214), (240, 190, 70)),       # geel
              ((208, 190, 250), (234, 226, 255), (150, 124, 226))]      # lila


def bloemwolk(i):
    basis, licht, bloem = WOLK_KLEUR[i]
    r = _rng(f"bloemwolk_{i}")
    img = Image.new("RGBA", (16, 16), basis + (255,))
    for _ in range(9):                           # puffs
        cx, cy, s = r.randrange(16), r.randrange(16), r.choice((1, 2, 2))
        for dy in range(-s, s + 1):
            for dx in range(-s, s + 1):
                if dx * dx + dy * dy <= s * s:
                    img.putpixel(((cx + dx) % 16, (cy + dy) % 16), _mix(basis, licht, 0.8) + (255,))
    ox, oy = 4 + i % 2, 5 - i % 2
    for y, rij in enumerate(BLOEM_STEMPEL):
        for x, ch in enumerate(rij):
            if ch == "#":
                img.putpixel((ox + x, oy + y), bloem + (255,))
            elif ch == "o":
                img.putpixel((ox + x, oy + y), licht + (255,))
    return img


def koraal():
    r = _rng("koraal")
    donker, basis, licht, gloed = (206, 70, 86), (255, 132, 96), (255, 190, 130), (255, 244, 190)
    img = Image.new("RGBA", (16, 16), basis + (255,))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), _mix(basis, licht, r.random() * 0.35) + (255,))
    for _ in range(11):                          # knobs with a pore in the middle
        cx, cy = r.randrange(16), r.randrange(16)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            img.putpixel(((cx + dx) % 16, (cy + dy) % 16), licht + (255,))
        img.putpixel((cx, cy), donker + (255,))
    for _ in range(8):
        img.putpixel((r.randrange(16), r.randrange(16)), gloed + (255,))
    return img


def pylon():
    """Orange and white in blocks of four pixels: the model picks its colours by uv."""
    oranje, donker, wit, grijs = (242, 118, 28), (196, 84, 16), (244, 244, 238), (206, 206, 200)
    img = Image.new("RGBA", (16, 16), oranje + (255,))
    for y in range(16):
        for x in range(16):
            c = wit if x >= 8 else oranje
            if y >= 8:
                c = (70, 62, 58) if x < 8 else donker
            if (x + y) % 5 == 0:
                c = _mix(c, grijs if x >= 8 and y < 8 else donker, 0.35)
            img.putpixel((x, y), c + (255,))
    return img


PORTRET_VOOR = ["FFFFFFFFFFFFFFFF",
                "FBBBBBBBBBBBBBBF",
                "FBBBBSSSSSSBBBBF",
                "FBBBSSSSSSSSBBBF",
                "FBBBSSSSSSSSBBBF",
                "FBBSsssSSsssSBBF",
                "FBBSEPsSSEPsSBBF",
                "FBBSSSSssSSSSBBF",
                "FBBBSSSssSSSBBBF",
                "FBBBSSSssSSSBBBF",
                "FBBBBSMssMSBBBBF",
                "FBBBBSSssSSBBBBF",
                "FBBBBBSssSBBBBBF",
                "FBBBHHHssHHHBBBF",
                "FBBHHHHHHHHHHBBF",
                "FFFFFFFFFFFFFFFF"]
PORTRET_OPZIJ = ["FFFFFFFFFFFFFFFF",
                 "FBBBBBBBBBBBBBBF",
                 "FBBBBBSSSSSBBBBF",
                 "FBBBBSSSSSSSBBBF",
                 "FBBBBsssSSSSBBBF",
                 "FBBBBSEPSSSSBBBF",
                 "FBBBSSSSSSSSBBBF",
                 "FBBSsSSSSSSSBBBF",
                 "FBBSsSMMSSSBBBBF",
                 "FBBssKKSSSSBBBBF",
                 "FBKKBBSSSSBBBBBF",
                 "FKKBBBHHHHHBBBBF",
                 "FKBBBHHHHHHHBBBF",
                 "FBBBHHHHHHHHHBBF",
                 "FBBBHHHHHHHHHBBF",
                 "FFFFFFFFFFFFFFFF"]
# soort -> (the picture, the palette): classic, his blue period, pop art, in profile with the clarinet
PORTRET_PALET = [
    (PORTRET_VOOR, {"F": (201, 152, 42), "B": (96, 60, 44), "S": (126, 176, 172), "s": (86, 132, 134), "E": (240, 240, 220), "P": (96, 34, 34),
                    "M": (48, 70, 72), "H": (156, 104, 52)}),
    (PORTRET_VOOR, {"F": (60, 70, 110), "B": (34, 52, 110), "S": (120, 160, 220), "s": (70, 104, 176), "E": (226, 236, 250), "P": (20, 30, 80),
                    "M": (30, 44, 100), "H": (52, 76, 150)}),
    (PORTRET_VOOR, {"F": (250, 250, 250), "B": (255, 214, 60), "S": (240, 96, 160), "s": (176, 50, 120), "E": (255, 255, 255), "P": (40, 40, 60),
                    "M": (60, 30, 70), "H": (70, 200, 220)}),
    (PORTRET_OPZIJ, {"F": (120, 84, 40), "B": (176, 204, 196), "S": (126, 176, 172), "s": (86, 132, 134), "E": (240, 240, 220), "P": (96, 34, 34),
                     "M": (48, 70, 72), "H": (156, 104, 52), "K": (26, 24, 30)}),
]


def zelfportret(i):
    rijen, palet = PORTRET_PALET[i]
    img = _raster(rijen, palet)
    f = palet["F"]
    for k in range(16):                          # the frame has a lit and a dark side
        img.putpixel((k, 0), _mix(f, (255, 255, 255), 0.25) + (255,))
        img.putpixel((0, k), _mix(f, (255, 255, 255), 0.25) + (255,))
        img.putpixel((k, 15), _mix(f, (0, 0, 0), 0.3) + (255,))
        img.putpixel((15, k), _mix(f, (0, 0, 0), 0.3) + (255,))
    return img


def klarinet():
    """Left half: the black tube with silver rings (rows = height); right half: the wooden stand."""
    zwart, glans, zilver, hout, hout_d = (28, 26, 32), (62, 58, 70), (206, 210, 216), (150, 104, 60), (112, 76, 42)
    img = Image.new("RGBA", (16, 16), zwart + (255,))
    for y in range(16):
        for x in range(8):
            c = glans if x in (2, 3) else zwart
            if y in (3, 6, 9, 12):
                c = zilver
            img.putpixel((x, y), c + (255,))
        for x in range(8, 16):
            img.putpixel((x, y), (hout_d if (y % 4 == 3 or x == 8) else hout) + (255,))
    return img


def staal(name, basis, donker, licht):
    r = _rng(name)
    img = Image.new("RGBA", (16, 16), basis + (255,))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), _mix(basis, licht if r.random() < 0.5 else donker, r.random() * 0.18) + (255,))
    for k in range(16):
        img.putpixel((k, 0), licht + (255,))
        img.putpixel((0, k), licht + (255,))
        img.putpixel((k, 15), donker + (255,))
        img.putpixel((15, k), donker + (255,))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        img.putpixel((x, y), donker + (255,))
    return img


def kluis_voor():
    basis, donker, licht = (128, 136, 144), (78, 84, 92), (190, 196, 202)
    img = staal("kluis_voor", basis, donker, licht)
    for y in range(2, 14):                       # the door
        for x in range(2, 14):
            if x in (2, 13) or y in (2, 13):
                img.putpixel((x, y), donker + (255,))
    for (dx, dy) in ((0, -2), (1, -2), (-1, -1), (2, -1), (-2, 0), (2, 0), (-2, 1), (2, 1), (-1, 2), (0, 2), (1, 2), (-1, -2), (-2, -1)):   # the dial
        img.putpixel((6 + dx, 8 + dy), (40, 44, 50, 255))
    img.putpixel((6, 8), (232, 196, 70, 255))
    img.putpixel((6, 7), (232, 196, 70, 255))
    for y in range(6, 11):                       # the handle
        img.putpixel((11, y), licht + (255,))
    img.putpixel((10, 8), licht + (255,))
    return img


def archief_voor():
    basis, donker, licht = (122, 140, 126), (76, 92, 82), (176, 190, 178)
    img = staal("archief_voor", basis, donker, licht)
    for la in range(3):
        y0 = 1 + la * 5
        for x in range(1, 15):
            img.putpixel((x, y0), licht + (255,))
            img.putpixel((x, y0 + 4), donker + (255,))
        for x in range(6, 10):                   # the handle
            img.putpixel((x, y0 + 3), (48, 54, 50, 255))
        for x in range(5, 11):                   # the label
            img.putpixel((x, y0 + 1), (246, 240, 214, 255))
        img.putpixel((6 + la * 2, y0 + 1), (60, 60, 70, 255))
    return img


def kassa():
    """uv blocks: (0..8, 0..8) the body, (8..16, 0..8) the screen, (0..8, 8..16) the keys, (8..16, 8..16) the little bow."""
    body, body_d, scherm, cijfer = (88, 150, 160), (56, 104, 116), (22, 44, 30), (130, 240, 120)
    img = Image.new("RGBA", (16, 16), body + (255,))
    for y in range(16):
        for x in range(16):
            c = body_d if (x % 8 in (0, 7) or y % 8 in (0, 7)) else body
            if x >= 8 and y < 8:
                c = scherm
            if x < 8 and y >= 8:
                c = (232, 226, 206) if (x % 2 == 1 and y % 2 == 1) else body_d
            if x >= 8 and y >= 8:
                c = (176, 60, 52) if y < 12 else (236, 232, 220)
            img.putpixel((x, y), c + (255,))
    # (green digits: a price nobody likes)
    for (x, y) in ((10, 3), (10, 4), (12, 2), (12, 3), (12, 4), (12, 5), (13, 2), (13, 5), (14, 2), (14, 3), (14, 4), (14, 5)):
        img.putpixel((x, y), cijfer + (255,))
    return img


def camera():
    """uv blocks: (0..4, 0..4) the lens, (4..16, 0..8) the body, (0..4, 8..12) the mount."""
    body, body_d, body_l = (150, 154, 160), (96, 100, 108), (206, 210, 214)
    img = Image.new("RGBA", (16, 16), body + (255,))
    for y in range(16):
        for x in range(16):
            c = body_l if y == 0 else body_d if y in (7, 15) or x in (4, 15) else body
            img.putpixel((x, y), c + (255,))
    for y in range(4):
        for x in range(4):
            rand = x in (0, 3) or y in (0, 3)
            img.putpixel((x, y), ((34, 36, 44) if rand else (30, 52, 96)) + (255,))
    img.putpixel((1, 1), (170, 210, 255, 255))
    for y in range(8, 12):
        for x in range(4):
            img.putpixel((x, y), body_d + (255,))
    img.putpixel((13, 2), (230, 40, 40, 255))    # the little red light: Karen is watching
    return img


def textures(h):
    BLOCKS.clear()

    def bewaar(img, name):
        h.save(img, "block", f"{name}.png")
        BLOCKS.append(name)

    bewaar(zeepbel(), "zeepbel")
    for richting in ("omhoog", "omlaag"):
        bewaar(bubbelstroom(richting == "omhoog"), f"bubbelstroom_{richting}")
        h.w(f"{h.A}/textures/block/bubbelstroom_{richting}.png.mcmeta", {"animation": {"frametime": 2}})
    for i in range(4):
        bewaar(bloemwolk(i), f"bloemwolk_{i}")
        bewaar(zelfportret(i), f"{P}zelfportret_{i}")
    bewaar(koraal(), f"{P}koraal")
    bewaar(pylon(), f"{P}pylon")
    bewaar(klarinet(), f"{P}klarinet")
    bewaar(staal("kluis", (128, 136, 144), (78, 84, 92), (190, 196, 202)), f"{P}kluis")
    bewaar(kluis_voor(), f"{P}kluis_voor")
    bewaar(staal("archief", (122, 140, 126), (76, 92, 82), (176, 190, 178)), f"{P}archiefkast")
    bewaar(archief_voor(), f"{P}archiefkast_voor")
    bewaar(kassa(), f"{P}kassa")
    bewaar(camera(), f"{P}camera")


def preview(out):
    """A sheet of every texture, eight times enlarged (python tools/features/einde2_bouw_bikinibodem_tex.py <png>)."""
    plaatjes = [zeepbel(), bubbelstroom(True).crop((0, 0, 16, 16)), bubbelstroom(False).crop((0, 16, 16, 32))] + [bloemwolk(i) for i in range(4)] + \
        [koraal(), pylon()] + [zelfportret(i) for i in range(4)] + [klarinet(), kluis_voor(), archief_voor(), kassa(), camera()]
    sheet = Image.new("RGBA", (len(plaatjes) * 17 * 8, 16 * 8), (60, 40, 80, 255))
    for i, p in enumerate(plaatjes):
        groot = p.resize((128, 128), Image.NEAREST)
        sheet.alpha_composite(groot, (i * 136, 0))
    sheet.save(out)


if __name__ == "__main__":
    import sys
    preview(sys.argv[1])
