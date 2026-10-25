"""
1.7.0 "Onder de Wol", rondes 41-43 (design: MineScapeStory/ontwerp-guhs170-mijnen.md; Java: feature/grotspellen): three cave
minigames, each in its own hall deep under a little entrance hut with a Wolkenliftje down.

  build(h)    the blocks (the Schatgraven field, the puzzle lamps, glimpotjes, glimplanten, six building blocks, deco),
              the coins, the borrowed tools, the five clothes (on existing bones: their textures and icons), the hosts'
              looks, the golden glimworm, models, blockstates, the 26.1 item definitions, loot, tags (the coins in
              guhs:knus/grijptickets, the tools in guhs:loaned, mineable), hidden advancements, texts, and the three
              structures (template + start pool + structure + structure set + biome tag):
                grotspel_schatgraven  the treasure hall (a 15x15 field)       Kapitein Schepsnuit
                grotspel_glimwormen   the dark glimworm hall (glimpotjes)     Juf Glimmie
                grotspel_lichtjes     the mushroom-lamp hall (7x7 lamps of 2) Meester Knipperlicht
              Each template: the hall at the bottom (y 0 = its floor), DIEPTE blocks of the world's own rock, and the hut
              on top (its floor at G = the ground); a Wolkenliftje in both floors, straight above each other.
  check       the halls fit the Java (SchatVeld.GROOTTE, Lichtjespuzzel.CEL, the scan range around the host), both
              Wolkenliftjes have room above them, every guhs: block exists.

On its own (like the slices since 1.5): python tools/features/grotspellen.py from the project root. It writes only its own
files (and adds to the tags), and merges its texts into nl_nl.json / en_us.json. English: tools/lang/en/c194_grotspellen.json.
"""
import json
import math
import os
import random
import re
import shutil
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
if os.path.join(HERE, "..") not in sys.path:
    sys.path.insert(0, os.path.join(HERE, ".."))

DIEPTE = 16          # the world's own rock between the hall's ceiling and the hut
LIFT = (None, 4)     # the hut and both Wolkenliftjes stand at z = 4 of every template (x per game)

# ---------------------------------------------------------------------------------------------------------------------
# small helpers
# ---------------------------------------------------------------------------------------------------------------------


def _clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def _noise(base, seed, amp=8, size=16):
    r = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = r.randint(-amp, amp)
            img.putpixel((x, y), _clamp(c + v for c in base) + (255,))
    return img


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def _grid(rows, pal):
    img = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), tuple(pal[ch]) + (255,))
    return img


def _dot(img, cx, cy, r, col):
    for y in range(img.height):
        for x in range(img.width):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                img.putpixel((x, y), tuple(col) + (255,))


def _img(h, *path):
    return Image.open(os.path.join(h.TEX, *path)).convert("RGBA")


# ---------------------------------------------------------------------------------------------------------------------
# textures
# ---------------------------------------------------------------------------------------------------------------------
GOUD, GOUD_D = (250, 206, 70), (196, 140, 30)
ROZE, ROZE_D = (240, 150, 190), (200, 90, 140)
KAAS, KAAS_GAT = (252, 214, 96), (220, 168, 50)


def textures_blokken(h):
    wol, kiezel = _img(h, "block", "pluiswolblok.png"), _img(h, "block", "viltkiezel.png")
    r = random.Random(1741)
    # the field: closed (a few golden specks of what might be under it), resting (greyer, a rope edge), the pit
    for naam, bron in (("wol", wol), ("kiezel", kiezel)):
        dicht = bron.copy()
        for _ in range(2):
            dicht.putpixel((r.randrange(2, 14), r.randrange(2, 14)), GOUD + (255,))
        h.save(dicht, "block", f"grotspel_schatgrond_{naam}.png")
        rust = bron.copy()
        px = rust.load()
        for y in range(16):
            for x in range(16):
                c = px[x, y]
                px[x, y] = _mix(c[:3], (150, 132, 150), 0.45) + (255,)
        for i in range(16):
            if i % 4 < 2:
                for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
                    px[x, y] = (150, 104, 60, 255)
        h.save(rust, "block", f"grotspel_schatgrond_{naam}_rust.png")
    kuil = _noise((150, 100, 112), 1742, 10)
    p = kuil.load()
    for i in range(16):                                   # a darker rim: it's a hole
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            p[x, y] = (110, 70, 84, 255)
    h.save(kuil, "block", "grotspel_kuil.png")
    schat = kuil.copy()
    for (cx, cy) in ((5, 6), (9, 5), (7, 9), (11, 9), (5, 11), (9, 12)):
        _dot(schat, cx, cy, 2.0, GOUD_D)
        _dot(schat, cx - 0.3, cy - 0.3, 1.3, GOUD)
    h.save(schat, "block", "grotspel_kuil_schat.png")
    goud = kuil.copy()
    for y in range(4, 13):
        for x in range(3, 3 + (y - 3) + 2):
            if x < 14:
                goud.putpixel((x, y), (255, 214, 60, 255) if (x + y) % 5 else (255, 245, 170, 255))
    for (x, y) in ((6, 9), (9, 11), (5, 11)):
        goud.putpixel((x, y), (214, 150, 20, 255))
    h.save(goud, "block", "grotspel_kuil_goud.png")
    piep = kuil.copy()
    for y in range(6, 13):
        for x in range(4, 13):
            piep.putpixel((x, y), KAAS + (255,))
    for (x, y) in ((6, 8), (10, 10), (8, 11)):
        piep.putpixel((x, y), KAAS_GAT + (255,))
    for (x, y) in ((4, 5), (5, 4), (11, 5), (12, 4)):        # little mouse ears: it squeaks!
        piep.putpixel((x, y), (250, 170, 200, 255))
    piep.putpixel((6, 7), (40, 30, 40, 255))
    piep.putpixel((10, 7), (40, 30, 40, 255))
    h.save(piep, "block", "grotspel_kuil_piep.png")

    # the mushroom lamps (puzzle + deco tile): on = a glowing red-pink cap with white dots, off = dark
    for staat, cap, stip in (("aan", (246, 92, 120), (255, 250, 236)), ("uit", (96, 40, 56), (120, 104, 110))):
        img = _noise(cap, 1743, 6)
        for (cx, cy, rr) in ((4, 4, 1.6), (11, 5, 1.3), (7, 10, 1.8), (13, 12, 1.2), (3, 12, 1.1), (9, 2, 0.9)):
            _dot(img, cx, cy, rr, stip)
        if staat == "aan":
            px = img.load()
            for y in range(16):
                for x in range(16):
                    d = math.hypot(x - 7.5, y - 7.5) / 10.6
                    px[x, y] = _clamp(c + 30 * (1 - d) for c in px[x, y][:3]) + (255,)
        h.save(img, "block", f"lichtjes_lamp_{staat}.png")
    rand = _noise((236, 220, 196), 1744, 6)
    for x in range(16):
        rand.putpixel((x, 0), (200, 170, 140, 255))
        rand.putpixel((x, 15), (200, 170, 140, 255))
    h.save(rand, "block", "lichtjes_lamp_rand.png")

    # glimpotjes: glass with glimworms, darker to brighter (0-4), a cork lid
    for n in range(5):
        t = n / 4
        img = Image.new("RGBA", (16, 16))
        rr = random.Random(1745 + n)
        bg = _mix((34, 52, 66), (150, 220, 140), t)
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y), _clamp(c + rr.randint(-5, 5) for c in bg) + (255,))
        for x in range(16):                                 # the glass edges
            img.putpixel((x, 0), _mix((200, 230, 240), (255, 255, 255), t) + (255,))
            img.putpixel((0, x), _mix((150, 190, 210), (230, 255, 230), t) + (255,))
            img.putpixel((15, x), _mix((150, 190, 210), (230, 255, 230), t) + (255,))
        for k in range(2 + n * 2):                          # the glimworms in it
            x, y = rr.randrange(2, 14), rr.randrange(3, 14)
            col = _mix((140, 200, 90), (255, 255, 170), t)
            img.putpixel((x, y), col + (255,))
            img.putpixel((min(15, x + 1), y), _mix(col, bg, 0.5) + (255,))
        h.save(img, "block", f"glimpotje_{n}.png")
    deksel = _noise((176, 130, 90), 1750, 8)
    h.save(deksel, "block", "glimpotje_deksel.png")
    # the glimplant: a cross plant, teal leaves with glowing dots
    plant = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rr = random.Random(1751)
    for k in range(7):
        x0 = 1 + k * 2
        top = rr.randrange(2, 7)
        for y in range(top, 16):
            x = int(x0 + math.sin(y * 0.6 + k) * 0.8)
            if 0 <= x < 16:
                plant.putpixel((x, y), _clamp(c + rr.randint(-12, 12) for c in (60, 170, 160)) + (255,))
        plant.putpixel((max(0, min(15, x0)), top), (220, 255, 170, 255))
    for _ in range(6):
        plant.putpixel((rr.randrange(1, 15), rr.randrange(4, 14)), (200, 255, 150, 255))
    h.save(plant, "block", "glimplant.png")

    # building blocks
    zand = _noise((236, 176, 176), 1752, 7)
    p = zand.load()
    for y in (4, 10):
        for x in range(16):
            p[x, y] = _clamp(c - 22 for c in p[x, y][:3]) + (255,)
    for _ in range(6):
        p[rr.randrange(16), rr.randrange(16)] = GOUD + (255,)
    h.save(zand, "block", "schatzandsteen.png")
    plank = _noise((226, 140, 160), 1753, 6)
    p = plank.load()
    for y in (0, 4, 8, 12):
        for x in range(16):
            p[x, y] = (176, 90, 116, 255)
    for (x, y) in ((2, 2), (13, 2), (7, 6), (2, 10), (13, 10), (8, 14)):
        p[x, y] = GOUD + (255,)
    h.save(plank, "block", "schatkistplanken.png")
    vilt = _noise((44, 50, 110), 1754, 6)
    for _ in range(5):
        vilt.putpixel((rr.randrange(16), rr.randrange(16)), (255, 246, 190, 255))
    h.save(vilt, "block", "nachtvilt.png")
    mos = _noise((70, 150, 120), 1755, 12)
    for _ in range(7):
        mos.putpixel((rr.randrange(16), rr.randrange(16)), (210, 255, 160, 255))
    h.save(mos, "block", "glimmos.png")
    pad = _noise((222, 74, 96), 1756, 7)
    for (cx, cy, r_) in ((4, 4, 1.8), (12, 6, 1.5), (7, 11, 2.0), (14, 14, 1.0), (1, 13, 1.0)):
        _dot(pad, cx, cy, r_, (252, 246, 236))
    h.save(pad, "block", "paddenstoelsteen.png")
    steel = _noise((238, 226, 204), 1757, 5)
    p = steel.load()
    for y in (7, 15):
        for x in range(16):
            p[x, y] = (196, 176, 150, 255)
    for y in range(16):
        p[0 if y < 8 else 8, y] = (196, 176, 150, 255)
    h.save(steel, "block", "steeltjessteen.png")
    # deco: the treasure chest (pink wood, golden bands), the golden cheese
    kist = plank.copy()
    p = kist.load()
    for y in range(16):
        for x in (3, 12):
            p[x, y] = GOUD_D + (255,)
    h.save(kist, "block", "schatkistje.png")
    kaas = _noise((255, 210, 70), 1758, 8)
    for (cx, cy, r_) in ((4, 5, 1.6), (11, 4, 1.2), (8, 11, 1.8), (13, 12, 1.0)):
        _dot(kaas, cx, cy, r_, (214, 156, 30))
    h.save(kaas, "block", "gouden_kaasje.png")


def textures_items(h):
    rows_munt = ["................", ".....aaaaaa.....", "...aabbbbbbaa...", "..abbccccccbba..", "..abccddddccba..",
                 ".abccdeeeedccba.", ".abcdeeffeedcba.", ".abcdeffffedcba.", ".abcdeffffedcba.", ".abcdeeffeedcba.",
                 ".abccdeeeedccba.", "..abccddddccba..", "..abbccccccbba..", "...aabbbbbbaa...", ".....aaaaaa.....",
                 "................"]
    munten = {
        "schatmuntje": {"a": (150, 100, 20), "b": GOUD_D, "c": GOUD, "d": (255, 236, 140), "e": GOUD, "f": ROZE_D},
        "glimmertje": {"a": (40, 90, 70), "b": (90, 180, 120), "c": (160, 230, 150), "d": (220, 255, 190), "e": (160, 230, 150), "f": (255, 255, 150)},
        "lampjesmunt": {"a": (120, 30, 50), "b": (220, 70, 100), "c": (246, 110, 140), "d": (255, 250, 236), "e": (246, 110, 140), "f": (255, 250, 236)},
    }
    for naam, pal in munten.items():
        h.save(_grid(rows_munt, pal), "item", f"{naam}.png")
    schep = ["................", "..........aa....", ".........abba...", "........abbba...", ".......abbba....",
             "......abbba.....", ".....cbba.......", "....cc..........", "...cc...........", "..cc............",
             ".cc.............", "cc..............", "................", "................", "................", "................"]
    h.save(_grid(schep, {"a": ROZE_D, "b": ROZE, "c": (150, 100, 60)}), "item", "schatschepje.png")
    net = ["................", "....aaaaaa......", "...abbbbbba.....", "..abwbwbwbba....", "..abbwbwbwba....",
           "..abwbwbwbba....", "..abbwbwbwba....", "...abbbbbba.....", "....aaaaaac.....", "..........c.....",
           "...........c....", "............c...", ".............c..", "..............c.", "................", "................"]
    h.save(_grid(net, {"a": ROZE_D, "b": (230, 246, 240), "w": (180, 210, 200), "c": (150, 100, 60)}), "item", "glimnetje.png")
    # the clothes' icons
    hoed = ["................", "................", "................", "......aaaa......", "....aabbbbaa....",
            "...abbbbbbbba...", "...abbbwwbbba...", "..abbbbbbbbbba..", "..apppppppppppa.", ".aaaaaaaaaaaaaaa",
            "aabbbbbbbbbbbbba", ".aaaaaaaaaaaaaa.", "................", "................", "................", "................"]
    h.save(_grid(hoed, {"a": (20, 18, 24), "b": (52, 46, 58), "w": (250, 250, 250), "p": ROZE}), "item", "schat_piratenhoedje.png")
    klep = ["................", "................", "aa..............", ".aa.............", "..aa..bbbbb.....",
            "...aabbbbbbb....", "....bbbbppbbb...", "....bbbppppbb...", "....bbbbppbbb...", ".....bbbbbbbaa..",
            "......bbbbb..aa.", ".............aa.", "..............aa", "................", "................", "................"]
    h.save(_grid(klep, {"a": (90, 60, 50), "b": (24, 22, 28), "p": ROZE}), "item", "schat_oogklepje.png")
    ant = ["................", "..gg........gg..", ".gyyg......gyyg.", ".gyyg......gyyg.", "..gg........gg..",
           "...s........s...", "...s........s...", "....s......s....", "....s......s....", ".....s....s.....",
           ".....s....s.....", "......s..s......", "......pppp......", ".....pppppp.....", "................", "................"]
    h.save(_grid(ant, {"g": (150, 230, 120), "y": (255, 255, 170), "s": (110, 170, 90), "p": ROZE}), "item", "glim_antennetjes.png")
    h.save(_grid(net, {"a": (120, 200, 170), "b": (240, 250, 246), "w": (190, 220, 210), "c": (150, 100, 60)}), "item", "glim_vlindernetje.png")
    pad = ["................", "................", ".....aaaaaa.....", "...aabbwbbbaa...", "..abbbbbbbwbba..",
           ".abwbbbbbbbbbba.", ".abbbbbwwbbbbba.", "abbbbbbwwbbbwbba", "abbwbbbbbbbbbbba", "acccccccccccccca",
           ".cdcdcdcdcdcdcd.", "................", "................", "................", "................", "................"]
    h.save(_grid(pad, {"a": (150, 40, 60), "b": (246, 92, 120), "w": (255, 250, 236), "c": (238, 226, 204), "d": (210, 190, 166)}),
           "item", "lichtjes_paddenstoelhoedje.png")


def textures_wezens(h):
    """The hosts (a tinted copy of an existing NPC skin each), Snuffie, and the golden glimworm."""
    ent = os.path.join(h.TEX, "entity")

    def tint(bron, doel, kleur, sterkte):
        img = Image.open(os.path.join(ent, bron)).convert("RGBA")
        px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                c = px[x, y]
                if c[3]:
                    px[x, y] = _mix(c[:3], kleur, sterkte) + (c[3],)
        img.save(os.path.join(ent, *doel.split("/")))

    tint("npc_kapiteinguh.png", "npc_schatgraafguh.png", (240, 150, 190), 0.12)
    tint("npc_sterrenkijkerguh.png", "npc_glimwormguh.png", (150, 230, 160), 0.18)
    tint("npc_mijnguh.png", "npc_lampjesguh.png", (246, 120, 140), 0.15)
    tint("npc_tipguh.png", "npc_snuffelhulpje.png", (250, 200, 150), 0.15)
    os.makedirs(os.path.join(ent, "grotspellen"), exist_ok=True)
    for bron, doel in (("glimworm_baby.png", "glimworm_goud.png"), ("glimworm_baby_glowmask.png", "glimworm_goud_glowmask.png")):
        img = Image.open(os.path.join(ent, "safari", bron)).convert("RGBA")
        px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                c = px[x, y]
                if c[3]:
                    l = (c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11) / 255
                    px[x, y] = _mix((150, 96, 10), (255, 236, 120), l) + (c[3],) if c[:3] != (0, 0, 0) else c
        img.save(os.path.join(ent, "grotspellen", doel))


# --- the clothes, on existing bones: their swatches are read from guh.geo.json ---------------------------------------
KLEDING = {   # id: [(bone, painter)]
    "schat_piratenhoedje": [("outfit_tricorn", "piraat")],
    "schat_oogklepje": [("outfit_eyepatch", "klep")],
    "glim_antennetjes": [("outfit_oren_sponsguh_links", "antenne")],
    "glim_vlindernetje": [("outfit_doolhof_rugzakje", "net"), ("outfit_doolhof_rugzakje_rol", "netrand"), ("outfit_circuit_vlagcape_stok", "stok")],
    "lichtjes_paddenstoelhoedje": [("outfit_theemutsje", "hoed"), ("outfit_theemutsje_rand", "lamellen"), ("outfit_theemutsje_pom", "stip")],
}


def _swatch(geo, bone):
    for b in geo["minecraft:geometry"][0]["bones"]:
        if b["name"] == bone:
            for c in b.get("cubes", []):
                for f in c["uv"].values():
                    return tuple(f["uv"])
    raise KeyError(bone)


def _verf(soort, rng):
    """A 32x32 swatch (the guh texture is 4x its UV units)."""
    img = Image.new("RGBA", (32, 32))
    p = img.load()

    def ruis(base, amp=6):
        for y in range(32):
            for x in range(32):
                p[x, y] = _clamp(c + rng.randint(-amp, amp) for c in base) + (255,)
    if soort == "piraat":
        ruis((36, 32, 42))
        for y in range(22, 27):
            for x in range(32):
                p[x, y] = ROZE + (255,)
        for (x, y) in ((14, 9), (17, 9), (15, 10), (16, 10), (15, 12), (16, 12)):   # a tiny guh face
            p[x, y] = (250, 250, 250, 255)
    elif soort == "klep":
        ruis((22, 20, 26), 3)
        for (x, y) in [(13, 13), (14, 13), (17, 13), (18, 13)] + [(x, 14) for x in range(12, 20)] + [(x, 15) for x in range(12, 20)] + \
                [(x, 16) for x in range(13, 19)] + [(x, 17) for x in range(14, 18)] + [(15, 18), (16, 18)]:
            p[x, y] = ROZE + (255,)
    elif soort == "antenne":
        for y in range(32):
            t = 1 - y / 31
            col = _mix((110, 180, 90), (255, 255, 160), t ** 1.5)
            for x in range(32):
                p[x, y] = _clamp(c + rng.randint(-4, 4) for c in col) + (255,)
    elif soort == "net":
        ruis((236, 248, 242), 3)
        for i in range(0, 32, 5):
            for j in range(32):
                p[i, j] = (170, 204, 192, 255)
                p[j, i] = (170, 204, 192, 255)
    elif soort == "netrand":
        ruis(ROZE, 6)
    elif soort == "stok":
        ruis((150, 100, 60), 8)
    elif soort == "hoed":
        ruis((240, 86, 112), 6)
        for (cx, cy, r_) in ((7, 7, 3.0), (22, 9, 2.4), (13, 20, 3.4), (26, 24, 2.0), (4, 25, 2.0)):
            _dot(img, cx, cy, r_, (255, 250, 236))
    elif soort == "lamellen":
        ruis((238, 222, 196), 4)
        for x in range(0, 32, 3):
            for y in range(32):
                p[x, y] = (206, 184, 156, 255)
    elif soort == "stip":
        ruis((255, 250, 236), 3)
    return img


def textures_kleding(h):
    geo = json.load(open(os.path.join(h.A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))
    rng = random.Random(1760)
    for naam, delen in KLEDING.items():
        img = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
        for bone, soort in delen:
            u, v = _swatch(geo, bone)
            img.paste(_verf(soort, rng), (u * 4, v * 4))
        h.save(img, "entity", "guh_clothes", f"{naam}.png")


# ---------------------------------------------------------------------------------------------------------------------
# models, blockstates, items, loot, tags
# ---------------------------------------------------------------------------------------------------------------------
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
BOUW = ["schatzandsteen", "schatkistplanken", "nachtvilt", "glimmos", "paddenstoelsteen", "steeltjessteen"]
DECO = ["schatkistje", "gouden_kaasje", "glimpotje", "glimplant", "paddenstoellampje", "paddenstoelkrukje", "lampjestegel"]
MUNTEN = ["schatmuntje", "glimmertje", "lampjesmunt"]
LEEN = ["schatschepje", "glimnetje", "glimpotje_rug"]
STATEN = ["dicht", "rust", "leeg", "schat", "goud", "piep"]


def _box(frm, to, tex, uv=None):
    out = {"from": frm, "to": to, "faces": {}}
    for f in ("north", "south", "east", "west", "up", "down"):
        face = {"texture": tex}
        if uv:
            face["uv"] = uv
        out["faces"][f] = face
    return out


def models(h):
    A, D, w = h.A, h.D, h.w

    def item_block(name, model=None):
        w(f"{A}/models/item/{name}.json", {"parent": model or f"guhs:block/{name}"})
        w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{name}"}})

    def item_flat(name, tex=None):
        w(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": tex or f"guhs:item/{name}"}})
        w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{name}"}})

    def drop(name):
        w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"guhs:{name}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    # the Schatgraven field: closed / resting = a full block; open = a pit half a block deep showing what was under it
    variants = {}
    for mat in ("wol", "kiezel"):
        side = f"guhs:block/grotspel_schatgrond_{mat}"
        w(f"{A}/models/block/grotspel_schatgrond_dicht_{mat}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": side}})
        w(f"{A}/models/block/grotspel_schatgrond_rust_{mat}.json", {"parent": "minecraft:block/cube_all",
                                                                     "textures": {"all": f"guhs:block/grotspel_schatgrond_{mat}_rust"}})
        for staat in ("leeg", "schat", "goud", "piep"):
            top = "guhs:block/grotspel_kuil" + ("" if staat == "leeg" else f"_{staat}")
            w(f"{A}/models/block/grotspel_schatgrond_{staat}_{mat}.json", {"parent": "minecraft:block/block",
                "textures": {"particle": side, "side": side, "top": top},
                "elements": [{"from": [0, 0, 0], "to": [16, 8, 16], "faces": {
                    "down": {"texture": "#side", "cullface": "down"}, "up": {"texture": "#top"},
                    "north": {"texture": "#side", "uv": [0, 8, 16, 16], "cullface": "north"},
                    "south": {"texture": "#side", "uv": [0, 8, 16, 16], "cullface": "south"},
                    "east": {"texture": "#side", "uv": [0, 8, 16, 16], "cullface": "east"},
                    "west": {"texture": "#side", "uv": [0, 8, 16, 16], "cullface": "west"}}}]})
        for staat in STATEN:
            variants[f"kiezel={'true' if mat == 'kiezel' else 'false'},staat={staat}"] = {"model": f"guhs:block/grotspel_schatgrond_{staat}_{mat}"}
    w(f"{A}/blockstates/grotspel_schatgrond.json", {"variants": variants})
    # the puzzle lamps and the deco tile
    for staat in ("aan", "uit"):
        w(f"{A}/models/block/lichtjes_lamp_{staat}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"guhs:block/lichtjes_lamp_{staat}", "side": "guhs:block/lichtjes_lamp_rand", "bottom": "guhs:block/lichtjes_lamp_rand"}})
    for blok in ("grotspel_puzzellamp", "lampjestegel"):
        w(f"{A}/blockstates/{blok}.json", {"variants": {"aan=true": {"model": "guhs:block/lichtjes_lamp_aan"},
                                                         "aan=false": {"model": "guhs:block/lichtjes_lamp_uit"}}})
    item_block("lampjestegel", "guhs:block/lichtjes_lamp_aan")
    drop("lampjestegel")
    # glimpotjes (0-4)
    for n in range(5):
        w(f"{A}/models/block/glimpotje_{n}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"particle": f"guhs:block/glimpotje_{n}", "glas": f"guhs:block/glimpotje_{n}", "deksel": "guhs:block/glimpotje_deksel"},
            "elements": [_box([4, 0, 4], [12, 10, 12], "#glas"), _box([5, 10, 5], [11, 12, 11], "#deksel")]})
    w(f"{A}/blockstates/glimpotje.json", {"variants": {f"licht={n}": {"model": f"guhs:block/glimpotje_{n}"} for n in range(5)}})
    item_block("glimpotje", "guhs:block/glimpotje_4")
    drop("glimpotje")
    # the jar on your back (borrowed): its model per stage (custom model data 0-4)
    w(f"{A}/items/glimpotje_rug.json", {"model": {"type": "minecraft:range_dispatch", "property": "minecraft:custom_model_data", "index": 0,
        "entries": [{"threshold": float(n), "model": {"type": "minecraft:model", "model": f"guhs:block/glimpotje_{n}"}} for n in range(1, 5)],
        "fallback": {"type": "minecraft:model", "model": "guhs:block/glimpotje_0"}}})
    # the glimplant
    w(f"{A}/models/block/glimplant.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": "guhs:block/glimplant"}})
    w(f"{A}/blockstates/glimplant.json", {"variants": {"": {"model": "guhs:block/glimplant"}}})
    item_flat("glimplant", "guhs:block/glimplant")
    drop("glimplant")
    # building blocks
    for naam in BOUW:
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/{naam}"}})
        w(f"{A}/blockstates/{naam}.json", {"variants": {"": {"model": f"guhs:block/{naam}"}}})
        item_block(naam)
        drop(naam)
    # deco with a facing
    furn = {
        "schatkistje": ({"hout": "guhs:block/schatkistje", "goud": "minecraft:block/gold_block"},
                        [_box([1, 0, 3], [15, 9, 13], "#hout"), _box([1, 9, 3], [15, 12, 13], "#hout"), _box([7, 6, 2], [9, 9, 3], "#goud")]),
        "gouden_kaasje": ({"kaas": "guhs:block/gouden_kaasje"},
                          [_box([3, 0, 3], [13, 3, 13], "#kaas"), _box([3, 3, 3], [13, 5, 10], "#kaas"), _box([3, 5, 3], [13, 7, 7], "#kaas")]),
        "paddenstoelkrukje": ({"hoed": "guhs:block/paddenstoelsteen", "steel": "guhs:block/steeltjessteen"},
                              [_box([5, 0, 5], [11, 6, 11], "#steel"), _box([2, 6, 2], [14, 9, 14], "#hoed")]),
    }
    for naam, (tex, els) in furn.items():
        t = dict(tex)
        t["particle"] = list(tex.values())[0]
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "textures": t, "elements": els})
        w(f"{A}/blockstates/{naam}.json", {"variants": {f"facing={f}": ({"model": f"guhs:block/{naam}", "y": r} if r else
                                                                         {"model": f"guhs:block/{naam}"}) for f, r in ROT.items()}})
        item_block(naam)
        drop(naam)
    w(f"{A}/models/block/paddenstoellampje.json", {"parent": "minecraft:block/block",
        "textures": {"particle": "guhs:block/lichtjes_lamp_aan", "hoed": "guhs:block/lichtjes_lamp_aan", "steel": "guhs:block/steeltjessteen"},
        "elements": [_box([6, 0, 6], [10, 6, 10], "#steel"), _box([3, 6, 3], [13, 10, 13], "#hoed"), _box([5, 10, 5], [11, 11, 11], "#hoed")]})
    w(f"{A}/blockstates/paddenstoellampje.json", {"variants": {"": {"model": "guhs:block/paddenstoellampje"}}})
    item_block("paddenstoellampje")
    drop("paddenstoellampje")
    # coins, tools, clothes
    for naam in MUNTEN + ["schatschepje", "glimnetje"] + list(KLEDING):
        item_flat(naam)
    # tags
    h.add_tag("guhs/tags/item/knus/grijptickets", [f"guhs:{m}" for m in MUNTEN])
    h.add_tag("guhs/tags/item/loaned", [f"guhs:{m}" for m in LEEN])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:schatzandsteen", "guhs:gouden_kaasje"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:schatkistplanken", "guhs:schatkistje", "guhs:paddenstoelsteen", "guhs:steeltjessteen",
                                                    "guhs:paddenstoelkrukje", "guhs:paddenstoellampje"])
    h.add_tag("minecraft/tags/block/mineable/hoe", ["guhs:glimmos", "guhs:glimplant"])
    h.add_tag("minecraft/tags/block/wool", ["guhs:nachtvilt"])
    # hidden advancements (given by the Java, for FTB)
    for spel in ("schatgraven", "glimwormvangen", "lichtjespuzzel"):
        for wat in ("gespeeld", "kleding"):
            w(f"{D}/advancement/quest/{spel}_{wat}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


# ---------------------------------------------------------------------------------------------------------------------
# the halls
# ---------------------------------------------------------------------------------------------------------------------
SPELEN = {   # id: (structure, size x, z, hall height H, lift x, host (x, z), host kind, salt)
    "schatgraven": ("grotspel_schatgraven", 23, 25, 7, 11, (14, 4), "schatgraafguh", 20261741),
    "glimwormvangen": ("grotspel_glimwormen", 27, 27, 9, 13, (16, 4), "glimwormguh", 20261742),
    "lichtjespuzzel": ("grotspel_lichtjes", 24, 26, 8, 11, (14, 4), "lampjesguh", 20261743),
}
VELD = (4, 8, 15)            # Schatgraven: the field's low corner (x, z) and size
RASTER = (5, 8, 7, 2)        # Lichtjespuzzel: the raster's low corner (x, z), lamps, blocks per lamp
BIOMES = ["guhs:guh_fields", "guhs:knabbel_crumbs", "guhs:pink_puffs", "guhs:kaas_flats", "guhs:guh_meadows", "guhs:bloemenvallei"]


def _g(H):
    """The template y of the ground (the hut's floor) for a hall of height H."""
    return H + 2 + DIEPTE


def sign_nbt(lines):
    from make_structures import NbtList, Byte
    msgs = [json.dumps(l) if l else json.dumps("") for l in (list(lines) + [""] * 4)[:4]]
    empty = [json.dumps("")] * 4
    return {"id": "minecraft:sign",
            "front_text": {"messages": NbtList(8, msgs), "color": "black", "has_glowing_text": Byte(0)},
            "back_text": {"messages": NbtList(8, empty), "color": "black", "has_glowing_text": Byte(0)},
            "is_waxed": Byte(1)}


SIGNS = {   # (game, where): [(Dutch, English)] (short lines)
    ("schatgraven", "hut"): [("Schatgraven!", "Treasure Dig!"), ("Spring op het", "Jump on the"), ("wolkje: naar", "cloud: down to"), ("de schatzaal", "the hall")],
    ("schatgraven", "zaal"): [("Kapitein", "Captain"), ("Schepsnuit", "Shovelsnout"), ("Graaf mee,", "Dig along,"), ("guhtje!", "guhtje!")],
    ("glimwormvangen", "hut"): [("Glimworm-", "Glowworm"), ("vangen!", "Catching!"), ("Wolkje springen", "Jump the cloud,"), ("= naar beneden", "= go down")],
    ("glimwormvangen", "zaal"): [("Ssst!", "Shhh!"), ("De glimwormpjes", "The glowworms"), ("slapen bijna.", "are nearly"), ("Bijna.", "asleep. Nearly.")],
    ("lichtjespuzzel", "hut"): [("Lichtjes-", "Little Lights"), ("puzzel!", "Puzzle!"), ("Spring op het", "Jump on the"), ("wolkje", "cloud")],
    ("lichtjespuzzel", "zaal"): [("Alle lampjes", "All lamps on"), ("aan = Meester", "= Master Blinky"), ("Knipperlicht", "can read his"), ("leest weer!", "paper again!")],
    ("alle", "terug"): [("Terug naar", "Back up?"), ("boven?", ""), ("Spring op het", "Jump on the"), ("wolkje!", "cloud!")],
}


def _lijn(key, nl, en, texts):
    texts[key] = (en, nl)
    return {"translate": key, "fallback": nl}


def _sign_lines(spel, waar, texts):
    return [_lijn(f"sign.guhs.grotspel.{spel}_{waar}_{i}", nl, en, texts) if nl or en else "" for i, (nl, en) in enumerate(SIGNS[(spel, waar)])]


def _hutje(S, mc, lx, lz, G, spel, texts):
    """The entrance hut: 7x7 of cherry wood with a pink striped roof, a door on the north, the Wolkenliftje in the middle."""
    for x in range(lx - 3, lx + 4):
        for z in range(lz - 3, lz + 4):
            S.set(x, G, z, mc("cherry_planks"))
            for y in range(G + 1, G + 4):
                rand = x in (lx - 3, lx + 3) or z in (lz - 3, lz + 3)
                hoek = x in (lx - 3, lx + 3) and z in (lz - 3, lz + 3)
                if hoek:
                    S.set(x, y, z, mc("stripped_cherry_log"), {"axis": "y"})
                elif rand:
                    raam = y == G + 2 and (x in (lx - 1, lx + 1) or z in (lz - 1, lz + 1))
                    S.set(x, y, z, mc("pink_stained_glass") if raam else mc("cherry_planks"))
                else:
                    S.set(x, y, z, mc("air"))
    for x in range(lx - 3, lx + 4):                     # a ceiling of birch planks
        for z in range(lz - 3, lz + 4):
            S.set(x, G + 4, z, mc("birch_planks") if x not in (lx - 3, lx + 3) and z not in (lz - 3, lz + 3) else mc("stripped_cherry_log"),
                  None if x not in (lx - 3, lx + 3) and z not in (lz - 3, lz + 3) else {"axis": "y"})
    for k, y in enumerate(range(G + 5, G + 9)):          # the roof: a pyramid in pink and white stripes, a lantern on top
        r = 4 - k
        for x in range(lx - r, lx + r + 1):
            for z in range(lz - r, lz + r + 1):
                if max(abs(x - lx), abs(z - lz)) == r or k == 3:
                    S.set(x, y, z, mc("pink_wool") if (x + z) % 2 == 0 else mc("white_wool"))
    S.set(lx, G + 9, lz, mc("lantern"), {"hanging": "false", "waterlogged": "false"})
    # the door (north) and a step
    S.set(lx, G + 1, lz - 3, mc("cherry_door"), {"facing": "south", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    S.set(lx, G + 2, lz - 3, mc("cherry_door"), {"facing": "south", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    S.set(lx - 2, G + 1, lz - 4, mc("cherry_fence"), {"east": "false", "north": "false", "south": "false", "west": "false", "waterlogged": "false"})
    S.set(lx - 2, G + 2, lz - 4, mc("lantern"), {"hanging": "false", "waterlogged": "false"})
    # inside: the Wolkenliftje, a hanging lamp, the sign by the door (outside)
    S.set(lx, G, lz, "guhs:wolkenliftje")
    S.set(lx, G + 3, lz + 2, mc("lantern"), {"hanging": "true", "waterlogged": "false"})
    S.set(lx + 2, G + 1, lz - 4, mc("cherry_sign"), {"rotation": "8", "waterlogged": "false"}, sign_nbt(_sign_lines(spel, "hut", texts)))
    S.set(lx + 2, G + 1, lz + 2, "guhs:gouden_kaasje" if spel == "schatgraven" else "guhs:glimpotje" if spel == "glimwormvangen"
          else "guhs:paddenstoellampje", {"facing": "south"} if spel == "schatgraven" else {"licht": "4"} if spel == "glimwormvangen" else None)
    # the jigsaw the structure is centred on (in the hut's floor)
    S.set(lx - 2, G, lz - 2, mc("jigsaw"), {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": f"guhs:{SPELEN[spel][0]}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "joint": "rollable", "final_state": "minecraft:cherry_planks", "selection_priority": 0, "placement_priority": 0})


def _zaal(S, mc, X, Z, H, muur, vloer, plafond, rng, muur2=None):
    """The hall's shell: floor (y 0), walls, ceiling (y H+1), air inside."""
    for x in range(X):
        for z in range(Z):
            buiten = x in (0, X - 1) or z in (0, Z - 1)
            S.set(x, 0, z, muur if buiten else vloer(x, z))
            S.set(x, H + 1, z, muur if buiten else plafond(x, z))
            for y in range(1, H + 1):
                if buiten:
                    S.set(x, y, z, muur2 if muur2 and (y + x + z) % 5 == 0 else muur)
                else:
                    S.set(x, y, z, mc("air"))


def _host(S, ms, spel, texts, mc):
    _s, X, Z, H, lx, (nx, nz), kind, _salt = SPELEN[spel]
    S.entity(nx + 0.5, 1.0, nz + 0.5, {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(0.0, 0.0)})
    S.set(nx + 2, 1, nz - 2, mc("cherry_sign"), {"rotation": "0", "waterlogged": "false"}, sign_nbt(_sign_lines(spel, "zaal", texts)))
    S.set(lx, 0, LIFT[1], "guhs:wolkenliftje")
    S.set(lx - 1, 1, LIFT[1] - 2, mc("cherry_sign"), {"rotation": "0", "waterlogged": "false"}, sign_nbt(_sign_lines("alle", "terug", texts)))


def zaal_schatgraven(h, texts):
    import make_structures as ms
    mc = ms.mc
    spel = "schatgraven"
    _s, X, Z, H, lx, _n, _k, _salt = SPELEN[spel]
    G = _g(H)
    S = ms.Structure((X, G + 10, Z))
    rng = random.Random(1770)
    _zaal(S, mc, X, Z, H, "guhs:viltsteen", lambda x, z: "guhs:viltsteen_tegels",
          lambda x, z: "guhs:guh_kristal_lamp" if x % 6 == 3 and z % 6 == 3 else "guhs:viltsteen", rng, muur2="guhs:pluiswolblok")
    vx, vz, n = VELD
    # the field: pluiswol with clusters of viltkiezel
    kiezels = [(rng.randrange(n), rng.randrange(n)) for _ in range(6)]
    for x in range(n):
        for z in range(n):
            kiezel = any(math.hypot(x - kx, z - kz) < 2.2 for kx, kz in kiezels)
            S.set(vx + x, 0, vz + z, "guhs:grotspel_schatgrond", {"kiezel": "true" if kiezel else "false", "staat": "dicht"})
    # a rope edge of schatkistplanken round the field, with gaps
    for i in range(-1, n + 1):
        for (x, z) in ((vx + i, vz - 1), (vx + i, vz + n), (vx - 1, vz + i), (vx + n, vz + i)):
            if 0 < x < X - 1 and 0 < z < Z - 1 and (i % 4 != 1):
                S.set(x, 0, z, "guhs:schatkistplanken")
    # deco: treasure chests and golden cheeses in the corners, lanterns on the walls
    for (x, z, f) in ((1, Z - 2, "north"), (X - 2, Z - 2, "north"), (1, 6, "east"), (X - 2, 6, "west")):
        S.set(x, 1, z, "guhs:schatkistje", {"facing": f})
    for (x, z) in ((2, Z - 2), (X - 3, Z - 2)):
        S.set(x, 1, z, mc("barrel"), {"facing": "up", "open": "false"})
        S.set(x, 2, z, "guhs:gouden_kaasje", {"facing": "south"})
    for z in range(3, Z - 1, 5):
        for x in (0, X - 1):
            S.set(x, 3, z, "guhs:guh_kristal_lamp")
    _host(S, ms, spel, texts, mc)
    _hutje(S, mc, lx, LIFT[1], G, spel, texts)
    return S, G


def zaal_glimwormen(h, texts):
    import make_structures as ms
    mc = ms.mc
    spel = "glimwormvangen"
    _s, X, Z, H, lx, _n, _k, _salt = SPELEN[spel]
    G = _g(H)
    S = ms.Structure((X, G + 10, Z))
    rng = random.Random(1771)
    sterren = {(rng.randrange(2, X - 2), rng.randrange(2, Z - 2)) for _ in range(14)}
    _zaal(S, mc, X, Z, H, "guhs:knuffelsteen", lambda x, z: "guhs:nachtvilt" if (x * 7 + z * 3) % 11 else "guhs:viltsteen",
          lambda x, z: "guhs:glimmos" if (x, z) in sterren else "guhs:nachtvilt", rng, muur2="guhs:nachtvilt")
    # glimpotjes on little posts along the walls (they also mark the hall for the game)
    for x in range(1, X - 1):
        for z in range(1, Z - 1):
            rand = x in (1, X - 2) or z in (1, Z - 2)
            if rand and ((x + z) % 4 == 0) and not (abs(x - lx) <= 1 and z <= 6):
                for y in (1, 2):
                    S.set(x, y, z, mc("cherry_fence"), {"east": "false", "north": "false", "south": "false", "west": "false", "waterlogged": "false"})
                S.set(x, 3, z, "guhs:glimpotje", {"licht": "1"})
    # glimplanten all over the floor (the hiders' spots)
    geplant = 0
    while geplant < 16:
        x, z = rng.randrange(3, X - 3), rng.randrange(8, Z - 3)
        if S.get(x, 1, z) == "minecraft:air":
            S.set(x, 1, z, "guhs:glimplant")
            geplant += 1
    _host(S, ms, spel, texts, mc)
    _hutje(S, mc, lx, LIFT[1], G, spel, texts)
    return S, G


def zaal_lichtjes(h, texts):
    import make_structures as ms
    mc = ms.mc
    spel = "lichtjespuzzel"
    _s, X, Z, H, lx, _n, _k, _salt = SPELEN[spel]
    G = _g(H)
    S = ms.Structure((X, G + 10, Z))
    rng = random.Random(1772)
    _zaal(S, mc, X, Z, H, "guhs:steeltjessteen", lambda x, z: "guhs:viltsteen_tegels",
          lambda x, z: "guhs:paddenstoelsteen" if (x // 3 + z // 3) % 3 == 0 else "guhs:steeltjessteen", rng, muur2="guhs:paddenstoelsteen")
    rx, rz, n, cel = RASTER
    for x in range(n * cel):
        for z in range(n * cel):
            S.set(rx + x, 0, rz + z, "guhs:grotspel_puzzellamp", {"aan": "true"})
    # mushroom lamps and stools along the walls
    for z in range(3, Z - 1, 4):
        for x in (1, X - 2):
            S.set(x, 1, z, "guhs:paddenstoellampje")
    for (x, z) in ((3, Z - 2), (X - 4, Z - 2), (3, 2), (X - 4, 2)):
        S.set(x, 1, z, "guhs:paddenstoelkrukje", {"facing": "south"})
    _host(S, ms, spel, texts, mc)
    _hutje(S, mc, lx, LIFT[1], G, spel, texts)
    return S, G


def structure_files(h, spel, G):
    struct, X, Z, H, lx, _n, _k, salt = SPELEN[spel]
    D = h.D
    tag = f"#guhs:has_structure/{struct}"
    spawn = {"monster": {"bounding_box": "piece", "spawns": []}, "ambient": {"bounding_box": "piece", "spawns": []}}
    h.w(f"{D}/worldgen/structure/{struct}.json", {
        "type": "guhs:flat_jigsaw", "biomes": tag, "step": "fluid_springs", "spawn_overrides": spawn, "terrain_adaptation": "beard_box",
        "check_radius": 16, "max_height_difference": 10, "keep_clear": 24,
        "jigsaw": {"type": "minecraft:jigsaw", "biomes": tag, "step": "fluid_springs", "spawn_overrides": spawn, "terrain_adaptation": "beard_box",
                   "start_pool": f"guhs:{struct}/start", "size": 1, "start_height": {"absolute": 0},
                   "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": 64, "use_expansion_hack": False,
                   "start_jigsaw_name": f"guhs:{struct}_midden"}})
    h.w(f"{D}/worldgen/template_pool/{struct}/start.json", {"fallback": "minecraft:empty", "elements": [{"weight": 1, "element": {
        "element_type": "guhs:grond_single_pool_element", "location": f"guhs:{struct}", "projection": "rigid", "processors": "minecraft:empty",
        "ground_level_delta": G + 1}}]})
    h.w(f"{D}/worldgen/structure_set/{struct}.json", {"structures": [{"structure": f"guhs:{struct}", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 32, "separation": 12, "salt": salt}})
    h.w(f"{D}/tags/worldgen/biome/has_structure/{struct}.json", {"values": BIOMES})


# ---------------------------------------------------------------------------------------------------------------------
# the self-check
# ---------------------------------------------------------------------------------------------------------------------
def _java(rel):
    return open(os.path.join(HERE, "..", "..", "src", "main", "java", "nl", "juiced", "guhs", "feature", "grotspellen", rel), encoding="utf-8").read()


def _const(text, name):
    m = re.search(r"\b" + name + r"\s*=\s*([^,;]+)", text)
    return m.group(1).strip()


def check(spel, S, G, A):
    problems = []
    struct, X, Z, H, lx, (nx, nz), kind, _salt = SPELEN[spel]
    lz = LIFT[1]
    for y, waar in ((0, "hall"), (G, "hut")):
        if S.get(lx, y, lz) != "guhs:wolkenliftje":
            problems.append(f"{spel}: no Wolkenliftje in the {waar} floor")
        for dy in (1, 2):
            if S.get(lx, y + dy, lz) not in (None, "minecraft:air"):
                problems.append(f"{spel}: no room above the {waar} Wolkenliftje ({S.get(lx, y + dy, lz)})")
    if S.get(nx, 0, nz) in (None, "minecraft:air") or S.get(nx, 1, nz) != "minecraft:air" or S.get(nx, 2, nz) != "minecraft:air":
        problems.append(f"{spel}: the host has no floor or no room")
    gs = _java("GrotSpel.java")
    scan, onder, boven = int(_const(gs, "SCAN")), int(_const(gs, "SCAN_ONDER")), int(_const(gs, "SCAN_BOVEN"))

    def bereik(blok):
        uit = []
        for (x, y, z), (name, _p, _nbt) in S.blocks.items():
            if name == blok and y <= H + 1 and (abs(x - nx) > scan or abs(z - nz) > scan or y - 1 < -onder or y - 1 > boven):
                uit.append((x, y, z))
        return uit
    if spel == "schatgraven":
        groot = int(_const(_java("SchatVeld.java"), "GROOTTE"))
        if groot != VELD[2]:
            problems.append(f"SchatVeld.GROOTTE {groot} != {VELD[2]}")
        n = sum(1 for (name, _p, _n) in S.blocks.values() if name == "guhs:grotspel_schatgrond")
        if n != VELD[2] ** 2:
            problems.append(f"schatgraven: {n} field blocks")
        if bereik("guhs:grotspel_schatgrond"):
            problems.append("schatgraven: the field is out of the host's scan range")
    elif spel == "lichtjespuzzel":
        lp = _java("Lichtjespuzzel.java")
        if int(_const(lp, "CEL")) != RASTER[3] or int(_const(_java("LichtjesRaster.java"), "N")) != RASTER[2]:
            problems.append("Lichtjespuzzel.CEL / LichtjesRaster.N differ from RASTER")
        n = sum(1 for (name, _p, _n) in S.blocks.values() if name == "guhs:grotspel_puzzellamp")
        if n != (RASTER[2] * RASTER[3]) ** 2:
            problems.append(f"lichtjespuzzel: {n} lamps")
        if bereik("guhs:grotspel_puzzellamp"):
            problems.append("lichtjespuzzel: the raster is out of the host's scan range")
    else:
        n = sum(1 for (name, _p, _n) in S.blocks.values() if name == "guhs:glimpotje" and _n is None)
        if n < 8:
            problems.append(f"glimwormvangen: only {n} glimpotjes")
        if bereik("guhs:glimpotje") or bereik("guhs:glimplant"):
            problems.append("glimwormvangen: jars or plants out of the host's scan range")
    have = {f[:-5] for f in os.listdir(os.path.join(A, "blockstates"))}
    missing = sorted({name for (name, _p, _n) in S.blocks.values() if name.startswith("guhs:") and name[5:] not in have})
    if missing:
        problems.append(f"{spel}: blocks without a blockstate: {missing}")
    return problems


# ---------------------------------------------------------------------------------------------------------------------
# texts
# ---------------------------------------------------------------------------------------------------------------------
TEKSTEN = {  # key: (Dutch, English)
    # names
    "entity.guhs.guh_npc.schatgraafguh": ("Kapitein Schepsnuit", "Captain Shovelsnout"),
    "entity.guhs.guh_npc.glimwormguh": ("Juf Glimmie", "Miss Glimmy"),
    "entity.guhs.guh_npc.lampjesguh": ("Meester Knipperlicht", "Master Blinky"),
    "entity.guhs.guh_npc.snuffelhulpje": ("Snuffie", "Sniffy"),
    "entity.guhs.vangglimworm": ("Glimwormpje", "Little Glowworm"),
    "block.guhs.grotspel_schatgrond": ("Schatgrond", "Treasure Ground"),
    "block.guhs.grotspel_puzzellamp": ("Puzzellampje", "Puzzle Lamp"),
    "block.guhs.schatzandsteen": ("Schatzandsteen", "Treasure Sandstone"),
    "block.guhs.schatkistplanken": ("Schatkistplanken", "Treasure Chest Planks"),
    "block.guhs.nachtvilt": ("Nachtvilt", "Night Felt"),
    "block.guhs.glimmos": ("Glimmos", "Glow Moss"),
    "block.guhs.paddenstoelsteen": ("Paddenstoelsteen", "Toadstool Stone"),
    "block.guhs.steeltjessteen": ("Steeltjessteen", "Mushroom Stem Bricks"),
    "block.guhs.schatkistje": ("Schatkistje", "Little Treasure Chest"),
    "block.guhs.gouden_kaasje": ("Gouden kaasje", "Golden Cheese"),
    "block.guhs.glimpotje": ("Glimpotje", "Glowworm Jar"),
    "block.guhs.glimplant": ("Glimplant", "Glow Bush"),
    "block.guhs.paddenstoellampje": ("Paddenstoellampje", "Toadstool Lamp"),
    "block.guhs.paddenstoelkrukje": ("Paddenstoelkrukje", "Toadstool Stool"),
    "block.guhs.lampjestegel": ("Lampjestegel", "Lamp Tile"),
    "item.guhs.schatmuntje": ("Schatmuntje", "Treasure Coin"),
    "item.guhs.glimmertje": ("Glimmertje", "Glimmer Coin"),
    "item.guhs.lampjesmunt": ("Lampjesmunt", "Lamp Coin"),
    "item.guhs.schatschepje": ("Zacht schepje", "Soft Shovel"),
    "item.guhs.glimnetje": ("Glimnetje", "Glowworm Net"),
    "item.guhs.glimpotje_rug": ("Glimpotje op je rug", "Glowworm Jar (on your back)"),
    "item.guhs.schatschepje.lore": ("Rechtsklik of linksklik op het schatveld om te graven. Zacht, want het veld is van pluis.",
                                    "Right-click or left-click the treasure field to dig. Soft, because the field is fluff."),
    "item.guhs.glimnetje.lore": ("Rechtsklik: zwaai met je netje naar de glimwormpjes vlak voor je.",
                                 "Right-click: swing your net at the glowworms right in front of you."),
    "item.guhs.glimpotje_rug.lore": ("Hoe meer glimwormpjes erin, hoe lichter hij gloeit.", "The more glowworms in it, the brighter it glows."),
    "item.guhs.grotspel.leen": ("Geleend, alleen voor dit spelletje", "Borrowed, just for this game"),
    "item.guhs.schat_piratenhoedje": ("Piratenhoedje", "Pirate Hat"),
    "item.guhs.schat_oogklepje": ("Oogklepje", "Eye Patch"),
    "item.guhs.glim_antennetjes": ("Glimworm-antennetjes", "Glowworm Feelers"),
    "item.guhs.glim_vlindernetje": ("Vlindernetje", "Butterfly Net"),
    "item.guhs.lichtjes_paddenstoelhoedje": ("Gloeiend paddenstoelhoedje", "Glowing Toadstool Hat"),
    # clothes sources and prices
    "gui.guhs.kledingbron.schatgraven": ("Schatgraven (in de grot)", "Treasure Dig (in the cave)"),
    "gui.guhs.kledingbron.glimwormvangen": ("Glimwormvangen (in de grot)", "Glowworm Catching (in the cave)"),
    "gui.guhs.kledingbron.lichtjespuzzel": ("De Lichtjespuzzel (in de grot)", "The Little Lights Puzzle (in the cave)"),
    "gui.guhs.kleding.prijs.munt.schatmuntjes": ("%s schatmuntjes", "%s treasure coins"),
    "gui.guhs.kleding.prijs.munt.glimmertjes": ("%s glimmertjes", "%s glimmer coins"),
    "gui.guhs.kleding.prijs.munt.lampjesmunten": ("%s lampjesmunten", "%s lamp coins"),
    # the Minigames tab, highscores, the boards, the structures
    "gui.guhs.spelgroep.schatgraven": ("Schatgraven", "Treasure Dig"),
    "gui.guhs.spelgroep.schatgraven.waar": ("Diep onder een roze hutje in de Guhmension: spring op het wolkenliftje, naar Kapitein Schepsnuit.",
                                            "Deep under a pink hut in the Guhmension: jump on the cloud lift, down to Captain Shovelsnout."),
    "gui.guhs.spelgroep.glimwormvangen": ("Glimwormvangen", "Glowworm Catching"),
    "gui.guhs.spelgroep.glimwormvangen.waar": ("Diep onder een roze hutje in de Guhmension: de donkere zaal van Juf Glimmie.",
                                               "Deep under a pink hut in the Guhmension: Miss Glimmy's dark hall."),
    "gui.guhs.spelgroep.lichtjespuzzel": ("De Lichtjespuzzel", "The Little Lights Puzzle"),
    "gui.guhs.spelgroep.lichtjespuzzel.waar": ("Diep onder een roze hutje in de Guhmension: de lampjeszaal van Meester Knipperlicht.",
                                               "Deep under a pink hut in the Guhmension: Master Blinky's lamp hall."),
    "gui.guhs.scorebord.schatgraven": ("Schatgraven top 3", "Treasure Dig top 3"),
    "gui.guhs.scorebord.glimwormvangen": ("Glimwormvangen top 3", "Glowworm Catching top 3"),
    "gui.guhs.scorebord.lichtjespuzzel": ("Lichtjespuzzel top 3", "Little Lights top 3"),
    "structure.guhs.grotspel_schatgraven": ("Schatgraven", "Treasure Dig"),
    "structure.guhs.grotspel_schatgraven.tooltip": ("Minigame in de grot: schatten opgraven met Kapitein Schepsnuit en Snuffie, schatmuntjes en een piratenpakje",
                                                    "Cave minigame: dig up treasure with Captain Shovelsnout and Sniffy, treasure coins and a pirate outfit"),
    "structure.guhs.grotspel_glimwormen": ("Glimwormvangen", "Glowworm Catching"),
    "structure.guhs.grotspel_glimwormen.tooltip": ("Minigame in de grot: glimwormpjes vangen met Juf Glimmie, glimmertjes en glimworm-antennetjes",
                                                   "Cave minigame: catch glowworms with Miss Glimmy, glimmer coins and glowworm feelers"),
    "structure.guhs.grotspel_lichtjes": ("De Lichtjespuzzel", "The Little Lights Puzzle"),
    "structure.guhs.grotspel_lichtjes.tooltip": ("Minigame in de grot: alle paddenstoellampjes aan met Meester Knipperlicht, lampjesmunten en een gloeiend hoedje",
                                                 "Cave minigame: switch on every toadstool lamp with Master Blinky, lamp coins and a glowing hat"),
    # shared
    "gui.guhs.grotspel.optie.makkelijk": ("Makkelijk spelen", "Play easy"),
    "gui.guhs.grotspel.optie.medium": ("Medium spelen", "Play medium"),
    "gui.guhs.grotspel.optie.lastig": ("Lastig spelen", "Play hard"),
    "gui.guhs.grotspel.optie.winkel": ("Winkeltje", "Shop"),
    "gui.guhs.grotspel.optie.uitleg": ("Hoe werkt het?", "How does it work?"),
    "gui.guhs.grotspel.optie.stop": ("Stoppen", "Stop"),
    "gui.guhs.grotspel.optie.verder": ("Verder spelen!", "Keep playing!"),
    "gui.guhs.grotspel.optie.terug": ("Iets anders", "Something else"),
    "gui.guhs.grotspel.optie.doei": ("Doei!", "Bye!"),
    "gui.guhs.grotspel.bezig": ("%s is nu aan het spelen. Kijk maar even mee, of neem een kijkje in mijn winkeltje!",
                                "%s is playing right now. Watch for a bit, or have a look in my shop!"),
    "gui.guhs.grotspel.aftel_sub": ("Maak je klaar...", "Get ready..."),
    "gui.guhs.grotspel.go": ("GUH!", "GUH!"),
    "gui.guhs.grotspel.klaar_goed": ("VAHOEG! %s punten!", "WAHOOG! %s points!"),
    "gui.guhs.grotspel.klaar_aardig": ("Netjes! %s punten", "Nice! %s points"),
    "gui.guhs.grotspel.klaar_oefenen": ("%s punten", "%s points"),
    "gui.guhs.grotspel.klaar_sub": ("+%s %s", "+%s %s"),
    "gui.guhs.grotspel.no_build": ("Niet verbouwen, guhtje: dit is een spelletjeszaal!", "No rebuilding, guhtje: this is a game hall!"),
    "quest.guhs.grotspel.bezig": ("%s is nu aan het spelen. Even wachten, guhtje!", "%s is playing right now. Just a moment, guhtje!"),
    "quest.guhs.grotspel.elders": ("Je bent nog met een ander spelletje bezig! Eerst die afmaken.", "You're still busy with another game! Finish that one first."),
    "quest.guhs.grotspel.kapot": ("Oei... mijn zaal is kapot. Zo kan ik geen spelletje doen.", "Oops... my hall is broken. I can't run a game like this."),
    "quest.guhs.grotspel.vol": ("Je zakken zitten vol! Maak een plekje vrij voor je spullen.", "Your pockets are full! Make some room for your things."),
    "quest.guhs.grotspel.gestopt": ("Gestopt. Geen punten deze keer, maar wel een knuffel.", "Stopped. No points this time, but a hug."),
    "quest.guhs.grotspel.weggelopen": ("Je liep weg! Het spelletje is voorbij.", "You walked away! The game is over."),
    "quest.guhs.grotspel.uitslag": ("%s (%s): %s punten!", "%s (%s): %s points!"),
    "quest.guhs.grotspel.record": ("Nieuw persoonlijk record: %s (was %s)! +%s munten extra!", "New personal record: %s (was %s)! +%s extra coins!"),
    "quest.guhs.grotspel.eerste_record": ("Je eerste score op dit niveau: %s!", "Your first score on this level: %s!"),
    "quest.guhs.grotspel.geen_record": ("Je record blijft %s.", "Your record stays %s."),
    "quest.guhs.grotspel.munten": ("Je krijgt %s %s.", "You get %s %s."),
    "quest.guhs.grotspel.cadeautje": ("Je allereerste potje! Een cadeautje: 16 kaasknabbels en wat moois voor je huisje.",
                                      "Your very first game! A present: 16 cheese nibbles and something nice for your home."),
    "quest.guhs.grotspel.huis_record": ("Het record van de hele wereld! Je naam hangt bovenaan het bord.", "The record of the whole world! Your name is at the top of the board."),
    # --- Schatgraven ---
    "gui.guhs.schatgraven.hallo.0": ("Ahoi, guhtje! Onder dit pluisveld liggen schatten begraven. Pak mijn zachte schepje en graaf ze op! "
                                     "Snuffie helpt je snuffelen. Jouw records: makkelijk %s, medium %s, lastig %s.",
                                     "Ahoy, guhtje! Treasure lies buried under this fluff field. Grab my soft shovel and dig it up! "
                                     "Sniffy helps you sniff. Your records: easy %s, medium %s, hard %s."),
    "gui.guhs.schatgraven.hallo.1": ("Guhrrr! Ik bedoel: arrr! Wie graaft er vandaag mijn schatten op? Records: makkelijk %s, medium %s, lastig %s.",
                                     "Guhrrr! I mean: arrr! Who's digging up my treasure today? Records: easy %s, medium %s, hard %s."),
    "gui.guhs.schatgraven.hallo.2": ("Pas op voor de piepkaas, die piept je tijd weg! Maar een gouden kaasje... vahoeg! Records: makkelijk %s, medium %s, lastig %s.",
                                     "Watch out for the squeaky cheese, it squeaks your time away! But a golden cheese... wahoog! Records: easy %s, medium %s, hard %s."),
    "gui.guhs.schatgraven.bezig_jij": ("Graven, graven, guhtje! Wil je stoppen?", "Dig, dig, guhtje! Do you want to stop?"),
    "gui.guhs.schatgraven.uitleg": ("Je hebt 90 seconden. Rechtsklik (of linksklik) met het zachte schepje op het pluisveld om een plekje open te graven. "
                                    "Snuffie snuffelt mee: KOUD, warm of HEET (dan springt hij!). Elke schat is 10 punten, het gouden kaasje 25, een piepkaas kost je "
                                    "10 seconden. Alles gevonden? Dan tellen de seconden die over zijn ook mee! Makkelijk: een klein veld en veel hulp van Snuffie. "
                                    "Lastig: het hele veld, en Snuffie snuffelt minder vaak.",
                                    "You have 90 seconds. Right-click (or left-click) the fluff field with the soft shovel to dig a spot open. Sniffy sniffs along: "
                                    "COLD, warm or HOT (then he jumps!). Every treasure is 10 points, the golden cheese 25, a squeaky cheese costs you 10 seconds. "
                                    "Found everything? Then the seconds left count too! Easy: a small field and lots of help from Sniffy. Hard: the whole field, and "
                                    "Sniffy sniffs less often."),
    "gui.guhs.schatgraven.go_sub": ("Graaf de schatten op!", "Dig up the treasure!"),
    "gui.guhs.schatgraven.balk": ("Schatten %s/%s  |  %s  |  %s punten", "Treasure %s/%s  |  %s  |  %s points"),
    "gui.guhs.schatgraven.snuffie.koud": ("Snuffie: koud...", "Sniffy: cold..."),
    "gui.guhs.schatgraven.snuffie.warm": ("Snuffie: warm!", "Sniffy: warm!"),
    "gui.guhs.schatgraven.snuffie.heet": ("Snuffie: HEET!!!", "Sniffy: HOT!!!"),
    "gui.guhs.schatgraven.snuffie.veld": ("Snuffie: eerst het veld op, guhtje!", "Sniffy: onto the field first, guhtje!"),
    "gui.guhs.schatgraven.rust": ("Dat stukje speelt nu niet mee.", "That bit isn't part of the game now."),
    "gui.guhs.schatgraven.schat": ("SCHAT! +%s  (%s/%s)", "TREASURE! +%s  (%s/%s)"),
    "gui.guhs.schatgraven.goud": ("GOUDEN KAASJE! +%s", "GOLDEN CHEESE! +%s"),
    "gui.guhs.schatgraven.piep": ("PIEP! Een piepkaas: -%s seconden!", "SQUEAK! A squeaky cheese: -%s seconds!"),
    "gui.guhs.schatgraven.leeg.0": ("Niks... alleen pluis.", "Nothing... just fluff."),
    "gui.guhs.schatgraven.leeg.1": ("Hier ligt alleen een verdwaald sokje.", "Only a lost sock here."),
    "gui.guhs.schatgraven.leeg.2": ("Leeg! Snuffie kijkt beteuterd.", "Empty! Sniffy looks crestfallen."),
    "gui.guhs.schatgraven.leeg.3": ("Pluis, pluis en nog eens pluis.", "Fluff, fluff and more fluff."),
    "quest.guhs.schatgraven.start": ("Arrr, %s! Snuffie snuffelt al. Klaar? 3... 2... 1...", "Arrr, %s! Sniffy is already sniffing. Ready? 3... 2... 1..."),
    "quest.guhs.schatgraven.hoe": ("Rechtsklik met het schepje op het veld om te graven. Snuffie piept: koud, warm, HEET.",
                                   "Right-click the field with the shovel to dig. Sniffy beeps: cold, warm, HOT."),
    "quest.guhs.schatgraven.uitslag": ("Schatten: %s van de %s (%s punten)", "Treasure: %s of %s (%s points)"),
    "quest.guhs.schatgraven.uitslag_goud": ("Gouden kaasje gevonden! (+%s)", "Golden cheese found! (+%s)"),
    "quest.guhs.schatgraven.uitslag_geen_goud": ("Het gouden kaasje bleef verstopt (dat was %s punten geweest)", "The golden cheese stayed hidden (that would have been %s points)"),
    "quest.guhs.schatgraven.uitslag_piep": ("Piepkazen opgegraven: %s", "Squeaky cheeses dug up: %s"),
    "quest.guhs.schatgraven.uitslag_tijd": ("Alles gevonden! Tijdbonus: +%s", "Found everything! Time bonus: +%s"),
    "quest.guhs.schatgraven.uitslag_niet_alles": ("Niet alles gevonden: geen tijdbonus", "Not everything found: no time bonus"),
    "quest.guhs.schatgraven.oordeel_goed": ("Arrr! Jij bent een echte schatguh! Ik maak het veld weer dicht.", "Arrr! You're a real treasure guh! I'll fill the field in again."),
    "quest.guhs.schatgraven.oordeel_aardig": ("Mooi gegraven, guhtje! Snuffie kwispelt.", "Nicely dug, guhtje! Sniffy wags his tail."),
    "quest.guhs.schatgraven.oordeel_oefenen": ("Ach, de schatten zitten er nog. Volgende keer luister je beter naar Snuffie!",
                                               "Oh well, the treasure is still there. Next time listen to Sniffy better!"),
    "quest.guhs.schatgraven.snuffie.0": ("Snuf snuf! Ruik jij ook kaas?", "Sniff sniff! Do you smell cheese too?"),
    "quest.guhs.schatgraven.snuffie.1": ("Ik ben Snuffie. Mijn neus is mijn schepje!", "I'm Sniffy. My nose is my shovel!"),
    "quest.guhs.schatgraven.snuffie.2": ("Piepkaas ruikt ook lekker... dat is het probleem.", "Squeaky cheese smells nice too... that's the problem."),
    "quest.guhs.schatgraven.snuffie.3": ("HEET betekent: hier graven! Echt waar!", "HOT means: dig here! Really!"),
    # --- Glimwormvangen ---
    "gui.guhs.glimwormvangen.hallo.0": ("Psst, zachtjes! Hier fladderen de glimwormpjes. Vang ze met mijn netje en stop ze in het potje op je rug. "
                                        "Jouw records: makkelijk %s, medium %s, lastig %s.",
                                        "Psst, quietly! The glowworms flutter about here. Catch them with my net and put them in the jar on your back. "
                                        "Your records: easy %s, medium %s, hard %s."),
    "gui.guhs.glimwormvangen.hallo.1": ("Een gouden glimworm is vijf keer zo glimmerig! Records: makkelijk %s, medium %s, lastig %s.",
                                        "A golden glowworm is five times as glimmery! Records: easy %s, medium %s, hard %s."),
    "gui.guhs.glimwormvangen.hallo.2": ("Hoe meer je vangt, hoe lichter mijn zaal. Ik lees graag in bed, snap je? Records: makkelijk %s, medium %s, lastig %s.",
                                        "The more you catch, the brighter my hall. I like reading in bed, you see? Records: easy %s, medium %s, hard %s."),
    "gui.guhs.glimwormvangen.bezig_jij": ("Vang ze, guhtje! Wil je stoppen?", "Catch them, guhtje! Do you want to stop?"),
    "gui.guhs.glimwormvangen.uitleg": ("Je hebt 60 seconden. Zwaai met je netje (rechtsklik) naar de glimwormpjes vlak voor je, of tik ze aan. "
                                       "Sommige zijn snel, sommige verstoppen zich in de glimplanten. Een gouden glimworm is 5 punten. Elke vangst gaat in het "
                                       "potje op je rug, en hoe meer je vangt, hoe lichter het potje en de zaal. Lastig: meer en snellere glimwormen, en het begint donkerder.",
                                       "You have 60 seconds. Swing your net (right-click) at the glowworms right in front of you, or tap them. Some are quick, "
                                       "some hide in the glow bushes. A golden glowworm is 5 points. Every catch goes in the jar on your back, and the more you catch, "
                                       "the brighter the jar and the hall. Hard: more and quicker glowworms, and it starts darker."),
    "gui.guhs.glimwormvangen.go_sub": ("Vang de glimwormpjes!", "Catch the glowworms!"),
    "gui.guhs.glimwormvangen.balk": ("Gevangen %s (goud %s)  |  %s  |  %s punten", "Caught %s (gold %s)  |  %s  |  %s points"),
    "gui.guhs.glimwormvangen.vang": ("Gevangen! %s punten", "Caught! %s points"),
    "gui.guhs.glimwormvangen.goud": ("EEN GOUDEN GLIMWORM! +%s", "A GOLDEN GLOWWORM! +%s"),
    "gui.guhs.glimwormvangen.goud_daar": ("Kijk! Een gouden glimworm!", "Look! A golden glowworm!"),
    "quest.guhs.glimwormvangen.start": ("Netje klaar, potje op je rug, %s! Ssst... 3... 2... 1...", "Net ready, jar on your back, %s! Shhh... 3... 2... 1..."),
    "quest.guhs.glimwormvangen.hoe": ("Rechtsklik met het netje: je vangt de glimwormpjes vlak voor je.", "Right-click with the net: you catch the glowworms right in front of you."),
    "quest.guhs.glimwormvangen.uitslag": ("Glimwormpjes: %s", "Glowworms: %s"),
    "quest.guhs.glimwormvangen.uitslag_goud": ("Gouden glimwormen: %s (+%s)", "Golden glowworms: %s (+%s)"),
    "quest.guhs.glimwormvangen.uitslag_geen_goud": ("Geen gouden glimworm deze keer (%s, +%s)", "No golden glowworm this time (%s, +%s)"),
    "quest.guhs.glimwormvangen.oordeel_goed": ("Wat een licht! Nu kan ik eindelijk mijn boekje uitlezen. Dank je, guhtje!",
                                               "What a light! Now I can finally finish my book. Thank you, guhtje!"),
    "quest.guhs.glimwormvangen.oordeel_aardig": ("Mooi zo! Ik zie mijn tenen weer.", "Lovely! I can see my toes again."),
    "quest.guhs.glimwormvangen.oordeel_oefenen": ("Het is nog een beetje donker... maar knus is het wel!", "It's still a bit dark... but it is cosy!"),
    # --- Lichtjespuzzel ---
    "gui.guhs.lichtjespuzzel.hallo.0": ("Oei, mijn paddenstoellampjes zijn in de war! Spring op een lampje: dan knipt hij om, en zijn vier buurtjes ook. "
                                        "Maak ze allemaal aan, dan kan ik weer mijn krantje lezen. Jouw records: makkelijk %s, medium %s, lastig %s.",
                                        "Oh dear, my toadstool lamps are all mixed up! Jump on a lamp: it switches, and so do its four neighbours. "
                                        "Switch them all on, so I can read my paper again. Your records: easy %s, medium %s, hard %s."),
    "gui.guhs.lichtjespuzzel.hallo.1": ("Een lampje erbij is een guh erbij! Of zoiets. Records: makkelijk %s, medium %s, lastig %s.",
                                        "One more lamp is one more guh! Or something like that. Records: easy %s, medium %s, hard %s."),
    "gui.guhs.lichtjespuzzel.hallo.2": ("Denk eerst, spring dan! Minder sprongetjes is meer munten. Records: makkelijk %s, medium %s, lastig %s.",
                                        "Think first, then jump! Fewer jumps is more coins. Records: easy %s, medium %s, hard %s."),
    "gui.guhs.lichtjespuzzel.bezig_jij": ("Springen, springen! Wil je stoppen?", "Jump, jump! Do you want to stop?"),
    "gui.guhs.lichtjespuzzel.uitleg": ("Het grote raster heeft 7 bij 7 lampjes. Spring op een lampje (of buk erop): hij en zijn vier buurtjes schakelen om, "
                                       "aan wordt uit en uit wordt aan. Maak ze ALLEMAAL aan voordat de tijd op is. Elke puzzel kan, en ik weet hoeveel sprongetjes "
                                       "het minstens kost: hoe dichter je daarbij zit en hoe sneller je bent, hoe meer lampjesmunten. Lastig: een moeilijkere puzzel en minder tijd.",
                                       "The big raster has 7 by 7 lamps. Jump on a lamp (or crouch on it): it and its four neighbours switch, on becomes off and "
                                       "off becomes on. Switch them ALL on before the time is up. Every puzzle can be done, and I know the fewest jumps it takes: "
                                       "the closer you get to that and the faster you are, the more lamp coins. Hard: a trickier puzzle and less time."),
    "gui.guhs.lichtjespuzzel.go_sub": ("Alle lampjes aan!", "Every lamp on!"),
    "gui.guhs.lichtjespuzzel.balk": ("Lampjes aan %s/%s  |  sprongetjes %s (kan in %s)  |  %s", "Lamps on %s/%s  |  jumps %s (can be done in %s)  |  %s"),
    "gui.guhs.lichtjespuzzel.klik": ("Klik! %s/%s aan", "Click! %s/%s on"),
    "quest.guhs.lichtjespuzzel.start": ("Een nieuwe puzzel, %s! Kijk goed, denk na... 3... 2... 1...", "A new puzzle, %s! Look closely, think... 3... 2... 1..."),
    "quest.guhs.lichtjespuzzel.hoe": ("Spring op een lampje (of buk erop): hij en zijn buurtjes knippen om. Alle lampjes aan = klaar!",
                                      "Jump on a lamp (or crouch on it): it and its neighbours switch. Every lamp on = done!"),
    "quest.guhs.lichtjespuzzel.uitslag": ("Opgelost in %s sprongetjes (het kon in %s)", "Solved in %s jumps (it could be done in %s)"),
    "quest.guhs.lichtjespuzzel.uitslag_tijd": ("Nog %s seconden over!", "%s seconds left!"),
    "quest.guhs.lichtjespuzzel.uitslag_niet": ("Niet opgelost: %s van de %s lampjes aan", "Not solved: %s of %s lamps on"),
    "quest.guhs.lichtjespuzzel.oordeel_goed": ("Alles aan! Ik kan mijn krantje weer lezen. Er staat in dat jij heel slim bent!",
                                               "Everything on! I can read my paper again. It says you're very clever!"),
    "quest.guhs.lichtjespuzzel.oordeel_aardig": ("Goed gedaan! Het is hier weer gezellig licht.", "Well done! It's nice and bright in here again."),
    "quest.guhs.lichtjespuzzel.oordeel_oefenen": ("Hmm, nog wat donkere plekjes. Probeer het nog eens, guhtje!", "Hmm, still a few dark spots. Try again, guhtje!"),
}
for _spel, _naam in (("schatgraven", ("Schatgraven", "Treasure Dig")), ("glimwormvangen", ("Glimwormvangen", "Glowworm Catching")),
                     ("lichtjespuzzel", ("Lichtjespuzzel", "Little Lights"))):
    for _n, (_nl, _en) in (("makkelijk", ("makkelijk", "easy")), ("medium", ("medium", "medium")), ("lastig", ("lastig", "hard"))):
        TEKSTEN[f"gui.guhs.highscores.game.{_spel}_{_n}"] = (f"{_naam[0]}: {_nl}", f"{_naam[1]}: {_en}")
        TEKSTEN[f"gui.guhs.gids.rij.{_spel}_{_n}"] = (_nl.capitalize(), _en.capitalize())


def texts(h, extra):
    for key, (nl, en) in TEKSTEN.items():
        h.lang(key, en, nl)
    for key, (en, nl) in extra.items():
        h.lang(key, en, nl)


def build(h):
    textures_blokken(h)
    textures_items(h)
    textures_wezens(h)
    textures_kleding(h)
    models(h)
    extra = {}
    problems = []
    for spel, maak in (("schatgraven", zaal_schatgraven), ("glimwormvangen", zaal_glimwormen), ("lichtjespuzzel", zaal_lichtjes)):
        S, G = maak(h, extra)
        structure_files(h, spel, G)
        problems += check(spel, S, G, h.A)
        S.save(SPELEN[spel][0])
    texts(h, extra)
    if problems:
        raise AssertionError("grotspellen self-check:\n  " + "\n  ".join(problems))
    print(f"grotspellen: 3 halls, self-check OK")


# =====================================================================================================================
# on its own: python tools/features/grotspellen.py (from the project root)
# =====================================================================================================================
def _direct():
    import types
    import make_structures as ms
    R = os.path.join("src", "main", "resources")
    A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
    NL, EN = {}, {}

    def w(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def save(img, *path):
        full = os.path.join(A, "textures", *path)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        img.save(full)

    def add_tag(path, values):
        full = f"{R}/data/{path}.json"
        data = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
        for v in values:
            if v not in data["values"]:
                data["values"].append(v)
        w(full, data)

    def lang(key, en, nl):
        EN[key] = en
        NL[key] = nl
    h = types.SimpleNamespace(R=R, A=A, D=D, TEX=os.path.join(A, "textures"), w=w, save=save, add_tag=add_tag, lang=lang,
                              mc=ms.mc, Structure=ms.Structure)
    build(h)
    w(os.path.join("tools", "lang", "en", "c194_grotspellen.json"), dict(sorted(EN.items())))   # (the English overlay of a full run)
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    print(f"grotspellen: {len(NL)} texts")


# --- FTB Quests (tools/make_ftbquests.py): one section per game in the chapter "Onder de Wol" -------------------------------
FTB_CHAPTER = "guhs_onderwol"
FTB_SECTIES = [("ow_schatgraven", "Schatgraven", "npc:schatgraafguh", ["schatgraven_hal", "schatgraven_gespeeld", "schatgraven_pakje"]),
               ("ow_glimwormvangen", "Glimwormvangen", "npc:glimwormguh", ["glimwormvangen_hal", "glimwormvangen_gespeeld", "glimwormvangen_pakje"]),
               ("ow_lichtjespuzzel", "De Lichtjespuzzel", "npc:lampjesguh", ["lichtjespuzzel_hal", "lichtjespuzzel_gespeeld", "lichtjespuzzel_pakje"])]


def ftb(fq):
    q, adv = fq.q, fq.adv
    q("schatgraven_hal", "Schatgraven", "Boven de grond staat een hutje met een wolkenliftje naar beneden. In de grot daaronder wacht "
      "&6Kapitein Schepsnuit&r met &6Snuffie&r (Superkompas: Minigames).", "guhs:schatmuntje", [fq.structure("grotspel_schatgraven")],
      xp=100)
    q("schatgraven_gespeeld", "Koud, warm, HEET!", "Graaf schatten op met het zachte schepje. Snuffie piept koud, warm of HEET en springt "
      "als je er bijna bent. Een gouden kaasje is een bonus, een piepkaas kost tijd. Je verdient &6schatmuntjes&r.", "guhs:schatschepje",
      [adv("schatgraven_gespeeld")], rewards=(("guhs:schatmuntje", 4),), xp=150)
    q("schatgraven_pakje", "Ahoi, piraatje!", "Koop het piratenhoedje en het oogklepje bij Kapitein Schepsnuit, voor schatmuntjes.",
      "guhs:schat_piratenhoedje", [adv("schatgraven_kleding")], shape="gear", xp=200)
    q("glimwormvangen_hal", "Glimwormvangen", "Onder een hutje met een wolkenliftje ligt een donkere zaal vol glimwormpjes. Daar woont "
      "&6Juf Glimmie&r (Superkompas: Minigames).", "guhs:glimmertje", [fq.structure("grotspel_glimwormen")], xp=100)
    q("glimwormvangen_gespeeld", "Vang ze met je netje", "Vang zoveel glimwormpjes als je kunt. Het potje op je rug gaat gloeien en de zaal "
      "wordt steeds lichter. Een gouden glimworm telt extra! Je verdient &6glimmertjes&r.", "guhs:glimnetje",
      [adv("glimwormvangen_gespeeld")], rewards=(("guhs:glimmertje", 4),), xp=150)
    q("glimwormvangen_pakje", "Zelf een glimwormpje", "Koop de glimworm-antennetjes en het vlindernetje bij Juf Glimmie, voor glimmertjes.",
      "guhs:glim_antennetjes", [adv("glimwormvangen_kleding")], shape="gear", xp=200)
    q("lichtjespuzzel_hal", "De Lichtjespuzzel", "Onder een hutje met een wolkenliftje ligt een grot met een groot raster van "
      "paddenstoellampjes. &6Meester Knipperlicht&r legt het uit (Superkompas: Minigames).", "guhs:lampjesmunt",
      [fq.structure("grotspel_lichtjes")], xp=100)
    q("lichtjespuzzel_gespeeld", "Alle lampjes aan!", "Een lampje springt om, samen met zijn vier buren. Krijg ze allemaal aan voor de tijd "
      "om is. Elk potje een nieuwe puzzel! Je verdient &6lampjesmunten&r.", "guhs:paddenstoellampje",
      [adv("lichtjespuzzel_gespeeld")], rewards=(("guhs:lampjesmunt", 4),), xp=150)
    q("lichtjespuzzel_pakje", "Een gloeiend hoedje", "Koop het gloeiende paddenstoelhoedje bij Meester Knipperlicht, voor lampjesmunten.",
      "guhs:lichtjes_paddenstoelhoedje", [adv("lichtjespuzzel_kleding")], shape="gear", xp=200)


if __name__ == "__main__":
    _direct()
