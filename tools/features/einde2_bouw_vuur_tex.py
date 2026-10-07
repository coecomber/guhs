"""
einde2, slice bouw_vuur: the textures of the six decoration blocks of the Vuurnatie-hoofdstad (called by
einde2_bouw_vuur.build).

  lauwe_lava       lava that went lukewarm: the glow of lava with a skin of molten cheese on it (animated, slow)
  lauwe_vlam       a flame you can put your paw in (a cross model, animated)
  embleem          the emblem of the Vuurnatie: a golden flame with Mika's angry eyes and his one fang
  portret_mikai    Vuurheer Mikai in a golden frame (the four sides; _top is the frame from above)
  zonnesteen       the carved sun of the Zonnekrijgers in sandstone
  lotustegel       the tile of the Witte Lotus: a white flower on a round white stone

Every painter is seeded from a NAME (kit.seed), never from an index.
"""
import math
import random

from PIL import Image

import features.einde2_kit as kit

BLOCKS = ["einde2_bouw_vuur_lauwe_lava", "einde2_bouw_vuur_lauwe_vlam", "einde2_bouw_vuur_embleem", "einde2_bouw_vuur_portret_mikai",
          "einde2_bouw_vuur_portret_mikai_top", "einde2_bouw_vuur_zonnesteen", "einde2_bouw_vuur_lotustegel"]
FRAMES_LAVA, FRAMES_VLAM = 16, 8

GOUD, GOUD_LICHT, GOUD_DONKER = (232, 176, 44), (255, 226, 120), (168, 112, 20)
VLAM_KERN, VLAM_GEEL, VLAM_ORANJE, VLAM_ROOD = (255, 246, 190), (255, 204, 70), (246, 136, 28), (208, 78, 22)
MIKA, MIKA_LICHT, MIKA_DONKER = (122, 42, 173), (156, 78, 204), (84, 26, 124)


def _rng(name):
    return random.Random(kit.seed("tex/bouw_vuur/" + name))


def _mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def _ramp(stops, t):
    """A colour on a ramp of (position, colour) stops."""
    t = max(0.0, min(1.0, t))
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        if t <= p1:
            return _mix(c0, c1, (t - p0) / (p1 - p0) if p1 > p0 else 0.0)
    return stops[-1][1]


def grid(rows, palette, base=None):
    """An image from rows of characters; a character that is not in the palette leaves the base (or nothing)."""
    img = base.copy() if base else Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                img.putpixel((x, y), tuple(palette[ch]) + (255,))
    return img


def lauwe_lava():
    """Sixteen frames under each other: slow swirls that run in a loop (every wave makes a whole number of turns), so the
    animation has no jump."""
    r = _rng("lauwe_lava")
    korrel = [[r.uniform(-0.05, 0.05) for _ in range(16)] for _ in range(16)]
    ramp = [(0.0, (188, 70, 14)), (0.35, (230, 112, 22)), (0.6, (250, 162, 42)), (0.82, (255, 212, 96)), (1.0, (255, 240, 176))]
    img = Image.new("RGBA", (16, 16 * FRAMES_LAVA))
    tau = 2 * math.pi
    for f in range(FRAMES_LAVA):
        t = f / FRAMES_LAVA
        for y in range(16):
            for x in range(16):
                u, v = x / 16.0, y / 16.0
                a = math.sin(tau * (u + 0.35 * math.sin(tau * (v + t))))
                b = math.sin(tau * (2 * v - u + t) + 1.3)
                c = math.sin(tau * (2 * u + v - t) + 0.4)
                w = 0.5 + 0.24 * a + 0.16 * b + 0.12 * c + korrel[y][x]
                img.putpixel((x, f * 16 + y), _ramp(ramp, w) + (255,))
    return img


def lauwe_vlam():
    """Eight frames: a flame with a pale core, flickering tongues and loose sparks, see-through around it."""
    r = _rng("lauwe_vlam")
    img = Image.new("RGBA", (16, 16 * FRAMES_VLAM), (0, 0, 0, 0))
    tau = 2 * math.pi
    for f in range(FRAMES_VLAM):
        t = f / FRAMES_VLAM
        for x in range(16):
            # how high the flame stands in this column: a wide foot, three tongues that sway
            d = abs(x - 7.5) / 7.5
            hoog = 13.5 * (1 - d ** 1.6) + 2.2 * math.sin(tau * (x / 5.0 + t)) + 1.4 * math.sin(tau * (x / 3.0 - 2 * t) + 1.0)
            hoog = max(0.0, min(15.0, hoog))
            for k in range(int(round(hoog)) + 1):
                rel = k / max(hoog, 1.0)
                kleur = _ramp([(0.0, VLAM_KERN), (0.3, VLAM_GEEL), (0.68, VLAM_ORANJE), (1.0, VLAM_ROOD)], rel * (0.75 + 0.5 * d))
                img.putpixel((x, f * 16 + 15 - k), kleur + (255,))
        for _ in range(3):
            x, y = r.randrange(2, 14), r.randrange(0, 6)
            if img.getpixel((x, f * 16 + y))[3] == 0:
                img.putpixel((x, f * 16 + y), VLAM_GEEL + (255,))
    return img


def embleem():
    r = _rng("embleem")
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = r.randint(-8, 8)
            steen = (112 + v, 18 + v // 2, 24 + v // 2)
            if y % 8 == 0 or (x + (4 if (y // 8) % 2 else 0)) % 8 == 0:
                steen = (84 + v, 12, 18)
            img.putpixel((x, y), steen + (255,))
    rows = ["................",
            ".GGGGGGGGGGGGGG.",
            ".G............G.",
            ".G.....y......G.",
            ".G....yy...y..G.",
            ".G....yyy..y..G.",
            ".G..y.yoyy.yy.G.",
            ".G..yyyooyyyy.G.",
            ".G..yyoooooyy.G.",
            ".G..yoKKooKKy.G.",
            ".G..yooKooKoy.G.",
            ".G..yoooooooy.G.",
            ".G...yoWoooy..G.",
            ".G....yyyyy...G.",
            ".GGGGGGGGGGGGGG.",
            "................"]
    return grid(rows, {"G": GOUD, "y": VLAM_GEEL, "o": VLAM_ORANJE, "K": (30, 14, 20), "W": (250, 246, 236)}, img)


def portret_mikai():
    """The Vuurheer: a purple Mika's head with angry brows, red eyes and his fang, a golden flame between his ears, in
    front of a dark red cloth, in a golden frame."""
    rows = ["FfFFFFFFFFFFFFfF",
            "frrrrrryYrrrrrrf",
            "FrrrrryYYyrrrrrF",
            "FrPPrrryyrrrPPrF",
            "FrPppPPPPPPppPrF",
            "FrrPPPPPPPPPPrrF",
            "FrPKKPPPPPPKKPrF",
            "FrPPPKPPPPKPPPrF",
            "FrPPRRPPPPRRPPrF",
            "FrPPRrPPPPRrPPrF",
            "FrPPPPPppPPPPPrF",
            "FrPPKKKKKKKKPPrF",
            "FrrPPWPPPPPPPrrF",
            "FrrrDDDDDDDDrrrF",
            "frrrrrrrrrrrrrrf",
            "FfFFFFFFFFFFFFfF"]
    r = _rng("portret")
    img = grid(rows, {"F": GOUD, "f": GOUD_LICHT, "r": (96, 14, 22), "P": MIKA, "p": MIKA_LICHT, "D": MIKA_DONKER, "K": (20, 12, 26),
                      "R": (232, 40, 34), "W": (250, 246, 236), "y": VLAM_ORANJE, "Y": VLAM_GEEL})
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = px[x, y]
            if c[:3] in ((96, 14, 22), MIKA):
                v = r.randint(-6, 6)
                px[x, y] = (max(0, c[0] + v), max(0, c[1] + v // 2), max(0, c[2] + v), 255)
            elif c[:3] == GOUD and (x in (0, 15) or y in (0, 15)) and (x + y) % 5 == 0:
                px[x, y] = GOUD_DONKER + (255,)
    return img


def portret_top():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            rand = min(x, y, 15 - x, 15 - y)
            img.putpixel((x, y), (GOUD_LICHT if rand == 0 else (GOUD if rand in (1, 2) or (x + y) % 6 else GOUD_DONKER)) + (255,))
    return img


def zonnesteen():
    """Sandstone with the sun of the Zonnekrijgers cut into it: a ring, a golden heart and eight rays."""
    r = _rng("zonnesteen")
    img = Image.new("RGBA", (16, 16))
    licht, midden, donker, groef = (226, 212, 162), (210, 192, 138), (190, 168, 112), (150, 120, 64)
    for y in range(16):
        for x in range(16):
            c = r.choice((licht, midden, midden, donker))
            if min(x, y, 15 - x, 15 - y) == 0:
                c = donker if (x + y) % 3 else groef
            d = math.hypot(x - 7.5, y - 7.5)
            hoek = math.degrees(math.atan2(y - 7.5, x - 7.5)) % 45
            if d < 1.6:
                c = GOUD if d > 0.8 else GOUD_LICHT
            elif 3.0 <= d < 4.1:
                c = groef
            elif 4.6 <= d < 7.0 and min(hoek, 45 - hoek) < 7.0:
                c = groef if d < 6.2 else GOUD_DONKER
            img.putpixel((x, y), c + (255,))
    return img


def lotustegel():
    """A round white stone in a frame of pale wood, a lotus on it: white petals with a blue edge, a golden heart."""
    r = _rng("lotustegel")
    hout, hout_donker, wit, rand, blauw = (214, 196, 150), (176, 152, 104), (250, 250, 246), (206, 212, 224), (112, 156, 220)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 6.6:
                c = wit if d < 5.8 else rand
            else:
                c = hout if (y + r.randint(0, 1)) % 4 else hout_donker
            img.putpixel((x, y), c + (255,))
    rows = ["................",
            "................",
            "......bbbb......",
            ".....bwwwwb.....",
            "...bbbwwwwbbb...",
            "..bwwbbwwbbwwb..",
            "..bwwwbggbwwwb..",
            "..bwwbgGGgbwwb..",
            "..bwwbgGGgbwwb..",
            "..bwwwbggbwwwb..",
            "..bwwbbwwbbwwb..",
            "...bbbwwwwbbb...",
            ".....bwwwwb.....",
            "......bbbb......",
            "................",
            "................"]
    return grid(rows, {"b": blauw, "w": (255, 255, 255), "g": GOUD, "G": GOUD_LICHT}, img)


def textures(h):
    h.save(lauwe_lava(), "block", "einde2_bouw_vuur_lauwe_lava.png")
    h.w(f"{h.TEX}/block/einde2_bouw_vuur_lauwe_lava.png.mcmeta", {"animation": {"frametime": 5, "interpolate": True}})
    h.save(lauwe_vlam(), "block", "einde2_bouw_vuur_lauwe_vlam.png")
    h.w(f"{h.TEX}/block/einde2_bouw_vuur_lauwe_vlam.png.mcmeta", {"animation": {"frametime": 2, "interpolate": False}})
    h.save(embleem(), "block", "einde2_bouw_vuur_embleem.png")
    h.save(portret_mikai(), "block", "einde2_bouw_vuur_portret_mikai.png")
    h.save(portret_top(), "block", "einde2_bouw_vuur_portret_mikai_top.png")
    h.save(zonnesteen(), "block", "einde2_bouw_vuur_zonnesteen.png")
    h.save(lotustegel(), "block", "einde2_bouw_vuur_lotustegel.png")


def preview(path):
    """One picture of all of them, eight times enlarged (the animated ones: their first four frames)."""
    tiles = [lauwe_lava().crop((0, 16 * k, 16, 16 * k + 16)) for k in (0, 4, 8, 12)] + \
            [lauwe_vlam().crop((0, 16 * k, 16, 16 * k + 16)) for k in (0, 2, 4, 6)] + \
            [embleem(), portret_mikai(), portret_top(), zonnesteen(), lotustegel()]
    sheet = Image.new("RGBA", (len(tiles) * 18 + 2, 20), (40, 40, 48, 255))
    for k, t in enumerate(tiles):
        sheet.alpha_composite(t, (2 + k * 18, 2))
    sheet.resize((sheet.width * 8, sheet.height * 8), Image.NEAREST).save(path)
